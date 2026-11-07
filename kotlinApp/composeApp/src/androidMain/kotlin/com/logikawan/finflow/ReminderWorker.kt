package com.logikawan.finflow

import com.logikawan.finflow.i18n.*
import com.logikawan.finflow.core.i18n.text
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("finflow", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("reminder", false)) return Result.success()
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("daily", MR.strings.core_channel_daily.text(), NotificationManager.IMPORTANCE_DEFAULT))
        val allowed = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (allowed) {
            val intent = PendingIntent.getActivity(applicationContext, 0, Intent(applicationContext, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            manager.notify(1001, NotificationCompat.Builder(applicationContext, "daily")
                .setSmallIcon(android.R.drawable.ic_menu_edit).setContentTitle(MR.strings.core_reminder_title.text())
                .setContentText(MR.strings.core_reminder_body.text())
                .setContentIntent(intent).setAutoCancel(true).build())
        }
        schedule(applicationContext, replace = false)
        return Result.success()
    }
    companion object {
        fun schedule(context: Context, replace: Boolean = true) {
            val work = WorkManager.getInstance(context)
            val prefs = context.getSharedPreferences("finflow", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("reminder", false)) { work.cancelUniqueWork("finflow-daily"); return }
            val now = ZonedDateTime.now()
            var next = now.withHour(prefs.getInt("hour", 20)).withMinute(prefs.getInt("minute", 0)).withSecond(0).withNano(0)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(Duration.between(now, next).seconds, TimeUnit.SECONDS).build()
            work.enqueueUniqueWork("finflow-daily", if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }
    }
}
