package com.clonical.kasiva.core.i18n

import com.clonical.kasiva.core.model.FinanceData
import com.clonical.kasiva.i18n.*
import dev.icerock.moko.resources.StringResource

/*
 * Kategori bawaan dan dompet "Kas" disimpan dengan nama dalam bahasa saat dibuat. Selama pengguna belum
 * mengganti namanya (nama masih salah satu terjemahan bawaan), tampilkan dalam bahasa aktif.
 * Nama yang sudah diganti pengguna dibiarkan apa adanya.
 */
private val categoryKeys: Map<String, Pair<String, StringResource>> get() =
    (com.clonical.kasiva.core.model.defaultCategorySpecs + com.clonical.kasiva.core.model.legacyCategorySpecs).associate { it.id to (it.key to it.name) }

/** Nama tampilan: terjemahan bawaan bila [name] masih nama bawaan untuk kunci [key]. */
private fun localizedDefault(name: String, key: String, resource: StringResource): String =
    if (defaultNameVariants[key]?.contains(name) == true) resource.text() else name

/** Data untuk ditampilkan: nama kategori bawaan dan dompet "Kas" mengikuti bahasa aktif. */
fun FinanceData.withLocalizedDefaults(): FinanceData {
    val keys = categoryKeys
    return copy(
        categories = categories.map { category ->
            val (key, resource) = keys[category.id] ?: return@map category
            val shown = localizedDefault(category.name, key, resource)
            if (shown == category.name) category else category.copy(name = shown)
        },
        accounts = accounts.map { account ->
            // Hanya dompet bawaan yang dibuat aplikasi; dompet buatan pengguna bernama "Cash"/"Tunai" tetap (P6-03).
            if (account.id != "acc_cash") return@map account
            val shown = localizedDefault(account.name, "core_default_wallet_name", MR.strings.core_default_wallet_name)
            if (shown == account.name) account else account.copy(name = shown)
        },
    )
}
