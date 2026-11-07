package com.clonical.kasiva.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
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
import com.clonical.kasiva.app.navigation.Editor
import com.clonical.kasiva.core.i18n.withLocalizedDefaults
import com.clonical.kasiva.app.navigation.EntityEditor
import com.clonical.kasiva.app.navigation.MainNavigation
import com.clonical.kasiva.app.navigation.mainPages
import com.clonical.kasiva.app.navigation.aiPages
import com.clonical.kasiva.core.data.FinanceRepository
import com.clonical.kasiva.core.data.ConnectedDevice
import com.clonical.kasiva.core.ui.LocalWalletIcons
import com.clonical.kasiva.core.ui.WalletIconStore
import com.clonical.kasiva.core.ui.WALLET_ICON_MAX_BYTES
import com.clonical.kasiva.core.model.FinanceData
import com.clonical.kasiva.core.model.PreferenceState
import com.clonical.kasiva.core.model.LocalPreferenceState
import com.clonical.kasiva.core.model.LocalPreferences
import com.clonical.kasiva.core.model.ImportPlan
import com.clonical.kasiva.core.platform.AppLifecycle
import com.clonical.kasiva.core.platform.NativeServices
import com.clonical.kasiva.core.ui.FFColors
import com.clonical.kasiva.core.ui.LocalTopBarActions
import com.clonical.kasiva.core.model.financeMenus
import com.clonical.kasiva.core.ui.FFIcon
import com.clonical.kasiva.core.ui.FFIconButton
import com.clonical.kasiva.core.ui.FFType
import com.clonical.kasiva.core.ui.KasivaTheme
import com.clonical.kasiva.core.ui.designBackground
import com.clonical.kasiva.feature.auth.SplashScreen
import com.clonical.kasiva.core.util.today
import com.clonical.kasiva.feature.account.WalletScreen
import com.clonical.kasiva.feature.account.WalletDetail
import com.clonical.kasiva.feature.settings.PreferenceScreen
import com.clonical.kasiva.feature.settings.AccountDataScreen
import com.clonical.kasiva.feature.settings.DeleteAccountDialog
import com.clonical.kasiva.feature.settings.accountDataPages
import com.clonical.kasiva.feature.ai.AiFeature
import com.clonical.kasiva.feature.ai.aiAccountSection
import com.clonical.kasiva.core.model.BackupCodec
import com.clonical.kasiva.core.model.FinanceBackup
import com.clonical.kasiva.feature.settings.preferencePages
import com.clonical.kasiva.core.util.activeRecordingMonth
import com.clonical.kasiva.feature.auth.AuthScreen
import com.clonical.kasiva.feature.auth.OnboardingScreen
import com.clonical.kasiva.feature.auth.OlloSetupFlow
import com.clonical.kasiva.feature.budget.BudgetScreen
import com.clonical.kasiva.feature.budget.budgetAlerts
import com.clonical.kasiva.feature.connections.BankConnectionScreen
import com.clonical.kasiva.feature.dashboard.Dashboard
import com.clonical.kasiva.feature.goal.GoalScreen
import com.clonical.kasiva.feature.goal.GoalDetail
import com.clonical.kasiva.feature.importdata.CsvImport
import com.clonical.kasiva.feature.importdata.ImportScreen
import com.clonical.kasiva.feature.importdata.NotificationImportScreen
import com.clonical.kasiva.feature.investment.InvestmentDetail
import com.clonical.kasiva.feature.investment.InvestmentScreen
import com.clonical.kasiva.feature.report.Reports
import com.clonical.kasiva.feature.settings.NotificationSettings
import com.clonical.kasiva.feature.settings.ProfileScreen
import com.clonical.kasiva.feature.settings.Settings
import com.clonical.kasiva.feature.transaction.Transactions
import com.clonical.kasiva.feature.transaction.TransactionDetail
import com.clonical.kasiva.feature.transaction.ScheduledScreen
import com.clonical.kasiva.feature.transaction.AddButtonIntroSheet
import com.clonical.kasiva.feature.transaction.RecordMethodsSheet
import com.clonical.kasiva.feature.planning.*
import com.clonical.kasiva.core.model.managedTransactionSources
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.clonical.kasiva.i18n.*
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.i18n.menuLabel
import com.clonical.kasiva.core.i18n.tabLabel

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
    var deviceRevoked by remember { mutableStateOf(false) }
    // Perangkat Terhubung: daftarkan instalasi ini di users/{uid}/devices saat aplikasi dibuka (mode cloud), lalu
    // keluar bila perangkat lain mencabutnya. Kegagalan jaringan/rules tidak mengganggu pemakaian aplikasi.
    LaunchedEffect(user?.uid, localMode) {
        if (localMode || user == null) return@LaunchedEffect
        val deviceId = native.readSetting(DEVICE_ID_KEY)?.takeIf { it.matches(FinanceRepository.DEVICE_ID) }
            ?: ("dev-" + com.clonical.kasiva.core.util.newId()).also { native.writeSetting(DEVICE_ID_KEY, it) }
        try {
            val active = repository.registerDevice(deviceId, native.deviceDescription(), native.devicePlatform(), native.appVersion())
            if (active) repository.deviceRevocations(deviceId).first { it }
            try { repository.forgetDevice(deviceId) } catch (e: CancellationException) { throw e } catch (_: Exception) {}
            native.writeSetting(DEVICE_ID_KEY, "")
            deviceRevoked = true
            repository.logout()
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) {}
    }
    var starting by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(800); starting = false }
    DisposableEffect(lifecycle) {
        lifecycle.onBackground = { if (native.lockEnabled()) locked = true }
        onDispose { lifecycle.onBackground = null }
    }
    val preferenceOwner=if(localMode)"local" else user?.uid
    val preferences = remember(preferenceOwner) { PreferenceState(native, preferenceOwner) }
    val activeLanguage = com.clonical.kasiva.core.i18n.resolveLanguage(preferences.value.language, native.deviceLanguage())
    remember(activeLanguage) { com.clonical.kasiva.core.i18n.AppLocale.apply(activeLanguage) }
    val setupState = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    LaunchedEffect(preferences, localMode, user?.uid, preferenceRetry) {
        if (!localMode && user != null) {
            preferenceError = null
            try { repository.synchronizePreferences(preferences) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { preferenceError = e.message ?: MR.strings.app_pref_sync_failed.text() }
        }
    }
    fun googleSignIn() {
        if (googleBusy) return
        googleBusy = true
        googleError = null
        native.googleSignIn { token, failure ->
            if (token == null) {
                googleBusy = false
                googleError = failure ?: MR.strings.app_google_login_cancelled.text()
            } else authScope.launch {
                try { repository.googleLogin(token); native.setOnboardingComplete(); tourDone = true }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) { googleError = e.message ?: MR.strings.app_google_login_failed.text() }
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
    KasivaTheme {
        Surface(Modifier.fillMaxSize(), color = FFColors.background) {
            when {
                starting -> SplashScreen(showLogo = !native.hasSystemSplash())
                locked && (user != null || localMode) -> Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center) {
                    Text(MR.strings.app_locked_title.text(), style = MaterialTheme.typography.headlineMedium)
                    Text(MR.strings.app_locked_body.text())
                    Button(onClick = { native.unlock { if (it) locked = false } }) { Text(MR.strings.app_locked_unlock.text()) }
                }
                connecting && user!=null -> com.clonical.kasiva.feature.settings.CloudSyncScreen(localRepository.value,repository,native,preferences.value,::useCloud,{connecting=false},lifecycle)
                connecting -> AuthScreen(repository,native,authScope,lifecycle,::startLocal,::restoreLocal,cancel={connecting=false})
                previewTour -> OnboardingScreen { previewTour = false; previewSetup = true }
                previewSetup -> OlloSetupFlow(native, lifecycle, busy=false,
                    onComplete = { previewSetup = false; previewFinished = true },
                    onSignIn = { cloudAuth = true },
                    onRestore = {},
                )
                previewFinished -> Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center) {
                    Text(MR.strings.app_preview_done_title.text(), style = FFType.appTitle)
                    Spacer(Modifier.height(10.dp))
                    Text(MR.strings.app_preview_done_body.text(), style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { native.closeApp() }, modifier = Modifier.fillMaxWidth()) { Text(MR.strings.app_preview_close.text()) }
                }
                localMode -> key("local") { FinanceScreen(localRepository.value,native,MR.strings.app_default_user.text(),lifecycle,onConnect={connecting=true},initialRestore=incomingBackup,onRestoreOpened={incomingBackup=null},askAddButton=askAddButton,onAskAddButtonShown={askAddButton=false}) }
                user != null -> key(user!!.uid) { FinanceScreen(repository, native, user!!.displayName ?: MR.strings.app_default_user.text(), lifecycle,onLocal={authScope.launch{
                    try {val profile=localRepository.value.observe().first().profile;startLocal(profile?.name ?: "Pengguna",profile?.baseCurrency ?: "IDR","Kas",0)}
                    catch(e:CancellationException){throw e}catch(e:Exception){localFailure=e.message}
                }}) }
                cloudAuth -> AuthScreen(repository, native, authScope, lifecycle,::startLocal,::restoreLocal,cancel={cloudAuth=false},initialScreen="login")
                !tourDone -> OnboardingScreen { native.setOnboardingComplete(); tourDone = true }
                else -> setupState.SaveableStateProvider("setup") { OlloSetupFlow(native, lifecycle, busy=googleBusy,
                    onComplete = { result ->
                        startLocal(result.name.trim().ifBlank { "Pengguna" }, result.currency, result.walletName, result.openingBalanceMinor)
                        PreferenceState(native, "local").update(result.preferences.copy(language = "system"))
                        result.photoBytes?.let { bytes -> runCatching { localRepository.value.saveProfilePhoto(native, bytes) } }
                        askAddButton = true
                    },
                    onSignIn = ::googleSignIn,
                    onEmailSignIn = { cloudAuth = true },
                    onRestore = { backup -> restoreLocal(backup) },
                ) }
            }
        }
        if (deviceRevoked) AlertDialog(onDismissRequest = { deviceRevoked = false },
            title = { Text(MR.strings.r3_device_revoked_title.text()) }, text = { Text(MR.strings.r3_device_revoked_body.text()) },
            confirmButton = { TextButton(onClick = { deviceRevoked = false }) { Text(MR.strings.common_close.text()) } })
        localFailure?.let{AlertDialog(onDismissRequest={localFailure=null},title={Text(MR.strings.app_local_open_failed_title.text())},text={Text(it)},confirmButton={TextButton(onClick={localFailure=null}){Text(MR.strings.common_close.text())}})}
        googleError?.let { message -> AlertDialog(onDismissRequest = { googleError = null }, title = { Text(MR.strings.app_google_login_title.text()) }, text = { Text(message) }, confirmButton = { TextButton(onClick = { googleError = null }) { Text(MR.strings.common_close.text()) } }) }
        preferenceError?.let { message -> AlertDialog(onDismissRequest = { preferenceError = null },
            title = { Text(MR.strings.app_pref_not_synced_title.text()) },
            text = { Text(MR.strings.app_pref_not_synced_body.text(message)) },
            confirmButton = { TextButton(onClick = { preferenceError = null; preferenceRetry++ }) { Text(MR.strings.app_retry.text()) } },
            dismissButton = { TextButton(onClick = { preferenceError = null }) { Text(MR.strings.app_later.text()) } }) }
    }
    }
}

