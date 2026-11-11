package com.clonical.kasiva.feature.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonical.kasiva.core.model.FinanceData
import com.clonical.kasiva.core.model.LocalPreferences
import kotlin.math.pow
import com.clonical.kasiva.core.model.Transaction
import com.clonical.kasiva.core.model.maskedBalance
import com.clonical.kasiva.core.ui.FFColors
import com.clonical.kasiva.core.ui.FFIcon
import com.clonical.kasiva.core.ui.FFType
import com.clonical.kasiva.core.ui.categoryColor
import com.clonical.kasiva.core.ui.categoryIcon
import com.clonical.kasiva.core.util.compactDisplayMoney
import com.clonical.kasiva.core.util.monthLabel
import com.clonical.kasiva.core.util.shiftMonth
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.i18n.transactionTypeLabel
import com.clonical.kasiva.i18n.*
import dev.icerock.moko.resources.StringResource

/**
 * Riwayat Transaksi bergaya "Transaction Log" Ollo 1.7.9: kolom cari, tujuh filter dropdown
 * (periode, jenis, kategori, urutan, dompet, rentang nominal, tag) dan baris datar per tanggal.
 */
@Composable
fun Transactions(data: FinanceData, month: String, monthChange: (String) -> Unit, edit: (Transaction) -> Unit, onBack: () -> Unit = {}, initialType: String = "all") {
    val preferences = LocalPreferences.current
    var query by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable(initialType) { mutableStateOf(initialType) }
    var allMonths by rememberSaveable { mutableStateOf(false) }
    var category by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf("newest") }
    var wallet by rememberSaveable { mutableStateOf("") }
    var range by rememberSaveable { mutableStateOf("all") }
    var tag by rememberSaveable { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    fun reset() { query = ""; type = initialType; allMonths = false; category = ""; sort = "newest"; wallet = ""; range = "all"; tag = "" }
    val filtered = data.transactions.filter { tx ->
        // `month` adalah kunci periode pencatatan, bukan bulan kalender (P3-02).
        (allMonths || tx.date in com.clonical.kasiva.core.util.recordingPeriod(month, preferences.recordingDay)) &&
            (type == "scheduled" && tx.scheduled || type != "scheduled" && !tx.scheduled && (type == "all" || tx.type == type)) &&
            (category.isEmpty() || tx.categoryId == category || tx.subcategoryId == category) &&
            (wallet.isEmpty() || tx.accountId == wallet || tx.toAccountId == wallet) &&
            (tag.isEmpty() || tag in tx.tags) &&
            // Ambang dalam satuan utama mata uang (100rb / 1jt), bukan satuan minor tetap (P3-08).
            (tx.amountMinor.toDouble() / 10.0.pow(com.clonical.kasiva.core.util.currencyDigits(tx.currency))).let { major -> when (range) { "small" -> major < 100_000; "mid" -> major in 100_000.0..1_000_000.0; "large" -> major > 1_000_000; else -> true } } &&
            (query.isBlank() || "${tx.title} ${tx.description} ${tx.tags.joinToString(" ")} ${data.accountName(tx.accountId)} ${data.categoryName(tx.categoryId)}".contains(query, ignoreCase = true))
    }.let { list ->
        when (sort) {
            "oldest" -> list.sortedWith(compareBy<Transaction> { it.date }.thenBy { it.createdAt })
            "largest" -> list.sortedByDescending { it.amountMinor }
            "smallest" -> list.sortedBy { it.amountMinor }
            else -> list.sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.createdAt })
        }
    }
    val byDate = sort == "newest" || sort == "oldest"
    val months = (0 until 12).map { shiftMonth(month.takeIf { allMonths.not() } ?: month, -it) }.distinct()

    Column(Modifier.fillMaxSize().background(FFColors.surface).testTag("transaction-log")) {
        Box(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 8.dp).height(56.dp)) {
            IconButton(onBack, Modifier.align(Alignment.CenterStart)) { FFIcon("hi_chevron_left", FFColors.onSurface, 24.dp, contentDescription = MR.strings.common_back.text()) }
            Text(MR.strings.tx_history_title.text(), Modifier.align(Alignment.Center), style = FFType.appTitle)
            Box(Modifier.align(Alignment.CenterEnd)) {
                IconButton({ menu = true }, Modifier.testTag("transaction-log-menu")) { FFIcon("more_vert", FFColors.onSurface, 24.dp, contentDescription = MR.strings.tx_history_menu.text()) }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text(MR.strings.tx_reset_filters.text()) }, onClick = { menu = false; reset() })
                }
            }
        }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp)).background(FFColors.background).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                FFIcon("hi_search", FFColors.onSurfaceVariant, 22.dp)
                Spacer(Modifier.width(16.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text(MR.strings.tx_search_placeholder.text(), style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
                    BasicTextField(query, { query = it }, Modifier.fillMaxWidth().testTag("transaction-search").semantics { contentDescription = MR.strings.tx_search_description.text() }, singleLine = true,
                        textStyle = FFType.bodyMain.copy(color = FFColors.onSurface), cursorBrush = SolidColor(FFColors.onSurface))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterPill(if (allMonths) MR.strings.tx_all_time.text() else shortMonthLabel(month), listOf("all" to MR.strings.tx_all_time.text()) + months.map { it to monthLabel(it) }, Modifier.weight(1f), "filter-period") {
                    if (it == "all") allMonths = true else { allMonths = false; monthChange(it) }
                }
                FilterPill(when (type) { "expense", "income", "transfer" -> transactionTypeLabel(type); "scheduled" -> MR.strings.tx_scheduled.text(); else -> MR.strings.common_all.text() },
                    listOf("all" to MR.strings.common_all.text(), "expense" to transactionTypeLabel("expense"), "income" to transactionTypeLabel("income"), "transfer" to transactionTypeLabel("transfer"), "scheduled" to MR.strings.tx_scheduled.text()), Modifier.weight(1f), "filter-type") { type = it }
                FilterPill(if (category.isEmpty()) MR.strings.tx_category.text() else data.categoryName(category),
                    listOf("" to MR.strings.tx_all_categories.text()) + data.categories.filter { it.parentId == null }.map { it.id to it.name }, Modifier.weight(1f), "filter-category") { category = it }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val sorts = listOf("newest" to MR.strings.tx_sort_newest.text(), "oldest" to MR.strings.tx_sort_oldest.text(), "largest" to MR.strings.tx_sort_largest.text(), "smallest" to MR.strings.tx_sort_smallest.text())
                FilterPill(sorts.first { it.first == sort }.second, sorts, Modifier.weight(1f), "filter-sort") { sort = it }
                FilterPill(if (wallet.isEmpty()) MR.strings.tx_all_wallets.text() else data.accountName(wallet),
                    listOf("" to MR.strings.tx_all_wallets.text()) + data.accounts.filterNot { it.archived }.map { it.id to it.name }, Modifier.weight(1f), "filter-wallet") { wallet = it }
                val ranges = listOf("all" to MR.strings.tx_amount.text(), "small" to MR.strings.tx_range_small.text(), "mid" to MR.strings.tx_range_mid.text(), "large" to MR.strings.tx_range_large.text())
                FilterPill(ranges.first { it.first == range }.second, ranges, Modifier.weight(1f), "filter-range") { range = it }
            }
            val tags = data.transactions.flatMap { it.tags }.distinct().sorted()
            FilterPill(if (tag.isEmpty()) MR.strings.tx_all_tags.text() else "#$tag", listOf("" to MR.strings.tx_all_tags.text()) + tags.map { it to "#$it" }, Modifier.fillMaxWidth(), "filter-tag") { tag = it }
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(FFColors.hairline))
        if (filtered.isEmpty()) {
            Column(Modifier.weight(1f).fillMaxWidth().padding(bottom = 100.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                FFIcon("hi_search", FFColors.onSurfaceVariant.copy(alpha = .45f), 56.dp)
                Spacer(Modifier.height(14.dp))
                Text(if (data.transactions.isEmpty()) MR.strings.tx_empty_none.text() else MR.strings.tx_empty_no_match.text(), style = FFType.bodyMain.copy(fontSize = 15.sp), color = FFColors.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(if (data.transactions.isEmpty()) MR.strings.tx_empty_none_hint.text() else MR.strings.tx_empty_no_match_hint.text(), style = FFType.metaData, color = FFColors.onSurfaceVariant.copy(alpha = .75f), textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(Modifier.weight(1f).testTag("transaction-log-list"), contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp)) {
                if (byDate) filtered.groupBy { it.date.take(10) }.forEach { (date, rows) ->
                    item("date-$date") { Text(transactionDateLabel(date), Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp), style = FFType.metaData, color = FFColors.onSurfaceVariant) }
                    items(rows, key = { it.id }) { tx -> TransactionCard(tx, data) { edit(tx) } }
                } else items(filtered, key = { it.id }) { tx -> TransactionCard(tx, data) { edit(tx) } }
            }
        }
    }
}

@Composable
private fun FilterPill(label: String, options: List<Pair<String, String>>, modifier: Modifier, tag: String, select: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        val shape = RoundedCornerShape(20.dp)
        Row(Modifier.fillMaxWidth().height(38.dp).clip(shape).border(1.dp, FFColors.hairline, shape).clickable(role = Role.Button) { open = true }.padding(horizontal = 10.dp).testTag(tag),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f, false), style = FFType.metaData.copy(fontSize = 10.5.sp), color = FFColors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(4.dp)); FFIcon("hi_chevron_down", FFColors.onSurface, 14.dp)
        }
        DropdownMenu(open, { open = false }) {
            options.forEach { (value, text) -> DropdownMenuItem(text = { Text(text) }, onClick = { open = false; select(value) }) }
        }
    }
}

