# NEXVARY FaceLab — Stage 2000

Version: `2000.0.0`

## Main additions

- Independent outfit composition: top + bottom + outerwear + shoes.
- AI Stylist now presents three local recommendations per occasion instead of one.
- New saved dressing-room collections persisted locally on Android.
- Existing visual wardrobe, favorites, saved looks, full outfit packs, colors and accessories retained.
- New self-hosted endpoint: `GET /v1/wardrobe/stylist/recommendations`.
- Wardrobe prompts include the independently selected pieces, so the server cache and generative engine distinguish complete compositions.
- No paid per-image inference API added.
- Android/API/client versions synchronized to `2000.0.0`.

## Notes

The project still uses a self-hosted AI architecture. External VTON integration remains optional and command-adapter based. Exact model/code/weight licenses must be approved before redistribution or commercial release.

A full Android APK compilation is not claimed unless Gradle dependencies are available in the build environment.
