# NEXVARY FaceLab — AI license gate (Stage 50)

This build is designed around **self-hosted inference with no per-request API fee**.

## Default generative path
- Hugging Face `diffusers`: Apache-2.0 code.
- Default model ID: `stable-diffusion-v1-5/stable-diffusion-inpainting`.
- Model license: CreativeML OpenRAIL-M. It permits redistribution/commercial/service use subject to its use restrictions and license obligations. Read and ship the model license when distributing weights or a service based on them.
- OpenCV Haar face detector: used only to localize the edit mask; no cloud/API service.

## Enhancement path
- Built-in Pillow enhancement: always available.
- GFPGAN integration remains optional and self-hosted. Review the exact downloaded model files/dependencies before a commercial release.

## Deliberately not enabled by default
Research face stacks that inherit StyleGAN/InsightFace/research-only checkpoints are not the default commercial path. They may be evaluated in a lab build only after their **code, checkpoint, training-data-derived model, and dependency** licenses are separately cleared.

## Product rule
"Free API" in this project means **our own local HTTP API**, not a third-party free tier. There is no per-image vendor bill. Hardware/electricity/hosting remain normal operating costs.
