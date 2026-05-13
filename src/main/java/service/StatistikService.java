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
    private List<Map<String, Object>> kalkulasiTrenByRange(LocalDate dari, LocalDate sampai) {
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

        return kalkulasiTren(data);
    }

    private List<Map<String, Object>> kalkulasiTren(List<ProduksiHarian> data) {
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
    // PERFORMANCE CALCULATION FUNCTIONS
    // =========================================================

    /**
     * Menghitung performa produk untuk 1 hari (hari ini)
     * Rumus: (total jumlah produksi - jumlah defect) / target produksi harian per produk
     *
     * @param idProduk ID dari produk yang akan dihitung performanya
     * @param tanggal Tanggal yang akan dihitung
     * @return Map berisi idProduk, tanggal, target, produksi bersih, dan performa (dalam persen)
     */
    public Map<String, Object> getPerforma1Hari(int idProduk, LocalDate tanggal) {
        Map<String, Object> hasil = new HashMap<>();

        // Ambil data produksi untuk tanggal tersebut
        List<ProduksiHarian> dataProduksi = ProduksiHarian.getByIdProduk(idProduk).stream()
                .filter(ph -> ph.getTanggalProduksi().equals(tanggal))
                .toList();

        // Ambil target produksi untuk produk tersebut
        TargetProduksi target = TargetProduksi.getAktifPadaTanggal(idProduk, tanggal);

        hasil.put("idProduk", idProduk);
        hasil.put("tanggal", tanggal.toString());

        if (dataProduksi.isEmpty() || target == null) {
            hasil.put("target", 0);
            hasil.put("produksiBersih", 0);
            hasil.put("performa", 0.0);
            hasil.put("performaText", "0.00%");
            hasil.put("keterangan", "Tidak ada data produksi atau target");
            return hasil;
        }

        int totalAktual = dataProduksi.stream().mapToInt(ProduksiHarian::getJumlahAktual).sum();
        int totalDefect = dataProduksi.stream().mapToInt(ProduksiHarian::getJumlahDefect).sum();
        int produksiBersih = totalAktual - totalDefect;
        int targetHarian = target.getJumlahTarget() / target.getPeriodeTarget();

        double performa = (targetHarian > 0) ? (produksiBersih / (double) targetHarian) * 100 : 0;

        hasil.put("target", targetHarian);
        hasil.put("produksiBersih", produksiBersih);
        hasil.put("performa", Math.round(performa * 100.0) / 100.0);
        hasil.put("performaText", String.format("%.2f%%", performa));

        return hasil;
    }

    /**
     * Menghitung performa rata-rata produk untuk 7 hari terakhir
     * Rumus: Rata-rata dari performa harian dalam 7 hari
     *
     * @param idProduk ID dari produk yang akan dihitung performanya
     * @param tanggalAkhir Tanggal akhir perhitungan
     * @return Map berisi idProduk, periode, total produksi, rata-rata performa harian
     */
    public Map<String, Object> getPerforma7Hari(int idProduk, LocalDate tanggalAkhir) {
        LocalDate tanggalAwal = tanggalAkhir.minusDays(6);
        return calculateAveragePerforma(idProduk, tanggalAwal, tanggalAkhir, "7 Hari");
    }

    /**
     * Menghitung performa rata-rata produk untuk 30 hari terakhir
     * Rumus: Rata-rata dari performa harian dalam 30 hari
     *
     * @param idProduk ID dari produk yang akan dihitung performanya
     * @param tanggalAkhir Tanggal akhir perhitungan
     * @return Map berisi idProduk, periode, total produksi, rata-rata performa harian
     */
    public Map<String, Object> getPerforma30Hari(int idProduk, LocalDate tanggalAkhir) {
        LocalDate tanggalAwal = tanggalAkhir.minusDays(29);
        return calculateAveragePerforma(idProduk, tanggalAwal, tanggalAkhir, "30 Hari");
    }

    /**
     * Menghitung performa rata-rata produk untuk 3 bulan terakhir
     * Rumus: Rata-rata dari performa harian dalam 3 bulan
     *
     * @param idProduk ID dari produk yang akan dihitung performanya
     * @param tanggalAkhir Tanggal akhir perhitungan
     * @return Map berisi idProduk, periode, total produksi, rata-rata performa harian
     */
    public Map<String, Object> getPerforma3Bulan(int idProduk, LocalDate tanggalAkhir) {
        LocalDate tanggalAwal = tanggalAkhir.minusMonths(3);
        return calculateAveragePerforma(idProduk, tanggalAwal, tanggalAkhir, "3 Bulan");
    }

    /**
     * Helper method untuk menghitung rata-rata performa dalam range waktu tertentu
     *
     * @param idProduk ID produk
     * @param tanggalAwal Tanggal awal range
     * @param tanggalAkhir Tanggal akhir range
     * @param namaPeriode Nama periode untuk label
     * @return Map berisi data performa rata-rata
     */
    private Map<String, Object> calculateAveragePerforma(int idProduk, LocalDate tanggalAwal,
                                                          LocalDate tanggalAkhir, String namaPeriode) {
        Map<String, Object> hasil = new HashMap<>();

        // Ambil data produksi dalam range
        List<ProduksiHarian> dataProduksi = ProduksiHarian.getByIdProduk(idProduk).stream()
                .filter(ph -> !ph.getTanggalProduksi().isBefore(tanggalAwal) &&
                             !ph.getTanggalProduksi().isAfter(tanggalAkhir))
                .toList();

        // Ambil target produksi
        TargetProduksi target = TargetProduksi.getAktifPadaTanggal(idProduk, tanggalAkhir);

        hasil.put("idProduk", idProduk);
        hasil.put("periode", namaPeriode);
        hasil.put("tanggalAwal", tanggalAwal.toString());
        hasil.put("tanggalAkhir", tanggalAkhir.toString());

        if (dataProduksi.isEmpty() || target == null) {
            hasil.put("totalProduksiBersih", 0);
            hasil.put("targetHarian", 0);
            hasil.put("rataRataPerforma", 0.0);
            hasil.put("rataRataPerformaText", "0.00%");
            hasil.put("jumlahHari", 0);
            hasil.put("keterangan", "Tidak ada data produksi atau target");
            return hasil;
        }

        // Kelompokkan data per tanggal untuk menghitung performa harian
        Map<LocalDate, List<ProduksiHarian>> perTanggal = dataProduksi.stream()
                .collect(Collectors.groupingBy(ProduksiHarian::getTanggalProduksi));

        int targetHarian = target.getJumlahTarget() / target.getPeriodeTarget();
        int jumlahHari = perTanggal.size();
        double totalPerforma = 0;
        int totalProduksiBersih = 0;

        // Hitung performa untuk setiap hari
        for (List<ProduksiHarian> dataHari : perTanggal.values()) {
            int totalAktual = dataHari.stream().mapToInt(ProduksiHarian::getJumlahAktual).sum();
            int totalDefect = dataHari.stream().mapToInt(ProduksiHarian::getJumlahDefect).sum();
            int produksiBersih = totalAktual - totalDefect;

            totalProduksiBersih += produksiBersih;

            // Performa hari ini
            double performaHari = (targetHarian > 0) ? (produksiBersih / (double) targetHarian) * 100 : 0;
            totalPerforma += performaHari;
        }

        // Hitung rata-rata performa
        double rataRataPerforma = (jumlahHari > 0) ? totalPerforma / jumlahHari : 0;

        hasil.put("totalProduksiBersih", totalProduksiBersih);
        hasil.put("targetHarian", targetHarian);
        hasil.put("jumlahHari", jumlahHari);
        hasil.put("rataRataPerforma", Math.round(rataRataPerforma * 100.0) / 100.0);
        hasil.put("rataRataPerformaText", String.format("%.2f%%", rataRataPerforma));

        return hasil;
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
        return kalkulasiTrenByRange(dari, sampai);
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

    public List<Map<String, Object>> getTrenDariData(List<ProduksiHarian> data) {
        return kalkulasiTren(data); // tinggal panggil private method yang sudah ada
    }

    public Map<String, Object> getRingkasanDariData(List<ProduksiHarian> data) {
        // sama seperti getRingkasan() tapi tanpa query DB
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
        long jumlahHari = data.stream().map(ProduksiHarian::getTanggalProduksi).distinct().count();
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

    public Map<String, Object> getProduksiTertinggiDariData(List<ProduksiHarian> data) {
        return countProduksiTertinggi(data);
    }

    public Map<String, Object> getDefectTerbanyakDariData(List<ProduksiHarian> data) {
        return countDefectTerbanyak(data);
    }

    // Getters & Setters
    public String getKriteriaUrutan() { return kriteriaUrutan; }
    public void setKriteriaUrutan(String kriteriaUrutan) { this.kriteriaUrutan = kriteriaUrutan; }
    public int getLimitData() { return limitData; }
    public void setLimitData(int limitData) { this.limitData = limitData; }
}