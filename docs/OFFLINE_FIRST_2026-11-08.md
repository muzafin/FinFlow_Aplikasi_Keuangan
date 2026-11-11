# Offline-first transaksi Kasiva (mode cloud) — 8 November 2026

Pertanyaan pengguna: apakah aplikasi sudah offline-first — bisa menulis transaksi tanpa internet lalu memperbarui data saat online?

## Sebelum perbaikan

- **Mode Lokal**: bisa menulis transaksi tanpa internet, tetapi data hanya di perangkat dan **tidak otomatis** sinkron ke cloud; sinkron hanya lewat fitur manual "Sinkronkan Data Lokal" (akun tujuan harus kosong).
- **Mode Cloud/akun**: belum offline-first.
  - Saat aplikasi dibuka, `initializeProfile()` memakai Firestore transaction; semua simpan/edit/hapus juga transaction. Firestore transaction membutuhkan koneksi.
  - Akibatnya saat offline aplikasi menampilkan "Data akun gagal dimuat" (`UNAVAILABLE: Unable to resolve host ...`) dan transaksi tidak bisa disimpan.
  - Firestore Android sebenarnya menyimpan cache offline default, tetapi aplikasi belum memanfaatkannya karena jalur transaksi di atas.

## Perubahan

- `core/platform/NativeServices.kt` + `androidMain/MainActivity.kt`: `isOnline()` memakai ConnectivityManager (default true untuk platform lain).
- **Baru** `core/data/PendingTransactions.kt`:
  - `PendingTransactionStore` menyimpan antrean per akun di perangkat (`pending.transactions.<uid>`) sebagai JSON: simpan/edit/hapus transaksi.
  - `FinanceData.withPendingTransactions()` menggabungkan antrean ke data yang tampil (transaksi baru muncul, yang dihapus disembunyikan).
- `core/data/FinanceRepository.kt`:
  - `saveTransaction`/`deleteTransaction`: saat offline (atau gagal jaringan) transaksi masuk antrean, tidak error; saat online tetap memakai transaction Firestore seperti biasa.
  - `flushPendingTransactions()`: mengirim ulang antrean berurutan lewat transaksi server; jika validasi gagal (mis. dompet sudah diarsipkan di perangkat lain) item dilepas dan alasannya dilaporkan.
  - `initializeProfile()` tidak lagi menggagalkan pembukaan aplikasi saat offline (data dibaca dari cache Firestore).
- `app/App.kt`:
  - Data layar = data Firestore + antrean offline.
  - Antrean berjalan **senyap**: tidak ada banner/log di atas layar (permintaan pengguna 8 November); transaksi yang tertunda tetap tampil normal dan dikirim otomatis.
  - Pengiriman ulang otomatis setiap 4 detik selama ada antrean dan koneksi tersedia.
  - Sinkronisasi preferensi tidak lagi memunculkan dialog saat offline; dicoba ulang saat koneksi kembali.
- `moko-resources`: string `app_offline_mode` dan `app_pending_sync` (base + en; locale lain memakai fallback).

## Bukti uji di HP (8 November 2026)

1. WiFi + data dimatikan → aplikasi dibuka ulang: Beranda tampil dari cache, tidak ada layar error.
2. Tambah transaksi Rp5.000 saat offline → tersimpan dan saldo & pengeluaran Beranda ikut berubah (tanpa banner/log).
3. Aplikasi di-force-stop lalu dibuka lagi (masih offline) → transaksi dan antrean tetap ada.
4. WiFi dinyalakan → dalam ±15 detik antrean terkirim: transaksi muncul di Firestore (dicek lewat REST).
5. Banner "Mode offline"/"N transaksi menunggu sinkron" kemudian dihapus dari UI atas permintaan pengguna; antrean tetap berjalan.
5. Transaksi uji dihapus kembali dari Firestore; data pengguna tidak berubah.

Build APK debug sukses (`:composeApp:assembleDebug`).

## Batas saat ini

- Yang diantrekan hanya **transaksi keuangan** (simpan, edit, hapus). Dompet, kategori, anggaran, target, tagihan, reimburse, impor, restore cadangan, dan fitur AI masih memerlukan internet.
- Antrean disimpan per akun per perangkat; belum ada sinkronisasi dua arah penuh. Konflik antarperangkat diselesaikan saat pengiriman (transaksi divalidasi ulang di server); jika gagal, pengguna diberi tahu dan item antrean dilepas.
- Data offline berasal dari cache Firestore (default Android); pada instalasi baru yang belum pernah membuka data, cache bisa kosong.
- Layar yang hanya membaca data lain (dompet/anggaran) memakai data cache terakhir sampai koneksi kembali.
