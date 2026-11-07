package com.clonical.kasiva.core.model

import com.clonical.kasiva.i18n.*
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.util.formatBalance
import dev.icerock.moko.resources.StringResource

// Preset tampilan dipakai bersama oleh layar setup dan Pengaturan; AppPreferences hanya menyimpan id-nya.
// Label disimpan sebagai resource agar daftar top-level tidak membekukan satu bahasa.
data class CardGradientPreset(val id: String, val labelRes: StringResource, val colors: List<Long>, val dark: Boolean = false) { val label: String get() = labelRes.text() }
data class MenuThemePreset(val id: String, val labelRes: StringResource, val colors: List<Long>, val single: Boolean = false) { val label: String get() = labelRes.text() }
data class ThemeModePreset(val id: String, val labelRes: StringResource, val theme: String, val palette: String, val accent: Long) { val label: String get() = labelRes.text() }

/** Preset kartu yang dipakai setup awal (urutan & jumlah dikunci uji). Juga tampil di tab "Gelap" Tampilan Kartu. */
val cardGradientPresets = listOf(
    CardGradientPreset("teal", MR.strings.core_preset_teal, listOf(0xFF0D9488, 0xFF06B6D4, 0xFF2563EB), dark = true),
    CardGradientPreset("emerald", MR.strings.core_preset_emerald, listOf(0xFF059669, 0xFF10B981, 0xFF34D399), dark = true),
    CardGradientPreset("ruby", MR.strings.core_preset_ruby, listOf(0xFFE11D48, 0xFFF43F5E, 0xFFFB7185), dark = true),
    CardGradientPreset("sunset", MR.strings.core_preset_sunset, listOf(0xFFF59E0B, 0xFFF97316, 0xFFEF4444), dark = true),
    CardGradientPreset("ocean", MR.strings.core_preset_ocean, listOf(0xFF06B6D4, 0xFF0891B2, 0xFF0E7490), dark = true),
    CardGradientPreset("violet", MR.strings.core_preset_violet, listOf(0xFF7C3AED, 0xFF8B5CF6, 0xFFA78BFA), dark = true),
)

/** Preset tambahan halaman Tampilan Kartu (mengikuti daftar gratis Ollo "Select Theme"). */
val extraCardGradientPresets = listOf(
    CardGradientPreset("classic_blue", MR.strings.pc_card_classic_blue, listOf(0xFF4A90E2, 0xFF50C9C3, 0xFF56DDC0)),
    CardGradientPreset("rainbow", MR.strings.pc_card_rainbow, listOf(0xFFF093FB, 0xFF2BD2FF, 0xFF2BFF88)),
    CardGradientPreset("ocean_teal", MR.strings.pc_card_ocean_teal, listOf(0xFF43E97B, 0xFF38F9D7)),
    CardGradientPreset("berry_purple", MR.strings.pc_card_berry_purple, listOf(0xFFF78CA0, 0xFFF9748F, 0xFFFD9A8B)),
    CardGradientPreset("sunset_orange", MR.strings.pc_card_sunset_orange, listOf(0xFFF9D423, 0xFFFF8C42, 0xFFFF4E50)),
    CardGradientPreset("aurora", MR.strings.pc_card_aurora, listOf(0xFF00E676, 0xFF00B09B, 0xFF0072FF)),
    CardGradientPreset("calm_oasis", MR.strings.pc_card_calm_oasis, listOf(0xFFDDE4C9, 0xFF9FD3C7, 0xFF6EC3CE)),
    CardGradientPreset("soft_lavender", MR.strings.pc_card_soft_lavender, listOf(0xFFA3A8D8, 0xFFC8CDE0, 0xFFE3F2C6)),
    CardGradientPreset("pastel_dream", MR.strings.pc_card_pastel_dream, listOf(0xFFFBC2EB, 0xFFA6C1EE)),
    CardGradientPreset("simply_mint", MR.strings.pc_card_simply_mint, listOf(0xFF98E5C5, 0xFF5CC9A7)),
    CardGradientPreset("simply_peach", MR.strings.pc_card_simply_peach, listOf(0xFFFFCBA4, 0xFFFF9A8B)),
    CardGradientPreset("simply_blue", MR.strings.pc_card_simply_blue, listOf(0xFF8EC5FC, 0xFF5A9DF5)),
    CardGradientPreset("simply_lilac", MR.strings.pc_card_simply_lilac, listOf(0xFFD4BFF9, 0xFFA88BEB)),
    CardGradientPreset("simply_lemon", MR.strings.pc_card_simply_lemon, listOf(0xFFFFE082, 0xFFF6C344)),
    CardGradientPreset("arctic_blue", MR.strings.pc_card_arctic_blue, listOf(0xFF8FD3F4, 0xFFC4E4FA, 0xFFE0EEFF)),
    CardGradientPreset("coral_sunset", MR.strings.pc_card_coral_sunset, listOf(0xFFFF6F61, 0xFFFF9F4A, 0xFFFFD93D)),
    CardGradientPreset("sage_garden", MR.strings.pc_card_sage_garden, listOf(0xFF7FB77E, 0xFFB5DDB0)),
    CardGradientPreset("crimson", MR.strings.pc_card_crimson, listOf(0xFFD32F2F, 0xFFB71C1C)),
    CardGradientPreset("midnight", MR.strings.pc_card_midnight, listOf(0xFF0F2027, 0xFF203A43, 0xFF2C5364), dark = true),
    CardGradientPreset("deep_space", MR.strings.pc_card_deep_space, listOf(0xFF141E30, 0xFF243B55), dark = true),
    CardGradientPreset("dark_berry", MR.strings.pc_card_dark_berry, listOf(0xFF42275A, 0xFF734B6D), dark = true),
    CardGradientPreset("forest_night", MR.strings.pc_card_forest_night, listOf(0xFF0B3D2E, 0xFF1F6F50), dark = true),
)

