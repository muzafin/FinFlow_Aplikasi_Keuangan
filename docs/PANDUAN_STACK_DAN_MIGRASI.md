# Mengenal FinFlow: stack, Kotlin Multiplatform, Firebase, impor, dan koneksi bank

Dokumen ini berdasarkan pembacaan kode pada 26–28 Oktober 2026. Tidak ada build, analisis otomatis, emulator, pengujian perangkat, atau tes yang dijalankan dalam pekerjaan ini, sesuai permintaan pengguna. Berkas pengujian uang dan impor CSV disediakan untuk dijalankan sendiri nanti.

## 1. Stack itu apa?

Stack adalah kumpulan teknologi yang bekerja sama membentuk aplikasi. Bayangkan aplikasi sebagai restoran:

- Bahasa pemrograman adalah bahasa yang dipakai tim restoran untuk bekerja.
- UI adalah ruangan dan menu yang dilihat pelanggan.
- State adalah kondisi restoran saat ini: meja yang terisi, pesanan yang sedang dibuat.
- Router adalah penunjuk jalan dari satu ruangan ke ruangan lain.
- Database adalah buku pencatatan.
- Backend adalah bagian dapur yang melayani permintaan aplikasi.
- Authentication adalah pemeriksaan identitas sebelum seseorang boleh membuka data miliknya.

Multiplatform berarti sebagian kode dapat digunakan di beberapa sistem operasi. Sebagian layanan tetap memerlukan kode khusus perangkat, misalnya biometrik, pemilihan file, dan notifikasi.

## 2. Stack Flutter yang ditemukan dalam repository

### Bahasa dan UI

**Dart** adalah bahasa aplikasi lama. Berkasnya berakhiran `.dart`, misalnya `lib/main.dart`. Batas SDK dalam `pubspec.yaml` adalah `^3.8.0`; ini batas kompatibilitas, bukan bukti versi SDK yang sedang terpasang.

**Flutter** adalah framework untuk membuat tampilan dan menjalankan aplikasi Dart. Komponen tampilannya disebut widget. `Text` membuat tulisan, `Column` menyusun komponen vertikal, dan `Scaffold` menyediakan kerangka halaman.

**Material** menyediakan komponen seperti tombol, dialog, formulir, dan tema. Warna serta tipografi FinFlow dikumpulkan di `lib/core/theme/`, sehingga perubahan warna tidak perlu ditulis ulang di semua layar.

Repository lama memiliki direktori Android, iOS, dan web. Adanya direktori target tidak membuktikan setiap target sudah dikonfigurasi dan berjalan. Konfigurasi Firebase Dart lama hanya menyediakan Android/iOS, sehingga web belum boleh dianggap siap.

### State management: Riverpod

`flutter_riverpod ^2.6.1` mengatur data yang dipakai layar. Contohnya, daftar transaksi datang dari provider; ketika data Firestore berubah, UI yang mengamati provider dapat ikut berubah.

Bayangkan provider sebagai stopkontak data. Layar cukup meminta data pada stopkontak yang benar, bukan membuat koneksi database sendiri di setiap widget.

Ada `riverpod_annotation`, `riverpod_generator`, dan `build_runner` sebagai alat bantu pembuatan kode. Banyak provider pada checkout ini ditulis manual, sehingga keberadaan generator di dependensi tidak berarti seluruh aplikasi memakai hasil generator.

### Navigasi: go_router

`go_router ^14.8.1` menentukan perpindahan halaman. Konfigurasinya ada di `lib/core/router/app_router.dart`. Contohnya: dari login ke dashboard, atau dari daftar transaksi ke formulir transaksi.

### Backend: Firebase

- `firebase_core ^3.13.0`: membuka koneksi awal aplikasi dengan project Firebase yang dipilih.
- `firebase_auth ^5.5.2`: akun pengguna, login, pendaftaran, dan pemulihan kata sandi.
- `google_sign_in ^6.2.2`: antarmuka login akun Google, kemudian identitasnya dipakai Firebase Authentication.
- `cloud_firestore ^5.6.7`: database cloud untuk profil, dompet, transaksi, anggaran, kategori, investasi, dan target.
- `firebase_storage ^12.4.5`: terdaftar dan disediakan lewat getter di `FirebaseService`, tetapi saya tidak menemukan alur unggah lampiran yang lengkap. Jangan menganggap fitur unggah sudah selesai hanya karena dependensinya ada.
- `firebase_messaging ^15.2.10`: kode layanan FCM tersedia di `notification_service.dart`. Push notification juga memerlukan pengirim/backend dan konfigurasi platform; dependensi saja belum membuat pengiriman aktif.

