import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:hive_flutter/hive_flutter.dart';
import '../../../core/constants/app_constants.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_typography.dart';

/// Data slide onboarding
class _OnboardingData {
  final String title;
  final String subtitle;
  final IconData icon;
  final Color color;
  final Color bgColor;

  const _OnboardingData({
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.color,
    required this.bgColor,
  });
}

const _slides = [
  _OnboardingData(
    title: 'Rekam Cepat',
    subtitle: 'Catat pemasukan dan pengeluaran dengan mudah. Cukup beberapa ketukan!',
    icon: Icons.receipt_long_rounded,
    color: AppColors.primary,
    bgColor: AppColors.surfaceContainerLow,
  ),
  _OnboardingData(
    title: 'Analisis Statistik',
    subtitle: 'Lihat grafik dan laporan keuanganmu. Ketahui ke mana uangmu pergi.',
    icon: Icons.bar_chart_rounded,
    color: AppColors.secondary,
    bgColor: Color(0xFFECF0FF),
  ),
  _OnboardingData(
    title: 'Kontrol Total',
    subtitle: 'Buat anggaran, pantau investasi, dan raih tujuan keuanganmu bersama FinFlow.',
    icon: Icons.savings_rounded,
    color: AppColors.income,
    bgColor: AppColors.surfaceContainerLow,
  ),
];

/// Onboarding screen 3 slide dengan page indicator
class OnboardingScreen extends StatefulWidget {
  const OnboardingScreen({super.key});

  @override
  State<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends State<OnboardingScreen> {
  final _pageController = PageController();
  int _currentPage = 0;

  void _next() {
    if (_currentPage < _slides.length - 1) {
      _pageController.nextPage(
        duration: const Duration(milliseconds: 350),
        curve: Curves.easeInOut,
      );
    } else {
      _finish();
    }
  }

  Future<void> _finish() async {
    final box = await Hive.openBox(AppConstants.settingsBox);
    await box.put(AppConstants.onboardingSeenKey, true);
    if (mounted) context.go('/login');
  }

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bgLight,
      body: SafeArea(
        child: Column(
          children: [
            // Skip button
            Align(
              alignment: Alignment.topRight,
              child: TextButton(
                onPressed: _finish,
                child: Text('Lewati',
                    style: AppTypography.bodyMain
                        .copyWith(color: AppColors.onSurfaceVariant)),
              ),
            ),

            // Pages
            Expanded(
              child: PageView.builder(
                controller: _pageController,
                onPageChanged: (i) => setState(() => _currentPage = i),
                itemCount: _slides.length,
                itemBuilder: (context, i) => _OnboardingPage(data: _slides[i]),
              ),
            ),

            // Indicator + Button
            Padding(
              padding: const EdgeInsets.fromLTRB(24, 0, 24, 32),
              child: Column(
                children: [
                  // Dots
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: List.generate(
                      _slides.length,
                      (i) => AnimatedContainer(
                        duration: const Duration(milliseconds: 300),
                        margin: const EdgeInsets.symmetric(horizontal: 4),
                        width: i == _currentPage ? 24 : 8,
                        height: 8,
                        decoration: BoxDecoration(
                          color: i == _currentPage
                              ? AppColors.primary
                              : AppColors.outlineVariant,
                          borderRadius: BorderRadius.circular(4),
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(height: 32),

                  // CTA Button
                  SizedBox(
                    width: double.infinity,
                    height: 56,
                    child: ElevatedButton(
                      onPressed: _next,
                      child: Text(
                        _currentPage == _slides.length - 1
                            ? 'Mulai Sekarang'
                            : 'Lanjut',
                        style: AppTypography.bodyMain.copyWith(
                          color: Colors.white,
                          fontWeight: FontWeight.w600,
                          fontSize: 16,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _OnboardingPage extends StatelessWidget {
  final _OnboardingData data;
  const _OnboardingPage({required this.data});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 32),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          // Illustration
          Container(
            width: 180,
            height: 180,
            decoration: BoxDecoration(
              color: data.bgColor,
              shape: BoxShape.circle,
            ),
            child: Center(
              child: Container(
                width: 100,
                height: 100,
                decoration: BoxDecoration(
                  color: data.color.withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(28),
                ),
                child: Icon(data.icon, color: data.color, size: 52),
              ),
            ),
          ),
          const SizedBox(height: 48),

          Text(
            data.title,
            style: AppTypography.appTitle.copyWith(fontSize: 28),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 16),
          Text(
            data.subtitle,
            style: AppTypography.bodyMain.copyWith(
              color: AppColors.onSurfaceVariant,
              height: 1.6,
            ),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }
}
