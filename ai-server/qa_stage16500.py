import json, tempfile
from pathlib import Path
from providers.continuity import analyze
from providers.audio_director import build_timeline
from providers.director_presets import catalog as director_catalog
from providers.project_templates import catalog as template_catalog
from providers.motion_presets import catalog as motion_catalog
from providers.film import build_plan
p=build_plan('A friend walks and smiles.',45,'cinematic','16:9','walk_forward')
assert len(p['shots'])>=5
assert analyze(p)['score']>=80
assert build_timeline(p)['duration_seconds']==45
assert director_catalog()['count']>=8
assert template_catalog()['count']>=5
assert motion_catalog()['count']>=12
print('FaceLab Stage 16500 QA: PASS')
