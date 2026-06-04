import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../features/auth/presentation/splash_screen.dart';
import '../../features/auth/presentation/onboarding_screen.dart';
import '../../features/auth/presentation/login_screen.dart';
import '../../features/auth/presentation/register_screen.dart';
import '../../features/dashboard/presentation/dashboard_screen.dart';
import '../../features/transaction/presentation/add_transaction_screen.dart';
import '../../features/transaction/presentation/transaction_list_screen.dart';
import '../../features/report/presentation/report_screen.dart';
import '../../features/budget/presentation/add_budget_screen.dart';
import '../../features/investment/presentation/add_investment_screen.dart';
import '../../features/account/presentation/account_list_screen.dart';
import '../../features/settings/presentation/profile_screen.dart';
import '../../features/settings/presentation/notification_settings_screen.dart';
import '../../features/investment/presentation/investment_detail_screen.dart';
import '../../shared/models/investment_model.dart';
import '../../shared/models/transaction_model.dart';
import '../../shared/models/budget_model.dart';
import '../../shared/widgets/main_shell.dart';

class AppRoutes {
  static const splash = '/';
  static const onboarding = '/onboarding';
  static const login = '/login';
  static const register = '/register';
  static const home = '/home';
  static const statistics = '/statistics';
  static const wallet = '/wallet';
  static const profile = '/profile';
  static const addTransaction = '/transaction/add';
  static const transactions = '/transactions';
  static const addBudget = '/budget/add';
  static const addInvestment = '/investment/add';
  static const investmentDetail = '/investment/detail';
  static const notificationSettings = '/settings/notifications';
}

final routerProvider = Provider<GoRouter>((ref) {
  return GoRouter(
    initialLocation: AppRoutes.splash,
    debugLogDiagnostics: false,
    routes: [
      GoRoute(
        path: AppRoutes.splash,
        builder: (c, s) => const SplashScreen(),
      ),
      GoRoute(
        path: AppRoutes.onboarding,
        pageBuilder: (c, s) => _fadeRoute(const OnboardingScreen()),
      ),
      GoRoute(
        path: AppRoutes.login,
        pageBuilder: (c, s) => _fadeRoute(const LoginScreen()),
      ),
      GoRoute(
        path: AppRoutes.register,
        pageBuilder: (c, s) {
          final isLogin = s.uri.queryParameters['mode'] == 'login';
          return _slideRoute(RegisterScreen(isLogin: isLogin));
        },
      ),
      GoRoute(
        path: AppRoutes.addTransaction,
        pageBuilder: (c, s) {
          final tx = s.extra as TransactionModel?;
          return _bottomSheetRoute(AddTransactionScreen(existingTx: tx));
        },
      ),
      GoRoute(
        path: AppRoutes.transactions,
        pageBuilder: (c, s) => _slideRoute(const TransactionListScreen()),
      ),

      GoRoute(
        path: AppRoutes.addBudget,
        pageBuilder: (c, s) {
          final budget = s.extra as BudgetModel?;
          return _bottomSheetRoute(AddBudgetScreen(existingBudget: budget));
        },
      ),

      GoRoute(
        path: AppRoutes.addInvestment,
        pageBuilder: (c, s) => _slideRoute(const AddInvestmentScreen()),
      ),

      GoRoute(
        path: AppRoutes.notificationSettings,
        pageBuilder: (c, s) => _slideRoute(const NotificationSettingsScreen()),
      ),

      GoRoute(
        path: AppRoutes.investmentDetail,
        pageBuilder: (c, s) => _slideRoute(InvestmentDetailScreen(investment: s.extra as InvestmentModel)),
      ),

      // Shell: tabbed navigation
      ShellRoute(
        builder: (c, s, child) => MainShell(child: child),
        routes: [
          GoRoute(
            path: AppRoutes.home,
            pageBuilder: (c, s) =>
                const NoTransitionPage(child: DashboardScreen()),
          ),
          GoRoute(
            path: AppRoutes.statistics,
            pageBuilder: (c, s) =>
                const NoTransitionPage(child: ReportScreen()),
          ),
          GoRoute(
            path: AppRoutes.wallet,
            pageBuilder: (c, s) =>
                const NoTransitionPage(child: AccountListScreen()),
          ),
          GoRoute(
            path: AppRoutes.profile,
            pageBuilder: (c, s) =>
                const NoTransitionPage(child: ProfileScreen()),
          ),
        ],
      ),
    ],
  );
});

// ─── Page Transition Helpers ──────────────────────────────────────────────────

CustomTransitionPage<void> _fadeRoute(Widget child) =>
    CustomTransitionPage<void>(
      child: child,
      transitionsBuilder: (_, anim, __, c) =>
          FadeTransition(opacity: anim, child: c),
    );

CustomTransitionPage<void> _slideRoute(Widget child) =>
    CustomTransitionPage<void>(
      child: child,
      transitionsBuilder: (_, anim, __, c) => SlideTransition(
        position: Tween<Offset>(
          begin: const Offset(1, 0),
          end: Offset.zero,
        ).animate(CurvedAnimation(parent: anim, curve: Curves.easeOutCubic)),
        child: c,
      ),
    );

CustomTransitionPage<void> _bottomSheetRoute(Widget child) =>
    CustomTransitionPage<void>(
      child: child,
      opaque: false,
      barrierColor: Colors.black54,
      barrierDismissible: true,
      transitionsBuilder: (_, anim, __, c) => SlideTransition(
        position: Tween<Offset>(
          begin: const Offset(0, 1),
          end: Offset.zero,
        ).animate(CurvedAnimation(parent: anim, curve: Curves.easeOutCubic)),
        child: c,
      ),
    );
