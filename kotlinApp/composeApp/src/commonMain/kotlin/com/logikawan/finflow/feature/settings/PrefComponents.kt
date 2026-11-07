package com.logikawan.finflow.feature.settings

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logikawan.finflow.core.ui.*

// Komponen bersama halaman Preferensi, diukur dari tangkapan Ollo 1.7.9 (1 dp = 3 px).
internal val PrefShape = RoundedCornerShape(16.dp)

/** Kartu baris Ollo: permukaan abu muda; terpilih = bingkai tebal (dan latar sedikit lebih gelap bila [selectedFill]). */
@Composable
internal fun PrefCard(modifier: Modifier = Modifier, selected: Boolean = false, selectedFill: Boolean = true, onClick: (() -> Unit)? = null,
                      content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(PrefShape)
        .background(if (selected && selectedFill) FFColors.surfaceContainerHigh else FFColors.surface)
        .then(if (selected) Modifier.border(2.dp, FFColors.onSurface, PrefShape) else Modifier)
        .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier), content = content)
}

/** Judul bagian tebal ("Preview", "Hide Balance Appearance"). */
@Composable
internal fun PrefTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.padding(top = 4.dp), style = FFType.sectionTitle.copy(fontSize = 19.sp, lineHeight = 26.sp), fontWeight = FontWeight.SemiBold, color = FFColors.onSurface)
}

/** Label bagian abu-abu; [caps] untuk gaya "LIGHT THEMES". */
@Composable
internal fun PrefSectionLabel(text: String, caps: Boolean = false, modifier: Modifier = Modifier) {
    if (caps) Text(text.uppercase(), modifier.padding(top = 8.dp), style = FFType.metaData.copy(fontSize = 12.5.sp, letterSpacing = 1.2.sp),
        fontWeight = FontWeight.Medium, color = FFColors.onSurfaceVariant)
    else Text(text, modifier.padding(top = 8.dp), style = FFType.bodyMain.copy(fontSize = 16.sp, lineHeight = 22.sp), color = FFColors.onSurfaceVariant)
}

/** Kotak ikon Ollo (lingkaran atau persegi membulat) dengan latar iconCircle. */
@Composable
internal fun PrefIconBox(icon: String, circle: Boolean = false, size: Dp = 40.dp, tint: Color = FFColors.onSurface, background: Color = FFColors.iconCircle) {
    Box(Modifier.size(size).clip(if (circle) CircleShape else RoundedCornerShape(12.dp)).background(background), contentAlignment = Alignment.Center) {
        FFIcon(icon, tint, size * .5f)
    }
}

@Composable
internal fun PrefSwitch(checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Switch(checked, onChange, modifier, enabled = enabled, colors = SwitchDefaults.colors(
        checkedTrackColor = FFColors.pill, checkedThumbColor = FFColors.onPill, checkedBorderColor = Color.Transparent,
        uncheckedTrackColor = FFColors.raised, uncheckedThumbColor = FFColors.outline, uncheckedBorderColor = Color.Transparent))
}

/** Baris kartu: ikon opsional, judul + subjudul, dan elemen kanan (chevron bawaan bila dapat diketuk). */
@Composable
internal fun PrefRow(title: String, subtitle: String? = null, icon: String? = null, iconCircle: Boolean = false, onClick: (() -> Unit)? = null,
                     modifier: Modifier = Modifier, titleColor: Color = FFColors.onSurface, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().heightIn(min = 60.dp)
        .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
        .padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) { PrefIconBox(icon, iconCircle); Spacer(Modifier.width(16.dp)) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = FFType.bodyMain.copy(fontSize = 16.sp, lineHeight = 22.sp), color = titleColor)
            subtitle?.let { Text(it, style = FFType.bodyMain.copy(fontSize = 13.5.sp, lineHeight = 18.sp), color = FFColors.onSurfaceVariant) }
        }
        if (trailing != null) { Spacer(Modifier.width(12.dp)); trailing() }
        else if (onClick != null) { Spacer(Modifier.width(12.dp)); FFIcon("hi_chevron_right", FFColors.onSurfaceVariant, 20.dp) }
    }
}

@Composable
internal fun PrefSwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit, tag: String, icon: String? = null, enabled: Boolean = true) {
    PrefRow(title, subtitle, icon, iconCircle = true, onClick = { if (enabled) onChange(!checked) }, modifier = Modifier.testTag(tag)) {
        PrefSwitch(checked, onChange, enabled = enabled)
    }
}

@Composable
internal fun PrefDivider() = HorizontalDivider(color = FFColors.hairline, thickness = 1.dp)

/** Radio Ollo: lingkaran bergaris; terpilih berisi titik. */
@Composable
internal fun PrefRadio(selected: Boolean, size: Dp = 22.dp) {
    Box(Modifier.size(size).border(2.dp, if (selected) FFColors.onSurface else FFColors.outline, CircleShape), contentAlignment = Alignment.Center) {
        if (selected) Box(Modifier.size(size * .5f).clip(CircleShape).background(FFColors.onSurface))
    }
}

/** Tanda terpilih: lingkaran bergaris dengan centang (Tema) atau lingkaran penuh (Paket Ikon). */
@Composable
internal fun PrefCheck(filled: Boolean = false, size: Dp = 24.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(if (filled) FFColors.pill else Color.Transparent)
        .then(if (filled) Modifier else Modifier.border(2.dp, FFColors.onSurface, CircleShape)), contentAlignment = Alignment.Center) {
        FFIcon("hi_check", if (filled) FFColors.onPill else FFColors.onSurface, size * .6f)
    }
}

/** Lingkaran warna yang saling menumpuk (Tema) atau berjajar (Menu/Palet) bila [step] >= [size]. */
@Composable
internal fun PrefSwatches(colors: List<Long>, size: Dp = 20.dp, step: Dp = 14.dp) {
    val ring = FFColors.hairline
    Box(Modifier.width(size + step * (colors.size - 1).coerceAtLeast(0)).height(size)) {
        colors.forEachIndexed { index, color ->
            Box(Modifier.offset(x = step * index).size(size).clip(CircleShape).background(Color(color)).border(1.dp, ring, CircleShape))
        }
    }
}

/** Lembar pilihan bawah dengan radio, dipakai untuk pilihan satu nilai. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PrefChoiceSheet(title: String, options: List<Pair<String, String>>, selected: String, tagPrefix: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = FFColors.canvas) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(title, Modifier.padding(horizontal = 4.dp, vertical = 8.dp), style = FFType.sectionTitle, fontWeight = FontWeight.SemiBold)
            LazyColumn(Modifier.heightIn(max = 480.dp), contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(options, key = { it.first }) { (key, label) ->
                    PrefCard(selected = key == selected, selectedFill = false, onClick = { onSelect(key); onDismiss() }, modifier = Modifier.testTag("$tagPrefix-$key")) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(label, Modifier.weight(1f), style = FFType.bodyMain.copy(fontSize = 16.sp), color = FFColors.onSurface)
                            PrefRadio(key == selected)
                        }
                    }
                }
            }
        }
    }
}
