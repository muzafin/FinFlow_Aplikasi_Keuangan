package com.logikawan.finflow

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.logikawan.finflow.core.model.*
import com.logikawan.finflow.core.platform.NativeServices
import com.logikawan.finflow.core.ui.FFColors
import com.logikawan.finflow.core.ui.FinFlowTheme
import com.logikawan.finflow.core.util.today
import com.logikawan.finflow.feature.settings.ProfileScreen
import com.logikawan.finflow.feature.settings.BugReportForm
import com.logikawan.finflow.feature.level.LevelScreen
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Membandingkan halaman Profil FinFlow dengan susunan Ollo memakai data contoh terisolasi. */
class ProfileParityDeviceTest {
    @get:Rule val ui = createComposeRule()

    private fun fixture(): FinanceData {
        val day = today()
        fun tx(id: String, type: String, amount: Long, daysAgo: Int, category: String, title: String) =
            Transaction(id = id, type = type, amountMinor = amount, currency = "IDR", accountId = "prof-cash",
                categoryId = category, title = title, date = day.plus(DatePeriod(days = -daysAgo)).toString(),
                createdAt = day.toEpochDays().toLong() * 86_400_000L + 36_000_000L - daysAgo * 86_400_000L)
        return FinanceData(
            profile = Profile("profile-qa", "Muhammad", "muhammad@example.invalid"),
            accounts = listOf(Account(id = "prof-cash", name = "Cash"), Account(id = "prof-bank", name = "BCA", type = "bank", openingBalanceMinor = 2_000_000)),
            categories = defaultCategories,
            transactions = listOf(
                tx("prof-1", "income", 2_000_000, 2, "salary", "Gaji November"),
                tx("prof-2", "expense", 45_000, 1, "food", "Makan siang"),
                tx("prof-3", "expense", 25_000, 0, "transport", "Ojek online"),
            ),
            budgets = listOf(Budget("prof-budget", "food", 1_500_000, "IDR", day.toString().take(7))),
        )
    }

    @Test fun profileFollowsOlloStructureInLightTheme() {
        var nav: String? = null
        var loggedOut = false
        val data = fixture()
        val native = FakeNative()
        ui.setContent {
            CompositionLocalProvider(LocalPreferences provides AppPreferences(theme = "light")) {
                FinFlowTheme {
                    Box(Modifier.fillMaxSize().background(FFColors.background)) {
                        ProfileScreen(native, "Muhammad", "muhammad@example.invalid", data, { nav = it }, { loggedOut = true })
                    }
                }
            }
        }
        ui.mainClock.advanceTimeBy(600)
        ui.waitForIdle()
        ui.onNodeWithTag("profile-list").assertExists()
        ui.onNodeWithText("Profil").assertIsDisplayed()
        ui.onNodeWithText("Muhammad").assertIsDisplayed()
        ui.onNodeWithText("LEVEL").assertExists()
        ui.onNodeWithText("STREAK").assertExists()
        ui.onNodeWithText("HARI AKTIF").assertExists()
        ui.onNodeWithText("Akun & Data").assertExists()
        ui.onNodeWithText("Manajemen Akun").assertExists()
        ui.onNodeWithText("Cadangan & Pemulihan").assertExists()
        ui.onNodeWithText("Perangkat Terhubung").assertExists()
        ui.onNodeWithText("Ekspor Data").assertExists()
        ui.onNodeWithText("Upgrade ke Premium").assertDoesNotExist()
        screenshot("profile-ollo-light")

        ui.onNodeWithText("Manajemen Akun").performClick()
        ui.runOnIdle { assertEquals("Manajemen Akun", nav); assertFalse(loggedOut) }

        ui.onNodeWithTag("profile-list").performScrollToNode(hasText("Preferensi"))
        ui.mainClock.advanceTimeBy(300)
        ui.waitForIdle()
        ui.onNodeWithText("Preferensi").assertExists()
        ui.onNodeWithTag("profile-list").performScrollToNode(hasText("Format Saldo"))
        ui.mainClock.advanceTimeBy(300)
        ui.waitForIdle()
        screenshot("profile-ollo-light-bottom")
        ui.onNodeWithTag("profile-list").performScrollToNode(hasText("Hapus Data"))
        ui.mainClock.advanceTimeBy(300)
        ui.waitForIdle()
        screenshot("profile-ollo-light-help")
    }

