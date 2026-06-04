import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:fpdart/fpdart.dart';
import 'package:google_sign_in/google_sign_in.dart';
import '../../../core/constants/app_constants.dart';
import '../../../core/services/firebase_service.dart';
import '../../../shared/models/account_model.dart';
import '../../../shared/models/category_model.dart';
import '../../../shared/models/user_model.dart';

/// Failure sealed class untuk error handling
sealed class AuthFailure {
  final String message;
  const AuthFailure(this.message);
}

class NetworkFailure extends AuthFailure {
  const NetworkFailure() : super('Tidak ada koneksi internet');
}

class InvalidCredentialsFailure extends AuthFailure {
  const InvalidCredentialsFailure() : super('Email atau password salah');
}

class EmailAlreadyInUseFailure extends AuthFailure {
  const EmailAlreadyInUseFailure() : super('Email sudah terdaftar');
}

class WeakPasswordFailure extends AuthFailure {
  const WeakPasswordFailure() : super('Password terlalu lemah');
}

class UserNotFoundFailure extends AuthFailure {
  const UserNotFoundFailure() : super('Akun tidak ditemukan');
}

class UnknownFailure extends AuthFailure {
  const UnknownFailure(super.message);
}

/// Repository untuk semua operasi autentikasi Firebase
class AuthRepository {
  final _auth = FirebaseService.auth;
  final _firestore = FirebaseService.firestore;
  final _googleSignIn = GoogleSignIn();

  // ─── Sign In with Google ──────────────────────────────────────────────────
  Future<Either<AuthFailure, UserModel>> signInWithGoogle() async {
    try {
      final googleUser = await _googleSignIn.signIn();
      if (googleUser == null) {
        return left(const UnknownFailure('Login Google dibatalkan'));
      }
      final googleAuth = await googleUser.authentication;
      final credential = GoogleAuthProvider.credential(
        accessToken: googleAuth.accessToken,
        idToken: googleAuth.idToken,
      );
      final result = await _auth.signInWithCredential(credential);
      final user = result.user!;

      // Cek apakah user sudah ada di Firestore
      final doc = await FirebaseService.userDoc(user.uid).get();
      if (!doc.exists) {
        await _createUserDoc(user);
      }

      final userDoc = await FirebaseService.userDoc(user.uid).get();
      return right(UserModel.fromJson(userDoc.data()!));
    } on FirebaseAuthException catch (e) {
      return left(_mapFirebaseError(e));
    } catch (e) {
      return left(UnknownFailure(e.toString()));
    }
  }

  // ─── Sign In with Email ───────────────────────────────────────────────────
  Future<Either<AuthFailure, UserModel>> signInWithEmail({
    required String email,
    required String password,
  }) async {
    try {
      final result = await _auth.signInWithEmailAndPassword(
        email: email,
        password: password,
      );
      final userDoc = await FirebaseService.userDoc(result.user!.uid).get();
      return right(UserModel.fromJson(userDoc.data()!));
    } on FirebaseAuthException catch (e) {
      return left(_mapFirebaseError(e));
    } catch (e) {
      return left(UnknownFailure(e.toString()));
    }
  }

  // ─── Register ─────────────────────────────────────────────────────────────
  Future<Either<AuthFailure, UserModel>> register({
    required String name,
    required String email,
    required String password,
  }) async {
    try {
      final result = await _auth.createUserWithEmailAndPassword(
        email: email,
        password: password,
      );
      final user = result.user!;
      await user.updateDisplayName(name);
      await _createUserDoc(user, displayName: name);
      final userDoc = await FirebaseService.userDoc(user.uid).get();
      return right(UserModel.fromJson(userDoc.data()!));
    } on FirebaseAuthException catch (e) {
      return left(_mapFirebaseError(e));
    } catch (e) {
      return left(UnknownFailure(e.toString()));
    }
  }

  // ─── Sign Out ─────────────────────────────────────────────────────────────
  Future<void> signOut() async {
    await _googleSignIn.signOut();
    await _auth.signOut();
  }

  // ─── Reset Password ───────────────────────────────────────────────────────
  Future<Either<AuthFailure, void>> resetPassword(String email) async {
    try {
      await _auth.sendPasswordResetEmail(email: email);
      return right(null);
    } on FirebaseAuthException catch (e) {
      return left(_mapFirebaseError(e));
    }
  }

  // ─── Get Current User ─────────────────────────────────────────────────────
  Future<UserModel?> getCurrentUser() async {
    final user = _auth.currentUser;
    if (user == null) return null;
    final doc = await FirebaseService.userDoc(user.uid).get();
    if (!doc.exists) return null;
    return UserModel.fromJson(doc.data()!);
  }

  // ─── Private Helpers ──────────────────────────────────────────────────────

  Future<void> _createUserDoc(User user, {String? displayName}) async {
    final uid = user.uid;
    final now = DateTime.now();
    final batch = _firestore.batch();

    // User profile
    final userModel = UserModel(
      uid: uid,
      name: displayName ?? user.displayName ?? 'Pengguna',
      email: user.email ?? '',
      photoUrl: user.photoURL,
      createdAt: now,
    );
    batch.set(FirebaseService.userDoc(uid), userModel.toJson());

    // Default categories
    for (final cat in DefaultCategories.all) {
      batch.set(
        FirebaseService.userCollection(uid, AppConstants.categoriesCollection)
            .doc(cat.id),
        cat.toJson(),
      );
    }

    // Default account (Kas)
    final cashAccount = DefaultAccounts.cash;
    batch.set(
      FirebaseService.userCollection(uid, AppConstants.accountsCollection)
          .doc(cashAccount.id),
      cashAccount.toJson(),
    );

    await batch.commit();
  }

  AuthFailure _mapFirebaseError(FirebaseAuthException e) {
    switch (e.code) {
      case 'wrong-password':
      case 'invalid-credential':
        return const InvalidCredentialsFailure();
      case 'email-already-in-use':
        return const EmailAlreadyInUseFailure();
      case 'weak-password':
        return const WeakPasswordFailure();
      case 'user-not-found':
        return const UserNotFoundFailure();
      case 'network-request-failed':
        return const NetworkFailure();
      default:
        return UnknownFailure(e.message ?? 'Terjadi kesalahan');
    }
  }
}

// ─── Riverpod Provider ────────────────────────────────────────────────────────
final authRepositoryProvider = Provider<AuthRepository>(
  (ref) => AuthRepository(),
);
