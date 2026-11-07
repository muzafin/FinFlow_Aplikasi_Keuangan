package com.logikawan.finflow

import com.logikawan.finflow.i18n.*
import com.logikawan.finflow.core.i18n.text
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Pemilih gambar umum (thumbnail JPEG persegi) dan penyimpanan ikon dompet kustom.
 * Cloud: Storage `users/{uid}/wallet-icons/{accountId}.jpg` + cache di cacheDir. Lokal: noBackupFilesDir/finflow-local/wallet-icons.
 * Harus dibuat saat Activity diinisialisasi (registerForActivityResult), seperti [ProfilePhotos].
 */
class WalletIcons(private val activity: FragmentActivity) {
    private var pending: Pair<Int, (ByteArray?, String?) -> Unit>? = null
    private val storage get() = FirebaseStorage.getInstance("gs://finflow-logikawan.firebasestorage.app")
    private val picker = activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val (maxBytes, callback) = pending ?: return@registerForActivityResult
        pending = null
        if (uri == null) { callback(null, null); return@registerForActivityResult }
        activity.lifecycleScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    val raw = activity.contentResolver.openInputStream(uri).use { input ->
                        requireNotNull(input) { MR.strings.core_photo_unreadable.text() }
                        val output = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            require(output.size() + count <= MAX_SOURCE_BYTES) { MR.strings.core_photo_max_5mb.text() }
                            output.write(buffer, 0, count)
                        }
                        output.toByteArray()
                    }
                    thumbnail(raw, maxBytes)
                }
                callback(bytes, null)
            } catch (e: Exception) { callback(null, e.message ?: MR.strings.core_photo_read_failed.text()) }
        }
    }

    fun pick(maxBytes: Int, result: (ByteArray?, String?) -> Unit) {
        if (maxBytes < 4 * 1024) { result(null, MR.strings.r3_icon_too_large.text()); return }
        if (pending != null) { result(null, MR.strings.core_photo_picking_in_progress.text()); return }
        pending = maxBytes to result
        try { picker.launch("image/*") }
        catch (e: Exception) { pending = null; result(null, MR.strings.core_photo_picker_unavailable.text()) }
    }

    fun save(ownerId: String, accountId: String, bytes: ByteArray, result: (String?) -> Unit) {
        if (!validIds(ownerId, accountId) || bytes.isEmpty() || bytes.size > MAX_ICON_BYTES) { result(MR.strings.r3_icon_invalid_id.text()); return }
        if (ownerId == "local") {
            activity.lifecycleScope.launch {
                val failure = withContext(Dispatchers.IO) { runCatching { write(cacheFile(ownerId, accountId), bytes) }.exceptionOrNull() }
                result(if (failure == null) null else MR.strings.r3_icon_save_failed.text())
            }
            return
        }
        if (ownerId != FirebaseAuth.getInstance().currentUser?.uid) { result(MR.strings.core_photo_invalid_account_size.text()); return }
        storage.reference.child("users/$ownerId/wallet-icons/$accountId.jpg")
            .putBytes(bytes, StorageMetadata.Builder().setContentType("image/jpeg").build())
            .addOnSuccessListener {
                activity.lifecycleScope.launch {
                    // Cache hanya mempercepat tampilan; kegagalan menulis cache tidak membatalkan unggahan.
                    withContext(Dispatchers.IO) { runCatching { write(cacheFile(ownerId, accountId), bytes) } }
                    result(null)
                }
            }
            .addOnFailureListener { result(MR.strings.r3_icon_upload_failed.text()) }
    }

    fun load(ownerId: String, accountId: String, result: (ByteArray?, String?) -> Unit) {
        if (!validIds(ownerId, accountId)) { result(null, MR.strings.r3_icon_invalid_id.text()); return }
        activity.lifecycleScope.launch {
            val file = cacheFile(ownerId, accountId)
            val cached = withContext(Dispatchers.IO) { runCatching { file.takeIf { it.length() in 1..MAX_ICON_BYTES.toLong() }?.readBytes() }.getOrNull() }
            if (cached != null) result(cached, null)
            if (ownerId == "local") { if (cached == null) result(null, MR.strings.core_photo_local_load_failed.text()); return@launch }
            if (ownerId != FirebaseAuth.getInstance().currentUser?.uid) { if (cached == null) result(null, MR.strings.core_photo_access_denied.text()); return@launch }
            // Versi cloud diperiksa sekali per sesi agar ikon yang diganti di perangkat lain ikut terbarui.
            storage.reference.child("users/$ownerId/wallet-icons/$accountId.jpg").getBytes(MAX_ICON_BYTES.toLong())
                .addOnSuccessListener { remote ->
                    if (cached == null || !remote.contentEquals(cached)) {
                        activity.lifecycleScope.launch {
                            withContext(Dispatchers.IO) { runCatching { write(file, remote) } }
                            result(remote, null)
                        }
                    }
                }
                .addOnFailureListener { if (cached == null) result(null, MR.strings.core_photo_load_failed.text()) }
        }
    }

    /** Menghapus semua ikon mode lokal (dipakai saat reset data lokal). */
    fun clearLocal(): Boolean {
        val directory = File(activity.noBackupFilesDir, "finflow-local/wallet-icons")
        return !directory.exists() || directory.deleteRecursively()
    }

    private fun validIds(ownerId: String, accountId: String) = ACCOUNT_ID.matches(accountId) && (ownerId == "local" || OWNER_ID.matches(ownerId))

    private fun cacheFile(ownerId: String, accountId: String): File =
        if (ownerId == "local") File(activity.noBackupFilesDir, "finflow-local/wallet-icons/$accountId.jpg")
        else File(activity.cacheDir, "wallet-icons/$ownerId/$accountId.jpg")

    private fun write(file: File, bytes: ByteArray) {
        val parent = checkNotNull(file.parentFile)
        check(parent.isDirectory || parent.mkdirs())
        val atomic = android.util.AtomicFile(file)
        val output = atomic.startWrite()
        try { output.write(bytes); atomic.finishWrite(output) } catch (e: Exception) { atomic.failWrite(output); throw e }
    }

    companion object {
        private const val MAX_SOURCE_BYTES = 5 * 1024 * 1024
        private const val MAX_ICON_BYTES = 100 * 1024
        private val ACCOUNT_ID = Regex("[A-Za-z0-9_-]{1,93}")
        private val OWNER_ID = Regex("[A-Za-z0-9]{1,128}")

        /** Potong tengah menjadi persegi, perkecil, lalu turunkan kualitas JPEG sampai ≤ [maxBytes]. */
        fun thumbnail(raw: ByteArray, maxBytes: Int): ByteArray {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)
            require(bounds.outWidth in 1..20000 && bounds.outHeight in 1..20000) { MR.strings.core_photo_format_size_unsupported.text() }
            var sample = 1
            while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 512) sample *= 2
            val decoded = requireNotNull(BitmapFactory.decodeByteArray(raw, 0, raw.size, BitmapFactory.Options().apply { inSampleSize = sample })) {
                MR.strings.core_photo_unreadable.text()
            }
            val orientation = runCatching { ExifInterface(ByteArrayInputStream(raw)).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
                .getOrDefault(ExifInterface.ORIENTATION_NORMAL)
            val rotate = Matrix().apply {
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
            val side = minOf(decoded.width, decoded.height)
            try {
                for (target in listOf(256, 160, 96)) {
                    val scale = target.toFloat() / side
                    val matrix = Matrix(rotate).apply { postScale(scale, scale) }
                    val square = Bitmap.createBitmap(decoded, (decoded.width - side) / 2, (decoded.height - side) / 2, side, side, matrix, true)
                    try {
                        for (quality in listOf(85, 75, 65, 50, 40)) {
                            val bytes = ByteArrayOutputStream().also { check(square.compress(Bitmap.CompressFormat.JPEG, quality, it)) }.toByteArray()
                            if (bytes.size <= maxBytes) return bytes
                        }
                    } finally { if (square !== decoded) square.recycle() }
                }
            } finally { decoded.recycle() }
            throw IllegalArgumentException(MR.strings.r3_icon_too_large.text())
        }
    }
}
