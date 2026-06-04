import 'dart:ui';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/services/firebase_service.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_typography.dart';
import '../../../core/utils/formatters.dart';
import '../../../features/account/data/account_repository.dart';
import '../../../features/transaction/data/transaction_repository.dart';
import '../../../shared/models/transaction_model.dart';

/// Dashboard utama — live data dari Firestore
class DashboardScreen extends ConsumerStatefulWidget {
  const DashboardScreen({super.key});

  @override
  ConsumerState<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends ConsumerState<DashboardScreen> {
  bool _privacyMode = false;
  String _selectedPeriod = 'Bulan';
  final _periods = ['Hari', 'Minggu', 'Bulan', 'Tahun'];

  final _menuItems = [
    _MenuData('Anggaran', Icons.pie_chart_outline_rounded, const Color(0xFF4CAF50)),
    _MenuData('Berulang', Icons.autorenew_rounded, const Color(0xFF2196F3)),
    _MenuData('Target', Icons.flag_outlined, const Color(0xFFFF9800)),
    _MenuData('Tagihan', Icons.receipt_long_outlined, const Color(0xFFE91E63)),
    _MenuData('Utang', Icons.account_balance_outlined, const Color(0xFF9C27B0)),
    _MenuData('Keinginan', Icons.favorite_border_rounded, const Color(0xFFF44336)),
    _MenuData('Kartu', Icons.credit_card_outlined, const Color(0xFF00BCD4)),
    _MenuData('Catatan', Icons.note_outlined, const Color(0xFF795548)),
  ];

  String _mask(String value) => _privacyMode ? '• • • • • •' : value;

  @override
  Widget build(BuildContext context) {
    final user = FirebaseService.currentUser;
    final name = user?.displayName?.split(' ').first ?? 'Kamu';
    final greeting = AppFormatters.getGreeting();

    // Riverpod watchers
    final accountsAsync = ref.watch(accountsProvider);
    final now = DateTime.now();
    final monthStart = DateTime(now.year, now.month, 1);
    final txAsync = ref.watch(monthlyTransactionsProvider(monthStart));

    return SafeArea(
      bottom: false,
      child: CustomScrollView(
        slivers: [
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(20, 16, 20, 0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // ── Header ──────────────────────────────────────────────
                  _buildHeader(name, greeting),
                  const SizedBox(height: 20),

                  // ── Period Selector ──────────────────────────────────────
                  _buildPeriodSelector(),
                  const SizedBox(height: 20),

                  // ── Balance Card ─────────────────────────────────────────
                  _buildBalanceCard(accountsAsync, txAsync),
                  const SizedBox(height: 20),

                  // ── Menu Grid ────────────────────────────────────────────
                  _buildMenuGrid(),
                  const SizedBox(height: 20),

                  // ── Recent Transactions ───────────────────────────────────
                  _buildRecentTransactions(txAsync),
                  const SizedBox(height: 120),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  // ── Widgets ─────────────────────────────────────────────────────────────────

  Widget _buildHeader(String name, String greeting) {
    return Row(
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('$greeting,',
                  style: AppTypography.metaData
                      .copyWith(color: AppColors.onSurfaceVariant)),
              Row(
                children: [
                  Flexible(
                    child: Text('~ Hi, $name! 👋',
                        style: AppTypography.appTitle),
                  ),
                  const SizedBox(width: 8),
                  Container(
                    padding: const EdgeInsets.symmetric(
                        horizontal: 8, vertical: 2),
                    decoration: BoxDecoration(
                      color: AppColors.primaryContainer,
                      borderRadius: BorderRadius.circular(99),
                    ),
                    child: Text('Lvl 1',
                        style: AppTypography.badgeLabel
                            .copyWith(color: Colors.white)),
                  ),
                ],
              ),
            ],
          ),
        ),
        GestureDetector(
          onTap: () => context.go('/profile'),
          child: Container(
            width: 44,
            height: 44,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: AppColors.primaryFixed,
              border: Border.all(color: AppColors.primary, width: 2),
            ),
            child: ClipOval(
              child: FirebaseService.currentUser?.photoURL != null
                  ? Image.network(
                      FirebaseService.currentUser!.photoURL!,
                      fit: BoxFit.cover,
                    )
                  : Center(
                      child: Text(
                        (FirebaseService.currentUser?.displayName ?? 'U')[0]
                            .toUpperCase(),
                        style: AppTypography.bodyMain.copyWith(
                            color: AppColors.primary,
                            fontWeight: FontWeight.w700),
                      ),
                    ),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildPeriodSelector() {
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        children: _periods.map((p) {
          final isSelected = p == _selectedPeriod;
          return GestureDetector(
            onTap: () => setState(() => _selectedPeriod = p),
            child: AnimatedContainer(
              duration: const Duration(milliseconds: 200),
              margin: const EdgeInsets.only(right: 8),
              padding: const EdgeInsets.symmetric(
                  horizontal: 16, vertical: 8),
              decoration: BoxDecoration(
                color: isSelected
                    ? AppColors.primary
                    : AppColors.surfaceContainerLow,
                borderRadius: BorderRadius.circular(99),
              ),
              child: Text(
                p,
                style: AppTypography.badgeLabel.copyWith(
                  color: isSelected
                      ? Colors.white
                      : AppColors.onSurfaceVariant,
                  fontWeight: isSelected
                      ? FontWeight.w600
                      : FontWeight.w500,
                ),
              ),
            ),
          );
        }).toList(),
      ),
    );
  }

  Widget _buildBalanceCard(
    AsyncValue<List> accountsAsync,
    AsyncValue<List<TransactionModel>> txAsync,
  ) {
    // Hitung saldo total dan summary bulan ini
    double totalBalance = 0;
    double income = 0;
    double expense = 0;

    accountsAsync.whenData((accounts) {
      for (final a in accounts) {
        totalBalance += (a as dynamic).balance as double;
      }
    });

    txAsync.whenData((txList) {
      for (final tx in txList) {
        if (tx.type == TransactionType.income) {
          income += tx.amount;
        } else if (tx.type == TransactionType.expense) {
          expense += tx.amount;
        }
      }
    });

    return Container(
      width: double.infinity,
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xCC4A90E2), Color(0xCC5CE1E6)],
        ),
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF4A90E2).withValues(alpha: 0.25),
            blurRadius: 20,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(24),
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: 10, sigmaY: 10),
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text('Total Saldo',
                        style: AppTypography.bodyMain
                            .copyWith(color: Colors.white70)),
                    Row(
                      children: [
                        Text(
                          AppFormatters.formatMonthYear(DateTime.now()),
                          style: AppTypography.metaData
                              .copyWith(color: Colors.white60),
                        ),
                        const SizedBox(width: 8),
                        GestureDetector(
                          onTap: () =>
                              setState(() => _privacyMode = !_privacyMode),
                          child: Icon(
                            _privacyMode
                                ? Icons.visibility_off_outlined
                                : Icons.visibility_outlined,
                            color: Colors.white70,
                            size: 18,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
                const SizedBox(height: 4),
                accountsAsync.when(
                  data: (_) => Text(
                    _mask(AppFormatters.formatRupiah(totalBalance)),
                    style: AppTypography.amountLg.copyWith(color: Colors.white),
                  ),
                  loading: () => const SizedBox(
                    height: 48,
                    child: Center(
                      child: SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(
                            color: Colors.white54, strokeWidth: 2),
                      ),
                    ),
                  ),
                  error: (_, __) => Text('Error',
                      style: AppTypography.amountLg
                          .copyWith(color: Colors.white70)),
                ),
                const SizedBox(height: 16),
                Row(
                  children: [
                    Expanded(
                      child: _GlassSubCard(
                        label: 'Pemasukan',
                        amount: _mask(AppFormatters.formatRupiahCompact(income)),
                        icon: Icons.arrow_downward_rounded,
                        color: AppColors.income,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: _GlassSubCard(
                        label: 'Pengeluaran',
                        amount:
                            _mask(AppFormatters.formatRupiahCompact(expense)),
                        icon: Icons.arrow_upward_rounded,
                        color: AppColors.expense,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildMenuGrid() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Fitur', style: AppTypography.sectionTitle),
        const SizedBox(height: 12),
        SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          child: Row(
            children: _menuItems.map((item) {
              return GestureDetector(
                onTap: () {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: Text('Fitur ${item.label} akan segera hadir!'),
                      behavior: SnackBarBehavior.floating,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                      backgroundColor: AppColors.primary,
                      duration: const Duration(seconds: 2),
                    ),
                  );
                },
                child: Container(
                  margin: const EdgeInsets.only(right: 12),
                  child: Column(
                    children: [
                      Container(
                        width: 56,
                        height: 56,
                        decoration: BoxDecoration(
                          color: item.color.withValues(alpha: 0.12),
                          borderRadius: BorderRadius.circular(16),
                        ),
                        child: Icon(item.icon, color: item.color, size: 26),
                      ),
                      const SizedBox(height: 6),
                      Text(item.label,
                          style: AppTypography.badgeLabel.copyWith(
                              color: AppColors.onSurfaceVariant)),
                    ],
                  ),
                ),
              );
            }).toList(),
          ),
        ),
      ],
    );
  }

  Widget _buildRecentTransactions(
      AsyncValue<List<TransactionModel>> txAsync) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text('Transaksi Terakhir', style: AppTypography.sectionTitle),
            TextButton(
              onPressed: () => context.push('/transactions'),
              child: Text('Lihat Semua',
                  style: AppTypography.metaData
                      .copyWith(color: AppColors.primary,
                          fontWeight: FontWeight.w600)),
            ),
          ],
        ),
        const SizedBox(height: 12),

        txAsync.when(
          loading: () => const Center(
            child: Padding(
              padding: EdgeInsets.all(32),
              child: CircularProgressIndicator(),
            ),
          ),
          error: (e, _) => Center(
              child: Text('Error: $e',
                  style: AppTypography.bodyMain
                      .copyWith(color: AppColors.expense))),
          data: (txList) {
            if (txList.isEmpty) return _buildEmptyTransactions();

            final recent = txList.take(5).toList();
            return Column(
              children: [
                Container(
                  decoration: BoxDecoration(
                    color: AppColors.surfaceContainerLowest,
                    borderRadius: BorderRadius.circular(16),
                    border: Border.all(
                        color: AppColors.outlineVariant, width: 0.5),
                  ),
                  child: ListView.separated(
                    shrinkWrap: true,
                    physics: const NeverScrollableScrollPhysics(),
                    itemCount: recent.length,
                    separatorBuilder: (_, __) =>
                        const Divider(height: 1, indent: 68, endIndent: 16),
                    itemBuilder: (context, i) =>
                        _TransactionTile(transaction: recent[i]),
                  ),
                ),
              ],
            );
          },
        ),
      ],
    );
  }

  Widget _buildEmptyTransactions() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(32),
      decoration: BoxDecoration(
        color: AppColors.surfaceContainerLowest,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.outlineVariant, width: 0.5),
      ),
      child: Column(
        children: [
          const Icon(Icons.receipt_long_outlined,
              size: 48, color: AppColors.outlineVariant),
          const SizedBox(height: 16),
          Text('Belum ada transaksi',
              style: AppTypography.bodyMain
                  .copyWith(fontWeight: FontWeight.w600)),
          const SizedBox(height: 4),
          Text('Mulai catat pemasukan & pengeluaranmu',
              style: AppTypography.metaData
                  .copyWith(color: AppColors.onSurfaceVariant),
              textAlign: TextAlign.center),
          const SizedBox(height: 16),
          ElevatedButton.icon(
            onPressed: () => context.push('/transaction/add'),
            icon: const Icon(Icons.add_rounded, size: 18),
            label: const Text('Tambah Transaksi'),
            style: ElevatedButton.styleFrom(
                minimumSize: const Size(180, 40)),
          ),
        ],
      ),
    );
  }
}

