# NEXVARY FaceLab — Stage 22500

Stage 22500 turns the Stage 22000 timeline data into an editable visual timeline inside Android.

Implemented:
- Visual horizontal shot cards with duration-scaled widths.
- Video / Motion / Audio / Subtitle track metadata shown per shot.
- Shot selection and direct preview.
- Duration trim editing (1–60 seconds server-side guard).
- Nudge left/right to reorder shots.
- Dedicated timeline undo/redo history, capped to 30 states.
- Timeline refresh and history API.
- Version aligned to 22500.0.0.

New API:
- POST `/v1/film/jobs/{job_id}/timeline/{shot_id}/duration`
- POST `/v1/film/jobs/{job_id}/timeline/{shot_id}/nudge`
- POST `/v1/film/jobs/{job_id}/timeline/undo`
- POST `/v1/film/jobs/{job_id}/timeline/redo`
- GET `/v1/film/jobs/{job_id}/timeline/history`

AI engine GPU inference is not claimed by this stage. Full Android Gradle compilation depends on an Android/Gradle environment with required dependencies available.
