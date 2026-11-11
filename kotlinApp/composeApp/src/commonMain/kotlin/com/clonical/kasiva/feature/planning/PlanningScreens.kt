package com.clonical.kasiva.feature.planning

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonical.kasiva.core.model.*
import com.clonical.kasiva.core.ui.*
import com.clonical.kasiva.core.util.*
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.i18n.*
import kotlinx.datetime.LocalDate
import kotlin.math.roundToInt

data class PlanningActions(
    val bill:(Bill?)->Unit, val payBill:(Bill)->Unit,
    val debt:(Debt?)->Unit, val payDebt:(Debt)->Unit,
    val wish:(Wish?)->Unit, val fulfillWish:(Wish)->Unit,
    val card:(WalletCard?)->Unit, val note:(FinanceNote?)->Unit,
    val claim:(Reimbursement?)->Unit, val receiveClaim:(Reimbursement)->Unit,
    val recurring:(RecurringTransaction?)->Unit, val processRecurring:()->Unit,
    val manualScheduled:()->Unit, val addExpense:()->Unit, val reverse:(FinancialEvent)->Unit,
    val payBundle:(FinanceNote,List<String>)->Unit = {_,_->}, val updateNote:(FinanceNote)->Unit = {},
    val splitBill:()->Unit = {},
)

@Composable
fun PlanningScreen(page:String,data:FinanceData,busy:Boolean,actions:PlanningActions) {
    when(page) {
        "Tagihan" -> Bills(data,busy,actions)
        "Utang" -> Debts(data,busy,actions)
        "Keinginan" -> Wishes(data,busy,actions)
        "Kartu" -> Cards(data,busy,actions)
        "Catatan" -> Notes(data,busy,actions)
        "Reimburse" -> Claims(data,busy,actions)
        "Terjadwal" -> Recurring(data,busy,actions)
    }
}

@Composable
private fun PlanningList(data:FinanceData,busy:Boolean,addLabel:String,add:()->Unit,extendedFab:Boolean=false,content:LazyListScope.()->Unit) {
    Box(Modifier.fillMaxSize().testTag("planning-page")) {
        if(!data.featuresReady) {
            Box(Modifier.fillMaxSize().padding(24.dp),contentAlignment=Alignment.Center) {
                Text(data.featuresError?.let{MR.strings.plan_features_failed.text(it)} ?: MR.strings.plan_loading.text(),style=FFType.bodyMain,color=if(data.featuresError!=null) FFColors.expense else FFColors.outline)
            }
        } else LazyColumn(Modifier.fillMaxSize().testTag("planning-list"),contentPadding=PaddingValues(start=16.dp,end=16.dp,top=16.dp,bottom=104.dp),verticalArrangement=Arrangement.spacedBy(16.dp),content=content)
        // Ollo: tombol + mengambang di kanan-bawah (Catatan memakai pil berlabel).
        val label=addLabel.removePrefix("+").trim()
        if(data.featuresReady)Box(Modifier.align(Alignment.BottomEnd).padding(16.dp)){
            if(extendedFab)Surface(onClick=add,enabled=!busy,modifier=Modifier.height(56.dp).semantics{contentDescription=label},shape=RoundedCornerShape(16.dp),color=FFColors.pill,shadowElevation=6.dp){
                Row(Modifier.padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically){
                    FFIcon("hi_add",FFColors.onPill,22.dp);Spacer(Modifier.width(12.dp));Text(label,style=FFType.bodyMain.copy(fontSize=15.sp),color=FFColors.onPill)
                }
            } else OlloFab({if(!busy)add()},label)
        }
    }
}

// State kosong Ollo: ikon garis besar redup di tengah, judul abu, keterangan kecil.
@Composable
private fun PlanningEmpty(icon:String,title:String,message:String?) {
    Column(Modifier.fillMaxWidth().padding(top=200.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        FFIcon(icon,FFColors.onSurfaceVariant.copy(alpha=.45f),64.dp)
        Spacer(Modifier.height(16.dp))
        Text(title,style=FFType.bodyMain.copy(fontSize=16.sp,lineHeight=22.sp),color=FFColors.onSurfaceVariant,textAlign=TextAlign.Center)
        if(message!=null){Spacer(Modifier.height(6.dp));Text(message,style=FFType.metaData,color=FFColors.onSurfaceVariant.copy(alpha=.75f),textAlign=TextAlign.Center)}
    }
}

// Tab bergaris bawah seperti Ollo (Reimbursement, My Cards).
@Composable
private fun OlloUnderlineTabs(options:List<Triple<String,String,String?>>,selected:String,select:(String)->Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            options.forEach{(value,label,icon)->
                val chosen=value==selected
                Column(Modifier.weight(1f).clickable(role=Role.Tab){select(value)}.semantics{this.selected=chosen}.padding(top=6.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    if(icon!=null){FFIcon(icon,if(chosen)FFColors.onSurface else FFColors.onSurfaceVariant,20.dp);Spacer(Modifier.height(4.dp))}
                    Text(label,style=FFType.bodyMain.copy(fontSize=15.sp),color=if(chosen)FFColors.onSurface else FFColors.onSurfaceVariant,maxLines=1)
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.width(36.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(if(chosen)FFColors.onSurface else Color.Transparent))
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(FFColors.outlineVariant))
    }
}

@Composable
private fun EmptyPlanning(title:String,message:String) {
    DesignCard {
        Text(title,style=FFType.sectionTitle)
        Spacer(Modifier.height(8.dp));Text(message,style=FFType.bodyMain,color=FFColors.outline)
    }
}

@Composable
private fun PaymentHistory(event:FinancialEvent,busy:Boolean,reverse:(FinancialEvent)->Unit) {
    val accent=when(event.kind){"goal_withdrawal"->FFColors.income else->FFColors.expense}
    val icon=when(event.kind){"goal_deposit","reimbursement"->"arrow_downward_rounded" "goal_withdrawal"->"arrow_upward_rounded" "bundle"->"checklist_rounded" "bill"->"receipt_long_rounded" else->"payments_rounded"}
    DesignCard {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(accent.copy(alpha=.12f)),contentAlignment=Alignment.Center){FFIcon(icon,accent,20.dp)}
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(event.title,style=FFType.bodyMain.copy(fontSize=15.sp,lineHeight=20.sp),fontWeight=FontWeight.SemiBold,color=FFColors.onSurface,maxLines=1,overflow=TextOverflow.Ellipsis)
                Text(shortDateLabel(event.date),style=FFType.metaData,color=FFColors.onSurfaceVariant)
            }
            Box(Modifier.weight(1f)){FitMoneyText(moneyLabel(event.amountMinor,event.currency),FFType.amountMd,accent,TextAlign.End)}
            OlloMenuButton(listOf(OlloMenuItem("reverse",MR.strings.plan_reverse_record.text(),"delete_outline_rounded"))){key->if(key=="reverse"&&!busy)reverse(event)}
        }
    }
}

