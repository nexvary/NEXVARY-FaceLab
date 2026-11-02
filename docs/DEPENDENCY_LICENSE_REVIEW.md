# Dependency and model review — 2026-11-02

## Implemented runtime and test packages

Python licenses below were read from the installed distributions' metadata in a clean Python 3.12 environment, not inferred from project names.

| Package | Pinned version | Declared license |
|---|---|---|
| FastAPI | 0.162.0 | MIT |
| Starlette | 1.10.0 | BSD-3-Clause |
| Uvicorn | 0.54.0 | BSD-3-Clause |
| python-multipart | 0.0.33 | Apache-2.0 |
| Pillow | 13.0.0 | MIT-CMU |
| HTTPX (tests) | 0.28.1 | BSD-3-Clause |
| pytest (tests) | 9.2.0 | MIT |

The initial requirements resolved to older Pillow, Starlette and python-multipart distributions with known advisory findings. After upgrading and retesting, `pip-audit -r ai-server/requirements.txt` reported no known vulnerabilities. See `evidence/python-dependency-audit.json`; this is a dated advisory scan, not proof of absence of vulnerabilities.

Android uses Kotlin 2.0.21, AGP 8.8.2, Compose BOM 2025.02.00, Activity Compose 1.10.0, CameraX 1.4.2 and Lifecycle 2.8.7. The cached CameraX camera-core 1.4.2 POM declares Apache-2.0 plus a BSD notice for libyuv; Activity Compose and Kotlin stdlib POMs declare Apache-2.0. Their transitive notices must be included in the commercial distribution notice inventory. `evidence/android-release-dependencies.txt` preserves the resolved graph and its version-selection arrows; `evidence/android-pom-license-inventory.json` inventories 126 POMs mentioned in that graph, including constraints/unselected version mentions. It is a review aid, not a final notice bundle. Android instrumentation dependencies are test-only and do not ship inside the production application APK. Gradle's debug and release runtime dependency graph is the authoritative resolved inventory, rather than this direct-dependency summary.

## AI adapters and weights

No `.onnx`, `.tflite`, `.safetensors`, `.pt` or `.pth` model files were present among the recovered/published source files. Optional adapters are retained; no new inference weights were downloaded, benchmarked or approved for commercial redistribution in this work.

The inherited license notes for GFPGAN, rembg, diffusion, LivePortrait, MimicMotion and other video providers remain review leads. They are not commercial approval for code, weights, dependencies or training-data restrictions. The model ID/revision, model weight SHA-256, applicable license text and tested runtime must be recorded before enabling each engine for release. Camera luminance analysis and JPEG capture do not use model weights or identity recognition.

## Remaining release gates

- Complete a transitive Android/Python notice bundle from the exact release dependency graph and distribution license files.
- Audit the provenance/reuse rights of inherited illustrations and image assets.
- Choose and document the project's own source license; do not infer a grant from the public repository's visibility.
- Approve each optional model separately. No commercial-model approval or GPU inference success is claimed.
