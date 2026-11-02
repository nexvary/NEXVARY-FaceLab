import asyncio
import hashlib
import hmac
import io
import os
import shutil
import tempfile
import time
import uuid
from urllib.parse import unquote
from pathlib import Path
from typing import Optional

from fastapi import Depends, FastAPI, File, Form, Header, HTTPException, UploadFile
from fastapi.responses import FileResponse, JSONResponse
from PIL import Image, UnidentifiedImageError

from providers.runner import EFFECTS, provider_status, run_effect
from providers.wardrobe import WARDROBE_STYLES, run_wardrobe, wardrobe_status
from providers.heritage import HERITAGE_SITES, composite as heritage_composite, status as heritage_status
from providers.engine_doctor import report as engine_doctor_report
from providers.driving_motions import catalog as driving_motion_catalog, register as register_driving_motion, resolve as resolve_driving_motion, make_liveportrait_template
from providers.motion_presets import catalog as motion_catalog, apply_preset as motion_apply_preset
from providers.motion_library import visual_catalog as visual_motion_catalog, ensure_thumbnail as motion_thumbnail
from providers.continuity import analyze as continuity_analyze
from providers.audio_director import build_timeline as audio_build_timeline
from providers.director_presets import catalog as director_catalog
from providers.project_templates import catalog as project_template_catalog, get as project_template_get
from providers.storyboard_studio import reorder as sb_reorder, duplicate as sb_duplicate, remove as sb_remove, split as sb_split, batch_apply as sb_batch_apply, validate as sb_validate
from providers.production_pipeline import readiness as production_readiness, render_manifest as production_render_manifest
from providers.timeline_studio import build_multitrack as timeline_build, render_queue as timeline_queue, snapshot as timeline_snapshot, list_snapshots as timeline_snapshots, restore as timeline_restore
from providers.timeline_editor import edit_duration as timeline_edit_duration, nudge_shot as timeline_nudge, undo as timeline_undo, redo as timeline_redo, history_state as timeline_history_state
from providers.timeline_pro import session as timeline_pro_session, set_view as timeline_pro_set_view, precise_trim as timeline_pro_trim, add_marker as timeline_pro_add_marker, delete_marker as timeline_pro_delete_marker, move_shot as timeline_pro_move_shot, set_transition as timeline_pro_transition, set_audio as timeline_pro_audio, set_subtitle as timeline_pro_subtitle, render_range as timeline_pro_render_range, autosave as timeline_pro_autosave, recovery_status as timeline_pro_recovery_status, restore_recovery as timeline_pro_restore_recovery
from providers.advanced_timeline import studio as advanced_timeline_studio, ruler as advanced_timeline_ruler, waveform as advanced_timeline_waveform, snap as advanced_timeline_snap, selection as advanced_timeline_selection, batch_update as advanced_timeline_batch, set_keyframe as advanced_timeline_keyframe, delete_keyframe as advanced_timeline_delete_keyframe, track_state as advanced_timeline_track_state, ripple_trim as advanced_timeline_ripple_trim, proxy_preview as advanced_timeline_proxy_preview
from providers.timeline_stage33000 import studio as timeline33000_studio, scrub as timeline33000_scrub, playback as timeline33000_playback, color_tag as timeline33000_color_tag, curve as timeline33000_curve, proxy_cache as timeline33000_proxy_cache, cache_existing_proxy as timeline33000_cache_proxy
from providers.timeline_stage33500 import monitor as timeline33500_monitor, set_playhead as timeline33500_set_playhead, frame_step as timeline33500_frame_step, safe_area as timeline33500_safe_area, onion as timeline33500_onion, workspace as timeline33500_workspace, render_monitor_frame as timeline33500_render_frame
from providers.film import (
    FILM_PRESETS, EXPORT_PRESETS, ASPECT_RATIOS,
    build_plan as film_build_plan, status as film_status,
    create_render_job as film_create_render_job, read_job as film_read_job,
    list_jobs as film_list_jobs, cancel_job as film_cancel_job, retry_job as film_retry_job,
    read_plan as film_read_plan, update_shot as film_update_shot, read_log as film_read_log,
    attach_audio as film_attach_audio, mux_audio as film_mux_audio,
    apply_lipsync as film_apply_lipsync, create_project_archive as film_create_project_archive,
)

VERSION = "40000.0.0"
MAX_BYTES = int(os.getenv("FACELAB_MAX_UPLOAD_MB", "25")) * 1024 * 1024
MAX_PIXELS = int(os.getenv("FACELAB_MAX_PIXELS", "40000000"))
CACHE_ENABLED = os.getenv("FACELAB_CACHE", "1") == "1"
CACHE_TTL_HOURS = max(1, int(os.getenv("FACELAB_CACHE_TTL_HOURS", "24")))
MAX_CONCURRENT = max(1, int(os.getenv("FACELAB_MAX_CONCURRENT", "1")))
API_TOKEN = os.getenv("FACELAB_API_TOKEN", "").strip()
DATA_ROOT = Path(os.getenv("FACELAB_DATA_ROOT", tempfile.gettempdir())).expanduser()
DATA_ROOT.mkdir(parents=True, exist_ok=True)
CACHE = DATA_ROOT / "facelab_cache_v33000"
CACHE.mkdir(parents=True, exist_ok=True)
HERITAGE_DIR = Path(os.getenv("FACELAB_HERITAGE_DIR", str(DATA_ROOT / "heritage_backgrounds"))).expanduser()
HERITAGE_DIR.mkdir(parents=True, exist_ok=True)
FILM_JOBS = Path(os.getenv("FACELAB_FILM_DIR", str(DATA_ROOT / "film_jobs_v33000"))).expanduser()
FILM_JOBS.mkdir(parents=True, exist_ok=True)
AI_GATE = asyncio.Semaphore(MAX_CONCURRENT)
STARTED_AT = time.time()
RUNTIME_STATS = {"requests": 0, "cache_hits": 0, "cache_misses": 0, "errors": 0, "total_process_ms": 0}

app = FastAPI(title="NEXVARY FaceLab Self-Hosted API", version=VERSION)


@app.middleware("http")
async def reject_unsafe_path_segments(request, call_next):
    """Reject traversal/control characters before any filesystem-backed route runs."""
    raw = request.scope.get("raw_path", b"")
    try:
        decoded = unquote(raw.decode("utf-8", "strict"))
    except (UnicodeDecodeError, ValueError):
        return JSONResponse({"detail": "Invalid request path"}, status_code=400)
    if "\x00" in decoded or "\\" in decoded:
        return JSONResponse({"detail": "Unsafe request path"}, status_code=400)
    segments = decoded.split("/")
    if any(seg in {".", ".."} for seg in segments):
        return JSONResponse({"detail": "Unsafe request path"}, status_code=400)
    return await call_next(request)


def _auth(x_facelab_token: Optional[str] = Header(None)):
    if API_TOKEN and not hmac.compare_digest(x_facelab_token or "", API_TOKEN):
        raise HTTPException(401, "Invalid FaceLab token")


def _cleanup_cache():
    if not CACHE_ENABLED:
        return
    cutoff = time.time() - CACHE_TTL_HOURS * 3600
    for path in CACHE.glob("*.jpg"):
        try:
            if path.stat().st_mtime < cutoff:
                path.unlink(missing_ok=True)
        except OSError:
            pass


def _validate_image_bytes(data: bytes, label: str):
    if not data or len(data) > MAX_BYTES:
        raise HTTPException(413, f"{label} image is empty or too large")
    try:
        with Image.open(io.BytesIO(data)) as im:
            im.verify()
        with Image.open(io.BytesIO(data)) as im:
            w, h = im.size
            if w < 64 or h < 64:
                raise HTTPException(422, f"{label} image is too small")
            if w * h > MAX_PIXELS:
                raise HTTPException(413, f"{label} image exceeds pixel safety limit")
    except HTTPException:
        raise
    except (Image.DecompressionBombError, Image.DecompressionBombWarning) as exc:
        raise HTTPException(413, f"{label} image exceeds pixel safety limit") from exc
    except (UnidentifiedImageError, OSError, ValueError) as exc:
        raise HTTPException(415, f"Invalid {label} image") from exc


async def _read_upload(upload: UploadFile, label: str) -> tuple[bytes, str]:
    suffix = Path(upload.filename or f"{label}.jpg").suffix.lower() or ".jpg"
    if suffix not in {".jpg", ".jpeg", ".png", ".webp"}:
        raise HTTPException(415, f"Unsupported {label} image type")
    data = await upload.read(MAX_BYTES + 1)
    _validate_image_bytes(data, label)
    return data, suffix


