package com.logikawan.finflow

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.logikawan.finflow.core.model.*
import com.logikawan.finflow.core.ui.FinFlowTheme
import com.logikawan.finflow.core.util.today
import com.logikawan.finflow.feature.budget.BudgetScreen
import com.logikawan.finflow.feature.goal.GoalScreen
import com.logikawan.finflow.feature.investment.InvestmentScreen
import com.logikawan.finflow.feature.planning.PlanningActions
import com.logikawan.finflow.feature.planning.PlanningScreen
import com.logikawan.finflow.feature.transaction.RecurringListScreen
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Menu Beranda bergaya Ollo: aksi ⋮ (yang di App tampil di header) dan tombol + tetap berfungsi. Data terisolasi. */
class MenuParityDeviceTest {
    @get:Rule val ui = createComposeRule()

    private val data = FinanceData(
        profile = Profile("menu-qa", "Uji", "uji@example.invalid"),
        accounts = listOf(Account(id = "m-cash", name = "Kas", type = "cash", currency = "IDR")),
        categories = defaultCategories, featuresReady = true,
    )

    private fun theme(content: @Composable () -> Unit) = ui.setContent {
        CompositionLocalProvider(LocalPreferences provides AppPreferences(theme = "dark")) { FinFlowTheme(content = content) }
    }

    @Test fun budgetPeriodMovesToMenuAndAddWorks() {
        var added = false
        val month = today().toString().take(7)
        theme { BudgetScreen(data, month, {}, {}, { added = true }) }
        ui.onNodeWithText("Bulanan").assertDoesNotExist()
        ui.onNodeWithText("Sisa bulan ini").assertExists()
        ui.onNodeWithContentDescription("Menu").performClick()
        ui.onNodeWithText("Anggaran Mingguan").performClick()
        ui.onNodeWithText("Sisa minggu ini").assertExists()
        ui.onNodeWithTag("budget-fab").performClick()
        ui.runOnIdle { assertTrue(added) }
    }

    @Test fun goalArchiveMovesToMenuAndAddWorks() {
        var added = false
        theme { GoalScreen(data, {}, { added = true }) }
        ui.onNodeWithText("Aktif").assertDoesNotExist()
        ui.onNodeWithText("Belum ada target").assertExists()
        ui.onNodeWithContentDescription("Menu").performClick()
        ui.onNodeWithText("Tampilkan Arsip").performClick()
        ui.onNodeWithText("Belum ada target diarsipkan").assertExists()
        ui.onNodeWithContentDescription("Buat target baru").performClick()
        ui.runOnIdle { assertTrue(added) }
    }

    @Test fun billsTabsMenuAndAddWork() {
        var addedBill = false
        val actions = PlanningActions(bill = { if (it == null) addedBill = true }, payBill = {}, debt = {}, payDebt = {}, wish = {}, fulfillWish = {},
            card = {}, note = {}, claim = {}, receiveClaim = {}, recurring = {}, processRecurring = {}, manualScheduled = {}, addExpense = {}, reverse = {})
        theme { PlanningScreen("Tagihan", data, false, actions) }
        ui.onNodeWithText("SEMUA BERES!").assertExists()
        ui.onNodeWithText("Lunas").performClick()
        ui.onNodeWithText("Lunas").assertIsSelected()
        ui.onNodeWithText("BELUM ADA PEMBAYARAN").assertExists()
        ui.onNodeWithContentDescription("Menu").performClick()
        ui.onNodeWithText("Tampilkan Arsip").assertExists()
        ui.onNodeWithText("Tampilkan Arsip").performClick()
        ui.onNodeWithContentDescription("Tambah tagihan").performClick()
        ui.runOnIdle { assertTrue(addedBill) }
    }

    @Test fun recurringMenuAndAddWork() {
        var processed = false
        var calendar = false
        var added = false
        theme { RecurringListScreen(data, false, { added = true }, {}, {}, { processed = true }, { calendar = true }) }
        ui.onNodeWithText("Proses jadwal").assertDoesNotExist()
        ui.onNodeWithContentDescription("Menu").performClick()
        ui.onNodeWithText("Proses jadwal").performClick()
        ui.onNodeWithContentDescription("Menu").performClick()
        ui.onNodeWithText("Kalender terjadwal").performClick()
        ui.onNodeWithTag("recurring-page").performScrollToNode(hasTestTag("recurring-add"))
        ui.onNodeWithTag("recurring-add").performClick()
        ui.runOnIdle { assertTrue(processed); assertTrue(calendar); assertTrue(added) }
    }
    @Test fun pageTwoMenusTabsAndAddWork() {
        val added = mutableListOf<String>()
        val actions = PlanningActions(bill = {}, payBill = {}, debt = { if (it == null) added += "debt" }, payDebt = {}, wish = { if (it == null) added += "wish" }, fulfillWish = {},
            card = { if (it == null) added += "card" }, note = { if (it == null) added += "note" }, claim = { if (it == null) added += "claim" }, receiveClaim = {},
            recurring = {}, processRecurring = {}, manualScheduled = {}, addExpense = { added += "expense" }, reverse = {})
        var page by mutableStateOf("Utang")
        theme { PlanningScreen(page, data, false, actions) }
        // Utang: ringkasan dua kolom + tab
        ui.onNodeWithTag("debt-overview").assertExists()
        ui.onNodeWithText("Kamu bebas hutang!").assertExists()
        ui.onNode(hasText("Piutang Saya") and hasClickAction()).performClick()
        ui.onNodeWithText("Belum ada yang berutang padamu").assertExists()
        ui.onNodeWithContentDescription("Tambah hutang atau piutang").performClick()
        // Keinginan
        ui.runOnIdle { page = "Keinginan" }
        ui.onNodeWithText("Total Nilai Keinginan").assertExists()
        ui.onNodeWithText("Tercapai").performClick()
        ui.onNodeWithText("Belum ada keinginan tercapai").assertExists()
        ui.onNodeWithContentDescription("Tambah Keinginan").performClick()
        // Kartu
        ui.runOnIdle { page = "Kartu" }
        ui.onNodeWithText("E-Wallet").performClick()
        ui.onNodeWithText("Belum ada e-wallet").assertExists()
        ui.onNodeWithContentDescription("Tambah Kartu / Rekening").performClick()
        // Catatan
        ui.runOnIdle { page = "Catatan" }
        ui.onNodeWithText("Buat bundle pertamamu!").assertExists()
        ui.onNodeWithText("Catatan", substring = false).performClick()
        ui.onNodeWithText("Belum ada catatan").assertExists()
        ui.onNodeWithContentDescription("Bundle Baru").performClick()
        // Reimburse (belum ada pengeluaran → tombol mencatat pengeluaran)
        ui.runOnIdle { page = "Reimburse" }
        ui.onNodeWithText("Diterima").performClick()
        ui.onNodeWithText("Belum ada penerimaan").assertExists()
        ui.onNodeWithContentDescription("Catat Pengeluaran").performClick()
        ui.runOnIdle { assertEquals(listOf("debt", "wish", "card", "note", "expense"), added) }
    }

    @Test fun investmentEmptyAddWorks() {
        var added = false
        theme { InvestmentScreen(data, {}, { added = true }) }
        ui.onNodeWithText("Total Nilai Aset").assertExists()
        ui.onNodeWithTag("investment-add").performClick()
        ui.runOnIdle { assertTrue(added) }
    }
}
