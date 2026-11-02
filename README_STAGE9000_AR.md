# NEXVARY FaceLab — Stage 9000

هذه الحزمة تمثل قفزة تطويرية متتابعة عبر عشر مراحل: 4500، 5000، 5500، 6000، 6500، 7000، 7500، 8000، 8500، 9000.

النتيجة النهائية هي خط إنتاج أفلام قصيرة محلي Self-hosted مبني فوق الصورة النهائية التي أنشأها المستخدم بعد تعديل الوجه والملابس والخلفية.

## أهم القدرات
- Storyboard متعدد اللقطات مع Timeline ومعلومات الاستمرارية.
- Render متعدد اللقطات ثم دمج فعلي MP4 عبر FFmpeg.
- Adapter محلي آمن لمحرك Image-to-Video بدون shell وبدون API مدفوعة.
- Character / Face / Wardrobe / Location / Color-grade locks كبيانات استمرارية لكل لقطة.
- تحرير بيانات كل لقطة من API: prompt، camera، dialogue، narration، transition، duration، enabled.
- Subtitles SRT من الحوار/السرد.
- رفع مسار Narration أو Music ثم مزج الصوت محليًا عبر FFmpeg.
- Hook مستقل لـ Lip Sync المحلي بعد تركيب محرك مدقق الترخيص.
- إلغاء، إعادة محاولة، واستكمال اللقطات التي تم توليدها بالفعل بدل إعادة كل الفيلم.
- سجل Job history، Render log، Thumbnail، Project archive ZIP.
- نسب 16:9 و9:16 و1:1 وتصدير Preview / 720p / 1080p.
- واجهة Android محدثة لاختيار النسبة والجودة ومتابعة اللقطة الحالية وإلغاء/إعادة التوليد.

## تشغيل الخادم
```bat
cd ai-server
run_film_windows.bat
```

## تجهيز Runtime الفيديو
```bat
cd ai-server
install_video_engine_windows.bat
```
هذا السكربت لا يقوم بتنزيل أوزان نموذج فيديو. يجب وضع نموذج محلي تم تدقيق ترخيصه ثم ضبط المسار.

## البناء على Windows
```bat
gradlew.bat clean assembleDebug --stacktrace
```
