# Status pekerjaan FinFlow — 29 Oktober 2026

Pembaruan 4 November 2026: Beranda, halaman Level/Aktivitas, kalender, likuiditas di kartu saldo, panel tiga titik, dan skala tipografi (Poppins judul 20/18, isi 14, keterangan 12; `FitMoneyText` anti-terpotong) disamakan dengan Ollo; lima menu Beranda (Anggaran, Berulang, Target, Tagihan, Utang) ditulis ulang mengikuti Ollo termasuk pemilih dompet berpencarian dan form masing-masing, dan Profil mendapat "Laporkan Bug" dengan informasi perangkat nyata (tanpa logcat mentah, saldo, transaksi, token). Build debug + APK instrumentasi berhasil; 55 unit test Kotlin dan 30 uji instrumentasi lulus di HP Xiaomi. Bukti di [screenshot 4 November](ui-evidence/2026-11-04/) dan rincian di [laporan paritas](PARITAS_OLLO_2026-11-04.md). Batas: leaderboard/Premium masih rencana (belum ada backend), beberapa field model Ollo (jam/custom icon/jumlah pengulangan pada Berulang; nominal per periode pada Anggaran) belum ada di model, dan iOS tidak dibangun di Windows.

Pembaruan 3 November 2026: tour onboarding Ollo empat slide dan wizard pengaturan awal enam langkah (welcome dengan nama/foto/pulihkan cadangan/Google, pilih bahasa, pilih mata uang, notifikasi pintar, pilih gaya, dompet pertama) sudah ditulis dan diuji pada HP Xiaomi. Langkah welcome kini mewajibkan nama lengkap dengan error inline ID/EN ("Nama lengkap wajib diisi." / "Full name is required.", testTag `setup-name-error`), dan dialog "Lihat Semua" mata uang diganti `OlloCurrencyDialog` netral berpencarian sehingga warna ungu bawaan Material3 tidak lagi muncul di alur onboarding/auth. Alur pertama kali kini splash → tour → wizard → mode lokal; login cloud tetap lewat halaman masuk. Preferensi tampilan mendapat preset gradien kartu saldo, tema warna ikon menu dan perilaku tombol +. Build debug dan APK pengujian berhasil, 50 unit test Kotlin lulus, 22 uji instrumentasi lulus di HP pada ronde 2; uji end-to-end nyata mode lokal pada data bersih juga dijalankan sampai Beranda menampilkan sapaan "~ Hai, Muhammad!" dan Total Saldo Rp1.000.000,00, dengan preferensi tetap tersimpan setelah aplikasi ditutup paksa dan dibuka ulang. Pada ronde 3 sore hari yang sama, Beranda disamakan dengan susunan Ollo: header dengan tombol profil dan tombol pintasan tiga titik, tab periode pil hitam, kartu "Total Saldo (KODE)" dengan ikon privasi dan swap urutan mata uang, dompet grid dua kolom, transaksi terbaru dikelompokkan per tanggal Indonesia, serta state kosong anggaran; dua uji `DashboardParityDeviceTest` ditambahkan sehingga seluruhnya 24 uji instrumentasi lulus di HP. Pada ronde 4 sore hari yang sama, halaman Statistik ditulis ulang mengikuti susunan Ollo: header dengan filter/info/menu tiga titik, tab periode, kartu donut total pengeluaran/pemasukan, ringkasan harian, perbandingan bulanan, wawasan siklus, peta aktivitas, pengeluaran terbesar, log transaksi, dan kartu Analisis Lanjutan dengan skor kesehatan finansial sederhana (bukan AI); dua uji `StatisticsParityDeviceTest` ditambahkan sehingga seluruhnya 26 uji instrumentasi lulus di HP. Pada ronde 5 sore hari yang sama, halaman Dompet dan Detail dompet disamakan dengan susunan Ollo: header dengan komposisi uang/menu tiga titik/mode arsip, kartu total saldo per mata uang dengan perubahan 30 hari, grid Total Kekayaan/Liabilitas/Tabungan Aktif/Pembayaran Mendatang, grup dompet dan empty state, kartu Target Tabungan, dialog Komposisi Uang, serta detail dompet dengan tab periode, grafik saldo, ringkasan, metrik dan Transaksi Terbaru; tiga uji `WalletParityDeviceTest` ditambahkan sehingga seluruhnya 29 uji instrumentasi lulus di HP. Pada ronde 6 sore hari yang sama, integrasi Firebase diverifikasi memakai emulator project demo `demo-finflow`: Firestore rules 17/17, storage rules 3/3, Cloud Functions hapus akun/reset data 2/2, dan 40 uji instrumentasi di HP lulus termasuk `CloudModeSmokeDeviceTest` baru yang membuat akun cloud lewat Auth emulator, menyimpan transaksi lewat Firestore emulator dan memastikan tampilan lokal ikut terbarui; rules/functions produksi tidak diubah dan tidak ada operasi destruktif pada data produksi, sementara verifikasi ini bukan uji produksi dan login Google nyata belum diotomasi. Pada ronde 7 sore hari yang sama, halaman Profil disamakan dengan susunan Ollo: header avatar 88dp dengan badge kamera 30dp, nama tanpa baris email, metrik LEVEL/STREAK/HARI AKTIF dengan divider dan chevron, section caption, tile grid dua kolom 172dp, row list ikon lingkaran 40dp, serta palette dan preferensi; bug ikon chevron/fallback juga diperbaiki sehingga tidak lagi menampilkan glyph generik. Instrumentasi di HP menjadi 42 test lulus yang dijalankan dalam dua proses: 31 test UI lokal termasuk dua test `ProfileParityDeviceTest` baru, dan 11 test Firebase cloud. Lokalisasi ID/EN baru mencakup wizard; formulir/menu keuangan lain, login/akun cloud dari langkah welcome, langkah perintah suara Ollo, uji manual pengguna/akun cloud nyata, iOS, Premium/AI dan paritas piksel belum diuji atau dikerjakan. Data uji dikosongkan lagi setelah pemeriksaan; data cloud pengguna tidak disentuh. [Validasi 3 November](VALIDASI_2026-11-03.md).

