import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:equatable/equatable.dart';
import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';

enum BudgetPeriod { weekly, monthly }

/// Model anggaran per kategori
class BudgetModel extends Equatable {
  final String id;
  final String categoryId;
  final String categoryName;
  final String categoryIcon;
  final int categoryColor;
  final double limit;
  final double spent;
  final BudgetPeriod period;
  final DateTime startDate;
  final DateTime endDate;
  final bool rollover;
  final int notifyAt; // persentase untuk notifikasi, default 80
  final DateTime createdAt;
  final String month; // format: 'YYYY-MM'

  const BudgetModel({
    required this.id,
    required this.categoryId,
    required this.categoryName,
    required this.categoryIcon,
    required this.categoryColor,
    required this.limit,
    this.spent = 0,
    this.period = BudgetPeriod.monthly,
    required this.startDate,
    required this.endDate,
    this.rollover = false,
    this.notifyAt = 80,
    required this.createdAt,
    required this.month,
  });

  // ─── Computed ─────────────────────────────────────────────────────────────
  double get percentUsed => limit <= 0 ? 0 : (spent / limit * 100).clamp(0, 999);
  double get remaining => limit - spent;
  bool get isOverBudget => spent > limit;

  Color get statusColor {
    if (percentUsed >= 100) return AppColors.expense;
    if (percentUsed >= 90) return const Color(0xFFF5A623); // orange
    if (percentUsed >= 70) return const Color(0xFFFFD700); // yellow
    return AppColors.income;
  }

  factory BudgetModel.fromJson(Map<String, dynamic> json) => BudgetModel(
        id: json['id'] as String,
        categoryId: json['categoryId'] as String,
        categoryName: json['categoryName'] as String? ?? '',
        categoryIcon: json['categoryIcon'] as String? ?? 'category',
        categoryColor: json['categoryColor'] as int? ?? AppColors.primary.toARGB32(),
        limit: (json['limit'] as num).toDouble(),
        spent: (json['spent'] as num?)?.toDouble() ?? 0,
        period: BudgetPeriod.values.firstWhere(
          (e) => e.name == json['period'],
          orElse: () => BudgetPeriod.monthly,
        ),
        startDate: (json['startDate'] as Timestamp).toDate(),
        endDate: (json['endDate'] as Timestamp).toDate(),
        rollover: json['rollover'] as bool? ?? false,
        notifyAt: json['notifyAt'] as int? ?? 80,
        createdAt: (json['createdAt'] as Timestamp).toDate(),
        month: json['month'] as String? ?? '${(json['startDate'] as Timestamp).toDate().year}-${(json['startDate'] as Timestamp).toDate().month.toString().padLeft(2, '0')}',
      );

  Map<String, dynamic> toJson() => {
        'id': id,
        'categoryId': categoryId,
        'categoryName': categoryName,
        'categoryIcon': categoryIcon,
        'categoryColor': categoryColor,
        'limit': limit,
        'spent': spent,
        'period': period.name,
        'startDate': Timestamp.fromDate(startDate),
        'endDate': Timestamp.fromDate(endDate),
        'rollover': rollover,
        'notifyAt': notifyAt,
        'createdAt': Timestamp.fromDate(createdAt),
        'month': month,
      };

  BudgetModel copyWith({
    String? id, String? categoryId, String? categoryName,
    String? categoryIcon, int? categoryColor, double? limit, double? spent,
    BudgetPeriod? period, DateTime? startDate, DateTime? endDate,
    bool? rollover, int? notifyAt, DateTime? createdAt, String? month,
  }) => BudgetModel(
        id: id ?? this.id,
        categoryId: categoryId ?? this.categoryId,
        categoryName: categoryName ?? this.categoryName,
        categoryIcon: categoryIcon ?? this.categoryIcon,
        categoryColor: categoryColor ?? this.categoryColor,
        limit: limit ?? this.limit,
        spent: spent ?? this.spent,
        period: period ?? this.period,
        startDate: startDate ?? this.startDate,
        endDate: endDate ?? this.endDate,
        rollover: rollover ?? this.rollover,
        notifyAt: notifyAt ?? this.notifyAt,
        createdAt: createdAt ?? this.createdAt,
        month: month ?? this.month,
      );

  @override
  List<Object?> get props => [id, categoryId, limit, spent, period, month];
}
