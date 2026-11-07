package com.clonical.kasiva

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.clonical.kasiva.core.model.*
import com.clonical.kasiva.core.ui.KasivaTheme
import com.clonical.kasiva.core.model.LocalPreferences
import com.clonical.kasiva.core.util.today
import com.clonical.kasiva.feature.dashboard.Dashboard
import com.clonical.kasiva.core.ui.WalletPicker
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.material3.Surface
import com.clonical.kasiva.core.ui.FFColors
import org.junit.Assert.*
import com.clonical.kasiva.feature.planning.RecurringEditor
import java.io.File
import java.io.FileOutputStream
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.junit.Rule
import org.junit.Test

/** Membandingkan Beranda Kasiva dengan susunan Beranda Ollo memakai data contoh terisolasi. */
class DashboardParityDeviceTest {
    @get:Rule val ui = createComposeRule()

    private fun fixture(): FinanceData {
        val day = today()
        val yesterday = day.plus(DatePeriod(days = -1))
        val earlier = day.plus(DatePeriod(days = -2))
        val cash = Account(id = "par-cash", name = "Cash", type = "cash", currency = "IDR", openingBalanceMinor = 1_200_000)
        val bank = Account(id = "par-bank", name = "BCA", type = "bank", currency = "IDR", openingBalanceMinor = 3_500_000)
        val savings = Account(id = "par-savings", name = "Tabungan", type = "savings", currency = "IDR", openingBalanceMinor = 500_000)
        fun tx(id: String, type: String, amount: Long, account: Account, category: String, title: String, date: LocalDate) =
            Transaction(id = id, type = type, amountMinor = amount, currency = account.currency, accountId = account.id,
                categoryId = category, title = title, date = date.toString(), createdAt = date.toEpochDays().toLong() * 86_400_000L + 36_000_000L)
        return FinanceData(
            profile = Profile("par-user", "Muhammad", "muhammad@example.invalid"),
            accounts = listOf(cash, bank, savings),
            categories = defaultCategories,
            transactions = listOf(
                tx("par-1", "income", 5_000_000, bank, "salary", "Gaji November", day),
                tx("par-2", "expense", 45_000, cash, "food", "Makan siang", day),
                tx("par-3", "expense", 25_000, cash, "transport", "Ojek online", day),
                tx("par-4", "expense", 120_000, bank, "shopping", "Belanja mingguan", yesterday),
                tx("par-5", "expense", 350_000, bank, "bills", "Listrik dan air", earlier),
            ),
            budgets = listOf(Budget("par-budget", "food", 1_500_000, "IDR", day.toString().take(7))),
            goals = listOf(Goal("par-goal", "Laptop Baru", 12_000_000, 2_500_000, "IDR", "2027-06-15")),
        )
    }

    @Test fun dashboardFollowsOlloStructureInLightTheme() {
        var destination: String? = null
        var edited: String? = null
        val data = fixture()
        ui.setContent {
            CompositionLocalProvider(LocalPreferences provides AppPreferences(theme = "light")) {
                KasivaTheme {
                    Dashboard(data, data.transactions.first().date.take(7), {}, { edited = it.id }, "Muhammad") { destination = it }
                }
            }
        }
        ui.mainClock.advanceTimeBy(600)
        ui.waitForIdle()
        ui.onNodeWithTag("dashboard").assertExists()
        ui.onNodeWithText("Total Saldo (IDR)").assertIsDisplayed()
        ui.onNodeWithTag("dashboard-profile").assertExists()
        ui.onNodeWithTag("dashboard-shortcuts").assertExists()
        ui.onNodeWithTag("dashboard-privacy").assertExists()
        ui.onNodeWithText("Dompet Saya").assertExists()
        ui.onNodeWithTag("dashboard-wallet-par-cash").assertExists()
        screenshot("dashboard-ollo-light")
        ui.onNodeWithTag("dashboard").performScrollToNode(hasText("Transaksi Terbaru"))
        ui.onNodeWithText("Transaksi Terbaru").assertExists()
        ui.mainClock.advanceTimeBy(300)
        ui.waitForIdle()
        screenshot("dashboard-ollo-light-transactions")
    }

    @Test fun dashboardKeepsWorkingInDarkTheme() {
        val data = fixture()
        ui.setContent {
            CompositionLocalProvider(LocalPreferences provides AppPreferences(theme = "dark")) {
                KasivaTheme { Dashboard(data, data.transactions.first().date.take(7), {}, {}, "Muhammad") {} }
            }
        }
        ui.mainClock.advanceTimeBy(600)
        ui.waitForIdle()
        ui.onNodeWithTag("dashboard").assertExists()
        ui.onNodeWithText("Total Saldo (IDR)").assertIsDisplayed()
        screenshot("dashboard-ollo-dark")
    }

    @Test fun calendarReplacesSummaryAndLiquidityDoesNotTogglePrivacy() {
        val data=fixture()
        ui.setContent { CompositionLocalProvider(LocalPreferences provides AppPreferences(theme="light")){KasivaTheme { Surface(color=FFColors.background){Dashboard(data,today().toString().take(7),{},{},"Muhammad"){} } }} }
        ui.onNodeWithTag("dashboard-liquidity-toggle").performClick()
        ui.onNodeWithText("Likuid (IDR)").assertIsDisplayed()
        screenshot("dashboard-liquidity")
        ui.onNodeWithText("Kalender",false).performClick()
        ui.onNodeWithTag("transaction-calendar").assertExists()
        ui.onNodeWithTag("balance-card-IDR").assertDoesNotExist()
        ui.onNodeWithText("Menu",false).assertDoesNotExist()
        ui.onNodeWithText("Gaji November",false).assertExists()
        screenshot("dashboard-calendar")
    }