Firestore menyimpan **collection → document → field**. Padanan sederhananya dengan SQL adalah **tabel → baris → kolom**, tetapi Firestore bukan database SQL dan tidak memakai join SQL.

Contoh lama: `users/UID/transactions/TRANSACTION_ID`. UID adalah identitas unik akun login. Pengguna lain harus dilarang membaca jalur tersebut melalui Security Rules, bukan hanya dengan menyembunyikan menu di UI.

Project lama yang tercatat pada konfigurasi adalah `finflow-c38e8`. Project itu berhasil dibuka menggunakan akun `basudewakrisna901@gmail.com`. Ini membuktikan akses akun tersebut; pemeriksaan daftar IAM diperlukan jika ingin memastikan seluruh pemilik project.

### Penyimpanan perangkat

`hive` dan `hive_flutter ^1.1.0` menyimpan pengaturan lokal, misalnya status onboarding dan pilihan keamanan. Hive dalam kode ini bukan pengganti Firestore untuk seluruh transaksi keuangan.

`local_auth ^3.0.1` meminta sistem operasi melakukan autentikasi perangkat. Aplikasi tidak menerima atau menyimpan sidik jari pengguna.

### Grafik, font, dan ekspor

- `fl_chart ^0.70.2`: grafik pada laporan dan investasi.
- `google_fonts ^6.2.1`: tipografi aplikasi.
- `intl ^0.19.0`: format tanggal, angka, mata uang, serta locale Indonesia.
- `path_provider ^2.1.5`: menemukan folder penyimpanan file aplikasi.
- `share_plus ^13.1.0`: membagikan hasil ekspor melalui menu bagikan perangkat.
- `uuid ^4.5.1`: ID data baru.
- `equatable ^2.0.7`: membandingkan objek berdasarkan isinya.
- `fpdart ^1.1.0`: tipe hasil seperti `Either`, untuk membedakan keberhasilan dan kegagalan operasi login.
- `flutter_local_notifications ^18.0.1`, `timezone ^0.10.1`, dan `flutter_timezone ^5.1.0`: pengingat lokal dan penyesuaian zona waktu.

`shimmer`, `cached_network_image`, `connectivity_plus`, dan `riverpod_annotation` juga dideklarasikan. Pencarian import tidak menemukan pemakaian langsungnya pada kode aplikasi saat pemeriksaan. Bedakan teknologi yang benar-benar dipakai dengan paket yang baru dicantumkan.

### Alat pembangunan

`pubspec.yaml` adalah daftar paket Flutter. `pubspec.lock` mencatat versi yang dipilih. Gradle membangun bagian Android, sementara Xcode membangun bagian iOS. Berkas Kotlin pada `android/` dalam aplikasi Flutter hanya pintu masuk native; itu belum menjadikan logika Dart sebagai Kotlin Multiplatform.

Konfigurasi Android Flutter lama mencantumkan AGP 9.0.1, Kotlin 2.3.20, Gradle wrapper 9.1.0, serta target kompilasi Java 17. Versi project Kotlin baru dipatok terpisah, sehingga mengganti konfigurasi project Kotlin tidak mengubah proses build Flutter lama.

Alur data yang paling penting:

1. Pengguna mengisi formulir transaksi.
2. Layar membentuk `TransactionModel`.
3. Repository menulis ke collection transaksi Firestore.
4. Stream/provider menerima data terbaru.
5. Dashboard dan daftar transaksi diperbarui.

## 3. Apa yang berubah di versi Kotlin?

Versi baru berada di `kotlinApp/`. Kode Flutter dipertahankan sebagai referensi migrasi; keduanya bukan satu aplikasi campuran.

