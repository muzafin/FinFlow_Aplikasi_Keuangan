package com.clonical.kasiva.core.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.i18n.*
import dev.icerock.moko.resources.StringResource
import kotlin.math.roundToInt

/**
 * Panduan pertama kali (coach mark): layar diredupkan, elemen yang dijelaskan diberi lubang + garis sorot berdenyut,
 * dan kartu penjelasan dengan anak panah menunjuk elemen tersebut.
 *
 * Elemen didaftarkan dengan [coachTarget]; posisinya diukur dalam koordinat jendela sehingga overlay cukup
 * digambar sekali di atas seluruh layar ([CoachMarkOverlay]).
 */
class CoachMarkRegistry {
    val bounds = mutableStateMapOf<String, Rect>()
}

val LocalCoachMarks = staticCompositionLocalOf<CoachMarkRegistry?> { null }

fun Modifier.coachTarget(id: String): Modifier = composed {
    val registry = LocalCoachMarks.current
    if (registry == null) this else this.onGloballyPositioned { coordinates ->
        // Koordinat layar: overlay digambar di jendela Popup terpisah agar selalu di atas bilah kaca navigasi.
        val rect = Rect(coordinates.positionOnScreen(), coordinates.size.let { Size(it.width.toFloat(), it.height.toFloat()) })
        if (rect.width > 0f && rect.height > 0f) registry.bounds[id] = rect
    }
}

data class CoachStep(val target: String, val title: StringResource, val body: StringResource)

/** Langkah panduan Beranda (urutan dari atas ke bawah layar agar mudah diikuti). */
val homeCoachSteps = listOf(
    CoachStep("coach-add", MR.strings.coach_add_title, MR.strings.coach_add_body),
    CoachStep("coach-balance", MR.strings.coach_balance_title, MR.strings.coach_balance_body),
    CoachStep("coach-menu", MR.strings.coach_menu_title, MR.strings.coach_menu_body),
    CoachStep("coach-level", MR.strings.coach_level_title, MR.strings.coach_level_body),
    CoachStep("coach-tabs", MR.strings.coach_tabs_title, MR.strings.coach_tabs_body),
)

/** Hijau toska Kasiva — sama dengan tur onboarding & cincin level. */
internal val CoachAccent = Color(0xFF009688)
private val Accent = CoachAccent