async def _read_video_upload(upload: UploadFile, label: str) -> tuple[bytes, str]:
    suffix = Path(upload.filename or f"{label}.mp4").suffix.lower() or ".mp4"
    if suffix not in {".mp4", ".mov", ".mkv", ".webm"}:
        raise HTTPException(415, f"Unsupported {label} video type")
    data = await upload.read(120 * 1024 * 1024 + 1)
    if not data:
        raise HTTPException(400, f"Empty {label} upload")
    if len(data) > 120 * 1024 * 1024:
        raise HTTPException(413, f"{label} is too large")
    return data, suffix


@app.on_event("startup")
def startup_housekeeping():
    _cleanup_cache()


@app.get("/health")
def health():
    return {
        "ok": True,
        "version": VERSION,
        "billing": "self-hosted/no per-request API fee",
        "privacy": "temporary processing; cache configurable",
        "auth_enabled": bool(API_TOKEN),
        "providers": provider_status(),
    }


@app.get("/v1/capabilities", dependencies=[Depends(_auth)])
def capabilities():
    providers = provider_status()
    return {
        "version": VERSION,
        "effects": EFFECTS,
        "max_upload_mb": MAX_BYTES // 1024 // 1024,
        "max_pixels": MAX_PIXELS,
        "paid_api_required": False,
        "reference_image_supported": True,
        "cache_enabled": CACHE_ENABLED,
        "max_concurrent_jobs": MAX_CONCURRENT,
        "providers": providers,
        "generative_ready": providers.get("diffusion_face", {}).get("ready", False),
        "virtual_wardrobe": wardrobe_status(),
    }


@app.get("/v1/presets", dependencies=[Depends(_auth)])
def presets():
    return {
        "natural": {"strength": 0.38, "description": "subtle identity-preserving edit"},
        "balanced": {"strength": 0.65, "description": "default balanced edit"},
        "strong": {"strength": 0.88, "description": "stronger visible edit"},
    }


@app.get("/v1/license-report", dependencies=[Depends(_auth)])
def license_report():
    return {
        "policy": "self-hosted and no paid API dependency; redistribute only audited model weights",
        "providers": provider_status(),
        "commercial_release_rule": "code license and model-weight license must both be approved",
    }


@app.get("/v1/runtime", dependencies=[Depends(_auth)])
def runtime():
    _cleanup_cache()
    jpgs = list(CACHE.glob("*.jpg"))
    cache_bytes = sum(p.stat().st_size for p in jpgs if p.exists())
    processed = max(1, RUNTIME_STATS["cache_misses"])
    return {
        "version": VERSION,
        "uptime_seconds": int(time.time() - STARTED_AT),
        "requests": RUNTIME_STATS["requests"],
        "cache_hits": RUNTIME_STATS["cache_hits"],
        "cache_misses": RUNTIME_STATS["cache_misses"],
        "errors": RUNTIME_STATS["errors"],
        "average_process_ms": int(RUNTIME_STATS["total_process_ms"] / processed),
        "cache_files": len(jpgs),
        "cache_megabytes": round(cache_bytes / 1024 / 1024, 2),
        "max_concurrent_jobs": MAX_CONCURRENT,
    }




@app.get("/v1/system", dependencies=[Depends(_auth)])
def system_info():
    import platform
    return {
        "version": VERSION,
        "python": platform.python_version(),
        "platform": platform.platform(),
        "machine": platform.machine(),
        "processor": platform.processor(),
        "paid_api_required": False,
        "self_hosted": True,
    }


@app.get("/v1/storage", dependencies=[Depends(_auth)])
def storage_info():
    root = DATA_ROOT
    try:
        usage = shutil.disk_usage(root)
        return {
            "version": VERSION,
            "path": str(root),
            "total_gb": round(usage.total / 1024**3, 2),
            "used_gb": round(usage.used / 1024**3, 2),
            "free_gb": round(usage.free / 1024**3, 2),
            "cache_path": str(CACHE),
            "cache_enabled": CACHE_ENABLED,
        }
    except OSError as exc:
        raise HTTPException(500, f"Storage probe failed: {exc}")


@app.get("/v1/models", dependencies=[Depends(_auth)])
def models():
    status = provider_status()
    return {
        "version": VERSION,
        "policy": "self-hosted; no paid API dependency; weights must be license-audited before redistribution",
        "providers": status,
        "effects": EFFECTS,
    }




@app.get("/v1/film/status", dependencies=[Depends(_auth)])
def film_engine_status():
    return {"version": VERSION, **film_status()}


@app.get("/v1/film/doctor", dependencies=[Depends(_auth)])
def film_engine_doctor():
    return {"version": VERSION, **engine_doctor_report()}


@app.get("/v1/film/motions", dependencies=[Depends(_auth)])
def film_motion_presets():
    return {"version": VERSION, **motion_catalog()}


@app.get("/v1/film/driving-motions", dependencies=[Depends(_auth)])
def film_driving_motions():
    return {"version": VERSION, **driving_motion_catalog()}


@app.post("/v1/film/driving-motions", dependencies=[Depends(_auth)])
async def film_add_driving_motion(clip: UploadFile = File(...), motion_id: str = Form(...), label: str = Form(""), engine_hint: str = Form("mimicmotion")):
    data, suffix = await _read_video_upload(clip, "driving_motion")
    try:
        item = register_driving_motion(motion_id, data, suffix or ".mp4", label, engine_hint)
    except ValueError as exc:
        raise HTTPException(422, str(exc))
    return {"version": VERSION, "item": item, **driving_motion_catalog()}

@app.get("/v1/film/driving-motions/{motion_id}/preview", dependencies=[Depends(_auth)])
def film_driving_motion_preview(motion_id: str):
    path = resolve_driving_motion(motion_id)
    if not path: raise HTTPException(404, "Driving motion not found")
    if path.suffix.lower() == ".pkl": raise HTTPException(409, "Motion template has no raw-video preview")
    media = {".mp4":"video/mp4",".mov":"video/quicktime",".webm":"video/webm",".mkv":"video/x-matroska"}.get(path.suffix.lower(),"application/octet-stream")
    return FileResponse(path, media_type=media, filename=path.name)

@app.post("/v1/film/driving-motions/{motion_id}/template", dependencies=[Depends(_auth)])
async def film_make_motion_template(motion_id: str, source: UploadFile = File(...), delete_original: bool = Form(False)):
    data, _ = await _read_upload(source, "motion_template_source")
    try:
        item = make_liveportrait_template(motion_id, data, delete_original)
    except ValueError as exc: raise HTTPException(422, str(exc))
    except RuntimeError as exc: raise HTTPException(503, str(exc))
    return {"version": VERSION, "item": item, "privacy_mode": bool(delete_original), **driving_motion_catalog()}




@app.get("/v1/film/motion-library", dependencies=[Depends(_auth)])
def film_visual_motion_library():
    return {"version": VERSION, **visual_motion_catalog()}

@app.get("/v1/film/driving-motions/{motion_id}/thumbnail", dependencies=[Depends(_auth)])
def film_motion_thumbnail(motion_id: str):
    path = motion_thumbnail(motion_id)
    if not path: raise HTTPException(404, "Motion thumbnail unavailable")
    return FileResponse(path, media_type="image/jpeg", filename=path.name)

@app.get("/v1/film/director-presets", dependencies=[Depends(_auth)])
def film_director_presets():
    return {"version": VERSION, **director_catalog()}

@app.get("/v1/film/project-templates", dependencies=[Depends(_auth)])
def film_project_templates():
    return {"version": VERSION, **project_template_catalog()}

@app.get("/v1/film/project-templates/{template_id}", dependencies=[Depends(_auth)])
def film_project_template(template_id: str):
    item=project_template_get(template_id)
    if not item: raise HTTPException(404,"Template not found")
    return {"version":VERSION,"id":template_id,"template":item}

@app.get("/v1/film/jobs/{job_id}/continuity", dependencies=[Depends(_auth)])
def film_continuity_report(job_id: str):
    plan=film_read_plan(FILM_JOBS,job_id)
    if not plan: raise HTTPException(404,"Film job not found")
    return {"version":VERSION,"job_id":job_id,**continuity_analyze(plan)}

@app.get("/v1/film/jobs/{job_id}/audio-timeline", dependencies=[Depends(_auth)])
def film_audio_timeline(job_id: str, language: str="auto"):
    plan=film_read_plan(FILM_JOBS,job_id)
    if not plan: raise HTTPException(404,"Film job not found")
    return {"version":VERSION,"job_id":job_id,**audio_build_timeline(plan,language)}

@app.get("/v1/film/jobs/{job_id}/production-report", dependencies=[Depends(_auth)])
def film_production_report(job_id: str):
    plan=film_read_plan(FILM_JOBS,job_id); job=film_read_job(FILM_JOBS,job_id)
    if not plan or not job: raise HTTPException(404,"Film job not found")
    return {"version":VERSION,"job_id":job_id,"state":job.get("state"),"progress":job.get("progress"),"continuity":continuity_analyze(plan),"audio":audio_build_timeline(plan),"assets":{"video_ready":job.get("video_ready"),"thumbnail_ready":job.get("thumbnail_ready"),"subtitle_ready":job.get("subtitle_ready")},"personal_noncommercial":True}


