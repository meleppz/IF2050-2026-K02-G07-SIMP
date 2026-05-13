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

        // 1. Ambil Produk menggunakan method static yang sudah ada di Produk.java temanmu
        List<Produk> semuaProduk = Produk.getAll();
        List<Produk> produkTerpilih = semuaProduk.stream()
                .filter(p -> idProdukList == null || idProdukList.isEmpty() || idProdukList.contains(p.getIdProduk()))
                .collect(Collectors.toList());

        List<LaporanProduksi.BarisTabel> barisTabel = new ArrayList<>();
        List<LaporanProduksi.PerformaProduk> performaList = new ArrayList<>();

        for (Produk produk : produkTerpilih) {
            // 2. Gunakan method static dari ProduksiHarian.java temanmu (getByIdProduk)
            List<ProduksiHarian> semuaProduksi = ProduksiHarian.getByIdProduk(produk.getIdProduk());

            // Filter tanggal secara manual di sini agar tidak perlu ubah file ProduksiHarian
            List<ProduksiHarian> listFiltered = semuaProduksi.stream()
                    .filter(ph -> (tglMulai == null || !ph.getTanggalProduksi().isBefore(tglMulai)) &&
                            (tglSelesai == null || !ph.getTanggalProduksi().isAfter(tglSelesai)))
                    .sorted(Comparator.comparing(ProduksiHarian::getTanggalProduksi))
                    .collect(Collectors.toList());

            // 3. Tambahkan ke baris tabel
            if (listFiltered.isEmpty()) {
                barisTabel.add(new LaporanProduksi.BarisTabel(produk.getIdProduk(), produk.getNama(), null, 0, 0));
            } else {
                for (ProduksiHarian ph : listFiltered) {
                    barisTabel.add(new LaporanProduksi.BarisTabel(
                            produk.getIdProduk(), produk.getNama(), ph.getTanggalProduksi(),
                            ph.getJumlahAktual(), ph.getJumlahDefect()));
                }
            }

            // 4. Hitung Performa (Gunakan TargetProduksi.getAktifPadaTanggal yang sudah ada)
            TargetProduksi target = TargetProduksi.getAktifPadaTanggal(produk.getIdProduk(), LocalDate.now());
            performaList.add(hitungPerformaManual(produk, listFiltered, target));
        }

        return new LaporanProduksi(barisTabel, performaList, tglMulai, tglSelesai);
    }

    private LaporanProduksi.PerformaProduk hitungPerformaManual(Produk p, List<ProduksiHarian> lp, TargetProduksi target) {
        int totalProd = lp.stream().mapToInt(ProduksiHarian::getJumlahAktual).sum();
        int totalDef = lp.stream().mapToInt(ProduksiHarian::getJumlahDefect).sum();
        double rasioDef = totalProd > 0 ? (double) totalDef / totalProd * 100 : 0;

        Double rasioVsTarget = null;
        if (target != null && target.getJumlahTarget() > 0) {
            rasioVsTarget = (double) totalProd / target.getJumlahTarget() * 100;
        }

        return new LaporanProduksi.PerformaProduk(p.getIdProduk(), p.getNama(), totalProd, totalDef, rasioDef, rasioVsTarget);
    }
}