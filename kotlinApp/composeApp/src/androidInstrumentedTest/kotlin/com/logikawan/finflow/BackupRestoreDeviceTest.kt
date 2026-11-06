package com.logikawan.finflow

import androidx.test.platform.app.InstrumentationRegistry
import com.logikawan.finflow.core.data.*
import com.logikawan.finflow.core.model.*
import com.logikawan.finflow.core.model.Transaction
import com.logikawan.finflow.core.util.today
import dev.gitlive.firebase.*
import dev.gitlive.firebase.auth.*
import dev.gitlive.firebase.firestore.*
import kotlinx.coroutines.*
import kotlinx.datetime.*
import org.junit.*
import org.junit.Assert.*
import java.util.UUID

// Secondary demo app only; adb reverse must point at the local emulator, never production.
class BackupRestoreDeviceTest {
    private lateinit var app:FirebaseApp
    private lateinit var db:FirebaseFirestore
    private lateinit var root:DocumentReference
    private lateinit var finance:FinanceRepository
    private lateinit var restore:BackupRestoreRepository
    private fun ref(collection:String,id:String)=root.collection(collection).document(id)
    private fun check(block:suspend ()->Unit)=runBlocking{withTimeout(180000){block()}}
    private suspend inline fun <reified T> records(collection:String)=root.collection(collection).get().documents.map{it.data<T>()}

    @Before fun setup()=check {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        app=Firebase.initialize(context,FirebaseOptions(applicationId="1:123:android:test",apiKey="fake-api-key",projectId="demo-finflow"),"backup-${UUID.randomUUID()}")
        val auth=Firebase.auth(app);auth.useEmulator("127.0.0.1",9098)
        val user=auth.createUserWithEmailAndPassword("backup-${UUID.randomUUID()}@example.test","FinFlow-test-123!").user!!
        db=Firebase.firestore(app);db.useEmulator("127.0.0.1",8088)
        db.settings=firestoreSettings{cacheSettings=memoryCacheSettings{}}
        root=db.collection("users").document(user.uid)
        finance=FinanceRepository(app);finance.initializeProfile("Pemulihan Uji")
        restore=BackupRestoreRepository(db,root)
    }
    @After fun teardown()=runBlocking{if(::app.isInitialized)app.delete()}

    @Test fun fullRestorePreservesBalancesEventsAndMetadataAndResumesWithoutDuplicateMoney()=check {
        val backup=fixture();BackupCodec.validate(backup)
        var began=false
        val stages=mutableListOf<String>()
        restore.restore(backup,resume=false,begin={began=true},progress={stages+=it})
        assertTrue(began);assertTrue(stages.any{it.startsWith("Riwayat pembayaran")})
        val expected=BackupCodec.restoreData(backup)
        val actual=snapshot()
        assertRestored(expected,actual)
        assertEquals(81000L,actual.balance(actual.accounts.single{it.name=="Kas sumber"}))
        assertEquals(15000L,actual.balance(actual.accounts.single{it.id=="goal-g"}))
        assertEquals(96000L,actual.includedAccounts().sumOf{actual.balance(it)})
        assertTrue(actual.accounts.single{it.id=="acc_cash"}.archived)
        val txIds=actual.transactions.map{it.id}.toSet()
        val eventIds=actual.financialEvents.map{it.id}.toSet()
        restore.restore(backup,resume=true,begin={},progress={})
        val resumed=snapshot()
        assertRestored(expected,resumed)
        assertEquals(txIds,resumed.transactions.map{it.id}.toSet());assertEquals(eventIds,resumed.financialEvents.map{it.id}.toSet())
        assertEquals(96000L,resumed.includedAccounts().sumOf{resumed.balance(it)})
    }

