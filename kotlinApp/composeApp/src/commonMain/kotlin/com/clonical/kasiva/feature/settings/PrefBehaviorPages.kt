package com.clonical.kasiva.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonical.kasiva.core.i18n.periodLabel
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.model.*
import com.clonical.kasiva.core.ui.*
import com.clonical.kasiva.core.util.formatBalance
import com.clonical.kasiva.i18n.*

// ───────────── Tampilan Beranda (Ollo "Home View") ─────────────

internal val homePeriods = listOf("Hari Ini", "Minggu Ini", "Bulan Ini", "Tahun", "Kalender")

/** Label pendek periode Beranda (tanpa akhiran " Ini"); nilai tersimpan tetap kunci Indonesia. */
internal fun homePeriodShortLabel(id: String): String = when (id) {
    "Hari Ini" -> MR.strings.set_period_day.text()
    "Minggu Ini" -> MR.strings.set_period_week.text()
    "Bulan Ini" -> MR.strings.set_period_month.text()
    else -> periodLabel(id)
}

internal fun LazyListScope.homeViewPage(p: AppPreferences, update: (AppPreferences) -> Unit, onNavigate: (String) -> Unit) {
    item {
        var picking by remember { mutableStateOf(false) }
        PrefCard {
            PrefRow(MR.strings.pc_home_default_tab.text(), MR.strings.pc_home_default_tab_desc.text(homePeriodShortLabel(p.homePeriod)), "hi_calendar",
                onClick = { picking = true }, modifier = Modifier.testTag("home-default-tab"))
        }
        if (picking) PrefChoiceSheet(MR.strings.pc_home_default_tab.text(), homePeriods.map { it to homePeriodShortLabel(it) }, p.homePeriod, "home-period",
            { picking = false }) { update(p.copy(homePeriod = it)) }
    }
    item {
        PrefCard {
            PrefRow(MR.strings.pc_home_menu_order.text(), MR.strings.pc_home_menu_order_desc.text(), "hi_sort",
                onClick = { onNavigate("Susunan Menu") }, modifier = Modifier.testTag("home-menu-order"))
        }
    }
    item {
        PrefCard {
            PrefRow(MR.strings.pc_home_menu_appearance.text(), MR.strings.pc_home_menu_appearance_desc.text(), "hi_paint",
                onClick = { onNavigate("Tampilan Menu") }, modifier = Modifier.testTag("home-menu-appearance"))
        }
    }
}

// ───────────── Tampilan Tambah Transaksi (Ollo "Add Transaction Display") ─────────────

internal fun LazyListScope.addTransactionPage(p: AppPreferences, update: (AppPreferences) -> Unit) {
    item {
        var picking by remember { mutableStateOf(false) }
        val options = listOf("calendar" to MR.strings.pc_add_picker_calendar.text(), "wheel" to MR.strings.pc_add_picker_wheel.text())
        PrefCard {
            PrefRow(MR.strings.pc_add_date_picker.text(), options.first { it.first == p.datePickerStyle }.second, "hi_calendar", iconCircle = true,
                onClick = { picking = true }, modifier = Modifier.testTag("add-date-picker"))
        }
        if (picking) PrefChoiceSheet(MR.strings.pc_add_date_picker.text(), options, p.datePickerStyle, "add-picker", { picking = false }) { update(p.copy(datePickerStyle = it)) }
    }
    item { PrefSectionLabel(MR.strings.pc_add_preferences.text()) }
    item {
        PrefCard {
            PrefSwitchRow(MR.strings.pc_add_quick_fill.text(), MR.strings.pc_add_quick_fill_desc.text(), p.quickFill, { update(p.copy(quickFill = it)) },
                "add-quick-fill", icon = "hi_pencil")
        }
    }
    item {
        var picking by remember { mutableStateOf<String?>(null) }
        val density = listOf("comfortable" to MR.strings.set_pref_comfortable.text(), "compact" to MR.strings.set_pref_compact.text())
        val button = listOf("quick" to MR.strings.set_pref_add_quick.text(), "all" to MR.strings.set_pref_add_all.text())
        PrefCard {
            PrefRow(MR.strings.set_pref_tx_density.text(), density.first { it.first == p.transactionDisplay }.second, "hi_layout", iconCircle = true,
                onClick = { picking = "density" }, modifier = Modifier.testTag("add-density"))
            PrefDivider()
            PrefRow(MR.strings.set_pref_add_button.text(), button.first { it.first == p.addButtonMode }.second, "hi_add_circle", iconCircle = true,
                onClick = { picking = "button" }, modifier = Modifier.testTag("add-button-mode"))
        }
        when (picking) {
            "density" -> PrefChoiceSheet(MR.strings.set_pref_tx_density.text(), density, p.transactionDisplay, "add-density", { picking = null }) { update(p.copy(transactionDisplay = it)) }
            "button" -> PrefChoiceSheet(MR.strings.set_pref_add_button.text(), button, p.addButtonMode, "add-button", { picking = null }) { update(p.copy(addButtonMode = it)) }
        }
    }
}

