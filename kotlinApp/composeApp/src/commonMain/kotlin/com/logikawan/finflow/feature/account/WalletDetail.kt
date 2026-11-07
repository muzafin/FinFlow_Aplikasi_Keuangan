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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import com.logikawan.finflow.feature.dashboard.DashboardTransactionRow
import com.logikawan.finflow.feature.dashboard.fullDateLabel
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
import com.logikawan.finflow.core.i18n.transactionTypeLabel
import dev.icerock.moko.resources.StringResource
import com.logikawan.finflow.core.i18n.text
import com.logikawan.finflow.i18n.*
import kotlinx.datetime.*

fun accountDelta(transaction:Transaction,accountId:String):Long = when {
    transaction.scheduled->0L
    transaction.accountId==accountId -> if(transaction.type=="income") transaction.amountMinor else -transaction.amountMinor
    transaction.type=="transfer" && transaction.toAccountId==accountId -> transaction.receivedAmountMinor ?: transaction.amountMinor
    else -> 0L
}

private val walletMonths:List<StringResource> get()=listOf(MR.strings.acc_month_jan,MR.strings.acc_month_feb,MR.strings.acc_month_mar,MR.strings.acc_month_apr,
    MR.strings.acc_month_may,MR.strings.acc_month_jun,MR.strings.acc_month_jul,MR.strings.acc_month_aug,MR.strings.acc_month_sep,MR.strings.acc_month_oct,
    MR.strings.acc_month_nov,MR.strings.acc_month_dec)

private fun dateRangeLabel(period:FinancePeriod):String {
    val start=period.start;val end=period.endExclusive.plus(DatePeriod(days=-1))
    return MR.strings.acc_date_range_year.text(start.dayOfMonth,walletMonths[start.monthNumber-1].text(),end.dayOfMonth,walletMonths[end.monthNumber-1].text(),end.year)
}

private fun shortRangeLabel(period:FinancePeriod):String {
    val start=period.start;val end=period.endExclusive.plus(DatePeriod(days=-1))
    return MR.strings.acc_date_range.text(start.dayOfMonth,walletMonths[start.monthNumber-1].text(),end.dayOfMonth,walletMonths[end.monthNumber-1].text())
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
    val summaryTitle=when(kind){"week"->MR.strings.acc_summary_weekly.text();"year"->MR.strings.acc_summary_yearly.text();"all"->MR.strings.acc_summary_all.text();else->MR.strings.acc_summary_monthly.text()}
    val summaryRange=if(kind=="all")MR.strings.acc_all_transactions.text() else shortRangeLabel(range)
    // Susunan detail dompet Ollo: segmen periode, navigasi tanggal, saldo di tengah, grafik pendek berisi,
    // ringkasan 2×2, tombol aksi, lalu transaksi terbaru bergaya Beranda.
    val canGoNext=kind=="all" || range.endExclusive<=today()
    LazyColumn(Modifier.fillMaxSize().testTag("wallet-detail"),contentPadding=PaddingValues(start=16.dp,end=16.dp,top=4.dp,bottom=40.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item{SegmentedTabs(listOf("week" to MR.strings.acc_tab_weekly.text(),"month" to MR.strings.acc_tab_monthly.text(),"year" to MR.strings.acc_tab_yearly.text(),"all" to MR.strings.common_all.text()),kind,{kind=it})}
        if(kind!="all")item{Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically){
            IconButton({shift(-1)}){FFIcon("hi_chevron_left",FFColors.onSurface,20.dp,contentDescription=MR.strings.acc_previous.text())}
            Text(dateRangeLabel(range),Modifier.weight(1f),style=FFType.bodyMain,textAlign=TextAlign.Center)
            IconButton({shift(1)},enabled=canGoNext){FFIcon("hi_chevron_right",if(canGoNext)FFColors.onSurface else FFColors.onSurfaceVariant.copy(alpha=.4f),20.dp,contentDescription=MR.strings.acc_next_period.text())}
        }}
        item{Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally){
            Text(MR.strings.acc_current_balance.text(),style=FFType.bodyMain.copy(fontSize=15.sp),color=FFColors.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            FitMoneyText(formatBalance(data.balance(account),account.currency,p),FFType.amountXl.copy(fontSize=36.sp),FFColors.onSurface,TextAlign.Center)
        }}
        item {
            val start=if(kind=="all")all.minOfOrNull{LocalDate.parse(it.date)} ?: today() else range.start
            val end=if(kind=="all")today().plus(DatePeriod(days=1)) else range.endExclusive
            val days=(end.toEpochDays()-start.toEpochDays()).coerceAtLeast(1)
            val steps=minOf(days,60)
            val lastIndex=if(today()<end) ((today().toEpochDays()-start.toEpochDays()).toLong()*steps/days).toInt().coerceIn(0,steps) else steps
            val points=(0..steps).map{i->val day=start.plus(DatePeriod(days=(days.toLong()*i/steps).toInt()));all.filter{it.date<=day.toString()}.fold(account.openingBalanceMinor){sum,t->checkedAdd(sum,accountDelta(t,account.id))}}
            WalletBalanceChart(points,lastIndex,account.currency)
        }
        item {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text(summaryTitle,Modifier.weight(1f),style=FFType.bodyMain.copy(fontSize=16.sp,fontWeight=FontWeight.Medium))
                Text(summaryRange,style=FFType.metaData,color=FFColors.onSurfaceVariant)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){WalletMetric(MR.strings.acc_inflow.text(),formatBalance(inflow,account.currency,p),Modifier.weight(1f));WalletMetric(MR.strings.acc_outflow.text(),formatBalance(outflow,account.currency,p),Modifier.weight(1f))}
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){WalletMetric(MR.strings.acc_net_flow.text(),formatBalance(checkedAdd(inflow,-outflow),account.currency,p),Modifier.weight(1f));WalletMetric(MR.strings.acc_transaction_count.text(),entries.size.toString(),Modifier.weight(1f))}
        }
        if(!account.archived)item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){
            WalletAction(transactionTypeLabel("expense"),"hi_arrow_up_right",FFColors.expense,Modifier.weight(1f)){add("expense")}
            WalletAction(transactionTypeLabel("income"),"hi_arrow_down_left",FFColors.income,Modifier.weight(1f)){add("income")}
            WalletAction(transactionTypeLabel("transfer"),"hi_sort",Color(0xFF2196F3),Modifier.weight(1f)){add("transfer")}
        }}
        item{Text(MR.strings.acc_recent_transactions.text(),Modifier.padding(top=8.dp),style=FFType.sectionTitle)}
        if(entries.isEmpty())item{Column(Modifier.fillMaxWidth().padding(vertical=24.dp),horizontalAlignment=Alignment.CenterHorizontally){Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(FFColors.surface),contentAlignment=Alignment.Center){FFIcon("receipt_long",FFColors.outline,30.dp)};Spacer(Modifier.height(12.dp));Text(MR.strings.acc_no_transactions.text(),style=FFType.bodyMain,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(6.dp));Text(MR.strings.acc_no_transactions_hint.text(),style=FFType.metaData,color=FFColors.outline,textAlign=TextAlign.Center)}}
        else entries.groupBy{it.date}.forEach{(date,rows)->
            item(key="date-$date"){
                val net=rows.fold(0L){sum,t->checkedAdd(sum,accountDelta(t,account.id))}
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    Text(fullDateLabel(date),Modifier.weight(1f),style=FFType.bodyMain,color=FFColors.onSurfaceVariant)
                    if(net!=0L)Text((if(net>0)"+" else "")+formatBalance(net,account.currency,p),style=FFType.bodyMain.copy(fontSize=17.sp,lineHeight=22.sp),color=if(net>0)FFColors.income else FFColors.expense)
                }
            }
            items(rows,key={it.id}){tx->
                val delta=accountDelta(tx,account.id)
                DashboardTransactionRow(tx,data,(if(delta>0)"+" else if(delta<0)"−" else "")+formatBalance(kotlin.math.abs(delta),account.currency,p),openTransaction,topPadding=0.dp)
            }
        }
    }
}

