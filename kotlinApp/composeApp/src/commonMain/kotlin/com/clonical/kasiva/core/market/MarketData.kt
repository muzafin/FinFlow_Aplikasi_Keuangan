package com.clonical.kasiva.core.market

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/*
 * Data pasar dari API publik gratis tanpa kunci:
 *  - Yahoo Finance chart/spark (tidak resmi): saham IDX (.JK), saham AS, kripto (XXX-USD), logam berjangka (GC=F dst).
 *  - fawazahmed0/exchange-api (GitHub, CC0): kurs 200+ mata uang, dengan cermin Cloudflare sebagai cadangan.
 * Harga non-IDX dikonversi ke mata uang dasar pengguna memakai kurs USD dari exchange-api.
 */

enum class MarketGroup { Idx, Us, Metals, Crypto }

/** [unit] "g" untuk logam (harga per gram setelah konversi dari troy ounce). */
data class MarketAsset(val symbol: String, val ticker: String, val name: String, val group: MarketGroup, val exchange: String = "", val unit: String = "")

/** Harga sudah dalam [currency]; [closes] sudah diskalakan ke mata uang yang sama. */
data class MarketQuote(
    val asset: MarketAsset, val currency: String, val price: Double, val changePercent: Double,
    val closes: List<Double> = emptyList(),
)

const val TROY_OUNCE_GRAMS = 31.1034768

val marketWatchlist: List<MarketAsset> = listOf(
    MarketAsset("BBCA.JK", "BBCA", "Bank Central Asia", MarketGroup.Idx, "IDX"),
    MarketAsset("BBRI.JK", "BBRI", "Bank Rakyat Indonesia", MarketGroup.Idx, "IDX"),
    MarketAsset("BMRI.JK", "BMRI", "Bank Mandiri", MarketGroup.Idx, "IDX"),
    MarketAsset("TLKM.JK", "TLKM", "Telkom Indonesia", MarketGroup.Idx, "IDX"),
    MarketAsset("BBNI.JK", "BBNI", "Bank Negara Indonesia", MarketGroup.Idx, "IDX"),
    MarketAsset("ASII.JK", "ASII", "Astra International", MarketGroup.Idx, "IDX"),
    MarketAsset("ICBP.JK", "ICBP", "Indofood CBP", MarketGroup.Idx, "IDX"),
    MarketAsset("INDF.JK", "INDF", "Indofood Sukses Makmur", MarketGroup.Idx, "IDX"),
    MarketAsset("UNTR.JK", "UNTR", "United Tractors", MarketGroup.Idx, "IDX"),
    MarketAsset("KLBF.JK", "KLBF", "Kalbe Farma", MarketGroup.Idx, "IDX"),
    MarketAsset("CPIN.JK", "CPIN", "Charoen Pokphand Indonesia", MarketGroup.Idx, "IDX"),
    MarketAsset("GOTO.JK", "GOTO", "GoTo Gojek Tokopedia", MarketGroup.Idx, "IDX"),
    MarketAsset("AAPL", "AAPL", "Apple", MarketGroup.Us, "NASDAQ"),
    MarketAsset("MSFT", "MSFT", "Microsoft", MarketGroup.Us, "NASDAQ"),
    MarketAsset("GOOGL", "GOOGL", "Alphabet", MarketGroup.Us, "NASDAQ"),
    MarketAsset("AMZN", "AMZN", "Amazon", MarketGroup.Us, "NASDAQ"),
    MarketAsset("NVDA", "NVDA", "NVIDIA", MarketGroup.Us, "NASDAQ"),
    MarketAsset("TSLA", "TSLA", "Tesla", MarketGroup.Us, "NASDAQ"),
    MarketAsset("META", "META", "Meta Platforms", MarketGroup.Us, "NASDAQ"),
    MarketAsset("NFLX", "NFLX", "Netflix", MarketGroup.Us, "NASDAQ"),
    MarketAsset("GC=F", "XAU", "Gold", MarketGroup.Metals, unit = "g"),
    MarketAsset("SI=F", "XAG", "Silver", MarketGroup.Metals, unit = "g"),
    MarketAsset("PL=F", "XPT", "Platinum", MarketGroup.Metals, unit = "g"),
    MarketAsset("PA=F", "XPD", "Palladium", MarketGroup.Metals, unit = "g"),
    MarketAsset("BTC-USD", "BTC", "Bitcoin", MarketGroup.Crypto),
    MarketAsset("ETH-USD", "ETH", "Ethereum", MarketGroup.Crypto),
    MarketAsset("USDT-USD", "USDT", "Tether", MarketGroup.Crypto),
    MarketAsset("BNB-USD", "BNB", "Binance Coin", MarketGroup.Crypto),
    MarketAsset("SOL-USD", "SOL", "Solana", MarketGroup.Crypto),
    MarketAsset("XRP-USD", "XRP", "XRP", MarketGroup.Crypto),
    MarketAsset("DOGE-USD", "DOGE", "Dogecoin", MarketGroup.Crypto),
    MarketAsset("ADA-USD", "ADA", "Cardano", MarketGroup.Crypto),
    MarketAsset("TRX-USD", "TRX", "TRON", MarketGroup.Crypto),
    MarketAsset("DOT-USD", "DOT", "Polkadot", MarketGroup.Crypto),
    MarketAsset("LTC-USD", "LTC", "Litecoin", MarketGroup.Crypto),
    MarketAsset("LINK-USD", "LINK", "Chainlink", MarketGroup.Crypto),
)

