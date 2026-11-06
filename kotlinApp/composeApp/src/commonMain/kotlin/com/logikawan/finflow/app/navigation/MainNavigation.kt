package com.logikawan.finflow.app.navigation

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.*
import com.kyant.backdrop.isRenderEffectSupported
import com.kyant.backdrop.shadow.InnerShadow
import com.logikawan.finflow.core.ui.*
import kotlin.math.roundToInt

val mainPages = listOf("Beranda", "Statistik", "Dompet", "Profil")

// Efek memakai Backdrop Kyant0; posisi dan komponen sama pada seluruh tab.
@Composable
fun MainNavigation(page: String, navigate: (String) -> Unit, add: () -> Unit, enabled: Boolean,
    backdrop: Backdrop? = null) {
    val primary = FFColors.primary
    val background = FFColors.background
    val glass = FFColors.canvas
    val sourceBackdrop = backdrop ?: rememberCanvasBackdrop { drawRect(background) }
    val selected = mainPages.indexOf(page).coerceAtLeast(0)
    val shape = RoundedCornerShape(50)
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    var dragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(selected.toFloat()) }
    val position by animateFloatAsState(if (dragging) dragPosition else selected.toFloat(),
        animationSpec = if (dragging) snap() else spring(dampingRatio = .8f, stiffness = 500f), label = "glass-selection")
    val press by animateFloatAsState(if (dragging || pressed) 1f else 0f, spring(stiffness = 600f), label = "glass-press")
    val tint = if (isRenderEffectSupported()) .44f else .92f
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val currentNavigate by rememberUpdatedState(navigate)

    Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 20.dp, top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        BoxWithConstraints(Modifier.weight(1f).height(64.dp).testTag("navigation-glass")
            .semantics { stateDescription = if (dragging) "dragging" else "idle" }) {
            val tabWidth = (maxWidth - 8.dp) / mainPages.size
            val widthPx = with(LocalDensity.current) { tabWidth.toPx() }
            Box(Modifier.fillMaxSize().drawBackdrop(sourceBackdrop, { shape }, effects = {
                vibrancy(); blur(10.dp.toPx()); lens(24.dp.toPx(), 18.dp.toPx(), depthEffect = true)
            }, onDrawSurface = { drawRect(glass.copy(alpha = tint)) }))
            Box(Modifier.padding(4.dp).offset(x = tabWidth * if (isLtr) position else 3f - position)
                .width(tabWidth).height(56.dp)
                .graphicsLayer { scaleX = 1f + .1f * press; scaleY = 1f + .12f * press }
                .drawBackdrop(sourceBackdrop, { shape }, effects = {
                    vibrancy(); blur(4.dp.toPx()); lens(12.dp.toPx(), (8 + 10 * press).dp.toPx(), chromaticAberration = true)
                }, innerShadow = { InnerShadow(radius = 6.dp, alpha = .12f) },
                    onDrawSurface = {
                        drawRect(glass.copy(alpha = .28f + .12f * press))
                        drawRect(primary.copy(alpha = .06f))
                    }))
            Row(Modifier.fillMaxSize().padding(4.dp)
                .pointerInput(widthPx, isLtr) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            dragPosition = ((offset.x / widthPx) - .5f).let { if (isLtr) it else 3f - it }.coerceIn(0f, 3f)
                            dragging = true
                        },
                        onDrag = { change, delta ->
                            change.consume()
                            dragPosition = (dragPosition + delta.x / widthPx * if (isLtr) 1f else -1f).coerceIn(0f, 3f)
                        },
                        onDragEnd = {
                            currentNavigate(mainPages[dragPosition.roundToInt().coerceIn(0, 3)])
                            dragging = false
                        },
                        onDragCancel = { dragging = false },
                    )
                }, verticalAlignment = Alignment.CenterVertically) {
                mainPages.forEachIndexed { index, item ->
                    val active = index == selected
                    val icon = when (item) { "Beranda" -> "home_outlined"; "Statistik" -> "bar_chart_rounded"; "Dompet" -> "account_balance_wallet_outlined"; else -> "person_outline_rounded" }
                    Column(Modifier.weight(1f).fillMaxHeight()
                        .clickable(interactionSource = interactions, indication = null, role = Role.Tab) { navigate(item) }
                        .testTag("tab-$item").semantics { this.selected = active },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        FFIcon(icon, if (active) FFColors.primary else FFColors.onSurfaceVariant, 22.dp)
                        Spacer(Modifier.height(4.dp))
                        Text(item, style = FFType.badgeLabel.copy(fontSize = 12.sp, lineHeight = 14.sp, letterSpacing = 0.sp),
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, color = if (active) FFColors.primary else FFColors.onSurfaceVariant)
                    }
                }
            }
        }
        Box(Modifier.size(56.dp).drawBackdrop(sourceBackdrop, { shape }, effects = {
            vibrancy(); blur(10.dp.toPx()); lens(24.dp.toPx(), 20.dp.toPx(), depthEffect = true)
        }, onDrawSurface = { drawRect(primary.copy(alpha = .24f)); drawRect(glass.copy(alpha = tint * .5f)) })
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = add)
            .testTag("add-transaction").semantics { contentDescription = "Tambah transaksi" }, contentAlignment = Alignment.Center) {
            FFIcon("add_rounded", FFColors.primary, 24.dp)
        }
    }
}


// Halaman AI & Premium memakai top bar sendiri (seperti Ollo).
val aiPages = setOf("Asisten", "Pengaturan Asisten", "Pengaturan AI Scan", "Premium")
