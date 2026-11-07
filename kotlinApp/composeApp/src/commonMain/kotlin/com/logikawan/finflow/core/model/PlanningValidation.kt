package com.logikawan.finflow.core.model

import com.logikawan.finflow.core.data.FinanceRepository
import com.logikawan.finflow.core.util.today
import com.logikawan.finflow.core.i18n.text
import com.logikawan.finflow.i18n.*
import dev.icerock.moko.resources.StringResource
import kotlinx.datetime.*

/**
 * Pesan galat validasi yang ditampilkan ke pengguna. Bila resource belum dapat dibaca (mis. unit test JVM tanpa Context),
 * tetap menghasilkan teks agar jenis exception (IllegalArgumentException) tidak berubah.
 */
fun planText(resource: StringResource, vararg args: Any): String = runCatching { resource.text(*args) }.getOrElse { resource.toString() }

object PlanningValidation {
    private fun text(value: String, maximum: Int = 100) = require(value.isNotBlank() && value.length <= maximum) { planText(MR.strings.plan_err_text_required,maximum) }
    private fun amount(value: Long) = require(value in 1..FinanceRepository.MAX_MONEY) { planText(MR.strings.plan_err_amount_range) }
    private fun currency(value: String) = require(value.matches(Regex("[A-Z]{3}"))) { planText(MR.strings.plan_err_currency_invalid) }
    private fun date(value: String) {
        require(value.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}"))) { planText(MR.strings.plan_err_date_format) }
        LocalDate.parse(value)
    }
    fun bill(value: Bill) {
        text(value.title); amount(value.amountMinor); currency(value.currency); text(value.accountId, 1000)
        text(value.categoryId, 1000); date(value.dueDate)
        require(value.frequency in listOf("once", "weekly", "monthly", "yearly") && value.anchorDay in 1..31)
        require(value.note.length <= 4000 && value.revision >= 0)
    }
    fun debt(value: Debt) {
        text(value.person); amount(value.amountMinor); currency(value.currency); text(value.accountId, 1000)
        require(value.direction in listOf("borrowed", "lent") && value.paidMinor in 0..value.amountMinor)
        date(value.issueDate); date(value.dueDate)
        require(value.issueDate <= today().toString()) { planText(MR.strings.plan_err_issue_future) }
        require(value.dueDate >= value.issueDate) { planText(MR.strings.plan_err_due_before_issue) }
        require(value.note.length <= 4000 && value.revision >= 0)
    }
    fun wish(value: Wish) {
        text(value.title); amount(value.targetMinor); currency(value.currency)
        require(value.priority in listOf("low", "medium", "high") && value.note.length <= 4000 && value.link.length <= 2000)
        require(value.link.isBlank() || value.link.startsWith("https://") || value.link.startsWith("http://")) { planText(MR.strings.plan_err_link_scheme) }
    }
    fun card(value: WalletCard) {
        text(value.name)
        require(value.lastFour.matches(Regex("[0-9]{4}")) || value.recordType != "card" && value.lastFour.isEmpty()) { planText(MR.strings.plan_err_card_full_number) }
        require(value.brand in listOf("visa", "mastercard", "jcb", "amex", "other") && value.note.length <= 4000)
        require(value.recordType in listOf("bank","card","ewallet","other") && value.accountNumber.length<=120 && value.ownerName.length<=100)
        require(value.recordType!="card" || value.accountNumber.isEmpty()) { planText(MR.strings.plan_err_card_number_stored) }
        require(value.recordType=="card" || value.accountNumber.length in 4..120) { planText(MR.strings.plan_err_account_number) }
        require(value.label.length<=100 && value.branch.length<=100 && value.ownership in listOf("personal","business"))
        require(value.category in listOf("main","savings","business","other") && value.color in 0xFF000000..0xFFFFFFFF && value.design in 0..6)
        require(value.expiryMonth == 0 && value.expiryYear == 0 || value.expiryMonth in 1..12 && value.expiryYear in 2000..2199) { planText(MR.strings.plan_err_expiry) }
    }
    fun note(value: FinanceNote) {
        text(value.title); require(value.body.length <= 10000) { planText(MR.strings.plan_err_note_length) }
        require(value.kind in listOf("note","bundle") && value.items.size<=MAX_BUNDLE_ITEMS && value.revision>=0){planText(MR.strings.plan_err_bundle_max,MAX_BUNDLE_ITEMS)}
        if(value.kind=="bundle") {
            currency(value.currency);text(value.accountId.orEmpty(),1000)
            require(value.recordAs in listOf("single","items") && value.items.isNotEmpty()) { planText(MR.strings.plan_err_bundle_min_item) }
            require(value.items.map{it.id}.distinct().size==value.items.size)
            var total=0L
            value.items.forEach { text(it.id,100);text(it.name);amount(it.amountMinor);total=com.logikawan.finflow.core.util.checkedAdd(total,it.amountMinor) }
            amount(total)
        } else require(value.items.isEmpty())
    }
    fun reimbursement(value: Reimbursement) {
        text(value.title); text(value.party); amount(value.amountMinor); currency(value.currency)
        require(value.id == value.expenseId && value.expenseId.isNotBlank() && value.expenseId.length <= 200 && value.note.length <= 4000)
    }
    fun recurring(value: RecurringTransaction) {
        text(value.title); amount(value.amountMinor); currency(value.currency); text(value.accountId, 1000); text(value.categoryId, 1000)
        require(value.type in listOf("income", "expense") && value.frequency in listOf("daily", "weekly", "monthly", "yearly"))
        require(value.interval in 1..120 && value.anchorDay in 1..31 && value.note.length <= 4000 && value.revision >= 0)
        date(value.startDate); date(value.nextDate)
        require(value.nextDate >= value.startDate) { planText(MR.strings.plan_err_next_before_start) }
        value.endDate?.let { date(it); require(it >= value.startDate) { planText(MR.strings.plan_err_end_before_start) } }
    }
}

// Hari jangkar mencegah jadwal tanggal 31 bergeser permanen menjadi tanggal 28.
fun nextOccurrence(date: String, frequency: String, interval: Int = 1, anchorDay: Int = LocalDate.parse(date).dayOfMonth): String {
    require(interval in 1..120 && anchorDay in 1..31)
    val current = LocalDate.parse(date)
    val next = when (frequency) {
        "daily" -> current.plus(DatePeriod(days = interval))
        "weekly" -> current.plus(DatePeriod(days = 7 * interval))
        "monthly", "yearly" -> {
            val first = LocalDate(current.year, current.monthNumber, 1).plus(DatePeriod(months = if (frequency == "yearly") 12 * interval else interval))
            val monthDays = first.plus(DatePeriod(months = 1)).toEpochDays() - first.toEpochDays()
            LocalDate(first.year, first.monthNumber, minOf(anchorDay, monthDays))
        }
        else -> error(planText(MR.strings.plan_err_frequency_invalid))
    }
    require(next.year in 0..9999) { planText(MR.strings.plan_err_calendar_limit) }
    return next.toString()
}

