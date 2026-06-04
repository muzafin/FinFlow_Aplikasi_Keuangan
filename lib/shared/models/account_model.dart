import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:equatable/equatable.dart';
import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';

enum AccountType { cash, bank, ewallet, investment, loan }

/// Model akun / dompet keuangan
class AccountModel extends Equatable {
  final String id;
  final String name;
  final AccountType type;
  final double balance;
  final int color;
  final String icon;
  final int order;
  final DateTime createdAt;

  const AccountModel({
    required this.id,
    required this.name,
    required this.type,
    required this.balance,
    required this.color,
    required this.icon,
    this.order = 0,
    required this.createdAt,
  });

  Color get accountColor => Color(color);

  String get typeLabel {
    switch (type) {
      case AccountType.cash: return 'Kas';
      case AccountType.bank: return 'Bank';
      case AccountType.ewallet: return 'E-Wallet';
      case AccountType.investment: return 'Investasi';
      case AccountType.loan: return 'Pinjaman';
    }
  }

  factory AccountModel.fromJson(Map<String, dynamic> json) => AccountModel(
        id: json['id'] as String,
        name: json['name'] as String,
        type: AccountType.values.firstWhere(
          (e) => e.name == json['type'],
          orElse: () => AccountType.cash,
        ),
        balance: (json['balance'] as num?)?.toDouble() ?? 0,
        color: json['color'] as int? ?? AppColors.primary.toARGB32(),
        icon: json['icon'] as String? ?? 'account_balance_wallet',
        order: json['order'] as int? ?? 0,
        createdAt: json['createdAt'] is Timestamp
            ? (json['createdAt'] as Timestamp).toDate()
            : DateTime.now(),
      );

  Map<String, dynamic> toJson() => {
        'id': id,
        'name': name,
        'type': type.name,
        'balance': balance,
        'color': color,
        'icon': icon,
        'order': order,
        'createdAt': Timestamp.fromDate(createdAt),
      };

  AccountModel copyWith({
    String? id, String? name, AccountType? type,
    double? balance, int? color, String? icon,
    int? order, DateTime? createdAt,
  }) => AccountModel(
        id: id ?? this.id,
        name: name ?? this.name,
        type: type ?? this.type,
        balance: balance ?? this.balance,
        color: color ?? this.color,
        icon: icon ?? this.icon,
        order: order ?? this.order,
        createdAt: createdAt ?? this.createdAt,
      );

  @override
  List<Object?> get props => [id, name, type, balance];
}

// ─── Default Account ──────────────────────────────────────────────────────────
class DefaultAccounts {
  DefaultAccounts._();

  static AccountModel get cash => AccountModel(
        id: 'acc_cash',
        name: 'Kas',
        type: AccountType.cash,
        balance: 0,
        color: AppColors.income.toARGB32(),
        icon: 'account_balance_wallet',
        order: 0,
        createdAt: DateTime.now(),
      );
}
