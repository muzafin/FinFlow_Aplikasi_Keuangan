package com.logikawan.finflow.feature.account

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.logikawan.finflow.core.model.*
import com.logikawan.finflow.core.ui.*
import com.logikawan.finflow.core.util.*
import kotlinx.datetime.*

fun accountDelta(transaction:Transaction,accountId:String):Long = when {
    transaction.scheduled->0L
    transaction.accountId==accountId -> if(transaction.type=="income") transaction.amountMinor else -transaction.amountMinor
    transaction.type=="transfer" && transaction.toAccountId==accountId -> transaction.receivedAmountMinor ?: transaction.amountMinor
    else -> 0L
}

private val walletMonths=listOf("Jan","Feb","Mar","Apr","Mei","Jun","Jul","Agu","Sep","Okt","Nov","Des")

private fun dateRangeLabel(period:FinancePeriod):String {
    val start=period.start;val end=period.endExclusive.plus(DatePeriod(days=-1))
    return "${start.dayOfMonth} ${walletMonths[start.monthNumber-1]} – ${end.dayOfMonth} ${walletMonths[end.monthNumber-1]} ${end.year}"
}

private fun shortRangeLabel(period:FinancePeriod):String {
    val start=period.start;val end=period.endExclusive.plus(DatePeriod(days=-1))
    return "${start.dayOfMonth} ${walletMonths[start.monthNumber-1]} – ${end.dayOfMonth} ${walletMonths[end.monthNumber-1]}"
}

