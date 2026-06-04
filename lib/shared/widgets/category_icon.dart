import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';

class CategoryIcon extends StatelessWidget {
  final String categoryName;
  final double size;

  const CategoryIcon({
    super.key,
    required this.categoryName,
    this.size = 44,
  });

  @override
  Widget build(BuildContext context) {
    // Generate a consistent color and icon based on category name
    final color = _getColor(categoryName);
    final icon = _getIcon(categoryName);

    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: color.withValues(alpha: 0.2)),
      ),
      child: Icon(icon, color: color, size: size * 0.5),
    );
  }

  Color _getColor(String name) {
    final lower = name.toLowerCase();
    if (lower.contains('makanan') || lower.contains('food')) return AppColors.foodAccent;
    if (lower.contains('transport') || lower.contains('bensin')) return AppColors.transportAccent;
    if (lower.contains('belanja') || lower.contains('shopping')) return AppColors.shoppingAccent;
    if (lower.contains('gaji') || lower.contains('salary')) return AppColors.income;
    if (lower.contains('tagihan') || lower.contains('bill')) return AppColors.expense;
    return AppColors.primary;
  }

  IconData _getIcon(String name) {
    final lower = name.toLowerCase();
    if (lower.contains('makanan') || lower.contains('food')) return Icons.restaurant_rounded;
    if (lower.contains('transport') || lower.contains('bensin')) return Icons.directions_car_rounded;
    if (lower.contains('belanja') || lower.contains('shopping')) return Icons.shopping_bag_rounded;
    if (lower.contains('gaji') || lower.contains('salary')) return Icons.account_balance_wallet_rounded;
    if (lower.contains('tagihan') || lower.contains('bill')) return Icons.receipt_long_rounded;
    if (lower.contains('kesehatan') || lower.contains('health')) return Icons.medical_services_rounded;
    if (lower.contains('hiburan') || lower.contains('entertainment')) return Icons.movie_rounded;
    return Icons.category_rounded;
  }
}
