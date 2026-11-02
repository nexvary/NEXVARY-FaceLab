from pathlib import Path
root=Path(__file__).parent
main=(root/'app/src/main/java/com/nexvary/facelab/MainActivity.kt').read_text()
api=(root/'app/src/main/java/com/nexvary/facelab/FaceApiClient.kt').read_text()
server=(root/'ai-server/app.py').read_text()
provider=(root/'ai-server/providers/wardrobe.py').read_text()
gradle=(root/'app/build.gradle.kts').read_text()
assert 'versionCode = 1000' in gradle and '1000.0.0' in gradle
assert 'WardrobeGender' in main and 'WardrobeColor' in main and 'WardrobeAccessory' in main
assert 'favorites' in main and 'saved_looks' in main and 'OutlinedTextField' in main
assert 'textPart("color", color)' in api and 'textPart("accessory", accessory)' in api
assert 'color: str = Form("auto")' in server and 'accessory: str = Form("none")' in server
assert 'primary garment color' in provider and 'coordinated accessory' in provider
print('FaceLab Stage 1000 QA: PASS')
