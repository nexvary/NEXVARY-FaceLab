# NEXVARY FaceLab — Stage 3500

Stage 3500 introduces **NEXVARY Short Film Studio** as a new application section after the image, wardrobe, and heritage workflows.

## Implemented
- New Film tab in the Android application.
- Final edited portrait preview is passed into Film Studio.
- Six film presets: cinematic, historical, travel, drama, documentary, commercial.
- Duration selector: 30 seconds to 5 minutes.
- Story/action prompt editor.
- Continuity lock UX for face/character, wardrobe, location, lighting/camera.
- AI Director storyboard request from Android to the self-hosted API.
- Local deterministic storyboard planner splitting a film into short shots.
- Every shot includes timing, camera guidance, continuity locks and a generation prompt.
- Motion-engine status probing for CogVideoX-2B, LTX-Video, MuseTalk and FFmpeg via local environment paths.
- No paid inference API dependency.

## New API
- `GET /v1/film/status`
- `GET /v1/film/presets`
- `GET /v1/film/plan?story=...&duration_seconds=60&preset=cinematic`
- `POST /v1/film/render` is intentionally guarded until an audited local video engine snapshot is configured and its model-specific adapter has passed hardware validation.

## Important release boundary
Stage 3500 implements the Film Studio UI, storyboard/orchestration layer and local engine discovery. It does **not** falsely claim to bundle working video-model weights. Actual image-to-video rendering requires a locally installed model whose exact code + weights license has been approved and whose VRAM/runtime behavior has been tested on the target machine.
