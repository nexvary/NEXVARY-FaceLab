# NEXVARY FaceLab — Stage 35500 Compiler Fix

هذه المرحلة لا تضيف ميزات. تم عزل إصلاح خاص ببطاقة Before/After ComparePreviewCard بعد ظهور خطأ Kotlin وحيد متبقٍ أثناء Build على Windows.

الإصلاح:
- إزالة الاعتماد على `PointerInputScope.size` داخل gesture lambda.
- التقاط عرض البطاقة باستخدام `Modifier.onSizeChanged` في `previewWidthPx`.
- حساب نسبة السحب من قيمة Int صريحة وتحويلها إلى Float.
- حساب عرض الجزء المقارن مرة واحدة كـ Dp (`resultWidth`).
- استخدام `width(resultWidth)` بدل تركيب `fillMaxWidth(fraction)` في هذا المسار.
- الحفاظ على clipToBounds للجزء الناتج وخط الفصل.

الهدف: تقليل الغموض في overload/type inference في Kotlin/Compose على بيئة Android Studio لدى المستخدم.
