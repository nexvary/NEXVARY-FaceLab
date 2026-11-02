package com.nexvary.facelab

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class FaceEditResult(
    val bitmap: Bitmap,
    val requestId: String,
    val processMs: Long?,
    val cacheState: String,
    val engineVersion: String
)

object FaceApiClient {
    private const val CONNECT_TIMEOUT = 15_000
    private const val READ_TIMEOUT = 300_000

    private fun connection(path: String, method: String): HttpURLConnection {
        ServerSession.validate(method)
        return         (URL(ServerSession.baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT
            readTimeout = if (method == "POST") READ_TIMEOUT else 10_000
            setRequestProperty("Accept", if (path == "/v1/edit") "image/jpeg" else "application/json")
            setRequestProperty("X-FaceLab-Client-Version", "39500.0.0")
            if (ServerSession.token.isNotBlank()) setRequestProperty("X-FaceLab-Token", ServerSession.token)
        }

    }

    suspend fun edit(bitmap: Bitmap, effect: String, strength: Float, reference: Bitmap? = null): Result<FaceEditResult> = withContext(Dispatchers.IO) {
        runCatching {
            val boundary = "FaceLab-${UUID.randomUUID()}"
            val conn = connection("/v1/edit", "POST").apply {
                doOutput = true
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            }
            fun encode(bmp: Bitmap): ByteArray = ByteArrayOutputStream().use { bos ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 94, bos)
                bos.toByteArray()
            }
            val imageBytes = encode(bitmap)
            val referenceBytes = reference?.let(::encode)
            conn.outputStream.buffered().use { out ->
                fun textPart(name: String, value: String) {
                    out.write("--$boundary\r\n".toByteArray())
                    out.write("Content-Disposition: form-data; name=\"$name\"\r\n\r\n".toByteArray())
                    out.write(value.toByteArray())
                    out.write("\r\n".toByteArray())
                }
                fun imagePart(name: String, filename: String, bytes: ByteArray) {
                    out.write("--$boundary\r\n".toByteArray())
                    out.write("Content-Disposition: form-data; name=\"$name\"; filename=\"$filename\"\r\n".toByteArray())
                    out.write("Content-Type: image/jpeg\r\n\r\n".toByteArray())
                    out.write(bytes)
                    out.write("\r\n".toByteArray())
                }
                textPart("effect", effect)
                textPart("strength", strength.coerceIn(0f, 1f).toString())
                imagePart("image", "portrait.jpg", imageBytes)
                referenceBytes?.let { imagePart("reference", "reference.jpg", it) }
                out.write("--$boundary--\r\n".toByteArray())
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                val body = (conn.errorStream ?: conn.inputStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
                conn.disconnect()
                error("Face API HTTP $code: $body")
            }
            val requestId = conn.getHeaderField("X-Request-ID").orEmpty()
            val processMs = conn.getHeaderField("X-Process-MS")?.toLongOrNull()
            val cacheState = conn.getHeaderField("X-FaceLab-Cache").orEmpty().ifBlank { "UNKNOWN" }
            val version = conn.getHeaderField("X-FaceLab-Version").orEmpty()
            val bytes = conn.inputStream.use { it.readBytes() }
            conn.disconnect()
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: error("Face API returned an invalid image")
            FaceEditResult(decoded, requestId, processMs, cacheState, version)
        }
    }



    suspend fun tryOn(person: Bitmap, styleId: String, category: String, strength: Float, garment: Bitmap? = null, color: String = "auto", accessory: String = "none", occasion: String = "work", outfit: String = ""): Result<FaceEditResult> = withContext(Dispatchers.IO) {
        runCatching {
            val boundary = "FaceLab-${UUID.randomUUID()}"
            val conn = connection("/v1/wardrobe/try-on", "POST").apply {
                doOutput = true
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                setRequestProperty("Accept", "image/jpeg")
            }
            fun encode(bmp: Bitmap): ByteArray = ByteArrayOutputStream().use { bos -> bmp.compress(Bitmap.CompressFormat.JPEG, 94, bos); bos.toByteArray() }
            val personBytes = encode(person)
            val garmentBytes = garment?.let(::encode)
            conn.outputStream.buffered().use { out ->
                fun textPart(name: String, value: String) {
                    out.write("--$boundary\r\n".toByteArray()); out.write("Content-Disposition: form-data; name=\"$name\"\r\n\r\n".toByteArray()); out.write(value.toByteArray()); out.write("\r\n".toByteArray())
                }
                fun imagePart(name: String, filename: String, bytes: ByteArray) {
                    out.write("--$boundary\r\n".toByteArray()); out.write("Content-Disposition: form-data; name=\"$name\"; filename=\"$filename\"\r\n".toByteArray()); out.write("Content-Type: image/jpeg\r\n\r\n".toByteArray()); out.write(bytes); out.write("\r\n".toByteArray())
                }
                textPart("style_id", styleId); textPart("category", category); textPart("strength", strength.coerceIn(0f,1f).toString()); textPart("color", color); textPart("accessory", accessory); textPart("occasion", occasion); textPart("outfit", outfit); imagePart("person", "person.jpg", personBytes); garmentBytes?.let { imagePart("garment", "garment.jpg", it) }; out.write("--$boundary--\r\n".toByteArray())
            }
            val code=conn.responseCode
            if(code !in 200..299){ val body=(conn.errorStream?:conn.inputStream)?.bufferedReader()?.use{it.readText()}.orEmpty(); conn.disconnect(); error("Wardrobe API HTTP $code: $body") }
            val requestId=conn.getHeaderField("X-Request-ID").orEmpty(); val processMs=conn.getHeaderField("X-Process-MS")?.toLongOrNull(); val cacheState=conn.getHeaderField("X-FaceLab-Cache").orEmpty().ifBlank{"UNKNOWN"}; val version=conn.getHeaderField("X-FaceLab-Version").orEmpty(); val bytes=conn.inputStream.use{it.readBytes()}; conn.disconnect(); val decoded=BitmapFactory.decodeByteArray(bytes,0,bytes.size)?:error("Wardrobe API returned an invalid image"); FaceEditResult(decoded,requestId,processMs,cacheState,version)
        }
    }

    suspend fun heritage(person: Bitmap, background: Bitmap, siteId: String, mode: String, lighting: String, strength: Float): Result<FaceEditResult> = withContext(Dispatchers.IO) {
        runCatching {
            val boundary="FaceLab-${UUID.randomUUID()}"; val conn=connection("/v1/heritage/composite","POST").apply{doOutput=true;setRequestProperty("Content-Type","multipart/form-data; boundary=$boundary");setRequestProperty("Accept","image/jpeg")}
            fun enc(b:Bitmap)=ByteArrayOutputStream().use{bos->b.compress(Bitmap.CompressFormat.JPEG,94,bos);bos.toByteArray()}
            conn.outputStream.buffered().use{out->
                fun text(n:String,v:String){out.write("--$boundary\r\nContent-Disposition: form-data; name=\"$n\"\r\n\r\n$v\r\n".toByteArray())}
                fun img(n:String,f:String,b:ByteArray){out.write("--$boundary\r\nContent-Disposition: form-data; name=\"$n\"; filename=\"$f\"\r\nContent-Type: image/jpeg\r\n\r\n".toByteArray());out.write(b);out.write("\r\n".toByteArray())}
                text("site_id",siteId);text("mode",mode);text("lighting",lighting);text("strength",strength.coerceIn(0f,1f).toString());img("person","person.jpg",enc(person));img("background","background.jpg",enc(background));out.write("--$boundary--\r\n".toByteArray())}
            val code=conn.responseCode;if(code !in 200..299){val body=(conn.errorStream?:conn.inputStream)?.bufferedReader()?.use{it.readText()}.orEmpty();conn.disconnect();error("Heritage API HTTP $code: $body")};val rid=conn.getHeaderField("X-Request-ID").orEmpty();val ms=conn.getHeaderField("X-Process-MS")?.toLongOrNull();val cache=conn.getHeaderField("X-FaceLab-Cache").orEmpty().ifBlank{"UNKNOWN"};val ver=conn.getHeaderField("X-FaceLab-Version").orEmpty();val bytes=conn.inputStream.use{it.readBytes()};conn.disconnect();val bmp=BitmapFactory.decodeByteArray(bytes,0,bytes.size)?:error("Invalid heritage image");FaceEditResult(bmp,rid,ms,cache,ver)
        }
    }

    suspend fun heritageCatalog(): Result<String> = getJson("/v1/heritage/catalog")

    suspend fun wardrobeStatus(): Result<String> = getJson("/v1/wardrobe/status")
    suspend fun wardrobeCatalog(): Result<String> = getJson("/v1/wardrobe/catalog")
    suspend fun wardrobeRecommendations(occasion: String = "work", gender: String = "all", climate: String = "mild"): Result<String> =
        getJson("/v1/wardrobe/stylist/recommendations?occasion=$occasion&gender=$gender&climate=$climate&limit=3")

    suspend fun filmStatus(): Result<String> = getJson("/v1/film/status")
    suspend fun filmDoctor(): Result<String> = getJson("/v1/film/doctor")
    suspend fun filmMotions(): Result<String> = getJson("/v1/film/motions")
    suspend fun filmDrivingMotions(): Result<String> = getJson("/v1/film/driving-motions")
    suspend fun filmMotionLibrary(): Result<String> = getJson("/v1/film/motion-library")
    suspend fun filmDirectorPresets(): Result<String> = getJson("/v1/film/director-presets")
    suspend fun filmProjectTemplates(): Result<String> = getJson("/v1/film/project-templates")
    suspend fun filmContinuity(jobId: String): Result<String> = getJson("/v1/film/jobs/$jobId/continuity")
    suspend fun filmAudioTimeline(jobId: String): Result<String> = getJson("/v1/film/jobs/$jobId/audio-timeline")
    suspend fun filmProductionReport(jobId: String): Result<String> = getJson("/v1/film/jobs/$jobId/production-report")
    suspend fun filmStoryboardValidate(jobId: String): Result<String> = getJson("/v1/film/jobs/$jobId/storyboard/validate")
    suspend fun filmRenderManifest(jobId: String): Result<String> = getJson("/v1/film/jobs/$jobId/render-manifest")
    suspend fun filmReadiness(jobId: String): Result<String> = getJson("/v1/film/jobs/$jobId/readiness")
    suspend fun filmTimeline(jobId: String): Result<String> = getJson("/v1/film/jobs/$jobId/timeline")

    suspend fun filmTimelineDuration(jobId: String, shotId: String, durationSeconds: Float): Result<String> = postForm("/v1/film/jobs/$jobId/timeline/$shotId/duration", mapOf("duration_seconds" to durationSeconds.toString()))
    suspend fun filmTimelineNudge(jobId: String, shotId: String, direction: Int): Result<String> = postForm("/v1/film/jobs/$jobId/timeline/$shotId/nudge", mapOf("direction" to direction.toString()))
    suspend fun filmTimelineUndo(jobId: String): Result<String> = postEmpty("/v1/film/jobs/$jobId/timeline/undo")
    suspend fun filmTimelineRedo(jobId: String): Result<String> = postEmpty("/v1/film/jobs/$jobId/timeline/redo")
    suspend fun filmTimelineHistory(jobId: String): Result<String> = getJson("/v1/film/jobs/$jobId/timeline/history")
    suspend fun filmTimelineEditor(jobId: String): Result<String> = getJson("/v1/film/jobs/$jobId/timeline/editor")
    suspend fun filmTimelineView(jobId: String, playhead: Float? = null, zoom: Float? = null, inPoint: Float? = null, outPoint: Float? = null, selectedShotId: String? = null): Result<String> = postForm("/v1/film/jobs/$jobId/timeline/view", buildMap {
        playhead?.let { put("playhead_seconds", it.toString()) }; zoom?.let { put("zoom", it.toString()) }; inPoint?.let { put("in_seconds", it.toString()) }; outPoint?.let { put("out_seconds", it.toString()) }; selectedShotId?.let { put("selected_shot_id", it) }
    })
    suspend fun filmTimelinePreciseTrim(jobId: String, shotId: String, head: Float, tail: Float): Result<String> = postForm("/v1/film/jobs/$jobId/timeline/$shotId/precise-trim", mapOf("head_seconds" to head.toString(), "tail_seconds" to tail.toString()))
    suspend fun filmTimelineAddMarker(jobId: String, at: Float, label: String, kind: String = "marker"): Result<String> = postForm("/v1/film/jobs/$jobId/timeline/markers", mapOf("at_seconds" to at.toString(), "label" to label, "kind" to kind))
    suspend fun filmTimelineMove(jobId: String, shotId: String, targetIndex: Int): Result<String> = postForm("/v1/film/jobs/$jobId/timeline/$shotId/move", mapOf("target_index" to targetIndex.toString()))
    suspend fun filmTimelineTransition(jobId: String, shotId: String, transition: String, duration: Float): Result<String> = postForm("/v1/film/jobs/$jobId/timeline/$shotId/transition", mapOf("transition" to transition, "duration_seconds" to duration.toString()))
    suspend fun filmTimelineAudio(jobId: String, shotId: String, gainDb: Float, fadeIn: Float, fadeOut: Float, muted: Boolean): Result<String> = postForm("/v1/film/jobs/$jobId/timeline/$shotId/audio", mapOf("gain_db" to gainDb.toString(), "fade_in" to fadeIn.toString(), "fade_out" to fadeOut.toString(), "muted" to muted.toString()))
    suspend fun filmTimelineSubtitle(jobId: String, shotId: String, text: String, offset: Float, enabled: Boolean): Result<String> = postForm("/v1/film/jobs/$jobId/timeline/$shotId/subtitle", mapOf("text" to text, "offset_seconds" to offset.toString(), "enabled" to enabled.toString()))
    suspend fun filmTimelineRenderRange(jobId: String): Result<String> = getJson("/v1/film/jobs/$jobId/timeline/render-range")
    suspend fun filmTimelineAutosave(jobId: String): Result<String> = postEmpty("/v1/film/jobs/$jobId/timeline/autosave")
    suspend fun filmTimelineRecovery(jobId: String): Result<String> = getJson("/v1/film/jobs/$jobId/timeline/recovery")
    suspend fun filmTimelineRestoreRecovery(jobId: String): Result<String> = postEmpty("/v1/film/jobs/$jobId/timeline/recovery/restore")
    suspend fun filmTimelineAdvanced(jobId: String): Result<String> = getJson("/v1/film/jobs/$jobId/timeline/advanced")
    suspend fun filmTimelineRuler(jobId: String, zoom: Float = 1f): Result<String> = getJson("/v1/film/jobs/$jobId/timeline/ruler?zoom=$zoom")
    suspend fun filmTimelineWaveform(jobId: String, shotId: String, samples: Int = 96): Result<String> = getJson("/v1/film/jobs/$jobId/timeline/$shotId/waveform?samples=$samples")
    suspend fun filmTimelineSnap(jobId: String, seconds: Float): Result<String> = postForm("/v1/film/jobs/$jobId/timeline/snap", mapOf("seconds" to seconds.toString(), "include_shots" to "true"))
    suspend fun filmTimelineSelection(jobId: String, shotIds: List<String>): Result<String> = postForm("/v1/film/jobs/$jobId/timeline/selection", mapOf("shot_ids" to shotIds.joinToString(",")))
    suspend fun filmTimelineBatch(jobId: String, shotIds: List<String>, transition: String? = null, gainDb: Float? = null, muted: Boolean? = null, motion: String? = null, camera: String? = null): Result<String> = postForm("/v1/film/jobs/$jobId/timeline/batch", buildMap { put("shot_ids", shotIds.joinToString(",")); transition?.let{put("transition",it)}; gainDb?.let{put("audio_gain_db",it.toString())}; muted?.let{put("muted",it.toString())}; motion?.let{put("motion_preset",it)}; camera?.let{put("camera_preset",it)} })
    suspend fun filmTimelineCameraKeyframe(jobId: String, shotId: String, at: Float, zoom: Float, panX: Float = 0f, panY: Float = 0f, rotation: Float = 0f): Result<String> = postForm("/v1/film/jobs/$jobId/timeline/$shotId/camera-keyframe", mapOf("at_seconds" to at.toString(), "zoom" to zoom.toString(), "pan_x" to panX.toString(), "pan_y" to panY.toString(), "rotation" to rotation.toString()))
    suspend fun filmTimelineMotionKeyframe(jobId: String, shotId: String, at: Float, intensity: Float, speed: Float = 1f): Result<String> = postForm("/v1/film/jobs/$jobId/timeline/$shotId/motion-keyframe", mapOf("at_seconds" to at.toString(), "intensity" to intensity.toString(), "speed" to speed.toString()))
    suspend fun filmTimelineTrack(jobId: String, track: String, locked: Boolean? = null, muted: Boolean? = null, solo: Boolean? = null): Result<String> = postForm("/v1/film/jobs/$jobId/timeline-controls/tracks/$track", buildMap { locked?.let{put("locked",it.toString())}; muted?.let{put("muted",it.toString())}; solo?.let{put("solo",it.toString())} })
    suspend fun filmTimelineRippleTrim(jobId: String, shotId: String, delta: Float): Result<String> = postForm("/v1/film/jobs/$jobId/timeline/$shotId/ripple-trim", mapOf("delta_seconds" to delta.toString()))
    suspend fun filmTimelineProxyPreview(jobId: String, start: Float? = null, end: Float? = null): Result<String> = postForm("/v1/film/jobs/$jobId/timeline/proxy-preview", buildMap { start?.let{put("start_seconds",it.toString())}; end?.let{put("end_seconds",it.toString())}; put("width","640"); put("height","360") })
    suspend fun filmTimelineStage33000(jobId: String): Result<String> = getJson("/v1/film/jobs/$jobId/timeline/stage33000")
    suspend fun filmTimelineScrub(jobId: String, seconds: Float, snapSeconds: Float? = null): Result<String> = postForm("/v1/film/jobs/$jobId/timeline/scrub", buildMap { put("seconds", seconds.toString()); snapSeconds?.let{put("snap_seconds",it.toString())} })
    suspend fun filmTimelinePlayback(jobId: String, command: String): Result<String> = postForm("/v1/film/jobs/$jobId/timeline/playback", mapOf("command" to command))
    suspend fun filmTimelineColorTag(jobId: String, targetType: String, targetId: String, color: String): Result<String> = postForm("/v1/film/jobs/$jobId/timeline/color-tag", mapOf("target_type" to targetType, "target_id" to targetId, "color" to color))
    suspend fun filmTimelineCurve(jobId: String, shotId: String, bucket: String = "camera", propertyName: String = "zoom", samples: Int = 64): Result<String> = getJson("/v1/film/jobs/$jobId/timeline/$shotId/curve?bucket="+java.net.URLEncoder.encode(bucket,"UTF-8")+"&property_name="+java.net.URLEncoder.encode(propertyName,"UTF-8")+"&samples=$samples")
    suspend fun filmTimelineProxyCache(jobId: String): Result<String> = getJson("/v1/film/jobs/$jobId/timeline/proxy-cache")
    suspend fun filmTimelineProxyCachePrune(jobId: String): Result<String> = postEmpty("/v1/film/jobs/$jobId/timeline/proxy-cache/prune")
    suspend fun filmTimelineProxyVideo(jobId: String): Result<ByteArray> = downloadBytes("/v1/film/jobs/$jobId/timeline/proxy-preview/video", "video/mp4")
    suspend fun filmTimelineMonitor(jobId: String): Result<String> = getJson("/v1/film/jobs/$jobId/timeline/monitor")
    suspend fun filmTimelineMonitorPlayhead(jobId: String, seconds: Float, fps: Float = 24f): Result<String> = postForm("/v1/film/jobs/$jobId/timeline/monitor/playhead", mapOf("seconds" to seconds.toString(), "fps" to fps.toString()))
    suspend fun filmTimelineFrameStep(jobId: String, direction: Int, frames: Int = 1, fps: Float = 24f): Result<String> = postForm("/v1/film/jobs/$jobId/timeline/monitor/frame-step", mapOf("direction" to direction.toString(), "frames" to frames.toString(), "fps" to fps.toString()))
    suspend fun filmTimelineSafeArea(jobId: String, preset: String): Result<String> = postForm("/v1/film/jobs/$jobId/timeline/monitor/safe-area", mapOf("preset" to preset))
    suspend fun filmTimelineOnion(jobId: String, enabled: Boolean, frames: Int = 1, opacity: Float = 0.35f): Result<String> = postForm("/v1/film/jobs/$jobId/timeline/monitor/onion", mapOf("enabled" to enabled.toString(), "frames" to frames.toString(), "opacity" to opacity.toString()))
    suspend fun filmTimelineWorkspace(jobId: String, preset: String): Result<String> = postForm("/v1/film/jobs/$jobId/timeline/workspace", mapOf("preset" to preset))
    suspend fun filmTimelineMonitorFrame(jobId: String): Result<ByteArray> = downloadBytes("/v1/film/jobs/$jobId/timeline/monitor/frame?width=960&height=540", "image/jpeg")
    suspend fun filmShotPreview(jobId: String, shotId: String): Result<ByteArray> = downloadBytes("/v1/film/jobs/$jobId/shots/$shotId/preview", "video/mp4")
    suspend fun filmRenderQueue(): Result<String> = getJson("/v1/film/render-queue")
    suspend fun filmSnapshot(jobId: String): Result<String> = postEmpty("/v1/film/jobs/$jobId/snapshot")
    suspend fun filmSnapshots(jobId: String): Result<String> = getJson("/v1/film/jobs/$jobId/snapshots")
    suspend fun filmDuplicateShot(jobId: String, shotId: String): Result<String> = postEmpty("/v1/film/jobs/$jobId/storyboard/$shotId/duplicate")
    suspend fun filmSplitShot(jobId: String, shotId: String): Result<String> = postEmpty("/v1/film/jobs/$jobId/storyboard/$shotId/split")
    suspend fun filmDeleteShot(jobId: String, shotId: String): Result<String> = postEmpty("/v1/film/jobs/$jobId/storyboard/$shotId/delete")

    suspend fun filmUploadDrivingMotion(bytes: ByteArray, filename: String, motionId: String, label: String, engineHint: String = "mimicmotion"): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val boundary = "FaceLab-${UUID.randomUUID()}"
            val conn = connection("/v1/film/driving-motions", "POST").apply {
                doOutput = true; setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary"); setRequestProperty("Accept", "application/json")
            }
            conn.outputStream.buffered().use { out ->
                fun text(name:String,value:String){ out.write("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n$value\r\n".toByteArray()) }
                text("motion_id", motionId); text("label", label); text("engine_hint", engineHint)
                out.write("--$boundary\r\nContent-Disposition: form-data; name=\"clip\"; filename=\"$filename\"\r\nContent-Type: video/mp4\r\n\r\n".toByteArray()); out.write(bytes); out.write("\r\n--$boundary--\r\n".toByteArray())
            }
            val code=conn.responseCode; val body=(if(code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use{it.readText()}.orEmpty(); conn.disconnect(); if(code !in 200..299) error("Driving motion upload HTTP $code: $body"); body
        }
    }

    suspend fun filmDrivingMotionPreview(motionId: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            val path="/v1/film/driving-motions/"+java.net.URLEncoder.encode(motionId,"UTF-8")+"/preview"
            val conn=connection(path,"GET").apply{setRequestProperty("Accept","video/*");readTimeout=120_000}
            val code=conn.responseCode; if(code !in 200..299){val body=(conn.errorStream?:conn.inputStream)?.bufferedReader()?.use{it.readText()}.orEmpty();conn.disconnect();error("Motion preview HTTP $code: $body")}; val data=conn.inputStream.use{it.readBytes()};conn.disconnect();data
        }
    }

