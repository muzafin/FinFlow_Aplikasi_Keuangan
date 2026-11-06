package com.logikawan.finflow.feature.transaction
import com.logikawan.finflow.core.ui.WalletPicker
import com.logikawan.finflow.core.ui.WalletChoice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logikawan.finflow.core.data.FinanceRepository
import com.logikawan.finflow.core.model.FinanceData
import com.logikawan.finflow.core.model.Transaction
import com.logikawan.finflow.core.model.LocalPreferences
import com.logikawan.finflow.core.ui.Choice
import com.logikawan.finflow.core.ui.FigmaAsset
import com.logikawan.finflow.core.ui.FFColors
import com.logikawan.finflow.core.ui.FFIcon
import com.logikawan.finflow.core.ui.FFType
import com.logikawan.finflow.core.ui.categoryColor
import com.logikawan.finflow.core.ui.categoryIcon
import com.logikawan.finflow.core.util.currencyDigits
import com.logikawan.finflow.core.util.decimalAmount
import com.logikawan.finflow.core.util.minorAmount
import com.logikawan.finflow.core.util.newId
import com.logikawan.finflow.core.util.nowMillis
import com.logikawan.finflow.core.util.today
import com.logikawan.finflow.core.util.calculateAmount
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditor(old: Transaction?, data: FinanceData, busy: Boolean, dismiss: () -> Unit,
                      save: (Transaction) -> Unit, delete: (() -> Unit)?, isDraft: Boolean = false, startingScheduled: Boolean = false, startingAccountId: String? = null, startingType: String = "expense") {
    val preferences = LocalPreferences.current
    val id = remember { old?.id ?: newId() }
    var type by remember { mutableStateOf(old?.type ?: startingType) }
    var accountId by remember { mutableStateOf(old?.accountId
        ?: listOfNotNull(startingAccountId,preferences.defaultWalletId).firstOrNull{id->data.accounts.any{it.id==id && !it.archived && it.type!="savings"}}
        ?: data.accounts.firstOrNull { !it.archived && it.type!="savings" }?.id.orEmpty()) }
    var toId by remember { mutableStateOf(old?.toAccountId.orEmpty()) }
    fun initialCategory(kind:String):String {
        val last=if(kind=="income")preferences.lastIncomeCategory else preferences.lastExpenseCategory
        return data.categories.find{preferences.rememberCategory && it.id==last && it.type==kind && it.parentId==null}?.id
            ?:data.categories.firstOrNull{it.type==kind && it.parentId==null}?.id.orEmpty()
    }
    var categoryId by remember { mutableStateOf(old?.categoryId ?: initialCategory(type)) }
    var subcategoryId by remember { mutableStateOf(old?.subcategoryId) }
    var tags by remember { mutableStateOf(old?.tags ?: emptyList()) }
    var tagInput by remember { mutableStateOf("") }
    var tagDialog by remember { mutableStateOf(false) }
    var calculator by remember { mutableStateOf(false) }
    var expression by remember { mutableStateOf("") }
    var calculationError by remember { mutableStateOf<String?>(null) }
    val compact=preferences.transactionDisplay=="compact"
    var title by remember { mutableStateOf(old?.title.orEmpty()) }
    var description by remember { mutableStateOf(old?.description.orEmpty()) }
    val account = data.accounts.find { it.id == accountId }
    val target = data.accounts.find { it.id == toId }
    var amount by remember { mutableStateOf(old?.let { decimalAmount(it.amountMinor, it.currency) }.orEmpty()) }
    var received by remember { mutableStateOf(old?.receivedAmountMinor?.let { decimalAmount(it, target?.currency ?: old.currency) }.orEmpty()) }
    var date by remember { mutableStateOf(old?.date ?: today().toString()) }
    var scheduled by remember { mutableStateOf(old?.scheduled ?: startingScheduled) }
    var error by remember { mutableStateOf<String?>(null) }
    var pickingDate by remember { mutableStateOf(false) }
    var pickingAccount by remember { mutableStateOf(false) }
    val currentBusy by rememberUpdatedState(busy)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true,
        confirmValueChange = { !currentBusy || it != SheetValue.Hidden })
    val currency = account?.currency ?: old?.currency ?: "IDR"
    val typeLabel = when (type) { "income" -> "Pemasukan"; "transfer" -> "Transfer"; else -> "Pengeluaran" }
    val typeColor = when (type) { "income" -> FFColors.income; "transfer" -> Color(0xFF1E5BBA); else -> FFColors.expense }
    fun submit() {
        if (busy) return
        try {
            val source = requireNotNull(account) { "Buat dompet terlebih dahulu" }
            val selectedCategory = data.categories.find { it.id == categoryId && it.type == type }
            require(type == "transfer" || selectedCategory != null) { "Pilih kategori yang sesuai" }
            require(type != "transfer" || target != null && (target.currency == source.currency || received.isNotBlank())) {
                "Transfer beda mata uang wajib mengisi nominal yang diterima"
            }
            val tx = Transaction(id = id, type = type, amountMinor = minorAmount(amount, source.currency),
                currency = source.currency, accountId = source.id, categoryId = if (type == "transfer") "transfer" else categoryId,
                title = title.trim().ifBlank{data.categories.find{it.id==(subcategoryId ?: categoryId)}?.name ?: typeLabel}, description = description.trim(), date = date.trim(),
                toAccountId = if (type == "transfer") target?.id else null,
                receivedAmountMinor = if (type == "transfer") minorAmount(received.ifBlank { amount }, target?.currency ?: source.currency) else null,
                createdAt = old?.createdAt ?: nowMillis(), source = old?.source ?: "manual", sourceId = old?.sourceId, scheduled = scheduled,
                subcategoryId=if(type=="transfer")null else subcategoryId,tags=tags)
            FinanceRepository.validateTransaction(tx)
            error = null
            save(tx)
        } catch (e: Exception) { error = e.message ?: "Transaksi belum dapat disimpan" }
    }
    fun press(key: String) {
        if (busy) return
        error = null
        when (key) {
            "backspace" -> amount = amount.dropLast(1)
            "clear" -> amount = ""
            "calendar" -> pickingDate = true
            "confirm" -> submit()
            "." -> if (currencyDigits(currency) > 0 && !amount.contains('.')) amount = amount.ifEmpty { "0" } + "."
            else -> {
                val decimals = currencyDigits(currency)
                val available = if (amount.contains('.')) (decimals - amount.substringAfter('.').length).coerceAtLeast(0) else key.length
                val next = amount + key.take(available)
                if (next.length <= 20) amount = if (!next.contains('.')) next.trimStart('0').ifEmpty { "0" } else next
            }
        }
    }
    val windowHeight = with(androidx.compose.ui.platform.LocalDensity.current) { androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.height.toDp() }
    ModalBottomSheet(onDismissRequest = { if (!busy) dismiss() }, sheetState = sheetState,
        modifier = Modifier.fillMaxWidth().testTag("transaction-sheet"), sheetMaxWidth = androidx.compose.ui.unit.Dp.Unspecified,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp), containerColor = FFColors.canvas,
        dragHandle = { Box(Modifier.padding(top = 8.dp, bottom = 12.dp).size(36.dp, 4.dp).background(FFColors.outline, RoundedCornerShape(2.dp))) }) {
        Column(Modifier.fillMaxWidth().heightIn(max = windowHeight * .82f).imePadding().navigationBarsPadding()) {
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).testTag("transaction-form"),
                verticalArrangement = Arrangement.spacedBy(if(compact)6.dp else 12.dp)) {
                if (old != null || isDraft) Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isDraft) "Tinjau Transaksi" else "Edit Transaksi", style = FFType.sectionTitle, modifier = Modifier.weight(1f))
                    delete?.let { IconButton(onClick = it, enabled = !busy) { FFIcon("delete_outline_rounded", FFColors.expense, contentDescription = "Hapus transaksi") } }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("expense" to "Pengeluaran", "income" to "Pemasukan", "transfer" to "Transfer").forEach { (value, label) ->
                        Box(Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(100.dp))
                            .background(if (type == value) FFColors.onSurface else FFColors.surfaceLow)
                            .semantics { selected = type == value }
                            .clickable(enabled = !busy, role = Role.Tab) {
                                type = value; categoryId = initialCategory(value); subcategoryId=null; error = null
                            }, contentAlignment = Alignment.Center) {
                            Text(label, style = FFType.bodyMain.copy(fontSize = 12.sp), fontWeight = FontWeight.Bold,
                                color = if (type == value) FFColors.canvas else FFColors.onSurfaceVariant)
                        }
                    }
                }
                if (type != "transfer") {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        data.categories.filter { it.type == type && it.parentId==null }.forEach { category ->
                            val selected = categoryId == category.id
                            val color = category.color?.let{Color(it)} ?: categoryColor(category.id)
                            Column(Modifier.width(if(compact)60.dp else 68.dp).clickable(enabled = !busy, role = Role.RadioButton) { categoryId = category.id;subcategoryId=null }
                                .semantics { this.selected = selected }, horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.size(48.dp).clip(RoundedCornerShape(24.dp)).background(color.copy(alpha = .08f))
                                    .then(if (selected) Modifier.border(2.dp, color, RoundedCornerShape(24.dp)) else Modifier), contentAlignment = Alignment.Center) {
                                    FFIcon(category.icon.ifBlank{categoryIcon(category.id)}, color, 20.dp)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(category.name, style = FFType.badgeLabel.copy(fontSize = 10.sp, letterSpacing = 0.sp),
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (selected) FFColors.onSurface else FFColors.onSurfaceVariant,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    val children=data.categories.filter{it.parentId==categoryId && it.type==type}
                    if(children.isNotEmpty())Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        com.logikawan.finflow.core.ui.DesignPill("Semua",subcategoryId==null,{subcategoryId=null})
                        children.forEach{child->com.logikawan.finflow.core.ui.DesignPill(child.name,subcategoryId==child.id,{subcategoryId=child.id})}
                    }
                } else {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(onClick = { pickingAccount = true }, enabled = !busy, shape = RoundedCornerShape(16.dp), color = FFColors.surfaceLow, modifier = Modifier.weight(1f)) {
                            Column(Modifier.padding(12.dp)) { Text("Dari", style = FFType.metaData, color = FFColors.outline); Text(account?.name ?: "Pilih dompet", style = FFType.bodyMain, fontWeight = FontWeight.Bold) }
                        }
                        FFIcon("swap_horiz_rounded")
                        Column(Modifier.weight(1f)) { WalletChoice("Ke", toId, data, data.accounts.filter { !it.archived && it.id != accountId && it.type!="savings" },!busy) { toId = it } }
                    }
                }
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (currency == "IDR") "Rp" else currency, Modifier.clip(RoundedCornerShape(100.dp)).background(FFColors.surfaceLow).padding(horizontal = 12.dp, vertical = 6.dp),
                            style = FFType.bodyMain.copy(fontSize = 16.sp), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(12.dp))
                        BoxWithConstraints(Modifier.widthIn(max = 260.dp)) {
                            val shown = transactionInputAmount(amount, currency).removePrefix(if (currency == "IDR") "Rp" else "$currency ")
                            val size = (maxWidth.value / (shown.length.coerceAtLeast(1) * .62f)).coerceIn(18f, 48f)
                            Text(shown, style = FFType.amountLg.copy(fontSize = size.sp, lineHeight = 58.sp), color = FFColors.onSurface,
                                maxLines = 1, softWrap = false, modifier = Modifier.testTag("transaction-amount"))
                        }
                        Box(Modifier.width(2.dp).height(42.dp).background(FFColors.primary))
                    }
                    Text("Masukkan jumlah", style = FFType.metaData, color = FFColors.outline)
                    TextButton(enabled=!busy,onClick={expression=amount;calculationError=null;calculator=true}){FFIcon("calculate_outlined",size=18.dp);Text(" Kalkulator",style=FFType.metaData)}
                }
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(title, { title = it }, modifier = Modifier.fillMaxWidth().testTag("transaction-title"), enabled = !busy,
                        placeholder = { Text("Judul", style = FFType.bodyMain, color = FFColors.outline) }, leadingIcon = { FFIcon("edit_note_rounded", size = 20.dp) },
                        textStyle = FFType.bodyMain, singleLine = true, shape = RoundedCornerShape(20.dp),
                        colors = transactionFieldColors())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextField(description, { description = it }, modifier = Modifier.weight(1f), enabled = !busy,
                            placeholder = { Text("Tambah catatan...", style = FFType.bodyMain, color = FFColors.outline) },
                            leadingIcon = { FFIcon("edit_rounded", size = 20.dp) }, textStyle = FFType.bodyMain, singleLine = true,
                            shape = RoundedCornerShape(20.dp), colors = transactionFieldColors())
                        Surface(onClick = { pickingAccount = true }, enabled = !busy, shape = RoundedCornerShape(20.dp), color = FFColors.surfaceLow) {
                            Row(Modifier.widthIn(max = 110.dp).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(account?.name ?: "Dompet", style = FFType.bodyMain, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                                FFIcon("expand_more_rounded", size = 16.dp)
                            }
                        }
                    }
                    if (type == "transfer" && target != null && target.currency != currency) {
                        OutlinedTextField(received, { received = it }, label = { Text("Diterima (${target.currency})") }, enabled = !busy, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }
                    Row(verticalAlignment=Alignment.CenterVertically){TextButton(enabled=!busy,onClick={tagInput="";tagDialog=true}){FFIcon("label_outline",size=18.dp);Text(if(tags.isEmpty())" Tambah tag" else " "+tags.joinToString(" · "),maxLines=1,overflow=TextOverflow.Ellipsis)}}
                    if (scheduled || startingScheduled || old != null || date > today().toString()) Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(scheduled, { scheduled = it }, enabled = !busy)
                        Text("Terjadwal; belum masuk saldo", style = FFType.metaData)
                    }
                    error?.let { Text(it, style = FFType.bodyMain, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("transaction-error")) }
                }
            }
            TransactionKeypad(busy, date, typeLabel, ::press)
        }
    }

    if (pickingDate) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = runCatching { LocalDate.parse(date).toEpochDays().toLong() * 86_400_000L }.getOrNull())
        DatePickerDialog(onDismissRequest = { pickingDate = false }, confirmButton = {
            TextButton(enabled = picker.selectedDateMillis != null, onClick = {
                picker.selectedDateMillis?.let { date = LocalDate.fromEpochDays((it / 86_400_000L).toInt()).toString() }
                pickingDate = false
            }) { Text("Pilih") }
        }, dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Batal") } }) {
            DatePicker(picker)
        }
    }
    if (pickingAccount) WalletPicker(data, data.accounts.filter { (!it.archived || it.id == accountId) && it.type!="savings" }, accountId, { pickingAccount = false }) {
        accountId = it
        if (toId == accountId) toId = ""
        pickingAccount = false
    }
    if(tagDialog)AlertDialog(onDismissRequest={tagDialog=false},title={Text("Tag Transaksi")},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
        OutlinedTextField(tagInput,{tagInput=it.take(30)},label={Text("Tag baru")},singleLine=true)
        TextButton(enabled=tagInput.isNotBlank() && tags.size<10,onClick={tags=(tags+tagInput.trim()).distinct();tagInput=""}){Text("Tambahkan")}
        FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){tags.forEach{tag->InputChip(selected=true,onClick={tags=tags-tag},label={Text("$tag ×")})}}
        Text("Ketuk tag untuk menghapus. Maksimal 10 tag.",style=FFType.metaData)
    }},confirmButton={TextButton(onClick={tagDialog=false}){Text("Selesai")}})
    if(calculator)AlertDialog(onDismissRequest={calculator=false},title={Text("Kalkulator")},text={Column{
        OutlinedTextField(expression,{expression=it.take(100);calculationError=null},label={Text("Contoh: 10000+5000/2")},singleLine=true)
        Text("Gunakan + − * / dan titik untuk desimal.",style=FFType.metaData)
        calculationError?.let{Text(it,color=FFColors.expense)}
    }},confirmButton={TextButton(onClick={try{amount=decimalAmount(calculateAmount(expression,currency),currency);calculator=false}catch(e:Exception){calculationError=e.message}}){Text("Gunakan Hasil")}},dismissButton={TextButton(onClick={calculator=false}){Text("Batal")}})
}

