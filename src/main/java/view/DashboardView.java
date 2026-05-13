package view;


import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import service.StatistikService;
import controller.ProdukController;
import model.ProduksiHarian;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class DashboardView extends VBox {

    // Fields (sesuai CD-12)
    private String rentangWaktu;
    private String tipeGrafik;
    private String pilihanPeriode;

    // UI Components
    private LineChart<String, Number> grafikTren;
    private Label lblTotalProduksi;
    private Label lblTotalDefect;
    private Label lblRataRata;
    private Label lblPersentaseDefect;
    private Label lblProdukTertinggi;
    private Label lblDefectTerbanyak;
    private ComboBox<String> comboPeriode;
    private DatePicker datePickerDari;
    private DatePicker datePickerSampai;
    private Label lblPesanError;

    // Service
    private StatistikService statistikService;
    private ProdukController produkController;

    /*
    // =========================================================
    // Constructor
    // =========================================================
    public DashboardView() {
        this.statistikService = new StatistikService();
        this.produkController = new ProdukController();
        this.pilihanPeriode   = "7 Hari Terakhir";
        this.rentangWaktu     = "minggu";
        this.tipeGrafik       = "line";

        buildUI();
        showDashboard(); // langsung load data saat dibuka
    }
     */

    /*
    // =========================================================
    // 1. showDashboard() — PUBLIC
    //    Menampilkan dashboard: grafik tren, tabel rekapitulasi,
    //    card summary, dan kontrol filter periode
    // =========================================================
    public void showDashboard() {
        LocalDate[] range = getRentangTanggal();
        LocalDate dari    = range[0];
        LocalDate sampai  = range[1];

        try {
            // Ambil data mentah dari controller, lalu proses di StatistikService
            List<ProduksiHarian> dataProduksi = produkController.getProduksiHarianByDateRange(dari, sampai);
            List<Map<String, Object>> tren = statistikService.getTrenDariData(dataProduksi);
            Map<String, Object> ringkasan = statistikService.getRingkasanDariData(dataProduksi);
            Map<String, Object> produksiTertinggi = statistikService.getProduksiTertinggiDariData(dataProduksi);
            Map<String, Object> defectTerbanyak = statistikService.getDefectTerbanyakDariData(dataProduksi);

            // Update card summary
            lblTotalProduksi.setText(String.valueOf(ringkasan.get("totalBersih")));
            lblTotalDefect.setText(String.valueOf(ringkasan.get("totalDefect")));
            lblRataRata.setText(ringkasan.get("rataRataHarian") + " / hari");
            lblPersentaseDefect.setText(ringkasan.get("persentaseDefect") + "%");

            // Update highlight produk
            lblProdukTertinggi.setText((String) produksiTertinggi.getOrDefault("namaProduk", "-"));
            lblDefectTerbanyak.setText((String) defectTerbanyak.getOrDefault("namaProduk", "-"));

            // Update grafik
            updateGrafik(tren);

            // Sembunyikan pesan error kalau ada
            lblPesanError.setVisible(false);

        } catch (Exception e) {
            showPesanError("Gagal memuat data dashboard: " + e.getMessage());
        }
    }

    // =========================================================
    // 2. updateGrafik() — PRIVATE
    //    Memperbarui grafik tren dengan data terbaru
    // =========================================================
    private void updateGrafik(List<Map<String, Object>> tren) {
        grafikTren.getData().clear();

        XYChart.Series<String, Number> seriesAktual = new XYChart.Series<>();
        seriesAktual.setName("Produksi Aktual");

        XYChart.Series<String, Number> seriesDefect = new XYChart.Series<>();
        seriesDefect.setName("Defect");

        for (Map<String, Object> poin : tren) {
            String tanggal   = (String) poin.get("tanggal");
            int totalAktual  = (int)    poin.get("totalAktual");
            int totalDefect  = (int)    poin.get("totalDefect");

            seriesAktual.getData().add(new XYChart.Data<>(tanggal, totalAktual));
            seriesDefect.getData().add(new XYChart.Data<>(tanggal, totalDefect));
        }

        grafikTren.getData().addAll(seriesAktual, seriesDefect);
    }

    // =========================================================
    // 3. showPesanError() — PRIVATE
    //    Menampilkan pesan error di dashboard
    // =========================================================
    private void showPesanError(String pesan) {
        lblPesanError.setText("⚠ " + pesan);
        lblPesanError.setVisible(true);
    }

    // =========================================================
    // 4. onFilterChanged() — PUBLIC
    //    Dipanggil saat user mengubah filter periode
    // =========================================================
    public void onFilterChanged() {
        String pilihan = comboPeriode.getValue();
        if (pilihan != null) {
            this.pilihanPeriode = pilihan;
            this.rentangWaktu   = mapPilihanKeRentang(pilihan);
        }
        showDashboard(); // refresh data
    }

    // =========================================================
    // HELPER: Bangun seluruh UI dashboard
    // =========================================================
    private void buildUI() {
        setSpacing(16);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: #f4f6f9;");

        // --- Header ---
        Label lblJudul = new Label("Dashboard Performa Produksi");
        lblJudul.setFont(Font.font("System", FontWeight.BOLD, 22));
        lblJudul.setStyle("-fx-text-fill: #2c3e50;");

        // --- Error label ---
        lblPesanError = new Label();
        lblPesanError.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 13px;");
        lblPesanError.setVisible(false);

        // --- Filter bar ---
        HBox filterBar = buildFilterBar();

        // --- Card summary ---
        HBox cardRow = buildCardRow();

        // --- Highlight produk ---
        HBox highlightRow = buildHighlightRow();

        // --- Grafik ---
        VBox grafikBox = buildGrafikBox();

        getChildren().addAll(lblJudul, lblPesanError, filterBar, cardRow, highlightRow, grafikBox);
    }

    private HBox buildFilterBar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 14, 10, 14));
        bar.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 4, 0, 0, 2);");

        Label lblFilter = new Label("Periode:");
        lblFilter.setFont(Font.font("System", FontWeight.BOLD, 13));

        comboPeriode = new ComboBox<>();
        comboPeriode.getItems().addAll(
                "7 Hari Terakhir",
                "30 Hari Terakhir",
                "3 Bulan Terakhir",
                "Custom"
        );
        comboPeriode.setValue(pilihanPeriode);
        comboPeriode.setOnAction(e -> {
            boolean isCustom = "Custom".equals(comboPeriode.getValue());
            datePickerDari.setVisible(isCustom);
            datePickerSampai.setVisible(isCustom);
            if (!isCustom) onFilterChanged();
        });

        Label lblDari   = new Label("Dari:");
        Label lblSampai = new Label("Sampai:");

        datePickerDari   = new DatePicker(LocalDate.now().minusDays(7));
        datePickerSampai = new DatePicker(LocalDate.now());
        datePickerDari.setVisible(false);
        datePickerSampai.setVisible(false);

        Button btnTerapkan = new Button("Terapkan");
        btnTerapkan.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
        btnTerapkan.setOnAction(e -> onFilterChanged());

        bar.getChildren().addAll(lblFilter, comboPeriode, lblDari, datePickerDari, lblSampai, datePickerSampai, btnTerapkan);
        return bar;
    }

    private HBox buildCardRow() {
        lblTotalProduksi    = new Label("-");
        lblTotalDefect      = new Label("-");
        lblRataRata         = new Label("-");
        lblPersentaseDefect = new Label("-");

        HBox row = new HBox(16);
        row.getChildren().addAll(
                buildCard("Total Produksi Bersih", lblTotalProduksi, "#2ecc71"),
                buildCard("Total Defect",           lblTotalDefect,      "#e74c3c"),
                buildCard("Rata-rata / Hari",        lblRataRata,         "#3498db"),
                buildCard("% Defect",                lblPersentaseDefect, "#f39c12")
        );
        return row;
    }

    private HBox buildHighlightRow() {
        lblProdukTertinggi = new Label("-");
        lblDefectTerbanyak = new Label("-");

        HBox row = new HBox(16);
        row.getChildren().addAll(
                buildCard("🏆 Produksi Tertinggi", lblProdukTertinggi, "#8e44ad"),
                buildCard("⚠ Defect Terbanyak",    lblDefectTerbanyak,  "#c0392b")
        );
        return row;
    }

    private VBox buildGrafikBox() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        xAxis.setLabel("Tanggal");
        yAxis.setLabel("Jumlah");

        grafikTren = new LineChart<>(xAxis, yAxis);
        grafikTren.setTitle("Tren Produksi Harian");
        grafikTren.setPrefHeight(300);
        grafikTren.setAnimated(false);

        VBox box = new VBox(8, grafikTren);
        box.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 4, 0, 0, 2);");
        box.setPadding(new Insets(12));
        return box;
    }

    private VBox buildCard(String judul, Label lblNilai, String warnaBorder) {
        lblNilai.setFont(Font.font("System", FontWeight.BOLD, 22));
        lblNilai.setStyle("-fx-text-fill: " + warnaBorder + ";");

        Label lblJudul = new Label(judul);
        lblJudul.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");

        VBox card = new VBox(4, lblJudul, lblNilai);
        card.setPadding(new Insets(16));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMinWidth(170);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: " + warnaBorder + ";" +
                        "-fx-border-width: 0 0 0 4;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 4, 0, 0, 2);"
        );
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    // =========================================================
    // HELPER: Hitung rentang tanggal berdasarkan pilihan periode
    // =========================================================
    private LocalDate[] getRentangTanggal() {
        LocalDate sampai = LocalDate.now();
        LocalDate dari;

        if ("Custom".equals(pilihanPeriode)) {
            dari   = datePickerDari.getValue() != null   ? datePickerDari.getValue()   : sampai.minusDays(7);
            sampai = datePickerSampai.getValue() != null ? datePickerSampai.getValue() : sampai;
        } else {
            dari = switch (rentangWaktu) {
                case "bulan3" -> sampai.minusMonths(3);
                case "bulan1" -> sampai.minusDays(30);
                default       -> sampai.minusDays(7); // default: minggu
            };
        }

        return new LocalDate[]{dari, sampai};
    }

    private String mapPilihanKeRentang(String pilihan) {
        return switch (pilihan) {
            case "30 Hari Terakhir"  -> "bulan1";
            case "3 Bulan Terakhir"  -> "bulan3";
            case "Custom"            -> "custom";
            default                  -> "minggu";
        };
    }
*/

    // Getters & Setters
    public String getRentangWaktu()               { return rentangWaktu; }
    public void setRentangWaktu(String v)         { this.rentangWaktu = v; }
    public String getTipeGrafik()                 { return tipeGrafik; }
    public void setTipeGrafik(String v)           { this.tipeGrafik = v; }
    public String getPilihanPeriode()             { return pilihanPeriode; }
    public void setPilihanPeriode(String v)       { this.pilihanPeriode = v; }
}