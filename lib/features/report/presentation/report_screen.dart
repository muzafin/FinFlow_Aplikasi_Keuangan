import 'package:flutter/material.dart';
import 'package:fl_chart/fl_chart.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_typography.dart';
import '../../../core/utils/formatters.dart';
import '../../transaction/data/transaction_repository.dart';
import '../../../shared/models/transaction_model.dart';
import 'export_service.dart';

/// Report / Statistik screen — live data from Firestore
class ReportScreen extends ConsumerStatefulWidget {
  const ReportScreen({super.key});

  @override
  ConsumerState<ReportScreen> createState() => _ReportScreenState();
}

class _ReportScreenState extends ConsumerState<ReportScreen> {
  String _period = 'Bulanan';
  bool _showIncome = false; // false = expense, true = income

  final _periods = ['Mingguan', 'Bulanan', 'Tahunan'];
  DateTime _currentMonth = DateTime(DateTime.now().year, DateTime.now().month, 1);

  void _nextMonth() {
    setState(() => _currentMonth = DateTime(_currentMonth.year, _currentMonth.month + 1, 1));
  }

  void _prevMonth() {
    setState(() => _currentMonth = DateTime(_currentMonth.year, _currentMonth.month - 1, 1));
  }

  @override
  Widget build(BuildContext context) {
    final txAsync = ref.watch(monthlyTransactionsProvider(_currentMonth));

    return SafeArea(
      child: Column(
        children: [
          // Header
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 16, 20, 0),
            child: Row(
              children: [
                Text('Statistik', style: AppTypography.appTitle),
                const Spacer(),
                // Period selector
                Container(
                  height: 36,
                  padding: const EdgeInsets.symmetric(horizontal: 12),
                  decoration: BoxDecoration(
                    color: AppColors.surfaceContainerLow,
                    borderRadius: BorderRadius.circular(99),
                  ),
                  child: DropdownButtonHideUnderline(
                    child: DropdownButton<String>(
                      value: _period,
                      items: _periods
                          .map((p) => DropdownMenuItem(
                                value: p,
                                child: Text(p,
                                    style: AppTypography.metaData.copyWith(
                                        fontWeight: FontWeight.w600)),
                              ))
                          .toList(),
                      onChanged: (v) => setState(() => _period = v!),
                      style: AppTypography.metaData,
                      icon: const Icon(Icons.expand_more_rounded, size: 18),
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                // Export button
                IconButton(
                  icon: const Icon(Icons.file_download_outlined),
                  onPressed: () {
                    final txAsync = ref.read(monthlyTransactionsProvider(_currentMonth));
                    txAsync.whenData((transactions) {
                      if (transactions.isEmpty) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(content: Text('Tidak ada data untuk diekspor')),
                        );
                        return;
                      }
                      ExportService.exportAndShareCSV(
                        transactions,
                        '${_currentMonth.year}_${_currentMonth.month.toString().padLeft(2, '0')}',
                      );
                    });
                  },
                ),
              ],
            ),
          ),
          const SizedBox(height: 12),

          // Month navigation
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20),
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

          // Income/Expense toggle
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20),
            child: Container(
              height: 40,
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLow,
                borderRadius: BorderRadius.circular(99),
              ),
              child: Row(
                children: [
                  _ToggleTab(
                    label: 'Pengeluaran',
                    isActive: !_showIncome,
                    activeColor: AppColors.expense,
                    onTap: () => setState(() => _showIncome = false),
                  ),
                  _ToggleTab(
                    label: 'Pemasukan',
                    isActive: _showIncome,
                    activeColor: AppColors.income,
                    onTap: () => setState(() => _showIncome = true),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 20),

          // Main content
          Expanded(
            child: txAsync.when(
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (err, _) => Center(child: Text('Gagal memuat data statistik: $err')),
              data: (transactions) {
                // Filter transactions by type
                final targetType = _showIncome ? TransactionType.income : TransactionType.expense;
                final filteredTx = transactions.where((t) => t.type == targetType).toList();
                
                // Group by category
                final Map<String, double> categoryTotals = {};
                double totalAmount = 0;
                for (var tx in filteredTx) {
                  categoryTotals[tx.categoryName] = (categoryTotals[tx.categoryName] ?? 0) + tx.amount;
                  totalAmount += tx.amount;
                }

                if (totalAmount == 0) {
                  return _buildEmptyState();
                }

                // Sort categories by amount descending
                final sortedCategories = categoryTotals.entries.toList()
                  ..sort((a, b) => b.value.compareTo(a.value));

                return SingleChildScrollView(
                  padding: const EdgeInsets.symmetric(horizontal: 20),
                  child: Column(
                    children: [
                      // Donut chart
                      _buildDonutChart(sortedCategories, totalAmount),
                      const SizedBox(height: 24),

                      // Category breakdown
                      _buildCategoryList(sortedCategories, totalAmount),
                      const SizedBox(height: 100),
                    ],
                  ),
                );
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
          Icon(Icons.pie_chart_outline_rounded, size: 64, color: AppColors.outlineVariant),
          const SizedBox(height: 16),
          Text('Belum ada data statistik', style: AppTypography.sectionTitle),
          Text('Data akan muncul setelah ada transaksi', style: AppTypography.metaData.copyWith(color: AppColors.outline)),
        ],
      ),
    );
  }

  Color _getColor(String name) {
    final lower = name.toLowerCase();
    if (lower.contains('makanan') || lower.contains('food')) return AppColors.foodAccent;
    if (lower.contains('transport') || lower.contains('bensin')) return AppColors.transportAccent;
    if (lower.contains('belanja') || lower.contains('shopping')) return AppColors.shoppingAccent;
    if (lower.contains('gaji') || lower.contains('salary')) return AppColors.income;
    if (lower.contains('tagihan') || lower.contains('bill')) return AppColors.expense;
    if (lower.contains('hiburan') || lower.contains('entertainment')) return const Color(0xFF8E44AD);
    if (lower.contains('kesehatan') || lower.contains('health')) return AppColors.primaryContainer;
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

  Widget _buildDonutChart(List<MapEntry<String, double>> categories, double totalAmount) {
    final sections = categories.map((e) {
      final pct = totalAmount > 0 ? e.value / totalAmount * 100 : 0.0;
      final color = _getColor(e.key);
      return PieChartSectionData(
        value: e.value,
        color: color,
        title: pct > 5 ? '${pct.toStringAsFixed(0)}%' : '',
        radius: 55,
        titleStyle: AppTypography.badgeLabel
            .copyWith(color: Colors.white, fontSize: 11),
      );
    }).toList();

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: AppColors.surfaceContainerLowest,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: AppColors.outlineVariant, width: 0.5),
      ),
      child: Column(
        children: [
          SizedBox(
            height: 200,
            child: Stack(
              alignment: Alignment.center,
              children: [
                PieChart(
                  PieChartData(
                    sections: sections,
                    centerSpaceRadius: 65,
                    sectionsSpace: 3,
                    startDegreeOffset: -90,
                  ),
                ),
                SizedBox(
                  width: 120, // constrain width to prevent overlap
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        _showIncome ? 'Total Pemasukan' : 'Total Pengeluaran',
                        style: AppTypography.metaData
                            .copyWith(color: AppColors.onSurfaceVariant, fontSize: 10),
                        textAlign: TextAlign.center,
                        maxLines: 2,
                      ),
                      const SizedBox(height: 4),
                      FittedBox(
                        fit: BoxFit.scaleDown,
                        child: Text(
                          AppFormatters.formatRupiahCompact(totalAmount),
                          style: AppTypography.amountMd.copyWith(
                              color: _showIncome
                                  ? AppColors.income
                                  : AppColors.expense),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          // Legend
          Wrap(
            spacing: 16, runSpacing: 8,
            children: categories.map((c) {
              final color = _getColor(c.key);
              return Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Container(
                    width: 10, height: 10,
                    decoration: BoxDecoration(
                        color: color, shape: BoxShape.circle),
                  ),
                  const SizedBox(width: 6),
                  Text(c.key,
                      style: AppTypography.metaData
                          .copyWith(color: AppColors.onSurfaceVariant)),
                ],
              );
            }).toList(),
          ),
        ],
      ),
    );
  }

  Widget _buildCategoryList(List<MapEntry<String, double>> categories, double totalAmount) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Rincian Kategori', style: AppTypography.sectionTitle),
        const SizedBox(height: 12),
        ...categories.map((c) {
          final pct = totalAmount > 0 ? c.value / totalAmount * 100 : 0.0;
          final color = _getColor(c.key);
          final icon = _getIcon(c.key);
          
          return Container(
            margin: const EdgeInsets.only(bottom: 10),
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: AppColors.surfaceContainerLowest,
              borderRadius: BorderRadius.circular(14),
              border: Border.all(color: AppColors.outlineVariant, width: 0.5),
            ),
            child: Column(
              children: [
                Row(
                  children: [
                    Container(
                      width: 40, height: 40,
                      decoration: BoxDecoration(
                        color: color.withValues(alpha: 0.12),
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: Icon(icon, color: color, size: 20),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Text(c.key,
                          style: AppTypography.bodyMain
                              .copyWith(fontWeight: FontWeight.w600)),
                    ),
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: [
                        Text(AppFormatters.formatRupiah(c.value),
                            style: AppTypography.amountSm.copyWith(
                              color: _showIncome ? AppColors.income : AppColors.expense,
                            )),
                        Text(AppFormatters.formatPercentAbs(pct),
                            style: AppTypography.metaData.copyWith(
                                color: AppColors.onSurfaceVariant)),
                      ],
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                ClipRRect(
                  borderRadius: BorderRadius.circular(3),
                  child: LinearProgressIndicator(
                    value: pct / 100,
                    minHeight: 5,
                    backgroundColor: AppColors.surfaceContainerHigh,
                    valueColor: AlwaysStoppedAnimation<Color>(color),
                  ),
                ),
              ],
            ),
          );
        }),
      ],
    );
  }
}

class _ToggleTab extends StatelessWidget {
  final String label;
  final bool isActive;
  final Color activeColor;
  final VoidCallback onTap;

  const _ToggleTab({
    required this.label,
    required this.isActive,
    required this.activeColor,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: GestureDetector(
        onTap: onTap,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 200),
          margin: const EdgeInsets.all(3),
          decoration: BoxDecoration(
            color: isActive ? activeColor : Colors.transparent,
            borderRadius: BorderRadius.circular(99),
          ),
          child: Center(
            child: Text(
              label,
              style: AppTypography.badgeLabel.copyWith(
                color: isActive ? Colors.white : AppColors.onSurfaceVariant,
                fontWeight:
                    isActive ? FontWeight.w700 : FontWeight.w500,
              ),
            ),
          ),
        ),
      ),
    );
  }
}
