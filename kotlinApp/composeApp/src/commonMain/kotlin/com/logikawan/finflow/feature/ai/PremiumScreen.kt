package com.logikawan.finflow.feature.ai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logikawan.finflow.app.navigation.Editor
import com.logikawan.finflow.core.model.FinanceData
import com.logikawan.finflow.core.platform.NativeServices
import com.logikawan.finflow.core.platform.PremiumPlan
import com.logikawan.finflow.core.ui.DesignCard
import com.logikawan.finflow.core.ui.FFColors
import com.logikawan.finflow.core.ui.FFIcon
import com.logikawan.finflow.core.ui.FFIconButton
import com.logikawan.finflow.core.ui.FFType

private val premiumBlue = Color(0xFF3B82F6)
private val planCopy = mapOf(
    "monthly" to Pair("Bulanan", "Ditagih bulanan"), "six_month" to Pair("6 Bulan", "ditagih per 6 bulan"),
    "annual" to Pair("Tahunan", "ditagih per tahun"), "lifetime" to Pair("Seumur Hidup", "sekali bayar, milikmu selamanya"),
)
private val premiumFeatures = listOf(
    Triple("forum_outlined", "AI tanpa batas", "Asisten, Chat, Voice & Scan. Free: terbatas, Premium: tanpa batas"),
    Triple("account_balance_wallet_outlined", "Dompet & kartu tanpa batas", "Free: ${FreeLimits.WALLETS} dompet + ${FreeLimits.CARDS} kartu, Premium: tanpa batas"),
    Triple("repeat_rounded", "Berulang & target tanpa batas", "Free: ${FreeLimits.RECURRING} item masing-masing, Premium: tanpa batas"),
    Triple("redeem_outlined", "Wishlist tanpa batas", "Free: ${FreeLimits.WISHES} item, Premium: tanpa batas"),
    Triple("stars_rounded", "Dukung pengembangan FinFlow", "Membantu biaya server dan fitur baru"),
)

/** Batas paket Free (mengikuti Ollo). */
object FreeLimits { const val WALLETS = 7; const val CARDS = 10; const val RECURRING = 7; const val GOALS = 7; const val WISHES = 7 }

/** Mengembalikan pesan bila editor baru melewati batas Free. */
fun freeLimitReached(editor: Editor?, data: FinanceData): String? = when {
    editor is Editor.Wallet && editor.value == null && data.accounts.count { !it.archived && it.goalId == null } >= FreeLimits.WALLETS -> "Paket Free mendukung ${FreeLimits.WALLETS} dompet aktif."
    editor is Editor.CardForm && editor.value == null && data.cards.size >= FreeLimits.CARDS -> "Paket Free mendukung ${FreeLimits.CARDS} kartu."
    editor is Editor.RepeatForm && editor.value == null && data.recurring.count { it.active } >= FreeLimits.RECURRING -> "Paket Free mendukung ${FreeLimits.RECURRING} jadwal berulang."
    editor is Editor.Target && editor.value == null && data.goals.size >= FreeLimits.GOALS -> "Paket Free mendukung ${FreeLimits.GOALS} target tabungan."
    editor is Editor.WishForm && editor.value == null && data.wishes.size >= FreeLimits.WISHES -> "Paket Free mendukung ${FreeLimits.WISHES} wishlist."
    else -> null
}

