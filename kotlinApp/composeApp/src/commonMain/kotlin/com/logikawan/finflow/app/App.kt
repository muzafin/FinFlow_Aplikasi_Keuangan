package com.logikawan.finflow.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.logikawan.finflow.app.navigation.Editor
import com.logikawan.finflow.app.navigation.EntityEditor
import com.logikawan.finflow.app.navigation.MainNavigation
import com.logikawan.finflow.app.navigation.mainPages
import com.logikawan.finflow.app.navigation.aiPages
import com.logikawan.finflow.core.data.FinanceRepository
import com.logikawan.finflow.core.model.FinanceData
import com.logikawan.finflow.core.model.PreferenceState
import com.logikawan.finflow.core.model.LocalPreferenceState
import com.logikawan.finflow.core.model.LocalPreferences
import com.logikawan.finflow.core.model.ImportPlan
import com.logikawan.finflow.core.platform.AppLifecycle
import com.logikawan.finflow.core.platform.NativeServices
import com.logikawan.finflow.core.ui.FFColors
import com.logikawan.finflow.core.ui.FFIcon
import com.logikawan.finflow.core.ui.FFIconButton
import com.logikawan.finflow.core.ui.FFType
import com.logikawan.finflow.core.ui.FinFlowTheme
import com.logikawan.finflow.core.ui.designBackground
import com.logikawan.finflow.feature.auth.SplashScreen
import com.logikawan.finflow.core.util.today
import com.logikawan.finflow.feature.account.WalletScreen
import com.logikawan.finflow.feature.account.WalletDetail
import com.logikawan.finflow.feature.settings.PreferenceScreen
import com.logikawan.finflow.feature.settings.AccountDataScreen
import com.logikawan.finflow.feature.settings.DeleteAccountDialog
import com.logikawan.finflow.feature.settings.accountDataPages
import com.logikawan.finflow.feature.ai.AiFeature
import com.logikawan.finflow.feature.ai.aiAccountSection
import com.logikawan.finflow.core.model.BackupCodec
import com.logikawan.finflow.core.model.FinanceBackup
import com.logikawan.finflow.feature.settings.preferencePages
import com.logikawan.finflow.core.util.activeRecordingMonth
import com.logikawan.finflow.feature.auth.AuthScreen
import com.logikawan.finflow.feature.auth.OnboardingScreen
import com.logikawan.finflow.feature.auth.OlloSetupFlow
import com.logikawan.finflow.feature.budget.BudgetScreen
import com.logikawan.finflow.feature.budget.budgetAlerts
import com.logikawan.finflow.feature.connections.BankConnectionScreen
import com.logikawan.finflow.feature.dashboard.Dashboard
import com.logikawan.finflow.feature.goal.GoalScreen
import com.logikawan.finflow.feature.goal.GoalDetail
import com.logikawan.finflow.feature.importdata.CsvImport
import com.logikawan.finflow.feature.importdata.ImportScreen
import com.logikawan.finflow.feature.importdata.NotificationImportScreen
import com.logikawan.finflow.feature.investment.InvestmentDetail
import com.logikawan.finflow.feature.investment.InvestmentScreen
import com.logikawan.finflow.feature.report.Reports
import com.logikawan.finflow.feature.settings.NotificationSettings
import com.logikawan.finflow.feature.settings.ProfileScreen
import com.logikawan.finflow.feature.settings.Settings
import com.logikawan.finflow.feature.transaction.Transactions
import com.logikawan.finflow.feature.transaction.TransactionDetail
import com.logikawan.finflow.feature.transaction.ScheduledScreen
import com.logikawan.finflow.feature.transaction.AddButtonIntroSheet
import com.logikawan.finflow.feature.transaction.RecordMethodsSheet
import com.logikawan.finflow.feature.planning.*
import com.logikawan.finflow.core.model.managedTransactionSources
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop

