# Status Firebase — diperiksa ulang 29 Oktober 2026

Pembaruan lanjutan 2 November: `resetFinancialData` sudah ACTIVE; dua fungsi penghapusan akun diperbarui, rules reset diterbitkan dan dicocokkan langsung dengan kode lokal. Endpoint tanpa autentikasi ditolak. [Hasil pemeriksaan terbaru](../../docs/VALIDASI_LOKAL_DAN_PETA_JALAN_2026-11-02.md).

- Akun tujuan: `muzafinarifin.dev@gmail.com` — berstatus **Owner** pada Users and permissions, diverifikasi ulang langsung pada Firebase Console. Tidak ada pemilik manusia lain pada daftar anggota yang ditampilkan.
- Project: `finflow-logikawan` / FinFlow Logikawan — dibuat baru; project number 644917550039.
- Android `com.logikawan.finflow` — terdaftar; app ID `1:644917550039:android:cb5208624b8c69f983c992`.
- Konfigurasi Android terbaru setelah OAuth/SHA — diunduh dan dipasang ke `composeApp/google-services.json`; project ID dan package cocok. Terdapat OAuth client Android dan Web.
- Firebase Authentication Email/Password — aktif, diverifikasi status Enabled di Console.
- Firebase Authentication Google — aktif, diverifikasi status Enabled di Console.
- Password policy — diperbarui 29 Oktober 2026: **Require enforcement**, minimum **8 karakter**, maksimum 4096, tanpa Force upgrade on sign-in. Sesuai validasi pendaftaran Kotlin; akun lama tidak dipaksa mengganti password ketika login. Bukti: `../../docs/firebase-evidence/password-policy-2026-10-29.png`.
- Nama publik: FinFlow; email dukungan: `muzafinarifin.dev@gmail.com`.
- SHA-1 dan SHA-256 debug lokal — tersimpan, diverifikasi pada tabel sertifikat Firebase Console.
- Firestore Standard `(default)` — dibuat pada `asia-southeast2` (Jakarta), mulai dalam production mode.
- Rules schema v2 — diperbarui melalui Console pada 29 Oktober 2026 14:42 WIB untuk koleksi perencanaan dan pembayaran. Isi editor cocok dengan `firestore.rules` yang diuji setelah normalisasi spasi; versi aktif bertanda bintang teramati. Lima uji rules dan lima uji repository Kotlin dari device menggunakan Firebase Emulator berhasil. [Bukti](../../docs/firebase-evidence/firestore-planning-rules-2026-10-29.png).
- Indeks — konfigurasi default otomatis Firestore. Query aplikasi saat ini tidak membutuhkan composite index; file indeks lokal kosong secara sengaja.
- iOS `com.logikawan.finflow` — terdaftar, app ID `1:644917550039:ios:313fd6f05f5ef03783c992`.
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
