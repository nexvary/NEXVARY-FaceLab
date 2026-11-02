package com.nexvary.facelab

import java.io.File

internal object CaptureFiles {
    private val namePattern = Regex("capture_[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.jpg")
    fun resolve(directory: File, name: String): File? {
        if (!namePattern.matches(name)) return null
        val candidate = File(directory, name).canonicalFile
        return candidate.takeIf { it.parentFile == directory.canonicalFile }
    }
}
