package com.nexvary.facelab

import androidx.camera.core.ImageProxy

/** Samples the visible Y plane using row/pixel strides, without copying or uploading frames. */
internal object LightingAnalyzer {
    fun meanLuma(frame: ImageProxy, maxSamples: Int = 4096): Int {
        val plane = frame.planes.firstOrNull() ?: return 0
        val crop = frame.cropRect
        return LumaSampler.measure(
            plane.buffer, frame.width, frame.height, plane.rowStride, plane.pixelStride,
            crop.left, crop.top, crop.right, crop.bottom, maxSamples
        )?.mean ?: 0
    }

    fun guidance(luma: Int, arabic: Boolean): String = when {
        luma < 65 -> if (arabic) "الإضاءة ضعيفة: اقترب من مصدر إضاءة" else "Low light: move toward a light source"
        luma > 205 -> if (arabic) "الإضاءة قوية جدًا: تجنب الوهج المباشر" else "Strong light: avoid direct glare"
        else -> if (arabic) "الإضاءة مناسبة مبدئيًا" else "Lighting looks usable"
    }
}
