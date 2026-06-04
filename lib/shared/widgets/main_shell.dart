import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_typography.dart';

/// Bottom navigation items
class _NavItem {
  final String label;
  final IconData icon;
  final IconData activeIcon;
  final String path;

  const _NavItem({
    required this.label,
    required this.icon,
    required this.activeIcon,
    required this.path,
  });
}

const _navItems = [
  _NavItem(
    label: 'Beranda',
    icon: Icons.home_outlined,
    activeIcon: Icons.home_rounded,
    path: '/home',
  ),
  _NavItem(
    label: 'Statistik',
    icon: Icons.bar_chart_rounded,
    activeIcon: Icons.bar_chart_rounded,
    path: '/statistics',
  ),
  _NavItem(
    label: 'Dompet',
    icon: Icons.account_balance_wallet_outlined,
    activeIcon: Icons.account_balance_wallet_rounded,
    path: '/wallet',
  ),
  _NavItem(
    label: 'Profil',
    icon: Icons.person_outline_rounded,
    activeIcon: Icons.person_rounded,
    path: '/profile',
  ),
];

/// Shell utama dengan bottom navigation bar bergaya Stitch
class MainShell extends StatelessWidget {
  final Widget child;
  const MainShell({super.key, required this.child});

  int _currentIndex(BuildContext context) {
    final loc = GoRouterState.of(context).uri.path;
    for (var i = 0; i < _navItems.length; i++) {
      if (loc.startsWith(_navItems[i].path)) return i;
    }
    return 0;
  }

  @override
  Widget build(BuildContext context) {
    final currentIndex = _currentIndex(context);

    return Scaffold(
      backgroundColor: AppColors.bgLight,
      body: child,
      extendBody: true,
      bottomNavigationBar: _FinFlowBottomNav(
        currentIndex: currentIndex,
        onTap: (i) => context.go(_navItems[i].path),
        onFabTap: () => context.push('/transaction/add'),
      ),
    );
  }
}

class _FinFlowBottomNav extends StatelessWidget {
  final int currentIndex;
  final ValueChanged<int> onTap;
  final VoidCallback onFabTap;

  const _FinFlowBottomNav({
    required this.currentIndex,
    required this.onTap,
    required this.onFabTap,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: AppColors.surfaceContainerLowest,
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
        boxShadow: [
          BoxShadow(
            color: Color(0x0D000000),
            blurRadius: 20,
            offset: Offset(0, -4),
          ),
        ],
      ),
      child: SafeArea(
        top: false,
        child: SizedBox(
          height: 72,
          child: Stack(
            clipBehavior: Clip.none,
            children: [
              // Nav items (2 kiri + 2 kanan, tengah kosong untuk FAB)
              Row(
                children: [
                  // Left items (0, 1)
                  ..._navItems.take(2).toList().asMap().entries.map((e) {
                    return Expanded(
                      child: _NavTile(
                        item: e.value,
                        isActive: currentIndex == e.key,
                        onTap: () => onTap(e.key),
                      ),
                    );
                  }),

                  // Center spacer for FAB
                  const SizedBox(width: 72),

                  // Right items (2, 3)
                  ..._navItems.skip(2).toList().asMap().entries.map((e) {
                    final idx = e.key + 2;
                    return Expanded(
                      child: _NavTile(
                        item: e.value,
                        isActive: currentIndex == idx,
                        onTap: () => onTap(idx),
                      ),
                    );
                  }),
                ],
              ),

              // Center FAB
              Positioned(
                top: -24,
                left: 0,
                right: 0,
                child: Center(
                  child: GestureDetector(
                    onTap: onFabTap,
                    child: Container(
                      width: 60,
                      height: 60,
                      decoration: BoxDecoration(
                        color: AppColors.navFab,
                        shape: BoxShape.circle,
                        boxShadow: [
                          BoxShadow(
                            color: AppColors.navFab.withValues(alpha: 0.35),
                            blurRadius: 16,
                            offset: const Offset(0, 4),
                          ),
                        ],
                      ),
                      child: const Icon(
                        Icons.add_rounded,
                        color: Colors.white,
                        size: 30,
                      ),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _NavTile extends StatelessWidget {
  final _NavItem item;
  final bool isActive;
  final VoidCallback onTap;

  const _NavTile({
    required this.item,
    required this.isActive,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      behavior: HitTestBehavior.opaque,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        padding: const EdgeInsets.symmetric(vertical: 8),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            AnimatedSwitcher(
              duration: const Duration(milliseconds: 200),
              child: Icon(
                isActive ? item.activeIcon : item.icon,
                key: ValueKey(isActive),
                color: isActive
                    ? AppColors.primary
                    : AppColors.onSurfaceVariant,
                size: 26,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              item.label,
              style: AppTypography.badgeLabel.copyWith(
                color: isActive
                    ? AppColors.primary
                    : AppColors.onSurfaceVariant,
                fontWeight: isActive ? FontWeight.w600 : FontWeight.w500,
                fontSize: 10,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
