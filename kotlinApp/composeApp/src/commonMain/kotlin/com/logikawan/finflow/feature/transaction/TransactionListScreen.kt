package com.logikawan.finflow.feature.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logikawan.finflow.core.model.FinanceData
import com.logikawan.finflow.core.model.Transaction
import com.logikawan.finflow.core.ui.Choice
import com.logikawan.finflow.core.ui.FFColors
import com.logikawan.finflow.core.ui.FFIcon
import com.logikawan.finflow.core.ui.FFIconButton
import com.logikawan.finflow.core.ui.FFType
import com.logikawan.finflow.core.ui.Field
import com.logikawan.finflow.core.ui.MonthField
import com.logikawan.finflow.core.util.compactDisplayMoney
import com.logikawan.finflow.core.util.monthLabel
import kotlinx.datetime.LocalDate

@Composable
fun Transactions(data: FinanceData, month: String, monthChange: (String) -> Unit, edit: (Transaction) -> Unit, onBack: () -> Unit = {}, initialType: String = "all") {
    var query by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable(initialType) { mutableStateOf(initialType) }
    var allMonths by rememberSaveable { mutableStateOf(false) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    val filtered = data.transactions.filter { tx ->
        (allMonths || tx.date.startsWith(month)) &&
            (type == "scheduled" && tx.scheduled || type != "scheduled" && !tx.scheduled && (type == "all" || tx.type == type)) &&
            (query.isBlank() || "${tx.title} ${tx.description} ${data.accountName(tx.accountId)} ${data.categoryName(tx.categoryId)}".contains(query, ignoreCase = true))
    }.sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.createdAt })
    val grouped = filtered.groupBy { it.date.take(10) }
    val filteredByType = type != "all" || query.isNotBlank()

    Column(Modifier.fillMaxSize().background(FFColors.background)) {
        Box(Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp, end = 16.dp, bottom = 12.dp).height(48.dp)) {
            FFIconButton("arrow_left_rounded", onBack, Modifier.align(Alignment.CenterStart), tint = FFColors.onSurface)
            Text("Riwayat Transaksi", Modifier.align(Alignment.Center), style = FFType.sectionTitle)
            FFIconButton("filter_list_rounded", { showFilters = true }, Modifier.align(Alignment.CenterEnd), tint = FFColors.onSurface)
        }
        Surface(
            onClick = { showFilters = true },
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(56.dp).shadow(2.dp, RoundedCornerShape(12.dp), ambientColor = Color.Black.copy(alpha = .02f), spotColor = Color.Black.copy(alpha = .02f)),
            shape = RoundedCornerShape(12.dp), color = FFColors.surface,
        ) {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(if (allMonths) "Semua Periode" else monthLabel(month), style = FFType.bodyMain.copy(fontWeight = FontWeight.Medium))
                    if (filteredByType) Text("${filtered.size} transaksi • Filter aktif", style = FFType.metaData, color = FFColors.onSurfaceVariant)
                }
                FFIcon("keyboard_arrow_down_rounded", MaterialTheme.colorScheme.outline)
            }
        }
        Spacer(Modifier.height(24.dp))
        if (filtered.isEmpty()) {
            Column(Modifier.weight(1f).fillMaxWidth().padding(bottom = 100.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(98.dp).background(FFColors.surface, CircleShape).border(1.dp, FFColors.outlineVariant.copy(alpha = .5f), CircleShape), contentAlignment = Alignment.Center) {
                    FFIcon("receipt_long_outlined", FFColors.outlineVariant, 48.dp)
                }
                Spacer(Modifier.height(16.dp))
                Text(if (data.transactions.isEmpty()) "Belum ada transaksi" else "Tidak ada transaksi yang sesuai", style = FFType.bodyMain.copy(fontWeight = FontWeight.SemiBold))
                Spacer(Modifier.height(4.dp))
                Text(if (data.transactions.isEmpty()) "Catat transaksi pertamamu sekarang" else "Ubah periode atau filter transaksi", style = FFType.metaData, color = FFColors.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 110.dp)) {
                grouped.forEach { (date, transactions) ->
                    item("date-$date") {
                        Text(transactionDateLabel(date), Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp), style = FFType.sectionTitle.copy(fontSize = 14.sp), color = FFColors.onSurfaceVariant)
                    }
                    items(transactions, key = { it.id }) { tx ->
                        Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) { TransactionCard(tx, data) { edit(tx) } }
                    }
                    item("space-$date") { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
    if (showFilters) AlertDialog(
        onDismissRequest = { showFilters = false },
        title = { Text("Filter Transaksi", style = FFType.sectionTitle) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Field("Cari transaksi, kategori, atau dompet", query, { query = it })
                MonthField(month, monthChange)
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(allMonths, { allMonths = it }); Text("Semua bulan", style = FFType.bodyMain) }
                Choice("Jenis transaksi", type, listOf("all" to "Semua", "expense" to "Pengeluaran", "income" to "Pemasukan", "transfer" to "Transfer", "scheduled" to "Terjadwal"), { type = it })
            }
        },
        confirmButton = { TextButton(onClick = { showFilters = false }) { Text("Tampilkan") } },
        dismissButton = { TextButton(onClick = { query = ""; type = initialType; allMonths = false }) { Text("Atur ulang") } },
    )
}

@Composable
fun TransactionCard(tx: Transaction, data: FinanceData, click: () -> Unit) {
    val amountColor = when (tx.type) { "income" -> FFColors.income; "expense" -> FFColors.expense; else -> MaterialTheme.colorScheme.secondary }
    val prefix = when (tx.type) { "income" -> "+"; "expense" -> "-"; else -> "" }
    val shape = RoundedCornerShape(16.dp)
    Surface(
        onClick = click, modifier = Modifier.fillMaxWidth().shadow(3.dp, shape, ambientColor = Color.Black.copy(alpha = .03f), spotColor = Color.Black.copy(alpha = .03f)),
        shape = shape, color = FFColors.surface, border = androidx.compose.foundation.BorderStroke(1.dp, FFColors.outlineVariant.copy(alpha = .3f)),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(amountColor.copy(alpha = .1f), RoundedCornerShape(12.dp)).border(1.dp, amountColor.copy(alpha = .2f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                FFIcon(when (tx.type) { "income" -> "arrow_downward_rounded"; "expense" -> "arrow_upward_rounded"; else -> "swap_horiz_rounded" }, amountColor, 22.dp)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(tx.title.ifBlank { data.categoryName(tx.categoryId) }, style = FFType.bodyMain.copy(fontWeight = FontWeight.SemiBold, lineHeight = 17.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(data.accountName(tx.accountId) + if (tx.type == "transfer") " → ${tx.toAccountId?.let(data::accountName) ?: "Dompet tujuan"}" else "", style = FFType.metaData, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (tx.scheduled) Text("Terjadwal • belum memengaruhi saldo", style = FFType.metaData, color = MaterialTheme.colorScheme.outline)
            }
            Spacer(Modifier.width(8.dp))
            Text(prefix + compactDisplayMoney(tx.amountMinor, tx.currency), style = FFType.amountSm, color = amountColor)
        }
    }
}

private fun transactionDateLabel(value: String): String {
    val date = runCatching { LocalDate.parse(value) }.getOrNull() ?: return value
    val days = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
    val months = listOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
    return "${days[date.dayOfWeek.ordinal]}, ${date.dayOfMonth} ${months[date.monthNumber - 1]} ${date.year}"
}
