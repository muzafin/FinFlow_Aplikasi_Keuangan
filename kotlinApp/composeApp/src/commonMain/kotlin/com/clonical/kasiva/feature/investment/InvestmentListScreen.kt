package com.clonical.kasiva.feature.investment

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clonical.kasiva.core.model.FinanceData
import com.clonical.kasiva.core.model.Investment
import com.clonical.kasiva.core.ui.FFColors
import com.clonical.kasiva.core.ui.FFIcon
import com.clonical.kasiva.core.ui.FFType
import com.clonical.kasiva.core.ui.FeatureEmpty
import com.clonical.kasiva.core.util.checkedAdd
import com.clonical.kasiva.core.util.displayMoney
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.i18n.*

/** Label tampilan jenis investasi (id `saham`, `reksaDana`, ... tetap pengenal yang disimpan). */
fun investmentTypeLabel(type: String): String = when (type) {
    "saham" -> MR.strings.acc_inv_stocks.text()
    "reksaDana" -> MR.strings.acc_inv_mutual_fund.text()
    "crypto" -> MR.strings.acc_inv_crypto.text()
    "emas" -> MR.strings.acc_inv_gold.text()
    "deposito" -> MR.strings.acc_inv_deposit.text()
    "properti" -> MR.strings.acc_inv_property.text()
    "p2pLending" -> MR.strings.acc_inv_p2p.text()
    else -> type
}

@Composable
fun InvestmentScreen(data: FinanceData, edit: (Investment) -> Unit, add: () -> Unit) {
    if (data.investments.isEmpty()) {
        // Ollo (Asset Management): kartu Total Nilai Aset + kartu ajakan dengan tombol bergaris.
        val currency = data.profile?.baseCurrency ?: "IDR"
        Column(Modifier.fillMaxSize().padding(16.dp).testTag("investment-empty"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(FFColors.surface).padding(20.dp)) {
                Text(MR.strings.acc_total_asset_value.text(), style = FFType.bodyMain.copy(fontSize = 15.sp), color = FFColors.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))
                Text(com.clonical.kasiva.core.util.formatBalance(0L, currency, com.clonical.kasiva.core.model.LocalPreferences.current), style = FFType.amountLg.copy(fontSize = 33.sp, lineHeight = 40.sp), color = FFColors.onSurface)
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(FFColors.surface).padding(horizontal = 24.dp, vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                FFIcon("hi_wallet", FFColors.onSurfaceVariant.copy(alpha = .6f), 48.dp)
                Spacer(Modifier.height(14.dp))
                Text(MR.strings.acc_investment_empty_hint.text(), style = FFType.bodyMain.copy(fontSize = 15.sp), color = FFColors.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(23.dp)).border(1.dp, FFColors.outlineVariant, RoundedCornerShape(23.dp)).clickable(onClick = add).testTag("investment-add"),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    FFIcon("hi_add", FFColors.onSurface, 18.dp); Spacer(Modifier.width(8.dp)); Text(MR.strings.acc_add_investment.text(), style = FFType.bodyMain.copy(fontSize = 15.sp))
                }
            }
        }
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
                    Text(MR.strings.acc_total_portfolio.text(), style = FFType.bodyMain, color = Color.White.copy(alpha = .7f))
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
                Text(MR.strings.acc_asset_allocation.text(), style = FFType.sectionTitle)
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
                Text(MR.strings.acc_investment_list.text(), style = FFType.sectionTitle)
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
                            Text(investmentTypeLabel(inv.type), style = FFType.metaData, color = FFColors.onSurfaceVariant)
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
