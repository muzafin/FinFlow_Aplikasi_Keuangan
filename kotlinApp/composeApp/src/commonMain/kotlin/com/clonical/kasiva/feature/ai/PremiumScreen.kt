package com.clonical.kasiva.feature.ai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
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
import com.clonical.kasiva.app.navigation.Editor
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.model.FinanceData
import com.clonical.kasiva.core.platform.NativeServices
import com.clonical.kasiva.core.platform.PremiumPlan
import com.clonical.kasiva.core.ui.DesignCard
import com.clonical.kasiva.core.ui.FFColors
import com.clonical.kasiva.core.ui.FFIcon
import com.clonical.kasiva.core.ui.FFIconButton
import com.clonical.kasiva.core.ui.FFType
import com.clonical.kasiva.i18n.*

private val premiumBlue = Color(0xFF3B82F6)
private fun planCopy(kind: String): Pair<String, String> = when (kind) {
    "monthly" -> Pair(MR.strings.ai_plan_monthly.text(), MR.strings.ai_plan_monthly_billing.text())
    "six_month" -> Pair(MR.strings.ai_plan_six_month.text(), MR.strings.ai_plan_six_month_billing.text())
    "annual" -> Pair(MR.strings.ai_plan_annual.text(), MR.strings.ai_plan_annual_billing.text())
    "lifetime" -> Pair(MR.strings.ai_plan_lifetime.text(), MR.strings.ai_plan_lifetime_billing.text())
    else -> throw NoSuchElementException("Unknown plan $kind")
}
private fun premiumFeatures() = listOf(
    Triple("forum_outlined", MR.strings.ai_feature_unlimited_ai.text(), MR.strings.ai_feature_unlimited_ai_desc.text()),
    Triple("account_balance_wallet_outlined", MR.strings.ai_feature_wallets.text(), MR.strings.ai_feature_wallets_desc.text(FreeLimits.WALLETS, FreeLimits.CARDS)),
    Triple("repeat_rounded", MR.strings.ai_feature_recurring.text(), MR.strings.ai_feature_recurring_desc.text(FreeLimits.RECURRING)),
    Triple("redeem_outlined", MR.strings.ai_feature_wishlist.text(), MR.strings.ai_feature_wishlist_desc.text(FreeLimits.WISHES)),
    Triple("stars_rounded", MR.strings.ai_feature_support.text(), MR.strings.ai_feature_support_desc.text()),
)

/** Batas paket Free (mengikuti Ollo). */
object FreeLimits { const val WALLETS = 7; const val CARDS = 10; const val RECURRING = 7; const val GOALS = 7; const val WISHES = 7 }

/** Mengembalikan pesan bila editor baru melewati batas Free. */
fun freeLimitReached(editor: Editor?, data: FinanceData): String? = when {
    editor is Editor.Wallet && editor.value == null && data.accounts.count { !it.archived && it.goalId == null } >= FreeLimits.WALLETS -> MR.strings.ai_limit_wallets.text(FreeLimits.WALLETS)
    editor is Editor.CardForm && editor.value == null && data.cards.size >= FreeLimits.CARDS -> MR.strings.ai_limit_cards.text(FreeLimits.CARDS)
    editor is Editor.RepeatForm && editor.value == null && data.recurring.count { it.active } >= FreeLimits.RECURRING -> MR.strings.ai_limit_recurring.text(FreeLimits.RECURRING)
    editor is Editor.Target && editor.value == null && data.goals.size >= FreeLimits.GOALS -> MR.strings.ai_limit_goals.text(FreeLimits.GOALS)
    editor is Editor.WishForm && editor.value == null && data.wishes.size >= FreeLimits.WISHES -> MR.strings.ai_limit_wishes.text(FreeLimits.WISHES)
    else -> null
}

