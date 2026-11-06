package com.logikawan.finflow.feature.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.logikawan.finflow.core.model.*
import com.logikawan.finflow.core.platform.NativeServices
import com.logikawan.finflow.core.ui.*
import com.logikawan.finflow.core.util.today
import kotlinx.datetime.*

private data class ProfileItem(val title:String,val caption:String,val icon:String,val route:String)
private val accountItems=listOf(
    ProfileItem("Manajemen Akun","Profil, login dan sinkronisasi Firebase","person_outline","Manajemen Akun"),
    ProfileItem("Cadangan & Pemulihan","Simpan dan pulihkan seluruh data","backup_outlined","Cadangan & Pemulihan"),
    ProfileItem("Perangkat Terhubung","Sesi pada perangkat ini","devices","Perangkat Terhubung"),
    ProfileItem("Ekspor Data","Ekspor transaksi ke PDF atau CSV","file_download_outlined","Ekspor Data"),
)
private val financeItems=listOf(
    ProfileItem("Impor Data","Masukkan data dari berkas","file_upload_outlined","Impor data"),
    ProfileItem("Periode Pencatatan","Awal bulan anggaran dan minggu","calendar_today_outlined","Periode Pencatatan"),
    ProfileItem("Kategori","Kelola kategori dan subkategori","category","Kategori"),
    ProfileItem("Pengaturan Dompet","Kelola dompet dan pocket","account_balance_wallet_outlined","Dompet"),
    ProfileItem("Notifikasi","Pengingat dan peringatan keuangan","notifications_outlined","Notifikasi"),
)
private val appearanceItems=listOf(
    ProfileItem("Tema","Mode terang, gelap dan warna","dark_mode_outlined","Tema"),
    ProfileItem("Tampilan Kartu","Gaya kartu dompet","credit_card_outlined","Tampilan Kartu"),
    ProfileItem("Tampilan Menu","Susunan menu Beranda","view_module_outlined","Tampilan Menu"),
    ProfileItem("Palet Transaksi","Warna pemasukan dan pengeluaran","palette_outlined","Palet Transaksi"),
    ProfileItem("Urutan Statistik","Ubah urutan bagian statistik","checklist","Urutan Statistik"),
    ProfileItem("Tampilan Beranda","Periode awal Beranda","home_outlined","Tampilan Beranda"),
    ProfileItem("Tampilan Tambah Transaksi","Kepadatan panel pencatatan","tune","Tampilan Tambah Transaksi"),
    ProfileItem("Paket Ikon","Pilih gaya ikon aplikasi","interests","Paket Ikon"),
    ProfileItem("Format Saldo","Desimal, simbol dan privasi","payments","Format Saldo"),
    ProfileItem("Memori Pencatatan","Dompet dan kategori awal","history","Memori Pencatatan"),
)
private val futureItems=listOf(
    ProfileItem("Masukan & Peta Jalan","Lihat status dan usulkan fitur berikutnya","flag_outlined","Masukan & Peta Jalan"),
)
private val securityItems=listOf(
    ProfileItem("Keamanan","Biometrik dan kunci perangkat","lock_outline","Pengaturan"),
    ProfileItem("Notifikasi Bank/E-Wallet","Impor transaksi dengan persetujuan","notifications_outlined","Impor notifikasi"),
    ProfileItem("Bantuan","Panduan penggunaan FinFlow","info_outline_rounded","Bantuan"),
    ProfileItem("Kirim Masukan","Sampaikan saranmu","email_outlined","Masukan"),
    ProfileItem("Bagikan","Bagikan FinFlow","north_east","Bagikan"),
    ProfileItem("Laporkan Bug","Beri tahu masalah yang kamu temui","warning_amber_outlined","Laporkan Bug"),
    ProfileItem("Tentang","FinFlow dan informasi versi","info_outline_rounded","Tentang"),
    ProfileItem("Catatan Pembaruan","Lihat perubahan aplikasi","history","Catatan Pembaruan"),
    ProfileItem("Hapus Data","Mulai ulang catatan, pertahankan akun","delete_outline","Hapus Data"),
)

