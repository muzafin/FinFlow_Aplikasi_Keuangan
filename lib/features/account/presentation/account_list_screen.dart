import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_typography.dart';
import '../../../core/utils/formatters.dart';
import '../../../features/account/data/account_repository.dart';
import '../../../shared/models/account_model.dart';

class AccountListScreen extends ConsumerWidget {
  const AccountListScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final accountsAsync = ref.watch(accountsProvider);

    return SafeArea(
      child: CustomScrollView(
        slivers: [
          // Header
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(20, 20, 16, 12),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const SizedBox(width: 40), // Spacer for centering
                  Text('Dompet Saya', style: AppTypography.appTitle),
                  IconButton(
                    icon: const Icon(Icons.add_rounded),
                    onPressed: () => _showAddAccountSheet(context, ref),
                    style: IconButton.styleFrom(
                      backgroundColor: AppColors.surfaceContainerLow,
                    ),
                  ),
                ],
              ),
            ),
          ),

          // Content
          SliverToBoxAdapter(
            child: accountsAsync.when(
              loading: () => const Center(
                  child: Padding(
                padding: EdgeInsets.all(40),
                child: CircularProgressIndicator(),
              )),
              error: (e, _) => Padding(
                padding: const EdgeInsets.all(20),
                child: _errorCard(e.toString()),
              ),
              data: (accounts) {
                final total = accounts.fold(0.0, (s, a) => s + a.balance);

                // Group accounts
                final cashAccounts = accounts.where((a) => a.type == AccountType.cash).toList();
                final bankAccounts = accounts.where((a) => a.type == AccountType.bank).toList();
                final ewalletAccounts = accounts.where((a) => a.type == AccountType.ewallet).toList();

                return Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Gradient Card
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 16),
                      child: _TotalBalanceCard(total: total),
                    ),
                    const SizedBox(height: 24),

                    // Cash
                    _buildSection('Tunai', AccountType.cash, cashAccounts, context, ref, Icons.account_balance_wallet_rounded),
                    
                    // Bank
                    _buildSection('Akun Bank', AccountType.bank, bankAccounts, context, ref, Icons.account_balance_rounded),
                    
                    // E-Wallet
                    _buildSection('E-Wallet', AccountType.ewallet, ewalletAccounts, context, ref, Icons.wallet_rounded),
                    
                    const SizedBox(height: 100), // Bottom padding
                  ],
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _errorCard(String e) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.errorContainer,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Text(e,
          style: AppTypography.bodyMain
              .copyWith(color: AppColors.onErrorContainer)),
    );
  }

  Widget _buildSection(
      String title, AccountType type, List<AccountModel> accounts, BuildContext context, WidgetRef ref, IconData icon) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: AppTypography.sectionTitle),
          const SizedBox(height: 12),
          if (accounts.isEmpty)
            _EmptyAccountCard(title: 'Belum ada dompet $title', icon: icon, onTap: () => _showAddAccountSheet(context, ref))
          else
            ...accounts.map((acc) => _AccountCard(
                  account: acc,
                  onTap: () => _showAddAccountSheet(context, ref, acc),
                )),
        ],
      ),
    );
  }

  void _showAddAccountSheet(BuildContext context, WidgetRef ref, [AccountModel? existing]) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) => _AddAccountSheet(ref: ref, existingAccount: existing),
    );
  }
}

