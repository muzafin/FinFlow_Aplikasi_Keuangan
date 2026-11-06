package com.logikawan.finflow.feature.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.logikawan.finflow.core.model.*
import com.logikawan.finflow.core.ui.*
import com.logikawan.finflow.core.util.*

@Composable
fun AccountEditor(old: Account?, data: FinanceData, busy: Boolean, dismiss: () -> Unit, save: (Account) -> Unit) {
    val id = remember { old?.id ?: newId() }
    var name by remember { mutableStateOf(old?.name.orEmpty()) }
    var type by remember { mutableStateOf(old?.type ?: "cash") }
    var currency by remember { mutableStateOf(old?.currency ?: data.profile?.baseCurrency ?: "IDR") }
    var opening by remember { mutableStateOf(old?.let { decimalAmount(it.openingBalanceMinor, it.currency) } ?: "0") }
    var archived by remember { mutableStateOf(old?.archived ?: false) }
    var excluded by remember { mutableStateOf(old?.excludedFromTotal ?: false) }
    var pocket by remember { mutableStateOf(old?.parentId!=null) }
    var parent by remember { mutableStateOf(old?.parentId.orEmpty()) }
    var icon by remember { mutableStateOf(old?.icon ?: "account_balance_wallet") }
    var color by remember { mutableStateOf(old?.color ?: 0xFF2196F3) }
    var error by remember { mutableStateOf<String?>(null) }
    val keys = listOf("cash", "bank", "ewallet", "credit", "investment", "loan")
    val labels = listOf("Tunai", "Bank", "E-Wallet", "Kartu Kredit", "Investasi", "Pinjaman")
    EditorDialog(if (old == null) "Tambah Dompet" else "Edit Dompet", busy, dismiss, save = {
        if (!busy) try {
            val code = currency.trim().uppercase()
            require(name.isNotBlank() && name.trim().length <= 100) { "Nama akun wajib diisi, maksimal 100 karakter" }
            require(code.matches(Regex("[A-Z]{3}"))) { "Gunakan kode mata uang 3 huruf" }
            require(old == null || code == old.currency) { "Mata uang akun tidak boleh diubah" }
            require(type in keys) { "Pilih jenis akun yang valid" }
            require(!pocket || parent.isNotBlank()){ "Pilih dompet induk" }
            require(!pocket || data.accounts.none{it.parentId==id}){"Dompet yang memiliki pocket tidak dapat dijadikan pocket"}
            save(Account(id, name.trim(), type, code, minorAmount(opening, code, allowNegative = true), archived, old?.createdAt ?: nowMillis(),excluded,if(pocket)parent else null,icon,color))
        } catch (e: Exception) { error = e.message ?: "Data akun tidak valid" }
    }, error = error) {
        Text("Detail Dompet", style = FFType.sectionTitle)
        Choice("Tipe", type, keys.zip(labels), { if (!busy) type = it })
        Field("Nama Dompet", name, { if (!busy) name = it })
        if (old == null) Field("Mata Uang Dompet", currency, { if (!busy) currency = it.uppercase().take(3) })
        else Text("Mata Uang Dompet: $currency", style = FFType.bodyMain)
        Field("Saldo Awal", opening, { if (!busy) opening = it })
        DesignCard { Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Kecualikan dari Total",style=FFType.bodyMain);Text("Saldo dan transaksi dompet tidak masuk ringkasan utama/statistik.",style=FFType.metaData,color=FFColors.onSurfaceVariant)};Switch(excluded,{excluded=it},enabled=!busy)} }
        DesignCard { Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Jadikan Pocket",style=FFType.bodyMain);Text("Subdompet dengan saldo sendiri; tidak dihitung dua kali.",style=FFType.metaData,color=FFColors.onSurfaceVariant)};Switch(pocket,{pocket=it},enabled=!busy)} }
        if(pocket)WalletChoice("Dompet Induk",parent,data,data.accounts.filter{it.id!=id && !it.archived && it.parentId==null && it.currency==currency},!busy){parent=it}
        Choice("Ikon dompet",icon,listOf("account_balance_wallet" to "Dompet","account_balance" to "Bank","payments" to "Tunai","credit_card" to "Kartu","savings" to "Tabungan","work_outline" to "Bisnis"),{icon=it})
        Choice("Warna",color.toString(),listOf(0xFF2196F3L to "Biru",0xFF4CAF50L to "Hijau",0xFFF44336L to "Merah",0xFFFF9800L to "Oranye",0xFF9C27B0L to "Ungu").map{it.first.toString() to it.second},{color=it.toLong()})
        if (old != null) DesignCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Arsipkan Dompet", style = FFType.bodyMain)
                    Text("Riwayat tetap tersimpan.", style = FFType.metaData, color = FFColors.outline)
                }
                Switch(archived, { if (!busy) archived = it }, enabled = !busy)
            }
        }
        Text("Ikon", style = FFType.sectionTitle)
        Surface(shape = RoundedCornerShape(16.dp), color = FFColors.surface) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                FigmaAsset(when(type) { "bank" -> "fig_92_6_building"; "ewallet", "loan" -> "fig_92_6_credit_card"; "investment" -> "fig_86_157_trending_up"; else -> "fig_92_6_wallet" })
                Spacer(Modifier.width(12.dp)); Text(labels.getOrElse(keys.indexOf(type)) { "Dompet" }, style = FFType.bodyMain)
            }
        }
        Text("Saldo awal memakai titik untuk desimal, tanpa pemisah ribuan.", style = FFType.metaData, color = FFColors.onSurfaceVariant)
        if (type == "bank" || type == "ewallet") Text("Akun dicatat manual. Sinkronisasi otomatis belum tersedia.", style = FFType.metaData, color = FFColors.onSurfaceVariant)
    }
}

