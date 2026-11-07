package com.clonical.kasiva.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.platform.NativeServices
import com.clonical.kasiva.core.ui.*
import com.clonical.kasiva.i18n.*

/**
 * Halaman "Hapus Data" seperti Ollo: peringatan, hapus transaksi saja, reset pabrik, dan permintaan hapus data server.
 * [factoryReset] membuka dialog konfirmasi yang sudah ada ([DeleteAccountDialog] dengan dataOnly=true).
 * [deleteTransactions] null berarti repositori belum menyediakan penghapusan riwayat saja; tombolnya dinonaktifkan.
 */
@Composable
fun AccountDataDeleteScreen(native: NativeServices, email: String, localMode: Boolean, busy: Boolean,
    factoryReset: () -> Unit, deleteTransactions: (() -> Unit)?) {
    var confirmHistory by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 20.dp).testTag("delete-data-page"),
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
        AdBanner(AdIcons.alert, MR.strings.pa_delete_warning_body.text(), AdTone.dangerBg, AdTone.danger,
            title = MR.strings.pa_delete_warning_title.text(), bodyColor = FFColors.onSurfaceVariant)
        DeleteOptionCard("hi_clock", AdTone.orangeBg, AdTone.orange, MR.strings.pa_delete_tx_title.text(), MR.strings.pa_delete_tx_body.text()) {
            DeleteActionButton(MR.strings.pa_delete_tx_button.text(), filled = false, enabled = deleteTransactions != null && !busy, tag = "delete-history") { confirmHistory = true }
            if (deleteTransactions == null) { Spacer(Modifier.height(8.dp)); AdBody(MR.strings.pa_delete_tx_unavailable.text(), size = 12.sp) }
        }
        DeleteOptionCard("hi_delete", AdTone.redCircleBg, AdTone.danger, MR.strings.pa_factory_title.text(),
            if (localMode) MR.strings.pa_factory_body_local.text() else MR.strings.pa_factory_body_cloud.text()) {
            DeleteActionButton(MR.strings.pa_factory_button.text(), filled = true, enabled = !busy, tag = "factory-reset", onClick = factoryReset)
        }
        DeleteOptionCard(AdIcons.cloud, FFColors.iconCircle, FFColors.onSurfaceVariant, MR.strings.pa_server_title.text(),
            if (localMode) MR.strings.pa_server_body_local.text() else MR.strings.pa_server_body_cloud.text()) {
            DeleteActionButton(MR.strings.pa_contact_cs.text(), filled = false, enabled = true, tag = "delete-contact-cs") {
                message = native.composeSupport("Kasiva · ${MR.strings.pa_server_title.text()}",
                    "${MR.strings.pa_server_mail_body.text()}\n\n${if (localMode) MR.strings.set_local_mode.text() else email}\nKasiva ${native.appVersion()}\n${native.deviceDescription()}")
                    ?: MR.strings.set_bug_draft_opened.text()
            }
        }
        message?.let { AdBody(it, color = FFColors.onSurface) }
        Spacer(Modifier.height(8.dp))
    }
    if (confirmHistory && deleteTransactions != null) AdInfoDialog(MR.strings.pa_delete_tx_confirm_title.text(),
        listOf(MR.strings.pa_delete_tx_body.text(), MR.strings.pa_delete_warning_body.text()), { confirmHistory = false },
        confirmLabel = MR.strings.pa_delete_tx_button.text(), onConfirm = { confirmHistory = false; deleteTransactions() }, danger = true)
}

@Composable
private fun DeleteOptionCard(icon: String, circle: Color, tint: Color, title: String, body: String, action: @Composable ColumnScope.() -> Unit) {
    AdCard(padding = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AdIconCircle(icon, circle, tint, 48.dp, 22.dp)
            Spacer(Modifier.width(18.dp))
            Text(title, Modifier.weight(1f), style = adText(18.sp, FontWeight.SemiBold, 24.sp), color = FFColors.onSurface)
        }
        Spacer(Modifier.height(20.dp))
        AdBody(body, size = 15.sp)
        Spacer(Modifier.height(20.dp))
        action()
    }
}

@Composable
private fun DeleteActionButton(label: String, filled: Boolean, enabled: Boolean, tag: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val danger = AdTone.danger
    Box(Modifier.fillMaxWidth().height(52.dp).clip(shape)
        .then(if (filled) Modifier.background(if (enabled) danger else danger.copy(alpha = .4f)) else Modifier.border(1.dp, FFColors.hairline, shape))
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick).testTag(tag), contentAlignment = Alignment.Center) {
        Text(label, style = adText(16.sp, FontWeight.Medium, 22.sp),
            color = when { filled -> Color.White; enabled -> FFColors.onSurface; else -> FFColors.onSurfaceVariant.copy(alpha = .5f) })
    }
}
