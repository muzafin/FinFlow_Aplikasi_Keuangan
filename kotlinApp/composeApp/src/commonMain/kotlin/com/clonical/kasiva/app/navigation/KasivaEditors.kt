package com.clonical.kasiva.app.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.clonical.kasiva.core.data.FinanceRepository
import com.clonical.kasiva.core.model.Account
import com.clonical.kasiva.core.model.Budget
import com.clonical.kasiva.core.model.Category
import com.clonical.kasiva.core.model.FinanceData
import com.clonical.kasiva.core.model.Goal
import com.clonical.kasiva.core.model.Investment
import com.clonical.kasiva.core.model.Transaction
import com.clonical.kasiva.core.model.Profile
import com.clonical.kasiva.feature.account.AccountEditor
import com.clonical.kasiva.feature.budget.BudgetEditor
import com.clonical.kasiva.feature.goal.GoalEditor
import com.clonical.kasiva.feature.investment.InvestmentEditor
import com.clonical.kasiva.feature.settings.CategoryEditor
import com.clonical.kasiva.feature.settings.ProfileEditor
import com.clonical.kasiva.feature.transaction.RecurringFormEditor
import com.clonical.kasiva.feature.transaction.TransactionEditor
import com.clonical.kasiva.feature.planning.*
import com.clonical.kasiva.i18n.*
import com.clonical.kasiva.core.i18n.text

