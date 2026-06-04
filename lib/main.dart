import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:hive_flutter/hive_flutter.dart';
import 'package:finflow/core/services/firebase_service.dart';
import 'package:finflow/core/theme/app_theme.dart';
import 'package:finflow/core/router/app_router.dart';
import 'package:intl/date_symbol_data_local.dart';

/// Entry point for the FinFlow application.
///
/// Initializes system UI, Firebase, Hive local storage, and then
/// launches the app wrapped in a [ProviderScope] for Riverpod.
void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // ─── System UI Configuration ───────────────────────────────────
  SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
    statusBarColor: Colors.transparent,
    statusBarIconBrightness: Brightness.dark,
  ));
  await SystemChrome.setPreferredOrientations([
    DeviceOrientation.portraitUp,
    DeviceOrientation.portraitDown,
  ]);

  // ─── Service Initialization ────────────────────────────────────
  await FirebaseService.initialize();
  await Hive.initFlutter();
  await initializeDateFormatting('id_ID', null);

  runApp(const ProviderScope(child: FinFlowApp()));
}

/// Root widget for the FinFlow application.
///
/// Uses [MaterialApp.router] with [GoRouter] for declarative navigation,
/// and watches the [routerProvider] from Riverpod for the router config.
class FinFlowApp extends ConsumerWidget {
  const FinFlowApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final router = ref.watch(routerProvider);

    return MaterialApp.router(
      title: 'FinFlow',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.lightTheme,
      darkTheme: AppTheme.darkTheme,
      themeMode: ThemeMode.light,
      routerConfig: router,
    );
  }
}
