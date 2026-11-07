package com.clonical.kasiva.feature.settings

import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.i18n.*

// `label` tetap berbahasa Indonesia karena dipakai di prompt yang disalin; layar memakai `displayLabel`.
enum class RoadmapStatus(val label: String) {
    Planned("Direncanakan"), InProgress("Sedang Dikerjakan"), Done("Selesai");

    val displayLabel: String get() = when (this) {
        Planned -> MR.strings.rm_status_planned.text()
        InProgress -> MR.strings.rm_status_in_progress.text()
        Done -> MR.strings.rm_status_done.text()
    }
}

data class RoadmapFeature(
    val id: String,
    val title: String,
    val area: String,
    val status: RoadmapStatus,
    val description: String,
    val next: String,
) {
    fun prompt(): String = """
        Fitur Kasiva $id: $title
        Status: ${status.label}
        Bagian: $area
        Kondisi saat ini: $description
        ${if (status == RoadmapStatus.Done) "Lingkup yang sudah tersedia" else "Target berikutnya"}: $next

        ${if (status == RoadmapStatus.Done) "Tinjau fitur ini sebelum mengubahnya. Perubahan yang saya inginkan: [tulis perubahan di sini]." else "Lanjutkan fitur ini sampai target di atas terpenuhi dan verifikasi alurnya."}
        Ikuti UI dan perilaku Ollo, gunakan logo Kasiva, dan pertahankan navigasi liquid glass. Jaga data keuangan yang sudah ada.
    """.trimIndent()
}

const val roadmapUpdated = "7 November 2026"