    suspend fun filmMakeMotionTemplate(source: Bitmap, motionId: String, deleteOriginal: Boolean): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val boundary="FaceLab-${UUID.randomUUID()}"; val conn=connection("/v1/film/driving-motions/"+java.net.URLEncoder.encode(motionId,"UTF-8")+"/template","POST").apply{doOutput=true;setRequestProperty("Content-Type","multipart/form-data; boundary=$boundary");setRequestProperty("Accept","application/json")}
            val imageBytes=ByteArrayOutputStream().use{bos->source.compress(Bitmap.CompressFormat.JPEG,94,bos);bos.toByteArray()}
            conn.outputStream.buffered().use{out->
                out.write("--$boundary\r\nContent-Disposition: form-data; name=\"delete_original\"\r\n\r\n${deleteOriginal}\r\n".toByteArray())
                out.write("--$boundary\r\nContent-Disposition: form-data; name=\"source\"; filename=\"source.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n".toByteArray());out.write(imageBytes);out.write("\r\n--$boundary--\r\n".toByteArray())
            }
            val code=conn.responseCode;val body=(if(code in 200..299)conn.inputStream else conn.errorStream)?.bufferedReader()?.use{it.readText()}.orEmpty();conn.disconnect();if(code !in 200..299)error("Motion template HTTP $code: $body");body
        }
    }
    suspend fun filmPresets(): Result<String> = getJson("/v1/film/presets")
    suspend fun filmPlan(story: String, durationSeconds: Int, preset: String, aspectRatio: String = "16:9", motionPreset: String = ""): Result<String> =
        getJson("/v1/film/plan?story=" + java.net.URLEncoder.encode(story, "UTF-8") + "&duration_seconds=$durationSeconds&preset=$preset&aspect_ratio=" + java.net.URLEncoder.encode(aspectRatio, "UTF-8") + "&motion_preset=" + java.net.URLEncoder.encode(motionPreset, "UTF-8"))

    suspend fun filmRender(source: Bitmap, story: String, durationSeconds: Int, preset: String, renderMode: String = "auto", aspectRatio: String = "16:9", exportPreset: String = "720p", motionPreset: String = ""): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val boundary = "FaceLab-${UUID.randomUUID()}"
            val conn = connection("/v1/film/render", "POST").apply {
                doOutput = true
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                setRequestProperty("Accept", "application/json")
            }
            val imageBytes = ByteArrayOutputStream().use { bos -> source.compress(Bitmap.CompressFormat.JPEG, 94, bos); bos.toByteArray() }
            conn.outputStream.buffered().use { out ->
                fun text(name: String, value: String) {
                    out.write("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n$value\r\n".toByteArray())
                }
                fun image(name: String, filename: String, bytes: ByteArray) {
                    out.write("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"; filename=\"$filename\"\r\nContent-Type: image/jpeg\r\n\r\n".toByteArray())
                    out.write(bytes); out.write("\r\n".toByteArray())
                }
                text("story", story)
                text("duration_seconds", durationSeconds.coerceIn(30, 300).toString())
                text("preset", preset)
                text("render_mode", renderMode)
                text("aspect_ratio", aspectRatio)
                text("export_preset", exportPreset)
                text("motion_preset", motionPreset)
                image("source", "final-portrait.jpg", imageBytes)
                out.write("--$boundary--\r\n".toByteArray())
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            conn.disconnect()
            if (code !in 200..299) error("Film render HTTP $code: $body")
            body
        }
    }

    suspend fun filmJob(jobId: String): Result<String> = getJson("/v1/film/jobs/$jobId")
    suspend fun filmJobs(): Result<String> = getJson("/v1/film/jobs?limit=20")
    suspend fun filmJobPlan(jobId: String): Result<String> = getJson("/v1/film/jobs/$jobId/plan")
    suspend fun filmCancel(jobId: String): Result<String> = postEmpty("/v1/film/jobs/$jobId/cancel")
    suspend fun filmRetry(jobId: String): Result<String> = postEmpty("/v1/film/jobs/$jobId/retry")
    suspend fun filmLog(jobId: String): Result<String> = getJson("/v1/film/jobs/$jobId/log")


    fun filmVideoUrl(jobId: String): String = ServerSession.baseUrl.trimEnd('/') + "/v1/film/jobs/$jobId/video"

    suspend fun filmVideo(jobId: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = connection("/v1/film/jobs/$jobId/video", "GET").apply { readTimeout = READ_TIMEOUT; setRequestProperty("Accept", "video/mp4") }
            val code = conn.responseCode
            if (code !in 200..299) {
                val body = (conn.errorStream ?: conn.inputStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
                conn.disconnect(); error("Film video HTTP $code: $body")
            }
            val bytes = conn.inputStream.use { it.readBytes() }
            conn.disconnect()
            bytes
        }
    }

    suspend fun health(): Result<String> = getJson("/health")
    suspend fun capabilities(): Result<String> = getJson("/v1/capabilities")
    suspend fun runtime(): Result<String> = getJson("/v1/runtime")
    suspend fun models(): Result<String> = getJson("/v1/models")
    suspend fun storage(): Result<String> = getJson("/v1/storage")
    suspend fun system(): Result<String> = getJson("/v1/system")


    private suspend fun postForm(path: String, fields: Map<String,String>): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val boundary = "FaceLab-${UUID.randomUUID()}"
            val conn = connection(path, "POST").apply { doOutput = true; setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary"); setRequestProperty("Accept","application/json") }
            conn.outputStream.buffered().use { out ->
                fields.forEach { (name,value) -> out.write("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n$value\r\n".toByteArray()) }
                out.write("--$boundary--\r\n".toByteArray())
            }
            val code=conn.responseCode; val body=(if(code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use{it.readText()}.orEmpty(); conn.disconnect(); if(code !in 200..299) error("HTTP $code: $body"); body
        }
    }

    private suspend fun downloadBytes(path: String, accept: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            val conn=connection(path,"GET").apply{setRequestProperty("Accept",accept);readTimeout=120_000}
            val code=conn.responseCode
            if(code !in 200..299){ val body=(conn.errorStream?:conn.inputStream)?.bufferedReader()?.use{it.readText()}.orEmpty(); conn.disconnect(); error("HTTP $code: $body") }
            val data=conn.inputStream.use{it.readBytes()}; conn.disconnect(); data
        }
    }

    private suspend fun postEmpty(path: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = connection(path, "POST").apply { doOutput = true; setRequestProperty("Content-Length", "0") }
            conn.outputStream.use { }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            conn.disconnect()
            if (code !in 200..299) error("HTTP $code: $body")
            body
        }
    }

    private suspend fun getJson(path: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = connection(path, "GET")
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            conn.disconnect()
            if (code !in 200..299) error("HTTP $code: $body")
            body
        }
    }
}
