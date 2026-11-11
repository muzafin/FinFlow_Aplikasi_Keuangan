package com.clonical.kasiva

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.clonical.kasiva.core.model.*
import com.clonical.kasiva.core.platform.NativeServices
import com.clonical.kasiva.core.ui.FFColors
import com.clonical.kasiva.core.ui.KasivaTheme
import com.clonical.kasiva.core.util.today
import com.clonical.kasiva.feature.settings.ProfileScreen
import com.clonical.kasiva.feature.settings.BugReportForm
import com.clonical.kasiva.feature.level.LevelScreen
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Membandingkan halaman Profil Kasiva dengan susunan Ollo memakai data contoh terisolasi. */
class ProfileParityDeviceTest {
    @get:Rule val ui = createComposeRule()

    private fun fixture(): FinanceData {
        val day = today()
        fun tx(id: String, type: String, amount: Long, daysAgo: Int, category: String, title: String) =
            Transaction(id = id, type = type, amountMinor = amount, currency = "IDR", accountId = "prof-cash",
                categoryId = category, title = title, date = day.plus(DatePeriod(days = -daysAgo)).toString(),
                createdAt = day.toEpochDays().toLong() * 86_400_000L + 36_000_000L - daysAgo * 86_400_000L)
        return FinanceData(
            profile = Profile("profile-qa", "Muhammad", "muhammad@example.invalid"),
            accounts = listOf(Account(id = "prof-cash", name = "Cash"), Account(id = "prof-bank", name = "BCA", type = "bank", openingBalanceMinor = 2_000_000)),
            categories = defaultCategories,
            transactions = listOf(
                tx("prof-1", "income", 2_000_000, 2, "salary", "Gaji November"),
                tx("prof-2", "expense", 45_000, 1, "food", "Makan siang"),
                tx("prof-3", "expense", 25_000, 0, "transport", "Ojek online"),
            ),
            budgets = listOf(Budget("prof-budget", "food", 1_500_000, "IDR", day.toString().take(7))),
        )
    }

    @Test fun profileFollowsOlloStructureInLightTheme() {
        var nav: String? = null
        var loggedOut = false
        val data = fixture()
        val native = FakeNative()
        ui.setContent {
            CompositionLocalProvider(LocalPreferences provides AppPreferences(theme = "light")) {
                KasivaTheme {
                    Box(Modifier.fillMaxSize().background(FFColors.background)) {
                        ProfileScreen(native, "Muhammad", "muhammad@example.invalid", data, { nav = it }, { loggedOut = true })
                    }
                }
            }
        }
        ui.mainClock.advanceTimeBy(600)
        ui.waitForIdle()
        ui.onNodeWithTag("profile-list").assertExists()
        ui.onNodeWithText("Profil").assertIsDisplayed()
        ui.onNodeWithText("Muhammad").assertIsDisplayed()
        ui.onNodeWithText("LEVEL").assertExists()
        ui.onNodeWithText("STREAK").assertExists()
        ui.onNodeWithText("HARI AKTIF").assertExists()
        ui.onNodeWithText("Akun & Data").assertExists()
        ui.onNodeWithText("Manajemen Akun").assertExists()
        ui.onNodeWithText("Cadangan & Pemulihan").assertExists()
        ui.onNodeWithText("Perangkat Terhubung").assertExists()
        ui.onNodeWithText("Ekspor Data").assertExists()
        ui.onNodeWithText("Upgrade ke Premium").assertDoesNotExist()
        screenshot("profile-ollo-light")

        ui.onNodeWithText("Manajemen Akun").performClick()
        ui.runOnIdle { assertEquals("Manajemen Akun", nav); assertFalse(loggedOut) }

        ui.onNodeWithTag("profile-list").performScrollToNode(hasText("Preferensi"))
        ui.mainClock.advanceTimeBy(300)
        ui.waitForIdle()
        ui.onNodeWithText("Preferensi").assertExists()
        // Menu Profil dikelompokkan: "Format Saldo" ada di grup "Tampilan & Format".
        ui.onNodeWithText("Format Saldo").assertDoesNotExist()
        ui.onNodeWithTag("profile-list").performScrollToNode(hasText("Tampilan & Format"))
        ui.mainClock.advanceTimeBy(300)
        ui.waitForIdle()
        screenshot("profile-ollo-light-bottom")
        ui.onNodeWithText("Tampilan & Format").performClick()
        ui.runOnIdle { assertEquals(com.clonical.kasiva.feature.settings.PROFILE_GROUP_PREFIX + "display", nav) }
        ui.onNodeWithTag("profile-list").performScrollToNode(hasText("Hapus Data"))
        ui.mainClock.advanceTimeBy(300)
        ui.waitForIdle()
        screenshot("profile-ollo-light-help")
    }

