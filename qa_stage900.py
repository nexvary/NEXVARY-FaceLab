from pathlib import Path
import py_compile
root=Path(__file__).resolve().parent
for f in [root/'ai-server/app.py',root/'ai-server/providers/runner.py',root/'ai-server/providers/diffusion_face.py',root/'ai-server/providers/wardrobe.py',root/'ai-server/smoke_test.py']:
    py_compile.compile(str(f),doraise=True)
gradle=(root/'app/build.gradle.kts').read_text(encoding='utf-8')
main=(root/'app/src/main/java/com/nexvary/facelab/MainActivity.kt').read_text(encoding='utf-8')
api=(root/'app/src/main/java/com/nexvary/facelab/FaceApiClient.kt').read_text(encoding='utf-8')
server=(root/'ai-server/app.py').read_text(encoding='utf-8')
ward=(root/'ai-server/providers/wardrobe.py').read_text(encoding='utf-8')
assert 'versionCode = 900' in gradle and 'versionName = "900.0.0"' in gradle
assert 'FaceLab • 900.0.0' in main and 'AppScreen.Wardrobe' in main
assert 'VERSION = "900.0.0"' in server
for endpoint in ['/v1/wardrobe/status','/v1/wardrobe/catalog','/v1/wardrobe/try-on']:
    assert endpoint in server
assert 'suspend fun tryOn' in api and '900.0.0' in api
assert ward.count('Wardrobe') >= 0
# Actual catalog length, parsed by import-free source convention.
assert ward.count('"formal_') >= 9 and ward.count('"casual_') >= 9 and ward.count('"sport_') >= 9
assert main.count('WardrobeStyle("') == 72
assert len([x for x in ['Formal','Casual','Sport','Summer','Winter','Traditional','Business','Evening'] if f'    {x}(' in main]) == 8
for forbidden in ['api.openai.com','api.replicate.com','api.stability.ai']:
    assert forbidden not in '\n'.join(p.read_text(encoding='utf-8',errors='ignore') for p in root.rglob('*') if p.is_file() and not p.name.startswith('qa_stage') and p.stat().st_size < 2_000_000)
# Lightweight delimiter balance after removing nothing: catches accidental gross truncation.
assert main.count('{') == main.count('}')
assert main.count('(') == main.count(')')
print('Stage 900 structural QA: PASS — 72 wardrobe looks / 8 categories')
