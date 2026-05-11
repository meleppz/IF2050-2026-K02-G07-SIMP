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

    // Produk
    public List<Produk> getAllProduk() {
        return Produk.getAll();
    }

    public Produk getProdukById(int idProduk) {
        return Produk.getById(idProduk);
    }

    public Produk tambahProduk(ProdukData data, String namaKategori) {
        Produk produk = Produk.create(data);

        // Gunakan logic dinamis: ambil objek Kategori berdasarkan String
        Kategori kat = Kategori.getOrCreate(namaKategori);
        produk.setKategori(kat);

        produk.save();
        return produk;
    }

    public boolean editProduk(int idProduk, ProdukData data, String namaKategori) {
        Produk produk = Produk.getById(idProduk);
        if (produk == null) return false;

        // Update kategori jika ada input
        if (namaKategori != null && !namaKategori.isEmpty()) {
            Kategori kat = Kategori.getOrCreate(namaKategori);
            produk.setKategori(kat);
        }

        produk.update(data);
        return true;
    }

    public boolean hapusProduk(int idProduk) {
        Produk produk = Produk.getById(idProduk);
        if (produk == null) {
            return false;
        }
        produk.delete();
        HistoryLog.catatAksi("DELETE", produk);
        return true;
    }

    // Produksi Harian
    public List<ProduksiHarian> getAllProduksiHarian() {
        return ProduksiHarian.getAll();
    }

    public ProduksiHarian getProduksiHarianById(int idProduksi) {
        return ProduksiHarian.getById(idProduksi);
    }

    public List<ProduksiHarian> getProduksiHarianByProduk(int idProduk) {
        return ProduksiHarian.getByIdProduk(idProduk);
    }

    public List<ProduksiHarian> getProduksiHarianByDateRange(LocalDate dari, LocalDate sampai) {
        return ProduksiHarian.getByDateRange(dari, sampai);
    }

    public boolean tambahProduksiHarian(ProduksiHarian produksi) {
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

    // Target Produksi
    public List<TargetProduksi> getAllTarget() {
        return TargetProduksi.getAll();
    }

    public TargetProduksi getTargetById(int idTarget) {
        return TargetProduksi.getById(idTarget);
    }

    public List<TargetProduksi> getTargetByProduk(int idProduk) {
        return TargetProduksi.getByIdProduk(idProduk);
    }

    public TargetProduksi getTargetAktif(int idProduk, LocalDate tanggal) {
        return TargetProduksi.getAktifPadaTanggal(idProduk, tanggal);
    }

    public boolean tambahTarget(TargetProduksi target) {
        return target.save();
    }

    public boolean editTarget(TargetProduksi target) {
        return target.update();
    }

    public boolean hapusTarget(int idTarget) {
        return TargetProduksi.deleteById(idTarget);
    }
}
