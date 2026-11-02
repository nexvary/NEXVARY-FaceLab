# NEXVARY FaceLab — Stage 900

Stage 900 introduces the **AI Virtual Wardrobe** as a first-class product area while preserving the self-hosted/no-per-image-fee architecture.

## Virtual Wardrobe
- Dedicated bottom-navigation destination with `Checkroom` icon.
- 72 built-in looks across 8 categories: Formal, Casual, Sport, Summer, Winter, Traditional, Business, Evening.
- All wardrobe labels are available in Arabic, English, Turkish, Spanish, German, and Italian.
- Import a garment image from the phone and use it as a visual reference.
- Reuses the selected person photo from FaceLab Editor; wardrobe results are written back into the main result state and persistent gallery.
- Before/after, save/share, and gallery workflows remain compatible with wardrobe output.

## Self-hosted API
- `GET /v1/wardrobe/status`
- `GET /v1/wardrobe/catalog`
- `POST /v1/wardrobe/try-on`
- Default wardrobe path uses the existing local Diffusers inpainting engine and optional local IP-Adapter garment reference.
- Optional `FASHN_VTON_COMMAND` adapter allows wiring an audited local FASHN VTON installation without a paid cloud API.
- Wardrobe cache keys include person image, garment image, selected style, category, and strength.

## Licensing rule
Do not redistribute third-party VTON code or model weights until both the exact code license and exact weight license have been reviewed and approved for the intended commercial distribution. Stage 900 includes only an adapter interface; it does not bundle FASHN VTON weights.

## Version
Android: `900.0.0` / versionCode `900`
AI server: `900.0.0`