@Composable
private fun Bills(data:FinanceData,busy:Boolean,a:PlanningActions) {
    var tab by rememberSaveable{mutableIntStateOf(0)}
    var sortAmount by rememberSaveable{mutableStateOf(false)}
    var showArchived by rememberSaveable{mutableStateOf(false)}
    val unpaid=data.bills.filter{!it.archived && it.paidAt==0L}.let{list->if(sortAmount)list.sortedBy{it.amountMinor} else list.sortedBy{it.dueDate}}
    val paid=data.bills.filter{it.paidAt>0L}.sortedByDescending{it.paidAt}
    val archived=data.bills.filter{it.archived}.sortedBy{it.dueDate}
    val events=data.financialEvents.filter{it.kind=="bill"}.sortedByDescending{it.createdAt}
    Box(Modifier.fillMaxSize().testTag("planning-page")) {
        if(!data.featuresReady) {
            Box(Modifier.fillMaxSize().padding(24.dp),contentAlignment=Alignment.Center) {
                Text(data.featuresError?.let{MR.strings.plan_features_failed.text(it)} ?: MR.strings.plan_loading.text(),style=FFType.bodyMain,color=if(data.featuresError!=null) FFColors.expense else FFColors.outline,textAlign=TextAlign.Center)
            }
        } else {
        LazyColumn(Modifier.fillMaxSize().testTag("planning-list"),contentPadding=PaddingValues(start=16.dp,end=16.dp,top=16.dp,bottom=104.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            (if(unpaid.isEmpty()) listOf(data.profile?.baseCurrency ?: "IDR") else unpaid.map{it.currency}.distinct()).forEach{currency->
                item(key="bill-summary-$currency"){BillsSummary(unpaid.filter{it.currency==currency},currency)}
            }
            item { OlloSegmented(listOf("unpaid" to MR.strings.plan_unpaid.text(),"paid" to MR.strings.plan_paid_off.text()),if(tab==0)"unpaid" else "paid",{tab=if(it=="unpaid")0 else 1},trailing={OlloRefreshButton{sortAmount=!sortAmount}}) }
            if(tab==0) {
                if(unpaid.isEmpty()) item { OlloEmptyState("check_rounded",MR.strings.plan_bills_all_done.text(),MR.strings.plan_bills_all_done_msg.text(),MR.strings.plan_bills_archive_tip.text()) }
                items(unpaid,key={it.id}){bill->BillCard(bill,busy,false,a)}
                if(showArchived && archived.isNotEmpty()) {
                    item{Text(MR.strings.plan_bills_archive_title.text(),style=FFType.sectionTitle,color=FFColors.onSurface)}
                    items(archived,key={"arch-${it.id}"}){bill->BillCard(bill,busy,true,a)}
                }
            } else {
                if(paid.isEmpty() && events.isEmpty()) item { OlloEmptyState("history_rounded",MR.strings.plan_bills_no_payments.text(),MR.strings.plan_bills_no_payments_msg.text()) }
                items(paid,key={"paid-${it.id}"}){bill->PaidBillRow(bill,a)}
                if(events.isNotEmpty()) item{Text(MR.strings.plan_payment_history.text(),style=FFType.sectionTitle,color=FFColors.onSurface)}
                items(events,key={"event-${it.id}"}){event->PaymentHistory(event,busy,a.reverse)}
            }
        }
        // Diletakkan setelah daftar agar versi tanpa App (uji) tetap dapat disentuh.
        TopBarActions {
            OlloMenuButton(listOf(
                OlloMenuItem("sort",if(sortAmount)MR.strings.plan_sort_due.text() else MR.strings.plan_sort_amount.text(),"filter_list_rounded"),
                OlloMenuItem("archive",if(showArchived)MR.strings.plan_hide_archive.text() else MR.strings.plan_show_archive.text(),"archive_rounded"),
            ),tint=FFColors.onSurface){key->when(key){"sort"->sortAmount=!sortAmount;else->showArchived=!showArchived}}
        }
        }
        if(data.featuresReady)Box(Modifier.align(Alignment.BottomEnd).padding(16.dp)){OlloFab({a.bill(null)},MR.strings.plan_add_bill.text())}
    }
}

@Composable
private fun BillsSummary(unpaid:List<Bill>,currency:String) {
    val total=unpaid.fold(0L){sum,bill->checkedAdd(sum,bill.amountMinor)}
    DesignCard {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text(MR.strings.plan_total_unpaid.text(),Modifier.weight(1f),style=FFType.bodyMain.copy(fontSize=15.sp,lineHeight=20.sp),color=FFColors.onSurfaceVariant)
            OlloPill(MR.strings.plan_bill_count.text(unpaid.size),FFColors.outlineVariant,FFColors.onSurfaceVariant)
        }
        Spacer(Modifier.height(10.dp))
        FitMoneyText(moneyLabel(total,currency),FFType.amountLg.copy(fontSize=32.sp,lineHeight=40.sp),FFColors.onSurface)
        Spacer(Modifier.height(14.dp))
        if(unpaid.isEmpty()) OlloPill(MR.strings.plan_all_paid.text(),FFColors.income.copy(alpha=.12f),FFColors.income,icon="check_rounded")
        else OlloPill(MR.strings.plan_due_on.text(shortDateLabel(unpaid.minOf{it.dueDate})),FFColors.expense.copy(alpha=.10f),FFColors.expense,icon="calendar_today_rounded")
    }
}

