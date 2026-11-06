# Paritas Beranda FinFlow dengan Ollo — 4 November 2026

Putaran lanjutan setelah laporan [detail 3 November](DETAIL_OLLO_2026-11-03.md). Fokus ronde ini: Beranda (level, kalender, likuiditas, menu tiga titik, font, nama), 5 menu awal (Anggaran, Berulang, Target, Tagihan, Utang) dan Laporkan Bug dengan informasi perangkat.

## Acuan dan metode

- Aplikasi acuan: Ollo 1.7.9 terpasang di HP Xiaomi 2311DRK48G (Flutter, font Poppins).
- Screenshot + dump uiautomator: `kotlinApp/build/ollo-reference/` (acuan lama) dan `kotlinApp/build/ollo-capture/` (tangkapan 4 November: form New Budget, menu page 2/3, Profile, Settings).
- Bukti FinFlow sebelumnya: `docs/ui-evidence/2026-11-03-detail/`. Tangkapan app terpasang 4 November: `kotlinApp/build/lead-verifikasi/current/`.
- HP acuan: 1220x2712 px, density 480 dpi, font scale 1.0 → 3 px = 1 dp = 1 sp.
- Skrip pengukur ukuran teks: `kotlinApp/build/ollo-extract/measure_text.py` dan `measure2.py`; keluaran tersimpan di `typography.txt` / `typography2.txt`.
- Font Ollo dipastikan **Poppins** (rasio lebar/tinggi "Report Bug" 5.5614 vs Poppins Medium 5.557; aset res/font & flag widget_use_poppins di APK Ollo), bukan typeface baru.

## Skala tipografi hasil ukur (Poppins, px/3)

| Elemen Ollo | Ukuran |
|---|---|
| Sapaan Beranda "~ Hi, User!" | 20sp SemiBold |
| Judul halaman (Recurring/Bills/Report Bug/Profile) | 20sp SemiBold |
| Judul seksi (Menu, My Wallets, Recent Transactions) | 18sp SemiBold |
| Label menu Beranda / isi form | 14sp Medium |
| "View All" / caption / XP | 12sp SemiBold |
| Pill level "Lvl 1" | 14sp SemiBold, tinggi 40dp, radius 20dp |
| Nominal besar kartu saldo | ±40sp Medium (FitMoneyText di FinFlow) |
| Nominal Pemasukan/Pengeluaran di kartu | 18sp Medium |
| Nomor hari kalender | 12sp |
| Judul tanggal kalender | 15sp Medium |
| Nama halaman level | 24sp Bold; rank 16sp; XP 12sp SemiBold |
| Angka metrik level | 22sp Bold; label 14sp |
| Judul form (New Budget/New Recurring/Add Bill) | 20sp SemiBold |
| Label field form | 16sp Medium |

Token FinFlow (`core/ui/FinFlowTheme.kt`): `FFType.appTitle` 20 SemiBold, `sectionTitle` 18 SemiBold, `bodyMain` 14, `metaData` 12, `badgeLabel` 12 Medium, `amountXl` 40 SemiBold (baru), `amountLg` 28 SemiBold, `amountMd` 18 Medium, `amountSm` 14 Medium. `FitMoneyText` kini auto-shrink hingga 8sp, satu baris, tanpa elipsis.

## Temuan Ollo per butir

1. **Level**: Beranda memakai pill "Lvl N" (bg abu terang, teks 14 Bold) di antara sapaan dan ikon profil; tombol tiga titik di kanan. Halaman level: top bar back + gear, avatar 96dp + cincin progres + pill "Level 1", nama 24 Bold, rank (Rookie Saver), "15 / 500 XP", 6 kartu metrik 2 kolom (Streak, Longest Streak, Points, Active Days, Transactions, Badges) dengan ikon berwarna, kartu Track: Financial Freedom, lalu Achievements (total XP + grup Konsistensi & Streak, Budgeting, Savings Goals dengan badge terbuka berwarna / terkunci abu).
   Leaderboard belum ada di Ollo; keputusan pengguna: global & lokal per negara, bulanan, hadiah Premium dari admin (juara 1 = 3 bulan, 2 = 2 bulan, 3 = 1 bulan, setelah verifikasi). Ditampilkan sebagai kartu "segera hadir" di halaman level.
