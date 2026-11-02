from pathlib import Path
import re, py_compile
root = Path(__file__).resolve().parent
files = [root/'ai-server/app.py', root/'ai-server/providers/runner.py', root/'ai-server/providers/diffusion_face.py', root/'ai-server/smoke_test.py']
for f in files: py_compile.compile(str(f), doraise=True)
gradle=(root/'app/build.gradle.kts').read_text(encoding='utf-8')
main=(root/'app/src/main/java/com/nexvary/facelab/MainActivity.kt').read_text(encoding='utf-8')
api=(root/'app/src/main/java/com/nexvary/facelab/FaceApiClient.kt').read_text(encoding='utf-8')
server=(root/'ai-server/app.py').read_text(encoding='utf-8')
assert 'versionCode = 800' in gradle and 'versionName = "800.0.0"' in gradle
assert 'FaceLab • 800.0.0' in main
assert 'VERSION = "800.0.0"' in server
assert '/v1/storage' in server and '/v1/system' in server
assert 'FACELAB_API_TOKEN' in gradle and 'X-FaceLab-Token' in api
for tool in ['Relight','BlemishFix','EyeEnhance','Headshot','BackgroundStudio']:
    assert f'SmartTool.{tool}' in main
# Guard against accidentally adding known paid inference dependencies.
combined='\n'.join(p.read_text(encoding='utf-8', errors='ignore') for p in root.rglob('*') if p.is_file() and p.name != 'qa_stage800.py' and p.stat().st_size < 2_000_000)
for forbidden in ['api.openai.com','api.replicate.com','api.stability.ai']:
    assert forbidden not in combined
print('Stage 800 structural QA: PASS')