@Composable
fun App(native: NativeServices, lifecycle: AppLifecycle = remember { AppLifecycle() },cloudRepository:FinanceRepository?=null,previewFirstRun:Boolean=false) {
    val repository = remember { cloudRepository ?: FinanceRepository() }
    val localRepository = remember { lazy{FinanceRepository(local=native.localFinancePersistence())} }
    val authScope = rememberCoroutineScope()
    val user by repository.authChanges.collectAsState(initial = repository.currentUser)
    var localMode by remember { mutableStateOf(native.readSetting("data.mode")=="local") }
    var connecting by rememberSaveable { mutableStateOf(false) }
    var incomingBackup by remember { mutableStateOf<Pair<FinanceBackup,String>?>(null) }
    var localFailure by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit){if(native.readSetting("local.sync.checkpoint").isNullOrBlank())native.writeSetting("local.sync.frozen","")}
    LaunchedEffect(user?.uid,localMode) { native.setNotificationImportUser(if(localMode)"local" else user?.uid) }
    var tourDone by remember { mutableStateOf(native.onboardingComplete()) }
    var cloudAuth by rememberSaveable { mutableStateOf(false) }
    var googleBusy by remember { mutableStateOf(false) }
    var googleError by remember { mutableStateOf<String?>(null) }
    var preferenceError by remember(localMode, user?.uid) { mutableStateOf<String?>(null) }
    var preferenceRetry by remember { mutableIntStateOf(0) }
    var askAddButton by remember { mutableStateOf(false) }
    var previewTour by remember { mutableStateOf(previewFirstRun) }
    var previewSetup by remember { mutableStateOf(false) }
    var previewFinished by remember { mutableStateOf(false) }
    var locked by remember { mutableStateOf(native.lockEnabled()) }
    var starting by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(800); starting = false }
    DisposableEffect(lifecycle) {
        lifecycle.onBackground = { if (native.lockEnabled()) locked = true }
        onDispose { lifecycle.onBackground = null }
    }
    val preferenceOwner=if(localMode)"local" else user?.uid
    val preferences = remember(preferenceOwner) { PreferenceState(native, preferenceOwner) }
    val setupState = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    LaunchedEffect(preferences, localMode, user?.uid, preferenceRetry) {
        if (!localMode && user != null) {
            preferenceError = null
            try { repository.synchronizePreferences(preferences) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { preferenceError = e.message ?: "Preferensi cloud belum tersinkron" }
        }
    }
    fun googleSignIn() {
        if (googleBusy) return
        googleBusy = true
        googleError = null
        native.googleSignIn { token, failure ->
            if (token == null) {
                googleBusy = false
                googleError = failure ?: "Login Google dibatalkan"
            } else authScope.launch {
                try { repository.googleLogin(token); native.setOnboardingComplete(); tourDone = true }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) { googleError = e.message ?: "Login Google belum berhasil" }
                finally { googleBusy = false }
            }
        }
    }
    suspend fun startLocal(name:String,currency:String,wallet:String,balance:Long) {
        localRepository.value.initializeProfile(name,currency,wallet,balance)
        native.writeSetting("data.mode","local");native.writeSetting("local.name",name)
        localMode=true;connecting=false
    }
    suspend fun restoreLocal(backup:FinanceBackup) {
        localRepository.value.initializeProfile()
        incomingBackup=backup to native.sha256(BackupCodec.encode(backup))
        native.writeSetting("data.mode","local");localMode=true;connecting=false
    }
    fun useCloud(){native.writeSetting("data.mode","cloud");localMode=false;connecting=false}
    CompositionLocalProvider(LocalPreferenceState provides preferences, LocalPreferences provides preferences.value) {
    FinFlowTheme {
        Surface(Modifier.fillMaxSize(), color = FFColors.background) {
            when {
                starting -> SplashScreen()
                locked && (user != null || localMode) -> Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center) {
                    Text("FinFlow terkunci", style = MaterialTheme.typography.headlineMedium)
                    Text("Buka dengan biometrik atau kunci perangkat.")
                    Button(onClick = { native.unlock { if (it) locked = false } }) { Text("Buka FinFlow") }
                }
                connecting && user!=null -> com.logikawan.finflow.feature.settings.CloudSyncScreen(localRepository.value,repository,native,preferences.value,::useCloud,{connecting=false},lifecycle)
                connecting -> AuthScreen(repository,native,authScope,lifecycle,::startLocal,::restoreLocal,cancel={connecting=false})
                previewTour -> OnboardingScreen { previewTour = false; previewSetup = true }
                previewSetup -> OlloSetupFlow(native, lifecycle, busy=false,
                    onComplete = { previewSetup = false; previewFinished = true },
                    onSignIn = { cloudAuth = true },
                    onRestore = {},
                )
                previewFinished -> Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center) {
                    Text("Pratinjau pengaturan awal selesai", style = FFType.appTitle)
                    Spacer(Modifier.height(10.dp))
                    Text("Tour dan wizard sudah ditampilkan tanpa menyimpan profil, dompet, preferensi atau foto.", style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { native.closeApp() }, modifier = Modifier.fillMaxWidth()) { Text("Tutup Pratinjau") }
                }
                localMode -> key("local") { FinanceScreen(localRepository.value,native,"Pengguna",lifecycle,onConnect={connecting=true},initialRestore=incomingBackup,onRestoreOpened={incomingBackup=null},askAddButton=askAddButton,onAskAddButtonShown={askAddButton=false}) }
                user != null -> key(user!!.uid) { FinanceScreen(repository, native, user!!.displayName ?: "Pengguna", lifecycle,onLocal={authScope.launch{
                    try {val profile=localRepository.value.observe().first().profile;startLocal(profile?.name ?: "Pengguna",profile?.baseCurrency ?: "IDR","Kas",0)}
                    catch(e:CancellationException){throw e}catch(e:Exception){localFailure=e.message}
                }}) }
                cloudAuth -> AuthScreen(repository, native, authScope, lifecycle,::startLocal,::restoreLocal,cancel={cloudAuth=false},initialScreen="login")
                !tourDone -> OnboardingScreen { native.setOnboardingComplete(); tourDone = true }
                else -> setupState.SaveableStateProvider("setup") { OlloSetupFlow(native, lifecycle, busy=googleBusy,
                    onComplete = { result ->
                        startLocal(result.name.trim().ifBlank { "Pengguna" }, result.currency, result.walletName, result.openingBalanceMinor)
                        PreferenceState(native, "local").update(result.preferences.copy(language = result.language))
                        result.photoBytes?.let { bytes -> runCatching { localRepository.value.saveProfilePhoto(native, bytes) } }
                        askAddButton = true
                    },
                    onSignIn = ::googleSignIn,
                    onEmailSignIn = { cloudAuth = true },
                    onRestore = { backup -> restoreLocal(backup) },
                ) }
            }
        }
        localFailure?.let{AlertDialog(onDismissRequest={localFailure=null},title={Text("Mode Lokal Belum Dapat Dibuka")},text={Text(it)},confirmButton={TextButton(onClick={localFailure=null}){Text("Tutup")}})}
        googleError?.let { message -> AlertDialog(onDismissRequest = { googleError = null }, title = { Text("Login Google") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { googleError = null }) { Text("Tutup") } }) }
        preferenceError?.let { message -> AlertDialog(onDismissRequest = { preferenceError = null },
            title = { Text("Preferensi Belum Tersinkron") },
            text = { Text("Perubahan di perangkat tetap disimpan. $message") },
            confirmButton = { TextButton(onClick = { preferenceError = null; preferenceRetry++ }) { Text("Coba Lagi") } },
            dismissButton = { TextButton(onClick = { preferenceError = null }) { Text("Nanti") } }) }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinanceScreen(repo: FinanceRepository, native: NativeServices, name: String, lifecycle: AppLifecycle,onConnect:()->Unit={},onLocal:()->Unit={},initialRestore:Pair<FinanceBackup,String>?=null,onRestoreOpened:()->Unit={},askAddButton:Boolean=false,onAskAddButtonShown:()->Unit={}) {
    val scope = rememberCoroutineScope()
    val navigationBackdrop = rememberLayerBackdrop()
    var data by remember { mutableStateOf(FinanceData()) }
    var ready by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var deleteAccount by remember { mutableStateOf(false) }
    var resetData by remember { mutableStateOf(false) }
    var page by rememberSaveable { mutableStateOf("Beranda") }
    var previousPage by rememberSaveable { mutableStateOf("Beranda") }
    var selectedInvestmentId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedAccountId by rememberSaveable { mutableStateOf<String?>(null) }
    var accountParent by rememberSaveable { mutableStateOf("Dompet") }
    var goalParent by rememberSaveable { mutableStateOf("Target") }
    var transactionFilter by rememberSaveable { mutableStateOf("all") }
    var selectedGoalId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedTransactionId by rememberSaveable { mutableStateOf<String?>(null) }
    var transactionParent by rememberSaveable { mutableStateOf("Beranda") }
    var month by rememberSaveable { mutableStateOf(today().toString().take(7)) }
    var editor by remember { mutableStateOf<Editor?>(null) }
    var deletion by remember { mutableStateOf<Pair<String, suspend () -> Unit>?>(null) }
    var plan by remember { mutableStateOf<ImportPlan?>(null) }
    var importSource by rememberSaveable { mutableStateOf("ivy") }
    var importProgress by remember { mutableStateOf("") }
    var restorePreview by remember { mutableStateOf<Pair<FinanceBackup,String>?>(null) }
    val restoreKey="restore.${requireNotNull(repo.ownerId)}"
    var restorePending by remember{mutableStateOf(native.readSetting(restoreKey)?.takeIf{it.isNotBlank()})}
    val appearance=LocalPreferences.current
    val preferenceStore=LocalPreferenceState.current
    var recordSheet by remember { mutableStateOf(false) }
    val ai = com.logikawan.finflow.feature.ai.rememberAiController(native, if (repo.isLocal) null else repo.ownerId)
    var aiChat by remember { mutableStateOf(false) }
    var aiScan by remember { mutableStateOf(false) }
    var premiumGate by remember { mutableStateOf<String?>(null) }
    var aiParent by rememberSaveable { mutableStateOf("Beranda") }
    val speechLanguage = if (appearance.language == "en") "en-US" else "id-ID"
    var walletDetailMenu by remember { mutableStateOf(false) }
    var addIntro by remember { mutableStateOf(false) }
    LaunchedEffect(askAddButton) { if (askAddButton) { addIntro = true; onAskAddButtonShown() } }
    val syncPending=repo.isLocal && !native.readSetting("local.sync.checkpoint").isNullOrBlank()
    val resetPending=!native.readSetting("reset.${repo.ownerId}").isNullOrBlank()
    val dataPaused=restorePending!=null || syncPending || resetPending
    LaunchedEffect(initialRestore){if(initialRestore!=null){restorePreview=initialRestore;onRestoreOpened()}}
    LaunchedEffect(appearance.recordingDay){month=activeRecordingMonth(today(),appearance.recordingDay)}
    LaunchedEffect(retry) {
        ready = false
        error = null
        try {
            repo.initializeProfile()
            repo.observe().collect { data = it; ready = true }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message ?: "Data gagal dimuat" }
    }
    fun action(closeEditor: Boolean = false, block: suspend () -> Unit) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try { block(); if (closeEditor) editor = null }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "Penyimpanan gagal. Silakan ulangi." }
            finally { busy = false }
        }
    }
    LaunchedEffect(ready, data.featuresReady, dataPaused) {
        if (ready && data.featuresReady && !dataPaused) {
            try { repo.planner.processDueRecurring() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { notice = "Jadwal berulang belum diproses: ${e.message}. Coba lagi melalui menu Berulang." }
        }
    }
    val savedPages = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    val displayName = data.profile?.name ?: name
    fun openTransaction(tx: com.logikawan.finflow.core.model.Transaction) {
        selectedTransactionId = tx.id
        transactionParent = page
        page = "Detail transaksi"
    }
    LaunchedEffect(data, page,appearance.recordingDay) {
        val uid = repo.ownerId
        if (ready && !dataPaused && uid != null && native.budgetAlertsEnabled()) {
            budgetAlerts(data, activeRecordingMonth(today(),appearance.recordingDay),appearance.recordingDay).forEach { alert ->
                native.showBudgetAlert(uid, alert.id, alert.title, alert.message)
            }
        }
    }
    fun navigate(destination: String) {
        if(destination.startsWith("Dompet:")){
            selectedAccountId=destination.substringAfter(':');previousPage=page
            val goal=data.goals.find{it.holdingAccountId==selectedAccountId}
            if(goal!=null){goalParent=page;selectedGoalId=goal.id;page="Detail impian"}else {accountParent=page;page="Detail dompet"}
            return
        }
        if(destination.startsWith("Transaksi:")){transactionFilter=destination.substringAfter(':');previousPage=page;page="Transaksi";return}
        if(destination=="Transaksi")transactionFilter="all"
        when (destination) {
            "Hapus Data" -> resetData=true
            "Bagikan" -> { native.shareApp()?.let{error=it} }
            "Tambah transaksi" -> editor = Editor.Tx()
            "Profil akun" -> editor = Editor.Person
            "Berulang" -> { previousPage = page; page = "Terjadwal" }
            "Tagihan", "Utang", "Keinginan", "Kartu", "Catatan", "Reimburse" -> { previousPage = page; page = destination }
            in aiPages -> { if (page !in aiPages) aiParent = page; page = destination }
            "Ubah password" -> action {
                val email = repo.currentUser?.email
                require(!email.isNullOrBlank()) { "Email akun belum tersedia" }
                repo.resetPassword(email)
                notice = "Tautan pemulihan password dikirim ke email akunmu. Periksa kotak masuk."
            }
            else -> { if(destination=="Detail impian")goalParent=page;if (destination !in mainPages) previousPage = page; page = destination }
        }
    }
    fun chooseBackup(){native.pickCsv{content,failure->if(content!=null)try{restorePreview=BackupCodec.decode(content) to native.sha256(content)}catch(e:Exception){error=e.message}else if(failure!=null)error=failure}}
    fun back() { page = when (page) { "Pengaturan Asisten" -> "Asisten"; "Asisten", "Premium", "Pengaturan AI Scan" -> aiParent; "Masukan & Peta Jalan" -> "Profil"; "Masukan" -> previousPage.takeIf { it == "Masukan & Peta Jalan" || it in mainPages } ?: "Profil"; "Detail dompet" -> accountParent; "Transaksi terjadwal" -> "Terjadwal"; "Detail investasi" -> "Investasi"; "Detail impian" -> goalParent; "Detail transaksi" -> transactionParent; else -> previousPage.takeIf { it in mainPages } ?: "Beranda" } }
    val planningActions = PlanningActions(
        bill = { editor = Editor.BillForm(it) }, payBill = { editor = Editor.PayBill(it) },
        debt = { editor = Editor.DebtForm(it) }, payDebt = { editor = Editor.PayDebt(it) },
        wish = { editor = Editor.WishForm(it) }, fulfillWish = { action { repo.planner.saveWish(it) } },
        card = { editor = Editor.CardForm(it) }, note = { editor = Editor.NoteForm(it) },
        claim = { editor = Editor.ClaimForm(it) }, receiveClaim = { editor = Editor.ReceiveClaim(it) },
        recurring = { editor = Editor.RepeatForm(it) }, processRecurring = { action { val count = repo.planner.processDueRecurring(); notice = "$count transaksi berulang dicatat" } },
        manualScheduled = { navigate("Transaksi terjadwal") }, addExpense = { editor = Editor.Tx() },
        reverse = { event -> deletion = "batal:pembayaran" to { repo.planner.reverseEvent(event.id) } },
        payBundle = {note,items->editor=Editor.PayBundle(note,items)},
        updateNote = {note->action{repo.planner.saveNote(note)}},
    )
    DisposableEffect(lifecycle, page) {
        lifecycle.onBack = { if (page !in mainPages) { back(); true } else false }
        onDispose { lifecycle.onBack = null }
    }
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (!dataPaused && page !in mainPages && page !in listOf("Transaksi", "Detail investasi", "Aktivitas") && page !in aiPages) CenterAlignedTopAppBar(
                title = { Text(when(page) {
                    "Masukan & Peta Jalan" -> "Peta Jalan Produk"; "Anggaran" -> "Anggaran"; "Target" -> "Target Tabungan"
                    "Terjadwal" -> "Berulang"; "Aktivitas" -> "Aktivitas"
                    "Detail impian" -> "Detail Impian"; "Detail transaksi" -> "Detail Transaksi"; "Detail dompet" -> data.accounts.find { it.id == selectedAccountId }?.name ?: "Detail Dompet"
                    "Notifikasi" -> "Pengaturan Notifikasi"; else -> page
                }, style = FFType.appTitle) },
                navigationIcon = { FFIconButton("arrow_back_rounded", ::back) },
                actions = {
                    when (page) {
                        "Investasi" -> FFIconButton("add_rounded", {
                            editor = when(page) { "Anggaran" -> Editor.Limit(); "Target" -> Editor.Target(); else -> Editor.Asset() }
                        })
                        "Terjadwal" -> FFIconButton("add_rounded", { editor = Editor.RepeatForm(null) })
                        "Detail dompet" -> data.accounts.find { it.id == selectedAccountId }?.let { account -> Box {
                            IconButton({ walletDetailMenu = true }, Modifier.testTag("wallet-detail-menu")) { FFIcon("more_vert", contentDescription = "Menu dompet") }
                            DropdownMenu(walletDetailMenu, { walletDetailMenu = false }) {
                                DropdownMenuItem(text = { Text("Sesuaikan Saldo") }, onClick = { walletDetailMenu = false; editor = Editor.AdjustWallet(account) })
                                DropdownMenuItem(text = { Text("Edit / Arsipkan Dompet") }, onClick = { walletDetailMenu = false; editor = Editor.Wallet(account) })
                            }
                        } }
                    }
                },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = FFColors.background),
            )
        },
        bottomBar = { if (!dataPaused && page in mainPages) MainNavigation(page, ::navigate, {
            if (appearance.addButtonMode == "all") recordSheet = true else editor = Editor.Tx()
        }, ready && !busy, navigationBackdrop) },
    ) { padding ->
        Column(Modifier.fillMaxSize().layerBackdrop(navigationBackdrop).background(designBackground).padding(top = padding.calculateTopPadding())
            .then(if (page in mainPages && page != "Profil" || page == "Transaksi") Modifier.statusBarsPadding() else Modifier)
            .then(if (page !in mainPages && page != "Detail investasi" && page !in aiPages) Modifier.navigationBarsPadding() else Modifier)) {
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { msg -> Card(Modifier.fillMaxWidth().padding(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)) {
                Text(msg, Modifier.padding(12.dp))
                if (!ready) TextButton(onClick = { retry++ }) { Text("Muat ulang") }
                else TextButton(onClick = { error = null }) { Text("Tutup") }
            } }
            notice?.let { TextButton(onClick = { notice = null }) { Text(it) } }
            if(syncPending){
                Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp,Alignment.CenterVertically)){
                    Text("Sinkronisasi Belum Selesai",style=FFType.appTitle)
                    Text("Data lokal tetap tersimpan. Pencatatan dijeda agar salinan yang sedang dipindahkan tidak berubah.")
                    Button(onClick=onConnect,modifier=Modifier.fillMaxWidth()){Text("Lanjutkan Sinkronisasi Akun")}
                    if(ready)OutlinedButton(onClick={native.shareTextFile("finflow-lokal-${today()}.json",BackupCodec.encode(FinanceBackup(ownerId="local",data=data,preferences=appearance)),"application/json")}){Text("Simpan Cadangan Lokal")}
                }
            }else if(resetPending){
                Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp,Alignment.CenterVertically)) {
                    Text("Penghapusan Data Belum Selesai",style=FFType.appTitle)
                    Text("Akun tetap ada. Pencatatan dijeda sampai pembersihan selesai agar data tidak dibuat ulang di tengah proses.")
                    Button(enabled=!busy,onClick={resetData=true}){Text("Lanjutkan Penghapusan Data")}
                }
            }else if(restorePending!=null){
                Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),verticalArrangement=Arrangement.Center){
                    Text("Pemulihan Belum Selesai",style=FFType.appTitle)
                    Spacer(Modifier.height(16.dp));Text("Pilih berkas cadangan yang sama untuk melanjutkan. Pencatatan dan jadwal otomatis dijeda agar data tidak berubah selama pemulihan. Jangan mengubah akun tujuan dari perangkat lain.",style=FFType.bodyMain)
                    Spacer(Modifier.height(20.dp));Button(enabled=!busy,onClick=::chooseBackup,modifier=Modifier.fillMaxWidth()){Text("Lanjutkan Pemulihan JSON")}
                    TextButton(enabled=!busy,onClick={action{repo.logout()}}){Text("Keluar dari Akun")}
                }
            } else if (!ready) {
                Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (error == null) "Menyiapkan data akun…" else "Data akun gagal dimuat", style = FFType.sectionTitle)
                    if (error != null) {
                        Spacer(Modifier.height(12.dp))
                        Text("Akun ini belum dapat mengakses data. Coba muat ulang atau masuk dengan akun lain.", style = FFType.bodyMain)
                        TextButton(enabled = !busy, onClick = { action { repo.logout() } }) { Text("Keluar / Ganti Akun") }
                    }
                }
            } else savedPages.SaveableStateProvider(page) { when (page) {
                "Beranda" -> Dashboard(data, month, { month = it }, ::openTransaction, displayName, ::navigate)
                "Transaksi" -> Transactions(data, month, { month = it }, ::openTransaction, ::back, initialType=transactionFilter)
                "Dompet" -> WalletScreen(data, { editor = Editor.Wallet(it) }, { editor = Editor.Wallet() }, { navigate("Dompet:${it.id}") })
                "Detail dompet" -> data.accounts.find{it.id==selectedAccountId}?.let{account->WalletDetail(account,data,{editor=Editor.Tx(accountId=account.id,type=it)},::openTransaction)}
                in preferencePages -> PreferenceScreen(page,data)
                "Masukan & Peta Jalan" -> com.logikawan.finflow.feature.settings.RoadmapScreen { navigate("Masukan") }
                in accountDataPages -> AccountDataScreen(page,data,native,repo.currentUser?.email.orEmpty(),::navigate,{retry++},back=::back,
                    backup={native.shareTextFile("finflow-backup-${today()}.json",BackupCodec.encode(FinanceBackup(ownerId=requireNotNull(repo.ownerId),data=data,preferences=appearance)),"application/json")},
                    chooseRestore=::chooseBackup,busy=busy,changePhoto={
                        val uid=repo.ownerId
                        native.pickProfilePhoto { bytes,failure ->
                            if (failure!=null) error=failure
                            else if (bytes!=null && uid==repo.ownerId) action { repo.saveProfilePhoto(native,bytes);notice="Foto profil diperbarui" }
                        }
                    },deleteAccount={deleteAccount=true},localMode=repo.isLocal,connect=onConnect,useLocal=onLocal,
                    accountExtras={ aiAccountSection(ai, !repo.isLocal) { navigate("Premium") } })
                "Kategori" -> com.logikawan.finflow.feature.settings.CategoryScreen(data,busy,edit={editor=Editor.Cat(it)})
                "Profil" -> ProfileScreen(native, displayName, if(repo.isLocal)"Mode Lokal" else repo.currentUser?.email.orEmpty(), data, ::navigate, {
                    if(repo.isLocal){if(!native.closeApp())notice="Tutup FinFlow melalui pengalih aplikasi perangkat."}else action { repo.logout() }
                },localMode=repo.isLocal,premiumCard={ com.logikawan.finflow.feature.ai.UpgradePremiumCard(ai) { navigate("Premium") } })
                "Transaksi terjadwal" -> ScheduledScreen(data, month, { month = it }, ::openTransaction, { editor = Editor.Tx(scheduled = true) })
                "Statistik" -> Reports(data, month, { month = it }, { native.shareCsv("finflow-$month.csv", CsvImport.export(data, data.transactions.filter { it.date.startsWith(month) })) }, { navigate("Transaksi") }, ::openTransaction,
                    exportFiltered={transactions->native.shareCsv("finflow-statistik.csv",CsvImport.export(data,transactions))},
                    onSectionOrder = { navigate("Urutan Statistik") }, onPreferences = { navigate("Tema") })
                "Detail transaksi" -> data.transactions.find { it.id == selectedTransactionId }?.let { tx ->
                    TransactionDetail(tx, data, {
                        if (tx.source in managedTransactionSources) navigate(when(tx.source) {
                            "bill" -> "Tagihan"; "debt_initial", "debt_payment" -> "Utang"; "reimbursement" -> "Reimburse"; "bundle"->"Catatan"; "goal_deposit","goal_withdrawal"->"Target"; else -> "Terjadwal"
                        }) else editor = Editor.Tx(tx)
                    }, {
                        deletion = (if(tx.source in managedTransactionSources) "batal:pencatatan" else "transaksi") to { repo.deleteTransaction(tx); page = transactionParent }
                    }, busy)
                } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Transaksi tidak ditemukan") }
                "Anggaran" -> BudgetScreen(data, month, { month = it }, { editor = Editor.Limit(it) }, { editor = Editor.Limit() })
                "Target" -> GoalScreen(data, { selectedGoalId = it.id; navigate("Detail impian") }, { editor = Editor.Target() })
                "Detail impian" -> data.goals.find { it.id == selectedGoalId }?.let { goal -> GoalDetail(goal,edit={editor=Editor.Target(goal)},data=data,
                    deposit={editor=Editor.GoalMovement(goal,false)},withdraw={editor=Editor.GoalMovement(goal,true)},reverse=planningActions.reverse) }
                    ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Target tidak ditemukan") }
                "Investasi" -> InvestmentScreen(data, { selectedInvestmentId = it.id; navigate("Detail investasi") }, { editor = Editor.Asset() })
                "Detail investasi" -> data.investments.find { it.id == selectedInvestmentId }?.let { investment ->
                    InvestmentDetail(investment, ::back, { editor = Editor.Asset(investment) }, {
                        val remove: suspend () -> Unit = { repo.deleteInvestment(investment.id); page = "Investasi" }
                        deletion = "investasi" to remove
                    }, onUpdateValue = { value -> action { repo.saveInvestment(investment.copy(currentMinor = value)) } }, busy = busy)
                } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Investasi tidak ditemukan") }
                "Notifikasi" -> NotificationSettings(native)
                "Asisten" -> com.logikawan.finflow.feature.ai.AssistantScreen(ai, data, appearance.language, ::back, { navigate("Pengaturan Asisten") }, { navigate("Premium") })
                "Pengaturan Asisten" -> com.logikawan.finflow.feature.ai.AssistantSettingsScreen(ai, ::back)
                "Pengaturan AI Scan" -> com.logikawan.finflow.feature.ai.ScanSettingsScreen(ai, ::back)
                "Premium" -> com.logikawan.finflow.feature.ai.PremiumScreen(ai, native, ::back, onConnect)
                "Impor notifikasi" -> NotificationImportScreen(data, { draft, completed ->
                    editor = Editor.Tx(draft, notificationImport = true, onImported = completed)
                }, { navigate("Impor data") })
                "Impor data" -> ImportScreen(plan, importSource, { importSource = it; plan = null }, busy, importProgress, onPick = {
                    native.pickCsv { content, failure ->
                        if (content == null) error = failure
                        else try { plan = CsvImport.preview(content, importSource, data) } catch (e: Exception) { error = e.message }
                    }
                }, onTemplate = { native.shareCsv("finflow-template.csv", CsvImport.template) }, onImport = {
                    plan?.let { preview -> action {
                        val added = repo.importCsv(preview) { done, total -> importProgress = "$done / $total" }
                        notice = "$added transaksi ditambahkan. Transaksi yang sudah ada dilewati."
                        plan = null
                        importProgress = ""
                    } }
                })
                "Koneksi bank" -> BankConnectionScreen({ navigate("Impor data") }, { navigate("Impor notifikasi") })
                "Pengaturan" -> Settings(native, repo.currentUser?.email ?: "Mode Lokal", data, { editor = Editor.Cat() }, {
                    if(repo.isLocal){if(!native.closeApp())notice="Tutup FinFlow melalui pengalih aplikasi perangkat."}else action { repo.logout() }
                })
                "Terjadwal" -> com.logikawan.finflow.feature.transaction.RecurringListScreen(
                    data = data, busy = busy,
                    onAdd = { editor = Editor.RepeatForm(null) },
                    onEdit = { editor = Editor.RepeatForm(it) },
                    onArchive = { value -> deletion = "arsip:jadwal berulang" to { repo.planner.archiveRecurring(value.id) } },
                    onProcess = planningActions.processRecurring,
                    onCalendar = planningActions.manualScheduled,
                )
                "Tagihan", "Utang", "Keinginan", "Kartu", "Catatan", "Reimburse" -> PlanningScreen(page, data, busy, planningActions)
            } }
        }
    }
    if (addIntro) AddButtonIntroSheet(appearance.addButtonMode, onChoose = { mode ->
        preferenceStore?.update(appearance.copy(addButtonMode = mode))
        addIntro = false
        if (mode == "all") recordSheet = true
    }, onDismiss = { addIntro = false })
    if (recordSheet) com.logikawan.finflow.feature.ai.QuickRecordBar(ai,
        onAssistant = { navigate("Asisten") },
        onVoice = { ai.requireConsent(AiFeature.Voice) {
            native.recognizeSpeech(speechLanguage) { text, failure ->
                if (text != null) ai.parse(AiFeature.Voice, text, data, appearance.defaultWalletId, appearance.language)
                else if (failure != null) ai.error = failure
            }
        } },
        onScan = { ai.requireConsent(AiFeature.Scan) { aiScan = true } },
        onChat = { ai.requireConsent(AiFeature.Chat) { aiChat = true } },
        onAdd = { editor = Editor.Tx() },
        onDismiss = { recordSheet = false },
    )
    if (aiChat) com.logikawan.finflow.feature.ai.AiChatSheet(ai, onSend = { text ->
        ai.parse(AiFeature.Chat, text, data, appearance.defaultWalletId, appearance.language) { aiChat = false }
    }, onDismiss = { aiChat = false })
    if (aiScan) com.logikawan.finflow.feature.ai.AiScanSheet(ai,
        onCamera = { native.captureReceiptPhoto { photo, failure ->
            if (photo != null) { aiScan = false; ai.scan(listOf(photo), data, appearance.defaultWalletId, appearance.language) } else if (failure != null) ai.error = failure
        } },
        onGallery = { native.pickReceiptPhotos(5) { photos, failure ->
            if (!photos.isNullOrEmpty()) { aiScan = false; ai.scan(photos, data, appearance.defaultWalletId, appearance.language) } else if (failure != null) ai.error = failure
        } },
        onSettings = { aiScan = false; navigate("Pengaturan AI Scan") },
        onDismiss = { aiScan = false })
    com.logikawan.finflow.feature.ai.AiDraftReviewSheet(ai, data, busy,
        onEdit = { tx -> editor = Editor.Tx(tx, aiDraft = true, onImported = { ai.drafts = ai.drafts.filterNot { it.id == tx.id } }) },
        onSaveAll = { drafts -> action {
            drafts.forEach { tx -> repo.saveTransaction(tx); ai.drafts = ai.drafts.filterNot { it.id == tx.id } }
            notice = "${drafts.size} transaksi dari AI disimpan"
        } })
    com.logikawan.finflow.feature.ai.AiConsentDialog(ai)
    com.logikawan.finflow.feature.ai.AiErrorDialog(ai) { navigate("Premium") }
    if (ai.working != null && !aiChat && !aiScan && page != "Asisten") AlertDialog(onDismissRequest = {}, confirmButton = {},
        text = { Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(12.dp)); Text(ai.working.orEmpty()) } })
    LaunchedEffect(editor, ai.isPremium) {
        com.logikawan.finflow.feature.ai.freeLimitReached(editor, data)?.takeIf { !ai.isPremium }?.let { premiumGate = it; editor = null }
    }
    premiumGate?.let { com.logikawan.finflow.feature.ai.PremiumGateDialog(it, { navigate("Premium") }, { premiumGate = null }) }
    editor?.let { current ->
        EntityEditor(current, data, busy, dismiss = { if (!busy) editor = null },
            saveTx = { transaction -> action(true) {
                if (current is Editor.Tx && current.aiDraft) {
                    repo.saveTransaction(transaction)
                    current.onImported?.invoke()
                    notice = "Transaksi dari AI disimpan"
                } else if (current is Editor.Tx && current.notificationImport) {
                    val added = repo.importNotification(transaction)
                    current.onImported?.invoke()
                    notice = if (added) "Transaksi notifikasi disimpan" else "Transaksi ini sudah diimpor sebelumnya"
                } else {
                    repo.saveTransaction(transaction)
                    if(appearance.rememberCategory && transaction.type!="transfer")preferenceStore?.update(if(transaction.type=="income")appearance.copy(lastIncomeCategory=transaction.categoryId) else appearance.copy(lastExpenseCategory=transaction.categoryId))
                }
            } },
            saveAccount = { action(true) { repo.saveAccount(it) } },
            saveBudget = { action(true) { repo.saveBudget(it) } },
            saveGoal = { action(true) { repo.saveGoal(it) } },
            saveInvestment = { action(true) { repo.saveInvestment(it) } },
            saveCategory = { action(true) { repo.saveCategory(it) } },
            saveProfile = { action(true) { repo.updateProfile(it.name, it.baseCurrency) } },
            delete = { label, block -> deletion = label to block }, repo = repo, savePlanning = { action(true, it) },
        )
    }
    restorePreview?.let{(backup,checksum)->AlertDialog(onDismissRequest={if(!busy)restorePreview=null},title={Text("Pulihkan cadangan lengkap?")},text={
        Text("${backup.data.profile?.name.orEmpty()}\n${backup.data.accounts.size} dompet · ${backup.data.transactions.size} transaksi\n\nPemulihan hanya untuk akun tujuan kosong. Riwayat pembayaran dan preferensi ikut dipulihkan. Data akun yang sudah berisi tidak ditimpa.")
    },confirmButton={TextButton(enabled=!busy,onClick={
        restorePreview=null
        action {
            val uid=requireNotNull(repo.ownerId)
            val jobKey="restore.$uid"
            val pending=native.readSetting(jobKey)
            require(pending==null || pending.isEmpty() || pending==checksum){"Ada pemulihan lain yang belum selesai. Pilih berkas cadangan sebelumnya."}
            val resuming=pending==checksum
            if(!resuming){
                require(data.transactions.isEmpty() && data.goals.isEmpty() && data.bills.isEmpty() && data.debts.isEmpty() && data.notes.isEmpty() && data.cards.isEmpty() && data.recurring.isEmpty() && data.investments.isEmpty() && data.budgets.isEmpty() && data.wishes.isEmpty() && data.reimbursements.isEmpty() && data.financialEvents.isEmpty()){"Akun tujuan sudah berisi data"}
            }
            repo.restoreBackup(backup,resuming,{native.writeSetting(jobKey,checksum);restorePending=checksum}){notice=it}
            val restored=BackupCodec.restoreData(backup)
            val wallet=backup.data.accounts.indexOfFirst{it.id==backup.preferences.defaultWalletId}
            preferenceStore?.update(backup.preferences.copy(defaultWalletId=if(wallet>=0)restored.accounts[wallet].id else ""))
            native.writeSetting(jobKey,"")
            restorePending=null
            notice="Cadangan lengkap dipulihkan. Data lama tidak ditimpa."
        }
    }){Text("Pulihkan")}},dismissButton={TextButton(enabled=!busy,onClick={restorePreview=null}){Text("Batal")}})}
    if (deleteAccount) DeleteAccountDialog(native,repo.currentUser?.providerData?.any{it.providerId=="google.com"}==true,busy,
        dismiss={deleteAccount=false},confirm={password,token->action{repo.requestAccountDeletion(password,token)}})
    if(resetData)DeleteAccountDialog(native,repo.currentUser?.providerData?.any{it.providerId=="google.com"}==true,busy,
        dismiss={resetData=false},confirm={password,token->action{
            if(repo.isLocal){repo.clearLocalData();notice=native.deleteLocalProfilePhoto() ?: "Data lokal dihapus. Profil tetap disimpan."}
            else {repo.resetCloudData(native,password,token);notice="Data cloud dihapus. Akun tetap tersimpan."}
            resetData=false;page="Beranda";retry++
        }},dataOnly=true,localMode=repo.isLocal)
    deletion?.let { (label, block) ->
        val verb = when { label.startsWith("arsip:") -> "Arsipkan"; label.startsWith("batal:") -> "Batalkan"; else -> "Hapus" }
        AlertDialog(onDismissRequest = { deletion = null }, title = { Text("$verb ${label.substringAfter(':')}?") },
        text = { Text(when(verb) { "Arsipkan" -> "Riwayat pembayaran tetap disimpan. Jadwal yang diarsipkan tidak diproses lagi."; "Batalkan" -> "Pencatatan ini dibatalkan dan saldo jurnal dikembalikan. Pembayaran tagihan harus dibatalkan mulai dari yang terbaru."; else -> "Data ini akan dihapus. Menghapus transaksi juga mengubah saldo hasil perhitungan jurnal." }) },
        confirmButton = { TextButton(enabled = !busy, onClick = { deletion = null; action(true, block) }) { Text(verb, color = FFColors.expense) } },
        dismissButton = { TextButton(onClick = { deletion = null }) { Text("Batal") } }) }
    if (editor != null && error != null) AlertDialog(
        onDismissRequest = { error = null }, title = { Text("Belum dapat menyimpan") },
        text = { Text(error.orEmpty()) },
        confirmButton = { TextButton(onClick = { error = null }) { Text("Kembali ke formulir") } },
    )
}
