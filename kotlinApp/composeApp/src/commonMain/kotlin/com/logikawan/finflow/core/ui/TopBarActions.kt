package com.logikawan.finflow.core.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Arrangement

/** Slot aksi di header sub-layar (seperti ⋮ / + di header Ollo). Disediakan oleh App. */
val LocalTopBarActions = staticCompositionLocalOf<MutableState<(@Composable RowScope.() -> Unit)?>?> { null }

/**
 * Menaruh aksi di header App. Bila layar dirender tanpa App (mis. pada uji), aksi tampil di dalam layar
 * pada baris rata kanan agar tetap dapat dipakai.
 */
@Composable
fun TopBarActions(content: @Composable RowScope.() -> Unit) {
    val slot = LocalTopBarActions.current
    if (slot == null) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically, content = content)
        return
    }
    val latest by rememberUpdatedState(content)
    DisposableEffect(slot) {
        val entry: @Composable RowScope.() -> Unit = { latest(this) }
        slot.value = entry
        onDispose { if (slot.value === entry) slot.value = null }
    }
}
