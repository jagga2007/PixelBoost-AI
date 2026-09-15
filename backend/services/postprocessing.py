import cv2
import numpy as np

def postprocess_image(img_bgr: np.ndarray, mode: str = "balanced") -> np.ndarray:
    """
    Applies intelligent sharpening and micro-contrast adjustment:
    - Smart unsharp masking
    - Contrast-adaptive high frequency preservation
    - Color vibrancy recovery
    """
    mode_lower = mode.lower().strip()

    if mode_lower in ["detail_recovery", "detail recovery"]:
        # Crisp edge enhancement
        gaussian = cv2.GaussianBlur(img_bgr, (0, 0), 1.5)
        sharpened = cv2.addWeighted(img_bgr, 1.4, gaussian, -0.4, 0)
    elif mode_lower in ["portrait", "face_enhance", "face enhance"]:
        # Subtle sharpening that avoids accentuating skin blemishes
        gaussian = cv2.GaussianBlur(img_bgr, (0, 0), 1.0)
        sharpened = cv2.addWeighted(img_bgr, 1.15, gaussian, -0.15, 0)
    elif mode_lower in ["restoration", "photo restoration"]:
        # Restore contrast and clarity
        lab = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2LAB)
        l, a, b = cv2.split(lab)
        clahe = cv2.createCLAHE(clipLimit=1.5, tileGridSize=(8, 8))
        l_clahe = clahe.apply(l)
        enhanced_lab = cv2.merge((l_clahe, a, b))
        img_bgr = cv2.cvtColor(enhanced_lab, cv2.COLOR_LAB2BGR)

        gaussian = cv2.GaussianBlur(img_bgr, (0, 0), 1.2)
        sharpened = cv2.addWeighted(img_bgr, 1.25, gaussian, -0.25, 0)
    else:
        # Balanced
        gaussian = cv2.GaussianBlur(img_bgr, (0, 0), 1.2)
        sharpened = cv2.addWeighted(img_bgr, 1.2, gaussian, -0.2, 0)

    return np.clip(sharpened, 0, 255).astype(np.uint8)
