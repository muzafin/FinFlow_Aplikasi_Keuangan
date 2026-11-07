package com.logikawan.finflow.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.logikawan.finflow.i18n.*
import com.logikawan.finflow.core.i18n.text

@Composable
fun MonthField(value: String, change: (String) -> Unit) {
    var input by remember(value) { mutableStateOf(value) }
    Field(MR.strings.app_month_field.text(), input, {
        input = it
        if (Regex("\\d{4}-(0[1-9]|1[0-2])").matches(it)) change(it)
    })
}
