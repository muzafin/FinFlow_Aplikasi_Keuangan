package com.clonical.kasiva.feature.account

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.model.*
import com.clonical.kasiva.core.ui.*
import com.clonical.kasiva.core.util.*
import com.clonical.kasiva.feature.auth.OlloCurrencyDialog
import com.clonical.kasiva.i18n.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Jenis dompet yang dapat dipilih di editor (urutan pill seperti Ollo). `savings` hanya lewat Target. */
private val walletEditorTypes = listOf("cash", "bank", "ewallet", "credit", "investment", "loan")

/** Ikon bawaan per jenis (sama dengan daftar Dompet Saya) — dipakai saat ikon masih nilai bawaan lama. */
private fun defaultWalletIcon(type: String) = when (type) {
    "bank" -> "hi_bank"; "ewallet" -> "hi_ewallet"; "savings" -> "hi_piggy"; "credit" -> "hi_card"
    "investment" -> "hi_invest"; "loan" -> "hi_debt"; else -> "hi_wallet"
}

private fun isLegacyDefaultIcon(icon: String) = icon.isBlank() || icon.startsWith("account_balance")

/**
 * Ikon umum untuk dompet (grid "Ikon Umum" Ollo). Nilai disimpan apa adanya di `Account.icon` (≤100 karakter)
 * dan harus dapat dirender [FFIcon]: Hugeicons `hi_*` atau nama ikon Flutter di FlutterIcons.kt.
 */
val walletGenericIcons = listOf(
    "hi_wallet", "hi_bank", "hi_card", "hi_debt", "hi_piggy", "hi_coins",
    "hi_ewallet", "hi_reimburse", "hi_user", "attach_money_outlined", "hi_trend_up", "hi_image",
    "hi_invest", "hi_bill", "hi_bag", "shopping_bag_outlined", "store_outlined", "storefront_outlined",
    "hi_wish", "redeem_outlined", "hi_tag", "local_mall_outlined", "payments_outlined", "savings_outlined",
    "directions_bus_outlined", "directions_car_outlined", "hi_home", "business_outlined", "work_outline_outlined", "hi_laptop",
    "restaurant_outlined", "local_hospital_outlined", "medical_services_outlined", "school_outlined", "movie_outlined", "sports_esports_outlined",
    "hi_heart", "hi_flag", "hi_calculator", "hi_camera", "hi_shield", "hi_bulb",
    "people_outlined", "emoji_events_outlined", "interests_outlined", "devices_outlined", "phone_outlined", "location_on_outlined",
    "hi_calendar", "hi_clock", "hi_percent", "hi_mail", "hi_note_doc", "hi_settings",
)

private val walletColors = listOf(
    0xFF2196F3L to MR.strings.acc_color_blue, 0xFF4CAF50L to MR.strings.acc_color_green, 0xFFF44336L to MR.strings.acc_color_red,
    0xFFFF9800L to MR.strings.acc_color_orange, 0xFF9C27B0L to MR.strings.acc_color_purple,
)

