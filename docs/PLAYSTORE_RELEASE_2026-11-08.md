# Kesiapan rilis Play Store — Kasiva 1.0.0 (8 November 2026)

Dokumen kerja untuk mengunggah aplikasi ke Google Play Console. Yang bisa disiapkan dari repo sudah disiapkan; unggah akhir dan pengisian formulir dilakukan pemilik akun di Play Console.

## 1. Artefak rilis

| Item | Nilai |
|---|---|
| Application ID | `com.clonical.kasiva` |
| versionName / versionCode | `1.0.0` / `1` |
| minSdk / targetSdk | 26 / 36 (compileSdk 37) |
| AAB | `composeApp/build/outputs/bundle/release/composeApp-release.aab` |
| Ukuran AAB | 20.633.979 byte (build final 19:59: perbaikan rotasi/foto/hapus offline/AI CSV + badge pensil avatar) |
| SHA-256 AAB | `48B46E2737BD2AAA3D87D05A8D5D91A6A072627FDB928D7A5C48F3661C32F53E` |
| APK rilis (uji lokal) | `composeApp/build/outputs/apk/release/composeApp-release.apk` |

AAB final sudah ditandatangani upload key dan lolos `jarsigner -verify` (jar verified). Build ulang:
```powershell
cd D:\Project\FinFlow_Aplikasi_Keuangan\kotlinApp
.\gradlew.bat :composeApp:bundleRelease
```

## 2. Signing

- **Upload key** dibuat: `kotlinApp/upload-keystore.jks` (alias `upload`), konfigurasi `kotlinApp/keystore.properties` (di-ignore Git). **Backup kedua file**; bila hilang, upload key bisa direset lewat Play Console.
- SHA-1 upload key: `28:77:EF:3A:02:97:E5:CA:CC:45:06:44:D1:4A:DB:17:19:66:27:47`
- SHA-256 upload key: `AA:E6:45:EC:58:2C:C8:57:14:E9:CF:79:70:45:3D:73:4E:FF:A4:3F:5C:13:B7:93:A1:E6:9E:CC:6F:95:B0:52`
- SHA-1 dan SHA-256 upload key sudah didaftarkan ke aplikasi Firebase Android `1:644917550039:android:fd8cafbfec4ed63983c992` (lewat Firebase Management API), jadi **Google Sign-In berfungsi pada build rilis yang ditandatangani upload key**.
- Play App Signing aktif untuk aplikasi baru. Setelah unggahan pertama, Play Console menampilkan **App signing key certificate** (SHA-1 berbeda). SHA-1 itu **wajib ditambahkan juga** ke Firebase app yang sama agar Google Sign-In pada versi dari Play Store tidak gagal:
  1. Play Console → app → **Test and release → App integrity → App signing** → salin SHA-1.
  2. Console Firebase → Project settings → Your apps → Android `com.clonical.kasiva` → **Add fingerprint**.
  3. Atau kirim SHA-1 ke saya; saya daftarkan lewat Firebase Management API.

## 3. URL publik (sudah aktif)

- Kebijakan Privasi: <https://finflow-logikawan.web.app/privasi>
- Hapus Akun (wajib untuk Play): <https://finflow-logikawan.web.app/hapus-akun>
- Halaman utama: <https://finflow-logikawan.web.app/>
- Sumber: `kotlinApp/firebase/hosting/` (dideploy lewat `firebase deploy --only hosting`).

## 4. Aset store

| Aset | File | Spek |
|---|---|---|
| App icon | `docs/playstore/icon-512.png` | 512×512 PNG |
| Feature graphic | `docs/playstore/feature-graphic-1024x500.png` | 1024×500 PNG |
| Screenshot HP | `docs/playstore/screenshots/` | **belum dibuat** — minimal 2, disarankan 4–6; rasio 9:16, sisi 320–3840 px |

Rencana screenshot: Beranda (tema terang), Statistik, Tambah transaksi, Impor Data (daftar sumber + AI), Profil/Manajemen Akun, AI Asisten. Gunakan akun demo dengan data contoh (bukan data pribadi pengguna).

## 5. Store listing (draft id-ID)

