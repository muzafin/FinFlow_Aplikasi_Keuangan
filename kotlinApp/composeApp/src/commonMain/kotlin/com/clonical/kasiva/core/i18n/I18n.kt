package com.clonical.kasiva.core.i18n

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.desc.StringDesc

/** Bahasa yang didukung aplikasi. Teks dasar (moko-resources/base) berbahasa Indonesia. */
data class AppLanguage(val tag: String, val nativeName: String)

val appLanguages = listOf(
    AppLanguage("id", "Bahasa Indonesia"),
    AppLanguage("en", "English"),
    AppLanguage("ms", "Bahasa Melayu"),
    AppLanguage("de", "Deutsch"),
    AppLanguage("fr", "Français"),
    AppLanguage("ja", "日本語"),
    AppLanguage("es", "Español"),
    AppLanguage("hi", "हिन्दी"),
    AppLanguage("zh", "中文（简体）"),
)

/** Nilai preferensi `system` mengikuti bahasa HP; bila tidak didukung, pakai English. */
fun resolveLanguage(preference: String, deviceLanguage: String): String = when {
    preference != "system" && appLanguages.any { it.tag == preference } -> preference
    appLanguages.any { it.tag == deviceLanguage } -> deviceLanguage
    deviceLanguage == "in" -> "id"
    else -> "en"
}

/** Bahasa aktif. Dibaca oleh [text] sehingga UI otomatis ter-recompose saat bahasa diganti. */
object AppLocale {
    var current by mutableStateOf("")
        private set

    fun apply(tag: String) {
        if (tag == current) return
        StringDesc.localeType = StringDesc.LocaleType.Custom(tag)
        current = tag
    }
}

/** Teks terlokalisasi dari moko-resources. Argumen format memakai `%1$s`, `%2$s`, dst. */
fun StringResource.text(vararg args: Any): String {
    testTextProvider?.let { return it(this, args.map { arg -> arg.toString() }) }
    AppLocale.current // baca state agar composable pemanggil ikut recompose saat bahasa berubah
    return platformText(this, args.map { it.toString() })
}

/** Hanya untuk unit test JVM/iOS tanpa Context Android: mengganti pembacaan resource. */
var testTextProvider: ((StringResource, List<String>) -> String)? = null

/** Kode bahasa sistem (ISO 639-1), misal "id". */
expect fun systemLanguage(): String

internal expect fun platformText(resource: StringResource, args: List<String>): String
