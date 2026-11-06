# Riwayat Transaksi FinFlow = Ollo "Transaction Log" (7 November 2026)

Acuan: Ollo 1.7.9 di HP Xiaomi, mode gelap, layar "View All" → Transaction Log.

Bukti (kiri Ollo, kanan FinFlow): [riwayat](ui-evidence/2026-11-07-riwayat/01-riwayat-ollo-vs-finflow.png).

## Perubahan

- Latar halaman `surface` (#101014 gelap), header ‹ · **Riwayat Transaksi** 20sp · ⋮ (Atur ulang filter).
- Kolom **Cari transaksi...** selalu terlihat (latar `background`, ikon Hugeicons), mencari judul, catatan, tag, dompet, kategori.
- Tujuh filter dropdown bergaris (38dp, 10,5sp) seperti Ollo, sebelumnya disembunyikan dalam dialog:
  periode (Semua Waktu / 12 bulan terakhir), jenis (Semua, Pengeluaran, Pemasukan, Transfer, Terjadwal), kategori, urutan (Terbaru, Terlama, Terbesar, Terkecil), dompet, rentang nominal (< 100rb, 100rb–1jt, > 1jt), tag.
- Baris transaksi **datar** (tanpa kartu): ikon kategori 40dp, judul 14sp, "Kategori • Dompet" 11sp, nominal ringkas berwarna (+Rp6.0jt), jam + tanggal 10,5sp. Urutan nominal menampilkan daftar tanpa pengelompokan tanggal.
- `TransactionCard` (dipakai bersama) ikut bergaya baris Ollo.

## Verifikasi

- Build debug terpasang di HP; tidak ada crash.
- Uji baru `TransactionLogParityDeviceTest`: cari, filter jenis, filter tag, atur ulang lewat ⋮, urutan nominal terkecil, ketuk baris membuka detail (data terisolasi).
- 47/47 uji UI lulus dua kali.

## Fitur baru dibanding sebelumnya

Filter kategori, dompet, rentang nominal, tag, dan urutan sebelumnya tidak ada di FinFlow; kini tersedia seperti Ollo.
