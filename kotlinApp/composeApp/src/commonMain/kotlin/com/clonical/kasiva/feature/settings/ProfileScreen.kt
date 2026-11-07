package com.clonical.kasiva.feature.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonical.kasiva.core.model.*
import com.clonical.kasiva.core.platform.NativeServices
import com.clonical.kasiva.core.ui.*
import com.clonical.kasiva.core.i18n.tabLabel
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.i18n.*
import dev.icerock.moko.resources.StringResource

/*
 * Tab Profil mengikuti susunan Ollo 1.7.9: bilah atas (gear · judul · cari) → kepala profil → kartu Premium →
 * Akun & Data → Preferensi → AI → Fitur Masa Mendatang → Umum → Lainnya → versi.
 * key = id stabil berbahasa Indonesia (kunci LazyColumn); route = id halaman yang ditangani App.kt.
 */
private data class ProfileItem(val key:String,val title:StringResource,val caption:StringResource,val icon:String,val route:String,val danger:Boolean=false)
private data class ProfileSection(val id:String,val caption:StringResource,val tiles:List<ProfileItem> = emptyList(),val rows:List<ProfileItem> = emptyList(),val palette:Boolean=false,val searchOnly:Boolean=false)

private val profileSections=listOf(
    ProfileSection("account",MR.strings.set_profile_section_account,
        tiles=listOf(
            ProfileItem("Manajemen Akun",MR.strings.set_page_account_mgmt,MR.strings.pe_account_mgmt_caption,"hi_user","Manajemen Akun"),
            ProfileItem("Cadangan & Pemulihan",MR.strings.set_page_backup,MR.strings.pe_backup_caption,"hi_cloud_upload","Cadangan & Pemulihan"),
            ProfileItem("Perangkat Terhubung",MR.strings.set_page_devices,MR.strings.pe_devices_caption,"hi_laptop","Perangkat Terhubung"),
            ProfileItem("Ekspor Data",MR.strings.set_page_export,MR.strings.set_profile_export_caption,"hi_download","Ekspor Data"),
        ),
        rows=listOf(
            ProfileItem("Impor Data",MR.strings.set_profile_import,MR.strings.set_profile_import_caption,"hi_upload","Impor data"),
            ProfileItem("Periode Pencatatan",MR.strings.set_page_recording_period,MR.strings.pe_recording_period_caption,"hi_calendar","Periode Pencatatan"),
            ProfileItem("Kategori",MR.strings.set_profile_category,MR.strings.pe_categories_caption,"hi_tags","Kategori"),
            ProfileItem("Pengaturan Dompet",MR.strings.set_profile_wallet_settings,MR.strings.set_profile_wallet_settings_caption,"hi_wallet","Pengaturan Dompet"),
            ProfileItem("Notifikasi",MR.strings.set_profile_notifications,MR.strings.set_profile_notifications_caption,"hi_bell","Notifikasi"),
        )),
    ProfileSection("preferences",MR.strings.set_profile_section_preferences,palette=true,
        tiles=listOf(
            ProfileItem("Tema",MR.strings.set_page_theme,MR.strings.set_profile_theme_caption,"hi_moon","Tema"),
            ProfileItem("Tampilan Kartu",MR.strings.set_page_card_display,MR.strings.pe_card_caption,"hi_card","Tampilan Kartu"),
            ProfileItem("Tampilan Menu",MR.strings.set_page_menu_display,MR.strings.pe_menu_caption,"hi_layout","Tampilan Menu"),
            ProfileItem("Palet Transaksi",MR.strings.set_page_tx_palette,MR.strings.pe_tx_palette_caption,"hi_paint","Palet Transaksi"),
        ),
        rows=listOf(
            ProfileItem("Urutan Statistik",MR.strings.set_page_stats_order,MR.strings.set_profile_stats_order_caption,"hi_sort","Urutan Statistik"),
            ProfileItem("Tampilan Beranda",MR.strings.set_page_home_display,MR.strings.pe_home_view_caption,"hi_calendar_doc","Tampilan Beranda"),
            ProfileItem("Tampilan Tambah Transaksi",MR.strings.set_page_add_tx_display,MR.strings.pe_add_tx_caption,"hi_add_circle","Tampilan Tambah Transaksi"),
            ProfileItem("Paket Ikon",MR.strings.set_page_icon_pack,MR.strings.set_profile_icon_pack_caption,"hi_grid_view","Paket Ikon"),
            ProfileItem("Format Saldo",MR.strings.set_page_balance_format,MR.strings.pe_balance_caption,"hi_coins","Format Saldo"),
        )),
    ProfileSection("ai",MR.strings.pe_section_ai,rows=listOf(
        ProfileItem("Asisten",MR.strings.pe_assistant,MR.strings.pe_assistant_caption,"hi_chat","Asisten"),
        ProfileItem("Pengaturan Asisten",MR.strings.app_page_assistant_settings,MR.strings.pe_assistant_settings_caption,"hi_settings","Pengaturan Asisten"),
        ProfileItem("Pengaturan AI Scan",MR.strings.app_page_ai_scan_settings,MR.strings.pe_ai_scan_caption,"hi_scan","Pengaturan AI Scan"),
        ProfileItem("Memori Pencatatan",MR.strings.set_page_recording_memory,MR.strings.pe_record_memory_caption,"hi_brain","Memori Pencatatan"),
        ProfileItem("Bahasa Input Suara",MR.strings.pe_voice_language,MR.strings.pe_voice_language_caption,"hi_voice","Bahasa Input Suara"),
    )),
    ProfileSection("future",MR.strings.set_profile_section_future,rows=listOf(
        ProfileItem("Otomasi",MR.strings.pe_automation,MR.strings.pe_automation_caption,"hi_rocket","Otomasi"),
        ProfileItem("Masukan & Peta Jalan",MR.strings.set_profile_roadmap,MR.strings.pe_roadmap_caption,"hi_flag","Masukan & Peta Jalan"),
    )),
    ProfileSection("general",MR.strings.pe_section_general,rows=listOf(
        ProfileItem("Pengaturan",MR.strings.set_settings,MR.strings.pe_settings_caption,"hi_settings","Pengaturan"),
        ProfileItem("Bantuan",MR.strings.pe_help,MR.strings.pe_help_caption,"hi_help","Bantuan"),
        ProfileItem("Kirim Masukan",MR.strings.set_profile_send_feedback,MR.strings.pe_send_feedback_caption,"hi_mail","Masukan"),
        ProfileItem("Bagikan",MR.strings.pe_share,MR.strings.pe_share_caption,"hi_share","Bagikan"),
        ProfileItem("Laporkan Bug",MR.strings.set_page_bug_report,MR.strings.pe_bug_caption,"hi_bug","Laporkan Bug"),
        ProfileItem("Tentang",MR.strings.pe_about,MR.strings.pe_about_caption,"hi_info","Tentang"),
        ProfileItem("Catatan Pembaruan",MR.strings.set_page_changelog,MR.strings.pe_changelog_caption,"hi_clock","Catatan Pembaruan"),
    )),
    ProfileSection("other",MR.strings.pe_section_other,rows=listOf(
        ProfileItem("Hapus Data",MR.strings.set_profile_delete_data,MR.strings.set_profile_delete_data_caption,"hi_delete","Hapus Data",danger=true),
        ProfileItem("Keluar Aplikasi",MR.strings.pe_exit,MR.strings.pe_exit_caption,"hi_logout","Keluar Aplikasi",danger=true),
    )),
    // Isi halaman Pengaturan (Ollo menaruh Bahasa & Keamanan di sana) ikut dicari dari tab Profil.
    ProfileSection("settings-search",MR.strings.set_settings,searchOnly=true,rows=listOf(
        ProfileItem("Mata Uang",MR.strings.pe_set_currency,MR.strings.set_profile_base_currency,"hi_reimburse","Pengaturan"),
        ProfileItem("Kurs",MR.strings.pe_set_exchange_rates,MR.strings.pe_set_exchange_rates_caption,"hi_trend_up","Kurs"),
        ProfileItem("Bahasa",MR.strings.set_page_language,MR.strings.set_profile_language_caption,"hi_language","Bahasa"),
        ProfileItem("Keamanan",MR.strings.set_profile_security,MR.strings.pe_set_security_caption,"hi_shield","Pengaturan"),
        ProfileItem("Notifikasi Bank/E-Wallet",MR.strings.set_profile_bank_notifications,MR.strings.set_profile_bank_notifications_caption,"hi_bank","Impor notifikasi"),
    )),
)