@Composable
fun ProfileScreen(native:NativeServices,name:String,email:String,data:FinanceData,onNavigate:(String)->Unit,logout:()->Unit,localMode:Boolean=false,premiumCard:@Composable ()->Unit={}) {
    var search by remember{mutableStateOf(false)}
    var query by remember{mutableStateOf("")}
    var confirmLogout by remember{mutableStateOf(false)}
    val p=LocalPreferences.current
    val store=LocalPreferenceState.current
    val days=recordingProgress(data).activeDays
    val matches:(ProfileItem)->Boolean={query.isBlank() || "${it.title} ${it.caption}".contains(query,true)}
    LazyColumn(Modifier.fillMaxSize().testTag("profile-list").background(FFColors.background).statusBarsPadding(),contentPadding=PaddingValues(start=16.dp,end=16.dp,top=8.dp,bottom=124.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){FFIconButton("settings_outlined",{onNavigate("Pengaturan")});Text("Profil",Modifier.weight(1f),style=FFType.appTitle,textAlign=TextAlign.Center);FFIconButton("search",{search=!search})};if(search)Field("Cari pengaturan",query,{query=it})}
        if(query.isBlank())item { ProfileHeader(native,name,data,days,onNavigate) }
        if(query.isBlank())item { premiumCard() }
        item{SectionCaption("Akun & Data")}
        accountItems.filter(matches).chunked(2).forEach{row->item{Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){row.forEach{ProfileTile(it,Modifier.weight(1f),onNavigate)};if(row.size==1)Spacer(Modifier.weight(1f))}}}
        items(financeItems.filter(matches),key={it.title}){ProfileRow(it,onNavigate)}
        item{SectionCaption("Preferensi")}
        if(query.isBlank())item{LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp)){items(listOf("neutral" to "Neutral Light","arctic" to "Arctic Blue","ruby" to "Ruby","emerald" to "Emerald","indigo" to "Indigo")){(value,label)->Surface(onClick={store?.update(p.copy(palette=value))},color=if(value==p.palette)FFColors.primaryContainer else FFColors.surface,shape=RoundedCornerShape(16.dp)){Column(Modifier.width(92.dp).padding(12.dp),horizontalAlignment=Alignment.CenterHorizontally){FFIcon("palette_outlined",FFColors.onSurfaceVariant,30.dp);Spacer(Modifier.height(8.dp));Text(label,style=FFType.metaData,maxLines=1)}}}}}
        appearanceItems.take(4).filter(matches).chunked(2).forEach{row->item{Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){row.forEach{ProfileTile(it,Modifier.weight(1f),onNavigate)};if(row.size==1)Spacer(Modifier.weight(1f))}}}
        items(appearanceItems.drop(4).filter(matches),key={it.title}){ProfileRow(it,onNavigate)}
        item{SectionCaption("Fitur Masa Mendatang")}
        items(futureItems.filter(matches),key={it.title}){ProfileRow(it,onNavigate)}
        item{SectionCaption("Keamanan & Lainnya")}
        items(securityItems.filter(matches),key={it.title}){ProfileRow(it,onNavigate)}
        item{OutlinedButton(onClick={confirmLogout=true},modifier=Modifier.fillMaxWidth()){Text(if(localMode)"Keluar" else "Keluar dari Akun",color=FFColors.expense)}}
        item{Text("FinFlow · Android / iOS",Modifier.fillMaxWidth(),style=FFType.metaData,color=FFColors.onSurfaceVariant,textAlign=TextAlign.Center)}
    }
    if(confirmLogout)AlertDialog(onDismissRequest={confirmLogout=false},title={Text("Keluar dari FinFlow?")},text={Text(if(localMode)"Data tetap tersimpan di perangkat ini." else "Data akun tetap tersimpan. Masuk kembali untuk membukanya.")},confirmButton={TextButton(onClick={confirmLogout=false;logout()}){Text("Keluar")}},dismissButton={TextButton(onClick={confirmLogout=false}){Text("Batal")}})
}

