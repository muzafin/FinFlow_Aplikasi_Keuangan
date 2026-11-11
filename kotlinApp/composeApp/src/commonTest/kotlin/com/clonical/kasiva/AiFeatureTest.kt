package com.clonical.kasiva

import com.clonical.kasiva.app.navigation.Editor
import com.clonical.kasiva.core.model.*
import com.clonical.kasiva.core.util.today
import com.clonical.kasiva.feature.ai.*
import kotlin.test.*

class AiFeatureTest {
    private val data = FinanceData(
        profile = Profile("u", "Arifin", "a@example.test", "IDR"),
        accounts = listOf(Account("cash", "Kas", currency = "IDR"), Account("usd", "Dolar", currency = "USD"),
            Account("old", "Lama", archived = true)),
        categories = listOf(Category("food", "Makan", "expense"), Category("other_expense", "Lainnya", "expense"),
            Category("salary", "Gaji", "income")),
    )

    @Test fun draftUsesWalletCurrencyDigitsAndFallsBackToValidIds() {
        val tx = assertNotNull(AiDraft("expense", 12.5, null, "Kopi", "food", "usd").toTransaction(data, "cash"))
        assertEquals(1250L, tx.amountMinor)
        assertEquals("USD", tx.currency)
        val fallback = assertNotNull(AiDraft("expense", 20000.0, null, "", "salary", "old", date = "kemarin").toTransaction(data, "cash"))
        assertEquals("cash", fallback.accountId)
        assertEquals("other_expense", fallback.categoryId) // kategori pemasukan tidak boleh dipakai untuk pengeluaran
        assertEquals(today().toString(), fallback.date)
        assertEquals("Pengeluaran", fallback.title)
        assertNull(AiDraft("income", 0.0, title = "Nol").toTransaction(data, "cash"))
    }

    @Test fun transferWithoutValidTargetBecomesExpenseDraft() {
        val transfer = assertNotNull(AiDraft("transfer", 100.0, title = "Pindah", walletId = "cash", toWalletId = "usd").toTransaction(data, "cash"))
        assertEquals("transfer", transfer.type); assertEquals("usd", transfer.toAccountId); assertEquals(10000L, transfer.receivedAmountMinor)
        val broken = assertNotNull(AiDraft("transfer", 100.0, title = "Pindah", walletId = "cash", toWalletId = "cash").toTransaction(data, "cash"))
        assertEquals("expense", broken.type); assertNull(broken.toAccountId)
    }

    @Test fun receiptNoteFollowsUserOrderAndToggles() {
        val details = ReceiptDetails(listOf(ReceiptItem("Indomie Goreng", 5.0, 17500.0)), location = "Indomaret Solo", tax = 4318.0, phone = "0800")
        val settings = ScanSettings(fields = listOf(ScanNoteField("location", true), ScanNoteField("items", true), ScanNoteField("tax", false), ScanNoteField("phone", false)))
        val note = receiptNote(details, settings, "IDR")
        assertTrue(note.startsWith("Lokasi: Indomaret Solo\nIndomie Goreng x5: "))
        assertFalse(note.contains("Pajak")); assertFalse(note.contains("Telepon"))
    }

    @Test fun freeLimitsBlockOnlyNewItemsPastTheLimit() {
        val wallets = data.copy(accounts = (1..FreeLimits.WALLETS).map { Account("w$it", "Dompet $it") })
        assertNotNull(freeLimitMessage(Editor.Wallet(), wallets))
        assertNull(freeLimitMessage(Editor.Wallet(wallets.accounts.first()), wallets))
        assertNull(freeLimitMessage(Editor.Wallet(), data))
        // Selama langganan belum dibuka, batas gratis tidak boleh menghalangi pengguna.
        if (!PREMIUM_ENABLED) assertNull(freeLimitReached(Editor.Wallet(), wallets))
        val wishes = data.copy(wishes = (1..FreeLimits.WISHES).map { Wish(title = "W$it", targetMinor = 1) })
        assertNotNull(freeLimitMessage(Editor.WishForm(null), wishes))
    }

    @Test fun assistantSummaryContainsTotalsButNotIndividualTitles() {
        val month = today().toString()
        val withTx = data.copy(transactions = listOf(Transaction("t1", "expense", 5_000_000, "IDR", "cash", "food", "Rahasia warung", date = month),
            Transaction("t2", "income", 10_000_000, "IDR", "cash", "salary", "Gaji kantor", date = month)))
        val summary = assistantSummary(withTx)
        assertTrue(summary.contains("Makan")); assertTrue(summary.contains("Gaji"))
        assertFalse(summary.contains("Rahasia warung"))
        assertTrue(summary.length <= 12000)
    }
}