// ─── Total Balance Card (Gradient) ──────────────────────────────────────────
class _TotalBalanceCard extends StatelessWidget {
  final double total;
  const _TotalBalanceCard({required this.total});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [Color(0xFF3B82F6), Color(0xFF14B8A6), Color(0xFF34D399)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF14B8A6).withValues(alpha: 0.3),
            blurRadius: 20,
            offset: const Offset(0, 10),
          ),
        ],
      ),
      child: Column(
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text('Total Saldo (IDR)',
                  style: AppTypography.bodyMain.copyWith(color: Colors.white.withValues(alpha: 0.9))),
              const SizedBox(width: 4),
              const Icon(Icons.swap_horiz_rounded, color: Colors.white, size: 16),
            ],
          ),
          const SizedBox(height: 8),
          Text(AppFormatters.formatRupiah(total),
              style: AppTypography.amountLg.copyWith(color: Colors.white, fontSize: 32)),
          const SizedBox(height: 16),
          
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: Colors.white.withValues(alpha: 0.2),
                  borderRadius: BorderRadius.circular(99),
                  border: Border.all(color: Colors.white.withValues(alpha: 0.1)),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.arrow_upward_rounded, color: Colors.white, size: 14),
                    const SizedBox(width: 4),
                    Text('+0.0%', style: AppTypography.metaData.copyWith(color: Colors.white, fontWeight: FontWeight.w600)),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              Text('(+Rp0) 30 hari terakhir', style: AppTypography.metaData.copyWith(color: Colors.white.withValues(alpha: 0.9))),
            ],
          ),
          const SizedBox(height: 24),
          
          // Grid Stats
          Row(
            children: [
              Expanded(child: _StatBox('Saldo Bersih', total)),
              const SizedBox(width: 12),
              Expanded(child: _StatBox('Hutang Aktif', 0)),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(child: _StatBox('Tabungan Aktif', 0)),
              const SizedBox(width: 12),
              Expanded(child: _StatBox('Pembayaran Mendatang', 0)),
            ],
          ),
        ],
      ),
    );
  }
}

class _StatBox extends StatelessWidget {
  final String label;
  final double amount;
  const _StatBox(this.label, this.amount);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.white.withValues(alpha: 0.1)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: AppTypography.metaData.copyWith(color: Colors.white.withValues(alpha: 0.9))),
          const SizedBox(height: 4),
          Text(AppFormatters.formatRupiah(amount),
              style: AppTypography.bodyMain.copyWith(color: Colors.white, fontWeight: FontWeight.w600)),
        ],
      ),
    );
  }
}

// ─── Account Card ─────────────────────────────────────────────────────────────
class _AccountCard extends StatelessWidget {
  final AccountModel account;
  final VoidCallback onTap;
  const _AccountCard({required this.account, required this.onTap});

