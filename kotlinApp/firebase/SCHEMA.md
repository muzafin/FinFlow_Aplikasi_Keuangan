# Schema Firestore v2 — FinFlow Logikawan

Project: `finflow-logikawan`. Pemisahan pengguna: `users/{FirebaseAuth.uid}`. Semua waktu audit berupa epoch millisecond `Long`; tanggal transaksi adalah tanggal kalender `YYYY-MM-DD`, dan anggaran `YYYY-MM`.

| Jalur | Field utama | Keterangan |
|---|---|---|
| `users/{uid}` | id, name, email, baseCurrency, schemaVersion=2, photoPath?, photoUpdatedAt? | Profil; foto hanya menunjuk path milik UID yang sama |
| `users/{uid}/accounts/{id}` | id, name, type, currency, openingBalanceMinor, archived, createdAt, excludedFromTotal?, parentId?, icon?, color?, goalId? | Mata uang tetap; pocket satu tingkat; savings dikelola melalui target |
| `users/{uid}/categories/{id}` | id, name, type, parentId?, icon?, color? | Tipe income/expense tetap; subkategori satu tingkat |
| `users/{uid}/transactions/{id}` | id, type, amountMinor, currency, accountId, categoryId, title, description, date, toAccountId, receivedAmountMinor, createdAt, updatedAt, source, sourceId, scheduled | Jurnal; transfer memakai dua dompet; nominal selalu positif |
| `users/{uid}/budgets/{id}` | id, categoryId, limitMinor, currency, month | Penggunaan dihitung dari jurnal expense |
| `users/{uid}/goals/{id}` | id, name, targetMinor, savedMinor, currency, targetDate, bucketType?, protection?, holdingAccountId?, legacySavedMinor?, archived?, revision?, lastEventId? | Setor/tarik memakai transfer internal ke dompet savings; progres manual lama dipisahkan |
| `users/{uid}/investments/{id}` | id, name, type, investedMinor, currentMinor, dividendsMinor, currency, platform, notes | Penilaian manual dalam mata uang fiat |
| `users/{uid}/bills/{id}` | title, amountMinor, currency, accountId, categoryId, dueDate, frequency, anchorDay, paidAt, lastPaymentId, archived, revision | Pembayaran eksplisit, ID periode mencegah pembayaran ganda |
| `users/{uid}/debts/{id}` | person, direction, amountMinor, paidMinor, currency, accountId, issueDate, dueDate, includeBalance, archived, lastEventId, revision | borrowed/lent; cicilan parsial; pilihan jurnal atau pelacakan saja |
| `users/{uid}/wishes/{id}` | title, targetMinor, currency, priority, link, note, fulfilled | Status tidak memengaruhi saldo |
| `users/{uid}/cards/{id}` | name, brand, lastFour, accountId, expiryMonth, expiryYear, note, recordType?, accountNumber?, ownerName?, label?, branch?, ownership?, category?, pinned?, color?, design? | Nomor rekening untuk catatan bank/e-wallet; kartu pembayaran hanya empat digit terakhir, tanpa PAN/PIN/CVV |
| `users/{uid}/notes/{id}` | title, body, pinned, createdAt, updatedAt, kind?, currency?, accountId?, recordAs?, items?, archived?, revision?, lastPaymentId? | Catatan teks atau bundle maksimal lima item; pembayaran berpasangan dengan jurnal/event |
| `users/{uid}/reimbursements/{expenseId}` | expenseId, title, party, amountMinor, currency, accountId, receivedAt, receivedAccountId, revision | Satu klaim per pengeluaran aktual; penerimaan maksimal nilai klaim |
| `users/{uid}/recurring/{id}` | title, type, amountMinor, currency, accountId, categoryId, frequency, interval, anchorDay, startDate, nextDate, endDate, active, lastEventId, revision | Satu event per periode; tanggal 31 tetap menjadi jangkar setelah Februari |
| `users/{uid}/financialEvents/{id}` | kind, referenceId, title, amountMinor, currency, accountId, date, periodDate, transactionId, previousEventId, createdAt | Event pembayaran dan jurnal disimpan/dibatalkan bersama; histori bersifat immutable |
| `users/{uid}/connections/{id}` | Belum digunakan | Baca pemilik, tulis backend saja |
| `users/{uid}/externalAccounts/{id}` | Belum digunakan | Baca pemilik, tulis backend saja |
| `users/{uid}/syncEvents/{id}` | Belum digunakan | Baca pemilik, tulis backend saja |
| `_accountDeletions/{uid}` | status, requestedAt, completedAt? | Hanya backend; penanda pending/complete memblokir akses UID tersebut dan dipertahankan setelah penghapusan |
| `_dataResets/{uid}` | status, requestId, requestedAt, completedAt?, profile? | Backend saja; pending menyimpan identitas untuk pemulihan dan memblokir data/media pemilik; complete membolehkan akun dipakai kembali |
| `_dataResets/{uid}/requests/{requestId}` | status, completedAt | Receipt backend agar retry reset lama tidak menghapus data yang dibuat sesudah reset lain |