@Composable
fun PremiumGateDialog(message: String, onUpgrade: () -> Unit, onDismiss: () -> Unit) = AlertDialog(onDismissRequest = onDismiss,
    title = { Text("Batas Paket Free") }, text = { Text("$message Upgrade ke Premium untuk menambah tanpa batas.") },
    confirmButton = { TextButton(onClick = { onDismiss(); onUpgrade() }) { Text("Lihat Premium", fontWeight = FontWeight.SemiBold) } },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Nanti") } })

@Composable
fun PremiumScreen(controller: AiController, native: NativeServices, back: () -> Unit, connect: () -> Unit) {
    var plans by remember { mutableStateOf<List<PremiumPlan>?>(null) }
    var selected by rememberSaveable { mutableStateOf("annual") }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val uid = controller.uid
    LaunchedEffect(uid) {
        if (uid != null && native.premiumConfigured()) native.premiumPlans(uid) { list, failure -> plans = list; if (failure != null) message = failure }
    }
    fun afterStore(ok: Boolean, failure: String?, success: String) {
        if (!ok) { busy = false; failure?.let { message = it }; return }
        controller.run("Memeriksa status Premium…") {
            try { controller.premium = controller.service.refreshPremium(); message = if (controller.isPremium) success else "Pembelian tercatat, tetapi hak Premium belum aktif. Coba Pulihkan Pembelian beberapa saat lagi." }
            finally { busy = false }
        }
    }
    val plan = plans?.find { it.kind == selected }
    Box(Modifier.fillMaxSize().background(FFColors.background).testTag("premium-screen")) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 120.dp)) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF1E3A8A), Color(0xFF2563EB), Color(0xFF06B6D4)))).statusBarsPadding().padding(bottom = 28.dp)) {
                FFIconButton("arrow_back_rounded", back, tint = Color.White)
                Column(Modifier.align(Alignment.CenterHorizontally)) {
                    Text(if (controller.isPremium) "FinFlow Premium Aktif" else "Coba FinFlow Premium", style = FFType.appTitle.copy(fontSize = 26.sp), color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    listOf("Buka pengalaman FinFlow sepenuhnya", "AI tanpa batas: Asisten, Chat, Voice & Scan", "Dompet, kartu & target tanpa batas").forEach {
                        Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            FFIcon("check_rounded", Color.White, 16.dp); Spacer(Modifier.width(10.dp)); Text(it, style = FFType.bodyMain, color = Color.White.copy(alpha = 0.92f))
                        }
                    }
                }
            }
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Spacer(Modifier.height(12.dp))
                if (controller.isPremium) DesignCard {
                    Text("Status: Premium", style = FFType.sectionTitle)
                    Text(controller.premium.productId?.let { "Paket: $it" } ?: "Hak Premium aktif untuk akun ini.", style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
                }
                Text("Pilih Paketmu", style = FFType.sectionTitle)
                listOf("monthly", "six_month", "annual", "lifetime").forEach { kind ->
                    val item = plans?.find { it.kind == kind }
                    PlanCard(kind, item, selected == kind) { selected = kind }
                }
                if (plans == null) Text(when {
                    uid == null -> "Masuk ke akun FinFlow untuk berlangganan; status Premium terikat ke akun."
                    !native.premiumConfigured() -> "Harga tampil setelah produk Premium dikonfigurasi di Google Play dan RevenueCat."
                    else -> "Memuat harga dari Google Play…"
                }, style = FFType.metaData, color = FFColors.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text("Fitur Premium", style = FFType.sectionTitle)
                premiumFeatures.forEach { (icon, title, caption) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(premiumBlue.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { FFIcon(icon, premiumBlue, 22.dp) }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) { Text(title, style = FFType.bodyMain.copy(fontSize = 16.sp), fontWeight = FontWeight.Medium); Text(caption, style = FFType.metaData, color = FFColors.onSurfaceVariant) }
                        FFIcon("check_circle_rounded", premiumBlue, 22.dp)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    TextButton(enabled = uid != null && !busy, onClick = {
                        busy = true; native.restorePremium(requireNotNull(uid)) { ok, failure -> afterStore(ok, failure, "Pembelian dipulihkan. Premium aktif.") }
                    }) { Text("Pulihkan Pembelian", color = premiumBlue) }
                }
                Text("Langganan bulanan, 6 bulan, atau tahunan ditagih ke akun Google Play saat pembelian dikonfirmasi dan diperpanjang otomatis kecuali dimatikan paling lambat 24 jam sebelum periode berakhir. Kelola atau batalkan di Google Play. Paket Seumur Hidup adalah pembelian sekali bayar.",
                    style = FFType.metaData, color = FFColors.onSurfaceVariant)
                TextButton(onClick = { native.manageSubscription()?.let { message = it } }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Kelola Langganan") }
            }
        }
        Surface(Modifier.align(Alignment.BottomCenter).fillMaxWidth(), color = FFColors.background) {
            Button(onClick = {
                when {
                    uid == null -> connect()
                    plan == null -> message = if (native.premiumConfigured()) "Paket belum tersedia. Coba lagi sebentar." else "Pembelian Premium belum dikonfigurasi."
                    else -> { busy = true; native.purchasePremium(plan.id) { ok, failure -> afterStore(ok, failure, "Terima kasih! Premium aktif.") } }
                }
            }, enabled = !busy && !controller.isPremium, shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = premiumBlue, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp).height(56.dp).testTag("premium-subscribe")) {
                if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text(when { controller.isPremium -> "Premium Aktif"; uid == null -> "Masuk untuk Berlangganan"; else -> "Berlangganan Sekarang" },
                    style = FFType.bodyMain.copy(fontSize = 17.sp), fontWeight = FontWeight.SemiBold)
            }
        }
    }
    message?.let { AlertDialog(onDismissRequest = { message = null }, title = { Text("FinFlow Premium") }, text = { Text(it) },
        confirmButton = { TextButton(onClick = { message = null }) { Text("Tutup") } }) }
}

