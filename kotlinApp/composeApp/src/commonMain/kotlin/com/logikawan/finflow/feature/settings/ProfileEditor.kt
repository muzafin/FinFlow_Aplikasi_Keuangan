package com.logikawan.finflow.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.logikawan.finflow.core.model.Profile
import com.logikawan.finflow.core.ui.EditorDialog
import com.logikawan.finflow.core.i18n.text
import com.logikawan.finflow.i18n.*

@Composable
fun ProfileEditor(profile: Profile?, busy: Boolean, dismiss: () -> Unit, save: (Profile) -> Unit) {
    var name by remember { mutableStateOf(profile?.name.orEmpty()) }
    var currency by remember { mutableStateOf(profile?.baseCurrency ?: "IDR") }
    var error by remember { mutableStateOf<String?>(null) }
    EditorDialog(MR.strings.set_profile_editor_title.text(), busy, dismiss, save = {
        val current = profile
        val code = currency.trim().uppercase()
        error = when {
            current == null -> MR.strings.set_profile_err_unavailable.text()
            name.trim().isEmpty() || name.trim().length > 100 -> MR.strings.set_profile_err_name.text()
            !code.matches(Regex("[A-Z]{3}")) -> MR.strings.set_profile_err_currency.text()
            else -> null
        }
        if (error == null && current != null) save(current.copy(name = name.trim(), baseCurrency = code))
    }, error = error) {
        OutlinedTextField(name, { name = it }, enabled = !busy, label = { Text(MR.strings.set_profile_full_name.text()) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(currency, { currency = it.uppercase().take(3) }, enabled = !busy, label = { Text(MR.strings.set_profile_base_currency.text()) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Text(profile?.email.orEmpty(), style = MaterialTheme.typography.bodySmall)
        Text(MR.strings.set_profile_currency_hint.text(), style = MaterialTheme.typography.bodySmall)
    }
}
