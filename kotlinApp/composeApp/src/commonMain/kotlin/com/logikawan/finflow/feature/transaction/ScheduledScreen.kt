package com.logikawan.finflow.feature.transaction

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.logikawan.finflow.core.model.*
import com.logikawan.finflow.core.ui.*
import com.logikawan.finflow.core.util.*
import kotlinx.datetime.*

@Composable
fun ScheduledScreen(data: FinanceData, month: String, monthChange: (String) -> Unit, edit: (Transaction) -> Unit, add: () -> Unit) {
    var selectedDate by remember(month) { mutableStateOf<String?>(null) }
    val first = LocalDate.parse("$month-01")
    val days = first.plus(DatePeriod(months = 1)).toEpochDays() - first.toEpochDays()
    val entries = data.transactions.filter { it.scheduled && it.date.startsWith(month) }.sortedBy { it.date }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { DesignCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FFIconButton("chevron_left_rounded", { monthChange(shiftMonth(month, -1)) })
                Text(monthLabel(month), Modifier.weight(1f), style = FFType.sectionTitle, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                FFIconButton("chevron_right_rounded", { monthChange(shiftMonth(month, 1)) })
            }
            Row(Modifier.fillMaxWidth()) { listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min").forEach { Text(it, Modifier.weight(1f), style = FFType.badgeLabel, color = FFColors.outline, textAlign = androidx.compose.ui.text.style.TextAlign.Center) } }
            Spacer(Modifier.height(16.dp))
            val offset = first.dayOfWeek.ordinal
            repeat((offset + days + 6) / 7) { row ->
                Row(Modifier.fillMaxWidth()) {
                    repeat(7) { col ->
                        val day = row * 7 + col - offset + 1
                        val date = "$month-${day.toString().padStart(2, '0')}"
                        val selected = selectedDate == date
                        Column(Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(12.dp)).background(if (selected) FFColors.primary else Color.Transparent)
                            .then(if (day in 1..days) Modifier.clickable { selectedDate = if (selected) null else date } else Modifier),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            if (day in 1..days) {
                                Text(day.toString(), style = FFType.bodyMain, color = if (selected) MaterialTheme.colorScheme.onPrimary else FFColors.onSurface)
                                if (entries.any { it.date == date }) Box(Modifier.padding(top = 4.dp).size(5.dp).background(if (date < today().toString()) FFColors.expense else FFColors.primary, CircleShape))
                            }
                        }
                    }
                }
            }
        } }
        item { DesignCard {
            Text("Ringkasan Bulan Ini", style = FFType.bodyMain, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp)); Text("${entries.size} transaksi terjadwal", style = FFType.metaData, color = FFColors.outline)
            val expenses = entries.filter { it.type == "expense" }
            expenses.map { it.currency }.distinct().forEach { currency ->
                Spacer(Modifier.height(8.dp))
                Text(displayMoney(expenses.filter { it.currency == currency }.fold(0L) { sum, tx -> checkedAdd(sum, tx.amountMinor) }, currency), style = FFType.amountMd)
            }
            if (entries.isEmpty()) { Spacer(Modifier.height(16.dp)); Text("Belum ada jadwal pada bulan ini.", style = FFType.bodyMain, color = FFColors.outline) }
        } }
        entries.filter { selectedDate == null || it.date == selectedDate }.forEach { tx -> item(key = tx.id) {
            DesignCard(onClick = { edit(tx) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FFIcon(categoryIcon(tx.categoryId), categoryColor(tx.categoryId)); Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tx.title, style = FFType.bodyMain, fontWeight = FontWeight.Bold)
                        Text("Jatuh tempo ${tx.date}", style = FFType.metaData, color = if (tx.date < today().toString()) FFColors.expense else FFColors.outline)
                    }
                    Text(displayMoney(tx.amountMinor, tx.currency), style = FFType.bodyMain, fontWeight = FontWeight.Bold)
                }
            }
        } }
        item { OutlinedButton(onClick = add, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("+  Tambah Transaksi Terjadwal") } }
    }
}

