package com.clonical.kasiva.feature.importdata

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.model.FinanceData
import com.clonical.kasiva.core.model.Transaction
import com.clonical.kasiva.i18n.*

@Composable
actual fun NotificationImportScreen(data: FinanceData, onReview: (Transaction, () -> Unit) -> Unit, onOpenCsv: () -> Unit) {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(MR.strings.ai_notif_ios_title.text(), style = MaterialTheme.typography.headlineMedium)
        Text(MR.strings.ai_notif_ios_body.text())
        Button(onClick = onOpenCsv) { Text(MR.strings.ai_import_csv.text()) }
    }
}
