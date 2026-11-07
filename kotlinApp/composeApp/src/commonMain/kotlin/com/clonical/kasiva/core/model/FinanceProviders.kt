package com.clonical.kasiva.core.model

/**
 * Katalog bank dan e-wallet untuk dompet. Disimpan di `Account.icon` sebagai `provider:<id>` sehingga
 * tidak perlu kolom/aturan Firestore baru. Badge memakai warna merek + singkatan; berkas logo resmi
 * Logo resmi ada di composeResources/drawable/provider_<id>.png (lihat core/ui/ProviderBadge.kt).
 */
data class FinanceProvider(val id: String, val name: String, val type: String, val color: Long, val short: String, val dark: Boolean = false)

const val PROVIDER_PREFIX = "provider:"

val financeProviders = listOf(
    // Bank
    FinanceProvider("bca", "BCA", "bank", 0xFF0060AF, "BCA"),
    FinanceProvider("mandiri", "Bank Mandiri", "bank", 0xFF003D79, "MDR"),
    FinanceProvider("bri", "BRI", "bank", 0xFF00529C, "BRI"),
    FinanceProvider("bni", "BNI", "bank", 0xFFF15A23, "BNI"),
    FinanceProvider("bsi", "BSI", "bank", 0xFF00A39D, "BSI"),
    FinanceProvider("btn", "BTN", "bank", 0xFF0055A5, "BTN"),
    FinanceProvider("cimb", "CIMB Niaga", "bank", 0xFF7A0000, "CIMB"),
    FinanceProvider("permata", "Permata", "bank", 0xFF00833E, "PRM"),
    FinanceProvider("danamon", "Danamon", "bank", 0xFFF7A600, "DNM", dark = true),
    FinanceProvider("ocbc", "OCBC", "bank", 0xFFE30613, "OCBC"),
    FinanceProvider("maybank", "Maybank", "bank", 0xFFFFC72C, "MAY", dark = true),
    FinanceProvider("panin", "Panin Bank", "bank", 0xFF003B7A, "PNN"),
    FinanceProvider("btpn", "Jenius / SMBC", "bank", 0xFF00A6E2, "JNS"),
    FinanceProvider("jago", "Bank Jago", "bank", 0xFFFDB813, "JGO", dark = true),
    FinanceProvider("seabank", "SeaBank", "bank", 0xFFFF6600, "SEA"),
    FinanceProvider("blu", "blu by BCA Digital", "bank", 0xFF00B7F1, "blu"),
    FinanceProvider("allo", "Allo Bank", "bank", 0xFF7B2CBF, "ALO"),
    FinanceProvider("neo", "Bank Neo Commerce", "bank", 0xFFFFD100, "NEO", dark = true),
    FinanceProvider("superbank", "Superbank", "bank", 0xFF5B2DFF, "SPR"),
    FinanceProvider("krom", "Krom Bank", "bank", 0xFF111111, "KRM"),
    // E-wallet
    FinanceProvider("gopay", "GoPay", "ewallet", 0xFF00AED6, "GP"),
    FinanceProvider("ovo", "OVO", "ewallet", 0xFF4C3494, "OVO"),
    FinanceProvider("dana", "DANA", "ewallet", 0xFF118EEA, "DN"),
    FinanceProvider("shopeepay", "ShopeePay", "ewallet", 0xFFEE4D2D, "SP"),
    FinanceProvider("linkaja", "LinkAja", "ewallet", 0xFFE82529, "LA"),
    FinanceProvider("flip", "Flip", "ewallet", 0xFFFD6542, "FLP"),
    FinanceProvider("isaku", "i.saku", "ewallet", 0xFF0B5FA5, "iS"),
    FinanceProvider("astrapay", "AstraPay", "ewallet", 0xFF0D47A1, "AP"),
    FinanceProvider("sakuku", "Sakuku", "ewallet", 0xFF2E3192, "SK"),
    FinanceProvider("doku", "DOKU", "ewallet", 0xFFE1251B, "DK"),
    FinanceProvider("paypal", "PayPal", "ewallet", 0xFF003087, "PP"),
)


