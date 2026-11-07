# Menu Profil, Tambah Dompet, logo bank/e-wallet, dan backend (7 November 2026)

Acuan: Ollo 1.7.9 di HP Xiaomi. Sekitar 30 sub-layar Profil dan layar "Add New Wallet" ditangkap (screenshot + teks) lalu dicocokkan.

## Tampilan
- **Profil:** urutan dan bagian sama dengan Ollo:
  - Akun & Data (ubin 2 kolom + baris), Preferensi (chip palet + ubin), AI, Fitur Mendatang, Umum, Lainnya.
  - Kolom cari pengaturan, footer versi.
  - Bahasa dan Keamanan dipindah ke halaman Pengaturan seperti di Ollo.
- **Pengaturan:**
  - Mata Uang, Mata Uang Transaksi Bawaan, Kurs, Bahasa, Notifikasi, Notifikasi Bank/E-Wallet, Keamanan, Widget (panduan saja).
  - Animasi: Ketuk, Transisi Halaman, Daftar.
- **Tambah/Ubah Dompet:**
  - Judul di tengah dengan tombol ✓ di kanan atas, chip tipe, baris mata uang, kartu "Kecualikan dari Total" dan "Tautkan sebagai Pocket".
  - Grid ikon umum dan **Unggah Ikon Kustom**.
  - Pemilih bank (20) / e-wallet (11) dengan **logo asli** (26 logo; ShopeePay, i.saku, AstraPay, Sakuku, DOKU memakai badge warna). Sumber dan lisensi logo ada di `kotlinApp/licenses/ProviderLogos.md`.
- **Halaman lain:**
  - Akun & Data: Manajemen Akun, Cadangan & Pemulihan, Perangkat Terhubung, Ekspor Data, Impor, Bantuan, Tentang, Catatan Pembaruan, Hapus Data.
  - Preferensi (13 halaman), Kategori (tab Pengeluaran/Pemasukan/Sistem), Pengaturan Dompet, Notifikasi + riwayat.
  - Pengaturan Asisten, AI Scan, Bahasa Input Suara, Otomasi (Segera Hadir), Masukan & Peta Jalan.

## Pengaturan yang kini benar-benar dipakai
- Karakter sensor saldo dan pertahankan format, warna transfer.
- Dompet yang tampil di Beranda, tata letak dan urutan pemilih dompet, urutan "Dompet Saya", urutan kategori.
- Mata uang transaksi bawaan, pemilih tanggal model roda, Quick Fill (saran judul).
- Animasi ketuk dan transisi halaman, bahasa input suara.
- Bug diperbaiki: di Susunan Menu, menyembunyikan menu lalu memindah urutan membuat menu muncul lagi (closure lama).

## Backend (Firebase `finflow-logikawan`, sudah di-deploy)
- **AI:**
  - Pindah ke **DeepSeek** (`functions/deepseek.js`, model `deepseek-chat`). Kunci disimpan di Secret Manager `DEEPSEEK_API_KEY`.
  - Asisten menjawab dalam 9 bahasa aplikasi.
  - AI Scan (foto) belum didukung DeepSeek: pengguna mendapat pesan jelas dan kuota dikembalikan.
- **`exchangeRates`:** kurs referensi ECB lewat Frankfurter, di-cache 12 jam di `_rates/{base}`.
- **`deleteTransactions`:** "Hapus Transaksi Saja". Menghapus transaksi dan kejadian keuangan, lalu mengembalikan status tagihan/utang/target/bundle agar data tetap valid. Mode lokal ditangani di perangkat.
- **Koleksi `users/{uid}/devices`:** daftar Perangkat Terhubung; perangkat yang dicabut keluar sendiri.
- **Storage `users/{uid}/wallet-icons/{id}.jpg`:** ikon dompet kustom, JPEG ≤ 100 KB, hanya pemilik.
- **Riwayat notifikasi:** pengingat dan peringatan anggaran yang tampil dicatat di perangkat.
- **Secret RevenueCat:** berisi nilai pengganti acak supaya fungsi lain bisa di-deploy. Ganti dengan kunci asli sebelum Premium dipakai.

## Verifikasi
- Unit test 60/60; uji UI di HP 52/52.
- Emulator: rules Firestore 20/20, Storage 4/4, Cloud Functions 31/31.
- `exchangeRates` diuji langsung di produksi. Layar Profil, Pengaturan, Kurs, Tambah Dompet, dan grid logo dicek di HP.

## Belum
- AI Scan perlu penyedia AI yang bisa membaca gambar.
- Premium perlu kunci RevenueCat asli.
- Mencabut perangkat belum mencabut token login di server (perangkat yang offline tetap masuk sampai dibuka lagi).
- Animasi daftar transaksi belum diterapkan.
- Ikon kustom belum ikut cadangan JSON.
- iOS belum diuji.
