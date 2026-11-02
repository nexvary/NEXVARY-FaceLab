# NEXVARY FaceLab — Stage 100

Stage 100 is a quality/architecture milestone rather than a cosmetic stage number.

## Added / strengthened
- Self-hosted API remains the only required backend path; no per-request paid API dependency.
- API version 100.0.0 with upload byte + pixel validation.
- Image verification before inference to reject malformed or deceptive uploads.
- Concurrency gate to prevent multiple heavy diffusion jobs from exhausting one GPU.
- Configurable cache, TTL, maximum upload size, maximum pixel count, and worker concurrency.
- Optional `FACELAB_API_TOKEN` protection for deployments outside a trusted LAN.
- `/v1/presets` and `/v1/license-report` endpoints.
- Output normalization strips source EXIF/metadata before returning the result.
- Android client timeouts centralized and capabilities endpoint added.
- Stage 100 smoke test checks a real local enhancement output.

## Product quality policy
Every AI model is treated as two separate licensing items: source code and model weights. A model is not approved for commercial redistribution until both are cleared.

## Privacy
The server processes images in temporary working folders. The result cache can be disabled with `FACELAB_CACHE=0`; cache lifetime is controlled by `FACELAB_CACHE_TTL_HOURS`.
