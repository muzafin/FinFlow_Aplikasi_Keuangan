package com.logikawan.finflow.feature.importdata

import androidx.compose.runtime.Composable
import com.logikawan.finflow.core.model.FinanceData
import com.logikawan.finflow.core.model.Transaction

@Composable
expect fun NotificationImportScreen(data: FinanceData, onReview: (Transaction, () -> Unit) -> Unit, onOpenCsv: () -> Unit)
