package com.clonical.kasiva.feature.importdata

import com.clonical.kasiva.core.data.FinanceRepository
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.model.Account
import com.clonical.kasiva.core.model.Category
import com.clonical.kasiva.core.model.FinanceData
import com.clonical.kasiva.core.model.ImportPlan
import com.clonical.kasiva.core.model.Transaction
import com.clonical.kasiva.core.util.decimalAmount
import com.clonical.kasiva.core.util.minorAmount
import com.clonical.kasiva.core.util.normalizeAmountText
import com.clonical.kasiva.core.util.today
import com.clonical.kasiva.feature.ai.AiService
import com.clonical.kasiva.i18n.*
import kotlinx.datetime.*



object CsvImport {
    const val MAX_FILE_BYTES = 5 * 1024 * 1024
    const val MAX_ROWS = 10000
    val headers = listOf("Date", "Title", "Category", "Account", "Amount", "Currency", "Type", "Transfer Amount", "Transfer Currency", "To Account", "Receive Amount", "Receive Currency", "Description", "Due Date", "ID")
    private val exportHeaders = headers + listOf("Kasiva ID", "Source", "Source ID")

    // Parser ditulis mandiri berdasarkan format publik CSV Ivy; bukan menyalin implementasi GPL.
    fun parse(text: String, delimiter: Char = ','): List<List<String>> {
        require(text.encodeToByteArray().size <= MAX_FILE_BYTES) { MR.strings.ai_csv_max_size.text() }
        val result = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var closedQuote = false
        var i = 0
        val input = text.removePrefix("\uFEFF")
        fun endCell() { row.add(cell.toString()); cell.clear(); closedQuote = false }
        fun endRow() { endCell(); if (row.any { it.isNotBlank() }) result.add(row); row = mutableListOf(); require(result.size <= MAX_ROWS + 1) { MR.strings.ai_csv_max_rows.text(MAX_ROWS) } }
        while (i < input.length) {
            val c = input[i]
            when {
                quoted && c == '"' && input.getOrNull(i + 1) == '"' -> { cell.append('"'); i++ }
                quoted && c == '"' -> { quoted = false; closedQuote = true }
                quoted -> cell.append(c)
                c == '"' && cell.isEmpty() && !closedQuote -> quoted = true
                c == delimiter -> endCell()
                c == '\n' || c == '\r' -> { endRow(); if (c == '\r' && input.getOrNull(i + 1) == '\n') i++ }
                closedQuote -> require(c == ' ' || c == '\t') { MR.strings.ai_csv_invalid_after_quote.text() }
                c == '"' -> error(MR.strings.ai_csv_quote_whole_cell.text())
                else -> cell.append(c)
            }
            i++
        }
        require(!quoted) { MR.strings.ai_csv_quote_unclosed.text() }
        if (cell.isNotEmpty() || row.isNotEmpty() || closedQuote) endRow()
        return result
    }

    /** Pemisah kolom ekspor: koma, titik koma (locale Eropa/Indonesia), atau tab; dipilih dari baris awal di luar tanda kutip. */
    fun detectDelimiter(text: String): Char {
        val counts = mapOf(',' to 0, ';' to 0, '\t' to 0).toMutableMap()
        var quoted = false
        var lines = 0
        for (c in text.removePrefix("﻿")) {
            if (c == '"') quoted = !quoted
            else if (!quoted && c == '\n') { if (++lines >= 40) break }
            else if (!quoted && c in counts) counts[c] = counts.getValue(c) + 1
        }
        return counts.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.key ?: ','
    }

    fun preview(text: String, source: String, existing: FinanceData): ImportPlan {
        require(CsvImport.sources.any { it.id == source })
        // Deteksi otomatis (AI) dipetakan server lewat CsvImport.previewWithAi; tidak ada mapping posisional.
        if (source == "ai") error("Sumber ai memakai CsvImport.previewWithAi")
        // Wallet by BudgetBakers memakai ";" sebagai pemisah kolom dan koma sebagai pemisah desimal.
        // Dulu koma diganti spasi sehingga "12500,50" terbaca 1.250.050 (100×). Kini dibaca langsung dengan pemisah ";".
        val rows = if (source == "wallet") parse(text, ';') else parse(text)
        require(rows.isNotEmpty()) { MR.strings.ai_csv_empty.text() }
        return if (source == "ivy" || source == "finflow") previewTemplate(rows, source, existing) else previewPositional(rows, source, existing)
    }

