package com.logikawan.finflow.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logikawan.finflow.core.data.ConnectedDevice
import com.logikawan.finflow.core.model.*
import com.logikawan.finflow.core.platform.NativeServices
import com.logikawan.finflow.core.ui.*
import com.logikawan.finflow.core.util.*
import com.logikawan.finflow.feature.ai.AiController
import com.logikawan.finflow.feature.importdata.CsvImport
import com.logikawan.finflow.feature.level.LevelScreen
import com.logikawan.finflow.core.i18n.text
import com.logikawan.finflow.core.i18n.transactionTypeLabel
import com.logikawan.finflow.i18n.*
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

val accountDataPages=setOf("Manajemen Akun","Cadangan & Pemulihan","Perangkat Terhubung","Ekspor Data","Aktivitas","Bantuan","Masukan","Laporkan Bug","Tentang","Catatan Pembaruan","Hapus Data")

/** Label tampilan untuk kunci halaman di [accountDataPages] (kunci navigasi tetap berbahasa Indonesia). */
fun accountDataPageLabel(page:String):String=when(page){
    "Manajemen Akun"->MR.strings.set_page_account_mgmt.text()
    "Cadangan & Pemulihan"->MR.strings.set_page_backup.text()
    "Perangkat Terhubung"->MR.strings.set_page_devices.text()
    "Ekspor Data"->MR.strings.set_page_export.text()
    "Aktivitas"->MR.strings.set_page_activity.text()
    "Bantuan"->MR.strings.set_page_help.text()
    "Masukan"->MR.strings.set_page_feedback.text()
    "Laporkan Bug"->MR.strings.set_page_bug_report.text()
    "Tentang"->MR.strings.set_page_about.text()
    "Catatan Pembaruan"->MR.strings.set_page_changelog.text()
    "Hapus Data"->MR.strings.set_profile_delete_data.text()
    else->page
}

private const val PLAY_STORE_LINK = "https://play.google.com/store/apps/details?id=com.logikawan.finflow"

/** Panduan (tab Guide di Bantuan) — topik FinFlow yang sudah ada. */
private val guideTopics=listOf(
    MR.strings.set_help_record_title to MR.strings.set_help_record_body,
    MR.strings.set_help_balance_title to MR.strings.set_help_balance_body,
    MR.strings.set_help_goals_title to MR.strings.set_help_goals_body,
    MR.strings.set_help_bundle_title to MR.strings.set_help_bundle_body,
    MR.strings.set_help_fix_title to MR.strings.set_help_fix_body,
    MR.strings.set_help_bank_title to MR.strings.set_help_bank_body,
    MR.strings.set_help_about_title to MR.strings.set_help_about_body,
)

/** FAQ seperti Ollo, jawabannya sesuai fitur FinFlow. */
private val faqTopics=listOf(
    MR.strings.pa_faq_wallet_q to MR.strings.pa_faq_wallet_a,
    MR.strings.pa_faq_export_q to MR.strings.pa_faq_export_a,
    MR.strings.pa_faq_delete_q to MR.strings.pa_faq_delete_a,
    MR.strings.pa_faq_secure_q to MR.strings.pa_faq_secure_a,
    MR.strings.pa_faq_quick_q to MR.strings.pa_faq_quick_a,
    MR.strings.pa_faq_scan_q to MR.strings.pa_faq_scan_a,
    MR.strings.pa_faq_scan_safe_q to MR.strings.pa_faq_scan_safe_a,
    MR.strings.pa_faq_widget_q to MR.strings.pa_faq_widget_a,
)

