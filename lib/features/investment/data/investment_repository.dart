import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/constants/app_constants.dart';
import '../../../core/services/firebase_service.dart';
import '../../../shared/models/investment_model.dart';

class InvestmentRepository {
  String get _uid => FirebaseService.currentUser!.uid;

  CollectionReference<Map<String, dynamic>> get _col =>
      FirebaseService.userCollection(_uid, AppConstants.investmentsCollection);

  // ─── Watch ─────────────────────────────────────────────────────────────────
  Stream<List<InvestmentModel>> watchInvestments() {
    return _col.orderBy('createdAt', descending: true).snapshots().map((snap) =>
        snap.docs.map((d) => InvestmentModel.fromJson(d.data())).toList());
  }

  // ─── Create / Update ───────────────────────────────────────────────────────
  Future<void> saveInvestment(InvestmentModel inv) async {
    await _col.doc(inv.id).set(inv.toJson());
  }

  // ─── Delete ────────────────────────────────────────────────────────────────
  Future<void> deleteInvestment(String id) async {
    await _col.doc(id).delete();
  }

  // ─── Update Current Value ──────────────────────────────────────────────────
  Future<void> updateCurrentValue(String id, double newValue) async {
    await _col.doc(id).update({
      'currentValue': newValue,
      'updatedAt': Timestamp.now(),
    });
  }
}

// ─── Providers ────────────────────────────────────────────────────────────────
final investmentRepositoryProvider =
    Provider<InvestmentRepository>((ref) => InvestmentRepository());

final investmentsProvider = StreamProvider<List<InvestmentModel>>((ref) {
  return ref.read(investmentRepositoryProvider).watchInvestments();
});