@Composable
private fun PlanCard(kind: String, plan: PremiumPlan?, selected: Boolean, onClick: () -> Unit) {
    val (title, billing) = planCopy.getValue(kind)
    val chip = when (kind) {
        "monthly" -> plan?.trialDays?.takeIf { it > 0 }?.let { "$it HARI GRATIS" }
        "six_month" -> "HEMAT 17%"; "lifetime" -> "SEUMUR HIDUP"; else -> null
    }
    Box(Modifier.fillMaxWidth().padding(top = if (kind == "annual") 10.dp else 0.dp)) {
        Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = FFColors.surface,
            border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) premiumBlue else FFColors.outlineVariant),
            modifier = Modifier.fillMaxWidth().testTag("plan-$kind")) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(title, style = FFType.sectionTitle.copy(fontSize = 18.sp))
                        chip?.let { Spacer(Modifier.width(8.dp)); Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0x3322C55E)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text(it, style = FFType.badgeLabel.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = Color(0xFF22C55E)) } }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(plan?.let { it.pricePerMonth?.let { m -> "$m/bln" } ?: it.price } ?: "—", style = FFType.bodyMain.copy(fontSize = 16.sp),
                            fontWeight = FontWeight.SemiBold, color = if (selected) premiumBlue else FFColors.onSurface)
                        Text(" · " + (plan?.takeIf { it.pricePerMonth != null }?.let { "$billing ${it.price}" } ?: billing), style = FFType.metaData, color = FFColors.onSurfaceVariant)
                    }
                }
                Box(Modifier.size(24.dp).clip(CircleShape).background(if (selected) premiumBlue else Color.Transparent)
                    .then(if (selected) Modifier else Modifier.background(Color.Transparent)), contentAlignment = Alignment.Center) {
                    if (selected) FFIcon("check_rounded", Color.White, 16.dp)
                    else Box(Modifier.size(22.dp).clip(CircleShape).background(FFColors.outlineVariant).padding(2.dp).clip(CircleShape).background(FFColors.surface))
                }
            }
        }
        if (kind == "annual") Box(Modifier.align(Alignment.TopEnd).offset(x = (-16).dp, y = (-10).dp).clip(RoundedCornerShape(8.dp)).background(premiumBlue).padding(horizontal = 8.dp, vertical = 2.dp)) {
            Text("Terbaik · HEMAT 33%", style = FFType.badgeLabel.copy(fontSize = 11.sp), color = Color.White)
        }
    }
}

