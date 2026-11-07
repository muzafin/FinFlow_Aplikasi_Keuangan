package com.clonical.kasiva.app.navigation

import com.clonical.kasiva.core.model.Account
import com.clonical.kasiva.core.model.Budget
import com.clonical.kasiva.core.model.Goal
import com.clonical.kasiva.core.model.Investment
import com.clonical.kasiva.core.model.Transaction

sealed interface Editor {
    data class BillForm(val value: com.clonical.kasiva.core.model.Bill? = null) : Editor
    data class DebtForm(val value: com.clonical.kasiva.core.model.Debt? = null) : Editor
    data class WishForm(val value: com.clonical.kasiva.core.model.Wish? = null) : Editor
    data class CardForm(val value: com.clonical.kasiva.core.model.WalletCard? = null) : Editor
    data class NoteForm(val value: com.clonical.kasiva.core.model.FinanceNote? = null) : Editor
    data class ClaimForm(val value: com.clonical.kasiva.core.model.Reimbursement? = null) : Editor
    data class RepeatForm(val value: com.clonical.kasiva.core.model.RecurringTransaction? = null) : Editor
    data class PayBill(val value: com.clonical.kasiva.core.model.Bill) : Editor
    data class PayDebt(val value: com.clonical.kasiva.core.model.Debt) : Editor
    data class ReceiveClaim(val value: com.clonical.kasiva.core.model.Reimbursement) : Editor
    data class PayBundle(val value: com.clonical.kasiva.core.model.FinanceNote, val itemIds: List<String>) : Editor
    data class Tx(val value: Transaction? = null, val notificationImport: Boolean = false, val onImported: (() -> Unit)? = null, val scheduled: Boolean = false, val accountId: String? = null, val type: String = "expense", val aiDraft: Boolean = false) : Editor
    data class AdjustWallet(val value: Account) : Editor
    data class GoalMovement(val value: Goal, val withdraw: Boolean) : Editor
    data class Wallet(val value: Account? = null) : Editor
    data class Limit(val value: Budget? = null) : Editor
    data class Target(val value: Goal? = null) : Editor
    data class Asset(val value: Investment? = null) : Editor
    data class Cat(val value: com.clonical.kasiva.core.model.Category? = null, val parentId: String? = null) : Editor
    data object Person : Editor
}
