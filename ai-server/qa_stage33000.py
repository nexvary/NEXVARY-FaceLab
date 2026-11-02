from pathlib import Path
import json, tempfile
from providers.timeline_stage33000 import scrub,playback,color_tag,curve,proxy_cache,cache_existing_proxy,studio
root=Path(tempfile.mkdtemp(prefix='facelab33000_')); d=root/'qa33000'; d.mkdir(parents=True)
plan={"shots":[
 {"id":"shot_001","duration_seconds":4.0,"start_seconds":0.0,"end_seconds":4.0,"camera_keyframes":[{"id":"a","at_seconds":0.0,"values":{"zoom":1.0}},{"id":"b","at_seconds":4.0,"values":{"zoom":2.0}}]},
 {"id":"shot_002","duration_seconds":3.0,"start_seconds":4.0,"end_seconds":7.0}
]}
(d/'plan.json').write_text(json.dumps(plan),encoding='utf-8')
r=scrub(root,'qa33000',2.2); assert r['active_shot_id']=='shot_001'
assert playback(root,'qa33000','L')['playback']['rate']==1.0
assert playback(root,'qa33000','L')['playback']['rate']==2.0
assert playback(root,'qa33000','J')['playback']['rate']==-1.0
assert playback(root,'qa33000','K')['playback']['rate']==0.0
assert color_tag(root,'qa33000','shot','shot_001','cyan')['tag']['hex']=='#72E6FF'
c=curve(root,'qa33000','shot_001','camera','zoom',32); assert c['sample_count']==32 and abs(c['curve'][0]['value']-1.0)<0.01 and abs(c['curve'][-1]['value']-2.0)<0.01
(d/'timeline_range_proxy.mp4').write_bytes(b'0'*2048)
pc=cache_existing_proxy(root,'qa33000',0,4,640,360); assert pc['cached'] and proxy_cache(root,'qa33000')['entries']==1
assert proxy_cache(root,'qa33000','prune')['removed']==1
st=studio(root,'qa33000'); assert st['stage']==33000 and 'palette' in st
print('FaceLab Stage 33000 provider QA: PASS')
