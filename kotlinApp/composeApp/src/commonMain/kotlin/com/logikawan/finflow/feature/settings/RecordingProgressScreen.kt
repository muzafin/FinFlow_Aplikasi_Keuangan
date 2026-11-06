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
import com.logikawan.finflow.core.ui.*

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
            Surface(Modifier.align(Alignment.BottomCenter),shape=CircleShape,color=foreground){Text("Level ${progress.level}",Modifier.padding(horizontal=16.dp,vertical=6.dp),color=FFColors.canvas,style=FFType.bodyMain)}
        }
        Text(data.profile?.name.orEmpty(), style = FFType.appTitle, textAlign = TextAlign.Center)
        Text(if (progress.level == 1) "Rookie Saver" else "Pencatat Konsisten", style = FFType.bodyMain)
        Text("${progress.levelXp} / 500 XP", style = FFType.metaData)
        listOf("Streak" to progress.streak, "Streak Terpanjang" to progress.longestStreak,
            "Poin" to progress.xp, "Hari Aktif" to progress.activeDays,
            "Transaksi" to progress.transactions, "Lencana" to progress.achievements.count { it.earned }).chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (title, value) -> DesignCard(Modifier.weight(1f)) {
                    Column(Modifier.fillMaxWidth().heightIn(min=140.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        val icon=when(title){"Streak"->"local_fire_department";"Streak Terpanjang"->"trending_up";"Poin"->"emoji_events_outlined";"Hari Aktif"->"event_available";"Transaksi"->"receipt_long";else->"military_tech"}
                        Surface(shape=CircleShape,color=FFColors.primaryContainer){Box(Modifier.size(44.dp),contentAlignment=Alignment.Center){FFIcon(icon,FFColors.primary,26.dp)}}
                        Text(value.toString(), style = FFType.amountLg)
                        Text(title, style = FFType.bodyMain,textAlign=TextAlign.Center)
                    }
                } }
            }
        }
        DesignCard {
            Text("Leaderboard Bulanan", style = FFType.sectionTitle)
            Text("Segera hadir · Global dan per negara", style = FFType.bodyMain)
            Text("Peringkat dihitung dari aktivitas pencatatan setiap bulan. Hadiah diberikan admin FinFlow setelah verifikasi.", style = FFType.metaData)
            Text("🥇 Peringkat 1 · Premium gratis 3 bulan\n🥈 Peringkat 2 · Premium gratis 2 bulan\n🥉 Peringkat 3 · Premium gratis 1 bulan", style = FFType.bodyMain)
        }
        Text("Pencapaian", Modifier.fillMaxWidth(), style = FFType.sectionTitle)
        progress.achievements.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { badge -> DesignCard(Modifier.weight(1f)) {
                    FFIcon(if (badge.earned) "emoji_events_outlined" else "lock_outline", if (badge.earned) FFColors.primary else FFColors.outline)
                    Text("${badge.xp} XP", style = FFType.metaData)
                    Text(badge.title, style = FFType.bodyMain)
                    Text(badge.description, style = FFType.metaData, color = FFColors.onSurfaceVariant)
                } }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Text("5 XP per transaksi aktual, ditambah bonus pencapaian. Progres lokal mengikuti jurnal yang masih tersimpan.", style = FFType.metaData, color = FFColors.onSurfaceVariant)
    }
}
