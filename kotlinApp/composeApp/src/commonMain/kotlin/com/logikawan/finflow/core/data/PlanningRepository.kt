package com.logikawan.finflow.core.data

import com.logikawan.finflow.core.model.*
import com.logikawan.finflow.core.util.*
import dev.gitlive.firebase.firestore.DocumentReference
import dev.gitlive.firebase.firestore.FirebaseFirestore
import com.logikawan.finflow.core.data.FinanceTransaction as FirestoreTransaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import com.logikawan.finflow.i18n.*
import kotlinx.datetime.LocalDate

class PlanningRepository(private val db: FinanceDatabase, private val root: FinanceDocument) {
    constructor(db:FirebaseFirestore,root:DocumentReference):this(FinanceDatabase(remote=db),root.path)
    private constructor(db:FinanceDatabase,path:String):this(db,db.document(path))
    private fun collection(name: String) = root.collection(name)
    private inline fun <reified T> records(name: String) = collection(name).snapshots.map { s -> s.documents.map { it.data<T>() } }

    fun observe(): Flow<FinanceData> {
        val finance = combine(records<Bill>("bills"), records<Debt>("debts"), records<Reimbursement>("reimbursements"), records<FinancialEvent>("financialEvents")) { b, d, r, e ->
            FinanceData(bills = b.sortedBy { it.dueDate }, debts = d.sortedByDescending { it.createdAt }, reimbursements = r, financialEvents = e.sortedByDescending { it.createdAt })
        }
        val personal = combine(records<Wish>("wishes"), records<WalletCard>("cards"), records<FinanceNote>("notes")) { w, c, n ->
            FinanceData(wishes = w.sortedByDescending { it.createdAt }, cards = c, notes = n.sortedWith(compareByDescending<FinanceNote> { it.pinned }.thenByDescending { it.updatedAt }))
        }
        return combine(finance, personal, records<RecurringTransaction>("recurring")) { f, p, r ->
            f.copy(wishes = p.wishes, cards = p.cards, notes = p.notes, recurring = r.sortedBy { it.nextDate }, featuresReady = true)
        }.catch { e ->
            if (e is CancellationException) throw e
            emit(FinanceData(featuresReady = false, featuresError = e.message ?: planText(MR.strings.plan_err_features_load)))
        }
    }

    private fun validAccount(account: Account, currency: String, active: Boolean = true) {
        require((!active || !account.archived) && account.currency == currency && account.type!="savings") { planText(MR.strings.plan_err_wallet_choice) }
    }
    private fun paymentDate(date: String) {
        LocalDate.parse(date)
        require(date <= today().toString()) { planText(MR.strings.plan_err_payment_date_future) }
    }
    private fun eventId(prefix: String, id: String, suffix: String = "") = "$prefix-$id$suffix"

    // Event dan jurnal selalu disimpan/dibatalkan dalam transaksi Firestore yang sama.
    private fun FirestoreTransaction.stageEvent(event: FinancialEvent, type: String, category: String, note: String = "", toAccountId: String? = null) {
        val tx = event.transactionId?.let {
            Transaction(id = it, type = type, amountMinor = event.amountMinor, currency = event.currency,
                accountId = event.accountId, categoryId = category, title = event.title, description = note, date = event.date,
                createdAt = event.createdAt, updatedAt = event.createdAt, source = event.kind, sourceId = event.referenceId,
                toAccountId = toAccountId, receivedAmountMinor = if(toAccountId!=null)event.amountMinor else null)
        }
        tx?.let { FinanceRepository.validateTransaction(it) }
        set(collection("financialEvents").document(event.id), event)
        tx?.let { set(collection("transactions").document(it.id), it) }
    }

    suspend fun saveBill(value: Bill) {
        PlanningValidation.bill(value)
        val ref = collection("bills").document(value.id)
        db.runTransaction {
            val snapshot = get(ref)
            val account = get(collection("accounts").document(value.accountId)).data<Account>()
            val category = get(collection("categories").document(value.categoryId)).data<Category>()
            validAccount(account, value.currency); require(category.type == "expense")
            val previous = if (snapshot.exists) snapshot.data<Bill>() else null
            if (previous != null) {
                require(previous.revision == value.revision) { planText(MR.strings.plan_err_bill_changed) }
                require(value.paidAt == previous.paidAt && value.lastPaymentId == previous.lastPaymentId)
            } else require(value.paidAt == 0L && value.lastPaymentId == null)
            if (previous != null && previous.paidAt > 0) require(value.amountMinor == previous.amountMinor && value.currency == previous.currency &&
                value.accountId == previous.accountId && value.categoryId == previous.categoryId && value.dueDate == previous.dueDate && value.frequency == previous.frequency) { planText(MR.strings.plan_err_bill_paid_edit) }
            set(ref, value.copy(anchorDay = if (previous != null && previous.dueDate == value.dueDate) previous.anchorDay else LocalDate.parse(value.dueDate).dayOfMonth,
                revision = (previous?.revision ?: -1) + 1, createdAt = previous?.createdAt ?: value.createdAt))
        }
    }

