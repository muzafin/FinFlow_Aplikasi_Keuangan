package com.clonical.kasiva

import com.clonical.kasiva.core.data.*
import com.clonical.kasiva.core.model.*
import com.clonical.kasiva.core.util.*
import com.clonical.kasiva.feature.ai.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import kotlin.test.*

class AdditionalBugRegressionTest {
    private class Memory : LocalFinancePersistence {
        override val id = "additional-${newId()}"
        private var content: String? = null
        override fun load() = LocalFinanceLoad(content)
        override fun save(content: String): String? { this.content = content; return null }
    }

    @Test fun notificationCannotPostToGoalWalletOrWrongCategory() = runTest {
        val repo = FinanceRepository(local = Memory())
        repo.initializeProfile("Lokal", "IDR", "Kas", 100_000)
        repo.saveGoal(Goal("trip", "Liburan", 100_000))
        val hash = "a".repeat(64)
        val notification = Transaction("notification-$hash", "expense", 1000, "IDR", "goal-trip", "food", "Makan", source = "notification", sourceId = hash)
        assertFailsWith<IllegalArgumentException> { repo.importNotification(notification) }
        assertFailsWith<IllegalArgumentException> { repo.importNotification(notification.copy(accountId = "acc_cash", categoryId = "salary")) }
        assertTrue(repo.observe().first { it.featuresReady }.transactions.isEmpty())
        assertTrue(repo.importNotification(notification.copy(accountId = "acc_cash")))
        assertFalse(repo.importNotification(notification.copy(accountId = "acc_cash")))
        BackupCodec.encode(FinanceBackup(ownerId = "local", data = repo.observe().first { it.featuresReady }))
    }

    @Test fun csvCannotBypassCategoryAndSubcategoryValidation() = runTest {
        val repo = FinanceRepository(local = Memory())
        repo.initializeProfile("Lokal", "IDR", "Kas", 100_000)
        val tx = Transaction("csv-test", "expense", 1000, "IDR", "acc_cash", "salary", "Salah kategori")
        fun plan(value: Transaction) = ImportPlan(emptyList(), emptyList(), listOf(value), emptyList(), emptyList(), 0)
        assertFailsWith<IllegalArgumentException> { repo.importCsv(plan(tx)) { _, _ -> } }
        assertFailsWith<IllegalArgumentException> { repo.importCsv(plan(tx.copy(categoryId = "food", subcategoryId = "salary"))) { _, _ -> } }
        assertTrue(repo.observe().first { it.featuresReady }.transactions.isEmpty())
    }

    private val aiData = FinanceData(
        profile = Profile("local", "Nama", "", "IDR"),
        accounts = listOf(Account("cash", "Kas"), Account("usd", "Dolar", currency = "USD"), Account("goal", "Target", type = "savings")),
        categories = defaultCategories,
    )

    @Test fun aiTransferNeverInventsExchangeRateOrBecomesExpense() {
        val crossCurrency = assertNotNull(AiDraft(type = "transfer", amount = 100.0, walletId = "cash", toWalletId = "usd").toTransaction(aiData, "cash"))
        assertEquals("transfer", crossCurrency.type)
        assertNull(crossCurrency.receivedAmountMinor)
        assertFailsWith<IllegalArgumentException> { FinanceRepository.validateTransaction(crossCurrency) }
        assertNull(AiDraft(type = "transfer", amount = 100.0, walletId = "cash", toWalletId = "goal").toTransaction(aiData, "cash"))
        assertNull(AiDraft(type = "transfer", amount = 100.0, walletId = "cash", toWalletId = "cash").toTransaction(aiData, "cash"))
    }

    @Test fun aiDraftRejectsNonfiniteAndOutOfRangeAmountsAndRepairsInvalidDates() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -1.0, 1e20).forEach { amount ->
            assertNull(AiDraft(amount = amount).toTransaction(aiData, "cash"))
        }
        val tx = assertNotNull(AiDraft(amount = 100.0, date = "2026-02-30").toTransaction(aiData, "cash"))
        assertEquals(today().toString(), tx.date)
        FinanceRepository.validateTransaction(tx)
    }

    @Test fun assistantBudgetSpendingUsesBudgetCurrencyAndPeriod() {
        val day = today().toString()
        val data = aiData.copy(
            transactions = listOf(Transaction("idr", "expense", 1_000_000, "IDR", "cash", "food", "Makan"),
                Transaction("usd", "expense", 500, "USD", "usd", "food", "Lunch"),
                Transaction("old", "expense", 100, "USD", "usd", "food", "Old lunch", date = today().plus(DatePeriod(days = -1)).toString())),
            budgets = listOf(Budget("weekly", "food", 1000, currency = "USD", month = day, title = "Dolar mingguan", period = "weekly"),
                Budget("old-week", "food", 1000, currency = "USD", month = today().plus(DatePeriod(days = -8)).toString(), title = "Minggu lalu", period = "weekly")),
        )
        val line = assistantSummary(data).lineSequence().single { it.startsWith("Anggaran Dolar mingguan:") }
        assertTrue(line.contains("terpakai USD 5,00"), line)
        assertFalse(line.contains("10.000"), line)
        assertFalse(assistantSummary(data).contains("Anggaran Minggu lalu:"))
    }
}
