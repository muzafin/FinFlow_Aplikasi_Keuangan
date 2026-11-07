package com.clonical.kasiva

import com.clonical.kasiva.i18n.*
import com.clonical.kasiva.core.i18n.text
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import com.clonical.kasiva.core.data.LocalFinanceLoad
import com.clonical.kasiva.core.data.LocalFinancePersistence
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.NoSuchFileException
import java.nio.file.attribute.BasicFileAttributes
import java.security.KeyStore
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class AndroidLocalFinancePersistence private constructor(private val context: Context) : LocalFinancePersistence {
    override val id: String = "finflow-local-v1"

    @Synchronized
    override fun load(): LocalFinanceLoad = try {
        val file = store()
        if (!present(file.baseFile) && !present(File(file.baseFile.path + ".bak"))) LocalFinanceLoad(null)
        else {
            val bytes = file.openRead().use { input ->
                require(input.channel.size() <= MAX_STORED_BYTES)
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= MAX_STORED_BYTES)
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            require(bytes.size > HEADER.size + IV_BYTES + TAG_BYTES)
            require(bytes.copyOfRange(0, HEADER.size).contentEquals(HEADER))
            val iv = bytes.copyOfRange(HEADER.size, HEADER.size + IV_BYTES)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(create = false), GCMParameterSpec(TAG_BYTES * 8, iv))
            cipher.updateAAD(HEADER)
            val plain = cipher.doFinal(bytes, HEADER.size + IV_BYTES, bytes.size - HEADER.size - IV_BYTES)
            require(plain.size <= MAX_JSON_BYTES)
            val content = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(plain)).toString()
            require(Json.parseToJsonElement(content) is JsonObject)
            LocalFinanceLoad(content)
        }
    } catch (_: MissingKeyException) {
        LocalFinanceLoad(null, MR.strings.core_local_key_unavailable.text())
    } catch (_: AEADBadTagException) {
        LocalFinanceLoad(null, MR.strings.core_local_corrupt_key.text())
    } catch (_: Exception) {
        LocalFinanceLoad(null, MR.strings.core_local_cannot_open.text())
    }

    @Synchronized
    override fun save(content: String): String? {
        if(context.getSharedPreferences("finflow",Context.MODE_PRIVATE).getString("local.sync.frozen",null)=="true")return MR.strings.core_local_sync_paused.text()
        var file: AtomicFile? = null
        var output: FileOutputStream? = null
        return try {
            require(content.length <= MAX_JSON_BYTES)
            val plain = content.toByteArray(Charsets.UTF_8)
            require(plain.isNotEmpty() && plain.size <= MAX_JSON_BYTES)
            require(Json.parseToJsonElement(content) is JsonObject)
            val store = store()
            file = store
            val directory = requireNotNull(store.baseFile.parentFile)
            check(directory.isDirectory || directory.mkdirs())
            val exists = present(store.baseFile) || present(File(store.baseFile.path + ".bak"))
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key(create = !exists))
            cipher.updateAAD(HEADER)
            val iv = cipher.iv
            require(iv.size == IV_BYTES)
            val encrypted = cipher.doFinal(plain)
            val prefix = HEADER + iv
            val stream = store.startWrite()
            output = stream
            stream.write(prefix)
            stream.write(encrypted)
            stream.fd.sync()
            store.finishWrite(stream)
            output = null
            // AtomicFile dapat mencatat kegagalan rename tanpa melempar; pastikan versi baru benar-benar tersimpan.
            store.openRead().use { input ->
                check(input.channel.size() == (prefix.size + encrypted.size).toLong())
                val storedPrefix = ByteArray(prefix.size)
                DataInputStream(input).readFully(storedPrefix)
                check(storedPrefix.contentEquals(prefix))
            }
            null
        } catch (_: MissingKeyException) {
            MR.strings.core_local_key_missing.text()
        } catch (_: Exception) {
            MR.strings.core_local_save_failed_device.text()
        } finally {
            output?.let { stream -> runCatching { file?.failWrite(stream) } }
        }
    }

    private fun store() = AtomicFile(File(context.noBackupFilesDir, "finflow-local/finance-v1.aes"))

    private fun present(file: File): Boolean = try {
        val attributes = Files.readAttributes(file.toPath(), BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS)
        require(attributes.isRegularFile)
        true
    } catch (_: NoSuchFileException) { false }

    private fun key(create: Boolean): SecretKey {
        val keys = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keys.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        if (!create || keys.containsAlias(KEY_ALIAS)) throw MissingKeyException()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).setRandomizedEncryptionRequired(true).build())
        }.generateKey()
    }

    private class MissingKeyException : IllegalStateException()

    companion object {
        private const val KEY_ALIAS = "finflow.local.finance.v1"
        private const val MAX_JSON_BYTES = 20 * 1024 * 1024
        private const val IV_BYTES = 12
        private const val TAG_BYTES = 16
        private val HEADER = "FFLOCAL1".toByteArray(Charsets.US_ASCII)
        private val MAX_STORED_BYTES = MAX_JSON_BYTES + HEADER.size + IV_BYTES + TAG_BYTES
        @Volatile private var instance: AndroidLocalFinancePersistence? = null

        fun get(context: Context): AndroidLocalFinancePersistence = instance ?: synchronized(this) {
            instance ?: AndroidLocalFinancePersistence(context.applicationContext).also { instance = it }
        }
    }
}
