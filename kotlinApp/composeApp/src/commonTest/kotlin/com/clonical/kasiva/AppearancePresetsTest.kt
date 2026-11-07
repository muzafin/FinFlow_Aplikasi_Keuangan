package com.clonical.kasiva

import com.clonical.kasiva.core.model.AppPreferences
import com.clonical.kasiva.core.model.cardGradientColors
import com.clonical.kasiva.core.model.cardGradientPresets
import com.clonical.kasiva.core.model.menuThemeColors
import com.clonical.kasiva.core.model.menuThemePresets
import com.clonical.kasiva.core.model.themeModePresetOf
import com.clonical.kasiva.core.model.themeModePresets
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AppearancePresetsTest {
    private fun assertUniqueIdsAndLabels(presets: List<Pair<String, String>>) {
        assertEquals(presets.size, presets.map { it.first }.distinct().size)
        assertTrue(presets.all { it.second.isNotBlank() })
    }

    @Test fun gradientPresetsAreUniqueAndComplete() {
        assertUniqueIdsAndLabels(cardGradientPresets.map { it.id to it.label })
        assertEquals(6, cardGradientPresets.size)
        assertEquals(listOf("teal", "emerald", "ruby", "sunset", "ocean", "violet"), cardGradientPresets.map { it.id })
        assertTrue(cardGradientPresets.all { it.colors.size >= 2 })
    }

    @Test fun menuThemePresetsHaveTenColorsEach() {
        assertUniqueIdsAndLabels(menuThemePresets.map { it.id to it.label })
        assertEquals(5, menuThemePresets.size)
        assertEquals(listOf("colorful", "ocean", "sunset", "forest", "candy"), menuThemePresets.map { it.id })
        assertTrue(menuThemePresets.all { it.colors.size == 10 })
    }

    @Test fun themeModePresetsMatchPreferenceValidation() {
        assertUniqueIdsAndLabels(themeModePresets.map { it.id to it.label })
        assertEquals(6, themeModePresets.size)
        val themes = setOf("light", "dark")
        val palettes = setOf("neutral", "arctic", "ruby", "emerald", "indigo")
        assertTrue(themeModePresets.all { it.theme in themes && it.palette in palettes })
    }

    @Test fun everyPresetColorIsOpaqueArgb() {
        val colors = cardGradientPresets.flatMap { it.colors } + menuThemePresets.flatMap { it.colors } + themeModePresets.map { it.accent }
        assertTrue(colors.all { it in 0xFF000000L..0xFFFFFFFFL })
    }

    @Test fun unknownPresetIdsFallBackToDefaults() {
        assertEquals(cardGradientPresets.first().colors, cardGradientColors("tidak-ada"))
        assertEquals(menuThemePresets.first { it.id == "colorful" }.colors, menuThemeColors("tidak-ada"))
        assertEquals(cardGradientPresets.first { it.id == "ruby" }.colors, cardGradientColors("ruby"))
        assertEquals(menuThemePresets.first { it.id == "forest" }.colors, menuThemeColors("forest"))
    }

    @Test fun themeModePresetLookupFallsBackToLight() {
        assertEquals("dark", themeModePresetOf("dark", "neutral"))
        assertEquals("arctic", themeModePresetOf("light", "arctic"))
        assertEquals("light", themeModePresetOf("x", "y"))
        assertEquals("light", themeModePresetOf("system", "neutral"))
    }

    @Test fun defaultPreferencesAreValid() {
        AppPreferences().validate()
    }

    @Test fun validationRejectsUnknownAppearanceValues() {
        assertFailsWith<IllegalArgumentException> { AppPreferences(language = "xx").validate() }
        assertFailsWith<IllegalArgumentException> { AppPreferences(cardGradient = "xx").validate() }
        assertFailsWith<IllegalArgumentException> { AppPreferences(menuTheme = "xx").validate() }
        assertFailsWith<IllegalArgumentException> { AppPreferences(addButtonMode = "xx").validate() }
    }

    @Test fun oldJsonWithoutAppearanceFieldsUsesDefaults() {
        assertEquals(AppPreferences(), Json { ignoreUnknownKeys = true }.decodeFromString<AppPreferences>("{}"))
        assertEquals(AppPreferences(), Json { ignoreUnknownKeys = true }.decodeFromString<AppPreferences>("{\"versiLama\":true}"))
    }
}