def _save_storyboard(job_id: str, plan: dict):
    d = FILM_JOBS / job_id
    if not d.exists(): raise HTTPException(404, "Film job not found")
    import json
    tmp=d/"plan.json.tmp"; tmp.write_text(json.dumps(plan,ensure_ascii=False,indent=2),encoding="utf-8"); tmp.replace(d/"plan.json")
    try: timeline_snapshot(FILM_JOBS, job_id)
    except Exception: pass
    return plan

@app.post("/v1/film/jobs/{job_id}/storyboard/reorder", dependencies=[Depends(_auth)])
def storyboard_reorder(job_id: str, shot_ids: str = Form(...)):
    plan=film_read_plan(FILM_JOBS,job_id)
    if not plan: raise HTTPException(404,"Film job not found")
    try: ids=[x.strip() for x in shot_ids.split(',') if x.strip()]; return {"version":VERSION,**_save_storyboard(job_id,sb_reorder(plan,ids))}
    except ValueError as e: raise HTTPException(422,str(e))

@app.post("/v1/film/jobs/{job_id}/storyboard/{shot_id}/duplicate", dependencies=[Depends(_auth)])
def storyboard_duplicate(job_id:str,shot_id:str):
    plan=film_read_plan(FILM_JOBS,job_id)
    if not plan: raise HTTPException(404,"Film job not found")
    try:return {"version":VERSION,**_save_storyboard(job_id,sb_duplicate(plan,shot_id))}
    except KeyError:raise HTTPException(404,"Shot not found")

@app.post("/v1/film/jobs/{job_id}/storyboard/{shot_id}/split", dependencies=[Depends(_auth)])
def storyboard_split(job_id:str,shot_id:str):
    plan=film_read_plan(FILM_JOBS,job_id)
    if not plan: raise HTTPException(404,"Film job not found")
    try:return {"version":VERSION,**_save_storyboard(job_id,sb_split(plan,shot_id))}
    except KeyError:raise HTTPException(404,"Shot not found")

@app.post("/v1/film/jobs/{job_id}/storyboard/{shot_id}/delete", dependencies=[Depends(_auth)])
def storyboard_delete(job_id:str,shot_id:str):
    plan=film_read_plan(FILM_JOBS,job_id)
    if not plan: raise HTTPException(404,"Film job not found")
    try:return {"version":VERSION,**_save_storyboard(job_id,sb_remove(plan,shot_id))}
    except KeyError:raise HTTPException(404,"Shot not found")
    except ValueError as e:raise HTTPException(422,str(e))

@app.post("/v1/film/jobs/{job_id}/storyboard/batch", dependencies=[Depends(_auth)])
def storyboard_batch(job_id:str, shot_ids:str=Form(...), motion_preset:Optional[str]=Form(None), preferred_engine:Optional[str]=Form(None), camera_preset:Optional[str]=Form(None), transition:Optional[str]=Form(None), driving_motion_id:Optional[str]=Form(None)):
    plan=film_read_plan(FILM_JOBS,job_id)
    if not plan:raise HTTPException(404,"Film job not found")
    updates={k:v for k,v in locals().copy().items() if k in {'motion_preset','preferred_engine','camera_preset','transition','driving_motion_id'} and v is not None}
    return {"version":VERSION,**_save_storyboard(job_id,sb_batch_apply(plan,[x.strip() for x in shot_ids.split(',') if x.strip()],updates))}

@app.get("/v1/film/jobs/{job_id}/storyboard/validate", dependencies=[Depends(_auth)])
def storyboard_validate(job_id:str):
    plan=film_read_plan(FILM_JOBS,job_id)
    if not plan:raise HTTPException(404,"Film job not found")
    return {"version":VERSION,**sb_validate(plan)}

@app.get("/v1/film/jobs/{job_id}/render-manifest", dependencies=[Depends(_auth)])
def film_render_manifest(job_id:str):
    plan=film_read_plan(FILM_JOBS,job_id)
    if not plan:raise HTTPException(404,"Film job not found")
    return {"version":VERSION,**production_render_manifest(plan)}

@app.get("/v1/film/jobs/{job_id}/readiness", dependencies=[Depends(_auth)])
def film_readiness(job_id:str):
    plan=film_read_plan(FILM_JOBS,job_id)
    if not plan:raise HTTPException(404,"Film job not found")
    return {"version":VERSION,**production_readiness(plan,film_status())}

@app.get("/v1/film/presets", dependencies=[Depends(_auth)])
def film_presets():
    return {
        "version": VERSION, "presets": FILM_PRESETS,
        "export_presets": EXPORT_PRESETS, "aspect_ratios": list(ASPECT_RATIOS),
        "max_duration_seconds": 300, "min_duration_seconds": 30,
        "paid_api_required": False,
    }


@app.get("/v1/film/plan", dependencies=[Depends(_auth)])
def film_plan(story: str, duration_seconds: int = 60, preset: str = "cinematic", aspect_ratio: str = "16:9", motion_preset: str = ""):
    try:
        return {"version": VERSION, **film_build_plan(story, duration_seconds, preset, aspect_ratio, motion_preset)}
    except ValueError as exc:
        raise HTTPException(422, str(exc))


@app.post("/v1/film/render", dependencies=[Depends(_auth)])
async def film_render(
    source: UploadFile = File(...),
    story: str = Form(...),
    duration_seconds: int = Form(60),
    preset: str = Form("cinematic"),
    render_mode: str = Form("auto"),
    aspect_ratio: str = Form("16:9"),
    export_preset: str = Form("720p"),
    motion_preset: str = Form(""),
):
    data, _suffix = await _read_upload(source, "film_source")
    try:
        with Image.open(io.BytesIO(data)) as im:
            canvas = im.convert("RGB")
            buf = io.BytesIO(); canvas.save(buf, "JPEG", quality=94, optimize=True); normalized = buf.getvalue()
    except Exception as exc:
        raise HTTPException(415, "Invalid film source image") from exc
    try:
        job = film_create_render_job(FILM_JOBS, normalized, story, duration_seconds, preset, render_mode, aspect_ratio, export_preset, motion_preset)
    except ValueError as exc:
        raise HTTPException(422, str(exc))
    except RuntimeError as exc:
        raise HTTPException(503, str(exc))
    return {"version": VERSION, **job, "status_url": f"/v1/film/jobs/{job['job_id']}", "video_url": f"/v1/film/jobs/{job['job_id']}/video"}


@app.get("/v1/film/jobs", dependencies=[Depends(_auth)])
def film_jobs(limit: int = 30):
    return {"version": VERSION, "jobs": film_list_jobs(FILM_JOBS, limit)}



@app.get("/v1/film/jobs/{job_id}/timeline", dependencies=[Depends(_auth)])
def film_timeline(job_id: str):
    plan=film_read_plan(FILM_JOBS,job_id)
    if not plan: raise HTTPException(404,"Film job not found")
    return {"version":VERSION,**timeline_build(plan)}


@app.post("/v1/film/jobs/{job_id}/timeline/{shot_id}/duration", dependencies=[Depends(_auth)])
def film_timeline_duration(job_id:str, shot_id:str, duration_seconds:float=Form(...)):
    try: plan=timeline_edit_duration(FILM_JOBS,job_id,shot_id,duration_seconds); return {"version":VERSION,"job_id":job_id,**timeline_build(plan),"history":timeline_history_state(FILM_JOBS,job_id)}
    except FileNotFoundError: raise HTTPException(404,"Film job not found")
    except KeyError: raise HTTPException(404,"Shot not found")

@app.post("/v1/film/jobs/{job_id}/timeline/{shot_id}/nudge", dependencies=[Depends(_auth)])
def film_timeline_nudge(job_id:str, shot_id:str, direction:int=Form(...)):
    try: plan=timeline_nudge(FILM_JOBS,job_id,shot_id,direction); return {"version":VERSION,"job_id":job_id,**timeline_build(plan),"history":timeline_history_state(FILM_JOBS,job_id)}
    except FileNotFoundError: raise HTTPException(404,"Film job not found")
    except KeyError: raise HTTPException(404,"Shot not found")

@app.post("/v1/film/jobs/{job_id}/timeline/undo", dependencies=[Depends(_auth)])
def film_timeline_undo(job_id:str):
    try:
        r=timeline_undo(FILM_JOBS,job_id); return {"version":VERSION,"job_id":job_id,"changed":r["changed"],**timeline_build(r["plan"]),"history":{"undo_count":r["undo_count"],"redo_count":r["redo_count"],"can_undo":r["undo_count"]>0,"can_redo":r["redo_count"]>0}}
    except FileNotFoundError: raise HTTPException(404,"Film job not found")

