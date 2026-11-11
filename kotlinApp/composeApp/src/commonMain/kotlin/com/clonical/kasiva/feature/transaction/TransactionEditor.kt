package com.clonical.kasiva.feature.transaction
import com.clonical.kasiva.core.ui.WalletPicker
import com.clonical.kasiva.core.ui.WalletChoice

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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonical.kasiva.core.data.FinanceRepository
import com.clonical.kasiva.core.model.FinanceData
import com.clonical.kasiva.core.model.Transaction
import com.clonical.kasiva.core.model.LocalPreferences
import com.clonical.kasiva.core.ui.Choice
import com.clonical.kasiva.core.ui.FigmaAsset
import com.clonical.kasiva.core.ui.FFColors
import com.clonical.kasiva.core.ui.FFIcon
import com.clonical.kasiva.core.ui.FFType
import com.clonical.kasiva.core.ui.categoryColor
import com.clonical.kasiva.core.ui.categoryIcon
import com.clonical.kasiva.core.util.currencyDigits
import com.clonical.kasiva.core.util.decimalAmount
import com.clonical.kasiva.core.util.minorAmount
import com.clonical.kasiva.core.util.newId
import com.clonical.kasiva.core.util.nowMillis
import com.clonical.kasiva.core.util.today
import com.clonical.kasiva.core.util.calculateAmount
import com.clonical.kasiva.core.util.orderedBy
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.i18n.transactionTypeLabel
import com.clonical.kasiva.i18n.*
import kotlin.math.pow
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditor(old: Transaction?, data: FinanceData, busy: Boolean, dismiss: () -> Unit,
                      save: (Transaction) -> Unit, delete: (() -> Unit)?, isDraft: Boolean = false, startingScheduled: Boolean = false, startingAccountId: String? = null, startingType: String = "expense", onScan: (() -> Unit)? = null, openCurrencySettings: (() -> Unit)? = null,
                      /** Simpan pengeluaran baru beserta klaim reimburse-nya (chip "Reimburse" Ollo). */
                      saveReimbursed: ((Transaction, Long) -> Unit)? = null) {
    val preferences = LocalPreferences.current
    val id = remember { old?.id ?: newId() }
    var type by remember { mutableStateOf(old?.type ?: startingType) }
    var accountId by remember { mutableStateOf(old?.accountId
        ?: listOfNotNull(startingAccountId,preferences.defaultWalletId).firstOrNull{id->data.accounts.any{it.id==id && !it.archived && it.type!="savings"}}
        // Mata uang transaksi bawaan: transaksi Kasiva selalu memakai mata uang dompetnya (aturan Firestore:
        // currency transaksi == currency dompet), jadi preferensi ini diterapkan dengan memilih dompet aktif pertama
        // bermata uang tersebut saat tidak ada dompet bawaan. Tanpa dompet yang cocok, perilaku lama dipakai.
        ?: preferences.defaultTransactionCurrency.takeIf{it.isNotEmpty()}?.let{code->data.accounts.firstOrNull{!it.archived && it.type!="savings" && it.currency==code}?.id}
        ?: data.accounts.firstOrNull { !it.archived && it.type!="savings" }?.id.orEmpty()) }
    var toId by remember { mutableStateOf(old?.toAccountId.orEmpty()) }
    fun initialCategory(kind:String):String {
        val last=if(kind=="income")preferences.lastIncomeCategory else preferences.lastExpenseCategory
        return data.categories.find{preferences.rememberCategory && it.id==last && it.type==kind && it.parentId==null}?.id
            ?:data.categories.filter{it.type==kind && it.parentId==null}.orderedBy(preferences.categoryOrder){it.id}.firstOrNull()?.id.orEmpty()
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
    // Mata uang ketikan (Ollo "Select Currency"); null = ikut mata uang dompet. Disimpan dalam mata uang dompet
    // setelah dikonversi dengan kurs harian, jumlah aslinya di originalCurrency/originalAmountMinor.
    var entryCurrency by remember { mutableStateOf(old?.originalCurrency) }
    var pickingCurrency by remember { mutableStateOf(false) }
    // Chip Ollo di bawah subkategori (khusus pengeluaran): Reimburse, Split Bill (segera hadir), CC/Paylater.
    var reimburseMinor by remember { mutableStateOf<Long?>(null) }
    var editingReimburse by remember { mutableStateOf(false) }
    var pickingCredit by remember { mutableStateOf(false) }
    var walletBeforeCredit by remember { mutableStateOf<String?>(null) }
    var chipMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(chipMessage) { if (chipMessage != null) { kotlinx.coroutines.delay(2500); chipMessage = null } }
    var amount by remember { mutableStateOf(old?.let { o -> o.originalAmountMinor?.let { decimalAmount(it, o.originalCurrency ?: o.currency) } ?: decimalAmount(o.amountMinor, o.currency) }.orEmpty()) }
    var received by remember { mutableStateOf(old?.receivedAmountMinor?.let { decimalAmount(it, target?.currency ?: old.currency) }.orEmpty()) }
    var date by remember { mutableStateOf(old?.date ?: today().toString()) }
    var scheduled by remember { mutableStateOf(old?.scheduled ?: startingScheduled) }
    var error by remember { mutableStateOf<String?>(null) }
    var pickingDate by remember { mutableStateOf(false) }
    // Waktu pilihan roda (hanya transaksi baru): createdAt tidak boleh berubah saat edit (aturan Firestore).
    var pickedCreatedAt by remember { mutableStateOf<Long?>(null) }
    var pickingAccount by remember { mutableStateOf(false) }
    val currentBusy by rememberUpdatedState(busy)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true,
        confirmValueChange = { !currentBusy || it != SheetValue.Hidden })
    val walletCurrency = account?.currency ?: old?.currency ?: "IDR"
    val currency = if (type == "transfer") walletCurrency else entryCurrency ?: walletCurrency
    val converting = currency != walletCurrency
    var rate by remember { mutableStateOf<Double?>(null) }
    var rateError by remember { mutableStateOf(false) }
    LaunchedEffect(currency, walletCurrency) {
        rate = null; rateError = false
        if (converting) try { rate = com.clonical.kasiva.feature.settings.loadExchangeRates(currency).rates[walletCurrency]; rateError = rate == null }
            catch (e: Exception) { rateError = true }
    }
    /** Jumlah dalam mata uang dompet (minor), atau null bila kurs belum ada. */
    fun walletMinor(): Long? {
        val typed = runCatching { minorAmount(amount.ifEmpty { "0" }, currency) }.getOrNull() ?: return null
        if (!converting) return typed
        if (old != null && old.originalCurrency == currency && old.originalAmountMinor == typed && old.currency == walletCurrency) return old.amountMinor
        val r = rate ?: return null
        val major = typed / 10.0.pow(currencyDigits(currency)) * r
        return (major * 10.0.pow(currencyDigits(walletCurrency))).roundToLong()
    }
    val typeLabel = transactionTypeLabel(when (type) { "income", "transfer" -> type; else -> "expense" })
    val typeColor = when (type) { "income" -> FFColors.income; "transfer" -> Color(0xFF1E5BBA); else -> FFColors.expense }
    fun submit() {
        if (busy) return
        try {
            val source = requireNotNull(account) { MR.strings.tx_err_create_wallet_first.text() }
            val selectedCategory = data.categories.find { it.id == categoryId && it.type == type }
            require(type == "transfer" || selectedCategory != null) { MR.strings.tx_err_pick_matching_category.text() }
            require(type != "transfer" || target != null && (target.currency == source.currency || received.isNotBlank())) {
                MR.strings.tx_err_transfer_received.text()
            }
            val converted = if (converting) requireNotNull(walletMinor()?.takeIf { it > 0 }) { MR.strings.tx_err_rate_unavailable.text(currency, walletCurrency) } else null
            val tx = Transaction(id = id, type = type, amountMinor = converted ?: minorAmount(amount, source.currency),
                currency = source.currency, accountId = source.id, categoryId = if (type == "transfer") "transfer" else categoryId,
                title = title.trim().ifBlank{data.categories.find{it.id==(subcategoryId ?: categoryId)}?.name ?: typeLabel}, description = description.trim(), date = date.trim(),
                toAccountId = if (type == "transfer") target?.id else null,
                receivedAmountMinor = if (type != "transfer") null else if (target == null || target.currency == source.currency) (converted ?: minorAmount(amount, source.currency)) else minorAmount(received, target.currency), // P3-01: sesama mata uang selalu = nominal
                createdAt = old?.createdAt ?: pickedCreatedAt ?: nowMillis(), source = old?.source ?: "manual", sourceId = old?.sourceId, scheduled = scheduled,
                subcategoryId=if(type=="transfer")null else subcategoryId,tags=tags,
                originalCurrency = if (converting) currency else null, originalAmountMinor = if (converting) minorAmount(amount, currency) else null)
            FinanceRepository.validateTransaction(tx)
            val claim = reimburseMinor?.takeIf { type == "expense" && saveReimbursed != null }
            require(claim == null || claim <= tx.amountMinor) { MR.strings.tx_reimburse_too_large.text() }
            error = null
            if (claim != null) saveReimbursed!!(tx, claim) else save(tx)
        } catch (e: Exception) { error = e.message ?: MR.strings.tx_err_cannot_save.text() }
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
        Column(Modifier.fillMaxWidth().heightIn(max = windowHeight * .94f).imePadding().navigationBarsPadding()) {
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).testTag("transaction-form"),
                verticalArrangement = Arrangement.spacedBy(if(compact)6.dp else 12.dp)) {
                if (old != null || isDraft) Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isDraft) MR.strings.tx_review_transaction.text() else MR.strings.tx_edit_transaction.text(), style = FFType.sectionTitle, modifier = Modifier.weight(1f))
                    delete?.let { IconButton(onClick = it, enabled = !busy) { FFIcon("delete_outline_rounded", FFColors.expense, contentDescription = MR.strings.tx_delete_transaction.text()) } }
                }
                // Ollo: segmen jenis di kiri (wadah #101014, terpilih #E4E4E7) dan tombol kamera (AI Scan) di kanan.
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.clip(RoundedCornerShape(24.dp)).background(FFColors.surface).padding(4.dp)) {
                        listOf("expense", "income", "transfer").map { it to transactionTypeLabel(it) }.forEach { (value, label) ->
                            val chosen = type == value
                            Box(Modifier.height(40.dp).clip(RoundedCornerShape(20.dp))
                                .background(if (chosen) FFColors.pill else Color.Transparent)
                                .semantics { selected = chosen }
                                .clickable(enabled = !busy, role = Role.Tab) {
                                    type = value; categoryId = initialCategory(value); subcategoryId=null; error = null
                                    if (value == "transfer") entryCurrency = null
                                }.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                                Text(label, style = FFType.bodyMain.copy(fontSize = 13.5.sp),
                                    color = if (chosen) FFColors.onPill else FFColors.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (onScan != null && old == null && !isDraft) Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(FFColors.surface)
                        .clickable(enabled = !busy, role = Role.Button, onClick = onScan).testTag("transaction-scan"), contentAlignment = Alignment.Center) {
                        FFIcon("hi_camera", FFColors.onSurface, 24.dp, contentDescription = MR.strings.tx_scan_receipt_description.text())
                    }
                }
                if (type != "transfer") {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        data.categories.filter { it.type == type && it.parentId==null }.orderedBy(preferences.categoryOrder) { it.id }.forEach { category ->
                            val selected = categoryId == category.id
                            val color = category.color?.let{Color(it)} ?: categoryColor(category.id)
                            Column(Modifier.widthIn(min = if(compact)62.dp else 72.dp, max = 128.dp).clickable(enabled = !busy, role = Role.RadioButton) { categoryId = category.id;subcategoryId=null }
                                .semantics { this.selected = selected }, horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.size(if(compact)50.dp else 56.dp).clip(CircleShape).background(if (selected) color else color.copy(alpha = .10f)), contentAlignment = Alignment.Center) {
                                    FFIcon(category.icon.ifBlank{categoryIcon(category.id)}, if (selected) Color.White else color, 24.dp)
                                }
                                Spacer(Modifier.height(7.dp))
                                Text(category.name, style = FFType.metaData.copy(fontSize = 10.5.sp, letterSpacing = 0.sp),
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = if (selected) FFColors.onSurface else FFColors.onSurfaceVariant,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    val children=data.categories.filter{it.parentId==categoryId && it.type==type}
                    if(children.isNotEmpty())Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        children.forEach{child->
                            val chosen=subcategoryId==child.id
                            val shape=RoundedCornerShape(24.dp)
                            Row(Modifier.height(46.dp).clip(shape).background(if(chosen)FFColors.pill else FFColors.chip).semantics{selected=chosen}
                                .clickable(enabled=!busy,role=Role.RadioButton){subcategoryId=if(chosen)null else child.id}.padding(horizontal=16.dp).testTag("subcategory-${child.id}"),
                                verticalAlignment=Alignment.CenterVertically){
                                FFIcon(child.icon.ifBlank{categoryIcon(child.id)},if(chosen)FFColors.onPill else FFColors.onSurfaceVariant,20.dp)
                                Spacer(Modifier.width(10.dp))
                                Text(child.name,style=FFType.metaData,color=if(chosen)FFColors.onPill else FFColors.onSurface,maxLines=1)
                            }
                        }
                    }
                    if (type == "expense") Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp).testTag("expense-options"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (saveReimbursed != null && old == null) OptionChip(
                            reimburseMinor?.let { MR.strings.tx_reimburse_chip_amount.text(transactionInputAmount(decimalAmount(it, walletCurrency).let { d -> if (d.contains('.')) d.trimEnd('0').trimEnd('.') else d }, walletCurrency)) }
                                ?: MR.strings.tx_reimburse.text(), reimburseMinor != null, "option-reimburse", !busy) { editingReimburse = true }
                        OptionChip(MR.strings.tx_split_bill.text(), false, "option-split-bill", !busy) { chipMessage = MR.strings.tx_split_bill_soon.text() }
                        val creditChosen = account?.type == "credit"
                        OptionChip(if (creditChosen) MR.strings.tx_credit_chip_wallet.text(account?.name.orEmpty()) else MR.strings.tx_credit_paylater.text(),
                            creditChosen, "option-credit", !busy) {
                            when {
                                creditChosen -> { accountId = walletBeforeCredit?.takeIf { id -> data.accounts.any { it.id == id && !it.archived } } ?: accountId; walletBeforeCredit = null }
                                data.accounts.none { it.type == "credit" && !it.archived } -> chipMessage = MR.strings.tx_credit_none.text()
                                else -> pickingCredit = true
                            }
                        }
                    }
                    chipMessage?.let { Text(it, Modifier.padding(horizontal = 20.dp).testTag("option-message"), style = FFType.metaData, color = FFColors.onSurfaceVariant) }
                } else {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(onClick = { pickingAccount = true }, enabled = !busy, shape = RoundedCornerShape(16.dp), color = FFColors.surfaceLow, modifier = Modifier.weight(1f)) {
                            Column(Modifier.padding(12.dp)) { Text(MR.strings.tx_from.text(), style = FFType.metaData, color = FFColors.outline); Text(account?.name ?: MR.strings.tx_pick_wallet.text(), style = FFType.bodyMain, fontWeight = FontWeight.Bold) }
                        }
                        FFIcon("swap_horiz_rounded")
                        Column(Modifier.weight(1f)) { WalletChoice(MR.strings.tx_to.text(), toId, data, data.accounts.filter { !it.archived && it.id != accountId && it.type!="savings" },!busy) { toId = it } }
                    }
                }
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.clip(RoundedCornerShape(16.dp)).background(FFColors.raised).clickable(enabled = !busy) { if (type == "transfer") pickingAccount = true else pickingCurrency = true }
                            .padding(horizontal = 14.dp, vertical = 8.dp).testTag("transaction-currency"), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (currency == "IDR") "Rp" else currency, style = FFType.bodyMain.copy(fontSize = 18.sp))
                            Spacer(Modifier.width(6.dp)); FFIcon("hi_chevron_down", FFColors.onSurface, 18.dp)
                        }
                        Spacer(Modifier.width(14.dp))
                        BoxWithConstraints(Modifier.widthIn(max = 260.dp)) {
                            val shown = transactionInputAmount(amount, currency).removePrefix(if (currency == "IDR") "Rp" else "$currency ")
                            val size = (maxWidth.value / (shown.length.coerceAtLeast(1) * .62f)).coerceIn(18f, 44f)
                            Text(shown, style = FFType.amountLg.copy(fontSize = size.sp, lineHeight = 54.sp, fontWeight = FontWeight.Medium), color = FFColors.onSurface,
                                maxLines = 1, softWrap = false, modifier = Modifier.testTag("transaction-amount"))
                        }
                        Box(Modifier.width(2.dp).height(40.dp).background(FFColors.onSurfaceVariant))
                    }
                    Text(MR.strings.tx_enter_amount.text(), style = FFType.metaData, color = FFColors.onSurfaceVariant)
                    if (converting) Text(
                        when {
                            rateError -> MR.strings.tx_err_rate_unavailable.text(currency, walletCurrency)
                            rate == null -> MR.strings.tx_rate_loading.text()
                            else -> MR.strings.tx_converted_to_wallet.text(transactionInputAmount(decimalAmount(walletMinor() ?: 0, walletCurrency), walletCurrency), account?.name.orEmpty())
                        }, style = FFType.metaData, color = if (rateError) FFColors.expense else FFColors.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp).testTag("transaction-converted"))
                }
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextField(title, { title = it }, modifier = Modifier.weight(1f).testTag("transaction-title"), enabled = !busy,
                            placeholder = { Text(MR.strings.tx_title.text(), style = FFType.bodyMain, color = FFColors.onSurfaceVariant) }, leadingIcon = { FFIcon("hi_text", FFColors.onSurfaceVariant, 22.dp) },
                            textStyle = FFType.bodyMain, singleLine = true, shape = RoundedCornerShape(16.dp),
                            colors = transactionFieldColors())
                        Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(FFColors.surface).clickable(enabled = !busy) { tagInput = ""; tagDialog = true }
                            .testTag("transaction-tags"), contentAlignment = Alignment.Center) {
                            FFIcon("hi_tag", if (tags.isEmpty()) FFColors.onSurface else FFColors.income, 24.dp, contentDescription = MR.strings.tx_tags_description.text())
                            if (tags.isNotEmpty()) Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(16.dp).clip(CircleShape).background(FFColors.income), contentAlignment = Alignment.Center) {
                                Text("${tags.size}", style = FFType.metaData.copy(fontSize = 9.sp, lineHeight = 10.sp), color = Color.White)
                            }
                        }
                    }
                    // Isi cepat: judul transaksi sebelumnya (jenis sama) yang memuat teks ketikan, terbaru dulu.
                    val quickFillSuggestions = remember(preferences.quickFill, title, type, data.transactions) {
                        val query = title.trim()
                        if (!preferences.quickFill || query.isEmpty()) emptyList()
                        else data.transactions.asSequence()
                            .filter { it.type == type && it.id != id && it.title.isNotBlank() && it.title.contains(query, true) && !it.title.trim().equals(query, true) }
                            .sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.createdAt })
                            .distinctBy { it.title.trim().lowercase() }.take(5).toList()
                    }
                    if (quickFillSuggestions.isNotEmpty()) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("quick-fill"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        quickFillSuggestions.forEachIndexed { index, suggestion ->
                            Box(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(FFColors.chip)
                                .clickable(enabled = !busy, role = Role.Button) {
                                    title = suggestion.title.trim()
                                    if (suggestion.type != "transfer" && data.categories.any { it.id == suggestion.categoryId && it.type == type && it.parentId == null }) {
                                        categoryId = suggestion.categoryId
                                        subcategoryId = suggestion.subcategoryId?.takeIf { sub -> data.categories.any { it.id == sub && it.parentId == suggestion.categoryId && it.type == type } }
                                    }
                                }.padding(horizontal = 14.dp).testTag("quick-fill-$index"), contentAlignment = Alignment.Center) {
                                Text(suggestion.title.trim(), style = FFType.metaData, color = FFColors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextField(description, { description = it }, modifier = Modifier.weight(1f), enabled = !busy,
                            placeholder = { Text(MR.strings.tx_add_note.text(), style = FFType.bodyMain, color = FFColors.onSurfaceVariant) },
                            leadingIcon = { FFIcon("hi_note_doc", FFColors.onSurfaceVariant, 22.dp) }, textStyle = FFType.bodyMain, singleLine = true,
                            shape = RoundedCornerShape(16.dp), colors = transactionFieldColors())
                        Row(Modifier.width(140.dp).height(56.dp).clip(RoundedCornerShape(16.dp)).border(1.dp, FFColors.hairline, RoundedCornerShape(16.dp))
                            .clickable(enabled = !busy) { pickingAccount = true }.padding(horizontal = 14.dp).testTag("transaction-wallet"), verticalAlignment = Alignment.CenterVertically) {
                            Text(account?.name ?: MR.strings.tx_wallet.text(), style = FFType.bodyMain.copy(fontSize = 13.5.sp), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            FFIcon("hi_chevron_down", FFColors.onSurface, 18.dp)
                        }
                    }
                    if (type == "transfer" && target != null && target.currency != currency) {
                        OutlinedTextField(received, { received = it }, label = { Text(MR.strings.tx_received.text(target.currency)) }, enabled = !busy, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }
                    if(tags.isNotEmpty())Text(tags.joinToString("  ·  "){"#$it"},Modifier.padding(start=6.dp).clickable(enabled=!busy){tagInput="";tagDialog=true},style=FFType.metaData,color=FFColors.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
                    if (scheduled || startingScheduled || old != null || date > today().toString()) Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(scheduled, { scheduled = it }, enabled = !busy)
                        Text(MR.strings.tx_scheduled_checkbox.text(), style = FFType.metaData)
                    }
                    error?.let { Text(it, style = FFType.bodyMain, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("transaction-error")) }
                }
            }
            TransactionKeypad(busy, date, old?.createdAt ?: pickedCreatedAt ?: nowMillis(), typeLabel, ::press) { expression = amount; calculationError = null; calculator = true }
        }
    }

    if (pickingDate && preferences.datePickerStyle == "wheel") {
        val zone = TimeZone.currentSystemDefault()
        val moment = kotlinx.datetime.Instant.fromEpochMilliseconds(old?.createdAt ?: pickedCreatedAt ?: nowMillis()).toLocalDateTime(zone)
        WheelDateTimeDialog(
            initialDate = runCatching { LocalDate.parse(date) }.getOrDefault(today()),
            initialHour = moment.hour, initialMinute = moment.minute,
            // Jam/menit hanya untuk transaksi baru; waktu transaksi lama (createdAt) tidak bisa diubah.
            showTime = old == null,
            dismiss = { pickingDate = false },
        ) { picked, hour, minute ->
            date = picked.toString()
            if (old == null) pickedCreatedAt = LocalDateTime(picked.year, picked.monthNumber, picked.dayOfMonth, hour, minute).toInstant(zone).toEpochMilliseconds()
            pickingDate = false
        }
    } else if (pickingDate) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = runCatching { LocalDate.parse(date).toEpochDays().toLong() * 86_400_000L }.getOrNull())
        DatePickerDialog(onDismissRequest = { pickingDate = false }, confirmButton = {
            TextButton(enabled = picker.selectedDateMillis != null, onClick = {
                picker.selectedDateMillis?.let { date = LocalDate.fromEpochDays((it / 86_400_000L).toInt()).toString() }
                pickingDate = false
            }) { Text(MR.strings.tx_pick.text()) }
        }, dismissButton = { TextButton(onClick = { pickingDate = false }) { Text(MR.strings.common_cancel.text()) } }) {
            DatePicker(picker)
        }
    }
    if (pickingCurrency) com.clonical.kasiva.feature.auth.OlloCurrencyDialog(
        title = MR.strings.tx_select_currency.text(), closeLabel = MR.strings.common_close.text(), selected = currency,
        onSelect = { entryCurrency = it.takeIf { code -> code != walletCurrency }; pickingCurrency = false },
        onDismiss = { pickingCurrency = false }, defaultCode = walletCurrency,
        onSettings = openCurrencySettings?.let { open -> { pickingCurrency = false; open() } })
    if (pickingCredit) WalletPicker(data, data.accounts.filter { it.type == "credit" && !it.archived }, accountId, { pickingCredit = false }) {
        if (account?.type != "credit") walletBeforeCredit = accountId
        accountId = it
        pickingCredit = false
    }
    if (editingReimburse) ReimburseAmountSheet(reimburseMinor, walletCurrency, { editingReimburse = false }) { reimburseMinor = it; editingReimburse = false }
    if (pickingAccount) WalletPicker(data, data.accounts.filter { (!it.archived || it.id == accountId) && it.type!="savings" }, accountId, { pickingAccount = false }) {
        accountId = it
        if (toId == accountId) toId = ""
        pickingAccount = false
    }
    if(tagDialog)AlertDialog(onDismissRequest={tagDialog=false},title={Text(MR.strings.tx_tags_title.text())},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
        OutlinedTextField(tagInput,{tagInput=it.take(30)},label={Text(MR.strings.tx_new_tag.text())},singleLine=true)
        TextButton(enabled=tagInput.isNotBlank() && tags.size<10,onClick={tags=(tags+tagInput.trim()).distinct();tagInput=""}){Text(MR.strings.tx_add_tag.text())}
        FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){tags.forEach{tag->InputChip(selected=true,onClick={tags=tags-tag},label={Text("$tag ×")})}}
        Text(MR.strings.tx_tags_hint.text(),style=FFType.metaData)
    }},confirmButton={TextButton(onClick={tagDialog=false}){Text(MR.strings.tx_done.text())}})
    if(calculator)AlertDialog(onDismissRequest={calculator=false},title={Text(MR.strings.tx_calculator.text())},text={Column{
        OutlinedTextField(expression,{expression=it.take(100);calculationError=null},label={Text(MR.strings.tx_calculator_example.text())},singleLine=true)
        Text(MR.strings.tx_calculator_hint.text(),style=FFType.metaData)
        calculationError?.let{Text(it,color=FFColors.expense)}
    }},confirmButton={TextButton(onClick={try{amount=decimalAmount(calculateAmount(expression,currency),currency);calculator=false}catch(e:Exception){calculationError=e.message}}){Text(MR.strings.tx_use_result.text())}},dismissButton={TextButton(onClick={calculator=false}){Text(MR.strings.common_cancel.text())}})
}

