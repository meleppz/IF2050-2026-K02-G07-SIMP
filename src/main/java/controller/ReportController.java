package controller;

import model.LaporanProduksi;
import model.Produk;
import model.ProduksiHarian;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class ReportController {

    // Sebaiknya inisialisasi ProdukController atau DAO di sini
    private final ProdukController produkController = new ProdukController();

    public LaporanProduksi susunLaporan(List<Integer> idProdukList, LocalDate tglMulai, LocalDate tglSelesai) {
        System.out.println("[LOG] Menyusun laporan dari " + tglMulai + " sampai " + tglSelesai);

        // 1. Ambil data produk (Gunakan produkController agar konsisten)
        List<Produk> semuaProduk = produkController.getAllProduk();

        List<Produk> produkTerpilih = semuaProduk.stream()
                .filter(p -> idProdukList == null || idProdukList.isEmpty() || idProdukList.contains(p.getIdProduk()))
                .collect(Collectors.toList());

        List<LaporanProduksi.BarisTabel> barisTabel = new ArrayList<>();
        List<LaporanProduksi.PerformaProduk> performaList = new ArrayList<>();

        for (Produk produk : produkTerpilih) {
            // 2. Ambil riwayat produksi harian
            // Pastikan method ini ada di ProduksiHarian atau ambil lewat ProduksiController
            List<ProduksiHarian> semuaProduksi = ProduksiHarian.getByIdProduk(produk.getIdProduk());

            // 3. Filter berdasarkan jangka waktu
            List<ProduksiHarian> listFiltered = semuaProduksi.stream()
                    .filter(ph -> (tglMulai == null || !ph.getTanggalProduksi().isBefore(tglMulai)) &&
                            (tglSelesai == null || !ph.getTanggalProduksi().isAfter(tglSelesai)))
                    .sorted(Comparator.comparing(ProduksiHarian::getTanggalProduksi))
                    .collect(Collectors.toList());

            // 4. Masukkan ke detail Baris Tabel jika ada data
            if (!listFiltered.isEmpty()) {
                for (ProduksiHarian ph : listFiltered) {
                    barisTabel.add(new LaporanProduksi.BarisTabel(
                            produk.getIdProduk(),
                            produk.getNama(),
                            ph.getTanggalProduksi(),
                            ph.getJumlahAktual(),
                            ph.getJumlahDefect()));
                }
                // 5. Hitung Summary hanya jika ada aktivitas produksi
                performaList.add(hitungSummaryProduk(produk, listFiltered));
            }
        }

        return new LaporanProduksi(barisTabel, performaList, tglMulai, tglSelesai);
    }

    private LaporanProduksi.PerformaProduk hitungSummaryProduk(Produk p, List<ProduksiHarian> lp) {
        int totalProduksi = lp.stream().mapToInt(ProduksiHarian::getJumlahAktual).sum();
        int totalDefect = lp.stream().mapToInt(ProduksiHarian::getJumlahDefect).sum();

        double persentasePerforma = 0;
        if (totalProduksi > 0) {
            // Sesuai logika: (Produksi Bagus / Total Produksi)
            double produksiBersih = (double) totalProduksi - totalDefect;
            persentasePerforma = (produksiBersih / totalProduksi) * 100;
        }

        // Rasio Defect
        double rasioDefect = totalProduksi > 0 ? ((double) totalDefect / totalProduksi) * 100 : 0;

        return new LaporanProduksi.PerformaProduk(
                p.getIdProduk(),
                p.getNama(),
                totalProduksi,
                totalDefect,
                persentasePerforma,
                rasioDefect
        );
    }
}