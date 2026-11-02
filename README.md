# NEXVARY FaceLab

Android photography/editor studio with an optional self-hosted Python image-processing server.
This is a development build, not an accepted production or Stage 150 release.

## Current implementation

- Jetpack Compose editor, persistent gallery, wardrobe/background/film interfaces inherited from the recovered source.
- CameraX preview, bounded local luminance guidance, composition grid, camera switching, torch when supported, and private JPEG capture returned to the editor.
- Local image editing and built-in server image enhancement. Optional generative, segmentation, restoration and video adapters need separately configured and licensed engines; adapter presence is not proof of working inference.
- Session-only server URL/token and explicit upload consent; release builds require HTTPS. Upload permission is revoked when the server URL changes.

## Build and test

Use JDK 17 (not a JRE), Android SDK platform 35, build-tools 35.0.0 and Gradle 8.10.2.
`./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleRelease :app:testReleaseUnitTest :app:assembleDebugAndroidTest`

The restored archive lacked the standard wrapper JAR. The supplied bootstrap scripts download the pinned Gradle distribution and verify its SHA-256. CI uses `gradle/actions/setup-gradle` instead.

For Python 3.12, install `ai-server/requirements.txt` and `ai-server/requirements-test.txt` in a fresh virtual environment, then run `python -m pytest ai-server/tests -q` and `python ai-server/smoke_test.py`.
See [server setup](docs/SERVER_SETUP.md) for authentication, retention and deployment settings.

## Verification

[Pull request](https://github.com/nexvary/NEXVARY-FaceLab/pull/1) · [Actions](https://github.com/nexvary/NEXVARY-FaceLab/actions)

CI compiles debug/release variants, runs JVM and Python tests, checks lint and known Python advisories, and produces APK/checksum artifacts. An Android 15 emulator job exercises JPEG capture and camera lifecycle. Check the specific run for its actual result; configuration alone is not a pass.

Historical `STAGE_*` and `QA_*` documents are recovery records. Their numbers do not establish verified completion. Physical camera quality/performance, the complete studio roadmap and commercial model-license approval remain separate acceptance gates.
