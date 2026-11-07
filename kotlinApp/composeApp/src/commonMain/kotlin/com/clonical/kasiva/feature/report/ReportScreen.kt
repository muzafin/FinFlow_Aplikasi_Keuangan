package com.clonical.kasiva.feature.report

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import kotlin.math.pow
import kotlin.math.roundToLong
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
import com.clonical.kasiva.core.i18n.menuLabel
import com.clonical.kasiva.core.i18n.tabLabel
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.i18n.transactionTypeLabel
import com.clonical.kasiva.i18n.*
import com.clonical.kasiva.core.model.*
import com.clonical.kasiva.core.ui.*
import com.clonical.kasiva.core.util.*
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
    fun shown(amount: Long) = if (preferences.hideBalances) maskedBalance(amount, currency, preferences) else formatBalance(amount, currency, preferences)
    fun percent(value: Long, of: Long) = if (of > 0) "${(value.toDouble() / of * 100).roundToInt()}%" else "0%"
    val eligible = all.filter { it.currency == currency && (selectedWallet.isEmpty() || it.accountId == selectedWallet) }
    val relevant = eligible.filter { it.date in period }
    val selected = relevant.filter { it.type == type }
    val total = selected.fold(0L) { sum, tx -> checkedAdd(sum, tx.amountMinor) }
    val previousTotal = eligible.filter { it.date in previous && it.type == type }.fold(0L) { sum, tx -> checkedAdd(sum, tx.amountMinor) }
    val income = relevant.filter { it.type == "income" }.fold(0L) { sum, tx -> checkedAdd(sum, tx.amountMinor) }
    val expense = relevant.filter { it.type == "expense" }.fold(0L) { sum, tx -> checkedAdd(sum, tx.amountMinor) }
    val activeColor = if (type == "income") FFColors.income else FFColors.expense
    val delta = checkedAdd(total, -previousTotal)
    val groups = selected.groupBy { it.categoryId }.map { (id, rows) -> id to rows.fold(0L) { sum, tx -> checkedAdd(sum, tx.amountMinor) } }.sortedByDescending { it.second }
    val incomeTone = FFColors.income
    // Ollo: kategori pemasukan tanpa warna khusus memakai hijau pemasukan.
    val groupColors = groups.map { (id, _) -> data.categories.find { it.id == id }?.color?.let { Color(it) } ?: if (type == "income") incomeTone else categoryColor(id) }
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
    // Ollo: "Rata Tabungan" = setoran ke Target (goal_deposit), bukan selisih pemasukan-pengeluaran.
    val goalDeposits = data.transactions.filter { it.source == "goal_deposit" && it.currency == currency && it.date in period && (selectedWallet.isEmpty() || it.accountId == selectedWallet) }
        .fold(0L) { sum, tx -> checkedAdd(sum, tx.amountMinor) }
    val averageSaving = if (elapsedDays > 0) goalDeposits / elapsedDays else 0L
    val trendTenths = if (previousTotal > 0) (abs(delta.toDouble() / previousTotal * 100) * 10).roundToInt() else 0
    val trendLabel = (if (delta > 0) "+" else if (delta < 0) "-" else "") + "${trendTenths / 10},${trendTenths % 10}%"
    val comparisonSentence = when {
        delta > 0 -> if (type == "expense") MR.strings.dash_stats_expense_higher.text() else MR.strings.dash_stats_income_higher.text()
        delta < 0 -> if (type == "expense") MR.strings.dash_stats_expense_lower.text() else MR.strings.dash_stats_income_lower.text()
        else -> if (type == "expense") MR.strings.dash_stats_expense_same.text() else MR.strings.dash_stats_income_same.text()
    }
    val log = selected.filter { query.isBlank() || "${it.title} ${it.description} ${it.tags.joinToString(" ")} ${data.categoryName(it.categoryId)} ${data.accountName(it.accountId)}".contains(query, true) }
        .sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.createdAt })
    val heatEnd = if (currentDate >= period.start && currentDate < period.endExclusive) currentDate.plus(DatePeriod(days = 1)) else period.endExclusive
    val heatStart = maxOf(period.start, heatEnd.plus(DatePeriod(days = -70)))
    val heatPeriod = FinancePeriod(heatStart, heatEnd)
    val heatAmounts = reportAmounts(heatPeriod, selected, type)
    val periodLabel = if (kind == "month") monthLabel(month) else if (kind == "year") LocalDate.parse(anchor).year.toString() else "${period.start} – ${period.endExclusive.plus(DatePeriod(days = -1))}"
    val walletLabel = wallets.find { it.id == selectedWallet }?.name ?: MR.strings.dash_stats_all_wallets.text()

    // Susunan Ollo 1.7.9 (diukur di HP 480dpi): header tetap, pemilih periode yang menyusut saat digulir,
    // donut tanpa kartu, ringkasan harian, lalu kartu-kartu #101014 dengan judul 16–18sp.
    val listState = rememberLazyListState()
    val collapsed by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val kindLabels = listOf("week" to MR.strings.dash_stats_weekly.text(), "month" to MR.strings.dash_stats_monthly.text(), "year" to MR.strings.dash_stats_yearly.text(), "custom" to MR.strings.dash_stats_range.text())
    fun selectKind(value: String) {
        if (value == "custom") { openFilters("custom"); return }
        val contextDate = when (kind) {
            "custom" -> LocalDate.parse(customStart)
            "month" -> if (month == currentDate.toString().take(7)) currentDate else LocalDate.parse("$month-01")
            else -> LocalDate.parse(anchor)
        }
        if (value == "month" && kind != "month") monthChange(activeRecordingMonth(contextDate, preferences.recordingDay))
        if (value != "month") anchor = contextDate.toString()
        kind = value; logLimit = 5
    }
    val rangeLabel = "(${shortRange(period.start)} - ${shortRange(period.endExclusive.plus(DatePeriod(days = -1)))})"
    val subtitle = listOfNotNull(rangeLabel, wallets.find { it.id == selectedWallet }?.name, currency.takeIf { currencies.size > 1 }).joinToString(" · ")
    val chipBg = FFColors.chip.takeIf { FFColors.background.luminance() < .5f } ?: FFColors.surfaceContainer
    val heatColor = Color(0xFF00897B)
    var kindMenu by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
            IconButton({ openFilters() }, Modifier.align(Alignment.CenterStart).testTag("statistics-filters")) { FFIcon("hi_filter", FFColors.onSurface, 24.dp, contentDescription = MR.strings.dash_stats_cd_filter.text()) }
            Text(tabLabel("Statistik"), Modifier.align(Alignment.Center), style = FFType.appTitle.copy(fontSize = 20.sp))
            Row(Modifier.align(Alignment.CenterEnd)) {
                IconButton({ info = true }, Modifier.testTag("statistics-info")) { FFIcon("hi_info", FFColors.onSurface, 24.dp, contentDescription = MR.strings.dash_stats_about.text()) }
                Box {
                    IconButton({ menu = true }, Modifier.testTag("statistics-menu")) { FFIcon("more_vert", FFColors.onSurface, 24.dp, contentDescription = MR.strings.dash_stats_cd_menu.text()) }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text(MR.strings.dash_stats_export.text()) }, onClick = { menu = false; exportFiltered?.invoke(selected) ?: export() })
                        DropdownMenuItem(text = { Text(MR.strings.dash_stats_section_order.text()) }, onClick = { menu = false; onSectionOrder?.invoke() })
                        DropdownMenuItem(text = { Text(MR.strings.dash_stats_display_prefs.text()) }, onClick = { menu = false; onPreferences?.invoke() })
                    }
                }
            }
        }
        if (collapsed) Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                Row(Modifier.clip(RoundedCornerShape(20.dp)).background(chipBg).clickable { kindMenu = true }.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(kindLabels.first { it.first == kind }.second, style = FFType.metaData)
                    Spacer(Modifier.width(4.dp)); FFIcon("hi_chevron_down", FFColors.onSurface, 16.dp)
                }
                DropdownMenu(kindMenu, { kindMenu = false }) {
                    kindLabels.forEach { (value, label) -> DropdownMenuItem(text = { Text(label) }, onClick = { kindMenu = false; selectKind(value) }) }
                }
            }
            Column(Modifier.weight(1f).clickable { openFilters() }, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(periodLabel, style = FFType.bodyMain, color = FFColors.onSurfaceVariant.copy(alpha = 1f), maxLines = 1)
                Text(rangeLabel, style = FFType.badgeLabel.copy(fontSize = 9.sp, fontWeight = FontWeight.Normal), color = FFColors.onSurfaceVariant)
            }
            IconButton({ movePeriod(-1) }) { FFIcon("hi_chevron_left", FFColors.onSurface, 20.dp, contentDescription = MR.strings.dash_previous.text()) }
            IconButton({ movePeriod(1) }) { FFIcon("hi_chevron_right", FFColors.onSurface, 20.dp, contentDescription = MR.strings.dash_next.text()) }
        }
    LazyColumn(Modifier.fillMaxSize().testTag("statistics"), state = listState, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item(key = "period") {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.clip(RoundedCornerShape(24.dp)).background(chipBg).padding(4.dp)) {
                    kindLabels.forEach { (value, label) ->
                        val chosen = kind == value
                        Box(Modifier.height(36.dp).clip(RoundedCornerShape(20.dp)).background(if (chosen) FFColors.surface else Color.Transparent)
                            .testTag("stats-kind-$value").clickable { selectKind(value) }.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                            Text(label, style = FFType.metaData, fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (chosen) FFColors.onSurface else FFColors.onSurfaceVariant, maxLines = 1)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton({ movePeriod(-1) }) { FFIcon("hi_chevron_left", FFColors.onSurface, 22.dp, contentDescription = MR.strings.dash_previous.text()) }
                    Column(Modifier.weight(1f).clickable { openFilters() }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(periodLabel, style = FFType.sectionTitle, textAlign = TextAlign.Center)
                        Text(subtitle, style = FFType.badgeLabel.copy(fontSize = 10.sp, fontWeight = FontWeight.Normal), color = FFColors.onSurfaceVariant)
                    }
                    IconButton({ movePeriod(1) }) { FFIcon("hi_chevron_right", FFColors.onSurface, 22.dp, contentDescription = MR.strings.dash_next.text()) }
                }
            }
        }
        item(key = "type") {
            Row(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp)).background(chipBg).padding(4.dp)) {
                listOf("income" to transactionTypeLabel("income"), "expense" to transactionTypeLabel("expense")).forEach { (value, label) ->
                    val chosen = type == value
                    val color = if (value == "income") FFColors.income else FFColors.expense
                    Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(13.dp)).background(if (chosen) color.copy(alpha = .12f) else Color.Transparent)
                        .testTag("stats-type-$value").clickable { type = value; logLimit = 5 }, contentAlignment = Alignment.Center) {
                        Text(label, style = FFType.bodyMain.copy(fontSize = 15.sp), color = if (chosen) color else FFColors.onSurfaceVariant)
                    }
                }
            }
        }
        item(key = "overview") {
            if (groups.isEmpty()) Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(chipBg).padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(28.dp).clip(CircleShape).background(FFColors.surface), contentAlignment = Alignment.Center) { FFIcon("hi_info", FFColors.onSurfaceVariant, 16.dp) }
                Spacer(Modifier.width(12.dp))
                Text(MR.strings.dash_stats_no_data.text(), style = FFType.bodyMain)
                Box(Modifier.size(0.dp).testTag("statistics-total"))
            } else Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    AllocationDonut(groups.map { it.second }, groupColors, total, shown(total))
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        groups.take(6).forEachIndexed { index, (id, value) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(9.dp).clip(RoundedCornerShape(2.dp)).background(groupColors[index]))
                                Spacer(Modifier.width(7.dp))
                                Text(data.categoryName(id), style = FFType.metaData.copy(fontSize = 10.sp), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                                Spacer(Modifier.width(6.dp))
                                Text(percent(value, total), style = FFType.metaData.copy(fontSize = 10.sp), color = FFColors.onSurfaceVariant)
                            }
                        }
                    }
                }
                Column(Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(16.dp)).background(FFColors.surface).padding(horizontal = 12.dp, vertical = 4.dp)) {
                    groups.forEachIndexed { index, (id, value) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(groupColors[index].copy(alpha = .14f)), contentAlignment = Alignment.Center) {
                                val category = data.categories.find { it.id == id }
                                FFIcon(category?.icon?.takeIf { it.isNotBlank() } ?: categoryIcon(id), groupColors[index], 22.dp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(data.categoryName(id), Modifier.weight(1f), style = FFType.bodyMain.copy(fontSize = 14.5.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.width(100.dp).height(8.dp).clip(CircleShape).background(FFColors.outlineVariant)) {
                                        Box(Modifier.fillMaxHeight().fillMaxWidth(if (total > 0) (value.toFloat() / total).coerceIn(0f, 1f) else 0f).clip(CircleShape).background(groupColors[index]))
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(percent(value, total), style = FFType.bodyMain)
                                }
                                Text(shown(value), style = FFType.metaData, color = FFColors.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        item(key = "daily") {
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text(if (bucketDays == 1) MR.strings.dash_stats_daily_summary.text() else MR.strings.dash_stats_bucket_summary.text(bucketDays), Modifier.weight(1f), style = FFType.sectionTitle)
                    Column(horizontalAlignment = Alignment.End) {
                        AverageLine(MR.strings.dash_stats_avg_income.text(shown(averageIncome)), FFColors.income)
                        AverageLine(MR.strings.dash_stats_avg_expense.text(shown(averageExpense)), FFColors.expense)
                        AverageLine(MR.strings.dash_stats_avg_saving.text(shown(averageSaving)), Color(0xFF2196F3))
                    }
                }
                Spacer(Modifier.height(12.dp))
                ReportBars(incomeSeries, expenseSeries, period.start, bucketDays, averageIncome, averageSaving)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    LegendDot(transactionTypeLabel("income"), FFColors.income); Spacer(Modifier.width(16.dp))
                    LegendDot(transactionTypeLabel("expense"), FFColors.expense); Spacer(Modifier.width(16.dp))
                    LegendDot(menuLabel("Target"), Color(0xFF2196F3))
                }
            }
        }
        preferences.statisticsOrder.forEach { section -> item(key = "statistics-$section") {
            when (section) {
                "comparison" -> OlloCard(Modifier.testTag("statistics-comparison")) {
                    var compare by remember { mutableStateOf(false) }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (kind == "month") MR.strings.dash_stats_monthly_comparison.text() else MR.strings.dash_stats_period_comparison.text(), Modifier.weight(1f), style = FFType.bodyMain.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium))
                        val good = if (type == "expense") delta <= 0 else delta >= 0
                        val chipColor = if (good) FFColors.income else FFColors.expense
                        Row(Modifier.clip(RoundedCornerShape(10.dp)).background(chipColor.copy(alpha = .12f)).padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            FFIcon(if (delta < 0) "hi_trend_down" else "hi_trend_up", chipColor, 16.dp)
                            Spacer(Modifier.width(4.dp))
                            Text(if (previousTotal > 0) trendLabel else "100,0%", style = FFType.bodyMain.copy(fontSize = 13.sp), color = chipColor)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(comparisonSentence, style = FFType.metaData, color = FFColors.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    CompareToggle(compare) { compare = !compare }
                    Spacer(Modifier.height(8.dp))
                    val current = cumulative(reportAmounts(period, relevant, type))
                    val before = cumulative(reportAmounts(FinancePeriod(previous.start, previous.start.plus(DatePeriod(days = days))), eligible.filter { it.date in previous }, type))
                    TrendChart(current, if (compare) before else null, activeColor, todayIndex(period, currentDate), currency)
                }
                "trend" -> OlloCard(Modifier.testTag("statistics-trend")) {
                    var compare by remember { mutableStateOf(false) }
                    val net = checkedAdd(income, -expense)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(MR.strings.dash_stats_net_trend.text(), Modifier.weight(1f), style = FFType.bodyMain.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium))
                        val chipColor = if (net >= 0) FFColors.income else FFColors.expense
                        Text(shown(net), Modifier.clip(RoundedCornerShape(10.dp)).background(chipColor.copy(alpha = .12f)).padding(horizontal = 10.dp, vertical = 5.dp),
                            style = FFType.bodyMain.copy(fontSize = 13.sp), color = chipColor, maxLines = 1)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(MR.strings.dash_stats_net_balance.text(), Modifier.weight(1f), style = FFType.metaData, color = FFColors.onSurfaceVariant)
                        CompareToggle(compare) { compare = !compare }
                    }
                    Spacer(Modifier.height(8.dp))
                    val prevNet = run {
                        val p = FinancePeriod(previous.start, previous.start.plus(DatePeriod(days = days)))
                        val rows = eligible.filter { it.date in previous }
                        val inc = reportAmounts(p, rows, "income"); val exp = reportAmounts(p, rows, "expense")
                        cumulative(inc.indices.map { checkedAdd(inc[it], -exp[it]) })
                    }
                    TrendChart(netTrend, if (compare) prevNet else null, if (net >= 0) FFColors.income else FFColors.expense, todayIndex(period, currentDate), currency)
                    Text(MR.strings.dash_stats_net_note.text(), style = FFType.badgeLabel.copy(fontSize = 9.sp, fontWeight = FontWeight.Normal), color = FFColors.onSurfaceVariant.copy(alpha = .7f))
                }
                "cycle" -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OlloCard(Modifier.testTag("statistics-cycle")) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(MR.strings.dash_stats_daily_average.text(), style = FFType.metaData, color = FFColors.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                                FitMoneyText(shown(average), FFType.amountMd.copy(fontSize = 20.sp))
                            }
                            Box(Modifier.padding(horizontal = 12.dp).width(1.dp).height(52.dp).background(FFColors.hairline))
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text(MR.strings.dash_stats_projected_total.text(), style = FFType.metaData, color = FFColors.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                                FitMoneyText(shown(projected), FFType.amountMd.copy(fontSize = 20.sp), FFColors.onSurfaceVariant, TextAlign.End)
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF2196F3).copy(alpha = .08f)).padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            FFIcon("hi_info", Color(0xFF2196F3), 18.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(if (elapsedDays == 0) MR.strings.dash_stats_not_started.text() else if (elapsedDays < days) MR.strings.dash_stats_based_on_days.text(elapsedDays) else MR.strings.dash_stats_all_days.text(days),
                                style = FFType.metaData, color = Color(0xFF2196F3))
                        }
                    }
                    OlloCard {
                        Text(MR.strings.dash_stats_activity_map.text(), style = FFType.bodyMain.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium))
                        Spacer(Modifier.height(14.dp))
                        ActivityHeatmap(period.start, reportAmounts(period, selected, type), heatColor, preferences.weekStart, currency)
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                            Text(MR.strings.dash_stats_less.text(), style = FFType.metaData.copy(fontSize = 10.sp), color = FFColors.onSurfaceVariant)
                            Spacer(Modifier.width(6.dp))
                            repeat(5) { step -> Box(Modifier.padding(horizontal = 2.dp).size(14.dp).clip(RoundedCornerShape(4.dp)).background(heatColor.copy(alpha = .2f + .8f * step / 4f))) }
                            Spacer(Modifier.width(6.dp))
                            Text(MR.strings.dash_stats_more.text(), style = FFType.metaData.copy(fontSize = 10.sp), color = FFColors.onSurfaceVariant)
                        }
                    }
                }
                "categories" -> OlloCard(Modifier.testTag("statistics-categories")) {
                    var by by remember { mutableStateOf("category") }
                    var limit by remember { mutableIntStateOf(5) }
                    Text(if (type == "expense") MR.strings.dash_stats_top_expenses.text() else MR.strings.dash_stats_top_sources.text(), style = FFType.bodyMain.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium))
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OlloDropdown(if (by == "category") MR.strings.dash_stats_by_category.text() else MR.strings.dash_stats_by_wallet.text(), listOf("category" to MR.strings.dash_stats_by_category.text(), "wallet" to MR.strings.dash_stats_by_wallet.text()), chipBg) { by = it }
                        OlloDropdown(if (limit == Int.MAX_VALUE) MR.strings.common_all.text() else MR.strings.dash_stats_top_n.text(limit), listOf("5" to MR.strings.dash_stats_top_n.text(5), "10" to MR.strings.dash_stats_top_n.text(10), "all" to MR.strings.common_all.text()), chipBg) { limit = it.toIntOrNull() ?: Int.MAX_VALUE }
                    }
                    val ranked = if (by == "category") groups.map { (id, amount) -> Triple(data.categoryName(id), groupCounts[id] ?: 0, amount) }
                        else selected.groupBy { it.accountId }.map { (id, rows) -> Triple(data.accountName(id), rows.size, rows.fold(0L) { s, t -> checkedAdd(s, t.amountMinor) }) }.sortedByDescending { it.third }
                    if (ranked.isEmpty()) Text(MR.strings.dash_stats_no_tx_period.text(), Modifier.padding(top = 16.dp), style = FFType.metaData, color = FFColors.onSurfaceVariant)
                    ranked.take(limit).forEachIndexed { index, (name, count, amount) ->
                        Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(32.dp).clip(CircleShape).background(chipBg), contentAlignment = Alignment.Center) {
                                Text("${index + 1}", style = FFType.metaData, color = FFColors.onSurfaceVariant)
                            }
                            Spacer(Modifier.width(18.dp))
                            Column(Modifier.weight(1f)) {
                                Text(name, style = FFType.bodyMain.copy(fontSize = 14.5.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(MR.strings.dash_stats_tx_count.text(count), style = FFType.metaData, color = FFColors.onSurfaceVariant)
                            }
                            Text(shown(amount), style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
                        }
                    }
                }
                "transactions" -> OlloCard(Modifier.testTag("statistics-transactions")) {
                    var logType by remember { mutableStateOf("all") }
                    val rows = relevant.filter { logType == "all" || it.type == logType }
                        .filter { query.isBlank() || "${it.title} ${it.description} ${it.tags.joinToString(" ")} ${data.categoryName(it.categoryId)} ${data.accountName(it.accountId)}".contains(query, true) }
                        .sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.createdAt })
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(MR.strings.dash_stats_tx_log.text(), Modifier.weight(1f), style = FFType.sectionTitle)
                        Row(Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onTransactions).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(MR.strings.dash_see_all.text(), style = FFType.metaData)
                            FFIcon("hi_chevron_right", FFColors.onSurface, 16.dp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp)).background(chipBg).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        FFIcon("hi_search", FFColors.onSurfaceVariant, 20.dp)
                        Spacer(Modifier.width(14.dp))
                        Box(Modifier.weight(1f)) {
                            if (query.isEmpty()) Text(MR.strings.dash_stats_search_hint.text(), style = FFType.metaData, color = FFColors.onSurfaceVariant)
                            androidx.compose.foundation.text.BasicTextField(query, { query = it; logLimit = 5 }, Modifier.fillMaxWidth().testTag("statistics-search"), singleLine = true,
                                textStyle = FFType.metaData.copy(color = FFColors.onSurface), cursorBrush = androidx.compose.ui.graphics.SolidColor(FFColors.onSurface))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OlloDropdown(when (logType) { "income", "expense", "transfer" -> transactionTypeLabel(logType); else -> MR.strings.common_all.text() },
                            listOf("all" to MR.strings.common_all.text(), "income" to transactionTypeLabel("income"), "expense" to transactionTypeLabel("expense"), "transfer" to transactionTypeLabel("transfer")), null) { logType = it; logLimit = 5 }
                        Row(Modifier.clip(RoundedCornerShape(20.dp)).border(1.dp, FFColors.hairline, RoundedCornerShape(20.dp)).clickable { openFilters() }
                            .padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(walletLabel, style = FFType.metaData); Spacer(Modifier.width(6.dp)); FFIcon("hi_chevron_down", FFColors.onSurface, 16.dp)
                        }
                    }
                    if (rows.isEmpty()) Text(MR.strings.dash_no_transactions.text(), Modifier.padding(vertical = 20.dp), style = FFType.metaData, color = FFColors.onSurfaceVariant)
                    rows.take(logLimit).forEach { tx ->
                        val category = data.categories.find { it.id == tx.categoryId }
                        val color = if (tx.type == "income") FFColors.income else if (tx.type == "transfer") FFColors.transfer else category?.color?.let { Color(it) } ?: categoryColor(tx.categoryId)
                        Row(Modifier.fillMaxWidth().clickable { edit(tx) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(runCatching { LocalDate.parse(tx.date).let { "${it.dayOfMonth.toString().padStart(2, '0')}/${it.monthNumber.toString().padStart(2, '0')}" } }.getOrDefault(tx.date),
                                Modifier.width(52.dp), style = FFType.metaData.copy(fontSize = 10.5.sp), color = FFColors.onSurfaceVariant)
                            Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
                                FFIcon(if (tx.type == "transfer") "hi_swap" else category?.icon?.takeIf { it.isNotBlank() } ?: categoryIcon(tx.categoryId), color, 16.dp)
                            }
                            Spacer(Modifier.width(14.dp))
                            Text(tx.title, Modifier.weight(1f), style = FFType.metaData, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val sign = when (tx.type) { "expense" -> "−"; "income" -> "+"; else -> "" }
                            Text(sign + if (preferences.hideBalances) maskedBalance(tx.amountMinor, tx.currency, preferences) else compactDisplayMoney(tx.amountMinor, tx.currency), style = FFType.metaData,
                                color = when (tx.type) { "income" -> FFColors.onSurface; "expense" -> FFColors.expense; "transfer" -> FFColors.transfer; else -> FFColors.onSurfaceVariant })
                        }
                    }
                    if (rows.size > logLimit) TextButton(onClick = { logLimit += 20 }, modifier = Modifier.fillMaxWidth()) { Text(MR.strings.dash_stats_show_more.text(), style = FFType.metaData) }
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, FFColors.onSurface.copy(alpha = .7f), RoundedCornerShape(12.dp))
                        .clickable(onClick = onTransactions), contentAlignment = Alignment.Center) {
                        Text(MR.strings.dash_stats_see_all_transactions.text(), style = FFType.metaData)
                    }
                }
            }
        } }
        item(key = "statistics-advanced") {
            OlloCard(Modifier.testTag("statistics-advanced")) {
                AdvancedAnalysis(data, eligible, currency, currentDate, preferences, chipBg, ::shown)
            }
        }
    }
    }
    if (filters) AlertDialog(onDismissRequest = { filters = false }, title = { Text(MR.strings.dash_stats_filter_title.text()) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Choice(MR.strings.dash_stats_period.text(), draftKind, listOf("week" to MR.strings.dash_stats_weekly.text(), "month" to MR.strings.dash_stats_monthly.text(), "year" to MR.strings.dash_stats_yearly.text(), "custom" to MR.strings.dash_stats_date_range.text()), { draftKind = it })
            if (draftKind == "custom") {
                Field(MR.strings.dash_stats_start_date.text(), draftStart, { draftStart = it })
                Field(MR.strings.dash_stats_end_date.text(), draftEnd, { draftEnd = it })
            } else if (draftKind == "month") MonthField(draftMonth, { draftMonth = it })
            else Field(MR.strings.dash_stats_anchor_date.text(), draftAnchor, { draftAnchor = it })
            Choice(MR.strings.dash_stats_currency.text(), draftCurrency, currencies.map { it to it }, { draftCurrency = it; draftWallet = "" })
            Choice(MR.strings.dash_stats_by_wallet.text(), draftWallet, listOf("" to MR.strings.dash_stats_all_wallets_lower.text()) + data.accounts.filter { it.currency == draftCurrency && !it.excludedFromTotal }.map {
                it.id to (if (it.archived) MR.strings.dash_stats_archived_name.text(it.name) else it.name)
            }, { draftWallet = it })
            Text(MR.strings.dash_stats_filter_note.text(), style = FFType.metaData, color = FFColors.outline)
            filterError?.let { Text(it, style = FFType.metaData, color = FFColors.expense) }
        }
    }, confirmButton = { TextButton(onClick = {
        try {
            if (draftKind == "custom") {
                val start = LocalDate.parse(draftStart); val end = LocalDate.parse(draftEnd)
                require(start <= end && start.year in 1..9998 && end.year in 1..9998) { MR.strings.dash_stats_err_end_before_start.text() }
                customStart = start.toString(); customEnd = end.toString()
            } else if (draftKind == "month") {
                val first = LocalDate.parse("$draftMonth-01")
                require(first.year in 1..9998) { MR.strings.dash_stats_err_year_range.text() }
                if (draftMonth != month) monthChange(draftMonth)
            } else {
                val date = LocalDate.parse(draftAnchor)
                require(date.year in 1..9998) { MR.strings.dash_stats_err_year_range.text() }
                anchor = date.toString()
            }
            kind = draftKind; selectedCurrency = draftCurrency; walletId = draftWallet; logLimit = 5; filters = false
        } catch (e: Exception) { filterError = e.message ?: MR.strings.dash_stats_err_check_period.text() }
    }) { Text(MR.strings.dash_stats_apply.text()) } }, dismissButton = { TextButton(onClick = { filters = false }) { Text(MR.strings.common_cancel.text()) } })
    if (info) AlertDialog(onDismissRequest = { info = false }, title = { Text(MR.strings.dash_stats_about.text()) }, text = {
        Text(MR.strings.dash_stats_about_body.text())
    }, confirmButton = { TextButton(onClick = { info = false }) { Text(MR.strings.dash_stats_got_it.text()) } })
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

