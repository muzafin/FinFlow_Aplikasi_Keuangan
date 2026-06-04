import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../shared/models/user_model.dart';
import '../data/auth_repository.dart';

/// State autentikasi
sealed class AuthState {
  const AuthState();
}

class AuthInitial extends AuthState {
  const AuthInitial();
}

class AuthLoading extends AuthState {
  const AuthLoading();
}

class AuthAuthenticated extends AuthState {
  final UserModel user;
  const AuthAuthenticated(this.user);
}

class AuthUnauthenticated extends AuthState {
  const AuthUnauthenticated();
}

class AuthError extends AuthState {
  final String message;
  const AuthError(this.message);
}

/// Notifier untuk state autentikasi
class AuthNotifier extends Notifier<AuthState> {
  @override
  AuthState build() {
    // Cek user yang sedang login saat startup
    _checkCurrentUser();
    return const AuthInitial();
  }

  AuthRepository get _repo => ref.read(authRepositoryProvider);

  Future<void> _checkCurrentUser() async {
    final user = await _repo.getCurrentUser();
    if (user != null) {
      state = AuthAuthenticated(user);
    } else {
      state = const AuthUnauthenticated();
    }
  }

  Future<void> signInWithGoogle() async {
    state = const AuthLoading();
    final result = await _repo.signInWithGoogle();
    result.fold(
      (failure) => state = AuthError(failure.message),
      (user) => state = AuthAuthenticated(user),
    );
  }

  Future<void> signInWithEmail(String email, String password) async {
    state = const AuthLoading();
    final result = await _repo.signInWithEmail(
        email: email, password: password);
    result.fold(
      (failure) => state = AuthError(failure.message),
      (user) => state = AuthAuthenticated(user),
    );
  }

  Future<void> register(
      String name, String email, String password) async {
    state = const AuthLoading();
    final result = await _repo.register(
        name: name, email: email, password: password);
    result.fold(
      (failure) => state = AuthError(failure.message),
      (user) => state = AuthAuthenticated(user),
    );
  }

  Future<void> signOut() async {
    await _repo.signOut();
    state = const AuthUnauthenticated();
  }

  /// Clear error state
  void clearError() {
    state = const AuthUnauthenticated();
  }
}

// ─── Provider ─────────────────────────────────────────────────────────────────
final authNotifierProvider =
    NotifierProvider<AuthNotifier, AuthState>(() => AuthNotifier());

/// Convenience: current authenticated user (null if not logged in)
final currentUserProvider = Provider<UserModel?>((ref) {
  final authState = ref.watch(authNotifierProvider);
  if (authState is AuthAuthenticated) return authState.user;
  return null;
});
