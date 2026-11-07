package com.logikawan.finflow.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logikawan.finflow.i18n.*
import com.logikawan.finflow.core.i18n.text

val designBackground: Brush @Composable get() = Brush.verticalGradient(listOf(FFColors.background, FFColors.background))
val balanceGradient = Brush.horizontalGradient(listOf(Color(0xFF0D9488), Color(0xFF0EA5E9)))

@Composable
fun SegmentedTabs(options: List<Pair<String,String>>, selected: String, select: (String)->Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(FFColors.surface).padding(4.dp)) {
        options.forEach{(value,label)->
            Box(Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(if(value==selected)(if(FFColors.background.luminance()<.5f)FFColors.chip else FFColors.canvas) else Color.Transparent)
                .clickable(role=Role.Tab){select(value)}.padding(vertical=10.dp),contentAlignment=Alignment.Center){
                Text(label,style=FFType.metaData,fontWeight=FontWeight.Normal,color=if(value==selected)FFColors.onSurface else FFColors.onSurfaceVariant,maxLines=1)
            }
        }
    }
}

@Composable
fun DesignHeader(title: String, subtitle: String? = null, back: (() -> Unit)? = null, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        if (back != null) {
            IconButton(onClick = back) { FFIcon("chevron_left_rounded", FFColors.onSurface, 24.dp, MR.strings.common_back.text()) }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = FFType.appTitle)
            subtitle?.let { Text(it, style = FFType.metaData, color = FFColors.onSurfaceVariant) }
        }
        action?.invoke()
    }
}

@Composable
fun DesignCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(modifier.fillMaxWidth().clip(shape).background(FFColors.surface)
        .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier).padding(20.dp), content = content)
}

@Composable
fun DesignPill(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(100.dp)
    Text(label, modifier.clip(shape).background(if (selected) FFColors.primary else FFColors.surface)
        .border(1.dp, if (selected) FFColors.primary else FFColors.outlineVariant, shape)
        .clickable(role = Role.Tab, onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp),
        style = FFType.badgeLabel, fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
        color = if (selected) MaterialTheme.colorScheme.onPrimary else FFColors.onSurfaceVariant)
}

@Composable
fun DesignProgressRing(progress: Float, modifier: Modifier = Modifier, caption: String = MR.strings.app_used_caps.text(), color: Color = FFColors.primary) {
    val track = FFColors.outlineVariant
    Box(modifier.size(90.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize().padding(4.dp)) {
            val stroke = androidx.compose.ui.graphics.drawscope.Stroke(8.dp.toPx())
            drawCircle(track, style = stroke)
            drawArc(color, -90f, progress.coerceIn(0f, 1f) * 360, false, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${(progress * 100).toInt()}%", style = FFType.appTitle)
            if (caption.isNotEmpty()) Text(caption, style = FFType.badgeLabel.copy(fontSize = 9.sp), color = FFColors.outline)
        }
    }
}
