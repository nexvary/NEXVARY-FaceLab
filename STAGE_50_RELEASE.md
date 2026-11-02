# NEXVARY FaceLab — Stage 50

Stage 50 changes the face editor from placeholder adapters to a real self-hosted generative execution path.

1. Added lazy Diffusers inpainting provider.
2. Added face localization with OpenCV's bundled Haar cascade.
3. Added effect-specific masks for age, hair, beard, smile and pose.
4. Added identity-preservation prompts and negative prompts.
5. Added GPU/CPU auto-selection.
6. Added deterministic seed support (`FACE_AI_SEED`).
7. Added configurable steps/guidance.
8. Added configurable model ID (`FACE_DIFFUSION_MODEL`).
9. Added generative dependency installer scripts.
10. Added `/v1/models` and generative readiness reporting.
11. Kept enhancement available without any generative dependency.
12. Removed the Stage-40 default routing to research-only SAM/HyperStyle adapters.
13. Added explicit license gate documentation.
14. No paid external API is required.
15. API cache and upload protections remain enabled.

The first model download is large and occurs on the machine running the AI server. Subsequent requests use the local Hugging Face cache.
