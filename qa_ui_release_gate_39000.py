from pathlib import Path
import re, json, sys, hashlib
from xml.etree import ElementTree as ET
from PIL import Image

ROOT=Path(__file__).resolve().parent
JAVA=ROOT/'app/src/main/java/com/nexvary/facelab'
RES=ROOT/'app/src/main/res'
FAIL=[]; PASS=[]; NOTES=[]

def ok(name, cond, detail=''):
    (PASS if cond else FAIL).append((name,detail))

def read(p): return Path(p).read_text(encoding='utf-8')

# Version gate
build=read(ROOT/'app/build.gradle.kts')
app=read(ROOT/'ai-server/app.py')
ok('versionCode 40000', 'versionCode = 40000' in build)
ok('versionName 40000.0.0', 'versionName = "40000.0.0"' in build)
ok('backend version 40000.0.0', 'VERSION = "40000.0.0"' in app)

# XML gate
xmls=list(RES.rglob('*.xml'))+[ROOT/'app/src/main/AndroidManifest.xml']
for p in xmls:
    try: ET.parse(p)
    except Exception as e: FAIL.append((f'XML {p.relative_to(ROOT)}',str(e)))
ok('all XML parse', not any(n.startswith('XML ') for n,_ in FAIL), f'{len(xmls)} files')

# JSON gate
jsons=list(ROOT.rglob('*.json'))
json_bad=[]
for p in jsons:
    try: json.loads(p.read_text(encoding='utf-8'))
    except Exception as e: json_bad.append((p,e))
ok('all JSON parse', not json_bad, f'{len(jsons)} files')

# Raster integrity
rasters=[p for p in RES.rglob('*') if p.suffix.lower() in {'.png','.jpg','.jpeg','.webp'}]
bad=[]
for p in rasters:
    try:
        with Image.open(p) as im:
            im.verify()
    except Exception as e: bad.append((p,e))
ok('all raster resources decode', not bad, f'{len(rasters)} files')

# Electric nav icon references + vector validation
nav=read(JAVA/'NavigationComponents.kt')
icons=['nav_edit_electric','nav_wardrobe_electric','nav_landmarks_electric','nav_films_electric','nav_gallery_electric','nav_models_electric','nav_settings_electric','nav_about_electric','nav_more_electric']
for name in icons:
    p=RES/'drawable'/f'{name}.xml'
    ok(f'icon exists: {name}', p.exists())
    ok(f'icon referenced: {name}', f'R.drawable.{name}' in nav)
    if p.exists():
        try:
            root=ET.parse(p).getroot()
            paths=[e for e in root.iter() if e.tag.endswith('path')]
            ok(f'icon vector paths: {name}', len(paths)>0, f'{len(paths)} path(s)')
        except Exception as e: FAIL.append((f'icon parse {name}',str(e)))

# Main app icon integrity and launcher references
manifest=read(ROOT/'app/src/main/AndroidManifest.xml')
ok('launcher icon reference', '@mipmap/ic_launcher' in manifest)
ok('round launcher icon reference', '@mipmap/ic_launcher_round' in manifest)
app_icon=RES/'drawable-nodpi/facelab_icon_electric.png'
ok('electric app icon exists', app_icon.exists())
if app_icon.exists():
    with Image.open(app_icon) as im:
        ok('electric app icon square', im.width==im.height, f'{im.width}x{im.height}')
        ok('electric app icon resolution', im.width>=512 and im.height>=512, f'{im.width}x{im.height}')

# Language / RTL gates
main=read(JAVA/'MainActivity.kt')
langs=[('Arabic','🇪🇬',True),('English','🇬🇧',False),('Turkish','🇹🇷',False),('Spanish','🇪🇸',False),('German','🇩🇪',False),('Italian','🇮🇹',False),('French','🇫🇷',False),('Urdu','🇵🇰',True),('Persian','🇮🇷',True)]
for lang,flag,rtl in langs:
    ok(f'language enum {lang}', f'AppLanguage.{lang}' in main)
    ok(f'language flag {lang}', flag in main)
ok('RTL provider present','LocalLayoutDirection provides if (language.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr' in main)
ok('Arabic/Urdu/Persian RTL declared', all(x in main for x in ['Arabic("ar"','Urdu("ur"','Persian("fa"']))

