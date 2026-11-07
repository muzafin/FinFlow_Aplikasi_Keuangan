package com.logikawan.finflow.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import com.logikawan.finflow.core.model.LocalPreferences
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logikawan.finflow.resources.*
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource

object FFColors {
    val primary: Color @Composable get() = MaterialTheme.colorScheme.primary
    val primaryContainer: Color @Composable get() = MaterialTheme.colorScheme.primaryContainer
    val primaryFixed: Color @Composable get() = MaterialTheme.colorScheme.primaryContainer
    val secondary: Color @Composable get() = MaterialTheme.colorScheme.secondary
    val secondaryFixed: Color @Composable get() = MaterialTheme.colorScheme.secondaryContainer
    val tertiary: Color @Composable get() = MaterialTheme.colorScheme.tertiary
    val background: Color @Composable get() = MaterialTheme.colorScheme.background
    val canvas: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainerLowest
    val surface: Color @Composable get() = MaterialTheme.colorScheme.surface
    val onSurface: Color @Composable get() = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val outline: Color @Composable get() = MaterialTheme.colorScheme.outline
    val outlineVariant: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
    val surfaceLow: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainerLow
    val surfaceContainer: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainer
    val surfaceContainerHigh: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh
    val surfaceContainerHighest: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainerHighest
    val income: Color @Composable get() = Color(LocalPreferences.current.incomeColor)
    val expense: Color @Composable get() = Color(LocalPreferences.current.expenseColor)
    val transfer: Color @Composable get() = Color(LocalPreferences.current.transferColor)
    val navFab: Color @Composable get() = primary
    // Token Ollo yang berbalik di mode terang (diukur dari Ollo 1.7.9 gelap & terang).
    val isDark: Boolean @Composable get() = MaterialTheme.colorScheme.background.luminance() < .5f
    /** Pil/tombol terpilih: terang di mode gelap, gelap di mode terang. */
    val pill: Color @Composable get() = if (isDark) Color(0xFFE4E4E7) else Color(0xFF1C1E27)
    val onPill: Color @Composable get() = if (isDark) Color(0xFF18181B) else Color.White
    /** Wadah segmen/chip. */
    val chip: Color @Composable get() = if (isDark) Color(0xFF18181D) else Color(0xFFF1F1F2)
    /** Pil sekunder (Lvl, mata uang). */
    val raised: Color @Composable get() = if (isDark) Color(0xFF171717) else Color(0xFFE7E8E9)
    /** Lingkaran ikon. */
    val iconCircle: Color @Composable get() = if (isDark) Color(0xFF393B41) else Color(0xFFE2E2E3)
    /** Garis/bingkai tipis. */
    val hairline: Color @Composable get() = if (isDark) Color(0xFF26262A) else Color(0xFFE5E6E9)
    /** Latar paling dalam (sel kosong heatmap, lingkaran redup). */
    val deep: Color @Composable get() = if (isDark) Color(0xFF09090C) else Color(0xFFF1F1F3)
}

private val LocalMoneyFont = staticCompositionLocalOf<FontFamily> { FontFamily.Monospace }

