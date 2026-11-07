package com.logikawan.finflow.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logikawan.finflow.core.platform.NativeServices
import com.logikawan.finflow.core.ui.*
import com.logikawan.finflow.core.i18n.menuLabel
import com.logikawan.finflow.core.i18n.tabLabel
import com.logikawan.finflow.core.i18n.text
import com.logikawan.finflow.i18n.*
import kotlinx.coroutines.delay

// Acuan Ollo: px/3 = dp. Ukuran teks memakai token FFType dengan variasi .copy().
private const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.logikawan.finflow"
private val BugBannerBlue = Color(0xFF4A90E2)
private val BugBannerBackground = Color(0xFFECF4FC)
private val BugBannerBody = Color(0xFF757575)
private val BugOrange = Color(0xFFFF9800)
private val BugLow = Color(0xFF4CAF50)
private val BugHigh = Color(0xFFFF5722)
private val BugCritical = Color(0xFFF44336)

private val bugFeatures = listOf(
    "Beranda" to "home_outlined",
    "Transaksi" to "swap_horiz",
    "Dompet" to "account_balance_wallet_outlined",
    "Anggaran" to "pie_chart_outline_rounded",
    "Berulang" to "autorenew_rounded",
    "Target" to "savings_rounded",
    "Tagihan" to "receipt_long_outlined",
    "Utang" to "south_west",
    "Keinginan" to "favorite_border_rounded",
    "Kartu" to "credit_card_outlined",
    "Catatan" to "note_outlined",
    "Reimburse" to "payments_outlined",
    "Investasi" to "trending_up_rounded",
    "Statistik" to "bar_chart_outlined",
    "Profil" to "person_outline",
    "Pengaturan" to "settings_outlined",
)
private val bugSeverities = listOf("Rendah", "Sedang", "Tinggi", "Kritis")

// Nilai fitur/keparahan/label perangkat tetap berbahasa Indonesia karena dikirim ke pengembang lewat email;
// fungsi berikut hanya untuk teks di layar.
private fun bugFeatureLabel(id: String): String = when (id) {
    "Beranda", "Statistik", "Dompet", "Profil" -> tabLabel(id)
    "Transaksi" -> MR.strings.set_bug_feature_transactions.text()
    "Pengaturan" -> MR.strings.set_settings.text()
    else -> menuLabel(id)
}

private fun bugSeverityLabel(id: String): String = when (id) {
    "Rendah" -> MR.strings.set_bug_severity_low.text()
    "Sedang" -> MR.strings.set_bug_severity_medium.text()
    "Tinggi" -> MR.strings.set_bug_severity_high.text()
    "Kritis" -> MR.strings.set_bug_severity_critical.text()
    else -> id
}

private fun deviceRowLabel(id: String): String = when (id) {
    "Aplikasi" -> MR.strings.set_bug_dev_app.text()
    "Perangkat" -> MR.strings.set_bug_dev_device.text()
    "Layar" -> MR.strings.set_bug_dev_screen.text()
    "Skala font" -> MR.strings.set_bug_dev_font_scale.text()
    "Bahasa" -> MR.strings.set_bug_dev_language.text()
    "Zona waktu" -> MR.strings.set_bug_dev_timezone.text()
    else -> id
}

private fun deviceRowValue(value: String): String = if (value == "Tidak tersedia") MR.strings.set_bug_unavailable.text() else value

@Composable private fun bugSectionStyle() = FFType.sectionTitle.copy(fontSize = 16.sp, lineHeight = 22.sp)
@Composable private fun bugValueStyle() = FFType.bodyMain.copy(fontSize = 16.sp, lineHeight = 22.sp)
@Composable private fun bugChipStyle() = FFType.bodyMain.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)

fun bugReportBody(feature: String, severity: String, title: String, description: String, steps: String, diagnostics: String): String {
    require(title.isNotBlank() && description.isNotBlank())
    require(title.length <= 160 && description.length <= 4000 && steps.length <= 4000)
    return "Fitur: $feature\nKeparahan: $severity\nJudul: ${title.trim()}\n\nDeskripsi\n${description.trim()}\n\nLangkah reproduksi\n${steps.trim().ifBlank { "Tidak diisi" }}\n\nInformasi Perangkat\n$diagnostics"
}

