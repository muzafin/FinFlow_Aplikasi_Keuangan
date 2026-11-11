package com.clonical.kasiva.feature.level

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonical.kasiva.core.i18n.menuLabel
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.model.FinanceData
import com.clonical.kasiva.core.platform.NativeServices
import com.clonical.kasiva.core.ui.*
import com.clonical.kasiva.feature.settings.RecordingAchievement
import com.clonical.kasiva.feature.settings.displayTitle
import com.clonical.kasiva.feature.settings.recordingProgress
import com.clonical.kasiva.i18n.*
import kotlinx.coroutines.launch

private data class LevelMetric(val label:String, val value:String, val icon:String, val color:Color, val background:Color)
private data class BadgeGroup(val title:String, val color:Color, val badges:List<RecordingAchievement>)

private fun rankName(level:Int):String = when(level) {
    1 -> MR.strings.rm_rank_rookie.text()
    2 -> MR.strings.rm_rank_smart.text()
    3 -> MR.strings.rm_rank_budget_master.text()
    4 -> MR.strings.rm_rank_finance_pro.text()
    5 -> MR.strings.rm_rank_wealth_builder.text()
    else -> MR.strings.rm_rank_legend.text()
}

private fun achievementIcon(title:String):String = when(title) {
    "Catatan Pertama" -> "edit_note_outlined"
    "Mulai Konsisten" -> "local_fire_department_outlined"
    "On Fire" -> "local_fire_department_outlined"
    "Tak Terhentikan" -> "trending_up_rounded"
    "Disiplin" -> "workspace_premium_outlined"
    "Legendaris" -> "emoji_events_outlined"
    "Anggaran Pertama" -> "receipt_long"
    "Tabungan Pertama" -> "savings_outlined"
    else -> "emoji_events_outlined"
}

