package com.logikawan.finflow.feature.ai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logikawan.finflow.core.model.FinanceData
import com.logikawan.finflow.core.ui.FFColors
import com.logikawan.finflow.core.ui.FFIcon
import com.logikawan.finflow.core.ui.FFIconButton
import com.logikawan.finflow.core.ui.FFType
import com.logikawan.finflow.core.util.nowMillis

private val suggestions = listOf(
    "Berapa pengeluaranku bulan ini?", "Prediksi pengeluaranku sampai akhir bulan", "Dari mana pemasukanku bulan ini?",
    "Ada tagihan yang belum kubayar?", "Kategori apa yang paling boros?", "Beri tips hemat berdasarkan dataku",
)
val personalities = listOf(
    Triple("friendly", "Ramah", "Hangat, santai, menyemangati"), Triple("concise", "Ringkas", "Langsung ke intinya"),
    Triple("detailed", "Detail", "Menjelaskan alasan di balik angka"), Triple("motivator", "Motivator", "Seperti pelatih keuangan yang suportif"),
    Triple("strict", "Tegas", "Tegas, blak-blakan, tanpa basa-basi"), Triple("relaxed", "Santai", "Tenang, ringan, tetap praktis"),
)

@Composable
private fun AiTopBar(title: String, back: () -> Unit, beta: Boolean = false, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        FFIconButton("arrow_back_rounded", back)
        Text(title, style = FFType.appTitle, maxLines = 1)
        if (beta) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0x33FF9800)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text("BETA", style = FFType.badgeLabel.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = Color(0xFFFF9800))
            }
        }
        Spacer(Modifier.weight(1f))
        actions()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(controller: AiController, data: FinanceData, language: String, back: () -> Unit, openSettings: () -> Unit, openPremium: () -> Unit) {
    var conversation by remember { mutableStateOf(AssistantConversation()) }
    var input by rememberSaveable { mutableStateOf("") }
    var history by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    LaunchedEffect(conversation.turns.size) { if (conversation.turns.isNotEmpty()) listState.animateScrollToItem(conversation.turns.size - 1) }
    fun send(text: String) {
        val question = text.trim().take(2000)
        if (question.isBlank() || controller.working != null) return
        controller.requireConsent(AiFeature.Assistant) {
            val asked = conversation.copy(title = conversation.title.ifBlank { question.take(60) }, updatedAt = nowMillis(),
                turns = conversation.turns + AssistantTurn("user", question))
            conversation = asked
            input = ""
            controller.run("Asisten sedang berpikir…") {
                val reply = controller.service.ask(asked.turns, assistantSummary(data), controller.store.assistantSettings(), language)
                conversation = asked.copy(turns = asked.turns + AssistantTurn("assistant", reply), updatedAt = nowMillis())
                controller.store.saveConversation(conversation)
            }
        }
    }
    // Jika permintaan gagal, pertanyaan terakhir dikembalikan ke kolom input agar bisa dikirim ulang.
    LaunchedEffect(controller.error) {
        val last = conversation.turns.lastOrNull()
        if (controller.error != null && last?.role == "user") { input = last.text; conversation = conversation.copy(turns = conversation.turns.dropLast(1)) }
    }
    Column(Modifier.fillMaxSize().background(FFColors.background).navigationBarsPadding().imePadding().testTag("assistant-screen")) {
        AiTopBar("FinFlow Assistant", back, beta = true) {
            FFIconButton("history_rounded", { history = true })
            FFIconButton("tune_rounded", openSettings)
        }
        if (conversation.turns.isEmpty()) Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(48.dp))
            Box(Modifier.size(72.dp).clip(CircleShape).background(FFColors.surface), contentAlignment = Alignment.Center) {
                FFIcon("chat_bubble_outline_rounded", FFColors.onSurface, 32.dp)
            }
            Spacer(Modifier.height(20.dp))
            Text("Diskusikan keuanganmu", style = FFType.appTitle)
            Spacer(Modifier.height(8.dp))
            Text("Asisten melihat ringkasan angkamu (total bulanan, kategori teratas, anggaran, saldo) — bukan transaksi satu per satu. Jawaban AI bisa keliru; ini bukan nasihat keuangan profesional.",
                style = FFType.bodyMain, color = FFColors.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            controller.status?.usage?.get("assistant")?.let { usage ->
                if (!controller.isPremium && usage.limit != null) {
                    Text("${(usage.limit - usage.used).coerceAtLeast(0)} percobaan tersisa", style = FFType.metaData, color = FFColors.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                }
            }
            suggestions.forEach { suggestion ->
                Surface(onClick = { send(suggestion) }, shape = RoundedCornerShape(24.dp), color = Color.Transparent,
                    border = BorderStroke(1.dp, FFColors.outline), modifier = Modifier.padding(vertical = 6.dp).testTag("assistant-suggestion")) {
                    Text(suggestion, Modifier.padding(horizontal = 18.dp, vertical = 12.dp), style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(24.dp))
        } else LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(conversation.turns) { turn -> Bubble(turn) }
            if (controller.working != null) item { Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp))
                Text(controller.working.orEmpty(), style = FFType.metaData, color = FFColors.onSurfaceVariant)
            } }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(input, { if (it.length <= 2000) input = it }, Modifier.weight(1f).testTag("assistant-input"),
                placeholder = { Text("Tanya tentang keuanganmu…", color = FFColors.outline) }, maxLines = 4, shape = RoundedCornerShape(28.dp),
                colors = TextFieldDefaults.colors(focusedContainerColor = FFColors.surface, unfocusedContainerColor = FFColors.surface,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
            Spacer(Modifier.width(10.dp))
            val ready = input.isNotBlank() && controller.working == null
            Box(Modifier.size(52.dp).clip(CircleShape).background(if (ready) FFColors.onSurface else FFColors.surfaceContainerHighest)
                .clickable(enabled = ready) { send(input) }.testTag("assistant-send"), contentAlignment = Alignment.Center) {
                FFIcon("arrow_upward_outlined", if (ready) FFColors.canvas else FFColors.outline, 24.dp, contentDescription = "Kirim")
            }
        }
    }
    if (history) ModalBottomSheet(onDismissRequest = { history = false }, containerColor = FFColors.canvas) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text("Riwayat percakapan", style = FFType.sectionTitle)
            Spacer(Modifier.height(12.dp))
            val saved = remember { controller.store.history() }
            if (saved.isEmpty()) Text("Belum ada percakapan tersimpan. Hingga 5 percakapan terakhir disimpan di perangkat ini.",
                style = FFType.bodyMain, color = FFColors.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
            saved.forEach { item ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { conversation = item; history = false }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    FFIcon("chat_bubble_outline_rounded", FFColors.onSurfaceVariant, 20.dp); Spacer(Modifier.width(12.dp))
                    Text(item.title.ifBlank { "Percakapan" }, Modifier.weight(1f), style = FFType.bodyMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (conversation.turns.isNotEmpty()) TextButton(onClick = { conversation = AssistantConversation(); history = false }) { Text("Percakapan baru") }
        }
    }
}

@Composable
private fun Bubble(turn: AssistantTurn) {
    val mine = turn.role == "user"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Surface(shape = RoundedCornerShape(20.dp, 20.dp, if (mine) 6.dp else 20.dp, if (mine) 20.dp else 6.dp),
            color = if (mine) FFColors.onSurface else FFColors.surface, modifier = Modifier.widthIn(max = 320.dp)) {
            Text(turn.text, Modifier.padding(horizontal = 16.dp, vertical = 12.dp), style = FFType.bodyMain, color = if (mine) FFColors.canvas else FFColors.onSurface)
        }
    }
}

