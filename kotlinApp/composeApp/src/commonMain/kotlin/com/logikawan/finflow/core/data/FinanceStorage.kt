package com.logikawan.finflow.core.data

import com.logikawan.finflow.i18n.*
import com.logikawan.finflow.core.i18n.text
import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.DocumentReference
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Transaction as GitLiveTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

data class LocalFinanceLoad(val content: String?, val error: String? = null)

interface LocalFinancePersistence {
    val id: String
    fun load(): LocalFinanceLoad
    // Implementasi native wajib mengganti berkas secara atomik dan mengembalikan kegagalan tanpa menghapus berkas lama.
    fun save(content: String): String?
}

@PublishedApi
internal val financeStorageJson = Json { encodeDefaults = true; ignoreUnknownKeys = true }

// shortcut: snapshot lokal maksimal 20 MB; gunakan database lokal ketika riwayat membutuhkan ukuran lebih besar.
private const val MAX_LOCAL_BYTES = 20 * 1024 * 1024
private val localCollections = setOf("accounts", "categories", "transactions", "budgets", "goals", "investments",
    "bills", "debts", "wishes", "cards", "notes", "reimbursements", "recurring", "financialEvents")

private fun localDocumentPath(path: String) {
    val parts = path.split('/')
    require(path == "users/local" || (parts.size == 4 && parts[0] == "users" && parts[1] == "local" && parts[2] in localCollections
        && parts[3].isNotBlank() && parts[3] !in listOf(".", "..") && parts[3].length <= 1000)) { "Jalur data lokal tidak valid" }
}

private fun localCollectionPath(path: String) {
    val parts = path.split('/')
    require(path == "users" || (parts.size == 3 && parts[0] == "users" && parts[1] == "local" && parts[2] in localCollections)) {
        "Koleksi data lokal tidak valid"
    }
}

@Serializable
private data class LocalFinanceEnvelope(val version: Int, val records: Map<String, JsonObject>)

data class FinanceLocalSnapshot internal constructor(val generation: Long, @PublishedApi internal val records: Map<String, JsonObject>) {
    fun document(path: String): FinanceSnapshot {
        localDocumentPath(path)
        return FinanceSnapshot(path, local = records[path])
    }
    fun collection(path: String): FinanceQuerySnapshot {
        localCollectionPath(path)
        val prefix = "$path/"
        return FinanceQuerySnapshot(records.entries.filter { it.key.startsWith(prefix) && '/' !in it.key.removePrefix(prefix) }
            .sortedBy { it.key }.map { FinanceSnapshot(it.key, local = it.value) })
    }
}

private class SharedLocalFinanceStore(private val persistence: LocalFinancePersistence) {
    val mutex = Mutex()
    private var persisted = false
    val state = MutableStateFlow(FinanceLocalSnapshot(0, readRecords()))

    private fun readRecords(): Map<String, JsonObject> {
        val loaded = persistence.load()
        check(loaded.error == null) { loaded.error ?: MR.strings.core_error_local_read_failed.text() }
        val content = loaded.content
        if (content == null) {
            check(!persisted) { MR.strings.core_error_local_file_missing.text() }
            return emptyMap()
        }
        check(content.length <= MAX_LOCAL_BYTES && content.encodeToByteArray().size <= MAX_LOCAL_BYTES) { MR.strings.core_error_local_too_large.text() }
        val envelope = try { Json.decodeFromString<LocalFinanceEnvelope>(content) }
        catch (error: Exception) { throw IllegalStateException(MR.strings.core_error_local_corrupt.text(), error) }
        check(envelope.version == 1) { MR.strings.core_error_local_version.text() }
        envelope.records.keys.forEach(::localDocumentPath)
        persisted = true
        return envelope.records.toMap()
    }

    private fun reload(): FinanceLocalSnapshot {
        val records = readRecords()
        if (records != state.value.records) state.value = FinanceLocalSnapshot(state.value.generation + 1, records)
        return state.value
    }
    suspend fun snapshot(): FinanceLocalSnapshot = mutex.withLock { reload() }

    suspend fun <T> transaction(block: suspend (MutableMap<String, JsonObject>) -> T): T = mutex.withLock {
        val before = reload()
        val draft = before.records.toMutableMap()
        val result = block(draft)
        if (draft != before.records) {
            val content = financeStorageJson.encodeToString(LocalFinanceEnvelope(1, draft))
            check(content.length <= MAX_LOCAL_BYTES && content.encodeToByteArray().size <= MAX_LOCAL_BYTES) { MR.strings.core_error_local_too_large_export.text() }
            val failure = persistence.save(content)
            check(failure == null) { failure ?: MR.strings.core_error_local_save_failed.text() }
            persisted = true
            state.value = FinanceLocalSnapshot(before.generation + 1, draft.toMap())
        }
        result
    }
}

private object LocalFinanceStores {
    private val mutex = Mutex()
    private val stores = mutableMapOf<String, SharedLocalFinanceStore>()
    suspend fun get(persistence: LocalFinancePersistence): SharedLocalFinanceStore = mutex.withLock {
        require(persistence.id.isNotBlank()) { "Identitas penyimpanan lokal tidak valid" }
        stores.getOrPut(persistence.id) { SharedLocalFinanceStore(persistence) }
    }
}

