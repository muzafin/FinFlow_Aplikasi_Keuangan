package com.logikawan.finflow.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.logikawan.finflow.core.model.Profile
import com.logikawan.finflow.core.ui.EditorDialog

@Composable
fun ProfileEditor(profile: Profile?, busy: Boolean, dismiss: () -> Unit, save: (Profile) -> Unit) {
    var name by remember { mutableStateOf(profile?.name.orEmpty()) }
    var currency by remember { mutableStateOf(profile?.baseCurrency ?: "IDR") }
    var error by remember { mutableStateOf<String?>(null) }
    EditorDialog("Profil Akun", busy, dismiss, save = {
        val current = profile
        val code = currency.trim().uppercase()
        error = when {
            current == null -> "Profil belum tersedia. Tutup formulir lalu muat ulang data."
            name.trim().isEmpty() || name.trim().length > 100 -> "Nama wajib diisi, maksimal 100 karakter"
            !code.matches(Regex("[A-Z]{3}")) -> "Gunakan kode mata uang 3 huruf"
            else -> null
        }
        if (error == null && current != null) save(current.copy(name = name.trim(), baseCurrency = code))
    }, error = error) {
        OutlinedTextField(name, { name = it }, enabled = !busy, label = { Text("Nama lengkap") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(currency, { currency = it.uppercase().take(3) }, enabled = !busy, label = { Text("Mata uang utama (IDR, USD, EUR)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Text(profile?.email.orEmpty(), style = MaterialTheme.typography.bodySmall)
        Text("Mata uang utama menentukan urutan tampilan. Nominal dan mata uang dompet yang sudah ada tetap sama; saldo lintas mata uang tidak dijumlahkan.", style = MaterialTheme.typography.bodySmall)
    }
}
