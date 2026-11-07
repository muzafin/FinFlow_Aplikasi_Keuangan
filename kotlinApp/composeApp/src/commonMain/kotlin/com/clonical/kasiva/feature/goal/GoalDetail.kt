package com.clonical.kasiva.feature.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonical.kasiva.core.model.*
import com.clonical.kasiva.core.ui.*
import com.clonical.kasiva.core.util.*
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.i18n.*
import com.clonical.kasiva.feature.planning.OlloMenuButton
import com.clonical.kasiva.feature.planning.OlloMenuItem
import com.clonical.kasiva.feature.planning.OlloPrimaryButton
import com.clonical.kasiva.feature.planning.OlloProgress
import com.clonical.kasiva.feature.planning.OlloSecondaryButton

@Composable
fun GoalDetail(goal:Goal,edit:()->Unit,data:FinanceData=FinanceData(),deposit:(()->Unit)?=null,withdraw:(()->Unit)?=null,reverse:((FinancialEvent)->Unit)?=null) {
    val preferences=LocalPreferences.current
    val funded=fundedGoalAmount(goal)
    val legacy=legacyGoalAmount(goal)
    val saved=goal.savedMinor
    val progress=if(goal.targetMinor>0)(saved.toFloat()/goal.targetMinor).coerceIn(0f,1f) else 0f
    val remaining=(goal.targetMinor-saved).coerceAtLeast(0)
    val canWithdraw=goal.holdingAccountId!=null && saved>legacy
    val overdue=goal.targetDate.isNotBlank() && goal.targetDate<today().toString() && funded<goal.targetMinor
    val history=data.financialEvents.filter{it.referenceId==goal.id && it.kind in listOf("goal_deposit","goal_withdrawal")}.sortedByDescending{it.createdAt}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(start=16.dp,end=16.dp,top=8.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        item {
            DesignCard {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.size(52.dp).clip(CircleShape).background(GoalAccent.copy(alpha=.12f)),contentAlignment=Alignment.Center){FFIcon(bucketIcon(goal.bucketType),GoalAccent,26.dp)}
                    Spacer(Modifier.weight(1f))
                    val menu=buildList{
                        add(OlloMenuItem("edit",MR.strings.acc_edit_goal.text(),"edit_rounded"))
                        if(!goal.archived && deposit!=null)add(OlloMenuItem("deposit",MR.strings.acc_add_funds.text(),"arrow_downward_rounded"))
                        if(!goal.archived && canWithdraw && withdraw!=null)add(OlloMenuItem("withdraw",MR.strings.acc_withdraw_funds.text(),"arrow_upward_rounded"))
                    }
                    OlloMenuButton(menu,tint=FFColors.onSurface){key->when(key){"edit"->edit();"deposit"->deposit?.invoke();"withdraw"->withdraw?.invoke()}}
                }
                Spacer(Modifier.height(22.dp))
                Text(goal.name,style=FFType.appTitle,color=FFColors.onSurface,maxLines=2,overflow=TextOverflow.Ellipsis)
                Spacer(Modifier.height(10.dp))
                FitMoneyText(goalMoney(saved,goal.currency,preferences),FFType.amountXl.copy(fontSize=34.sp,lineHeight=42.sp),FFColors.onSurface)
                Spacer(Modifier.height(4.dp))
                FitMoneyText(MR.strings.acc_of_target.text(goalMoney(goal.targetMinor,goal.currency,preferences)),FFType.bodyMain.copy(fontSize=15.sp,lineHeight=20.sp),FFColors.onSurfaceVariant)
                if(goal.targetDate.isNotBlank()){
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment=Alignment.CenterVertically){
                        FFIcon("calendar_today_rounded",if(overdue)FFColors.expense else FFColors.outline,15.dp)
                        Spacer(Modifier.width(6.dp))
                        Text(MR.strings.acc_due_date.text(shortDateLabel(goal.targetDate)),style=FFType.metaData,color=if(overdue)FFColors.expense else FFColors.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(22.dp))
                OlloProgress(progress,10.dp)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    Text(MR.strings.acc_percent_done.text((progress*100).toInt()),style=FFType.metaData,color=FFColors.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.weight(1f,fill=false)){FitMoneyText(MR.strings.acc_remaining_amount.text(goalMoney(remaining,goal.currency,preferences)),FFType.metaData,FFColors.onSurfaceVariant,TextAlign.End)}
                }
                if(!goal.archived){
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){
                        if(canWithdraw && withdraw!=null)OlloSecondaryButton(MR.strings.acc_withdraw_funds.text(),Modifier.weight(1f)){withdraw()}
                        OlloPrimaryButton(if(goal.holdingAccountId==null)MR.strings.acc_add_funds.text() else MR.strings.acc_deposit_funds.text(),Modifier.weight(1f)){deposit?.invoke() ?: edit()}
                    }
                }
            }
        }
        if(legacy>0)item{Text(MR.strings.acc_legacy_progress_detail.text(goalMoney(legacy,goal.currency,preferences)),style=FFType.metaData,color=FFColors.onSurfaceVariant)}
        item{Text(MR.strings.acc_movement_history.text(),style=FFType.sectionTitle,color=FFColors.onSurface)}
        if(history.isEmpty())item{Text(MR.strings.acc_no_movements.text(),style=FFType.bodyMain,color=FFColors.outline,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth().padding(vertical=16.dp))}
        items(history,key={it.id}){event->GoalHistoryRow(event,data,preferences,reverse)}
    }
}

@Composable
private fun GoalHistoryRow(event:FinancialEvent,data:FinanceData,preferences:AppPreferences,reverse:((FinancialEvent)->Unit)?) {
    val deposit=event.kind=="goal_deposit"
    val accent=if(deposit)FFColors.income else FFColors.expense
    val walletId=if(deposit)event.accountId else data.transactions.find{it.id==event.transactionId}?.toAccountId
    val walletName=walletId?.let{id->data.accounts.find{it.id==id}?.name}.orEmpty()
    DesignCard {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(accent.copy(alpha=.12f)),contentAlignment=Alignment.Center){FFIcon(if(deposit)"arrow_downward_rounded" else "arrow_upward_rounded",accent,20.dp)}
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(shortDateLabel(event.date),style=FFType.bodyMain.copy(fontSize=15.sp,lineHeight=20.sp),fontWeight=FontWeight.SemiBold,color=FFColors.onSurface)
                Text(if(walletName.isBlank())event.title else if(deposit)MR.strings.acc_from_wallet.text(walletName) else MR.strings.acc_to_wallet.text(walletName),style=FFType.metaData,color=FFColors.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
            }
            Box(Modifier.weight(1f)){FitMoneyText(goalMoney(event.amountMinor,event.currency,preferences),FFType.amountMd,accent,TextAlign.End)}
            if(reverse!=null)OlloMenuButton(listOf(OlloMenuItem("reverse",MR.strings.acc_reverse_entry.text(),"delete_outline_rounded"))){key->if(key=="reverse")reverse(event)}
        }
    }
}