class FinanceDatabase(@PublishedApi internal val remote: FirebaseFirestore? = null, private val local: LocalFinancePersistence? = null) {
    init { require((remote != null) != (local != null)) { "Pilih satu penyimpanan: cloud atau lokal" } }
    val isLocal: Boolean get() = local != null

    val localSnapshots: Flow<FinanceLocalSnapshot> get() = flow {
        val store = LocalFinanceStores.get(checkNotNull(local) { "Penyimpanan ini bukan mode lokal" })
        store.snapshot()
        emitAll(store.state)
    }
    suspend fun localSnapshot(): FinanceLocalSnapshot = LocalFinanceStores.get(checkNotNull(local) { "Penyimpanan ini bukan mode lokal" }).snapshot()

    fun document(path: String): FinanceDocument {
        if (isLocal) localDocumentPath(path)
        return FinanceDocument(this, path, remote?.document(path))
    }
    fun collection(path: String): FinanceCollection {
        if (isLocal) localCollectionPath(path)
        return FinanceCollection(this, path, remote?.collection(path))
    }
    suspend fun <T> runTransaction(block: suspend FinanceTransaction.() -> T): T {
        if (remote != null) return remote.runTransaction { block(FinanceTransaction(this@FinanceDatabase, this, null)) }
        return LocalFinanceStores.get(checkNotNull(local)).transaction { block(FinanceTransaction(this, null, it)) }
    }
    @PublishedApi internal fun accepts(document: FinanceDocument) {
        require((remote != null && document.remote != null) || (local != null && document.database.local?.id == local.id)) {
            "Dokumen berasal dari penyimpanan lain"
        }
    }
}

class FinanceDocument internal constructor(@PublishedApi internal val database: FinanceDatabase, val path: String,
    @PublishedApi internal val remote: DocumentReference? = null) {
    val id: String get() = path.substringAfterLast('/')
    fun collection(name: String): FinanceCollection = database.collection("$path/$name")
    suspend fun get(): FinanceSnapshot = if (remote != null) FinanceSnapshot(path, remote = remote.get()) else database.localSnapshot().document(path)
    suspend inline fun <reified T : Any> set(value: T) {
        if (remote != null) remote.set(value) else database.runTransaction { set(this@FinanceDocument, value) }
    }
    suspend fun delete() {
        if (remote != null) remote.delete() else database.runTransaction { delete(this@FinanceDocument) }
    }
    val snapshots: Flow<FinanceSnapshot> get() = remote?.snapshots?.map { FinanceSnapshot(path, remote = it) }
        ?: database.localSnapshots.map { it.document(path) }
}

class FinanceCollection internal constructor(private val database: FinanceDatabase, val path: String, private val remote: CollectionReference? = null) {
    fun document(id: String): FinanceDocument = database.document("$path/$id")
    suspend fun get(): FinanceQuerySnapshot = remote?.get()?.let { FinanceQuerySnapshot(it.documents.map { document -> FinanceSnapshot("$path/${document.id}", remote = document) }) }
        ?: database.localSnapshot().collection(path)
    val snapshots: Flow<FinanceQuerySnapshot> get() = remote?.snapshots?.map { snapshot ->
        FinanceQuerySnapshot(snapshot.documents.map { FinanceSnapshot("$path/${it.id}", remote = it) })
    } ?: database.localSnapshots.map { it.collection(path) }
}

class FinanceSnapshot @PublishedApi internal constructor(val path: String, @PublishedApi internal val remote: DocumentSnapshot? = null,
    @PublishedApi internal val local: JsonObject? = null) {
    val id: String get() = path.substringAfterLast('/')
    val exists: Boolean get() = remote?.exists ?: (local != null)
    inline fun <reified T> data(): T = if (remote != null) remote.data<T>() else financeStorageJson.decodeFromJsonElement(checkNotNull(local) { "Dokumen $path tidak ditemukan" })
}

class FinanceQuerySnapshot internal constructor(val documents: List<FinanceSnapshot>)

class FinanceTransaction internal constructor(@PublishedApi internal val database: FinanceDatabase,
    @PublishedApi internal val remote: GitLiveTransaction?, @PublishedApi internal val draft: MutableMap<String, JsonObject>?) {
    suspend fun get(document: FinanceDocument): FinanceSnapshot {
        database.accepts(document)
        return if (remote != null) FinanceSnapshot(document.path, remote = remote.get(checkNotNull(document.remote)))
            else FinanceSnapshot(document.path, local = checkNotNull(draft)[document.path])
    }
    inline fun <reified T : Any> set(document: FinanceDocument, value: T) {
        database.accepts(document)
        if (remote != null) remote.set(checkNotNull(document.remote), value)
        else checkNotNull(draft)[document.path] = financeStorageJson.encodeToJsonElement(value) as? JsonObject
            ?: throw IllegalArgumentException("Dokumen keuangan harus berupa objek")
    }
    fun delete(document: FinanceDocument) {
        database.accepts(document)
        if (remote != null) remote.delete(checkNotNull(document.remote)) else checkNotNull(draft).remove(document.path)
    }
}
