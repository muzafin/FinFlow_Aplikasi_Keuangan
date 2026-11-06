package com.logikawan.finflow.core.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun FeatureEmpty(icon: String, color: Color, title: String, description: String, action: String, onAction: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(80.dp).background(color.copy(alpha = .1f), RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) { FFIcon(icon, color, 40.dp) }
        Spacer(Modifier.height(20.dp))
        Text(title, style = FFType.sectionTitle)
        Spacer(Modifier.height(8.dp))
        Text(description, style = FFType.bodyMain, color = FFColors.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAction, Modifier.height(52.dp)) { FFIcon("add_rounded", MaterialTheme.colorScheme.onPrimary, 20.dp); Spacer(Modifier.width(8.dp)); Text(action) }
    }
}
