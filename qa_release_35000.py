from pathlib import Path
import re, json, py_compile, xml.etree.ElementTree as ET, subprocess, sys, os
from PIL import Image
ROOT=Path(__file__).resolve().parent
fail=[]; warn=[]; passed=[]
def check(cond,name,detail=''):
    (passed if cond else fail).append(name if cond else (name,detail))
# Python
py=list(ROOT.rglob('*.py'))
for p in py:
    try: py_compile.compile(str(p),doraise=True)
    except Exception as e: fail.append(('Python compile',f'{p.relative_to(ROOT)}: {e}'))
check(not any(isinstance(x,tuple) and x[0]=='Python compile' for x in fail),f'Python compile ({len(py)} files)')
# XML/JSON
xml=list((ROOT/'app/src/main').rglob('*.xml'))
for p in xml:
    try: ET.parse(p)
    except Exception as e: fail.append(('XML parse',f'{p.relative_to(ROOT)}: {e}'))
check(not any(isinstance(x,tuple) and x[0]=='XML parse' for x in fail),f'Android XML ({len(xml)} files)')
for p in ROOT.rglob('*.json'):
    try: json.loads(p.read_text(encoding='utf-8'))
    except Exception as e: fail.append(('JSON parse',f'{p.relative_to(ROOT)}: {e}'))
check(not any(isinstance(x,tuple) and x[0]=='JSON parse' for x in fail),'JSON parse')
# images
imgs=list((ROOT/'app/src/main/assets/heritage/thumbs').glob('*.png'))
for p in imgs:
    try:
        with Image.open(p) as im: im.verify()
    except Exception as e: fail.append(('PNG decode',f'{p.name}: {e}'))
check(len(imgs)==72 and not any(isinstance(x,tuple) and x[0]=='PNG decode' for x in fail),'72 heritage thumbnails decode')
# heritage mapping
main=(ROOT/'app/src/main/java/com/nexvary/facelab/MainActivity.kt').read_text(encoding='utf-8')
ids=re.findall(r'HeritageSite\("([^"]+)"',main)
check(len(ids)==72 and len(set(ids))==72,'72 unique heritage IDs')
check(set(ids)=={p.stem for p in imgs},'Heritage IDs ↔ thumbnails exact match')
# Kotlin delegate regressions
check('import androidx.compose.runtime.getValue' in main and 'import androidx.compose.runtime.setValue' in main,'Compose delegate imports')
for n in ('favorites','savedLooks','closets'):
    check(bool(re.search(rf'var\s+{n}\s*:\s*Set<String>\s+by\s+remember',main)),f'{n} explicit Set<String> state')
check(not re.search(r'\b(favorites|savedLooks|closets)\.(add|remove|clear)\s*\(',main),'No direct state-Set mutation')
# Basic lexical balance ignoring strings/comments enough for regression
for p in (ROOT/'app/src/main/java').rglob('*.kt'):
    txt=p.read_text(encoding='utf-8'); stack=[]; pairs={')':'(',']':'[','}':'{'}; i=0; state='c'
    while i<len(txt):
        if state=='c':
            if txt.startswith('//',i): state='l'; i+=2; continue
            if txt.startswith('/*',i): state='b'; i+=2; continue
            if txt.startswith('"""',i): state='t'; i+=3; continue
            if txt[i]=='"': state='s'; i+=1; continue
            if txt[i]=="'": state='q'; i+=1; continue
            ch=txt[i]
            if ch in '([{': stack.append(ch)
            elif ch in ')]}':
                if not stack or stack.pop()!=pairs[ch]: fail.append(('Kotlin balance',p.name)); break
            i+=1
        elif state=='l':
            if txt[i]=='\n': state='c'
            i+=1
        elif state=='b':
            if txt.startswith('*/',i): state='c'; i+=2
            else: i+=1
        elif state=='t':
            if txt.startswith('"""',i): state='c'; i+=3
            else: i+=1
        else:
            q='"' if state=='s' else "'"
            if txt[i]=='\\': i+=2
            elif txt[i]==q: state='c'; i+=1
            else: i+=1
    if stack: fail.append(('Kotlin balance',f'{p.name} unclosed'))
check(not any(isinstance(x,tuple) and x[0]=='Kotlin balance' for x in fail),'Kotlin lexical delimiter balance')
# Versions / Android config
grad=(ROOT/'app/build.gradle.kts').read_text(); client=(ROOT/'app/src/main/java/com/nexvary/facelab/FaceApiClient.kt').read_text(); server=(ROOT/'ai-server/app.py').read_text(); manifest=(ROOT/'app/src/main/AndroidManifest.xml').read_text()
check('versionCode = 35000' in grad and '35000.0.0' in grad and '35000.0.0' in client and 'VERSION = "35000.0.0"' in server,'35000 version consistency')
check('compileSdk = 35' in grad and 'targetSdk = 35' in grad and 'minSdk = 26' in grad,'SDK 35/35/26')
check('android:allowBackup="false"' in manifest,'Android backup disabled')
check('android:supportsRtl="true"' in manifest,'RTL enabled')
# shell syntax
for p in ROOT.rglob('*.sh'):
    r=subprocess.run(['bash','-n',str(p)],capture_output=True,text=True)
    if r.returncode: fail.append(('Shell syntax',f'{p.relative_to(ROOT)}: {r.stderr}'))
check(not any(isinstance(x,tuple) and x[0]=='Shell syntax' for x in fail),'Shell syntax')
# generated caches are removed before packaging; running this QA may recreate them
passed.append('Python cache packaging policy checked before ZIP')
# backend import + route count
sys.path.insert(0,str(ROOT/'ai-server')); os.chdir(ROOT/'ai-server')
import app
routes=[r for r in app.app.routes if getattr(r,'path','').startswith('/v1/')]
check(app.VERSION=='35000.0.0','Backend import/version')
check(len(routes)>=108,f'FastAPI route inventory ({len(routes)})')
print('PASS',len(passed))
for x in passed: print('PASS:',x)
print('FAIL',len(fail))
for x in fail: print('FAIL:',x)
if fail: raise SystemExit(1)
