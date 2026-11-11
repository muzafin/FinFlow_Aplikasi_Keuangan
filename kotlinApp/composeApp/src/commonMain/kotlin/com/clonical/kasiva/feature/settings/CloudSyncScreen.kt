package com.clonical.kasiva.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import com.clonical.kasiva.core.data.FinanceRepository
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.i18n.*
import com.clonical.kasiva.core.model.*
import com.clonical.kasiva.core.platform.NativeServices
import com.clonical.kasiva.core.platform.AppLifecycle
import com.clonical.kasiva.core.ui.*
import com.clonical.kasiva.core.util.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class LocalSyncCheckpoint(val backupId:String,val createdAt:Long,val userId:String,val checksum:String)

@Composable
fun CloudSyncScreen(local:FinanceRepository,cloud:FinanceRepository,native:NativeServices,preferences:AppPreferences,finish:()->Unit,cancel:()->Unit,lifecycle:AppLifecycle?=null) {
    val scope=rememberCoroutineScope()
    var busy by remember{mutableStateOf(false)}
    var progress by remember{mutableStateOf("")}
    var error by remember{mutableStateOf<String?>(null)}
    var checkpoint by remember{mutableStateOf(native.readSetting("local.sync.checkpoint")?.takeIf{it.isNotBlank()})}
    DisposableEffect(lifecycle,busy) {
        lifecycle?.onBack={if(!busy)cancel();true}
        onDispose { lifecycle?.onBack=null }
    }
    fun synchronize() {
        if(busy)return
        busy=true;error=null
        scope.launch {
            var began=checkpoint!=null
            try {
                val uid=requireNotNull(cloud.ownerId){MR.strings.auth_sync_sign_in_first.text()}
                val previous=checkpoint?.let{Json.decodeFromString<LocalSyncCheckpoint>(it)}
                require(previous==null || previous.userId==uid){MR.strings.auth_sync_same_account.text()}
                native.writeSetting("local.sync.frozen","true")
                val data=local.observe().first{it.featuresReady && it.profile!=null}
                val backup=FinanceBackup(id=previous?.backupId ?: newId(),createdAt=previous?.createdAt ?: nowMillis(),ownerId="local",data=data,preferences=preferences)
                val checksum=native.sha256(BackupCodec.encode(backup))
                require(previous==null || previous.checksum==checksum){MR.strings.auth_sync_data_changed.text()}
                val job=LocalSyncCheckpoint(backup.id,backup.createdAt,uid,checksum)
                cloud.initializeProfile()
                cloud.restoreBackup(backup,previous!=null,begin={
                    val encoded=Json.encodeToString(job)
                    native.writeSetting("local.sync.checkpoint",encoded);checkpoint=encoded;began=true
                },progress={progress=it})
                PreferenceState(native,uid).update(BackupCodec.restoredPreferences(backup,preferences))
                native.writeSetting("local.sync.checkpoint","");native.writeSetting("local.sync.frozen","")
                finish()
            } catch(e:CancellationException){throw e}
            catch(e:Exception){error=e.message ?: MR.strings.auth_sync_unfinished.text();if(!began)native.writeSetting("local.sync.frozen","")}
            finally{if(!began)native.writeSetting("local.sync.frozen","");busy=false}
        }
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).testTag("cloud-sync-screen")) {
        DesignHeader(MR.strings.auth_sync_title.text(),back={if(!busy)cancel()})
        Column(Modifier.padding(horizontal=24.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            AdCard(padding=20.dp) {
                Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                    AdIconCircle(AdIcons.cloudSync,AdTone.blueBg,AdTone.blue,52.dp,24.dp)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(MR.strings.ai_cloud_sync.text(),style=adText(17.sp,androidx.compose.ui.text.font.FontWeight.Medium,23.sp),color=FFColors.onSurface)
                        Text(cloud.currentUser?.email.orEmpty(),style=adText(14.sp,line=19.sp),color=FFColors.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(16.dp))
                AdBody(MR.strings.auth_sync_description.text(),color=FFColors.onSurface)
                Spacer(Modifier.height(8.dp))
                AdBody(MR.strings.auth_sync_target_note.text(),size=13.sp)
            }
            if(checkpoint!=null)AdBanner("hi_info",MR.strings.auth_sync_pending_note.text(),AdTone.infoBg,AdTone.infoFg)
            if(busy){;AdBody(progress)}
            error?.let{AdBody(it,color=AdTone.danger)}
            AdPrimaryButton(if(checkpoint==null)MR.strings.auth_sync_start.text() else MR.strings.auth_sync_resume.text(),AdIcons.cloudSync,::synchronize,enabled=!busy,tag="cloud-sync-start")
            if(checkpoint==null)AdOutlineButton(MR.strings.auth_sync_open_account.text(),null,finish,enabled=!busy,tag="cloud-sync-open-account")
            AdPillButton(MR.strings.auth_sync_other_account.text(),"hi_user",{scope.launch{busy=true;try{cloud.logout()}finally{busy=false}}},Modifier.fillMaxWidth(),enabled=!busy,height=48.dp,tag="cloud-sync-other")
            AdPillButton(MR.strings.auth_back_to_local.text(),null,cancel,Modifier.fillMaxWidth(),enabled=!busy,height=48.dp,border=false,tint=FFColors.onSurfaceVariant,tag="cloud-sync-local")
        }
    }
}