| Tanggung jawab | Flutter lama | Kotlin baru |
|---|---|---|
| Bahasa | Dart | Kotlin |
| Tampilan bersama | Flutter widgets | Compose Multiplatform |
| State UI | Riverpod | Compose State dan Coroutine Flow |
| Perpindahan halaman | go_router | State halaman dan editor Compose |
| Operasi asynchronous | Future/Stream | suspend function, coroutine, Flow |
| Model data | Kelas Dart + toJson/fromJson | data class + kotlinx.serialization |
| Tanggal | DateTime + intl | kotlinx-datetime |
| Firebase bersama | FlutterFire | Firebase Kotlin SDK dari GitLive |
| Grafik | fl_chart | Canvas dan komponen Compose |
| Pengaturan lokal | Hive | SharedPreferences Android / UserDefaults iOS |
| Kunci perangkat | local_auth | BiometricPrompt Android / LocalAuthentication iOS |
| Pengingat | Plugin Flutter | WorkManager Android / UserNotifications iOS |
| Login Google | Plugin Flutter | Credential Manager Android / GoogleSignIn iOS |
| Berkas CSV | Ekspor saja | Importir bersama + pemilih/bagikan file native |

GitLive adalah library pihak ketiga yang membungkus SDK Firebase platform, bukan SDK KMP resmi Google. Dokumentasi dan versi yang dipakai tercantum di `kotlinApp/README.md`.

Kode `commonMain` menangani tampilan, perhitungan, model, repository, dan parser impor. `androidMain` menangani layanan Android. `iosMain` menyediakan pintu masuk Compose untuk iOS; layanan perangkat iOS dijembatani melalui Swift dalam `iosApp/Sources/`.

Target versi baru yang disiapkan adalah **Android dan iOS**. Web dan desktop belum ditambahkan ke proyek Kotlin. Dukungan KMP secara umum tidak otomatis membuat semua target tersedia dalam satu repository.

Mengubah Dart menjadi Kotlin bukan mengganti ekstensi file. Struktur UI, asynchronous code, plugin, dan proses build semuanya perlu dipindahkan. Tahap migrasi awal ditulis tanpa pengujian sesuai permintaan saat itu. Pada pembaruan UI Figma 29 Oktober 2026, pengguna mengizinkan pengujian perangkat; build Android, unit test dan uji UI berhasil. [Cakupan validasi](VALIDASI_UI_FIGMA.md) tetap dibedakan dari pengujian backend dan iOS.

## 4. Package name dan identitas aplikasi

Identitas versi Kotlin:

- Android `applicationId`: `com.logikawan.finflow`.
- Android namespace dan package Kotlin: `com.logikawan.finflow`.
- iOS bundle identifier: `com.logikawan.finflow`.
- Nama yang dilihat pengguna: FinFlow.

Package name seperti alamat unik aplikasi, bukan alamat email developer. Android menggunakan `applicationId` untuk mengenali aplikasi, Firebase menggunakannya untuk registrasi Android, dan Play Console menggunakannya untuk mencocokkan aplikasi yang diterbitkan.

Mengubah applicationId dari aplikasi yang sudah diterbitkan menghasilkan identitas aplikasi berbeda; itu bukan pembaruan pada listing lama. Konfigurasi Flutter lama dibiarkan utuh sebagai referensi agar tidak tersambung tanpa sengaja ke schema database baru.

## 5. Firebase baru dan hubungannya dengan Play Console

Pengguna memilih mulai dengan database baru. Project baru bernama **FinFlow Logikawan**, ID **`finflow-logikawan`**, pada akun **`muzafinarifin.dev@gmail.com`**. Konfigurasi Android yang diunduh pengguna telah dipasang pada `kotlinApp/composeApp/google-services.json`.

Satu email mempermudah pengelolaan, tetapi Firebase dan Google Play tetap dua produk berbeda. Membeli akun Play Console tidak otomatis mengaktifkan Firestore, menghubungkan semua layanan, atau membayar tagihan Google Cloud.

Registrasi Android, metode login, SHA sertifikat, registrasi iOS, aturan database, serta pengaitan Google Play adalah langkah yang berbeda. Status aktual dicatat pada `kotlinApp/firebase/SETUP_STATUS.md`, bukan disimpulkan dari keberadaan file konfigurasi.

`google-services.json` berisi konfigurasi klien dan API key Firebase, bukan private key Admin SDK. Aturan akses tetap wajib. Service account, private key, token bank, dan signing keystore tidak boleh dimasukkan ke kode aplikasi.

