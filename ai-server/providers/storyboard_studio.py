from __future__ import annotations
import copy, time
from typing import Any

def _reflow(plan: dict[str,Any])->dict[str,Any]:
    cur=0
    for i,s in enumerate(plan.get('shots',[]),1):
        s['shot']=i; s['start_second']=cur; cur+=max(1,int(s.get('duration_seconds',1)))
    plan['shot_count']=len(plan.get('shots',[])); plan['duration_seconds']=cur; plan['storyboard_updated_at']=time.time()
    return plan

def reorder(plan:dict[str,Any], shot_ids:list[str])->dict[str,Any]:
    shots=plan.get('shots',[]); by={s.get('id'):s for s in shots}
    if set(shot_ids)!=set(by) or len(shot_ids)!=len(shots): raise ValueError('shot_ids must contain every shot exactly once')
    plan['shots']=[by[x] for x in shot_ids]; return _reflow(plan)

def duplicate(plan:dict[str,Any], shot_id:str)->dict[str,Any]:
    shots=plan.get('shots',[])
    for i,s in enumerate(shots):
        if s.get('id')==shot_id:
            n=copy.deepcopy(s); n['id']=f"{shot_id}-copy-{int(time.time()*1000)%1000000}"; n['purpose']=str(n.get('purpose',''))+' (duplicate)'; shots.insert(i+1,n); return _reflow(plan)
    raise KeyError('shot not found')

def remove(plan:dict[str,Any],shot_id:str)->dict[str,Any]:
    old=len(plan.get('shots',[])); plan['shots']=[s for s in plan.get('shots',[]) if s.get('id')!=shot_id]
    if len(plan['shots'])==old: raise KeyError('shot not found')
    if not plan['shots']: raise ValueError('storyboard must keep at least one shot')
    return _reflow(plan)

def split(plan:dict[str,Any],shot_id:str)->dict[str,Any]:
    shots=plan.get('shots',[])
    for i,s in enumerate(shots):
        if s.get('id')==shot_id:
            d=max(2,int(s.get('duration_seconds',2))); a=d//2; b=d-a
            s['duration_seconds']=a; n=copy.deepcopy(s); n['id']=f"{shot_id}-b-{int(time.time()*1000)%1000000}"; n['duration_seconds']=b; n['purpose']=str(n.get('purpose',''))+' — continuation'; shots.insert(i+1,n); return _reflow(plan)
    raise KeyError('shot not found')

def batch_apply(plan:dict[str,Any], shot_ids:list[str], updates:dict[str,Any])->dict[str,Any]:
    allowed={'motion_preset','driving_motion_id','preferred_engine','camera_preset','transition','color_grade','speed','enabled'}
    clean={k:v for k,v in updates.items() if k in allowed}
    selected=set(shot_ids)
    for s in plan.get('shots',[]):
        if s.get('id') in selected: s.update(clean)
    return _reflow(plan)

def validate(plan:dict[str,Any])->dict[str,Any]:
    issues=[]; ids=set()
    for i,s in enumerate(plan.get('shots',[]),1):
        sid=s.get('id');
        if not sid or sid in ids: issues.append({'shot':i,'severity':'error','message':'missing or duplicate shot id'})
        ids.add(sid)
        if int(s.get('duration_seconds',0))<2: issues.append({'shot':i,'severity':'warning','message':'very short shot'})
        if not s.get('prompt'): issues.append({'shot':i,'severity':'warning','message':'empty prompt'})
        if s.get('dialogue') and not s.get('preferred_engine'): issues.append({'shot':i,'severity':'info','message':'dialogue shot can benefit from portrait/lipsync engine'})
    return {'healthy':not any(x['severity']=='error' for x in issues),'issues':issues,'shot_count':len(plan.get('shots',[]))}