/** Rentang grafik detail: label, `range` dan `interval` Yahoo. */
enum class ChartRange(val label: String, val range: String, val interval: String) {
    W1("1W", "5d", "30m"), M1("1M", "1mo", "1d"), M6("6M", "6mo", "1d"), Y1("1Y", "1y", "1d"), Y5("5Y", "5y", "1wk")
}

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

private fun JsonElement?.num(): Double? = runCatching { this?.jsonPrimitive?.doubleOrNull }.getOrNull()?.takeIf { it.isFinite() }

/** Parsing respons `v8/finance/spark`: simbol → (harga terakhir, % 1 hari, daftar penutupan). Pure, agar bisa diuji. */
internal fun parseSpark(body: String): Map<String, Triple<Double, Double, List<Double>>> {
    val root = json.parseToJsonElement(body).jsonObject
    return root.mapNotNull { (symbol, node) ->
        val obj = node as? JsonObject ?: return@mapNotNull null
        val closes = (obj["close"] as? JsonArray).orEmpty().mapNotNull { it.num()?.takeIf { price -> price > 0 } }
        val price = obj["fulldayPrice"].num()?.takeIf { it > 0 } ?: closes.lastOrNull() ?: return@mapNotNull null
        val previous = obj["previousClose"].num()?.takeIf { it > 0 }
            ?: obj["chartPreviousClose"].num()?.takeIf { it > 0 }
        // Intraday candles are minutes apart; daily change must use the previous session close.
        val change = (previous?.let { (price / it - 1) * 100 } ?: obj["fulldayChangePercent"].num())
            ?.takeIf { it.isFinite() } ?: return@mapNotNull null
        symbol to Triple(price, change, closes)
    }.toMap()
}

/** Parsing `v8/finance/chart`: daftar (epochDetik, penutupan) yang valid. */
internal fun parseChart(body: String): List<Pair<Long, Double>> {
    // Simbol tak dikenal: Yahoo mengirim "result": null (JsonNull) — anggap kosong, jangan lempar.
    val result = runCatching { json.parseToJsonElement(body).jsonObject["chart"]?.jsonObject?.get("result")?.jsonArray?.firstOrNull()?.jsonObject }.getOrNull() ?: return emptyList()
    val times = result["timestamp"] as? JsonArray ?: return emptyList()
    val indicators = result["indicators"] as? JsonObject ?: return emptyList()
    val quote = (indicators["quote"] as? JsonArray)?.firstOrNull() as? JsonObject ?: return emptyList()
    val closes = quote["close"] as? JsonArray ?: return emptyList()
    return times.indices.mapNotNull { i ->
        val time = times[i].num()?.takeIf { it > 0 }?.toLong() ?: return@mapNotNull null
        closes.getOrNull(i).num()?.takeIf { it > 0 }?.let { time to it }
    }
}

/** Parsing exchange-api `currencies/{base}.json`: kode HURUF BESAR → jumlah per 1 [base]. */
internal fun parseCurrencyApi(body: String, base: String): Pair<String, Map<String, Double>> {
    val root = json.parseToJsonElement(body).jsonObject
    val date = root["date"]?.jsonPrimitive?.content.orEmpty()
    val rates = root[base.lowercase()]?.jsonObject?.mapNotNull { (code, v) -> v.num()?.takeIf { it > 0 }?.let { code.uppercase() to it } }?.toMap().orEmpty()
    return date to rates
}