fun providerOf(icon: String?): FinanceProvider? =
    icon?.takeIf { it.startsWith(PROVIDER_PREFIX) }?.removePrefix(PROVIDER_PREFIX)?.let { id -> financeProviders.firstOrNull { it.id == id } }

/** Nama mata uang (Inggris, seperti Ollo) untuk baris "Mata Uang Dompet"; kode dipakai bila tidak tercatat. */
private val walletCurrencyNames = mapOf(
    "AED" to "UAE Dirham", "AUD" to "Australian Dollar", "BDT" to "Bangladeshi Taka", "BHD" to "Bahraini Dinar",
    "BRL" to "Brazilian Real", "CAD" to "Canadian Dollar", "CHF" to "Swiss Franc", "CNY" to "Chinese Yuan",
    "CZK" to "Czech Koruna", "DKK" to "Danish Krone", "EGP" to "Egyptian Pound", "EUR" to "Euro",
    "GBP" to "British Pound", "HKD" to "Hong Kong Dollar", "HUF" to "Hungarian Forint", "IDR" to "Indonesian Rupiah",
    "ILS" to "Israeli New Shekel", "INR" to "Indian Rupee", "ISK" to "Icelandic Krona", "JPY" to "Japanese Yen",
    "KRW" to "South Korean Won", "KWD" to "Kuwaiti Dinar", "LKR" to "Sri Lankan Rupee", "MAD" to "Moroccan Dirham",
    "MXN" to "Mexican Peso", "MYR" to "Malaysian Ringgit", "NOK" to "Norwegian Krone", "NZD" to "New Zealand Dollar",
    "OMR" to "Omani Rial", "PHP" to "Philippine Peso", "PKR" to "Pakistani Rupee", "PLN" to "Polish Zloty",
    "QAR" to "Qatari Riyal", "RON" to "Romanian Leu", "RUB" to "Russian Ruble", "SAR" to "Saudi Riyal",
    "SEK" to "Swedish Krona", "SGD" to "Singapore Dollar", "THB" to "Thai Baht", "TRY" to "Turkish Lira",
    "TWD" to "New Taiwan Dollar", "UAH" to "Ukrainian Hryvnia", "USD" to "US Dollar", "VND" to "Vietnamese Dong",
    "ZAR" to "South African Rand",
)

fun walletCurrencyName(code: String): String = walletCurrencyNames[code] ?: code

/**
 * Templat dompet untuk "Impor Templat Dompet": jenis + penyedia (atau ikon umum). Nama tampil diambil dari penyedia;
 * templat tanpa penyedia memakai label jenis yang diterjemahkan di layar.
 */
data class WalletTemplate(val id: String, val type: String, val providerId: String? = null, val icon: String = "account_balance_wallet")

val walletTemplates = listOf(
    WalletTemplate("cash", "cash", icon = "hi_wallet"),
    WalletTemplate("bca", "bank", "bca"), WalletTemplate("mandiri", "bank", "mandiri"), WalletTemplate("bri", "bank", "bri"),
    WalletTemplate("bni", "bank", "bni"), WalletTemplate("bsi", "bank", "bsi"), WalletTemplate("jago", "bank", "jago"),
    WalletTemplate("seabank", "bank", "seabank"), WalletTemplate("blu", "bank", "blu"),
    WalletTemplate("gopay", "ewallet", "gopay"), WalletTemplate("ovo", "ewallet", "ovo"), WalletTemplate("dana", "ewallet", "dana"),
    WalletTemplate("shopeepay", "ewallet", "shopeepay"), WalletTemplate("linkaja", "ewallet", "linkaja"),
    WalletTemplate("credit", "credit", icon = "hi_card"), WalletTemplate("investment", "investment", icon = "hi_invest"),
)