val allCardGradientPresets: List<CardGradientPreset> get() = cardGradientPresets + extraCardGradientPresets

private const val CUSTOM_CARD_PREFIX = "custom:"
private fun argbHex(color: Long): String = (color and 0xFFFFFFFFL).toString(16).uppercase().padStart(8, '0')

/** Id gradien kustom (tab "Kustom"): warna disimpan di dalam id agar setiap pemakai [cardGradientColors] ikut berubah. */
fun customCardGradientId(start: Long, end: Long): String = CUSTOM_CARD_PREFIX + argbHex(start) + ":" + argbHex(end)

fun customCardGradientColors(id: String): List<Long>? {
    if (!id.startsWith(CUSTOM_CARD_PREFIX)) return null
    val parts = id.removePrefix(CUSTOM_CARD_PREFIX).split(':')
    if (parts.size != 2 || parts.any { !it.matches(Regex("[0-9A-F]{8}")) }) return null
    val colors = parts.map { it.toLong(16) }
    return colors.takeIf { it.all { c -> c in 0xFF000000L..0xFFFFFFFFL } }
}

fun isValidCardGradient(id: String): Boolean = allCardGradientPresets.any { it.id == id } || customCardGradientColors(id) != null

fun cardGradientColors(id: String): List<Long> =
    customCardGradientColors(id) ?: (allCardGradientPresets.firstOrNull { it.id == id } ?: cardGradientPresets.first()).colors

val menuThemePresets = listOf(
    MenuThemePreset("colorful", MR.strings.core_preset_colorful,
        listOf(0xFF29B6F6, 0xFF64A5F7, 0xFF26BBA9, 0xFF08A9D9, 0xFF407BFF, 0xFF20B2C7, 0xFF3548D7, 0xFF56ADB7, 0xFF079AC7, 0xFF407BFF)),
    MenuThemePreset("ocean", MR.strings.core_preset_ocean,
        listOf(0xFF06B6D4, 0xFF0891B2, 0xFF0EA5E9, 0xFF0284C7, 0xFF0369A1, 0xFF0F766E, 0xFF14B8A6, 0xFF2DD4BF, 0xFF38BDF8, 0xFF075985)),
    MenuThemePreset("sunset", MR.strings.core_preset_sunset,
        listOf(0xFFF59E0B, 0xFFF97316, 0xFFEF4444, 0xFFFB923C, 0xFFFBBF24, 0xFFF43F5E, 0xFFDC2626, 0xFFEA580C, 0xFFD97706, 0xFFFACC15)),
    MenuThemePreset("forest", MR.strings.core_preset_forest,
        listOf(0xFF059669, 0xFF10B981, 0xFF047857, 0xFF65A30D, 0xFF4D7C0F, 0xFF84CC16, 0xFF166534, 0xFF78350F, 0xFF92400E, 0xFFA16207)),
    MenuThemePreset("candy", MR.strings.core_preset_candy,
        listOf(0xFFEC4899, 0xFFF472B6, 0xFFD946EF, 0xFFA855F7, 0xFF8B5CF6, 0xFF6366F1, 0xFF14B8A6, 0xFF2DD4BF, 0xFFFB7185, 0xFFC084FC)),
)

