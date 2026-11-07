package com.logikawan.finflow.feature.settings

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
import com.logikawan.finflow.core.i18n.text
import com.logikawan.finflow.core.model.FinanceData
import com.logikawan.finflow.core.model.LocalPreferences
import com.logikawan.finflow.core.ui.*
import com.logikawan.finflow.core.util.currencyDigits
import com.logikawan.finflow.core.util.formatBalance
import com.logikawan.finflow.core.util.shortDateLabel
import com.logikawan.finflow.i18n.*
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.functions.FirebaseFunctionsException
import dev.gitlive.firebase.functions.functions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.seconds

/** Respons fungsi `exchangeRates`: 1 [base] = rates[X] X (kurs referensi harian ECB). */
@Serializable
data class ExchangeRates(val base: String, val date: String = "", val rates: Map<String, Double> = emptyMap(), val stale: Boolean = false, val source: String = "")

@Serializable private data class RatesRequest(val base: String)

private val majorCurrencies = listOf("USD", "EUR", "SGD", "MYR", "JPY", "GBP", "AUD", "CNY", "HKD", "KRW", "THB", "INR", "SAR", "CHF")

suspend fun loadExchangeRates(base: String): ExchangeRates =
    Firebase.functions("asia-southeast2").httpsCallable("exchangeRates", 30.seconds)(RatesRequest(base)).data<ExchangeRates>()

/** Nilai 1 [code] dalam satuan minor mata uang [base]. */
internal fun oneUnitInBase(rate: Double, base: String): Long? =
    if (rate <= 0.0) null else ((1.0 / rate) * 10.0.pow(currencyDigits(base))).roundToLong()

/** Halaman Kurs (Ollo: Settings › Exchange Rates). Data dari Cloud Function, bukan angka buatan. */
@Composable
fun ExchangeRatesScreen(data: FinanceData) {
    val preferences = LocalPreferences.current
    val base = data.profile?.baseCurrency ?: "IDR"
    val scope = rememberCoroutineScope()
    var rates by remember(base) { mutableStateOf<ExchangeRates?>(null) }
    var error by remember(base) { mutableStateOf<String?>(null) }
    var loading by remember(base) { mutableStateOf(true) }
    fun refresh() {
        loading = true; error = null
        scope.launch {
            try { rates = loadExchangeRates(base) }
            catch (e: CancellationException) { throw e }
            catch (e: FirebaseFunctionsException) { error = e.message?.takeIf { it.isNotBlank() } ?: MR.strings.pf_rates_error.text() }
            catch (e: Exception) { error = MR.strings.pf_rates_error.text() }
            finally { loading = false }
        }
    }
    LaunchedEffect(base) { refresh() }
    val walletCurrencies = data.accounts.filter { !it.archived }.map { it.currency }.filter { it != base }.distinct()
    val codes = (walletCurrencies + majorCurrencies).filter { it != base }.distinct()
    LazyColumn(Modifier.fillMaxSize().testTag("exchange-rates"), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(MR.strings.pf_rates_intro.text(base), style = FFType.metaData, color = FFColors.onSurfaceVariant)
        }
        val current = rates
        when {
            loading && current == null -> item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp) }
            }
            current == null -> item {
                DesignCard {
                    Text(error ?: MR.strings.pf_rates_error.text(), style = FFType.bodyMain, color = FFColors.onSurface)
                    TextButton(onClick = ::refresh, modifier = Modifier.testTag("exchange-rates-retry")) { Text(MR.strings.common_retry.text()) }
                }
            }
            else -> {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(MR.strings.pf_rates_updated.text(shortDateLabel(current.date)) + if (current.stale) " · " + MR.strings.pf_rates_stale.text() else "",
                            Modifier.weight(1f), style = FFType.metaData, color = FFColors.onSurfaceVariant)
                        TextButton(onClick = ::refresh, enabled = !loading, modifier = Modifier.testTag("exchange-rates-refresh")) { Text(MR.strings.pf_rates_refresh.text()) }
                    }
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
                            if (code in walletCurrencies) Text(MR.strings.pf_rates_used_by_wallet.text(), style = FFType.metaData, color = FFColors.onSurfaceVariant)
                        }
                        Text(value?.let { formatBalance(it, base, preferences.copy(hideBalances = false)) } ?: MR.strings.pf_rates_unavailable.text(),
                            style = FFType.bodyMain, fontWeight = FontWeight.SemiBold, color = FFColors.onSurface)
                    }
                }
                item { Text(MR.strings.pf_rates_source.text(), style = FFType.metaData, color = FFColors.onSurfaceVariant) }
            }
        }
    }
}
