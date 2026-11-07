package com.clonical.kasiva.notificationimport

import com.clonical.kasiva.i18n.*
import com.clonical.kasiva.core.i18n.text
import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.clonical.kasiva.feature.importdata.NotificationTransactionParser
import java.security.MessageDigest

class TransactionNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.user != android.os.Process.myUserHandle()) return
        val store = NotificationImportStore(this)
        if (!store.enabled()) return
        val owner = store.owner() ?: return
        // Periksa sumber sebelum mengakses isi notifikasi; aplikasi lain langsung diabaikan.
        val source = store.source(sbn.packageName) ?: return
        if (sbn.postTime < store.consentAt() || sbn.postTime > System.currentTimeMillis() + 60_000) return
        val notification = sbn.notification
        if (notification.flags and (Notification.FLAG_GROUP_SUMMARY or Notification.FLAG_ONGOING_EVENT) != 0) return
        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val body = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (title.length + body.length > 16_000) return
        val parsed = NotificationTransactionParser.parse("$title\n$body", source.currency) ?: return
        val fingerprint = MessageDigest.getInstance("SHA-256").digest(
            "$owner|${sbn.key}|${sbn.postTime}|${parsed.type}|${parsed.currency}|${parsed.amountMinor}".toByteArray(Charsets.UTF_8)
        ).joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }
        val appName = runCatching { packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString() }
            .getOrElse { MR.strings.core_notification_app_fallback.text() }
        // Isi asli, nomor rekening, OTP dan judul notifikasi tidak disimpan atau dikirim ke Firebase.
        store.add(owner, source, NotificationCandidate(fingerprint, appName.take(100), source.accountId,
            parsed.type, parsed.amountMinor, parsed.currency, sbn.postTime))
    }
}
