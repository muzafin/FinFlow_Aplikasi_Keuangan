package com.logikawan.finflow.feature.budget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
import kotlin.math.roundToInt
import kotlinx.datetime.*

/*
 * Halaman Anggaran bergaya Ollo: kartu ringkasan bulan (sisa bisa dibelanjakan, cincin
 * persentase terpakai, total terpakai/limit), lalu kartu progres per kategori, empty state
 * dengan tombol buat, dan FAB tambah. Nominal memakai FitMoneyText agar tidak terpotong.
 */

private val budgetWarn = Color(0xFFF59E0B)

@Composable
fun BudgetScreen(data: FinanceData, month: String, monthChange: (String) -> Unit, edit: (Budget) -> Unit, add: () -> Unit) {
    val preferences = LocalPreferences.current
    var period by rememberSaveable { mutableStateOf("monthly") }
    var weekAnchor by rememberSaveable { mutableStateOf(today().toString()) }
    val range = if (period == "weekly") financePeriod("week", LocalDate.parse(weekAnchor), weekStart = preferences.weekStart)
        else recordingPeriod(month, preferences.recordingDay)
    val budgets = data.budgets.filter {
        it.period == period && (if (period == "weekly") weekAnchor in budgetPeriod(it, preferences.recordingDay) else it.month == month)
    }
    val reportExpenses = data.reportTransactions().filter { it.type == "expense" }
    val currencies = budgets.map { it.currency }.distinct().ifEmpty { listOf(data.profile?.baseCurrency ?: "IDR") }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().testTag("budget-page"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                BudgetPeriodTabs(period) { period = it }
            }
            currencies.forEach { currency ->
                val entries = budgets.filter { it.currency == currency }
                val limit = entries.fold(0L) { sum, b -> checkedAdd(sum, b.limitMinor) }
                val spent = reportExpenses.filter { tx ->
                    tx.currency == currency && entries.any { it.categoryId == tx.categoryId && tx.date in budgetPeriod(it, preferences.recordingDay) }
                }.fold(0L) { sum, tx -> checkedAdd(sum, tx.amountMinor) }
                item {
                    BudgetSummaryCard(
                        month = month, period = period, range = range, limit = limit, spent = spent, currency = currency,
                        onPrev = {
                            if (period == "weekly") weekAnchor = LocalDate.parse(weekAnchor).minus(DatePeriod(days = 7)).toString()
                            else monthChange(shiftMonth(month, -1))
                        },
                        onNext = {
                            if (period == "weekly") weekAnchor = LocalDate.parse(weekAnchor).plus(DatePeriod(days = 7)).toString()
                            else monthChange(shiftMonth(month, 1))
                        },
                    )
                }
                if (entries.isEmpty()) {
                    item { BudgetEmptyState(add) }
                } else {
                    entries.forEach { budget -> item(key = budget.id) {
                        val budgetRange = budgetPeriod(budget, preferences.recordingDay)
                        val used = reportExpenses.filter { it.date in budgetRange && it.currency == currency && it.categoryId == budget.categoryId }
                            .fold(0L) { sum, tx -> checkedAdd(sum, tx.amountMinor) }
                        BudgetProgressCard(budget, data, used, preferences) { edit(budget) }
                    } }
                }
            }
        }
        Box(
            Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 20.dp).size(56.dp)
                .shadow(8.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp)).background(FFColors.primary)
                .clickable(onClick = add).testTag("budget-fab"),
            contentAlignment = Alignment.Center,
        ) { FFIcon("add_rounded", Color.White, 26.dp, "Buat anggaran") }
    }
}

