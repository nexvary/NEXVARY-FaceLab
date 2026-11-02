import json, os, sys
from pathlib import Path
from providers.heritage import HERITAGE_SITES

root = Path(os.getenv("FACELAB_HERITAGE_DIR", Path(__file__).parent / "heritage_backgrounds"))
manifest = root / "licenses.json"
if not manifest.exists():
    print(f"LICENSE MANIFEST MISSING: {manifest}")
    sys.exit(2)
data = json.loads(manifest.read_text(encoding="utf-8"))
items = {x.get("site_id"): x for x in data.get("assets", [])}
errors=[]
for site_id in HERITAGE_SITES:
    files=[p for ext in (".jpg",".jpeg",".png",".webp") if (p:=root/f"{site_id}{ext}").exists()]
    if not files: continue
    rec=items.get(site_id)
    if not rec: errors.append(f"{site_id}: image exists but license entry is missing"); continue
    for field in ("source","author","license","commercial_use"):
        if field not in rec: errors.append(f"{site_id}: missing {field}")
    if rec.get("commercial_use") is not True: errors.append(f"{site_id}: commercial_use must be true for release")
print(f"Heritage images installed: {sum(any((root/f'{sid}{ext}').exists() for ext in ('.jpg','.jpeg','.png','.webp')) for sid in HERITAGE_SITES)} / {len(HERITAGE_SITES)}")
if errors:
    print("\n".join(errors)); sys.exit(1)
print("Heritage license validation: PASS")
