package com.logikawan.finflow.core.model

// Preset tampilan dipakai bersama oleh layar setup dan Pengaturan; AppPreferences hanya menyimpan id-nya.
data class CardGradientPreset(val id: String, val label: String, val colors: List<Long>)
data class MenuThemePreset(val id: String, val label: String, val colors: List<Long>)
data class ThemeModePreset(val id: String, val label: String, val theme: String, val palette: String, val accent: Long)

val cardGradientPresets = listOf(
    CardGradientPreset("teal", "Teal", listOf(0xFF0D9488, 0xFF06B6D4, 0xFF2563EB)),
    CardGradientPreset("emerald", "Zamrud", listOf(0xFF059669, 0xFF10B981, 0xFF34D399)),
    CardGradientPreset("ruby", "Ruby", listOf(0xFFE11D48, 0xFFF43F5E, 0xFFFB7185)),
    CardGradientPreset("sunset", "Senja", listOf(0xFFF59E0B, 0xFFF97316, 0xFFEF4444)),
    CardGradientPreset("ocean", "Samudra", listOf(0xFF06B6D4, 0xFF0891B2, 0xFF0E7490)),
    CardGradientPreset("violet", "Violet", listOf(0xFF7C3AED, 0xFF8B5CF6, 0xFFA78BFA)),
)

fun cardGradientColors(id: String): List<Long> = (cardGradientPresets.firstOrNull { it.id == id } ?: cardGradientPresets.first()).colors

val menuThemePresets = listOf(
    MenuThemePreset("colorful", "Warna-warni",
        listOf(0xFF29B6F6, 0xFF64A5F7, 0xFF26BBA9, 0xFF08A9D9, 0xFF407BFF, 0xFF20B2C7, 0xFF3548D7, 0xFF56ADB7, 0xFF079AC7, 0xFF407BFF)),
    MenuThemePreset("ocean", "Samudra",
        listOf(0xFF06B6D4, 0xFF0891B2, 0xFF0EA5E9, 0xFF0284C7, 0xFF0369A1, 0xFF0F766E, 0xFF14B8A6, 0xFF2DD4BF, 0xFF38BDF8, 0xFF075985)),
    MenuThemePreset("sunset", "Senja",
        listOf(0xFFF59E0B, 0xFFF97316, 0xFFEF4444, 0xFFFB923C, 0xFFFBBF24, 0xFFF43F5E, 0xFFDC2626, 0xFFEA580C, 0xFFD97706, 0xFFFACC15)),
    MenuThemePreset("forest", "Hutan",
        listOf(0xFF059669, 0xFF10B981, 0xFF047857, 0xFF65A30D, 0xFF4D7C0F, 0xFF84CC16, 0xFF166534, 0xFF78350F, 0xFF92400E, 0xFFA16207)),
    MenuThemePreset("candy", "Permen",
        listOf(0xFFEC4899, 0xFFF472B6, 0xFFD946EF, 0xFFA855F7, 0xFF8B5CF6, 0xFF6366F1, 0xFF14B8A6, 0xFF2DD4BF, 0xFFFB7185, 0xFFC084FC)),
)

fun menuThemeColors(id: String): List<Long> = (menuThemePresets.firstOrNull { it.id == id } ?: menuThemePresets.first { it.id == "colorful" }).colors

val themeModePresets = listOf(
    ThemeModePreset("light", "Terang", "light", "neutral", 0xFF1D1E26),
    ThemeModePreset("dark", "Gelap", "dark", "neutral", 0xFF141519),
    ThemeModePreset("arctic", "Biru Arktik", "light", "arctic", 0xFF006B8F),
    ThemeModePreset("ruby", "Ruby", "light", "ruby", 0xFFC62828),
    ThemeModePreset("emerald", "Zamrud", "light", "emerald", 0xFF237A43),
    ThemeModePreset("indigo", "Indigo", "light", "indigo", 0xFF4548AF),
)

fun themeModePresetOf(theme: String, palette: String): String =
    themeModePresets.firstOrNull { it.theme == theme && it.palette == palette }?.id ?: "light"
