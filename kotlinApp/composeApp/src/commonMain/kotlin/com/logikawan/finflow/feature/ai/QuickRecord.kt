package com.logikawan.finflow.feature.ai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logikawan.finflow.core.model.FinanceData
import com.logikawan.finflow.core.model.Transaction
import com.logikawan.finflow.core.ui.FFColors
import com.logikawan.finflow.core.ui.FFIcon
import com.logikawan.finflow.core.ui.FFType
import com.logikawan.finflow.core.util.displayMoney

private val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

private fun badge(controller: AiController, feature: AiFeature): String {
    if (!controller.available) return "LOGIN"
    if (controller.isPremium) return "PRO"
    val usage = controller.status?.usage?.get(feature.id) ?: return if (controller.statusUnavailable) "–" else "…"
    return usage.limit?.let { (it - usage.used).coerceAtLeast(0).toString() } ?: "PRO"
}

/** Panel tombol + : Asisten, AI Voice, AI Scan, AI Chat, Tambah — susunan sama dengan Ollo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickRecordBar(controller: AiController, onAssistant: () -> Unit, onVoice: () -> Unit, onScan: () -> Unit, onChat: () -> Unit,
                   onAdd: () -> Unit, onDismiss: () -> Unit) {
    LaunchedEffect(Unit) { controller.refreshStatus() }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = FFColors.canvas, shape = sheetShape,
        dragHandle = null, contentWindowInsets = { WindowInsets(0, 0, 0, 0) }) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 18.dp).testTag("quick-record-bar"),
            horizontalArrangement = Arrangement.SpaceEvenly) {
            QuickItem("Asisten", "forum_outlined", badge(controller, AiFeature.Assistant), "quick-assistant") { onDismiss(); onAssistant() }
            QuickItem("AI Voice", "graphic_eq_rounded", badge(controller, AiFeature.Voice), "quick-voice") { onDismiss(); onVoice() }
            QuickItem("AI Scan", "document_scanner_outlined", badge(controller, AiFeature.Scan), "quick-scan") { onDismiss(); onScan() }
            QuickItem("AI Chat", "chat_bubble_outline_rounded", badge(controller, AiFeature.Chat), "quick-chat") { onDismiss(); onChat() }
            QuickItem("Tambah", "add_rounded", null, "quick-add") { onDismiss(); onAdd() }
        }
    }
}

@Composable
private fun RowScope.QuickItem(label: String, icon: String, badge: String?, tag: String, onClick: () -> Unit) {
    Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 8.dp).testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally) {
        FFIcon(icon, FFColors.onSurface, 26.dp, contentDescription = label)
        Spacer(Modifier.height(6.dp))
        if (badge != null) Box(Modifier.clip(RoundedCornerShape(8.dp)).background(FFColors.surfaceContainerHighest).padding(horizontal = 6.dp, vertical = 1.dp)) {
            Text(badge, style = FFType.badgeLabel.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold), color = FFColors.onSurface)
        } else Spacer(Modifier.height(16.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, style = FFType.metaData, color = FFColors.onSurface, maxLines = 1)
    }
}

private fun consentPoints(feature: AiFeature): List<String> = when (feature) {
    AiFeature.Voice -> listOf(
        "Suaramu diubah menjadi teks di perangkat; transkrip itu dikirim ke server FinFlow dan penyedia model AI untuk mengambil data transaksi.",
        "FinFlow tidak mengirim rekaman suara mentah ke server — hanya teks transkrip.")
    AiFeature.Scan -> listOf(
        "Foto struk dikirim aman ke server FinFlow dan penyedia model AI untuk dibaca, lalu tidak disimpan.",
        "Setiap foto memakai satu kredit AI Scan.")
    AiFeature.Chat -> listOf("Teks yang kamu ketik dikirim ke server FinFlow dan penyedia model AI untuk mengambil data transaksi.")
    AiFeature.Assistant -> listOf(
        "Asisten melihat ringkasan angkamu (total bulanan, kategori teratas, anggaran, saldo, tagihan) — bukan transaksi satu per satu.",
        "Jawaban AI bisa keliru dan bukan nasihat keuangan profesional.")
} + listOf(
    "Saat ini AI memakai penyedia model AI (Anthropic Claude) melalui server FinFlow. Pemrosesan oleh penyedia mengikuti kebijakannya.",
    "Hasil AI bisa salah dan wajib ditinjau sebelum disimpan.",
    "Kamu bisa mencabut persetujuan di Manajemen Akun; FinFlow akan bertanya lagi sebelum pemrosesan AI berikutnya.",
)

@Composable
fun AiConsentDialog(controller: AiController) {
    val (feature, onReady) = controller.pendingConsent ?: return
    AlertDialog(onDismissRequest = { controller.pendingConsent = null }, modifier = Modifier.testTag("ai-consent"),
        title = { Text("Persetujuan ${feature.title}", style = FFType.sectionTitle) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            consentPoints(feature).forEach { point ->
                Row { Text("•  ", style = FFType.bodyMain); Text(point, style = FFType.bodyMain, color = FFColors.onSurfaceVariant) }
            }
        } },
        confirmButton = { TextButton(onClick = { controller.pendingConsent = null; controller.setConsent(feature, true, onReady) },
            modifier = Modifier.testTag("ai-consent-agree")) { Text("Setuju & Lanjutkan", fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = { controller.pendingConsent = null }) { Text("Batal") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatSheet(controller: AiController, onSend: (String) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = FFColors.canvas, shape = sheetShape, dragHandle = null) {
        Column(Modifier.fillMaxWidth().imePadding().navigationBarsPadding().padding(20.dp).testTag("ai-chat-sheet")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Quick Record", style = FFType.appTitle, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { FFIcon("close_rounded", FFColors.onSurfaceVariant, contentDescription = "Tutup") }
            }
            Spacer(Modifier.height(16.dp))
            Text("Untuk beberapa transaksi, pisahkan dengan koma atau baris baru.", style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(FFColors.surface).padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                TextField(text, { if (it.length <= 2000) text = it }, Modifier.weight(1f).testTag("ai-chat-input"),
                    placeholder = { Text("cth. \"Makan siang 50rb\", \"Gaji 10jt\"", color = FFColors.outline) },
                    colors = TextFieldDefaults.colors(focusedContainerColor = FFColors.surface, unfocusedContainerColor = FFColors.surface,
                        focusedIndicatorColor = FFColors.surface, unfocusedIndicatorColor = FFColors.surface),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Default), maxLines = 5)
                val ready = text.isNotBlank() && controller.working == null
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(if (ready) FFColors.onSurface else FFColors.surfaceContainerHighest)
                    .clickable(enabled = ready) { onSend(text.trim()) }.testTag("ai-chat-send"), contentAlignment = Alignment.Center) {
                    if (controller.working != null) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = FFColors.canvas)
                    else FFIcon("send_rounded", if (ready) FFColors.canvas else FFColors.outline, 20.dp, contentDescription = "Kirim")
                }
            }
            controller.working?.let { Spacer(Modifier.height(10.dp)); Text(it, style = FFType.metaData, color = FFColors.onSurfaceVariant) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiScanSheet(controller: AiController, onCamera: () -> Unit, onGallery: () -> Unit, onSettings: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = FFColors.canvas, shape = sheetShape, dragHandle = null) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp).testTag("ai-scan-sheet"), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Quick Record", style = FFType.appTitle, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { FFIcon("close_rounded", FFColors.onSurfaceVariant, contentDescription = "Tutup") }
            }
            Spacer(Modifier.height(24.dp))
            Text("Scan Struk AI", style = FFType.sectionTitle)
            Spacer(Modifier.height(8.dp))
            Text("Foto satu atau beberapa struk sekaligus (maksimal 5 per foto). Beri jarak antar struk, pastikan teks jelas, lalu tinjau setiap draf.",
                style = FFType.bodyMain, color = FFColors.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                ScanSource("Kamera", "photo_camera_outlined", "ai-scan-camera", controller.working == null, onCamera)
                ScanSource("Galeri", "photo_library_outlined", "ai-scan-gallery", controller.working == null, onGallery)
            }
            Spacer(Modifier.height(12.dp))
            Text("Pilih hingga 5 foto; tiap foto memakai satu kredit AI Scan.", style = FFType.metaData, color = FFColors.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onSettings, shape = RoundedCornerShape(24.dp)) { Text("Pengaturan AI Scan", color = FFColors.onSurface) }
            controller.working?.let { Spacer(Modifier.height(12.dp)); Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)); Text(it, style = FFType.metaData)
            } }
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.Top) {
                FFIcon("lock_outline_rounded", FFColors.outline, 16.dp); Spacer(Modifier.width(8.dp))
                Text("Foto dikirim aman ke server FinFlow untuk diproses AI dan tidak disimpan. Hasil tetap bisa diedit sebelum disimpan.",
                    style = FFType.metaData, color = FFColors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ScanSource(label: String, icon: String, tag: String, enabled: Boolean, onClick: () -> Unit) {
    Column(Modifier.size(80.dp, 105.dp).clip(RoundedCornerShape(20.dp)).background(FFColors.surface).clickable(enabled = enabled, onClick = onClick)
        .testTag(tag), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        FFIcon(icon, FFColors.onSurface, 30.dp); Spacer(Modifier.height(8.dp)); Text(label, style = FFType.bodyMain)
    }
}

/** Semua hasil AI ditinjau dulu; ketuk satu draf untuk mengubahnya di editor transaksi. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiDraftReviewSheet(controller: AiController, data: FinanceData, busy: Boolean, onEdit: (Transaction) -> Unit, onSaveAll: (List<Transaction>) -> Unit) {
    val drafts = controller.drafts
    if (drafts.isEmpty()) return
    ModalBottomSheet(onDismissRequest = { if (!busy) controller.drafts = emptyList() }, containerColor = FFColors.canvas, shape = sheetShape) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 20.dp).testTag("ai-draft-review")) {
            Text("Tinjau Hasil AI", style = FFType.appTitle)
            Text("${drafts.size} draf transaksi. Ketuk untuk mengubah sebelum disimpan.", style = FFType.metaData, color = FFColors.onSurfaceVariant)
            controller.draftMessage?.let { Spacer(Modifier.height(6.dp)); Text(it, style = FFType.metaData, color = FFColors.onSurfaceVariant) }
            Spacer(Modifier.height(12.dp))
            LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(drafts, key = { it.id }) { tx ->
                    val category = data.categories.find { it.id == tx.categoryId }?.name ?: if (tx.type == "transfer") "Transfer" else "Tanpa kategori"
                    val wallet = data.accounts.find { it.id == tx.accountId }?.name.orEmpty()
                    Surface(onClick = { onEdit(tx) }, shape = RoundedCornerShape(18.dp), color = FFColors.surface,
                        border = BorderStroke(1.dp, FFColors.outlineVariant), modifier = Modifier.fillMaxWidth().testTag("ai-draft-${tx.id}")) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(tx.title, style = FFType.bodyMain, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("$category · $wallet · ${tx.date}", style = FFType.metaData, color = FFColors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (tx.description.isNotBlank()) Text(tx.description, style = FFType.metaData, color = FFColors.outline, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text((if (tx.type == "income") "+" else if (tx.type == "expense") "-" else "") + displayMoney(tx.amountMinor, tx.currency),
                                style = FFType.amountSm, color = when (tx.type) { "income" -> FFColors.income; "expense" -> FFColors.expense; else -> FFColors.onSurface })
                            IconButton(onClick = { controller.drafts = drafts - tx }) { FFIcon("close_rounded", FFColors.outline, 18.dp, contentDescription = "Buang draf") }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { onSaveAll(drafts) }, enabled = !busy && drafts.all { it.categoryId.isNotBlank() }, shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FFColors.onSurface, contentColor = FFColors.canvas),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("ai-draft-save-all")) {
                Text(if (drafts.size == 1) "Simpan Transaksi" else "Simpan Semua (${drafts.size})", fontWeight = FontWeight.SemiBold)
            }
            if (drafts.any { it.categoryId.isBlank() }) Text("Lengkapi kategori pada draf yang ditandai sebelum menyimpan.", style = FFType.metaData, color = FFColors.expense)
            TextButton(onClick = { controller.drafts = emptyList() }, enabled = !busy, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Batal") }
        }
    }
}

@Composable
fun AiErrorDialog(controller: AiController, onUpgrade: () -> Unit) {
    val message = controller.error ?: return
    val quota = message.contains("Premium")
    AlertDialog(onDismissRequest = { controller.error = null }, title = { Text(if (quota) "Kuota AI" else "AI belum berhasil") }, text = { Text(message) },
        confirmButton = { TextButton(onClick = { controller.error = null; if (quota) onUpgrade() }) { Text(if (quota) "Lihat Premium" else "Tutup") } },
        dismissButton = if (quota) ({ TextButton(onClick = { controller.error = null }) { Text("Nanti") } }) else null)
}
