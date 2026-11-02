package com.nexvary.facelab

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import java.util.Locale

/** Offline-first camera foundation: luminance guidance only; no face or pose recognition claims. */
class SmartCameraActivity : ComponentActivity() {
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private lateinit var guidance: TextView
    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera() else guidance.text = "Camera permission denied / تم رفض إذن الكاميرا"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        guidance = TextView(this).apply { text = "Smart camera / الكاميرا الذكية"; textSize = 18f; setPadding(20, 20, 20, 20) }
        val preview = PreviewView(this).apply { id = android.view.View.generateViewId() }
        layout.addView(guidance)
        layout.addView(preview, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(layout)
        previewView = preview
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startCamera()
        else requestPermission.launch(Manifest.permission.CAMERA)
    }

    private lateinit var previewView: PreviewView
    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                analysis.setAnalyzer(cameraExecutor) { frame ->
                    try {
                        val mean = LightingAnalyzer.meanLuma(frame)
                        val arabic = Locale.getDefault().language == "ar"
                        val advice = LightingAnalyzer.guidance(mean, arabic)
                        runOnUiThread { if (!isFinishing && !isDestroyed) guidance.text = advice }
                    } finally { frame.close() }
                }
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            } catch (e: Exception) { guidance.text = "Camera unavailable: ${e.javaClass.simpleName}" }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() { cameraExecutor.shutdown(); super.onDestroy() }
}