/** Baris transaksi datar seperti log Ollo: ikon kategori, judul, "Kategori • Dompet", nominal ringkas, jam + tanggal. */
@Composable
fun TransactionCard(tx: Transaction, data: FinanceData, click: () -> Unit) {
    val category = data.categories.find { it.id == tx.categoryId }
    val tone = when (tx.type) { "income" -> FFColors.income; "transfer" -> FFColors.transfer; else -> category?.color?.let { androidx.compose.ui.graphics.Color(it) } ?: categoryColor(tx.categoryId) }
    val amountColor = when (tx.type) { "income" -> FFColors.income; "expense" -> FFColors.expense; "transfer" -> FFColors.transfer; else -> FFColors.onSurfaceVariant }
    val prefix = when (tx.type) { "income" -> "+"; "expense" -> "-"; else -> "" }
    val preferences = LocalPreferences.current
    val sub = if (tx.type == "transfer") "${data.accountName(tx.accountId)} → ${tx.toAccountId?.let(data::accountName) ?: MR.strings.tx_destination_wallet.text()}"
        else "${category?.name ?: data.categoryName(tx.categoryId)} • ${data.accountName(tx.accountId)}"
    val stamp = runCatching {
        val t = Instant.fromEpochMilliseconds(tx.createdAt).toLocalDateTime(TimeZone.currentSystemDefault())
        val d = LocalDate.parse(tx.date)
        MR.strings.tx_stamp.text("${t.hour.toString().padStart(2, '0')}:${t.minute.toString().padStart(2, '0')}", d.dayOfMonth.toString().padStart(2, '0'), txShortMonth(d.monthNumber), d.year)
    }.getOrDefault(tx.date)
    Row(Modifier.fillMaxWidth().clickable(onClick = click).padding(horizontal = 16.dp, vertical = 10.dp).testTag("log-${tx.id}"), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(tone.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
            FFIcon(if (tx.type == "transfer") "hi_swap" else category?.icon?.takeIf { it.isNotBlank() } ?: categoryIcon(tx.categoryId), tone, 22.dp)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(tx.title.ifBlank { data.categoryName(tx.categoryId) }, style = FFType.bodyMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(sub, style = FFType.metaData.copy(fontSize = 11.sp), color = FFColors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (tx.scheduled) Text(MR.strings.tx_scheduled_not_affecting.text(), style = FFType.metaData.copy(fontSize = 10.5.sp), color = FFColors.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(prefix + if (preferences.hideBalances) maskedBalance(tx.amountMinor, tx.currency, preferences) else compactDisplayMoney(tx.amountMinor, tx.currency), style = FFType.bodyMain.copy(fontSize = 15.sp), color = amountColor, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(stamp, style = FFType.metaData.copy(fontSize = 10.5.sp), color = FFColors.onSurfaceVariant, maxLines = 1)
        }
    }
}

private val shortMonths: List<StringResource> = listOf(
    MR.strings.tx_month_short_1, MR.strings.tx_month_short_2, MR.strings.tx_month_short_3, MR.strings.tx_month_short_4,
    MR.strings.tx_month_short_5, MR.strings.tx_month_short_6, MR.strings.tx_month_short_7, MR.strings.tx_month_short_8,
    MR.strings.tx_month_short_9, MR.strings.tx_month_short_10, MR.strings.tx_month_short_11, MR.strings.tx_month_short_12,
)
private val longMonths: List<StringResource> = listOf(
    MR.strings.tx_month_long_1, MR.strings.tx_month_long_2, MR.strings.tx_month_long_3, MR.strings.tx_month_long_4,
    MR.strings.tx_month_long_5, MR.strings.tx_month_long_6, MR.strings.tx_month_long_7, MR.strings.tx_month_long_8,
    MR.strings.tx_month_long_9, MR.strings.tx_month_long_10, MR.strings.tx_month_long_11, MR.strings.tx_month_long_12,
)
private val shortWeekdays: List<StringResource> = listOf(
    MR.strings.tx_weekday_short_1, MR.strings.tx_weekday_short_2, MR.strings.tx_weekday_short_3, MR.strings.tx_weekday_short_4,
    MR.strings.tx_weekday_short_5, MR.strings.tx_weekday_short_6, MR.strings.tx_weekday_short_7,
)
private val longWeekdays: List<StringResource> = listOf(
    MR.strings.tx_weekday_long_1, MR.strings.tx_weekday_long_2, MR.strings.tx_weekday_long_3, MR.strings.tx_weekday_long_4,
    MR.strings.tx_weekday_long_5, MR.strings.tx_weekday_long_6, MR.strings.tx_weekday_long_7,
)

/** Nama bulan singkat terlokalisasi ("Jan".."Des"); [month] 1..12. */
fun txShortMonth(month: Int): String = shortMonths[month - 1].text()

/** Nama hari singkat terlokalisasi ("Sen".."Min"); [isoDay] 1 = Senin .. 7 = Minggu. */
fun txShortWeekday(isoDay: Int): String = shortWeekdays[isoDay - 1].text()

/** "12 Jan" (tanggal + bulan singkat), urutan mengikuti bahasa. */
fun txDayMonth(date: LocalDate): String = MR.strings.tx_day_month.text(date.dayOfMonth, txShortMonth(date.monthNumber))

private fun shortMonthLabel(month: String) = runCatching { MR.strings.tx_month_year.text(txShortMonth(month.substring(5, 7).toInt()), month.take(4)) }.getOrDefault(month)

private fun transactionDateLabel(value: String): String {
    val date = runCatching { LocalDate.parse(value) }.getOrNull() ?: return value
    return MR.strings.tx_date_long.text(longWeekdays[date.dayOfWeek.ordinal].text(), date.dayOfMonth, longMonths[date.monthNumber - 1].text(), date.year)
}
