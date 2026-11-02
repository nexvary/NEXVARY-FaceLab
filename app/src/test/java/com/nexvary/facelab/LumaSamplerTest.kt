package com.nexvary.facelab

import java.nio.ByteBuffer
import org.junit.Assert.*
import org.junit.Test

class LumaSamplerTest {
    @Test fun unsignedPaddedPixels() {
        val buffer = ByteBuffer.wrap(byteArrayOf(-1, 0, 127, 0, 9, 9, 64, 0, 0, 0))
        assertEquals(LumaReading(111, 4), LumaSampler.measure(buffer, 2, 2, 6, 2))
    }
    @Test fun respectsBufferPositionAndLimit() {
        val buffer = ByteBuffer.wrap(byteArrayOf(9, 9, 10, 20, 30, 40, 99))
        buffer.position(2); buffer.limit(6)
        assertEquals(LumaReading(25, 4), LumaSampler.measure(buffer, 2, 2, 2, 1))
        assertEquals(2, buffer.position())
    }
    @Test fun cropUsesOnlyVisiblePixels() {
        val buffer = ByteBuffer.wrap(byteArrayOf(0, 0, 0, 0, 10, 20, 0, 30, 40))
        assertEquals(LumaReading(25, 4), LumaSampler.measure(buffer, 3, 3, 3, 1, 1, 1, 3, 3))
    }
    @Test fun workIsBoundedOnLargeFrames() {
        val reading = LumaSampler.measure(ByteBuffer.wrap(ByteArray(1920 * 1080) { 100 }), 1920, 1080, 1920, 1, maxSamples = 257)!!
        assertTrue(reading.samples in 1..257)
        assertEquals(100, reading.mean)
    }
    @Test fun emptyOrInvalidPlanesDoNotReadOutOfBounds() {
        assertNull(LumaSampler.measure(ByteBuffer.allocate(0), 2, 2, 2, 1))
        assertNull(LumaSampler.measure(ByteBuffer.allocate(4), 0, 2, 2, 1))
        assertNull(LumaSampler.measure(ByteBuffer.allocate(4), 2, 2, 0, 1))
    }
    @Test fun truncatedLastRowIsSafe() {
        assertEquals(LumaReading(20, 3), LumaSampler.measure(ByteBuffer.wrap(byteArrayOf(10, 20, 30)), 2, 2, 2, 1))
    }
    @Test fun oneSampleBudgetIsRespected() {
        assertEquals(1, LumaSampler.measure(ByteBuffer.allocate(100), 10, 10, 10, 1, maxSamples = 1)!!.samples)
    }
}