    @Test fun profileKeepsOlloLayoutInDarkTheme() {
        val data = fixture()
        val native = FakeNative()
        ui.setContent {
            CompositionLocalProvider(LocalPreferences provides AppPreferences(theme = "dark")) {
                KasivaTheme {
                    Box(Modifier.fillMaxSize().background(FFColors.background)) {
                        ProfileScreen(native, "Muhammad", "muhammad@example.invalid", data, {}, {})
                    }
                }
            }
        }
        ui.mainClock.advanceTimeBy(600)
        ui.waitForIdle()
        ui.onNodeWithTag("profile-list").assertExists()
        ui.onNodeWithText("Profil").assertIsDisplayed()
        ui.onNodeWithText("Muhammad").assertIsDisplayed()
        ui.onNodeWithText("LEVEL").assertExists()
        ui.onNodeWithText("STREAK").assertExists()
        ui.onNodeWithText("HARI AKTIF").assertExists()
        screenshot("profile-ollo-dark")
    }

    private fun screenshot(name: String) {
        ui.mainClock.advanceTimeBy(500)
        ui.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        android.os.SystemClock.sleep(350)
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "profile-qa").apply { mkdirs() }
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        checkNotNull(bitmap) { "Device screenshot unavailable" }
        FileOutputStream(File(directory, name + ".png")).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun everyProfileMenuNavigatesAndPaletteAndSearchWork() {
        val routes = mutableListOf<String>()
        val data = fixture()
        val native = FakeNative()
        val store = PreferenceState(native, "profile-menu-qa")
        // null = tab Profil; selain itu id halaman grup yang sedang dibuka.
        var group by mutableStateOf<String?>(null)
        ui.setContent {
            CompositionLocalProvider(LocalPreferenceState provides store, LocalPreferences provides store.value.copy(theme = "dark")) {
                KasivaTheme {
                    Box(Modifier.fillMaxSize().background(FFColors.background)) {
                        val shown = group
                        if (shown == null) ProfileScreen(native, "Muhammad", "muhammad@example.invalid", data, { routes += it }, {})
                        else com.clonical.kasiva.feature.settings.ProfileGroupScreen(shown) { routes += it }
                    }
                }
            }
        }
        ui.waitForIdle()
        val prefix = com.clonical.kasiva.feature.settings.PROFILE_GROUP_PREFIX
        // Tab Profil: ubin utama, baris grup, dan baris umum/bahaya membuka rutenya masing-masing.
        val expected = listOf("Manajemen Akun" to "Manajemen Akun", "Cadangan & Pemulihan" to "Cadangan & Pemulihan", "Perangkat Terhubung" to "Perangkat Terhubung",
            "Ekspor Data" to "Ekspor Data", "Tema" to "Tema", "Tampilan Kartu" to "Tampilan Kartu", "Tampilan Menu" to "Tampilan Menu",
            "Palet Transaksi" to "Palet Transaksi", "Data & Dompet" to prefix + "data", "Tampilan & Format" to prefix + "display", "AI" to prefix + "ai",
            "Fitur Masa Mendatang" to prefix + "future", "Bantuan & Info" to prefix + "help",
            "Pengaturan" to "Pengaturan", "Hapus Data" to "Hapus Data", "Keluar Aplikasi" to "Keluar Aplikasi")
        expected.forEach { (title, route) ->
            ui.onNodeWithTag("profile-list").performScrollToNode(hasText(title))
            ui.onNodeWithText(title).performClick()
            ui.runOnIdle { assertEquals("$title -> $route", route, routes.last()) }
        }
        // Urutan bagian: Akun & Data, Preferensi, Menu Lainnya (grup), Umum, Lainnya; versi paling bawah.
        listOf("Akun & Data", "Preferensi", "Menu Lainnya", "Umum", "Lainnya").forEach {
            ui.onNodeWithTag("profile-list").performScrollToNode(hasText(it))
            ui.onNodeWithText(it).assertExists()
        }
        ui.onNodeWithTag("profile-list").performScrollToNode(hasTestTag("profile-version"))
        ui.onNodeWithText("Kasiva v1.0.0").assertExists()
        // Setiap halaman grup berisi menu yang dipindahkan dari tab Profil.
        val groups = mapOf(
            "data" to listOf("Impor Data" to "Impor data", "Periode Pencatatan" to "Periode Pencatatan", "Kategori" to "Kategori",
                "Pengaturan Dompet" to "Pengaturan Dompet", "Notifikasi" to "Notifikasi"),
            "display" to listOf("Urutan Statistik" to "Urutan Statistik", "Tampilan Beranda" to "Tampilan Beranda",
                "Tampilan Tambah Transaksi" to "Tampilan Tambah Transaksi", "Paket Ikon" to "Paket Ikon", "Format Saldo" to "Format Saldo"),
            "ai" to listOf("Asisten Kasiva (Beta)" to "Asisten", "Pengaturan Asisten" to "Pengaturan Asisten", "Pengaturan AI Scan" to "Pengaturan AI Scan",
                "Memori Pencatatan" to "Memori Pencatatan", "Bahasa Input Suara" to "Bahasa Input Suara"),
            "future" to listOf("Otomasi & Integrasi" to "Otomasi", "Masukan & Peta Jalan" to "Masukan & Peta Jalan"),
            "help" to listOf("Bantuan & Dukungan" to "Bantuan", "Kirim Masukan" to "Masukan", "Bagikan Kasiva ke Teman" to "Bagikan",
                "Laporkan Bug" to "Laporkan Bug", "Tentang Kasiva" to "Tentang", "Catatan Pembaruan" to "Catatan Pembaruan"),
        )
        groups.forEach { (id, items) ->
            ui.runOnIdle { group = prefix + id }
            ui.waitForIdle()
            items.forEach { (title, route) ->
                ui.onNodeWithTag("profile-group-$id").performScrollToNode(hasText(title))
                ui.onNodeWithText(title).performClick()
                ui.runOnIdle { assertEquals("$title -> $route", route, routes.last()) }
            }
        }
        ui.runOnIdle { group = null }
        ui.waitForIdle()
        // Avatar membuka editor profil, statistik membuka halaman level.
        ui.onNodeWithTag("profile-list").performScrollToIndex(0)
        ui.onNodeWithTag("profile-avatar").performClick()
        ui.runOnIdle { assertEquals("Profil akun", routes.last()) }
        ui.onNodeWithTag("profile-level-link").performClick()
        ui.runOnIdle { assertEquals("Aktivitas", routes.last()) }
        ui.onNodeWithTag("profile-settings").performClick()
        ui.runOnIdle { assertEquals("Pengaturan", routes.last()) }
        // Palet: pratinjau mini-layar mengubah preferensi.
        ui.onNodeWithTag("profile-list").performScrollToNode(hasTestTag("palette-ruby"))
        ui.onNodeWithTag("palette-ruby").performClick()
        ui.runOnIdle { assertEquals("ruby", store.value.palette) }
        ui.onNodeWithTag("palette-neutral").performClick()
        // Pencarian menyaring daftar.
        ui.onNodeWithTag("profile-list").performScrollToIndex(0)
        ui.onNodeWithContentDescription("Cari pengaturan").performClick()
        ui.onNodeWithTag("profile-list").performScrollToIndex(0)
        ui.onAllNodes(hasSetTextAction()).onFirst().performTextInput("Bug")
        ui.waitForIdle()
        ui.onNodeWithText("Laporkan Bug").assertExists()
        ui.onNodeWithText("Manajemen Akun").assertDoesNotExist()
        ui.onNodeWithTag("profile-search-field").performTextClearance()
        ui.onNodeWithTag("profile-search-field").performTextInput("Bahasa")
        ui.waitForIdle()
        ui.onNodeWithTag("profile-item-bahasa").performClick()
        ui.runOnIdle { assertEquals("Bahasa", routes.last()) }
    }

