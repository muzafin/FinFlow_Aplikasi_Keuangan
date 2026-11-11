package com.clonical.kasiva.feature.importdata

import com.clonical.kasiva.core.data.FinanceRepository
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.model.FinanceData
import com.clonical.kasiva.core.model.ImportPlan
import com.clonical.kasiva.core.model.Transaction
import com.clonical.kasiva.core.util.minorAmount
import com.clonical.kasiva.feature.ai.AiCsvMapping
import com.clonical.kasiva.feature.ai.AiCsvTypeMapping
import com.clonical.kasiva.i18n.*
import kotlinx.datetime.LocalDate

// Format CSV tak dikenal: AI hanya memilih index kolom; konversi seluruh file tetap di perangkat.

/** Konversi murni tanpa jaringan agar bisa diuji dan dipakai ulang untuk seluruh file. */
internal fun buildAiPlan(rows: List<List<String>>, mapping: AiCsvMapping, existing: FinanceData, rowNumbers: List<Int> = emptyList()): ImportPlan {
    if (mapping.allIndexesNull()) {
        return ImportPlan(emptyList(), emptyList(), emptyList(), listOf(MR.strings.ai_csv_unrecognized.text()), emptyList(), 0)
    }
    val planner = CsvImport.Planner("ai", existing)
    var warnedTransfer = false
    fun cell(row: List<String>, index: Int?): String = if (index == null) "" else row.getOrNull(index)?.trim().orEmpty()
    rows.forEachIndexed { offset, row ->
        // Baris kosong atau catatan kaki ("File ini dibuat oleh ...") tanpa tanggal dan nominal bukan transaksi.
        if (cell(row, mapping.date).isBlank() && cell(row, mapping.amount).isBlank()) return@forEachIndexed
        val lineNumber = rowNumbers.getOrNull(offset) ?: (offset + 2)
        try {
            val currency = cell(row, mapping.currency).uppercase().takeIf { it.matches(Regex("[A-Z]{3}")) } ?: planner.baseCurrency
            val (negative, amount) = aiAmount(cell(row, mapping.amount), mapping.amountFormat, currency)
            val date = aiDate(cell(row, mapping.date), mapping.dateFormat)
            val categoryName = cell(row, mapping.category).ifBlank { "Lainnya" }
            var type = aiType(cell(row, mapping.type), mapping.typeMapping) ?: when {
                mapping.typeFromSign -> if (negative) "expense" else "income"
                else -> "expense"
            }
            val fromName = cell(row, mapping.account).ifBlank { existing.accounts.firstOrNull { !it.archived }?.name ?: "Kas" }
            var toName = ""
            var toCurrency = ""
            var received: Long? = null
            if (type == "transfer") {
                toName = cell(row, mapping.toAccount)
                if (toName.isBlank()) {
                    // Format tanpa kolom dompet tujuan: catat sebagai pengeluaran, jangan gagalkan file.
                    type = "expense"
                    if (!warnedTransfer) { warnedTransfer = true; planner.warnings.add(MR.strings.ai_csv_warn_transfer_no_target.text()) }
                } else {
                    toCurrency = existing.accounts.firstOrNull { it.name == toName && !it.archived }?.currency ?: currency
                    val receivedText = cell(row, mapping.transferAmount)
                    received = if (receivedText.isBlank()) amount else aiAmount(receivedText, mapping.amountFormat, toCurrency).second
                }
            }
            val title = cell(row, mapping.title).ifBlank { if (type == "transfer") "Transfer" else categoryName }
            val id = "ai_" + stableHash(listOf("ai", (offset + 2).toString(), date, type, amount.toString(), currency, fromName, toName, title).joinToString("|"))
            if (id in planner.seen) planner.duplicates++ else {
                val from = planner.account(fromName, currency)
                val to = if (type == "transfer") planner.account(toName, toCurrency) else null
                val category = if (type == "transfer") null else planner.category(categoryName, type)
                val tx = Transaction(id = id, type = type, amountMinor = amount, currency = currency,
                    accountId = from.id, toAccountId = to?.id, receivedAmountMinor = received,
                    categoryId = category?.id ?: "transfer", title = title,
                    description = cell(row, mapping.description), date = date, source = "ai")
                FinanceRepository.validateTransaction(tx)
                if (!planner.seen.add(id)) planner.duplicates++ else planner.transactions.add(tx)
            }
        } catch (e: Exception) {
            planner.fail(lineNumber, e)
        }
    }
    return planner.plan()
}

private fun AiCsvMapping.allIndexesNull(): Boolean =
    listOf(date, title, category, account, amount, currency, type, toAccount, transferAmount, description).all { it == null }

