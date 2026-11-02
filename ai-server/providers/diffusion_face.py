import os
from pathlib import Path
from typing import Optional, Tuple

from PIL import Image, ImageDraw, ImageFilter

DEFAULT_MODEL = os.getenv("FACE_DIFFUSION_MODEL", "stable-diffusion-v1-5/stable-diffusion-inpainting")
DEFAULT_IP_ADAPTER_REPO = os.getenv("FACE_IP_ADAPTER_REPO", "h94/IP-Adapter")
DEFAULT_IP_ADAPTER_WEIGHT = os.getenv("FACE_IP_ADAPTER_WEIGHT", "ip-adapter_sd15.bin")

_PIPE = None
_PIPE_MODEL = None
_IP_ADAPTER_LOADED = False


def _torch_dtype(torch):
    return torch.float16 if torch.cuda.is_available() else torch.float32


def _load_pipeline(with_reference: bool = False):
    global _PIPE, _PIPE_MODEL, _IP_ADAPTER_LOADED
    if os.getenv("ENABLE_GENERATIVE", "0") != "1":
        raise RuntimeError("Generative editing is disabled. Set ENABLE_GENERATIVE=1 after installing requirements-generative.txt.")
    try:
        import torch
        from diffusers import StableDiffusionInpaintPipeline
    except Exception as exc:
        raise RuntimeError("Generative dependencies are missing. Install ai-server/requirements-generative.txt.") from exc

    model_id = os.getenv("FACE_DIFFUSION_MODEL", DEFAULT_MODEL)
    if _PIPE is None or _PIPE_MODEL != model_id:
        pipe = StableDiffusionInpaintPipeline.from_pretrained(model_id, torch_dtype=_torch_dtype(torch))
        pipe = pipe.to("cuda" if torch.cuda.is_available() else "cpu")
        try:
            pipe.enable_attention_slicing()
        except Exception:
            pass
        pipe.set_progress_bar_config(disable=True)
        _PIPE, _PIPE_MODEL, _IP_ADAPTER_LOADED = pipe, model_id, False

    if with_reference and not _IP_ADAPTER_LOADED:
        if os.getenv("ENABLE_IP_ADAPTER", "0") != "1":
            raise RuntimeError("Reference-image editing is disabled. Set ENABLE_IP_ADAPTER=1 after reviewing the selected adapter/model license.")
        repo = os.getenv("FACE_IP_ADAPTER_REPO", DEFAULT_IP_ADAPTER_REPO)
        weight = os.getenv("FACE_IP_ADAPTER_WEIGHT", DEFAULT_IP_ADAPTER_WEIGHT)
        subfolder = os.getenv("FACE_IP_ADAPTER_SUBFOLDER", "models")
        try:
            _PIPE.load_ip_adapter(repo, subfolder=subfolder, weight_name=weight)
            _PIPE.set_ip_adapter_scale(float(os.getenv("FACE_IP_ADAPTER_SCALE", "0.65")))
            _IP_ADAPTER_LOADED = True
        except Exception as exc:
            raise RuntimeError(f"Could not load IP-Adapter reference module: {exc}") from exc
    return _PIPE


