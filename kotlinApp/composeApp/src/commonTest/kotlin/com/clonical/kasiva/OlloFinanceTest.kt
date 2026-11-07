package com.clonical.kasiva

import com.clonical.kasiva.core.model.*
import com.clonical.kasiva.core.util.*
import com.clonical.kasiva.feature.account.*
import com.clonical.kasiva.feature.goal.*
import com.clonical.kasiva.feature.report.reportAmounts
import kotlinx.datetime.LocalDate
import kotlin.test.*

class OlloFinanceTest {
    @Test fun reportBucketsKeepBoundaryDaysAndIgnoreScheduledAndOtherTypes() {
        val period=FinancePeriod(LocalDate(2026,10,25),LocalDate(2026,11,5))
        fun tx(id:String,date:String,amount:Long,type:String="expense",scheduled:Boolean=false)=Transaction(id,type,amount,"IDR","cash","food","Uji",date=date,scheduled=scheduled)
        val entries=listOf(tx("before","2026-10-24",99),tx("first","2026-10-25",100),tx("week-end","2026-10-31",200),
            tx("second-week","2026-11-01",300),tx("last","2026-11-04",400),tx("after","2026-11-05",99),
            tx("scheduled","2026-10-26",99,scheduled=true),tx("income","2026-10-26",99,"income"))
        assertEquals(listOf(300L,700L),reportAmounts(period,entries,"expense",7))
        assertEquals(1000L,reportAmounts(period,entries,"expense").sum())
        assertEquals(11,reportAmounts(period,entries,"expense").size)
        assertFailsWith<IllegalArgumentException>{reportAmounts(period,entries,"expense",0)}
    }

    @Test fun walletDeltaUsesReceivedCurrencyAndLegacyTransferFallback() {
        val converted=Transaction("fx","transfer",150000,"IDR","cash","transfer","Konversi",toAccountId="usd",receivedAmountMinor=1000)
        assertEquals(-150000L,accountDelta(converted,"cash"))
        assertEquals(1000L,accountDelta(converted,"usd"))
        assertEquals(0L,accountDelta(converted,"other"))
        assertEquals(0L,accountDelta(converted.copy(scheduled=true),"cash"))
        assertEquals(150000L,accountDelta(converted.copy(receivedAmountMinor=null),"usd"))
    }

    @Test fun internalGoalFundingKeepsWealthAndFlowWithoutMintingLegacyProgress() {
        val date=LocalDate(2026,10,29)
        val cash=Account("cash","Kas",openingBalanceMinor=100000)
        val holding=Account("goal-g","Dana darurat",type="savings",goalId="g")
        val excluded=Account("excluded","Di luar laporan",openingBalanceMinor=999999,excludedFromTotal=true)
        val credit=Account("credit","Kredit",type="credit",openingBalanceMinor=-10000)
        val goal=Goal("g","Dana darurat",50000,savedMinor=17000,holdingAccountId=holding.id,legacySavedMinor=2000)
        val deposit=Transaction("dep","transfer",15000,"IDR",cash.id,"transfer","Setor",date=date.toString(),toAccountId=holding.id,receivedAmountMinor=15000)
        val d=FinanceData(accounts=listOf(cash,holding,excluded,credit),transactions=listOf(deposit),goals=listOf(goal,Goal("legacy","Lama",50000,savedMinor=7000)),
            debts=listOf(Debt("debt","Pemberi pinjaman",amountMinor=6000,paidMinor=1000,accountId=cash.id,issueDate=date.toString(),dueDate=date.toString())),
            bills=listOf(Bill("bill","Tagihan",3000,accountId=cash.id,dueDate="2026-11-02")),
            recurring=listOf(RecurringTransaction("rule","Rutin",amountMinor=5000,accountId=cash.id,categoryId="food",nextDate="2026-11-03")))
        val v=walletSummaryValues(d,"IDR",date)
        assertEquals(90000L,v.total)
        assertEquals(0L,v.flow30)
        assertEquals(15000L,v.savings)
        assertEquals(5000L,v.liabilities)
        assertEquals(10000L,v.negativeWallets)
        assertEquals(85000L,v.netWorth)
        assertEquals(3000L,v.upcoming)
        assertEquals(15000L,fundedGoalAmount(goal));assertEquals(2000L,legacyGoalAmount(goal))
        assertEquals(0L,fundedGoalAmount(d.goals.last()));assertEquals(7000L,legacyGoalAmount(d.goals.last()))
        assertEquals(85000L,d.balance(cash));assertEquals(15000L,d.balance(holding))
    }

