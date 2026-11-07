package com.logikawan.finflow.feature.goal

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logikawan.finflow.core.model.*
import com.logikawan.finflow.core.ui.*
import com.logikawan.finflow.core.util.*
import com.logikawan.finflow.core.i18n.text
import com.logikawan.finflow.i18n.*
import com.logikawan.finflow.feature.planning.OlloDashedProgress
import com.logikawan.finflow.feature.planning.OlloEmptyState
import com.logikawan.finflow.feature.planning.OlloFab
import com.logikawan.finflow.feature.planning.OlloMenuButton
import com.logikawan.finflow.feature.planning.OlloMenuItem
import com.logikawan.finflow.feature.planning.OlloPill
import com.logikawan.finflow.feature.planning.OlloProgress
import com.logikawan.finflow.feature.planning.OlloSegmented
import kotlin.math.abs
import kotlin.math.roundToInt

internal val GoalAccent = Color(0xFF2196F3)

internal fun fundedGoalAmount(goal:Goal)=if(goal.holdingAccountId==null)0L else (goal.savedMinor-goal.legacySavedMinor).coerceAtLeast(0)
internal fun legacyGoalAmount(goal:Goal)=if(goal.holdingAccountId==null)goal.savedMinor else goal.legacySavedMinor
internal fun goalMoney(value:Long,currency:String,preferences:AppPreferences)=if(preferences.hideBalances)"••••" else formatBalance(value,currency,preferences)
internal fun bucketName(type:String)=when(type){"emergency"->MR.strings.acc_bucket_emergency.text();"locked"->MR.strings.acc_bucket_locked.text();else->MR.strings.acc_bucket_standard.text()}
internal fun bucketIcon(type:String)=when(type){"emergency"->"warning_amber_rounded";"locked"->"lock_outline_rounded";else->"savings_rounded"}
private fun bucketTypeLabel(type:String)=when(type){"emergency"->MR.strings.acc_bucket_badge_emergency.text();"locked"->MR.strings.acc_bucket_badge_locked.text();else->MR.strings.acc_bucket_badge_standard.text()}
private fun percentText(fraction:Float):String{
    val tenths=(fraction*1000f).roundToInt()
    return "${tenths/10}.${abs(tenths%10)}"
}

@Composable
fun GoalScreen(data:FinanceData,edit:(Goal)->Unit,add:()->Unit) {
    val preferences=LocalPreferences.current
    var archived by rememberSaveable{mutableStateOf(false)}
    val entries=data.goals.filter{it.archived==archived}
    val currencies=if(entries.isEmpty()) listOf(data.profile?.baseCurrency ?: "IDR") else entries.map{it.currency}.distinct()
    Box(Modifier.fillMaxSize().testTag("goal-page")) {
        LazyColumn(Modifier.fillMaxSize().testTag("goal-list"),contentPadding=PaddingValues(start=16.dp,end=16.dp,top=8.dp,bottom=104.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            currencies.forEach{currency->
                item(key="goal-summary-$currency"){GoalSummary(entries.filter{it.currency==currency},currency,preferences)}
            }
            item { Text(if(archived)MR.strings.acc_bucket_archive.text() else MR.strings.acc_bucket_list.text(),style=FFType.sectionTitle,color=FFColors.onSurface) }
            if(entries.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(top=48.dp),horizontalAlignment=Alignment.CenterHorizontally){
                    FFIcon("hi_piggy",FFColors.onSurfaceVariant.copy(alpha=.5f),64.dp)
                    Spacer(Modifier.height(14.dp))
                    Text(if(archived)MR.strings.acc_no_archived_goals.text() else MR.strings.acc_no_goals.text(),style=FFType.bodyMain.copy(fontSize=15.5.sp),color=FFColors.onSurfaceVariant)
                }
            }
            items(entries,key={it.id}){goal->GoalCard(goal,preferences,edit)}
        }
        // Diletakkan setelah daftar agar versi tanpa App (uji) tetap dapat disentuh.
        TopBarActions {
            OlloMenuButton(listOf(OlloMenuItem("archived",if(archived)MR.strings.acc_show_active_goals.text() else MR.strings.acc_show_archive.text(),"archive_rounded")),tint=FFColors.onSurface){key->if(key=="archived")archived=!archived}
        }
        Box(Modifier.align(Alignment.BottomEnd).padding(16.dp)){OlloFab({add()},MR.strings.acc_create_goal.text())}
    }
}