    /** Pratinjau tanpa jaringan bila kolom CSV dikenali dari header-nya; null bila format asing (perlu AI). */
    fun previewLocal(text: String, existing: FinanceData): ImportPlan? {
        val parsed = parse(text, detectDelimiter(text))
        require(parsed.isNotEmpty()) { MR.strings.ai_csv_empty.text() }
        val table = normalizeCsv(parsed)
        val mapping = table.mapping ?: return null
        return buildAiPlan(table.rows, mapping, existing, table.rowNumbers)
    }

    /** Impor sumber "ai": server hanya memetakan kolom (header + maksimum 20 baris contoh); seluruh file dikonversi lokal. */
    suspend fun previewWithAi(text: String, service: AiService, language: String, existing: FinanceData): ImportPlan {
        val parsed = parse(text, detectDelimiter(text))
        require(parsed.isNotEmpty()) { MR.strings.ai_csv_empty.text() }
        // Header dicari sendiri, kolom kosong dibuang, kolom Pengeluaran/Pemasukan digabung. Bila kolom
        // dikenali, pemetaan selesai di perangkat (juga berjalan offline); AI hanya cadangan untuk format asing.
        val table = normalizeCsv(parsed)
        val mapping = table.mapping ?: service.csvMap(table.header, table.rows.filter { row -> row.any { it.isNotBlank() } }.take(20), language)
        return buildAiPlan(table.rows, mapping, existing, table.rowNumbers)
    }

