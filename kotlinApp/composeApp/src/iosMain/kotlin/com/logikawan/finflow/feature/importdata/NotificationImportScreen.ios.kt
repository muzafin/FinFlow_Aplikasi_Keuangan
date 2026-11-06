package com.logikawan.finflow.feature.importdata

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.logikawan.finflow.core.model.FinanceData
import com.logikawan.finflow.core.model.Transaction

@Composable
actual fun NotificationImportScreen(data: FinanceData, onReview: (Transaction, () -> Unit) -> Unit, onOpenCsv: () -> Unit) {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Impor notifikasi tersedia di Android", style = MaterialTheme.typography.headlineMedium)
        Text("iOS tidak menyediakan akses umum untuk membaca notifikasi aplikasi bank/e-wallet lain. Gunakan CSV atau catat transaksi secara manual pada iPhone.")
        Button(onClick = onOpenCsv) { Text("Impor CSV") }
    }
}
