package com.clonical.kasiva.feature.importdata

import com.clonical.kasiva.feature.ai.AiCsvMapping
import com.clonical.kasiva.feature.ai.AiCsvTypeMapping

/**
 * Ekspor aplikasi keuangan lain sering memuat baris ringkasan sebelum header, kolom kosong di kiri,
 * kolom Pengeluaran/Pemasukan terpisah, dan baris catatan di akhir. Normalisasi ini bekerja lokal
 * (tanpa jaringan) sehingga tabel yang dikirim ke AI maupun dikonversi sudah bersih.
 */
internal data class NormalizedCsv(
    val header: List<String>,
    val rows: List<List<String>>,
    /** Nomor baris asli (1-based) tiap baris data, untuk pesan galat yang akurat. */
    val rowNumbers: List<Int>,
    /** Pemetaan hasil deteksi lokal; null bila kolom penting tidak dikenali (lanjut ke AI). */
    val mapping: AiCsvMapping?,
)

private val dateWords = listOf("tanggal", "tgl", "date", "waktu", "time")
private val titleWords = listOf("keterangan", "judul", "title", "description", "deskripsi", "catatan", "note", "memo", "payee", "uraian")
private val categoryWords = listOf("kategori", "category")
private val accountWords = listOf("dompet", "akun", "rekening", "account", "wallet", "sumber")
private val expenseWords = listOf("pengeluaran", "expense", "keluar", "debit", "outflow")
private val incomeWords = listOf("pemasukan", "income", "masuk", "kredit", "credit", "inflow")
private val amountWords = listOf("jumlah", "nominal", "amount", "nilai", "total")
private val typeWords = listOf("tipe", "jenis", "type")
private val currencyWords = listOf("mata uang", "currency", "kurs")
private val allWords = dateWords + titleWords + categoryWords + accountWords + expenseWords + incomeWords + amountWords + typeWords

private fun matches(cell: String, words: List<String>): Boolean {
    val value = cell.trim().lowercase()
    return value.isNotEmpty() && words.any { value == it || value.startsWith("$it ") || value.endsWith(" $it") || value.contains(it) }
}

private fun headerScore(row: List<String>): Int = row.count { matches(it, allWords) }

internal fun normalizeCsv(all: List<List<String>>): NormalizedCsv {
    val headerAt = all.take(40).indexOfFirst { headerScore(it) >= 3 }.takeIf { it >= 0 } ?: 0
    val rawHeader = all.getOrNull(headerAt).orEmpty().map { it.trim() }
    val body = all.drop(headerAt + 1)
    val width = maxOf(rawHeader.size, body.maxOfOrNull { it.size } ?: 0)
    // Buang kolom yang header dan seluruh isinya kosong (mis. kolom kiri kosong pada ekspor spreadsheet).
    val keep = (0 until width).filter { column ->
        rawHeader.getOrNull(column).orEmpty().isNotBlank() || body.any { it.getOrNull(column).orEmpty().isNotBlank() }
    }
    var header = keep.map { rawHeader.getOrNull(it).orEmpty() }
    var rows = body.map { row -> keep.map { row.getOrNull(it).orEmpty().trim() } }
    val numbers = body.indices.map { headerAt + 2 + it }

    fun find(words: List<String>, skip: Set<Int> = emptySet()) = header.indices.firstOrNull { it !in skip && matches(header[it], words) }
    val date = find(dateWords)
    val expense = find(expenseWords)
    val income = find(incomeWords, setOfNotNull(expense))
    var amount = find(amountWords, setOfNotNull(date, expense, income))
    var type = find(typeWords, setOfNotNull(date, expense, income, amount))
    val title = find(titleWords, setOfNotNull(date, expense, income, amount))
    val category = find(categoryWords, setOfNotNull(date, title))
    val account = find(accountWords, setOfNotNull(date, title, category, amount))
    val currency = find(currencyWords)

    var typeMapping: AiCsvTypeMapping? = null
    // Kolom Pengeluaran dan Pemasukan terpisah digabung menjadi Jumlah + Tipe.
    if (amount == null && (expense != null || income != null)) {
        val merged = rows.map { row ->
            val out = row.getOrNull(expense ?: -1).orEmpty()
            val inc = row.getOrNull(income ?: -1).orEmpty()
            fun hasAmount(text: String) = text.any { it.isDigit() } && text.filter { it.isDigit() }.any { it != '0' }
            when {
                hasAmount(out) -> row + listOf(out, "expense")
                hasAmount(inc) -> row + listOf(inc, "income")
                else -> row + listOf("", "")
            }
        }
        rows = merged
        amount = header.size
        type = header.size + 1
        header = header + listOf("Jumlah", "Tipe")
        typeMapping = AiCsvTypeMapping(income = listOf("income"), expense = listOf("expense"))
    }

    val rowsForSample = rows.filter { row -> row.getOrNull(date ?: -1).orEmpty().isNotBlank() }
    val recognized = date != null && amount != null && (title != null || category != null) && rowsForSample.isNotEmpty()
    val mapping = if (!recognized) null else AiCsvMapping(
        date = date, title = title ?: category, category = category, account = account, amount = amount,
        currency = currency, type = type, description = null,
        dateFormat = detectDateFormat(rowsForSample.map { it[date!!] }),
        amountFormat = null,
        typeMapping = typeMapping, typeFromSign = type == null && rowsForSample.any { it[amount!!].trim().startsWith("-") },
    )
    return NormalizedCsv(header, rows, numbers, mapping)
}

private fun detectDateFormat(samples: List<String>): String? {
    val values = samples.map { it.trim().substringBefore(' ') }.filter { it.isNotEmpty() }.take(50)
    if (values.isEmpty()) return null
    if (values.all { Regex("[0-9]{4}-[0-9]{1,2}-[0-9]{1,2}").matches(it) }) return "yyyy-MM-dd"
    if (values.all { Regex("[0-9]{4}/[0-9]{1,2}/[0-9]{1,2}").matches(it) }) return "yyyy/MM/dd"
    if (values.all { Regex("[0-9]{1,2}/[0-9]{1,2}/[0-9]{4}").matches(it) }) {
        val secondOver12 = values.any { it.split('/')[1].toInt() > 12 }
        return if (secondOver12) "MM/dd/yyyy" else "dd/MM/yyyy"
    }
    if (values.all { Regex("[0-9]{1,2}\\.[0-9]{1,2}\\.[0-9]{4}").matches(it) }) return "dd.MM.yyyy"
    if (values.all { Regex("[0-9]{1,2}-[0-9]{1,2}-[0-9]{4}").matches(it) }) return "dd-MM-yyyy"
    return null
}
