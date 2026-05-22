/* schema.sql */

CREATE TABLE kategori (
                          id_kategori SERIAL PRIMARY KEY,
                          nama_kategori VARCHAR(20) NOT NULL
);

CREATE TABLE pengguna (
                          nik VARCHAR(16) PRIMARY KEY,
                          nama VARCHAR(30) NOT NULL,
                          username VARCHAR(30) UNIQUE NOT NULL,
                          password VARCHAR(30) NOT NULL,
                          nomor_telepon VARCHAR(20),
                          email VARCHAR(30),
                          peran VARCHAR(20),
                          divisi VARCHAR(30),
                          status_aktif BOOLEAN DEFAULT TRUE,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE produk (
                        id_produk SERIAL PRIMARY KEY,
                        nama VARCHAR(30) NOT NULL,
                        kode VARCHAR(50) UNIQUE NOT NULL,
                        id_kategori INT REFERENCES kategori(id_kategori),
                        satuan VARCHAR(20),
                        deskripsi TEXT,
                        foto VARCHAR(50),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE target_produksi (
                                 id_target SERIAL PRIMARY KEY,
                                 id_produk INT REFERENCES produk(id_produk),
                                 nik_pengguna VARCHAR(16) REFERENCES pengguna(nik),
                                 jumlah_target INT,
                                 periode_target INT,
                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE produksi_harian (
                                 id_produksi SERIAL PRIMARY KEY,
                                 id_produk INT REFERENCES produk(id_produk),
                                 nik_operator VARCHAR(16) REFERENCES pengguna(nik),
                                 tanggal_produksi DATE,
                                 jumlah_aktual INT,
                                 jumlah_defect INT,
                                 kendala TEXT,
                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE history_log (
                             id_histori SERIAL PRIMARY KEY,
                             id_produk INT REFERENCES produk(id_produk),
                             nik_pengguna VARCHAR(16) REFERENCES pengguna(nik),
                             aksi VARCHAR(30),
                             timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                             keterangan TEXT
);