# Menu Beranda FinFlow = Ollo (7 November 2026)

Acuan: Ollo 1.7.9 di HP Xiaomi (480 dpi), mode gelap — Budgets, Recurring, Goals, Bills. Tidak ada data yang dibuat di Ollo maupun FinFlow.

Bukti (kiri Ollo, kanan FinFlow): [Anggaran](ui-evidence/2026-11-07-menu/01-anggaran-ollo-vs-finflow.png), [Berulang](ui-evidence/2026-11-07-menu/02-berulang-ollo-vs-finflow.png), [Target](ui-evidence/2026-11-07-menu/03-target-ollo-vs-finflow.png), [Tagihan](ui-evidence/2026-11-07-menu/04-tagihan-ollo-vs-finflow.png), [menu ⋮ di header](ui-evidence/2026-11-07-menu/05-anggaran-menu-header.png).

## Perubahan bersama

- **Header menu** (Anggaran, Berulang, Target, Tagihan, Utang, Keinginan, Kartu, Catatan, Reimburse, Investasi): latar #101014 seperti Ollo.
- **Slot aksi header** baru (`core/ui/TopBarActions.kt`): layar menaruh ⋮ di header App seperti Ollo. Tanpa App (mis. uji), aksi tetap tampil di dalam layar.
- Komponen Ollo bersama (dipakai semua menu): segmen (#101014, terpilih #E4E4E7 teks gelap reguler), tombol + mengambang (#E4E4E7 radius 16, + gelap), state kosong (lingkaran 96dp #171717, judul 18sp, pesan 14sp, tip berbingkai dengan lampu kuning), tombol muat ulang Hugeicons.

## Per menu

- **Anggaran**: tab Bulanan/Mingguan dihapus → pilihan "Anggaran Bulanan / Mingguan" di ⋮ header. Ukuran Ollo: label 12sp, sisa 18sp, rentang 10sp, gauge lebih kecil dengan 29sp, "Terpakai" 12sp, Rp/Rp 18sp; status dengan ikon centang-lingkaran; state kosong ikon gambar + "Buat Anggaran".
- **Berulang**: tanggal di kiri-atas sel (10,7sp), hari di luar bulan tetap bersel redup, ringkasan dalam kartu #18181D, tombol "+ Tambah Berulang" pil #E4E4E7. Tautan "Proses jadwal / Kalender terjadwal" (khusus FinFlow) pindah ke ⋮ header.
- **Target**: tab Aktif/Arsip dihapus → "Tampilkan Arsip" di ⋮ header; ringkasan "Total Tabungan" 12sp + 33sp, Terkumpul/Sisa Target 10,5/12sp; state kosong ikon celengan seperti Ollo.
- **Tagihan** & **Utang**: ⋮ yang sebelumnya di bawah header kini di header; state kosong & segmen ikut gaya Ollo.

## Verifikasi

- Build debug terpasang di HP; menu ⋮ di header App dicoba langsung di HP (Anggaran) dan terbuka.
- Uji baru `MenuParityDeviceTest` (4): Anggaran pindah ke mingguan lewat ⋮ dan tombol + bekerja; Target menampilkan arsip lewat ⋮ dan tombol + bekerja; Tagihan tab Lunas terpilih, ⋮ "Tampilkan Arsip", tombol + bekerja; Berulang ⋮ "Proses jadwal" & "Kalender terjadwal" serta "Tambah Berulang" bekerja.
- `FinFlowDesignDeviceTest` diperbarui ("Dana di dompet tabungan" → "Total Tabungan").
- 44/44 uji UI lulus dua kali berturut-turut; unit test lulus.

## Belum

- Menu halaman 2 (Utang, Keinginan, Kartu, Catatan, Reimburse, Investasi) sudah ikut komponen bersama, tetapi belum dibandingkan satu per satu dengan Ollo.
- Mode terang belum dibandingkan ulang.
