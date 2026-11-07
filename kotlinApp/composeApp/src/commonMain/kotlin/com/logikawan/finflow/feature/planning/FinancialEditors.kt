package com.logikawan.finflow.feature.planning

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.logikawan.finflow.core.model.*
import com.logikawan.finflow.core.ui.*
import com.logikawan.finflow.core.util.*
import com.logikawan.finflow.core.i18n.text
import com.logikawan.finflow.core.i18n.transactionTypeLabel
import com.logikawan.finflow.i18n.*
import kotlinx.datetime.LocalDate

private val decimalKeyboard=KeyboardOptions(keyboardType=KeyboardType.Decimal)
private fun frequencies()=listOf("daily" to MR.strings.plan_freq_daily.text(),"weekly" to MR.strings.plan_freq_weekly.text(),"monthly" to MR.strings.plan_freq_monthly.text(),"yearly" to MR.strings.plan_freq_yearly.text())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDateField(label:String,value:String,change:(String)->Unit,enabled:Boolean=true) {
    var open by remember{mutableStateOf(false)}
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(label,style=FFType.metaData,fontWeight=FontWeight.SemiBold,color=FFColors.onSurfaceVariant)
        OutlinedButton(onClick={open=true},enabled=enabled,modifier=Modifier.fillMaxWidth().heightIn(min=50.dp),shape=RoundedCornerShape(16.dp),
            colors=ButtonDefaults.outlinedButtonColors(containerColor=FFColors.surface,contentColor=FFColors.onSurface)) {
            Text(shortDateLabel(value),Modifier.weight(1f),style=FFType.bodyMain);FFIcon("expand_more_rounded",size=18.dp)
        }
    }
    if(open) {
        val state=rememberDatePickerState(initialSelectedDateMillis=runCatching{LocalDate.parse(value).toEpochDays().toLong()*86400000L}.getOrNull())
        DatePickerDialog(onDismissRequest={open=false},confirmButton={TextButton(onClick={
            state.selectedDateMillis?.let{change(LocalDate.fromEpochDays((it/86400000L).toInt()).toString())};open=false
        }){Text(MR.strings.plan_pick.text())}},dismissButton={TextButton(onClick={open=false}){Text(MR.strings.common_cancel.text())}}) { DatePicker(state) }
    }
}

@Composable
fun BillEditor(old:Bill?,data:FinanceData,busy:Boolean,dismiss:()->Unit,save:(Bill)->Unit,archive:(()->Unit)?) {
    val id=remember{old?.id ?: newId()}
    var title by remember{mutableStateOf(old?.title.orEmpty())}
    var accountId by remember{mutableStateOf(old?.accountId ?: data.accounts.firstOrNull{!it.archived && it.type!="savings"}?.id.orEmpty())}
    val account=data.accounts.find{it.id==accountId}
    val currency=account?.currency ?: old?.currency ?: "IDR"
    var amount by remember{mutableStateOf(old?.let{decimalAmount(it.amountMinor,it.currency)}.orEmpty())}
    var category by remember{mutableStateOf(old?.categoryId ?: "bills")}
    var date by remember{mutableStateOf(old?.dueDate ?: today().toString())}
    var frequency by remember{mutableStateOf(old?.frequency ?: "once")}
    var note by remember{mutableStateOf(old?.note.orEmpty())}
    var error by remember{mutableStateOf<String?>(null)}
    val paid=old?.paidAt?.let{it>0} ?: false
    var archived by remember{mutableStateOf(old?.archived ?: false)}
    EditorDialog(if(old==null)MR.strings.plan_bill_add_title.text() else MR.strings.plan_bill_edit_title.text(),busy,dismiss,save={
        if(!busy)try {
            val value=Bill(id,title.trim(),minorAmount(amount,currency),currency,accountId,category,date,frequency,LocalDate.parse(date).dayOfMonth,note.trim(),
                old?.paidAt ?: 0,old?.lastPaymentId,archived,old?.revision ?: 0,old?.createdAt ?: nowMillis())
            PlanningValidation.bill(value);save(value)
        }catch(e:Exception){error=e.message}
    },archive,error) {
        OlloFormSection(MR.strings.plan_bill_section_detail.text())
        Field(MR.strings.plan_bill_name.text(),title,{title=it},enabled=!busy)
        Field(MR.strings.plan_bill_amount.text(currency),amount,{amount=it},enabled=!busy,readOnly=paid,keyboardOptions=decimalKeyboard)
        PlanDateField(MR.strings.plan_due_date.text(),date,{date=it},enabled=!busy && !paid)
        if(!paid) {
            OlloFormSection(MR.strings.plan_section_category_wallet.text())
            Choice(MR.strings.plan_category.text(),category,data.categories.filter{it.type=="expense"}.map{it.id to it.name},{if(!busy)category=it})
            Choice(MR.strings.plan_repeat.text(),frequency,listOf("once" to MR.strings.plan_freq_once.text()) + frequencies().filter{it.first!="daily"},{if(!busy)frequency=it})
            WalletChoice(MR.strings.plan_payment_wallet.text(),accountId,data,data.accounts.filter{!it.archived && it.type!="savings"},!busy){accountId=it}
        } else Text(MR.strings.plan_bill_paid_locked.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant)
        Field(MR.strings.plan_note.text(),note,{note=it},singleLine=false,enabled=!busy)
        if(old!=null) Row(verticalAlignment=Alignment.CenterVertically) { Switch(archived,{archived=it},enabled=!busy);Spacer(Modifier.width(12.dp));Text(MR.strings.plan_archived.text(),style=FFType.bodyMain) }
        Text(MR.strings.plan_bill_balance_hint.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant)
    }
}

