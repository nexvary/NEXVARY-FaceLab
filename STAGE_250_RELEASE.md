# NEXVARY FaceLab — Stage 250

Stage 250 focuses on product polish and persistence rather than adding paid cloud dependencies.

## 201–215 — Comparison UX
- Interactive Before/After comparison directly on the main preview.
- Drag anywhere horizontally to move the comparison divider.
- Clear BEFORE/AFTER labels in all six supported languages.
- Existing single-view Before/After toggle retained for accessibility and simple use.

## 216–230 — Persistent gallery
- AI results are stored in a private FaceLab application gallery.
- Gallery survives app restarts.
- Maximum 30 locally cached results to prevent uncontrolled storage growth.
- Open any saved result back in the editor.
- Clear-local-gallery action included.

## 231–240 — AI engine visibility
- Model Manager now queries `/v1/models` from the self-hosted server.
- The server model report can be inspected in the app after a successful engine check.
- No paid per-image API dependency was added.

## 241–250 — Reliability & release polish
- Gallery reopening now restores a valid editor source state.
- Stage/version raised to 250.0.0.
- Python AI server syntax and smoke tests passed.
- ZIP excludes Python bytecode caches.
- Full Android compile could not be executed in the build environment because `services.gradle.org` DNS resolution is unavailable; this is recorded rather than reported as a false success.
