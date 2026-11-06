# Mode lokal, kontras, reset data dan peta jalan — 2 November 2026

Pembaruan ini melanjutkan [validasi sebelumnya](VALIDASI_2026-11-02.md). Acuan terbaru adalah UI/menu/font Ollo dengan logo FinFlow; navigasi liquid glass tetap memakai desain FinFlow. Pernyataan lama tentang pengecualian desain Figma/neon tidak menjadi acuan baru.

## Peta jalan di aplikasi

Profil → **Masukan & Peta Jalan** → **Direncanakan / Sedang Dikerjakan / Selesai**.

Ada **60 fitur**: 19 direncanakan, 7 sedang dikerjakan, 34 selesai dalam lingkup yang dijelaskan masing-masing. Setiap fitur mempunyai ID stabil `FF-001`–`FF-060`, bagian, kondisi saat ini dan lingkup/target berikutnya. Pencarian mencakup seluruh rincian dalam tab aktif. Ketuk kartu untuk membuka rincian dan **Salin Prompt**; **Salin Semua** menyalin ketiga tab, termasuk fitur yang tidak terlihat karena pencarian. **Kirim Masukan** membuka formulir draf email yang sudah ada; pengguna sendiri meninjau dan mengirim email.

Sumber daftar adalah `kotlinApp/composeApp/src/commonMain/kotlin/com/logikawan/finflow/feature/settings/ProductRoadmap.kt`. Perbarui status, rincian dan tanggal di file itu ketika pekerjaan berubah. Status merupakan katalog yang disertakan dalam APK, bukan pelacakan pekerjaan server secara otomatis. Jangan menaikkan seluruh modul menjadi selesai hanya karena sebagian alurnya sudah ditulis.

## Perubahan fungsi

- Perbaikan label transaksi terpilih, ikon keypad, pemilih dompet dan berbagai permukaan agar mengikuti warna tema. Uji render mencakup label Pengeluaran, ikon konfirmasi dan isi pemilih dompet pada 10 kombinasi tema/palet.
- Splash memakai logo FinFlow; empat halaman onboarding dan welcome mengikuti susunan Ollo. Mode lokal tidak meminta login. Pengaturan awal menyediakan mata uang, dompet dan saldo awal.
- Repository yang sama dapat memakai Firebase atau snapshot lokal. Penulisan lokal memakai mutex, perubahan atomik dan publikasi setelah native storage berhasil menyimpan. File rusak/versi tidak dikenal tidak diperlakukan sebagai data kosong.
- Android memakai AndroidKeyStore AES-256-GCM dan AtomicFile di `noBackupFilesDir`; iOS memakai file atomik dengan perlindungan perangkat dan pengecualian backup sistem. Batas snapshot lokal 20 MB. Backup JSON tetap ekspor yang dipilih pengguna sendiri.
- Pilihan hubungkan akun menyalin ke akun tujuan kosong, mempertahankan salinan lokal dan menyimpan checkpoint agar proses terputus dapat dilanjutkan. Setelah berhasil, pencatatan memakai akun cloud. Ini belum mirror dua arah dari dua penyimpanan independen atau merge akun yang sudah terisi.
- Foto profil lokal disimpan privat. Foto lokal belum ikut dipindahkan ke cloud dalam alur sinkronisasi ini.
- Selama restore/reset/sinkronisasi tertunda, pencatatan dan jadwal dijeda. Detail dompet/target kembali ke halaman asal; halaman peta jalan dan masukan memiliki jalur Back yang terhubung.
- Hapus Data mempertahankan identitas akun dan membuat ulang data dasar kosong. Penghapusan akun tetap alur terpisah.

## Firebase produksi

Login Firebase CLI sudah tersedia kembali. Deployment berhasil: callable `resetFinancialData` dibuat; `requestAccountDeletion` dan `deleteAccountData` diperbarui; rules Firestore dan Storage diterbitkan. Ketiga fungsi diverifikasi **ACTIVE** di `asia-southeast2` dan rules aktif cocok dengan file lokal. Bucket tidak mempunyai anggota IAM publik. Endpoint reset dan penghapusan menolak permintaan tanpa autentikasi dengan HTTP 401.

Reset memakai autentikasi baru, konfirmasi `HAPUS DATA`, request ID, job pending dan receipt per permintaan. Receipt mencegah retry permintaan lama menghapus data yang dibuat setelah reset berikutnya. Rules memblokir akses ketika reset sedang berlangsung; worker mempertahankan Auth. Penghapusan akun juga membersihkan job/receipt reset agar tidak meninggalkan identitas lama.

