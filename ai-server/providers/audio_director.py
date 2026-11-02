from typing import Any

def build_timeline(plan:dict[str,Any], language:str='auto')->dict[str,Any]:
    events=[]; cursor=0.0
    for shot in plan.get('shots',[]):
        if not shot.get('enabled',True): continue
        dur=float(shot.get('duration_seconds') or 1)
        for kind in ('dialogue','narration'):
            text=str(shot.get(kind) or '').strip()
            if text:
                words=len(text.split()); est=min(dur,max(1.0,words/2.4))
                events.append({'shot_id':shot.get('id'),'kind':kind,'text':text,'start_second':round(cursor,2),'max_seconds':round(est,2),'language':language})
        cursor+=dur
    return {'schema_version':2,'duration_seconds':round(cursor,2),'event_count':len(events),'events':events,'mix':{'dialogue_db':0,'narration_db':-1,'music_db':-18,'duck_music_under_voice':True},'tts_required':False,'paid_api_required':False}