    @Test fun profileKeepsOlloLayoutInDarkTheme() {
        val data = fixture()
        val native = FakeNative()
        ui.setContent {
            CompositionLocalProvider(LocalPreferences provides AppPreferences(theme = "dark")) {
                FinFlowTheme {
                    Box(Modifier.fillMaxSize().background(FFColors.background)) {
                        ProfileScreen(native, "Muhammad", "muhammad@example.invalid", data, {}, {})
                    }
                }
            }
        }
        ui.mainClock.advanceTimeBy(600)
        ui.waitForIdle()
        ui.onNodeWithTag("profile-list").assertExists()
        ui.onNodeWithText("Profil").assertIsDisplayed()
        ui.onNodeWithText("Muhammad").assertIsDisplayed()
        ui.onNodeWithText("LEVEL").assertExists()
        ui.onNodeWithText("STREAK").assertExists()
        ui.onNodeWithText("HARI AKTIF").assertExists()
        screenshot("profile-ollo-dark")
    }

    private fun screenshot(name: String) {
        ui.mainClock.advanceTimeBy(500)
        ui.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        android.os.SystemClock.sleep(350)
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "profile-qa").apply { mkdirs() }
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        checkNotNull(bitmap) { "Device screenshot unavailable" }
        FileOutputStream(File(directory, name + ".png")).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun bugReportShowsDeviceInformationBeforeSending() {
        ui.setContent { CompositionLocalProvider(LocalPreferences provides AppPreferences(theme="light")){FinFlowTheme { Surface(color=FFColors.background){Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp)){BugReportForm(FakeNative())} } }} }
        ui.onNodeWithTag("bug-report-form").assertExists()
        ui.onNodeWithText("Informasi Perangkat").assertIsDisplayed()
        ui.onNodeWithText("Fitur yang bermasalah").assertExists()
        screenshot("profile-bug-report")
    }

    @Test fun levelPageShowsProgressAndMonthlyRewardRules() {
        ui.setContent { CompositionLocalProvider(LocalPreferences provides AppPreferences(theme="light")){FinFlowTheme { Surface(color=FFColors.background){LevelScreen(fixture(),FakeNative(),{},{}) } }} }
        ui.onNodeWithTag("level-page").assertExists()
        ui.onNodeWithTag("level-back").assertExists()
        ui.onNodeWithTag("level-settings").assertExists()
        ui.onNodeWithText("Streak Terpanjang").assertExists()
        ui.onNodeWithText("Poin").assertExists()
        screenshot("profile-level-progress")
        ui.onNodeWithTag("level-page").performScrollToNode(hasText("Peringkat Bulanan (Segera Hadir)"))
        ui.onNodeWithText("Peringkat Bulanan (Segera Hadir)").assertIsDisplayed()
        ui.onNodeWithText("Juara 1 · Premium gratis 3 bulan").assertExists()
        ui.onNodeWithText("Juara 2 · Premium gratis 2 bulan").assertExists()
        ui.onNodeWithText("Juara 3 · Premium gratis 1 bulan").assertExists()
        ui.onNodeWithTag("level-leaderboard-disabled").assertExists()
        screenshot("profile-level-leaderboard")
    }

    private class FakeNative : NativeServices {
        override fun onboardingComplete() = false
        override fun setOnboardingComplete() {}
        override fun setNotificationImportUser(userId: String?) {}
        override fun pickCsv(result: (String?, String?) -> Unit) {}
        override fun shareCsv(name: String, content: String) {}
        override fun googleSignIn(result: (String?, String?) -> Unit) {}
        override fun unlock(result: (Boolean) -> Unit) { result(true) }
        override fun lockEnabled() = false
        override fun setLockEnabled(enabled: Boolean) {}
        override fun reminderEnabled() = false
        override fun reminderHour() = 20
        override fun reminderMinute() = 0
        override fun setReminder(enabled: Boolean, hour: Int, minute: Int, result: (String?) -> Unit) { result(null) }
        override fun budgetAlertsEnabled() = false
        override fun setBudgetAlerts(enabled: Boolean, result: (String?) -> Unit) { result(null) }
        override fun showBudgetAlert(userId: String, alertId: String, title: String, message: String) {}
    }
}
