package com.clonical.kasiva.feature.planning

import com.clonical.kasiva.core.model.RecurringTransaction
import kotlinx.datetime.*

fun recurringOn(rule: RecurringTransaction, date: LocalDate): Boolean {
    if (!rule.active || date.toString() < rule.nextDate || rule.endDate?.let { date.toString() > it } == true) return false
    if (date.toString() == rule.nextDate) return true
    val next = LocalDate.parse(rule.nextDate)
    val days = date.toEpochDays() - next.toEpochDays()
    val months = (date.year - next.year) * 12 + date.monthNumber - next.monthNumber
    val first = LocalDate(date.year, date.monthNumber, 1)
    val monthDays = first.plus(DatePeriod(months = 1)).toEpochDays() - first.toEpochDays()
    return when (rule.frequency) {
        "daily" -> days % rule.interval == 0
        "weekly" -> days % (7 * rule.interval) == 0
        "monthly" -> months > 0 && months % rule.interval == 0 && date.dayOfMonth == minOf(rule.anchorDay, monthDays)
        "yearly" -> months > 0 && months % (12 * rule.interval) == 0 && date.dayOfMonth == minOf(rule.anchorDay, monthDays)
        else -> false
    }
}