@app.post("/v1/film/jobs/{job_id}/timeline/redo", dependencies=[Depends(_auth)])
def film_timeline_redo(job_id:str):
    try:
        r=timeline_redo(FILM_JOBS,job_id); return {"version":VERSION,"job_id":job_id,"changed":r["changed"],**timeline_build(r["plan"]),"history":{"undo_count":r["undo_count"],"redo_count":r["redo_count"],"can_undo":r["undo_count"]>0,"can_redo":r["redo_count"]>0}}
    except FileNotFoundError: raise HTTPException(404,"Film job not found")

@app.get("/v1/film/jobs/{job_id}/timeline/history", dependencies=[Depends(_auth)])
def film_timeline_history(job_id:str):
    try: return {"version":VERSION,"job_id":job_id,**timeline_history_state(FILM_JOBS,job_id)}
    except FileNotFoundError: raise HTTPException(404,"Film job not found")

@app.get("/v1/film/jobs/{job_id}/timeline/editor", dependencies=[Depends(_auth)])
def film_timeline_editor(job_id: str):
    try: return {"version": VERSION, **timeline_pro_session(FILM_JOBS, job_id)}
    except FileNotFoundError: raise HTTPException(404, "job not found")
    except Exception as e: raise HTTPException(400, str(e))

@app.post("/v1/film/jobs/{job_id}/timeline/view", dependencies=[Depends(_auth)])
def film_timeline_view(job_id: str, playhead_seconds: float | None = Form(None), zoom: float | None = Form(None), in_seconds: float | None = Form(None), out_seconds: float | None = Form(None), selected_shot_id: str | None = Form(None)):
    try: return {"version": VERSION, "job_id": job_id, "state": timeline_pro_set_view(FILM_JOBS, job_id, playhead_seconds, zoom, in_seconds, out_seconds, selected_shot_id)}
    except FileNotFoundError: raise HTTPException(404, "job not found")
    except Exception as e: raise HTTPException(400, str(e))

@app.post("/v1/film/jobs/{job_id}/timeline/{shot_id}/precise-trim", dependencies=[Depends(_auth)])
def film_timeline_precise_trim(job_id: str, shot_id: str, head_seconds: float = Form(0.0), tail_seconds: float = Form(0.0)):
    try: return {"version": VERSION, "job_id": job_id, **timeline_pro_trim(FILM_JOBS, job_id, shot_id, head_seconds, tail_seconds)}
    except FileNotFoundError: raise HTTPException(404, "job not found")
    except KeyError: raise HTTPException(404, "shot not found")
    except Exception as e: raise HTTPException(400, str(e))

@app.post("/v1/film/jobs/{job_id}/timeline/markers", dependencies=[Depends(_auth)])
def film_timeline_marker_add(job_id: str, at_seconds: float = Form(...), label: str = Form("Marker"), kind: str = Form("marker")):
    try: return {"version": VERSION, "job_id": job_id, "marker": timeline_pro_add_marker(FILM_JOBS, job_id, at_seconds, label, kind)}
    except Exception as e: raise HTTPException(400, str(e))

@app.post("/v1/film/jobs/{job_id}/timeline/markers/{marker_id}/delete", dependencies=[Depends(_auth)])
def film_timeline_marker_delete(job_id: str, marker_id: str):
    try: return {"version": VERSION, "job_id": job_id, **timeline_pro_delete_marker(FILM_JOBS, job_id, marker_id)}
    except Exception as e: raise HTTPException(400, str(e))

@app.post("/v1/film/jobs/{job_id}/timeline/{shot_id}/move", dependencies=[Depends(_auth)])
def film_timeline_move(job_id: str, shot_id: str, target_index: int = Form(...)):
    try: return {"version": VERSION, "job_id": job_id, **timeline_pro_move_shot(FILM_JOBS, job_id, shot_id, target_index)}
    except KeyError: raise HTTPException(404, "shot not found")
    except Exception as e: raise HTTPException(400, str(e))

@app.post("/v1/film/jobs/{job_id}/timeline/{shot_id}/transition", dependencies=[Depends(_auth)])
def film_timeline_transition(job_id: str, shot_id: str, transition: str = Form(...), duration_seconds: float = Form(0.0)):
    try: return {"version": VERSION, "job_id": job_id, **timeline_pro_transition(FILM_JOBS, job_id, shot_id, transition, duration_seconds)}
    except KeyError: raise HTTPException(404, "shot not found")
    except Exception as e: raise HTTPException(400, str(e))

@app.post("/v1/film/jobs/{job_id}/timeline/{shot_id}/audio", dependencies=[Depends(_auth)])
def film_timeline_audio(job_id: str, shot_id: str, gain_db: float = Form(0.0), fade_in: float = Form(0.0), fade_out: float = Form(0.0), muted: bool = Form(False)):
    try: return {"version": VERSION, "job_id": job_id, **timeline_pro_audio(FILM_JOBS, job_id, shot_id, gain_db, fade_in, fade_out, muted)}
    except KeyError: raise HTTPException(404, "shot not found")
    except Exception as e: raise HTTPException(400, str(e))

@app.post("/v1/film/jobs/{job_id}/timeline/{shot_id}/subtitle", dependencies=[Depends(_auth)])
def film_timeline_subtitle(job_id: str, shot_id: str, text: str = Form(""), offset_seconds: float = Form(0.0), enabled: bool = Form(True)):
    try: return {"version": VERSION, "job_id": job_id, **timeline_pro_subtitle(FILM_JOBS, job_id, shot_id, text, offset_seconds, enabled)}
    except KeyError: raise HTTPException(404, "shot not found")
    except Exception as e: raise HTTPException(400, str(e))

@app.get("/v1/film/jobs/{job_id}/timeline/render-range", dependencies=[Depends(_auth)])
def film_timeline_render_range(job_id: str):
    try: return {"version": VERSION, **timeline_pro_render_range(FILM_JOBS, job_id)}
    except Exception as e: raise HTTPException(400, str(e))

@app.post("/v1/film/jobs/{job_id}/timeline/autosave", dependencies=[Depends(_auth)])
def film_timeline_autosave(job_id: str):
    try: return {"version": VERSION, "job_id": job_id, **timeline_pro_autosave(FILM_JOBS, job_id)}
    except Exception as e: raise HTTPException(400, str(e))

@app.get("/v1/film/jobs/{job_id}/timeline/recovery", dependencies=[Depends(_auth)])
def film_timeline_recovery(job_id: str):
    try: return {"version": VERSION, "job_id": job_id, **timeline_pro_recovery_status(FILM_JOBS, job_id)}
    except Exception as e: raise HTTPException(400, str(e))

@app.post("/v1/film/jobs/{job_id}/timeline/recovery/restore", dependencies=[Depends(_auth)])
def film_timeline_recovery_restore(job_id: str):
    try: return {"version": VERSION, "job_id": job_id, **timeline_pro_restore_recovery(FILM_JOBS, job_id)}
    except FileNotFoundError: raise HTTPException(404, "recovery not found")
    except Exception as e: raise HTTPException(400, str(e))

@app.get("/v1/film/jobs/{job_id}/timeline/advanced", dependencies=[Depends(_auth)])
def film_timeline_advanced(job_id: str):
    try: return {"version": VERSION, **advanced_timeline_studio(FILM_JOBS, job_id)}
    except Exception as e: raise HTTPException(404 if isinstance(e, FileNotFoundError) else 400, str(e))

@app.get("/v1/film/jobs/{job_id}/timeline/ruler", dependencies=[Depends(_auth)])
def film_timeline_ruler(job_id: str, zoom: float = 1.0):
    try: return {"version": VERSION, "job_id": job_id, **advanced_timeline_ruler(FILM_JOBS, job_id, zoom)}
    except Exception as e: raise HTTPException(400, str(e))

@app.get("/v1/film/jobs/{job_id}/timeline/{shot_id}/waveform", dependencies=[Depends(_auth)])
def film_timeline_waveform(job_id: str, shot_id: str, samples: int = 96):
    try: return {"version": VERSION, "job_id": job_id, **advanced_timeline_waveform(FILM_JOBS, job_id, shot_id, samples)}
    except KeyError: raise HTTPException(404, "shot not found")
    except Exception as e: raise HTTPException(400, str(e))

@app.post("/v1/film/jobs/{job_id}/timeline/snap", dependencies=[Depends(_auth)])
def film_timeline_snap(job_id: str, seconds: float = Form(...), include_shots: bool = Form(True)):
    try: return {"version": VERSION, "job_id": job_id, **advanced_timeline_snap(FILM_JOBS, job_id, seconds, include_shots)}
    except Exception as e: raise HTTPException(400, str(e))