/** Nilai preferensi palet + label + warna pratinjau (bilah, tiga kotak, tiga titik) seperti chip palet Ollo. */
private data class PaletteOption(val value:String,val label:StringResource,val bar:Long,val blocks:List<Long>,val dots:List<Long>)
private val paletteOptions=listOf(
    PaletteOption("neutral",MR.strings.pe_palette_neutral,0xFF1C1E27,listOf(0xFF1C1E27,0xFF1C1E27,0xFF1C1E27),listOf(0xFF1C1E27,0xFF1C1E27,0xFF1C1E27)),
    PaletteOption("arctic",MR.strings.pe_palette_arctic,0xFF8DCFF0,listOf(0xFF38BDF8,0xFF3B82F6,0xFF2DD4BF),listOf(0xFF14B8A6,0xFF4F5FBF,0xFF94A3B8)),
    PaletteOption("ruby",MR.strings.pe_palette_ruby,0xFFC62828,listOf(0xFFE57373,0xFFEF5350,0xFFF44336),listOf(0xFFFF8A80,0xFFE53935,0xFFC62828)),
    PaletteOption("emerald",MR.strings.pe_palette_emerald,0xFF2E7D32,listOf(0xFF81C784,0xFF66BB6A,0xFF4CAF50),listOf(0xFFA5D6A7,0xFF43A047,0xFF1B5E20)),
    PaletteOption("indigo",MR.strings.pe_palette_indigo,0xFF3949AB,listOf(0xFF64B5F6,0xFF42A5F5,0xFF5C6BC0),listOf(0xFF9FA8DA,0xFF3F51B5,0xFF283593)),
)