private fun transactionInputAmount(input: String, currency: String): String {
    val parts = input.ifEmpty { "0" }.split('.')
    val whole = parts[0].ifEmpty { "0" }.reversed().chunked(3).joinToString(".").reversed()
    return (if (currency == "IDR") "Rp" else "$currency ") + whole + if (parts.size > 1) ",${parts[1]}" else ""
}

private fun transactionDateLabel(date: String): String = runCatching {
    val value = LocalDate.parse(date)
    val months = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
    "${value.dayOfMonth} ${months[value.monthNumber - 1]} ${value.year}"
}.getOrDefault(date)

@Composable
private fun transactionFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = FFColors.surfaceLow, unfocusedContainerColor = FFColors.surfaceLow,
    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent)

@Composable
private fun TransactionKeypad(busy: Boolean, date: String, typeLabel: String, press: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().testTag("transaction-keypad")) {
        val keys = listOf(listOf("1", "2", "3", "backspace"), listOf("4", "5", "6", "clear"), listOf("7", "8", "9", "calendar"), listOf(".", "0", "000", "confirm"))
        keys.forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { key ->
                    val bg = when (key) { "backspace" -> FFColors.expense.copy(alpha=.16f); "confirm" -> FFColors.primary; else -> FFColors.canvas }
                    Box(Modifier.weight(1f).height(54.dp).background(bg).border(.5.dp, FFColors.outlineVariant)
                        .testTag("key-$key").semantics { contentDescription = when (key) {
                            "backspace" -> "Hapus angka terakhir"; "clear" -> "Kosongkan nominal"; "calendar" -> "Pilih tanggal"; "confirm" -> "Simpan $typeLabel"; else -> key
                        } }.clickable(enabled = !busy, role = Role.Button) { press(key) }, contentAlignment = Alignment.Center) {
                        when (key) {
                            "backspace" -> FFIcon("backspace_outlined", FFColors.expense, 22.dp)
                            "clear" -> FFIcon("close_rounded", FFColors.onSurfaceVariant, 22.dp)
                            "confirm" -> if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp) else FFIcon("check", MaterialTheme.colorScheme.onPrimary, 24.dp)
                            "calendar" -> Text(transactionDateLabel(date), style = FFType.metaData.copy(fontSize = 10.sp), color = FFColors.onSurfaceVariant, maxLines = 1)
                            else -> Text(key, style = FFType.numpadDigit, color = FFColors.onSurface)
                        }
                    }
                }
            }
        }
    }
}

