package com.clonical.kasiva

import com.clonical.kasiva.i18n.*
import com.clonical.kasiva.core.i18n.text
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.io.File

class ProfilePhotos(private val activity: FragmentActivity) {
    private var pending: ((ByteArray?, String?) -> Unit)? = null
    private var pendingCapture: ((ByteArray?, String?) -> Unit)? = null
    private var captureFile: File? = null
    private val storage get() = FirebaseStorage.getInstance("gs://finflow-logikawan.firebasestorage.app")

    private fun deliver(result: (ByteArray?, String?) -> Unit, read: suspend () -> ByteArray) {
        activity.lifecycleScope.launch {
            try { result(withContext(Dispatchers.IO) { compress(read()) }, null) }
            catch (e: Exception) { result(null, e.message ?: MR.strings.core_photo_read_failed.text()) }
        }
    }

    private val picker = activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val callback = pending ?: return@registerForActivityResult
        pending = null
        if (uri == null) { callback(null, null); return@registerForActivityResult }
        deliver(callback) { rawFromUri(uri) }
    }

    // Ollo: foto profil bisa dari kamera atau galeri. File kamera ditulis ke cache lalu dibaca ulang
    // supaya orientasi EXIF dan kompresi memakai jalur yang sama dengan galeri.
    private val takePicture = activity.registerForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val callback = pendingCapture ?: return@registerForActivityResult
        pendingCapture = null
        val file = captureFile
        captureFile = null
        if (!saved || file == null) { file?.delete(); callback(null, null); return@registerForActivityResult }
        deliver(callback) {
            try { withContext(Dispatchers.IO) { file.readBytes() } } finally { file.delete() }
        }
    }

    private suspend fun rawFromUri(uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        activity.contentResolver.openInputStream(uri).use { input ->
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
    }

    /** JPEG persegi maksimal 1024 px dan 2 MB; memperbaiki rotasi EXIF. */
    private fun compress(raw: ByteArray): ByteArray {
        require(raw.size <= 25 * 1024 * 1024) { MR.strings.core_ai_photo_max_25mb.text() }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)
        require(bounds.outWidth in 1..20000 && bounds.outHeight in 1..20000) { MR.strings.core_photo_format_size_unsupported.text() }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1024) sample *= 2
        val bitmap = requireNotNull(BitmapFactory.decodeByteArray(raw, 0, raw.size,
            BitmapFactory.Options().apply { inSampleSize = sample })) { MR.strings.core_photo_unreadable.text() }
        val orientation = runCatching { ExifInterface(ByteArrayInputStream(raw)).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(270f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(270f)
            }
        }
        val bytes = try {
            val oriented = if (matrix.isIdentity) bitmap else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            try { ByteArrayOutputStream().also { check(oriented.compress(Bitmap.CompressFormat.JPEG, 85, it)) }.toByteArray() }
            finally { if (oriented !== bitmap) oriented.recycle() }
        } finally { bitmap.recycle() }
        require(bytes.size <= 2 * 1024 * 1024) { MR.strings.core_photo_compressed_too_large.text() }
        return bytes
    }

    fun pick(result: (ByteArray?, String?) -> Unit) {
        if (pending != null || pendingCapture != null) { result(null, MR.strings.core_photo_picking_in_progress.text()); return }
        pending = result
        try { picker.launch("image/*") }
        catch (e: Exception) { pending = null; result(null, MR.strings.core_photo_picker_unavailable.text()) }
    }

    fun capture(result: (ByteArray?, String?) -> Unit) {
        if (pending != null || pendingCapture != null) { result(null, MR.strings.core_photo_picking_in_progress.text()); return }
        val dir = File(activity.cacheDir, "exports").apply { if (!isDirectory) mkdirs() }
        val file = File(dir, "profile-capture.jpg")
        val uri = try { FileProvider.getUriForFile(activity, "${activity.packageName}.files", file) }
            catch (e: Exception) { result(null, MR.strings.core_native_camera_unavailable.text()); return }
        pendingCapture = result
        captureFile = file
        try { takePicture.launch(uri) }
        catch (e: Exception) { pendingCapture = null; captureFile = null; result(null, MR.strings.core_native_camera_unavailable.text()) }
    }

    fun upload(uid: String, bytes: ByteArray, result: (String?, String?) -> Unit) {
        if(uid=="local") {
            activity.lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        require(bytes.isNotEmpty() && bytes.size<=2*1024*1024)
                        val file=android.util.AtomicFile(java.io.File(activity.noBackupFilesDir,"finflow-local/avatar.jpg"))
                        check(file.baseFile.parentFile!!.isDirectory || file.baseFile.parentFile!!.mkdirs())
                        val output=file.startWrite()
                        try{output.write(bytes);file.finishWrite(output)}catch(e:Exception){file.failWrite(output);throw e}
                    }
                    result("local/profile/avatar.jpg",null)
                }catch(e:Exception){result(null,MR.strings.core_photo_local_save_failed.text())}
            }
            return
        }
        if (uid != FirebaseAuth.getInstance().currentUser?.uid || bytes.isEmpty() || bytes.size > 2 * 1024 * 1024) {
            result(null, MR.strings.core_photo_invalid_account_size.text()); return
        }
        val path = "users/$uid/profile/avatar.jpg"
        // Foto disimpan di perangkat dulu sehingga selalu berhasil (juga tanpa internet); pengunggahan ke
        // cloud berjalan di latar belakang dan diulang otomatis sampai berhasil.
        activity.lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val cache = cacheFile(uid)
                    check(cache.parentFile!!.isDirectory || cache.parentFile!!.mkdirs())
                    val file = android.util.AtomicFile(cache)
                    val output = file.startWrite()
                    try { output.write(bytes); file.finishWrite(output) } catch (e: Exception) { file.failWrite(output); throw e }
                    pendingMarker(uid).writeText("1")
                }
            } catch (e: Exception) { result(null, MR.strings.core_photo_local_save_failed.text()); return@launch }
            result(path, null)
            syncPending(uid)
        }
    }

    private fun safeUid(uid: String) = uid.takeIf { it.matches(Regex("[A-Za-z0-9_-]{1,128}")) }
    private fun cacheFile(uid: String) = File(activity.noBackupFilesDir, "finflow-cloud/avatar-${safeUid(uid)}.jpg")
    private fun pendingMarker(uid: String) = File(activity.noBackupFilesDir, "finflow-cloud/avatar-${safeUid(uid)}.pending")
    private var uploading = false

    private fun online(): Boolean = runCatching {
        val manager = activity.getSystemService(android.net.ConnectivityManager::class.java)
        manager.activeNetwork?.let { manager.getNetworkCapabilities(it)?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) } == true
    }.getOrDefault(true)

    /** Unggah foto yang tertunda (disimpan saat offline). Aman dipanggil berulang. */
    fun syncPending(uid: String) {
        if (safeUid(uid) == null || uploading || uid != FirebaseAuth.getInstance().currentUser?.uid) return
        if (!pendingMarker(uid).exists() || !online()) return
        uploading = true
        activity.lifecycleScope.launch {
            val bytes = withContext(Dispatchers.IO) { runCatching { cacheFile(uid).readBytes() }.getOrNull() }
            if (bytes == null || bytes.isEmpty() || bytes.size > 2 * 1024 * 1024) { pendingMarker(uid).delete(); uploading = false; return@launch }
            storage.maxUploadRetryTimeMillis = 60_000
            storage.reference.child("users/$uid/profile/avatar.jpg")
                .putBytes(bytes, StorageMetadata.Builder().setContentType("image/jpeg").build())
                .addOnSuccessListener { pendingMarker(uid).delete(); uploading = false }
                .addOnFailureListener { error -> android.util.Log.w("KasivaProfilePhoto", "upload avatar tertunda", error); uploading = false }
        }
    }

    fun load(path: String, result: (ByteArray?, String?) -> Unit) {
        if(path=="local/profile/avatar.jpg") {
            activity.lifecycleScope.launch {
                try {
                    val bytes=withContext(Dispatchers.IO){val file=java.io.File(activity.noBackupFilesDir,"finflow-local/avatar.jpg");require(file.length() in 1..2*1024*1024);file.readBytes()}
                    result(bytes,null)
                }catch(e:Exception){result(null,MR.strings.core_photo_local_load_failed.text())}
            }
            return
        }
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null || path != "users/$uid/profile/avatar.jpg") { result(null, MR.strings.core_photo_access_denied.text()); return }
        // Salinan lokal dipakai bila ada unggahan tertunda, saat offline, atau jika cloud gagal dimuat.
        activity.lifecycleScope.launch {
            val cached = withContext(Dispatchers.IO) { runCatching { cacheFile(uid).takeIf { it.length() in 1..2L * 1024 * 1024 }?.readBytes() }.getOrNull() }
            if (cached != null && (pendingMarker(uid).exists() || !online())) { result(cached, null); return@launch }
            if (cached != null) result(cached, null)
            if (!online()) { if (cached == null) result(null, MR.strings.core_photo_load_failed.text()); return@launch }
            storage.reference.child(path).getBytes(2L * 1024 * 1024)
                .addOnSuccessListener { bytes ->
                    activity.lifecycleScope.launch(Dispatchers.IO) {
                        runCatching { cacheFile(uid).apply { parentFile?.mkdirs() }.writeBytes(bytes) }
                    }
                    result(bytes, null)
                }
                .addOnFailureListener { if (cached == null) result(null, MR.strings.core_photo_load_failed.text()) }
        }
    }
}