  static const _icons = {
    AccountType.cash: Icons.account_balance_wallet_rounded,
    AccountType.bank: Icons.account_balance_rounded,
    AccountType.ewallet: Icons.wallet_rounded,
    AccountType.investment: Icons.trending_up_rounded,
    AccountType.loan: Icons.credit_card_rounded,
  };

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        margin: const EdgeInsets.only(bottom: 12),
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: AppColors.surfaceContainerLowest,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: AppColors.outlineVariant.withValues(alpha: 0.3)),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.02),
              blurRadius: 4,
              offset: const Offset(0, 2),
            ),
          ],
        ),
        child: Row(
          children: [
            Container(
              width: 48,
              height: 48,
              decoration: const BoxDecoration(
                color: AppColors.surfaceContainer,
                shape: BoxShape.circle,
              ),
              child: Icon(
                _icons[account.type] ?? Icons.account_balance_wallet_rounded,
                color: AppColors.onSurfaceVariant,
                size: 24,
              ),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(account.name,
                      style: AppTypography.bodyMain.copyWith(fontWeight: FontWeight.w600)),
                  Text('${account.typeLabel.toUpperCase()} • IDR',
                      style: AppTypography.metaData.copyWith(color: AppColors.outline)),
                ],
              ),
            ),
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text('Saldo Saat Ini',
                    style: AppTypography.metaData.copyWith(color: AppColors.outline)),
                const SizedBox(height: 2),
                Text(
                  AppFormatters.formatRupiah(account.balance),
                  style: AppTypography.bodyMain.copyWith(
                    color: AppColors.onSurface,
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

// ─── Empty Account Card ───────────────────────────────────────────────────────
class _EmptyAccountCard extends StatelessWidget {
  final String title;
  final IconData icon;
  final VoidCallback onTap;
  const _EmptyAccountCard({required this.title, required this.icon, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(24),
        decoration: BoxDecoration(
          color: AppColors.surfaceContainerLowest,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(
            color: AppColors.outlineVariant.withValues(alpha: 0.5),
            style: BorderStyle.solid,
          ),
        ),
        child: Row(
          children: [
            Container(
              width: 48, height: 48,
              decoration: const BoxDecoration(
                color: AppColors.surfaceContainer,
                shape: BoxShape.circle,
              ),
              child: Icon(icon, color: AppColors.outline),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: AppTypography.bodyMain.copyWith(fontWeight: FontWeight.w500)),
                  const SizedBox(height: 4),
                  Text('Ketuk + di pojok kanan atas untuk menambahkan',
                      style: AppTypography.metaData.copyWith(color: AppColors.outline)),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// ─── Add Account Bottom Sheet ─────────────────────────────────────────────────
class _AddAccountSheet extends ConsumerStatefulWidget {
  final WidgetRef ref;
  final AccountModel? existingAccount;
  const _AddAccountSheet({required this.ref, this.existingAccount});

  @override
  ConsumerState<_AddAccountSheet> createState() => _AddAccountSheetState();
}

class _AddAccountSheetState extends ConsumerState<_AddAccountSheet> {
  final _nameCtrl = TextEditingController();
  final _balanceCtrl = TextEditingController(text: '0');
  AccountType _type = AccountType.cash;
  bool _saving = false;

  static const _types = [
    (AccountType.cash, 'Kas', Icons.account_balance_wallet_rounded, AppColors.income),
    (AccountType.bank, 'Bank', Icons.account_balance_rounded, AppColors.secondary),
    (AccountType.ewallet, 'E-Wallet', Icons.wallet_rounded, AppColors.shoppingAccent),
    (AccountType.investment, 'Investasi', Icons.trending_up_rounded, AppColors.primary),
    (AccountType.loan, 'Pinjaman', Icons.credit_card_rounded, AppColors.expense),
  ];

  @override
  void initState() {
    super.initState();
    if (widget.existingAccount != null) {
      final acc = widget.existingAccount!;
      _nameCtrl.text = acc.name;
      final amountStr = acc.balance.toInt().toString();
      _balanceCtrl.text = amountStr == '0' ? '' : amountStr;
      _type = acc.type;
    }
  }

  @override
  void dispose() {
    _nameCtrl.dispose();
    _balanceCtrl.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    if (_nameCtrl.text.trim().isEmpty) return;
    setState(() => _saving = true);

    final balance = double.tryParse(
          _balanceCtrl.text.replaceAll('.', '').replaceAll(',', '.')) ??
        0;

    final color = _types
        .firstWhere((t) => t.$1 == _type,
            orElse: () => _types.first)
        .$4;

    final isEdit = widget.existingAccount != null;
    final account = AccountModel(
      id: isEdit ? widget.existingAccount!.id : 'acc_${DateTime.now().millisecondsSinceEpoch}',
      name: _nameCtrl.text.trim(),
      type: _type,
      balance: balance,
      color: color.toARGB32(),
      icon: _type.name,
      order: isEdit ? widget.existingAccount!.order : 0,
      createdAt: isEdit ? widget.existingAccount!.createdAt : DateTime.now(),
    );

    try {
      await ref.read(accountRepositoryProvider).saveAccount(account);
      if (mounted) Navigator.pop(context);
    } catch (e) {
      if (mounted) {
        setState(() => _saving = false);
        ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Gagal: $e')));
      }
    }
  }

  Future<void> _confirmDelete() async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (c) => AlertDialog(
        title: Text('Hapus Akun?', style: AppTypography.sectionTitle),
        content: Text('Apakah Anda yakin ingin menghapus akun ini beserta seluruh transaksi di dalamnya?', style: AppTypography.bodyMain),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(c, false),
            child: Text('Batal', style: AppTypography.bodyMain.copyWith(color: AppColors.onSurfaceVariant)),
          ),
          TextButton(
            onPressed: () => Navigator.pop(c, true),
            child: Text('Hapus', style: AppTypography.bodyMain.copyWith(color: AppColors.expense)),
          ),
        ],
      ),
    );

    if (confirm == true && mounted && widget.existingAccount != null) {
      setState(() => _saving = true);
      try {
        await ref.read(accountRepositoryProvider).deleteAccount(widget.existingAccount!.id);
        if (!mounted) return;
        Navigator.pop(context);
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Akun dihapus'), backgroundColor: AppColors.expense),
        );
      } catch (e) {
        if (!mounted) return;
        setState(() => _saving = false);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Gagal menghapus: $e'), backgroundColor: AppColors.expense),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.fromLTRB(
          24, 20, 24, MediaQuery.viewInsetsOf(context).bottom + 24),
      decoration: const BoxDecoration(
        color: AppColors.surfaceContainerLowest,
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Center(
            child: Container(
              width: 40,
              height: 4,
              decoration: BoxDecoration(
                  color: AppColors.outlineVariant,
                  borderRadius: BorderRadius.circular(2)),
            ),
          ),
          const SizedBox(height: 20),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(widget.existingAccount != null ? 'Edit Akun' : 'Tambah Akun', style: AppTypography.sectionTitle),
              if (widget.existingAccount != null)
                IconButton(
                  onPressed: _saving ? null : _confirmDelete,
                  icon: const Icon(Icons.delete_outline_rounded, color: AppColors.expense),
                ),
            ],
          ),
          const SizedBox(height: 20),

          // Type selector
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: _types.map((t) {
              final isSelected = _type == t.$1;
              return GestureDetector(
                onTap: () => setState(() => _type = t.$1),
                child: AnimatedContainer(
                  duration: const Duration(milliseconds: 150),
                  padding: const EdgeInsets.symmetric(
                      horizontal: 12, vertical: 8),
                  decoration: BoxDecoration(
                    color: isSelected
                        ? t.$4.withValues(alpha: 0.12)
                        : AppColors.surfaceContainerLow,
                    borderRadius: BorderRadius.circular(99),
                    border: isSelected
                        ? Border.all(color: t.$4, width: 1.5)
                        : null,
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(t.$3,
                          color: isSelected
                              ? t.$4
                              : AppColors.onSurfaceVariant,
                          size: 16),
                      const SizedBox(width: 6),
                      Text(t.$2,
                          style: AppTypography.badgeLabel.copyWith(
                              color: isSelected
                                  ? t.$4
                                  : AppColors.onSurfaceVariant,
                              fontWeight: isSelected
                                  ? FontWeight.w700
                                  : FontWeight.w500)),
                    ],
                  ),
                ),
              );
            }).toList(),
          ),
          const SizedBox(height: 16),

          TextField(
            controller: _nameCtrl,
            textCapitalization: TextCapitalization.words,
            decoration: const InputDecoration(
              labelText: 'Nama Akun',
              hintText: 'cth. BCA, Dana, GoPay',
              prefixIcon: Icon(Icons.label_outline_rounded, size: 20),
            ),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _balanceCtrl,
            keyboardType: TextInputType.number,
            decoration: const InputDecoration(
              labelText: 'Saldo Awal',
              prefixText: 'Rp ',
              prefixIcon: Icon(Icons.attach_money_rounded, size: 20),
            ),
          ),
          const SizedBox(height: 24),

          SizedBox(
            width: double.infinity,
            height: 50,
            child: ElevatedButton(
              onPressed: _saving ? null : _save,
              child: _saving
                  ? const SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(
                          color: Colors.white, strokeWidth: 2))
                  : const Text('Simpan Akun'),
            ),
          ),
        ],
      ),
    );
  }
}
