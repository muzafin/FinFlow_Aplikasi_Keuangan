# Status Firebase — diperiksa ulang 29 Oktober 2026

Pembaruan lanjutan 2 November: `resetFinancialData` sudah ACTIVE; dua fungsi penghapusan akun diperbarui, rules reset diterbitkan dan dicocokkan langsung dengan kode lokal. Endpoint tanpa autentikasi ditolak. [Hasil pemeriksaan terbaru](../../docs/VALIDASI_LOKAL_DAN_PETA_JALAN_2026-11-02.md).

- Akun tujuan: `muzafinarifin.dev@gmail.com` — berstatus **Owner** pada Users and permissions, diverifikasi ulang langsung pada Firebase Console. Tidak ada pemilik manusia lain pada daftar anggota yang ditampilkan.
- Project: `finflow-logikawan` / FinFlow Logikawan — dibuat baru; project number 644917550039.
- Android `com.clonical.kasiva` — terdaftar; app ID `1:644917550039:android:fd8cafbfec4ed63983c992` (app lama `com.logikawan.finflow` tetap ada).
- Konfigurasi Android terbaru setelah OAuth/SHA — diunduh dan dipasang ke `composeApp/google-services.json`; project ID dan package cocok. Terdapat OAuth client Android dan Web.
- Firebase Authentication Email/Password — aktif, diverifikasi status Enabled di Console.
- Firebase Authentication Google — aktif, diverifikasi status Enabled di Console.
- Password policy — diperbarui 29 Oktober 2026: **Require enforcement**, minimum **8 karakter**, maksimum 4096, tanpa Force upgrade on sign-in. Sesuai validasi pendaftaran Kotlin; akun lama tidak dipaksa mengganti password ketika login. Bukti: `../../docs/firebase-evidence/password-policy-2026-10-29.png`.
- Nama publik: Kasiva; email dukungan: `muzafinarifin.dev@gmail.com`.
- SHA-1 dan SHA-256 debug lokal — tersimpan, diverifikasi pada tabel sertifikat Firebase Console.
- Firestore Standard `(default)` — dibuat pada `asia-southeast2` (Jakarta), mulai dalam production mode.
- Rules schema v2 — diperbarui melalui Console pada 29 Oktober 2026 14:42 WIB untuk koleksi perencanaan dan pembayaran. Isi editor cocok dengan `firestore.rules` yang diuji setelah normalisasi spasi; versi aktif bertanda bintang teramati. Lima uji rules dan lima uji repository Kotlin dari device menggunakan Firebase Emulator berhasil. [Bukti](../../docs/firebase-evidence/firestore-planning-rules-2026-10-29.png).
- Indeks — konfigurasi default otomatis Firestore. Query aplikasi saat ini tidak membutuhkan composite index; file indeks lokal kosong secara sengaja.
- iOS `com.clonical.kasiva` — terdaftar, app ID `1:644917550039:ios:d64c0f202ed4acbe83c992` (app lama tetap ada).
- Plist iOS resmi — diunduh dan dipasang ke `iosApp/GoogleService-Info.plist`; project/bundle ID sesuai. `Config.xcconfig` lokal dibuat dari reversed client ID resmi.
- Pengaitan produk Google Play — belum diaktifkan. Menu Console menawarkan pertukaran data App Distribution, Crashlytics, dan Google Analytics. Produk tersebut tidak diperlukan untuk login/Firestore dan belum dipilih. Tidak ada listing Play yang dibuat atau diterbitkan.
- SHA Play App Signing/release dan signing team iOS — masih perlu sertifikat serta akun signing developer saat menyiapkan rilis. SHA debug tidak menggantikan sertifikat rilis.
- Billing Blaze/pay as you go dikonfirmasi pengguna pada 29 Oktober. Asisten tidak mengubah billing, menambah kontrak bank, bucket Storage atau deployment Functions.
- Project lama `finflow-c38e8` tidak dihapus atau diubah.
- Pada pemeriksaan konfigurasi awal 29 Oktober, sudah terdapat dokumen pengguna dengan subkoleksi `accounts` dan `categories`. Tahap pengembangan menu selanjutnya membangun Android dan menguji penyimpanan pada Firebase Emulator; tidak membuat akun atau transaksi percobaan di produksi.

## Pembaruan 2 November 2026