@Composable
private fun BudgetPeriodTabs(period: String, select: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(16.dp)).background(FFColors.surface).padding(4.dp),
    ) {
        listOf("monthly" to "Bulanan", "weekly" to "Mingguan").forEach { (value, label) ->
            val selected = period == value
            Box(
                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp))
                    .background(if (selected) FFColors.canvas else Color.Transparent)
                    .clickable { select(value) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = FFType.bodyMain, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) FFColors.onSurface else FFColors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun BudgetSummaryCard(month: String, period: String, range: FinancePeriod, limit: Long, spent: Long, currency: String,
                              onPrev: () -> Unit, onNext: () -> Unit) {
    val preferences = LocalPreferences.current
    val ratio = if (limit > 0) (spent.toDouble() / limit).toFloat() else if (spent > 0) 1f else 0f
    val remaining = if (limit == 0L) 0L else checkedAdd(limit, -spent)
    val statusColor = when { limit == 0L -> FFColors.income; spent > limit -> FFColors.expense; ratio >= .8f -> budgetWarn; else -> FFColors.income }
    val statusLabel = when { limit == 0L -> "Normal"; spent > limit -> "Terlampaui"; ratio >= .8f -> "Waspada"; else -> "Normal" }
    val endDate = range.endExclusive.minus(DatePeriod(days = 1))
    val title = if (period == "weekly") {
        "${shortDateLabel(range.start.toString())} – ${shortDateLabel(endDate.toString())}"
    } else monthLabel(month)
    val periodLabel = "(${range.start.dayOfMonth} ${monthAbbreviation(range.start)} - ${endDate.dayOfMonth} ${monthAbbreviation(endDate)})"
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(FFColors.surface).padding(20.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(CircleShape).clickable(onClick = onPrev).testTag("budget-prev"), contentAlignment = Alignment.Center) {
                FFIcon("chevron_left_rounded", FFColors.outline, 22.dp, "Periode sebelumnya")
            }
            Text(title, Modifier.weight(1f), style = FFType.sectionTitle, color = FFColors.onSurface, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Box(Modifier.size(34.dp).clip(CircleShape).clickable(onClick = onNext).testTag("budget-next"), contentAlignment = Alignment.Center) {
                FFIcon("chevron_right_rounded", FFColors.outline, 22.dp, "Periode berikutnya")
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Anda bisa belanja", style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
                FitMoneyText(if (preferences.hideBalances) "••••••" else formatBalance(remaining, currency, preferences), FFType.amountLg)
                Text(periodLabel, style = FFType.metaData, color = FFColors.outline)
                Spacer(Modifier.height(4.dp))
                Text(if (period == "weekly") "Sisa minggu ini" else "Sisa bulan ini", style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
            }
            Row(
                Modifier.clip(RoundedCornerShape(100.dp)).background(statusColor.copy(alpha = .12f)).padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FFIcon("check_rounded", statusColor, 15.dp)
                Spacer(Modifier.width(5.dp))
                Text(statusLabel, style = FFType.bodyMain, fontWeight = FontWeight.SemiBold, color = statusColor, maxLines = 1)
            }
        }
        Spacer(Modifier.height(14.dp))
        val trackColor = FFColors.outlineVariant
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.width(120.dp).height(76.dp)) {
                Canvas(Modifier.width(120.dp).height(60.dp).align(Alignment.TopCenter)) {
                    val strokeWidth = 9.dp.toPx()
                    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    val inset = strokeWidth / 2f
                    val arcSize = Size(size.width - strokeWidth, size.width - strokeWidth)
                    drawArc(trackColor, 180f, 180f, false, topLeft = Offset(inset, inset), size = arcSize, style = stroke)
                    if (ratio > 0f) drawArc(statusColor, 180f, 180f * ratio.coerceIn(0f, 1f), false, topLeft = Offset(inset, inset), size = arcSize, style = stroke)
                }
                Column(Modifier.align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally) {
                    FitMoneyText("${(ratio * 100).roundToInt()}%", FFType.amountLg, FFColors.onSurface, TextAlign.Center)
                }
            }
            Text("Terpakai", style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    FitMoneyText(if (preferences.hideBalances) "••••••" else formatBalance(spent, currency, preferences), FFType.amountMd, FFColors.onSurface, TextAlign.End)
                }
                Text(" / ", style = FFType.amountMd, color = FFColors.outline)
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    FitMoneyText(if (preferences.hideBalances) "••••••" else formatBalance(limit, currency, preferences), FFType.amountMd, FFColors.outline)
                }
            }
        }
    }
}

@Composable
private fun BudgetProgressCard(budget: Budget, data: FinanceData, used: Long, preferences: AppPreferences, onClick: () -> Unit) {
    val color = categoryColor(budget.categoryId)
    val ratio = if (budget.limitMinor > 0) (used.toDouble() / budget.limitMinor).toFloat() else 0f
    val remaining = checkedAdd(budget.limitMinor, -used)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(FFColors.surface).clickable(onClick = onClick).padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                FFIcon(categoryIcon(budget.categoryId), color, 22.dp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    budget.title.ifBlank { data.categoryName(budget.categoryId) },
                    style = FFType.bodyMain.copy(fontSize = 16.sp, lineHeight = 22.sp), fontWeight = FontWeight.SemiBold,
                    color = FFColors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    data.categoryName(budget.categoryId),
                    style = FFType.metaData, color = FFColors.outline, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.width(112.dp), horizontalAlignment = Alignment.End) {
                FitMoneyText(if (preferences.hideBalances) "••••••" else formatBalance(used, budget.currency, preferences), FFType.amountMd, FFColors.onSurface, TextAlign.End)
                Text("${(ratio * 100).roundToInt()}%", style = FFType.metaData, color = if (used > budget.limitMinor) FFColors.expense else FFColors.outline)
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(color.copy(alpha = .14f))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(ratio.coerceIn(0f, 1f)).clip(RoundedCornerShape(4.dp)).background(color))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Tersisa", style = FFType.metaData, color = FFColors.outline)
            Spacer(Modifier.width(6.dp))
            Box(Modifier.weight(1f)) {
                FitMoneyText(if (preferences.hideBalances) "••••••" else formatBalance(remaining, budget.currency, preferences),
                    FFType.metaData, if (remaining < 0) FFColors.expense else FFColors.outline)
            }
            Spacer(Modifier.width(12.dp))
            Box(Modifier.width(110.dp), contentAlignment = Alignment.CenterEnd) {
                FitMoneyText(if (preferences.hideBalances) "••••••" else formatBalance(budget.limitMinor, budget.currency, preferences),
                    FFType.metaData, FFColors.outline, TextAlign.End)
            }
        }
    }
}

@Composable
private fun BudgetEmptyState(add: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        FFIcon("pie_chart_outline_rounded", FFColors.outlineVariant, 56.dp)
        Spacer(Modifier.height(14.dp))
        Text("Belum ada anggaran", style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = add) { Text("Buat Anggaran", style = FFType.bodyMain, fontWeight = FontWeight.SemiBold, color = FFColors.onSurface) }
    }
}

private fun monthAbbreviation(date: LocalDate): String = when (date.monthNumber) {
    1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"; 5 -> "Mei"; 6 -> "Jun"
    7 -> "Jul"; 8 -> "Agu"; 9 -> "Sep"; 10 -> "Okt"; 11 -> "Nov"; else -> "Des"
}