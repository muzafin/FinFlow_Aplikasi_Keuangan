package com.clonical.kasiva.core.util

import kotlin.math.roundToLong
import com.clonical.kasiva.core.model.AppPreferences
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.i18n.*
import dev.icerock.moko.resources.StringResource
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

// Nama bulan/hari disimpan sebagai resource dan baru diterjemahkan saat dipakai agar mengikuti bahasa aktif.
private val monthNames: List<StringResource> = listOf(MR.strings.dash_month_1, MR.strings.dash_month_2, MR.strings.dash_month_3, MR.strings.dash_month_4,
    MR.strings.dash_month_5, MR.strings.dash_month_6, MR.strings.dash_month_7, MR.strings.dash_month_8, MR.strings.dash_month_9,
    MR.strings.dash_month_10, MR.strings.dash_month_11, MR.strings.dash_month_12)
private val shortMonthNames: List<StringResource> = listOf(MR.strings.dash_month_short_1, MR.strings.dash_month_short_2, MR.strings.dash_month_short_3,
    MR.strings.dash_month_short_4, MR.strings.dash_month_short_5, MR.strings.dash_month_short_6, MR.strings.dash_month_short_7, MR.strings.dash_month_short_8,
    MR.strings.dash_month_short_9, MR.strings.dash_month_short_10, MR.strings.dash_month_short_11, MR.strings.dash_month_short_12)
private val dayNames: List<StringResource> = listOf(MR.strings.dash_day_1, MR.strings.dash_day_2, MR.strings.dash_day_3, MR.strings.dash_day_4,
    MR.strings.dash_day_5, MR.strings.dash_day_6, MR.strings.dash_day_7)
private val dayInitials: List<StringResource> = listOf(MR.strings.dash_day_initial_1, MR.strings.dash_day_initial_2, MR.strings.dash_day_initial_3,
    MR.strings.dash_day_initial_4, MR.strings.dash_day_initial_5, MR.strings.dash_day_initial_6, MR.strings.dash_day_initial_7)

/** Nama bulan lengkap terlokalisasi, `number` 1..12. */
fun monthName(number: Int): String = monthNames[number - 1].text()
/** Nama bulan singkat terlokalisasi (Jan, Feb, ... Agu, Okt, Des), `number` 1..12. */
fun shortMonthName(number: Int): String = shortMonthNames[number - 1].text()
/** Nama hari terlokalisasi, `isoDay` 1 = Senin .. 7 = Minggu. */
fun dayName(isoDay: Int): String = dayNames[isoDay - 1].text()
/** Inisial hari terlokalisasi (S, S, R, K, J, S, M), `isoDay` 1 = Senin .. 7 = Minggu. */
fun dayInitial(isoDay: Int): String = dayInitials[isoDay - 1].text()

fun shortDateLabel(value:String):String = runCatching {
    val date=LocalDate.parse(value)
    MR.strings.dash_short_date.text(date.dayOfMonth,shortMonthName(date.monthNumber),date.year)
}.getOrDefault(value)
fun monthLabel(month: String): String = month.substring(5, 7).toIntOrNull()?.takeIf { it in 1..12 }?.let { MR.strings.dash_month_year.text(monthName(it), month.take(4)) } ?: month
fun shiftMonth(month: String, delta: Int): String {
    val index = month.take(4).toInt() * 12 + month.substring(5, 7).toInt() - 1 + delta
    return "${index / 12}-${(index % 12 + 1).toString().padStart(2, '0')}"
}

fun compactDisplayMoney(value: Long, currency: String): String {
    if (currency != "IDR") return displayMoney(value, currency)
    val amount = value / 100.0
    val units = listOf(1_000.0 to MR.strings.dash_compact_thousand.text(), 1_000_000.0 to MR.strings.dash_compact_million.text(),
        1_000_000_000.0 to MR.strings.dash_compact_billion.text())
    var index = units.indexOfLast { kotlin.math.abs(amount) >= it.first }
    if (index < 0) {
        val whole = amount.roundToLong()
        if (kotlin.math.abs(whole) < 1_000) return "Rp$whole"
        index = 0
    }
    // Pembulatan bisa menaikkan ke satuan berikutnya: 999.950 → "1.0jt", bukan "1000.0rb" (P1-05).
    if (index < units.lastIndex && kotlin.math.abs((amount / units[index].first * 10).roundToLong()) >= 10_000) index++
    val divisor = units[index]
    val tenths = (amount / divisor.first * 10).roundToLong()
    val sign = if (tenths < 0) "-" else ""
    val magnitude = kotlin.math.abs(tenths)
    return "Rp$sign${magnitude / 10}.${magnitude % 10}${divisor.second}"
}
