package com.clonical.kasiva

import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.view.WindowManager
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class ScreenshotPolicyDeviceTest {
    @Test fun releaseScreenAllowsScreenshots() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals("Run this check against a release build",0,activity.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE)
                assertEquals(0,activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE)
            }
            instrumentation.waitForIdleSync()
            val bitmap=checkNotNull(instrumentation.uiAutomation.takeScreenshot())
            val file=File(instrumentation.targetContext.getExternalFilesDir(null),"release-screenshot-policy.png")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
            bitmap.recycle()
            assertTrue(file.length()>1000)
        }
    }

    @Test fun recordSyntheticDiagnostic() {
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        reportTechnicalError("diagnostic_verification",IllegalStateException("Synthetic verification; no user data"))
        Thread.sleep(2000) // Crashlytics persists nonfatal reports asynchronously before the test process exits.
    }

    @Test fun sendSyntheticDiagnostic() {
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        FirebaseCrashlytics.getInstance().sendUnsentReports()
        Thread.sleep(15000) // Keep this isolated test process alive while the SDK uploads the previous session.
    }
}
