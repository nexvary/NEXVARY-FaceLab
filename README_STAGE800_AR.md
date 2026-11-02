# NEXVARY FaceLab — Stage 800

هذه النسخة تستمر من Stage 250 وتضيف بنية أقوى للخادم، نقل تخزين نماذج الذكاء الاصطناعي إلى قرص كبير، حماية اختيارية للتطبيق/الخادم بواسطة Token، وأدوات بورتريه جديدة.

## بناء تطبيق Android على Windows
```bat
gradlew.bat clean assembleDebug --stacktrace
```
الملف المتوقع:
`app\build\outputs\apk\debug\app-debug.apk`

## بعد تركيب القرص الجديد
مثال إذا كان القرص الجديد D:
```bat
cd ai-server
configure_storage_windows.bat D:\NEXVARY-AI
```
أغلق CMD وافتحه من جديد، ثم:
```bat
install_generative_windows.bat
run_generative_windows.bat
```

## هاتف حقيقي على نفس Wi-Fi
قبل البناء:
```bat
set FACE_API_BASE_URL=http://IP-الكمبيوتر:8000
```
ثم نفّذ أمر البناء.

## حماية الخادم اختيارياً
```bat
set FACELAB_API_TOKEN=YourSecretToken
set FACE_API_BASE_URL=http://IP-الكمبيوتر:8000
gradlew.bat assembleDebug
```
ويجب تشغيل الخادم بنفس قيمة `FACELAB_API_TOKEN`.

لا توجد رسوم API لكل صورة؛ المحرك مصمم للتشغيل الذاتي.
