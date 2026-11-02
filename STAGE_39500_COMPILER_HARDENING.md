# NEXVARY FaceLab — Stage 39500 Compiler Hardening

This release fixes the real Kotlin compiler error observed on Windows/Android Studio after Stage 39000.

Root cause:
- `ModalBottomSheet` from Material 3 is an experimental API in the Compose Material3 version used by this project.
- `FaceLabBottomBar` invoked it without opting in, which is a Kotlin compilation error (not merely a warning) under the project's compiler settings.

Fix:
- Added `@OptIn(ExperimentalMaterial3Api::class)` directly to `FaceLabBottomBar`.
- Scanned the full Android Kotlin source tree for other known experimental Material3/Foundation API usages; no second occurrence was found.
- Kept the Stage 37000 proven architecture and Stage 39000 UI changes intact.
