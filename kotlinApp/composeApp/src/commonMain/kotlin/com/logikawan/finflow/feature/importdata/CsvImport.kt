package com.logikawan.finflow.feature.importdata

import com.logikawan.finflow.core.data.FinanceRepository
import com.logikawan.finflow.core.model.Account
import com.logikawan.finflow.core.model.Category
import com.logikawan.finflow.core.model.FinanceData
import com.logikawan.finflow.core.model.ImportPlan
import com.logikawan.finflow.core.model.Transaction
import com.logikawan.finflow.core.util.decimalAmount
import com.logikawan.finflow.core.util.minorAmount
import kotlinx.datetime.*

object CsvImport {
    const val MAX_FILE_BYTES = 5 * 1024 * 1024
    const val MAX_ROWS = 10000
    val headers = listOf("Date", "Title", "Category", "Account", "Amount", "Currency", "Type", "Transfer Amount", "Transfer Currency", "To Account", "Receive Amount", "Receive Currency", "Description", "Due Date", "ID")
    private val exportHeaders = headers + listOf("FinFlow ID", "Source", "Source ID")

    // Parser ditulis mandiri berdasarkan format publik CSV Ivy; bukan menyalin implementasi GPL.
    fun parse(text: String): List<List<String>> {
        require(text.encodeToByteArray().size <= MAX_FILE_BYTES) { "CSV maksimal 5 MB" }
        val result = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var closedQuote = false
        var i = 0
        val input = text.removePrefix("\uFEFF")
        fun endCell() { row.add(cell.toString()); cell.clear(); closedQuote = false }
        fun endRow() { endCell(); if (row.any { it.isNotBlank() }) result.add(row); row = mutableListOf(); require(result.size <= MAX_ROWS + 1) { "Maksimal $MAX_ROWS transaksi per file" } }
        while (i < input.length) {
            val c = input[i]
            when {
                quoted && c == '"' && input.getOrNull(i + 1) == '"' -> { cell.append('"'); i++ }
                quoted && c == '"' -> { quoted = false; closedQuote = true }
                quoted -> cell.append(c)
                c == '"' && cell.isEmpty() && !closedQuote -> quoted = true
                c == ',' -> endCell()
                c == '\n' || c == '\r' -> { endRow(); if (c == '\r' && input.getOrNull(i + 1) == '\n') i++ }
                closedQuote -> require(c == ' ' || c == '\t') { "Karakter tidak valid setelah tanda kutip CSV" }
                c == '"' -> error("Tanda kutip CSV harus membungkus seluruh isi kolom")
                else -> cell.append(c)
            }
            i++
        }
        require(!quoted) { "Tanda kutip CSV belum ditutup" }
        if (cell.isNotEmpty() || row.isNotEmpty() || closedQuote) endRow()
        return result
    }

