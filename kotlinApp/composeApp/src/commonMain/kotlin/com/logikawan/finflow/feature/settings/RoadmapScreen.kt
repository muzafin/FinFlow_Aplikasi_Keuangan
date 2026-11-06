package com.logikawan.finflow.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.logikawan.finflow.core.ui.*

@Composable
fun RoadmapScreen(sendFeedback: () -> Unit) {
    var status by rememberSaveable { mutableStateOf(RoadmapStatus.Planned) }
    var query by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf<RoadmapFeature?>(null) }
    var copied by remember { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboardManager.current
    val listState = rememberLazyListState()
    LaunchedEffect(status, query) { listState.scrollToItem(0) }
    val features = productRoadmap.filter { it.status == status &&
        (query.isBlank() || "${it.id} ${it.title} ${it.area} ${it.description} ${it.next}".contains(query.trim(), true)) }
    Column(Modifier.fillMaxSize().testTag("product-roadmap")) {
        TabRow(selectedTabIndex = status.ordinal, containerColor = FFColors.background, contentColor = FFColors.onSurface) {
            RoadmapStatus.entries.forEach { tab ->
                Tab(selected = tab == status, onClick = { status = tab; copied = null },
                    text = { Text(tab.label, style = FFType.metaData, maxLines = 2) },
                    selectedContentColor = FFColors.onSurface, unselectedContentColor = FFColors.onSurfaceVariant)
            }
        }
        LazyColumn(Modifier.weight(1f).testTag("roadmap-list"), state = listState, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text("Status FinFlow • $roadmapUpdated", style = FFType.metaData, color = FFColors.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Field("Cari fitur / ID", query, { query = it })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { clipboard.setText(AnnotatedString(roadmapPrompt())); copied = "Seluruh peta jalan disalin." }) { Text("Salin Semua") }
                    TextButton(onClick = sendFeedback) { Text("Kirim Masukan") }
                }
                copied?.let { Text("$it Tempelkan di chat untuk pekerjaan berikutnya.", style = FFType.metaData, color = FFColors.onSurface) }
                Text("${features.size} fitur • Ketuk kartu untuk rincian dan salin prompt", style = FFType.metaData, color = FFColors.onSurfaceVariant)
            }
            items(features, key = { it.id }) { feature ->
                DesignCard(modifier = Modifier.testTag("roadmap-${feature.id}"), onClick = { selected = feature }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(feature.title, Modifier.weight(1f), style = FFType.sectionTitle, fontWeight = FontWeight.SemiBold)
                        Surface(color = FFColors.outlineVariant, shape = MaterialTheme.shapes.small) {
                            Text(feature.area, Modifier.padding(horizontal = 8.dp, vertical = 5.dp), style = FFType.badgeLabel, color = FFColors.onSurface)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(feature.description, style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    Text(feature.id, style = FFType.metaData, color = FFColors.onSurfaceVariant)
                }
            }
            if (features.isEmpty()) item { Text("Tidak ada fitur yang cocok di tab ini.", style = FFType.bodyMain) }
        }
    }
    selected?.let { feature ->
        AlertDialog(onDismissRequest = { selected = null }, title = { Text(feature.title) }, text = {
            SelectionContainer {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${feature.id} • ${feature.status.label} • ${feature.area}", style = FFType.metaData)
                    Text(feature.description)
                    Text(if (feature.status == RoadmapStatus.Done) "Lingkup yang sudah tersedia" else "Target berikutnya", fontWeight = FontWeight.SemiBold)
                    Text(feature.next)
                }
            }
        }, confirmButton = { TextButton(onClick = {
            clipboard.setText(AnnotatedString(feature.prompt())); selected = null; copied = "Prompt ${feature.id} disalin."
        }) { Text("Salin Prompt") } }, dismissButton = { TextButton(onClick = { selected = null }) { Text("Tutup") } })
    }
}
