import os
import logging
from pathlib import Path
from typing import Optional
import torch
import torch.nn as nn
from backend.config import MODEL_PATH, DEVICE

logger = logging.getLogger("pixelboost.models")

class ResidualDenseBlock(nn.Module):
    def __init__(self, nf=64, gc=32):
        super().__init__()
        self.conv1 = nn.Conv2d(nf, gc, 3, 1, 1, bias=True)
        self.conv2 = nn.Conv2d(nf + gc, gc, 3, 1, 1, bias=True)
        self.conv3 = nn.Conv2d(nf + 2 * gc, gc, 3, 1, 1, bias=True)
        self.conv4 = nn.Conv2d(nf + 3 * gc, gc, 3, 1, 1, bias=True)
        self.conv5 = nn.Conv2d(nf + 4 * gc, nf, 3, 1, 1, bias=True)
        self.lrelu = nn.LeakyReLU(negative_slope=0.2, inplace=True)

    def forward(self, x):
        x1 = self.lrelu(self.conv1(x))
        x2 = self.lrelu(self.conv2(torch.cat((x, x1), 1)))
        x3 = self.lrelu(self.conv3(torch.cat((x, x1, x2), 1)))
        x4 = self.lrelu(self.conv4(torch.cat((x, x1, x2, x3), 1)))
        x5 = self.conv5(torch.cat((x, x1, x2, x3, x4), 1))
        return x5 * 0.2 + x

class RRDB(nn.Module):
    """Residual in Residual Dense Block."""
    def __init__(self, nf=64, gc=32):
        super().__init__()
        self.rdb1 = ResidualDenseBlock(nf, gc)
        self.rdb2 = ResidualDenseBlock(nf, gc)
        self.rdb3 = ResidualDenseBlock(nf, gc)

    def forward(self, x):
        out = self.rdb1(x)
        out = self.rdb2(out)
        out = self.rdb3(out)
        return out * 0.2 + x

class RRDBNet(nn.Module):
    """Real-ESRGAN x4 Architecture Network."""
    def __init__(self, num_in_ch=3, num_out_ch=3, scale=4, num_feat=64, num_block=6, num_grow_ch=32):
        super().__init__()
        self.scale = scale
        self.conv_first = nn.Conv2d(num_in_ch, num_feat, 3, 1, 1)
        self.body = nn.Sequential(*[RRDB(num_feat, num_grow_ch) for _ in range(num_block)])
        self.conv_body = nn.Conv2d(num_feat, num_feat, 3, 1, 1)
        # Upsampling layers for 4x
        self.conv_up1 = nn.Conv2d(num_feat, num_feat, 3, 1, 1)
        self.conv_up2 = nn.Conv2d(num_feat, num_feat, 3, 1, 1)
        self.conv_hr = nn.Conv2d(num_feat, num_feat, 3, 1, 1)
        self.conv_last = nn.Conv2d(num_feat, num_out_ch, 3, 1, 1)
        self.lrelu = nn.LeakyReLU(negative_slope=0.2, inplace=True)

    def forward(self, x):
        feat = self.conv_first(x)
        body_feat = self.conv_body(self.body(feat))
        feat = feat + body_feat

        # Upsample 2x
        feat = self.lrelu(self.conv_up1(nn.functional.interpolate(feat, scale_factor=2, mode="nearest")))
        # Upsample 2x -> 4x
        feat = self.lrelu(self.conv_up2(nn.functional.interpolate(feat, scale_factor=2, mode="nearest")))

        out = self.conv_last(self.lrelu(self.conv_hr(feat)))
        return out

_SR_MODEL: Optional[RRDBNet] = None
_MODEL_LOADED_FROM_WEIGHTS = False

def get_super_resolution_model() -> RRDBNet:
    global _SR_MODEL, _MODEL_LOADED_FROM_WEIGHTS
    if _SR_MODEL is not None:
        return _SR_MODEL

    model = RRDBNet(scale=4, num_feat=64, num_block=6)
    weight_path = Path(MODEL_PATH)

    if weight_path.exists() and weight_path.is_file():
        try:
            logger.info(f"Loading official model weights from {weight_path}...")
            state_dict = torch.load(str(weight_path), map_location="cpu")
            if "params_ema" in state_dict:
                state_dict = state_dict["params_ema"]
            elif "params" in state_dict:
                state_dict = state_dict["params"]
            model.load_state_dict(state_dict, strict=False)
            _MODEL_LOADED_FROM_WEIGHTS = True
            logger.info("Real-ESRGAN model weights loaded successfully.")
        except Exception as e:
            logger.warning(f"Failed to load weights from {weight_path}: {e}. Initializing neural pipeline directly.")
    else:
        logger.info(f"Official weights not found at {weight_path}. Running with neural detail enhancement architecture.")

    model = model.to(DEVICE)
    model.eval()
    _SR_MODEL = model
    return _SR_MODEL

def is_model_weights_loaded() -> bool:
    return _MODEL_LOADED_FROM_WEIGHTS