/** Preset menu tambahan (Ollo "Menu Appearance": Mixed Colors & Single Color yang gratis). */
val extraMenuThemePresets = listOf(
    MenuThemePreset("classic", MR.strings.pc_menu_classic, listOf(0xFFFF9800, 0xFF2196F3, 0xFF4CAF50, 0xFFF44336)),
    MenuThemePreset("neon", MR.strings.pc_menu_neon, listOf(0xFFE000E0, 0xFF00C8D7, 0xFF2DBE12, 0xFFFF3131)),
    MenuThemePreset("pastel", MR.strings.pc_menu_pastel, listOf(0xFFF48FB1, 0xFF64B5F6, 0xFF81C784, 0xFFFFB74D)),
    MenuThemePreset("arctic_blue", MR.strings.pc_menu_arctic_blue, listOf(0xFF38BDF8, 0xFF60A5FA, 0xFF2DD4BF, 0xFF0EA5E9)),
    MenuThemePreset("monochrome", MR.strings.pc_menu_monochrome, listOf(0xFF757575), single = true),
    MenuThemePreset("navy", MR.strings.pc_menu_navy, listOf(0xFF1E3A5F), single = true),
    MenuThemePreset("rose", MR.strings.pc_menu_rose, listOf(0xFFE91E63), single = true),
)

val allMenuThemePresets: List<MenuThemePreset> get() = menuThemePresets + extraMenuThemePresets

fun isValidMenuTheme(id: String): Boolean = allMenuThemePresets.any { it.id == id }

fun menuThemeColors(id: String): List<Long> = (allMenuThemePresets.firstOrNull { it.id == id } ?: menuThemePresets.first { it.id == "colorful" }).colors

val themeModePresets = listOf(
    ThemeModePreset("light", MR.strings.core_preset_light, "light", "neutral", 0xFF1D1E26),
    ThemeModePreset("dark", MR.strings.core_preset_dark, "dark", "neutral", 0xFF141519),
    ThemeModePreset("arctic", MR.strings.core_preset_arctic, "light", "arctic", 0xFF006B8F),
    ThemeModePreset("ruby", MR.strings.core_preset_ruby, "light", "ruby", 0xFFC62828),
    ThemeModePreset("emerald", MR.strings.core_preset_emerald, "light", "emerald", 0xFF237A43),
    ThemeModePreset("indigo", MR.strings.core_preset_indigo, "light", "indigo", 0xFF4548AF),
)

fun themeModePresetOf(theme: String, palette: String): String =
    themeModePresets.firstOrNull { it.theme == theme && it.palette == palette }?.id ?: "light"

/**
 * Pilihan halaman Tema (gaya Ollo "Select Theme"): setiap palet tersedia untuk mode terang dan gelap.
 * [swatches] = latar, permukaan, aksen — sama dengan nilai yang dipakai KasivaTheme.
 */
data class ThemeChoice(val theme: String, val palette: String, val labelRes: StringResource, val icon: String, val swatches: List<Long>) {
    val id: String get() = "$theme-$palette"
    val label: String get() = labelRes.text()
}

private fun paletteAccent(palette: String, dark: Boolean): Long = if (dark) when (palette) {
    "arctic" -> 0xFF8DCFF0; "ruby" -> 0xFFFFB4AB; "emerald" -> 0xFFA8DAB5; "indigo" -> 0xFFBEC2FF; else -> 0xFFD5D8E1
} else when (palette) {
    "arctic" -> 0xFF006B8F; "ruby" -> 0xFFC62828; "emerald" -> 0xFF237A43; "indigo" -> 0xFF4548AF; else -> 0xFF1D1E26
}

private val paletteLabels = listOf("neutral" to MR.strings.core_preset_light, "arctic" to MR.strings.core_preset_arctic,
    "ruby" to MR.strings.core_preset_ruby, "emerald" to MR.strings.core_preset_emerald, "indigo" to MR.strings.core_preset_indigo)