internal fun cumulative(values: List<Long>): List<Long> {
    var sum = 0L
    return values.map { sum = checkedAdd(sum, it); sum }
}

private fun todayIndex(period: FinancePeriod, today: LocalDate): Int? =
    if (today >= period.start && today < period.endExclusive) today.toEpochDays() - period.start.toEpochDays() else null

private fun shortRange(date: LocalDate) = MR.strings.dash_day_month.text(date.dayOfMonth, shortMonthName(date.monthNumber))

// Sumbu Ollo memakai satuan ringkas Indonesia: 6.5jt, 3.2jt.
private fun axisLabel(value: Long, currency: String): String = compactDisplayMoney(value, currency).removePrefix("Rp").removePrefix("$currency ")

@Composable
private fun OlloCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(FFColors.surface).padding(horizontal = 16.dp, vertical = 18.dp), content = content)
}

@Composable
private fun OlloDropdown(label: String, options: List<Pair<String, String>>, background: Color?, select: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        val shape = RoundedCornerShape(if (background != null) 14.dp else 20.dp)
        Row(Modifier.clip(shape).then(if (background != null) Modifier.background(background) else Modifier.border(1.dp, FFColors.hairline, shape))
            .clickable { open = true }.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = FFType.metaData)
            Spacer(Modifier.width(if (background != null) 18.dp else 6.dp))
            FFIcon("hi_chevron_down", FFColors.onSurface, 16.dp)
        }
        DropdownMenu(open, { open = false }) {
            options.forEach { (value, text) -> DropdownMenuItem(text = { Text(text) }, onClick = { open = false; select(value) }) }
        }
    }
}

