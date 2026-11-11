package com.clonical.kasiva

import com.clonical.kasiva.core.model.Account
import com.clonical.kasiva.core.model.FinanceData
import com.clonical.kasiva.core.model.ImportPlan
import com.clonical.kasiva.core.model.Transaction
import com.clonical.kasiva.core.model.defaultCategories
import com.clonical.kasiva.core.util.checkedAdd
import com.clonical.kasiva.core.util.decimalAmount
import com.clonical.kasiva.core.util.minorAmount
import com.clonical.kasiva.feature.ai.AiCsvMapping
import com.clonical.kasiva.feature.ai.AiCsvTypeMapping
import com.clonical.kasiva.feature.importdata.CsvImport
import com.clonical.kasiva.feature.importdata.buildAiPlan
import com.clonical.kasiva.feature.importdata.sources

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

    @Test
    fun sourcesListAllImportersInUiOrder() {
        assertEquals(
            listOf("ai", "ivy", "finflow", "money_manager", "wallet", "spendee", "monefy", "onemoney", "bluecoins", "ktw", "fortunecity", "financisto"),
            CsvImport.sources.map { it.id },
        )
        assertEquals("Deteksi otomatis (AI)", CsvImport.sources.first().label)
        assertEquals("Ivy Wallet CSV", CsvImport.sources[1].label)
        assertEquals("Template Kasiva", CsvImport.sources[2].label)
        assertTrue(CsvImport.sources.all { it.label.isNotBlank() })
    }

    @Test
    fun otherAppExportsMapPositionalColumns() {
        val cases = listOf(
            ImportCase("money_manager", "2026-01-15,Kas,Makan,-,Siang,-,EXPENSE,Catatan,\"1,234.50\",IDR", "expense", 123450, "Siang", "Makan", "Catatan"),
            ImportCase("spendee", "2026-01-15,Kas,EXPENSE,Makan,25000,IDR,Siang", "expense", 2_500_000, "Siang", "Makan", ""),
            ImportCase("onemoney", "2026-01-15,INCOME,Kas,Uang Saku,50000,IDR,,,,Bonus", "income", 5_000_000, "Bonus", "Uang Saku", ""),
            ImportCase("bluecoins", "EXPENSE,2026-01-15,-,Siang,25000,IDR,-,-,Makan,Kas,Catatan", "expense", 2_500_000, "Siang", "Makan", "Catatan"),
            ImportCase("ktw", "2026-01-15,EXPENSE,Makan,25000,IDR,Siang,Kas", "expense", 2_500_000, "Siang", "Makan", ""),
        )
        for (case in cases) {
            val plan = CsvImport.preview("header\n" + case.row + "\n", case.source, seed)
            assertTrue(plan.errors.isEmpty(), case.source + ": " + plan.errors.joinToString())
            val tx = plan.transactions.single()
            val data = seed.copy(accounts = seed.accounts + plan.accounts, categories = seed.categories + plan.categories)
            assertEquals(case.type, tx.type, case.source)
            assertEquals("2026-01-15", tx.date, case.source)
            assertEquals(case.amountMinor, tx.amountMinor, case.source)
            assertEquals("IDR", tx.currency, case.source)
            assertEquals("cash", tx.accountId, case.source)
            assertEquals(case.title, tx.title, case.source)
            assertEquals(case.category, data.categoryName(tx.categoryId), case.source)
            assertEquals(case.description, tx.description, case.source)
        }
    }

    @Test
    fun walletByBudgetBakersSemicolonExportIsNormalizedBeforeParsing() {
        val csv = "Date;Account;Category;Currency;Amount;Type;X;Y;Title;Z\n" +
            "Kas;Makan;IDR;25000;Rekening;EXPENSE;;;Siang;15/01/2026\n"
        val plan = CsvImport.preview(csv, "wallet", seed)
        assertTrue(plan.errors.isEmpty(), plan.errors.joinToString())
        val tx = plan.transactions.single()
        val data = seed.copy(categories = seed.categories + plan.categories)
        assertEquals("expense", tx.type)
        assertEquals("2026-01-15", tx.date)
        assertEquals(2_500_000L, tx.amountMinor)
        assertEquals("cash", tx.accountId)
        assertEquals("Siang", tx.title)
        assertEquals("Makan", data.categoryName(tx.categoryId))
    }

    @Test
    fun monefyUsesDayFirstDateAndAmountSignForType() {
        val csv = "header\n" +
            "15/01/2026,Kas,Makan,(25000),IDR,-,-,Siang\n" +
            "16/01/2026,Kas,Uang Saku,50000,IDR,,,Bonus\n"
        val plan = CsvImport.preview(csv, "monefy", seed)
        assertTrue(plan.errors.isEmpty(), plan.errors.joinToString())
        assertEquals(listOf("expense", "income"), plan.transactions.map { it.type })
        assertEquals(listOf(2_500_000L, 5_000_000L), plan.transactions.map { it.amountMinor })
        assertEquals(listOf("2026-01-15", "2026-01-16"), plan.transactions.map { it.date })
    }

    @Test
    fun fortuneCityTypeComesFromIncomeCategory() {
        val csv = "header\n" +
            "Kas,Income,50000,IDR,2026-01-15,Bonus\n" +
            "Kas,Makan,25000,IDR,2026-01-15,Siang\n"
        val plan = CsvImport.preview(csv, "fortunecity", seed)
        assertTrue(plan.errors.isEmpty(), plan.errors.joinToString())
        assertEquals(listOf("income", "expense"), plan.transactions.map { it.type })
        assertEquals(listOf(5_000_000L, 2_500_000L), plan.transactions.map { it.amountMinor })
        assertEquals("cash", plan.transactions.first().accountId)
    }

    @Test
    fun financistoPairsTransferRowsAndPositiveRowsBecomeIncome() {
        val csv = "header\n" +
            "2026-01-15,10:00,Kas,-100000,IDR,,,Makan,,Pindah,TRANSFER,,Catatan\n" +
            "2026-01-15,10:05,Bank,100000,USD,,,Makan,,Pindah,TRANSFER,,Catatan\n" +
            "2026-01-16,10:00,Kas,50000,IDR,,,Uang Saku,,Bonus,EXPENSE,,\n"
        val plan = CsvImport.preview(csv, "financisto", seed)
        assertTrue(plan.errors.isEmpty(), plan.errors.joinToString())
        assertEquals(2, plan.transactions.size)
        val transfer = plan.transactions[0]
        assertEquals("transfer", transfer.type)
        assertEquals("cash", transfer.accountId)
        assertEquals("bank", transfer.toAccountId)
        assertEquals(10000000L, transfer.amountMinor)
        assertEquals(10000000L, transfer.receivedAmountMinor)
        assertEquals("IDR", transfer.currency)
        assertEquals("2026-01-15", transfer.date)
        assertEquals("Pindah", transfer.title)
        assertEquals("transfer", transfer.categoryId)
        val income = plan.transactions[1]
        assertEquals("income", income.type)
        assertEquals(5000000L, income.amountMinor)
        assertEquals("2026-01-16", income.date)
        assertEquals("Bonus", income.title)
    }

    @Test
    fun transferWithoutTargetColumnBecomesExpenseWithSingleWarning() {
        val csv = "header\n2026-01-15,Kas,TRANSFER,Makan,25000,IDR,Siang\n"
        val plan = CsvImport.preview(csv, "spendee", seed)
        assertTrue(plan.errors.isEmpty(), plan.errors.joinToString())
        assertEquals("expense", plan.transactions.single().type)
        assertEquals(1, plan.warnings.count { it.contains("tanpa dompet tujuan") })
    }

    @Test
    fun positionalImportIsDeterministicSoReimportOnlyCountsDuplicates() {
        val csv = "header\n2026-01-15,Kas,Makan,-,Siang,-,EXPENSE,Catatan,25000,IDR\n"
        val first = CsvImport.preview(csv, "money_manager", seed)
        assertTrue(first.errors.isEmpty(), first.errors.joinToString())
        val again = CsvImport.preview(csv, "money_manager", seed.copy(transactions = first.transactions))
        assertTrue(again.errors.isEmpty(), again.errors.joinToString())
        assertEquals(1, again.duplicateCount)
        assertTrue(again.transactions.isEmpty())
        assertTrue(again.accounts.isEmpty() && again.categories.isEmpty())
    }

    @Test
    fun amountAndDateVariantsFromEuropeanExportsAreNormalized() {
        val csv = "header\n" +
            "15.01.2026,Kas,Makan,-,Siang,-,EXPENSE,Catatan,\"1.234,50\",IDR\n" +
            "2026-01-16,Kas,Makan,-,Siang,-,EXPENSE,Catatan,25 IDR,\n"
        val plan = CsvImport.preview(csv, "money_manager", seed)
        assertTrue(plan.errors.isEmpty(), plan.errors.joinToString())
        assertEquals(listOf("2026-01-15", "2026-01-16"), plan.transactions.map { it.date })
        assertEquals(listOf(123450L, 2500L), plan.transactions.map { it.amountMinor })
        assertEquals(listOf("IDR", "IDR"), plan.transactions.map { it.currency })
    }

    @Test
    fun badRowIsReportedWithoutFailingWholeFile() {
        val csv = "header\n" +
            "bukan-tanggal,Kas,Makan,-,Siang,-,EXPENSE,Catatan,25000,IDR\n" +
            "2026-01-15,Kas,Makan,-,Siang,-,EXPENSE,Catatan,25000,IDR\n"
        val plan = CsvImport.preview(csv, "money_manager", seed)
        assertEquals(1, plan.errors.size)
        assertTrue(plan.errors.single().startsWith("Baris 2:"), plan.errors.single())
        assertEquals(1, plan.transactions.size)
    }

    @Test
    fun kasivaExportOfOtherSourceTransactionsCanBeReimported() {
        val csv = "header\n2026-01-15,Kas,Makan,-,Siang,-,EXPENSE,Catatan,25000,IDR\n"
        val plan = CsvImport.preview(csv, "money_manager", seed)
        val data = seed.copy(
            accounts = seed.accounts + plan.accounts,
            categories = seed.categories + plan.categories,
            transactions = plan.transactions,
        )
        val exported = CsvImport.export(data)
        val repeated = CsvImport.preview(exported, "finflow", data)
        assertTrue(repeated.errors.isEmpty(), repeated.errors.joinToString())
        assertEquals(1, repeated.duplicateCount)
        val restored = CsvImport.preview(exported, "finflow", data.copy(transactions = emptyList()))
        assertTrue(restored.errors.isEmpty(), restored.errors.joinToString())
        val tx = restored.transactions.single()
        assertEquals(plan.transactions.single().id, tx.id)
        assertEquals("money_manager", tx.source)
    }

    @Test
    fun aiMappingConvertsAllRowsWithExplicitAmountAndDateFormat() {
        val mapping = AiCsvMapping(date = 0, title = 1, category = 2, account = 3, amount = 4, currency = 5,
            dateFormat = "dd/MM/yyyy", amountFormat = "eu")
        val eu = buildAiPlan(listOf(listOf("15/01/2026", "Makan siang", "Makan", "Kas", "1.234,50", "IDR")), mapping, seed)
        assertTrue(eu.errors.isEmpty(), eu.errors.joinToString())
        val euTx = eu.transactions.single()
        assertEquals("2026-01-15", euTx.date)
        assertEquals(123450L, euTx.amountMinor)
        assertEquals("IDR", euTx.currency)
        assertEquals("expense", euTx.type)
        assertEquals("cash", euTx.accountId)
        assertEquals("ai", euTx.source)
        assertNull(euTx.sourceId)

        val en = buildAiPlan(listOf(listOf("16/01/2026", "Gaji", "Gaji", "Bank", "1,234.50", "USD")),
            mapping.copy(amountFormat = "en", typeFromSign = true), seed)
        assertTrue(en.errors.isEmpty(), en.errors.joinToString())
        val enTx = en.transactions.single()
        assertEquals("2026-01-16", enTx.date)
        assertEquals(123450L, enTx.amountMinor)
        assertEquals("USD", enTx.currency)
        assertEquals("income", enTx.type)
        assertEquals("bank", enTx.accountId)
    }

    @Test
    fun aiMappingUsesTypeMappingThenSignFallback() {
        val mapping = AiCsvMapping(date = 0, account = 1, amount = 2, type = 3, dateFormat = "dd/MM/yyyy",
            typeMapping = AiCsvTypeMapping(income = listOf("Masuk"), expense = listOf("Keluar")))
        val rows = listOf(
            listOf("15/01/2026", "Kas", "25000", "Masuk"),
            listOf("15/01/2026", "Kas", "10000", "Keluar"),
            listOf("15/01/2026", "Kas", "5000", "Tidak dikenal"),
        )
        val plan = buildAiPlan(rows, mapping, seed)
        assertTrue(plan.errors.isEmpty(), plan.errors.joinToString())
        assertEquals(listOf("income", "expense", "expense"), plan.transactions.map { it.type })
        assertEquals(listOf(2_500_000L, 1_000_000L, 500_000L), plan.transactions.map { it.amountMinor })
    }

    @Test
    fun aiMappingHandlesTransferColumnsAndTargetFallback() {
        val mapping = AiCsvMapping(date = 0, title = 1, account = 2, amount = 3, type = 4, toAccount = 5, transferAmount = 6,
            dateFormat = "dd/MM/yyyy", typeMapping = AiCsvTypeMapping(transfer = listOf("TRANSFER")))
        val plan = buildAiPlan(listOf(listOf("15/01/2026", "Pindah", "Kas", "25000", "TRANSFER", "Bank", "1.50")), mapping, seed)
        assertTrue(plan.errors.isEmpty(), plan.errors.joinToString())
        val tx = plan.transactions.single()
        assertEquals("transfer", tx.type)
        assertEquals("cash", tx.accountId)
        assertEquals("bank", tx.toAccountId)
        assertEquals(2_500_000L, tx.amountMinor)
        assertEquals(150L, tx.receivedAmountMinor)
        assertEquals("transfer", tx.categoryId)

        val broken = buildAiPlan(listOf(listOf("16/01/2026", "Pindah", "Kas", "25000", "TRANSFER", "", "")), mapping, seed)
        assertTrue(broken.errors.isEmpty(), broken.errors.joinToString())
        assertEquals("expense", broken.transactions.single().type)
        assertNull(broken.transactions.single().toAccountId)
        assertEquals(1, broken.warnings.count { it.contains("tanpa dompet tujuan") })
    }

    @Test
    fun aiMappingIsolatesBadRowsAndCountsDeterministicDuplicates() {
        val mapping = AiCsvMapping(date = 0, title = 1, category = 2, account = 3, amount = 4, currency = 5, dateFormat = "dd/MM/yyyy")
        val rows = listOf(
            listOf("15/01/2026", "Makan", "Makan", "Kas", "25.000", "IDR"),
            listOf("bukan-tanggal", "Rusak", "Makan", "Kas", "25.000", "IDR"),
        )
        val first = buildAiPlan(rows, mapping, seed)
        assertEquals(1, first.errors.size)
        assertTrue(first.errors.single().startsWith("Baris 3:"), first.errors.single())
        assertEquals(1, first.transactions.size)
        assertEquals("ai", first.transactions.single().source)
        val again = buildAiPlan(rows, mapping, seed.copy(transactions = first.transactions))
        assertEquals(1, again.duplicateCount)
        assertTrue(again.transactions.isEmpty())
    }

    @Test
    fun aiMappingWithoutColumnsReturnsRejection() {
        val plan = buildAiPlan(listOf(listOf("15/01/2026", "Kas")), AiCsvMapping(note = "Bukan transaksi keuangan"), seed)
        assertTrue(plan.transactions.isEmpty())
        assertTrue(plan.accounts.isEmpty() && plan.categories.isEmpty())
        assertEquals(1, plan.errors.size)
        assertTrue(plan.errors.single().contains("Format CSV tidak dikenali"), plan.errors.single())
    }

    private class ImportCase(
        val source: String, val row: String, val type: String, val amountMinor: Long,
        val title: String, val category: String, val description: String,
    ) {
        override fun toString() = source
    }
}