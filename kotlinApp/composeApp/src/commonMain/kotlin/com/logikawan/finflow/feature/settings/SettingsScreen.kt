package com.logikawan.finflow.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.logikawan.finflow.core.model.FinanceData
import com.logikawan.finflow.core.platform.NativeServices
import com.logikawan.finflow.core.ui.Field
import com.logikawan.finflow.feature.importdata.CsvImport

@Composable
fun Settings(native: NativeServices, email: String, data: FinanceData, addCategory: () -> Unit, logout: () -> Unit) {
    var locked by remember { mutableStateOf(native.lockEnabled()) }
    var reminder by remember { mutableStateOf(native.reminderEnabled()) }
    var hour by rememberSaveable { mutableStateOf(native.reminderHour().toString().padStart(2, '0')) }
    var minute by rememberSaveable { mutableStateOf(native.reminderMinute().toString().padStart(2, '0')) }
    var message by remember { mutableStateOf<String?>(null) }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(email, style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Kunci aplikasi", modifier = Modifier.weight(1f))
            Switch(locked, { requested -> native.unlock { ok -> if (ok) { locked = requested; native.setLockEnabled(requested) } else message = "Autentikasi perangkat dibatalkan atau tidak tersedia" } })
        }
        Text("Kunci memakai biometrik atau kode perangkat. FinFlow tidak menyimpan sidik jari atau PIN perangkat.", style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.CenterVertically) { Text("Pengingat harian", Modifier.weight(1f)); Switch(reminder, { reminder = it }) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Field("Jam 0–23", hour, { hour = it }, Modifier.weight(1f))
            Field("Menit 0–59", minute, { minute = it }, Modifier.weight(1f))
        }
        Button(onClick = {
            val h = hour.toIntOrNull(); val m = minute.toIntOrNull()
            if (h == null || m == null || h !in 0..23 || m !in 0..59) message = "Masukkan jam dan menit yang valid"
            else native.setReminder(reminder, h, m) { error -> message = error ?: "Pengingat disimpan"; reminder = native.reminderEnabled() }
        }) { Text("Simpan pengingat") }
        message?.let { Text(it) }
        OutlinedButton(onClick = addCategory) { Text("Tambah kategori") }
        OutlinedButton(onClick = { native.shareCsv("finflow-transactions.csv", CsvImport.export(data)) }) { Text("Ekspor semua transaksi CSV") }
        Text("CSV transaksi tidak menggantikan backup lengkap dompet, saldo awal, anggaran, target, dan investasi.", style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = logout) { Text("Keluar") }
    }
}
