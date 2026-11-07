package com.logikawan.finflow

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.logikawan.finflow.app.navigation.MainNavigation
import com.logikawan.finflow.core.model.*
import com.logikawan.finflow.core.platform.NativeServices
import com.logikawan.finflow.core.ui.*
import com.logikawan.finflow.core.util.today
import com.logikawan.finflow.feature.account.*
import com.logikawan.finflow.feature.auth.*
import com.logikawan.finflow.feature.budget.*
import com.logikawan.finflow.feature.dashboard.*
import com.logikawan.finflow.feature.goal.*
import com.logikawan.finflow.feature.report.*
import com.logikawan.finflow.feature.settings.*
import com.logikawan.finflow.feature.transaction.*
import com.logikawan.finflow.feature.planning.*
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toPixelMap
import com.logikawan.finflow.core.util.formatBalance
import kotlin.math.abs
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class FinFlowDesignDeviceTest {
    @get:Rule val ui = createComposeRule()
    private val month = today().toString().take(7)
    private val fixture = FinanceData(
        profile = Profile("design-qa", "Pengguna Uji", "design@example.invalid"),
        accounts = listOf(Account(id = "qa-cash", name = "Dompet Uji")),
        categories = defaultCategories,
        transactions = listOf(Transaction(id = "qa-tx", type = "expense", amountMinor = 4500000, currency = "IDR",
            accountId = "qa-cash", categoryId = defaultCategories.first { it.type == "expense" }.id,
            title = "Makan Siang Uji", date = today().toString())),
        budgets = listOf(Budget("qa-budget", defaultCategories.first { it.type == "expense" }.id, 30000000, "IDR", month)),
        goals = listOf(Goal("qa-goal", "Laptop Baru", 1200000000, 360000000, "IDR", "2027-06-15")),
    )

    @Test fun onboardingHasFourRealIllustrationsAndFinishes() {
        var completed = false
        ui.setContent { FinFlowTheme { OnboardingScreen { completed = true } } }
        val titles = listOf("Catat dalam Sekejap", "Pahami Keuanganmu", "Kendali Penuh", "Tumbuhkan Tabungan")
        titles.forEachIndexed { index, title ->
            ui.onNodeWithText(title).assertIsDisplayed()
            screenshot("onboarding-${index + 1}")
            ui.onNodeWithTag("onboarding-next").performClick()
            ui.waitForIdle()
        }
        ui.runOnIdle { assertTrue(completed) }
    }

    @Test fun welcomeLoginAndRegistrationKeepExistingAccountFlow() {
        ui.setContent {
            var screen by remember { mutableStateOf("welcome") }
            var name by remember { mutableStateOf("") }
            var email by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            FinFlowTheme { AuthViews(screen, { screen = it }, name, { name = it }, email, { email = it },
                password, { password = it }, false, null, {}, {}, {}) }
        }
        ui.onNodeWithTag("auth-welcome").assertExists()
        screenshot("welcome")
        ui.onNodeWithText("Masuk", substring = false).performScrollTo().performClick()
        ui.onNodeWithTag("auth-login").assertExists()
        screenshot("login")
        ui.onNodeWithText("Daftar", substring = false).performScrollTo().performClick()
        ui.onNodeWithTag("auth-register").assertExists()
        ui.onNodeWithText("Nama", substring = false).assertExists()
        ui.onNodeWithText("Nomor Telepon", substring = false).assertDoesNotExist()
        screenshot("register")
    }

    @Test fun navigationAndAllPrimaryScreensRenderFromIsolatedData() {
        var page by mutableStateOf("Beranda")
        var added = false
        ui.setContent { FinFlowTheme {
            val backdrop = rememberLayerBackdrop()
            Scaffold(bottomBar = { MainNavigation(page, { page = it }, { added = true }, true, backdrop) }) { padding ->
                Column(Modifier.fillMaxSize().layerBackdrop(backdrop).background(designBackground).padding(top = padding.calculateTopPadding())) {
                    when(page) {
                        "Beranda" -> Dashboard(fixture, month, {}, {}, "Pengguna Uji", {})
                        "Dompet" -> WalletScreen(fixture, {}, {})
                        "Statistik" -> Reports(fixture, month, {}, {}, {}, {})
                        "Profil" -> ProfileScreen(FakeNative(), "Pengguna Uji", "design@example.invalid", fixture, {}, {})
                    }
                }
            }
        } }
        ui.onNodeWithText("Total Saldo (IDR)").assertIsDisplayed()
        val homeNav=ui.onNodeWithTag("navigation-glass").fetchSemanticsNode().boundsInWindow
        screenshot("dashboard")
        ui.onNodeWithTag("tab-Dompet").performClick()
        ui.onNodeWithText("Dompet Saya").assertIsDisplayed()
        screenshot("wallet")
        ui.onNodeWithTag("tab-Statistik").performClick()
        ui.onNodeWithTag("stats-type-expense").assertExists()
        screenshot("statistics-top")
        ui.onNodeWithTag("statistics").performScrollToNode(hasText("Peta Aktivitas"))
        screenshot("statistics-activity")
        ui.onNodeWithTag("tab-Profil").performClick()
        ui.onNodeWithText("Pengguna Uji").assertIsDisplayed()
        ui.onNodeWithText("Manajemen Akun").assertExists()
        ui.onNodeWithText("Upgrade ke Premium").assertDoesNotExist()
        assertEquals(homeNav,ui.onNodeWithTag("navigation-glass").fetchSemanticsNode().boundsInWindow)
        screenshot("profile")
        ui.onNodeWithTag("add-transaction").performClick()
        ui.runOnIdle { assertTrue(added) }
    }

    @Test fun glassDragReleaseAndCancellationRestoreIdleState() {
        var page by mutableStateOf("Beranda")
        ui.setContent { FinFlowTheme { Box(Modifier.fillMaxSize()) { MainNavigation(page,{page=it},{},true) } } }
        ui.onNodeWithTag("navigation-glass").performTouchInput {
            swipe(Offset(width*.12f,height*.5f),Offset(width*.92f,height*.5f),600)
        }
        ui.runOnIdle { assertEquals("Profil",page) }
        ui.onNodeWithTag("navigation-glass").assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription,"idle"))
        ui.onNodeWithTag("navigation-glass").performTouchInput {
            down(Offset(width*.87f,height*.5f));moveTo(Offset(width*.3f,height*.5f),200);cancel()
        }
        ui.runOnIdle { assertEquals("Profil",page) }
        ui.onNodeWithTag("navigation-glass").assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription,"idle"))
        ui.onNodeWithTag("tab-Beranda").performClick()
        ui.runOnIdle { assertEquals("Beranda",page) }
    }

    @Test fun allSevenPlanningMenusOpenEditorsAndPaymentActions() {
        var page by mutableStateOf("Tagihan")
        var opened:String?=null
        val data=fixture.copy(featuresReady=true,
            bills=listOf(Bill(id="qa-bill",title="Listrik Uji",amountMinor=10000,accountId="qa-cash")),
            debts=listOf(Debt(id="qa-debt",person="Peminjam Uji",amountMinor=10000,accountId="qa-cash")),
            wishes=listOf(Wish(title="Kamera Uji",targetMinor=10000)),
            cards=listOf(WalletCard(name="Kartu Uji",lastFour="1234")),notes=listOf(FinanceNote(title="Catatan Uji",body="Isi uji")),
            reimbursements=listOf(Reimbursement("qa-tx","qa-tx","Klaim Uji","Kantor Uji",1000,"IDR","qa-cash")),
            recurring=listOf(RecurringTransaction(title="Internet Uji",amountMinor=1000,accountId="qa-cash",categoryId="bills")))
        val actions=PlanningActions({opened="bill"},{opened="pay-bill"},{opened="debt"},{opened="pay-debt"},
            {opened="wish"},{opened="wish-status"},{opened="card"},{opened="note"},{opened="claim"},{opened="receive"},
            {opened="recurring"},{opened="process"},{opened="calendar"},{opened="expense"},{opened="reverse"})
        ui.setContent { FinFlowTheme { key(page){PlanningScreen(page,data,false,actions)} } }
        listOf("Tagihan" to "Listrik Uji","Utang" to "Peminjam Uji","Keinginan" to "Kamera Uji","Kartu" to "Kartu Uji","Catatan" to "Catatan Uji","Reimburse" to "Klaim Uji","Terjadwal" to "Internet Uji").forEach{(destination,title)->
            ui.runOnIdle{page=destination}
            if(destination=="Catatan")ui.onNodeWithText("Catatan",false).performClick()
            ui.onNodeWithText(title).assertExists();screenshot("planning-$destination")
        }
        ui.onNodeWithText("Proses Jadwal").performClick();ui.runOnIdle{assertEquals("process",opened)}
        ui.runOnIdle{page="Tagihan"};ui.onNodeWithText("Bayar Tagihan").performClick();ui.runOnIdle{assertEquals("pay-bill",opened)}
        ui.runOnIdle{page="Utang"};ui.onNodeWithText("Bayar Cicilan").performScrollTo().performClick();ui.runOnIdle{assertEquals("pay-debt",opened)}
        ui.runOnIdle{page="Reimburse"};ui.onNodeWithText("Terima Reimburse").performClick();ui.runOnIdle{assertEquals("receive",opened)}
    }

    @Test fun cardAndPaymentFormsRejectUnsafeInputAndReturnValidPayload() {
        var card:WalletCard?=null
        var showPayment by mutableStateOf(false)
        var payment:PaymentRequest?=null
        ui.setContent { FinFlowTheme {
            if(!showPayment) CardEditor(null,fixture,false,{}, {card=it;showPayment=true},null)
            else PaymentEditor("Bayar Cicilan","IDR",10000,fixture,"qa-cash",false,{}, {payment=it},partial=true)
        } }
        ui.onNodeWithText("Bank",false).performScrollTo().performClick()
        ui.onNodeWithText("Kartu",false).performClick()
        ui.onNodeWithContentDescription("Nama kartu").performTextInput("Kartu Aman")
        ui.onNodeWithContentDescription("4 digit terakhir").performScrollTo().performTextInput("123")
        ui.onNodeWithText("Simpan",false).performClick()
        ui.onNodeWithTag("editor-error").assertExists();ui.runOnIdle{assertNull(card)}
        ui.onNodeWithContentDescription("4 digit terakhir").performScrollTo().performTextReplacement("1234")
        screenshot("card-editor")
        ui.onNodeWithText("Simpan",false).performClick()
        ui.runOnIdle{assertEquals("1234",card?.lastFour)}
        ui.onNodeWithContentDescription("Nominal (IDR)").performTextReplacement("101")
        ui.onNodeWithText("Simpan",false).performClick();ui.runOnIdle{assertNull(payment)}
        ui.onNodeWithContentDescription("Nominal (IDR)").performTextReplacement("40")
        screenshot("payment-editor")
        ui.onNodeWithText("Simpan",false).performClick()
        ui.runOnIdle{assertEquals(4000L,payment?.amountMinor);assertEquals("qa-cash",payment?.accountId)}
    }

    @Test fun transactionKeypadStaysAttachedValidatesAndSavesOnlyToLocalCallback() {
        var result: Transaction? = null
        var open by mutableStateOf(true)
        ui.setContent { FinFlowTheme {
            Text("Halaman di belakang panel")
            if (open) TransactionEditor(null, fixture, false, { open = false }, { result = it }, null)
        } }
        ui.onNodeWithTag("key-confirm").assertIsDisplayed()
        screenshot("transaction-expense")
        val keypadNode = ui.onNodeWithTag("transaction-keypad").fetchSemanticsNode()
        val screenHeight = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.heightPixels
        ui.runOnIdle {
            val keypad = keypadNode.boundsInWindow
            assertTrue("Keypad must stay inside the screen: height=$screenHeight keypad=$keypad", keypad.bottom <= screenHeight + 1f)
            assertTrue("Only the system navigation area may be below the keypad: height=$screenHeight keypad=$keypad", screenHeight - keypad.bottom < 160f)
        }
        ui.onNodeWithText("Pemasukan", substring = false).performClick()
        screenshot("transaction-income")
        ui.onNodeWithText("Transfer", substring = false).performClick()
        screenshot("transaction-transfer")
        ui.onNodeWithText("Pengeluaran", substring = false).performClick()
        ui.onNodeWithTag("key-confirm").performClick()
        ui.onNodeWithTag("transaction-form").performScrollToNode(hasTestTag("transaction-error"))
        ui.onNodeWithTag("transaction-error").assertIsDisplayed()
        ui.runOnIdle { assertNull(result) }
        ui.onNodeWithTag("key-1").performClick()
        ui.onNodeWithTag("key-2").performClick()
        ui.onNodeWithTag("key-3").performClick()
        ui.onNodeWithTag("transaction-amount").assertTextEquals("123")
        ui.onNodeWithTag("transaction-form").performScrollToNode(hasTestTag("transaction-title"))
        ui.onNodeWithTag("transaction-title").performTextInput("Transaksi Uji")
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        ui.waitForIdle()
        ui.onNodeWithTag("key-confirm").assertIsDisplayed().performClick()
        ui.runOnIdle {
            assertNotNull(result)
            assertEquals(12300L, result!!.amountMinor)
            assertEquals("Transaksi Uji", result!!.title)
        }
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        ui.waitForIdle()
        ui.onNodeWithTag("transaction-sheet").assertDoesNotExist()
    }

    @Test fun editorsUseFullPageAndKeepValidation() {
        var editor by mutableStateOf("wallet")
        ui.setContent { FinFlowTheme {
            when(editor) {
                "wallet" -> AccountEditor(null, fixture, false, {}, {})
                "budget" -> BudgetEditor(null, fixture, false, {}, {}, null)
                "goal" -> GoalEditor(null, false, {}, {}, null)
            }
        } }
        ui.onNodeWithTag("editor-page").assertIsDisplayed()
        screenshot("wallet-editor")
        ui.runOnIdle { editor = "budget" }
        ui.onNodeWithTag("budget-editor").assertExists()
        ui.onNodeWithTag("budget-save").performClick()
        ui.onNodeWithTag("budget-error").assertExists()
        screenshot("budget-editor")
        ui.runOnIdle { editor = "goal" }
        ui.onNodeWithContentDescription("Target Dana").assertExists()
        screenshot("goal-editor")
    }

    @Test fun budgetGoalsCalendarAndTransactionDetailRenderRealValues() {
        var screen by mutableStateOf("budget")
        ui.setContent { FinFlowTheme {
            Column(Modifier.fillMaxSize()) {
                when(screen) {
                    "budget" -> BudgetScreen(fixture, month, {}, {}, {})
                    "goals" -> GoalScreen(fixture, {}, {})
                    "goal-detail" -> GoalDetail(fixture.goals.first(), {})
                    "calendar" -> ScheduledScreen(fixture.copy(transactions = fixture.transactions.map { it.copy(scheduled = true) }), month, {}, {}, {})
                    "detail" -> TransactionDetail(fixture.transactions.first(), fixture, {}, {}, false)
                }
            }
        } }
        ui.onNodeWithText("Terpakai").assertExists()
        screenshot("budget-overview")
        ui.runOnIdle { screen = "goals" }
        ui.onNodeWithText("Total Tabungan").assertExists()
        screenshot("goals-overview")
        ui.runOnIdle { screen = "goal-detail" }
        ui.onNodeWithText("Laptop Baru").assertExists()
        ui.onNodeWithText("Tambah Dana").assertExists()
        screenshot("goal-detail")
        ui.runOnIdle { screen = "calendar" }
        ui.onNodeWithText("1 transaksi terjadwal").assertExists()
        screenshot("calendar")
        ui.runOnIdle { screen = "detail" }
        ui.onNodeWithText("Makan Siang Uji").assertExists()
        screenshot("transaction-detail")
    }

    @Test fun balanceGradientMovesWithoutChangingDataAndPeriodLabelsDropIni() {
        ui.mainClock.autoAdvance = false
        try {
            // Preset "teal" kini statis seperti kartu Ollo; animasi neon tetap diuji pada preset lain.
            ui.setContent { CompositionLocalProvider(LocalPreferences provides AppPreferences(cardGradient="sunset")) { FinFlowTheme { Dashboard(fixture,month,{},{},"Pengguna Uji",{}) } } }
            ui.mainClock.advanceTimeByFrame()
            ui.waitForIdle()
            listOf("Hari","Minggu","Bulan","Tahun","Kalender").forEach { ui.onNodeWithText(it,false).assertExists() }
            listOf("Hari Ini","Minggu Ini","Bulan Ini").forEach { ui.onNodeWithText(it,false).assertDoesNotExist() }
            val node=ui.onNodeWithTag("balance-card-IDR")
            val bounds=node.fetchSemanticsNode().boundsInWindow
            val first=node.captureToImage().toPixelMap()
            val samples=listOf(.2f,.35f,.65f,.8f).map { fraction ->
                val x=(first.width*fraction).toInt()
                x to first[x,(first.height*.04f).toInt()]
            }
            ui.mainClock.advanceTimeBy(3500)
            ui.waitForIdle()
            val second=node.captureToImage().toPixelMap()
            assertEquals(bounds,node.fetchSemanticsNode().boundsInWindow)
            val changed=samples.count { (x,before) ->
                val after=second[x,(second.height*.04f).toInt()]
                abs(before.red-after.red)+abs(before.green-after.green)+abs(before.blue-after.blue)>.06f
            }
            assertTrue("The neon background should change at fixed uncovered points after a quarter cycle: "+samples.map{(x,c)->"$x:$c->${second[x,(second.height*.04f).toInt()]}"},changed>=2)
            ui.onNodeWithText("Total Saldo (IDR)",false).assertIsDisplayed()
        } finally { ui.mainClock.autoAdvance = true }
    }

    @Test fun menuAndPrivacyPreferencesApplyAndSurviveReloadPerUser() {
        val native=FakeNative()
        var store by mutableStateOf(PreferenceState(native,"preferences-qa"))
        var screen by mutableStateOf("Tampilan Menu")
        var destination:String?=null
        ui.setContent {
            CompositionLocalProvider(LocalPreferenceState provides store,LocalPreferences provides store.value) {
                FinFlowTheme {
                    if(screen=="dashboard")Dashboard(fixture,month,{},{},"Pengguna Uji",{destination=it})
                    else PreferenceScreen(screen,fixture)
                }
            }
        }
        ui.onNodeWithText("Geser horizontal",false).performClick()
        ui.onNodeWithText("Grid",false).performClick()
        ui.runOnIdle { assertEquals("grid",store.value.menuAppearance);screen="Susunan Menu" }
        ui.onNodeWithTag("menu-visible-Anggaran").performClick()
        ui.onNodeWithContentDescription("Turun Anggaran").performClick()
        ui.runOnIdle {
            assertTrue("Anggaran" in store.value.hiddenMenus)
            assertEquals("Berulang",store.value.menuOrder.first())
            screen="Format Saldo"
        }
        ui.onNodeWithTag("preferences-list").performScrollToNode(hasText("Sembunyikan saldo saat dibuka"))
        ui.onNodeWithTag("pref-hide-balance").performClick()
        ui.runOnIdle {
            assertTrue(store.value.hideBalances)
            val saved=store.value
            store=PreferenceState(native,"preferences-qa")
            assertEquals(saved,store.value)
            assertEquals("horizontal",PreferenceState(native,"different-user").value.menuAppearance)
            screen="dashboard"
        }
        ui.onNodeWithTag("balance-card-IDR").assert(hasAnyDescendant(hasText("••••••")))
        ui.onNodeWithTag("dashboard").performScrollToNode(hasTestTag("dashboard-menu-more"))
        // Mode grid menampilkan semua menu; Anggaran tersembunyi sesuai preferensi.
        ui.onNodeWithText("Anggaran",false).assertDoesNotExist()
        ui.onNodeWithText("Investasi",false).assertExists()
        ui.onNodeWithText("Target",false).performScrollTo().performClick()
        ui.runOnIdle { assertEquals("Target",destination) }
        screenshot("preferences-grid-hidden-budget")
    }

    @Test fun categoryAndSubcategoryEditorsKeepJournalIdentity() {
        val child=Category("qa-breakfast","Sarapan Uji","expense",parentId="food",icon="restaurant",color=0xFFFF6B00)
        val data=fixture.copy(categories=fixture.categories+child)
        var selected:Category? by mutableStateOf(null)
        var saved:Category?=null
        ui.setContent { FinFlowTheme {
            if(selected==null)CategoryScreen(data,false,{selected=it})
            else CategoryEditor(false,{selected=null},{saved=it},old=selected,data=data)
        } }
        // Kategori Ollo dimulai tertutup: buka "food" dulu agar subkategori terlihat.
        ui.onNodeWithTag("category-expand-food").performScrollTo().performClick()
        ui.onNodeWithText("Sarapan Uji",false).performScrollTo().performClick()
        ui.onNodeWithContentDescription("Nama kategori").performTextReplacement("Sarapan Kantor")
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        ui.onNodeWithTag("category-icon-home").performScrollTo().performClick()
        ui.onNodeWithTag("category-color-Biru").performScrollTo().performClick()
        ui.onNodeWithText("Simpan",false).performClick()
        ui.runOnIdle {
            assertEquals(child.id,saved?.id)
            assertEquals("food",saved?.parentId)
            assertEquals("expense",saved?.type)
            assertEquals("Sarapan Kantor",saved?.name)
            assertEquals("home",saved?.icon)
            assertEquals(0xFF3B82F6L,saved?.color)
        }
        screenshot("subcategory-editor")
    }

    @Test fun statisticsFiltersExportOnlySelectedWalletAndRespectSectionOrder() {
        val native=FakeNative()
        val store=PreferenceState(native,"statistics-qa")
        store.update(store.value.copy(statisticsOrder=listOf("transactions","categories","cycle","trend","comparison")))
        val data=fixture.copy(accounts=fixture.accounts+listOf(Account(id="qa-second",name="Dompet Kedua"),Account(id="qa-excluded",name="Dompet Privat",excludedFromTotal=true)),
            transactions=fixture.transactions+listOf(
                fixture.transactions.first().copy(id="qa-second-tx",accountId="qa-second",amountMinor=1_000_000,title="Pengeluaran Kedua"),
                fixture.transactions.first().copy(id="qa-private-tx",accountId="qa-excluded",amountMinor=123_000_000,title="Pengeluaran Privat")))
        var exported:List<Transaction>?=null
        ui.setContent { CompositionLocalProvider(LocalPreferenceState provides store,LocalPreferences provides store.value) {
            FinFlowTheme { Reports(data,month,{},{},exportFiltered={exported=it}) }
        } }
        ui.onNodeWithTag("statistics-total").assertTextEquals(formatBalance(5_500_000,"IDR",store.value))
        ui.onNodeWithTag("statistics-filters").performClick()
        ui.onNodeWithText("Semua dompet",false).performClick()
        ui.onNodeWithText("Dompet Uji",false).performClick()
        ui.onNodeWithText("Terapkan",false).performClick()
        ui.onNodeWithTag("statistics-total").assertTextEquals(formatBalance(4_500_000,"IDR",store.value))
        ui.onNodeWithTag("statistics-menu").performClick()
        ui.onNodeWithText("Ekspor CSV/PDF").performClick()
        ui.runOnIdle { assertEquals(listOf("qa-tx"),exported?.map{it.id}) }
        ui.onNodeWithTag("statistics").performScrollToIndex(4) // period, type, overview, daily, lalu bagian berurutan
        ui.onNodeWithTag("statistics-transactions").assertIsDisplayed()
        val journal=ui.onNodeWithTag("statistics-transactions").fetchSemanticsNode().boundsInWindow
        val categories=ui.onNodeWithTag("statistics-categories").fetchSemanticsNode().boundsInWindow
        assertTrue("Configured journal section must appear before category section",journal.top<categories.top)
        screenshot("statistics-filtered-reordered")
    }

    @Test fun accountDeletionNeedsExactConfirmationAndCancelledGoogleDoesNotDelete() {
        var deleted=false
        val native=object:NativeServices by FakeNative(){
            override fun googleSignIn(result:(String?,String?)->Unit){result(null,"Konfirmasi Google dibatalkan")}
        }
        ui.setContent { FinFlowTheme { DeleteAccountDialog(native,true,false,{}, {_,_->deleted=true}) } }
        ui.onNodeWithText("Konfirmasi dengan Google").assertIsNotEnabled()
        ui.onNodeWithContentDescription("Ketik HAPUS").performTextInput("hapus")
        ui.onNodeWithText("Konfirmasi dengan Google").assertIsNotEnabled()
        ui.onNodeWithContentDescription("Ketik HAPUS").performTextReplacement("HAPUS")
        ui.onNodeWithText("Konfirmasi dengan Google").assertIsEnabled().performClick()
        ui.onNodeWithText("Konfirmasi Google dibatalkan").assertIsDisplayed()
        ui.runOnIdle { assertFalse(deleted) }
    }

    private fun screenshot(name: String) {
        ui.mainClock.advanceTimeBy(500)
        ui.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "design-qa").apply { mkdirs() }
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        checkNotNull(bitmap) { "Device screenshot unavailable" }
        FileOutputStream(File(directory, "$name.png")).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}

private class FakeNative : NativeServices {
    private val settings = mutableMapOf<String,String>()
    override fun readSetting(key: String): String? = settings[key]
    override fun writeSetting(key: String, value: String) { settings[key] = value }
    override fun onboardingComplete() = false
    override fun setOnboardingComplete() {}
    override fun setNotificationImportUser(userId: String?) {}
    override fun pickCsv(result: (String?, String?) -> Unit) {}
    override fun shareCsv(name: String, content: String) {}
    override fun googleSignIn(result: (String?, String?) -> Unit) {}
    override fun unlock(result: (Boolean) -> Unit) { result(true) }
    override fun lockEnabled() = false
    override fun setLockEnabled(enabled: Boolean) {}
    override fun reminderEnabled() = false
    override fun reminderHour() = 20
    override fun reminderMinute() = 0
    override fun setReminder(enabled: Boolean, hour: Int, minute: Int, result: (String?) -> Unit) { result(null) }
    override fun budgetAlertsEnabled() = false
    override fun setBudgetAlerts(enabled: Boolean, result: (String?) -> Unit) { result(null) }
    override fun showBudgetAlert(userId: String, alertId: String, title: String, message: String) {}
}

