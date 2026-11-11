package com.clonical.kasiva.feature.auth

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
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.ui.FFColors
import com.clonical.kasiva.core.ui.FFIcon
import com.clonical.kasiva.i18n.*

/*
 * Palet netral Ollo, sengaja eksplisit agar dialog tidak mewarisi peran warna Material3.
 * Mengikuti tema: mode terang memakai nilai Ollo asli, mode gelap memakai padanan gelapnya
 * (sebelumnya selalu putih sehingga lembar ini menyilaukan di tema gelap).
 */
private val pickerInk: Color @Composable get() = if (FFColors.isDark) Color(0xFFE4E4E7) else Color(0xFF1D1E26)
private val pickerOnInk: Color @Composable get() = if (FFColors.isDark) Color(0xFF18181B) else Color.White
private val pickerMuted: Color @Composable get() = if (FFColors.isDark) Color(0xFF9A9AA1) else Color(0xFF818185)
private val pickerSheet: Color @Composable get() = if (FFColors.isDark) Color(0xFF0F0F12) else Color.White
private val pickerSurface: Color @Composable get() = if (FFColors.isDark) Color(0xFF1A1A1F) else Color(0xFFF7F7F8)
private val pickerSelectedSurface: Color @Composable get() = if (FFColors.isDark) Color(0xFF26262C) else Color(0xFFEFEFEF)
private val pickerLine: Color @Composable get() = if (FFColors.isDark) Color(0xFF2E2E34) else Color(0xFFE5E7EB)
private val pickerGrabber: Color @Composable get() = if (FFColors.isDark) Color(0xFF45454C) else Color(0xFFD5D6DB)

