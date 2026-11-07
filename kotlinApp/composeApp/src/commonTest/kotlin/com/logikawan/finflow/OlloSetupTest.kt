package com.logikawan.finflow

import com.logikawan.finflow.core.i18n.appLanguages
import com.logikawan.finflow.core.i18n.resolveLanguage
import com.logikawan.finflow.core.util.minorAmount
import com.logikawan.finflow.feature.auth.SetupLanguage
import com.logikawan.finflow.feature.auth.allTexts
import com.logikawan.finflow.feature.auth.setupLanguages
import com.logikawan.finflow.feature.auth.setupStrings
import kotlin.test.*

class OlloSetupTest {
    @Test fun setupTextsComeFromMokoAndAreComplete() {
        // Unit test JVM membaca teks dasar (Indonesia) dari moko-resources/base.
        val texts = setupStrings().allTexts()
        assertTrue(texts.size >= 50, "Wizard harus mempunyai teks yang lengkap: ${texts.size}")
        texts.forEach { assertTrue(it.isNotBlank() && !it.startsWith("#"), "Teks kosong/tidak ditemukan: $it") }
        texts.forEach { assertFalse('�' in it, "Teks rusak: $it") }
        assertEquals("Lanjut", setupStrings().next)
        assertEquals("Mulai Sekarang", setupStrings().startNow)
        assertEquals("Nama lengkap wajib diisi.", setupStrings().nameRequired)
    }

    @Test fun allNineLanguagesAreEnabled() {
        assertEquals(9, setupLanguages.size)
        assertTrue(setupLanguages.all { it.enabled })
        assertEquals(appLanguages.map { it.tag }.toSet(), setupLanguages.map { it.tag }.toSet())
        assertTrue(setupLanguages.all { it.tag.isNotBlank() && it.label.isNotBlank() })
        assertEquals(setupLanguages.size, setupLanguages.map { it.tag }.toSet().size)
        setupLanguages.forEach { language: SetupLanguage -> assertTrue(language.tag.matches(Regex("[a-z]{2}"))) }
    }

    @Test fun languagePreferenceFollowsDeviceByDefault() {
        assertEquals("id", resolveLanguage("system", "id"))
        assertEquals("id", resolveLanguage("system", "in"))
        assertEquals("ja", resolveLanguage("system", "ja"))
        assertEquals("en", resolveLanguage("system", "ko"))
        assertEquals("de", resolveLanguage("de", "id"))
        assertEquals("fr", resolveLanguage("xx", "fr"))
        assertEquals("system", com.logikawan.finflow.core.model.AppPreferences().language)
    }

    @Test fun openingBalanceAcceptsZeroAndRejectsUnsafeInput() {
        assertEquals(0L, minorAmount("0", "IDR"))
        assertEquals(0L, minorAmount("0", "JPY"))
        assertEquals(1245L, minorAmount("12.45", "USD"))
        assertFailsWith<IllegalArgumentException> { minorAmount("", "IDR") }
        assertFailsWith<IllegalArgumentException> { minorAmount("abc", "IDR") }
        assertFailsWith<IllegalArgumentException> { minorAmount("1.234", "IDR") }
    }
}
