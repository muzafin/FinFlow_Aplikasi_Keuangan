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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.sp
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
    ProfileItem("Manajemen Akun","Profil, login dan sinkronisasi Firebase","hi_user","Manajemen Akun"),
    ProfileItem("Cadangan & Pemulihan","Simpan dan pulihkan seluruh data","hi_cloud_upload","Cadangan & Pemulihan"),
    ProfileItem("Perangkat Terhubung","Sesi pada perangkat ini","hi_laptop","Perangkat Terhubung"),
    ProfileItem("Ekspor Data","Ekspor transaksi ke PDF atau CSV","hi_download","Ekspor Data"),
)
private val financeItems=listOf(
    ProfileItem("Impor Data","Masukkan data dari berkas","hi_upload","Impor data"),
    ProfileItem("Periode Pencatatan","Awal bulan anggaran dan minggu","hi_calendar","Periode Pencatatan"),
    ProfileItem("Kategori","Kelola kategori dan subkategori","hi_tags","Kategori"),
    ProfileItem("Pengaturan Dompet","Kelola dompet dan pocket","hi_wallet","Dompet"),
    ProfileItem("Notifikasi","Pengingat dan peringatan keuangan","hi_bell","Notifikasi"),
)
private val appearanceItems=listOf(
    ProfileItem("Tema","Mode terang, gelap dan warna","hi_moon","Tema"),
    ProfileItem("Tampilan Kartu","Gaya kartu dompet","hi_card","Tampilan Kartu"),
    ProfileItem("Tampilan Menu","Susunan menu Beranda","hi_layout","Tampilan Menu"),
    ProfileItem("Palet Transaksi","Warna pemasukan dan pengeluaran","hi_paint","Palet Transaksi"),
    ProfileItem("Urutan Statistik","Ubah urutan bagian statistik","hi_sort_az","Urutan Statistik"),
    ProfileItem("Tampilan Beranda","Periode awal Beranda","hi_home","Tampilan Beranda"),
    ProfileItem("Tampilan Tambah Transaksi","Kepadatan panel pencatatan","hi_add_circle","Tampilan Tambah Transaksi"),
    ProfileItem("Paket Ikon","Pilih gaya ikon aplikasi","hi_grid_view","Paket Ikon"),
    ProfileItem("Format Saldo","Desimal, simbol dan privasi","hi_coins","Format Saldo"),
    ProfileItem("Memori Pencatatan","Dompet dan kategori awal","hi_brain","Memori Pencatatan"),
)
private val futureItems=listOf(
    ProfileItem("Masukan & Peta Jalan","Lihat status dan usulkan fitur berikutnya","hi_flag","Masukan & Peta Jalan"),
)
private val securityItems=listOf(
    ProfileItem("Keamanan","Biometrik dan kunci perangkat","hi_shield","Pengaturan"),
    ProfileItem("Notifikasi Bank/E-Wallet","Impor transaksi dengan persetujuan","hi_bell","Impor notifikasi"),
    ProfileItem("Bantuan","Panduan penggunaan FinFlow","hi_help","Bantuan"),
    ProfileItem("Kirim Masukan","Sampaikan saranmu","hi_mail","Masukan"),
    ProfileItem("Bagikan","Bagikan FinFlow","hi_share","Bagikan"),
    ProfileItem("Laporkan Bug","Beri tahu masalah yang kamu temui","hi_bug","Laporkan Bug"),
    ProfileItem("Tentang","FinFlow dan informasi versi","hi_info","Tentang"),
    ProfileItem("Catatan Pembaruan","Lihat perubahan aplikasi","hi_clock","Catatan Pembaruan"),
    ProfileItem("Hapus Data","Mulai ulang catatan, pertahankan akun","hi_delete","Hapus Data"),
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
    LazyColumn(Modifier.fillMaxSize().testTag("profile-list").background(FFColors.background).statusBarsPadding(),contentPadding=PaddingValues(start=16.dp,end=16.dp,top=8.dp,bottom=124.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){IconButton({onNavigate("Pengaturan")}){FFIcon("hi_settings",FFColors.onSurface,26.dp,contentDescription="Pengaturan")};Text("Profil",Modifier.weight(1f),style=FFType.appTitle,textAlign=TextAlign.Center);IconButton({search=!search}){FFIcon("hi_search",FFColors.onSurface,26.dp,contentDescription="Cari pengaturan")}};if(search)Field("Cari pengaturan",query,{query=it})}
        if(query.isBlank())item { ProfileHeader(native,name,data,days,onNavigate) }
        if(query.isBlank())item { premiumCard() }
        item{SectionCaption("Akun & Data")}
        accountItems.filter(matches).chunked(2).forEach{row->item{Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){row.forEach{ProfileTile(it,Modifier.weight(1f),onNavigate)};if(row.size==1)Spacer(Modifier.weight(1f))}}}
        items(financeItems.filter(matches),key={it.title}){ProfileRow(it,onNavigate)}
        item{SectionCaption("Preferensi")}
        if(query.isBlank())item{LazyRow(horizontalArrangement=Arrangement.spacedBy(9.dp)){items(listOf("neutral" to "Neutral","arctic" to "Arctic Blue","ruby" to "Ruby","emerald" to "Emerald","indigo" to "Indigo")){(value,label)->PaletteTile(value,label,value==p.palette){store?.update(p.copy(palette=value))}}}}
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

