# Menu keuangan dan navigasi liquid glass — 29 Oktober 2026

Permintaan: lengkapi menu Beranda yang belum berfungsi dan gunakan navigasi liquid glass yang konsisten, termasuk saat membuka Profil. Pengguna mengizinkan pengujian langsung pada device. Blaze dilaporkan sudah aktif oleh pengguna; tidak ada perubahan billing atau layanan berbayar baru dalam pekerjaan ini.

## Implementasi

- Satu `MainNavigation` untuk Beranda, Statistik, Dompet dan Profil. Menggunakan `io.github.kyant0:backdrop:2.0.0` dari [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass): rekaman halaman di belakang navigasi, blur, refraksi, indikator animasi, tekan, geser, pelepasan dan pembatalan. Perangkat tanpa RenderEffect memakai permukaan dengan opasitas lebih tinggi. Kode aplikasi Convx GPL tidak disalin.
- Tagihan: tambah/edit, jatuh tempo, pembayaran, pengulangan mingguan/bulanan/tahunan, riwayat dan arsip yang dapat diaktifkan kembali. Pembayaran berulang memakai tanggal periode sebagai identitas; pembatalan dari pembayaran terbaru.
- Hutang/piutang: pencatatan awal, cicilan parsial, saldo tersisa, histori, pembatalan cicilan, arsip. Pilihan pelacakan saja tidak membuat jurnal dompet. Nominal awal dikunci setelah pencatatan.
- Keinginan: biaya, prioritas, tautan, catatan dan status terpenuhi; tidak memindahkan saldo.
- Kartu: jaringan, nama, empat digit terakhir, masa berlaku opsional, tautan dompet. Tidak ada field nomor kartu lengkap, PIN atau CVV.
- Catatan: tambah/edit/hapus, pencarian dan sematan.
- Reimburse: klaim dari pengeluaran aktual, maksimal nominal pengeluaran, penerimaan satu kali ke dompet bermata uang sama, dan pembatalan. Pengeluaran asal tidak dapat dihapus ketika klaim masih ada.
- Berulang: pemasukan/pengeluaran harian, mingguan, bulanan atau tahunan; interval dan tanggal akhir. Diproses saat aplikasi dibuka, melalui tombol Proses Jadwal, serta Android WorkManager berkoneksi internet setiap enam jam sesuai pembatasan sistem. Maksimal 100 periode per proses; jalankan kembali untuk backlog lebih besar. Tidak menjanjikan pencatatan tepat waktu ketika aplikasi tertutup atau perangkat offline. iOS belum memiliki penjadwal latar belakang untuk fitur ini.
- Kalender transaksi terjadwal manual tetap tersedia dari menu Berulang. Transaksi `scheduled` tidak memengaruhi saldo.

Saldo tetap dihitung dari jurnal integer minor units. Pembuatan/pembatalan pembayaran dan perubahan status disimpan dalam transaksi Firestore yang sama. Transaksi pembayaran dikelola melalui fitur asalnya; edit manual tidak dapat memutus keterkaitan dengan histori.

CSV tetap membawa jurnal saja. Impor ulang ekspor yang berisi pembayaran mengenali ID yang sudah ada. Saat dipulihkan ke data kosong, jurnal pembayaran menjadi transaksi biasa dengan peringatan di pratinjau; definisi tagihan, hutang, klaim dan jadwal tidak direkonstruksi dari CSV.

## Validasi

- Build `:composeApp:assembleDebug` dan `:composeApp:assembleDebugAndroidTest` berhasil. Compile SDK 37 diperlukan dependensi Backdrop; target SDK tetap 36 dan minimum 26.
- 15 unit test: impor/uang 5, parser notifikasi 3, peringatan anggaran 2, validasi perencanaan 5. Termasuk tanggal 31 yang kembali setelah Februari, tahun kabisat, interval, batas nominal, kartu, tanggal dan URL.
- 5 pengujian rules melalui Firebase Emulator: CRUD pemilik, penolakan lintas pengguna/tanpa login, kartu hanya empat digit, batas field/masa berlaku, penolakan koleksi/koneksi bank yang tidak didukung.
- 5 pengujian repository Kotlin dari Xiaomi melalui Auth/Firestore Emulator `demo-finflow`: tagihan atomik/idempoten/pembatalan; cicilan dan penolakan kelebihan bayar; hutang pelacakan; reimburse/mata uang; proses jadwal tepat sekali dan nonaktif. Memakai FirebaseApp sekunder khusus pengujian, bukan instance Firebase produksi.
- 9 pengujian UI Xiaomi 2311DRK48G: onboarding, autentikasi, empat halaman utama, panel transaksi, anggaran/target/kalender, detail transaksi, drag/cancel glass, tujuh menu perencanaan, serta validasi formulir kartu/cicilan. Screenshot menggunakan fixture terpisah dan tidak menulis transaksi produksi.
- Rules diterbitkan ke `finflow-logikawan` pada 29 Oktober 2026 14:42 WIB. Isi editor dicocokkan dengan berkas lokal yang diuji, lalu Console memperlihatkan versi aktif bertanda bintang. [Bukti publikasi](firebase-evidence/firestore-planning-rules-2026-10-29.png).
- Setelah publikasi, aplikasi produksi pada device berhasil memuat Beranda, halaman Tagihan dan formulir Tambah Tagihan; tombol tambah aktif tanpa error izin. Formulir ditutup tanpa menyimpan. APK debug terakhir tetap terpasang; APK pengujian, koneksi adb reverse dan proses emulator sudah dibersihkan.

Log dan screenshot lokal berada di `kotlinApp/build/liquid-work/` (diabaikan Git). Perintah perangkat memakai APK aplikasi dan APK pengujian hasil build yang sama. Debug hanya mengizinkan HTTP loopback untuk Firebase Emulator; release tetap tidak membuka cleartext atau screenshot data aplikasi.

## Menjalankan ulang pengujian backend

Jalankan Firebase Auth Emulator pada `127.0.0.1:9098` dan Firestore Emulator pada `127.0.0.1:8088`, project `demo-finflow`, memakai `kotlinApp/firebase/firestore.rules`. Dari folder `firebase/tests`, jalankan `npm install` kemudian `npm test` (Node 22+). Uji device memerlukan `adb reverse tcp:8088 tcp:8088` dan `adb reverse tcp:9098 tcp:9098`, lalu jalankan kelas `PlanningRepositoryDeviceTest` menggunakan AndroidJUnitRunner. Jangan arahkan pengujian ke project produksi.

iOS belum dibangun atau diuji karena pekerjaan berlangsung di Windows. Login/Google Sign-In, notifikasi sistem, bank otomatis, Premium, foto profil dan hapus akun bukan cakupan validasi ini. Tidak ada transaksi uji yang disisipkan ke produksi.
