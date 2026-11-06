package com.logikawan.finflow.core.model

import androidx.compose.runtime.*
import com.logikawan.finflow.core.platform.NativeServices
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

val financeMenus = listOf("Anggaran", "Berulang", "Target", "Tagihan", "Utang", "Keinginan", "Kartu", "Catatan", "Reimburse", "Investasi")
val statisticsSections = listOf("comparison", "trend", "cycle", "categories", "transactions")

@Serializable
data class AppPreferences(
    val theme: String = "system", val palette: String = "neutral", val cardAppearance: String = "gradient",
    val menuAppearance: String = "horizontal", val menuOrder: List<String> = financeMenus, val hiddenMenus: List<String> = emptyList(),
    val statisticsOrder: List<String> = statisticsSections, val homePeriod: String = "Bulan Ini",
    val transactionDisplay: String = "comfortable", val iconPack: String = "outlined",
    val incomeColor: Long = 0xFF4CAF50, val expenseColor: Long = 0xFFFF5252,
    val showDecimals: Boolean = true, val showCurrencySymbol: Boolean = true, val hideBalances: Boolean = false,
    val numberFormat: String = "id", val recordingDay: Int = 1, val weekStart: Int = 1,
    val defaultWalletId: String = "", val rememberCategory: Boolean = true,
    val lastExpenseCategory: String = "", val lastIncomeCategory: String = "",
    val language: String = "id", val cardGradient: String = "teal", val menuTheme: String = "colorful", val addButtonMode: String = "all",
) {
    fun validate() {
        require(theme in listOf("system","light","dark") && palette in listOf("neutral","arctic","ruby","emerald","indigo"))
        require(cardAppearance in listOf("gradient","plain","outline") && menuAppearance in listOf("horizontal","grid","list"))
        require(menuOrder.size == financeMenus.size && menuOrder.toSet() == financeMenus.toSet())
        require(hiddenMenus.distinct().size == hiddenMenus.size && hiddenMenus.all { it in financeMenus } && hiddenMenus.size < financeMenus.size)
        require(statisticsOrder.size == statisticsSections.size && statisticsOrder.toSet() == statisticsSections.toSet())
        require(homePeriod in listOf("Hari Ini","Minggu Ini","Bulan Ini","Tahun","Kalender"))
        require(transactionDisplay in listOf("comfortable","compact") && iconPack in listOf("outlined","rounded"))
        require(numberFormat in listOf("id","en") && recordingDay in 1..31 && weekStart in 1..7)
        require(lastExpenseCategory.length<=1000 && lastIncomeCategory.length<=1000)
        require(incomeColor in 0xFF000000..0xFFFFFFFF && expenseColor in 0xFF000000..0xFFFFFFFF && defaultWalletId.length <= 1000)
        require(language in listOf("id","en") && addButtonMode in listOf("quick","all"))
        require(cardGradient in cardGradientPresets.map{it.id} && menuTheme in menuThemePresets.map{it.id})
    }
}

class PreferenceState(private val native: NativeServices, userId: String?) {
    private val key = "appearance.${userId ?: "guest"}"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    var value by mutableStateOf(runCatching { json.decodeFromString<AppPreferences>(native.readSetting(key) ?: "{}").also { it.validate() } }.getOrDefault(AppPreferences()))
        private set
    private val pending = MutableStateFlow(if (native.readSetting("$key.pending") == "true") value else null)
    val pendingChanges = pending.asStateFlow()
    fun update(value: AppPreferences) {
        value.validate()
        native.writeSetting("$key.pending", "true")
        native.writeSetting(key, json.encodeToString(value))
        this.value = value
        pending.value = value
    }
    fun acceptRemote(value: AppPreferences) {
        value.validate()
        if (pending.value != null) return
        native.writeSetting(key, json.encodeToString(value))
        this.value = value
    }
    fun acknowledge(saved: AppPreferences) {
        if (pending.value != saved) return
        native.writeSetting("$key.pending", "")
        pending.value = null
    }
}

val LocalPreferences = staticCompositionLocalOf { AppPreferences() }
val LocalPreferenceState = staticCompositionLocalOf<PreferenceState?> { null }
