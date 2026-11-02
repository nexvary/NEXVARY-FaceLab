from typing import Any

def analyze(plan:dict[str,Any])->dict[str,Any]:
    shots=[s for s in plan.get('shots',[]) if s.get('enabled',True)]
    issues=[]; score=100
    required={'character','face','wardrobe','location'}
    engines=[]; motions=[]
    for s in shots:
        missing=sorted(required-set(s.get('locks') or []))
        if missing: issues.append({'shot_id':s.get('id'),'type':'missing_locks','missing':missing}); score-=4*len(missing)
        if not str(s.get('prompt','')).strip(): issues.append({'shot_id':s.get('id'),'type':'empty_prompt'}); score-=8
        if int(s.get('duration_seconds') or 0)>12: issues.append({'shot_id':s.get('id'),'type':'long_ai_shot','recommendation':'split to <=12s'}); score-=2
        if s.get('preferred_engine'): engines.append(s['preferred_engine'])
        if s.get('motion_preset'): motions.append(s['motion_preset'])
    if len(set(engines))>3: issues.append({'type':'engine_fragmentation','count':len(set(engines))}); score-=5
    return {'score':max(0,min(100,score)),'grade':'A' if score>=90 else 'B' if score>=80 else 'C' if score>=65 else 'D','issue_count':len(issues),'issues':issues,'shot_count':len(shots),'engines':sorted(set(engines)),'motions':sorted(set(motions)),'recommendation':'ready' if score>=85 else 'review flagged shots'}
