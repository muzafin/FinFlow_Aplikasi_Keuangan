package com.logikawan.finflow.feature.auth

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logikawan.finflow.core.i18n.text
import com.logikawan.finflow.core.ui.*
import com.logikawan.finflow.i18n.*
import dev.icerock.moko.resources.StringResource
import kotlinx.coroutines.launch

internal val entryAccent = Color(0xFF009688)

private val entryInk = Color(0xFF1D1E26)
private val entryMuted = Color(0xFF818185)

/**
 * Tema netral untuk alur onboarding dan pengaturan awal. Seluruh peran warna Material3 ditulis
 * eksplisit agar tidak ada warna bawaan (lavender surfaceContainerHigh/secondaryContainer) yang
 * bocor ke dialog, alert, atau dropdown. Aksen teal tetap dipakai untuk tombol utama.
 */
@Composable
internal fun EntryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = entryAccent,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFD7EDEA),
            onPrimaryContainer = Color(0xFF0B3B36),
            inversePrimary = Color(0xFF7FD4C8),
            secondary = entryInk,
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFE7E8EC),
            onSecondaryContainer = entryInk,
            tertiary = Color(0xFF5C5E66),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFF1F1F3),
            onTertiaryContainer = entryInk,
            background = Color.White,
            onBackground = entryInk,
            surface = Color.White,
            onSurface = entryInk,
            surfaceVariant = Color(0xFFF7F7F8),
            onSurfaceVariant = entryMuted,
            surfaceTint = entryAccent,
            inverseSurface = Color(0xFF2F3036),
            inverseOnSurface = Color.White,
            error = Color(0xFFB3261E),
            onError = Color.White,
            errorContainer = Color(0xFFF9DEDC),
            onErrorContainer = Color(0xFF410E0B),
            outline = Color(0xFFA1A1A6),
            outlineVariant = Color(0xFFE5E6E9),
            scrim = Color.Black,
            surfaceBright = Color.White,
            surfaceDim = Color(0xFFDEE0E5),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color(0xFFF7F7F8),
            surfaceContainer = Color(0xFFF1F1F3),
            surfaceContainerHigh = Color(0xFFE7E8EC),
            surfaceContainerHighest = Color(0xFFDEE0E5),
            primaryFixed = Color(0xFFD7EDEA),
            primaryFixedDim = Color(0xFFB7DED8),
            onPrimaryFixed = Color(0xFF0B3B36),
            onPrimaryFixedVariant = Color(0xFF1F5A54),
            secondaryFixed = Color(0xFFE7E8EC),
            secondaryFixedDim = Color(0xFFDEE0E5),
            onSecondaryFixed = entryInk,
            onSecondaryFixedVariant = Color(0xFF3D3F47),
            tertiaryFixed = Color(0xFFF1F1F3),
            tertiaryFixedDim = Color(0xFFE7E8EC),
            onTertiaryFixed = entryInk,
            onTertiaryFixedVariant = Color(0xFF5C5E66),
        ),
        content = content,
    )
}

private val tourBadges: List<StringResource> = listOf(
    MR.strings.auth_tour_badge_1, MR.strings.auth_tour_badge_2, MR.strings.auth_tour_badge_3, MR.strings.auth_tour_badge_4,
)
private val tourTitles: List<StringResource> = listOf(
    MR.strings.auth_tour_title_1, MR.strings.auth_tour_title_2, MR.strings.auth_tour_title_3, MR.strings.auth_tour_title_4,
)
private val tourDescriptions: List<StringResource> = listOf(
    MR.strings.auth_tour_desc_1, MR.strings.auth_tour_desc_2, MR.strings.auth_tour_desc_3, MR.strings.auth_tour_desc_4,
)
private val tourImages = listOf(
    "fig_3_25_illustration_image",
    "fig_3_79_illustration_image",
    "fig_3_52_illustration_image",
    "fig_3_106_illustration_image",
)
private val tourAccents = listOf(Color(0xFF009688), Color(0xFF2196F3), Color(0xFF1565C0), Color(0xFF7B1FA2))

