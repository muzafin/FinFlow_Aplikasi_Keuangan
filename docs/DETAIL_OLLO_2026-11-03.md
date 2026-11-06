# Detail Beranda, formulir dan laporan bug — 3 November 2026

Acuan diperiksa langsung pada Ollo 1.7.9 di HP Xiaomi, tanpa menyimpan perubahan atau membuat transaksi. Screenshot/XML acuan berada di `kotlinApp/build/ollo-reference/nov03-*`. FinFlow tetap memakai logo dan navigasi liquid glass miliknya.

## Perubahan

1. **Level:** Beranda dan Profil membaca perhitungan yang sama. Halaman Aktivitas sekarang menampilkan avatar dengan cincin progres, level, XP, streak, streak terpanjang, hari aktif, transaksi dan pencapaian. Catatan terjadwal, tanggal masa depan dan ID ganda tidak memberi poin. Aturan FinFlow saat ini adalah 5 XP per transaksi aktual ditambah bonus pencapaian, dengan 500 XP per level. Ini menggantikan rumus lama satu level per 20 transaksi. Referensi awal Ollo yang diamati menunjukkan Level 1, Rookie Saver, 15/500 XP setelah satu transaksi dan bonus First Log 10 XP; kurva level lanjut dan seluruh katalog pencapaian Ollo belum dibuktikan.
2. **Leaderboard mendatang:** pilihan pengguna adalah global dan lokal **per negara**, **bulanan**. Halaman level menjelaskan hadiah Premium gratis: juara 1 tiga bulan, juara 2 dua bulan, juara 3 satu bulan, diberikan admin setelah verifikasi. Peta jalan FF-061 mencatat kebutuhan ledger server, persetujuan profil publik, penetapan negara, penutupan periode, aturan seri dan pemberian hak akses yang tidak berulang. Belum ada peringkat atau klaim hadiah aktif.
3. **Kalender Beranda:** tab Kalender mengganti kartu saldo/menu/dompet dengan kalender enam minggu, tanda nominal harian per mata uang, navigasi bulan dan daftar seluruh transaksi tanggal terpilih. Privasi saldo diterapkan pada tanda kalender. Mata uang berbeda tidak dijumlahkan menjadi satu angka.
4. **Likuiditas:** mengetuk kartu saldo berganti Total Saldo/Likuid. Ikon mata tetap untuk privasi. Likuid mencakup dompet tunai, bank dan e-wallet yang aktif dan disertakan dalam total; tabungan, investasi dan dompet kredit/pinjaman dipisahkan. Tidak ada konversi mata uang otomatis.
5. **Menu tiga titik di samping level:** Notifikasi, Catatan Pembaruan, Tampilan Tambah Transaksi, Tampilan Kartu, Palet Transaksi, Tampilan Beranda, Format Saldo, Kategori dan Periode Pencatatan, masing-masing menuju halaman FinFlow yang sudah tersedia.
6. **Teks dan nominal:** tetap memakai Poppins. Nominal menengah memakai 18sp/26sp; nominal kecil mempunyai line height 20sp, label formulir/pilihan 14sp. Nominal utama, pemasukan/pengeluaran dan kartu dompet mengukur teks dengan font/style aktual, mengecil dalam ruang yang tersedia, lalu dapat membungkus jika diperlukan. Nama pada Beranda dan Profil tidak lagi dibatasi satu baris dengan elipsis.
7. **Formulir dan recurring:** pemilih dompet bersama berbentuk dialog besar dengan pencarian, filter jenis, grid/daftar, pilihan aktif dan saldo. Digunakan pada recurring, tagihan, utang, pembayaran, bundle, setor/tarik target, transaksi sumber/tujuan, kartu terkait dan pocket. Pembatasan dompet/mata uang setiap formulir tetap berlaku. Pilihan umum berubah dari dropdown menjadi sheet berpencarian untuk daftar panjang. Form recurring menempatkan nominal, jenis, kategori, judul, frekuensi/tanggal, interval, akhir dan dompet dalam susunan acuan. Daftar recurring memiliki kalender pratinjau; tanggal 31 tetap kembali ke tanggal 31 setelah bulan pendek.
8. **Laporkan Bug:** tersedia dari Profil, berisi informasi perangkat yang dapat ditinjau, fitur, keparahan, judul, deskripsi dan langkah reproduksi. Android menyertakan merek/model, OS/API/build, ABI, layar/density, skala font, RAM, bahasa, zona waktu dan versi aplikasi. iOS memiliki implementasi informasi model hardware, OS, layar, ukuran teks, bahasa, zona waktu dan versi. Informasi perangkat dapat dikecualikan. Tombol membuka draf email; pengguna mengirim sendiri. Tidak mengambil logcat mentah, transaksi, saldo, token atau ID perangkat.

## Batas yang masih berlaku

- XP dihitung dari jurnal yang masih ada, bukan ledger permanen. Menghapus transaksi atau memulihkan jurnal dapat mengubah progres. Jangan memakai angka klien ini untuk menentukan hadiah.
- Layanan leaderboard, verifikasi anti-manipulasi, panel admin dan hak Premium belum diaktifkan atau diterbitkan.
- Recurring masih mendukung pemasukan/pengeluaran. Transfer berulang, custom icon, waktu mulai dan batas jumlah pengulangan seperti pada Ollo belum diimplementasikan; backend dan validasi yang ada dipertahankan.
- Penyesuaian komponen bersama tidak berarti seluruh isi setiap menu telah identik dengan Ollo. Audit fitur di FF-037 masih berjalan; kurva level lanjut, seluruh pencapaian dan kesamaan piksel semua layar/font scale belum dibuktikan.
- iOS tidak dibangun di Windows. Pengiriman email nyata dan penerimaan laporan oleh admin belum diuji.

## Verifikasi

- Build Android, unit test dan APK instrumentasi berhasil. Suite unit: **55 lulus, 0 gagal**.
- Pemeriksaan perangkat: **27 lulus, 0 gagal** pada HP Xiaomi, terdiri dari DashboardParityDeviceTest (6), ProfileParityDeviceTest (4), WalletParityDeviceTest (3) dan FinFlowDesignDeviceTest (14). Termasuk nama panjang, nominal Rp987.654.321.000,00 tanpa elipsis, kalender/likuiditas, filter dompet, recurring memilih BCA, halaman level dan formulir bug.
- Setelah perapian label tanggal singkat dan pengambilan screenshot yang menunggu animasi jendela native, suite beranda/profil pada APK terakhir lulus **10/10**; hasilnya dicatat di `kotlinApp/build/home-details-device-final.log`.
- Log build: `kotlinApp/build/home-details-build.log`. Log suite 27 pemeriksaan: `kotlinApp/build/home-details-device.log`. Hasil unit: `kotlinApp/composeApp/build/test-results/testDebugUnitTest/`.
- Pengujian memakai fixture terisolasi dan tidak menghapus data pengguna atau mengubah produksi Firebase. Instalasi APK memakai pembaruan `-r` yang mempertahankan data aplikasi.

Bukti tampilan tersimpan di [folder screenshot](ui-evidence/2026-11-03-detail/): kalender, likuiditas, nama/nominal panjang, pemilih dompet recurring, formulir setelah memilih BCA, level dan laporan bug. Screenshot memakai data contoh; informasi perangkat pada formulir bug dalam fixture memakai layanan native tiruan. Implementasi Android nyata membaca informasi melalui `MainActivity.deviceDiagnostics()`.
