package controller;

import model.LaporanProduksi;
import model.Produk;
import model.ProduksiHarian;
import model.TargetProduksi;

import java.time.LocalDate;
import java.util.*;

/**
 * ReportController bertanggung jawab mengumpulkan data dari model Produk,
 * mengolahnya menjadi struktur LaporanProduksi, lalu menyerahkan ke FileGeneratorService.
 */
public class ReportController {

    // Menggunakan instance Produk karena repository sudah dimerge ke dalam kelas Produk
    private final Produk produkManager;

    public ReportController() {
        this.produkManager = new Produk();
    }

    // =========================================================
    // ENTRY POINT — dipanggil dari EksporLaporanView
    // =========================================================

    public LaporanProduksi susunLaporan(List<Integer> idProdukList,
                                        LocalDate tglMulai,
                                        LocalDate tglSelesai) {

        // 1. Ambil data produk melalui produkManager
        List<Produk> daftarProduk = ambilDaftarProduk(idProdukList);

        // 2. Kumpulkan baris tabel & data performa
        List<LaporanProduksi.BarisTabel> barisTabel = new ArrayList<>();
        List<LaporanProduksi.PerformaProduk> performaList = new ArrayList<>();

        for (Produk produk : daftarProduk) {
            // Memanggil method query yang sekarang ada di kelas Produk
            List<ProduksiHarian> listProduksi = produkManager
                    .getProduksiByFilter(produk.getIdProduk(), tglMulai, tglSelesai);

            List<TargetProduksi> listTarget = produkManager
                    .getTargetByFilter(produk.getIdProduk(), tglMulai, tglSelesai);

            if (listProduksi.isEmpty()) {
                barisTabel.add(new LaporanProduksi.BarisTabel(
                        produk.getIdProduk(),
                        produk.getNama(),
                        null,
                        0, 0
                ));
            } else {
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
            return produkManager.getAllProduk();
        }
        List<Produk> hasil = new ArrayList<>();
        for (int id : idProdukList) {
            Produk p = produkManager.getProdukById(id);
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
                .