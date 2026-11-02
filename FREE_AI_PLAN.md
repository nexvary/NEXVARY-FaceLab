# FaceLab Free / Self-Hosted AI Plan

| الوظيفة | المسار المقترح | رسوم API | حالة الدمج |
|---|---|---:|---|
| تحسين/استعادة الوجه | GFPGAN أو Local Enhance | 0 | Adapter جاهز |
| تكبير/تصغير العمر | SAM / بديل مرخص تجاريًا لاحقًا | 0 | Adapter جاهز، يحتاج weights |
| الشعر | HairFastGAN / بديل مرخص | 0 | Adapter جاهز، يحتاج reference/weights |
| اللحية/الابتسامة | semantic editor provider | 0 | واجهة Provider جاهزة |
| Upscale | Real-ESRGAN | 0 | التالي |

المبدأ: التطبيق لا يستدعي Hugging Face Inference أو Replicate أو أي خدمة محاسبة لكل صورة. كل المعالجة على جهاز/سيرفر نملكه.
