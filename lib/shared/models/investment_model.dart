import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:equatable/equatable.dart';

enum InvestmentType {
  saham,
  reksaDana,
  crypto,
  emas,
  deposito,
  properti,
  p2pLending,
}

/// Titik harga historis investasi
class PricePoint extends Equatable {
  final DateTime date;
  final double value;

  const PricePoint({required this.date, required this.value});

  factory PricePoint.fromJson(Map<String, dynamic> json) => PricePoint(
        date: (json['date'] as Timestamp).toDate(),
        value: (json['value'] as num).toDouble(),
      );

  Map<String, dynamic> toJson() => {
        'date': Timestamp.fromDate(date),
        'value': value,
      };

  @override
  List<Object?> get props => [date, value];
}

/// Model portofolio investasi
class InvestmentModel extends Equatable {
  final String id;
  final String name;
  final InvestmentType type;
  final double initialAmount;
  final double currentValue;
  final DateTime purchaseDate;
  final String? platform;
  final String? notes;
  final List<PricePoint> priceHistory;
  final double dividends;
  final DateTime createdAt;
  final DateTime updatedAt;

  // Fields spesifik per tipe
  final double? quantity;      // lot (saham), koin (crypto), gram (emas)
  final double? buyPrice;      // harga beli per unit
  final double? currentPrice;  // harga pasar saat ini
  final String? tickerCode;    // kode saham/crypto
  final double? interestRate;  // bunga deposito (%)
  final DateTime? maturityDate; // jatuh tempo deposito

  const InvestmentModel({
    required this.id,
    required this.name,
    required this.type,
    required this.initialAmount,
    required this.currentValue,
    required this.purchaseDate,
    this.platform,
    this.notes,
    this.priceHistory = const [],
    this.dividends = 0,
    required this.createdAt,
    required this.updatedAt,
    this.quantity,
    this.buyPrice,
    this.currentPrice,
    this.tickerCode,
    this.interestRate,
    this.maturityDate,
  });

  // ─── Computed ─────────────────────────────────────────────────────────────
  double get profitLoss => (currentValue + dividends) - initialAmount;
  double get profitLossPercent =>
      initialAmount <= 0 ? 0 : (profitLoss / initialAmount * 100);
  bool get isProfit => profitLoss >= 0;

  String get typeLabel {
    switch (type) {
      case InvestmentType.saham: return 'Saham';
      case InvestmentType.reksaDana: return 'Reksa Dana';
      case InvestmentType.crypto: return 'Crypto';
      case InvestmentType.emas: return 'Emas';
      case InvestmentType.deposito: return 'Deposito';
      case InvestmentType.properti: return 'Properti';
      case InvestmentType.p2pLending: return 'P2P Lending';
    }
  }

  factory InvestmentModel.fromJson(Map<String, dynamic> json) =>
      InvestmentModel(
        id: json['id'] as String,
        name: json['name'] as String,
        type: InvestmentType.values.firstWhere(
          (e) => e.name == json['type'],
          orElse: () => InvestmentType.saham,
        ),
        initialAmount: (json['initialAmount'] as num).toDouble(),
        currentValue: (json['currentValue'] as num).toDouble(),
        purchaseDate: (json['purchaseDate'] as Timestamp).toDate(),
        platform: json['platform'] as String?,
        notes: json['notes'] as String?,
        priceHistory: (json['priceHistory'] as List? ?? [])
            .map((e) => PricePoint.fromJson(e as Map<String, dynamic>))
            .toList(),
        dividends: (json['dividends'] as num?)?.toDouble() ?? 0,
        createdAt: (json['createdAt'] as Timestamp).toDate(),
        updatedAt: (json['updatedAt'] as Timestamp).toDate(),
        quantity: (json['quantity'] as num?)?.toDouble(),
        buyPrice: (json['buyPrice'] as num?)?.toDouble(),
        currentPrice: (json['currentPrice'] as num?)?.toDouble(),
        tickerCode: json['tickerCode'] as String?,
        interestRate: (json['interestRate'] as num?)?.toDouble(),
        maturityDate: json['maturityDate'] != null
            ? (json['maturityDate'] as Timestamp).toDate()
            : null,
      );

  Map<String, dynamic> toJson() => {
        'id': id,
        'name': name,
        'type': type.name,
        'initialAmount': initialAmount,
        'currentValue': currentValue,
        'purchaseDate': Timestamp.fromDate(purchaseDate),
        'platform': platform,
        'notes': notes,
        'priceHistory': priceHistory.map((e) => e.toJson()).toList(),
        'dividends': dividends,
        'createdAt': Timestamp.fromDate(createdAt),
        'updatedAt': Timestamp.fromDate(updatedAt),
        'quantity': quantity,
        'buyPrice': buyPrice,
        'currentPrice': currentPrice,
        'tickerCode': tickerCode,
        'interestRate': interestRate,
        'maturityDate':
            maturityDate != null ? Timestamp.fromDate(maturityDate!) : null,
      };

  InvestmentModel copyWith({
    String? id, String? name, InvestmentType? type,
    double? initialAmount, double? currentValue, DateTime? purchaseDate,
    String? platform, String? notes, List<PricePoint>? priceHistory,
    double? dividends, DateTime? createdAt, DateTime? updatedAt,
    double? quantity, double? buyPrice, double? currentPrice,
    String? tickerCode, double? interestRate, DateTime? maturityDate,
  }) => InvestmentModel(
        id: id ?? this.id,
        name: name ?? this.name,
        type: type ?? this.type,
        initialAmount: initialAmount ?? this.initialAmount,
        currentValue: currentValue ?? this.currentValue,
        purchaseDate: purchaseDate ?? this.purchaseDate,
        platform: platform ?? this.platform,
        notes: notes ?? this.notes,
        priceHistory: priceHistory ?? this.priceHistory,
        dividends: dividends ?? this.dividends,
        createdAt: createdAt ?? this.createdAt,
        updatedAt: updatedAt ?? this.updatedAt,
        quantity: quantity ?? this.quantity,
        buyPrice: buyPrice ?? this.buyPrice,
        currentPrice: currentPrice ?? this.currentPrice,
        tickerCode: tickerCode ?? this.tickerCode,
        interestRate: interestRate ?? this.interestRate,
        maturityDate: maturityDate ?? this.maturityDate,
      );

  @override
  List<Object?> get props => [id, name, type, initialAmount, currentValue];
}