@Composable
private fun CompareToggle(active: Boolean, toggle: () -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = toggle).padding(horizontal = 6.dp, vertical = 4.dp).testTag("statistics-compare"),
        verticalAlignment = Alignment.CenterVertically) {
        FFIcon("hi_sort", if (active) FFColors.onSurface else FFColors.onSurfaceVariant, 18.dp)
        Spacer(Modifier.width(6.dp))
        Text(MR.strings.dash_stats_compare.text(), style = FFType.metaData, color = if (active) FFColors.onSurface else FFColors.onSurfaceVariant)
    }
}

// Garis kumulatif bergaya Ollo: area terisi tipis, garis putus-putus setelah hari ini, label sumbu Y ringkas.
@Composable
private fun TrendChart(values: List<Long>, previous: List<Long>?, color: Color, today: Int?, currency: String) {
    val axis = FFColors.onSurfaceVariant
    val grid = FFColors.outlineVariant
    val measurer = rememberTextMeasurer()
    val labelStyle = FFType.metaData.copy(fontSize = 8.sp, color = axis)
    val maximum = maxOf(values.maxOrNull() ?: 0L, previous?.maxOrNull() ?: 0L, 1L)
    val minimum = minOf(values.minOrNull() ?: 0L, previous?.minOrNull() ?: 0L, 0L)
    val top = maximum + (maximum - minimum) / 12
    Canvas(Modifier.fillMaxWidth().height(170.dp).testTag("statistics-trend-chart")) {
        val left = 28.dp.toPx(); val bottom = size.height - 18.dp.toPx(); val chartTop = 8.dp.toPx()
        val range = (top - minimum).coerceAtLeast(1L).toFloat()
        fun y(v: Long) = bottom - (v - minimum) / range * (bottom - chartTop)
        val count = values.size.coerceAtLeast(1)
        fun x(i: Int) = left + i.toFloat() / (count - 1).coerceAtLeast(1) * (size.width - left - 4.dp.toPx())
        listOf(minimum, (minimum + top) / 2, top).forEach { v ->
            val label = measurer.measure(axisLabel(v, currency), labelStyle)
            drawText(label, topLeft = Offset(0f, y(v) - label.size.height / 2f))
        }
        drawLine(grid, Offset(left, bottom), Offset(size.width, bottom), 1.dp.toPx())
        listOf(0, count / 3, count * 2 / 3).distinct().forEach { i ->
            val label = measurer.measure("${i + 1}", labelStyle)
            drawText(label, topLeft = Offset(x(i) - label.size.width / 2f, bottom + 4.dp.toPx()))
        }
        previous?.let { prev ->
            val path = Path()
            prev.take(count).forEachIndexed { i, v -> if (i == 0) path.moveTo(x(i), y(v)) else path.lineTo(x(i), y(v)) }
            drawPath(path, axis.copy(alpha = .6f), style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))))
        }
        val end = (today ?: (count - 1)).coerceIn(0, count - 1)
        val solid = Path()
        for (i in 0..end) { if (i == 0) solid.moveTo(x(i), y(values.getOrElse(i) { 0L })) else solid.lineTo(x(i), y(values.getOrElse(i) { 0L })) }
        val fill = Path().apply { addPath(solid); lineTo(x(end), bottom); lineTo(x(0), bottom); close() }
        drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = .22f), color.copy(alpha = 0f)), chartTop, bottom))
        drawPath(solid, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        if (end < count - 1) {
            val last = y(values.getOrElse(end) { 0L })
            drawLine(color.copy(alpha = .6f), Offset(x(end), last), Offset(x(count - 1), last), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())))
        }
    }
}

