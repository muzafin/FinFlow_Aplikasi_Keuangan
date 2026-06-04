import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_typography.dart';
import '../../../shared/models/investment_model.dart';
import '../data/investment_repository.dart';

class AddInvestmentScreen extends ConsumerStatefulWidget {
  const AddInvestmentScreen({super.key});

  @override
  ConsumerState<AddInvestmentScreen> createState() => _AddInvestmentScreenState();
}

class _AddInvestmentScreenState extends ConsumerState<AddInvestmentScreen> {
  final _nameCtrl = TextEditingController();
  final _platformCtrl = TextEditingController();
  final _amountCtrl = TextEditingController();
  InvestmentType _type = InvestmentType.saham;
  bool _isLoading = false;

  @override
  void dispose() {
    _nameCtrl.dispose();
    _platformCtrl.dispose();
    _amountCtrl.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    final name = _nameCtrl.text.trim();
    final amountStr = _amountCtrl.text.replaceAll('.', '').replaceAll(',', '.');
    final amount = double.tryParse(amountStr) ?? 0.0;

    if (name.isEmpty || amount <= 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Nama dan jumlah investasi harus diisi')),
      );
      return;
    }

    setState(() => _isLoading = true);
    final now = DateTime.now();
    
    final inv = InvestmentModel(
      id: 'inv_${now.millisecondsSinceEpoch}',
      name: name,
      type: _type,
      initialAmount: amount,
      currentValue: amount, // Awalnya current = initial
      purchaseDate: now,
      platform: _platformCtrl.text.trim(),
      createdAt: now,
      updatedAt: now,
    );

    try {
      await ref.read(investmentRepositoryProvider).saveInvestment(inv);
      if (mounted) {
        context.pop();
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isLoading = false);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Gagal menyimpan: $e')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bgLight,
      appBar: AppBar(
        title: Text('Tambah Investasi', style: AppTypography.sectionTitle),
        centerTitle: true,
        backgroundColor: AppColors.surfaceContainerLowest,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.close_rounded),
          onPressed: () => context.pop(),
        ),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Jenis Investasi', style: AppTypography.bodyMain.copyWith(fontWeight: FontWeight.w600)),
            const SizedBox(height: 12),
            Wrap(
              spacing: 8, runSpacing: 8,
              children: InvestmentType.values.map((t) {
                final isSelected = _type == t;
                return ChoiceChip(
                  label: Text(t.name),
                  selected: isSelected,
                  onSelected: (val) {
                    if (val) setState(() => _type = t);
                  },
                  selectedColor: AppColors.primary.withValues(alpha: 0.2),
                  labelStyle: AppTypography.bodyMain.copyWith(
                    color: isSelected ? AppColors.primary : AppColors.onSurfaceVariant,
                    fontWeight: isSelected ? FontWeight.w600 : FontWeight.w400,
                  ),
                );
              }).toList(),
            ),
            const SizedBox(height: 24),

            TextField(
              controller: _nameCtrl,
              textCapitalization: TextCapitalization.words,
              decoration: const InputDecoration(
                labelText: 'Nama Investasi (cth. BBCA, Reksadana Sucor)',
                prefixIcon: Icon(Icons.business_center_rounded),
              ),
            ),
            const SizedBox(height: 16),
            
            TextField(
              controller: _platformCtrl,
              textCapitalization: TextCapitalization.words,
              decoration: const InputDecoration(
                labelText: 'Platform / Broker (cth. Bibit, Ajaib)',
                prefixIcon: Icon(Icons.account_balance_rounded),
              ),
            ),
            const SizedBox(height: 16),
            
            TextField(
              controller: _amountCtrl,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(
                labelText: 'Jumlah Investasi Awal (Rp)',
                prefixIcon: Icon(Icons.attach_money_rounded),
              ),
            ),
            const SizedBox(height: 32),

            SizedBox(
              width: double.infinity,
              height: 52,
              child: ElevatedButton(
                onPressed: _isLoading ? null : _save,
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppColors.primary,
                ),
                child: _isLoading
                  ? const CircularProgressIndicator(color: Colors.white)
                  : Text('Simpan Portofolio', style: AppTypography.bodyMain.copyWith(color: Colors.white, fontWeight: FontWeight.w600)),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
