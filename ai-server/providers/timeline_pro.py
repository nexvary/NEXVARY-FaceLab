from __future__ import annotations

import json
import time
import uuid
from pathlib import Path
from typing import Any

MIN_SHOT_SECONDS = 0.25
MAX_SHOT_SECONDS = 120.0
MAX_MARKERS = 200
ALLOWED_TRANSITIONS = {"cut", "fade", "dissolve", "dip_black", "wipe", "match_cut"}


def _job_dir(root: Path, job_id: str) -> Path:
    d = root / job_id
    if not d.exists() or not d.is_dir():
        raise FileNotFoundError(job_id)
    return d


def _read_json(path: Path, default: Any) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception:
        return default


def _write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_suffix(path.suffix + ".tmp")
    tmp.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    tmp.replace(path)


def _read_plan(d: Path) -> dict[str, Any]:
    p = d / "plan.json"
    if not p.exists():
        raise FileNotFoundError("plan.json")
    data = _read_json(p, None)
    if not isinstance(data, dict):
        raise ValueError("invalid plan.json")
    return data


def _edit_file(d: Path) -> Path:
    return d / "timeline_editor_state.json"


def _recovery_file(d: Path) -> Path:
    return d / "timeline_recovery.json"


def _default_state() -> dict[str, Any]:
    return {
        "schema": 10,
        "revision": 0,
        "saved_revision": 0,
        "dirty": False,
        "playhead_seconds": 0.0,
        "zoom": 1.0,
        "in_seconds": 0.0,
        "out_seconds": None,
        "markers": [],
        "track_visibility": {"video": True, "motion": True, "audio": True, "subtitles": True},
        "selected_shot_id": "",
        "updated_at": time.time(),
    }


def _load_state(d: Path, duration: float = 0.0) -> dict[str, Any]:
    state = _read_json(_edit_file(d), _default_state())
    if not isinstance(state, dict):
        state = _default_state()
    base = _default_state(); base.update(state); state = base
    if state.get("out_seconds") is None:
        state["out_seconds"] = round(max(0.0, duration), 3)
    state["zoom"] = max(0.5, min(8.0, float(state.get("zoom", 1.0) or 1.0)))
    state["playhead_seconds"] = max(0.0, min(float(state.get("playhead_seconds", 0.0) or 0.0), max(0.0, duration)))
    state["in_seconds"] = max(0.0, min(float(state.get("in_seconds", 0.0) or 0.0), max(0.0, duration)))
    state["out_seconds"] = max(state["in_seconds"], min(float(state.get("out_seconds", duration) or duration), max(0.0, duration)))
    return state


def _save_state(d: Path, state: dict[str, Any], mark_dirty: bool = True) -> dict[str, Any]:
    state = dict(state)
    if mark_dirty:
        state["revision"] = int(state.get("revision", 0)) + 1
        state["dirty"] = True
    state["updated_at"] = time.time()
    _write_json(_edit_file(d), state)
    return state


def _rebuild(plan: dict[str, Any]) -> dict[str, Any]:
    cursor = 0.0
    for i, s in enumerate(plan.get("shots", [])):
        dur = max(MIN_SHOT_SECONDS, min(MAX_SHOT_SECONDS, float(s.get("duration_seconds", 5.0) or 5.0)))
        s["duration_seconds"] = round(dur, 3)
        s["shot"] = i + 1
        s["index"] = i
        s["start_second"] = round(cursor, 3)
        s["start_seconds"] = round(cursor, 3)
        cursor += dur
        s["end_seconds"] = round(cursor, 3)
    plan["duration_seconds"] = round(cursor, 3)
    plan["shot_count"] = len(plan.get("shots", []))
    plan["timeline_schema"] = 10
    plan["timeline_updated_at"] = time.time()
    return plan


def _before_edit(d: Path, plan: dict[str, Any], state: dict[str, Any]) -> None:
    _write_json(_recovery_file(d), {"created_at": time.time(), "plan": plan, "state": state})


