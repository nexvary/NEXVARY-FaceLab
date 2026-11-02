# NEXVARY FaceLab — Stage 1500

Version: 1500.0.0

## Stage 1200 → 1500 development line
- Visual wardrobe catalog cards with category-specific presentation.
- Dedicated wardrobe shelves: Clothes, Full Outfits, Shoes, Glasses, Hats.
- Expanded accessories: glasses, hats, watches, scarves, shoes, bags and necklaces.
- Seven complete outfit packs with coordinated multi-piece prompts.
- AI Stylist occasion workflow: work, wedding, travel, photo shoot, sport, winter and evening.
- One-tap stylist suggestion updates the selected clothing category/style.
- Outfit prompts, occasion, color and accessory are transported to the self-hosted wardrobe endpoint and are included in cache identity.
- New self-hosted `/v1/wardrobe/stylist` endpoint; no external paid inference API.
- Saved looks now include occasion and outfit pack identity.
- Fail-fast Windows generative installer: stops immediately when pip/PyTorch installation fails instead of displaying false success.

## Important implementation note
The visual catalog currently uses polished local UI cards/icons rather than bundled copyrighted clothing photographs. The catalog architecture is ready for licensed or user-provided garment thumbnails later.

## Commercial release note
Exact third-party model/code weights used by any optional VTON engine must still be license-audited before redistribution or commercial release.
