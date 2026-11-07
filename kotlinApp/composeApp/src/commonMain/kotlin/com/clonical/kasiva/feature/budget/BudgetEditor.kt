package com.clonical.kasiva.feature.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.clonical.kasiva.core.data.FinanceRepository
import com.clonical.kasiva.core.model.*
import com.clonical.kasiva.core.ui.*
import com.clonical.kasiva.core.util.*
import kotlinx.datetime.*
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.i18n.*

/*
 * Form anggaran bergaya Ollo: layar penuh dengan tombol tutup (X), judul tengah, dan tombol
 * simpan (centang) di bar atas. Susunan: batas nominal (pill mata uang + angka besar), nama
 * anggaran, periode mingguan/bulanan, kategori berbentuk lingkaran, dan hapus untuk data lama.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetEditor(old: Budget?, data: FinanceData, busy: Boolean, dismiss: () -> Unit, save: (Budget) -> Unit, delete: (() -> Unit)?) {
    val id = remember { old?.id ?: newId() }
    val preferences = LocalPreferences.current
    val categories = data.categories.filter { it.type == "expense" && (it.parentId == null || it.id == old?.categoryId) }
    var title by remember { mutableStateOf(old?.title.orEmpty()) }
    var period by remember { mutableStateOf(old?.period ?: "monthly") }
    var category by remember { mutableStateOf(old?.categoryId ?: categories.firstOrNull()?.id.orEmpty()) }
    var currency by remember { mutableStateOf(old?.currency ?: data.profile?.baseCurrency ?: "IDR") }
    var amount by remember { mutableStateOf(old?.let { decimalAmount(it.limitMinor, it.currency) }.orEmpty()) }
    var month by remember { mutableStateOf(old?.takeIf { it.period == "monthly" }?.month ?: activeRecordingMonth(today(), preferences.recordingDay)) }
    var week by remember { mutableStateOf(old?.takeIf { it.period == "weekly" }?.month ?: financePeriod("week", today(), weekStart = preferences.weekStart).start.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    var currencyMenu by remember { mutableStateOf(false) }
    var datePicker by remember { mutableStateOf(false) }
    val currencyOptions = (listOfNotNull(data.profile?.baseCurrency) + data.accounts.map { it.currency } + currency).distinct().sorted()
    val activeRange = runCatching {
        if (period == "weekly") financePeriod("week", LocalDate.parse(week), weekStart = preferences.weekStart)
        else recordingPeriod(month, preferences.recordingDay)
    }.getOrNull()

    fun submit() {
        if (busy) return
        try {
            val code = currency.trim().uppercase()
            require(categories.any { it.id == category }) { MR.strings.tx_err_pick_expense_category.text() }
            require(code.matches(Regex("[A-Z]{3}"))) { MR.strings.tx_err_currency_code.text() }
            require(title.trim().length <= 100) { MR.strings.tx_err_budget_name_max.text() }
            if (period == "monthly") {
                require(Regex("\\d{4}-(0[1-9]|1[0-2])").matches(month)) { MR.strings.tx_err_month_format.text() }
                LocalDate.parse("$month-01")
            } else LocalDate.parse(week)
            val value = minorAmount(amount, code)
            require(value in 1..FinanceRepository.MAX_MONEY) { MR.strings.tx_err_amount_range.text() }
            val budget = Budget(
                id = id, categoryId = category, limitMinor = value, currency = code,
                month = if (period == "weekly") week else month,
                title = title.trim(), period = period,
            )
            budgetPeriod(budget, preferences.recordingDay)
            error = null
            save(budget)
        } catch (e: Exception) { error = e.message ?: MR.strings.tx_err_budget_invalid.text() }
    }

    Dialog(onDismissRequest = { if (!busy) dismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(Modifier.fillMaxSize().background(designBackground).safeDrawingPadding().imePadding().testTag("budget-editor")) {
            Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).clickable { if (!busy) dismiss() }.testTag("budget-close"), contentAlignment = Alignment.Center) {
                    FFIcon("close_rounded", FFColors.onSurface, 24.dp, MR.strings.common_close.text())
                }
                Text(if (old == null) MR.strings.tx_new_budget.text() else MR.strings.tx_edit_budget.text(), Modifier.weight(1f), style = FFType.appTitle, textAlign = TextAlign.Center, color = FFColors.onSurface)
                Box(Modifier.size(48.dp).clip(CircleShape).clickable { submit() }.testTag("budget-save"), contentAlignment = Alignment.Center) {
                    if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else FFIcon("check_rounded", if (amount.isNotBlank()) FFColors.onSurface else FFColors.outline, 26.dp, MR.strings.common_save.text())
                }
            }
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            ) {
                Spacer(Modifier.height(16.dp))
                BudgetFieldLabel(MR.strings.tx_limit_amount.text())
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        Row(
                            Modifier.height(54.dp).clip(RoundedCornerShape(16.dp)).background(FFColors.surface)
                                .clickable { currencyMenu = true }.testTag("budget-currency").padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(if (currency == "IDR") "Rp" else currency, style = FFType.amountLg.copy(fontSize = 30.sp, lineHeight = 34.sp), color = FFColors.onSurface)
                            Spacer(Modifier.width(6.dp))
                            FFIcon("expand_more_rounded", FFColors.outline, 20.dp)
                        }
                        DropdownMenu(currencyMenu, { currencyMenu = false }) {
                            currencyOptions.forEach { code ->
                                DropdownMenuItem(
                                    text = { Text(if (code == "IDR") "Rp · IDR" else code, style = FFType.bodyMain, fontWeight = if (code == currency) FontWeight.SemiBold else FontWeight.Normal) },
                                    onClick = { currency = code; currencyMenu = false },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    BasicTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        modifier = Modifier.weight(1f).testTag("budget-amount"),
                        singleLine = true,
                        enabled = !busy,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = FFType.amountXl.copy(color = FFColors.onSurface),
                        cursorBrush = SolidColor(FFColors.primary),
                        decorationBox = { inner ->
                            Box {
                                if (amount.isEmpty()) Text("0", style = FFType.amountXl, color = FFColors.outline)
                                inner()
                            }
                        },
                    )
                }
                Spacer(Modifier.height(26.dp))
                BudgetFieldLabel(MR.strings.tx_budget_name.text())
                Spacer(Modifier.height(10.dp))
                BudgetTextField(title, { title = it.take(100) }, MR.strings.tx_budget_name_placeholder.text(), enabled = !busy)
                Spacer(Modifier.height(26.dp))
                BudgetFieldLabel(MR.strings.tx_period.text())
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(16.dp)).background(FFColors.surface).padding(4.dp)) {
                    listOf("weekly" to MR.strings.tx_freq_weekly.text(), "monthly" to MR.strings.tx_freq_monthly.text()).forEach { (value, label) ->
                        val selected = period == value
                        Box(
                            Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp))
                                .background(if (selected) FFColors.canvas else Color.Transparent)
                                .clickable { if (!busy) period = value },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(label, style = FFType.bodyMain, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) FFColors.onSurface else FFColors.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    if (period == "monthly") MR.strings.tx_budget_monthly_hint.text()
                    else MR.strings.tx_budget_weekly_hint.text(),
                    style = FFType.metaData, color = FFColors.outline,
                )
                Spacer(Modifier.height(26.dp))
                BudgetFieldLabel(MR.strings.tx_category.text())
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                    items(categories, key = { it.id }) { cat ->
                        BudgetCategoryChip(cat.id, cat.name, category == cat.id, enabled = !busy) { category = cat.id }
                    }
                }
                if (period == "weekly") {
                    Spacer(Modifier.height(22.dp))
                    BudgetFieldLabel(MR.strings.tx_budget_week_date.text())
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(FFColors.surface)
                            .clickable(enabled = !busy) { datePicker = true }.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FFIcon("calendar_today_outlined", FFColors.onSurfaceVariant, 20.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(shortDateLabel(week), Modifier.weight(1f), style = FFType.bodyMain, color = FFColors.onSurface)
                        activeRange?.let { range ->
                            Text("${range.start.dayOfMonth}–${range.endExclusive.minus(DatePeriod(days = 1)).dayOfMonth}", style = FFType.metaData, color = FFColors.outline)
                        }
                    }
                } else {
                    Spacer(Modifier.height(22.dp))
                    BudgetFieldLabel(MR.strings.tx_active_period.text())
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(FFColors.surface).padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FFIcon("calendar_today_outlined", FFColors.onSurfaceVariant, 20.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(monthLabel(month), Modifier.weight(1f), style = FFType.bodyMain, color = FFColors.onSurface)
                        activeRange?.let { range ->
                            Text("${range.start.dayOfMonth}–${range.endExclusive.minus(DatePeriod(days = 1)).dayOfMonth}", style = FFType.metaData, color = FFColors.outline)
                        }
                    }
                }
                error?.let {
                    Spacer(Modifier.height(14.dp))
                    Text(it, style = FFType.bodyMain, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("budget-error"))
                }
                if (delete != null) {
                    Spacer(Modifier.height(18.dp))
                    TextButton(enabled = !busy, onClick = delete, modifier = Modifier.fillMaxWidth()) {
                        Text(MR.strings.tx_delete_budget.text(), style = FFType.bodyMain, color = FFColors.expense)
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
    if (datePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = runCatching { LocalDate.parse(week).toEpochDays().toLong() * 86400000L }.getOrNull())
        DatePickerDialog(
            onDismissRequest = { datePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        week = financePeriod("week", LocalDate.fromEpochDays((millis / 86400000L).toInt()), weekStart = preferences.weekStart).start.toString()
                    }
                    datePicker = false
                }) { Text(MR.strings.tx_pick.text()) }
            },
            dismissButton = { TextButton(onClick = { datePicker = false }) { Text(MR.strings.common_cancel.text()) } },
        ) { DatePicker(state) }
    }
}

@Composable
private fun BudgetFieldLabel(text: String) {
    Text(text, style = FFType.bodyMain.copy(fontSize = 16.sp, lineHeight = 22.sp), fontWeight = FontWeight.Medium, color = FFColors.onSurfaceVariant)
}

@Composable
private fun BudgetTextField(value: String, change: (String) -> Unit, placeholder: String, enabled: Boolean = true) {
    BasicTextField(
        value = value,
        onValueChange = change,
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("budget-field"),
        singleLine = true,
        enabled = enabled,
        textStyle = FFType.bodyMain.copy(color = FFColors.onSurface),
        cursorBrush = SolidColor(FFColors.primary),
        decorationBox = { inner ->
            Row(
                Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(FFColors.surface).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, style = FFType.bodyMain, color = FFColors.outline, maxLines = 1)
                    inner()
                }
            }
        },
    )
}

@Composable
private fun BudgetCategoryChip(id: String, name: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val color = categoryColor(id)
    Column(
        Modifier.width(72.dp).clip(RoundedCornerShape(16.dp)).clickable(enabled = enabled, onClick = onClick).testTag("budget-category-$id"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(color.copy(alpha = .14f))
                .then(if (selected) Modifier.border(2.dp, color, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) { FFIcon(categoryIcon(id), color, 22.dp) }
        Spacer(Modifier.height(6.dp))
        Text(
            name, style = FFType.metaData, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) FFColors.onSurface else FFColors.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
        )
    }
}