@Composable
fun CoachMarkOverlay(registry: CoachMarkRegistry, steps: List<CoachStep>, onFinish: () -> Unit) {
    // Langkah yang elemennya tidak tampil (mis. disembunyikan pengguna) dilewati otomatis.
    val available = steps.filter { registry.bounds[it.target] != null }
    var index by remember { mutableIntStateOf(0) }
    if (available.isEmpty()) { LaunchedEffect(Unit) { onFinish() }; return }
    val step = available[index.coerceIn(0, available.lastIndex)]
    val density = LocalDensity.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    var overlaySize by remember { mutableStateOf(IntSize.Zero) }
    val target = registry.bounds[step.target]?.translate(-origin) ?: return
    val pad = with(density) { 6.dp.toPx() }
    val hole = Rect(target.left - pad, target.top - pad, target.right + pad, target.bottom + pad)
    // Kartu mengikuti tema aplikasi: putih di mode terang, abu gelap terangkat di mode gelap.
    val cardColor = if (FFColors.isDark) FFColors.surfaceContainerHigh else Color.White
    val ink = FFColors.onSurface
    val muted = FFColors.onSurfaceVariant
    val dotIdle = FFColors.outlineVariant
    val pulse by rememberInfiniteTransition(label = "coach").animateFloat(0f, 1f,
        infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "coach-pulse")

    androidx.compose.ui.window.Popup(properties = androidx.compose.ui.window.PopupProperties(focusable = true, clippingEnabled = false)) {
    Box(Modifier.fillMaxSize().testTag("coach-overlay")
        .onGloballyPositioned { origin = it.positionOnScreen() }
        .onSizeChanged { overlaySize = it }
        // Menahan sentuhan ke layar di bawahnya; ketuk area redup = lanjut.
        .pointerInput(index, available.size) { detectTapGestures { if (index < available.lastIndex) index++ else onFinish() } }) {
        Canvas(Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
            val radius = CornerRadius(with(density) { 18.dp.toPx() })
            drawRect(Color.Black.copy(alpha = 0.72f))
            drawRoundRect(Color.Transparent, hole.topLeft, hole.size, radius, blendMode = BlendMode.Clear)
            val glow = with(density) { (3.dp + 5.dp * pulse).toPx() }
            drawRoundRect(Accent.copy(alpha = 0.35f * (1f - pulse)), Offset(hole.left - glow / 2, hole.top - glow / 2),
                Size(hole.width + glow, hole.height + glow), radius, style = Stroke(glow))
            drawRoundRect(Accent, hole.topLeft, hole.size, radius, style = Stroke(with(density) { 2.5.dp.toPx() }))
        }

        // Ukuran overlay belum diketahui pada frame pertama: tunggu sebelum menghitung posisi kartu/panah.
        if (overlaySize.width == 0 || overlaySize.height == 0) return@Box
        // Kartu di bawah elemen bila elemen di separuh atas layar, selain itu di atasnya.
        val below = hole.center.y < overlaySize.height / 2f
        val margin = with(density) { 16.dp.toPx() }
        val arrowHeight = with(density) { 12.dp.toPx() }
        val cardWidthPx = (overlaySize.width - margin * 2).coerceAtLeast(0f).coerceAtMost(with(density) { 380.dp.toPx() })
        val cardLeft = (hole.center.x - cardWidthPx / 2).coerceIn(margin, (overlaySize.width - margin - cardWidthPx).coerceAtLeast(margin))
        var cardHeight by remember(step) { mutableIntStateOf(0) }
        val cardTop = if (below) hole.bottom + arrowHeight + with(density) { 4.dp.toPx() }
            else hole.top - arrowHeight - with(density) { 4.dp.toPx() } - cardHeight

        // Anak panah menunjuk tengah elemen.
        Canvas(Modifier.fillMaxSize()) {
            val tipX = if (cardWidthPx > 96f) hole.center.x.coerceIn(cardLeft + 48f, cardLeft + cardWidthPx - 48f) else hole.center.x
            if (cardHeight == 0) return@Canvas
            val baseY = if (below) cardTop + 1f else cardTop + cardHeight - 1f
            val tipY = if (below) hole.bottom + 4f else hole.top - 4f
            val half = arrowHeight
            drawPath(Path().apply { moveTo(tipX, tipY); lineTo(tipX - half, baseY); lineTo(tipX + half, baseY); close() }, cardColor)
        }

        Column(Modifier.offset { IntOffset(cardLeft.roundToInt(), cardTop.roundToInt()) }
            .width(with(density) { cardWidthPx.toDp() })
            .onSizeChanged { cardHeight = it.height }
            .shadow(20.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp)).background(cardColor)
            .pointerInput(Unit) { detectTapGestures { } }
            .padding(start = 22.dp, end = 14.dp, top = 14.dp, bottom = 18.dp).testTag("coach-card")) {
            // Baris atas: penomoran langkah + "Lewati" (seperti tur onboarding).
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(MR.strings.coach_step.text(index + 1, available.size), Modifier.weight(1f),
                    style = FFType.metaData.copy(fontWeight = FontWeight.SemiBold, letterSpacing = .3.sp), color = Accent, maxLines = 1)
                if (index < available.lastIndex) TextButton(onClick = onFinish, modifier = Modifier.heightIn(min = 36.dp).testTag("coach-skip"),
                    contentPadding = PaddingValues(horizontal = 10.dp)) {
                    Text(MR.strings.coach_skip.text(), color = muted, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                } else Spacer(Modifier.height(36.dp))
            }
            Column(Modifier.padding(end = 8.dp)) {
                Text(step.title.text(), style = FFType.appTitle.copy(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold), color = ink)
                Spacer(Modifier.height(8.dp))
                Text(step.body.text(), style = FFType.bodyMain.copy(fontSize = 15.sp, lineHeight = 23.sp), color = muted)
            }
            Spacer(Modifier.height(18.dp))
            // Baris bawah: titik progres, tombol kembali (ikon, tanpa teks agar muat di semua bahasa), tombol lanjut.
            Row(Modifier.padding(end = 4.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(available.size) { dot ->
                        val width by animateDpAsState(if (dot == index) 24.dp else 8.dp, tween(250), label = "coach-dot")
                        Box(Modifier.size(width, 8.dp).clip(RoundedCornerShape(8.dp)).background(if (dot == index) Accent else dotIdle))
                    }
                }
                if (index > 0) {
                    Box(Modifier.size(44.dp).clip(CircleShape).border(1.dp, FFColors.outlineVariant, CircleShape)
                        .clickable(role = androidx.compose.ui.semantics.Role.Button) { index-- }.testTag("coach-back"),
                        contentAlignment = androidx.compose.ui.Alignment.Center) {
                        FFIcon("hi_chevron_left", ink, 20.dp, contentDescription = MR.strings.coach_back.text())
                    }
                    Spacer(Modifier.width(10.dp))
                }
                Button(onClick = { if (index < available.lastIndex) index++ else onFinish() },
                    modifier = Modifier.heightIn(min = 46.dp).widthIn(max = with(density) { (cardWidthPx * .55f).toDp() }).testTag("coach-next"),
                    shape = RoundedCornerShape(28.dp), contentPadding = PaddingValues(start = 20.dp, end = 14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White)) {
                    Text(if (index < available.lastIndex) MR.strings.coach_next.text() else MR.strings.coach_done.text(),
                        Modifier.weight(1f, fill = false), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Spacer(Modifier.width(6.dp))
                    FFIcon("hi_chevron_right", Color.White, 18.dp)
                }
            }
        }
    }
    }
}
