import hashlib
import json
import os
import shutil
import subprocess
import threading
import time
import uuid
import zipfile
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from providers.film_engines import status as multi_engine_status, choose_engine, command_for
from providers.driving_motions import resolve as resolve_driving_motion
from providers.motion_presets import apply_preset as apply_motion_preset, MOTION_PRESETS

FILM_PRESETS = {
    "cinematic": {"shot_seconds": 6, "camera": "slow cinematic dolly, natural subject motion", "transition": "fade"},
    "historical": {"shot_seconds": 7, "camera": "measured historical drama framing, subtle camera movement", "transition": "dissolve"},
    "travel": {"shot_seconds": 5, "camera": "travel-film gimbal movement, environmental reveal", "transition": "wipe"},
    "drama": {"shot_seconds": 6, "camera": "character-focused dramatic coverage", "transition": "fade"},
    "documentary": {"shot_seconds": 7, "camera": "documentary observational coverage", "transition": "cut"},
    "advert": {"shot_seconds": 4, "camera": "polished commercial camera movement", "transition": "cut"},
}

EXPORT_PRESETS = {
    "preview": {"width": 854, "height": 480, "fps": 24, "crf": 27, "audio_kbps": 128},
    "720p": {"width": 1280, "height": 720, "fps": 24, "crf": 23, "audio_kbps": 160},
    "1080p": {"width": 1920, "height": 1080, "fps": 24, "crf": 21, "audio_kbps": 192},
}

ASPECT_RATIOS = {
    "16:9": (16, 9),
    "9:16": (9, 16),
    "1:1": (1, 1),
}

@dataclass(frozen=True)
class EngineProbe:
    name: str
    env_var: str
    license_note: str

ENGINES = [
    EngineProbe("CogVideoX-2B", "FACELAB_COGVIDEOX_DIR", "candidate only; audit exact code + weights before redistribution"),
    EngineProbe("LTX-Video", "FACELAB_LTX_VIDEO_DIR", "candidate only; audit exact version and weight license before commercial packaging"),
    EngineProbe("MuseTalk", "FACELAB_MUSETALK_DIR", "lip-sync candidate; audit bundled dependencies and exact weights"),
    EngineProbe("FFmpeg", "FACELAB_FFMPEG", "post-production executable; distribution obligations depend on selected build"),
]

_JOB_LOCK = threading.Lock()
_EXECUTOR = ThreadPoolExecutor(max_workers=max(1, int(os.getenv("FACELAB_FILM_WORKERS", "1"))), thread_name_prefix="facelab-film")


def _ffmpeg_path() -> str | None:
    configured = os.getenv("FACELAB_FFMPEG", "").strip()
    if configured:
        p = Path(configured).expanduser()
        if p.is_file():
            return str(p)
        found = shutil.which(configured)
        if found:
            return found
    return shutil.which("ffmpeg")


def _ffprobe_path() -> str | None:
    ffmpeg = _ffmpeg_path()
    if ffmpeg:
        p = Path(ffmpeg)
        candidate = p.with_name("ffprobe" + p.suffix)
        if candidate.exists():
            return str(candidate)
    return shutil.which("ffprobe")


def _external_engine_command() -> list[str] | None:
    raw = os.getenv("FACELAB_VIDEO_ENGINE_CMD_JSON", "").strip()
    if not raw:
        return None
    try:
        value = json.loads(raw)
    except json.JSONDecodeError:
        return None
    if not isinstance(value, list) or not value or not all(isinstance(x, str) and x for x in value):
        return None
    return value


def _lipsync_command() -> list[str] | None:
    raw = os.getenv("FACELAB_LIPSYNC_CMD_JSON", "").strip()
    if not raw:
        return None
    try:
        value = json.loads(raw)
    except json.JSONDecodeError:
        return None
    if not isinstance(value, list) or not value or not all(isinstance(x, str) and x for x in value):
        return None
    return value