@Composable
private fun BillCard(bill:Bill,busy:Boolean,archived:Boolean,a:PlanningActions) {
    val overdue=!archived && bill.dueDate<today().toString()
    val accent=categoryColor(bill.categoryId)
    val dueLabel=when{archived->MR.strings.plan_archived.text();bill.frequency!="once"->MR.strings.plan_due_on_recurring.text(shortDateLabel(bill.dueDate));else->MR.strings.plan_due_on.text(shortDateLabel(bill.dueDate))}
    DesignCard(onClick={a.bill(bill)}) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(accent.copy(alpha=.12f)),contentAlignment=Alignment.Center){FFIcon(categoryIcon(bill.categoryId),accent,22.dp)}
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(bill.title,style=FFType.bodyMain.copy(fontSize=16.sp,lineHeight=22.sp),fontWeight=FontWeight.SemiBold,color=FFColors.onSurface,maxLines=1,overflow=TextOverflow.Ellipsis)
                Text(dueLabel,style=FFType.metaData,color=when{archived->FFColors.outline;overdue->FFColors.expense;else->FFColors.onSurfaceVariant})
            }
            if(!busy)OlloMenuButton(listOf(OlloMenuItem("pay",MR.strings.plan_pay_bill.text(),"payments_rounded"),OlloMenuItem("edit",MR.strings.plan_bill_edit_title.text(),"edit_rounded"))){key->if(key=="pay")a.payBill(bill) else a.bill(bill)}
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.weight(1f)){FitMoneyText(moneyLabel(bill.amountMinor,bill.currency),FFType.amountMd.copy(fontSize=20.sp,lineHeight=26.sp,fontWeight=FontWeight.SemiBold),FFColors.onSurface)}
            OlloPill(if(archived)MR.strings.plan_archive.text() else MR.strings.plan_unpaid.text(),if(archived)FFColors.outlineVariant else FFColors.expense.copy(alpha=.10f),if(archived)FFColors.onSurfaceVariant else FFColors.expense)
        }
        if(!archived){Spacer(Modifier.height(14.dp));OlloPrimaryButton(MR.strings.plan_pay_bill.text(),enabled=!busy){a.payBill(bill)}}
    }
}

@Composable
private fun PaidBillRow(bill:Bill,a:PlanningActions) {
    DesignCard(onClick={a.bill(bill)}) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(FFColors.income.copy(alpha=.12f)),contentAlignment=Alignment.Center){FFIcon("check_rounded",FFColors.income,22.dp)}
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(bill.title,style=FFType.bodyMain.copy(fontSize=16.sp,lineHeight=22.sp),fontWeight=FontWeight.SemiBold,color=FFColors.onSurface,maxLines=1,overflow=TextOverflow.Ellipsis)
                Text(MR.strings.plan_paid_on.text(shortDateLabel(bill.dueDate)),style=FFType.metaData,color=FFColors.income)
            }
            Box(Modifier.weight(1f)){FitMoneyText(moneyLabel(bill.amountMinor,bill.currency),FFType.amountMd,FFColors.onSurface,TextAlign.End)}
        }
    }
}

