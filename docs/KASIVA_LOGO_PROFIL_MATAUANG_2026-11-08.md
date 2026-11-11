# Logo Kasiva, sheet tambah transaksi, Profil berkelompok, liquid glass (8 November 2026)

## Perubahan
- **Logo:** logo Kasiva baru tanpa latar (teknik color-to-alpha).
  - Ikon peluncur adaptif (latar putih, simbol K, ikon monokrom untuk themed icon), splash Android 12+, dan logo di aplikasi.
  - Halaman Tentang memakai logo lengkap di mode terang, simbol K di mode gelap.
- **Tombol + / tambah transaksi:**
  - Mengetuk "Rp" kini membuka **Pilih Mata Uang** gaya Ollo: pencarian, lencana "Bawaan", mata uang populer di atas, roda gigi ke Pengaturan.
  - Jumlah dalam mata uang lain dikonversi ke mata uang dompet dengan kurs harian (fungsi `exchangeRates`). Jumlah aslinya disimpan di `originalCurrency`/`originalAmountMinor` dan tampil di detail transaksi.
- **Tombol AI:** bar Asisten/AI Voice/AI Scan/AI Chat hanya muncul kalau Preferensi › Tampilan & Format › Tampilan Tambah Transaksi › *Perilaku tombol +* diset "Buka semua cara mencatat". Pengaturan di HP pemilik sudah dikembalikan ke mode itu.
- **Langganan:** dimatikan sementara (`PREMIUM_ENABLED = false`). Kartu dan tombol Premium disembunyikan dan batas gratis di aplikasi tidak berlaku. Kuota AI tetap dijaga server.
- **Palet Preferensi:** memilih palet menerapkan paket tema seperti Ollo, yaitu aksen, kartu saldo, dan warna ikon menu sekaligus.
- **Profil berkelompok:** Akun & Data dan Preferensi tetap di halaman utama. Menu lain dikelompokkan di "Menu Lainnya" (Data & Dompet, Tampilan & Format, AI, Fitur Masa Mendatang, Bantuan & Info), masing-masing di halaman sendiri. Pencarian tetap mencari semua menu.
- **Liquid glass:** bilah navigasi dan tombol + lebih bening (lapisan 14%/22%, blur 3dp), dengan pembiasan tepi + chromatic aberration, kilau tepi, dan bayangan.

## Backend
- Rules Firestore: transaksi boleh membawa `originalCurrency` + `originalAmountMinor` (keduanya diisi atau keduanya null, mata uang berbeda dari dompet, bukan transfer). Sudah di-deploy ke `finflow-logikawan`.

## Verifikasi
- Unit test lulus, uji UI di HP 52/52 (uji Profil disesuaikan dengan grup).
- Emulator: rules 21/21 (termasuk uji mata uang asing), Storage 4/4.
- Dicek manual di HP: pemilih mata uang + konversi USD→IDR, bar AI, halaman grup Profil, navigasi kaca.

## Investasi realtime (riset, belum dibangun)
- Grafik native dengan **Vico** (Compose) memakai data dari Firestore. Data diisi oleh Cloud Function terjadwal:
  - kripto dari Indodax atau CoinGecko,
  - emas dari metals API × kurs Frankfurter.
- Saham IDX/AS: belum ada sumber gratis yang jelas boleh dipakai di aplikasi komersial. Widget TradingView di WebView butuh konfirmasi izin aplikasi mobile dari TradingView.

## Lanjutan: kategori & chip sheet tambah transaksi seperti Ollo
- **Kategori bawaan = Ollo.**
  - Pengeluaran: Makan & minum, Transportasi, Belanja, Rumah, Hiburan, Kesehatan, Pendidikan, Pribadi, Keuangan, Keluarga, Tidak tercatat, Teman, Hewan peliharaan, plus Tagihan & Lainnya bawaan lama.
  - Pemasukan: Gaji, Usaha, Investasi, Hadiah, Lainnya.
  - Setiap kategori punya subkategori seperti Ollo (total 127 kategori), dengan ikon Hugeicons dan warna, dalam 9 bahasa.
  - Satu sumber data: `tools/i18n/gen_categories.py` membuat `core/model/DefaultCategoryData.kt`, string moko, dan daftar ikon.
- **Akun lama dilengkapi otomatis** saat aplikasi dibuka (`ensureDefaultCategories`). Hanya menambah kategori yang belum ada; kategori buatan pengguna tidak disentuh. Akun pemilik sudah terisi.
- **Chip di bawah subkategori (Pengeluaran):**
  - *Reimburse* membuka panel jumlah. Saat disimpan, pengeluaran dan klaimnya dibuat (muncul di menu Reimburse).
  - *Split Bill* menampilkan "belum tersedia", sama seperti Ollo.
  - *CC/Paylater* memilih dompet Kartu Kredit/Paylater, atau memberi tahu bila belum ada.
- Uji: rules 23/23 (termasuk seluruh pohon kategori dalam satu batch pendaftaran), uji UI di HP 52/52.
