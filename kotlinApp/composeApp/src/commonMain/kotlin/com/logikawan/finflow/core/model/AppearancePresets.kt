package com.logikawan.finflow.core.model

import com.logikawan.finflow.i18n.*
import com.logikawan.finflow.core.i18n.text
import dev.icerock.moko.resources.StringResource

// Preset tampilan dipakai bersama oleh layar setup dan Pengaturan; AppPreferences hanya menyimpan id-nya.
// Label disimpan sebagai resource agar daftar top-level tidak membekukan satu bahasa.
data class CardGradientPreset(val id: String, val labelRes: StringResource, val colors: List<Long>) { val label: String get() = labelRes.text() }
data class MenuThemePreset(val id: String, val labelRes: StringResource, val colors: List<Long>) { val label: String get() = labelRes.text() }
data class ThemeModePreset(val id: String, val labelRes: StringResource, val theme: String, val palette: String, val accent: Long) { val label: String get() = labelRes.text() }

val cardGradientPresets = listOf(
    CardGradientPreset("teal", MR.strings.core_preset_teal, listOf(0xFF0D9488, 0xFF06B6D4, 0xFF2563EB)),
    CardGradientPreset("emerald", MR.strings.core_preset_emerald, listOf(0xFF059669, 0xFF10B981, 0xFF34D399)),
    CardGradientPreset("ruby", MR.strings.core_preset_ruby, listOf(0xFFE11D48, 0xFFF43F5E, 0xFFFB7185)),
    CardGradientPreset("sunset", MR.strings.core_preset_sunset, listOf(0xFFF59E0B, 0xFFF97316, 0xFFEF4444)),
    CardGradientPreset("ocean", MR.strings.core_preset_ocean, listOf(0xFF06B6D4, 0xFF0891B2, 0xFF0E7490)),
    CardGradientPreset("violet", MR.strings.core_preset_violet, listOf(0xFF7C3AED, 0xFF8B5CF6, 0xFFA78BFA)),
)

fun cardGradientColors(id: String): List<Long> = (cardGradientPresets.firstOrNull { it.id == id } ?: cardGradientPresets.first()).colors

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

fun menuThemeColors(id: String): List<Long> = (menuThemePresets.firstOrNull { it.id == id } ?: menuThemePresets.first { it.id == "colorful" }).colors

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
