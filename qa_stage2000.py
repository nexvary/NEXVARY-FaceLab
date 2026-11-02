from pathlib import Path
import re, py_compile, xml.etree.ElementTree as ET
ROOT=Path(__file__).parent
checks=[]
def ok(name, cond):
    checks.append((name, bool(cond)))

py_compile.compile(str(ROOT/'ai-server/app.py'), doraise=True)
py_compile.compile(str(ROOT/'ai-server/providers/wardrobe.py'), doraise=True)
ok('Python syntax', True)

app=(ROOT/'ai-server/app.py').read_text(encoding='utf-8')
client=(ROOT/'app/src/main/java/com/nexvary/facelab/FaceApiClient.kt').read_text(encoding='utf-8')
main=(ROOT/'app/src/main/java/com/nexvary/facelab/MainActivity.kt').read_text(encoding='utf-8')
build=(ROOT/'app/build.gradle.kts').read_text(encoding='utf-8')
ok('Version consistency', all('2000.0.0' in x for x in [app,client,main,build]) and 'versionCode = 2000' in build)
ok('Recommendations endpoint', '/v1/wardrobe/stylist/recommendations' in app)
ok('Independent pieces', all(x in main for x in ['TOP_PIECES','BOTTOM_PIECES','OUTER_PIECES','SHOE_PIECES','top=${topPiece.code}']))
ok('Dressing rooms', all(x in main for x in ['var closets','"closets"','غرف الملابس المحفوظة']))
ok('Six languages', all(x in main for x in ['Arabic','English','Turkish','Spanish','German','Italian']))
for xml in (ROOT/'app/src/main/res').rglob('*.xml'):
    ET.parse(xml)
ok('Android XML', True)

failed=[n for n,v in checks if not v]
for n,v in checks: print(f'{n}: {"PASS" if v else "FAIL"}')
if failed: raise SystemExit('FAILED: '+', '.join(failed))
print('FaceLab Stage 2000 structural QA: PASS')
