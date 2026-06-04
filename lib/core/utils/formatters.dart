import 'package:intl/intl.dart';

/// Formatter utility untuk format angka, tanggal, dan teks
class AppFormatters {
  AppFormatters._();

  // ─── Currency ─────────────────────────────────────────────────────────────

  /// Format nominal ke Rupiah: Rp1.500.000
  static String formatRupiah(double amount) {
    final formatter = NumberFormat.currency(
      locale: 'id_ID',
      symbol: 'Rp',
      decimalDigits: 0,
    );
    return formatter.format(amount);
  }

  /// Format nominal ke Rupiah dengan desimal: Rp1.500.000,00
  static String formatRupiahDecimal(double amount) {
    final formatter = NumberFormat.currency(
      locale: 'id_ID',
      symbol: 'Rp',
      decimalDigits: 2,
    );
    return formatter.format(amount);
  }

  /// Format nominal dengan simbol kustom
  static String formatCurrency(
    double amount, {
    String symbol = 'Rp',
    String locale = 'id_ID',
    int decimalDigits = 0,
  }) {
    final formatter = NumberFormat.currency(
      locale: locale,
      symbol: symbol,
      decimalDigits: decimalDigits,
    );
    return formatter.format(amount);
  }

  /// Format compact: 33jt, 1.5rb
  static String formatCompact(double value) {
    if (value.abs() >= 1000000000) {
      return '${(value / 1000000000).toStringAsFixed(1)}M';
    }
    if (value.abs() >= 1000000) {
      return '${(value / 1000000).toStringAsFixed(1)}jt';
    }
    if (value.abs() >= 1000) {
      return '${(value / 1000).toStringAsFixed(1)}rb';
    }
    return value.toStringAsFixed(0);
  }

  /// Format compact dengan simbol Rupiah: Rp33jt
  static String formatRupiahCompact(double value) {
    return 'Rp${formatCompact(value)}';
  }

  // ─── Date & Time ──────────────────────────────────────────────────────────

  /// Format tanggal panjang: 23 Mei 2026
  static String formatDate(DateTime date) {
    return DateFormat('d MMMM yyyy', 'id_ID').format(date);
  }

  /// Format tanggal pendek: 23 Mei
  static String formatDateShort(DateTime date) {
    return DateFormat('d MMM', 'id_ID').format(date);
  }

  /// Format jam: 14:30
  static String formatTime(DateTime date) {
    return DateFormat('HH:mm').format(date);
  }

  /// Format tanggal + jam: 23 Mei 2026, 14:30
  static String formatDateTime(DateTime date) {
    return '${formatDate(date)}, ${formatTime(date)}';
  }

  /// Format bulan + tahun: Mei 2026
  static String formatMonthYear(DateTime date) {
    return DateFormat('MMMM yyyy', 'id_ID').format(date);
  }

  /// Format relatif: Hari ini, Kemarin, 2 hari lalu
  static String formatRelativeDate(DateTime date) {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final target = DateTime(date.year, date.month, date.day);
    final diff = today.difference(target).inDays;
    if (diff == 0) return 'Hari ini';
    if (diff == 1) return 'Kemarin';
    if (diff < 7) return '$diff hari lalu';
    if (diff < 30) return '${(diff / 7).floor()} minggu lalu';
    return formatDate(date);
  }

  // ─── Percentage ───────────────────────────────────────────────────────────

  /// Format persentase: 73.3%
  static String formatPercent(double value, {int decimals = 1}) {
    final sign = value > 0 ? '+' : '';
    return '$sign${value.toStringAsFixed(decimals)}%';
  }

  /// Format persentase tanpa tanda: 73.3%
  static String formatPercentAbs(double value, {int decimals = 1}) {
    return '${value.abs().toStringAsFixed(decimals)}%';
  }

  // ─── Greeting ─────────────────────────────────────────────────────────────

  /// Salam berdasarkan waktu: Selamat pagi / siang / sore / malam
  static String getGreeting() {
    final hour = DateTime.now().hour;
    if (hour < 10) return 'Selamat pagi';
    if (hour < 15) return 'Selamat siang';
    if (hour < 18) return 'Selamat sore';
    return 'Selamat malam';
  }

  // ─── Input Parsing ────────────────────────────────────────────────────────

  /// Parse string angka (hapus titik/koma) ke double
  static double? parseAmount(String value) {
    final cleaned = value
        .replaceAll('.', '')
        .replaceAll(',', '.')
        .replaceAll(RegExp(r'[^0-9.]'), '');
    return double.tryParse(cleaned);
  }

  /// Format string input numpad ke nominal: "1500000" -> "1.500.000"
  static String formatNumpadInput(String digits) {
    if (digits.isEmpty) return '0';
    final number = int.tryParse(digits.replaceAll('.', '')) ?? 0;
    return NumberFormat('#,###', 'id_ID').format(number);
  }
}