// ─── Sub-widgets ──────────────────────────────────────────────────────────────

class _MenuData {
  final String label;
  final IconData icon;
  final Color color;
  const _MenuData(this.label, this.icon, this.color);
}

class _GlassSubCard extends StatelessWidget {
  final String label, amount;
  final IconData icon;
  final Color color;

  const _GlassSubCard({
    required this.label,
    required this.amount,
    required this.icon,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.15),
        borderRadius: BorderRadius.circular(14),
      ),
      child: Row(
        children: [
          Container(
            width: 32,
            height: 32,
            decoration: BoxDecoration(
              color: color.withValues(alpha: 0.25),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Icon(icon, color: Colors.white, size: 18),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(label,
                    style: AppTypography.badgeLabel
                        .copyWith(color: Colors.white70)),
                Text(amount,
                    style: AppTypography.amountSm.copyWith(
                        color: Colors.white,
                        fontWeight: FontWeight.w700)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _TransactionTile extends StatelessWidget {
  final TransactionModel transaction;
  const _TransactionTile({required this.transaction});

  @override
  Widget build(BuildContext context) {
    final isIncome = transaction.type == TransactionType.income;
    final isExpense = transaction.type == TransactionType.expense;
    final amountColor =
        isIncome ? AppColors.income : isExpense ? AppColors.expense : AppColors.secondary;
    final icon = isIncome
        ? Icons.arrow_downward_rounded
        : isExpense
            ? Icons.arrow_upward_rounded
            : Icons.swap_horiz_rounded;

    return ListTile(
      onTap: () => context.push('/transaction/add', extra: transaction),
      contentPadding:
          const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      leading: Container(
        width: 44,
        height: 44,
        decoration: BoxDecoration(
          color: amountColor.withValues(alpha: 0.1),
          borderRadius: BorderRadius.circular(12),
        ),
        child: Icon(icon, color: amountColor, size: 22),
      ),
      title: Text(
        transaction.title.isEmpty
            ? transaction.categoryName
            : transaction.title,
        style: AppTypography.bodyMain.copyWith(fontWeight: FontWeight.w600),
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
      ),
      subtitle: Text(
        AppFormatters.formatRelativeDate(transaction.date),
        style: AppTypography.metaData
            .copyWith(color: AppColors.onSurfaceVariant),
      ),
      trailing: Text(
        '${isExpense ? '-' : '+'}${AppFormatters.formatRupiahCompact(transaction.amount)}',
        style: AppTypography.amountSm.copyWith(
            color: amountColor, fontWeight: FontWeight.w700),
      ),
    );
  }
}
