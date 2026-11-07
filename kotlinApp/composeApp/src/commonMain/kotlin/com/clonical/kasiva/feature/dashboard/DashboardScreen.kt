package com.clonical.kasiva.feature.dashboard

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.clonical.kasiva.core.i18n.menuLabel
import com.clonical.kasiva.core.util.orderedWallets
import com.clonical.kasiva.core.i18n.tabLabel
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.i18n.transactionTypeLabel
import com.clonical.kasiva.i18n.*
import com.clonical.kasiva.core.model.*
import com.clonical.kasiva.core.ui.*
import com.clonical.kasiva.core.util.*
import com.clonical.kasiva.feature.settings.recordingProgress
import kotlinx.datetime.*
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val menuTitles=listOf("Anggaran","Berulang","Target","Tagihan","Utang","Keinginan","Kartu","Catatan","Reimburse","Investasi")
private val menuIcons=listOf("hi_percent","hi_recurring","hi_piggy","hi_bill","hi_debt","hi_wish","hi_card","hi_note","hi_reimburse","hi_invest")
// Warna pastel tile menu mengikuti Ollo; urutan sejajar financeMenus.
private val menuPastel=listOf(
    Pair(0xFFF8F1E5, 0xFFBF7200), Pair(0xFFE8F3FC, 0xFF1E8ADF), Pair(0xFFEBF5EC, 0xFF439946), Pair(0xFFFDECEB, 0xFFF44336),
    Pair(0xFFF3EDFD, 0xFF7E57C2), Pair(0xFFFDF0F5, 0xFFE91E63), Pair(0xFFE6F7F5, 0xFF00897B), Pair(0xFFFFF6E0, 0xFFF59E0B),
    Pair(0xFFEDF0FD, 0xFF5C6BC0), Pair(0xFFE7F4FB, 0xFF0288D1),
)
// Mode gelap Ollo: ikon warna Material 500, latar = warna sama 18% di atas hitam (diukur: #2E1B00, #061B2C, #0E200E, #2C0C0A).
private val menuDark=listOf(0xFFFF9800,0xFF2196F3,0xFF4CAF50,0xFFF44336,0xFF9C27B0,0xFFE91E63,0xFF5C6BC0,0xFF009688,0xFFFF9800,0xFF536DFE)
@Composable private fun isDarkUi()=FFColors.background.luminance()<.5f
internal val dashboardShortcuts=listOf(
    Triple("Notifikasi","Notifikasi","notifications_outlined"),
    Triple("Catatan Pembaruan","Catatan Pembaruan","history"),
    Triple("Tambah Transaksi","Tampilan Tambah Transaksi","add_rounded"),
    Triple("Tampilan Kartu","Tampilan Kartu","credit_card_outlined"),
    Triple("Palet Transaksi","Palet Transaksi","palette_outlined"),
    Triple("Tampilan Beranda","Tampilan Beranda","home_outlined"),
    Triple("Format Saldo","Format Saldo","payments"),
    Triple("Kategori","Kategori","category"),
    Triple("Periode Pencatatan","Periode Pencatatan","calendar_today_outlined"),
)
private const val shortcutBadge="Catatan Pembaruan"

/** Label tampilan pintasan Beranda; label di [dashboardShortcuts] tetap menjadi pengenal. */
fun shortcutLabel(label:String):String = when(label){
    "Notifikasi"->MR.strings.dash_shortcut_notifications.text()
    "Catatan Pembaruan"->MR.strings.dash_shortcut_changelog.text()
    "Tambah Transaksi"->MR.strings.dash_shortcut_add_transaction.text()
    "Tampilan Kartu"->MR.strings.dash_shortcut_card_view.text()
    "Palet Transaksi"->MR.strings.dash_shortcut_palette.text()
    "Tampilan Beranda"->MR.strings.dash_shortcut_home_view.text()
    "Format Saldo"->MR.strings.dash_shortcut_balance_format.text()
    "Kategori"->MR.strings.dash_shortcut_category.text()
    "Periode Pencatatan"->MR.strings.dash_shortcut_recording_period.text()
    else->label
}

fun dashboardBalance(data:FinanceData,currency:String,liquid:Boolean):Long = data.includedAccounts()
    .filter{it.currency==currency && (!liquid || it.type in listOf("cash","bank","ewallet"))}
    .fold(0L){sum,a->checkedAdd(sum,data.balance(a))}

