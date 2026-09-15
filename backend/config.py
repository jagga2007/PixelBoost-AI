import os
from pathlib import Path
import torch

BASE_DIR = Path(__file__).resolve().parent

# Device configuration - automatically use CUDA if available, else MPS or CPU
if torch.cuda.is_available():
    DEVICE = "cuda"
elif hasattr(torch.backends, "mps") and torch.backends.mps.is_available():
    DEVICE = "mps"
else:
    DEVICE = "cpu"

# Directory paths
WEIGHTS_DIR = BASE_DIR / "weights"
TEMP_DIR = BASE_DIR / "temp"
TEMP_DIR.mkdir(parents=True, exist_ok=True)
WEIGHTS_DIR.mkdir(parents=True, exist_ok=True)

# Model configuration paths
MODEL_PATH = os.getenv("MODEL_PATH", str(WEIGHTS_DIR / "RealESRGAN_x4plus.pth"))
FACE_MODEL_PATH = os.getenv("FACE_MODEL_PATH", str(WEIGHTS_DIR / "GFPGANv1.4.pth"))

# Safety and memory boundaries
MAX_UPLOAD_MB = int(os.getenv("MAX_UPLOAD_MB", "50"))
MAX_UPLOAD_BYTES = MAX_UPLOAD_MB * 1024 * 1024

# 16K limit: ~15360 x 8640 = 132,710,400 pixels
MAX_OUTPUT_PIXELS = int(os.getenv("MAX_OUTPUT_PIXELS", str(15360 * 8640)))

# Tiling engine for large images to prevent OOM
TILE_SIZE = int(os.getenv("TILE_SIZE", "512"))
TILE_PAD = int(os.getenv("TILE_PAD", "32"))

# Supported image types
SUPPORTED_MIME_TYPES = {
    "image/jpeg": ".jpg",
    "image/jpg": ".jpg",
    "image/png": ".png",
    "image/webp": ".webp",
}
