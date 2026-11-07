package com.logikawan.finflow.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.logikawan.finflow.core.model.*
import com.logikawan.finflow.core.util.compactDisplayMoney
import com.logikawan.finflow.core.util.formatBalance
import com.logikawan.finflow.i18n.*
import com.logikawan.finflow.core.i18n.text

/*
 * Dialog pemilih dompet bergaya Ollo: kartu tengah 358dp, judul dengan ikon, pencarian, chip
 * jenis dompet, dan grid 4 kolom berisi kartu dompet. Warna memakai token tema FinFlow yang
 * setara dengan palet netral Ollo (surface, surface container, outline) agar mode gelap tetap aman.
 */

private val walletChipTypes = listOf(
    "all" to MR.strings.common_all,
    "cash" to MR.strings.app_wallet_type_cash,
    "bank" to MR.strings.app_wallet_type_bank,
    "ewallet" to MR.strings.app_wallet_type_ewallet,
    "savings" to MR.strings.app_wallet_type_savings,
    "credit" to MR.strings.app_wallet_type_credit,
    "loan" to MR.strings.app_wallet_type_loan,
    "pocket" to MR.strings.app_wallet_type_pocket,
)

private fun matchesType(account: Account, type: String): Boolean = when (type) {
    "all" -> true
    "pocket" -> account.parentId != null
    "savings" -> account.parentId == null && (account.type == "investment" || account.icon == "savings")
    "credit" -> account.parentId == null && account.type == "credit"
    "loan" -> account.parentId == null && account.type == "loan"
    "cash", "bank", "ewallet" -> account.parentId == null && account.type == type
    else -> true
}

@Composable
fun WalletPicker(data: FinanceData, accounts: List<Account>, selected: String, dismiss: () -> Unit, select: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("all") }
    var list by remember { mutableStateOf(false) }
    val preferences = LocalPreferences.current
    val visible = accounts.filter { account ->
        (query.isBlank() || "${account.name} ${account.currency}".contains(query.trim(), true)) && matchesType(account, type)
    }
    Dialog(onDismissRequest = dismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
            Column(
                Modifier.testTag("wallet-picker")
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(FFColors.canvas),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(FFColors.surfaceLow),
                        contentAlignment = Alignment.Center,
                    ) { FFIcon("settings_outlined", FFColors.onSurfaceVariant, 22.dp) }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        MR.strings.app_choose_wallet_title.text(),
                        Modifier.weight(1f),
                        style = FFType.sectionTitle.copy(fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
                        color = FFColors.onSurface,
                    )
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(FFColors.surfaceLow).clickable(onClick = dismiss).testTag("wallet-picker-close"),
                        contentAlignment = Alignment.Center,
                    ) { FFIcon("close_rounded", FFColors.onSurfaceVariant, 20.dp, MR.strings.common_close.text()) }
                }
                HorizontalDivider(thickness = 1.dp, color = FFColors.outlineVariant)
                Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp).weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WalletSearchField(query, { query = it }, Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier.size(38.dp).clip(RoundedCornerShape(14.dp)).background(FFColors.surfaceLow)
                                .clickable { list = !list }.testTag("wallet-picker-layout"),
                            contentAlignment = Alignment.Center,
                        ) { FFIcon(if (list) "view_module_outlined" else "format_list_bulleted_outlined", FFColors.onSurfaceVariant, 20.dp) }
                    }
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(walletChipTypes, key = { it.first }) { (key, label) ->
                            WalletFilterChip(label.text(), type == key) { type = key }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(bottom = 20.dp)) {
                        if (visible.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                                    Text(MR.strings.app_no_matching_wallet.text(), style = FFType.bodyMain, color = FFColors.outline)
                                }
                            }
                        }
                        items(visible.chunked(if (list) 1 else 4)) { row ->
                            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { account ->
                                    WalletGridCard(
                                        account = account,
                                        balance = if (preferences.hideBalances) "••••••" else compactDisplayMoney(data.balance(account), account.currency),
                                        selected = account.id == selected,
                                        modifier = Modifier.weight(1f),
                                    ) { select(account.id) }
                                }
                                if (!list) repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletSearchField(value: String, change: (String) -> Unit, modifier: Modifier = Modifier) {
    BasicTextField(
        value = value,
        onValueChange = change,
        modifier = modifier.height(38.dp),
        singleLine = true,
        textStyle = FFType.bodyMain.copy(color = FFColors.onSurface),
        cursorBrush = SolidColor(FFColors.primary),
        decorationBox = { innerTextField ->
            Row(
                Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)).background(FFColors.surfaceLow).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FFIcon("search_outlined", FFColors.outline, 20.dp)
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(MR.strings.app_search_wallet.text(), style = FFType.bodyMain, color = FFColors.outline, maxLines = 1)
                    innerTextField()
                }
            }
        },
    )
}