Pembaruan lanjutan 2 November: mode lokal dan perpindahan akun, koreksi dark mode, reset data tanpa hapus akun serta menu Masukan & Peta Jalan sudah ditambahkan. Backend reset/rules aktif di produksi. Peta jalan di aplikasi memuat 60 fitur dengan lingkup/status nyata. [Validasi dan batas terbaru](VALIDASI_LOKAL_DAN_PETA_JALAN_2026-11-02.md).

Pembaruan 2 November 2026: nama aplikasi kini **FinFlow**, error akun baru akibat rules lama diperbaiki dan diverifikasi pada HP. Storage privat dan dua Cloud Functions penghapusan akun sudah aktif. Lihat [hasil dan batas validasi terbaru](VALIDASI_2026-11-02.md).

## Sudah aktif pada Firebase Console

| Komponen | Status teramati |
|---|---|
| Pemilik project | `muzafinarifin.dev@gmail.com` — Owner |
| Project | `finflow-logikawan` / 644917550039 |
| Aplikasi | Android dan iOS `com.logikawan.finflow` |
| Authentication | Email/Password dan Google Enabled |
| Password policy | Require, minimal 8 karakter; tanpa pemaksaan upgrade saat login |
| Sertifikat | SHA-1 dan SHA-256 debug terdaftar |
| Firestore | `(default)`, Jakarta `asia-southeast2` |
| Rules | Versi aktif 29 Oktober 14:42 WIB; koleksi perencanaan dan pasangan jurnal sudah diterbitkan setelah pengujian emulator |
| Data | Sudah terdapat profil, akun dan kategori; tidak ditambahkan data percobaan |
| Billing | Blaze / pay as you go menurut konfirmasi pengguna 29 Oktober; tidak diubah oleh asisten |

Berkas Android memuat OAuth Android/Web. Plist iOS memuat reversed client ID dan `Config.xcconfig` lokal tersedia. API key tidak dicantumkan dalam dokumen ini.

## Implementasi Kotlin yang sudah ditulis

Android/iOS berbagi UI Compose; acuan visual terbaru memakai Figma FinFlow, dengan Inter, Poppins, DM Mono dan aset lokal. Kode dipisahkan menjadi `app`, `core` dan `feature`. Onboarding, autentikasi, transaksi/transfer, dompet, statistik, anggaran, target, investasi, impor/ekspor CSV, kunci perangkat dan pengingat harian sudah dihubungkan. [Validasi UI Android](VALIDASI_UI_FIGMA.md) mencatat build, 9 unit test dan 6 pengujian UI perangkat yang telah berhasil atas izin pengguna.

Nama dan mata uang utama profil kini dapat diubah dari nama pada header Profil atau baris Mata Uang. Profil dibaca sebagai stream dari Firestore, sehingga nama yang disimpan saat pendaftaran atau diperbarui dapat muncul tanpa menunggu login ulang. Mata uang utama hanya memprioritaskan urutan tampilan; tidak mengubah uang atau mata uang akun yang sudah ada.

