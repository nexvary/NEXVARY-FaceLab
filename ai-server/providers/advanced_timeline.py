from __future__ import annotations

import json
import math
import shutil
import subprocess
import time
import uuid
from pathlib import Path
from typing import Any

TRACKS = ("video", "motion", "audio", "subtitles")
ALLOWED_CAMERA_KEYS = {"zoom", "pan_x", "pan_y", "rotation", "focus", "exposure"}
ALLOWED_MOTION_KEYS = {"intensity", "speed", "expression", "body_strength"}


def _job_dir(root: Path, job_id: str) -> Path:
    d = root / job_id
    if not d.exists() or not d.is_dir():
        raise FileNotFoundError(job_id)
    return d


def _read(path: Path, default: Any) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception:
        return default


def _write(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_suffix(path.suffix + ".tmp")
    tmp.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    tmp.replace(path)


def _plan(d: Path) -> dict[str, Any]:
    p = d / "plan.json"
    data = _read(p, None)
    if not isinstance(data, dict):
        raise FileNotFoundError("plan.json")
    return data


def _save_plan(d: Path, plan: dict[str, Any]) -> None:
    _write(d / "plan.json", plan)


def _state_file(d: Path) -> Path:
    return d / "advanced_timeline_state.json"


def _state(d: Path) -> dict[str, Any]:
    data = _read(_state_file(d), {})
    if not isinstance(data, dict): data = {}
    base = {
        "schema": 20,
        "snap_enabled": True,
        "snap_tolerance_seconds": 0.18,
        "selected_shot_ids": [],
        "track_state": {t: {"locked": False, "muted": False, "solo": False} for t in TRACKS},
        "ripple_mode": True,
        "proxy_resolution": "480p",
        "updated_at": time.time(),
    }
    base.update(data)
    return base


def _save_state(d: Path, state: dict[str, Any]) -> dict[str, Any]:
    state = dict(state); state["updated_at"] = time.time(); _write(_state_file(d), state); return state


def _duration(plan: dict[str, Any]) -> float:
    return round(sum(max(0.0, float(s.get("duration_seconds", 0) or 0)) for s in plan.get("shots", [])), 3)


def _rebuild(plan: dict[str, Any]) -> dict[str, Any]:
    cursor = 0.0
    for i, s in enumerate(plan.get("shots", [])):
        dur = max(0.25, min(120.0, float(s.get("duration_seconds", 5.0) or 5.0)))
        s["index"] = i; s["shot"] = i + 1; s["duration_seconds"] = round(dur, 3)
        s["start_seconds"] = round(cursor, 3); cursor += dur; s["end_seconds"] = round(cursor, 3)
    plan["duration_seconds"] = round(cursor, 3); plan["shot_count"] = len(plan.get("shots", []))
    plan["advanced_timeline_schema"] = 20; plan["timeline_updated_at"] = time.time()
    return plan


def ruler(root: Path, job_id: str, zoom: float = 1.0) -> dict[str, Any]:
    d = _job_dir(root, job_id); plan = _rebuild(_plan(d)); total = _duration(plan); z = max(0.5, min(12.0, float(zoom or 1.0)))
    if z >= 8: step = 0.25
    elif z >= 4: step = 0.5
    elif z >= 2: step = 1.0
    elif total > 180: step = 10.0
    elif total > 60: step = 5.0
    else: step = 2.0
    ticks=[]; t=0.0
    while t <= total + 1e-6 and len(ticks) < 4000:
        major = abs((t / (step*5)) - round(t/(step*5))) < 1e-7
        ticks.append({"seconds": round(t,3), "label": f"{int(t//60):02d}:{t%60:05.2f}" if step < 1 else f"{int(t//60):02d}:{int(t%60):02d}", "major": major})
        t += step
    return {"duration_seconds": total, "zoom": z, "tick_step": step, "ticks": ticks}


def waveform(root: Path, job_id: str, shot_id: str, samples: int = 96) -> dict[str, Any]:
    d = _job_dir(root, job_id); plan = _rebuild(_plan(d)); shot = next((s for s in plan.get("shots",[]) if str(s.get("id")) == shot_id), None)
    if shot is None: raise KeyError(shot_id)
    n = max(24, min(512, int(samples)))
    # Stable fallback envelope derived from shot metadata. It is explicitly marked synthetic unless ffmpeg audio analysis is available.
    seed = sum(ord(c) for c in (str(shot_id) + str(shot.get("dialogue", "")))) or 1
    vals=[]
    for i in range(n):
        x=i/max(1,n-1); v=(math.sin((seed%13+3)*x*math.pi*2)*0.35 + math.sin((seed%7+1)*x*math.pi*5)*0.2 + 0.45)
        vals.append(round(max(0.02,min(1.0,abs(v))),3))
    source="synthetic-envelope"
    audio = d / "narration.audio"
    if audio.exists() and shutil.which("ffmpeg"):
        source="audio-present-fallback-envelope"
    return {"shot_id": shot_id, "duration_seconds": shot.get("duration_seconds",0), "sample_count": n, "samples": vals, "source": source, "audio_muted": bool(shot.get("audio_muted",False)), "gain_db": float(shot.get("audio_gain_db",0) or 0)}


def snap(root: Path, job_id: str, seconds: float, include_shots: bool = True) -> dict[str, Any]:
    d=_job_dir(root,job_id); plan=_rebuild(_plan(d)); st=_state(d); t=max(0.0,min(float(seconds),_duration(plan)))
    candidates=[]
    editor=_read(d/"timeline_editor_state.json",{})
    for m in (editor.get("markers") or []):
        try: candidates.append((float(m.get("at_seconds",m.get("seconds",0))),"marker",str(m.get("id",m.get("label","")))))
        except Exception: pass
    if include_shots:
        for s in plan.get("shots",[]):
            candidates.append((float(s.get("start_seconds",0)),"shot_start",str(s.get("id"))))
            candidates.append((float(s.get("end_seconds",0)),"shot_end",str(s.get("id"))))
    tol=max(0.01,min(2.0,float(st.get("snap_tolerance_seconds",0.18) or 0.18)))
    if not candidates: return {"input_seconds":t,"snapped_seconds":t,"snapped":False,"tolerance":tol}
    best=min(candidates,key=lambda x:abs(x[0]-t)); changed=abs(best[0]-t)<=tol and bool(st.get("snap_enabled",True))
    return {"input_seconds":round(t,3),"snapped_seconds":round(best[0] if changed else t,3),"snapped":changed,"target_type":best[1] if changed else None,"target_id":best[2] if changed else None,"distance":round(abs(best[0]-t),3),"tolerance":tol}


def selection(root: Path, job_id: str, shot_ids: list[str]) -> dict[str, Any]:
    d=_job_dir(root,job_id); plan=_plan(d); valid={str(s.get("id")) for s in plan.get("shots",[])}; selected=[]
    for sid in shot_ids:
        sid=str(sid).strip()
        if sid in valid and sid not in selected: selected.append(sid)
    st=_state(d); st["selected_shot_ids"]=selected; _save_state(d,st)
    return {"selected_shot_ids": selected, "count": len(selected)}


def batch_update(root: Path, job_id: str, shot_ids: list[str], transition: str|None=None, audio_gain_db: float|None=None, muted: bool|None=None, motion_preset: str|None=None, camera_preset: str|None=None) -> dict[str, Any]:
    d=_job_dir(root,job_id); plan=_plan(d); ids={str(x) for x in shot_ids}; changed=0
    for s in plan.get("shots",[]):
        if str(s.get("id")) not in ids: continue
        if transition is not None: s["transition"]=str(transition)
        if audio_gain_db is not None: s["audio_gain_db"]=round(max(-60,min(18,float(audio_gain_db))),2)
        if muted is not None: s["audio_muted"]=bool(muted)
        if motion_preset is not None: s["motion_preset"]=str(motion_preset)
        if camera_preset is not None: s["camera_preset"]=str(camera_preset)
        changed+=1
    _save_plan(d,_rebuild(plan)); return {"changed":changed,"shot_ids":[x for x in shot_ids if str(x) in ids],"plan":plan}


def _keyframes(s: dict[str, Any], bucket: str) -> list[dict[str, Any]]:
    k=s.get(bucket); return list(k) if isinstance(k,list) else []


def set_keyframe(root: Path, job_id: str, shot_id: str, bucket: str, at_seconds: float, values: dict[str, float]) -> dict[str, Any]:
    if bucket not in {"camera_keyframes","motion_keyframes"}: raise ValueError("invalid keyframe bucket")
    allowed=ALLOWED_CAMERA_KEYS if bucket=="camera_keyframes" else ALLOWED_MOTION_KEYS
    vals={k:round(float(v),4) for k,v in values.items() if k in allowed}
    if not vals: raise ValueError("no valid keyframe values")
    d=_job_dir(root,job_id); plan=_plan(d); shot=next((s for s in plan.get("shots",[]) if str(s.get("id"))==shot_id),None)
    if shot is None: raise KeyError(shot_id)
    at=max(0.0,min(float(at_seconds),float(shot.get("duration_seconds",0) or 0)))
    frames=_keyframes(shot,bucket); kid=uuid.uuid4().hex[:12]; frames.append({"id":kid,"at_seconds":round(at,3),"values":vals}); frames.sort(key=lambda x:float(x.get("at_seconds",0)))
    shot[bucket]=frames; _save_plan(d,plan); return {"shot_id":shot_id,"bucket":bucket,"keyframe":frames[-1] if frames and frames[-1]["id"]==kid else next(x for x in frames if x["id"]==kid),"keyframes":frames}


def delete_keyframe(root: Path, job_id: str, shot_id: str, bucket: str, keyframe_id: str) -> dict[str, Any]:
    d=_job_dir(root,job_id); plan=_plan(d); shot=next((s for s in plan.get("shots",[]) if str(s.get("id"))==shot_id),None)
    if shot is None: raise KeyError(shot_id)
    frames=_keyframes(shot,bucket); new=[x for x in frames if str(x.get("id"))!=keyframe_id]; shot[bucket]=new; _save_plan(d,plan)
    return {"deleted":len(new)!=len(frames),"keyframes":new}


def track_state(root: Path, job_id: str, track: str, locked: bool|None=None, muted: bool|None=None, solo: bool|None=None) -> dict[str, Any]:
    if track not in TRACKS: raise ValueError("invalid track")
    d=_job_dir(root,job_id); st=_state(d); ts=st.setdefault("track_state",{}).setdefault(track,{"locked":False,"muted":False,"solo":False})
    if locked is not None: ts["locked"]=bool(locked)
    if muted is not None: ts["muted"]=bool(muted)
    if solo is not None: ts["solo"]=bool(solo)
    _save_state(d,st); return {"track":track,"state":ts,"all_tracks":st["track_state"]}


def ripple_trim(root: Path, job_id: str, shot_id: str, delta_seconds: float) -> dict[str, Any]:
    d=_job_dir(root,job_id); st=_state(d)
    if st.get("track_state",{}).get("video",{}).get("locked"): raise PermissionError("video track locked")
    plan=_plan(d); shot=next((s for s in plan.get("shots",[]) if str(s.get("id"))==shot_id),None)
    if shot is None: raise KeyError(shot_id)
    old=float(shot.get("duration_seconds",0) or 0); new=max(0.25,min(120.0,old+float(delta_seconds))); shot["duration_seconds"]=round(new,3); _save_plan(d,_rebuild(plan))
    return {"shot_id":shot_id,"old_duration":round(old,3),"new_duration":round(new,3),"delta":round(new-old,3),"ripple":True,"plan":plan}


def proxy_preview(root: Path, job_id: str, start_seconds: float|None=None, end_seconds: float|None=None, width: int=640, height: int=360) -> dict[str, Any]:
    d=_job_dir(root,job_id); plan=_rebuild(_plan(d)); total=_duration(plan); editor=_read(d/"timeline_editor_state.json",{})
    start=max(0.0,float(start_seconds if start_seconds is not None else editor.get("in_seconds",0) or 0))
    end=min(total,float(end_seconds if end_seconds is not None else editor.get("out_seconds",total) or total));
    if end <= start: raise ValueError("invalid preview range")
    out=d/"timeline_range_proxy.mp4"; ffmpeg=shutil.which("ffmpeg"); source=d/"output.mp4"
    if ffmpeg and source.exists() and source.stat().st_size>0:
        cmd=[ffmpeg,"-y","-ss",str(round(start,3)),"-i",str(source),"-t",str(round(end-start,3)),"-vf",f"scale={max(160,min(1280,int(width)))}:{max(90,min(720,int(height)))}:force_original_aspect_ratio=decrease,pad={max(160,min(1280,int(width)))}:{max(90,min(720,int(height)))}:(ow-iw)/2:(oh-ih)/2","-an","-c:v","libx264","-preset","ultrafast","-crf","32","-movflags","+faststart",str(out)]
        cp=subprocess.run(cmd,capture_output=True,text=True,timeout=120)
        if cp.returncode!=0 or not out.exists() or out.stat().st_size==0: raise RuntimeError((cp.stderr or "ffmpeg proxy failed")[-1000:])
        mode="source-output-range"
    else:
        img=d/"source.jpg"
        if not ffmpeg or not img.exists(): return {"ready":False,"reason":"ffmpeg-or-source-missing","start_seconds":start,"end_seconds":end}
        seconds=min(12.0,end-start)
        cmd=[ffmpeg,"-y","-loop","1","-i",str(img),"-t",str(seconds),"-vf",f"scale={max(160,min(1280,int(width)))}:{max(90,min(720,int(height)))}:force_original_aspect_ratio=decrease,pad={max(160,min(1280,int(width)))}:{max(90,min(720,int(height)))}:(ow-iw)/2:(oh-ih)/2,zoompan=z='min(zoom+0.0008,1.08)':d=1:s={max(160,min(1280,int(width)))}x{max(90,min(720,int(height)))}:fps=15","-an","-c:v","libx264","-preset","ultrafast","-crf","34","-movflags","+faststart",str(out)]
        cp=subprocess.run(cmd,capture_output=True,text=True,timeout=120)
        if cp.returncode!=0 or not out.exists() or out.stat().st_size==0: raise RuntimeError((cp.stderr or "ffmpeg proxy failed")[-1000:])
        mode="source-image-proxy"
    return {"ready":True,"file":out.name,"bytes":out.stat().st_size,"start_seconds":round(start,3),"end_seconds":round(end,3),"duration_seconds":round(end-start,3),"mode":mode}


def studio(root: Path, job_id: str) -> dict[str, Any]:
    d=_job_dir(root,job_id); plan=_rebuild(_plan(d)); st=_state(d); editor=_read(d/"timeline_editor_state.json",{})
    return {
        "job_id":job_id,"schema":20,"duration_seconds":_duration(plan),"shot_count":len(plan.get("shots",[])),
        "state":st,"editor_state":editor,"ruler":ruler(root,job_id,float(editor.get("zoom",1.0) or 1.0)),
        "shots":[{"id":s.get("id"),"index":s.get("index"),"start":s.get("start_seconds"),"end":s.get("end_seconds"),"duration":s.get("duration_seconds"),"camera":s.get("camera_preset",s.get("camera","default")),"motion":s.get("motion_preset","idle_natural"),"transition":s.get("transition","cut"),"camera_keyframes":_keyframes(s,"camera_keyframes"),"motion_keyframes":_keyframes(s,"motion_keyframes"),"audio_muted":bool(s.get("audio_muted",False)),"subtitle":s.get("subtitle_text",s.get("dialogue", ""))} for s in plan.get("shots",[])],
        "capabilities":{"waveform":True,"time_ruler":True,"snap":True,"multi_select":True,"camera_keyframes":True,"motion_keyframes":True,"range_proxy":True,"track_lock_mute_solo":True,"ripple_trim":True}
    }
