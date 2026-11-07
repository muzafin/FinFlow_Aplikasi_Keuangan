package com.logikawan.finflow.feature.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.saveable.rememberSaveable
import com.logikawan.finflow.core.i18n.text
import com.logikawan.finflow.core.ui.*
import com.logikawan.finflow.i18n.*

// Komponen bersama halaman Akun & Data. Ukuran diukur dari tangkapan Ollo 1.7.9 (3 px = 1 dp).

/**
 * Ikon halaman Akun & Data. Nilai non-"hi_" adalah pengganti sementara sampai Hugeicons berikut ditambahkan ke HugeIconData:
 * hi_cloud=Cloud, hi_cloud_sync=CloudLoading, hi_logout=Logout03, hi_headset=CustomerSupport, hi_refresh=Refresh,
 * hi_undo=ArrowTurnBackward, hi_alert=Alert02, hi_archive=Archive, hi_smartphone=SmartPhone01, hi_pdf=Pdf01,
 * hi_calendar_check=CalendarCheckIn01, hi_globe=Globe02.
 */
internal object AdIcons {
    const val cloud = "hi_cloud"
    const val cloudSync = "hi_cloud_sync"
    const val logout = "hi_logout"
    const val headset = "hi_headset"
    const val refresh = "hi_refresh"
    const val undo = "hi_undo"
    const val alert = "hi_alert"
    const val archive = "hi_archive"
    const val smartphone = "hi_smartphone"
    const val pdf = "hi_pdf"
    const val calendarCheck = "hi_calendar_check"
    const val globe = "hi_globe"
}

/** Warna aksen Ollo yang punya padanan terang & gelap (bukan warna gelap saja). */
internal object AdTone {
    val infoBg: Color @Composable get() = if (FFColors.isDark) Color(0xFF10202F) else Color(0xFFECF4FC)
    val infoFg: Color @Composable get() = if (FFColors.isDark) Color(0xFF7DB4F0) else Color(0xFF4A90E2)
    val dangerBg: Color @Composable get() = if (FFColors.isDark) Color(0xFF2A1414) else Color(0xFFFDEAEA)
    val danger: Color @Composable get() = if (FFColors.isDark) Color(0xFFFF6B60) else Color(0xFFF44336)
    val orangeBg: Color @Composable get() = if (FFColors.isDark) Color(0xFF2B2013) else Color(0xFFFDF0E2)
    val orange: Color @Composable get() = if (FFColors.isDark) Color(0xFFFFB04A) else Color(0xFFF59E0B)
    val blueBg: Color @Composable get() = if (FFColors.isDark) Color(0xFF11233A) else Color(0xFFE3F0FB)
    val blue: Color @Composable get() = if (FFColors.isDark) Color(0xFF6CB2F5) else Color(0xFF2D8CEB)
    val redCircleBg: Color @Composable get() = if (FFColors.isDark) Color(0xFF2E1717) else Color(0xFFFBE4E4)
}

@Composable
internal fun adText(size: TextUnit, weight: FontWeight = FontWeight.Normal, line: TextUnit = (size.value * 1.4f).sp): TextStyle =
    FFType.bodyMain.copy(fontSize = size, fontWeight = weight, lineHeight = line)

/** Kartu abu-abu Ollo: radius 20, isi 16. */
@Composable
internal fun AdCard(modifier: Modifier = Modifier, padding: Dp = 16.dp, color: Color = FFColors.surface, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(color).padding(padding), content = content)
}

@Composable
internal fun AdCardTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = adText(18.sp, FontWeight.SemiBold, 24.sp), color = FFColors.onSurface)
}

@Composable
internal fun AdBody(text: String, modifier: Modifier = Modifier, color: Color = FFColors.onSurfaceVariant, size: TextUnit = 14.sp, align: TextAlign? = null) {
    Text(text, modifier, style = adText(size, line = (size.value * 1.45f).sp), color = color, textAlign = align)
}

/** Label bagian di atas field (Date Range, Wallet, ...). */
@Composable
internal fun AdFieldLabel(text: String) {
    Text(text, style = adText(15.sp, FontWeight.Medium, 20.sp), color = FFColors.onSurfaceVariant)
}

