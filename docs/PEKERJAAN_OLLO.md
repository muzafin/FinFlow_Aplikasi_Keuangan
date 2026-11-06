# Penyesuaian FinFlow terhadap Ollo

Pembaruan detail Beranda/formulir/level/laporan bug pada 3 November tersedia di [laporan detail](DETAIL_OLLO_2026-11-03.md). Laporan ini memperjelas batas paritas: leaderboard masih rencana global/per negara bulanan dengan hadiah Premium admin 3/2/1 bulan, dan tidak menyatakan seluruh fitur/menu Ollo telah identik.

Acuan terbaru 2 November: **semua UI/menu/font mengikuti Ollo, memakai logo FinFlow; hanya navigasi liquid glass tetap desain sendiri**. Mode lokal tanpa login dengan pilihan sinkronisasi akun telah diminta. Catatan izin/desain lama di bawah adalah riwayat. [Pembaruan lokal, reset, peta jalan dan validasi](VALIDASI_LOKAL_DAN_PETA_JALAN_2026-11-02.md).

Status pekerjaan sekarang juga dapat dibuka langsung melalui Profil → Masukan & Peta Jalan. Katalog 60 fitur mencatat pekerjaan yang selesai dalam lingkupnya, sedang dikerjakan dan direncanakan; tidak menyatakan seluruh kesamaan Ollo telah selesai.

Permintaan 29 Oktober 2026: gradien saldo bergerak, hilangkan kata Ini pada periode Beranda, ikuti UI dan fungsi Ollo pada HP pengguna. Konfirmasi pengguna: pertahankan neon dan liquid glass; AI dan Premium menyusul. Login/akun Firebase FinFlow yang ada tetap dipakai.

## Acuan yang diperiksa langsung

Ollo `com.ollo.ollo`, versi yang ditampilkan 1.7.9 build 292, pada Xiaomi pengguna. Penelusuran hanya membuka halaman/form dan menutup tanpa menyimpan; tidak login ke Ollo dan tidak membuat transaksi. Screenshot/XML lokal di `kotlinApp/build/ollo-reference/`, diabaikan Git. `inspect.py` di sana hanya alat penelusuran sementara melalui adb, bukan kode aplikasi.

**Izin lanjutan pengguna:** pengguna mengizinkan pembuatan/penghapusan contoh berlabel UJI FINFLOW dan juga mengizinkan login bila diperlukan. AI/Premium tetap ditunda. Izin ini menggantikan batas penelusuran baca-saja awal di atas. Login belum dilakukan.

**Data contoh Ollo yang WAJIB dibersihkan sebelum selesai:** bundle `UJI_FINFLOW_BUNDLE`, satu item `UJI_FINFLOW_ITEM`, Rp1.000, mode satu transaksi, dompet Cash. Bundle sudah dicentang dan dibayar pada 29 Oktober sekitar 16:30; dibuat jurnal `Bundle: UJI_FINFLOW_BUNDLE` sehingga Cash sementara -Rp1.000. Menu histori bundle menyediakan Undo Pay dan Delete. Gunakan Undo Pay lalu hapus bundle agar jurnal uji dan saldo kembali semula; jangan hapus data lain. Pembayaran juga memunculkan badge/10 XP Ollo. Catat semua contoh baru di bagian ini sampai dibersihkan.

Tambahan fixture: `UJI_FINFLOW_GOAL`, target Rp10.000, Standard Goal, Flexible, tanpa tanggal/foto. Ollo otomatis membuat wallet Savings dengan nama yang sama. Dibuat income `UJI_FINFLOW_FUNDS` Rp10.000 ke Cash (kategori Salary) agar deposit dapat diuji; goal menerima deposit Rp1.000 sekitar 16:41. Posisi sementara: Cash Rp8.000, wallet goal Rp1.000; total Rp9.000 (income 10.000 dikurangi expense bundle 1.000). **Wajib kembalikan deposit/bersihkan goal & savings wallet, Undo Pay/hapus bundle, dan hapus income UJI_FINFLOW_FUNDS. Saldo awal sebelum pengujian adalah Cash Rp0 dan tidak ada goal/bundle/transaksi.** Badge tambahan muncul setelah goal dan income; jangan menghapus data lain atau reset seluruh aplikasi.

