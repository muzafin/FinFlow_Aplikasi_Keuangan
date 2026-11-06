package com.logikawan.finflow.feature.auth

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.logikawan.finflow.core.ui.*
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

private val tourBadges = listOf("CATAT CEPAT", "STATISTIK", "PENGELOLAAN", "TABUNGAN")
private val tourTitles = listOf("Catat dalam Sekejap", "Pahami Keuanganmu", "Kendali Penuh", "Tumbuhkan Tabungan")
private val tourDescriptions = listOf(
    "Catat pemasukan dan pengeluaran, pilih kategori serta dompet. Semua tercatat dalam hitungan detik.",
    "Analisis tren pemasukan dan pengeluaran melalui laporan terperinci untuk mengambil keputusan keuangan yang lebih baik.",
    "Kelola semua dompet dan anggaran di satu tempat. Mulai tanpa akun, simpan data di perangkat, dan hubungkan akun saat ingin menggunakan cloud.",
    "Mulai menabung dengan lebih efektif. Ketahui ke mana uangmu pergi dan capai target yang kamu inginkan.",
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
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(12.dp))
                FigmaAsset(
                    tourImages[page],
                    Modifier.fillMaxWidth().aspectRatio(1.15f).clip(RoundedCornerShape(32.dp)),
                    crop = true,
                )
                Spacer(Modifier.height(28.dp))
                Text(
                    tourBadges[page],
                    style = FFType.metaData.copy(fontSize = 13.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold),
                    color = tourAccents[page],
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    tourTitles[page],
                    style = FFType.appTitle.copy(fontSize = 34.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold),
                    color = Color(0xFF1B1B1F),
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    tourDescriptions[page],
                    style = FFType.bodyMain.copy(fontSize = 16.sp, lineHeight = 26.sp),
                    color = Color(0xFF797979),
                )
            }
        }
        if (current == 0) {
            Box(Modifier.fillMaxWidth().padding(end = 32.dp), contentAlignment = Alignment.CenterEnd) {
                TextButton(onClick = onComplete, modifier = Modifier.testTag("onboarding-skip")) {
                    Text("Lewati", color = Color(0xFF797979), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            Spacer(Modifier.height(48.dp))
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(4) { index ->
                    val width by animateDpAsState(if (current == index) 24.dp else 8.dp, tween(250))
                    Box(
                        Modifier.size(width, 6.dp).clip(RoundedCornerShape(8.dp))
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
                modifier = Modifier.height(56.dp).testTag("onboarding-next"),
                shape = RoundedCornerShape(32.dp),
                contentPadding = PaddingValues(horizontal = 26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = tourAccents[current],
                    contentColor = Color.White,
                    disabledContainerColor = tourAccents[current].copy(alpha = .5f),
                    disabledContentColor = Color.White,
                ),
            ) {
                Text(if (current == 3) "Mulai" else "Lanjut", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.width(10.dp))
                FFIcon("chevron_right_rounded", Color.White, 20.dp)
            }
        }
    }
}

@Composable
fun SplashScreen() = EntryTheme {
    Box(Modifier.fillMaxSize().background(Color.White).testTag("splash"),contentAlignment=Alignment.Center) { FinFlowLogo(144.dp) }
}