/**
 * Halaman Akun & Data. Parameter baru (semuanya opsional agar App lama tetap terkompilasi):
 * - [ai]: bila diisi, Manajemen Akun menggambar Status Akun/Batas AI/Privasi AI bergaya Ollo (accountExtras diabaikan).
 * - [signOut]: tombol Keluar di Status Akun (mode cloud).
 * - [factoryReset]/[deleteTransactions]: aksi halaman "Hapus Data".
 * - [saveToDownloads]: (nama, isi, mime) -> pesan gagal atau null; tombol "Simpan ke Unduhan" hanya tampil bila tersedia.
 * - [cloudBackup]/[autoBackup]: baris Google Drive & cadangan otomatis hanya tampil bila tersedia.
 * - [devices]/[currentDeviceId]/[removeDevice]: daftar Perangkat Terhubung (cloud); [removeDevice] null = tanpa tombol keluarkan.
 */
@Composable
fun AccountDataScreen(page:String,data:FinanceData,native:NativeServices,email:String,navigate:(String)->Unit,
    reload:()->Unit,backup:()->Unit,chooseRestore:()->Unit,busy:Boolean=false,changePhoto:()->Unit={},deleteAccount:()->Unit={},localMode:Boolean=false,connect:()->Unit={},useLocal:()->Unit={},back:()->Unit={},accountExtras:LazyListScope.()->Unit={},
    ai:AiController?=null,signOut:(()->Unit)?=null,factoryReset:()->Unit={},deleteTransactions:(()->Unit)?=null,
    saveToDownloads:((String,String,String)->String?)?=null,cloudBackup:(()->Unit)?=null,autoBackup:(()->Unit)?=null,
    devices:List<ConnectedDevice> = emptyList(),currentDeviceId:String?=null,removeDevice:((String)->Unit)?=null) {
    when(page){
        "Aktivitas"->LevelScreen(data,native,back){ navigate(it) }
        "Manajemen Akun"->AccountManagementPage(data,native,email,localMode,busy,ai,
            AccountManagementActions(navigate,reload,backup,changePhoto,deleteAccount,connect,useLocal,signOut),accountExtras)
        "Hapus Data"->AccountDataDeleteScreen(native,email,localMode,busy,factoryReset,deleteTransactions)
        "Ekspor Data"->ExportDataPage(data,native,saveToDownloads)
        "Bantuan"->HelpPage(native,email)
        "Laporkan Bug"->PageColumn{BugReportForm(native)}
        "Masukan"->PageColumn{SupportForm(page,native)}
        "Cadangan & Pemulihan"->BackupPage(data,busy,localMode,email,backup,chooseRestore,connect,{navigate("Impor data")},cloudBackup,autoBackup)
        "Perangkat Terhubung"->DevicesPage(native,email,localMode,devices,currentDeviceId,busy,removeDevice)
        "Tentang"->AboutPage(native)
        "Catatan Pembaruan"->ChangelogPage(native)
    }
}

@Composable
private fun PageColumn(horizontal:androidx.compose.ui.unit.Dp=20.dp,spacing:androidx.compose.ui.unit.Dp=16.dp,content:@Composable ColumnScope.()->Unit){
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("account-data-page").padding(horizontal=horizontal,vertical=20.dp),
        verticalArrangement=Arrangement.spacedBy(spacing),content=content)
}

// ---------- Cadangan & Pemulihan ----------

