package com.clonical.kasiva

import com.clonical.kasiva.core.market.marketWatchlist
import com.clonical.kasiva.feature.investment.hasMarketAssetLogo
import kotlin.test.Test
import kotlin.test.assertTrue

class MarketAssetIconTest {
    @Test fun everyWatchedAssetHasAnOfflineLogoOrMetalPictogram() {
        val missing = marketWatchlist.filterNot(::hasMarketAssetLogo).map { it.symbol }
        assertTrue(missing.isEmpty(), "Missing market icons: $missing")
    }
}
