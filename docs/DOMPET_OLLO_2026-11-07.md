# Dompet FinFlow = Ollo (7 November 2026)

Acuan: Ollo 1.7.9 di HP Xiaomi (480 dpi), mode gelap. Navigasi bawah FinFlow tetap (keputusan pengguna).

Bukti (kiri Ollo, kanan FinFlow): [01 daftar dompet](ui-evidence/2026-11-07-dompet/01-daftar-ollo-vs-finflow.png), [02 detail dompet](ui-evidence/2026-11-07-dompet/02-detail-ollo-vs-finflow.png).

## Daftar dompet

- Header tetap (sticky): ikon grid (Komposisi Uang) · **Dompet Saya** 20sp · **+** (Tambah Dompet, `wallet-add`). Tombol bawah "+ Tambah Dompet" dihapus seperti Ollo.
- Menu ⋮ (Tambah, Dompet Diarsipkan, Sembunyikan Saldo) pindah ke judul grup pertama, seperti ⋮ di samping "Cash" pada Ollo.
- Kartu saldo memakai gradien Ollo yang sama dengan Beranda (`core/ui/OlloGradient.kt`, dipakai bersama): label 14sp putih 80%, nominal 36–40sp, pil perubahan berbingkai putih + "(+Rp…) 30 hari", 4 kotak kaca (putih 10%, garis putih 25%, label 10sp, nominal 13,5sp).
- **Perbaikan logika**: perubahan 30 hari kini +100% bila saldo 30 hari lalu 0 dan ada arus masuk (sebelumnya +0,0%), sama seperti Ollo.
- Kartu dompet: lingkaran ikon netral 40dp (#393B41), nama 16sp, jenis 12sp, "Saldo Saat Ini" 10sp, nominal 16sp. Grup kosong: lingkaran 46dp redup, teks 14/12sp abu.
- Ikon Hugeicons: DashboardSquare01, PlusSign, Bank, Wallet01/03.

## Detail dompet

- Ikon kembali di semua sub-layar kini chevron (`hi_chevron_left`, label aksesibilitas "Kembali") seperti Ollo.
- Segmen periode: wadah #101014, terpilih #18181D, teks reguler (berlaku untuk semua `SegmentedTabs`).
- Navigasi tanggal 14sp; panah berikutnya redup bila periode berikutnya di masa depan.
- "Saldo Saat Ini" 15sp + nominal 36sp **di tengah**; grafik saldo 84dp dengan label sumbu ringkas (6.6jt), garis grid vertikal, isian gradien, berhenti di hari ini.
- Ringkasan 2×2 (radius 20, label 14sp, nominal 18sp); tombol aksi 62dp dengan ikon panah diagonal merah/hijau dan ⇅ biru, label 12,5sp.
- Transaksi Terbaru memakai baris yang sama dengan Beranda (tanggal + total harian, kartu ikon/judul/dompet/catatan/jam/nominal).

## Verifikasi

- Build debug terpasang di HP; tidak ada crash.
- Uji baru `walletHeaderAddAndHundredPercentChange` (+100,0%, tombol +, Komposisi Uang).
- `LocalUiDeviceTest` diperbarui: tombol + kini membuka panel 5 tombol (sejak 6 Nov), uji mengetuk "Tambah" (`quick-add`) — sebelumnya gagal karena masih mencari "Manual".
- 38/38 uji UI lulus dua kali (Dompet, Statistik, Desain, Beranda, Tambah, Profil, Pemilih mata uang, UI lokal); unit test lulus.

## Perbedaan yang disengaja / belum

- Ikon ⇅ di kartu saldo Ollo diganti ikon mata FinFlow (sembunyikan saldo) karena itulah fungsi yang tersedia di FinFlow.
- Mode terang belum dibandingkan ulang.
