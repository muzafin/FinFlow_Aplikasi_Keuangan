package com.logikawan.finflow.feature.investment

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.logikawan.finflow.core.model.FinanceData
import com.logikawan.finflow.core.model.Investment
import com.logikawan.finflow.core.ui.FFColors
import com.logikawan.finflow.core.ui.FFIcon
import com.logikawan.finflow.core.ui.FFType
import com.logikawan.finflow.core.ui.FeatureEmpty
import com.logikawan.finflow.core.util.checkedAdd
import com.logikawan.finflow.core.util.displayMoney

@Composable
fun InvestmentScreen(data: FinanceData, edit: (Investment) -> Unit, add: () -> Unit) {
    if (data.investments.isEmpty()) {
        FeatureEmpty("trending_up_rounded", FFColors.income, "Belum ada investasi", "Pantau portofolio investasimu\ndi satu tempat", "Tambah Investasi", add)
        return
    }
    val palette = listOf(FFColors.income, FFColors.secondary, Color(0xFFF5A623), Color(0xFF9B59B6), Color(0xFF5B8DEF), FFColors.tertiary)
    LazyColumn(contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 100.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        data.investments.groupBy { it.currency }.forEach { (currency, investments) ->
            val value = investments.fold(0L) { total, inv -> checkedAdd(total, inv.currentMinor) }
            val initial = investments.fold(0L) { total, inv -> checkedAdd(total, inv.investedMinor) }
            val dividends = investments.fold(0L) { total, inv -> checkedAdd(total, inv.dividendsMinor) }
            val profit = checkedAdd(checkedAdd(value, dividends), -initial)
            val positive = profit >= 0
            item("summary-$currency") {
                Column(Modifier.fillMaxWidth().background(Brush.linearGradient(if (positive) listOf(FFColors.income.copy(alpha = .85f), FFColors.primary.copy(alpha = .85f)) else listOf(FFColors.expense.copy(alpha = .85f), Color(0xD9D32F2F))), RoundedCornerShape(20.dp)).padding(20.dp)) {
                    Text("Total Portofolio", style = FFType.bodyMain, color = Color.White.copy(alpha = .7f))
                    Spacer(Modifier.height(4.dp))
                    Text(displayMoney(value, currency), style = FFType.amountLg, color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FFIcon(if (positive) "arrow_upward_rounded" else "arrow_downward_rounded", Color.White.copy(alpha = .7f), 16.dp)
                        Spacer(Modifier.width(4.dp))
                        Text((if (positive) "+" else "") + displayMoney(profit, currency), style = FFType.bodyMain, color = Color.White.copy(alpha = .7f))
                    }
                }
            }
            item("allocation-$currency") {
                Text("Alokasi Aset", style = FFType.sectionTitle)
                Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    val trackColor = FFColors.outlineVariant
                    Canvas(Modifier.size(150.dp).padding(20.dp)) {
                        if (value <= 0) drawArc(trackColor, -90f, 360f, false, style = Stroke(40.dp.toPx()))
                        else {
                            var start = -90f
                            investments.forEachIndexed { index, inv ->
                                val sweep = (inv.currentMinor.toDouble() / value * 360).toFloat()
                                drawArc(palette[index % palette.size], start + 1f, (sweep - 2).coerceAtLeast(0f), false, style = Stroke(40.dp.toPx()))
                                start += sweep
                            }
                        }
                    }
                }
                Text("Daftar Investasi", style = FFType.sectionTitle)
            }
            items(investments, key = { it.id }) { inv ->
                val gain = checkedAdd(checkedAdd(inv.currentMinor, inv.dividendsMinor), -inv.investedMinor)
                val color = if (gain >= 0) FFColors.income else FFColors.expense
                Surface(onClick = { edit(inv) }, shape = RoundedCornerShape(16.dp), color = FFColors.surface, border = BorderStroke(.5.dp, FFColors.outlineVariant)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).background(color.copy(alpha = .12f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { FFIcon("trending_up_rounded", color, 22.dp) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(inv.name, style = FFType.bodyMain, fontWeight = FontWeight.SemiBold)
                            Text(inv.type, style = FFType.metaData, color = FFColors.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(displayMoney(inv.currentMinor, currency), style = FFType.amountSm)
                            Text((if (gain >= 0) "+" else "") + displayMoney(gain, currency), style = FFType.metaData, color = color)
                        }
                    }
                }
            }
        }
    }
}
