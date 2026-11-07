package com.logikawan.finflow.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logikawan.finflow.core.i18n.text
import com.logikawan.finflow.core.i18n.transactionTypeLabel
import com.logikawan.finflow.core.model.*
import com.logikawan.finflow.core.ui.*
import com.logikawan.finflow.core.util.formatBalance
import com.logikawan.finflow.i18n.*

// ───────────── Tema (Ollo "Select Theme") ─────────────

internal fun LazyListScope.themePage(p: AppPreferences, update: (AppPreferences) -> Unit) {
    item { Text(MR.strings.pc_theme_subtitle.text(), style = FFType.bodyMain.copy(fontSize = 15.sp, lineHeight = 21.sp), color = FFColors.onSurfaceVariant) }
    item {
        val selected = p.theme == "system"
        PrefCard(Modifier.testTag("theme-system"), selected = selected, onClick = { update(p.copy(theme = "system")) }) {
            ThemeRowContent("hi_laptop", MR.strings.set_pref_theme_system.text(), null, selected)
        }
    }
    item { PrefSectionLabel(MR.strings.pc_theme_light_section.text(), caps = true) }
    items(lightThemeChoices, key = { it.id }) { choice -> ThemeChoiceCard(choice, p, update) }
    item { PrefSectionLabel(MR.strings.pc_theme_dark_section.text(), caps = true) }
    items(darkThemeChoices, key = { it.id }) { choice -> ThemeChoiceCard(choice, p, update) }
}

@Composable
private fun ThemeChoiceCard(choice: ThemeChoice, p: AppPreferences, update: (AppPreferences) -> Unit) {
    val selected = p.theme == choice.theme && p.palette == choice.palette
    PrefCard(Modifier.testTag("theme-${choice.id}"), selected = selected, onClick = { update(p.copy(theme = choice.theme, palette = choice.palette)) }) {
        ThemeRowContent(choice.icon, choice.label, choice.swatches, selected)
    }
}

@Composable
private fun ThemeRowContent(icon: String, label: String, swatches: List<Long>?, selected: Boolean) {
    Row(Modifier.fillMaxWidth().heightIn(min = 62.dp).padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        PrefIconBox(icon, size = 36.dp)
        Spacer(Modifier.width(13.dp))
        Text(label, Modifier.weight(1f), style = FFType.bodyMain.copy(fontSize = 16.5.sp, lineHeight = 22.sp),
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal, color = FFColors.onSurface)
        swatches?.let { PrefSwatches(it, 19.dp, 14.dp) }
        if (selected) { Spacer(Modifier.width(12.dp)); PrefCheck() }
    }
}

// ───────────── Tampilan Kartu (Ollo "Select Theme" kartu saldo) ─────────────

private val customCardColors = listOf(0xFFEF4444, 0xFFF97316, 0xFFF59E0B, 0xFFFACC15, 0xFF22C55E, 0xFF10B981, 0xFF14B8A6,
    0xFF06B6D4, 0xFF3B82F6, 0xFF6366F1, 0xFF8B5CF6, 0xFFEC4899, 0xFF64748B, 0xFF1F2937)

internal fun cardTabOf(id: String): Int = when {
    customCardGradientColors(id) != null -> 2
    allCardGradientPresets.firstOrNull { it.id == id }?.dark == true -> 1
    else -> 0
}

internal fun LazyListScope.cardPage(p: AppPreferences, update: (AppPreferences) -> Unit, tab: Int, selectTab: (Int) -> Unit) {
    item { CardTabs(tab, selectTab) }
    if (tab == 2) {
        item { CustomCardEditor(p, update) }
        return
    }
    item { PrefTitle(MR.strings.pc_preview.text()) }
    item { CardPreview(cardGradientColors(p.cardGradient), p) }
    val presets = allCardGradientPresets.filter { it.dark == (tab == 1) }
    items(presets.chunked(2), key = { row -> row.joinToString { it.id } }) { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            row.forEach { preset -> CardGradientTile(preset, preset.id == p.cardGradient, Modifier.weight(1f)) { update(p.copy(cardGradient = preset.id)) } }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun CardTabs(tab: Int, selectTab: (Int) -> Unit) {
    val labels = listOf(MR.strings.pc_card_tab_light.text(), MR.strings.pc_card_tab_dark.text(), MR.strings.pc_card_tab_custom.text())
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            labels.forEachIndexed { index, label ->
                Column(Modifier.weight(1f).clickable(role = Role.Tab) { selectTab(index) }.testTag("card-tab-$index").padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, style = FFType.bodyMain.copy(fontSize = 16.sp), color = if (index == tab) FFColors.onSurface else FFColors.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.width(34.dp).height(3.dp).clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(if (index == tab) FFColors.onSurface else Color.Transparent))
                }
            }
        }
        HorizontalDivider(color = FFColors.hairline, thickness = 1.dp)
    }
}

