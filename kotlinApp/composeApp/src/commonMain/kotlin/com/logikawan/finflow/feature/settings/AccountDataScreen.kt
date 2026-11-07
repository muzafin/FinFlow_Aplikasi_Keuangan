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
import com.logikawan.finflow.core.i18n.tabLabel
import com.logikawan.finflow.core.i18n.text
import com.logikawan.finflow.core.i18n.transactionTypeLabel
import com.logikawan.finflow.i18n.*

val accountDataPages=setOf("Manajemen Akun","Cadangan & Pemulihan","Perangkat Terhubung","Ekspor Data","Aktivitas","Bantuan","Masukan","Laporkan Bug","Tentang","Catatan Pembaruan")

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
    else->page
}

private val helpTopics=listOf(
    MR.strings.set_help_record_title to MR.strings.set_help_record_body,
    MR.strings.set_help_balance_title to MR.strings.set_help_balance_body,
    MR.strings.set_help_goals_title to MR.strings.set_help_goals_body,
    MR.strings.set_help_bundle_title to MR.strings.set_help_bundle_body,
    MR.strings.set_help_fix_title to MR.strings.set_help_fix_body,
    MR.strings.set_help_bank_title to MR.strings.set_help_bank_body,
    MR.strings.set_help_about_title to MR.strings.set_help_about_body,
)

@Composable
fun AccountDataScreen(page:String,data:FinanceData,native:NativeServices,email:String,navigate:(String)->Unit,
    reload:()->Unit,backup:()->Unit,chooseRestore:()->Unit,busy:Boolean=false,changePhoto:()->Unit={},deleteAccount:()->Unit={},localMode:Boolean=false,connect:()->Unit={},useLocal:()->Unit={},back:()->Unit={},accountExtras:androidx.compose.foundation.lazy.LazyListScope.()->Unit={}) {
    if(page=="Aktivitas"){ LevelScreen(data,native,back){ navigate(it) }; return }
    var message by remember{mutableStateOf<String?>(null)}
    fun export(block:()->Unit){try{block()}catch(e:Exception){message=e.message ?: MR.strings.set_export_failed.text()}}
    LazyColumn(Modifier.fillMaxSize().testTag("account-data-page"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        when(page){
            "Manajemen Akun"->{
                item{DesignCard{ProfilePhoto(data.profile,native,Modifier.size(72.dp));Spacer(Modifier.height(12.dp));Text(data.profile?.name.orEmpty(),style=FFType.sectionTitle);Text(if(localMode)MR.strings.set_local_mode.text() else email,style=FFType.bodyMain);Spacer(Modifier.height(12.dp));Text(if(localMode)MR.strings.set_acct_local_desc.text() else MR.strings.set_acct_cloud_desc.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant)}}
                accountExtras()
                item{Button(onClick=if(localMode)connect else useLocal,enabled=!busy,modifier=Modifier.fillMaxWidth()){Text(if(localMode)MR.strings.set_acct_connect.text() else MR.strings.set_acct_use_local.text())}}
                item{OutlinedButton(onClick=changePhoto,enabled=!busy,modifier=Modifier.fillMaxWidth()){Text(MR.strings.set_acct_change_photo.text())}}
                item{Button(onClick={navigate("Profil akun")},modifier=Modifier.fillMaxWidth()){Text(MR.strings.set_acct_edit_profile.text())}}
                if(!localMode)item{OutlinedButton(onClick={navigate("Ubah password")},modifier=Modifier.fillMaxWidth()){Text(MR.strings.set_acct_send_reset.text())}}
                item{OutlinedButton(onClick=reload,enabled=!busy,modifier=Modifier.fillMaxWidth()){Text(MR.strings.set_acct_reload.text())}}
                if(!localMode){item{Text(MR.strings.set_acct_google_hint.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant)}
                item{OutlinedButton(onClick=deleteAccount,enabled=!busy,modifier=Modifier.fillMaxWidth()){Text(MR.strings.set_acct_delete.text(),color=FFColors.expense)}}}
            }
            "Cadangan & Pemulihan"->{
                item{DesignCard{FFIcon("backup_outlined",size=42.dp);Spacer(Modifier.height(12.dp));Text(MR.strings.set_backup_full.text(),style=FFType.sectionTitle);Text(MR.strings.set_backup_full_body.text(),style=FFType.bodyMain);Spacer(Modifier.height(12.dp));Text(MR.strings.set_backup_private_hint.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant)}}
                item{Button(onClick={export(backup)},enabled=data.featuresReady && !busy,modifier=Modifier.fillMaxWidth()){Text(MR.strings.set_backup_save.text())}}
                item{OutlinedButton(onClick=chooseRestore,enabled=data.featuresReady && !busy,modifier=Modifier.fillMaxWidth()){Text(MR.strings.set_backup_restore.text())}}
                item{Text(MR.strings.set_backup_restore_hint.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant)}
                item{OutlinedButton(onClick={navigate("Impor data")},modifier=Modifier.fillMaxWidth()){Text(MR.strings.set_backup_import_csv.text())}}
            }
            "Perangkat Terhubung"->{
                item{DesignCard{FFIcon("devices",size=42.dp);Spacer(Modifier.height(12.dp));Text(native.deviceDescription(),style=FFType.sectionTitle);Text(MR.strings.set_devices_this.text(email),style=FFType.bodyMain);Spacer(Modifier.height(12.dp));Text(MR.strings.set_devices_session_hint.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant)}}
                item{Text(MR.strings.set_devices_list_hint.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant)}
            }
            "Ekspor Data"->item{ExportDataForm(data,native,::export)}
            "Aktivitas"->{
                item{RecordingProgressSummary(data,native)}
            }
            "Bantuan"->{
                helpTopics.forEach{(title,body)->item{DesignCard{Text(title.text(),style=FFType.sectionTitle);Spacer(Modifier.height(10.dp));Text(body.text(),style=FFType.bodyMain,color=FFColors.onSurfaceVariant)}}}
            }
            "Masukan"->item{SupportForm(page,native)}
            "Laporkan Bug"->item{BugReportForm(native)}
            "Tentang"->item{DesignCard{FinFlowLogo(88.dp);Text("FinFlow",style=FFType.appTitle);Text(MR.strings.set_version.text(native.appVersion()),style=FFType.metaData,color=FFColors.onSurfaceVariant);Spacer(Modifier.height(12.dp));Text(MR.strings.set_about_body.text(),style=FFType.bodyMain)}}
            "Catatan Pembaruan"->item{DesignCard{Text(MR.strings.set_version.text(native.appVersion()),style=FFType.sectionTitle);Text(MR.strings.set_changelog_date.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant);Spacer(Modifier.height(12.dp));Text(MR.strings.set_changelog_body.text(),style=FFType.bodyMain)}}
        }
    }
    message?.let{AlertDialog(onDismissRequest={message=null},title={Text(MR.strings.set_export_unavailable.text())},text={Text(it)},confirmButton={TextButton(onClick={message=null}){Text(MR.strings.common_close.text())}})}
}

@Composable private fun SupportForm(page:String,native:NativeServices) {
    var body by remember{mutableStateOf("")}
    var message by remember{mutableStateOf<String?>(null)}
    Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text(if(page=="Masukan")MR.strings.set_support_feedback_title.text() else MR.strings.set_support_bug_title.text(),style=FFType.sectionTitle)
        Field(if(page=="Masukan")MR.strings.set_support_suggestion.text() else MR.strings.set_support_steps.text(),body,{body=it.take(4000)},singleLine=false)
        Text(MR.strings.set_support_email_hint.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant)
        Button(enabled=body.isNotBlank(),onClick={message=native.composeSupport("FinFlow · $page",body+"\n\nFinFlow "+native.appVersion()+"\n"+native.deviceDescription())},modifier=Modifier.fillMaxWidth()){Text(MR.strings.set_support_open_draft.text())}
        message?.let{Text(it,color=MaterialTheme.colorScheme.error)}
    }
}

