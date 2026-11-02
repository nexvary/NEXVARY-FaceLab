from __future__ import annotations
import json, math, shutil, subprocess, time
from pathlib import Path
from typing import Any

SAFE_AREA_PRESETS = {
    "off": {"action": 1.0, "title": 1.0},
    "broadcast": {"action": 0.90, "title": 0.80},
    "social": {"action": 0.92, "title": 0.84},
    "cinema": {"action": 0.95, "title": 0.90},
}
WORKSPACE_PRESETS = {
    "edit": {"monitor": 0.40, "timeline": 0.45, "inspector": 0.15},
    "motion": {"monitor": 0.34, "timeline": 0.40, "inspector": 0.26},
    "audio": {"monitor": 0.30, "timeline": 0.50, "inspector": 0.20},
    "compact": {"monitor": 0.36, "timeline": 0.54, "inspector": 0.10},
}

def _job(root: Path, job_id: str) -> Path:
    d = root / job_id
    if not d.exists(): raise FileNotFoundError(job_id)
    return d

def _read(p: Path, default: Any):
    try: return json.loads(p.read_text(encoding="utf-8"))
    except Exception: return default

def _write(p: Path, value: Any):
    p.parent.mkdir(parents=True, exist_ok=True)
    t = p.with_suffix(p.suffix + ".tmp")
    t.write_text(json.dumps(value, ensure_ascii=False, indent=2), encoding="utf-8")
    t.replace(p)

def _plan(d: Path) -> dict:
    x = _read(d / "plan.json", None)
    if not isinstance(x, dict): raise FileNotFoundError("plan.json")
    return x

def _state(d: Path) -> dict:
    x = _read(d / "timeline33500_state.json", {})
    if not isinstance(x, dict): x = {}
    x.setdefault("fps", 24.0)
    x.setdefault("playhead_seconds", 0.0)
    x.setdefault("safe_area", {"preset": "broadcast", **SAFE_AREA_PRESETS["broadcast"]})
    x.setdefault("workspace", {"preset": "edit", **WORKSPACE_PRESETS["edit"]})
    x.setdefault("onion", {"enabled": False, "frames": 1, "opacity": 0.35})
    return x

def _duration(plan: dict) -> float:
    shots = plan.get("shots", []) or []
    if not shots: return 0.0
    return max(float(s.get("end_seconds", 0) or 0) for s in shots)

def _active_shot(plan: dict, t: float):
    for s in plan.get("shots", []) or []:
        if float(s.get("start_seconds", 0) or 0) <= t <= float(s.get("end_seconds", 0) or 0):
            return s
    return None

