package com.logikawan.finflow.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun EditorDialog(title: String, busy: Boolean, dismiss: () -> Unit, save: () -> Unit,
    delete: (() -> Unit)? = null, error: String?, content: @Composable ColumnScope.() -> Unit) {
    val scroll = rememberScrollState()
    LaunchedEffect(error) {
        if (error != null) { withFrameNanos { }; scroll.animateScrollTo(scroll.maxValue) }
    }
    Dialog(onDismissRequest = { if (!busy) dismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(Modifier.fillMaxSize().background(designBackground).safeDrawingPadding().imePadding().testTag("editor-page")) {
            DesignHeader(title, back = { if (!busy) dismiss() })
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                content()
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("editor-error")) }
                Spacer(Modifier.height(8.dp))
            }
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(enabled = !busy, onClick = save, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(24.dp)) {
                    if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Text(when { title.contains("Anggaran", true) -> "Simpan Anggaran"; title.contains("Target", true) -> "Simpan Target Tabungan"; title.contains("Dompet", true) -> "Simpan Dompet"; else -> "Simpan" })
                }
                if (title.contains("Anggaran", true)) OutlinedButton(enabled = !busy, onClick = dismiss, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Batal") }
                delete?.let { TextButton(enabled = !busy, onClick = it, modifier = Modifier.fillMaxWidth()) { Text("Hapus", color = FFColors.expense) } }
            }
        }
    }
}

