# Sinkronisasi preferensi cloud Kasiva — 8 November 2026

Laporan pengguna: dialog **"Preferensi Belum Tersinkron"** muncul dengan isi **"Perubahan di perangkat tetap disimpan. Failed requirement."**, berulang saat aplikasi dibuka.

## Akar masalah (terbukti)

- Dialog itu berasal dari `App.kt` saat `FinanceRepository.synchronizePreferences()` melempar exception.
- Teks `Failed requirement.` adalah pesan bawaan Kotlin untuk `require(...)` tanpa pesan kustom. Di jalur ini, `AppPreferences.validate()` memanggil `require(...)` untuk setiap field preferensi.
- Data preferensi cloud (`users/{uid}/settings/appearance`) bisa ditulis oleh build/perangkat lain dengan nilai yang tidak lagi valid untuk versi aplikasi sekarang (mis. id tema/palet/gradien, daftar menu, bahasa). Saat dibaca, `validate()` gagal, exception mentahnya ditampilkan ke dialog, dan sinkronisasi berhenti.
- Bukti reproduksi: dokumen preferensi valid disuntik dengan payload tidak valid lewat Firestore REST, lalu aplikasi dibuka → logcat menampilkan `IllegalArgumentException: Failed requirement.` dan dialog muncul. Ini **bukan** masalah Firestore rules/kunci API.

## Perbaikan

`composeApp/src/commonMain/kotlin/com/clonical/kasiva/core/model/AppPreferences.kt`
- Tambah `repaired()`: migrasi nilai preferensi yang tidak valid ke nilai yang masih dikenal/bawaan (urutan menu dipertahankan, menu baru ditambahkan, nilai tak dikenal kembali ke default). Pengaturan yang valid tidak diubah.

`composeApp/src/commonMain/kotlin/com/clonical/kasiva/core/data/FinanceRepository.kt`
- Pembacaan preferensi cloud dibuat toleran: coba validasi ketat; bila gagal, gunakan hasil `repaired()` lalu `validate()`.
- Dokumen yang diperbaiki ditulis kembali ke cloud otomatis (jika tidak ada perubahan lokal tertunda), sehingga perangkat/versi lain tidak mengulang masalah.
- `null` (dokumen benar-benar tidak terbaca/versi tidak didukung) mengganti preferensi cloud dengan pengaturan perangkat ini.

`composeApp/src/commonMain/kotlin/com/clonical/kasiva/app/App.kt`
- Gangguan sementara (jaringan/rules) diulang 3 kali dengan jeda sebelum menampilkan dialog.
- Dialog memakai pesan terjemahan `app_pref_sync_failed` ("Preferensi cloud belum tersinkron"), bukan teks exception mentah.

`composeApp/src/commonTest/kotlin/com/clonical/kasiva/AppPreferencesTest.kt`
- Uji: payload tidak valid diperbaiki tanpa melempar; preferensi valid tidak berubah.

## Verifikasi 8 November 2026

- `:composeApp:testDebugUnitTest --tests "com.clonical.kasiva.AppPreferencesTest"` → 2 test lulus.
- `:composeApp:assembleDebug` → build APK sukses.
- Simulasi dokumen preferensi cloud tidak valid: aplikasi baru dibuka **tanpa dialog**, logcat bersih, dan dokumen cloud otomatis menjadi payload valid (diperiksa lewat Firestore REST). Setelah uji, payload preferensi pengguna yang valid dipulihkan.
- APK dipasang ulang di HP pengujian (data tidak dihapus); Beranda tampil normal.

## Catatan

- Data transaksi, dompet, dan keuangan tidak disentuh oleh migrasi ini; hanya dokumen preferensi tampilan.
- Nilai preferensi yang sudah tidak dikenal akan kembali ke default (mis. tema/palet); nilai yang masih dikenal dipertahankan.
