package com.logikawan.finflow.feature.importdata

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.logikawan.finflow.core.i18n.text
import com.logikawan.finflow.core.model.ImportPlan
import com.logikawan.finflow.core.ui.Choice
import com.logikawan.finflow.core.ui.FFColors
import com.logikawan.finflow.core.ui.FinanceCard
import com.logikawan.finflow.core.util.money
import com.logikawan.finflow.i18n.*

@Composable
fun ImportScreen(plan: ImportPlan?, source: String, changeSource: (String) -> Unit, busy: Boolean, progress: String, onPick: () -> Unit, onTemplate: () -> Unit, onImport: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text(MR.strings.ai_import_title.text(), style = MaterialTheme.typography.headlineSmall) }
        item { Text(MR.strings.ai_import_intro.text()) }
        item { Choice(MR.strings.ai_import_source.text(), source, listOf("ivy" to "Ivy Wallet CSV", "finflow" to MR.strings.ai_import_source_template.text()), changeSource) }
        item { OutlinedButton(enabled = !busy, onClick = onTemplate) { Text(MR.strings.ai_import_save_template.text()) } }
        item { Button(enabled = !busy, onClick = onPick) { Text(MR.strings.ai_import_pick.text()) } }
        plan?.let { preview ->
            item { FinanceCard {
                Text(MR.strings.ai_import_preview.text(), style = MaterialTheme.typography.titleLarge)
                Text(MR.strings.ai_import_summary_tx.text(preview.transactions.size, preview.duplicateCount))
                Text(MR.strings.ai_import_summary_new.text(preview.accounts.size, preview.categories.size))
                preview.warnings.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                Text(MR.strings.ai_import_scheduled.text(preview.transactions.count { it.scheduled }))
            } }
            items(preview.errors.take(30)) { Text(it, color = FFColors.expense) }
            if (preview.errors.size > 30) item { Text(MR.strings.ai_import_more_errors.text(preview.errors.size - 30)) }
            items(preview.transactions.take(10), key = { it.id }) { tx -> Text("${tx.date} · ${tx.title} · ${money(tx.amountMinor, tx.currency)}") }
            item { Text(MR.strings.ai_import_source_id_note.text()) }
            item { Button(enabled = !busy && preview.errors.isEmpty() && preview.transactions.isNotEmpty(), onClick = onImport) { Text(if (busy) MR.strings.ai_import_importing.text(progress) else MR.strings.ai_import_button.text(preview.transactions.size)) } }
        }
    }
}
