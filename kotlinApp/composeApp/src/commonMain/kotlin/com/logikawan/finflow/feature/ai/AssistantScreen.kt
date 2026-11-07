package com.logikawan.finflow.feature.ai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logikawan.finflow.core.i18n.text
import com.logikawan.finflow.core.model.FinanceData
import com.logikawan.finflow.core.ui.FFColors
import com.logikawan.finflow.core.ui.FFIcon
import com.logikawan.finflow.core.ui.FFIconButton
import com.logikawan.finflow.core.ui.FFType
import com.logikawan.finflow.core.util.nowMillis
import com.logikawan.finflow.i18n.*
import dev.icerock.moko.resources.StringResource

private val suggestions = listOf(
    MR.strings.ai_suggest_spending_month, MR.strings.ai_suggest_predict_spending, MR.strings.ai_suggest_income_sources,
    MR.strings.ai_suggest_unpaid_bills, MR.strings.ai_suggest_wasteful_category, MR.strings.ai_suggest_saving_tips,
)
/** Kepribadian Asisten: id (dikirim ke server) + judul & keterangan sebagai resource (diterjemahkan saat ditampilkan). */
val personalities: List<Triple<String, StringResource, StringResource>> = listOf(
    Triple("friendly", MR.strings.ai_personality_friendly, MR.strings.ai_personality_friendly_desc),
    Triple("concise", MR.strings.ai_personality_concise, MR.strings.ai_personality_concise_desc),
    Triple("detailed", MR.strings.ai_personality_detailed, MR.strings.ai_personality_detailed_desc),
    Triple("motivator", MR.strings.ai_personality_motivator, MR.strings.ai_personality_motivator_desc),
    Triple("strict", MR.strings.ai_personality_strict, MR.strings.ai_personality_strict_desc),
    Triple("relaxed", MR.strings.ai_personality_relaxed, MR.strings.ai_personality_relaxed_desc),
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
            controller.run(MR.strings.ai_working_assistant.text()) {
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
        if (controller.serviceNotDeployed) AiUnavailableBanner({ controller.refreshStatus() })
        if (conversation.turns.isEmpty()) Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(48.dp))
            Box(Modifier.size(72.dp).clip(CircleShape).background(FFColors.surface), contentAlignment = Alignment.Center) {
                FFIcon("chat_bubble_outline_rounded", FFColors.onSurface, 32.dp)
            }
            Spacer(Modifier.height(20.dp))
            Text(MR.strings.ai_assistant_empty_title.text(), style = FFType.appTitle)
            Spacer(Modifier.height(8.dp))
            Text(MR.strings.ai_assistant_empty_body.text(),
                style = FFType.bodyMain, color = FFColors.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            controller.status?.usage?.get("assistant")?.let { usage ->
                if (!controller.isPremium && usage.limit != null) {
                    Text(MR.strings.ai_attempts_left.text((usage.limit - usage.used).coerceAtLeast(0)), style = FFType.metaData, color = FFColors.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                }
            }
            suggestions.forEach { resource ->
                val suggestion = resource.text()
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
                placeholder = { Text(MR.strings.ai_assistant_input_hint.text(), color = FFColors.outline) }, maxLines = 4, shape = RoundedCornerShape(28.dp),
                colors = TextFieldDefaults.colors(focusedContainerColor = FFColors.surface, unfocusedContainerColor = FFColors.surface,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
            Spacer(Modifier.width(10.dp))
            val ready = input.isNotBlank() && controller.working == null
            Box(Modifier.size(52.dp).clip(CircleShape).background(if (ready) FFColors.onSurface else FFColors.surfaceContainerHighest)
                .clickable(enabled = ready) { send(input) }.testTag("assistant-send"), contentAlignment = Alignment.Center) {
                FFIcon("arrow_upward_outlined", if (ready) FFColors.canvas else FFColors.outline, 24.dp, contentDescription = MR.strings.ai_send.text())
            }
        }
    }
    if (history) ModalBottomSheet(onDismissRequest = { history = false }, containerColor = FFColors.canvas) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(MR.strings.ai_history_title.text(), style = FFType.sectionTitle)
            Spacer(Modifier.height(12.dp))
            val saved = remember { controller.store.history() }
            if (saved.isEmpty()) Text(MR.strings.ai_history_empty.text(),
                style = FFType.bodyMain, color = FFColors.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
            saved.forEach { item ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { conversation = item; history = false }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    FFIcon("chat_bubble_outline_rounded", FFColors.onSurfaceVariant, 20.dp); Spacer(Modifier.width(12.dp))
                    Text(item.title.ifBlank { MR.strings.ai_conversation_default.text() }, Modifier.weight(1f), style = FFType.bodyMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (conversation.turns.isNotEmpty()) TextButton(onClick = { conversation = AssistantConversation(); history = false }) { Text(MR.strings.ai_conversation_new.text()) }
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

/** Header sub-layar AI seperti header App: chevron kiri, judul di tengah, aksi di kanan. */
@Composable
private fun SettingsTopBar(title: String, back: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    Box(Modifier.fillMaxWidth().statusBarsPadding().height(64.dp)) {
        FFIconButton("hi_chevron_left", back, Modifier.align(Alignment.CenterStart).padding(start = 4.dp), tint = FFColors.onSurface)
        Text(title, Modifier.align(Alignment.Center).padding(horizontal = 56.dp), style = FFType.appTitle, color = FFColors.onSurface,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(Modifier.align(Alignment.CenterEnd).padding(end = 4.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** Keadaan "layanan AI belum tersedia" (Cloud Functions belum di-deploy/dikonfigurasi). */
@Composable
fun AiUnavailableBanner(retry: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(18.dp)).background(FFColors.surface)
        .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 4.dp).testTag("ai-unavailable"), verticalAlignment = Alignment.Top) {
        FFIcon("hi_info", FFColors.onSurfaceVariant, 20.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(MR.strings.pd_ai_banner_title.text(), style = FFType.bodyMain, fontWeight = FontWeight.Medium, color = FFColors.onSurface)
            Spacer(Modifier.height(2.dp))
            Text(MR.strings.pd_ai_banner_body.text(), style = FFType.metaData, color = FFColors.onSurfaceVariant)
            TextButton(onClick = retry, contentPadding = PaddingValues(0.dp), modifier = Modifier.testTag("ai-unavailable-retry")) {
                Text(MR.strings.pd_ai_banner_retry.text(), style = FFType.bodyMain, fontWeight = FontWeight.Medium, color = FFColors.onSurface)
            }
        }
    }
}

@Composable
fun AssistantSettingsScreen(controller: AiController, back: () -> Unit) {
    var settings by remember { mutableStateOf(controller.store.assistantSettings()) }
    fun save(value: AssistantSettings) { settings = value; controller.store.saveAssistantSettings(value) }
    Column(Modifier.fillMaxSize().background(FFColors.background).navigationBarsPadding().imePadding().testTag("assistant-settings")) {
        SettingsTopBar(MR.strings.ai_assistant_settings_title.text(), back)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(8.dp))
            // Ukuran diukur dari Ollo 1.7.9: judul 20sp, keterangan 14sp, kartu 18sp/14sp dengan jarak 8dp.
            Text(MR.strings.ai_personality_title.text(), style = FFType.bodyMain.copy(fontSize = 20.sp, lineHeight = 26.sp),
                fontWeight = FontWeight.SemiBold, color = FFColors.onSurface)
            Spacer(Modifier.height(6.dp))
            Text(MR.strings.ai_personality_caption.text(), style = FFType.bodyMain.copy(lineHeight = 18.sp), color = FFColors.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            personalities.forEach { (id, title, caption) ->
                val selected = settings.personality == id
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(18.dp)).background(FFColors.surface)
                    .selectable(selected, role = Role.RadioButton) { save(settings.copy(personality = id)) }
                    .padding(start = 4.dp, end = 16.dp, top = 13.dp, bottom = 13.dp).testTag("personality-$id"),
                    verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected, onClick = null, modifier = Modifier.size(48.dp),
                        colors = RadioButtonDefaults.colors(selectedColor = FFColors.onSurface, unselectedColor = FFColors.onSurfaceVariant))
                    Column(Modifier.weight(1f)) {
                        Text(title.text(), style = FFType.bodyMain.copy(fontSize = 18.sp, lineHeight = 24.sp), fontWeight = FontWeight.Medium, color = FFColors.onSurface)
                        Text(caption.text(), style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            LimitedField(MR.strings.ai_nickname_label.text(), MR.strings.ai_nickname_hint.text(), settings.nickname, 40,
                modifier = Modifier.testTag("assistant-nickname")) { save(settings.copy(nickname = it)) }
            Spacer(Modifier.height(16.dp))
            LimitedField(MR.strings.ai_preference_label.text(), MR.strings.ai_preference_hint.text(), settings.preference, 200, minLines = 2,
                modifier = Modifier.testTag("assistant-preference")) { save(settings.copy(preference = it)) }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.Top) {
                FFIcon("hi_shield", FFColors.onSurfaceVariant, 20.dp)
                Spacer(Modifier.width(12.dp))
                Text(MR.strings.pd_assistant_scope_note.text(), Modifier.weight(1f), style = FFType.bodyMain.copy(lineHeight = 18.sp), color = FFColors.onSurfaceVariant)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun LimitedField(label: String, placeholder: String, value: String, max: Int, minLines: Int = 1, modifier: Modifier = Modifier, change: (String) -> Unit) {
    Column {
        Text(label, style = FFType.bodyMain.copy(fontSize = 15.sp, lineHeight = 20.sp), color = FFColors.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        TextField(value, { if (it.length <= max) change(it) }, modifier.fillMaxWidth().heightIn(min = 48.dp), minLines = minLines,
            textStyle = FFType.bodyMain.copy(fontSize = 16.sp, lineHeight = 22.sp, color = FFColors.onSurface),
            placeholder = { Text(placeholder, style = FFType.bodyMain.copy(fontSize = 16.sp, lineHeight = 22.sp), color = FFColors.outline) },
            shape = RoundedCornerShape(20.dp), colors = fieldColors())
        Text("${value.length}/$max", Modifier.fillMaxWidth().padding(top = 8.dp, end = 20.dp), style = FFType.bodyMain, color = FFColors.onSurfaceVariant, textAlign = TextAlign.End)
    }
}

@Composable
private fun fieldColors() = TextFieldDefaults.colors(focusedContainerColor = FFColors.surface, unfocusedContainerColor = FFColors.surface,
    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, cursorColor = FFColors.onSurface)

@Composable
private fun scanSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = FFColors.onPill, checkedTrackColor = FFColors.pill, checkedBorderColor = Color.Transparent,
    uncheckedThumbColor = FFColors.onSurfaceVariant, uncheckedTrackColor = FFColors.raised, uncheckedBorderColor = Color.Transparent,
)

/** Judul, contoh catatan, dan ikon tiap detail struk (teks diterjemahkan saat dipanggil). */
private fun scanFieldInfo(id: String): Triple<String, String, String> = when (id) {
    "items" -> Triple(MR.strings.ai_scan_field_items.text(), "Indomie Goreng x5: 17.500", "hi_bill")
    "location" -> Triple(MR.strings.ai_scan_field_location.text(), MR.strings.ai_receipt_location.text("Indomaret Solo"), "storefront_outlined")
    "discount" -> Triple(MR.strings.ai_scan_field_discount.text(), MR.strings.ai_receipt_discount.text("2.000"), "percent")
    "tax" -> Triple(MR.strings.ai_scan_field_tax.text(), MR.strings.ai_receipt_tax.text("4.318"), "hi_bank")
    "fees" -> Triple(MR.strings.ai_scan_field_fees.text(), MR.strings.ai_receipt_fees.text("2.500"), "hi_bill")
    "payment" -> Triple(MR.strings.ai_scan_field_payment.text(), MR.strings.ai_receipt_payment.text("QRIS"), "hi_debt")
    "address" -> Triple(MR.strings.ai_scan_field_address.text(), MR.strings.ai_receipt_address.text("Jl. Slamet Riyadi No. 10"), "location_on_outlined")
    "phone" -> Triple(MR.strings.ai_scan_field_phone.text(), MR.strings.ai_receipt_phone.text("0800-1-113456"), "phone_outlined")
    else -> throw NoSuchElementException("Unknown scan field $id")
}

@Composable
fun ScanSettingsScreen(controller: AiController, back: () -> Unit) {
    var settings by remember { mutableStateOf(controller.store.scanSettings()) }
    var menu by remember { mutableStateOf(false) }
    var ownerInfo by rememberSaveable { mutableStateOf(false) }
    // Urutan diubah dengan tekan lama + seret kartu (seperti Ollo); disimpan saat jari dilepas.
    var dragging by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    val heights = remember { mutableStateMapOf<String, Int>() }
    val gapPx = with(LocalDensity.current) { 8.dp.toPx() }
    fun save(value: ScanSettings) { settings = value; controller.store.saveScanSettings(value) }
    fun move(index: Int, delta: Int) {
        val target = index + delta
        if (target !in settings.fields.indices) return
        save(settings.copy(fields = settings.fields.toMutableList().apply { add(target, removeAt(index)) }))
    }
    fun dragBy(id: String, dy: Float) {
        dragOffset += dy
        val list = settings.fields
        val index = list.indexOfFirst { it.id == id }
        if (index < 0) return
        if (dragOffset > 0 && index < list.lastIndex) {
            val step = (heights[list[index + 1].id] ?: return) + gapPx
            if (dragOffset > step / 2) { settings = settings.copy(fields = list.toMutableList().apply { add(index + 1, removeAt(index)) }); dragOffset -= step }
        } else if (dragOffset < 0 && index > 0) {
            val step = (heights[list[index - 1].id] ?: return) + gapPx
            if (-dragOffset > step / 2) { settings = settings.copy(fields = list.toMutableList().apply { add(index - 1, removeAt(index)) }); dragOffset += step }
        }
    }
    fun endDrag() { if (dragging != null) { dragging = null; dragOffset = 0f; save(settings) } }
    Column(Modifier.fillMaxSize().background(FFColors.background).navigationBarsPadding().imePadding().testTag("scan-settings")) {
        SettingsTopBar(MR.strings.ai_scan_settings_title.text(), back) {
            Box {
                IconButton({ menu = true }, Modifier.testTag("scan-settings-menu")) {
                    FFIcon("hi_more", FFColors.onSurface, 24.dp, contentDescription = MR.strings.pd_scan_menu.text())
                }
                DropdownMenu(menu, { menu = false }, containerColor = FFColors.surface) {
                    DropdownMenuItem(text = { Text(MR.strings.pd_scan_reset.text(), style = FFType.bodyMain, color = FFColors.onSurface) },
                        onClick = { menu = false; save(settings.copy(fields = defaultScanFields)) }, modifier = Modifier.testTag("scan-settings-reset"))
                }
            }
        }
        Column(Modifier.verticalScroll(rememberScrollState(), enabled = dragging == null).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(4.dp))
            TextField(settings.ownerName, { if (it.length <= 80) save(settings.copy(ownerName = it)) },
                Modifier.fillMaxWidth().heightIn(min = 62.dp).testTag("scan-owner-name"), singleLine = true,
                textStyle = FFType.bodyMain.copy(fontSize = 17.sp, lineHeight = 24.sp, color = FFColors.onSurface),
                placeholder = { Text(MR.strings.ai_owner_name_label.text(), style = FFType.bodyMain.copy(fontSize = 17.sp, lineHeight = 24.sp), color = FFColors.outline, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                trailingIcon = { IconButton({ ownerInfo = !ownerInfo }, Modifier.testTag("scan-owner-info")) {
                    FFIcon("hi_info", FFColors.onSurfaceVariant, 24.dp, contentDescription = MR.strings.pd_scan_owner_info.text())
                } },
                shape = RoundedCornerShape(20.dp), colors = fieldColors())
            if (ownerInfo) Text(MR.strings.ai_owner_name_hint.text(), Modifier.padding(start = 4.dp, top = 8.dp), style = FFType.metaData, color = FFColors.onSurfaceVariant)
            Spacer(Modifier.height(18.dp))
            Text(MR.strings.pd_scan_fields_caption.text(), Modifier.padding(horizontal = 4.dp), style = FFType.bodyMain.copy(lineHeight = 18.sp), color = FFColors.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            val moveUp = MR.strings.ai_move_up.text()
            val moveDown = MR.strings.ai_move_down.text()
            val handle = MR.strings.pd_scan_drag_handle.text()
            settings.fields.forEachIndexed { index, field ->
                key(field.id) {
                    val (title, sample, icon) = scanFieldInfo(field.id)
                    val lifted = dragging == field.id
                    Row(Modifier.padding(bottom = 8.dp).zIndex(if (lifted) 1f else 0f)
                        .graphicsLayer { translationY = if (lifted) dragOffset else 0f; scaleX = if (lifted) 1.02f else 1f; scaleY = if (lifted) 1.02f else 1f }
                        .fillMaxWidth().onSizeChanged { heights[field.id] = it.height }
                        .clip(RoundedCornerShape(20.dp)).background(if (lifted) FFColors.surfaceContainerHigh else FFColors.surface)
                        .pointerInput(field.id) {
                            detectDragGesturesAfterLongPress(onDragStart = { dragging = field.id; dragOffset = 0f },
                                onDragEnd = { endDrag() }, onDragCancel = { endDrag() },
                                onDrag = { change, amount -> change.consume(); dragBy(field.id, amount.y) })
                        }
                        .semantics {
                            customActions = listOfNotNull(
                                if (index > 0) CustomAccessibilityAction(moveUp) { move(index, -1); true } else null,
                                if (index < settings.fields.lastIndex) CustomAccessibilityAction(moveDown) { move(index, 1); true } else null,
                            )
                        }
                        .padding(start = 16.dp, end = 6.dp, top = 14.dp, bottom = 14.dp).testTag("scan-field-${field.id}"),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(FFColors.iconCircle), contentAlignment = Alignment.Center) { FFIcon(icon, FFColors.onSurface, 20.dp) }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(title, style = FFType.bodyMain.copy(fontSize = 18.sp, lineHeight = 24.sp), color = FFColors.onSurface)
                            Text(sample, style = FFType.bodyMain.copy(fontSize = 15.sp, lineHeight = 20.sp), color = FFColors.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(8.dp))
                        Switch(field.enabled, { on -> save(settings.copy(fields = settings.fields.map { if (it.id == field.id) it.copy(enabled = on) else it })) },
                            Modifier.testTag("scan-field-switch-${field.id}"), colors = scanSwitchColors())
                        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                            FFIcon("drag_handle_rounded", FFColors.onSurfaceVariant, 26.dp, contentDescription = handle)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
