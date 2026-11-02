"""Safe adapter helper for local third-party video engines.
It validates paths and launches argv without invoking a command shell. Configure the actual repo command in environment JSON.
"""
import argparse, json, subprocess
from pathlib import Path

ap=argparse.ArgumentParser()
ap.add_argument('--command-json', required=True)
ap.add_argument('--input', required=True)
ap.add_argument('--output', required=True)
ap.add_argument('--prompt', default='')
ap.add_argument('--duration', type=int, default=5)
ap.add_argument('--seed', type=int, default=1)
a=ap.parse_args()
cmd=json.loads(a.command_json)
if not isinstance(cmd,list) or not cmd: raise SystemExit('invalid command-json')
Path(a.input).resolve(strict=True)
out=Path(a.output).resolve(); out.parent.mkdir(parents=True,exist_ok=True)
m={'input':str(Path(a.input).resolve()),'output':str(out),'prompt':a.prompt,'duration':str(max(1,min(a.duration,30))),'seed':str(a.seed)}
argv=[str(x).format(**m) for x in cmd]
r=subprocess.run(argv, check=False)
if r.returncode: raise SystemExit(r.returncode)
if not out.exists() or out.stat().st_size==0: raise SystemExit('engine produced no output')
