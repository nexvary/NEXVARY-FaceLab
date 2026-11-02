import shutil, tempfile
from pathlib import Path
root = Path(tempfile.gettempdir())
for p in root.glob("facelab_*"):
    if p.is_dir():
        shutil.rmtree(p, ignore_errors=True)
print("FaceLab temporary folders cleaned.")
