package com.logikawan.finflow.feature.goal

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.logikawan.finflow.core.model.Goal
import com.logikawan.finflow.core.ui.*
import com.logikawan.finflow.core.util.*
import com.logikawan.finflow.feature.planning.OlloFormSection
import com.logikawan.finflow.feature.planning.PlanDateField

@Composable
fun GoalEditor(old:Goal?,busy:Boolean,dismiss:()->Unit,save:(Goal)->Unit,delete:(()->Unit)?) {
    val id=remember{old?.id ?: newId()}
    var name by remember{mutableStateOf(old?.name.orEmpty())}
    var currency by remember{mutableStateOf(old?.currency ?: "IDR")}
    var target by remember{mutableStateOf(old?.let{decimalAmount(it.targetMinor,it.currency)}.orEmpty())}
    var hasDate by remember{mutableStateOf(!old?.targetDate.isNullOrBlank())}
    var date by remember{mutableStateOf(old?.targetDate?.takeIf{it.isNotBlank()} ?: today().toString())}
    var type by remember{mutableStateOf(old?.bucketType ?: "standard")}
    var protection by remember{mutableStateOf(old?.protection ?: "flexible")}
    var archived by remember{mutableStateOf(old?.archived ?: false)}
    var error by remember{mutableStateOf<String?>(null)}
    EditorDialog(if(old==null)"Target Baru" else "Edit Target",busy,dismiss,save={try{
        val code=currency.trim().uppercase()
        require(name.isNotBlank() && code.matches(Regex("[A-Z]{3}"))){"Isi nama dan mata uang yang valid"}
        val amount=minorAmount(target,code)
        require(amount in 1..com.logikawan.finflow.core.data.FinanceRepository.MAX_MONEY){"Target harus positif dan tidak melebihi batas"}
        val base=old ?: Goal(id,name.trim(),amount,currency=code)
        save(base.copy(name=name.trim(),targetMinor=amount,currency=code,targetDate=if(hasDate)date else "",bucketType=type,protection=protection,archived=archived))
    }catch(e:Exception){error=e.message}},delete,error){
        OlloFormSection("Detail Target")
        Field("Target Dana",target,{target=it},enabled=!busy)
        Field("Nama Bucket",name,{name=it},enabled=!busy)
        Field("Mata Uang",currency,{currency=it.uppercase().take(3)},enabled=!busy,readOnly=old!=null)
        Choice("Jenis Bucket",type,listOf("standard" to "Target Standar","emergency" to "Dana Darurat","locked" to "Terkunci / Deposito"),{type=it})
        OlloFormSection("Jadwal & Proteksi")
        Row(verticalAlignment=Alignment.CenterVertically){Checkbox(hasDate,{hasDate=it},enabled=!busy);Spacer(Modifier.width(8.dp));Text("Gunakan tanggal target",style=FFType.bodyMain)}
        if(hasDate)PlanDateField("Tanggal Target",date,{date=it},!busy)
        Choice("Proteksi Penarikan",protection,listOf("flexible" to "Fleksibel","soft" to "Peringatan sebelum menarik","hard" to "Konfirmasi dua tahap"),{protection=it})
        if(old!=null && (old.holdingAccountId==null && old.savedMinor>0 || old.legacySavedMinor>0))Text("Progres manual lama tetap tersimpan. Progres tersebut tidak otomatis menjadi uang baru di dompet.",style=FFType.metaData,color=FFColors.onSurfaceVariant)
        if(old!=null)Row(verticalAlignment=Alignment.CenterVertically){Switch(archived,{archived=it},enabled=!busy && old.savedMinor==old.legacySavedMinor);Spacer(Modifier.width(12.dp));Text("Diarsipkan",style=FFType.bodyMain)}
        Text("Dompet target dibuat otomatis saat dana pertama disetor. Setoran dan penarikan adalah transfer internal, bukan pengeluaran statistik.",style=FFType.metaData,color=FFColors.onSurfaceVariant)
    }
}
