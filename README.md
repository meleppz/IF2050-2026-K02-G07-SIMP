# SIMP — Sistem Informasi Monitoring Produksi

Deskripsi singkat
-----------------
SIMP (Sistem Informasi Monitoring Produksi) adalah aplikasi desktop berbasis JavaFX untuk memantau dan mengelola proses produksi pabrik/plant secara lokal. Aplikasi ini menyediakan fitur autentikasi pengguna, manajemen produk, pencatatan produksi harian, pengaturan target produksi, histori aktivitas (audit log), serta penyusunan dan ekspor laporan (Excel/PDF).

Highlight
---------
- Tampilan: JavaFX + FXML (`src/main/res/view/*.fxml`).
- Database: PostgreSQL (skema tersedia di `src/main/res/schema.sql`).
- Ekspor laporan: Apache POI (Excel) dan iText (PDF).
- Konfigurasi rahasia lewat `.env` (menggunakan `dotenv-java`).
- Arsitektur modular: `controller` (business logic), `model` (entitas & operasi DB), `util` (koneksi DB, session), `view` (FXML + controller).

Fitur utama
-----------
- Autentikasi (login & registrasi).
- CRUD produk (tambah / edit / hapus), kategori otomatis dibuat bila belum ada.
- Pencatatan produksi harian (operator, tanggal, jumlah aktual, defect, kendala).
- Target produksi per produk.
- Penyusunan laporan periode (filter tanggal & produk) serta ekspor ke Excel/PDF.
- History log untuk audit (CREATE / UPDATE / DELETE) dengan pencatatan NIK pengguna.

Perubahan yang Dilakukan setelah Release Pertama
-----------
- Logika penyimpanan update data target pada produkViewController.
- Tambah panel pesan eror ketika ekspor laporan produk yang tidak ada datanya.
- Polishing UI yang masih berantakan (screen flexibility dan margin, pesan penyambut user pada screen ekspor laporan, header tabel produksi harian, gambar logo diubah jadi HD).
- Fix eror saat klik menu Ekspor Laporan.
- Fix data type query SQL.

Struktur proyek (singkat)
------------------------
- `src/main/java/`
  - `controller/` — logika aplikasi (mis. `AuthController`, `ProdukController`, `ReportController`).
  - `model/` — entitas & operasi DB (mis. `Produk`, `ProduksiHarian`, `Pengguna`, `Kategori`).
  - `util/` — utilitas seperti `DBConnection`, `Session`.
  - `view/` — `MainApp.java` (entry point) dan controller untuk FXML.
- `src/main/res/` — resource: `view/` (FXML), `schema.sql`, `fonts/`, `images/`.
- `build.gradle` — konfigurasi build (JavaFX plugin, dependencies).

Teknologi & dependensi utama
---------------------------
- Java (direkomendasikan JDK 24; `build.gradle` mengatur JavaFX versi 24).
- JavaFX (modules: `javafx.controls`, `javafx.fxml`).
- PostgreSQL JDBC driver.
- `dotenv-java` untuk membaca `.env`.
- `org.apache.poi:poi-ooxml` untuk Excel.
- `iText 7` untuk PDF.

Persiapan (prasyarat)
---------------------
1. Install JDK yang kompatibel (disarankan JDK 24).
2. Install PostgreSQL dan pastikan service berjalan.
3. Pastikan `JAVA_HOME` diset ke JDK yang benar.

Setup Database
--------------
1. Buat database PostgreSQL (contoh `simp_db`):

```powershell
# Masuk ke psql (ganti user jika perlu)
psql -U postgres
# Di dalam psql:
CREATE DATABASE simp_db;
\c simp_db
```

2. Jalankan skema SQL yang ada di `src/main/res/schema.sql`:

```powershell
psql -U postgres -d simp_db -f "D:\SIMP\src\main\res\schema.sql"
```

Konfigurasi koneksi DB (.env)
-----------------------------
Proyek memanfaatkan `dotenv-java`. Secara default `DBConnection.java` mengambil password dari `.env`, sedangkan URL dan USER masih hardcode.

Buat file `.env` di root proyek `D:\SIMP\.env` dengan minimal isi:

```
DB_PASSWORD=your_postgres_password_here
```

Jika Anda butuh mengganti host/port/user, edit `src/main/java/util/DBConnection.java` atau tambahkan variabel `DB_URL` dan `DB_USER` di `.env` dan modifikasi kode untuk membacanya.

Build & Jalankan (Windows PowerShell)
------------------------------------
1. Build & run dengan Gradle wrapper (direkomendasikan):

```powershell
.\gradlew.bat build
.\gradlew.bat run
```

2. Membuat JAR/distribusi (opsional):

```powershell
.\gradlew.bat jar
.\gradlew.bat distZip
```

Catatan: Karena JavaFX modular, menjalankan JAR secara manual mungkin memerlukan pengaturan `--module-path` atau opsi khusus. Menggunakan `gradlew run` lebih mudah.

Menambahkan akun admin (contoh)
------------------------------
Tidak ada pengguna default di skema. Password disimpan sebagai SHA-256 hex (lihat `AuthController.hashPassword()`). Contoh cara membuat hash di PowerShell:

```powershell
$pw = "admin123"
$hash = [System.BitConverter]::ToString((New-Object System.Security.Cryptography.SHA256Managed).ComputeHash([System.Text.Encoding]::UTF8.GetBytes($pw))).Replace("-","").ToLower()
$hash
```

Lalu masukkan pengguna ke DB (ganti `<HASH>`):

```sql
INSERT INTO pengguna (nik, nama, username, password, peran, status_aktif)
VALUES ('0000000000000001', 'Administrator', 'admin', '<HASH>', 'supervisor', true);
```
