package com.clonical.kasiva.feature.settings

import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.model.FinanceData
import com.clonical.kasiva.i18n.*
import com.clonical.kasiva.core.util.today
import kotlinx.datetime.*

data class RecordingAchievement(val title: String, val description: String, val xp: Int, val earned: Boolean)
data class RecordingProgress(val transactions: Int, val activeDays: Int, val streak: Int, val longestStreak: Int,
    val xp: Int, val achievements: List<RecordingAchievement>, val verified: Boolean = false) {
    // Kurva sama dengan server: Lvl 2 = 100 XP, Lvl 3 = 300, Lvl 4 = 600, … (dulu 500 XP per level → macet di Lvl 1).
    val level get() = com.clonical.kasiva.feature.level.LevelCurve.levelFor(xp)
    val levelStartXp get() = com.clonical.kasiva.feature.level.LevelCurve.start(level)
    val nextLevelXp get() = com.clonical.kasiva.feature.level.LevelCurve.start(level + 1)
    val levelXp get() = xp - levelStartXp
    val levelSpan get() = nextLevelXp - levelStartXp
}

/**
 * Akun cloud: XP transaksi, streak, hari aktif dihitung server (tidak turun saat transaksi dihapus, batas 20/hari);
 * bonus lencana tetap dihitung dari data. Mode lokal / belum ada status server: dihitung di perangkat.
 */
fun recordingProgress(data: FinanceData, server: com.clonical.kasiva.feature.level.GamificationStatus?, date: LocalDate = today()): RecordingProgress {
    val local = recordingProgress(data, date)
    server ?: return local
    val badges = local.achievements.filter { it.earned }.sumOf { it.xp }
    return RecordingProgress(server.transactions, server.activeDays, server.streak, maxOf(server.longestStreak, 0),
        server.xp + badges, local.achievements, verified = true)
}

fun recordingProgress(data: FinanceData, date: LocalDate = today()): RecordingProgress {
    val records = data.transactions.filter { !it.scheduled && it.date <= date.toString() }.distinctBy { it.id }
    val days = records.map { LocalDate.parse(it.date) }.distinct().sorted()
    var longest = 0
    var run = 0
    days.forEachIndexed { index, day ->
        run = if (index > 0 && days[index - 1].plus(DatePeriod(days = 1)) == day) run + 1 else 1
        longest = maxOf(longest, run)
    }
    val daySet = days.toSet()
    var cursor = if (date in daySet) date else date.minus(DatePeriod(days = 1))
    var streak = 0
    while (cursor in daySet) { streak++; cursor = cursor.minus(DatePeriod(days = 1)) }
    val achievements = listOf(
        RecordingAchievement("Catatan Pertama", "Catat transaksi pertama", 10, records.isNotEmpty()),
        RecordingAchievement("Mulai Konsisten", "Mencatat 3 hari berturut-turut", 20, longest >= 3),
        RecordingAchievement("On Fire", "Mencatat 7 hari berturut-turut", 50, longest >= 7),
        RecordingAchievement("Tak Terhentikan", "Mencatat 14 hari berturut-turut", 100, longest >= 14),
        RecordingAchievement("Disiplin", "Mencatat 30 hari berturut-turut", 200, longest >= 30),
        RecordingAchievement("Legendaris", "Mencatat 100 hari berturut-turut", 500, longest >= 100),
        RecordingAchievement("Anggaran Pertama", "Buat anggaran pertama", 20, data.budgets.isNotEmpty()),
        RecordingAchievement("Tabungan Pertama", "Buat target tabungan pertama", 20, data.goals.isNotEmpty()),
    )
    // shortcut: progres dihitung dari jurnal yang tersedia; ledger XP permanen diperlukan sebelum kompetisi server.
    return RecordingProgress(records.size, days.size, streak, longest, records.size * 5 + achievements.filter { it.earned }.sumOf { it.xp }, achievements)
}

/*
 * Judul pencapaian berbahasa Indonesia juga dipakai sebagai pengenal (ikon, pengelompokan lencana),
 * jadi data tidak diubah; layar memakai label terjemahan berikut.
 */
val RecordingAchievement.displayTitle: String get() = achievementTitleLabel(title)
val RecordingAchievement.displayDescription: String get() = achievementDescriptionLabel(title, description)

fun achievementTitleLabel(title: String): String = when (title) {
    "Catatan Pertama" -> MR.strings.rm_ach_first_record.text()
    "Mulai Konsisten" -> MR.strings.rm_ach_consistent.text()
    "On Fire" -> MR.strings.rm_ach_on_fire.text()
    "Tak Terhentikan" -> MR.strings.rm_ach_unstoppable.text()
    "Disiplin" -> MR.strings.rm_ach_disciplined.text()
    "Legendaris" -> MR.strings.rm_ach_legendary.text()
    "Anggaran Pertama" -> MR.strings.rm_ach_first_budget.text()
    "Tabungan Pertama" -> MR.strings.rm_ach_first_goal.text()
    else -> title
}

fun achievementDescriptionLabel(title: String, fallback: String): String = when (title) {
    "Catatan Pertama" -> MR.strings.rm_ach_first_record_desc.text()
    "Mulai Konsisten" -> MR.strings.rm_ach_streak_days_desc.text(3)
    "On Fire" -> MR.strings.rm_ach_streak_days_desc.text(7)
    "Tak Terhentikan" -> MR.strings.rm_ach_streak_days_desc.text(14)
    "Disiplin" -> MR.strings.rm_ach_streak_days_desc.text(30)
    "Legendaris" -> MR.strings.rm_ach_streak_days_desc.text(100)
    "Anggaran Pertama" -> MR.strings.rm_ach_first_budget_desc.text()
    "Tabungan Pertama" -> MR.strings.rm_ach_first_goal_desc.text()
    else -> fallback
}