@Composable
fun DebtEditor(old:Debt?,data:FinanceData,busy:Boolean,dismiss:()->Unit,save:(Debt)->Unit,delete:(()->Unit)?) {
    val id=remember{old?.id ?: newId()}
    var person by remember{mutableStateOf(old?.person.orEmpty())}
    var direction by remember{mutableStateOf(old?.direction ?: "borrowed")}
    var accountId by remember{mutableStateOf(old?.accountId ?: data.accounts.firstOrNull{!it.archived && it.type!="savings"}?.id.orEmpty())}
    val currency=data.accounts.find{it.id==accountId}?.currency ?: old?.currency ?: "IDR"
    var amount by remember{mutableStateOf(old?.let{decimalAmount(it.amountMinor,it.currency)}.orEmpty())}
    var issueDate by remember{mutableStateOf(old?.issueDate ?: today().toString())}
    var dueDate by remember{mutableStateOf(old?.dueDate ?: today().toString())}
    var include by remember{mutableStateOf(old?.includeBalance ?: true)}
    var archived by remember{mutableStateOf(old?.archived ?: false)}
    var note by remember{mutableStateOf(old?.note.orEmpty())}
    var error by remember{mutableStateOf<String?>(null)}
    EditorDialog(if(old==null)MR.strings.plan_debt_add_title.text() else MR.strings.plan_debt_edit_title.text(),busy,dismiss,save={
        if(!busy)try {
            val value=Debt(id,person.trim(),direction,minorAmount(amount,currency),old?.paidMinor ?: 0,currency,accountId,issueDate,dueDate,
                include,note.trim(),archived,old?.lastEventId,old?.revision ?: 0,old?.createdAt ?: nowMillis())
            PlanningValidation.debt(value);save(value)
        }catch(e:Exception){error=e.message}
    },delete,error) {
        if(old==null) Choice(MR.strings.plan_type.text(),direction,listOf("borrowed" to MR.strings.plan_debt_borrowed.text(),"lent" to MR.strings.plan_debt_lent.text()),{if(!busy)direction=it})
        OlloFormSection(MR.strings.plan_debt_section_detail.text())
        Field(MR.strings.plan_debt_person.text(),person,{person=it},enabled=!busy)
        Field(MR.strings.plan_amount_currency.text(currency),amount,{amount=it},enabled=!busy,readOnly=old!=null,keyboardOptions=decimalKeyboard)
        if(old==null) PlanDateField(MR.strings.plan_issue_date.text(),issueDate,{issueDate=it},!busy)
        PlanDateField(MR.strings.plan_due_date.text(),dueDate,{dueDate=it},!busy)
        OlloFormSection(MR.strings.plan_section_wallet_record.text())
        if(old==null) {
            WalletChoice(MR.strings.plan_wallet.text(),accountId,data,data.accounts.filter{!it.archived && it.type!="savings"},!busy){accountId=it}
            Row(verticalAlignment=Alignment.CenterVertically) { Switch(include,{include=it},enabled=!busy);Spacer(Modifier.width(12.dp));Column { Text(MR.strings.plan_debt_include.text(),style=FFType.bodyMain);Text(MR.strings.plan_debt_include_hint.text(),style=FFType.metaData,color=FFColors.outline) } }
        } else Text(MR.strings.plan_debt_locked.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant)
        Field(MR.strings.plan_note.text(),note,{note=it},singleLine=false,enabled=!busy)
        if(old!=null) Row(verticalAlignment=Alignment.CenterVertically) { Switch(archived,{archived=it},enabled=!busy);Spacer(Modifier.width(12.dp));Text(MR.strings.plan_archived.text(),style=FFType.bodyMain) }
        Text(if(old==null) (if(include) (if(direction=="borrowed")MR.strings.plan_debt_hint_increase.text() else MR.strings.plan_debt_hint_decrease.text()) else MR.strings.plan_debt_hint_tracking.text()) else MR.strings.plan_debt_hint_archive.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant)
    }
}

