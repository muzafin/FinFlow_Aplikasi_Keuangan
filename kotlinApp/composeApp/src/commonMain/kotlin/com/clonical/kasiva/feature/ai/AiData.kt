package com.clonical.kasiva.feature.ai

import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.i18n.transactionTypeLabel
import com.clonical.kasiva.core.model.FinanceData
import com.clonical.kasiva.core.model.Transaction
import com.clonical.kasiva.core.platform.NativeServices
import com.clonical.kasiva.core.util.currencyDigits
import com.clonical.kasiva.core.util.money
import com.clonical.kasiva.core.util.newId
import com.clonical.kasiva.core.util.today
import com.clonical.kasiva.i18n.*
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.functions.FirebaseFunctionsException
import dev.gitlive.firebase.functions.FunctionsExceptionCode
import dev.gitlive.firebase.functions.code
import dev.gitlive.firebase.functions.functions
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.seconds

// Fitur AI mengikuti nama di server (functions/ai.js).
enum class AiFeature(val id: String, val title: String) {
    Chat("chat", "AI Chat"), Voice("voice", "AI Voice"), Scan("scan", "AI Scan"), Assistant("assistant", "Kasiva Assistant")
}

@Serializable data class AiCategory(val id: String, val name: String, val type: String)
@Serializable data class AiWallet(val id: String, val name: String, val currency: String)
@Serializable data class AiContext(val today: String, val baseCurrency: String, val language: String,
    val categories: List<AiCategory>, val wallets: List<AiWallet>)
@Serializable private data class ParseRequest(val mode: String, val text: String, val context: AiContext)
@Serializable private data class ScanRequest(val image: String, val mediaType: String, val ownerName: String, val context: AiContext)
@Serializable private data class ConsentRequest(val feature: String, val granted: Boolean)
@Serializable data class AssistantTurn(val role: String, val text: String)
@Serializable private data class AssistantRequest(val messages: List<AssistantTurn>, val summary: String, val personality: String,
    val nickname: String, val preference: String, val language: String)

@Serializable data class AiDraft(val type: String = "expense", val amount: Double = 0.0, val currency: String? = null,
    val title: String = "", val categoryId: String? = null, val walletId: String? = null, val toWalletId: String? = null,
    val date: String? = null, val note: String? = null)
@Serializable data class ParseResult(val transactions: List<AiDraft> = emptyList(), val message: String? = null)
@Serializable data class ReceiptItem(val name: String = "", val quantity: Double? = null, val total: Double? = null)
@Serializable data class ReceiptDetails(val items: List<ReceiptItem> = emptyList(), val location: String? = null,
    val discount: Double? = null, val tax: Double? = null, val fees: Double? = null, val paymentMethod: String? = null,
    val address: String? = null, val phone: String? = null)
@Serializable data class ScannedReceipt(val transaction: AiDraft = AiDraft(), val details: ReceiptDetails = ReceiptDetails())
@Serializable data class ScanResult(val receipts: List<ScannedReceipt> = emptyList(), val message: String? = null)
@Serializable private data class AssistantReply(val reply: String = "")
@Serializable data class AiUsage(val used: Int = 0, val limit: Int? = null)
@Serializable data class AiStatus(val premium: Boolean = false, val consent: Map<String, Boolean> = emptyMap(),
    val usage: Map<String, AiUsage> = emptyMap(), val month: String = "")
@Serializable data class PremiumState(val active: Boolean = false, val expiresAt: Long? = null, val productId: String? = null,
    val store: String? = null, val manualUntil: Long? = null)

/** Layanan AI belum ter-deploy/aktif di server (NOT_FOUND) atau sementara tidak dapat dijangkau (UNAVAILABLE). */
class AiUnavailableException(message: String, val notDeployed: Boolean, cause: Throwable? = null) : IllegalStateException(message, cause)

/**
 * Menerjemahkan error Cloud Functions menjadi pesan berbahasa aplikasi. Kode dibaca dari `code` (bukan teks pesan),
 * sehingga function yang belum di-deploy (NOT_FOUND) atau server yang tidak terjangkau (UNAVAILABLE) selalu
 * menghasilkan keadaan "layanan AI belum tersedia" yang diterjemahkan, bukan pesan teknis.
 */