@Composable
fun EntityEditor(
    editor: Editor, data: FinanceData, busy: Boolean, dismiss: () -> Unit,
    saveTx: (Transaction) -> Unit, saveAccount: (Account) -> Unit,
    saveBudget: (Budget) -> Unit, saveGoal: (Goal) -> Unit,
    saveInvestment: (Investment) -> Unit, saveCategory: (Category) -> Unit,
    saveProfile: (Profile) -> Unit,
    delete: (String, suspend () -> Unit) -> Unit, repo: FinanceRepository, savePlanning: (suspend () -> Unit) -> Unit,
    onScan: (() -> Unit)? = null,
) {
    when (editor) {
        is Editor.GoalMovement -> com.clonical.kasiva.feature.goal.GoalMovementEditor(editor.value,data,editor.withdraw,busy,dismiss,{request->savePlanning{repo.planner.moveGoal(editor.value.id,request.amountMinor,request.accountId,request.date,request.requestId,editor.withdraw)}})
        is Editor.AdjustWallet -> com.clonical.kasiva.feature.account.AccountBalanceEditor(editor.value,data,busy,dismiss,saveTx)
        is Editor.BillForm -> BillEditor(editor.value, data, busy, dismiss, { value -> savePlanning { repo.planner.saveBill(value) } },
            editor.value?.let { value -> { delete("arsip:tagihan") { repo.planner.archiveBill(value.id) } } })
        is Editor.DebtForm -> DebtEditor(editor.value, data, busy, dismiss, { value -> savePlanning { repo.planner.saveDebt(value) } },
            editor.value?.let { value -> { delete(if (value.paidMinor > 0) "arsip:hutang / piutang" else "batal:pencatatan hutang / piutang") { repo.planner.deleteDebt(value.id) } } })
        is Editor.WishForm -> WishEditor(editor.value, busy, dismiss, { value -> savePlanning { repo.planner.saveWish(value) } },
            editor.value?.let { value -> { delete("keinginan") { repo.planner.deleteWish(value.id) } } })
        is Editor.CardForm -> CardEditor(editor.value, data, busy, dismiss, { value -> savePlanning { repo.planner.saveCard(value) } },
            editor.value?.let { value -> { delete("catatan kartu") { repo.planner.deleteCard(value.id) } } })
        is Editor.NoteForm -> NoteEditor(editor.value, busy, dismiss, { value -> savePlanning { repo.planner.saveNote(value) } },
            editor.value?.let { value -> { delete("catatan") { repo.planner.deleteNote(value.id) } } }, data=data)
        is Editor.ClaimForm -> ReimbursementEditor(editor.value, data, busy, dismiss, { value -> savePlanning { repo.planner.saveReimbursement(value) } },
            editor.value?.let { value -> { delete("klaim reimburse") { repo.planner.deleteReimbursement(value.id) } } })
        is Editor.RepeatForm -> RecurringFormEditor(editor.value, data, busy, dismiss, { value -> savePlanning { repo.planner.saveRecurring(value); repo.planner.processDueRecurring() } },
            editor.value?.let { value -> { delete("arsip:jadwal berulang") { repo.planner.archiveRecurring(value.id) } } })
        is Editor.PayBill -> PaymentEditor(MR.strings.app_pay_bill.text(), editor.value.currency, editor.value.amountMinor, data, editor.value.accountId, busy, dismiss,
            { request -> savePlanning { repo.planner.payBill(editor.value.id, editor.value.dueDate, request.date) } }, selectAccount = false)
        is Editor.PayDebt -> PaymentEditor(if (editor.value.direction == "borrowed") MR.strings.app_pay_debt.text() else MR.strings.app_receive_receivable.text(), editor.value.currency,
            editor.value.amountMinor - editor.value.paidMinor, data, editor.value.accountId, busy, dismiss,
            { request -> savePlanning { repo.planner.payDebt(editor.value.id, request.amountMinor, request.accountId, request.date, request.requestId) } },
            partial = true, changesBalance = editor.value.includeBalance)
        is Editor.ReceiveClaim -> PaymentEditor(MR.strings.app_receive_reimburse.text(), editor.value.currency, editor.value.amountMinor, data, editor.value.accountId, busy, dismiss,
            { request -> savePlanning { repo.planner.receiveReimbursement(editor.value.id, request.accountId, request.date) } })
        is Editor.PayBundle -> PaymentEditor(MR.strings.app_pay_bundle.text(), editor.value.currency,
            editor.value.items.filter{it.id in editor.itemIds && it.paymentId==null}.fold(0L){sum,item->com.clonical.kasiva.core.util.checkedAdd(sum,item.amountMinor)},
            data,editor.value.accountId.orEmpty(),busy,dismiss,{request->savePlanning{
                repo.planner.payBundle(editor.value.id,editor.itemIds,request.accountId,request.date,request.requestId,editor.value.revision)
            }})
        is Editor.Tx -> TransactionEditor(editor.value, data, busy, dismiss, saveTx,
            editor.value?.takeUnless { editor.notificationImport || editor.aiDraft }?.let { tx -> { delete("catatan transaksi") { repo.deleteTransaction(tx) } } }, isDraft = editor.notificationImport || editor.aiDraft, startingScheduled = editor.scheduled, startingAccountId = editor.accountId, startingType = editor.type, onScan = onScan)
        is Editor.Wallet -> AccountEditor(editor.value, data, busy, dismiss, saveAccount)
        is Editor.Limit -> BudgetEditor(editor.value, data, busy, dismiss, saveBudget,
            editor.value?.let { value -> { delete("anggaran") { repo.deleteBudget(value.id) } } })
        is Editor.Target -> GoalEditor(editor.value, busy, dismiss, saveGoal,
            editor.value?.let { value -> { delete(if(value.holdingAccountId!=null)"arsip:target tabungan" else "target") { repo.deleteGoal(value.id) } } })
        is Editor.Asset -> InvestmentEditor(editor.value, busy, dismiss, saveInvestment,
            editor.value?.let { value -> { delete("investasi") { repo.deleteInvestment(value.id) } } })
        is Editor.Cat -> CategoryEditor(busy, dismiss, saveCategory, old=editor.value, data=data, initialParentId=editor.parentId)
        Editor.Person -> ProfileEditor(data.profile, busy, dismiss, saveProfile)
    }
}
