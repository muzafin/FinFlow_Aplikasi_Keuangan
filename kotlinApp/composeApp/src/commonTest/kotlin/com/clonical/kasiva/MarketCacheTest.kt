package com.clonical.kasiva

import com.clonical.kasiva.core.market.MarketQuote
import com.clonical.kasiva.core.market.marketWatchlist
import com.clonical.kasiva.feature.investment.MarketCache
import com.clonical.kasiva.feature.investment.MarketState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MarketCacheTest {
    @Test fun reopeningPartialPricesRetainsFailureAndCurrencyBoundary() = runTest {
        try {
            MarketCache.put("IDR", listOf(MarketQuote(marketWatchlist.first(), "IDR", 5000.0, 1.0)), incomplete = true)
            val reopened = MarketState("IDR")
            reopened.load()
            assertTrue(reopened.failed)
            assertTrue(reopened.quotes.isNotEmpty())
            assertTrue(MarketState("USD").quotes.isEmpty())
            assertFalse(MarketCache.fresh("USD"))
        } finally { MarketCache.put("", emptyList()) }
    }
}
