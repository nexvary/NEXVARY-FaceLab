# FaceLab source restoration audit — 2026-11-01

Source: user-supplied `NEXVARY-FaceLab-Stage40000-GITHUB-ACTIONS-WINDOWS.zip`.

- This is an Android Kotlin/Compose application plus Python AI server, not merely the existing README-only GitHub scaffold.
- `python -m compileall -q ai-server`: PASS (syntax only).
- `python qa_ui_release_gate_39000.py`: 96 PASS, 0 FAIL after correcting two obsolete assertions in that QA script (versionCode 39500 vs current 40000; navigation expected `heightIn(min=72.dp)` despite current 78dp nav cells). No application UI code was modified for this QA correction.
- `python qa_stage37000_strict.py`: FAIL due to historic hardcoded version 37000 expectations; this is not a Stage 40000 acceptance test.
- Android APK compilation: NOT VERIFIED. Gradle bootstrap downloads from the network; build was not run in this environment.
- Previous GitHub `main` contained only README and workflow. This restored source has NOT been pushed or merged.

Next: publish on a dedicated restoration branch; validate Android CI; then split large activity, add CameraX and on-device guidance as independent modules. Keep production credentials out of repository and use HTTPS for production API traffic.
