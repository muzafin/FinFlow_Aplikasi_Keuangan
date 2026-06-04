import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import '../../../core/router/app_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_typography.dart';
import '../../../core/utils/formatters.dart';
import '../../../shared/models/transaction_model.dart';
import '../../../features/account/data/account_repository.dart';
import '../data/transaction_repository.dart';

class TransactionListScreen extends ConsumerStatefulWidget {
  const TransactionListScreen({super.key});

  @override
  ConsumerState<TransactionListScreen> createState() => _TransactionListScreenState();
}

class _TransactionListScreenState extends ConsumerState<TransactionListScreen> {
  final String _selectedPeriod = 'Tahun Ini';

  @override
  Widget build(BuildContext context) {
    final transactionsAsync = ref.watch(allTransactionsProvider);
    final accountsAsync = ref.watch(accountsProvider);

    return Scaffold(
      backgroundColor: AppColors.bgLight,
      body: SafeArea(
        child: Column(
          children: [
            // Top App Bar
            Padding(
              padding: const EdgeInsets.fromLTRB(8, 8, 16, 12),
              child: Row(
                children: [
                  IconButton(
                    icon: const Icon(Icons.arrow_left_rounded, size: 36, color: AppColors.onSurface),
                    onPressed: () => context.pop(),
                  ),
                  Expanded(
                    child: Padding(
                      padding: const EdgeInsets.only(right: 36), // Balance the back button
                      child: Text(
                        'Riwayat Transaksi',
                        style: AppTypography.sectionTitle,
                        textAlign: TextAlign.center,
                      ),
                    ),
                  ),
                ],
              ),
            ),

            // Filter / Timeframe Selector
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: InkWell(
                onTap: () {
                  // TODO: Show period selector bottom sheet
                },
                borderRadius: BorderRadius.circular(12),
                child: Container(
                  height: 56,
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  decoration: BoxDecoration(
                    color: AppColors.surfaceContainerLowest,
                    borderRadius: BorderRadius.circular(12),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withValues(alpha: 0.02),
                        blurRadius: 8,
                        offset: const Offset(0, 2),
                      ),
                    ],
                  ),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(_selectedPeriod, style: AppTypography.bodyMain.copyWith(fontWeight: FontWeight.w500)),
                      const Icon(Icons.keyboard_arrow_down_rounded, color: AppColors.outline),
                    ],
                  ),
                ),
              ),
            ),
            const SizedBox(height: 24),

            // Transaction List
            Expanded(
              child: transactionsAsync.when(
                loading: () => const Center(child: CircularProgressIndicator()),
                error: (err, _) => Center(child: Text('Gagal memuat transaksi: $err', style: AppTypography.bodyMain.copyWith(color: AppColors.expense))),
                data: (transactions) {
                  if (transactions.isEmpty) {
                    return _buildEmptyState();
                  }

                  // Group transactions by date
                  final Map<String, List<TransactionModel>> grouped = {};
                  for (var tx in transactions) {
                    final dateStr = DateFormat('EEEE, d MMMM y', 'id_ID').format(tx.date);
                    grouped.putIfAbsent(dateStr, () => []).add(tx);
                  }

                  // Accounts map for fast lookup
                  final accounts = accountsAsync.valueOrNull ?? [];
                  final accountMap = {for (var a in accounts) a.id: a.name};

                  return ListView.builder(
                    padding: const EdgeInsets.only(bottom: 100),
                    itemCount: grouped.keys.length,
                    itemBuilder: (context, index) {
                      final dateStr = grouped.keys.elementAt(index);
                      final txs = grouped[dateStr]!;

                      return Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Padding(
                            padding: const EdgeInsets.fromLTRB(24, 0, 24, 8),
                            child: Text(
                              dateStr,
                              style: AppTypography.sectionTitle.copyWith(
                                fontSize: 14,
                                color: AppColors.onSurfaceVariant,
                              ),
                            ),
                          ),
                          ...txs.map((tx) {
                            final accountName = accountMap[tx.accountId] ?? 'Akun Terhapus';
                            return _TransactionItem(tx: tx, accountName: accountName);
                          }),
                          const SizedBox(height: 16),
                        ],
                      );
                    },
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            padding: const EdgeInsets.all(24),
            decoration: BoxDecoration(
              color: AppColors.surfaceContainerLowest,
              shape: BoxShape.circle,
              border: Border.all(color: AppColors.outlineVariant.withValues(alpha: 0.5)),
            ),
            child: const Icon(Icons.receipt_long_outlined, size: 48, color: AppColors.outlineVariant),
          ),
          const SizedBox(height: 16),
          Text('Belum ada transaksi', style: AppTypography.bodyMain.copyWith(fontWeight: FontWeight.w600)),
          const SizedBox(height: 4),
          Text('Catat transaksi pertamamu sekarang',
              style: AppTypography.metaData.copyWith(color: AppColors.onSurfaceVariant)),
        ],
      ),
    );
  }
}

class _TransactionItem extends StatelessWidget {
  final TransactionModel tx;
  final String accountName;

  const _TransactionItem({required this.tx, required this.accountName});

  @override
  Widget build(BuildContext context) {
    final isIncome = tx.type == TransactionType.income;
    final isExpense = tx.type == TransactionType.expense;
    final amountColor = isIncome ? AppColors.income : isExpense ? AppColors.expense : AppColors.secondary;
    final prefix = isIncome ? '+' : isExpense ? '-' : '';
    
    final icon = isIncome
        ? Icons.arrow_downward_rounded
        : isExpense
            ? Icons.arrow_upward_rounded
            : Icons.swap_horiz_rounded;

    return GestureDetector(
      onTap: () => context.push(AppRoutes.addTransaction, extra: tx),
      child: Container(
      margin: const EdgeInsets.fromLTRB(16, 0, 16, 8),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.surfaceContainerLowest,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.outlineVariant.withValues(alpha: 0.3)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.03),
            blurRadius: 12,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Row(
        children: [
          Container(
            width: 44,
            height: 44,
            decoration: BoxDecoration(
              color: amountColor.withValues(alpha: 0.1),
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: amountColor.withValues(alpha: 0.2)),
            ),
            child: Icon(icon, color: amountColor, size: 22),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  tx.title.isNotEmpty ? tx.title : tx.categoryName,
                  style: AppTypography.bodyMain.copyWith(fontWeight: FontWeight.w600, height: 1.2),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 4),
                Row(
                  children: [
                    Text(
                      DateFormat('hh:mm a').format(tx.date),
                      style: AppTypography.metaData.copyWith(color: AppColors.outline),
                    ),
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 4),
                      child: Text('•', style: AppTypography.metaData.copyWith(color: AppColors.outline.withValues(alpha: 0.4), fontSize: 10)),
                    ),
                    Expanded(
                      child: Text(
                        accountName,
                        style: AppTypography.metaData.copyWith(color: AppColors.outline),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          Text(
            '$prefix${AppFormatters.formatRupiahCompact(tx.amount)}',
            style: AppTypography.amountSm.copyWith(
              color: amountColor,
              fontWeight: FontWeight.w700,
              letterSpacing: -0.5,
            ),
          ),
        ],
      ),
    ));
  }
}
