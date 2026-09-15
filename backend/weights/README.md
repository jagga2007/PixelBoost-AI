# AI Model Weights Setup for PixelBoost AI

PixelBoost AI leverages state-of-the-art super-resolution and face-restoration architectures.
To enable complete neural inference on the backend, place the following official model weights into this directory (`backend/weights/`):

### 1. Super-Resolution Model (Real-ESRGAN x4)
- **File**: `RealESRGAN_x4plus.pth`
- **Official Source**: [Real-ESRGAN Releases on GitHub](https://github.com/xinntao/Real-ESRGAN/releases/download/v0.1.0/RealESRGAN_x4plus.pth)
- **Purpose**: 4x deep convolutional residual-in-residual dense network (RRDBNet) for edge/texture/detail reconstruction.

### 2. Face Restoration Model (GFPGAN / CodeFormer)
- **File**: `GFPGANv1.4.pth`
- **Official Source**: [GFPGAN Releases on GitHub](https://github.com/TencentARC/GFPGAN/releases/download/v1.3.0/GFPGANv1.4.pth)
- **Purpose**: Blind face restoration preserving identity and facial contours without hallucinating extreme artifacts.

### Automatic Fallback:
If weights are not yet placed in this folder, the backend automatically initializes an adaptive high-order PyTorch neural detail and frequency reconstruction pipeline, and will log the setup instructions without crashing.
