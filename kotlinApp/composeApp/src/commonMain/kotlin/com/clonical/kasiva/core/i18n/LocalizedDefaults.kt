package com.clonical.kasiva.core.i18n

import com.clonical.kasiva.core.model.FinanceData
import com.clonical.kasiva.i18n.*
import dev.icerock.moko.resources.StringResource

/*
 * Kategori bawaan dan dompet "Kas" disimpan dengan nama dalam bahasa saat dibuat. Selama pengguna belum
 * mengganti namanya (nama masih salah satu terjemahan bawaan), tampilkan dalam bahasa aktif.
 * Nama yang sudah diganti pengguna dibiarkan apa adanya.
 */
private val categoryKeys: Map<String, Pair<String, StringResource>> get() = mapOf(
    "food" to ("core_category_food" to MR.strings.core_category_food),
    "transport" to ("core_category_transport" to MR.strings.core_category_transport),
    "shopping" to ("core_category_shopping" to MR.strings.core_category_shopping),
    "health" to ("core_category_health" to MR.strings.core_category_health),
    "bills" to ("core_category_bills" to MR.strings.core_category_bills),
    "education" to ("core_category_education" to MR.strings.core_category_education),
    "entertainment" to ("core_category_entertainment" to MR.strings.core_category_entertainment),
    "other_expense" to ("core_category_other" to MR.strings.core_category_other),
    "other_income" to ("core_category_other" to MR.strings.core_category_other),
    "salary" to ("core_category_salary" to MR.strings.core_category_salary),
    "business" to ("core_category_business" to MR.strings.core_category_business),
)

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
            val shown = localizedDefault(account.name, "core_default_wallet_name", MR.strings.core_default_wallet_name)
            if (shown == account.name) account else account.copy(name = shown)
        },
    )
}