2. **Kalender**: tab Kalender menampilkan header "3 Nov, 26 Selasa" + panah bulan, 6 baris minggu (Sen..Min), sel putih, tanggal terpilih border hitam tebal + isi abu, badge nominal ringkas (+Rp6.0m / -Rp3.5rb) di dalam sel, privasi saldo ikut, lalu daftar transaksi tanggal terpilih.
3. **Likuiditas**: label kartu "Liquid (IDR) ⇅ 👁". Ikon ⇅ mengganti Total Saldo ↔ Likuid; ikon mata menyembunyikan saldo. Likuid = dompet tunai/bank/e-wallet aktif; tabungan/investasi/kartu kredit tidak dihitung.
4. **Menu tiga titik**: kartu melayang rounded 24 berisi 9 baris: ikon dalam kotak abu 40dp radius 12 + label 18sp; item "What's New" berbadge "New" hitam; titik merah pada ikon tiga titik di header.
5. **Font**: sapaan/heading lebih kecil & lebih tebal dari FinFlow lama; nominal tidak boleh terpotong (auto-shrink).
6. **Nama**: sapaan tidak boleh elipsis; nama panjang boleh membungkus maksimal 2 baris tanpa menabrak pill level.
7. **Menu Beranda**: Ollo menampilkan 4 tile per halaman dengan tombol More di kanan header (halaman 2: Cards/Notes/Reimburse/Asset Management; Debts/Wishlist sedang disembunyikan di pengaturan Ollo). Tile 64dp radius 20, background pastel per menu, ikon outline berwarna, label 14sp. Form Berulang memakai urutan: nominal, jenis, kategori, judul, frekuensi/tanggal, interval, akhir, dompet; pemilih dompet = dialog besar berpencarian + filter jenis + grid/list + saldo.
8. **Laporkan Bug**: top bar back + judul, banner biru "Sudah memeriksa pembaruan?", kartu Informasi Perangkat (Aplikasi/perangkat/OS/bahasa + detail), Fitur yang bermasalah (dropdown berikon), Tingkat Keparahan (Rendah/Sedang/Tinggi/Kritis; Sedang terpilih oranye), Judul Bug, Deskripsi, Langkah Reproduksi + chip Opsional, tombol hitam "Kirim Laporan". Tidak ada logcat mentah; hanya info perangkat.

## Pembagian kerja ronde ini

