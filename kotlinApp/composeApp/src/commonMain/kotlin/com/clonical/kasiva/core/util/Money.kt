package com.clonical.kasiva.core.util

import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.i18n.*

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

/**
 * Ubah teks angka gaya pengguna ("25.000", "1.500.000", "12.500,50", "1,234.5") menjadi digit dengan "." sebagai
 * desimal saja ("25000", "1500000", "12500.50"). Aturan pemisah:
 *  - ada "." dan "," → yang terakhir adalah desimal, yang lain ribuan;
 *  - salah satu muncul lebih dari sekali → ribuan;
 *  - muncul sekali diikuti tepat 3 digit → ribuan (kecuali mata uang 3 desimal), selain itu desimal.
 * Tanda "-" di depan atau kurung "(25.000)" dibaca sebagai negatif. Hasil null bila tidak ada digit.
 */
fun normalizeAmountText(raw: String, decimalDigits: Int = 2): String? {
    var value = raw.trim()
    var negative = false
    if (value.startsWith("(") && value.endsWith(")")) { negative = true; value = value.substring(1, value.length - 1).trim() }
    if (value.startsWith("-")) { negative = true; value = value.removePrefix("-") } else if (value.startsWith("+")) value = value.removePrefix("+")
    value = value.filter { it in '0'..'9' || it == '.' || it == ',' }
    if (value.none { it in '0'..'9' }) return null
    fun groupingOnly(sep: Char): Boolean {
        val after = value.length - value.indexOf(sep) - 1
        return after == 3 && decimalDigits != 3 && value.indexOf(sep) > 0
    }
    value = when {
        '.' in value && ',' in value -> if (value.lastIndexOf(',') > value.lastIndexOf('.')) value.replace(".", "").replace(',', '.') else value.replace(",", "")
        value.count { it == ',' } > 1 -> value.replace(",", "")
        value.count { it == '.' } > 1 -> value.replace(".", "")
        value.count { it == ',' } == 1 -> if (groupingOnly(',')) value.replace(",", "") else value.replace(',', '.')
        value.count { it == '.' } == 1 -> if (groupingOnly('.')) value.replace(".", "") else value
        else -> value
    }
    if (value.startsWith(".")) value = "0$value"
    value = value.trimEnd('.')
    return (if (negative) "-" else "") + value
}

/** [minorAmount] untuk teks yang diketik/ditempel pengguna: memahami pemisah ribuan "." dan desimal ",". */
fun parseUserAmount(text: String, currency: String, allowNegative: Boolean = false): Long =
    minorAmount(normalizeAmountText(text, currencyDigits(currency)) ?: text, currency, allowNegative)