// Status describes each stated scope, not a claim that the entire app matches Ollo.
val productRoadmap = listOf(
    RoadmapFeature("FF-001", "Nama dan identitas Kasiva", "Identitas", RoadmapStatus.Done,
        "Nama aplikasi Kasiva dan logo milik Kasiva sudah digunakan.", "Label launcher, splash dan identitas aplikasi memakai nama Kasiva."),
    RoadmapFeature("FF-002", "Login Google dan email", "Akun", RoadmapStatus.Done,
        "Masuk Google, daftar/login email, keluar dan pemulihan password tersedia.", "Data cloud terpisah per akun Firebase; akun baru dapat membuat profil dan dompet awal."),
    RoadmapFeature("FF-003", "Profil dan mata uang utama", "Akun", RoadmapStatus.Done,
        "Nama dan mata uang utama profil dapat diubah.", "Perubahan mata uang profil tidak mengubah nominal atau mata uang dompet lama."),
    RoadmapFeature("FF-004", "Transaksi manual", "Pencatatan", RoadmapStatus.Done,
        "Pemasukan, pengeluaran, judul, catatan, kategori, dompet dan tanggal terhubung ke penyimpanan.", "Tambah, detail, edit dan hapus transaksi biasa; jurnal pembayaran dikelola melalui menu asalnya."),
    RoadmapFeature("FF-005", "Transfer antar dompet", "Pencatatan", RoadmapStatus.Done,
        "Transfer memindahkan dana tanpa menjadi pendapatan atau pengeluaran statistik.", "Sumber dan tujuan divalidasi; transfer beda mata uang menyimpan nominal yang diterima secara terpisah."),
    RoadmapFeature("FF-006", "Keypad, kalkulator dan tanggal", "Pencatatan", RoadmapStatus.Done,
        "Panel transaksi menyediakan keypad angka, hapus, reset, kalkulator dan pemilih tanggal.", "Nominal divalidasi sebelum disimpan dan transaksi dapat ditandai terjadwal."),
    RoadmapFeature("FF-007", "Kategori, subkategori dan tag", "Pencatatan", RoadmapStatus.Done,
        "Kategori/subkategori, ikon, warna serta tag transaksi dapat dikelola.", "Identitas kategori lama dipertahankan ketika namanya diubah; kategori harus sesuai jenis transaksi."),
    RoadmapFeature("FF-008", "Dompet dan pocket", "Dompet", RoadmapStatus.Done,
        "Dompet tunai, bank, e-wallet, pocket, ikon dan mata uang tersedia.", "Edit, arsip, pengelompokan dan pengecualian dari total/statistik mempertahankan riwayat jurnal."),
    RoadmapFeature("FF-009", "Penyesuaian saldo", "Dompet", RoadmapStatus.Done,
        "Saldo aktual dihitung dari saldo awal dan jurnal. Penyesuaian membuat catatan selisih.", "Perubahan saldo dapat ditelusuri melalui jurnal tanpa menimpa nominal awal secara diam-diam."),
    RoadmapFeature("FF-010", "Detail dan tren dompet", "Dompet", RoadmapStatus.Done,
        "Detail menampilkan tren saldo, arus masuk/keluar, ringkasan dan jurnal per periode.", "Filter mingguan, bulanan, tahunan dan semua; pintasan pemasukan, pengeluaran dan transfer."),
    RoadmapFeature("FF-011", "Statistik dan filter laporan", "Statistik", RoadmapStatus.Done,
        "Grafik kategori, perbandingan bulan, tren, siklus, aktivitas, top pengeluaran dan jurnal tersedia.", "Rentang tanggal dan filter memengaruhi laporan; lima bagian utama dapat diurutkan."),
    RoadmapFeature("FF-012", "Anggaran", "Perencanaan", RoadmapStatus.Done,
        "Anggaran per kategori dan periode menghitung realisasi dari jurnal aktual.", "Tambah/edit anggaran serta tampilkan progres dan sisa tanpa menghitung transaksi terjadwal."),
    RoadmapFeature("FF-013", "Target tabungan dan proteksi", "Perencanaan", RoadmapStatus.Done,
        "Target memiliki dompet tabungan, setoran, penarikan, histori dan proteksi penarikan.", "Setor/tarik berupa transfer internal; pembatalan mengembalikan saldo tanpa menciptakan uang baru."),
    RoadmapFeature("FF-014", "Tagihan dan pembayaran", "Perencanaan", RoadmapStatus.Done,
        "Tagihan, jatuh tempo, pembayaran dan pembatalannya sudah terhubung ke jurnal.", "Pembayaran berulang untuk kejadian yang sama tidak menggandakan transaksi."),
    RoadmapFeature("FF-015", "Utang dan piutang", "Perencanaan", RoadmapStatus.Done,
        "Pencatatan utang/piutang, cicilan, sisa dan riwayat pembayaran tersedia.", "Pembayaran dan pembatalan menjaga pasangan jurnal serta saldo dompet."),
    RoadmapFeature("FF-016", "Wishlist", "Perencanaan", RoadmapStatus.Done,
        "Daftar keinginan, target harga, progres dan tautan produk tersedia.", "Tautan produk dapat dibuka; kegagalan membuka tautan ditampilkan."),
    RoadmapFeature("FF-017", "Kartu dan rekening", "Perencanaan", RoadmapStatus.Done,
        "Catatan bank/kartu/e-wallet, pemilik, nomor, label, kategori, warna dan pin tersedia.", "Kelola metadata rekening/kartu; ini belum merupakan pembayaran kartu kredit atau sinkronisasi bank."),
    RoadmapFeature("FF-018", "Bundle belanja dan catatan", "Perencanaan", RoadmapStatus.Done,
        "Catatan teks lama dipertahankan. Bundle mendukung item, centang, estimasi dan pembayaran.", "Bayar satu transaksi atau per item; histori dan pembatalan pembayaran memperbaiki jurnal."),
    RoadmapFeature("FF-019", "Reimburse", "Perencanaan", RoadmapStatus.Done,
        "Klaim penggantian biaya, penerimaan dan pembatalannya tersedia melalui menu Reimburse.", "Kelola klaim dan jurnal pembayaran; nominal reimburse langsung di editor transaksi masih direncanakan."),
    RoadmapFeature("FF-020", "Transaksi berulang", "Perencanaan", RoadmapStatus.Done,
        "Jadwal berulang dapat membuat kejadian transaksi secara idempoten dan dibatalkan dari riwayat.", "Jadwal otomatis Android mengikuti pemilik data aktif serta berhenti selama restore/reset/sinkronisasi."),
    RoadmapFeature("FF-021", "Portofolio aset manual", "Aset", RoadmapStatus.Done,
        "Aset dan investasi dapat dicatat serta nilai terkini diperbarui secara manual.", "Tambah, detail, ubah nilai dan hapus portofolio; harga pasar langsung belum tersedia."),
    RoadmapFeature("FF-022", "Cadangan dan pemulihan JSON", "Data", RoadmapStatus.Done,
        "Cadangan lengkap menyimpan data keuangan dan preferensi dengan validasi integritas.", "Restore ke tujuan kosong, lanjutkan proses terputus dan tolak penimpaan data yang sudah terisi."),
    RoadmapFeature("FF-023", "Impor CSV dan Ivy", "Data", RoadmapStatus.Done,
        "Pratinjau, template, validasi baris dan impor jurnal CSV/Ivy tersedia.", "Transaksi yang sudah diimpor dilewati untuk menghindari duplikasi."),
    RoadmapFeature("FF-024", "Ekspor CSV dan PDF", "Data", RoadmapStatus.Done,
        "Ekspor jurnal dapat difilter menurut tanggal, jenis, dompet dan mata uang.", "Bagikan berkas CSV atau PDF melalui aplikasi perangkat; JSON tetap digunakan untuk cadangan lengkap."),
    RoadmapFeature("FF-025", "Impor notifikasi Android", "Integrasi", RoadmapStatus.Done,
        "Notifikasi aplikasi yang dipilih diparsing menjadi calon transaksi untuk ditinjau.", "Persetujuan dan konfirmasi diperlukan; OTP/PIN dan pesan ambigu ditolak. Bukan koneksi API bank."),
    RoadmapFeature("FF-026", "Tema dan preferensi tampilan", "Tampilan", RoadmapStatus.Done,
        "Tema, palet, gaya kartu/menu, ikon, warna transaksi, format saldo dan privasi saldo tersimpan per pemilik, ditambah preset gradien kartu saldo, tema warna ikon menu dan perilaku tombol +.", "Perubahan preferensi memengaruhi UI dan bertahan setelah aplikasi dibuka ulang."),
    RoadmapFeature("FF-027", "Periode dan memori pencatatan", "Preferensi", RoadmapStatus.Done,
        "Awal bulan anggaran, awal minggu, periode Beranda, dompet awal dan kategori terakhir dapat diatur.", "Pilihan dompet biasa tidak mengambil dompet target tabungan sebagai sumber default."),
    RoadmapFeature("FF-028", "Navigasi liquid glass", "Tampilan", RoadmapStatus.Done,
        "Empat tab utama dan tombol tambah menggunakan navigasi liquid glass Kasiva.", "Desain navigasi ini dipertahankan saat halaman lain disamakan dengan Ollo."),
    RoadmapFeature("FF-029", "Kunci aplikasi", "Keamanan", RoadmapStatus.Done,
        "Kunci perangkat/biometrik dapat diminta saat kembali ke aplikasi.", "Penguncian memakai mekanisme perangkat yang tersedia dan mengikuti pengaturan pengguna."),
    RoadmapFeature("FF-030", "Foto profil privat", "Akun", RoadmapStatus.Done,
        "Foto dipilih dari perangkat, diperkecil dan disimpan privat untuk pemiliknya.", "Mode cloud memakai Storage dengan rules pemilik; mode lokal menyimpan foto di ruang privat aplikasi."),
    RoadmapFeature("FF-031", "Hapus akun beserta data", "Keamanan", RoadmapStatus.Done,
        "Backend penghapusan akun aktif dengan autentikasi ulang, konfirmasi dan worker yang dapat mencoba ulang.", "Hapus data Firestore, Storage dan Auth milik sendiri; cegah token lama membuat ulang akun yang dihapus."),
    RoadmapFeature("FF-032", "Hapus data, pertahankan akun", "Keamanan", RoadmapStatus.Done,
        "Reset lokal tersedia dan backend reset cloud sudah diterbitkan serta diuji dengan emulator.", "Pertahankan identitas akun, kembalikan dompet/kategori awal, dan lanjutkan permintaan terputus tanpa menghapus data baru dua kali."),
    RoadmapFeature("FF-033", "Bantuan, masukan dan informasi aplikasi", "Profil", RoadmapStatus.Done,
        "Panduan, draf email saran/bug, versi aplikasi dan catatan pembaruan tersedia.", "Pengguna meninjau dan mengirim email sendiri. Android dapat membagikan APK melalui pemilih aplikasi."),
    RoadmapFeature("FF-034", "Masukan & Peta Jalan", "Profil", RoadmapStatus.Done,
        "Peta jalan mengikuti susunan Ollo: tiga tab teks dan kartu judul, badge bagian serta deskripsi. Ollo tidak memiliki voting fitur.", "Ketuk kartu untuk rincian dan salin prompt; cari, salin semua dan Kirim Masukan ada di aksi header."),
    RoadmapFeature("FF-035", "Mode lokal tanpa login", "Data", RoadmapStatus.InProgress,
        "Repository lokal, penyimpanan privat Android/iOS, transaksi atomik dan alur masuk lokal sudah ditulis.", "Selesaikan pengujian penyimpanan native, restart aplikasi, alur awal dan seluruh modul dalam mode tanpa jaringan."),
    RoadmapFeature("FF-036", "Pindahkan data lokal ke akun", "Sinkronisasi", RoadmapStatus.InProgress,
        "Pilihan hubungkan akun, salin ke cloud kosong, checkpoint dan lanjutkan proses terputus sudah ditulis. Salinan lokal dipertahankan.", "Uji integrasi perpindahan dan kegagalan jaringan; sertakan foto profil. Ini belum merupakan mirror dua arah mode lokal dan cloud."),
    RoadmapFeature("FF-037", "Samakan seluruh UI dan menu Ollo", "Tampilan", RoadmapStatus.InProgress,
        "Poppins, menu keuangan dan banyak detail sudah mengikuti referensi HP. Tour onboarding empat slide dan wizard pengaturan awal enam langkah sudah ditulis mengikuti susunan Ollo. Beranda pada ronde 3 (3 November 2026) sudah disamakan: header dengan tombol profil dan tombol pintasan tiga titik, tab periode pil hitam, kartu \"Total Saldo (KODE)\" dengan ikon privasi dan swap urutan mata uang, dompet grid dua kolom, transaksi terbaru dikelompokkan per tanggal Indonesia, dan state kosong anggaran. Statistik pada ronde 4 (3 November 2026) juga sudah disamakan: header dengan filter/info/menu tiga titik, tab periode, kartu donut total pengeluaran/pemasukan, ringkasan harian, perbandingan bulanan, wawasan siklus, peta aktivitas, pengeluaran terbesar, log transaksi, dan kartu Analisis Lanjutan dengan skor kesehatan finansial sederhana (bukan AI). Dompet dan detail dompet pada ronde 5 (3 November 2026) sudah disamakan: header dengan komposisi uang/menu tiga titik/mode arsip, kartu total saldo per mata uang dengan perubahan 30 hari, grid Total Kekayaan/Liabilitas/Tabungan Aktif/Pembayaran Mendatang, grup dompet dan empty state, Target Tabungan, dialog Komposisi Uang, serta detail dompet dengan tab periode, grafik saldo, ringkasan, metrik, kartu aksi dan Transaksi Terbaru. Arsip dompet tetap dipakai; penghapusan permanen belum ada (FF-046). Profil pada ronde 7 (3 November 2026) juga sudah disamakan: header avatar 88dp dengan badge kamera 30dp, nama tanpa baris email, metrik LEVEL/STREAK/HARI AKTIF dengan divider dan chevron, section caption, tile grid dua kolom tinggi 172dp, row list ikon lingkaran 40dp, serta palette dan preferensi; bug ikon chevron/fallback diperbaiki termasuk penggunaan aset Figma sebagai fallback. Upgrade ke Premium tidak dihadirkan (Premium/AI ditunda).", "Audit formulir dan menu keuangan lain yang belum disamakan, lalu uji manual pengguna; bandingkan halaman/form/dialog lain, ukuran font, jarak, ikon, gestur dan tombol; pertahankan hanya logo Kasiva dan navigasi liquid glass."),
    RoadmapFeature("FF-038", "Splash, onboarding dan login Ollo", "Tampilan", RoadmapStatus.InProgress,
        "Splash logo Kasiva, tour onboarding empat slide (ilustrasi, badge, judul, titik halaman, tombol pill) dan wizard pengaturan awal enam langkah (welcome dengan nama/foto/pulihkan cadangan/Google, pilih bahasa ID/EN, pilih mata uang, notifikasi pintar, pilih gaya, dompet pertama) sudah ditulis mengikuti susunan Ollo. Langkah welcome memvalidasi nama lengkap wajib dengan error inline ID/EN, dialog \"Lihat Semua\" mata uang memakai OlloCurrencyDialog netral berpencarian, dan uji end-to-end mode lokal sudah dijalankan di HP.", "Langkah perintah suara Ollo, paritas visual penuh, dan uji login/akun cloud dari langkah welcome."),
    RoadmapFeature("FF-039", "Kontras dark mode seluruh aplikasi", "Aksesibilitas", RoadmapStatus.InProgress,
        "Label Pengeluaran, tombol keypad, pemilih dompet dan berbagai permukaan sudah memakai warna tema.", "Lulus uji kontras hasil render untuk semua palet/tema, lalu periksa dialog, form dan keadaan terpilih di HP."),
    RoadmapFeature("FF-040", "Pengingat dan peringatan anggaran", "Notifikasi", RoadmapStatus.InProgress,
        "Pengingat harian dan peringatan 80%/100% anggaran sudah dihubungkan dengan izin perangkat.", "Verifikasi pengiriman sistem pada HP, pergantian akun, waktu lokal dan batasan penjadwalan latar belakang."),
    RoadmapFeature("FF-041", "Audit semua tombol dan alur kembali", "Kualitas", RoadmapStatus.InProgress,
        "Uji perangkat sudah mencakup sejumlah menu, preferensi, repository dan cadangan. Asal halaman detail dompet/target telah diperbaiki. Integrasi Firebase diverifikasi pada ronde 6 (3 November 2026) memakai Firebase Emulator Suite project demo demo-finflow: Firestore rules 17/17, storage rules 3/3, Cloud Functions hapus akun/reset data 2/2, dan 42 instrumentasi di HP (31 UI lokal + 11 Firebase cloud, dijalankan dua proses terpisah) termasuk CloudModeSmokeDeviceTest yang membuat akun cloud lewat Auth emulator, menyimpan transaksi lewat Firestore emulator dan memastikan tampilan lokal ikut terbarui, serta ProfileParityDeviceTest untuk halaman Profil; rules/functions produksi tidak diubah.", "Uji manual pengguna dan akun cloud nyata (bukan emulator), lalu tuntaskan matriks semua tombol, batal, Back, validasi error, mode lokal/cloud dan data kosong; jangan menyatakan setara penuh sebelum semuanya diperiksa."),
    RoadmapFeature("FF-042", "Quick Record: chat, suara dan scan", "Pencatatan", RoadmapStatus.InProgress,
        "Lembar Quick Record dengan AI Chat, AI Voice dan AI Scan sudah ditulis; hasil AI selalu ditinjau di editor sebelum disimpan. Cloud Functions AI (DeepSeek) belum di-deploy karena secret belum diisi, sehingga aplikasi menampilkan keadaan layanan AI belum tersedia. Penyedia saat ini tidak menerima gambar, jadi AI Scan ditolak server dengan pesan yang jelas.", "Deploy functions AI beserta secret, uji kuota/persetujuan di HP, dan pilih penyedia yang mendukung gambar untuk AI Scan."),
    RoadmapFeature("FF-043", "Memori dan bahasa Quick Record", "Preferensi", RoadmapStatus.InProgress,
        "Bahasa Input Suara tersedia sebagai preferensi (ikut bahasa aplikasi atau pilih salah satu dari 11 bahasa) dan tersinkron bersama preferensi lain.", "Pastikan App memakai preferensi ini saat memanggil pengenalan suara, lalu tambahkan memori preferensi Quick Record yang dapat ditinjau dan direset."),
    RoadmapFeature("FF-044", "Reimburse langsung dari transaksi", "Pencatatan", RoadmapStatus.Planned,
        "Reimburse terpisah sudah tersedia, tetapi kolom nominal penggantian di editor transaksi belum ada.", "Tautkan transaksi dan klaim secara atomik; edit/hapus/pembatalan tidak boleh menggandakan saldo."),
    RoadmapFeature("FF-045", "Foto target, kartu dan lampiran", "Media", RoadmapStatus.Planned,
        "Foto profil tersedia; gambar target/kartu dan lampiran transaksi belum terhubung.", "Tambahkan gambar privat pada editor/detail, mode lokal/cloud, penggantian/pembersihan berkas, rules dan batas ukuran."),
    RoadmapFeature("FF-046", "Hapus dompet sesuai perilaku Ollo", "Dompet", RoadmapStatus.Planned,
        "Kasiva mendukung arsip untuk menjaga transaksi terkait; penghapusan permanen belum tersedia.", "Periksa konfirmasi Ollo dan tentukan perlakuan jurnal/pocket/target sebelum menambahkan penghapusan yang aman."),
    RoadmapFeature("FF-047", "Kartu kredit dan paylater", "Perencanaan", RoadmapStatus.Planned,
        "Catatan metadata kartu tersedia; siklus tagihan, limit dan pembayaran kredit/paylater belum ada.", "Verifikasi perilaku Ollo, kemudian implementasikan kewajiban dan pembayaran tanpa menghitung uang dua kali."),
    RoadmapFeature("FF-048", "Split bill", "Perencanaan", RoadmapStatus.Planned,
        "Pembagian tagihan antar orang belum tersedia; referensi Ollo yang diperiksa menandainya dalam pengembangan.", "Tentukan peserta, pembagian, pembulatan dan pelunasan, lalu hubungkan ke jurnal/reimburse dengan konfirmasi pengguna."),
    RoadmapFeature("FF-049", "Kelola sesi perangkat jarak jauh", "Keamanan", RoadmapStatus.Planned,
        "Perangkat Terhubung hanya menampilkan perangkat yang sedang digunakan.", "Tambahkan daftar sesi yang sah dan pencabutan akses yang benar-benar ditegakkan backend/rules."),
    RoadmapFeature("FF-050", "Sinkronisasi konflik dan revisi server", "Sinkronisasi", RoadmapStatus.Planned,
        "Mode akun memakai Firebase dan perpindahan lokal dilakukan ke akun kosong. Penggabungan akun terisi belum didukung.", "Rancang revisi server, penanganan konflik dan pemulihan lintas perangkat sebelum menawarkan merge atau mirror lokal/cloud."),
    RoadmapFeature("FF-051", "Koneksi API bank dan e-wallet", "Integrasi", RoadmapStatus.Planned,
        "Manual, CSV/Ivy dan calon transaksi dari notifikasi Android tersedia. Belum ada koneksi API penyedia keuangan.", "Pilih penyedia serta cakupan bank yang konkret, persetujuan pengguna, kredensial, token backend dan webhook; Blaze saja tidak menyediakan integrasi bank."),
    RoadmapFeature("FF-052", "Harga pasar aset otomatis", "Aset", RoadmapStatus.Planned,
        "Nilai portofolio saat ini diisi manual.", "Integrasikan sumber harga berlisensi dengan simbol, mata uang, waktu pembaruan, cache dan penanganan data tidak tersedia."),
    RoadmapFeature("FF-053", "Asisten keuangan AI", "AI", RoadmapStatus.InProgress,
        "Layar Asisten, riwayat 5 percakapan, Pengaturan Asisten (kepribadian, panggilan, preferensi) dan persetujuan per fitur sudah ditulis. Asisten hanya menerima ringkasan angka dan menjawab dalam bahasa aplikasi. Layanan server belum aktif.", "Deploy functions AI dan secret, verifikasi biaya, batas Free/Premium, privasi dan pesan error di HP sebelum dibuka untuk pengguna."),
    RoadmapFeature("FF-054", "Scan struk per item", "AI", RoadmapStatus.Planned,
        "Pengaturan AI Scan (nama pemilik rekening, detail struk yang ditulis ke catatan, urutan dengan tekan lama dan seret) sudah ada. Pembagian satu struk menjadi transaksi per item belum ada.", "Sesudah penyedia AI dengan input gambar aktif, periksa total/diskon/pajak/pembulatan dan konfirmasi pembagian sebelum menyimpan jurnal."),
    RoadmapFeature("FF-055", "Anggaran bersama dan keluarga", "Kolaborasi", RoadmapStatus.Planned,
        "Data saat ini milik satu akun atau satu penyimpanan lokal.", "Tambahkan ruang bersama, undangan, peran, izin data dan audit setelah sinkronisasi serta konflik aman."),
    RoadmapFeature("FF-056", "Perjalanan finansial personal", "Perencanaan", RoadmapStatus.Planned,
        "Target dan anggaran tersedia terpisah; rencana perjalanan finansial terpadu belum ada.", "Hubungkan tujuan dana darurat, bebas utang dan kebiasaan dengan milestone yang dapat disesuaikan pengguna."),
    RoadmapFeature("FF-057", "Bahasa antarmuka", "Lokalisasi", RoadmapStatus.InProgress,
        "Wizard pengaturan awal sudah tersedia dalam Bahasa Indonesia dan Inggris (55 teks); pilihan bahasa lain ditandai 'Segera' dan belum mengubah antarmuka.", "Terjemahkan halaman utama secara bertahap, lengkapi format tanggal/angka, dan sediakan pemilih bahasa di Pengaturan."),
    RoadmapFeature("FF-058", "Validasi dan rilis iOS", "Platform", RoadmapStatus.Planned,
        "Kode bersama dan layanan native iOS tersedia, tetapi perubahan terbaru belum dibangun atau diuji pada perangkat Apple.", "Build di macOS, uji autentikasi, penyimpanan lokal, foto, restore, aksesibilitas dan alur UI di iPhone/iPad sebelum distribusi."),
    RoadmapFeature("FF-059", "Distribusi resmi dan pembaruan", "Rilis", RoadmapStatus.Planned,
        "APK Android debug dapat dipasang. Belum ada tautan toko resmi atau pemeriksa pembaruan produksi.", "Siapkan signing rilis, kebijakan privasi, listing, jalur uji dan mekanisme pembaruan sebelum publikasi."),
    RoadmapFeature("FF-060", "Premium dan langganan", "Rilis", RoadmapStatus.InProgress,
        "Halaman Premium, status hak akses dari Firestore dan pemulihan pembelian sudah ditulis di aplikasi. Produk toko dan secret RevenueCat belum dikonfigurasi.", "Konfigurasikan produk toko dan secret, verifikasi pembelian dan pemulihan hak akses di backend, tanpa mengunci fitur secara semu."),
    RoadmapFeature("FF-061", "Leaderboard global dan per negara", "Komunitas", RoadmapStatus.Planned,
        "Kompetisi pencatatan bulanan global dan lokal per negara disepakati pengguna pada 3 November 2026. Halaman level menampilkan aturan hadiah; layanan peringkat belum aktif.",
        "Gunakan ledger aktivitas yang diverifikasi server, persetujuan profil publik dan pilihan negara. Tutup periode bulanan di server, cegah penggandaan poin lewat edit/impor/restore, dan tentukan aturan seri serta zona waktu kompetisi. Admin memverifikasi pemenang dan memberikan Premium gratis: peringkat 1 tiga bulan, peringkat 2 dua bulan, peringkat 3 satu bulan. Pemberian hak akses harus idempoten dan hanya melalui admin."),
    RoadmapFeature("FF-062", "Otomasi & Integrasi", "Integrasi", RoadmapStatus.Planned,
        "Menu Otomasi & Integrasi menampilkan kartu Segera Hadir seperti Ollo; belum ada otomasi atau integrasi eksternal.", "Tentukan alat eksternal dan pemicu otomasi yang konkret, izin data, token backend dan pembatalan sebelum membangunnya."),
)

