package com.logikawan.finflow.feature.connections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.logikawan.finflow.core.i18n.text
import com.logikawan.finflow.i18n.*

@Composable
fun BankConnectionScreen(onImport: () -> Unit, onNotifications: () -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(MR.strings.set_bank_title.text(), style = MaterialTheme.typography.headlineSmall)
        Text(MR.strings.set_bank_not_enabled.text())
        Text(MR.strings.set_bank_read_only.text())
        Text(MR.strings.set_bank_for_now.text())
        Button(onClick = onImport) { Text(MR.strings.set_bank_import.text()) }
        OutlinedButton(onClick = onNotifications) { Text(MR.strings.set_bank_import_notifications.text()) }
        Text(MR.strings.set_bank_notifications_hint.text())
    }
}