    /** Format Ivy/Kasiva: kolom dikenali dari nama header. Perilaku lama dipertahankan. */
    private fun previewTemplate(rows: List<List<String>>, source: String, existing: FinanceData): ImportPlan {
        val planner = Planner(source, existing)
        val header = rows.first().map { it.trim().lowercase() }
        require(header.distinct().size == header.size) { MR.strings.ai_csv_duplicate_columns.text() }
        val required = listOf("Date", "Account", "Amount", "Currency", "Type", "ID")
        require(required.all { it.lowercase() in header }) { MR.strings.ai_csv_required_columns.text(required.joinToString()) }
        rows.drop(1).forEachIndexed { index, row ->
            try {
                require(row.size == header.size) { MR.strings.ai_csv_column_count.text() }
                // Ekspor Kasiva menambah "'" di depan teks berawalan = + - @ (anti formula); buang lagi saat impor (P4-06).
                fun unescape(raw: String) = if (source == "finflow" && raw.startsWith("'") && raw.length > 1 &&
                    (raw.substring(1).trimStart().firstOrNull() in listOf('=', '+', '-', '@') || raw[1] in listOf('\t', '\r', '\n'))) raw.substring(1) else raw
                fun value(name: String) = header.indexOf(name.lowercase()).let { if (it < 0) "" else unescape(row[it]).trim() }
                val externalId = value("ID")
                val exportedId = if (source == "finflow") value("Kasiva ID") else ""
                require(externalId.isNotBlank()) { MR.strings.ai_csv_id_required.text() }
                require(exportedId.isEmpty() || exportedId.matches(Regex("[A-Za-z0-9_-]{1,1400}"))) { MR.strings.ai_csv_invalid_finflow_id.text() }
                val origin = if (exportedId.isEmpty()) source else value("Source").ifBlank { "finflow" }
                val originId = if (exportedId.isEmpty()) externalId else value("Source ID").ifBlank { null }
                val managed = origin in com.clonical.kasiva.core.model.managedTransactionSources
                require(origin in listOf("manual", "notification") || CsvImport.sources.any { it.id == origin } || managed) { MR.strings.ai_csv_unknown_source.text() }
                require(originId == null || originId.length <= 200 && originId.encodeToByteArray().size <= 600) { MR.strings.ai_csv_source_id_too_long.text() }
                val id = exportedId.ifEmpty { "${source}_" + hex(externalId) }
                // Ekspor Kasiva mempertahankan ID jurnal agar impor ulang tidak menyalin transaksi sendiri.
                if (id in planner.seen) { planner.duplicates++; return@forEachIndexed }
                if (managed) {
                    val warning = MR.strings.ai_csv_warn_managed.text()
                    if (warning !in planner.warnings) planner.warnings.add(warning)
                }
                val type = value("Type").lowercase()
                require(type in listOf("income", "expense", "transfer")) { MR.strings.ai_csv_invalid_type.text() }
                val currency = (if (type == "transfer") value("Transfer Currency").ifBlank { value("Currency") } else value("Currency")).uppercase()
                val from = planner.account(value("Account"), currency)
                val to = if (type == "transfer") planner.account(value("To Account"), value("Receive Currency").ifBlank { currency }.uppercase()) else null
                val categoryName = value("Category").ifBlank { "Lainnya" }
                val category = if (type == "transfer") null else planner.category(categoryName, type)
                val dateText = value("Date")
                val scheduled = dateText.isBlank()
                val date = parseDate(dateText.ifBlank { value("Due Date") })
                fun amountValue(raw: String, code: String): Long {
                    // Ekspor Ivy memakai angka Inggris berkoma ribuan, misalnya "1,234.50".
                    val normalized = if (source == "ivy" && ',' in raw) {
                        require(Regex("[0-9]{1,3}(,[0-9]{3})+(\\.[0-9]+)?").matches(raw)) { MR.strings.ai_csv_invalid_ivy_grouping.text() }
                        raw.replace(",", "")
                    } else raw
                    return minorAmount(normalized, code)
                }
                val amount = amountValue(if (type == "transfer") value("Transfer Amount") else value("Amount"), currency)
                val received = to?.let { amountValue(value("Receive Amount"), it.currency) }
                val tx = Transaction(id = id, type = type, amountMinor = amount, currency = currency,
                    accountId = from.id, toAccountId = to?.id, receivedAmountMinor = received,
                    categoryId = category?.id ?: "transfer", title = value("Title").ifBlank { categoryName },
                    description = value("Description"), date = date, source = if(managed) "finflow" else origin, sourceId = originId, scheduled = scheduled)
                FinanceRepository.validateTransaction(tx)
                if (!planner.seen.add(id)) planner.duplicates++ else planner.transactions.add(tx)
            } catch (e: Exception) {
                planner.fail(index + 2, e)
            }
        }
        return planner.plan()
    }