@Composable
fun ProfileScreen(native:NativeServices,name:String,email:String,data:FinanceData,onNavigate:(String)->Unit,logout:()->Unit,localMode:Boolean=false,premiumCard:@Composable ()->Unit={}) {
    var search by remember{mutableStateOf(false)}
    var query by remember{mutableStateOf("")}
    val p=LocalPreferences.current
    val store=LocalPreferenceState.current
    val progress=recordingProgress(data)
    val keyword=query.trim()
    val searching=keyword.isNotEmpty()
    val matches:(ProfileItem)->Boolean={!searching || "${it.title.text()} ${it.caption.text()}".contains(keyword,true)}
    val version=remember(native){native.appVersion()}
    LazyColumn(Modifier.fillMaxSize().testTag("profile-list").background(FFColors.background).statusBarsPadding(),contentPadding=PaddingValues(start=16.dp,end=16.dp,top=4.dp,bottom=124.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        item(key="top-bar"){
            Column{
                Row(Modifier.fillMaxWidth().height(56.dp),verticalAlignment=Alignment.CenterVertically){
                    IconButton({onNavigate("Pengaturan")},Modifier.testTag("profile-settings")){FFIcon("hi_settings",FFColors.onSurface,26.dp,contentDescription=MR.strings.set_settings.text())}
                    Text(tabLabel("Profil"),Modifier.weight(1f),style=FFType.appTitle,textAlign=TextAlign.Center)
                    IconButton({search=!search;if(!search)query=""},Modifier.testTag("profile-search")){FFIcon("hi_search",FFColors.onSurface,26.dp,contentDescription=MR.strings.set_profile_search.text())}
                }
                if(search)ProfileSearchField(query){query=it}
            }
        }
        if(!searching){
            item(key="header"){ProfileHeader(native,name,data,progress.level,progress.streak,progress.activeDays,onNavigate)}
            item(key="premium"){premiumCard()}
        }
        var any=false
        profileSections.forEach{section->
            if(section.searchOnly && !searching)return@forEach
            val tiles=section.tiles.filter(matches)
            val rows=section.rows.filter(matches)
            val palette=section.palette && !searching
            if(tiles.isEmpty() && rows.isEmpty() && !palette)return@forEach
            any=true
            item(key="section-${section.id}"){SectionCaption(section.caption.text())}
            if(palette)item(key="palette"){PaletteRow(p.palette){value->store?.update(p.copy(palette=value))}}
            tiles.chunked(2).forEach{row->
                item(key="tiles-${row.first().key}"){
                    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){row.forEach{ProfileTile(it,Modifier.weight(1f),onNavigate)};if(row.size==1)Spacer(Modifier.weight(1f))}
                }
            }
            items(rows,key={"${section.id}-${it.key}"}){ProfileRow(it,onNavigate)}
        }
        if(searching && !any)item(key="empty"){Text(MR.strings.pe_no_results.text(),Modifier.fillMaxWidth().padding(top=32.dp),style=FFType.bodyMain,color=FFColors.onSurfaceVariant,textAlign=TextAlign.Center)}
        if(!searching)item(key="version"){
            Column(Modifier.fillMaxWidth().padding(top=22.dp).testTag("profile-version"),horizontalAlignment=Alignment.CenterHorizontally){
                Text(MR.strings.pe_version.text(version),style=FFType.bodyMain.copy(fontSize=13.5.sp,lineHeight=18.sp),color=FFColors.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(MR.strings.pe_by_logikawan.text(),style=FFType.metaData.copy(fontSize=11.sp,lineHeight=14.sp),color=FFColors.onSurfaceVariant.copy(alpha=.6f))
            }
        }
    }
}

@Composable private fun ProfileSearchField(value:String,change:(String)->Unit){
    val shape=RoundedCornerShape(24.dp)
    Row(Modifier.fillMaxWidth().padding(top=4.dp,bottom=6.dp).height(48.dp).clip(shape).background(FFColors.surface).border(1.dp,FFColors.hairline,shape).padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically){
        FFIcon("hi_search",FFColors.onSurfaceVariant,18.dp)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)){
            if(value.isEmpty())Text(MR.strings.pe_search_placeholder.text(),style=FFType.bodyMain,color=FFColors.onSurfaceVariant)
            BasicTextField(value,change,Modifier.fillMaxWidth().testTag("profile-search-field"),singleLine=true,textStyle=FFType.bodyMain.copy(color=FFColors.onSurface),cursorBrush=SolidColor(FFColors.onSurface))
        }
    }
}