@Composable
private fun Debts(data:FinanceData,busy:Boolean,a:PlanningActions) {
    var direction by rememberSaveable{mutableStateOf("borrowed")}
    var closed by rememberSaveable{mutableStateOf(false)}
    var expanded by rememberSaveable{mutableStateOf<String?>(null)}
    val entries=data.debts.filter{it.direction==direction && if(closed) it.archived || it.paidMinor>=it.amountMinor else !it.archived && it.paidMinor<it.amountMinor}
    Box(Modifier.fillMaxSize().testTag("planning-page")) {
        if(!data.featuresReady) {
            Box(Modifier.fillMaxSize().padding(24.dp),contentAlignment=Alignment.Center) {
                Text(data.featuresError?.let{MR.strings.plan_features_failed.text(it)} ?: MR.strings.plan_loading.text(),style=FFType.bodyMain,color=if(data.featuresError!=null) FFColors.expense else FFColors.outline,textAlign=TextAlign.Center)
            }
        } else {
        LazyColumn(Modifier.fillMaxSize().testTag("planning-list"),contentPadding=PaddingValues(start=16.dp,end=16.dp,top=16.dp,bottom=104.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            val open=data.debts.filter{!it.archived && it.paidMinor<it.amountMinor}
            (if(open.isEmpty()) listOf(data.profile?.baseCurrency ?: "IDR") else open.map{it.currency}.distinct()).forEach{currency->
                item(key="debt-summary-$currency"){DebtsOverview(data.debts.filter{it.currency==currency && !it.archived},currency)}
            }
            item { OlloSegmented(listOf("borrowed" to MR.strings.plan_my_debts.text(),"lent" to MR.strings.plan_my_receivables.text()),direction,{direction=it}) }
            if(entries.isNotEmpty()) item { Text(if(direction=="borrowed")MR.strings.plan_debt_list.text() else MR.strings.plan_receivable_list.text(),style=FFType.sectionTitle,color=FFColors.onSurface) }
            if(entries.isEmpty()) item { PlanningEmpty("hi_check_circle",if(closed)MR.strings.plan_no_closed.text() else if(direction=="borrowed")MR.strings.plan_debt_free.text() else MR.strings.plan_no_receivables.text(),null) }
            items(entries,key={it.id}){debt->DebtCard(debt,data,busy,expanded==debt.id,{expanded=if(expanded==debt.id)null else debt.id},a)}
        }
        // Diletakkan setelah daftar agar versi tanpa App (uji) tetap dapat disentuh.
        TopBarActions {
            OlloMenuButton(listOf(OlloMenuItem("closed",if(closed)MR.strings.plan_show_active.text() else MR.strings.plan_show_closed.text(),"visibility_off_rounded")),tint=FFColors.onSurface){key->if(key=="closed")closed=!closed}
        }
        }
        if(data.featuresReady)Row(Modifier.align(Alignment.BottomEnd).padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
            // Split Bill: bayar tagihan bersama, bagian teman otomatis menjadi piutang.
            Surface(onClick=a.splitBill,enabled=!busy,modifier=Modifier.height(48.dp).testTag("debt-split-bill"),shape=RoundedCornerShape(16.dp),color=FFColors.surfaceContainerHigh,shadowElevation=4.dp) {
                Row(Modifier.padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically) {
                    FFIcon("hi_reimburse",FFColors.onSurface,20.dp);Spacer(Modifier.width(8.dp))
                    Text(MR.strings.sb_button.text(),style=FFType.bodyMain.copy(fontWeight=FontWeight.SemiBold),color=FFColors.onSurface)
                }
            }
            Spacer(Modifier.width(12.dp))
            OlloFab({a.debt(null)},MR.strings.plan_add_debt.text())
        }
    }
}

@Composable
private fun DebtsOverview(debts:List<Debt>,currency:String) {
    fun rest(list:List<Debt>)=list.fold(0L){sum,d->checkedAdd(sum,(d.amountMinor-d.paidMinor).coerceAtLeast(0))}
    fun paidPct(list:List<Debt>):Int{val total=list.fold(0L){s,d->checkedAdd(s,d.amountMinor)};val paid=list.fold(0L){s,d->checkedAdd(s,d.paidMinor.coerceAtMost(d.amountMinor))};return if(total>0)(paid*100/total).toInt() else 0}
    val owe=debts.filter{it.direction=="borrowed"};val owed=debts.filter{it.direction!="borrowed"}
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(RoundedCornerShape(20.dp)).background(FFColors.surface).padding(horizontal=20.dp,vertical=16.dp).testTag("debt-overview")) {
        listOf(Triple(MR.strings.plan_my_debts.text(),owe,0),Triple(MR.strings.plan_my_receivables.text(),owed,1)).forEach{(label,list,index)->
            if(index==1)Box(Modifier.padding(horizontal=18.dp).width(1.dp).fillMaxHeight().background(FFColors.outlineVariant))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment=Alignment.CenterVertically){
                    Text(label,style=FFType.metaData,color=FFColors.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp));Text("${paidPct(list)}%",style=FFType.metaData,color=FFColors.onSurfaceVariant.copy(alpha=.6f))
                }
                Spacer(Modifier.height(4.dp))
                Text(moneyLabel(rest(list),currency),style=FFType.amountMd.copy(fontSize=17.sp),color=FFColors.onSurface,maxLines=1)
            }
        }
    }
}

@Composable
private fun DebtsSummary(entries:List<Debt>,direction:String,currency:String) {
    val remaining=entries.fold(0L){sum,debt->checkedAdd(sum,checkedAdd(debt.amountMinor,-debt.paidMinor))}
    val paidCount=entries.count{it.paidMinor>=it.amountMinor || it.archived}
    DesignCard {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text(if(direction=="borrowed")MR.strings.plan_remaining_debt.text() else MR.strings.plan_remaining_receivable.text(),Modifier.weight(1f),style=FFType.bodyMain.copy(fontSize=15.sp,lineHeight=20.sp),color=FFColors.onSurfaceVariant)
            OlloPill(MR.strings.plan_record_count.text(entries.size),FFColors.outlineVariant,FFColors.onSurfaceVariant)
        }
        Spacer(Modifier.height(10.dp))
        FitMoneyText(moneyLabel(remaining,currency),FFType.amountLg.copy(fontSize=32.sp,lineHeight=40.sp),if(direction=="borrowed")FFColors.expense else FFColors.income)
        Spacer(Modifier.height(14.dp))
        if(entries.isEmpty()) OlloPill(if(direction=="borrowed")MR.strings.plan_no_debt.text() else MR.strings.plan_no_receivable.text(),FFColors.outlineVariant,FFColors.onSurfaceVariant)
        else OlloPill(MR.strings.plan_paid_count.text(paidCount),FFColors.income.copy(alpha=.12f),FFColors.income,icon="check_rounded")
    }
}