private fun transactionInputAmount(input: String, currency: String): String {
    val parts = input.ifEmpty { "0" }.split('.')
    val whole = parts[0].ifEmpty { "0" }.reversed().chunked(3).joinToString(".").reversed()
    return (if (currency == "IDR") "Rp" else "$currency ") + whole + if (parts.size > 1) ",${parts[1]}" else ""
}

/** Tanggal ringkas tanpa tahun untuk tombol kalender keypad, mis. "12 Jan". */
private fun keypadDateLabel(date: String): String = runCatching { txDayMonth(LocalDate.parse(date)) }
    .getOrDefault(date.substringBeforeLast(' '))

@Composable
private fun transactionFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = FFColors.surface, unfocusedContainerColor = FFColors.surface, disabledContainerColor = FFColors.surface,
    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransactionKeypad(busy: Boolean, date: String, createdAt: Long, typeLabel: String, press: (String) -> Unit, openCalculator: () -> Unit) {
    val time = runCatching {
        val t = kotlinx.datetime.Instant.fromEpochMilliseconds(createdAt).toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
        "${t.hour.toString().padStart(2, '0')}:${t.minute.toString().padStart(2, '0')}"
    }.getOrDefault("")
    Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp).testTag("transaction-keypad"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val keys = listOf(listOf("1", "2", "3", "backspace"), listOf("4", "5", "6", "calculator"), listOf("7", "8", "9", "calendar"), listOf(".", "0", "000", "confirm"))
        keys.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    val bg = when (key) { "backspace" -> FFColors.expense.copy(alpha = .10f); "confirm" -> FFColors.pill; else -> FFColors.surface }
                    Box(Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(18.dp)).background(bg)
                        .testTag("key-$key").semantics { contentDescription = when (key) {
                            "backspace" -> MR.strings.tx_key_backspace.text(); "calculator" -> MR.strings.tx_calculator.text(); "calendar" -> MR.strings.tx_pick_date.text(); "confirm" -> MR.strings.tx_save_type.text(typeLabel); else -> key
                        } }.combinedClickable(enabled = !busy, role = Role.Button,
                            onLongClick = if (key == "backspace") ({ press("clear") }) else null,
                            onClick = { if (key == "calculator") openCalculator() else press(key) }), contentAlignment = Alignment.Center) {
                        when (key) {
                            "backspace" -> FFIcon("backspace_outlined", FFColors.expense, 24.dp)
                            "calculator" -> FFIcon("hi_calculator", FFColors.onSurface, 24.dp)
                            "confirm" -> FFIcon("check", FFColors.onPill, 26.dp)
                            "calendar" -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(keypadDateLabel(date), style = FFType.metaData.copy(lineHeight = 14.sp), color = FFColors.onSurfaceVariant, maxLines = 1)
                                if (time.isNotBlank()) Text(time, style = FFType.bodyMain.copy(fontSize = 15.sp, lineHeight = 18.sp), color = FFColors.onSurface, maxLines = 1)
                            }
                            else -> Text(key, style = FFType.numpadDigit.copy(fontSize = 20.sp, fontWeight = FontWeight.Normal), color = FFColors.onSurface)
                        }
                    }
                }
            }
        }
    }
}

