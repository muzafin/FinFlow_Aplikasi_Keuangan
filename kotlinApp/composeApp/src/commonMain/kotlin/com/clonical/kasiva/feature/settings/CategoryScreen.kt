package com.clonical.kasiva.feature.settings

import androidx.compose.animation.animateContentSize
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.i18n.transactionTypeLabel
import com.clonical.kasiva.core.model.*
import com.clonical.kasiva.core.ui.*
import com.clonical.kasiva.i18n.*

/** Kategori yang dibuat dan dikelola aplikasi (mis. "notes" untuk Catatan/bundel); tampil di tab Sistem saja. */
private val systemCategoryIds = setOf("notes")

private fun List<Category>.ordered(order: List<String>): List<Category> {
    if (order.isEmpty()) return this
    val rank = order.withIndex().associate { it.value to it.index }
    return withIndex().sortedBy { rank[it.value.id] ?: (order.size + it.index) }.map { it.value }
}

/**
 * Daftar kategori bergaya Ollo: tab Pengeluaran/Pemasukan/Kategori Sistem, kartu kategori utama yang dapat dibuka
 * untuk melihat subkategori, aksi header Urutkan & Ubah warna cepat, serta tombol + mengambang.
 * [addSub] membuka editor subkategori untuk induk tertentu; [saveCategory] menyimpan perubahan warna cepat.
 * Keduanya opsional: bila null, kontrol terkait disembunyikan.
 */
@Composable
fun CategoryScreen(data: FinanceData, busy: Boolean = false, edit: (Category?) -> Unit,
    addSub: ((Category) -> Unit)? = null, saveCategory: ((Category) -> Unit)? = null) {
    var tab by rememberSaveable { mutableStateOf("expense") }
    var reorder by rememberSaveable { mutableStateOf(false) }
    var quickColor by rememberSaveable { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf(listOf<String>()) }
    var colorTarget by remember { mutableStateOf<Category?>(null) }
    val prefs = LocalPreferences.current
    val store = LocalPreferenceState.current
    val order = prefs.categoryOrder
    fun saveOrder(ids: List<String>) {
        runCatching { store?.update(prefs.copy(categoryOrder = ids.distinct().take(1000))) }
    }
    TopBarActions {
        if (tab != "system") IconButton({ reorder = !reorder; if (reorder) quickColor = false }, Modifier.testTag("category-reorder")) {
            FFIcon(if (reorder) "hi_check" else "hi_sort", FFColors.onSurface, 24.dp, MR.strings.pb_cat_reorder.text())
        }
        if (saveCategory != null && tab != "system") IconButton({ quickColor = !quickColor; if (quickColor) reorder = false }, Modifier.testTag("category-quick-color")) {
            FFIcon(if (quickColor) "hi_check" else "hi_paint", FFColors.onSurface, 24.dp, MR.strings.pb_cat_quick_color.text())
        }
    }
    val categories = data.categories.filter { it.type == tab && it.id !in systemCategoryIds }
    val roots = categories.filter { it.parentId == null }.ordered(order)
    Box(Modifier.fillMaxSize().background(FFColors.background).testTag("categories-page")) {
        Column(Modifier.fillMaxSize()) {
            CategoryTabs(tab) { tab = it; reorder = false; quickColor = false }
            LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("categories-list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (tab == "system") {
                    item { Text(MR.strings.pb_cat_system_hint.text(), style = FFType.metaData, color = FFColors.onSurfaceVariant) }
                    item { SystemRow(transactionTypeLabel("transfer"), "hi_swap") }
                    items(data.categories.filter { it.id in systemCategoryIds }, key = { it.id }) { category ->
                        SystemRow(category.name, category.icon.ifBlank { categoryIcon(category.id) })
                    }
                } else {
                    if (roots.isEmpty()) item {
                        Text(MR.strings.set_cat_empty.text(), Modifier.padding(vertical = 24.dp), style = FFType.bodyMain, color = FFColors.outline)
                    }
                    items(roots, key = { it.id }) { root ->
                        val children = categories.filter { it.parentId == root.id }.ordered(order)
                        val index = roots.indexOf(root)
                        RootCard(root, children, root.id in expanded, busy, reorder, quickColor,
                            toggle = { expanded = if (root.id in expanded) expanded - root.id else expanded + root.id },
                            edit = edit, addSub = addSub, pickColor = { colorTarget = it },
                            move = { by ->
                                val ids = roots.map { it.id }.toMutableList()
                                val to = index + by
                                if (to in ids.indices) {
                                    ids.add(to, ids.removeAt(index))
                                    // Pertahankan urutan tab lain dan subkategori yang sudah tersimpan.
                                    saveOrder(ids + order.filter { it !in ids })
                                }
                            },
                            moveChild = { child, by ->
                                val ids = children.map { it.id }.toMutableList()
                                val from = ids.indexOf(child.id); val to = from + by
                                if (from >= 0 && to in ids.indices) {
                                    ids.add(to, ids.removeAt(from))
                                    val base = order.ifEmpty { data.categories.map { it.id } }.filter { it !in ids }
                                    saveOrder(base + ids)
                                }
                            })
                    }
                    item { Text(MR.strings.set_cat_hint.text(), style = FFType.metaData, color = FFColors.outline) }
                }
            }
        }
        if (tab != "system") Box(Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 24.dp).size(60.dp)
            .shadow(10.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp)).background(FFColors.pill)
            .clickable(enabled = !busy, role = Role.Button) { edit(null) }.testTag("category-add"), contentAlignment = Alignment.Center) {
            FFIcon("hi_add", FFColors.onPill, 26.dp, MR.strings.set_cat_add_title.text())
        }
    }
    colorTarget?.let { target ->
        QuickColorSheet(target, { colorTarget = null }) { color ->
            saveCategory?.invoke(target.copy(color = color)); colorTarget = null
        }
    }
}

