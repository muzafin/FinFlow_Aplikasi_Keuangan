package com.clonical.kasiva

import com.clonical.kasiva.core.model.*
import com.clonical.kasiva.core.ui.calendarDates
import com.clonical.kasiva.feature.dashboard.dashboardBalance
import com.clonical.kasiva.feature.dashboard.dashboardShortcuts
import com.clonical.kasiva.feature.planning.recurringOn
import com.clonical.kasiva.feature.settings.bugReportBody
import com.clonical.kasiva.feature.settings.recordingProgress
import kotlinx.datetime.LocalDate
import kotlin.test.*

class HomeDetailsTest {
    private fun tx(id: String, date: String = "2026-11-03", scheduled: Boolean = false) =
        Transaction(id, "income", 1000, "IDR", "cash", "salary", "Gaji", date = date, scheduled = scheduled)

    @Test fun progressIgnoresScheduledFutureAndDuplicateIdsAndUsesCalendarDays() {
        val records = listOf(tx("a", "2026-10-30"), tx("b", "2026-10-31"), tx("c", "2026-11-01"),
            tx("c", "2026-11-01"), tx("scheduled", scheduled = true), tx("future", "2026-11-04"))
        val p = recordingProgress(FinanceData(transactions = records), LocalDate(2026, 11, 2))
        assertEquals(3, p.transactions)
        assertEquals(3, p.activeDays)
        assertEquals(3, p.streak)
        assertEquals(3, p.longestStreak)
        assertEquals(45, p.xp)
        assertEquals(0, recordingProgress(FinanceData(transactions = records), LocalDate(2026, 11, 3)).streak)
        val first = recordingProgress(FinanceData(transactions = listOf(tx("a"))), LocalDate(2026, 11, 3))
        assertEquals(15, first.xp)
        assertEquals(1, first.level)
        assertEquals(15, first.levelXp)
    }

    @Test fun liquidityKeepsCurrenciesAndExclusionsAndDoesNotCountSavingsOrInvestments() {
        val cash = Account("cash", "Tunai", openingBalanceMinor = 1000)
        val savings = Account("savings", "Tabungan", type = "savings", openingBalanceMinor = 2000)
        val data = FinanceData(accounts = listOf(cash, savings,
            Account("investment", "Investasi", type = "investment", openingBalanceMinor = 3000),
            Account("usd", "USD", currency = "USD", openingBalanceMinor = 9000),
            Account("private", "Privat", openingBalanceMinor = 9000, excludedFromTotal = true),
            Account("archive", "Arsip", openingBalanceMinor = 9000, archived = true)), transactions = listOf(tx("a")))
        assertEquals(7000L, dashboardBalance(data, "IDR", false))
        assertEquals(2000L, dashboardBalance(data, "IDR", true))
        assertEquals(9000L, dashboardBalance(data, "USD", true))
    }

    @Test fun calendarIncludesAdjacentMonthsAndRespectsWeekStart() {
        val monday = calendarDates("2026-11", 1)
        assertEquals(42, monday.size)
        assertEquals(LocalDate(2026, 10, 26), monday.first())
        assertEquals(LocalDate(2026, 12, 6), monday.last())
        assertEquals(LocalDate(2026, 11, 1), calendarDates("2026-11", 7).first())
        assertFailsWith<IllegalArgumentException> { calendarDates("2026-11", 0) }
    }

    @Test fun recurringCalendarClampsMonthEndWithoutMovingTheAnchorOrIgnoringEndDate() {
        val rule = RecurringTransaction("r", "Bulanan", amountMinor = 1000, accountId = "cash", categoryId = "bills",
            nextDate = "2026-01-31", startDate = "2026-01-31", anchorDay = 31, endDate = "2026-03-31")
        assertTrue(recurringOn(rule, LocalDate(2026, 2, 28)))
        assertTrue(recurringOn(rule, LocalDate(2026, 3, 31)))
        assertFalse(recurringOn(rule, LocalDate(2026, 3, 28)))
        assertFalse(recurringOn(rule, LocalDate(2026, 4, 30)))
        assertFalse(recurringOn(rule.copy(active = false), LocalDate(2026, 2, 28)))
        assertFalse(recurringOn(rule.copy(interval = 2), LocalDate(2026, 2, 28)))
        assertTrue(recurringOn(rule.copy(frequency = "weekly"), LocalDate(2026, 2, 7)))
    }

    @Test fun reportRequiresDescriptionAndIncludesOnlyProvidedDiagnostics() {
        assertFailsWith<IllegalArgumentException> { bugReportBody("Beranda", "Tinggi", "", "Error", "", "Device") }
        val body = bugReportBody("Berulang", "Tinggi", "Dompet", "Pilihan dompet gagal", "1. Pilih dompet", "POCO · Android 16")
        assertTrue(body.contains("POCO · Android 16"))
        assertTrue(body.contains("1. Pilih dompet"))
        assertEquals(10, dashboardShortcuts.size)
        assertEquals("Notifikasi", dashboardShortcuts.first().second)
        assertEquals("Panduan Aplikasi", dashboardShortcuts.last().second)
    }
}
