# Langkah upload Kasiva ke Play Store (panduan pemula)

Dokumen ini untuk Anda (pemilik akun). Tidak perlu membuat keystore baru di Android Studio — keystore sudah dibuat dan AAB sudah ditandatangani.

## A. Di mana file-nya?

Semua ada di laptop Anda, folder proyek `D:\Project\FinFlow_Aplikasi_Keuangan\kotlinApp`:

| File | Lokasi | Isi |
|---|---|---|
| AAB siap upload | `composeApp\build\outputs\bundle\release\composeApp-release.aab` | File yang di-upload ke Play Console (sudah ditandatangani) |
| Keystore upload | `upload-keystore.jks` | Kunci upload (JANGAN hilang; backup) |
| Password & alias | `keystore.properties` | Komentar + storePassword, keyAlias=upload, keyPassword |
| Icon store | `..\docs\playstore\icon-512.png` | 512×512 |
| Feature graphic | `..\docs\playstore\feature-graphic-1024x500.png` | 1024×500 |
| Screenshot | `..\docs\playstore\screenshots\` | minimal 2, disarankan 4–6 |

Cara melihat password: buka `keystore.properties` dengan Notepad/VS Code. File ini **tidak di-commit ke Git**. Backup `upload-keystore.jks` + `keystore.properties` ke tempat aman (flashdisk/cloud pribadi). Kalau hilang, upload key bisa direset lewat Play Console.

Di Android Studio:
- Dialog **New Key Store** → klik **Cancel** (tidak perlu bikin baru).
- Dialog **Generate Signed App Bundle or APK** → pilih **Choose existing…** → arahkan ke `upload-keystore.jks` → alias `upload` → password dari `keystore.properties`. Tapi **tidak wajib**: AAB sudah jadi, langsung upload saja.

## B. Upload ke Play Console

1. Buka <https://play.google.com/console> → **Create app**.
   - App name: `Kasiva - Catatan Keuangan`
   - Default language: **Indonesian**
   - App or game: **App**; Free/Paid: **Free**
   - Setujui deklarasi developer → **Create app**.
2. **Dashboard → Set up your app** (isi satu per satu):
   - **App access**: pilih "All functionality is available without special access" karena Mode Lokal bisa dipakai tanpa login (atau isi akun demo bila Anda punya).
   - **Ads**: No.
   - **Content ratings**: isi kuesioner (tidak ada kekerasan/sexual/gambling; kategori Finance) → dapat rating.
   - **Target audience**: 18+ (aplikasi keuangan).
   - **News app**: No.
   - **Data safety**: ikuti tabel di `PLAYSTORE_RELEASE_2026-11-08.md` bagian 7.
   - **Privacy policy**: `https://finflow-logikawan.web.app/privasi`
   - **Account deletion**: `https://finflow-logikawan.web.app/hapus-akun`
3. **Grow → Store presence → Main store listing**:
   - App name, short description, full description → salin dari `PLAYSTORE_RELEASE_2026-11-08.md` bagian 5.
   - Upload **app icon 512×512** dan **feature graphic 1024×500**.
   - Upload **minimal 2 screenshot HP** (9:16). Boleh pakai `docs/playstore/screenshots/01-profil-badge-pensil.png` sebagai salah satunya.
   - Contact email: `muzafinarifin.dev@gmail.com`.
4. **Test and release → Testing → Internal testing → Create new release**:
   - App bundle → **Upload** → pilih `composeApp-release.aab`.
   - Release name: `1.0.0 (1)`; release notes: `Rilis pertama Kasiva.`
   - **Next → Save → Review release → Start rollout to Internal testing**.
   - Tambahkan email Anda sebagai tester (tab **Testers**), lalu buka link opt-in dan install dari Play Store.
5. Setelah aplikasi terpasang dari Play:
   - Cek login Google, AI, impor CSV, mode offline.
   - Jika login Google gagal: Play Console → **Test and release → App integrity → App signing** → salin **SHA-1** → kirim ke saya atau tambahkan sendiri di Firebase Console → Project settings → app Android → **Add fingerprint**.
6. Kalau internal testing sudah oke → **Production → Create new release** → upload AAB yang sama → **Start rollout to Production** (bisa bertahap 20% dulu).

## C. Kalau nanti perlu build ulang AAB

Dari folder `D:\Project\FinFlow_Aplikasi_Keuangan\kotlinApp`:

```powershell
.\gradlew.bat :composeApp:bundleRelease
```

AAB baru muncul di `composeApp\build\outputs\bundle\release\composeApp-release.aab`. Naikkan `versionCode` di `composeApp/build.gradle.kts` bila versi sebelumnya sudah diunggah (mis. 1 → 2), lalu upload ulang.

## D. Jangan lakukan ini

- Jangan membuat **keystore baru** di dialog Android Studio "New Key Store"; AAB sudah ditandatangani upload key yang ada.
- Jangan commit `upload-keystore.jks` / `keystore.properties` ke Git (sudah di-ignore).
- Jangan uninstall aplikasi debug dari HP sebelum mencadangkan data, karena build dari Play ditandatangani kunci berbeda dan tidak bisa menimpa data aplikasi debug.
