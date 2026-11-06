package com.logikawan.finflow.feature.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.logikawan.finflow.core.model.*
import com.logikawan.finflow.core.ui.*
import com.logikawan.finflow.core.util.*
import kotlin.math.abs

val preferencePages=setOf("Tema","Tampilan Kartu","Tampilan Menu","Susunan Menu","Palet Transaksi","Urutan Statistik","Tampilan Beranda","Tampilan Tambah Transaksi","Paket Ikon","Format Saldo","Periode Pencatatan","Memori Pencatatan")
private val sectionNames=mapOf("comparison" to "Perbandingan Bulanan","trend" to "Tren Saldo Bersih","cycle" to "Wawasan Periode","categories" to "Pengeluaran Terbesar","transactions" to "Jurnal Transaksi")
private val menuNames=mapOf("Anggaran" to "Anggaran","Berulang" to "Berulang","Target" to "Tabungan","Tagihan" to "Tagihan","Utang" to "Utang","Keinginan" to "Wishlist","Kartu" to "Kartu","Catatan" to "Catatan","Reimburse" to "Reimburse","Investasi" to "Manajemen Aset")

@Composable
fun PreferenceScreen(page:String,data:FinanceData) {
    val store=LocalPreferenceState.current
    val p=LocalPreferences.current
    var error by remember{mutableStateOf<String?>(null)}
    fun update(value:AppPreferences){try{requireNotNull(store){"Pengaturan belum tersedia"}.update(value);error=null}catch(e:Exception){error=e.message}}
    LazyColumn(Modifier.fillMaxSize().testTag("preferences-list"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        if(page=="Susunan Menu" || page=="Urutan Statistik") {
            val order=if(page=="Susunan Menu")p.menuOrder else p.statisticsOrder
            fun move(from:Int,to:Int){if(to in order.indices){val next=order.toMutableList().apply{add(to,removeAt(from))};update(if(page=="Susunan Menu")p.copy(menuOrder=next) else p.copy(statisticsOrder=next))}}
            item { Text("Tahan dan geser pegangan untuk mengubah urutan. Tombol panah juga dapat digunakan.",style=FFType.metaData,color=FFColors.onSurfaceVariant) }
            itemsIndexed(order,key={_,id->id}){index,id->
                val step=with(LocalDensity.current){88.dp.toPx()}
                val currentIndex by rememberUpdatedState(index)
                val currentMove by rememberUpdatedState(::move)
                var distance by remember{mutableFloatStateOf(0f)}
                DesignCard {
                    Row(Modifier.fillMaxWidth().heightIn(min=48.dp),verticalAlignment=Alignment.CenterVertically) {
                        Text(menuNames[id] ?: sectionNames[id] ?: id,Modifier.weight(1f),style=FFType.bodyMain)
                        if(page=="Susunan Menu")Switch(id !in p.hiddenMenus,{show->update(p.copy(hiddenMenus=if(show)p.hiddenMenus-id else p.hiddenMenus+id))})
                        Column { IconButton(onClick={move(index,index-1)},enabled=index>0,modifier=Modifier.size(28.dp)){FFIcon("keyboard_arrow_up",size=20.dp,contentDescription="Naik $id")};IconButton(onClick={move(index,index+1)},enabled=index<order.lastIndex,modifier=Modifier.size(28.dp)){FFIcon("keyboard_arrow_down",size=20.dp,contentDescription="Turun $id")} }
                        Box(Modifier.size(40.dp).pointerInput(id,step){detectDragGesturesAfterLongPress(
                            onDragStart={distance=0f},onDragEnd={distance=0f},onDragCancel={distance=0f},onDrag={change,amount->
                                change.consume();distance+=amount.y
                                if(abs(distance)>=step){val direction=if(distance>0)1 else -1;currentMove(currentIndex,currentIndex+direction);distance-=direction*step}
                            })},contentAlignment=Alignment.Center){FFIcon("drag_handle",contentDescription="Geser $id")}
                    }
                }
            }
            item { OutlinedButton(onClick={update(if(page=="Susunan Menu")p.copy(menuOrder=financeMenus,hiddenMenus=emptyList()) else p.copy(statisticsOrder=statisticsSections))}){Text("Pulihkan urutan awal")} }
        } else when(page) {
            "Tema" -> {
                item{Choice("Preset mode",themeModePresetOf(p.theme,p.palette),themeModePresets.map{it.id to it.label},{id->themeModePresets.firstOrNull{it.id==id}?.let{update(p.copy(theme=it.theme,palette=it.palette))}})}
                item{Choice("Mode tampilan",p.theme,listOf("system" to "Ikuti perangkat","light" to "Terang","dark" to "Gelap"),{update(p.copy(theme=it))})}
                item{Choice("Palet warna",p.palette,listOf("neutral" to "Neutral","arctic" to "Arctic Blue","ruby" to "Ruby","emerald" to "Emerald","indigo" to "Indigo"),{update(p.copy(palette=it))})}
            }
            "Tampilan Kartu" -> {
                item{Choice("Gaya kartu dompet",p.cardAppearance,listOf("gradient" to "Gradien","plain" to "Polos","outline" to "Garis tepi"),{update(p.copy(cardAppearance=it))})}
                item{Choice("Gradien kartu saldo",p.cardGradient,cardGradientPresets.map{it.id to it.label}){update(p.copy(cardGradient=it))}}
            }
            "Tampilan Menu" -> {
                item{Choice("Susunan menu Beranda",p.menuAppearance,listOf("horizontal" to "Geser horizontal","grid" to "Grid","list" to "Daftar"),{update(p.copy(menuAppearance=it))})}
                item{Choice("Warna ikon menu",p.menuTheme,menuThemePresets.map{it.id to it.label}){update(p.copy(menuTheme=it))}}
            }
            "Palet Transaksi" -> {
                item{Choice("Pemasukan",p.incomeColor.toString(),listOf(0xFF4CAF50L to "Hijau",0xFF009688L to "Teal",0xFF2196F3L to "Biru").map{it.first.toString() to it.second},{update(p.copy(incomeColor=it.toLong()))})}
                item{Choice("Pengeluaran",p.expenseColor.toString(),listOf(0xFFFF5252L to "Merah",0xFFFF9800L to "Oranye",0xFF9C27B0L to "Ungu").map{it.first.toString() to it.second},{update(p.copy(expenseColor=it.toLong()))})}
            }
            "Tampilan Beranda" -> item{Choice("Periode saat Beranda dibuka",p.homePeriod,listOf("Hari Ini","Minggu Ini","Bulan Ini","Tahun","Kalender").map{it to it.removeSuffix(" Ini")},{update(p.copy(homePeriod=it))})}
            "Tampilan Tambah Transaksi" -> {
                item{Choice("Kepadatan panel transaksi",p.transactionDisplay,listOf("comfortable" to "Nyaman","compact" to "Ringkas"),{update(p.copy(transactionDisplay=it))})}
                item{Choice("Perilaku tombol +",p.addButtonMode,listOf("quick" to "Langsung buka tambah transaksi","all" to "Buka semua cara mencatat"),{update(p.copy(addButtonMode=it))})}
            }
            "Paket Ikon" -> item{Choice("Gaya ikon",p.iconPack,listOf("outlined" to "Garis","rounded" to "Solid / rounded"),{update(p.copy(iconPack=it))})}
            "Format Saldo" -> {
                item{Text(formatBalance(123456789,"IDR",p),style=FFType.amountLg)}
                item{PreferenceSwitch("Tampilkan angka desimal",p.showDecimals){update(p.copy(showDecimals=it))}}
                item{PreferenceSwitch("Gunakan simbol mata uang",p.showCurrencySymbol){update(p.copy(showCurrencySymbol=it))}}
                item{PreferenceSwitch("Sembunyikan saldo saat dibuka",p.hideBalances){update(p.copy(hideBalances=it))}}
                item{Choice("Pemisah angka",p.numberFormat,listOf("id" to "1.234.567,89","en" to "1,234,567.89"),{update(p.copy(numberFormat=it))})}
            }
            "Periode Pencatatan" -> {
                item{Text("Tanggal awal bulan anggaran: ${p.recordingDay}",style=FFType.sectionTitle);Slider(p.recordingDay.toFloat(),{update(p.copy(recordingDay=it.toInt().coerceIn(1,31)))},valueRange=1f..31f,steps=29)}
                item{Text("Bulan yang lebih pendek memakai hari terakhir bulan tersebut. Periode berikutnya kembali menggunakan tanggal pilihan Anda.",style=FFType.metaData,color=FFColors.onSurfaceVariant)}
                item{Choice("Awal minggu",p.weekStart.toString(),listOf("1" to "Senin","2" to "Selasa","3" to "Rabu","4" to "Kamis","5" to "Jumat","6" to "Sabtu","7" to "Minggu"),{update(p.copy(weekStart=it.toInt()))})}
            }
            "Memori Pencatatan" -> {
                item{Choice("Dompet awal",p.defaultWalletId,listOf("" to "Dompet aktif pertama")+data.accounts.filterNot{it.archived || it.type=="savings"}.map{it.id to it.name},{update(p.copy(defaultWalletId=it))})}
                item{PreferenceSwitch("Ingat kategori terakhir",p.rememberCategory){update(p.copy(rememberCategory=it))}}
            }
        }
        error?.let{item{Text(it,color=MaterialTheme.colorScheme.error)}}
        item{DesignCard {Text("Pratinjau",style=FFType.sectionTitle);Text("Pengaturan ini langsung diterapkan dan disimpan pada perangkat untuk akun Anda.",style=FFType.metaData);Spacer(Modifier.height(12.dp));Row{Text("Pemasukan",Modifier.weight(1f),color=FFColors.income);Text("Pengeluaran",color=FFColors.expense)};Text(formatBalance(1234500,"IDR",p),style=FFType.amountMd)}}
    }
}

@Composable private fun PreferenceSwitch(title:String,value:Boolean,change:(Boolean)->Unit) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(title,Modifier.weight(1f),style=FFType.bodyMain);Switch(value,change)}
}