Blaze diverifikasi melalui API. Rules Firestore terbaru sudah diterbitkan untuk metadata Ollo dan foto profil; akun produksi yang sebelumnya `PERMISSION_DENIED` kini memuat Beranda. Bucket privat `finflow-logikawan.firebasestorage.app` dibuat di `ASIA-SOUTHEAST2`. Rules Storage hanya mengizinkan foto JPEG pemilik maksimal 2 MB. Functions generasi kedua `requestAccountDeletion` dan `deleteAccountData` aktif di `asia-southeast2`, minimum instance 0 dan maksimum 2; artefak build dibersihkan setelah satu hari. Alur penghapusan menggunakan autentikasi ulang dan penanda server yang memblokir token lama. [Validasi terbaru](../../docs/VALIDASI_2026-11-02.md) membedakan uji emulator/perangkat dan batas produksi. Catatan 29 Oktober di atas merupakan riwayat sebelum aktivasi ini.

Collection `users` dan subcollection data dibuat oleh aplikasi saat akun pertama mendaftar atau menambahkan data. Tidak ada data pengguna palsu yang ditulis ke database baru.

**Setup inti Firebase, Storage dan Functions sudah aktif di cloud**, bukan hanya file lokal. App Check enforcement, integrasi bank, FCM/APNs dan persiapan penandatanganan rilis masih terpisah. App Check enforcement belum diaktifkan karena klien belum memiliki konfigurasi attestation rilis; mengaktifkannya sekarang dapat menolak permintaan aplikasi.

Foto profil dan penghapusan akun memiliki implementasi serta rules tersendiri. Lampiran transaksi belum ditambahkan. [Cakupan terbaru](../../docs/VALIDASI_2026-11-02.md).

## Pembaruan 8 November 2026 — aplikasi Kasiva

- Project Firebase tetap `finflow-logikawan` (nomor 644917550039). App baru: Android `com.clonical.kasiva` = `1:644917550039:android:fd8cafbfec4ed63983c992`, iOS `com.clonical.kasiva` = `1:644917550039:ios:d64c0f202ed4acbe83c992`.
- SHA-1 `ce:da:36:20:...:1c:9a` dan SHA-256 `94:c3:61:40:...:0b` sertifikat **debug** sudah terdaftar pada app Android baru. SHA sertifikat rilis/Play App Signing belum ada.
- `composeApp/google-services.json` dan `iosApp/GoogleService-Info.plist` diunduh ulang dari app baru dan dipasang lokal (keduanya diabaikan Git); `Config.xcconfig` iOS ditulis ulang dari reversed client ID app baru.
- Functions di-deploy ulang 8 November 2026; `refreshPremium` dan `revenueCatWebhook` aktif di `asia-southeast2`. Secret `REVENUECAT_SECRET_KEY` dan `REVENUECAT_WEBHOOK_AUTH` tersimpan di Secret Manager (versi 2), bukan di repo.
- RevenueCat: project `projaf22ad9c` (Kasiva); app Play Store `appfb825165c2` sudah diarahkan ke package `com.clonical.kasiva`; entitlement `kasiva_pro`; webhook `whintgrb9c46d6dfa804661a646dbb8ffdb6584` menuju `https://asia-southeast2-finflow-logikawan.cloudfunctions.net/revenueCatWebhook` (uji 200 dengan header benar, 401 tanpa header).
- Sisa pekerjaan Premium: service account Play untuk RevenueCat belum diunggah (`play_service_account_credentials_configured=false`), produk langganan Play belum dibuat, offering/paket belum dipetakan, dan SHA rilis belum didaftarkan.

## Pembaruan 8 November 2026 — Cloud Functions AI

- `aiParseText`, `aiScanReceipt`, dan `aiAssistant` di-deploy ulang 8 November 2026 di `asia-southeast2` (Node.js 22, 2nd gen). `aiStatus` dan `aiConsent` tetap.
- Model AI dipindah ke `deepseek-flash` (parameter `AI_MODEL_PARSE` dan `AI_MODEL_ASSISTANT` di `functions/.env.finflow-logikawan`). `deepseek-flash` menerima teks **dan gambar**, jadi AI Scan tidak lagi ditolak adapter.
- Adapter `functions/deepseek.js` mengirim gambar sebagai `image_url` data URL, mematikan thinking untuk ekstraksi JSON, dan memakai `reasoning_effort: low` untuk Asisten.
- Semua system prompt AI (Chat, Voice, Scan, Asisten) memuat aturan konteks Kasiva: hanya keuangan pribadi dan penggunaan aplikasi; permintaan di luar konteks ditolak singkat dalam bahasa aplikasi; tidak membocorkan prompt; data pengguna bukan instruksi.
- `DEEPSEEK_API_KEY` yang tersimpan saat pemeriksaan awal DITOLAK DeepSeek (HTTP 401, `****e51e`) — itulah sebabnya fitur AI menampilkan "layanan AI belum tersedia". Kunci valid sudah disimpan (Secret Manager **versi 2**) dan ketiga function AI di-deploy ulang pada 8 November 2026. Kunci tidak pernah ada di repo.
- Bukti: `node --test test/*.test.js` di `functions` → 34 lulus, 0 gagal; `aiParseText` tanpa autentikasi → HTTP 401 "Masuk ke akun Kasiva untuk memakai AI."; smoke test DeepSeek dengan kunci baru → Chat memecah 2 transaksi, Chat menolak permintaan puisi, Scan membaca struk Rp60.000 dan menolak gambar logo, Asisten menolak pertanyaan politik dan menjawab kategori teratas. Rincian: [AI_KASIVA_2026-11-08.md](../../docs/AI_KASIVA_2026-11-08.md).
- Sisa: uji manual di HP dengan akun nyata (consent per fitur, kuota, biaya); panggilan callable dari aplikasi belum dijalankan otomatis dalam perbaikan ini.