private fun FirebaseFunctionsException.toClientError(fallback: String): Exception {
    val text = message.orEmpty()
    val raw = text.isBlank() || text.matches(Regex("[A-Z_]+"))
    return when (code) {
        FunctionsExceptionCode.NOT_FOUND, FunctionsExceptionCode.UNIMPLEMENTED ->
            AiUnavailableException(MR.strings.pd_ai_service_not_available.text(), notDeployed = true, cause = this)
        FunctionsExceptionCode.UNAVAILABLE, FunctionsExceptionCode.DEADLINE_EXCEEDED ->
            AiUnavailableException(MR.strings.pd_ai_service_busy.text(), notDeployed = false, cause = this)
        FunctionsExceptionCode.UNAUTHENTICATED -> IllegalStateException(MR.strings.ai_err_session_expired.text(), this)
        // Server mengirim "belum dikonfigurasi" bila secret/kunci AI belum diisi (functions/ai.js).
        FunctionsExceptionCode.INTERNAL -> if (text.contains("dikonfigurasi")) AiUnavailableException(MR.strings.pd_ai_service_not_available.text(), notDeployed = true, cause = this)
            else IllegalStateException(if (raw) fallback else text, this)
        // Penyedia AI saat ini (DeepSeek) tidak menerima gambar; server menolak AI Scan dengan failed-precondition (functions/deepseek.js).
        FunctionsExceptionCode.FAILED_PRECONDITION -> when {
            text.startsWith("AI Scan belum didukung") -> IllegalStateException(MR.strings.pd_ai_scan_not_supported.text(), this)
            text.startsWith("Setujui pemrosesan AI") -> IllegalStateException(MR.strings.pd_ai_consent_required.text(), this)
            else -> IllegalStateException(if (raw) fallback else text, this)
        }
        else ->IllegalStateException(if (raw) fallback else text, this)
    }
}

class AiService {
    private val functions get() = Firebase.functions("asia-southeast2")
    private suspend inline fun <reified T, reified R> call(name: String, data: T, timeoutSeconds: Int = 120): R = try {
        functions.httpsCallable(name, timeoutSeconds.seconds)(data).data<R>()
    } catch (e: FirebaseFunctionsException) { throw e.toClientError(MR.strings.ai_err_generic.text()) }
    catch (e: SerializationException) { throw IllegalStateException(MR.strings.ai_err_generic.text(), e) }

    suspend fun status(): AiStatus = try { functions.httpsCallable("aiStatus", 30.seconds)().data<AiStatus>() }
        catch (e: FirebaseFunctionsException) { throw e.toClientError(MR.strings.ai_err_status_unavailable.text()) }
        catch (e: SerializationException) { throw IllegalStateException(MR.strings.ai_err_status_unavailable.text(), e) }
    suspend fun setConsent(feature: AiFeature, granted: Boolean) { call<ConsentRequest, Map<String, String?>>("aiConsent", ConsentRequest(feature.id, granted), 30) }
    suspend fun parse(feature: AiFeature, text: String, context: AiContext): ParseResult =
        call("aiParseText", ParseRequest(feature.id, text.take(2000), context))
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun scan(jpeg: ByteArray, ownerName: String, context: AiContext): ScanResult =
        call("aiScanReceipt", ScanRequest(Base64.encode(jpeg), "image/jpeg", ownerName.take(80), context), 150)
    suspend fun ask(turns: List<AssistantTurn>, summary: String, settings: AssistantSettings, language: String): String =
        call<AssistantRequest, AssistantReply>("aiAssistant", AssistantRequest(turns.takeLast(19).let { if (it.first().role != "user") it.drop(1) else it },
            summary.take(12000), settings.personality, settings.nickname.take(40), settings.preference.take(200), language)).reply
    suspend fun refreshPremium(): PremiumState = try { functions.httpsCallable("refreshPremium", 60.seconds)().data<PremiumState>() }
        catch (e: FirebaseFunctionsException) { throw e.toClientError(MR.strings.ai_err_premium_check.text()) }
}

fun aiContext(data: FinanceData, language: String): AiContext {
    val base = data.profile?.baseCurrency ?: "IDR"
    return AiContext(today().toString(), base, language,
        data.categories.filter { it.parentId == null }.take(200).map { AiCategory(it.id, it.name.take(120), it.type) },
        data.accounts.filter { !it.archived && it.type != "savings" }.take(100).map { AiWallet(it.id, it.name.take(120), it.currency) })
}

