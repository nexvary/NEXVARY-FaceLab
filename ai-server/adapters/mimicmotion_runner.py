from __future__ import annotations
import argparse, os, subprocess, sys, tempfile, yaml, shutil
from pathlib import Path

def main():
    ap=argparse.ArgumentParser(description='NEXVARY MimicMotion official inference adapter')
    ap.add_argument('--input',required=True); ap.add_argument('--driving',required=True); ap.add_argument('--output',required=True)
    ap.add_argument('--repo',default=os.getenv('FACELAB_MIMICMOTION_DIR','')); ap.add_argument('--frames',type=int,default=72)
    args=ap.parse_args(); repo=Path(args.repo).expanduser(); script=repo/'inference.py'
    if not script.exists(): raise SystemExit('MimicMotion inference.py not found')
    py=(repo/'.venv'/'Scripts'/'python.exe') if os.name=='nt' else (repo/'.venv'/'bin'/'python')
    python=str(py if py.exists() else Path(sys.executable)); target=Path(args.output).resolve(); target.parent.mkdir(parents=True,exist_ok=True)
    cfg={'base_model_path':os.getenv('FACELAB_MIMIC_SVD','stabilityai/stable-video-diffusion-img2vid-xt-1-1'),'ckpt_path':os.getenv('FACELAB_MIMIC_CKPT','models/MimicMotion_1-1.pth'),'test_case':[{'ref_video_path':str(Path(args.driving).resolve()),'ref_image_path':str(Path(args.input).resolve()),'num_frames':max(16,min(args.frames,72)),'resolution':int(os.getenv('FACELAB_MIMIC_RESOLUTION','576')),'frames_overlap':6,'num_inference_steps':25,'noise_aug_strength':0,'guidance_scale':2.0,'sample_stride':2,'fps':15,'seed':42}]}
    cfgp=target.parent/'mimicmotion_job.yaml'; cfgp.write_text(yaml.safe_dump(cfg,sort_keys=False),encoding='utf-8')
    before={x.resolve() for x in repo.rglob('*.mp4')}
    p=subprocess.run([python,str(script),'--inference_config',str(cfgp)],cwd=repo,stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True,timeout=int(os.getenv('FACELAB_VIDEO_ENGINE_TIMEOUT','3600')))
    if p.returncode: raise SystemExit((p.stderr or p.stdout)[-4000:])
    c=sorted([x for x in repo.rglob('*.mp4') if x.resolve() not in before],key=lambda x:x.stat().st_mtime,reverse=True)
    if not c: raise SystemExit('MimicMotion finished but no new MP4 was produced')
    shutil.copy2(c[0],target)
if __name__=='__main__': main()
