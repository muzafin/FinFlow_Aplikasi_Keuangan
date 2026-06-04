import 'package:flutter/material.dart';
import 'package:fl_chart/fl_chart.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_typography.dart';
import '../../../core/utils/formatters.dart';
import '../../../shared/models/investment_model.dart';
import '../data/investment_repository.dart';

/// Investment portfolio overview screen
class InvestmentListScreen extends ConsumerStatefulWidget {
  const InvestmentListScreen({super.key});

  @override
  ConsumerState<InvestmentListScreen> createState() =>
      _InvestmentListScreenState();
}

class _InvestmentListScreenState extends ConsumerState<InvestmentListScreen> {
  List<InvestmentModel> _investments = [];

  double get _totalValue =>
      _investments.fold(0.0, (s, i) => s + i.currentValue);
  double get _totalInitial =>
      _investments.fold(0.0, (s, i) => s + i.initialAmount);
  double get _totalPL => _totalValue - _totalInitial;
  bool get _isProfit => _totalPL >= 0;

  @override
  Widget build(BuildContext context) {
    final investmentsAsync = ref.watch(investmentsProvider);

    return Scaffold(
      backgroundColor: AppColors.bgLight,
      appBar: AppBar(
        title: Text('Investasi', style: AppTypography.sectionTitle),
        actions: [
          IconButton(
            icon: const Icon(Icons.add_rounded),
            onPressed: () => context.push('/investment/add'),
          ),
        ],
      ),
      body: investmentsAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (err, _) => Center(child: Text('Gagal memuat: $err')),
        data: (data) {
          _investments = data;
          if (_investments.isEmpty) return _buildEmpty();
          return _buildPortfolio();
        },
      ),
    );
  }

  Widget _buildEmpty() {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 80,
            height: 80,
            decoration: BoxDecoration(
              color: AppColors.income.withValues(alpha: 0.1),
              borderRadius: BorderRadius.circular(20),
            ),
            child: const Icon(
              Icons.trending_up_rounded,
              color: AppColors.income,
              size: 40,
            ),
          ),
          const SizedBox(height: 20),
          Text('Belum ada investasi', style: AppTypography.sectionTitle),
          const SizedBox(height: 8),
          Text(
            'Pantau portofolio investasimu\ndi satu tempat',
            style: AppTypography.bodyMain.copyWith(
              color: AppColors.onSurfaceVariant,
            ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 24),
          ElevatedButton.icon(
            onPressed: () => context.push('/investment/add'),
            icon: const Icon(Icons.add_rounded, size: 20),
            label: const Text('Tambah Investasi'),
          ),
        ],
      ),
    );
  }

  Widget _buildPortfolio() {
    return ListView(
      padding: const EdgeInsets.all(20),
      children: [
        // Portfolio summary card
        Container(
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: _isProfit
                  ? [
                      AppColors.income.withValues(alpha: 0.85),
                      AppColors.primary.withValues(alpha: 0.85),
                    ]
                  : [
                      AppColors.expense.withValues(alpha: 0.85),
                      const Color(0xFFD32F2F).withValues(alpha: 0.85),
                    ],
            ),
            borderRadius: BorderRadius.circular(20),
            boxShadow: [
              BoxShadow(
                color: (_isProfit ? AppColors.income : AppColors.expense)
                    .withValues(alpha: 0.25),
                blurRadius: 16,
                offset: const Offset(0, 6),
              ),
            ],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Total Portofolio',
                style: AppTypography.bodyMain.copyWith(color: Colors.white70),
              ),
              const SizedBox(height: 4),
              Text(
                AppFormatters.formatRupiah(_totalValue),
                style: AppTypography.amountLg.copyWith(color: Colors.white),
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Icon(
                    _isProfit
                        ? Icons.arrow_upward_rounded
                        : Icons.arrow_downward_rounded,
                    color: Colors.white70,
                    size: 16,
                  ),
                  const SizedBox(width: 4),
                  Text(
                    '${_isProfit ? '+' : ''}${AppFormatters.formatRupiah(_totalPL)} '
                    '(${AppFormatters.formatPercent(_totalInitial > 0 ? _totalPL / _totalInitial * 100 : 0)})',
                    style: AppTypography.bodyMain.copyWith(
                      color: Colors.white70,
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
        const SizedBox(height: 20),

        // Allocation pie chart
        if (_investments.isNotEmpty) ...[
          Text('Alokasi Aset', style: AppTypography.sectionTitle),
          const SizedBox(height: 12),
          _buildAllocationChart(),
          const SizedBox(height: 20),
        ],

        // Investment list
        Text('Daftar Investasi', style: AppTypography.sectionTitle),
        const SizedBox(height: 12),
        ..._investments.map((inv) => _InvestmentCard(investment: inv)),
      ],
    );
  }

  Widget _buildAllocationChart() {
    final colors = [
      AppColors.income,
      AppColors.secondary,
      AppColors.foodAccent,
      AppColors.shoppingAccent,
      AppColors.transportAccent,
      AppColors.tertiary,
    ];
    final sections = _investments.asMap().entries.map((e) {
      final pct = _totalValue > 0
          ? e.value.currentValue / _totalValue * 100
          : 0.0;
      return PieChartSectionData(
        value: e.value.currentValue,
        color: colors[e.key % colors.length],
        title: '${pct.toStringAsFixed(0)}%',
        radius: 50,
        titleStyle: AppTypography.badgeLabel.copyWith(
          color: Colors.white,
          fontSize: 11,
        ),
      );
    }).toList();

    return SizedBox(
      height: 160,
      child: PieChart(
        PieChartData(
          sections: sections,
          centerSpaceRadius: 40,
          sectionsSpace: 2,
        ),
      ),
    );
  }
}

class _InvestmentCard extends StatelessWidget {
  final InvestmentModel investment;
  const _InvestmentCard({required this.investment});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () => context.push('/investment/detail', extra: investment),
      behavior: HitTestBehavior.opaque,
      child: Container(
        margin: const EdgeInsets.only(bottom: 12),
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: AppColors.surfaceContainerLowest,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: AppColors.outlineVariant, width: 0.5),
        ),
        child: Row(
          children: [
            Container(
              width: 44,
              height: 44,
              decoration: BoxDecoration(
                color:
                    (investment.isProfit ? AppColors.income : AppColors.expense)
                        .withValues(alpha: 0.12),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Icon(
                Icons.trending_up_rounded,
                color: investment.isProfit
                    ? AppColors.income
                    : AppColors.expense,
                size: 22,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    investment.name,
                    style: AppTypography.bodyMain.copyWith(
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  Text(
                    investment.typeLabel,
                    style: AppTypography.metaData.copyWith(
                      color: AppColors.onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            ),
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(
                  AppFormatters.formatRupiah(investment.currentValue),
                  style: AppTypography.amountSm,
                ),
                Text(
                  AppFormatters.formatPercent(investment.profitLossPercent),
                  style: AppTypography.metaData.copyWith(
                    color: investment.isProfit
                        ? AppColors.income
                        : AppColors.expense,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
