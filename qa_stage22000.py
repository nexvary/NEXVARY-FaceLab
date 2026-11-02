from pathlib import Path
import tempfile, json
import sys
sys.path.insert(0,str(Path(__file__).parent/'ai-server'))
from providers.timeline_studio import build_multitrack, render_queue, snapshot, list_snapshots, restore

plan={"shots":[{"id":"shot-1","duration_seconds":4,"motion_preset":"smile","preferred_engine":"liveportrait","dialogue":"hello"},{"id":"shot-2","duration_seconds":6,"camera_preset":"hero_orbit"}]}
t=build_multitrack(plan)
assert t["track_count"]==4 and t["shot_count"]==2 and t["duration_seconds"]==10.0
with tempfile.TemporaryDirectory() as td:
    r=Path(td); d=r/'abc'; d.mkdir(); (d/'plan.json').write_text(json.dumps(plan)); (d/'job.json').write_text(json.dumps({"job_id":"abc","state":"queued","progress":0,"created_at":1,"shot_count":2}))
    q=render_queue(r); assert q["counts"]["queued"]==1
    x=snapshot(r,'abc'); assert list_snapshots(r,'abc')
    (d/'plan.json').write_text(json.dumps({"shots":[]})); rr=restore(r,'abc',x['snapshot_id']); assert len(rr['plan']['shots'])==2
print('Stage 22000 QA PASS')