def status() -> dict[str, Any]:
    probes = []
    for e in ENGINES:
        raw = os.getenv(e.env_var, "").strip()
        exists = _ffmpeg_path() is not None if e.name == "FFmpeg" else bool(raw and Path(raw).expanduser().exists())
        probes.append({"name": e.name, "configured": bool(raw), "path_exists": exists, "env": e.env_var, "license_note": e.license_note})
    return {
        "engine": "NEXVARY Short Film Engine v27500",
        "entertainment_profile": "personal-friends-noncommercial",
        "multi_engine": multi_engine_status(),
        "ready_for_storyboard": True,
        "ready_for_motion_preview": _ffmpeg_path() is not None,
        "ready_for_ai_video_generation": _external_engine_command() is not None,
        "ready_for_lipsync": _lipsync_command() is not None,
        "engines": probes,
        "external_adapter_configured": _external_engine_command() is not None,
        "ffmpeg_ready": _ffmpeg_path() is not None,
        "project_system": True,
        "multi_shot_render": True,
        "audio_pipeline": True,
        "subtitle_pipeline": True,
        "resume_retry_cancel": True,
        "export_presets": EXPORT_PRESETS,
        "aspect_ratios": list(ASPECT_RATIOS),
        "identity_lock": "continuity metadata is injected into every shot prompt",
        "wardrobe_lock": "wardrobe continuity metadata is injected into every shot prompt",
        "location_lock": "location continuity metadata is injected into every shot prompt",
        "paid_api_required": False,
    }


def _clean_story(story: str) -> str:
    return " ".join((story or "").strip().split())


def build_plan(story: str, duration_seconds: int, preset: str, aspect_ratio: str = "16:9", motion_preset: str = "") -> dict[str, Any]:
    story = _clean_story(story)
    if not story:
        raise ValueError("story is required")
    duration_seconds = max(30, min(int(duration_seconds), 300))
    if aspect_ratio not in ASPECT_RATIOS:
        aspect_ratio = "16:9"
    cfg = FILM_PRESETS.get(preset, FILM_PRESETS["cinematic"])
    preset = preset if preset in FILM_PRESETS else "cinematic"
    shot_seconds = cfg["shot_seconds"]
    count = max(5, min(60, round(duration_seconds / shot_seconds)))
    base, rem = divmod(duration_seconds, count)
    beats = [
        "establish the location and character",
        "introduce the main movement",
        "medium shot preserving face and wardrobe",
        "reaction or look toward camera",
        "environmental reveal while preserving location continuity",
        "return to the character with controlled body motion",
        "cinematic transition into the next action",
        "closing hero shot with stable identity",
    ]
    shots, cursor = [], 0
    for i in range(count):
        dur = base + (1 if i < rem else 0)
        beat = beats[i % len(beats)]
        shot_id = f"shot-{i+1:03d}"
        narration = story if i == 0 else ""
        shots.append({
            "id": shot_id,
            "shot": i + 1,
            "start_second": cursor,
            "duration_seconds": dur,
            "purpose": beat,
            "camera": cfg["camera"],
            "transition": cfg["transition"] if i else "cut",
            "prompt": f"{story}. {beat}. Preserve the exact same character identity, face, outfit, body proportions and location. Natural coherent motion. No identity drift.",
            "negative_prompt": "face change, different person, wardrobe change, extra limbs, distorted hands, identity drift, text, watermark",
            "dialogue": "",
            "narration": narration,
            "motion_preset": motion_preset or "idle_natural",
            "driving_motion_id": "",
            "camera_preset": "",
            "locks": ["character", "face", "wardrobe", "location", "color-grade"],
            "enabled": True,
        })
        cursor += dur
    if motion_preset:
        if motion_preset not in MOTION_PRESETS:
            raise ValueError("unknown motion_preset")
        shots = [apply_motion_preset(shot, motion_preset) for shot in shots]
    return {
        "schema_version": 5,
        "preset": preset,
        "aspect_ratio": aspect_ratio,
        "motion_preset": motion_preset,
        "duration_seconds": duration_seconds,
        "shot_count": count,
        "story": story,
        "continuity": {
            "character_lock": True, "face_lock": True, "wardrobe_lock": True,
            "location_lock": True, "color_grade_lock": True, "seed_strategy": "project-stable",
        },
        "shots": shots,
        "render_policy": "render short shots independently; validate continuity; retry drifted shots; assemble with FFmpeg",
        "paid_api_required": False,
    }


def _write_json(path: Path, payload: dict[str, Any]):
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_suffix(path.suffix + ".tmp")
    tmp.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    tmp.replace(path)


def _read_json(path: Path, default=None):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception:
        return {} if default is None else default


