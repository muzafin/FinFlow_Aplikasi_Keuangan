package com.clonical.kasiva

import android.app.NotificationManager
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationChannelsDeviceTest {
    @Test fun newChannelsAllowHeadsUpAndExistingPreferencesArePreserved() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = context.getSystemService(NotificationManager::class.java)
        val previous = listOf("announcements", "bills", "daily", "budget").associateWith { manager.getNotificationChannel(it)?.importance }
        PushMessaging.createChannels(context)
        for ((id, importance) in previous) {
            assertEquals(id, importance ?: NotificationManager.IMPORTANCE_HIGH, manager.getNotificationChannel(id).importance)
        }
    }
}