// Beranda gaya Ollo: header ringkas, panel pintasan melayang, kartu saldo gradien, menu pager 4 tile,
// anggaran, dompet grid 2 kolom, dan transaksi terbaru.
@Composable
fun Dashboard(data:FinanceData,month:String,monthChange:(String)->Unit,edit:(Transaction)->Unit,name:String,onNavigate:(String)->Unit) {
    val preferences=LocalPreferences.current
    var privacy by rememberSaveable(preferences.hideBalances){mutableStateOf(preferences.hideBalances)}
    var period by rememberSaveable(preferences.homePeriod){mutableStateOf(preferences.homePeriod)}
    var menuPage by rememberSaveable{mutableIntStateOf(0)}
    var shortcuts by remember{mutableStateOf(false)}
    var liquid by rememberSaveable{mutableStateOf(false)}
    var selectedDate by rememberSaveable{mutableStateOf(today().toString())}
    var currencyOffset by rememberSaveable{mutableIntStateOf(0)}
    val now=today()
    val range=financePeriod(period,now,month,preferences.recordingDay,preferences.weekStart)
    val transactions=data.reportTransactions().filter{if(period=="Kalender")it.date==selectedDate else it.date in range}.sortedWith(compareByDescending<Transaction>{it.date}.thenByDescending{it.createdAt})
    val active=data.accounts.filterNot{it.archived}
    val included=data.includedAccounts()
    val currencies=(included.map{it.currency}+transactions.map{it.currency}).distinct().ifEmpty{listOf(data.profile?.baseCurrency ?: "IDR")}.sortedBy{if(it==data.profile?.baseCurrency)0 else 1}
    val orderedCurrencies=if(currencies.isEmpty())currencies else List(currencies.size){currencies[(it+currencyOffset)%currencies.size]}
    val motion=rememberInfiniteTransition(label="balance-neon")
    val angle by motion.animateFloat(0f,(2*kotlin.math.PI).toFloat(),infiniteRepeatable(tween(14000,easing=LinearEasing)),label="balance-gradient-angle")
    fun amount(value:Long,currency:String)=if(privacy)"••••••" else formatBalance(value,currency,preferences)
    // Saldo dompet di "Dompet Saya" memakai masker dari Format Saldo (karakter & format) saat disembunyikan.
    fun walletAmount(value:Long,currency:String)=if(privacy)maskedBalance(value,currency,preferences) else formatBalance(value,currency,preferences)
    // Pengaturan Dompet → Dompet di Beranda: daftar pilihan (urut sesuai pilihan) lalu batas jumlah (0 = semua).
    val dashboardWallets=(if(preferences.dashboardWalletIds.isEmpty())active.orderedWallets(preferences)
        else preferences.dashboardWalletIds.mapNotNull{id->active.find{it.id==id}}.ifEmpty{active.orderedWallets(preferences)})
        .let{if(preferences.dashboardWalletCount>0)it.take(preferences.dashboardWalletCount) else it}
    val visibleMenus=preferences.menuOrder.filter{it !in preferences.hiddenMenus}
    val menuPages=visibleMenus.chunked(4)
    val level=recordingProgress(data).level
    fun cycleCurrency(){ if(currencies.size>1) currencyOffset+=1 }

    Column(Modifier.fillMaxSize()) {
    Box(Modifier.fillMaxWidth().padding(start=16.dp,end=8.dp,top=12.dp,bottom=12.dp)) {
        DashboardHeader(name,level,onProfile={onNavigate("Profil")},onLevel={onNavigate("Aktivitas")},shortcuts=shortcuts,setShortcuts={shortcuts=it},onNavigate=onNavigate)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(FFColors.surface))
    LazyColumn(Modifier.fillMaxSize().testTag("dashboard"),contentPadding=PaddingValues(start=16.dp,end=16.dp,top=8.dp,bottom=124.dp)) {
        item(key="period") {
            Column(Modifier.fillMaxWidth()) {
                DashboardPeriodTabs(period){period=it}
                if(period=="Kalender"){Spacer(Modifier.height(8.dp));TransactionCalendar(month,monthChange,selectedDate,{selectedDate=it},data.reportTransactions(),privacy)}
            }
        }
        if(period!="Kalender") {
            item(key="balance") {
                Column(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(8.dp))
                    orderedCurrencies.forEachIndexed{index,currency->
                        if(index>0)Spacer(Modifier.height(16.dp))
                        val balance=dashboardBalance(data,currency,liquid)
                        val income=transactions.filter{it.currency==currency && it.type=="income"}.fold(0L){sum,t->checkedAdd(sum,t.amountMinor)}
                        val expense=transactions.filter{it.currency==currency && it.type=="expense"}.fold(0L){sum,t->checkedAdd(sum,t.amountMinor)}
                        BalanceCard(currency=currency,liquid=liquid,balance=amount(balance,currency),income=amount(income,currency),expense=amount(expense,currency),
                            gradient=cardGradientColors(preferences.cardGradient),angle=angle,showCurrencySwap=currencies.size>1,privacy=privacy,
                            toggleLiquid={liquid=!liquid},cycleCurrency={cycleCurrency()},togglePrivacy={privacy=!privacy},
                            onIncome={onNavigate("Transaksi:income")},onExpense={onNavigate("Transaksi:expense")})
                    }
                }
            }
            item(key="menu") {
                Column(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(24.dp))
                    MenuSection(menus=visibleMenus,page=menuPage.takeIf{it<menuPages.size} ?: 0,onMore={if(menuPages.size>1)menuPage=(menuPage+1)%menuPages.size},onNavigate=onNavigate,menuTheme=preferences.menuTheme,appearance=preferences.menuAppearance)
                }
            }
            item(key="budget") {
                Column(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(16.dp))
                    MonthlyBudgetCard(data=data,month=month,preferences=preferences,amount=::amount,navigate=onNavigate)
                }
            }
            item(key="wallets") {
                Column(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(24.dp))
                    WalletsSection(accounts=dashboardWallets,data=data,amount=::walletAmount,navigate=onNavigate)
                }
            }
        }
        item(key="transactions") {
            Column(Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(24.dp))
                RecentTransactionsSection(transactions=transactions,sizeLimit=if(period=="Kalender")Int.MAX_VALUE else 6,calendarMode=period=="Kalender",data=data,amount=::amount,edit=edit,navigate=onNavigate)
            }
        }
    }
    }
}

