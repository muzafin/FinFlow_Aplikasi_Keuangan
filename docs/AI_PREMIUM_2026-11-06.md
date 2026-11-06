# AI & Premium FinFlow (paritas Ollo) — 6 November 2026

Lanjutan tugas "samakan FinFlow dengan Ollo". Keputusan pengguna 6 November: **AI dan Premium ikut dikerjakan** (sebelumnya ditunda). Arsitektur mengikuti `D:\Project\FinFlow\docs\FIREBASE.md` §9: model AI hanya dipanggil dari Cloud Functions, Premium diverifikasi server lewat RevenueCat.

## 1. Logo

- `finflow_logo.png` sekarang transparan (latar abu dihapus, bintik sisa di sekitar logo dibersihkan).
- Salinan Android dipindah ke `res/drawable-nodpi/` agar gambar 1254 px tidak diperbesar 3× oleh Android (berisiko lambat/crash di splash). Ikon aplikasi dan ikon splash Android 12+ memakai file yang sama.

## 2. Acuan Ollo yang dicatat (HP Xiaomi, Ollo 1.7.9)

Tangkapan di `kotlinApp/build/ollo-capture/ai-premium/` (diabaikan Git). Tidak ada consent yang disetujui dan tidak ada kuota AI Ollo yang dipakai.

- Tombol + membuka panel: **Assistant** (badge "10" = sisa trial), **AI Voice**, **AI Scan**, **AI Chat** (badge FREE), **Add**.
- AI Voice/Chat/Scan meminta **persetujuan per fitur** (teks diproses server + penyedia AI, suara ditranskrip di perangkat, hasil wajib ditinjau, consent bisa dicabut di Account Management).
- AI Chat: sheet "Quick Record", beberapa transaksi dipisah koma/baris baru.
- AI Scan: Kamera/Galeri, maks 5 foto (1 kredit per foto), maks 5 struk per foto, Pengaturan AI Scan (nama pemilik rekening + 8 detail struk yang bisa diaktifkan & diurutkan untuk catatan transaksi).
- Ollo Assistant (BETA): riwayat (maks 5 percakapan di perangkat), pengaturan (6 kepribadian, nama panggilan 40 karakter, preferensi 200 karakter), 6 saran pertanyaan, hanya melihat ringkasan angka.
- Account Management: Status Akun (Tier/Cloud Sync/Assistant), AI Limits, AI Privacy & Consent.
- Paywall: Monthly (7 hari gratis), 6-Month (hemat 17%), Annual (Terbaik · hemat 33%), Lifetime; daftar fitur; Restore Purchases, Subscription Terms, Manage Subscription.

## 3. Yang dibangun

| Bagian | File |
|---|---|
| Cloud Functions AI: `aiConsent`, `aiStatus`, `aiParseText` (chat & voice), `aiScanReceipt`, `aiAssistant` | `kotlinApp/firebase/functions/ai.js`, `index.js` |
| Cloud Functions Premium: `refreshPremium`, `revenueCatWebhook` | `kotlinApp/firebase/functions/premium.js` |
| Rules: `users/{uid}/ai/{consent,usage}` dan `users/{uid}/entitlements/premium` hanya bisa dibaca pemilik, ditulis server | `kotlinApp/firebase/firestore.rules` |
| Data & pemanggil AI, ringkasan Asisten, konversi draf, catatan struk | `feature/ai/AiData.kt`, `AiController.kt` |
| Panel Quick Record, consent, AI Chat, AI Scan, tinjau draf | `feature/ai/QuickRecord.kt` |
| Asisten, Pengaturan Asisten, Pengaturan AI Scan | `feature/ai/AssistantScreen.kt` |
| Paywall, kartu Upgrade di Profil, batas Free, Status/Batas/Consent AI di Manajemen Akun | `feature/ai/PremiumScreen.kt` |
| Suara (transkrip di perangkat), kamera & galeri struk | `androidMain/.../AiDevice.kt` |
| Google Play Billing via RevenueCat | `androidMain/.../PremiumBilling.kt` |

Aturan penting:
- **Semua hasil AI ditinjau dulu** (lembar "Tinjau Hasil AI"; ketuk draf untuk mengedit di editor transaksi), tidak ada yang tersimpan otomatis.
- **Kuota** dihitung server dalam transaksi Firestore dan dikembalikan bila panggilan AI gagal. Free: Chat 30, Voice 30, Scan 10 per bulan; Asisten 10 seumur akun. Premium: tanpa batas dengan batas wajar 200 permintaan/hari.
- **Premium hanya ditulis server** dari data RevenueCat; aplikasi tidak bisa mengaktifkan Premium sendiri. Field `manualUntil` disiapkan untuk hadiah leaderboard/kode promo dari admin.
- **Batas Free (seperti Ollo)**: 7 dompet aktif, 10 kartu, 7 jadwal berulang, 7 target, 7 wishlist. Hanya menahan penambahan baru; data lama tidak diubah.
- Mode lokal tidak bisa memakai AI/Premium (perlu akun karena lewat server), sama seperti Ollo.
- Model: parse/scan `claude-haiku-5-5`, Asisten `claude-sonnet-5-5` (mengikuti FIREBASE.md; Haiku dinaikkan ke generasi 5.5 yang lebih murah dari 4.5). Bisa diganti tanpa ubah kode lewat parameter `AI_MODEL_PARSE` / `AI_MODEL_ASSISTANT`. Asisten memakai fallback otomatis server bila model menolak permintaan.

