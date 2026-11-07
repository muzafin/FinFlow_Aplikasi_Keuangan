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
import com.logikawan.finflow.core.ui.liquid.LiquidBottomTab
import com.logikawan.finflow.core.ui.liquid.LiquidBottomTabs
import kotlin.math.roundToInt
import com.logikawan.finflow.i18n.*
import com.logikawan.finflow.core.i18n.text
import com.logikawan.finflow.core.i18n.tabLabel

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
    val containerColor = if (FFColors.isDark) Color(0xFF121212).copy(.4f) else Color(0xFFFAFAFA).copy(.4f)

    Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        LiquidBottomTabs(
            selectedTabIndex = selectedProvider,
            onTabSelected = { index -> if (mainPages[index] != currentPage) currentNavigate(mainPages[index]) },
            backdrop = sourceBackdrop,
            tabsCount = mainPages.size,
            modifier = Modifier.weight(1f).testTag("navigation-glass")
                .semantics { stateDescription = if (dragging) "dragging" else "idle" },
            onDragging = { dragging = it },
        ) {
            mainPages.forEachIndexed { index, item ->
                val active = index == selected
                val icon = when (item) { "Beranda" -> "hi_home"; "Statistik" -> "hi_invest"; "Dompet" -> "hi_wallet"; else -> "hi_user" }
                LiquidBottomTab(onClick = { currentNavigate(item) }, modifier = Modifier.testTag("tab-$item").semantics { this.selected = active }) {
                    FFIcon(icon, if (active) FFColors.onSurface else FFColors.onSurfaceVariant, 24.dp)
                    Text(tabLabel(item), style = FFType.metaData.copy(fontSize = 11.sp, lineHeight = 13.sp, letterSpacing = 0.sp),
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal, color = if (active) FFColors.onSurface else FFColors.onSurfaceVariant, maxLines = 1)
                }
            }
        }
        // Tombol + kaca bulat terpisah (seperti tombol cari Convx).
        Box(Modifier.size(64.dp).drawBackdrop(sourceBackdrop, { RoundedCornerShape(50) }, effects = {
            vibrancy(); blur(8.dp.toPx()); lens(24.dp.toPx(), 24.dp.toPx())
        }, onDrawSurface = { drawRect(containerColor) })
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = add)
            .testTag("add-transaction").semantics { contentDescription = MR.strings.app_add_transaction.text() }, contentAlignment = Alignment.Center) {
            FFIcon("hi_add", FFColors.onSurface, 26.dp)
        }
    }
}


// Halaman AI & Premium memakai top bar sendiri (seperti Ollo).
val aiPages = setOf("Asisten", "Pengaturan Asisten", "Pengaturan AI Scan", "Premium")
