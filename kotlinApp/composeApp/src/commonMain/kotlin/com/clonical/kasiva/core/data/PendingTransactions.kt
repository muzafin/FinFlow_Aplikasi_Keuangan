package com.clonical.kasiva.core.data

import com.clonical.kasiva.core.model.FinanceData
import com.clonical.kasiva.core.model.Transaction
import com.clonical.kasiva.core.platform.NativeServices
import com.clonical.kasiva.core.util.nowMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Operasi transaksi yang dicatat saat perangkat offline dan menunggu dikirim ke Firestore. */
@Serializable
data class PendingTransactionOp(
    val id: String,
    val remove: Boolean = false,
    val transaction: Transaction? = null,
    val createdAt: Long = nowMillis(),
)

/**
 * Antrean transaksi offline untuk mode cloud. Disimpan per akun di perangkat; dikirim ulang
 * lewat FinanceRepository.flushPendingTransactions() saat internet kembali.
 */
class PendingTransactionStore(private val native: NativeServices) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private var owner: String? = null
    private val _operations = MutableStateFlow<List<PendingTransactionOp>>(emptyList())
    val operations = _operations.asStateFlow()

    fun bind(owner: String?) {
        if (this.owner == owner) return
        this.owner = owner
        _operations.value = load(owner)
    }

    fun enqueueSave(transaction: Transaction) = add(PendingTransactionOp(transaction.id, false, transaction))
    fun enqueueDelete(transaction: Transaction) = add(PendingTransactionOp(transaction.id, true, null))

    fun remove(operation: PendingTransactionOp) {
        val next = _operations.value.filterNot { it.id == operation.id }
        persist(next)
        _operations.value = next
    }

    private fun add(operation: PendingTransactionOp) {
        // Satu transaksi hanya punya satu operasi terakhir: edit menimpa simpan sebelumnya, hapus membatalkannya.
        val next = _operations.value.filterNot { it.id == operation.id } + operation
        persist(next)
        _operations.value = next
    }

    private fun load(owner: String?): List<PendingTransactionOp> {
        if (owner.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString<List<PendingTransactionOp>>(native.readSetting(key(owner)) ?: "[]")
        }.getOrDefault(emptyList())
    }

    private fun persist(list: List<PendingTransactionOp>) {
        val id = owner ?: return
        native.writeSetting(key(id), json.encodeToString(list))
    }

    private fun key(owner: String) = "pending.transactions.$owner"
}

/** Terapkan antrean offline ke data cloud yang sedang tampil di UI. */
fun FinanceData.withPendingTransactions(operations: List<PendingTransactionOp>): FinanceData {
    if (operations.isEmpty()) return this
    val deletes = operations.filter { it.remove }.map { it.id }.toSet()
    val byId = LinkedHashMap<String, Transaction>()
    transactions.filterNot { it.id in deletes }.forEach { byId[it.id] = it }
    operations.filter { !it.remove }.mapNotNull { it.transaction }.forEach { byId[it.id] = it }
    return copy(transactions = byId.values.sortedByDescending { it.date })
}
