import os
import subprocess
import sys
from pathlib import Path
from typing import Optional

from PIL import Image, ImageEnhance, ImageFilter
from providers.diffusion_face import run_diffusion_edit, diffusion_status

EFFECTS = [
    "enhance", "restore", "older", "younger", "hair", "hairstyle", "beard", "remove_beard",
    "smile", "pose", "makeup", "hair_color", "eye_color", "teeth_whiten", "skin_smooth",
    "background_remove", "background_blur", "relight", "blemish_fix", "eye_enhance", "headshot", "background_studio"
]
GENERATIVE_EFFECTS = {
    "older", "younger", "hair", "hairstyle", "beard", "remove_beard", "smile", "pose",
    "makeup", "hair_color", "eye_color", "teeth_whiten", "skin_smooth", "relight", "blemish_fix", "eye_enhance", "headshot", "background_studio"
}


def _run(cmd, cwd: Path):
    p = subprocess.run(cmd, cwd=str(cwd), stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    if p.returncode != 0:
        tail = "\n".join(p.stdout.splitlines()[-40:])
        raise RuntimeError(f"AI provider failed:\n{tail}")


def _rembg_installed() -> bool:
    try:
        import rembg  # noqa: F401
        return True
    except Exception:
        return False


def provider_status():
    return {
        "local_enhance": {"ready": True, "fee": "none", "type": "built-in"},
        "diffusion_face": diffusion_status(),
        "background_remove": {
            "ready": _rembg_installed(), "fee": "none/self-hosted", "engine": "rembg",
            "license_note": "rembg code is MIT; audit the exact segmentation weight provenance/license before commercial redistribution."
        },
        "gfpgan": {"ready": bool(os.getenv("GFPGAN_REPO")), "fee": "none/self-hosted", "license_note": "repository Apache-2.0; verify downloaded weights/dependencies"},
        "legacy_sam": {"ready": False, "fee": "none/self-hosted", "license_note": "kept disabled in commercial profile until inherited weight terms are cleared"},
        "legacy_hairfastgan": {"ready": False, "fee": "none/self-hosted", "license_note": "kept disabled in commercial profile until checkpoint terms are cleared"},
    }


def _local_enhance(src: Path, out: Path, strength: float) -> Path:
    img = Image.open(src).convert("RGB")
    img = img.filter(ImageFilter.UnsharpMask(radius=1.2 + strength, percent=int(110 + 80*strength), threshold=3))
    img = ImageEnhance.Contrast(img).enhance(1.02 + 0.10*strength)
    img = ImageEnhance.Color(img).enhance(1.01 + 0.06*strength)
    img.save(out, "JPEG", quality=95)
    return out


def _gfpgan(src: Path, work: Path) -> Path:
    repo = os.getenv("GFPGAN_REPO")
    if not repo:
        raise RuntimeError("GFPGAN is not configured. Set GFPGAN_REPO to a local clone.")
    out_dir = work / "gfpgan_out"; out_dir.mkdir(exist_ok=True)
    _run([sys.executable, "inference_gfpgan.py", "-i", str(src), "-o", str(out_dir), "-v", "1.4", "-s", "2"], Path(repo))
    candidates = list((out_dir/"restored_imgs").glob("*")) + list(out_dir.rglob("*.jpg")) + list(out_dir.rglob("*.png"))
    if not candidates:
        raise RuntimeError("GFPGAN completed but no restored image was found.")
    out = work / "result.jpg"; Image.open(candidates[0]).convert("RGB").save(out, "JPEG", quality=95); return out


def _background(src: Path, work: Path, blur: bool) -> Path:
    try:
        from rembg import remove
    except Exception as exc:
        raise RuntimeError("Background engine is not installed. Run: pip install 'rembg[cpu]'") from exc
    img = Image.open(src).convert("RGBA")
    fg = remove(img)
    if blur:
        bg = img.convert("RGB").filter(ImageFilter.GaussianBlur(radius=max(8, int(min(img.size)*.018)))).convert("RGBA")
    else:
        bg = Image.new("RGBA", img.size, (245, 245, 245, 255))
    alpha = fg.getchannel("A")
    result = Image.composite(fg, bg, alpha).convert("RGB")
    out = work / "result.jpg"; result.save(out, "JPEG", quality=95); return out


def run_effect(src: Path, effect: str, strength: float, work: Path, reference: Optional[Path] = None) -> Path:
    if effect in {"enhance", "restore"}:
        return _gfpgan(src, work) if os.getenv("GFPGAN_REPO") else _local_enhance(src, work/"result.jpg", strength)
    if effect == "background_remove":
        return _background(src, work, False)
    if effect == "background_blur":
        return _background(src, work, True)
    if effect in GENERATIVE_EFFECTS:
        if os.getenv("ENABLE_GENERATIVE", "0") == "1":
            return run_diffusion_edit(src, effect, strength, work, reference=reference)
        raise RuntimeError("Generative provider is disabled. Set ENABLE_GENERATIVE=1 after installing the local model stack. No paid API is required.")
    raise RuntimeError(f"Unsupported effect: {effect}")