def _commit(d: Path, plan: dict[str, Any], state: dict[str, Any]) -> tuple[dict[str, Any], dict[str, Any]]:
    plan = _rebuild(plan)
    _write_json(d / "plan.json", plan)
    state = _save_state(d, state, True)
    return plan, state


def session(root: Path, job_id: str) -> dict[str, Any]:
    d = _job_dir(root, job_id); plan = _rebuild(_read_plan(d)); state = _load_state(d, float(plan.get("duration_seconds", 0)))
    return {"job_id": job_id, "duration_seconds": plan.get("duration_seconds", 0), "shot_count": plan.get("shot_count", 0), "state": state, "shots": _shot_rows(plan), "validation": validate(plan, state)}


def _shot_rows(plan: dict[str, Any]) -> list[dict[str, Any]]:
    rows = []
    for s in plan.get("shots", []):
        rows.append({
            "shot_id": s.get("id"), "index": s.get("index", 0), "start": s.get("start_seconds", s.get("start_second", 0)),
            "end": s.get("end_seconds", 0), "duration": s.get("duration_seconds", 0),
            "source_in": float(s.get("source_in_seconds", 0.0) or 0.0), "source_out": s.get("source_out_seconds"),
            "camera": s.get("camera_preset") or s.get("camera") or "default", "transition": s.get("transition", "cut"),
            "transition_duration": float(s.get("transition_duration", 0.0) or 0.0),
            "motion": s.get("motion_preset", "idle_natural"), "engine": s.get("preferred_engine", ""),
            "audio_gain_db": float(s.get("audio_gain_db", 0.0) or 0.0), "audio_muted": bool(s.get("audio_muted", False)),
            "subtitle_text": s.get("subtitle_text", s.get("dialogue") or s.get("narration") or ""),
            "subtitle_enabled": bool(s.get("subtitle_enabled", True)), "enabled": bool(s.get("enabled", True)),
        })
    return rows


def set_view(root: Path, job_id: str, playhead: float | None = None, zoom: float | None = None, in_seconds: float | None = None, out_seconds: float | None = None, selected_shot_id: str | None = None) -> dict[str, Any]:
    d = _job_dir(root, job_id); plan = _rebuild(_read_plan(d)); duration = float(plan.get("duration_seconds", 0)); state = _load_state(d, duration)
    if playhead is not None: state["playhead_seconds"] = max(0.0, min(float(playhead), duration))
    if zoom is not None: state["zoom"] = max(0.5, min(8.0, float(zoom)))
    if in_seconds is not None: state["in_seconds"] = max(0.0, min(float(in_seconds), duration))
    if out_seconds is not None: state["out_seconds"] = max(0.0, min(float(out_seconds), duration))
    if state["out_seconds"] < state["in_seconds"]: state["in_seconds"], state["out_seconds"] = state["out_seconds"], state["in_seconds"]
    if selected_shot_id is not None: state["selected_shot_id"] = str(selected_shot_id)
    return _save_state(d, state, False)


def precise_trim(root: Path, job_id: str, shot_id: str, head_seconds: float = 0.0, tail_seconds: float = 0.0) -> dict[str, Any]:
    d = _job_dir(root, job_id); plan = _read_plan(d); state = _load_state(d, float(plan.get("duration_seconds", 0))); shot = next((s for s in plan.get("shots", []) if str(s.get("id")) == shot_id), None)
    if shot is None: raise KeyError(shot_id)
    head = max(0.0, float(head_seconds)); tail = max(0.0, float(tail_seconds)); old = float(shot.get("duration_seconds", 0) or 0)
    if head + tail >= old - MIN_SHOT_SECONDS: raise ValueError("trim would make shot too short")
    _before_edit(d, plan, state)
    shot["source_in_seconds"] = round(float(shot.get("source_in_seconds", 0.0) or 0.0) + head, 3)
    prior_out = shot.get("source_out_seconds")
    shot["source_out_seconds"] = round((float(prior_out) if prior_out is not None else old) - tail, 3)
    shot["duration_seconds"] = round(old - head - tail, 3)
    plan, state = _commit(d, plan, state)
    return {"plan": plan, "state": state}


