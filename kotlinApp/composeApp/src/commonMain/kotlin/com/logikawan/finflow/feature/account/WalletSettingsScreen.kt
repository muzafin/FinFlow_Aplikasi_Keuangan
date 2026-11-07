package com.logikawan.finflow.feature.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logikawan.finflow.core.i18n.text
import com.logikawan.finflow.core.model.*
import com.logikawan.finflow.core.ui.*
import com.logikawan.finflow.core.util.newId
import com.logikawan.finflow.core.util.nowMillis
import com.logikawan.finflow.i18n.*

private enum class WalletSettingsSheet { DefaultWallet, Dashboard, PickerLayout, MyWalletsOrder, PickerOrder, Import }

private val pickerOrders = listOf("my_wallets", "most_used", "name", "balance")
private val dashboardCounts = listOf(0, 2, 3, 4, 6, 8)

@Composable
private fun pickerOrderLabel(value: String) = when (value) {
    "most_used" -> MR.strings.pb_order_most_used.text(); "name" -> MR.strings.pb_order_name.text()
    "balance" -> MR.strings.pb_order_balance.text(); else -> MR.strings.pb_order_my_wallets.text()
}

/** Kelompok dompet di "Dompet Saya": jenis yang tidak dikenal masuk "other". */
private fun walletGroupOf(account: Account) = if (account.type in setOf("cash", "bank", "ewallet", "savings", "credit", "investment", "loan")) account.type else "other"

/**
 * Halaman "Pengaturan Dompet" bergaya Ollo (Wallet Settings). Semua pilihan disimpan di [AppPreferences]
 * (sinkron sebagai satu payload preferensi). [importWallets] menyimpan dompet dari templat; null = baris impor disembunyikan.
 */
@Composable
fun WalletSettingsScreen(data: FinanceData, busy: Boolean = false, importWallets: ((List<Account>) -> Unit)? = null) {
    val prefs = LocalPreferences.current
    val store = LocalPreferenceState.current
    var sheet by remember { mutableStateOf<WalletSettingsSheet?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    fun update(value: AppPreferences) {
        try { requireNotNull(store).update(value); error = null } catch (e: Exception) { error = e.message ?: MR.strings.pb_save_failed.text() }
    }
    val active = data.accounts.filter { !it.archived }
    val defaultName = active.find { it.id == prefs.defaultWalletId }?.name ?: MR.strings.pb_none.text()
    Column(Modifier.fillMaxSize().background(FFColors.background).verticalScroll(rememberScrollState()).testTag("wallet-settings")
        .padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(MR.strings.pb_general.text(), Modifier.padding(start = 4.dp, top = 12.dp, bottom = 8.dp),
            style = FFType.metaData.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = .4.sp), color = FFColors.onSurfaceVariant)
        SettingsRow("hi_wallet", MR.strings.set_pref_default_wallet.text(), null, defaultName, "ws-default-wallet") { sheet = WalletSettingsSheet.DefaultWallet }
        SettingsRow("hi_wallet", MR.strings.pb_ws_dashboard_display.text(), MR.strings.pb_ws_dashboard_display_desc.text(), null, "ws-dashboard") { sheet = WalletSettingsSheet.Dashboard }
        SettingsRow("hi_wallet", MR.strings.pb_ws_picker_layout.text(), MR.strings.pb_ws_picker_layout_desc.text(),
            if (prefs.walletPickerLayout == "list") MR.strings.pb_layout_list.text() else MR.strings.pb_layout_grid.text(), "ws-picker-layout") { sheet = WalletSettingsSheet.PickerLayout }
        SettingsRow("hi_sort", MR.strings.pb_ws_my_wallets_order.text(), MR.strings.pb_ws_my_wallets_order_desc.text(), null, "ws-my-wallets-order") { sheet = WalletSettingsSheet.MyWalletsOrder }
        SettingsRow("hi_sort", MR.strings.pb_ws_picker_order.text(), MR.strings.pb_ws_picker_order_desc.text(), pickerOrderLabel(prefs.walletPickerOrder), "ws-picker-order") { sheet = WalletSettingsSheet.PickerOrder }
        if (importWallets != null) SettingsRow("hi_note_doc", MR.strings.pb_ws_import_templates.text(), null, null, "ws-import-templates") { sheet = WalletSettingsSheet.Import }
        error?.let { Text(it, style = FFType.bodyMain, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(4.dp).testTag("ws-error")) }
        Spacer(Modifier.height(24.dp))
    }
    when (sheet) {
        WalletSettingsSheet.DefaultWallet -> OptionSheet(MR.strings.set_pref_default_wallet.text(),
            listOf("" to MR.strings.pb_none.text()) + active.map { it.id to it.name }, prefs.defaultWalletId, { sheet = null }) {
            update(prefs.copy(defaultWalletId = it)); sheet = null
        }
        WalletSettingsSheet.PickerLayout -> OptionSheet(MR.strings.pb_ws_picker_layout.text(),
            listOf("grid" to MR.strings.pb_layout_grid.text(), "list" to MR.strings.pb_layout_list.text()), prefs.walletPickerLayout, { sheet = null }) {
            update(prefs.copy(walletPickerLayout = it)); sheet = null
        }
        WalletSettingsSheet.PickerOrder -> OptionSheet(MR.strings.pb_ws_picker_order.text(),
            pickerOrders.map { it to pickerOrderLabel(it) }, prefs.walletPickerOrder, { sheet = null }) {
            update(prefs.copy(walletPickerOrder = it)); sheet = null
        }
        WalletSettingsSheet.Dashboard -> DashboardWalletsEditor(prefs, active, { sheet = null }) { update(it); sheet = null }
        WalletSettingsSheet.MyWalletsOrder -> MyWalletsOrderEditor(prefs, active, { sheet = null }) { update(it); sheet = null }
        WalletSettingsSheet.Import -> importWallets?.let { save -> ImportTemplatesEditor(data, busy, { sheet = null }) { save(it); sheet = null } }
        null -> {}
    }
}