@Composable
private fun AllocationDonut(amounts: List<Long>, colors: List<Color>, total: Long, value: String) {
    val track = FFColors.outlineVariant
    Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(13.dp)) {
            val stroke = Stroke(26.dp.toPx(), cap = StrokeCap.Butt)
            drawCircle(track, style = stroke)
            var start = -90f
            amounts.forEachIndexed { index, amount ->
                val sweep = if (total > 0) amount.toDouble() / total * 360 else 0.0
                drawArc(colors[index], start, sweep.toFloat(), false, style = stroke)
                start += sweep.toFloat()
            }
        }
        Column(Modifier.padding(horizontal = 34.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(MR.strings.dash_stats_total.text(), style = FFType.metaData.copy(fontSize = 10.sp), color = FFColors.onSurfaceVariant)
            FitMoneyText(value, FFType.amountSm.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), FFColors.onSurface, TextAlign.Center, Modifier.testTag("statistics-total"))
        }
    }
}

@Composable
private fun AverageLine(text: String, color: Color) {
    Text(text, style = FFType.metaData.copy(fontSize = 10.5.sp), color = color, maxLines = 1)
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).background(color, CircleShape))
        Spacer(Modifier.width(7.dp))
        Text(label, style = FFType.metaData, color = FFColors.onSurface)
    }
}

