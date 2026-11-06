package com.logikawan.finflow.feature.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.logikawan.finflow.core.model.*
import com.logikawan.finflow.core.ui.*
import com.logikawan.finflow.core.util.*
import com.logikawan.finflow.feature.planning.recurringOn
import kotlinx.datetime.*

/*
 * Halaman "Berulang" bergaya Ollo: kartu kalender bulan dengan total jadwal, empty state
 * dengan tombol pill tambah, dan kartu jadwal (ikon kategori, judul, nominal berwarna,
 * frekuensi + dompet, tanggal berikutnya, menu tiga titik edit/arsip). Form tambah/edit
 * mengikuti susunan Ollo: nominal, jenis, kategori lingkaran, judul, frekuensi + tanggal
 * mulai, ulangi setiap, tanggal akhir opsional, dan pemilih dompet WalletPicker.
 */

@Composable
fun RecurringListScreen(
    data: FinanceData,
    busy: Boolean,
    onAdd: () -> Unit,
    onEdit: (RecurringTransaction) -> Unit,
    onArchive: (RecurringTransaction) -> Unit,
    onProcess: () -> Unit,
    onCalendar: () -> Unit,
) {
    val preferences = LocalPreferences.current
    var month by rememberSaveable { mutableStateOf(today().toString().take(7)) }
    var selectedDate by rememberSaveable { mutableStateOf(today().toString()) }
    val days = calendarDates(month, preferences.weekStart)
    val active = data.recurring.filter { it.active }.sortedBy { it.nextDate }
    val occurrences = days.filter { it.toString().take(7) == month }.flatMap { date ->
        active.filter { recurringOn(it, date) }.map { rule -> rule to date }
    }
    val totalByCurrency = occurrences.groupBy { it.first.currency }
        .mapValues { (_, list) -> list.fold(0L) { sum, pair -> checkedAdd(sum, pair.first.amountMinor) } }
    val selectedCount = occurrences.count { it.second.toString() == selectedDate }

    if (!data.featuresReady) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                data.featuresError?.let { "Data fitur gagal dimuat. $it\nBuka ulang aplikasi untuk memuat kembali." } ?: "Memuat data…",
                style = FFType.bodyMain, color = if (data.featuresError != null) FFColors.expense else FFColors.outline,
            )
        }
        return
    }

    LazyColumn(
        Modifier.fillMaxSize().testTag("recurring-page"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            RecurringCalendarCard(
                month = month,
                monthChange = { month = it },
                days = days,
                selectedDate = if (selectedDate.startsWith(month)) selectedDate else "$month-01",
                select = { selectedDate = it },
                totalByCurrency = totalByCurrency,
                baseCurrency = data.profile?.baseCurrency ?: "IDR",
                count = occurrences.size,
                selectedCount = selectedCount,
                preferences = preferences,
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(enabled = !busy, onClick = onProcess) { Text("Proses jadwal", style = FFType.bodyMain, color = FFColors.onSurfaceVariant) }
                TextButton(onClick = onCalendar) { Text("Kalender terjadwal", style = FFType.bodyMain, color = FFColors.onSurfaceVariant) }
            }
        }
        if (active.isEmpty()) {
            item { RecurringEmptyState(onAdd) }
        } else {
            items(active, key = { it.id }) { rule ->
                RecurringItemCard(rule, data, preferences, onEdit = { onEdit(rule) }, onArchive = { onArchive(rule) })
            }
        }
    }
}

