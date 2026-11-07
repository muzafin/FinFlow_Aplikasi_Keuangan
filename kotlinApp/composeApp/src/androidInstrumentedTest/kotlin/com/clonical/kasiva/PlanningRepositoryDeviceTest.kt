package com.clonical.kasiva

import androidx.test.platform.app.InstrumentationRegistry
import com.clonical.kasiva.core.data.PlanningRepository
import com.clonical.kasiva.core.model.*
import com.clonical.kasiva.core.util.today
import dev.gitlive.firebase.*
import dev.gitlive.firebase.auth.*
import dev.gitlive.firebase.firestore.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import java.util.UUID

// Secondary demo app plus adb reverse: these tests never use the default production Firebase app.
class PlanningRepositoryDeviceTest {
    private lateinit var app: FirebaseApp
    private lateinit var db: FirebaseFirestore
    private lateinit var root: DocumentReference
    private lateinit var planner: PlanningRepository
    private val date get()=today().toString()
    private fun ref(collection:String,id:String)=root.collection(collection).document(id)
    private fun check(block:suspend ()->Unit)=runBlocking { withTimeout(60000){block()} }
    @Before fun setup()=check {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        app=Firebase.initialize(context,FirebaseOptions(applicationId="1:123:android:test",apiKey="fake-api-key",projectId="demo-finflow"),"planning-${UUID.randomUUID()}")
        val auth=Firebase.auth(app);auth.useEmulator("127.0.0.1",9098)
        val user=auth.signInAnonymously().user!!
        db=Firebase.firestore(app);db.useEmulator("127.0.0.1",8088)
        db.settings=firestoreSettings { cacheSettings=memoryCacheSettings {} }
        root=db.collection("users").document(user.uid);planner=PlanningRepository(db,root)
        ref("accounts","cash").set(Account(id="cash",name="Dompet Uji"))
        ref("accounts","usd").set(Account(id="usd",name="USD Uji",currency="USD"))
        defaultCategories.forEach{ref("categories",it.id).set(it)}
    }
    @After fun teardown()=runBlocking { if(::app.isInitialized) app.delete() }

    @Test fun billPaymentIsAtomicIdempotentAndReversible()=check {
        planner.saveBill(Bill(id="bill",title="Listrik uji",amountMinor=10000,accountId="cash"))
        assertTrue(planner.payBill("bill",date,date));assertFalse(planner.payBill("bill",date,date))
        val id="bill-bill-$date"
        assertEquals(1,ref("transactions",id).get().let{if(it.exists)1 else 0})
        assertTrue(ref("bills","bill").get().data<Bill>().paidAt>0)
        planner.reverseEvent(id)
        assertFalse(ref("transactions",id).get().exists)
        assertEquals(0L,ref("bills","bill").get().data<Bill>().paidAt)
        assertTrue(planner.payBill("bill",date,date))
    }
    @Test fun debtPartialPaymentRejectsOverpaymentAndSupportsUndo()=check {
        planner.saveDebt(Debt(id="debt",person="Uji",amountMinor=10000,accountId="cash"))
        assertEquals("income",ref("transactions","debt-initial-debt").get().data<com.clonical.kasiva.core.model.Transaction>().type)
        assertTrue(planner.payDebt("debt",4000,"cash",date,"p1"))
        assertFalse(planner.payDebt("debt",4000,"cash",date,"p1"))
        try { planner.payDebt("debt",6001,"cash",date,"too-much");fail("Overpayment accepted") }catch(_:IllegalArgumentException){}
        assertEquals(4000L,ref("debts","debt").get().data<Debt>().paidMinor)
        planner.reverseEvent("debt-payment-p1")
        assertEquals(0L,ref("debts","debt").get().data<Debt>().paidMinor)
        planner.deleteDebt("debt")
        assertFalse(ref("transactions","debt-initial-debt").get().exists)
    }
    @Test fun trackingOnlyDebtNeverCreatesWalletJournal()=check {
        planner.saveDebt(Debt(id="tracking",person="Uji",amountMinor=10000,accountId="cash",includeBalance=false))
        planner.payDebt("tracking",3000,"cash",date,"tracking")
        assertFalse(ref("transactions","debt-initial-tracking").get().exists)
        assertFalse(ref("transactions","debt-payment-tracking").get().exists)
        assertEquals(3000L,ref("debts","tracking").get().data<Debt>().paidMinor)
        planner.reverseEvent("debt-payment-tracking")
        assertEquals(0L,ref("debts","tracking").get().data<Debt>().paidMinor)
    }
    @Test fun reimbursementRequiresActualExpenseAndMatchingCurrency()=check {
        val expense=com.clonical.kasiva.core.model.Transaction(id="expense",type="expense",amountMinor=10000,currency="IDR",accountId="cash",categoryId="other_expense",title="Uji",date=date)
        ref("transactions","expense").set(expense)
        planner.saveReimbursement(Reimbursement("expense","expense","Uji","Kantor uji",10000,"IDR","cash"))
        try { planner.receiveReimbursement("expense","usd",date);fail("Currency mismatch accepted") }catch(_:IllegalArgumentException){}
        assertTrue(planner.receiveReimbursement("expense","cash",date))
        assertFalse(planner.receiveReimbursement("expense","cash",date))
        try { ref("transactions","expense").delete();fail("Claimed expense deleted") }catch(_:FirebaseFirestoreException){}
        planner.reverseEvent("reimburse-expense")
        assertEquals(0L,ref("reimbursements","expense").get().data<Reimbursement>().receivedAt)
        planner.deleteReimbursement("expense")
    }
    @Test fun recurringTodayIsExactlyOnceAndInactiveRuleDoesNotRun()=check {
        planner.saveRecurring(RecurringTransaction(id="repeat",title="Uji",amountMinor=10000,accountId="cash",categoryId="bills"))
        assertEquals(1,planner.processDueRecurring());assertEquals(0,planner.processDueRecurring())
        val id="recurring-repeat-$date"
        assertTrue(ref("transactions",id).get().exists)
        planner.reverseEvent(id)
        assertEquals(0,planner.processDueRecurring())
        planner.saveRecurring(RecurringTransaction(id="paused",title="Uji nonaktif",amountMinor=10000,accountId="cash",categoryId="bills",active=false))
        assertEquals(0,planner.processDueRecurring())
        assertFalse(ref("transactions","recurring-paused-$date").get().exists)
    }
}
