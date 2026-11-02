# NEXVARY FaceLab — Stage 34000 FIXED

تضيف هذه المرحلة Preview Monitor حقيقيًا إلى Film Studio مع Timecode بصيغة SMPTE، خطوة إطار Frame Step للأمام/الخلف، Safe Areas، معاينة Onion للحواف الزمنية، وحفظ تخطيط Workspace.

## أهم الإضافات
- `GET /v1/film/jobs/{job_id}/timeline/monitor`
- `POST /v1/film/jobs/{job_id}/timeline/monitor/playhead`
- `POST /v1/film/jobs/{job_id}/timeline/monitor/frame-step`
- `POST /v1/film/jobs/{job_id}/timeline/monitor/safe-area`
- `POST /v1/film/jobs/{job_id}/timeline/monitor/onion`
- `POST /v1/film/jobs/{job_id}/timeline/workspace`
- `GET /v1/film/jobs/{job_id}/timeline/monitor/frame`

استخراج الإطار يستخدم FFmpeg من Proxy أو الفيديو النهائي/اللقطة المتاحة، ولا يدّعي وجود معاينة AI إذا لم يكن الفيديو قد تولد فعلاً.