@Composable
private fun PaletteRow(selected:String,select:(String)->Unit){
    LazyRow(Modifier.testTag("profile-palettes"),horizontalArrangement=Arrangement.spacedBy(9.dp)){
        items(paletteOptions,key={it.value}){PaletteTile(it,it.value==selected){select(it.value)}}
    }
}

// Chip palet Ollo: pratinjau mini-layar terang (gelap di mode gelap) berisi bilah aksen, kotak warna dan titik.
@Composable private fun PaletteTile(option:PaletteOption,selected:Boolean,select:()->Unit){
    val shape=RoundedCornerShape(16.dp)
    // Palet "neutral" memakai warna tinta tema agar tetap terlihat di mode gelap.
    val ink=FFColors.onSurface
    fun tone(c:Long)=if(option.value=="neutral")ink else Color(c)
    Column(Modifier.size(75.dp,73.dp).clip(shape).background(FFColors.surface).border(if(selected)2.dp else 1.dp,if(selected)FFColors.onSurface else FFColors.hairline,shape)
        .clickable(onClick=select).padding(top=7.dp,bottom=5.dp).testTag("palette-${option.value}"),horizontalAlignment=Alignment.CenterHorizontally){
        val inner=RoundedCornerShape(10.dp)
        Column(Modifier.size(47.dp,44.dp).clip(inner).background(FFColors.background).border(1.dp,FFColors.hairline,inner).padding(horizontal=5.dp,vertical=5.dp),verticalArrangement=Arrangement.SpaceBetween){
            Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(4.dp)).background(tone(option.bar))){
                Box(Modifier.padding(start=4.dp).align(Alignment.CenterStart).size(12.dp,2.dp).clip(CircleShape).background(Color.White.copy(alpha=.85f)))
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){option.blocks.forEach{Box(Modifier.size(10.dp,9.dp).clip(RoundedCornerShape(3.dp)).background(tone(it)))}}
            Row(Modifier.fillMaxWidth().padding(horizontal=2.dp),horizontalArrangement=Arrangement.SpaceBetween){option.dots.forEach{Box(Modifier.size(4.dp).clip(CircleShape).background(tone(it)))}}
        }
        Spacer(Modifier.weight(1f))
        Text(option.label.text(),style=FFType.metaData.copy(fontSize=11.5.sp,lineHeight=14.sp),color=FFColors.onSurface,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}

