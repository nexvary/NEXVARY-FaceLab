from pathlib import Path
import ast, json, re, sys
R=Path(__file__).resolve().parent
checks=[]
def ok(name, cond):
    checks.append((name,bool(cond)))
    print(('PASS' if cond else 'FAIL'), name)
for f in ['ai-server/app.py','ai-server/providers/film.py','ai-server/providers/driving_motions.py','ai-server/providers/motion_presets.py','ai-server/adapters/liveportrait_runner.py','ai-server/adapters/mimicmotion_runner.py']:
    ast.parse((R/f).read_text(encoding='utf-8')); ok('python:'+f,True)
app=(R/'ai-server/app.py').read_text(encoding='utf-8')
film=(R/'ai-server/providers/film.py').read_text(encoding='utf-8')
client=(R/'app/src/main/java/com/nexvary/facelab/FaceApiClient.kt').read_text(encoding='utf-8')
ui=(R/'app/src/main/java/com/nexvary/facelab/MainActivity.kt').read_text(encoding='utf-8')
build=(R/'app/build.gradle.kts').read_text(encoding='utf-8')
ok('server version', 'VERSION = "11000.0.0"' in app)
ok('android version', 'versionCode = 11000' in build and '11000.0.0' in build)
ok('motion routes', '/v1/film/motions' in app and '/v1/film/driving-motions' in app)
ok('motion preset wired', 'motion_preset' in app and 'apply_motion_preset' in film)
ok('client motion methods', 'filmMotions()' in client and 'filmDoctor()' in client and 'filmDrivingMotions()' in client)
ok('android motion studio', 'Motion Studio' in ui and 'walk_forward' in ui and 'MimicMotion' in ui)
ok('windows bootstrap', (R/'ai-server/install_stage11000_motion_windows.bat').exists())
if not all(x[1] for x in checks): sys.exit(1)
