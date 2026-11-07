package com.clonical.kasiva

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.clonical.kasiva.core.platform.AppLifecycle
import com.clonical.kasiva.core.platform.NativeServices
import com.clonical.kasiva.feature.auth.OlloSetupFlow
import com.clonical.kasiva.feature.auth.SetupResult
import com.clonical.kasiva.feature.auth.setupStrings
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class OlloSetupDeviceTest {
    @get:Rule val ui = createComposeRule()

    @Test fun setupFlowWalksEveryStepAndCapturesResult() {
        var captured: SetupResult? = null
        var signedIn = false
        val native = SetupFakeNative()
        val lifecycle = AppLifecycle()
        ui.setContent {
            OlloSetupFlow(
                native = native,
                lifecycle = lifecycle,
                busy = false,
                onComplete = { captured = it },
                onSignIn = { signedIn = true },
                onRestore = { },
            )
        }

        ui.onNodeWithTag("setup-flow").assertIsDisplayed()
        ui.onNodeWithTag("setup-step-welcome").assertIsDisplayed()
        ui.onNodeWithTag("setup-command-google").performScrollTo().performClick()
        ui.runOnIdle { assertTrue(signedIn) }
        screenshot("setup-1-welcome")

        // Nama lengkap wajib: Next tidak boleh lanjut dan error tampil inline.
        ui.onNodeWithTag("setup-next").performClick()
        ui.waitForIdle()
        ui.onNodeWithTag("setup-step-welcome").assertIsDisplayed()
        ui.onNodeWithTag("setup-name").performScrollTo()
        ui.waitForIdle()
        ui.onNodeWithTag("setup-name-error", useUnmergedTree = true).assertIsDisplayed()
        ui.onNodeWithTag("setup-name-error", useUnmergedTree = true).assertTextEquals(setupStrings().nameRequired)
        screenshot("setup-welcome-error")

        ui.onNodeWithTag("setup-name").performTextInput("Pengguna Uji")
        ui.waitForIdle()
        ui.onNodeWithTag("setup-name-error", useUnmergedTree = true).assertDoesNotExist()
        ui.onNodeWithTag("setup-next").performClick()
        ui.waitForIdle()

        // Bahasa & mata uang tidak lagi ditanyakan: mengikuti HP (SetupFakeNative: id / IDR).
        ui.onNodeWithTag("setup-step-language").assertDoesNotExist()
        ui.onNodeWithTag("setup-step-currency").assertDoesNotExist()
        ui.runOnIdle { assertTrue("Tombol back harus kembali ke langkah sebelumnya", lifecycle.back()) }
        ui.waitForIdle()
        ui.onNodeWithTag("setup-step-welcome").assertIsDisplayed()
        ui.onNodeWithTag("setup-next").performClick()
        ui.waitForIdle()

        ui.onNodeWithTag("setup-step-notification").assertIsDisplayed()
        screenshot("setup-4-notification")
        ui.onAllNodes(isToggleable())[0].performClick()
        ui.waitForIdle()
        ui.onNodeWithTag("setup-next").performClick()
        ui.waitForIdle()

        ui.onNodeWithTag("setup-step-style").assertIsDisplayed()
        screenshot("setup-5-style")
        listOf("theme-dark", "card-emerald", "menu-ocean", "income-teal", "expense-orange").forEach { tag ->
            ui.onNodeWithTag("setup-scroll").performScrollToNode(hasTestTag(tag))
            ui.onNodeWithTag(tag).performClick()
            ui.waitForIdle()
        }
        ui.onNodeWithTag("setup-next").performClick()
        ui.waitForIdle()

        ui.onNodeWithTag("setup-step-wallet").assertIsDisplayed()
        ui.onNodeWithTag("setup-scroll").performScrollToNode(hasTestTag("setup-balance"))
        ui.onNodeWithTag("setup-balance").performTextInput("0")
        ui.waitForIdle()
        screenshot("setup-6-wallet")
        ui.onNodeWithTag("setup-next").performClick()
        ui.waitForIdle()
        ui.waitUntil(10_000) { captured != null }

        ui.runOnIdle {
            assertNotNull("Wizard harus menyerahkan SetupResult", captured)
            val result = requireNotNull(captured)
            assertEquals("Pengguna Uji", result.name)
            assertEquals("id", result.language)
            assertEquals("IDR", result.currency)
            assertEquals("Cash", result.walletName)
            assertEquals(0L, result.openingBalanceMinor)
            assertNull(result.photoBytes)
            assertEquals("system", result.preferences.language)
            assertEquals("dark", result.preferences.theme)
            assertEquals("emerald", result.preferences.cardGradient)
            assertEquals("ocean", result.preferences.menuTheme)
            assertEquals(0xFF009688L, result.preferences.incomeColor)
            assertEquals(0xFFFF9800L, result.preferences.expenseColor)
        }
        File(screenshotDirectory(), "setup-1-welcome.png").let { assertTrue("Screenshot langkah pertama harus tersimpan", it.isFile) }
        File(screenshotDirectory(), "setup-welcome-error.png").let { assertTrue("Screenshot error nama lengkap harus tersimpan", it.isFile) }
    }

    private fun screenshot(name: String) {
        ui.mainClock.advanceTimeBy(500)
        ui.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "setup-qa").apply { mkdirs() }
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        checkNotNull(bitmap) { "Device screenshot unavailable" }
        FileOutputStream(File(directory, "$name.png")).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    private fun screenshotDirectory(): File =
        File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "setup-qa")
}

private class SetupFakeNative : NativeServices {
    private val settings = mutableMapOf<String, String>()
    private var reminder = false
    override fun deviceLanguage(): String = "id"
    override fun deviceCurrency(): String = "IDR"
    override fun readSetting(key: String): String? = settings[key]
    override fun writeSetting(key: String, value: String) { settings[key] = value }
    override fun onboardingComplete() = false
    override fun setOnboardingComplete() {}
    override fun setNotificationImportUser(userId: String?) {}
    override fun pickCsv(result: (String?, String?) -> Unit) {}
    override fun shareCsv(name: String, content: String) {}
    override fun googleSignIn(result: (String?, String?) -> Unit) {}
    override fun unlock(result: (Boolean) -> Unit) { result(true) }
    override fun lockEnabled() = false
    override fun setLockEnabled(enabled: Boolean) {}
    override fun reminderEnabled() = reminder
    override fun reminderHour() = 20
    override fun reminderMinute() = 0
    override fun setReminder(enabled: Boolean, hour: Int, minute: Int, result: (String?) -> Unit) {
        reminder = enabled
        result(null)
    }
    override fun budgetAlertsEnabled() = false
    override fun setBudgetAlerts(enabled: Boolean, result: (String?) -> Unit) { result(null) }
    override fun showBudgetAlert(userId: String, alertId: String, title: String, message: String) {}
}
