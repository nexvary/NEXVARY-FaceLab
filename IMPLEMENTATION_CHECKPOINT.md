# FaceLab verified engineering checkpoint — 2026-11-02

Repository: `nexvary/NEXVARY-FaceLab`
Branch: `development/facelab-studio`
Draft PR: https://github.com/nexvary/NEXVARY-FaceLab/pull/1
Validated executable-source commit: `39cfea86bf799269ab6a66ed1b05d7f3d80686c7`.
A later documentation/evidence commit may exist; inspect actual HEAD before resuming.

## Implemented

- Restored the attached Android/Python source into a repository that previously had only README/workflow.
- CameraX preview, local luminance guidance, composition grid, torch when supported, front/back switching, private JPEG capture and import into the editor.
- Crop/stride/buffer-position-aware luminance sampler bounded by sample budget.
- Initial extraction of image I/O and processing from MainActivity; bounded image decode dimensions.
- Session-only server credentials and explicit upload consent, revoked on URL change. Release HTTPS policy; no compiled API token.
- Updated server dependencies, pre-multipart authentication, production-token startup gate, private default data directory, bounded upload reads, disabled default image caching and response-file cleanup after sending.
- Linux/Windows CI builds, JVM/Python tests, lint, Python advisory scan, APK checksums/artifacts and Android 15 emulator test configuration.

## Verified locally

- Clean Gradle 8.10.2 / JDK 17 / SDK 35 build: **success** for debug, unsigned release and instrumentation APK.
- JVM tests: **15 debug + 15 release**, zero failures/errors.
- Android lint: **0 errors, 25 warnings**. The old API-27 style attribute and LocalContext-to-Activity cast errors were fixed.
- Python: **11 behavioral tests** passed in a fresh Python 3.12 environment.
- Real Uvicorn HTTP smoke test passed: authenticated image enhancement returned a decodable JPEG, unauthenticated access was rejected and temporary response image cleanup was observed.
- Known-advisory Python dependency scan: no known vulnerabilities found at scan time.
- Source/resource contracts: **117 checks + 7 camera source assertions** passed. These are source checks, not device tests.
- Debug APK signature verified (Android debug key). Release APK is **unsigned**.

Local debug APK SHA-256: `4e71fca3d225bab95417f646b2d1fb763def90cbb1bd8efe5c6ab25514b1754b`.
Local release-unsigned APK SHA-256: `d87fcacd2c62577ea6caabaf8346570a8c9b8c8a3085389aa4acd92dc0c21aab`.
CI signatures/hashes may differ by runner; use each run's own SHA256SUMS artifact.

## CI evidence

Run: https://github.com/nexvary/NEXVARY-FaceLab/actions/runs/41389080891
Final observed result: **success**. Python, Linux Android, Windows Android and Android 15 emulator jobs all passed on executable-source commit `39cfea86bf799269ab6a66ed1b05d7f3d80686c7`. Both instrumentation tests passed: private JPEG capture/decoding and recreation/background/resume UI readiness. See `docs/evidence/emulator-instrumentation.log`. Later documentation-only commits use `[skip ci]`; no production/test source changed after this verified code commit. Read the actual latest run before making future current-state claims.

## Next work, in order

1. Read actual branch HEAD, PR #1 and the latest CI job/artifact results. Fix any failed build/emulator gate before adding inference engines.
2. Test capture, permission denial, rotation, background/resume, storage exhaustion, flashlight/front camera and output orientation on a physical phone. No physical phone is connected here. Emulator capture does not establish OEM camera quality or thermal performance.
3. Replace bitmap-heavy gallery/undo state with a bounded repository/ViewModel/file-reference design and perform memory/performance tests. Current decoding is bounded; the full memory gate has **not** passed.
4. Continue gradual modularization; MainActivity is still 2,077 lines. Do not scaffold empty modules to claim stages.
5. Implement/test horizon/tilt and framing guidance using real local sensor/analysis inputs. No pose, smile, smart focus or identity-inference engine has been integrated in this work.
6. Select a real on-device segmentation/inference adapter only after checking code and exact weight licenses, size/hash, Android performance and offline behavior. No model weights or GPU inference are verified here.
7. Complete shared project storage and the product/wedding/family/events/restoration/album/printing workflows. Inherited adapters/screens are not proof of fully working studios.
8. Complete accessibility, backup/device-transfer privacy, Android 26–28 export/orientation behavior, commercial asset/model licensing and release signing gates.

See `docs/STAGE_LEDGER.md`, `docs/DEPENDENCY_LICENSE_REVIEW.md` and `docs/evidence/verification-summary.json`.
The 91-stage roadmap and Stage 150 are **not completed or accepted**. Do not merge this draft into main as a final release.
