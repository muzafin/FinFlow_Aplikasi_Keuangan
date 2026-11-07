package com.clonical.kasiva

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.clonical.kasiva.feature.auth.EntryTheme
import com.clonical.kasiva.feature.auth.OlloCurrencyDialog
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** QA perangkat untuk dialog "Lihat Semua" mata uang: daftar netral, pencarian, pilihan, tutup. */
class OlloCurrencyPickerDeviceTest {
    @get:Rule val ui = createComposeRule()

    @Test fun dialogShowsCurrencyListSearchAndSelection() {
        var selected = "IDR"
        var dismissed = false
        ui.setContent {
            EntryTheme {
                OlloCurrencyDialog(
                    title = "Mata Uang",
                    closeLabel = "Tutup",
                    selected = selected,
                    onSelect = { selected = it },
                    onDismiss = { dismissed = true },
                )
            }
        }

        ui.onNodeWithTag("currency-picker").assertIsDisplayed()
        ui.onNodeWithTag("currency-picker-search").assertIsDisplayed()
        screenshot("currency-picker")

        ui.onNodeWithTag("currency-picker-list").performScrollToNode(hasTestTag("currency-picker-row-USD"))
        ui.onNodeWithTag("currency-picker-row-USD").assertExists()
        ui.onNodeWithTag("currency-picker-list").performScrollToNode(hasTestTag("currency-picker-row-IDR"))
        ui.onNodeWithTag("currency-picker-row-IDR").assertExists().assertIsSelected()

        ui.onNodeWithTag("currency-picker-search").performTextInput("yen")
        ui.waitForIdle()
        ui.onNodeWithTag("currency-picker-row-JPY").assertExists()
        ui.onNodeWithTag("currency-picker-row-USD").assertDoesNotExist()
        screenshot("currency-picker-search")

        ui.onNodeWithTag("currency-picker-row-JPY").performClick()
        ui.runOnIdle { assertEquals("JPY", selected) }

        ui.onNodeWithTag("currency-picker-close").performClick()
        ui.runOnIdle { assertTrue(dismissed) }
    }

    private fun screenshot(name: String) {
        ui.mainClock.advanceTimeBy(500)
        ui.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = checkNotNull(instrumentation.targetContext.getExternalFilesDir("currency-picker-qa")) {
            "Direktori screenshot tidak tersedia"
        }
        directory.mkdirs()
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot()) { "Device screenshot unavailable" }
        FileOutputStream(File(directory, "$name.png")).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
