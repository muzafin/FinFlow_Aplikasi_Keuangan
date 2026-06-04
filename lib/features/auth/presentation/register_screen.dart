import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_typography.dart';
import '../../../core/utils/validators.dart';
import '../data/auth_repository.dart';

/// Register screen — matching Stitch "Daftar dengan Email - FinFlow"
class RegisterScreen extends ConsumerStatefulWidget {
  final bool isLogin;
  const RegisterScreen({super.key, this.isLogin = false});

  @override
  ConsumerState<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends ConsumerState<RegisterScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nameCtrl = TextEditingController();
  final _emailCtrl = TextEditingController();
  final _passwordCtrl = TextEditingController();
  bool _obscurePassword = true;
  bool _isLoading = false;
  bool _isLogin = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _isLogin = widget.isLogin;
  }

  Future<void> _submit() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    setState(() { _isLoading = true; _error = null; });

    final repo = ref.read(authRepositoryProvider);
    final result = _isLogin
        ? await repo.signInWithEmail(
            email: _emailCtrl.text.trim(),
            password: _passwordCtrl.text,
          )
        : await repo.register(
            name: _nameCtrl.text.trim(),
            email: _emailCtrl.text.trim(),
            password: _passwordCtrl.text,
          );

    if (!mounted) return;
    result.fold(
      (f) => setState(() { _isLoading = false; _error = f.message; }),
      (u) { _isLoading = false; context.go('/home'); },
    );
  }

  @override
  void dispose() {
    _nameCtrl.dispose();
    _emailCtrl.dispose();
    _passwordCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bgLight,
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded),
          onPressed: () => context.pop(),
        ),
        title: Text(_isLogin ? 'Masuk' : 'Daftar',
            style: AppTypography.sectionTitle),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  _isLogin ? 'Selamat datang kembali!' : 'Buat akun baru',
                  style: AppTypography.appTitle,
                ),
                const SizedBox(height: 8),
                Text(
                  _isLogin
                      ? 'Masuk untuk melanjutkan ke FinFlow'
                      : 'Bergabung dan mulai kelola keuanganmu',
                  style: AppTypography.bodyMain
                      .copyWith(color: AppColors.onSurfaceVariant),
                ),
                const SizedBox(height: 32),

                if (_error != null) ...[
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(12),
                    margin: const EdgeInsets.only(bottom: 20),
                    decoration: BoxDecoration(
                      color: AppColors.errorContainer,
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Text(_error!,
                        style: AppTypography.bodyMain
                            .copyWith(color: AppColors.onErrorContainer)),
                  ),
                ],

                // Name field (only register)
                if (!_isLogin) ...[
                  TextFormField(
                    controller: _nameCtrl,
                    textCapitalization: TextCapitalization.words,
                    validator: AppValidators.name,
                    decoration: const InputDecoration(
                      labelText: 'Nama Lengkap',
                      hintText: 'Masukkan nama lengkap',
                      prefixIcon: Icon(Icons.person_outline_rounded),
                    ),
                  ),
                  const SizedBox(height: 16),
                ],

                // Email field
                TextFormField(
                  controller: _emailCtrl,
                  keyboardType: TextInputType.emailAddress,
                  autocorrect: false,
                  validator: AppValidators.email,
                  decoration: const InputDecoration(
                    labelText: 'Email',
                    hintText: 'nama@email.com',
                    prefixIcon: Icon(Icons.email_outlined),
                  ),
                ),
                const SizedBox(height: 16),

                // Password field
                TextFormField(
                  controller: _passwordCtrl,
                  obscureText: _obscurePassword,
                  validator: AppValidators.password,
                  decoration: InputDecoration(
                    labelText: 'Password',
                    hintText: 'Minimal 8 karakter',
                    prefixIcon: const Icon(Icons.lock_outline_rounded),
                    suffixIcon: IconButton(
                      icon: Icon(_obscurePassword
                          ? Icons.visibility_outlined
                          : Icons.visibility_off_outlined),
                      onPressed: () => setState(
                          () => _obscurePassword = !_obscurePassword),
                    ),
                  ),
                ),
                const SizedBox(height: 32),

                // Submit button
                SizedBox(
                  width: double.infinity,
                  height: 56,
                  child: ElevatedButton(
                    onPressed: _isLoading ? null : _submit,
                    child: _isLoading
                        ? const SizedBox(
                            width: 22, height: 22,
                            child: CircularProgressIndicator(
                                strokeWidth: 2, color: Colors.white),
                          )
                        : Text(_isLogin ? 'Masuk' : 'Daftar'),
                  ),
                ),
                const SizedBox(height: 24),

                // Toggle login/register
                Center(
                  child: GestureDetector(
                    onTap: () => setState(() {
                      _isLogin = !_isLogin;
                      _error = null;
                    }),
                    child: RichText(
                      text: TextSpan(
                        style: AppTypography.bodyMain
                            .copyWith(color: AppColors.onSurfaceVariant),
                        children: [
                          TextSpan(
                              text: _isLogin
                                  ? 'Belum punya akun? '
                                  : 'Sudah punya akun? '),
                          TextSpan(
                            text: _isLogin ? 'Daftar' : 'Masuk',
                            style: AppTypography.bodyMain.copyWith(
                              color: AppColors.primary,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),

                if (_isLogin) ...[
                  const SizedBox(height: 16),
                  Center(
                    child: TextButton(
                      onPressed: () {
                        // TODO: forgot password
                      },
                      child: Text(
                        'Lupa password?',
                        style: AppTypography.bodyMain
                            .copyWith(color: AppColors.primary),
                      ),
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}
