import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_typography.dart';
import '../../../core/utils/formatters.dart';
import '../../../shared/models/goal_model.dart';

/// Goals / Financial Target screen
class GoalsScreen extends StatefulWidget {
  const GoalsScreen({super.key});

  @override
  State<GoalsScreen> createState() => _GoalsScreenState();
}

class _GoalsScreenState extends State<GoalsScreen> {
  final List<GoalModel> _goals = [];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bgLight,
      appBar: AppBar(
        title: Text('Target Keuangan', style: AppTypography.sectionTitle),
        actions: [
          IconButton(
            icon: const Icon(Icons.add_rounded),
            onPressed: () {},
          ),
        ],
      ),
      body: _goals.isEmpty ? _buildEmpty() : _buildList(),
    );
  }

  Widget _buildEmpty() {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 80, height: 80,
            decoration: BoxDecoration(
              color: AppColors.tertiary.withValues(alpha: 0.1),
              borderRadius: BorderRadius.circular(20),
            ),
            child: const Icon(Icons.flag_outlined,
                color: AppColors.tertiary, size: 40),
          ),
          const SizedBox(height: 20),
          Text('Belum ada target', style: AppTypography.sectionTitle),
          const SizedBox(height: 8),
          Text('Tetapkan target keuanganmu dan\npantau perkembangannya',
              style: AppTypography.bodyMain
                  .copyWith(color: AppColors.onSurfaceVariant),
              textAlign: TextAlign.center),
          const SizedBox(height: 24),
          ElevatedButton.icon(
            onPressed: () {},
            icon: const Icon(Icons.add_rounded, size: 20),
            label: const Text('Tambah Target'),
          ),
        ],
      ),
    );
  }

  Widget _buildList() {
    return ListView.builder(
      padding: const EdgeInsets.all(20),
      itemCount: _goals.length,
      itemBuilder: (context, i) => _GoalCard(goal: _goals[i]),
    );
  }
}

class _GoalCard extends StatelessWidget {
  final GoalModel goal;
  const _GoalCard({required this.goal});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: AppColors.surfaceContainerLowest,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.outlineVariant, width: 0.5),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 48, height: 48,
                decoration: BoxDecoration(
                  color: goal.goalColor.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(Icons.flag, color: goal.goalColor, size: 24),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(goal.name, style: AppTypography.bodyMain
                        .copyWith(fontWeight: FontWeight.w600)),
                    Text(
                      goal.isAchieved
                          ? '🎉 Target tercapai!'
                          : '${goal.daysRemaining} hari lagi',
                      style: AppTypography.metaData.copyWith(
                          color: goal.isAchieved
                              ? AppColors.income
                              : AppColors.onSurfaceVariant),
                    ),
                  ],
                ),
              ),
              Text(
                '${goal.progressPercent.toStringAsFixed(0)}%',
                style: AppTypography.sectionTitle.copyWith(
                    color: goal.goalColor),
              ),
            ],
          ),
          const SizedBox(height: 16),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(AppFormatters.formatRupiah(goal.currentAmount),
                  style: AppTypography.amountSm
                      .copyWith(color: AppColors.income)),
              Text(AppFormatters.formatRupiah(goal.targetAmount),
                  style: AppTypography.amountSm
                      .copyWith(color: AppColors.onSurfaceVariant)),
            ],
          ),
          const SizedBox(height: 8),
          ClipRRect(
            borderRadius: BorderRadius.circular(4),
            child: LinearProgressIndicator(
              value: goal.progressPercent / 100,
              minHeight: 8,
              backgroundColor: AppColors.surfaceContainerHigh,
              valueColor: AlwaysStoppedAnimation<Color>(goal.goalColor),
            ),
          ),
          const SizedBox(height: 8),
          Text(
            'Sisa ${AppFormatters.formatRupiah(goal.remainingAmount)} lagi',
            style: AppTypography.metaData
                .copyWith(color: AppColors.onSurfaceVariant),
          ),
        ],
      ),
    );
  }
}