@Composable
private fun BackupPage(data:FinanceData,busy:Boolean,localMode:Boolean,email:String,backup:()->Unit,chooseRestore:()->Unit,connect:()->Unit,importCsv:()->Unit,
    cloudBackup:(()->Unit)?,autoBackup:(()->Unit)?) {
    var dialog by remember{mutableStateOf<String?>(null)}
    var message by remember{mutableStateOf<String?>(null)}
    TopBarActions{ FFIconButton("hi_help",{dialog="help"},tint=FFColors.onSurface) }
    PageColumn(horizontal=24.dp){
        AdBody(MR.strings.pa_backup_intro.text(),size=16.sp)
        Spacer(Modifier.height(4.dp))
        BackupRow("hi_download",FFColors.iconCircle,FFColors.onSurface,MR.strings.pa_backup_create.text(),MR.strings.pa_backup_create_sub.text(),
            enabled=data.featuresReady && !busy,tag="backup-create"){try{backup()}catch(e:Exception){message=e.message ?: MR.strings.set_export_failed.text()}}
        BackupRow(AdIcons.undo,AdTone.orangeBg,AdTone.orange,MR.strings.pa_backup_restore.text(),MR.strings.pa_backup_restore_sub.text(),
            titleColor=AdTone.danger,enabled=data.featuresReady && !busy,tag="backup-restore",onClick=chooseRestore)
        BackupRow(AdIcons.cloudSync,AdTone.blueBg,AdTone.blue,MR.strings.ai_cloud_sync.text(),MR.strings.pa_backup_sync_sub.text(),tag="backup-cloud-sync"){
            if(localMode)connect() else dialog="sync"
        }
        cloudBackup?.let{BackupRow(AdIcons.cloudSync,AdTone.blueBg,AdTone.blue,MR.strings.pa_backup_drive.text(),MR.strings.pa_backup_drive_sub.text(),enabled=!busy,tag="backup-drive",onClick=it)}
        autoBackup?.let{BackupRow(AdIcons.archive,FFColors.iconCircle,FFColors.onSurface,MR.strings.pa_backup_auto.text(),MR.strings.pa_backup_auto_sub.text(),enabled=!busy,tag="backup-auto",onClick=it)}
        BackupRow("hi_upload",FFColors.iconCircle,FFColors.onSurface,MR.strings.set_backup_import_csv.text(),MR.strings.set_profile_import_caption.text(),tag="backup-import-csv",onClick=importCsv)
        message?.let{AdBody(it,color=AdTone.danger)}
    }
    when(dialog){
        "help"->AdInfoDialog(MR.strings.set_backup_full.text(),listOf(MR.strings.set_backup_full_body.text(),MR.strings.set_backup_restore_hint.text(),MR.strings.set_backup_private_hint.text(),MR.strings.set_csv_not_backup.text()),{dialog=null})
        "sync"->AdInfoDialog(MR.strings.pa_sync_active_title.text(),listOf(MR.strings.set_devices_this.text(email),MR.strings.set_acct_cloud_desc.text()),{dialog=null})
    }
}