    /** Format aplikasi lain: baris pertama header, sisanya dipetakan sesuai posisi kolom [CsvColumns]. */
    private fun previewPositional(rows: List<List<String>>, source: String, existing: FinanceData): ImportPlan {
        val planner = Planner(source, existing)
        val columns = csvColumns(source)
        var warnedTransfer = false
        fun cell(row: List<String>, index: Int?): String = if (index == null) "" else row.getOrNull(index)?.trim().orEmpty()
        fun currencyOf(row: List<String>): String {
            val code = cell(row, columns.currency).uppercase()
            return if (code.matches(Regex("[A-Z]{3}"))) code else planner.baseCurrency
        }
        val data = rows.drop(1)
        val monthFirst = detectMonthFirst(data.map { cell(it, columns.date) })
        var index = 0
        var line = 2
        while (index < data.size) {
            val row = data[index]
            val pair = if (columns.pairedTransfer && index + 1 < data.size && isTransferType(cell(row, columns.type), source)) data[index + 1] else null
            try {
                val amount = parseNumber(cell(row, columns.amount))
                val currency = currencyOf(row)
                val fromName = cell(row, columns.account)
                val categoryName = cell(row, columns.category).ifBlank { "Lainnya" }
                val title = cell(row, columns.title)
                val description = cell(row, columns.description)
                val date = importDate(cell(row, columns.date), monthFirst)
                var type = when {
                    columns.typeFromCategory -> if (categoryName.lowercase() == "income") "income" else "expense"
                    cell(row, columns.type).isNotBlank() -> resolveType(cell(row, columns.type), source)
                    else -> if (amount.negative) "expense" else "income"
                }
                // Financisto mengekspor pengeluaran bernilai negatif dan pemasukan positif.
                if (source == "financisto" && type == "expense" && !amount.negative) type = "income"
                var toName = ""
                var toCurrency = ""
                var received: Long? = null
                if (type == "transfer") {
                    val pairName = pair?.let { cell(it, columns.account) }.orEmpty()
                    if (pair != null && pairName.isNotBlank()) {
                        toName = pairName
                        toCurrency = currencyOf(pair)
                        received = minorAmount(parseNumber(cell(pair, columns.amount)).digits, toCurrency)
                    } else {
                        // Format ini tidak punya kolom dompet tujuan: catat sebagai pengeluaran, jangan gagalkan file.
                        type = "expense"
                        if (!warnedTransfer) { warnedTransfer = true; planner.warnings.add(MR.strings.ai_csv_warn_transfer_no_target.text()) }
                    }
                }
                val amountMinor = minorAmount(amount.digits, currency)
                val id = planner.importId(source, source, date, type, amountMinor, currency, fromName, toName, title.ifBlank { categoryName })
                if (id == null || id in planner.seen) planner.duplicates++ else {
                    val from = planner.account(fromName, currency)
                    val to = if (type == "transfer") planner.account(toName, toCurrency) else null
                    val category = if (type == "transfer") null else planner.category(categoryName, type)
                    val tx = Transaction(id = id, type = type, amountMinor = amountMinor, currency = currency,
                        accountId = from.id, toAccountId = to?.id, receivedAmountMinor = received,
                        categoryId = category?.id ?: "transfer", title = title.ifBlank { categoryName },
                        description = description, date = date, source = source)
                    FinanceRepository.validateTransaction(tx)
                    if (!planner.seen.add(id)) planner.duplicates++ else planner.transactions.add(tx)
                }
            } catch (e: Exception) {
                planner.fail(line, e)
            }
            index += if (pair != null) 2 else 1
            line += if (pair != null) 2 else 1
        }
        return planner.plan()
    }

    internal class Planner(source: String, private val existing: FinanceData) {
        val accounts = mutableMapOf<String, Account>()
        val categories = mutableMapOf<String, Category>()
        val transactions = mutableListOf<Transaction>()
        val errors = mutableListOf<String>()
        val warnings = mutableListOf(MR.strings.ai_csv_warn_opening_balance.text()).apply {
            if (source == "ivy" || source == "finflow") add(MR.strings.ai_csv_warn_timezone.text())
        }
        val seen = existing.transactions.map { it.id }.toMutableSet()
        var duplicates = 0
        private val occurrences = mutableMapOf<String, Int>()
        private val importedContent = existing.transactions.groupingBy { listOf(it.source, it.date, it.type, it.amountMinor.toString(), it.currency, it.title).joinToString("|") }.eachCount()

        /**
         * Id impor dari ISI baris + urutan kemunculan isi yang sama di file (bukan nomor baris), sehingga ekspor yang
         * lebih baru (baris bergeser) tidak menggandakan riwayat (P4-03). Null bila isi ini sudah pernah diimpor,
         * termasuk impor lama yang id-nya masih memakai nomor baris.
         */
        fun importId(prefix: String, txSource: String, date: String, type: String, amount: Long, currency: String, from: String, to: String, title: String): String? {
            val content = listOf(txSource, date, type, amount.toString(), currency, from, to, title).joinToString("|")
            val n = occurrences[content] ?: 0
            occurrences[content] = n + 1
            if (n < (importedContent[listOf(txSource, date, type, amount.toString(), currency, title).joinToString("|")] ?: 0)) return null
            return prefix + "_" + stableHash("$content|#$n")
        }
        val baseCurrency = existing.profile?.baseCurrency ?: "IDR"

