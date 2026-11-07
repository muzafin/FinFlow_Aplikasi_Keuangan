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
import androidx.compose.ui.graphics.luminance
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
import com.logikawan.finflow.core.i18n.text
import com.logikawan.finflow.i18n.*
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

private val walletGroups = listOf("cash", "bank", "ewallet", "savings", "credit", "investment", "loan", "other")

private val pinnedWalletGroups = listOf("cash", "bank", "ewallet")

/** Label tampilan jenis dompet (id `cash`, `bank`, ... tetap pengenal). Jenis tak dikenal → "Lainnya". */
fun walletTypeLabel(type:String):String = when(type) {
    "cash" -> MR.strings.acc_type_cash.text()
    "bank" -> MR.strings.acc_type_bank_account.text()
    "ewallet" -> MR.strings.acc_type_ewallet.text()
    "savings" -> MR.strings.acc_type_savings.text()
    "credit" -> MR.strings.acc_type_credit.text()
    "investment" -> MR.strings.acc_type_investment.text()
    "loan" -> MR.strings.acc_type_loan.text()
    else -> MR.strings.acc_type_other.text()
}

// Ikon Hugeicons seperti Ollo (lingkaran netral).
private fun walletGroupIcon(type:String)=when(type) {
    "bank" -> "hi_bank"; "ewallet" -> "hi_ewallet"; "savings" -> "hi_piggy"; "credit" -> "hi_card"
    "investment" -> "hi_invest"; "loan" -> "hi_debt"; else -> "hi_wallet"
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
    // Pengaturan Dompet → Urutan Dompet Saya: dompet sesuai walletOrder (id tak tercantum tetap di belakang, urutan asli).
    val visible=data.accounts.filter{it.archived==archived}.orderedWallets(preferences)
    // Grup mengikuti walletGroupOrder (selalu berisi 8 grup yang valid); bawaan sama dengan urutan lama.
    val groupOrder=preferences.walletGroupOrder.filter{it in walletGroups}.let{if(it.toSet()==walletGroups.toSet())it else walletGroups}
    val currencies=(active.map{it.currency}+data.debts.filter{it.direction=="borrowed" && it.paidMinor<it.amountMinor}.map{it.currency})
        .distinct().ifEmpty{listOf(data.profile?.baseCurrency ?: "IDR")}.sortedWith(compareBy<String>{if(it==data.profile?.baseCurrency)0 else 1}.thenBy{it})
    val types=walletGroups.toSet()-"other"
    val roots=visible.filter{a->a.parentId==null || visible.none{it.id==a.parentId}}
    fun openAccount(account:Account)=(open?:edit)(account)
    @Composable fun WalletMenu() {
        Box {
            IconButton({ menu = true }, Modifier.size(36.dp).testTag("wallet-menu")) { FFIcon("more_vert", FFColors.onSurface, 22.dp, contentDescription = MR.strings.acc_wallet_menu.text()) }
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem(text = { Text(MR.strings.acc_add_wallet.text()) }, onClick = { menu = false; add() })
                DropdownMenuItem(text = { Text(if (archived) MR.strings.acc_back_to_active.text() else MR.strings.acc_archived_wallets.text()) }, onClick = { menu = false; archived = !archived })
                DropdownMenuItem(text = { Text(if (preferences.hideBalances) MR.strings.acc_show_balance.text() else MR.strings.acc_hide_balance.text()) }, enabled = preferenceState != null,
                    onClick = { menu = false; preferenceState?.update(preferences.copy(hideBalances = !preferences.hideBalances)) })
            }
        }
    }
    val firstGroup = groupOrder.firstOrNull { type -> type in pinnedWalletGroups || roots.any { if (type == "other") it.type !in types else it.type == type } }
    LazyColumn(Modifier.fillMaxSize().testTag("wallet-list"),contentPadding=PaddingValues(start=16.dp,end=16.dp,bottom=120.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        stickyHeader(key = "wallet-header") {
            Box(Modifier.fillMaxWidth().background(FFColors.background).padding(top = 6.dp)) {
                IconButton({ composition = true }, Modifier.align(Alignment.CenterStart).testTag("wallet-composition")) { FFIcon("hi_grid", FFColors.onSurface, 24.dp, contentDescription = MR.strings.acc_money_composition_cd.text()) }
                Text(MR.strings.acc_my_wallets.text(), Modifier.align(Alignment.Center), style = FFType.appTitle)
                IconButton(add, Modifier.align(Alignment.CenterEnd).testTag("wallet-add")) { FFIcon("hi_add", FFColors.onSurface, 26.dp, contentDescription = MR.strings.acc_add_wallet_cd.text()) }
            }
        }
        if (archived) item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) { WalletMenu() } }
        if (archived) item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(FFColors.surface).padding(start = 14.dp, end = 6.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(MR.strings.acc_showing_archived.text(), Modifier.weight(1f), style = FFType.metaData, color = FFColors.onSurfaceVariant)
                TextButton({ archived = false }) { Text(MR.strings.common_back.text(), style = FFType.metaData) }
            }
        }
        if (!archived) currencies.forEach { currency -> item(key = "summary-${currency}") {
            WalletBalanceCard(data, currency, preferences, preferenceState)
        } }
        if (!archived && !data.featuresReady) item {
            Text(data.featuresError ?: MR.strings.acc_loading_features.text(), style = FFType.metaData,
                color = if (data.featuresError != null) FFColors.expense else FFColors.outline)
        }
        if (!archived) {
            groupOrder.forEach { type ->
                val accounts = roots.filter { if (type == "other") it.type !in types else it.type == type }
                if (type in pinnedWalletGroups || accounts.isNotEmpty()) item(key = "group-${type}") {
                    val title = walletTypeLabel(type)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth().heightIn(min = 36.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(title, Modifier.weight(1f).padding(start = 4.dp), style = FFType.sectionTitle)
                            if (type == firstGroup) WalletMenu()
                        }
                        if (accounts.isEmpty()) WalletGroupEmpty(type, title)
                        else accounts.forEach { account ->
                            WalletAccountCard(account, data, preferences, onClick = { openAccount(account) })
                            visible.filter { it.parentId == account.id }.forEach { pocket ->
                                WalletAccountCard(pocket, data, preferences, MR.strings.acc_pocket_of.text(account.name), Modifier.padding(start = 24.dp), onClick = { openAccount(pocket) })
                            }
                        }
                    }
                }
            }
            val goals = data.goals.filter { !it.archived }
            if (goals.isNotEmpty()) item(key = "wallet-goals") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(MR.strings.acc_savings_goals.text(), style = FFType.sectionTitle)
                    goals.forEach { goal -> WalletGoalCard(goal, data, preferences) { account -> openAccount(account) } }
                }
            }
        } else {
            if (visible.isEmpty()) item {
                DesignCard { Text(MR.strings.acc_no_archived_wallets.text(), style = FFType.bodyMain, color = FFColors.onSurfaceVariant) }
            } else item(key = "archived-list") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(MR.strings.acc_archived_wallets.text(), style = FFType.sectionTitle)
                    visible.forEach { account -> WalletAccountCard(account, data, preferences, onClick = { openAccount(account) }) }
                }
            }
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
    val base=checkedAdd(values.total,-values.flow30)
    // Seperti Ollo: saldo 30 hari lalu 0 dan ada arus masuk = +100%.
    val percent=when { base!=0L -> values.flow30.toDouble()/abs(base)*100; values.flow30!=0L -> if(values.flow30>0)100.0 else -100.0; else -> 0.0 }
    val tenths=(abs(percent)*10).roundToInt()
    val percentLabel=(if(values.flow30<0)"-" else "+")+"${tenths/10},${tenths%10}%"
    val flowLabel=if(hidden)MR.strings.acc_flow_30_days.text("••••")
        else MR.strings.acc_flow_30_days.text("(${if(values.flow30>=0)"+" else "−"}${formatBalance(abs(values.flow30),currency,preferences)})")
    val white=Color.White
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).olloBalanceGradient(FFColors.isDark).testTag("wallet-balance-$currency")
        .padding(start=16.dp,end=16.dp,top=16.dp,bottom=16.dp), horizontalAlignment=Alignment.CenterHorizontally) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text(MR.strings.acc_total_balance_currency.text(currency),style=FFType.bodyMain,color=white.copy(alpha=.8f))
            Box(Modifier.size(32.dp).clip(CircleShape).clickable(enabled=preferenceState!=null){preferenceState?.update(preferences.copy(hideBalances=!hidden))},contentAlignment=Alignment.Center) {
                FFIcon(if(hidden)"hi_view_off" else "hi_view",white.copy(alpha=.8f),18.dp,if(hidden)MR.strings.acc_show_balance_cd.text() else MR.strings.acc_hide_balance_cd.text())
            }
        }
        Spacer(Modifier.height(4.dp))
        FitMoneyText(walletMoney(values.total,currency,preferences),FFType.amountXl,white,TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment=Alignment.CenterVertically) {
            val pill=RoundedCornerShape(20.dp)
            Row(Modifier.clip(pill).background(white.copy(alpha=.12f)).border(1.dp,white.copy(alpha=.3f),pill).padding(horizontal=13.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically) {
                FFIcon(if(values.flow30<0)"hi_arrow_down" else "hi_arrow_up",white,16.dp)
                Spacer(Modifier.width(6.dp))
                Text(percentLabel,style=FFType.bodyMain,fontWeight=FontWeight.SemiBold,color=white)
            }
            Spacer(Modifier.width(12.dp))
            Text(flowLabel,Modifier.weight(1f,false),style=FFType.metaData.copy(fontSize=13.sp),color=white.copy(alpha=.75f),maxLines=1,overflow=TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            WalletStatTile(MR.strings.acc_net_worth.text(),values.netWorth,currency,preferences,Modifier.weight(1f))
            WalletStatTile(MR.strings.acc_liabilities.text(),values.liabilities,currency,preferences,Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            WalletStatTile(MR.strings.acc_active_savings.text(),values.savings,currency,preferences,Modifier.weight(1f))
            WalletStatTile(MR.strings.acc_upcoming_payments.text(),values.upcoming,currency,preferences,Modifier.weight(1f))
        }
        if(values.negativeWallets>0){Spacer(Modifier.height(12.dp));Text(MR.strings.acc_negative_wallets.text(walletMoney(values.negativeWallets,currency,preferences)),Modifier.fillMaxWidth(),style=FFType.metaData,color=white,textAlign=TextAlign.Start)}
        if(values.liabilities>0 || values.negativeWallets>0){Spacer(Modifier.height(8.dp));Text(MR.strings.acc_net_worth_note.text(),Modifier.fillMaxWidth(),style=FFType.metaData,color=white.copy(alpha=.75f),textAlign=TextAlign.Start)}
    }
}

@Composable
private fun WalletStatTile(label:String,value:Long,currency:String,preferences:AppPreferences,modifier:Modifier) {
    val shape=RoundedCornerShape(14.dp)
    Column(modifier.clip(shape).background(Color.White.copy(alpha=.10f)).border(1.dp,Color.White.copy(alpha=.25f),shape).padding(horizontal=12.dp,vertical=10.dp)) {
        Text(label,style=FFType.metaData.copy(fontSize=10.sp),color=Color.White.copy(alpha=.75f),maxLines=1,overflow=TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        FitMoneyText(walletMoney(value,currency,preferences),FFType.amountMd.copy(fontSize=13.5.sp),Color.White)
    }
}

@Composable
private fun WalletGroupEmpty(type:String,title:String) {
    val dark=FFColors.background.luminance()<.5f
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(FFColors.surface).padding(horizontal=16.dp,vertical=20.dp),verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(if(dark)FFColors.deep else FFColors.surfaceContainer),contentAlignment=Alignment.Center){FFIcon(walletGroupIcon(type),FFColors.onSurfaceVariant.copy(alpha=.45f),22.dp)}
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(MR.strings.acc_group_empty.text(title),style=FFType.bodyMain,color=FFColors.onSurfaceVariant)
            Text(MR.strings.acc_group_empty_hint.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant.copy(alpha=.65f))
        }
    }
}

