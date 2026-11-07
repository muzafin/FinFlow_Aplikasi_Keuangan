package com.clonical.kasiva

import com.clonical.kasiva.core.model.Account
import com.clonical.kasiva.core.model.FinanceData
import com.clonical.kasiva.core.model.Transaction
import com.clonical.kasiva.core.model.defaultCategories
import com.clonical.kasiva.core.util.checkedAdd
import com.clonical.kasiva.core.util.decimalAmount
import com.clonical.kasiva.core.util.minorAmount
import com.clonical.kasiva.feature.importdata.CsvImport

import kotlin.test.*

class FinanceImportTest {
    private val seed = FinanceData(
        accounts = listOf(Account(id = "cash", name = "Kas"), Account(id = "bank", name = "Bank", currency = "USD")),
        categories = defaultCategories,
    )

    @Test
    fun moneyKeepsExactMinorUnitsAndRejectsExtraPrecision() {
        assertEquals(123450L, minorAmount("1234.50", "IDR"))
        assertEquals(1234L, minorAmount("1.234", "KWD"))
        assertEquals(1234L, minorAmount("1234.00", "JPY"))
        assertEquals("1.234", decimalAmount(1234, "KWD"))
        assertFailsWith<IllegalArgumentException> { minorAmount("1.001", "USD") }
        assertFailsWith<IllegalArgumentException> { checkedAdd(Long.MAX_VALUE, 1) }
    }

    @Test
    fun csvPreservesQuotedCommasEscapedQuotesAndMultilineNotes() {
        val parsed = CsvImport.parse("\uFEFFTitle,Description\r\n\"Makan, siang\",\"Catatan \"\"penting\"\"\nbaris kedua\"\r\n")
        assertEquals(listOf("Makan, siang", "Catatan \"penting\"\nbaris kedua"), parsed[1])
        assertFailsWith<IllegalArgumentException> { CsvImport.parse("Title\n\"Belum ditutup") }
    }

    @Test
    fun ivyGroupingAndCrossCurrencyTransfersKeepLedgerBalances() {
        val csv = CsvImport.headers.joinToString(",") + "\n" +
            "2026-10-26,Siang,Makan & minum,Kas,\"1,234.50\",IDR,EXPENSE,,,,,,,,ivy-001\n" +
            "2026-10-27,Transfer,,Kas,0.00,IDR,TRANSFER,\"2,000.00\",IDR,Bank,1.00,USD,,,ivy-002\n"
        val plan = CsvImport.preview(csv, "ivy", seed)
        assertTrue(plan.errors.isEmpty(), plan.errors.joinToString())
        assertEquals(2, plan.transactions.size)
        val data = seed.copy(transactions = plan.transactions)
        assertEquals(-323450L, data.balance(seed.accounts[0]))
        assertEquals(100L, data.balance(seed.accounts[1]))
    }

    @Test
    fun reimportingKasivaExportSkipsManualAndIvyRecords() {
        val manual = Transaction(id = "manual-1", type = "income", amountMinor = 500000, currency = "IDR",
            accountId = "cash", categoryId = "salary", title = "Gaji")
        val imported = manual.copy(id = "ivy_303031", source = "ivy", sourceId = "001")
        val data = seed.copy(transactions = listOf(manual, imported))
        val csv = CsvImport.export(data)
        val repeated = CsvImport.preview(csv, "finflow", data)
        assertTrue(repeated.errors.isEmpty(), repeated.errors.joinToString())
        assertEquals(2, repeated.duplicateCount)
        assertTrue(repeated.transactions.isEmpty())
        val restored = CsvImport.preview(csv, "finflow", seed)
        assertTrue(restored.errors.isEmpty(), restored.errors.joinToString())
        assertEquals(listOf("manual-1", "ivy_303031"), restored.transactions.map { it.id })
        assertEquals(listOf("manual", "ivy"), restored.transactions.map { it.source })
        assertEquals(listOf(null, "001"), restored.transactions.map { it.sourceId })
    }

    @Test fun managedPaymentsAreDeduplicatedAndCsvRestoreExplainsJournalOnlyScope() {
        val payment=Transaction(id="bill-1-2026-10-29",type="expense",amountMinor=10000,currency="IDR",accountId="cash",categoryId="bills",title="Listrik",source="bill",sourceId="1")
        val existing=seed.copy(transactions=listOf(payment))
        val csv=CsvImport.export(existing)
        val repeated=CsvImport.preview(csv,"finflow",existing)
        assertTrue(repeated.errors.isEmpty());assertEquals(1,repeated.duplicateCount)
        val restored=CsvImport.preview(csv,"finflow",seed)
        assertTrue(restored.errors.isEmpty(),restored.errors.joinToString())
        assertEquals(payment.id,restored.transactions.single().id)
        assertEquals("finflow",restored.transactions.single().source)
        assertTrue(restored.warnings.any{it.contains("transaksi biasa")})
    }
}
