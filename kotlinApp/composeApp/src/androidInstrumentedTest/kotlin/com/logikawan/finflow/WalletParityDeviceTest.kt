package com.logikawan.finflow

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.logikawan.finflow.core.model.*
import com.logikawan.finflow.core.ui.FFColors
import com.logikawan.finflow.core.ui.FinFlowTheme
import com.logikawan.finflow.core.util.today
import com.logikawan.finflow.feature.account.WalletDetail
import com.logikawan.finflow.feature.account.WalletScreen
import java.io.File
import java.io.FileOutputStream
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Membandingkan halaman Dompet dan detail dompet FinFlow dengan susunan Ollo memakai data contoh terisolasi. */
class WalletParityDeviceTest {
    @get:Rule val ui = createComposeRule()

    private fun fixture(): FinanceData {
        val day = today()
        val yesterday = day.plus(DatePeriod(days = -1))
        val earlier = day.plus(DatePeriod(days = -2))
        val cash = Account(id = "wal-cash", name = "Cash", type = "cash", currency = "IDR", openingBalanceMinor = 1_200_000)
        val bank = Account(id = "wal-bank", name = "BCA", type = "bank", currency = "IDR", openingBalanceMinor = 3_500_000)
        val ewallet = Account(id = "wal-ovo", name = "OVO", type = "ewallet", currency = "IDR", openingBalanceMinor = 250_000)
        val savings = Account(id = "wal-savings", name = "Tabungan Laptop", type = "savings", currency = "IDR", openingBalanceMinor = 1_000_000)
        val credit = Account(id = "wal-credit", name = "Kartu Kredit", type = "credit", currency = "IDR", openingBalanceMinor = -500_000)
        val archived = Account(id = "wal-archived", name = "Dompet Lama", type = "cash", currency = "IDR", openingBalanceMinor = 100_000, archived = true)
        fun tx(id: String, type: String, amount: Long, account: Account, category: String, title: String, date: LocalDate) =
            Transaction(id = id, type = type, amountMinor = amount, currency = account.currency, accountId = account.id,
                categoryId = category, title = title, date = date.toString(), createdAt = date.toEpochDays().toLong() * 86_400_000L + 36_000_000L)
        return FinanceData(
            profile = Profile("wallet-user", "Muhammad", "muhammad@example.invalid"),
            accounts = listOf(cash, bank, ewallet, savings, credit, archived),
            categories = defaultCategories,
            transactions = listOf(
                tx("wal-tx-1", "income", 4_000_000, bank, "salary", "Gaji November", day),
                tx("wal-tx-2", "expense", 45_000, cash, "food", "Makan siang", day),
                tx("wal-tx-3", "expense", 25_000, ewallet, "transport", "Ojek online", yesterday),
                tx("wal-tx-4", "expense", 150_000, bank, "shopping", "Belanja mingguan", earlier),
            ),
            budgets = listOf(Budget("wal-budget", "food", 1_500_000, "IDR", day.toString().take(7))),
            goals = listOf(Goal(id = "wal-goal", name = "Laptop Baru", targetMinor = 5_000_000, savedMinor = 1_000_000,
                currency = "IDR", targetDate = "2027-06-15", holdingAccountId = "wal-savings")),
            featuresReady = true,
        )
    }

    @Test fun walletPageFollowsOlloStructureInLightTheme() {
        val data = fixture()
        ui.setContent {
            CompositionLocalProvider(LocalPreferences provides AppPreferences(theme = "light")) {
                FinFlowTheme {
                    Box(Modifier.fillMaxSize().background(FFColors.background)) { WalletScreen(data, {}, {}, {}) }
                }
            }
        }
        ui.mainClock.advanceTimeBy(600)
        ui.waitForIdle()
        ui.onNodeWithTag("wallet-list").assertExists()
        ui.onNodeWithText("Dompet Saya").assertIsDisplayed()
        ui.onNodeWithText("Total Saldo (IDR)").assertExists()
        ui.onNodeWithText("Total Kekayaan").assertExists()
        ui.onNodeWithText("Liabilitas").assertExists()
        ui.onNodeWithText("Tabungan Aktif").assertExists()
        ui.onNodeWithText("Pembayaran Mendatang").assertExists()
        screenshot("wallet-ollo-light")

        ui.onNodeWithTag("wallet-composition").performClick()
        ui.onNodeWithText("Komposisi Uang").assertExists()
        ui.waitForIdle()
        Thread.sleep(450)
        screenshot("wallet-ollo-composition")
        ui.onNodeWithText("Tutup").performClick()
        ui.onNodeWithText("Komposisi Uang").assertDoesNotExist()

        scrollToText("Tunai")
        ui.onNodeWithText("Tunai").assertExists()
        ui.onNodeWithTag("wallet-wal-cash").assertExists()
        scrollToText("Akun Bank")
        ui.onNodeWithText("Akun Bank").assertExists()
        scrollToText("E-Wallet")
        ui.onNodeWithText("E-Wallet").assertExists()
        ui.onNodeWithTag("wallet-wal-ovo").assertExists()
        scrollToText("Target Tabungan")
        ui.onNodeWithText("Target Tabungan").assertExists()

        scrollToText("Dompet Saya")
        ui.onNodeWithTag("wallet-menu").performClick()
        ui.onNodeWithText("Dompet Diarsipkan").assertExists()
        ui.onNodeWithText("Dompet Diarsipkan").performClick()
        ui.onNodeWithText("Menampilkan dompet diarsipkan").assertExists()
        screenshot("wallet-ollo-archived")
    }