## 6. Database baru: collection apa saja?

Lihat `kotlinApp/firebase/SCHEMA.md`. Data aplikasi diletakkan di bawah `users/{uid}`:

- `accounts`: dompet dan saldo awal, jenis, serta mata uang.
- `categories`: kategori pemasukan/pengeluaran.
- `transactions`: jurnal transaksi dan transfer.
- `budgets`: batas pengeluaran per kategori dan bulan.
- `goals`: target tabungan serta kemajuan manual.
- `investments`: modal, nilai saat ini, dan dividen manual.

Collection muncul ketika dokumen pertama dibuat. Saat akun mendaftar, repository membuat profil, kategori awal, dan dompet Kas secara atomik. Collection anggaran/investasi yang masih kosong tidak membutuhkan baris palsu untuk dibuat terlebih dahulu.

Nominal baru memakai `Long` dalam satuan pecahan terkecil. Contoh IDR 10.000,50 disimpan sebagai `1000050`. Ini menghindari galat perhitungan pecahan biner. Pecahan mata uang seperti JPY dan KWD ditangani berbeda. Aset kripto berpresisi tinggi memerlukan model aset tersendiri; importir uang fiat tidak menganggap semua token sebagai mata uang dua desimal.

Saldo dihitung dari saldo awal dan jurnal. Transfer mengurangi dompet asal dan menambah dompet tujuan, tetapi tidak dimasukkan sebagai pemasukan atau pengeluaran laporan. Transaksi terjadwal belum memengaruhi saldo sampai ditandai selesai.

## 7. Impor Ivy Wallet dan aplikasi lain

Alur yang ditulis:

1. Ekspor **CSV transaksi** dari Ivy Wallet.
2. Di FinFlow Kotlin, buka Lainnya → Impor data.
3. Pilih sumber Ivy Wallet CSV, kemudian pilih berkas UTF-8.
4. Periksa pratinjau transaksi, dompet, kategori, duplikat, dan kesalahan.
5. Impor hanya dapat dimulai jika seluruh baris valid.
6. Setelah impor, periksa saldo awal setiap dompet; CSV transaksi tidak menyediakan saldo awal lengkap.

Format publik Ivy yang dirujuk berisi `Date`, `Title`, `Category`, `Account`, `Amount`, `Currency`, `Type`, kolom transfer, `Description`, `Due Date`, dan `ID`. ID sumber dipakai untuk mencegah impor ulang pada sumber yang sama. Jika koneksi terputus di tengah penyimpanan, impor dapat diulang; baris yang sudah tersimpan dilewati.

Impor tidak mengambil alih kode aplikasi Ivy. Implementasi parser dibuat mandiri berdasarkan format data publiknya. Repository Ivy menyatakan proyeknya tidak lagi dipelihara. Jangan menyalin seluruh kode berlisensi GPL ke produk lain tanpa meninjau kewajiban lisensinya.

Untuk aplikasi lain, FinFlow menyediakan template CSV. Pengguna perlu menyesuaikan nama kolom dan format datanya. **Belum ada klaim bahwa semua format Money Manager, Wallet, Spendee, XLS, PDF, OFX, ZIP, atau JSON dapat diimpor otomatis.** Adapter khusus dapat dibuat dari contoh ekspor asli setiap aplikasi.

## 8. Membaca transaksi bank/e-wallet otomatis

Ada tiga pendekatan yang berbeda:

**Impor berkas:** pengguna mengunduh mutasi/ekspor lalu mengimpornya. Ini dapat dikerjakan tanpa kontrak API bank, tetapi memerlukan format file yang sesuai dan tidak otomatis.

**Open banking/penyedia agregasi:** pengguna memilih institusi, menyetujui akses, lalu layanan mengambil saldo dan transaksi. FinFlow memerlukan backend, akun penyedia, cakupan negara dan institusi yang sesuai, serta pemenuhan ketentuan penyedia. Sering ada biaya komersial dan proses persetujuan bisnis. Akses pembayaran tidak dibutuhkan untuk aplikasi pencatat keuangan.

