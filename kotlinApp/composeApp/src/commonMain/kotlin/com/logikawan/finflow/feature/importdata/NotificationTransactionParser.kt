package com.logikawan.finflow.feature.importdata

import com.logikawan.finflow.core.data.FinanceRepository
import com.logikawan.finflow.core.util.currencyDigits
import com.logikawan.finflow.core.util.minorAmount

data class ParsedNotificationTransaction(val type: String, val amountMinor: Long, val currency: String)

object NotificationTransactionParser {
    private val secret = Regex("(?i)\\b(otp|pin|password|passcode|verification|authentication|verifikasi|sandi|kode\\s+akses|security\\s+code|one[- ]?time)\\b")
    private val rejected = Regex("(?i)\\b(gagal|failed|declined|ditolak|dibatalkan|cancelled|canceled|pending|tertunda|menunggu|diproses|processing|promo|diskon|penawaran|voucher|cashback|reward)\\b")
    private val completed = Regex("(?i)\\b(berhasil|sukses|successful|successfully|completed|settled|diterima|menerima|received|credited|debited|paid|terpotong|didebet|didebit|debet|masuk|bertambah)\\b")
    private val incoming = Regex("(?i)\\b(diterima|menerima|received|credited|pemasukan)\\b|(?:dana|uang|saldo)\\s+(?:masuk|bertambah)")
    private val outgoing = Regex("(?i)\\b(terpotong|debited|debit|debet|didebet|didebit|paid|membayar|dibayar|pengeluaran)\\b|transfer\\s+(?:telah\\s+)?(?:berhasil|sukses)|successfully\\s+(?:sent|transferred)|(?:sent|transferred)\\s+to")
    private val payment = Regex("(?i)\\b(pembayaran|payment|pembelian|purchase|transfer)\\b")
    private val balancePrefix = Regex("(?i)(?:saldo(?:\\s+(?:akhir|tersedia|anda|rekening))?|sisa\\s+saldo|(?:(?:available|remaining|current)\\s+)?balance)\\s*(?:[:=]|adalah|sebesar)?\\s*$")

    fun parse(text: String, currency: String): ParsedNotificationTransaction? {
        if (text.length > 16_000 || secret.containsMatchIn(text) || rejected.containsMatchIn(text) || !completed.containsMatchIn(text)) return null
        val code = currency.uppercase()
        if (!code.matches(Regex("[A-Z]{3}"))) return null
        val credit = incoming.containsMatchIn(text)
        val debit = outgoing.containsMatchIn(text)
        if (credit && debit) return null
        if (credit && Regex("(?i)diterima\\s+(?:oleh|merchant|penerima)|received\\s+by").containsMatchIn(text)) return null
        val type = when {
            credit -> "income"
            debit || payment.containsMatchIn(text) -> "expense"
            else -> return null
        }
        val symbol = when (code) {
            "IDR" -> "Rp\\.?|IDR"
            "USD" -> "USD|US\\$"
            "EUR" -> "EUR|€"
            "GBP" -> "GBP|£"
            "JPY" -> "JPY|JP¥"
            "SGD" -> "SGD|S\\$"
            "AUD" -> "AUD|A\\$"
            "CAD" -> "CAD|C\\$"
            else -> Regex.escape(code)
        }
        val number = "[0-9](?:[0-9.,]*[0-9])?(?:[ \\u00a0][0-9]{3})*"
        val money = Regex("(?i)(?:$symbol)\\s*($number)(?![0-9])|($number)\\s*(?:$symbol)")
        val matches = money.findAll(text).filterNot { match ->
            val prefix = text.substring(0, match.range.first).takeLast(80)
            balancePrefix.containsMatchIn(prefix)
        }.toList()
        val amounts = matches.map { normalize(it.groupValues[1].ifEmpty { it.groupValues[2] }, code) ?: return null }.toSet()
        // Beberapa nominal selain saldo tidak ditebak: biaya/nominal berbeda perlu dicatat manual.
        if (amounts.size != 1) return null
        return ParsedNotificationTransaction(type, amounts.single(), code)
    }

    private fun normalize(raw: String, currency: String): Long? {
        return runCatching {
        val value = raw.filterNot { it.isWhitespace() }
        if (value.endsWith('.') || value.endsWith(',')) return null
        val digits = currencyDigits(currency)
        val separators = value.filter { it == '.' || it == ',' }.toSet()
        val plain = when {
            separators.isEmpty() -> value
            separators.size == 2 -> {
                val decimal = if (value.lastIndexOf('.') > value.lastIndexOf(',')) '.' else ','
                val grouping = if (decimal == '.') ',' else '.'
                val parts = value.split(decimal)
                if (parts.size != 2 || parts[1].length !in 1..digits || !validGrouping(parts[0], grouping)) return null
                parts[0].replace(grouping.toString(), "") + "." + parts[1]
            }
            else -> {
                val separator = separators.single()
                val groups = value.split(separator)
                if (groups.size == 2 && groups[1].length == 3 && digits == 3) return null
                if (validGrouping(value, separator)) groups.joinToString("")
                else if (groups.size == 2 && groups[1].length in 1..digits) groups[0] + "." + groups[1]
                else return null
            }
        }
        minorAmount(plain, currency).takeIf { it in 1..FinanceRepository.MAX_MONEY }
        }.getOrNull()
    }

    private fun validGrouping(text: String, separator: Char): Boolean {
        val groups = text.split(separator)
        return groups.size > 1 && groups.first().length in 1..3 && groups.all { it.all(Char::isDigit) } && groups.drop(1).all { it.length == 3 }
    }
}
