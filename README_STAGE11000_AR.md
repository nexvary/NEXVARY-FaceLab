# NEXVARY FaceLab — Stage 11000

هذه المرحلة تربط Motion Studio فعليًا بخط الفيلم بدل بقاء حركات Stage 10000 مجرد كتالوج.

## الجديد
- شاشة Motion Studio داخل Android مع 12 حركة واختيار المحرك الأنسب.
- `motion_preset` أصبح يدخل إلى Storyboard وإلى Render Job.
- Storyboard يضيف `preferred_engine` و`motion_preset` لكل لقطة.
- API جديد لمكتبة Driving Motions الخاصة: GET/POST `/v1/film/driving-motions`.
- يمكن إضافة مقطع حركة خاص بك؛ يحفظ محليًا ويصبح متاحًا لـ LivePortrait/MimicMotion.
- Engine Doctor وMotion Library يمكن فحصهما من الهاتف.
- Bootstrap Windows منفصل لـ LivePortrait وMimicMotion باستخدام بيئات Conda مستقلة.

## تجهيز Windows
```bat
cd ai-server
install_stage11000_motion_windows.bat D:\NEXVARY-AI
download_stage11000_motion_weights_windows.bat D:\NEXVARY-AI
configure_stage11000_motion_windows.bat D:\NEXVARY-AI
```
ثم أغلق CMD وافتحه من جديد وشغل الخادم.

## ملاحظة
الحزمة لا تحتوي أوزان AI الكبيرة. التنزيل يتم على جهازك من المصادر الرسمية. مشروع Stage 11000 يفترض الاستخدام الشخصي/الترفيهي الخاص مع الأصدقاء، مع احترام شروط كل نموذج ومقطع حركة.