@Composable
fun AccountEditor(old: Account?, data: FinanceData, busy: Boolean, dismiss: () -> Unit, save: (Account) -> Unit) {
    val id = remember { old?.id ?: newId() }
    // Ikon kustom: thumbnail dipilih dulu, diunggah/disimpan hanya saat dompet disimpan.
    val walletIcons = LocalWalletIcons.current
    val scope = rememberCoroutineScope()
    var pendingIcon by remember { mutableStateOf<ByteArray?>(null) }
    var uploading by remember { mutableStateOf(false) }
    @Suppress("NAME_SHADOWING") val busy = busy || uploading
    var name by remember { mutableStateOf(old?.name.orEmpty()) }
    var type by remember { mutableStateOf(old?.type ?: "cash") }
    var currency by remember { mutableStateOf(old?.currency ?: data.profile?.baseCurrency ?: "IDR") }
    var opening by remember { mutableStateOf(old?.let { decimalAmount(it.openingBalanceMinor, it.currency) } ?: "0") }
    var archived by remember { mutableStateOf(old?.archived ?: false) }
    var excluded by remember { mutableStateOf(old?.excludedFromTotal ?: false) }
    var pocket by remember { mutableStateOf(old?.parentId != null) }
    var parent by remember { mutableStateOf(old?.parentId.orEmpty()) }
    var icon by remember { mutableStateOf(old?.icon ?: "account_balance_wallet") }
    var color by remember { mutableStateOf(old?.color ?: 0xFF2196F3) }
    var error by remember { mutableStateOf<String?>(null) }
    var pickingCurrency by remember { mutableStateOf(false) }
    var typeInfo by remember { mutableStateOf(false) }
    // Dompet target (savings) dibuat lewat menu Target; jenisnya dikunci agar tetap dapat disimpan.
    val keys = if (old?.type == "savings") listOf("savings") else walletEditorTypes
    val currencyLocked = old != null
    EditorDialog(if (old == null) MR.strings.acc_add_wallet.text() else MR.strings.acc_edit_wallet.text(), busy, dismiss, save = {
        if (!busy) try {
            val code = currency.trim().uppercase()
            require(name.isNotBlank() && name.trim().length <= 100) { MR.strings.acc_err_name_required.text() }
            require(code.matches(Regex("[A-Z]{3}"))) { MR.strings.acc_err_currency_code.text() }
            require(old == null || code == old.currency) { MR.strings.acc_err_currency_locked.text() }
            require(type in keys) { MR.strings.acc_err_invalid_type.text() }
            require(!pocket || parent.isNotBlank()) { MR.strings.acc_err_choose_parent.text() }
            require(!pocket || data.accounts.none { it.parentId == id }) { MR.strings.acc_err_parent_has_pockets.text() }
            val account = Account(id, name.trim(), type, code, minorAmount(opening, code, allowNegative = true), archived, old?.createdAt ?: nowMillis(),
                excluded, if (pocket) parent else null, icon, color, goalId = old?.goalId)
            val bytes = pendingIcon
            if (bytes != null && icon == uploadedIconRef(id) && walletIcons != null) {
                uploading = true
                error = null
                scope.launch {
                    try { walletIcons.save(id, bytes); pendingIcon = null; save(account) }
                    catch (e: CancellationException) { throw e }
                    catch (e: Exception) { error = e.message ?: MR.strings.r3_icon_upload_failed.text() }
                    finally { uploading = false }
                }
            } else save(account)
        } catch (e: Exception) { error = e.message ?: MR.strings.acc_err_invalid_data.text() }
    }, error = error, kind = EditorKind.Wallet) {
        Text(MR.strings.acc_wallet_details.text(), style = FFType.sectionTitle.copy(fontSize = 19.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold), color = FFColors.onSurface)

        // Tipe: pill bergulir horizontal dengan ikon (Ollo).
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(MR.strings.acc_type.text())
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(28.dp).clip(CircleShape).clickable(role = Role.Button) { typeInfo = true }.testTag("wallet-type-info"), contentAlignment = Alignment.Center) {
                    FFIcon("hi_info", FFColors.onSurfaceVariant, 18.dp, MR.strings.pb_type_info_title.text())
                }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                keys.forEach { key ->
                    TypePill(walletTypePillLabel(key), defaultWalletIcon(key), key == type, !busy, Modifier.testTag("wallet-type-$key")) {
                        if (type != key) {
                            // Penyedia hanya berlaku untuk jenisnya sendiri.
                            if (providerOf(icon)?.type != null && providerOf(icon)?.type != key) icon = "account_balance_wallet"
                            type = key
                        }
                    }
                }
            }
        }

        if (type == "bank" || type == "ewallet") ProviderPicker(type, providerOf(icon)?.id, !busy) { provider ->
            val previous = providerOf(icon)
            if (provider == null) { if (previous != null) { icon = if (type == "bank") "account_balance" else "account_balance_wallet" } }
            else {
                if (name.isBlank() || name == previous?.name) name = provider.name
                icon = PROVIDER_PREFIX + provider.id; color = provider.color
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel(MR.strings.acc_wallet_name.text())
            OlloInput(name, { if (!busy) name = it }, MR.strings.pb_wallet_name_placeholder.text(), MR.strings.acc_wallet_name.text(),
                Modifier.testTag("wallet-name"), enabled = !busy)
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel(MR.strings.acc_wallet_currency.text())
            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(18.dp)).background(FFColors.surface)
                .then(if (!currencyLocked && !busy) Modifier.clickable(role = Role.Button) { pickingCurrency = true } else Modifier)
                .testTag("wallet-currency").padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(FFColors.iconCircle), contentAlignment = Alignment.Center) {
                    FFIcon("hi_language", FFColors.onSurface, 18.dp)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(currency, style = FFType.bodyMain.copy(fontSize = 17.sp, lineHeight = 22.sp), fontWeight = FontWeight.Bold, color = FFColors.onSurface)
                    Text(walletCurrencyName(currency), style = FFType.metaData.copy(fontSize = 13.sp, lineHeight = 18.sp), color = FFColors.onSurfaceVariant)
                }
                if (currencyLocked) FFIcon("lock_outline_rounded", FFColors.outline, 18.dp, MR.strings.acc_err_currency_locked.text())
                else FFIcon("hi_chevron_right", FFColors.onSurfaceVariant, 20.dp)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel(MR.strings.acc_opening_balance.text())
            OlloInput(opening, { if (!busy) opening = it }, "0", MR.strings.acc_opening_balance.text(), Modifier.testTag("wallet-opening"),
                enabled = !busy, keyboardType = KeyboardType.Decimal)
            Text(MR.strings.acc_opening_balance_hint.text(), style = FFType.metaData, color = FFColors.onSurfaceVariant)
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ToggleCard(MR.strings.acc_exclude_from_total.text(), MR.strings.pb_exclude_desc.text(), excluded, !busy, "wallet-exclude") { excluded = it }
            ToggleCard(MR.strings.pb_link_pocket.text(), MR.strings.pb_link_pocket_desc.text(), pocket, !busy && type != "savings", "wallet-pocket") { pocket = it; if (!it) parent = "" }
            if (pocket) WalletChoice(MR.strings.acc_parent_wallet.text(), parent, data,
                data.accounts.filter { it.id != id && !it.archived && it.parentId == null && it.currency == currency }, !busy) { parent = it }
            if (old != null) ToggleCard(MR.strings.acc_archive_wallet.text(), MR.strings.acc_history_kept.text(), archived, !busy, "wallet-archive") { archived = it }
        }

        // Ikon: grid "Ikon Umum", unggah ikon kustom (bila App menyediakan penyimpan ikon), lalu warna.
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel(MR.strings.acc_icon.text())
            Text(MR.strings.pb_generic_icons.text(), style = FFType.bodyMain.copy(fontSize = 16.sp, lineHeight = 22.sp), color = FFColors.onSurfaceVariant)
            val provider = providerOf(icon)
            val highlighted = when {
                provider != null -> null
                isLegacyDefaultIcon(icon) -> defaultWalletIcon(type)
                else -> icon
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(FFColors.surface).padding(horizontal = 8.dp, vertical = 12.dp)) {
                walletGenericIcons.chunked(6).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { value ->
                            val selected = value == highlighted
                            Box(Modifier.weight(1f).aspectRatio(1f).padding(5.dp).clip(CircleShape)
                                .background(if (selected) FFColors.raised else Color.Transparent)
                                .border(if (selected) 2.dp else 0.dp, if (selected) FFColors.onSurface else Color.Transparent, CircleShape)
                                .semantics { this.selected = selected }
                                .clickable(enabled = !busy, role = Role.RadioButton) { icon = value }
                                .testTag("wallet-icon-$value"), contentAlignment = Alignment.Center) {
                                FFIcon(value, if (selected) FFColors.onSurface else FFColors.onSurfaceVariant, 24.dp)
                            }
                        }
                        repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            if (walletIcons != null && walletIconIdPattern.matches(id)) CustomIconRow(id, isUploadedIcon(icon), pendingIcon, !busy) {
                walletIcons.pick { bytes, failure ->
                    if (bytes != null) { pendingIcon = bytes; icon = uploadedIconRef(id); error = null }
                    else if (failure != null) error = failure
                }
            }
            if (provider == null) {
                Text(MR.strings.acc_color.text(), style = FFType.bodyMain.copy(fontSize = 16.sp, lineHeight = 22.sp), color = FFColors.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    walletColors.forEach { (value, label) ->
                        val text = label.text()
                        Box(Modifier.size(36.dp).clip(CircleShape).background(Color(value))
                            .border(if (color == value) 2.dp else 0.dp, if (color == value) FFColors.onSurface else Color.Transparent, CircleShape)
                            .semantics { contentDescription = text; selected = color == value }
                            .clickable(enabled = !busy, role = Role.RadioButton) { color = value }.testTag("wallet-color-$value"),
                            contentAlignment = Alignment.Center) {
                            if (color == value) FFIcon("check_rounded", Color.White, 18.dp)
                        }
                    }
                }
            }
        }
        if (type == "bank" || type == "ewallet") Text(MR.strings.acc_manual_sync_note.text(), style = FFType.metaData, color = FFColors.onSurfaceVariant)
    }
    if (pickingCurrency) OlloCurrencyDialog(
        title = MR.strings.auth_currency_dialog_title.text(),
        closeLabel = MR.strings.common_close.text(),
        selected = currency,
        onSelect = { code ->
            if (code != currency) { currency = code; if (pocket) parent = "" }
            pickingCurrency = false
        },
        onDismiss = { pickingCurrency = false },
    )
    if (typeInfo) AlertDialog(onDismissRequest = { typeInfo = false },
        title = { Text(MR.strings.pb_type_info_title.text(), style = FFType.sectionTitle) },
        text = { Text(MR.strings.pb_type_info_body.text(), style = FFType.bodyMain) },
        confirmButton = { TextButton(onClick = { typeInfo = false }) { Text(MR.strings.common_ok.text()) } })
}