@Composable private fun WalletBalanceChart(points:List<Long>,lastIndex:Int,currency:String) {
    val line=FFColors.onSurface
    val grid=FFColors.outlineVariant
    val axis=FFColors.onSurfaceVariant
    val measurer=androidx.compose.ui.text.rememberTextMeasurer()
    val labelStyle=FFType.metaData.copy(fontSize=8.sp,color=axis)
    val maximum=maxOf(points.maxOrNull() ?: 0L,1L)
    val minimum=minOf(points.minOrNull() ?: 0L,0L)
    val top=maximum+(maximum-minimum)/10
    Canvas(Modifier.fillMaxWidth().height(84.dp).testTag("wallet-balance-chart")){
        val left=26.dp.toPx(); val bottom=size.height-2.dp.toPx(); val chartTop=4.dp.toPx()
        val span=(top-minimum).coerceAtLeast(1L).toFloat()
        fun y(v:Long)=bottom-(v-minimum)/span*(bottom-chartTop)
        fun x(i:Int)=left+i.toFloat()/(points.size-1).coerceAtLeast(1)*(size.width-left)
        repeat(4){i->val gx=left+(size.width-left)*(i+1)/5;drawLine(grid.copy(alpha=.5f),Offset(gx,chartTop),Offset(gx,bottom),1.dp.toPx())}
        listOf(minimum,(minimum+top)/2,top).forEach{v->
            val label=measurer.measure(compactDisplayMoney(v,currency).removePrefix("Rp"),labelStyle)
            drawText(label,topLeft=Offset(0f,(y(v)-label.size.height/2f).coerceIn(0f,size.height-label.size.height)))
        }
        val end=lastIndex.coerceIn(0,points.size-1)
        val path=Path(); for(i in 0..end){if(i==0)path.moveTo(x(i),y(points[i])) else path.lineTo(x(i),y(points[i]))}
        val fill=Path().apply{addPath(path);lineTo(x(end),bottom);lineTo(x(0),bottom);close()}
        drawPath(fill,Brush.verticalGradient(listOf(line.copy(alpha=.28f),line.copy(alpha=0f)),chartTop,bottom))
        drawPath(path,line,style=Stroke(2.dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round))
    }
}

@Composable private fun WalletAction(label:String,icon:String,color:Color,modifier:Modifier,click:()->Unit) {
    Column(modifier.height(62.dp).clip(RoundedCornerShape(18.dp)).background(FFColors.surface)
        .clickable(role=Role.Button,onClick=click),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
        FFIcon(icon,color,22.dp)
        Spacer(Modifier.height(4.dp))
        Text(label,style=FFType.bodyMain.copy(fontSize=12.5.sp))
    }
}

@Composable private fun WalletMetric(label:String,value:String,modifier:Modifier){
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(FFColors.surface).padding(horizontal=16.dp,vertical=14.dp)){
        Text(label,style=FFType.bodyMain,color=FFColors.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        FitMoneyText(value,FFType.amountMd.copy(fontSize=18.sp))
    }
}