    fun preview(text: String, source: String, existing: FinanceData): ImportPlan {
        require(source in listOf("ivy", "finflow"))
        val rows = parse(text)
        require(rows.isNotEmpty()) { "CSV kosong" }
        val header = rows.first().map { it.trim().lowercase() }
        require(header.distinct().size == header.size) { "Nama kolom CSV tidak boleh berulang" }
        val required = listOf("Date", "Account", "Amount", "Currency", "Type", "ID")
        require(required.all { it.lowercase() in header }) { "Kolom wajib: ${required.joinToString()}. Gunakan ekspor CSV Ivy atau template FinFlow." }
        val accounts = mutableMapOf<String, Account>()
        val categories = mutableMapOf<String, Category>()
        val transactions = mutableListOf<Transaction>()
        val errors = mutableListOf<String>()
        val warnings = mutableListOf("CSV transaksi tidak berisi saldo awal: sesuaikan saldo awal setiap dompet setelah impor.", "Tanggal CSV Ivy tanpa zona waktu dibaca memakai zona waktu perangkat saat impor.")
        val seen = existing.transactions.map { it.id }.toMutableSet()
        var duplicates = 0
        fun account(name: String, currency: String): Account {
            require(name.isNotBlank() && name.length <= 100 && currency.matches(Regex("[A-Z]{3}"))) { "Nama dompet/kode mata uang tidak valid; aset kripto memerlukan impor khusus" }
            val matches = existing.accounts.filter { it.name == name && it.currency == currency && !it.archived }
            require(matches.size <= 1) { "Ada beberapa dompet bernama $name ($currency); ubah nama agar unik" }
            return matches.singleOrNull() ?: accounts.getOrPut("$currency:$name") {
                val id = "import_" + hex("$currency:$name")
                require(existing.accounts.none { it.id == id && it.archived }) { "Dompet $name diarsipkan; aktifkan kembali sebelum impor" }
                Account(id = id, name = name, currency = currency)
            }
        }
        rows.drop(1).forEachIndexed { index, row ->
            try {
                require(row.size == header.size) { "Jumlah kolom berbeda dengan header" }
                fun value(name: String) = header.indexOf(name.lowercase()).let { if (it < 0) "" else row[it].trim() }
                val externalId = value("ID")
                val exportedId = if (source == "finflow") value("FinFlow ID") else ""
                require(externalId.isNotBlank()) { "ID transaksi wajib diisi" }
                require(exportedId.isEmpty() || exportedId.matches(Regex("[A-Za-z0-9_-]{1,1400}"))) { "FinFlow ID tidak valid" }
                val origin = if (exportedId.isEmpty()) source else value("Source").ifBlank { "finflow" }
                val originId = if (exportedId.isEmpty()) externalId else value("Source ID").ifBlank { null }
                val managed = origin in com.logikawan.finflow.core.model.managedTransactionSources
                require(origin in listOf("manual", "ivy", "finflow", "notification") || managed) { "Source transaksi tidak dikenali" }
                require(originId == null || originId.length <= 200 && originId.encodeToByteArray().size <= 600) { "ID sumber maksimal 200 karakter/600 byte UTF-8" }
                val id = exportedId.ifEmpty { "${source}_" + hex(externalId) }
                // Ekspor FinFlow mempertahankan ID jurnal agar impor ulang tidak menyalin transaksi sendiri.
                if (id in seen) { duplicates++; return@forEachIndexed }
                if (managed) {
                    val warning = "CSV memulihkan jurnal pembayaran sebagai transaksi biasa. Tagihan, hutang, klaim dan jadwal berulang tidak disertakan dalam CSV."
                    if (warning !in warnings) warnings.add(warning)
                }
                val type = value("Type").lowercase()
                require(type in listOf("income", "expense", "transfer")) { "Type harus INCOME, EXPENSE, atau TRANSFER" }
                val currency = (if (type == "transfer") value("Transfer Currency").ifBlank { value("Currency") } else value("Currency")).uppercase()
                val from = account(value("Account"), currency)
                val to = if (type == "transfer") account(value("To Account"), value("Receive Currency").ifBlank { currency }.uppercase()) else null
                val categoryName = value("Category").ifBlank { "Lainnya" }
                require(categoryName.length <= 100) { "Nama kategori maksimal 100 karakter" }
                val category = if (type == "transfer") null else {
                    existing.categories.find { it.name == categoryName && it.type == type }
                        ?: categories.getOrPut("$type:$categoryName") { Category("import_" + hex("$type:$categoryName"), categoryName, type) }
                }
                val dateText = value("Date")
                val scheduled = dateText.isBlank()
                val date = parseDate(dateText.ifBlank { value("Due Date") })
                fun amountValue(raw: String, code: String): Long {
                    // Ekspor Ivy memakai angka Inggris berkoma ribuan, misalnya "1,234.50".
                    val normalized = if (source == "ivy" && ',' in raw) {
                        require(Regex("[0-9]{1,3}(,[0-9]{3})+(\\.[0-9]+)?").matches(raw)) { "Pemisah ribuan pada nominal Ivy tidak valid" }
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
                if (!seen.add(id)) duplicates++ else transactions.add(tx)
            } catch (e: Exception) {
                errors.add("Baris ${index + 2}: ${e.message ?: "Data tidak valid"}")
            }
        }
        return ImportPlan(accounts.values.toList(), categories.values.toList(), transactions, errors, warnings, duplicates)
    }

    private fun hex(value: String) = value.encodeToByteArray().joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
    private fun parseDate(value: String): String {
        require(value.isNotBlank()) { "Date atau Due Date wajib diisi" }
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
