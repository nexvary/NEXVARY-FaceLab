package com.nexvary.facelab

import androidx.camera.core.ImageProxy

/** Samples the visible Y plane using row/pixel strides, without copying or uploading frames. */
internal object LightingAnalyzer {
    fun meanLuma(frame: ImageProxy, maxSamples: Int = 4096): Int {
        val plane = frame.planes[0]
        val buffer = plane.buffer
        val width = frame.width
        val height = frame.height
        if (width <= 0 || height <= 0) return 0
        val stride = maxOf(1, kotlin.math.sqrt((width.toLong() * height / maxOf(1, maxSamples)).toDouble()).toInt())
        var sum = 0L
        var count = 0
        for (y in 0 until height step stride) {
            for (x in 0 until width step stride) {
                val index = y.toLong() * plane.rowStride + x.toLong() * plane.pixelStride
                if (index >= 0 && index < buffer.limit()) {
                    sum += buffer.get(index.toInt()).toInt() and 0xff
                    count++
                }
            }
        }
        return if (count == 0) 0 else (sum / count).toInt()
    }

    fun guidance(luma: Int, arabic: Boolean): String = when {
        luma < 65 -> if (arabic) "الإضاءة ضعيفة: اقترب من مصدر إضاءة" else "Low light: move toward a light source"
        luma > 205 -> if (arabic) "الإضاءة قوية جدًا: تجنب الوهج المباشر" else "Strong light: avoid direct glare"
        else -> if (arabic) "الإضاءة مناسبة مبدئيًا" else "Lighting looks usable"
    }
}