@Composable
private fun CardPreview(colors: List<Long>, p: AppPreferences) {
    val shape = RoundedCornerShape(24.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(Brush.linearGradient(colors.map { Color(it) }))
        .padding(horizontal = 16.dp, vertical = 18.dp).testTag("card-preview")) {
        Text(MR.strings.pc_card_preview_balance.text(), style = FFType.bodyMain.copy(fontSize = 13.sp), color = Color.White.copy(alpha = .85f))
        Spacer(Modifier.height(6.dp))
        Text(formatBalance(1_250_000_000, "IDR", p), style = FFType.amountLg.copy(fontSize = 24.sp, lineHeight = 30.sp), fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(Triple("hi_arrow_down", transactionTypeLabel("income"), 800_000_000L), Triple("hi_arrow_up", transactionTypeLabel("expense"), 320_000_000L))
                .forEach { (icon, label, value) ->
                    val pill = RoundedCornerShape(16.dp)
                    Column(Modifier.weight(1f).clip(pill).background(Color.White.copy(alpha = .12f)).border(1.dp, Color.White.copy(alpha = .28f), pill)
                        .padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FFIcon(icon, Color.White.copy(alpha = .9f), 14.dp); Spacer(Modifier.width(4.dp))
                            Text(label, style = FFType.metaData, color = Color.White.copy(alpha = .9f), maxLines = 1)
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(formatBalance(value, "IDR", p), style = FFType.amountMd.copy(fontSize = 14.sp), color = Color.White, maxLines = 1)
                    }
                }
        }
    }
}

@Composable
private fun CardGradientTile(preset: CardGradientPreset, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(modifier.height(118.dp).clip(shape).background(Brush.linearGradient(preset.colors.map { Color(it) }))
        .then(if (selected) Modifier.border(3.dp, FFColors.onSurface, shape) else Modifier)
        .clickable(role = Role.RadioButton, onClick = onClick).testTag("card-gradient-${preset.id}"), contentAlignment = Alignment.Center) {
        Text(preset.label, Modifier.padding(horizontal = 8.dp), textAlign = TextAlign.Center,
            style = FFType.bodyMain.copy(fontSize = 18.sp, lineHeight = 24.sp, shadow = Shadow(Color.Black.copy(alpha = .35f), Offset(0f, 2f), 6f)),
            fontWeight = FontWeight.SemiBold, color = Color.White)
        if (selected) Box(Modifier.align(Alignment.TopEnd).padding(10.dp).size(26.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
            FFIcon("hi_check", Color(0xFF1D1E26), 16.dp)
        }
    }
}

@Composable
private fun CustomCardEditor(p: AppPreferences, update: (AppPreferences) -> Unit) {
    val saved = customCardGradientColors(p.cardGradient)
    var start by remember(p.cardGradient) { mutableStateOf(saved?.getOrNull(0) ?: customCardColors[8]) }
    var end by remember(p.cardGradient) { mutableStateOf(saved?.getOrNull(1) ?: customCardColors[4]) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PrefTitle(MR.strings.pc_preview.text())
        CardPreview(listOf(start, end), p)
        ColorPickerRow(MR.strings.pc_card_custom_start.text(), start, "card-start") { start = it }
        ColorPickerRow(MR.strings.pc_card_custom_end.text(), end, "card-end") { end = it }
        val id = customCardGradientId(start, end)
        Button(onClick = { update(p.copy(cardGradient = id)) }, enabled = id != p.cardGradient,
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("card-custom-apply"), shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FFColors.pill, contentColor = FFColors.onPill)) {
            Text(if (id == p.cardGradient) MR.strings.pc_card_custom_applied.text() else MR.strings.pc_card_custom_apply.text(), style = FFType.bodyMain, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ColorPickerRow(title: String, value: Long, tag: String, change: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = FFType.bodyMain.copy(fontSize = 15.sp), color = FFColors.onSurfaceVariant)
        customCardColors.chunked(7).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                row.forEach { color ->
                    val selected = color == value
                    Box(Modifier.size(38.dp).clip(CircleShape).background(Color(color))
                        .border(if (selected) 3.dp else 1.dp, if (selected) FFColors.onSurface else FFColors.hairline, CircleShape)
                        .clickable(role = Role.RadioButton) { change(color) }.testTag("$tag-${color.toString(16)}"), contentAlignment = Alignment.Center) {
                        if (selected) FFIcon("hi_check", Color.White, 18.dp)
                    }
                }
            }
        }
    }
}