fun roadmapPrompt(features: List<RoadmapFeature> = productRoadmap): String =
    "Peta Jalan Kasiva • $roadmapUpdated\n\n" + features.joinToString("\n\n---\n\n") { it.prompt() }

/*
 * Teks tampilan peta jalan. Data `RoadmapFeature` (judul, bagian, deskripsi, target) tetap berbahasa Indonesia
 * karena menjadi isi prompt yang disalin ke chat; layar menerjemahkan judul, bagian dan status.
 */
val RoadmapFeature.displayTitle: String get() = roadmapTitleLabel(id, title)
val RoadmapFeature.displayArea: String get() = roadmapAreaLabel(area)

fun roadmapTitleLabel(id: String, fallback: String): String = when (id) {
    "FF-001" -> MR.strings.rm_ff001_title.text()
    "FF-002" -> MR.strings.rm_ff002_title.text()
    "FF-003" -> MR.strings.rm_ff003_title.text()
    "FF-004" -> MR.strings.rm_ff004_title.text()
    "FF-005" -> MR.strings.rm_ff005_title.text()
    "FF-006" -> MR.strings.rm_ff006_title.text()
    "FF-007" -> MR.strings.rm_ff007_title.text()
    "FF-008" -> MR.strings.rm_ff008_title.text()
    "FF-009" -> MR.strings.rm_ff009_title.text()
    "FF-010" -> MR.strings.rm_ff010_title.text()
    "FF-011" -> MR.strings.rm_ff011_title.text()
    "FF-012" -> MR.strings.rm_ff012_title.text()
    "FF-013" -> MR.strings.rm_ff013_title.text()
    "FF-014" -> MR.strings.rm_ff014_title.text()
    "FF-015" -> MR.strings.rm_ff015_title.text()
    "FF-016" -> MR.strings.rm_ff016_title.text()
    "FF-017" -> MR.strings.rm_ff017_title.text()
    "FF-018" -> MR.strings.rm_ff018_title.text()
    "FF-019" -> MR.strings.rm_ff019_title.text()
    "FF-020" -> MR.strings.rm_ff020_title.text()
    "FF-021" -> MR.strings.rm_ff021_title.text()
    "FF-022" -> MR.strings.rm_ff022_title.text()
    "FF-023" -> MR.strings.rm_ff023_title.text()
    "FF-024" -> MR.strings.rm_ff024_title.text()
    "FF-025" -> MR.strings.rm_ff025_title.text()
    "FF-026" -> MR.strings.rm_ff026_title.text()
    "FF-027" -> MR.strings.rm_ff027_title.text()
    "FF-028" -> MR.strings.rm_ff028_title.text()
    "FF-029" -> MR.strings.rm_ff029_title.text()
    "FF-030" -> MR.strings.rm_ff030_title.text()
    "FF-031" -> MR.strings.rm_ff031_title.text()
    "FF-032" -> MR.strings.rm_ff032_title.text()
    "FF-033" -> MR.strings.rm_ff033_title.text()
    "FF-034" -> MR.strings.rm_ff034_title.text()
    "FF-035" -> MR.strings.rm_ff035_title.text()
    "FF-036" -> MR.strings.rm_ff036_title.text()
    "FF-037" -> MR.strings.rm_ff037_title.text()
    "FF-038" -> MR.strings.rm_ff038_title.text()
    "FF-039" -> MR.strings.rm_ff039_title.text()
    "FF-040" -> MR.strings.rm_ff040_title.text()
    "FF-041" -> MR.strings.rm_ff041_title.text()
    "FF-042" -> MR.strings.rm_ff042_title.text()
    "FF-043" -> MR.strings.rm_ff043_title.text()
    "FF-044" -> MR.strings.rm_ff044_title.text()
    "FF-045" -> MR.strings.rm_ff045_title.text()
    "FF-046" -> MR.strings.rm_ff046_title.text()
    "FF-047" -> MR.strings.rm_ff047_title.text()
    "FF-048" -> MR.strings.rm_ff048_title.text()
    "FF-049" -> MR.strings.rm_ff049_title.text()
    "FF-050" -> MR.strings.rm_ff050_title.text()
    "FF-051" -> MR.strings.rm_ff051_title.text()
    "FF-052" -> MR.strings.rm_ff052_title.text()
    "FF-053" -> MR.strings.rm_ff053_title.text()
    "FF-054" -> MR.strings.rm_ff054_title.text()
    "FF-055" -> MR.strings.rm_ff055_title.text()
    "FF-056" -> MR.strings.rm_ff056_title.text()
    "FF-057" -> MR.strings.rm_ff057_title.text()
    "FF-058" -> MR.strings.rm_ff058_title.text()
    "FF-059" -> MR.strings.rm_ff059_title.text()
    "FF-060" -> MR.strings.rm_ff060_title.text()
    "FF-061" -> MR.strings.rm_ff061_title.text()
    "FF-062" -> MR.strings.pd_rm_ff062_title.text()
    else -> fallback
}

