"""Pre-publication scan for common credential formats and forbidden binary payloads.
This is a bounded pattern scan, not a guarantee that all secrets have been detected.
"""
import json
import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PATTERNS = [
    re.compile(r'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----'),
    re.compile(r'\b(?:ghp|gho|ghu|ghs|github_pat)_[A-Za-z0-9_]{30,}\b'),
    re.compile(r'\bAKIA[A-Z0-9]{16}\b'),
    re.compile(r'\bsk-(?:proj-)?[A-Za-z0-9_-]{40,}\b'),
]
FORBIDDEN = {'.jks', '.keystore', '.p12', '.pem', '.pt', '.pth', '.onnx', '.tflite', '.safetensors', '.apk', '.aab'}
paths = subprocess.check_output(['git', 'ls-files', '--cached', '--others', '--exclude-standard', '-z'], cwd=ROOT).decode().split('\0')
findings = []
checked = 0
for rel in sorted(set(filter(None, paths))):
    path = ROOT / rel
    if path.suffix.lower() in FORBIDDEN or path.name in {'.env', 'local.properties'}:
        findings.append({'path': rel, 'reason': 'sensitive file or unaudited distributable'})
    try:
        content = path.read_text(encoding='utf-8')
    except UnicodeDecodeError:
        continue
    checked += 1
    if any(pattern.search(content) for pattern in PATTERNS):
        findings.append({'path': rel, 'reason': 'credential pattern; value intentionally redacted'})
print(json.dumps({'text_files_checked': checked, 'findings': findings}, indent=2))
raise SystemExit(bool(findings))
