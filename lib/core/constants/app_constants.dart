/// Konstanta global aplikasi FinFlow
class AppConstants {
  AppConstants._();

  // ─── App Info ─────────────────────────────────────────────────────────────
  static const String appName = 'FinFlow';
  static const String appVersion = '1.0.0';
  static const String appTagline = 'Kelola Keuangan Pribadi';

  // ─── Default Values ───────────────────────────────────────────────────────
  static const String defaultCurrency = 'IDR';
  static const String defaultCurrencySymbol = 'Rp';
  static const String defaultLocale = 'id_ID';

  // ─── Pagination ───────────────────────────────────────────────────────────
  static const int defaultPageSize = 20;

  // ─── Firestore Collections ────────────────────────────────────────────────
  static const String usersCollection = 'users';
  static const String transactionsCollection = 'transactions';
  static const String budgetsCollection = 'budgets';
  static const String investmentsCollection = 'investments';
  static const String categoriesCollection = 'categories';
  static const String accountsCollection = 'accounts';
  static const String goalsCollection = 'goals';

  // ─── Hive Boxes ───────────────────────────────────────────────────────────
  static const String settingsBox = 'settings';
  static const String cacheBox = 'cache';

  // ─── Shared Preferences Keys ──────────────────────────────────────────────
  static const String onboardingSeenKey = 'onboarding_seen';
  static const String themeModeKey = 'theme_mode';
  static const String privacyModeKey = 'privacy_mode';
  static const String currencyKey = 'currency';
  static const String localeKey = 'locale';
  static const String biometricKey = 'biometric_enabled';
}