- **Nama aplikasi (maks 30)**: `Kasiva - Catatan Keuangan`
- **Deskripsi singkat (maks 80)**: `Catat keuangan pribadi, anggaran, target, dan AI. Bisa offline, data aman.`
- **Deskripsi lengkap (maks 4000)**:

> Kasiva adalah aplikasi pencatatan keuangan pribadi yang rapi, cepat, dan bisa dipakai tanpa internet.
>
> Catat pemasukan dan pengeluaran lewat chat, suara, atau foto struk. Kelola banyak dompet, kategori, anggaran, target tabungan, tagihan, utang, kartu, investasi, dan reimburse dalam satu tempat. Statistik dan wawasan membantu melihat ke mana uang pergi.
>
> **Fitur utama**
> - Catat transaksi manual, AI Chat, AI Voice, dan AI Scan struk.
> - Kelola dompet tunai, bank, e-wallet, tabungan, dan investasi.
> - Anggaran, target tabungan, tagihan berulang, utang/piutang, wishlist, dan kartu.
> - Statistik, grafik, dan ringkasan bulanan.
> - Mode lokal tanpa akun: data hanya di perangkat.
> - Mode cloud: sinkron antar perangkat dengan akun, data dilindungi security rules Firebase.
> - Offline-first: transaksi yang dicatat saat offline otomatis dikirim saat internet kembali.
> - Impor CSV dari Ivy Wallet, Money Manager, Wallet by BudgetBakers, Spendee, Monefy, 1Money, Bluecoins, KTW, Fortune City, Financisto, atau deteksi otomatis dengan AI.
> - 9 bahasa, tema terang/gelap, dan pengaturan tampilan.
>
> Kasiva bukan lembaga keuangan dan tidak memberikan nasihat investasi. Fitur AI memerlukan akun dan persetujuan pengguna.

- **Kategori**: Finance (Personal finance). **Tag**: Personal finance, Budgeting.
- **Email dukungan**: muzafinarifin.dev@gmail.com
- **Kebijakan privasi**: URL di atas.
- **Situs web**: <https://finflow-logikawan.web.app/> (opsional)
- **Bahasa default**: Indonesia (id-ID). Terjemahan en-US opsional.

## 6. App content (jawaban yang disarankan)

| Form | Jawaban |
|---|---|
| Privacy policy | `https://finflow-logikawan.web.app/privasi` |
| Account deletion | `https://finflow-logikawan.web.app/hapus-akun` (in-app deletion juga tersedia) |
| App access | Semua fitur inti dapat dipakai tanpa login lewat **Mode Lokal**; fitur cloud/AI perlu akun. Untuk review fitur cloud/AI, sediakan akun demo (lihat bagian 8). |
| Ads | Tidak ada iklan. |
| In-app purchases | Tidak ada pada rilis ini (`PREMIUM_ENABLED = false`; Play Billing belum diaktifkan). |
| Content rating | Kuesioner: tidak ada kekerasan/sexual/gambling/drugs; aplikasi keuangan pribadi; kemungkinan rating **Everyone / PEGI 3**. Target audiens disarankan **18+** (aplikasi keuangan & akun). |
| News app | Tidak. |
| COVID-19 app | Tidak. |
| Government app | Tidak. |
| Financial features | Bukan produk keuangan (tidak memberi pinjaman/investasi); hanya pencatatan pribadi. |
| Data safety | Lihat bagian 7. |
| Sensitive permissions | Aplikasi memakai NotificationListenerService **opsional** untuk "Impor notifikasi"; pengguna mengaktifkannya sendiri di pengaturan Android, isi notifikasi diproses lokal dan hanya data transaksi yang dikonfirmasi dikirim ke akun. Aplikasi juga meminta izin notifikasi (pengingat) dan biometrik (kunci aplikasi). |

## 7. Data safety (draft)

Data yang **dikumpulkan**:
| Kategori | Data | Wajib? | Tujuan |
|---|---|---|---|
| Personal info | Nama, email | Ya (akun cloud) | App functionality, account management |
| Financial info | Transaksi, dompet, anggaran, target, tagihan, utang, investasi | Ya (akun cloud) | App functionality |
| Photos | Foto profil; foto struk untuk AI Scan (dikirim untuk diproses, tidak disimpan server Kasiva) | Opsional | App functionality |
| App activity | Preferensi tampilan, riwayat percakapan Asisten (maks 5, di perangkat) | Opsional | App functionality |
| Device or other IDs | ID instalasi Firebase untuk auth/sinkronisasi | Ya (akun cloud) | App functionality, security |

