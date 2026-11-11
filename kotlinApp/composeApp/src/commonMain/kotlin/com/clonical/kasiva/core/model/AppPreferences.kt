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

    /**
     * Data dari perangkat/build lain bisa memuat nilai yang tidak lagi valid. Perbaiki sebisanya
     * daripada menolak seluruh preferensi: pengaturan yang masih dikenal dipertahankan, sisanya kembali ke bawaan.
     */
    fun repaired(): AppPreferences {
        val group = listOf("cash", "bank", "ewallet", "savings", "credit", "investment", "loan", "other")
        fun ordered(order: List<String>, known: List<String>) = (order.filter { it in known } + known).distinct()
        fun validColor(value: Long) = value in 0xFF000000..0xFFFFFFFF
        return copy(
            theme = theme.takeIf { it in listOf("system", "light", "dark") } ?: "system",
            palette = palette.takeIf { it in listOf("neutral", "arctic", "ruby", "emerald", "indigo") } ?: "neutral",
            cardAppearance = cardAppearance.takeIf { it in listOf("gradient", "plain", "outline") } ?: "gradient",
            menuAppearance = menuAppearance.takeIf { it in listOf("horizontal", "grid", "list") } ?: "horizontal",
            menuOrder = ordered(menuOrder, financeMenus),
            hiddenMenus = hiddenMenus.filter { it in financeMenus }.distinct().take(financeMenus.size - 1),
            statisticsOrder = ordered(statisticsOrder, statisticsSections),
            homePeriod = homePeriod.takeIf { it in listOf("Hari Ini", "Minggu Ini", "Bulan Ini", "Tahun", "Kalender") } ?: "Bulan Ini",
            transactionDisplay = transactionDisplay.takeIf { it in listOf("comfortable", "compact") } ?: "comfortable",
            iconPack = iconPack.takeIf { it in listOf("outlined", "rounded") } ?: "outlined",
            incomeColor = incomeColor.takeIf(::validColor) ?: 0xFF4CAF50,
            expenseColor = expenseColor.takeIf(::validColor) ?: 0xFFFF5252,
            numberFormat = numberFormat.takeIf { it in listOf("id", "en") } ?: "id",
            recordingDay = recordingDay.coerceIn(1, 31),
            weekStart = weekStart.coerceIn(1, 7),
            defaultWalletId = defaultWalletId.take(1000),
            lastExpenseCategory = lastExpenseCategory.take(1000),
            lastIncomeCategory = lastIncomeCategory.take(1000),
            language = language.takeIf { it == "system" || com.clonical.kasiva.core.i18n.appLanguages.any { l -> l.tag == it } } ?: "system",
            cardGradient = cardGradient.takeIf { isValidCardGradient(it) } ?: "teal",
            menuTheme = menuTheme.takeIf { isValidMenuTheme(it) } ?: "colorful",
            addButtonMode = addButtonMode.takeIf { it in listOf("quick", "all") } ?: "all",
            defaultTransactionCurrency = defaultTransactionCurrency.takeIf { it.isEmpty() || it.matches(Regex("[A-Z]{3}")) } ?: "",
            transferColor = transferColor.takeIf(::validColor) ?: 0xFF2196F3,
            maskCharacter = maskCharacter.takeIf { it in maskCharacters } ?: "•",
            datePickerStyle = datePickerStyle.takeIf { it in listOf("calendar", "wheel") } ?: "calendar",
            dashboardWalletIds = dashboardWalletIds.filter { it.length in 1..100 }.distinct().take(50),
            dashboardWalletCount = dashboardWalletCount.coerceIn(0, 20),
            walletPickerLayout = walletPickerLayout.takeIf { it in listOf("grid", "list") } ?: "grid",
            walletPickerOrder = walletPickerOrder.takeIf { it in listOf("my_wallets", "most_used", "name", "balance") } ?: "my_wallets",
            walletGroupOrder = walletGroupOrder.takeIf { it.size == 8 && it.toSet() == group.toSet() } ?: group,
            walletOrder = walletOrder.filter { it.length in 1..100 }.distinct().take(500),
            categoryOrder = categoryOrder.filter { it.length in 1..100 }.distinct().take(1000),
            voiceLanguage = voiceLanguage.takeIf { it == "system" || it.matches(Regex("[a-z]{2,3}-[A-Z]{2}")) } ?: "system",
        )
    }
}

class PreferenceState(private val native: NativeServices, userId: String?) {
    private val key = "appearance.${userId ?: "guest"}"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    // Satu nilai yang tidak valid lagi (mis. setelah pembaruan) tidak boleh mereset SEMUA preferensi; perbaiki dulu (P1-07).
    var value by mutableStateOf(runCatching { json.decodeFromString<AppPreferences>(native.readSetting(key) ?: "{}").let { d -> runCatching { d.also { it.validate() } }.getOrElse { d.repaired().also { it.validate() } } } }.getOrDefault(AppPreferences()))
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
