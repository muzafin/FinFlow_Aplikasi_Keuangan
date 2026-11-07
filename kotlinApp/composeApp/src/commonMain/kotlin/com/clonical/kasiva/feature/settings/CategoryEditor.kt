package com.clonical.kasiva.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.clonical.kasiva.core.model.*
import com.clonical.kasiva.core.ui.*
import com.clonical.kasiva.core.util.newId
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.i18n.transactionTypeLabel
import com.clonical.kasiva.i18n.*

@Composable
fun CategoryEditor(busy: Boolean, dismiss: () -> Unit, save: (Category) -> Unit,
    old: Category? = null, data: FinanceData = FinanceData(), initialParentId: String? = null) {
    val initialParent = data.categories.find { it.id == initialParentId && it.parentId == null }
    val id = remember { old?.id ?: newId() }
    var name by remember { mutableStateOf(old?.name.orEmpty()) }
    var type by remember { mutableStateOf(old?.type ?: initialParent?.type ?: "expense") }
    var parentId by remember { mutableStateOf(old?.parentId ?: initialParent?.id.orEmpty()) }
    var icon by remember { mutableStateOf(old?.icon?.takeIf { it.isNotBlank() } ?: categoryIcon(id)) }
    val defaultColor = categoryColor(id)
    var color by remember { mutableLongStateOf(old?.color ?: (defaultColor.toArgb().toLong() and 0xFFFFFFFFL)) }
    var error by remember { mutableStateOf<String?>(null) }
    EditorDialog(if (old == null) MR.strings.set_cat_add_title.text() else MR.strings.set_cat_edit_title.text(), busy, dismiss, save = {
        if (!busy) try {
            require(name.isNotBlank() && name.trim().length <= 100) { MR.strings.set_cat_err_name.text() }
            require(type in listOf("expense", "income")) { MR.strings.set_cat_err_type.text() }
            val parent = parentId.takeIf { it.isNotEmpty() }?.let { selected -> data.categories.find { it.id == selected } }
            require(parentId.isEmpty() || parent != null && parent.id != id && parent.parentId == null && parent.type == type) { MR.strings.set_cat_err_parent.text() }
            require(parentId.isEmpty() || data.categories.none { it.parentId == id }) { MR.strings.set_cat_err_has_children.text() }
            require(old == null || type == old.type && parentId == old.parentId.orEmpty()) { MR.strings.set_cat_err_locked.text() }
            val value = (old ?: Category(id, name.trim(), type)).copy(name = name.trim(), type = type,
                parentId = parentId.takeIf { it.isNotEmpty() }, icon = icon, color = color)
            error = null
            save(value)
        } catch (e: Exception) { error = e.message ?: MR.strings.set_cat_err_save.text() }
    }, error = error) {
        DesignCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(Color(color).copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                    FFIcon(icon, Color(color), 28.dp)
                }
                Column {
                    Text(name.ifBlank { MR.strings.set_cat_preview.text() }, style = FFType.sectionTitle)
                    Text(transactionTypeLabel(if (type == "expense") "expense" else "income"), style = FFType.metaData, color = FFColors.outline)
                }
            }
        }
        Field(MR.strings.set_cat_name.text(), name, { name = it }, enabled = !busy)
        if (old == null) {
            Choice(MR.strings.set_type.text(), type, listOf("expense" to transactionTypeLabel("expense"), "income" to transactionTypeLabel("income")), { if (!busy) { type = it; parentId = "" } })
            Choice(MR.strings.set_cat_parent.text(), parentId, listOf("" to MR.strings.set_cat_main.text()) + data.categories.filter {
                it.type == type && it.parentId == null && it.id != id
            }.map { it.id to it.name }, { if (!busy) parentId = it })
        } else {
            Text("${transactionTypeLabel(if (old.type == "expense") "expense" else "income")} · ${old.parentId?.let { data.categoryName(it) } ?: MR.strings.set_cat_main.text()}",
                style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
            Text(MR.strings.set_cat_locked_hint.text(), style = FFType.metaData, color = FFColors.outline)
        }
        Text(MR.strings.set_cat_icon.text(), style = FFType.bodyMain)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            categoryEditorIcons.forEach { (value, labelRes) ->
                val label = labelRes.text()
                Box(Modifier.size(48.dp).clip(CircleShape).background(FFColors.surface)
                    .border(if (icon == value) 2.dp else 0.dp, if (icon == value) Color(color) else Color.Transparent, CircleShape)
                    .testTag("category-icon-$value").semantics { contentDescription = label; selected = icon == value }
                    .clickable(enabled = !busy, role = Role.RadioButton) { icon = value }, contentAlignment = Alignment.Center) {
                    FFIcon(value, if (icon == value) Color(color) else FFColors.onSurfaceVariant)
                }
            }
        }
        Text(MR.strings.set_cat_color.text(), style = FFType.bodyMain)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            categoryEditorColors.forEach { (value, tag, labelRes) ->
                val label = labelRes.text()
                Box(Modifier.size(44.dp).clip(CircleShape).background(Color(value))
                    .testTag("category-color-$tag").semantics { contentDescription = label; selected = color == value }
                    .clickable(enabled = !busy, role = Role.RadioButton) { color = value }, contentAlignment = Alignment.Center) {
                    if (color == value) FFIcon("check_rounded", Color.White, 22.dp)
                }
            }
        }
    }
}

private val categoryEditorIcons = listOf(
    "restaurant" to MR.strings.set_cat_icon_food, "directions_bus" to MR.strings.set_cat_icon_transport, "local_mall" to MR.strings.set_cat_icon_shopping,
    "home" to MR.strings.set_cat_icon_home, "local_hospital" to MR.strings.set_cat_icon_health, "school" to MR.strings.set_cat_icon_education,
    "movie" to MR.strings.set_cat_icon_entertainment, "receipt_long" to MR.strings.set_cat_icon_bills, "attach_money" to MR.strings.set_cat_icon_salary,
    "store" to MR.strings.set_cat_icon_business, "sports_esports" to MR.strings.set_cat_icon_games, "category" to MR.strings.set_cat_icon_other,
)
// Nama Indonesia di tengah dipakai sebagai testTag (stabil); label tampilan diterjemahkan.
internal val categoryEditorColors = listOf(
    Triple(0xFFFF6B00L, "Oranye", MR.strings.set_color_orange), Triple(0xFFEF4444L, "Merah", MR.strings.set_color_red), Triple(0xFFEC4899L, "Merah muda", MR.strings.set_color_pink),
    Triple(0xFF8B5CF6L, "Ungu", MR.strings.set_color_purple), Triple(0xFF3B82F6L, "Biru", MR.strings.set_color_blue), Triple(0xFF06B6D4L, "Sian", MR.strings.set_color_cyan),
    Triple(0xFF10B981L, "Hijau", MR.strings.set_color_green), Triple(0xFFEAB308L, "Kuning", MR.strings.set_color_yellow), Triple(0xFF64748BL, "Abu-abu", MR.strings.set_color_gray),
)