@Composable
fun AssistantSettingsScreen(controller: AiController, back: () -> Unit) {
    var settings by remember { mutableStateOf(controller.store.assistantSettings()) }
    fun save(value: AssistantSettings) { settings = value; controller.store.saveAssistantSettings(value) }
    Column(Modifier.fillMaxSize().background(FFColors.background).navigationBarsPadding().imePadding().testTag("assistant-settings")) {
        Box(Modifier.fillMaxWidth()) {
            AiTopBar("", back)
            Text("Pengaturan Asisten", Modifier.align(Alignment.Center).statusBarsPadding(), style = FFType.appTitle)
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Kepribadian", style = FFType.sectionTitle)
            Text("Mengatur gaya bicara asisten. Tidak mengubah data yang dapat dilihatnya.", style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
            personalities.forEach { (id, title, caption) ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(FFColors.surface).clickable { save(settings.copy(personality = id)) }
                    .padding(horizontal = 12.dp, vertical = 14.dp).testTag("personality-$id"), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(settings.personality == id, { save(settings.copy(personality = id)) })
                    Spacer(Modifier.width(8.dp))
                    Column { Text(title, style = FFType.bodyMain.copy(fontSize = 16.sp), fontWeight = FontWeight.Medium); Text(caption, style = FFType.metaData, color = FFColors.onSurfaceVariant) }
                }
            }
            Spacer(Modifier.height(8.dp))
            LimitedField("Asisten memanggilmu apa? (opsional)", "cth. Arifin, Bos", settings.nickname, 40) { save(settings.copy(nickname = it)) }
            LimitedField("Preferensi tambahan (opsional)", "cth. Fokus bantu aku menabung untuk rumah", settings.preference, 200, minLines = 3) { save(settings.copy(preference = it)) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun LimitedField(label: String, placeholder: String, value: String, max: Int, minLines: Int = 1, change: (String) -> Unit) {
    Column {
        Text(label, style = FFType.bodyMain, fontWeight = FontWeight.Medium, color = FFColors.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        TextField(value, { if (it.length <= max) change(it) }, Modifier.fillMaxWidth(), minLines = minLines,
            placeholder = { Text(placeholder, color = FFColors.outline) }, shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(focusedContainerColor = FFColors.surface, unfocusedContainerColor = FFColors.surface,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
        Text("${value.length}/$max", Modifier.fillMaxWidth().padding(top = 4.dp), style = FFType.metaData, color = FFColors.onSurfaceVariant, textAlign = TextAlign.End)
    }
}

private val scanFieldInfo = mapOf(
    "items" to Triple("Item & harga", "Indomie Goreng x5: Rp17.500", "format_list_bulleted_rounded"),
    "location" to Triple("Lokasi (nama toko)", "Lokasi: Indomaret Solo", "storefront_outlined"),
    "discount" to Triple("Diskon", "Diskon: Rp2.000", "percent"),
    "tax" to Triple("Pajak", "Pajak: Rp4.318", "account_balance_outlined"),
    "fees" to Triple("Biaya layanan/admin", "Biaya: Rp2.500", "receipt_long_outlined"),
    "payment" to Triple("Metode pembayaran", "Pembayaran: QRIS", "payments_outlined"),
    "address" to Triple("Alamat toko", "Alamat: Jl. Slamet Riyadi No. 10", "location_on_outlined"),
    "phone" to Triple("Nomor telepon", "Telepon: 0800-1-113456", "phone_outlined"),
)

@Composable
fun ScanSettingsScreen(controller: AiController, back: () -> Unit) {
    var settings by remember { mutableStateOf(controller.store.scanSettings()) }
    fun save(value: ScanSettings) { settings = value; controller.store.saveScanSettings(value) }
    fun move(index: Int, delta: Int) {
        val target = index + delta
        if (target !in settings.fields.indices) return
        save(settings.copy(fields = settings.fields.toMutableList().apply { add(target, removeAt(index)) }))
    }
    Column(Modifier.fillMaxSize().background(FFColors.background).navigationBarsPadding().imePadding().testTag("scan-settings")) {
        Box(Modifier.fillMaxWidth()) {
            AiTopBar("", back)
            Text("Pengaturan AI Scan", Modifier.align(Alignment.Center).statusBarsPadding(), style = FFType.appTitle)
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LimitedField("Nama pemilik rekening (opsional)", "Dipakai untuk mengenali kamu pada bukti transfer", settings.ownerName, 80) { save(settings.copy(ownerName = it)) }
            Text("Pilih detail struk yang ditulis ke catatan transaksi. Gunakan panah untuk mengubah urutan.", style = FFType.metaData, color = FFColors.onSurfaceVariant)
            settings.fields.forEachIndexed { index, field ->
                val (title, sample, icon) = scanFieldInfo.getValue(field.id)
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(FFColors.surface).padding(12.dp).testTag("scan-field-${field.id}"),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(FFColors.surfaceContainerHighest), contentAlignment = Alignment.Center) { FFIcon(icon, FFColors.onSurfaceVariant, 20.dp) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text(title, style = FFType.bodyMain.copy(fontSize = 16.sp)); Text(sample, style = FFType.metaData, color = FFColors.onSurfaceVariant) }
                    Switch(field.enabled, { on -> save(settings.copy(fields = settings.fields.map { if (it.id == field.id) it.copy(enabled = on) else it })) })
                    Column {
                        Box(Modifier.size(28.dp, 22.dp).clickable(enabled = index > 0) { move(index, -1) }, contentAlignment = Alignment.Center) {
                            FFIcon("keyboard_arrow_up_rounded", if (index > 0) FFColors.onSurfaceVariant else FFColors.outlineVariant, 22.dp, contentDescription = "Naikkan")
                        }
                        Box(Modifier.size(28.dp, 22.dp).clickable(enabled = index < settings.fields.lastIndex) { move(index, 1) }, contentAlignment = Alignment.Center) {
                            FFIcon("keyboard_arrow_down_rounded", if (index < settings.fields.lastIndex) FFColors.onSurfaceVariant else FFColors.outlineVariant, 22.dp, contentDescription = "Turunkan")
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
