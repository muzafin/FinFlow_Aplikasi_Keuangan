package com.logikawan.finflow.core.data

import com.logikawan.finflow.core.model.Account
import com.logikawan.finflow.core.model.Budget
import com.logikawan.finflow.core.model.Category
import com.logikawan.finflow.core.model.FinanceData
import com.logikawan.finflow.core.model.Goal
import com.logikawan.finflow.core.model.ImportPlan
import com.logikawan.finflow.core.model.Investment
import com.logikawan.finflow.core.model.Profile
import com.logikawan.finflow.core.model.Transaction
import com.logikawan.finflow.core.model.defaultCategories
import com.logikawan.finflow.core.model.managedTransactionSources
import com.logikawan.finflow.core.util.nowMillis
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.EmailAuthProvider
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.functions.functions
import com.logikawan.finflow.core.platform.NativeServices
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.flow.*
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import com.logikawan.finflow.core.model.AppPreferences
import com.logikawan.finflow.core.model.PreferenceState
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

class FinanceRepository(app:FirebaseApp?=null, local:LocalFinancePersistence?=null) {
    val isLocal = local != null
    private val auth by lazy { if(app==null)Firebase.auth else Firebase.auth(app) }
    private val db = if(local!=null)FinanceDatabase(local=local) else FinanceDatabase(remote=if(app==null)Firebase.firestore else Firebase.firestore(app))
    val authChanges get() = if(isLocal) flowOf(null) else auth.authStateChanged
    val currentUser get() = if(isLocal) null else auth.currentUser
    val ownerId get() = if(isLocal) "local" else auth.currentUser?.uid
    private fun root() = db.collection("users").document(checkNotNull(ownerId) { "Silakan login" })
    private fun collection(name: String) = root().collection(name)
    val planner get() = PlanningRepository(db, root())
    suspend fun synchronizePreferences(state: PreferenceState) = coroutineScope {
        require(!isLocal)
        val ref = collection("settings").document("appearance")
        fun decode(snapshot: FinanceSnapshot): AppPreferences {
            val stored = snapshot.data<CloudPreferences>()
            require(stored.version == 1 && stored.payload.length <= 16000) { "Versi preferensi cloud belum didukung" }
            return financeStorageJson.decodeFromString<AppPreferences>(stored.payload).also { it.validate() }
        }
        // An existing account wins on a new device; pending edits survive restarts and failed writes.
        val initial = ref.get()
        if (initial.exists) state.acceptRemote(decode(initial)) else state.update(state.value)
        launch {
            ref.snapshots.collect { snapshot -> if (snapshot.exists) state.acceptRemote(decode(snapshot)) }
        }
        state.pendingChanges.filterNotNull().collect { value ->
            val payload = financeStorageJson.encodeToString(value)
            require(payload.length <= 16000) { "Preferensi terlalu besar" }
            ref.set(CloudPreferences(payload = payload))
            state.acknowledge(value)
            val latest = ref.get()
            if (latest.exists) state.acceptRemote(decode(latest))
        }
    }
    suspend fun restoreBackup(backup:com.logikawan.finflow.core.model.FinanceBackup,resume:Boolean,begin:()->Unit,progress:(String)->Unit) {
        val owner=root()
        BackupRestoreRepository(db,owner).restore(backup,resume,begin,progress)
        updateProfile(requireNotNull(backup.data.profile).name,backup.data.profile.baseCurrency)
    }

