package com.clonical.kasiva.feature.ai

import androidx.compose.runtime.*
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.model.FinanceData
import com.clonical.kasiva.core.model.Transaction
import com.clonical.kasiva.core.platform.NativeServices
import com.clonical.kasiva.core.util.nowMillis
import com.clonical.kasiva.i18n.*
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Batas total satu permintaan AI (pindai beberapa foto bisa lama); koneksi putus dibatalkan lebih awal oleh pengawas. */
private const val AI_REQUEST_TIMEOUT_MS = 180_000L

/** Status AI & Premium untuk satu sesi akun. Mode lokal tidak punya akses AI (permintaan harus lewat server). */
class AiController(private val native: NativeServices, val uid: String?, private val scope: CoroutineScope) {
    val service = AiService()
    val store = AiLocalStore(native, uid ?: "local")
    val available get() = uid != null
    var status by mutableStateOf<AiStatus?>(null)
    var statusUnavailable by mutableStateOf(false)
    /** true bila server menjawab bahwa function AI tidak ditemukan/dikonfigurasi (NOT_FOUND). */
    var serviceNotDeployed by mutableStateOf(false)
    var premium by mutableStateOf(PremiumState())
    var working by mutableStateOf<String?>(null)
    var error by mutableStateOf<String?>(null)
    var pendingConsent by mutableStateOf<Pair<AiFeature, () -> Unit>?>(null)
    var drafts by mutableStateOf<List<Transaction>>(emptyList())
    var draftMessage by mutableStateOf<String?>(null)

    val isPremium: Boolean get() {
        val now = nowMillis()
        return (premium.active && (premium.expiresAt == null || premium.expiresAt!! > now)) || (premium.manualUntil ?: 0) > now
    }

    suspend fun observePremium() {
        val id = uid ?: return
        Firebase.firestore.document("users/$id/entitlements/premium").snapshots.collect { snapshot ->
            premium = if (snapshot.exists) runCatching { snapshot.data<PremiumState>() }.getOrDefault(PremiumState()) else PremiumState()
        }
    }

    fun refreshStatus() {
        if (!available) return
        scope.launch {
            // Pemeriksaan latar belakang tidak memunculkan dialog; error baru ditampilkan saat pengguna memakai AI.
            try { status = service.status(); statusUnavailable = false; serviceNotDeployed = false }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { statusUnavailable = true; note(e) }
        }
    }

