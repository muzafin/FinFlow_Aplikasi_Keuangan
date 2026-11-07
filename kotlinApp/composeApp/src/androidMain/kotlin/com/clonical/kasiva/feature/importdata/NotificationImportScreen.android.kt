package com.clonical.kasiva.feature.importdata

import android.content.Intent
import android.content.SharedPreferences
import android.provider.Settings
import android.view.ViewTreeObserver
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.model.FinanceData
import com.clonical.kasiva.core.model.Transaction
import com.clonical.kasiva.core.ui.Choice
import com.clonical.kasiva.core.ui.FFColors
import com.clonical.kasiva.core.ui.FinanceCard
import com.clonical.kasiva.core.ui.Field
import com.clonical.kasiva.core.util.displayMoney
import com.clonical.kasiva.core.util.nowMillis
import com.clonical.kasiva.i18n.*
import com.clonical.kasiva.notificationimport.NotificationCandidate
import com.clonical.kasiva.notificationimport.NotificationImportStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

@Composable
actual fun NotificationImportScreen(data: FinanceData, onReview: (Transaction, () -> Unit) -> Unit, onOpenCsv: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val store = remember { NotificationImportStore(context) }
    var revision by remember { mutableIntStateOf(0) }
    var consent by rememberSaveable { mutableStateOf(false) }
    var apps by remember { mutableStateOf(emptyList<Pair<String, String>>()) }
    var candidates by remember { mutableStateOf(emptyList<NotificationCandidate>()) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedApp by rememberSaveable { mutableStateOf("") }
    var selectedAccount by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var revoke by remember { mutableStateOf(false) }
    var clear by remember { mutableStateOf(false) }
    val accepted = remember(revision) { store.disclosureAccepted() }
    val enabled = remember(revision) { store.enabled() }
    val granted = remember(revision) { NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName) }
    val sources = remember(revision) { store.sources() }
    val accounts = data.accounts.filterNot { it.archived }
    DisposableEffect(view, store) {
        val changed = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> revision++ }
        val focused = ViewTreeObserver.OnWindowFocusChangeListener { focused -> if (focused) revision++ }
        store.preferences.registerOnSharedPreferenceChangeListener(changed)
        view.viewTreeObserver.addOnWindowFocusChangeListener(focused)
        onDispose {
            store.preferences.unregisterOnSharedPreferenceChangeListener(changed)
            if (view.viewTreeObserver.isAlive) view.viewTreeObserver.removeOnWindowFocusChangeListener(focused)
        }
    }
    LaunchedEffect(accepted) {
        apps = if (!accepted) emptyList() else withContext(Dispatchers.IO) {
            val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            context.packageManager.queryIntentActivities(launcher, 0).mapNotNull { result ->
                result.activityInfo?.packageName?.takeIf { it != context.packageName }?.let { it to result.loadLabel(context.packageManager).toString() }
            }.distinctBy { it.first }.sortedBy { it.second.lowercase() }
        }
    }
    LaunchedEffect(revision, accepted) { candidates = if (accepted) withContext(Dispatchers.IO) { store.candidates() } else emptyList() }
    fun settings() {
        try { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        catch (_: Exception) { error = MR.strings.ai_notif_settings_unavailable.text() }
    }
    fun review(candidate: NotificationCandidate) {
        val account = accounts.find { it.id == candidate.accountId && it.currency == candidate.currency }
            ?: accounts.firstOrNull { it.currency == candidate.currency }
        if (account == null) { error = MR.strings.ai_notif_add_wallet_first.text(candidate.currency); return }
        val category = data.categories.firstOrNull { it.type == candidate.type }
        if (category == null) { error = MR.strings.ai_notif_add_category_first.text(candidate.type); return }
        val date = Instant.ofEpochMilli(candidate.postedAt).atZone(ZoneId.systemDefault()).toLocalDate().toString()
        val draft = Transaction(id = "notification-${candidate.id}", type = candidate.type, amountMinor = candidate.amountMinor,
            currency = candidate.currency, accountId = account.id, categoryId = category.id, title = MR.strings.ai_notif_draft_title.text(),
            date = date, createdAt = nowMillis(), source = "notification", sourceId = candidate.id)
        onReview(draft) { store.dismiss(candidate.id) }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text(MR.strings.ai_notif_title.text(), style = MaterialTheme.typography.headlineMedium) }
        item { Disclosure() }
        if (!accepted) {
            item {
                Row {
                    Checkbox(consent, { consent = it })
                    Text(MR.strings.ai_notif_consent_check.text(), Modifier.weight(1f))
                }
                Button(enabled = consent && data.profile?.id != null, onClick = { store.acceptDisclosure() }, modifier = Modifier.fillMaxWidth()) { Text(MR.strings.ai_notif_accept.text()) }
                TextButton(onClick = onOpenCsv) { Text(MR.strings.ai_notif_use_csv.text()) }
            }
        } else {
            item { FinanceCard {
                Text(if (enabled && granted) MR.strings.ai_notif_active.text() else MR.strings.ai_notif_inactive.text(), style = MaterialTheme.typography.titleMedium)
                Text(if (granted) MR.strings.ai_notif_permission_granted.text() else MR.strings.ai_notif_permission_not_granted.text())
                if (!enabled) Button(enabled = sources.isNotEmpty(), onClick = { store.setEnabled(true); settings() }) { Text(MR.strings.ai_notif_enable_open_settings.text()) }
                else {
                    if (!granted) Button(onClick = ::settings) { Text(MR.strings.ai_notif_open_access.text()) }
                    OutlinedButton(onClick = { store.setEnabled(false) }) { Text(MR.strings.ai_notif_disable.text()) }
                }
                TextButton(onClick = ::settings) { Text(MR.strings.ai_notif_manage_permission.text()) }
                Text(MR.strings.ai_notif_permission_note.text(), style = MaterialTheme.typography.bodySmall)
            } }
            item { FinanceCard {
                Text(MR.strings.ai_notif_pick_source_title.text(), style = MaterialTheme.typography.titleMedium)
                Text(MR.strings.ai_notif_pick_source_caption.text(), style = MaterialTheme.typography.bodySmall)
                Field(MR.strings.ai_notif_search_app.text(), query, { query = it; selectedApp = "" })
                val choices = apps.filter { it.second.contains(query, true) || it.first.contains(query, true) }.take(50)
                Choice(MR.strings.ai_notif_app.text(), selectedApp, choices.map { it.first to "${it.second} (${it.first})" }, { selectedApp = it })
                Choice(MR.strings.ai_notif_target_wallet.text(), selectedAccount, accounts.map { it.id to "${it.name} (${it.currency})" }, { selectedAccount = it })
                if (accounts.isEmpty()) Text(MR.strings.ai_notif_create_wallet_first.text())
                Button(enabled = apps.any { it.first == selectedApp } && accounts.any { it.id == selectedAccount }, onClick = {
                    val account = accounts.first { it.id == selectedAccount }
                    store.selectSource(selectedApp, account.id, account.currency)
                    selectedApp = ""
                }) { Text(MR.strings.ai_notif_save_source.text()) }
                Text(MR.strings.ai_notif_results_note.text(), style = MaterialTheme.typography.bodySmall)
            } }
            items(sources, key = { "source-${it.packageName}" }) { source -> FinanceCard {
                Text(apps.find { it.first == source.packageName }?.second ?: source.packageName, style = MaterialTheme.typography.titleMedium)
                Text("${data.accountName(source.accountId)} · ${source.currency}")
                TextButton(onClick = { store.removeSource(source.packageName) }) { Text(MR.strings.ai_notif_remove_source.text()) }
            } }
            item {
                Text(MR.strings.ai_notif_candidates_title.text(candidates.size), style = MaterialTheme.typography.titleLarge)
                Text(MR.strings.ai_notif_candidates_note.text(), style = MaterialTheme.typography.bodySmall)
                Text(MR.strings.ai_notif_format_note.text(), style = MaterialTheme.typography.bodySmall)
            }
            if (candidates.isEmpty()) item { Text(MR.strings.ai_notif_no_candidates.text()) }
            items(candidates, key = { "candidate-${it.id}" }) { candidate -> FinanceCard {
                val existing = data.transactions.any { it.id == "notification-${candidate.id}" }
                val date = Instant.ofEpochMilli(candidate.postedAt).atZone(ZoneId.systemDefault()).toLocalDate().toString()
                val possibleDuplicate = data.transactions.any { it.date == date && it.type == candidate.type && it.amountMinor == candidate.amountMinor && it.currency == candidate.currency && it.accountId == candidate.accountId }
                Text(candidate.appName, style = MaterialTheme.typography.titleMedium)
                Text(if (candidate.type == "income") MR.strings.ai_notif_income_amount.text(displayMoney(candidate.amountMinor, candidate.currency)) else MR.strings.ai_notif_expense_amount.text(displayMoney(candidate.amountMinor, candidate.currency)), color = if (candidate.type == "income") FFColors.income else FFColors.expense)
                Text("$date · ${data.accountName(candidate.accountId)}")
                if (existing) Text(MR.strings.ai_notif_already_imported.text())
                else if (possibleDuplicate) Text(MR.strings.ai_notif_possible_duplicate.text(), color = FFColors.expense)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = !existing, onClick = { review(candidate) }) { Text(MR.strings.ai_notif_review.text()) }
                    TextButton(onClick = { store.dismiss(candidate.id) }) { Text(MR.strings.ai_notif_ignore.text()) }
                }
            } }
            item {
                Text(MR.strings.ai_notif_storage_note.text(), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { clear = true }) { Text(MR.strings.ai_notif_clear_all.text()) }
                TextButton(onClick = { revoke = true }) { Text(MR.strings.ai_notif_revoke_all.text()) }
                TextButton(onClick = onOpenCsv) { Text(MR.strings.ai_import_csv.text()) }
            }
        }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
    }
    if (clear || revoke) AlertDialog(onDismissRequest = { clear = false; revoke = false },
        title = { Text(if (revoke) MR.strings.ai_notif_revoke_title.text() else MR.strings.ai_notif_clear_title.text()) },
        text = { Text(MR.strings.ai_notif_clear_body.text()) },
        confirmButton = { TextButton(onClick = {
            if (revoke) { store.revokeConsent(); consent = false } else store.clearCandidates()
            clear = false; revoke = false
        }) { Text(MR.strings.ai_continue.text()) } }, dismissButton = { TextButton(onClick = { clear = false; revoke = false }) { Text(MR.strings.common_cancel.text()) } })
}

@Composable
private fun Disclosure() {
    FinanceCard {
        Text(MR.strings.ai_notif_disclosure_title.text(), style = MaterialTheme.typography.titleMedium)
        Text(MR.strings.ai_notif_disclosure_1.text())
        Text(MR.strings.ai_notif_disclosure_2.text())
        Text(MR.strings.ai_notif_disclosure_3.text())
        Text(MR.strings.ai_notif_disclosure_4.text())
    }
}
