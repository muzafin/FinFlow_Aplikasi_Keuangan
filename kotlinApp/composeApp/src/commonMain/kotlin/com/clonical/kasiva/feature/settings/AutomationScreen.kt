package com.clonical.kasiva.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.platform.NativeServices
import com.clonical.kasiva.core.ui.FFColors
import com.clonical.kasiva.core.ui.FFIcon
import com.clonical.kasiva.core.ui.FFType
import com.clonical.kasiva.i18n.*

/**
 * Ollo "Automation & Integration": kartu Coming Soon di atas halaman Profil. Fitur ini belum ada, jadi tombol utama
 * hanya menutup kartu (tanpa penghitung minat palsu); tautan kecil membuka draf email masukan agar minat tercatat jujur.
 */
@Composable
fun AutomationComingSoonDialog(native: NativeServices, onDismiss: () -> Unit) {
    var failure by remember { mutableStateOf<String?>(null) }
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(36.dp)).background(FFColors.surface)
            .padding(horizontal = 20.dp, vertical = 22.dp).testTag("automation-coming-soon"),
            horizontalAlignment = Alignment.CenterHorizontally) {
            // Lingkaran 64dp gelap (terang di mode gelap) dengan ikon roket seperti Ollo.
            Box(Modifier.size(64.dp).clip(CircleShape).background(FFColors.pill), contentAlignment = Alignment.Center) {
                FFIcon("hi_bulb", FFColors.onPill, 30.dp)
            }
            Spacer(Modifier.height(20.dp))
            Text(MR.strings.pd_auto_coming_soon.text(), style = FFType.bodyMain.copy(fontSize = 22.sp, lineHeight = 28.sp),
                fontWeight = FontWeight.SemiBold, color = FFColors.onSurface, textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            Text(MR.strings.pd_auto_title.text(), style = FFType.bodyMain.copy(fontSize = 18.sp, lineHeight = 24.sp),
                fontWeight = FontWeight.Medium, color = FFColors.onSurface, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(MR.strings.pd_auto_body.text(), style = FFType.bodyMain.copy(fontSize = 16.sp, lineHeight = 20.sp),
                color = FFColors.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(26.dp))
            Button(onClick = onDismiss, Modifier.fillMaxWidth().height(40.dp).testTag("automation-cant-wait"), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FFColors.pill, contentColor = FFColors.onPill), contentPadding = PaddingValues(0.dp)) {
                Text(MR.strings.pd_auto_cant_wait.text(), style = FFType.bodyMain.copy(fontSize = 16.sp))
            }
            TextButton(onClick = {
                failure = native.composeSupport("Kasiva · ${MR.strings.pd_auto_title.text()}", MR.strings.pd_auto_email_body.text())
                if (failure == null) onDismiss()
            }, Modifier.testTag("automation-tell-us")) {
                Text(MR.strings.pd_auto_tell_us.text(), style = FFType.metaData, color = FFColors.onSurfaceVariant, textAlign = TextAlign.Center)
            }
            failure?.let { Text(it, style = FFType.metaData, color = FFColors.expense, textAlign = TextAlign.Center) }
        }
    }
}
