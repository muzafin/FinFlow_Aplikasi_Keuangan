package com.logikawan.finflow

import com.logikawan.finflow.feature.importdata.NotificationTransactionParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// Kasus sumber saja; tidak dijalankan selama pengerjaan.
class NotificationTransactionParserTest {
    @Test
    fun parsesExpenseWithoutMistakingRemainingBalanceForAmount() {
        val tx = NotificationTransactionParser.parse("Pembayaran QRIS berhasil Rp10.000. Saldo tersedia Rp90.000", "IDR")!!
        assertEquals("expense", tx.type)
        assertEquals(1_000_000L, tx.amountMinor)
    }

    @Test
    fun recognizesIncomingAndExplicitInternationalCurrency() {
        assertEquals("income", NotificationTransactionParser.parse("Anda menerima transfer Rp125.000", "IDR")?.type)
        assertEquals(123450L, NotificationTransactionParser.parse("Payment received USD 1,234.50", "USD")?.amountMinor)
        assertEquals(123456L, NotificationTransactionParser.parse("Payment received 1.234,56 EUR", "EUR")?.amountMinor)
    }

    @Test
    fun rejectsSecretsPromotionsFailedAndAmbiguousTransactions() {
        listOf("OTP transfer berhasil Rp100.000 kode 123456", "Transfer gagal Rp100.000", "Promo pembayaran berhasil Rp100.000",
            "Transfer berhasil Rp100.000 biaya admin Rp2.500", "Saldo Anda Rp100.000", "Pembayaran berhasil Rp100.000 diterima oleh merchant")
            .forEach { assertNull(NotificationTransactionParser.parse(it, "IDR"), it) }
        assertNull(NotificationTransactionParser.parse("Payment received KWD 1.234", "KWD"))
    }
}
