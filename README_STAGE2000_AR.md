# NEXVARY FaceLab — Stage 2000

أصبح قسم خزانة الملابس يدعم تكوين اللوك قطعة بقطعة بدل الاعتماد على قطعة أو Outfit جاهز فقط.

يمكن اختيار:

- الجزء العلوي.
- الجزء السفلي.
- الطبقة الخارجية.
- الحذاء.
- لون اللوك.
- الإكسسوار.
- المناسبة.

كما أصبح AI Stylist يعرض ثلاثة اقتراحات محلية لكل مناسبة، مع إمكانية حفظ التكوين داخل «غرفة ملابس» باسم يحدده المستخدم. الحفظ يتم محليًا داخل التطبيق.

## بناء Android على Windows

```bat
gradlew.bat clean assembleDebug --stacktrace
```

## تشغيل خادم AI على Windows

```bat
cd ai-server
install_generative_windows.bat
run_generative_windows.bat
```

## Endpoint الجديد

```text
GET /v1/wardrobe/stylist/recommendations?occasion=work&gender=all&climate=mild&limit=3
```

لا تتم إضافة API مدفوعة لكل صورة في Stage 2000.
