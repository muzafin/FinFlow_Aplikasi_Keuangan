import 'package:flutter/material.dart';

/// Semua warna dari Stitch Design System "Soft Modern Clean"
class AppColors {
  AppColors._();

  // ─── Primary ──────────────────────────────────────────────────────────────
  static const Color primary = Color(0xFF006947);
  static const Color primaryContainer = Color(0xFF02855B);
  static const Color onPrimary = Color(0xFFFFFFFF);
  static const Color onPrimaryContainer = Color(0xFFF5FFF6);
  static const Color inversePrimary = Color(0xFF72DAA9);
  static const Color primaryFixed = Color(0xFF8FF7C4);
  static const Color primaryFixedDim = Color(0xFF72DAA9);
  static const Color onPrimaryFixed = Color(0xFF002113);
  static const Color onPrimaryFixedVariant = Color(0xFF005236);

  // ─── Secondary ────────────────────────────────────────────────────────────
  static const Color secondary = Color(0xFF1E5BBA);
  static const Color secondaryContainer = Color(0xFF6B9CFF);
  static const Color onSecondary = Color(0xFFFFFFFF);
  static const Color onSecondaryContainer = Color(0xFF003275);
  static const Color secondaryFixed = Color(0xFFD8E2FF);
  static const Color secondaryFixedDim = Color(0xFFAEC6FF);
  static const Color onSecondaryFixed = Color(0xFF001A43);
  static const Color onSecondaryFixedVariant = Color(0xFF004397);

  // ─── Tertiary ─────────────────────────────────────────────────────────────
  static const Color tertiary = Color(0xFF805200);
  static const Color tertiaryContainer = Color(0xFFA16900);
  static const Color onTertiary = Color(0xFFFFFFFF);
  static const Color onTertiaryContainer = Color(0xFFFFFBFF);
  static const Color tertiaryFixed = Color(0xFFFFDDB4);
  static const Color tertiaryFixedDim = Color(0xFFFFB955);
  static const Color onTertiaryFixed = Color(0xFF291800);
  static const Color onTertiaryFixedVariant = Color(0xFF633F00);

  // ─── Error ────────────────────────────────────────────────────────────────
  static const Color error = Color(0xFFBA1A1A);
  static const Color errorContainer = Color(0xFFFFDAD6);
  static const Color onError = Color(0xFFFFFFFF);
  static const Color onErrorContainer = Color(0xFF93000A);

  // ─── Surface (Light) ──────────────────────────────────────────────────────
  static const Color surface = Color(0xFFEDFEEA);
  static const Color surfaceBright = Color(0xFFEDFEEA);
  static const Color surfaceDim = Color(0xFFCDDFCC);
  static const Color surfaceContainerLowest = Color(0xFFFFFFFF);
  static const Color surfaceContainerLow = Color(0xFFE7F8E5);
  static const Color surfaceContainer = Color(0xFFE1F3DF);
  static const Color surfaceContainerHigh = Color(0xFFDCEDDA);
  static const Color surfaceContainerHighest = Color(0xFFD6E7D4);
  static const Color surfaceVariant = Color(0xFFD6E7D4);
  static const Color surfaceTint = Color(0xFF006C49);
  static const Color onSurface = Color(0xFF111F13);
  static const Color onSurfaceVariant = Color(0xFF3E4942);
  static const Color inverseSurface = Color(0xFF253427);
  static const Color inverseOnSurface = Color(0xFFE4F6E2);

  // ─── Outline ──────────────────────────────────────────────────────────────
  static const Color outline = Color(0xFF6E7A72);
  static const Color outlineVariant = Color(0xFFBDCAC0);

  // ─── Background ───────────────────────────────────────────────────────────
  static const Color background = Color(0xFFEDFEEA);
  static const Color onBackground = Color(0xFF111F13);
  static const Color bgLight = Color(0xFFF5F7F5);
  static const Color bgDark = Color(0xFF111412);

  // ─── Semantic ─────────────────────────────────────────────────────────────
  static const Color income = Color(0xFF2D9B6F);
  static const Color expense = Color(0xFFE05C5C);

  // ─── Category Accents ─────────────────────────────────────────────────────
  static const Color foodAccent = Color(0xFFF5A623);
  static const Color transportAccent = Color(0xFF5B8DEF);
  static const Color shoppingAccent = Color(0xFF9B59B6);
  static const Color healthAccent = Color(0xFFE74C3C);
  static const Color entertainmentAccent = Color(0xFF8E44AD);
  static const Color educationAccent = Color(0xFF2980B9);
  static const Color housingAccent = Color(0xFF16A085);
  static const Color savingsAccent = Color(0xFF27AE60);

  // ─── Navigation FAB ───────────────────────────────────────────────────────
  static const Color navFab = Color(0xFF3E525E);

  // ─── Glass ────────────────────────────────────────────────────────────────
  static const Color glassCardStart = Color(0xE64A90E2); // rgba(74,144,226,0.9)
  static const Color glassCardEnd = Color(0xE65CE1E6);   // rgba(92,225,230,0.9)

  // ─── ColorScheme builders ─────────────────────────────────────────────────
  static ColorScheme get lightColorScheme => const ColorScheme(
    brightness: Brightness.light,
    primary: primary,
    onPrimary: onPrimary,
    primaryContainer: primaryContainer,
    onPrimaryContainer: onPrimaryContainer,
    secondary: secondary,
    onSecondary: onSecondary,
    secondaryContainer: secondaryContainer,
    onSecondaryContainer: onSecondaryContainer,
    tertiary: tertiary,
    onTertiary: onTertiary,
    tertiaryContainer: tertiaryContainer,
    onTertiaryContainer: onTertiaryContainer,
    error: error,
    onError: onError,
    errorContainer: errorContainer,
    onErrorContainer: onErrorContainer,
    surface: surface,
    onSurface: onSurface,
    onSurfaceVariant: onSurfaceVariant,
    outline: outline,
    outlineVariant: outlineVariant,
    inverseSurface: inverseSurface,
    onInverseSurface: inverseOnSurface,
    inversePrimary: inversePrimary,
    surfaceTint: surfaceTint,
  );

  static ColorScheme get darkColorScheme => const ColorScheme(
    brightness: Brightness.dark,
    primary: inversePrimary,
    onPrimary: Color(0xFF003822),
    primaryContainer: Color(0xFF005236),
    onPrimaryContainer: primaryFixed,
    secondary: secondaryFixedDim,
    onSecondary: Color(0xFF002E6A),
    secondaryContainer: Color(0xFF004397),
    onSecondaryContainer: secondaryFixed,
    tertiary: tertiaryFixedDim,
    onTertiary: Color(0xFF452B00),
    tertiaryContainer: onTertiaryFixedVariant,
    onTertiaryContainer: tertiaryFixed,
    error: Color(0xFFFFB4AB),
    onError: Color(0xFF690005),
    errorContainer: Color(0xFF93000A),
    onErrorContainer: errorContainer,
    surface: bgDark,
    onSurface: Color(0xFFD6E7D4),
    onSurfaceVariant: outlineVariant,
    outline: Color(0xFF88938B),
    outlineVariant: Color(0xFF3E4942),
    inverseSurface: surfaceContainerHighest,
    onInverseSurface: inverseSurface,
    inversePrimary: primary,
    surfaceTint: inversePrimary,
  );
}
