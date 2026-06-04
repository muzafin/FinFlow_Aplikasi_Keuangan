import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:equatable/equatable.dart';

/// Model data user FinFlow
class UserModel extends Equatable {
  final String uid;
  final String name;
  final String email;
  final String? photoUrl;
  final String currency;
  final String currencySymbol;
  final double? monthlyIncome;
  final int level;
  final DateTime createdAt;

  const UserModel({
    required this.uid,
    required this.name,
    required this.email,
    this.photoUrl,
    this.currency = 'IDR',
    this.currencySymbol = 'Rp',
    this.monthlyIncome,
    this.level = 1,
    required this.createdAt,
  });

  factory UserModel.fromJson(Map<String, dynamic> json) => UserModel(
        uid: json['uid'] as String,
        name: json['name'] as String? ?? '',
        email: json['email'] as String? ?? '',
        photoUrl: json['photoUrl'] as String?,
        currency: json['currency'] as String? ?? 'IDR',
        currencySymbol: json['currencySymbol'] as String? ?? 'Rp',
        monthlyIncome: (json['monthlyIncome'] as num?)?.toDouble(),
        level: json['level'] as int? ?? 1,
        createdAt: json['createdAt'] is Timestamp
            ? (json['createdAt'] as Timestamp).toDate()
            : DateTime.now(),
      );

  Map<String, dynamic> toJson() => {
        'uid': uid,
        'name': name,
        'email': email,
        'photoUrl': photoUrl,
        'currency': currency,
        'currencySymbol': currencySymbol,
        'monthlyIncome': monthlyIncome,
        'level': level,
        'createdAt': Timestamp.fromDate(createdAt),
      };

  UserModel copyWith({
    String? uid,
    String? name,
    String? email,
    String? photoUrl,
    String? currency,
    String? currencySymbol,
    double? monthlyIncome,
    int? level,
    DateTime? createdAt,
  }) =>
      UserModel(
        uid: uid ?? this.uid,
        name: name ?? this.name,
        email: email ?? this.email,
        photoUrl: photoUrl ?? this.photoUrl,
        currency: currency ?? this.currency,
        currencySymbol: currencySymbol ?? this.currencySymbol,
        monthlyIncome: monthlyIncome ?? this.monthlyIncome,
        level: level ?? this.level,
        createdAt: createdAt ?? this.createdAt,
      );

  @override
  List<Object?> get props => [uid, name, email, photoUrl, currency,
        currencySymbol, monthlyIncome, level, createdAt];
}