/** Draf AI selalu ditinjau pengguna di editor sebelum disimpan. */
fun AiDraft.toTransaction(data: FinanceData, fallbackWalletId: String, note: String? = null): Transaction? {
    val wallet = data.accounts.firstOrNull { it.id == walletId && !it.archived }
        ?: data.accounts.firstOrNull { it.id == fallbackWalletId && !it.archived }
        ?: data.accounts.firstOrNull { !it.archived && it.type != "savings" } ?: return null
    val kind = if (type in listOf("income", "transfer")) type else "expense"
    val target = toWalletId?.takeIf { kind == "transfer" && it != wallet.id && data.accounts.any { a -> a.id == it && !a.archived } }
    val finalType = if (kind == "transfer" && target == null) "expense" else kind
    val category = categoryId?.takeIf { id -> data.categories.any { it.id == id && it.type == finalType } }
        ?: data.categories.firstOrNull { it.parentId == null && it.type == finalType && it.id.startsWith("other") }?.id
        ?: data.categories.firstOrNull { it.parentId == null && it.type == finalType }?.id ?: ""
    val minor = (amount * 10.0.pow(currencyDigits(wallet.currency))).roundToLong()
    if (minor <= 0) return null
    val day = date?.takeIf { Regex("\\d{4}-\\d{2}-\\d{2}").matches(it) } ?: today().toString()
    return Transaction(id = newId(), type = finalType, amountMinor = minor, currency = wallet.currency, accountId = wallet.id,
        categoryId = if (finalType == "transfer") "transfer" else category, title = title.ifBlank { transactionTypeLabel(if (finalType == "income") "income" else "expense") }.take(100),
        description = listOfNotNull(this.note, note).filter { it.isNotBlank() }.joinToString("\n").take(500), date = day,
        toAccountId = target, receivedAmountMinor = if (target != null) minor else null)
}

/** Ringkasan yang dikirim ke Asisten: total, kategori, anggaran, saldo, tagihan — tanpa transaksi satu per satu. */
fun assistantSummary(data: FinanceData): String {
    val base = data.profile?.baseCurrency ?: "IDR"
    val month = today().toString().take(7)
    val previous = today().let { d -> if (d.monthNumber == 1) "${d.year - 1}-12" else "${d.year}-${(d.monthNumber - 1).toString().padStart(2, '0')}" }
    val dayOfMonth = today().dayOfMonth
    fun totals(m: String) = data.transactions.filter { !it.scheduled && it.date.startsWith(m) && it.currency == base }
    val names = data.categories.associate { it.id to it.name }
    val lines = mutableListOf("Tanggal hari ini: ${today()} (hari ke-$dayOfMonth bulan ini). Mata uang utama: $base.")
    listOf("Bulan ini ($month)" to month, "Bulan lalu ($previous)" to previous).forEach { (label, m) ->
        val list = totals(m)
        lines += "$label: pemasukan ${money(list.filter { it.type == "income" }.sumOf { it.amountMinor }, base)}, pengeluaran ${money(list.filter { it.type == "expense" }.sumOf { it.amountMinor }, base)}, ${list.size} transaksi."
    }
    val current = totals(month)
    current.filter { it.type == "expense" }.groupBy { it.categoryId }.mapValues { (_, v) -> v.sumOf { it.amountMinor } }
        .entries.sortedByDescending { it.value }.take(6).takeIf { it.isNotEmpty() }?.let { top ->
            lines += "Kategori pengeluaran teratas bulan ini: " + top.joinToString("; ") { "${names[it.key] ?: it.key} ${money(it.value, base)}" }
        }
    current.filter { it.type == "income" }.groupBy { it.categoryId }.mapValues { (_, v) -> v.sumOf { it.amountMinor } }
        .entries.sortedByDescending { it.value }.take(4).takeIf { it.isNotEmpty() }?.let { top ->
            lines += "Sumber pemasukan bulan ini: " + top.joinToString("; ") { "${names[it.key] ?: it.key} ${money(it.value, base)}" }
        }
    data.budgets.filter { it.month == month || it.period != "monthly" }.take(10).forEach { budget ->
        val spent = current.filter { it.type == "expense" && it.categoryId == budget.categoryId }.sumOf { it.amountMinor }
        lines += "Anggaran ${budget.title.ifBlank { names[budget.categoryId] ?: budget.categoryId }}: terpakai ${money(spent, budget.currency)} dari ${money(budget.limitMinor, budget.currency)}."
    }
    data.accounts.filter { !it.archived }.take(15).forEach { lines += "Saldo dompet ${it.name} (${it.type}): ${money(data.balance(it), it.currency)}." }
    data.bills.filter { !it.archived && it.paidAt == 0L }.sortedBy { it.dueDate }.take(10).forEach {
        lines += "Tagihan belum dibayar: ${it.title} ${money(it.amountMinor, it.currency)}, jatuh tempo ${it.dueDate}."
    }
    data.goals.take(6).forEach { lines += "Target ${it.name}: terkumpul ${money(it.savedMinor, it.currency)} dari ${money(it.targetMinor, it.currency)}." }
    return lines.joinToString("\n").take(12000)
}

