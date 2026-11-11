package com.clonical.kasiva.feature.importdata

/** Satu sumber CSV yang bisa dipilih di layar impor. */
data class CsvSource(val id: String, val label: String)

/**
 * Daftar sumber impor. "ivy" dan "finflow" memakai header bernama, sedangkan format
 * aplikasi lain dibaca posisional mengikuti urutan kolom ekspor publik masing-masing.
 * Mapping ditulis sendiri dari dokumentasi/format publik; bukan salinan kode Ivy Wallet.
 */
private val csvSources = listOf(
    // Format tak dikenal: header + 20 baris contoh dipetakan AI (CsvImport.previewWithAi), sisanya dikonversi lokal.
    CsvSource("ai", "Deteksi otomatis (AI)"),
    CsvSource("ivy", "Ivy Wallet CSV"),
    CsvSource("finflow", "Template Kasiva"),
    CsvSource("money_manager", "Money Manager"),
    CsvSource("wallet", "Wallet by BudgetBakers"),
    CsvSource("spendee", "Spendee"),
    CsvSource("monefy", "Monefy"),
    CsvSource("onemoney", "1Money"),
    CsvSource("bluecoins", "Bluecoins Finance"),
    CsvSource("ktw", "Money Manager (KTW)"),
    CsvSource("fortunecity", "Fortune City"),
    CsvSource("financisto", "Financisto"),
)

/** Daftar sumber impor yang dipakai UI. */
val CsvImport.sources: List<CsvSource> get() = csvSources

/** Indeks kolom 0-based tiap format; baris pertama dianggap header dan dilewati. */
internal data class CsvColumns(
    val date: Int,
    val amount: Int,
    val account: Int,
    val type: Int? = null,
    val currency: Int? = null,
    val category: Int? = null,
    val title: Int? = null,
    val description: Int? = null,
    /** Fortune City tidak punya kolom tipe: kategori "Income" berarti pemasukan. */
    val typeFromCategory: Boolean = false,
    /** Financisto memisahkan transfer keluar dan transfer masuk menjadi dua baris berurutan. */
    val pairedTransfer: Boolean = false,
)

internal fun csvColumns(source: String): CsvColumns = when (source) {
    "money_manager" -> CsvColumns(date = 0, amount = 8, account = 1, type = 6, currency = 9, category = 2, title = 4, description = 7)
    "wallet" -> CsvColumns(date = 9, amount = 3, account = 0, type = 5, currency = 2, category = 1, title = 8)
    "spendee" -> CsvColumns(date = 0, amount = 4, account = 1, type = 2, currency = 5, category = 3, title = 6)
    "monefy" -> CsvColumns(date = 0, amount = 3, account = 1, currency = 4, category = 2, title = 7)
    "onemoney" -> CsvColumns(date = 0, amount = 4, account = 2, type = 1, currency = 5, category = 3, title = 9)
    "bluecoins" -> CsvColumns(date = 1, amount = 4, account = 9, type = 0, currency = 5, category = 8, title = 3, description = 10)
    "ktw" -> CsvColumns(date = 0, amount = 3, account = 6, type = 1, currency = 4, category = 2, title = 5)
    "fortunecity" -> CsvColumns(date = 4, amount = 2, account = 0, currency = 3, category = 1, title = 5, typeFromCategory = true)
    "financisto" -> CsvColumns(date = 0, amount = 3, account = 2, type = 10, currency = 4, category = 7, title = 9, description = 12, pairedTransfer = true)
    else -> error("Sumber CSV tidak didukung: $source")
}