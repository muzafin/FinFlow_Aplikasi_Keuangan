package com.logikawan.finflow.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.logikawan.finflow.core.model.*
import com.logikawan.finflow.core.platform.NativeServices
import com.logikawan.finflow.core.ui.*
import com.logikawan.finflow.core.util.*
import com.logikawan.finflow.feature.importdata.CsvImport
import com.logikawan.finflow.feature.level.LevelScreen

val accountDataPages=setOf("Manajemen Akun","Cadangan & Pemulihan","Perangkat Terhubung","Ekspor Data","Aktivitas","Bantuan","Masukan","Laporkan Bug","Tentang","Catatan Pembaruan")

@Composable
fun AccountDataScreen(page:String,data:FinanceData,native:NativeServices,email:String,navigate:(String)->Unit,
    reload:()->Unit,backup:()->Unit,chooseRestore:()->Unit,busy:Boolean=false,changePhoto:()->Unit={},deleteAccount:()->Unit={},localMode:Boolean=false,connect:()->Unit={},useLocal:()->Unit={},back:()->Unit={},accountExtras:androidx.compose.foundation.lazy.LazyListScope.()->Unit={}) {
    if(page=="Aktivitas"){ LevelScreen(data,native,back){ navigate(it) }; return }
    var message by remember{mutableStateOf<String?>(null)}
    fun export(block:()->Unit){try{block()}catch(e:Exception){message=e.message ?: "Ekspor belum dapat dibuat"}}
    LazyColumn(Modifier.fillMaxSize().testTag("account-data-page"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        when(page){
            "Manajemen Akun"->{
                item{DesignCard{ProfilePhoto(data.profile,native,Modifier.size(72.dp));Spacer(Modifier.height(12.dp));Text(data.profile?.name.orEmpty(),style=FFType.sectionTitle);Text(if(localMode)"Mode Lokal" else email,style=FFType.bodyMain);Spacer(Modifier.height(12.dp));Text(if(localMode)"Data hanya disimpan di perangkat ini. Hubungkan akun untuk memilih sinkronisasi cloud." else "Data tersimpan pada akun ini dan dapat dibuka melalui akun yang sama di perangkat lain.",style=FFType.metaData,color=FFColors.onSurfaceVariant)}}
                accountExtras()
                item{Button(onClick=if(localMode)connect else useLocal,enabled=!busy,modifier=Modifier.fillMaxWidth()){Text(if(localMode)"Hubungkan Akun / Sinkronisasi" else "Buka Mode Lokal")}}
                item{OutlinedButton(onClick=changePhoto,enabled=!busy,modifier=Modifier.fillMaxWidth()){Text("Ubah Foto Profil")}}
                item{Button(onClick={navigate("Profil akun")},modifier=Modifier.fillMaxWidth()){Text("Edit Profil")}}
                if(!localMode)item{OutlinedButton(onClick={navigate("Ubah password")},modifier=Modifier.fillMaxWidth()){Text("Kirim Tautan Pemulihan Password")}}
                item{OutlinedButton(onClick=reload,enabled=!busy,modifier=Modifier.fillMaxWidth()){Text("Muat Ulang Data Akun")}}
                if(!localMode){item{Text("Untuk akun Google, gunakan tombol Google saat masuk. Password Google tetap dikelola oleh Google.",style=FFType.metaData,color=FFColors.onSurfaceVariant)}
                item{OutlinedButton(onClick=deleteAccount,enabled=!busy,modifier=Modifier.fillMaxWidth()){Text("Hapus Akun dan Data",color=FFColors.expense)}}}
            }
            "Cadangan & Pemulihan"->{
                item{DesignCard{FFIcon("backup_outlined",size=42.dp);Spacer(Modifier.height(12.dp));Text("Cadangan Lengkap",style=FFType.sectionTitle);Text("Dompet dan saldo awal, jurnal, anggaran, target, investasi, tagihan, utang, kartu, bundle, reimburse, jadwal, serta preferensi tampilan disimpan dalam berkas JSON.",style=FFType.bodyMain);Spacer(Modifier.height(12.dp));Text("Berkas berisi data keuangan pribadi. Simpan di tempat milik Anda.",style=FFType.metaData,color=FFColors.onSurfaceVariant)}}
                item{Button(onClick={export(backup)},enabled=data.featuresReady && !busy,modifier=Modifier.fillMaxWidth()){Text("Simpan Cadangan JSON")}}
                item{OutlinedButton(onClick=chooseRestore,enabled=data.featuresReady && !busy,modifier=Modifier.fillMaxWidth()){Text("Pulihkan dari JSON")}}
                item{Text("Pemulihan lengkap hanya untuk akun tujuan kosong agar tidak menimpa data. Proses yang terputus bisa dilanjutkan dengan berkas yang sama pada perangkat ini. Untuk akun yang sama pada HP lain, cukup masuk untuk memuat data cloud.",style=FFType.metaData,color=FFColors.onSurfaceVariant)}
                item{OutlinedButton(onClick={navigate("Impor data")},modifier=Modifier.fillMaxWidth()){Text("Impor Jurnal CSV / Ivy")}}
            }
            "Perangkat Terhubung"->{
                item{DesignCard{FFIcon("devices",size=42.dp);Spacer(Modifier.height(12.dp));Text(native.deviceDescription(),style=FFType.sectionTitle);Text("Perangkat ini · $email",style=FFType.bodyMain);Spacer(Modifier.height(12.dp));Text("Sesi aktif pada perangkat ini. Keluar dari akun melalui halaman Profil.",style=FFType.metaData,color=FFColors.onSurfaceVariant)}}
                item{Text("Daftar dan pencabutan sesi perangkat lain memerlukan layanan pengelolaan sesi akun. Halaman ini hanya menampilkan perangkat yang sedang digunakan.",style=FFType.metaData,color=FFColors.onSurfaceVariant)}
            }
            "Ekspor Data"->item{ExportDataForm(data,native,::export)}
            "Aktivitas"->{
                item{RecordingProgressSummary(data,native)}
            }
            "Bantuan"->{
                listOf("Mencatat transaksi" to "Tombol + membuka panel pencatatan di bagian bawah. Pilih jenis, kategori, dompet dan nominal. Tag dan subkategori dapat ditambahkan. Transaksi terjadwal belum dihitung ke saldo.",
                    "Saldo dan transfer" to "Saldo = saldo awal + jurnal aktual. Transfer antar dompet memindahkan dana tanpa menjadi pemasukan atau pengeluaran. Mata uang yang berbeda dihitung terpisah.",
                    "Target tabungan" to "Setor dan Tarik memindahkan dana antara dompet biasa dan dompet target. Progres manual dari FinFlow lama tetap diberi label terpisah agar tidak menciptakan uang baru.",
                    "Bundle belanja" to "Tambahkan item pada Catatan, centang yang dibeli, lalu Bayar. Pembayaran dapat berupa satu transaksi atau per item. Batalkan dari Riwayat untuk mengembalikan jurnal.",
                    "Memperbaiki pembayaran" to "Pembayaran tagihan, utang, reimburse, bundle dan target dikelola melalui menu asalnya. Batalkan pembayaran sebelum memperbaiki nominal agar riwayat dan saldo tetap cocok.",
                    "Koneksi bank" to "Pencatatan manual, CSV/Ivy dan impor notifikasi Android tersedia. Sinkronisasi bank langsung memerlukan integrasi penyedia yang sesuai.",
                    "Tentang FinFlow" to "FinFlow · Android / iOS. Catat keuangan, atur dompet, rencanakan tabungan, dan lihat statistik dalam satu aplikasi.").forEach{(title,body)->item{DesignCard{Text(title,style=FFType.sectionTitle);Spacer(Modifier.height(10.dp));Text(body,style=FFType.bodyMain,color=FFColors.onSurfaceVariant)}}}
            }
            "Masukan"->item{SupportForm(page,native)}
            "Laporkan Bug"->item{BugReportForm(native)}
            "Tentang"->item{DesignCard{FinFlowLogo(88.dp);Text("FinFlow",style=FFType.appTitle);Text("Versi ${native.appVersion()}",style=FFType.metaData,color=FFColors.onSurfaceVariant);Spacer(Modifier.height(12.dp));Text("Catat, pahami, dan kelola keuanganmu. Gunakan penyimpanan lokal tanpa akun atau hubungkan akun untuk sinkronisasi cloud.",style=FFType.bodyMain)}}
            "Catatan Pembaruan"->item{DesignCard{Text("Versi ${native.appVersion()}",style=FFType.sectionTitle);Text("2 November 2026",style=FFType.metaData,color=FFColors.onSurfaceVariant);Spacer(Modifier.height(12.dp));Text("• Mode lokal tanpa login\n• Pilihan sinkronisasi akun\n• Pemulihan cadangan tanpa login\n• Tampilan awal mengikuti Ollo dengan logo FinFlow\n• Perbaikan keterbacaan tema gelap",style=FFType.bodyMain)}}
        }
    }
    message?.let{AlertDialog(onDismissRequest={message=null},title={Text("Ekspor belum tersedia")},text={Text(it)},confirmButton={TextButton(onClick={message=null}){Text("Tutup")}})}
}

@Composable private fun SupportForm(page:String,native:NativeServices) {
    var body by remember{mutableStateOf("")}
    var message by remember{mutableStateOf<String?>(null)}
    Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text(if(page=="Masukan")"Apa yang bisa kami tingkatkan?" else "Ceritakan masalah yang terjadi",style=FFType.sectionTitle)
        Field(if(page=="Masukan")"Saran" else "Langkah dan masalah",body,{body=it.take(4000)},singleLine=false)
        Text("Tombol berikut membuka draf pada aplikasi email. Kamu dapat memeriksa dan mengirimkannya sendiri.",style=FFType.metaData,color=FFColors.onSurfaceVariant)
        Button(enabled=body.isNotBlank(),onClick={message=native.composeSupport("FinFlow · $page",body+"\n\nFinFlow "+native.appVersion()+"\n"+native.deviceDescription())},modifier=Modifier.fillMaxWidth()){Text("Buka Draf Email")}
        message?.let{Text(it,color=MaterialTheme.colorScheme.error)}
    }
}