@Composable
private fun GoalSummary(goals:List<Goal>,currency:String,preferences:AppPreferences) {
    val funded=goals.fold(0L){sum,goal->checkedAdd(sum,fundedGoalAmount(goal))}
    val legacy=goals.fold(0L){sum,goal->checkedAdd(sum,legacyGoalAmount(goal))}
    val target=goals.fold(0L){sum,goal->checkedAdd(sum,goal.targetMinor)}
    val fraction=if(target>0)(funded.toFloat()/target).coerceIn(0f,1f) else 0f
    val percent=fraction*100f
    DesignCard {
        Text(MR.strings.acc_total_savings.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        FitMoneyText(goalMoney(funded,currency,preferences),FFType.amountLg.copy(fontSize=33.sp,lineHeight=40.sp),FFColors.onSurface)
        Spacer(Modifier.height(18.dp))
        OlloDashedProgress(fraction)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(MR.strings.acc_collected.text(),style=FFType.metaData.copy(fontSize=10.5.sp),color=FFColors.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.weight(1f,fill=false)){FitMoneyText(goalMoney(funded,currency,preferences),FFType.metaData,FFColors.onSurface)}
                    if(!preferences.hideBalances){Spacer(Modifier.width(8.dp));OlloPill("+${percentText(fraction)}%",FFColors.income.copy(alpha=.12f),FFColors.income,icon="trending_up_rounded")}
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f),horizontalAlignment=Alignment.End) {
                Text(MR.strings.acc_remaining_target.text(),style=FFType.metaData.copy(fontSize=10.5.sp),color=FFColors.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth()){FitMoneyText(goalMoney((target-funded).coerceAtLeast(0),currency,preferences),FFType.metaData,FFColors.onSurface,TextAlign.End)}
            }
        }

        if(legacy>0){Spacer(Modifier.height(6.dp));Text(MR.strings.acc_legacy_progress_summary.text(goalMoney(legacy,currency,preferences)),style=FFType.metaData,color=FFColors.onSurfaceVariant)}
    }
}

@Composable
private fun GoalCard(goal:Goal,preferences:AppPreferences,edit:(Goal)->Unit) {
    val funded=fundedGoalAmount(goal)
    val legacy=legacyGoalAmount(goal)
    val fraction=if(goal.targetMinor>0)(funded.toFloat()/goal.targetMinor).coerceIn(0f,1f) else 0f
    val accent=GoalAccent
    val overdue=goal.targetDate.isNotBlank() && goal.targetDate<today().toString() && funded<goal.targetMinor
    DesignCard(modifier=Modifier.testTag("goal-${goal.id}"),onClick={edit(goal)}) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            GoalRing(fraction,accent,bucketIcon(goal.bucketType))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(goal.name,style=FFType.bodyMain.copy(fontSize=16.sp,lineHeight=22.sp),fontWeight=FontWeight.SemiBold,color=FFColors.onSurface,maxLines=1,overflow=TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(bucketTypeLabel(goal.bucketType),style=FFType.metaData.copy(fontSize=10.sp,lineHeight=14.sp,letterSpacing=1.sp),fontWeight=FontWeight.Medium,color=FFColors.onSurfaceVariant)
            }
            OlloMenuButton(listOf(OlloMenuItem("detail",MR.strings.acc_view_detail.text(),"chevron_right_rounded")),tint=FFColors.outline){edit(goal)}
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Bottom) {
            Box(Modifier.weight(1f)){FitMoneyText(goalMoney(funded,goal.currency,preferences),FFType.amountMd.copy(fontWeight=FontWeight.SemiBold),FFColors.onSurface)}
            Box(Modifier.weight(1f)){FitMoneyText(MR.strings.acc_of_target.text(goalMoney(goal.targetMinor,goal.currency,preferences)),FFType.metaData,FFColors.onSurfaceVariant,TextAlign.End)}
        }
        Spacer(Modifier.height(10.dp))
        OlloProgress(fraction,6.dp,accent)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            FFIcon("calendar_today_rounded",if(overdue)FFColors.expense else FFColors.outline,14.dp)
            Spacer(Modifier.width(6.dp))
            Text(if(goal.targetDate.isNotBlank())MR.strings.acc_due_date.text(shortDateLabel(goal.targetDate)) else MR.strings.acc_no_due_date.text(),style=FFType.metaData,color=if(overdue)FFColors.expense else FFColors.outline)
            Spacer(Modifier.weight(1f))
            Text("${(fraction*100).toInt()}%",style=FFType.metaData,fontWeight=FontWeight.SemiBold,color=accent)
        }
        if(legacy>0){Spacer(Modifier.height(8.dp));Text(MR.strings.acc_legacy_progress_card.text(goalMoney(legacy,goal.currency,preferences)),style=FFType.metaData,color=FFColors.onSurfaceVariant)}
    }
}

@Composable
private fun GoalRing(fraction:Float,accent:Color,icon:String) {
    Box(Modifier.size(50.dp),contentAlignment=Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(3.dp)) {
            val stroke=androidx.compose.ui.graphics.drawscope.Stroke(width=4.dp.toPx(),cap=StrokeCap.Round)
            drawCircle(accent.copy(alpha=.14f),style=stroke)
            if(fraction>0f)drawArc(accent,-90f,fraction*360f,false,style=stroke)
        }
        FFIcon(icon,accent,20.dp)
    }
}