## 4. Langkah yang harus dilakukan Arifin (belum dilakukan asisten)

Asisten **tidak** men-deploy, tidak membuat produk di Play Console, dan tidak memasukkan kunci rahasia apa pun.

1. **Kunci Claude API** — buat API key di console.anthropic.com, lalu dari folder `kotlinApp/firebase`:
   ```
   firebase functions:secrets:set ANTHROPIC_API_KEY
   ```
2. **Produk Google Play** (Play Console → Monetize): langganan bulanan (dengan free trial 7 hari), 6 bulan, tahunan, dan produk sekali beli "lifetime".
3. **RevenueCat**: buat project, hubungkan aplikasi Play `com.logikawan.finflow`, buat entitlement **`premium`**, offering *current* berisi paket `$rc_monthly`, `$rc_six_month`, `$rc_annual`, `$rc_lifetime`.
   - Kunci SDK publik Android (`goog_...`) → buat file `composeApp/src/androidMain/res/values/revenuecat.xml`:
     `<resources><string name="revenuecat_api_key" translatable="false">goog_xxx</string></resources>`
   - Kunci rahasia API v1 RevenueCat: `firebase functions:secrets:set REVENUECAT_SECRET_KEY`
   - Webhook RevenueCat → URL function `revenueCatWebhook` (region asia-southeast2), header Authorization isi bebas yang panjang, lalu: `firebase functions:secrets:set REVENUECAT_WEBHOOK_AUTH` dengan nilai **persis sama** (termasuk kata `Bearer ` bila dipakai).
4. **Deploy**:
   ```
   firebase deploy --only functions:finflow,firestore:rules --project finflow-logikawan
   ```
5. Setelah deploy, buka FinFlow → tombol + → AI Chat, setujui consent, coba "makan siang 25rb, parkir 5rb".

## 5. Verifikasi 6 November

- Unit test Cloud Functions: **17 lulus** (7 baru: consent, sanitasi output, kuota & refund, reset bulanan & Premium, Asisten, webhook/refresh Premium).
- Rules Firestore di emulator: **18 lulus** (1 baru: data AI/Premium hanya-baca untuk pemilik).
- Unit test Kotlin: **60 lulus** (5 baru di `AiFeatureTest`).
- Build Android debug berhasil dan terpasang di HP tanpa menghapus data.
- Dicek langsung di HP Xiaomi (APK debug, data pengguna tidak dihapus): splash logo transparan di Android 16, kartu Upgrade ke Premium di Profil, halaman Premium (4 paket, harga menunggu Play/RevenueCat), Manajemen Akun (Status Akun, Batas AI, Persetujuan AI), panel + lima tombol, layar Asisten + 6 saran, Pengaturan Asisten. Pengaturan "Perilaku tombol +" di HP sempat diubah untuk pengujian lalu dikembalikan ke "Langsung buka tambah transaksi".
- Karena function belum di-deploy, menekan AI Chat/Voice/Scan menampilkan "Layanan AI belum aktif di server FinFlow" (bukan crash). Sheet chat/scan, consent, dan tinjau draf baru bisa dicek setelah deploy (§4).
- Temuan: sinkron preferensi (pekerjaan sebelumnya) memunculkan dialog "Preferensi Belum Tersinkron · PERMISSION_DENIED" setiap aplikasi dibuka, karena rules `settings/appearance` belum di-deploy ke produksi. Hilang setelah langkah deploy rules di §4.
- Default "Perilaku tombol +" untuk instalasi baru diubah ke panel semua cara (seperti Ollo); pengguna lama tetap memakai pilihannya.
- **Belum diverifikasi**: panggilan AI sungguhan dan pembelian (menunggu langkah §4).

## 6. Perbedaan yang disengaja / belum

- Urutan detail struk diubah dengan tombol panah, bukan tekan-lama-seret seperti Ollo.
- Daftar fitur Premium hanya mencantumkan yang benar-benar dibatasi di FinFlow (AI, dompet/kartu, berulang/target, wishlist). Fitur Ollo lain (3 foto per transaksi, statistik lanjutan, tema premium, latar kartu kustom) belum ada di FinFlow sehingga tidak dijual.
- Cloud Sync tetap gratis untuk pengguna yang login (di Ollo termasuk Premium). Mengubahnya berarti memindahkan penyimpanan pengguna Free ke perangkat — perlu keputusan terpisah.
- Panel + kini berisi 5 tombol seperti Ollo; "Impor notifikasi" tetap dapat dibuka dari Koneksi Bank / Impor Data.
- App Check belum diwajibkan pada function AI (sama dengan status App Check proyek saat ini).
- iOS: suara, kamera, dan pembelian masih "belum tersedia" (bawaan NativeServices); dikerjakan bersama rilis iOS.
