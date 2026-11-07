package com.logikawan.finflow

import com.logikawan.finflow.core.i18n.withLocalizedDefaults
import com.logikawan.finflow.core.model.*
import kotlin.test.*

class LocalizedDefaultsTest {
    @Test fun untouchedDefaultNamesFollowActiveLanguageButRenamedOnesStay() {
        // Unit test membaca teks dasar (Indonesia): nama bawaan dari bahasa lain tampil sebagai Indonesia.
        val data = FinanceData(
            accounts = listOf(Account(id = "acc_cash", name = "Cash", type = "cash"), Account(id = "a2", name = "Tabungan Rumah", type = "bank")),
            categories = listOf(
                Category("salary", "Salary", "income"), Category("food", "飲食", "expense"),
                Category("transport", "Ojek kantor", "expense"), Category("custom", "Gaji", "income"),
            ),
        )
        val shown = data.withLocalizedDefaults()
        assertEquals("Kas", shown.accounts[0].name)
        assertEquals("Tabungan Rumah", shown.accounts[1].name)
        assertEquals("Gaji", shown.categories[0].name)
        assertEquals("Makan & minum", shown.categories[1].name)
        assertEquals("Ojek kantor", shown.categories[2].name, "nama yang diganti pengguna tidak boleh diubah")
        assertEquals("Gaji", shown.categories[3].name)
        assertEquals(data.transactions, shown.transactions)
    }

    @Test fun providerCatalogIsConsistent() {
        assertEquals(financeProviders.size, financeProviders.map { it.id }.toSet().size)
        assertTrue(financeProviders.all { it.type in listOf("bank", "ewallet") && it.short.length in 1..4 && it.color in 0xFF000000..0xFFFFFFFF })
        assertTrue(financeProviders.all { (PROVIDER_PREFIX + it.id).length <= 100 })
        assertEquals("BCA", providerOf("provider:bca")?.name)
        assertNull(providerOf("account_balance"))
        assertNull(providerOf("provider:tidak-ada"))
    }
}