// Batang harian Ollo: dapat digeser, garis rata-rata putus-putus (hijau = pemasukan, biru = tabungan).
@Composable
private fun ReportBars(income: List<Long>, expense: List<Long>, start: LocalDate, bucketDays: Int, averageIncome: Long, averageSaving: Long) {
    val incomeColor = FFColors.income
    val expenseColor = FFColors.expense
    val savingColor = Color(0xFF2196F3)
    val axis = FFColors.onSurfaceVariant
    val measurer = rememberTextMeasurer()
    val labelStyle = FFType.metaData.copy(color = axis)
    val count = maxOf(income.size, expense.size)
    val maximum = maxOf(income.maxOrNull() ?: 0L, expense.maxOrNull() ?: 0L, averageIncome, averageSaving, 1L).toFloat()
    val slot = 32.dp
    val firstWithData = (0 until count).firstOrNull { income.getOrElse(it) { 0L } > 0 || expense.getOrElse(it) { 0L } > 0 } ?: 0
    val scroll = rememberScrollState()
    val density = androidx.compose.ui.platform.LocalDensity.current
    LaunchedEffect(count, firstWithData) { scroll.scrollTo(with(density) { (slot * firstWithData).roundToPx() }) }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width = maxOf(maxWidth, slot * count)
        Box(Modifier.fillMaxWidth().horizontalScroll(scroll)) {
            Canvas(Modifier.width(width).height(200.dp).testTag("statistics-daily-bars")) {
                val bottom = size.height - 22.dp.toPx()
                val slotPx = size.width / count.coerceAtLeast(1)
                fun y(v: Float) = bottom - v / maximum * (bottom - 8.dp.toPx())
                val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))
                if (averageIncome > 0) drawLine(incomeColor.copy(alpha = .8f), Offset(0f, y(averageIncome.toFloat())), Offset(size.width, y(averageIncome.toFloat())), 1.5.dp.toPx(), pathEffect = dash)
                drawLine(savingColor.copy(alpha = .6f), Offset(0f, y(averageSaving.coerceAtLeast(0).toFloat())), Offset(size.width, y(averageSaving.coerceAtLeast(0).toFloat())), 1.5.dp.toPx(), pathEffect = dash)
                val barWidth = 4.dp.toPx()
                repeat(count) { index ->
                    val center = slotPx * index + slotPx / 2
                    listOf(income.getOrElse(index) { 0L } to incomeColor, expense.getOrElse(index) { 0L } to expenseColor).forEachIndexed { bar, (value, color) ->
                        if (value > 0) drawRoundRect(color, topLeft = Offset(center - barWidth - 1.dp.toPx() + bar * (barWidth + 2.dp.toPx()), y(value.toFloat())),
                            size = Size(barWidth, bottom - y(value.toFloat())), cornerRadius = CornerRadius(barWidth / 2, barWidth / 2))
                    }
                    val label = measurer.measure("${start.plus(DatePeriod(days = index * bucketDays)).dayOfMonth}", labelStyle)
                    drawText(label, topLeft = Offset(center - label.size.width / 2f, bottom + 6.dp.toPx()))
                }
            }
        }
    }
}

