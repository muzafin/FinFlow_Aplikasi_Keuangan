package com.clonical.kasiva.core.model

import com.clonical.kasiva.i18n.*
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.data.FinanceRepository
import com.clonical.kasiva.core.util.*
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class FinanceBackup(val version:Int=1,val id:String=newId(),val createdAt:Long=nowMillis(),val ownerId:String,
    val data:FinanceData,val preferences:AppPreferences=AppPreferences())

object BackupCodec {
    private val json=Json{encodeDefaults=true;prettyPrint=true}
    fun encode(backup:FinanceBackup):String {validate(backup);return json.encodeToString(backup).also{require(it.encodeToByteArray().size<=5_000_000){MR.strings.core_error_backup_max_size.text()}}}
    fun decode(content:String):FinanceBackup {
        require(content.encodeToByteArray().size<=5_000_000){MR.strings.core_error_backup_max_size.text()}
        return json.decodeFromString<FinanceBackup>(content).also(::validate)
    }
    fun validate(backup:FinanceBackup) {
        require(backup.version==1 && backup.id.matches(Regex("[A-Za-z0-9-]{8,100}")) && backup.ownerId.isNotBlank()){MR.strings.core_error_backup_identity.text()}
        backup.preferences.validate()
        val d=backup.data
        val lists=listOf(d.accounts.map{it.id},d.categories.map{it.id},d.transactions.map{it.id},d.goals.map{it.id},d.budgets.map{it.id},
            d.bills.map{it.id},d.debts.map{it.id},d.recurring.map{it.id},d.reimbursements.map{it.id},d.notes.map{it.id},d.cards.map{it.id},d.wishes.map{it.id},d.investments.map{it.id},d.financialEvents.map{it.id})
        require(lists.sumOf{it.size}<=10000 && lists.all{ids->ids.distinct().size==ids.size && ids.all{it.isNotBlank() && it.length<=300 && '/' !in it}}){MR.strings.core_error_backup_duplicate_ids.text()}
        require(d.profile!=null && d.profile.name.isNotBlank() && d.profile.name.length<=100 && d.profile.baseCurrency.matches(Regex("[A-Z]{3}")))
        fun account(id:String,currency:String)=require(d.accounts.any{it.id==id && it.currency==currency}){MR.strings.core_error_backup_wallet_ref.text()}
        d.accounts.forEach{a->
            require(a.name.isNotBlank() && a.name.length<=100 && a.currency.matches(Regex("[A-Z]{3}")) && a.openingBalanceMinor in -FinanceRepository.MAX_MONEY..FinanceRepository.MAX_MONEY)
            require(a.type in listOf("cash","bank","ewallet","credit","investment","loan","savings") && a.icon.length in 1..100 && a.color in 0xFF000000..0xFFFFFFFF)
            a.parentId?.let{id->require(d.accounts.any{it.id==id && it.id!=a.id && it.currency==a.currency && it.parentId==null})}
            require(a.type!="savings" || a.openingBalanceMinor==0L && a.parentId==null && d.goals.any{it.id==a.goalId && it.holdingAccountId==a.id})
            d.balance(a)
        }
        d.categories.forEach{c->require(c.name.isNotBlank() && c.name.length<=100 && c.type in listOf("income","expense") && c.icon.length<=100 && (c.color==null || c.color in 0xFF000000..0xFFFFFFFF));c.parentId?.let{p->require(d.categories.any{it.id==p && it.id!=c.id && it.type==c.type && it.parentId==null})}}
        d.transactions.forEach{t->
            FinanceRepository.validateTransaction(t);account(t.accountId,t.currency)
            if(t.type=="transfer")require(d.accounts.any{it.id==t.toAccountId}) else require(d.categories.any{it.id==t.categoryId && it.type==t.type})
            t.subcategoryId?.let{s->require(d.categories.any{it.id==s && it.type==t.type && it.parentId==t.categoryId})}
            if(t.source in managedTransactionSources)require(d.financialEvents.any{it.id==t.id && it.kind==t.source && it.referenceId==t.sourceId})
        }
        d.financialEvents.forEach{e->
            require(e.kind in managedTransactionSources && e.amountMinor in 1..FinanceRepository.MAX_MONEY);account(e.accountId,e.currency);LocalDate.parse(e.date);LocalDate.parse(e.periodDate)
            val t=e.transactionId?.let{id->d.transactions.single{it.id==id}}
            require(e.transactionId==null && e.kind=="debt_payment" || t!=null && t.id==e.id && t.source==e.kind && t.sourceId==e.referenceId && t.accountId==e.accountId && t.amountMinor==e.amountMinor && t.currency==e.currency && t.date==e.date && !t.scheduled)
            when(e.kind){
                "bill"->require(d.bills.any{it.id==e.referenceId})
                "debt_initial","debt_payment"->require(d.debts.any{it.id==e.referenceId})
                "reimbursement"->require(d.reimbursements.any{it.id==e.referenceId})
                "recurring"->require(d.recurring.any{it.id==e.referenceId})
                "bundle"->{val n=d.notes.single{it.id==e.referenceId};val items=n.items.filter{it.paymentId==e.id};require(e.itemIds.toSet()==items.map{it.id}.toSet() && items.isNotEmpty() && items.fold(0L){s,i->checkedAdd(s,i.amountMinor)}==e.amountMinor)}
                "goal_deposit","goal_withdrawal"->{val g=d.goals.single{it.id==e.referenceId};require(t?.type=="transfer" && t.receivedAmountMinor==e.amountMinor && (if(e.kind=="goal_deposit")t.toAccountId==g.holdingAccountId else t.accountId==g.holdingAccountId))}
            }
        }
        d.bills.forEach{PlanningValidation.bill(it);account(it.accountId,it.currency);require(d.categories.any{c->c.id==it.categoryId && c.type=="expense"})}
        d.debts.forEach{debt->PlanningValidation.debt(debt);account(debt.accountId,debt.currency);require(d.financialEvents.filter{it.kind=="debt_payment" && it.referenceId==debt.id}.fold(0L){sum,e->checkedAdd(sum,e.amountMinor)}==debt.paidMinor);require(!debt.includeBalance || d.financialEvents.any{it.id=="debt-initial-${debt.id}"})}
        d.recurring.forEach{PlanningValidation.recurring(it);account(it.accountId,it.currency);require(d.categories.any{c->c.id==it.categoryId && c.type==it.type})}
        d.reimbursements.forEach{r->PlanningValidation.reimbursement(r);require(d.transactions.any{it.id==r.expenseId && it.type=="expense" && !it.scheduled && it.currency==r.currency && it.amountMinor>=r.amountMinor});require(r.receivedAt==0L || d.financialEvents.any{it.kind=="reimbursement" && it.referenceId==r.id})}
        d.notes.forEach{n->PlanningValidation.note(n);if(n.kind=="bundle"){account(n.accountId.orEmpty(),n.currency);require(d.accounts.none{it.id==n.accountId && it.type=="savings"})};n.items.filter{it.paymentId!=null}.forEach{i->require(d.financialEvents.any{it.id==i.paymentId && it.kind=="bundle" && it.referenceId==n.id && i.id in it.itemIds})}}
        d.cards.forEach{PlanningValidation.card(it);require(it.accountId.isEmpty() || d.accounts.any{a->a.id==it.accountId})};d.wishes.forEach{PlanningValidation.wish(it)}
        d.reimbursements.forEach{r->account(r.accountId,r.currency);r.receivedAccountId?.let{account(it,r.currency)}}
        d.budgets.forEach{require(it.limitMinor in 1..FinanceRepository.MAX_MONEY && it.period in listOf("monthly","weekly") && it.title.length<=100 && it.currency.matches(Regex("[A-Z]{3}")) && d.categories.any{c->c.id==it.categoryId && c.type=="expense"});budgetPeriod(it,backup.preferences.recordingDay)}
        d.investments.forEach{require(it.name.isNotBlank() && it.name.length<=100 && it.type in listOf("saham","reksaDana","crypto","emas","deposito","properti","p2pLending") && it.platform.length<=100 && it.notes.length<=4000 && it.investedMinor in 1..FinanceRepository.MAX_MONEY && it.currentMinor in 0..FinanceRepository.MAX_MONEY && it.dividendsMinor in 0..FinanceRepository.MAX_MONEY && it.currency.matches(Regex("[A-Z]{3}")))}
        d.goals.forEach{g->
            require(g.name.isNotBlank() && g.name.length<=100 && g.targetMinor in 1..FinanceRepository.MAX_MONEY && g.savedMinor in 0..FinanceRepository.MAX_MONEY && g.legacySavedMinor in 0..g.savedMinor)
            require(g.currency.matches(Regex("[A-Z]{3}")))
            require(g.bucketType in listOf("standard","emergency","locked") && g.protection in listOf("flexible","soft","hard"));if(g.targetDate.isNotBlank())LocalDate.parse(g.targetDate)
            val movements=d.financialEvents.filter{it.referenceId==g.id && it.kind in listOf("goal_deposit","goal_withdrawal")}.fold(0L){sum,e->checkedAdd(sum,if(e.kind=="goal_deposit")e.amountMinor else -e.amountMinor)}
            require(if(g.holdingAccountId==null)movements==0L else g.savedMinor-g.legacySavedMinor==movements && d.accounts.any{it.id==g.holdingAccountId && it.type=="savings" && it.goalId==g.id && d.balance(it)==movements}){MR.strings.core_error_backup_goal_balance.text()}
        }
        eventsForRestore(d)
    }

