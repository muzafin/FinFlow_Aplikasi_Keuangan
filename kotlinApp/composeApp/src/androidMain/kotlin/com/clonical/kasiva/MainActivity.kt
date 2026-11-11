package com.clonical.kasiva

import com.clonical.kasiva.i18n.*
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.app.App
import com.clonical.kasiva.core.platform.AppLifecycle
import com.clonical.kasiva.core.platform.NativeServices
import com.clonical.kasiva.feature.importdata.CsvImport
import com.clonical.kasiva.notificationimport.NotificationImportStore

import android.Manifest
import android.app.Activity
import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.ClipData
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

class MainActivity : FragmentActivity(), NativeServices {
    override fun localFinancePersistence() = AndroidLocalFinancePersistence.get(this)
    override fun closeApp():Boolean { finishAndRemoveTask();return true }
    override fun appVersion():String = packageManager.getPackageInfo(packageName,0).versionName ?: "1.0.0"
    override fun reportError(operation: String, error: Throwable) = reportTechnicalError(operation, error)
    override fun deleteLocalProfilePhoto():String? {
        val file=File(noBackupFilesDir,"finflow-local/avatar.jpg")
        // Ikon dompet kustom mode lokal ikut dibersihkan bersama data lokal.
        val iconsCleared=walletIcons.clearLocal()
        return if((!file.exists() || file.delete()) && iconsCleared)null else MR.strings.core_main_local_photo_not_cleaned.text()
    }
    override fun composeSupport(subject:String,body:String):String? = try {
        val uri=android.net.Uri.parse("mailto:muzafinarifin.dev@gmail.com?subject="+android.net.Uri.encode(subject)+"&body="+android.net.Uri.encode(body))
        startActivity(Intent(Intent.ACTION_SENDTO,uri));null
    }catch(e:Exception){MR.strings.core_main_no_email_app.text()}
    override fun shareApp():String? = try {
        val file=exportFile("Kasiva.apk")
        File(applicationInfo.sourceDir).copyTo(file,overwrite=true)
        shareExport(file,"application/vnd.android.package-archive");null
    }catch(e:Exception){MR.strings.core_main_apk_share_failed.text()}
    private val profilePhotos = ProfilePhotos(this)
    override fun pickProfilePhoto(result: (ByteArray?, String?) -> Unit) = profilePhotos.pick(result)
    override fun captureProfilePhoto(result: (ByteArray?, String?) -> Unit) = profilePhotos.capture(result)
    override fun uploadProfilePhoto(userId: String, bytes: ByteArray, result: (String?, String?) -> Unit) = profilePhotos.upload(userId, bytes, result)
    override fun loadProfilePhoto(path: String, result: (ByteArray?, String?) -> Unit) = profilePhotos.load(path, result)
    override fun syncPendingProfilePhoto(userId: String) = profilePhotos.syncPending(userId)
    private val walletIcons = WalletIcons(this)
    override fun pickImage(maxBytes: Int, result: (ByteArray?, String?) -> Unit) = walletIcons.pick(maxBytes, result)
    override fun saveWalletIcon(ownerId: String, accountId: String, bytes: ByteArray, result: (String?) -> Unit) = walletIcons.save(ownerId, accountId, bytes, result)
    override fun loadWalletIcon(ownerId: String, accountId: String, result: (ByteArray?, String?) -> Unit) = walletIcons.load(ownerId, accountId, result)
    override fun devicePlatform(): String = "android"
    override fun isOnline(): Boolean = runCatching {
        val manager = getSystemService(android.net.ConnectivityManager::class.java)
        manager.activeNetwork?.let { network ->
            manager.getNetworkCapabilities(network)?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } == true
    }.getOrDefault(true)
    private val aiDevice = AiDevice(this)
    override fun recognizeSpeech(languageTag: String, result: (String?, String?) -> Unit) = aiDevice.recognizeSpeech(languageTag, result)
    override fun captureReceiptPhoto(result: (ByteArray?, String?) -> Unit) = aiDevice.captureReceipt(result)
    override fun pickReceiptPhotos(max: Int, result: (List<ByteArray>?, String?) -> Unit) = aiDevice.pickReceipts(max, result)
    private val premiumBilling = PremiumBilling(this)
    override fun premiumConfigured() = premiumBilling.configured()
    override fun premiumPlans(userId: String, result: (List<com.clonical.kasiva.core.platform.PremiumPlan>?, String?) -> Unit) = premiumBilling.plans(userId, result)
    override fun purchasePremium(planId: String, result: (Boolean, String?) -> Unit) = premiumBilling.purchase(planId, result)
    override fun restorePremium(userId: String, result: (Boolean, String?) -> Unit) = premiumBilling.restore(userId, result)
    override fun manageSubscription(): String? = premiumBilling.manage()
    private val prefs by lazy { getSharedPreferences("finflow", MODE_PRIVATE) }
    private val appLifecycle = AppLifecycle()
    private var csvResult: ((String?, String?) -> Unit)? = null
    private var reminderResult: ((String?) -> Unit)? = null
    private var budgetPermissionResult: ((String?) -> Unit)? = null
    private var credentialResult: ((Boolean) -> Unit)? = null
    private var requestedHour = 20
    private var requestedMinute = 0
    private val deviceCredential = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { response ->
        val callback = credentialResult
        credentialResult = null
        callback?.invoke(response.resultCode == Activity.RESULT_OK)
    }
    private val openCsv = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val callback = csvResult ?: return@registerForActivityResult
        csvResult = null
        if (uri == null) { callback(null, null); return@registerForActivityResult }
        lifecycleScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri).use { stream ->
                        requireNotNull(stream) { MR.strings.core_file_unreadable.text() }
                        val output = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = stream.read(buffer)
                            if (count < 0) break
                            require(output.size() + count <= CsvImport.MAX_FILE_BYTES) { MR.strings.core_csv_max_5mb.text() }
                            output.write(buffer, 0, count)
                        }
                        Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(output.toByteArray())).toString()
                    }
                }
                callback(text, null)
            } catch (e: Exception) { callback(null, e.message ?: MR.strings.core_file_read_failed_csv.text()) }
        }
    }
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val callback = reminderResult
        reminderResult = null
        if (granted) { saveReminder(true, requestedHour, requestedMinute); callback?.invoke(null) }
        else callback?.invoke(MR.strings.core_reminder_permission_denied.text())
    }
    private val budgetNotificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val callback = budgetPermissionResult
        budgetPermissionResult = null
        if (granted) { callback?.let { setBudgetAlerts(true, it) } }
        else callback?.invoke(MR.strings.core_budget_permission_denied.text())
    }

    private var billPermissionResult: ((String?) -> Unit)? = null
    private val billNotificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val callback = billPermissionResult
        billPermissionResult = null
        if (granted) { callback?.let { setBillPush(true, it) } }
        else callback?.invoke(MR.strings.core_notifications_disabled.text())
    }
    private var pushUid: String? = null
    override fun setPushUser(userId: String?, language: String) {
        pushUid = userId
        PushMessaging.sync(this, userId, language)
        // Android 13+: tanpa izin notifikasi, push (pengingat tagihan/pengumuman) tidak pernah tampil. Minta sekali
        // setelah masuk akun cloud; bila ditolak, pengguna masih bisa menyalakannya dari Profil → Notifikasi.
        if (userId != null && Build.VERSION.SDK_INT >= 33 && !prefs.getBoolean("push.permissionAsked", false)
            && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            && billPermissionResult == null && budgetPermissionResult == null) {
            prefs.edit().putBoolean("push.permissionAsked", true).apply()
            billNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    override fun billPushEnabled(): Boolean = pushUid != null && PushMessaging.billRemindersEnabled(this) && NotificationManagerCompat.from(this).areNotificationsEnabled()
    override fun setBillPush(enabled: Boolean, result: (String?) -> Unit) {
        if (pushUid == null) { result(MR.strings.set_notif_bills_cloud_only.text()); return }
        if (!enabled) { PushMessaging.setBillReminders(this, false); result(null); return }
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            if (billPermissionResult != null) { result(MR.strings.core_permission_request_pending.text()); return }
            billPermissionResult = result
            billNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            result(MR.strings.core_notifications_disabled.text())
        } else { PushMessaging.setBillReminders(this, true); result(null) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!appLifecycle.back()) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
        val previewFirstRun = intent.getBooleanExtra("finflow_preview_first_run", false)
        setContent { App(this, appLifecycle, previewFirstRun = previewFirstRun) }
        // Android 12+: tahan splash sistem (logo yang sama) selama aplikasi bersiap, agar tidak ada
        // splash kedua dengan logo berukuran lain (logo "besar lalu mengecil").
        if (Build.VERSION.SDK_INT >= 31) {
            val start = android.os.SystemClock.uptimeMillis()
            val content = findViewById<android.view.View>(android.R.id.content)
            content.viewTreeObserver.addOnPreDrawListener(object : android.view.ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (android.os.SystemClock.uptimeMillis() - start < SPLASH_HOLD_MS) return false
                    content.viewTreeObserver.removeOnPreDrawListener(this)
                    return true
                }
            })
        }
    }
    override fun hasSystemSplash(): Boolean = Build.VERSION.SDK_INT >= 31
    override fun deviceCurrency(): String = runCatching { java.util.Currency.getInstance(resources.configuration.locales[0]).currencyCode }.getOrNull() ?: "USD"
    override fun onStop() { super.onStop(); appLifecycle.background() }
    override fun onboardingComplete() = prefs.getBoolean("onboarding.complete", false)
    override fun setOnboardingComplete() { prefs.edit().putBoolean("onboarding.complete", true).apply() }
    override fun readSetting(key: String): String? = prefs.getString(key, null)
    override fun sha256(text: String): String = java.security.MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it.toInt() and 255)}
    override fun writeSetting(key: String, value: String) {
        val edit=prefs.edit().putString(key,value)
        if(key=="data.mode" || key.startsWith("local.sync.") || key.startsWith("restore.") || key.startsWith("reset."))check(edit.commit()){MR.strings.core_main_state_save_failed.text()}else edit.apply()
    }
    override fun setNotificationImportUser(userId: String?) {
        NotificationImportStore(this).setOwner(userId)
        RecurringTransactionsWorker.schedule(this, userId)
    }
    override fun pickCsv(result: (String?, String?) -> Unit) {
        csvResult = result
        openCsv.launch(arrayOf("text/*", "application/csv", "application/json", "application/vnd.ms-excel", "application/octet-stream"))
    }
    override fun shareCsv(name: String, content: String) = shareTextFile(name, content, "text/csv")
    override fun saveToDownloads(name: String, content: String, mimeType: String): String? = try {
        if (Build.VERSION.SDK_INT < 29) MR.strings.pf_downloads_unavailable.text()
        else {
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = requireNotNull(contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
            requireNotNull(contentResolver.openOutputStream(uri)).use { it.write(content.toByteArray(Charsets.UTF_8)) }
            values.clear(); values.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
            contentResolver.update(uri, values, null, null)
            null
        }
    } catch (e: Exception) { e.message ?: MR.strings.pf_downloads_unavailable.text() }
    override fun shareTextFile(name: String, content: String, mimeType: String) {
        require(mimeType.matches(Regex("[a-zA-Z0-9.+-]+/[a-zA-Z0-9.+-]+"))) { "Jenis file tidak valid" }
        require(content.toByteArray(Charsets.UTF_8).size <= CsvImport.MAX_FILE_BYTES) { MR.strings.core_export_max_5mb.text() }
        val file = exportFile(name)
        file.writeText(content, Charsets.UTF_8)
        shareExport(file, mimeType)
    }
    override fun deviceDescription(): String = "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}"
    override fun deviceDiagnostics(): String {
        val metrics = resources.displayMetrics
        val config = resources.configuration
        val info = android.app.ActivityManager.MemoryInfo()
        (getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager).getMemoryInfo(info)
        val widthDp = (metrics.widthPixels / metrics.density).toInt()
        val heightDp = (metrics.heightPixels / metrics.density).toInt()
        return listOf(
            "Perangkat: ${Build.MANUFACTURER} ${Build.MODEL}",
            "OS: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            "Build: ${Build.DISPLAY}",
            "ABI: ${Build.SUPPORTED_ABIS.joinToString()}",
            "Layar: ${metrics.widthPixels} × ${metrics.heightPixels} px · $widthDp × $heightDp dp · ${metrics.density}×",
            "Skala font: ${config.fontScale}",
            "RAM: ${info.totalMem / (1024 * 1024)} MB",
            "Bahasa: ${config.locales.toLanguageTags()}",
            "Zona waktu: ${java.util.TimeZone.getDefault().id}",
        ).joinToString("\n")
    }
    override fun sharePdf(name: String, title: String, lines: List<String>) {
        require(lines.size <= 25_000 && lines.sumOf { it.length.toLong() } <= 5L * 1024 * 1024) { MR.strings.core_pdf_too_large.text() }
        val file = exportFile(name)
        val document = PdfDocument()
        val width = 595
        val height = 842
        val margin = 36f
        val textWidth = width - (margin * 2).toInt()
        val body = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; color = android.graphics.Color.DKGRAY }
        val heading = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 18f; typeface = Typeface.DEFAULT_BOLD; color = android.graphics.Color.BLACK }
        val footer = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f; color = android.graphics.Color.GRAY }
        val titleLayout = StaticLayout.Builder.obtain(title, 0, title.length, heading, textWidth)
            .setIncludePad(false).setMaxLines(2).setEllipsize(TextUtils.TruncateAt.END).build()
        var page: PdfDocument.Page? = null
        var pageNumber = 0
        var baseline = 0f
        fun finishPage() { page?.let { document.finishPage(it) }; page = null }
        fun beginPage() {
            require(pageNumber < 500) { MR.strings.core_pdf_too_large.text() }
            finishPage()
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(width, height, pageNumber).create())
            val canvas = page!!.canvas
            canvas.save(); canvas.translate(margin, margin); titleLayout.draw(canvas); canvas.restore()
            canvas.drawText(MR.strings.core_pdf_footer.text(pageNumber), margin, height - margin, footer)
            baseline = margin + titleLayout.height + 20f - body.fontMetrics.ascent
        }
        try {
            beginPage()
            val content = lines.ifEmpty { listOf(MR.strings.core_pdf_empty.text()) }
            content.forEach { row ->
                row.replace('\t', ' ').split('\n').forEach { paragraph ->
                    val layout = StaticLayout.Builder.obtain(paragraph, 0, paragraph.length, body, textWidth).setIncludePad(false).build()
                    repeat(layout.lineCount.coerceAtLeast(1)) { line ->
                        if (baseline + body.fontMetrics.descent > height - margin - 20f) beginPage()
                        if (paragraph.isNotEmpty()) page!!.canvas.drawText(paragraph, layout.getLineStart(line), layout.getLineEnd(line), margin, baseline, body)
                        baseline += 16f
                    }
                }
            }
            finishPage()
            file.outputStream().use { document.writeTo(it) }
        } finally { finishPage(); document.close() }
        shareExport(file, "application/pdf")
    }
    private fun exportFile(name: String): File {
        val directory = File(cacheDir, "exports")
        check(directory.isDirectory || directory.mkdirs()) { MR.strings.core_export_folder_failed.text() }
        val clean = name.replace(Regex("[^a-zA-Z0-9._-]"), "_").takeLast(120)
        return File(directory, clean.takeUnless { it.isBlank() || it == "." || it == ".." } ?: "finflow-export")
    }
    private fun shareExport(file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = mimeType; putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(contentResolver, file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, if (mimeType == "text/csv") MR.strings.core_share_csv.text() else MR.strings.core_share_file.text()))
    }
    override fun googleSignIn(result: (String?, String?) -> Unit) {
        val clientIdResource = resources.getIdentifier("default_web_client_id", "string", packageName)
        if (clientIdResource == 0) {
            result(null, MR.strings.core_google_not_configured.text())
            return
        }
        lifecycleScope.launch {
            try {
                val option = GetSignInWithGoogleOption.Builder(getString(clientIdResource)).build()
                val credential = CredentialManager.create(this@MainActivity).getCredential(this@MainActivity,
                    GetCredentialRequest.Builder().addCredentialOption(option).build()).credential
                require(credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) { MR.strings.core_google_credential_unknown.text() }
                result(GoogleIdTokenCredential.createFrom(credential.data).idToken, null)
            } catch (_: GetCredentialCancellationException) { result(null, MR.strings.core_google_cancelled.text()) }
            catch (e: Exception) { result(null, e.message ?: MR.strings.core_google_failed.text()) }
        }
    }
    override fun unlock(result: (Boolean) -> Unit) {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val allowed = if (Build.VERSION.SDK_INT >= 30) authenticators else BiometricManager.Authenticators.BIOMETRIC_STRONG
        if (BiometricManager.from(this).canAuthenticate(allowed) != BiometricManager.BIOMETRIC_SUCCESS) {
            if (Build.VERSION.SDK_INT < 30) unlockWithDeviceCredential(result) else result(false)
            return
        }
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(value: BiometricPrompt.AuthenticationResult) { result(true) }
            override fun onAuthenticationError(code: Int, message: CharSequence) {
                if (Build.VERSION.SDK_INT < 30 && code == BiometricPrompt.ERROR_NEGATIVE_BUTTON) unlockWithDeviceCredential(result)
                else result(false)
            }
        })
        val info = BiometricPrompt.PromptInfo.Builder().setTitle(MR.strings.core_unlock_title.text()).setAllowedAuthenticators(allowed)
        if (Build.VERSION.SDK_INT < 30) info.setNegativeButtonText(MR.strings.core_unlock_use_device_lock.text())
        prompt.authenticate(info.build())
    }
    @Suppress("DEPRECATION")
    private fun unlockWithDeviceCredential(result: (Boolean) -> Unit) {
        val keyguard = getSystemService(KeyguardManager::class.java)
        val intent = keyguard.createConfirmDeviceCredentialIntent(MR.strings.core_unlock_title.text(), MR.strings.core_unlock_confirm_device_lock.text())
        if (intent == null) result(false) else { credentialResult = result; deviceCredential.launch(intent) }
    }
    override fun lockEnabled() = prefs.getBoolean("lock", false)
    override fun setLockEnabled(enabled: Boolean) { prefs.edit().putBoolean("lock", enabled).apply() }
    override fun reminderEnabled() = prefs.getBoolean("reminder", false)
    override fun reminderHour() = prefs.getInt("hour", 20)
    override fun reminderMinute() = prefs.getInt("minute", 0)
    override fun setReminder(enabled: Boolean, hour: Int, minute: Int, result: (String?) -> Unit) {
        require(hour in 0..23 && minute in 0..59)
        if (enabled && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestedHour = hour; requestedMinute = minute; reminderResult = result
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else { saveReminder(enabled, hour, minute); result(null) }
    }
    private fun saveReminder(enabled: Boolean, hour: Int, minute: Int) {
        prefs.edit().putBoolean("reminder", enabled).putInt("hour", hour).putInt("minute", minute).apply()
        ReminderWorker.schedule(this)
    }
    override fun budgetAlertsEnabled() = prefs.getBoolean("budget.enabled", false)
    override fun setBudgetAlerts(enabled: Boolean, result: (String?) -> Unit) {
        if (!enabled) { prefs.edit().putBoolean("budget.enabled", false).apply(); result(null); return }
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            if (budgetPermissionResult != null) { result(MR.strings.core_permission_request_pending.text()); return }
            budgetPermissionResult = result
            budgetNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            result(MR.strings.core_notifications_disabled.text())
        } else {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel("budget", MR.strings.core_channel_budget.text(), NotificationManager.IMPORTANCE_DEFAULT))
            if (manager.getNotificationChannel("budget").importance == NotificationManager.IMPORTANCE_NONE) {
                result(MR.strings.core_channel_budget_disabled.text())
            } else { prefs.edit().putBoolean("budget.enabled", true).apply(); result(null) }
        }
    }
    override fun showBudgetAlert(userId: String, alertId: String, title: String, message: String) {
        val key = "budget.sent.$userId.$alertId"
        if (!budgetAlertsEnabled() || prefs.getBoolean(key, false) || !NotificationManagerCompat.from(this).areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("budget", MR.strings.core_channel_budget.text(), NotificationManager.IMPORTANCE_DEFAULT))
        if (manager.getNotificationChannel("budget").importance == NotificationManager.IMPORTANCE_NONE) return
        val intent = PendingIntent.getActivity(this, 1002, Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        try {
            manager.notify("$userId:$alertId", 1002, NotificationCompat.Builder(this, "budget")
                .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(message)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setContentIntent(intent).setAutoCancel(true).build())
            prefs.edit().putBoolean(key, true).apply()
            NotificationHistory.append(this, title, message, "budget")
        } catch (_: SecurityException) {
            // Izin dapat dicabut ketika aplikasi sedang terbuka; coba lagi setelah diaktifkan.
        }
    }
}

private const val SPLASH_HOLD_MS = 800L
