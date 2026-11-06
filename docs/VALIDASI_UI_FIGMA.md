# Validasi UI Figma — 29 Oktober 2026

Pembaruan ini memakai file Figma `jPPKsuxOU84DScKQsh08FP` sebagai acuan Compose Multiplatform. Pengguna mengizinkan build dan pengujian langsung di perangkat pada pekerjaan ini.

| Alur | Frame acuan |
|---|---|
| Empat onboarding | `3:25`, `3:52`, `3:79`, `3:106` |
| Splash, welcome, masuk, daftar | `117:347`, `117:354`, `171:310`, `160:27` |
| Beranda dan navigasi pil | `15:4` |
| Pengeluaran, pemasukan, transfer | `86:6`, `86:157`, `86:280` |
| Detail transaksi | `149:116` |
| Dompet dan form | `92:6`, `92:108` |
| Statistik | `80:2` |
| Anggaran dan form | `27:7`, `27:129` |
| Kalender transaksi terjadwal | `27:187` |
| Target, detail, form | `49:8`, `49:96`, `49:173` |
| Profil | `149:2` |

Susunan visual disesuaikan dengan data dan fungsi yang tersedia. Pengguna memilih mempertahankan alur akun sekarang: nomor telepon, tanggal lahir, kode verifikasi email dan opsi akun baru tidak ditambahkan. Begitu pula pengulangan otomatis, histori setoran, proteksi target, ikon kustom dan fitur keuangan yang belum memiliki implementasi tidak ditampilkan sebagai fitur yang sudah berfungsi. Tagihan, utang, keinginan, kartu, catatan, reimburse dan Premium tetap menyampaikan status belum tersedia.

## Aset dan panel

- 83 gambar/ikon Figma yang dipakai tersimpan di `kotlinApp/composeApp/src/commonMain/composeResources/drawable/`. Ekspor yang tidak dipakai dibuang.
- SVG dirender menjadi PNG dengan resolusi 4× untuk Android/iOS; ukuran logis asli tercatat pada `FigmaAssets.kt`. Ilustrasi asli ditempatkan pada slot onboarding masing-masing. Tidak ada screenshot frame yang dipakai sebagai UI.
- Inter disimpan bersama lisensinya; Poppins dan DM Mono dipertahankan untuk frame yang memakainya.
- Panel transaksi memakai lebar layar dan menempel ke bawah. Form dapat digulir, keypad tetap terlihat, tanggal/dompet dapat dipilih, dan validasi penyimpanan tetap dipakai.
- Editor bersama, dompet, anggaran dan target menggunakan halaman penuh. Tampilan OS memakai status bar dan navigasi Android asli.

## Hasil pemeriksaan

Catatan ini merekam tahap UI Figma awal. Tahap berikutnya telah menambahkan menu keuangan dan navigasi liquid glass; status placeholder dan jumlah pengujian di bawah bersifat historis. Lihat [validasi menu dan liquid glass](VALIDASI_MENU_DAN_LIQUID_GLASS.md) untuk hasil terbaru.

Perangkat: Xiaomi `2311DRK48G`, serial `H6K79LIRINPVWWWS`, ukuran 1220 × 2712, density 480.

Build `:composeApp:assembleDebug`, `:composeApp:assembleDebugAndroidTest` dan `:composeApp:testDebugUnitTest` berhasil. APK aplikasi dan APK pengujian dibangun serta dipasang berpasangan.

9 unit test berhasil: `FinanceImportTest` 4, `NotificationTransactionParserTest` 3, `BudgetAlertsTest` 2.

6 pengujian perangkat berhasil melalui `FinFlowDesignDeviceTest`:

1. Empat slide onboarding, ilustrasi dan tombol selesai.
2. Welcome → masuk → daftar, dengan alur akun yang dipertahankan.
3. Navigasi Beranda/Statistik/Dompet/Profil dan tombol tambah.
4. Panel pengeluaran/pemasukan/transfer, keypad di bawah, error judul kosong, input nominal dan callback simpan lokal, serta batal/kembali.
5. Editor halaman penuh dan validasi anggaran.
6. Ringkasan anggaran, target/detail, kalender dan detail transaksi menggunakan nilai dari fixture.

Uji nominal `123` menghasilkan `amountMinor = 12300` untuk IDR. Callback pengujian tidak menulis Firestore. Screenshot 23 keadaan UI disimpan lokal di `kotlinApp/build/figma-reference/device-qa/`; log build dan pengujian berada di folder induknya. Screenshot panel juga diambil dari aplikasi terpasang dengan akun pengguna tanpa memasukkan atau menyimpan transaksi.

## Batas validasi

UI perangkat telah dibandingkan secara visual dengan screenshot Figma; ini bukan sertifikasi kesamaan setiap piksel pada seluruh ukuran layar/skala font. Pembandingan Android/iOS, build iOS, login/provider Firebase, penyimpanan transaksi nyata, izin/notifikasi sistem, keamanan rules dan koneksi bank tidak dibuktikan oleh uji UI ini.

Screenshot diizinkan pada build debug untuk pemeriksaan UI. Perlindungan `FLAG_SECURE` tetap diterapkan pada build rilis.