/** Faktor konversi 1 unit mata uang kuotasi Yahoo ([quoteCurrency]) ke [base]; logam juga dibagi troy ounce → gram. */
internal fun conversionFactor(asset: MarketAsset, quoteCurrency: String, base: String, usdRates: Map<String, Double>): Double? {
    val fx = when {
        quoteCurrency.equals(base, true) -> 1.0
        quoteCurrency == "USD" -> usdRates[base.uppercase()]
        else -> {
            val perUsdQuote = usdRates[quoteCurrency] ?: return null
            val perUsdBase = usdRates[base.uppercase()] ?: return null
            perUsdBase / perUsdQuote
        }
    }?.takeIf { it.isFinite() && it > 0 } ?: return null
    return if (asset.unit == "g") fx / TROY_OUNCE_GRAMS else fx
}

/** Mata uang kuotasi Yahoo per kelompok (IDX dalam IDR, sisanya USD). */
internal fun quoteCurrencyOf(asset: MarketAsset) = if (asset.group == MarketGroup.Idx) "IDR" else "USD"

object MarketData {
    private val client by lazy {
        HttpClient {
            expectSuccess = false
            install(HttpTimeout) { requestTimeoutMillis = 20_000; connectTimeoutMillis = 10_000 }
        }
    }
    private const val YAHOO = "https://query1.finance.yahoo.com"
    private const val YAHOO_BACKUP = "https://query2.finance.yahoo.com"

    internal suspend fun getText(vararg urls: String): String {
        var last: Exception? = null
        for (url in urls) {
            try {
                val response = client.get(url) {
                    header("User-Agent", "Mozilla/5.0 (Linux; Android) Kasiva")
                    header("Cache-Control", "no-cache")
                }
                if (response.status.isSuccess()) return response.bodyAsText()
                last = IllegalStateException("HTTP ${response.status.value}")
            } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { last = e }
        }
        throw last ?: IllegalStateException("no url")
    }

    /** Kurs exchange-api: 1 [base] = rates[X] X. [date] "latest" atau "YYYY-MM-DD". */
    suspend fun currencyRates(base: String, date: String = "latest"): Pair<String, Map<String, Double>> {
        val code = base.lowercase()
        val body = getText(
            "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@$date/v1/currencies/$code.min.json",
            "https://$date.currency-api.pages.dev/v1/currencies/$code.min.json",
        )
        return parseCurrencyApi(body, base)
    }

    private fun spark(symbols: List<String>) = symbols.joinToString(",") { it.replace("=", "%3D") }

    /** Seluruh [marketWatchlist] dalam mata uang [base]. Kelompok yang gagal dimuat dilewati. */
    suspend fun quotes(base: String): List<MarketQuote> = coroutineScope {
        val usd = async {
            try { CurrentCurrencyRates.load("USD").rates }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { emptyMap() }
        }
        val batches = marketWatchlist.chunked(12).map { batch ->
            async {
                try {
                    parseSpark(getText("$YAHOO/v8/finance/spark?symbols=${spark(batch.map { it.symbol })}&range=1d&interval=5m",
                        "$YAHOO_BACKUP/v8/finance/spark?symbols=${spark(batch.map { it.symbol })}&range=1d&interval=5m"))
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { emptyMap() }
            }
        }
        val raw = batches.flatMap { it.await().entries }.associate { it.key to it.value }
        val usdRates = usd.await()
        marketWatchlist.mapNotNull { asset ->
            val (price, change, closes) = raw[asset.symbol] ?: return@mapNotNull null
            val factor = conversionFactor(asset, quoteCurrencyOf(asset), base, usdRates) ?: return@mapNotNull null
            val converted = (price * factor).takeIf { it.isFinite() && it > 0 } ?: return@mapNotNull null
            MarketQuote(asset, base, converted, change, closes.map { it * factor }.filter { it.isFinite() && it > 0 })
        }
    }

    /** Riwayat harga untuk grafik detail, diskalakan ke [base] dengan kurs terkini. */
    suspend fun history(asset: MarketAsset, range: ChartRange, base: String): List<Pair<Long, Double>> {
        val symbol = asset.symbol.replace("=", "%3D")
        val points = parseChart(getText("$YAHOO/v8/finance/chart/$symbol?range=${range.range}&interval=${range.interval}",
            "$YAHOO_BACKUP/v8/finance/chart/$symbol?range=${range.range}&interval=${range.interval}"))
        val quote = quoteCurrencyOf(asset)
        val factor = if (quote.equals(base, true) && asset.unit.isEmpty()) 1.0
            else conversionFactor(asset, quote, base, CurrentCurrencyRates.load("USD").rates) ?: error("fx")
        return points.mapNotNull { (time, price) -> (price * factor).takeIf { it.isFinite() && it > 0 }?.let { time to it } }
    }
}
