from __future__ import annotations
import json, tempfile
from pathlib import Path

from providers.timeline_pro import (
    session, set_view, precise_trim, add_marker, move_shot, set_transition,
    set_audio, set_subtitle, render_range, autosave, recovery_status, restore_recovery
)


def seed(root: Path) -> str:
    jid='12345678-1234-1234-1234-123456789abc'
    d=root/jid; d.mkdir(parents=True)
    plan={
        'story':'Stage 27500 QA','duration_seconds':15,'shot_count':3,'shots':[
            {'id':'shot-001','duration_seconds':5,'camera':'close','transition':'cut','motion_preset':'idle_natural','dialogue':'Hello'},
            {'id':'shot-002','duration_seconds':5,'camera':'medium','transition':'cut','motion_preset':'wave'},
            {'id':'shot-003','duration_seconds':5,'camera':'wide','transition':'cut','motion_preset':'walk_forward'},
        ]
    }
    (d/'plan.json').write_text(json.dumps(plan),encoding='utf-8')
    (d/'job.json').write_text(json.dumps({'job_id':jid,'state':'completed','progress':100,'shot_count':3}),encoding='utf-8')
    return jid


def main():
    with tempfile.TemporaryDirectory() as td:
        root=Path(td); jid=seed(root)
        s=session(root,jid); assert s['shot_count']==3 and s['validation']['healthy']
        st=set_view(root,jid,playhead=4.25,zoom=2.5,in_seconds=1.0,out_seconds=12.0,selected_shot_id='shot-002'); assert st['zoom']==2.5 and st['playhead_seconds']==4.25
        r=precise_trim(root,jid,'shot-001',0.5,0.75); assert abs(r['plan']['shots'][0]['duration_seconds']-3.75)<0.01
        m=add_marker(root,jid,2.0,'Intro','chapter'); assert m['kind']=='chapter'
        mv=move_shot(root,jid,'shot-003',0); assert mv['plan']['shots'][0]['id']=='shot-003'
        tr=set_transition(root,jid,'shot-002','fade',0.5); sh=next(x for x in tr['plan']['shots'] if x['id']=='shot-002'); assert sh['transition']=='fade' and sh['transition_duration']==0.5
        au=set_audio(root,jid,'shot-002',-3.0,0.2,0.3,False); sh=next(x for x in au['plan']['shots'] if x['id']=='shot-002'); assert sh['audio_gain_db']==-3.0
        su=set_subtitle(root,jid,'shot-002','Subtitle QA',0.15,True); sh=next(x for x in su['plan']['shots'] if x['id']=='shot-002'); assert sh['subtitle_text']=='Subtitle QA'
        set_view(root,jid,in_seconds=0.5,out_seconds=6.5)
        rr=render_range(root,jid); assert rr['ready'] and rr['shot_count']>=1 and abs(rr['range']['duration']-6.0)<0.01
        a=autosave(root,jid); assert a['saved']
        rs=recovery_status(root,jid); assert rs['available'] and not rs['dirty']
        set_audio(root,jid,'shot-002',7.0,0,0,True)
        restored=restore_recovery(root,jid); assert restored['restored']
        print('Stage 27500 timeline pro QA: PASS')

if __name__=='__main__': main()
