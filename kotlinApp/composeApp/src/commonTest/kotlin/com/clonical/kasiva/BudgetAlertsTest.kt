package com.clonical.kasiva

import com.clonical.kasiva.core.model.Budget
import com.clonical.kasiva.core.model.FinanceData
import com.clonical.kasiva.core.model.Transaction
import com.clonical.kasiva.feature.budget.budgetAlerts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Disiapkan untuk pengguna; tidak dijalankan selama pengerjaan.
class BudgetAlertsTest {
    private val budget = Budget(id = "food", categoryId = "food", limitMinor = 1000, currency = "IDR", month = "2026-10")
    private fun expense(id: String, amount: Long, currency: String = "IDR", date: String = "2026-10-29", scheduled: Boolean = false, type: String = "expense") =
        Transaction(id = id, type = type, amountMinor = amount, currency = currency, accountId = "cash", categoryId = "food", title = "Makan", date = date, scheduled = scheduled)

    @Test
    fun excludesOtherCurrenciesMonthsIncomeAndScheduledTransactions() {
        val data = FinanceData(budgets = listOf(budget), transactions = listOf(
            expense("real", 799), expense("usd", 5000, currency = "USD"), expense("old", 5000, date = "2026-09-29"),
            expense("scheduled", 5000, scheduled = true), expense("income", 5000, type = "income"),
        ))
        assertTrue(budgetAlerts(data, "2026-10").isEmpty())
    }

    @Test
    fun levelsHaveStableDistinctIdentifiers() {
        fun alerts(amount: Long) = budgetAlerts(FinanceData(budgets = listOf(budget), transactions = listOf(expense("real", amount))), "2026-10")
        assertEquals("food:2026-10:80", alerts(800).single().id)
        assertEquals("food:2026-10:100", alerts(1000).single().id)
        assertEquals(alerts(1000).single().id, alerts(1200).single().id)
        val largeHistory = FinanceData(budgets = listOf(budget), transactions = listOf(expense("a", Long.MAX_VALUE), expense("b", Long.MAX_VALUE)))
        assertEquals("food:2026-10:100", budgetAlerts(largeHistory, "2026-10").single().id)
    }
}