// Halaman Level/Aktivitas gaya Ollo: avatar bercincin progres, kartu metrik, target kebebasan finansial,
// pameran lencana, dan penjelasan peringkat bulanan yang belum aktif.
@Composable
fun LevelScreen(data:FinanceData, native:NativeServices, back:()->Unit, onNavigate:(String)->Unit = {}) {
    val gamification=LocalGamification.current
    val progress=recordingProgress(data,gamification?.status)
    val displayName=data.profile?.name?.takeIf{it.isNotBlank()} ?: MR.strings.rm_user_fallback.text()
    val metrics=listOf(
        LevelMetric(MR.strings.rm_metric_streak.text(),progress.streak.toString(),"local_fire_department_outlined",Color(0xFFF57C00),Color(0xFFFFF1E2)),
        LevelMetric(MR.strings.rm_metric_longest_streak.text(),progress.longestStreak.toString(),"trending_up_rounded",Color(0xFFF44336),Color(0xFFFDEDEC)),
        LevelMetric(MR.strings.rm_metric_points.text(),progress.xp.toString(),"emoji_events_outlined",Color(0xFFF2B300),Color(0xFFFFF6DE)),
        LevelMetric(MR.strings.rm_metric_active_days.text(),progress.activeDays.toString(),"event_available_outlined",Color(0xFF2196F3),Color(0xFFE8F2FD)),
        LevelMetric(MR.strings.rm_metric_transactions.text(),progress.transactions.toString(),"receipt_long",Color(0xFF14B8A6),Color(0xFFE4F7F5)),
        LevelMetric(MR.strings.rm_metric_badges.text(),progress.achievements.count{it.earned}.toString(),"military_tech_outlined",Color(0xFF4CAF50),Color(0xFFEAF5EA)),
    )
    val streakTitles=setOf("Catatan Pertama","Mulai Konsisten","On Fire","Tak Terhentikan","Disiplin","Legendaris")
    val groups=listOf(
        BadgeGroup(MR.strings.rm_group_streak.text(),Color(0xFF2196F3),progress.achievements.filter{it.title in streakTitles}),
        BadgeGroup(menuLabel("Anggaran"),Color(0xFF3ED9BE),progress.achievements.filter{it.title=="Anggaran Pertama"}),
        BadgeGroup(MR.strings.rm_group_savings.text(),Color(0xFF4CAF50),progress.achievements.filter{it.title=="Tabungan Pertama"}),
    ).filter{it.badges.isNotEmpty()}

    Column(Modifier.fillMaxSize().background(FFColors.background).statusBarsPadding().verticalScroll(rememberScrollState()).testTag("level-page")) {
        Row(Modifier.fillMaxWidth().padding(start=4.dp,end=4.dp,top=20.dp).height(48.dp),verticalAlignment=Alignment.CenterVertically) {
            FFIconButton("chevron_left_rounded",back,Modifier.testTag("level-back"),tint=FFColors.onSurface)
            Spacer(Modifier.weight(1f))
            FFIconButton("settings_outlined",{onNavigate("Pengaturan")},Modifier.testTag("level-settings"),tint=FFColors.onSurface)
        }
        Column(Modifier.fillMaxWidth().padding(horizontal=24.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Spacer(Modifier.height(8.dp))
            val ringTrack=FFColors.outlineVariant
            val ringProgress=FFColors.onSurface
            Box(Modifier.size(120.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke=4.dp.toPx()
                    drawCircle(ringTrack,style=Stroke(stroke))
                    drawArc(ringProgress,-90f,(progress.levelXp.toFloat()/progress.levelSpan*360f).coerceIn(0f,360f),false,style=Stroke(stroke,cap=StrokeCap.Round))
                }
                ProfilePhoto(data.profile,native,Modifier.size(96.dp).clip(CircleShape).background(FFColors.primaryContainer).align(Alignment.Center))
                Surface(Modifier.align(Alignment.BottomCenter),shape=CircleShape,color=FFColors.onSurface) {
                    Text(MR.strings.rm_level_n.text(progress.level),Modifier.padding(horizontal=14.dp,vertical=4.dp),
                        style=FFType.bodyMain.copy(fontSize=14.sp,fontWeight=FontWeight.SemiBold,lineHeight=18.sp),color=FFColors.canvas)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(displayName,style=FFType.appTitle.copy(fontSize=24.sp,lineHeight=30.sp,fontWeight=FontWeight.Bold),textAlign=TextAlign.Center)
            Text(rankName(progress.level),style=FFType.bodyMain.copy(fontSize=16.sp,lineHeight=22.sp),color=FFColors.onSurfaceVariant,textAlign=TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(MR.strings.rm_xp_progress.text(progress.levelXp,progress.levelSpan),style=FFType.bodyMain.copy(fontSize=12.sp,fontWeight=FontWeight.SemiBold,lineHeight=16.sp),textAlign=TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                metrics.chunked(2).forEach{row->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        row.forEach{metric->MetricCard(metric,Modifier.weight(1f))}
                        if(row.size==1)Spacer(Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            FreedomTrackCard{onNavigate("Target")}
            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text(MR.strings.rm_achievements.text(),Modifier.weight(1f),style=FFType.appTitle)
                Text(MR.strings.rm_xp.text(progress.xp),style=FFType.metaData.copy(fontWeight=FontWeight.SemiBold),color=FFColors.onSurfaceVariant)
            }
            groups.forEach{group->
                Spacer(Modifier.height(18.dp))
                Text(group.title,Modifier.fillMaxWidth(),style=FFType.bodyMain.copy(fontSize=16.sp,fontWeight=FontWeight.SemiBold),color=FFColors.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))
                group.badges.chunked(3).forEach{row->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        row.forEach{badge->AchievementBadge(badge,group.color,Modifier.weight(1f))}
                        repeat(3-row.size){Spacer(Modifier.weight(1f))}
                    }
                    Spacer(Modifier.height(18.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            LeaderboardCard(gamification)
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun MetricCard(metric:LevelMetric,modifier:Modifier) {
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(FFColors.surface).padding(vertical=20.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(metric.background),contentAlignment=Alignment.Center){
            FFIcon(metric.icon,metric.color,24.dp)
        }
        Spacer(Modifier.height(12.dp))
        Text(metric.value,style=FFType.amountLg.copy(fontSize=22.sp,lineHeight=30.sp,fontWeight=FontWeight.Bold))
        Spacer(Modifier.height(4.dp))
        Text(metric.label,style=FFType.bodyMain,color=FFColors.onSurfaceVariant,textAlign=TextAlign.Center,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}

@Composable
private fun FreedomTrackCard(onSetTarget:()->Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(FFColors.surface).clickable(role=Role.Button,onClick=onSetTarget).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            FFIcon("flag",FFColors.onSurface,22.dp)
            Spacer(Modifier.width(10.dp))
            Text(MR.strings.rm_freedom_title.text(),Modifier.weight(1f),style=FFType.bodyMain.copy(fontSize=16.sp,fontWeight=FontWeight.SemiBold))
            Text(MR.strings.rm_freedom_set.text(),style=FFType.metaData.copy(fontSize=12.sp,fontWeight=FontWeight.SemiBold),color=FFColors.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))
            FFIcon("chevron_right_rounded",FFColors.onSurfaceVariant,16.dp)
        }
        Spacer(Modifier.height(10.dp))
        Text(MR.strings.rm_freedom_desc.text(),style=FFType.bodyMain,color=FFColors.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(FFColors.outlineVariant))
    }
}

@Composable
private fun AchievementBadge(badge:RecordingAchievement,color:Color,modifier:Modifier) {
    val earned=badge.earned
    Column(modifier,horizontalAlignment=Alignment.CenterHorizontally) {
        Box(Modifier.size(80.dp)) {
            Box(Modifier.fillMaxSize().clip(CircleShape).background(if(earned)color else FFColors.surfaceContainerHigh),contentAlignment=Alignment.Center){
                FFIcon(achievementIcon(badge.title),if(earned)Color.White else FFColors.outline,32.dp)
            }
            Surface(Modifier.align(Alignment.BottomCenter).offset(y=8.dp),shape=RoundedCornerShape(50),
                color=if(earned)FFColors.canvas else FFColors.surfaceContainerHigh,shadowElevation=if(earned)2.dp else 0.dp) {
                Text(MR.strings.rm_xp.text(badge.xp),Modifier.padding(horizontal=8.dp,vertical=2.dp),
                    style=FFType.metaData.copy(fontSize=12.sp,fontWeight=FontWeight.SemiBold),
                    color=if(earned)color else FFColors.outline)
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(badge.displayTitle,style=FFType.metaData.copy(fontSize=11.sp,lineHeight=14.sp,fontWeight=FontWeight.SemiBold),
            color=if(earned)FFColors.onSurface else FFColors.outline,textAlign=TextAlign.Center,maxLines=2,overflow=TextOverflow.Ellipsis)
    }
}

@Composable
private fun LeaderboardCard(state:GamificationState?) {
    val scope=androidx.compose.runtime.rememberCoroutineScope()
    androidx.compose.runtime.LaunchedEffect(state){ state?.loadLeaderboard() }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(FFColors.surface).padding(20.dp).testTag("level-leaderboard")) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            FFIcon("emoji_events_outlined",Color(0xFFF2B300),24.dp)
            Spacer(Modifier.width(8.dp))
            Text(MR.strings.lb_title.text(),Modifier.weight(1f),style=FFType.bodyMain.copy(fontSize=18.sp,fontWeight=FontWeight.SemiBold))
            if(state?.uid!=null) FFIconButton("hi_recurring",{ scope.launch{ state.refresh(); state.loadLeaderboard() } },Modifier.testTag("leaderboard-refresh"),tint=FFColors.onSurfaceVariant)
        }
        Spacer(Modifier.height(6.dp))
        Text(MR.strings.lb_desc.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant)
        Spacer(Modifier.height(14.dp))
        if(state?.uid==null) { Text(MR.strings.lb_local_only.text(),style=FFType.bodyMain,color=FFColors.onSurfaceVariant); return@Column }
        val board=state.leaderboard
        val status=state.status
        if(status!=null) {
            Text(MR.strings.lb_today_cap.text(status.todayCount,status.dailyCap),style=FFType.metaData,color=FFColors.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
        }
        when {
            board==null && state.leaderboardError!=null -> Text(MR.strings.lb_load_failed.text(),style=FFType.bodyMain,color=FFColors.expense)
            board==null -> Text(MR.strings.plan_loading.text(),style=FFType.bodyMain,color=FFColors.outline)
            else -> {
                Text(MR.strings.lb_month.text(com.clonical.kasiva.core.util.monthLabel(board.month),board.participants),style=FFType.metaData.copy(fontWeight=FontWeight.SemiBold),color=FFColors.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                if(board.rows.isEmpty()) Text(MR.strings.lb_empty.text(),style=FFType.bodyMain,color=FFColors.onSurfaceVariant,modifier=Modifier.padding(vertical=8.dp))
                board.rows.forEach{row->LeaderboardRowView(row)}
                val me=board.me
                if(me!=null && board.rows.none{it.me}) {
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(FFColors.outlineVariant))
                    Spacer(Modifier.height(6.dp))
                    LeaderboardRowView(me)
                }
                if(board.hidden) { Spacer(Modifier.height(6.dp)); Text(MR.strings.lb_hidden.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant) }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(MR.strings.lb_show_me.text(),style=FFType.bodyMain.copy(fontWeight=FontWeight.Medium))
                Text(MR.strings.lb_show_me_desc.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            androidx.compose.material3.Switch(status?.leaderboard ?: true,{visible->scope.launch{state.setVisible(visible)}},enabled=!state.busy && status!=null,
                modifier=Modifier.testTag("leaderboard-visibility"))
        }
    }
}

@Composable
private fun LeaderboardRowView(row:LeaderboardRow) {
    val medal=when(row.rank){1->Color(0xFFF2B300);2->Color(0xFF9EA7B3);3->Color(0xFFC77B30);else->null}
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if(row.me)FFColors.surfaceContainerHigh else Color.Transparent)
        .padding(horizontal=10.dp,vertical=10.dp).testTag(if(row.me)"leaderboard-me" else "leaderboard-row"),verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(medal?.copy(alpha=.18f) ?: FFColors.surfaceContainer),contentAlignment=Alignment.Center) {
            Text("${row.rank}",style=FFType.bodyMain.copy(fontSize=13.sp,fontWeight=FontWeight.Bold),color=medal ?: FFColors.onSurface)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(if(row.me) "${row.name} (${MR.strings.lb_you.text()})" else row.name,style=FFType.bodyMain.copy(fontWeight=FontWeight.SemiBold),maxLines=1,overflow=TextOverflow.Ellipsis)
            Text(MR.strings.lb_row.text(row.xp,row.transactions,row.activeDays),style=FFType.metaData,color=FFColors.onSurfaceVariant,maxLines=1)
        }
    }
}

