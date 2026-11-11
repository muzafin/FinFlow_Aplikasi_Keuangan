package com.clonical.kasiva

import com.clonical.kasiva.core.data.*
import com.clonical.kasiva.core.model.*
import com.clonical.kasiva.core.ui.sanitizeAmountInput
import com.clonical.kasiva.core.util.*
import com.clonical.kasiva.core.i18n.withLocalizedDefaults
import com.clonical.kasiva.feature.importdata.CsvImport
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.*

/** Regresi untuk temuan bug hunt 2026-11-11 (qa-bug-hunt/bug-report-20261111-155643-kasiva.md). */
class BugHuntRegressionTest {
    private class Memory : LocalFinancePersistence {
        override val id = "bh-${newId()}"; var content: String? = null
        override fun load() = LocalFinanceLoad(content)
        override fun save(content: String): String? { this.content = content; return null }
    }
    private val seed = FinanceData(accounts = listOf(Account(id = "cash", name = "Kas")), categories = defaultCategories)

    @Test fun indonesianThousandsSeparatorIsNotADecimal() { // P4-02, P5-04
        assertEquals("25000", normalizeAmountText("25.000"))
        assertEquals("1500000", normalizeAmountText("1.500.000"))
        assertEquals("12500.50", normalizeAmountText("12.500,50"))
        assertEquals("1234.5", normalizeAmountText("1,234.5"))
        assertEquals("12.5", normalizeAmountText("12,5"))
        assertEquals("-25000", normalizeAmountText("(25.000)"))
        assertEquals("1.250", normalizeAmountText("1.250", decimalDigits = 3)) // BHD: 3 digit = desimal
        assertEquals(5_000_000L, parseUserAmount("50.000", "IDR"))
        assertEquals(-2_500_000L, parseUserAmount("-25.000", "IDR", allowNegative = true))
    }

    @Test fun pastedGroupedAmountKeepsItsValue() { // P3-03
        assertEquals("1500000", sanitizeAmountInput("1.500.000", "IDR"))
        assertEquals("1500000", sanitizeAmountInput("1.500.000", "JPY"))
        assertEquals("25000", sanitizeAmountInput("25.000", "IDR"))
        assertEquals("12500.50", sanitizeAmountInput("12.500,50", "IDR"))
        assertEquals("300.", sanitizeAmountInput("300.", "IDR")) // mengetik desimal tetap bisa
    }

    @Test fun positionalCsvReadsDotThousands() { // P4-02
        val csv = "header\n2026-01-15,Kas,EXPENSE,Makan,25.000,IDR,Siang\n2026-01-15,Kas,EXPENSE,Makan,1.500.000,IDR,Sewa\n"
        val plan = CsvImport.preview(csv, "spendee", seed)
        assertEquals(listOf("Siang" to 2_500_000L, "Sewa" to 150_000_000L), plan.transactions.map { it.title to it.amountMinor })
    }

    @Test fun walletCsvKeepsDecimalComma() { // P4-01
        val csv = "Date;Account;Category;Currency;Amount;Type;X;Y;Title;Z\n" +
            "Kas;Makan;IDR;-12500,50;Rekening;EXPENSE;;;Siang;15/01/2026\n"
        assertEquals(1_250_050L, CsvImport.preview(csv, "wallet", seed).transactions.singleOrNull()?.amountMinor)
    }

    @Test fun reimportingShiftedExportOnlyAddsNewRows() { // P4-03
        val old = "2026-01-15,Kas,Makan,-,Siang,-,EXPENSE,Catatan,25000,IDR\n"
        val first = CsvImport.preview("header\n$old", "money_manager", seed)
        val data = seed.copy(transactions = first.transactions, categories = seed.categories + first.categories)
        val second = CsvImport.preview("header\n2026-01-20,Kas,Makan,-,Malam,-,EXPENSE,,30000,IDR\n$old", "money_manager", data)
        assertEquals(1, second.transactions.size)
        assertEquals(1, second.duplicateCount)
        // Dua baris identik dalam satu file tetap dua transaksi.
        val twice = CsvImport.preview("header\n$old$old", "money_manager", seed)
        assertEquals(2, twice.transactions.size)
    }

    @Test fun csvRowsNeverLandInGoalSavingsWallet() = runTest { // P1-02
        val repo = FinanceRepository(local = Memory()); repo.initializeProfile("Saya", "IDR", "Kas", 0)
        repo.saveGoal(Goal(id = "g1", name = "Liburan", targetMinor = 100_000_00))
        val before = repo.observe().first { it.featuresReady }
        val csv = "Date,Account,Amount,Currency,Type,ID,Category,Title\n2026-11-01,Liburan,50000,IDR,expense,row1,Makan,Tiket\n"
        val plan = CsvImport.preview(csv, "finflow", before)
        assertTrue(plan.transactions.none { it.accountId == "goal-g1" })
        runCatching { repo.importCsv(plan) { _, _ -> } }
        val after = repo.observe().first { it.featuresReady }
        assertEquals(0L, after.balance(after.accounts.single { it.id == "goal-g1" }))
        BackupCodec.encode(FinanceBackup(ownerId = "local", data = after))
    }

