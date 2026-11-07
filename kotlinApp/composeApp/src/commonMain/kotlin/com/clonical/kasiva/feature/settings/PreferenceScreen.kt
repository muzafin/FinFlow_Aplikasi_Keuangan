package com.clonical.kasiva.feature.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonical.kasiva.core.model.*
import com.clonical.kasiva.core.ui.*
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.i18n.*
import kotlin.math.abs

val preferencePages=setOf("Bahasa","Tema","Tampilan Kartu","Tampilan Menu","Susunan Menu","Palet Transaksi","Urutan Statistik","Tampilan Beranda","Tampilan Tambah Transaksi","Paket Ikon","Format Saldo","Periode Pencatatan","Memori Pencatatan")

/** Label tampilan untuk kunci halaman di [preferencePages] (kunci navigasi tetap berbahasa Indonesia). */
fun preferencePageLabel(page:String):String=when(page){
    "Bahasa"->MR.strings.set_page_language.text()
    "Tema"->MR.strings.set_page_theme.text()
    "Tampilan Kartu"->MR.strings.set_page_card_display.text()
    "Tampilan Menu"->MR.strings.set_page_menu_display.text()
    "Susunan Menu"->MR.strings.set_page_menu_order.text()
    "Palet Transaksi"->MR.strings.set_page_tx_palette.text()
    "Urutan Statistik"->MR.strings.set_page_stats_order.text()
    "Tampilan Beranda"->MR.strings.set_page_home_display.text()
    "Tampilan Tambah Transaksi"->MR.strings.set_page_add_tx_display.text()
    "Paket Ikon"->MR.strings.set_page_icon_pack.text()
    "Format Saldo"->MR.strings.set_page_balance_format.text()
    "Periode Pencatatan"->MR.strings.set_page_recording_period.text()
    "Memori Pencatatan"->MR.strings.set_page_recording_memory.text()
    else->page
}
private fun sectionName(id:String):String?=when(id){
    "comparison"->MR.strings.set_section_comparison.text()
    "trend"->MR.strings.set_section_trend.text()
    "cycle"->MR.strings.set_section_cycle.text()
    "categories"->MR.strings.set_section_categories.text()
    "transactions"->MR.strings.set_section_transactions.text()
    else->null
}
private val sectionIcons=mapOf("comparison" to "hi_sort","trend" to "hi_trend_up","cycle" to "hi_recurring","categories" to "hi_invest","transactions" to "hi_note_doc")
private fun menuName(id:String):String?=when(id){
    "Anggaran"->MR.strings.set_menu_budget.text()
    "Berulang"->MR.strings.set_menu_recurring.text()
    "Target"->MR.strings.set_menu_savings.text()
    "Tagihan"->MR.strings.set_menu_bills.text()
    "Utang"->MR.strings.set_menu_debts.text()
    "Keinginan"->MR.strings.set_menu_wishlist.text()
    "Kartu"->MR.strings.set_menu_cards.text()
    "Catatan"->MR.strings.set_menu_notes.text()
    "Reimburse"->MR.strings.set_menu_reimburse.text()
    "Investasi"->MR.strings.set_menu_assets.text()
    else->null
}

/**
 * Halaman preferensi bergaya Ollo. [onNavigate] dipakai Tampilan Beranda untuk membuka Susunan/Tampilan Menu
 * (App perlu meneruskan `::navigate`; tanpa itu baris tersebut tidak berpindah halaman).
 */
