package com.logikawan.finflow.feature.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logikawan.finflow.core.model.*
import com.logikawan.finflow.core.ui.*
import com.logikawan.finflow.core.util.*
import kotlinx.datetime.*

@Composable
fun TransactionDetail(tx: Transaction, data: FinanceData, edit: () -> Unit, delete: () -> Unit, busy: Boolean) {
    val expense = tx.type == "expense"
    val color = if (expense) Color(0xFFEF555B) else if (tx.type == "income") FFColors.income else FFColors.secondary
    FinFlowTheme(usePoppins = true) {
        Column(Modifier.fillMaxSize().background(FFColors.background)) {
            Column(Modifier.weight(1f).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(32.dp))
                Box(Modifier.size(64.dp).clip(CircleShape).background(color.copy(alpha = .2f)), contentAlignment = Alignment.Center) {
                    if (expense) FigmaAsset("fig_149_116_container3")
                    else FFIcon(if (tx.type == "income") "arrow_downward_rounded" else "swap_horiz_rounded", color, 32.dp)
                }
                Spacer(Modifier.height(16.dp))
                Text((if (expense) "-" else if (tx.type == "income") "+" else "") + displayPreciseMoney(tx.amountMinor, tx.currency), style = FFType.amountLg.copy(fontSize = 28.sp), color = color)
                Spacer(Modifier.height(4.dp)); Text(tx.currency, style = FFType.badgeLabel, color = FFColors.onSurfaceVariant)
                Spacer(Modifier.height(4.dp)); Text(when(tx.type) { "expense" -> "Pengeluaran"; "income" -> "Pemasukan"; else -> "Transfer" }, style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
                Spacer(Modifier.height(32.dp))
                val time = runCatching { Instant.fromEpochMilliseconds(tx.createdAt).toLocalDateTime(TimeZone.currentSystemDefault()).time.toString().take(5) }.getOrDefault("—")
                Surface(color = FFColors.surface, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        val rows = listOf("Judul" to tx.title, "Kategori" to data.categoryName(tx.categoryId), "Dompet" to data.accountName(tx.accountId), "Tanggal" to tx.date, "Jam dibuat" to time, "Catatan" to tx.description.ifBlank { "—" })
                        rows.forEachIndexed { index, (label, value) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(label, style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
                                Spacer(Modifier.width(16.dp))
                                Text(value, Modifier.weight(1f), style = FFType.bodyMain, color = FFColors.onSurface, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                            }
                            if (index < rows.lastIndex) HorizontalDivider(color = FFColors.outlineVariant, thickness = .5.dp)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().background(FFColors.surface).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = delete, enabled = !busy, shape = RoundedCornerShape(20.dp), modifier = Modifier.width(72.dp).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFDAD6))) { FigmaAsset("fig_149_116_container", description = "Hapus transaksi") }
                Button(onClick = edit, enabled = !busy, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(20.dp)) {
                    FFIcon("edit_outlined", MaterialTheme.colorScheme.onPrimary); Spacer(Modifier.width(8.dp)); Text(if(tx.source in com.logikawan.finflow.core.model.managedTransactionSources) "Kelola" else "Ubah", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                }
            }
        }
    }
}

