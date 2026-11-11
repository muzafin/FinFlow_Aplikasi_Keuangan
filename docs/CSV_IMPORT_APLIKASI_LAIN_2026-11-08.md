# Impor CSV dari aplikasi keuangan lain — 8 November 2026

Pengguna meminta fitur impor CSV dari aplikasi manajemen keuangan lain, mengikuti daftar importer Ivy Wallet (`D:\Project\ivy-wallet`), dengan tampilan mengikuti tema Kasiva.

## Yang sudah ada sebelumnya

- Layar **Impor Data** (Profil → Impor Data) dengan sumber "Ivy Wallet CSV" dan "Template Kasiva".
- Parser CSV mandiri, pembuatan dompet/kategori baru, validasi per baris, pratinjau + duplikat, dan impor atomik.

## Tambahan 8 November 2026

**Format baru yang didukung** (baris pertama header, pemetaan kolom posisional sesuai format ekspor publik masing-masing aplikasi):

| Sumber | Kolom yang dipetakan |
|---|---|
| Money Manager (RealByte) | tanggal, judul, kategori, dompet, amount, mata uang, tipe, deskripsi |
| Wallet by BudgetBakers | dinormalisasi (`;` pemisah, koma desimal), lalu tanggal, kategori, dompet, amount, mata uang, tipe, judul |
| Spendee | tanggal, dompet, tipe, kategori, amount, mata uang, judul |
| Monefy | tanggal `dd/MM/yyyy`, dompet, kategori, amount (tanda +/− = tipe), mata uang, judul |
| 1Money | tanggal, tipe, dompet, kategori, amount, mata uang, judul |
| Bluecoins Finance | tipe, tanggal, judul, amount, mata uang, kategori, dompet, deskripsi |
| Money Manager (KTW) | tanggal, tipe, kategori, amount, mata uang, judul, dompet |
| Fortune City | dompet, kategori, amount, mata uang, tanggal, judul; kategori "Income" = pemasukan |
| Financisto | tanggal, dompet, amount, mata uang, kategori, judul, tipe, deskripsi; transfer dua baris digabung, amount positif = pemasukan |

**Perilaku impor**
- Nominal menerima `1,234.50`, `1.234,50`, `-25`, `(25)`, `25 IDR`; tanggal menerima ISO, `dd/MM/yyyy`, `dd.MM.yyyy`, `yyyy/MM/dd` (waktu diabaikan).
- Dompet/kategori baru dibuat dengan id deterministik `import_…`; yang sudah ada dipakai ulang.
- ID transaksi deterministik dari sumber+baris+isi → impor ulang file yang sama masuk hitungan duplikat, bukan menggandakan.
- Tipe kosong ditentukan dari tanda nominal; format tanpa kolom dompet tujuan mengubah TRANSFER menjadi pengeluaran + satu peringatan (string sudah diterjemahkan).
- Baris rusak dicatat di `errors` dan tidak menggagalkan baris lain.
- "Ivy Wallet CSV" dan "Template Kasiva" tetap berperilaku sama seperti sebelumnya.

**UI**
- Pemilih sumber berbentuk daftar ber-radio di kartu bergaya Kasiva (bukan deretan tab), berisi 11 sumber di atas.
- Panduan impor berubah sesuai sumber: untuk Ivy/Kasiva menampilkan kolom template; untuk aplikasi lain menampilkan instruksi ekspor dari aplikasi tersebut.
- Tombol "Unduh Template" hanya tampil untuk sumber Ivy/Kasiva.

**File**
- `composeApp/src/commonMain/kotlin/com/clonical/kasiva/feature/importdata/CsvImport.kt` — pemetaan posisional, normalisasi angka/tanggal, duplikat, transfer pasangan Financisto.
- `…/importdata/CsvSources.kt` (baru) — `CsvSource`, `CsvImport.sources`, `CsvColumns`.
- `…/importdata/ImportScreen.kt` — pemilih sumber + panduan adaptif.
- `composeApp/src/commonTest/kotlin/com/clonical/kasiva/FinanceImportTest.kt` — 11 test baru (semua format, normalisasi, duplikat, round-trip export→import).

## Verifikasi

- `:composeApp:testDebugUnitTest` (semua) lulus; `FinanceImportTest` 16/16 lulus.
- `:composeApp:assembleDebug` sukses (APK build, belum dipasang ulang karena koneksi USB HP terputus saat akhir pekerjaan).
- Catatan: mapping ditulis sendiri berdasarkan format publik CSV aplikasi sumber, bukan menyalin kode GPL Ivy Wallet.

## Tambahan: deteksi format otomatis dengan AI (8 November 2026)

Karena format CSV tiap aplikasi berbeda dan bisa berubah, ditambahkan sumber **"Deteksi otomatis (AI)"** di layar Impor Data:

- Klien mengirim **header + maksimal 20 baris contoh** ke Cloud Function `aiCsvMap` (consent & kuota memakai fitur AI Chat). AI (DeepSeek) hanya mengembalikan **pemetaan kolom**: index tanggal/judul/kategori/dompet/amount/currency/type/dompet tujuan, format tanggal, format angka, dan mapping nilai tipe.
- Seluruh file CSV kemudian **dikonversi di perangkat** memakai pemetaan itu (bukan dikirim seluruhnya ke AI). Nominal/tanggal diparse toleran (EN/EU/tanda +/-/kurung, beberapa format tanggal), akun/kategori baru dibuat dengan pola `import_`, ID deterministik untuk deteksi duplikat, validasi per baris, baris rusak hanya dicatat sebagai error.
- Server: `firebase/functions/ai.js` (`csvMap`), `index.js` (`aiCsvMap`), sudah di-deploy ke `asia-southeast2` pada 8 November 2026. Prompt memuat aturan konteks Kasiva dan memperlakukan CSV sebagai data, bukan instruksi.
- Klien: `AiData.kt` (`AiService.csvMap`), `AiCsvMapper.kt` (konversi murni `buildAiPlan`), `CsvImport.previewWithAi`, `CsvSources.kt` urutan "ai" di paling atas.
- Uji: `node --test test/*.test.js` 36 lulus; `FinanceImportTest` 21 lulus (termasuk 5 test AI). Uji di HP dengan file CSV nyata berhasil menampilkan pratinjau 31 transaksi (tidak disimpan).
