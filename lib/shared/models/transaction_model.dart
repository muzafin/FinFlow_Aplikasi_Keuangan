import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:equatable/equatable.dart';
import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';

enum TransactionType { income, expense, transfer }

enum RecurringPeriod { daily, weekly, monthly, yearly }

/// Model transaksi keuangan
class TransactionModel extends Equatable {
  final String id;
  final TransactionType type;
  final double amount;
  final String categoryId;
  final String categoryName;
  final String categoryIcon;
  final int categoryColor;
  final String title;          // judul transaksi user-defined
  final String? description;   // catatan tambahan
  final DateTime date;
  final String accountId;
  final String accountName;
  final List<String> tags;
  final bool isRecurring;
  final RecurringPeriod? recurringPeriod;
  final String? attachmentUrl;
  final String? toAccountId;    // untuk transfer
  final String? toAccountName;  // untuk transfer
  final DateTime createdAt;
  final DateTime updatedAt;

  const TransactionModel({
    required this.id,
    required this.type,
    required this.amount,
    required this.categoryId,
    required this.categoryName,
    required this.categoryIcon,
    required this.categoryColor,
    this.title = '',
    this.description,
    required this.date,
    required this.accountId,
    required this.accountName,
    this.tags = const [],
    this.isRecurring = false,
    this.recurringPeriod,
    this.attachmentUrl,
    this.toAccountId,
    this.toAccountName,
    required this.createdAt,
    required this.updatedAt,
  });

  /// Warna semantik berdasarkan tipe
  Color get semanticColor {
    switch (type) {
      case TransactionType.income:
        return AppColors.income;
      case TransactionType.expense:
        return AppColors.expense;
      case TransactionType.transfer:
        return AppColors.secondary;
    }
  }

  /// Nominal dengan tanda +/-
  double get signedAmount {
    switch (type) {
      case TransactionType.income:
        return amount;
      case TransactionType.expense:
        return -amount;
      case TransactionType.transfer:
        return -amount;
    }
  }

  factory TransactionModel.fromJson(Map<String, dynamic> json) =>
      TransactionModel(
        id: json['id'] as String,
        type: TransactionType.values.firstWhere(
          (e) => e.name == json['type'],
          orElse: () => TransactionType.expense,
        ),
        amount: (json['amount'] as num).toDouble(),
        categoryId: json['categoryId'] as String? ?? '',
        categoryName: json['categoryName'] as String? ?? '',
        categoryIcon: json['categoryIcon'] as String? ?? 'category',
        categoryColor: json['categoryColor'] as int? ?? 0xFF2D9B6F,
        title: json['title'] as String? ?? '',
        description: json['description'] as String?,
        date: (json['date'] as Timestamp).toDate(),
        accountId: json['accountId'] as String? ?? '',
        accountName: json['accountName'] as String? ?? 'Kas',
        tags: List<String>.from(json['tags'] as List? ?? []),
        isRecurring: json['isRecurring'] as bool? ?? false,
        recurringPeriod: json['recurringPeriod'] != null
            ? RecurringPeriod.values.firstWhere(
                (e) => e.name == json['recurringPeriod'],
                orElse: () => RecurringPeriod.monthly,
              )
            : null,
        attachmentUrl: json['attachmentUrl'] as String?,
        toAccountId: json['toAccountId'] as String?,
        toAccountName: json['toAccountName'] as String?,
        createdAt: (json['createdAt'] as Timestamp).toDate(),
        updatedAt: (json['updatedAt'] as Timestamp).toDate(),
      );

  Map<String, dynamic> toJson() => {
        'id': id,
        'type': type.name,
        'amount': amount,
        'categoryId': categoryId,
        'categoryName': categoryName,
        'categoryIcon': categoryIcon,
        'categoryColor': categoryColor,
        'title': title,
        'description': description,
        'date': Timestamp.fromDate(date),
        'accountId': accountId,
        'accountName': accountName,
        'tags': tags,
        'isRecurring': isRecurring,
        'recurringPeriod': recurringPeriod?.name,
        'attachmentUrl': attachmentUrl,
        'toAccountId': toAccountId,
        'toAccountName': toAccountName,
        'createdAt': Timestamp.fromDate(createdAt),
        'updatedAt': Timestamp.fromDate(updatedAt),
      };

  TransactionModel copyWith({
    String? id,
    TransactionType? type,
    double? amount,
    String? categoryId,
    String? categoryName,
    String? categoryIcon,
    int? categoryColor,
    String? title,
    String? description,
    DateTime? date,
    String? accountId,
    String? accountName,
    List<String>? tags,
    bool? isRecurring,
    RecurringPeriod? recurringPeriod,
    String? attachmentUrl,
    String? toAccountId,
    String? toAccountName,
    DateTime? createdAt,
    DateTime? updatedAt,
  }) =>
      TransactionModel(
        id: id ?? this.id,
        type: type ?? this.type,
        amount: amount ?? this.amount,
        categoryId: categoryId ?? this.categoryId,
        categoryName: categoryName ?? this.categoryName,
        categoryIcon: categoryIcon ?? this.categoryIcon,
        categoryColor: categoryColor ?? this.categoryColor,
        title: title ?? this.title,
        description: description ?? this.description,
        date: date ?? this.date,
        accountId: accountId ?? this.accountId,
        accountName: accountName ?? this.accountName,
        tags: tags ?? this.tags,
        isRecurring: isRecurring ?? this.isRecurring,
        recurringPeriod: recurringPeriod ?? this.recurringPeriod,
        attachmentUrl: attachmentUrl ?? this.attachmentUrl,
        toAccountId: toAccountId ?? this.toAccountId,
        toAccountName: toAccountName ?? this.toAccountName,
        createdAt: createdAt ?? this.createdAt,
        updatedAt: updatedAt ?? this.updatedAt,
      );

  @override
  List<Object?> get props => [id, type, amount, categoryId, date, accountId];
}