/** Setting lokal berisi ID perangkat ini untuk users/{uid}/devices. */
private const val DEVICE_ID_KEY = "device.id"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinanceScreen(repo: FinanceRepository, native: NativeServices, name: String, lifecycle: AppLifecycle,onConnect:()->Unit={},onLocal:()->Unit={},initialRestore:Pair<FinanceBackup,String>?=null,onRestoreOpened:()->Unit={},askAddButton:Boolean=false,onAskAddButtonShown:()->Unit={}) {
    val scope = rememberCoroutineScope()
    val navigationBackdrop = rememberLayerBackdrop()
    var rawData by remember { mutableStateOf(FinanceData()) }
    // Nama kategori bawaan & dompet "Kas" yang belum diganti pengguna tampil dalam bahasa aktif.
    val activeLocale = com.clonical.kasiva.core.i18n.AppLocale.current
    val data = remember(rawData, activeLocale) { rawData.withLocalizedDefaults() }
    var ready by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var deleteAccount by remember { mutableStateOf(false) }
    var resetData by remember { mutableStateOf(false) }
    var automationDialog by remember { mutableStateOf(false) }
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
    val ai = com.clonical.kasiva.feature.ai.rememberAiController(native, if (repo.isLocal) null else repo.ownerId)
    var aiChat by remember { mutableStateOf(false) }
    var aiScan by remember { mutableStateOf(false) }
    var premiumGate by remember { mutableStateOf<String?>(null) }
    var aiParent by rememberSaveable { mutableStateOf("Beranda") }
    val speechLanguage = com.clonical.kasiva.feature.ai.resolveSpeechLocale(appearance.voiceLanguage, com.clonical.kasiva.core.i18n.AppLocale.current)
    // Backend AI (DeepSeek) menjawab dalam 9 bahasa aplikasi.
    val aiLanguage = com.clonical.kasiva.core.i18n.AppLocale.current.ifBlank { "id" }
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
            repo.observe().collect { rawData = it; ready = true }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message ?: MR.strings.app_data_load_failed.text() }
    }
    fun action(closeEditor: Boolean = false, block: suspend () -> Unit) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try { block(); if (closeEditor) editor = null }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: MR.strings.app_save_failed.text() }
            finally { busy = false }
        }
    }
    LaunchedEffect(ready, data.featuresReady, dataPaused) {
        if (ready && data.featuresReady && !dataPaused) {
            try { repo.planner.processDueRecurring() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { notice = MR.strings.app_recurring_not_processed.text(e.message.toString()) }
        }
    }
    val savedPages = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    val displayName = data.profile?.name ?: name
    fun openTransaction(tx: com.clonical.kasiva.core.model.Transaction) {
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
        if (destination == "Premium" && !com.clonical.kasiva.feature.ai.PREMIUM_ENABLED) return
        if(destination.startsWith("Dompet:")){
            selectedAccountId=destination.substringAfter(':');previousPage=page
            val goal=data.goals.find{it.holdingAccountId==selectedAccountId}
            if(goal!=null){goalParent=page;selectedGoalId=goal.id;page="Detail impian"}else {accountParent=page;page="Detail dompet"}
            return
        }
        if(destination.startsWith("Transaksi:")){transactionFilter=destination.substringAfter(':');previousPage=page;page="Transaksi";return}
        if(destination=="Transaksi")transactionFilter="all"
        when (destination) {
            "Otomasi" -> automationDialog = true
            "Keluar Aplikasi" -> { if (!native.closeApp()) notice = MR.strings.app_close_via_switcher.text() }
            "Bagikan" -> { native.shareApp()?.let{error=it} }
            "Tambah transaksi" -> editor = Editor.Tx()
            "Profil akun" -> editor = Editor.Person
            "Berulang" -> { previousPage = page; page = "Terjadwal" }
            "Tagihan", "Utang", "Keinginan", "Kartu", "Catatan", "Reimburse" -> { previousPage = page; page = destination }
            in aiPages -> { if (page !in aiPages) aiParent = page; page = destination }
            "Ubah password" -> action {
                val email = repo.currentUser?.email
                require(!email.isNullOrBlank()) { MR.strings.app_email_unavailable.text() }
                repo.resetPassword(email)
                notice = MR.strings.app_password_reset_sent.text()
            }
            else -> { if(destination=="Detail impian")goalParent=page;if (destination !in mainPages) previousPage = page; page = destination }
        }
    }
    fun chooseBackup(){native.pickCsv{content,failure->if(content!=null)try{restorePreview=BackupCodec.decode(content) to native.sha256(content)}catch(e:Exception){error=e.message}else if(failure!=null)error=failure}}
    fun back() { page = if (page.startsWith(com.clonical.kasiva.feature.settings.PROFILE_GROUP_PREFIX)) "Profil" else when (page) { "Pengaturan Asisten" -> "Asisten"; "Asisten", "Premium", "Pengaturan AI Scan" -> aiParent; "Masukan & Peta Jalan" -> "Profil"; "Masukan" -> previousPage.takeIf { it == "Masukan & Peta Jalan" || it in mainPages } ?: "Profil"; "Detail dompet" -> accountParent; "Transaksi terjadwal" -> "Terjadwal"; "Detail investasi" -> "Investasi"; "Detail impian" -> goalParent; "Detail transaksi" -> transactionParent; else -> previousPage.takeIf { it in mainPages || it.startsWith(com.clonical.kasiva.feature.settings.PROFILE_GROUP_PREFIX) } ?: "Beranda" } }
    val planningActions = PlanningActions(
        bill = { editor = Editor.BillForm(it) }, payBill = { editor = Editor.PayBill(it) },
        debt = { editor = Editor.DebtForm(it) }, payDebt = { editor = Editor.PayDebt(it) },
        wish = { editor = Editor.WishForm(it) }, fulfillWish = { action { repo.planner.saveWish(it) } },
        card = { editor = Editor.CardForm(it) }, note = { editor = Editor.NoteForm(it) },
        claim = { editor = Editor.ClaimForm(it) }, receiveClaim = { editor = Editor.ReceiveClaim(it) },
        recurring = { editor = Editor.RepeatForm(it) }, processRecurring = { action { val count = repo.planner.processDueRecurring(); notice = MR.strings.app_recurring_recorded.text(count) } },
        manualScheduled = { navigate("Transaksi terjadwal") }, addExpense = { editor = Editor.Tx() },
        reverse = { event -> deletion = "batal:pembayaran" to { repo.planner.reverseEvent(event.id) } },
        payBundle = {note,items->editor=Editor.PayBundle(note,items)},
        updateNote = {note->action{repo.planner.saveNote(note)}},
    )
    DisposableEffect(lifecycle, page) {
        lifecycle.onBack = { if (page !in mainPages) { back(); true } else false }
        onDispose { lifecycle.onBack = null }
    }
    val topBarSlot = remember { mutableStateOf<(@Composable RowScope.() -> Unit)?>(null) }
    // Ikon dompet kustom milik pemilik data ini (cloud: Storage + cache; lokal: berkas di perangkat).
    val walletIcons = remember(repo) {
        val owner = requireNotNull(repo.ownerId)
        WalletIconStore(
            pickImage = { result -> native.pickImage(WALLET_ICON_MAX_BYTES, result) },
            saveIcon = { accountId, bytes, result -> native.saveWalletIcon(owner, accountId, bytes, result) },
            loadIcon = { accountId, result -> native.loadWalletIcon(owner, accountId) { bytes, _ -> if (bytes != null) result(bytes) } },
        )
    }
    // Perangkat Terhubung: daftar perangkat hanya didengarkan selama halamannya terbuka.
    var devices by remember(repo) { mutableStateOf<List<ConnectedDevice>>(emptyList()) }
    val devicesOpen = page == "Perangkat Terhubung"
    LaunchedEffect(repo, devicesOpen) {
        if (devicesOpen && !repo.isLocal) try { repo.observeDevices().collect { devices = it } }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message ?: MR.strings.app_data_load_failed.text() }
    }
    CompositionLocalProvider(LocalWalletIcons provides walletIcons) {
    CompositionLocalProvider(LocalTopBarActions provides topBarSlot) {
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (!dataPaused && page !in mainPages && page !in listOf("Transaksi", "Detail investasi", "Aktivitas") && page !in aiPages) CenterAlignedTopAppBar(
                title = { Text(when(page) {
                    "Detail dompet" -> data.accounts.find { it.id == selectedAccountId }?.name ?: pageTitle(page)
                    else -> pageTitle(page)
                }, style = FFType.appTitle) },
                navigationIcon = { FFIconButton("hi_chevron_left", ::back, tint = FFColors.onSurface) },
                actions = {
                    topBarSlot.value?.invoke(this)
                    when (page) {
                        "Investasi" -> FFIconButton("add_rounded", {
                            editor = when(page) { "Anggaran" -> Editor.Limit(); "Target" -> Editor.Target(); else -> Editor.Asset() }
                        })
                        "Terjadwal" -> FFIconButton("add_rounded", { editor = Editor.RepeatForm(null) })
                        "Detail dompet" -> data.accounts.find { it.id == selectedAccountId }?.let { account -> Box {
                            IconButton({ walletDetailMenu = true }, Modifier.testTag("wallet-detail-menu")) { FFIcon("more_vert", contentDescription = MR.strings.app_wallet_menu.text()) }
                            DropdownMenu(walletDetailMenu, { walletDetailMenu = false }) {
                                DropdownMenuItem(text = { Text(MR.strings.app_adjust_balance.text()) }, onClick = { walletDetailMenu = false; editor = Editor.AdjustWallet(account) })
                                DropdownMenuItem(text = { Text(MR.strings.app_edit_archive_wallet.text()) }, onClick = { walletDetailMenu = false; editor = Editor.Wallet(account) })
                            }
                        } }
                    }
                },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = if (page in financeMenus || page == "Terjadwal") FFColors.surface else FFColors.background),
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
                if (!ready) TextButton(onClick = { retry++ }) { Text(MR.strings.app_reload.text()) }
                else TextButton(onClick = { error = null }) { Text(MR.strings.common_close.text()) }
            } }
            notice?.let { TextButton(onClick = { notice = null }) { Text(it) } }
            if(syncPending){
                Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp,Alignment.CenterVertically)){
                    Text(MR.strings.app_sync_pending_title.text(),style=FFType.appTitle)
                    Text(MR.strings.app_sync_pending_body.text())
                    Button(onClick=onConnect,modifier=Modifier.fillMaxWidth()){Text(MR.strings.app_sync_pending_continue.text())}
                    if(ready)OutlinedButton(onClick={native.shareTextFile("finflow-lokal-${today()}.json",BackupCodec.encode(FinanceBackup(ownerId="local",data=data,preferences=appearance)),"application/json")}){Text(MR.strings.app_save_local_backup.text())}
                }
            }else if(resetPending){
                Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp,Alignment.CenterVertically)) {
                    Text(MR.strings.app_reset_pending_title.text(),style=FFType.appTitle)
                    Text(MR.strings.app_reset_pending_body.text())
                    Button(enabled=!busy,onClick={resetData=true}){Text(MR.strings.app_reset_pending_continue.text())}
                }
            }else if(restorePending!=null){
                Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),verticalArrangement=Arrangement.Center){
                    Text(MR.strings.app_restore_pending_title.text(),style=FFType.appTitle)
                    Spacer(Modifier.height(16.dp));Text(MR.strings.app_restore_pending_body.text(),style=FFType.bodyMain)
                    Spacer(Modifier.height(20.dp));Button(enabled=!busy,onClick=::chooseBackup,modifier=Modifier.fillMaxWidth()){Text(MR.strings.app_restore_pending_continue.text())}
                    TextButton(enabled=!busy,onClick={action{repo.logout()}}){Text(MR.strings.app_sign_out_account.text())}
                }
            } else if (!ready) {
                Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (error == null) MR.strings.app_preparing_account_data.text() else MR.strings.app_account_data_load_failed.text(), style = FFType.sectionTitle)
                    if (error != null) {
                        Spacer(Modifier.height(12.dp))
                        Text(MR.strings.app_account_data_access_failed.text(), style = FFType.bodyMain)
                        TextButton(enabled = !busy, onClick = { action { repo.logout() } }) { Text(MR.strings.app_sign_out_switch_account.text()) }
                    }
                }
            } else {
            val pageContent: @Composable (String) -> Unit = { shown -> savedPages.SaveableStateProvider(shown) { when (shown) {
                "Beranda" -> Dashboard(data, month, { month = it }, ::openTransaction, displayName, ::navigate)
                "Transaksi" -> Transactions(data, month, { month = it }, ::openTransaction, ::back, initialType=transactionFilter)
                "Dompet" -> WalletScreen(data, { editor = Editor.Wallet(it) }, { editor = Editor.Wallet() }, { navigate("Dompet:${it.id}") })
                "Detail dompet" -> data.accounts.find{it.id==selectedAccountId}?.let{account->WalletDetail(account,data,{editor=Editor.Tx(accountId=account.id,type=it)},::openTransaction)}
                in preferencePages -> PreferenceScreen(shown,data,::navigate)
                "Masukan & Peta Jalan" -> com.clonical.kasiva.feature.settings.RoadmapScreen { navigate("Masukan") }
                in accountDataPages -> AccountDataScreen(shown,data,native,repo.currentUser?.email.orEmpty(),::navigate,{retry++},back=::back,
                    backup={native.shareTextFile("finflow-backup-${today()}.json",BackupCodec.encode(FinanceBackup(ownerId=requireNotNull(repo.ownerId),data=data,preferences=appearance)),"application/json")},
                    chooseRestore=::chooseBackup,busy=busy,changePhoto={
                        val uid=repo.ownerId
                        native.pickProfilePhoto { bytes,failure ->
                            if (failure!=null) error=failure
                            else if (bytes!=null && uid==repo.ownerId) action { repo.saveProfilePhoto(native,bytes);notice=MR.strings.app_profile_photo_updated.text() }
                        }
                    },deleteAccount={deleteAccount=true},localMode=repo.isLocal,connect=onConnect,useLocal=onLocal,
                    ai=ai,signOut=if(repo.isLocal) null else ({ action { repo.logout() } }),factoryReset={resetData=true},
                    saveToDownloads=native::saveToDownloads,
                    deleteTransactions={ action {
                        repo.deleteAllTransactions()
                        notice=MR.strings.r3_delete_tx_done.text()
                    } },
                    devices=devices,currentDeviceId=native.readSetting(DEVICE_ID_KEY)?.takeIf{it.isNotBlank()},
                    removeDevice=if(repo.isLocal) null else ({ id -> action { repo.revokeDevice(id); notice=MR.strings.r3_devices_removed.text() } }))
                "Kategori" -> com.clonical.kasiva.feature.settings.CategoryScreen(data,busy,edit={editor=Editor.Cat(it)},
                    addSub={root->editor=Editor.Cat(null,root.id)},saveCategory={category->action{repo.saveCategory(category)}})
                "Profil" -> ProfileScreen(native, displayName, if(repo.isLocal)MR.strings.app_local_mode.text() else repo.currentUser?.email.orEmpty(), data, ::navigate, {
                    if(repo.isLocal){if(!native.closeApp())notice=MR.strings.app_close_via_switcher.text()}else action { repo.logout() }
                },localMode=repo.isLocal,premiumCard={ if (com.clonical.kasiva.feature.ai.PREMIUM_ENABLED) com.clonical.kasiva.feature.ai.UpgradePremiumCard(ai) { navigate("Premium") } },
                    onChangePhoto={ camera ->
                        val uid=repo.ownerId
                        val done:(ByteArray?,String?)->Unit = { bytes,failure ->
                            if (failure!=null) error=failure
                            else if (bytes!=null && uid==repo.ownerId) action { repo.saveProfilePhoto(native,bytes);notice=MR.strings.app_profile_photo_updated.text() }
                        }
                        if (camera) native.captureProfilePhoto(done) else native.pickProfilePhoto(done)
                    })
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
                } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(MR.strings.app_transaction_not_found.text()) }
                "Anggaran" -> BudgetScreen(data, month, { month = it }, { editor = Editor.Limit(it) }, { editor = Editor.Limit() })
                "Target" -> GoalScreen(data, { selectedGoalId = it.id; navigate("Detail impian") }, { editor = Editor.Target() })
                "Detail impian" -> data.goals.find { it.id == selectedGoalId }?.let { goal -> GoalDetail(goal,edit={editor=Editor.Target(goal)},data=data,
                    deposit={editor=Editor.GoalMovement(goal,false)},withdraw={editor=Editor.GoalMovement(goal,true)},reverse=planningActions.reverse) }
                    ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(MR.strings.app_goal_not_found.text()) }
                "Investasi" -> InvestmentScreen(data, { selectedInvestmentId = it.id; navigate("Detail investasi") }, { editor = Editor.Asset() })
                "Detail investasi" -> data.investments.find { it.id == selectedInvestmentId }?.let { investment ->
                    InvestmentDetail(investment, ::back, { editor = Editor.Asset(investment) }, {
                        val remove: suspend () -> Unit = { repo.deleteInvestment(investment.id); page = "Investasi" }
                        deletion = "investasi" to remove
                    }, onUpdateValue = { value -> action { repo.saveInvestment(investment.copy(currentMinor = value)) } }, busy = busy)
                } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(MR.strings.app_investment_not_found.text()) }
                "Notifikasi" -> NotificationSettings(native)
                "Asisten" -> com.clonical.kasiva.feature.ai.AssistantScreen(ai, data, aiLanguage, ::back, { navigate("Pengaturan Asisten") }, { navigate("Premium") })
                "Pengaturan Asisten" -> com.clonical.kasiva.feature.ai.AssistantSettingsScreen(ai, ::back)
                "Pengaturan AI Scan" -> com.clonical.kasiva.feature.ai.ScanSettingsScreen(ai, ::back)
                "Premium" -> com.clonical.kasiva.feature.ai.PremiumScreen(ai, native, ::back, onConnect)
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
                        notice = MR.strings.app_import_added.text(added)
                        plan = null
                        importProgress = ""
                    } }
                })
                "Koneksi bank" -> BankConnectionScreen({ navigate("Impor data") }, { navigate("Impor notifikasi") })
                "Pengaturan" -> Settings(native, repo.currentUser?.email ?: MR.strings.app_local_mode.text(), data, { editor = Editor.Cat() }, {
                    if(repo.isLocal){if(!native.closeApp())notice=MR.strings.app_close_via_switcher.text()}else action { repo.logout() }
                }, navigate = ::navigate, localMode = repo.isLocal)
                "Pengaturan Dompet" -> com.clonical.kasiva.feature.account.WalletSettingsScreen(data, busy, importWallets = { list -> action { list.forEach { repo.saveAccount(it) } } })
                "Bahasa Input Suara" -> com.clonical.kasiva.feature.ai.VoiceLanguageScreen()
                "Kurs" -> com.clonical.kasiva.feature.settings.ExchangeRatesScreen(data)
                "Terjadwal" -> com.clonical.kasiva.feature.transaction.RecurringListScreen(
                    data = data, busy = busy,
                    onAdd = { editor = Editor.RepeatForm(null) },
                    onEdit = { editor = Editor.RepeatForm(it) },
                    onArchive = { value -> deletion = "arsip:jadwal berulang" to { repo.planner.archiveRecurring(value.id) } },
                    onProcess = planningActions.processRecurring,
                    onCalendar = planningActions.manualScheduled,
                )
                "Tagihan", "Utang", "Keinginan", "Kartu", "Catatan", "Reimburse" -> PlanningScreen(shown, data, busy, planningActions)
                else -> if (shown.startsWith(com.clonical.kasiva.feature.settings.PROFILE_GROUP_PREFIX)) com.clonical.kasiva.feature.settings.ProfileGroupScreen(shown, ::navigate)
            } } }
            // Preferensi "Transisi halaman": geser + pudar singkat; mati = berpindah seketika.
            if (appearance.pageTransitions) AnimatedContent(page, transitionSpec = {
                (fadeIn(tween(200, delayMillis = 40)) + slideInHorizontally(tween(240)) { it / 14 }) togetherWith fadeOut(tween(120))
            }, label = "page") { shown -> pageContent(shown) }
            else pageContent(page)
            }
        }
    }
    }
    if (addIntro) AddButtonIntroSheet(appearance.addButtonMode, onChoose = { mode ->
        preferenceStore?.update(appearance.copy(addButtonMode = mode))
        addIntro = false
        if (mode == "all") recordSheet = true
    }, onDismiss = { addIntro = false })
    if (recordSheet) com.clonical.kasiva.feature.ai.QuickRecordBar(ai,
        onAssistant = { navigate("Asisten") },
        onVoice = { ai.requireConsent(AiFeature.Voice) {
            native.recognizeSpeech(speechLanguage) { text, failure ->
                if (text != null) ai.parse(AiFeature.Voice, text, data, appearance.defaultWalletId, aiLanguage)
                else if (failure != null) ai.error = failure
            }
        } },
        onScan = { ai.requireConsent(AiFeature.Scan) { aiScan = true } },
        onChat = { ai.requireConsent(AiFeature.Chat) { aiChat = true } },
        onAdd = { editor = Editor.Tx() },
        onDismiss = { recordSheet = false },
    )
    if (aiChat) com.clonical.kasiva.feature.ai.AiChatSheet(ai, onSend = { text ->
        ai.parse(AiFeature.Chat, text, data, appearance.defaultWalletId, aiLanguage) { aiChat = false }
    }, onDismiss = { aiChat = false })
    if (aiScan) com.clonical.kasiva.feature.ai.AiScanSheet(ai,
        onCamera = { native.captureReceiptPhoto { photo, failure ->
            if (photo != null) { aiScan = false; ai.scan(listOf(photo), data, appearance.defaultWalletId, aiLanguage) } else if (failure != null) ai.error = failure
        } },
        onGallery = { native.pickReceiptPhotos(5) { photos, failure ->
            if (!photos.isNullOrEmpty()) { aiScan = false; ai.scan(photos, data, appearance.defaultWalletId, aiLanguage) } else if (failure != null) ai.error = failure
        } },
        onSettings = { aiScan = false; navigate("Pengaturan AI Scan") },
        onDismiss = { aiScan = false })
    com.clonical.kasiva.feature.ai.AiDraftReviewSheet(ai, data, busy,
        onEdit = { tx -> editor = Editor.Tx(tx, aiDraft = true, onImported = { ai.drafts = ai.drafts.filterNot { it.id == tx.id } }) },
        onSaveAll = { drafts -> action {
            drafts.forEach { tx -> repo.saveTransaction(tx); ai.drafts = ai.drafts.filterNot { it.id == tx.id } }
            notice = MR.strings.app_ai_drafts_saved.text(drafts.size)
        } })
    com.clonical.kasiva.feature.ai.AiConsentDialog(ai)
    com.clonical.kasiva.feature.ai.AiErrorDialog(ai) { navigate("Premium") }
    if (ai.working != null && !aiChat && !aiScan && page != "Asisten") AlertDialog(onDismissRequest = {}, confirmButton = {},
        text = { Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(12.dp)); Text(ai.working.orEmpty()) } })
    LaunchedEffect(editor, ai.isPremium) {
        com.clonical.kasiva.feature.ai.freeLimitReached(editor, data)?.takeIf { !ai.isPremium }?.let { premiumGate = it; editor = null }
    }
    premiumGate?.let { com.clonical.kasiva.feature.ai.PremiumGateDialog(it, { navigate("Premium") }, { premiumGate = null }) }
    editor?.let { current ->
        EntityEditor(current, data, busy, dismiss = { if (!busy) editor = null },
            openCurrencySettings = { if (!busy) { editor = null; navigate("Pengaturan") } },
            onScan = if (current is Editor.Tx && current.value == null && !current.aiDraft && !current.notificationImport) ({ editor = null; ai.requireConsent(AiFeature.Scan) { aiScan = true } }) else null,
            saveTx = { transaction -> action(true) {
                if (current is Editor.Tx && current.aiDraft) {
                    repo.saveTransaction(transaction)
                    current.onImported?.invoke()
                    notice = MR.strings.app_ai_tx_saved.text()
                } else if (current is Editor.Tx && current.notificationImport) {
                    val added = repo.importNotification(transaction)
                    current.onImported?.invoke()
                    notice = if (added) MR.strings.app_notification_tx_saved.text() else MR.strings.app_tx_already_imported.text()
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
    restorePreview?.let{(backup,checksum)->AlertDialog(onDismissRequest={if(!busy)restorePreview=null},title={Text(MR.strings.app_restore_confirm_title.text())},text={
        Text(MR.strings.app_restore_confirm_body.text(backup.data.profile?.name.orEmpty(), backup.data.accounts.size, backup.data.transactions.size))
    },confirmButton={TextButton(enabled=!busy,onClick={
        restorePreview=null
        action {
            val uid=requireNotNull(repo.ownerId)
            val jobKey="restore.$uid"
            val pending=native.readSetting(jobKey)
            require(pending==null || pending.isEmpty() || pending==checksum){MR.strings.app_restore_other_pending.text()}
            val resuming=pending==checksum
            if(!resuming){
                require(data.transactions.isEmpty() && data.goals.isEmpty() && data.bills.isEmpty() && data.debts.isEmpty() && data.notes.isEmpty() && data.cards.isEmpty() && data.recurring.isEmpty() && data.investments.isEmpty() && data.budgets.isEmpty() && data.wishes.isEmpty() && data.reimbursements.isEmpty() && data.financialEvents.isEmpty()){MR.strings.app_restore_target_not_empty.text()}
            }
            repo.restoreBackup(backup,resuming,{native.writeSetting(jobKey,checksum);restorePending=checksum}){notice=it}
            val restored=BackupCodec.restoreData(backup)
            val wallet=backup.data.accounts.indexOfFirst{it.id==backup.preferences.defaultWalletId}
            preferenceStore?.update(backup.preferences.copy(defaultWalletId=if(wallet>=0)restored.accounts[wallet].id else ""))
            native.writeSetting(jobKey,"")
            restorePending=null
            notice=MR.strings.app_restore_done.text()
        }
    }){Text(MR.strings.app_restore.text())}},dismissButton={TextButton(enabled=!busy,onClick={restorePreview=null}){Text(MR.strings.common_cancel.text())}})}
    if (deleteAccount) DeleteAccountDialog(native,repo.currentUser?.providerData?.any{it.providerId=="google.com"}==true,busy,
        dismiss={deleteAccount=false},confirm={password,token->action{repo.requestAccountDeletion(password,token)}})
    if (automationDialog) com.clonical.kasiva.feature.settings.AutomationComingSoonDialog(native) { automationDialog = false }
    if(resetData)DeleteAccountDialog(native,repo.currentUser?.providerData?.any{it.providerId=="google.com"}==true,busy,
        dismiss={resetData=false},confirm={password,token->action{
            if(repo.isLocal){repo.clearLocalData();notice=native.deleteLocalProfilePhoto() ?: MR.strings.app_local_data_deleted.text()}
            else {repo.resetCloudData(native,password,token);notice=MR.strings.app_cloud_data_deleted.text()}
            resetData=false;page="Beranda";retry++
        }},dataOnly=true,localMode=repo.isLocal)
    deletion?.let { (label, block) ->
        val kind = when { label.startsWith("arsip:") -> "arsip"; label.startsWith("batal:") -> "batal"; else -> "hapus" }
        val noun = deletionNoun(label.substringAfter(':'))
        val verb = when(kind) { "arsip" -> MR.strings.app_verb_archive.text(); "batal" -> MR.strings.app_verb_cancel_record.text(); else -> MR.strings.common_delete.text() }
        AlertDialog(onDismissRequest = { deletion = null }, title = { Text(when(kind) {
            "arsip" -> MR.strings.app_confirm_archive_title.text(noun); "batal" -> MR.strings.app_confirm_cancel_title.text(noun); else -> MR.strings.app_confirm_delete_title.text(noun)
        }) },
        text = { Text(when(kind) { "arsip" -> MR.strings.app_confirm_archive_body.text(); "batal" -> MR.strings.app_confirm_cancel_body.text(); else -> MR.strings.app_confirm_delete_body.text() }) },
        confirmButton = { TextButton(enabled = !busy, onClick = { deletion = null; action(true, block) }) { Text(verb, color = FFColors.expense) } },
        dismissButton = { TextButton(onClick = { deletion = null }) { Text(MR.strings.common_cancel.text()) } }) }
    if (editor != null && error != null) AlertDialog(
        onDismissRequest = { error = null }, title = { Text(MR.strings.app_cannot_save_title.text()) },
        text = { Text(error.orEmpty()) },
        confirmButton = { TextButton(onClick = { error = null }) { Text(MR.strings.app_back_to_form.text()) } },
    )
    }
}

/** Kata benda pada dialog konfirmasi hapus/arsip/batal. Label pengenal (mis. "arsip:tagihan") tetap berbahasa Indonesia. */
private fun deletionNoun(id: String): String = when (id) {
    "pembayaran" -> MR.strings.app_noun_payment.text()
    "pencatatan" -> MR.strings.app_noun_record.text()
    "transaksi" -> MR.strings.app_noun_transaction.text()
    "investasi" -> MR.strings.app_noun_investment.text()
    "jadwal berulang" -> MR.strings.app_noun_recurring_schedule.text()
    "tagihan" -> MR.strings.app_noun_bill.text()
    "hutang / piutang" -> MR.strings.app_noun_debt.text()
    "pencatatan hutang / piutang" -> MR.strings.app_noun_debt_record.text()
    "keinginan" -> MR.strings.app_noun_wish.text()
    "catatan kartu" -> MR.strings.app_noun_card_note.text()
    "catatan" -> MR.strings.app_noun_note.text()
    "klaim reimburse" -> MR.strings.app_noun_reimburse_claim.text()
    "catatan transaksi" -> MR.strings.app_noun_transaction_record.text()
    "anggaran" -> MR.strings.app_noun_budget.text()
    "target tabungan" -> MR.strings.app_noun_savings_goal.text()
    "target" -> MR.strings.app_noun_goal.text()
    else -> id
}

/** Judul top bar untuk pengenal halaman navigasi. Pengenal `page` tetap berbahasa Indonesia; hanya judulnya diterjemahkan. */
fun pageTitle(page: String): String = com.clonical.kasiva.feature.settings.profileGroupTitle(page) ?: when (page) {
    "Beranda", "Statistik", "Dompet", "Profil" -> tabLabel(page)
    "Anggaran", "Tagihan", "Utang", "Keinginan", "Kartu", "Catatan", "Reimburse", "Investasi" -> menuLabel(page)
    "Terjadwal" -> menuLabel("Berulang")
    "Target" -> MR.strings.app_page_savings_goals.text()
    "Masukan & Peta Jalan" -> MR.strings.app_page_roadmap.text()
    "Aktivitas" -> MR.strings.app_page_activity.text()
    "Detail impian" -> MR.strings.app_page_goal_detail.text()
    "Detail transaksi" -> MR.strings.app_page_tx_detail.text()
    "Detail dompet" -> MR.strings.app_page_wallet_detail.text()
    "Detail investasi" -> MR.strings.app_page_investment_detail.text()
    "Notifikasi" -> MR.strings.app_page_notification_settings.text()
    "Transaksi" -> MR.strings.app_page_transactions.text()
    "Transaksi terjadwal" -> MR.strings.app_page_scheduled_tx.text()
    "Kategori" -> MR.strings.app_page_categories.text()
    "Impor notifikasi" -> MR.strings.app_page_import_notifications.text()
    "Impor data" -> MR.strings.app_page_import_data.text()
    "Koneksi bank" -> MR.strings.app_page_bank_connection.text()
    "Pengaturan" -> MR.strings.app_page_settings.text()
    "Asisten" -> MR.strings.app_page_assistant.text()
    "Pengaturan Asisten" -> MR.strings.app_page_assistant_settings.text()
    "Pengaturan AI Scan" -> MR.strings.app_page_ai_scan_settings.text()
    "Bahasa" -> MR.strings.set_page_language.text()
    "Hapus Data" -> MR.strings.set_profile_delete_data.text()
    "Pengaturan Dompet" -> MR.strings.set_profile_wallet_settings.text()
    "Bahasa Input Suara" -> MR.strings.pd_voice_page_title.text()
    "Kurs" -> MR.strings.pf_page_exchange_rates.text()
    "Tema" -> MR.strings.app_page_theme.text()
    "Tampilan Kartu" -> MR.strings.app_page_card_display.text()
    "Tampilan Menu" -> MR.strings.app_page_menu_display.text()
    "Susunan Menu" -> MR.strings.app_page_menu_order.text()
    "Palet Transaksi" -> MR.strings.app_page_tx_palette.text()
    "Urutan Statistik" -> MR.strings.app_page_stats_order.text()
    "Tampilan Beranda" -> MR.strings.app_page_home_display.text()
    "Tampilan Tambah Transaksi" -> MR.strings.app_page_add_tx_display.text()
    "Paket Ikon" -> MR.strings.app_page_icon_pack.text()
    "Format Saldo" -> MR.strings.app_page_balance_format.text()
    "Periode Pencatatan" -> MR.strings.app_page_recording_period.text()
    "Memori Pencatatan" -> MR.strings.app_page_recording_memory.text()
    "Manajemen Akun" -> MR.strings.app_page_account_management.text()
    "Cadangan & Pemulihan" -> MR.strings.app_page_backup_restore.text()
    "Perangkat Terhubung" -> MR.strings.app_page_connected_devices.text()
    "Ekspor Data" -> MR.strings.app_page_export_data.text()
    "Bantuan" -> MR.strings.app_page_help.text()
    "Masukan" -> MR.strings.app_page_feedback.text()
    "Laporkan Bug" -> MR.strings.app_page_report_bug.text()
    "Tentang" -> MR.strings.app_page_about.text()
    "Catatan Pembaruan" -> MR.strings.app_page_release_notes.text()
    else -> page
}