@Composable
private fun DebtCard(debt:Debt,data:FinanceData,busy:Boolean,expanded:Boolean,toggle:()->Unit,a:PlanningActions) {
    val paid=debt.paidMinor>=debt.amountMinor
    val remaining=(debt.amountMinor-debt.paidMinor).coerceAtLeast(0)
    val fraction=if(debt.amountMinor>0)(debt.paidMinor.toFloat()/debt.amountMinor).coerceIn(0f,1f) else 0f
    val accent=if(debt.direction=="borrowed")FFColors.expense else FFColors.income
    val payLabel=if(debt.direction=="borrowed")MR.strings.plan_pay_installment.text() else MR.strings.plan_receive_installment.text()
    val status=if(debt.archived)MR.strings.plan_archived.text() else if(paid)MR.strings.plan_paid_off.text() else MR.strings.plan_due_on.text(shortDateLabel(debt.dueDate))
    DesignCard(onClick=toggle) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(accent.copy(alpha=.12f)),contentAlignment=Alignment.Center){FFIcon(if(debt.direction=="borrowed")"south_west_rounded" else "north_east_rounded",accent,22.dp)}
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(debt.person,style=FFType.bodyMain.copy(fontSize=16.sp,lineHeight=22.sp),fontWeight=FontWeight.SemiBold,color=FFColors.onSurface,maxLines=1,overflow=TextOverflow.Ellipsis)
                Text(status,style=FFType.metaData,color=when{debt.archived->FFColors.outline;paid->FFColors.income;debt.dueDate<today().toString()->FFColors.expense;else->FFColors.onSurfaceVariant})
            }
            if(!busy)OlloMenuButton(listOf(OlloMenuItem("pay",payLabel,"payments_rounded"),OlloMenuItem("edit",MR.strings.plan_detail_edit.text(),"edit_rounded"))){key->if(key=="pay")a.payDebt(debt) else a.debt(debt)}
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Bottom) {
            Box(Modifier.weight(1f)){FitMoneyText(MR.strings.plan_remaining_amount.text(moneyLabel(remaining,debt.currency)),FFType.amountMd.copy(fontSize=20.sp,lineHeight=26.sp,fontWeight=FontWeight.SemiBold),if(paid)FFColors.income else FFColors.onSurface)}
            Box(Modifier.weight(1f)){FitMoneyText(MR.strings.plan_of_amount.text(moneyLabel(debt.amountMinor,debt.currency)),FFType.metaData,FFColors.onSurfaceVariant,TextAlign.End)}
        }
        Spacer(Modifier.height(10.dp))
        OlloProgress(fraction,6.dp,accent)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.weight(1f)){FitMoneyText(MR.strings.plan_paid_amount.text(moneyLabel(debt.paidMinor,debt.currency)),FFType.metaData,FFColors.onSurfaceVariant)}
            OlloPill(if(paid)MR.strings.plan_paid_off.text() else MR.strings.plan_ongoing.text(),if(paid)FFColors.income.copy(alpha=.12f) else FFColors.outlineVariant,if(paid)FFColors.income else FFColors.onSurfaceVariant)
        }
        if(!paid && !debt.archived){Spacer(Modifier.height(14.dp));OlloPrimaryButton(payLabel,enabled=!busy){a.payDebt(debt)}}
        if(expanded) {
            Spacer(Modifier.height(14.dp))
            Text(if(debt.includeBalance)MR.strings.plan_debt_started_balance.text(shortDateLabel(debt.issueDate)) else MR.strings.plan_debt_started_tracking.text(shortDateLabel(debt.issueDate)),style=FFType.metaData,color=FFColors.onSurfaceVariant)
            if(debt.note.isNotBlank()){Spacer(Modifier.height(6.dp));Text(debt.note,style=FFType.bodyMain,color=FFColors.onSurface)}
            data.financialEvents.filter{it.kind=="debt_payment" && it.referenceId==debt.id}.sortedByDescending{it.createdAt}.forEach{event->
                Spacer(Modifier.height(8.dp));PaymentHistory(event,busy,a.reverse)
            }
        }
    }
}

@Composable
private fun Wishes(data:FinanceData,busy:Boolean,a:PlanningActions) {
    val links=androidx.compose.ui.platform.LocalUriHandler.current
    var linkError by remember{mutableStateOf<String?>(null)}
    var completed by rememberSaveable{mutableStateOf(false)}
    val entries=data.wishes.filter{it.fulfilled==completed}.sortedBy{listOf("high","medium","low").indexOf(it.priority)}
    PlanningList(data,busy,MR.strings.plan_add_wish.text(),{a.wish(null)}) {
        val active=data.wishes.filter{!it.fulfilled}
        (if(active.isEmpty()) listOf(data.profile?.baseCurrency ?: "IDR") else active.map{it.currency}.distinct()).forEach{currency->
            item(key="wish-summary-$currency"){
                val list=active.filter{it.currency==currency}
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(FFColors.surface).padding(horizontal=20.dp,vertical=20.dp)) {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Text(MR.strings.plan_total_wish_value.text(),Modifier.weight(1f),style=FFType.bodyMain.copy(fontSize=15.sp),color=FFColors.onSurfaceVariant)
                        OlloPill(MR.strings.plan_wish_count.text(list.size),FFColors.outlineVariant,FFColors.onSurface,icon="stars_rounded")
                    }
                    Spacer(Modifier.height(14.dp))
                    FitMoneyText(moneyLabel(list.fold(0L){s,w->checkedAdd(s,w.targetMinor)},currency),FFType.amountLg.copy(fontSize=33.sp,lineHeight=40.sp),FFColors.onSurface)
                }
            }
        }
        item{OlloSegmented(listOf("active" to MR.strings.plan_active.text(),"done" to MR.strings.plan_achieved.text()),if(completed)"done" else "active",{completed=it=="done"})}
        if(entries.isEmpty())item{PlanningEmpty("hi_heart",if(completed)MR.strings.plan_no_wish_achieved.text() else MR.strings.plan_wishlist_empty.text(),MR.strings.plan_wish_empty_msg.text())}
        items(entries,key={it.id}){wish->DesignCard(onClick={a.wish(wish)}) {
            Row {Text(wish.title,Modifier.weight(1f),style=FFType.sectionTitle);FFIcon("favorite_border_rounded",FFColors.expense)}
            Text(displayMoney(wish.targetMinor,wish.currency),style=FFType.amountMd)
            Text(MR.strings.plan_priority_label.text(when(wish.priority){"high"->MR.strings.plan_priority_high.text();"low"->MR.strings.plan_priority_low.text();else->MR.strings.plan_priority_medium.text()}),style=FFType.metaData,color=FFColors.outline)
            if(wish.link.isNotBlank())TextButton(enabled=!busy,onClick={try{links.openUri(wish.link)}catch(e:Exception){linkError=MR.strings.plan_link_error.text()}}){Text(MR.strings.plan_open_product_link.text(),style=FFType.metaData)}
            if(wish.note.isNotBlank())Text(wish.note,style=FFType.bodyMain)
            TextButton(onClick={a.fulfillWish(wish.copy(fulfilled=!wish.fulfilled))},enabled=!busy) {Text(if(wish.fulfilled)MR.strings.plan_back_to_wishlist.text() else MR.strings.plan_mark_fulfilled.text())}
        }}
    }
    linkError?.let{AlertDialog(onDismissRequest={linkError=null},title={Text(MR.strings.plan_product_link.text())},text={Text(it)},confirmButton={TextButton(onClick={linkError=null}){Text(MR.strings.common_close.text())}})}
}

