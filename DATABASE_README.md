# Cara Setup Database SIMP

Biar aplikasi bisa jalan di lokal, perlu setup database PostgreSQL dulu. Ikuti langkah simpel di bawah:

### Prereq
* PostgreSQL (v14+ or terbaru lebih ok)
* pgAdmin
* Sudah clone repo project

### 1. Buat Database simp_db
* Buka pgAdmin dan login ke server
* Klik kanan pada Databases -> Create -> Database
* Masukkan nama simp_db, lalu Save

### 2. Import schema.sql
* Klik kanan pada database simp_db yang tadi dibuat -> pilih Query Tool.
* Load file schema.sql (lokasi: src/main/resources/schema.sql). Bisa langsung drag-and-drop file ke editor atau copy-paste isinya.
* Tekan F5 atau klik ikon Execute.
* Pastikan muncul notif "Query returned successfully". Cek folder Tables untuk memastikan tabel produk, kategori, dkk sudah muncul.

### 3. Setup File .env
Supaya aplikasi bisa akses database, perlu buat file konfigurasi:
* Di root folder project (sejajar dengan build.gradle), buat file baru dengan nama .env.
* Tambahkan baris enih:
    ```env
    DB_PASSWORD=password_postgres_masing_masing
    ```
* Ganti password_postgres_masing_masing dengan password yang dipakai saat instalasi PostgreSQL. contoh DB_PASSWORD=praktikum (di laptop gweh)

Catatan: File .env sudah masuk .gitignore jadi tidak akan ter-push ke GitHub. Setiap developer harus buat sendiri di lokal.

### 4. Verifikasi
* Run aplikasi via IntelliJ.
* Kalau tidak ada error Connection refused atau password authentication failed di console, berarti setup sudah aman.

Note: Kalau ada update struktur tabel (tambah kolom/tabel baru), kabari di grup sebelum update schema.sql.