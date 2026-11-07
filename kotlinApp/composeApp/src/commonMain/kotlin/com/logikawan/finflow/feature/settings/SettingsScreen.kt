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
import com.logikawan.finflow.core.i18n.text
import com.logikawan.finflow.i18n.*

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
            Text(MR.strings.set_app_lock.text(), modifier = Modifier.weight(1f))
            Switch(locked, { requested -> native.unlock { ok -> if (ok) { locked = requested; native.setLockEnabled(requested) } else message = MR.strings.set_auth_cancelled.text() } })
        }
        Text(MR.strings.set_app_lock_hint.text(), style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.CenterVertically) { Text(MR.strings.set_daily_reminder.text(), Modifier.weight(1f)); Switch(reminder, { reminder = it }) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Field(MR.strings.set_hour_field.text(), hour, { hour = it }, Modifier.weight(1f))
            Field(MR.strings.set_minute_field.text(), minute, { minute = it }, Modifier.weight(1f))
        }
        Button(onClick = {
            val h = hour.toIntOrNull(); val m = minute.toIntOrNull()
            if (h == null || m == null || h !in 0..23 || m !in 0..59) message = MR.strings.set_invalid_time.text()
            else native.setReminder(reminder, h, m) { error -> message = error ?: MR.strings.set_reminder_saved.text(); reminder = native.reminderEnabled() }
        }) { Text(MR.strings.set_save_reminder.text()) }
        message?.let { Text(it) }
        OutlinedButton(onClick = addCategory) { Text(MR.strings.set_add_category.text()) }
        OutlinedButton(onClick = { native.shareCsv("finflow-transactions.csv", CsvImport.export(data)) }) { Text(MR.strings.set_export_all_csv.text()) }
        Text(MR.strings.set_csv_not_backup.text(), style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = logout) { Text(MR.strings.set_logout.text()) }
    }
}
