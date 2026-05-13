package controller;

import model.LaporanProduksi;
import model.Produk;
import model.ProduksiHarian;
import model.TargetProduksi;
import repository.ProdukRepository;

import java.time.LocalDate;
import java.util.*;

/**
 * ReportController bertanggung jawab mengumpulkan data dari repository/model,
 * mengolahnya menjadi struktur LaporanProduksi, lalu menyerahkan ke FileGeneratorService.
 *
 * Kelas ini TIDAK tahu soal JavaFX — murni logika bisnis.
 */
public class ReportController {

    private final ProdukRepository produkRepository;

    public ReportController() {
        this.produkRepository = new ProdukRepository();
    }

    // =========================================================
    // ENTRY POINT — dipanggil dari EksporLaporanView
    // =========================================================

    /**
     * Mengumpulkan dan menyusun data laporan berdasarkan filter.
     *
     * @param idProdukList  list id produk yang dipilih user. Jika null atau kosong = semua produk.
     * @param tglMulai      tanggal awal rentang laporan
     * @param tglSelesai    tanggal akhir rentang laporan
     * @return LaporanProduksi yang siap diserahkan ke FileGeneratorService
     * @throws IllegalArgumentException jika tidak ada data sama sekali
     */
    public LaporanProduksi susunLaporan(List<Integer> idProdukList,
                                        LocalDate tglMulai,
                                        LocalDate tglSelesai) {

        // 1. Ambil data produk
        List<Produk> daftarProduk = ambilDaftarProduk(idProdukList);

        // 2. Kumpulkan baris tabel & data performa
        List<LaporanProduksi.BarisTabel> barisTabel = new ArrayList<>();
        List<LaporanProduksi.PerformaProduk> performaList = new ArrayList<>();

        for (Produk produk : daftarProduk) {
            // Ambil semua data produksi harian produk ini di rentang waktu
            List<ProduksiHarian> listProduksi = produkRepository
                    .getProduksiByFilter(produk.getIdProduk(), tglMulai, tglSelesai);

            // Ambil target produksi (untuk hitung rasio vs target)
            List<TargetProduksi> listTarget = produkRepository
                    .getTargetByFilter(produk.getIdProduk(), tglMulai, tglSelesai);

            // Susun baris tabel — satu baris per data harian
            // Tetap masukkan produk meskipun data kosong (isi 0)
            if (listProduksi.isEmpty()) {
                barisTabel.add(new LaporanProduksi.BarisTabel(
                        produk.getIdProduk(),
                        produk.getNama(),
                        null,   // null = tidak ada data di rentang ini
                        0, 0
                ));
            } else {
                // Urutkan per tanggal ascending sebelum masuk tabel
                listProduksi.sort(Comparator.comparing(ProduksiHarian::getTanggalProduksi));
                for (ProduksiHarian ph : listProduksi) {
                    barisTabel.add(new LaporanProduksi.BarisTabel(
                            produk.getIdProduk(),
                            produk.getNama(),
                            ph.getTanggalProduksi(),
                            ph.getJumlahAktual(),
                            ph.getJumlahDefect()
                    ));
                }
            }

            // Hitung performa produk ini
            LaporanProduksi.PerformaProduk performa = hitungPerforma(produk, listProduksi, listTarget);
            performaList.add(performa);
        }

        return new LaporanProduksi(barisTabel, performaList, tglMulai, tglSelesai);
    }

    // =========================================================
    // HELPER — ambil produk
    // =========================================================

    private List<Produk> ambilDaftarProduk(List<Integer> idProdukList) {
        if (idProdukList == null || idProdukList.isEmpty()) {
            // "Semua Produk" dipilih
            return produkRepository.getAllProduk();
        }
        List<Produk> hasil = new ArrayList<>();
        for (int id : idProdukList) {
            Produk p = produkRepository.getProdukById(id);
            if (p != null) hasil.add(p);
        }
        return hasil;
    }

    // =========================================================
    // HELPER — hitung performa satu produk
    // =========================================================

    private LaporanProduksi.PerformaProduk hitungPerforma(Produk produk,
                                                          List<ProduksiHarian> listProduksi,
                                                          List<TargetProduksi> listTarget) {
        int totalProduksi = listProduksi.stream()
                .mapToInt(ProduksiHarian::getJumlahAktual)
                .sum();

        int totalDefect = listProduksi.stream()
                .mapToInt(ProduksiHarian::getJumlahDefect)
                .sum();

        // rasioDefect = (defect / produksi) * 100, hindari division by zero
        double rasioDefect = totalProduksi > 0
                ? (double) totalDefect / totalProduksi * 100
                : 0.0;

        // rasioVsTarget = (totalProduksi / totalTarget) * 100
        // null jika tidak ada target sama sekali
        Double rasioVsTarget = null;
        int totalTarget = listTarget.stream()
                .mapToInt(TargetProduksi::getJumlahTarget)
                .sum();
        if (totalTarget > 0) {
            rasioVsTarget = (double) totalProduksi / totalTarget * 100;
        }

        return new LaporanProduksi.PerformaProduk(
                produk.getIdProduk(),
                produk.getNama(),
                totalProduksi,
                totalDefect,
                rasioDefect,
                rasioVsTarget
        );
    }
}