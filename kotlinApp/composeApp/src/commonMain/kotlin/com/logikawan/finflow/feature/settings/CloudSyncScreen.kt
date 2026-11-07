package com.logikawan.finflow.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.logikawan.finflow.core.data.FinanceRepository
import com.logikawan.finflow.core.i18n.text
import com.logikawan.finflow.i18n.*
import com.logikawan.finflow.core.model.*
import com.logikawan.finflow.core.platform.NativeServices
import com.logikawan.finflow.core.platform.AppLifecycle
import com.logikawan.finflow.core.ui.*
import com.logikawan.finflow.core.util.*
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
                val restored=BackupCodec.restoreData(backup)
                val index=backup.data.accounts.indexOfFirst{it.id==preferences.defaultWalletId}
                PreferenceState(native,uid).update(preferences.copy(defaultWalletId=if(index>=0)restored.accounts[index].id else ""))
                native.writeSetting("local.sync.checkpoint","");native.writeSetting("local.sync.frozen","")
                finish()
            } catch(e:CancellationException){throw e}
            catch(e:Exception){error=e.message ?: MR.strings.auth_sync_unfinished.text();if(!began)native.writeSetting("local.sync.frozen","")}
            finally{if(!began)native.writeSetting("local.sync.frozen","");busy=false}
        }
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        DesignHeader(MR.strings.auth_sync_title.text(),back={if(!busy)cancel()})
        DesignCard {
            FFIcon("sync_alt",size=40.dp)
            Text(cloud.currentUser?.email.orEmpty(),style=FFType.sectionTitle)
            Text(MR.strings.auth_sync_description.text(),style=FFType.bodyMain)
            Text(MR.strings.auth_sync_target_note.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant)
        }
        if(checkpoint!=null)Text(MR.strings.auth_sync_pending_note.text(),style=FFType.bodyMain)
        if(busy){LinearProgressIndicator(Modifier.fillMaxWidth());Text(progress)}
        error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        Button(onClick=::synchronize,enabled=!busy,modifier=Modifier.fillMaxWidth()){Text(if(checkpoint==null)MR.strings.auth_sync_start.text() else MR.strings.auth_sync_resume.text())}
        if(checkpoint==null)OutlinedButton(onClick=finish,enabled=!busy,modifier=Modifier.fillMaxWidth()){Text(MR.strings.auth_sync_open_account.text())}
        OutlinedButton(onClick={scope.launch{busy=true;try{cloud.logout()}finally{busy=false}}},enabled=!busy,modifier=Modifier.fillMaxWidth()){Text(MR.strings.auth_sync_other_account.text())}
        TextButton(onClick=cancel,enabled=!busy,modifier=Modifier.fillMaxWidth()){Text(MR.strings.auth_back_to_local.text())}
    }
}