    suspend fun archiveBill(id: String) {
        val ref = collection("bills").document(id)
        db.runTransaction { val current = get(ref).data<Bill>(); set(ref, current.copy(archived = true, revision = current.revision + 1)) }
    }

    suspend fun payBill(id: String, dueDate: String, date: String): Boolean {
        paymentDate(date)
        val ref = collection("bills").document(id)
        val paymentId = eventId("bill", id, "-$dueDate")
        return db.runTransaction {
            val current = get(ref).data<Bill>()
            val exists = get(collection("financialEvents").document(paymentId)).exists
            val account = get(collection("accounts").document(current.accountId)).data<Account>()
            if (exists) false else {
                require(!current.archived && current.paidAt == 0L && current.dueDate == dueDate) { planText(MR.strings.plan_err_bill_paid_or_changed) }
                validAccount(account, current.currency)
                val event = FinancialEvent(paymentId, "bill", id, current.title, current.amountMinor, current.currency,
                    current.accountId, date, dueDate, previousEventId = current.lastPaymentId)
                val nextDate = if (current.frequency == "once") dueDate else nextOccurrence(dueDate, current.frequency, anchorDay = current.anchorDay)
                stageEvent(event, "expense", current.categoryId, current.note)
                set(ref, current.copy(dueDate = nextDate, paidAt = if (current.frequency == "once") event.createdAt else 0,
                    lastPaymentId = event.id, revision = current.revision + 1))
                true
            }
        }
    }

    suspend fun saveDebt(value: Debt) {
        PlanningValidation.debt(value)
        val ref = collection("debts").document(value.id)
        db.runTransaction {
            val snapshot = get(ref)
            val account = get(collection("accounts").document(value.accountId)).data<Account>()
            validAccount(account, value.currency, value.includeBalance)
            if (snapshot.exists) {
                val old = snapshot.data<Debt>()
                require(value.revision == old.revision) { planText(MR.strings.plan_err_debt_changed) }
                require(value.amountMinor == old.amountMinor && value.currency == old.currency && value.accountId == old.accountId &&
                    value.direction == old.direction && value.includeBalance == old.includeBalance && value.issueDate == old.issueDate) {
                    planText(MR.strings.plan_err_debt_locked)
                }
                set(ref, value.copy(paidMinor = old.paidMinor, lastEventId = old.lastEventId, revision = old.revision + 1, createdAt = old.createdAt))
            } else {
                require(value.paidMinor == 0L && value.lastEventId == null)
                if (value.includeBalance) {
                    val initial = FinancialEvent(eventId("debt-initial", value.id), "debt_initial", value.id,
                        if (value.direction == "borrowed") planText(MR.strings.plan_event_borrowed_from,value.person) else planText(MR.strings.plan_event_lent_to,value.person),
                        value.amountMinor, value.currency, value.accountId, value.issueDate)
                    stageEvent(initial, if (value.direction == "borrowed") "income" else "expense",
                        if (value.direction == "borrowed") "other_income" else "other_expense", value.note)
                }
                set(ref, value.copy(revision = 0))
            }
        }
    }