    @Test fun restoredPaymentsCanBeReversedThroughTheRealRepository()=check {
        val backup=fixture();BackupCodec.validate(backup)
        restore.restore(backup,resume=false,begin={},progress={})
        val p=finance.planner
        p.reverseEvent("goal-withdraw")
        assertEquals(20000L,ref("goals","g").get().data<Goal>().savedMinor)
        p.reverseEvent("goal-deposit")
        assertEquals(0L,ref("goals","g").get().data<Goal>().savedMinor)
        p.reverseEvent("bundle-payment")
        assertNull(ref("notes","bundle").get().data<FinanceNote>().items.single().paymentId)
        assertFalse(ref("notes","bundle").get().data<FinanceNote>().items.single().checked)
        p.reverseEvent("bill-b-${today()}")
        assertEquals(0L,ref("bills","b").get().data<Bill>().paidAt)
        p.reverseEvent("reimburse-expense")
        assertEquals(0L,ref("reimbursements","expense").get().data<Reimbursement>().receivedAt)
        p.reverseEvent("debt-payment")
        assertEquals(0L,ref("debts","d").get().data<Debt>().paidMinor)
        p.reverseEvent("debt-initial-d")
        assertFalse(ref("debts","d").get().exists)
        p.reverseEvent("recurring-r-${today()}")
        val actual=snapshot()
        assertEquals(setOf("expense"),actual.transactions.map{it.id}.toSet())
        assertTrue(actual.financialEvents.isEmpty())
        assertEquals(94000L,actual.includedAccounts().sumOf{actual.balance(it)})
        assertEquals(0,finance.planner.processDueRecurring())
    }

    @Test fun interruptedRestoreResumesTheSameIdsWithoutDuplicatingCommittedJournals()=check {
        val backup=fixture();BackupCodec.validate(backup)
        var paymentStages=0
        try {
            restore.restore(backup,resume=false,begin={},progress={stage->
                if(stage.startsWith("Riwayat pembayaran") && ++paymentStages==2)throw InterruptedRestore()
            })
            fail("Expected interrupted restore")
        } catch(_:InterruptedRestore){}
        assertTrue(records<FinancialEvent>("financialEvents").isNotEmpty())
        val committed=records<Transaction>("transactions").associateBy{it.id}
        restore.restore(backup,resume=true,begin={},progress={})
        val actual=snapshot()
        assertRestored(BackupCodec.restoreData(backup),actual)
        committed.forEach{(id,tx)->assertEquals(tx,actual.transactions.single{it.id==id})}
        assertEquals(96000L,actual.includedAccounts().sumOf{actual.balance(it)})
    }

    @Test fun zeroFundedLinkedGoalAndItsLinkedCardRestoreWithoutInventingAnEvent()=check {
        val holding=Account("goal-zero","Target kosong",type="savings",goalId="zero",createdAt=1000)
        val backup=FinanceBackup(id="zero-goal-fixture",ownerId="source",createdAt=1000,data=FinanceData(
            profile=Profile("source","Sumber","source@example.test"),accounts=listOf(holding),categories=defaultCategories,
            goals=listOf(Goal("zero","Target kosong",10000,holdingAccountId=holding.id)),
            cards=listOf(WalletCard("linked","Rekening target",lastFour="",recordType="bank",accountNumber="1234567890",accountId=holding.id,createdAt=1000))))
        BackupCodec.validate(backup)
        restore.restore(backup,resume=false,begin={},progress={})
        assertEquals("goal-zero",ref("goals","zero").get().data<Goal>().holdingAccountId)
        assertEquals(0L,ref("goals","zero").get().data<Goal>().savedMinor)
        assertEquals("zero",ref("accounts","goal-zero").get().data<Account>().goalId)
        assertEquals("goal-zero",ref("cards","linked").get().data<WalletCard>().accountId)
        assertTrue(records<Transaction>("transactions").isEmpty());assertTrue(records<FinancialEvent>("financialEvents").isEmpty())
    }