    @Test fun menuPagesSwitchAndShortcutPanelFollowsOllo() {
        val data=fixture()
        var destination:String?=null
        ui.setContent { CompositionLocalProvider(LocalPreferences provides AppPreferences(theme="light")){KasivaTheme { Surface(color=FFColors.background){Dashboard(data,today().toString().take(7),{},{},"Muhammad"){destination=it} } }} }
        ui.mainClock.advanceTimeBy(400)
        ui.waitForIdle()
        ui.onNodeWithText("Anggaran",false).assertExists()
        // Ollo: menu satu baris digeser; ⋮ membuka Tampilan Menu / Susunan Menu.
        ui.onNodeWithTag("dashboard-menu-row").performScrollToNode(hasText("Investasi"))
        ui.onNodeWithText("Investasi").performClick()
        ui.runOnIdle{ assertEquals("Investasi",destination) }
        screenshot("dashboard-menu-page-2")
        ui.onNodeWithTag("dashboard-menu-more").performClick()
        ui.mainClock.advanceTimeBy(200)
        ui.onNodeWithText("Susunan Menu").performClick()
        ui.runOnIdle{ assertEquals("Susunan Menu",destination) }
        ui.onNodeWithText("Lvl 1").performClick()
        ui.runOnIdle{ assertEquals("Aktivitas",destination) }
        ui.onNodeWithTag("dashboard-shortcuts-button").performClick()
        ui.mainClock.advanceTimeBy(300)
        ui.onNodeWithText("Pembaruan").assertIsDisplayed()
        ui.onNodeWithText("New").assertIsDisplayed()
        screenshot("dashboard-shortcuts-panel")
    }

    @Test fun longNameAndLargeIncomeKeepTheirFullText() {
        val name="Muhammad Arifin Nama Lengkap"
        val data=fixture().copy(transactions=listOf(fixture().transactions.first().copy(amountMinor=987_654_321_000_00)))
        ui.setContent { CompositionLocalProvider(LocalPreferences provides AppPreferences(theme="light")){KasivaTheme { Surface(color=FFColors.background){Dashboard(data,today().toString().take(7),{},{},name){} } }} }
        screenshot("dashboard-long-name-large-income")
        ui.onNodeWithText("~ Hai, $name!").assertExists().performSemanticsAction(SemanticsActions.GetTextLayoutResult){get->
            val layouts=mutableListOf<TextLayoutResult>();get(layouts)
            assertTrue(layouts.isNotEmpty())
            assertFalse(layouts.first().hasVisualOverflow)
        }
        ui.onAllNodes(hasText("Rp987.654.321.000,00"),useUnmergedTree=true).fetchSemanticsNodes().also{assertTrue(it.isNotEmpty())}
        ui.onAllNodes(hasText("Rp987.654.321.000,00"),useUnmergedTree=true).apply {
            repeat(fetchSemanticsNodes().size){index->get(index).performSemanticsAction(SemanticsActions.GetTextLayoutResult){get->
                val layouts=mutableListOf<TextLayoutResult>();get(layouts)
                assertFalse("${layouts.first().layoutInput.text}: size=${layouts.first().size}, width=${layouts.first().didOverflowWidth}, height=${layouts.first().didOverflowHeight}",layouts.first().hasVisualOverflow)
            }}
        }
        screenshot("dashboard-long-name-large-income")
    }

    @Test fun walletPickerFiltersAndReturnsTheSelectedWallet() {
        val data=fixture()
        var selected:String?=null
        ui.setContent { CompositionLocalProvider(LocalPreferences provides AppPreferences(theme="light")){KasivaTheme { WalletPicker(data,data.accounts,"par-cash",{}){selected=it} }} }
        ui.onNodeWithTag("wallet-picker").assertExists()
        ui.onNodeWithText("Bank",false).performClick()
        ui.onNodeWithText("Cash",false).assertDoesNotExist()
        ui.onNodeWithText("BCA",false).assertIsDisplayed().performClick()
        ui.runOnIdle{assertEquals("par-bank",selected)}
        screenshot("wallet-picker-bank")
    }

    @Test fun recurringEditorUsesTheSharedWalletPicker() {
        val data=fixture().copy(featuresReady=true)
        ui.setContent { CompositionLocalProvider(LocalPreferences provides AppPreferences(theme="light")){KasivaTheme {
            RecurringEditor(null,data,false,{},{},null)
        }} }
        ui.onNodeWithTag("wallet-choice-Bayar dengan Dompet").performScrollTo().performClick()
        ui.onNodeWithTag("wallet-picker").assertExists()
        screenshot("recurring-wallet-picker")
        ui.onNodeWithText("BCA",false).performClick()
        ui.onNodeWithTag("wallet-picker").assertDoesNotExist()
        ui.onNodeWithText("BCA",false).assertExists()
        screenshot("recurring-selected-wallet")
    }

    private fun screenshot(name: String) {
        ui.mainClock.advanceTimeBy(500)
        ui.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        android.os.SystemClock.sleep(350)
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "dashboard-qa").apply { mkdirs() }
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        checkNotNull(bitmap) { "Device screenshot unavailable" }
        FileOutputStream(File(directory, name + ".png")).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
