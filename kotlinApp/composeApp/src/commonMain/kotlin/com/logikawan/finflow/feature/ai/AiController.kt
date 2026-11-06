package com.logikawan.finflow.feature.ai

import androidx.compose.runtime.*
import com.logikawan.finflow.core.model.FinanceData
import com.logikawan.finflow.core.model.Transaction
import com.logikawan.finflow.core.platform.NativeServices
import com.logikawan.finflow.core.util.nowMillis
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Status AI & Premium untuk satu sesi akun. Mode lokal tidak punya akses AI (permintaan harus lewat server). */
class AiController(private val native: NativeServices, val uid: String?, private val scope: CoroutineScope) {
    val service = AiService()
    val store = AiLocalStore(native, uid ?: "local")
    val available get() = uid != null
    var status by mutableStateOf<AiStatus?>(null)
    var statusUnavailable by mutableStateOf(false)
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
            try { status = service.status(); statusUnavailable = false }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { statusUnavailable = true }
        }
    }

    /** Persetujuan diminta per fitur sebelum data pertama dikirim (seperti Ollo). */
    fun requireConsent(feature: AiFeature, onReady: () -> Unit) {
        if (!available) { error = "Masuk ke akun FinFlow untuk memakai AI. Permintaan AI diproses lewat server FinFlow."; return }
        scope.launch {
            try {
                val current = status ?: service.status().also { status = it }
                if (current.consent[feature.id] == true) onReady() else pendingConsent = feature to onReady
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "Status AI belum dapat dimuat" }
        }
    }

    fun setConsent(feature: AiFeature, granted: Boolean, then: () -> Unit = {}) = run(if (granted) "Menyimpan persetujuan…" else "Mencabut persetujuan…") {
        service.setConsent(feature, granted)
        status = service.status()
        then()
    }

    fun run(label: String, block: suspend () -> Unit) {
        if (working != null) return
        working = label
        error = null
        scope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "AI belum berhasil memproses permintaan" }
            finally { working = null; if (available) runCatching { status = service.status() } }
        }
    }

    fun parse(feature: AiFeature, text: String, data: FinanceData, fallbackWallet: String, language: String, done: () -> Unit = {}) =
        run(if (feature == AiFeature.Voice) "Memahami suaramu…" else "Membaca transaksimu…") {
            val result = service.parse(feature, text, aiContext(data, language))
            val converted = result.transactions.mapNotNull { it.toTransaction(data, fallbackWallet) }
            if (converted.isEmpty()) error = result.message ?: "Tidak ada transaksi yang dikenali. Coba tulis seperti \"Makan siang 50rb\"."
            else { drafts = converted; draftMessage = result.message; done() }
        }

    fun scan(photos: List<ByteArray>, data: FinanceData, fallbackWallet: String, language: String) =
        run("Memindai struk…") {
            val settings = store.scanSettings()
            val found = mutableListOf<Transaction>()
            var message: String? = null
            photos.forEachIndexed { index, photo ->
                working = if (photos.size > 1) "Memindai foto ${index + 1} dari ${photos.size}…" else "Memindai struk…"
                val result = service.scan(photo, settings.ownerName, aiContext(data, language))
                message = result.message ?: message
                result.receipts.forEach { receipt ->
                    val currency = data.accounts.find { it.id == receipt.transaction.walletId }?.currency ?: data.profile?.baseCurrency ?: "IDR"
                    receipt.transaction.toTransaction(data, fallbackWallet, receiptNote(receipt.details, settings, currency))?.let(found::add)
                }
            }
            if (found.isEmpty()) error = message ?: "Struk belum terbaca. Pastikan teks jelas dan beri jarak antar struk."
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