    @Test fun freshRestoreRejectsNonemptyTargetBeforeAnyMutation()=check {
        val backup=fixture();BackupCodec.validate(backup)
        finance.saveTransaction(Transaction("existing","expense",500,"IDR","acc_cash","food","Sudah ada",date=today().toString()))
        var began=false
        try {restore.restore(backup,resume=false,begin={began=true},progress={});fail("Nonempty target accepted")}
        catch(_:IllegalArgumentException){}
        assertFalse(began)
        assertEquals(setOf("existing"),records<Transaction>("transactions").map{it.id}.toSet())
        assertEquals(setOf("acc_cash"),records<Account>("accounts").map{it.id}.toSet())
        assertTrue(records<FinancialEvent>("financialEvents").isEmpty())
        assertFalse(ref("categories","notes").get().exists)
    }

    private suspend fun snapshot()=FinanceData(
        accounts=records("accounts"),categories=records("categories"),transactions=records("transactions"),
        goals=records("goals"),budgets=records("budgets"),investments=records("investments"),
        bills=records("bills"),debts=records("debts"),wishes=records("wishes"),cards=records("cards"),notes=records("notes"),
        reimbursements=records("reimbursements"),recurring=records("recurring"),financialEvents=records("financialEvents"),
        profile=root.get().data<Profile>(),featuresReady=true)

    private fun assertRestored(expected:FinanceData,actual:FinanceData) {
        assertEquals(finance.currentUser!!.uid,actual.profile!!.id)
        assertEquals(expected.transactions.sortedBy{it.id},actual.transactions.sortedBy{it.id})
        assertEquals(expected.financialEvents.sortedBy{it.id},actual.financialEvents.sortedBy{it.id})
        expected.accounts.forEach{account->
            val restored=actual.accounts.single{it.id==account.id}
            assertEquals(account,restored);assertEquals(expected.balance(account),actual.balance(restored))
        }
        assertEquals(expected.budgets,actual.budgets);assertEquals(expected.investments,actual.investments)
        assertEquals(expected.wishes,actual.wishes);assertEquals(expected.cards,actual.cards)
        assertEquals(expected.goals.single().savedMinor,actual.goals.single().savedMinor)
        assertEquals(expected.goals.single().holdingAccountId,actual.goals.single().holdingAccountId)
        assertEquals(expected.goals.single().legacySavedMinor,actual.goals.single().legacySavedMinor)
        assertEquals(expected.notes.single().items,actual.notes.single().items)
        assertEquals(expected.bills.single().paidAt,actual.bills.single().paidAt)
        assertEquals(expected.debts.single().paidMinor,actual.debts.single().paidMinor)
        assertEquals(expected.reimbursements.single().receivedAt,actual.reimbursements.single().receivedAt)
        assertEquals(expected.recurring.single().nextDate,actual.recurring.single().nextDate)
        assertFalse(actual.recurring.single().active)
    }

    private class InterruptedRestore:RuntimeException("Interupsi pemulihan uji")

