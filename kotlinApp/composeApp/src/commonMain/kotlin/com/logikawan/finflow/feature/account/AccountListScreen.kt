package com.logikawan.finflow.feature.account

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logikawan.finflow.core.model.*
import com.logikawan.finflow.core.ui.*
import com.logikawan.finflow.core.util.*
import kotlinx.datetime.*
import kotlin.math.abs
import kotlin.math.roundToInt

internal data class WalletSummaryValues(val total:Long,val flow30:Long,val netWorth:Long,val liabilities:Long,val savings:Long,val upcoming:Long,val negativeWallets:Long)

internal fun walletSummaryValues(data:FinanceData,currency:String,date:LocalDate=today()):WalletSummaryValues {
    val included=data.includedAccounts().filter{it.currency==currency}
    val ids=included.map{it.id}.toSet()
    val total=included.fold(0L){sum,a->checkedAdd(sum,data.balance(a))}
    val first=date.minus(DatePeriod(days=29)).toString()
    val flow=data.transactions.filter{!it.scheduled && it.date>=first && it.date<=date.toString()}.fold(0L){sum,t->
        included.fold(sum){subtotal,a->checkedAdd(subtotal,accountDelta(t,a.id))}
    }
    val excluded=data.accounts.filter{it.excludedFromTotal}.map{it.id}.toSet()
    val debtTotal=data.debts.filter{it.direction=="borrowed" && it.currency==currency && it.accountId !in excluded}
        .fold(0L){sum,d->checkedAdd(sum,(d.amountMinor-d.paidMinor).coerceAtLeast(0))}
    val negativeWallets=included.filter{it.type in listOf("credit","loan")}.fold(0L){sum,a->checkedAdd(sum,-data.balance(a).coerceAtMost(0))}
    val savings=data.goals.filter{!it.archived && it.currency==currency && it.holdingAccountId in ids}
        .fold(0L){sum,g->checkedAdd(sum,(g.savedMinor-g.legacySavedMinor).coerceAtLeast(0))}
    val last=date.plus(DatePeriod(days=30)).toString()
    val bills=data.bills.filter{!it.archived && it.paidAt==0L && it.currency==currency && it.accountId in ids && it.dueDate>=date.toString() && it.dueDate<=last}
        .fold(0L){sum,b->checkedAdd(sum,b.amountMinor)}
    val scheduled=data.transactions.filter{it.scheduled && it.type=="expense" && it.currency==currency && it.accountId in ids && it.date>=date.toString() && it.date<=last}
        .fold(0L){sum,t->checkedAdd(sum,t.amountMinor)}
    return WalletSummaryValues(total,flow,checkedAdd(total,-debtTotal),debtTotal,savings,checkedAdd(bills,scheduled),negativeWallets)
}

private val walletGroups = listOf(
    "cash" to "Tunai", "bank" to "Akun Bank", "ewallet" to "E-Wallet", "savings" to "Tabungan",
    "credit" to "Kartu Kredit", "investment" to "Investasi", "loan" to "Pinjaman", "other" to "Lainnya")

private val pinnedWalletGroups = listOf("cash", "bank", "ewallet")

private fun walletTypeTitle(type:String)=walletGroups.find{it.first==type}?.second ?: "Lainnya"

private fun walletGroupIcon(type:String)=when(type) {
    "bank" -> "account_balance_rounded"; "savings" -> "savings_rounded"; "credit" -> "credit_card_rounded"
    "investment" -> "trending_up_rounded"; "loan" -> "payments_rounded"; else -> "account_balance_wallet_rounded"
}

