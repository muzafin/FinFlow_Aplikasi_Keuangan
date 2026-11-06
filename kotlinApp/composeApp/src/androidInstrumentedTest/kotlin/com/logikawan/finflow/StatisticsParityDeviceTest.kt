package com.logikawan.finflow

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.logikawan.finflow.core.model.*
import com.logikawan.finflow.core.ui.FinFlowTheme
import com.logikawan.finflow.core.util.today
import com.logikawan.finflow.feature.report.Reports
import java.io.File
import java.io.FileOutputStream
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Membandingkan halaman Statistik FinFlow dengan susunan Statistik Ollo memakai data contoh terisolasi. */
class StatisticsParityDeviceTest {
    @get:Rule val ui = createComposeRule()

    private fun fixture(): FinanceData {
        val day = today()
        val yesterday = day.plus(DatePeriod(days = -1))
        val earlier = day.plus(DatePeriod(days = -2))
        val cash = Account(id = "stat-cash", name = "Tunai", type = "cash", currency = "IDR", openingBalanceMinor = 1_200_000)
        val bank = Account(id = "stat-bank", name = "BCA", type = "bank", currency = "IDR", openingBalanceMinor = 3_500_000)
        fun tx(id: String, type: String, amount: Long, account: Account, category: String, title: String, date: LocalDate) =
            Transaction(id = id, type = type, amountMinor = amount, currency = account.currency, accountId = account.id,
                categoryId = category, title = title, date = date.toString(), createdAt = date.toEpochDays().toLong() * 86_400_000L + 36_000_000L)
        return FinanceData(
            profile = Profile("stat-user", "Muhammad", "muhammad@example.invalid"),
            accounts = listOf(cash, bank),
            categories = defaultCategories,
            transactions = listOf(
                tx("stat-1", "income", 6_500_000, bank, "salary", "Gaji November", day),
                tx("stat-2", "expense", 45_000, cash, "food", "Makan siang", day),
                tx("stat-3", "expense", 25_000, cash, "transport", "Ojek online", day),
                tx("stat-4", "expense", 150_000, bank, "shopping", "Belanja mingguan", yesterday),
                tx("stat-5", "expense", 320_000, bank, "bills", "Listrik dan air", earlier),
            ),
            budgets = listOf(Budget("stat-budget", "food", 1_500_000, "IDR", day.toString().take(7))),
        )
    }

    @Test fun statisticsFollowOlloStructureInLightTheme() {
        var ordered = false
        var preferencesTapped = false
        var exported: List<Transaction>? = null
        val data = fixture()
        ui.setContent {
            CompositionLocalProvider(LocalPreferences provides AppPreferences(theme = "light")) {
                FinFlowTheme {
                    Reports(data, data.transactions.first().date.take(7), {}, {},
                        onSectionOrder = { ordered = true }, onPreferences = { preferencesTapped = true },
                        exportFiltered = { exported = it })
                }
            }
        }
        ui.mainClock.advanceTimeBy(600)
        ui.waitForIdle()
        ui.onNodeWithTag("statistics").assertExists()
        ui.onNodeWithText("Statistik").assertIsDisplayed()
        ui.onNodeWithTag("stats-type-expense").assertExists() // Ollo: tanpa label "Total Pengeluaran"
        ui.onNodeWithTag("statistics-total").assertExists()
        screenshot("statistics-ollo-light-top")

        ui.onNodeWithTag("statistics-menu").performClick()
        ui.onNodeWithText("Ekspor CSV/PDF").assertExists()
        ui.onNodeWithText("Urutan Bagian Statistik").assertExists()
        ui.onNodeWithText("Preferensi Tampilan").assertExists()
        ui.onNodeWithText("Urutan Bagian Statistik").performClick()
        ui.runOnIdle { assertTrue(ordered) }

        ui.onNodeWithTag("statistics-menu").performClick()
        ui.onNodeWithText("Preferensi Tampilan").performClick()
        ui.runOnIdle { assertTrue(preferencesTapped) }

        ui.onNodeWithTag("statistics-menu").performClick()
        ui.onNodeWithText("Ekspor CSV/PDF").performClick()
        ui.runOnIdle { assertNotNull(exported) }

        ui.onNodeWithTag("statistics-info").performClick()
        ui.onNodeWithText("Tentang statistik").assertExists()
        ui.onNodeWithText("Mengerti").performClick()
        ui.onNodeWithText("Tentang statistik").assertDoesNotExist()

        scrollToText("Perbandingan Bulanan")
        screenshot("statistics-ollo-light-comparison")
        scrollToText("Peta Aktivitas")
        screenshot("statistics-ollo-light-heatmap")
        scrollToText("Analisis Lanjutan")
        screenshot("statistics-ollo-light-advanced")
    }

