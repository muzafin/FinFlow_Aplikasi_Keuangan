# FinFlow - Aplikasi Manajemen Keuangan Pribadi 💸

FinFlow adalah aplikasi manajemen keuangan pribadi modern yang dibangun menggunakan **Flutter** dan **Firebase**. Aplikasi ini dirancang untuk memudahkan Anda dalam melacak pemasukan, pengeluaran, memantau anggaran bulanan, serta menganalisis portofolio investasi dengan tampilan antarmuka *glassmorphism* yang elegan dan kekinian.

## 🌟 Fitur Utama

- **Autentikasi Aman & Cerdas**: Mendukung Login/Register menggunakan Email dan Google. Terintegrasi dengan keamanan biometrik perangkat (Sidik Jari / Face ID) dan PIN (*local_auth*) sebagai lapis keamanan ganda untuk menjaga privasi finansial Anda.
- **Dashboard Dinamis & Informatif**: Menampilkan ringkasan total saldo, mutasi dompet/rekening, serta akses cepat ke daftar riwayat transaksi terbaru.
- **Catat Transaksi Real-time**: Menggunakan *bottom sheet* dengan *custom numpad* yang mempermudah pencatatan transaksi masuk maupun keluar kapan pun.
- **Manajemen Anggaran (Budgeting)**: Atur batas pengeluaran bulanan per kategori dan pantau dengan indikator batang visual (progress bar).
- **Statistik & Laporan**: Lihat distribusi pengeluaran atau pemasukan Anda secara detail melalui grafik donat interaktif (menggunakan *fl_chart*).
- **Pengingat Harian Otomatis (Custom Notifications)**: Atur jam pengingat harian secara spesifik melalui *TimePicker* bawaan agar Anda tidak pernah lupa mencatat keuangan harian Anda.
- **Dompet Cerdas**: Saldo selalu sinkron (_real-time_) ketika ada penambahan, pembaruan, atau penghapusan transaksi (CRUD lengkap).

## 🛠 Teknologi & Arsitektur

Proyek ini dibangun berdasarkan *best-practices* Flutter modern:

- **Framework**: Flutter & Dart
- **State Management**: [Riverpod v2](https://riverpod.dev/) (menggunakan `riverpod_generator` & `riverpod_annotation`)
- **Routing**: [go_router](https://pub.dev/packages/go_router)
- **Database & Backend**: Firebase Authentication & Cloud Firestore
- **Penyimpanan Lokal**: [Hive](https://pub.dev/packages/hive_flutter) (untuk *settings*, biometrik, preferensi pengguna)
- **Keamanan**: `local_auth` (Biometrik, PIN, Pattern)
- **Notifikasi**: `flutter_local_notifications`, `timezone`, `firebase_messaging`
- **UI & Grafis**: `fl_chart`, `google_fonts`

## 🚀 Panduan Instalasi (Getting Started)

### Prasyarat
Pastikan Anda sudah menginstal:
- Flutter SDK (versi >= 3.19.0)
- Android Studio / VS Code
- Akun Firebase (untuk menghubungkan Google-services.json jika Anda mem-build ulang Firebase-nya).

### Menjalankan Proyek

1. **Clone Repositori**
   ```bash
   git clone https://github.com/muzafin/FinFlow_Aplikasi_Keuangan.git
   cd FinFlow_Aplikasi_Keuangan
   ```

2. **Unduh Dependensi (Packages)**
   ```bash
   flutter pub get
   ```

3. **Jalankan Aplikasi**
   Pastikan emulator atau perangkat fisik Anda terhubung, lalu jalankan:
   ```bash
   flutter run
   ```
   > **Penting**: Karena aplikasi ini menggunakan integrasi *Native* untuk notifikasi jadwal lokal (`flutter_local_notifications` dan `timezone`) serta modul keamanan (`local_auth`), sangat disarankan untuk me-*restart* total (menggunakan `flutter run` atau `flutter build apk`) dari awal jika Anda baru menambahkan *package* baru. *Hot restart* terkadang gagal memuat sensor *native*.

## 📱 Tangkapan Layar (Screenshots)
*(Anda dapat menambahkan screenshot aplikasi di folder `/assets` dan menautkannya ke bagian ini nanti)*

---

*Dibuat oleh [Muzafin](https://github.com/muzafin) & Tim Pengembangan.*