@Composable
fun WalletScreen(data:FinanceData,edit:(Account)->Unit,add:()->Unit,open:((Account)->Unit)?=null) {
    val preferences=LocalPreferences.current
    val preferenceState=LocalPreferenceState.current
    var archived by rememberSaveable{mutableStateOf(false)}
    var menu by remember{mutableStateOf(false)}
    var composition by remember{mutableStateOf(false)}
    var compositionCode by rememberSaveable{mutableStateOf("")}
    val active=data.accounts.filterNot{it.archived}
    val visible=data.accounts.filter{it.archived==archived}
    val currencies=(active.map{it.currency}+data.debts.filter{it.direction=="borrowed" && it.paidMinor<it.amountMinor}.map{it.currency})
        .distinct().ifEmpty{listOf(data.profile?.baseCurrency ?: "IDR")}.sortedWith(compareBy<String>{if(it==data.profile?.baseCurrency)0 else 1}.thenBy{it})
    val types=walletGroups.map{it.first}.toSet()-"other"
    val roots=visible.filter{a->a.parentId==null || visible.none{it.id==a.parentId}}
    fun openAccount(account:Account)=(open?:edit)(account)
    LazyColumn(Modifier.fillMaxSize().testTag("wallet-list"),contentPadding=PaddingValues(start=20.dp,end=20.dp,top=12.dp,bottom=120.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        item {
            Box(Modifier.fillMaxWidth()) {
                Row(Modifier.align(Alignment.CenterStart)) {
                    IconButton({ composition = true }, Modifier.testTag("wallet-composition")) { FFIcon("pie_chart_outline_rounded", contentDescription = "Komposisi uang") }
                }
                Text("Dompet Saya", Modifier.fillMaxWidth().align(Alignment.Center), style = FFType.appTitle, textAlign = TextAlign.Center)
                Row(Modifier.align(Alignment.CenterEnd)) {
                    Box {
                        IconButton({ menu = true }, Modifier.testTag("wallet-menu")) { FFIcon("more_vert", contentDescription = "Menu dompet") }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem(text = { Text("Tambah Dompet") }, onClick = { menu = false; add() })
                            DropdownMenuItem(text = { Text(if (archived) "Kembali ke Dompet Aktif" else "Dompet Diarsipkan") }, onClick = { menu = false; archived = !archived })
                            DropdownMenuItem(text = { Text(if (preferences.hideBalances) "Tampilkan Saldo" else "Sembunyikan Saldo") }, enabled = preferenceState != null,
                                onClick = { menu = false; preferenceState?.update(preferences.copy(hideBalances = !preferences.hideBalances)) })
                        }
                    }
                }
            }
        }
        if (archived) item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(FFColors.surface).padding(start = 14.dp, end = 6.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("Menampilkan dompet diarsipkan", Modifier.weight(1f), style = FFType.metaData, color = FFColors.onSurfaceVariant)
                TextButton({ archived = false }) { Text("Kembali", style = FFType.metaData) }
            }
        }
        if (!archived) currencies.forEach { currency -> item(key = "summary-${currency}") {
            WalletBalanceCard(data, currency, preferences, preferenceState)
        } }
        if (!archived && !data.featuresReady) item {
            Text(data.featuresError ?: "Memuat rincian pinjaman, tabungan, dan pembayaran…", style = FFType.metaData,
                color = if (data.featuresError != null) FFColors.expense else FFColors.outline)
        }
        if (!archived) {
            walletGroups.forEach { (type, title) ->
                val accounts = roots.filter { if (type == "other") it.type !in types else it.type == type }
                if (type in pinnedWalletGroups || accounts.isNotEmpty()) item(key = "group-${type}") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(title, style = FFType.sectionTitle)
                        if (accounts.isEmpty()) WalletGroupEmpty(type, title)
                        else accounts.forEach { account ->
                            WalletAccountCard(account, data, preferences, onClick = { openAccount(account) })
                            visible.filter { it.parentId == account.id }.forEach { pocket ->
                                WalletAccountCard(pocket, data, preferences, "Pocket · ${account.name}", Modifier.padding(start = 24.dp), onClick = { openAccount(pocket) })
                            }
                        }
                    }
                }
            }
            val goals = data.goals.filter { !it.archived }
            if (goals.isNotEmpty()) item(key = "wallet-goals") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Target Tabungan", style = FFType.sectionTitle)
                    goals.forEach { goal -> WalletGoalCard(goal, data, preferences) { account -> openAccount(account) } }
                }
            }
        } else {
            if (visible.isEmpty()) item {
                DesignCard { Text("Belum ada dompet diarsipkan.", style = FFType.bodyMain, color = FFColors.onSurfaceVariant) }
            } else item(key = "archived-list") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Dompet Diarsipkan", style = FFType.sectionTitle)
                    visible.forEach { account -> WalletAccountCard(account, data, preferences, onClick = { openAccount(account) }) }
                }
            }
        }
        if (!archived) item {
            OutlinedButton(onClick = add, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(24.dp)) { Text("+ Tambah Dompet") }
        }
    }
    if (composition) {
        val code = compositionCode.takeIf { it in currencies } ?: currencies.first()
        MoneyCompositionDialog(currencies, code, { compositionCode = it }, data, preferences) { composition = false }
    }
}

