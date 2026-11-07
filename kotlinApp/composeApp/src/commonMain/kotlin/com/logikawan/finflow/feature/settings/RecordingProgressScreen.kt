package com.logikawan.finflow.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.logikawan.finflow.core.model.FinanceData
import com.logikawan.finflow.core.platform.NativeServices
import com.logikawan.finflow.core.i18n.text
import com.logikawan.finflow.core.ui.*
import com.logikawan.finflow.i18n.*

@Composable
fun RecordingProgressSummary(data: FinanceData, native: NativeServices) {
    val progress = recordingProgress(data)
    Column(Modifier.fillMaxWidth().testTag("recording-progress"), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val track=FFColors.outlineVariant
        val foreground=FFColors.onSurface
        Box(Modifier.size(136.dp),contentAlignment=Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(track,style=Stroke(4.dp.toPx()))
                drawArc(foreground,-90f,progress.levelXp/500f*360,false,style=Stroke(4.dp.toPx()))
            }
            ProfilePhoto(data.profile,native,Modifier.size(112.dp).clip(CircleShape))
            Surface(Modifier.align(Alignment.BottomCenter),shape=CircleShape,color=foreground){Text(MR.strings.rm_level_n.text(progress.level),Modifier.padding(horizontal=16.dp,vertical=6.dp),color=FFColors.canvas,style=FFType.bodyMain)}
        }
        Text(data.profile?.name.orEmpty(), style = FFType.appTitle, textAlign = TextAlign.Center)
        Text(if (progress.level == 1) MR.strings.rm_rank_rookie.text() else MR.strings.rm_rank_consistent.text(), style = FFType.bodyMain)
        Text(MR.strings.rm_xp_progress.text(progress.levelXp), style = FFType.metaData)
        listOf(Triple(MR.strings.rm_metric_streak.text(), progress.streak, "local_fire_department"),
            Triple(MR.strings.rm_metric_longest_streak.text(), progress.longestStreak, "trending_up"),
            Triple(MR.strings.rm_metric_points.text(), progress.xp, "emoji_events_outlined"),
            Triple(MR.strings.rm_metric_active_days.text(), progress.activeDays, "event_available"),
            Triple(MR.strings.rm_metric_transactions.text(), progress.transactions, "receipt_long"),
            Triple(MR.strings.rm_metric_badges.text(), progress.achievements.count { it.earned }, "military_tech")).chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (title, value, icon) -> DesignCard(Modifier.weight(1f)) {
                    Column(Modifier.fillMaxWidth().heightIn(min=140.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        Surface(shape=CircleShape,color=FFColors.primaryContainer){Box(Modifier.size(44.dp),contentAlignment=Alignment.Center){FFIcon(icon,FFColors.primary,26.dp)}}
                        Text(value.toString(), style = FFType.amountLg)
                        Text(title, style = FFType.bodyMain,textAlign=TextAlign.Center)
                    }
                } }
            }
        }
        DesignCard {
            Text(MR.strings.rm_rp_lb_title.text(), style = FFType.sectionTitle)
            Text(MR.strings.rm_rp_lb_soon.text(), style = FFType.bodyMain)
            Text(MR.strings.rm_rp_lb_desc.text(), style = FFType.metaData)
            Text(MR.strings.rm_rp_prizes.text(), style = FFType.bodyMain)
        }
        Text(MR.strings.rm_achievements.text(), Modifier.fillMaxWidth(), style = FFType.sectionTitle)
        progress.achievements.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { badge -> DesignCard(Modifier.weight(1f)) {
                    FFIcon(if (badge.earned) "emoji_events_outlined" else "lock_outline", if (badge.earned) FFColors.primary else FFColors.outline)
                    Text(MR.strings.rm_xp.text(badge.xp), style = FFType.metaData)
                    Text(badge.displayTitle, style = FFType.bodyMain)
                    Text(badge.displayDescription, style = FFType.metaData, color = FFColors.onSurfaceVariant)
                } }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Text(MR.strings.rm_rp_footer.text(), style = FFType.metaData, color = FFColors.onSurfaceVariant)
    }
}
