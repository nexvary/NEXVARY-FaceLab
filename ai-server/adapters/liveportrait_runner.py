from __future__ import annotations
import argparse, os, shutil, subprocess, sys
from pathlib import Path

def main():
    ap=argparse.ArgumentParser(description='NEXVARY LivePortrait official CLI adapter')
    ap.add_argument('--input',required=True); ap.add_argument('--driving',required=True); ap.add_argument('--output',required=True)
    ap.add_argument('--repo',default=os.getenv('FACELAB_LIVEPORTRAIT_DIR',''))
    ap.add_argument('--driving-option',default=os.getenv('FACELAB_LIVEPORTRAIT_DRIVING_OPTION','expression-friendly'))
    ap.add_argument('--driving-multiplier',default=os.getenv('FACELAB_LIVEPORTRAIT_DRIVING_MULTIPLIER','1.0'))
    args=ap.parse_args(); repo=Path(args.repo).expanduser(); script=repo/'inference.py'
    if not script.exists(): raise SystemExit('LivePortrait inference.py not found. Set FACELAB_LIVEPORTRAIT_DIR.')
    py=(repo/'.venv'/'Scripts'/'python.exe') if os.name=='nt' else (repo/'.venv'/'bin'/'python')
    python=str(py if py.exists() else Path(sys.executable))
    target=Path(args.output).resolve(); target.parent.mkdir(parents=True,exist_ok=True)
    before={x.resolve() for x in repo.rglob('*.mp4')}
    cmd=[python,str(script),'-s',str(Path(args.input).resolve()),'-d',str(Path(args.driving).resolve()),'--driving_option',args.driving_option,'--driving_multiplier',str(args.driving_multiplier)]
    p=subprocess.run(cmd,cwd=repo,stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True,timeout=int(os.getenv('FACELAB_VIDEO_ENGINE_TIMEOUT','2400')))
    if p.returncode: raise SystemExit((p.stderr or p.stdout)[-4000:])
    candidates=[x for x in repo.rglob('*.mp4') if x.resolve() not in before]
    if not candidates: candidates=list(repo.rglob('*.mp4'))
    candidates=sorted(candidates,key=lambda x:x.stat().st_mtime,reverse=True)
    if not candidates: raise SystemExit('LivePortrait finished but no MP4 was produced')
    shutil.copy2(candidates[0],target)
if __name__=='__main__': main()