@Composable
private fun Cards(data:FinanceData,busy:Boolean,a:PlanningActions) {
    var tab by rememberSaveable{mutableStateOf(if(data.cards.any{it.recordType=="card"})"card" else "bank")}
    val entries=data.cards.filter{it.recordType==tab}.sortedWith(compareByDescending<WalletCard>{it.pinned}.thenByDescending{it.createdAt})
    PlanningList(data,busy,MR.strings.plan_card_add_title.text(),{a.card(null)}) {
        item { OlloUnderlineTabs(listOf(Triple("bank",MR.strings.plan_record_bank.text(),"hi_bank"),Triple("card",MR.strings.plan_record_card.text(),"hi_card"),Triple("ewallet",MR.strings.plan_record_ewallet.text(),"hi_ewallet"),Triple("other",MR.strings.plan_other.text(),"hi_more")),tab,{tab=it}) }
        if(entries.isEmpty())item{PlanningEmpty(when(tab){"card"->"hi_card";"ewallet"->"hi_ewallet";"other"->"hi_wallet";else->"hi_bank"},when(tab){"bank"->MR.strings.plan_no_bank_accounts.text();"card"->MR.strings.plan_no_cards.text();"ewallet"->MR.strings.plan_no_ewallets.text();else->MR.strings.plan_no_other_accounts.text()},MR.strings.plan_cards_empty_msg.text())}
        if(tab=="card")item{Text(MR.strings.plan_card_last4_note.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant)}
        items(entries,key={it.id}){card->
            WalletCardPreview(card,if(card.accountId.isNotBlank())data.accountName(card.accountId) else "",onClick={if(!busy)a.card(card)})
        }
    }
}

@Composable
private fun Notes(data:FinanceData,busy:Boolean,a:PlanningActions) {
    var query by rememberSaveable{mutableStateOf("")}
    var tab by rememberSaveable{mutableStateOf("active")}
    val entries=data.notes.filter{note->
        (query.isBlank() || "${note.title} ${note.body} ${note.items.joinToString(" "){it.name}}".contains(query,true)) &&
            when(tab){"notes"->note.kind=="note";"history"->note.kind=="bundle" && (note.archived || note.items.any{it.paymentId!=null});else->note.kind=="bundle" && !note.archived && note.items.any{it.paymentId==null}}
    }.sortedWith(compareByDescending<FinanceNote>{it.pinned}.thenByDescending{it.updatedAt})
    PlanningList(data,busy,MR.strings.plan_new_bundle.text(),{a.note(null)},extendedFab=true) {
        item{OlloSegmented(listOf("active" to MR.strings.plan_active.text(),"history" to MR.strings.plan_history.text(),"notes" to MR.strings.plan_note.text()),tab,{tab=it})}
        if(data.notes.isNotEmpty())item{
            Row(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp)).background(FFColors.surface).padding(horizontal=14.dp),verticalAlignment=Alignment.CenterVertically){
                FFIcon("hi_search",FFColors.onSurfaceVariant,20.dp);Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f)){
                    if(query.isEmpty())Text(MR.strings.plan_search_notes.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant)
                    androidx.compose.foundation.text.BasicTextField(query,{query=it},Modifier.fillMaxWidth().semantics{contentDescription=MR.strings.plan_search_notes.text()},singleLine=true,
                        textStyle=FFType.metaData.copy(color=FFColors.onSurface),cursorBrush=androidx.compose.ui.graphics.SolidColor(FFColors.onSurface))
                }
            }
        }
        if(entries.isEmpty())item{PlanningEmpty(if(tab=="notes")"hi_note_doc" else "hi_bag",when(tab){"notes"->MR.strings.plan_no_notes.text();"history"->MR.strings.plan_history_empty.text();else->MR.strings.plan_first_bundle.text()},null)}
        items(entries,key={it.id}){note->
            if(note.kind=="note")DesignCard(onClick={a.note(note)}) {
                Row {Text(note.title,Modifier.weight(1f),style=FFType.sectionTitle);if(note.pinned)Text(MR.strings.plan_pinned.text(),style=FFType.badgeLabel,color=FFColors.primary)}
                Spacer(Modifier.height(8.dp));Text(note.body,style=FFType.bodyMain,color=FFColors.onSurfaceVariant,maxLines=5,overflow=TextOverflow.Ellipsis)
            } else BundleCard(note,data,busy,tab=="history",a)
        }
    }
}

@Composable
private fun BundleCard(note:FinanceNote,data:FinanceData,busy:Boolean,history:Boolean,a:PlanningActions) {
    val selected=note.items.filter{it.checked && it.paymentId==null}
    val total=note.items.fold(0L){sum,item->checkedAdd(sum,item.amountMinor)}
    val payable=selected.fold(0L){sum,item->checkedAdd(sum,item.amountMinor)}
    val paidCount=note.items.count{it.paymentId!=null}
    DesignCard(modifier=Modifier.testTag("bundle-${note.id}")) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(note.title,style=FFType.sectionTitle)
                Text(MR.strings.plan_bundle_paid_progress.text(paidCount,note.items.size,if(note.recordAs=="single")MR.strings.plan_record_single.text() else MR.strings.plan_per_item.text()),style=FFType.metaData,color=FFColors.outline)
            }
            TextButton(onClick={a.note(note)},enabled=!busy){Text(MR.strings.plan_detail.text())}
        }
        Spacer(Modifier.height(12.dp))
        note.items.forEach { item ->
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Checkbox(checked=item.checked,onCheckedChange={checked->
                    a.updateNote(note.copy(items=note.items.map{if(it.id==item.id)it.copy(checked=checked) else it}))
                },enabled=!busy && !note.archived && item.paymentId==null && !history,modifier=Modifier.semantics{contentDescription=MR.strings.plan_item_n.text(item.name)})
                Column(Modifier.weight(1f)) {
                    Text(item.name,style=FFType.bodyMain,maxLines=2,overflow=TextOverflow.Ellipsis)
                    if(item.paymentId!=null)Text(MR.strings.plan_already_paid.text(),style=FFType.metaData,color=FFColors.income)
                }
                Text(displayMoney(item.amountMinor,note.currency),style=FFType.metaData,fontWeight=FontWeight.SemiBold)
            }
        }
        HorizontalDivider(Modifier.padding(vertical=12.dp),color=FFColors.outlineVariant)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text(MR.strings.plan_estimated_total.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant)
            Text(displayMoney(total,note.currency),style=FFType.bodyMain,fontWeight=FontWeight.SemiBold)
        }
        if(note.body.isNotBlank()){Spacer(Modifier.height(8.dp));Text(note.body,style=FFType.metaData,color=FFColors.onSurfaceVariant,maxLines=3,overflow=TextOverflow.Ellipsis)}
        Spacer(Modifier.height(12.dp))
        if(!history && !note.archived)Button(onClick={a.payBundle(note,selected.map{it.id})},enabled=!busy && selected.isNotEmpty(),modifier=Modifier.fillMaxWidth()) {
            Text(if(selected.isEmpty())MR.strings.plan_check_items_to_pay.text() else MR.strings.plan_pay_amount.text(displayMoney(payable,note.currency)))
        }
        if(history) {
            if(note.archived)Text(MR.strings.plan_archived.text(),style=FFType.metaData,color=FFColors.outline)
            data.financialEvents.filter{it.kind=="bundle" && it.referenceId==note.id}.sortedByDescending{it.createdAt}.forEach { event ->
                Spacer(Modifier.height(8.dp));PaymentHistory(event,busy,a.reverse)
            }
        }
    }
}

