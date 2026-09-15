import logging
from contextlib import asynccontextmanager
import uvicorn
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from backend.api.routes import router
from backend.config import DEVICE
from backend.models.model_loader import get_super_resolution_model
from backend.utils.cleanup import cleanup_memory

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s"
)
logger = logging.getLogger("pixelboost.main")

@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("=" * 60)
    logger.info("Initializing PixelBoost AI — 16K Image Enhancer Server")
    logger.info(f"Target compute device: {DEVICE}")
    logger.info("=" * 60)
    try:
        # Pre-warm model in memory
        get_super_resolution_model()
    except Exception as e:
        logger.warning(f"Model warm-up note: {e}")
    yield
    cleanup_memory()
    logger.info("PixelBoost AI server shutting down. Resources released.")

app = FastAPI(
    title="PixelBoost AI — 16K Image Enhancer API",
    description="High-throughput super-resolution and face enhancement engine supporting up to 16K output.",
    version="1.0.0",
    lifespan=lifespan,
)

# CORS middleware for mobile and web clients
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Mount API routes
app.include_router(router)

if __name__ == "__main__":
    uvicorn.run("backend.app:app", host="0.0.0.0", port=8000, reload=False, workers=1)