    @Test fun rollingFlowAndUpcomingUseDatesAccountsAndActualJournals() {
        val day=LocalDate(2026,10,29)
        val cash=Account("cash","Kas",openingBalanceMinor=100000)
        val pocket=Account("pocket","Pocket",parentId="cash")
        val hidden=Account("hidden","Pribadi",excludedFromTotal=true)
        fun tx(id:String,date:String,amount:Long,account:String="cash",scheduled:Boolean=false)=Transaction(id,"expense",amount,"IDR",account,"food","Uji",date=date,scheduled=scheduled)
        val d=FinanceData(accounts=listOf(cash,pocket,hidden),transactions=listOf(tx("old","2026-09-29",100),tx("first","2026-09-30",200),
            tx("today",day.toString(),300),tx("hidden",day.toString(),999,"hidden"),tx("future","2026-10-30",400,scheduled=true),
            tx("far","2026-12-01",500,scheduled=true)))
        val summary=walletSummaryValues(d,"IDR",day)
        assertEquals(-500L,summary.flow30)
        assertEquals(99400L,summary.total)
        assertEquals(400L,summary.upcoming)
        assertEquals(setOf("old","first","today"),d.reportTransactions().map{it.id}.toSet())
        assertFalse(d.reportTransactions().any{it.scheduled || it.accountId=="hidden"})
    }

    @Test fun backupRoundTripPreservesMetadataAndRemapsEveryAccountReference() {
        val backup=fixture()
        BackupCodec.validate(backup)
        assertEquals(backup,BackupCodec.decode(BackupCodec.encode(backup)))
        val restored=BackupCodec.restoreData(backup)
        assertEquals(restored,BackupCodec.restoreData(backup))
        val cash=restored.accounts.single{it.name=="Kas"}
        assertTrue(cash.id.startsWith("restore-a1b2c3d4-"))
        assertEquals("goal-g",restored.goals.single().holdingAccountId)
        assertEquals(cash.id,restored.cards.single().accountId)
        assertEquals(cash.id,restored.notes.single().accountId)
        assertEquals(cash.id,restored.debts.single().accountId)
        assertEquals(cash.id,restored.reimbursements.single().receivedAccountId)
        assertTrue(restored.transactions.all{it.accountId in restored.accounts.map{a->a.id}})
        assertEquals(backup.data.balance(backup.data.accounts.first()),restored.balance(cash))
        BackupCodec.validate(backup.copy(data=restored))
    }

    @Test fun replayOrderUsesDependenciesWhenDatesAndClockMoveBackwards() {
        val backup=fixture()
        val ordered=BackupCodec.eventsForRestore(backup.data).map{it.id}
        assertTrue(ordered.indexOf("goal-deposit")<ordered.indexOf("goal-withdraw"))
        assertTrue(ordered.indexOf("debt-initial-debt")<ordered.indexOf("debt-payment"))
        assertTrue(ordered.indexOf("bundle-pay")<ordered.indexOf("reimburse-bundle-pay"))
        assertEquals(backup.data.financialEvents.size,ordered.distinct().size)
        val broken=backup.copy(data=backup.data.copy(financialEvents=backup.data.financialEvents.map{if(it.id=="goal-deposit")it.copy(previousEventId="goal-withdraw") else it}))
        assertFailsWith<IllegalStateException>{BackupCodec.validate(broken)}
    }

    @Test fun backupRejectsMissingJournalsBrokenSavingsAndDuplicateIds() {
        val backup=fixture()
        assertFails{BackupCodec.validate(backup.copy(data=backup.data.copy(transactions=backup.data.transactions.filterNot{it.id=="goal-deposit"})))}
        assertFailsWith<IllegalArgumentException>{BackupCodec.validate(backup.copy(data=backup.data.copy(goals=backup.data.goals.map{it.copy(savedMinor=16000)})))}
        assertFailsWith<IllegalArgumentException>{BackupCodec.validate(backup.copy(data=backup.data.copy(accounts=backup.data.accounts+backup.data.accounts.first())))}
        assertFailsWith<IllegalArgumentException>{BackupCodec.validate(backup.copy(version=2))}
        assertFails{BackupCodec.decode("{}")}
    }