@Composable
private fun BackupRow(icon:String,circle:Color,tint:Color,title:String,subtitle:String,titleColor:Color=FFColors.onSurface,enabled:Boolean=true,tag:String,onClick:()->Unit){
    Row(Modifier.fillMaxWidth().heightIn(min=91.dp).clip(RoundedCornerShape(20.dp)).background(FFColors.surface)
        .clickable(enabled=enabled,role=Role.Button,onClick=onClick).testTag(tag).padding(horizontal=20.dp,vertical=18.dp),verticalAlignment=Alignment.CenterVertically){
        AdIconCircle(icon,circle,tint,52.dp,24.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)){
            Text(title,style=adText(17.sp,FontWeight.Medium,23.sp),color=if(enabled)titleColor else titleColor.copy(alpha=.45f))
            Spacer(Modifier.height(4.dp))
            Text(subtitle,style=adText(14.sp,line=19.sp),color=FFColors.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        FFIcon("hi_chevron_right",FFColors.onSurfaceVariant,20.dp)
    }
}

// ---------- Perangkat Terhubung ----------

@Composable
private fun DevicesPage(native:NativeServices,email:String,localMode:Boolean,devices:List<ConnectedDevice>,currentDeviceId:String?,busy:Boolean,removeDevice:((String)->Unit)?){
    var removing by remember{mutableStateOf<ConnectedDevice?>(null)}
    val others=devices.filter{it.id!=currentDeviceId}
    PageColumn{
        AdFieldLabel(MR.strings.pa_devices_current.text())
        AdCard(Modifier.testTag("device-current"),padding=18.dp){
            Row(verticalAlignment=Alignment.CenterVertically){
                AdIconCircle(AdIcons.smartphone,FFColors.iconCircle,FFColors.onSurface,48.dp,22.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)){
                    Text(native.deviceDescription(),style=adText(16.sp,FontWeight.Medium,22.sp),color=FFColors.onSurface)
                    Spacer(Modifier.height(2.dp))
                    Text(if(localMode)MR.strings.set_local_mode.text() else email,style=adText(13.sp,line=18.sp),color=FFColors.onSurfaceVariant)
                    Text("FinFlow ${native.appVersion()}",style=adText(13.sp,line=18.sp),color=FFColors.onSurfaceVariant)
                }
                Box(Modifier.clip(RoundedCornerShape(100.dp)).background(AdTone.blueBg).padding(horizontal=10.dp,vertical=4.dp)){
                    Text(MR.strings.pa_devices_this_badge.text(),style=adText(12.sp,FontWeight.Medium,16.sp),color=AdTone.blue)
                }
            }
        }
        if(!localMode){
            Spacer(Modifier.height(4.dp))
            AdFieldLabel(MR.strings.r3_devices_other.text())
            if(others.isEmpty())AdBody(MR.strings.r3_devices_none.text(),Modifier.testTag("devices-empty"),size=13.sp)
            others.forEach{device->
                AdCard(Modifier.testTag("device-${device.id}"),padding=18.dp){
                    Row(verticalAlignment=Alignment.CenterVertically){
                        AdIconCircle(if(device.record.platform=="android"||device.record.platform=="ios")AdIcons.smartphone else "hi_laptop",FFColors.iconCircle,FFColors.onSurface,48.dp,22.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)){
                            Text(device.record.name,style=adText(16.sp,FontWeight.Medium,22.sp),color=FFColors.onSurface)
                            Spacer(Modifier.height(2.dp))
                            Text("FinFlow ${device.record.appVersion}",style=adText(13.sp,line=18.sp),color=FFColors.onSurfaceVariant)
                            Text(MR.strings.r3_devices_last_active.text(deviceTime(device.record.lastActiveAt)),style=adText(13.sp,line=18.sp),color=FFColors.onSurfaceVariant)
                        }
                        if(removeDevice!=null){
                            Spacer(Modifier.width(8.dp))
                            Box(Modifier.clip(RoundedCornerShape(100.dp)).background(AdTone.dangerBg).clickable(enabled=!busy,role=Role.Button){removing=device}
                                .testTag("device-remove-${device.id}").padding(horizontal=12.dp,vertical=6.dp)){
                                Text(MR.strings.r3_devices_remove.text(),style=adText(13.sp,FontWeight.Medium,18.sp),color=AdTone.danger)
                            }
                        }
                    }
                }
            }
        }
        AdBanner("hi_info",if(localMode)MR.strings.pa_devices_local_hint.text() else MR.strings.set_devices_list_hint.text(),AdTone.infoBg,AdTone.infoFg)
        if(!localMode)AdBody(MR.strings.set_devices_session_hint.text(),size=13.sp)
    }
    removing?.let{device->
        AdInfoDialog(MR.strings.r3_devices_remove_title.text(),listOf(MR.strings.r3_devices_remove_body.text(device.record.name)),{removing=null},
            confirmLabel=MR.strings.r3_devices_remove.text(),onConfirm={removing=null;removeDevice?.invoke(device.id)},danger=true)
    }
}

private fun deviceTime(millis:Long):String {
    val t=Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
    fun two(v:Int)=v.toString().padStart(2,'0')
    return "${shortDateLabel(t.date.toString())} ${two(t.hour)}:${two(t.minute)}"
}

