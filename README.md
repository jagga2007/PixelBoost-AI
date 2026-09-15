# PixelBoost AI — 16K Image Enhancer

**PixelBoost AI** is a production-grade Android application and AI super-resolution backend capable of transforming low-quality, blurry, compressed, and noisy images into crystal-clear masterpieces up to **16K** resolution (e.g. 15360 × 8640 for 16:9).

---

## 🌟 Key Features

### 1. Ultra-High Resolution Output
- **2K** (~2560px)
- **4K** (~3840px)
- **8K** (~7680px)
- **12K** (~11520px)
- **16K** (~15360px)
- **Aspect-Ratio Preserved**: Does not force distort non-16:9 images. 4:3, 1:1, and portrait orientations scale proportionally.

### 2. Specialized Enhancement Modes
- **Balanced**: Optimal mix of sharpening, artifact removal, and noise reduction.
- **Face Enhance**: Eyes, lips, facial contour, and skin texture reconstruction.
- **Detail Recovery**: High-frequency recovery for textures, fabrics, foliage, and text.
- **Portrait**: Natural skin smoothing and facial detail preservation.
- **Photo Restoration**: Cleans up heavily compressed, vintage, or degraded photographs.

### 3. Production Architecture
- **Dual Processing Engines**:
  - **AI Server (FastAPI + PyTorch)**: Deep neural super-resolution via Real-ESRGAN / SwinIR and GFPGAN face restoration. Supports tiled inference with linear edge blending to avoid seams and eliminate GPU out-of-memory errors on massive 16K canvases.
  - **On-Device Neural Engine**: Local high-fidelity multi-stage upscaling pipeline running entirely on device with zero internet connection required.
  - **Auto & Fallback Mode**: Automatically switches between on-device and server; if the AI server is unreachable, the app prompts to gracefully execute high-quality local processing.
- **Interactive Before / After Comparison Slider**: Smooth draggable split divider showing real-time comparison between original and enhanced results.
- **Full-Screen Zoomable Viewer**: Pinch-to-zoom (up to 10x), double-tap zoom, and smooth pan controls.
- **Local Room Database History**: Offline SQLite persistence for all past enhancements, output resolutions, processing metrics, and thumbnails.
- **Secure Media Export**: Save directly to Android `Pictures/PixelBoost AI/` via the modern `MediaStore` API and share via `FileProvider`.
- **Material 3 Studio Dark Aesthetics**: Modern obsidian canvas, electric cyan accents, luminous violet badges, and generous 8.dp grid spacing.

---

## 🚀 Quick Start: Backend Server

The Python backend lives in `/backend`.

### Prerequisites
- Python 3.10+
- PyTorch with CUDA (or MPS / CPU)

### 1. Install Dependencies
```bash
pip install -r backend/requirements.txt
```

### 2. Download Model Weights (Optional for testing, required for neural inference)
Place weights into `backend/weights/`:
- `RealESRGAN_x4plus.pth`
- `GFPGANv1.4.pth`

*(If weights are absent, the server automatically executes the high-order structural enhancement fallback pipeline without crashing).*

### 3. Start the Server
```bash
python -m backend.app
# OR
uvicorn backend.app:app --host 0.0.0.0 --port 8000
```

Verify the server is running by opening:
`http://localhost:8000/health`

---

## 📱 Android Client Configuration

1. **Emulator Access**:
   The default server URL is configured to `http://10.0.2.2:8000` (the Android emulator loopback to the host machine).
2. **Physical Device**:
   Navigate to **Settings** within the app and enter your computer's local IP address (e.g., `http://192.168.1.100:8000`), then tap **Test Connection**.
3. **On-Device Only**:
   Select **On Device** under Processing Mode in Settings to enforce 100% offline local processing.

---

## 🧪 Testing

Run Robolectric and unit tests:
```bash
gradle :app:testDebugUnitTest
```

---

## 📄 License
MIT License.
