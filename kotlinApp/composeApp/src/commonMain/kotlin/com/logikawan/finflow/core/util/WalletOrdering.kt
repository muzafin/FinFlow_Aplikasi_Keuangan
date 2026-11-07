package com.logikawan.finflow.core.util

import com.logikawan.finflow.core.model.Account
import com.logikawan.finflow.core.model.AppPreferences
import com.logikawan.finflow.core.model.FinanceData

/*
 * Urutan dompet bersama (Pengaturan Dompet → Urutan Dompet Saya / Urutan Pemilih Dompet).
 * Id yang tidak tercantum di daftar urutan tetap di belakang dengan urutan aslinya, jadi preferensi
 * kosong (bawaan) tidak mengubah urutan apa pun.
 */

private val knownWalletGroups = setOf("cash", "bank", "ewallet", "savings", "credit", "investment", "loan")

/** Grup dompet untuk pengurutan; jenis tak dikenal masuk "other". */
fun walletGroupKey(account: Account): String = if (account.type in knownWalletGroups) account.type else "other"

/** Urutkan elemen menurut daftar [order] (berdasarkan [key]); yang tidak tercantum mempertahankan urutan asli di belakang. */
fun <T> List<T>.orderedBy(order: List<String>, key: (T) -> String): List<T> {
    if (order.isEmpty()) return this
    val rank = order.withIndex().associate { it.value to it.index }
    return withIndex().sortedBy { rank[key(it.value)] ?: (order.size + it.index) }.map { it.value }
}

/** Dompet diurutkan sesuai [AppPreferences.walletOrder] saja (tanpa pengelompokan). */
fun List<Account>.orderedWallets(preferences: AppPreferences): List<Account> = orderedBy(preferences.walletOrder) { it.id }

/** Urutan "Dompet Saya": grup sesuai walletGroupOrder, lalu dompet di dalam grup sesuai walletOrder. */
fun List<Account>.myWalletsOrder(preferences: AppPreferences): List<Account> {
    val groupRank = preferences.walletGroupOrder.withIndex().associate { it.value to it.index }
    val ordered = orderedWallets(preferences)
    return ordered.withIndex().sortedWith(compareBy<IndexedValue<Account>> { groupRank[walletGroupKey(it.value)] ?: Int.MAX_VALUE }.thenBy { it.index })
        .map { it.value }
}

/** Urutan pemilih dompet sesuai [AppPreferences.walletPickerOrder]. Kantong (pocket) ikut aturan yang sama. */
fun List<Account>.pickerOrder(data: FinanceData, preferences: AppPreferences): List<Account> = when (preferences.walletPickerOrder) {
    "my_wallets" -> myWalletsOrder(preferences)
    "most_used" -> {
        val counts = HashMap<String, Int>()
        data.transactions.forEach { tx ->
            counts[tx.accountId] = (counts[tx.accountId] ?: 0) + 1
            tx.toAccountId?.let { counts[it] = (counts[it] ?: 0) + 1 }
        }
        withIndex().sortedWith(compareByDescending<IndexedValue<Account>> { counts[it.value.id] ?: 0 }.thenBy { it.index }).map { it.value }
    }
    "name" -> sortedWith(compareBy<Account> { it.name.lowercase() }.thenBy { it.name })
    "balance" -> withIndex().sortedWith(compareByDescending<IndexedValue<Account>> { data.balance(it.value) }.thenBy { it.index }).map { it.value }
    else -> this
}
