package com.logikawan.finflow.core.platform

import com.logikawan.finflow.i18n.*
import com.logikawan.finflow.core.i18n.text

// Layanan perangkat disediakan Android/iOS; aturan keuangan tetap berada di commonMain.
interface NativeServices {
    fun closeApp(): Boolean = false
    /** true bila OS sudah menampilkan splash berlogo (Android 12+), sehingga splash Compose tidak perlu logo lagi. */
    fun hasSystemSplash(): Boolean = false
    /** Kode bahasa ISO 639-1 dari pengaturan HP (mis. "id", "en"). */
    fun deviceLanguage(): String = com.logikawan.finflow.core.i18n.systemLanguage()
    /** Kode mata uang ISO 4217 dari wilayah HP (mis. "IDR"). */
    fun deviceCurrency(): String = "USD"
    fun appVersion():String = "1.0.0"
    fun composeSupport(subject:String,body:String):String? = MR.strings.core_native_email_unavailable.text()
    fun shareApp():String? = MR.strings.core_native_share_unavailable.text()
    fun deleteLocalProfilePhoto():String? = null
    fun localFinancePersistence(): com.logikawan.finflow.core.data.LocalFinancePersistence = error(MR.strings.core_native_local_storage_unavailable.text())
    fun pickProfilePhoto(result: (ByteArray?, String?) -> Unit) { result(null, MR.strings.core_native_photo_picker_unavailable.text()) }
    fun uploadProfilePhoto(userId: String, bytes: ByteArray, result: (String?, String?) -> Unit) { result(null, MR.strings.core_native_photo_upload_unavailable.text()) }
    fun loadProfilePhoto(path: String, result: (ByteArray?, String?) -> Unit) { result(null, MR.strings.core_native_photo_unavailable.text()) }
    fun readSetting(key: String): String? = null
    fun writeSetting(key: String, value: String) {}
    fun onboardingComplete(): Boolean
    fun setOnboardingComplete()
    fun setNotificationImportUser(userId: String?)
    fun pickCsv(result: (String?, String?) -> Unit)
    fun shareCsv(name: String, content: String)
    fun shareTextFile(name: String, content: String, mimeType: String) = shareCsv(name, content)
    fun sharePdf(name: String, title: String, lines: List<String>) {}
    fun deviceDescription(): String = MR.strings.core_native_this_device.text()
    fun deviceDiagnostics(): String = deviceDescription()
    fun sha256(text: String): String = error("Checksum perangkat belum tersedia")
    fun googleSignIn(result: (String?, String?) -> Unit)
    fun unlock(result: (Boolean) -> Unit)
    fun lockEnabled(): Boolean
    fun setLockEnabled(enabled: Boolean)
    fun reminderEnabled(): Boolean
    fun reminderHour(): Int
    fun reminderMinute(): Int
    fun setReminder(enabled: Boolean, hour: Int, minute: Int, result: (String?) -> Unit)
    fun budgetAlertsEnabled(): Boolean
    fun setBudgetAlerts(enabled: Boolean, result: (String?) -> Unit)
    fun showBudgetAlert(userId: String, alertId: String, title: String, message: String)

    // AI: transkrip suara dibuat di perangkat; hanya teks yang dikirim ke server.
    fun recognizeSpeech(languageTag: String, result: (String?, String?) -> Unit) { result(null, MR.strings.core_native_voice_unavailable.text()) }
    fun captureReceiptPhoto(result: (ByteArray?, String?) -> Unit) { result(null, MR.strings.core_native_camera_unavailable.text()) }
    fun pickReceiptPhotos(max: Int, result: (List<ByteArray>?, String?) -> Unit) { result(null, MR.strings.core_native_gallery_unavailable.text()) }

    // Premium: pembelian lewat toko aplikasi (RevenueCat); hak akses diverifikasi server.
    fun premiumConfigured(): Boolean = false
    fun premiumPlans(userId: String, result: (List<PremiumPlan>?, String?) -> Unit) { result(null, MR.strings.core_native_purchase_unavailable.text()) }
    fun purchasePremium(planId: String, result: (Boolean, String?) -> Unit) { result(false, MR.strings.core_native_purchase_unavailable.text()) }
    fun restorePremium(userId: String, result: (Boolean, String?) -> Unit) { result(false, MR.strings.core_native_restore_purchase_unavailable.text()) }
    fun manageSubscription(): String? = MR.strings.core_native_manage_subscription_unavailable.text()
}

/** Paket dari toko; harga sudah dilokalkan oleh Google Play / App Store. */
data class PremiumPlan(val id: String, val kind: String, val price: String, val pricePerMonth: String? = null, val trialDays: Int = 0)