- Beranda: sapaan, level, shortcut; periode Day/Week/Month/Year/Calendar; saldo dan income/expense; menu horizontal Budget/Recurring/Goals/Bills serta menu lain di pengaturan arrangement; monthly budget, wallet carousel, recent transactions.
- Menu Arrangement: pratinjau, aktif/nonaktif dan urutan 10 menu: Budget, Recurring, Goals, Bills, Debts, Wishlist, Cards, Notes, Reimburse, Asset Management. More juga menyediakan Menu Appearance.
- Profil: header avatar/nama/level/streak/active days; grid Account Management, Backup & Restore, Connected Devices, Data Export; Import Data, Recording Period, Categories, Wallet Settings, Notifications. Preferensi: palet, theme, card appearance, menu appearance, transaction palette, statistics section order, home view, add transaction display, icon pack, balance formatting. Bagian bawah Help, Feedback, Share, Bug Report, About, Update Log, Delete Data dan Exit.
- Account Management Ollo meminta login untuk AI/kuota dan Cloud Sync; tidak dimasuki lebih jauh. AI/Premium dikecualikan pengguna.
- Dompet: ringkasan saldo, perubahan 30 hari, kekayaan/liabilitas/tabungan/pembayaran mendatang; kelompok Cash/Bank/E-wallet. Detail dompet: Weekly/Monthly/Yearly/All, rentang waktu, tren saldo, inflow/outflow/netflow/count, tombol income/expense/transfer dan jurnal. Menu: Update Balance, Edit Wallet, Delete.
- Edit dompet: tipe, nama, mata uang, current balance, exclude from total/statistics, link as pocket, custom icon. Tidak menyimpan perubahan pada Ollo.
- Statistik: Weekly/Monthly/Yearly/Custom Range, filter, periode, Income/Expense. Urutan bagian: Monthly Comparison, Net Balance Trend, Cycle Insights, Top Spenders, Transaction Log. Akun Ollo kosong sehingga chart detail dengan data belum dapat diamati.
- Transaksi: bottom sheet menempel, category/subcategory, reimburse amount, tags, title/note/wallet, keypad, kalkulator dan tanggal. Split Bill menampilkan Under Development; CC/Paylater belum terverifikasi. AI scan/voice/chat dikecualikan.
- Goals: ringkasan savings/remaining, bucket; form target amount, name, optional image, bucket type, optional date, withdrawal protection. Detail bucket belum teramati karena Ollo tidak memiliki goal tersimpan.
- Bucket types: Standard Goal, Emergency Fund, Locked / Deposito. Proteksi: Flexible, Soft Lock (peringatan), Hard Lock (konfirmasi dua tahap).
- Goal detail: target/progres, Deposit, Withdrawal setelah ada saldo, histori. Deposit memilih nominal (tombol 5/10/25/50%), source wallet dan tanggal. Info Ollo menyatakan setoran adalah **transfer internal**, bukan expense statistik. Deposit ditolak ketika saldo sumber tidak cukup. Setelah fixture deposit: Cash 8.000 + wallet Savings 1.000 = Home/Wallet Total Balance dan Net Worth 9.000; income tetap 10.000, expense tetap 1.000, Active Savings 1.000. Implementasi FinFlow perlu wallet tabungan dan jurnal transfer, bukan expense/income biasa. Progres manual goal FinFlow lama jangan diubah menjadi uang baru secara otomatis.
- Cards: tab Bank/Cards/E-Wallet/Others. Form bank: pratinjau tujuh desain, jenis catatan, warna, foto/logo, nama bank, nomor rekening, nama pemilik, label, cabang, ownership Personal, kategori Main, pin. FinFlow sekarang hanya metadata kartu empat digit, sehingga belum setara.
- Notes Ollo ternyata **bundle belanja**, bukan hanya catatan teks. Form berisi nama, satu transaksi/per item, wallet/currency, daftar item nama-harga, catatan, total estimasi. Centang item lalu Pay membuka konfirmasi wallet/tanggal dan membuat expense. Histori mendukung Undo Pay dan Delete. Catatan teks FinFlow yang sudah ada harus tetap terjaga ketika menambahkan mode bundle.
- Asset Management: harga pasar ditampilkan; Add Asset/Add Mutual Fund berikon kunci pada akun Ollo yang diperiksa. Jangan mengklaim layanan harga pasar Ollo telah tersedia di FinFlow; AI/Premium ditunda, portofolio manual FinFlow tetap dijaga.
- Statistik setelah fixture bundle: donut alokasi kategori, Daily Overview, Monthly Comparison, Net Balance Trend, Cycle Insights (rata-rata/proyeksi), Activity Heatmap, Top Spenders, Transaction Log, Advanced Analysis. Bagian yang dapat diatur urutannya tetap lima seperti daftar di atas.

## Pembaruan 2 November 2026

Lanjutan yang sudah ditulis telah dipasang dan diuji pada Xiaomi: Beranda/Profil/Statistik/detail dompet, preferensi tampilan, kategori/tag/pocket, bundle, target dengan jurnal transfer, kartu/rekening, serta pemulihan cadangan. Sebanyak 23 pengujian UI/repository/backup lulus, ditambah pengujian dua akun baru dan konfirmasi penghapusan. Rules metadata terbaru kini aktif di cloud; akun yang sebelumnya gagal login berhasil membuka Beranda. Storage foto profil dan backend hapus akun sudah aktif. [Laporan lengkap dan batasnya](VALIDASI_2026-11-02.md).

