import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_typography.dart';
import '../../../core/utils/formatters.dart';
import '../../../shared/models/budget_model.dart';
import '../../../shared/models/category_model.dart';
import '../data/budget_repository.dart';

class AddBudgetScreen extends ConsumerStatefulWidget {
  final BudgetModel? existingBudget;
  const AddBudgetScreen({super.key, this.existingBudget});

  @override
  ConsumerState<AddBudgetScreen> createState() => _AddBudgetScreenState();
}

class _AddBudgetScreenState extends ConsumerState<AddBudgetScreen> {
  String _numpadInput = '';
  String _selectedCategoryId = 'exp_makan';
  bool _isSaving = false;

  final List<CategoryModel> _categories = DefaultCategories.expense;

  CategoryModel? get _selectedCategory {
    try {
      return _categories.firstWhere((c) => c.id == _selectedCategoryId);
    } catch (_) {
      return _categories.isNotEmpty ? _categories.first : null;
    }
  }

  @override
  void initState() {
    super.initState();
    if (widget.existingBudget != null) {
      final b = widget.existingBudget!;
      final amountStr = b.limit.toInt().toString();
      _numpadInput = amountStr == '0' ? '' : amountStr;
      _selectedCategoryId = b.categoryId;
    }
  }

  String get _formattedAmount {
    if (_numpadInput.isEmpty) return '0';
    final n = int.tryParse(_numpadInput) ?? 0;
    return AppFormatters.formatNumpadInput(n.toString());
  }

  double get _parsedAmount =>
      double.tryParse(_numpadInput.isEmpty ? '0' : _numpadInput) ?? 0;

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

