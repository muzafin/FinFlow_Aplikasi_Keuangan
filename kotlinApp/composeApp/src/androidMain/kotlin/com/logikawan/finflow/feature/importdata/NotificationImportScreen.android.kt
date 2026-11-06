package com.logikawan.finflow.feature.importdata

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
import com.logikawan.finflow.core.model.FinanceData
import com.logikawan.finflow.core.model.Transaction
import com.logikawan.finflow.core.ui.Choice
import com.logikawan.finflow.core.ui.FFColors
import com.logikawan.finflow.core.ui.FinanceCard
import com.logikawan.finflow.core.ui.Field
import com.logikawan.finflow.core.util.displayMoney
import com.logikawan.finflow.core.util.nowMillis
import com.logikawan.finflow.notificationimport.NotificationCandidate
import com.logikawan.finflow.notificationimport.NotificationImportStore
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
        catch (_: Exception) { error = "Pengaturan akses notifikasi tidak tersedia pada perangkat ini" }
    }
    fun review(candidate: NotificationCandidate) {
        val account = accounts.find { it.id == candidate.accountId && it.currency == candidate.currency }
            ?: accounts.firstOrNull { it.currency == candidate.currency }
        if (account == null) { error = "Tambahkan dompet aktif ${candidate.currency} terlebih dahulu"; return }
        val category = data.categories.firstOrNull { it.type == candidate.type }
        if (category == null) { error = "Tambahkan kategori ${candidate.type} terlebih dahulu"; return }
        val date = Instant.ofEpochMilli(candidate.postedAt).atZone(ZoneId.systemDefault()).toLocalDate().toString()
        val draft = Transaction(id = "notification-${candidate.id}", type = candidate.type, amountMinor = candidate.amountMinor,
            currency = candidate.currency, accountId = account.id, categoryId = category.id, title = "Transaksi dari notifikasi",
            date = date, createdAt = nowMillis(), source = "notification", sourceId = candidate.id)
        onReview(draft) { store.dismiss(candidate.id) }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Impor transaksi bank/e-wallet", style = MaterialTheme.typography.headlineMedium) }
        item { Disclosure() }
        if (!accepted) {
            item {
                Row {
                    Checkbox(consent, { consent = it })
                    Text("Saya memahami dan menyetujui akses untuk fitur impor transaksi ini.", Modifier.weight(1f))
                }
                Button(enabled = consent && data.profile?.id != null, onClick = { store.acceptDisclosure() }, modifier = Modifier.fillMaxWidth()) { Text("Setujui & pilih aplikasi") }
                TextButton(onClick = onOpenCsv) { Text("Gunakan CSV") }
            }
        } else {
            item { FinanceCard {
                Text(if (enabled && granted) "Impor notifikasi aktif" else "Impor notifikasi belum aktif", style = MaterialTheme.typography.titleMedium)
                Text("Izin Android: ${if (granted) "diberikan" else "belum diberikan"}")
                if (!enabled) Button(enabled = sources.isNotEmpty(), onClick = { store.setEnabled(true); settings() }) { Text("Aktifkan & buka pengaturan Android") }
                else {
                    if (!granted) Button(onClick = ::settings) { Text("Buka akses notifikasi Android") }
                    OutlinedButton(onClick = { store.setEnabled(false) }) { Text("Nonaktifkan pemrosesan") }
                }
                TextButton(onClick = ::settings) { Text("Kelola/cabut izin Android") }
                Text("Izin sistem mencakup akses notifikasi umum. FinFlow memeriksa pilihan aplikasi sebelum membaca isinya. Nonaktifkan pemrosesan di sini, atau cabut izin melalui Android.", style = MaterialTheme.typography.bodySmall)
            } }
            item { FinanceCard {
                Text("Pilih sumber dan dompet", style = MaterialTheme.typography.titleMedium)
                Text("Pilih hanya aplikasi bank/e-wallet milikmu. Daftar aplikasi tetap di perangkat.", style = MaterialTheme.typography.bodySmall)
                Field("Cari aplikasi bank/e-wallet", query, { query = it; selectedApp = "" })
                val choices = apps.filter { it.second.contains(query, true) || it.first.contains(query, true) }.take(50)
                Choice("Aplikasi", selectedApp, choices.map { it.first to "${it.second} (${it.first})" }, { selectedApp = it })
                Choice("Dompet tujuan pencatatan", selectedAccount, accounts.map { it.id to "${it.name} (${it.currency})" }, { selectedAccount = it })
                if (accounts.isEmpty()) Text("Buat dompet terlebih dahulu pada tab Dompet.")
                Button(enabled = apps.any { it.first == selectedApp } && accounts.any { it.id == selectedAccount }, onClick = {
                    val account = accounts.first { it.id == selectedAccount }
                    store.selectSource(selectedApp, account.id, account.currency)
                    selectedApp = ""
                }) { Text("Simpan sumber") }
                Text("Maksimal 50 hasil ditampilkan; gunakan pencarian. Pemetaan dompet adalah nilai awal untuk pratinjau, bukan verifikasi rekening bank.", style = MaterialTheme.typography.bodySmall)
            } }
            items(sources, key = { "source-${it.packageName}" }) { source -> FinanceCard {
                Text(apps.find { it.first == source.packageName }?.second ?: source.packageName, style = MaterialTheme.typography.titleMedium)
                Text("${data.accountName(source.accountId)} · ${source.currency}")
                TextButton(onClick = { store.removeSource(source.packageName) }) { Text("Hapus sumber") }
            } }
            item {
                Text("Kandidat transaksi (${candidates.size})", style = MaterialTheme.typography.titleLarge)
                Text("Kandidat belum memengaruhi saldo. Periksa nominal, arah, tanggal dan dompet sebelum menyimpan. Transfer antar-dompet milikmu dapat muncul dari dua aplikasi; catat satu transfer dan abaikan pasangannya.", style = MaterialTheme.typography.bodySmall)
                Text("Format awal: nominal dengan kode/simbol mata uang yang jelas, pesan transaksi selesai berbahasa Indonesia/Inggris. Notifikasi ambigu, OTP, promo, atau konten yang disembunyikan Android tidak diimpor.", style = MaterialTheme.typography.bodySmall)
            }
            if (candidates.isEmpty()) item { Text("Belum ada kandidat. Hanya notifikasi baru setelah aktivasi yang diproses; kelengkapan bergantung pada isi notifikasi aplikasi sumber.") }
            items(candidates, key = { "candidate-${it.id}" }) { candidate -> FinanceCard {
                val existing = data.transactions.any { it.id == "notification-${candidate.id}" }
                val date = Instant.ofEpochMilli(candidate.postedAt).atZone(ZoneId.systemDefault()).toLocalDate().toString()
                val possibleDuplicate = data.transactions.any { it.date == date && it.type == candidate.type && it.amountMinor == candidate.amountMinor && it.currency == candidate.currency && it.accountId == candidate.accountId }
                Text(candidate.appName, style = MaterialTheme.typography.titleMedium)
                Text((if (candidate.type == "income") "Pemasukan " else "Pengeluaran ") + displayMoney(candidate.amountMinor, candidate.currency), color = if (candidate.type == "income") FFColors.income else FFColors.expense)
                Text("$date · ${data.accountName(candidate.accountId)}")
                if (existing) Text("Sudah diimpor. Kandidat dapat diabaikan.")
                else if (possibleDuplicate) Text("Ada transaksi dengan tanggal, dompet dan nominal sama. Periksa kemungkinan duplikat.", color = FFColors.expense)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = !existing, onClick = { review(candidate) }) { Text("Tinjau") }
                    TextButton(onClick = { store.dismiss(candidate.id) }) { Text("Abaikan") }
                }
            } }
            item {
                Text("Kandidat disimpan lokal, maksimal 300 item. Data berusia lebih dari 30 hari dibersihkan saat fitur dibaca/digunakan. Isi notifikasi asli tidak disimpan.", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { clear = true }) { Text("Hapus semua kandidat lokal") }
                TextButton(onClick = { revoke = true }) { Text("Cabut persetujuan & hapus pengaturan impor") }
                TextButton(onClick = onOpenCsv) { Text("Impor CSV") }
            }
        }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
    }
    if (clear || revoke) AlertDialog(onDismissRequest = { clear = false; revoke = false },
        title = { Text(if (revoke) "Cabut persetujuan impor?" else "Hapus kandidat lokal?") },
        text = { Text("Kandidat yang belum disimpan akan dihapus. Transaksi yang sudah dikonfirmasi di akun Firebase tetap ada. Untuk mencabut izin sistem, gunakan pengaturan Android.") },
        confirmButton = { TextButton(onClick = {
            if (revoke) { store.revokeConsent(); consent = false } else store.clearCandidates()
            clear = false; revoke = false
        }) { Text("Lanjutkan") } }, dismissButton = { TextButton(onClick = { clear = false; revoke = false }) { Text("Batal") } })
}

@Composable
private fun Disclosure() {
    FinanceCard {
        Text("Persetujuan akses notifikasi", style = MaterialTheme.typography.titleMedium)
        Text("FinFlow menggunakan akses notifikasi Android untuk menemukan kandidat pemasukan dan pengeluaran dari aplikasi bank/e-wallet yang kamu pilih, termasuk ketika FinFlow tidak sedang dibuka.")
        Text("Android memberikan akses notifikasi umum. FinFlow menyaring aplikasi pilihan sebelum membaca isi pesannya. OTP, PIN, password, promo dan pesan yang tidak dapat dikenali sebagai transaksi diabaikan.")
        Text("Daftar aplikasi dan kandidat diproses lokal di perangkat. Isi notifikasi asli tidak disimpan atau dikirim. Hanya data transaksi yang kamu tinjau dan konfirmasi yang dikirim ke Firebase akunmu untuk pencatatan/sinkronisasi.")
        Text("Fitur opsional; pencatatan manual dan CSV tetap bisa dipakai tanpa izin ini. Kamu dapat menghentikan pemrosesan, menghapus kandidat lokal, serta mencabut izin pada pengaturan Android.")
    }
}
