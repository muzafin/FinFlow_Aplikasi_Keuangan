package com.logikawan.finflow

import com.logikawan.finflow.core.util.minorAmount
import com.logikawan.finflow.feature.auth.SetupLanguage
import com.logikawan.finflow.feature.auth.allTexts
import com.logikawan.finflow.feature.auth.setupLanguages
import com.logikawan.finflow.feature.auth.setupStrings
import kotlin.test.*

class OlloSetupTest {
    @Test fun indonesianAndEnglishSetupTextsAreComplete() {
        val id = setupStrings("id")
        val en = setupStrings("en")
        val idTexts = id.allTexts()
        val enTexts = en.allTexts()
        assertTrue(idTexts.size >= 50, "Wizard harus mempunyai teks yang lengkap: ${idTexts.size}")
        assertEquals(idTexts.size, enTexts.size)
        idTexts.forEach { assertTrue(it.isNotBlank(), "Teks Indonesia kosong") }
        enTexts.forEach { assertTrue(it.isNotBlank(), "Teks Inggris kosong") }
        (idTexts + enTexts).forEach { assertFalse('\uFFFD' in it, "Teks rusak: $it") }
    }

    @Test fun englishReallyDiffersFromIndonesian() {
        val id = setupStrings("id")
        val en = setupStrings("en")
        val differing = id.allTexts().zip(en.allTexts()).count { (a, b) -> a != b }
        assertTrue(differing >= 20, "Terjemahan Inggris harus benar-benar berbeda: $differing")
        assertEquals("Lanjut", id.next)
        assertEquals("Continue", en.next)
        assertEquals("Pilih Bahasa", id.languageTitle)
        assertEquals("Choose Language", en.languageTitle)
        assertEquals("Mulai Sekarang", id.startNow)
        assertEquals("Start Now", en.startNow)
    }

    @Test fun fullNameRequirementIsLocalizedForBothLanguages() {
        val id = setupStrings("id")
        val en = setupStrings("en")
        assertTrue(id.nameRequired.isNotBlank(), "Pesan nama wajib (ID) harus terisi")
        assertTrue(en.nameRequired.isNotBlank(), "Pesan nama wajib (EN) harus terisi")
        assertEquals("Nama lengkap wajib diisi.", id.nameRequired)
        assertEquals("Full name is required.", en.nameRequired)
        assertTrue(id.nameRequired != en.nameRequired, "Pesan wajib nama harus dilokalisasi")
        assertTrue(id.allTexts().contains(id.nameRequired), "Pesan wajib nama (ID) harus ikut allTexts()")
        assertTrue(en.allTexts().contains(en.nameRequired), "Pesan wajib nama (EN) harus ikut allTexts()")
    }

    @Test fun languageListKeepsOnlyWorkingOptionsEnabled() {
        assertEquals(9, setupLanguages.size)
        assertEquals(listOf("en", "id"), setupLanguages.filter { it.enabled }.map { it.tag })
        assertTrue(setupLanguages.filter { !it.enabled }.all { it.tag != "en" && it.tag != "id" })
        assertTrue(setupLanguages.all { it.tag.isNotBlank() && it.label.isNotBlank() })
        assertEquals(setupLanguages.size, setupLanguages.map { it.tag }.toSet().size)
        setupLanguages.forEach { language: SetupLanguage -> assertTrue(language.tag.matches(Regex("[a-z]{2}"))) }
    }

    @Test fun setupStringsFallBackToIndonesianAndStayLocalized() {
        val id = setupStrings("id")
        assertEquals(id, setupStrings(""))
        assertEquals(id, setupStrings("xx"))
        assertEquals(id, setupStrings("id"))
        assertEquals(setupStrings("en"), setupStrings("EN"))
        assertNotEquals(id, setupStrings("en"))
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