    @Test fun walletDetailFollowsOlloStructure() {
        val data = fixture()
        val cash = requireNotNull(data.accounts.find { it.id == "wal-cash" })
        ui.setContent {
            CompositionLocalProvider(LocalPreferences provides AppPreferences(theme = "light")) {
                FinFlowTheme {
                    Box(Modifier.fillMaxSize().background(FFColors.background)) { WalletDetail(cash, data, {}, {}) }
                }
            }
        }
        ui.mainClock.advanceTimeBy(600)
        ui.waitForIdle()
        ui.onNodeWithTag("wallet-detail").assertExists()
        ui.onNodeWithText("Saldo Saat Ini").assertIsDisplayed()
        ui.onNodeWithText("Ringkasan Bulanan").assertExists()
        ui.onNodeWithText("Arus Masuk").assertExists()
        ui.onNodeWithText("Arus Keluar").assertExists()
        ui.onNodeWithText("Arus Bersih").assertExists()
        ui.onNodeWithText("Jumlah Transaksi").assertExists()
        screenshot("wallet-ollo-detail")
        ui.onNodeWithTag("wallet-detail").performScrollToNode(hasText("Transaksi Terbaru"))
        ui.onNodeWithText("Transaksi Terbaru").assertExists()
    }

    @Test fun walletPageKeepsOlloLayoutInDarkTheme() {
        val data = fixture()
        ui.setContent {
            CompositionLocalProvider(LocalPreferences provides AppPreferences(theme = "dark")) {
                FinFlowTheme {
                    Box(Modifier.fillMaxSize().background(FFColors.background)) { WalletScreen(data, {}, {}, {}) }
                }
            }
        }
        ui.mainClock.advanceTimeBy(600)
        ui.waitForIdle()
        ui.onNodeWithTag("wallet-list").assertExists()
        ui.onNodeWithText("Dompet Saya").assertIsDisplayed()
        ui.onNodeWithText("Total Saldo (IDR)").assertExists()
        screenshot("wallet-ollo-dark")
    }

    @Test fun walletHeaderAddAndHundredPercentChange() {
        var added = false
        val cash = Account(id = "new-cash", name = "Kas", type = "cash", currency = "IDR", openingBalanceMinor = 0)
        val data = FinanceData(profile = Profile("w2", "Uji", "u@example.invalid"), accounts = listOf(cash), categories = defaultCategories,
            transactions = listOf(Transaction(id = "w2-1", type = "income", amountMinor = 600_000_000, currency = "IDR", accountId = "new-cash", categoryId = "salary", title = "Gaji", date = today().toString())))
        ui.setContent {
            CompositionLocalProvider(LocalPreferences provides AppPreferences(theme = "dark")) {
                FinFlowTheme { WalletScreen(data, {}, { added = true }, {}) }
            }
        }
        ui.waitForIdle()
        // Ollo: saldo 30 hari lalu 0 → +100%.
        ui.onNodeWithText("+100,0%").assertExists()
        ui.onNodeWithTag("wallet-add").performClick()
        ui.runOnIdle { assertTrue(added) }
        ui.onNodeWithTag("wallet-composition").performClick()
        ui.onNodeWithText("Komposisi Uang").assertExists()
    }

    private fun scrollToText(text: String) {
        ui.onNodeWithTag("wallet-list").performScrollToIndex(0)
        ui.onNodeWithTag("wallet-list").performScrollToNode(hasText(text))
        ui.mainClock.advanceTimeBy(300)
        ui.waitForIdle()
    }

    private fun screenshot(name: String) {
        ui.mainClock.advanceTimeBy(500)
        ui.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "wallet-qa").apply { mkdirs() }
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        checkNotNull(bitmap) { "Device screenshot unavailable" }
        FileOutputStream(File(directory, name + ".png")).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