    @Test fun statisticsKeepOlloLayoutInDarkTheme() {
        val data = fixture()
        ui.setContent {
            CompositionLocalProvider(LocalPreferences provides AppPreferences(theme = "dark")) {
                FinFlowTheme { Reports(data, data.transactions.first().date.take(7), {}, {}) }
            }
        }
        ui.mainClock.advanceTimeBy(600)
        ui.waitForIdle()
        ui.onNodeWithTag("statistics").assertExists()
        ui.onNodeWithText("Statistik").assertIsDisplayed()
        ui.onNodeWithTag("stats-type-expense").assertExists() // Ollo: tanpa label "Total Pengeluaran"
        screenshot("statistics-ollo-dark-top")
    }

    @Test fun statisticsOlloControlsWork() {
        val data = fixture()
        val prefs = AppPreferences(theme = "dark")
        ui.setContent {
            CompositionLocalProvider(LocalPreferences provides prefs) {
                FinFlowTheme { Reports(data, data.transactions.first().date.take(7), {}, {}) }
            }
        }
        ui.mainClock.advanceTimeBy(600)
        ui.waitForIdle()
        // Default Pengeluaran seperti Ollo; ganti ke Pemasukan mengubah total donut.
        ui.onNodeWithTag("statistics-total").assertTextEquals(com.logikawan.finflow.core.util.formatBalance(540_000, "IDR", prefs))
        ui.onNodeWithTag("stats-type-income").performClick()
        ui.waitForIdle()
        ui.onNodeWithTag("statistics-total").assertTextEquals(com.logikawan.finflow.core.util.formatBalance(6_500_000, "IDR", prefs))
        ui.onNodeWithTag("stats-type-expense").performClick()
        // Bandingkan dapat diaktifkan tanpa error.
        scrollToText("Perbandingan Bulanan")
        ui.onAllNodesWithTag("statistics-compare").onFirst().performClick()
        ui.waitForIdle()
        // Pencarian log hanya menyisakan transaksi yang cocok.
        scrollToText("Log Transaksi")
        ui.onNodeWithTag("statistics-search").performTextInput("Ojek")
        ui.waitForIdle()
        ui.onNodeWithText("Ojek online").assertExists()
        ui.onNodeWithText("Makan siang").assertDoesNotExist()
        // Kalkulator cicilan: 12.000 tanpa bunga selama 12 bulan = 1.000 per bulan.
        scrollToText("Kalkulator keuangan")
        ui.onNodeWithTag("statistics-calculator").performClick()
        ui.onNodeWithTag("statistics").performScrollToNode(hasText("Cicilan pinjaman"))
        ui.onNodeWithText("Cicilan pinjaman").performClick()
        ui.onNodeWithTag("statistics").performScrollToNode(hasContentDescription("Tenor (bulan)"))
        ui.onNodeWithContentDescription("Jumlah pinjaman (IDR)").performTextInput("12000")
        ui.onNodeWithContentDescription("Bunga per tahun (%)").performTextInput("0")
        ui.onNodeWithContentDescription("Tenor (bulan)").performTextInput("12")
        ui.waitForIdle()
        ui.onNodeWithTag("statistics").performScrollToNode(hasText("per bulan", substring = true))
        ui.onNodeWithText("Cicilan ${com.logikawan.finflow.core.util.formatBalance(100_000, "IDR", prefs)} per bulan.").assertExists()
        screenshot("statistics-ollo-controls")
    }

    private fun scrollToText(text: String) {
        ui.onNodeWithTag("statistics").performScrollToIndex(0)
        ui.onNodeWithTag("statistics").performScrollToNode(hasText(text))
        ui.mainClock.advanceTimeBy(300)
        ui.waitForIdle()
    }

    private fun screenshot(name: String) {
        ui.mainClock.advanceTimeBy(500)
        ui.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "statistics-qa").apply { mkdirs() }
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        checkNotNull(bitmap) { "Device screenshot unavailable" }
        FileOutputStream(File(directory, name + ".png")).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