    suspend fun payDebt(id: String, amount: Long, accountId: String, date: String, requestId: String): Boolean {
        paymentDate(date)
        val ref = collection("debts").document(id)
        val paymentId = eventId("debt-payment", requestId)
        return db.runTransaction {
            val current = get(ref).data<Debt>()
            val exists = get(collection("financialEvents").document(paymentId)).exists
            val account = get(collection("accounts").document(accountId)).data<Account>()
            if (exists) false else {
                require(!current.archived && amount in 1..checkedAdd(current.amountMinor, -current.paidMinor)) { planText(MR.strings.plan_err_installment_exceeds) }
                require(date >= current.issueDate) { planText(MR.strings.plan_err_installment_before_issue) }
                validAccount(account, current.currency, current.includeBalance)
                val event = FinancialEvent(paymentId, "debt_payment", id,
                    if (current.direction == "borrowed") planText(MR.strings.plan_event_pay_debt,current.person) else planText(MR.strings.plan_event_receivable_from,current.person),
                    amount, current.currency, accountId, date, transactionId = if (current.includeBalance) paymentId else null)
                stageEvent(event, if (current.direction == "borrowed") "expense" else "income",
                    if (current.direction == "borrowed") "other_expense" else "other_income", current.note)
                set(ref, current.copy(paidMinor = checkedAdd(current.paidMinor, amount), lastEventId = paymentId, revision = current.revision + 1))
                true
            }
        }
    }

    suspend fun deleteDebt(id: String) {
        val ref = collection("debts").document(id)
        val initialId = eventId("debt-initial", id)
        db.runTransaction {
            val current = get(ref).data<Debt>()
            val initial = get(collection("financialEvents").document(initialId))
            if (current.paidMinor > 0) set(ref, current.copy(archived = true, revision = current.revision + 1))
            else {
                delete(ref)
                if (initial.exists) {
                    delete(collection("financialEvents").document(initialId))
                    delete(collection("transactions").document(initialId))
                }
            }
        }
    }

    suspend fun saveWish(value: Wish) { PlanningValidation.wish(value); collection("wishes").document(value.id).set(value) }
    suspend fun deleteWish(id: String) { collection("wishes").document(id).delete() }
    suspend fun saveNote(value: FinanceNote) {
        PlanningValidation.note(value)
        val ref=collection("notes").document(value.id)
        db.runTransaction {
            val snapshot=get(ref)
            if(value.kind=="bundle")validAccount(get(collection("accounts").document(value.accountId!!)).data(),value.currency)
            val old=if(snapshot.exists)snapshot.data<FinanceNote>() else null
            if(old!=null) {
                require(value.revision==old.revision){planText(MR.strings.plan_err_bundle_changed_form)}
                val paid=old.items.filter{it.paymentId!=null}
                require(paid.all{it in value.items}){planText(MR.strings.plan_err_bundle_paid_items)}
                require(paid.isEmpty() || value.currency==old.currency && value.recordAs==old.recordAs && value.kind==old.kind)
                require(value.items.filter{it.paymentId!=null}.all{item->old.items.any{it.id==item.id && it.paymentId==item.paymentId}})
            } else require(value.items.none{it.paymentId!=null})
            set(ref,value.copy(createdAt=old?.createdAt ?: value.createdAt,updatedAt=nowMillis(),revision=(old?.revision ?: -1)+1,lastPaymentId=old?.lastPaymentId))
        }
    }
    suspend fun deleteNote(id:String) {
        val ref=collection("notes").document(id)
        db.runTransaction {
            val current=get(ref).data<FinanceNote>()
            if(current.items.any{it.paymentId!=null})set(ref,current.copy(archived=true,revision=current.revision+1,updatedAt=nowMillis()))
            else delete(ref)
        }
    }

