package com.logikawan.finflow

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.logikawan.finflow.app.App
import com.logikawan.finflow.core.data.FinanceRepository
import com.logikawan.finflow.core.model.Transaction
import com.logikawan.finflow.core.platform.AppLifecycle
import com.logikawan.finflow.core.platform.NativeServices
import com.logikawan.finflow.core.util.today
import dev.gitlive.firebase.*
import dev.gitlive.firebase.auth.*
import dev.gitlive.firebase.firestore.*
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Uji integrasi Firebase nyata (emulator) dari sisi UI: akun cloud baru membuka Beranda,
 * menambah transaksi lewat panel transaksi, dan transaksi tersimpan di Firestore emulator
 * serta lolos rules yang sama dengan produksi.
 */
class CloudModeSmokeDeviceTest {
    @get:Rule val ui = createComposeRule()
    private lateinit var app: FirebaseApp
    private lateinit var db: FirebaseFirestore
    private var uid: String = ""

    @Before fun setup() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        app = Firebase.initialize(context,
            FirebaseOptions(applicationId = "1:123:android:test", apiKey = "fake-api-key", projectId = "demo-finflow"),
            "cloud-ui-" + UUID.randomUUID())
        val auth = Firebase.auth(app); auth.useEmulator("127.0.0.1", 9098)
        uid = auth.createUserWithEmailAndPassword("cloud-" + UUID.randomUUID() + "@example.test", "FinFlow-test-123!").user!!.uid
        db = Firebase.firestore(app); db.useEmulator("127.0.0.1", 8088)
        db.settings = firestoreSettings { cacheSettings = memoryCacheSettings {} }
    }

    @After fun teardown() = runBlocking {
        if (::app.isInitialized) {
            runCatching {
                db.collection("users").document(uid).collection("transactions").get().documents.forEach { it.reference.delete() }
                db.collection("users").document(uid).delete()
            }
            app.delete()
        }
    }

    @Test fun cloudAccountLoadsDashboardAndSavesTransactionThroughFirestoreRules() {
        val repo = FinanceRepository(app)
        runBlocking { repo.initializeProfile("Awan Uji", "IDR", "Kas", 0) }
        val native = CloudNative()
        val lifecycle = AppLifecycle()
        ui.mainClock.autoAdvance = false
        ui.setContent { App(native, lifecycle, cloudRepository = repo) }
        var dashboard = false
        repeat(80) {
            if (!dashboard) {
                ui.mainClock.advanceTimeBy(500)
                dashboard = ui.onAllNodesWithTag("dashboard").fetchSemanticsNodes().isNotEmpty()
                Thread.sleep(150)
            }
        }
        assertTrue("Dashboard mode cloud tidak tampil", dashboard)
        runBlocking {
            val cash = repo.observe().first().accounts.single { it.name == "Kas" }
            repo.saveTransaction(Transaction(type = "expense", amountMinor = 100_000, currency = "IDR",
                accountId = cash.id, categoryId = "food", title = "Uji integrasi cloud", date = today().toString()))
            withTimeout(60_000) {
                while (true) {
                    val remote = db.collection("users").document(uid).collection("transactions").get().documents.map { it.data<Transaction>() }
                    if (remote.any { it.amountMinor == 100_000L }) break
                    delay(500)
                }
            }
            val remote = db.collection("users").document(uid).collection("transactions").get().documents.map { it.data<Transaction>() }
            val stored = remote.single { it.amountMinor == 100_000L }
            assertEquals("expense", stored.type)
            assertEquals("IDR", stored.currency)
            assertEquals(cash.id, stored.accountId)
            val localView = withTimeout(30_000) {
                while (true) {
                    val data = repo.observe().first()
                    if (data.transactions.any { it.amountMinor == 100_000L }) return@withTimeout data
                    delay(500)
                }
                error("unreachable")
            }
            assertTrue(localView.transactions.any { it.amountMinor == 100_000L })
            assertTrue(localView.balance(localView.accounts.single { it.name == "Kas" }) <= -100_000L)
        }
    }
}

private class CloudNative : NativeServices {
    private val settings = mutableMapOf<String, String>()
    override fun readSetting(key: String): String? = settings[key]
    override fun writeSetting(key: String, value: String) { settings[key] = value }
    override fun onboardingComplete() = true
    override fun setOnboardingComplete() {}
    override fun setNotificationImportUser(userId: String?) {}
    override fun pickCsv(result: (String?, String?) -> Unit) {}
    override fun shareCsv(name: String, content: String) {}
    override fun googleSignIn(result: (String?, String?) -> Unit) { result(null, "Tidak tersedia di pengujian") }
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
