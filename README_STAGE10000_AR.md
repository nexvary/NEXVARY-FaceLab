# NEXVARY FaceLab — Stage 10500

هذه المرحلة تركز على جعل Short Film Studio قابلاً للتجهيز على جهاز Windows الحقيقي بدلاً من بقاء المحركات مجرد أسماء في الواجهة.

## الجديد
- Engine Doctor يفحص NVIDIA GPU وVRAM وDriver وPyTorch وCUDA والمساحة المتاحة وحالة كل محرك.
- 12 Motion Presets أولية: مشي، جلوس، تلويح، التفات، حديث، ابتسامة، نظرة للكاميرا، reaction، hero pose وغيرها.
- Endpoint جديد: `GET /v1/film/doctor`.
- Endpoint جديد: `GET /v1/film/motions`.
- Bootstrap Windows لتنزيل مستودعات Wan 2.2 وMimicMotion وLivePortrait وEchoMimic V2 وMuseTalk إلى هارد منفصل.
- LivePortrait runner فعلي يتعامل مع `inference.py` المحلي ويستخرج MP4 الناتج.
- Routing أصبح يحترم `preferred_engine` الذي يأتي من Motion Preset قبل اللجوء إلى التحليل النصي.
- كل محرك يبقى في بيئة مستقلة لتجنب تضارب PyTorch/CUDA.

## نقطة مهمة
ملف bootstrap يقوم بجلب المصدر فقط ولا يفرض تنزيل أوزان ضخمة. الأوزان وبيئات Python لكل مشروع تُثبت بعد معرفة GPU/VRAM من Engine Doctor. هذه المرحلة لا تدّعي أن Wan/MimicMotion/LivePortrait تولد على هذا الجهاز ما لم تكن مستودعاتها وأوزانها موجودة فعلاً.
