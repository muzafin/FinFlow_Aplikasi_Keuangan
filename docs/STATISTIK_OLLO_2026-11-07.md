# Statistik FinFlow = Ollo (7 November 2026)

Acuan: Ollo 1.7.9 di HP Xiaomi (480 dpi), mode gelap, tab Pemasukan & Pengeluaran, periode Bulanan. Navigasi bawah FinFlow tetap (keputusan pengguna).

Bukti (kiri Ollo, kanan FinFlow): [01 atas](ui-evidence/2026-11-07-statistik/01-ollo-vs-finflow.png), [02 ringkasan & perbandingan](ui-evidence/2026-11-07-statistik/02-ollo-vs-finflow.png), [03 heatmap & sumber](ui-evidence/2026-11-07-statistik/03-ollo-vs-finflow.png).

## Susunan baru (mengikuti Ollo)

1. Header tetap: Filter · **Statistik** (20sp) · Info · menu ⋮ (Ekspor, Urutan Bagian, Preferensi).
2. Segmen periode **Mingguan / Bulanan / Tahunan / Rentang** (12sp, wadah #18181D, terpilih #101014). Saat digulir, berubah menjadi pil "Bulanan ▾" + bulan + panah (seperti Ollo).
3. Navigasi bulan: `<  November 2026 (18sp)  >` dengan rentang "(1 Nov - 30 Nov)" 10sp.
4. Segmen **Pemasukan / Pengeluaran** (15sp, terpilih diwarnai 12%).
5. Donut tanpa kartu (190dp, cincin 26dp) + legenda 10sp; daftar kategori dalam kartu (ikon 40dp, bilah 100×8dp, % 14sp, nominal 12sp). Kosong → banner "Tidak ada data untuk periode ini".
6. **Ringkasan Harian** tanpa kartu: batang harian yang dapat digeser, garis rata-rata putus-putus, legenda Pemasukan/Pengeluaran/Target. "Rata Tabungan" = setoran Target (seperti "Avg Sav" Ollo), bukan selisih.
7. Kartu (#101014, radius 20) sesuai urutan bagian pengguna:
   - **Perbandingan Bulanan**: lencana %, kalimat, tombol "Bandingkan" (menampilkan garis periode lalu), grafik kumulatif terisi + garis putus setelah hari ini, sumbu "6.5jt".
   - **Tren Saldo Bersih**: lencana nominal, "Saldo Bersih", Bandingkan, grafik.
   - **Rata-rata Harian | Proyeksi Total** (20sp) + banner info biru; lalu **Peta Aktivitas** satu periode penuh (kotak tanggal, teal #00897B, nominal ringkas, legenda Sedikit–Banyak).
   - **Sumber Teratas / Pengeluaran Teratas** dengan dropdown Kategori/Dompet dan Top 5/10/Semua.
   - **Log Transaksi**: Lihat Semua, kolom cari, dropdown Semua/Pemasukan/Pengeluaran/Transfer, dropdown dompet, baris "dd/MM · ikon · judul · +Rp6.0jt", tombol "Lihat Semua Transaksi".
8. **Analisis Lanjutan**: skor kesehatan dari 8 minggu lengkap terakhir (butuh ≥3 minggu berdata), rata-rata per kategori Harian/Mingguan/Bulanan, tabel arus kas 12 bulan, **Kalkulator keuangan** (target tabungan & cicilan pinjaman).

Ikon baru dari Hugeicons: Filter, InformationCircle, panah, ChartIncrease/Decrease, Search01.

## Verifikasi

- Build debug terpasang di HP; tidak ada crash saat menggulir seluruh halaman (crash `IntrinsicSize` + `BoxWithConstraints` ditemukan dan diperbaiki).
- Uji baru `statisticsOlloControlsWork`: ganti Pemasukan/Pengeluaran (total berubah), Bandingkan, pencarian log, kalkulator cicilan (12.000 / 12 bulan = 1.000).
- 34/34 uji UI lulus dua kali (Statistik, Desain, Beranda, Tambah, Dompet, Profil, Pemilih mata uang); unit test lulus. Uji lama disesuaikan: label "Total Pengeluaran" diganti segmen tipe; indeks gulir log 5 → 4.

## Perbedaan yang disengaja / belum

- Ollo menampilkan banner "No data for this period" walau ada data (bug Ollo) — tidak ditiru.
- Nominal di tabel arus kas memakai format ringkas FinFlow ("Rp6.0jt"), Ollo mencampur "Rp6M"/"Rp0.00".
- Mode terang Statistik belum dibandingkan ulang.
