package com.logikawan.finflow.core.util

import kotlin.math.roundToLong
import com.logikawan.finflow.core.model.AppPreferences
import kotlinx.datetime.LocalDate

fun formatBalance(value: Long, currency: String, preferences: AppPreferences): String {
    val parts=decimalAmount(value,currency).split('.')
    val grouping=if(preferences.numberFormat=="id") "." else ","
    val decimal=if(preferences.numberFormat=="id") "," else "."
    val whole=parts[0].removePrefix("-").reversed().chunked(3).joinToString(grouping).reversed()
    val fraction=parts.getOrNull(1)?.takeIf{preferences.showDecimals || it.any{c->c!='0'}}?.let{decimal+it}.orEmpty()
    val prefix=if(preferences.showCurrencySymbol && currency=="IDR") "Rp" else "$currency "
    return prefix+(if(value<0)"-" else "")+whole+fraction
}

fun displayMoney(value: Long, currency: String): String {
    val formatted = money(value, currency).removePrefix("$currency ")
    val decimals = currencyDigits(currency)
    val compact = if (decimals > 0 && formatted.endsWith("," + "0".repeat(decimals))) formatted.dropLast(decimals + 1) else formatted
    return (if (currency == "IDR") "Rp" else "$currency ") + compact
}

fun displayPreciseMoney(value: Long, currency: String): String =
    (if (currency == "IDR") "Rp" else "$currency ") + money(value, currency).removePrefix("$currency ")

private val monthNames = listOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
fun shortDateLabel(value:String):String = runCatching {
    val date=LocalDate.parse(value)
    "${date.dayOfMonth} ${monthNames[date.monthNumber-1].take(3)} ${date.year}"
}.getOrDefault(value)
fun monthLabel(month: String): String = month.substring(5, 7).toIntOrNull()?.takeIf { it in 1..12 }?.let { "${monthNames[it - 1]} ${month.take(4)}" } ?: month
fun shiftMonth(month: String, delta: Int): String {
    val index = month.take(4).toInt() * 12 + month.substring(5, 7).toInt() - 1 + delta
    return "${index / 12}-${(index % 12 + 1).toString().padStart(2, '0')}"
}

fun compactDisplayMoney(value: Long, currency: String): String {
    if (currency != "IDR") return displayMoney(value, currency)
    val amount = value / 100.0
    val divisor = when {
        kotlin.math.abs(amount) >= 1_000_000_000 -> 1_000_000_000.0 to "M"
        kotlin.math.abs(amount) >= 1_000_000 -> 1_000_000.0 to "jt"
        kotlin.math.abs(amount) >= 1_000 -> 1_000.0 to "rb"
        else -> return "Rp${amount.roundToLong()}"
    }
    val tenths = (amount / divisor.first * 10).roundToLong()
    val sign = if (tenths < 0) "-" else ""
    val magnitude = kotlin.math.abs(tenths)
    return "Rp$sign${magnitude / 10}.${magnitude % 10}${divisor.second}"
}