// ───────────── Tampilan Menu (Ollo "Menu Appearance") ─────────────

internal val menuIconsById = mapOf("Anggaran" to "hi_percent", "Berulang" to "hi_recurring", "Target" to "hi_piggy", "Tagihan" to "hi_bill", "Utang" to "hi_debt",
    "Keinginan" to "hi_wish", "Kartu" to "hi_card", "Catatan" to "hi_note", "Reimburse" to "hi_reimburse", "Investasi" to "hi_invest")

internal fun LazyListScope.menuAppearancePage(p: AppPreferences, update: (AppPreferences) -> Unit, menuName: (String) -> String) {
    item { PrefTitle(MR.strings.pc_preview.text()) }
    item { MenuPreview(p, menuName) }
    item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PrefSectionLabel(MR.strings.set_pref_home_menu_layout.text())
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(FFColors.chip).padding(4.dp)) {
                listOf("horizontal" to MR.strings.set_pref_menu_horizontal.text(), "grid" to MR.strings.set_pref_menu_grid.text(), "list" to MR.strings.set_pref_menu_list.text())
                    .forEach { (id, label) ->
                        val selected = p.menuAppearance == id
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(if (selected) FFColors.pill else Color.Transparent)
                            .clickable(role = Role.RadioButton) { update(p.copy(menuAppearance = id)) }.testTag("menu-layout-$id").padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center) {
                            Text(label, style = FFType.bodyMain.copy(fontSize = 13.sp), color = if (selected) FFColors.onPill else FFColors.onSurfaceVariant, maxLines = 1)
                        }
                    }
            }
        }
    }
    item { PrefSectionLabel(MR.strings.pc_menu_mixed_section.text()) }
    items(allMenuThemePresets.filterNot { it.single }, key = { it.id }) { preset -> MenuThemeRow(preset, p, update) }
    item { PrefSectionLabel(MR.strings.pc_menu_single_section.text()) }
    items(allMenuThemePresets.filter { it.single }, key = { it.id }) { preset -> MenuThemeRow(preset, p, update) }
}

