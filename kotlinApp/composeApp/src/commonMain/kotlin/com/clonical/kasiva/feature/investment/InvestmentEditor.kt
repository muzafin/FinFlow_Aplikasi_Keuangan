package com.clonical.kasiva.feature.investment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.clonical.kasiva.core.model.Investment
import com.clonical.kasiva.core.ui.FFColors
import com.clonical.kasiva.core.ui.FFIcon
import com.clonical.kasiva.core.ui.FFType
import com.clonical.kasiva.core.util.decimalAmount
import com.clonical.kasiva.core.util.minorAmount
import com.clonical.kasiva.core.util.newId
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.i18n.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InvestmentEditor(old: Investment?, busy: Boolean, dismiss: () -> Unit, save: (Investment) -> Unit, delete: (() -> Unit)?) {
    val id = remember { old?.id ?: newId() }
    var name by remember { mutableStateOf(old?.name.orEmpty()) }
    var type by remember { mutableStateOf(old?.type ?: "saham") }
    var currency by remember { mutableStateOf(old?.currency ?: "IDR") }
    var initial by remember { mutableStateOf(old?.let { decimalAmount(it.investedMinor, it.currency) }.orEmpty()) }
    var current by remember { mutableStateOf(old?.let { decimalAmount(it.currentMinor, it.currency) }.orEmpty()) }
    var dividends by remember { mutableStateOf(old?.let { decimalAmount(it.dividendsMinor, it.currency) } ?: "0") }
    var platform by remember { mutableStateOf(old?.platform.orEmpty()) }
    var notes by remember { mutableStateOf(old?.notes.orEmpty()) }
    var details by remember { mutableStateOf(old != null) }
    var error by remember { mutableStateOf<String?>(null) }
    fun submit() {
        if (busy) return
        try {
            val code = currency.trim().uppercase()
            require(name.isNotBlank() && code.matches(Regex("[A-Z]{3}"))) { MR.strings.acc_err_investment_name_currency.text() }
            val invested = minorAmount(initial, code)
            val present = minorAmount(current.ifBlank { initial }, code)
            val paid = minorAmount(dividends, code)
            require(name.trim().length <= 100 && platform.trim().length <= 100 && notes.trim().length <= 4000 &&
                invested in 1..9_000_000_000_000_000L && present in 0..9_000_000_000_000_000L && paid in 0..9_000_000_000_000_000L) {
                MR.strings.acc_err_investment_fields.text()
            }
            save(Investment(id, name.trim(), type, invested, present, paid, code, platform.trim(), notes.trim()))
        } catch (e: Exception) { error = e.message ?: MR.strings.acc_err_investment_save.text() }
    }
    Dialog(onDismissRequest = { if (!busy) dismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = FFColors.background) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth().height(56.dp).background(FFColors.surface), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(enabled = !busy, onClick = dismiss) { FFIcon("close_rounded", contentDescription = MR.strings.common_close.text()) }
                    Text(if (old == null) MR.strings.acc_add_investment.text() else MR.strings.acc_edit_investment.text(), Modifier.weight(1f),
                        style = FFType.sectionTitle, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    if (delete == null) Spacer(Modifier.width(48.dp)) else IconButton(enabled = !busy, onClick = delete) {
                        FFIcon("delete_outline_rounded", FFColors.expense, contentDescription = MR.strings.acc_delete_investment.text())
                    }
                }
                Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp)) {
                    Text(MR.strings.acc_investment_type.text(), style = FFType.bodyMain.copy(fontWeight = FontWeight.SemiBold))
                    Spacer(Modifier.height(12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("saham", "reksaDana", "crypto", "emas", "deposito", "properti", "p2pLending").forEach { value ->
                            FilterChip(selected = type == value, onClick = { type = value }, enabled = !busy,
                                label = { Text(investmentTypeLabel(value), style = FFType.bodyMain.copy(fontWeight = if (type == value) FontWeight.SemiBold else FontWeight.Normal)) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = FFColors.primary.copy(alpha = .2f),
                                    selectedLabelColor = FFColors.primary, labelColor = FFColors.onSurfaceVariant))
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(), enabled = !busy,
                        label = { Text(MR.strings.acc_investment_name_label.text()) }, leadingIcon = { FFIcon("business_center_rounded") },
                        singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words))
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(platform, { platform = it }, modifier = Modifier.fillMaxWidth(), enabled = !busy,
                        label = { Text(MR.strings.acc_platform_label.text()) }, leadingIcon = { FFIcon("account_balance_rounded") },
                        singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words))
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(initial, { initial = it }, modifier = Modifier.fillMaxWidth(), enabled = !busy,
                        label = { Text(MR.strings.acc_initial_amount_label.text(currency.trim().uppercase())) }, leadingIcon = { FFIcon("attach_money_rounded") },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    TextButton(enabled = !busy, onClick = { details = !details }) {
                        Text(if (details) MR.strings.acc_hide_value_details.text() else MR.strings.acc_value_details.text())
                        FFIcon(if (details) "expand_less_rounded" else "expand_more_rounded", FFColors.primary, 18.dp)
                    }
                    if (details) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(currency, { currency = it }, label = { Text(MR.strings.acc_valuation_currency.text()) }, modifier = Modifier.fillMaxWidth(),
                            enabled = !busy, singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters))
                        OutlinedTextField(current, { current = it }, label = { Text(MR.strings.acc_present_value.text()) }, placeholder = { Text(MR.strings.acc_present_value_placeholder.text()) },
                            modifier = Modifier.fillMaxWidth(), enabled = !busy, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        OutlinedTextField(dividends, { dividends = it }, label = { Text(MR.strings.acc_dividends.text()) }, modifier = Modifier.fillMaxWidth(),
                            enabled = !busy, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        OutlinedTextField(notes, { notes = it }, label = { Text(MR.strings.acc_notes.text()) }, modifier = Modifier.fillMaxWidth(), enabled = !busy)
                    }
                    Text(MR.strings.acc_investment_editor_note.text(),
                        style = FFType.metaData, color = FFColors.onSurfaceVariant)
                    error?.let { Text(it, Modifier.padding(top = 12.dp), style = FFType.bodyMain, color = MaterialTheme.colorScheme.error) }
                    Spacer(Modifier.height(32.dp))
                    Button(onClick = ::submit, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        if (busy) CircularProgressIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        else Text(MR.strings.acc_save_portfolio.text(), style = FFType.bodyMain.copy(fontWeight = FontWeight.SemiBold))
                    }
                }
            }
        }
    }
}
