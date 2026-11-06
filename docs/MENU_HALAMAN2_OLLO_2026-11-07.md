# Menu Beranda halaman 2 FinFlow = Ollo (7 November 2026)

Acuan: Ollo 1.7.9 di HP Xiaomi (480 dpi), mode gelap — Debts, Wishlist, My Cards, My Bundles, Reimbursement, Asset Management. Tidak ada data yang dibuat di Ollo maupun FinFlow.

Bukti (kiri Ollo, kanan FinFlow): [Utang/Keinginan/Kartu](ui-evidence/2026-11-07-menu2/01-utang-keinginan-kartu.png), [Catatan/Reimburse/Investasi](ui-evidence/2026-11-07-menu2/02-catatan-reimburse-investasi.png), baris menu [Ollo](ui-evidence/2026-11-07-menu2/03-baris-menu-ollo.png) vs [FinFlow](ui-evidence/2026-11-07-menu2/03-baris-menu-finflow.png).

## Menu di Beranda

- **Temuan**: menu Ollo adalah satu baris yang **digeser** (10 ikon, 4 terlihat), bukan halaman yang diganti lewat ⋮. Mode bawaan FinFlow ("Geser horizontal") kini sama: baris geser, 4 ubin terlihat.
- ⋮ di samping "Menu" kini membuka popup **Tampilan Menu / Susunan Menu** seperti Ollo (sebelumnya mengganti halaman).
- Ikon & warna ubin halaman 2 diambil dari Ollo (warna 18% di atas hitam, diukur): Utang Payment02 ungu #9C27B0, Keinginan Gift pink #E91E63, Kartu CreditCard indigo #5C6BC0, Catatan CheckList teal #009688, Reimburse Refresh oranye #FF9800, Investasi ChartCandle biru #536DFE.

## Per menu

- **Utang**: ringkasan dua kolom "Hutang Saya | Piutang Saya" dengan % terbayar (seperti "I Owe | Owed to Me"); state kosong centang "Kamu bebas hutang!"; judul "Daftar Hutang" hanya saat ada data.
- **Keinginan**: kartu "Total Nilai Keinginan" + pil jumlah; segmen Aktif/Tercapai; state kosong hati + "Mulai tambahkan barang yang ingin kamu beli".
- **Kartu**: tab bergaris-bawah berikon (Bank, Kartu, E-Wallet, Lainnya); state kosong ikon bank.
- **Catatan**: segmen Ollo, kolom cari berikon (muncul bila sudah ada catatan), state kosong tas "Buat bundle pertamamu!", tombol pil **+ Bundle Baru**.
- **Reimburse**: tab bergaris-bawah Menunggu/Diterima, teks kosong di tengah.
- **Investasi** (kosong): kartu "Total Nilai Aset" + kartu ajakan dengan tombol bergaris "Tambah Investasi".
- Tombol bawah lebar diganti tombol **+ mengambang** Ollo di semua menu ini.

## Verifikasi

- Build debug terpasang di HP; semua 6 layar dibuka lewat baris menu yang digeser — tidak ada crash.
- `MenuParityDeviceTest` +2 uji: Utang (tab Piutang, +), Keinginan (Tercapai, +), Kartu (E-Wallet, +), Catatan (tab Catatan, + Bundle Baru), Reimburse (Diterima, + Catat Pengeluaran), Investasi (Tambah Investasi).
- 46/46 uji UI lulus dua kali berturut-turut.
- Uji lama disesuaikan: `DashboardParityDeviceTest` (geser ke Investasi, ⋮ → Susunan Menu), `FinFlowDesignDeviceTest` (mode grid tanpa ⋮ ganti halaman).

## Perbedaan yang disengaja / belum

- Ollo "Asset Management" menampilkan harga pasar saham (fitur Premium Ollo); FinFlow belum punya data harga pasar, jadi tidak ditampilkan.
- Catatan FinFlow tetap punya tab Aktif/Riwayat/Catatan (Ollo hanya bundle) agar catatan teks lama tetap dapat diakses.
- Ikon ⋮/bantuan di header menu halaman 2 milik Ollo tidak ditambahkan karena belum ada aksi yang setara.
