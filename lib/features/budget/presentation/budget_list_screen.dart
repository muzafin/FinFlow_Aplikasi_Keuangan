import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_typography.dart';
import '../../../core/utils/formatters.dart';
import '../../../shared/models/budget_model.dart';
import '../data/budget_repository.dart';
import '../../../shared/widgets/category_icon.dart';

/// Budget List Screen — daftar anggaran per kategori
class BudgetListScreen extends ConsumerStatefulWidget {
  const BudgetListScreen({super.key});

  @override
  ConsumerState<BudgetListScreen> createState() => _BudgetListScreenState();
}

class _BudgetListScreenState extends ConsumerState<BudgetListScreen> {
  DateTime _currentMonth = DateTime(DateTime.now().year, DateTime.now().month, 1);

  void _nextMonth() {
    setState(() => _currentMonth = DateTime(_currentMonth.year, _currentMonth.month + 1, 1));
  }

  void _prevMonth() {
    setState(() => _currentMonth = DateTime(_currentMonth.year, _currentMonth.month - 1, 1));
  }

  @override
  Widget build(BuildContext context) {
    final budgetsAsync = ref.watch(budgetsProvider(_currentMonth));

    return Scaffold(
      backgroundColor: AppColors.bgLight,
      appBar: AppBar(
        title: Text('Anggaran', style: AppTypography.sectionTitle),
        centerTitle: true,
        backgroundColor: AppColors.surfaceContainerLowest,
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.add_rounded),
            onPressed: () {
              context.push('/budget/add');
            },
          ),
        ],
      ),
      body: Column(
        children: [
          // Month navigation
          Container(
            color: AppColors.surfaceContainerLowest,
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                IconButton(
                  icon: const Icon(Icons.chevron_left_rounded),
                  onPressed: _prevMonth,
                ),
                Text(AppFormatters.formatMonthYear(_currentMonth),
                    style: AppTypography.bodyMain
                        .copyWith(fontWeight: FontWeight.w600)),
                IconButton(
                  icon: const Icon(Icons.chevron_right_rounded),
                  onPressed: _nextMonth,
                ),
              ],
            ),
          ),
          
          Expanded(
            child: budgetsAsync.when(
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (err, _) => Center(child: Text('Gagal memuat anggaran: $err')),
              data: (budgets) {
                if (budgets.isEmpty) return _buildEmptyState();
                return _buildBudgetList(budgets);
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 80, height: 80,
            decoration: BoxDecoration(
              color: AppColors.primary.withValues(alpha: 0.1),
              borderRadius: BorderRadius.circular(20),
            ),
            child: const Icon(Icons.pie_chart_outline_rounded,
                color: AppColors.primary, size: 40),
          ),
          const SizedBox(height: 20),
          Text('Belum ada anggaran', style: AppTypography.sectionTitle),
          const SizedBox(height: 8),
          Text(
            'Buat anggaran untuk memantau\npengeluaran per kategori',
            style: AppTypography.bodyMain.copyWith(
                color: AppColors.onSurfaceVariant),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 24),
          ElevatedButton.icon(
            onPressed: () => context.push('/budget/add'),
            icon: const Icon(Icons.add_rounded, size: 20),
            label: const Text('Buat Anggaran'),
          ),
        ],
      ),
    );
  }

  Widget _buildBudgetList(List<BudgetModel> budgets) {
    final totalBudget = budgets.fold(0.0, (sum, b) => sum + b.limit);
    final totalSpent = budgets.fold(0.0, (sum, b) => sum + b.spent);
    final healthPercent = totalBudget > 0
        ? (totalSpent / totalBudget * 100).clamp(0.0, 100.0)
        : 0.0;

    return ListView(
      padding: const EdgeInsets.all(20),
      children: [
        // Summary header
        Container(
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            color: AppColors.surfaceContainerLowest,
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: AppColors.outlineVariant, width: 0.5),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withValues(alpha: 0.03),
                blurRadius: 10, offset: const Offset(0, 4),
              ),
            ],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text('Total Anggaran', style: AppTypography.bodyMain
                      .copyWith(color: AppColors.onSurfaceVariant)),
                  Text(AppFormatters.formatRupiah(totalBudget),
                      style: AppTypography.amountMd),
                ],
              ),
              const SizedBox(height: 8),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text('Terpakai', style: AppTypography.bodyMain
                      .copyWith(color: AppColors.onSurfaceVariant)),
                  Text(AppFormatters.formatRupiah(totalSpent),
                      style: AppTypography.amountSm
                          .copyWith(color: AppColors.expense)),
                ],
              ),
              const SizedBox(height: 12),
              ClipRRect(
                borderRadius: BorderRadius.circular(4),
                child: LinearProgressIndicator(
                  value: healthPercent / 100,
                  minHeight: 8,
                  backgroundColor: AppColors.surfaceContainerHigh,
                  valueColor: AlwaysStoppedAnimation<Color>(
                    healthPercent >= 100
                        ? AppColors.expense
                        : healthPercent >= 80
                            ? AppColors.foodAccent
                            : AppColors.income,
                  ),
                ),
              ),
              const SizedBox(height: 4),
              Text(
                '${AppFormatters.formatPercentAbs(healthPercent)} dari total anggaran',
                style: AppTypography.metaData.copyWith(
                    color: AppColors.onSurfaceVariant),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        ...budgets.map((b) => _BudgetCard(budget: b)),
      ],
    );
  }
}

class _BudgetCard extends StatelessWidget {
  final BudgetModel budget;
  const _BudgetCard({required this.budget});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () => context.push('/budget/add', extra: budget),
      child: Container(
        margin: const EdgeInsets.only(bottom: 12),
        padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.surfaceContainerLowest,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.outlineVariant, width: 0.5),
      ),
      child: Column(
        children: [
          Row(
            children: [
              CategoryIcon(categoryName: budget.categoryName),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(budget.categoryName,
                        style: AppTypography.bodyMain
                            .copyWith(fontWeight: FontWeight.w600)),
                    Text(
                      '${AppFormatters.formatRupiah(budget.spent)} / '
                      '${AppFormatters.formatRupiah(budget.limit)}',
                      style: AppTypography.metaData.copyWith(
                          color: AppColors.onSurfaceVariant),
                    ),
                  ],
                ),
              ),
              Text(
                AppFormatters.formatPercentAbs(budget.percentUsed),
                style: AppTypography.bodyMain.copyWith(
                    color: budget.statusColor,
                    fontWeight: FontWeight.w700),
              ),
            ],
          ),
          const SizedBox(height: 12),
          ClipRRect(
            borderRadius: BorderRadius.circular(4),
            child: LinearProgressIndicator(
              value: (budget.percentUsed / 100).clamp(0.0, 1.0),
              minHeight: 6,
              backgroundColor: AppColors.surfaceContainerHigh,
              valueColor: AlwaysStoppedAnimation<Color>(budget.statusColor),
            ),
          ),
          if (budget.isOverBudget) ...[
            const SizedBox(height: 8),
            Row(
              children: [
                Icon(Icons.warning_amber_rounded,
                    color: AppColors.expense, size: 14),
                const SizedBox(width: 4),
                Text(
                  'Melebihi anggaran ${AppFormatters.formatRupiah(budget.spent - budget.limit)}',
                  style: AppTypography.metaData
                      .copyWith(color: AppColors.expense),
                ),
              ],
            ),
          ],
        ],
      ),
    ));
  }
}
