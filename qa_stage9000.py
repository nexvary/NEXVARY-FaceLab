from pathlib import Path
import os, re, tempfile, time, zipfile
from xml.etree import ElementTree as ET
from PIL import Image

ROOT=Path(__file__).resolve().parent

def ok(name, cond):
    if not cond: raise AssertionError(name)
    print(f'{name}: PASS')

# versions
app=(ROOT/'app/build.gradle.kts').read_text(encoding='utf-8')
server=(ROOT/'ai-server/app.py').read_text(encoding='utf-8')
client=(ROOT/'app/src/main/java/com/nexvary/facelab/FaceApiClient.kt').read_text(encoding='utf-8')
ok('Version 9000', 'versionCode = 9000' in app and '9000.0.0' in app and 'VERSION = "9000.0.0"' in server and '9000.0.0' in client)

# Android resources
ET.parse(ROOT/'app/src/main/AndroidManifest.xml')
for p in (ROOT/'app/src/main/res').rglob('*.xml'): ET.parse(p)
ok('Android XML', True)

# Kotlin structural delimiter check (not a compiler)
kt=(ROOT/'app/src/main/java/com/nexvary/facelab/MainActivity.kt').read_text(encoding='utf-8')
for a,b in [('(',')'),('{','}'),('[',']')]: ok(f'Kotlin balance {a}{b}', kt.count(a)==kt.count(b))
ok('Film UI stage9000 controls', all(x in kt for x in ['aspectRatio','exportPreset','filmCancel','filmRetry','currentShot']))

# backend syntax
import py_compile
for p in (ROOT/'ai-server').rglob('*.py'): py_compile.compile(str(p), doraise=True)
ok('Python syntax', True)

# actual multi-shot preview
os.environ['FACELAB_PREVIEW_SHOT_SECONDS']='2'
import sys
sys.path.insert(0,str(ROOT/'ai-server'))
from providers.film import create_render_job, read_job, read_plan, list_jobs, create_project_archive, EXPORT_PRESETS
with tempfile.TemporaryDirectory() as td:
    r=Path(td); src=r/'x.jpg'; Image.new('RGB',(960,1280),(90,120,160)).save(src)
    job=create_render_job(r,src.read_bytes(),'A traveler walks by an ancient landmark and turns toward camera.',30,'travel','preview','9:16','preview')
    jid=job['job_id']; deadline=time.time()+150
    while time.time()<deadline:
        state=read_job(r,jid)
        if state and state['state'] in ('completed','failed','cancelled'): break
        time.sleep(.5)
    ok('Multi-shot render state', state['state']=='completed')
    ok('Real MP4', state['video_ready'] and (r/jid/'output.mp4').stat().st_size>1000)
    ok('Thumbnail', state['thumbnail_ready'])
    ok('Subtitles', state['subtitle_ready'])
    ok('Storyboard', read_plan(r,jid)['shot_count']>=5)
    ok('Job history', len(list_jobs(r))==1)
    ok('Archive', create_project_archive(r,jid).stat().st_size>1000)

ok('Export presets', set(EXPORT_PRESETS)=={'preview','720p','1080p'})
ok('Paid inference API', 'requests.post(' not in (ROOT/'ai-server/providers/film.py').read_text(encoding='utf-8'))
print('FaceLab Stage 9000 QA: PASS')