// Heatmap satu periode penuh seperti Ollo: kotak tanggal, kosong #09090C, aktif teal, nominal ringkas di pojok.
@Composable
private fun ActivityHeatmap(first: LocalDate, values: List<Long>, color: Color, weekStart: Int, currency: String) {
    val shown = values.take(42)
    val offset = (first.dayOfWeek.isoDayNumber - weekStart + 7) % 7
    val rows = (offset + shown.size + 6) / 7
    val maximum = (shown.maxOrNull() ?: 0L).coerceAtLeast(1).toFloat()
    val week = (1..7).map { dayInitial(it) }
    val empty = if (FFColors.background.luminance() < .5f) FFColors.deep else FFColors.surfaceContainer
    Row(Modifier.fillMaxWidth()) {
        repeat(7) { index -> Text(week[(weekStart - 1 + index) % 7], Modifier.weight(1f), style = FFType.metaData.copy(fontSize = 10.sp), color = FFColors.onSurfaceVariant, textAlign = TextAlign.Center) }
    }
    Spacer(Modifier.height(8.dp))
    repeat(rows) { row ->
        Row(Modifier.fillMaxWidth()) {
            repeat(7) { col ->
                val day = row * 7 + col - offset
                val value = shown.getOrNull(day)
                Box(Modifier.weight(1f).padding(2.5.dp).aspectRatio(1f).clip(RoundedCornerShape(6.dp))
                    .background(when { value == null -> empty.copy(alpha = .5f); value > 0 -> color.copy(alpha = .35f + .65f * value / maximum); else -> empty })) {
                    if (value != null) {
                        Text(first.plus(DatePeriod(days = day)).dayOfMonth.toString(), Modifier.align(Alignment.TopStart).padding(5.dp),
                            style = FFType.metaData.copy(fontSize = 10.sp), color = if (value > 0) Color.White else FFColors.onSurfaceVariant)
                        if (value > 0) Text(axisLabel(value, currency), Modifier.align(Alignment.BottomEnd).padding(4.dp),
                            style = FFType.metaData.copy(fontSize = 8.sp), color = Color.White, maxLines = 1)
                    }
                }
            }
        }
    }
}

