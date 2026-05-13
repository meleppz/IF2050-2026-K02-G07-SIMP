package controller;

import model.HistoryLog;
import model.Kategori;
import model.Produk;
import model.ProdukData;
import model.ProduksiHarian;
import model.TargetProduksi;

import java.time.LocalDate;
import java.util.List;

public class ProdukController {

    // =========================================================
    // PRODUK — CORE OPERATIONS
    // =========================================================

    public List<Produk> getAllProduk() {
        return Produk.getAll();
    }

    public Produk getProdukById(int idProduk) {
        return Produk.getById(idProduk);
    }

    public Produk tambahProduk(ProdukData data, String namaKategori) {
        // 1. Buat instance produk dari data input
        Produk produk = Produk.create(data);

        // 2. Set Kategori (Dinamis: cari yang ada atau buat baru)
        Kategori kat = Kategori.getOrCreate(namaKategori);
        produk.setKategori(kat);

        // 3. Simpan ke database
        boolean berhasil = produk.save();

        if (berhasil) {
            // 4. Catat ke History Log hanya jika simpan berhasil
            // Pastikan produk.save() sudah mengisi idProduk ke objek produk
            HistoryLog.catatAksi("CREATE", produk);
            return produk;
        }

        return null;
    }

    public boolean editProduk(int idProduk, ProdukData data, String namaKategori) {
        Produk produk = Produk.getById(idProduk);
        if (produk == null) return false;

        // 1. Update kategori jika ada perubahan input
        if (namaKategori != null && !namaKategori.isEmpty()) {
            Kategori kat = Kategori.getOrCreate(namaKategori);
            produk.setKategori(kat);
        }

        // 2. Jalankan update data ke database
        boolean berhasil = produk.update(data);

        if (berhasil) {
            // 3. Catat ke History Log
            HistoryLog.catatAksi("UPDATE", produk);
            return true;
        }

        return false;
    }

    public boolean hapusProduk(int idProduk) {
        Produk produk = Produk.getById(idProduk);
        if (produk == null) return false;

        try {
            // 1. Hapus data relasi yang bergantung secara manual (jika tidak ada ON DELETE CASCADE)

            // Hapus target produksi terkait
            List<TargetProduksi> listTarget = TargetProduksi.getByIdProduk(idProduk);
            for (TargetProduksi target : listTarget) {
                TargetProduksi.deleteById(target.getIdTarget());
            }

            // Hapus produksi harian terkait
            List<ProduksiHarian> listProduksi = ProduksiHarian.getByIdProduk(idProduk);
            for (ProduksiHarian produksi : listProduksi) {
                ProduksiHarian.deleteById(produksi.getIdProduksi());
            }

            // 2. Catat Log SEBELUM produk benar-benar hilang dari tabel
            // Ini penting agar informasi produk masih lengkap saat dicatat
            HistoryLog.catatAksi("DELETE", produk);

            // 3. Hapus produk secara permanen
            return produk.delete();

        } catch (Exception e) {
            System.err.println("Gagal menghapus produk: " + e.getMessage());
            return false;
        }
    }

    // =========================================================
    // PRODUKSI HARIAN
    // =========================================================

    public List<ProduksiHarian> getAllProduksiHarian() {
        return ProduksiHarian.getAll();
    }

    public ProduksiHarian getProduksiHarianById(int idProduksi) {
        return ProduksiHarian.getById(idProduksi);
    }

    public List<ProduksiHarian> getProduksiHarianByProduk(int idProduk) {
        return ProduksiHarian.getByIdProduk(idProduk);
    }

    public boolean tambahProduksiHarian(ProduksiHarian produksi) {
        // Validasi: pastikan produknya ada
        if (Produk.getById(produksi.getIdProduk()) == null) {
            return false;
        }
        return produksi.save();
    }

    public boolean editProduksiHarian(ProduksiHarian produksi) {
        return produksi.update();
    }

    public boolean hapusProduksiHarian(int idProduksi) {
        return ProduksiHarian.deleteById(idProduksi);
    }

    // =========================================================
    // TARGET PRODUKSI
    // =========================================================

    public List<TargetProduksi> getAllTarget() {
        return TargetProduksi.getAll();
    }

    public TargetProduksi getTargetAktif(int idProduk, LocalDate tanggal) {
        return TargetProduksi.getAktifPadaTanggal(idProduk, tanggal);
    }

    public boolean tambahTarget(TargetProduksi target) {
        // Validasi: pastikan produknya ada
        if (Produk.getById(target.getIdProduk()) == null) {
            return false;
        }
        return target.save();
    }

    public boolean editTarget(TargetProduksi target) {
        return target.update();
    }

    public boolean hapusTarget(int idTarget) {
        return TargetProduksi.deleteById(idTarget);
    }
}