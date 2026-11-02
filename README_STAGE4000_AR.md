# NEXVARY FaceLab — Stage 4000

هذه المرحلة تنقل **Short Film Studio** من طبقة التخطيط فقط إلى خط إنتاج فيديو قابل للتشغيل محليًا.

## ما تم تنفيذه فعليًا

- Job Queue محلي لمهام الفيديو مع `job_id` وحالات `queued / running / completed / failed` ونسبة تقدم.
- رفع الصورة النهائية من Android إلى الخادم مع القصة، المدة، النمط، ووضع الرندر.
- إخراج **MP4 فعلي** عبر FFmpeg كـ Motion Preview بحركة كاميرا ناعمة على الصورة النهائية.
- حفظ الفيديو النهائي من Android مباشرة إلى `Movies/NEXVARY FaceLab`.
- Adapter آمن لمحرك Image-to-Video محلي عبر `FACELAB_VIDEO_ENGINE_CMD_JSON`، بدون `shell=True` وبدون API مدفوعة.
- Storyboard من 30 ثانية إلى 5 دقائق مع Continuity Locks للوجه والشخصية والملابس والموقع.
- فصل واضح بين `preview` و`ai`: معاينة FFmpeg تحرك الكاميرا فقط، أما حركة الوجه/الجسم فتحتاج نموذج Image-to-Video محليًا.

## API الجديدة

```text
POST /v1/film/render
GET  /v1/film/jobs/{job_id}
GET  /v1/film/jobs/{job_id}/video
```

وتستمر:

```text
GET /v1/film/status
GET /v1/film/presets
GET /v1/film/plan
```

## أوضاع الرندر

- `auto`: يستخدم AI المحلي إذا كان Adapter مدققًا ومضبوطًا، وإلا FFmpeg preview.
- `preview`: يولد MP4 حقيقي بحركة كاميرا من الصورة.
- `ai`: يرفض التشغيل إن لم يكن هناك Adapter محلي مُعدّ، بدل إعطاء نجاح وهمي.

## تركيب Adapter محلي

مثال فقط بعد تدقيق ترخيص المحرك والأوزان التي ستستخدمها فعليًا:

```text
FACELAB_VIDEO_ENGINE_CMD_JSON=["python","D:/AI/video-engine/render.py","--image","{input}","--prompt","{prompt}","--seconds","{duration}","--output","{output}"]
```

لا يقوم FaceLab بتشغيل الأمر داخل shell، لتقليل مخاطر حقن الأوامر من نص القصة.

## ملاحظة الجودة

Stage 4000 ينتج فيديو MP4 حقيقي في وضع Preview، لكنه **ليس ادعاءً بحركة جسم AI**. حركة الشخص الفعلية مرتبطة بتركيب نموذج Image-to-Video محلي مدقق ومتوافق مع عتاد الجهاز.