Peringatan anggaran yang sebelumnya nonaktif kini memakai notifikasi lokal pada batas 80% dan 100%. Izin perangkat diperlukan. Pemeriksaan terjadi saat aplikasi dibuka atau data berubah, dengan penanda per pengguna/anggaran/bulan/batas pada masing-masing perangkat agar tidak berulang. Jumlah uang dan kategori tidak ditampilkan di notifikasi. Ini tidak memantau database dari server ketika aplikasi ditutup. Dua unit test aturan peringatan berhasil; pengiriman notifikasi sistem belum diuji dalam validasi UI ini.

## Pekerjaan tersisa dan kebutuhan konkretnya

Pembaruan menu dan liquid glass telah selesai diimplementasikan: Tagihan, Hutang/Piutang, Keinginan, Kartu, Catatan, Reimburse dan Berulang tersambung ke Firestore. Navigasi Profil menggunakan komponen yang sama dengan tiga tab lain. [Validasi terbaru](VALIDASI_MENU_DAN_LIQUID_GLASS.md): 15 unit test, 5 uji rules, 5 uji repository dari device memakai emulator dan 9 uji UI Xiaomi berhasil.

| Pekerjaan | Kondisi sekarang | Yang diperlukan |
|---|---|---|
| Build dan UI Android | Build, 15 unit test, 5 uji rules, 5 uji repository lewat emulator dan 9 uji UI Xiaomi berhasil; APK terpasang | Validasi iOS, perangkat lain, login dan pengujian pengguna terhadap data produksi masih terpisah |
| Bank/e-wallet otomatis lintas negara | Belum ada penyedia atau koneksi produksi | Akun/kontrak penyedia, kredensial API, cakupan bank, backend token/webhook, persetujuan pengguna; satu integrasi tidak menjamin seluruh institusi dunia |
| Foto profil/lampiran ke Firebase Storage | Foto profil sudah dihubungkan ke Storage privat; bucket/rules aktif di Jakarta | Unggah foto pengguna di produksi dan iOS belum diuji; lampiran transaksi belum ditambahkan |
| Hapus akun beserta seluruh data | Konfirmasi + autentikasi ulang dan dua Cloud Functions aktif; alur menyeluruh lulus di emulator | Tidak diuji dengan menghapus akun nyata di produksi |
| Push FCM dan peringatan anggaran server | Belum ada pengirim/backend | Backend tepercaya, token per perangkat dan aturan akses; konfigurasi APNs untuk iOS |
| Android rilis | Baru sertifikat debug yang terdaftar | Aplikasi Play Console, SHA App Signing/release dan proses signing milik developer |
| iOS rilis | Proyek host dan konfigurasi tersedia | Mac/Xcode serta Apple Developer Team/signing |
| App Check enforcement | Belum diaktifkan | Implementasi attestation dan konfigurasi platform yang benar sebelum enforcement |
| Menu perencanaan Beranda | Sudah memiliki halaman, formulir, penyimpanan dan histori pembayaran | Validasi iOS/perangkat lain dan pengujian pengguna pada data sendiri |
| Web/desktop Kotlin, harga pasar/kurs langsung | Belum diimplementasikan | Target tambahan dan integrasi API tersendiri; target migrasi saat ini Android/iOS |

Firestore memakai **collection/document**, bukan tabel SQL. Collection transaksi, anggaran, target dan investasi muncul setelah dokumen pertamanya disimpan. Schema dan rules sudah mendefinisikan field serta validasinya; tidak perlu mengisi database dengan dokumen dummy agar tampak lengkap. Lihat [schema](../kotlinApp/firebase/SCHEMA.md).

Memakai akun developer yang sama sudah dipenuhi melalui kepemilikan project. Penautan produk Google Play adalah konfigurasi tambahan untuk produk seperti analytics/distribusi; bukan syarat login dan Firestore.

Blaze sudah diverifikasi melalui API pada 2 November. Cloud Storage untuk foto profil dan Cloud Functions untuk penghapusan akun kini aktif; layanan bank/AI dan pengirim FCM/APNs tetap memerlukan implementasi/integrasi tersendiri.

## Bukti pemeriksaan konfigurasi

- [Password policy yang tersimpan](firebase-evidence/password-policy-2026-10-29.png)
- [Rules aktif](firebase-evidence/firestore-rules-2026-10-29.png)

Pemeriksaan Console dan pencocokan file konfigurasi tidak membuktikan seluruh integrasi aplikasi. [Validasi terbaru](VALIDASI_MENU_DAN_LIQUID_GLASS.md) mencatat uji penyimpanan memakai Firebase Emulator dan pemeriksaan baca halaman/form Tagihan pada aplikasi produksi; tidak ada transaksi percobaan yang disimpan di produksi.