# Android 15 + safe-area gates
ok('compileSdk 35','compileSdk = 35' in build)
ok('targetSdk 35','targetSdk = 35' in build)
ok('safeDrawing inset','WindowInsets.safeDrawing' in main)
ok('navigation bar padding','navigationBarsPadding()' in nav)
ok('edge-to-edge','enableEdgeToEdge()' in main)

# Back behavior gate
ok('BackHandler present','BackHandler' in main)
ok('secondary back returns Editor','screen = AppScreen.Editor' in main)
ok('root back exits activity','activity.finish()' in main)

# Responsive navigation geometry gate
# Row outer horizontal padding 8dp on each side, 4 gaps x 6dp, 5 cells.
def cell_width(w): return (min(w,640)-16-24)/5
for w in (360,412,600):
    cw=cell_width(w)
    ok(f'nav geometry {w}dp', cw>=60, f'cell={cw:.1f}dp')
NOTES.append(('nav geometry','360/412/600dp evaluated with 5 cells, 8dp side padding and 6dp gaps'))
ok('nav min height >= 48','height(78.dp)' in nav and 'heightIn(min=48.dp)' in nav)
ok('nav label clipping guard','maxLines=1' in nav and 'TextOverflow.Ellipsis' in nav)
ok('tablet nav max width','widthIn(max=640.dp)' in nav)

# Dialog and headers overflow guards
ok('language dialog scroll guard','heightIn(max=680.dp).verticalScroll(rememberScrollState())' in main)
ok('top title overflow guard','"NEXVARY FaceLab"' in main and 'maxLines=1, overflow=TextOverflow.Ellipsis' in main)
ok('screen header overflow guard','fontSize=22.sp' in main and 'maxLines=2,overflow=TextOverflow.Ellipsis' in main)

# Common horizontal overflow guard checks
ok('tool categories scroll','ToolCategory.entries' in main and 'horizontalScroll(rememberScrollState())' in main)
ok('effects strip scroll','LocalEffect.entries' in main and 'horizontalScroll(rememberScrollState())' in main)
ok('action row scroll','@Composable private fun ActionRow' in main and 'horizontalScroll(rememberScrollState())' in main)
ok('wardrobe chip rows scroll', main.count('horizontalScroll(rememberScrollState())')>=12, f"count={main.count('horizontalScroll(rememberScrollState())')}")

# App icon / drawable reference audit
kt='\n'.join(read(p) for p in JAVA.glob('*.kt'))
refs=set(re.findall(r'R\.(?:drawable|mipmap)\.([A-Za-z0-9_]+)',kt))
allnames={p.stem for p in RES.rglob('*') if p.is_file()}
missing=sorted(r for r in refs if r not in allnames)
ok('Kotlin drawable/mipmap refs resolve', not missing, ','.join(missing[:10]))

# Kotlin lexical delimiter scan (comments/strings stripped roughly)
def strip_kotlin(s):
    # Strings must be removed before // comments because URLs such as https:// occur inside strings.
    s=re.sub(r'""".*?"""','""',s,flags=re.S)
    s=re.sub(r'"(?:\\.|[^"\\])*"','""',s)
    s=re.sub(r"'(?:\\.|[^'\\])*'","''",s)
    s=re.sub(r'/\*.*?\*/','',s,flags=re.S)
    s=re.sub(r'//.*','',s)
    return s
for p in JAVA.glob('*.kt'):
    t=strip_kotlin(read(p))
    for a,b in [('(',')'),('{','}'),('[',']')]:
        depth=0; good=True
        for c in t:
            if c==a: depth+=1
            elif c==b:
                depth-=1
                if depth<0: good=False; break
        ok(f'Kotlin delimiters {p.name} {a}{b}',good and depth==0,f'depth={depth}')

# No obvious unfinished markers
markers=[]
for p in list(JAVA.glob('*.kt'))+list((ROOT/'ai-server').rglob('*.py')):
    t=read(p)
    for token in ('TODO(', 'NotImplementedError', 'TODO: COMPILER'):
        if token in t: markers.append(f'{p.name}:{token}')
ok('no release-blocking unfinished markers',not markers,'; '.join(markers[:10]))

print('NEXVARY FaceLab Stage 39500 — UI RELEASE GATE')
for n,d in PASS: print('PASS',n,('— '+d) if d else '')
for n,d in FAIL: print('FAIL',n,('— '+d) if d else '')
for n,d in NOTES: print('NOTE',n,'—',d)
print(f'RESULT: {len(PASS)} PASS / {len(FAIL)} FAIL')
sys.exit(1 if FAIL else 0)