    @Test fun archivedWalletScheduleDoesNotBlockOthers() = runTest { // P2-02
        val repo = FinanceRepository(local = Memory()); repo.initializeProfile("L", "IDR", "Kas", 100000)
        repo.saveAccount(Account("w2", "Lama", openingBalanceMinor = 0))
        val t = today().toString(); val d = today().dayOfMonth
        repo.planner.saveRecurring(RecurringTransaction(id = "a-old-wallet", title = "Lama", amountMinor = 500, accountId = "w2", categoryId = "food", startDate = t, nextDate = t, anchorDay = d))
        repo.planner.saveRecurring(RecurringTransaction(id = "z-good", title = "Gaji", type = "income", amountMinor = 700, accountId = "acc_cash", categoryId = "salary", startDate = t, nextDate = t, anchorDay = d))
        val w2 = repo.observe().first { it.featuresReady }.accounts.first { it.id == "w2" }
        repo.saveAccount(w2.copy(archived = true))
        runCatching { repo.planner.processDueRecurring() }
        assertTrue(repo.observe().first { it.featuresReady }.transactions.any { it.sourceId == "z-good" })
    }

    @Test fun recurringNextDateCanBeMovedWhenStartDateIsKept() = runTest { // P2-01
        val repo = FinanceRepository(local = Memory()); repo.initializeProfile("L", "IDR", "Kas", 100000)
        val start = today().plus(DatePeriod(days = 5)).toString()
        repo.planner.saveRecurring(RecurringTransaction(id = "r1", title = "Sewa", amountMinor = 1000, accountId = "acc_cash", categoryId = "food", startDate = start, nextDate = start, anchorDay = LocalDate.parse(start).dayOfMonth))
        val old = repo.observe().first { it.featuresReady }.recurring.single()
        val next = today().plus(DatePeriod(days = 10)).toString()
        repo.planner.saveRecurring(old.copy(nextDate = next, startDate = old.startDate, anchorDay = LocalDate.parse(next).dayOfMonth))
        assertEquals(next, repo.observe().first { it.featuresReady }.recurring.single().nextDate)
    }

    @Test fun calculatorAcceptsDecimalFactorsAndRoundsOnce() { // P1-04, P1-06
        assertEquals(1100L, calculateAmount("1000*1.1", "JPY"))
        assertEquals(22_500_000L, calculateAmount("200000*1.125", "IDR"))
        assertEquals(10_000_000L, calculateAmount("100000/3*3", "IDR"))
        assertEquals(1_500_000L, calculateAmount("10000+5000", "IDR"))
    }

    @Test fun compactMoneyPromotesAfterRounding() { // P1-05
        // Rp999.950 → "1.0jt", bukan "1000.0rb".
        assertTrue(compactDisplayMoney(99_995_000L, "IDR").startsWith("Rp1.0"))
        assertFalse(compactDisplayMoney(99_995_000L, "IDR").contains("1000"))
    }

    @Test fun csvDateOrderIsDecidedPerFile() { // P4-04
        val csv = "header\n01/13/2026,Kas,Makan,(25000),IDR,-,-,A\n01/02/2026,Kas,Makan,(25000),IDR,-,-,B\n"
        assertEquals(listOf("2026-01-13", "2026-01-02"), CsvImport.preview(csv, "monefy", seed).transactions.map { it.date })
    }

    @Test fun kasivaExportRoundTripKeepsFormulaLikeText() { // P4-06
        val cat = defaultCategories.first { it.type == "expense" }
        val tx = Transaction(id = "t1", type = "expense", amountMinor = 500000, currency = "IDR", accountId = "cash", categoryId = cat.id,
            title = "-50% diskon sepatu", description = "+ongkir", date = "2026-01-15")
        val back = CsvImport.preview(CsvImport.export(seed.copy(transactions = listOf(tx))), "finflow", seed).transactions.single()
        assertEquals(tx.title, back.title); assertEquals(tx.description, back.description)
    }

    @Test fun userWalletNamedCashIsNotRenamed() { // P6-03
        val data = FinanceData(accounts = listOf(Account(id = "usd_wallet", name = "Cash", type = "cash", currency = "USD")))
        assertEquals(listOf("Cash"), data.withLocalizedDefaults().accounts.map { it.name })
    }

    @Test fun restoreKeepsWalletOrderAndDashboardSelection() = runTest { // P1-03
        val src = FinanceRepository(local = Memory()); src.initializeProfile("Asal", "IDR", "Kas", 0)
        src.saveAccount(Account(id = "acc_bank", name = "Bank", type = "bank", createdAt = 1))
        src.saveAccount(Account(id = "acc_ewallet", name = "Dana", type = "ewallet", createdAt = 2))
        val data = src.observe().first { it.featuresReady }
        val prefs = AppPreferences(walletOrder = listOf("acc_ewallet", "acc_bank", "acc_cash"), dashboardWalletIds = listOf("acc_ewallet", "acc_bank"), defaultWalletId = "acc_bank")
        val backup = BackupCodec.decode(BackupCodec.encode(FinanceBackup(ownerId = "local", data = data, preferences = prefs)))
        val restored = BackupCodec.restoreData(backup)
        val mapped = BackupCodec.restoredPreferences(backup, prefs)
        fun name(id: String) = restored.accounts.single { it.id == id }.name
        assertEquals(listOf("Dana", "Bank", "Kas"), mapped.walletOrder.map(::name))
        assertEquals(listOf("Dana", "Bank"), mapped.dashboardWalletIds.map(::name))
        assertEquals("Bank", name(mapped.defaultWalletId))
    }
}