@app.post("/v1/film/jobs/{job_id}/timeline/selection", dependencies=[Depends(_auth)])
def film_timeline_selection(job_id: str, shot_ids: str = Form("")):
    try: return {"version": VERSION, "job_id": job_id, **advanced_timeline_selection(FILM_JOBS, job_id, [x for x in shot_ids.split(",") if x.strip()])}
    except Exception as e: raise HTTPException(400, str(e))

@app.post("/v1/film/jobs/{job_id}/timeline/batch", dependencies=[Depends(_auth)])
def film_timeline_batch(job_id: str, shot_ids: str = Form(""), transition: str | None = Form(None), audio_gain_db: float | None = Form(None), muted: bool | None = Form(None), motion_preset: str | None = Form(None), camera_preset: str | None = Form(None)):
    try: return {"version": VERSION, "job_id": job_id, **advanced_timeline_batch(FILM_JOBS, job_id, [x for x in shot_ids.split(",") if x.strip()], transition, audio_gain_db, muted, motion_preset, camera_preset)}
    except Exception as e: raise HTTPException(400, str(e))

@app.post("/v1/film/jobs/{job_id}/timeline/{shot_id}/camera-keyframe", dependencies=[Depends(_auth)])
def film_timeline_camera_keyframe(job_id: str, shot_id: str, at_seconds: float = Form(...), zoom: float | None = Form(None), pan_x: float | None = Form(None), pan_y: float | None = Form(None), rotation: float | None = Form(None), focus: float | None = Form(None), exposure: float | None = Form(None)):
    values={k:v for k,v in {"zoom":zoom,"pan_x":pan_x,"pan_y":pan_y,"rotation":rotation,"focus":focus,"exposure":exposure}.items() if v is not None}
    try: return {"version": VERSION, "job_id": job_id, **advanced_timeline_keyframe(FILM_JOBS, job_id, shot_id, "camera_keyframes", at_seconds, values)}
    except KeyError: raise HTTPException(404, "shot not found")
    except Exception as e: raise HTTPException(400, str(e))

@app.post("/v1/film/jobs/{job_id}/timeline/{shot_id}/motion-keyframe", dependencies=[Depends(_auth)])
def film_timeline_motion_keyframe(job_id: str, shot_id: str, at_seconds: float = Form(...), intensity: float | None = Form(None), speed: float | None = Form(None), expression: float | None = Form(None), body_strength: float | None = Form(None)):
    values={k:v for k,v in {"intensity":intensity,"speed":speed,"expression":expression,"body_strength":body_strength}.items() if v is not None}
    try: return {"version": VERSION, "job_id": job_id, **advanced_timeline_keyframe(FILM_JOBS, job_id, shot_id, "motion_keyframes", at_seconds, values)}
    except KeyError: raise HTTPException(404, "shot not found")
    except Exception as e: raise HTTPException(400, str(e))

@app.post("/v1/film/jobs/{job_id}/timeline/{shot_id}/keyframes/{bucket}/{keyframe_id}/delete", dependencies=[Depends(_auth)])
def film_timeline_keyframe_delete(job_id: str, shot_id: str, bucket: str, keyframe_id: str):
    bucket_name = "camera_keyframes" if bucket == "camera" else "motion_keyframes" if bucket == "motion" else bucket
    try: return {"version": VERSION, "job_id": job_id, **advanced_timeline_delete_keyframe(FILM_JOBS, job_id, shot_id, bucket_name, keyframe_id)}
    except Exception as e: raise HTTPException(400, str(e))

@app.post("/v1/film/jobs/{job_id}/timeline-controls/tracks/{track}", dependencies=[Depends(_auth)])
def film_timeline_track_state(job_id: str, track: str, locked: bool | None = Form(None), muted: bool | None = Form(None), solo: bool | None = Form(None)):
    try: return {"version": VERSION, "job_id": job_id, **advanced_timeline_track_state(FILM_JOBS, job_id, track, locked, muted, solo)}
    except Exception as e: raise HTTPException(400, str(e))

@app.post("/v1/film/jobs/{job_id}/timeline/{shot_id}/ripple-trim", dependencies=[Depends(_auth)])
def film_timeline_ripple_trim(job_id: str, shot_id: str, delta_seconds: float = Form(...)):
    try: return {"version": VERSION, "job_id": job_id, **advanced_timeline_ripple_trim(FILM_JOBS, job_id, shot_id, delta_seconds)}
    except PermissionError as e: raise HTTPException(423, str(e))
    except Exception as e: raise HTTPException(400, str(e))

@app.post("/v1/film/jobs/{job_id}/timeline/proxy-preview", dependencies=[Depends(_auth)])
def film_timeline_proxy_preview(job_id: str, start_seconds: float | None = Form(None), end_seconds: float | None = Form(None), width: int = Form(640), height: int = Form(360)):
    try:
        result=advanced_timeline_proxy_preview(FILM_JOBS, job_id, start_seconds, end_seconds, width, height)
        cache=timeline33000_cache_proxy(FILM_JOBS, job_id, start_seconds, end_seconds, width, height)
        return {"version": VERSION, "job_id": job_id, **result, "proxy_cache": cache}
    except Exception as e: raise HTTPException(400, str(e))

@app.get("/v1/film/jobs/{job_id}/timeline/stage33000", dependencies=[Depends(_auth)])
def film_timeline_stage33000(job_id: str):
    try: return {"version": VERSION, "job_id": job_id, **timeline33000_studio(FILM_JOBS, job_id)}
    except Exception as e: raise HTTPException(404 if isinstance(e, FileNotFoundError) else 400, str(e))

@app.post("/v1/film/jobs/{job_id}/timeline/scrub", dependencies=[Depends(_auth)])
def film_timeline_scrub(job_id: str, seconds: float = Form(...), snap_seconds: float | None = Form(None)):
    try: return {"version": VERSION, "job_id": job_id, **timeline33000_scrub(FILM_JOBS, job_id, seconds, snap_seconds)}
    except Exception as e: raise HTTPException(400, str(e))

@app.post("/v1/film/jobs/{job_id}/timeline/playback", dependencies=[Depends(_auth)])
def film_timeline_playback(job_id: str, command: str = Form(...)):
    try: return {"version": VERSION, "job_id": job_id, **timeline33000_playback(FILM_JOBS, job_id, command)}
    except Exception as e: raise HTTPException(400, str(e))

@app.post("/v1/film/jobs/{job_id}/timeline/color-tag", dependencies=[Depends(_auth)])
def film_timeline_color_tag(job_id: str, target_type: str = Form(...), target_id: str = Form(...), color: str = Form(...)):
    try: return {"version": VERSION, "job_id": job_id, **timeline33000_color_tag(FILM_JOBS, job_id, target_type, target_id, color)}
    except KeyError: raise HTTPException(404, "shot not found")
    except Exception as e: raise HTTPException(400, str(e))

@app.get("/v1/film/jobs/{job_id}/timeline/{shot_id}/curve", dependencies=[Depends(_auth)])
def film_timeline_curve(job_id: str, shot_id: str, bucket: str = "camera", property_name: str = "zoom", samples: int = 64):
    try: return {"version": VERSION, "job_id": job_id, **timeline33000_curve(FILM_JOBS, job_id, shot_id, bucket, property_name, samples)}
    except KeyError: raise HTTPException(404, "shot not found")
    except Exception as e: raise HTTPException(400, str(e))

@app.get("/v1/film/jobs/{job_id}/timeline/proxy-cache", dependencies=[Depends(_auth)])
def film_timeline_proxy_cache(job_id: str):
    try: return {"version": VERSION, "job_id": job_id, **timeline33000_proxy_cache(FILM_JOBS, job_id)}
    except Exception as e: raise HTTPException(400, str(e))

@app.post("/v1/film/jobs/{job_id}/timeline/proxy-cache/prune", dependencies=[Depends(_auth)])
def film_timeline_proxy_cache_prune(job_id: str):
    try: return {"version": VERSION, "job_id": job_id, **timeline33000_proxy_cache(FILM_JOBS, job_id, "prune")}
    except Exception as e: raise HTTPException(400, str(e))

@app.get("/v1/film/jobs/{job_id}/timeline/proxy-preview/video", dependencies=[Depends(_auth)])
def film_timeline_proxy_preview_video(job_id: str):
    d=FILM_JOBS/job_id; f=d/"timeline_range_proxy.mp4"
    if not f.exists() or f.stat().st_size == 0: raise HTTPException(404, "proxy preview not ready")
    return FileResponse(f, media_type="video/mp4", filename=f"{job_id}-timeline-proxy.mp4")

@app.get("/v1/film/jobs/{job_id}/timeline/monitor", dependencies=[Depends(_auth)])
def film_timeline_monitor(job_id: str):
    try: return {"version": VERSION, "job_id": job_id, **timeline33500_monitor(FILM_JOBS, job_id)}
    except Exception as e: raise HTTPException(404 if isinstance(e, FileNotFoundError) else 400, str(e))