/** Tipe dari typeMapping hasil AI, lalu isi kolom tipe, lalu tanda nominal; fallback pengeluaran. */
private fun aiType(raw: String, mapping: AiCsvTypeMapping?): String? {
    val value = raw.trim().lowercase()
    if (value.isEmpty()) return null
    if (mapping != null) {
        when {
            mapping.income.any { it.trim().lowercase() == value } -> return "income"
            mapping.expense.any { it.trim().lowercase() == value } -> return "expense"
            mapping.transfer.any { it.trim().lowercase() == value } -> return "transfer"
        }
    }
    return when {
        "income" in value || "pemasukan" in value || "masuk" in value -> "income"
        "expense" in value || "pengeluaran" in value || "keluar" in value -> "expense"
        "transfer" in value -> "transfer"
        else -> null
    }
}

/**
 * Nominal dari CSV asing: coba format yang dipilih AI dulu, lalu heuristic pemisah desimal/ribuan.
 * Tujuannya tetap bisa mengimpor walau AI salah menilai format angka.
 */
private fun aiAmount(raw: String, format: String?, currency: String): Pair<Boolean, Long> {
    val text = raw.trim()
    val negative = Regex("^[^0-9]*-").containsMatchIn(text) || (text.startsWith("(") && text.endsWith(")"))
    val digitsOnly = text.replace(Regex("[^0-9.,]"), "")
    require(digitsOnly.any { it.isDigit() }) { MR.strings.dash_err_number_format.text() }
    val candidates = buildList {
        if (format == "en") add(digitsOnly.replace(",", ""))
        if (format == "eu") add(digitsOnly.replace(".", "").replace(',', '.'))
        add(normalizeSeparators(digitsOnly))
        add(digitsOnly.replace(".", "").replace(",", ""))
    }.filter { it.isNotBlank() }.distinct()
    for (candidate in candidates) {
        runCatching { return negative to minorAmount(candidate, currency) }
    }
    error(MR.strings.dash_err_number_format.text())
}

/** "1,234.50" -> 1234.50, "1.234,50" -> 1234.50, "50.000" -> 50000, "12,34" -> 12.34. */
private fun normalizeSeparators(value: String): String {
    val lastDot = value.lastIndexOf('.')
    val lastComma = value.lastIndexOf(',')
    val separator = maxOf(lastDot, lastComma)
    if (separator < 0) return value
    val tail = value.substring(separator + 1)
    val bothSeparators = lastDot >= 0 && lastComma >= 0
    val decimal = bothSeparators || tail.length in 1..2
    return if (decimal) {
        value.substring(0, separator).replace(".", "").replace(",", "") + "." + tail
    } else {
        value.replace(".", "").replace(",", "")
    }
}

private val aiMonthNames = mapOf(
    "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "mei" to 5, "may" to 5, "jun" to 6,
    "jul" to 7, "agu" to 8, "aug" to 8, "sep" to 9, "okt" to 10, "oct" to 10, "nov" to 11, "des" to 12, "dec" to 12,
)

/** Tanggal sesuai dateFormat hasil AI; format tak dikenal jatuh ke parser umum impor CSV. */
private fun aiDate(raw: String, format: String?): String {
    val value = raw.trim()
    val withTime = format == "yyyy-MM-dd HH:mm:ss" || format == "dd/MM/yyyy HH:mm"
    val pattern = if (withTime) format.orEmpty().substringBefore(' ') else format
    val datePart = if (withTime) value.substringBefore(' ') else value
    val parsed = when (pattern) {
        "yyyy-MM-dd" -> runCatching { LocalDate.parse(datePart) }.getOrNull()?.toString()
        "yyyy/MM/dd" -> numericDate(datePart.split('/'), year = 0, month = 1, day = 2)
        "dd/MM/yyyy" -> numericDate(datePart.split('/'), year = 2, month = 1, day = 0)
        "MM/dd/yyyy" -> numericDate(datePart.split('/'), year = 2, month = 0, day = 1)
        "dd.MM.yyyy" -> numericDate(datePart.split('.'), year = 2, month = 1, day = 0)
        "dd-MM-yyyy" -> numericDate(datePart.split('-'), year = 2, month = 1, day = 0)
        "dd MMM yyyy" -> datePart.split(' ').let { parts ->
            val month = aiMonthNames[parts.getOrNull(1)?.take(3)?.lowercase()]
            if (parts.size == 3 && month != null) dateOf(parts[2].toIntOrNull(), month, parts[0].toIntOrNull()) else null
        }
        else -> null
    }
    return parsed ?: importDate(value)
}

private fun numericDate(parts: List<String>, year: Int, month: Int, day: Int): String? =
    if (parts.size == 3) dateOf(parts[year].toIntOrNull(), parts[month].toIntOrNull(), parts[day].toIntOrNull()) else null

private fun dateOf(year: Int?, month: Int?, day: Int?): String? {
    if (year == null || month == null || day == null || year !in 1..9999 || month !in 1..12 || day !in 1..31) return null
    val normalized = "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
    return runCatching { LocalDate.parse(normalized).toString() }.getOrNull()
}