    suspend fun payBundle(id:String,itemIds:List<String>,accountId:String,date:String,requestId:String,expectedRevision:Int):Int {
        paymentDate(date)
        require(itemIds.isNotEmpty() && itemIds.size<=MAX_BUNDLE_ITEMS && itemIds.distinct().size==itemIds.size)
        val note=collection("notes").document(id).get().data<FinanceNote>()
        val groups=if(note.recordAs=="single")listOf(itemIds) else itemIds.map{listOf(it)}
        var recorded=0
        for((groupIndex,group) in groups.withIndex()) {
            val paymentId="bundle-$requestId"+if(note.recordAs=="items")"-${group.single()}" else ""
            try {
                val inserted=db.runTransaction {
                    val ref=collection("notes").document(id)
                    val current=get(ref).data<FinanceNote>()
                    val exists=get(collection("financialEvents").document(paymentId)).exists
                    val account=get(collection("accounts").document(accountId)).data<Account>()
                    val categoryRef=collection("categories").document("notes")
                    val category=get(categoryRef)
                    if(exists)return@runTransaction false
                    require(current.revision==expectedRevision+groupIndex){planText(MR.strings.plan_err_bundle_changed_confirm)}
                    require(current.kind=="bundle" && !current.archived && current.recordAs==note.recordAs){planText(MR.strings.plan_err_bundle_changed_archived)}
                    val selected=group.map{itemId->current.items.singleOrNull{it.id==itemId} ?: error(planText(MR.strings.plan_err_item_not_found))}
                    require(selected.none{it.paymentId!=null}){planText(MR.strings.plan_err_item_already_paid)}
                    validAccount(account,current.currency)
                    val total=selected.fold(0L){sum,item->checkedAdd(sum,item.amountMinor)}
                    require(total in 1..FinanceRepository.MAX_MONEY)
                    if(category.exists)require(category.data<Category>().type=="expense")
                    val event=FinancialEvent(paymentId,"bundle",id,if(current.recordAs=="single")planText(MR.strings.plan_event_bundle,current.title) else selected.single().name,
                        total,current.currency,accountId,date,previousEventId=current.lastPaymentId,itemIds=group)
                    if(!category.exists)set(categoryRef,Category("notes",planText(MR.strings.plan_kind_bundle),"expense",icon="checklist"))
                    stageEvent(event,"expense","notes",current.body)
                    set(ref,current.copy(accountId=accountId,items=current.items.map{if(it.id in group)it.copy(checked=true,paymentId=paymentId) else it},
                        lastPaymentId=paymentId,revision=current.revision+1,updatedAt=nowMillis()))
                    true
                }
                if(inserted)recorded++
            } catch(e:CancellationException){throw e}
            catch(e:Exception){if(recorded>0)throw IllegalStateException(planText(MR.strings.plan_err_partial_recorded,recorded),e) else throw e}
        }
        return recorded
    }

    suspend fun moveGoal(id:String,amount:Long,accountId:String,date:String,requestId:String,withdraw:Boolean):Boolean {
        paymentDate(date)
        require(amount in 1..FinanceRepository.MAX_MONEY)
        val paymentId="goal-$requestId"
        if(collection("financialEvents").document(paymentId).get().exists)return false
        val ref=collection("goals").document(id)
        val goal=ref.get().data<Goal>()
        val holdingId=goal.holdingAccountId ?: "goal-$id"
        val holdingRef=collection("accounts").document(holdingId)
        // Penolakan saldo kurang melengkapi validasi nominal; jurnal transfer tetap atomik.
        if(!withdraw) {
            val source=collection("accounts").document(accountId).get().data<Account>()
            val txs=collection("transactions").get().documents.map{it.data<Transaction>()}
            require(FinanceData(transactions=txs).balance(source)>=amount){planText(MR.strings.plan_err_source_balance)}
        }
        return db.runTransaction {
            val current=get(ref).data<Goal>()
            val exists=get(collection("financialEvents").document(paymentId)).exists
            val wallet=get(collection("accounts").document(accountId)).data<Account>()
            val holding=get(holdingRef)
            if(exists)return@runTransaction false
            require(!current.archived && current.holdingAccountId==goal.holdingAccountId){planText(MR.strings.plan_err_goal_changed)}
            require(wallet.type!="savings" && wallet.id!=holdingId){planText(MR.strings.plan_err_pick_regular_wallet)}
            validAccount(wallet,current.currency)
            val legacy=if(current.holdingAccountId==null)current.savedMinor else current.legacySavedMinor
            if(withdraw)require(current.holdingAccountId!=null && amount<=current.savedMinor-legacy){planText(MR.strings.plan_err_withdraw_exceeds)}
            if(holding.exists){val account=holding.data<Account>();require(account.type=="savings" && account.goalId==id && account.currency==current.currency && !account.archived)}
            else require(!withdraw)
            val next=checkedAdd(current.savedMinor,if(withdraw)-amount else amount)
            require(next in 0..FinanceRepository.MAX_MONEY)
            val event=FinancialEvent(paymentId,if(withdraw)"goal_withdrawal" else "goal_deposit",id,
                if(withdraw)planText(MR.strings.plan_event_goal_withdraw,current.name) else planText(MR.strings.plan_event_goal_deposit,current.name),amount,current.currency,if(withdraw)holdingId else accountId,date)
            if(!holding.exists)set(holdingRef,Account(id=holdingId,name=current.name,type="savings",currency=current.currency,icon="savings",goalId=id))
            stageEvent(event,"transfer","transfer",toAccountId=if(withdraw)accountId else holdingId)
            set(ref,current.copy(savedMinor=next,holdingAccountId=holdingId,legacySavedMinor=legacy,revision=current.revision+1,lastEventId=event.id))
            true
        }
    }

