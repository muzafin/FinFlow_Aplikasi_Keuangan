# FCM (push) + perbaikan chat pertama AI — 11 November 2026

## 1. Bug: pesan pertama ke Kasiva Assistant tidak dijawab

**Gejala:** pengguna baru membuka Asisten, mengetik lalu kirim → baru muncul dialog persetujuan AI → setelah "Setuju", pesan tampil di layar tetapi AI tidak pernah menjawab.

**Akar masalah:** `AiController.setConsent(feature, true, then)` menjalankan `then()` (= mengirim pesan) **di dalam** blok `run(...)`. Selama blok itu berjalan `working != null`, sehingga `controller.run(...)` milik pesan langsung `return` karena guard `if (working != null) return`. Pesan sudah ditambahkan ke percakapan dan kolom input dikosongkan, tetapi permintaan ke server tidak pernah dibuat. Hal yang sama terjadi pada AI Chat/Voice pertama kali.

**Perbaikan:** `run()` menerima `onSuccess`, yang dipanggil **setelah** `working` dikosongkan. `setConsent` memakai `onSuccess = then` dan langsung menandai consent di `status` lokal. File: `composeApp/src/commonMain/kotlin/com/clonical/kasiva/feature/ai/AiController.kt`.

## 2. Push FCM

### Server (Cloud Functions, `asia-southeast2`, sudah di-deploy 11 Nov 2026)

| Function | Jenis | Fungsi |
|---|---|---|
| `registerPushToken` | callable | Simpan token perangkat ke `users/{uid}/pushTokens/{sha256}` (bahasa, zona waktu, versi, preferensi pengingat). Token yang pindah akun dihapus dari akun lama. |
| `unregisterPushToken` | callable | Hapus token milik pemanggil. |
| `billReminderPush` | terjadwal 08:00 WIB | Tagihan aktif (`archived=false`) yang jatuh tempo **hari ini/besok** (zona waktu perangkat) → satu notifikasi per perangkat. Token yang ditolak FCM dihapus otomatis. |

- Isi notifikasi **tidak memuat nominal atau nama tagihan** (bisa tampil di layar kunci), hanya jumlah tagihan. Bahasa: ID/EN/MS (lainnya EN).
- Klien tidak bisa menulis `pushTokens` (rules tidak membuka path itu); hanya Admin SDK. Hapus akun ikut menghapusnya (`recursiveDelete users/{uid}`).
- Index: field override `pushTokens.tokenId` (COLLECTION_GROUP) di `firestore.indexes.json` — sudah di-deploy.
- Test: `firebase/functions/test/push.test.js` (5 uji); seluruh suite functions 41/41 lulus.

### Android (versi 1.0.4, versionCode 5)

- Dependensi: `firebase-messaging`, `firebase-functions`, `firebase-appcheck-playintegrity`, `firebase-appcheck-debug`, `kotlinx-coroutines-play-services`.
- `PushMessaging.kt` + `KasivaMessagingService`:
  - Semua instalasi (termasuk mode lokal) berlangganan topik **`announcements`** dan **`lang_<kode>`** (mis. `lang_id`).
  - Akun cloud mendaftarkan token lewat `registerPushToken` (di-cache; diperbarui bila token/bahasa/zona waktu/preferensi berubah atau tiap 7 hari).
  - Keluar/pindah akun → `deleteToken()` sehingga akun lama tidak lagi menerima push di perangkat ini.
  - Pesan saat aplikasi di depan ditampilkan sendiri dan dicatat di Riwayat Notifikasi.
- Kanal: `bills` (Pengingat tagihan, prioritas tinggi) dan `announcements` (Info Kasiva, bawaan untuk pesan Console). Ikon status bar `drawable/ic_stat_kasiva.xml`.
- Halaman **Profil → Notifikasi** punya sakelar baru **"Pengingat tagihan (push)"** (meminta izin notifikasi Android 13+; hanya akun cloud).
- Google Analytics **dinonaktifkan** lewat manifest (tidak ada ID iklan), jadi Data safety tidak berubah karena FCM.

### Cara mengirim pengumuman dari Firebase Console

Firebase Console → **DevOps & Engagement → Messaging** → *New campaign* → **Notifications** → isi judul/teks → Target: **Topic** `announcements` (semua) atau `lang_id` (hanya pengguna bahasa Indonesia) → kirim sekarang / jadwalkan. Pilihan target "User segment/App" juga bisa, tetapi statistik kampanye terbatas karena Analytics nonaktif.

Uji cepat ke satu HP: di Console pilih *Send test message* lalu tempel token FCM. Token bisa dilihat di Firestore `users/{uid}/pushTokens/*` → field `token`.

## 3. App Check (klien terpasang, belum di-enforce)

Klien memasang Play Integrity (build Play) / provider debug (build lokal). Server **belum** mewajibkan App Check, jadi tidak ada yang ditolak. Langkah agar bisa di-enforce (dilakukan pemilik akun):

