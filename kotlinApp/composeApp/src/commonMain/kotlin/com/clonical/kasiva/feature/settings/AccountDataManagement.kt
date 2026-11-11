package com.clonical.kasiva.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.model.FinanceData
import com.clonical.kasiva.core.platform.NativeServices
import com.clonical.kasiva.core.ui.*
import com.clonical.kasiva.feature.ai.AiController
import com.clonical.kasiva.feature.ai.AiFeature
import com.clonical.kasiva.feature.planning.OlloMenuButton
import com.clonical.kasiva.feature.planning.OlloMenuItem
import com.clonical.kasiva.i18n.*

/** Aksi halaman Manajemen Akun; semuanya disediakan App (repositori/NativeServices). */
internal class AccountManagementActions(
    val navigate: (String) -> Unit,
    val reload: () -> Unit,
    val backup: () -> Unit,
    val changePhoto: () -> Unit,
    val deleteAccount: () -> Unit,
    val connect: () -> Unit,
    val useLocal: () -> Unit,
    val signOut: (() -> Unit)?,
)

/** Manajemen Akun seperti Ollo: kartu profil, Status Akun, Batas AI, Privasi AI, penghapusan akun, dan catatan sinkronisasi. */
@Composable
internal fun AccountManagementPage(data: FinanceData, native: NativeServices, email: String, localMode: Boolean, busy: Boolean,
    ai: AiController?, actions: AccountManagementActions, accountExtras: LazyListScope.() -> Unit) {
    var dialog by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val premium = ai?.isPremium == true
    LaunchedEffect(ai) { ai?.refreshStatus() }
    TopBarActions {
        OlloMenuButton(buildList {
            add(OlloMenuItem("edit", MR.strings.set_acct_edit_profile.text(), "hi_pencil"))
            add(OlloMenuItem("photo", MR.strings.set_acct_change_photo.text(), "hi_image"))
            if (!localMode) add(OlloMenuItem("password", MR.strings.set_acct_send_reset.text(), "hi_mail"))
            add(OlloMenuItem("reload", MR.strings.set_acct_reload.text(), AdIcons.refresh))
        }, tint = FFColors.onSurface) { key ->
            when (key) {
                "edit" -> actions.navigate("Profil akun")
                "photo" -> if (!busy) actions.changePhoto()
                "password" -> actions.navigate("Ubah password")
                else -> if (!busy) actions.reload()
            }
        }
    }
    LazyColumn(Modifier.fillMaxSize().testTag("account-data-page"), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ProfileHeaderCard(data, native, email, localMode, premium, { actions.navigate("Profil akun") }, actions.changePhoto) }
        if (ai == null) {
            // Kompatibilitas: App lama mengirim bagian AI sendiri.
            accountExtras()
            item { LocalOrCloudButtons(localMode, busy, actions) { dialog = "sync" } }
        } else {
            item { AccountStatusCard(ai, localMode, premium, busy, actions) { dialog = "sync" } }
            if (ai.available) item { AiLimitsCard(ai) }
            if (ai.available) item { AiConsentCard(ai, { dialog = "privacy" }, { dialog = "withdraw" }) }
        }
        if (!localMode) item {
            AdCard(Modifier.testTag("account-deletion"), padding = 16.dp) {
                AdCardTitle(MR.strings.pa_deletion_title.text())
                Spacer(Modifier.height(12.dp))
                AdBody(MR.strings.pa_deletion_body.text())
                Spacer(Modifier.height(16.dp))
                AdPillButton(MR.strings.pa_backup_first.text(), "hi_cloud_upload", { try { actions.backup() } catch (e: Exception) { message = e.message } },
                    Modifier.fillMaxWidth(), enabled = data.featuresReady && !busy, tag = "account-backup-first")
                Spacer(Modifier.height(12.dp))
                AdPillButton(MR.strings.pa_request_deletion.text(), "hi_delete", actions.deleteAccount, Modifier.fillMaxWidth(), tint = AdTone.danger,
                    enabled = !busy, tag = "account-request-deletion")
                Spacer(Modifier.height(12.dp))
                AdPillButton(MR.strings.pa_contact_cs.text(), AdIcons.headset, {
                    message = native.composeSupport("Kasiva · ${MR.strings.pa_deletion_title.text()}",
                        "$email\nKasiva ${native.appVersion()}\n${native.deviceDescription()}") ?: MR.strings.set_bug_draft_opened.text()
                }, Modifier.fillMaxWidth(), tag = "account-contact-cs")
                Spacer(Modifier.height(14.dp))
                AdBody(MR.strings.pa_deletion_note.text(), size = 13.sp)
            }
        }
        message?.let { item { AdBody(it, color = FFColors.onSurface) } }
        item {
            AdCard {
                AdCardTitle(MR.strings.pa_sync_optional_title.text())
                Spacer(Modifier.height(12.dp))
                AdBody(MR.strings.pa_sync_optional_body.text())
            }
        }
    }
    when (dialog) {
        "sync" -> if (localMode) AdInfoDialog(MR.strings.ai_cloud_sync.text(), listOf(MR.strings.set_acct_local_desc.text()), { dialog = null },
            confirmLabel = MR.strings.set_acct_connect.text(), onConfirm = { dialog = null; actions.connect() })
        else AdInfoDialog(MR.strings.pa_sync_active_title.text(), listOf(MR.strings.set_devices_this.text(email), MR.strings.set_acct_cloud_desc.text()), { dialog = null },
            confirmLabel = MR.strings.set_acct_use_local.text(), onConfirm = { dialog = null; actions.useLocal() })
        "privacy" -> AdInfoDialog(MR.strings.pa_privacy_policy.text(), listOf(MR.strings.ai_consent_provider.text(), MR.strings.ai_consent_chat_text.text(),
            MR.strings.ai_consent_voice_no_audio.text(), MR.strings.ai_consent_scan_photo.text(), MR.strings.ai_consent_assistant_summary.text(),
            MR.strings.ai_consent_review.text(), MR.strings.ai_consent_revoke.text()), { dialog = null })
        "withdraw" -> AdInfoDialog(MR.strings.pa_withdraw_consent.text(), listOf(MR.strings.pa_withdraw_confirm_body.text()), { dialog = null },
            confirmLabel = MR.strings.pa_withdraw_consent.text(), danger = true, onConfirm = {
                dialog = null
                ai?.let { controller ->
                    val granted = AiFeature.entries.filter { controller.status?.consent?.get(it.id) == true }
                    controller.run(MR.strings.ai_working_revoking_consent.text()) {
                        granted.forEach { controller.service.setConsent(it, false) }
                        controller.status = controller.service.status()
                        message = MR.strings.pa_withdraw_done.text()
                    }
                }
            })
    }
}