        fun account(name: String, currency: String): Account {
            require(name.isNotBlank() && name.length <= 100 && currency.matches(Regex("[A-Z]{3}"))) { MR.strings.ai_csv_invalid_wallet.text() }
            // Dompet tabungan target dikelola lewat Setor/Tarik; baris CSV tidak boleh masuk ke sana (P1-02).
            val matches = existing.accounts.filter { it.name == name && it.currency == currency && !it.archived && it.type != "savings" }
            require(matches.size <= 1) { MR.strings.ai_csv_ambiguous_wallet.text(name, currency) }
            return matches.singleOrNull() ?: accounts.getOrPut("$currency:$name") {
                val id = "import_" + hex("$currency:$name")
                require(existing.accounts.none { it.id == id && it.archived }) { MR.strings.ai_csv_archived_wallet.text(name) }
                Account(id = id, name = name, currency = currency)
            }
        }

        fun category(name: String, type: String): Category {
            require(name.length <= 100) { MR.strings.ai_csv_category_too_long.text() }
            return existing.categories.find { it.name == name && it.type == type }
                ?: categories.getOrPut("$type:$name") { Category("import_" + hex("$type:$name"), name, type) }
        }

        fun fail(line: Int, e: Exception) {
            errors.add(MR.strings.ai_csv_row_error.text(line, e.message ?: MR.strings.ai_csv_invalid_data.text()))
        }

        fun plan() = ImportPlan(accounts.values.toList(), categories.values.toList(), transactions, errors, warnings, duplicates)
    }

    private fun parseDate(value: String): String {
        require(value.isNotBlank()) { MR.strings.ai_csv_date_required.text() }
        return when {
            value.length == 10 -> LocalDate.parse(value).toString()
            value.endsWith('Z') || Regex("[+-]\\d\\d:\\d\\d$").containsMatchIn(value) -> Instant.parse(value).toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
            else -> LocalDateTime.parse(value.replace(' ', 'T')).date.toString()
        }
    }

    fun export(data: FinanceData, transactions: List<Transaction> = data.transactions): String = buildString {
        appendLine(exportHeaders.joinToString(","))
        for (tx in transactions) {
            val target = data.accounts.find { it.id == tx.toAccountId }
            val fields = listOf(if (tx.scheduled) "" else tx.date, tx.title, data.categoryName(tx.categoryId), data.accountName(tx.accountId),
                decimalAmount(tx.amountMinor, tx.currency), tx.currency, tx.type.uppercase(),
                if (target != null) decimalAmount(tx.amountMinor, tx.currency) else "", if (target != null) tx.currency else "",
                target?.name ?: "", if (target != null) decimalAmount(tx.receivedAmountMinor ?: tx.amountMinor, target.currency) else "",
                target?.currency ?: "", tx.description, if (tx.scheduled) tx.date else "", tx.sourceId ?: tx.id,
                tx.id, tx.source, tx.sourceId.orEmpty())
            appendLine(fields.joinToString(",") { csvCell(it) })
        }
    }
    private fun csvCell(value: String): String {
        // Awalan formula diperlakukan sebagai teks saat laporan dibuka di spreadsheet.
        val safe = if (value.trimStart().firstOrNull() in listOf('=', '+', '-', '@') || value.firstOrNull() in listOf('\t', '\r', '\n')) "'$value" else value
        return "\"${safe.replace("\"", "\"\"")}\""
    }
    val template = headers.joinToString(",") + "\n2026-10-26,Gaji bulanan,Gaji,Bank,5000000.00,IDR,INCOME,,,,,,,,sample-001\n"
}

internal fun hex(value: String) = value.encodeToByteArray().joinToString("") { it.toUByte().toString(16).padStart(2, '0') }

internal fun stableHash(value: String): String {
    var hash = -3750763034362895579L
    for (byte in value.encodeToByteArray()) hash = (hash xor (byte.toLong() and 0xFF)) * 1099511628211L
    return hash.toULong().toString(16).padStart(16, '0')
}

