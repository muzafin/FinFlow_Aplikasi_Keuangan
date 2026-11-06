package com.logikawan.finflow.feature.importdata

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.logikawan.finflow.core.model.ImportPlan
import com.logikawan.finflow.core.ui.Choice
import com.logikawan.finflow.core.ui.FFColors
import com.logikawan.finflow.core.ui.FinanceCard
import com.logikawan.finflow.core.util.money

@Composable
fun ImportScreen(plan: ImportPlan?, source: String, changeSource: (String) -> Unit, busy: Boolean, progress: String, onPick: () -> Unit, onTemplate: () -> Unit, onImport: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Bawa riwayat ke FinFlow", style = MaterialTheme.typography.headlineSmall) }
        item { Text("Di Ivy Wallet pilih Export to CSV. Impor ini menerima CSV transaksi, bukan backup ZIP/JSON. Untuk aplikasi lain, sesuaikan kolom dengan template FinFlow.") }
        item { Choice("Sumber file", source, listOf("ivy" to "Ivy Wallet CSV", "finflow" to "CSV template FinFlow"), changeSource) }
        item { OutlinedButton(enabled = !busy, onClick = onTemplate) { Text("Simpan template CSV") } }
        item { Button(enabled = !busy, onClick = onPick) { Text("Pilih CSV (maks. 5 MB)") } }
        plan?.let { preview ->
            item { FinanceCard {
                Text("Pratinjau impor", style = MaterialTheme.typography.titleLarge)
                Text("${preview.transactions.size} transaksi baru · ${preview.duplicateCount} duplikat dilewati")
                Text("${preview.accounts.size} dompet baru · ${preview.categories.size} kategori baru")
                preview.warnings.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                Text("${preview.transactions.count { it.scheduled }} transaksi terjadwal belum dihitung dalam saldo.")
            } }
            items(preview.errors.take(30)) { Text(it, color = FFColors.expense) }
            if (preview.errors.size > 30) item { Text("Dan ${preview.errors.size - 30} kesalahan lainnya. Perbaiki file lalu pilih ulang.") }
            items(preview.transactions.take(10), key = { it.id }) { tx -> Text("${tx.date} · ${tx.title} · ${money(tx.amountMinor, tx.currency)}") }
            item { Text("ID sumber mencegah file yang sama masuk dua kali. Jika koneksi terputus, pilih dan impor ulang file yang sama; data yang sudah tersimpan dilewati.") }
            item { Button(enabled = !busy && preview.errors.isEmpty() && preview.transactions.isNotEmpty(), onClick = onImport) { Text(if (busy) "Mengimpor $progress" else "Impor ${preview.transactions.size} transaksi") } }
        }
    }
}
