import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:hive_flutter/hive_flutter.dart';
import '../../../core/constants/app_constants.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_typography.dart';
import '../../../core/services/notification_service.dart';

class NotificationSettingsScreen extends ConsumerStatefulWidget {
  const NotificationSettingsScreen({super.key});

  @override
  ConsumerState<NotificationSettingsScreen> createState() => _NotificationSettingsScreenState();
}

class _NotificationSettingsScreenState extends ConsumerState<NotificationSettingsScreen> {
  bool _dailyReminder = false;
  bool _budgetAlerts = false;
  bool _isLoading = false;
  TimeOfDay _reminderTime = const TimeOfDay(hour: 20, minute: 0);

  @override
  void initState() {
    super.initState();
    _loadSettings();
  }

  Future<void> _loadSettings() async {
    setState(() => _isLoading = true);
    await ref.read(notificationServiceProvider).initialize();
    
    final box = await Hive.openBox(AppConstants.settingsBox);
    final reminderOn = box.get('dailyReminderOn', defaultValue: false) as bool;
    final reminderHour = box.get('dailyReminderHour', defaultValue: 20) as int;
    final reminderMinute = box.get('dailyReminderMinute', defaultValue: 0) as int;
    final budgetAlertsOn = box.get('budgetAlertsOn', defaultValue: true) as bool;

    setState(() {
      _dailyReminder = reminderOn;
      _reminderTime = TimeOfDay(hour: reminderHour, minute: reminderMinute);
      _budgetAlerts = budgetAlertsOn;
      _isLoading = false;
    });
  }

  Future<void> _saveDailyReminder(bool on, TimeOfDay time) async {
    final box = await Hive.openBox(AppConstants.settingsBox);
    await box.put('dailyReminderOn', on);
    await box.put('dailyReminderHour', time.hour);
    await box.put('dailyReminderMinute', time.minute);

    if (on) {
      await ref.read(notificationServiceProvider).scheduleDailyReminder(time);
    } else {
      await ref.read(notificationServiceProvider).cancelDailyReminder();
    }
  }

  Future<void> _pickTime() async {
    if (!_dailyReminder) return;
    final newTime = await showTimePicker(
      context: context,
      initialTime: _reminderTime,
      builder: (context, child) {
        return Theme(
          data: Theme.of(context).copyWith(
            colorScheme: AppColors.lightColorScheme,
          ),
          child: child!,
        );
      },
    );
    if (newTime != null && newTime != _reminderTime) {
      setState(() => _reminderTime = newTime);
      await _saveDailyReminder(true, newTime);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bgLight,
      appBar: AppBar(
        title: Text('Pengaturan Notifikasi', style: AppTypography.sectionTitle),
        centerTitle: true,
        backgroundColor: AppColors.surfaceContainerLowest,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded),
          onPressed: () => context.pop(),
        ),
      ),
      body: _isLoading 
        ? const Center(child: CircularProgressIndicator())
        : ListView(
            padding: const EdgeInsets.all(20),
            children: [
              Text('Pengingat', style: AppTypography.sectionTitle.copyWith(color: AppColors.primary, fontSize: 16)),
              const SizedBox(height: 12),
              
              _buildSwitchTile(
                title: 'Pengingat Harian',
                subtitle: 'Ingatkan saya untuk mencatat transaksi',
                value: _dailyReminder,
                onChanged: (val) {
                  setState(() => _dailyReminder = val);
                  _saveDailyReminder(val, _reminderTime);
                },
                trailingContent: _dailyReminder ? GestureDetector(
                  onTap: _pickTime,
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                    decoration: BoxDecoration(
                      color: AppColors.primaryContainer.withValues(alpha: 0.1),
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: AppColors.primary),
                    ),
                    child: Text(
                      _reminderTime.format(context),
                      style: AppTypography.badgeLabel.copyWith(color: AppColors.primary, fontWeight: FontWeight.bold),
                    ),
                  ),
                ) : null,
              ),
              
              const Divider(height: 32),
              Text('Peringatan', style: AppTypography.sectionTitle.copyWith(color: AppColors.primary, fontSize: 16)),
              const SizedBox(height: 12),

              _buildSwitchTile(
                title: 'Peringatan Anggaran',
                subtitle: 'Beri tahu jika pengeluaran mendekati batas anggaran bulanan',
                value: _budgetAlerts,
                onChanged: (val) async {
                  setState(() => _budgetAlerts = val);
                  final box = await Hive.openBox(AppConstants.settingsBox);
                  await box.put('budgetAlertsOn', val);
                  if (val) {
                    ref.read(notificationServiceProvider).subscribeToTopic('budget_alerts');
                  } else {
                    ref.read(notificationServiceProvider).unsubscribeFromTopic('budget_alerts');
                  }
                },
              ),
            ],
          ),
    );
  }

  Widget _buildSwitchTile({
    required String title,
    required String subtitle,
    required bool value,
    required ValueChanged<bool> onChanged,
    Widget? trailingContent,
  }) {
    return Container(
      padding: const EdgeInsets.all(16),
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
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(title, style: AppTypography.bodyMain.copyWith(fontWeight: FontWeight.w600)),
                    const SizedBox(height: 4),
                    Text(subtitle, style: AppTypography.metaData.copyWith(color: AppColors.onSurfaceVariant)),
                  ],
                ),
              ),
              const SizedBox(width: 16),
              Switch(
                value: value,
                onChanged: onChanged,
                activeThumbColor: AppColors.primary,
              ),
            ],
          ),
          if (trailingContent != null) ...[
            const SizedBox(height: 12),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text('Waktu Pengingat', style: AppTypography.metaData.copyWith(color: AppColors.onSurfaceVariant)),
                trailingContent,
              ],
            )
          ]
        ],
      ),
    );
  }
}
