package com.logikawan.finflow.feature.settings

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
import com.logikawan.finflow.core.model.*
import com.logikawan.finflow.core.ui.*
import com.logikawan.finflow.core.util.newId

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
    EditorDialog(if (old == null) "Tambah Kategori" else "Edit Kategori", busy, dismiss, save = {
        if (!busy) try {
            require(name.isNotBlank() && name.trim().length <= 100) { "Nama kategori wajib diisi, maksimal 100 karakter" }
            require(type in listOf("expense", "income")) { "Pilih jenis kategori" }
            val parent = parentId.takeIf { it.isNotEmpty() }?.let { selected -> data.categories.find { it.id == selected } }
            require(parentId.isEmpty() || parent != null && parent.id != id && parent.parentId == null && parent.type == type) { "Pilih kategori induk dengan jenis yang sama" }
            require(parentId.isEmpty() || data.categories.none { it.parentId == id }) { "Kategori yang memiliki subkategori harus tetap menjadi kategori utama" }
            require(old == null || type == old.type && parentId == old.parentId.orEmpty()) { "Jenis dan induk kategori lama dipertahankan agar riwayat tetap sesuai" }
            val value = (old ?: Category(id, name.trim(), type)).copy(name = name.trim(), type = type,
                parentId = parentId.takeIf { it.isNotEmpty() }, icon = icon, color = color)
            error = null
            save(value)
        } catch (e: Exception) { error = e.message ?: "Kategori belum dapat disimpan" }
    }, error = error) {
        DesignCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(Color(color).copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                    FFIcon(icon, Color(color), 28.dp)
                }
                Column {
                    Text(name.ifBlank { "Pratinjau kategori" }, style = FFType.sectionTitle)
                    Text(if (type == "expense") "Pengeluaran" else "Pemasukan", style = FFType.metaData, color = FFColors.outline)
                }
            }
        }
        Field("Nama kategori", name, { name = it }, enabled = !busy)
        if (old == null) {
            Choice("Jenis", type, listOf("expense" to "Pengeluaran", "income" to "Pemasukan"), { if (!busy) { type = it; parentId = "" } })
            Choice("Kategori induk", parentId, listOf("" to "Kategori utama") + data.categories.filter {
                it.type == type && it.parentId == null && it.id != id
            }.map { it.id to it.name }, { if (!busy) parentId = it })
        } else {
            Text("${if (old.type == "expense") "Pengeluaran" else "Pemasukan"} · ${old.parentId?.let { data.categoryName(it) } ?: "Kategori utama"}",
                style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
            Text("Jenis dan induk kategori lama dipertahankan agar riwayat transaksi tetap sesuai.", style = FFType.metaData, color = FFColors.outline)
        }
        Text("Ikon", style = FFType.bodyMain)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            categoryEditorIcons.forEach { (value, label) ->
                Box(Modifier.size(48.dp).clip(CircleShape).background(FFColors.surface)
                    .border(if (icon == value) 2.dp else 0.dp, if (icon == value) Color(color) else Color.Transparent, CircleShape)
                    .testTag("category-icon-$value").semantics { contentDescription = label; selected = icon == value }
                    .clickable(enabled = !busy, role = Role.RadioButton) { icon = value }, contentAlignment = Alignment.Center) {
                    FFIcon(value, if (icon == value) Color(color) else FFColors.onSurfaceVariant)
                }
            }
        }
        Text("Warna", style = FFType.bodyMain)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            categoryEditorColors.forEach { (value, label) ->
                Box(Modifier.size(44.dp).clip(CircleShape).background(Color(value))
                    .testTag("category-color-$label").semantics { contentDescription = label; selected = color == value }
                    .clickable(enabled = !busy, role = Role.RadioButton) { color = value }, contentAlignment = Alignment.Center) {
                    if (color == value) FFIcon("check_rounded", Color.White, 22.dp)
                }
            }
        }
    }
}

private val categoryEditorIcons = listOf(
    "restaurant" to "Makanan", "directions_bus" to "Transportasi", "local_mall" to "Belanja",
    "home" to "Rumah", "local_hospital" to "Kesehatan", "school" to "Pendidikan",
    "movie" to "Hiburan", "receipt_long" to "Tagihan", "attach_money" to "Gaji",
    "store" to "Usaha", "sports_esports" to "Permainan", "category" to "Lainnya",
)
private val categoryEditorColors = listOf(
    0xFFFF6B00L to "Oranye", 0xFFEF4444L to "Merah", 0xFFEC4899L to "Merah muda",
    0xFF8B5CF6L to "Ungu", 0xFF3B82F6L to "Biru", 0xFF06B6D4L to "Sian",
    0xFF10B981L to "Hijau", 0xFFEAB308L to "Kuning", 0xFF64748BL to "Abu-abu",
)
