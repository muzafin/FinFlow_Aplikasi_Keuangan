package com.clonical.kasiva.core.market

import com.clonical.kasiva.core.util.nowMillis
import com.clonical.kasiva.core.util.today
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class CurrentRates(val date: String, val rates: Map<String, Double>, val current: Boolean,
    val dailyCodes: Set<String> = emptySet(), val dailyDate: String = "")

internal fun combineCurrentRates(current: Map<String, Double>, daily: Pair<String, Map<String, Double>>?): CurrentRates =
    CurrentRates(today().toString(), daily?.second.orEmpty() + current, current = true,
        dailyCodes = daily?.second.orEmpty().keys - current.keys, dailyDate = daily?.first.orEmpty())

internal fun parseCurrentRates(body: String, base: String): Map<String, Double> {
    val data = Json.parseToJsonElement(body).jsonObject.getValue("data").jsonObject
    require(data["currency"]?.jsonPrimitive?.content.equals(base, ignoreCase = true))
    val rates = data.getValue("rates").jsonObject.mapNotNull { (code, value) ->
        value.jsonPrimitive.doubleOrNull?.takeIf { it.isFinite() && it > 0 }?.let { code.uppercase() to it }
    }.toMap()
    require(rates[base.uppercase()] == 1.0 && rates.size > 1)
    return rates
}

/** Current reference rates; historical/daily data remains available if the current feed is unreachable. */
object CurrentCurrencyRates {
    private val mutex = Mutex()
    private var cachedBase = ""
    private var cachedAt = 0L
    private var cached: CurrentRates? = null
    private var dailyBase = ""
    private var dailyAt = 0L
    private var daily: Pair<String, Map<String, Double>>? = null

    private suspend fun dailyRates(base: String): Pair<String, Map<String, Double>> {
        daily?.takeIf { dailyBase == base && nowMillis() - dailyAt in 0 until 3_600_000 }?.let { return it }
        return MarketData.currencyRates(base).also { dailyBase = base; dailyAt = nowMillis(); daily = it }
    }

    suspend fun load(base: String, force: Boolean = false): CurrentRates = mutex.withLock {
        val code = base.uppercase()
        require(code.length == 3 && code.all { it in 'A'..'Z' })
        cached?.takeIf { !force && cachedBase == code && nowMillis() - cachedAt in 0 until 60_000 }?.let { return@withLock it }
        val result = try {
            val rates = parseCurrentRates(MarketData.getText("https://api.coinbase.com/v2/exchange-rates?currency=$code"), code)
            // Coinbase provides no quote timestamp: this date records retrieval, not a market tick.
            val supplement = try { dailyRates(code) }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { null }
            combineCurrentRates(rates, supplement)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            val (date, rates) = dailyRates(code)
            require(rates.isNotEmpty())
            CurrentRates(date, rates, current = false)
        }
        cachedBase = code
        cachedAt = nowMillis()
        cached = result
        result
    }
}
