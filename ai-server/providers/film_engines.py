import json
import os
import shutil
from dataclasses import dataclass, asdict
from pathlib import Path
from typing import Any

@dataclass(frozen=True)
class FilmEngine:
    id: str
    name: str
    role: str
    command_env: str
    home_env: str
    license: str
    usage: str
    priority: int

ENGINES = [
    FilmEngine('wan22','Wan 2.2','cinematic image-to-video','FACELAB_WAN22_CMD_JSON','FACELAB_WAN22_DIR','Apache-2.0','general cinematic/full-body motion',10),
    FilmEngine('mimicmotion','MimicMotion','pose-guided body animation','FACELAB_MIMICMOTION_CMD_JSON','FACELAB_MIMICMOTION_DIR','project license; intended here only under non-commercial/private profile','walking, gestures, body pose control',20),
    FilmEngine('liveportrait','LivePortrait','portrait/face animation','FACELAB_LIVEPORTRAIT_CMD_JSON','FACELAB_LIVEPORTRAIT_DIR','MIT code; InsightFace model assets are non-commercial research','close-up face/head/expression motion',30),
    FilmEngine('echomimicv2','EchoMimic V2','audio-driven semi-body animation','FACELAB_ECHOMIMICV2_CMD_JSON','FACELAB_ECHOMIMICV2_DIR','Apache-2.0','talking semi-body shots',40),
    FilmEngine('musetalk','MuseTalk','lip sync','FACELAB_MUSETALK_CMD_JSON','FACELAB_MUSETALK_DIR','project/dependency licenses apply','lip-sync post-processing',50),
    FilmEngine('ffmpeg','FFmpeg','assembly/post-production','FACELAB_FFMPEG','','LGPL/GPL depends on build','assembly, audio, subtitles, preview',100),
]

def _cmd(name: str) -> list[str] | None:
    raw=os.getenv(name,'').strip()
    if not raw:
        return None
    try: value=json.loads(raw)
    except Exception: return None
    if not isinstance(value,list) or not value or not all(isinstance(x,str) and x for x in value):
        return None
    return value

def engine_state(e: FilmEngine) -> dict[str,Any]:
    if e.id=='ffmpeg':
        exe=os.getenv('FACELAB_FFMPEG','').strip() or 'ffmpeg'
        ready=bool(shutil.which(exe) or Path(exe).expanduser().is_file())
        return {**asdict(e),'ready':ready,'command_configured':ready,'home_exists':ready}
    home=os.getenv(e.home_env,'').strip()
    return {**asdict(e),'ready':_cmd(e.command_env) is not None,'command_configured':_cmd(e.command_env) is not None,'home_exists':bool(home and Path(home).expanduser().exists())}

def status() -> dict[str,Any]:
    return {'profile':'personal-friends-noncommercial','engines':[engine_state(e) for e in ENGINES]}

def choose_engine(shot: dict[str,Any]) -> str:
    preferred=str(shot.get("preferred_engine","")).strip().lower()
    if preferred:
        for e in ENGINES:
            if e.id==preferred and (e.id=="ffmpeg" or _cmd(e.command_env)):
                return e.id
    text=' '.join(str(shot.get(k,'')) for k in ('purpose','prompt','dialogue','narration')).lower()
    if shot.get('dialogue') or any(x in text for x in ('talk','speak','dialogue','conversation','يتحدث','يقول')):
        if _cmd('FACELAB_ECHOMIMICV2_CMD_JSON'): return 'echomimicv2'
        if _cmd('FACELAB_LIVEPORTRAIT_CMD_JSON'): return 'liveportrait'
    if any(x in text for x in ('walk','body','gesture','pose','يمشي','حركة الجسم','يلوح')) and _cmd('FACELAB_MIMICMOTION_CMD_JSON'):
        return 'mimicmotion'
    if any(x in text for x in ('close-up','face','reaction','portrait','وجه','تعبير')) and _cmd('FACELAB_LIVEPORTRAIT_CMD_JSON'):
        return 'liveportrait'
    if _cmd('FACELAB_WAN22_CMD_JSON'): return 'wan22'
    for eid, env in [('mimicmotion','FACELAB_MIMICMOTION_CMD_JSON'),('liveportrait','FACELAB_LIVEPORTRAIT_CMD_JSON'),('echomimicv2','FACELAB_ECHOMIMICV2_CMD_JSON')]:
        if _cmd(env): return eid
    return 'legacy'

def command_for(engine_id: str) -> list[str] | None:
    for e in ENGINES:
        if e.id==engine_id and e.command_env:
            return _cmd(e.command_env)
    return None
