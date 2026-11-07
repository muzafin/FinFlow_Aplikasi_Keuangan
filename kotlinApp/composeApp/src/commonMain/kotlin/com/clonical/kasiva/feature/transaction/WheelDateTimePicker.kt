package com.clonical.kasiva.feature.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.ui.FFColors
import com.clonical.kasiva.core.ui.FFType
import com.clonical.kasiva.core.util.shortMonthName
import com.clonical.kasiva.core.util.today
import com.clonical.kasiva.i18n.*
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

private val wheelItemHeight = 40.dp
private const val wheelVisibleRows = 5

private fun daysInMonth(year: Int, month: Int): Int =
    LocalDate(year, month, 1).plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).dayOfMonth

/**
 * Pemilih tanggal gaya roda (Preferensi → Gaya Pemilih Tanggal = "wheel"): roda hari/bulan/tahun dan,
 * bila [showTime], jam/menit. Dibangun dari LazyColumn yang dipaskan ke baris tengah setelah gulir berhenti.
 */
@Composable
internal fun WheelDateTimeDialog(
    initialDate: LocalDate,
    initialHour: Int,
    initialMinute: Int,
    showTime: Boolean,
    dismiss: () -> Unit,
    confirm: (LocalDate, Int, Int) -> Unit,
) {
    val currentYear = today().year
    val years = remember(initialDate.year, currentYear) {
        (minOf(initialDate.year, currentYear - 50)..maxOf(initialDate.year, currentYear + 10)).toList()
    }
    var year by remember { mutableIntStateOf(initialDate.year) }
    var month by remember { mutableIntStateOf(initialDate.monthNumber) }
    var day by remember { mutableIntStateOf(initialDate.dayOfMonth) }
    var hour by remember { mutableIntStateOf(initialHour.coerceIn(0, 23)) }
    var minute by remember { mutableIntStateOf(initialMinute.coerceIn(0, 59)) }
    val maxDay = daysInMonth(year, month)
    LaunchedEffect(maxDay) { if (day > maxDay) day = maxDay }
    AlertDialog(
        onDismissRequest = dismiss,
        modifier = Modifier.testTag("wheel-date-picker"),
        title = { Text(MR.strings.tx_pick_date.text(), style = FFType.sectionTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Wheel((1..maxDay).map { it.toString().padStart(2, '0') }, day.coerceAtMost(maxDay) - 1, { day = it + 1 }, "wheel-day", Modifier.weight(1f))
                    Wheel((1..12).map { shortMonthName(it) }, month - 1, { month = it + 1 }, "wheel-month", Modifier.weight(1.3f))
                    Wheel(years.map { it.toString() }, years.indexOf(year).coerceAtLeast(0), { year = years[it] }, "wheel-year", Modifier.weight(1.3f))
                }
                if (showTime) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(.5f))
                    Wheel((0..23).map { it.toString().padStart(2, '0') }, hour, { hour = it }, "wheel-hour", Modifier.weight(1f))
                    Text(":", style = FFType.bodyMain.copy(fontSize = 20.sp), color = FFColors.onSurface)
                    Wheel((0..59).map { it.toString().padStart(2, '0') }, minute, { minute = it }, "wheel-minute", Modifier.weight(1f))
                    Spacer(Modifier.weight(.5f))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { confirm(LocalDate(year, month, day.coerceIn(1, maxDay)), hour, minute) }, modifier = Modifier.testTag("wheel-date-confirm")) {
                Text(MR.strings.tx_pick.text())
            }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text(MR.strings.common_cancel.text()) } },
        containerColor = FFColors.canvas,
    )
}

@Composable
private fun Wheel(values: List<String>, index: Int, onIndex: (Int) -> Unit, tag: String, modifier: Modifier = Modifier, itemHeight: Dp = wheelItemHeight) {
    val lastIndex = values.lastIndex.coerceAtLeast(0)
    val selectedIndex = index.coerceIn(0, lastIndex)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val scope = rememberCoroutineScope()
    val itemPx = with(LocalDensity.current) { itemHeight.toPx() }
    val currentOnIndex by rememberUpdatedState(onIndex)
    val currentLast by rememberUpdatedState(lastIndex)
    fun centered(): Int = (state.firstVisibleItemIndex + if (state.firstVisibleItemScrollOffset > itemPx / 2) 1 else 0).coerceIn(0, currentLast)
    // Pas ke baris tengah setelah gulir/fling selesai, lalu laporkan nilainya.
    LaunchedEffect(state.isScrollInProgress) {
        if (!state.isScrollInProgress) {
            val target = centered()
            if (state.firstVisibleItemIndex != target || state.firstVisibleItemScrollOffset != 0) state.animateScrollToItem(target)
            currentOnIndex(target)
        }
    }
    // Nilai diubah dari luar (mis. hari dipotong saat bulan berganti) → posisikan ulang roda.
    LaunchedEffect(selectedIndex, lastIndex) {
        if (!state.isScrollInProgress && centered() != selectedIndex) state.scrollToItem(selectedIndex)
    }
    val center = centered()
    Box(modifier.height(itemHeight * wheelVisibleRows).testTag(tag)) {
        Box(Modifier.align(Alignment.Center).fillMaxWidth().height(itemHeight).clip(RoundedCornerShape(12.dp)).background(FFColors.surfaceLow))
        LazyColumn(Modifier.fillMaxSize(), state = state, contentPadding = PaddingValues(vertical = itemHeight * (wheelVisibleRows / 2)),
            horizontalAlignment = Alignment.CenterHorizontally) {
            itemsIndexed(values) { i, label ->
                val chosen = i == center
                Box(Modifier.fillMaxWidth().height(itemHeight).clickable { scope.launch { state.animateScrollToItem(i); currentOnIndex(i) } }
                    .testTag("$tag-$i"), contentAlignment = Alignment.Center) {
                    Text(label, style = FFType.bodyMain.copy(fontSize = if (chosen) 18.sp else 15.sp),
                        fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (chosen) FFColors.onSurface else FFColors.onSurfaceVariant, maxLines = 1, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