    suspend fun saveCard(value: WalletCard) {
        PlanningValidation.card(value)
        db.runTransaction {
            if (value.accountId.isNotBlank()) get(collection("accounts").document(value.accountId)).data<Account>()
            set(collection("cards").document(value.id), value)
        }
    }
    suspend fun deleteCard(id: String) { collection("cards").document(id).delete() }

    suspend fun saveReimbursement(value: Reimbursement) {
        PlanningValidation.reimbursement(value)
        val ref = collection("reimbursements").document(value.id)
        db.runTransaction {
            val expense = get(collection("transactions").document(value.expenseId)).data<Transaction>()
            val existing = get(ref)
            require(expense.type == "expense" && !expense.scheduled && value.amountMinor <= expense.amountMinor) { planText(MR.strings.plan_err_claim_source) }
            require(value.currency == expense.currency && value.accountId == expense.accountId)
            if (existing.exists) {
                val old = existing.data<Reimbursement>()
                require(value.revision == old.revision && old.receivedAt == 0L) { planText(MR.strings.plan_err_claim_changed) }
                set(ref, value.copy(receivedAt = 0, receivedAccountId = null, revision = old.revision + 1, createdAt = old.createdAt))
            } else {
                require(value.receivedAt == 0L && value.receivedAccountId == null)
                set(ref, value.copy(revision = 0))
            }
        }
    }

    suspend fun receiveReimbursement(id: String, accountId: String, date: String): Boolean {
        paymentDate(date)
        val ref = collection("reimbursements").document(id)
        val paymentId = eventId("reimburse", id)
        return db.runTransaction {
            val current = get(ref).data<Reimbursement>()
            val exists = get(collection("financialEvents").document(paymentId)).exists
            val account = get(collection("accounts").document(accountId)).data<Account>()
            if (exists) false else {
                require(current.receivedAt == 0L) { planText(MR.strings.plan_err_claim_received) }
                validAccount(account, current.currency)
                val event = FinancialEvent(paymentId, "reimbursement", id, planText(MR.strings.plan_event_reimburse,current.title), current.amountMinor, current.currency, accountId, date)
                stageEvent(event, "income", "other_income", current.note)
                set(ref, current.copy(receivedAt = event.createdAt, receivedAccountId = accountId, revision = current.revision + 1))
                true
            }
        }
    }

    suspend fun deleteReimbursement(id: String) {
        val ref = collection("reimbursements").document(id)
        db.runTransaction {
            val current = get(ref).data<Reimbursement>()
            require(current.receivedAt == 0L) { planText(MR.strings.plan_err_claim_cancel_first) }
            delete(ref)
        }
    }

    suspend fun saveRecurring(value: RecurringTransaction) {
        PlanningValidation.recurring(value)
        val ref = collection("recurring").document(value.id)
        db.runTransaction {
            val snapshot = get(ref)
            val account = get(collection("accounts").document(value.accountId)).data<Account>()
            val category = get(collection("categories").document(value.categoryId)).data<Category>()
            validAccount(account, value.currency); require(category.type == value.type)
            if (snapshot.exists) {
                val old = snapshot.data<RecurringTransaction>()
                require(old.revision == value.revision) { planText(MR.strings.plan_err_schedule_processed) }
                require(old.startDate == value.startDate)
                set(ref, value.copy(revision = old.revision + 1, createdAt = old.createdAt, lastEventId = old.lastEventId,
                    anchorDay = if (value.nextDate == old.nextDate) old.anchorDay else value.anchorDay))
            } else {
                require(value.startDate >= today().toString() && value.nextDate == value.startDate) { planText(MR.strings.plan_err_schedule_start) }
                set(ref, value.copy(revision = 0, lastEventId = null))
            }
        }
    }
    suspend fun archiveRecurring(id: String) {
        val ref = collection("recurring").document(id)
        db.runTransaction { val current = get(ref).data<RecurringTransaction>(); set(ref, current.copy(active = false, revision = current.revision + 1)) }
    }

