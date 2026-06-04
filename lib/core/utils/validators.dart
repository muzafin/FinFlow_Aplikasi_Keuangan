/// Validator functions untuk form FinFlow (pesan error dalam Bahasa Indonesia)
class AppValidators {
  AppValidators._();

  static String? required(String? value, {String field = 'Field ini'}) {
    if (value == null || value.trim().isEmpty) {
      return '$field tidak boleh kosong';
    }
    return null;
  }

  static String? email(String? value) {
    if (value == null || value.trim().isEmpty) {
      return 'Email tidak boleh kosong';
    }
    final regex = RegExp(r'^[\w\-\.]+@([\w\-]+\.)+[\w\-]{2,4}$');
    if (!regex.hasMatch(value.trim())) {
      return 'Format email tidak valid';
    }
    return null;
  }

  static String? password(String? value) {
    if (value == null || value.isEmpty) return 'Password tidak boleh kosong';
    if (value.length < 8) return 'Password minimal 8 karakter';
    return null;
  }

  static String? confirmPassword(String? value, String password) {
    if (value == null || value.isEmpty) {
      return 'Konfirmasi password tidak boleh kosong';
    }
    if (value != password) return 'Password tidak cocok';
    return null;
  }

  static String? name(String? value) {
    if (value == null || value.trim().isEmpty) return 'Nama tidak boleh kosong';
    if (value.trim().length < 2) return 'Nama minimal 2 karakter';
    return null;
  }

  static String? amount(String? value) {
    if (value == null || value.isEmpty) return 'Nominal tidak boleh kosong';
    final cleaned = value.replaceAll(RegExp(r'[^0-9]'), '');
    final parsed = double.tryParse(cleaned);
    if (parsed == null || parsed <= 0) return 'Nominal harus lebih dari 0';
    return null;
  }
}
