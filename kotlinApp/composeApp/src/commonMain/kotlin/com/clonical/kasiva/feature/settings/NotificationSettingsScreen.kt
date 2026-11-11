package com.clonical.kasiva.feature.settings

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonical.kasiva.core.platform.NativeServices
import com.clonical.kasiva.core.ui.FFColors
import com.clonical.kasiva.core.ui.FFIcon
import com.clonical.kasiva.core.ui.FFType
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.i18n.*

/**
 * Halaman Notifikasi bergaya Ollo: kotak info + riwayat notifikasi di perangkat (atau keadaan kosong),
 * lalu pengaturan pengingat harian dan peringatan anggaran yang benar-benar menjadwalkan notifikasi lewat [NativeServices].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettings(native: NativeServices) {
    var enabled by remember { mutableStateOf(native.reminderEnabled()) }
    var budgetEnabled by remember { mutableStateOf(native.budgetAlertsEnabled()) }
    var budgetBusy by remember { mutableStateOf(false) }
    var billEnabled by remember { mutableStateOf(native.billPushEnabled()) }
    var billBusy by remember { mutableStateOf(false) }
    var hour by remember { mutableIntStateOf(native.reminderHour()) }
    var minute by remember { mutableIntStateOf(native.reminderMinute()) }
    var busy by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val history = remember { readNotificationHistory(native) }
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
            error = failure.message ?: MR.strings.set_notif_save_failed.text()
            busy = false
        }
    }
    val switchColors = SwitchDefaults.colors(
        checkedThumbColor = FFColors.onPill, checkedTrackColor = FFColors.pill, checkedBorderColor = Color.Transparent,
        uncheckedThumbColor = FFColors.onSurfaceVariant, uncheckedTrackColor = FFColors.raised, uncheckedBorderColor = Color.Transparent,
    )
    Column(Modifier.fillMaxSize().background(FFColors.background).verticalScroll(rememberScrollState()).testTag("notifications-page")
        .padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (native.devicePlatform() == "android") {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(FFColors.surface).padding(16.dp)) {
                Text(MR.strings.set_notif_floating_title.text(), style = FFType.sectionTitle, color = FFColors.onSurface)
                Spacer(Modifier.height(8.dp))
                Text(MR.strings.set_notif_floating_desc.text(), style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
                TextButton(onClick = { native.openNotificationSettings { error = it } }, modifier = Modifier.testTag("notif-system-settings")) {
                    Text(MR.strings.set_notif_floating_manage.text())
                }
                listOf(
                    "announcements" to MR.strings.core_channel_announcements.text(),
                    "bills" to MR.strings.core_channel_bills.text(),
                    "daily" to MR.strings.core_channel_daily.text(),
                    "budget" to MR.strings.core_channel_budget.text(),
                ).forEach { (id, label) ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button) { native.openNotificationSettings(id) { error = it } }
                        .testTag("notif-channel-$id").padding(vertical = 12.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(label, Modifier.weight(1f), style = FFType.bodyMain, color = FFColors.onSurface)
                        FFIcon("hi_chevron_right", FFColors.onSurfaceVariant, 18.dp)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        NotificationHistoryBanner()
        if (history.isNotEmpty()) Spacer(Modifier.height(16.dp))
        NotificationHistoryList(history)
        Spacer(Modifier.height(8.dp))
        SectionTitle(MR.strings.set_notif_reminders.text())
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(FFColors.surface).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(MR.strings.set_notif_daily.text(), style = FFType.bodyMain.copy(fontSize = 16.sp, lineHeight = 22.sp), fontWeight = FontWeight.Medium, color = FFColors.onSurface)
                    Spacer(Modifier.height(2.dp))
                    Text(MR.strings.set_notif_daily_desc.text(), style = FFType.metaData.copy(fontSize = 13.sp, lineHeight = 18.sp), color = FFColors.onSurfaceVariant)
                }
                Spacer(Modifier.width(16.dp))
                Switch(checked = enabled, onCheckedChange = { update(it) }, enabled = !busy && !budgetBusy, colors = switchColors,
                    modifier = Modifier.testTag("notif-daily-switch"))
            }
            if (enabled) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(FFColors.background)
                    .clickable(enabled = !busy && !budgetBusy, role = Role.Button) { picking = true }.testTag("notif-time")
                    .padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    FFIcon("hi_clock", FFColors.onSurfaceVariant, 20.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(MR.strings.set_notif_time.text(), Modifier.weight(1f), style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
                    Text("${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}", style = FFType.bodyMain.copy(fontSize = 16.sp),
                        fontWeight = FontWeight.SemiBold, color = FFColors.onSurface)
                    Spacer(Modifier.width(6.dp))
                    FFIcon("hi_chevron_right", FFColors.onSurfaceVariant, 18.dp)
                }
            }
            if (busy) {
                Spacer(Modifier.height(12.dp))
                
            }
        }
        Spacer(Modifier.height(16.dp))
        SectionTitle(MR.strings.set_notif_alerts.text())
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(FFColors.surface).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(MR.strings.set_notif_budget.text(), style = FFType.bodyMain.copy(fontSize = 16.sp, lineHeight = 22.sp), fontWeight = FontWeight.Medium, color = FFColors.onSurface)
                    Spacer(Modifier.height(2.dp))
                    Text(MR.strings.set_notif_budget_desc.text(), style = FFType.metaData.copy(fontSize = 13.sp, lineHeight = 18.sp), color = FFColors.onSurfaceVariant)
                }
                Spacer(Modifier.width(16.dp))
                Switch(checked = budgetEnabled, onCheckedChange = { requested ->
                    budgetBusy = true
                    error = null
                    native.setBudgetAlerts(requested) { failure ->
                        budgetBusy = false
                        if (failure == null) budgetEnabled = native.budgetAlertsEnabled() else error = failure
                    }
                }, enabled = !budgetBusy && !busy, colors = switchColors, modifier = Modifier.testTag("notif-budget-switch"))
            }
            Spacer(Modifier.height(12.dp))
            Text(MR.strings.set_notif_budget_hint.text(), style = FFType.metaData, color = FFColors.onSurfaceVariant)
        }
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(FFColors.surface).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(MR.strings.set_notif_bills.text(), style = FFType.bodyMain.copy(fontSize = 16.sp, lineHeight = 22.sp), fontWeight = FontWeight.Medium, color = FFColors.onSurface)
                    Spacer(Modifier.height(2.dp))
                    Text(MR.strings.set_notif_bills_desc.text(), style = FFType.metaData.copy(fontSize = 13.sp, lineHeight = 18.sp), color = FFColors.onSurfaceVariant)
                }
                Spacer(Modifier.width(16.dp))
                Switch(checked = billEnabled, onCheckedChange = { requested ->
                    billBusy = true
                    error = null
                    native.setBillPush(requested) { failure ->
                        billBusy = false
                        if (failure == null) billEnabled = native.billPushEnabled() else error = failure
                    }
                }, enabled = !billBusy && !budgetBusy && !busy, colors = switchColors, modifier = Modifier.testTag("notif-bill-push-switch"))
            }
        }
        error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, style = FFType.bodyMain, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(24.dp))
    }
    if (picking) {
        val time = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
        AlertDialog(onDismissRequest = { picking = false },
            title = { Text(MR.strings.set_notif_time.text(), style = FFType.sectionTitle) },
            text = { TimePicker(state = time) },
            confirmButton = { TextButton(onClick = { picking = false; update(true, time.hour, time.minute) }) { Text(MR.strings.common_save.text()) } },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(MR.strings.common_cancel.text()) } },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, Modifier.padding(start = 4.dp, top = 8.dp, bottom = 10.dp), style = FFType.metaData.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = .4.sp),
        color = FFColors.onSurfaceVariant)
}