@Composable
private fun RecurringCalendarCard(
    month: String,
    monthChange: (String) -> Unit,
    days: List<LocalDate>,
    selectedDate: String,
    select: (String) -> Unit,
    totalByCurrency: Map<String, Long>,
    baseCurrency: String,
    count: Int,
    selectedCount: Int,
    preferences: AppPreferences,
) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(FFColors.surface).padding(horizontal = 12.dp, vertical = 14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(CircleShape).clickable { moveMonth(month, -1, monthChange) }.testTag("recurring-prev"), contentAlignment = Alignment.Center) {
                FFIcon("chevron_left_rounded", FFColors.outline, 22.dp, "Bulan sebelumnya")
            }
            Text(monthLabel(month), Modifier.weight(1f), style = FFType.sectionTitle, color = FFColors.onSurface, textAlign = TextAlign.Center, maxLines = 1)
            Box(Modifier.size(34.dp).clip(CircleShape).clickable { moveMonth(month, 1, monthChange) }.testTag("recurring-next"), contentAlignment = Alignment.Center) {
                FFIcon("chevron_right_rounded", FFColors.outline, 22.dp, "Bulan berikutnya")
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            days.take(7).forEach { day ->
                Text(
                    listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")[day.dayOfWeek.isoDayNumber - 1],
                    Modifier.weight(1f), style = FFType.bodyMain, color = FFColors.onSurfaceVariant, textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        days.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth().padding(bottom = 3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                week.forEach { day ->
                    val date = day.toString()
                    val inMonth = date.take(7) == month
                    val selected = date == selectedDate
                    Box(
                        Modifier.weight(1f).height(64.dp).clip(RoundedCornerShape(8.dp))
                            .background(if (selected) FFColors.surfaceContainerHigh else if (inMonth) FFColors.canvas else Color.Transparent)
                            .then(if (selected) Modifier.border(1.5.dp, FFColors.onSurface, RoundedCornerShape(8.dp)) else Modifier)
                            .clickable { select(date); if (!inMonth) monthChange(date.take(7)) },
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Text(
                            day.dayOfMonth.toString(),
                            Modifier.padding(top = 6.dp),
                            style = FFType.metaData,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (inMonth) FFColors.onSurface else FFColors.outline,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        HorizontalDivider(thickness = 1.dp, color = FFColors.outlineVariant)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Total berulang bulan ini", style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
                Text(
                    if (selectedCount > 0) "$selectedCount jadwal pada ${shortDateLabel(selectedDate)}" else "$count jadwal terjadwal",
                    style = FFType.metaData, color = FFColors.outline,
                )
            }
            Column(Modifier.width(150.dp), horizontalAlignment = Alignment.End) {
                if (totalByCurrency.isEmpty()) {
                    FitMoneyText(formatBalance(0L, baseCurrency, preferences), FFType.amountMd, FFColors.onSurface, TextAlign.End)
                } else {
                    totalByCurrency.forEach { (currency, total) ->
                        FitMoneyText(if (preferences.hideBalances) "••••••" else formatBalance(total, currency, preferences), FFType.amountMd, FFColors.onSurface, TextAlign.End)
                    }
                }
            }
        }
    }
}

private fun moveMonth(month: String, delta: Int, monthChange: (String) -> Unit) {
    monthChange(shiftMonth(month, delta))
}

@Composable
private fun RecurringEmptyState(onAdd: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        FFIcon("calendar_today_outlined", FFColors.outlineVariant, 56.dp)
        Spacer(Modifier.height(14.dp))
        Text("Belum ada jadwal berulang", style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.height(52.dp).clip(RoundedCornerShape(100.dp)).background(FFColors.primary)
                .clickable(onClick = onAdd).testTag("recurring-add").padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FFIcon("add_rounded", MaterialTheme.colorScheme.onPrimary, 20.dp)
            Spacer(Modifier.width(8.dp))
            Text("Tambah Berulang", style = FFType.bodyMain.copy(fontSize = 16.sp, lineHeight = 22.sp), fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun RecurringItemCard(rule: RecurringTransaction, data: FinanceData, preferences: AppPreferences, onEdit: () -> Unit, onArchive: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val color = categoryColor(rule.categoryId)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(FFColors.surface).clickable(onClick = onEdit).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(color.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                FFIcon(categoryIcon(rule.categoryId), color, 22.dp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(rule.title, style = FFType.bodyMain.copy(fontSize = 16.sp, lineHeight = 22.sp), fontWeight = FontWeight.SemiBold,
                    color = FFColors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${frequencyLabel(rule.frequency, rule.interval)} · ${data.accountName(rule.accountId)}",
                    style = FFType.metaData, color = FFColors.outline, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "Berikutnya ${shortDateLabel(rule.nextDate)}",
                    style = FFType.metaData, color = if (rule.nextDate < today().toString()) FFColors.expense else FFColors.outline,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.width(112.dp), horizontalAlignment = Alignment.End) {
                FitMoneyText(
                    if (preferences.hideBalances) "••••••" else formatBalance(rule.amountMinor, rule.currency, preferences),
                    FFType.amountMd, if (rule.type == "income") FFColors.income else FFColors.expense, TextAlign.End,
                )
                Text(if (rule.type == "income") "Masuk" else "Keluar", style = FFType.metaData, color = FFColors.outline)
            }
            Box {
                Box(
                    Modifier.size(34.dp).clip(CircleShape).clickable { menu = true }.testTag("recurring-menu-${rule.id}"),
                    contentAlignment = Alignment.Center,
                ) { FFIcon("more_vert", FFColors.outline, 20.dp, "Menu jadwal") }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text("Edit", style = FFType.bodyMain) }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(text = { Text("Arsipkan", style = FFType.bodyMain, color = FFColors.expense) }, onClick = { menu = false; onArchive() })
                }
            }
        }
        rule.endDate?.let {
            Spacer(Modifier.height(8.dp))
            Text("Berakhir ${shortDateLabel(it)}", style = FFType.metaData, color = FFColors.outline)
        }
    }
}

private fun frequencyLabel(frequency: String, interval: Int): String {
    val unit = when (frequency) { "daily" -> "hari"; "weekly" -> "minggu"; "yearly" -> "tahun"; else -> "bulan" }
    return if (interval == 1) "Setiap $unit" else "Setiap $interval $unit"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringFormEditor(
    old: RecurringTransaction?,
    data: FinanceData,
    busy: Boolean,
    dismiss: () -> Unit,
    save: (RecurringTransaction) -> Unit,
    archive: (() -> Unit)?,
) {
    val id = remember { old?.id ?: newId() }
    val walletOptions = data.accounts.filter { !it.archived && it.type != "savings" }
    var title by remember { mutableStateOf(old?.title.orEmpty()) }
    var type by remember { mutableStateOf(old?.type ?: "expense") }
    var accountId by remember { mutableStateOf(old?.accountId ?: walletOptions.firstOrNull()?.id.orEmpty()) }
    val currency = data.accounts.find { it.id == accountId }?.currency ?: old?.currency ?: "IDR"
    var category by remember { mutableStateOf(old?.categoryId ?: data.categories.firstOrNull { it.type == type }?.id.orEmpty()) }
    var amount by remember { mutableStateOf(old?.let { decimalAmount(it.amountMinor, it.currency) }.orEmpty()) }
    var frequency by remember { mutableStateOf(old?.frequency ?: "monthly") }
    var interval by remember { mutableIntStateOf(old?.interval ?: 1) }
    var date by remember { mutableStateOf(old?.nextDate ?: today().toString()) }
    var hasEnd by remember { mutableStateOf(old?.endDate != null) }
    var endDate by remember { mutableStateOf(old?.endDate ?: today().toString()) }
    var note by remember { mutableStateOf(old?.note.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    var datePicker by remember { mutableStateOf(false) }
    var endPicker by remember { mutableStateOf(false) }
    var frequencyMenu by remember { mutableStateOf(false) }
    val categories = data.categories.filter { it.type == type && (it.parentId == null || it.id == old?.categoryId) }

    fun submit() {
        if (busy) return
        try {
            require(title.isNotBlank()) { "Judul transaksi berulang wajib diisi" }
            require(categories.any { it.id == category }) { "Pilih kategori" }
            require(accountId.isNotBlank()) { "Pilih dompet pembayaran" }
            val value = minorAmount(amount, currency)
            val next = date
            val start = old?.takeIf { it.nextDate == next }?.startDate ?: next
            val anchor = if (old != null && old.nextDate == next) old.anchorDay else LocalDate.parse(next).dayOfMonth
            val recurring = RecurringTransaction(
                id = id, title = title.trim(), type = type, amountMinor = value, currency = currency,
                accountId = accountId, categoryId = category, frequency = frequency, interval = interval,
                anchorDay = anchor, startDate = start, nextDate = next,
                endDate = if (hasEnd) endDate else null, active = old?.active ?: true, note = note.trim(),
                lastEventId = old?.lastEventId, revision = old?.revision ?: 0, createdAt = old?.createdAt ?: nowMillis(),
            )
            PlanningValidation.recurring(recurring)
            error = null
            save(recurring)
        } catch (e: Exception) { error = e.message ?: "Data jadwal berulang tidak valid" }
    }

    Dialog(onDismissRequest = { if (!busy) dismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(Modifier.fillMaxSize().background(designBackground).safeDrawingPadding().imePadding().testTag("recurring-editor")) {
            Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).clickable { if (!busy) dismiss() }.testTag("recurring-close"), contentAlignment = Alignment.Center) {
                    FFIcon("close_rounded", FFColors.onSurface, 24.dp, "Tutup")
                }
                Text(if (old == null) "Berulang Baru" else "Edit Berulang", Modifier.weight(1f), style = FFType.appTitle, textAlign = TextAlign.Center, color = FFColors.onSurface)
                Box(Modifier.size(48.dp).clip(CircleShape).clickable { submit() }.testTag("recurring-save"), contentAlignment = Alignment.Center) {
                    if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else FFIcon("check_rounded", if (amount.isNotBlank()) FFColors.onSurface else FFColors.outline, 26.dp, "Simpan")
                }
            }
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(16.dp))
                OlloLabel("Nominal")
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.height(54.dp).clip(RoundedCornerShape(16.dp)).background(FFColors.surface).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (currency == "IDR") "Rp" else currency, style = FFType.amountLg.copy(fontSize = 30.sp, lineHeight = 34.sp), color = FFColors.onSurface)
                    }
                    Spacer(Modifier.width(14.dp))
                    BasicTextField(
                        value = amount, onValueChange = { amount = it },
                        modifier = Modifier.weight(1f).testTag("recurring-amount"),
                        singleLine = true, enabled = !busy,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = FFType.amountXl.copy(color = FFColors.onSurface),
                        cursorBrush = SolidColor(FFColors.primary),
                        decorationBox = { inner ->
                            Box { if (amount.isEmpty()) Text("0", style = FFType.amountXl, color = FFColors.outline); inner() }
                        },
                    )
                }
                Spacer(Modifier.height(22.dp))
                Row(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(16.dp)).background(FFColors.surface).padding(4.dp)) {
                    listOf("expense" to "Pengeluaran", "income" to "Pemasukan").forEach { (value, label) ->
                        val selected = type == value
                        Box(
                            Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp))
                                .background(if (selected) FFColors.canvas else Color.Transparent)
                                .clickable { if (!busy) { type = value; category = data.categories.firstOrNull { it.type == value }?.id.orEmpty() } },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(label, style = FFType.bodyMain, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) FFColors.onSurface else FFColors.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
                OlloLabel("Kategori")
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                    items(categories, key = { it.id }) { cat ->
                        OlloCategoryChip(cat.id, cat.name, category == cat.id, !busy) { category = cat.id }
                    }
                }
                Spacer(Modifier.height(22.dp))
                OlloLabel("Judul")
                Spacer(Modifier.height(10.dp))
                OlloTextField(title, { title = it.take(100) }, "cth. Gaji bulanan", enabled = !busy, tag = "recurring-title")
                Spacer(Modifier.height(22.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OlloLabel("Frekuensi")
                        Box {
                            Row(
                                Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(FFColors.surface)
                                    .clickable(enabled = !busy) { frequencyMenu = true }.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(frequencyOption(frequency), Modifier.weight(1f), style = FFType.bodyMain, color = FFColors.onSurface, maxLines = 1)
                                FFIcon("expand_more_rounded", FFColors.outline, 20.dp)
                            }
                            DropdownMenu(frequencyMenu, { frequencyMenu = false }) {
                                frequencyChoices.forEach { (value, label) ->
                                    DropdownMenuItem(text = { Text(label, style = FFType.bodyMain) }, onClick = {
                                        frequency = value; frequencyMenu = false
                                        interval = interval.coerceIn(1, 120)
                                    })
                                }
                            }
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OlloLabel(if (old == null) "Tanggal mulai" else "Jadwal berikutnya")
                        Row(
                            Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(FFColors.surface)
                                .clickable(enabled = !busy) { datePicker = true }.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FFIcon("calendar_today_outlined", FFColors.onSurfaceVariant, 20.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(shortDateLabel(date), Modifier.weight(1f), style = FFType.bodyMain, color = FFColors.onSurface, maxLines = 1)
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
                OlloLabel("Ulangi setiap (${periodUnit(frequency)})")
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp)).background(FFColors.surface).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IntervalButton("−", enabled = !busy && interval > 1) { interval = (interval - 1).coerceAtLeast(1) }
                    Text(interval.toString(), Modifier.width(52.dp), style = FFType.amountMd, color = FFColors.onSurface, textAlign = TextAlign.Center)
                    IntervalButton("+", enabled = !busy && interval < 120) { interval = (interval + 1).coerceAtMost(120) }
                    Spacer(Modifier.width(10.dp))
                    Text(periodUnit(frequency), style = FFType.bodyMain.copy(fontSize = 16.sp, lineHeight = 22.sp), color = FFColors.onSurfaceVariant)
                }
                Spacer(Modifier.height(22.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Tanggal akhir", style = FFType.bodyMain.copy(fontSize = 16.sp, lineHeight = 22.sp), fontWeight = FontWeight.Medium, color = FFColors.onSurfaceVariant)
                        Text("Transaksi berhenti setelah tanggal ini", style = FFType.metaData, color = FFColors.outline)
                    }
                    Switch(checked = hasEnd, onCheckedChange = { if (!busy) hasEnd = it })
                }
                if (hasEnd) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(FFColors.surface)
                            .clickable(enabled = !busy) { endPicker = true }.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FFIcon("calendar_today_outlined", FFColors.onSurfaceVariant, 20.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(shortDateLabel(endDate), Modifier.weight(1f), style = FFType.bodyMain, color = FFColors.onSurface)
                    }
                }
                Spacer(Modifier.height(24.dp))
                WalletChoice("Bayar dengan Dompet", accountId, data, walletOptions, !busy) { accountId = it }
                Spacer(Modifier.height(22.dp))
                OlloLabel("Catatan (opsional)")
                Spacer(Modifier.height(10.dp))
                OlloTextField(note, { note = it.take(4000) }, "Catatan", enabled = !busy, tag = "recurring-note")
                error?.let {
                    Spacer(Modifier.height(14.dp))
                    Text(it, style = FFType.bodyMain, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("recurring-error"))
                }
                archive?.let {
                    Spacer(Modifier.height(18.dp))
                    TextButton(enabled = !busy, onClick = it, modifier = Modifier.fillMaxWidth()) {
                        Text("Arsipkan jadwal berulang", style = FFType.bodyMain, color = FFColors.expense)
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
    if (datePicker) {
        OlloDatePickerDialog(date, { date = it; datePicker = false }, { datePicker = false })
    }
    if (endPicker) {
        OlloDatePickerDialog(endDate, { endDate = it; endPicker = false }, { endPicker = false })
    }
}

private val frequencyChoices = listOf("daily" to "Harian", "weekly" to "Mingguan", "monthly" to "Bulanan", "yearly" to "Tahunan")

private fun frequencyOption(frequency: String): String = frequencyChoices.firstOrNull { it.first == frequency }?.second ?: "Bulanan"

private fun periodUnit(frequency: String): String = when (frequency) { "daily" -> "hari"; "weekly" -> "minggu"; "yearly" -> "tahun"; else -> "bulan" }

@Composable
private fun OlloLabel(text: String) {
    Text(text, style = FFType.bodyMain.copy(fontSize = 16.sp, lineHeight = 22.sp), fontWeight = FontWeight.Medium, color = FFColors.onSurfaceVariant)
}

@Composable
private fun OlloTextField(value: String, change: (String) -> Unit, placeholder: String, enabled: Boolean = true, tag: String = "recurring-field") {
    BasicTextField(
        value = value, onValueChange = change,
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag(tag),
        singleLine = true, enabled = enabled,
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
private fun IntervalButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = FFType.amountMd.copy(fontSize = 22.sp, lineHeight = 26.sp),
            color = if (enabled) FFColors.onSurface else FFColors.outline)
    }
}

@Composable
private fun OlloCategoryChip(id: String, name: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val color = categoryColor(id)
    Column(
        Modifier.width(72.dp).clip(RoundedCornerShape(16.dp)).clickable(enabled = enabled, onClick = onClick).testTag("recurring-category-$id"),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OlloDatePickerDialog(value: String, pick: (String) -> Unit, dismiss: () -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = runCatching { LocalDate.parse(value).toEpochDays().toLong() * 86400000L }.getOrNull())
    DatePickerDialog(
        onDismissRequest = dismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis -> pick(LocalDate.fromEpochDays((millis / 86400000L).toInt()).toString()) }
            }) { Text("Pilih") }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("Batal") } },
    ) { DatePicker(state) }
}