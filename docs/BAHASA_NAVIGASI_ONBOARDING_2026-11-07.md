# Navigasi liquid glass, splash, onboarding, setup, dan 9 bahasa (7 November 2026)

Bukti: [halaman Bahasa → English → Jepang](ui-evidence/2026-11-07-bahasa/01-halaman-bahasa-id-en-ja.png), [onboarding](ui-evidence/2026-11-07-bahasa/02-onboarding.png).

## 1. Navigasi bawah liquid glass (seperti Convx)

- Efek lensa dan slider di-port dari Kyant0/AndroidLiquidGlass (Apache-2.0), **bukan** dari kode Convx (GPL).
  Kodenya ada di `core/ui/liquid/`, lisensinya di `licenses/AndroidLiquidGlass-LICENSE.txt`.
- Indikator bisa digeser. Saat ditekan atau digeser, indikator membesar menjadi lensa: refraksi, tepi kromatik, lalu kembali dengan pegas.
- Drag yang dibatalkan mengembalikan indikator ke tab semula.
- Perbaikan: indikator sekarang ikut berpindah bila halaman berganti dari luar (misalnya kembali dari sub-halaman lalu ketuk Beranda).
  Penyebabnya, lambda `selectedTabIndex` berubah setiap recompose, sehingga state indikator diganti.

## 2. Splash

Android 12+ menahan splash sistem (OnPreDrawListener, 800 ms), dan logo Compose disembunyikan (`hasSystemSplash()`).
Sekarang hanya **satu logo** yang tampil, tidak lagi logo besar lalu mengecil.

## 3. Onboarding

Tata letak dan warna diukur dari onboarding Ollo:
- kartu ilustrasi persegi ber-tint
- label 13sp berspasi
- judul 36sp ExtraBold
- deskripsi 15sp/26
- "Lewati" hanya di slide pertama
- indikator 32×8dp
- tombol 54dp

Aksen per slide mengikuti Ollo: #009688, #2196F3, #1565C0, #7B1FA2. Ilustrasi tetap milik FinFlow, karena gambar Ollo berhak cipta.

## 4. Setup awal

Langkah **pilih bahasa** dan **mata uang** dihapus. Urutannya sekarang: Sambutan → Notifikasi → Gaya → Dompet.
- Bahasa mengikuti HP (`AppPreferences.language = "system"`).
- Mata uang diambil dari locale HP.
- Keduanya bisa diganti nanti: bahasa lewat Profil › Tampilan › **Bahasa**, mata uang lewat Edit Profil.

## 5. moko-resources, 9 bahasa

- Bahasa: Indonesia (base), English, Melayu, Deutsch, Français, 日本語, Español, हिन्दी, 中文(简体).
- Ada **2.205 string** di `composeApp/src/commonMain/moko-resources/<bahasa>/strings.xml`. Seluruh UI sudah dipindah dari teks yang ditulis langsung di kode.
- Pemakaian di kode: `MR.strings.<kunci>.text(arg...)`, dengan `import com.logikawan.finflow.i18n.*` dan `com.logikawan.finflow.core.i18n.text`. Import wildcard wajib, karena moko memecah string menjadi extension property.
- Bahasa aktif disimpan di `AppLocale`. Saat bahasa diganti, UI langsung recompose tanpa restart.
- `"system"` → bahasa HP bila didukung, selain itu English.
- Pengenal (kunci navigasi, `financeMenus`, nilai preferensi) tetap berbahasa Indonesia. Teks tampilannya lewat `menuLabel`, `tabLabel`, `periodLabel`, `transactionTypeLabel`, `pageTitle`, `preferencePageLabel`.
- Pemeriksaan konsistensi (kunci dan placeholder sama di semua bahasa): `python -I kotlinApp/tools/i18n/check.py`.
- Build: AGP diturunkan ke 8.13.2 (moko belum mendukung AGP 9) dan Gradle ke 9.5.1, dengan `compileSdk 37` (`android.suppressUnsupportedCompileSdk=37`).

### Batasan yang diketahui

- Terjemahan selain Indonesia/English dibuat mesin dan **perlu ditinjau penutur asli** sebelum rilis.
- Deskripsi dan target di Peta Jalan Produk masih berbahasa Indonesia; hanya judul, area, dan status yang diterjemahkan.
- Backend AI baru mendukung id/en, jadi bahasa lain meminta jawaban English.
- Data yang dibuat aplikasi (nama dompet "Kas", kategori bawaan, judul transaksi otomatis) memakai bahasa yang aktif saat data dibuat.
- Pengguna lama yang tersimpan dengan `language="id"` tetap Indonesia sampai memilih "Ikuti pengaturan HP".
- Build iOS belum diverifikasi, karena mesin ini Windows.

## Verifikasi

- 58/58 unit test lulus. Unit test JVM membaca teks base dari classpath.
- 50/50 uji UI di HP Xiaomi lulus, termasuk `LanguageDeviceTest` baru (ganti id/en/ja/zh saat berjalan, 9 bahasa, string berformat).
- Dicek manual di HP: ganti ke English dan 日本語 → Beranda, menu, dan navigasi ikut berubah. Bahasa dikembalikan ke "Ikuti pengaturan HP".
