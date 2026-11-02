# NEXVARY FaceLab — Stage 9000

أحدث نسخة تضيف NEXVARY Short Film Production Studio عبر عشر مراحل متتابعة من 4500 إلى 9000. راجع README_STAGE9000_AR.md وSTAGE_9000_RELEASE.md.

# تحديث Stage 4000 — NEXVARY Short Film Render Pipeline

تمت إضافة Job Queue للفيديو، إخراج MP4 فعلي عبر FFmpeg، Adapter آمن لمحرك Image-to-Video محلي، متابعة حالة المهمة وحفظ الفيديو من Android. راجع `README_STAGE4000_AR.md` و`STAGE_4000_RELEASE.md`.

# تحديث Stage 3500 — NEXVARY Short Film Studio

أضيف استوديو أفلام قصيرة بمدة 30 ثانية إلى 5 دقائق، AI Director محلي لتقسيم القصة إلى لقطات، Continuity Locks، وفحص لمحركات الفيديو المحلية. راجع `README_STAGE3500_AR.md` و`STAGE_3500_RELEASE.md`.

# تحديث Stage 3000
تم توسيع استوديو المعالم والتراث إلى 72 معلمًا في 9 دول، مع البحث والتصفية والمفضلة وحزمة صور مرخصة قابلة للتثبيت وفحص ترخيص قبل الإصدار. راجع README_STAGE3000_AR.md.

# تحديث Stage 2000

تمت إضافة تركيب اللوك قطعة بقطعة، ثلاثة اقتراحات AI Stylist، وغرف ملابس محفوظة محليًا. راجع `README_STAGE2000_AR.md`.

# NEXVARY FaceLab — Stage 170

واجهة متعددة اللغات (العربية، الإنجليزية، التركية، الإسبانية، الألمانية، الإيطالية) مع دعم RTL للعربية، اختيار لغة محفوظ، وتحسينات واجهة الهاتف.

# NEXVARY FaceLab — Stage 100

تطبيق Android لتحرير الوجه بالذكاء الاصطناعي مع خادم ذاتي الاستضافة، بدون API مدفوعة لكل صورة.

أبرز أدوات المرحلة 100: تغيير العمر، إضافة/إزالة اللحية، الشعر مع صورة تسريحة مرجعية اختيارية، لون الشعر، الابتسامة، المكياج، لون العين، تبييض الأسنان، تنعيم البشرة، تحسين AI، إزالة الخلفية وتمويهها.

## تشغيل الخادم على Windows

```bat
cd ai-server
install_generative_windows.bat
run_generative_windows.bat
```

لفتح دعم صورة تسريحة مرجعية بعد مراجعة تراخيص النموذج الذي ستستخدمه:

```bat
set ENABLE_IP_ADAPTER=1
run_generative_windows.bat
```

## بناء Android

```bat
gradlew.bat clean assembleDebug --stacktrace
```

إذا كان الهاتف الحقيقي على نفس الشبكة، اضبط عنوان الكمبيوتر قبل البناء:

```bat
set FACE_API_BASE_URL=http://192.168.1.10:8000
gradlew.bat assembleDebug
```

الـAPK المتوقع داخل:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## ملاحظة الترخيص

لا توجد رسوم API خارجية في هذا التصميم. مع ذلك، يجب مراجعة ترخيص كل وزن AI قبل توزيعه تجاريًا؛ ترخيص الكود وحده لا يضمن ترخيص ملفات النموذج.

## تحديث Stage 250
- مقارنة Before/After تفاعلية بالسحب على الصورة.
- معرض محلي دائم يحفظ آخر 30 نتيجة AI داخل مساحة التطبيق الخاصة.
- فتح النتائج المحفوظة مرة أخرى داخل المحرر ومسح المعرض عند الحاجة.
- صفحة النماذج تستعلم فعليًا عن `/v1/models` من خادم AI الذاتي.
- الإصدار الحالي: `250.0.0`.

---
## تحديث Stage 800
راجع `README_STAGE800_AR.md` و`STAGE_800_RELEASE.md`. الإصدار الحالي 800.0.0 ويضيف تخزين AI قابل للنقل إلى قرص مستقل، فحص مساحة التخزين/النظام، حماية Token اختيارية، وأدوات Portrait Relight / Blemish Cleanup / Eye Enhance / Professional Headshot / Studio Background.


## تحديث Stage 900
تمت إضافة **خزانة الملابس الذكية** كقسم رئيسي: 72 لوك في 8 أقسام، دعم استيراد صورة قطعة ملابس، وواجهات API ذاتية الاستضافة `/v1/wardrobe/*`. راجع `README_STAGE900_AR.md` و`STAGE_900_RELEASE.md`. الإصدار الحالي `900.0.0`.


## Stage 1000
Virtual Wardrobe 2.0: بحث وفلاتر ومفضلة وألوان وإكسسوارات وحفظ اللوك. الإصدار 1000.0.0.

## Stage 1500
تمت إضافة Virtual Wardrobe 3.0: كتالوج بصري، أطقم كاملة متعددة القطع، تبويبات أحذية/نظارات/قبعات، AI Stylist للمناسبات، وإرسال تكوين اللوك الكامل إلى الخادم المحلي. الإصدار 1500.0.0.

## Stage 10500
راجع `README_STAGE10000_AR.md`. تمت إضافة Engine Doctor وMotion Presets وWindows multi-engine bootstrap وLivePortrait local runner.
