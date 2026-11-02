package com.nexvary.facelab

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class CaptureFilesTest {
    @Test fun validCaptureStaysInsideItsDirectory() {
        val directory = File(System.getProperty("java.io.tmpdir"), "facelab-capture-test")
        val name = "capture_12345678-1234-1234-1234-123456789abc.jpg"
        assertEquals(directory.canonicalFile, CaptureFiles.resolve(directory, name)!!.parentFile)
    }
    @Test fun pathTraversalAndUnexpectedNamesAreRejected() {
        val directory = File(System.getProperty("java.io.tmpdir"), "facelab-capture-test")
        for (name in listOf("../secret.jpg", "/tmp/secret.jpg", "photo.jpg", "capture_foo.jpg", "capture_12345678-1234-1234-1234-123456789abc.jpg/extra")) {
            assertNull(CaptureFiles.resolve(directory, name))
        }
    }
}
