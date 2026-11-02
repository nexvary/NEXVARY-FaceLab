# NEXVARY FaceLab — Stage 800

Stage 800 is a cumulative engineering release that advances the Stage 250 baseline without introducing any paid per-image API dependency.

## Development checkpoints 300 → 800
- **300:** editor hardening, multilingual/RTL preservation, persistent gallery and draggable before/after retained and verified.
- **400:** self-hosted runtime diagnostics expanded and Android client metadata standardized.
- **500:** server storage diagnostics added through `/v1/storage` and system diagnostics through `/v1/system`.
- **600:** movable AI data root (`FACELAB_DATA_ROOT`) plus Windows/Linux storage configuration helpers for large model disks.
- **700:** optional API-token transport wired from Android BuildConfig to `X-FaceLab-Token`; client version header added.
- **800:** five new portrait tools integrated end-to-end: Portrait Relight, Blemish Cleanup, Eye Enhance, Professional Headshot and Studio Background. Diffusion masks/prompts and server effect registry were extended accordingly.

## AI architecture
Android remains a lightweight client. Heavy generative editing is self-hosted. No OpenAI/Replicate/Stability paid endpoint was added. Model weights remain subject to their own license audit before redistribution/commercial release.

## New server endpoints
- `GET /v1/system`
- `GET /v1/storage`

Existing endpoints retained:
- `/health`
- `/v1/capabilities`
- `/v1/presets`
- `/v1/license-report`
- `/v1/runtime`
- `/v1/models`
- `/v1/edit`

## New AI storage layout
Set `FACELAB_DATA_ROOT` to a large disk. `configure_storage_windows.bat` also configures Hugging Face and Torch caches to that disk.

## Version
Android: `800.0.0` / versionCode `800`
AI server: `800.0.0`
