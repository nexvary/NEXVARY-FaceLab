from pathlib import Path
import tempfile, json
from providers.timeline_editor import edit_duration, nudge_shot, undo, redo, history_state
from providers.timeline_studio import build_multitrack

with tempfile.TemporaryDirectory() as td:
    root=Path(td); job=root/'job-1'; job.mkdir()
    plan={'shots':[{'id':'shot-1','duration_seconds':5,'camera_preset':'close','motion_preset':'smile'},{'id':'shot-2','duration_seconds':6,'camera_preset':'wide','motion_preset':'walk_forward'}]}
    (job/'plan.json').write_text(json.dumps(plan),encoding='utf-8')
    edited=edit_duration(root,'job-1','shot-1',8.5)
    assert edited['shots'][0]['duration_seconds']==8.5
    assert history_state(root,'job-1')['can_undo']
    moved=nudge_shot(root,'job-1','shot-1',1)
    assert moved['shots'][1]['id']=='shot-1'
    u=undo(root,'job-1'); assert u['changed'] and u['plan']['shots'][0]['id']=='shot-1'
    r=redo(root,'job-1'); assert r['changed'] and r['plan']['shots'][1]['id']=='shot-1'
    tl=build_multitrack(r['plan']); assert tl['track_count']==4 and tl['shot_count']==2
print('Stage 22500 timeline editor QA: PASS')