@Composable
private fun Claims(data:FinanceData,busy:Boolean,a:PlanningActions) {
    var received by rememberSaveable{mutableStateOf(false)}
    val entries=data.reimbursements.filter{(it.receivedAt>0)==received}
    val available=data.transactions.any{it.type=="expense" && !it.scheduled && data.reimbursements.none{c->c.expenseId==it.id}}
    PlanningList(data,busy,if(available)MR.strings.plan_claim_new_title.text() else MR.strings.plan_record_expense.text(),if(available)({a.claim(null)}) else a.addExpense) {
        item{OlloUnderlineTabs(listOf(Triple("wait",MR.strings.plan_waiting.text(),null),Triple("paid",MR.strings.plan_received.text(),null)),if(received)"paid" else "wait",{received=it=="paid"})}
        if(entries.isEmpty())item{
            Column(Modifier.fillMaxWidth().padding(top=180.dp),horizontalAlignment=Alignment.CenterHorizontally){
                Text(if(received)MR.strings.plan_no_receipts.text() else MR.strings.plan_no_claims.text(),style=FFType.bodyMain.copy(fontSize=16.sp),color=FFColors.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text(MR.strings.plan_claims_empty_msg.text(),Modifier.padding(horizontal=24.dp),style=FFType.metaData,color=FFColors.onSurfaceVariant.copy(alpha=.75f),textAlign=TextAlign.Center)
            }
        }
        items(entries,key={it.id}){claim->DesignCard(onClick=if(claim.receivedAt==0L)({a.claim(claim)}) else null) {
            Text(claim.title,style=FFType.sectionTitle)
            Text(claim.party,style=FFType.metaData,color=FFColors.outline)
            Text(displayMoney(claim.amountMinor,claim.currency),style=FFType.amountMd)
            Spacer(Modifier.height(8.dp))
            if(claim.receivedAt==0L)Button(onClick={a.receiveClaim(claim)},enabled=!busy){Text(MR.strings.plan_receive_reimburse.text())}
            else data.financialEvents.find{it.kind=="reimbursement" && it.referenceId==claim.id}?.let {PaymentHistory(it,busy,a.reverse)}
        }}
    }
}

@Composable
private fun Recurring(data:FinanceData,busy:Boolean,a:PlanningActions) {
    var inactive by rememberSaveable{mutableStateOf(false)}
    var month by rememberSaveable{mutableStateOf(today().toString().take(7))}
    var selectedDay by rememberSaveable{mutableStateOf(today().toString())}
    val entries=data.recurring.filter{it.active!=inactive}
    val calendarEntries=calendarDates(month,LocalPreferences.current.weekStart).flatMap{date->
        data.recurring.filter{recurringOn(it,date)}.map{rule->Transaction(id="${rule.id}:$date",type=rule.type,amountMinor=rule.amountMinor,
            currency=rule.currency,accountId=rule.accountId,categoryId=rule.categoryId,title=rule.title,date=date.toString())}
    }
    PlanningList(data,busy,MR.strings.plan_recurring_new_title.text(),{a.recurring(null)}) {
        item{TransactionCalendar(month,{month=it},selectedDay,{selectedDay=it},calendarEntries)}
        item{DesignCard{
            Text(MR.strings.plan_schedule_on.text(selectedDay),style=FFType.sectionTitle)
            val due=calendarEntries.filter{it.date==selectedDay}
            Text(MR.strings.plan_scheduled_count.text(due.size),style=FFType.metaData)
            due.groupBy{it.currency}.forEach{(currency,items)->
                FitMoneyText(displayMoney(items.fold(0L){sum,t->checkedAdd(sum,t.amountMinor)},currency),FFType.amountMd)
            }
        }}
        item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){DesignPill(MR.strings.plan_active.text(),!inactive,{inactive=false});DesignPill(MR.strings.plan_inactive.text(),inactive,{inactive=true})}}
        item {Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {OutlinedButton(onClick=a.processRecurring,enabled=!busy){Text(MR.strings.plan_process_schedule.text())};TextButton(onClick=a.manualScheduled){Text(MR.strings.plan_scheduled_calendar.text())}}}
        if(entries.isEmpty())item{EmptyPlanning(MR.strings.plan_no_recurring.text(),MR.strings.plan_no_recurring_msg.text())}
        items(entries,key={it.id}){rule->DesignCard(onClick={a.recurring(rule)}) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)){Text(rule.title,style=FFType.sectionTitle);Text(MR.strings.plan_next_on.text(rule.nextDate),style=FFType.metaData,color=FFColors.outline)}
                Text(displayMoney(rule.amountMinor,rule.currency),style=FFType.amountMd,color=if(rule.type=="income")FFColors.income else FFColors.expense)
            }
            Spacer(Modifier.height(8.dp))
            Text(when(rule.frequency){"daily"->MR.strings.plan_every_day.text(rule.interval,data.accountName(rule.accountId));"weekly"->MR.strings.plan_every_week.text(rule.interval,data.accountName(rule.accountId));"yearly"->MR.strings.plan_every_year.text(rule.interval,data.accountName(rule.accountId));else->MR.strings.plan_every_month.text(rule.interval,data.accountName(rule.accountId))},style=FFType.metaData)
            if(rule.endDate!=null)Text(MR.strings.plan_until.text(rule.endDate.orEmpty()),style=FFType.metaData,color=FFColors.outline)
        }}
    }
}