Pemeriksaan ulang Ollo di HP menunjukkan Cash, income, expense dan total savings Rp0, **No goals yet**, serta **Create your first bundle!** pada Notes. Fixture lama sudah tidak ada pada daftar aktif. Tidak membuat contoh baru pada sesi 2 November. Catatan fixture 29 Oktober di atas merupakan riwayat; jangan menghapus data pengguna atau mereset aplikasi berdasarkan catatan lama tersebut.

## Pembaruan 3 November 2026

Tour onboarding dan wizard pengaturan awal Ollo sudah ditulis ulang dan diuji pada HP Xiaomi pengguna:

- Tour onboarding empat slide: ilustrasi Figma, badge berwarna, judul 34sp, titik halaman, tombol pill dengan chevron, dan tombol "Lewati" pada slide pertama. Teks tour tetap Bahasa Indonesia.
- Wizard pengaturan awal enam langkah: welcome (nama, foto profil opsional, pulihkan cadangan, masuk Google atau email), pilih bahasa, pilih mata uang, notifikasi pintar, pilih gaya, dan dompet pertama. Data dikumpulkan sebagai `SetupResult` lalu disimpan oleh `App.kt` ke mode lokal.
- Lokalisasi ID/EN hanya untuk layar wizard (55 teks). Halaman utama tetap Bahasa Indonesia; pilihan bahasa lain ditandai "Segera" dan belum mengubah antarmuka.
- Alur pertama kali: splash → tour → wizard → mode lokal. Login/pemulihan cadangan tersedia dari langkah welcome; login cloud email/Google tetap melalui `AuthScreen`.
- Setelah setup selesai, sheet "Tombol + mau buka apa?" muncul sekali. Tombol + kemudian mengikuti preferensi `addButtonMode`: langsung membuka editor transaksi atau `RecordMethodsSheet` ("Semua cara mencatat"; rekam suara dan scan struk ditandai "Segera").
- Preferensi baru `language`, `cardGradient`, `menuTheme` dan `addButtonMode` tersimpan di `AppPreferences`, ditambah `AppearancePresets` (6 gradien kartu saldo, 5 tema warna ikon menu, 6 mode tema). Beranda memakai gradien kartu dan warna menu terpilih; Pengaturan mendapat "Preset mode", "Gradien kartu saldo", "Warna ikon menu" dan "Perilaku tombol +".

Hasil uji: build `:composeApp:assembleDebug`, `:composeApp:testDebugUnitTest` dan `:composeApp:assembleDebugAndroidTest` berhasil. 49 unit test Kotlin lulus. Di HP Xiaomi, 20 uji instrumentasi lulus: `OlloSetupDeviceTest` (1), `FinFlowDesignDeviceTest` (14), `LocalUiDeviceTest` (3) dan `AddButtonSheetDeviceTest` (2). Tangkapan layar tour dan wizard disalin ke `docs/ui-evidence/2026-11-03/`; sumber aslinya di `kotlinApp/build/lead-verifikasi/preview/`. [Laporan lengkap dan batasnya](VALIDASI_2026-11-03.md).

Batas: langkah "Perintah Suara" Ollo belum dibuat karena Quick Record suara belum tersedia (wizard FinFlow saat ini enam langkah, sedangkan acuan Ollo yang diperiksa tujuh langkah); terjemahan EN baru mencakup wizard; sheet intro tombol + belum diuji end-to-end melalui alur setup pada data bersih (ditutup pada ronde 2 sore hari yang sama; lihat di bawah); iOS belum dibangun/diuji; paritas piksel seluruh halaman Ollo masih berjalan.

Mode pratinjau QA (alat verifikasi, bukan fitur pengguna): `adb shell am start -n com.logikawan.finflow/.MainActivity --ez finflow_preview_first_run true` menampilkan tour lalu wizard dari awal tanpa menyimpan data pengguna; selesai/Back kembali ke layar normal. Alat ini hanya untuk build debug di perangkat sendiri dan tidak boleh dipakai untuk mengubah data pengguna.

### Pembaruan ronde 2 (3 November 2026, sore)

Ronde 2 memvalidasi dua perbaikan QA dan menjalankan uji end-to-end nyata mode lokal pada data bersih di HP Xiaomi pengguna (bukan emulator), sekitar 14.08–14.16 WIB (Asia/Jakarta):

