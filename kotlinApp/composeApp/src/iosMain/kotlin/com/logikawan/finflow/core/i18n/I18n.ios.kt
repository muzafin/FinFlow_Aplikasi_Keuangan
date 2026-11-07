package com.logikawan.finflow.core.i18n

import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.ResourceFormatted
import dev.icerock.moko.resources.desc.StringDesc

internal actual fun platformText(resource: StringResource, args: List<String>): String =
    if (args.isEmpty()) StringDesc.Resource(resource).localized()
    else StringDesc.ResourceFormatted(resource, *args.toTypedArray()).localized()

actual fun systemLanguage(): String =
    (platform.Foundation.NSLocale.preferredLanguages.firstOrNull() as? String)?.substringBefore('-')?.lowercase()?.ifBlank { null } ?: "en"
