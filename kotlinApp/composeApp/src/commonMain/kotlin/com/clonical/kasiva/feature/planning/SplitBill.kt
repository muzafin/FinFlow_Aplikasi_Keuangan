package com.clonical.kasiva.feature.planning

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.model.*
import com.clonical.kasiva.core.ui.*
import com.clonical.kasiva.core.util.*
import com.clonical.kasiva.i18n.*
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * Split Bill (disepakati pemilik 11 Nov 2026): pengguna membayar tagihan bersama dari satu dompet.
 * - Bagian sendiri → transaksi pengeluaran biasa (source manual, ikut statistik & XP).
 * - Bagian tiap teman → Piutang ("lent", memengaruhi saldo) yang dilunasi lewat halaman Hutang/Piutang.
 * Saldo dompet berkurang sebesar total; tidak ada koleksi/rules Firestore baru.
 */
data class SplitPlan(val myShare: Transaction?, val receivables: List<Debt>)

/** Bagi rata dalam satuan minor; sisa pembagian diberikan 1 per orang mulai dari urutan pertama agar total tepat. */
fun splitEvenly(totalMinor: Long, people: Int): List<Long> {
    require(people > 0 && totalMinor >= 0)
    val base = totalMinor / people
    val rest = (totalMinor % people).toInt()
    return List(people) { index -> base + if (index < rest) 1 else 0 }
}

fun buildSplitPlan(
    title: String, totalMinor: Long, currency: String, accountId: String, categoryId: String, date: String, dueDate: String,
    myShareMinor: Long, friends: List<Pair<String, Long>>, note: String = "",
): SplitPlan {
    val name = title.trim()
    require(name.isNotEmpty()) { MR.strings.sb_err_title.text() }
    require(totalMinor > 0) { MR.strings.sb_err_total.text() }
    require(friends.isNotEmpty()) { MR.strings.sb_err_people.text() }
    require(friends.all { it.first.isNotBlank() }) { MR.strings.sb_err_person_name.text() }
    require(friends.all { it.second > 0 } && myShareMinor >= 0) { MR.strings.sb_err_share.text() }
    val sum = friends.fold(myShareMinor) { acc, friend -> checkedAdd(acc, friend.second) }
    require(sum == totalMinor) { MR.strings.sb_err_mismatch.text(decimalAmount(sum, currency), decimalAmount(totalMinor, currency)) }
    require(dueDate >= date) { MR.strings.sb_err_due.text() }
    val label = MR.strings.sb_note_prefix.text(name)
    val detail = listOf(label, note.trim()).filter { it.isNotEmpty() }.joinToString("\n")
    val mine = if (myShareMinor > 0) Transaction(type = "expense", amountMinor = myShareMinor, currency = currency, accountId = accountId,
        categoryId = categoryId, title = name, description = detail, date = date, source = "manual", tags = emptyList()) else null
    val receivables = friends.map { (person, amount) ->
        Debt(person = person.trim().take(100), direction = "lent", amountMinor = amount, currency = currency, accountId = accountId,
            issueDate = date, dueDate = dueDate, includeBalance = true, note = detail.take(4000))
    }
    receivables.forEach(PlanningValidation::debt)
    return SplitPlan(mine, receivables)
}

private val splitKeyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal)

private class SplitPerson(name: String = "", amount: String = "") {
    var name by mutableStateOf(name)
    var amount by mutableStateOf(amount)
}

