from pathlib import Path
import re, json, xml.etree.ElementTree as ET, hashlib, sys
from PIL import Image
root=Path(__file__).resolve().parent
errors=[]; checks=[]
def ok(name, cond, detail=''):
    checks.append((name, bool(cond), detail))
    if not cond: errors.append(name + ((': '+detail) if detail else ''))

main=(root/'app/src/main/java/com/nexvary/facelab/MainActivity.kt').read_text(encoding='utf-8')
cmp=(root/'app/src/main/java/com/nexvary/facelab/ComparePreviewCard.kt').read_text(encoding='utf-8')
api=(root/'app/src/main/java/com/nexvary/facelab/FaceApiClient.kt').read_text(encoding='utf-8')

# Version gates
bg=(root/'app/build.gradle.kts').read_text()
ok('versionCode 36000', 'versionCode = 36000' in bg)
ok('versionName 36000.0.0', 'versionName = "36000.0.0"' in bg)
ok('compare extracted from MainActivity', 'private fun ComparePreviewCard' not in main)
ok('compare component exists', 'private fun ComparePreviewCard' in cmp)
ok('no BoxWithConstraints in compare', 'BoxWithConstraints' not in cmp)
ok('no Dp resultWidth compare arithmetic', 'resultWidth' not in cmp and 'maxWidth *' not in cmp)
ok('pixel width is typed primitive state', 'mutableIntStateOf(1)' in cmp)
ok('split fill fraction', '.fillMaxWidth(safeSplit)' in cmp)

# Delegated state hazards that caused real compiler regressions
for name in ('favorites','savedLooks','closets'):
    ok(f'{name} explicit Set<String>', bool(re.search(rf'var\s+{name}(?:\s*:\s*Set<String>)?\s+by[\s\S]{{0,120}}mutableStateOf<Set<String>>', main)))
    ok(f'{name} no direct mutation', not re.search(rf'\b{name}\.(add|remove|clear)\s*\(', main))
ok('runtime getValue import', 'import androidx.compose.runtime.getValue' in main)
ok('runtime setValue import', 'import androidx.compose.runtime.setValue' in main)

# Lexical Kotlin delimiter scan that ignores strings/comments enough for regression gating
def balanced(text):
    st=[]; i=0; quote=None; esc=False; line=False; block=0
    pairs={')':'(',']':'[','}':'{'}
    while i<len(text):
        c=text[i]; n=text[i+1] if i+1<len(text) else ''
        if line:
            if c=='\n': line=False
            i+=1; continue
        if block:
            if c=='/' and n=='*': block+=1; i+=2; continue
            if c=='*' and n=='/': block-=1; i+=2; continue
            i+=1; continue
        if quote:
            if esc: esc=False
            elif c=='\\': esc=True
            elif c==quote: quote=None
            i+=1; continue
        if c=='/' and n=='/': line=True; i+=2; continue
        if c=='/' and n=='*': block=1; i+=2; continue
        if c in ('"',"'"): quote=c; i+=1; continue
        if c in '([{': st.append(c)
        elif c in ')]}':
            if not st or st[-1]!=pairs[c]: return False
            st.pop()
        i+=1
    return not st and quote is None and block==0
for f in root.glob('app/src/main/java/**/*.kt'):
    ok('Kotlin delimiters '+f.name, balanced(f.read_text(encoding='utf-8')))

# XML / JSON / images
xmls=list(root.glob('app/src/main/**/*.xml'))
for f in xmls:
    try: ET.parse(f); good=True; d=''
    except Exception as e: good=False; d=str(e)
    ok('XML '+str(f.relative_to(root)), good, d)
for f in root.rglob('*.json'):
    try: json.loads(f.read_text(encoding='utf-8')); good=True; d=''
    except Exception as e: good=False; d=str(e)
    ok('JSON '+str(f.relative_to(root)), good, d)
pngs=list(root.glob('app/src/main/res/drawable*/**/*.png'))
for f in pngs:
    try:
        with Image.open(f) as im: im.verify()
        good=True; d=''
    except Exception as e: good=False; d=str(e)
    ok('PNG '+f.name, good, d)

# Manifest policy / obvious dangerous APIs
manifest=(root/'app/src/main/AndroidManifest.xml').read_text()
ok('allowBackup false', 'android:allowBackup="false"' in manifest)
ok('no broad external storage permission', 'MANAGE_EXTERNAL_STORAGE' not in manifest)
ok('no eval/exec/os.system backend', not any(re.search(r'\b(eval|exec|os\.system)\s*\(', p.read_text(errors='ignore')) for p in (root/'ai-server').rglob('*.py')))

# No build/cache trash in release tree
trash=[p for p in root.rglob('*') if p.name in {'__pycache__','.gradle','build'} or p.suffix=='.pyc']
ok('no cache/build trash', not trash, ', '.join(map(str,trash[:5])))

for name,good,detail in checks:
    print(('PASS' if good else 'FAIL'), name, detail)
print(f'CHECKS={len(checks)} FAILURES={len(errors)}')
if errors:
    print('\n'.join(errors)); sys.exit(1)
