package com.clonical.kasiva

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.i18n.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.util.TimeZone

/**
 * Push FCM Kasiva (Android).
 *
 * - Semua instalasi berlangganan topik `announcements` + `lang_<bahasa>` agar pengumuman dari Firebase Console
 *   (Messaging → kampanye) bisa dikirim juga ke pengguna mode lokal.
 * - Akun cloud mendaftarkan token lewat callable `registerPushToken` (server menyimpannya di users/{uid}/pushTokens)
 *   untuk pengingat tagihan terjadwal. Keluar akun menghapus token FCM sehingga server tidak lagi bisa mengirim ke
 *   akun lama dari perangkat ini.
 */
object PushMessaging {
    private const val TAG = "KasivaPush"
    const val CHANNEL_BILLS = "bills"
    const val CHANNEL_ANNOUNCEMENTS = "announcements"
    private const val PREFS = "finflow"
    private const val KEY_UID = "push.uid"
    private const val KEY_LANGUAGE = "push.language"
    private const val KEY_FINGERPRINT = "push.registered"
    private const val KEY_REGISTERED_AT = "push.registeredAt"
    private const val KEY_TOPIC_LANGUAGE = "push.topicLanguage"
    const val KEY_BILLS = "push.bills"
    private const val REFRESH_MS = 7L * 24 * 60 * 60 * 1000
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Mutex()

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_BILLS, MR.strings.core_channel_bills.text(), NotificationManager.IMPORTANCE_HIGH))
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ANNOUNCEMENTS, MR.strings.core_channel_announcements.text(), NotificationManager.IMPORTANCE_DEFAULT))
    }

    fun billRemindersEnabled(context: Context) = prefs(context).getBoolean(KEY_BILLS, true)

    fun setBillReminders(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_BILLS, enabled).remove(KEY_FINGERPRINT).apply()
        val p = prefs(context)
        sync(context, p.getString(KEY_UID, null), p.getString(KEY_LANGUAGE, null) ?: "id")
    }

    /** Dipanggil saat akun/mode/bahasa berubah. `uid` null = mode lokal atau belum masuk. */
    fun sync(context: Context, uid: String?, language: String) {
        val app = context.applicationContext
        scope.launch {
            lock.withLock {
                try { syncLocked(app, uid, language) }
                catch (e: Exception) { Log.w(TAG, "push sync failed: ${e.javaClass.simpleName}") }
            }
        }
    }

    private suspend fun syncLocked(context: Context, uid: String?, language: String) {
        val p = prefs(context)
        val messaging = FirebaseMessaging.getInstance()
        val previousUid = p.getString(KEY_UID, null)
        if (previousUid != null && previousUid != uid) {
            // Keluar/pindah akun: token lama dibuang agar notifikasi akun lama tidak lagi sampai ke perangkat ini.
            runCatching { messaging.deleteToken().await() }
            p.edit().remove(KEY_UID).remove(KEY_FINGERPRINT).remove(KEY_REGISTERED_AT).remove(KEY_TOPIC_LANGUAGE).apply()
        }
        p.edit().putString(KEY_LANGUAGE, language).apply()
        val topicLanguage = p.getString(KEY_TOPIC_LANGUAGE, null)
        if (topicLanguage != language) {
            // Gagal topik (mis. SERVICE_NOT_AVAILABLE sesaat setelah instal) tidak boleh menghalangi pendaftaran token;
            // penanda tidak disimpan sehingga dicoba lagi pada sinkronisasi berikutnya.
            runCatching {
                messaging.subscribeToTopic("announcements").await()
                if (topicLanguage != null) runCatching { messaging.unsubscribeFromTopic("lang_$topicLanguage").await() }
                messaging.subscribeToTopic("lang_$language").await()
                p.edit().putString(KEY_TOPIC_LANGUAGE, language).apply()
            }.onFailure { Log.w(TAG, "topic subscribe failed: ${it.javaClass.simpleName}: ${it.message}") }
        }
        if (uid == null || FirebaseAuth.getInstance().currentUser?.uid != uid) return
        val token = messaging.token.await()
        register(context, uid, token, language)
    }

    private suspend fun register(context: Context, uid: String, token: String, language: String) {
        val p = prefs(context)
        val billReminders = billRemindersEnabled(context)
        val timeZone = TimeZone.getDefault().id
        val fingerprint = listOf(uid, token, language, timeZone, billReminders).joinToString("|").hashCode().toString()
        val fresh = System.currentTimeMillis() - p.getLong(KEY_REGISTERED_AT, 0) < REFRESH_MS
        if (p.getString(KEY_FINGERPRINT, null) == fingerprint && fresh) { p.edit().putString(KEY_UID, uid).apply(); return }
        val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
        FirebaseFunctions.getInstance("asia-southeast2").getHttpsCallable("registerPushToken").call(mapOf(
            "token" to token, "platform" to "android", "language" to language, "timeZone" to timeZone,
            "appVersion" to version, "billReminders" to billReminders,
        )).await()
        p.edit().putString(KEY_UID, uid).putString(KEY_FINGERPRINT, fingerprint).putLong(KEY_REGISTERED_AT, System.currentTimeMillis()).apply()
    }

    /** Token baru dari FCM (instal ulang data, token diputar): daftarkan ulang untuk akun yang sedang masuk. */
    fun onNewToken(context: Context, token: String) {
        val p = prefs(context)
        val uid = p.getString(KEY_UID, null) ?: return
        if (FirebaseAuth.getInstance().currentUser?.uid != uid) return
        val language = p.getString(KEY_LANGUAGE, null) ?: "id"
        scope.launch {
            lock.withLock {
                p.edit().remove(KEY_FINGERPRINT).apply()
                runCatching { register(context.applicationContext, uid, token, language) }
                    .onFailure { Log.w(TAG, "token refresh failed: ${it.javaClass.simpleName}") }
            }
        }
    }

    /** Pesan yang tiba saat aplikasi di depan (atau pesan data-only): tampilkan sendiri dan catat ke riwayat. */
    fun show(context: Context, message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        val kind = message.data["kind"] ?: "push"
        val channel = message.notification?.channelId ?: if (kind.startsWith("bill")) CHANNEL_BILLS else CHANNEL_ANNOUNCEMENTS
        createChannels(context)
        NotificationHistory.append(context, title, body, if (kind.startsWith("bill")) "bill" else "push")
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val intent = PendingIntent.getActivity(context, 1003, Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra("push.route", message.data["route"]), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        try {
            context.getSystemService(NotificationManager::class.java).notify(kind, 1003, NotificationCompat.Builder(context, channel)
                .setSmallIcon(R.drawable.ic_stat_kasiva).setContentTitle(title).setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setContentIntent(intent).setAutoCancel(true).build())
        } catch (_: SecurityException) {
            // Izin dicabut di antara pemeriksaan dan pengiriman.
        }
    }
}

class KasivaMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) = PushMessaging.onNewToken(this, token)
    override fun onMessageReceived(message: RemoteMessage) = PushMessaging.show(this, message)
}
