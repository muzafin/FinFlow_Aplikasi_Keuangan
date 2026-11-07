package com.clonical.kasiva.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonical.kasiva.core.i18n.appLanguages
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.model.FinanceData
import com.clonical.kasiva.core.model.LocalPreferenceState
import com.clonical.kasiva.core.model.LocalPreferences
import com.clonical.kasiva.core.platform.NativeServices
import com.clonical.kasiva.core.ui.*
import com.clonical.kasiva.feature.auth.OlloCurrencyDialog
import com.clonical.kasiva.i18n.*

/**
 * Halaman Pengaturan mengikuti Ollo: Umum (Mata Uang, Mata Uang Transaksi Bawaan, Kurs, Bahasa, Notifikasi, Keamanan,
 * Widget) lalu Animasi. Item khas Kasiva (Notifikasi Bank/E-Wallet, Keluar akun) ditaruh di grup yang paling dekat.
 * [addCategory] dipertahankan demi kompatibilitas pemanggil; Kategori kini dibuka dari tab Profil.
 */
@Composable
fun Settings(native: NativeServices, email: String, data: FinanceData, addCategory: () -> Unit, logout: () -> Unit,
             navigate: (String) -> Unit = {}, localMode: Boolean = false) {
    val p = LocalPreferences.current
    val store = LocalPreferenceState.current
    val base = data.profile?.baseCurrency ?: "IDR"
    var locked by remember { mutableStateOf(native.lockEnabled()) }
    // Status pengingat dibaca ulang setiap kali halaman tampil (diubah di halaman Notifikasi).
    val reminderOn = remember { native.reminderEnabled() }
    val reminderTime = remember { "${native.reminderHour().toString().padStart(2, '0')}:${native.reminderMinute().toString().padStart(2, '0')}" }
    var security by remember { mutableStateOf(false) }
    var widgets by remember { mutableStateOf(false) }
    var currencyPicker by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val languageLabel = if (p.language == "system") MR.strings.set_pref_theme_system.text() else appLanguages.firstOrNull { it.tag == p.language }?.nativeName ?: p.language

    Column(Modifier.fillMaxSize().background(FFColors.background).verticalScroll(rememberScrollState()).testTag("settings-page")
        .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 40.dp)) {
        SettingsCaption(MR.strings.pe_section_general.text())
        SettingsGroup {
            SettingsRow("hi_reimburse", MR.strings.pe_set_currency.text(), base, "settings-currency") { navigate("Profil akun") }
            SettingsDivider()
            SettingsRow("hi_wallet", MR.strings.pe_set_default_tx_currency.text(),
                if (p.defaultTransactionCurrency.isEmpty()) MR.strings.pe_set_same_as_display.text(base) else p.defaultTransactionCurrency,
                "settings-default-currency") { currencyPicker = true }
            SettingsDivider()
            SettingsRow("hi_trend_up", MR.strings.pe_set_exchange_rates.text(), MR.strings.pe_set_exchange_rates_caption.text(), "settings-exchange-rates") { navigate("Kurs") }
            SettingsDivider()
            SettingsRow("hi_language", MR.strings.set_page_language.text(), languageLabel, "settings-language") { navigate("Bahasa") }
            SettingsDivider()
            SettingsRow("hi_bell", MR.strings.set_profile_notifications.text(),
                if (reminderOn) MR.strings.pe_set_reminder_on.text(reminderTime) else MR.strings.pe_set_reminder_off.text(), "settings-notifications") { navigate("Notifikasi") }
            SettingsDivider()
            SettingsRow("hi_bank", MR.strings.set_profile_bank_notifications.text(), MR.strings.set_profile_bank_notifications_caption.text(), "settings-bank-notifications") { navigate("Impor notifikasi") }
            SettingsDivider()
            SettingsRow("hi_shield", MR.strings.set_profile_security.text(), MR.strings.pe_set_security_caption.text(), "settings-security") { security = true }
            SettingsDivider()
            SettingsRow("hi_grid_view", MR.strings.pe_set_widgets.text(), MR.strings.pe_set_widgets_caption.text(), "settings-widgets") { widgets = true }
        }
        SettingsCaption(MR.strings.pe_set_section_animations.text())
        SettingsGroup {
            SettingsSwitchRow("hi_add_circle", MR.strings.pe_set_tap_anim.text(), MR.strings.pe_set_tap_anim_caption.text(), p.tapAnimations, "settings-tap-animations") {
                store?.update(p.copy(tapAnimations = it))
            }
            SettingsDivider()
            SettingsSwitchRow("hi_sort", MR.strings.pe_set_page_anim.text(), MR.strings.pe_set_page_anim_caption.text(), p.pageTransitions, "settings-page-transitions") {
                store?.update(p.copy(pageTransitions = it))
            }
            SettingsDivider()
            SettingsSwitchRow("hi_note", MR.strings.pe_set_list_anim.text(), MR.strings.pe_set_list_anim_caption.text(), p.listAnimations, "settings-list-animations") {
                store?.update(p.copy(listAnimations = it))
            }
        }
        SettingsCaption(MR.strings.pe_set_section_account.text())
        SettingsGroup {
            SettingsRow("hi_user", if (localMode) MR.strings.set_logout.text() else MR.strings.set_logout_account.text(), email, "settings-logout", danger = true) { confirmLogout = true }
        }
        message?.let { Text(it, Modifier.padding(top = 12.dp, start = 4.dp), style = FFType.metaData, color = FFColors.onSurfaceVariant) }
    }

    if (currencyPicker) OlloCurrencyDialog(
        title = MR.strings.pe_set_default_tx_currency.text(),
        closeLabel = MR.strings.common_close.text(),
        selected = p.defaultTransactionCurrency.ifEmpty { base },
        onSelect = { code ->
            // Memilih mata uang tampilan = kembali ke "Sama dengan tampilan".
            store?.update(p.copy(defaultTransactionCurrency = if (code == base) "" else code))
            currencyPicker = false
        },
        onDismiss = { currencyPicker = false },
    )
    if (security) AlertDialog(
        onDismissRequest = { security = false },
        title = { Text(MR.strings.set_profile_security.text()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(MR.strings.set_app_lock.text(), Modifier.weight(1f), style = FFType.bodyMain)
                    Switch(locked, { requested ->
                        native.unlock { ok -> if (ok) { locked = requested; native.setLockEnabled(requested); message = null } else message = MR.strings.set_auth_cancelled.text() }
                    }, Modifier.testTag("settings-app-lock"), colors = settingsSwitchColors())
                }
                Text(MR.strings.set_app_lock_hint.text(), style = FFType.metaData, color = FFColors.onSurfaceVariant)
                message?.let { Text(it, style = FFType.metaData, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton({ security = false }) { Text(MR.strings.common_close.text()) } },
    )
    if (widgets) AlertDialog(
        onDismissRequest = { widgets = false },
        title = { Text(MR.strings.pe_set_widgets.text()) },
        text = { Text(MR.strings.pe_set_widgets_body.text(), style = FFType.bodyMain) },
        confirmButton = { TextButton({ widgets = false }) { Text(MR.strings.common_close.text()) } },
    )
    if (confirmLogout) AlertDialog(
        onDismissRequest = { confirmLogout = false },
        title = { Text(MR.strings.set_logout_title.text()) },
        text = { Text(if (localMode) MR.strings.set_logout_body_local.text() else MR.strings.set_logout_body_account.text()) },
        confirmButton = { TextButton({ confirmLogout = false; logout() }, Modifier.testTag("settings-logout-confirm")) { Text(MR.strings.set_logout.text()) } },
        dismissButton = { TextButton({ confirmLogout = false }) { Text(MR.strings.common_cancel.text()) } },
    )
}

@Composable private fun settingsSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = FFColors.onPill, checkedTrackColor = FFColors.pill, checkedBorderColor = FFColors.pill,
    uncheckedThumbColor = FFColors.onSurfaceVariant, uncheckedTrackColor = FFColors.surfaceContainerHigh, uncheckedBorderColor = FFColors.outlineVariant,
)

