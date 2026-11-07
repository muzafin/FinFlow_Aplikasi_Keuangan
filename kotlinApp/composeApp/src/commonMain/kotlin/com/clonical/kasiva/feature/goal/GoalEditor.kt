package com.clonical.kasiva.feature.goal

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clonical.kasiva.core.model.Goal
import com.clonical.kasiva.core.ui.*
import com.clonical.kasiva.core.util.*
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.i18n.*
import com.clonical.kasiva.feature.planning.OlloFormSection
import com.clonical.kasiva.feature.planning.PlanDateField

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
    EditorDialog(if(old==null)MR.strings.acc_new_goal.text() else MR.strings.acc_edit_goal.text(),busy,dismiss,save={try{
        val code=currency.trim().uppercase()
        require(name.isNotBlank() && code.matches(Regex("[A-Z]{3}"))){MR.strings.acc_err_goal_name_currency.text()}
        val amount=minorAmount(target,code)
        require(amount in 1..com.clonical.kasiva.core.data.FinanceRepository.MAX_MONEY){MR.strings.acc_err_goal_target_range.text()}
        val base=old ?: Goal(id,name.trim(),amount,currency=code)
        save(base.copy(name=name.trim(),targetMinor=amount,currency=code,targetDate=if(hasDate)date else "",bucketType=type,protection=protection,archived=archived))
    }catch(e:Exception){error=e.message}},delete,error,EditorKind.Goal){
        OlloFormSection(MR.strings.acc_goal_details.text())
        Field(MR.strings.acc_goal_amount.text(),target,{target=it},enabled=!busy)
        Field(MR.strings.acc_bucket_name.text(),name,{name=it},enabled=!busy)
        Field(MR.strings.acc_currency.text(),currency,{currency=it.uppercase().take(3)},enabled=!busy,readOnly=old!=null)
        Choice(MR.strings.acc_bucket_type.text(),type,listOf("standard" to bucketName("standard"),"emergency" to bucketName("emergency"),"locked" to bucketName("locked")),{type=it})
        OlloFormSection(MR.strings.acc_schedule_protection.text())
        Row(verticalAlignment=Alignment.CenterVertically){Checkbox(hasDate,{hasDate=it},enabled=!busy);Spacer(Modifier.width(8.dp));Text(MR.strings.acc_use_target_date.text(),style=FFType.bodyMain)}
        if(hasDate)PlanDateField(MR.strings.acc_target_date.text(),date,{date=it},!busy)
        Choice(MR.strings.acc_withdraw_protection.text(),protection,listOf("flexible" to MR.strings.acc_protection_flexible.text(),"soft" to MR.strings.acc_protection_soft.text(),"hard" to MR.strings.acc_protection_hard.text()),{protection=it})
        if(old!=null && (old.holdingAccountId==null && old.savedMinor>0 || old.legacySavedMinor>0))Text(MR.strings.acc_legacy_progress_editor.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant)
        if(old!=null)Row(verticalAlignment=Alignment.CenterVertically){Switch(archived,{archived=it},enabled=!busy && old.savedMinor==old.legacySavedMinor);Spacer(Modifier.width(12.dp));Text(MR.strings.acc_archived.text(),style=FFType.bodyMain)}
        Text(MR.strings.acc_goal_wallet_note.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant)
    }
}
