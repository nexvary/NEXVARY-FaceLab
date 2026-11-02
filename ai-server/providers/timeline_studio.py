from __future__ import annotations

import json, time, shutil
from pathlib import Path
from typing import Any

def build_multitrack(plan: dict[str, Any]) -> dict[str, Any]:
    cursor=0.0; video=[]; motion=[]; audio=[]; subtitles=[]
    for i,s in enumerate(plan.get("shots", [])):
        dur=float(s.get("duration_seconds",5) or 5); start=cursor; end=cursor+dur
        base={"shot_id":s.get("id",f"shot-{i+1}"),"index":i,"start":round(start,3),"end":round(end,3),"duration":dur,"enabled":bool(s.get("enabled",True))}
        video.append({**base,"camera":s.get("camera_preset") or s.get("camera") or "default","transition":s.get("transition","cut"),"prompt":s.get("prompt","")})
        motion.append({**base,"motion_preset":s.get("motion_preset","") or "idle_natural","driving_motion_id":s.get("driving_motion_id","") or None,"engine":s.get("preferred_engine","") or None})
        if s.get("dialogue") or s.get("narration"):
            audio.append({**base,"dialogue":s.get("dialogue","") or "","narration":s.get("narration","") or "","duck_music":bool(s.get("dialogue") or s.get("narration"))})
        if s.get("dialogue") or s.get("narration"):
            subtitles.append({**base,"text":s.get("dialogue") or s.get("narration") or ""})
        cursor=end
    return {"duration_seconds":round(cursor,3),"tracks":{"video":video,"motion":motion,"audio":audio,"subtitles":subtitles},"track_count":4,"shot_count":len(video)}

def render_queue(job_root: Path) -> dict[str, Any]:
    rows=[]
    if not job_root.exists(): return {"queue":[],"counts":{}}
    for d in job_root.iterdir():
        if not d.is_dir(): continue
        f=d/'job.json'
        if not f.exists(): continue
        try: j=json.loads(f.read_text(encoding='utf-8'))
        except Exception: continue
        rows.append({k:j.get(k) for k in ('job_id','state','progress','created_at','current_shot','rendered_shots','shot_count','preset','render_mode')})
    rows.sort(key=lambda x: float(x.get('created_at') or 0), reverse=True)
    counts={}
    for r in rows: counts[r.get('state') or 'unknown']=counts.get(r.get('state') or 'unknown',0)+1
    return {"queue":rows[:100],"counts":counts,"active":sum(counts.get(x,0) for x in ('queued','running','cancelling'))}

def snapshot(job_root: Path, job_id: str) -> dict[str, Any]:
    d=job_root/job_id
    if not d.exists(): raise FileNotFoundError(job_id)
    plan=d/'plan.json'; job=d/'job.json'
    if not plan.exists() or not job.exists(): raise FileNotFoundError(job_id)
    sd=d/'snapshots'; sd.mkdir(exist_ok=True)
    stamp=time.strftime('%Y%m%d-%H%M%S')
    payload={"snapshot_id":stamp,"created_at":time.time(),"plan":json.loads(plan.read_text(encoding='utf-8')),"job":json.loads(job.read_text(encoding='utf-8'))}
    out=sd/f'{stamp}.json'; out.write_text(json.dumps(payload,ensure_ascii=False,indent=2),encoding='utf-8')
    return {"snapshot_id":stamp,"path":out.name,"created_at":payload['created_at']}

def list_snapshots(job_root: Path, job_id: str) -> list[dict[str,Any]]:
    sd=job_root/job_id/'snapshots'
    if not sd.exists(): return []
    out=[]
    for f in sorted(sd.glob('*.json'), reverse=True):
        try:
            x=json.loads(f.read_text(encoding='utf-8')); out.append({"snapshot_id":x.get('snapshot_id',f.stem),"created_at":x.get('created_at'),"file":f.name})
        except Exception: pass
    return out[:50]

def restore(job_root: Path, job_id: str, snapshot_id: str) -> dict[str,Any]:
    d=job_root/job_id; f=d/'snapshots'/f'{snapshot_id}.json'
    if not f.exists(): raise FileNotFoundError(snapshot_id)
    x=json.loads(f.read_text(encoding='utf-8'))
    (d/'plan.json').write_text(json.dumps(x['plan'],ensure_ascii=False,indent=2),encoding='utf-8')
    j=x.get('job',{}); current=json.loads((d/'job.json').read_text(encoding='utf-8'))
    for k in ('story','duration_seconds','preset','aspect_ratio','export_preset','motion_preset','shot_count'): current[k]=j.get(k,current.get(k))
    current['restored_snapshot']=snapshot_id; current['updated_at']=time.time()
    (d/'job.json').write_text(json.dumps(current,ensure_ascii=False,indent=2),encoding='utf-8')
    return {"restored":True,"snapshot_id":snapshot_id,"plan":x['plan']}
