import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_typography.dart';
import '../../../core/services/firebase_service.dart';
import '../../../features/auth/data/auth_repository.dart';
import 'package:local_auth/local_auth.dart';
import 'package:hive_flutter/hive_flutter.dart';
import '../../../core/constants/app_constants.dart';

/// Profile & Settings screen
class ProfileScreen extends ConsumerStatefulWidget {
  const ProfileScreen({super.key});

  @override
  ConsumerState<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends ConsumerState<ProfileScreen> {
  bool _notifEnabled = false;
  bool _biometricEnabled = false;
  final LocalAuthentication _auth = LocalAuthentication();

  @override
  void initState() {
    super.initState();
    _loadSettings();
  }

  Future<void> _loadSettings() async {
    final box = await Hive.openBox(AppConstants.settingsBox);
    setState(() {
      _notifEnabled = box.get('dailyReminderOn', defaultValue: false);
      _biometricEnabled = box.get('biometricEnabled', defaultValue: false);
    });
  }

  Future<void> _toggleBiometric(bool value) async {
    if (value) {
      try {
        final bool canAuthenticateWithBiometrics = await _auth.canCheckBiometrics;
        final bool canAuthenticate = canAuthenticateWithBiometrics || await _auth.isDeviceSupported();
        if (!canAuthenticate) {
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text('Perangkat tidak mendukung biometrik')),
            );
          }
          return;
        }

        final authenticated = await _auth.authenticate(
          localizedReason: 'Konfirmasi identitas Anda untuk mengaktifkan biometrik',
          biometricOnly: false,
          persistAcrossBackgrounding: true,
        );

        if (authenticated) {
          final box = await Hive.openBox(AppConstants.settingsBox);
          await box.put('biometricEnabled', true);
          setState(() => _biometricEnabled = true);
        }
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Error: $e')),
          );
        }
      }
    } else {
      final box = await Hive.openBox(AppConstants.settingsBox);
      await box.put('biometricEnabled', false);
      setState(() => _biometricEnabled = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final user = FirebaseService.currentUser;
    final displayName = user?.displayName ?? 'Pengguna';
    final email = user?.email ?? '';
    final photoUrl = user?.photoURL;

    return CustomScrollView(
      slivers: [
        // Profile header
        SliverToBoxAdapter(
          child: Container(
            padding: const EdgeInsets.fromLTRB(20, 60, 20, 32),
            decoration: const BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                colors: [AppColors.primary, AppColors.surface],
              ),
            ),
            child: Column(
              children: [
                // Avatar
                Stack(
                  children: [
                    Container(
                      width: 88, height: 88,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        color: AppColors.primaryFixed,
                        border: Border.all(color: Colors.white, width: 3),
                      ),
                      child: ClipOval(
                        child: photoUrl != null
                            ? Image.network(photoUrl, fit: BoxFit.cover)
                            : Center(
                                child: Text(
                                  displayName[0].toUpperCase(),
                                  style: AppTypography.appTitle.copyWith(
                                      color: AppColors.primary,
                                      fontSize: 36),
                                ),
                              ),
                      ),
                    ),
                    Positioned(
                      bottom: 0, right: 0,
                      child: Container(
                        width: 28, height: 28,
                        decoration: BoxDecoration(
                          color: AppColors.primary,
                          shape: BoxShape.circle,
                          border: Border.all(color: Colors.white, width: 2),
                        ),
                        child: const Icon(Icons.edit_rounded,
                            color: Colors.white, size: 14),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                Text(displayName,
                    style: AppTypography.sectionTitle
                        .copyWith(color: AppColors.onSurface)),
                const SizedBox(height: 4),
                Text(email,
                    style: AppTypography.bodyMain
                        .copyWith(color: AppColors.onSurfaceVariant)),
                const SizedBox(height: 12),
                // Level badge
                Container(
                  padding: const EdgeInsets.symmetric(
                      horizontal: 16, vertical: 6),
                  decoration: BoxDecoration(
                    color: AppColors.primaryContainer,
                    borderRadius: BorderRadius.circular(99),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Icon(Icons.workspace_premium_rounded,
                          color: Colors.white, size: 14),
                      const SizedBox(width: 6),
                      Text('Level 1 — Pemula',
                          style: AppTypography.badgeLabel
                              .copyWith(color: Colors.white)),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),

        // Settings groups
        SliverToBoxAdapter(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(20, 0, 20, 100),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Preferensi
                _SettingsGroup(
                  title: 'Preferensi',
                  children: [
                    _SettingsTile(
                      icon: Icons.palette_outlined,
                      iconColor: AppColors.shoppingAccent,
                      title: 'Tema Aplikasi',
                      subtitle: 'Terang',
                      onTap: () {},
                      trailing: const Icon(Icons.chevron_right_rounded,
                          color: AppColors.outlineVariant),
                    ),
                    _SettingsTile(
                      icon: Icons.language_rounded,
                      iconColor: AppColors.transportAccent,
                      title: 'Bahasa',
                      subtitle: 'Bahasa Indonesia',
                      onTap: () {},
                      trailing: const Icon(Icons.chevron_right_rounded,
                          color: AppColors.outlineVariant),
                    ),
                    _SettingsTile(
                      icon: Icons.attach_money_rounded,
                      iconColor: AppColors.income,
                      title: 'Mata Uang',
                      subtitle: 'IDR (Rupiah)',
                      onTap: () {},
                      trailing: const Icon(Icons.chevron_right_rounded,
                          color: AppColors.outlineVariant),
                    ),
                  ],
                ),
                const SizedBox(height: 20),

                // Notifikasi
                _SettingsGroup(
                  title: 'Notifikasi',
                  children: [
                    _SettingsTile(
                      icon: Icons.notifications_outlined,
                      iconColor: AppColors.foodAccent,
                      title: 'Notifikasi',
                      subtitle: 'Pengingat dan peringatan',
                      onTap: () => context.push('/settings/notifications'),
                      trailing: Switch(
                        value: _notifEnabled,
                        onChanged: (v) =>
                            setState(() => _notifEnabled = v),
                        activeThumbColor: AppColors.primary,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 20),

                // Keamanan
                _SettingsGroup(
                  title: 'Keamanan',
                  children: [
                    _SettingsTile(
                      icon: Icons.fingerprint_rounded,
                      iconColor: AppColors.secondary,
                      title: 'Biometrik',
                      subtitle: 'Sidik jari / wajah',
                      onTap: () => _toggleBiometric(!_biometricEnabled),
                      trailing: Switch(
                        value: _biometricEnabled,
                        onChanged: (v) => _toggleBiometric(v),
                        activeThumbColor: AppColors.primary,
                      ),
                    ),
                    _SettingsTile(
                      icon: Icons.lock_outline_rounded,
                      iconColor: AppColors.tertiary,
                      title: 'Ubah Password',
                      onTap: () {},
                      trailing: const Icon(Icons.chevron_right_rounded,
                          color: AppColors.outlineVariant),
                    ),
                  ],
                ),
                const SizedBox(height: 20),

                // Data
                _SettingsGroup(
                  title: 'Data',
                  children: [
                    _SettingsTile(
                      icon: Icons.cloud_download_outlined,
                      iconColor: AppColors.income,
                      title: 'Ekspor Data',
                      subtitle: 'PDF, CSV, atau Excel',
                      onTap: () {},
                      trailing: const Icon(Icons.chevron_right_rounded,
                          color: AppColors.outlineVariant),
                    ),
                    _SettingsTile(
                      icon: Icons.backup_outlined,
                      iconColor: AppColors.primary,
                      title: 'Cadangan Data',
                      subtitle: 'Simpan ke cloud',
                      onTap: () {},
                      trailing: const Icon(Icons.chevron_right_rounded,
                          color: AppColors.outlineVariant),
                    ),
                  ],
                ),
                const SizedBox(height: 20),

                // Tentang
                _SettingsGroup(
                  title: 'Tentang',
                  children: [
                    _SettingsTile(
                      icon: Icons.info_outline_rounded,
                      iconColor: AppColors.onSurfaceVariant,
                      title: 'Versi Aplikasi',
                      subtitle: '1.0.0',
                      onTap: () {},
                    ),
                    _SettingsTile(
                      icon: Icons.star_outline_rounded,
                      iconColor: AppColors.foodAccent,
                      title: 'Beri Rating',
                      onTap: () {},
                      trailing: const Icon(Icons.chevron_right_rounded,
                          color: AppColors.outlineVariant),
                    ),
                  ],
                ),
                const SizedBox(height: 20),

                // Danger zone
                _SettingsGroup(
                  title: 'Zona Bahaya',
                  children: [
                    _SettingsTile(
                      icon: Icons.logout_rounded,
                      iconColor: AppColors.expense,
                      title: 'Keluar',
                      titleColor: AppColors.expense,
                      onTap: () async {
                        await ref.read(authRepositoryProvider).signOut();
                        if (context.mounted) context.go('/login');
                      },
                    ),
                    _SettingsTile(
                      icon: Icons.delete_forever_rounded,
                      iconColor: AppColors.error,
                      title: 'Hapus Akun',
                      titleColor: AppColors.error,
                      onTap: () {},
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

// ─── Sub-widgets ──────────────────────────────────────────────────────────────

class _SettingsGroup extends StatelessWidget {
  final String title;
  final List<Widget> children;

  const _SettingsGroup({required this.title, required this.children});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(left: 4, bottom: 8),
          child: Text(title,
              style: AppTypography.metaData.copyWith(
                  color: AppColors.onSurfaceVariant,
                  fontWeight: FontWeight.w600,
                  letterSpacing: 0.5)),
        ),
        Container(
          decoration: BoxDecoration(
            color: AppColors.surfaceContainerLowest,
            borderRadius: BorderRadius.circular(16),
            border: Border.all(
                color: AppColors.outlineVariant, width: 0.5),
          ),
          child: Column(
            children: List.generate(children.length * 2 - 1, (i) {
              if (i.isOdd) {
                return const Divider(
                    height: 1, indent: 60, endIndent: 16);
              }
              return children[i ~/ 2];
            }),
          ),
        ),
      ],
    );
  }
}

class _SettingsTile extends StatelessWidget {
  final IconData icon;
  final Color iconColor;
  final String title;
  final String? subtitle;
  final Color? titleColor;
  final VoidCallback onTap;
  final Widget? trailing;

  const _SettingsTile({
    required this.icon,
    required this.iconColor,
    required this.title,
    this.subtitle,
    this.titleColor,
    required this.onTap,
    this.trailing,
  });

  @override
  Widget build(BuildContext context) {
    return ListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      leading: Container(
        width: 38, height: 38,
        decoration: BoxDecoration(
          color: iconColor.withValues(alpha: 0.12),
          borderRadius: BorderRadius.circular(10),
        ),
        child: Icon(icon, color: iconColor, size: 20),
      ),
      title: Text(title,
          style: AppTypography.bodyMain.copyWith(
              fontWeight: FontWeight.w500,
              color: titleColor ?? AppColors.onSurface)),
      subtitle: subtitle != null
          ? Text(subtitle!,
              style: AppTypography.metaData
                  .copyWith(color: AppColors.onSurfaceVariant))
          : null,
      trailing: trailing,
      onTap: onTap,
    );
  }
}