@Composable private fun ExportDataForm(data:FinanceData,native:NativeServices,attempt:(()->Unit)->Unit) {
    var from by remember{mutableStateOf(today().toString().take(7)+"-01")}
    var to by remember{mutableStateOf(today().toString())}
    var type by remember{mutableStateOf("all")};var wallet by remember{mutableStateOf("")};var currency by remember{mutableStateOf("")}
    var error by remember{mutableStateOf<String?>(null)}
    val txs=data.transactions.filter{it.date>=from && it.date<=to && (type=="all" || it.type==type) && (wallet.isBlank() || it.accountId==wallet || it.toAccountId==wallet) && (currency.isBlank() || it.currency==currency)}.sortedWith(compareBy<Transaction>{it.date}.thenBy{it.createdAt})
    fun valid():Boolean=try{kotlinx.datetime.LocalDate.parse(from);kotlinx.datetime.LocalDate.parse(to);require(from<=to){"Tanggal akhir harus setelah tanggal awal"};error=null;true}catch(e:Exception){error=e.message;false}
    Column(verticalArrangement=Arrangement.spacedBy(16.dp)){
        DesignCard{Text("Rentang Ekspor",style=FFType.sectionTitle);Spacer(Modifier.height(12.dp));Field("Dari YYYY-MM-DD",from,{from=it});Field("Sampai YYYY-MM-DD",to,{to=it});Choice("Jenis",type,listOf("all" to "Semua","income" to "Pemasukan","expense" to "Pengeluaran","transfer" to "Transfer"),{type=it});Choice("Dompet",wallet,listOf("" to "Semua dompet")+data.accounts.map{it.id to it.name},{wallet=it});Choice("Mata uang",currency,listOf("" to "Semua mata uang")+data.accounts.map{it.currency}.distinct().map{it to it},{currency=it});Text("${txs.size} transaksi",style=FFType.metaData)}
        error?.let{Text(it,color=FFColors.expense)}
        Button(onClick={if(valid())attempt{native.shareCsv("finflow-$from-$to.csv",CsvImport.export(data,txs))}},modifier=Modifier.fillMaxWidth()){Text("Ekspor CSV")}
        OutlinedButton(onClick={if(valid())attempt{
            val lines=listOf("${data.profile?.name.orEmpty()} · $from s.d. $to","${txs.size} transaksi","")+txs.flatMap{t->listOf("${t.date} | ${t.title}","${when(t.type){"income"->"Pemasukan";"expense"->"Pengeluaran";else->"Transfer"}} | ${displayPreciseMoney(t.amountMinor,t.currency)} | ${data.accountName(t.accountId)}${t.toAccountId?.let{" → "+data.accountName(it)}.orEmpty()}","${data.categoryName(t.categoryId)}${if(t.scheduled)" · terjadwal" else ""}${if(t.description.isNotBlank())" | "+t.description else ""}","")}
            native.sharePdf("finflow-$from-$to.pdf","Laporan Keuangan FinFlow",lines)
        }},modifier=Modifier.fillMaxWidth()){Text("Ekspor PDF")}
        Text("CSV memuat jurnal yang dipilih, termasuk penanda jadwal. Gunakan Cadangan JSON untuk menyimpan seluruh data aplikasi.",style=FFType.metaData,color=FFColors.onSurfaceVariant)
    }
}
