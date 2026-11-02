package com.nexvary.facelab

import android.content.ContentValues
import android.graphics.*
import android.media.FaceDetector
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

internal fun galleryDir(activity: ComponentActivity): File = File(activity.filesDir, "facelab_gallery").apply { mkdirs() }

internal fun persistGalleryBitmap(activity: ComponentActivity, bitmap: Bitmap) {
    val dir = galleryDir(activity)
    val file = File(dir, "result_${System.currentTimeMillis()}.jpg")
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 94, it) }
    dir.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() }?.drop(30)?.forEach { it.delete() }
}

internal fun loadPersistentGallery(activity: ComponentActivity): List<Bitmap> =
    galleryDir(activity).listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() }?.take(30)?.mapNotNull { loadBitmap(activity, Uri.fromFile(it)) } ?: emptyList()

internal fun clearPersistentGallery(activity: ComponentActivity) { galleryDir(activity).listFiles()?.forEach { it.delete() } }

internal fun loadBitmap(activity: ComponentActivity, uri: Uri): Bitmap? {
    return try {
        val source = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val src = android.graphics.ImageDecoder.createSource(activity.contentResolver, uri)
            android.graphics.ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
                decoder.isMutableRequired = true
                decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                val largest = max(info.size.width, info.size.height)
                if (largest > 1600) {
                    val scale = 1600.0 / largest
                    decoder.setTargetSize(max(1, (info.size.width * scale).toInt()), max(1, (info.size.height * scale).toInt()))
                }
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            activity.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            val options = BitmapFactory.Options().apply {
                inMutable = true
                inSampleSize = 1
                while (max(bounds.outWidth, bounds.outHeight) / (inSampleSize * 2) >= 1600) inSampleSize *= 2
            }
            activity.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
        }
        downscale(source, 1600)
    } catch (_: Exception) {
        null
    }
}

internal fun downscale(bitmap: Bitmap, maxSide: Int): Bitmap {
    val largest = max(bitmap.width, bitmap.height)
    if (largest <= maxSide) return bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val scale = maxSide.toFloat() / largest.toFloat()
    return Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
}

internal fun renderBitmap(source: Bitmap, effect: LocalEffect, tool: SmartTool, intensity: Float): Bitmap {
    var out = source.copy(Bitmap.Config.ARGB_8888, true)
    out = applyColorEffect(out, effect)
    out = when (tool) {
        SmartTool.None -> out
        SmartTool.Younger -> youngerEffect(out, intensity)
        SmartTool.Older -> olderEffect(out, intensity)
        SmartTool.Beard -> beardEffect(out, intensity)
        SmartTool.Hair -> hairEffect(out, intensity)
        SmartTool.Smooth -> smoothEffect(out, intensity)
        else -> out // Stage 120 advanced tools render on the self-hosted AI server.
    }
    return out
}

internal fun applyColorEffect(bitmap: Bitmap, effect: LocalEffect): Bitmap {
    if (effect == LocalEffect.Original) return bitmap
    val matrix = when (effect) {
        LocalEffect.Original -> ColorMatrix()
        LocalEffect.Clean -> ColorMatrix().apply { setSaturation(1.18f) }
        LocalEffect.Mono -> ColorMatrix().apply { setSaturation(0f) }
        LocalEffect.Warm -> ColorMatrix(floatArrayOf(
            1.10f, 0f, 0f, 0f, 8f,
            0f, 1.03f, 0f, 0f, 2f,
            0f, 0f, 0.92f, 0f, -3f,
            0f, 0f, 0f, 1f, 0f
        ))
        LocalEffect.Cool -> ColorMatrix(floatArrayOf(
            0.95f, 0f, 0f, 0f, -2f,
            0f, 1.02f, 0f, 0f, 2f,
            0f, 0f, 1.12f, 0f, 8f,
            0f, 0f, 0f, 1f, 0f
        ))
    }
    val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    Canvas(result).drawBitmap(bitmap, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = ColorMatrixColorFilter(matrix) })
    return result
}

internal data class FaceBox(val cx: Float, val cy: Float, val eyeDistance: Float)

internal fun detectFaceBox(bitmap: Bitmap): FaceBox? {
    return try {
        val evenWidth = if (bitmap.width % 2 == 0) bitmap.width else bitmap.width - 1
        if (evenWidth < 2 || bitmap.height < 2) return null
        val prepared = Bitmap.createBitmap(evenWidth, bitmap.height, Bitmap.Config.RGB_565)
        Canvas(prepared).drawBitmap(bitmap, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))
        val faces = arrayOfNulls<FaceDetector.Face>(3)
        val detector = FaceDetector(prepared.width, prepared.height, faces.size)
        val count = detector.findFaces(prepared, faces)
        if (count <= 0) return null
        val face = faces.filterNotNull().maxByOrNull { it.eyesDistance() } ?: return null
        val point = PointF()
        face.getMidPoint(point)
        FaceBox(point.x, point.y, face.eyesDistance())
    } catch (_: Throwable) {
        null
    }
}