@Composable
fun PremiumGateDialog(message: String, onUpgrade: () -> Unit, onDismiss: () -> Unit) = AlertDialog(onDismissRequest = onDismiss,
    title = { Text(MR.strings.ai_gate_title.text()) }, text = { Text(MR.strings.ai_gate_body.text(message)) },
    confirmButton = { TextButton(onClick = { onDismiss(); onUpgrade() }) { Text(MR.strings.ai_view_premium.text(), fontWeight = FontWeight.SemiBold) } },
    dismissButton = { TextButton(onClick = onDismiss) { Text(MR.strings.ai_later.text()) } })

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
        controller.run(MR.strings.ai_working_premium_check.text()) {
            try { controller.premium = controller.service.refreshPremium(); message = if (controller.isPremium) success else MR.strings.ai_purchase_not_active.text() }
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
                    Text(if (controller.isPremium) MR.strings.ai_premium_active_title.text() else MR.strings.ai_premium_try.text(), style = FFType.appTitle.copy(fontSize = 26.sp), color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    listOf(MR.strings.ai_premium_hero_full.text(), MR.strings.ai_premium_hero_ai.text(), MR.strings.ai_premium_hero_limits.text()).forEach {
                        Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            FFIcon("check_rounded", Color.White, 16.dp); Spacer(Modifier.width(10.dp)); Text(it, style = FFType.bodyMain, color = Color.White.copy(alpha = 0.92f))
                        }
                    }
                }
            }
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Spacer(Modifier.height(12.dp))
                if (controller.isPremium) DesignCard {
                    Text(MR.strings.ai_status_premium.text(), style = FFType.sectionTitle)
                    Text(controller.premium.productId?.let { MR.strings.ai_plan_label.text(it) } ?: MR.strings.ai_premium_active_account.text(), style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
                }
                Text(MR.strings.ai_choose_plan.text(), style = FFType.sectionTitle)
                listOf("monthly", "six_month", "annual", "lifetime").forEach { kind ->
                    val item = plans?.find { it.kind == kind }
                    PlanCard(kind, item, selected == kind) { selected = kind }
                }
                if (plans == null) Text(when {
                    uid == null -> MR.strings.ai_login_to_subscribe_hint.text()
                    !native.premiumConfigured() -> MR.strings.ai_price_not_configured.text()
                    else -> MR.strings.ai_loading_prices.text()
                }, style = FFType.metaData, color = FFColors.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text(MR.strings.ai_premium_features.text(), style = FFType.sectionTitle)
                premiumFeatures().forEach { (icon, title, caption) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(premiumBlue.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { FFIcon(icon, premiumBlue, 22.dp) }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) { Text(title, style = FFType.bodyMain.copy(fontSize = 16.sp), fontWeight = FontWeight.Medium); Text(caption, style = FFType.metaData, color = FFColors.onSurfaceVariant) }
                        FFIcon("check_circle_rounded", premiumBlue, 22.dp)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    TextButton(enabled = uid != null && !busy, onClick = {
                        busy = true; native.restorePremium(requireNotNull(uid)) { ok, failure -> afterStore(ok, failure, MR.strings.ai_restore_success.text()) }
                    }) { Text(MR.strings.ai_restore_purchases.text(), color = premiumBlue) }
                }
                Text(MR.strings.ai_subscription_terms.text(),
                    style = FFType.metaData, color = FFColors.onSurfaceVariant)
                TextButton(onClick = { native.manageSubscription()?.let { message = it } }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(MR.strings.ai_manage_subscription.text()) }
            }
        }
        Surface(Modifier.align(Alignment.BottomCenter).fillMaxWidth(), color = FFColors.background) {
            Button(onClick = {
                when {
                    uid == null -> connect()
                    plan == null -> message = if (native.premiumConfigured()) MR.strings.ai_plan_unavailable.text() else MR.strings.ai_purchase_not_configured.text()
                    else -> { busy = true; native.purchasePremium(plan.id) { ok, failure -> afterStore(ok, failure, MR.strings.ai_purchase_thanks.text()) } }
                }
            }, enabled = !busy && !controller.isPremium, shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = premiumBlue, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp).height(56.dp).testTag("premium-subscribe")) {
                if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text(when { controller.isPremium -> MR.strings.ai_premium_active.text(); uid == null -> MR.strings.ai_login_to_subscribe.text(); else -> MR.strings.ai_subscribe_now.text() },
                    style = FFType.bodyMain.copy(fontSize = 17.sp), fontWeight = FontWeight.SemiBold)
            }
        }
    }
    message?.let { AlertDialog(onDismissRequest = { message = null }, title = { Text("Kasiva Premium") }, text = { Text(it) },
        confirmButton = { TextButton(onClick = { message = null }) { Text(MR.strings.common_close.text()) } }) }
}

@Composable
private fun PlanCard(kind: String, plan: PremiumPlan?, selected: Boolean, onClick: () -> Unit) {
    val (title, billing) = planCopy(kind)
    val chip = when (kind) {
        "monthly" -> plan?.trialDays?.takeIf { it > 0 }?.let { MR.strings.ai_chip_free_days.text(it) }
        "six_month" -> MR.strings.ai_chip_save_17.text(); "lifetime" -> MR.strings.ai_chip_lifetime.text(); else -> null
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
                        Text(plan?.let { it.pricePerMonth?.let { m -> MR.strings.ai_price_per_month.text(m) } ?: it.price } ?: "—", style = FFType.bodyMain.copy(fontSize = 16.sp),
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
            Text(MR.strings.ai_chip_best.text(), style = FFType.badgeLabel.copy(fontSize = 11.sp), color = Color.White)
        }
    }
}

