# NEXVARY FaceLab — Stage 1500

تم تطوير خزانة الملابس إلى استوديو مظهر متكامل:

- كتالوج بصري منظم.
- تبويبات مستقلة للملابس، الأطقم الكاملة، الأحذية، النظارات والقبعات.
- أطقم كاملة متعددة القطع.
- AI Stylist يقترح اللوك بحسب المناسبة.
- مناسبات: العمل، الزفاف، السفر، جلسة التصوير، الرياضة، الشتاء، السهرة.
- دعم اللون والإكسسوارات وحفظ اللوك.
- نقل `occasion` و `outfit` إلى خادم AI المحلي.
- لا توجد خدمة استدلال مدفوعة لكل صورة.

## البناء على Windows
```bat
gradlew.bat clean assembleDebug --stacktrace
```

## تشغيل خادم AI
```bat
cd ai-server
install_generative_windows.bat
run_generative_windows.bat
```

إذا كان الهاتف حقيقيًا وليس Emulator، ابنِ التطبيق مع عنوان IPv4 للكمبيوتر في `FACE_API_BASE_URL`.