private fun deviceInfoRows(native: NativeServices): List<Pair<String, String>> {
    val fields = native.deviceDiagnostics().lineSequence().mapNotNull { line ->
        val separator = line.indexOf(':')
        if (separator <= 0) null else line.substring(0, separator).trim() to line.substring(separator + 1).trim()
    }.toMap()
    fun field(vararg keys: String) = keys.firstNotNullOfOrNull { fields[it] }
    val platform = field("OS") ?: (field("Perangkat") ?: "").substringAfter(" · ", "").takeIf { it.isNotEmpty() } ?: "Android / iOS"
    val os = buildString {
        append(platform)
        field("Build", "Build OS")?.let { append(" · build ").append(it) }
    }
    val language = (field("Bahasa", "Bahasa sistem") ?: "").substringBefore(',').substringBefore('-').trim().uppercase()
    return listOf(
        "Aplikasi" to native.appVersion(),
        "Perangkat" to (field("Perangkat") ?: native.deviceDescription()),
        "OS" to os,
        "ABI" to (field("ABI", "Arsitektur") ?: "Tidak tersedia"),
        "Layar" to (field("Layar") ?: "Tidak tersedia"),
        "Skala font" to (field("Skala font", "Ukuran teks") ?: "Tidak tersedia"),
        "RAM" to (field("RAM")?.substringBefore(';') ?: "Tidak tersedia"),
        "Bahasa" to language.ifBlank { "Tidak tersedia" },
        "Zona waktu" to (field("Zona waktu") ?: "Tidak tersedia"),
    )
}

@Composable
fun BugReportForm(native: NativeServices) {
    var feature by remember { mutableStateOf("Beranda") }
    var severity by remember { mutableStateOf("Sedang") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var steps by remember { mutableStateOf("") }
    var includeDevice by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }
    val deviceRows = remember { deviceInfoRows(native) }
    val uriHandler = LocalUriHandler.current
    val canSend = title.isNotBlank() && description.isNotBlank()
    LaunchedEffect(message) {
        if (message != null) { delay(4000); message = null }
    }
    fun sendReport() {
        val diagnostics = if (includeDevice) deviceRows.joinToString("\n") { "${it.first}: ${it.second}" } else "Tidak disertakan"
        val failure = native.composeSupport("FinFlow · Bug: ${title.trim()}", bugReportBody(feature, severity, title, description, steps, diagnostics))
        message = failure ?: MR.strings.set_bug_draft_opened.text()
    }
    Column(Modifier.fillMaxWidth().testTag("bug-report-form")) {
        UpdateBanner { try { uriHandler.openUri(PLAY_STORE_URL) } catch (_: Exception) { message = MR.strings.set_bug_store_failed.text() } }
        Spacer(Modifier.height(24.dp))
        DeviceInformationCard(deviceRows, includeDevice, { includeDevice = it })
        Spacer(Modifier.height(16.dp))
        BugSectionLabel(MR.strings.set_bug_feature.text())
        Spacer(Modifier.height(12.dp))
        FeatureField(feature, { feature = it })
        Spacer(Modifier.height(16.dp))
        BugSectionLabel(MR.strings.set_bug_severity.text())
        Spacer(Modifier.height(12.dp))
        SeverityRow(severity) { severity = it }
        Spacer(Modifier.height(16.dp))
        BugSectionLabel(MR.strings.set_bug_title.text())
        Spacer(Modifier.height(12.dp))
        BugTextField(title, { title = it.take(160) }, MR.strings.set_bug_title_hint.text(), height = 53.dp, singleLine = true)
        Spacer(Modifier.height(16.dp))
        BugSectionLabel(MR.strings.set_bug_description.text())
        Spacer(Modifier.height(12.dp))
        BugTextField(description, { description = it.take(4000) }, MR.strings.set_bug_description_hint.text(), height = 116.dp)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BugSectionLabel(MR.strings.set_bug_steps.text())
            Spacer(Modifier.width(8.dp))
            OptionalChip()
        }
        Spacer(Modifier.height(12.dp))
        BugTextField(steps, { steps = it.take(4000) }, MR.strings.set_bug_steps_hint.text(), height = 95.dp)
        Spacer(Modifier.height(32.dp))
        message?.let {
            Snackbar(Modifier.fillMaxWidth().padding(bottom = 12.dp)) { Text(it, style = FFType.bodyMain) }
        }
        SendReportButton(canSend, ::sendReport)
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun UpdateBanner(onOpenStore: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(BugBannerBackground)
            .clickable(role = Role.Button, onClickLabel = MR.strings.set_bug_open_store.text(), onClick = onOpenStore)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FFIcon("system_update_outlined", BugBannerBlue, 24.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(MR.strings.set_bug_update_title.text(), style = bugSectionStyle(), color = BugBannerBlue)
            Spacer(Modifier.height(4.dp))
            Text(MR.strings.set_bug_update_body.text(), style = FFType.bodyMain.copy(lineHeight = 18.sp), color = BugBannerBody)
        }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            FFIcon("open_in_new_outlined", BugBannerBlue, 20.dp, MR.strings.set_bug_open_store.text())
        }
    }
}

