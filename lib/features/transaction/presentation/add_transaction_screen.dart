import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:uuid/uuid.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_typography.dart';
import '../../../core/utils/formatters.dart';
import '../../../features/account/data/account_repository.dart';
import '../../../features/transaction/data/transaction_repository.dart';
import '../../../shared/models/account_model.dart';
import '../../../shared/models/category_model.dart';
import '../../../shared/models/transaction_model.dart';

/// Add Transaction screen — bottom sheet dengan Firestore save
class AddTransactionScreen extends ConsumerStatefulWidget {
  final TransactionModel? existingTx;
  const AddTransactionScreen({super.key, this.existingTx});

  @override
  ConsumerState<AddTransactionScreen> createState() =>
      _AddTransactionScreenState();
}

class _AddTransactionScreenState extends ConsumerState<AddTransactionScreen>
    with TickerProviderStateMixin {
  late TabController _typeController;
  String _numpadInput = '';
  String _selectedCategoryId = 'exp_makan';
  String? _selectedAccountId;
  DateTime _selectedDate = DateTime.now();
  bool _isSaving = false;

  final _titleCtrl = TextEditingController();
  final _noteCtrl = TextEditingController();

  List<CategoryModel> get _currentCategories {
    switch (_typeController.index) {
      case 0:
        return DefaultCategories.expense;
      case 1:
        return DefaultCategories.income;
      default:
        return DefaultCategories.expense;
    }
  }

  @override
  void initState() {
    super.initState();
    int initialIndex = 0;
    if (widget.existingTx != null) {
      final type = widget.existingTx!.type;
      if (type == TransactionType.income) initialIndex = 1;
      if (type == TransactionType.transfer) initialIndex = 2;
    }
    _typeController = TabController(length: 3, vsync: this, initialIndex: initialIndex);
    
    if (widget.existingTx != null) {
      final tx = widget.existingTx!;
      final amountStr = tx.amount.toInt().toString();
      _numpadInput = amountStr == '0' ? '' : amountStr;
      _selectedCategoryId = tx.categoryId;
      _selectedAccountId = tx.accountId;
      _selectedDate = tx.date;
      _titleCtrl.text = tx.title;
      _noteCtrl.text = tx.description ?? '';
    }
    
    _typeController.addListener(() {
      setState(() {
        // Reset category ke default tipe baru
        _selectedCategoryId = _currentCategories.first.id;
      });
    });
  }

  @override
  void dispose() {
    _typeController.dispose();
    _titleCtrl.dispose();
    _noteCtrl.dispose();
    super.dispose();
  }

  // ── Helpers ──────────────────────────────────────────────────────────────────

  void _onNumpad(String key) {
    HapticFeedback.lightImpact();
    setState(() {
      if (key == '⌫') {
        if (_numpadInput.isNotEmpty) {
          _numpadInput = _numpadInput.substring(0, _numpadInput.length - 1);
        }
      } else if (key == '00') {
        if (_numpadInput.isNotEmpty && _numpadInput.length < 12) {
          _numpadInput += '00';
        }
      } else if (_numpadInput.length < 12) {
        if (_numpadInput == '0' && key != '.') {
          _numpadInput = key;
        } else {
          _numpadInput += key;
        }
      }
    });
  }

  String get _formattedAmount {
    if (_numpadInput.isEmpty) return '0';
    final n = int.tryParse(_numpadInput) ?? 0;
    return AppFormatters.formatNumpadInput(n.toString());
  }

  double get _parsedAmount =>
      double.tryParse(_numpadInput.isEmpty ? '0' : _numpadInput) ?? 0;

  TransactionType get _transactionType {
    switch (_typeController.index) {
      case 0:
        return TransactionType.expense;
      case 1:
        return TransactionType.income;
      case 2:
        return TransactionType.transfer;
      default:
        return TransactionType.expense;
    }
  }

  String get _typeLabel {
    switch (_typeController.index) {
      case 0:
        return 'Pengeluaran';
      case 1:
        return 'Pemasukan';
      case 2:
        return 'Transfer';
      default:
        return 'Pengeluaran';
    }
  }

  Color get _typeColor {
    switch (_typeController.index) {
      case 0:
        return AppColors.expense;
      case 1:
        return AppColors.income;
      case 2:
        return AppColors.secondary;
      default:
        return AppColors.expense;
    }
  }

  CategoryModel? get _selectedCategory {
    try {
      return _currentCategories.firstWhere((c) => c.id == _selectedCategoryId);
    } catch (_) {
      return _currentCategories.isNotEmpty ? _currentCategories.first : null;
    }
  }

  AccountModel? _resolveSelectedAccount(List<AccountModel> accounts) {
    if (accounts.isEmpty) return null;

    final selectedId = _selectedAccountId;
    if (selectedId != null) {
      for (final account in accounts) {
        if (account.id == selectedId) return account;
      }
    }

    return accounts.first;
  }

  // ── Save ─────────────────────────────────────────────────────────────────────

  Future<void> _save() async {
    if (_parsedAmount <= 0) return;
    final cat = _selectedCategory;
    if (cat == null) return;

    final accountsAsync = ref.read(accountsProvider);
    final accounts = accountsAsync.valueOrNull ?? [];
    final account = _resolveSelectedAccount(accounts);

    if (account == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Belum ada akun. Buat akun terlebih dahulu.'),
        ),
      );
      return;
    }

    setState(() => _isSaving = true);
    final now = DateTime.now();
    final isEdit = widget.existingTx != null;
    final tx = TransactionModel(
      id: isEdit ? widget.existingTx!.id : const Uuid().v4(),
      type: _transactionType,
      amount: _parsedAmount,
      categoryId: cat.id,
      categoryName: cat.name,
      categoryIcon: cat.icon,
      categoryColor: cat.color,
      title: _titleCtrl.text.trim(),
      description: _noteCtrl.text.trim().isEmpty ? null : _noteCtrl.text.trim(),
      date: _selectedDate,
      accountId: account.id,
      accountName: account.name,
      createdAt: isEdit ? widget.existingTx!.createdAt : now,
      updatedAt: now,
    );

    try {
      final repo = ref.read(transactionRepositoryProvider);
      if (isEdit) {
        await repo.updateTransaction(widget.existingTx!, tx);
      } else {
        await repo.addTransaction(tx);
      }
      if (!mounted) return;
      HapticFeedback.mediumImpact();
      Navigator.of(context).pop();
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(isEdit ? '$_typeLabel berhasil diubah' : '$_typeLabel berhasil disimpan'),
          backgroundColor: _typeColor,
        ),
      );
    } catch (e) {
      if (!mounted) return;
      setState(() => _isSaving = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Gagal menyimpan: $e'),
          backgroundColor: AppColors.expense,
        ),
      );
    }
  }

  Future<void> _pickDate() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: _selectedDate,
      firstDate: DateTime(2000),
      lastDate: DateTime.now().add(const Duration(days: 1)),
    );
    if (picked != null) setState(() => _selectedDate = picked);
  }

  Future<void> _confirmDelete() async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (c) => AlertDialog(
        title: Text('Hapus Transaksi?', style: AppTypography.sectionTitle),
        content: Text('Apakah Anda yakin ingin menghapus transaksi ini? Saldo dompet akan disesuaikan kembali.', style: AppTypography.bodyMain),
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

    if (confirm == true && mounted && widget.existingTx != null) {
      setState(() => _isSaving = true);
      try {
        await ref.read(transactionRepositoryProvider).deleteTransaction(widget.existingTx!);
        if (!mounted) return;
        Navigator.pop(context);
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Transaksi dihapus'), backgroundColor: AppColors.expense),
        );
      } catch (e) {
        if (!mounted) return;
        setState(() => _isSaving = false);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Gagal menghapus: $e'), backgroundColor: AppColors.expense),
        );
      }
    }
  }

  // ── Build ─────────────────────────────────────────────────────────────────────

  @override
  Widget build(BuildContext context) {
    final accountsAsync = ref.watch(accountsProvider);

    return Scaffold(
      backgroundColor: Colors.transparent,
      body: DraggableScrollableSheet(
        initialChildSize: 0.93,
        minChildSize: 0.6,
        maxChildSize: 0.97,
        builder: (context, scrollController) {
          return Container(
            decoration: const BoxDecoration(
              color: AppColors.surfaceContainerLowest,
              borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
            ),
            child: ListView(
              controller: scrollController,
              padding: EdgeInsets.zero,
              children: [
                // Drag handle
                Center(
                  child: Container(
                    margin: const EdgeInsets.only(top: 12, bottom: 8),
                    width: 40,
                    height: 4,
                    decoration: BoxDecoration(
                      color: AppColors.outlineVariant,
                      borderRadius: BorderRadius.circular(2),
                    ),
                  ),
                ),

                // Header with Delete button (if edit)
                Padding(
                  padding: const EdgeInsets.fromLTRB(20, 8, 8, 0),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        widget.existingTx != null ? 'Edit Transaksi' : 'Tambah Transaksi',
                        style: AppTypography.sectionTitle,
                      ),
                      if (widget.existingTx != null)
                        IconButton(
                          onPressed: _isSaving ? null : _confirmDelete,
                          icon: const Icon(Icons.delete_outline_rounded, color: AppColors.expense),
                        )
                      else
                        const SizedBox(height: 48), // Match height of IconButton
                    ],
                  ),
                ),

                // Type tabs
                Padding(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 20,
                    vertical: 8,
                  ),
                  child: Container(
                    height: 40,
                    decoration: BoxDecoration(
                      color: AppColors.surfaceContainerLow,
                      borderRadius: BorderRadius.circular(99),
                    ),
                    child: TabBar(
                      controller: _typeController,
                      indicator: BoxDecoration(
                        color: _typeColor,
                        borderRadius: BorderRadius.circular(99),
                      ),
                      indicatorSize: TabBarIndicatorSize.tab,
                      dividerColor: Colors.transparent,
                      labelColor: Colors.white,
                      unselectedLabelColor: AppColors.onSurfaceVariant,
                      labelStyle: AppTypography.badgeLabel.copyWith(
                        fontWeight: FontWeight.w600,
                        fontSize: 12,
                      ),
                      tabs: const [
                        Tab(text: 'Pengeluaran'),
                        Tab(text: 'Pemasukan'),
                        Tab(text: 'Transfer'),
                      ],
                    ),
                  ),
                ),

                // Category picker
                Padding(
                  padding: const EdgeInsets.only(left: 20, top: 4, bottom: 8),
                  child: Text(
                    'Kategori',
                    style: AppTypography.metaData.copyWith(
                      color: AppColors.onSurfaceVariant,
                    ),
                  ),
                ),
                SizedBox(
                  height: 88,
                  child: ListView.builder(
                    scrollDirection: Axis.horizontal,
                    padding: const EdgeInsets.symmetric(horizontal: 20),
                    itemCount: _currentCategories.length,
                    itemBuilder: (context, i) {
                      final cat = _currentCategories[i];
                      final isSelected = cat.id == _selectedCategoryId;
                      return GestureDetector(
                        onTap: () =>
                            setState(() => _selectedCategoryId = cat.id),
                        child: AnimatedContainer(
                          duration: const Duration(milliseconds: 200),
                          margin: const EdgeInsets.only(right: 10),
                          width: 64,
                          padding: const EdgeInsets.symmetric(vertical: 6),
                          decoration: BoxDecoration(
                            color: isSelected
                                ? cat.categoryColor.withValues(alpha: 0.12)
                                : Colors.transparent,
                            borderRadius: BorderRadius.circular(12),
                            border: isSelected
                                ? Border.all(
                                    color: cat.categoryColor,
                                    width: 1.5,
                                  )
                                : null,
                          ),
                          child: Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Container(
                                width: 40,
                                height: 40,
                                decoration: BoxDecoration(
                                  color: cat.categoryColor.withValues(
                                    alpha: 0.15,
                                  ),
                                  borderRadius: BorderRadius.circular(10),
                                ),
                                child: Icon(
                                  _iconData(cat.icon),
                                  color: cat.categoryColor,
                                  size: 20,
                                ),
                              ),
                              const SizedBox(height: 4),
                              Text(
                                cat.name,
                                style: AppTypography.badgeLabel.copyWith(
                                  color: isSelected
                                      ? cat.categoryColor
                                      : AppColors.onSurfaceVariant,
                                  fontSize: 9,
                                ),
                                textAlign: TextAlign.center,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ],
                          ),
                        ),
                      );
                    },
                  ),
                ),

                const Divider(height: 1),
                const SizedBox(height: 12),

                // Amount display
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 20),
                  child: Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 10,
                          vertical: 6,
                        ),
                        decoration: BoxDecoration(
                          color: AppColors.surfaceContainerLow,
                          borderRadius: BorderRadius.circular(99),
                        ),
                        child: Text(
                          'IDR',
                          style: AppTypography.metaData.copyWith(
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: FittedBox(
                          alignment: Alignment.centerLeft,
                          fit: BoxFit.scaleDown,
                          child: Text(
                            'Rp$_formattedAmount',
                            style: AppTypography.amountLgMobile.copyWith(
                              color: _typeColor,
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 12),

                // Meta fields: title, note, date, account
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 20),
                  child: Column(
                    children: [
                      TextField(
                        controller: _titleCtrl,
                        textCapitalization: TextCapitalization.sentences,
                        decoration: const InputDecoration(
                          hintText: 'Judul transaksi',
                          prefixIcon: Icon(Icons.edit_note_rounded, size: 20),
                        ),
                        style: AppTypography.bodyMain,
                      ),
                      const SizedBox(height: 8),
                      TextField(
                        controller: _noteCtrl,
                        decoration: const InputDecoration(
                          hintText: 'Catatan (opsional)',
                          prefixIcon: Icon(Icons.note_outlined, size: 20),
                        ),
                        style: AppTypography.bodyMain,
                      ),
                      const SizedBox(height: 8),

                      // Date + Account row
                      Row(
                        children: [
                          Expanded(
                            child: GestureDetector(
                              onTap: _pickDate,
                              child: Container(
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 12,
                                  vertical: 12,
                                ),
                                decoration: BoxDecoration(
                                  border: Border.all(
                                    color: AppColors.outlineVariant,
                                  ),
                                  borderRadius: BorderRadius.circular(12),
                                ),
                                child: Row(
                                  children: [
                                    const Icon(
                                      Icons.calendar_today_outlined,
                                      size: 18,
                                      color: AppColors.onSurfaceVariant,
                                    ),
                                    const SizedBox(width: 8),
                                    Text(
                                      AppFormatters.formatDateShort(
                                        _selectedDate,
                                      ),
                                      style: AppTypography.bodyMain,
                                    ),
                                  ],
                                ),
                              ),
                            ),
                          ),
                          const SizedBox(width: 8),
                          Expanded(
                            child: accountsAsync.when(
                              data: (accounts) {
                                if (accounts.isEmpty) {
                                  return Container(
                                    padding: const EdgeInsets.symmetric(
                                      horizontal: 12,
                                      vertical: 12,
                                    ),
                                    decoration: BoxDecoration(
                                      border: Border.all(
                                        color: AppColors.outlineVariant,
                                      ),
                                      borderRadius: BorderRadius.circular(12),
                                    ),
                                    child: Text(
                                      'Buat akun dulu',
                                      style: AppTypography.metaData.copyWith(
                                        color: AppColors.onSurfaceVariant,
                                      ),
                                    ),
                                  );
                                }
                                final selectedAccountId =
                                    accounts.any(
                                      (account) =>
                                          account.id == _selectedAccountId,
                                    )
                                    ? _selectedAccountId
                                    : accounts.first.id;

                                return Container(
                                  decoration: BoxDecoration(
                                    border: Border.all(
                                      color: AppColors.outlineVariant,
                                    ),
                                    borderRadius: BorderRadius.circular(12),
                                  ),
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 12,
                                  ),
                                  child: DropdownButtonHideUnderline(
                                    child: DropdownButton<String>(
                                      value: selectedAccountId,
                                      isExpanded: true,
                                      icon: const Icon(
                                        Icons.expand_more_rounded,
                                        size: 18,
                                      ),
                                      items: accounts
                                          .map(
                                            (a) => DropdownMenuItem(
                                              value: a.id,
                                              child: Text(
                                                a.name,
                                                style: AppTypography.bodyMain,
                                                overflow: TextOverflow.ellipsis,
                                              ),
                                            ),
                                          )
                                          .toList(),
                                      onChanged: (id) => setState(
                                        () => _selectedAccountId = id,
                                      ),
                                    ),
                                  ),
                                );
                              },
                              loading: () => const SizedBox(
                                height: 48,
                                child: Center(
                                  child: CircularProgressIndicator(
                                    strokeWidth: 2,
                                  ),
                                ),
                              ),
                              error: (_, __) => const SizedBox.shrink(),
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),

                const SizedBox(height: 12),
                const Divider(height: 1),

                // Numpad
                Padding(
                  padding: const EdgeInsets.all(14),
                  child: _buildNumpad(),
                ),

                // Save button
                Padding(
                  padding: EdgeInsets.fromLTRB(
                    20,
                    0,
                    20,
                    MediaQuery.viewInsetsOf(context).bottom + 20,
                  ),
                  child: SizedBox(
                    height: 52,
                    child: ElevatedButton(
                      onPressed: (_numpadInput.isEmpty || _isSaving)
                          ? null
                          : _save,
                      style: ElevatedButton.styleFrom(
                        backgroundColor: _typeColor,
                        disabledBackgroundColor: AppColors.outlineVariant,
                      ),
                      child: _isSaving
                          ? const SizedBox(
                              width: 22,
                              height: 22,
                              child: CircularProgressIndicator(
                                color: Colors.white,
                                strokeWidth: 2,
                              ),
                            )
                          : Text(
                              'Simpan $_typeLabel',
                              style: AppTypography.bodyMain.copyWith(
                                color: Colors.white,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                    ),
                  ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }

  Widget _buildNumpad() {
    const keys = [
      '1',
      '2',
      '3',
      '⌫',
      '4',
      '5',
      '6',
      '000',
      '7',
      '8',
      '9',
      '00',
      '📅',
      '0',
      '.',
      '✓',
    ];

    return GridView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 4,
        childAspectRatio: 1.5,
        mainAxisSpacing: 6,
        crossAxisSpacing: 6,
      ),
      itemCount: keys.length,
      itemBuilder: (context, i) {
        final key = keys[i];
        final isBackspace = key == '⌫';
        final isConfirm = key == '✓';
        final isCalendar = key == '📅';

        Color bg = AppColors.surfaceContainerLow;
        Color fg = AppColors.onSurface;
        if (isBackspace) {
          bg = AppColors.expense.withValues(alpha: 0.1);
          fg = AppColors.expense;
        } else if (isConfirm) {
          bg = _typeColor;
          fg = Colors.white;
        }

        return GestureDetector(
          onTap: () {
            if (isCalendar) {
              _pickDate();
            } else if (isConfirm) {
              _save();
            } else {
              _onNumpad(key);
            }
          },
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 80),
            decoration: BoxDecoration(
              color: bg,
              borderRadius: BorderRadius.circular(10),
            ),
            child: Center(
              child: isCalendar
                  ? Icon(
                      Icons.calendar_today_outlined,
                      color: AppColors.onSurfaceVariant,
                      size: 20,
                    )
                  : isBackspace
                  ? Icon(Icons.backspace_outlined, color: fg, size: 20)
                  : isConfirm
                  ? Icon(Icons.check_rounded, color: fg, size: 24)
                  : Text(
                      key,
                      style: AppTypography.numpadDigit.copyWith(color: fg),
                    ),
            ),
          ),
        );
      },
    );
  }

  IconData _iconData(String name) {
    const map = {
      'restaurant': Icons.restaurant,
      'directions_bus': Icons.directions_bus,
      'local_mall': Icons.local_mall,
      'movie': Icons.movie,
      'local_hospital': Icons.local_hospital,
      'school': Icons.school,
      'receipt_long': Icons.receipt_long,
      'home': Icons.home,
      'savings': Icons.savings,
      'trending_up': Icons.trending_up,
      'more_horiz': Icons.more_horiz,
      'attach_money': Icons.attach_money,
      'work_outline': Icons.work_outline,
      'store': Icons.store,
      'card_giftcard': Icons.card_giftcard,
      'category': Icons.category,
      'people': Icons.people,
      'sports_esports': Icons.sports_esports,
    };
    return map[name] ?? Icons.category;
  }
}
