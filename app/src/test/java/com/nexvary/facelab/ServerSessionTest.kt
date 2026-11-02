package com.nexvary.facelab

import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class ServerSessionTest {
    @After fun reset() { ServerSession.baseUrl = BuildConfig.FACE_API_BASE_URL; ServerSession.token = ""; ServerSession.uploadAllowed = false }
    @Test fun readsDoNotGrantUploadPermission() {
        ServerSession.baseUrl = "https://example.org"
        ServerSession.validate("GET")
        assertThrows(IllegalStateException::class.java) { ServerSession.validate("POST") }
    }
    @Test fun explicitPermissionAllowsUpload() {
        ServerSession.baseUrl = "https://example.org"
        ServerSession.uploadAllowed = true
        ServerSession.validate("POST")
    }
    @Test fun credentialsInUrlAreRejected() {
        ServerSession.baseUrl = "https://user:secret@example.org"
        assertThrows(IllegalArgumentException::class.java) { ServerSession.validate("GET") }
    }
    @Test fun invalidSchemeIsRejected() {
        ServerSession.baseUrl = "ftp://example.org"
        assertThrows(IllegalArgumentException::class.java) { ServerSession.validate("GET") }
    }
    @Test fun missingHostIsRejected() {
        ServerSession.baseUrl = ""
        assertThrows(IllegalArgumentException::class.java) { ServerSession.validate("GET") }
    }
    @Test fun cleartextPolicyMatchesBuildType() {
        ServerSession.baseUrl = "http://10.0.2.2:8000"
        if (BuildConfig.DEBUG) ServerSession.validate("GET")
        else assertThrows(IllegalArgumentException::class.java) { ServerSession.validate("GET") }
    }
}
