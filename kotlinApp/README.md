# Kasiva

**Detail Ollo, 3 November 2026:** kalender harian Beranda, mode saldo likuid, pintasan tiga titik, halaman level/XP, nominal panjang dan nama, pemilih dompet bersama pada formulir serta laporan bug dengan informasi perangkat sudah disesuaikan. Leaderboard global/per negara bulanan dan hadiah Premium 3/2/1 bulan masih rencana layanan admin; progres klien belum boleh dipakai untuk membagikan hadiah. [Cakupan, bukti dan batas penyesuaian](../docs/DETAIL_OLLO_2026-11-03.md).

Versi Kotlin Android/iOS memakai package dan bundle ID `com.clonical.kasiva`. Sumber Flutter di direktori induk dipertahankan sebagai referensi migrasi. Ini schema Firebase baru, tidak kompatibel langsung dengan dokumen Flutter lama.

**Pembaruan 3 November 2026:** tour onboarding Ollo empat slide (ilustrasi Figma, badge, titik halaman, tombol pill) dan wizard pengaturan awal enam langkah (welcome dengan nama/foto/pulihkan cadangan/Google, pilih bahasa ID/EN, pilih mata uang, notifikasi pintar, pilih gaya, dompet pertama) sudah ditulis. Pada ronde 2 sore hari yang sama, langkah welcome divalidasi mewajibkan nama lengkap dengan error inline ID/EN (testTag `setup-name-error`), dan dialog "Lihat Semua" mata uang diganti `OlloCurrencyDialog` berupa sheet putih netral berpencarian sehingga warna ungu bawaan Material3 tidak muncul di alur onboarding/auth. Alur pertama kali kini splash → tour → wizard → mode lokal; login cloud tetap lewat halaman masuk. Preferensi tampilan baru: preset gradien kartu saldo, tema warna ikon menu, dan perilaku tombol +. Build debug dan APK pengujian berhasil, 50 unit test Kotlin lulus dan 22 uji instrumentasi lulus di HP Xiaomi pada ronde 2. Uji end-to-end nyata mode lokal pada data bersih juga dijalankan di HP: isi nama "Muhammad", pilih bahasa ID, pilih IDR, gaya Gelap, dompet Cash Rp1.000.000 sampai Beranda menampilkan Total Saldo Rp1.000.000,00 dan preferensi tetap tersimpan setelah aplikasi dibuka ulang; data uji kemudian dikosongkan lagi. Pada ronde 3 sore hari yang sama, Beranda disamakan dengan susunan Ollo: header dengan tombol profil dan tombol pintasan tiga titik, tab periode pil hitam, kartu "Total Saldo (KODE)" dengan ikon privasi dan swap urutan mata uang, dompet grid dua kolom, transaksi terbaru dikelompokkan per tanggal Indonesia, dan state kosong anggaran; 24 uji instrumentasi lulus di HP Xiaomi setelah dua uji `DashboardParityDeviceTest` ditambahkan, dengan tangkapan layar di `docs/ui-evidence/2026-11-03-ronde3/`. Pada ronde 4 sore hari yang sama, halaman Statistik ditulis ulang mengikuti Ollo: header dengan filter/info/menu tiga titik, tab periode, kartu donut total pengeluaran/pemasukan, ringkasan harian, perbandingan bulanan, wawasan siklus, peta aktivitas, pengeluaran terbesar, log transaksi, dan kartu Analisis Lanjutan dengan skor kesehatan finansial sederhana (bukan AI); 26 uji instrumentasi lulus di HP Xiaomi setelah dua uji `StatisticsParityDeviceTest` ditambahkan, dengan tangkapan layar di `docs/ui-evidence/2026-11-03-ronde4/`. Pada ronde 5 sore hari yang sama, halaman Dompet dan Detail dompet disamakan dengan Ollo: header komposisi uang/menu tiga titik/mode arsip, kartu total saldo per mata uang dengan perubahan 30 hari, grid Total Kekayaan/Liabilitas/Tabungan Aktif/Pembayaran Mendatang, grup dompet, Target Tabungan, dialog Komposisi Uang, serta detail dompet (tab periode, grafik saldo, ringkasan, metrik, aksi, Transaksi Terbaru); 29 uji instrumentasi lulus di HP Xiaomi setelah tiga uji `WalletParityDeviceTest` ditambahkan, dengan tangkapan layar di `docs/ui-evidence/2026-11-03-ronde5/`. Pada ronde 6 sore hari yang sama, integrasi Firebase diverifikasi memakai emulator project demo `demo-finflow`: Firestore rules 17/17, storage rules 3/3, Cloud Functions hapus akun/reset data 2/2, dan 40 uji instrumentasi di HP lulus termasuk `CloudModeSmokeDeviceTest` baru (akun cloud Auth emulator, transaksi tersimpan di Firestore emulator dan lolos rules); rules/functions produksi tidak diubah. Pada ronde 7 sore hari yang sama, halaman Profil disamakan dengan Ollo (header avatar + badge kamera, nama tanpa email, metrik LEVEL/STREAK/HARI AKTIF, section caption, tile grid dua kolom, row list) dan bug ikon chevron/fallback diperbaiki sehingga tidak lagi menampilkan glyph generik; 42 uji instrumentasi lulus di HP dalam dua proses (31 UI lokal + 11 Firebase cloud), termasuk dua uji `ProfileParityDeviceTest` baru, dengan tangkapan layar di `docs/ui-evidence/2026-11-03-ronde7/`. [Validasi 3 November](../docs/VALIDASI_2026-11-03.md). Batas: terjemahan EN baru mencakup wizard, langkah perintah suara Ollo belum ada, login/akun cloud dari langkah welcome dan iOS belum diuji, serta paritas piksel masih berjalan.

