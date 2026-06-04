import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/constants/app_constants.dart';
import '../../../core/services/firebase_service.dart';
import '../../../shared/models/transaction_model.dart';

/// Repository untuk operasi CRUD transaksi di Firestore
class TransactionRepository {
  String get _uid => FirebaseService.currentUser!.uid;

  CollectionReference<Map<String, dynamic>> get _col =>
      FirebaseService.userCollection(_uid, AppConstants.transactionsCollection);

  // ─── Create ────────────────────────────────────────────────────────────────
  Future<void> addTransaction(TransactionModel tx) async {
    await _col.doc(tx.id).set(tx.toJson());

    // Update account balance
    await _updateAccountBalance(tx.accountId, tx.amount, tx.type);
  }

  // ─── Read ──────────────────────────────────────────────────────────────────
  Stream<List<TransactionModel>> watchTransactions({
    DateTime? startDate,
    DateTime? endDate,
    String? categoryId,
    String? accountId,
    TransactionType? type,
    int limit = 50,
  }) {
    Query<Map<String, dynamic>> q = _col.orderBy('date', descending: true);

    if (startDate != null) {
      q = q.where('date',
          isGreaterThanOrEqualTo: Timestamp.fromDate(startDate));
    }
    if (endDate != null) {
      q = q.where('date', isLessThanOrEqualTo: Timestamp.fromDate(endDate));
    }
    if (categoryId != null) {
      q = q.where('categoryId', isEqualTo: categoryId);
    }
    if (accountId != null) {
      q = q.where('accountId', isEqualTo: accountId);
    }
    if (type != null) {
      q = q.where('type', isEqualTo: type.name);
    }

    return q.limit(limit).snapshots().map(
          (snap) => snap.docs
              .map((d) => TransactionModel.fromJson(d.data()))
              .toList(),
        );
  }

  /// Ambil transaksi bulan tertentu
  Stream<List<TransactionModel>> watchMonthlyTransactions(
      DateTime month) {
    final start = DateTime(month.year, month.month, 1);
    final end = DateTime(month.year, month.month + 1, 1);
    return watchTransactions(startDate: start, endDate: end, limit: 200);
  }

  // ─── Update ────────────────────────────────────────────────────────────────
  Future<void> updateTransaction(
    TransactionModel oldTx,
    TransactionModel newTx,
  ) async {
    // Revert old balance effect
    await _updateAccountBalance(
        oldTx.accountId, oldTx.amount, oldTx.type, revert: true);

    // Apply new balance effect
    await _col.doc(newTx.id).set(newTx.toJson());
    await _updateAccountBalance(newTx.accountId, newTx.amount, newTx.type);
  }

  // ─── Delete ────────────────────────────────────────────────────────────────
  Future<void> deleteTransaction(TransactionModel tx) async {
    await _col.doc(tx.id).delete();
    await _updateAccountBalance(tx.accountId, tx.amount, tx.type, revert: true);
  }

  // ─── Statistics ────────────────────────────────────────────────────────────
  /// Summary pemasukan dan pengeluaran untuk periode tertentu
  Future<Map<String, double>> getMonthlySummary(DateTime month) async {
    final start = DateTime(month.year, month.month, 1);
    final end = DateTime(month.year, month.month + 1, 1);

    final snap = await _col
        .where('date', isGreaterThanOrEqualTo: Timestamp.fromDate(start))
        .where('date', isLessThan: Timestamp.fromDate(end))
        .get();

    double income = 0, expense = 0;
    for (final doc in snap.docs) {
      final tx = TransactionModel.fromJson(doc.data());
      if (tx.type == TransactionType.income) {
        income += tx.amount;
      } else if (tx.type == TransactionType.expense) {
        expense += tx.amount;
      }
    }
    return {'income': income, 'expense': expense, 'balance': income - expense};
  }

  // ─── Private ───────────────────────────────────────────────────────────────
  Future<void> _updateAccountBalance(
    String accountId,
    double amount,
    TransactionType type, {
    bool revert = false,
  }) async {
    final accountRef = FirebaseService.userCollection(
            _uid, AppConstants.accountsCollection)
        .doc(accountId);

    double delta;
    switch (type) {
      case TransactionType.income:
        delta = revert ? -amount : amount;
        break;
      case TransactionType.expense:
        delta = revert ? amount : -amount;
        break;
      case TransactionType.transfer:
        delta = 0; // transfer perlu 2 account
        break;
    }

    if (delta != 0) {
      await accountRef.update({'balance': FieldValue.increment(delta)});
    }
  }
}

// ─── Providers ────────────────────────────────────────────────────────────────

final transactionRepositoryProvider = Provider<TransactionRepository>(
  (ref) => TransactionRepository(),
);

/// Stream semua transaksi bulan ini
final monthlyTransactionsProvider =
    StreamProvider.family<List<TransactionModel>, DateTime>((ref, month) {
  final repo = ref.read(transactionRepositoryProvider);
  return repo.watchMonthlyTransactions(month);
});

/// Stream summary keuangan bulan ini
final monthlySummaryProvider =
    FutureProvider.family<Map<String, double>, DateTime>((ref, month) {
  final repo = ref.read(transactionRepositoryProvider);
  return repo.getMonthlySummary(month);
});

/// Stream semua transaksi
final allTransactionsProvider = StreamProvider<List<TransactionModel>>((ref) {
  final repo = ref.read(transactionRepositoryProvider);
  return repo.watchTransactions(limit: 100);
});