@Composable
private fun WalletAccountCard(account:Account,data:FinanceData,preferences:AppPreferences,pocket:String?=null,modifier:Modifier=Modifier,onClick:()->Unit) {
    val color=Color(account.color)
    val typeLine=pocket ?: "${walletTypeLabel(account.type).uppercase()} • ${account.currency}"
    val dark=FFColors.background.luminance()<.5f
    // Ollo: lingkaran ikon netral 52dp.
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(FFColors.surface)
        .clickable(role=Role.Button,onClick=onClick).padding(horizontal=16.dp,vertical=16.dp).testTag("wallet-${account.id}")) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(if(dark)FFColors.iconCircle else FFColors.surfaceContainerHigh),contentAlignment=Alignment.Center){
                AccountIconContent(account,if(account.icon.isBlank()||account.icon.startsWith("account_balance_wallet")) walletGroupIcon(account.type) else account.icon,FFColors.onSurface,20.dp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(account.name,style=FFType.bodyMain.copy(fontSize=16.sp,lineHeight=21.sp),maxLines=1,overflow=TextOverflow.Ellipsis)
                Text(typeLine,style=FFType.metaData,color=FFColors.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1.2f),horizontalAlignment=Alignment.End) {
                Text(MR.strings.acc_current_balance.text(),style=FFType.metaData.copy(fontSize=10.sp),color=FFColors.onSurfaceVariant)
                FitMoneyText(walletMoney(data.balance(account),account.currency,preferences),FFType.amountMd.copy(fontSize=16.sp),FFColors.onSurface,TextAlign.End)
            }
        }
        if(account.excludedFromTotal){Spacer(Modifier.height(10.dp));Text(MR.strings.acc_excluded_badge.text(),style=FFType.metaData,color=FFColors.outline)}
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
                Text(MR.strings.acc_goal_badge.text(currency),style=FFType.badgeLabel.copy(fontSize=10.sp,letterSpacing=.8.sp),color=FFColors.outline,maxLines=1)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.Bottom) {
            Column {
                Text(MR.strings.acc_current_balance.text(),style=FFType.badgeLabel.copy(fontSize=10.sp),color=FFColors.outline)
                FitMoneyText(formatBalance(saved,currency,preferences),FFType.amountMd)
            }
            Text(MR.strings.acc_goal_target_amount.text(formatBalance(target,currency,preferences)),style=FFType.metaData,color=FFColors.outline)
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
    val types=walletGroups.toSet()-"other"
    val total=included.fold(0L){sum,a->checkedAdd(sum,data.balance(a).coerceAtLeast(0))}
    val rows=walletGroups.mapNotNull { type ->
        if(type=="credit" || type=="loan") null
        else {
            val value=included.filter{if(type=="other")it.type !in types else it.type==type}.fold(0L){sum,a->checkedAdd(sum,data.balance(a).coerceAtLeast(0))}
            if(value==0L)null else walletTypeLabel(type) to value
        }
    }
    AlertDialog(onDismissRequest=dismiss,title={Text(MR.strings.acc_money_composition.text())},text={
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
            if(rows.isEmpty())Text(MR.strings.acc_no_wallets_in_currency.text(),style=FFType.metaData,color=FFColors.outline)
        }
    },confirmButton={TextButton(onClick=dismiss){Text(MR.strings.common_close.text())}})
}

private fun walletMoney(value:Long,currency:String,preferences:AppPreferences)=if(preferences.hideBalances)maskedBalance(value,currency,preferences) else formatBalance(value,currency,preferences)