@Composable
fun WalletDetail(account:Account,data:FinanceData,add:(String)->Unit,openTransaction:(Transaction)->Unit) {
    var kind by rememberSaveable{mutableStateOf("month")}
    var anchor by rememberSaveable{mutableStateOf(today().toString())}
    val p=LocalPreferences.current
    val date=LocalDate.parse(anchor)
    val range=financePeriod(kind,date,activeRecordingMonth(date,p.recordingDay),p.recordingDay,p.weekStart)
    val all=data.transactions.filter{!it.scheduled && (it.accountId==account.id || it.toAccountId==account.id)}
    val entries=all.filter{kind=="all" || it.date in range}.sortedWith(compareByDescending<Transaction>{it.date}.thenByDescending{it.createdAt})
    val inflow=entries.fold(0L){sum,t->checkedAdd(sum,accountDelta(t,account.id).coerceAtLeast(0))}
    val outflow=entries.fold(0L){sum,t->checkedAdd(sum,-accountDelta(t,account.id).coerceAtMost(0))}
    fun shift(direction:Int){anchor=date.plus(when(kind){"week"->DatePeriod(days=7*direction);"year"->DatePeriod(years=direction);else->DatePeriod(months=direction)}).toString()}
    val summaryTitle=when(kind){"week"->"Ringkasan Mingguan";"year"->"Ringkasan Tahunan";"all"->"Ringkasan Semua";else->"Ringkasan Bulanan"}
    val summaryRange=if(kind=="all")"Semua transaksi" else shortRangeLabel(range)
    LazyColumn(Modifier.fillMaxSize().testTag("wallet-detail"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        item{SegmentedTabs(listOf("week" to "Mingguan","month" to "Bulanan","year" to "Tahunan","all" to "Semua"),kind,{kind=it})}
        if(kind!="all")item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){FFIconButton("chevron_left_rounded",{shift(-1)});Text(dateRangeLabel(range),Modifier.weight(1f),style=FFType.metaData,textAlign=TextAlign.Center);FFIconButton("chevron_right_rounded",{shift(1)})}}
        item{Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally){Text("Saldo Saat Ini",style=FFType.bodyMain,color=FFColors.onSurfaceVariant);FitMoneyText(formatBalance(data.balance(account),account.currency,p),FFType.amountLg)}}
        item {
            val start=if(kind=="all")all.minOfOrNull{LocalDate.parse(it.date)} ?: today() else range.start
            val end=if(kind=="all")today().plus(DatePeriod(days=1)) else range.endExclusive
            val days=(end.toEpochDays()-start.toEpochDays()).coerceAtLeast(1)
            val points=(0..minOf(days,60)).map{i->val day=start.plus(DatePeriod(days=(days.toLong()*i/minOf(days,60)).toInt()));all.filter{it.date<=day.toString()}.fold(account.openingBalanceMinor){sum,t->checkedAdd(sum,accountDelta(t,account.id))}.toDouble()}
            val lineColor=FFColors.primary;val gridColor=FFColors.outlineVariant
            Column {
                Canvas(Modifier.fillMaxWidth().height(140.dp)){
                    repeat(5){i->val y=size.height*i/4;drawLine(gridColor,Offset(0f,y),Offset(size.width,y),1.dp.toPx())}
                    val min=points.minOrNull() ?: 0.0;val span=((points.maxOrNull() ?: 0.0)-min).coerceAtLeast(1.0)
                    val path=Path();points.forEachIndexed{i,v->val x=size.width*i/(points.size-1).coerceAtLeast(1);val y=(size.height*(1-(v-min)/span)).toFloat();if(i==0)path.moveTo(x,y) else path.lineTo(x,y)};drawPath(path,lineColor,style=Stroke(2.dp.toPx()))
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    Text(formatBalance(points.first().toLong(),account.currency,p),style=FFType.badgeLabel,color=FFColors.outline)
                    Text(formatBalance(points.last().toLong(),account.currency,p),style=FFType.badgeLabel,color=FFColors.outline)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text(summaryTitle,Modifier.weight(1f),style=FFType.sectionTitle)
                Text(summaryRange,style=FFType.metaData,color=FFColors.outline)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){WalletMetric("Arus Masuk",formatBalance(inflow,account.currency,p),Modifier.weight(1f));WalletMetric("Arus Keluar",formatBalance(outflow,account.currency,p),Modifier.weight(1f))}
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){WalletMetric("Arus Bersih",formatBalance(checkedAdd(inflow,-outflow),account.currency,p),Modifier.weight(1f));WalletMetric("Jumlah Transaksi",entries.size.toString(),Modifier.weight(1f))}
        }
        if(!account.archived)item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){
            WalletAction("Pengeluaran","arrow_upward_rounded",FFColors.expense,Modifier.weight(1f)){add("expense")}
            WalletAction("Pemasukan","arrow_downward_rounded",FFColors.income,Modifier.weight(1f)){add("income")}
            WalletAction("Transfer","swap_horiz",FFColors.primary,Modifier.weight(1f)){add("transfer")}
        }}
        item{Text("Transaksi Terbaru",style=FFType.sectionTitle)}
        if(entries.isEmpty())item{Column(Modifier.fillMaxWidth().padding(vertical=24.dp),horizontalAlignment=Alignment.CenterHorizontally){Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(FFColors.surface),contentAlignment=Alignment.Center){FFIcon("receipt_long",FFColors.outline,30.dp)};Spacer(Modifier.height(12.dp));Text("Belum ada transaksi",style=FFType.bodyMain,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(6.dp));Text("Ayo mulai catat pengeluaran dan pemasukanmu agar keuangan lebih rapi!",style=FFType.metaData,color=FFColors.outline,textAlign=TextAlign.Center)}}
        else items(entries,key={it.id}){tx->DesignCard(onClick={openTransaction(tx)}){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(tx.title,style=FFType.bodyMain);Text(tx.date,style=FFType.metaData,color=FFColors.onSurfaceVariant)};Text(formatBalance(accountDelta(tx,account.id),account.currency,p),style=FFType.amountSm)}}}
    }
}

@Composable private fun WalletAction(label:String,icon:String,color:Color,modifier:Modifier,click:()->Unit) {
    Column(modifier.height(86.dp).clip(RoundedCornerShape(22.dp)).background(FFColors.surface)
        .clickable(role=Role.Button,onClick=click),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
        FFIcon(icon,color,22.dp)
        Spacer(Modifier.height(8.dp))
        Text(label,style=FFType.metaData,fontWeight=FontWeight.SemiBold)
    }
}

@Composable private fun WalletMetric(label:String,value:String,modifier:Modifier){DesignCard(modifier){Text(label,style=FFType.metaData,color=FFColors.onSurfaceVariant);Spacer(Modifier.height(8.dp));FitMoneyText(value,FFType.amountMd)}}
