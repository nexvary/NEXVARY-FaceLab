# NEXVARY FaceLab — Stage 10500

مرحلة الربط الحقيقي الأولى لمحركات الحركة المحلية.

## الجديد
- إصلاح LivePortrait adapter ليتوافق مع CLI الرسمي (`-s` و `-d`).
- دعم Driving Motion Library محلية؛ المستخدم يضع فيديوهات الحركة التي يملك حق استخدامها.
- تمرير `{driving}` تلقائياً إلى أوامر المحركات بحسب `motion_preset`.
- MimicMotion runner ينشئ YAML خاص بكل مهمة ويستدعي `inference.py --inference_config`.
- إصلاح Auto AI: يعتبر أي محرك Multi-Engine جاهزاً، وليس فقط الـ legacy external adapter.
- عدم تنزيل أو تضمين فيديوهات أشخاص أو أوزان ضخمة داخل الحزمة.

## مثال إعداد LivePortrait
set FACELAB_LIVEPORTRAIT_DIR=D:\NEXVARY-AI\LivePortrait
set FACELAB_LIVEPORTRAIT_CMD_JSON=["python","ai-server/adapters/liveportrait_runner.py","--input","{input}","--driving","{driving}","--output","{output}"]

## مثال إعداد MimicMotion
set FACELAB_MIMICMOTION_DIR=D:\NEXVARY-AI\MimicMotion
set FACELAB_MIMICMOTION_CMD_JSON=["python","ai-server/adapters/mimicmotion_runner.py","--input","{input}","--driving","{driving}","--output","{output}"]

انسخ `driving_motions_manifest.example.json` إلى مجلد Driving Motions باسم `manifest.json`، ثم ضع فيديوهاتك المرجعية في المسارات المسجلة داخله.