internal fun youngerEffect(bitmap: Bitmap, intensity: Float): Bitmap {
    val smooth = smoothEffect(bitmap, 0.35f + intensity * 0.45f)
    val matrix = ColorMatrix().apply {
        setSaturation(1f + 0.12f * intensity)
        postConcat(ColorMatrix(floatArrayOf(
            1.02f, 0f, 0f, 0f, 6f * intensity,
            0f, 1.02f, 0f, 0f, 6f * intensity,
            0f, 0f, 1.02f, 0f, 6f * intensity,
            0f, 0f, 0f, 1f, 0f
        )))
    }
    val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    Canvas(result).drawBitmap(smooth, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = ColorMatrixColorFilter(matrix) })
    return result
}

internal fun olderEffect(bitmap: Bitmap, intensity: Float): Bitmap {
    val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)
    val face = detectFaceBox(bitmap) ?: return applyColorEffect(bitmap, LocalEffect.Mono)
    val d = face.eyeDistance
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb((70 + 85 * intensity).toInt(), 70, 55, 50)
        style = Paint.Style.STROKE
        strokeWidth = max(1.2f, d * 0.014f)
    }
    val yEye = face.cy
    repeat(4) { i ->
        val offset = d * (0.40f + i * 0.11f)
        canvas.drawArc(face.cx - d * 0.72f, yEye + offset, face.cx - d * 0.05f, yEye + offset + d * 0.18f, 195f, 120f, false, paint)
        canvas.drawArc(face.cx + d * 0.05f, yEye + offset, face.cx + d * 0.72f, yEye + offset + d * 0.18f, 225f, 120f, false, paint)
    }
    repeat(3) { i ->
        val y = yEye - d * (0.65f + i * 0.12f)
        canvas.drawLine(face.cx - d * 0.55f, y, face.cx + d * 0.55f, y + d * 0.03f, paint)
    }
    return result
}

internal fun beardEffect(bitmap: Bitmap, intensity: Float): Bitmap {
    val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)
    val face = detectFaceBox(bitmap) ?: return result
    val d = face.eyeDistance
    val path = Path().apply {
        moveTo(face.cx - d * 0.78f, face.cy + d * 0.52f)
        quadTo(face.cx - d * 0.82f, face.cy + d * 1.18f, face.cx, face.cy + d * 1.56f)
        quadTo(face.cx + d * 0.82f, face.cy + d * 1.18f, face.cx + d * 0.78f, face.cy + d * 0.52f)
        quadTo(face.cx, face.cy + d * 0.80f, face.cx - d * 0.78f, face.cy + d * 0.52f)
        close()
    }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb((100 + 100 * intensity).toInt(), 35, 26, 22)
        style = Paint.Style.FILL
    }
    canvas.drawPath(path, paint)
    paint.color = android.graphics.Color.argb((70 + 90 * intensity).toInt(), 18, 14, 12)
    paint.strokeWidth = max(1f, d * 0.018f)
    repeat(45) { i ->
        val t = i / 44f
        val x = face.cx - d * 0.66f + t * d * 1.32f
        val wave = kotlin.math.sin(t * Math.PI).toFloat()
        val y = face.cy + d * (0.66f + 0.58f * wave)
        canvas.drawLine(x, y, x + d * 0.03f, y + d * 0.19f, paint)
    }
    return result
}

internal fun hairEffect(bitmap: Bitmap, intensity: Float): Bitmap {
    val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)
    val face = detectFaceBox(bitmap) ?: return result
    val d = face.eyeDistance
    val rect = RectF(face.cx - d * 1.05f, face.cy - d * 1.55f, face.cx + d * 1.05f, face.cy - d * 0.20f)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb((120 + 110 * intensity).toInt(), 30, 24, 22)
        style = Paint.Style.FILL
    }
    canvas.drawOval(rect, paint)
    paint.color = android.graphics.Color.argb((75 + 80 * intensity).toInt(), 70, 52, 40)
    paint.strokeWidth = max(1.5f, d * 0.025f)
    repeat(26) { i ->
        val x = rect.left + rect.width() * (i / 25f)
        canvas.drawLine(x, rect.centerY(), face.cx + (x - face.cx) * 0.55f, face.cy - d * 0.28f, paint)
    }
    return result
}

internal fun smoothEffect(bitmap: Bitmap, intensity: Float): Bitmap {
    val scale = (0.82f - 0.35f * intensity).coerceIn(0.42f, 0.82f)
    val w = max(2, (bitmap.width * scale).toInt())
    val h = max(2, (bitmap.height * scale).toInt())
    val small = Bitmap.createScaledBitmap(bitmap, w, h, true)
    val smooth = Bitmap.createScaledBitmap(small, bitmap.width, bitmap.height, true)
    val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)
    val alpha = (55 + 120 * intensity).toInt().coerceIn(0, 200)
    canvas.drawBitmap(smooth, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.alpha = alpha })
    return result
}

internal fun saveBitmap(activity: ComponentActivity, bitmap: Bitmap): Uri? {
    return try {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val name = "FaceLab_$stamp.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FaceLab")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = activity.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        activity.contentResolver.openOutputStream(uri)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 94, stream)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            activity.contentResolver.update(uri, values, null, null)
        }
        uri
    } catch (_: Exception) {
        null
    }
}