    fun eventsForRestore(data:FinanceData):List<FinancialEvent> {
        val pending=data.financialEvents.sortedWith(compareBy<FinancialEvent>{it.date}.thenBy{it.createdAt}).toMutableList()
        val done=mutableSetOf<String>();val ordered=mutableListOf<FinancialEvent>();val saved=data.goals.associate{it.id to if(it.holdingAccountId==null)it.savedMinor else it.legacySavedMinor}.toMutableMap()
        while(pending.isNotEmpty()) {
            val event=pending.firstOrNull{e->
                val previous=e.previousEventId
                (previous==null || previous in done || data.financialEvents.none{it.id==previous}) && when(e.kind){
                    "debt_payment"->data.debts.single{it.id==e.referenceId}.let{!it.includeBalance || "debt-initial-${it.id}" in done}
                    "reimbursement"->data.reimbursements.single{it.id==e.referenceId}.let{r->data.financialEvents.find{it.transactionId==r.expenseId}?.id.let{it==null || it in done}}
                    "goal_deposit"->saved.getValue(e.referenceId)<=FinanceRepository.MAX_MONEY-e.amountMinor
                    "goal_withdrawal"->saved.getValue(e.referenceId)-e.amountMinor>=data.goals.single{it.id==e.referenceId}.legacySavedMinor
                    else->true
                }
            } ?: error(MR.strings.core_error_backup_history_order.text())
            if(event.kind.startsWith("goal_"))saved[event.referenceId]=checkedAdd(saved.getValue(event.referenceId),if(event.kind=="goal_deposit")event.amountMinor else -event.amountMinor)
            done+=event.id;ordered+=event;pending.remove(event)
        }
        return ordered
    }

