import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/constants/app_constants.dart';
import '../../../core/services/firebase_service.dart';
import '../../../shared/models/budget_model.dart';
import '../../../shared/models/category_model.dart';

class BudgetRepository {
  String get _uid => FirebaseService.currentUser!.uid;

  CollectionReference<Map<String, dynamic>> get _col =>
      FirebaseService.userCollection(_uid, AppConstants.budgetsCollection);

  // ─── Watch ─────────────────────────────────────────────────────────────────
  Stream<List<BudgetModel>> watchBudgets(DateTime month) {
    return _col
        .where('month',
            isEqualTo: '${month.year}-${month.month.toString().padLeft(2, '0')}')
        .snapshots()
        .map((snap) =>
            snap.docs.map((d) => BudgetModel.fromJson(d.data())).toList());
  }

  // ─── Create / Update ───────────────────────────────────────────────────────
  Future<void> saveBudget(BudgetModel budget) async {
    await _col.doc(budget.id).set(budget.toJson());
  }

  // ─── Delete ────────────────────────────────────────────────────────────────
  Future<void> deleteBudget(String id) async {
    await _col.doc(id).delete();
  }

  // ─── Update Spent ──────────────────────────────────────────────────────────
  Future<void> updateSpent(
      String budgetId, double newSpentAmount) async {
    await _col.doc(budgetId).update({'spent': newSpentAmount});
  }
}

// ─── Category Repository ──────────────────────────────────────────────────────
class CategoryRepository {
  String get _uid => FirebaseService.currentUser!.uid;

  CollectionReference<Map<String, dynamic>> get _col =>
      FirebaseService.userCollection(_uid, AppConstants.categoriesCollection);

  Stream<List<CategoryModel>> watchCategories({String? type}) {
    Query<Map<String, dynamic>> q = _col;
    if (type != null) q = q.where('type', isEqualTo: type);
    return q.snapshots().map((snap) =>
        snap.docs.map((d) => CategoryModel.fromJson(d.data())).toList());
  }

  Future<void> saveCategory(CategoryModel cat) async {
    await _col.doc(cat.id).set(cat.toJson());
  }

  Future<void> deleteCategory(String id) async {
    await _col.doc(id).delete();
  }
}

// ─── Providers ────────────────────────────────────────────────────────────────
final budgetRepositoryProvider =
    Provider<BudgetRepository>((ref) => BudgetRepository());

final categoryRepositoryProvider =
    Provider<CategoryRepository>((ref) => CategoryRepository());

final budgetsProvider =
    StreamProvider.family<List<BudgetModel>, DateTime>((ref, month) {
  return ref.read(budgetRepositoryProvider).watchBudgets(month);
});

final categoriesProvider =
    StreamProvider.family<List<CategoryModel>, String>((ref, type) {
  return ref.read(categoryRepositoryProvider).watchCategories(type: type);
});

final allCategoriesProvider = StreamProvider<List<CategoryModel>>((ref) {
  return ref.read(categoryRepositoryProvider).watchCategories();
});