@Composable private fun ExportDataForm(data:FinanceData,native:NativeServices,attempt:(()->Unit)->Unit) {
    var from by remember{mutableStateOf(today().toString().take(7)+"-01")}
    var to by remember{mutableStateOf(today().toString())}
    var type by remember{mutableStateOf("all")};var wallet by remember{mutableStateOf("")};var currency by remember{mutableStateOf("")}
    var error by remember{mutableStateOf<String?>(null)}
    val txs=data.transactions.filter{it.date>=from && it.date<=to && (type=="all" || it.type==type) && (wallet.isBlank() || it.accountId==wallet || it.toAccountId==wallet) && (currency.isBlank() || it.currency==currency)}.sortedWith(compareBy<Transaction>{it.date}.thenBy{it.createdAt})
    fun valid():Boolean=try{kotlinx.datetime.LocalDate.parse(from);kotlinx.datetime.LocalDate.parse(to);require(from<=to){MR.strings.set_export_date_order.text()};error=null;true}catch(e:Exception){error=e.message;false}
    Column(verticalArrangement=Arrangement.spacedBy(16.dp)){
        DesignCard{Text(MR.strings.set_export_range.text(),style=FFType.sectionTitle);Spacer(Modifier.height(12.dp));Field(MR.strings.set_export_from.text(),from,{from=it});Field(MR.strings.set_export_to.text(),to,{to=it});Choice(MR.strings.set_type.text(),type,listOf("all" to MR.strings.common_all.text(),"income" to transactionTypeLabel("income"),"expense" to transactionTypeLabel("expense"),"transfer" to transactionTypeLabel("transfer")),{type=it});Choice(tabLabel("Dompet"),wallet,listOf("" to MR.strings.set_export_all_wallets.text())+data.accounts.map{it.id to it.name},{wallet=it});Choice(MR.strings.set_currency.text(),currency,listOf("" to MR.strings.set_export_all_currencies.text())+data.accounts.map{it.currency}.distinct().map{it to it},{currency=it});Text(MR.strings.set_tx_count.text(txs.size),style=FFType.metaData)}
        error?.let{Text(it,color=FFColors.expense)}
        Button(onClick={if(valid())attempt{native.shareCsv("finflow-$from-$to.csv",CsvImport.export(data,txs))}},modifier=Modifier.fillMaxWidth()){Text(MR.strings.set_export_csv.text())}
        OutlinedButton(onClick={if(valid())attempt{
            val lines=listOf(MR.strings.set_pdf_range.text(data.profile?.name.orEmpty(),from,to),MR.strings.set_tx_count.text(txs.size),"")+txs.flatMap{t->listOf("${t.date} | ${t.title}","${when(t.type){"income"->transactionTypeLabel("income");"expense"->transactionTypeLabel("expense");else->transactionTypeLabel("transfer")}} | ${displayPreciseMoney(t.amountMinor,t.currency)} | ${data.accountName(t.accountId)}${t.toAccountId?.let{" → "+data.accountName(it)}.orEmpty()}","${data.categoryName(t.categoryId)}${if(t.scheduled)" · "+MR.strings.set_pdf_scheduled.text() else ""}${if(t.description.isNotBlank())" | "+t.description else ""}","")}
            native.sharePdf("finflow-$from-$to.pdf",MR.strings.set_pdf_title.text(),lines)
        }},modifier=Modifier.fillMaxWidth()){Text(MR.strings.set_export_pdf.text())}
        Text(MR.strings.set_export_csv_hint.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant)
    }
}