@app.post("/v1/film/jobs/{job_id}/timeline/monitor/playhead", dependencies=[Depends(_auth)])
def film_timeline_monitor_playhead(job_id: str, seconds: float = Form(...), fps: float | None = Form(None)):
    try: return {"version": VERSION, "job_id": job_id, **timeline33500_set_playhead(FILM_JOBS, job_id, seconds, fps)}
    except Exception as e: raise HTTPException(400, str(e))

@app.post("/v1/film/jobs/{job_id}/timeline/monitor/frame-step", dependencies=[Depends(_auth)])
def film_timeline_monitor_frame_step(job_id: str, direction: int = Form(...), frames: int = Form(1), fps: float | None = Form(None)):
    try: return {"version": VERSION, "job_id": job_id, **timeline33500_frame_step(FILM_JOBS, job_id, direction, frames, fps)}
    except Exception as e: raise HTTPException(400, str(e))

@app.post("/v1/film/jobs/{job_id}/timeline/monitor/safe-area", dependencies=[Depends(_auth)])
def film_timeline_monitor_safe_area(job_id: str, preset: str = Form(...)):
    try: return {"version": VERSION, "job_id": job_id, "safe_area": timeline33500_safe_area(FILM_JOBS, job_id, preset)}
    except Exception as e: raise HTTPException(400, str(e))

@app.post("/v1/film/jobs/{job_id}/timeline/monitor/onion", dependencies=[Depends(_auth)])
def film_timeline_monitor_onion(job_id: str, enabled: bool = Form(...), frames: int = Form(1), opacity: float = Form(0.35)):
    try: return {"version": VERSION, "job_id": job_id, "onion": timeline33500_onion(FILM_JOBS, job_id, enabled, frames, opacity)}
    except Exception as e: raise HTTPException(400, str(e))

@app.post("/v1/film/jobs/{job_id}/timeline/workspace", dependencies=[Depends(_auth)])
def film_timeline_workspace(job_id: str, preset: str | None = Form(None), monitor: float | None = Form(None), timeline: float | None = Form(None), inspector: float | None = Form(None)):
    try: return {"version": VERSION, "job_id": job_id, "workspace": timeline33500_workspace(FILM_JOBS, job_id, preset, monitor, timeline, inspector)}
    except Exception as e: raise HTTPException(400, str(e))

@app.get("/v1/film/jobs/{job_id}/timeline/monitor/frame", dependencies=[Depends(_auth)])
def film_timeline_monitor_frame(job_id: str, width: int = 960, height: int = 540):
    try:
        out=FILM_JOBS/job_id/'monitor'/'frame.jpg'
        timeline33500_render_frame(FILM_JOBS, job_id, out, width, height)
        return FileResponse(out, media_type="image/jpeg", filename=f"{job_id}-monitor.jpg")
    except FileNotFoundError as e: raise HTTPException(409, str(e))
    except Exception as e: raise HTTPException(400, str(e))

@app.get("/v1/film/render-queue", dependencies=[Depends(_auth)])
def film_render_queue():
    return {"version":VERSION,**timeline_queue(FILM_JOBS)}

@app.post("/v1/film/jobs/{job_id}/snapshot", dependencies=[Depends(_auth)])
def film_project_snapshot(job_id:str):
    try: return {"version":VERSION,**timeline_snapshot(FILM_JOBS,job_id)}
    except FileNotFoundError: raise HTTPException(404,"Film job not found")

@app.get("/v1/film/jobs/{job_id}/snapshots", dependencies=[Depends(_auth)])
def film_project_snapshots(job_id:str):
    if not (FILM_JOBS/job_id).exists(): raise HTTPException(404,"Film job not found")
    return {"version":VERSION,"job_id":job_id,"snapshots":timeline_snapshots(FILM_JOBS,job_id)}

@app.post("/v1/film/jobs/{job_id}/snapshots/{snapshot_id}/restore", dependencies=[Depends(_auth)])
def film_project_restore(job_id:str,snapshot_id:str):
    try: return {"version":VERSION,**timeline_restore(FILM_JOBS,job_id,snapshot_id)}
    except FileNotFoundError: raise HTTPException(404,"Snapshot not found")

@app.get("/v1/film/jobs/{job_id}/shots/{shot_id}/preview", dependencies=[Depends(_auth)])
def film_shot_preview(job_id:str,shot_id:str):
    path=FILM_JOBS/job_id/'shots'/f'{shot_id}.mp4'
    if not path.exists(): raise HTTPException(409,"Shot preview is not ready")
    return FileResponse(path,media_type="video/mp4",filename=f"{shot_id}.mp4")

@app.get("/v1/film/jobs/{job_id}", dependencies=[Depends(_auth)])
def film_job_status(job_id: str):
    job = film_read_job(FILM_JOBS, job_id)
    if not job:
        raise HTTPException(404, "Film job not found")
    return {"version": VERSION, **job, "video_url": f"/v1/film/jobs/{job_id}/video" if job.get("video_ready") else None}


@app.get("/v1/film/jobs/{job_id}/plan", dependencies=[Depends(_auth)])
def film_job_plan(job_id: str):
    plan = film_read_plan(FILM_JOBS, job_id)
    if not plan:
        raise HTTPException(404, "Film job not found")
    return {"version": VERSION, **plan}


@app.post("/v1/film/jobs/{job_id}/shots/{shot_id}", dependencies=[Depends(_auth)])
def film_edit_shot(
    job_id: str, shot_id: str,
    prompt: Optional[str] = Form(None), negative_prompt: Optional[str] = Form(None),
    camera: Optional[str] = Form(None), dialogue: Optional[str] = Form(None),
    narration: Optional[str] = Form(None), transition: Optional[str] = Form(None),
    enabled: Optional[bool] = Form(None), duration_seconds: Optional[int] = Form(None),
    motion_preset: Optional[str] = Form(None), driving_motion_id: Optional[str] = Form(None),
    preferred_engine: Optional[str] = Form(None), camera_preset: Optional[str] = Form(None),
):
    updates = {k:v for k,v in {
        "prompt": prompt, "negative_prompt": negative_prompt, "camera": camera,
        "dialogue": dialogue, "narration": narration, "transition": transition,
        "enabled": enabled, "duration_seconds": duration_seconds,
        "motion_preset": motion_preset, "driving_motion_id": driving_motion_id,
        "preferred_engine": preferred_engine, "camera_preset": camera_preset,
    }.items() if v is not None}
    try:
        plan = film_update_shot(FILM_JOBS, job_id, shot_id, updates)
        try: timeline_snapshot(FILM_JOBS, job_id)
        except Exception: pass
        return {"version": VERSION, **plan}
    except FileNotFoundError:
        raise HTTPException(404, "Film job not found")
    except KeyError:
        raise HTTPException(404, "Shot not found")


@app.post("/v1/film/jobs/{job_id}/cancel", dependencies=[Depends(_auth)])
def film_cancel(job_id: str):
    job = film_cancel_job(FILM_JOBS, job_id)
    if not job:
        raise HTTPException(404, "Film job not found")
    return {"version": VERSION, **job}


@app.post("/v1/film/jobs/{job_id}/retry", dependencies=[Depends(_auth)])
def film_retry(job_id: str):
    try:
        return {"version": VERSION, **film_retry_job(FILM_JOBS, job_id)}
    except FileNotFoundError:
        raise HTTPException(404, "Film job not found")


@app.get("/v1/film/jobs/{job_id}/log", dependencies=[Depends(_auth)])
def film_job_log(job_id: str):
    log = film_read_log(FILM_JOBS, job_id)
    if log is None:
        raise HTTPException(404, "Film job not found")
    return {"version": VERSION, "job_id": job_id, "log": log}


@app.post("/v1/film/jobs/{job_id}/audio", dependencies=[Depends(_auth)])
async def film_job_audio(job_id: str, audio: UploadFile = File(...), kind: str = Form("narration")):
    data = await audio.read()
    try:
        return {"version": VERSION, **film_attach_audio(FILM_JOBS, job_id, data, kind)}
    except FileNotFoundError:
        raise HTTPException(404, "Film job not found")
    except ValueError as exc:
        raise HTTPException(422, str(exc))


@app.post("/v1/film/jobs/{job_id}/postprocess", dependencies=[Depends(_auth)])
def film_job_postprocess(job_id: str):
    try:
        return {"version": VERSION, **film_mux_audio(FILM_JOBS, job_id)}
    except FileNotFoundError:
        raise HTTPException(404, "Film job not found")
    except RuntimeError as exc:
        raise HTTPException(409, str(exc))


@app.post("/v1/film/jobs/{job_id}/lipsync", dependencies=[Depends(_auth)])
def film_job_lipsync(job_id: str):
    try:
        return {"version": VERSION, **film_apply_lipsync(FILM_JOBS, job_id)}
    except FileNotFoundError:
        raise HTTPException(404, "Film job not found")
    except RuntimeError as exc:
        raise HTTPException(409, str(exc))