- Data **terenkripsi saat transit** (TLS).
- Pengguna dapat **meminta penghapusan data** (in-app + URL web).
- **Data sharing**: fitur AI (opsional, setelah persetujuan) mengirim teks/foto/ringkasan ke server Kasiva lalu ke penyedia model AI **DeepSeek** untuk diproses. Nyatakan data "Financial info" dan "Photos" **shared** untuk *App functionality* bila formulir meminta pihak ketiga; jika DeepSeek diperlakukan sebagai service provider dengan perjanjian, pilih "processed on behalf". Konsultasikan dengan kebijakan yang Anda pilih; jangan menyatakan "no data shared" karena fitur AI ada.
- Tidak ada penjualan data, tidak ada iklan, tidak ada pelacakan lintas aplikasi.
- Fitur AI dapat dinonaktifkan tanpa persetujuan; riwayat Asisten disimpan lokal.

## 8. Akun demo untuk reviewer (disarankan)

Buat akun email/password khusus review, mis. `review@kasiva.app`, isi 3–5 transaksi contoh (jangan data pribadi), lalu masukkan kredensial di App content → App access → "All functionality is available without special access" **atau** "Some functionality requires login" + kredensial tersebut. Mode Lokal sudah bisa diakses tanpa akun, tetapi fitur AI/cloud butuh akun.

## 9. Langkah unggah di Play Console

1. **Create app**: nama `Kasiva - Catatan Keuangan`, bahasa default Indonesia, jenis App, Free; setujui deklarasi.
2. **Set up store listing** (bagian 5) + upload icon, feature graphic, minimal 2 screenshot.
3. **App content**: isi semua formulir (bagian 6–7): privacy policy, account deletion, data safety, content rating, target audience, ads, app access.
4. **Testing → Internal testing → Create new release**:
   - Upload `composeApp-release.aab`.
   - Release name: `1.0.0 (1)`; release notes: `Rilis pertama Kasiva: pencatatan keuangan offline-first, AI, impor CSV, 9 bahasa.`
   - Tambahkan email tester (termasuk email Anda) → Save → Review release → Start rollout to Internal testing.
5. Tunggu review internal (biasanya cepat), pasang dari link tester. **Penting**: hapus dulu APK debug dari HP (tanda tangan berbeda; data akan hilang—cadangkan dulu) karena build Play ditandatangani Play App Signing.
6. Setelah terpasang dari Play: cek login Google (bila gagal, tambahkan SHA-1 App signing ke Firebase, bagian 2), cek AI, impor CSV, offline.
7. **Production → Create release** → upload AAB yang sama (atau naikkan versionCode bila ada perubahan) → rollout bertahap 20% → 100%.

## 10. Status checklist

- [x] Upload keystore + keystore.properties (belum di-commit)
- [x] SHA upload terdaftar di Firebase (Google Sign-In rilis lokal)
- [x] AAB rilis ditandatangani & terverifikasi (`jar verified`)
- [x] Kebijakan privasi + halaman hapus akun online (Firebase Hosting)
- [x] Icon 512 + feature graphic 1024×500
- [ ] Screenshot HP (perlu device/emulator dengan data demo) — belum dibuat; minimal 2, disarankan 4–6
- [ ] App dibuat di Play Console + formulir App content
- [ ] Unggah AAB ke internal testing
- [ ] SHA-1 Play App Signing didaftarkan ke Firebase
- [ ] Rollout production

## 11. Catatan risiko review

- **Notification access** (Impor notifikasi) adalah fitur opsional; jelaskan di listing/privacy. Jika reviewer mempertanyakan, fitur bisa dinonaktifkan pada rilis berikutnya tanpa memengaruhi fitur inti.
- **AI-generated content**: Asisten AI menjawab pertanyaan keuangan; sediakan jalur masukan/keluhan (menu Masukan sudah ada) dan consent per fitur.
- **Account deletion**: URL web harus dapat diakses tanpa login (sudah).
- **Premium** dinonaktifkan pada rilis ini; jangan mengaktifkan produk Play Billing sebelum produk langganan & RevenueCat siap.