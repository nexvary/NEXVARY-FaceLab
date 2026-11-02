from __future__ import annotations
from typing import Any

MOTION_PRESETS: dict[str, dict[str, Any]] = {
    "idle_natural": {"label":"Natural Idle","ar":"وقفة طبيعية","engine_hint":"liveportrait","seconds":5,"prompt":"natural breathing, subtle blink, tiny head motion, stable identity"},
    "look_camera": {"label":"Look at Camera","ar":"ينظر للكاميرا","engine_hint":"liveportrait","seconds":5,"prompt":"slowly turns gaze toward camera, subtle expression, stable face"},
    "smile": {"label":"Smile","ar":"ابتسامة","engine_hint":"liveportrait","seconds":5,"prompt":"natural gentle smile, realistic blink and breathing"},
    "talking": {"label":"Talking","ar":"يتحدث","engine_hint":"echomimicv2","seconds":6,"prompt":"natural conversational upper-body motion synchronized to speech"},
    "wave": {"label":"Wave","ar":"يلوح بيده","engine_hint":"mimicmotion","seconds":6,"prompt":"raises one hand and waves naturally, full body remains consistent"},
    "walk_forward": {"label":"Walk Forward","ar":"يمشي للأمام","engine_hint":"mimicmotion","seconds":7,"prompt":"walks forward naturally for two to three steps, stable body proportions"},
    "turn_side": {"label":"Cinematic Turn","ar":"يلتفت سينمائياً","engine_hint":"wan22","seconds":6,"prompt":"cinematic body turn to the side, cloth and hair react naturally"},
    "sit_down": {"label":"Sit Down","ar":"يجلس","engine_hint":"mimicmotion","seconds":7,"prompt":"carefully sits down with anatomically coherent body movement"},
    "hero_pose": {"label":"Hero Pose","ar":"وقفة بطولية","engine_hint":"wan22","seconds":6,"prompt":"confident hero pose, subtle camera push-in, realistic garment motion"},
    "travel_reveal": {"label":"Travel Reveal","ar":"كشف المكان","engine_hint":"wan22","seconds":7,"prompt":"subject turns as camera reveals the environment, cinematic travel shot"},
    "nod": {"label":"Nod","ar":"يهز رأسه بالموافقة","engine_hint":"liveportrait","seconds":4,"prompt":"small natural nod, preserve exact face identity"},
    "reaction": {"label":"Reaction","ar":"رد فعل","engine_hint":"liveportrait","seconds":5,"prompt":"subtle natural facial reaction and eye movement, no identity drift"},
}

def catalog() -> dict[str, Any]:
    return {"count": len(MOTION_PRESETS), "presets": [{"id": k, **v} for k,v in MOTION_PRESETS.items()]}

def apply_preset(shot: dict[str,Any], preset_id: str) -> dict[str,Any]:
    p=MOTION_PRESETS.get(preset_id)
    if not p:
        raise KeyError(preset_id)
    out=dict(shot)
    base=str(out.get("prompt","")).strip()
    out["prompt"]=(base+". "+p["prompt"]).strip(". ")+". Preserve exact character, face, wardrobe and location continuity."
    out["motion_preset"]=preset_id
    out["preferred_engine"]=p["engine_hint"]
    out["duration_seconds"]=int(out.get("duration_seconds") or p["seconds"])
    return out
