package com.logikawan.finflow.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.logikawan.finflow.core.i18n.text
import com.logikawan.finflow.core.ui.FFIcon
import com.logikawan.finflow.i18n.*

/* Palet netral Ollo, sengaja eksplisit agar dialog tidak mewarisi peran warna Material3. */
private val pickerInk = Color(0xFF1D1E26)
private val pickerMuted = Color(0xFF818185)
private val pickerSurface = Color(0xFFF7F7F8)
private val pickerSelectedSurface = Color(0xFFEFEFEF)
private val pickerLine = Color(0xFFE5E7EB)
private val pickerGrabber = Color(0xFFD5D6DB)

/** Nama Inggris untuk seluruh kode pada [setupAllCurrencyCodes]. */
private val currencyNames = mapOf(
    "AED" to "UAE Dirham",
    "AUD" to "Australian Dollar",
    "BDT" to "Bangladeshi Taka",
    "BHD" to "Bahraini Dinar",
    "BRL" to "Brazilian Real",
    "CAD" to "Canadian Dollar",
    "CHF" to "Swiss Franc",
    "CNY" to "Chinese Yuan",
    "CZK" to "Czech Koruna",
    "DKK" to "Danish Krone",
    "EGP" to "Egyptian Pound",
    "EUR" to "Euro",
    "GBP" to "British Pound",
    "HKD" to "Hong Kong Dollar",
    "HUF" to "Hungarian Forint",
    "IDR" to "Indonesian Rupiah",
    "ILS" to "Israeli New Shekel",
    "INR" to "Indian Rupee",
    "ISK" to "Icelandic Krona",
    "JPY" to "Japanese Yen",
    "KRW" to "South Korean Won",
    "KWD" to "Kuwaiti Dinar",
    "LKR" to "Sri Lankan Rupee",
    "MAD" to "Moroccan Dirham",
    "MXN" to "Mexican Peso",
    "MYR" to "Malaysian Ringgit",
    "NOK" to "Norwegian Krone",
    "NZD" to "New Zealand Dollar",
    "OMR" to "Omani Rial",
    "PHP" to "Philippine Peso",
    "PKR" to "Pakistani Rupee",
    "PLN" to "Polish Zloty",
    "QAR" to "Qatari Riyal",
    "RON" to "Romanian Leu",
    "RUB" to "Russian Ruble",
    "SAR" to "Saudi Riyal",
    "SEK" to "Swedish Krona",
    "SGD" to "Singapore Dollar",
    "THB" to "Thai Baht",
    "TRY" to "Turkish Lira",
    "TWD" to "New Taiwan Dollar",
    "UAH" to "Ukrainian Hryvnia",
    "USD" to "US Dollar",
    "VND" to "Vietnamese Dong",
    "ZAR" to "South African Rand",
)

/** Simbol tampil untuk tiap kode; kode dipakai sebagai fallback bila tidak tercatat. */
private val currencySymbols = mapOf(
    "AED" to "د.إ",
    "AUD" to "A\$",
    "BDT" to "৳",
    "BHD" to "د.ب",
    "BRL" to "R\$",
    "CAD" to "C\$",
    "CHF" to "CHF",
    "CNY" to "¥",
    "CZK" to "Kč",
    "DKK" to "kr",
    "EGP" to "ج.م",
    "EUR" to "€",
    "GBP" to "£",
    "HKD" to "HK\$",
    "HUF" to "Ft",
    "IDR" to "Rp",
    "ILS" to "₪",
    "INR" to "₹",
    "ISK" to "kr",
    "JPY" to "¥",
    "KRW" to "₩",
    "KWD" to "د.ك",
    "LKR" to "Rs",
    "MAD" to "د.م",
    "MXN" to "MX\$",
    "MYR" to "RM",
    "NOK" to "kr",
    "NZD" to "NZ\$",
    "OMR" to "﷼",
    "PHP" to "₱",
    "PKR" to "₨",
    "PLN" to "zł",
    "QAR" to "﷼",
    "RON" to "lei",
    "RUB" to "₽",
    "SAR" to "﷼",
    "SEK" to "kr",
    "SGD" to "S\$",
    "THB" to "฿",
    "TRY" to "₺",
    "TWD" to "NT\$",
    "UAH" to "₴",
    "USD" to "\$",
    "VND" to "₫",
    "ZAR" to "R",
)

