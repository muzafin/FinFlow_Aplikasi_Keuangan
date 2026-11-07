package com.clonical.kasiva

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.clonical.kasiva.core.model.*
import com.clonical.kasiva.core.ui.KasivaTheme
import com.clonical.kasiva.feature.transaction.TransactionEditor
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Form Tambah Transaksi bergaya Ollo: semua kontrol tetap berfungsi. Data terisolasi, tidak menyentuh data asli. */
class TransactionEditorParityDeviceTest {
    @get:Rule val ui = createComposeRule()

    private val data = FinanceData(
        profile = Profile("tx-qa", "Uji", "uji@example.invalid"),
        accounts = listOf(Account(id = "tx-cash", name = "Kas", type = "cash", currency = "IDR")),
        categories = defaultCategories + listOf(
            Category("food-lunch", "Makan siang", "expense", parentId = "food"),
            Category("food-dinner", "Makan malam", "expense", parentId = "food")),
    )

    @Test fun olloStyleFormSavesAndEveryControlWorks() {
        var saved: Transaction? = null
        var scanned = false
        ui.setContent {
            CompositionLocalProvider(LocalPreferences provides AppPreferences(theme = "dark", rememberCategory = false)) {
                KasivaTheme { TransactionEditor(null, data, false, {}, { saved = it }, null, onScan = { scanned = true }) }
            }
        }
        ui.waitForIdle()
        // Tombol kamera memanggil AI Scan.
        ui.onNodeWithTag("transaction-scan").performClick()
        ui.runOnIdle { assertTrue(scanned) }
        // Tab jenis bergaya segmen tetap memakai semantik "selected".
        ui.onNode(hasText("Pengeluaran") and isSelected()).assertExists()
        // Chip subkategori: pilih lalu ketuk lagi untuk batal.
        ui.onNodeWithText("Makan & minum").performClick()
        ui.onNodeWithTag("subcategory-food-lunch").performClick()
        ui.onNodeWithTag("subcategory-food-lunch").assertIsSelected()
        ui.onNodeWithTag("subcategory-food-lunch").performClick()
        ui.onNodeWithTag("subcategory-food-lunch").assertIsNotSelected()
        ui.onNodeWithTag("subcategory-food-dinner").performClick()
        // Tahan ⌫ mengosongkan nominal.
        ui.onNodeWithTag("key-9").performClick()
        ui.onNodeWithTag("key-backspace").performTouchInput { longClick() }
        ui.onNodeWithTag("transaction-amount").assertTextEquals("0")
        // Tombol kalkulator membuka dialog dan hasilnya dipakai.
        ui.onNodeWithTag("key-calculator").performClick()
        ui.onNodeWithText("Kalkulator").assertExists()
        ui.onNode(hasSetTextAction() and hasText("Contoh", substring = true)).performTextReplacement("20000+5000")
        ui.onNodeWithText("Gunakan Hasil").performClick()
        ui.onNodeWithTag("transaction-amount").assert(hasText("25.000", substring = true))
        // Tombol tag membuka dialog tag.
        ui.onNodeWithTag("transaction-form").performScrollToNode(hasTestTag("transaction-tags"))
        ui.onNodeWithTag("transaction-tags").performClick()
        ui.onNodeWithText("Tag Transaksi").assertExists()
        ui.onNode(hasSetTextAction() and hasText("Tag baru", substring = true)).performTextInput("kantor")
        ui.onNodeWithText("Tambahkan").performClick()
        ui.onNodeWithText("Selesai").performClick()
        // Simpan.
        ui.onNodeWithTag("key-confirm").performClick()
        ui.runOnIdle {
            val tx = requireNotNull(saved)
            assertEquals(2_500_000L, tx.amountMinor)
            assertEquals("food", tx.categoryId)
            assertEquals("food-dinner", tx.subcategoryId)
            assertEquals(listOf("kantor"), tx.tags)
            assertEquals("tx-cash", tx.accountId)
        }
    }
}
