import 'package:equatable/equatable.dart';
import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';

enum CategoryType { income, expense }

/// Model kategori transaksi
class CategoryModel extends Equatable {
  final String id;
  final String name;
  final String icon;       // material icon name
  final int color;         // Color.value (int)
  final CategoryType type;
  final bool isDefault;
  final bool isHidden;
  final int order;

  const CategoryModel({
    required this.id,
    required this.name,
    required this.icon,
    required this.color,
    required this.type,
    this.isDefault = false,
    this.isHidden = false,
    this.order = 0,
  });

  Color get categoryColor => Color(color);

  factory CategoryModel.fromJson(Map<String, dynamic> json) => CategoryModel(
        id: json['id'] as String,
        name: json['name'] as String,
        icon: json['icon'] as String? ?? 'category',
        color: json['color'] as int? ?? AppColors.primary.toARGB32(),
        type: CategoryType.values.firstWhere(
          (e) => e.name == json['type'],
          orElse: () => CategoryType.expense,
        ),
        isDefault: json['isDefault'] as bool? ?? false,
        isHidden: json['isHidden'] as bool? ?? false,
        order: json['order'] as int? ?? 0,
      );

  Map<String, dynamic> toJson() => {
        'id': id,
        'name': name,
        'icon': icon,
        'color': color,
        'type': type.name,
        'isDefault': isDefault,
        'isHidden': isHidden,
        'order': order,
      };

  CategoryModel copyWith({
    String? id, String? name, String? icon, int? color,
    CategoryType? type, bool? isDefault, bool? isHidden, int? order,
  }) => CategoryModel(
        id: id ?? this.id,
        name: name ?? this.name,
        icon: icon ?? this.icon,
        color: color ?? this.color,
        type: type ?? this.type,
        isDefault: isDefault ?? this.isDefault,
        isHidden: isHidden ?? this.isHidden,
        order: order ?? this.order,
      );

  @override
  List<Object?> get props => [id, name, icon, color, type];
}

// ─── Default Categories ───────────────────────────────────────────────────────

class DefaultCategories {
  DefaultCategories._();

  static List<CategoryModel> get income => [
    CategoryModel(id: 'inc_gaji', name: 'Gaji', icon: 'attach_money',
        color: AppColors.income.toARGB32(), type: CategoryType.income,
        isDefault: true, order: 0),
    CategoryModel(id: 'inc_freelance', name: 'Freelance', icon: 'work_outline',
        color: AppColors.secondary.toARGB32(), type: CategoryType.income,
        isDefault: true, order: 1),
    CategoryModel(id: 'inc_bisnis', name: 'Bisnis', icon: 'store',
        color: AppColors.tertiary.toARGB32(), type: CategoryType.income,
        isDefault: true, order: 2),
    CategoryModel(id: 'inc_investasi', name: 'Investasi', icon: 'trending_up',
        color: AppColors.income.toARGB32(), type: CategoryType.income,
        isDefault: true, order: 3),
    CategoryModel(id: 'inc_hadiah', name: 'Hadiah', icon: 'card_giftcard',
        color: AppColors.shoppingAccent.toARGB32(), type: CategoryType.income,
        isDefault: true, order: 4),
    CategoryModel(id: 'inc_lainnya', name: 'Lainnya', icon: 'more_horiz',
        color: AppColors.onSurfaceVariant.toARGB32(), type: CategoryType.income,
        isDefault: true, order: 5),
  ];

  static List<CategoryModel> get expense => [
    CategoryModel(id: 'exp_makan', name: 'Makan & Minum', icon: 'restaurant',
        color: AppColors.foodAccent.toARGB32(), type: CategoryType.expense,
        isDefault: true, order: 0),
    CategoryModel(id: 'exp_transport', name: 'Transportasi', icon: 'directions_bus',
        color: AppColors.transportAccent.toARGB32(), type: CategoryType.expense,
        isDefault: true, order: 1),
    CategoryModel(id: 'exp_belanja', name: 'Belanja', icon: 'local_mall',
        color: AppColors.shoppingAccent.toARGB32(), type: CategoryType.expense,
        isDefault: true, order: 2),
    CategoryModel(id: 'exp_hiburan', name: 'Hiburan', icon: 'movie',
        color: AppColors.entertainmentAccent.toARGB32(), type: CategoryType.expense,
        isDefault: true, order: 3),
    CategoryModel(id: 'exp_kesehatan', name: 'Kesehatan', icon: 'local_hospital',
        color: AppColors.healthAccent.toARGB32(), type: CategoryType.expense,
        isDefault: true, order: 4),
    CategoryModel(id: 'exp_pendidikan', name: 'Pendidikan', icon: 'school',
        color: AppColors.educationAccent.toARGB32(), type: CategoryType.expense,
        isDefault: true, order: 5),
    CategoryModel(id: 'exp_tagihan', name: 'Tagihan', icon: 'receipt_long',
        color: AppColors.error.toARGB32(), type: CategoryType.expense,
        isDefault: true, order: 6),
    CategoryModel(id: 'exp_rumah', name: 'Rumah', icon: 'home',
        color: AppColors.housingAccent.toARGB32(), type: CategoryType.expense,
        isDefault: true, order: 7),
    CategoryModel(id: 'exp_tabungan', name: 'Tabungan', icon: 'savings',
        color: AppColors.savingsAccent.toARGB32(), type: CategoryType.expense,
        isDefault: true, order: 8),
    CategoryModel(id: 'exp_investasi', name: 'Investasi', icon: 'trending_up',
        color: AppColors.income.toARGB32(), type: CategoryType.expense,
        isDefault: true, order: 9),
    CategoryModel(id: 'exp_lainnya', name: 'Lainnya', icon: 'more_horiz',
        color: AppColors.onSurfaceVariant.toARGB32(), type: CategoryType.expense,
        isDefault: true, order: 10),
  ];

  static List<CategoryModel> get all => [...income, ...expense];
}
