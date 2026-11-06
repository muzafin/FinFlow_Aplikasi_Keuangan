package com.logikawan.finflow

import com.logikawan.finflow.core.data.FinanceRepository
import com.logikawan.finflow.core.data.LocalFinanceLoad
import com.logikawan.finflow.core.data.LocalFinancePersistence
import com.logikawan.finflow.core.model.AppPreferences
import com.logikawan.finflow.core.model.PreferenceState
import com.logikawan.finflow.core.platform.NativeServices
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Memeriksa langkah penyimpanan setelah wizard pengaturan awal selesai, memakai
 * LocalFinancePersistence in-memory supaya data pengguna di perangkat tidak tersentuh.
 * Ini menutup celah "alur setup nyata" pada lapisan repository tanpa menghapus data HP.
 */
class LocalSetupPersistenceDeviceTest {
    private class MemoryPersistence : LocalFinancePersistence {
        var content: String? = null
        override val id: String = "setup-test-local"
        override fun load(): LocalFinanceLoad = LocalFinanceLoad(content)
        override fun save(content: String): String? { this.content = content; return null }
    }

    private class SetupNative : NativeServices {
        private val settings = mutableMapOf<String, String>()
        override fun readSetting(key: String): String? = settings[key]
        override fun writeSetting(key: String, value: String) { settings[key] = value }
        override fun onboardingComplete() = false
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
        override fun uploadProfilePhoto(userId: String, bytes: ByteArray, result: (String?, String?) -> Unit) {
            result("local/avatar.jpg", null)
        }
    }

    @Test fun completingSetupPersistsProfileWalletPreferencesAndPhoto() = runBlocking {
        val persistence = MemoryPersistence()
        val repo = FinanceRepository(local = persistence)
        val native = SetupNative()

        // 1) App.onComplete -> startLocal -> initializeProfile
        repo.initializeProfile("Pengguna Uji", "IDR", "Cash", 0)
        val data = repo.observe().first()
        assertEquals("Pengguna Uji", data.profile?.name)
        assertEquals("IDR", data.profile?.baseCurrency)
        val wallet = data.accounts.single { it.name == "Cash" }
        assertEquals("cash", wallet.type)
        assertEquals("IDR", wallet.currency)
        assertEquals(0L, data.balance(wallet))
        assertTrue("Kategori awal harus dibuat", data.categories.isNotEmpty())

        // 2) App.onComplete -> PreferenceState(native, "local").update(preferences)
        val draft = AppPreferences(
            theme = "dark", palette = "neutral", language = "id", cardGradient = "emerald",
            menuTheme = "ocean", addButtonMode = "all", incomeColor = 0xFF009688L, expenseColor = 0xFFFF9800L,
        )
        PreferenceState(native, "local").update(draft)
        val saved = PreferenceState(native, "local").value
        assertEquals("dark", saved.theme)
        assertEquals("emerald", saved.cardGradient)
        assertEquals("ocean", saved.menuTheme)
        assertEquals("all", saved.addButtonMode)
        assertEquals(0xFF009688L, saved.incomeColor)

        // 3) App.onComplete -> foto profil opsional setelah profil lokal ada
        repo.saveProfilePhoto(native, ByteArray(32) { it.toByte() })
        assertEquals("local/avatar.jpg", repo.observe().first().profile?.photoPath)

        // 4) Pengulangan initializeProfile (mis. proses terputus lalu dibuka ulang) tidak menggandakan data
        repo.initializeProfile("Pengguna Uji", "IDR", "Cash", 0)
        val again = repo.observe().first()
        assertEquals(1, again.accounts.count { it.name == "Cash" })
        assertEquals(1, again.profile?.let { 1 } ?: 0)
        assertTrue(again.categories.isNotEmpty())

        // 5) Snapshot tersimpan dan dapat dibuka kembali oleh repository baru (simulasi restart aplikasi)
        assertNotNull("Penyimpanan lokal harus menulis snapshot", persistence.content)
        val reopened = FinanceRepository(local = persistence).observe().first()
        assertEquals("Pengguna Uji", reopened.profile?.name)
        assertEquals("IDR", reopened.profile?.baseCurrency)
        assertTrue(reopened.accounts.any { it.name == "Cash" })
        assertEquals("local/avatar.jpg", reopened.profile?.photoPath)
    }
}
