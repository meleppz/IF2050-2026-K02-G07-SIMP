package model;

import java.time.LocalDate;
import java.util.List;

/**
 * Model untuk menampung hasil olahan data laporan produksi.
 * Berisi dua bagian: baris tabel utama dan ringkasan performa per produk.
 */
public class LaporanProduksi {

    // =========================================================
    // INNER CLASS — satu baris di tabel utama laporan
    // =========================================================
    public static class BarisTabel {
        private final int idProduk;
        private final String namaProduk;
        private final LocalDate tanggalProduksi;
        private final int jumlahProduksi;
        private final int jumlahDefect;

        public BarisTabel(int idProduk, String namaProduk,
                          LocalDate tanggalProduksi,
                          int jumlahProduksi, int jumlahDefect) {
            this.idProduk = idProduk;
            this.namaProduk = namaProduk;
            this.tanggalProduksi = tanggalProduksi;
            this.jumlahProduksi = jumlahProduksi;
            this.jumlahDefect = jumlahDefect;
        }

        public int getIdProduk()              { return idProduk; }
        public String getNamaProduk()         { return namaProduk; }
        public LocalDate getTanggalProduksi() { return tanggalProduksi; }
        public int getJumlahProduksi()        { return jumlahProduksi; }
        public int getJumlahDefect()          { return jumlahDefect; }
    }

    // =========================================================
    // INNER CLASS — satu baris di tabel performa (ringkasan)
    // =========================================================
    public static class PerformaProduk {
        private final int idProduk;
        private final String namaProduk;
        private final int totalProduksi;
        private final int totalDefect;
        private final double rasioDefect;      // (totalDefect / totalProduksi) * 100
        private final Double rasioVsTarget;    // null jika tidak ada target

        public PerformaProduk(int idProduk, String namaProduk,
                              int totalProduksi, int totalDefect,
                              double rasioDefect, Double rasioVsTarget) {
            this.idProduk = idProduk;
            this.namaProduk = namaProduk;
            this.totalProduksi = totalProduksi;
            this.totalDefect = totalDefect;
            this.rasioDefect = rasioDefect;
            this.rasioVsTarget = rasioVsTarget;
        }

        public int getIdProduk()           { return idProduk; }
        public String getNamaProduk()      { return namaProduk; }
        public int getTotalProduksi()      { return totalProduksi; }
        public int getTotalDefect()        { return totalDefect; }
        public double getRasioDefect()     { return rasioDefect; }
        public Double getRasioVsTarget()   { return rasioVsTarget; } // nullable
    }

    // =========================================================
    // FIELD UTAMA LaporanProduksi
    // =========================================================
    private final List<BarisTabel> barisTabel;
    private final List<PerformaProduk> performaList;
    private final LocalDate tglMulai;
    private final LocalDate tglSelesai;
    private final LocalDate tglGenerate;

    public LaporanProduksi(List<BarisTabel> barisTabel,
                           List<PerformaProduk> performaList,
                           LocalDate tglMulai,
                           LocalDate tglSelesai) {
        this.barisTabel = barisTabel;
        this.performaList = performaList;
        this.tglMulai = tglMulai;
        this.tglSelesai = tglSelesai;
        this.tglGenerate = LocalDate.now();
    }

    public List<BarisTabel> getBarisTabel()       { return barisTabel; }
    public List<PerformaProduk> getPerformaList() { return performaList; }
    public LocalDate getTglMulai()                { return tglMulai; }
    public LocalDate getTglSelesai()              { return tglSelesai; }
    public LocalDate getTglGenerate()             { return tglGenerate; }
}