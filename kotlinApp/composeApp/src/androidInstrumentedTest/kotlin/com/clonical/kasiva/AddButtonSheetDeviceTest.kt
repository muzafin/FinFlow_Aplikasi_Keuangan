package com.clonical.kasiva

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.clonical.kasiva.core.ui.KasivaTheme
import com.clonical.kasiva.feature.transaction.AddButtonIntroSheet
import com.clonical.kasiva.feature.transaction.RecordMethodsSheet
import java.io.File
import java.io.FileOutputStream
import org.junit.Rule
import org.junit.Test

class AddButtonSheetDeviceTest {
    @get:Rule val ui = createComposeRule()

    @Test fun introSheetChoosesAddButtonMode() {
        var chosen: String? = null
        ui.setContent {
            KasivaTheme {
                AddButtonIntroSheet("all", onChoose = { chosen = it }, onDismiss = {})
            }
        }
        ui.mainClock.advanceTimeBy(800)
        ui.waitForIdle()
        ui.onNodeWithTag("add-button-intro").assertExists()
        ui.onNodeWithText("Tombol + mau buka apa?").assertExists()
        ui.onNodeWithText("Semua cara mencatat").assertExists()
        screenshot("add-sheet-intro")
        ui.onNodeWithTag("add-mode-quick").performClick()
        ui.waitForIdle()
        check(chosen == "quick") { "Mode tombol + tidak tersimpan: " + chosen }
    }

    @Test fun recordMethodsSheetOpensRealAndMarksUnavailable() {
        var added = false
        var imported = false
        ui.setContent {
            KasivaTheme {
                RecordMethodsSheet(onAddTransaction = { added = true }, onImportNotification = { imported = true }, onDismiss = {})
            }
        }
        ui.mainClock.advanceTimeBy(800)
        ui.waitForIdle()
        ui.onNodeWithTag("record-methods").assertExists()
        ui.onNodeWithTag("record-voice").assertIsNotEnabled()
        ui.onNodeWithTag("record-scan").assertIsNotEnabled()
        screenshot("add-sheet-methods")
        ui.onNodeWithTag("record-manual").performClick()
        ui.waitForIdle()
        check(added) { "Tambah manual tidak memanggil aksi" }
        check(!imported) { "Aksi impor tidak seharusnya berjalan" }
    }

    private fun screenshot(name: String) {
        ui.mainClock.advanceTimeBy(500)
        ui.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "add-sheet-qa").apply { mkdirs() }
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        checkNotNull(bitmap) { "Device screenshot unavailable" }
        FileOutputStream(File(directory, name + ".png")).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
