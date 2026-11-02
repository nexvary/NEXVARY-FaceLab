from pathlib import Path
import ast

root = Path(__file__).resolve().parent
main = (root/'app/src/main/java/com/nexvary/facelab/MainActivity.kt').read_text(encoding='utf-8')
client = (root/'app/src/main/java/com/nexvary/facelab/FaceApiClient.kt').read_text(encoding='utf-8')
gradle = (root/'app/build.gradle.kts').read_text(encoding='utf-8')
server = (root/'ai-server/app.py').read_text(encoding='utf-8')
wardrobe = (root/'ai-server/providers/wardrobe.py').read_text(encoding='utf-8')
installer = (root/'ai-server/install_generative_windows.bat').read_text(encoding='utf-8')

assert 'versionCode = 1500' in gradle and '1500.0.0' in gradle
assert 'VERSION = "1500.0.0"' in server
assert 'X-FaceLab-Client-Version", "1500.0.0"' in client
assert 'WardrobeShelf' in main and 'WardrobeOutfitPack' in main and 'WardrobeOccasion' in main
assert 'AI Stylist' in main and 'WARDROBE_OUTFITS' in main
assert 'textPart("occasion", occasion)' in client and 'textPart("outfit", outfit)' in client
assert '/v1/wardrobe/stylist' in server
assert 'occasion: str = Form("work")' in server and 'outfit: str = Form("")' in server
assert 'occasion.encode() + outfit.encode()' in server
assert 'coordinated multi-piece outfit' in wardrobe
assert 'if errorlevel 1 goto :fail' in installer

# Lightweight delimiter balance; ignores string nuance but catches gross truncation.
for a,b in [('(',')'),('{','}'),('[',']')]:
    assert main.count(a) == main.count(b), (a,b,main.count(a),main.count(b))

ast.parse(server)
ast.parse(wardrobe)
print('FaceLab Stage 1500 QA: PASS')