// ---------- Ekspor Data ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportDataPage(data:FinanceData,native:NativeServices,saveToDownloads:((String,String,String)->String?)?){
    val now=today()
    val monthStart=LocalDate(now.year,now.monthNumber,1)
    var from by remember{mutableStateOf(monthStart.toString())}
    var to by remember{mutableStateOf(now.toString())}
    var preset by remember{mutableStateOf("month")}
    var type by remember{mutableStateOf("all")}
    var wallet by remember{mutableStateOf("")}
    var category by remember{mutableStateOf("")}
    var picker by remember{mutableStateOf<String?>(null)}
    var message by remember{mutableStateOf<String?>(null)}
    val allTime=preset=="all"
    val txs=data.transactions.filter{(allTime || it.date>=from && it.date<=to) && (type=="all" || it.type==type) &&
        (wallet.isBlank() || it.accountId==wallet || it.toAccountId==wallet) && (category.isBlank() || it.categoryId==category || it.subcategoryId==category)}
        .sortedWith(compareBy<Transaction>{it.date}.thenBy{it.createdAt})
    val rangeLabel=if(allTime)MR.strings.pa_export_all_time.text() else "${shortDateLabel(from)} - ${shortDateLabel(to)}"
    val fileStem=if(allTime)"finflow-semua" else "finflow-$from-$to"
    fun attempt(block:()->String?){message=try{block()}catch(e:Exception){e.message ?: MR.strings.set_export_failed.text()}}
    PageColumn(horizontal=24.dp,spacing=12.dp){
        AdFieldLabel(MR.strings.pa_export_date_range.text())
        Row(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp)).background(FFColors.surface).clickable(role=Role.Button){picker="date"}
            .testTag("export-date-range").padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically){
            FFIcon(AdIcons.calendarCheck,FFColors.onSurface,22.dp);Spacer(Modifier.width(14.dp))
            Text(rangeLabel,Modifier.weight(1f),style=adText(16.sp,line=22.sp),color=FFColors.onSurfaceVariant)
            FFIcon("hi_chevron_right",FFColors.onSurfaceVariant,20.dp)
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            AdChip(MR.strings.pa_export_this_month.text(),preset=="month",{preset="month";from=monthStart.toString();to=now.toString()},"export-this-month")
            AdChip(MR.strings.pa_export_last_month.text(),preset=="last",{
                val start=monthStart.minus(1,DateTimeUnit.MONTH);preset="last";from=start.toString();to=monthStart.minus(1,DateTimeUnit.DAY).toString()
            },"export-last-month")
            AdChip(MR.strings.pa_export_all_time.text(),allTime,{preset="all"},"export-all-time")
        }
        Spacer(Modifier.height(8.dp))
        AdFieldLabel(MR.strings.pa_export_tx_type.text())
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            listOf("all" to MR.strings.common_all.text(),"income" to transactionTypeLabel("income"),"expense" to transactionTypeLabel("expense"),"transfer" to transactionTypeLabel("transfer"))
                .forEach{(key,label)->AdChip(label,type==key,{type=key},"export-type-$key")}
        }
        Spacer(Modifier.height(8.dp))
        AdFieldLabel(MR.strings.pa_export_wallet.text())
        AdSelectField(data.accounts.find{it.id==wallet}?.name ?: MR.strings.set_export_all_wallets.text(),"hi_wallet",{picker="wallet"},"export-wallet")
        Spacer(Modifier.height(8.dp))
        AdFieldLabel(MR.strings.pa_export_category.text())
        AdSelectField(if(category.isBlank())MR.strings.pa_export_all_categories.text() else data.categoryName(category),"hi_tag",{picker="category"},"export-category")
        Spacer(Modifier.height(20.dp))
        if(txs.isEmpty())AdBody(MR.strings.pa_export_empty.text(),Modifier.fillMaxWidth(),align=TextAlign.Center)
        else AdBody(MR.strings.set_tx_count.text(txs.size),Modifier.fillMaxWidth(),align=TextAlign.Center)
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
            AdPrimaryButton(MR.strings.pa_export_pdf.text(),AdIcons.pdf,{attempt{
                val lines=listOf(MR.strings.set_pdf_range.text(data.profile?.name.orEmpty(),if(allTime)"-" else from,if(allTime)"-" else to),MR.strings.set_tx_count.text(txs.size),"")+txs.flatMap{t->listOf("${t.date} | ${t.title}","${transactionTypeLabel(if(t.type=="income"||t.type=="expense")t.type else "transfer")} | ${displayPreciseMoney(t.amountMinor,t.currency)} | ${data.accountName(t.accountId)}${t.toAccountId?.let{" → "+data.accountName(it)}.orEmpty()}","${data.categoryName(t.categoryId)}${if(t.scheduled)" · "+MR.strings.set_pdf_scheduled.text() else ""}${if(t.description.isNotBlank())" | "+t.description else ""}","")}
                native.sharePdf("$fileStem.pdf",MR.strings.set_pdf_title.text(),lines);null
            }},Modifier.weight(1f),enabled=txs.isNotEmpty(),height=52.dp,tag="export-pdf")
            AdOutlineButton(MR.strings.pa_export_share_csv.text(),"hi_share",{attempt{native.shareCsv("$fileStem.csv",CsvImport.export(data,txs));null}},
                Modifier.weight(1f),enabled=txs.isNotEmpty(),height=52.dp,tag="export-csv")
        }
        saveToDownloads?.let{save->
            AdOutlineButton(MR.strings.pa_export_save_downloads.text(),"hi_download",{attempt{save("$fileStem.csv",CsvImport.export(data,txs),"text/csv") ?: MR.strings.pa_export_saved.text()}},
                enabled=txs.isNotEmpty(),height=54.dp,strong=false,tint=FFColors.onSurfaceVariant,tag="export-downloads")
        }
        message?.let{AdBody(it,color=FFColors.onSurface)}
        AdBody(MR.strings.set_export_csv_hint.text(),size=12.sp)
    }
    when(picker){
        "wallet"->AdSelectSheet(MR.strings.pa_export_wallet.text(),listOf(AdOption("",MR.strings.set_export_all_wallets.text(),"hi_wallet"))+
            data.accounts.filter{!it.archived}.map{AdOption(it.id,it.name)},wallet,{picker=null}){wallet=it;picker=null}
        "category"->AdSelectSheet(MR.strings.pa_export_category.text(),listOf(AdOption("",MR.strings.pa_export_all_categories.text(),"hi_tag"))+
            data.categories.filter{type=="all" || it.type==type}.map{c->AdOption(c.id,(if(c.parentId!=null)"— " else "")+c.name)},category,{picker=null}){category=it;picker=null}
        "date"->{
            val state=rememberDateRangePickerState(
                initialSelectedStartDateMillis=runCatching{LocalDate.parse(from).toEpochDays().toLong()*86400000L}.getOrNull(),
                initialSelectedEndDateMillis=runCatching{LocalDate.parse(to).toEpochDays().toLong()*86400000L}.getOrNull())
            DatePickerDialog(onDismissRequest={picker=null},confirmButton={TextButton(enabled=state.selectedStartDateMillis!=null,onClick={
                val start=state.selectedStartDateMillis
                if(start!=null){
                    val end=state.selectedEndDateMillis ?: start
                    from=LocalDate.fromEpochDays((start/86400000L).toInt()).toString();to=LocalDate.fromEpochDays((end/86400000L).toInt()).toString();preset="custom"
                }
                picker=null
            }){Text(MR.strings.common_ok.text())}},dismissButton={TextButton(onClick={picker=null}){Text(MR.strings.common_cancel.text())}}){
                DateRangePicker(state,Modifier.weight(1f),title=null,showModeToggle=false)
            }
        }
    }
}