def add_marker(root: Path, job_id: str, at_seconds: float, label: str, kind: str = "marker") -> dict[str, Any]:
    d = _job_dir(root, job_id); plan = _rebuild(_read_plan(d)); state = _load_state(d, float(plan.get("duration_seconds", 0)))
    if len(state["markers"]) >= MAX_MARKERS: raise ValueError("too many markers")
    marker = {"id": uuid.uuid4().hex[:12], "at": round(max(0.0, min(float(at_seconds), float(plan.get("duration_seconds", 0)))), 3), "label": (label or "Marker")[:80], "kind": kind if kind in {"marker", "chapter", "beat", "note"} else "marker"}
    state["markers"].append(marker); state["markers"].sort(key=lambda x: float(x.get("at", 0)))
    _save_state(d, state, True); return marker


def delete_marker(root: Path, job_id: str, marker_id: str) -> dict[str, Any]:
    d = _job_dir(root, job_id); plan = _rebuild(_read_plan(d)); state = _load_state(d, float(plan.get("duration_seconds", 0))); old = len(state["markers"]); state["markers"] = [m for m in state["markers"] if str(m.get("id")) != marker_id]
    _save_state(d, state, True); return {"deleted": len(state["markers"]) < old, "markers": state["markers"]}


def move_shot(root: Path, job_id: str, shot_id: str, target_index: int) -> dict[str, Any]:
    d = _job_dir(root, job_id); plan = _read_plan(d); state = _load_state(d, float(plan.get("duration_seconds", 0))); shots = plan.get("shots", []); idx = next((i for i,s in enumerate(shots) if str(s.get("id")) == shot_id), None)
    if idx is None: raise KeyError(shot_id)
    target = max(0, min(len(shots)-1, int(target_index))); _before_edit(d, plan, state); shot = shots.pop(idx); shots.insert(target, shot); plan, state = _commit(d, plan, state); return {"plan": plan, "state": state}


def set_transition(root: Path, job_id: str, shot_id: str, transition: str, duration_seconds: float = 0.0) -> dict[str, Any]:
    d = _job_dir(root, job_id); plan = _read_plan(d); state = _load_state(d, float(plan.get("duration_seconds", 0))); shot = next((s for s in plan.get("shots", []) if str(s.get("id")) == shot_id), None)
    if shot is None: raise KeyError(shot_id)
    t = transition if transition in ALLOWED_TRANSITIONS else "cut"; max_d = max(0.0, min(float(shot.get("duration_seconds", 1)) / 2.0, 3.0)); td = 0.0 if t == "cut" else max(0.0, min(float(duration_seconds), max_d))
    _before_edit(d, plan, state); shot["transition"] = t; shot["transition_duration"] = round(td, 3); plan, state = _commit(d, plan, state); return {"plan": plan, "state": state}


def set_audio(root: Path, job_id: str, shot_id: str, gain_db: float = 0.0, fade_in: float = 0.0, fade_out: float = 0.0, muted: bool = False) -> dict[str, Any]:
    d = _job_dir(root, job_id); plan = _read_plan(d); state = _load_state(d, float(plan.get("duration_seconds", 0))); shot = next((s for s in plan.get("shots", []) if str(s.get("id")) == shot_id), None)
    if shot is None: raise KeyError(shot_id)
    dur = float(shot.get("duration_seconds", 1)); _before_edit(d, plan, state); shot["audio_gain_db"] = round(max(-60.0, min(12.0, float(gain_db))), 2); shot["audio_fade_in"] = round(max(0.0, min(float(fade_in), dur/2)), 3); shot["audio_fade_out"] = round(max(0.0, min(float(fade_out), dur/2)), 3); shot["audio_muted"] = bool(muted); plan, state = _commit(d, plan, state); return {"plan": plan, "state": state}