/** Kartu "Upgrade ke Premium" di Profil: biru tua dengan pola lingkaran dan chevron dalam lingkaran (seperti Ollo). */
@Composable
fun UpgradePremiumCard(controller: AiController, onClick: () -> Unit) {
    val dark = FFColors.isDark
    Box(Modifier.fillMaxWidth().height(115.dp).clip(RoundedCornerShape(24.dp))
        .drawBehind {
            drawRect(Brush.linearGradient(if (dark) listOf(Color(0xFF112155), Color(0xFF1A48C9), Color(0xFF0F91E3)) else listOf(Color(0xFF2665EA), Color(0xFF377DF3), Color(0xFF37AFF6)), Offset(0f, size.height), Offset(size.width, 0f)))
            val center = Offset(size.width * 1.02f, size.height * .5f)
            listOf(.35f, .55f, .75f, .95f).forEach { r ->
                drawCircle(Color.White.copy(alpha = .16f), radius = size.height * r * 1.2f, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
            }
        }
        .clickable(onClick = onClick).testTag("profile-upgrade-premium")) {
        Column(Modifier.align(Alignment.CenterStart).padding(start = 20.dp, end = 70.dp)) {
            Text(if (controller.isPremium) "Kasiva Premium" else MR.strings.ai_upgrade_premium.text(), style = FFType.sectionTitle.copy(fontSize = 17.sp, lineHeight = 22.sp), color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(if (controller.isPremium) MR.strings.ai_upgrade_thanks.text() else MR.strings.ai_upgrade_caption.text(), style = FFType.metaData.copy(fontSize = 11.sp), color = Color.White.copy(alpha = 0.85f))
        }
        Box(Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp).size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = .14f))
            .border(1.dp, Color.White.copy(alpha = .3f), CircleShape), contentAlignment = Alignment.Center) {
            FFIcon("hi_chevron_right", Color.White, 18.dp)
        }
    }
}

/** Bagian Manajemen Akun: status akun, batas AI, dan persetujuan AI (seperti Ollo). */
fun LazyListScope.aiAccountSection(controller: AiController, cloudSync: Boolean, openPremium: () -> Unit) {
    item {
        LaunchedEffect(Unit) { controller.refreshStatus() }
        DesignCard(Modifier.testTag("account-status")) {
            Text(MR.strings.ai_account_status.text(), style = FFType.sectionTitle)
            Spacer(Modifier.height(10.dp))
            StatusRow(MR.strings.ai_tier.text(), if (controller.isPremium) "PREMIUM" else "FREE")
            StatusRow(MR.strings.ai_cloud_sync.text(), if (cloudSync) MR.strings.ai_active.text() else MR.strings.ai_inactive.text())
            StatusRow("Kasiva Assistant", if (controller.available) MR.strings.ai_available.text() else MR.strings.ai_needs_login.text())
            if (!controller.isPremium) TextButton(onClick = openPremium) { Text(MR.strings.ai_upgrade_premium.text()) }
        }
    }
    if (controller.available) item {
        DesignCard(Modifier.testTag("account-ai-limits")) {
            Text(MR.strings.ai_limits_title.text(), style = FFType.sectionTitle)
            Spacer(Modifier.height(10.dp))
            val usage = controller.status?.usage
            AiFeature.entries.forEach { feature ->
                val value = usage?.get(feature.id)
                StatusRow(feature.title, when {
                    value == null -> if (controller.statusUnavailable) MR.strings.ai_not_active.text() else "…"
                    value.limit == null -> MR.strings.ai_unlimited_premium.text()
                    feature == AiFeature.Assistant -> MR.strings.ai_assistant_usage.text((value.limit - value.used).coerceAtLeast(0), value.used, value.limit)
                    else -> MR.strings.ai_monthly_usage.text(value.used, value.limit)
                })
            }
        }
    }
    if (controller.available) item {
        DesignCard(Modifier.testTag("account-ai-consent")) {
            Text(MR.strings.ai_privacy_title.text(), style = FFType.sectionTitle)
            Spacer(Modifier.height(6.dp))
            Text(MR.strings.ai_privacy_body.text(),
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
        Text(MR.strings.ai_login_required_note.text(), style = FFType.metaData, color = FFColors.onSurfaceVariant)
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, Modifier.weight(1f), style = FFType.bodyMain, color = FFColors.onSurfaceVariant)
        Text(value, style = FFType.bodyMain, fontWeight = FontWeight.Medium)
    }
}
