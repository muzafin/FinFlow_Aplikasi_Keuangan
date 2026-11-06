package com.logikawan.finflow.feature.investment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.logikawan.finflow.core.model.Investment
import com.logikawan.finflow.core.ui.FFColors
import com.logikawan.finflow.core.ui.FFIcon
import com.logikawan.finflow.core.ui.FFType
import com.logikawan.finflow.core.ui.FinanceCard
import com.logikawan.finflow.core.util.checkedAdd
import com.logikawan.finflow.core.util.decimalAmount
import com.logikawan.finflow.core.util.displayMoney
import com.logikawan.finflow.core.util.minorAmount
import kotlin.math.round

@Composable
fun InvestmentDetail(investment: Investment, onBack: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit,
                     onUpdateValue: ((Long) -> Unit)? = null, busy: Boolean = false) {
    var current by remember(investment.id, investment.currentMinor, investment.currency) {
        mutableStateOf(decimalAmount(investment.currentMinor, investment.currency))
    }
    var error by remember { mutableStateOf<String?>(null) }
    val profit = checkedAdd(checkedAdd(investment.currentMinor, investment.dividendsMinor), -investment.investedMinor)
    val positive = profit >= 0
    val percentage = if (investment.investedMinor > 0) round(profit.toDouble() / investment.investedMinor.toDouble() * 1000) / 10 else 0.0
    Column(Modifier.fillMaxSize().background(FFColors.background).safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(enabled = !busy, onClick = onBack) { FFIcon("arrow_back_rounded", contentDescription = "Kembali") }
            Text(investment.name, Modifier.weight(1f), style = FFType.sectionTitle, textAlign = TextAlign.Center)
            IconButton(enabled = !busy, onClick = onDelete) { FFIcon("delete_outline_rounded", FFColors.expense, contentDescription = "Hapus investasi") }
        }
        Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brush.horizontalGradient(
                if (positive) listOf(FFColors.income, Color(0xFF02855B)) else listOf(FFColors.expense, Color(0xFFD32F2F))))
                .padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Nilai Saat Ini", style = FFType.metaData, color = Color.White.copy(alpha = .7f))
                Spacer(Modifier.height(8.dp))
                Text(displayMoney(investment.currentMinor, investment.currency), style = FFType.amountLg, color = Color.White)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("Modal Awal", style = FFType.metaData, color = Color.White.copy(alpha = .7f))
                        Text(displayMoney(investment.investedMinor, investment.currency), style = FFType.bodyMain.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("Return", style = FFType.metaData, color = Color.White.copy(alpha = .7f))
                        Text("${if (positive) "+" else ""}$percentage%", style = FFType.bodyMain.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Update Nilai Pasar", style = FFType.sectionTitle)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(current, { current = it; error = null }, label = { Text("Masukkan nilai investasi terkini (${investment.currency})") },
                leadingIcon = { FFIcon("show_chart_rounded") }, modifier = Modifier.fillMaxWidth(), enabled = !busy && onUpdateValue != null,
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            error?.let { Text(it, Modifier.padding(top = 8.dp), style = FFType.metaData, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                if (onUpdateValue == null) onEdit() else try {
                    val value = minorAmount(current, investment.currency)
                    require(value in 0..9_000_000_000_000_000L) { "Nilai investasi tidak valid" }
                    onUpdateValue(value)
                } catch (e: Exception) { error = e.message ?: "Nilai investasi tidak valid" }
            }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                if (busy) CircularProgressIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                else Text("Perbarui Nilai", style = FFType.bodyMain.copy(fontWeight = FontWeight.SemiBold))
            }
            Spacer(Modifier.height(12.dp))
            Text("Nominal memakai titik desimal tanpa pemisah ribuan. Penilaian dicatat manual.", style = FFType.metaData, color = FFColors.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            FinanceCard {
                Text("Rincian Investasi", style = FFType.sectionTitle)
                InvestmentInfo("Jenis", when (investment.type) { "saham" -> "Saham"; "reksaDana" -> "Reksa Dana"; "crypto" -> "Crypto"; "emas" -> "Emas"; "deposito" -> "Deposito"; "properti" -> "Properti"; "p2pLending" -> "P2P Lending"; else -> investment.type })
                if (investment.platform.isNotBlank()) InvestmentInfo("Platform / Broker", investment.platform)
                InvestmentInfo("Dividen", displayMoney(investment.dividendsMinor, investment.currency))
                InvestmentInfo("Laba / Rugi", displayMoney(profit, investment.currency))
                if (investment.notes.isNotBlank()) Text(investment.notes, style = FFType.bodyMain)
                TextButton(enabled = !busy, onClick = onEdit) { Text("Edit Investasi") }
            }
        }
    }
}

@Composable
private fun InvestmentInfo(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.weight(1f), style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
        Text(value, Modifier.weight(1f), style = FFType.bodyMain.copy(fontWeight = FontWeight.Medium), textAlign = TextAlign.End)
    }
}
