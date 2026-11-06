package com.logikawan.finflow.feature.report

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logikawan.finflow.core.model.*
import com.logikawan.finflow.core.ui.*
import com.logikawan.finflow.core.util.*
import kotlinx.datetime.*
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun Reports(data: FinanceData, month: String, monthChange: (String) -> Unit, export: () -> Unit,
    onTransactions: () -> Unit = {}, edit: (Transaction) -> Unit = {}, exportFiltered: ((List<Transaction>) -> Unit)? = null,
    onSectionOrder: (() -> Unit)? = null, onPreferences: (() -> Unit)? = null) {
    val preferences = LocalPreferences.current
    val currentDate = today()
    var kind by rememberSaveable { mutableStateOf("month") }
    var anchor by rememberSaveable { mutableStateOf(if (month == currentDate.toString().take(7)) currentDate.toString() else "$month-01") }
    var customStart by rememberSaveable { mutableStateOf(recordingPeriod(month, preferences.recordingDay).start.toString()) }
    var customEnd by rememberSaveable { mutableStateOf(recordingPeriod(month, preferences.recordingDay).endExclusive.plus(DatePeriod(days = -1)).toString()) }
    val period = if (kind == "custom") FinancePeriod(LocalDate.parse(customStart), LocalDate.parse(customEnd).plus(DatePeriod(days = 1)))
        else financePeriod(kind, LocalDate.parse(anchor), month, preferences.recordingDay, preferences.weekStart)
    val days = period.endExclusive.toEpochDays() - period.start.toEpochDays()
    val previous = when (kind) {
        "month" -> recordingPeriod(shiftMonth(month, -1), preferences.recordingDay)
        "year" -> financePeriod("year", LocalDate.parse(anchor).plus(DatePeriod(years = -1)))
        else -> FinancePeriod(period.start.plus(DatePeriod(days = -days)), period.start)
    }
    val all = data.reportTransactions()
    val currencies = (data.accounts.map { it.currency } + all.map { it.currency }).distinct()
        .ifEmpty { listOf(data.profile?.baseCurrency ?: "IDR") }.sortedBy { if (it == data.profile?.baseCurrency) 0 else 1 }
    var selectedCurrency by rememberSaveable { mutableStateOf("") }
    val currency = selectedCurrency.takeIf { it in currencies } ?: currencies.first()
    var walletId by rememberSaveable { mutableStateOf("") }
    val wallets = data.accounts.filter { it.currency == currency && !it.excludedFromTotal }
    val selectedWallet = walletId.takeIf { id -> wallets.any { it.id == id } }.orEmpty()
    var type by rememberSaveable { mutableStateOf("expense") }
    var query by rememberSaveable { mutableStateOf("") }
    var logLimit by rememberSaveable { mutableIntStateOf(5) }
    var advanced by rememberSaveable { mutableStateOf(false) }
    var filters by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf(false) }
    var draftKind by remember { mutableStateOf(kind) }
    var draftMonth by remember { mutableStateOf(month) }
    var draftAnchor by remember { mutableStateOf(anchor) }
    var draftStart by remember { mutableStateOf(customStart) }
    var draftEnd by remember { mutableStateOf(customEnd) }
    var draftCurrency by remember { mutableStateOf(currency) }
    var draftWallet by remember { mutableStateOf(selectedWallet) }
    var filterError by remember { mutableStateOf<String?>(null) }
    fun openFilters(mode: String = kind) {
        draftKind = mode; draftMonth = month; draftAnchor = anchor; draftStart = customStart; draftEnd = customEnd
        draftCurrency = currency; draftWallet = selectedWallet; filterError = null; filters = true
    }
    fun movePeriod(delta: Int) {
        when (kind) {
            "month" -> monthChange(shiftMonth(month, delta))
            "week" -> anchor = LocalDate.parse(anchor).plus(DatePeriod(days = delta * 7)).toString()
            "year" -> anchor = LocalDate.parse(anchor).plus(DatePeriod(years = delta)).toString()
            else -> {
                customStart = period.start.plus(DatePeriod(days = days * delta)).toString()
                customEnd = period.endExclusive.plus(DatePeriod(days = days * delta - 1)).toString()
            }
        }
    }
    fun shown(amount: Long) = if (preferences.hideBalances) "••••••" else formatBalance(amount, currency, preferences)
    fun percent(value: Long, of: Long) = if (of > 0) "${(value.toDouble() / of * 100).roundToInt()}%" else "0%"
    val eligible = all.filter { it.currency == currency && (selectedWallet.isEmpty() || it.accountId == selectedWallet) }
    val relevant = eligible.filter { it.date in period }
    val selected = relevant.filter { it.type == type }
    val total = selected.fold(0L) { sum, tx -> checkedAdd(sum, tx.amountMinor) }
    val previousTotal = eligible.filter { it.date in previous && it.type == type }.fold(0L) { sum, tx -> checkedAdd(sum, tx.amountMinor) }
    val income = relevant.filter { it.type == "income" }.fold(0L) { sum, tx -> checkedAdd(sum, tx.amountMinor) }
    val expense = relevant.filter { it.type == "expense" }.fold(0L) { sum, tx -> checkedAdd(sum, tx.amountMinor) }
    val activeColor = if (type == "income") FFColors.income else FFColors.expense
    val typeWord = if (type == "expense") "Pengeluaran" else "Pemasukan"
    val delta = checkedAdd(total, -previousTotal)
    val groups = selected.groupBy { it.categoryId }.map { (id, rows) -> id to rows.fold(0L) { sum, tx -> checkedAdd(sum, tx.amountMinor) } }.sortedByDescending { it.second }
    val groupColors = groups.map { (id, _) -> data.categories.find { it.id == id }?.color?.let { Color(it) } ?: categoryColor(id) }
    val groupCounts = selected.groupBy { it.categoryId }.mapValues { it.value.size }
    val bucketDays = if (days <= 366) 1 else (days + 119) / 120
    val incomeSeries = reportAmounts(period, relevant, "income", bucketDays)
    val expenseSeries = reportAmounts(period, relevant, "expense", bucketDays)
    var cumulative = 0L
    val netTrend = incomeSeries.indices.map { index -> cumulative = checkedAdd(cumulative, checkedAdd(incomeSeries[index], -expenseSeries[index])); cumulative }
    val elapsedDays = (currentDate.toEpochDays() - period.start.toEpochDays() + 1).coerceIn(0, days)
    val average = if (elapsedDays > 0) total / elapsedDays else 0L
    val projected = if (elapsedDays > 0) (total.toDouble() / elapsedDays * days).toLong() else 0L
    val averageIncome = if (elapsedDays > 0) income / elapsedDays else 0L
    val averageExpense = if (elapsedDays > 0) expense / elapsedDays else 0L
    val averageSaving = if (elapsedDays > 0) checkedAdd(income, -expense) / elapsedDays else 0L
    val trendTenths = if (previousTotal > 0) (abs(delta.toDouble() / previousTotal * 100) * 10).roundToInt() else 0
    val trendLabel = (if (delta > 0) "+" else if (delta < 0) "-" else "") + "${trendTenths / 10},${trendTenths % 10}%"
    val comparisonSentence = when {
        delta > 0 -> "$typeWord lebih tinggi dari periode sebelumnya."
        delta < 0 -> "$typeWord lebih rendah dari periode sebelumnya."
        else -> "$typeWord sama dengan periode sebelumnya."
    }
    val log = selected.filter { query.isBlank() || "${it.title} ${it.description} ${it.tags.joinToString(" ")} ${data.categoryName(it.categoryId)} ${data.accountName(it.accountId)}".contains(query, true) }
        .sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.createdAt })
    val heatEnd = if (currentDate >= period.start && currentDate < period.endExclusive) currentDate.plus(DatePeriod(days = 1)) else period.endExclusive
    val heatStart = maxOf(period.start, heatEnd.plus(DatePeriod(days = -70)))
    val heatPeriod = FinancePeriod(heatStart, heatEnd)
    val heatAmounts = reportAmounts(heatPeriod, selected, type)
    val periodLabel = if (kind == "month") monthLabel(month) else if (kind == "year") LocalDate.parse(anchor).year.toString() else "${period.start} – ${period.endExclusive.plus(DatePeriod(days = -1))}"
    val walletLabel = wallets.find { it.id == selectedWallet }?.name ?: "Semua Dompet"

    LazyColumn(Modifier.fillMaxSize().testTag("statistics"), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Box(Modifier.fillMaxWidth()) {
                Row(Modifier.align(Alignment.CenterStart)) {
                    IconButton({ openFilters() }, Modifier.testTag("statistics-filters")) { FFIcon("filter_list_rounded", contentDescription = "Filter statistik") }
                }
                Text("Statistik", Modifier.fillMaxWidth().align(Alignment.Center), style = FFType.sectionTitle, textAlign = TextAlign.Center)
                Row(Modifier.align(Alignment.CenterEnd)) {
                    IconButton({ info = true }, Modifier.testTag("statistics-info")) { FFIcon("info_outline_rounded", contentDescription = "Tentang statistik") }
                    Box {
                        IconButton({ menu = true }, Modifier.testTag("statistics-menu")) { FFIcon("more_vert", contentDescription = "Menu statistik") }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem(text = { Text("Ekspor CSV/PDF") }, onClick = { menu = false; exportFiltered?.invoke(selected) ?: export() })
                            DropdownMenuItem(text = { Text("Urutan Bagian Statistik") }, onClick = { menu = false; onSectionOrder?.invoke() })
                            DropdownMenuItem(text = { Text("Preferensi Tampilan") }, onClick = { menu = false; onPreferences?.invoke() })
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp)).background(FFColors.surface).padding(4.dp)) {
                listOf("week" to "Minggu", "month" to "Bulan", "year" to "Tahun", "custom" to "Rentang").forEach { (value, label) ->
                    val chosen = kind == value
                    Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(20.dp)).background(if (chosen) FFColors.canvas else Color.Transparent)
                        .testTag("stats-kind-$value").clickable {
                            if (value == "custom") openFilters("custom") else {
                                val contextDate = when (kind) {
                                    "custom" -> LocalDate.parse(customStart)
                                    "month" -> if (month == currentDate.toString().take(7)) currentDate else LocalDate.parse("$month-01")
                                    else -> LocalDate.parse(anchor)
                                }
                                if (value == "month" && kind != "month") monthChange(activeRecordingMonth(contextDate, preferences.recordingDay))
                                if (value != "month") anchor = contextDate.toString()
                                kind = value; logLimit = 5
                            }
                        }, contentAlignment = Alignment.Center) {
                        Text(label, style = FFType.metaData, fontWeight = if (chosen) FontWeight.Bold else FontWeight.Normal,
                            color = if (chosen) FFColors.onSurface else FFColors.onSurfaceVariant, maxLines = 1)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                FFIconButton("chevron_left_rounded", { movePeriod(-1) })
                Column(Modifier.weight(1f).clickable { openFilters() }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(periodLabel, style = FFType.metaData, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    Text("${wallets.find { it.id == selectedWallet }?.name ?: "Semua dompet"} · $currency", style = FFType.badgeLabel, color = FFColors.outline)
                }
                FFIconButton("chevron_right_rounded", { movePeriod(1) })
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("income" to "Pemasukan", "expense" to "Pengeluaran").forEach { (value, label) ->
                    val chosen = type == value
                    val color = if (value == "income") FFColors.income else FFColors.expense
                    Box(Modifier.weight(1f).height(40.dp).clip(CircleShape).background(if (chosen) color.copy(alpha = .12f) else FFColors.surface)
                        .clickable { type = value; logLimit = 5 }, contentAlignment = Alignment.Center) {
                        Text(label, style = FFType.metaData, fontWeight = FontWeight.SemiBold, color = if (chosen) color else FFColors.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            DesignCard {
                Text(if (type == "expense") "Total Pengeluaran" else "Total Pemasukan", style = FFType.metaData, color = FFColors.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    AllocationDonut(groups.map { it.second }, groupColors, total, shown(total))
                    Spacer(Modifier.width(14.dp))
                    if (groups.isEmpty()) Text("Belum ada transaksi pada periode ini.", Modifier.weight(1f), style = FFType.metaData, color = FFColors.outline)
                    else Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        groups.take(4).forEachIndexed { index, (id, value) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(groupColors[index]))
                                Spacer(Modifier.width(8.dp))
                                Text(data.categoryName(id), Modifier.weight(1f), style = FFType.metaData, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(percent(value, total), style = FFType.metaData, color = FFColors.outline)
                            }
                        }
                    }
                }
                if (groups.isNotEmpty()) groups.take(4).forEachIndexed { index, (id, value) ->
                    Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(groupColors[index].copy(alpha = .15f)), contentAlignment = Alignment.Center) {
                            val category = data.categories.find { it.id == id }
                            FFIcon(category?.icon?.takeIf { it.isNotBlank() } ?: categoryIcon(id), groupColors[index], 20.dp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(data.categoryName(id), style = FFType.metaData, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(progress = { if (total > 0) (value.toFloat() / total).coerceIn(0f, 1f) else 0f },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = groupColors[index], trackColor = FFColors.outlineVariant)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(percent(value, total), style = FFType.metaData, fontWeight = FontWeight.SemiBold)
                            Text(shown(value), style = FFType.badgeLabel.copy(fontSize = 10.sp), color = FFColors.outline)
                        }
                    }
                }
            }
        }
        item {
            DesignCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text(if (bucketDays == 1) "Ringkasan Harian" else "Ringkasan per $bucketDays Hari", Modifier.weight(1f), style = FFType.bodyMain, fontWeight = FontWeight.SemiBold)
                    Column(horizontalAlignment = Alignment.End) {
                        AverageLine("Rata Pemasukan", shown(averageIncome), FFColors.income)
                        AverageLine("Rata Pengeluaran", shown(averageExpense), FFColors.expense)
                        AverageLine("Rata Tabungan", shown(averageSaving), FFColors.primary)
                    }
                }
                Spacer(Modifier.height(16.dp))
                ReportBars(incomeSeries, expenseSeries)
                GraphDates(period)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    LegendDot("Pemasukan", FFColors.income); LegendDot("Pengeluaran", FFColors.expense); LegendDot("Tabungan", Color(0xFF3B82F6))
                }
            }
        }
        item {
            DesignCard {
                Text("Peta Aktivitas", style = FFType.bodyMain, fontWeight = FontWeight.SemiBold)
                if (heatStart > period.start) Text("70 hari terakhir dalam periode", style = FFType.metaData, color = FFColors.outline)
                Spacer(Modifier.height(12.dp))
                ActivityHeatmap(heatStart, heatAmounts, activeColor, preferences.weekStart)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Sedikit", style = FFType.badgeLabel.copy(fontSize = 10.sp), color = FFColors.outline)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(5) { step -> Box(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(activeColor.copy(alpha = .15f + .6f * (step + 1) / 5f))) }
                    }
                    Text("Banyak", style = FFType.badgeLabel.copy(fontSize = 10.sp), color = FFColors.outline)
                }
            }
        }
        preferences.statisticsOrder.forEach { section -> item(key = "statistics-$section") {
            when (section) {
                "comparison" -> DesignCard(Modifier.testTag("statistics-comparison")) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Perbandingan Bulanan", Modifier.weight(1f), style = FFType.bodyMain, fontWeight = FontWeight.SemiBold)
                        if (previousTotal > 0) {
                            val chipColor = when { delta > 0 -> FFColors.expense; delta < 0 -> FFColors.income; else -> FFColors.onSurfaceVariant }
                            val chipIcon = if (delta < 0) "arrow_downward_rounded" else "trending_up_rounded"
                            Row(Modifier.clip(RoundedCornerShape(50)).background(chipColor.copy(alpha = .12f)).padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                FFIcon(chipIcon, chipColor, 16.dp)
                                Spacer(Modifier.width(4.dp))
                                Text(trendLabel, style = FFType.badgeLabel, fontWeight = FontWeight.SemiBold, color = chipColor)
                            }
                        }
                    }
                    Text("${previous.start} – ${previous.endExclusive.plus(DatePeriod(days = -1))}", style = FFType.metaData, color = FFColors.outline)
                    Spacer(Modifier.height(4.dp))
                    Text(comparisonSentence, style = FFType.metaData, color = FFColors.onSurfaceVariant)
                    val maximum = maxOf(total, previousTotal, 1L).toFloat()
                    listOf("Sebelumnya" to previousTotal, "Periode ini" to total).forEachIndexed { index, (label, amount) ->
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, style = FFType.metaData); Text(shown(amount), style = FFType.metaData, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { (amount / maximum).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = if (index == 1) activeColor else FFColors.outline, trackColor = FFColors.outlineVariant)
                    }
                    Spacer(Modifier.height(12.dp)); Text("Selisih ${shown(delta)}", style = FFType.metaData, color = FFColors.onSurfaceVariant)
                }
                "trend" -> DesignCard(Modifier.testTag("statistics-trend")) {
                    Text("Tren Saldo Bersih", style = FFType.bodyMain, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp)); Text(shown(checkedAdd(income, -expense)), style = FFType.amountMd)
                    Text("Akumulasi pemasukan dikurangi pengeluaran periode ini. Transfer tidak menambah penghasilan.", style = FFType.metaData, color = FFColors.outline)
                    Spacer(Modifier.height(16.dp)); ReportLine(netTrend, FFColors.primary); GraphDates(period)
                }
                "cycle" -> DesignCard(Modifier.testTag("statistics-cycle")) {
                    Text("Wawasan Siklus", style = FFType.bodyMain, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(14.dp))
                    MetricRow("Rata-rata Harian", shown(average))
                    MetricRow("Proyeksi Total", shown(projected))
                    Text(if (elapsedDays == 0) "Periode belum dimulai." else if (elapsedDays < days) "Estimasi berdasarkan rata-rata $elapsedDays hari yang sudah berjalan; bukan transaksi atau tagihan mendatang." else "$days hari dalam periode.",
                        style = FFType.metaData, color = FFColors.outline)
                    TextButton(onClick = { advanced = !advanced }) { Text(if (advanced) "Tutup analisis lanjutan" else "Analisis lanjutan") }
                    if (advanced) {
                        MetricRow("Jumlah transaksi", selected.size.toString())
                        MetricRow("Transaksi terbesar", shown(selected.maxOfOrNull { it.amountMinor } ?: 0L))
                        MetricRow("Rata-rata per transaksi", shown(if (selected.isEmpty()) 0L else total / selected.size))
                        MetricRow("Pemasukan", shown(income)); MetricRow("Pengeluaran", shown(expense))
                        if (income > 0) MetricRow("Porsi saldo bersih", "${((income.toDouble() - expense) / income * 100).toInt()}%")
                    }
                }
                "categories" -> DesignCard(Modifier.testTag("statistics-categories")) {
                    Text(if (type == "expense") "Pengeluaran Terbesar" else "Pemasukan Terbesar", style = FFType.bodyMain, fontWeight = FontWeight.SemiBold)
                    if (groups.isEmpty()) Text("Belum ada transaksi pada periode ini.", Modifier.padding(top = 12.dp), style = FFType.metaData, color = FFColors.outline)
                    groups.forEachIndexed { index, (id, amount) ->
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(24.dp).clip(CircleShape).background(groupColors[index].copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                                Text("${index + 1}", style = FFType.badgeLabel.copy(fontSize = 10.sp), fontWeight = FontWeight.Bold, color = groupColors[index])
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(data.categoryName(id), style = FFType.metaData, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${groupCounts[id] ?: 0} transaksi", style = FFType.badgeLabel.copy(fontSize = 10.sp), color = FFColors.outline)
                            }
                            Text(shown(amount), style = FFType.metaData, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { if (total > 0) (amount.toFloat() / total).coerceIn(0f, 1f) else 0f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = groupColors[index], trackColor = FFColors.outlineVariant)
                    }
                }
                "transactions" -> DesignCard(Modifier.testTag("statistics-transactions")) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Log Transaksi", Modifier.weight(1f), style = FFType.bodyMain, fontWeight = FontWeight.SemiBold)
                        Text("${log.size}", style = FFType.metaData, color = FFColors.outline)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DesignPill("Semua", true, { openFilters() })
                        DesignPill(walletLabel, selectedWallet.isNotEmpty(), { openFilters() })
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(query, { query = it; logLimit = 5 }, placeholder = { Text("Cari transaksi atau tag...", style = FFType.metaData) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp))
                    if (log.isEmpty()) Text("Belum ada transaksi", Modifier.padding(vertical = 20.dp), style = FFType.metaData, color = FFColors.outline)
                    log.take(logLimit).forEach { tx ->
                        val category = data.categories.find { it.id == tx.categoryId }
                        val color = category?.color?.let { Color(it) } ?: categoryColor(tx.categoryId)
                        Row(Modifier.fillMaxWidth().clickable { edit(tx) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = .1f)), contentAlignment = Alignment.Center) {
                                FFIcon(category?.icon?.takeIf { it.isNotBlank() } ?: categoryIcon(tx.categoryId), color, 20.dp)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(tx.title, style = FFType.metaData, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${data.accountName(tx.accountId)} · ${tx.date}", style = FFType.badgeLabel.copy(fontSize = 10.sp, letterSpacing = 0.sp), color = FFColors.outline)
                            }
                            Text((if (tx.type == "expense") "−" else "+") + shown(tx.amountMinor), style = FFType.metaData, color = activeColor)
                        }
                    }
                    if (log.size > logLimit) TextButton(onClick = { logLimit += 20 }, modifier = Modifier.fillMaxWidth()) { Text("Tampilkan lebih banyak") }
                    OutlinedButton(onClick = onTransactions, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Buka Semua Transaksi", style = FFType.metaData) }
                }
            }
        } }
        item(key = "statistics-advanced") {
            DesignCard(Modifier.testTag("statistics-advanced")) {
                Text("Analisis Lanjutan", style = FFType.bodyMain, fontWeight = FontWeight.SemiBold)
                Text("Gambaran cepat arus kas dan pola pengeluaran terbaru.", style = FFType.metaData, color = FFColors.outline)
                Spacer(Modifier.height(16.dp))
                Text("Skor kesehatan finansial", style = FFType.metaData, color = FFColors.onSurfaceVariant)
                if (income > 0) {
                    val score = (checkedAdd(income, -expense).toDouble() / income * 100).roundToInt().coerceIn(0, 100)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(progress = { score / 100f }, modifier = Modifier.weight(1f).height(10.dp).clip(CircleShape),
                            color = if (score >= 50) FFColors.income else FFColors.expense, trackColor = FFColors.outlineVariant)
                        Spacer(Modifier.width(12.dp))
                        Text("$score/100", style = FFType.metaData, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(when { income > expense -> "Arus kas naik: pemasukan lebih besar daripada pengeluaran."; income == expense -> "Arus kas stabil: pemasukan sama dengan pengeluaran."; else -> "Arus kas turun: pengeluaran lebih besar daripada pemasukan." },
                        style = FFType.metaData, color = if (income >= expense) FFColors.income else FFColors.expense)
                } else {
                    Spacer(Modifier.height(6.dp))
                    Text("Tambahkan catatan pemasukan dan pengeluaran beberapa minggu untuk menghitung skor.", style = FFType.metaData, color = FFColors.outline)
                }
            }
        }
    }
    if (filters) AlertDialog(onDismissRequest = { filters = false }, title = { Text("Filter Statistik") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Choice("Periode", draftKind, listOf("week" to "Mingguan", "month" to "Bulanan", "year" to "Tahunan", "custom" to "Rentang tanggal"), { draftKind = it })
            if (draftKind == "custom") {
                Field("Mulai (YYYY-MM-DD)", draftStart, { draftStart = it })
                Field("Sampai (YYYY-MM-DD)", draftEnd, { draftEnd = it })
            } else if (draftKind == "month") MonthField(draftMonth, { draftMonth = it })
            else Field("Tanggal acuan (YYYY-MM-DD)", draftAnchor, { draftAnchor = it })
            Choice("Mata uang", draftCurrency, currencies.map { it to it }, { draftCurrency = it; draftWallet = "" })
            Choice("Dompet", draftWallet, listOf("" to "Semua dompet") + data.accounts.filter { it.currency == draftCurrency && !it.excludedFromTotal }.map {
                it.id to (it.name + if (it.archived) " (arsip)" else "")
            }, { draftWallet = it })
            Text("Dompet yang dikecualikan dari statistik tidak disertakan. Transfer tidak dihitung sebagai pemasukan atau pengeluaran.", style = FFType.metaData, color = FFColors.outline)
            filterError?.let { Text(it, style = FFType.metaData, color = FFColors.expense) }
        }
    }, confirmButton = { TextButton(onClick = {
        try {
            if (draftKind == "custom") {
                val start = LocalDate.parse(draftStart); val end = LocalDate.parse(draftEnd)
                require(start <= end && start.year in 1..9998 && end.year in 1..9998) { "Tanggal akhir harus sesudah atau sama dengan tanggal mulai" }
                customStart = start.toString(); customEnd = end.toString()
            } else if (draftKind == "month") {
                val first = LocalDate.parse("$draftMonth-01")
                require(first.year in 1..9998) { "Tahun harus 1–9998" }
                if (draftMonth != month) monthChange(draftMonth)
            } else {
                val date = LocalDate.parse(draftAnchor)
                require(date.year in 1..9998) { "Tahun harus 1–9998" }
                anchor = date.toString()
            }
            kind = draftKind; selectedCurrency = draftCurrency; walletId = draftWallet; logLimit = 5; filters = false
        } catch (e: Exception) { filterError = e.message ?: "Periksa periode tanggal" }
    }) { Text("Terapkan") } }, dismissButton = { TextButton(onClick = { filters = false }) { Text("Batal") } })
    if (info) AlertDialog(onDismissRequest = { info = false }, title = { Text("Tentang statistik") }, text = {
        Text("Statistik dihitung dari jurnal aktual pada periode dan dompet terpilih. Transfer tidak dihitung sebagai pemasukan/pengeluaran. Dompet yang dikecualikan dari statistik tidak disertakan.")
    }, confirmButton = { TextButton(onClick = { info = false }) { Text("Mengerti") } })
}

