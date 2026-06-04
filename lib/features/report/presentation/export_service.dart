import 'dart:io';
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';
import '../../../core/utils/formatters.dart';
import '../../../shared/models/transaction_model.dart';

class ExportService {
  ExportService._();

  static Future<void> exportAndShareCSV(List<TransactionModel> transactions, String periodName) async {
    final buffer = StringBuffer();
    // 1. Header
    buffer.writeln('Tanggal,Kategori,Tipe,Judul,Catatan,Nominal,Akun');

    // 2. Rows
    for (var tx in transactions) {
      final date = AppFormatters.formatDateShort(tx.date);
      final cat = _escapeCsv(tx.categoryName);
      final type = _escapeCsv(tx.type.name.toUpperCase());
      final title = _escapeCsv(tx.title);
      final note = _escapeCsv(tx.description ?? '');
      final amount = tx.amount.toString();
      final acc = _escapeCsv(tx.accountName);

      buffer.writeln('$date,$cat,$type,$title,$note,$amount,$acc');
    }

    // 3. Dapatkan direktori sementara
    final directory = await getTemporaryDirectory();
    final fileName = 'FinFlow_Report_$periodName.csv';
    final path = '${directory.path}/$fileName';

    // 4. Simpan file
    final file = File(path);
    await file.writeAsString(buffer.toString());

    // 5. Share file
    // ignore: deprecated_member_use
    await Share.shareXFiles(
      [XFile(path)],
      text: 'Berikut adalah laporan transaksi FinFlow Anda untuk periode $periodName.',
    );
  }

  static String _escapeCsv(String value) {
    if (value.contains(',') || value.contains('"') || value.contains('\n')) {
      final escaped = value.replaceAll('"', '""');
      return '"$escaped"';
    }
    return value;
  }
}
