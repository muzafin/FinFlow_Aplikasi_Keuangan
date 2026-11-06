package com.logikawan.finflow.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*

@Composable
fun MonthField(value: String, change: (String) -> Unit) {
    var input by remember(value) { mutableStateOf(value) }
    Field("Bulan (YYYY-MM)", input, {
        input = it
        if (Regex("\\d{4}-(0[1-9]|1[0-2])").matches(it)) change(it)
    })
}