@Composable
fun SplitBillEditor(data: FinanceData, busy: Boolean, dismiss: () -> Unit, save: (SplitPlan) -> Unit) {
    var title by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }
    var accountId by remember { mutableStateOf(data.accounts.firstOrNull { !it.archived && it.type != "savings" }?.id.orEmpty()) }
    val currency = data.accounts.find { it.id == accountId }?.currency ?: data.profile?.baseCurrency ?: "IDR"
    var category by remember { mutableStateOf(data.categories.firstOrNull { it.type == "expense" && it.id == "food" }?.id
        ?: data.categories.firstOrNull { it.type == "expense" }?.id.orEmpty()) }
    var date by remember { mutableStateOf(today().toString()) }
    var dueDate by remember { mutableStateOf(today().plus(DatePeriod(days = 7)).toString()) }
    var includeMe by remember { mutableStateOf(true) }
    var myShare by remember { mutableStateOf("") }
    val people = remember { mutableStateListOf(SplitPerson()) }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun parse(text: String): Long = if (text.isBlank()) 0 else minorAmount(text, currency)
    fun divideEvenly() {
        error = try {
            val shares = splitEvenly(parse(total), people.size + if (includeMe) 1 else 0)
            if (includeMe) myShare = decimalAmount(shares.first(), currency)
            people.forEachIndexed { index, person -> person.amount = decimalAmount(shares[index + if (includeMe) 1 else 0], currency) }
            null
        } catch (e: Exception) { e.message ?: MR.strings.sb_err_total.text() }
    }
    val assigned = runCatching { people.fold(if (includeMe) parse(myShare) else 0L) { acc, p -> checkedAdd(acc, parse(p.amount)) } }.getOrNull()
    val totalMinor = runCatching { parse(total) }.getOrNull()

    EditorDialog(MR.strings.sb_title.text(), busy, dismiss, save = {
        if (!busy) try {
            save(buildSplitPlan(title, parse(total), currency, accountId, category, date, dueDate,
                if (includeMe) parse(myShare) else 0, people.map { it.name to parse(it.amount) }, note))
        } catch (e: Exception) { error = e.message }
    }, error = error) {
        Text(MR.strings.sb_intro.text(), style = FFType.metaData, color = FFColors.onSurfaceVariant)
        OlloFormSection(MR.strings.sb_section_bill.text())
        Field(MR.strings.sb_bill_name.text(), title, { title = it }, enabled = !busy, modifier = Modifier.testTag("split-title"))
        Field(MR.strings.plan_amount_currency.text(currency), total, { total = it }, enabled = !busy, keyboardOptions = splitKeyboard, modifier = Modifier.testTag("split-total"))
        Choice(MR.strings.plan_category.text(), category, data.categories.filter { it.type == "expense" && it.parentId == null }.map { it.id to it.name }, { if (!busy) category = it })
        WalletChoice(MR.strings.plan_payment_wallet.text(), accountId, data, data.accounts.filter { !it.archived && it.type != "savings" }, !busy) { accountId = it }
        PlanDateField(MR.strings.sb_paid_on.text(), date, { date = it }, !busy)

        OlloFormSection(MR.strings.sb_section_people.text())
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(includeMe, { includeMe = it }, enabled = !busy, modifier = Modifier.testTag("split-include-me"))
            Spacer(Modifier.width(12.dp))
            Text(MR.strings.sb_include_me.text(), style = FFType.bodyMain)
        }
        if (includeMe) Field(MR.strings.sb_my_share.text(currency), myShare, { myShare = it }, enabled = !busy, keyboardOptions = splitKeyboard, modifier = Modifier.testTag("split-my-share"))
        people.forEachIndexed { index, person ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Field(MR.strings.sb_person_n.text(index + 1), person.name, { person.name = it }, Modifier.weight(1.3f).testTag("split-person-$index"), enabled = !busy)
                Spacer(Modifier.width(8.dp))
                Field(currency, person.amount, { person.amount = it }, Modifier.weight(1f).testTag("split-amount-$index"), enabled = !busy, keyboardOptions = splitKeyboard)
                if (people.size > 1) FFIconButton("close_rounded", { if (!busy) people.removeAt(index) }, tint = FFColors.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ people.add(SplitPerson()) }, enabled = !busy && people.size < 20, modifier = Modifier.testTag("split-add-person")) { Text(MR.strings.sb_add_person.text()) }
            OutlinedButton({ divideEvenly() }, enabled = !busy, modifier = Modifier.testTag("split-even")) { Text(MR.strings.sb_split_even.text()) }
        }
        if (assigned != null && totalMinor != null && totalMinor > 0) {
            val ok = assigned == totalMinor
            Text(MR.strings.sb_assigned.text(decimalAmount(assigned, currency), decimalAmount(totalMinor, currency)),
                style = FFType.metaData.copy(fontWeight = FontWeight.SemiBold), color = if (ok) FFColors.income else FFColors.expense,
                modifier = Modifier.testTag("split-assigned"))
        }
        PlanDateField(MR.strings.sb_due.text(), dueDate, { dueDate = it }, !busy)
        Field(MR.strings.plan_note.text(), note, { note = it }, singleLine = false, enabled = !busy)
        Text(MR.strings.sb_hint.text(), style = FFType.metaData, color = FFColors.onSurfaceVariant)
    }
}