@Composable
private fun MenuPreview(p: AppPreferences, menuName: (String) -> String) {
    val colors = menuThemeColors(p.menuTheme)
    val menus = p.menuOrder.filterNot { it in p.hiddenMenus }.take(5)
    PrefCard {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            menus.forEach { id ->
                val accent = Color(colors[financeMenus.indexOf(id).coerceAtLeast(0) % colors.size])
                Column(Modifier.width(62.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
                        FFIcon(menuIconsById[id] ?: "hi_grid", accent, 24.dp)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(menuName(id), style = FFType.metaData.copy(fontSize = 12.sp), color = FFColors.onSurfaceVariant, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun MenuThemeRow(preset: MenuThemePreset, p: AppPreferences, update: (AppPreferences) -> Unit) {
    val selected = p.menuTheme == preset.id
    PrefCard(Modifier.testTag("menu-theme-${preset.id}"), selected = selected, onClick = { update(p.copy(menuTheme = preset.id)) }) {
        Row(Modifier.fillMaxWidth().heightIn(min = 54.dp).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            val swatch = if (preset.single) List(4) { preset.colors.first() } else preset.colors.take(4)
            PrefSwatches(swatch, 20.dp, 24.dp)
            Spacer(Modifier.width(16.dp))
            Text(preset.label, Modifier.weight(1f), style = FFType.bodyMain.copy(fontSize = 16.5.sp, lineHeight = 22.sp), color = FFColors.onSurface)
            if (selected) PrefCheck()
        }
    }
}

// ───────────── Palet Transaksi (Ollo "Transaction Palette") ─────────────

internal fun LazyListScope.transactionPalettePage(p: AppPreferences, update: (AppPreferences) -> Unit) {
    item { PrefTitle(MR.strings.pc_preview.text()) }
    item { TransactionPalettePreview(p) }
    item { PrefTitle(MR.strings.pc_palette_select.text(), Modifier.padding(top = 8.dp)) }
    val current = transactionPaletteOf(p)
    items(transactionPalettes, key = { it.id }) { palette ->
        val selected = current?.id == palette.id
        PrefCard(Modifier.testTag("tx-palette-${palette.id}"), selected = selected,
            onClick = { update(p.copy(incomeColor = palette.income, expenseColor = palette.expense, transferColor = palette.transfer)) }) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(24.dp).clip(CircleShape).background(if (selected) FFColors.pill else FFColors.raised), contentAlignment = Alignment.Center) {
                    if (selected) FFIcon("hi_check", FFColors.onPill, 14.dp)
                }
                Spacer(Modifier.width(16.dp))
                Text(palette.label, Modifier.weight(1f), style = FFType.bodyMain.copy(fontSize = 16.5.sp, lineHeight = 22.sp), color = FFColors.onSurface)
                PrefSwatches(listOf(palette.income, palette.expense, palette.transfer), 22.dp, 30.dp)
            }
        }
    }
}

@Composable
private fun TransactionPalettePreview(p: AppPreferences) {
    val rows = listOf(
        PreviewTx("restaurant", MR.strings.pc_preview_lunch.text(), MR.strings.pc_preview_today.text(), "- " + formatBalance(5_000_000, "IDR", p), p.expenseColor),
        PreviewTx("hi_bag", MR.strings.pc_preview_salary.text(), MR.strings.pc_preview_yesterday.text(), "+ " + formatBalance(500_000_000, "IDR", p), p.incomeColor),
        PreviewTx("hi_sort", MR.strings.pc_preview_transfer.text(), MR.strings.pc_preview_date.text(), "- " + formatBalance(100_000_000, "IDR", p), p.transferColor),
    )
    PrefCard(Modifier.testTag("tx-palette-preview")) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            rows.forEachIndexed { index, row ->
                if (index > 0) PrefDivider()
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Color(row.color).copy(alpha = .15f)), contentAlignment = Alignment.Center) {
                        FFIcon(row.icon, Color(row.color), 20.dp)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(row.title, style = FFType.bodyMain.copy(fontSize = 16.sp, lineHeight = 22.sp), color = FFColors.onSurface, maxLines = 1)
                        Text(row.subtitle, style = FFType.bodyMain.copy(fontSize = 13.sp), color = FFColors.onSurfaceVariant)
                    }
                    Text(row.amount, style = FFType.amountMd.copy(fontSize = 16.sp), fontWeight = FontWeight.Bold, color = Color(row.color), maxLines = 1)
                }
            }
        }
    }
}

private data class PreviewTx(val icon: String, val title: String, val subtitle: String, val amount: String, val color: Long)

// ───────────── Paket Ikon (Ollo "Icon Pack") ─────────────

private val iconPackPreview = listOf("account_balance_wallet", "shopping_bag", "restaurant", "directions_car", "home")

internal fun LazyListScope.iconPackPage(p: AppPreferences, update: (AppPreferences) -> Unit) {
    item {
        Row(Modifier.fillMaxWidth().clip(PrefShape).background(FFColors.surfaceContainerHigh).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            FFIcon("hi_info", FFColors.onSurface, 22.dp)
            Spacer(Modifier.width(14.dp))
            Text(MR.strings.pc_icon_info.text(), style = FFType.bodyMain.copy(fontSize = 14.5.sp, lineHeight = 20.sp), color = FFColors.onSurface)
        }
    }
    items(listOf("outlined" to (MR.strings.pc_icon_outlined to MR.strings.pc_icon_outlined_desc), "rounded" to (MR.strings.pc_icon_rounded to MR.strings.pc_icon_rounded_desc)),
        key = { it.first }) { (id, labels) ->
        val selected = p.iconPack == id
        PrefCard(Modifier.testTag("icon-pack-$id"), selected = selected, selectedFill = false, onClick = { update(p.copy(iconPack = id)) }) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(labels.first.text(), style = FFType.sectionTitle.copy(fontSize = 18.sp), fontWeight = FontWeight.Medium, color = FFColors.onSurface)
                        Text(labels.second.text(), style = FFType.bodyMain.copy(fontSize = 13.5.sp), color = FFColors.onSurfaceVariant)
                    }
                    if (selected) PrefCheck(filled = true) else PrefRadio(false, 24.dp)
                }
                Spacer(Modifier.height(20.dp))
                // Pratinjau memakai FFIcon asli dengan preferensi paket ini, jadi hasilnya sama dengan di aplikasi.
                CompositionLocalProvider(LocalPreferences provides p.copy(iconPack = id)) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        iconPackPreview.forEach { FFIcon(it, FFColors.onSurface, 28.dp) }
                    }
                }
            }
        }
    }
}