| Pekerjaan | Pemilik | File utama |
|---|---|---|
| Beranda (header, level, kalender, likuiditas, 3 titik, menu, dompet, transaksi) + halaman level | home-ui | feature/dashboard/DashboardScreen.kt, core/ui/TransactionCalendar.kt, feature/level/*, feature/settings/AccountDataScreen.kt |
| Anggaran + Berulang + pemilih dompet | menu-budget-recurring | feature/budget/*, core/ui/WalletPicker.kt, core/ui/FormComponents.kt, core/ui/MonthField.kt, feature/transaction/RecurringScreen.kt (baru) |
| Laporkan Bug + info perangkat | profile-bug | feature/settings/BugReportForm.kt, feature/settings/ProfileScreen.kt, core/platform/NativeServices.kt, androidMain/MainActivity.kt, iosMain |
| Target + Tagihan + Utang | menu-planning | feature/goal/*, feature/planning/PlanningScreens.kt, feature/planning/FinancialEditors.kt |
| Tipografi global, wiring App.kt/FinFlowEditors.kt, build, uji HP, bukti | Lead | core/ui/FinFlowTheme.kt, core/ui/FitMoneyText.kt, app/App.kt, app/navigation/* |

## Hasil verifikasi 4 November 2026

**Build Android** (dijalankan Lead): `:composeApp:assembleDebug`, `:composeApp:assembleDebugAndroidTest`, `:composeApp:testDebugUnitTest` → **BUILD SUCCESSFUL**. Unit test Kotlin **55 lulus, 0 gagal** dari 12 kelas. APK debug dan APK instrumentasi terpasang di HP Xiaomi 2311DRK48G (Android 16) tanpa menghapus data aplikasi.

**Uji instrumentasi di HP (bukan emulator): 30 test lulus.**
- `DashboardParityDeviceTest` (7): struktur Beranda terang/gelap, tombol ⋮ + panel 9 item, halaman menu berpindah (3 halaman untuk 10 menu), likuiditas lewat ikon ⇅, tab Kalender, nama panjang tanpa elipsis, nominal Rp987.654.321.000,00 tidak terpotong, pemilih dompet & form Berulang.
- `ProfileParityDeviceTest` (4): Profil terang/gelap, halaman Level lengkap (tag level-page/back/settings, metrik, Pencapaian, kartu Peringkat Bulanan + aturan hadiah 3/2/1 bulan), form Laporkan Bug menampilkan Informasi Perangkat.
- `FinFlowDesignDeviceTest` (14), `WalletParityDeviceTest` (3), `StatisticsParityDeviceTest` (2).

**Verifikasi manual aplikasi terpasang** (bukan fixture), dengan data lokal pengguna:
1. Beranda: sapaan "~ Hai, Muhammad Zaenal Arifin!" dua baris tanpa terpotong; pill Lvl 1; titik merah; kartu Total Saldo 40sp dengan ⇅ likuid + ikon mata; sub-kartu Pemasukan/Pengeluaran penuh; menu 4 tile pastel per halaman + More (3 halaman); Anggaran Bulanan; Dompet Saya 2 kolom; Transaksi Terbaru.
2. Likuiditas: ikon ⇅ mengganti label menjadi "Likuid (IDR)" (tabungan dikeluarkan dari angka).
3. Kalender: header "4 Nov, 26 Rabu", sel putih, border hitam tanggal terpilih, badge nominal ringkas "+Rp6.0jt" muat dalam sel, daftar transaksi tanggal terpilih.
4. Panel tiga titik: 9 item berikon, "Pembaruan" + badge New, label tidak terpotong.
5. Level: back + gear berfungsi, cincin progres + pill Level, nama/rank/XP, 6 metrik berwarna, Lacak Kebebasan Finansial, Pencapaian (badge terbuka/terkunci, nama "Anggaran Pertama"/"Tabungan Pertama" terbaca penuh), kartu Peringkat Bulanan dengan hadiah Premium 3/2/1 bulan.
6. Halaman 5 menu: Anggaran (ringkasan, arc gauge, kosong + FAB), form Anggaran Baru (nominal, nama, periode, kategori lingkaran, periode berlaku), Berulang (kalender + total + empty state), form Berulang Baru (nominal, jenis, kategori, judul, frekuensi/tanggal, ulangi setiap, tanggal akhir, dompet, catatan), dialog Pilih Dompet (cari, chip jenis, grid, kartu terpilih), Target, Tagihan, Utang (ringkasan + segmented + empty state + FAB).
7. Laporkan Bug: banner pembaruan, Informasi Perangkat asli — Aplikasi 1.0.0, Xiaomi 2311DRK48G, Android 16 (API 36) build BP2A.250605.031.A3, ABI arm64-v8a, layar 1220×2712 px · 406×904 dp · 3.0×, skala font 1.0, RAM 11323 MB, bahasa ID, zona waktu Asia/Jakarta; checkbox sertakan info perangkat; fitur, tingkat keparahan, judul/deskripsi/langkah, tombol Kirim Laporan (nonaktif sampai judul+deskripsi terisi).

Bukti tangkapan layar (aplikasi nyata + hasil uji): `docs/ui-evidence/2026-11-04/` — 19 gambar lapangan (01–19) dan 6 gambar hasil uji (20–25).

## Checklist permintaan pengguna

| # | Permintaan | Status |
|---|---|---|
| 1 | Level Beranda sama dengan Ollo; halaman level + aturan hadiah leaderboard global/lokal (3/2/1 bulan Premium) | Selesai (UI; backend leaderboard belum) |
| 2 | Kalender Beranda sama dengan Ollo | Selesai |
| 3 | Likuiditas terlihat di kartu total saldo | Selesai (ikon ⇅) |
| 4 | Menu tiga titik di samping level sama dengan Ollo | Selesai |
| 5 | Ukuran font Beranda/menu sama; nominal tidak terpotong | Selesai (skala tipografi + FitMoneyText auto-shrink) |
| 6 | Nama tidak terpotong di sapaan "Halo {user}" | Selesai (2 baris, tanpa elipsis) |
| 7 | Menu Beranda dirapikan; 5 menu (Anggaran, Berulang, Target, Tagihan, Utang) UI+logika+font ala Ollo, termasuk pemilih dompet di Berulang | Selesai (5 menu; gap model dicatat di bawah) |
| 8 | Laporkan Bug persis Ollo + log/informasi perangkat | Selesai (info perangkat asli, tanpa logcat mentah) |

## Batas yang masih berlaku

- Leaderboard global/lokal dan pemberian Premium belum punya backend; halaman level hanya menjelaskan aturan & hadiah.
- Menu Ollo "Debts" tidak punya acuan tangkapan layar (disembunyikan di pengaturan Ollo) — halaman Utang FinFlow dibuat mengikuti pola Bills/Goals Ollo (best effort).
- Transfer berulang, waktu mulai, custom icon dan batas pengulangan Ollo belum masuk model recurring FinFlow; Budget "nominal per periode" juga belum ada di model.
- Dua deviasi kecil yang disengaja: tab Bulanan/Mingguan di Anggaran dan aksi "Proses jadwal"/"Kalender terjadwal" di Berulang adalah fitur FinFlow yang dipertahankan.
- Tema gelap belum punya varian warna pastel tile menu/lencana.
- Nominal ekstrem mengecil sampai 8sp agar tetap utuh — perilaku Ollo pada nominal ekstrem belum bisa diamati.
- iOS tidak dibangun di Windows; implementasi informasi perangkat iOS hanya diperiksa secara kode.
