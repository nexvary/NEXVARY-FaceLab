from pathlib import Path
import re, json, xml.etree.ElementTree as ET, sys
root=Path(__file__).resolve().parent
errors=[]; checks=[]
def ok(name, cond, detail=''):
    checks.append((name,bool(cond),detail))
    if not cond: errors.append(name + ((': '+detail) if detail else ''))
main=(root/'app/src/main/java/com/nexvary/facelab/MainActivity.kt').read_text(encoding='utf-8')
bg=(root/'app/build.gradle.kts').read_text(encoding='utf-8')
man=(root/'app/src/main/AndroidManifest.xml').read_text(encoding='utf-8')
loc=(root/'app/src/main/res/xml/locales_config.xml').read_text(encoding='utf-8')
styles=(root/'app/src/main/res/values/styles.xml').read_text(encoding='utf-8')
# release/version
ok('versionCode 36500','versionCode = 36500' in bg)
ok('versionName 36500.0.0','versionName = "36500.0.0"' in bg)
# languages
for code in ['ar','en','tr','es','de','it','fr','ur','fa']:
    ok('locale '+code, f'android:name="{code}"' in loc)
for n in ['Arabic','English','Turkish','Spanish','German','Italian','French','Urdu','Persian']:
    ok('AppLanguage '+n, n+'(' in main)
ok('RTL Arabic','Arabic("ar", "AR", "العربية", true)' in main)
ok('RTL Urdu','Urdu("ur", "UR", "اردو", true)' in main)
ok('RTL Persian','Persian("fa", "FA", "فارسی", true)' in main)
ok('layout direction uses language.rtl','if (language.rtl) LayoutDirection.Rtl' in main)
# back behavior
ok('BackHandler present','BackHandler {' in main)
ok('subscreen back to Editor','if (screen != AppScreen.Editor) screen = AppScreen.Editor' in main)
ok('main back exits','else activity.finish()' in main)
ok('visible back label','"رجوع","Back"' in main)
# Android 15 / insets
ok('targetSdk35','targetSdk = 35' in bg)
ok('compileSdk35','compileSdk = 35' in bg)
ok('edge to edge','enableEdgeToEdge()' in main)
ok('safeDrawing insets','contentWindowInsets = WindowInsets.safeDrawing' in main)
ok('nav bar padding','navigationBarsPadding()' in main)
ok('transparent status bar','android:statusBarColor">@android:color/transparent' in styles)
ok('transparent nav bar','android:navigationBarColor">@android:color/transparent' in styles)
ok('adjustResize','android:windowSoftInputMode="adjustResize"' in man)
# icon/nav overlap architecture
ok('adaptive icon','@mipmap/ic_launcher' in man)
ok('round icon','@mipmap/ic_launcher_round' in man)
ok('icon foreground exists',(root/'app/src/main/res/drawable/ic_app_foreground.xml').exists())
ok('bottom nav scrollable','.horizontalScroll(rememberScrollState())' in main)
ok('bottom item minimum width','.widthIn(min=76.dp)' in main)
ok('bottom item min height','.heightIn(min=58.dp)' in main)
ok('all screens in nav','AppScreen.Models, AppScreen.Settings, AppScreen.About' in main)
# About/socials
for val in ['https://nexvary.com/','https://www.facebook.com/share/14p9krEn5ij/','mailto:info@nexvary.com','https://www.youtube.com/@NexvaryInc','https://x.com/Nexvary']:
    ok('about link '+val,val in main)
# XML validity
for f in root.glob('app/src/main/**/*.xml'):
    try: ET.parse(f); good=True; d=''
    except Exception as e: good=False; d=str(e)
    ok('XML '+str(f.relative_to(root)),good,d)
# Kotlin delimiter scan
def balanced(text):
    st=[]; i=0; quote=None; esc=False; line=False; block=0; pairs={')':'(',']':'[','}':'{'}
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
# generated junk
trash=[p for p in root.rglob('*') if p.name in {'__pycache__','.gradle','build'} or p.suffix=='.pyc']
ok('no cache/build trash',not trash, str(trash[:5]))
for name,good,detail in checks: print(('PASS' if good else 'FAIL'),name,detail)
print(f'CHECKS={len(checks)} FAILURES={len(errors)}')
if errors:
    print('\n'.join(errors)); sys.exit(1)
