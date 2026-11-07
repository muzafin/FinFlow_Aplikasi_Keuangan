package com.logikawan.finflow.core.util

import com.logikawan.finflow.core.i18n.text
import com.logikawan.finflow.i18n.*

fun currencyDigits(code: String): Int = when (code.uppercase()) {
    "JPY", "KRW", "VND", "CLP", "ISK", "PYG", "RWF", "UGX", "VUV", "XAF", "XOF", "XPF", "BIF", "DJF", "GNF", "KMF" -> 0
    "BHD", "IQD", "JOD", "KWD", "LYD", "OMR", "TND" -> 3
    "CLF", "UYW" -> 4
    else -> 2
}

fun checkedAdd(a: Long, b: Long): Long {
    require(!(b > 0 && a > Long.MAX_VALUE - b || b < 0 && a < Long.MIN_VALUE - b)) { MR.strings.dash_err_amount_too_large.text() }
    return a + b
}

fun minorAmount(text: String, currency: String, allowNegative: Boolean = false): Long {
    val value = text.trim()
    require(Regex(if (allowNegative) "-?[0-9]+(\\.[0-9]+)?" else "[0-9]+(\\.[0-9]+)?").matches(value)) {
        MR.strings.dash_err_number_format.text()
    }
    val digits = currencyDigits(currency)
    val parts = value.removePrefix("-").split('.')
    val fraction = parts.getOrElse(1) { "" }
    require(fraction.drop(digits).all { it == '0' }) { MR.strings.dash_err_max_decimals.text(currency, digits) }
    val raw = (parts[0] + fraction.take(digits).padEnd(digits, '0')).toLongOrNull()
        ?: error(MR.strings.dash_err_amount_too_large.text())
    return if (value.startsWith('-')) -raw else raw
}

fun decimalAmount(minor: Long, currency: String): String {
    require(minor != Long.MIN_VALUE) { MR.strings.dash_err_amount_out_of_range.text() }
    val digits = currencyDigits(currency)
    val raw = minor.toString().removePrefix("-").padStart(digits + 1, '0')
    val number = if (digits == 0) raw else raw.dropLast(digits) + "." + raw.takeLast(digits)
    return (if (minor < 0) "-" else "") + number
}

fun money(minor: Long, currency: String): String {
    val raw = decimalAmount(minor, currency).split('.')
    val whole = raw[0].removePrefix("-").reversed().chunked(3).joinToString(".").reversed()
    return "$currency ${if (minor < 0) "-" else ""}$whole${if (raw.size > 1) ",${raw[1]}" else ""}"
}
