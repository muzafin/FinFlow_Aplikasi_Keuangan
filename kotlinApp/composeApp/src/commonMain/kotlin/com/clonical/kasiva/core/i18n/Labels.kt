package com.clonical.kasiva.core.i18n

import com.clonical.kasiva.i18n.*

/*
 * Label tampilan untuk nilai yang juga dipakai sebagai pengenal (kunci navigasi, preferensi tersimpan).
 * Pengenalnya tetap berbahasa Indonesia; hanya teks di layar yang diterjemahkan.
 */

/** Menu keuangan di Beranda (`financeMenus`). */
fun menuLabel(id: String): String = when (id) {
    "Anggaran" -> MR.strings.label_menu_budget.text()
    "Berulang" -> MR.strings.label_menu_recurring.text()
    "Target" -> MR.strings.label_menu_goals.text()
    "Tagihan" -> MR.strings.label_menu_bills.text()
    "Utang" -> MR.strings.label_menu_debts.text()
    "Keinginan" -> MR.strings.label_menu_wishlist.text()
    "Kartu" -> MR.strings.label_menu_cards.text()
    "Catatan" -> MR.strings.label_menu_notes.text()
    "Reimburse" -> MR.strings.label_menu_reimburse.text()
    "Investasi" -> MR.strings.label_menu_investments.text()
    else -> id
}

/** Tab navigasi bawah (`mainPages`). */
fun tabLabel(id: String): String = when (id) {
    "Beranda" -> MR.strings.label_tab_home.text()
    "Statistik" -> MR.strings.label_tab_statistics.text()
    "Dompet" -> MR.strings.label_tab_wallet.text()
    "Profil" -> MR.strings.label_tab_profile.text()
    else -> id
}

/** Periode Beranda (`AppPreferences.homePeriod`). */
fun periodLabel(id: String): String = when (id) {
    "Hari Ini" -> MR.strings.label_period_today.text()
    "Minggu Ini" -> MR.strings.label_period_week.text()
    "Bulan Ini" -> MR.strings.label_period_month.text()
    "Tahun" -> MR.strings.label_period_year.text()
    "Kalender" -> MR.strings.label_period_calendar.text()
    else -> id
}

/** Jenis transaksi (`income`, `expense`, `transfer`). */
fun transactionTypeLabel(type: String): String = when (type) {
    "income" -> MR.strings.label_type_income.text()
    "expense" -> MR.strings.label_type_expense.text()
    "transfer" -> MR.strings.label_type_transfer.text()
    else -> type
}
