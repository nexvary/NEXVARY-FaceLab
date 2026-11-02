# NEXVARY FaceLab — Stage 40

هذه الحزمة تقفز بالمشروع من النموذج الأولي 0.4 إلى خط أساس هندسي Stage 40.

## المراحل 1–40 المنجزة
1–5: تثبيت بنية Gradle/JDK، Compose، RTL، اختيار الصور والمعاينة.  
6–10: قبل/بعد، شدة التأثير، الحفظ والمشاركة، فلاتر محلية، اكتشاف الوجه.  
11–15: فصل Android عن محرك AI، Self-hosted API، health/capabilities، Multipart.  
16–20: Provider abstraction، تحسين محلي، GFPGAN adapter، SAM adapter، Hair adapter.  
21–25: HyperStyle/semantic adapter reservation، أخطاء HTTP واضحة، حد 25MB، أنواع صور مقيدة.  
26–30: Registry موحد للتأثيرات، فحص جاهزية Providers، عدم اشتراط API مدفوعة، إعداد عنوان الخادم وقت البناء.  
31–35: Request IDs، قياس زمن المعالجة، تنظيف مجلدات العمل، Cache للنتائج، مفاتيح cache مرتبطة بالصورة/الأداة/القوة.  
36–40: API version 40، capabilities موسعة، توثيق ترخيص، خطة نماذج مجانية، اختبارات smoke/syntax وإرشادات تشغيل Windows/Linux.

## مبدأ الترخيص
لا تعتمد النسخة على API سحابية مدفوعة. أي نموذج إضافي يجب مراجعة ترخيص الكود **وترخيص الأوزان** قبل تفعيله تجاريًا.

## تشغيل الخادم
Windows: `ai-server\\run_windows.bat`  
Linux: `cd ai-server && ./run_linux.sh`

## بناء Android
Windows: `gradlew.bat clean assembleDebug --stacktrace`  
Linux: `./gradlew clean assembleDebug --stacktrace`

الهاتف الحقيقي: عيّن `FACE_API_BASE_URL=http://IP-OF-PC:8000` قبل البناء. المحاكي يستخدم `10.0.2.2` افتراضيًا.
