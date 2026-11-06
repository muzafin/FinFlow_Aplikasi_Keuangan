package com.logikawan.finflow.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.logikawan.finflow.core.model.*
import com.logikawan.finflow.core.ui.*

@Composable
fun CategoryScreen(data: FinanceData, busy: Boolean = false, edit: (Category?) -> Unit) {
    var type by rememberSaveable { mutableStateOf("expense") }
    var query by rememberSaveable { mutableStateOf("") }
    val categories = data.categories.filter { it.type == type }
    val roots = categories.filter { it.parentId == null }
    val visibleRoots = roots.filter { root -> query.isBlank() || root.name.contains(query, true) || categories.any {
        it.parentId == root.id && it.name.contains(query, true)
    } }
    Column(Modifier.fillMaxSize().testTag("categories-page")) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("categories-list"), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                SegmentedTabs(listOf("expense" to "Pengeluaran", "income" to "Pemasukan"), type, { type = it })
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(query, { query = it }, placeholder = { Text("Cari kategori") }, singleLine = true,
                    leadingIcon = { FFIcon("search_rounded") }, modifier = Modifier.fillMaxWidth().testTag("category-search"))
            }
            if (visibleRoots.isEmpty()) item {
                DesignCard { Text(if (query.isBlank()) "Belum ada kategori. Tambahkan kategori utama atau subkategori." else "Kategori tidak ditemukan.",
                    style = FFType.bodyMain, color = FFColors.outline) }
            }
            items(visibleRoots, key = { it.id }) { root ->
                val children = categories.filter { it.parentId == root.id }
                DesignCard {
                    CategoryRow(root, busy, false, { edit(root) })
                    children.filter { query.isBlank() || root.name.contains(query, true) || it.name.contains(query, true) }.forEach { child ->
                        Spacer(Modifier.height(8.dp))
                        CategoryRow(child, busy, true, { edit(child) })
                    }
                }
            }
            item { Text("Tambah subkategori melalui pilihan Kategori induk. Mengubah nama atau ikon tidak menghapus riwayat transaksi.",
                style = FFType.metaData, color = FFColors.outline) }
        }
        Button(onClick = { edit(null) }, enabled = !busy, modifier = Modifier.fillMaxWidth().padding(20.dp).height(50.dp)) {
            Text("+  Tambah Kategori")
        }
    }
}

@Composable
private fun CategoryRow(category: Category, busy: Boolean, child: Boolean, edit: () -> Unit) {
    val color = category.color?.let { Color(it) } ?: categoryColor(category.id)
    Row(Modifier.fillMaxWidth().testTag("category-${category.id}").clip(CircleShape)
        .clickable(enabled = !busy, role = Role.Button, onClick = edit).padding(start = if (child) 24.dp else 0.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(if (child) 36.dp else 44.dp).clip(CircleShape).background(color.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
            FFIcon(category.icon.ifBlank { categoryIcon(category.id) }, color, if (child) 18.dp else 22.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(category.name, style = FFType.bodyMain, fontWeight = if (child) FontWeight.Normal else FontWeight.SemiBold)
            if (child) Text("Subkategori", style = FFType.metaData, color = FFColors.outline)
        }
        FFIcon("chevron_right_rounded", size = 20.dp)
    }
}