// Analisis Lanjutan Ollo: skor 8 minggu lengkap, rata-rata per kategori, arus kas 12 bulan, kalkulator.
@Composable
private fun AdvancedAnalysis(data: FinanceData, eligible: List<Transaction>, currency: String, today: LocalDate, preferences: AppPreferences, chipBg: Color, shown: (Long) -> String) {
    val divider = FFColors.hairline
    Text(MR.strings.dash_adv_title.text(), style = FFType.sectionTitle)
    Text(MR.strings.dash_adv_subtitle.text(), style = FFType.metaData, color = FFColors.onSurfaceVariant)
    Spacer(Modifier.height(20.dp))
    Text(MR.strings.dash_adv_health_score.text(), style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
    Spacer(Modifier.height(10.dp))
    val weekEnd = today.minus(DatePeriod(days = (today.dayOfWeek.isoDayNumber - preferences.weekStart + 7) % 7))
    val weeks = (1..8).map { FinancePeriod(weekEnd.minus(DatePeriod(days = it * 7)), weekEnd.minus(DatePeriod(days = (it - 1) * 7))) }
    val weekly = weeks.map { w -> eligible.filter { it.date in w } }
    val activeWeeks = weekly.count { rows -> rows.any { it.type == "income" || it.type == "expense" } }
    if (activeWeeks < 3) Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(chipBg), contentAlignment = Alignment.Center) { Text("···", style = FFType.bodyMain, color = FFColors.onSurfaceVariant) }
        Spacer(Modifier.width(12.dp))
        Text(MR.strings.dash_adv_score_empty.text(), style = FFType.bodyMain.copy(fontSize = 15.sp))
    } else {
        val inc = weekly.flatten().filter { it.type == "income" }.fold(0L) { s, t -> checkedAdd(s, t.amountMinor) }
        val exp = weekly.flatten().filter { it.type == "expense" }.fold(0L) { s, t -> checkedAdd(s, t.amountMinor) }
        val score = if (inc > 0) (50 + (checkedAdd(inc, -exp).toDouble() / inc * 50)).roundToInt().coerceIn(0, 100) else 0
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(10.dp).clip(CircleShape).background(FFColors.outlineVariant)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(score / 100f).clip(CircleShape).background(if (score >= 50) FFColors.income else FFColors.expense))
            }
            Spacer(Modifier.width(12.dp)); Text("$score/100", style = FFType.bodyMain)
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(MR.strings.dash_adv_score_note.text(), style = FFType.metaData, color = FFColors.onSurfaceVariant)
    Box(Modifier.padding(vertical = 18.dp).fillMaxWidth().height(1.dp).background(divider))

    var basis by remember { mutableStateOf("week") }
    var showAll by remember { mutableStateOf(false) }
    Text(MR.strings.dash_adv_category_average.text(), style = FFType.bodyMain)
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("day" to MR.strings.dash_adv_daily.text(), "week" to MR.strings.dash_stats_weekly.text(), "month" to MR.strings.dash_stats_monthly.text()).forEach { (value, label) ->
            val chosen = basis == value
            Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(20.dp)).background(if (chosen) FFColors.onSurface else FFColors.background)
                .clickable { basis = value }, contentAlignment = Alignment.Center) {
                Text(label, style = FFType.bodyMain, color = if (chosen) FFColors.background else FFColors.onSurface)
            }
        }
    }
    val (window, divisor, caption) = when (basis) {
        "day" -> Triple(FinancePeriod(today.minus(DatePeriod(days = 30)), today), 30, MR.strings.dash_adv_per_day.text())
        "month" -> Triple(FinancePeriod(today.minus(DatePeriod(months = 3)), today), 3, MR.strings.dash_adv_per_month.text())
        else -> Triple(FinancePeriod(weekEnd.minus(DatePeriod(days = 28)), weekEnd), 4, MR.strings.dash_adv_per_week.text())
    }
    Spacer(Modifier.height(10.dp))
    Text(caption, style = FFType.metaData, color = FFColors.onSurfaceVariant)
    val spending = eligible.filter { it.type == "expense" && it.date in window }.groupBy { it.categoryId }.mapValues { (_, r) -> r.fold(0L) { s, t -> checkedAdd(s, t.amountMinor) } / divisor }
    if (spending.isEmpty()) Text(MR.strings.dash_adv_no_category_spending.text(), Modifier.padding(top = 10.dp), style = FFType.metaData, color = FFColors.onSurfaceVariant)
    val categories = data.categories.filter { it.type == "expense" }.sortedByDescending { spending[it.id] ?: 0L }
    (if (showAll) categories else categories.take(6)).forEach { category ->
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).clip(CircleShape).background((category.color?.let { Color(it) } ?: categoryColor(category.id)).copy(alpha = .6f)))
            Spacer(Modifier.width(10.dp))
            Text(category.name, Modifier.weight(1f), style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
            Text(shown(spending[category.id] ?: 0L), style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
        }
    }
    if (categories.size > 6) Text(if (showAll) MR.strings.dash_adv_show_less.text() else MR.strings.dash_adv_show_all_categories.text(categories.size),
        Modifier.padding(top = 14.dp).clickable { showAll = !showAll }, style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
    Box(Modifier.padding(vertical = 18.dp).fillMaxWidth().height(1.dp).background(divider))

    Text(MR.strings.dash_adv_monthly_cashflow.text(), style = FFType.bodyMain)
    Spacer(Modifier.height(10.dp))
    val small = FFType.metaData.copy(fontSize = 10.sp)
    Row(Modifier.fillMaxWidth()) {
        Spacer(Modifier.weight(1f))
        listOf(transactionTypeLabel("income"), transactionTypeLabel("expense"), MR.strings.dash_adv_net_flow.text()).forEach { Text(it, Modifier.weight(1.3f), style = small, color = FFColors.onSurfaceVariant, textAlign = TextAlign.End) }
    }
    val thisMonth = today.toString().take(7)
    (11 downTo 0).forEach { back ->
        val m = shiftMonth(thisMonth, -back)
        val rows = eligible.filter { it.date.startsWith(m) }
        val inc = rows.filter { it.type == "income" }.fold(0L) { s, t -> checkedAdd(s, t.amountMinor) }
        val exp = rows.filter { it.type == "expense" }.fold(0L) { s, t -> checkedAdd(s, t.amountMinor) }
        val net = checkedAdd(inc, -exp)
        fun c(v: Long) = if (preferences.hideBalances) preferences.maskCharacter.ifEmpty { "•" }.repeat(3) else compactDisplayMoney(v, currency)
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            Text(MR.strings.dash_month_short_year.text(shortMonthName(m.substring(5, 7).toInt()), m.substring(2, 4)), Modifier.weight(1f), style = small, color = FFColors.onSurfaceVariant)
            Text(c(inc), Modifier.weight(1.3f), style = small, color = FFColors.income, textAlign = TextAlign.End)
            Text(c(exp), Modifier.weight(1.3f), style = small, color = FFColors.expense, textAlign = TextAlign.End)
            Text((if (net > 0) "+" else "") + c(net), Modifier.weight(1.3f), style = small, color = if (net > 0) FFColors.income else if (net < 0) FFColors.expense else FFColors.onSurface, textAlign = TextAlign.End)
        }
    }
    Spacer(Modifier.height(10.dp))
    Text(MR.strings.dash_adv_cashflow_note.text(), style = FFType.metaData.copy(fontSize = 10.5.sp), color = FFColors.onSurfaceVariant)
    Box(Modifier.padding(vertical = 18.dp).fillMaxWidth().height(1.dp).background(divider))
    FinancialCalculator(currency, preferences)
}

