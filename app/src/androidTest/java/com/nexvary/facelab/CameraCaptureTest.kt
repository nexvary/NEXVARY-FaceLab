package com.nexvary.facelab

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class CameraCaptureTest {
    @get:Rule val cameraPermission: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun intent() = Intent(context, SmartCameraActivity::class.java).putExtra("language", "en")
    private fun waitForCaptureButton(): androidx.test.uiautomator.UiObject2 {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        return requireNotNull(device.wait(Until.findObject(By.text("Capture and open editor").enabled(true)), 45_000)) { "Camera did not become ready" }
    }

    @Test(timeout = 120_000) fun captureReturnsReadablePrivateJpeg() {
        ActivityScenario.launchActivityForResult<SmartCameraActivity>(intent()).use { scenario ->
            waitForCaptureButton().click()
            val deadline = SystemClock.elapsedRealtime() + 45_000
            while (scenario.state != Lifecycle.State.DESTROYED && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(100)
            assertEquals(Lifecycle.State.DESTROYED, scenario.state)
            val result = scenario.result
            assertEquals(Activity.RESULT_OK, result.resultCode)
            val name = requireNotNull(result.resultData?.getStringExtra(SmartCameraActivity.CAPTURE_FILE))
            val file = requireNotNull(CaptureFiles.resolve(File(context.filesDir, "camera_pending"), name))
            try {
                assertTrue(file.isFile)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                assertTrue(bounds.outWidth > 0 && bounds.outHeight > 0)
                assertEquals("image/jpeg", bounds.outMimeType)
                assertFalse(ServerSession.uploadAllowed)
            } finally { file.delete() }
        }
    }

    @Test(timeout = 120_000) fun recreationAndBackgroundResumeKeepCameraUsable() {
        ActivityScenario.launch<SmartCameraActivity>(intent()).use { scenario ->
            waitForCaptureButton()
            scenario.recreate()
            waitForCaptureButton()
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            waitForCaptureButton()
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        }
    }
}
