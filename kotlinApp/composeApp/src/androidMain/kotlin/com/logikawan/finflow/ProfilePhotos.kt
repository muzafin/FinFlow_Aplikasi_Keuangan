package com.logikawan.finflow

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
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream

class ProfilePhotos(private val activity: FragmentActivity) {
    private var pending: ((ByteArray?, String?) -> Unit)? = null
    private val storage get() = FirebaseStorage.getInstance("gs://finflow-logikawan.firebasestorage.app")
    private val picker = activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val callback = pending ?: return@registerForActivityResult
        pending = null
        if (uri == null) { callback(null, null); return@registerForActivityResult }
        activity.lifecycleScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    val raw = activity.contentResolver.openInputStream(uri).use { input ->
                        requireNotNull(input) { "Foto tidak dapat dibaca" }
                        val output = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            require(output.size() + count <= 5 * 1024 * 1024) { "Pilih foto maksimal 5 MB" }
                            output.write(buffer, 0, count)
                        }
                        output.toByteArray()
                    }
                    require(raw.size <= 5 * 1024 * 1024) { "Pilih foto maksimal 5 MB" }
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)
                    require(bounds.outWidth in 1..20000 && bounds.outHeight in 1..20000) { "Format atau ukuran foto tidak didukung" }
                    var sample = 1
                    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1024) sample *= 2
                    val bitmap = requireNotNull(BitmapFactory.decodeByteArray(raw, 0, raw.size,
                        BitmapFactory.Options().apply { inSampleSize = sample })) { "Foto tidak dapat dibaca" }
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
                    try {
                        val oriented = if (matrix.isIdentity) bitmap else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                        try { ByteArrayOutputStream().also { check(oriented.compress(Bitmap.CompressFormat.JPEG, 85, it)) }.toByteArray() }
                        finally { if (oriented !== bitmap) oriented.recycle() }
                    }
                    finally { bitmap.recycle() }
                }
                require(bytes.size <= 2 * 1024 * 1024) { "Foto hasil kompresi terlalu besar" }
                callback(bytes, null)
            } catch (e: Exception) { callback(null, e.message ?: "Foto gagal dibaca") }
        }
    }
    fun pick(result: (ByteArray?, String?) -> Unit) {
        if (pending != null) { result(null, "Pemilihan foto masih berjalan"); return }
        pending = result
        try { picker.launch("image/*") }
        catch (e: Exception) { pending = null; result(null, "Pemilih foto tidak tersedia") }
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
                }catch(e:Exception){result(null,"Foto lokal gagal disimpan")}
            }
            return
        }
        if (uid != FirebaseAuth.getInstance().currentUser?.uid || bytes.isEmpty() || bytes.size > 2 * 1024 * 1024) {
            result(null, "Akun atau ukuran foto tidak valid"); return
        }
        val path = "users/$uid/profile/avatar.jpg"
        storage.reference.child(path).putBytes(bytes, StorageMetadata.Builder().setContentType("image/jpeg").build())
            .addOnSuccessListener { result(path, null) }
            .addOnFailureListener { result(null, "Foto gagal diunggah. Periksa koneksi dan konfigurasi Storage.") }
    }
    fun load(path: String, result: (ByteArray?, String?) -> Unit) {
        if(path=="local/profile/avatar.jpg") {
            activity.lifecycleScope.launch {
                try {
                    val bytes=withContext(Dispatchers.IO){val file=java.io.File(activity.noBackupFilesDir,"finflow-local/avatar.jpg");require(file.length() in 1..2*1024*1024);file.readBytes()}
                    result(bytes,null)
                }catch(e:Exception){result(null,"Foto lokal belum dapat dimuat")}
            }
            return
        }
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null || path != "users/$uid/profile/avatar.jpg") { result(null, "Akses foto ditolak"); return }
        storage.reference.child(path).getBytes(2L * 1024 * 1024)
            .addOnSuccessListener { result(it, null) }.addOnFailureListener { result(null, "Foto belum dapat dimuat") }
    }
}
