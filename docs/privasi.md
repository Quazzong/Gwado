# Kebijakan Privasi Gwado

Berlaku sejak 10 November 2026.

**Singkatnya:** semua data Gwado tinggal di HP-mu. Gwado tidak punya server, tidak memakai analitik atau iklan, dan tidak mengirim data ke siapa pun selain situs yang kamu sambungkan.

Gwado dibuat perorangan oleh quazzong sebagai proyek terbuka ([kode sumber](https://github.com/Quazzong/Gwado)). Gwado bukan aplikasi resmi Universitas Brawijaya maupun Google.

## Data yang diambil

**Dari Brone dan Eling (Moodle):** daftar mata kuliah yang kamu ikuti; tugas dan kuis (judul, deskripsi, deadline, tautan); status pengumpulan dan tanda selesai aktivitas. Nomor pengguna Moodle dipakai untuk meminta tanda selesai, tapi tidak disimpan.

**Dari Google Classroom**, hanya dengan dua izin baca:

- `classroom.courses.readonly`: daftar kelasmu.
- `classroom.coursework.me.readonly`: tugas di kelas itu (judul, deskripsi, deadline, tautan) dan status pengumpulanmu.

Gwado tidak bisa mengubah, mengumpulkan, atau menghapus apa pun di Classroom maupun Moodle.

**Yang kamu masukkan sendiri:** centang, tugas tambahan beserta catatannya, semester, tanggal kuliah pertama, mata kuliah yang diikuti atau diabaikan, dan pengaturan tampilan.

## Untuk apa data dipakai

Hanya untuk menampilkan daftar tugas, kalender, widget, dan pengingat di HP-mu. Data tidak dipakai untuk iklan, tidak dijual, dan tidak dibagikan ke pihak lain.

## Di mana data disimpan

- Di penyimpanan pribadi aplikasi di HP-mu. Tidak ada salinan di server mana pun, dan pembuat Gwado tidak bisa melihat datamu.
- Data Gwado tidak ikut cadangan Google dan tidak ikut dipindahkan saat ganti HP.
- Gwado tidak memakai analitik, pelacak, atau laporan crash otomatis.
- Daftar tugas tidak dienkripsi tersendiri; perlindungannya bergantung pada Android dan kunci layar HP.

## Login dan token

- Password tidak pernah dilihat Gwado. Login Brone dan Eling dilakukan di browser, di halaman login kampus; Gwado hanya menerima token akses dari situs.
- Token Brone dan Eling disimpan terenkripsi dengan kunci dari Android Keystore, dan tidak dicatat di log.
- Untuk Google, Gwado tidak menyimpan token. Akses diurus Google Play services di HP-mu.

## Siapa yang dihubungi Gwado

Hanya `brone.ub.ac.id`, `eling.ub.ac.id`, dan Google (`classroom.googleapis.com`, Google Play services), sesuai layanan yang kamu sambungkan.

## Izin Android

| Izin | Untuk |
|---|---|
| Internet | Mengambil tugas dari situs yang disambungkan |
| Notifikasi | Pengingat deadline |
| Alarm dan pengingat | Pengingat dan alarm tepat waktu |
| Berjalan setelah HP menyala | Memasang ulang pengingat setelah HP dinyalakan ulang |
| Getar, mencegah tidur, layanan latar | Alarm 3 jam sebelum deadline |

Gwado tidak meminta izin lokasi, kontak, kamera, mikrofon, atau berkas.

Pengingat menampilkan judul tugas, dan Android bisa menampilkannya di layar kunci. Atur di **Pengaturan → Notifikasi** kalau tidak ingin terlihat.

## Menghapus data

- **Lupakan login** (menu ⋯ di kartu layanan) menghapus token Brone atau Eling dari HP.
- **Matikan layanan** berhenti mengambil dari layanan itu; untuk Classroom, izin Google sekaligus dicabut.
- Menghapus data aplikasi atau mencopot Gwado menghapus semua data Gwado di HP.
- Izin Google bisa dicabut di [myaccount.google.com/permissions](https://myaccount.google.com/permissions); token Moodle di halaman **Kunci keamanan** pada preferensi akun Moodle.

## Data pengguna Google

Penggunaan dan pemindahan informasi yang diterima Gwado dari Google API mematuhi [Kebijakan Data Pengguna Layanan Google API](https://developers.google.com/terms/api-services-user-data-policy), termasuk persyaratan Penggunaan Terbatas (*Limited Use*).

## Perubahan

Kalau cara Gwado memakai data berubah, halaman ini diperbarui beserta tanggal berlakunya.

## Kontak

Tulis di [GitHub Issues](https://github.com/Quazzong/Gwado/issues). Issues bisa dibaca publik, jadi jangan menulis token, NIM, atau data pribadi lain di sana.
