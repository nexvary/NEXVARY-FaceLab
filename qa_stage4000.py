from pathlib import Path
import json, sys, tempfile, time
from PIL import Image

ROOT = Path(__file__).resolve().parent
AI = ROOT / 'ai-server'
sys.path.insert(0, str(AI))
from providers.film import build_plan, create_render_job, read_job, status

assert '4000.0.0' in (AI/'app.py').read_text(encoding='utf-8')
assert 'versionCode = 4000' in (ROOT/'app/build.gradle.kts').read_text(encoding='utf-8')
assert 'versionName = "4000.0.0"' in (ROOT/'app/build.gradle.kts').read_text(encoding='utf-8')
api=(ROOT/'app/src/main/java/com/nexvary/facelab/FaceApiClient.kt').read_text(encoding='utf-8')
main=(ROOT/'app/src/main/java/com/nexvary/facelab/MainActivity.kt').read_text(encoding='utf-8')
for token in ('filmRender(', 'filmJob(', 'filmVideo(', '/v1/film/jobs/'):
    assert token in api
for token in ('توليد أول فيديو','GENERATE FIRST VIDEO','renderMode','videoReady','LaunchedEffect(jobId)'):
    assert token in main

plan=build_plan('A traveler walks through a historic location and smiles to camera',60,'travel')
assert plan['duration_seconds']==60 and plan['shot_count']>=5
assert all({'character','face','wardrobe','location'}.issubset(set(s['locks'])) for s in plan['shots'])

st=status()
assert st['paid_api_required'] is False

# If ffmpeg exists, verify an actual MP4 is produced by the local preview pipeline.
if st['ffmpeg_ready']:
    with tempfile.TemporaryDirectory() as td:
        root=Path(td)
        src=root/'source.jpg'
        Image.new('RGB',(720,1080),(30,55,75)).save(src,quality=90)
        job=create_render_job(root/'jobs',src.read_bytes(),'Cinematic portrait in a heritage location',30,'cinematic','preview')
        deadline=time.time()+30
        final=None
        while time.time()<deadline:
            final=read_job(root/'jobs',job['job_id'])
            if final and final.get('state') in {'completed','failed'}: break
            time.sleep(.2)
        assert final and final['state']=='completed', final
        out=root/'jobs'/job['job_id']/'output.mp4'
        assert out.exists() and out.stat().st_size>1000
        assert final.get('video_ready') is True

print('NEXVARY FaceLab Stage 4000 QA: PASS')