@Composable
private fun WalletBalanceCard(data:FinanceData,currency:String,preferences:AppPreferences,preferenceState:PreferenceState?) {
    val values=walletSummaryValues(data,currency)
    val hidden=preferences.hideBalances
    val changeColor=when{values.flow30>0 -> FFColors.income; values.flow30<0 -> FFColors.expense; else -> FFColors.onSurfaceVariant}
    val base=checkedAdd(values.total,-values.flow30)
    val percent=if(base!=0L) values.flow30.toDouble()/base*100 else 0.0
    val tenths=(abs(percent)*10).roundToInt()
    val percentLabel=(if(values.flow30>0)"+" else if(values.flow30<0)"-" else "+")+"${tenths/10}.${tenths%10}%"
    val flowLabel=if(hidden)"•••• 30 hari terakhir"
        else "(${if(values.flow30>=0)"+" else "−"}${formatBalance(abs(values.flow30),currency,preferences)}) 30 hari terakhir"
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(FFColors.surface).padding(20.dp), horizontalAlignment=Alignment.CenterHorizontally) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text("Total Saldo (${currency})",style=FFType.metaData,color=FFColors.onSurfaceVariant)
            IconButton({preferenceState?.update(preferences.copy(hideBalances=!hidden))},enabled=preferenceState!=null,modifier=Modifier.size(40.dp)) {
                FFIcon(if(hidden)"visibility_off" else "visibility",FFColors.onSurfaceVariant,18.dp,if(hidden)"Tampilkan saldo" else "Sembunyikan saldo")
            }
        }
        FitMoneyText(walletMoney(values.total,currency,preferences),FFType.amountLg)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Row(Modifier.clip(CircleShape).background(changeColor.copy(alpha=.12f)).padding(horizontal=10.dp,vertical=5.dp),verticalAlignment=Alignment.CenterVertically) {
                FFIcon(if(values.flow30<0)"arrow_downward_rounded" else "arrow_upward_rounded",changeColor,14.dp)
                Spacer(Modifier.width(3.dp))
                Text(percentLabel,style=FFType.badgeLabel,fontWeight=FontWeight.SemiBold,color=changeColor)
            }
            Text(flowLabel,style=FFType.metaData,color=changeColor)
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            WalletStatTile("Total Kekayaan",values.netWorth,currency,preferences,Modifier.weight(1f))
            WalletStatTile("Liabilitas",values.liabilities,currency,preferences,Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            WalletStatTile("Tabungan Aktif",values.savings,currency,preferences,Modifier.weight(1f))
            WalletStatTile("Pembayaran Mendatang",values.upcoming,currency,preferences,Modifier.weight(1f))
        }
        if(values.negativeWallets>0){Spacer(Modifier.height(12.dp));Text("Saldo minus dompet: ${walletMoney(values.negativeWallets,currency,preferences)}",Modifier.fillMaxWidth(),style=FFType.metaData,color=FFColors.expense,textAlign=TextAlign.Start)}
        if(values.liabilities>0 || values.negativeWallets>0){Spacer(Modifier.height(8.dp));Text("Kekayaan bersih = total saldo − sisa utang tercatat. Saldo kredit / pinjaman negatif sudah masuk ke total saldo dan tidak dikurangi lagi.",Modifier.fillMaxWidth(),style=FFType.metaData,color=FFColors.outline,textAlign=TextAlign.Start)}
    }
}

@Composable
private fun WalletStatTile(label:String,value:Long,currency:String,preferences:AppPreferences,modifier:Modifier) {
    val shape=RoundedCornerShape(18.dp)
    Column(modifier.clip(shape).background(FFColors.canvas).border(1.dp,FFColors.outlineVariant.copy(alpha=.6f),shape).padding(14.dp)) {
        Text(label,style=FFType.badgeLabel,color=FFColors.outline,maxLines=1,overflow=TextOverflow.Ellipsis)
        Spacer(Modifier.height(6.dp))
        FitMoneyText(walletMoney(value,currency,preferences),FFType.amountMd)
    }
}

@Composable
private fun WalletGroupEmpty(type:String,title:String) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(FFColors.surface).padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(15.dp)).background(FFColors.canvas),contentAlignment=Alignment.Center){FFIcon(walletGroupIcon(type),FFColors.outline,22.dp)}
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Belum ada dompet ${title}",style=FFType.bodyMain,color=FFColors.onSurfaceVariant)
            Text("Ketuk tombol + untuk menambahkan",style=FFType.metaData,color=FFColors.outline)
        }
    }
}