def _detect_face_box(img: Image.Image) -> Tuple[int, int, int, int]:
    w, h = img.size
    try:
        import cv2
        import numpy as np
        rgb = np.asarray(img.convert("RGB"))
        gray = cv2.cvtColor(rgb, cv2.COLOR_RGB2GRAY)
        cascade = cv2.CascadeClassifier(cv2.data.haarcascades + "haarcascade_frontalface_default.xml")
        faces = cascade.detectMultiScale(gray, 1.1, 5, minSize=(max(48, w // 12), max(48, h // 12)))
        if len(faces):
            x, y, fw, fh = max(faces, key=lambda r: int(r[2]) * int(r[3]))
            return int(x), int(y), int(x + fw), int(y + fh)
    except Exception:
        pass
    fw, fh = int(w * 0.46), int(h * 0.52)
    cx, cy = w // 2, int(h * 0.43)
    return max(0, cx-fw//2), max(0, cy-fh//2), min(w, cx+fw//2), min(h, cy+fh//2)


def _ellipse(d, box, fill=255):
    d.ellipse(tuple(int(v) for v in box), fill=fill)


def _mask_for_effect(img: Image.Image, effect: str) -> Image.Image:
    w, h = img.size
    x1, y1, x2, y2 = _detect_face_box(img)
    fw, fh = x2-x1, y2-y1
    mask = Image.new("L", (w, h), 0)
    d = ImageDraw.Draw(mask)

    if effect in {"older", "younger", "age", "pose", "makeup"}:
        _ellipse(d, (x1-fw*.18, y1-fh*.18, x2+fw*.18, y2+fh*.18))
    elif effect in {"hair", "hairstyle", "hair_color", "reference_hair"}:
        _ellipse(d, (x1-fw*.30, y1-fh*.62, x2+fw*.30, y1+fh*.50))
    elif effect in {"beard", "remove_beard"}:
        _ellipse(d, (x1+fw*.05, y1+fh*.43, x2-fw*.05, y2+fh*.10))
    elif effect in {"smile", "teeth_whiten"}:
        _ellipse(d, (x1+fw*.20, y1+fh*.55, x2-fw*.20, y1+fh*.91))
    elif effect in {"eye_color", "eye_enhance"}:
        _ellipse(d, (x1+fw*.13, y1+fh*.25, x2-fw*.13, y1+fh*.53))
    elif effect in {"skin_smooth", "blemish_fix", "relight", "headshot"}:
        _ellipse(d, (x1+fw*.04, y1+fh*.08, x2-fw*.04, y2-fh*.04))
    elif effect == "wardrobe":
        d.rounded_rectangle((max(0, x1-fw*.75), max(0, y2-fh*.05), min(w, x2+fw*.75), min(h, y2+fh*2.15)), radius=max(10, int(fw*.15)), fill=255)
    else:
        _ellipse(d, (x1, y1, x2, y2))
    return mask.filter(ImageFilter.GaussianBlur(radius=max(4, int(min(fw, fh)*.035))))


def _prompt(effect: str, strength: float) -> tuple[str, str]:
    intensity = "subtle" if strength < .4 else "natural" if strength < .75 else "pronounced but photorealistic"
    base = "same person, preserve identity and facial geometry, photorealistic portrait, realistic skin texture, same lighting, same camera, high detail"
    prompts = {
        "older": f"{base}, {intensity} older appearance, natural age progression, realistic wrinkles and mature features",
        "younger": f"{base}, {intensity} younger adult appearance, youthful skin, preserve identity",
        "hair": f"{base}, {intensity} realistic attractive hairstyle, individual natural hair strands, unchanged face",
        "hairstyle": f"{base}, {intensity} realistic attractive hairstyle, natural hair strands, unchanged face",
        "reference_hair": f"{base}, hairstyle inspired by the reference image, transfer only hairstyle and hair appearance, unchanged face",
        "hair_color": f"{base}, {intensity} professionally recolored hair, realistic roots and highlights, unchanged face",
        "beard": f"{base}, {intensity} natural realistic beard and facial hair",
        "remove_beard": f"{base}, clean shaven face, remove beard and moustache naturally, preserve skin texture",
        "smile": f"{base}, {intensity} natural friendly smile, realistic mouth and teeth",
        "teeth_whiten": f"{base}, naturally cleaner slightly whiter teeth, preserve tooth shape, no artificial glow",
        "eye_color": f"{base}, natural realistic eye color change, preserve iris structure and eye shape",
        "makeup": f"{base}, {intensity} tasteful professional makeup, realistic cosmetics, preserve identity",
        "skin_smooth": f"{base}, subtle skin cleanup, reduce temporary blemishes while preserving pores and natural texture",
        "pose": f"{base}, subtle portrait pose adjustment, preserve identity",
        "relight": f"{base}, professional studio portrait lighting, balanced face illumination, preserve skin tone and identity",
        "blemish_fix": f"{base}, remove temporary blemishes and minor skin distractions, preserve pores, freckles and natural texture",
        "eye_enhance": f"{base}, clearer natural eyes, subtle catchlights, realistic iris detail, preserve eye shape and identity",
        "headshot": f"{base}, professional clean headshot, refined portrait presentation, realistic studio lighting, unchanged identity",
        "background_studio": f"{base}, clean neutral professional studio background, realistic depth separation, unchanged person",
    }
    negative = "different person, identity change, altered facial proportions, deformed face, distorted eyes, extra teeth, duplicate face, plastic skin, cartoon, painting, blurry, low quality"
    return prompts.get(effect, base), negative


def run_diffusion_edit(src: Path, effect: str, strength: float, work: Path, reference: Optional[Path] = None) -> Path:
    import torch
    use_reference = reference is not None and effect in {"hair", "hairstyle", "reference_hair"}
    pipe = _load_pipeline(with_reference=use_reference)
    source = Image.open(src).convert("RGB")
    original_size = source.size
    max_side = 768 if torch.cuda.is_available() else 512
    scale = min(1.0, max_side / max(source.size))
    size = (max(64, int(source.width*scale)//8*8), max(64, int(source.height*scale)//8*8))
    image = source.resize(size, Image.Resampling.LANCZOS)
    mask = _mask_for_effect(source, "reference_hair" if use_reference else effect).resize(size, Image.Resampling.LANCZOS)
    prompt, negative = _prompt("reference_hair" if use_reference else effect, strength)
    device = "cuda" if torch.cuda.is_available() else "cpu"
    generator = torch.Generator(device=device).manual_seed(int(os.getenv("FACE_AI_SEED", "424242")))
    steps = max(10, min(50, int(os.getenv("FACE_AI_STEPS", "24"))))
    guidance = max(1.0, min(15.0, float(os.getenv("FACE_AI_GUIDANCE", "7.0"))))
    kwargs = dict(prompt=prompt, negative_prompt=negative, image=image, mask_image=mask,
                  num_inference_steps=steps, guidance_scale=guidance, generator=generator,
                  strength=max(.20, min(.95, .30 + strength*.55)))
    if use_reference:
        ref = Image.open(reference).convert("RGB")
        kwargs["ip_adapter_image"] = ref
    result = pipe(**kwargs).images[0].resize(original_size, Image.Resampling.LANCZOS)
    out = work / "result.jpg"
    result.save(out, "JPEG", quality=95)
    return out


def diffusion_status():
    enabled = os.getenv("ENABLE_GENERATIVE", "0") == "1"
    return {
        "ready": enabled,
        "enabled": enabled,
        "fee": "none/self-hosted",
        "engine": "Diffusers inpainting",
        "model": os.getenv("FACE_DIFFUSION_MODEL", DEFAULT_MODEL),
        "reference_image_ready": enabled and os.getenv("ENABLE_IP_ADAPTER", "0") == "1",
        "reference_engine": "IP-Adapter" if os.getenv("ENABLE_IP_ADAPTER", "0") == "1" else "disabled",
        "license_note": "Diffusers code is Apache-2.0. Model and adapter weights have their own licenses and must be audited before redistribution/commercial release.",
    }


def run_wardrobe_diffusion(src: Path, wardrobe_prompt: str, strength: float, work: Path, garment: Optional[Path] = None) -> Path:
    import torch
    use_reference = garment is not None
    pipe = _load_pipeline(with_reference=use_reference)
    source = Image.open(src).convert("RGB")
    original_size = source.size
    max_side = 768 if torch.cuda.is_available() else 512
    scale = min(1.0, max_side / max(source.size))
    size = (max(64, int(source.width*scale)//8*8), max(64, int(source.height*scale)//8*8))
    image = source.resize(size, Image.Resampling.LANCZOS)
    mask = _mask_for_effect(source, "wardrobe").resize(size, Image.Resampling.LANCZOS)
    prompt = f"same person, preserve face identity, hair, body proportions and pose, photorealistic full-body portrait, replace only clothing with {wardrobe_prompt}, realistic fabric drape, seams and shadows, coherent lighting, high detail"
    negative = "different person, changed face, changed body shape, nudity, underwear, deformed body, extra limbs, duplicate person, malformed hands, fused clothing, floating fabric, text, logo, cartoon, painting, blurry, low quality"
    device = "cuda" if torch.cuda.is_available() else "cpu"
    generator = torch.Generator(device=device).manual_seed(int(os.getenv("FACE_AI_SEED", "424242")))
    steps = max(12, min(50, int(os.getenv("FACE_AI_STEPS", "26"))))
    guidance = max(1.0, min(15.0, float(os.getenv("FACE_AI_GUIDANCE", "7.0"))))
    kwargs = dict(prompt=prompt, negative_prompt=negative, image=image, mask_image=mask, num_inference_steps=steps, guidance_scale=guidance, generator=generator, strength=max(.28, min(.95, .38+strength*.48)))
    if use_reference:
        ref = Image.open(garment).convert("RGB")
        kwargs["ip_adapter_image"] = ref
    result = pipe(**kwargs).images[0].resize(original_size, Image.Resampling.LANCZOS)
    out = work / "wardrobe-result.jpg"
    result.save(out, "JPEG", quality=95)
    return out
