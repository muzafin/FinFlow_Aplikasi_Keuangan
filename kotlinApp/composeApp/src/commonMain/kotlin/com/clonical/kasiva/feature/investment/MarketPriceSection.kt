package com.clonical.kasiva.feature.investment

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.market.ChartRange
import com.clonical.kasiva.core.market.MarketAsset
import com.clonical.kasiva.core.market.MarketData
import com.clonical.kasiva.core.market.MarketGroup
import com.clonical.kasiva.core.market.MarketQuote
import com.clonical.kasiva.core.market.marketWatchlist
import com.clonical.kasiva.core.ui.FFColors
import com.clonical.kasiva.core.ui.FFIconButton
import com.clonical.kasiva.core.ui.FFType
import com.clonical.kasiva.core.util.currencyDigits
import com.clonical.kasiva.core.util.displayMoney
import com.clonical.kasiva.i18n.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong
import com.clonical.kasiva.core.util.nowMillis

/** Cache for one refresh interval when reopening the screen. */
internal object MarketCache {
    var base = ""; var at = 0L; var quotes: List<MarketQuote> = emptyList()
    var failed = false
    fun fresh(b: String) = b == base && quotes.isNotEmpty() && nowMillis() - at in 0 until 60_000
    fun put(b: String, q: List<MarketQuote>, incomplete: Boolean = false) { base = b; quotes = q; failed = incomplete; at = nowMillis() }
}

class MarketState(val base: String) {
    var quotes by mutableStateOf(if (MarketCache.base == base) MarketCache.quotes else emptyList())
    var loading by mutableStateOf(false)
    var failed by mutableStateOf(MarketCache.base == base && MarketCache.failed)
    var selected by mutableStateOf<MarketQuote?>(null)

    suspend fun load(force: Boolean = false) {
        if (loading) return
        if (!force && MarketCache.fresh(base)) { quotes = MarketCache.quotes; failed = MarketCache.failed; return }
        loading = true; failed = false
        try {
            val result = MarketData.quotes(base)
            if (result.isEmpty()) failed = true else {
                // A partial response must not remove previously loaded asset groups.
                quotes = (quotes + result).associateBy { it.asset.symbol }.values.toList()
                failed = result.size < marketWatchlist.size
                selected = selected?.let { old -> quotes.firstOrNull { it.asset.symbol == old.asset.symbol } ?: old }
                MarketCache.put(base, quotes, failed)
            }
        } catch (e: CancellationException) { throw e } catch (e: Exception) { failed = true }
        finally {
            if (MarketCache.base == base) MarketCache.failed = failed
            loading = false
        }
    }
}

@Composable
fun rememberMarketState(base: String): MarketState {
    val state = remember(base) { MarketState(base) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(state, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            state.load()
            while (isActive) { delay(60_000); state.load(force = true) }
        }
    }
    return state
}

/** Harga desimal → satuan minor mata uang, lalu format tampilan aplikasi. */
internal fun marketMoney(value: Double, currency: String): String =
    displayMoney((value * 10.0.pow(currencyDigits(currency))).roundToLong(), currency)

private fun percentLabel(change: Double): String {
    val rounded = (abs(change) * 100).roundToLong()
    return "${rounded / 100}.${(rounded % 100).toString().padStart(2, '0')}%"
}

@Composable private fun changeColor(change: Double) = if (change < 0) FFColors.expense else FFColors.income

private fun metalName(asset: MarketAsset): String = when (asset.ticker) {
    "XAU" -> MR.strings.inv_mkt_gold.text()
    "XAG" -> MR.strings.inv_mkt_silver.text()
    "XPT" -> MR.strings.inv_mkt_platinum.text()
    "XPD" -> MR.strings.inv_mkt_palladium.text()
    else -> asset.name
}

internal fun displayName(asset: MarketAsset) = if (asset.group == MarketGroup.Metals) metalName(asset) else asset.name