// ---------- Bantuan ----------

@Composable
private fun HelpPage(native:NativeServices,email:String){
    var tab by remember{mutableStateOf("faq")}
    var message by remember{mutableStateOf<String?>(null)}
    Column(Modifier.fillMaxSize().testTag("account-data-page")){
        Row(Modifier.fillMaxWidth()){
            listOf("faq" to "FAQ","guide" to MR.strings.pa_help_guide.text()).forEach{(key,label)->
                Column(Modifier.weight(1f).clickable(role=Role.Tab){tab=key}.testTag("help-tab-$key").padding(top=8.dp),horizontalAlignment=Alignment.CenterHorizontally){
                    Text(label,style=adText(16.sp,line=22.sp),color=if(tab==key)FFColors.onSurface else FFColors.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.width(28.dp).height(3.dp).clip(RoundedCornerShape(100.dp)).background(if(tab==key)FFColors.onSurface else Color.Transparent))
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(FFColors.hairline))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=24.dp,vertical=24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            if(tab=="faq"){
                HelpHero()
                Spacer(Modifier.height(12.dp))
                Text(MR.strings.pa_help_title.text(),Modifier.fillMaxWidth(),style=adText(20.sp,FontWeight.SemiBold,26.sp),color=FFColors.onSurface,textAlign=TextAlign.Center)
                AdBody(MR.strings.pa_help_body.text(),Modifier.fillMaxWidth(),align=TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(MR.strings.pa_help_faq.text(),style=adText(18.sp,FontWeight.SemiBold,24.sp),color=FFColors.onSurface)
                faqTopics.forEachIndexed{index,(q,a)->AdExpandableText(q.text(),a.text(),tag="faq-$index")}
            } else guideTopics.forEachIndexed{index,(title,body)->AdExpandableText(title.text(),body.text(),tag="guide-$index")}
            Spacer(Modifier.height(16.dp))
            AdPrimaryButton(MR.strings.pa_contact_support.text(),AdIcons.headset,{
                message=native.composeSupport("FinFlow · ${MR.strings.pa_contact_support.text()}","\n\n$email\nFinFlow ${native.appVersion()}\n${native.deviceDescription()}") ?: MR.strings.set_bug_draft_opened.text()
            },radius=28.dp,tag="help-contact-support")
            message?.let{AdBody(it,color=FFColors.onSurface)}
        }
    }
}

@Composable
private fun HelpHero(){
    val gradient=if(FFColors.isDark)listOf(Color(0xFF0E3B47),Color(0xFF145A66)) else listOf(Color(0xFF5FB3C4),Color(0xFF7CCFD2))
    Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(28.dp)).background(Brush.linearGradient(gradient)),contentAlignment=Alignment.Center){
        Box(Modifier.size(120.dp).clip(CircleShape).background(Color.White.copy(alpha=.18f)),contentAlignment=Alignment.Center){
            Box(Modifier.size(88.dp).clip(CircleShape).background(Color.White.copy(alpha=.9f)),contentAlignment=Alignment.Center){FFIcon(AdIcons.headset,Color(0xFF1F6F7F),44.dp)}
        }
    }
}