/** Kartu "Upgrade ke Premium" di Profil (posisi sama dengan Ollo: di bawah header). */
@Composable
fun UpgradePremiumCard(controller: AiController, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Color(0xFF1E3A8A), Color(0xFF2563EB), Color(0xFF06B6D4))))
        .clickable(onClick = onClick).padding(20.dp).testTag("profile-upgrade-premium"), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            FFIcon("workspace_premium_outlined", Color.White, 24.dp)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(if (controller.isPremium) "FinFlow Premium" else "Upgrade ke Premium", style = FFType.sectionTitle.copy(fontSize = 18.sp), color = Color.White)
            Text(if (controller.isPremium) "Terima kasih telah mendukung FinFlow." else "Buka semua fitur & hapus batas.", style = FFType.metaData, color = Color.White.copy(alpha = 0.85f))
        }
        FFIcon("chevron_right_rounded", Color.White, 22.dp)
    }
}

/** Bagian Manajemen Akun: status akun, batas AI, dan persetujuan AI (seperti Ollo). */
fun LazyListScope.aiAccountSection(controller: AiController, cloudSync: Boolean, openPremium: () -> Unit) {
    item {
        LaunchedEffect(Unit) { controller.refreshStatus() }
        DesignCard(Modifier.testTag("account-status")) {
            Text("Status Akun", style = FFType.sectionTitle)
            Spacer(Modifier.height(10.dp))
            StatusRow("Tier", if (controller.isPremium) "PREMIUM" else "FREE")
            StatusRow("Sinkronisasi Cloud", if (cloudSync) "Aktif" else "Tidak aktif")
            StatusRow("FinFlow Assistant", if (controller.available) "Tersedia" else "Perlu login")
            if (!controller.isPremium) TextButton(onClick = openPremium) { Text("Upgrade ke Premium") }
        }
    }
    if (controller.available) item {
        DesignCard(Modifier.testTag("account-ai-limits")) {
            Text("Batas AI", style = FFType.sectionTitle)
            Spacer(Modifier.height(10.dp))
            val usage = controller.status?.usage
            AiFeature.entries.forEach { feature ->
                val value = usage?.get(feature.id)
                StatusRow(feature.title, when {
                    value == null -> if (controller.statusUnavailable) "Belum aktif" else "…"
                    value.limit == null -> "Tanpa batas (Premium)"
                    feature == AiFeature.Assistant -> "${(value.limit - value.used).coerceAtLeast(0)} percobaan tersisa · ${value.used}/${value.limit} seumur akun"
                    else -> "${value.used}/${value.limit} bulan ini"
                })
            }
        }
    }
    if (controller.available) item {
        DesignCard(Modifier.testTag("account-ai-consent")) {
            Text("Privasi & Persetujuan AI", style = FFType.sectionTitle)
            Spacer(Modifier.height(6.dp))
            Text("Pemrosesan AI memerlukan persetujuan per fitur. Kamu bisa mencabutnya kapan saja; FinFlow akan bertanya lagi sebelum pemrosesan berikutnya.",
                style = FFType.metaData, color = FFColors.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            val consent = controller.status?.consent.orEmpty()
            AiFeature.entries.forEach { feature ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(feature.title, Modifier.weight(1f), style = FFType.bodyMain)
                    Switch(consent[feature.id] == true, { on -> if (on) controller.requireConsent(feature) {} else controller.setConsent(feature, false) },
                        enabled = controller.working == null)
                }
            }
        }
    }
    item {
        Text("Login akun diperlukan untuk AI karena permintaan diproses lewat server FinFlow.", style = FFType.metaData, color = FFColors.onSurfaceVariant)
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, Modifier.weight(1f), style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
        Text(value, style = FFType.bodyMain, fontWeight = FontWeight.Medium)
    }
}