/** Seksi "Harga Pasar" gaya Ollo Asset Management, ditambahkan ke LazyColumn layar Investasi. */
fun LazyListScope.marketPriceItems(state: MarketState, refresh: () -> Unit) {
    item("market-header") {
        Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(MR.strings.inv_mkt_title.text(), Modifier.weight(1f), style = FFType.sectionTitle)
                if (state.loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else FFIconButton("hi_recurring", refresh, Modifier.testTag("market-refresh"))
            }
            Text(MR.strings.inv_mkt_note.text(), style = FFType.metaData, color = FFColors.onSurfaceVariant)
            if (state.failed && state.quotes.isNotEmpty()) {
                Text(MR.strings.inv_mkt_error.text(), style = FFType.metaData, color = FFColors.expense)
            }
        }
    }
    if (state.quotes.isEmpty()) {
        item("market-status") {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(FFColors.surface).padding(20.dp), contentAlignment = Alignment.Center) {
                if (state.failed) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(MR.strings.inv_mkt_error.text(), style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
                    TextButton(onClick = refresh, modifier = Modifier.testTag("market-retry")) { Text(MR.strings.common_retry.text()) }
                } else CircularProgressIndicator(Modifier.size(28.dp))
            }
        }
        return
    }
    val byGroup = state.quotes.groupBy { it.asset.group }
    listOf(MarketGroup.Idx to MR.strings.inv_mkt_idx, MarketGroup.Us to MR.strings.inv_mkt_us).forEach { (group, title) ->
        val quotes = byGroup[group].orEmpty()
        if (quotes.isEmpty()) return@forEach
        item("market-title-$group") { Text(title.text(), Modifier.padding(top = 8.dp), style = FFType.bodyMain.copy(fontSize = 17.sp), fontWeight = FontWeight.SemiBold) }
        quotes.chunked(2).forEachIndexed { index, pair ->
            item("market-$group-$index") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { quote -> StockCard(quote, Modifier.weight(1f)) { state.selected = quote } }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
    listOf(MarketGroup.Metals to MR.strings.inv_mkt_metals, MarketGroup.Crypto to MR.strings.inv_mkt_crypto).forEach { (group, title) ->
        val quotes = byGroup[group].orEmpty()
        if (quotes.isEmpty()) return@forEach
        item("market-title-$group") { Text(title.text(), Modifier.padding(top = 8.dp), style = FFType.bodyMain.copy(fontSize = 17.sp), fontWeight = FontWeight.SemiBold) }
        quotes.forEach { quote -> item("market-row-${quote.asset.symbol}") { AssetRow(quote) { state.selected = quote } } }
    }
}

@Composable
private fun StockCard(quote: MarketQuote, modifier: Modifier, onClick: () -> Unit) {
    val color = changeColor(quote.changePercent)
    Column(modifier.clip(RoundedCornerShape(22.dp)).background(FFColors.surface).clickable(onClick = onClick).padding(14.dp).testTag("market-card-${quote.asset.ticker}")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MarketAssetIcon(quote.asset)
            Spacer(Modifier.width(10.dp))
            Sparkline(quote.closes, FFColors.onSurfaceVariant, Modifier.weight(1f).height(34.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(quote.asset.ticker, style = FFType.bodyMain.copy(fontSize = 17.sp), fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(quote.asset.name, style = FFType.metaData, color = FFColors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(10.dp))
        Text(marketMoney(quote.price, quote.currency), style = FFType.bodyMain.copy(fontSize = 15.sp), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(MR.strings.inv_mkt_last_price.text(), Modifier.weight(1f), style = FFType.metaData.copy(fontSize = 11.sp), color = FFColors.onSurfaceVariant, maxLines = 1)
            Text((if (quote.changePercent < 0) "▼ " else "▲ ") + percentLabel(quote.changePercent), style = FFType.metaData.copy(fontSize = 11.sp), color = color, maxLines = 1)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ExchangeLogo(quote.asset.exchange, 16.dp)
            Text(quote.asset.exchange + " · 1D", style = FFType.metaData.copy(fontSize = 10.sp), color = FFColors.onSurfaceVariant)
        }
    }
}

@Composable
private fun AssetRow(quote: MarketQuote, onClick: () -> Unit) {
    val color = changeColor(quote.changePercent)
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(FFColors.surface).clickable(onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 14.dp).testTag("market-row-${quote.asset.ticker}"), verticalAlignment = Alignment.CenterVertically) {
        MarketAssetIcon(quote.asset, 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(displayName(quote.asset), style = FFType.bodyMain.copy(fontSize = 16.sp), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(quote.asset.ticker, style = FFType.metaData, color = FFColors.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(marketMoney(quote.price, quote.currency) + if (quote.asset.unit.isNotEmpty()) " / ${quote.asset.unit}" else "",
                style = FFType.bodyMain.copy(fontSize = 15.sp), fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text((if (quote.changePercent < 0) "▼ " else "▲ ") + percentLabel(quote.changePercent) + " 1D", style = FFType.metaData, color = color)
        }
    }
}

@Composable
fun Sparkline(values: List<Double>, color: Color, modifier: Modifier, fill: Boolean = false) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val min = values.min(); val max = values.max(); val span = (max - min).takeIf { it > 0 } ?: 1.0
        val stepX = size.width / (values.size - 1)
        fun point(i: Int) = Offset(i * stepX, (size.height - ((values[i] - min) / span * size.height)).toFloat())
        val line = Path().apply { moveTo(point(0).x, point(0).y); for (i in 1 until values.size) lineTo(point(i).x, point(i).y) }
        if (fill) {
            val area = Path().apply { addPath(line); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
            drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = .28f), Color.Transparent)))
        }
        drawPath(line, color, style = Stroke(width = (if (fill) 2.5f else 1.6f) * density, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Detail harga: grafik besar dengan pilihan rentang 1W/1M/6M/1Y/5Y. */
@Composable
fun MarketDetailDialog(quote: MarketQuote, dismiss: () -> Unit) {
    var range by remember { mutableStateOf(ChartRange.M1) }
    var points by remember { mutableStateOf<List<Pair<Long, Double>>?>(null) }
    var failed by remember { mutableStateOf(false) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(quote.asset.symbol, quote.currency, range, lifecycle) {
        points = null; failed = false
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                try {
                    val loaded = MarketData.history(quote.asset, range, quote.currency)
                    if (loaded.size >= 2) points = loaded
                    failed = loaded.size < 2
                } catch (e: CancellationException) { throw e } catch (e: Exception) { failed = true }
                delay(60_000)
            }
        }
    }
    val values = points?.map { it.second }.orEmpty()
    val first = values.firstOrNull(); val last = values.lastOrNull()
    val rangeChange = if (first != null && last != null && first > 0) (last / first - 1) * 100 else quote.changePercent
    val color = changeColor(rangeChange)
    Dialog(onDismissRequest = dismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(28.dp)).background(FFColors.surface).padding(20.dp).testTag("market-detail")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MarketAssetIcon(quote.asset, 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(displayName(quote.asset), style = FFType.bodyMain.copy(fontSize = 17.sp), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(listOf(quote.asset.ticker, quote.asset.exchange).filter { it.isNotEmpty() }.joinToString(" · "), style = FFType.metaData, color = FFColors.onSurfaceVariant)
                }
                FFIconButton("close_rounded", dismiss, Modifier.testTag("market-detail-close"))
            }
            Spacer(Modifier.height(16.dp))
            Text(marketMoney(quote.price, quote.currency) + if (quote.asset.unit.isNotEmpty()) " / ${quote.asset.unit}" else "",
                style = FFType.amountLg.copy(fontSize = 28.sp, lineHeight = 34.sp), color = FFColors.onSurface)
            Text((if (rangeChange < 0) "▼ " else "▲ ") + percentLabel(rangeChange) + " · " + range.label, style = FFType.bodyMain, color = color)
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                when {
                    failed -> Text(MR.strings.inv_mkt_chart_error.text(), style = FFType.metaData, color = FFColors.onSurfaceVariant)
                    points == null -> CircularProgressIndicator(Modifier.size(28.dp))
                    else -> Sparkline(values, color, Modifier.fillMaxSize().testTag("market-chart"), fill = true)
                }
            }
            if (values.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(MR.strings.inv_mkt_high_low.text(marketMoney(values.max(), quote.currency), marketMoney(values.min(), quote.currency)),
                    style = FFType.metaData, color = FFColors.onSurfaceVariant)
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(FFColors.chip).padding(4.dp)) {
                ChartRange.entries.forEach { option ->
                    val chosen = option == range
                    Box(Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(16.dp)).background(if (chosen) FFColors.pill else Color.Transparent)
                        .clickable { range = option }.testTag("market-range-${option.label}"), contentAlignment = Alignment.Center) {
                        Text(option.label, style = FFType.metaData, fontWeight = FontWeight.SemiBold, color = if (chosen) FFColors.onPill else FFColors.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
