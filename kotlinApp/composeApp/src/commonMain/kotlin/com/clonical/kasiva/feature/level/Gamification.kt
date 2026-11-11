package com.clonical.kasiva.feature.level

import androidx.compose.runtime.*
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.platform.NativeServices
import com.clonical.kasiva.i18n.*
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.functions.functions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

/** Ringkasan XP dari server (callable `gamificationStatus`); aturan ada di firebase/functions/gamification.js. */
@Serializable
data class GamificationStatus(
    val xp: Int = 0, val level: Int = 1, val levelStartXp: Int = 0, val nextLevelXp: Int = 100,
    val monthXp: Int = 0, val monthTransactions: Int = 0, val monthActiveDays: Int = 0,
    val transactions: Int = 0, val activeDays: Int = 0, val streak: Int = 0, val longestStreak: Int = 0,
    val todayCount: Int = 0, val dailyCap: Int = 20, val leaderboard: Boolean = true, val month: String = "",
)

@Serializable
data class LeaderboardRow(val rank: Int = 0, val me: Boolean = false, val name: String = "", val xp: Int = 0,
    val transactions: Int = 0, val activeDays: Int = 0, val level: Int = 1)

@Serializable
data class Leaderboard(val month: String = "", val rows: List<LeaderboardRow> = emptyList(), val me: LeaderboardRow? = null,
    val participants: Int = 0, val hidden: Boolean = false)

@Serializable private class MonthRequest(val month: String? = null)
@Serializable private class VisibilityRequest(val visible: Boolean)

/** Kurva level yang sama dengan server: total XP untuk mencapai level L = 50·L·(L−1). */
object LevelCurve {
    fun start(level: Int): Int = 50 * level * (level - 1)
    fun levelFor(xp: Int): Int { var level = 1; while (start(level + 1) <= xp) level++; return level }
}

/**
 * Status XP akun cloud. `null` di mode lokal (XP dihitung di perangkat, tanpa peringkat).
 * Disimpan di setting perangkat agar level tetap tampil saat offline.
 */
class GamificationState(private val native: NativeServices, val uid: String?) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val cacheKey = "gamification.status.$uid"
    private val functions get() = Firebase.functions("asia-southeast2")
    var status by mutableStateOf(uid?.let { cached() })
        private set
    var leaderboard by mutableStateOf<Leaderboard?>(null)
        private set
    var leaderboardError by mutableStateOf<String?>(null)
        private set
    var busy by mutableStateOf(false)
        private set

    private fun cached(): GamificationStatus? =
        native.readSetting(cacheKey)?.let { runCatching { json.decodeFromString<GamificationStatus>(it) }.getOrNull() }

    private fun remember(value: GamificationStatus) {
        status = value
        runCatching { native.writeSetting(cacheKey, json.encodeToString(value)) }
    }

    suspend fun refresh() {
        if (uid == null || !native.isOnline()) return
        try { remember(functions.httpsCallable("gamificationStatus", 30.seconds)().data<GamificationStatus>()) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { native.reportError("gamification_status", e) }
    }

    suspend fun loadLeaderboard() {
        if (uid == null) return
        if (!native.isOnline()) { leaderboardError = MR.strings.core_error_need_internet.text(); return }
        busy = true
        try {
            leaderboard = functions.httpsCallable("monthlyLeaderboard", 30.seconds)(MonthRequest()).data<Leaderboard>()
            leaderboardError = null
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { native.reportError("leaderboard", e); leaderboardError = e.message }
        finally { busy = false }
    }

    suspend fun setVisible(visible: Boolean) {
        if (uid == null) return
        busy = true
        try {
            remember(functions.httpsCallable("setLeaderboardVisibility", 30.seconds)(VisibilityRequest(visible)).data<GamificationStatus>())
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { native.reportError("leaderboard_visibility", e); leaderboardError = e.message }
        finally { busy = false }
        loadLeaderboard()
    }
}

val LocalGamification = staticCompositionLocalOf<GamificationState?> { null }

/** Dibuat sekali per akun; status diperbarui saat dibuka dan beberapa detik setelah jumlah transaksi berubah (XP diberikan trigger server). */
@Composable
fun rememberGamification(native: NativeServices, uid: String?, transactionCount: Int): GamificationState {
    val state = remember(uid) { GamificationState(native, uid) }
    LaunchedEffect(state, transactionCount) {
        delay(if (state.status == null) 0 else 4_000)
        state.refresh()
    }
    return state
}
