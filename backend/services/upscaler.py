import math
import logging
from typing import Tuple
import cv2
import numpy as np
import torch
import torch.nn.functional as F

from backend.config import DEVICE, TILE_SIZE, TILE_PAD
from backend.models.model_loader import get_super_resolution_model, is_model_weights_loaded
from backend.services.preprocessing import preprocess_image
from backend.services.face_enhancer import enhance_faces
from backend.services.postprocessing import postprocess_image
from backend.utils.cleanup import cleanup_memory
from backend.utils.image_utils import calculate_target_dimensions

logger = logging.getLogger("pixelboost.upscaler")

def _infer_tile(model: torch.nn.Module, tile_rgb: np.ndarray) -> np.ndarray:
    """Passes a single RGB tile through the 4x PyTorch model."""
    img_tensor = torch.from_numpy(tile_rgb.transpose(2, 0, 1)).float().div(255.0).unsqueeze(0).to(DEVICE)
    with torch.no_grad():
        out_tensor = model(img_tensor)
        out_tensor = torch.clamp(out_tensor, 0.0, 1.0)
    out_np = (out_tensor.squeeze(0).permute(1, 2, 0).cpu().numpy() * 255.0).round().astype(np.uint8)
    return out_np

def run_tiled_inference_4x(img_bgr: np.ndarray) -> np.ndarray:
    """
    Performs memory-conscious 4x super-resolution using overlapping tiles.
    Uses linear weight blending to eliminate border artifacts and seams.
    """
    model = get_super_resolution_model()
    h, w, c = img_bgr.shape
    tile_size = TILE_SIZE
    tile_pad = TILE_PAD
    scale = 4

    out_h = h * scale
    out_w = w * scale

    # If small enough, run direct inference without tiling
    if h <= tile_size and w <= tile_size:
        img_rgb = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2RGB)
        out_rgb = _infer_tile(model, img_rgb)
        return cv2.cvtColor(out_rgb, cv2.COLOR_RGB2BGR)

    # Convert to RGB for inference
    img_rgb = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2RGB)

    output = np.zeros((out_h, out_w, c), dtype=np.float32)
    weight_map = np.zeros((out_h, out_w, 1), dtype=np.float32)

    tiles_x = math.ceil(w / tile_size)
    tiles_y = math.ceil(h / tile_size)

    for yi in range(tiles_y):
        for xi in range(tiles_x):
            # Tile coordinates in input
            x_start = xi * tile_size
            x_end = min(x_start + tile_size, w)
            y_start = yi * tile_size
            y_end = min(y_start + tile_size, h)

            # Add padding
            x_start_pad = max(x_start - tile_pad, 0)
            x_end_pad = min(x_end + tile_pad, w)
            y_start_pad = max(y_start - tile_pad, 0)
            y_end_pad = min(y_end + tile_pad, h)

            tile_in = img_rgb[y_start_pad:y_end_pad, x_start_pad:x_end_pad]
            tile_out = _infer_tile(model, tile_in).astype(np.float32)

            # Determine coordinates of non-padded tile region in the output
            out_x_start = x_start * scale
            out_x_end = x_end * scale
            out_y_start = y_start * scale
            out_y_end = y_end * scale

            # Offsets within the padded tile output
            offset_x = (x_start - x_start_pad) * scale
            offset_y = (y_start - y_start_pad) * scale
            crop_w = (x_end - x_start) * scale
            crop_h = (y_end - y_start) * scale

            cropped_tile_out = tile_out[offset_y:offset_y + crop_h, offset_x:offset_x + crop_w]

            # Linear edge feathering for smooth stitching
            tw, th = crop_w, crop_h
            w_tile = np.ones((th, tw, 1), dtype=np.float32)
            feather = min(tile_pad * scale, tw // 4, th // 4)
            if feather > 0:
                for i in range(feather):
                    factor = float(i) / feather
                    if xi > 0:
                        w_tile[:, i, 0] = np.minimum(w_tile[:, i, 0], factor)
                    if xi < tiles_x - 1:
                        w_tile[:, tw - 1 - i, 0] = np.minimum(w_tile[:, tw - 1 - i, 0], factor)
                    if yi > 0:
                        w_tile[i, :, 0] = np.minimum(w_tile[i, :, 0], factor)
                    if yi < tiles_y - 1:
                        w_tile[th - 1 - i, :, 0] = np.minimum(w_tile[th - 1 - i, :, 0], factor)

            output[out_y_start:out_y_end, out_x_start:out_x_end] += cropped_tile_out * w_tile
            weight_map[out_y_start:out_y_end, out_x_start:out_x_end] += w_tile

    # Normalize by weights
    weight_map = np.maximum(weight_map, 1e-4)
    final_rgb = np.clip(output / weight_map, 0, 255).astype(np.uint8)
    cleanup_memory()

    return cv2.cvtColor(final_rgb, cv2.COLOR_RGB2BGR)

def enhance_and_upscale_pipeline(
    img_bgr: np.ndarray,
    quality: str = "4K",
    mode: str = "balanced",
    enable_face_enhance: bool = True
) -> Tuple[np.ndarray, Tuple[int, int]]:
    """
    Complete production enhancement pipeline:
    1. Preprocessing (AI Denoise, JPEG artifact removal, tone prep)
    2. Real AI 4x Super-Resolution (RRDBNet / Real-ESRGAN with tiled inference)
    3. Target Resolution Intelligent Upscaling (e.g. up to 16K, strictly aspect-ratio preserving)
    4. Face Restoration (if requested or in portrait/face mode)
    5. Postprocessing (Contrast-Adaptive Sharpening & detail optimization)
    """
    orig_h, orig_w = img_bgr.shape[:2]
    target_w, target_h = calculate_target_dimensions(orig_w, orig_h, quality)

    logger.info(f"Pipeline started: Input {orig_w}x{orig_h} -> Target {target_w}x{target_h} ({quality}, {mode})")

    # Step 1: Preprocessing
    step1_pre = preprocess_image(img_bgr, mode=mode)
    cleanup_memory()

    # Step 2: AI 4x Super-Resolution
    step2_sr = run_tiled_inference_4x(step1_pre)
    cleanup_memory()

    # Step 3: Progressive Scaling to Target Resolution if required
    cur_h, cur_w = step2_sr.shape[:2]
    if (cur_w, cur_h) != (target_w, target_h):
        # Use high-quality Lanczos / Area interpolation for progressive adjustment
        interp = cv2.INTER_LANCZOS4 if (target_w > cur_w) else cv2.INTER_AREA
        step3_scaled = cv2.resize(step2_sr, (target_w, target_h), interpolation=interp)
    else:
        step3_scaled = step2_sr

    del step2_sr
    cleanup_memory()

    # Step 4: Face Enhancement
    if enable_face_enhance or mode.lower() in ["face_enhance", "portrait", "face enhance"]:
        step4_face = enhance_faces(step3_scaled, strength=0.8)
    else:
        step4_face = step3_scaled

    # Step 5: Postprocessing & Final Sharpening
    final_enhanced = postprocess_image(step4_face, mode=mode)
    cleanup_memory()

    logger.info(f"Pipeline finished: Output resolution {target_w}x{target_h}")
    return final_enhanced, (target_w, target_h)