// Pratinjau palet Ollo: mini-layar gelap dengan bilah aksen dan titik-titik warna.
private val paletteSwatches=mapOf(
    "neutral" to listOf(0xFFF4F4F5,0xFFE4E4E7,0xFFD4D4D8),"arctic" to listOf(0xFF8DCFF0,0xFF4FB3E8,0xFF2CC4C9),
    "ruby" to listOf(0xFFE53935,0xFFEF5350,0xFFFF8A80),"emerald" to listOf(0xFF2E7D32,0xFF43A047,0xFF66BB6A),"indigo" to listOf(0xFF3949AB,0xFF5C6BC0,0xFF4FC3F7))
@Composable private fun PaletteTile(value:String,label:String,selected:Boolean,select:()->Unit){
    val colors=paletteSwatches.getValue(value).map{Color(it)}
    val shape=RoundedCornerShape(14.dp)
    Column(Modifier.width(76.dp).clip(shape).background(FFColors.surface).border(1.dp,if(selected)FFColors.onSurface.copy(alpha=.6f) else FFColors.hairline,shape)
        .clickable(onClick=select).padding(top=8.dp,bottom=6.dp).testTag("palette-$value"),horizontalAlignment=Alignment.CenterHorizontally){
        Column(Modifier.size(50.dp,44.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFF09090B)).padding(6.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
            Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(3.dp)).background(colors[0])){Box(Modifier.padding(start=4.dp).align(Alignment.CenterStart).size(10.dp,2.dp).background(Color(0xFF09090B).copy(alpha=.5f)))}
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){colors.forEach{Box(Modifier.size(8.dp).clip(CircleShape).background(it))}}
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){colors.reversed().forEach{Box(Modifier.size(4.dp).clip(CircleShape).background(it.copy(alpha=.8f)))}}
        }
        Spacer(Modifier.height(5.dp))
        Text(label,style=FFType.metaData.copy(fontSize=9.sp,lineHeight=12.sp),maxLines=1)
    }
}