Transaksi mendukung field opsional `subcategoryId` dan maksimal 10 `tags` unik. Metadata tambahan bersifat opsional untuk dokumen lama. Inisialisasi profil/dompet/kategori memakai field inti agar akun baru tetap kompatibel selama transisi rules.

`resetFinancialData` mempertahankan Firebase Auth, nama/email/mata uang profil, menghapus data/media sendiri dan mengembalikan Kas nol serta kategori default. Identitas pemulihan di job dibersihkan setelah complete. Penghapusan akun turut membersihkan seluruh job/receipt reset. [Validasi reset dan mode lokal](../../docs/VALIDASI_LOKAL_DAN_PETA_JALAN_2026-11-02.md).

Cloud Storage terpisah dari Firestore: `users/{uid}/profile/avatar.jpg`, JPEG maksimal 2 MB, baca/tulis pemilik saja. Data foto tidak dimasukkan sebagai URL publik atau biner di dokumen Firestore maupun cadangan JSON. Penghapusan akun membersihkan seluruh prefix `users/{uid}/` pada bucket, kemudian dokumen/subkoleksi dan Auth; rules memblokir token lama sejak penanda server dibuat.

Saldo dompet = `openingBalanceMinor + income - expense - transfer_keluar + transfer_masuk`. Jurnal `scheduled=true` dikecualikan. Arus kas laporan hanya menghitung `income` dan `expense`. Saldo lintas mata uang tidak dijumlahkan.

`amountMinor` tidak menggunakan Double: IDR 15000.50 → 1500050; JPY 15000 → 15000; KWD 1.234 → 1234. Batas satu nilai adalah 9.000.000.000.000.000 satuan minor. Penjumlahan memeriksa overflow Long. Model fiat ini tidak untuk menampung kuantitas token kripto berpresisi 18 desimal.

Pendaftaran membuat profil, kategori standar dan Kas dengan Firestore transaction. Tidak ada koleksi global yang dapat dibaca publik. Dokumen pengguna lain, schema yang tidak sesuai, referensi kategori yang salah, dan penulisan koneksi bank dari klien ditolak rules. Dompet diarsipkan agar histori tidak kehilangan referensi.

ID impor diturunkan secara deterministik dari jenis sumber dan ID transaksi pada CSV. Dompet impor dicocokkan berdasarkan nama persis + mata uang. Jika lebih dari satu dompet aktif cocok, pratinjau menolak baris dan meminta pengguna memperjelas nama. Impor tidak menimpa jurnal yang sudah memiliki ID sama.

CSV ekspor Kasiva membawa `Kasiva ID`, `Source`, dan `Source ID` agar impor kembali mempertahankan ID jurnal yang sama, termasuk transaksi manual dan transaksi asal Ivy. Kolom metadata ini bukan field tambahan pada dokumen Firestore.

Collection Firestore tidak perlu dibuat dengan data palsu: collection tersedia saat dokumen pertama ditulis. Collection kosong lainnya muncul ketika pengguna membuat anggaran/target/investasi pertama. Tidak ada data transaksi finansial contoh yang disisipkan pada produksi.

Rules dan indeks ada di folder ini. Keberadaan file rules lokal tidak menandakan sudah diterbitkan. Lihat SETUP_STATUS.md.

Query versi ini mengamati collection milik pengguna dan memfilter data pada aplikasi. Indeks single-field bawaan Firestore tetap berlaku; composite index belum diperlukan, sehingga `firestore.indexes.json` berisi daftar kosong. Agregasi/paginasi backend untuk riwayat besar perlu dirancang bersama perubahan query, agar pembatasan hasil tidak mengubah saldo secara keliru.
