package com.nexvary.facelab

import java.nio.ByteBuffer
import kotlin.math.ceil
import kotlin.math.sqrt

internal data class LumaReading(val mean: Int, val samples: Int)

/** Pure, bounded Y-plane sampling. The buffer's position and limit define the readable plane. */
internal object LumaSampler {
    fun measure(buffer: ByteBuffer, width: Int, height: Int, rowStride: Int, pixelStride: Int,
                left: Int = 0, top: Int = 0, right: Int = width, bottom: Int = height,
                maxSamples: Int = 4096): LumaReading? {
        require(maxSamples > 0)
        if (width <= 0 || height <= 0 || rowStride <= 0 || pixelStride <= 0) return null
        if (left < 0 || top < 0 || right > width || bottom > height || right <= left || bottom <= top) return null
        val cropWidth = right - left
        val cropHeight = bottom - top
        val columns = minOf(cropWidth, maxSamples, ceil(sqrt(maxSamples.toDouble() * cropWidth / cropHeight)).toInt()).coerceAtLeast(1)
        val rows = minOf(cropHeight, maxSamples / columns).coerceAtLeast(1)
        val start = buffer.position().toLong()
        var total = 0L
        var count = 0
        for (row in 0 until rows) {
            val y = top + (row.toLong() * cropHeight / rows).toInt()
            for (column in 0 until columns) {
                val x = left + (column.toLong() * cropWidth / columns).toInt()
                val index = start + y.toLong() * rowStride + x.toLong() * pixelStride
                if (index in start until buffer.limit().toLong()) {
                    total += buffer.get(index.toInt()).toInt() and 255
                    count++
                }
            }
        }
        return if (count == 0) null else LumaReading((total / count).toInt(), count)
    }
}
