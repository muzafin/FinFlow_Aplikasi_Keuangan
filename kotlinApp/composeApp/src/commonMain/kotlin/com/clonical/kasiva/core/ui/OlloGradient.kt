package com.clonical.kasiva.core.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

// Gradien kartu saldo Ollo (diukur dari piksel): kiri-bawah hampir hitam, tengah biru, kanan-atas hijau toska.
fun DrawScope.drawOlloBalanceGradient(dark: Boolean = true) {
    if (!dark) {
        // Mode terang Ollo: biru lembut → toska, tanpa sudut gelap (diukur: #4892DF → #4ED6C5).
        drawRect(Brush.linearGradient(listOf(Color(0xFF4892DF), Color(0xFF4AAED4), Color(0xFF4ED6C5)), Offset(0f, 0f), Offset(size.width, size.height * .6f)))
        return
    }
    drawRect(Brush.linearGradient(listOf(Color(0xFF0B2533), Color(0xFF1477AE), Color(0xFF1BAEF5), Color(0xFF19C9EC)),
        Offset(0f, size.height * .55f), Offset(size.width, size.height * .45f)))
    drawRect(Brush.radialGradient(listOf(Color(0xFF17F0D4), Color(0x0017F0D4)), Offset(size.width, 0f), size.width * .55f))
    drawRect(Brush.radialGradient(listOf(Color(0xF0050A0D), Color(0x00050A0D)), Offset(0f, size.height), size.width * .6f))
}

fun Modifier.olloBalanceGradient(dark: Boolean = true): Modifier = drawBehind { drawOlloBalanceGradient(dark) }
