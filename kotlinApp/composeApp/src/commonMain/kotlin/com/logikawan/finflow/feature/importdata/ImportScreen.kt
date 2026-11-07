package com.logikawan.finflow.feature.importdata

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logikawan.finflow.core.i18n.text
import com.logikawan.finflow.core.model.ImportPlan
import com.logikawan.finflow.core.ui.FFColors
import com.logikawan.finflow.core.ui.SegmentedTabs
import com.logikawan.finflow.core.util.money
import com.logikawan.finflow.feature.settings.*
import com.logikawan.finflow.i18n.*

/** Impor Transaksi seperti Ollo: banner info, panduan lipat, Unduh Template, Unggah CSV, lalu pratinjau sebelum menyimpan. */
@Composable
fun ImportScreen(plan: ImportPlan?, source: String, changeSource: (String) -> Unit, busy: Boolean, progress: String, onPick: () -> Unit, onTemplate: () -> Unit, onImport: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().testTag("import-data-page"), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { AdBanner("hi_info", MR.strings.pa_import_banner.text(), AdTone.infoBg, AdTone.infoFg) }
        item {
            AdExpandable(MR.strings.pa_import_guide.text(), {
                AdBody(MR.strings.ai_import_intro.text())
                Spacer(Modifier.height(10.dp))
                AdBody(MR.strings.pa_import_columns.text(CsvImport.headers.joinToString(", ")), size = 13.sp)
                Spacer(Modifier.height(10.dp))
                AdBody(MR.strings.ai_import_source_id_note.text(), size = 13.sp)
            }, icon = "hi_help", tag = "import-guide")
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AdFieldLabel(MR.strings.ai_import_source.text())
                SegmentedTabs(listOf("ivy" to "Ivy Wallet CSV", "finflow" to MR.strings.ai_import_source_template.text()), source, changeSource)
            }
        }
        item { AdOutlineButton(MR.strings.pa_import_download_template.text(), "hi_download", onTemplate, enabled = !busy, height = 64.dp, tag = "import-template") }
        item { AdPrimaryButton(MR.strings.pa_import_upload.text(), "hi_upload", onPick, enabled = !busy, height = 64.dp, tag = "import-upload") }
        plan?.let { preview ->
            item {
                AdCard(Modifier.testTag("import-preview")) {
                    AdCardTitle(MR.strings.ai_import_preview.text())
                    Spacer(Modifier.height(10.dp))
                    AdBody(MR.strings.ai_import_summary_tx.text(preview.transactions.size, preview.duplicateCount), color = FFColors.onSurface)
                    AdBody(MR.strings.ai_import_summary_new.text(preview.accounts.size, preview.categories.size), color = FFColors.onSurface)
                    AdBody(MR.strings.ai_import_scheduled.text(preview.transactions.count { it.scheduled }))
                    preview.warnings.forEach { Spacer(Modifier.height(6.dp)); AdBody(it, size = 12.sp) }
                }
            }
            items(preview.errors.take(30)) { AdBody(it, color = AdTone.danger, size = 13.sp) }
            if (preview.errors.size > 30) item { AdBody(MR.strings.ai_import_more_errors.text(preview.errors.size - 30), color = AdTone.danger) }
            if (preview.transactions.isNotEmpty()) item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(FFColors.surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    preview.transactions.take(10).forEach { tx ->
                        Row {
                            Text("${tx.date} · ${tx.title}", Modifier.weight(1f), style = adText(14.sp, line = 19.sp), color = FFColors.onSurface, maxLines = 1)
                            Spacer(Modifier.width(8.dp))
                            Text(money(tx.amountMinor, tx.currency), style = adText(14.sp, FontWeight.Medium, 19.sp), color = FFColors.onSurface)
                        }
                    }
                }
            }
            item {
                AdPrimaryButton(if (busy) MR.strings.ai_import_importing.text(progress) else MR.strings.ai_import_button.text(preview.transactions.size), "hi_check",
                    onImport, enabled = !busy && preview.errors.isEmpty() && preview.transactions.isNotEmpty(), tag = "import-confirm")
            }
        }
    }
}