private fun currencyName(code: String): String = currencyNames[code] ?: code

private fun currencySymbol(code: String): String = currencySymbols[code] ?: code

private val pickerCodes: List<String> = setupAllCurrencyCodes.sorted()

/**
 * Dialog "Lihat Semua" mata uang bergaya Ollo: lembar bawah putih dengan pencarian dan daftar
 * seluruh [setupAllCurrencyCodes]. Warna seluruhnya eksplisit netral sehingga tidak memakai
 * surfaceContainerHigh ungu bawaan Material3.
 */
@Composable
fun OlloCurrencyDialog(
    title: String,
    closeLabel: String,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val keyword = query.trim()
    val codes = if (keyword.isEmpty()) {
        pickerCodes
    } else {
        pickerCodes.filter { code ->
            code.contains(keyword, ignoreCase = true) || currencyName(code).contains(keyword, ignoreCase = true)
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            val scrimInteraction = remember { MutableInteractionSource() }
            Box(
                Modifier.fillMaxSize()
                    .clickable(interactionSource = scrimInteraction, indication = null, onClick = onDismiss),
            )
            Column(
                Modifier.testTag("currency-picker")
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Color.White)
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                Box(
                    Modifier.padding(top = 10.dp).size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp))
                        .background(pickerGrabber).align(Alignment.CenterHorizontally),
                )
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        title,
                        Modifier.weight(1f),
                        style = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold),
                        color = pickerInk,
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("currency-picker-close")) {
                        FFIcon("close_rounded", pickerInk, 22.dp, closeLabel)
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).testTag("currency-picker-search"),
                    placeholder = {
                        Text(MR.strings.auth_currency_search.text(), style = TextStyle(fontSize = 15.sp, lineHeight = 20.sp), color = pickerMuted)
                    },
                    leadingIcon = { FFIcon("search_outlined", pickerMuted, 20.dp) },
                    trailingIcon = if (query.isNotEmpty()) {
                        {
                            Box(
                                Modifier.size(40.dp).clickable { query = "" },
                                contentAlignment = Alignment.Center,
                            ) { FFIcon("clear", pickerMuted, 18.dp, MR.strings.auth_clear_search.text()) }
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    textStyle = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, color = pickerInk),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = pickerInk,
                        unfocusedTextColor = pickerInk,
                        cursorColor = pickerInk,
                        focusedContainerColor = pickerSurface,
                        unfocusedContainerColor = pickerSurface,
                        focusedBorderColor = pickerInk,
                        unfocusedBorderColor = pickerLine,
                        focusedPlaceholderColor = pickerMuted,
                        unfocusedPlaceholderColor = pickerMuted,
                    ),
                )
                Spacer(Modifier.height(10.dp))
                if (codes.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            MR.strings.auth_currency_not_found.text(),
                            Modifier.testTag("currency-picker-empty"),
                            style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
                            color = pickerMuted,
                        )
                    }
                } else {
                    LazyColumn(
                        Modifier.fillMaxWidth().weight(1f).testTag("currency-picker-list"),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(codes, key = { it }) { code ->
                            CurrencyRow(
                                code = code,
                                name = currencyName(code),
                                symbol = currencySymbol(code),
                                selected = code == selected,
                                onClick = { onSelect(code) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencyRow(code: String, name: String, symbol: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().testTag("currency-picker-row-" + code)
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) pickerSelectedSurface else pickerSurface)
            .border(2.dp, if (selected) pickerInk else Color.Transparent, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(if (selected) pickerInk else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                symbol,
                style = TextStyle(
                    fontSize = when {
                        selected -> 15.sp
                        symbol.length >= 3 -> 13.sp
                        else -> 19.sp
                    },
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = if (selected) Color.White else pickerInk,
                maxLines = 1,
                softWrap = false,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                name,
                style = TextStyle(fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold),
                color = pickerInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(code, style = TextStyle(fontSize = 12.sp, lineHeight = 16.sp), color = pickerMuted)
        }
        if (selected) {
            Spacer(Modifier.width(8.dp))
            FFIcon("check_rounded", pickerInk, 18.dp)
        }
    }
}