def _job_file(job_dir: Path) -> Path:
    return job_dir / "job.json"


def _append_log(job_dir: Path, text: str):
    line = f"{time.strftime('%Y-%m-%d %H:%M:%S')} {text}\n"
    with (job_dir / "render.log").open("a", encoding="utf-8") as f:
        f.write(line)


def _update_job(job_dir: Path, **changes):
    with _JOB_LOCK:
        payload = _read_json(_job_file(job_dir), {})
        payload.update(changes)
        payload["updated_at"] = time.time()
        _write_json(_job_file(job_dir), payload)


def _safe_id(value: str) -> bool:
    return bool(value) and len(value) <= 64 and all(c in "0123456789abcdef-" for c in value.lower())


def read_job(job_root: Path, job_id: str) -> dict[str, Any] | None:
    if not _safe_id(job_id):
        return None
    job_dir = job_root / job_id
    jf = _job_file(job_dir)
    if not jf.exists():
        return None
    payload = _read_json(jf, None)
    if not isinstance(payload, dict):
        return None
    output = job_dir / "output.mp4"
    payload["video_ready"] = output.exists() and output.stat().st_size > 0
    payload["thumbnail_ready"] = (job_dir / "thumbnail.jpg").exists()
    payload["subtitle_ready"] = (job_dir / "subtitles.srt").exists()
    return payload


def list_jobs(job_root: Path, limit: int = 30) -> list[dict[str, Any]]:
    items = []
    if not job_root.exists():
        return items
    for d in job_root.iterdir():
        if not d.is_dir():
            continue
        job = read_job(job_root, d.name)
        if job:
            items.append(job)
    items.sort(key=lambda x: float(x.get("created_at", 0)), reverse=True)
    return items[:max(1, min(limit, 100))]


def cancel_job(job_root: Path, job_id: str) -> dict[str, Any] | None:
    job = read_job(job_root, job_id)
    if not job:
        return None
    d = job_root / job_id
    (d / "cancel.flag").write_text("cancel", encoding="utf-8")
    if job.get("state") in {"queued", "running"}:
        _update_job(d, state="cancelling")
    _append_log(d, "Cancellation requested")
    return read_job(job_root, job_id)


def _cancelled(job_dir: Path) -> bool:
    return (job_dir / "cancel.flag").exists()


def _target_dimensions(export_preset: str, aspect_ratio: str) -> tuple[int, int, int, int]:
    cfg = EXPORT_PRESETS.get(export_preset, EXPORT_PRESETS["720p"])
    w, h = cfg["width"], cfg["height"]
    if aspect_ratio == "9:16":
        w, h = h, w
    elif aspect_ratio == "1:1":
        side = min(w, h)
        w = h = side
    return int(w), int(h), int(cfg["fps"]), int(cfg["crf"])


def _render_ffmpeg_preview(input_path: Path, output_path: Path, seconds: int, export_preset: str, aspect_ratio: str, variant: int = 0):
    ffmpeg = _ffmpeg_path()
    if not ffmpeg:
        raise RuntimeError("FFmpeg is not installed or configured")
    seconds = max(2, min(int(seconds), 30))
    width, height, fps, crf = _target_dimensions(export_preset, aspect_ratio)
    frames = seconds * fps
    drift = 0.00055 + (variant % 4) * 0.00015
    vf = (
        f"scale={width}:{height}:force_original_aspect_ratio=increase,"
        f"crop={width}:{height},"
        f"zoompan=z='min(zoom+{drift},1.13)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d={frames}:s={width}x{height}:fps={fps},"
        "format=yuv420p"
    )
    cmd = [ffmpeg, "-y", "-loop", "1", "-i", str(input_path), "-vf", vf, "-t", str(seconds), "-r", str(fps), "-an", "-c:v", "libx264", "-preset", "veryfast", "-crf", str(crf), "-movflags", "+faststart", str(output_path)]
    proc = subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE, text=True, timeout=max(120, seconds * 12))
    if proc.returncode != 0:
        raise RuntimeError("FFmpeg shot preview failed: " + proc.stderr[-1200:])


