# Generator ikon Hugeicons

Menghasilkan `composeApp/src/commonMain/kotlin/com/clonical/kasiva/core/ui/HugeIconData.kt` dari paket MIT `@hugeicons/core-free-icons` (keluarga ikon yang dipakai Ollo). Nama `hi_*` dipakai lewat `FFIcon`.

```bash
mkdir -p /tmp/hi && cd /tmp/hi && npm pack @hugeicons/core-free-icons && tar xzf hugeicons-core-free-icons-*.tgz
python -I kotlinApp/tools/hugeicons/gen.py /tmp/hi/package/dist/esm kotlinApp/composeApp/src/commonMain/kotlin/com/clonical/kasiva/core/ui/HugeIconData.kt $(cat kotlinApp/tools/hugeicons/icons.txt)
```

Tambah ikon baru: tambahkan baris `hi_nama=NamaIkonHugeicons` di `icons.txt`, lalu jalankan ulang. Skrip hanya membaca file sebagai teks (tidak menjalankan JavaScript paket).