    /** Persetujuan diminta per fitur sebelum data pertama dikirim (seperti Ollo). */
    fun requireConsent(feature: AiFeature, onReady: () -> Unit) {
        if (!available) { error = MR.strings.ai_err_login_required.text(); return }
        scope.launch {
            try {
                val current = status ?: service.status().also { status = it; statusUnavailable = false; serviceNotDeployed = false }
                if (current.consent[feature.id] == true) onReady() else pendingConsent = feature to onReady
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { note(e); error = e.message ?: MR.strings.ai_err_status_unavailable.text() }
        }
    }

    /**
     * `then` dijalankan SETELAH status sibuk dilepas. Dulu `then()` dipanggil di dalam blok `run`, sehingga
     * permintaan lanjutan (mis. pesan pertama ke Asisten setelah menekan Setuju) ditolak oleh guard `working != null`
     * dan pesan pengguna tidak pernah dijawab.
     */
    fun setConsent(feature: AiFeature, granted: Boolean, then: () -> Unit = {}) = run(
        if (granted) MR.strings.ai_working_saving_consent.text() else MR.strings.ai_working_revoking_consent.text(),
        onSuccess = then,
    ) {
        service.setConsent(feature, granted)
        status = service.status().let { fresh -> fresh.copy(consent = fresh.consent + (feature.id to granted)) }
    }

    private var activeJob: kotlinx.coroutines.Job? = null

    fun run(label: String, onSuccess: (() -> Unit)? = null, block: suspend () -> Unit) {
        if (working != null) return
        // Tanpa internet gagal seketika; jangan menggantung sampai batas waktu panggilan jaringan.
        if (!native.isOnline()) { error = MR.strings.core_error_need_internet.text(); return }
        working = label
        error = null
        val job = scope.launch {
            var offline = false
            // Pengawas: bila koneksi putus di tengah permintaan, batalkan agar status "sibuk" tidak tertahan
            // dan permintaan berikutnya (setelah koneksi kembali) memakai sambungan yang baru.
            val watchdog = launch {
                while (true) { delay(1_500); if (!native.isOnline()) { offline = true; activeJob?.cancel(); break } }
            }
            var succeeded = false
            try { kotlinx.coroutines.withTimeout(AI_REQUEST_TIMEOUT_MS) { block() }; serviceNotDeployed = false; statusUnavailable = false; succeeded = true }
            catch (e: kotlinx.coroutines.TimeoutCancellationException) { error = MR.strings.pd_ai_service_busy.text() }
            catch (e: CancellationException) { if (offline) error = MR.strings.core_error_need_internet.text() else throw e }
            catch (e: Exception) { note(e); error = e.message ?: MR.strings.ai_err_generic.text() }
            finally {
                watchdog.cancel()
                working = null
                activeJob = null
                if (available && native.isOnline() && onSuccess == null) runCatching { status = service.status() }
            }
            // Lanjutan baru boleh jalan setelah `working` kosong agar tidak ditolak guard di atas.
            if (succeeded) onSuccess?.invoke()
        }
        activeJob = job
    }

    /** Mencatat keadaan "layanan AI belum tersedia" agar layar AI menampilkan banner, bukan hanya dialog sekali lewat. */
    private fun note(e: Exception) {
        native.reportError("ai_request", e)
        if (e is AiUnavailableException) { statusUnavailable = true; if (e.notDeployed) serviceNotDeployed = true }
    }

    fun parse(feature: AiFeature, text: String, data: FinanceData, fallbackWallet: String, language: String, done: () -> Unit = {}) =
        run(if (feature == AiFeature.Voice) MR.strings.ai_working_voice.text() else MR.strings.ai_working_reading.text()) {
            val result = service.parse(feature, text, aiContext(data, language))
            val converted = result.transactions.mapNotNull { it.toTransaction(data, fallbackWallet) }
            if (converted.isEmpty()) error = result.message ?: MR.strings.ai_err_no_transactions.text()
            else { drafts = converted; draftMessage = result.message; done() }
        }

    fun scan(photos: List<ByteArray>, data: FinanceData, fallbackWallet: String, language: String) =
        run(MR.strings.ai_working_scanning.text()) {
            val settings = store.scanSettings()
            val found = mutableListOf<Transaction>()
            var message: String? = null
            photos.forEachIndexed { index, photo ->
                working = if (photos.size > 1) MR.strings.ai_working_scanning_photo.text(index + 1, photos.size) else MR.strings.ai_working_scanning.text()
                val result = service.scan(photo, settings.ownerName, aiContext(data, language))
                message = result.message ?: message
                result.receipts.forEach { receipt ->
                    val currency = data.accounts.find { it.id == receipt.transaction.walletId }?.currency ?: data.profile?.baseCurrency ?: "IDR"
                    receipt.transaction.toTransaction(data, fallbackWallet, receiptNote(receipt.details, settings, currency))?.let(found::add)
                }
            }
            if (found.isEmpty()) error = message ?: MR.strings.ai_err_scan_unreadable.text()
            else { drafts = found; draftMessage = message }
        }
}

@Composable
fun rememberAiController(native: NativeServices, uid: String?): AiController {
    val scope = rememberCoroutineScope()
    val controller = remember(uid) { AiController(native, uid, scope) }
    LaunchedEffect(controller) {
        controller.refreshStatus()
        try { controller.observePremium() } catch (e: CancellationException) { throw e } catch (_: Exception) {}
    }
    return controller
}
