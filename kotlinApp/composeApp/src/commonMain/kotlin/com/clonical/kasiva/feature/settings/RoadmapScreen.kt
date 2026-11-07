package com.clonical.kasiva.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.ui.*
import com.clonical.kasiva.i18n.*

/**
 * Ollo "Product Roadmap": tiga tab teks (Direncanakan / Sedang Dikerjakan / Selesai) dengan garis bawah selebar teks,
 * lalu kartu judul + badge bagian + deskripsi. Pencarian, salin semua dan kirim masukan (khas Kasiva) dipindah ke
 * aksi header agar susunan halaman sama dengan Ollo.
 */
@Composable
fun RoadmapScreen(sendFeedback: () -> Unit) {
    var status by rememberSaveable { mutableStateOf(RoadmapStatus.Planned) }
    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<RoadmapFeature?>(null) }
    var copied by remember { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboardManager.current
    val listState = rememberLazyListState()
    LaunchedEffect(status, query) { listState.scrollToItem(0) }
    val features = productRoadmap.filter { it.status == status &&
        (query.isBlank() || "${it.id} ${it.title} ${it.displayTitle} ${it.area} ${it.displayArea} ${it.description} ${it.next}".contains(query.trim(), true)) }
    TopBarActions {
        IconButton({ searching = !searching; if (!searching) query = "" }, Modifier.testTag("roadmap-search-toggle")) {
            FFIcon("hi_search", FFColors.onSurface, 22.dp, contentDescription = MR.strings.pd_rm_search_toggle.text())
        }
        Box {
            IconButton({ menu = true }, Modifier.testTag("roadmap-menu")) {
                FFIcon("hi_more", FFColors.onSurface, 22.dp, contentDescription = MR.strings.pd_rm_menu.text())
            }
            DropdownMenu(menu, { menu = false }, containerColor = FFColors.surface) {
                DropdownMenuItem(text = { Text(MR.strings.rm_copy_all.text(), style = FFType.bodyMain, color = FFColors.onSurface) }, onClick = {
                    menu = false; clipboard.setText(AnnotatedString(roadmapPrompt())); copied = MR.strings.rm_copied_all.text()
                }, modifier = Modifier.testTag("roadmap-copy-all"))
                DropdownMenuItem(text = { Text(MR.strings.rm_send_feedback.text(), style = FFType.bodyMain, color = FFColors.onSurface) },
                    onClick = { menu = false; sendFeedback() }, modifier = Modifier.testTag("roadmap-send-feedback"))
            }
        }
    }
    Column(Modifier.fillMaxSize().background(FFColors.background).testTag("product-roadmap")) {
        RoadmapTabs(status) { status = it; copied = null }
        if (searching) Field(MR.strings.rm_search_hint.text(), query, { query = it }, Modifier.padding(horizontal = 16.dp).padding(top = 12.dp))
        LazyColumn(Modifier.weight(1f).testTag("roadmap-list"), state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            copied?.let { message -> item(key = "copied") {
                Text(MR.strings.rm_copied_hint.text(message), Modifier.fillMaxWidth().clickable { copied = null },
                    style = FFType.metaData, color = FFColors.onSurface)
            } }
            items(features, key = { it.id }) { feature -> RoadmapCard(feature) { selected = feature } }
            if (features.isEmpty()) item(key = "empty") { Text(MR.strings.rm_empty.text(), style = FFType.bodyMain, color = FFColors.onSurfaceVariant) }
        }
    }
    selected?.let { feature ->
        AlertDialog(onDismissRequest = { selected = null }, containerColor = FFColors.surface, title = { Text(feature.displayTitle, color = FFColors.onSurface) }, text = {
            SelectionContainer {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${feature.id} • ${feature.status.displayLabel} • ${feature.displayArea}", style = FFType.metaData, color = FFColors.onSurfaceVariant)
                    Text(feature.description, color = FFColors.onSurface)
                    Text(if (feature.status == RoadmapStatus.Done) MR.strings.rm_scope_available.text() else MR.strings.rm_next_target.text(),
                        fontWeight = FontWeight.SemiBold, color = FFColors.onSurface)
                    Text(feature.next, color = FFColors.onSurface)
                }
            }
        }, confirmButton = { TextButton(onClick = {
            clipboard.setText(AnnotatedString(feature.prompt())); selected = null; copied = MR.strings.rm_copied_prompt.text(feature.id)
        }) { Text(MR.strings.rm_copy_prompt.text(), color = FFColors.onSurface) } },
            dismissButton = { TextButton(onClick = { selected = null }) { Text(MR.strings.common_close.text(), color = FFColors.onSurfaceVariant) } })
    }
}

/** Tab teks 17sp; indikator 3dp selebar teks di atas garis rambut, seperti Ollo. */
@Composable
private fun RoadmapTabs(status: RoadmapStatus, select: (RoadmapStatus) -> Unit) {
    val density = LocalDensity.current
    Box(Modifier.fillMaxWidth()) {
        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(1.dp).background(FFColors.hairline))
        Row(Modifier.fillMaxWidth()) {
            RoadmapStatus.entries.forEach { tab ->
                val active = tab == status
                var textWidth by remember { mutableStateOf(0.dp) }
                Column(Modifier.weight(1f).selectable(active, role = Role.Tab) { select(tab) }.testTag("roadmap-tab-${tab.name}"),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(14.dp))
                    Text(tab.displayLabel, Modifier.onSizeChanged { textWidth = with(density) { it.width.toDp() } },
                        style = FFType.bodyMain.copy(fontSize = 17.sp, lineHeight = 22.sp), maxLines = 1,
                        color = if (active) FFColors.onSurface else FFColors.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Box(Modifier.width(textWidth).height(3.dp).clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(if (active) FFColors.onSurface else androidx.compose.ui.graphics.Color.Transparent))
                }
            }
        }
    }
}

@Composable
private fun RoadmapCard(feature: RoadmapFeature, open: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(Modifier.fillMaxWidth().shadow(if (FFColors.isDark) 0.dp else 2.dp, shape, ambientColor = FFColors.hairline, spotColor = FFColors.hairline)
        .clip(shape).background(FFColors.surface).clickable(role = Role.Button, onClick = open)
        .padding(horizontal = 16.dp, vertical = 18.dp).testTag("roadmap-${feature.id}")) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(feature.displayTitle, Modifier.weight(1f), style = FFType.bodyMain.copy(fontSize = 18.sp, lineHeight = 24.sp),
                fontWeight = FontWeight.Medium, color = FFColors.onSurface)
            Text(feature.displayArea, Modifier.clip(RoundedCornerShape(50)).background(FFColors.raised).padding(horizontal = 9.dp, vertical = 3.dp),
                style = FFType.badgeLabel.copy(fontSize = 12.sp, lineHeight = 16.sp), fontWeight = FontWeight.Medium, color = FFColors.onSurfaceVariant, maxLines = 1)
        }
        Spacer(Modifier.height(10.dp))
        Text(feature.description, style = FFType.bodyMain.copy(fontSize = 15.sp, lineHeight = 20.sp), color = FFColors.onSurfaceVariant)
    }
}
