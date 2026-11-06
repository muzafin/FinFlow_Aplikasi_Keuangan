package com.logikawan.finflow.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logikawan.finflow.core.platform.NativeServices
import com.logikawan.finflow.core.ui.FFColors
import com.logikawan.finflow.core.ui.FFType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettings(native: NativeServices) {
    var enabled by remember { mutableStateOf(native.reminderEnabled()) }
    var budgetEnabled by remember { mutableStateOf(native.budgetAlertsEnabled()) }
    var budgetBusy by remember { mutableStateOf(false) }
    var hour by remember { mutableIntStateOf(native.reminderHour()) }
    var minute by remember { mutableIntStateOf(native.reminderMinute()) }
    var busy by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    fun update(on: Boolean, newHour: Int = hour, newMinute: Int = minute) {
        if (busy || budgetBusy) return
        busy = true
        error = null
        try {
            native.setReminder(on, newHour, newMinute) { failure ->
                if (failure == null) {
                    enabled = on
                    hour = newHour
                    minute = newMinute
                } else error = failure
                busy = false
            }
        } catch (failure: Exception) {
            error = failure.message ?: "Pengaturan pengingat belum dapat disimpan."
            busy = false
        }
    }
    Column(Modifier.fillMaxSize().background(FFColors.background).verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Pengingat", style = FFType.sectionTitle.copy(fontSize = 16.sp), color = FFColors.primary)
        Spacer(Modifier.height(12.dp))
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(.5.dp, FFColors.outlineVariant)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Pengingat Harian", style = FFType.bodyMain, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text("Ingatkan saya untuk mencatat transaksi", style = FFType.metaData, color = FFColors.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(16.dp))
                    Switch(checked = enabled, onCheckedChange = { update(it) }, enabled = !busy && !budgetBusy,
                        colors = SwitchDefaults.colors(checkedThumbColor = FFColors.primary, checkedTrackColor = FFColors.primaryFixed))
                }
                if (enabled) {
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Waktu Pengingat", style = FFType.metaData, color = FFColors.onSurfaceVariant)
                        Text("${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}", style = FFType.badgeLabel, color = FFColors.primary, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(FFColors.primaryContainer.copy(alpha = .1f)).border(1.dp, FFColors.primary, RoundedCornerShape(8.dp))
                                .clickable(enabled = !busy && !budgetBusy) { picking = true }.padding(horizontal = 12.dp, vertical = 6.dp))
                    }
                }
                if (busy) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 16.dp), thickness = .5.dp, color = FFColors.outlineVariant)
        Text("Peringatan", style = FFType.sectionTitle.copy(fontSize = 16.sp), color = FFColors.primary)
        Spacer(Modifier.height(12.dp))
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(.5.dp, FFColors.outlineVariant)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Peringatan Anggaran", style = FFType.bodyMain, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text("Beri tahu jika pengeluaran mendekati batas anggaran bulanan", style = FFType.metaData, color = FFColors.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(16.dp))
                    Switch(checked = budgetEnabled, onCheckedChange = { requested ->
                        budgetBusy = true
                        error = null
                        native.setBudgetAlerts(requested) { failure ->
                            budgetBusy = false
                            if (failure == null) budgetEnabled = native.budgetAlertsEnabled() else error = failure
                        }
                    }, enabled = !budgetBusy && !busy,
                        colors = SwitchDefaults.colors(checkedThumbColor = FFColors.primary, checkedTrackColor = FFColors.primaryFixed))
                }
                Spacer(Modifier.height(12.dp))
                Text("Peringatan pada 80% dan 100%, diperiksa saat aplikasi dibuka atau data berubah. Setiap batas diberitahukan sekali per anggaran dan bulan.", style = FFType.metaData, color = FFColors.onSurfaceVariant)
            }
        }
        error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, style = FFType.bodyMain, color = MaterialTheme.colorScheme.error)
        }
    }
    if (picking) {
        val time = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
        AlertDialog(onDismissRequest = { picking = false },
            title = { Text("Waktu Pengingat", style = FFType.sectionTitle) },
            text = { TimePicker(state = time) },
            confirmButton = { TextButton(onClick = { picking = false; update(true, time.hour, time.minute) }) { Text("Simpan") } },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Batal") } },
        )
    }
}
