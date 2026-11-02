#!/usr/bin/env bash
set -e
export ENABLE_GENERATIVE=1
export FACE_DIFFUSION_MODEL="${FACE_DIFFUSION_MODEL:-stable-diffusion-v1-5/stable-diffusion-inpainting}"
exec ./run_linux.sh