@Composable
private fun CategoryTabs(selected: String, select: (String) -> Unit) {
    val tabs = listOf("expense" to transactionTypeLabel("expense"), "income" to transactionTypeLabel("income"), "system" to MR.strings.pb_cat_system_tab.text())
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            tabs.forEach { (value, label) ->
                val isSelected = value == selected
                Column(Modifier.weight(1f).clickable(role = Role.Tab) { select(value) }.semantics { this.selected = isSelected }
                    .testTag("category-tab-$value").padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, style = FFType.bodyMain.copy(fontSize = 17.sp, lineHeight = 22.sp), color = if (isSelected) FFColors.onSurface else FFColors.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.width(56.dp).height(3.dp).clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(if (isSelected) FFColors.onSurface else Color.Transparent))
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(FFColors.hairline))
    }
}

@Composable
private fun CategoryIconTile(category: Category, size: Int, enabled: Boolean, onClick: (() -> Unit)?) {
    val color = category.color?.let { Color(it) } ?: categoryColor(category.id)
    Box(Modifier.size(size.dp).clip(RoundedCornerShape((size * .3f).dp)).background(color.copy(alpha = .14f))
        .then(if (onClick != null) Modifier.border(1.5.dp, color, RoundedCornerShape((size * .3f).dp)).clickable(enabled = enabled, role = Role.Button, onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center) {
        FFIcon(category.icon.ifBlank { categoryIcon(category.id) }, color, (size * .5f).dp)
    }
}

@Composable
private fun RootCard(root: Category, children: List<Category>, open: Boolean, busy: Boolean, reorder: Boolean, quickColor: Boolean,
    toggle: () -> Unit, edit: (Category?) -> Unit, addSub: ((Category) -> Unit)?, pickColor: (Category) -> Unit,
    move: (Int) -> Unit, moveChild: (Category, Int) -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(FFColors.surface).animateContentSize()) {
        Row(Modifier.fillMaxWidth().testTag("category-${root.id}")
            .clickable(enabled = !busy && !reorder, role = Role.Button) { if (quickColor) pickColor(root) else edit(root) }
            .padding(start = 16.dp, end = 8.dp, top = 20.dp, bottom = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            CategoryIconTile(root, 40, !busy, if (quickColor) ({ pickColor(root) }) else null)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(root.name, style = FFType.bodyMain.copy(fontSize = 17.sp, lineHeight = 23.sp), color = FFColors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(MR.strings.pb_cat_subcount.text(children.size.toString()), style = FFType.metaData.copy(fontSize = 14.sp, lineHeight = 19.sp), color = FFColors.onSurfaceVariant)
            }
            if (reorder) {
                IconButton({ move(-1) }, Modifier.size(40.dp).testTag("category-${root.id}-up")) { FFIcon("hi_chevron_up", FFColors.onSurface, 20.dp, MR.strings.pb_move_up.text()) }
                IconButton({ move(1) }, Modifier.size(40.dp).testTag("category-${root.id}-down")) { FFIcon("hi_chevron_down", FFColors.onSurface, 20.dp, MR.strings.pb_move_down.text()) }
            } else IconButton(toggle, Modifier.testTag("category-expand-${root.id}")) {
                FFIcon(if (open) "hi_chevron_up" else "hi_chevron_down", FFColors.onSurfaceVariant, 22.dp,
                    if (open) MR.strings.pb_cat_collapse.text() else MR.strings.pb_cat_expand.text())
            }
        }
        if (open || reorder && children.isNotEmpty()) {
            Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(1.dp).background(FFColors.hairline))
            children.forEachIndexed { index, child ->
                Row(Modifier.fillMaxWidth().testTag("category-${child.id}")
                    .clickable(enabled = !busy && !reorder, role = Role.Button) { if (quickColor) pickColor(child) else edit(child) }
                    .padding(start = 28.dp, end = 8.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    CategoryIconTile(child, 32, !busy, if (quickColor) ({ pickColor(child) }) else null)
                    Spacer(Modifier.width(14.dp))
                    Text(child.name, Modifier.weight(1f), style = FFType.bodyMain.copy(fontSize = 15.sp, lineHeight = 21.sp), color = FFColors.onSurface,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (reorder) {
                        IconButton({ moveChild(child, -1) }, Modifier.size(36.dp), enabled = index > 0) { FFIcon("hi_chevron_up", FFColors.onSurfaceVariant, 18.dp, MR.strings.pb_move_up.text()) }
                        IconButton({ moveChild(child, 1) }, Modifier.size(36.dp), enabled = index < children.lastIndex) { FFIcon("hi_chevron_down", FFColors.onSurfaceVariant, 18.dp, MR.strings.pb_move_down.text()) }
                    } else FFIcon("hi_chevron_right", FFColors.outline, 18.dp)
                }
            }
            if (addSub != null && !reorder) Row(Modifier.fillMaxWidth().clickable(enabled = !busy, role = Role.Button) { addSub(root) }
                .testTag("category-add-sub-${root.id}").padding(start = 28.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, FFColors.hairline, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    FFIcon("hi_add", FFColors.onSurfaceVariant, 18.dp)
                }
                Spacer(Modifier.width(14.dp))
                Text(MR.strings.pb_cat_add_sub.text(), style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
            } else Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SystemRow(name: String, icon: String) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(FFColors.surface).padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(FFColors.iconCircle), contentAlignment = Alignment.Center) {
            FFIcon(icon, FFColors.onSurface, 20.dp)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = FFType.bodyMain.copy(fontSize = 17.sp, lineHeight = 23.sp), color = FFColors.onSurface)
            Text(MR.strings.pb_cat_system_managed.text(), style = FFType.metaData, color = FFColors.onSurfaceVariant)
        }
        FFIcon("lock_outline_rounded", FFColors.outline, 18.dp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickColorSheet(category: Category, dismiss: () -> Unit, choose: (Long) -> Unit) {
    ModalBottomSheet(onDismissRequest = dismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = FFColors.canvas) {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(MR.strings.pb_cat_quick_color_title.text(category.name), style = FFType.sectionTitle)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                categoryEditorColors.forEach { (value, tag, labelRes) ->
                    val label = labelRes.text()
                    val isSelected = category.color == value
                    Box(Modifier.size(48.dp).clip(CircleShape).background(Color(value))
                        .border(if (isSelected) 2.dp else 0.dp, if (isSelected) FFColors.onSurface else Color.Transparent, CircleShape)
                        .semantics { contentDescription = label; selected = isSelected }
                        .clickable(role = Role.RadioButton) { choose(value) }.testTag("category-quick-color-$tag"), contentAlignment = Alignment.Center) {
                        if (isSelected) FFIcon("check_rounded", Color.White, 22.dp)
                    }
                }
            }
        }
    }
}
