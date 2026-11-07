package com.clonical.kasiva

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.clonical.kasiva.core.model.*
import com.clonical.kasiva.core.ui.KasivaTheme
import com.clonical.kasiva.feature.account.AccountEditor
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Dompet bank/e-wallet bisa memilih penyedia; badge merek disimpan sebagai icon `provider:<id>`. */
class ProviderPickerDeviceTest {
    @get:Rule val ui = createComposeRule()

    @Test fun choosingBankSetsProviderIconColorAndName() {
        var saved: Account? = null
        val old = Account(id = "p-bank", name = "", type = "bank", currency = "IDR")
        ui.setContent { KasivaTheme { AccountEditor(old, FinanceData(accounts = listOf(old)), false, {}, { saved = it }) } }
        ui.onNodeWithTag("provider-bca").performScrollTo().performClick()
        ui.onNodeWithTag("editor-save").performClick()
        ui.runOnIdle {
            val account = requireNotNull(saved)
            assertEquals("provider:bca", account.icon)
            assertEquals("BCA", account.name)
            assertEquals(0xFF0060AF, account.color)
        }
    }

    @Test fun otherKeepsPlainIcon() {
        var saved: Account? = null
        val old = Account(id = "p-ew", name = "Dompet lain", type = "ewallet", currency = "IDR", icon = "provider:ovo")
        ui.setContent { KasivaTheme { AccountEditor(old, FinanceData(accounts = listOf(old)), false, {}, { saved = it }) } }
        ui.onNodeWithTag("provider-other").performScrollTo().performClick()
        ui.onNodeWithTag("editor-save").performClick()
        ui.runOnIdle { assertEquals("account_balance_wallet", saved?.icon); assertEquals("Dompet lain", saved?.name) }
    }
}