def _render_external(input_path: Path, output_path: Path, prompt: str, seconds: int, preset: str, seed: int, aspect_ratio: str):
    template = _external_engine_command()
    if not template:
        raise RuntimeError("No audited local AI video adapter configured")
    mapping = {
        "input": str(input_path), "output": str(output_path), "prompt": prompt,
        "duration": str(max(1, min(int(seconds), 30))), "preset": preset,
        "seed": str(seed), "aspect_ratio": aspect_ratio,
    }
    cmd = [part.format(**mapping) for part in template]
    proc = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, timeout=int(os.getenv("FACELAB_VIDEO_ENGINE_TIMEOUT", "1800")))
    if proc.returncode != 0:
        raise RuntimeError("AI video adapter failed: " + (proc.stderr or proc.stdout)[-1600:])
    if not output_path.exists() or output_path.stat().st_size == 0:
        raise RuntimeError("AI video adapter completed without output")


def _render_selected_engine(engine_id: str, input_path: Path, output_path: Path, shot: dict[str, Any], seconds: int, preset: str, seed: int, aspect_ratio: str):
    template = command_for(engine_id)
    if not template:
        return _render_external(input_path, output_path, shot.get("prompt", ""), seconds, preset, seed, aspect_ratio)
    mapping = {
        "input": str(input_path), "output": str(output_path), "prompt": str(shot.get("prompt", "")),
        "negative_prompt": str(shot.get("negative_prompt", "")), "duration": str(max(1, min(int(seconds), 30))),
        "preset": preset, "seed": str(seed), "aspect_ratio": aspect_ratio,
        "dialogue": str(shot.get("dialogue", "")), "narration": str(shot.get("narration", "")),
        "driving": str(resolve_driving_motion(str(shot.get("motion_preset", ""))) or ""),
    }
    cmd=[part.format(**mapping) for part in template]
    proc=subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, timeout=int(os.getenv("FACELAB_VIDEO_ENGINE_TIMEOUT", "2400")))
    if proc.returncode != 0:
        raise RuntimeError(f"{engine_id} adapter failed: " + (proc.stderr or proc.stdout)[-1800:])
    if not output_path.exists() or output_path.stat().st_size == 0:
        raise RuntimeError(f"{engine_id} completed without output")

def _concat_shots(shots: list[Path], output_path: Path):
    ffmpeg = _ffmpeg_path()
    if not ffmpeg:
        raise RuntimeError("FFmpeg is required to assemble shots")
    concat = output_path.parent / "concat.txt"
    concat.write_text("".join(f"file '{p.resolve().as_posix()}'\n" for p in shots), encoding="utf-8")
    cmd = [ffmpeg, "-y", "-f", "concat", "-safe", "0", "-i", str(concat), "-c", "copy", "-movflags", "+faststart", str(output_path)]
    proc = subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE, text=True, timeout=600)
    if proc.returncode != 0:
        cmd = [ffmpeg, "-y", "-f", "concat", "-safe", "0", "-i", str(concat), "-c:v", "libx264", "-preset", "veryfast", "-crf", "22", "-an", "-movflags", "+faststart", str(output_path)]
        proc = subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE, text=True, timeout=1200)
        if proc.returncode != 0:
            raise RuntimeError("FFmpeg assembly failed: " + proc.stderr[-1400:])


def _make_thumbnail(video_path: Path, thumb_path: Path):
    ffmpeg = _ffmpeg_path()
    if not ffmpeg:
        return
    subprocess.run([ffmpeg, "-y", "-ss", "0.5", "-i", str(video_path), "-frames:v", "1", "-q:v", "3", str(thumb_path)], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=120)


def _format_srt_time(seconds: float) -> str:
    ms = int(round(seconds * 1000)); h, rem = divmod(ms, 3600000); m, rem = divmod(rem, 60000); s, ms = divmod(rem, 1000)
    return f"{h:02d}:{m:02d}:{s:02d},{ms:03d}"


def create_subtitles(job_dir: Path, plan: dict[str, Any]) -> Path:
    lines, idx = [], 1
    for shot in plan.get("shots", []):
        text = (shot.get("dialogue") or shot.get("narration") or "").strip()
        if not text:
            continue
        start = float(shot.get("start_second", 0)); end = start + float(shot.get("duration_seconds", 1))
        lines += [str(idx), f"{_format_srt_time(start)} --> {_format_srt_time(end)}", text, ""]
        idx += 1
    path = job_dir / "subtitles.srt"
    path.write_text("\n".join(lines), encoding="utf-8")
    return path


