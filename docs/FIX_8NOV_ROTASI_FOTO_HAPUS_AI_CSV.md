# Perbaikan 8 November 2026 (rotasi, foto profil, hapus data offline, AI CSV)

Laporan pengguna setelah uji lapangan:

## 1. Rotasi layar memuat ulang "menyiapkan data akun"

- Penyebab: MainActivity dibuat ulang saat konfigurasi berubah (orientasi), sehingga seluruh state/repository direset dan data dimuat lagi.
- Perbaikan: `AndroidManifest.xml` activity memakai
  `android:configChanges="orientation|screenSize|smallestScreenSize|screenLayout|keyboardHidden|uiMode|density|fontScale|locale|layoutDirection"`.
  Activity tidak dibuat ulang; Compose tetap menyesuaikan ukuran.
- Verifikasi di HP: layar diputar (user_rotation 1 → 0) — halaman tetap, tidak muncul status memuat ulang.

## 2. Foto profil tidak bisa diunggah

- Penyebab paling mungkin: foto dari kamera/galeri sering >5 MB; alur lama menolak file >5 MB sebelum dikompres.
- Perbaikan (`ProfilePhotos.kt`): batas baca mentah dinaikkan ke **25 MB** (sama dengan jalur foto struk AI), tetap dikompres ke JPEG ≤1024 px dan ≤2 MB sebelum upload. Pesan batas memakai string 25 MB.
- Diagnostik: kegagalan upload Storage sekarang ditulis ke logcat tag **`KasivaProfilePhoto`** dengan exception, sehingga penyebab sebenarnya (aturan Storage/jaringan/content-type) bisa dilihat bila masih gagal. Aturan Storage tetap: JPEG ≤2 MB, hanya pemilik `users/{uid}/profile/avatar.jpg`.
- Bila masih gagal: kirim logcat `adb logcat -s KasivaProfilePhoto` setelah mencoba upload.

## 3. Hapus data saat offline: disuruh login lalu error & lama

- Penyebab: aksi hapus di mode cloud memakai re-autentikasi + Cloud Function (butuh internet, timeout menit). Saat offline pengguna sudah diminta login/password lebih dulu, lalu gagal setelah menunggu.
- Perbaikan (`App.kt`): untuk **mode cloud saat offline**, ketiga aksi berikut langsung menampilkan pesan `core_error_need_internet` tanpa dialog login dan tanpa memanggil server:
  - Minta penghapusan akun,
  - Reset pabrik (`factoryReset`),
  - Hapus riwayat transaksi saja (`deleteTransactions`).
- Mode lokal tetap bisa menghapus data sepenuhnya offline. Hapus akun memang memerlukan internet (data ada di server) — sekarang pesannya jelas dan instan.

## 4. AI CSV import: consent lalu tidak terjadi apa-apa

- Penyebab: alur memanggil `ai.run(...)` di dalam callback `requireConsent` yang masih berjalan di dalam `ai.run` lain; `ai.run` menolak karena `working != null`, sehingga pemetaan AI tidak pernah dijalankan.
- Perbaikan (`App.kt`): setelah consent, pekerjaan dijalankan lewat `scope.launch` (busy/error sendiri) sehingga `CsvImport.previewWithAi` benar-benar dieksekusi.
- Parsing nominal AI juga dibuat toleran (`AiCsvMapper.aiAmount`): coba format pilihan AI lalu heuristic pemisah ribuan/desimal (EN/EU, tanda +/-, kurung) agar baris seperti "50.000" / "1.234,56" tidak ditolak.
- Verifikasi: pratinjau AI berhasil membaca file CSV nyata pengguna (31 transaksi tampil) tanpa menyimpan data; konsen AI Chat dihormati.

## File terkait

- `composeApp/src/androidMain/AndroidManifest.xml`
- `composeApp/src/androidMain/kotlin/com/clonical/kasiva/ProfilePhotos.kt`
- `composeApp/src/commonMain/kotlin/com/clonical/kasiva/app/App.kt`
- `composeApp/src/commonMain/kotlin/com/clonical/kasiva/feature/importdata/AiCsvMapper.kt`
- `composeApp/src/commonMain/kotlin/com/clonical/kasiva/feature/importdata/CsvImport.kt`
- `composeApp/src/commonMain/kotlin/com/clonical/kasiva/feature/ai/AiData.kt`
- `firebase/functions/ai.js`, `index.js` (callable `aiCsvMap`, sudah dideploy)
- test: `FinanceImportTest` (21 lulus), `node --test` functions (36 lulus)

Build: `:composeApp:assembleDebug` sukses; APK debug dipasang di HP uji.

## 5. Ikon pensil pada foto profil tertutup

- Laporan pengguna: badge pensil di avatar Profil terlihat terpotong/tertutup.
- Penyebab: kotak luar avatar memakai `clip(CircleShape)`; badge diletakkan di `BottomEnd` dengan offset keluar lingkaran sehingga separuh badge terpotong oleh clip.
- Perbaikan (`ProfileScreen.kt`): clip lingkaran hanya pada foto avatar; kontainer klik tidak lagi clip, badge 24 dp diberi cincin border warna kanvas agar utuh dan kontras.
- Verifikasi: build debug + screenshot layar Profil di emulator — badge pensil tampil penuh di kanan bawah avatar.
