from pathlib import Path
import tempfile
from PIL import Image
from providers.runner import EFFECTS, run_effect, provider_status

required = {"older", "younger", "background_remove", "relight", "blemish_fix", "eye_enhance", "headshot", "background_studio"}
assert required.issubset(set(EFFECTS)), sorted(required - set(EFFECTS))
status = provider_status()
assert status["local_enhance"]["ready"] is True

with tempfile.TemporaryDirectory() as d:
    root = Path(d)
    src = root / "in.jpg"
    Image.new("RGB", (256, 256), (130, 110, 100)).save(src)
    out = run_effect(src, "enhance", 0.5, root)
    assert out.exists() and out.stat().st_size > 100
    with Image.open(out) as im:
        assert im.size == (256, 256)
print("FaceLab Stage 39500 Electric UI smoke test: PASS")