**Pembacaan notifikasi perangkat:** Android dapat menawarkan Notification Listener dengan izin khusus, tetapi notifikasi tidak menjamin seluruh transaksi, histori, saldo, atau format stabil. iOS tidak menyediakan pembacaan umum notifikasi aplikasi lain untuk aplikasi pihak ketiga. Karena itu, pendekatan ini bukan pengganti sinkronisasi bank global.

Target “semua bank dan e-wallet di seluruh dunia” tidak dapat dijamin. Bahkan layanan besar seperti Spendee dan Wallet mempublikasikan cakupan dan batas dukungan. Angka pemasaran pada situs dan listing toko juga dapat berbeda, sehingga jangan menggunakannya sebagai jaminan dukungan. Ketersediaan harus diperiksa per institusi, negara, produk API, dan akun.

Arsitektur yang diperlukan untuk koneksi otomatis:

1. Aplikasi meminta sesi koneksi kepada backend FinFlow dengan token Firebase pengguna.
2. Backend memilih penyedia yang mendukung institusi dan membuat sesi persetujuan baca-saja.
3. Pengguna menyelesaikan autentikasi pada halaman resmi bank/penyedia.
4. Backend menerima hasil otorisasi dan menyimpan token rahasia dalam penyimpanan server.
5. Sinkronisasi/webhook mengambil transaksi secara bertahap menggunakan cursor.
6. Backend menyamakan format, mata uang, zona waktu, ID transaksi, pending/posted, koreksi, serta penghapusan transaksi.
7. Deduplikasi dan peninjauan memastikan transfer antar-rekening sendiri tidak dihitung dua kali sebagai pendapatan/belanja.
8. Pengguna dapat mencabut persetujuan; token dan pekerjaan sinkronisasi dihentikan.

Koneksi ini tidak cukup diimplementasikan sebagai SDK Firebase di ponsel. Token penyedia dan kunci rahasia harus tetap di server. Koleksi cadangan `connections`, `externalAccounts`, dan `syncEvents` dalam rules hanya dapat ditulis backend; belum ada koneksi bank produksi yang dibuat.

Untuk Indonesia, SNAP adalah standar Open API Pembayaran. Keberadaan standar itu tidak otomatis memberikan akses publik ke seluruh rekening dan e-wallet. Ketersediaan endpoint dan hubungan kerja sama tetap perlu dikonfirmasi dengan penyedia terkait.

## 9. Batas pekerjaan ini

- Penulisan ulang inti aplikasi tersedia untuk ditinjau di `kotlinApp/`.
- Kode Flutter lama dan project Firebase lama tidak dihapus.
- Web/desktop Kotlin, upload lampiran, push FCM jarak jauh, kurs otomatis, harga pasar langsung, dan sinkronisasi bank global belum diimplementasikan.
- Pengingat harian native disiapkan. Android WorkManager dapat menunda notifikasi karena kebijakan baterai; bukan alarm tepat detik.
- Riwayat transaksi saat ini diamati sebagai satu jurnal per pengguna. Penggunaan besar memerlukan agregasi backend dan paginasi yang tetap menjaga ketepatan saldo.
- Status deployment Firebase harus mengikuti `SETUP_STATUS.md`.
- Tidak ada build, tes otomatis, atau validasi perangkat yang dijalankan.

## Referensi

- Firebase Android: https://firebase.google.com/docs/android/setup
- Firebase projects: https://firebase.google.com/docs/projects/learn-more
- Firebase KMP GitLive: https://github.com/GitLiveApp/firebase-kotlin-sdk/tree/v2.4.0
- Compose Multiplatform: https://kotlinlang.org/docs/multiplatform/compose-multiplatform.html
- Android application ID: https://developer.android.com/build/configure-app-module#set-application-id
- Format ekspor Ivy: https://github.com/Ivy-Apps/ivy-wallet/blob/eb08c930cba1f988a5a8a7d6b5ff15b913d3168a/shared/domain/src/main/java/com/ivy/domain/usecase/csv/IvyCsvRow.kt
- Spendee bank connect: https://www.spendee.com/bank-connect
- Wallet BudgetBakers: https://budgetbakers.com/en/products/wallet/
- Cakupan Plaid: https://plaid.com/docs/api/institutions/
- SNAP Bank Indonesia: https://www.bi.go.id/id/layanan/Standar/SNAP/default.aspx
