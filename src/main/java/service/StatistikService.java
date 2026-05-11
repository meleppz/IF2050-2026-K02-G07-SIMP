package service;

import model.ProduksiHarian;
import model.Produk;
import model.TargetProduksi;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class StatistikService {

    // Fields (sesuai CD-13)
    private String kriteriaUrutan;
    private int limitData;

    // Constructor
    public StatistikService() {
        this.kriteriaUrutan = "produksi"; // default urutan
        this.limitData = 5;              // default limit
    }

    public StatistikService(String kriteriaUrutan, int limitData) {
        this.kriteriaUrutan = kriteriaUrutan;
        this.limitData = limitData;
    }

    // =========================================================
    // 1. countProduksiTertinggi()
    //    Menghitung dan menentukan produk dengan jumlah produksi tertinggi
    //    dari list data ProduksiHarian yang diberikan
    // =========================================================
    private Map<String, Object> countProduksiTertinggi(List<ProduksiHarian> data) {
        Map<String, Object> hasil = new HashMap<>();

        if (data == null || data.isEmpty()) {
            hasil.put("idProduk", -1);
            hasil.put("totalProduksi", 0);
            hasil.put("keterangan", "Tidak ada data");
            return hasil;
        }

        // Kelompokkan data per idProduk, jumlahkan jumlahAktual
        Map<Integer, Integer> totalPerProduk = new HashMap<>();
        for (ProduksiHarian ph : data) {
            totalPerProduk.merge(ph.getIdProduk(), ph.getJumlahAktual(), Integer::sum);
        }

        // Cari idProduk dengan total tertinggi
        int idProdukTertinggi = totalPerProduk.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(-1);

        int totalTertinggi = totalPerProduk.getOrDefault(idProdukTertinggi, 0);

        // Ambil nama produk dari database
        Produk produk = Produk.getById(idProdukTertinggi);
        String namaProduk = (produk != null) ? produk.getNama() : "Tidak diketahui";

        hasil.put("idProduk", idProdukTertinggi);
        hasil.put("namaProduk", namaProduk);
        hasil.put("totalProduksi", totalTertinggi);

        return hasil;
    }

    // =========================================================
    // 2. countDefectTerbanyak()
    //    Menghitung dan menentukan produk dengan jumlah defect tertinggi
    //    dari list data ProduksiHarian yang diberikan
    // =========================================================
    private Map<String, Object> countDefectTerbanyak(List<ProduksiHarian> data) {
        Map<String, Object> hasil = new HashMap<>();

        if (data == null || data.isEmpty()) {
            hasil.put("idProduk", -1);
            hasil.put("totalDefect", 0);
            hasil.put("keterangan", "Tidak ada data");
            return hasil;
        }

        // Kelompokkan data per idProduk, jumlahkan jumlahDefect
        Map<Integer, Integer> defectPerProduk = new HashMap<>();
        for (ProduksiHarian ph : data) {
            defectPerProduk.merge(ph.getIdProduk(), ph.getJumlahDefect(), Integer::sum);
        }

        // Cari idProduk dengan defect terbanyak
        int idProdukDefect = defectPerProduk.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(-1);

        int totalDefect = defectPerProduk.getOrDefault(idProdukDefect, 0);

        // Hitung persentase defect
        int totalAktual = data.stream()
                .filter(ph -> ph.getIdProduk() == idProdukDefect)
                .mapToInt(ProduksiHarian::getJumlahAktual)
                .sum();
        double persentase = (totalAktual > 0) ? (totalDefect / (double) totalAktual) * 100 : 0;

        // Ambil nama produk dari database
        Produk produk = Produk.getById(idProdukDefect);
        String namaProduk = (produk != null) ? produk.getNama() : "Tidak diketahui";

        hasil.put("idProduk", idProdukDefect);
        hasil.put("namaProduk", namaProduk);
        hasil.put("totalDefect", totalDefect);
        hasil.put("persentaseDefect", Math.round(persentase * 100.0) / 100.0);

        return hasil;
    }

    // =========================================================
    // 3. kalkulasiTren()
    //    Menghitung tren produksi harian dalam rentang waktu tertentu.
    //    Mengembalikan list Map berisi tanggal, total produksi, total defect
    //    yang sudah diurutkan per tanggal (cocok untuk grafik line chart)
    // =========================================================
    private List<Map<String, Object>> kalkulasiTren(LocalDate dari, LocalDate sampai) {
        List<ProduksiHarian> data = ProduksiHarian.getByDateRange(dari, sampai);
        List<Map<String, Object>> tren = new ArrayList<>();

        if (data == null || data.isEmpty()) return tren;

        // Kelompokkan per tanggal
        Map<LocalDate, List<ProduksiHarian>> perTanggal = data.stream()
                .collect(Collectors.groupingBy(ProduksiHarian::getTanggalProduksi));

        // Urutkan tanggal dari yang terlama
        List<LocalDate> tanggalUrut = new ArrayList<>(perTanggal.keySet());
        Collections.sort(tanggalUrut);

        for (LocalDate tanggal : tanggalUrut) {
            List<ProduksiHarian> listHari = perTanggal.get(tanggal);

            int totalAktual = listHari.stream().mapToInt(ProduksiHarian::getJumlahAktual).sum();
            int totalDefect = listHari.stream().mapToInt(ProduksiHarian::getJumlahDefect).sum();
            int totalBersih = totalAktual - totalDefect;

            Map<String, Object> poin = new HashMap<>();
            poin.put("tanggal", tanggal.toString());
            poin.put("totalAktual", totalAktual);
            poin.put("totalDefect", totalDefect);
            poin.put("totalBersih", totalBersih);
            tren.add(poin);
        }

        return tren;
    }

    // =========================================================
    // PUBLIC WRAPPER — dipanggil dari DashboardView
    // =========================================================

    /**
     * Ambil produk dengan produksi tertinggi dari rentang tanggal tertentu
     */
    public Map<String, Object> getProduksiTertinggi(LocalDate dari, LocalDate sampai) {
        List<ProduksiHarian> data = ProduksiHarian.getByDateRange(dari, sampai);
        return countProduksiTertinggi(data);
    }

    /**
     * Ambil produk dengan defect terbanyak dari rentang tanggal tertentu
     */
    public Map<String, Object> getDefectTerbanyak(LocalDate dari, LocalDate sampai) {
        List<ProduksiHarian> data = ProduksiHarian.getByDateRange(dari, sampai);
        return countDefectTerbanyak(data);
    }

    /**
     * Ambil data tren produksi untuk grafik
     */
    public List<Map<String, Object>> getTren(LocalDate dari, LocalDate sampai) {
        return kalkulasiTren(dari, sampai);
    }

    /**
     * Ambil ringkasan keseluruhan untuk card summary di dashboard
     * (total produksi, total defect, rata-rata harian, dll)
     */
    public Map<String, Object> getRingkasan(LocalDate dari, LocalDate sampai) {
        List<ProduksiHarian> data = ProduksiHarian.getByDateRange(dari, sampai);
        Map<String, Object> ringkasan = new HashMap<>();

        if (data == null || data.isEmpty()) {
            ringkasan.put("totalAktual", 0);
            ringkasan.put("totalDefect", 0);
            ringkasan.put("totalBersih", 0);
            ringkasan.put("rataRataHarian", 0.0);
            ringkasan.put("persentaseDefect", 0.0);
            return ringkasan;
        }

        int totalAktual = data.stream().mapToInt(ProduksiHarian::getJumlahAktual).sum();
        int totalDefect = data.stream().mapToInt(ProduksiHarian::getJumlahDefect).sum();
        int totalBersih = totalAktual - totalDefect;

        long jumlahHari = data.stream()
                .map(ProduksiHarian::getTanggalProduksi)
                .distinct()
                .count();

        double rataRata = (jumlahHari > 0) ? (double) totalBersih / jumlahHari : 0;
        double persentaseDefect = (totalAktual > 0) ? (totalDefect / (double) totalAktual) * 100 : 0;

        ringkasan.put("totalAktual", totalAktual);
        ringkasan.put("totalDefect", totalDefect);
        ringkasan.put("totalBersih", totalBersih);
        ringkasan.put("rataRataHarian", Math.round(rataRata * 100.0) / 100.0);
        ringkasan.put("persentaseDefect", Math.round(persentaseDefect * 100.0) / 100.0);
        ringkasan.put("jumlahHari", jumlahHari);

        return ringkasan;
    }

    // Getters & Setters
    public String getKriteriaUrutan() { return kriteriaUrutan; }
    public void setKriteriaUrutan(String kriteriaUrutan) { this.kriteriaUrutan = kriteriaUrutan; }
    public int getLimitData() { return limitData; }
    public void setLimitData(int limitData) { this.limitData = limitData; }
}