@Composable
private fun FinancialCalculator(currency: String, preferences: AppPreferences) {
    var open by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf("goal") }
    var target by remember { mutableStateOf("") }
    var monthly by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var tenor by remember { mutableStateOf("") }
    Row(Modifier.fillMaxWidth().clickable { open = !open }.testTag("statistics-calculator"), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(MR.strings.dash_calc_title.text(), style = FFType.bodyMain)
            Text(MR.strings.dash_calc_subtitle.text(), style = FFType.metaData, color = FFColors.onSurfaceVariant)
        }
        FFIcon(if (open) "hi_chevron_up" else "hi_chevron_down", FFColors.onSurfaceVariant, 20.dp)
    }
    if (!open) return
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("goal" to MR.strings.dash_calc_goal.text(), "loan" to MR.strings.dash_calc_loan.text()).forEach { (value, label) ->
            val chosen = mode == value
            Box(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(19.dp)).background(if (chosen) FFColors.onSurface else FFColors.background).clickable { mode = value },
                contentAlignment = Alignment.Center) { Text(label, style = FFType.metaData, color = if (chosen) FFColors.background else FFColors.onSurface) }
        }
    }
    fun amount(text: String) = text.filter { it.isDigit() }.toLongOrNull()?.let { it * 100 }
    Spacer(Modifier.height(8.dp))
    if (mode == "goal") {
        Field(MR.strings.dash_calc_target_amount.text(currency), target, { target = it.filter(Char::isDigit) })
        Field(MR.strings.dash_calc_monthly_saving.text(currency), monthly, { monthly = it.filter(Char::isDigit) })
        val t = amount(target); val m = amount(monthly)
        if (t != null && m != null && m > 0) {
            val months = ((t + m - 1) / m).toInt()
            Text(MR.strings.dash_calc_months_result.text(months, months / 12, months % 12), Modifier.padding(top = 10.dp), style = FFType.bodyMain, color = FFColors.income)
        }
    } else {
        Field(MR.strings.dash_calc_loan_amount.text(currency), target, { target = it.filter(Char::isDigit) })
        Field(MR.strings.dash_calc_interest.text(), rate, { rate = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.replace(',', '.') })
        Field(MR.strings.dash_calc_tenor.text(), tenor, { tenor = it.filter(Char::isDigit) })
        val p = amount(target); val n = tenor.toIntOrNull(); val r = (rate.toDoubleOrNull() ?: -1.0) / 100 / 12
        if (p != null && n != null && n > 0 && r >= 0) {
            val payment = if (r == 0.0) p.toDouble() / n else p * r / (1 - (1 + r).pow(-n.toDouble()))
            Text(MR.strings.dash_calc_installment_result.text(formatBalance(payment.roundToLong(), currency, preferences)), Modifier.padding(top = 10.dp), style = FFType.bodyMain, color = FFColors.expense)
        }
    }
}
