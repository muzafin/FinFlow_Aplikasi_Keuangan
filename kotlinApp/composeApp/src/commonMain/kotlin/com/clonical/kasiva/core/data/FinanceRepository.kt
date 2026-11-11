package com.clonical.kasiva.core.data

import com.clonical.kasiva.i18n.*
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.model.Account
import com.clonical.kasiva.core.model.Budget
import com.clonical.kasiva.core.model.Category
import com.clonical.kasiva.core.model.FinanceData
import com.clonical.kasiva.core.model.Goal
import com.clonical.kasiva.core.model.ImportPlan
import com.clonical.kasiva.core.model.Investment
import com.clonical.kasiva.core.model.Profile
import com.clonical.kasiva.core.model.Transaction
import com.clonical.kasiva.core.model.defaultCategories
import com.clonical.kasiva.core.model.defaultCategoryTree
import com.clonical.kasiva.core.model.managedTransactionSources
import com.clonical.kasiva.core.util.nowMillis
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.EmailAuthProvider
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.FirebaseFirestoreException
import dev.gitlive.firebase.firestore.FirestoreExceptionCode
import dev.gitlive.firebase.functions.functions
import com.clonical.kasiva.core.platform.NativeServices
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.flow.*
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import com.clonical.kasiva.core.model.AppPreferences
import com.clonical.kasiva.core.model.PreferenceState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString

@Serializable
private data class InitialProfile(val id: String, val name: String, val email: String, val baseCurrency: String, val schemaVersion: Int)

@Serializable
private data class InitialWallet(
    val id: String, val name: String, val type: String, val currency: String,
    val openingBalanceMinor: Long, val archived: Boolean, val createdAt: Long,
)

@Serializable
private data class CloudPreferences(val version: Int = 1, val payload: String)

/** Dokumen `users/{uid}/devices/{deviceId}` (lihat firestore.rules). */
@Serializable
data class DeviceRecord(val name: String, val platform: String, val appVersion: String, val createdAt: Long, val lastActiveAt: Long, val revoked: Boolean = false)

data class ConnectedDevice(val id: String, val record: DeviceRecord)

/** Hasil pengiriman antrean transaksi offline. */
data class PendingFlushResult(val sent: Int = 0, val failed: List<String> = emptyList())

