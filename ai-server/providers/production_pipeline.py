from __future__ import annotations
from typing import Any

def readiness(plan:dict[str,Any], engine_status:dict[str,Any])->dict[str,Any]:
    shots=plan.get('shots',[]); enabled=[s for s in shots if s.get('enabled',True)]
    dialogue=sum(bool(s.get('dialogue')) for s in enabled); motion=sum(bool(s.get('motion_preset')) for s in enabled)
    configured=[e.get('name') for e in engine_status.get('multi_engine',{}).get('engines',[]) if e.get('configured') or e.get('ready')]
    score=25
    if enabled: score+=20
    if all(s.get('prompt') for s in enabled): score+=15
    if motion: score+=10
    if configured: score+=15
    if engine_status.get('ffmpeg_ready'): score+=15
    return {'score':min(100,score),'enabled_shots':len(enabled),'dialogue_shots':dialogue,'motion_shots':motion,'configured_engines':configured,'ffmpeg_ready':bool(engine_status.get('ffmpeg_ready')),'ready_for_preview':bool(enabled and engine_status.get('ffmpeg_ready'))}

def render_manifest(plan:dict[str,Any])->dict[str,Any]:
    return {'schema_version':1,'shots':[{'id':s.get('id'),'order':i+1,'duration':s.get('duration_seconds'),'engine':s.get('preferred_engine') or 'auto','motion':s.get('motion_preset') or 'idle_natural','driving_motion_id':s.get('driving_motion_id') or '', 'camera':s.get('camera_preset') or s.get('camera') or '', 'transition':s.get('transition') or 'cut','dialogue':bool(s.get('dialogue'))} for i,s in enumerate(plan.get('shots',[])) if s.get('enabled',True)]}