@app.get("/v1/film/jobs/{job_id}/video", dependencies=[Depends(_auth)])
def film_job_video(job_id: str):
    job = film_read_job(FILM_JOBS, job_id)
    if not job:
        raise HTTPException(404, "Film job not found")
    if job.get("state") != "completed" or not job.get("video_ready"):
        raise HTTPException(409, "Film video is not ready")
    path = FILM_JOBS / job_id / "output.mp4"
    return FileResponse(path, media_type="video/mp4", filename=f"facelab-{job_id}.mp4")


@app.get("/v1/film/jobs/{job_id}/thumbnail", dependencies=[Depends(_auth)])
def film_job_thumbnail(job_id: str):
    job = film_read_job(FILM_JOBS, job_id)
    if not job:
        raise HTTPException(404, "Film job not found")
    path = FILM_JOBS / job_id / "thumbnail.jpg"
    if not path.exists():
        raise HTTPException(409, "Thumbnail is not ready")
    return FileResponse(path, media_type="image/jpeg", filename=f"facelab-{job_id}.jpg")


@app.get("/v1/film/jobs/{job_id}/subtitles", dependencies=[Depends(_auth)])
def film_job_subtitles(job_id: str):
    job = film_read_job(FILM_JOBS, job_id)
    if not job:
        raise HTTPException(404, "Film job not found")
    path = FILM_JOBS / job_id / "subtitles.srt"
    if not path.exists():
        raise HTTPException(409, "Subtitles are not ready")
    return FileResponse(path, media_type="application/x-subrip", filename=f"facelab-{job_id}.srt")


@app.get("/v1/film/jobs/{job_id}/archive", dependencies=[Depends(_auth)])
def film_job_archive(job_id: str):
    try:
        path = film_create_project_archive(FILM_JOBS, job_id)
        return FileResponse(path, media_type="application/zip", filename=path.name)
    except FileNotFoundError:
        raise HTTPException(404, "Film job not found")


@app.get("/v1/heritage/catalog", dependencies=[Depends(_auth)])
def heritage_catalog():
    return {"version":VERSION,"engine":heritage_status(),"sites":[{"id":k,"country":v[0],"city":v[1],"name":v[2]} for k,v in HERITAGE_SITES.items()],"countries":["Egypt","Türkiye","Algeria","Saudi Arabia","Morocco","Tunisia","Italy","Spain","Greece"],"custom_background_supported":True,"paid_api_required":False}


@app.get("/v1/heritage/packs", dependencies=[Depends(_auth)])
def heritage_packs():
    available = []
    missing = []
    for site_id in HERITAGE_SITES:
        found = None
        for ext in (".jpg", ".jpeg", ".png", ".webp"):
            candidate = HERITAGE_DIR / f"{site_id}{ext}"
            if candidate.exists():
                found = candidate
                break
        (available if found else missing).append(site_id)
    return {
        "version": VERSION,
        "heritage_dir": str(HERITAGE_DIR),
        "available": available,
        "missing": missing,
        "available_count": len(available),
        "total_sites": len(HERITAGE_SITES),
        "license_manifest": str(HERITAGE_DIR / "licenses.json"),
    }

@app.get("/v1/heritage/background/{site_id}", dependencies=[Depends(_auth)])
def heritage_background(site_id: str):
    if site_id not in HERITAGE_SITES:
        raise HTTPException(404, "Unknown heritage site")
    for ext in (".jpg", ".jpeg", ".png", ".webp"):
        candidate = HERITAGE_DIR / f"{site_id}{ext}"
        if candidate.exists():
            return FileResponse(candidate)
    raise HTTPException(404, "No licensed background installed for this site")

@app.post("/v1/heritage/composite", dependencies=[Depends(_auth)])
async def heritage_apply(person:UploadFile=File(...), background:UploadFile=File(...), site_id:str=Form(...), mode:str=Form("portrait"), lighting:str=Form("day"), strength:float=Form(.7)):
    if site_id not in HERITAGE_SITES: raise HTTPException(400,"Unsupported heritage site")
    if mode not in {"portrait","fullbody","travel","cinematic"}: raise HTTPException(400,"Unsupported heritage mode")
    if lighting not in {"day","sunset","night"}: raise HTTPException(400,"Unsupported lighting")
    strength=max(0.,min(1.,strength)); RUNTIME_STATS["requests"]+=1
    pdata,psuffix=await _read_upload(person,"person"); bdata,bsuffix=await _read_upload(background,"background")
    rid=uuid.uuid4().hex[:12]; key=hashlib.sha256(pdata+bdata+site_id.encode()+mode.encode()+lighting.encode()+f"{strength:.3f}".encode()).hexdigest(); cached=CACHE/f"heritage_{key}.jpg"
    if CACHE_ENABLED and cached.exists(): RUNTIME_STATS["cache_hits"]+=1; return FileResponse(cached,media_type="image/jpeg",headers={"X-FaceLab-Cache":"HIT","X-Request-ID":rid,"X-FaceLab-Version":VERSION})
    work=Path(tempfile.mkdtemp(prefix=f"facelab_heritage_{rid}_"))
    try:
        src=work/f"person{psuffix}"; src.write_bytes(pdata); bg=work/f"background{bsuffix}"; bg.write_bytes(bdata); started=time.perf_counter();RUNTIME_STATS["cache_misses"]+=1
        async with AI_GATE: out=await asyncio.to_thread(heritage_composite,src,bg,work,mode,lighting,strength)
        elapsed=int((time.perf_counter()-started)*1000);RUNTIME_STATS["total_process_ms"]+=elapsed; normalized=_normalize_output(out,cached); return FileResponse(normalized,media_type="image/jpeg",headers={"X-FaceLab-Cache":"MISS","X-Request-ID":rid,"X-Process-MS":str(elapsed),"X-FaceLab-Version":VERSION})
    except Exception as exc: RUNTIME_STATS["errors"]+=1; raise HTTPException(500,str(exc))
    finally: shutil.rmtree(work,ignore_errors=True)

@app.get("/v1/wardrobe/status", dependencies=[Depends(_auth)])
def wardrobe_engine_status():
    return {"version": VERSION, **wardrobe_status()}


@app.get("/v1/wardrobe/catalog", dependencies=[Depends(_auth)])
def wardrobe_catalog():
    return {
        "version": VERSION,
        "styles": [{"id": key, "prompt": prompt} for key, prompt in WARDROBE_STYLES.items()],
        "count": len(WARDROBE_STYLES),
        "categories": 8,
                "custom_garment_supported": True,
        "outfit_composition": True,
        "ai_stylist": True,
        "accessory_types": ["glasses","hat","watch","scarf","shoes","bag","necklace"],
        "paid_api_required": False,
    }



@app.get("/v1/wardrobe/stylist", dependencies=[Depends(_auth)])
def wardrobe_stylist(occasion: str = "work", gender: str = "all", climate: str = "mild"):
    """Rule-assisted self-hosted stylist endpoint. No external API call is made."""
    occasion = occasion.lower().strip()
    gender = gender.lower().strip()
    climate = climate.lower().strip()
    table = {
        "work": ("business_executive", "navy", "watch"),
        "wedding": (("evening_gown" if gender in {"women","woman","female"} else "formal_tuxedo"), "black", "watch"),
        "travel": ("business_smart", "beige", "glasses"),
        "photo": ("business_creative", "auto", "watch"),
        "sport": ("sport_gym", "black", "shoes"),
        "winter": ("winter_wool_coat", "navy", "scarf"),
        "evening": (("evening_gown" if gender in {"women","woman","female"} else "evening_dinner_suit"), "black", "watch"),
    }
    style_id, color, accessory = table.get(occasion, table["work"])
    if climate in {"hot","summer","warm"} and occasion not in {"wedding","evening"}:
        style_id, color, accessory = ("summer_linen", "white", "glasses")
    if climate in {"cold","snow"}:
        style_id, color, accessory = ("winter_wool_coat", "navy", "scarf")
    return {
        "version": VERSION,
        "style_id": style_id,
        "color": color,
        "accessory": accessory,
        "occasion": occasion,
        "engine": "FaceLab local stylist rules",
        "paid_api_required": False,
    }