    suspend fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password)
        initializeProfile()
    }
    suspend fun register(name: String, email: String, password: String) {
        require(name.isNotBlank() && name.trim().length <= 100 && password.length >= 8) { "Nama 1–100 karakter; kata sandi minimal 8 karakter" }
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
            defaultCategories.forEach{set(root.collection("categories").document(it.id),it)}
            set(root.collection("accounts").document("acc_cash"),Account("acc_cash","Kas",currency=profile.baseCurrency))
        }
    }
    suspend fun resetCloudData(native:NativeServices,password:String?,googleToken:String?) {
        require(!isLocal)
        val user=checkNotNull(currentUser){"Masuk kembali untuk menghapus data cloud"}
        val credential=if(googleToken!=null)GoogleAuthProvider.credential(googleToken,null)else EmailAuthProvider.credential(requireNotNull(user.email),requireNotNull(password))
        user.reauthenticate(credential);user.getIdToken(true)
        val key="reset.${user.uid}"
        val requestId=native.readSetting(key)?.takeIf{it.isNotBlank()} ?: com.logikawan.finflow.core.util.newId().also{native.writeSetting(key,it)}
        Firebase.functions("asia-southeast2").httpsCallable("resetFinancialData",kotlin.time.Duration.parse("9m"))(mapOf("confirmation" to "HAPUS DATA","requestId" to requestId))
        native.writeSetting(key,"")
    }
    suspend fun requestAccountDeletion(password: String?, googleToken: String?) {
        val user = checkNotNull(auth.currentUser) { "Silakan login kembali" }
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
                    else continuation.resumeWithException(IllegalStateException(failure ?: "Foto gagal diunggah"))
                }
            }
        }
        require(ownerId == ref.id) { "Akun berubah. Silakan masuk kembali." }
        db.runTransaction { val profile=get(ref).data<Profile>();set(ref,profile.copy(photoPath=path,photoUpdatedAt=nowMillis())) }
    }
    suspend fun updateProfile(name: String, baseCurrency: String) {
        val displayName = name.trim()
        val code = baseCurrency.trim().uppercase()
        require(displayName.isNotEmpty() && displayName.length <= 100) { "Nama wajib diisi, maksimal 100 karakter" }
        require(code.matches(Regex("[A-Z]{3}"))) { "Gunakan kode mata uang 3 huruf" }
        val user = currentUser
        val ref = root()
        user?.updateProfile(displayName = displayName)
        db.runTransaction {
            val current = get(ref).data<Profile>()
            set(ref, current.copy(name = displayName, baseCurrency = code))
        }
    }

    suspend fun initializeProfile(displayName: String? = null, baseCurrency: String = "IDR", initialWalletName:String="Kas", openingBalanceMinor:Long=0) {
        require(baseCurrency.matches(Regex("[A-Z]{3}"))) { "Kode mata uang tidak valid" }
        require(initialWalletName.isNotBlank() && initialWalletName.length<=100 && openingBalanceMinor in -MAX_MONEY..MAX_MONEY) { "Nama atau saldo dompet tidak valid" }
        val user = currentUser
        val ref = root()
        // Transaksi ini aman diulang ketika pendaftaran terputus setelah akun Auth terbentuk.
        db.runTransaction {
            val snapshot = get(ref)
            if (!snapshot.exists) {
                val name = (displayName ?: user?.displayName).orEmpty().trim().take(100).ifBlank { "Pengguna" }
                set(ref, InitialProfile(ref.id, name, user?.email ?: "", baseCurrency, 2))
                // Akun baru memakai schema inti agar tetap kompatibel saat rules metadata belum diterbitkan.
                defaultCategories.forEach {
                    set(ref.collection("categories").document(it.id), mapOf("id" to it.id, "name" to it.name, "type" to it.type))
                }
                set(ref.collection("accounts").document("acc_cash"), InitialWallet("acc_cash", initialWalletName.trim(), "cash", baseCurrency, openingBalanceMinor, false, nowMillis()))
            } else if (displayName != null) {
                set(ref, snapshot.data<Profile>().copy(name = displayName))
            }
        }
    }

    fun observe(): Flow<FinanceData> {
        val categoryOrder = defaultCategories.mapIndexed { index, category -> category.id to index }.toMap()
        if(isLocal) return db.localSnapshots.map { snapshot ->
            val path="users/local"
            fun docs(name:String)=snapshot.collection("$path/$name").documents
            FinanceData(profile=snapshot.document(path).takeIf{it.exists}?.data<Profile>(),
                accounts=docs("accounts").map{it.data<Account>()}.sortedBy{it.createdAt},
                categories=docs("categories").map{it.data<Category>()}.sortedWith(compareBy<Category>{categoryOrder[it.id] ?: Int.MAX_VALUE}.thenBy{it.name.lowercase()}),
                transactions=docs("transactions").map{it.data<Transaction>()}.sortedByDescending{it.date},
                budgets=docs("budgets").map{it.data<Budget>()},goals=docs("goals").map{it.data<Goal>()},investments=docs("investments").map{it.data<Investment>()},
                bills=docs("bills").map{it.data<com.logikawan.finflow.core.model.Bill>()},debts=docs("debts").map{it.data<com.logikawan.finflow.core.model.Debt>()},
                wishes=docs("wishes").map{it.data<com.logikawan.finflow.core.model.Wish>()},cards=docs("cards").map{it.data<com.logikawan.finflow.core.model.WalletCard>()},
                notes=docs("notes").map{it.data<com.logikawan.finflow.core.model.FinanceNote>()},reimbursements=docs("reimbursements").map{it.data<com.logikawan.finflow.core.model.Reimbursement>()},
                recurring=docs("recurring").map{it.data<com.logikawan.finflow.core.model.RecurringTransaction>()},financialEvents=docs("financialEvents").map{it.data<com.logikawan.finflow.core.model.FinancialEvent>()},featuresReady=true)
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
        require(account.name.isNotBlank() && account.name.length <= 100) { "Nama dompet wajib diisi, maksimal 100 karakter" }
        require(account.currency.matches(Regex("[A-Z]{3}"))) { "Gunakan kode mata uang 3 huruf, misalnya IDR" }
        require(account.openingBalanceMinor in -MAX_MONEY..MAX_MONEY) { "Saldo di luar batas" }
        require(account.type in listOf("cash","bank","ewallet","credit","investment","loan","savings"))
        require(account.icon.length in 1..100 && account.color in 0xFF000000..0xFFFFFFFF)
        val ref=collection("accounts").document(account.id)
        db.runTransaction {
            val previous=get(ref)
            val parent=account.parentId?.let{require(it!=account.id);get(collection("accounts").document(it)).data<Account>()}
            require(parent==null || !parent.archived && parent.parentId==null && parent.currency==account.currency){"Pocket harus memakai dompet induk aktif, mata uang sama, dan tidak bertingkat"}
            if(previous.exists){
                val old=previous.data<Account>()
                require(old.currency==account.currency && old.createdAt==account.createdAt){"Mata uang dan tanggal pembuatan dompet tidak dapat diubah"}
                if(old.type=="savings")require(account.type==old.type && account.goalId==old.goalId && account.openingBalanceMinor==old.openingBalanceMinor && account.parentId==null){"Dompet target hanya diubah melalui menu Target"}
                else require(account.goalId==null && account.type!="savings")
            } else require(account.type!="savings" && account.goalId==null){"Buat dompet tabungan melalui menu Target"}
            set(ref,account)
        }
    }
    suspend fun saveCategory(category: Category) {
        require(category.name.isNotBlank() && category.name.length <= 100 && category.type in listOf("income", "expense"))
        require(category.icon.length<=100 && (category.color==null || category.color in 0xFF000000..0xFFFFFFFF))
        db.runTransaction {
            val previous=get(collection("categories").document(category.id))
            val parent=category.parentId?.let{require(it!=category.id);get(collection("categories").document(it)).data<Category>()}
            require(parent==null || parent.type==category.type && parent.parentId==null){"Pilih kategori induk dengan jenis yang sama"}
            if(previous.exists){val old=previous.data<Category>();require(old.type==category.type && old.parentId==category.parentId){"Jenis dan induk kategori yang sudah tersimpan tidak dapat diubah"}}
            set(collection("categories").document(category.id),category)
        }
    }

    suspend fun saveTransaction(tx: Transaction) {
        validateTransaction(tx)
        require(tx.source !in managedTransactionSources) { "Transaksi ini dikelola oleh fitur asalnya. Batalkan pembayaran untuk memperbaiki pencatatan." }
        val accounts = collection("accounts")
        val ref = collection("transactions").document(tx.id)
        db.runTransaction {
            val previous = get(ref)
            val source = get(accounts.document(tx.accountId)).data<Account>()
            val target = tx.toAccountId?.let { get(accounts.document(it)).data<Account>() }
            if(tx.type!="transfer")require(get(collection("categories").document(tx.categoryId)).data<Category>().type==tx.type){"Kategori tidak sesuai dengan jenis transaksi"}
            tx.subcategoryId?.let { id -> val sub=get(collection("categories").document(id)).data<Category>();require(sub.parentId==tx.categoryId && sub.type==tx.type){"Subkategori tidak sesuai"} }
            if (previous.exists) {
                val claim = get(collection("reimbursements").document(tx.id))
                if (claim.exists) {
                    val old = previous.data<Transaction>()
                    require(tx.type == old.type && tx.amountMinor == old.amountMinor && tx.currency == old.currency && tx.accountId == old.accountId && !tx.scheduled) {
                        "Hapus klaim reimburse sebelum mengubah nominal atau dompet pengeluaran asal"
                    }
                }
            }
            require(!source.archived && source.currency == tx.currency) { "Dompet tidak aktif atau mata uang berbeda" }
            require(source.type!="savings" && target?.type!="savings"){"Gunakan menu Setor / Tarik pada target tabungan"}
            if (target != null) require(!target.archived) { "Dompet tujuan tidak aktif" }
            set(ref, tx.copy(updatedAt = nowMillis()))
        }
    }
    suspend fun deleteTransaction(tx: Transaction) {
        if (tx.source in managedTransactionSources) planner.reverseEvent(tx.id)
        else db.runTransaction {
            require(!get(collection("reimbursements").document(tx.id)).exists) { "Hapus klaim reimburse sebelum menghapus pengeluaran asal" }
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
                require(!account.archived && account.currency == tx.currency) { "Pilih dompet aktif dengan mata uang yang sesuai" }
                if (tx.toAccountId != null) {
                    require(!get(base.collection("accounts").document(tx.toAccountId)).data<Account>().archived) { "Dompet tujuan tidak aktif" }
                }
                set(ref, tx.copy(updatedAt = nowMillis()))
                true
            }
        }
    }

    suspend fun saveBudget(budget: Budget) {
        require(budget.limitMinor in 1..MAX_MONEY && budget.title.length<=100 && budget.period in listOf("monthly","weekly")) { "Batas dan periode anggaran tidak valid" }
        if(budget.period=="monthly") {require(budget.month.matches(Regex("\\d{4}-\\d{2}")));LocalDate.parse("${budget.month}-01")} else LocalDate.parse(budget.month)
        require(collection("categories").document(budget.categoryId).get().data<Category>().type=="expense"){"Pilih kategori pengeluaran"}
        collection("budgets").document(budget.id).set(budget)
    }
    suspend fun deleteBudget(id: String) { collection("budgets").document(id).delete() }
    suspend fun saveGoal(goal: Goal) {
        require(goal.name.isNotBlank() && goal.name.length <= 100 && goal.targetMinor in 1..MAX_MONEY && goal.savedMinor in 0..MAX_MONEY) { "Target atau tabungan tidak valid; nama maksimal 100 karakter" }
        require(goal.currency.matches(Regex("[A-Z]{3}"))) { "Kode mata uang tidak valid" }
        if(goal.targetDate.isNotBlank())LocalDate.parse(goal.targetDate)
        require(goal.bucketType in listOf("standard","emergency","locked") && goal.protection in listOf("flexible","soft","hard"))
        val ref=collection("goals").document(goal.id)
        db.runTransaction {
            val snapshot=get(ref)
            if(snapshot.exists) {
                val old=snapshot.data<Goal>()
                require(old.revision==goal.revision){"Target berubah. Tutup dan buka ulang formulir."}
                require(goal.savedMinor==old.savedMinor && goal.holdingAccountId==old.holdingAccountId && goal.legacySavedMinor==old.legacySavedMinor && goal.lastEventId==old.lastEventId){"Gunakan Setor / Tarik untuk mengubah dana target"}
                require(goal.currency==old.currency){"Mata uang target tidak dapat diubah"}
                require(!goal.archived || goal.holdingAccountId==null || goal.savedMinor==goal.legacySavedMinor){"Tarik dana target sebelum mengarsipkannya"}
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
            require(goal.holdingAccountId==null || goal.savedMinor==goal.legacySavedMinor){"Tarik seluruh dana dompet target sebelum mengarsipkannya"}
            if(holding!=null){set(collection("accounts").document(holding.id),holding.copy(archived=true));set(ref,goal.copy(archived=true,revision=goal.revision+1))}
            else delete(ref)
        }
    }
    suspend fun saveInvestment(investment: Investment) {
        require(investment.name.isNotBlank() && investment.name.length <= 100 && investment.platform.length <= 100 && investment.notes.length <= 4000 && investment.investedMinor in 1..MAX_MONEY && investment.currentMinor in 0..MAX_MONEY && investment.dividendsMinor in 0..MAX_MONEY) { "Nilai investasi atau panjang nama/catatan tidak valid" }
        require(investment.currency.matches(Regex("[A-Z]{3}"))) { "Kode mata uang tidak valid" }
        collection("investments").document(investment.id).set(investment)
    }
    suspend fun deleteInvestment(id: String) { collection("investments").document(id).delete() }

    suspend fun importCsv(plan: ImportPlan, progress: (Int, Int) -> Unit): Int {
        require(plan.errors.isEmpty() && plan.transactions.isNotEmpty()) { "Perbaiki seluruh kesalahan CSV sebelum impor" }
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
        const val MAX_MONEY = 9_000_000_000_000_000L
        fun validateTransaction(tx: Transaction) {
            require(tx.type in listOf("income", "expense", "transfer") && tx.amountMinor in 1..MAX_MONEY) { "Jenis atau nominal transaksi tidak valid" }
            require(tx.accountId.isNotBlank() && tx.categoryId.isNotBlank() && tx.title.length <= 300 && tx.description.length <= 4000)
            require(tx.currency.matches(Regex("[A-Z]{3}"))) { "Kode mata uang tidak valid" }
            require(tx.date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) { "Tanggal harus berformat YYYY-MM-DD" }
            LocalDate.parse(tx.date)
            require(tx.tags.size<=10 && tx.tags.distinct().size==tx.tags.size && tx.tags.all{it.isNotBlank() && it.length<=30}){"Maksimal 10 tag unik, masing-masing 30 karakter"}
            require(tx.subcategoryId==null || tx.subcategoryId.isNotBlank() && tx.subcategoryId.length<=1000)
            if (tx.type == "transfer") require(!tx.toAccountId.isNullOrBlank() && tx.toAccountId != tx.accountId && tx.receivedAmountMinor != null && tx.receivedAmountMinor in 1..MAX_MONEY) { "Transfer memerlukan dompet berbeda dan nominal diterima" }
            else require(tx.toAccountId == null && tx.receivedAmountMinor == null)
        }
    }
}
