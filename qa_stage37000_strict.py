from pathlib import Path
import re, json, xml.etree.ElementTree as ET, zipfile, sys
ROOT=Path(__file__).resolve().parent
errors=[]
main=(ROOT/'app/src/main/java/com/nexvary/facelab/MainActivity.kt').read_text(encoding='utf-8')
cmp=(ROOT/'app/src/main/java/com/nexvary/facelab/ComparePreviewCard.kt').read_text(encoding='utf-8')
sec=(ROOT/'app/src/main/java/com/nexvary/facelab/SecondaryScreens.kt').read_text(encoding='utf-8')
gradle=(ROOT/'app/build.gradle.kts').read_text(encoding='utf-8')
app=(ROOT/'ai-server/app.py').read_text(encoding='utf-8')
checks={
'compare_not_file_private': '@Composable\ninternal fun ComparePreviewCard(' in cmp,
'compare_called_from_main': 'ComparePreviewCard(' in main,
'gallery_moved': 'internal fun GalleryScreen(' in sec and 'private fun GalleryScreen(' not in main,
'about_moved': 'internal fun AboutScreen(' in sec and 'private fun AboutScreen(' not in main,
'common_section_visible': 'internal fun SectionCard(' in main,
'common_header_visible': 'internal fun ScreenHeader(' in main,
'version_code': 'versionCode = 37000' in gradle,
'version_name': 'versionName = "37000.0.0"' in gradle,
'backend_version': 'VERSION = "37000.0.0"' in app,
'build_script': (ROOT/'BUILD_ANDROID_STAGE37000.cmd').exists(),
}
for k,v in checks.items():
    if not v: errors.append(k)
# duplicate top-level function declarations across Kotlin files
seen={}
for f in (ROOT/'app/src/main/java/com/nexvary/facelab').glob('*.kt'):
    text=f.read_text(encoding='utf-8')
    for m in re.finditer(r'^(?:@Composable\s+)?(?:internal\s+|private\s+|public\s+)?fun\s+(\w+)\s*\(', text, re.M):
        name=m.group(1)
        seen.setdefault(name,[]).append(f.name)
for name,files in seen.items():
    if len(files)>1 and name not in {'label'}:
        errors.append(f'duplicate_fun:{name}:{files}')
# XML parse
for f in (ROOT/'app/src/main/res').rglob('*.xml'):
    try: ET.parse(f)
    except Exception as e: errors.append(f'xml:{f}:{e}')
# basic JSON parse
for f in ROOT.rglob('*.json'):
    try: json.loads(f.read_text(encoding='utf-8'))
    except Exception as e: errors.append(f'json:{f}:{e}')
print('Stage 37000 strict QA:', 'PASS' if not errors else 'FAIL')
print('checks=',len(checks),'kotlin_files=',len(list((ROOT/'app/src/main/java/com/nexvary/facelab').glob('*.kt'))))
if errors:
    print('\n'.join(errors)); sys.exit(1)
