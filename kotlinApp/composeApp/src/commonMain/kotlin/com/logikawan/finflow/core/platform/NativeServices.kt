package com.logikawan.finflow.core.platform

// Layanan perangkat disediakan Android/iOS; aturan keuangan tetap berada di commonMain.
interface NativeServices {
    fun closeApp(): Boolean = false
    fun appVersion():String = "1.0.0"
    fun composeSupport(subject:String,body:String):String? = "Aplikasi email belum tersedia"
    fun shareApp():String? = "Berbagi aplikasi belum tersedia"
    fun deleteLocalProfilePhoto():String? = null
    fun localFinancePersistence(): com.logikawan.finflow.core.data.LocalFinancePersistence = error("Penyimpanan lokal belum tersedia")
    fun pickProfilePhoto(result: (ByteArray?, String?) -> Unit) { result(null, "Pemilih foto belum tersedia") }
    fun uploadProfilePhoto(userId: String, bytes: ByteArray, result: (String?, String?) -> Unit) { result(null, "Upload foto belum tersedia") }
    fun loadProfilePhoto(path: String, result: (ByteArray?, String?) -> Unit) { result(null, "Foto belum tersedia") }
    fun readSetting(key: String): String? = null
    fun writeSetting(key: String, value: String) {}
    fun onboardingComplete(): Boolean
    fun setOnboardingComplete()
    fun setNotificationImportUser(userId: String?)
    fun pickCsv(result: (String?, String?) -> Unit)
    fun shareCsv(name: String, content: String)
    fun shareTextFile(name: String, content: String, mimeType: String) = shareCsv(name, content)
    fun sharePdf(name: String, title: String, lines: List<String>) {}
    fun deviceDescription(): String = "Perangkat ini"
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
    fun recognizeSpeech(languageTag: String, result: (String?, String?) -> Unit) { result(null, "Input suara belum tersedia di perangkat ini") }
    fun captureReceiptPhoto(result: (ByteArray?, String?) -> Unit) { result(null, "Kamera belum tersedia di perangkat ini") }
    fun pickReceiptPhotos(max: Int, result: (List<ByteArray>?, String?) -> Unit) { result(null, "Galeri belum tersedia di perangkat ini") }

    // Premium: pembelian lewat toko aplikasi (RevenueCat); hak akses diverifikasi server.
    fun premiumConfigured(): Boolean = false
    fun premiumPlans(userId: String, result: (List<PremiumPlan>?, String?) -> Unit) { result(null, "Pembelian belum tersedia di perangkat ini") }
    fun purchasePremium(planId: String, result: (Boolean, String?) -> Unit) { result(false, "Pembelian belum tersedia di perangkat ini") }
    fun restorePremium(userId: String, result: (Boolean, String?) -> Unit) { result(false, "Pemulihan pembelian belum tersedia di perangkat ini") }
    fun manageSubscription(): String? = "Kelola langganan belum tersedia di perangkat ini"
}

/** Paket dari toko; harga sudah dilokalkan oleh Google Play / App Store. */
data class PremiumPlan(val id: String, val kind: String, val price: String, val pricePerMonth: String? = null, val trialDays: Int = 0)