- Validasi nama lengkap wajib ala Ollo pada wizard pengaturan awal: "Lanjut" dengan nama kosong tetap di langkah welcome, field merah dan error inline "Nama lengkap wajib diisi." (ID) / "Full name is required." (EN) dengan testTag `setup-name-error`; error hilang saat mengetik; `finish()` punya pertahanan ganda agar tidak pernah selesai dengan nama kosong (`OlloSetup.kt`, `OlloSetupStrings.kt`).
- Dialog "Lihat Semua" mata uang dirombak menjadi `OlloCurrencyDialog` (`OlloCurrencyPicker.kt`): `AlertDialog` ungu bawaan Material3 dihapus, diganti sheet putih netral menempel bawah, pencarian case-insensitive kode+nama, 45 mata uang dengan simbol+nama+kode, baris terpilih border hitam, empty state dan tombol tutup. `EntryTheme` di `OnboardingScreen.kt` kini `lightColorScheme` netral penuh sehingga warna ungu bawaan (#ECE6F0/#E8DEF8/#FFD8E4) tidak bocor ke dialog/alert/dropdown mana pun di alur onboarding/auth.
- Mode pratinjau QA `finflow_preview_first_run` berhenti di layar "Pratinjau pengaturan awal selesai" dengan tombol Tutup; tidak kembali masuk ke wizard nyata bila tidak ada akun.

Hasil uji ronde 2: build final (`:composeApp:testDebugUnitTest`, `:composeApp:assembleDebug`, `:composeApp:assembleDebugAndroidTest`) berhasil; 50 unit test Kotlin lulus dari 11 kelas; 22 uji instrumentasi lulus di HP (`OlloSetupDeviceTest` 1, `OlloCurrencyPickerDeviceTest` 1, `LocalSetupPersistenceDeviceTest` 1, `FinFlowDesignDeviceTest` 14, `LocalUiDeviceTest` 3, `AddButtonSheetDeviceTest` 2). Uji end-to-end nyata dijalankan setelah data aplikasi dikosongkan pengguna: tour → welcome nama kosong dan error → isi "Muhammad" → bahasa ID → mata uang (cari "rupiah", pilih IDR) → notifikasi tanpa izin → gaya mode Gelap → dompet Cash Rp1.000.000 → Lanjut. Beranda lokal tampil dengan sapaan "~ Hai, Muhammad!" dan Total Saldo Rp1.000.000,00; sheet "Tombol + mau buka apa?" lalu sheet "Cara mencatat" muncul; setelah force-stop dan buka ulang, Beranda tetap tampil dan preferensi tersimpan. Data uji kemudian dikosongkan lagi agar pengguna mendapat first-run bersih; data cloud di Firestore tidak disentuh. [Laporan lengkap](VALIDASI_2026-11-03.md).

Batas ronde 2: login/akun cloud dari langkah welcome belum diuji end-to-end; langkah "Perintah Suara" Ollo belum ada; terjemahan hanya wizard; iOS belum diuji; auto-scroll dialog mata uang ke pilihan aktif belum dibuat.

### Pembaruan ronde 3 (3 November 2026) — Beranda

Ronde 3 berfokus pada Beranda dan dijalankan sore hari yang sama pada HP Xiaomi pengguna (bukan emulator). Yang disamakan dengan susunan Ollo:

- Header: sapaan "~ Hai, <nama>" tetap, badge level menjadi pill teks "Lvl N" tanpa ikon, ditambah tombol ikon profil (testTag `dashboard-profile`) dan tombol tiga titik (testTag `dashboard-shortcuts`) dengan menu Riwayat Transaksi/Kalender/Tambah Transaksi/Pengaturan Beranda.
- Tab periode: pil hitam hanya pada pilihan aktif, tanpa kontainer abu di sekelilingnya (Hari/Minggu/Bulan/Tahun/Kalender).
- Kartu saldo: judul "Total Saldo (KODE)" (sebelumnya "Total Saldo Utama" tanpa kode), ikon mata privasi (`dashboard-privacy`) dan ikon swap urutan mata uang (`dashboard-currency-swap`).
- Dompet: carousel horizontal diganti grid dua kolom dengan lingkaran ikon 40dp, nama, "TIPE • KURS" dan nominal; testTag `dashboard-wallet-<id>` tetap.
- Transaksi terbaru: dikelompokkan per tanggal dengan header tanggal Indonesia (contoh "Selasa, 3 November 2026") dan total bersih harian berwarna, baris ala Ollo (ikon kategori, judul, subjudul "dompet · kategori/sumber · jam", nominal bertanda), maksimal enam transaksi, dan label sumber seperti "Transaksi Tabungan"/"Pembayaran Tagihan".
- Anggaran bulanan kosong: state kosong berpusat dengan ikon plus di dalam lingkaran dan teks ajakan.

Hasil uji ronde 3: build `:composeApp:testDebugUnitTest`, `:composeApp:assembleDebug` dan `:composeApp:assembleDebugAndroidTest` berhasil; 50 unit test Kotlin lulus; 24 uji instrumentasi lulus di HP (`DashboardParityDeviceTest` 2, `FinFlowDesignDeviceTest` 14, `LocalUiDeviceTest` 3, `OlloSetupDeviceTest` 1, `OlloCurrencyPickerDeviceTest` 1, `LocalSetupPersistenceDeviceTest` 1, `AddButtonSheetDeviceTest` 2). `DashboardParityDeviceTest` baru merender Beranda tema terang dan gelap dengan fixture terisolasi dan menyimpan tiga tangkapan layar; salinannya di `docs/ui-evidence/2026-11-03-ronde3/`. [Laporan lengkap](VALIDASI_2026-11-03.md).

Batas ronde 3: ikon swap hanya muncul bila lebih dari satu mata uang; subjudul transaksi digabung satu baris (Ollo memecah beberapa baris); uji memakai fixture data terisolasi; tangkapan tema gelap dari harness uji berlatar putih di luar `LazyColumn` karena latar halaman dipasang induk di aplikasi nyata (bukan bug aplikasi); halaman statistik, dompet dan profil belum disamakan.

### Pembaruan ronde 4 (3 November 2026) — Statistik

Ronde 4 berfokus pada halaman Statistik dan dijalankan sore hari yang sama pada HP Xiaomi pengguna (bukan emulator). Yang disamakan dengan susunan Ollo:

- Header: tombol filter (testTag `statistics-filters`), judul "Statistik", ikon info (testTag `statistics-info`, dialog "Tentang statistik") dan menu tiga titik (testTag `statistics-menu`: Ekspor CSV/PDF, Urutan Bagian Statistik, Preferensi Tampilan); tombol ekspor di header lama dihapus dan fungsinya pindah ke menu.
- Tab periode: Minggu/Bulan/Tahun/Rentang dengan pilihan aktif tebal, disertai panah pindah periode.
- Kartu donut: judul "Total Pengeluaran"/"Total Pemasukan", donut 190dp dengan label "Total" dan nominal di tengah (testTag `statistics-total`), legenda kategori kanan (maksimal empat), rincian kategori (ikon, progress, persen, nominal).
- Ringkasan Harian: rata-rata Pemasukan/Pengeluaran/Tabungan, grafik batang berpasangan dan legenda.
- Perbandingan Bulanan: chip tren persen bila periode sebelumnya ada datanya, kalimat naik/turun, bar Sebelumnya/Periode ini dan Selisih.
- Wawasan Siklus: label "Rata-rata Harian"/"Proyeksi Total".
- Peta Aktivitas: header nama hari dan legenda Sedikit..Banyak.
- Pengeluaran Terbesar: peringkat, jumlah transaksi dan progress.
- Log Transaksi: chip Semua/nama dompet (dialog filter), pencarian, tombol "Tampilkan lebih banyak" dan "Buka Semua Transaksi".
- Kartu baru "Analisis Lanjutan" (testTag `statistics-advanced`): skor kesehatan finansial = (pemasukan − pengeluaran) / pemasukan × 100 dengan progress dan teks "X/100"; bila belum ada pemasukan tampil ajakan menambah catatan beberapa minggu (hitungan sederhana, bukan AI).

Hasil uji ronde 4: build `:composeApp:testDebugUnitTest`, `:composeApp:assembleDebug` dan `:composeApp:assembleDebugAndroidTest` berhasil (log `kotlinApp/build/lead-build-stats-2.log`; APK debug 14.42 WIB terpasang); 50 unit test Kotlin lulus; 26 uji instrumentasi lulus di HP (`StatisticsParityDeviceTest` 2 baru, `DashboardParityDeviceTest` 2, `FinFlowDesignDeviceTest` 14, `LocalUiDeviceTest` 3, `OlloSetupDeviceTest` 1, `OlloCurrencyPickerDeviceTest` 1, `LocalSetupPersistenceDeviceTest` 1, `AddButtonSheetDeviceTest` 2). Salinan tangkapan layar di `docs/ui-evidence/2026-11-03-ronde4/`. [Laporan lengkap](VALIDASI_2026-11-03.md).

Batas ronde 4: chip tren hanya muncul bila periode sebelumnya memiliki data; skor kesehatan finansial adalah hitungan sederhana, bukan AI; uji memakai fixture data terisolasi; tangkapan tema gelap dari harness hampir kosong karena latar halaman dipasang induk (bukan bug aplikasi); halaman dompet dan profil belum disamakan.

### Pembaruan ronde 5 (3 November 2026) — Dompet

Ronde 5 berfokus pada halaman Dompet dan Detail dompet, dijalankan sore hari yang sama pada HP Xiaomi pengguna (bukan emulator). Yang disamakan dengan susunan Ollo:

- Header: ikon pie chart "Komposisi Uang" (testTag `wallet-composition`), judul "Dompet Saya" di tengah, menu tiga titik (testTag `wallet-menu`: Tambah Dompet, Dompet Diarsipkan / Kembali ke Dompet Aktif, Sembunyikan/Tampilkan Saldo); tombol + di header dan tab Aktif/Arsip dihapus, mode arsip lewat menu dan banner.
- Kartu total saldo per mata uang: "Total Saldo (KODE)" + ikon privasi, nominal besar, chip persen perubahan 30 hari (contoh +69.4%) dan teks "(+RpX) 30 hari terakhir".
- Grid 2x2: Total Kekayaan, Liabilitas, Tabungan Aktif, Pembayaran Mendatang.
- Grup Tunai/Akun Bank/E-Wallet selalu tampil dengan empty state; grup lain tampil bila ada isi; kartu dompet ala Ollo (ikon, nama, "TIPE • IDR", Saldo Saat Ini, nominal); pocket dan testTag `wallet-<id>` dipertahankan.
- Section "Target Tabungan": kartu target (nama, "TARGET TABUNGAN • IDR", Saldo Saat Ini, Target, progress) yang membuka Detail impian lewat holding account.
- Dialog "Komposisi Uang": total + baris per tipe (persen, progress, nominal), mata uang dapat dipilih bila lebih dari satu; kartu kredit/pinjaman tidak dihitung sebagai komposisi uang (liabilitas).
- Detail dompet: top bar aplikasi menampilkan nama dompet dan menu Sesuaikan Saldo / Edit / Arsipkan Dompet; isi tab Mingguan/Bulanan/Tahunan/Semua, rentang tanggal, Saldo Saat Ini, grafik saldo dengan label awal/akhir, ringkasan berjudul dinamis + rentang, metrik Arus Masuk/Keluar/Bersih/Jumlah Transaksi, kartu aksi Pengeluaran/Pemasukan/Transfer, dan Transaksi Terbaru dengan empty state.

Hasil uji ronde 5: build `:composeApp:testDebugUnitTest`, `:composeApp:assembleDebug` dan `:composeApp:assembleDebugAndroidTest` berhasil (build APK final di `kotlinApp/build/lead-build-wallet-5.log`, 15.04 WIB; unit test pada `lead-build-wallet-2.log`); 50 unit test Kotlin lulus; 29 uji instrumentasi lulus di HP (`WalletParityDeviceTest` 3 baru, `StatisticsParityDeviceTest` 2, `DashboardParityDeviceTest` 2, `FinFlowDesignDeviceTest` 14, `LocalUiDeviceTest` 3, `OlloSetupDeviceTest` 1, `OlloCurrencyPickerDeviceTest` 1, `LocalSetupPersistenceDeviceTest` 1, `AddButtonSheetDeviceTest` 2); APK debug 15.04 WIB terpasang. Salinan tangkapan layar di `docs/ui-evidence/2026-11-03-ronde5/`. [Laporan lengkap](VALIDASI_2026-11-03.md).

Batas ronde 5: komposisi uang hanya dompet non-kredit/pinjaman dan saldo negatif dihitung 0; tombol "+ Tambah Dompet" di bawah daftar tetap ada; teks empty state memakai "Ketuk tombol + untuk menambahkan"; penghapusan dompet permanen belum ada (masih arsip, FF-046); uji memakai fixture data terisolasi; tangkapan detail dompet tidak memuat top bar aplikasi; halaman Profil belum disamakan.

### Pembaruan ronde 6 (3 November 2026) — verifikasi Firebase

Ronde 6 memverifikasi integrasi Firebase memakai Firebase Emulator Suite (project demo `demo-finflow`), bukan data produksi:

- Firestore rules 17/17 lulus, storage rules 3/3 lulus, Cloud Functions account deletion 1/1 dan financial reset 1/1 lulus di emulator; rules lokal ekuivalen dengan produksi (deployment terakhir 2 November 2026) dan tidak ada deploy ulang.
- Device test di HP Xiaomi: 40 test lulus, termasuk `CloudModeSmokeDeviceTest` baru (akun cloud Firebase Auth emulator → Beranda mode cloud → transaksi tersimpan di Firestore emulator lewat `FinanceRepository` dan lolos rules → tampilan lokal ikut terbarui), serta `PlanningRepositoryDeviceTest` (5) dan `BackupRestoreDeviceTest` (5).
- Unit test Kotlin: 50 lulus, 0 gagal (jumlah belum berubah; build final dijalankan Lead).
- Lingkungan: Firebase CLI 15.34.2; emulator auth 9098/firestore 8088/storage 9198/functions 5008; Functions memerlukan Node 22 sehingga dipasang `node@22 --no-save` di `firebase/functions` (package.json tidak berubah); Java default rusak sehingga memakai JBR Android Studio lewat `JAVA_HOME`; HP terhubung lewat `adb reverse` dan dibersihkan setelah pengujian.
- Log: `kotlinApp/firebase/firebase-debug.log`, `kotlinApp/firebase/firestore-debug.log`, `kotlinApp/build/lead-full-suite-firebase.log`, serta log uji di `firebase/tests/`.

Batas ronde 6: verifikasi memakai emulator (bukan produksi); login Google nyata tidak diotomasi (akun uji email/password emulator); tidak ada operasi destruktif pada data produksi; App Check/FCM, kuota produksi dan perangkat lain belum diuji. [Laporan lengkap](VALIDASI_2026-11-03.md).

### Pembaruan ronde 7 (3 November 2026) — Profil & ikon

Ronde 7 berfokus pada halaman Profil dan perbaikan ikon fallback, dijalankan sore hari yang sama pada HP Xiaomi pengguna (bukan emulator); sebagian uji memakai Firebase emulator. Yang disamakan dengan susunan Ollo:

- Header Profil: avatar 88dp + badge kamera 30dp di kanan-bawah, nama besar tanpa baris email; metrik LEVEL/STREAK/HARI AKTIF dengan dua divider vertikal 1dp dan chevron; seluruh header menuju "Profil akun", baris metrik menuju "Aktivitas".
- Section caption ("Akun & Data", "Preferensi", "Fitur Masa Mendatang", "Keamanan & Lainnya") kecil tebal abu; tile grid dua kolom tinggi 172dp; row list ikon lingkaran 40dp + chevron; palette lima tema dan preferensi dipertahankan; "Upgrade ke Premium" tidak ada (Premium/AI ditunda).
- Perbaikan ikon: chevron tanpa akhiran `_rounded` diganti nama terdaftar di `DashboardScreen.kt`, `WalletDetail.kt` dan `ProfileScreen.kt`; ikon Profil sort/feedback/help/share/bug/info diganti ke nama terdaftar; `AuthViews` restore→`history`, `CloudSyncScreen`→`sync_alt`, `TransactionEditor`→`close_rounded` + alias `calculate_outlined`; `FFIcon` memakai aset Figma sebagai fallback sehingga tidak lagi menampilkan glyph fallback generik.
- Uji baru `ProfileParityDeviceTest` (2 test, tema terang dan gelap, empat tangkapan layar); `FinFlowDesignDeviceTest` tidak diubah dan tetap lulus (termasuk "Pengguna Uji", "Manajemen Akun", dan "Upgrade ke Premium" tidak ada).

Hasil uji ronde 7: build `:composeApp:testDebugUnitTest`, `:composeApp:assembleDebug` dan `:composeApp:assembleDebugAndroidTest` berhasil (log `kotlinApp/build/lead-build-profile-4.log`, 15.33 WIB); 50 unit test Kotlin lulus; 42 uji instrumentasi lulus di HP dalam dua proses — 31 test UI lokal (`lead-local-suite-final.log`) dan 11 test Firebase cloud (`lead-cloud-suite-final.log`). Satu percobaan gabungan 42 test dalam satu proses sempat macet di tengah (bukan kegagalan test; proses dihentikan), lalu dijalankan dua proses terpisah dan semuanya lulus. Salinan tangkapan layar di `docs/ui-evidence/2026-11-03-ronde7/`. [Laporan lengkap](VALIDASI_2026-11-03.md).

Batas ronde 7: Premium/AI sengaja belum dihadirkan; email tetap tersedia di Manajemen Akun; login Google nyata tidak diotomasi; iOS dan formulir/menu keuangan lain belum disamakan; beberapa ikon memakai glyph valid terdaftar karena belum ada padanan aset; uji memakai fixture data terisolasi.

## Pelaksanaan

- [x] Sumber animasi gradien saldo ditulis dan compile Android berhasil.
- [x] Label periode menyembunyikan kata Ini tanpa mengganti kunci filter/saved state.
- [ ] Penelusuran menu keuangan dan pengaturan Ollo yang tersisa.
- [x] Komponen visual Ollo dan preferensi yang benar-benar memengaruhi tampilan (tour onboarding dan wizard pengaturan awal sudah ditulis; paritas seluruh halaman belum selesai).
- [x] Validasi nama lengkap wajib dan dialog mata uang netral pada wizard pengaturan awal, plus uji end-to-end lokal di HP pada 3 November 2026 sore.
- [ ] Langkah perintah suara Ollo, paritas visual penuh tour/wizard dengan referensi, dan uji login/akun cloud dari langkah welcome.
- [x] Beranda: header (sapaan, pill level, ikon profil, tombol tiga titik), tab periode pil hitam, kartu saldo "Total Saldo (KODE)", grid dompet dua kolom, transaksi terbaru per tanggal, state kosong anggaran — disamakan dengan Ollo pada ronde 3 (3 November 2026).
- [x] Statistik: header (filter, info, menu tiga titik), tab periode, kartu donut, ringkasan harian, perbandingan bulanan, wawasan siklus, peta aktivitas, pengeluaran terbesar, log transaksi, analisis lanjutan — disamakan dengan Ollo pada ronde 4 (3 November 2026).
- [x] Dompet dan detail dompet: header (komposisi uang, menu tiga titik, mode arsip), kartu total saldo per mata uang dengan perubahan 30 hari, grid Total Kekayaan/Liabilitas/Tabungan Aktif/Pembayaran Mendatang, grup dompet dengan empty state, kartu Target Tabungan, dialog Komposisi Uang, serta detail dompet (tab periode, grafik saldo, ringkasan, metrik, kartu aksi, transaksi terbaru) — disamakan dengan Ollo pada ronde 5 (3 November 2026).
- [x] Profil: header ala Ollo (avatar + badge kamera, nama, metrik LEVEL/STREAK/HARI AKTIF, chevron), section caption, tile grid dua kolom 172dp, row list ikon lingkaran 40dp, palette dan preferensi; "Upgrade ke Premium" tidak ada — disamakan dengan Ollo pada ronde 7 (3 November 2026), termasuk perbaikan ikon chevron/fallback generik.
- [ ] Formulir/menu keuangan lain, iOS dan uji manual pengguna — belum disamakan/diuji.
- [ ] Fungsi keuangan/persistensi yang belum dimiliki FinFlow, dengan perlindungan data lama.
- [x] Verifikasi integrasi Firebase di emulator (Firestore rules 17/17, storage rules 3/3, Cloud Functions 2/2, 40 device test termasuk uji cloud mode) — 3 November 2026; bukan uji produksi.
- [ ] Pengujian yang sesuai, pemasangan APK dan pencatatan batas yang belum dapat diverifikasi — sebagian: integrasi Firebase emulator sudah diverifikasi (ronde 6); uji manual pengguna dan akun cloud nyata belum.

Status ini masih pekerjaan berjalan. Jangan memakai catatan ini sebagai bukti bahwa seluruh fungsi sudah selesai atau telah diuji.

Checkpoint 2 November: build Android, pemasangan APK, pengujian UI/repository dan publikasi rules berhasil. Manajemen Akun, cadangan JSON, ekspor PDF/CSV, aktivitas dan bantuan sudah dirutekan. Perangkat Terhubung masih menampilkan perangkat ini; pencabutan sesi jarak jauh, AI/Premium, harga pasar langsung serta fitur cloud lain yang belum diimplementasikan tetap tidak diklaim setara dengan Ollo.

Checkpoint 3 November: tour onboarding dan wizard pengaturan awal sudah ditulis serta diuji pada HP Xiaomi. Pemeriksaan pagi mencatat build debug, APK pengujian, 49 unit test dan 20 uji instrumentasi; ronde 2 sore hari yang sama mencatat 50 unit test, 22 uji instrumentasi, validasi nama wajib, dialog mata uang netral dan uji end-to-end nyata mode lokal pada data bersih; ronde 3 pada sore yang sama menyamakan Beranda (header, tab periode, kartu saldo, grid dompet, transaksi per tanggal) dan mencatat 24 uji instrumentasi; ronde 4 menyamakan Statistik (header, tab periode, donut, ringkasan, perbandingan, siklus, peta aktivitas, log transaksi, analisis lanjutan) dan mencatat 26 uji instrumentasi; ronde 5 menyamakan Dompet dan detail dompet (header, kartu total saldo, grid ringkasan, grup dompet, target tabungan, komposisi uang, detail dompet) dan mencatat 29 uji instrumentasi; ronde 6 memverifikasi integrasi Firebase di emulator (Firestore rules 17/17, storage rules 3/3, Cloud Functions 2/2, 40 device test di HP termasuk `CloudModeSmokeDeviceTest`) tanpa mengubah rules/functions produksi; ronde 7 menyamakan Profil (header, metrik, section, tile/row, perbaikan ikon fallback) dan mencatat 42 uji instrumentasi (31 UI lokal + 11 Firebase cloud). [Validasi 3 November](VALIDASI_2026-11-03.md). Formulir/menu keuangan lain, langkah perintah suara, paritas visual penuh, login/akun cloud dari langkah welcome, uji manual pengguna, Premium/AI dan iOS belum selesai.
