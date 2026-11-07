package com.clonical.kasiva.feature.connections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.ui.FFColors
import com.clonical.kasiva.core.ui.FFIcon
import com.clonical.kasiva.core.ui.FFType
import com.clonical.kasiva.i18n.*

/** Koneksi bank belum tersedia: jelaskan apa adanya dan arahkan ke impor CSV/notifikasi yang benar-benar berfungsi. */
@Composable
fun BankConnectionScreen(onImport: () -> Unit, onNotifications: () -> Unit) {
    Column(Modifier.fillMaxSize().background(FFColors.background).verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 24.dp).testTag("bank-connection"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(FFColors.surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(FFColors.iconCircle), contentAlignment = Alignment.Center) { FFIcon("hi_bank", FFColors.onSurface, 22.dp) }
                Spacer(Modifier.width(14.dp))
                Text(MR.strings.set_bank_title.text(), style = FFType.bodyMain.copy(fontSize = 18.sp, lineHeight = 24.sp), fontWeight = FontWeight.Medium, color = FFColors.onSurface)
            }
            Text(MR.strings.set_bank_not_enabled.text(), style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
            Text(MR.strings.set_bank_read_only.text(), style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
            Text(MR.strings.set_bank_for_now.text(), style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
        }
        ActionRow("hi_upload", MR.strings.set_bank_import.text(), null, "bank-import", onImport)
        ActionRow("hi_bell", MR.strings.set_bank_import_notifications.text(), MR.strings.set_bank_notifications_hint.text(), "bank-import-notifications", onNotifications)
    }
}

@Composable
private fun ActionRow(icon: String, title: String, caption: String?, tag: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(FFColors.surface).clickable(role = Role.Button, onClick = onClick)
        .padding(16.dp).testTag(tag), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(FFColors.iconCircle), contentAlignment = Alignment.Center) { FFIcon(icon, FFColors.onSurface, 22.dp) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = FFType.bodyMain.copy(fontSize = 16.sp, lineHeight = 22.sp), fontWeight = FontWeight.Medium, color = FFColors.onSurface)
            caption?.let { Text(it, style = FFType.metaData, color = FFColors.onSurfaceVariant) }
        }
        FFIcon("hi_chevron_right", FFColors.onSurfaceVariant, 20.dp)
    }
}