/** Baris kartu Ollo: lingkaran ikon, judul + keterangan, nilai di kanan, chevron. */
@Composable
private fun SettingsRow(icon: String, title: String, subtitle: String?, value: String?, tag: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(20.dp)).background(FFColors.surface)
        .clickable(role = Role.Button, onClick = onClick).testTag(tag).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(FFColors.iconCircle), contentAlignment = Alignment.Center) {
            FFIcon(icon, FFColors.onSurface, 20.dp)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = FFType.bodyMain.copy(fontSize = 17.sp, lineHeight = 23.sp), fontWeight = FontWeight.Medium, color = FFColors.onSurface)
            subtitle?.let { Text(it, style = FFType.metaData.copy(fontSize = 14.sp, lineHeight = 19.sp), color = FFColors.onSurfaceVariant) }
        }
        value?.let {
            Spacer(Modifier.width(8.dp))
            Text(it, Modifier.widthIn(max = 140.dp), style = FFType.bodyMain.copy(fontSize = 16.sp, lineHeight = 22.sp), color = FFColors.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        FFIcon("hi_chevron_right", FFColors.onSurfaceVariant, 20.dp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionSheet(title: String, options: List<Pair<String, String>>, selected: String, dismiss: () -> Unit, choose: (String) -> Unit) {
    ModalBottomSheet(onDismissRequest = dismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = FFColors.canvas) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = FFType.sectionTitle)
            LazyColumn(Modifier.heightIn(max = 480.dp), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(options, key = { it.first }) { (value, label) ->
                    val isSelected = value == selected
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (isSelected) FFColors.raised else FFColors.surface)
                        .semantics { this.selected = isSelected }
                        .clickable(role = Role.RadioButton) { choose(value) }.testTag("ws-option-${value.ifEmpty { "none" }}")
                        .padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(label, Modifier.weight(1f), style = FFType.bodyMain, color = FFColors.onSurface)
                        if (isSelected) FFIcon("hi_check", FFColors.onSurface, 20.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckRow(title: String, subtitle: String?, checked: Boolean, enabled: Boolean, tag: String, leading: @Composable () -> Unit, toggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(FFColors.surface)
        .clickable(enabled = enabled, role = Role.Checkbox, onClick = toggle).testTag(tag).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        leading()
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = FFType.bodyMain, fontWeight = FontWeight.Medium, color = if (enabled) FFColors.onSurface else FFColors.outline)
            subtitle?.let { Text(it, style = FFType.metaData, color = FFColors.onSurfaceVariant) }
        }
        Checkbox(checked, { toggle() }, enabled = enabled, colors = CheckboxDefaults.colors(checkedColor = FFColors.pill, checkmarkColor = FFColors.onPill))
    }
}

@Composable
private fun WalletLeading(account: Account) {
    Box(Modifier.size(36.dp).clip(CircleShape).background(FFColors.iconCircle), contentAlignment = Alignment.Center) {
        AccountIconContent(account, if (account.icon.startsWith("account_balance")) "hi_wallet" else account.icon, FFColors.onSurface, 18.dp)
    }
}

@Composable
private fun DashboardWalletsEditor(prefs: AppPreferences, active: List<Account>, dismiss: () -> Unit, save: (AppPreferences) -> Unit) {
    var count by remember { mutableIntStateOf(prefs.dashboardWalletCount) }
    val chosen = remember { mutableStateListOf<String>().apply { addAll(prefs.dashboardWalletIds.filter { id -> active.any { it.id == id } }) } }
    EditorDialog(MR.strings.pb_ws_dashboard_display.text(), false, dismiss, save = {
        save(prefs.copy(dashboardWalletCount = count, dashboardWalletIds = chosen.toList()))
    }, error = null) {
        Text(MR.strings.pb_ws_count.text(), style = FFType.bodyMain.copy(fontSize = 16.sp), color = FFColors.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            dashboardCounts.forEach { value ->
                val isSelected = value == count
                Box(Modifier.height(40.dp).widthIn(min = 44.dp).clip(RoundedCornerShape(50))
                    .background(if (isSelected) FFColors.pill else FFColors.surface)
                    .semantics { this.selected = isSelected }
                    .clickable(role = Role.RadioButton) { count = value }.testTag("ws-count-$value").padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center) {
                    Text(if (value == 0) MR.strings.pb_ws_count_all.text() else value.toString(), style = FFType.bodyMain,
                        color = if (isSelected) FFColors.onPill else FFColors.onSurface, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Text(MR.strings.pb_ws_wallets_shown.text(), style = FFType.bodyMain.copy(fontSize = 16.sp), color = FFColors.onSurfaceVariant)
        Text(MR.strings.pb_ws_wallets_auto_hint.text(), style = FFType.metaData, color = FFColors.onSurfaceVariant)
        if (active.isEmpty()) Text(MR.strings.pb_ws_no_wallets.text(), style = FFType.bodyMain, color = FFColors.outline)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            active.forEach { account ->
                CheckRow(account.name, walletTypeLabel(account.type), account.id in chosen, true, "ws-dashboard-${account.id}", { WalletLeading(account) }) {
                    if (account.id in chosen) chosen.remove(account.id) else if (chosen.size < 50) chosen.add(account.id)
                }
            }
        }
    }
}

@Composable
private fun MoveButtons(index: Int, last: Int, tag: String, move: (Int) -> Unit) {
    IconButton({ move(-1) }, enabled = index > 0, modifier = Modifier.size(36.dp).testTag("$tag-up")) {
        FFIcon("hi_chevron_up", if (index > 0) FFColors.onSurface else FFColors.outline, 20.dp, MR.strings.pb_move_up.text())
    }
    IconButton({ move(1) }, enabled = index < last, modifier = Modifier.size(36.dp).testTag("$tag-down")) {
        FFIcon("hi_chevron_down", if (index < last) FFColors.onSurface else FFColors.outline, 20.dp, MR.strings.pb_move_down.text())
    }
}

private fun <T> MutableList<T>.moveItem(from: Int, by: Int) {
    val to = from + by
    if (from !in indices || to !in indices) return
    add(to, removeAt(from))
}

@Composable
private fun MyWalletsOrderEditor(prefs: AppPreferences, active: List<Account>, dismiss: () -> Unit, save: (AppPreferences) -> Unit) {
    val groups = remember { prefs.walletGroupOrder.toMutableStateList() }
    // Urutan dompet: urutan tersimpan dulu, dompet baru di belakang sesuai urutan data.
    val wallets = remember {
        val known = prefs.walletOrder.mapNotNull { id -> active.find { it.id == id } }
        (known + active.filter { it !in known }).toMutableStateList()
    }
    EditorDialog(MR.strings.pb_ws_my_wallets_order.text(), false, dismiss, save = {
        // Hanya dompet aktif yang diurutkan; dompet baru tampil setelahnya.
        save(prefs.copy(walletGroupOrder = groups.toList(), walletOrder = wallets.map { it.id }.take(500)))
    }, error = null) {
        Text(MR.strings.pb_ws_my_wallets_order_desc.text(), style = FFType.metaData, color = FFColors.onSurfaceVariant)
        Text(MR.strings.pb_ws_groups.text(), style = FFType.sectionTitle.copy(fontSize = 17.sp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            groups.forEachIndexed { index, group ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(FFColors.surface).testTag("ws-group-$group")
                    .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(walletTypeLabel(group), Modifier.weight(1f), style = FFType.bodyMain, fontWeight = FontWeight.Medium, color = FFColors.onSurface)
                    MoveButtons(index, groups.lastIndex, "ws-group-$group") { groups.moveItem(index, it) }
                }
            }
        }
        Text(MR.strings.pb_ws_wallets.text(), style = FFType.sectionTitle.copy(fontSize = 17.sp))
        if (wallets.isEmpty()) Text(MR.strings.pb_ws_no_wallets.text(), style = FFType.bodyMain, color = FFColors.outline)
        groups.forEach { group ->
            val inGroup = wallets.filter { walletGroupOf(it) == group }
            if (inGroup.isNotEmpty()) {
                Text(walletTypeLabel(group), style = FFType.metaData, color = FFColors.onSurfaceVariant)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    inGroup.forEachIndexed { position, account ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(FFColors.surface).testTag("ws-wallet-${account.id}")
                            .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            WalletLeading(account)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(account.name, style = FFType.bodyMain, fontWeight = FontWeight.Medium, color = FFColors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                account.parentId?.let { parentId -> active.find { it.id == parentId }?.let {
                                    Text(MR.strings.acc_pocket_of.text(it.name), style = FFType.metaData, color = FFColors.onSurfaceVariant)
                                } }
                            }
                            MoveButtons(position, inGroup.lastIndex, "ws-wallet-${account.id}") { by ->
                                // Tukar dengan tetangga di grup yang sama.
                                inGroup.getOrNull(position + by)?.let { neighbour ->
                                    val a = wallets.indexOf(account); val b = wallets.indexOf(neighbour)
                                    if (a >= 0 && b >= 0) { wallets[a] = neighbour; wallets[b] = account }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportTemplatesEditor(data: FinanceData, busy: Boolean, dismiss: () -> Unit, import: (List<Account>) -> Unit) {
    val currency = data.profile?.baseCurrency ?: "IDR"
    val chosen = remember { mutableStateListOf<String>() }
    var error by remember { mutableStateOf<String?>(null) }
    fun templateName(template: WalletTemplate) = template.providerId?.let { id -> financeProviders.find { it.id == id }?.name }
    fun exists(template: WalletTemplate, name: String) = data.accounts.any { account ->
        !account.archived && if (template.providerId != null) account.icon == PROVIDER_PREFIX + template.providerId
        else account.type == template.type && account.name.equals(name, ignoreCase = true)
    }
    val rows = walletTemplates.map { template -> template to (templateName(template) ?: walletTypeLabel(template.type)) }
    EditorDialog(MR.strings.pb_ws_import_templates.text(), busy, dismiss, save = {
        val created = rows.filter { (template, name) -> template.id in chosen && !exists(template, name) }.map { (template, name) ->
            val provider = template.providerId?.let { id -> financeProviders.find { it.id == id } }
            Account(id = newId(), name = name, type = template.type, currency = currency, openingBalanceMinor = 0, createdAt = nowMillis(),
                icon = provider?.let { PROVIDER_PREFIX + it.id } ?: template.icon, color = provider?.color ?: 0xFF2196F3)
        }
        if (created.isEmpty()) error = MR.strings.pb_ws_import_none_selected.text() else { error = null; import(created) }
    }, error = error) {
        Text(MR.strings.pb_ws_import_desc.text(currency), style = FFType.metaData, color = FFColors.onSurfaceVariant)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { (template, name) ->
                val already = exists(template, name)
                val provider = template.providerId?.let { id -> financeProviders.find { it.id == id } }
                CheckRow(name, if (already) MR.strings.pb_ws_import_exists.text() else walletTypeLabel(template.type), template.id in chosen && !already, !already && !busy,
                    "ws-template-${template.id}", {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(FFColors.iconCircle), contentAlignment = Alignment.Center) {
                            if (provider != null) ProviderBadge(provider, Modifier.matchParentSize()) else FFIcon(template.icon, FFColors.onSurface, 18.dp)
                        }
                    }) {
                    if (template.id in chosen) chosen.remove(template.id) else chosen.add(template.id)
                }
            }
        }
    }
}