@Composable
internal fun AdIconCircle(icon: String, background: Color, tint: Color, size: Dp = 52.dp, iconSize: Dp = 24.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(background), contentAlignment = Alignment.Center) { FFIcon(icon, tint, iconSize) }
}

/** Tombol pil bergaris tipis (Cloud Sync, Sign Out, Read Privacy Policy ...). */
@Composable
internal fun AdPillButton(label: String, icon: String?, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = FFColors.onSurface,
    enabled: Boolean = true, height: Dp = 40.dp, border: Boolean = true, tag: String? = null) {
    val shape = RoundedCornerShape(100.dp)
    Row(
        modifier.height(height).clip(shape)
            .then(if (border) Modifier.border(1.dp, FFColors.hairline, shape) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = if (enabled) tint else tint.copy(alpha = .38f)
        if (icon != null) { FFIcon(icon, color, 20.dp); Spacer(Modifier.width(10.dp)) }
        Text(label, style = adText(15.sp, line = 20.sp), color = color, maxLines = 1)
    }
}

/** Tombol utama gelap (pill) — Upload CSV, Contact Support. */
@Composable
internal fun AdPrimaryButton(label: String, icon: String?, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    height: Dp = 56.dp, radius: Dp = 20.dp, container: Color = FFColors.pill, content: Color = FFColors.onPill, tag: String? = null) {
    val shape = RoundedCornerShape(radius)
    val bg = if (enabled) container else FFColors.onSurface.copy(alpha = .12f)
    val fg = if (enabled) content else FFColors.onSurfaceVariant
    Row(
        modifier.fillMaxWidth().height(height).clip(shape).background(bg)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { FFIcon(icon, fg, 22.dp); Spacer(Modifier.width(12.dp)) }
        Text(label, style = adText(17.sp, FontWeight.Medium, 22.sp), color = fg, maxLines = 1)
    }
}

/** Tombol bergaris tebal (Download Template, Share CSV). */
@Composable
internal fun AdOutlineButton(label: String, icon: String?, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    height: Dp = 56.dp, radius: Dp = 20.dp, strong: Boolean = true, tint: Color = FFColors.onSurface, tag: String? = null) {
    val shape = RoundedCornerShape(radius)
    val fg = if (enabled) tint else FFColors.onSurfaceVariant.copy(alpha = .5f)
    Row(
        modifier.fillMaxWidth().height(height).clip(shape)
            .border(if (strong) 1.5.dp else 1.dp, if (strong && enabled) FFColors.onSurface else FFColors.hairline, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { FFIcon(icon, fg, 22.dp); Spacer(Modifier.width(12.dp)) }
        Text(label, style = adText(17.sp, FontWeight.Medium, 22.sp), color = fg, maxLines = 1)
    }
}

/** Banner info/peringatan dengan ikon di kiri. */
@Composable
internal fun AdBanner(icon: String, body: String, background: Color, tint: Color, modifier: Modifier = Modifier, title: String? = null, bodyColor: Color = tint) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(background).padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically) {
        FFIcon(icon, tint, if (title != null) 30.dp else 22.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            if (title != null) { Text(title, style = adText(16.sp, FontWeight.Medium, 22.sp), color = tint); Spacer(Modifier.height(4.dp)) }
            Text(body, style = adText(14.sp, line = 20.sp), color = bodyColor)
        }
    }
}

/** Baris akordeon (FAQ, Import Guide). */
@Composable
internal fun AdExpandable(title: String, body: @Composable ColumnScope.() -> Unit, modifier: Modifier = Modifier, icon: String? = null, tag: String? = null,
    initiallyOpen: Boolean = false) {
    var open by rememberSaveable(title) { mutableStateOf(initiallyOpen) }
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(FFColors.surface).animateContentSize()) {
        Row(Modifier.fillMaxWidth().heightIn(min = 58.dp).clickable(role = Role.Button) { open = !open }
            .then(if (tag != null) Modifier.testTag(tag) else Modifier).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { FFIcon(icon, FFColors.onSurface, 22.dp); Spacer(Modifier.width(16.dp)) }
            Text(title, Modifier.weight(1f), style = adText(16.sp, FontWeight.Medium, 22.sp), color = FFColors.onSurface)
            Spacer(Modifier.width(12.dp))
            Box(Modifier.rotate(if (open) 180f else 0f)) { FFIcon("hi_chevron_down", FFColors.onSurface, 20.dp) }
        }
        if (open) Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp), content = body)
    }
}

