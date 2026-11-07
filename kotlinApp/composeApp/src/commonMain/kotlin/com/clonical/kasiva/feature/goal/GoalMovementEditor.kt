package com.clonical.kasiva.feature.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonical.kasiva.core.data.FinanceRepository
import com.clonical.kasiva.core.model.*
import com.clonical.kasiva.core.ui.*
import com.clonical.kasiva.core.util.*
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.i18n.*
import com.clonical.kasiva.feature.planning.OlloFormSection
import com.clonical.kasiva.feature.planning.PaymentRequest
import com.clonical.kasiva.feature.planning.PlanDateField

@Composable
fun GoalMovementEditor(goal:Goal,data:FinanceData,withdraw:Boolean,busy:Boolean,dismiss:()->Unit,save:(PaymentRequest)->Unit) {
    val request=remember{newId()}
    val wallets=data.accounts.filter{!it.archived && it.type!="savings" && it.currency==goal.currency}
    val available=if(goal.holdingAccountId==null)0 else (goal.savedMinor-goal.legacySavedMinor).coerceAtLeast(0)
    val percentageBase=if(withdraw)available else (goal.targetMinor-goal.savedMinor).coerceAtLeast(0)
    var amount by remember{mutableStateOf("")}
    var account by remember{mutableStateOf(wallets.firstOrNull()?.id.orEmpty())}
    var date by remember{mutableStateOf(today().toString())}
    var error by remember{mutableStateOf<String?>(null)}
    var confirmStage by remember{mutableIntStateOf(0)}
    var pending by remember{mutableStateOf<PaymentRequest?>(null)}
    val preferences=LocalPreferences.current
    val selectedWallet=wallets.find{it.id==account}
    EditorDialog(if(withdraw)MR.strings.acc_withdraw_funds.text() else MR.strings.acc_deposit_funds.text(),busy,dismiss,save={try{
        val value=minorAmount(amount,goal.currency)
        require(value in 1..FinanceRepository.MAX_MONEY){MR.strings.acc_err_positive_amount.text()}
        require(date<=today().toString()){MR.strings.acc_err_future_date.text()}
        val wallet=wallets.find{it.id==account} ?: error(MR.strings.acc_err_choose_wallet.text())
        if(withdraw)require(value<=available){MR.strings.acc_err_insufficient_goal_funds.text()}
        else require(value<=data.balance(wallet)){MR.strings.acc_err_insufficient_source.text()}
        val payment=PaymentRequest(value,account,date,request)
        if(withdraw && goal.protection!="flexible"){pending=payment;confirmStage=1} else save(payment)
    }catch(e:Exception){error=e.message}},error=error){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(GoalAccent.copy(alpha=.12f)),contentAlignment=Alignment.Center){FFIcon(bucketIcon(goal.bucketType),GoalAccent,22.dp)}
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(goal.name,style=FFType.bodyMain.copy(fontSize=16.sp,lineHeight=22.sp),fontWeight=FontWeight.SemiBold,color=FFColors.onSurface,maxLines=1,overflow=TextOverflow.Ellipsis)
                Text(if(withdraw)MR.strings.acc_withdraw_subtitle.text() else MR.strings.acc_deposit_subtitle.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant)
            }
        }
        OlloFormSection(if(withdraw)MR.strings.acc_withdraw_amount.text() else MR.strings.acc_deposit_amount.text())
        Field(MR.strings.acc_amount_currency.text(goal.currency),amount,{amount=it},enabled=!busy,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal))
        Text(if(withdraw)MR.strings.acc_available.text(goalMoney(available,goal.currency,preferences)) else MR.strings.acc_remaining_target_value.text(goalMoney(percentageBase,goal.currency,preferences)),style=FFType.bodyMain,color=FFColors.onSurfaceVariant)
        OlloPercentRow(if(withdraw)listOf(25,50,100) else listOf(5,10,25,50),percentageBase,goal.currency,!busy){amount=it}
        WalletChoice(if(withdraw)MR.strings.acc_to_wallet_label.text() else MR.strings.acc_from_wallet_label.text(),account,data,wallets,!busy){account=it}
        selectedWallet?.let{Text(MR.strings.acc_balance_value.text(goalMoney(data.balance(it),it.currency,preferences)),style=FFType.metaData,color=FFColors.onSurfaceVariant)}
        PlanDateField(MR.strings.acc_date.text(),date,{date=it},!busy)
        Text(MR.strings.acc_movement_note.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant)
    }
    if(confirmStage>0)AlertDialog(onDismissRequest={confirmStage=0},title={Text(if(confirmStage==1)MR.strings.acc_confirm_withdraw_title.text() else MR.strings.acc_confirm_final_title.text())},text={Text(if(confirmStage==1)MR.strings.acc_confirm_withdraw_body.text(goal.name) else MR.strings.acc_confirm_final_body.text(goalMoney(pending?.amountMinor ?: 0,goal.currency,preferences)))},confirmButton={TextButton(onClick={if(goal.protection=="hard" && confirmStage==1)confirmStage=2 else {confirmStage=0;pending?.let(save)}}){Text(if(goal.protection=="hard" && confirmStage==1)MR.strings.acc_continue.text() else MR.strings.acc_confirm_withdraw_button.text())}},dismissButton={TextButton(onClick={confirmStage=0}){Text(MR.strings.common_cancel.text())}})
}

@Composable
private fun OlloPercentRow(percentages:List<Int>,base:Long,currency:String,enabled:Boolean,select:(String)->Unit) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        percentages.forEach{percentage->
            Surface(onClick={select(decimalAmount(base*percentage/100,currency))},enabled=enabled,modifier=Modifier.weight(1f).height(40.dp),shape=RoundedCornerShape(100.dp),color=FFColors.surface) {
                Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("$percentage%",style=FFType.bodyMain,color=FFColors.onSurface)}
            }
        }
    }
}