**Pembaruan 29 Oktober 2026:** menu Tagihan, Hutang/Piutang, Keinginan, Kartu, Catatan, Reimburse dan Berulang sudah dihubungkan ke Firestore. Empat tab utama memakai satu navigasi liquid glass Kyant0. Build Android, 15 unit test, 5 uji rules, 5 uji repository lewat emulator dari device dan 9 uji UI Xiaomi berhasil. Lihat [cakupan terbaru](../docs/VALIDASI_MENU_DAN_LIQUID_GLASS.md) serta [validasi Figma sebelumnya](../docs/VALIDASI_UI_FIGMA.md). iOS belum dibangun atau diuji.

## Struktur

```text
composeApp/src/commonMain/kotlin/com/clonical/kasiva/
  app/                     Titik masuk dan koordinasi sesi/halaman
    navigation/            Empat tab dan pemilihan editor
  core/
    model/                 Model akun, transaksi, anggaran, target, investasi
    data/                  Repository Firebase Auth dan Firestore
    platform/              Kontrak layanan perangkat dan lifecycle
    util/                  Hitungan uang integer dan format tanggal/nominal
    ui/                    Warna, tipografi, ikon, komponen bersama
  feature/
    auth/                  Onboarding, login, pendaftaran
    dashboard/             Beranda
    account/               Daftar dan editor dompet
    transaction/           Riwayat dan editor transaksi
    report/                Statistik dan grafik
    budget/                Anggaran
    goal/                  Target keuangan
    investment/            Portofolio, detail, editor investasi
    settings/              Profil, notifikasi, kategori, pengaturan data
    importdata/            Parser, pratinjau, impor CSV
    connections/           Status koneksi bank dan akses impor
    planning/              Tagihan, hutang, keinginan, kartu, catatan, reimburse, berulang
composeApp/src/commonMain/composeResources/
  font/                    Inter, Poppins, DM Mono, Material Icons
  drawable/                Logo, ilustrasi dan ikon lokal dari Figma
composeApp/src/androidMain/ Android, Credential Manager, biometrik, file, pengingat
composeApp/src/iosMain/     Pintu masuk Compose UIViewController
composeApp/src/commonTest/ Pengujian uang, impor, notifikasi dan batas anggaran
composeApp/src/androidInstrumentedTest/ Pengujian UI dengan data terpisah
iosApp/                    Host SwiftUI, layanan native iOS, XcodeGen, CocoaPods
firebase/                  Rules, indeks, schema, dan status setup project baru
gradle/                    Gradle wrapper resmi
licenses/                  Lisensi font dan ikon
```