// ---------------- Komponen paritas Ollo ----------------
// Dipakai layar Tagihan/Utang di berkas ini serta layar Target di feature/goal.

@Composable
internal fun OlloFormSection(title: String) {
    Text(title, style = FFType.sectionTitle, color = FFColors.onSurface)
}

@Composable
internal fun OlloSegmented(options: List<Pair<String, String>>, selected: String, select: (String) -> Unit, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(18.dp)).background(FFColors.surface).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
        options.forEach { (value, label) ->
            Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(14.dp)).background(if (value == selected) FFColors.pill else Color.Transparent).semantics { this.selected = value == selected }.clickable(role = Role.Tab) { select(value) }, contentAlignment = Alignment.Center) {
                Text(label, style = FFType.bodyMain.copy(fontSize = 15.sp, lineHeight = 20.sp), color = if (value == selected) FFColors.onPill else FFColors.onSurfaceVariant, maxLines = 1)
            }
        }
        trailing?.invoke()
    }
}

@Composable
internal fun OlloRefreshButton(onClick: () -> Unit) {
    Box(Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        FFIcon("hi_recurring", FFColors.onSurfaceVariant, 24.dp, MR.strings.plan_reload.text())
    }
}

internal data class OlloMenuItem(val key: String, val label: String, val icon: String = "")

@Composable
internal fun OlloMenuButton(items: List<OlloMenuItem>, tint: Color = FFColors.onSurfaceVariant, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton({ open = true }) { FFIcon("more_vert", tint, 22.dp, MR.strings.plan_menu.text()) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.label, style = FFType.bodyMain) },
                    leadingIcon = if (item.icon.isBlank()) null else ({
                        Box(Modifier.size(34.dp).clip(RoundedCornerShape(12.dp)).background(FFColors.surfaceLow), contentAlignment = Alignment.Center) {
                            FFIcon(item.icon, FFColors.onSurface, 18.dp)
                        }
                    }),
                    onClick = { open = false; onSelect(item.key) },
                )
            }
        }
    }
}

@Composable
internal fun OlloPrimaryButton(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(16.dp), color = FFColors.onSurface) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, style = FFType.bodyMain.copy(fontSize = 15.sp, lineHeight = 20.sp), fontWeight = FontWeight.SemiBold, color = FFColors.canvas)
        }
    }
}

@Composable
internal fun OlloSecondaryButton(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(16.dp), color = FFColors.canvas) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, style = FFType.bodyMain.copy(fontSize = 15.sp, lineHeight = 20.sp), fontWeight = FontWeight.SemiBold, color = FFColors.onSurface)
        }
    }
}

@Composable
internal fun OlloFab(onClick: () -> Unit, description: String = MR.strings.common_add.text()) {
    Surface(onClick = onClick, modifier = Modifier.size(56.dp), shape = RoundedCornerShape(16.dp), color = FFColors.pill, shadowElevation = 6.dp) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { FFIcon("hi_add", FFColors.onPill, 26.dp, description) }
    }
}

@Composable
internal fun OlloPill(label: String, background: Color, contentColor: Color, icon: String? = null) {
    Row(Modifier.clip(RoundedCornerShape(100.dp)).background(background).padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) { FFIcon(icon, contentColor, 14.dp); Spacer(Modifier.width(5.dp)) }
        Text(label, style = FFType.badgeLabel, color = contentColor, maxLines = 1)
    }
}

@Composable
internal fun OlloProgress(progress: Float, height: Dp = 6.dp, color: Color = FFColors.onSurface) {
    val fraction = progress.coerceIn(0f, 1f)
    Box(Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(100.dp)).background(FFColors.outlineVariant)) {
        if (fraction > 0f) Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(RoundedCornerShape(100.dp)).background(color))
    }
}

@Composable
internal fun OlloDashedProgress(progress: Float) {
    val filled = (progress.coerceIn(0f, 1f) * 12f).roundToInt()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(12) { index ->
            Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(100.dp)).background(if (index < filled) FFColors.onSurface else FFColors.outlineVariant))
        }
    }
}

@Composable
internal fun OlloEmptyState(icon: String, title: String, message: String, tip: String? = null) {
    Column(Modifier.fillMaxWidth().padding(top = 72.dp, bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(96.dp).clip(CircleShape).background(FFColors.raised), contentAlignment = Alignment.Center) { FFIcon(if (icon == "check_rounded") "hi_check_circle" else icon, FFColors.onSurfaceVariant, 44.dp) }
        Spacer(Modifier.height(22.dp))
        Text(title, style = FFType.sectionTitle.copy(fontWeight = FontWeight.SemiBold), color = FFColors.onSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(message, style = FFType.bodyMain, color = FFColors.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
        if (tip != null) {
            Spacer(Modifier.height(24.dp))
            Row(Modifier.padding(horizontal = 24.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.dp, FFColors.hairline, RoundedCornerShape(16.dp)).background(FFColors.surface).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                FFIcon("hi_bulb", Color(0xFFF0A63A), 20.dp)
                Spacer(Modifier.width(12.dp))
                Text(tip, style = FFType.metaData.copy(fontSize = 10.5.sp, lineHeight = 15.sp), color = FFColors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun moneyLabel(value: Long, currency: String): String {
    val preferences = LocalPreferences.current
    return if (preferences.hideBalances) maskedBalance(value, currency, preferences) else formatBalance(value, currency, preferences)
}