/** Baris "Unggah Ikon Kustom" (Ollo "Upload Custom Icon"): pratinjau bulat, judul, keterangan. */
@Composable
private fun CustomIconRow(accountId: String, selected: Boolean, pending: ByteArray?, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val preview = remember(pending) { WalletIconStore.decode(pending) }
    Row(Modifier.fillMaxWidth().clip(shape).background(FFColors.surface)
        .then(if (selected) Modifier.border(2.dp, FFColors.onSurface, shape) else Modifier)
        .semantics { this.selected = selected }
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick).testTag("wallet-icon-upload")
        .padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(FFColors.iconCircle), contentAlignment = Alignment.Center) {
            when {
                preview != null -> Image(preview, MR.strings.r3_icon_custom.text(), Modifier.matchParentSize(), contentScale = ContentScale.Crop)
                selected -> WalletUploadedIcon(accountId, Modifier.matchParentSize()) { FFIcon("hi_image", FFColors.onSurface, 22.dp) }
                else -> FFIcon("hi_image", FFColors.onSurface, 22.dp)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(MR.strings.r3_icon_upload.text(), style = FFType.bodyMain.copy(fontSize = 16.sp, lineHeight = 22.sp), fontWeight = FontWeight.Medium, color = FFColors.onSurface)
            Text(MR.strings.r3_icon_upload_caption.text(), style = FFType.metaData.copy(fontSize = 13.sp, lineHeight = 18.sp), color = FFColors.onSurfaceVariant)
        }
        FFIcon("hi_upload", FFColors.onSurfaceVariant, 20.dp)
    }
}

