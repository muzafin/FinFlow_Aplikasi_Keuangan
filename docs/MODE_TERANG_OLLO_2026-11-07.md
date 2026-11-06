# Mode terang FinFlow = Ollo (7 November 2026)

Acuan: Ollo 1.7.9 tema "Light" di HP Xiaomi. Untuk pengambilan gambar, mode gelap sistem HP dan tema Ollo diubah sementara ke terang, lalu **dikembalikan** (sistem: gelap; Ollo: Dark). Tidak ada data yang diubah.

Bukti (kiri Ollo, kanan FinFlow): [Beranda, Dompet, Profil, Tambah](ui-evidence/2026-11-07-terang/01-beranda-dompet-profil-tambah.png), [Anggaran, Tagihan, Statistik](ui-evidence/2026-11-07-terang/02-anggaran-tagihan-statistik.png), [mode gelap setelah perubahan](ui-evidence/2026-11-07-terang/03-gelap-tetap-sama.png).

## Temuan

Sebagian besar perbedaan berasal dari warna tetap yang sebelumnya diukur di mode gelap. Ollo **membalik** warna-warna itu di mode terang: pil terpilih dan tombol simpan menjadi gelap (#1C1E27, teks putih), pil sekunder #E7E8E9, lingkaran ikon #E2E2E3, kartu #F8F8F8.

## Perubahan

- Token tema baru di `FFColors` (otomatis gelap/terang): `pill`/`onPill`, `chip`, `raised`, `iconCircle`, `hairline`, `deep`, `isDark`. Semua warna tetap gelap di 9 file (Beranda, Statistik, Dompet, Profil, Tambah Transaksi, Anggaran, Berulang, menu perencanaan, `SegmentedTabs`) diganti token ini.
- Kartu saldo (Beranda & Dompet) mode terang: gradien biru lembut → toska (#4892DF → #4ED6C5) tanpa sudut gelap, seperti Ollo.
- Kartu Upgrade ke Premium mode terang: #2665EA → #377DF3 → #37AFF6.
- Tombol profil di Beranda tanpa lingkaran di mode terang (seperti Ollo).
- Hasilnya di mode terang: tab Pengeluaran, tombol simpan keypad, tab "Belum Bayar", tombol + mengambang, lencana edit profil menjadi gelap; pil "Rp", pil Lvl, lingkaran ikon menjadi abu terang — sama dengan Ollo.

## Verifikasi

- Build debug terpasang di HP; layar dicek di mode terang lalu mode gelap — mode gelap tidak berubah.
- 46/46 uji UI lulus dua kali; unit test lulus.

## Belum

- Layar "Riwayat Transaksi" (Lihat Semua) masih memakai ikon kembali/filter Material lama; belum dibandingkan dengan Ollo.