internal fun reportAmounts(period: FinancePeriod, transactions: List<Transaction>, type: String, bucketDays: Int = 1): List<Long> {
    require(bucketDays > 0)
    val days = period.endExclusive.toEpochDays() - period.start.toEpochDays()
    val amounts = MutableList((days + bucketDays - 1) / bucketDays) { 0L }
    transactions.filter { !it.scheduled && it.type == type && it.date in period }.forEach { tx ->
        val index = (LocalDate.parse(tx.date).toEpochDays() - period.start.toEpochDays()) / bucketDays
        amounts[index] = checkedAdd(amounts[index], tx.amountMinor)
    }
    return amounts
}

@Composable
private fun AllocationDonut(amounts: List<Long>, colors: List<Color>, total: Long, value: String) {
    val track = FFColors.outlineVariant
    Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(10.dp)) {
            val stroke = Stroke(20.dp.toPx(), cap = StrokeCap.Butt)
            drawCircle(track, style = stroke)
            var start = -90f
            amounts.forEachIndexed { index, amount ->
                val sweep = if (total > 0) amount.toDouble() / total * 360 else 0.0
                drawArc(colors[index], start, sweep.toFloat(), false, style = stroke)
                start += sweep.toFloat()
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Total", style = FFType.badgeLabel.copy(fontSize = 10.sp), color = FFColors.outline)
            Text(value, style = FFType.amountSm.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.testTag("statistics-total"), maxLines = 1, overflow = TextOverflow.Clip)
        }
    }
}

