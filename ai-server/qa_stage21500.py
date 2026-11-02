import tempfile, json
from pathlib import Path
from providers.film import build_plan, status
from providers.storyboard_studio import reorder, duplicate, split, remove, batch_apply, validate
from providers.production_pipeline import readiness, render_manifest
p=build_plan('A friend walks, waves, then looks at camera',40,'cinematic','16:9','idle_natural')
ids=[x['id'] for x in p['shots']]
assert len(ids)>=5
p=reorder(p,list(reversed(ids))); assert p['shots'][0]['id']==ids[-1]
p=duplicate(p,p['shots'][0]['id']); assert p['shot_count']==len(ids)+1
sid=p['shots'][0]['id']; p=split(p,sid); assert p['shot_count']==len(ids)+2
p=batch_apply(p,[p['shots'][0]['id']],{'motion_preset':'wave','preferred_engine':'mimicmotion','camera_preset':'walk_follow'})
assert p['shots'][0]['motion_preset']=='wave'
v=validate(p); assert v['healthy']
m=render_manifest(p); assert len(m['shots'])>=5
r=readiness(p,status()); assert 0<=r['score']<=100
print('Stage 21500 QA PASS', json.dumps({'shots':p['shot_count'],'readiness':r['score'],'manifest':len(m['shots'])}))
