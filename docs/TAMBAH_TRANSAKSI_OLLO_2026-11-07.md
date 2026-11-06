# Tambah Transaksi FinFlow = Ollo (7 November 2026)

Acuan: Ollo 1.7.9 di HP Xiaomi (480 dpi), mode gelap. Tidak ada transaksi yang disimpan di Ollo maupun FinFlow saat pengambilan gambar.

Bukti: [01 form (kiri Ollo, kanan FinFlow)](ui-evidence/2026-11-07-tambah/01-form-ollo-vs-finflow.png), [02 panel tombol + Ollo (acuan ikon)](ui-evidence/2026-11-07-tambah/02-panel-ollo-acuan.png).

## Form

- Tab **Pengeluaran / Pemasukan / Transfer** bergaya segmen di kiri (wadah #101014, terpilih #E4E4E7 teks gelap, 13,5sp) + tombol **kamera** di kanan (48dp) yang membuka AI Scan (hanya untuk transaksi baru).
- Kategori: lingkaran 56dp; belum dipilih = warna 10%, **terpilih = diisi penuh dengan ikon putih**; label 10,5sp.
- Subkategori: chip 46dp (#18181D, ikon + teks 12sp), terpilih #E4E4E7; ketuk lagi untuk membatalkan (chip "Semua" dihapus seperti Ollo).
- Nominal: pil mata uang "Rp ▾" (#171717, membuka pemilih dompet karena mata uang mengikuti dompet), angka 44sp, "Masukkan jumlah" 12sp.
- Judul (ikon "Aa") + **tombol tag** 56dp di sampingnya (lencana jumlah tag); catatan + **dropdown dompet berbingkai** 140dp. Baris "Tambah tag" & tombol "Kalkulator" teks dihapus.
- Keypad Ollo: tombol membulat (radius 18, jarak 8dp, #101014), angka 20sp, ⌫ merah 10% (**tahan untuk mengosongkan**), tombol **kalkulator**, tombol **tanggal + jam** ("7 Nov / 02:58"), simpan #E4E4E7 dengan centang gelap.

## Panel tombol +

- Ikon Hugeicons sama dengan Ollo: Chatting01 (Asisten), AudioWave01 (AI Voice), BarcodeScan (AI Scan), Message01 (AI Chat), PlusSign (Tambah); label abu 12sp.

## Verifikasi

- Build debug terpasang di HP; tidak ada crash.
- Uji baru `TransactionEditorParityDeviceTest`: kamera memanggil AI Scan; tab tetap "selected"; chip subkategori pilih/batal; tahan ⌫ mengosongkan nominal; kalkulator 20000+5000 → 25.000; tag "kantor" ditambahkan; simpan menghasilkan transaksi Rp25.000 kategori food/subkategori food-dinner dengan tag dan dompet benar (data terisolasi).
- 40/40 uji UI lulus dua kali; unit test lulus.

## Perbedaan yang disengaja / belum

- Chip "Reimburse / Split Bill / CC/Paylater" Ollo tidak ditambahkan: di FinFlow fitur ini ada sebagai menu tersendiri (Reimburse, Keinginan/Bundle, Kartu).
- Ikon kategori bawaan masih ikon Material milik FinFlow (bisa diganti pengguna); belum dipetakan ke ikon Hugeicons Ollo.
- Panel tombol + di HP pengguna tidak difoto karena pengaturan pengguna "langsung buka tambah transaksi" sengaja tidak diubah; panel diuji lewat `LocalUiDeviceTest`.