## Fitur yang ditulis

- Login email, pendaftaran, Google, dan pemulihan kata sandi.
- Nama dan mata uang utama profil dapat diubah melalui nama profil atau baris Mata Uang; perubahan dibaca langsung dari Firestore.
- Dompet tunai/bank/e-wallet/investasi/pinjaman; saldo awal dan arsip.
- Tambah, ubah, hapus transaksi; transfer termasuk dua mata uang; transaksi terjadwal.
- Pencarian dan filter pemasukan/pengeluaran/transfer/periode.
- Ringkasan per mata uang, laporan kategori, grafik, dan ekspor CSV.
- Anggaran bulanan, target tabungan, dan investasi dengan valuasi manual.
- Tagihan dan hutang/piutang beserta histori pembayaran, klaim reimburse, keinginan, identitas kartu empat digit, catatan serta jadwal berulang. Pembayaran terhubung ke jurnal dompet secara atomik.
- Impor CSV Ivy Wallet dan template Kasiva: pratinjau, validasi, deduplikasi ID, dan pengulangan setelah kegagalan parsial.
- Kunci perangkat serta pengingat harian native.
- Peringatan anggaran lokal pada batas 80% dan 100%; sekali per akun pengguna, anggaran, bulan dan batas pada perangkat tersebut. Perhitungan memakai pengeluaran yang sudah terjadi dengan kategori/mata uang yang sama.

Koneksi bank otomatis masih memerlukan kontrak penyedia, backend, persetujuan pengguna, dan cakupan institusi. Halaman koneksi menerangkan status tersebut secara eksplisit. Tidak ada bank yang ditampilkan sudah tersambung.

## Android di Windows

1. Buka **folder `kotlinApp`** di Android Studio sebagai project terpisah dari Flutter.
2. Gunakan **Gradle JDK yang kompatibel** pada Settings → Build, Execution, Deployment → Build Tools → Gradle. Konfigurasi Studio saat diperiksa memakai JBR 21; target bytecode aplikasi tetap Java 17. Android SDK 37 dan `local.properties` dengan `sdk.dir` yang sesuai diperlukan; target SDK aplikasi tetap 36. Hindari menyalin `local.properties` Flutter karena dapat memuat path lain yang tidak dibutuhkan.
3. File `composeApp/google-services.json` harus berasal dari project `finflow-logikawan`, aplikasi `com.clonical.kasiva`. File ini disimpan lokal dan diabaikan Git.
4. Klik **Sync Now** pada banner perubahan Gradle. Tunggu proses sinkronisasi dan indexing selesai; tombol Play dapat nonaktif selama proses ini. Bila sync gagal, buka hasil Gradle Sync untuk melihat error pertama.
5. Pilih konfigurasi **composeApp**, pilih perangkat, lalu klik **Play**. Project Gradle bernama `Kasiva` dan launcher mengikuti manifest utama. Jika IDE masih menampilkan nama modul lama setelah perubahan nama project, lakukan Gradle Sync kembali.
6. Bila ingin menghasilkan APK sendiri: `./gradlew.bat :composeApp:assembleDebug` dari folder ini.

Perintah build tersebut telah berhasil dijalankan untuk pembaruan UI Figma. Sebelum rilis, tambahkan signing release milik developer; konfigurasi ini sengaja tidak menandatangani rilis memakai debug key.