private fun walletTypePillLabel(type: String): String = if (type == "bank") MR.strings.acc_type_bank.text() else walletTypeLabel(type)

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = FFType.bodyMain.copy(fontSize = 16.sp, lineHeight = 22.sp), color = FFColors.onSurfaceVariant)
}

/** Pill jenis dompet: terpilih = biru bergradasi (aksen Ollo), lainnya bergaris tipis. */
@Composable
private fun TypePill(label: String, icon: String, selected: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(modifier.height(50.dp).clip(shape)
        .then(if (selected) Modifier.background(Brush.horizontalGradient(listOf(Color(0xFF1F78D1), Color(0xFF4A95E3))))
            else Modifier.border(1.dp, FFColors.hairline, shape).background(FFColors.background))
        .semantics { this.selected = selected }
        .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically) {
        FFIcon(icon, if (selected) Color.White else FFColors.onSurfaceVariant, 20.dp)
        Spacer(Modifier.width(10.dp))
        Text(label, style = FFType.bodyMain.copy(fontSize = 16.sp, lineHeight = 22.sp), color = if (selected) Color.White else FFColors.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
    }
}

/** Kolom isian Ollo: latar abu lembut, tanpa garis, sudut 16dp, placeholder abu. */
@Composable
private fun OlloInput(value: String, change: (String) -> Unit, placeholder: String, description: String, modifier: Modifier = Modifier,
    enabled: Boolean = true, keyboardType: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(value, change, modifier = modifier.fillMaxWidth().heightIn(min = 56.dp).semantics { contentDescription = description },
        singleLine = true, enabled = enabled, shape = RoundedCornerShape(16.dp),
        textStyle = FFType.bodyMain.copy(fontSize = 17.sp, lineHeight = 22.sp, color = FFColors.onSurface),
        placeholder = { Text(placeholder, style = FFType.bodyMain.copy(fontSize = 17.sp, lineHeight = 22.sp), color = FFColors.outline) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = FFColors.surface, focusedContainerColor = FFColors.surface, disabledContainerColor = FFColors.surface,
            unfocusedBorderColor = Color.Transparent, focusedBorderColor = FFColors.outline, disabledBorderColor = Color.Transparent,
        ))
}

/** Kartu "Exclude from Total" / "Link as Pocket?" Ollo: judul, deskripsi, sakelar di kanan. */
@Composable
private fun ToggleCard(title: String, description: String, checked: Boolean, enabled: Boolean, tag: String, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(FFColors.surface)
        .clickable(enabled = enabled, role = Role.Switch) { change(!checked) }.testTag(tag).padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = FFType.bodyMain.copy(fontSize = 16.sp, lineHeight = 22.sp), fontWeight = FontWeight.Medium, color = FFColors.onSurface)
            Spacer(Modifier.height(2.dp))
            Text(description, style = FFType.metaData.copy(fontSize = 13.sp, lineHeight = 18.sp), color = FFColors.onSurfaceVariant)
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked, change, enabled = enabled, colors = olloSwitchColors())
    }
}