def smpte(seconds: float, fps: float = 24.0) -> str:
    fps_i = max(1, int(round(float(fps))))
    total_frames = max(0, int(round(float(seconds) * fps_i)))
    frames = total_frames % fps_i
    total_seconds = total_frames // fps_i
    sec = total_seconds % 60
    minute = (total_seconds // 60) % 60
    hour = total_seconds // 3600
    return f"{hour:02d}:{minute:02d}:{sec:02d}:{frames:02d}"

def frame_step(root: Path, job_id: str, direction: int, frames: int = 1, fps: float | None = None) -> dict:
    d = _job(root, job_id); plan = _plan(d); st = _state(d)
    actual_fps = max(1.0, min(120.0, float(fps or st.get("fps", 24.0) or 24.0)))
    delta = (1 if int(direction) >= 0 else -1) * max(1, min(120, int(frames))) / actual_fps
    total = _duration(plan)
    t = max(0.0, min(total, float(st.get("playhead_seconds", 0.0) or 0.0) + delta))
    st["fps"] = actual_fps; st["playhead_seconds"] = round(t, 6); _write(d / "timeline33500_state.json", st)
    shot = _active_shot(plan, t)
    return {"playhead_seconds": round(t, 6), "timecode": smpte(t, actual_fps), "fps": actual_fps, "active_shot_id": str(shot.get("id")) if shot else None}

def set_playhead(root: Path, job_id: str, seconds: float, fps: float | None = None) -> dict:
    d = _job(root, job_id); plan = _plan(d); st = _state(d); total = _duration(plan)
    actual_fps = max(1.0, min(120.0, float(fps or st.get("fps", 24.0) or 24.0)))
    t = max(0.0, min(total, float(seconds)))
    st["fps"] = actual_fps; st["playhead_seconds"] = round(t, 6); _write(d / "timeline33500_state.json", st)
    shot = _active_shot(plan, t)
    return {"playhead_seconds": round(t, 6), "timecode": smpte(t, actual_fps), "fps": actual_fps, "active_shot_id": str(shot.get("id")) if shot else None, "duration_seconds": round(total, 6)}

def safe_area(root: Path, job_id: str, preset: str) -> dict:
    d = _job(root, job_id); p = (preset or "broadcast").lower().strip()
    if p not in SAFE_AREA_PRESETS: raise ValueError("unsupported safe-area preset")
    st = _state(d); st["safe_area"] = {"preset": p, **SAFE_AREA_PRESETS[p], "updated_at": time.time()}; _write(d / "timeline33500_state.json", st)
    return st["safe_area"]

def onion(root: Path, job_id: str, enabled: bool, frames: int = 1, opacity: float = 0.35) -> dict:
    d = _job(root, job_id); st = _state(d)
    st["onion"] = {"enabled": bool(enabled), "frames": max(1, min(4, int(frames))), "opacity": round(max(0.05, min(0.90, float(opacity))), 2), "updated_at": time.time()}
    _write(d / "timeline33500_state.json", st); return st["onion"]

def workspace(root: Path, job_id: str, preset: str | None = None, monitor: float | None = None, timeline: float | None = None, inspector: float | None = None) -> dict:
    d = _job(root, job_id); st = _state(d)
    if preset:
        p = preset.lower().strip()
        if p not in WORKSPACE_PRESETS: raise ValueError("unsupported workspace preset")
        ws = {"preset": p, **WORKSPACE_PRESETS[p]}
    else:
        vals = [float(monitor or 0.40), float(timeline or 0.45), float(inspector or 0.15)]
        vals = [max(0.05, min(0.90, v)) for v in vals]; s = sum(vals) or 1.0
        ws = {"preset": "custom", "monitor": round(vals[0]/s, 4), "timeline": round(vals[1]/s, 4), "inspector": round(vals[2]/s, 4)}
    ws["updated_at"] = time.time(); st["workspace"] = ws; _write(d / "timeline33500_state.json", st); return ws

def monitor(root: Path, job_id: str) -> dict:
    d = _job(root, job_id); plan = _plan(d); st = _state(d); t = float(st.get("playhead_seconds", 0) or 0); fps = float(st.get("fps", 24) or 24)
    shot = _active_shot(plan, t); total = _duration(plan)
    adjacent = {"previous_shot_id": None, "next_shot_id": None}
    shots = plan.get("shots", []) or []
    if shot:
        try:
            i = next(i for i,s in enumerate(shots) if str(s.get("id")) == str(shot.get("id")))
            if i > 0: adjacent["previous_shot_id"] = str(shots[i-1].get("id"))
            if i + 1 < len(shots): adjacent["next_shot_id"] = str(shots[i+1].get("id"))
        except Exception: pass
    return {"stage":33500,"playhead_seconds":round(t,6),"timecode":smpte(t,fps),"fps":fps,"duration_seconds":round(total,6),"active_shot_id":str(shot.get("id")) if shot else None,"safe_area":st["safe_area"],"onion":st["onion"],"workspace":st["workspace"],"adjacent":adjacent,"frame_endpoint":f"/v1/film/jobs/{job_id}/timeline/monitor/frame"}

def render_monitor_frame(root: Path, job_id: str, out_path: Path, width: int = 960, height: int = 540) -> Path:
    d = _job(root, job_id); st = _state(d); t = max(0.0, float(st.get("playhead_seconds", 0) or 0))
    candidates = [d / "timeline_range_proxy.mp4", d / "video.mp4", d / "final.mp4"]
    src = next((p for p in candidates if p.exists() and p.stat().st_size > 0), None)
    if src is None:
        plan = _plan(d); shot = _active_shot(plan, t)
        if shot:
            p = d / "shots" / f"{shot.get('id')}.mp4"
            if p.exists() and p.stat().st_size > 0:
                src = p; t = max(0.0, t - float(shot.get("start_seconds", 0) or 0))
    if src is None: raise FileNotFoundError("monitor source video is not ready")
    ffmpeg = shutil.which("ffmpeg")
    if not ffmpeg: raise RuntimeError("ffmpeg is not installed")
    w=max(160,min(1920,int(width))); h=max(90,min(1080,int(height)))
    out_path.parent.mkdir(parents=True, exist_ok=True)
    cmd=[ffmpeg,"-hide_banner","-loglevel","error","-ss",f"{t:.3f}","-i",str(src),"-frames:v","1","-vf",f"scale={w}:{h}:force_original_aspect_ratio=decrease,pad={w}:{h}:(ow-iw)/2:(oh-ih)/2","-q:v","3","-y",str(out_path)]
    cp=subprocess.run(cmd,stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True,timeout=30)
    if cp.returncode != 0 or not out_path.exists() or out_path.stat().st_size == 0: raise RuntimeError(cp.stderr.strip() or "ffmpeg frame extraction failed")
    return out_path
