package com.logikawan.finflow

import com.logikawan.finflow.i18n.*
import com.logikawan.finflow.core.i18n.text
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

/** Input suara (transkrip di perangkat) dan foto struk untuk fitur AI. */
class AiDevice(private val activity: FragmentActivity) {
    private var speechResult: ((String?, String?) -> Unit)? = null
    private var cameraResult: ((ByteArray?, String?) -> Unit)? = null
    private var galleryResult: ((List<ByteArray>?, String?) -> Unit)? = null
    private var cameraFile: File? = null

    private val speech = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { response ->
        val callback = speechResult ?: return@registerForActivityResult
        speechResult = null
        val text = response.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.trim()
        if (response.resultCode == Activity.RESULT_OK && !text.isNullOrBlank()) callback(text.take(2000), null)
        else callback(null, if (response.resultCode == Activity.RESULT_CANCELED) null else MR.strings.core_ai_voice_not_captured.text())
    }
    private val camera = activity.registerForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val callback = cameraResult ?: return@registerForActivityResult
        cameraResult = null
        val file = cameraFile
        cameraFile = null
        if (!saved || file == null) { file?.delete(); callback(null, null); return@registerForActivityResult }
        activity.lifecycleScope.launch {
            try { callback(withContext(Dispatchers.IO) { try { compressReceipt(file.readBytes()) } finally { file.delete() } }, null) }
            catch (e: Exception) { callback(null, e.message ?: MR.strings.core_ai_receipt_read_failed.text()) }
        }
    }
    private val gallery = activity.registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_PHOTOS)) { uris ->
        val callback = galleryResult ?: return@registerForActivityResult
        galleryResult = null
        if (uris.isEmpty()) { callback(null, null); return@registerForActivityResult }
        activity.lifecycleScope.launch {
            try { callback(withContext(Dispatchers.IO) { uris.take(MAX_PHOTOS).map { compressReceipt(read(it)) } }, null) }
            catch (e: Exception) { callback(null, e.message ?: MR.strings.core_ai_receipt_read_failed.text()) }
        }
    }

    fun recognizeSpeech(languageTag: String, result: (String?, String?) -> Unit) {
        if (speechResult != null) { result(null, MR.strings.core_ai_recording_in_progress.text()); return }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, MR.strings.core_ai_voice_prompt.text())
        speechResult = result
        try { speech.launch(intent) }
        catch (e: Exception) { speechResult = null; result(null, MR.strings.core_ai_speech_recognizer_unavailable.text()) }
    }

    fun captureReceipt(result: (ByteArray?, String?) -> Unit) {
        if (cameraResult != null) { result(null, MR.strings.core_ai_camera_open.text()); return }
        try {
            val directory = File(activity.cacheDir, "receipts").apply { check(isDirectory || mkdirs()) }
            val file = File(directory, "receipt-${System.currentTimeMillis()}.jpg")
            val uri: Uri = FileProvider.getUriForFile(activity, "${activity.packageName}.files", file)
            cameraFile = file; cameraResult = result
            camera.launch(uri)
        } catch (e: Exception) { cameraFile = null; cameraResult = null; result(null, MR.strings.core_ai_camera_unavailable.text()) }
    }

    fun pickReceipts(max: Int, result: (List<ByteArray>?, String?) -> Unit) {
        if (galleryResult != null) { result(null, MR.strings.core_photo_picking_in_progress.text()); return }
        galleryResult = result
        try { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        catch (e: Exception) { galleryResult = null; result(null, MR.strings.core_ai_gallery_unavailable.text()) }
    }

    private fun read(uri: Uri): ByteArray = activity.contentResolver.openInputStream(uri).use { input ->
        requireNotNull(input) { MR.strings.core_photo_unreadable.text() }
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            require(output.size() + count <= 25 * 1024 * 1024) { MR.strings.core_ai_photo_max_25mb.text() }
            output.write(buffer, 0, count)
        }
        output.toByteArray()
    }

    companion object {
        const val MAX_PHOTOS = 5

        // Struk perlu teks tajam: sisi terpanjang 2000 px, JPEG 85, maksimal 3,5 MB.
        fun compressReceipt(raw: ByteArray): ByteArray {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)
            require(bounds.outWidth in 1..20000 && bounds.outHeight in 1..20000) { MR.strings.core_ai_photo_format_unsupported.text() }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 2000) sample *= 2
            val decoded = requireNotNull(BitmapFactory.decodeByteArray(raw, 0, raw.size, BitmapFactory.Options().apply { inSampleSize = sample })) { MR.strings.core_photo_unreadable.text() }
            val orientation = runCatching { ExifInterface(ByteArrayInputStream(raw)).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
            val scale = minOf(1f, 2000f / maxOf(decoded.width, decoded.height))
            val matrix = Matrix().apply {
                postScale(scale, scale)
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                }
            }
            try {
                val oriented = if (matrix.isIdentity) decoded else Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                try {
                    var quality = 85
                    while (true) {
                        val bytes = ByteArrayOutputStream().also { check(oriented.compress(Bitmap.CompressFormat.JPEG, quality, it)) }.toByteArray()
                        if (bytes.size <= 3_500_000 || quality <= 55) return bytes.also { require(it.size <= 3_500_000) { MR.strings.core_ai_receipt_too_large.text() } }
                        quality -= 10
                    }
                } finally { if (oriented !== decoded) oriented.recycle() }
            } finally { decoded.recycle() }
        }
    }
}