1. Firebase Console → **Security → App Check** → Apps → Kasiva Android → **Play Integrity** → masukkan SHA-256 **App signing key** dari Play Console (App integrity → App signing). Tautkan Play Integrity API bila diminta.
2. Rilis 1.0.4 ke pengujian tertutup, tunggu beberapa hari, lihat metrik App Check (persentase permintaan "verified").
3. Bila >95% verified: aktifkan *Enforce* untuk Firestore/Storage, dan tambahkan `enforceAppCheck: true` pada callable AI (lindungi biaya DeepSeek).

Untuk build debug: jalankan aplikasi, ambil token di logcat (`DebugAppCheckProvider`), daftarkan di App Check → Manage debug tokens.

## 4. Produk Firebase yang sengaja tidak dipakai

| Produk | Alasan |
|---|---|
| SQL Connect, Realtime Database | Data sudah di Firestore. |
| AI Logic, Genkit | AI lewat Cloud Functions + DeepSeek (kunci di Secret Manager). |
| App Hosting | Hosting statis (`/privasi`, `/hapus-akun`) sudah cukup. |
| Extensions | Deprecated di Console; RevenueCat memakai webhook sendiri. |
| Phone Verification | Login email/Google saja. |
| Analytics, A/B Testing, Remote Config, Performance | Belum dibutuhkan; mengaktifkan Analytics/Performance mengubah formulir Data safety Play. Bisa ditambah di rilis berikut. |
| App Distribution, Test Lab | Distribusi lewat Play pengujian tertutup. |

## 5. Hasil uji di HP POCO 2311DRK48G (11 Nov 2026, build debug 1.0.4)

- Versi Play 1.0.3 di-uninstall atas persetujuan pemilik, lalu debug 1.0.4 dipasang dan login akun cloud.
- Token terdaftar di `users/{uid}/pushTokens` (android, id, Asia/Jakarta, 1.0.4, billReminders=true).
- Pesan uji FCM HTTP v1 ke token → HTTP 200 dan notifikasi "Tes notifikasi Kasiva" tampil di kanal Info Kasiva.
- Job Cloud Scheduler `firebase-schedule-billReminderPush-asia-southeast2` dijalankan manual → log `bill reminders { users: 1, sent: 0 }` (tidak ada tagihan jatuh tempo hari ini/besok).
- Temuan & perbaikan saat uji: (1) instalasi baru Android 13+ tidak pernah meminta izin notifikasi sehingga push diterima tetapi tidak tampil → kini diminta sekali setelah login cloud; (2) kegagalan topik sesaat (`SERVICE_NOT_AVAILABLE`) menghentikan pendaftaran token → kini topik dicoba ulang tanpa menghalangi token.
- App Check debug: permintaan mendapat 403 "App attestation failed" karena token debug belum didaftarkan; tidak berpengaruh selama enforcement mati.
- Laporan "saldo tidak sinkron" diperiksa dari data Firestore: Total Saldo Rp33.957.500 = saldo awal BRI Rp3.000.000 + pemasukan November Rp31.000.000 − pengeluaran Rp42.500. Bukan bug: kartu Pemasukan/Pengeluaran hanya periode berjalan, Total Saldo memuat saldo awal dompet.

- Uji manual pemilik: consent Asisten dimatikan → pesan pertama dikirim → Setuju → **AI langsung menjawab pesan tersebut** (bug terkonfirmasi teratasi).
- Pindah akun di HP (ke akun kedua): token baru terdaftar di akun baru; token akun lama ditolak FCM (404 UNREGISTERED) sehingga akun lama tidak lagi mengirim ke HP ini dan dokumennya dibersihkan otomatis saat pengiriman berikutnya.

## 6. Kontras ikon & teks navigasi bawah

Permintaan pengguna: efek kaca bening dipertahankan, yang dikontraskan ikon & teks menu navigasi. (Percobaan pertama memekatkan kaca dan sudah dikembalikan; `LiquidGlass` tetap 0,14/0,22/3 dp.) Perubahan akhir di `MainNavigation.kt`: semua tab memakai `onSurface` penuh (sebelumnya tab tidak aktif `onSurfaceVariant`), label SemiBold (aktif Bold) dengan halo tipis warna latar (`Shadow` blur 8), ikon garis lebih tebal (stroke 1,9; aktif & tombol + 2,2 — parameter baru `stroke` pada `FFIcon`/`HugeIcon`, bawaan tetap 1,5). Diverifikasi lewat screenshot di HP (mode gelap).

## 7. Yang masih perlu dilakukan pemilik akun

1. Unggah AAB 1.0.4 (vc 5) ke **Pengujian tertutup** (build: `.\gradlew.bat :composeApp:bundleRelease`).
2. Uji di HP: login akun cloud → Profil → Notifikasi → nyalakan "Pengingat tagihan (push)" → buat tagihan jatuh tempo besok → tunggu 08:00 WIB (atau kirim *test message* dari Console ke token).
4. iOS push butuh kunci APNs (Apple Developer) — belum.
