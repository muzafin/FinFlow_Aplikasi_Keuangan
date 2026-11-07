package com.clonical.kasiva.core.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun Field(label: String, value: String, change: (String) -> Unit, modifier: Modifier = Modifier, singleLine: Boolean = true, enabled: Boolean = true, readOnly: Boolean = false, keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = FFType.bodyMain, fontWeight = FontWeight.Medium, color = FFColors.onSurfaceVariant)
        OutlinedTextField(value, change, modifier = Modifier.fillMaxWidth().semantics { contentDescription = label }, singleLine = singleLine, enabled = enabled, readOnly = readOnly, keyboardOptions = keyboardOptions,
            shape = RoundedCornerShape(16.dp), textStyle = FFType.bodyMain,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = FFColors.surface, focusedContainerColor = FFColors.surface,
                unfocusedBorderColor = Color.Transparent, focusedBorderColor = FFColors.primary,
            ))
    }
}

@Composable
fun FinanceCard(onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = FFColors.surface, contentColor = FFColors.onSurface), border = BorderStroke(.5.dp, FFColors.outlineVariant), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}