@Composable
private fun AverageLine(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = FFType.badgeLabel.copy(fontSize = 10.sp), color = FFColors.outline)
        Spacer(Modifier.width(4.dp))
        Text(value, style = FFType.badgeLabel.copy(fontSize = 10.sp), fontWeight = FontWeight.SemiBold, color = color, maxLines = 1)
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(label, style = FFType.badgeLabel.copy(fontSize = 10.sp), color = FFColors.onSurfaceVariant)
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = FFType.metaData, color = FFColors.onSurfaceVariant)
        Spacer(Modifier.width(12.dp)); Text(value, style = FFType.metaData, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun GraphDates(period: FinancePeriod) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(period.start.toString(), style = FFType.badgeLabel, color = FFColors.outline)
        Text(period.endExclusive.plus(DatePeriod(days = -1)).toString(), style = FFType.badgeLabel, color = FFColors.outline)
    }
}

@Composable
private fun ReportLine(values: List<Long>, color: Color) {
    val grid = FFColors.outlineVariant
    Canvas(Modifier.fillMaxWidth().height(140.dp)) {
        repeat(4) { row ->
            val y = 8.dp.toPx() + row * (size.height - 16.dp.toPx()) / 3
            drawLine(grid, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        }
        val maximum = maxOf(values.maxOrNull() ?: 0L, 0L).toDouble()
        val minimum = minOf(values.minOrNull() ?: 0L, 0L).toDouble()
        val scale = (maximum - minimum).coerceAtLeast(1.0)
        val points = values.mapIndexed { index, value -> Offset(index.toFloat() / (values.size - 1).coerceAtLeast(1) * size.width,
            size.height - 8.dp.toPx() - ((value.toDouble() - minimum) / scale * (size.height - 16.dp.toPx())).toFloat()) }
        points.zipWithNext().forEach { (from, to) -> drawLine(color, from, to, 2.dp.toPx(), StrokeCap.Round) }
        if (points.size == 1) drawCircle(color, 3.dp.toPx(), points.first())
    }
}

@Composable
private fun ReportBars(income: List<Long>, expense: List<Long>) {
    val grid = FFColors.outlineVariant
    val incomeColor = FFColors.income
    val expenseColor = FFColors.expense
    val maximum = maxOf(income.maxOrNull() ?: 0L, expense.maxOrNull() ?: 0L, 1L).toFloat()
    Canvas(Modifier.fillMaxWidth().height(140.dp)) {
        val base = size.height - 1.dp.toPx()
        drawLine(grid, Offset(0f, base), Offset(size.width, base), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx())))
        val count = maxOf(income.size, expense.size)
        if (count > 0) {
            val slot = size.width / count
            val barWidth = minOf(slot * .3f, 12.dp.toPx()).coerceAtLeast(2.dp.toPx())
            val gap = 2.dp.toPx()
            repeat(count) { index ->
                val startX = slot * index + (slot - (barWidth * 2 + gap)) / 2
                listOf(income.getOrElse(index) { 0L } to incomeColor, expense.getOrElse(index) { 0L } to expenseColor).forEachIndexed { bar, (value, color) ->
                    val barHeight = (value / maximum * base).coerceIn(0f, base)
                    if (barHeight > 0f) {
                        drawRoundRect(color, topLeft = Offset(startX + bar * (barWidth + gap), base - barHeight),
                            size = Size(barWidth, barHeight), cornerRadius = CornerRadius(barWidth / 2, barWidth / 2))
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityHeatmap(first: LocalDate, values: List<Long>, color: Color, weekStart: Int) {
    val offset = (first.dayOfWeek.isoDayNumber - weekStart + 7) % 7
    val rows = (offset + values.size + 6) / 7
    val maximum = (values.maxOrNull() ?: 0L).coerceAtLeast(1).toFloat()
    val week = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
    Row(Modifier.fillMaxWidth()) {
        repeat(7) { index -> Text(week[(weekStart - 1 + index) % 7], Modifier.weight(1f), style = FFType.badgeLabel.copy(fontSize = 10.sp), color = FFColors.outline, textAlign = TextAlign.Center) }
    }
    Spacer(Modifier.height(8.dp))
    repeat(rows) { row ->
        Row(Modifier.fillMaxWidth()) {
            repeat(7) { col ->
                val day = row * 7 + col - offset
                val value = values.getOrNull(day)
                Box(Modifier.weight(1f).padding(3.dp).height(28.dp).clip(RoundedCornerShape(6.dp))
                    .background(if (value != null && value > 0) color.copy(alpha = .15f + .6f * value / maximum) else Color.Transparent), contentAlignment = Alignment.Center) {
                    if (value != null) Text(first.plus(DatePeriod(days = day)).dayOfMonth.toString(), style = FFType.badgeLabel.copy(fontSize = 10.sp), color = FFColors.onSurface)
                }
            }
        }
    }
}
