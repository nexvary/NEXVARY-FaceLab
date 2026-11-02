#!/usr/bin/env bash
set -euo pipefail
ROOT="${1:-}"
if [[ -z "$ROOT" ]]; then echo "Usage: ./configure_storage_linux.sh /mnt/ai/NEXVARY-AI"; exit 1; fi
mkdir -p "$ROOT/hf-cache" "$ROOT/torch-cache"
cat <<TXT
Add these lines to your shell profile or service environment:
export FACELAB_DATA_ROOT="$ROOT"
export HF_HOME="$ROOT/hf-cache"
export TORCH_HOME="$ROOT/torch-cache"
TXT
