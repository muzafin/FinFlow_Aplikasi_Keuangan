package com.logikawan.finflow.feature.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.logikawan.finflow.core.model.*
import com.logikawan.finflow.core.ui.*
import com.logikawan.finflow.core.util.*
import com.logikawan.finflow.core.i18n.text
import com.logikawan.finflow.i18n.*

@Composable
fun AccountEditor(old: Account?, data: FinanceData, busy: Boolean, dismiss: () -> Unit, save: (Account) -> Unit) {
    val id = remember { old?.id ?: newId() }
    var name by remember { mutableStateOf(old?.name.orEmpty()) }
    var type by remember { mutableStateOf(old?.type ?: "cash") }
    var currency by remember { mutableStateOf(old?.currency ?: data.profile?.baseCurrency ?: "IDR") }
    var opening by remember { mutableStateOf(old?.let { decimalAmount(it.openingBalanceMinor, it.currency) } ?: "0") }
    var archived by remember { mutableStateOf(old?.archived ?: false) }
    var excluded by remember { mutableStateOf(old?.excludedFromTotal ?: false) }
    var pocket by remember { mutableStateOf(old?.parentId!=null) }
    var parent by remember { mutableStateOf(old?.parentId.orEmpty()) }
    var icon by remember { mutableStateOf(old?.icon ?: "account_balance_wallet") }
    var color by remember { mutableStateOf(old?.color ?: 0xFF2196F3) }
    var error by remember { mutableStateOf<String?>(null) }
    val keys = listOf("cash", "bank", "ewallet", "credit", "investment", "loan")
    val labels = keys.map { if (it == "bank") MR.strings.acc_type_bank.text() else walletTypeLabel(it) }
    EditorDialog(if (old == null) MR.strings.acc_add_wallet.text() else MR.strings.acc_edit_wallet.text(), busy, dismiss, save = {
        if (!busy) try {
            val code = currency.trim().uppercase()
            require(name.isNotBlank() && name.trim().length <= 100) { MR.strings.acc_err_name_required.text() }
            require(code.matches(Regex("[A-Z]{3}"))) { MR.strings.acc_err_currency_code.text() }
            require(old == null || code == old.currency) { MR.strings.acc_err_currency_locked.text() }
            require(type in keys) { MR.strings.acc_err_invalid_type.text() }
            require(!pocket || parent.isNotBlank()){ MR.strings.acc_err_choose_parent.text() }
            require(!pocket || data.accounts.none{it.parentId==id}){MR.strings.acc_err_parent_has_pockets.text()}
            save(Account(id, name.trim(), type, code, minorAmount(opening, code, allowNegative = true), archived, old?.createdAt ?: nowMillis(),excluded,if(pocket)parent else null,icon,color))
        } catch (e: Exception) { error = e.message ?: MR.strings.acc_err_invalid_data.text() }
    }, error = error) {
        Text(MR.strings.acc_wallet_details.text(), style = FFType.sectionTitle)
        Choice(MR.strings.acc_type.text(), type, keys.zip(labels), { if (!busy) type = it })
        Field(MR.strings.acc_wallet_name.text(), name, { if (!busy) name = it })
        if (old == null) Field(MR.strings.acc_wallet_currency.text(), currency, { if (!busy) currency = it.uppercase().take(3) })
        else Text(MR.strings.acc_wallet_currency_value.text(currency), style = FFType.bodyMain)
        Field(MR.strings.acc_opening_balance.text(), opening, { if (!busy) opening = it })
        DesignCard { Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(MR.strings.acc_exclude_from_total.text(),style=FFType.bodyMain);Text(MR.strings.acc_exclude_from_total_desc.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant)};Switch(excluded,{excluded=it},enabled=!busy)} }
        DesignCard { Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(MR.strings.acc_make_pocket.text(),style=FFType.bodyMain);Text(MR.strings.acc_make_pocket_desc.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant)};Switch(pocket,{pocket=it},enabled=!busy)} }
        if(pocket)WalletChoice(MR.strings.acc_parent_wallet.text(),parent,data,data.accounts.filter{it.id!=id && !it.archived && it.parentId==null && it.currency==currency},!busy){parent=it}
        Choice(MR.strings.acc_wallet_icon.text(),icon,listOf("account_balance_wallet" to MR.strings.acc_wallet.text(),"account_balance" to MR.strings.acc_type_bank.text(),"payments" to MR.strings.acc_type_cash.text(),"credit_card" to MR.strings.acc_icon_card.text(),"savings" to MR.strings.acc_type_savings.text(),"work_outline" to MR.strings.acc_icon_business.text()),{icon=it})
        Choice(MR.strings.acc_color.text(),color.toString(),listOf(0xFF2196F3L to MR.strings.acc_color_blue.text(),0xFF4CAF50L to MR.strings.acc_color_green.text(),0xFFF44336L to MR.strings.acc_color_red.text(),0xFFFF9800L to MR.strings.acc_color_orange.text(),0xFF9C27B0L to MR.strings.acc_color_purple.text()).map{it.first.toString() to it.second},{color=it.toLong()})
        if (old != null) DesignCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(MR.strings.acc_archive_wallet.text(), style = FFType.bodyMain)
                    Text(MR.strings.acc_history_kept.text(), style = FFType.metaData, color = FFColors.outline)
                }
                Switch(archived, { if (!busy) archived = it }, enabled = !busy)
            }
        }
        Text(MR.strings.acc_icon.text(), style = FFType.sectionTitle)
        Surface(shape = RoundedCornerShape(16.dp), color = FFColors.surface) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                FigmaAsset(when(type) { "bank" -> "fig_92_6_building"; "ewallet", "loan" -> "fig_92_6_credit_card"; "investment" -> "fig_86_157_trending_up"; else -> "fig_92_6_wallet" })
                Spacer(Modifier.width(12.dp)); Text(labels.getOrElse(keys.indexOf(type)) { MR.strings.acc_wallet.text() }, style = FFType.bodyMain)
            }
        }
        Text(MR.strings.acc_opening_balance_hint.text(), style = FFType.metaData, color = FFColors.onSurfaceVariant)
        if (type == "bank" || type == "ewallet") Text(MR.strings.acc_manual_sync_note.text(), style = FFType.metaData, color = FFColors.onSurfaceVariant)
    }
}

