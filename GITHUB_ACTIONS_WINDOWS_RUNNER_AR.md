# NEXVARY FaceLab — GitHub Actions Windows Runner

تم تجهيز المشروع للبناء على `windows-latest` بدل الاعتماد على Android Studio المحلي في كل تعديل.

الـWorkflow:

`.github/workflows/facelab-windows.yml`

ويقوم بالآتي:

1. Checkout للمصدر.
2. تثبيت Java 17 وPython 3.12.
3. فحص Python syntax.
4. تشغيل UI Release Gate إن كان موجودًا.
5. تشغيل `:app:compileDebugKotlin`.
6. تشغيل `:app:assembleDebug`.
7. التأكد فعليًا من وجود `app-debug.apk`.
8. رفع APK كـGitHub Actions Artifact.
9. رفع تقارير البناء حتى عند الفشل.

العنوان الافتراضي للخادم المضمّن في APK:

`http://192.168.1.107:8000`

يمكن تغييره عند التشغيل اليدوي للـWorkflow من GitHub دون تعديل الكود.

نجاح الـWorkflow يعني أن Kotlin compile وAPK assembly قد نجحا فعليًا على Windows Runner.
