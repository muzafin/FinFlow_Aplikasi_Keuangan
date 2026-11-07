package com.clonical.kasiva.core.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.clonical.kasiva.i18n.*
import com.clonical.kasiva.core.i18n.text
import dev.icerock.moko.resources.StringResource

@Composable
fun EditorDialog(title: String, busy: Boolean, dismiss: () -> Unit, save: () -> Unit,
    delete: (() -> Unit)? = null, error: String?, kind: EditorKind = EditorKind.Generic, content: @Composable ColumnScope.() -> Unit) {
    val scroll = rememberScrollState()
    val isBudget = kind == EditorKind.Budget
    LaunchedEffect(error) {
        if (error != null) { withFrameNanos { }; scroll.animateScrollTo(scroll.maxValue) }
    }
    Dialog(onDismissRequest = { if (!busy) dismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(Modifier.fillMaxSize().background(designBackground).safeDrawingPadding().imePadding().testTag("editor-page")) {
            // Ollo: editor dompet memakai judul di tengah dan tombol ✓ di kanan atas (tanpa tombol bawah).
            val headerSave = kind == EditorKind.Wallet
            if (headerSave) Box(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp)) {
                IconButton(onClick = { if (!busy) dismiss() }, Modifier.align(Alignment.CenterStart)) { FFIcon("chevron_left_rounded", FFColors.onSurface, 24.dp, MR.strings.common_back.text()) }
                Text(title, Modifier.align(Alignment.Center), style = FFType.appTitle.copy(fontSize = 20.sp, lineHeight = 26.sp), color = FFColors.onSurface)
                IconButton(onClick = save, enabled = !busy, modifier = Modifier.align(Alignment.CenterEnd).testTag("editor-save")) {
                    if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else FFIcon("hi_check", FFColors.onSurface, 24.dp, MR.strings.app_save_wallet.text())
                }
            } else DesignHeader(title, back = { if (!busy) dismiss() })
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                content()
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("editor-error")) }
                Spacer(Modifier.height(8.dp))
            }
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!headerSave) Button(enabled = !busy, onClick = save, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(24.dp)) {
                    if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Text(when (kind) { EditorKind.Budget -> MR.strings.app_save_budget.text(); EditorKind.Goal -> MR.strings.app_save_savings_goal.text(); EditorKind.Wallet -> MR.strings.app_save_wallet.text(); EditorKind.Generic -> MR.strings.common_save.text() })
                }
                if (isBudget) OutlinedButton(enabled = !busy, onClick = dismiss, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text(MR.strings.common_cancel.text()) }
                delete?.let { TextButton(enabled = !busy, onClick = it, modifier = Modifier.fillMaxWidth()) { Text(MR.strings.common_delete.text(), color = FFColors.expense) } }
            }
        }
    }
}

/** Jenis editor menentukan label tombol simpan (tidak lagi ditebak dari judul yang sudah diterjemahkan). */
enum class EditorKind { Generic, Budget, Goal, Wallet }