@Composable
private fun DashboardHeader(name:String,level:Int,onProfile:()->Unit,onLevel:()->Unit,shortcuts:Boolean,setShortcuts:(Boolean)->Unit,onNavigate:(String)->Unit) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        GreetingText(name,Modifier.weight(1f),onProfile)
        Spacer(Modifier.width(8.dp))
        Surface(onClick=onLevel,color=FFColors.surfaceContainer,shape=RoundedCornerShape(50),
            modifier=Modifier.height(26.dp).widthIn(min=44.dp).testTag("dashboard-level-pill")) {
            Box(Modifier.fillMaxHeight().padding(horizontal=10.dp),contentAlignment=Alignment.Center) {
                Text(MR.strings.dash_level.text(level),style=FFType.bodyMain.copy(fontSize=12.sp,fontWeight=FontWeight.Bold,lineHeight=16.sp),color=FFColors.onSurface,maxLines=1)
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.size(40.dp).clip(CircleShape).background(if(FFColors.isDark)FFColors.surface else Color.Transparent).clickable(role=Role.Button,onClick=onProfile).testTag("dashboard-profile"),contentAlignment=Alignment.Center){
            FFIcon("hi_user",FFColors.onSurface,22.dp,contentDescription=tabLabel("Profil"))
        }
        Box(Modifier.testTag("dashboard-shortcuts")) {
            Box(Modifier.size(40.dp).clip(CircleShape).clickable(role=Role.Button){setShortcuts(true)}.testTag("dashboard-shortcuts-button"),contentAlignment=Alignment.Center) {
                FFIcon("more_vert",FFColors.onSurface,24.dp,contentDescription=MR.strings.dash_menu.text())
                Box(Modifier.align(Alignment.TopEnd).padding(top=7.dp,end=9.dp).size(6.dp).clip(CircleShape).background(Color(0xFFF44336)))
            }
            if(shortcuts)ShortcutPanel(onDismiss={setShortcuts(false)},onNavigate={setShortcuts(false);onNavigate(it)})
        }
    }
}