@app.get("/v1/wardrobe/stylist/recommendations", dependencies=[Depends(_auth)])
def wardrobe_stylist_recommendations(occasion: str = "work", gender: str = "all", climate: str = "mild", limit: int = 3):
    """Return several local stylist candidates without calling any paid/external API."""
    occasion = occasion.lower().strip()
    gender = gender.lower().strip()
    climate = climate.lower().strip()
    female = gender in {"women", "woman", "female"}
    table = {
        "work": [
            ("business_executive", "navy", "watch"),
            ("business_smart", "beige", "none"),
            ("formal_navy_suit", "navy", "watch"),
        ],
        "wedding": ([
            ("evening_gown", "black", "necklace"),
            ("evening_red_dress", "red", "necklace"),
            ("evening_gold", "gold", "necklace"),
        ] if female else [
            ("formal_tuxedo", "black", "watch"),
            ("formal_three_piece", "navy", "watch"),
            ("formal_black_suit", "black", "none"),
        ]),
        "travel": [
            ("business_smart", "beige", "glasses"),
            ("casual_bomber", "navy", "shoes"),
            ("summer_resort", "white", "glasses"),
        ],
        "photo": [
            ("business_creative", "auto", "watch"),
            ("formal_blazer", "navy", "none"),
            ("evening_premium", "black", "watch"),
        ],
        "sport": [
            ("sport_gym", "black", "shoes"),
            ("sport_running", "blue", "shoes"),
            ("sport_tracksuit", "navy", "shoes"),
        ],
        "winter": [
            ("winter_wool_coat", "navy", "scarf"),
            ("winter_trench", "beige", "scarf"),
            ("winter_puffer", "black", "shoes"),
        ],
        "evening": ([
            ("evening_gown", "black", "necklace"),
            ("evening_red_dress", "red", "necklace"),
            ("evening_silver", "silver", "necklace"),
        ] if female else [
            ("evening_dinner_suit", "black", "watch"),
            ("evening_velvet", "navy", "watch"),
            ("formal_tuxedo", "black", "none"),
        ]),
    }
    picks = list(table.get(occasion, table["work"]))
    if climate in {"hot", "summer", "warm"} and occasion not in {"wedding", "evening"}:
        picks = [("summer_linen", "white", "glasses"), ("summer_resort", "beige", "none"), ("summer_white", "white", "shoes")]
    elif climate in {"cold", "snow"}:
        picks = [("winter_wool_coat", "navy", "scarf"), ("winter_trench", "beige", "scarf"), ("winter_puffer", "black", "shoes")]
    limit = max(1, min(int(limit), 5))
    return {
        "version": VERSION,
        "occasion": occasion,
        "gender": gender,
        "climate": climate,
        "recommendations": [
            {"rank": i + 1, "style_id": style, "color": color, "accessory": accessory}
            for i, (style, color, accessory) in enumerate(picks[:limit])
        ],
        "engine": "FaceLab local multi-look stylist rules",
        "paid_api_required": False,
    }

@app.post("/v1/wardrobe/try-on", dependencies=[Depends(_auth)])
async def wardrobe_try_on(
    person: UploadFile = File(...),
    style_id: str = Form(...),
    category: str = Form("formal"),
    strength: float = Form(0.65),
    color: str = Form("auto"),
    accessory: str = Form("none"),
    occasion: str = Form("work"),
    outfit: str = Form(""),
    garment: Optional[UploadFile] = File(None),
):
    strength = max(0.0, min(1.0, strength))
    if style_id not in WARDROBE_STYLES:
        raise HTTPException(400, f"Unsupported wardrobe style: {style_id}")
    RUNTIME_STATS["requests"] += 1
    person_data, person_suffix = await _read_upload(person, "person")
    garment_data = b""
    garment_suffix = ".jpg"
    if garment is not None:
        garment_data, garment_suffix = await _read_upload(garment, "garment")
    request_id = uuid.uuid4().hex[:12]
    key = hashlib.sha256(person_data + garment_data + style_id.encode() + category.encode() + color.encode() + accessory.encode() + occasion.encode() + outfit.encode() + f"{strength:.3f}".encode()).hexdigest()
    cached = CACHE / f"wardrobe_{key}.jpg"
    if CACHE_ENABLED and cached.exists():
        RUNTIME_STATS["cache_hits"] += 1
        return FileResponse(cached, media_type="image/jpeg", filename="facelab-wardrobe.jpg", headers={"X-FaceLab-Cache":"HIT","X-Request-ID":request_id,"X-FaceLab-Version":VERSION})
    work = Path(tempfile.mkdtemp(prefix=f"facelab_wardrobe_{request_id}_"))
    try:
        src = work / f"person{person_suffix}"; src.write_bytes(person_data)
        garment_path = None
        if garment_data:
            garment_path = work / f"garment{garment_suffix}"; garment_path.write_bytes(garment_data)
        started = time.perf_counter(); RUNTIME_STATS["cache_misses"] += 1
        async with AI_GATE:
            out = await asyncio.to_thread(run_wardrobe, src, style_id, category, strength, work, garment_path, color, accessory, occasion, outfit)
        if not out.exists(): raise HTTPException(500, "Wardrobe provider did not produce an output image")
        normalized = work / "wardrobe-normalized.jpg"
        with Image.open(out) as result: result.convert("RGB").save(normalized, "JPEG", quality=95, optimize=True)
        target = cached if CACHE_ENABLED else CACHE / f"wardrobe_response_{request_id}.jpg"; shutil.copy2(normalized, target)
        ms = int((time.perf_counter()-started)*1000); RUNTIME_STATS["total_process_ms"] += ms
        return FileResponse(target, media_type="image/jpeg", filename="facelab-wardrobe.jpg", headers={"X-FaceLab-Cache":"MISS" if CACHE_ENABLED else "OFF","X-Request-ID":request_id,"X-Process-MS":str(ms),"X-FaceLab-Version":VERSION})
    except HTTPException: raise
    except RuntimeError as exc:
        RUNTIME_STATS["errors"] += 1; raise HTTPException(503, str(exc))
    except Exception as exc:
        RUNTIME_STATS["errors"] += 1; raise HTTPException(500, f"Wardrobe processing failed: {exc}")
    finally:
        shutil.rmtree(work, ignore_errors=True)


@app.post("/v1/edit", dependencies=[Depends(_auth)])
async def edit(
    image: UploadFile = File(...),
    effect: str = Form(...),
    strength: float = Form(0.65),
    reference: Optional[UploadFile] = File(None),
):
    effect = effect.lower().strip()
    strength = max(0.0, min(1.0, strength))
    RUNTIME_STATS["requests"] += 1
    if effect not in EFFECTS:
        raise HTTPException(400, f"Unsupported effect: {effect}")

    data, suffix = await _read_upload(image, "input")
    ref_data = b""
    ref_suffix = ".jpg"
    if reference is not None:
        ref_data, ref_suffix = await _read_upload(reference, "reference")

    request_id = uuid.uuid4().hex[:12]
    key = hashlib.sha256(data + ref_data + effect.encode() + f"{strength:.3f}".encode()).hexdigest()
    cached = CACHE / f"{key}.jpg"
    if CACHE_ENABLED and cached.exists():
        RUNTIME_STATS["cache_hits"] += 1
        return FileResponse(
            cached,
            media_type="image/jpeg",
            filename="facelab-result.jpg",
            headers={"X-FaceLab-Cache": "HIT", "X-Request-ID": request_id, "X-FaceLab-Version": VERSION},
        )

    work = Path(tempfile.mkdtemp(prefix=f"facelab_{request_id}_"))
    try:
        src = work / f"input{suffix}"
        src.write_bytes(data)
        ref_path = None
        if ref_data:
            ref_path = work / f"reference{ref_suffix}"
            ref_path.write_bytes(ref_data)

        started = time.perf_counter()
        RUNTIME_STATS["cache_misses"] += 1
        async with AI_GATE:
            out = await asyncio.to_thread(run_effect, src, effect, strength, work, ref_path)
        if not out.exists():
            raise HTTPException(500, "Provider did not produce an output image")

        # Re-encode to JPEG: normalizes output and strips source metadata/EXIF.
        normalized = work / "normalized.jpg"
        with Image.open(out) as result:
            result.convert("RGB").save(normalized, "JPEG", quality=95, optimize=True)

        target = cached if CACHE_ENABLED else CACHE / f"response_{request_id}.jpg"
        shutil.copy2(normalized, target)
        ms = int((time.perf_counter() - started) * 1000)
        RUNTIME_STATS["total_process_ms"] += ms
        return FileResponse(
            target,
            media_type="image/jpeg",
            filename="facelab-result.jpg",
            headers={
                "X-FaceLab-Cache": "MISS" if CACHE_ENABLED else "OFF",
                "X-Request-ID": request_id,
                "X-Process-MS": str(ms),
                "X-FaceLab-Version": VERSION,
            },
            background=None if CACHE_ENABLED else None,
        )
    except HTTPException:
        raise
    except RuntimeError as exc:
        RUNTIME_STATS["errors"] += 1
        raise HTTPException(503, str(exc))
    except Exception as exc:
        RUNTIME_STATS["errors"] += 1
        raise HTTPException(500, f"Processing failed: {exc}")
    finally:
        shutil.rmtree(work, ignore_errors=True)
