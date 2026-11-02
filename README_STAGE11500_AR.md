# NEXVARY FaceLab — Stage 11500

هذه المرحلة توسع Motion Studio بإضافة رفع فيديو الحركة من الهاتف مباشرة، ومعاينة الحركة، وإنشاء LivePortrait Motion Template خاص بصيغة .pkl مع خيار حذف فيديو القيادة الأصلي بعد نجاح إنشاء القالب لرفع الخصوصية.

## الجديد
- رفع Driving Motion من Android عبر SAF بدون صلاحية تخزين واسعة.
- GET preview لأي driving video مسجل.
- إنشاء Motion Template عبر LivePortrait الرسمي من فيديو قيادة مسجل.
- Privacy Mode: حذف الفيديو الأصلي بعد نجاح إنشاء القالب.
- تصنيف عناصر المكتبة إلى driving_video / motion_template.
- واجهة Android لإضافة الحركة، معاينتها، وتحويلها إلى قالب خاص.

## ملاحظات مهمة
إنشاء قالب LivePortrait يتطلب تثبيت LivePortrait وأوزانه محليًا وضبط FACELAB_LIVEPORTRAIT_DIR. لا يتم ادعاء نجاح GPU inference في بيئة البناء الحالية.
