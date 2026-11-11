package com.clonical.kasiva.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.clonical.kasiva.core.market.CurrentCurrencyRates
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.model.FinanceData
import com.clonical.kasiva.core.model.LocalPreferences
import com.clonical.kasiva.core.ui.*
import com.clonical.kasiva.core.util.currencyDigits
import com.clonical.kasiva.core.util.formatBalance
import com.clonical.kasiva.core.util.shortDateLabel
import com.clonical.kasiva.i18n.*
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.functions.functions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.serialization.Serializable
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.seconds

/** Respons fungsi `exchangeRates`: 1 [base] = rates[X] X (kurs referensi harian ECB). */
@Serializable
data class ExchangeRates(val base: String, val date: String = "", val rates: Map<String, Double> = emptyMap(), val stale: Boolean = false, val source: String = "",
    val dailyCodes: Set<String> = emptySet(), val dailyDate: String = "")

@Serializable private data class RatesRequest(val base: String)

private val majorCurrencies = listOf("USD", "EUR", "SGD", "MYR", "JPY", "GBP", "AUD", "CNY", "HKD", "KRW", "THB", "INR", "SAR", "CHF")

/**
 * Current reference rates with daily CDN and server fallbacks when the current feed is unreachable.
 */
suspend fun loadExchangeRates(base: String, force: Boolean = false): ExchangeRates = try {
    val current = CurrentCurrencyRates.load(base, force)
    ExchangeRates(base, current.date, current.rates.filterKeys { it.length == 3 && it != base.uppercase() },
        stale = !current.current, source = if (current.current) "Coinbase" else "fawazahmed0/exchange-api",
        dailyCodes = current.dailyCodes, dailyDate = current.dailyDate)
} catch (e: CancellationException) { throw e } catch (e: Exception) {
    Firebase.functions("asia-southeast2").httpsCallable("exchangeRates", 30.seconds)(RatesRequest(base)).data<ExchangeRates>().copy(stale = true)
}

/** Nilai 1 [code] dalam satuan minor mata uang [base]. */
internal fun oneUnitInBase(rate: Double, base: String): Long? =
    if (!rate.isFinite() || rate <= 0.0) null else {
        val minor = (1.0 / rate) * 10.0.pow(currencyDigits(base))
        minor.takeIf { it.isFinite() && it < Long.MAX_VALUE.toDouble() }?.roundToLong()
    }

/** Reference conversion rates; values are never written to wallet balances. */
@Composable
fun ExchangeRatesScreen(data: FinanceData) {
    val preferences = LocalPreferences.current
    val base = data.profile?.baseCurrency ?: "IDR"
    val scope = rememberCoroutineScope()
    var rates by remember(base) { mutableStateOf<ExchangeRates?>(null) }
    var error by remember(base) { mutableStateOf<String?>(null) }
    var loading by remember(base) { mutableStateOf(true) }
    suspend fun refresh(force: Boolean = false) {
        loading = true; error = null
        try { rates = loadExchangeRates(base, force) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = MR.strings.pf_rates_error.text() }
        finally { loading = false }
    }
    fun manualRefresh() { if (!loading) scope.launch { refresh(force = true) } }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(base, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            refresh()
            while (isActive) { delay(60_000); if (!loading) refresh(force = true) }
        }
    }
    val walletCurrencies = data.accounts.filter { !it.archived }.map { it.currency }.filter { it != base }.distinct()
    var query by remember { mutableStateOf("") }
    // Semua mata uang negara: dompet dulu, lalu mata uang utama, sisanya alfabetis.
    val allCodes = (walletCurrencies + majorCurrencies + com.clonical.kasiva.feature.auth.allCurrencyCodes()).filter { it != base }.distinct()
    val codes = query.trim().let { q -> if (q.isEmpty()) allCodes else allCodes.filter { it.contains(q, true) || com.clonical.kasiva.feature.auth.currencyDisplayName(it).contains(q, true) } }
    LazyColumn(Modifier.fillMaxSize().testTag("exchange-rates"), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(MR.strings.pf_rates_intro.text(base), style = FFType.metaData, color = FFColors.onSurfaceVariant)
        }
        val current = rates
        when {
            loading && current == null -> item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            current == null -> item {
                DesignCard {
                    Text(error ?: MR.strings.pf_rates_error.text(), style = FFType.bodyMain, color = FFColors.onSurface)
                    TextButton(onClick = ::manualRefresh, modifier = Modifier.testTag("exchange-rates-retry")) { Text(MR.strings.common_retry.text()) }
                }
            }
            else -> {
                item {
                    Field(MR.strings.auth_currency_search.text(), query, { query = it }, Modifier.testTag("exchange-rates-search"))
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(MR.strings.pf_rates_updated.text(shortDateLabel(current.date)) + if (current.stale) " · " + MR.strings.pf_rates_stale.text() else "",
                            Modifier.weight(1f), style = FFType.metaData, color = FFColors.onSurfaceVariant)
                        TextButton(onClick = ::manualRefresh, enabled = !loading, modifier = Modifier.testTag("exchange-rates-refresh")) { Text(MR.strings.pf_rates_refresh.text()) }
                    }
                    if (error != null) Text(error!!, style = FFType.metaData, color = FFColors.expense)
                }
                items(codes, key = { it }) { code ->
                    val value = current.rates[code]?.let { oneUnitInBase(it, base) }
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(FFColors.surfaceLow).padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("rate-$code"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(FFColors.iconCircle), contentAlignment = Alignment.Center) {
                            Text(code, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FFColors.onSurface)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("1 $code", style = FFType.bodyMain, fontWeight = FontWeight.SemiBold, color = FFColors.onSurface)
                            Text(com.clonical.kasiva.feature.auth.currencyDisplayName(code), style = FFType.metaData, color = FFColors.onSurfaceVariant, maxLines = 1)
                            if (code in walletCurrencies) Text(MR.strings.pf_rates_used_by_wallet.text(), style = FFType.metaData, color = FFColors.onSurfaceVariant)
                            if (code in current.dailyCodes) Text(shortDateLabel(current.dailyDate) + " · " + MR.strings.pf_rates_stale.text(),
                                style = FFType.metaData, color = FFColors.onSurfaceVariant)
                        }
                        Text(value?.let { formatBalance(it, base, preferences.copy(hideBalances = false)) } ?: MR.strings.pf_rates_unavailable.text(),
                            style = FFType.bodyMain, fontWeight = FontWeight.SemiBold, color = FFColors.onSurface)
                    }
                }
            }
        }
    }
}
