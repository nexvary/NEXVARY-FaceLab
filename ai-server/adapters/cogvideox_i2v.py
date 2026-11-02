"""Optional local CogVideoX image-to-video adapter for NEXVARY FaceLab.

This file intentionally does NOT download or redistribute model weights. Set
FACELAB_COGVIDEOX_MODEL to a local, separately license-audited model directory.
The adapter is invoked through FACELAB_VIDEO_ENGINE_CMD_JSON.
"""
import argparse
import os
from pathlib import Path


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--image', required=True)
    ap.add_argument('--prompt', required=True)
    ap.add_argument('--seconds', type=int, default=6)
    ap.add_argument('--output', required=True)
    ap.add_argument('--seed', type=int, default=42)
    ap.add_argument('--aspect-ratio', default='16:9')
    args = ap.parse_args()
    model = os.getenv('FACELAB_COGVIDEOX_MODEL', '').strip()
    if not model or not Path(model).exists():
        raise SystemExit('FACELAB_COGVIDEOX_MODEL must point to an audited local model directory')
    try:
        import torch
        from PIL import Image
        from diffusers import CogVideoXImageToVideoPipeline
        from diffusers.utils import export_to_video
    except Exception as exc:
        raise SystemExit(f'Missing video dependencies: {exc}')
    dtype = torch.bfloat16 if torch.cuda.is_available() and torch.cuda.is_bf16_supported() else torch.float16
    pipe = CogVideoXImageToVideoPipeline.from_pretrained(model, torch_dtype=dtype)
    if torch.cuda.is_available():
        pipe.enable_model_cpu_offload()
    image = Image.open(args.image).convert('RGB')
    fps = 8
    frames = max(17, min(81, args.seconds * fps + 1))
    gen = torch.Generator(device='cpu').manual_seed(args.seed)
    result = pipe(image=image, prompt=args.prompt, num_frames=frames, generator=gen).frames[0]
    export_to_video(result, args.output, fps=fps)

if __name__ == '__main__':
    main()