// ───────────── Format Saldo (Ollo "Balance Formatting") ─────────────

internal fun LazyListScope.balanceFormatPage(p: AppPreferences, update: (AppPreferences) -> Unit) {
    item {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(FFColors.surfaceContainerHigh).padding(vertical = 22.dp, horizontal = 16.dp)
            .testTag("balance-preview"), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(MR.strings.pc_balance_preview.text(), style = FFType.bodyMain.copy(fontSize = 14.sp), color = FFColors.onSurface)
            Text(if (p.hideBalances) maskedBalance(123456789, "IDR", p) else formatBalance(123456789, "IDR", p), textAlign = TextAlign.Center,
                style = FFType.amountLg.copy(fontSize = 24.sp, lineHeight = 30.sp), fontWeight = FontWeight.SemiBold, color = FFColors.onSurface, maxLines = 1)
        }
    }
    item { PrefTitle(MR.strings.pc_balance_hide_section.text(), Modifier.padding(top = 8.dp)) }
    item {
        var picking by remember { mutableStateOf(false) }
        PrefCard {
            PrefSwitchRow(MR.strings.set_pref_hide_balance.text(), null, p.hideBalances, { update(p.copy(hideBalances = it)) }, "pref-hide-balance")
            PrefDivider()
            PrefRow(MR.strings.pc_balance_mask_char.text(), p.maskCharacter, onClick = { picking = true }, modifier = Modifier.testTag("pref-mask-character"))
            PrefDivider()
            val sample = p.copy(showDecimals = false, preserveMaskFormat = true)
            PrefSwitchRow(MR.strings.pc_balance_preserve.text(), formatBalance(123456700, "IDR", sample) + " -> " + maskedBalance(123456700, "IDR", sample),
                p.preserveMaskFormat, { update(p.copy(preserveMaskFormat = it)) }, "pref-preserve-mask")
        }
        if (picking) PrefChoiceSheet(MR.strings.pc_balance_mask_char.text(), maskCharacters.map { it to it }, p.maskCharacter, "mask-char", { picking = false }) {
            update(p.copy(maskCharacter = it))
        }
    }
    item { PrefTitle(MR.strings.pc_balance_number_section.text(), Modifier.padding(top = 8.dp)) }
    item {
        PrefCard {
            listOf("id" to MR.strings.pc_balance_format_id.text(), "en" to MR.strings.pc_balance_format_en.text()).forEachIndexed { index, (id, label) ->
                if (index > 0) PrefDivider()
                val selected = p.numberFormat == id
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.RadioButton) { update(p.copy(numberFormat = id)) }
                    .testTag("number-format-$id").padding(horizontal = 24.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    PrefRadio(selected)
                    Spacer(Modifier.width(28.dp))
                    Text(label, style = FFType.bodyMain.copy(fontSize = 16.sp), color = if (selected) FFColors.onSurface else FFColors.onSurfaceVariant)
                }
            }
        }
    }
    item {
        PrefCard {
            PrefSwitchRow(MR.strings.set_pref_show_decimals.text(), formatBalance(123456, "IDR", p.copy(showDecimals = true)), p.showDecimals,
                { update(p.copy(showDecimals = it)) }, "pref-show-decimals")
            PrefDivider()
            PrefSwitchRow(MR.strings.set_pref_currency_symbol.text(), formatBalance(123400, "IDR", p.copy(showCurrencySymbol = true)) + " / " +
                formatBalance(123400, "IDR", p.copy(showCurrencySymbol = false)), p.showCurrencySymbol, { update(p.copy(showCurrencySymbol = it)) }, "pref-currency-symbol")
        }
    }
}

