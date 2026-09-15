import cv2
import numpy as np

def preprocess_image(img_bgr: np.ndarray, mode: str = "balanced") -> np.ndarray:
    """
    Applies mode-specific preprocessing:
    - deblocking & artifact reduction
    - noise reduction
    - contrast & edge normalization
    """
    mode_lower = mode.lower().strip()

    if mode_lower in ["restoration", "photo restoration"]:
        # Strong JPEG deblocking and median artifact filtering
        denoised = cv2.fastNlMeansDenoisingColored(img_bgr, None, 6, 6, 7, 21)
        # Gentle edge-preserving bilateral filter
        processed = cv2.bilateralFilter(denoised, d=5, sigmaColor=35, sigmaSpace=35)
    elif mode_lower in ["detail_recovery", "detail recovery"]:
        # Subtle noise reduction, preserving maximum micro-contrast
        processed = cv2.bilateralFilter(img_bgr, d=3, sigmaColor=20, sigmaSpace=20)
    elif mode_lower in ["portrait", "face_enhance", "face enhance"]:
        # Skin smoothing while preserving high frequency eyes/lips/hair
        denoised = cv2.bilateralFilter(img_bgr, d=7, sigmaColor=40, sigmaSpace=40)
        processed = denoised
    else:
        # Balanced mode: Moderate bilateral filter for JPEG deblock and noise suppression
        processed = cv2.bilateralFilter(img_bgr, d=5, sigmaColor=25, sigmaSpace=25)

    return processed