@Composable private fun SettingsCaption(label: String) {
    Text(label, Modifier.padding(start = 2.dp, top = 18.dp, bottom = 10.dp), style = FFType.metaData.copy(fontSize = 13.5.sp, lineHeight = 18.sp), color = FFColors.onSurfaceVariant)
}

@Composable private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(FFColors.surface), content = content)
}

@Composable private fun SettingsDivider() {
    Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(1.dp).background(FFColors.hairline))
}

@Composable private fun SettingsIcon(icon: String, danger: Boolean = false) {
    val error = MaterialTheme.colorScheme.error
    Box(Modifier.size(40.dp).clip(CircleShape).background(if (danger) error.copy(alpha = .12f) else if (FFColors.isDark) FFColors.hairline else FFColors.surfaceContainerHigh), contentAlignment = Alignment.Center) {
        FFIcon(icon, if (danger) error else FFColors.onSurface, 20.dp)
    }
}

@Composable private fun SettingsTexts(title: String, subtitle: String, modifier: Modifier, danger: Boolean = false) {
    Column(modifier) {
        Text(title, style = FFType.bodyMain.copy(fontSize = 17.sp, lineHeight = 22.sp), color = if (danger) MaterialTheme.colorScheme.error else FFColors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = FFType.metaData.copy(fontSize = 14.sp, lineHeight = 19.sp), color = FFColors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable private fun SettingsRow(icon: String, title: String, subtitle: String, tag: String, danger: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).testTag(tag).padding(horizontal = 16.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
        SettingsIcon(icon, danger)
        Spacer(Modifier.width(16.dp))
        SettingsTexts(title, subtitle, Modifier.weight(1f), danger)
        FFIcon("hi_chevron_right", FFColors.onSurfaceVariant, 20.dp)
    }
}

@Composable private fun SettingsSwitchRow(icon: String, title: String, subtitle: String, checked: Boolean, tag: String, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { change(!checked) }.padding(horizontal = 16.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
        SettingsIcon(icon)
        Spacer(Modifier.width(16.dp))
        SettingsTexts(title, subtitle, Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Switch(checked, change, Modifier.testTag(tag), colors = settingsSwitchColors())
    }
}