@Composable
fun RecurringEditor(old:RecurringTransaction?,data:FinanceData,busy:Boolean,dismiss:()->Unit,save:(RecurringTransaction)->Unit,archive:(()->Unit)?) {
    val id=remember{old?.id ?: newId()}
    var title by remember{mutableStateOf(old?.title.orEmpty())}
    var type by remember{mutableStateOf(old?.type ?: "expense")}
    var accountId by remember{mutableStateOf(old?.accountId ?: data.accounts.firstOrNull{!it.archived && it.type!="savings"}?.id.orEmpty())}
    val currency=data.accounts.find{it.id==accountId}?.currency ?: old?.currency ?: "IDR"
    var category by remember{mutableStateOf(old?.categoryId ?: data.categories.firstOrNull{it.type==type}?.id.orEmpty())}
    var amount by remember{mutableStateOf(old?.let{decimalAmount(it.amountMinor,it.currency)}.orEmpty())}
    var frequency by remember{mutableStateOf(old?.frequency ?: "monthly")}
    var interval by remember{mutableStateOf((old?.interval ?: 1).toString())}
    var date by remember{mutableStateOf(old?.nextDate ?: today().toString())}
    var hasEnd by remember{mutableStateOf(old?.endDate!=null)}
    var endDate by remember{mutableStateOf(old?.endDate ?: today().toString())}
    var active by remember{mutableStateOf(old?.active ?: true)}
    var note by remember{mutableStateOf(old?.note.orEmpty())}
    var error by remember{mutableStateOf<String?>(null)}
    EditorDialog(if(old==null)MR.strings.plan_recurring_new_title.text() else MR.strings.plan_recurring_edit_title.text(),busy,dismiss,save={
        if(!busy)try {
            val value=RecurringTransaction(id,title.trim(),type,minorAmount(amount,currency),currency,accountId,category,frequency,
                interval.toInt(),LocalDate.parse(date).dayOfMonth,old?.startDate ?: date,date,if(hasEnd)endDate else null,active,note.trim(),
                old?.lastEventId,old?.revision ?: 0,old?.createdAt ?: nowMillis())
            PlanningValidation.recurring(value);save(value)
        }catch(e:Exception){error=e.message}
    },archive,error) {
        Field(MR.strings.plan_amount_currency.text(currency),amount,{amount=it},enabled=!busy,keyboardOptions=decimalKeyboard)
        SegmentedTabs(listOf("expense" to transactionTypeLabel("expense"),"income" to transactionTypeLabel("income")),type,{if(!busy){type=it;category=data.categories.firstOrNull{c->c.type==it}?.id.orEmpty()}})
        Choice(MR.strings.plan_category.text(),category,data.categories.filter{it.type==type}.map{it.id to it.name},{if(!busy)category=it})
        Field(MR.strings.plan_title.text(),title,{title=it},enabled=!busy)
        Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f)){Choice(MR.strings.plan_frequency.text(),frequency,frequencies(),{if(!busy)frequency=it})}
            Column(Modifier.weight(1f)){PlanDateField(if(old==null)MR.strings.plan_start_date.text() else MR.strings.plan_next_schedule.text(),date,{date=it},!busy)}
        }
        Field(MR.strings.plan_repeat_every.text(when(frequency){"daily"->MR.strings.plan_unit_day.text();"weekly"->MR.strings.plan_unit_week.text();"yearly"->MR.strings.plan_unit_year.text();else->MR.strings.plan_unit_month.text()}),interval,{interval=it},enabled=!busy,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
        Row { Checkbox(hasEnd,{hasEnd=it},enabled=!busy);Text(MR.strings.plan_use_end_date.text()) }
        if(hasEnd)PlanDateField(MR.strings.plan_end_date.text(),endDate,{endDate=it},!busy)
        WalletChoice(MR.strings.plan_pay_with_wallet.text(),accountId,data,data.accounts.filter{!it.archived && it.type!="savings"},!busy){accountId=it}
        Row { Switch(active,{active=it},enabled=!busy);Spacer(Modifier.width(12.dp));Text(MR.strings.plan_schedule_active.text()) }
        Field(MR.strings.plan_note.text(),note,{note=it},singleLine=false,enabled=!busy)
        Text(MR.strings.plan_recurring_hint.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant)
    }
}