// ---------- Masukan ----------

@Composable private fun SupportForm(page:String,native:NativeServices) {
    var body by remember{mutableStateOf("")}
    var message by remember{mutableStateOf<String?>(null)}
    Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text(if(page=="Masukan")MR.strings.set_support_feedback_title.text() else MR.strings.set_support_bug_title.text(),style=adText(18.sp,FontWeight.SemiBold,24.sp),color=FFColors.onSurface)
        Field(if(page=="Masukan")MR.strings.set_support_suggestion.text() else MR.strings.set_support_steps.text(),body,{body=it.take(4000)},singleLine=false)
        AdBanner("hi_info",MR.strings.set_support_email_hint.text(),AdTone.infoBg,AdTone.infoFg)
        // Teks tombol dicocokkan uji perangkat ("Buka Draf Email").
        Button(enabled=body.isNotBlank(),onClick={message=native.composeSupport("FinFlow · $page",body+"\n\nFinFlow "+native.appVersion()+"\n"+native.deviceDescription()) ?: MR.strings.set_bug_draft_opened.text()},
            modifier=Modifier.fillMaxWidth().height(56.dp),shape=RoundedCornerShape(20.dp),
            colors=ButtonDefaults.buttonColors(containerColor=FFColors.pill,contentColor=FFColors.onPill)){Text(MR.strings.set_support_open_draft.text(),style=adText(16.sp,FontWeight.Medium,22.sp))}
        message?.let{AdBody(it,color=FFColors.onSurface)}
    }
}