/** Tour empat slide mengikuti susunan Ollo: ilustrasi besar, badge, judul, deskripsi, lalu titik dan tombol. */
@Composable
fun OnboardingScreen(onComplete: () -> Unit) = EntryTheme {
    val pager = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()
    val current = pager.currentPage
    Column(Modifier.fillMaxSize().background(Color.White).safeDrawingPadding().testTag("onboarding")) {
        HorizontalPager(pager, Modifier.weight(1f).testTag("onboarding-pager")) { page ->
            val accent = tourAccents[page]
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                // Ollo: kartu ilustrasi persegi ber-tint pastel, dengan bentuk dekoratif samar di belakangnya.
                Box(Modifier.fillMaxWidth().aspectRatio(1.08f)) {
                    Box(Modifier.align(Alignment.TopEnd).offset(x = 70.dp, y = (-40).dp).size(230.dp).clip(CircleShape).background(accent.copy(alpha = .06f)))
                    Box(Modifier.align(Alignment.BottomStart).offset(x = (-60).dp, y = 30.dp).size(150.dp).clip(CircleShape).background(accent.copy(alpha = .07f)))
                    Box(Modifier.align(Alignment.Center).padding(horizontal = 40.dp).fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(28.dp)).background(accent.copy(alpha = .06f))) {
                        FigmaAsset(tourImages[page], Modifier.fillMaxSize(), crop = true)
                    }
                }
                Column(Modifier.padding(horizontal = 32.dp)) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        tourBadges[page].text(),
                        style = FFType.metaData.copy(fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold),
                        color = accent,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        tourTitles[page].text(),
                        style = FFType.appTitle.copy(fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
                        color = Color(0xFF111114),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        tourDescriptions[page].text(),
                        style = FFType.bodyMain.copy(fontSize = 15.sp, lineHeight = 26.sp),
                        color = Color(0xFF6B6B70),
                    )
                }
            }
        }
        // Ollo: "Lewati" hanya di slide pertama, rata kanan di atas tombol.
        Box(Modifier.fillMaxWidth().height(40.dp).padding(end = 48.dp), contentAlignment = Alignment.CenterEnd) {
            if (current == 0) TextButton(onClick = onComplete, modifier = Modifier.testTag("onboarding-skip")) {
                Text(MR.strings.auth_tour_skip.text(), color = Color(0xFF6B6B70), fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 32.dp, end = 28.dp, top = 4.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(4) { index ->
                    val width by animateDpAsState(if (current == index) 32.dp else 8.dp, tween(250))
                    Box(
                        Modifier.size(width, 8.dp).clip(RoundedCornerShape(8.dp))
                            .background(if (current == index) tourAccents[current] else Color(0xFFE5E7EB)),
                    )
                }
            }
            Button(
                onClick = {
                    if (current == 3) onComplete()
                    else scope.launch { pager.animateScrollToPage(current + 1) }
                },
                enabled = !pager.isScrollInProgress,
                modifier = Modifier.height(54.dp).testTag("onboarding-next"),
                shape = RoundedCornerShape(28.dp),
                contentPadding = PaddingValues(horizontal = 28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = tourAccents[current],
                    contentColor = Color.White,
                    disabledContainerColor = tourAccents[current],
                    disabledContentColor = Color.White,
                ),
            ) {
                Text(if (current == 3) MR.strings.auth_tour_start.text() else MR.strings.common_next.text(), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(Modifier.width(10.dp))
                FFIcon("hi_chevron_right", Color.White, 18.dp)
            }
        }
    }
}

@Composable
fun SplashScreen(showLogo: Boolean = true) = EntryTheme {
    Box(Modifier.fillMaxSize().background(Color.White).testTag("splash"),contentAlignment=Alignment.Center) { if (showLogo) FinFlowLogo(144.dp) }
}
