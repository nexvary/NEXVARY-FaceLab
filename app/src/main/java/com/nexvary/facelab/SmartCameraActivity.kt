package com.nexvary.facelab

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors

/** Local preview, bounded light guidance and private JPEG capture. No identity inference. */
class SmartCameraActivity : ComponentActivity() {
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var provider: ProcessCameraProvider? = null
    private var analysis: ImageAnalysis? = null
    private var capture: ImageCapture? = null
    private var camera: Camera? = null
    private lateinit var previewView: PreviewView
    private var generation = 0
    private var front = false
    private var ready by mutableStateOf(false)
    private var capturing by mutableStateOf(false)
    private var grid by mutableStateOf(true)
    private var torch by mutableStateOf(false)
    private var hasFlash by mutableStateOf(false)
    private var advice by mutableStateOf("")
    private var cameraError by mutableStateOf(false)
    private val arabic by lazy { intent.getStringExtra("language") == "ar" }
    private fun text(ar: String, en: String) = if (arabic) ar else en
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera() else {
            cameraError = true
            advice = text("إذن الكاميرا مرفوض. يمكنك تفعيله من إعدادات الهاتف.", "Camera permission denied. Enable it in system settings.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        previewView = PreviewView(this)
        advice = text("جاري فتح الكاميرا…", "Opening camera…")
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides if (arabic) LayoutDirection.Rtl else LayoutDirection.Ltr) {
            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF00B8FF), surface = Color(0xFF0A0A0A))) {
                Column(Modifier.fillMaxSize().background(Color(0xFF0A0A0A)).safeDrawingPadding()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        IconButton(onClick = { finish() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, text("رجوع", "Back"), tint = Color(0xFFC5D5E5)) }
                        IconButton(onClick = { grid = !grid }) { Icon(Icons.Default.GridOn, text("شبكة التكوين", "Composition grid"), tint = if (grid) Color(0xFF00B8FF) else Color.Gray) }
                        IconButton(onClick = { torch = !torch; camera?.cameraControl?.enableTorch(torch) }, enabled = ready && hasFlash) {
                            Icon(if (torch) Icons.Default.FlashOn else Icons.Default.FlashOff, text("ضوء مستمر", "Torch"), tint = Color(0xFFFFD166))
                        }
                        IconButton(onClick = { front = !front; startCamera() }, enabled = ready && !capturing) {
                            Icon(Icons.Default.Cameraswitch, text("تبديل الكاميرا", "Switch camera"), tint = Color(0xFF00B8FF))
                        }
                    }
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
                        if (grid) Canvas(Modifier.fillMaxSize()) {
                            for (fraction in listOf(1f / 3f, 2f / 3f)) {
                                drawLine(Color.White.copy(alpha = 0.45f), Offset(size.width * fraction, 0f), Offset(size.width * fraction, size.height), 1.dp.toPx())
                                drawLine(Color.White.copy(alpha = 0.45f), Offset(0f, size.height * fraction), Offset(size.width, size.height * fraction), 1.dp.toPx())
                            }
                        }
                    }
                    Text(advice, color = if (cameraError) Color(0xFFFF5D73) else Color(0xFFC5D5E5), modifier = Modifier.padding(12.dp))
                    Button(onClick = { takePhoto() }, enabled = ready && !capturing, modifier = Modifier.fillMaxWidth().padding(12.dp).heightIn(min = 52.dp)) {
                        Icon(Icons.Default.PhotoCamera, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (capturing) text("جاري الحفظ…", "Saving…") else text("التقاط وفتح في المحرر", "Capture and open editor"))
                    }
                }
            }
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startCamera()
        else permission.launch(Manifest.permission.CAMERA)
    }

    private fun startCamera() {
        ready = false
        cameraError = false
        val request = ++generation
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            if (isDestroyed || isFinishing || request != generation) return@addListener
            try {
                val currentProvider = future.get()
                provider = currentProvider
                val selector = if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                if (!currentProvider.hasCamera(selector)) {
                    front = !front
                    cameraError = true
                    advice = text("الكاميرا المطلوبة غير متوفرة", "Requested camera is unavailable")
                    ready = camera != null
                    return@addListener
                }
                analysis?.clearAnalyzer()
                currentProvider.unbindAll()
                val rotation = previewView.display?.rotation ?: android.view.Surface.ROTATION_0
                val preview = Preview.Builder().setTargetRotation(rotation).build().also { it.surfaceProvider = previewView.surfaceProvider }
                val shot = ImageCapture.Builder().setTargetRotation(rotation).setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                capture = shot
                val analyzer = ImageAnalysis.Builder().setTargetRotation(rotation).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                analysis = analyzer
                var lastAdviceTime = 0L
                analyzer.setAnalyzer(cameraExecutor) { frame ->
                    try {
                        val now = SystemClock.elapsedRealtime()
                        if (now - lastAdviceTime >= 400) {
                            val mean = LightingAnalyzer.meanLuma(frame)
                            val guidance = LightingAnalyzer.guidance(mean, arabic)
                            lastAdviceTime = now
                            runOnUiThread { if (!isDestroyed && !isFinishing && request == generation && !cameraError) advice = guidance }
                        }
                    } finally { frame.close() }
                }
                camera = currentProvider.bindToLifecycle(this, selector, preview, analyzer, shot)
                torch = false
                hasFlash = camera?.cameraInfo?.hasFlashUnit() == true
                ready = true
            } catch (_: Exception) {
                ready = false
                cameraError = true
                advice = text("تعذر فتح الكاميرا. أغلق أي تطبيق يستخدمها ثم حاول مجددًا.", "Camera unavailable. Close other camera apps and try again.")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val shot = capture ?: return
        if (!ready || capturing) return
        capturing = true
        cameraError = false
        val file = File(captureDirectory(), "capture_${UUID.randomUUID()}.jpg")
        shot.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                capturing = false
                if (isDestroyed || isFinishing) { file.delete(); return }
                setResult(RESULT_OK, Intent().putExtra(CAPTURE_FILE, file.name))
                finish()
            }
            override fun onError(exception: ImageCaptureException) {
                file.delete()
                capturing = false
                cameraError = true
                advice = text("فشل حفظ الصورة. تحقق من المساحة المتاحة.", "Could not save the photo. Check available storage.")
            }
        })
    }

    private fun captureDirectory(): File = File(filesDir, "camera_pending").apply {
        mkdirs()
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        listFiles()?.filter { it.isFile && it.lastModified() < cutoff }?.forEach { it.delete() }
    }

    override fun onDestroy() {
        generation++
        analysis?.clearAnalyzer()
        provider?.unbindAll()
        cameraExecutor.shutdown()
        super.onDestroy()
    }

    companion object { const val CAPTURE_FILE = "facelab.capture.filename" }
}
