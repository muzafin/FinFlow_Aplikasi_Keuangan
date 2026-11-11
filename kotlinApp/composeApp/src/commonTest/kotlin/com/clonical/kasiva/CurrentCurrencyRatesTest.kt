package com.clonical.kasiva

import com.clonical.kasiva.core.market.parseCurrentRates
import com.clonical.kasiva.core.market.combineCurrentRates
import com.clonical.kasiva.feature.settings.oneUnitInBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class CurrentCurrencyRatesTest {
    @Test fun currentRatesOverrideDailyWhileMissingCurrenciesKeepTheirDate() {
        val combined = combineCurrentRates(mapOf("USD" to 1.0, "IDR" to 16000.0), "2026-11-10" to mapOf("USD" to 1.0, "IDR" to 15000.0, "SLE" to 23.0))
        assertEquals(16000.0, combined.rates["IDR"])
        assertEquals(23.0, combined.rates["SLE"])
        assertEquals(setOf("SLE"), combined.dailyCodes)
        assertEquals("2026-11-10", combined.dailyDate)
    }
    @Test fun currentRatesAcceptStringNumbersAndRejectInvalidRates() {
        val rates = parseCurrentRates("""{"data":{"currency":"USD","rates":{"USD":"1","IDR":"16000.5","eur":"0.9","BAD":"NaN","INF":"Infinity","ZERO":"0","NEG":"-1"}}}""", "usd")
        assertEquals(mapOf("USD" to 1.0, "IDR" to 16000.5, "EUR" to 0.9), rates)
    }

    @Test fun mismatchedOrEmptyRatesCannotMasqueradeAsValidBase() {
        assertFailsWith<IllegalArgumentException> { parseCurrentRates("""{"data":{"currency":"EUR","rates":{"USD":"1","IDR":"16000"}}}""", "USD") }
        assertFailsWith<IllegalArgumentException> { parseCurrentRates("""{"data":{"currency":"USD","rates":{}}}""", "USD") }
    }

    @Test fun exchangeRatesCannotCrashOrOverflowForInvalidValues() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.0, -1.0, Double.MIN_VALUE).forEach {
            assertNull(oneUnitInBase(it, "IDR"))
        }
        assertEquals(1_600_000L, oneUnitInBase(1.0 / 16000, "IDR"))
    }
}
