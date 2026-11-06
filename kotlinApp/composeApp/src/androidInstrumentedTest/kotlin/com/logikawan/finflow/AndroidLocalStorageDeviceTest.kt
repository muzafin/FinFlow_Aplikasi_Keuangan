package com.logikawan.finflow

import android.content.Context
import android.os.Process
import androidx.test.platform.app.InstrumentationRegistry
import com.logikawan.finflow.core.data.FinanceRepository
import com.logikawan.finflow.core.model.Transaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters
import org.junit.Test
import java.io.File

// Run the two methods in separate instrumentations on the isolated work-profile copy only.
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class AndroidLocalStorageDeviceTest {
    private fun fixtureContext(): Context {
        assumeTrue("Native storage fixtures require the isolated work-profile copy", Process.myUid() / 100000 == 10)
        return InstrumentationRegistry.getInstrumentation().targetContext
    }
    private fun store(context: Context) = File(context.noBackupFilesDir, "finflow-local/finance-v1.aes")

    @Test fun createEncryptedFixture() = runBlocking {
        val context = fixtureContext()
        assertFalse("Existing local data must never be replaced by this fixture", store(context).exists())
        val persistence = AndroidLocalFinancePersistence.get(context)
        assertNull(persistence.load().content)
        val repo = FinanceRepository(local = persistence)
        repo.initializeProfile("UJI_NATIVE_LOCAL", "IDR", "Kas Uji Native", 20000)
        repo.saveTransaction(Transaction("native-fixture", "expense", 1000, "IDR", "acc_cash", "food", "UJI_NATIVE_EXPENSE"))
        val loaded = persistence.load()
        assertNull(loaded.error)
        assertTrue(loaded.content!!.contains("UJI_NATIVE_LOCAL"))
        val raw = store(context).readBytes()
        assertTrue(raw.copyOfRange(0, 8).contentEquals("FFLOCAL1".toByteArray()))
        assertFalse(raw.toString(Charsets.UTF_8).contains("UJI_NATIVE"))
    }

    @Test fun reopenAfterProcessRestartRejectFreezeAndCorruptionThenCleanFixture() = runBlocking {
        val context = fixtureContext()
        val persistence = AndroidLocalFinancePersistence.get(context)
        val repo = FinanceRepository(local = persistence)
        val data = repo.observe().first()
        assertEquals("UJI_NATIVE_LOCAL", data.profile?.name)
        assertEquals("native-fixture", data.transactions.single().id)
        assertEquals(19000L, data.balance(data.accounts.single()))
        val file = store(context)
        val original = file.readBytes()
        val settings = context.getSharedPreferences("finflow", Context.MODE_PRIVATE)
        val frozen = settings.getString("local.sync.frozen", null)
        try {
            assertTrue(settings.edit().putString("local.sync.frozen", "true").commit())
            try { repo.saveTransaction(data.transactions.single().copy(title = "Tidak boleh tersimpan")); fail("Frozen writes must fail") }
            catch (_: IllegalStateException) {}
            assertArrayEquals(original, file.readBytes())
            settings.edit().remove("local.sync.frozen").commit()
            val damaged = original.clone().apply { this[lastIndex] = (this[lastIndex].toInt() xor 1).toByte() }
            file.writeBytes(damaged)
            assertNotNull(persistence.load().error)
            try { repo.initializeProfile(); fail("Corrupt storage must not be treated as empty") }
            catch (_: IllegalStateException) {}
            assertArrayEquals(damaged, file.readBytes())
        } finally {
            file.writeBytes(original)
            settings.edit().apply { if (frozen == null) remove("local.sync.frozen") else putString("local.sync.frozen", frozen) }.commit()
        }
        assertNull(persistence.load().error)
        assertTrue("Only the fixture created by the first test is removed", file.delete())
    }
}
