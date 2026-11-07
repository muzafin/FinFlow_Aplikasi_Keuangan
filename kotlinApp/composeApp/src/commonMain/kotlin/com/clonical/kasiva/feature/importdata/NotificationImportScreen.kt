package com.clonical.kasiva.feature.importdata

import androidx.compose.runtime.Composable
import com.clonical.kasiva.core.model.FinanceData
import com.clonical.kasiva.core.model.Transaction

@Composable
expect fun NotificationImportScreen(data: FinanceData, onReview: (Transaction, () -> Unit) -> Unit, onOpenCsv: () -> Unit)