data class PaymentRequest(val amountMinor:Long,val accountId:String,val date:String,val requestId:String)

@Composable
fun PaymentEditor(title:String,currency:String,amountMinor:Long,data:FinanceData,defaultAccountId:String,busy:Boolean,
    dismiss:()->Unit,confirm:(PaymentRequest)->Unit,partial:Boolean=false,selectAccount:Boolean=true,changesBalance:Boolean=true) {
    val requestId=remember{newId()}
    var amount by remember{mutableStateOf(decimalAmount(amountMinor,currency))}
    val accounts=data.accounts.filter{it.type!="savings" && it.currency==currency && (!changesBalance || !it.archived)}
    var account by remember{mutableStateOf(defaultAccountId.takeIf{id->accounts.any{it.id==id}} ?: accounts.firstOrNull()?.id.orEmpty())}
    var date by remember{mutableStateOf(today().toString())}
    var error by remember{mutableStateOf<String?>(null)}
    EditorDialog(title,busy,dismiss,save={
        if(!busy)try {
            val minor=minorAmount(amount,currency)
            require(minor in 1..amountMinor){MR.strings.plan_err_payment_exceeds.text()}
            require(date<=today().toString()){MR.strings.plan_err_payment_future.text()}
            require(account.isNotBlank() || !changesBalance){MR.strings.plan_err_pick_wallet.text()}
            confirm(PaymentRequest(minor,if(selectAccount)account else defaultAccountId,date,requestId))
        }catch(e:Exception){error=e.message}
    },error=error) {
        Field(MR.strings.plan_amount_nominal.text(currency),amount,{amount=it},enabled=!busy,readOnly=!partial,keyboardOptions=decimalKeyboard)
        if(selectAccount) WalletChoice(MR.strings.plan_wallet.text(),account,data,accounts,!busy){account=it}
        else Text(MR.strings.plan_wallet_named.text(data.accountName(defaultAccountId)),style=FFType.bodyMain)
        PlanDateField(MR.strings.plan_payment_date.text(),date,{date=it},!busy)
        Text(if(changesBalance)MR.strings.plan_payment_hint_balance.text()
            else MR.strings.plan_payment_hint_status.text(),style=FFType.bodyMain,color=FFColors.onSurfaceVariant)
    }
}