/** Chip pilihan Ollo (Reimburse / Split Bill / CC-Paylater): pil abu, terpilih sedikit lebih gelap. */
@Composable
private fun OptionChip(label: String, selected: Boolean, tag: String, enabled: Boolean, onClick: () -> Unit) {
    Box(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(if (selected) FFColors.raised else FFColors.chip)
        .semantics { this.selected = selected }.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(horizontal = 16.dp).testTag(tag), contentAlignment = Alignment.Center) {
        Text(label, style = FFType.metaData.copy(fontSize = 13.sp), color = if (selected) FFColors.onSurface else FFColors.onSurfaceVariant, maxLines = 1)
    }
}

/** Panel "Reimburse" Ollo: jumlah yang akan diganti (mata uang dompet), keypad, Reset, dan tombol centang. */
@Composable
private fun ReimburseAmountSheet(initial: Long?, currency: String, dismiss: () -> Unit, done: (Long?) -> Unit) {
    var input by remember { mutableStateOf(initial?.let { decimalAmount(it, currency) }.orEmpty()) }
    fun press(key: String) {
        when (key) {
            "backspace" -> input = input.dropLast(1)
            "C" -> input = ""
            "." -> if (currencyDigits(currency) > 0 && !input.contains('.')) input = input.ifEmpty { "0" } + "."
            "confirm" -> done(runCatching { minorAmount(input, currency) }.getOrNull()?.takeIf { it > 0 })
            else -> {
                val decimals = currencyDigits(currency)
                val room = if (input.contains('.')) (decimals - input.substringAfter('.').length).coerceAtLeast(0) else key.length
                val next = input + key.take(room)
                if (next.length <= 20) input = if (!next.contains('.')) next.trimStart('0').ifEmpty { "0" } else next
            }
        }
    }
    val noRipple = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val swallow = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    androidx.compose.ui.window.Dialog(onDismissRequest = dismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().clickable(interactionSource = noRipple, indication = null, onClick = dismiss), contentAlignment = Alignment.BottomCenter) {
            Column(Modifier.fillMaxWidth().padding(12.dp).navigationBarsPadding().clip(RoundedCornerShape(28.dp)).background(FFColors.canvas)
                .clickable(interactionSource = swallow, indication = null) {}
                .padding(16.dp).testTag("reimburse-sheet"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.align(Alignment.CenterHorizontally).size(36.dp, 4.dp).background(FFColors.outline, RoundedCornerShape(2.dp)))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(MR.strings.tx_reimburse.text(), style = FFType.sectionTitle, modifier = Modifier.weight(1f))
                    TextButton(onClick = { input = ""; done(null) }, modifier = Modifier.testTag("reimburse-reset")) { Text(MR.strings.tx_reset.text(), color = FFColors.onSurface) }
                }
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(FFColors.surface).padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(MR.strings.tx_reimburse_amount.text(), style = FFType.metaData, color = FFColors.onSurfaceVariant)
                    Text(transactionInputAmount(input, currency), style = FFType.amountLg.copy(fontSize = 22.sp, lineHeight = 30.sp), color = FFColors.onSurface,
                        modifier = Modifier.testTag("reimburse-amount"))
                }
                listOf(listOf("1", "2", "3", "backspace"), listOf("4", "5", "6", "C"), listOf("7", "8", "9", "confirm"), listOf(".", "0", "000", "")).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { key ->
                            if (key.isEmpty()) Spacer(Modifier.weight(1f).height(52.dp))
                            else Box(Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(16.dp))
                                .background(when (key) { "backspace" -> FFColors.expense.copy(alpha = .10f); "confirm" -> FFColors.pill; else -> FFColors.surface })
                                .clickable(role = Role.Button) { press(key) }.testTag("reimburse-key-$key"), contentAlignment = Alignment.Center) {
                                when (key) {
                                    "backspace" -> FFIcon("backspace_outlined", FFColors.expense, 22.dp, MR.strings.tx_key_backspace.text())
                                    "confirm" -> FFIcon("check", FFColors.onPill, 24.dp, MR.strings.tx_done.text())
                                    else -> Text(key, style = FFType.numpadDigit.copy(fontSize = 19.sp, fontWeight = FontWeight.Normal), color = FFColors.onSurface)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