@Composable
private fun ProfileHeader(native:NativeServices,name:String,data:FinanceData,level:Int,streak:Int,days:Int,onNavigate:(String)->Unit) {
    val dark=FFColors.isDark
    Row(Modifier.fillMaxWidth().testTag("profile-header").padding(top=10.dp,bottom=4.dp),verticalAlignment=Alignment.CenterVertically) {
        val editLabel=MR.strings.pe_edit_profile.text()
        Box(Modifier.testTag("profile-avatar").clip(CircleShape).clickable{onNavigate("Profil akun")}.semantics{contentDescription=editLabel}) {
            ProfilePhoto(data.profile,native,Modifier.size(78.dp).clip(CircleShape).background(if(dark)FFColors.chip else FFColors.surface).border(1.5.dp,if(dark)FFColors.hairline else FFColors.outlineVariant,CircleShape))
            Box(Modifier.align(Alignment.BottomEnd).offset(x=2.dp,y=2.dp).size(23.dp).clip(CircleShape).background(FFColors.pill),contentAlignment=Alignment.Center){
                FFIcon("hi_pencil",FFColors.onPill,12.dp)
            }
        }
        Spacer(Modifier.width(20.dp))
        Column(Modifier.weight(1f)) {
            Text(name,style=FFType.appTitle.copy(fontSize=24.sp,lineHeight=30.sp,fontWeight=FontWeight.Medium),maxLines=1,overflow=TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            val levelLabel=MR.strings.pe_open_level.text()
            Row(Modifier.fillMaxWidth().clickable{onNavigate("Aktivitas")}.semantics{contentDescription=levelLabel}.testTag("profile-level-link"),verticalAlignment=Alignment.CenterVertically) {
                ProfileCounter(MR.strings.set_profile_level.text(),level.toString())
                ProfileDivider()
                ProfileCounter(MR.strings.set_profile_streak.text(),streak.toString())
                ProfileDivider()
                ProfileCounter(MR.strings.set_profile_active_days.text(),days.toString())
                Spacer(Modifier.weight(1f))
                FFIcon("hi_chevron_right",FFColors.onSurfaceVariant,20.dp)
            }
        }
    }
}
@Composable private fun ProfileDivider(){Box(Modifier.padding(horizontal=14.dp).width(1.dp).height(30.dp).background(FFColors.outlineVariant))}

@Composable private fun SectionCaption(label:String){Text(label,Modifier.padding(start=5.dp,top=16.dp,bottom=2.dp),style=FFType.metaData.copy(fontSize=13.5.sp,lineHeight=18.sp,letterSpacing=.8.sp),color=FFColors.onSurfaceVariant.copy(alpha=.85f))}

fun recordingStreak(data:FinanceData):Int {
    return recordingProgress(data).streak
}
@Composable private fun ProfileCounter(label:String,value:String){Column{Text(value,style=FFType.bodyMain.copy(fontSize=17.sp,lineHeight=22.sp,fontWeight=FontWeight.Medium));Text(label,style=FFType.metaData.copy(fontSize=11.5.sp,lineHeight=14.sp),color=FFColors.onSurfaceVariant,maxLines=1)}}
@Composable private fun ProfileIconBox(icon:String,circle:Boolean,danger:Boolean=false){
    val error=MaterialTheme.colorScheme.error
    val bg=when{danger->error.copy(alpha=.12f);FFColors.isDark->FFColors.hairline;else->FFColors.surfaceContainerHigh}
    Box(Modifier.size(if(circle)40.dp else 38.dp).clip(if(circle)CircleShape else RoundedCornerShape(12.dp)).background(bg),contentAlignment=Alignment.Center){FFIcon(icon,if(danger)error else FFColors.onSurface,20.dp)}
}
private fun itemTag(item:ProfileItem)="profile-item-"+item.key.lowercase().replace(Regex("[^a-z0-9]+"),"-").trim('-')
@Composable private fun ProfileTile(item:ProfileItem,modifier:Modifier,navigate:(String)->Unit){
    Column(modifier.height(115.dp).clip(RoundedCornerShape(20.dp)).background(FFColors.surface).clickable{navigate(item.route)}.testTag(itemTag(item)).padding(start=14.dp,end=14.dp,top=12.dp,bottom=8.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){ProfileIconBox(item.icon,false);Spacer(Modifier.weight(1f));FFIcon("hi_chevron_right",FFColors.onSurfaceVariant,18.dp)}
        Spacer(Modifier.height(6.dp))
        FitMoneyText(item.title.text(),FFType.bodyMain.copy(fontSize=17.sp,lineHeight=21.sp,fontWeight=FontWeight.Medium))
        Text(item.caption.text(),style=FFType.metaData.copy(fontSize=13.sp,lineHeight=14.5.sp),color=FFColors.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)
    }
}
@Composable private fun ProfileRow(item:ProfileItem,navigate:(String)->Unit){
    val error=MaterialTheme.colorScheme.error
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(FFColors.surface).clickable{navigate(item.route)}.testTag(itemTag(item)).padding(horizontal=16.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically){
        ProfileIconBox(item.icon,true,item.danger)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)){
            Text(item.title.text(),style=FFType.bodyMain.copy(fontSize=18.sp,lineHeight=23.sp,fontWeight=FontWeight.Medium),color=if(item.danger)error else FFColors.onSurface,maxLines=1,overflow=TextOverflow.Ellipsis)
            Text(item.caption.text(),style=FFType.metaData.copy(fontSize=13.5.sp,lineHeight=18.sp),color=FFColors.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)
        }
        FFIcon("hi_chevron_right",FFColors.onSurfaceVariant,20.dp)
    }
}
