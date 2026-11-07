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
import com.logikawan.finflow.i18n.*
import com.logikawan.finflow.core.i18n.text
import dev.icerock.moko.resources.StringResource

@Composable
fun EditorDialog(title: String, busy: Boolean, dismiss: () -> Unit, save: () -> Unit,
    delete: (() -> Unit)? = null, error: String?, content: @Composable ColumnScope.() -> Unit) {
    val scroll = rememberScrollState()
    // Judul editor sudah diterjemahkan; cocokkan kata kunci Indonesia maupun bahasa aktif.
    fun titleHas(id: String, keyword: StringResource) = title.contains(id, true) || title.contains(keyword.text(), true)
    val isBudget = titleHas("Anggaran", MR.strings.app_kw_budget)
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
                    else Text(when { isBudget -> MR.strings.app_save_budget.text(); titleHas("Target", MR.strings.app_kw_goal) -> MR.strings.app_save_savings_goal.text(); titleHas("Dompet", MR.strings.app_kw_wallet) -> MR.strings.app_save_wallet.text(); else -> MR.strings.common_save.text() })
                }
                if (isBudget) OutlinedButton(enabled = !busy, onClick = dismiss, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text(MR.strings.common_cancel.text()) }
                delete?.let { TextButton(enabled = !busy, onClick = it, modifier = Modifier.fillMaxWidth()) { Text(MR.strings.common_delete.text(), color = FFColors.expense) } }
            }
        }
    }
}