@Composable
private fun ProfileHeader(native:NativeServices,name:String,data:FinanceData,days:Int,onNavigate:(String)->Unit) {
    val dark=FFColors.background.luminance()<.5f
    Row(Modifier.fillMaxWidth().testTag("profile-header").clickable{onNavigate("Profil akun")}.padding(top=12.dp,bottom=6.dp),verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.testTag("profile-avatar")) {
            ProfilePhoto(data.profile,native,Modifier.size(78.dp).clip(CircleShape).background(if(dark)FFColors.chip else FFColors.surface).border(1.5.dp,if(dark)FFColors.hairline else FFColors.outlineVariant,CircleShape))
            Box(Modifier.align(Alignment.BottomEnd).offset(x=2.dp,y=2.dp).size(26.dp).clip(CircleShape).background(FFColors.pill),contentAlignment=Alignment.Center){
                FFIcon("hi_pencil",FFColors.onPill,14.dp)
            }
        }
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(name,style=FFType.appTitle.copy(fontSize=23.sp,lineHeight=28.sp),maxLines=1,overflow=TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Row(Modifier.weight(1f).clickable{onNavigate("Aktivitas")},verticalAlignment=Alignment.CenterVertically) {
                    ProfileCounter("LEVEL",recordingProgress(data).level.toString(),Modifier.weight(1f))
                    Box(Modifier.padding(end=14.dp).width(1.dp).height(30.dp).background(FFColors.outlineVariant))
                    ProfileCounter("STREAK",recordingStreak(data).toString(),Modifier.weight(1f))
                    Box(Modifier.padding(end=14.dp).width(1.dp).height(30.dp).background(FFColors.outlineVariant))
                    ProfileCounter("HARI AKTIF",days.toString(),Modifier.weight(1.4f))
                }
                FFIcon("hi_chevron_right",FFColors.onSurfaceVariant,20.dp)
            }
        }
    }
}

@Composable private fun SectionCaption(label:String){Text(label,Modifier.padding(start=6.dp,top=12.dp,bottom=2.dp),style=FFType.metaData.copy(letterSpacing=1.sp),color=FFColors.onSurfaceVariant.copy(alpha=.75f))}

fun recordingStreak(data:FinanceData):Int {
    return recordingProgress(data).streak
}
@Composable private fun ProfileCounter(label:String,value:String,modifier:Modifier){Column(modifier){Text(value,style=FFType.bodyMain.copy(fontSize=16.sp,lineHeight=22.sp));Text(label,style=FFType.metaData.copy(fontSize=10.sp,lineHeight=13.sp),color=FFColors.onSurfaceVariant,maxLines=1)}}
@Composable private fun ProfileIconBox(icon:String,circle:Boolean){
    val dark=FFColors.background.luminance()<.5f
    Box(Modifier.size(40.dp).clip(if(circle)CircleShape else RoundedCornerShape(12.dp)).background(if(dark)FFColors.hairline else FFColors.surfaceContainerHigh),contentAlignment=Alignment.Center){FFIcon(icon,FFColors.onSurface,20.dp)}
}
@Composable private fun ProfileTile(item:ProfileItem,modifier:Modifier,navigate:(String)->Unit){
    Column(modifier.height(124.dp).clip(RoundedCornerShape(20.dp)).background(FFColors.surface).clickable{navigate(item.route)}.padding(horizontal=14.dp,vertical=12.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){ProfileIconBox(item.icon,false);Spacer(Modifier.weight(1f));FFIcon("hi_chevron_right",FFColors.onSurfaceVariant,18.dp)}
        Spacer(Modifier.height(10.dp))
        FitMoneyText(item.title,FFType.bodyMain.copy(fontSize=15.sp,lineHeight=19.sp))
        Text(item.caption,style=FFType.metaData.copy(lineHeight=15.sp),color=FFColors.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)
    }
}
@Composable private fun ProfileRow(item:ProfileItem,navigate:(String)->Unit){
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(FFColors.surface).clickable{navigate(item.route)}.padding(horizontal=16.dp,vertical=16.dp),verticalAlignment=Alignment.CenterVertically){
        ProfileIconBox(item.icon,true)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)){
            Text(item.title,style=FFType.bodyMain.copy(fontSize=16.5.sp,lineHeight=21.sp),maxLines=1,overflow=TextOverflow.Ellipsis)
            Text(item.caption,style=FFType.metaData,color=FFColors.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)
        }
        FFIcon("hi_chevron_right",FFColors.onSurfaceVariant,20.dp)
    }
}
