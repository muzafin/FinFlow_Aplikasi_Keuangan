import 'package:flutter/material.dart';
import 'app_colors.dart';
import 'app_typography.dart';

/// ThemeData untuk FinFlow – Light & Dark
class AppTheme {
  AppTheme._();

  static const _cardRadius = 16.0;
  static const _buttonRadius = 999.0;
  static const _inputRadius = 12.0;

  // ─── Light Theme ──────────────────────────────────────────────────────────
  static ThemeData get lightTheme => ThemeData(
        useMaterial3: true,
        brightness: Brightness.light,
        colorScheme: AppColors.lightColorScheme,
        textTheme: AppTypography.textTheme,
        scaffoldBackgroundColor: AppColors.bgLight,

        // App Bar
        appBarTheme: AppBarTheme(
          backgroundColor: Colors.transparent,
          elevation: 0,
          scrolledUnderElevation: 0,
          centerTitle: true,
          iconTheme: const IconThemeData(color: AppColors.onSurface),
          titleTextStyle: AppTypography.appTitle
              .copyWith(color: AppColors.onSurface),
        ),

        // Card
        cardTheme: CardThemeData(
          color: AppColors.surfaceContainerLowest,
          elevation: 0,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(_cardRadius),
            side: const BorderSide(
                color: AppColors.outlineVariant, width: 0.5),
          ),
          margin: EdgeInsets.zero,
        ),

        // Elevated Button (primary CTAs)
        elevatedButtonTheme: ElevatedButtonThemeData(
          style: ElevatedButton.styleFrom(
            backgroundColor: AppColors.primary,
            foregroundColor: AppColors.onPrimary,
            elevation: 0,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(_buttonRadius),
            ),
            minimumSize: const Size(double.infinity, 52),
            textStyle: AppTypography.bodyMain.copyWith(
              fontWeight: FontWeight.w600,
              fontSize: 15,
            ),
          ),
        ),

        // Outlined Button (secondary CTAs)
        outlinedButtonTheme: OutlinedButtonThemeData(
          style: OutlinedButton.styleFrom(
            foregroundColor: AppColors.primary,
            side: const BorderSide(color: AppColors.outlineVariant),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(_buttonRadius),
            ),
            minimumSize: const Size(double.infinity, 52),
            textStyle: AppTypography.bodyMain.copyWith(
              fontWeight: FontWeight.w600,
              fontSize: 15,
            ),
          ),
        ),

        // Text Button
        textButtonTheme: TextButtonThemeData(
          style: TextButton.styleFrom(
            foregroundColor: AppColors.primary,
            textStyle: AppTypography.bodyMain.copyWith(
              fontWeight: FontWeight.w600,
            ),
          ),
        ),

        // Input Decoration
        inputDecorationTheme: InputDecorationTheme(
          filled: true,
          fillColor: AppColors.surfaceContainerLow,
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(_inputRadius),
            borderSide: BorderSide.none,
          ),
          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(_inputRadius),
            borderSide: BorderSide.none,
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(_inputRadius),
            borderSide: const BorderSide(
                color: AppColors.primary, width: 1.5),
          ),
          errorBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(_inputRadius),
            borderSide: const BorderSide(color: AppColors.error),
          ),
          contentPadding:
              const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
          hintStyle: AppTypography.bodyMain
              .copyWith(color: AppColors.onSurfaceVariant),
          labelStyle: AppTypography.bodyMain
              .copyWith(color: AppColors.onSurfaceVariant),
        ),

        // Chip
        chipTheme: ChipThemeData(
          backgroundColor: AppColors.surfaceContainerLow,
          selectedColor: AppColors.primaryFixed,
          labelStyle: AppTypography.metaData,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(8),
          ),
          side: const BorderSide(color: AppColors.outlineVariant),
        ),

        // Divider
        dividerTheme: const DividerThemeData(
          color: AppColors.outlineVariant,
          thickness: 0.5,
          space: 1,
        ),

        // Bottom Navigation
        bottomNavigationBarTheme: const BottomNavigationBarThemeData(
          backgroundColor: Colors.transparent,
          elevation: 0,
          selectedItemColor: AppColors.primary,
          unselectedItemColor: AppColors.onSurfaceVariant,
          type: BottomNavigationBarType.fixed,
          showSelectedLabels: true,
          showUnselectedLabels: true,
        ),

        // Snack Bar
        snackBarTheme: SnackBarThemeData(
          behavior: SnackBarBehavior.floating,
          backgroundColor: AppColors.inverseSurface,
          contentTextStyle: AppTypography.bodyMain
              .copyWith(color: AppColors.inverseOnSurface),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
        ),

        // Dialog
        dialogTheme: DialogThemeData(
          backgroundColor: AppColors.surfaceContainerLowest,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(24),
          ),
          titleTextStyle: AppTypography.sectionTitle,
          contentTextStyle: AppTypography.bodyMain,
        ),

        // FAB
        floatingActionButtonTheme: const FloatingActionButtonThemeData(
          backgroundColor: AppColors.navFab,
          foregroundColor: Colors.white,
          elevation: 4,
          shape: CircleBorder(),
        ),
      );

  // ─── Dark Theme ───────────────────────────────────────────────────────────
  static ThemeData get darkTheme => lightTheme.copyWith(
        brightness: Brightness.dark,
        colorScheme: AppColors.darkColorScheme,
        scaffoldBackgroundColor: AppColors.bgDark,
        appBarTheme: AppBarTheme(
          backgroundColor: Colors.transparent,
          elevation: 0,
          scrolledUnderElevation: 0,
          centerTitle: true,
          iconTheme:
              const IconThemeData(color: Color(0xFFD6E7D4)),
          titleTextStyle: AppTypography.appTitle
              .copyWith(color: const Color(0xFFD6E7D4)),
        ),
      );
}
