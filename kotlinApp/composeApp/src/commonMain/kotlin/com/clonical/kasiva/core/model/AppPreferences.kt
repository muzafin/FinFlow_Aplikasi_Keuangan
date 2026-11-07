package com.clonical.kasiva.core.model

import androidx.compose.runtime.*
import com.clonical.kasiva.core.platform.NativeServices
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
    val language: String = "system", val cardGradient: String = "teal", val menuTheme: String = "colorful", val addButtonMode: String = "all",
    // Pengaturan (Ollo Settings): "" = sama dengan mata uang tampilan (profil).
    val defaultTransactionCurrency: String = "",
    val tapAnimations: Boolean = true, val pageTransitions: Boolean = true, val listAnimations: Boolean = true,
    // Preferensi Ollo (grup C): warna transfer, masker saldo, pemilih tanggal, isi cepat.
    val transferColor: Long = 0xFF2196F3, val maskCharacter: String = "•", val preserveMaskFormat: Boolean = true,
    val datePickerStyle: String = "calendar", val quickFill: Boolean = true,
    // Pengaturan Dompet & Kategori (grup B): kosong/0 = otomatis.
    val dashboardWalletIds: List<String> = emptyList(), val dashboardWalletCount: Int = 0,
    val walletPickerLayout: String = "grid", val walletPickerOrder: String = "my_wallets",
    val walletGroupOrder: List<String> = listOf("cash", "bank", "ewallet", "savings", "credit", "investment", "loan", "other"),
    val walletOrder: List<String> = emptyList(), val categoryOrder: List<String> = emptyList(),
    // Bahasa Input Suara (grup D): "system" = ikut bahasa aplikasi, selain itu tag BCP-47 seperti "id-ID".
    val voiceLanguage: String = "system",
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
        require((language == "system" || com.clonical.kasiva.core.i18n.appLanguages.any { it.tag == language }) && addButtonMode in listOf("quick","all"))
        require(isValidCardGradient(cardGradient) && isValidMenuTheme(menuTheme))
        require(defaultTransactionCurrency.isEmpty() || defaultTransactionCurrency.matches(Regex("[A-Z]{3}")))
        require(transferColor in 0xFF000000..0xFFFFFFFF && maskCharacter in maskCharacters && datePickerStyle in listOf("calendar","wheel"))
        require(dashboardWalletIds.size <= 50 && dashboardWalletIds.distinct().size == dashboardWalletIds.size && dashboardWalletIds.all { it.length in 1..100 } && dashboardWalletCount in 0..20)
        require(walletPickerLayout in listOf("grid","list") && walletPickerOrder in listOf("my_wallets","most_used","name","balance"))
        require(walletGroupOrder.size == 8 && walletGroupOrder.toSet() == setOf("cash","bank","ewallet","savings","credit","investment","loan","other"))
        require(walletOrder.size <= 500 && walletOrder.distinct().size == walletOrder.size && walletOrder.all { it.length in 1..100 })
        require(categoryOrder.size <= 1000 && categoryOrder.distinct().size == categoryOrder.size && categoryOrder.all { it.length in 1..100 })
        require(voiceLanguage == "system" || voiceLanguage.matches(Regex("[a-z]{2,3}-[A-Z]{2}")))
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
