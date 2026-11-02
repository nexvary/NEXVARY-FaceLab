# Self-hosted server setup

Use Python 3.12 in a fresh virtual environment. Install `ai-server/requirements.txt`.
Run from `ai-server`: `python -m uvicorn app:app --host 127.0.0.1 --port 8000`.

- The supplied launchers now bind loopback by default. `FACELAB_BIND_HOST` explicitly changes that binding.
- For production set `FACELAB_ENV=production` and a strong `FACELAB_API_TOKEN`. Startup fails without a token in production mode. Terminate HTTPS at a trusted reverse proxy and bound request sizes there as well.
- `FACELAB_DATA_ROOT` defaults to a private per-user application directory, created with mode 0700 on Unix. Existing or explicitly supplied directories retain their permissions; configure a private volume for deployment.
- Image caching is off by default. Temporary edit responses are deleted after the response completes. Film projects and explicitly saved assets persist until deleted; this is not a blanket deletion guarantee for every module.
- `FACELAB_CACHE=1` explicitly opts in to image result caching; `FACELAB_CACHE_TTL_HOURS` defaults to 24. Do not enable shared-user deployments without per-user authorization and storage isolation (not implemented).
- In Android Settings, configure the server URL and token, then explicitly allow uploads for the current app process. Changing the URL revokes upload permission. Credentials are kept only in memory.
- Release builds require HTTPS. Debug builds also allow HTTP for local emulator/LAN testing; never distribute a debug APK as a production release.
- Built-in Pillow enhancement works without AI weights. Optional generative/background/video adapters require separately installed, licensed models. Their existence is not proof of working inference.

## Checks

`python -m pytest ai-server/tests -q` from the repository root tests real image processing, input rejection, authorization, cache behavior and production startup protection.

`pip-audit -r ai-server/requirements.txt` checks known advisory data; a clean result is not a full security audit.