    private fun fixture():FinanceBackup {
        val date=today().toString();val yesterday=today().minus(DatePeriod(days=1)).toString();val tomorrow=today().plus(DatePeriod(days=1)).toString()
        val cash=Account("cash","Kas sumber",openingBalanceMinor=100000,createdAt=1000)
        val holding=Account("goal-g","Dana darurat",type="savings",goalId="g",icon="savings",createdAt=1000)
        fun event(id:String,kind:String,reference:String,title:String,amount:Long,account:String="cash",whenDate:String=date,time:Long=2000,previous:String?=null,itemIds:List<String> = emptyList())=
            FinancialEvent(id,kind,reference,title,amount,"IDR",account,whenDate,createdAt=time,previousEventId=previous,itemIds=itemIds)
        val events=listOf(
            event("goal-deposit","goal_deposit","g","Setor ke Dana darurat",20000,whenDate=yesterday,time=2000),
            event("goal-withdraw","goal_withdrawal","g","Tarik dari Dana darurat",5000,account=holding.id,time=2100),
            event("debt-initial-d","debt_initial","d","Pinjaman dari Pemberi pinjaman",10000,time=2200),
            event("debt-payment","debt_payment","d","Bayar hutang ke Pemberi pinjaman",2000,time=2300),
            event("bundle-payment","bundle","bundle","Bundle: Belanja uji",4000,time=2400,itemIds=listOf("item")),
            event("reimburse-expense","reimbursement","expense","Reimburse: Belanja kantor",2000,time=2500),
            event("bill-b-$date","bill","b","Tagihan uji",3000,time=2600),
            event("recurring-r-$date","recurring","r","Rutin uji",1000,time=2700),
        )
        val transactions=listOf(Transaction("expense","expense",6000,"IDR",cash.id,"food","Belanja kantor",date=date,createdAt=1500,updatedAt=1500,tags=listOf("kantor")))+events.map{e->
            val transfer=e.kind.startsWith("goal_")
            val income=e.kind in listOf("debt_initial","reimbursement")
            val category=when{transfer->"transfer";e.kind=="bundle"->"notes";e.kind=="bill"->"bills";e.kind=="recurring"->"food";income->"other_income";else->"other_expense"}
            Transaction(e.id,if(transfer)"transfer" else if(income)"income" else "expense",e.amountMinor,"IDR",e.accountId,category,e.title,date=e.date,
                toAccountId=if(transfer)if(e.kind=="goal_deposit")holding.id else cash.id else null,receivedAmountMinor=if(transfer)e.amountMinor else null,
                source=e.kind,sourceId=e.referenceId,createdAt=e.createdAt,updatedAt=e.createdAt)
        }
        return FinanceBackup(id="a1b2c3d4-full-fixture",createdAt=4000,ownerId="source",preferences=AppPreferences(recordingDay=25),data=FinanceData(
            profile=Profile("source","Sumber","source@example.test"),accounts=listOf(cash,holding),categories=defaultCategories+Category("notes","Bundle belanja","expense",icon="checklist"),transactions=transactions,
            goals=listOf(Goal("g","Dana darurat",100000,savedMinor=15000,holdingAccountId=holding.id,lastEventId="goal-withdraw",revision=2)),
            bills=listOf(Bill("b","Tagihan uji",3000,accountId=cash.id,dueDate=date,anchorDay=today().dayOfMonth,paidAt=2600,lastPaymentId="bill-b-$date",revision=1,createdAt=1000)),
            debts=listOf(Debt("d","Pemberi pinjaman",amountMinor=10000,paidMinor=2000,accountId=cash.id,issueDate=date,dueDate=tomorrow,lastEventId="debt-payment",revision=1,createdAt=1000)),
            notes=listOf(FinanceNote("bundle","Belanja uji",createdAt=1000,updatedAt=2400,kind="bundle",accountId=cash.id,items=listOf(BundleItem("item","Barang uji",4000,true,"bundle-payment")),revision=1,lastPaymentId="bundle-payment")),
            reimbursements=listOf(Reimbursement("expense","expense","Belanja kantor","Kantor",2000,"IDR",cash.id,receivedAt=2500,receivedAccountId=cash.id,revision=1,createdAt=1000)),
            recurring=listOf(RecurringTransaction("r","Rutin uji",amountMinor=1000,accountId=cash.id,categoryId="food",frequency="daily",anchorDay=today().dayOfMonth,startDate=date,nextDate=tomorrow,active=false,lastEventId="recurring-r-$date",revision=1,createdAt=1000)),
            cards=listOf(WalletCard("bank","Bank uji",brand="other",lastFour="",accountId=cash.id,recordType="bank",accountNumber="1234567890",ownerName="Sumber",pinned=true,createdAt=1000)),
            wishes=listOf(Wish("wish","Keinginan uji",10000,createdAt=1000)),
            budgets=listOf(Budget("budget","food",10000,month=date.take(7),title="Makan uji")),
            investments=listOf(Investment("gold","Emas uji","emas",10000,11000)),financialEvents=events,featuresReady=true))
    }
}
