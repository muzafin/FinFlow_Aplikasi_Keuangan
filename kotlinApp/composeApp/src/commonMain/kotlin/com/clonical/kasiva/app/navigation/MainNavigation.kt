package com.clonical.kasiva.app.navigation

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
import com.clonical.kasiva.core.ui.*
import com.clonical.kasiva.core.ui.liquid.LiquidBottomTab
import com.clonical.kasiva.core.ui.liquid.LiquidBottomTabs
import com.clonical.kasiva.core.ui.liquid.LiquidGlass
import kotlin.math.roundToInt
import com.clonical.kasiva.i18n.*
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.i18n.tabLabel

val mainPages = listOf("Beranda", "Statistik", "Dompet", "Profil")

// Navigasi liquid glass bergaya Convx/iOS: bilah kaca buram, indikator yang bisa digeser dan
// membesar menjadi lensa saat ditekan/digeser (komponen LiquidBottomTabs, Kyant0 Apache-2.0).
@Composable
fun MainNavigation(page: String, navigate: (String) -> Unit, add: () -> Unit, enabled: Boolean,
    backdrop: Backdrop? = null) {
    val background = FFColors.background
    val sourceBackdrop = backdrop ?: rememberCanvasBackdrop { drawRect(background) }
    val selected = mainPages.indexOf(page).coerceAtLeast(0)
    val currentNavigate by rememberUpdatedState(navigate)
    val currentPage by rememberUpdatedState(page)
    // Lambda harus stabil: LiquidBottomTabs mengamati nilainya lewat snapshotFlow.
    val selectedState = rememberUpdatedState(selected)
    val selectedProvider = remember { { selectedState.value } }
    var dragging by remember { mutableStateOf(false) }
    val containerColor = if (FFColors.isDark) Color(0xFF121212).copy(LiquidGlass.DARK_TINT) else Color.White.copy(LiquidGlass.LIGHT_TINT)

    Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        LiquidBottomTabs(
            selectedTabIndex = selectedProvider,
            onTabSelected = { index -> if (mainPages[index] != currentPage) currentNavigate(mainPages[index]) },
            backdrop = sourceBackdrop,
            tabsCount = mainPages.size,
            modifier = Modifier.weight(1f).coachTarget("coach-tabs").testTag("navigation-glass")
                .semantics { stateDescription = if (dragging) "dragging" else "idle" },
            onDragging = { dragging = it },
        ) {
            mainPages.forEachIndexed { index, item ->
                val active = index == selected
                val icon = when (item) { "Beranda" -> "hi_home"; "Statistik" -> "hi_invest"; "Dompet" -> "hi_wallet"; else -> "hi_user" }
                LiquidBottomTab(onClick = { currentNavigate(item) }, modifier = Modifier.testTag("tab-$item").semantics { this.selected = active }) {
                    // Kontras ikon & label (kaca tetap bening): warna teks utama penuh untuk semua tab + halo tipis
                    // warna latar di belakang label agar tetap terbaca saat isi halaman lewat di balik kaca.
                    FFIcon(icon, FFColors.onSurface, 24.dp, stroke = if (active) 2.2f else 1.9f)
                    Text(tabLabel(item), style = FFType.metaData.copy(fontSize = 11.sp, lineHeight = 13.sp, letterSpacing = 0.sp,
                        shadow = androidx.compose.ui.graphics.Shadow(FFColors.canvas.copy(alpha = 0.9f), blurRadius = 8f)),
                        fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold, color = FFColors.onSurface, maxLines = 1)
                }
            }
        }
        // Tombol + kaca bulat terpisah (seperti tombol cari Convx).
        Box(Modifier.size(64.dp).drawBackdrop(sourceBackdrop, { RoundedCornerShape(50) }, effects = {
            vibrancy(); blur(LiquidGlass.BLUR.toPx()); lens(LiquidGlass.REFRACTION_HEIGHT.toPx(), LiquidGlass.REFRACTION_AMOUNT.toPx(), chromaticAberration = true)
        }, highlight = { com.kyant.backdrop.highlight.Highlight.Default.copy(alpha = LiquidGlass.HIGHLIGHT) },
            shadow = { com.kyant.backdrop.shadow.Shadow(radius = 18.dp, alpha = LiquidGlass.SHADOW) },
            onDrawSurface = { drawRect(containerColor) })
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = add)
            .coachTarget("coach-add").testTag("add-transaction").semantics { contentDescription = MR.strings.app_add_transaction.text() }, contentAlignment = Alignment.Center) {
            FFIcon("hi_add", FFColors.onSurface, 26.dp, stroke = 2.2f)
        }
    }
}


// Halaman AI & Premium memakai top bar sendiri (seperti Ollo).
val aiPages = setOf("Asisten", "Pengaturan Asisten", "Pengaturan AI Scan", "Premium")
