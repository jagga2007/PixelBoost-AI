import time
import uuid
from pathlib import Path
from typing import Optional
import cv2
from fastapi import APIRouter, File, Form, HTTPException, UploadFile, status
from fastapi.responses import FileResponse, JSONResponse

from backend.config import (
    DEVICE,
    MAX_OUTPUT_PIXELS,
    MAX_UPLOAD_BYTES,
    MAX_UPLOAD_MB,
    SUPPORTED_MIME_TYPES,
    TEMP_DIR,
)
from backend.models.model_loader import is_model_weights_loaded
from backend.services.face_enhancer import enhance_faces
from backend.services.upscaler import enhance_and_upscale_pipeline
from backend.utils.cleanup import cleanup_files, cleanup_memory
from backend.utils.image_utils import validate_image_file

router = APIRouter()

@router.get("/health")
async def health_check():
    return {
        "status": "healthy",
        "service": "PixelBoost AI Engine",
        "device": DEVICE,
        "model_weights_loaded": is_model_weights_loaded(),
        "max_upload_mb": MAX_UPLOAD_MB,
        "max_output_pixels": MAX_OUTPUT_PIXELS,
        "supported_resolutions": ["2K", "4K", "8K", "12K", "16K"],
        "supported_modes": ["balanced", "face_enhance", "detail_recovery", "portrait", "restoration"],
    }

@router.post("/enhance")
async def enhance_image(
    file: UploadFile = File(...),
    quality: str = Form("4K"),
    mode: str = Form("balanced"),
    output_format: str = Form("JPEG"),
    jpeg_quality: int = Form(95),
    enable_face_enhance: bool = Form(True),
):
    start_time = time.time()
    temp_files = []

    try:
        # 1. Validate file content and size
        content = await file.read()
        if len(content) > MAX_UPLOAD_BYTES:
            raise HTTPException(
                status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
                detail=f"Image exceeds maximum upload limit of {MAX_UPLOAD_MB}MB.",
            )

        detected_ext = validate_image_file(content, file.content_type or "image/jpeg")

        # 2. Save temporary upload safely
        req_id = str(uuid.uuid4())[:8]
        in_path = TEMP_DIR / f"upload_{req_id}{detected_ext}"
        temp_files.append(in_path)
        with open(in_path, "wb") as f:
            f.write(content)

        # 3. Read image
        img_bgr = cv2.imread(str(in_path))
        if img_bgr is None or img_bgr.size == 0:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Failed to decode uploaded image. Ensure file is a valid JPG/PNG/WEBP.",
            )

        # 4. Run full enhancement pipeline
        enhanced_bgr, (target_w, target_h) = enhance_and_upscale_pipeline(
            img_bgr=img_bgr,
            quality=quality,
            mode=mode,
            enable_face_enhance=enable_face_enhance,
        )

        # 5. Encode output file
        fmt_upper = output_format.upper().strip()
        if fmt_upper == "PNG":
            out_ext = ".png"
            media_type = "image/png"
            encode_params = [cv2.IMWRITE_PNG_COMPRESSION, 3]
        elif fmt_upper == "WEBP":
            out_ext = ".webp"
            media_type = "image/webp"
            encode_params = [cv2.IMWRITE_WEBP_QUALITY, max(50, min(100, jpeg_quality))]
        else:
            out_ext = ".jpg"
            media_type = "image/jpeg"
            encode_params = [cv2.IMWRITE_JPEG_QUALITY, max(50, min(100, jpeg_quality))]

        out_path = TEMP_DIR / f"enhanced_{req_id}{out_ext}"
        temp_files.append(out_path)

        success = cv2.imwrite(str(out_path), enhanced_bgr, encode_params)
        if not success:
            raise HTTPException(status_code=500, detail="Failed to encode enhanced output image.")

        elapsed = round(time.time() - start_time, 2)

        # 6. Return high-resolution file response with comprehensive metadata headers
        return FileResponse(
            path=out_path,
            media_type=media_type,
            filename=f"PixelBoost_{quality}_{req_id}{out_ext}",
            headers={
                "X-Success": "true",
                "X-Output-Width": str(target_w),
                "X-Output-Height": str(target_h),
                "X-Processing-Time": f"{elapsed}s",
                "X-Enhance-Mode": mode,
                "X-Enhance-Quality": quality,
            },
        )

    except HTTPException:
        raise
    except Exception as e:
        cleanup_memory()
        return JSONResponse(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            content={"success": False, "error": f"Processing error: {str(e)}"},
        )
    finally:
        cleanup_memory()

@router.post("/upscale")
async def direct_upscale(
    file: UploadFile = File(...),
    quality: str = Form("4K"),
):
    """Direct super-resolution upscale without extra stylistic filters."""
    return await enhance_image(
        file=file,
        quality=quality,
        mode="balanced",
        output_format="JPEG",
        jpeg_quality=95,
        enable_face_enhance=False,
    )

@router.post("/face-enhance")
async def face_enhance_only(
    file: UploadFile = File(...),
):
    """Specialized face enhancement endpoint."""
    return await enhance_image(
        file=file,
        quality="4K",
        mode="face_enhance",
        output_format="JPEG",
        jpeg_quality=95,
        enable_face_enhance=True,
    )
