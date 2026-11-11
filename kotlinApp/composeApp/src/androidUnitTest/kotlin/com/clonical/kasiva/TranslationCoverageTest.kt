package com.clonical.kasiva

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Teks fitur baru (panduan, peringkat, Split Bill, pengingat tagihan) wajib ada di 9 bahasa dengan placeholder yang
 * sama — teks yang hilang jatuh ke bahasa Indonesia, placeholder berbeda bisa membuat format teks crash.
 */
class TranslationCoverageTest {
    private val languages = listOf("base", "en", "ms", "es", "fr", "de", "ja", "zh", "hi")
    private val prefixes = listOf("coach_", "lb_", "sb_", "set_notif_bills", "core_channel_bills", "core_channel_announcements")
    private val stringPattern = Regex("""<string name="([^"]+)">(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
    private val placeholder = Regex("""%\d+\$[sd]""")

    private fun load(lang: String): Map<String, String> {
        val file = File("src/commonMain/moko-resources/$lang/strings.xml")
        assertTrue(file.exists(), "missing ${file.path}")
        return stringPattern.findAll(file.readText()).associate { it.groupValues[1] to it.groupValues[2] }
    }

    @Test fun newFeatureStringsExistInEveryLanguageWithSamePlaceholders() {
        val base = load("base").filterKeys { key -> prefixes.any { key.startsWith(it) } }
        assertTrue(base.size >= 50, "expected new feature strings, found ${base.size}")
        val problems = mutableListOf<String>()
        for (lang in languages.drop(1)) {
            val strings = load(lang)
            for ((key, value) in base) {
                val translated = strings[key]
                if (translated.isNullOrBlank()) { problems += "$lang: $key missing"; continue }
                val expected = placeholder.findAll(value).map { it.value }.toSortedSet()
                val actual = placeholder.findAll(translated).map { it.value }.toSortedSet()
                if (expected != actual) problems += "$lang: $key placeholders $actual != $expected"
                if (Regex("""(?<!\\)'""").containsMatchIn(translated)) problems += "$lang: $key has unescaped apostrophe"
            }
        }
        assertTrue(problems.isEmpty(), problems.joinToString("\n"))
    }

    @Test fun userFacingTextAvoidsTechnicalJargon() {
        val jargon = Regex("""\b(server|Firestore|callable|UNAUTHENTICATED)\b""", RegexOption.IGNORE_CASE)
        val offenders = languages.flatMap { lang ->
            load(lang).filter { (key, value) -> prefixes.any { key.startsWith(it) } && jargon.containsMatchIn(value) }.map { "$lang: ${it.key}" }
        }
        assertTrue(offenders.isEmpty(), offenders.joinToString("\n"))
    }
}