    @Test fun preferencesFormattingKeepsNonzeroCentsAndCurrencyIdentity() {
        val id=AppPreferences()
        assertEquals("Rp1.234,50",formatBalance(123450,"IDR",id))
        assertEquals("Rp1,234.50",formatBalance(123450,"IDR",id.copy(numberFormat="en")))
        assertEquals("IDR 1.234,50",formatBalance(123450,"IDR",id.copy(showCurrencySymbol=false)))
        assertEquals("Rp1.234",formatBalance(123400,"IDR",id.copy(showDecimals=false)))
        assertEquals("Rp1.234,50",formatBalance(123450,"IDR",id.copy(showDecimals=false)))
        assertEquals("Rp-5,50",formatBalance(-550,"IDR",id))
        assertEquals("USD 12.34",formatBalance(1234,"USD",id.copy(numberFormat="en")))
    }

    private fun fixture():FinanceBackup {
        val date="2020-01-20"
        val cash=Account("cash","Kas",openingBalanceMinor=100000,createdAt=1000)
        val holding=Account("goal-g","Darurat",type="savings",createdAt=1000,goalId="g")
        fun event(id:String,kind:String,ref:String,title:String,amount:Long,account:String="cash",whenDate:String=date,time:Long=2000,previous:String?=null,itemIds:List<String> = emptyList())=
            FinancialEvent(id,kind,ref,title,amount,"IDR",account,whenDate,createdAt=time,previousEventId=previous,itemIds=itemIds)
        val events=listOf(
            event("goal-withdraw","goal_withdrawal","g","Tarik dari Darurat",5000,"goal-g","2020-01-19",1000),
            event("debt-payment","debt_payment","debt","Bayar hutang",2000,time=1000),
            event("reimburse-bundle-pay","reimbursement","bundle-pay","Reimburse belanja",1000,time=1000),
            event("goal-deposit","goal_deposit","g","Setor ke Darurat",20000,time=3000),
            event("debt-initial-debt","debt_initial","debt","Pinjaman",10000,time=3000),
            event("bundle-pay","bundle","n","Bundle: Belanja",4000,time=3000,itemIds=listOf("item")),
        )
        val transactions=events.map{e->
            val transfer=e.kind.startsWith("goal_")
            val income=e.kind in listOf("debt_initial","reimbursement")
            Transaction(e.id,if(transfer)"transfer" else if(income)"income" else "expense",e.amountMinor,e.currency,e.accountId,
                if(transfer)"transfer" else if(e.kind=="bundle")"notes" else if(income)"other_income" else "other_expense",e.title,date=e.date,
                toAccountId=if(transfer)if(e.kind=="goal_deposit")holding.id else cash.id else null,receivedAmountMinor=if(transfer)e.amountMinor else null,
                createdAt=e.createdAt,updatedAt=e.createdAt,source=e.kind,sourceId=e.referenceId)
        }
        return FinanceBackup(id="a1b2c3d4-fixture",createdAt=4000,ownerId="source",preferences=AppPreferences(theme="dark",recordingDay=25),data=FinanceData(
            accounts=listOf(cash,holding),categories=defaultCategories+Category("notes","Belanja","expense"),transactions=transactions,
            profile=Profile("source","Pemilik","source@example.test"),goals=listOf(Goal("g","Darurat",50000,savedMinor=15000,holdingAccountId=holding.id,lastEventId="goal-withdraw",revision=2)),
            debts=listOf(Debt("debt","Pemilik pinjaman",amountMinor=10000,paidMinor=2000,accountId=cash.id,issueDate=date,dueDate=date,lastEventId="debt-payment",revision=1,createdAt=1000)),
            notes=listOf(FinanceNote("n","Belanja",createdAt=1000,updatedAt=3000,kind="bundle",accountId=cash.id,items=listOf(BundleItem("item","Barang",4000,true,"bundle-pay")),revision=1,lastPaymentId="bundle-pay")),
            cards=listOf(WalletCard("card","Rekening",lastFour="",accountId=cash.id,recordType="bank",accountNumber="1234567890",createdAt=1000)),
            reimbursements=listOf(Reimbursement("bundle-pay","bundle-pay","Belanja","Kantor",1000,"IDR",cash.id,receivedAt=1000,receivedAccountId=cash.id,revision=1,createdAt=1000)),
            financialEvents=events,featuresReady=true))
    }
}
