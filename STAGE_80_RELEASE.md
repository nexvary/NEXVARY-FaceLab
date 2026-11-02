# NEXVARY FaceLab — Stage 80

Stage 80 extends the self-hosted editing stack without adding a paid per-request API.

## Implemented between Stage 51 and 80

- Expanded the Android tool palette for age, beard add/remove, hair, hair color, smile, makeup, eye color, teeth, skin smoothing, background blur/removal and AI enhancement.
- Added an optional second image picker for a hairstyle reference.
- Added multipart reference-image upload from Android to `/v1/edit`.
- Added optional IP-Adapter integration behind `ENABLE_IP_ADAPTER=1` for reference-guided hairstyle editing.
- Expanded intelligent masks for hair, beard, mouth/teeth, eyes and skin regions.
- Added stronger identity-preservation prompts and negative prompts.
- Added optional `rembg` background removal and background blur provider.
- Added `/health`, `/v1/models` and `/v1/capabilities` Stage-80 reporting.
- Added in-app AI server health check.
- Added cache key support for reference images so different references cannot collide.
- Kept legacy research-only face/hair provider paths disabled in the commercial profile until weight licensing is verified.
- Updated Android version code/name to 80 / 80.0.0.
- Updated smoke test to cover the Stage-80 effect registry.

## Important license rule

"Open source" code does not automatically grant commercial rights to every pretrained weight downloaded by that code. FaceLab therefore keeps model selection configurable and records licensing notes in provider status. Before distributing a commercial build, audit the exact model IDs/checkpoints included in the deployment.