def set_subtitle(root: Path, job_id: str, shot_id: str, text: str, offset_seconds: float = 0.0, enabled: bool = True) -> dict[str, Any]:
    d = _job_dir(root, job_id); plan = _read_plan(d); state = _load_state(d, float(plan.get("duration_seconds", 0))); shot = next((s for s in plan.get("shots", []) if str(s.get("id")) == shot_id), None)
    if shot is None: raise KeyError(shot_id)
    _before_edit(d, plan, state); shot["subtitle_text"] = (text or "")[:1000]; shot["subtitle_offset_seconds"] = round(max(-5.0, min(5.0, float(offset_seconds))), 3); shot["subtitle_enabled"] = bool(enabled); plan, state = _commit(d, plan, state); return {"plan": plan, "state": state}


def render_range(root: Path, job_id: str) -> dict[str, Any]:
    d = _job_dir(root, job_id); plan = _rebuild(_read_plan(d)); state = _load_state(d, float(plan.get("duration_seconds", 0))); start = float(state["in_seconds"]); end = float(state["out_seconds"])
    shots = []
    for row in _shot_rows(plan):
        if float(row["end"]) > start and float(row["start"]) < end:
            shots.append(row)
    return {"job_id": job_id, "range": {"in": start, "out": end, "duration": round(max(0.0,end-start),3)}, "shot_count": len(shots), "shots": shots, "ready": bool(shots) and end > start}


def autosave(root: Path, job_id: str) -> dict[str, Any]:
    d = _job_dir(root, job_id); plan = _rebuild(_read_plan(d)); state = _load_state(d, float(plan.get("duration_seconds", 0))); payload = {"saved_at": time.time(), "plan": plan, "state": state}; _write_json(_recovery_file(d), payload); state["saved_revision"] = int(state.get("revision", 0)); state["dirty"] = False; _save_state(d, state, False); return {"saved": True, "saved_at": payload["saved_at"], "revision": state["saved_revision"]}


def recovery_status(root: Path, job_id: str) -> dict[str, Any]:
    d = _job_dir(root, job_id); f = _recovery_file(d); state = _load_state(d, float(_rebuild(_read_plan(d)).get("duration_seconds", 0))); return {"available": f.exists(), "path": f.name if f.exists() else None, "dirty": bool(state.get("dirty")), "revision": state.get("revision", 0), "saved_revision": state.get("saved_revision", 0), "saved_at": _read_json(f, {}).get("saved_at") if f.exists() else None}


def restore_recovery(root: Path, job_id: str) -> dict[str, Any]:
    d = _job_dir(root, job_id); data = _read_json(_recovery_file(d), None)
    if not isinstance(data, dict) or not isinstance(data.get("plan"), dict): raise FileNotFoundError("recovery")
    plan = _rebuild(data["plan"]); state = data.get("state") if isinstance(data.get("state"), dict) else _default_state(); _write_json(d / "plan.json", plan); state["dirty"] = False; _save_state(d, state, False); return {"restored": True, "plan": plan, "state": state}


def validate(plan: dict[str, Any], state: dict[str, Any]) -> dict[str, Any]:
    issues = []
    duration = float(plan.get("duration_seconds", 0))
    if float(state.get("out_seconds", duration) or 0) <= float(state.get("in_seconds", 0) or 0): issues.append({"severity":"error","code":"invalid_range","message":"Out point must be after In point"})
    for i,s in enumerate(plan.get("shots", [])):
        dur=float(s.get("duration_seconds",0) or 0); td=float(s.get("transition_duration",0) or 0)
        if dur < MIN_SHOT_SECONDS: issues.append({"severity":"error","shot":i+1,"code":"too_short"})
        if td > dur/2: issues.append({"severity":"warning","shot":i+1,"code":"transition_too_long"})
        if float(s.get("audio_fade_in",0) or 0)+float(s.get("audio_fade_out",0) or 0)>dur: issues.append({"severity":"warning","shot":i+1,"code":"audio_fades_overlap"})
    return {"healthy": not any(x.get("severity")=="error" for x in issues), "issues": issues, "issue_count": len(issues)}
