package com.clonical.kasiva

import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.clonical.kasiva.core.i18n.AppLocale
import com.clonical.kasiva.core.i18n.appLanguages
import com.clonical.kasiva.core.i18n.menuLabel
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.ui.KasivaTheme
import com.clonical.kasiva.i18n.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Teks moko-resources mengikuti bahasa aktif dan UI ter-recompose saat bahasa diganti. */
class LanguageDeviceTest {
    @get:Rule val ui = createComposeRule()

    @After fun restore() { ui.runOnIdle { AppLocale.apply("id") } }

    @Test fun switchingLanguageRecomposesMokoText() {
        ui.setContent { KasivaTheme { Text(MR.strings.label_tab_home.text() + " · " + menuLabel("Anggaran")) } }
        ui.runOnIdle { AppLocale.apply("id") }
        ui.onNodeWithText("Beranda · Anggaran").assertIsDisplayed()
        ui.runOnIdle { AppLocale.apply("en") }
        ui.onNodeWithText("Home · Budget").assertIsDisplayed()
        ui.runOnIdle { AppLocale.apply("ja") }
        ui.onNodeWithText("ホーム · 予算").assertIsDisplayed()
        ui.runOnIdle { AppLocale.apply("zh") }
        ui.onNodeWithText("首页 · 预算").assertIsDisplayed()
    }

    @Test fun everySupportedLanguageHasItsOwnText() {
        val seen = appLanguages.associate { lang ->
            AppLocale.apply(lang.tag)
            lang.tag to MR.strings.set_page_language.text()
        }
        assertEquals("Bahasa", seen["id"]); assertEquals("Language", seen["en"]); assertEquals("Sprache", seen["de"])
        assertEquals("Langue", seen["fr"]); assertEquals("言語", seen["ja"]); assertEquals("Idioma", seen["es"])
        assertEquals("भाषा", seen["hi"]); assertEquals("语言", seen["zh"]); assertEquals("Bahasa", seen["ms"])
        AppLocale.apply("en")
        assertEquals("Available: Rp 5", MR.strings.acc_available.text("Rp 5"))
        AppLocale.apply("id")
        assertEquals("Tersedia: Rp 5", MR.strings.acc_available.text("Rp 5"))
    }
}
