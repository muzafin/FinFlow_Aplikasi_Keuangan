# XP & Level, Peringkat Bulanan, Split Bill — 11 November 2026

Keputusan pemilik (11 Nov): nama peringkat **disamarkan, ikut otomatis (bisa keluar)**; XP **dihitung server dengan batas**; Split Bill **bagian saya = pengeluaran, bagian teman = piutang**; teks hadiah Premium **dihapus dulu** (Premium belum aktif).

## 1. XP & Level (server)

`firebase/functions/gamification.js` (test: `test/gamification.test.js`, 6 uji; suite functions 47/47 lulus).

| Aturan | Nilai |
|---|---|
| XP per transaksi | +5, hanya `source` manual/notification dan bukan terjadwal |
| Batas harian | 20 transaksi ber-XP per hari (zona Asia/Jakarta, waktu server saat dokumen dibuat) |
| Bonus harian | +10 untuk catatan pertama setiap hari |
| Tidak memberi XP | impor CSV (ivy/finflow), berulang, hutang/tagihan/target otomatis, dokumen ber-`createdAt` > 2 hari (pulihkan cadangan) |
| Hapus transaksi | XP tidak berkurang; ledger `xpLedger/{txId}` mencegah dobel saat trigger diulang |
| Kurva level | total XP untuk level L = 50·L·(L−1) → Lvl 2 = 100, Lvl 3 = 300, Lvl 4 = 600, Lvl 5 = 1000 |
| Akun lama | statistik dibangun ulang dari riwayat transaksi saat status/trigger pertama kali dipanggil |

Di aplikasi, level = XP server + XP lencana (Catatan Pertama, Streak, Anggaran/Tabungan Pertama). Mode lokal tetap dihitung di perangkat (tanpa peringkat). Sebelumnya 500 XP per level sehingga hampir semua pengguna tertahan di Lvl 1.

Functions (Node 22, `asia-southeast2`, di-deploy 11 Nov): `awardTransactionXp` (trigger Firestore onCreate `users/{uid}/transactions/{txId}`), `gamificationStatus`, `monthlyLeaderboard`, `setLeaderboardVisibility`; `deleteAccountData` diperbarui agar menghapus baris peringkat 24 bulan terakhir.

Data: `users/{uid}/gamification/stats`, `users/{uid}/xpLedger/*`, `leaderboards/{yyyy-MM}/entries/{uid}` — semuanya hanya Admin SDK (rules tidak diubah; klien membaca lewat callable).

## 2. Peringkat Bulanan

Halaman Level (ketuk pil "Lvl" di Beranda) → kartu **Peringkat Bulanan**: 50 teratas bulan berjalan (nama disamarkan "Muhammad Z.", XP, jumlah transaksi, hari aktif), baris "Kamu" bila di luar 50 besar, jumlah peserta, counter "Hari ini x/20 transaksi mendapat XP", tombol muat ulang, sakelar **Tampilkan saya di peringkat**. Nominal dan isi transaksi tidak pernah dikirim ke peringkat.

Untuk melihat siapa yang paling rajin mencatat sebagai admin: Firestore Console → `leaderboards/2026-11/entries` (urutkan `xp`). Nama di sana juga samaran; UID dokumen = UID akun.

## 3. Split Bill

Hutang/Piutang → tombol **Split Bill** (di samping +). Isi nama tagihan, total, kategori, dompet, tanggal, lalu orang-orang + bagiannya (tombol **Bagi rata** membagi termasuk sisa pembulatan; "Saya ikut menanggung bagian" bisa dimatikan bila hanya membayarkan orang lain). Saat disimpan:
- bagian saya → transaksi **pengeluaran** biasa (ikut statistik & XP),
- bagian tiap teman → **Piutang** (`lent`, memengaruhi saldo) jatuh tempo sesuai isian; ditandai lunas lewat "Terima piutang".

Saldo dompet berkurang sebesar total. Catatan: perilaku Piutang yang sudah ada mencatat pemberian piutang sebagai pengeluaran "lainnya" dan pelunasan sebagai pemasukan "lainnya"; Split Bill mengikuti perilaku itu. Penyimpanan berurutan (piutang dulu, lalu pengeluaran); bila jaringan putus di tengah, sebagian bisa sudah tersimpan — periksa daftar Piutang.

Unit test: `SplitBillTest` (5 uji). Seluruh unit test Kotlin 95/95 lulus.

## 4. Verifikasi di HP (POCO 2311DRK48G, debug 1.0.4)

- Halaman Level: "Level 1 · 0 / 100 XP", kartu peringkat memuat "November 2026 · 0 peserta" dan sakelar aktif.
- Temuan: panggilan pertama ~40 detik setelah deploy ditolak Cloud Run (izin invoker belum tersebar) → muncul "UNAUTHENTICATED"; setelah itu normal. Tidak perlu perubahan kode.
- Belum diuji di HP: pemberian XP setelah mencatat transaksi baru dan alur simpan Split Bill (tidak membuat data uji di akun nyata tanpa izin).

## 5. Sisa

- Peringkat per negara, penutupan periode bulanan, hadiah Premium (setelah produk Play aktif).
- Pintasan Split Bill di Beranda / form transaksi.

## 6. Lanjutan 11 Nov sore — bahasa awam, cincin level, panduan pertama kali

- **Copywriting tanpa istilah teknis**: kata "server", "cloud", "model AI" dan kode error mentah (mis. UNAUTHENTICATED) dihapus dari teks yang dilihat pengguna (ID & EN penuh; teks peringkat/pengingat tagihan di 9 bahasa). Contoh: "XP dihitung server" → "Kumpulkan XP: +5 setiap mencatat transaksi…"; galat peringkat → "Peringkat belum bisa dimuat. Periksa internet, lalu ketuk muat ulang."
- **Cincin progres level** mengelilingi ikon profil di Beranda (hijau toska, animasi), menunjukkan XP menuju level berikutnya.
- **Panduan pertama kali (coach mark)** di Beranda, 5 langkah: tombol +, kartu saldo, menu keuangan, level/XP, navigasi bawah. Layar diredupkan, elemen yang dijelaskan diberi lubang + garis sorot berdenyut, kartu penjelasan dengan anak panah, tombol Lewati/Kembali/Lanjut/Mulai pakai. Muncul sekali per perangkat (setting `coach.home.v1`), bisa diulang dari menu titik tiga Beranda → "Ulangi panduan aplikasi". Overlay digambar di jendela Popup agar selalu di atas bilah kaca navigasi.
- Diuji di HP: kelima langkah tampil benar, status "selesai" tersimpan. Perbaikan saat uji: crash frame pertama (ukuran overlay 0) dan bilah navigasi yang tidak ikut redup.
- **Desain panduan disamakan dengan tur onboarding**: aksen hijau toska `#009688` (juga dipakai cincin level), titik progres, tombol pil "Lanjut ›", "Lewati" di pojok; kartu mengikuti tema (putih di mode terang, abu gelap di mode gelap); tombol kembali berupa ikon agar muat di semua bahasa.
- **Semua bahasa**: `TranslationCoverageTest` memastikan teks panduan/peringkat/Split Bill/pengingat tagihan ada di 9 bahasa dengan placeholder sama dan tanpa istilah teknis (97 unit test lulus). Diuji visual di HP dengan bahasa per-aplikasi Jerman (langkah 1) dan Hindi (langkah 1 & 5); bahasa aplikasi di HP dikembalikan ke bawaan sistem setelah uji.
- AAB rilis 1.0.4 (vc 5) dibangun ulang: SHA-256 `4119F5F6…1B1AB07A`.
