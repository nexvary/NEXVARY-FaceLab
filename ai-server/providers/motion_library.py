import json, os, shutil, subprocess
from pathlib import Path
from typing import Any
from providers.driving_motions import catalog as base_catalog, resolve

def _ffmpeg():
    x=os.getenv("FACELAB_FFMPEG","").strip()
    if x and Path(x).is_file(): return x
    return shutil.which(x) if x else shutil.which("ffmpeg")

def _ffprobe():
    f=_ffmpeg()
    if f:
        p=Path(f); c=p.with_name("ffprobe"+p.suffix)
        if c.exists(): return str(c)
    return shutil.which("ffprobe")

def _probe(path: Path)->dict[str,Any]:
    exe=_ffprobe()
    if not exe or not path.is_file(): return {"duration_seconds":None,"width":None,"height":None,"fps":None}
    try:
        cp=subprocess.run([exe,"-v","error","-select_streams","v:0","-show_entries","stream=width,height,r_frame_rate:format=duration","-of","json",str(path)],capture_output=True,text=True,timeout=20)
        d=json.loads(cp.stdout or '{}'); s=(d.get('streams') or [{}])[0]; fr=str(s.get('r_frame_rate') or '0/1')
        a,b=(fr.split('/')+['1'])[:2]; fps=round(float(a)/max(float(b),1.0),2)
        return {"duration_seconds":round(float((d.get('format') or {}).get('duration') or 0),2),"width":s.get('width'),"height":s.get('height'),"fps":fps}
    except Exception: return {"duration_seconds":None,"width":None,"height":None,"fps":None}

def ensure_thumbnail(motion_id:str)->Path|None:
    p=resolve(motion_id)
    if not p or p.suffix.lower()=='.pkl': return None
    out=p.with_suffix('.thumb.jpg')
    if out.exists() and out.stat().st_size>0: return out
    exe=_ffmpeg()
    if not exe: return None
    try:
        cp=subprocess.run([exe,'-y','-ss','0.2','-i',str(p),'-frames:v','1','-vf','scale=480:-2',str(out)],capture_output=True,timeout=30)
        return out if cp.returncode==0 and out.exists() and out.stat().st_size>0 else None
    except Exception: return None

def visual_catalog()->dict[str,Any]:
    data=base_catalog(); items=[]
    for item in data.get('items',[]):
        y=dict(item); p=resolve(str(y.get('id','')))
        y.update(_probe(p) if p else {})
        y['thumbnail_ready']=bool(ensure_thumbnail(str(y.get('id','')))) if p else False
        y['preview_supported']=bool(p and p.suffix.lower()!='.pkl')
        y['recommended_for']=['face','portrait'] if y.get('engine_hint')=='liveportrait' else ['body','pose','walk'] if y.get('engine_hint')=='mimicmotion' else ['cinematic']
        items.append(y)
    return {**data,'schema_version':5,'visual_library':True,'items':items}
