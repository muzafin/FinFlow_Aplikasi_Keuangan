package com.clonical.kasiva

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.clonical.kasiva.core.model.*
import com.clonical.kasiva.core.ui.KasivaTheme
import com.clonical.kasiva.core.util.today
import com.clonical.kasiva.feature.transaction.Transactions
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Riwayat Transaksi bergaya "Transaction Log" Ollo: cari, filter, urutan, atur ulang. Data terisolasi. */
class TransactionLogParityDeviceTest {
    @get:Rule val ui = createComposeRule()

    private val day = today().toString()
    private val data = FinanceData(
        profile = Profile("log-qa", "Uji", "uji@example.invalid"),
        accounts = listOf(Account(id = "l-cash", name = "Kas", type = "cash", currency = "IDR")),
        categories = defaultCategories,
        transactions = listOf(
            Transaction(id = "l1", type = "income", amountMinor = 600_000_000, currency = "IDR", accountId = "l-cash", categoryId = "salary", title = "Gaji", date = day),
            Transaction(id = "l2", type = "expense", amountMinor = 4_500_000, currency = "IDR", accountId = "l-cash", categoryId = "food", title = "Makan siang", date = day, tags = listOf("kantor")),
            Transaction(id = "l3", type = "expense", amountMinor = 2_500_000, currency = "IDR", accountId = "l-cash", categoryId = "transport", title = "Ojek", date = day),
        ),
    )

    @Test fun searchFiltersSortAndReset() {
        var opened: Transaction? = null
        ui.setContent {
            CompositionLocalProvider(LocalPreferences provides AppPreferences(theme = "dark")) {
                KasivaTheme { Transactions(data, day.take(7), {}, { opened = it }) }
            }
        }
        ui.waitForIdle()
        listOf("l1", "l2", "l3").forEach { ui.onNodeWithTag("log-$it").assertExists() }
        // Cari
        ui.onNodeWithTag("transaction-search").performTextInput("Ojek")
        ui.onNodeWithTag("log-l3").assertExists(); ui.onNodeWithTag("log-l1").assertDoesNotExist()
        ui.onNodeWithTag("transaction-search").performTextClearance()
        // Jenis: Pengeluaran
        ui.onNodeWithTag("filter-type").performClick(); ui.onNodeWithText("Pengeluaran").performClick()
        ui.onNodeWithTag("log-l1").assertDoesNotExist(); ui.onNodeWithTag("log-l2").assertExists()
        // Tag
        ui.onNodeWithTag("filter-tag").performClick(); ui.onNodeWithText("#kantor").performClick()
        ui.onNodeWithTag("log-l3").assertDoesNotExist(); ui.onNodeWithTag("log-l2").assertExists()
        // Atur ulang lewat ⋮
        ui.onNodeWithTag("transaction-log-menu").performClick(); ui.onNodeWithText("Atur ulang filter").performClick()
        ui.onNodeWithTag("log-l1").assertExists()
        // Urutan nominal terkecil → Ojek di atas
        ui.onNodeWithTag("filter-sort").performClick(); ui.onNodeWithText("Terkecil").performClick()
        val tops = listOf("l3", "l2", "l1").map { ui.onNodeWithTag("log-$it").fetchSemanticsNode().boundsInRoot.top }
        assertTrue("urutan nominal naik", tops[0] < tops[1] && tops[1] < tops[2])
        // Ketuk baris membuka detail
        ui.onNodeWithTag("log-l2").performClick()
        ui.runOnIdle { assertEquals("l2", opened?.id) }
    }
}