@Composable
private fun olloSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = FFColors.onPill, checkedTrackColor = FFColors.pill, checkedBorderColor = Color.Transparent,
    uncheckedThumbColor = FFColors.onSurfaceVariant, uncheckedTrackColor = FFColors.raised, uncheckedBorderColor = Color.Transparent,
)

/** Pilihan bank/e-wallet dengan badge merek; "Lainnya" untuk penyedia yang belum ada di katalog. */
@Composable
private fun ProviderPicker(type: String, selected: String?, enabled: Boolean, choose: (FinanceProvider?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(if (type == "bank") MR.strings.acc_choose_bank.text() else MR.strings.acc_choose_ewallet.text())
        val providers: List<FinanceProvider?> = financeProviders.filter { it.type == type } + listOf(null)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(FFColors.surface).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            providers.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { provider ->
                        if (provider != null) ProviderCell(provider.name, provider.id == selected, enabled, Modifier.weight(1f).testTag("provider-${provider.id}"), { choose(provider) }) {
                            ProviderBadge(provider, Modifier.size(40.dp))
                        } else ProviderCell(MR.strings.acc_provider_other.text(), selected == null, enabled, Modifier.weight(1f).testTag("provider-other"), { choose(null) }) {
                            Box(Modifier.size(40.dp).clip(CircleShape).background(FFColors.iconCircle), contentAlignment = Alignment.Center) {
                                FFIcon(if (type == "bank") "hi_bank" else "hi_ewallet", FFColors.onSurface, 20.dp)
                            }
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun ProviderCell(label: String, selected: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp))
            .background(if (selected) FFColors.raised else Color.Transparent)
            .then(if (selected) Modifier.border(1.5.dp, FFColors.onSurface, RoundedCornerShape(16.dp)) else Modifier)
            .semantics { this.selected = selected }
            .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        icon()
        Spacer(Modifier.height(6.dp))
        Text(label, style = FFType.metaData.copy(fontSize = 11.sp, lineHeight = 14.sp), color = FFColors.onSurface, maxLines = 2,
            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}