def attach_audio(job_root: Path, job_id: str, audio_bytes: bytes, kind: str) -> dict[str, Any]:
    job = read_job(job_root, job_id)
    if not job:
        raise FileNotFoundError("job not found")
    if kind not in {"narration", "music"}:
        raise ValueError("kind must be narration or music")
    if len(audio_bytes) > 100 * 1024 * 1024:
        raise ValueError("audio file too large")
    d = job_root / job_id
    ext = ".audio"
    path = d / f"{kind}{ext}"
    path.write_bytes(audio_bytes)
    _update_job(d, **{f"{kind}_attached": True})
    _append_log(d, f"Attached {kind} audio")
    return read_job(job_root, job_id) or {}


def mux_audio(job_root: Path, job_id: str, include_subtitles: bool = True) -> dict[str, Any]:
    job = read_job(job_root, job_id)
    if not job:
        raise FileNotFoundError("job not found")
    d = job_root / job_id
    source = d / "output.mp4"
    if not source.exists():
        raise RuntimeError("video not ready")
    ffmpeg = _ffmpeg_path()
    if not ffmpeg:
        raise RuntimeError("FFmpeg not available")
    narr = d / "narration.audio"; music = d / "music.audio"
    if not narr.exists() and not music.exists():
        return job
    temp = d / "output_audio.mp4"
    cmd = [ffmpeg, "-y", "-i", str(source)]
    audio_inputs = []
    if narr.exists():
        cmd += ["-i", str(narr)]; audio_inputs.append((len(audio_inputs)+1, 1.0))
    if music.exists():
        cmd += ["-i", str(music)]; audio_inputs.append((len(audio_inputs)+1, 0.22))
    if len(audio_inputs) == 1:
        idx, vol = audio_inputs[0]
        cmd += ["-filter_complex", f"[{idx}:a]volume={vol}[aout]", "-map", "0:v:0", "-map", "[aout]"]
    else:
        (i1, v1), (i2, v2) = audio_inputs
        cmd += ["-filter_complex", f"[{i1}:a]volume={v1}[a1];[{i2}:a]volume={v2}[a2];[a1][a2]amix=inputs=2:duration=longest[aout]", "-map", "0:v:0", "-map", "[aout]"]
    cmd += ["-c:v", "copy", "-c:a", "aac", "-b:a", "192k", "-shortest", "-movflags", "+faststart", str(temp)]
    proc = subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE, text=True, timeout=1200)
    if proc.returncode != 0:
        raise RuntimeError("Audio mux failed: " + proc.stderr[-1400:])
    temp.replace(source)
    _update_job(d, audio_muxed=True, subtitles_available=(d / "subtitles.srt").exists())
    _append_log(d, "Audio post-production completed")
    return read_job(job_root, job_id) or {}


def _continuity_fingerprint(source: bytes, story: str, preset: str) -> str:
    return hashlib.sha256(source + story.encode("utf-8") + preset.encode("ascii", "ignore")).hexdigest()[:20]


