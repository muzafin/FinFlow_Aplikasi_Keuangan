package com.logikawan.finflow.core.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

// Nominal tetap lengkap dan satu baris: mengecil sesuai ruang, tanpa elipsis/terpotong.
@Composable
fun FitMoneyText(value: String, style: TextStyle = FFType.amountMd, color: Color = FFColors.onSurface, textAlign: TextAlign = TextAlign.Start, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val effectiveStyle = LocalTextStyle.current.merge(style)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width = with(density) { maxWidth.toPx() }
        val start = effectiveStyle.fontSize.value
        var size = start
        while (size > 8f && measurer.measure(value, effectiveStyle.copy(fontSize = size.sp, lineHeight = (size * 1.25f).sp), softWrap = false).size.width > width) {
            size -= 0.5f
        }
        Text(value, modifier = modifier.fillMaxWidth(), style = effectiveStyle.copy(fontSize = size.sp, lineHeight = (size * 1.25f).sp), color = color, textAlign = textAlign, maxLines = 1, softWrap = false)
    }
}
