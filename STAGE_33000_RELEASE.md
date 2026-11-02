# NEXVARY FaceLab — Stage 33000

Stage 33000 upgrades the Android film timeline with a rendered waveform strip and drag scrubbing, J/K/L playback transport semantics, keyframe curve sampling/visualization, proxy-cache metadata/pruning, and persistent shot/track color tags.

## Backend endpoints
- GET `/v1/film/jobs/{job_id}/timeline/stage33000`
- POST `/v1/film/jobs/{job_id}/timeline/scrub`
- POST `/v1/film/jobs/{job_id}/timeline/playback`
- POST `/v1/film/jobs/{job_id}/timeline/color-tag`
- GET `/v1/film/jobs/{job_id}/timeline/{shot_id}/curve`
- GET `/v1/film/jobs/{job_id}/timeline/proxy-cache`
- POST `/v1/film/jobs/{job_id}/timeline/proxy-cache/prune`

Proxy preview generation now records a cache entry after a successful preview render.
