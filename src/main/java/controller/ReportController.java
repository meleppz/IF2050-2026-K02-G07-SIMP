package controller;

import model.LaporanProduksi;
import model.Produk;
import model.ProduksiHarian;
import model.TargetProduksi;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class ReportController {

    public LaporanProduksi susunLaporan(List<Integer> idProdukList, LocalDate tglMulai, LocalDate tglSelesai) {

        // 1. Ambil data produk
        List<Produk> semuaProduk = Produk.getAll();
        List<Produk> produkTerpilih = semuaProduk.stream()
                .filter(p -> idProdukList == null || idProdukList.isEmpty() || idProdukList.contains(p.getIdProduk()))
                .collect(Collectors.toList());

        List<LaporanProduksi.BarisTabel> barisTabel = new ArrayList<>();
        List<LaporanProduksi.PerformaProduk> performaList = new ArrayList<>();

        for (Produk produk : produkTerpilih) {
            // 2. Ambil riwayat produksi harian
            List<ProduksiHarian> semuaProduksi = ProduksiHarian.getByIdProduk(produk.getIdProduk());

            // Filter berdasarkan jangka waktu yang dipilih user
            List<ProduksiHarian> listFiltered = semuaProduksi.stream()
                    .filter(ph -> (tglMulai == null || !ph.getTanggalProduksi().isBefore(tglMulai)) &&
                            (tglSelesai == null || !ph.getTanggalProduksi().isAfter(tglSelesai)))
                    .sorted(Comparator.comparing(ProduksiHarian::getTanggalProduksi))
                    .collect(Collectors.toList());

            // 3. Masukkan ke detail Baris Tabel
            if (!listFiltered.isEmpty()) {
                for (ProduksiHarian ph : listFiltered) {
                    barisTabel.add(new LaporanProduksi.BarisTabel(
                            produk.getIdProduk(),
                            produk.getNama(),
                            ph.getTanggalProduksi(),
                            ph.getJumlahAktual(),
                            ph.getJumlahDefect()));
                }
            }

            // 4. Hitung Summary (Total & Performa) per Produk
            performaList.add(hitungSummaryProduk(produk, listFiltered));
        }

        return new LaporanProduksi(barisTabel, performaList, tglMulai, tglSelesai);
    }

    /**
     * Menghitung rangkuman performa berdasarkan data produksi yang sudah difilter
     */
    private LaporanProduksi.PerformaProduk hitungSummaryProduk(Produk p, List<ProduksiHarian> lp) {
        // Hitung total produksi dan total defect dalam range tanggal
        int totalProduksi = lp.stream().mapToInt(ProduksiHarian::getJumlahAktual).sum();
        int totalDefect = lp.stream().mapToInt(ProduksiHarian::getJumlahDefect).sum();

        // LOGIKA PERFORMA: (Produksi Bersih / Total Produksi) * 100
        // Produksi Bersih = Total Produksi - Total Defect
        double persentasePerforma = 0;
        if (totalProduksi > 0) {
            double produksiBersih = (double) totalProduksi - totalDefect;
            persentasePerforma = (produksiBersih / totalProduksi) * 100;
        }

        // Rasio Defect (tambahan untuk insight)
        double rasioDefect = totalProduksi > 0 ? ((double) totalDefect / totalProduksi) * 100 : 0;

        // Return object PerformaProduk sesuai struktur model LaporanProduksi
        return new LaporanProduksi.PerformaProduk(
                p.getIdProduk(),
                p.getNama(),
                totalProduksi,
                totalDefect,
                persentasePerforma, // Disimpan sebagai performa utama
                rasioDefect        // Disimpan sebagai data tambahan
        );
    }
}