internal data class ParsedNumber(val negative: Boolean, val digits: String)

/** Angka ekspor aplikasi lain: "1,234.50", "1.234,50", "-25", "(25)", "25 IDR". */
internal fun parseNumber(raw: String): ParsedNumber {
    // Satu parser untuk semua nominal; "25.000" kini 25 ribu, bukan 25 (P4-02).
    val normalized = normalizeAmountText(raw) ?: throw IllegalArgumentException(MR.strings.dash_err_number_format.text())
    val negative = normalized.startsWith("-")
    val value = normalized.removePrefix("-")
    require(Regex("[0-9]+(\\.[0-9]+)?").matches(value)) { MR.strings.dash_err_number_format.text() }
    return ParsedNumber(negative, value)
}

private fun resolveType(raw: String, source: String): String {
    val type = raw.trim().lowercase()
    return when {
        type.contains("income") -> "income"
        type.contains("expense") -> "expense"
        type.contains("transfer") -> "transfer"
        // Financisto mencampur kolom tipe dengan kolom lain; perlakukan sebagai pengeluaran.
        source == "financisto" -> "expense"
        else -> error(MR.strings.ai_csv_invalid_type.text())
    }
}

private fun isTransferType(raw: String, source: String) = raw.isNotBlank() && resolveType(raw, source) == "transfer"

/** Tanggal: ISO, dd/MM/yyyy, dd.MM.yyyy, yyyy/MM/dd; waktu diabaikan karena transaksi hanya menyimpan tanggal. */
/**
 * Urutan hari/bulan ditentukan SEKALI per file (P4-04): true = bulan dulu (AS), false = hari dulu, null = ambigu
 * (semua bagian ≤ 12, dipakai hari dulu seperti sebelumnya).
 */
internal fun detectMonthFirst(values: List<String>): Boolean? {
    var dayFirst = false; var monthFirst = false
    for (raw in values) {
        val parts = raw.trim().substringBefore('T').substringBefore(' ').split('/', '.', '-')
        if (parts.size != 3) continue
        val a = parts[0].toIntOrNull() ?: continue; val b = parts[1].toIntOrNull() ?: continue
        if (a >= 1000) continue
        if (a > 12) dayFirst = true
        if (b > 12) monthFirst = true
    }
    return when { monthFirst && !dayFirst -> true; dayFirst && !monthFirst -> false; else -> null }
}

internal fun importDate(raw: String, monthFirst: Boolean? = null): String {
    val value = raw.trim()
    require(value.isNotBlank()) { MR.strings.ai_csv_date_required.text() }
    if (value.endsWith('Z') || Regex("[+-]\\d\\d:?\\d\\d$").containsMatchIn(value)) {
        runCatching { Instant.parse(value).toLocalDateTime(TimeZone.currentSystemDefault()).date.toString() }.getOrNull()?.let { return it }
    }
    val datePart = value.substringBefore('T').substringBefore(' ').trim()
    runCatching { return LocalDate.parse(datePart).toString() }
    val parts = datePart.split('/', '.', '-')
    require(parts.size == 3) { MR.strings.core_error_date_format.text() }
    val numbers = parts.map { it.toIntOrNull() ?: error(MR.strings.core_error_date_format.text()) }
    val first = numbers[0]
    val second = numbers[1]
    val third = numbers[2]
    val year: Int
    val month: Int
    val day: Int
    if (first >= 1000) {
        year = first; month = second; day = third
    } else {
        // Tahun 2 digit: "99" = 1999, "26" = 2026 (dulu selalu 20yy).
        year = if (third in 1000..9999) third else if (third + 2000 > today().year + 1) third + 1900 else third + 2000
        when {
            monthFirst == true -> { day = second; month = first }
            monthFirst == false -> { day = first; month = second }
            first > 12 -> { day = first; month = second }
            second > 12 -> { day = second; month = first }
            else -> { day = first; month = second }
        }
    }
    val normalized = "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
    runCatching { return LocalDate.parse(normalized).toString() }
    error(MR.strings.core_error_date_format.text())
}
