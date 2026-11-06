package com.logikawan.finflow.core.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

// Gradien kartu saldo Ollo (diukur dari piksel): kiri-bawah hampir hitam, tengah biru, kanan-atas hijau toska.
fun DrawScope.drawOlloBalanceGradient() {
    drawRect(Brush.linearGradient(listOf(Color(0xFF0B2533), Color(0xFF1477AE), Color(0xFF1BAEF5), Color(0xFF19C9EC)),
        Offset(0f, size.height * .55f), Offset(size.width, size.height * .45f)))
    drawRect(Brush.radialGradient(listOf(Color(0xFF17F0D4), Color(0x0017F0D4)), Offset(size.width, 0f), size.width * .55f))
    drawRect(Brush.radialGradient(listOf(Color(0xF0050A0D), Color(0x00050A0D)), Offset(0f, size.height), size.width * .6f))
}

fun Modifier.olloBalanceGradient(): Modifier = drawBehind { drawOlloBalanceGradient() }
