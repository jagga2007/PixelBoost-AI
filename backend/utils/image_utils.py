import math
from typing import Tuple
from PIL import Image
from backend.config import MAX_OUTPUT_PIXELS, SUPPORTED_MIME_TYPES

# Quality presets corresponding to maximum dimension or standard bounding box
QUALITY_MAX_DIMENSIONS = {
    "2K": 2560,
    "4K": 3840,
    "8K": 7680,
    "12K": 11520,
    "16K": 15360,
}

def calculate_target_dimensions(orig_w: int, orig_h: int, quality: str) -> Tuple[int, int]:
    """
    Calculates target resolution strictly preserving original aspect ratio.
    Ensures total pixel count does not exceed MAX_OUTPUT_PIXELS to prevent memory exhaustion.
    """
    quality_upper = quality.upper().strip()
    max_dim = QUALITY_MAX_DIMENSIONS.get(quality_upper, 3840)

    aspect_ratio = orig_w / float(orig_h)

    if orig_w >= orig_h:
        target_w = max_dim
        target_h = int(round(target_w / aspect_ratio))
    else:
        target_h = max_dim
        target_w = int(round(target_h * aspect_ratio))

    # Guard total pixels against upper memory limit
    total_pixels = target_w * target_h
    if total_pixels > MAX_OUTPUT_PIXELS:
        scale_factor = math.sqrt(MAX_OUTPUT_PIXELS / float(total_pixels))
        target_w = int(round(target_w * scale_factor))
        target_h = int(round(target_h * scale_factor))

    # Ensure even dimensions (crucial for neural encoders/decoders)
    target_w = target_w if target_w % 2 == 0 else target_w + 1
    target_h = target_h if target_h % 2 == 0 else target_h + 1

    return target_w, target_h

def validate_image_file(file_content: bytes, content_type: str) -> str:
    """
    Validates file headers, content type, and integrity.
    Returns detected format extension (e.g., '.jpg').
    """
    if content_type not in SUPPORTED_MIME_TYPES:
        raise ValueError(f"Unsupported content type '{content_type}'. Allowed: {list(SUPPORTED_MIME_TYPES.keys())}")

    # Inspect magic bytes for JPG, PNG, WEBP
    if file_content.startswith(b"\xFF\xD8\xFF"):
        return ".jpg"
    elif file_content.startswith(b"\x89PNG\r\n\x1a\n"):
        return ".png"
    elif file_content.startswith(b"RIFF") and file_content[8:12] == b"WEBP":
        return ".webp"
    else:
        # Fallback inspection with PIL
        try:
            from io import BytesIO
            with Image.open(BytesIO(file_content)) as img:
                img.verify()
                fmt = img.format.lower()
                if fmt in ["jpeg", "jpg"]:
                    return ".jpg"
                elif fmt == "png":
                    return ".png"
                elif fmt == "webp":
                    return ".webp"
                else:
                    raise ValueError(f"Unsupported image format: {fmt}")
        except Exception as e:
            raise ValueError(f"Corrupted or invalid image data: {str(e)}")
