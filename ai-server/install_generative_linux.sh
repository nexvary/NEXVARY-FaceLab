#!/usr/bin/env bash
set -e
[ -d .venv ] || python3 -m venv .venv
source .venv/bin/activate
python -m pip install --upgrade pip
pip install -r requirements.txt
pip install -r requirements-generative.txt
python - <<'PY'
try:
 import torch; print('Torch', torch.__version__, 'CUDA', torch.cuda.is_available())
except Exception:
 print('Install a PyTorch build suitable for this machine, then rerun.')
PY