def _run_job(job_dir: Path):
    try:
        _update_job(job_dir, state="running", progress=2, started_at=time.time())
        meta = _read_json(_job_file(job_dir), {})
        plan = _read_json(job_dir / "plan.json", {})
        input_path = job_dir / "source.jpg"
        shots_dir = job_dir / "shots"; shots_dir.mkdir(exist_ok=True)
        enabled_shots = [s for s in plan.get("shots", []) if s.get("enabled", True)]
        if not enabled_shots:
            raise RuntimeError("No enabled shots")
        rendered: list[Path] = []
        mode = meta.get("render_mode", "preview")
        seed_base = int(meta.get("seed", 7000))
        max_ai_seconds = max(2, min(int(os.getenv("FACELAB_AI_SHOT_MAX_SECONDS", "8")), 12))
        preview_cap = max(2, min(int(os.getenv("FACELAB_PREVIEW_SHOT_SECONDS", "4")), 8))
        for idx, shot in enumerate(enabled_shots):
            if _cancelled(job_dir):
                _update_job(job_dir, state="cancelled", progress=100, completed_at=time.time())
                _append_log(job_dir, "Render cancelled")
                return
            shot_id = shot["id"]
            out = shots_dir / f"{shot_id}.mp4"
            seconds = int(shot.get("duration_seconds", 5))
            seconds = min(seconds, max_ai_seconds if mode == "ai" else preview_cap)
            if out.exists() and out.stat().st_size > 1000:
                _append_log(job_dir, f"Reusing completed {shot_id} (resume)")
            else:
                _append_log(job_dir, f"Rendering {shot_id} mode={mode} seconds={seconds}")
                if mode == "ai":
                    engine_id = choose_engine(shot)
                    _append_log(job_dir, f"Selected engine {engine_id} for {shot_id}")
                    _render_selected_engine(engine_id, input_path, out, shot, seconds, meta.get("preset", "cinematic"), seed_base + idx, meta.get("aspect_ratio", "16:9"))
                else:
                    _render_ffmpeg_preview(input_path, out, seconds, meta.get("export_preset", "720p"), meta.get("aspect_ratio", "16:9"), idx)
            rendered.append(out)
            progress = 5 + int(((idx + 1) / len(enabled_shots)) * 78)
            _update_job(job_dir, progress=progress, current_shot=shot_id, rendered_shots=idx + 1)
        _update_job(job_dir, progress=88, current_shot="assembly")
        output = job_dir / "output.mp4"
        _concat_shots(rendered, output)
        create_subtitles(job_dir, plan)
        _make_thumbnail(output, job_dir / "thumbnail.jpg")
        _update_job(job_dir, state="completed", progress=100, engine=("external-audited-local-ai-adapter" if mode == "ai" else "ffmpeg-multishot-preview"), rendered_seconds=sum(min(int(s.get("duration_seconds", 5)), max_ai_seconds if mode == "ai" else preview_cap) for s in enabled_shots), completed_at=time.time(), current_shot="done", note=("AI multi-shot output" if mode == "ai" else "Multi-shot camera-motion preview. Actual body/face motion requires configured local AI adapter."))
        _append_log(job_dir, "Film render completed")
    except Exception as exc:
        _update_job(job_dir, state="failed", progress=100, error=str(exc)[:3000], completed_at=time.time())
        _append_log(job_dir, "FAILED: " + str(exc)[:1000])


def create_render_job(job_root: Path, image_bytes: bytes, story: str, duration_seconds: int, preset: str, render_mode: str = "auto", aspect_ratio: str = "16:9", export_preset: str = "720p", motion_preset: str = "") -> dict[str, Any]:
    plan = build_plan(story, duration_seconds, preset, aspect_ratio, motion_preset)
    if export_preset not in EXPORT_PRESETS:
        export_preset = "720p"
    job_root.mkdir(parents=True, exist_ok=True)
    job_id = uuid.uuid4().hex
    job_dir = job_root / job_id; job_dir.mkdir(parents=True, exist_ok=False)
    (job_dir / "source.jpg").write_bytes(image_bytes)
    _write_json(job_dir / "plan.json", plan)
    external_ready = _external_engine_command() is not None
    multi_ready = any(e.get("ready") and e.get("id") != "ffmpeg" for e in multi_engine_status().get("engines", []))
    ai_ready = external_ready or multi_ready
    if render_mode == "auto":
        resolved_mode = "ai" if ai_ready else "preview"
    elif render_mode in {"ai", "preview"}:
        resolved_mode = render_mode
    else:
        raise ValueError("render_mode must be auto, ai or preview")
    if resolved_mode == "ai" and not ai_ready:
        raise RuntimeError("AI render requested but no audited local AI video adapter is configured")
    payload = {
        "job_id": job_id, "state": "queued", "progress": 0, "created_at": time.time(),
        "story": plan["story"], "duration_seconds": plan["duration_seconds"], "preset": plan["preset"],
        "aspect_ratio": aspect_ratio, "export_preset": export_preset, "motion_preset": plan.get("motion_preset", ""),
        "shot_count": plan["shot_count"], "rendered_shots": 0, "render_mode": resolved_mode,
        "requested_render_mode": render_mode, "seed": int(hashlib.sha256(image_bytes).hexdigest()[:8], 16),
        "continuity_fingerprint": _continuity_fingerprint(image_bytes, plan["story"], plan["preset"]),
        "paid_api_required": False,
    }
    _write_json(_job_file(job_dir), payload)
    _append_log(job_dir, "Film job created")
    _EXECUTOR.submit(_run_job, job_dir)
    return payload


