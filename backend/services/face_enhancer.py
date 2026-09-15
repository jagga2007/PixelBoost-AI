import cv2
import numpy as np

# Load built-in OpenCV frontal face cascade
FACE_CASCADE = cv2.CascadeClassifier(cv2.data.haarcascades + "haarcascade_frontalface_default.xml")
EYE_CASCADE = cv2.CascadeClassifier(cv2.data.haarcascades + "haarcascade_eye.xml")

def enhance_faces(img_bgr: np.ndarray, strength: float = 0.75) -> np.ndarray:
    """
    Detects faces in the image and performs fine structural restoration on facial regions:
    - Micro-detail sharpening on eyes and facial contours
    - Gentle skin tonal smoothing
    - Seamless alpha-feathered blending back into image
    Preserves original facial identity without artificial distortion.
    """
    gray = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2GRAY)
    faces = FACE_CASCADE.detectMultiScale(gray, scaleFactor=1.15, minNeighbors=5, minSize=(60, 60))

    if len(faces) == 0:
        return img_bgr

    result = img_bgr.copy()
    h_img, w_img = img_bgr.shape[:2]

    for (x, y, w, h) in faces:
        # Pad bounding box slightly
        pad_x = int(w * 0.1)
        pad_y = int(h * 0.1)
        x1 = max(0, x - pad_x)
        y1 = max(0, y - pad_y)
        x2 = min(w_img, x + w + pad_x)
        y2 = min(h_img, y + h + pad_y)

        face_roi = img_bgr[y1:y2, x1:x2]
        if face_roi.size == 0:
            continue

        # Detail enhancement on facial ROI:
        # 1. Bilateral filter for skin texture smoothing
        smooth = cv2.bilateralFilter(face_roi, d=5, sigmaColor=30, sigmaSpace=30)

        # 2. Unsharp mask for facial features (eyes, lips, brows)
        gaussian = cv2.GaussianBlur(smooth, (0, 0), 2.0)
        sharpened_face = cv2.addWeighted(smooth, 1.35, gaussian, -0.35, 0)

        # 3. Create an elliptical feathered mask for natural blending
        roi_h, roi_w = face_roi.shape[:2]
        mask = np.zeros((roi_h, roi_w), dtype=np.float32)
        center = (roi_w // 2, roi_h // 2)
        axes = (int(roi_w * 0.45), int(roi_h * 0.45))
        cv2.ellipse(mask, center, axes, 0, 0, 360, 1.0, -1)
        mask = cv2.GaussianBlur(mask, (21, 21), 11.0)
        mask = np.expand_dims(mask, axis=2)

        # Blend original with restored face
        blended = (face_roi * (1.0 - mask * strength) + sharpened_face * (mask * strength)).astype(np.uint8)
        result[y1:y2, x1:x2] = blended

    return result
