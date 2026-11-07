package com.logikawan.finflow.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logikawan.finflow.core.i18n.text
import com.logikawan.finflow.core.platform.NativeServices
import com.logikawan.finflow.core.ui.FFColors
import com.logikawan.finflow.core.ui.FFIcon
import com.logikawan.finflow.core.ui.FFType
import com.logikawan.finflow.i18n.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Riwayat notifikasi di perangkat (Ollo "Notifications"). Sisi native menambahkan entri ke setting
 * [NOTIFICATION_HISTORY_KEY] (JSON array [NotificationRecord], terbaru di akhir, maks. 200) setiap kali
 * pengingat/peringatan anggaran benar-benar dijadwalkan tampil. Layar ini hanya membaca; tidak ada data contoh.
 */
const val NOTIFICATION_HISTORY_KEY = "notification.history"

@Serializable
data class NotificationRecord(val title: String, val body: String = "", val at: Long, val kind: String = "reminder")

private val historyJson = Json { ignoreUnknownKeys = true }

fun readNotificationHistory(native: NativeServices): List<NotificationRecord> =
    runCatching { historyJson.decodeFromString<List<NotificationRecord>>(native.readSetting(NOTIFICATION_HISTORY_KEY) ?: "[]") }
        .getOrDefault(emptyList()).sortedByDescending { it.at }.take(200)

private fun formatWhen(millis: Long): String {
    val t = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
    fun two(v: Int) = v.toString().padStart(2, '0')
    return "${two(t.dayOfMonth)}/${two(t.monthNumber)}/${t.year} ${two(t.hour)}:${two(t.minute)}"
}

/** Kotak info abu Ollo di atas riwayat. */
@Composable
fun NotificationHistoryBanner() {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(FFColors.raised).padding(horizontal = 16.dp, vertical = 18.dp)) {
        FFIcon("hi_info", FFColors.onSurface, 22.dp)
        Spacer(Modifier.width(14.dp))
        Text(MR.strings.pb_notif_history_info.text(), style = FFType.bodyMain.copy(fontSize = 15.sp, lineHeight = 22.sp), color = FFColors.onSurfaceVariant)
    }
}

/** Daftar riwayat atau keadaan kosong (lonceng + teks). */
@Composable
fun NotificationHistoryList(records: List<NotificationRecord>) {
    if (records.isEmpty()) {
        Column(Modifier.fillMaxWidth().padding(vertical = 40.dp).testTag("notification-history-empty"), horizontalAlignment = Alignment.CenterHorizontally) {
            FFIcon("hi_bell", FFColors.onSurfaceVariant, 40.dp)
            Spacer(Modifier.height(20.dp))
            Text(MR.strings.pb_notif_history_empty.text(), style = FFType.bodyMain.copy(fontSize = 16.sp, lineHeight = 22.sp),
                color = FFColors.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        return
    }
    Column(Modifier.fillMaxWidth().testTag("notification-history"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        records.forEach { record ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(FFColors.surface).padding(16.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(50)).background(FFColors.iconCircle), contentAlignment = Alignment.Center) {
                    FFIcon(if (record.kind == "budget") "hi_percent" else "hi_bell", FFColors.onSurface, 18.dp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(record.title, style = FFType.bodyMain, fontWeight = FontWeight.Medium, color = FFColors.onSurface)
                    if (record.body.isNotBlank()) Text(record.body, style = FFType.metaData, color = FFColors.onSurfaceVariant)
                    Text(formatWhen(record.at), style = FFType.metaData.copy(fontSize = 12.sp), color = FFColors.outline)
                }
            }
        }
    }
}