// Sapaan tidak boleh terpotong: mengecil sampai maksimal dua baris tanpa elipsis.
@Composable
private fun GreetingText(name:String,modifier:Modifier,onClick:()->Unit) {
    val text=MR.strings.dash_greeting.text(name)
    val base=FFType.appTitle.copy(fontSize=20.sp,fontWeight=FontWeight.SemiBold)
    val measurer=rememberTextMeasurer()
    val density=LocalDensity.current
    BoxWithConstraints(modifier) {
        val width=with(density){maxWidth.toPx()}
        val limit=Constraints(maxWidth=width.toInt().coerceAtLeast(1))
        fun fits(size:Float,lines:Int):Boolean{
            val layout=measurer.measure(text,style=base.copy(fontSize=size.sp,lineHeight=(size*1.3f).sp),maxLines=lines,constraints=limit)
            return !layout.hasVisualOverflow && layout.lineCount<=lines
        }
        // Seperti Ollo satu baris; nama panjang mengecil dulu, lalu dua baris agar tidak pernah terpotong.
        var lines=1
        var size=base.fontSize.value
        while(size>15f && !fits(size,1)) size-=0.5f
        if(!fits(size,1)){ lines=2; size=base.fontSize.value; while(size>13f && !fits(size,2)) size-=0.5f }
        val style=base.copy(fontSize=size.sp,lineHeight=(size*1.3f).sp)
        Text(text,Modifier.clickable(role=Role.Button,onClick=onClick),style=style,maxLines=lines,overflow=TextOverflow.Clip)
    }
}

