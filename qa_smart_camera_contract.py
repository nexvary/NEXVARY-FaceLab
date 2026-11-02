"""Lightweight source contract; not a replacement for Gradle compilation or device tests."""
from pathlib import Path

root = Path(__file__).resolve().parent
camera = (root / 'app/src/main/java/com/nexvary/facelab/SmartCameraActivity.kt').read_text()
lighting = (root / 'app/src/main/java/com/nexvary/facelab/LightingAnalyzer.kt').read_text()
manifest = (root / 'app/src/main/AndroidManifest.xml').read_text()
assert 'ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST' in camera
assert 'finally { frame.close() }' in camera
assert 'ActivityResultContracts.RequestPermission()' in camera
assert 'LightingAnalyzer.meanLuma(frame)' in camera
assert 'plane.rowStride' in lighting and 'plane.pixelStride' in lighting
assert 'android:exported="false"' in manifest
assert 'android.permission.CAMERA' in manifest
print('Smart camera source contract: PASS (7 checks)')