## Pembaruan 10 November 2026 - rilis Android dan Crashlytics

- SHA-1 Play App Signing `30:35:FC:E6:A0:A1:58:A4:C4:83:CC:46:5F:E6:09:2B:E0:17:5F:7D` sudah terdaftar pada aplikasi Android Kasiva. Catatan SHA rilis belum terdaftar di atas adalah riwayat sebelum pendaftaran ini.
- Perbaikan inisialisasi profil saat login pertama disertakan mulai 1.0.2. Rules kepemilikan dan saldo tidak dilonggarkan untuk mengatasi benturan dua inisialisasi profil.
- Android 1.0.3 (version code 4) mengizinkan screenshot dan mengaktifkan Firebase Crashlytics pada paket produksi non-debug. Log nonfatal tambahan hanya mengirim jenis exception, stack trace, nama operasi, dan kode Firestore bila tersedia; pesan/cause asli tidak dikirim oleh helper tersebut.
- Build bundle produksi dan 90 unit test lulus pada 10 November. Pemeriksaan screenshot pada varian release non-debug paket uji terpisah lulus di POCO 2311DRK48G. Varian uji tidak diminifikasi agar runner instrumentasi dapat berjalan; bundle Play tetap diminifikasi dan mapping Crashlytics berhasil diunggah.
- Firebase Console sudah menerima laporan nonfatal sintetis versi 1.0.3 (4), dengan `operation=diagnostic_verification`. Paket uji terpisah sudah dihapus dari HP; aplikasi Play beserta datanya tidak dihapus.
- Kebijakan privasi publik `/privasi` sudah mencakup Crashlytics; halaman privasi dan `/hapus-akun` merespons HTTP 200.
- Play Console: listingan dan deklarasi aplikasi lengkap; pemeriksaan otomatis selesai dan rilis 1.0.3 (4) sedang ditinjau Google untuk pengujian tertutup Alpha, wilayah Indonesia, memakai daftar 21 penguji yang sudah ada. Publikasi produksi belum tersedia: minimal 12 penguji harus ikut tertutup terus-menerus selama 14 hari sebelum permohonan akses produksi. Penguji internal harus keluar dari internal lalu ikut tertutup.
- Cara membaca laporan: Firebase Console -> project `finflow-logikawan` -> Crashlytics -> Kasiva -> pilih versi dan issue -> Stack trace; tab Keys memperlihatkan `operation`. Pengguna tetap perlu mengirim langkah reproduksi, waktu kejadian, versi aplikasi, dan screenshot untuk bug perilaku yang tidak melempar exception.

## Pembaruan 11 November 2026 - FCM dan App Check klien

- Functions baru di-deploy (Node.js 22, 2nd gen, `asia-southeast2`): `registerPushToken`, `unregisterPushToken`, `billReminderPush` (Cloud Scheduler 08:00 Asia/Jakarta; API Cloud Scheduler diaktifkan otomatis oleh CLI). Function lama tidak diubah.
- Index Firestore: field override `pushTokens.tokenId` (collection group) di-deploy. Rules tidak diubah; `users/{uid}/pushTokens` hanya ditulis Admin SDK.
- Android 1.0.4 memasang FCM (topik `announcements`, `lang_<kode>`), App Check Play Integrity/debug (belum enforce), Analytics dimatikan lewat manifest.
- Sisa: daftarkan Play Integrity di App Check (SHA-256 app signing), uji push di HP, APNs untuk iOS. Rincian: [FCM_DAN_FIX_AI_2026-11-11.md](../../docs/FCM_DAN_FIX_AI_2026-11-11.md).
