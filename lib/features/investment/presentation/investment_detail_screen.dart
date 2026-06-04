import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_typography.dart';
import '../../../core/utils/formatters.dart';
import '../../../shared/models/investment_model.dart';
import '../data/investment_repository.dart';

class InvestmentDetailScreen extends ConsumerStatefulWidget {
  final InvestmentModel investment;

  const InvestmentDetailScreen({super.key, required this.investment});

  @override
  ConsumerState<InvestmentDetailScreen> createState() => _InvestmentDetailScreenState();
}

class _InvestmentDetailScreenState extends ConsumerState<InvestmentDetailScreen> {
  final _currentValueCtrl = TextEditingController();
  bool _isUpdating = false;

  @override
  void initState() {
    super.initState();
    _currentValueCtrl.text = widget.investment.currentValue.toStringAsFixed(0);
  }

  Future<void> _updateValue() async {
    final val = double.tryParse(_currentValueCtrl.text) ?? 0.0;
    if (val <= 0) return;

    setState(() => _isUpdating = true);
    await ref.read(investmentRepositoryProvider).updateCurrentValue(widget.investment.id, val);
    
    if (mounted) {
      setState(() => _isUpdating = false);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Nilai investasi berhasil diperbarui')),
      );
      context.pop();
    }
  }

  @override
  Widget build(BuildContext context) {
    final isProfit = widget.investment.isProfit;

    return Scaffold(
      backgroundColor: AppColors.bgLight,
      appBar: AppBar(
        title: Text(widget.investment.name, style: AppTypography.sectionTitle),
        centerTitle: true,
        actions: [
          IconButton(
            icon: const Icon(Icons.delete_outline_rounded, color: AppColors.expense),
            onPressed: () async {
              await ref.read(investmentRepositoryProvider).deleteInvestment(widget.investment.id);
              if (context.mounted) context.pop();
            },
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          // Summary Card
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              gradient: LinearGradient(
                colors: isProfit 
                  ? [AppColors.income, AppColors.primaryContainer] 
                  : [AppColors.expense, const Color(0xFFD32F2F)],
              ),
              borderRadius: BorderRadius.circular(20),
            ),
            child: Column(
              children: [
                Text('Nilai Saat Ini', style: AppTypography.metaData.copyWith(color: Colors.white70)),
                const SizedBox(height: 8),
                Text(AppFormatters.formatRupiah(widget.investment.currentValue), 
                  style: AppTypography.amountLg.copyWith(color: Colors.white)),
                const SizedBox(height: 16),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('Modal Awal', style: AppTypography.metaData.copyWith(color: Colors.white70)),
                        Text(AppFormatters.formatRupiah(widget.investment.initialAmount), 
                          style: AppTypography.bodyMain.copyWith(color: Colors.white, fontWeight: FontWeight.w600)),
                      ],
                    ),
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: [
                        Text('Return', style: AppTypography.metaData.copyWith(color: Colors.white70)),
                        Text('${isProfit ? '+' : ''}${AppFormatters.formatPercent(widget.investment.profitLossPercent)}', 
                          style: AppTypography.bodyMain.copyWith(color: Colors.white, fontWeight: FontWeight.w600)),
                      ],
                    ),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(height: 24),
          
          Text('Update Nilai Pasar', style: AppTypography.sectionTitle),
          const SizedBox(height: 12),
          TextField(
            controller: _currentValueCtrl,
            keyboardType: TextInputType.number,
            decoration: const InputDecoration(
              labelText: 'Masukkan nilai investasi terkini (Rp)',
              prefixIcon: Icon(Icons.show_chart_rounded),
            ),
          ),
          const SizedBox(height: 16),
          SizedBox(
            width: double.infinity,
            height: 52,
            child: ElevatedButton(
              onPressed: _isUpdating ? null : _updateValue,
              style: ElevatedButton.styleFrom(backgroundColor: AppColors.primary),
              child: _isUpdating 
                ? const CircularProgressIndicator(color: Colors.white)
                : Text('Perbarui Nilai', style: AppTypography.bodyMain.copyWith(color: Colors.white, fontWeight: FontWeight.w600)),
            ),
          ),
        ],
      ),
    );
  }
}