  Future<void> _save() async {
    if (_parsedAmount <= 0) return;
    final cat = _selectedCategory;
    if (cat == null) return;

    setState(() => _isSaving = true);
    final now = DateTime.now();
    final monthStr = '${now.year}-${now.month.toString().padLeft(2, '0')}';

    final isEdit = widget.existingBudget != null;
    final budget = BudgetModel(
      id: isEdit ? widget.existingBudget!.id : '${cat.id}_$monthStr',
      categoryId: cat.id,
      categoryName: cat.name,
      categoryIcon: cat.icon,
      categoryColor: cat.color,
      limit: _parsedAmount,
      spent: isEdit ? widget.existingBudget!.spent : 0,
      period: BudgetPeriod.monthly,
      startDate: isEdit ? widget.existingBudget!.startDate : DateTime(now.year, now.month, 1),
      endDate: isEdit ? widget.existingBudget!.endDate : DateTime(now.year, now.month + 1, 0),
      createdAt: isEdit ? widget.existingBudget!.createdAt : now,
      month: isEdit ? widget.existingBudget!.month : monthStr,
    );

    try {
      await ref.read(budgetRepositoryProvider).saveBudget(budget);
      if (!mounted) return;
      HapticFeedback.mediumImpact();
      Navigator.of(context).pop();
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(isEdit ? 'Anggaran berhasil diubah' : 'Anggaran berhasil disimpan'),
          backgroundColor: AppColors.primary,
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

  Future<void> _confirmDelete() async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (c) => AlertDialog(
        title: Text('Hapus Anggaran?', style: AppTypography.sectionTitle),
        content: Text('Apakah Anda yakin ingin menghapus anggaran ini?', style: AppTypography.bodyMain),
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

    if (confirm == true && mounted && widget.existingBudget != null) {
      setState(() => _isSaving = true);
      try {
        await ref.read(budgetRepositoryProvider).deleteBudget(widget.existingBudget!.id);
        if (!mounted) return;
        Navigator.pop(context);
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Anggaran dihapus'), backgroundColor: AppColors.expense),
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

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.transparent,
      body: DraggableScrollableSheet(
        initialChildSize: 0.90,
        minChildSize: 0.6,
        maxChildSize: 0.95,
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

                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(widget.existingBudget != null ? 'Edit Anggaran' : 'Buat Anggaran', style: AppTypography.sectionTitle),
                      if (widget.existingBudget != null)
                        IconButton(
                          onPressed: _isSaving ? null : _confirmDelete,
                          icon: const Icon(Icons.delete_outline_rounded, color: AppColors.expense),
                        ),
                    ],
                  ),
                ),

                // Category picker
                Padding(
                  padding: const EdgeInsets.only(left: 20, top: 4, bottom: 8),
                  child: Text('Kategori',
                      style: AppTypography.metaData
                          .copyWith(color: AppColors.onSurfaceVariant)),
                ),
                SizedBox(
                  height: 88,
                  child: ListView.builder(
                    scrollDirection: Axis.horizontal,
                    padding: const EdgeInsets.symmetric(horizontal: 20),
                    itemCount: _categories.length,
                    itemBuilder: (context, i) {
                      final cat = _categories[i];
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
                                    color: cat.categoryColor, width: 1.5)
                                : null,
                          ),
                          child: Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Container(
                                width: 40,
                                height: 40,
                                decoration: BoxDecoration(
                                  color: cat.categoryColor
                                      .withValues(alpha: 0.15),
                                  borderRadius: BorderRadius.circular(10),
                                ),
                                child: Icon(_iconData(cat.icon),
                                    color: cat.categoryColor, size: 20),
                              ),
                              const SizedBox(height: 4),
                              Text(cat.name,
                                  style: AppTypography.badgeLabel.copyWith(
                                      color: isSelected
                                          ? cat.categoryColor
                                          : AppColors.onSurfaceVariant,
                                      fontSize: 9),
                                  textAlign: TextAlign.center,
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis),
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
                            horizontal: 10, vertical: 6),
                        decoration: BoxDecoration(
                          color: AppColors.surfaceContainerLow,
                          borderRadius: BorderRadius.circular(99),
                        ),
                        child: Text('IDR',
                            style: AppTypography.metaData.copyWith(
                                fontWeight: FontWeight.w700)),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: FittedBox(
                          alignment: Alignment.centerLeft,
                          fit: BoxFit.scaleDown,
                          child: Text(
                            'Rp$_formattedAmount',
                            style: AppTypography.amountLgMobile
                                .copyWith(color: AppColors.primary),
                          ),
                        ),
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
                      MediaQuery.viewInsetsOf(context).bottom + 20),
                  child: SizedBox(
                    height: 52,
                    child: ElevatedButton(
                      onPressed: (_numpadInput.isEmpty || _isSaving)
                          ? null
                          : _save,
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppColors.primary,
                        disabledBackgroundColor: AppColors.outlineVariant,
                      ),
                      child: _isSaving
                          ? const SizedBox(
                              width: 22,
                              height: 22,
                              child: CircularProgressIndicator(
                                  color: Colors.white, strokeWidth: 2),
                            )
                          : Text('Simpan Anggaran',
                              style: AppTypography.bodyMain.copyWith(
                                  color: Colors.white,
                                  fontWeight: FontWeight.w600)),
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
      '1', '2', '3', '⌫',
      '4', '5', '6', '000',
      '7', '8', '9', '00',
      '',  '0', '.', '✓',
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
        if (key.isEmpty) return const SizedBox.shrink();

        final isBackspace = key == '⌫';
        final isConfirm = key == '✓';

        Color bg = AppColors.surfaceContainerLow;
        Color fg = AppColors.onSurface;
        if (isBackspace) {
          bg = AppColors.expense.withValues(alpha: 0.1);
          fg = AppColors.expense;
        } else if (isConfirm) {
          bg = AppColors.primary;
          fg = Colors.white;
        }

        return GestureDetector(
          onTap: () {
            if (isConfirm) {
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
              child: isBackspace
                  ? Icon(Icons.backspace_outlined, color: fg, size: 20)
                  : isConfirm
                      ? Icon(Icons.check_rounded, color: fg, size: 24)
                      : Text(key,
                          style: AppTypography.numpadDigit
                              .copyWith(color: fg)),
            ),
          ),
        );
      },
    );
  }
}
