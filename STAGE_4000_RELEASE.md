# NEXVARY FaceLab — Stage 4000 Release

Version: `4000.0.0`

Stage 4000 adds the first executable Short Film rendering pipeline: persistent local film jobs, asynchronous queueing, a real MP4 FFmpeg motion-preview renderer, safe local AI video adapter support, job status/download endpoints, and Android submission/progress/save controls.

## Truthful rendering modes

The FFmpeg fallback creates a real video file with cinematic camera motion from the final still. It does not claim to synthesize body motion. Full image-to-video animation is enabled only when an explicitly configured, license-audited local engine adapter is present.

## Privacy / cost

The pipeline remains self-hosted and adds no paid per-request inference API.
