package com.logikawan.finflow.core.model

import com.logikawan.finflow.core.util.newId
import com.logikawan.finflow.core.util.nowMillis
import com.logikawan.finflow.core.util.today
import kotlinx.serialization.Serializable

@Serializable
data class Bill(
    val id: String = newId(), val title: String, val amountMinor: Long, val currency: String = "IDR",
    val accountId: String, val categoryId: String = "bills", val dueDate: String = today().toString(),
    val frequency: String = "once", val anchorDay: Int = 1, val note: String = "",
    val paidAt: Long = 0, val lastPaymentId: String? = null, val archived: Boolean = false,
    val revision: Int = 0, val createdAt: Long = nowMillis(),
)

@Serializable
data class Debt(
    val id: String = newId(), val person: String, val direction: String = "borrowed",
    val amountMinor: Long, val paidMinor: Long = 0, val currency: String = "IDR",
    val accountId: String, val issueDate: String = today().toString(), val dueDate: String = today().toString(),
    val includeBalance: Boolean = true, val note: String = "", val archived: Boolean = false,
    val lastEventId: String? = null, val revision: Int = 0, val createdAt: Long = nowMillis(),
)

@Serializable
data class Wish(
    val id: String = newId(), val title: String, val targetMinor: Long, val currency: String = "IDR",
    val priority: String = "medium", val link: String = "", val note: String = "",
    val fulfilled: Boolean = false, val createdAt: Long = nowMillis(),
)

@Serializable
data class WalletCard(
    val id: String = newId(), val name: String, val brand: String = "visa", val lastFour: String,
    val accountId: String = "", val expiryMonth: Int = 0, val expiryYear: Int = 0,
    val note: String = "", val createdAt: Long = nowMillis(),
    val recordType: String = "card", val accountNumber: String = "", val ownerName: String = "",
    val label: String = "", val branch: String = "", val ownership: String = "personal", val category: String = "main",
    val pinned: Boolean = false, val color: Long = 0xFF2196F3, val design: Int = 0,
)

@Serializable
data class BundleItem(val id:String=newId(),val name:String,val amountMinor:Long,val checked:Boolean=false,val paymentId:String?=null)

// Pembuktian pembayaran tetap di bawah batas evaluasi rules Firestore per dokumen.
const val MAX_BUNDLE_ITEMS = 5

@Serializable
data class FinanceNote(
    val id: String = newId(), val title: String, val body: String = "", val pinned: Boolean = false,
    val createdAt: Long = nowMillis(), val updatedAt: Long = nowMillis(),
    val kind: String = "note", val currency: String = "IDR", val accountId: String? = null,
    val recordAs: String = "single", val items: List<BundleItem> = emptyList(), val archived: Boolean = false,
    val revision: Int = 0, val lastPaymentId: String? = null,
)

@Serializable
data class Reimbursement(
    val id: String, val expenseId: String, val title: String, val party: String, val amountMinor: Long,
    val currency: String, val accountId: String, val note: String = "",
    val receivedAt: Long = 0, val receivedAccountId: String? = null, val revision: Int = 0,
    val createdAt: Long = nowMillis(),
)

@Serializable
data class RecurringTransaction(
    val id: String = newId(), val title: String, val type: String = "expense", val amountMinor: Long,
    val currency: String = "IDR", val accountId: String, val categoryId: String,
    val frequency: String = "monthly", val interval: Int = 1, val anchorDay: Int = 1,
    val startDate: String = today().toString(), val nextDate: String = today().toString(),
    val endDate: String? = null, val active: Boolean = true, val note: String = "",
    val lastEventId: String? = null, val revision: Int = 0, val createdAt: Long = nowMillis(),
)

@Serializable
data class FinancialEvent(
    val id: String, val kind: String, val referenceId: String, val title: String,
    val amountMinor: Long, val currency: String, val accountId: String, val date: String,
    val periodDate: String = date, val transactionId: String? = id, val createdAt: Long = nowMillis(),
    val previousEventId: String? = null,
    val itemIds: List<String> = emptyList(),
)

val managedTransactionSources = setOf("bill", "debt_initial", "debt_payment", "reimbursement", "recurring", "bundle", "goal_deposit", "goal_withdrawal")

