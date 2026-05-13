package controller;

import model.LaporanProduksi;
import model.Produk;
import model.ProduksiHarian;
import model.TargetProduksi;

import java.time.LocalDate;
import java.util.*;

/**
 * ReportController bertanggung jawab mengumpulkan data dari model Produk,
 * mengolahnya menjadi struktur LaporanProduksi.
 */
public class ReportController {

    private final Produk produkManager;

    public ReportController() {
        this.produkManager = new Produk();
    }

    public LaporanProduksi susunLaporan(List<Integer> idProdukList,
                                        LocalDate tglMulai,
                                        LocalDate tglSelesai) {

        List<Produk> daftarProduk = ambilDaftarProduk(idProdukList);
        List<LaporanProduksi.BarisTabel> barisTabel = new ArrayList<>();
        List<LaporanProduksi.PerformaProduk> performaList = new ArrayList<>();

        for (Produk produk : daftarProduk) {
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

    private LaporanProduksi.PerformaProduk hitungPerforma(Produk produk,
                                                          List<ProduksiHarian> listProduksi,
                                                          List<TargetProduksi> listTarget) {
        int totalProduksi = listProduksi.stream()
                .mapToInt(ProduksiHarian::getJumlahAktual)
                .sum();

        int totalDefect = listProduksi.stream()
                .mapToInt(ProduksiHarian::getJumlahDefect)
                .sum();

        double rasioDefect = totalProduksi > 0
                ? (double) totalDefect / totalProduksi * 100
                : 0.0;

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
} // <--- Pastikan kurung penutup kelas ini ikut ter-copy