// ───────────── Periode Pencatatan (Ollo "Recording Period") ─────────────

internal fun weekdayLabel(day: Int): String = when (day) {
    1 -> MR.strings.set_day_monday.text(); 2 -> MR.strings.set_day_tuesday.text(); 3 -> MR.strings.set_day_wednesday.text()
    4 -> MR.strings.set_day_thursday.text(); 5 -> MR.strings.set_day_friday.text(); 6 -> MR.strings.set_day_saturday.text()
    else -> MR.strings.set_day_sunday.text()
}

/** Draf diubah di layar dan disimpan lewat ✓ di header (seperti Ollo). */
internal fun LazyListScope.recordingPeriodPage(day: Int, week: Int, changeDay: (Int) -> Unit, changeWeek: (Int) -> Unit) {
    item {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(FFColors.surface).padding(horizontal = 20.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically) {
            FFIcon("hi_calendar", FFColors.onSurface, 28.dp)
            Spacer(Modifier.width(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(MR.strings.set_page_recording_period.text(), style = FFType.sectionTitle.copy(fontSize = 19.sp), fontWeight = FontWeight.SemiBold, color = FFColors.onSurface)
                Text(MR.strings.pc_period_desc.text(), style = FFType.bodyMain.copy(fontSize = 14.sp, lineHeight = 19.sp), color = FFColors.onSurfaceVariant)
            }
        }
    }
    item {
        var info by remember { mutableStateOf(false) }
        var picking by remember { mutableStateOf(false) }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PrefSectionLabel(MR.strings.pc_period_monthly.text(), caps = true)
                Box(Modifier.padding(top = 8.dp, start = 8.dp).size(28.dp).clip(CircleShape).clickable(role = Role.Button) { info = true }.testTag("period-info"),
                    contentAlignment = Alignment.Center) { FFIcon("hi_info", FFColors.onSurfaceVariant, 18.dp, MR.strings.pc_period_info.text()) }
            }
            PrefValueRow(MR.strings.pc_period_start_day.text(), day.toString(), "period-start-day") { picking = true }
        }
        if (info) AlertDialog(onDismissRequest = { info = false }, confirmButton = { TextButton({ info = false }) { Text(MR.strings.pc_ok.text()) } },
            text = { Text(MR.strings.set_pref_short_month_hint.text(), style = FFType.bodyMain) }, containerColor = FFColors.canvas)
        if (picking) DayOfMonthSheet(day, { picking = false }) { changeDay(it) }
    }
    item {
        var picking by remember { mutableStateOf(false) }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PrefSectionLabel(MR.strings.pc_period_weekly.text(), caps = true)
            PrefValueRow(MR.strings.pc_period_week_start.text(), weekdayLabel(week), "period-week-start") { picking = true }
        }
        if (picking) PrefChoiceSheet(MR.strings.pc_period_week_start.text(), (1..7).map { it.toString() to weekdayLabel(it) }, week.toString(), "week-start",
            { picking = false }) { changeWeek(it.toInt()) }
    }
}

