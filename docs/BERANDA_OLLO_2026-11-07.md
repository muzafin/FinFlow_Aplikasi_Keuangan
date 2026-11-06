# Beranda FinFlow = Ollo (7 November 2026)

Acuan: Ollo 1.7.9 di HP Xiaomi (1220×2712, 480 dpi, font_scale 1.0), mode gelap. Navigasi bawah FinFlow **sengaja tidak diubah** (keputusan pengguna: pembeda FinFlow dan Ollo).

Bukti: [01](ui-evidence/2026-11-07-beranda/01-ollo-vs-finflow.png), [02](ui-evidence/2026-11-07-beranda/02-ollo-vs-finflow.png) (kiri Ollo, kanan FinFlow).

## Temuan dari APK Ollo (dibaca, tidak disalin)

- **Font**: Ollo memakai **Plus Jakarta Sans** (Google Fonts `plusJakartaSansTextTheme`), bukan Poppins. FinFlow kini membundel Plus Jakarta Sans 4 bobot (lisensi OFL, `kotlinApp/licenses/PlusJakartaSans-OFL.txt`) dan memakainya untuk seluruh aplikasi.
- **Ikon**: Ollo memakai paket **Hugeicons** (stroke 1.5, ujung bulat). FinFlow memakai `@hugeicons/core-free-icons` 4.4.0 (MIT, `kotlinApp/licenses/hugeicons-LICENSE.txt`) lewat `core/ui/HugeIcon.kt` + `HugeIconData.kt` (nama `hi_*` di `FFIcon`). Ikon ArrowUpDown, View/ViewOff, UserRound, PlusSignCircle, Rotate01 terbukti identik dengan path di binary Ollo. Ikon menu Ollo (persen, celengan ber-koin, dokumen) berasal dari versi Hugeicons Flutter yang sedikit berbeda; FinFlow memakai padanan terdekat dari set yang sama (PercentCircle, PiggyBank, File02).
- Aset gambar Ollo (latar kartu, maskot) **tidak** dipakai; gradien kartu dibuat ulang dari warna piksel.

## Nilai yang diukur dan diterapkan

| Elemen | Ollo (diukur) | FinFlow |
|---|---|---|
| Latar / kartu / pil / lingkaran ikon | #000000 / #101014 / #171718 / #252529 | sama (tema gelap global) |
| Teks sekunder | #A1A1AA | sama |
| Sapaan | 20sp SemiBold, 1 baris | sama; nama sangat panjang mengecil lalu 2 baris (tidak terpotong) |
| Pil Lvl | 26dp, 12sp Bold | sama |
| Tombol profil | lingkaran 40dp #101013 | sama |
| Header | tetap di atas + garis pemisah | sama |
| Tab periode | 40dp, 12sp, pil #E4E4E7 teks #18181B | sama |
| Kartu saldo | radius 24, gradien hitam→biru→toska | sama (preset "teal" kini statis seperti Ollo; preset lain tetap beranimasi) |
| Label/ikon kartu | 14sp putih 80%, ikon 20dp | sama |
| Pemasukan/Pengeluaran | label 12sp, nominal 16.5sp | sama |
| Judul seksi | 18sp Medium | sama |
| Ubin menu | 56dp radius 16, warna 18% di atas hitam, label 12sp | sama (#2E1B00 dst.) |
| Dompet | lingkaran 33dp, nama 14.5sp, CASH•IDR 10.5sp | sama |
| Transaksi | tanggal 14sp, total 17sp, baris: judul 13.5sp, sub/catatan/jam 10.5sp | sama; ikon pemasukan hijau |

## Verifikasi

- Build debug terpasang di HP (data pengguna tidak dihapus).
- Unit test Kotlin lulus; uji instrumentasi UI 33/33 lulus dua kali berturut-turut (Beranda, Desain, Tambah, Statistik, Dompet, Profil, Pemilih mata uang). Uji `FinFlowDesignDeviceTest` disesuaikan: animasi gradien diuji pada preset "sunset" karena "teal" kini statis.
- Build di Windows memerlukan `TEMP`/`TMP` dan `-Djdk.net.unixdomain.tmpdir` diarahkan ke `kotlinApp/build/jtmp` (tanpa itu Gradle gagal "Unable to establish loopback connection").

## Belum beres

- ~~Dialog "Preferensi Belum Tersinkron · PERMISSION_DENIED"~~ — **selesai 7 November**: atas persetujuan pengguna, `firebase deploy --only firestore:rules --project finflow-logikawan` dijalankan (hanya rules Firestore; functions/storage tidak disentuh). Setelah aplikasi dibuka ulang di HP, dialog tidak muncul lagi.
- Mode terang Beranda belum dibandingkan ulang dengan Ollo terang.
- Perubahan font & warna gelap berlaku global; layar lain belum dibandingkan ulang satu per satu.