@Composable
private fun WalletFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(100.dp)
    Box(
        Modifier.height(34.dp)
            .clip(shape)
            .background(if (selected) FFColors.surfaceContainerHigh else FFColors.surfaceLow)
            .then(if (selected) Modifier.border(1.dp, FFColors.outline, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = FFType.metaData,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) FFColors.onSurface else FFColors.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun WalletGridCard(account: Account, balance: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.height(88.dp)) {
        Column(
            Modifier.fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .background(if (selected) FFColors.surfaceContainerHigh else FFColors.surfaceLow)
                .then(if (selected) Modifier.border(1.dp, FFColors.outline, RoundedCornerShape(14.dp)) else Modifier)
                .clickable(onClick = onClick)
                .padding(top = 12.dp, bottom = 4.dp, start = 4.dp, end = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(FFColors.surfaceContainerHighest), contentAlignment = Alignment.Center) {
                AccountIconContent(account, account.icon, FFColors.onSurface, 22.dp)
            }
            Spacer(Modifier.height(4.dp))
            Text(account.name, style = FFType.metaData.copy(fontSize = 12.sp, lineHeight = 16.sp), fontWeight = FontWeight.SemiBold,
                color = FFColors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Box(Modifier.padding(horizontal = 2.dp)) {
                FitMoneyText(balance, FFType.metaData.copy(fontSize = 10.sp, lineHeight = 14.sp), FFColors.outline, TextAlign.Center)
            }
        }
        if (selected) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 2.dp).size(18.dp).clip(CircleShape)
                    .background(FFColors.canvas).border(1.dp, FFColors.outline, CircleShape),
                contentAlignment = Alignment.Center,
            ) { FFIcon("check_rounded", FFColors.onSurface, 11.dp) }
        }
    }
}

@Composable
fun WalletChoice(label: String, value: String, data: FinanceData, accounts: List<Account>, enabled: Boolean = true, allowNone: Boolean = false, change: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 72.dp).clip(RoundedCornerShape(16.dp)).background(FFColors.surfaceLow)
                .then(if (enabled) Modifier.clickable { open = true } else Modifier)
                .testTag("wallet-choice-$label").padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val account = accounts.find { it.id == value }
            AccountIcon(account, "account_balance_wallet_outlined", if (account == null) FFColors.outline else FFColors.onSurface, 24.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(account?.name ?: if (allowNone && value.isBlank()) MR.strings.app_no_link.text() else MR.strings.app_choose_wallet.text(),
                    style = FFType.bodyMain.copy(fontSize = 16.sp, lineHeight = 22.sp), fontWeight = FontWeight.SemiBold, color = FFColors.onSurface)
                account?.let {
                    FitMoneyText(
                        if (LocalPreferences.current.hideBalances) "••••••" else formatBalance(data.balance(it), it.currency, LocalPreferences.current),
                        FFType.bodyMain, FFColors.outline,
                    )
                }
            }
            FFIcon("expand_more_rounded", FFColors.outline, 20.dp)
        }
        if (allowNone && value.isNotBlank()) TextButton(enabled = enabled, onClick = { change("") }) { Text(MR.strings.app_unlink_wallet.text()) }
    }
    if (open) WalletPicker(data, accounts, value, { open = false }) { change(it); open = false }
}