// Skala tipografi mengikuti Ollo (Plus Jakarta Sans, diukur dari tangkapan Ollo 1.7.9 di 480dpi).
object FFType {
    val appTitle: TextStyle @Composable get() = MaterialTheme.typography.headlineLarge
    val sectionTitle: TextStyle @Composable get() = MaterialTheme.typography.headlineMedium
    val bodyMain: TextStyle @Composable get() = MaterialTheme.typography.bodyMedium
    val metaData: TextStyle @Composable get() = MaterialTheme.typography.bodySmall
    val badgeLabel: TextStyle @Composable get() = MaterialTheme.typography.labelMedium
    val numpadDigit: TextStyle @Composable get() = bodyMain.copy(fontSize = 22.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp)
    val amountXl: TextStyle @Composable get() = amountSm.copy(fontSize = 40.sp, lineHeight = 48.sp, fontWeight = FontWeight.Medium)
    val amountLg: TextStyle @Composable get() = amountSm.copy(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold)
    val amountMd: TextStyle @Composable get() = amountSm.copy(fontSize = 16.5.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium)
    val amountLgMobile: TextStyle @Composable get() = amountSm.copy(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
    val amountSm: TextStyle @Composable get() = TextStyle(fontFamily = LocalMoneyFont.current, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, color = FFColors.onSurface)
}

@Composable
fun FinFlowTheme(usePoppins: Boolean = true, content: @Composable () -> Unit) {
    val poppins = FontFamily(Font(Res.font.poppins_regular), Font(Res.font.poppins_medium, FontWeight.Medium), Font(Res.font.poppins_semibold, FontWeight.SemiBold), Font(Res.font.poppins_bold, FontWeight.Bold))
    val inter = FontFamily(Font(Res.font.inter), Font(Res.font.inter, FontWeight.Medium), Font(Res.font.inter, FontWeight.SemiBold), Font(Res.font.inter, FontWeight.Bold), Font(Res.font.inter, FontWeight.ExtraBold))
    val jakarta = FontFamily(Font(Res.font.plusjakartasans_regular), Font(Res.font.plusjakartasans_medium, FontWeight.Medium), Font(Res.font.plusjakartasans_semibold, FontWeight.SemiBold), Font(Res.font.plusjakartasans_bold, FontWeight.Bold), Font(Res.font.plusjakartasans_extrabold, FontWeight.ExtraBold))
    // Ollo memakai Plus Jakarta Sans di seluruh UI; parameter lama dipertahankan agar pemanggil tidak berubah.
    val uiFont = jakarta
    val mono = uiFont
    fun text(size: Int, weight: FontWeight = FontWeight.Normal, height: Int = size) = TextStyle(fontFamily = uiFont, fontSize = size.sp, fontWeight = weight, lineHeight = height.sp)
    val typography = Typography(
        displayLarge = TextStyle(fontFamily = mono, fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold),
        displayMedium = TextStyle(fontFamily = mono, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
        displaySmall = TextStyle(fontFamily = mono, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
        headlineLarge = text(20, FontWeight.Medium, 26), headlineMedium = text(18, FontWeight.Medium, 24),
        headlineSmall = text(18, FontWeight.Medium, 24), titleLarge = text(18, FontWeight.Medium, 24),
        titleMedium = text(16, FontWeight.Medium, 22), titleSmall = text(14, FontWeight.Medium, 20),
        bodyLarge = text(16, height = 24), bodyMedium = text(14, height = 20), bodySmall = text(12, height = 16),
        labelLarge = text(14, FontWeight.Medium, 20), labelMedium = text(12, FontWeight.Medium, 16), labelSmall = text(10, FontWeight.Medium, 14),
    )
    val preferences=LocalPreferences.current
    val dark=preferences.theme=="dark" || preferences.theme=="system" && isSystemInDarkTheme()
    val accent=Color(if(dark)when(preferences.palette){"arctic"->0xFF8DCFF0;"ruby"->0xFFFFB4AB;"emerald"->0xFFA8DAB5;"indigo"->0xFFBEC2FF;else->0xFFD5D8E1}
        else when(preferences.palette){"arctic"->0xFF006B8F;"ruby"->0xFFC62828;"emerald"->0xFF237A43;"indigo"->0xFF4548AF;else->0xFF1D1E26})
    val base=if(dark) darkColorScheme() else lightColorScheme()
    val colors=base.copy(primary=accent,onPrimary=if(dark)Color(0xFF1D1E26) else Color.White,
        primaryContainer=accent.copy(alpha=.12f),onPrimaryContainer=accent,secondary=Color(0xFF5B74CE),secondaryContainer=Color(0xFFE7EBFA),
        background=if(dark)Color(0xFF000000) else Color.White,onBackground=if(dark)Color(0xFFF2F2F5) else Color(0xFF1D1E26),
        surface=if(dark)Color(0xFF101014) else Color(0xFFF7F7F8),onSurface=if(dark)Color(0xFFF2F2F5) else Color(0xFF1D1E26),
        onSurfaceVariant=if(dark)Color(0xFFA1A1AA) else Color(0xFF818185),surfaceContainerLowest=if(dark)Color(0xFF000000) else Color.White,
        surfaceContainerLow=if(dark)Color(0xFF101014) else Color(0xFFF7F7F8),surfaceContainer=if(dark)Color(0xFF171718) else Color(0xFFF1F1F3),
        surfaceContainerHigh=if(dark)Color(0xFF252529) else Color(0xFFE7E8EC),surfaceContainerHighest=if(dark)Color(0xFF2E2E33) else Color(0xFFDEE0E5),
        outline=if(dark)Color(0xFF858791) else Color(0xFFA1A1A6),outlineVariant=if(dark)Color(0xFF1C1C21) else Color(0xFFE5E6E9),surfaceTint=accent)
    CompositionLocalProvider(LocalMoneyFont provides mono) {
        MaterialTheme(colorScheme = colors, typography = typography, shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(24.dp))) {
            CompositionLocalProvider(LocalContentColor provides colors.onSurface) {
                ProvideTextStyle(typography.bodyMedium, content = content)
            }
        }
    }
}

@Composable
fun FinFlowLogo(size: Dp = 72.dp) {
    Image(painterResource(Res.drawable.finflow_logo), "FinFlow", Modifier.size(size))
}
