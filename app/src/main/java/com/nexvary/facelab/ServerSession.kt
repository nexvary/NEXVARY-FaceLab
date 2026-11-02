package com.nexvary.facelab

import java.net.URI

/** Credentials and upload permission live only for this process; never stored or compiled in. */
internal object ServerSession {
    @Volatile var baseUrl: String = BuildConfig.FACE_API_BASE_URL
    @Volatile var token: String = ""
    @Volatile var uploadAllowed: Boolean = false

    fun validate(method: String) {
        val uri = URI(baseUrl)
        require(uri.host != null && uri.userInfo == null && uri.query == null && uri.fragment == null) {
            "Configure a server URL in Settings / اضبط رابط الخادم من الإعدادات"
        }
        require(uri.scheme == "https" || (BuildConfig.DEBUG && uri.scheme == "http")) {
            "HTTPS is required for release builds"
        }
        check(method != "POST" || uploadAllowed) {
            "Allow photo uploads for this session in Settings / اسمح برفع الصور لهذه الجلسة من الإعدادات"
        }
    }
}
