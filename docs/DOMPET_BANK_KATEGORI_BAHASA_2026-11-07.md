# Logo bank/e-wallet dan nama bawaan yang ikut bahasa (7 November 2026)

## Bank dan e-wallet

- Tambah/Edit Dompet: tipe **Bank** atau **E-Wallet** menampilkan grid penyedia (4 kolom) plus "Lainnya".
  - Bank (20): BCA, Mandiri, BRI, BNI, BSI, BTN, CIMB Niaga, Permata, Danamon, OCBC, Maybank, Panin, Jenius/SMBC, Jago, SeaBank, blu, Allo, Neo Commerce, Superbank, Krom.
  - E-wallet (11): GoPay, OVO, DANA, ShopeePay, LinkAja, Flip, i.saku, AstraPay, Sakuku, DOKU, PayPal.
- Memilih penyedia mengisi nama dompet (bila kosong), warna merek, dan ikon `provider:<id>`. Penyedia disimpan di kolom `icon` yang sudah ada, jadi tidak perlu perubahan skema atau aturan Firestore.
- Badge memakai warna merek + singkatan. Badge tampil di Beranda, halaman Dompet, dan pemilih dompet.
- Logo resmi belum dipakai, karena berkas logo adalah merek dagang masing-masing bank dan perlu sumber/izin resmi. Setelah berkasnya ada, isi `providerLogoAssets` di `core/model/FinanceProviders.kt`.

## Nama bawaan ikut bahasa

- Kategori bawaan (Gaji, Makan & minum, dll.) dan dompet "Kas" disimpan dengan nama dalam bahasa saat dibuat.
- Kini, selama namanya belum diganti pengguna (masih salah satu terjemahan bawaan), nama itu tampil dalam bahasa aktif. Contoh dalam English: Kas → Cash, Gaji → Salary.
- Nama yang sudah diganti pengguna tidak diubah.
- Daftar terjemahan dibuat dari moko-resources oleh `tools/i18n/gen_default_names.py`.
- Menambah string ke 9 bahasa sekaligus: `tools/i18n/add_strings.py`.

## Lain-lain

- Tombol simpan editor kini memakai `EditorKind` yang eksplisit, tidak lagi menebak dari kata di judul (yang rapuh setelah diterjemahkan).

## Verifikasi

- 60/60 unit test lulus, termasuk `LocalizedDefaultsTest`.
- 52/52 uji UI di HP lulus, termasuk `ProviderPickerDeviceTest`.
- Dicek manual di HP: mode English menampilkan Cash/Salary/Food & drink; grid bank dan e-wallet tampil rapi. Bahasa dikembalikan ke "Ikuti pengaturan HP".