fun roadmapAreaLabel(area: String): String = when (area) {
    "Identitas" -> MR.strings.rm_area_identity.text()
    "Akun" -> MR.strings.rm_area_account.text()
    "Pencatatan" -> MR.strings.rm_area_recording.text()
    "Dompet" -> MR.strings.rm_area_wallet.text()
    "Statistik" -> MR.strings.rm_area_statistics.text()
    "Perencanaan" -> MR.strings.rm_area_planning.text()
    "Aset" -> MR.strings.rm_area_assets.text()
    "Data" -> MR.strings.rm_area_data.text()
    "Integrasi" -> MR.strings.rm_area_integration.text()
    "Tampilan" -> MR.strings.rm_area_appearance.text()
    "Preferensi" -> MR.strings.rm_area_preferences.text()
    "Keamanan" -> MR.strings.rm_area_security.text()
    "Profil" -> MR.strings.rm_area_profile.text()
    "Sinkronisasi" -> MR.strings.rm_area_sync.text()
    "Aksesibilitas" -> MR.strings.rm_area_accessibility.text()
    "Notifikasi" -> MR.strings.rm_area_notifications.text()
    "Kualitas" -> MR.strings.rm_area_quality.text()
    "Media" -> MR.strings.rm_area_media.text()
    "Kolaborasi" -> MR.strings.rm_area_collaboration.text()
    "Lokalisasi" -> MR.strings.rm_area_localization.text()
    "Platform" -> MR.strings.rm_area_platform.text()
    "Rilis" -> MR.strings.rm_area_release.text()
    "Komunitas" -> MR.strings.rm_area_community.text()
    else -> area
}