Tidak menghapus akun atau data keuangan pengguna di produksi untuk pengujian.

## Hasil pemeriksaan

| Pemeriksaan | Hasil |
|---|---|
| APK debug, APK instrumentation, unit Kotlin | Build berhasil; 35 unit test lulus |
| UI nyata Xiaomi: LocalUiDeviceTest, FinFlowDesignDeviceTest | 17 lulus; termasuk tiga tab, pencarian, clipboard, jalur Masukan/Back, transaksi lokal dan kontras 10 tema/palet |
| PlanningRepositoryDeviceTest dan BackupRestoreDeviceTest | 10 lulus memakai Firebase Emulator melalui secondary FirebaseApp |
| Penyimpanan native pada profil kerja terpisah | 2 lulus dalam dua proses instrumentation: simpan terenkripsi, baca setelah restart proses, freeze, deteksi kerusakan tanpa overwrite, bersihkan fixture |
| Unit Cloud Functions | 10 lulus |
| Rules Firestore / Storage | 17 / 3 lulus di emulator |
| Integrasi hapus akun / reset data | Masing-masing 1 lulus di emulator |
| Produksi | Rules cocok, fungsi aktif, IAM bucket privat, endpoint tanpa autentikasi ditolak |

Instrumentation native hanya dijalankan pada profil kerja ID 10 dan menolak mengganti file lokal yang sudah ada. Dua metodenya dijalankan secara terpisah agar proses benar-benar dimulai ulang. Pada profil utama tes tersebut dilewati. Uji UI menggunakan NativeServices dan repository lokal fixture; uji Firebase memakai project demo, bukan kredensial data produksi.

Kegagalan awal tes telah diperbaiki dan dijalankan ulang: label menu Inggris tertinggal setelah pengindonesiaan; pencarian komponen lazy yang belum masuk viewport; pengukuran kontras memotong teks yang rata kiri; klik fixture dompet tertutup overlay navigasi karena target belum digulir cukup jauh. Hasil terakhir: **27 pengujian UI/repository + 2 native lulus**.

Setelah dua uji native lulus, ditambahkan guard agar keduanya dilewati pada profil utama dan urutan metode eksplisit; build APK tes terbaru berhasil. Pemasangan ulang APK tes terakhir ditolak Android dengan `INSTALL_FAILED_USER_RESTRICTED: Install canceled by user`, sehingga dua uji native tidak diulang setelah perubahan guard tersebut. APK aplikasi utama tidak berubah pada langkah ini dan tetap merupakan revisi yang lulus 27 pengujian. Tidak mencoba ulang atau mengubah pembatasan instalasi HP.

Log lokal berada di `kotlinApp/build/backend-tools/` (`roadmap-final-device.log`, `native-local-reopen.log`, `native-local-create-final-attempt.log`, `second-*.log`); log create awal telah tertimpa saat percobaan pemasangan ulang. Hasil awal kedua native test adalah masing-masing `OK (1 test)` pada dua proses berbeda. Build/unit ada di `kotlinApp/build/roadmap-build.log`. Screenshot referensi dan hasil berada di `kotlinApp/build/ollo-reference/`. Folder build diabaikan Git.

Salinan Ollo dan FinFlow sementara pada profil kerja, APK instrumentation serta reverse port pengujian telah dihapus. Emulator Firebase yang dimulai untuk tugas ini sudah dihentikan. Aplikasi/data utama Ollo tetap ada; APK FinFlow utama terbaru tetap terpasang.

## Batas yang tercatat dalam aplikasi

Kesamaan seluruh halaman/tombol Ollo masih dikerjakan. Onboarding belum mencakup semua langkah bahasa, suara, notifikasi dan gaya Ollo. Mode lokal dan perpindahan cloud masih memerlukan pengujian alur awal/semua modul/kegagalan jaringan. Quick Record chat/suara/scan, reimburse inline, foto target/kartu/lampiran, kredit/paylater, split bill, pencabutan sesi jarak jauh, merge/revisi server, API bank, harga pasar, AI, kolaborasi dan rilis resmi ada pada daftar pekerjaan, bukan diklaim aktif.

iOS belum dibangun atau diuji pada Mac/perangkat Apple. Pengiriman notifikasi sistem dan operasi destruktif produksi tidak diuji. Status **Selesai** di peta jalan berarti lingkup fitur yang tertulis tersedia; bukan pernyataan bahwa seluruh FinFlow sudah identik dengan Ollo.
