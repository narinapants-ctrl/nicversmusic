# Nicver's Music (aplikasi musik Android)

Pemutar musik native Android (Kotlin + Jetpack Compose) dari simulator MP3 kamu.
Saat lagu diputar, kontrolnya muncul otomatis di **Panel Kontrol / layar kunci** (sampul, judul, artis, sebelumnya / jeda / berikutnya).

## Fitur
- Tab: **Lagu, Album, Artis, Genre, Favorit, Sering (paling sering diputar), MV**
- **Genre** dengan tempat khusus **"Lainnya"** untuk genre kosong / tidak dikenal. Genre bisa diubah per lagu (disimpan di aplikasi, file lagu tidak disentuh).
- **MV**: video klip bisa ditonton langsung di aplikasi (layar penuh juga bisa).
  - Otomatis: simpan video dengan **nama yang sama** dengan lagunya (mis. `Lagu.mp3` dan `Lagu.mp4` di folder yang sama), atau nama videonya mengandung judul / "Artis - Judul" (tulisan seperti `(Official Video)` diabaikan).
  - Manual: menu ⋮ pada lagu, lalu **Pilih file MV manual**.
  - Keluar dari layar MV, lagu lanjut sebagai audio. Lagu berikutnya yang punya MV ikut tampil sebagai video selama layar MV terbuka.
- **Metronom** (30-300 BPM, 2/3/4/6 ketukan, ketuk tempo), bisa berbunyi bersamaan dengan musik.
- Pewaktu tidur, pencarian, tema Terang / Gelap / Sistem, 6 warna aksen, logo Nicver's saat dibuka.
- Dihapus dari versi simulator: kalkulator, dadu, jam / timer / stopwatch, foto, ebook, baterai, tema SD.

## Cara membuat APK (tanpa Android Studio)
1. Buat akun di https://github.com (kalau belum punya) lalu buat **repository baru** (boleh Private).
2. Upload **seluruh isi** folder ini ke repository tersebut (tombol *Add file > Upload files*; pastikan folder `.github` ikut terupload. Kalau folder tersembunyi tidak ikut, buat file lewat *Add file > Create new file* dengan nama `.github/workflows/build-apk.yml` lalu salin isinya).
3. Buka tab **Actions**. Pilih workflow **Build APK** lalu klik **Run workflow** (atau otomatis jalan saat upload ke branch `main`).
4. Tunggu sekitar 5-10 menit sampai hijau. Klik run tersebut, di bagian **Artifacts** unduh **NicversMusic-debug-apk** (zip).
5. Ekstrak zip, kirim file `app-debug.apk` ke HP, buka, lalu izinkan *instal dari sumber ini*.
6. Buka aplikasi dan beri izin (musik, video untuk MV, notifikasi).

Kalau build gagal (merah), buka run tersebut, salin pesan error dari langkah **Build debug APK**, lalu kirim ke Claude untuk diperbaiki.

## Catatan
- Kontrol di Panel Kontrol butuh izin notifikasi (Android 13+).
- Tampilan sampul di Panel Kontrol bisa berbeda antar merek HP.
- Lirik (.lrc) belum ada di versi ini.
- Hitungan "Sering diputar" bertambah setelah lagu diputar 30 detik.