class FinanceRepository(app:FirebaseApp?=null, local:LocalFinancePersistence?=null,
    private val pending: PendingTransactionStore? = null, private val online: () -> Boolean = { true }) {
    val isLocal = local != null
    val pendingStore: PendingTransactionStore? get() = pending
    val pendingOperations get() = pending?.operations ?: emptyPending
    private val emptyPending = MutableStateFlow<List<PendingTransactionOp>>(emptyList())
    private val auth by lazy { if(app==null)Firebase.auth else Firebase.auth(app) }
    private val db = if(local!=null)FinanceDatabase(local=local) else FinanceDatabase(remote=if(app==null)Firebase.firestore else Firebase.firestore(app))
    val authChanges get() = if(isLocal) flowOf(null) else auth.authStateChanged
    val currentUser get() = if(isLocal) null else auth.currentUser
    val ownerId get() = if(isLocal) "local" else auth.currentUser?.uid
    private fun root() = db.collection("users").document(checkNotNull(ownerId) { MR.strings.core_error_please_login.text() })
    private fun collection(name: String) = root().collection(name)
    val planner get() = PlanningRepository(db, root())
    suspend fun synchronizePreferences(state: PreferenceState) = coroutineScope {
        require(!isLocal)
        val ref = collection("settings").document("appearance")
        // Dokumen bisa berasal dari build/perangkat lain dengan nilai yang tidak lagi valid: perbaiki,
        // jangan hentikan sinkronisasi. null berarti dokumen tidak terbaca dan akan diganti pengaturan perangkat ini.
        fun read(snapshot: FinanceSnapshot): Pair<AppPreferences, Boolean>? = runCatching {
            val stored = snapshot.data<CloudPreferences>()
            if (stored.version != 1 || stored.payload.length > 16000) return@runCatching null
            val decoded = financeStorageJson.decodeFromString<AppPreferences>(stored.payload)
            runCatching { decoded.also { it.validate() } }.getOrNull()?.let { it to false }
                ?: (decoded.repaired().also { it.validate() } to true)
        }.getOrNull()
        suspend fun heal(repaired: AppPreferences) {
            // Dokumen lama diperbaiki lalu ditulis kembali agar perangkat/versi lain tidak mengulang masalah yang sama.
            if (state.pendingChanges.value == null) runCatching { ref.set(CloudPreferences(payload = financeStorageJson.encodeToString(repaired))) }
        }
        // An existing account wins on a new device; pending edits survive restarts and failed writes.
        val initial = ref.get()
        if (initial.exists) {
            val stored = read(initial)
            if (stored != null) { state.acceptRemote(stored.first); if (stored.second) heal(stored.first) } else state.update(state.value)
        } else state.update(state.value)
        launch {
            ref.snapshots.collect { snapshot ->
                if (snapshot.exists) read(snapshot)?.let { (value, repaired) -> state.acceptRemote(value); if (repaired) heal(value) }
            }
        }
        state.pendingChanges.filterNotNull().collect { value ->
            val payload = financeStorageJson.encodeToString(value)
            require(payload.length <= 16000) { MR.strings.core_error_prefs_too_large.text() }
            ref.set(CloudPreferences(payload = payload))
            state.acknowledge(value)
            read(ref.get())?.first?.let(state::acceptRemote)
        }
    }
    suspend fun restoreBackup(backup:com.clonical.kasiva.core.model.FinanceBackup,resume:Boolean,begin:()->Unit,progress:(String)->Unit) {
        val owner=root()
        BackupRestoreRepository(db,owner).restore(backup,resume,begin,progress)
        updateProfile(requireNotNull(backup.data.profile).name,backup.data.profile.baseCurrency)
    }

    suspend fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password)
        initializeProfile()
    }
    suspend fun register(name: String, email: String, password: String) {
        require(name.isNotBlank() && name.trim().length <= 100 && password.length >= 8) { MR.strings.core_error_register_invalid.text() }
        auth.createUserWithEmailAndPassword(email.trim(), password)
        auth.currentUser?.updateProfile(displayName = name.trim())
        initializeProfile(name.trim())
    }
    suspend fun googleLogin(idToken: String) {
        auth.signInWithCredential(GoogleAuthProvider.credential(idToken, null))
        initializeProfile()
    }
    suspend fun resetPassword(email: String) { auth.sendPasswordResetEmail(email.trim()) }
    suspend fun logout() { auth.signOut() }
    suspend fun clearLocalData() {
        require(isLocal){"Penghapusan data cloud harus melalui server"}
        val root=root()
        db.runTransaction {
            val profile=get(root).data<Profile>()
            requireNotNull(draft).keys.toList().forEach{delete(db.document(it))}
            set(root,profile.copy(photoPath="",photoUpdatedAt=0))
            defaultCategoryTree.forEach{set(root.collection("categories").document(it.id),it)}
            set(root.collection("accounts").document("acc_cash"),Account("acc_cash",MR.strings.core_default_wallet_name.text(),currency=profile.baseCurrency))
        }
    }
    suspend fun resetCloudData(native:NativeServices,password:String?,googleToken:String?) {
        require(!isLocal)
        val user=checkNotNull(currentUser){MR.strings.core_error_relogin_to_reset.text()}
        val credential=if(googleToken!=null)GoogleAuthProvider.credential(googleToken,null)else EmailAuthProvider.credential(requireNotNull(user.email),requireNotNull(password))
        user.reauthenticate(credential);user.getIdToken(true)
        val key="reset.${user.uid}"
        val requestId=native.readSetting(key)?.takeIf{it.isNotBlank()} ?: com.clonical.kasiva.core.util.newId().also{native.writeSetting(key,it)}
        Firebase.functions("asia-southeast2").httpsCallable("resetFinancialData",kotlin.time.Duration.parse("9m"))(mapOf("confirmation" to "HAPUS DATA","requestId" to requestId))
        native.writeSetting(key,"")
    }
    /**
     * "Hapus Transaksi Saja": menghapus semua jurnal, event keuangan, dan klaim reimburse, lalu menyetel ulang
     * status pembayaran tagihan/utang/target/bundle/jadwal agar data yang tersisa tetap memenuhi invarian rules.
     * Dompet, kategori, anggaran, keinginan, kartu, investasi, dan catatan teks tetap. Cloud memakai Cloud Function
     * `deleteTransactions` (Admin SDK, batch); lokal memakai satu transaksi penyimpanan. Aman diulang.
     */
    suspend fun deleteAllTransactions() {
        if (!isLocal) {
            checkNotNull(currentUser) { MR.strings.core_error_please_login_again.text() }
            Firebase.functions("asia-southeast2").httpsCallable("deleteTransactions", kotlin.time.Duration.parse("5m"))(mapOf("confirmation" to "HAPUS TRANSAKSI"))
            return
        }
        val base = root()
        db.runTransaction {
            val prefix = "${base.path}/"
            fun ids(name: String) = requireNotNull(draft).keys.filter { it.startsWith("$prefix$name/") }.map { it.substringAfterLast('/') }
            for (name in listOf("reimbursements", "financialEvents", "transactions")) ids(name).forEach { delete(base.collection(name).document(it)) }
            ids("bills").forEach { id ->
                val ref = base.collection("bills").document(id); val bill = get(ref).data<com.clonical.kasiva.core.model.Bill>()
                if (bill.paidAt != 0L || bill.lastPaymentId != null) set(ref, bill.copy(paidAt = 0, lastPaymentId = null, revision = bill.revision + 1))
            }
            ids("debts").forEach { id ->
                val ref = base.collection("debts").document(id); val debt = get(ref).data<com.clonical.kasiva.core.model.Debt>()
                if (debt.paidMinor != 0L || debt.lastEventId != null) set(ref, debt.copy(paidMinor = 0, lastEventId = null, revision = debt.revision + 1))
            }
            ids("goals").forEach { id ->
                val ref = base.collection("goals").document(id); val goal = get(ref).data<Goal>()
                val holding = goal.holdingAccountId != null
                if (goal.lastEventId != null || holding && goal.savedMinor != goal.legacySavedMinor)
                    set(ref, goal.copy(savedMinor = if (holding) goal.legacySavedMinor else goal.savedMinor, lastEventId = null, revision = goal.revision + 1))
            }
            ids("notes").forEach { id ->
                val ref = base.collection("notes").document(id); val note = get(ref).data<com.clonical.kasiva.core.model.FinanceNote>()
                if (note.lastPaymentId != null || note.items.any { it.paymentId != null })
                    set(ref, note.copy(items = note.items.map { if (it.paymentId == null) it else it.copy(checked = false, paymentId = null) },
                        lastPaymentId = null, revision = note.revision + 1))
            }
            ids("recurring").forEach { id ->
                val ref = base.collection("recurring").document(id); val rule = get(ref).data<com.clonical.kasiva.core.model.RecurringTransaction>()
                if (rule.lastEventId != null) set(ref, rule.copy(lastEventId = null, revision = rule.revision + 1))
            }
        }
    }

    // ---------- Perangkat terhubung (cloud) ----------

    /** Daftar perangkat yang masuk ke akun ini (tidak termasuk yang sudah dicabut). Mode lokal: kosong. */
    fun observeDevices(): Flow<List<ConnectedDevice>> = if (isLocal) flowOf(emptyList()) else collection("devices").snapshots.map { snapshot ->
        snapshot.documents.mapNotNull { doc -> runCatching { ConnectedDevice(doc.id, doc.data<DeviceRecord>()) }.getOrNull() }
            .filterNot { it.record.revoked }.sortedByDescending { it.record.lastActiveAt }
    }
    /**
     * Daftarkan/perbarui perangkat ini. Mengembalikan false bila perangkat ini sudah dicabut dari perangkat lain
     * (pemanggil harus keluar dan melupakan ID-nya).
     */
    suspend fun registerDevice(deviceId: String, name: String, platform: String, appVersion: String): Boolean {
        require(!isLocal && deviceId.matches(DEVICE_ID))
        val ref = collection("devices").document(deviceId)
        val cleanName = name.trim().take(100).ifBlank { "Android" }
        val cleanPlatform = platform.takeIf { it in DEVICE_PLATFORMS } ?: "other"
        val cleanVersion = appVersion.trim().take(40).ifBlank { "1.0.0" }
        return db.runTransaction {
            val snapshot = get(ref)
            val now = nowMillis()
            if (snapshot.exists) {
                val old = snapshot.data<DeviceRecord>()
                if (old.revoked) false
                else { set(ref, old.copy(name = cleanName, platform = cleanPlatform, appVersion = cleanVersion, lastActiveAt = maxOf(now, old.createdAt))); true }
            } else { set(ref, DeviceRecord(cleanName, cleanPlatform, cleanVersion, now, now, false)); true }
        }
    }
    /** true saat dokumen perangkat ini ditandai dicabut oleh perangkat lain. */
    fun deviceRevocations(deviceId: String): Flow<Boolean> = if (isLocal) flowOf(false) else collection("devices").document(deviceId).snapshots
        .map { it.exists && runCatching { it.data<DeviceRecord>().revoked }.getOrDefault(false) }.distinctUntilChanged()
    /** Keluarkan perangkat lain: perangkat itu keluar dari akun saat aplikasinya terbuka berikutnya. */
    suspend fun revokeDevice(deviceId: String) {
        require(!isLocal && deviceId.matches(DEVICE_ID))
        val ref = collection("devices").document(deviceId)
        db.runTransaction { val snapshot = get(ref); if (snapshot.exists) { val old = snapshot.data<DeviceRecord>(); if (!old.revoked) set(ref, old.copy(revoked = true)) } }
    }
    suspend fun forgetDevice(deviceId: String) { if (!isLocal && deviceId.matches(DEVICE_ID)) collection("devices").document(deviceId).delete() }

    suspend fun requestAccountDeletion(password: String?, googleToken: String?) {
        val user = checkNotNull(auth.currentUser) { MR.strings.core_error_please_login_again.text() }
        val credential = if (googleToken != null) GoogleAuthProvider.credential(googleToken, null)
            else EmailAuthProvider.credential(requireNotNull(user.email), requireNotNull(password))
        user.reauthenticate(credential)
        user.getIdToken(true)
        Firebase.functions("asia-southeast2").httpsCallable("requestAccountDeletion")(mapOf("confirmation" to "HAPUS"))
        auth.signOut()
    }
    suspend fun saveProfilePhoto(native: NativeServices, bytes: ByteArray) {
        val ref = root()
        val path = suspendCancellableCoroutine<String> { continuation ->
            native.uploadProfilePhoto(ref.id, bytes) { path, failure ->
                if (continuation.isActive) {
                    if (path != null) continuation.resume(path)
                    else continuation.resumeWithException(IllegalStateException(failure ?: MR.strings.core_error_photo_upload_failed.text()))
                }
            }
        }
        require(ownerId == ref.id) { MR.strings.core_error_account_changed.text() }
        // Tulis biasa (bukan transaksi): saat offline perubahan langsung masuk cache Firestore dan terkirim
        // otomatis ketika online. Konfirmasi server tidak ditunggu agar foto tidak menggantung tanpa internet.
        // Offline, get() dilayani dari cache lokal Firestore.
        val profile = runCatching { kotlinx.coroutines.withTimeoutOrNull(5_000) { ref.get().takeIf { it.exists }?.data<Profile>() } }.getOrNull()
        if (profile != null) kotlinx.coroutines.withTimeoutOrNull(1_500) { ref.set(profile.copy(photoPath = path, photoUpdatedAt = nowMillis())) }
    }
    suspend fun updateProfile(name: String, baseCurrency: String) {
        val displayName = name.trim()
        val code = baseCurrency.trim().uppercase()
        require(displayName.isNotEmpty() && displayName.length <= 100) { MR.strings.core_error_name_required.text() }
        require(code.matches(Regex("[A-Z]{3}"))) { MR.strings.core_error_currency_3_letters.text() }
        val user = currentUser
        val ref = root()
        user?.updateProfile(displayName = displayName)
        db.runTransaction {
            val current = get(ref).data<Profile>()
            set(ref, current.copy(name = displayName, baseCurrency = code))
        }
    }

    suspend fun initializeProfile(displayName: String? = null, baseCurrency: String = "IDR", initialWalletName:String=MR.strings.core_default_wallet_name.text(), openingBalanceMinor:Long=0) {
        require(baseCurrency.matches(Regex("[A-Z]{3}"))) { MR.strings.core_error_currency_invalid.text() }
        require(initialWalletName.isNotBlank() && initialWalletName.length<=100 && openingBalanceMinor in -MAX_MONEY..MAX_MONEY) { MR.strings.core_error_wallet_name_or_balance_invalid.text() }
        val user = currentUser
        val ref = root()
        // Saat offline, pembuatan profil awal tidak mungkin (transaction butuh server); data tetap dibaca dari cache.
        if (!isLocal && !online()) return
        for (attempt in 0..1) {
            var creatingProfile = false
            try {
                db.runTransaction {
                    val snapshot = get(ref)
                    creatingProfile = !snapshot.exists
                    if (!snapshot.exists) {
                        val name = (displayName ?: user?.displayName).orEmpty().trim().take(100).ifBlank { MR.strings.core_default_user_name.text() }
                        set(ref, InitialProfile(ref.id, name, user?.email ?: "", baseCurrency, 2))
                        // Akun baru memakai schema inti agar tetap kompatibel saat rules metadata belum diterbitkan.
                        defaultCategoryTree.forEach {
                            set(ref.collection("categories").document(it.id), mapOf("id" to it.id, "name" to it.name, "type" to it.type, "parentId" to it.parentId))
                        }
                        set(ref.collection("accounts").document("acc_cash"), InitialWallet("acc_cash", initialWalletName.trim(), "cash", baseCurrency, openingBalanceMinor, false, nowMillis()))
                    } else if (displayName != null) {
                        set(ref, snapshot.data<Profile>().copy(name = displayName))
                    }
                }
                break
            } catch (e: Exception) {
                if (!isLocal && isOfflineError(e)) return
                // Bootstrap paralel bisa ditolak rules dompet sebelum Firestore mengulang konflik transaksinya.
                if (attempt != 0 || !creatingProfile || e !is FirebaseFirestoreException || e.code != FirestoreExceptionCode.PERMISSION_DENIED) throw e
            }
        }
        try { seedDefaultCategories() }
        catch (e: Exception) { if (isLocal || !isOfflineError(e)) throw e }
    }

    /**
     * Tambahkan kategori dan subkategori bawaan yang belum ada (mis. akun lama tanpa subkategori).
     * Idempoten: kategori buatan pengguna tidak disentuh dan tidak ada data yang diubah.
     */
    suspend fun ensureDefaultCategories() = seedDefaultCategories()

    private suspend fun seedDefaultCategories() {
        val existing = collection("categories").get().documents.map { it.id }.toSet()
        defaultCategoryTree.filter { it.id !in existing }.forEach { category ->
            collection("categories").document(category.id).set(mapOf(
                "id" to category.id, "name" to category.name, "type" to category.type, "parentId" to category.parentId))
        }
    }

    fun observe(): Flow<FinanceData> {
        val categoryOrder = defaultCategoryTree.mapIndexed { index, category -> category.id to index }.toMap()
        if(isLocal) return db.localSnapshots.map { snapshot ->
            val path="users/local"
            fun docs(name:String)=snapshot.collection("$path/$name").documents
            FinanceData(profile=snapshot.document(path).takeIf{it.exists}?.data<Profile>(),
                accounts=docs("accounts").map{it.data<Account>()}.sortedBy{it.createdAt},
                categories=docs("categories").map{it.data<Category>()}.sortedWith(compareBy<Category>{categoryOrder[it.id] ?: Int.MAX_VALUE}.thenBy{it.name.lowercase()}),
                transactions=docs("transactions").map{it.data<Transaction>()}.sortedByDescending{it.date},
                budgets=docs("budgets").map{it.data<Budget>()},goals=docs("goals").map{it.data<Goal>()},investments=docs("investments").map{it.data<Investment>()},
                bills=docs("bills").map{it.data<com.clonical.kasiva.core.model.Bill>()},debts=docs("debts").map{it.data<com.clonical.kasiva.core.model.Debt>()},
                wishes=docs("wishes").map{it.data<com.clonical.kasiva.core.model.Wish>()},cards=docs("cards").map{it.data<com.clonical.kasiva.core.model.WalletCard>()},
                notes=docs("notes").map{it.data<com.clonical.kasiva.core.model.FinanceNote>()},reimbursements=docs("reimbursements").map{it.data<com.clonical.kasiva.core.model.Reimbursement>()},
                recurring=docs("recurring").map{it.data<com.clonical.kasiva.core.model.RecurringTransaction>()},financialEvents=docs("financialEvents").map{it.data<com.clonical.kasiva.core.model.FinancialEvent>()},featuresReady=true)
        }
        val base = combine(
            collection("accounts").snapshots.map { s -> s.documents.map { it.data<Account>() } },
            collection("categories").snapshots.map { s -> s.documents.map { it.data<Category>() } },
            collection("transactions").snapshots.map { s -> s.documents.map { it.data<Transaction>() }.sortedByDescending { it.date } },
            root().snapshots.map { if (it.exists) it.data<Profile>() else null },
        ) { a, c, t, profile -> FinanceData(accounts = a.sortedBy { it.createdAt },
            categories = c.sortedWith(compareBy<Category> { categoryOrder[it.id] ?: Int.MAX_VALUE }.thenBy { it.name.lowercase() }), transactions = t, profile = profile) }
        val core = combine(base,
            collection("budgets").snapshots.map { s -> s.documents.map { it.data<Budget>() } },
            collection("goals").snapshots.map { s -> s.documents.map { it.data<Goal>() } },
            collection("investments").snapshots.map { s -> s.documents.map { it.data<Investment>() } },
        ) { d, b, g, i -> d.copy(budgets = b, goals = g, investments = i) }
        return combine(core, planner.observe()) { data, extra ->
            extra.copy(accounts = data.accounts, categories = data.categories, transactions = data.transactions,
                budgets = data.budgets, goals = data.goals, investments = data.investments, profile = data.profile)
        }
    }

    suspend fun saveAccount(account: Account) {
        require(account.name.isNotBlank() && account.name.length <= 100) { MR.strings.core_error_wallet_name_required.text() }
        require(account.currency.matches(Regex("[A-Z]{3}"))) { MR.strings.core_error_currency_3_letters_example.text() }
        require(account.openingBalanceMinor in -MAX_MONEY..MAX_MONEY) { MR.strings.core_error_balance_out_of_range.text() }
        require(account.type in listOf("cash","bank","ewallet","credit","investment","loan","savings"))
        require(account.icon.length in 1..100 && account.color in 0xFF000000..0xFFFFFFFF)
        val ref=collection("accounts").document(account.id)
        db.runTransaction {
            val previous=get(ref)
            val parent=account.parentId?.let{require(it!=account.id);get(collection("accounts").document(it)).data<Account>()}
            require(parent==null || !parent.archived && parent.parentId==null && parent.currency==account.currency){MR.strings.core_error_pocket_parent.text()}
            if(previous.exists){
                val old=previous.data<Account>()
                require(old.currency==account.currency && old.createdAt==account.createdAt){MR.strings.core_error_wallet_immutable.text()}
                if(old.type=="savings")require(account.type==old.type && account.goalId==old.goalId && account.openingBalanceMinor==old.openingBalanceMinor && account.parentId==null){MR.strings.core_error_goal_wallet_via_goals.text()}
                else require(account.goalId==null && account.type!="savings")
            } else require(account.type!="savings" && account.goalId==null){MR.strings.core_error_create_savings_via_goals.text()}
            set(ref,account)
        }
    }
    suspend fun saveCategory(category: Category) {
        require(category.name.isNotBlank() && category.name.length <= 100 && category.type in listOf("income", "expense"))
        require(category.icon.length<=100 && (category.color==null || category.color in 0xFF000000..0xFFFFFFFF))
        db.runTransaction {
            val previous=get(collection("categories").document(category.id))
            val parent=category.parentId?.let{require(it!=category.id);get(collection("categories").document(it)).data<Category>()}
            require(parent==null || parent.type==category.type && parent.parentId==null){MR.strings.core_error_category_parent_type.text()}
            if(previous.exists){val old=previous.data<Category>();require(old.type==category.type && old.parentId==category.parentId){MR.strings.core_error_category_immutable.text()}}
            set(collection("categories").document(category.id),category)
        }
    }

    /** Kegagalan jaringan Firestore: aman diantrikan di perangkat lalu dikirim ulang saat online. */
    private fun isOfflineError(e: Exception): Boolean {
        val text = (e.message ?: "").lowercase()
        return "unavailable" in text || "offline" in text || "unable to resolve host" in text ||
            "network" in text || "timeout" in text || "deadline" in text
    }

    /**
     * Kirim antrean transaksi offline bila internet tersedia. Mengembalikan jumlah terkirim dan
     * pesan kegagalan validasi (mis. dompet sudah diarsipkan di perangkat lain).
     */
    suspend fun flushPendingTransactions(): PendingFlushResult {
        val store = pending ?: return PendingFlushResult()
        if (isLocal || !online()) return PendingFlushResult()
        var sent = 0
        val failed = mutableListOf<String>()
        for (operation in store.operations.value) {
            try {
                val tx = operation.transaction
                if (tx == null) { store.remove(operation); continue }
                if (operation.remove) writeDelete(tx) else writeTransaction(tx)
                store.remove(operation)
                sent++
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (isOfflineError(e)) break
                store.remove(operation)
                failed += (operation.transaction?.title ?: operation.id) + ": " + (e.message ?: "")
            }
        }
        return PendingFlushResult(sent, failed)
    }

    suspend fun saveTransaction(tx: Transaction) {
        validateTransaction(tx)
        require(tx.source !in managedTransactionSources) { MR.strings.core_error_managed_transaction.text() }
        if (!isLocal && pending != null && !online()) { pending.enqueueSave(tx); return }
        try { writeTransaction(tx) }
        catch (e: Exception) {
            if (isLocal || pending == null || !isOfflineError(e)) throw e
            pending.enqueueSave(tx)
        }
    }

    private suspend fun writeTransaction(tx: Transaction) {
        val accounts = collection("accounts")
        val ref = collection("transactions").document(tx.id)
        db.runTransaction {
            val previous = get(ref)
            val source = get(accounts.document(tx.accountId)).data<Account>()
            val target = tx.toAccountId?.let { get(accounts.document(it)).data<Account>() }
            if(tx.type!="transfer")require(get(collection("categories").document(tx.categoryId)).data<Category>().type==tx.type){MR.strings.core_error_category_type_mismatch.text()}
            tx.subcategoryId?.let { id -> val sub=get(collection("categories").document(id)).data<Category>();require(sub.parentId==tx.categoryId && sub.type==tx.type){MR.strings.core_error_subcategory_mismatch.text()} }
            if (previous.exists) {
                val claim = get(collection("reimbursements").document(tx.id))
                if (claim.exists) {
                    val old = previous.data<Transaction>()
                    require(tx.type == old.type && tx.amountMinor == old.amountMinor && tx.currency == old.currency && tx.accountId == old.accountId && !tx.scheduled) {
                        MR.strings.core_error_reimburse_blocks_edit.text()
                    }
                }
            }
            require(!source.archived && source.currency == tx.currency) { MR.strings.core_error_wallet_inactive_or_currency.text() }
            require(source.type!="savings" && target?.type!="savings"){MR.strings.core_error_use_goal_deposit.text()}
            if (target != null) require(!target.archived) { MR.strings.core_error_target_wallet_inactive.text() }
            set(ref, tx.copy(updatedAt = nowMillis()))
        }
    }
    suspend fun deleteTransaction(tx: Transaction) {
        if (!isLocal && pending != null && !online()) { pending.enqueueDelete(tx); return }
        try { writeDelete(tx) }
        catch (e: Exception) {
            if (isLocal || pending == null || !isOfflineError(e)) throw e
            pending.enqueueDelete(tx)
        }
    }

    private suspend fun writeDelete(tx: Transaction) {
        if (tx.source in managedTransactionSources) planner.reverseEvent(tx.id)
        else db.runTransaction {
            require(!get(collection("reimbursements").document(tx.id)).exists) { MR.strings.core_error_reimburse_blocks_delete.text() }
            delete(collection("transactions").document(tx.id))
        }
    }
    suspend fun importNotification(tx: Transaction): Boolean {
        validateTransaction(tx)
        require(tx.source == "notification" && tx.sourceId?.matches(Regex("[a-f0-9]{64}")) == true && tx.id == "notification-${tx.sourceId}") { "Identitas kandidat notifikasi tidak valid" }
        val base = root()
        val ref = base.collection("transactions").document(tx.id)
        return db.runTransaction {
            if (get(ref).exists) false else {
                val account = get(base.collection("accounts").document(tx.accountId)).data<Account>()
                require(!account.archived && account.currency == tx.currency) { MR.strings.core_error_pick_active_wallet.text() }
                if (tx.toAccountId != null) {
                    require(!get(base.collection("accounts").document(tx.toAccountId)).data<Account>().archived) { MR.strings.core_error_target_wallet_inactive.text() }
                }
                set(ref, tx.copy(updatedAt = nowMillis()))
                true
            }
        }
    }

    suspend fun saveBudget(budget: Budget) {
        require(budget.limitMinor in 1..MAX_MONEY && budget.title.length<=100 && budget.period in listOf("monthly","weekly")) { MR.strings.core_error_budget_invalid.text() }
        if(budget.period=="monthly") {require(budget.month.matches(Regex("\\d{4}-\\d{2}")));LocalDate.parse("${budget.month}-01")} else LocalDate.parse(budget.month)
        require(collection("categories").document(budget.categoryId).get().data<Category>().type=="expense"){MR.strings.core_error_pick_expense_category.text()}
        collection("budgets").document(budget.id).set(budget)
    }
    suspend fun deleteBudget(id: String) { collection("budgets").document(id).delete() }
    suspend fun saveGoal(goal: Goal) {
        require(goal.name.isNotBlank() && goal.name.length <= 100 && goal.targetMinor in 1..MAX_MONEY && goal.savedMinor in 0..MAX_MONEY) { MR.strings.core_error_goal_invalid.text() }
        require(goal.currency.matches(Regex("[A-Z]{3}"))) { MR.strings.core_error_currency_invalid.text() }
        if(goal.targetDate.isNotBlank())LocalDate.parse(goal.targetDate)
        require(goal.bucketType in listOf("standard","emergency","locked") && goal.protection in listOf("flexible","soft","hard"))
        val ref=collection("goals").document(goal.id)
        db.runTransaction {
            val snapshot=get(ref)
            if(snapshot.exists) {
                val old=snapshot.data<Goal>()
                require(old.revision==goal.revision){MR.strings.core_error_goal_changed.text()}
                require(goal.savedMinor==old.savedMinor && goal.holdingAccountId==old.holdingAccountId && goal.legacySavedMinor==old.legacySavedMinor && goal.lastEventId==old.lastEventId){MR.strings.core_error_goal_use_deposit.text()}
                require(goal.currency==old.currency){MR.strings.core_error_goal_currency_immutable.text()}
                require(!goal.archived || goal.holdingAccountId==null || goal.savedMinor==goal.legacySavedMinor){MR.strings.core_error_goal_withdraw_before_archive.text()}
                val holding=old.holdingAccountId?.let{get(collection("accounts").document(it)).data<Account>()}
                if(holding!=null && (holding.name!=goal.name || holding.archived!=goal.archived))set(collection("accounts").document(holding.id),holding.copy(name=goal.name,archived=goal.archived))
                set(ref,goal.copy(revision=old.revision+1))
            } else {
                require(goal.holdingAccountId==null && goal.lastEventId==null && goal.legacySavedMinor==0L)
                val holding=Account(id="goal-${goal.id}",name=goal.name,type="savings",currency=goal.currency,goalId=goal.id)
                set(collection("accounts").document(holding.id),holding)
                set(ref,goal.copy(holdingAccountId=holding.id,legacySavedMinor=goal.savedMinor,revision=0))
            }
        }
    }
    suspend fun deleteGoal(id:String) {
        val ref=collection("goals").document(id)
        db.runTransaction {
            val goal=get(ref).data<Goal>()
            val holding=goal.holdingAccountId?.let{get(collection("accounts").document(it)).data<Account>()}
            require(goal.holdingAccountId==null || goal.savedMinor==goal.legacySavedMinor){MR.strings.core_error_goal_withdraw_all_before_archive.text()}
            if(holding!=null){set(collection("accounts").document(holding.id),holding.copy(archived=true));set(ref,goal.copy(archived=true,revision=goal.revision+1))}
            else delete(ref)
        }
    }
    suspend fun saveInvestment(investment: Investment) {
        require(investment.name.isNotBlank() && investment.name.length <= 100 && investment.platform.length <= 100 && investment.notes.length <= 4000 && investment.investedMinor in 1..MAX_MONEY && investment.currentMinor in 0..MAX_MONEY && investment.dividendsMinor in 0..MAX_MONEY) { MR.strings.core_error_investment_invalid.text() }
        require(investment.currency.matches(Regex("[A-Z]{3}"))) { MR.strings.core_error_currency_invalid.text() }
        collection("investments").document(investment.id).set(investment)
    }
    suspend fun deleteInvestment(id: String) { collection("investments").document(id).delete() }

    suspend fun importCsv(plan: ImportPlan, progress: (Int, Int) -> Unit): Int {
        require(plan.errors.isEmpty() && plan.transactions.isNotEmpty()) { MR.strings.core_error_csv_fix_errors.text() }
        val ref = root() // UID ditangkap agar satu proses tidak berpindah ke akun lain.
        for (account in plan.accounts) {
            db.runTransaction {
                val doc = ref.collection("accounts").document(account.id)
                if (!get(doc).exists) set(doc, account)
            }
        }
        for (category in plan.categories) {
            db.runTransaction {
                val doc = ref.collection("categories").document(category.id)
                if (!get(doc).exists) set(doc, category)
            }
        }
        var added = 0
        plan.transactions.forEachIndexed { index, tx ->
            validateTransaction(tx)
            val inserted = db.runTransaction {
                val doc = ref.collection("transactions").document(tx.id)
                if (get(doc).exists) false else { set(doc, tx); true }
            }
            if (inserted) added++
            progress(index + 1, plan.transactions.size)
        }
        return added
    }

    companion object {
        val DEVICE_ID = Regex("[A-Za-z0-9_-]{16,64}")
        val DEVICE_PLATFORMS = setOf("android", "ios", "desktop", "web", "other")
        const val MAX_MONEY = 9_000_000_000_000_000L
        fun validateTransaction(tx: Transaction) {
            require(tx.type in listOf("income", "expense", "transfer") && tx.amountMinor in 1..MAX_MONEY) { MR.strings.core_error_tx_type_amount_invalid.text() }
            require(tx.accountId.isNotBlank() && tx.categoryId.isNotBlank() && tx.title.length <= 300 && tx.description.length <= 4000)
            require(tx.currency.matches(Regex("[A-Z]{3}"))) { MR.strings.core_error_currency_invalid.text() }
            require(tx.date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) { MR.strings.core_error_date_format.text() }
            LocalDate.parse(tx.date)
            require(tx.tags.size<=10 && tx.tags.distinct().size==tx.tags.size && tx.tags.all{it.isNotBlank() && it.length<=30}){MR.strings.core_error_tags_limit.text()}
            require(tx.subcategoryId==null || tx.subcategoryId.isNotBlank() && tx.subcategoryId.length<=1000)
            require(tx.originalCurrency==null && tx.originalAmountMinor==null || tx.type!="transfer" && tx.originalCurrency!=tx.currency && tx.originalCurrency.orEmpty().matches(Regex("[A-Z]{3}")) && tx.originalAmountMinor in 1..MAX_MONEY)
            if (tx.type == "transfer") require(!tx.toAccountId.isNullOrBlank() && tx.toAccountId != tx.accountId && tx.receivedAmountMinor != null && tx.receivedAmountMinor in 1..MAX_MONEY) { MR.strings.core_error_transfer_requires.text() }
            else require(tx.toAccountId == null && tx.receivedAmountMinor == null)
        }
    }
}