val lightThemeChoices: List<ThemeChoice> get() = paletteLabels.map { (palette, label) ->
    ThemeChoice("light", palette, label, if (palette == "neutral") "hi_bulb" else "hi_paint", listOf(0xFFFFFFFF, 0xFFF7F7F8, paletteAccent(palette, false)))
}

val darkThemeChoices: List<ThemeChoice> get() = paletteLabels.map { (palette, label) ->
    ThemeChoice("dark", palette, if (palette == "neutral") MR.strings.core_preset_dark else label, if (palette == "neutral") "hi_moon" else "hi_paint",
        listOf(0xFF000000, 0xFF101014, paletteAccent(palette, true)))
}

/** Palet warna transaksi (Ollo "Transaction Palette"): pemasukan, pengeluaran, transfer. */
data class TransactionPalette(val id: String, val labelRes: StringResource, val income: Long, val expense: Long, val transfer: Long) {
    val label: String get() = labelRes.text()
}

val transactionPalettes = listOf(
    TransactionPalette("classic", MR.strings.pc_palette_classic, 0xFF4CAF50, 0xFFFF5252, 0xFF2196F3),
    TransactionPalette("pastel", MR.strings.pc_palette_pastel, 0xFF77DD77, 0xFFFF6961, 0xFFAEC6CF),
    TransactionPalette("vibrant", MR.strings.pc_palette_vibrant, 0xFF00C853, 0xFFE040FB, 0xFF00B8D4),
    TransactionPalette("ocean", MR.strings.pc_palette_ocean, 0xFF4DB6AC, 0xFFE57373, 0xFF0288D1),
    TransactionPalette("warm", MR.strings.pc_palette_warm, 0xFFFFB74D, 0xFFD32F2F, 0xFF8D6E63),
    TransactionPalette("neon", MR.strings.pc_palette_neon, 0xFF00E5FF, 0xFFFF4081, 0xFFFFD600),
    TransactionPalette("mint_breeze", MR.strings.pc_palette_mint_breeze, 0xFF26A69A, 0xFFEF5350, 0xFF80CBC4),
    TransactionPalette("coral_bloom", MR.strings.pc_palette_coral_bloom, 0xFF66BB6A, 0xFFFF7F50, 0xFFFFAB91),
    TransactionPalette("forest_mist", MR.strings.pc_palette_forest_mist, 0xFF2E7D32, 0xFFA1887F, 0xFF78909C),
    TransactionPalette("dawn_petal", MR.strings.pc_palette_dawn_petal, 0xFF81C784, 0xFFF06292, 0xFFBA68C8),
    TransactionPalette("stonewave", MR.strings.pc_palette_stonewave, 0xFF607D8B, 0xFFA1887F, 0xFF90A4AE),
    TransactionPalette("ruby_tones", MR.strings.pc_palette_ruby_tones, 0xFFFA8072, 0xFFC62828, 0xFF8E1B1B),
)

/** Palet yang cocok dengan warna tersimpan, atau null bila kombinasi kustom (mis. dari setup awal). */
fun transactionPaletteOf(p: AppPreferences): TransactionPalette? =
    transactionPalettes.firstOrNull { it.income == p.incomeColor && it.expense == p.expenseColor && it.transfer == p.transferColor }
        ?: transactionPalettes.firstOrNull { it.income == p.incomeColor && it.expense == p.expenseColor }

/** Karakter yang boleh dipakai untuk menyamarkan saldo (Format Saldo → Karakter Masker). */
val maskCharacters = listOf("•", "*", "#", "x", "-")

/**
 * Saldo tersamar sesuai preferensi. Dengan [AppPreferences.preserveMaskFormat] setiap digit diganti karakter masker
 * (Rp1.234.567 → Rp•.•••.•••); tanpa itu hanya awalan mata uang yang dipertahankan (Rp••••••).
 */
fun maskedBalance(value: Long, currency: String, p: AppPreferences): String {
    val formatted = formatBalance(value, currency, p)
    val mask = p.maskCharacter.ifEmpty { "•" }
    if (p.preserveMaskFormat) return formatted.map { if (it.isDigit()) mask else it.toString() }.joinToString("")
    val prefix = formatted.takeWhile { !it.isDigit() && it != '-' }
    return prefix + mask.repeat(6)
}
