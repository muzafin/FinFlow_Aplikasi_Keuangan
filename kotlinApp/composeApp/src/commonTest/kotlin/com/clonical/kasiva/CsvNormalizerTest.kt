package com.clonical.kasiva

import com.clonical.kasiva.core.model.Account
import com.clonical.kasiva.core.model.FinanceData
import com.clonical.kasiva.core.model.defaultCategories
import com.clonical.kasiva.feature.importdata.CsvImport
import kotlin.test.*

/** Ekspor aplikasi pencatat keuangan lain: ringkasan sebelum header, kolom kiri kosong, Pengeluaran/Pemasukan terpisah, catatan kaki. */
class CsvNormalizerTest {
    private val seed = FinanceData(accounts = listOf(Account(id = "cash", name = "Kas")), categories = defaultCategories)

    private val export = """
,,,,,,
,,,,,,
,Tanggal,,01/11/2026 - 30/11/2026,,,
,Pemasukan,,Rp0,,,
,Pengeluaran,,Rp2.233.600,,,
,Saldo,,Rp-2.233.600,,,
,,,,,,
,No,Tanggal,Keterangan,Pengeluaran,Pemasukan,Kategori
,1,01/11/2026,"ayam, cabe",Rp50.000,,Makanan
,2,02/11/2026,belikopi,Rp11.300,,jajan
,3,03/11/2026,gaji,,Rp1.500.000,Gaji
,,,,,,
,File ini dibuat oleh aplikasi catatan keuangan,,,,,
,Download : bit.ly/x,,,,,
""".trimStart()

    @Test fun exportWithSummaryAndSplitColumnsIsMappedLocally() {
        val plan = assertNotNull(CsvImport.previewLocal(export, seed))
        assertEquals(emptyList(), plan.errors)
        assertEquals(3, plan.transactions.size)
        val coffee = plan.transactions.first { it.title == "belikopi" }
        assertEquals("2026-11-02", coffee.date)
        assertEquals(11_300_00L, coffee.amountMinor)
        assertEquals("expense", coffee.type)
        val salary = plan.transactions.first { it.title == "gaji" }
        assertEquals("income", salary.type)
        assertEquals(1_500_000_00L, salary.amountMinor)
        assertEquals("cash", salary.accountId)
    }

    @Test fun unknownLayoutFallsBackToAi() {
        assertNull(CsvImport.previewLocal("foo,bar\n1,2\n", seed))
    }

    // --- Matriks ekspor aplikasi pasaran (disimulasikan dari format publik masing-masing) ---

    @Test fun walletBudgetBakersSemicolonSignedAmounts() {
        val csv = "account;category;currency;amount;ref_currency_amount;type;payment_type;note;date\n" +
            "Kas;Food;IDR;-25000.00;-25000.00;Expenses;Cash;nasi goreng;2026-11-01T08:30:00.000Z\n" +
            "Kas;Salary;IDR;3000000.00;3000000.00;Income;Transfer;gaji;2026-11-02T09:00:00.000Z\n"
        val plan = assertNotNull(CsvImport.previewLocal(csv, seed))
        assertEquals(emptyList(), plan.errors)
        assertEquals(2, plan.transactions.size)
        assertEquals("expense", plan.transactions.first { it.title == "nasi goreng" }.type)
        assertEquals(25_000_00L, plan.transactions.first { it.title == "nasi goreng" }.amountMinor)
        assertEquals("income", plan.transactions.first { it.title == "gaji" }.type)
    }

    @Test fun spendeeStyleIsoDatesAndWalletColumn() {
        val csv = "Date,Wallet,Category name,Amount,Currency,Note,Labels,Author\n" +
            "2026-11-03T12:00:00+07:00,Bank BCA,Transport,-15000,IDR,ojek,,Aku\n" +
            "2026-11-04T12:00:00+07:00,Bank BCA,Salary,500000,IDR,bonus,,Aku\n"
        val plan = assertNotNull(CsvImport.previewLocal(csv, seed))
        assertEquals(emptyList(), plan.errors)
        assertEquals(2, plan.transactions.size)
        assertEquals(1, plan.accounts.size, "dompet 'Bank BCA' dibuat otomatis")
        assertEquals("expense", plan.transactions.first { it.title == "ojek" }.type)
    }

    @Test fun englishSplitColumnsWithAccount() {
        val csv = "Date,Account,Category,Description,Expense,Income\n" +
            "15/11/2026,Kas,Food,lunch,\"1,200.50\",\n" +
            "16/11/2026,Kas,Gift,angpao,,\"50,000.00\"\n"
        val plan = assertNotNull(CsvImport.previewLocal(csv, seed))
        assertEquals(emptyList(), plan.errors)
        assertEquals(120_050L, plan.transactions.first { it.title == "lunch" }.amountMinor)
        assertEquals("income", plan.transactions.first { it.title == "angpao" }.type)
    }

    @Test fun indonesianSemicolonWithRpAndDecimalComma() {
        val csv = "Tanggal;Keterangan;Pengeluaran;Pemasukan;Kategori;Dompet\n" +
            "01/11/2026;bensin;Rp 50.000,00;;Transportasi;Kas\n" +
            "02/11/2026;freelance;;Rp 1.250.000,00;Gaji;Kas\n"
        val plan = assertNotNull(CsvImport.previewLocal(csv, seed))
        assertEquals(emptyList(), plan.errors)
        assertEquals(50_000_00L, plan.transactions.first { it.title == "bensin" }.amountMinor)
        assertEquals(1_250_000_00L, plan.transactions.first { it.title == "freelance" }.amountMinor)
    }

    // --- Skenario gagal ---

    @Test fun emptyFileIsRejectedWithClearError() {
        assertFails { CsvImport.previewLocal("", seed) }
        assertFails { CsvImport.previewLocal("\n\n,,\n", seed) }
    }

    @Test fun headerOnlyAndGarbageFallBackInsteadOfCrashing() {
        assertNull(CsvImport.previewLocal("Tanggal,Keterangan,Jumlah,Kategori\n", seed))
        assertNull(CsvImport.previewLocal("lorem ipsum dolor sit amet", seed))
    }

    @Test fun badRowsAreReportedWithOriginalLineNumberAndGoodRowsStillParse() {
        val csv = "Tanggal,Keterangan,Pengeluaran,Pemasukan,Kategori\n" +
            "01/11/2026,ok,10.000,,Makanan\n" +
            "tanggal-rusak,rusak,5.000,,Makanan\n" +
            "03/11/2026,nominal rusak,abc,,Makanan\n"
        val plan = assertNotNull(CsvImport.previewLocal(csv, seed))
        assertEquals(1, plan.transactions.size)
        assertEquals(2, plan.errors.size, plan.errors.toString())
        assertTrue(plan.errors.any { it.contains("3") } && plan.errors.any { it.contains("4") }, plan.errors.toString())
    }

    @Test fun reimportSameFileIsDeduplicated() {
        val first = assertNotNull(CsvImport.previewLocal(export, seed))
        val again = assertNotNull(CsvImport.previewLocal(export, seed.copy(transactions = first.transactions)))
        assertEquals(0, again.transactions.size)
        assertEquals(3, again.duplicateCount)
    }
}
