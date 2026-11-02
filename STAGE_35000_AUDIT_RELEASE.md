# NEXVARY FaceLab — Stage 35000 AUDITED

This release is a stabilization/audit gate, not a feature expansion.

Hardening performed:
- Rechecked Compose collection states (`favorites`, `savedLooks`, `closets`) and explicit delegate imports.
- Android backup disabled (`allowBackup=false`) for privacy.
- Backend rejects unsafe traversal/control path segments before filesystem-backed film routes.
- Removed generated `__pycache__`/`.pyc` artifacts from the source package.
- Added `qa_release_35000.py` as a current-version release gate instead of relying on historical version-pinned QA scripts.
- Full static/source checks cover Python, XML, JSON, Kotlin lexical structure, heritage assets/catalog, versions, shell syntax, RTL, SDK configuration, and FastAPI route inventory.

Important verification boundary:
- Full Android Gradle/Kotlin compile still requires a machine that can reach Gradle/Maven repositories (or has the dependencies cached). This environment cannot resolve services.gradle.org, so no APK compile success is claimed here.
- Real GPU inference with Wan/LivePortrait/MimicMotion is not claimed here.
