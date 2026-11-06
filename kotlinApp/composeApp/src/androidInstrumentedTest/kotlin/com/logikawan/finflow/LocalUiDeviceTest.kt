package com.logikawan.finflow

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.logikawan.finflow.app.App
import com.logikawan.finflow.core.data.FinanceRepository
import com.logikawan.finflow.core.data.LocalFinanceLoad
import com.logikawan.finflow.core.data.LocalFinancePersistence
import com.logikawan.finflow.core.model.*
import com.logikawan.finflow.core.platform.AppLifecycle
import com.logikawan.finflow.core.platform.NativeServices
import com.logikawan.finflow.core.ui.Choice
import com.logikawan.finflow.core.ui.FFColors
import com.logikawan.finflow.core.ui.FinFlowTheme
import com.logikawan.finflow.feature.transaction.TransactionEditor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID

class LocalUiDeviceTest {
    @get:Rule val ui = createComposeRule()

    @Test fun transactionAndWalletControlsKeepRenderedContrastInEveryThemeAndPalette() {
        var preferences by mutableStateOf(AppPreferences(theme = "dark"))
        var editor by mutableStateOf(true)
        val data = FinanceData(accounts = listOf(Account("cash", "Kas Uji")), categories = defaultCategories)
        ui.setContent {
            CompositionLocalProvider(LocalPreferences provides preferences) {
                FinFlowTheme {
                    Surface(Modifier.fillMaxSize(), color = FFColors.background) {
                        if (editor) TransactionEditor(null, data, false, {}, {}, null)
                        else Column(Modifier.padding(24.dp)) { Choice("Dompet", "cash", listOf("cash" to "Kas Uji"), {}) }
                    }
                }
            }
        }
        for (palette in listOf("neutral", "arctic", "ruby", "emerald", "indigo")) {
            for (theme in listOf("dark", "light")) {
                ui.runOnIdle { preferences = AppPreferences(theme = theme, palette = palette); editor = true }
                ui.onNode(hasText("Pengeluaran") and isSelected()).assertExists()
                assertRenderedContrast(ui.onNodeWithText("Pengeluaran", useUnmergedTree = true), "$theme/$palette expense label")
                assertRenderedContrast(ui.onNodeWithTag("key-confirm"), "$theme/$palette confirm icon", inset = true)
                if (theme == "dark" && palette == "neutral") screenshot("second-dark-editor")
                ui.runOnIdle { editor = false }
                assertRenderedContrast(ui.onNodeWithText("Kas Uji", useUnmergedTree = true), "$theme/$palette selected wallet")
            }
        }
    }