    @Test fun bugReportShowsDeviceInformationBeforeSending() {
        ui.setContent { CompositionLocalProvider(LocalPreferences provides AppPreferences(theme="light")){KasivaTheme { Surface(color=FFColors.background){Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp)){BugReportForm(FakeNative())} } }} }
        ui.onNodeWithTag("bug-report-form").assertExists()
        ui.onNodeWithText("Informasi Perangkat").assertIsDisplayed()
        ui.onNodeWithText("Fitur yang bermasalah").assertExists()
        screenshot("profile-bug-report")
    }

    @Test fun levelPageShowsProgressAndMonthlyRewardRules() {
        ui.setContent { CompositionLocalProvider(LocalPreferences provides AppPreferences(theme="light")){KasivaTheme { Surface(color=FFColors.background){LevelScreen(fixture(),FakeNative(),{},{}) } }} }
        ui.onNodeWithTag("level-page").assertExists()
        ui.onNodeWithTag("level-back").assertExists()
        ui.onNodeWithTag("level-settings").assertExists()
        ui.onNodeWithText("Streak Terpanjang").assertExists()
        ui.onNodeWithText("Poin").assertExists()
        screenshot("profile-level-progress")
        ui.onNodeWithTag("level-page").performScrollToNode(hasText("Peringkat Bulanan (Segera Hadir)"))
        ui.onNodeWithText("Peringkat Bulanan (Segera Hadir)").assertIsDisplayed()
        ui.onNodeWithText("Juara 1 · Premium gratis 3 bulan").assertExists()
        ui.onNodeWithText("Juara 2 · Premium gratis 2 bulan").assertExists()
        ui.onNodeWithText("Juara 3 · Premium gratis 1 bulan").assertExists()
        ui.onNodeWithTag("level-leaderboard-disabled").assertExists()
        screenshot("profile-level-leaderboard")
    }

    private class FakeNative : NativeServices {
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
}