// ---------- Tentang ----------

@Composable
private fun AboutPage(native:NativeServices){
    val uri=LocalUriHandler.current
    var message by remember{mutableStateOf<String?>(null)}
    PageColumn(horizontal=24.dp){
        val gradient=if(FFColors.isDark)listOf(Color(0xFF12253F),Color(0xFF0E3A3A)) else listOf(Color(0xFFDCEEF8),Color(0xFFCDEFEA))
        Box(Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(28.dp)).background(Brush.linearGradient(gradient)),contentAlignment=Alignment.Center){
            FinFlowLogo(140.dp)
        }
        Spacer(Modifier.height(12.dp))
        Text(MR.strings.pa_about_title.text(),Modifier.fillMaxWidth(),style=adText(21.sp,FontWeight.SemiBold,28.sp),color=FFColors.onSurface,textAlign=TextAlign.Center)
        AdBody(MR.strings.pa_about_body.text(),Modifier.fillMaxWidth(),size=16.sp,align=TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Text(MR.strings.pa_about_connect.text(),Modifier.fillMaxWidth(),style=adText(18.sp,FontWeight.SemiBold,24.sp),color=FFColors.onSurface,textAlign=TextAlign.Center)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(20.dp,Alignment.CenterHorizontally)){
            SocialCircle("hi_mail",MR.strings.pa_about_email.text(),"about-email"){message=native.composeSupport("FinFlow","\n\nFinFlow ${native.appVersion()}")}
            SocialCircle(AdIcons.globe,MR.strings.set_bug_open_store.text(),"about-store"){try{uri.openUri(PLAY_STORE_LINK)}catch(_:Exception){message=MR.strings.set_bug_store_failed.text()}}
            SocialCircle("hi_share",MR.strings.set_profile_share.text(),"about-share"){message=native.shareApp()}
        }
        message?.let{AdBody(it,Modifier.fillMaxWidth(),color=FFColors.onSurface,align=TextAlign.Center)}
        Spacer(Modifier.height(24.dp))
        Text("v${native.appVersion()}",Modifier.fillMaxWidth(),style=adText(13.sp,line=18.sp),color=FFColors.onSurfaceVariant,textAlign=TextAlign.Center)
    }
}

@Composable
private fun SocialCircle(icon:String,label:String,tag:String,onClick:()->Unit){
    Box(Modifier.size(48.dp).clip(CircleShape).background(FFColors.surface).clickable(role=Role.Button,onClickLabel=label,onClick=onClick).testTag(tag),contentAlignment=Alignment.Center){
        FFIcon(icon,FFColors.onSurface,24.dp,label)
    }
}

// ---------- Catatan Pembaruan ----------

@Composable
private fun ChangelogPage(native:NativeServices){
    PageColumn(horizontal=16.dp){
        AdCard(padding=16.dp){
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(FFColors.raised).padding(horizontal=12.dp,vertical=12.dp)){
                Text(MR.strings.set_version.text(native.appVersion()),style=adText(18.sp,FontWeight.Bold,24.sp),color=FFColors.onSurface)
            }
            Spacer(Modifier.height(8.dp))
            AdBody(MR.strings.set_changelog_date.text(),Modifier.padding(horizontal=12.dp),size=13.sp)
            Spacer(Modifier.height(12.dp))
            MR.strings.set_changelog_body.text().lines().map{it.trim().removePrefix("•").trim()}.filter{it.isNotBlank()}.forEach{line->
                Row(Modifier.padding(start=8.dp,end=4.dp,bottom=10.dp)){
                    Text("◦",style=adText(15.sp,line=22.sp),color=FFColors.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Text(line,Modifier.weight(1f),style=adText(15.sp,line=22.sp),color=FFColors.onSurfaceVariant)
                }
            }
        }
    }
}
