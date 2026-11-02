from pathlib import Path
import xml.etree.ElementTree as ET
import json,re,sys
ROOT=Path(__file__).resolve().parent
checks={}
gradle=(ROOT/'app/build.gradle.kts').read_text(encoding='utf-8')
main=(ROOT/'app/src/main/java/com/nexvary/facelab/MainActivity.kt').read_text(encoding='utf-8')
nav=(ROOT/'app/src/main/java/com/nexvary/facelab/NavigationComponents.kt').read_text(encoding='utf-8')
app=(ROOT/'ai-server/app.py').read_text(encoding='utf-8')
checks['version_code']='versionCode = 38000' in gradle
checks['version_name']='versionName = "40000.0.0"' in gradle
checks['backend_version']='VERSION = "40000.0.0"' in app
checks['flags']=all(f in main for f in ['🇪🇬','🇬🇧','🇹🇷','🇪🇸','🇩🇪','🇮🇹','🇫🇷','🇵🇰','🇮🇷'])
checks['electric_palette']=all(x in (main+nav) for x in ['00E5FF','FFC857','D9E7F2'])
checks['custom_nav']=all(x in nav for x in ['nav_edit_electric','nav_wardrobe_electric','nav_landmarks_electric','nav_films_electric','nav_gallery_electric','nav_models_electric','nav_settings_electric','nav_about_electric','nav_more_electric'])
checks['app_icon']=(ROOT/'app/src/main/res/drawable-nodpi/facelab_icon_electric.png').exists()
checks['build_script']=(ROOT/'BUILD_ANDROID_STAGE39000.cmd').exists()
for p in ROOT.rglob('*.xml'):
    ET.parse(p)
checks['xml_parse']=True
for p in ROOT.rglob('*.json'):
    json.loads(p.read_text(encoding='utf-8'))
checks['json_parse']=True
failed=[k for k,v in checks.items() if not v]
for k,v in checks.items(): print(f'{k}: {"PASS" if v else "FAIL"}')
print('Stage 39500 UI QA:', 'PASS' if not failed else 'FAIL')
if failed: print('Failed:',failed); sys.exit(1)
