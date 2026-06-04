import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

/// Tipografi FinFlow berdasarkan Stitch Design System
/// - Poppins: semua elemen UI
/// - DM Mono: angka finansial
class AppTypography {
  AppTypography._();

  static TextStyle get _poppinsBase =>
      GoogleFonts.poppins(color: const Color(0xFF111F13));

  static TextStyle get _dmMonoBase =>
      GoogleFonts.dmMono(color: const Color(0xFF111F13));

  // ─── UI Text ──────────────────────────────────────────────────────────────

  /// App Title: Poppins 24px Bold  e.g. "~ Hi, Ollo!"
  static TextStyle get appTitle => _poppinsBase.copyWith(
        fontSize: 24,
        fontWeight: FontWeight.w700,
        height: 32 / 24,
      );

  /// Section Title: Poppins 18px SemiBold  e.g. "Transaksi Terakhir"
  static TextStyle get sectionTitle => _poppinsBase.copyWith(
        fontSize: 18,
        fontWeight: FontWeight.w600,
        height: 24 / 18,
      );

  /// Body Main: Poppins 14px Regular  e.g. transaction description
  static TextStyle get bodyMain => _poppinsBase.copyWith(
        fontSize: 14,
        fontWeight: FontWeight.w400,
        height: 20 / 14,
      );

  /// Meta Data: Poppins 12px Regular  e.g. timestamps, subtexts
  static TextStyle get metaData => _poppinsBase.copyWith(
        fontSize: 12,
        fontWeight: FontWeight.w400,
        height: 16 / 12,
      );

  /// Badge Label: Poppins 11px Medium  e.g. "Lvl 6"
  static TextStyle get badgeLabel => _poppinsBase.copyWith(
        fontSize: 11,
        fontWeight: FontWeight.w500,
        letterSpacing: 0.55,
      );

  /// Numpad Digit: Poppins 22px Medium  e.g. calculator keys
  static TextStyle get numpadDigit => _poppinsBase.copyWith(
        fontSize: 22,
        fontWeight: FontWeight.w500,
        height: 1.0,
      );

  // ─── Financial Figures (DM Mono) ──────────────────────────────────────────

  /// Amount Large: DM Mono 36px Bold  e.g. total balance card
  static TextStyle get amountLg => _dmMonoBase.copyWith(
        fontSize: 36,
        fontWeight: FontWeight.w700,
        height: 44 / 36,
      );

  /// Amount Medium: DM Mono 20px Medium  e.g. sub-card amounts
  static TextStyle get amountMd => _dmMonoBase.copyWith(
        fontSize: 20,
        fontWeight: FontWeight.w500,
        height: 28 / 20,
      );

  /// Amount Large Mobile: DM Mono 28px Bold  e.g. add transaction display
  static TextStyle get amountLgMobile => _dmMonoBase.copyWith(
        fontSize: 28,
        fontWeight: FontWeight.w700,
        height: 34 / 28,
      );

  /// Amount Small: DM Mono 14px Medium  e.g. list item amounts
  static TextStyle get amountSm => _dmMonoBase.copyWith(
        fontSize: 14,
        fontWeight: FontWeight.w500,
      );

  // ─── TextTheme for ThemeData ───────────────────────────────────────────────
  static TextTheme get textTheme => TextTheme(
        displayLarge: amountLg,
        displayMedium: amountMd,
        displaySmall: amountLgMobile,
        headlineLarge: appTitle,
        headlineMedium: sectionTitle,
        titleLarge: _poppinsBase.copyWith(
            fontSize: 20, fontWeight: FontWeight.w600),
        titleMedium: _poppinsBase.copyWith(
            fontSize: 16, fontWeight: FontWeight.w600),
        titleSmall: _poppinsBase.copyWith(
            fontSize: 14, fontWeight: FontWeight.w500),
        bodyLarge: _poppinsBase.copyWith(
            fontSize: 16, fontWeight: FontWeight.w400),
        bodyMedium: bodyMain,
        bodySmall: metaData,
        labelLarge: _poppinsBase.copyWith(
            fontSize: 14, fontWeight: FontWeight.w600),
        labelMedium: badgeLabel,
        labelSmall: _poppinsBase.copyWith(
            fontSize: 10, fontWeight: FontWeight.w500),
      );
}
