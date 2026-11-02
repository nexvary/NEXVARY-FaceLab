from __future__ import annotations
import json, tempfile
from pathlib import Path
from PIL import Image
from fastapi.testclient import TestClient
import app as server

root=Path(tempfile.mkdtemp(prefix='facelab32500_'))
server.FILM_JOBS=root
job='qa32500'; d=root/job; d.mkdir(parents=True)
plan={
  'id':job,'duration_seconds':10,'shots':[
    {'id':'shot-001','duration_seconds':4.0,'camera_preset':'portrait_closeup','motion_preset':'idle_natural','transition':'cut','dialogue':'hello'},
    {'id':'shot-002','duration_seconds':6.0,'camera_preset':'travel_reveal','motion_preset':'walk_forward','transition':'fade','dialogue':'world'}
  ]
}
(d/'plan.json').write_text(json.dumps(plan),encoding='utf-8')
Image.new('RGB',(640,360),(30,40,55)).save(d/'source.jpg')
(d/'timeline_editor_state.json').write_text(json.dumps({'zoom':2.0,'in_seconds':0.5,'out_seconds':4.0,'markers':[{'id':'m1','at_seconds':2.0,'label':'beat'}]}),encoding='utf-8')

c=TestClient(server.app)
def ok(r, code=200):
    assert r.status_code==code, (r.status_code,r.text)
    return r.json() if 'application/json' in r.headers.get('content-type','') else r.content

x=ok(c.get(f'/v1/film/jobs/{job}/timeline/advanced')); assert x['capabilities']['waveform'] and x['shot_count']==2
x=ok(c.get(f'/v1/film/jobs/{job}/timeline/ruler?zoom=4')); assert x['tick_step'] <= 0.5 and len(x['ticks'])>3
x=ok(c.get(f'/v1/film/jobs/{job}/timeline/shot-001/waveform?samples=64')); assert x['sample_count']==64 and len(x['samples'])==64
x=ok(c.post(f'/v1/film/jobs/{job}/timeline/snap',data={'seconds':'2.08','include_shots':'true'})); assert x['snapped'] and abs(x['snapped_seconds']-2.0)<.001
x=ok(c.post(f'/v1/film/jobs/{job}/timeline/selection',data={'shot_ids':'shot-001,shot-002'})); assert x['count']==2
x=ok(c.post(f'/v1/film/jobs/{job}/timeline/batch',data={'shot_ids':'shot-001,shot-002','transition':'dissolve','audio_gain_db':'-2'})); assert x['changed']==2
x=ok(c.post(f'/v1/film/jobs/{job}/timeline/shot-001/camera-keyframe',data={'at_seconds':'1.0','zoom':'1.2','pan_x':'0.1'})); assert len(x['keyframes'])==1
x=ok(c.post(f'/v1/film/jobs/{job}/timeline/shot-001/motion-keyframe',data={'at_seconds':'2.0','intensity':'1.3','speed':'0.9'})); assert len(x['keyframes'])==1
x=ok(c.post(f'/v1/film/jobs/{job}/timeline-controls/tracks/audio',data={'muted':'true'})); assert x['state']['muted'] is True
x=ok(c.post(f'/v1/film/jobs/{job}/timeline/shot-001/ripple-trim',data={'delta_seconds':'0.5'})); assert abs(x['new_duration']-4.5)<.001
r=c.post(f'/v1/film/jobs/{job}/timeline/proxy-preview',data={'start_seconds':'0','end_seconds':'2','width':'320','height':'180'})
if r.status_code==200 and r.json().get('ready'):
    v=c.get(f'/v1/film/jobs/{job}/timeline/proxy-preview/video'); assert v.status_code==200 and len(v.content)>1000
print('FaceLab Stage 32500 QA: PASS')