@Composable
private fun ProfileHeaderCard(data: FinanceData, native: NativeServices, email: String, localMode: Boolean, premium: Boolean, edit: () -> Unit, changePhoto: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(FFColors.surface).clickable(role = Role.Button, onClick = edit)
        .testTag("account-profile-card").padding(horizontal = 18.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(56.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = MR.strings.set_acct_change_photo.text(), onClick = changePhoto)) {
            ProfilePhoto(data.profile, native, Modifier.size(56.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(data.profile?.name.orEmpty(), style = adText(18.sp, FontWeight.SemiBold, 24.sp), color = FFColors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(if (localMode) MR.strings.set_local_mode.text() else email, style = adText(14.sp, line = 20.sp), color = FFColors.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.clip(RoundedCornerShape(100.dp)).background(FFColors.raised).padding(horizontal = 10.dp, vertical = 4.dp)) {
            Text(if (premium) "PREMIUM" else "FREE", style = adText(12.sp, FontWeight.Medium, 16.sp), color = FFColors.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatusLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = adText(14.sp, line = 19.sp), color = FFColors.onSurfaceVariant)
        Text(value, style = adText(14.sp, FontWeight.Medium, 19.sp), color = FFColors.onSurface)
    }
}

@Composable
private fun AccountStatusCard(ai: AiController, localMode: Boolean, premium: Boolean, busy: Boolean, actions: AccountManagementActions, openSync: () -> Unit) {
    AdCard(Modifier.testTag("account-status")) {
        AdCardTitle(MR.strings.ai_account_status.text())
        Spacer(Modifier.height(14.dp))
        StatusLine(MR.strings.ai_tier.text(), if (premium) "PREMIUM" else "FREE")
        Spacer(Modifier.height(4.dp))
        StatusLine(MR.strings.ai_cloud_sync.text(), if (localMode) MR.strings.ai_inactive.text() else MR.strings.ai_active.text())
        Spacer(Modifier.height(4.dp))
        StatusLine("Kasiva Assistant", if (ai.available) MR.strings.ai_available.text() else MR.strings.ai_needs_login.text())
        Spacer(Modifier.height(18.dp))
        LocalOrCloudButtonsRow(localMode, busy, actions, openSync)
        if (!premium) {
            Spacer(Modifier.height(10.dp))
            if (com.clonical.kasiva.feature.ai.PREMIUM_ENABLED) AdPillButton(MR.strings.ai_upgrade_premium.text(), "hi_flag", { actions.navigate("Premium") }, Modifier.fillMaxWidth(), border = false, tag = "account-upgrade")
        }
    }
}

@Composable
private fun LocalOrCloudButtons(localMode: Boolean, busy: Boolean, actions: AccountManagementActions, openSync: () -> Unit) {
    AdCard { LocalOrCloudButtonsRow(localMode, busy, actions, openSync) }
}

@Composable
private fun LocalOrCloudButtonsRow(localMode: Boolean, busy: Boolean, actions: AccountManagementActions, openSync: () -> Unit) {
    if (localMode) {
        // Teks ini dicocokkan uji perangkat (LocalUiDeviceTest).
        AdPillButton(MR.strings.set_acct_connect.text(), AdIcons.cloudSync, actions.connect, Modifier.fillMaxWidth(), enabled = !busy, tag = "account-connect")
    } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AdPillButton(MR.strings.ai_cloud_sync.text(), AdIcons.cloudSync, openSync, Modifier.weight(1f), enabled = !busy, tag = "account-cloud-sync")
        actions.signOut?.let { signOut ->
            AdPillButton(MR.strings.set_logout.text(), AdIcons.logout, signOut, Modifier.weight(1f), tint = AdTone.danger, enabled = !busy, tag = "account-sign-out")
        }
    }
}

private fun featureIcon(feature: AiFeature): String = when (feature) {
    AiFeature.Scan -> "hi_scan"
    AiFeature.Chat -> "hi_chat"
    AiFeature.Voice -> "hi_voice"
    AiFeature.Assistant -> "hi_assistant"
}

@Composable
private fun AiLimitsCard(ai: AiController) {
    AdCard(Modifier.testTag("account-ai-limits")) {
        AdCardTitle(MR.strings.ai_limits_title.text())
        Spacer(Modifier.height(16.dp))
        val usage = ai.status?.usage
        listOf(AiFeature.Scan, AiFeature.Chat, AiFeature.Voice, AiFeature.Assistant).forEachIndexed { index, feature ->
            if (index > 0) Spacer(Modifier.height(10.dp))
            val value = usage?.get(feature.id)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(FFColors.canvas).padding(horizontal = 12.dp, vertical = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FFIcon(featureIcon(feature), FFColors.onSurface, 22.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(feature.title, Modifier.weight(1f), style = adText(16.sp, line = 22.sp), color = FFColors.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text(when {
                        value == null -> if (ai.statusUnavailable) MR.strings.ai_not_active.text() else "…"
                        value.limit == null -> MR.strings.ai_unlimited_premium.text()
                        feature == AiFeature.Assistant -> MR.strings.pa_ai_trial_left.text((value.limit - value.used).coerceAtLeast(0))
                        else -> MR.strings.ai_monthly_usage.text(value.used, value.limit)
                    }, Modifier.widthIn(max = 170.dp), style = adText(14.sp, FontWeight.Medium, 19.sp), color = FFColors.onSurface, textAlign = TextAlign.End)
                }
                if (feature == AiFeature.Assistant && value?.limit != null) {
                    Spacer(Modifier.height(12.dp))
                    val fraction = (value.used.toFloat() / value.limit.coerceAtLeast(1)).coerceIn(0f, 1f)
                    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(100.dp)).background(FFColors.iconCircle)) {
                        Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(RoundedCornerShape(100.dp)).background(FFColors.pill))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(MR.strings.pa_ai_used_lifetime.text(value.used, value.limit), style = adText(13.sp, line = 18.sp), color = FFColors.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun AiConsentCard(ai: AiController, openPolicy: () -> Unit, withdraw: () -> Unit) {
    val anyGranted = ai.status?.consent?.values?.any { it } == true
    AdCard(Modifier.testTag("account-ai-consent")) {
        AdCardTitle(MR.strings.ai_privacy_title.text())
        Spacer(Modifier.height(12.dp))
        AdBody(MR.strings.pa_ai_privacy_body.text())
        Spacer(Modifier.height(16.dp))
        AdPillButton(MR.strings.pa_privacy_policy.text(), "hi_shield", openPolicy, Modifier.fillMaxWidth(), tag = "account-privacy-policy")
        Spacer(Modifier.height(8.dp))
        AdPillButton(MR.strings.pa_withdraw_consent.text(), AdIcons.refresh, withdraw, Modifier.fillMaxWidth(), tint = AdTone.danger, border = false,
            enabled = anyGranted && ai.working == null, tag = "account-withdraw-consent")
        ai.working?.let { Spacer(Modifier.height(8.dp)); AdBody(it, size = 13.sp) }
        ai.error?.let { Spacer(Modifier.height(8.dp)); AdBody(it, color = AdTone.danger, size = 13.sp) }
    }
}