    /**
     * Preferensi yang menyimpan id dompet (dompet bawaan, urutan dompet, dompet Beranda) diterjemahkan ke id dompet hasil
     * pulihkan; dulu hanya defaultWalletId sehingga urutan & pilihan Beranda hilang (P1-03).
     */
    fun restoredPreferences(backup:FinanceBackup,preferences:AppPreferences):AppPreferences {
        val restored=restoreData(backup)
        val map=backup.data.accounts.indices.associate{backup.data.accounts[it].id to restored.accounts[it].id}
        return preferences.copy(defaultWalletId=map[preferences.defaultWalletId].orEmpty(),
            walletOrder=preferences.walletOrder.mapNotNull{map[it]}.distinct(),dashboardWalletIds=preferences.dashboardWalletIds.mapNotNull{map[it]}.distinct())
    }

    // ID dompet baru menghindari penimpaan dompet Kas bawaan akun tujuan.
    fun restoreData(backup:FinanceBackup):FinanceData {
        val d=backup.data
        val ids=d.accounts.sortedBy{it.id}.mapIndexed{index,a->a.id to if(a.type=="savings")"goal-${requireNotNull(a.goalId)}" else "restore-${backup.id.take(8)}-$index"}.toMap()
        fun id(value:String)=ids.getValue(value)
        return d.copy(accounts=d.accounts.map{it.copy(id=id(it.id),parentId=it.parentId?.let(::id))},
            transactions=d.transactions.map{it.copy(accountId=id(it.accountId),toAccountId=it.toAccountId?.let(::id))},
            goals=d.goals.map{it.copy(holdingAccountId=it.holdingAccountId?.let(::id))},bills=d.bills.map{it.copy(accountId=id(it.accountId))},debts=d.debts.map{it.copy(accountId=id(it.accountId))},
            cards=d.cards.map{it.copy(accountId=it.accountId.takeIf{v->v.isNotBlank()}?.let(::id).orEmpty())},notes=d.notes.map{it.copy(accountId=it.accountId?.let(::id))},
            reimbursements=d.reimbursements.map{it.copy(accountId=id(it.accountId),receivedAccountId=it.receivedAccountId?.let(::id))},recurring=d.recurring.map{it.copy(accountId=id(it.accountId))},financialEvents=d.financialEvents.map{it.copy(accountId=id(it.accountId))})
    }
}