@Composable
fun PreferenceScreen(page:String,data:FinanceData,onNavigate:(String)->Unit={}) {
    val store=LocalPreferenceState.current
    val p=LocalPreferences.current
    var error by remember{mutableStateOf<String?>(null)}
    fun update(value:AppPreferences){try{requireNotNull(store){MR.strings.set_pref_unavailable.text()}.update(value);error=null}catch(e:Exception){error=e.message}}
    var cardTab by rememberSaveable(page){mutableStateOf(cardTabOf(p.cardGradient))}
    var draftDay by remember(page,p.recordingDay){mutableIntStateOf(p.recordingDay)}
    var draftWeek by remember(page,p.weekStart){mutableIntStateOf(p.weekStart)}
    val reorder=page=="Susunan Menu" || page=="Urutan Statistik"
    if(reorder) TopBarActions {
        var menu by remember{mutableStateOf(false)}
        Box {
            IconButton(onClick={menu=true},modifier=Modifier.testTag("reorder-more")){FFIcon("hi_more",FFColors.onSurface,24.dp,MR.strings.pc_more_options.text())}
            DropdownMenu(menu,{menu=false},containerColor=FFColors.surface) {
                DropdownMenuItem(text={Text(MR.strings.set_pref_reset_order.text(),style=FFType.bodyMain)},leadingIcon={FFIcon("hi_reimburse",FFColors.onSurfaceVariant,20.dp)},
                    modifier=Modifier.testTag("reorder-reset"),onClick={menu=false;update(if(page=="Susunan Menu")p.copy(menuOrder=financeMenus,hiddenMenus=emptyList()) else p.copy(statisticsOrder=statisticsSections))})
            }
        }
    }
    if(page=="Periode Pencatatan") TopBarActions {
        val changed=draftDay!=p.recordingDay || draftWeek!=p.weekStart
        IconButton(onClick={update(p.copy(recordingDay=draftDay,weekStart=draftWeek))},enabled=changed,modifier=Modifier.testTag("recording-period-save")){
            FFIcon("hi_check",if(changed)FFColors.onSurface else FFColors.outline,24.dp,MR.strings.common_save.text())
        }
    }
    LazyColumn(Modifier.fillMaxSize().testTag("preferences-list"),contentPadding=PaddingValues(start=16.dp,end=16.dp,top=12.dp,bottom=32.dp),
        verticalArrangement=Arrangement.spacedBy(if(reorder) 8.dp else 12.dp)) {
        if(reorder) {
            val menus=page=="Susunan Menu"
            val order=if(menus)p.menuOrder else p.statisticsOrder
            // Baca nilai terbaru dari store agar dua aksi berurutan tidak saling menimpa (closure lama).
            fun latest()=store?.value?:p
            fun move(from:Int,to:Int){val q=latest();val current=if(menus)q.menuOrder else q.statisticsOrder;if(to in current.indices && from in current.indices){val next=current.toMutableList().apply{add(to,removeAt(from))};update(if(menus)q.copy(menuOrder=next) else q.copy(statisticsOrder=next))}}
            itemsIndexed(order,key={_,id->id}){index,id->
                ReorderRow(id,index,order.lastIndex,menus,id !in p.hiddenMenus,::move){show->val q=latest();update(q.copy(hiddenMenus=if(show)q.hiddenMenus-id else q.hiddenMenus+id))}
            }
        } else when(page) {
            "Bahasa" -> {
                item{Text(MR.strings.set_language_note.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant)}
                val options=listOf("system" to MR.strings.set_language_follow_device.text())+com.clonical.kasiva.core.i18n.appLanguages.map{it.tag to it.nativeName}
                items(options,key={it.first}){(tag,name)->
                    val selected=p.language==tag
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable{update(p.copy(language=tag))}.padding(horizontal=12.dp,vertical=14.dp).testTag("language-$tag"),verticalAlignment=Alignment.CenterVertically){
                        Text(name,Modifier.weight(1f),style=FFType.bodyMain,fontWeight=if(selected)FontWeight.SemiBold else FontWeight.Normal,color=FFColors.onSurface)
                        if(selected)FFIcon("hi_check",FFColors.onSurface,20.dp)
                    }
                }
            }
            "Tema" -> themePage(p,::update)
            "Tampilan Kartu" -> cardPage(p,::update,cardTab){cardTab=it}
            "Tampilan Menu" -> menuAppearancePage(p,::update){menuName(it) ?: it}
            "Palet Transaksi" -> transactionPalettePage(p,::update)
            "Tampilan Beranda" -> homeViewPage(p,::update,onNavigate)
            "Tampilan Tambah Transaksi" -> addTransactionPage(p,::update)
            "Paket Ikon" -> iconPackPage(p,::update)
            "Format Saldo" -> balanceFormatPage(p,::update)
            "Periode Pencatatan" -> recordingPeriodPage(draftDay,draftWeek,{draftDay=it},{draftWeek=it})
            "Memori Pencatatan" -> recordingMemoryPage(p,::update,data)
        }
        error?.let{item{Text(it,color=MaterialTheme.colorScheme.error,style=FFType.metaData)}}
    }
}

