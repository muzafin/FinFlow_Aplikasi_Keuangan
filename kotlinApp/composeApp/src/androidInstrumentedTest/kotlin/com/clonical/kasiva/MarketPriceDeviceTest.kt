package com.clonical.kasiva

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.market.MarketQuote
import com.clonical.kasiva.core.market.marketWatchlist
import com.clonical.kasiva.feature.auth.EntryTheme
import com.clonical.kasiva.feature.investment.MarketState
import com.clonical.kasiva.feature.investment.marketPriceItems
import com.clonical.kasiva.i18n.*
import java.io.File
import java.io.FileOutputStream
import org.junit.Rule
import org.junit.Test

/** Isolated fixture screen: does not open accounts or request live market data. */
class MarketPriceDeviceTest {
    @get:Rule val ui = createComposeRule()

    @Test fun assetAndExchangeLogosRenderWithoutSourceFooter() {
        val state = MarketState("IDR").apply {
            quotes = marketWatchlist.mapIndexed { index, asset ->
                MarketQuote(asset, "IDR", 100_000.0 + index, 1.25,
                    listOf(98_000.0, 99_000.0, 98_500.0, 100_000.0))
            }
            loading = false
            failed = false
        }
        ui.setContent {
            EntryTheme {
                LazyColumn(Modifier.testTag("market-test-list"), contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    marketPriceItems(state) {}
                }
            }
        }
        val list = ui.onNodeWithTag("market-test-list")
        list.performScrollToNode(hasTestTag("market-card-BBCA"))
        ui.onNodeWithContentDescription("Bank Central Asia", useUnmergedTree = true).assertIsDisplayed()
        ui.onAllNodesWithContentDescription("IDX", useUnmergedTree = true).onFirst().assertIsDisplayed()
        screenshot("market-idx")

        list.performScrollToNode(hasTestTag("market-card-AAPL"))
        ui.onNodeWithContentDescription("Apple", useUnmergedTree = true).assertIsDisplayed()
        ui.onAllNodesWithContentDescription("NASDAQ", useUnmergedTree = true).onFirst().assertIsDisplayed()
        screenshot("market-us")

        list.performScrollToNode(hasTestTag("market-row-XAU"))
        ui.onNodeWithContentDescription("Gold", useUnmergedTree = true).assertIsDisplayed()
        screenshot("market-metals")

        list.performScrollToNode(hasTestTag("market-row-BTC"))
        ui.onNodeWithContentDescription("Bitcoin", useUnmergedTree = true).assertIsDisplayed()
        screenshot("market-crypto")

        list.performScrollToNode(hasTestTag("market-row-LINK"))
        ui.onNodeWithTag("market-source").assertDoesNotExist()
        ui.onNodeWithText(MR.strings.inv_mkt_source.text("IDR")).assertDoesNotExist()
    }

    private fun screenshot(name: String) {
        ui.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
            ?.let { File(it, "market-qa") }
            ?: checkNotNull(instrumentation.targetContext.getExternalFilesDir("market-qa"))
        directory.mkdirs()
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        FileOutputStream(File(directory, "$name.png")).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
