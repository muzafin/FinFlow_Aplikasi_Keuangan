package com.clonical.kasiva

import com.clonical.kasiva.core.market.MarketAsset
import com.clonical.kasiva.core.market.MarketGroup
import com.clonical.kasiva.core.market.TROY_OUNCE_GRAMS
import com.clonical.kasiva.core.market.conversionFactor
import com.clonical.kasiva.core.market.parseChart
import com.clonical.kasiva.core.market.parseCurrencyApi
import com.clonical.kasiva.core.market.parseSpark
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarketDataTest {
    private fun near(expected: Double, actual: Double?) = assertTrue(actual != null && abs(expected - actual) < 1e-6, "expected $expected got $actual")

    @Test fun sparkUsesPreviousSessionCloseForIntradayChange() {
        val body = """{"BBCA.JK":{"close":[5200.0,null,5300.0,5325.0],"previousClose":5300.0,"fulldayPrice":5325.0,"fulldayChangePercent":99.0},
            "AAPL":{"close":[349.0,350.0],"chartPreviousClose":340.0},"BAD":{"close":[]}}"""
        val parsed = parseSpark(body)
        assertEquals(listOf(5200.0, 5300.0, 5325.0), parsed.getValue("BBCA.JK").third)
        near((5325.0 / 5300.0 - 1) * 100, parsed.getValue("BBCA.JK").second)
        near(350.0, parsed.getValue("AAPL").first)
        near((350.0 / 340.0 - 1) * 100, parsed.getValue("AAPL").second)
        assertTrue("BAD" !in parsed)
    }

    @Test fun sparkHandlesMissingSeriesAndRejectsInvalidPricesOrDailyReferences() {
        val parsed = parseSpark("""{
            "VALID":{"close":null,"fulldayPrice":25,"fulldayChangePercent":2.5},
            "NAN":{"close":["NaN","Infinity",0,-1],"fulldayPrice":"Infinity","previousClose":10},
            "NO_REFERENCE":{"close":[10,11]},
            "INTRADAY":{"close":[10,null,11],"previousClose":10},
            "BAD_CHANGE":{"close":[10,11],"previousClose":0,"fulldayChangePercent":"NaN"}
        }""")
        assertEquals(setOf("VALID", "INTRADAY"), parsed.keys)
        near(25.0, parsed.getValue("VALID").first)
        near(2.5, parsed.getValue("VALID").second)
        near(10.0, parsed.getValue("INTRADAY").second)
    }

    @Test fun chartPairsTimestampsWithValidCloses() {
        val body = """{"chart":{"result":[{"timestamp":[1,2,3],"indicators":{"quote":[{"close":[10.0,null,12.5]}]}}]}}"""
        assertEquals(listOf(1L to 10.0, 3L to 12.5), parseChart(body))
        assertEquals(emptyList(), parseChart("""{"chart":{"result":null}}"""))
    }

    @Test fun chartInvalidTimestampDoesNotShiftRemainingPrices() {
        val body = """{"chart":{"result":[{"timestamp":[1,null,3,4],"indicators":{"quote":[{"close":[10.0,11.0,12.5,"Infinity"]}]}}]}}"""
        assertEquals(listOf(1L to 10.0, 3L to 12.5), parseChart(body))
        assertEquals(emptyList(), parseChart("""{"chart":{"result":[{"timestamp":[1],"indicators":{"quote":null}}]}}"""))
    }

    @Test fun currencyApiCodesAreUppercased() {
        val (date, rates) = parseCurrencyApi("""{"date":"2026-11-10","usd":{"idr":16500.5,"eur":0.92,"bad":0}}""", "USD")
        assertEquals("2026-11-10", date)
        near(16500.5, rates["IDR"]); assertTrue("BAD" !in rates)
    }

    @Test fun metalsConvertOunceUsdToGramInBase() {
        val gold = MarketAsset("GC=F", "XAU", "Gold", MarketGroup.Metals, unit = "g")
        near(16500.0 / TROY_OUNCE_GRAMS, conversionFactor(gold, "USD", "IDR", mapOf("IDR" to 16500.0)))
    }

    @Test fun idrStocksConvertToOtherBases() {
        val bbca = MarketAsset("BBCA.JK", "BBCA", "BCA", MarketGroup.Idx)
        near(1.0, conversionFactor(bbca, "IDR", "IDR", emptyMap()))
        near(1.0 / 16000.0, conversionFactor(bbca, "IDR", "USD", mapOf("IDR" to 16000.0, "USD" to 1.0)))
        near(0.9 / 16000.0, conversionFactor(bbca, "IDR", "EUR", mapOf("IDR" to 16000.0, "EUR" to 0.9)))
        assertEquals(null, conversionFactor(bbca, "IDR", "EUR", emptyMap()))
        assertEquals(null, conversionFactor(bbca, "IDR", "EUR", mapOf("IDR" to 0.0, "EUR" to 0.9)))
        assertEquals(null, conversionFactor(bbca, "IDR", "EUR", mapOf("IDR" to 16000.0, "EUR" to Double.NaN)))
    }
}
