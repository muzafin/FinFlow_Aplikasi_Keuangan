package com.clonical.kasiva.core.util

import kotlinx.datetime.*
import com.clonical.kasiva.core.model.Budget

data class FinancePeriod(val start: LocalDate, val endExclusive: LocalDate) {
    init { require(endExclusive > start) }
    operator fun contains(date: String) = date >= start.toString() && date < endExclusive.toString()
}

private fun anchoredDay(month: LocalDate, day: Int): LocalDate {
    require(day in 1..31)
    val first = LocalDate(month.year,month.monthNumber,1)
    val days = first.plus(DatePeriod(months=1)).toEpochDays() - first.toEpochDays()
    return LocalDate(first.year,first.monthNumber,minOf(day,days))
}

fun recordingPeriod(month: String, day: Int = 1): FinancePeriod {
    val first = LocalDate.parse("$month-01")
    return FinancePeriod(anchoredDay(first,day),anchoredDay(first.plus(DatePeriod(months=1)),day))
}

fun activeRecordingMonth(date: LocalDate, day: Int): String =
    (if(date < anchoredDay(date,day)) date.plus(DatePeriod(months=-1)) else date).toString().take(7)

fun budgetPeriod(budget: Budget, recordingDay: Int = 1): FinancePeriod = if(budget.period=="weekly") {
    val start=LocalDate.parse(budget.month);FinancePeriod(start,start.plus(DatePeriod(days=7)))
} else recordingPeriod(budget.month,recordingDay)

fun financePeriod(kind: String, anchor: LocalDate, month: String = anchor.toString().take(7), recordingDay: Int = 1, weekStart: Int = 1): FinancePeriod {
    require(weekStart in 1..7)
    return when(kind) {
        "day", "Hari Ini" -> FinancePeriod(anchor,anchor.plus(DatePeriod(days=1)))
        "week", "Minggu Ini" -> {
            val start=anchor.plus(DatePeriod(days=-((anchor.dayOfWeek.isoDayNumber-weekStart+7)%7)))
            FinancePeriod(start,start.plus(DatePeriod(days=7)))
        }
        "year", "Tahun" -> FinancePeriod(LocalDate(anchor.year,1,1),LocalDate(anchor.year+1,1,1))
        else -> recordingPeriod(month,recordingDay)
    }
}
