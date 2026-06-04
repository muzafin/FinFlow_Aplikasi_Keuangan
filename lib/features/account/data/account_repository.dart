import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/constants/app_constants.dart';
import '../../../core/services/firebase_service.dart';
import '../../../shared/models/account_model.dart';

class AccountRepository {
  String get _uid => FirebaseService.currentUser!.uid;

  CollectionReference<Map<String, dynamic>> get _col =>
      FirebaseService.userCollection(_uid, AppConstants.accountsCollection);

  // ─── Watch ─────────────────────────────────────────────────────────────────
  Stream<List<AccountModel>> watchAccounts() {
    return _col.orderBy('order').snapshots().map((snap) =>
        snap.docs.map((d) => AccountModel.fromJson(d.data())).toList());
  }

  // ─── Create / Update ───────────────────────────────────────────────────────
  Future<void> saveAccount(AccountModel account) async {
    await _col.doc(account.id).set(account.toJson());
  }

  // ─── Delete ────────────────────────────────────────────────────────────────
  Future<void> deleteAccount(String id) async {
    await _col.doc(id).delete();
  }

  // ─── Total Balance ─────────────────────────────────────────────────────────
  Future<double> getTotalBalance() async {
    final snap = await _col.get();
    double total = 0;
    for (final d in snap.docs) {
      final acc = AccountModel.fromJson(d.data());
      total += acc.balance;
    }
    return total;
  }
}

final accountRepositoryProvider = Provider<AccountRepository>(
  (ref) => AccountRepository(),
);

final accountsProvider = StreamProvider<List<AccountModel>>((ref) {
  return ref.read(accountRepositoryProvider).watchAccounts();
});

final totalBalanceProvider = FutureProvider<double>((ref) {
  return ref.read(accountRepositoryProvider).getTotalBalance();
});
