package com.logikawan.finflow.feature.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logikawan.finflow.core.ui.FFColors
import com.logikawan.finflow.core.ui.FFIcon
import com.logikawan.finflow.core.ui.FFType

private val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

private fun Modifier.sheetPadding() = padding(start = 20.dp, end = 20.dp, bottom = 28.dp)

@Composable
private fun SheetGrip() = Box(
    Modifier.padding(top = 10.dp, bottom = 14.dp).size(36.dp, 4.dp)
        .background(FFColors.outline, RoundedCornerShape(2.dp)),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddButtonIntroSheet(suggested: String = "all", onChoose: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = FFColors.canvas, shape = sheetShape,
        dragHandle = { SheetGrip() }, contentWindowInsets = { WindowInsets(0, 0, 0, 0) }) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().sheetPadding().testTag("add-button-intro")) {
            Text("Tombol + mau buka apa?", style = FFType.appTitle.copy(fontSize = 22.sp), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Pilih salah satu. Bisa diganti kapan saja di Tampilan Tambah Transaksi.",
                style = FFType.metaData, color = FFColors.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AddModeCard(suggested == "all", "Semua cara mencatat",
                    listOf("Rekam suara atau scan", "Impor notifikasi bank", "Tambah manual"),
                    Modifier.weight(1f).testTag("add-mode-all")) { onChoose("all") }
                AddModeCard(suggested == "quick", "Langsung tambah transaksi",
                    listOf("Buka editor transaksi", "Isi nominal lalu simpan"),
                    Modifier.weight(1f).testTag("add-mode-quick")) { onChoose("quick") }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally).testTag("add-mode-later")) {
                Text("Nanti saja", color = FFColors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AddModeCard(selected: Boolean, title: String, lines: List<String>, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.semantics { stateDescription = if (selected) "Disarankan" else "Pilihan" }) {
        Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = FFColors.background,
            border = androidx.compose.foundation.BorderStroke(if (selected) 2.dp else 1.dp,
                if (selected) FFColors.onSurface else FFColors.outlineVariant)) {
            Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(FFColors.surface)
                    .padding(horizontal = 12.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    lines.forEach { line ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(FFColors.outline))
                            Spacer(Modifier.width(8.dp))
                            Text(line, style = FFType.badgeLabel.copy(fontSize = 9.sp, letterSpacing = 0.sp),
                                color = FFColors.onSurfaceVariant, maxLines = 1)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.size(32.dp).clip(CircleShape).background(FFColors.onSurface), contentAlignment = Alignment.Center) {
                    FFIcon("add_rounded", Color.White, 18.dp, contentDescription = null)
                }
                Spacer(Modifier.height(4.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(title, style = FFType.bodyMain, fontWeight = FontWeight.Medium, color = FFColors.onSurface)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordMethodsSheet(onAddTransaction: () -> Unit, onImportNotification: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = FFColors.canvas, shape = sheetShape,
        dragHandle = { SheetGrip() }, contentWindowInsets = { WindowInsets(0, 0, 0, 0) }) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().sheetPadding().testTag("record-methods")) {
            Text("Cara mencatat", style = FFType.appTitle.copy(fontSize = 22.sp), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Pilih metode pencatatan. Metode yang belum tersedia ditandai.", style = FFType.metaData, color = FFColors.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            MethodRow("Rekam suara", "Belum tersedia", "notes_outlined", "record-voice", enabled = false, onClick = {})
            Spacer(Modifier.height(10.dp))
            MethodRow("Scan struk", "Belum tersedia", "receipt_long_outlined", "record-scan", enabled = false, onClick = {})
            Spacer(Modifier.height(10.dp))
            MethodRow("Impor notifikasi", "Dari notifikasi bank atau e-wallet yang diizinkan", "notifications_outlined", "record-notification") {
                onImportNotification(); onDismiss()
            }
            Spacer(Modifier.height(10.dp))
            MethodRow("Tambah manual", "Isi transaksi dengan keypad dan kategori", "add_rounded", "record-manual") {
                onAddTransaction(); onDismiss()
            }
        }
    }
}

@Composable
private fun MethodRow(title: String, caption: String, icon: String, tag: String, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(18.dp), color = FFColors.background,
        border = androidx.compose.foundation.BorderStroke(1.dp, FFColors.outlineVariant),
        modifier = Modifier.fillMaxWidth().testTag(tag)) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(FFColors.surface), contentAlignment = Alignment.Center) {
                FFIcon(icon, if (enabled) FFColors.onSurface else FFColors.outline, 22.dp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = FFType.bodyMain, fontWeight = FontWeight.Medium,
                    color = if (enabled) FFColors.onSurface else FFColors.onSurfaceVariant)
                Text(caption, style = FFType.metaData, color = FFColors.onSurfaceVariant, maxLines = 2)
            }
            if (enabled) FFIcon("chevron_right_rounded", FFColors.onSurfaceVariant, 20.dp)
            else Text("Segera", style = FFType.badgeLabel, color = FFColors.outline)
        }
    }
}