@Composable
private fun WalletAccountCard(account:Account,data:FinanceData,preferences:AppPreferences,pocket:String?=null,modifier:Modifier=Modifier,onClick:()->Unit) {
    val color=Color(account.color)
    val typeLine=pocket ?: "${walletTypeTitle(account.type).uppercase()} • ${account.currency}"
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(FFColors.surface)
        .clickable(role=Role.Button,onClick=onClick).padding(18.dp).testTag("wallet-${account.id}")) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(15.dp)).background(color.copy(alpha=.1f)),contentAlignment=Alignment.Center){FFIcon(account.icon,color,22.dp)}
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(account.name,style=FFType.bodyMain,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
                Text(typeLine,style=FFType.badgeLabel.copy(fontSize=10.sp,letterSpacing=.8.sp),color=FFColors.outline,maxLines=1,overflow=TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f),horizontalAlignment=Alignment.End) {
                Text("Saldo Saat Ini",style=FFType.badgeLabel.copy(fontSize=10.sp),color=FFColors.outline)
                FitMoneyText(walletMoney(data.balance(account),account.currency,preferences),FFType.amountMd)
            }
        }
        if(account.excludedFromTotal){Spacer(Modifier.height(10.dp));Text("Dikecualikan dari total dan statistik",style=FFType.metaData,color=FFColors.outline)}
    }
}

@Composable
private fun WalletGoalCard(goal:Goal,data:FinanceData,preferences:AppPreferences,onOpen:(Account)->Unit) {
    val currency=goal.currency
    val saved=(goal.savedMinor-goal.legacySavedMinor).coerceAtLeast(0)
    val target=goal.targetMinor.coerceAtLeast(0)
    val account=data.accounts.find{it.id==goal.holdingAccountId}
    val color=account?.let{Color(it.color)} ?: FFColors.primary
    DesignCard(Modifier.testTag("wallet-goal-${goal.id}"), onClick=account?.let{acc->{ onOpen(acc) }}) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(15.dp)).background(color.copy(alpha=.1f)),contentAlignment=Alignment.Center){FFIcon("savings_rounded",color,22.dp)}
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(goal.name,style=FFType.bodyMain,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
                Text("TARGET TABUNGAN • ${currency}",style=FFType.badgeLabel.copy(fontSize=10.sp,letterSpacing=.8.sp),color=FFColors.outline,maxLines=1)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.Bottom) {
            Column {
                Text("Saldo Saat Ini",style=FFType.badgeLabel.copy(fontSize=10.sp),color=FFColors.outline)
                FitMoneyText(formatBalance(saved,currency,preferences),FFType.amountMd)
            }
            Text("Target ${formatBalance(target,currency,preferences)}",style=FFType.metaData,color=FFColors.outline)
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(progress={if(target>0)(saved.toFloat()/target).coerceIn(0f,1f) else 0f},
            modifier=Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),color=color,trackColor=FFColors.outlineVariant)
    }
}

@Composable
private fun MoneyCompositionDialog(currencies:List<String>,currency:String,selectCurrency:(String)->Unit,data:FinanceData,preferences:AppPreferences,dismiss:()->Unit) {
    // Kartu kredit/pinjaman adalah liabilitas, bukan komposisi uang tunai; saldo negatif lain dihitung 0.
    val included=data.includedAccounts().filter{it.currency==currency && it.type !in setOf("credit","loan")}
    val types=walletGroups.map{it.first}.toSet()-"other"
    val total=included.fold(0L){sum,a->checkedAdd(sum,data.balance(a).coerceAtLeast(0))}
    val rows=walletGroups.mapNotNull { (type,title) ->
        if(title=="Kartu Kredit" || title=="Pinjaman") null
        else {
            val value=included.filter{if(type=="other")it.type !in types else it.type==type}.fold(0L){sum,a->checkedAdd(sum,data.balance(a).coerceAtLeast(0))}
            if(value==0L)null else title to value
        }
    }
    AlertDialog(onDismissRequest=dismiss,title={Text("Komposisi Uang")},text={
        Column(Modifier.testTag("wallet-composition-dialog").verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            if(currencies.size>1)Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){currencies.forEach{code->DesignPill(code,code==currency,{selectCurrency(code)})}}
            Text(formatBalance(total,currency,preferences),style=FFType.amountMd)
            rows.forEach { (title,value) ->
                Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        Text(title,style=FFType.metaData,fontWeight=FontWeight.SemiBold)
                        Text(if(total>0)"${(value.toDouble()/total*100).roundToInt()}%" else "0%",style=FFType.metaData,color=FFColors.outline)
                    }
                    LinearProgressIndicator(progress={if(total>0)(value.toFloat()/total).coerceIn(0f,1f) else 0f},
                        modifier=Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),color=FFColors.primary,trackColor=FFColors.outlineVariant)
                    Text(formatBalance(value,currency,preferences),style=FFType.badgeLabel,color=FFColors.outline)
                }
            }
            if(rows.isEmpty())Text("Belum ada dompet pada mata uang ini.",style=FFType.metaData,color=FFColors.outline)
        }
    },confirmButton={TextButton(onClick=dismiss){Text("Tutup")}})
}

private fun walletMoney(value:Long,currency:String,preferences:AppPreferences)=if(preferences.hideBalances)"••••" else formatBalance(value,currency,preferences)
