package com.logikawan.finflow.core.model

import com.logikawan.finflow.core.util.checkedAdd
import com.logikawan.finflow.core.util.newId
import com.logikawan.finflow.core.util.nowMillis
import com.logikawan.finflow.core.util.today
import kotlinx.serialization.Serializable

@Serializable
data class Profile(val id: String, val name: String, val email: String, val baseCurrency: String = "IDR", val schemaVersion: Int = 2,
    val photoPath: String = "", val photoUpdatedAt: Long = 0)

@Serializable
data class Account(
    val id: String = newId(), val name: String, val type: String = "cash",
    val currency: String = "IDR", val openingBalanceMinor: Long = 0,
    val archived: Boolean = false, val createdAt: Long = nowMillis(),
    val excludedFromTotal: Boolean = false, val parentId: String? = null,
    val icon: String = "account_balance_wallet", val color: Long = 0xFF2196F3,
    val goalId: String? = null,
)

@Serializable
data class Category(val id: String, val name: String, val type: String, val parentId: String? = null, val icon: String = "", val color: Long? = null)

@Serializable
data class Transaction(
    val id: String = newId(), val type: String, val amountMinor: Long,
    val currency: String, val accountId: String, val categoryId: String,
    val title: String, val description: String = "", val date: String = today().toString(),
    val toAccountId: String? = null, val receivedAmountMinor: Long? = null,
    val createdAt: Long = nowMillis(), val updatedAt: Long = nowMillis(),
    val source: String = "manual", val sourceId: String? = null,
    val scheduled: Boolean = false,
    val subcategoryId: String? = null, val tags: List<String> = emptyList(),
)

@Serializable
data class Budget(
    val id: String = newId(), val categoryId: String, val limitMinor: Long,
    val currency: String = "IDR", val month: String = today().toString().take(7),
    val title: String = "", val period: String = "monthly",
)

@Serializable
data class Goal(
    val id: String = newId(), val name: String, val targetMinor: Long,
    val savedMinor: Long = 0, val currency: String = "IDR", val targetDate: String = "",
    val bucketType: String = "standard", val protection: String = "flexible",
    val holdingAccountId: String? = null, val legacySavedMinor: Long = 0,
    val archived: Boolean = false, val revision: Int = 0,
    val lastEventId: String? = null,
)

@Serializable
data class Investment(
    val id: String = newId(), val name: String, val type: String,
    val investedMinor: Long, val currentMinor: Long, val dividendsMinor: Long = 0,
    val currency: String = "IDR", val platform: String = "", val notes: String = "",
)

@Serializable
data class FinanceData(
    val accounts: List<Account> = emptyList(), val categories: List<Category> = emptyList(),
    val transactions: List<Transaction> = emptyList(), val budgets: List<Budget> = emptyList(),
    val goals: List<Goal> = emptyList(), val investments: List<Investment> = emptyList(),
    val profile: Profile? = null,
    val bills: List<Bill> = emptyList(), val debts: List<Debt> = emptyList(),
    val wishes: List<Wish> = emptyList(), val cards: List<WalletCard> = emptyList(),
    val notes: List<FinanceNote> = emptyList(), val reimbursements: List<Reimbursement> = emptyList(),
    val recurring: List<RecurringTransaction> = emptyList(), val financialEvents: List<FinancialEvent> = emptyList(),
    val featuresReady: Boolean = false, val featuresError: String? = null,
) {
    // Saldo dihitung dari jurnal; impor ulang tidak menggandakan pembaruan saldo terpisah.
    fun balance(account: Account): Long = transactions.filterNot { it.scheduled }.fold(account.openingBalanceMinor) { sum, tx ->
        val delta = when {
            tx.accountId == account.id -> if (tx.type == "income") tx.amountMinor else -tx.amountMinor
            tx.type == "transfer" && tx.toAccountId == account.id -> tx.receivedAmountMinor ?: tx.amountMinor
            else -> 0L
        }
        checkedAdd(sum, delta)
    }
    fun categoryName(id: String) = categories.find { it.id == id }?.name ?: "Lainnya"
    fun accountName(id: String) = accounts.find { it.id == id }?.name ?: "Dompet diarsipkan"
    fun includedAccounts() = accounts.filter { !it.archived && !it.excludedFromTotal }
    fun reportTransactions() = transactions.filter { tx -> !tx.scheduled && accounts.none { it.excludedFromTotal && (it.id == tx.accountId || it.id == tx.toAccountId) } }
}

val defaultCategories = listOf(
    Category("food", "Makan & minum", "expense"), Category("transport", "Transportasi", "expense"),
    Category("shopping", "Belanja", "expense"), Category("health", "Kesehatan", "expense"),
    Category("bills", "Tagihan", "expense"), Category("education", "Pendidikan", "expense"),
    Category("entertainment", "Hiburan", "expense"), Category("other_expense", "Lainnya", "expense"),
    Category("salary", "Gaji", "income"), Category("business", "Usaha", "income"),
    Category("other_income", "Lainnya", "income"),
)