@Composable
internal fun AdExpandableText(title: String, body: String, modifier: Modifier = Modifier, tag: String? = null) =
    AdExpandable(title, { AdBody(body) }, modifier, tag = tag)

/** Chip pil (This Month, All, Income...). */
@Composable
internal fun AdChip(label: String, selected: Boolean, onClick: () -> Unit, tag: String? = null) {
    val shape = RoundedCornerShape(100.dp)
    Box(Modifier.height(32.dp).clip(shape).background(if (selected) FFColors.pill else FFColors.surface)
        .clickable(role = Role.Tab, onClick = onClick).then(if (tag != null) Modifier.testTag(tag) else Modifier).padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center) {
        Text(label, style = adText(14.sp, line = 18.sp), color = if (selected) FFColors.onPill else FFColors.onSurfaceVariant, maxLines = 1)
    }
}

/** Field pilihan 72 dp dengan kotak ikon dan chevron (All Wallets, All Categories). */
@Composable
internal fun AdSelectField(label: String, icon: String, onClick: () -> Unit, tag: String? = null) {
    Row(Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(20.dp)).background(FFColors.surface)
        .clickable(role = Role.Button, onClick = onClick).then(if (tag != null) Modifier.testTag(tag) else Modifier).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(FFColors.raised), contentAlignment = Alignment.Center) { FFIcon(icon, FFColors.onSurface, 20.dp) }
        Spacer(Modifier.width(14.dp))
        Text(label, Modifier.weight(1f), style = adText(17.sp, line = 22.sp), color = FFColors.onSurface, maxLines = 1)
        FFIcon("hi_chevron_down", FFColors.onSurface, 20.dp)
    }
}

internal data class AdOption(val key: String, val label: String, val icon: String = "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdSelectSheet(title: String, options: List<AdOption>, selected: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = FFColors.canvas) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), style = adText(18.sp, FontWeight.SemiBold, 24.sp), color = FFColors.onSurface)
                FFIconButton("close_rounded", onDismiss)
            }
            LazyColumn(Modifier.heightIn(max = 460.dp), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(options, key = { it.key }) { option ->
                    val isSelected = option.key == selected
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (isSelected) FFColors.surface else Color.Transparent)
                        .clickable { onSelect(option.key) }.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (option.icon.isNotBlank()) {
                            Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(FFColors.raised), contentAlignment = Alignment.Center) { FFIcon(option.icon, FFColors.onSurface, 18.dp) }
                            Spacer(Modifier.width(12.dp))
                        }
                        Text(option.label, Modifier.weight(1f), style = adText(15.sp, line = 20.sp), color = FFColors.onSurface)
                        if (isSelected) FFIcon("hi_check", FFColors.onSurface, 20.dp)
                    }
                }
            }
        }
    }
}

/** Dialog info sederhana bergaya Ollo. */
@Composable
internal fun AdInfoDialog(title: String, lines: List<String>, onDismiss: () -> Unit, confirmLabel: String? = null, onConfirm: () -> Unit = {}, danger: Boolean = false) {
    val dangerColor = AdTone.danger
    AlertDialog(onDismissRequest = onDismiss, containerColor = FFColors.canvas, shape = RoundedCornerShape(24.dp),
        title = { Text(title, style = adText(18.sp, FontWeight.SemiBold, 24.sp), color = FFColors.onSurface) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { lines.forEach { AdBody(it) } } },
        confirmButton = {
            if (confirmLabel != null) TextButton(onClick = onConfirm) { Text(confirmLabel, color = if (danger) dangerColor else FFColors.onSurface) }
            else TextButton(onClick = onDismiss) { Text(MR.strings.common_close.text(), color = FFColors.onSurface) }
        },
        dismissButton = {
            if (confirmLabel != null) TextButton(onClick = onDismiss) { Text(MR.strings.common_cancel.text(), color = FFColors.onSurfaceVariant) }
        })
}