    suspend fun processDueRecurring(limit: Int = 100): Int {
        require(limit in 1..100)
        val ids = collection("recurring").get().documents.map { it.data<RecurringTransaction>() }.filter { it.active }.map { it.id }
        var count = 0
        for (id in ids) {
            while (count < limit) {
                val processed = db.runTransaction {
                    val ref = collection("recurring").document(id)
                    val current = get(ref).data<RecurringTransaction>()
                    if (!current.active || current.nextDate > today().toString()) return@runTransaction false
                    if (current.endDate != null && current.nextDate > current.endDate) {
                        set(ref, current.copy(active = false, revision = current.revision + 1)); return@runTransaction false
                    }
                    val paymentId = eventId("recurring", id, "-${current.nextDate}")
                    val exists = get(collection("financialEvents").document(paymentId)).exists
                    val account = get(collection("accounts").document(current.accountId)).data<Account>()
                    validAccount(account, current.currency)
                    val next = nextOccurrence(current.nextDate, current.frequency, current.interval, current.anchorDay)
                    if (!exists) {
                        val event = FinancialEvent(paymentId, "recurring", id, current.title, current.amountMinor,
                            current.currency, current.accountId, current.nextDate)
                        stageEvent(event, current.type, current.categoryId, current.note)
                    }
                    set(ref, current.copy(nextDate = next, lastEventId = paymentId, revision = current.revision + 1,
                        active = current.endDate == null || next <= current.endDate))
                    true
                }
                if (!processed) break
                count++
            }
            if (count >= limit) break
        }
        return count
    }

    suspend fun reverseEvent(id: String) {
        val eventRef = collection("financialEvents").document(id)
        db.runTransaction {
            val snapshot = get(eventRef)
            if (!snapshot.exists) return@runTransaction
            val event = snapshot.data<FinancialEvent>()
            require(!get(collection("reimbursements").document(id)).exists){planText(MR.strings.plan_err_delete_claim_first)}
            when (event.kind) {
                "bundle" -> {
                    val ref=collection("notes").document(event.referenceId)
                    val current=get(ref).data<FinanceNote>()
                    require(event.itemIds.all{id->current.items.any{it.id==id && it.paymentId==event.id}}){planText(MR.strings.plan_err_bundle_item_changed)}
                    set(ref,current.copy(items=current.items.map{if(it.id in event.itemIds)it.copy(paymentId=null,checked=false) else it},archived=false,
                        lastPaymentId=if(current.lastPaymentId==id)event.previousEventId else current.lastPaymentId,revision=current.revision+1,updatedAt=nowMillis()))
                }
                "goal_deposit", "goal_withdrawal" -> {
                    val ref=collection("goals").document(event.referenceId)
                    val current=get(ref).data<Goal>()
                    val holdingRef=collection("accounts").document(requireNotNull(current.holdingAccountId))
                    val holding=get(holdingRef).data<Account>()
                    val next=checkedAdd(current.savedMinor,if(event.kind=="goal_deposit")-event.amountMinor else event.amountMinor)
                    require(next>=current.legacySavedMinor && next<=FinanceRepository.MAX_MONEY){planText(MR.strings.plan_err_cancel_withdraw_first)}
                    if(holding.archived)set(holdingRef,holding.copy(archived=false))
                    set(ref,current.copy(savedMinor=next,revision=current.revision+1,archived=false,lastEventId=id))
                }
                "bill" -> {
                    val ref = collection("bills").document(event.referenceId)
                    val current = get(ref).data<Bill>()
                    require(current.lastPaymentId == id) { planText(MR.strings.plan_err_cancel_last_bill_payment) }
                    set(ref, current.copy(dueDate = event.periodDate, paidAt = 0, lastPaymentId = event.previousEventId, revision = current.revision + 1))
                }
                "debt_payment" -> {
                    val ref = collection("debts").document(event.referenceId)
                    val current = get(ref).data<Debt>()
                    set(ref, current.copy(paidMinor = checkedAdd(current.paidMinor, -event.amountMinor),
                        lastEventId = id, archived = false, revision = current.revision + 1))
                }
                "debt_initial" -> {
                    val ref = collection("debts").document(event.referenceId)
                    require(get(ref).data<Debt>().paidMinor == 0L) { planText(MR.strings.plan_err_cancel_installments_first) }
                    delete(ref)
                }
                "reimbursement" -> {
                    val ref = collection("reimbursements").document(event.referenceId)
                    val current = get(ref).data<Reimbursement>()
                    set(ref, current.copy(receivedAt = 0, receivedAccountId = null, revision = current.revision + 1))
                }
                "recurring" -> Unit
                else -> error(planText(MR.strings.plan_err_payment_kind_invalid))
            }
            delete(eventRef)
            event.transactionId?.let { delete(collection("transactions").document(it)) }
        }
    }
}

