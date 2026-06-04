import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:equatable/equatable.dart';
import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';

/// Model target keuangan (financial goals)
class GoalModel extends Equatable {
  final String id;
  final String name;
  final double targetAmount;
  final double currentAmount;
  final DateTime targetDate;
  final String? accountId;
  final String icon;
  final int color;
  final String? notes;
  final DateTime createdAt;

  const GoalModel({
    required this.id,
    required this.name,
    required this.targetAmount,
    this.currentAmount = 0,
    required this.targetDate,
    this.accountId,
    this.icon = 'flag',
    required this.color,
    this.notes,
    required this.createdAt,
  });

  // ─── Computed ─────────────────────────────────────────────────────────────
  double get progressPercent =>
      targetAmount <= 0 ? 0 : (currentAmount / targetAmount * 100).clamp(0, 100);
  double get remainingAmount => (targetAmount - currentAmount).clamp(0, double.infinity);
  bool get isAchieved => currentAmount >= targetAmount;
  int get daysRemaining => targetDate.difference(DateTime.now()).inDays;
  Color get goalColor => Color(color);

  factory GoalModel.fromJson(Map<String, dynamic> json) => GoalModel(
        id: json['id'] as String,
        name: json['name'] as String,
        targetAmount: (json['targetAmount'] as num).toDouble(),
        currentAmount: (json['currentAmount'] as num?)?.toDouble() ?? 0,
        targetDate: (json['targetDate'] as Timestamp).toDate(),
        accountId: json['accountId'] as String?,
        icon: json['icon'] as String? ?? 'flag',
        color: json['color'] as int? ?? AppColors.primary.toARGB32(),
        notes: json['notes'] as String?,
        createdAt: (json['createdAt'] as Timestamp).toDate(),
      );

  Map<String, dynamic> toJson() => {
        'id': id,
        'name': name,
        'targetAmount': targetAmount,
        'currentAmount': currentAmount,
        'targetDate': Timestamp.fromDate(targetDate),
        'accountId': accountId,
        'icon': icon,
        'color': color,
        'notes': notes,
        'createdAt': Timestamp.fromDate(createdAt),
      };

  GoalModel copyWith({
    String? id, String? name, double? targetAmount, double? currentAmount,
    DateTime? targetDate, String? accountId, String? icon, int? color,
    String? notes, DateTime? createdAt,
  }) => GoalModel(
        id: id ?? this.id,
        name: name ?? this.name,
        targetAmount: targetAmount ?? this.targetAmount,
        currentAmount: currentAmount ?? this.currentAmount,
        targetDate: targetDate ?? this.targetDate,
        accountId: accountId ?? this.accountId,
        icon: icon ?? this.icon,
        color: color ?? this.color,
        notes: notes ?? this.notes,
        createdAt: createdAt ?? this.createdAt,
      );

  @override
  List<Object?> get props => [id, name, targetAmount, currentAmount];
}
