package com.logikawan.finflow.core.data

import com.logikawan.finflow.i18n.*
import com.logikawan.finflow.core.i18n.text
import com.logikawan.finflow.core.model.*
import com.logikawan.finflow.core.util.*
import dev.gitlive.firebase.firestore.DocumentReference
import dev.gitlive.firebase.firestore.FirebaseFirestore

// Memutar ulang event melalui aturan yang sama; tidak memakai akses admin atau mematikan rules.
class BackupRestoreRepository(private val db:FinanceDatabase,private val root:FinanceDocument) {
    constructor(db:FirebaseFirestore,root:DocumentReference):this(FinanceDatabase(remote=db),root.path)
    private constructor(db:FinanceDatabase,path:String):this(db,db.document(path))
    private fun ref(collection:String,id:String)=root.collection(collection).document(id)
    private suspend inline fun <reified T:Any> insert(collection:String,id:String,value:T) {
        val doc=ref(collection,id);db.runTransaction{if(!get(doc).exists)set(doc,value)}
    }
    suspend fun restore(backup:FinanceBackup,resume:Boolean=false,begin:()->Unit={},progress:(String)->Unit={}) {
        BackupCodec.validate(backup)
        val d=BackupCodec.restoreData(backup)
        if(!resume) {
            for(name in listOf("transactions","budgets","goals","investments","bills","debts","wishes","cards","notes","reimbursements","recurring","financialEvents"))
                require(root.collection(name).get().documents.isEmpty()){MR.strings.core_restore_target_not_empty.text()}
            val accounts=root.collection("accounts").get().documents.map{it.data<Account>()}
            require(accounts.all{it.id=="acc_cash" && it.openingBalanceMinor==0L && !it.archived}){MR.strings.core_restore_target_has_wallet.text()}
            val categories=root.collection("categories").get().documents.map{it.data<Category>()}
            require(categories.all{it.id in defaultCategories.map{c->c.id}}){MR.strings.core_restore_target_has_categories.text()}
        } else verifyResume(d)
        begin()
        progress(MR.strings.core_restore_progress_wallets.text())
        d.accounts.filter{it.type!="savings"}.sortedBy{it.parentId!=null}.forEach{insert("accounts",it.id,it.copy(archived=false))}
        d.categories.sortedBy{it.parentId!=null}.forEach{ref("categories",it.id).set(it)}
        d.goals.forEach{g->val doc=ref("goals",g.id);db.runTransaction{if(!get(doc).exists){
            val holding=g.holdingAccountId?.let{id->d.accounts.single{it.id==id}}
            holding?.let{set(ref("accounts",it.id),it.copy(archived=false))}
            set(doc,g.copy(savedMinor=if(holding==null)g.savedMinor else g.legacySavedMinor,legacySavedMinor=if(holding==null)0 else g.legacySavedMinor,revision=0,lastEventId=null,archived=false))
        }}}
        d.bills.forEach{b->insert("bills",b.id,b.copy(paidAt=0,lastPaymentId=null,revision=0,archived=false))}
        d.debts.filter{!it.includeBalance}.forEach{insert("debts",it.id,it.copy(paidMinor=0,lastEventId=null,revision=0,archived=false))}
        d.notes.forEach{n->insert("notes",n.id,n.copy(items=n.items.map{it.copy(paymentId=null)},lastPaymentId=null,revision=0,archived=false))}
        d.recurring.forEach{r->insert("recurring",r.id,r.copy(nextDate=r.startDate,active=false,lastEventId=null,revision=0))}
        d.wishes.forEach{insert("wishes",it.id,it)};d.cards.forEach{insert("cards",it.id,it)}
        d.budgets.forEach{insert("budgets",it.id,it)};d.investments.forEach{insert("investments",it.id,it)}
        val ordinary=d.transactions.filter{it.source !in managedTransactionSources}
        ordinary.forEachIndexed{index,t->insert("transactions",t.id,t);progress(MR.strings.core_restore_progress_journal.text(index+1, ordinary.size))}
        val ordered=BackupCodec.eventsForRestore(d)
        val completed=mutableSetOf<String>()
        suspend fun replay(event:FinancialEvent) {
            if(event.id in completed)return
            if(event.kind=="reimbursement") {
                val claim=d.reimbursements.single{it.id==event.referenceId}
                val dependency=d.financialEvents.find{it.transactionId==claim.expenseId}
                if(dependency!=null && dependency.id !in completed) {
                    require(dependency.kind!="reimbursement"){"Referensi reimburse melingkar"}
                    replayEvent(dependency,d);completed+=dependency.id
                }
                insert("reimbursements",claim.id,claim.copy(receivedAt=0,receivedAccountId=null,revision=0))
            }
            replayEvent(event,d);completed+=event.id
        }
        ordered.forEachIndexed{index,e->replay(e);progress(MR.strings.core_restore_progress_payments.text(index+1, ordered.size))}
        d.reimbursements.forEach{r->insert("reimbursements",r.id,r.copy(receivedAt=0,receivedAccountId=null,revision=0))}
        progress(MR.strings.core_restore_progress_settings.text())
        d.bills.forEach{b->val doc=ref("bills",b.id);db.runTransaction{val old=get(doc).data<Bill>();set(doc,b.copy(paidAt=old.paidAt,lastPaymentId=old.lastPaymentId,revision=old.revision+1))}}
        d.debts.forEach{v->val doc=ref("debts",v.id);db.runTransaction{val old=get(doc).data<Debt>();require(v.paidMinor==old.paidMinor);set(doc,v.copy(lastEventId=old.lastEventId,revision=old.revision+1))}}
        d.recurring.forEach{v->val doc=ref("recurring",v.id);db.runTransaction{val old=get(doc).data<RecurringTransaction>();set(doc,v.copy(lastEventId=old.lastEventId,revision=old.revision+1))}}
        d.notes.forEach{v->val doc=ref("notes",v.id);db.runTransaction{val old=get(doc).data<FinanceNote>();set(doc,v.copy(lastPaymentId=old.lastPaymentId,revision=old.revision+1))}}
        d.reimbursements.forEach{v->val doc=ref("reimbursements",v.id);db.runTransaction{val old=get(doc).data<Reimbursement>();set(doc,v.copy(receivedAt=old.receivedAt,receivedAccountId=old.receivedAccountId,revision=old.revision+1))}}
        d.goals.forEach{g->val doc=ref("goals",g.id);db.runTransaction{
            val old=get(doc).data<Goal>();require(old.savedMinor==g.savedMinor)
            val holding=g.holdingAccountId?.let{d.accounts.single{a->a.id==it}}
            set(doc,g.copy(holdingAccountId=old.holdingAccountId,legacySavedMinor=old.legacySavedMinor,lastEventId=old.lastEventId,revision=old.revision+1))
            holding?.let{set(ref("accounts",it.id),it)}
        }}
        d.accounts.filter{it.type!="savings"}.sortedBy{it.parentId==null}.forEach{ref("accounts",it.id).set(it)}
        val initialCash=ref("accounts","acc_cash").get()
        if(initialCash.exists)ref("accounts","acc_cash").set(initialCash.data<Account>().copy(archived=true))
    }
    private suspend fun verifyResume(d:FinanceData) {
        fun intact(value:Boolean){require(value){MR.strings.core_restore_target_changed.text()}}
        suspend fun ids(name:String,allowed:Set<String>){intact(root.collection(name).get().documents.all{it.id in allowed})}
        ids("accounts",d.accounts.map{it.id}.toSet()+"acc_cash");ids("categories",d.categories.map{it.id}.toSet()+defaultCategories.map{it.id})
        ids("transactions",d.transactions.map{it.id}.toSet());ids("financialEvents",d.financialEvents.map{it.id}.toSet())
        ids("goals",d.goals.map{it.id}.toSet());ids("bills",d.bills.map{it.id}.toSet());ids("debts",d.debts.map{it.id}.toSet())
        ids("notes",d.notes.map{it.id}.toSet());ids("recurring",d.recurring.map{it.id}.toSet());ids("reimbursements",d.reimbursements.map{it.id}.toSet())
        ids("wishes",d.wishes.map{it.id}.toSet());ids("cards",d.cards.map{it.id}.toSet());ids("budgets",d.budgets.map{it.id}.toSet());ids("investments",d.investments.map{it.id}.toSet())
        val events=root.collection("financialEvents").get().documents.map{it.data<FinancialEvent>()}
        events.forEach{intact(it in d.financialEvents)}
        root.collection("transactions").get().documents.forEach{intact(it.data<Transaction>() in d.transactions)}
        root.collection("accounts").get().documents.forEach{snapshot->val old=snapshot.data<Account>();if(old.id=="acc_cash")intact(old.openingBalanceMinor==0L) else {val value=d.accounts.single{it.id==old.id};intact(old==value || old==value.copy(archived=false))}}
        root.collection("categories").get().documents.forEach{val value=it.data<Category>();intact(value in d.categories || defaultCategories.any{it.copy(name=value.name)==value})}
        root.collection("wishes").get().documents.forEach{intact(it.data<Wish>() in d.wishes)}
        root.collection("cards").get().documents.forEach{intact(it.data<WalletCard>() in d.cards)}
        root.collection("budgets").get().documents.forEach{intact(it.data<Budget>() in d.budgets)}
        root.collection("investments").get().documents.forEach{intact(it.data<Investment>() in d.investments)}
        root.collection("goals").get().documents.forEach{snapshot->
            val old=snapshot.data<Goal>();val value=d.goals.single{it.id==old.id}
            intact(old==value.copy(savedMinor=old.savedMinor,holdingAccountId=old.holdingAccountId,legacySavedMinor=old.legacySavedMinor,revision=old.revision,lastEventId=old.lastEventId,archived=old.archived))
            intact(!old.archived || value.archived)
            val legacy=if(value.holdingAccountId==null)value.savedMinor else value.legacySavedMinor
            val movement=events.filter{it.referenceId==old.id && it.kind in listOf("goal_deposit","goal_withdrawal")}.fold(0L){sum,e->checkedAdd(sum,if(e.kind=="goal_deposit")e.amountMinor else -e.amountMinor)}
            intact(old.savedMinor==checkedAdd(legacy,movement) && old.holdingAccountId==value.holdingAccountId)
        }
        root.collection("debts").get().documents.forEach{snapshot->val old=snapshot.data<Debt>();val value=d.debts.single{it.id==old.id};intact(old==value.copy(paidMinor=old.paidMinor,lastEventId=old.lastEventId,revision=old.revision,archived=old.archived));intact(!old.archived || value.archived);intact(old.paidMinor==events.filter{it.kind=="debt_payment" && it.referenceId==old.id}.fold(0L){s,e->checkedAdd(s,e.amountMinor)})}
        root.collection("bills").get().documents.forEach{snapshot->val old=snapshot.data<Bill>();val value=d.bills.single{it.id==old.id};intact(old==value.copy(amountMinor=old.amountMinor,currency=old.currency,accountId=old.accountId,categoryId=old.categoryId,dueDate=old.dueDate,paidAt=old.paidAt,lastPaymentId=old.lastPaymentId,revision=old.revision,archived=old.archived));intact(!old.archived || value.archived)
            val event=events.find{it.id==old.lastPaymentId};if(event==null)intact(old.amountMinor==value.amountMinor && old.accountId==value.accountId && old.currency==value.currency && old.categoryId==value.categoryId) else {val tx=d.transactions.single{it.id==event.transactionId};intact(old.amountMinor==event.amountMinor && old.accountId==event.accountId && old.currency==event.currency && old.categoryId==tx.categoryId || old==value.copy(revision=old.revision,lastPaymentId=old.lastPaymentId))}}
        root.collection("notes").get().documents.forEach{snapshot->val old=snapshot.data<FinanceNote>();val value=d.notes.single{it.id==old.id};intact(old==value.copy(accountId=old.accountId,items=old.items,lastPaymentId=old.lastPaymentId,revision=old.revision,archived=old.archived));intact(!old.archived || value.archived)
            intact(old.items==value.items.map{i->if(i.paymentId!=null && events.none{it.id==i.paymentId})i.copy(paymentId=null) else i});intact(old.accountId==value.accountId || events.any{it.id==old.lastPaymentId && it.accountId==old.accountId})}
        root.collection("reimbursements").get().documents.forEach{snapshot->val old=snapshot.data<Reimbursement>();val value=d.reimbursements.single{it.id==old.id};val received=events.find{it.kind=="reimbursement" && it.referenceId==old.id};intact(old==value.copy(receivedAt=received?.createdAt ?: 0,receivedAccountId=received?.accountId,revision=old.revision))}
        root.collection("recurring").get().documents.forEach{snapshot->val old=snapshot.data<RecurringTransaction>();val value=d.recurring.single{it.id==old.id};intact(old==value.copy(type=old.type,amountMinor=old.amountMinor,currency=old.currency,accountId=old.accountId,categoryId=old.categoryId,nextDate=old.nextDate,lastEventId=old.lastEventId,active=old.active,revision=old.revision));intact(!old.active || value.active)
            val event=events.find{it.id==old.lastEventId};val tx=event?.transactionId?.let{id->d.transactions.single{it.id==id}};intact((old.type==value.type && old.amountMinor==value.amountMinor && old.currency==value.currency && old.accountId==value.accountId && old.categoryId==value.categoryId) || (tx!=null && old.type==tx.type && old.amountMinor==tx.amountMinor && old.currency==tx.currency && old.accountId==tx.accountId && old.categoryId==tx.categoryId))}
    }
    private suspend fun replayEvent(event:FinancialEvent,d:FinanceData) {
        val eventDoc=ref("financialEvents",event.id)
        db.runTransaction {
            if(get(eventDoc).exists)return@runTransaction
            val t=event.transactionId?.let{id->d.transactions.single{it.id==id}}
            when(event.kind){
                "bill"->{val doc=ref("bills",event.referenceId);val old=get(doc).data<Bill>();val template=d.bills.single{it.id==event.referenceId}
                    set(doc,old.copy(amountMinor=event.amountMinor,currency=event.currency,accountId=event.accountId,categoryId=requireNotNull(t).categoryId,
                        dueDate=if(template.frequency=="once")event.periodDate else nextOccurrence(event.periodDate,template.frequency,anchorDay=template.anchorDay),
                        paidAt=if(template.frequency=="once")event.createdAt else 0,lastPaymentId=event.id,revision=old.revision+1))}
                "debt_initial"->{val value=d.debts.single{it.id==event.referenceId};set(ref("debts",value.id),value.copy(paidMinor=0,lastEventId=null,revision=0,archived=false))}
                "debt_payment"->{val doc=ref("debts",event.referenceId);val old=get(doc).data<Debt>();set(doc,old.copy(paidMinor=checkedAdd(old.paidMinor,event.amountMinor),lastEventId=event.id,revision=old.revision+1))}
                "reimbursement"->{val doc=ref("reimbursements",event.referenceId);val old=get(doc).data<Reimbursement>();set(doc,old.copy(receivedAt=event.createdAt,receivedAccountId=event.accountId,revision=old.revision+1))}
                "recurring"->{val doc=ref("recurring",event.referenceId);val old=get(doc).data<RecurringTransaction>();set(doc,old.copy(type=requireNotNull(t).type,amountMinor=event.amountMinor,currency=event.currency,accountId=event.accountId,categoryId=t.categoryId,nextDate=nextOccurrence(event.periodDate,old.frequency,old.interval,old.anchorDay),lastEventId=event.id,active=false,revision=old.revision+1))}
                "bundle"->{val doc=ref("notes",event.referenceId);val old=get(doc).data<FinanceNote>();val original=d.notes.single{it.id==event.referenceId}
                    set(doc,old.copy(accountId=event.accountId,items=old.items.map{i->if(i.id in event.itemIds)original.items.single{it.id==i.id} else i},lastPaymentId=event.id,revision=old.revision+1))}
                "goal_deposit","goal_withdrawal"->{val doc=ref("goals",event.referenceId);val old=get(doc).data<Goal>();val original=d.goals.single{it.id==event.referenceId}
                    val holding=d.accounts.single{it.id==original.holdingAccountId};val holdingDoc=ref("accounts",holding.id);val exists=get(holdingDoc).exists
                    if(!exists)set(holdingDoc,holding.copy(archived=false))
                    set(doc,old.copy(savedMinor=checkedAdd(old.savedMinor,if(event.kind=="goal_deposit")event.amountMinor else -event.amountMinor),holdingAccountId=holding.id,
                        legacySavedMinor=if(old.holdingAccountId==null)old.savedMinor else old.legacySavedMinor,lastEventId=event.id,revision=old.revision+1))}
                else->error("Jenis riwayat tidak didukung")
            }
            set(eventDoc,event)
            t?.let{set(ref("transactions",it.id),it)}
        }
    }
}
