import gc
import logging
from pathlib import Path
from typing import List, Union
import torch

logger = logging.getLogger("pixelboost.cleanup")

def cleanup_memory():
    """Forces Python garbage collection and frees Torch GPU/MPS cache."""
    gc.collect()
    if torch.cuda.is_available():
        torch.cuda.empty_cache()
        torch.cuda.ipc_collect()

def cleanup_files(file_paths: List[Union[str, Path]]):
    """Safely removes temporary files."""
    for path in file_paths:
        try:
            p = Path(path)
            if p.exists():
                p.unlink()
        except Exception as e:
            logger.warning(f"Failed to delete temp file {path}: {e}")
