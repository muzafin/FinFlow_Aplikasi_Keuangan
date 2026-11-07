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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.logikawan.finflow.core.model.*
import com.logikawan.finflow.core.ui.*
import com.logikawan.finflow.core.util.*
import com.logikawan.finflow.core.i18n.periodLabel
import com.logikawan.finflow.core.i18n.text
import com.logikawan.finflow.core.i18n.transactionTypeLabel
import com.logikawan.finflow.i18n.*
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

@Composable
fun PreferenceScreen(page:String,data:FinanceData) {
    val store=LocalPreferenceState.current
    val p=LocalPreferences.current
    var error by remember{mutableStateOf<String?>(null)}
    fun update(value:AppPreferences){try{requireNotNull(store){MR.strings.set_pref_unavailable.text()}.update(value);error=null}catch(e:Exception){error=e.message}}
    LazyColumn(Modifier.fillMaxSize().testTag("preferences-list"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        if(page=="Susunan Menu" || page=="Urutan Statistik") {
            val order=if(page=="Susunan Menu")p.menuOrder else p.statisticsOrder
            fun move(from:Int,to:Int){if(to in order.indices){val next=order.toMutableList().apply{add(to,removeAt(from))};update(if(page=="Susunan Menu")p.copy(menuOrder=next) else p.copy(statisticsOrder=next))}}
            item { Text(MR.strings.set_pref_reorder_hint.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant) }
            itemsIndexed(order,key={_,id->id}){index,id->
                val step=with(LocalDensity.current){88.dp.toPx()}
                val currentIndex by rememberUpdatedState(index)
                val currentMove by rememberUpdatedState(::move)
                var distance by remember{mutableFloatStateOf(0f)}
                DesignCard {
                    Row(Modifier.fillMaxWidth().heightIn(min=48.dp),verticalAlignment=Alignment.CenterVertically) {
                        Text(menuName(id) ?: sectionName(id) ?: id,Modifier.weight(1f),style=FFType.bodyMain)
                        if(page=="Susunan Menu")Switch(id !in p.hiddenMenus,{show->update(p.copy(hiddenMenus=if(show)p.hiddenMenus-id else p.hiddenMenus+id))})
                        Column { IconButton(onClick={move(index,index-1)},enabled=index>0,modifier=Modifier.size(28.dp)){FFIcon("keyboard_arrow_up",size=20.dp,contentDescription=MR.strings.set_pref_move_up.text(id))};IconButton(onClick={move(index,index+1)},enabled=index<order.lastIndex,modifier=Modifier.size(28.dp)){FFIcon("keyboard_arrow_down",size=20.dp,contentDescription=MR.strings.set_pref_move_down.text(id))} }
                        Box(Modifier.size(40.dp).pointerInput(id,step){detectDragGesturesAfterLongPress(
                            onDragStart={distance=0f},onDragEnd={distance=0f},onDragCancel={distance=0f},onDrag={change,amount->
                                change.consume();distance+=amount.y
                                if(abs(distance)>=step){val direction=if(distance>0)1 else -1;currentMove(currentIndex,currentIndex+direction);distance-=direction*step}
                            })},contentAlignment=Alignment.Center){FFIcon("drag_handle",contentDescription=MR.strings.set_pref_drag.text(id))}
                    }
                }
            }
            item { OutlinedButton(onClick={update(if(page=="Susunan Menu")p.copy(menuOrder=financeMenus,hiddenMenus=emptyList()) else p.copy(statisticsOrder=statisticsSections))}){Text(MR.strings.set_pref_reset_order.text())} }
        } else when(page) {
            "Bahasa" -> {
                item{Text(MR.strings.set_language_note.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant)}
                val options=listOf("system" to MR.strings.set_language_follow_device.text())+com.logikawan.finflow.core.i18n.appLanguages.map{it.tag to it.nativeName}
                items(options,key={it.first}){(tag,name)->
                    val selected=p.language==tag
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable{update(p.copy(language=tag))}.padding(horizontal=12.dp,vertical=14.dp).testTag("language-$tag"),verticalAlignment=Alignment.CenterVertically){
                        Text(name,Modifier.weight(1f),style=FFType.bodyMain,fontWeight=if(selected)FontWeight.SemiBold else FontWeight.Normal,color=FFColors.onSurface)
                        if(selected)FFIcon("hi_check",FFColors.onSurface,20.dp)
                    }
                }
            }
            "Tema" -> {
                item{Choice(MR.strings.set_pref_preset_mode.text(),themeModePresetOf(p.theme,p.palette),themeModePresets.map{it.id to it.label},{id->themeModePresets.firstOrNull{it.id==id}?.let{update(p.copy(theme=it.theme,palette=it.palette))}})}
                item{Choice(MR.strings.set_pref_display_mode.text(),p.theme,listOf("system" to MR.strings.set_pref_theme_system.text(),"light" to MR.strings.set_pref_theme_light.text(),"dark" to MR.strings.set_pref_theme_dark.text()),{update(p.copy(theme=it))})}
                item{Choice(MR.strings.set_pref_color_palette.text(),p.palette,listOf("neutral" to "Neutral","arctic" to "Arctic Blue","ruby" to "Ruby","emerald" to "Emerald","indigo" to "Indigo"),{update(p.copy(palette=it))})}
            }
            "Tampilan Kartu" -> {
                item{Choice(MR.strings.set_pref_wallet_card_style.text(),p.cardAppearance,listOf("gradient" to MR.strings.set_pref_card_gradient.text(),"plain" to MR.strings.set_pref_card_plain.text(),"outline" to MR.strings.set_pref_card_outline.text()),{update(p.copy(cardAppearance=it))})}
                item{Choice(MR.strings.set_pref_balance_card_gradient.text(),p.cardGradient,cardGradientPresets.map{it.id to it.label}){update(p.copy(cardGradient=it))}}
            }
            "Tampilan Menu" -> {
                item{Choice(MR.strings.set_pref_home_menu_layout.text(),p.menuAppearance,listOf("horizontal" to MR.strings.set_pref_menu_horizontal.text(),"grid" to MR.strings.set_pref_menu_grid.text(),"list" to MR.strings.set_pref_menu_list.text()),{update(p.copy(menuAppearance=it))})}
                item{Choice(MR.strings.set_pref_menu_icon_color.text(),p.menuTheme,menuThemePresets.map{it.id to it.label}){update(p.copy(menuTheme=it))}}
            }
            "Palet Transaksi" -> {
                item{Choice(transactionTypeLabel("income"),p.incomeColor.toString(),listOf(0xFF4CAF50L to MR.strings.set_color_green.text(),0xFF009688L to MR.strings.set_color_teal.text(),0xFF2196F3L to MR.strings.set_color_blue.text()).map{it.first.toString() to it.second},{update(p.copy(incomeColor=it.toLong()))})}
                item{Choice(transactionTypeLabel("expense"),p.expenseColor.toString(),listOf(0xFFFF5252L to MR.strings.set_color_red.text(),0xFFFF9800L to MR.strings.set_color_orange.text(),0xFF9C27B0L to MR.strings.set_color_purple.text()).map{it.first.toString() to it.second},{update(p.copy(expenseColor=it.toLong()))})}
            }
            "Tampilan Beranda" -> item{Choice(MR.strings.set_pref_home_period.text(),p.homePeriod,listOf("Hari Ini","Minggu Ini","Bulan Ini","Tahun","Kalender").map{it to homePeriodShortLabel(it)},{update(p.copy(homePeriod=it))})}
            "Tampilan Tambah Transaksi" -> {
                item{Choice(MR.strings.set_pref_tx_density.text(),p.transactionDisplay,listOf("comfortable" to MR.strings.set_pref_comfortable.text(),"compact" to MR.strings.set_pref_compact.text()),{update(p.copy(transactionDisplay=it))})}
                item{Choice(MR.strings.set_pref_add_button.text(),p.addButtonMode,listOf("quick" to MR.strings.set_pref_add_quick.text(),"all" to MR.strings.set_pref_add_all.text()),{update(p.copy(addButtonMode=it))})}
            }
            "Paket Ikon" -> item{Choice(MR.strings.set_pref_icon_style.text(),p.iconPack,listOf("outlined" to MR.strings.set_pref_icon_outlined.text(),"rounded" to MR.strings.set_pref_icon_rounded.text()),{update(p.copy(iconPack=it))})}
            "Format Saldo" -> {
                item{Text(formatBalance(123456789,"IDR",p),style=FFType.amountLg)}
                item{PreferenceSwitch(MR.strings.set_pref_show_decimals.text(),p.showDecimals){update(p.copy(showDecimals=it))}}
                item{PreferenceSwitch(MR.strings.set_pref_currency_symbol.text(),p.showCurrencySymbol){update(p.copy(showCurrencySymbol=it))}}
                item{PreferenceSwitch(MR.strings.set_pref_hide_balance.text(),p.hideBalances){update(p.copy(hideBalances=it))}}
                item{Choice(MR.strings.set_pref_number_separator.text(),p.numberFormat,listOf("id" to "1.234.567,89","en" to "1,234,567.89"),{update(p.copy(numberFormat=it))})}
            }
            "Periode Pencatatan" -> {
                item{Text(MR.strings.set_pref_budget_start_day.text(p.recordingDay),style=FFType.sectionTitle);Slider(p.recordingDay.toFloat(),{update(p.copy(recordingDay=it.toInt().coerceIn(1,31)))},valueRange=1f..31f,steps=29)}
                item{Text(MR.strings.set_pref_short_month_hint.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant)}
                item{Choice(MR.strings.set_pref_week_start.text(),p.weekStart.toString(),listOf("1" to MR.strings.set_day_monday.text(),"2" to MR.strings.set_day_tuesday.text(),"3" to MR.strings.set_day_wednesday.text(),"4" to MR.strings.set_day_thursday.text(),"5" to MR.strings.set_day_friday.text(),"6" to MR.strings.set_day_saturday.text(),"7" to MR.strings.set_day_sunday.text()),{update(p.copy(weekStart=it.toInt()))})}
            }
            "Memori Pencatatan" -> {
                item{Choice(MR.strings.set_pref_default_wallet.text(),p.defaultWalletId,listOf("" to MR.strings.set_pref_first_active_wallet.text())+data.accounts.filterNot{it.archived || it.type=="savings"}.map{it.id to it.name},{update(p.copy(defaultWalletId=it))})}
                item{PreferenceSwitch(MR.strings.set_pref_remember_category.text(),p.rememberCategory){update(p.copy(rememberCategory=it))}}
            }
        }
        error?.let{item{Text(it,color=MaterialTheme.colorScheme.error)}}
        item{DesignCard {Text(MR.strings.set_pref_preview.text(),style=FFType.sectionTitle);Text(MR.strings.set_pref_preview_body.text(),style=FFType.metaData);Spacer(Modifier.height(12.dp));Row{Text(transactionTypeLabel("income"),Modifier.weight(1f),color=FFColors.income);Text(transactionTypeLabel("expense"),color=FFColors.expense)};Text(formatBalance(1234500,"IDR",p),style=FFType.amountMd)}}
    }
}

@Composable private fun PreferenceSwitch(title:String,value:Boolean,change:(Boolean)->Unit) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(title,Modifier.weight(1f),style=FFType.bodyMain);Switch(value,change)}
}

/** Label pendek periode Beranda (tanpa akhiran " Ini"); nilai tersimpan tetap kunci Indonesia. */
private fun homePeriodShortLabel(id:String):String=when(id){
    "Hari Ini"->MR.strings.set_period_day.text()
    "Minggu Ini"->MR.strings.set_period_week.text()
    "Bulan Ini"->MR.strings.set_period_month.text()
    else->periodLabel(id)
}