/** Nama Inggris untuk seluruh kode pada [setupAllCurrencyCodes]. */
private val currencyNames = mapOf(
    "AED" to "UAE Dirham",
    "AFN" to "Afghani",
    "ALL" to "Lek",
    "AMD" to "Armenian Dram",
    "AOA" to "Kwanza",
    "ARS" to "Argentine Peso",
    "AUD" to "Australian Dollar",
    "AWG" to "Aruban Florin",
    "AZN" to "Azerbaijan Manat",
    "BAM" to "Convertible Mark",
    "BBD" to "Barbados Dollar",
    "BDT" to "Bangladeshi Taka",
    "BHD" to "Bahraini Dinar",
    "BIF" to "Burundi Franc",
    "BMD" to "Bermudian Dollar",
    "BND" to "Brunei Dollar",
    "BOB" to "Boliviano",
    "BRL" to "Brazilian Real",
    "BSD" to "Bahamian Dollar",
    "BTN" to "Ngultrum",
    "BWP" to "Pula",
    "BYN" to "Belarusian Ruble",
    "BZD" to "Belize Dollar",
    "CAD" to "Canadian Dollar",
    "CDF" to "Congolese Franc",
    "CHF" to "Swiss Franc",
    "CLP" to "Chilean Peso",
    "CNY" to "Chinese Yuan",
    "COP" to "Colombian Peso",
    "CRC" to "Costa Rican Colon",
    "CUP" to "Cuban Peso",
    "CVE" to "Cabo Verde Escudo",
    "CZK" to "Czech Koruna",
    "DJF" to "Djibouti Franc",
    "DKK" to "Danish Krone",
    "DOP" to "Dominican Peso",
    "DZD" to "Algerian Dinar",
    "EGP" to "Egyptian Pound",
    "ERN" to "Nakfa",
    "ETB" to "Ethiopian Birr",
    "EUR" to "Euro",
    "FJD" to "Fiji Dollar",
    "FKP" to "Falkland Islands Pound",
    "GBP" to "British Pound",
    "GEL" to "Lari",
    "GHS" to "Ghana Cedi",
    "GIP" to "Gibraltar Pound",
    "GMD" to "Dalasi",
    "GNF" to "Guinean Franc",
    "GTQ" to "Quetzal",
    "GYD" to "Guyana Dollar",
    "HKD" to "Hong Kong Dollar",
    "HNL" to "Lempira",
    "HTG" to "Gourde",
    "HUF" to "Hungarian Forint",
    "IDR" to "Indonesian Rupiah",
    "ILS" to "Israeli New Shekel",
    "INR" to "Indian Rupee",
    "IQD" to "Iraqi Dinar",
    "IRR" to "Iranian Rial",
    "ISK" to "Icelandic Krona",
    "JMD" to "Jamaican Dollar",
    "JOD" to "Jordanian Dinar",
    "JPY" to "Japanese Yen",
    "KES" to "Kenyan Shilling",
    "KGS" to "Som",
    "KHR" to "Riel",
    "KMF" to "Comorian Franc",
    "KPW" to "North Korean Won",
    "KRW" to "South Korean Won",
    "KWD" to "Kuwaiti Dinar",
    "KYD" to "Cayman Islands Dollar",
    "KZT" to "Tenge",
    "LAK" to "Lao Kip",
    "LBP" to "Lebanese Pound",
    "LKR" to "Sri Lankan Rupee",
    "LRD" to "Liberian Dollar",
    "LSL" to "Loti",
    "LYD" to "Libyan Dinar",
    "MAD" to "Moroccan Dirham",
    "MDL" to "Moldovan Leu",
    "MGA" to "Malagasy Ariary",
    "MKD" to "Denar",
    "MMK" to "Kyat",
    "MNT" to "Tugrik",
    "MOP" to "Pataca",
    "MRU" to "Ouguiya",
    "MUR" to "Mauritius Rupee",
    "MVR" to "Rufiyaa",
    "MWK" to "Malawi Kwacha",
    "MXN" to "Mexican Peso",
    "MYR" to "Malaysian Ringgit",
    "MZN" to "Mozambique Metical",
    "NAD" to "Namibia Dollar",
    "NGN" to "Naira",
    "NIO" to "Cordoba Oro",
    "NOK" to "Norwegian Krone",
    "NPR" to "Nepalese Rupee",
    "NZD" to "New Zealand Dollar",
    "OMR" to "Omani Rial",
    "PAB" to "Balboa",
    "PEN" to "Sol",
    "PGK" to "Kina",
    "PHP" to "Philippine Peso",
    "PKR" to "Pakistani Rupee",
    "PLN" to "Polish Zloty",
    "PYG" to "Guarani",
    "QAR" to "Qatari Riyal",
    "RON" to "Romanian Leu",
    "RSD" to "Serbian Dinar",
    "RUB" to "Russian Ruble",
    "RWF" to "Rwanda Franc",
    "SAR" to "Saudi Riyal",
    "SBD" to "Solomon Islands Dollar",
    "SCR" to "Seychelles Rupee",
    "SDG" to "Sudanese Pound",
    "SEK" to "Swedish Krona",
    "SGD" to "Singapore Dollar",
    "SHP" to "Saint Helena Pound",
    "SLE" to "Leone",
    "SOS" to "Somali Shilling",
    "SRD" to "Surinam Dollar",
    "SSP" to "South Sudanese Pound",
    "STN" to "Dobra",
    "SVC" to "El Salvador Colon",
    "SYP" to "Syrian Pound",
    "SZL" to "Lilangeni",
    "THB" to "Thai Baht",
    "TJS" to "Somoni",
    "TMT" to "Turkmenistan New Manat",
    "TND" to "Tunisian Dinar",
    "TOP" to "Pa’anga",
    "TRY" to "Turkish Lira",
    "TTD" to "Trinidad and Tobago Dollar",
    "TWD" to "New Taiwan Dollar",
    "TZS" to "Tanzanian Shilling",
    "UAH" to "Ukrainian Hryvnia",
    "UGX" to "Uganda Shilling",
    "USD" to "US Dollar",
    "UYU" to "Peso Uruguayo",
    "UZS" to "Uzbekistan Sum",
    "VES" to "Bolívar Soberano",
    "VND" to "Vietnamese Dong",
    "VUV" to "Vatu",
    "WST" to "Tala",
    "XAF" to "CFA Franc BEAC",
    "XCD" to "East Caribbean Dollar",
    "XCG" to "Caribbean Guilder",
    "XOF" to "CFA Franc BCEAO",
    "XPF" to "CFP Franc",
    "YER" to "Yemeni Rial",
    "ZAR" to "South African Rand",
    "ZMW" to "Zambian Kwacha",
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

/** Nama Inggris mata uang (kode bila tidak dikenal). */
fun currencyDisplayName(code: String): String = currencyName(code)

/** Seluruh kode mata uang negara yang didukung, alfabetis. */
fun allCurrencyCodes(): List<String> = pickerCodes

private fun currencySymbol(code: String): String = currencySymbols[code] ?: code

private val pickerCodes: List<String> = setupAllCurrencyCodes.sorted()

/** Urutan Ollo pada pemilih mata uang transaksi: mata uang populer dulu, sisanya alfabetis. */
private val popularCodes = listOf("IDR", "USD", "EUR", "GBP", "JPY", "CNY", "HKD", "SGD", "MYR", "AUD", "KRW", "SAR", "THB")

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
    /** Mata uang bawaan (diberi lencana "Bawaan" dan ditaruh paling atas), gaya pemilih transaksi Ollo. */
    defaultCode: String? = null,
    /** Ikon roda gigi di judul: membuka pengaturan mata uang bawaan. */
    onSettings: (() -> Unit)? = null,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val keyword = query.trim()
    val ordered = if (defaultCode == null) pickerCodes
        else (listOf(defaultCode) + popularCodes.filter { it in pickerCodes } + pickerCodes).distinct()
    val codes = if (keyword.isEmpty()) {
        ordered
    } else {
        ordered.filter { code ->
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
                    .background(pickerSheet)
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
                    if (onSettings != null) IconButton(onClick = onSettings, modifier = Modifier.testTag("currency-picker-settings")) {
                        FFIcon("settings_outlined", pickerInk, 22.dp, MR.strings.tx_currency_settings.text())
                    }
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
                                codeFirst = defaultCode != null,
                                isDefault = code == defaultCode,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencyRow(code: String, name: String, symbol: String, selected: Boolean, onClick: () -> Unit,
                        codeFirst: Boolean = false, isDefault: Boolean = false) {
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
                color = if (selected) pickerOnInk else pickerInk,
                maxLines = 1,
                softWrap = false,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (codeFirst) code else name,
                    style = TextStyle(fontSize = if (codeFirst) 16.sp else 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
                    color = pickerInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isDefault) {
                    Spacer(Modifier.width(8.dp))
                    Text(MR.strings.tx_currency_default_badge.text(),
                        Modifier.clip(RoundedCornerShape(8.dp)).background(pickerLine).padding(horizontal = 8.dp, vertical = 2.dp),
                        style = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium), color = pickerInk)
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(if (codeFirst) name else code, style = TextStyle(fontSize = 12.sp, lineHeight = 16.sp), color = pickerMuted)
        }
        if (selected) {
            Spacer(Modifier.width(8.dp))
            FFIcon("check_rounded", pickerInk, 18.dp)
        }
    }
}
