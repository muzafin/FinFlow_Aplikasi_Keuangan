package com.clonical.kasiva.feature.investment

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.clonical.kasiva.core.market.MarketAsset
import com.clonical.kasiva.core.market.MarketGroup
import com.clonical.kasiva.core.ui.FFColors
import com.clonical.kasiva.core.ui.FFIcon
import com.clonical.kasiva.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val marketLogos: Map<String, DrawableResource> = mapOf(
    "BBCA" to Res.drawable.provider_bca,
    "BBRI" to Res.drawable.provider_bri,
    "BMRI" to Res.drawable.provider_mandiri,
    "BBNI" to Res.drawable.provider_bni,
    "TLKM" to Res.drawable.market_tlkm,
    "ASII" to Res.drawable.market_asii,
    "ICBP" to Res.drawable.market_icbp,
    "INDF" to Res.drawable.market_indf,
    "UNTR" to Res.drawable.market_untr,
    "KLBF" to Res.drawable.market_klbf,
    "CPIN" to Res.drawable.market_cpin,
    "GOTO" to Res.drawable.market_goto,
    "AAPL" to Res.drawable.market_aapl,
    "MSFT" to Res.drawable.market_msft,
    "GOOGL" to Res.drawable.market_googl,
    "AMZN" to Res.drawable.market_amzn,
    "NVDA" to Res.drawable.market_nvda,
    "TSLA" to Res.drawable.market_tsla,
    "META" to Res.drawable.market_meta,
    "NFLX" to Res.drawable.market_nflx,
    "BTC" to Res.drawable.market_btc,
    "ETH" to Res.drawable.market_eth,
    "USDT" to Res.drawable.market_usdt,
    "BNB" to Res.drawable.market_bnb,
    "SOL" to Res.drawable.market_sol,
    "XRP" to Res.drawable.market_xrp,
    "DOGE" to Res.drawable.market_doge,
    "ADA" to Res.drawable.market_ada,
    "TRX" to Res.drawable.market_trx,
    "DOT" to Res.drawable.market_dot,
    "LTC" to Res.drawable.market_ltc,
    "LINK" to Res.drawable.market_link,
)

internal fun hasMarketAssetLogo(asset: MarketAsset): Boolean =
    asset.ticker in marketLogos || asset.group == MarketGroup.Metals

/** Bundled logos remain visible offline; provenance is in licenses/MarketLogos.md. */
@Composable
fun MarketAssetIcon(asset: MarketAsset, size: Dp = 40.dp) {
    val logo = marketLogos[asset.ticker]
    when {
        logo != null -> Image(
            painterResource(logo), asset.name,
            Modifier.size(size).clip(CircleShape).background(Color.White).padding(3.dp),
            contentScale = ContentScale.Fit,
        )
        asset.group == MarketGroup.Metals -> MetalIcon(asset, size)
        else -> Box(Modifier.size(size).clip(CircleShape).background(FFColors.iconCircle).padding(size * .22f)) {
            FFIcon("trending_up_rounded", FFColors.onSurfaceVariant, size * .56f)
        }
    }
}

@Composable
private fun MetalIcon(asset: MarketAsset, size: Dp) {
    val color = when (asset.ticker) {
        "XAU" -> Color(0xFFD9A82C)
        "XAG" -> Color(0xFF9EA8B8)
        "XPT" -> Color(0xFF6E9DAB)
        else -> Color(0xFF9384B7)
    }
    Canvas(Modifier.size(size).clip(CircleShape).background(color.copy(alpha = .16f))
        .padding(size * .17f).semantics { contentDescription = asset.name }) {
        fun bar(left: Float, top: Float, width: Float, height: Float) {
            val x = left * this.size.width; val y = top * this.size.height
            val w = width * this.size.width; val h = height * this.size.height
            drawPath(Path().apply {
                moveTo(x + w * .18f, y); lineTo(x + w * .82f, y)
                lineTo(x + w, y + h); lineTo(x, y + h); close()
            }, color)
            drawLine(Color.White.copy(alpha = .6f), androidx.compose.ui.geometry.Offset(x + w * .25f, y + h * .22f),
                androidx.compose.ui.geometry.Offset(x + w * .7f, y + h * .22f), strokeWidth = h * .09f)
        }
        bar(.25f, .12f, .5f, .34f)
        bar(0f, .54f, .47f, .34f)
        bar(.53f, .54f, .47f, .34f)
    }
}

@Composable
fun ExchangeLogo(exchange: String, size: Dp = 24.dp) {
    val logo = when (exchange) {
        "IDX" -> Res.drawable.market_exchange_idx
        "NASDAQ" -> Res.drawable.market_exchange_nasdaq
        else -> return
    }
    Image(painterResource(logo), exchange,
        Modifier.size(size).clip(CircleShape).background(Color.White).padding(2.dp),
        contentScale = ContentScale.Fit)
}
