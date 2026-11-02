import json, os, re, shutil, subprocess, sys, tempfile
from pathlib import Path
MAX_BYTES = 120 * 1024 * 1024

def root(): return Path(os.getenv("FACELAB_DRIVING_MOTIONS_DIR", "driving_motions")).expanduser()
def _manifest_path():
    r=root(); r.mkdir(parents=True,exist_ok=True); return r / "manifest.json"
def _read():
    mf=_manifest_path()
    if not mf.exists(): mf.write_text(json.dumps({"version":2,"items":[]},indent=2),encoding="utf-8")
    try: data=json.loads(mf.read_text(encoding="utf-8"))
    except Exception: data={"version":2,"items":[]}
    if not isinstance(data.get("items"),list): data["items"]=[]
    return data

def catalog():
    r=root(); data=_read(); items=[]
    for x in data.get("items",[]):
        y=dict(x); p=r/str(y.get("file","")); y["ready"]=p.is_file() and p.stat().st_size>0; y["bytes"]=p.stat().st_size if y["ready"] else 0; y["kind"] = y.get("kind") or ("motion_template" if p.suffix.lower()==".pkl" else "driving_video"); items.append(y)
    return {"root":str(r.resolve()),"count":len(items),"ready":sum(bool(x["ready"]) for x in items),"items":items}

def resolve(motion_id):
    for x in catalog()["items"]:
        if x.get("id")==motion_id and x.get("ready"): return root()/x["file"]
    return None

def register(motion_id: str, data: bytes, suffix: str = ".mp4", label: str = "", engine_hint: str = "mimicmotion", kind: str = ""):
    motion_id=(motion_id or "").strip().lower()
    if not re.fullmatch(r"[a-z0-9][a-z0-9_-]{1,47}", motion_id): raise ValueError("motion_id must use 2-48 lowercase letters, numbers, _ or -")
    if not data or len(data)>MAX_BYTES: raise ValueError("driving clip/template must be 1 byte to 120 MB")
    ext=(suffix or ".mp4").lower()
    if ext not in {".mp4",".mov",".mkv",".webm",".pkl"}: ext=".mp4"
    r=root(); r.mkdir(parents=True,exist_ok=True); filename=motion_id+ext; (r/filename).write_bytes(data)
    manifest=_read(); items=[x for x in manifest.get("items",[]) if x.get("id")!=motion_id]
    inferred_kind=kind or ("motion_template" if ext==".pkl" else "driving_video")
    item={"id":motion_id,"label":(label or motion_id.replace("_"," ")).strip()[:80],"file":filename,"engine_hint":engine_hint[:32] or "mimicmotion","kind":inferred_kind,"source":"user-provided/private","license_note":"Use only clips you have permission to use."}
    items.append(item); manifest["version"]=2; manifest["items"]=items; _manifest_path().write_text(json.dumps(manifest,indent=2,ensure_ascii=False),encoding="utf-8")
    return {**item,"ready":True,"bytes":len(data)}

def make_liveportrait_template(motion_id: str, source_image: bytes, delete_original: bool = False):
    src_motion=resolve(motion_id)
    if not src_motion or src_motion.suffix.lower()==".pkl": raise ValueError("motion_id must point to a registered driving video")
    repo=Path(os.getenv("FACELAB_LIVEPORTRAIT_DIR","")).expanduser()
    script=repo/"inference.py"
    if not script.is_file(): raise RuntimeError("LivePortrait inference.py not found; configure FACELAB_LIVEPORTRAIT_DIR")
    py_env=os.getenv("FACELAB_LIVEPORTRAIT_PYTHON","").strip()
    if py_env: python=Path(py_env)
    else:
        py=(repo/".venv"/"Scripts"/"python.exe") if os.name=="nt" else (repo/".venv"/"bin"/"python")
        python=py if py.exists() else Path(sys.executable)
    before={p.resolve() for p in repo.rglob("*.pkl")}
    with tempfile.TemporaryDirectory(prefix="facelab-motion-template-") as td:
        src=Path(td)/"source.jpg"; src.write_bytes(source_image)
        cmd=[str(python),str(script),"-s",str(src.resolve()),"-d",str(src_motion.resolve()),"--flag_crop_driving_video"]
        proc=subprocess.run(cmd,cwd=repo,stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True,timeout=int(os.getenv("FACELAB_VIDEO_ENGINE_TIMEOUT","2400")))
        if proc.returncode: raise RuntimeError((proc.stderr or proc.stdout)[-4000:])
    candidates=[p for p in repo.rglob("*.pkl") if p.resolve() not in before]
    if not candidates: candidates=sorted(repo.rglob("*.pkl"),key=lambda p:p.stat().st_mtime,reverse=True)
    else: candidates=sorted(candidates,key=lambda p:p.stat().st_mtime,reverse=True)
    if not candidates: raise RuntimeError("LivePortrait finished but no motion template (.pkl) was produced")
    tpl_id=f"{motion_id}_template"[:48]
    item=register(tpl_id,candidates[0].read_bytes(),".pkl",f"{motion_id.replace('_',' ')} template","liveportrait","motion_template")
    if delete_original:
        try: src_motion.unlink(missing_ok=True)
        except OSError: pass
        data=_read(); data["items"]=[x for x in data["items"] if x.get("id")!=motion_id]; _manifest_path().write_text(json.dumps(data,indent=2,ensure_ascii=False),encoding="utf-8")
    return item