@Composable
private fun ProfileHeader(native:NativeServices,name:String,data:FinanceData,days:Int,onNavigate:(String)->Unit) {
    Row(Modifier.fillMaxWidth().testTag("profile-header").clickable{onNavigate("Profil akun")}.padding(vertical=12.dp),verticalAlignment=Alignment.Top) {
        Box(Modifier.testTag("profile-avatar")) {
            ProfilePhoto(data.profile,native,Modifier.size(88.dp).clip(CircleShape).background(FFColors.surface).border(1.dp,FFColors.outlineVariant,CircleShape))
            Box(Modifier.align(Alignment.BottomEnd).size(30.dp).clip(CircleShape).background(FFColors.onSurface).border(2.dp,FFColors.background,CircleShape),contentAlignment=Alignment.Center){
                FFIcon("edit_rounded",FFColors.canvas,16.dp)
            }
        }
        Spacer(Modifier.width(20.dp))
        Column(Modifier.weight(1f)) {
            Text(name,style=FFType.appTitle)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Row(Modifier.weight(1f).clickable{onNavigate("Aktivitas")},verticalAlignment=Alignment.CenterVertically) {
                    ProfileCounter("LEVEL",recordingProgress(data).level.toString(),Modifier.weight(1f))
                    Box(Modifier.width(1.dp).height(30.dp).background(FFColors.outlineVariant))
                    ProfileCounter("STREAK",recordingStreak(data).toString(),Modifier.weight(1f))
                    Box(Modifier.width(1.dp).height(30.dp).background(FFColors.outlineVariant))
                    ProfileCounter("HARI AKTIF",days.toString(),Modifier.weight(1.4f))
                }
                FFIcon("chevron_right_rounded",FFColors.outline,20.dp)
            }
        }
    }
}

@Composable private fun SectionCaption(label:String){Text(label,style=FFType.metaData.copy(fontWeight=FontWeight.SemiBold),color=FFColors.onSurfaceVariant)}

fun recordingStreak(data:FinanceData):Int {
    return recordingProgress(data).streak
}
@Composable private fun ProfileCounter(label:String,value:String,modifier:Modifier){Column(modifier){Text(value,style=FFType.sectionTitle);Text(label,style=FFType.badgeLabel,color=FFColors.onSurfaceVariant)}}
@Composable private fun ProfileTile(item:ProfileItem,modifier:Modifier,navigate:(String)->Unit){DesignCard(modifier.height(172.dp),onClick={navigate(item.route)}){Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(FFColors.outlineVariant),contentAlignment=Alignment.Center){FFIcon(item.icon,FFColors.onSurfaceVariant,20.dp)};Spacer(Modifier.weight(1f));FFIcon("chevron_right_rounded",FFColors.outline,20.dp)};Spacer(Modifier.height(16.dp));Text(item.title,style=FFType.bodyMain,fontWeight=FontWeight.Medium);Text(item.caption,style=FFType.metaData,color=FFColors.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)}}
@Composable private fun ProfileRow(item:ProfileItem,navigate:(String)->Unit){DesignCard(onClick={navigate(item.route)}){Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(40.dp).clip(CircleShape).background(FFColors.outlineVariant),contentAlignment=Alignment.Center){FFIcon(item.icon,FFColors.onSurfaceVariant,20.dp)};Spacer(Modifier.width(16.dp));Column(Modifier.weight(1f)){Text(item.title,style=FFType.bodyMain,fontWeight=FontWeight.Medium);Text(item.caption,style=FFType.metaData,color=FFColors.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)};FFIcon("chevron_right_rounded",FFColors.outline,20.dp)}}}
