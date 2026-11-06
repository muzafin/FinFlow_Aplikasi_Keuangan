# Profil FinFlow = Ollo (7 November 2026)

Acuan: Ollo 1.7.9 di HP Xiaomi (480 dpi), mode gelap. Navigasi bawah FinFlow tetap (keputusan pengguna).

Bukti (kiri Ollo, kanan FinFlow): [01 atas](ui-evidence/2026-11-07-profil/01-ollo-vs-finflow.png), [02 menu & preferensi](ui-evidence/2026-11-07-profil/02-ollo-vs-finflow.png), [03 lanjutan](ui-evidence/2026-11-07-profil/03-ollo-vs-finflow.png).

## Perubahan

- Header: ikon gerigi & cari Hugeicons (26dp), judul 20sp.
- Kepala profil: avatar 78dp (#18181D, garis #26262A, ikon pengguna garis tipis), lencana edit 26dp abu terang (#E4E4E7) dengan pensil gelap; nama 23sp satu baris; angka 16sp + label 10sp dengan pemisah.
- Kartu **Upgrade ke Premium**: tinggi 115dp, gradien #112155 → #1A48C9 → #0F91E3, pola lingkaran konsentris di kanan, judul 17sp, subjudul 11sp, chevron dalam lingkaran kaca di kanan-bawah (tanpa ikon di kiri, seperti Ollo).
- Keterangan bagian ("Akun & Data", "Preferensi", …) 12sp abu dengan jarak huruf.
- Kotak 2 kolom: tinggi 124dp (sebelumnya 172dp), ikon kotak 40dp #26262A, judul 15sp yang mengecil otomatis agar tidak terpotong, keterangan 12sp maks. 2 baris.
- Baris daftar: ikon lingkaran 40dp, judul 16,5sp, keterangan 12sp, jarak antar-baris 10dp.
- Pemilih palet: pratinjau mini-layar berwarna (bilah aksen + titik warna) seperti Ollo, bukan ikon palet generik.
- Semua ikon menu memakai Hugeicons (UserRound, CloudUpload, Laptop, Download01, Upload01, Calendar04, Tags, Wallet01, Notification01, Moon, CreditCard, LayoutThreeColumn, PaintBoard, SortingAZ01, Home01, Coins01, Brain, Flag01, Shield01, HelpCircle, Mail01, Share08, Bug01, InformationCircle, Clock01, Delete02, Settings01, PencilEdit01).

## Verifikasi

- Build debug terpasang di HP; tidak ada crash.
- Uji baru `everyProfileMenuNavigatesAndPaletteAndSearchWork`: 18 menu Profil diketuk satu per satu dan masing-masing membuka rute yang benar; palet Ruby tersimpan ke preferensi; pencarian "Bug" hanya menyisakan menu yang cocok.
- 39/39 uji UI lulus (Profil, Dompet, Statistik, Desain, Beranda, Tambah, Pemilih mata uang, UI lokal); unit test lulus.

## Perbedaan yang disengaja / belum

- Isi menu tetap fitur FinFlow (mis. tidak ada "Voice Input Language"/"Quick Record Memory" versi Ollo karena fitur itu berbeda di FinFlow); hanya tampilannya yang disamakan.
- Mode terang belum dibandingkan ulang.