@Composable
private fun ShortcutPanel(onDismiss:()->Unit,onNavigate:(String)->Unit) {
    val density=LocalDensity.current
    Popup(alignment=Alignment.TopEnd,
        offset=IntOffset(with(density){8.dp.roundToPx()},with(density){44.dp.roundToPx()}),
        onDismissRequest=onDismiss,
        properties=PopupProperties(focusable=true)) {
        Column(Modifier.width(306.dp).testTag("dashboard-shortcuts-panel").shadow(elevation=18.dp,shape=RoundedCornerShape(24.dp),clip=false)
            .clip(RoundedCornerShape(24.dp)).background(FFColors.surface).padding(vertical=8.dp)) {
            dashboardShortcuts.forEach{(label,destination,icon)->
                Row(Modifier.fillMaxWidth().height(48.dp).clickable(role=Role.Button){onNavigate(destination)}.padding(horizontal=12.dp),
                    verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(FFColors.surfaceContainerHigh),contentAlignment=Alignment.Center){
                        FFIcon(icon,FFColors.onSurface,18.dp)
                    }
                    Text(if(label==shortcutBadge)MR.strings.dash_shortcut_updates.text() else shortcutLabel(label),Modifier.weight(1f),style=FFType.bodyMain.copy(fontSize=18.sp,lineHeight=24.sp),
                        color=FFColors.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
                    if(label==shortcutBadge)Surface(shape=RoundedCornerShape(50),color=FFColors.onSurface){
                        Text(MR.strings.dash_new.text(),Modifier.padding(horizontal=8.dp,vertical=2.dp),style=FFType.metaData.copy(fontSize=12.sp,fontWeight=FontWeight.SemiBold),color=FFColors.canvas)
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardPeriodTabs(period:String,select:(String)->Unit) {
    val dark=isDarkUi()
    Row(Modifier.fillMaxWidth().height(40.dp)) {
        listOf("Hari Ini" to MR.strings.dash_period_day.text(),"Minggu Ini" to MR.strings.dash_period_week.text(),"Bulan Ini" to MR.strings.dash_period_month.text(),
            "Tahun" to MR.strings.dash_period_year.text(),"Kalender" to MR.strings.dash_period_calendar.text()).forEach{(value,label)->
            val active=period==value
            Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(20.dp)).background(if(active)(if(dark)FFColors.pill else FFColors.onSurface) else Color.Transparent)
                .clickable(role=Role.Tab){select(value)},contentAlignment=Alignment.Center) {
                Text(label,style=FFType.bodyMain.copy(fontSize=12.sp,lineHeight=16.sp,fontWeight=FontWeight.Normal),
                    color=if(active)(if(dark)FFColors.onPill else FFColors.canvas) else FFColors.onSurfaceVariant,maxLines=1)
            }
        }
    }
}

@Composable
private fun BalanceCard(currency:String,liquid:Boolean,balance:String,income:String,expense:String,gradient:List<Long>,angle:Float,
    showCurrencySwap:Boolean,privacy:Boolean,toggleLiquid:()->Unit,cycleCurrency:()->Unit,togglePrivacy:()->Unit,onIncome:()->Unit,onExpense:()->Unit) {
    val olloStyle=gradient==cardGradientColors("teal")
    val darkCard=FFColors.isDark
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).drawWithCache {
        val colors=gradient.map{Color(it)}
        onDrawBehind{
            if(olloStyle){
                drawOlloBalanceGradient(darkCard)
            } else {val radius=size.maxDimension*.7f;val offset=Offset(cos(angle)*radius,sin(angle)*radius);drawRect(Brush.linearGradient(colors,center-offset,center+offset))}
        }
    }.testTag("balance-card-$currency").padding(start=16.dp,end=16.dp,top=20.dp,bottom=16.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            Text(if(liquid)MR.strings.dash_balance_liquid.text(currency) else MR.strings.dash_balance_total.text(currency),style=FFType.bodyMain.copy(fontSize=14.sp,lineHeight=18.sp),color=Color.White.copy(alpha=.8f))
            if(showCurrencySwap)Box(Modifier.clip(CircleShape).clickable(role=Role.Button,onClick=cycleCurrency).padding(4.dp).testTag("dashboard-currency-swap")){
                FFIcon("hi_swap",Color.White.copy(alpha=.8f),18.dp,contentDescription=MR.strings.dash_cd_swap_currency.text())
            }
            Box(Modifier.clip(CircleShape).clickable(role=Role.Button,onClick=toggleLiquid).padding(4.dp).testTag("dashboard-liquidity-toggle")){
                FFIcon("hi_sort",Color.White.copy(alpha=.8f),20.dp,contentDescription=MR.strings.dash_cd_show_liquid.text())
            }
            Box(Modifier.clip(CircleShape).clickable(role=Role.Button,onClick=togglePrivacy).padding(4.dp).testTag("dashboard-privacy")){
                FFIcon(if(privacy)"hi_view_off" else "hi_view",Color.White.copy(alpha=.8f),20.dp,contentDescription=MR.strings.dash_cd_toggle_privacy.text())
            }
        }
        Spacer(Modifier.height(8.dp))
        FitMoneyText(balance,FFType.amountXl,Color.White,TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(13.dp)) {
            BalanceSummary(transactionTypeLabel("income"),income,"hi_arrow_down",Modifier.weight(1f),onIncome)
            BalanceSummary(transactionTypeLabel("expense"),expense,"hi_arrow_up",Modifier.weight(1f),onExpense)
        }
    }
}

@Composable
private fun BalanceSummary(title:String,value:String,icon:String,modifier:Modifier,click:()->Unit) {
    val shape=RoundedCornerShape(20.dp)
    Column(modifier.clip(shape).background(Color.White.copy(alpha=.10f)).border(1.dp,Color.White.copy(alpha=.25f),shape)
        .clickable(role=Role.Button,onClick=click).padding(horizontal=10.dp,vertical=10.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(28.dp).clip(CircleShape).background(Color.White.copy(alpha=.22f)),contentAlignment=Alignment.Center){
                FFIcon(icon,Color.White,18.dp)
            }
            Spacer(Modifier.width(9.dp))
            Text(title,style=FFType.bodyMain.copy(fontSize=12.sp,lineHeight=16.sp),color=Color.White.copy(alpha=.8f),maxLines=1)
        }
        FitMoneyText(value,FFType.amountMd,Color.White)
    }
}

private fun menuColors(destination:String,menuTheme:String,dark:Boolean):Pair<Color,Color> {
    val index=financeMenus.indexOf(destination).coerceAtLeast(0)
    val fallback=menuPastel.getOrElse(index){menuPastel[0]}
    if(menuTheme=="colorful"){
        if(dark){val c=Color(menuDark.getOrElse(index){menuDark[0]});return c.copy(alpha=.18f) to c}
        return Color(fallback.first) to Color(fallback.second)
    }
    val preset=menuThemeColors(menuTheme)
    val accent=Color(preset[index%preset.size])
    return accent.copy(alpha=.14f) to accent
}

@Composable
private fun MenuSection(menus:List<String>,page:Int,onMore:()->Unit,onNavigate:(String)->Unit,menuTheme:String,appearance:String) {
    val pages=menus.chunked(4)
    val dark=isDarkUi()
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text(MR.strings.dash_menu.text(),Modifier.weight(1f),style=FFType.sectionTitle)
            var menuPopup by remember{mutableStateOf(false)}
            Box(Modifier.testTag("dashboard-menu-more")) {
                Box(Modifier.size(40.dp).clip(CircleShape).clickable(role=Role.Button){menuPopup=true}.semantics{contentDescription=MR.strings.dash_more.text()},contentAlignment=Alignment.Center){
                    FFIcon("more_vert",FFColors.onSurface,24.dp)
                }
                // Ollo: ⋮ membuka Tampilan Menu & Susunan Menu (halaman menu digeser, bukan diganti lewat ⋮).
                DropdownMenu(menuPopup,{menuPopup=false},containerColor=FFColors.surface) {
                    DropdownMenuItem(text={Text(MR.strings.dash_menu_appearance.text(),style=FFType.bodyMain)},leadingIcon={FFIcon("hi_paint",FFColors.onSurfaceVariant,20.dp)},onClick={menuPopup=false;onNavigate("Tampilan Menu")})
                    DropdownMenuItem(text={Text(MR.strings.dash_menu_order.text(),style=FFType.bodyMain)},leadingIcon={FFIcon("hi_sort",FFColors.onSurfaceVariant,20.dp)},onClick={menuPopup=false;onNavigate("Susunan Menu")})
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        when(appearance) {
            "list" -> Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                menus.forEach{destination->
                    val (background,foreground)=menuColors(destination,menuTheme,dark)
                    val index=financeMenus.indexOf(destination).coerceAtLeast(0)
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(FFColors.surface).clickable(role=Role.Button){onNavigate(destination)}.padding(12.dp),
                        verticalAlignment=Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(background),contentAlignment=Alignment.Center){FFIcon(menuIcons.getOrElse(index){menuIcons[0]},foreground,20.dp)}
                        Spacer(Modifier.width(12.dp))
                        Text(menuLabel(menuTitles.getOrElse(index){destination}),Modifier.weight(1f),style=FFType.bodyMain,maxLines=1,overflow=TextOverflow.Ellipsis)
                        FFIcon("chevron_right_rounded",FFColors.onSurfaceVariant,16.dp)
                    }
                }
            }
            "grid" -> Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                menus.chunked(4).forEach{row->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        row.forEach{destination->val (background,foreground)=menuColors(destination,menuTheme,dark);val index=financeMenus.indexOf(destination).coerceAtLeast(0)
                            MenuTile(menuLabel(menuTitles.getOrElse(index){destination}),menuIcons.getOrElse(index){menuIcons[0]},background,foreground,Modifier.weight(1f)){onNavigate(destination)}}
                        repeat(4-row.size){Spacer(Modifier.weight(1f))}
                    }
                }
            }
            else -> BoxWithConstraints(Modifier.fillMaxWidth()) {
                val tile=(maxWidth-12.dp*3)/4
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("dashboard-menu-row"),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    menus.forEach{destination->
                        val (background,foreground)=menuColors(destination,menuTheme,dark)
                        val index=financeMenus.indexOf(destination).coerceAtLeast(0)
                        MenuTile(menuLabel(menuTitles.getOrElse(index){destination}),menuIcons.getOrElse(index){menuIcons[0]},background,foreground,Modifier.width(tile)){onNavigate(destination)}
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuTile(title:String,icon:String,background:Color,foreground:Color,modifier:Modifier,onClick:()->Unit) {
    Column(modifier.clickable(role=Role.Button,onClick=onClick).padding(vertical=2.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(background),contentAlignment=Alignment.Center){
            FFIcon(icon,foreground,26.dp)
        }
        Spacer(Modifier.height(10.dp))
        Text(title,style=FFType.bodyMain.copy(fontSize=12.sp,lineHeight=16.sp),color=FFColors.onSurface.copy(alpha=.9f),maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}

@Composable
private fun MonthlyBudgetCard(data:FinanceData,month:String,preferences:AppPreferences,amount:(Long,String)->String,navigate:(String)->Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(FFColors.surface).clickable(role=Role.Button){navigate("Anggaran")}.padding(horizontal=12.dp,vertical=14.dp)) {
        Text(MR.strings.dash_monthly_budget.text(),style=FFType.sectionTitle)
        val budgets=data.budgets.filter{it.period=="monthly" && it.month==month}
        if(budgets.isEmpty())Row(Modifier.fillMaxWidth().padding(top=16.dp),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically){
            FFIcon("hi_add_circle",FFColors.onSurfaceVariant,18.dp)
            Spacer(Modifier.width(10.dp))
            Text(MR.strings.dash_budget_empty.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant)
        }
        else budgets.map{it.currency}.distinct().forEach{currency->
            val entries=budgets.filter{it.currency==currency}
            val ids=entries.map{it.categoryId}.toSet()
            val limit=entries.fold(0L){sum,b->checkedAdd(sum,b.limitMinor)}
            val spent=data.reportTransactions().filter{it.type=="expense" && it.currency==currency && it.categoryId in ids && it.date in recordingPeriod(month,preferences.recordingDay)}.fold(0L){sum,t->checkedAdd(sum,t.amountMinor)}
            Spacer(Modifier.height(12.dp))
            Text(MR.strings.dash_budget_count.text(entries.size,monthLabel(month)),style=FFType.metaData,color=FFColors.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            FitMoneyText("${amount(spent,currency)} / ${amount(limit,currency)}",FFType.amountMd)
            LinearProgressIndicator(progress={if(limit>0)spent.toFloat()/limit else 0f},modifier=Modifier.fillMaxWidth().padding(top=8.dp),color=if(spent>limit)FFColors.expense else FFColors.income)
        }
    }
}

@Composable
private fun WalletsSection(accounts:List<Account>,data:FinanceData,amount:(Long,String)->String,navigate:(String)->Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text(MR.strings.dash_my_wallets.text(),Modifier.weight(1f),style=FFType.sectionTitle)
            Box(Modifier.testTag("dashboard-wallets-more")) {
                Box(Modifier.size(40.dp).clip(CircleShape).clickable(role=Role.Button){navigate("Dompet")}.semantics{contentDescription=MR.strings.dash_more.text()},contentAlignment=Alignment.Center){
                    FFIcon("more_vert",FFColors.onSurface,24.dp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if(accounts.isEmpty())OutlinedButton(onClick={navigate("Dompet")}){Text(MR.strings.dash_add_wallet.text())}
        else Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            accounts.chunked(2).forEach{row->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    row.forEach{account->DashboardWalletTile(account,amount(data.balance(account),account.currency),Modifier.weight(1f)){navigate("Dompet:${account.id}")}}
                    if(row.size==1)Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DashboardWalletTile(account:Account,formatted:String,modifier:Modifier,onClick:()->Unit) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(FFColors.surface).clickable(role=Role.Button,onClick=onClick).padding(horizontal=12.dp,vertical=11.dp)
        .testTag("dashboard-wallet-${account.id}")) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(33.dp).clip(CircleShape).background(FFColors.surfaceContainerHigh),contentAlignment=Alignment.Center){
                AccountIconContent(account,if(account.type=="cash")"hi_wallet" else account.icon,FFColors.onSurface,18.dp)
            }
            Spacer(Modifier.width(9.dp))
            Text(account.name,style=FFType.bodyMain.copy(fontSize=14.5.sp,lineHeight=20.sp),color=FFColors.onSurface.copy(alpha=.85f),maxLines=1,overflow=TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(10.dp))
        Text("${account.type.uppercase()} • ${account.currency}",style=FFType.metaData.copy(fontSize=10.5.sp,lineHeight=14.sp),color=FFColors.onSurfaceVariant,maxLines=1)
        Spacer(Modifier.height(4.dp))
        FitMoneyText(formatted,FFType.amountMd)
    }
}

@Composable
private fun RecentTransactionsSection(transactions:List<Transaction>,sizeLimit:Int,calendarMode:Boolean,data:FinanceData,amount:(Long,String)->String,edit:(Transaction)->Unit,navigate:(String)->Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text(MR.strings.dash_recent_transactions.text(),Modifier.weight(1f),style=FFType.sectionTitle)
            if(!calendarMode)Text(MR.strings.dash_see_all.text(),Modifier.clip(RoundedCornerShape(50)).clickable(role=Role.Button){navigate("Transaksi")}.padding(horizontal=4.dp,vertical=4.dp),
                style=FFType.metaData,color=FFColors.onSurface)
        }
        if(transactions.isEmpty())Column(Modifier.fillMaxWidth().padding(vertical=28.dp),horizontalAlignment=Alignment.CenterHorizontally){
            FFIcon("receipt_long",FFColors.outline,52.dp)
            Spacer(Modifier.height(16.dp))
            Text(MR.strings.dash_no_transactions.text(),style=FFType.sectionTitle)
            Text(MR.strings.dash_no_transactions_hint.text(),style=FFType.metaData,color=FFColors.onSurfaceVariant,textAlign=TextAlign.Center)
        }
        else transactions.take(sizeLimit).groupBy{it.date}.forEach{(date,items)->
            Row(Modifier.fillMaxWidth().padding(top=16.dp,bottom=4.dp),verticalAlignment=Alignment.CenterVertically) {
                Text(fullDateLabel(date),Modifier.weight(1f),style=FFType.bodyMain,color=FFColors.onSurfaceVariant)
                Column(Modifier.width(140.dp),horizontalAlignment=Alignment.End) {
                    items.groupBy{it.currency}.forEach{(currency,records)->
                        val net=records.fold(0L){sum,t->when(t.type){"income"->checkedAdd(sum,t.amountMinor);"expense"->checkedAdd(sum,-t.amountMinor);else->sum}}
                        if(net!=0L)FitMoneyText((if(net>0)"+" else "")+amount(net,currency),FFType.bodyMain.copy(fontSize=17.sp,lineHeight=22.sp),
                            if(net>0)FFColors.income else FFColors.expense,TextAlign.End)
                    }
                }
            }
            items.forEach{tx->DashboardTransactionRow(tx,data,(if(tx.type=="expense")"−" else if(tx.type=="income")"+" else "")+amount(tx.amountMinor,tx.currency),edit)}
        }
    }
}

@Composable
internal fun DashboardTransactionRow(tx:Transaction,data:FinanceData,formatted:String,edit:(Transaction)->Unit,topPadding:androidx.compose.ui.unit.Dp=16.dp) {
    val wallet=data.accounts.find{it.id==tx.accountId}?.name ?: data.accountName(tx.accountId)
    val subtitle=if(tx.source=="manual"||tx.source.isBlank()) wallet else "$wallet · ${transactionKindLabel(tx,data)}"
    Row(Modifier.fillMaxWidth().padding(top=topPadding).clip(RoundedCornerShape(20.dp)).background(FFColors.surface).clickable(role=Role.Button){edit(tx)}
        .padding(horizontal=16.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically) {
        // Ollo: kategori pemasukan memakai warna hijau pemasukan.
        val tone=if(tx.type=="income")FFColors.income else categoryColor(tx.categoryId)
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(tone.copy(alpha=.14f)),contentAlignment=Alignment.Center){
            FFIcon(categoryIcon(tx.categoryId),tone,24.dp)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(tx.title,style=FFType.bodyMain.copy(fontSize=13.5.sp,lineHeight=18.sp),maxLines=1,overflow=TextOverflow.Ellipsis)
            if(subtitle.isNotBlank())Text(subtitle,style=FFType.metaData.copy(fontSize=10.5.sp,lineHeight=14.sp),color=FFColors.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
            Text(tx.description.ifBlank{"-"},style=FFType.metaData.copy(fontSize=10.5.sp,lineHeight=14.sp),color=FFColors.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.width(150.dp),horizontalAlignment=Alignment.End) {
            val time=timeLabel(tx.createdAt)
            if(time.isNotBlank())Text(time,style=FFType.metaData.copy(fontSize=10.5.sp,lineHeight=14.sp),color=FFColors.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            FitMoneyText(formatted,FFType.bodyMain.copy(fontSize=17.sp,lineHeight=22.sp),if(tx.type=="expense")FFColors.expense else if(tx.type=="income")FFColors.income else FFColors.onSurfaceVariant,TextAlign.End)
        }
    }
}

private fun transactionKindLabel(tx:Transaction,data:FinanceData):String = when(tx.source){
    "goal_deposit","goal_withdrawal" -> MR.strings.dash_tx_saving.text()
    "bill" -> MR.strings.dash_tx_bill_payment.text()
    "debt_initial" -> MR.strings.dash_tx_debt.text()
    "debt_payment" -> MR.strings.dash_tx_debt_payment.text()
    "bundle" -> MR.strings.dash_tx_bundle.text()
    "reimbursement" -> menuLabel("Reimburse")
    "recurring" -> MR.strings.dash_tx_recurring.text()
    else -> if(tx.type=="transfer") transactionTypeLabel("transfer") else data.categories.find{it.id==tx.categoryId}?.name ?: transactionTypeLabel(if(tx.type=="income") "income" else "expense")
}

internal fun fullDateLabel(date:String):String = runCatching{
    val parsed=LocalDate.parse(date)
    MR.strings.dash_full_date.text(dayName(parsed.dayOfWeek.isoDayNumber),parsed.dayOfMonth,monthName(parsed.monthNumber),parsed.year)
}.getOrDefault(date)
private fun timeLabel(epochMillis:Long):String = runCatching{
    val time=Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    val hour=time.hour.toString().padStart(2,'0');val minute=time.minute.toString().padStart(2,'0')
    "$hour:$minute"
}.getOrDefault("")