    @Test fun localAppPersistsManualTransactionAndKeepsDataAcrossWalletBackAndAccountCancel() {
        val persistence = MemoryPersistence()
        val native = MemoryNative(persistence)
        val repo = FinanceRepository(local = persistence)
        runBlocking { repo.initializeProfile("Uji Lokal", "IDR", "Kas Uji", 2_000_000) }
        val lifecycle = AppLifecycle()
        ui.setContent { App(native, lifecycle, cloudRepository = repo) }
        ui.waitUntil(10_000) { ui.onAllNodesWithTag("dashboard").fetchSemanticsNodes().isNotEmpty() }

        ui.onNodeWithTag("add-transaction").performClick()
        ui.waitUntil(5_000) {
            ui.onAllNodesWithTag("transaction-sheet").fetchSemanticsNodes().isNotEmpty() ||
                ui.onAllNodesWithText("Manual").fetchSemanticsNodes().isNotEmpty() ||
                ui.onAllNodesWithTag("quick-add").fetchSemanticsNodes().isNotEmpty()
        }
        // Default tombol + kini panel 5 tombol seperti Ollo (sejak 6 Nov); "Tambah" membuka form manual.
        if (ui.onAllNodesWithTag("quick-add").fetchSemanticsNodes().isNotEmpty()) ui.onNodeWithTag("quick-add").performClick()
        ui.waitUntil(5_000) {
            ui.onAllNodesWithTag("transaction-sheet").fetchSemanticsNodes().isNotEmpty() ||
                ui.onAllNodesWithText("Manual").fetchSemanticsNodes().isNotEmpty()
        }
        if (ui.onAllNodesWithText("Manual").fetchSemanticsNodes().isNotEmpty()) ui.onNodeWithText("Manual").performClick()
        ui.onNodeWithTag("transaction-sheet").assertIsDisplayed()
        ui.onNodeWithTag("key-1").performClick()
        ui.onNodeWithTag("key-000").performClick()
        ui.onNodeWithTag("key-confirm").performClick()
        ui.waitUntil(5_000) { runBlocking { repo.observe().first().transactions.size == 1 } }
        ui.onNodeWithTag("transaction-sheet").assertDoesNotExist()
        val saved = runBlocking { repo.observe().first() }
        assertEquals(100_000L, saved.transactions.single().amountMinor)
        assertEquals("expense", saved.transactions.single().type)
        assertEquals(1_900_000L, saved.balance(saved.accounts.single()))
        val stored = requireNotNull(persistence.content)
        val reopened = runBlocking { FinanceRepository(local = MemoryPersistence(stored)).observe().first() }
        assertEquals(saved.transactions, reopened.transactions)
        assertEquals(1_900_000L, reopened.balance(reopened.accounts.single()))

        ui.onNodeWithTag("dashboard").performScrollToNode(hasTestTag("dashboard-wallet-acc_cash"))
        ui.onNodeWithTag("dashboard").performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 600f) }
        ui.onNodeWithTag("dashboard-wallet-acc_cash").performClick()
        screenshot("local-wallet-after-click")
        ui.waitUntil(5_000) { ui.onAllNodesWithText("Saldo Saat Ini").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithText("Saldo Saat Ini").assertIsDisplayed()
        ui.runOnIdle { assertTrue("Wallet detail must handle Back", lifecycle.back()) }
        ui.onNodeWithTag("dashboard").assertIsDisplayed()

        ui.onNodeWithTag("tab-Profil").performClick()
        ui.onNodeWithText("Manajemen Akun").performScrollTo().performClick()
        ui.onNodeWithText("Hubungkan Akun / Sinkronisasi").performScrollTo().performClick()
        ui.onNodeWithTag("auth-welcome").assertIsDisplayed()
        ui.onNodeWithText("Kembali ke Mode Lokal").performClick()
        ui.onNodeWithTag("auth-welcome").assertDoesNotExist()
        if (ui.onAllNodesWithTag("dashboard").fetchSemanticsNodes().isEmpty()) {
            ui.runOnIdle { lifecycle.back() }
            ui.onNodeWithTag("tab-Beranda").performClick()
        }
        ui.onNodeWithTag("dashboard").assertIsDisplayed()
        assertEquals(saved.transactions, runBlocking { repo.observe().first().transactions })
        assertEquals("local", native.readSetting("data.mode"))
        assertEquals(0, native.googleRequests)
    }

    @Test fun profileRoadmapTabsSearchCopyAndFeedbackAreConnected() {
        val persistence = MemoryPersistence()
        val native = MemoryNative(persistence)
        val repo = FinanceRepository(local = persistence)
        runBlocking { repo.initializeProfile("Uji Peta Jalan") }
        val lifecycle = AppLifecycle()
        ui.setContent { App(native, lifecycle, cloudRepository = repo) }
        ui.waitUntil(10_000) { ui.onAllNodesWithTag("dashboard").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithTag("tab-Profil").performClick()
        ui.onNodeWithTag("profile-list").performScrollToNode(hasText("Masukan & Peta Jalan"))
        ui.onNodeWithText("Masukan & Peta Jalan").performClick()
        ui.onNodeWithTag("product-roadmap").assertIsDisplayed()
        ui.onNodeWithText("Direncanakan").assertIsSelected()
        ui.onNodeWithTag("roadmap-list").performScrollToNode(hasTestTag("roadmap-FF-042"))
        ui.onNodeWithTag("roadmap-FF-042").performClick()
        ui.onNodeWithText("Salin Prompt").performClick()
        ui.runOnIdle {
            val manager = InstrumentationRegistry.getInstrumentation().targetContext.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            assertTrue(manager.primaryClip!!.getItemAt(0).text.toString().contains("Fitur FinFlow FF-042"))
        }
        ui.onNodeWithText("Sedang Dikerjakan").performClick()
        ui.onNodeWithTag("roadmap-list").performScrollToNode(hasTestTag("roadmap-FF-035"))
        ui.onNodeWithTag("roadmap-FF-035").assertIsDisplayed()
        ui.onNodeWithText("Selesai").performClick()
        ui.onNodeWithTag("roadmap-list").performScrollToNode(hasContentDescription("Cari fitur / ID"))
        ui.onNodeWithContentDescription("Cari fitur / ID").performTextReplacement("FF-022")
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        ui.onNodeWithTag("roadmap-FF-022").assertIsDisplayed()
        ui.onNodeWithTag("roadmap-FF-001").assertDoesNotExist()
        ui.onNodeWithText("Salin Semua").performClick()
        ui.runOnIdle {
            val manager = InstrumentationRegistry.getInstrumentation().targetContext.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val content = manager.primaryClip!!.getItemAt(0).text.toString()
            assertTrue(content.contains("FF-001")); assertTrue(content.contains("FF-060"))
        }
        screenshot("product-roadmap-dark")
        ui.onNodeWithText("Kirim Masukan").performClick()
        ui.onNodeWithContentDescription("Saran").assertIsDisplayed()
        ui.onNodeWithText("Buka Draf Email").assertIsNotEnabled()
        ui.runOnIdle { lifecycle.back() }
        ui.onNodeWithTag("product-roadmap").assertIsDisplayed()
    }

    private fun assertRenderedContrast(node: SemanticsNodeInteraction, label: String, inset: Boolean = false) {
        node.assertIsDisplayed()
        val pixels = node.captureToImage().toPixelMap()
        val marginX = if (inset) (pixels.width * .18f).toInt().coerceAtLeast(1) else 0
        val marginY = if (inset) (pixels.height * .18f).toInt().coerceAtLeast(1) else 0
        val colors = mutableMapOf<Int, Int>()
        for (y in marginY until pixels.height - marginY) {
            for (x in marginX until pixels.width - marginX) {
                val color = pixels[x, y].toArgb()
                colors[color] = (colors[color] ?: 0) + 1
            }
        }
        assertTrue("$label must contain rendered pixels", colors.isNotEmpty())
        val background = Color(colors.maxBy { it.value }.key).luminance().toDouble()
        val contrast = colors.filterValues { it >= 3 }.keys.maxOf { argb ->
            val luminance = Color(argb).luminance().toDouble()
            (maxOf(background, luminance) + .05) / (minOf(background, luminance) + .05)
        }
        if (contrast < 4.5) screenshot("contrast-failed-${label.replace('/', '-').replace(' ', '-')}")
        assertTrue("$label rendered contrast was $contrast; expected at least 4.5; colors=${colors.entries.sortedByDescending { it.value }.take(4)}", contrast >= 4.5)
    }

    private fun screenshot(name: String) {
        ui.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "design-qa").apply { mkdirs() }
        val image = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        try { FileOutputStream(File(directory, "$name.png")).use { image.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        finally { image.recycle() }
    }
}

private class MemoryPersistence(initial: String? = null) : LocalFinancePersistence {
    override val id = "ui-${UUID.randomUUID()}"
    @Volatile var content: String? = initial
        private set
    override fun load() = LocalFinanceLoad(content)
    override fun save(content: String): String? { this.content = content; return null }
}

private class MemoryNative(private val persistence: LocalFinancePersistence) : NativeServices {
    private val settings = mutableMapOf("data.mode" to "local", "appearance.local" to "{\"theme\":\"dark\"}")
    var googleRequests = 0
        private set
    override fun localFinancePersistence() = persistence
    override fun readSetting(key: String) = settings[key]
    override fun writeSetting(key: String, value: String) { settings[key] = value }
    override fun sha256(text: String) = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 255) }
    override fun onboardingComplete() = true
    override fun setOnboardingComplete() {}
    override fun setNotificationImportUser(userId: String?) {}
    override fun pickCsv(result: (String?, String?) -> Unit) { result(null, null) }
    override fun shareCsv(name: String, content: String) {}
    override fun googleSignIn(result: (String?, String?) -> Unit) { googleRequests++; result(null, "Tidak ada login produksi pada pengujian") }
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
