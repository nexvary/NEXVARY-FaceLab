# NEXVARY FaceLab — Stage 3000

Stage 3000 expands Heritage & Landmarks Studio from 16 to 72 landmark definitions across 9 countries: Egypt, Türkiye, Algeria, Saudi Arabia, Morocco, Tunisia, Italy, Spain, and Greece.

## Added
- 72 landmark catalog entries.
- Country filtering, text search, and persistent favorites in Android UI.
- 72 bundled original illustrated preview thumbnails (not copied landmark photography).
- Local self-hosted Heritage Pack directory controlled by `FACELAB_HERITAGE_DIR`.
- `GET /v1/heritage/packs` to report installed licensed background assets.
- `GET /v1/heritage/background/{site_id}` to serve an installed licensed asset.
- License manifest template and `validate_heritage_pack.py` release gate.
- Restored/implemented missing `HeritageScreen` composable from Stage 2500 source.

## Licensing rule
No internet photo is bundled merely because it is discoverable online. Full-resolution photographic landmark assets must have source/license records and commercial-use approval in `licenses.json` before release.