def retry_job(job_root: Path, job_id: str) -> dict[str, Any]:
    job = read_job(job_root, job_id)
    if not job:
        raise FileNotFoundError("job not found")
    d = job_root / job_id
    (d / "cancel.flag").unlink(missing_ok=True)
    for p in [d / "output.mp4", d / "thumbnail.jpg"]:
        p.unlink(missing_ok=True)
    _update_job(d, state="queued", progress=0, error=None, rendered_shots=0, retry_count=int(job.get("retry_count", 0)) + 1)
    _append_log(d, "Retry scheduled")
    _EXECUTOR.submit(_run_job, d)
    return read_job(job_root, job_id) or {}


def read_plan(job_root: Path, job_id: str) -> dict[str, Any] | None:
    if not read_job(job_root, job_id):
        return None
    return _read_json(job_root / job_id / "plan.json", None)


def update_shot(job_root: Path, job_id: str, shot_id: str, updates: dict[str, Any]) -> dict[str, Any]:
    plan = read_plan(job_root, job_id)
    if not plan:
        raise FileNotFoundError("job not found")
    allowed = {"prompt", "negative_prompt", "camera", "dialogue", "narration", "transition", "enabled", "duration_seconds", "motion_preset", "preferred_engine", "driving_motion_id", "camera_preset", "continuity_note", "color_grade", "speed", "favorite"}
    target = None
    for shot in plan.get("shots", []):
        if shot.get("id") == shot_id:
            target = shot; break
    if target is None:
        raise KeyError("shot not found")
    for k, v in updates.items():
        if k in allowed:
            if k == "duration_seconds":
                v = max(2, min(int(v), 20))
            target[k] = v
    # rebuild timeline
    cursor = 0
    for shot in plan.get("shots", []):
        shot["start_second"] = cursor
        cursor += int(shot.get("duration_seconds", 1))
    plan["duration_seconds"] = cursor
    _write_json(job_root / job_id / "plan.json", plan)
    create_subtitles(job_root / job_id, plan)
    _append_log(job_root / job_id, f"Updated {shot_id}")
    return plan


def read_log(job_root: Path, job_id: str, max_chars: int = 20000) -> str | None:
    if not read_job(job_root, job_id):
        return None
    p = job_root / job_id / "render.log"
    if not p.exists():
        return ""
    text = p.read_text(encoding="utf-8", errors="replace")
    return text[-max_chars:]


def apply_lipsync(job_root: Path, job_id: str) -> dict[str, Any]:
    job = read_job(job_root, job_id)
    if not job:
        raise FileNotFoundError("job not found")
    template = _lipsync_command()
    if not template:
        raise RuntimeError("No audited local lip-sync adapter configured")
    d = job_root / job_id
    video = d / "output.mp4"; audio = d / "narration.audio"
    if not video.exists():
        raise RuntimeError("video not ready")
    if not audio.exists():
        raise RuntimeError("narration audio is required for lip-sync")
    out = d / "output_lipsync.mp4"
    mapping = {"video": str(video), "audio": str(audio), "output": str(out)}
    cmd = [part.format(**mapping) for part in template]
    proc = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, timeout=int(os.getenv("FACELAB_LIPSYNC_TIMEOUT", "1800")))
    if proc.returncode != 0:
        raise RuntimeError("Lip-sync adapter failed: " + (proc.stderr or proc.stdout)[-1600:])
    if not out.exists() or out.stat().st_size == 0:
        raise RuntimeError("Lip-sync adapter completed without output")
    out.replace(video)
    _update_job(d, lipsync_applied=True)
    _append_log(d, "Lip-sync post-production completed")
    return read_job(job_root, job_id) or {}


def create_project_archive(job_root: Path, job_id: str) -> Path:
    job = read_job(job_root, job_id)
    if not job:
        raise FileNotFoundError("job not found")
    d = job_root / job_id
    archive = d / f"facelab-film-{job_id}.zip"
    with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED) as z:
        for name in ["job.json", "plan.json", "subtitles.srt", "thumbnail.jpg", "render.log"]:
            p = d / name
            if p.exists():
                z.write(p, name)
        if (d / "output.mp4").exists():
            z.write(d / "output.mp4", "output.mp4")
    return archive
