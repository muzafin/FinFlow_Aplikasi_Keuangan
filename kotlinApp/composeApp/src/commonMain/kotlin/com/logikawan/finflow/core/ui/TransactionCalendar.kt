package com.logikawan.finflow.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logikawan.finflow.core.model.*
import com.logikawan.finflow.core.util.*
import kotlinx.datetime.*
import com.logikawan.finflow.i18n.*
import com.logikawan.finflow.core.i18n.text

fun calendarDates(month: String, weekStart: Int): List<LocalDate> {
    require(weekStart in 1..7)
    val first = LocalDate.parse("$month-01")
    val start = first.minus(DatePeriod(days = (first.dayOfWeek.isoDayNumber - weekStart + 7) % 7))
    return List(42) { start.plus(DatePeriod(days = it)) }
}

private val calendarDayShort = listOf(MR.strings.app_day_short_mon, MR.strings.app_day_short_tue, MR.strings.app_day_short_wed, MR.strings.app_day_short_thu, MR.strings.app_day_short_fri, MR.strings.app_day_short_sat, MR.strings.app_day_short_sun)
private val calendarDayLong = listOf(MR.strings.app_day_long_mon, MR.strings.app_day_long_tue, MR.strings.app_day_long_wed, MR.strings.app_day_long_thu, MR.strings.app_day_long_fri, MR.strings.app_day_long_sat, MR.strings.app_day_long_sun)
private val calendarMonthShort = listOf(MR.strings.app_month_short_jan, MR.strings.app_month_short_feb, MR.strings.app_month_short_mar, MR.strings.app_month_short_apr, MR.strings.app_month_short_may, MR.strings.app_month_short_jun, MR.strings.app_month_short_jul, MR.strings.app_month_short_aug, MR.strings.app_month_short_sep, MR.strings.app_month_short_oct, MR.strings.app_month_short_nov, MR.strings.app_month_short_dec)

// Kalender gaya Ollo: kartu abu terang, tile putih per tanggal, tanggal terpilih bergaris hitam tebal,
// nominal ringkas di dalam sel, dan privasi nominal mengikuti preferensi.
@Composable
fun TransactionCalendar(month: String, monthChange: (String) -> Unit, selected: String, select: (String) -> Unit,
    transactions: List<Transaction>, privacy: Boolean = LocalPreferences.current.hideBalances) {
    val preferences = LocalPreferences.current
    val days = calendarDates(month, preferences.weekStart)
    val selectedDate = runCatching { LocalDate.parse(selected) }.getOrNull()
    fun moveMonth(delta:Int) {
        val target=shiftMonth(month,delta)
        val first=LocalDate.parse("$target-01")
        val count=first.plus(DatePeriod(months=1)).toEpochDays()-first.toEpochDays()
        select(LocalDate(first.year,first.monthNumber,minOf(LocalDate.parse(selected).dayOfMonth,count)).toString())
        monthChange(target)
    }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(FFColors.surface).padding(12.dp).testTag("transaction-calendar"),
        verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(buildAnnotatedString {
                if (selectedDate != null) {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append(MR.strings.app_calendar_selected_date.text(selectedDate.dayOfMonth, calendarMonthShort[selectedDate.monthNumber - 1].text(), (selectedDate.year % 100).toString().padStart(2, '0')))
                    }
                    append("  ${calendarDayLong[selectedDate.dayOfWeek.isoDayNumber - 1].text()}")
                } else append(shortDateLabel(selected))
            }, Modifier.weight(1f), style = FFType.bodyMain.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp))
            Box(Modifier.clip(CircleShape).clickable { moveMonth(-1) }.padding(6.dp)) {
                FFIcon("chevron_left_rounded", FFColors.onSurfaceVariant, 20.dp, MR.strings.app_previous_month.text())
            }
            Box(Modifier.clip(CircleShape).clickable { moveMonth(1) }.padding(6.dp)) {
                FFIcon("chevron_right_rounded", FFColors.onSurfaceVariant, 20.dp, MR.strings.app_next_month.text())
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            days.take(7).forEach { day ->
                Text(calendarDayShort[day.dayOfWeek.isoDayNumber - 1].text(), Modifier.weight(1f), style = FFType.metaData,
                    color = FFColors.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        days.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                week.forEach { day ->
                    val date = day.toString()
                    val inMonth = date.take(7) == month
                    val isSelected = date == selected
                    val entries = transactions.filter { it.date == date && !it.scheduled }
                    val shape = RoundedCornerShape(10.dp)
                    Column(Modifier.weight(1f).height(60.dp).clip(shape)
                        .background(if (isSelected) FFColors.surfaceContainerHigh else if (inMonth) FFColors.canvas else Color.Transparent)
                        .then(if (isSelected) Modifier.border(2.dp, FFColors.onSurface, shape) else Modifier)
                        .clickable { select(date); if (!inMonth) monthChange(date.take(7)) }
                        .semantics { contentDescription = MR.strings.app_calendar_day_desc.text(date, entries.size); this.selected = isSelected }
                        .padding(horizontal = 3.dp, vertical = 5.dp), horizontalAlignment = Alignment.Start) {
                        Text(day.dayOfMonth.toString(), style = FFType.bodyMain.copy(fontSize = 12.sp, lineHeight = 16.sp),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (inMonth) FFColors.onSurface else FFColors.outline)
                        Spacer(Modifier.weight(1f))
                        entries.groupBy { it.currency }.forEach { (currency, records) ->
                            val net = records.fold(0L) { sum, t -> checkedAdd(sum, when (t.type) { "income" -> t.amountMinor; "expense" -> -t.amountMinor; else -> 0 }) }
                            if (privacy) Text(LocalPreferences.current.maskCharacter.ifEmpty { "•" }.repeat(3), style = FFType.metaData.copy(fontSize = 9.sp, lineHeight = 11.sp), color = FFColors.onSurfaceVariant, maxLines = 1)
                            else FitMoneyText((if (net > 0) "+" else "") + compactDisplayMoney(net, currency),
                                FFType.metaData.copy(fontSize = 9.sp, lineHeight = 11.sp),
                                if (net >= 0) FFColors.income else FFColors.expense)
                        }
                    }
                }
            }
        }
    }
}