@Composable
private fun PrefValueRow(title: String, value: String, tag: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(24.dp)).background(FFColors.surface).clickable(role = Role.Button, onClick = onClick)
        .testTag(tag).padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = FFType.bodyMain.copy(fontSize = 16.sp), color = FFColors.onSurfaceVariant)
        Text(value, style = FFType.bodyMain.copy(fontSize = 16.sp), fontWeight = FontWeight.Medium, color = FFColors.onSurface)
        Spacer(Modifier.width(12.dp))
        FFIcon("hi_chevron_right", FFColors.onSurfaceVariant, 18.dp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayOfMonthSheet(selected: Int, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = FFColors.canvas) {
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(MR.strings.pc_period_start_day.text(), Modifier.padding(4.dp), style = FFType.sectionTitle, fontWeight = FontWeight.SemiBold)
            Text(MR.strings.set_pref_short_month_hint.text(), Modifier.padding(horizontal = 4.dp), style = FFType.metaData, color = FFColors.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            (1..31).chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    week.forEach { day ->
                        val active = day == selected
                        Box(Modifier.weight(1f).aspectRatio(1f).clip(CircleShape).background(if (active) FFColors.pill else FFColors.surface)
                            .clickable(role = Role.RadioButton) { onSelect(day); onDismiss() }.testTag("start-day-$day"), contentAlignment = Alignment.Center) {
                            Text(day.toString(), style = FFType.bodyMain.copy(fontSize = 15.sp), color = if (active) FFColors.onPill else FFColors.onSurface)
                        }
                    }
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

// ───────────── Memori Pencatatan (Ollo "Quick Record Memory") ─────────────

internal fun LazyListScope.recordingMemoryPage(p: AppPreferences, update: (AppPreferences) -> Unit, data: FinanceData) {
    item { Text(MR.strings.pc_memory_desc.text(), style = FFType.bodyMain.copy(fontSize = 15.sp, lineHeight = 21.sp), color = FFColors.onSurfaceVariant) }
    item {
        var confirm by remember { mutableStateOf(false) }
        var done by remember { mutableStateOf(false) }
        val hasMemory = p.lastExpenseCategory.isNotEmpty() || p.lastIncomeCategory.isNotEmpty()
        PrefCard {
            PrefSwitchRow(MR.strings.set_pref_remember_category.text(), MR.strings.pc_memory_remember_desc.text(), p.rememberCategory,
                { update(p.copy(rememberCategory = it)) }, "memory-remember")
            PrefDivider()
            val orange = Color(0xFFFF9800)
            Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).clickable(enabled = hasMemory, role = Role.Button) { confirm = true }.testTag("memory-reset")
                .padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                PrefIconBox("hi_reimburse", circle = true, tint = orange, background = orange.copy(alpha = .14f))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(MR.strings.pc_memory_reset.text(), style = FFType.bodyMain.copy(fontSize = 16.sp, lineHeight = 22.sp),
                        color = if (hasMemory) FFColors.onSurface else FFColors.onSurfaceVariant)
                    Text(when { done && !hasMemory -> MR.strings.pc_memory_reset_done.text(); hasMemory -> MR.strings.pc_memory_reset_desc.text(); else -> MR.strings.pc_memory_empty.text() },
                        style = FFType.bodyMain.copy(fontSize = 13.5.sp, lineHeight = 18.sp), color = FFColors.onSurfaceVariant)
                }
                if (hasMemory) { Spacer(Modifier.width(12.dp)); FFIcon("hi_chevron_right", FFColors.onSurfaceVariant, 20.dp) }
            }
        }
        if (confirm) AlertDialog(onDismissRequest = { confirm = false }, containerColor = FFColors.canvas,
            title = { Text(MR.strings.pc_memory_reset_confirm_title.text(), style = FFType.sectionTitle) },
            text = { Text(MR.strings.pc_memory_reset_confirm_body.text(), style = FFType.bodyMain) },
            confirmButton = { TextButton({ confirm = false; update(p.copy(lastExpenseCategory = "", lastIncomeCategory = "")); done = true }, Modifier.testTag("memory-reset-confirm")) {
                Text(MR.strings.pc_memory_reset_action.text(), color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton({ confirm = false }) { Text(MR.strings.common_cancel.text()) } })
    }
    item {
        var picking by remember { mutableStateOf(false) }
        val wallets = listOf("" to MR.strings.set_pref_first_active_wallet.text()) + data.accounts.filterNot { it.archived || it.type == "savings" }.map { it.id to it.name }
        val current = wallets.firstOrNull { it.first == p.defaultWalletId } ?: wallets.first()
        PrefCard {
            PrefRow(MR.strings.set_pref_default_wallet.text(), current.second, "hi_wallet", iconCircle = true, onClick = { picking = true },
                modifier = Modifier.testTag("memory-default-wallet"))
        }
        if (picking) PrefChoiceSheet(MR.strings.set_pref_default_wallet.text(), wallets, current.first, "default-wallet", { picking = false }) { update(p.copy(defaultWalletId = it)) }
    }
}