/** Baris urutan gaya Ollo: ikon lingkaran, nama, pegangan seret (tekan lama lalu geser). */
@Composable
private fun LazyItemScope.ReorderRow(id:String,index:Int,last:Int,menus:Boolean,visible:Boolean,move:(Int,Int)->Unit,toggle:(Boolean)->Unit) {
    val name=menuName(id) ?: sectionName(id) ?: id
    val step=with(LocalDensity.current){64.dp.toPx()}
    val currentIndex by rememberUpdatedState(index)
    val currentMove by rememberUpdatedState(move)
    var distance by remember{mutableFloatStateOf(0f)}
    val up=MR.strings.set_pref_move_up.text(name)
    val down=MR.strings.set_pref_move_down.text(name)
    PrefCard(Modifier.animateItem().testTag("reorder-$id").semantics{customActions=listOf(
        CustomAccessibilityAction(up){move(currentIndex,currentIndex-1);true},CustomAccessibilityAction(down){move(currentIndex,currentIndex+1);true})}) {
        Row(Modifier.fillMaxWidth().heightIn(min=56.dp).padding(start=16.dp,end=8.dp,top=6.dp,bottom=6.dp),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(FFColors.iconCircle),contentAlignment=Alignment.Center){
                FFIcon((if(menus)menuIconsById[id] else sectionIcons[id]) ?: "hi_grid",FFColors.onSurface,22.dp)
            }
            Spacer(Modifier.width(16.dp))
            Text(name,Modifier.weight(1f),style=FFType.bodyMain.copy(fontSize=16.5.sp,lineHeight=22.sp),color=if(visible)FFColors.onSurface else FFColors.onSurfaceVariant)
            if(menus){
                PrefSwitch(visible,toggle,Modifier.testTag("menu-visible-$id"))
                // Tombol naik/turun tetap ada untuk Susunan Menu (dipakai uji & aksesibilitas).
                Column {
                    IconButton(onClick={move(index,index-1)},enabled=index>0,modifier=Modifier.size(26.dp)){FFIcon("hi_chevron_up",FFColors.onSurfaceVariant,16.dp,MR.strings.set_pref_move_up.text(id))}
                    IconButton(onClick={move(index,index+1)},enabled=index<last,modifier=Modifier.size(26.dp)){FFIcon("hi_chevron_down",FFColors.onSurfaceVariant,16.dp,MR.strings.set_pref_move_down.text(id))}
                }
            }
            Box(Modifier.size(44.dp).pointerInput(id,step){detectDragGesturesAfterLongPress(
                onDragStart={distance=0f},onDragEnd={distance=0f},onDragCancel={distance=0f},onDrag={change,amount->
                    change.consume();distance+=amount.y
                    if(abs(distance)>=step){val direction=if(distance>0)1 else -1;currentMove(currentIndex,currentIndex+direction);distance-=direction*step}
                })}.testTag("reorder-handle-$id"),contentAlignment=Alignment.Center){FFIcon("drag_handle",FFColors.onSurfaceVariant,26.dp,MR.strings.set_pref_drag.text(name))}
        }
    }
}
