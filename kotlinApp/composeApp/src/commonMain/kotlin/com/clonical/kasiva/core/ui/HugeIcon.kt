package com.clonical.kasiva.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val hugeIconCache = mutableMapOf<String, ImageVector>()

// Ikon garis Hugeicons (keluarga ikon yang dipakai Ollo): viewport 24, stroke 1.5, ujung bulat.
internal fun hugeIconVector(name: String, stroke: Float = 1.5f): ImageVector? {
    val key = if (stroke == 1.5f) name else "$name@$stroke"
    hugeIconCache[key]?.let { return it }
    val paths = hugeIconPaths[name] ?: return null
    val builder = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
    paths.forEach { (data, filled) ->
        val nodes = PathParser().parsePathString(data).toNodes()
        if (filled) builder.addPath(nodes, fill = SolidColor(Color.Black))
        else builder.addPath(nodes, stroke = SolidColor(Color.Black), strokeLineWidth = stroke, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
    }
    return builder.build().also { hugeIconCache[key] = it }
}

@Composable
internal fun HugeIcon(name: String, tint: Color, size: Dp, contentDescription: String? = null, stroke: Float = 1.5f) {
    val vector = remember(name, stroke) { hugeIconVector(name, stroke) } ?: return
    Image(vector, null, Modifier.size(size).then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier.clearAndSetSemantics {}), colorFilter = ColorFilter.tint(tint))
}
