# NEXVARY FaceLab — Smart Camera foundation

- Added isolated CameraX preview and real-time Y-plane brightness sampling, offline.
- Camera permission is requested at runtime; no image upload or storage.
- Analysis uses KEEP_ONLY_LATEST and always closes ImageProxy.
- Camera is an independent Activity, accessible from the Editor top action; this avoids destabilizing the 200KB MainActivity.
- This is luminance guidance, **not** AI pose/composition/focus/smile analysis.
- Pending: capture flow, orientation testing, UI polish, Gradle compile on Android SDK-equipped CI.
- Security follow-up: legacy cleartext HTTP remains and must be restricted to debug/local networking before release.
