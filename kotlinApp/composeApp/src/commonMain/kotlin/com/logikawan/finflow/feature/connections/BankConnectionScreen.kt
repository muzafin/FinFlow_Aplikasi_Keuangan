package com.logikawan.finflow.feature.connections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BankConnectionScreen(onImport: () -> Unit, onNotifications: () -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Koneksi baca-saja", style = MaterialTheme.typography.headlineSmall)
        Text("Koneksi otomatis belum diaktifkan. Dukungan bergantung pada bank, negara, dan penyedia integrasi.")
        Text("Saat tersedia, izin hanya untuk membaca saldo dan transaksi. FinFlow tidak memerlukan PIN, OTP, atau kata sandi bank untuk dicatat di aplikasi.")
        Text("Untuk sekarang, gunakan CSV ekspor atau template impor untuk membawa riwayat transaksi. Bank yang belum didukung tetap dapat dicatat sebagai dompet manual.")
        Button(onClick = onImport) { Text("Impor transaksi") }
        OutlinedButton(onClick = onNotifications) { Text("Impor dari notifikasi Android") }
        Text("Impor notifikasi menggunakan izin perangkat dan konfirmasi sebelum menyimpan transaksi. Fitur ini tidak menghubungkan API bank atau menjamin seluruh transaksi terbaca.")
    }
}