Google Sign-In membutuhkan provider Google aktif, OAuth client yang benar dalam file JSON terbaru, SHA-1/SHA-256 debug yang sesuai, dan SHA sertifikat Play App Signing untuk rilis. Mengaktifkan provider tidak membuktikan login pada perangkat sudah berhasil.

## Acuan tampilan Figma

UI kini mengambil acuan langsung dari [Figma Kasiva](https://www.figma.com/design/jPPKsuxOU84DScKQsh08FP/FinFlow?node-id=0-1). Empat slide onboarding memakai ilustrasi asli, diikuti splash, welcome, masuk, dan pendaftaran. Beranda memakai kartu gradien teal/biru, grid menu, ringkasan anggaran/dompet, navigasi pil dan tombol tambah di kanan. Profil mengikuti frame Poppins tersendiri. Inter, Poppins, DM Mono serta 83 aset Figma disimpan lokal; aplikasi tidak mengambil aset dari URL sementara Figma saat berjalan.

Panel transaksi menempel ke bawah, dengan keypad tetap di luar area scroll. Form dompet, anggaran, target dan editor bersama menggunakan halaman penuh. Statistik mencakup ringkasan harian, perbandingan bulanan, peta aktivitas, kategori dan log dari data asli. Kalender transaksi terjadwal manual tersedia dari menu Berulang; jadwal otomatis kini memiliki definisi, interval dan pemrosesan tersendiri. Detail transaksi dan detail impian dapat dibuka dari daftar masing-masing.

Onboarding, autentikasi, beranda, dompet, profil, riwayat transaksi, statistik, anggaran, target, investasi, dan pengaturan notifikasi sudah dipindahkan. Fitur tambahan KMP seperti impor CSV, mata uang berbeda, arsip dan transaksi terjadwal tetap tersedia. Pilihan Flutter yang sebelumnya belum berfungsi menampilkan status belum tersedia. Sinkronisasi bank otomatis belum diaktifkan.

Peringatan anggaran bisa diaktifkan melalui Profil → Notifikasi. Pemeriksaan dilakukan saat data berubah di aplikasi atau halaman dibuka kembali; fitur ini memakai notifikasi lokal Android/iOS. Ini belum merupakan layanan pemantauan anggaran di server saat aplikasi ditutup. Nominal dan kategori tidak dicantumkan dalam pesan notifikasi.

Screenshot perangkat telah ditinjau terhadap screenshot frame Figma. Status bar dan navigasi sistem memakai Android asli. Kesamaan setiap piksel pada semua perangkat/skala font, efek blur/bayangan lintas platform, dan iOS belum dibuktikan. Lisensi font/ikon tersedia pada `licenses/`.

## iOS di Mac

1. iOS sudah didaftarkan pada project Firebase yang sama, bundle ID `com.clonical.kasiva`.
2. File resmi `GoogleService-Info.plist` sudah disimpan pada `iosApp/` secara lokal. File ini diabaikan Git; sediakan ulang dari Firebase Console jika memindahkan project ke Mac melalui Git.
3. Siapkan Xcode, JDK yang kompatibel dengan Gradle (konfigurasi daemon saat ini meminta JBR 21), CocoaPods dan XcodeGen pada Mac. Target bytecode aplikasi tetap Java 17.
4. Dari folder `kotlinApp/`, jalankan `chmod +x gradlew`. Dari `iosApp/`, jalankan `sh configure-ios.sh`, kemudian `xcodegen generate`.
5. Dari folder `kotlinApp/`, jalankan `./gradlew :composeApp:podspec`.
6. Dari `iosApp/`, jalankan `pod install`, lalu buka `Kasiva.xcworkspace` dan pilih signing team sendiri.

Jalankan tahapan itu sendiri saat siap melakukan build. Windows tidak dapat membangun/menjalankan iOS melalui Xcode. `Config.xcconfig` dibuat dari plist resmi; reversed client ID tidak ditebak atau diambil dari project lama.

## Firebase

Lihat `firebase/SETUP_STATUS.md` dan [validasi 2 November](../docs/VALIDASI_2026-11-02.md) untuk status cloud. Firestore, bucket foto profil privat dan Functions penghapusan akun sudah aktif di Jakarta. Uji penyimpanan/penghapusan memakai Firebase Emulator. Indeks otomatis bawaan mencukupi query yang dipakai saat ini.

Pemeriksaan Console pada 29 Oktober 2026 mengonfirmasi akun developer sebagai Owner, provider login aktif, serta rules produksi cocok dengan sumber lokal. Password policy server sekarang mewajibkan minimal 8 karakter untuk pendaftaran, tanpa force upgrade saat login. Lihat juga `../docs/STATUS_PEKERJAAN.md` untuk daftar pekerjaan tersisa.

Saat deployment CLI diperlukan, gunakan akun `muzafinarifin.dev@gmail.com`, jalankan dari folder `firebase/`, dan sebutkan project secara eksplisit:

```powershell
firebase deploy --only firestore --project finflow-logikawan
```

Jangan deploy rules schema v2 ini ke `finflow-c38e8`. Root `firebase.json` milik Flutter lama tidak diubah.

## Impor

Gunakan CSV UTF-8, maksimal 5 MB/10.000 baris per file. Untuk Ivy, pilih **Export to CSV**, bukan Backup Data ZIP/JSON. Jumlah kolom dan ID transaksi wajib valid; satu kesalahan baris menonaktifkan konfirmasi impor. Transfer memerlukan kedua dompet dan nominal diterima. Tanggal tanpa timezone mengikuti perangkat saat impor.

Impor bukan satu transaksi database raksasa: dompet/kategori dan setiap jurnal disimpan secara bertahap. Bila terputus, data yang sudah disimpan tetap ada, jumlah yang tersisa terlihat dari pratinjau ulang, dan impor ulang dengan sumber serta ID yang sama tidak menggandakan jurnal. Jangan mengganti label sumber ketika mengimpor ulang berkas yang sama.

Ekspor Kasiva menambahkan kolom `Kasiva ID`, `Source`, dan `Source ID` untuk mempertahankan identitas jurnal serta sumber aslinya. Pilih sumber CSV template Kasiva saat mengimpor kembali ekspor Kasiva. Metadata tersebut memungkinkan transaksi manual maupun hasil impor Ivy yang sudah ada dilewati tanpa menyalinnya lagi.

CSV hanya membawa transaksi. Sesuaikan saldo awal setelah impor; ekspor CSV tidak mencakup seluruh pengaturan, anggaran, tujuan, investasi, dan saldo awal.

Penyimpanan transaksi dan impor memakai Firestore transaction untuk memeriksa referensi dan ID; operasi ini memerlukan koneksi internet. Snapshot yang sudah dicache dapat tetap ditampilkan saat offline, tetapi keberadaan cache tidak berarti penulisan transaksi offline sudah didukung.

## Tahap pemeriksaan manual pengguna

Setelah pengguna sendiri membangun aplikasi, periksa: daftar/login/logout; dua akun terpisah; transaksi tambah/ubah/hapus; transfer satu dan dua mata uang; arsip dompet; laporan lintas bulan; target/investasi; CSV berkutip dan duplikat; putus koneksi saat impor; pengingat setelah reboot; dan kunci saat kembali dari background. Semua pemeriksaan ini **belum dijalankan**.

## Versi dan sumber

Versi dipatok dalam Gradle agar tidak bergantung pada `latest`: Kotlin 2.3.20, Compose Multiplatform 1.10.3, Material3 1.9.0, AGP 8.13.2, Gradle 8.14.3, coroutines 1.10.2, serialization 1.9.0, datetime 0.6.2, GitLive Firebase 2.4.0.

- [Kompatibilitas Compose](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html)
- [GitLive Firebase Kotlin SDK](https://github.com/GitLiveApp/firebase-kotlin-sdk/tree/v2.4.0)
- [Panduan belajar lengkap](../docs/PANDUAN_STACK_DAN_MIGRASI.md)
