package com.clonical.kasiva.core.i18n

import android.content.Context
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.ResourceFormatted
import dev.icerock.moko.resources.desc.StringDesc

/** Konteks aplikasi untuk membaca resource di luar composable (worker, repository). */
object I18nContext {
    lateinit var app: Context
    internal val ready get() = ::app.isInitialized
}

internal actual fun platformText(resource: StringResource, args: List<String>): String {
    if (!I18nContext.ready) return UnitTestStrings.text(resource.resourceId, args)
    return if (args.isEmpty()) StringDesc.Resource(resource).toString(I18nContext.app)
    else StringDesc.ResourceFormatted(resource, *args.toTypedArray()).toString(I18nContext.app)
}

/**
 * Hanya dipakai bila Context belum ada, yaitu di unit test JVM (aplikasi selalu menyetel Context lewat
 * KasivaApplication). Membaca teks dasar Indonesia dari `base/strings.xml` moko yang dipasang sebagai resource test.
 */
private object UnitTestStrings {
    private val names: Map<Int, String> by lazy {
        runCatching { Class.forName("com.clonical.kasiva.R\$string").fields.associate { it.getInt(null) to it.name } }.getOrDefault(emptyMap())
    }
    private val base: Map<String, String> by lazy {
        val xml = javaClass.classLoader?.getResourceAsStream("base/strings.xml")?.bufferedReader(Charsets.UTF_8)?.readText().orEmpty()
        Regex("""<string name="([a-z0-9_]+)">(.*?)</string>""", RegexOption.DOT_MATCHES_ALL).findAll(xml).associate { m ->
            m.groupValues[1] to m.groupValues[2].replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&").replace("\n", "\n")
        }
    }
    fun text(id: Int, args: List<String>): String {
        val raw = names[id]?.let(base::get) ?: return "#$id"
        return if (args.isEmpty()) raw else String.format(raw, *args.toTypedArray())
    }
}

actual fun systemLanguage(): String = java.util.Locale.getDefault().language.let { if (it == "in") "id" else it }.ifBlank { "en" }
