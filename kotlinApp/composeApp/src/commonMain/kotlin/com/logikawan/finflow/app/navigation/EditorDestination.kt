package com.logikawan.finflow.app.navigation

import com.logikawan.finflow.core.model.Account
import com.logikawan.finflow.core.model.Budget
import com.logikawan.finflow.core.model.Goal
import com.logikawan.finflow.core.model.Investment
import com.logikawan.finflow.core.model.Transaction

sealed interface Editor {
    data class BillForm(val value: com.logikawan.finflow.core.model.Bill? = null) : Editor
    data class DebtForm(val value: com.logikawan.finflow.core.model.Debt? = null) : Editor
    data class WishForm(val value: com.logikawan.finflow.core.model.Wish? = null) : Editor
    data class CardForm(val value: com.logikawan.finflow.core.model.WalletCard? = null) : Editor
    data class NoteForm(val value: com.logikawan.finflow.core.model.FinanceNote? = null) : Editor
    data class ClaimForm(val value: com.logikawan.finflow.core.model.Reimbursement? = null) : Editor
    data class RepeatForm(val value: com.logikawan.finflow.core.model.RecurringTransaction? = null) : Editor
    data class PayBill(val value: com.logikawan.finflow.core.model.Bill) : Editor
    data class PayDebt(val value: com.logikawan.finflow.core.model.Debt) : Editor
    data class ReceiveClaim(val value: com.logikawan.finflow.core.model.Reimbursement) : Editor
    data class PayBundle(val value: com.logikawan.finflow.core.model.FinanceNote, val itemIds: List<String>) : Editor
    data class Tx(val value: Transaction? = null, val notificationImport: Boolean = false, val onImported: (() -> Unit)? = null, val scheduled: Boolean = false, val accountId: String? = null, val type: String = "expense", val aiDraft: Boolean = false) : Editor
    data class AdjustWallet(val value: Account) : Editor
    data class GoalMovement(val value: Goal, val withdraw: Boolean) : Editor
    data class Wallet(val value: Account? = null) : Editor
    data class Limit(val value: Budget? = null) : Editor
    data class Target(val value: Goal? = null) : Editor
    data class Asset(val value: Investment? = null) : Editor
    data class Cat(val value: com.logikawan.finflow.core.model.Category? = null, val parentId: String? = null) : Editor
    data object Person : Editor
}
