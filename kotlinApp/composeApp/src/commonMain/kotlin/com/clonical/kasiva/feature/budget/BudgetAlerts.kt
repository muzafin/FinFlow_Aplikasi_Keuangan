package com.clonical.kasiva.feature.budget

import com.clonical.kasiva.core.model.FinanceData
import com.clonical.kasiva.core.util.*
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.i18n.*

data class BudgetAlert(val id: String, val title: String, val message: String)

fun budgetAlerts(data: FinanceData, month: String, recordingDay: Int = 1): List<BudgetAlert> = data.budgets
    .filter { (it.period=="monthly" && it.month == month || it.period=="weekly" && today().toString() in budgetPeriod(it,recordingDay)) && it.limitMinor > 0 }
    .mapNotNull { budget ->
        val spent = data.reportTransactions().asSequence().filter {
            !it.scheduled && it.type == "expense" && it.amountMinor > 0 && it.currency == budget.currency &&
                it.categoryId == budget.categoryId && it.date in budgetPeriod(budget,recordingDay)
        }.fold(0L) { sum, transaction ->
            // Peringatan hanya membutuhkan batas 100%; pembatasan ini menghindari overflow riwayat besar.
            if (transaction.amountMinor >= budget.limitMinor - sum) budget.limitMinor else sum + transaction.amountMinor
        }
        val level = when {
            spent >= budget.limitMinor -> 100
            spent >= budget.limitMinor - budget.limitMinor / 5 -> 80
            else -> return@mapNotNull null
        }
        BudgetAlert("${budget.id}:$month:$level",
            if (level == 100) MR.strings.dash_alert_budget_reached.text() else MR.strings.dash_alert_budget_near.text(),
            // Nominal dan kategori tidak ditampilkan di layar kunci perangkat.
            MR.strings.dash_alert_budget_message.text())
    }