// ---- Pengaturan perangkat (riwayat Asisten maksimal 5 percakapan, seperti Ollo) ----
@Serializable data class AssistantSettings(val personality: String = "friendly", val nickname: String = "", val preference: String = "")
@Serializable data class AssistantConversation(val id: String = newId(), val title: String = "", val updatedAt: Long = 0, val turns: List<AssistantTurn> = emptyList())
@Serializable data class ScanNoteField(val id: String, val enabled: Boolean)
@Serializable data class ScanSettings(val ownerName: String = "", val fields: List<ScanNoteField> = defaultScanFields)

val defaultScanFields = listOf(
    ScanNoteField("items", true), ScanNoteField("location", true), ScanNoteField("discount", true), ScanNoteField("tax", true),
    ScanNoteField("fees", true), ScanNoteField("payment", true), ScanNoteField("address", false), ScanNoteField("phone", false),
)

private val aiJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }
class AiLocalStore(private val native: NativeServices, owner: String) {
    private val prefix = "ai.$owner"
    fun assistantSettings() = read("$prefix.assistant", AssistantSettings())
    fun saveAssistantSettings(value: AssistantSettings) = write("$prefix.assistant", value)
    fun history(): List<AssistantConversation> = read("$prefix.history", emptyList<AssistantConversation>())
    fun saveConversation(value: AssistantConversation) {
        if (value.turns.isEmpty()) return
        write("$prefix.history", (listOf(value) + history().filter { it.id != value.id }).sortedByDescending { it.updatedAt }.take(5))
    }
    fun deleteConversation(id: String) = write("$prefix.history", history().filter { it.id != id })
    fun scanSettings(): ScanSettings = read("$prefix.scan", ScanSettings()).let { saved ->
        // Field baru dari versi aplikasi berikutnya ditambahkan di akhir tanpa mengubah urutan pilihan pengguna.
        val known = saved.fields.filter { field -> defaultScanFields.any { it.id == field.id } }.distinctBy { it.id }
        saved.copy(fields = known + defaultScanFields.filter { field -> known.none { it.id == field.id } })
    }
    fun saveScanSettings(value: ScanSettings) = write("$prefix.scan", value.copy(ownerName = value.ownerName.take(80)))
    private inline fun <reified T> read(key: String, fallback: T): T = runCatching { native.readSetting(key)?.let { aiJson.decodeFromString<T>(it) } }.getOrNull() ?: fallback
    private inline fun <reified T> write(key: String, value: T) = native.writeSetting(key, aiJson.encodeToString(value))
}

fun receiptNote(details: ReceiptDetails, settings: ScanSettings, currency: String): String {
    fun amount(value: Double?) = value?.let { v -> money((v * 10.0.pow(currencyDigits(currency))).roundToLong(), currency) }
    return settings.fields.filter { it.enabled }.mapNotNull { field ->
        when (field.id) {
            "items" -> details.items.takeIf { it.isNotEmpty() }?.joinToString("\n") { item ->
                item.name + (item.quantity?.let { q -> " x${if (q % 1.0 == 0.0) q.toLong() else q}" } ?: "") + (amount(item.total)?.let { ": $it" } ?: "")
            }
            "location" -> details.location?.let { MR.strings.ai_receipt_location.text(it) }
            "discount" -> amount(details.discount)?.let { MR.strings.ai_receipt_discount.text(it) }
            "tax" -> amount(details.tax)?.let { MR.strings.ai_receipt_tax.text(it) }
            "fees" -> amount(details.fees)?.let { MR.strings.ai_receipt_fees.text(it) }
            "payment" -> details.paymentMethod?.let { MR.strings.ai_receipt_payment.text(it) }
            "address" -> details.address?.let { MR.strings.ai_receipt_address.text(it) }
            "phone" -> details.phone?.let { MR.strings.ai_receipt_phone.text(it) }
            else -> null
        }
    }.joinToString("\n")
}
