from __future__ import annotations
import json, time
from pathlib import Path
from typing import Any

HISTORY_LIMIT = 30

def _job_dir(root: Path, job_id: str) -> Path:
    d = root / job_id
    if not d.exists(): raise FileNotFoundError(job_id)
    return d

def _read_plan(d: Path) -> dict[str, Any]:
    p=d/'plan.json'
    if not p.exists(): raise FileNotFoundError('plan.json')
    return json.loads(p.read_text(encoding='utf-8'))

def _write_plan(d: Path, plan: dict[str, Any]) -> None:
    tmp=d/'plan.json.tmp'; tmp.write_text(json.dumps(plan,ensure_ascii=False,indent=2),encoding='utf-8'); tmp.replace(d/'plan.json')

def _history_file(d: Path) -> Path: return d/'timeline_history.json'

def _load_history(d: Path) -> dict[str, Any]:
    f=_history_file(d)
    if not f.exists(): return {'undo':[],'redo':[]}
    try:
        x=json.loads(f.read_text(encoding='utf-8'))
        if not isinstance(x,dict): raise ValueError
        return {'undo':list(x.get('undo') or []), 'redo':list(x.get('redo') or [])}
    except Exception: return {'undo':[],'redo':[]}

def _save_history(d: Path, h: dict[str, Any]) -> None:
    h['undo']=h.get('undo',[])[-HISTORY_LIMIT:]; h['redo']=h.get('redo',[])[-HISTORY_LIMIT:]
    _history_file(d).write_text(json.dumps(h,ensure_ascii=False),encoding='utf-8')

def _push_undo(d: Path, plan: dict[str, Any]) -> None:
    h=_load_history(d); h['undo'].append({'at':time.time(),'plan':plan}); h['redo']=[]; _save_history(d,h)

def _rebuild(plan: dict[str, Any]) -> dict[str, Any]:
    cursor=0.0
    for i,s in enumerate(plan.get('shots', [])):
        dur=max(1.0,float(s.get('duration_seconds',5) or 5))
        s['duration_seconds']=round(dur,2); s['index']=i; s['start_seconds']=round(cursor,2); cursor+=dur; s['end_seconds']=round(cursor,2)
    plan['duration_seconds']=round(cursor,2); plan['shot_count']=len(plan.get('shots',[]))
    return plan

def edit_duration(root: Path, job_id: str, shot_id: str, duration_seconds: float) -> dict[str, Any]:
    d=_job_dir(root,job_id); plan=_read_plan(d); shots=plan.get('shots',[])
    shot=next((s for s in shots if str(s.get('id'))==shot_id),None)
    if shot is None: raise KeyError(shot_id)
    _push_undo(d, json.loads(json.dumps(plan)))
    shot['duration_seconds']=round(max(1.0,min(60.0,float(duration_seconds))),2)
    _write_plan(d,_rebuild(plan)); return plan

def nudge_shot(root: Path, job_id: str, shot_id: str, direction: int) -> dict[str, Any]:
    d=_job_dir(root,job_id); plan=_read_plan(d); shots=plan.get('shots',[])
    idx=next((i for i,s in enumerate(shots) if str(s.get('id'))==shot_id),None)
    if idx is None: raise KeyError(shot_id)
    target=max(0,min(len(shots)-1,idx+(1 if direction>0 else -1)))
    if target==idx: return _rebuild(plan)
    _push_undo(d, json.loads(json.dumps(plan))); shots[idx],shots[target]=shots[target],shots[idx]
    _write_plan(d,_rebuild(plan)); return plan

def undo(root: Path, job_id: str) -> dict[str, Any]:
    d=_job_dir(root,job_id); current=_read_plan(d); h=_load_history(d)
    if not h['undo']: return {'changed':False,'plan':current,'undo_count':0,'redo_count':len(h['redo'])}
    entry=h['undo'].pop(); h['redo'].append({'at':time.time(),'plan':current}); plan=_rebuild(entry['plan']); _write_plan(d,plan); _save_history(d,h)
    return {'changed':True,'plan':plan,'undo_count':len(h['undo']),'redo_count':len(h['redo'])}

def redo(root: Path, job_id: str) -> dict[str, Any]:
    d=_job_dir(root,job_id); current=_read_plan(d); h=_load_history(d)
    if not h['redo']: return {'changed':False,'plan':current,'undo_count':len(h['undo']),'redo_count':0}
    entry=h['redo'].pop(); h['undo'].append({'at':time.time(),'plan':current}); plan=_rebuild(entry['plan']); _write_plan(d,plan); _save_history(d,h)
    return {'changed':True,'plan':plan,'undo_count':len(h['undo']),'redo_count':len(h['redo'])}

def history_state(root: Path, job_id: str) -> dict[str, Any]:
    d=_job_dir(root,job_id); h=_load_history(d)
    return {'undo_count':len(h['undo']),'redo_count':len(h['redo']),'can_undo':bool(h['undo']),'can_redo':bool(h['redo'])}