@Composable
private fun DeviceInformationCard(rows: List<Pair<String, String>>, include: Boolean, onInclude: (Boolean) -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(FFColors.surface).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FFIcon("info_outline_rounded", FFColors.onSurface, 20.dp)
            Spacer(Modifier.width(8.dp))
            Text(MR.strings.set_bug_device_info.text(), style = bugSectionStyle(), color = FFColors.onSurface)
        }
        Spacer(Modifier.height(12.dp))
        rows.forEachIndexed { index, (label, value) ->
            if (index > 0) Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                Text(deviceRowLabel(label), Modifier.weight(1f), style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Text(deviceRowValue(value), Modifier.weight(1.8f), style = FFType.bodyMain, color = FFColors.onSurfaceVariant, textAlign = TextAlign.End)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(role = Role.Checkbox) { onInclude(!include) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = include,
                onCheckedChange = null,
                modifier = Modifier.size(20.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = FFColors.onSurface,
                    uncheckedColor = FFColors.outline,
                    checkmarkColor = FFColors.canvas,
                ),
            )
            Spacer(Modifier.width(12.dp))
            Text(MR.strings.set_bug_include_device.text(), style = FFType.bodyMain, color = FFColors.onSurface)
        }
    }
}

@Composable
private fun BugSectionLabel(text: String) {
    Text(text, style = bugSectionStyle(), color = FFColors.onSurface)
}

@Composable
private fun OptionalChip() {
    Text(
        MR.strings.set_bug_optional.text(),
        Modifier.clip(RoundedCornerShape(100.dp)).background(FFColors.surface)
            .border(1.dp, FFColors.onSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(100.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        style = bugChipStyle(),
        color = FFColors.onSurfaceVariant,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeatureField(feature: String, onPick: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val icon = bugFeatures.firstOrNull { it.first == feature }?.second ?: "category"
    Row(
        Modifier.fillMaxWidth().height(68.dp).clip(RoundedCornerShape(16.dp)).background(FFColors.surface)
            .clickable(role = Role.Button) { expanded = true }.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(FFColors.outlineVariant), contentAlignment = Alignment.Center) {
            FFIcon(icon, FFColors.onSurface, 20.dp)
        }
        Spacer(Modifier.width(12.dp))
        Text(bugFeatureLabel(feature), Modifier.weight(1f), style = bugValueStyle(), color = FFColors.onSurface)
        FFIcon("expand_more_rounded", FFColors.onSurfaceVariant, 20.dp)
    }
    if (expanded) ModalBottomSheet(
        onDismissRequest = { expanded = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = FFColors.canvas,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(MR.strings.set_bug_feature.text(), Modifier.weight(1f), style = FFType.sectionTitle)
                FFIconButton("close_rounded", { expanded = false })
            }
            LazyColumn(Modifier.heightIn(max = 460.dp), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(bugFeatures) { (name, featureIcon) ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(if (name == feature) FFColors.primaryContainer else Color.Transparent)
                            .clickable { onPick(name); expanded = false }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(FFColors.outlineVariant), contentAlignment = Alignment.Center) {
                            FFIcon(featureIcon, FFColors.onSurface, 18.dp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(bugFeatureLabel(name), Modifier.weight(1f), style = FFType.bodyMain, color = FFColors.onSurface)
                        if (name == feature) FFIcon("check_rounded", FFColors.primary, 20.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SeverityRow(selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        bugSeverities.forEach { label ->
            val color = when (label) {
                "Rendah" -> BugLow
                "Tinggi" -> BugHigh
                "Kritis" -> BugCritical
                else -> BugOrange
            }
            val shape = RoundedCornerShape(16.dp)
            val isSelected = label == selected
            Box(
                Modifier.weight(1f).height(45.dp)
                    .then(if (isSelected) Modifier.shadow(12.dp, shape, ambientColor = BugOrange, spotColor = BugOrange) else Modifier)
                    .clip(shape)
                    .background(if (isSelected) BugOrange else FFColors.surface)
                    .clickable(role = Role.Tab) { onSelect(label) },
                contentAlignment = Alignment.Center,
            ) {
                Text(bugSeverityLabel(label), style = bugChipStyle(), color = if (isSelected) Color.White else color)
            }
        }
    }
}

@Composable
private fun BugTextField(value: String, onValueChange: (String) -> Unit, placeholder: String, height: Dp, singleLine: Boolean = false) {
    val shape = RoundedCornerShape(16.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().height(height).clip(shape).background(FFColors.surface).border(1.dp, FFColors.outlineVariant, shape),
        textStyle = FFType.bodyMain.copy(lineHeight = 21.sp, color = FFColors.onSurface),
        cursorBrush = SolidColor(FFColors.onSurface),
        singleLine = singleLine,
        decorationBox = { innerTextField ->
            Box(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
                if (value.isEmpty()) {
                    Text(placeholder, style = FFType.bodyMain.copy(lineHeight = 21.sp), color = FFColors.onSurfaceVariant.copy(alpha = 0.45f))
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun SendReportButton(enabled: Boolean, onClick: () -> Unit) {
    val container = if (enabled) FFColors.onSurface else FFColors.onSurface.copy(alpha = 0.12f)
    val content = if (enabled) FFColors.canvas else FFColors.onSurfaceVariant
    Row(
        Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp)).background(container)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FFIcon("send_outlined", content, 20.dp)
        Spacer(Modifier.width(12.dp))
        Text(MR.strings.set_bug_send.text(), style = bugValueStyle().copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = content)
    }
}
