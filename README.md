# Gwado

Tugas kuliahku tersebar di tiga tempat: Brone, Eling, dan Google Classroom. Capek bukanya satu-satu, jadi aku bikin Gwado: aplikasi Android yang ngumpulin semua tugas itu ke satu daftar, lengkap dengan deadline, kalender, pengingat, dan widget.

Gwado nggak pakai server dan nggak perlu bikin akun. Tugas diambil langsung dari HP-mu dan datanya juga cuma ada di HP-mu.

**[Unduh APK](https://github.com/Quazzong/Gwado/releases)** · [Kebijakan privasi](docs/privasi.md)

<p>
  <img src="docs/gambar/beranda.png" alt="Beranda" width="240">
  <img src="docs/gambar/detail.png" alt="Detail tugas" width="240">
  <img src="docs/gambar/kalender.png" alt="Kalender" width="240">
</p>
<p><img src="docs/gambar/widget.png" alt="Widget" width="400"></p>

## Isinya

- Tugas diambil sendiri beberapa kali sehari. Mau sekarang juga? Tarik beranda ke bawah.
- Yang sudah lewat atau sisa kurang dari dua hari masuk bagian **Mendesak**.
- Tugas yang sudah kamu kumpulkan di situs otomatis dianggap beres.
- Pengingat H-3 dan H-1, ringkasan tiap jam 07.00, dan alarm yang bunyi 3 jam sebelum deadline.
- Kalender deadline dan dua widget.
- Tugas dari grup WA bisa dicatat sendiri.
- Tema terang/gelap dan warnanya bisa diganti.

## Cara pasang

1. Unduh `.apk` terbaru dari [Releases](https://github.com/Quazzong/Gwado/releases), lalu buka. Android bakal minta izin pasang dari sumber tak dikenal.
2. Sambungkan Brone dan Eling: login kampus lewat browser. Kalau HP nanya mau dibuka pakai apa, pilih **Gwado**.
3. Sambungkan Classroom: pilih akun Google kuliahmu, lalu izinkan.
4. Cek semesternya, lalu tekan **Mulai**.

> **Waktu menyambungkan Classroom, Google bakal bilang Gwado belum diverifikasi.** Itu wajar, karena Gwado belum lewat proses verifikasi Google. Ketuk **Lanjutan**, lalu **Buka Gwado**. Gwado cuma bisa membaca kelas dan tugasmu, nggak bisa mengubah apa pun.

## Tips

- **Nyalakan Jangan Ganggu.** Maka alarm nggak bakal bunyi, Notifikasi tetap masuk.
- Alarm yang lagi bunyi bisa dimatikan pakai tombol power atau volume.
- Di Android 14 ke atas, izinkan **Alarm & pengingat** untuk Gwado biar pengingatnya tepat waktu.

## Yang perlu kamu tahu

- Minimal Android 10.
- Cuma untuk Brone, Eling (UB), dan Google Classroom. Ini proyek pribadi, bukan aplikasi resmi kampus atau Google.
- Data nggak ikut cadangan Google. Kalau ganti HP, tugas bisa diambil ulang, tapi centang dan tugas yang kamu catat sendiri hilang.
- Pengambilan otomatis kadang telat, karena Android menghemat baterai. Beberapa merek HP juga suka mematikan aplikasi di latar.
- Login Brone/Eling memakai alamat `moodlemobile://`, sama kayak aplikasi Moodle resmi. Aplikasi lain bisa ikut mendaftarkan alamat itu, jadi pastikan yang kamu pilih **Gwado**.
- Database tugas nggak dienkripsi (sama seperti aplikasi Moodle resmi), tapi tersimpan di folder privat aplikasi. Token login Brone/Eling dienkripsi pakai Android Keystore. Token Google dipegang Google Play services, Gwado nggak menyimpannya.
- Judul tugas bisa kelihatan di notifikasi layar kunci, tergantung pengaturan HP-mu.

## Buat yang mau ngoprek

Kotlin + Jetpack Compose dengan komponen tampilan buatan sendiri, Navigation 3, Glance untuk widget, WorkManager untuk ambil di latar, dan `AlarmManager` untuk pengingat. Database pakai SQLite bawaan aplikasi (`androidx.sqlite` + `BundledSQLiteDriver`), dengan SQL mentah dan migrasi di `app/src/main/resources/migrasi/`, tanpa Room. HTTP cukup `HttpURLConnection`.

| Paket | Isi |
|---|---|
| `sumber/` | Parser jawaban Moodle dan Classroom |
| `fetch/` | HTTP, klien Moodle/Google, jendela hindari + jeda |
| `ingest/`, `db/` | Simpan hasil ambil ke database, migrasi |
| `tugas/`, `kalender/`, `waktu/`, `segar/`, `semester/` | Daftar, detail, tugas manual, kalender, label waktu, status sumber |
| `layanan/`, `masuk/` | Login Moodle, izin Google, alur Ambil |
| `penjadwal/`, `pengingat/`, `widget/` | Ambil di latar, notifikasi + alarm, widget |
| `ui/` | Tema, komponen, layar |

Logika intinya (parser, ingest, aturan semester, kalender) adalah terjemahan dari versi Python yang kupakai sendiri. Biar hasilnya pasti sama, ada **golden file** di `app/src/test/resources/kontrak/`: contoh jawaban API (karangan) beserta hasil yang seharusnya keluar. Test Kotlin harus cocok persis. File di folder itu jangan diedit manual.

```sh
./gradlew :app:testDebugUnitTest   # unit test, nggak butuh HP
./gradlew :app:installDebug        # pasang ke HP lewat USB
```

Build rilis butuh `keystore.properties` di root repo (nggak ikut repo). Tanpa itu, APK rilisnya nggak bertanda tangan.

Hati-hati dengan task Gradle yang memakai perangkat (`connected*`, `generate*BaselineProfile`): task itu meng-uninstall aplikasi di semua HP yang tersambung. Cabut dulu HP yang datanya mau kamu jaga.

Kodenya kutulis bareng Claude (asisten AI). Rancangan, keputusan, desain, dan pengujian di HP tetap kukerjakan sendiri.

## Lisensi

Lisensi kodenya belum kutentukan. Font Fredoka dan Nunito pakai SIL Open Font License, ikon Tabler Icons pakai MIT; teks lisensinya ada di `app/src/main/assets/lisensi/`.

## Kontak

Ada masalah atau pertanyaan? Tulis di [Issues](https://github.com/Quazzong/Gwado/issues). Issues bisa dibaca siapa saja, jadi jangan tulis token, NIM, atau data pribadi di sana.
