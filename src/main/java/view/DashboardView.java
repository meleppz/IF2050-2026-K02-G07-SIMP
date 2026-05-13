package view;

import controller.ProdukController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import model.ProduksiHarian;
import model.Produk;
import service.StatistikService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class DashboardView extends BorderPane {

    // ── Warna tema ───────────────────────────────────────────
    private static final String BG_MAIN     = "#0a1f1f";
    private static final String BG_CARD     = "#132929";
    private static final String BG_CARD2    = "#0d2626";
    private static final String ACCENT      = "#00e5a0";
    private static final String TEXT_WHITE  = "white";
    private static final String TEXT_MUTED  = "#5a8a8a";
    private static final String COLOR_GREEN = "#00e5a0";
    private static final String COLOR_RED   = "#e05a5a";

    // ── State ────────────────────────────────────────────────
    private LocalDate dari   = LocalDate.now().minusDays(6);
    private LocalDate sampai = LocalDate.now();

    // ── Services ─────────────────────────────────────────────
    private final ProdukController produkController = new ProdukController();
    private final StatistikService statistikService = new StatistikService();

    // ── UI refs ──────────────────────────────────────────────
    private Label  lblTotalProduksi;
    private Label  lblTotalDefect;
    private Label  lblPerforma;
    private VBox   aktivitasBox;
    private Canvas grafikCanvas;
    private VBox   topPerformingBox;
    private VBox   worstPerformingBox;
    private VBox   emptyStateBox;
    private VBox   dataStateBox;
    private Label  lblFilterRange;

    // ── Fonts ────────────────────────────────────────────────
    private Font fBold14, fBold20, fBold28, fBold36, fReg12, fReg13, fReg14;

    // ── Format tanggal ───────────────────────────────────────
    private static final DateTimeFormatter FMT_DISPLAY =
            DateTimeFormatter.ofPattern("d MMM yyyy", new Locale("id"));

    private MainViewController mainController;

    public DashboardView() {
        loadFonts();
        buildUI();
        refresh();
    }

    public DashboardView(MainViewController mainController) {
        this.mainController = mainController;
        loadFonts();
        buildUI();
        refresh();
    }

    // =========================================================
    // FONTS
    // =========================================================
    private void loadFonts() {
        fBold14 = Font.font("Plus Jakarta Sans", FontWeight.BOLD, 14);
        fBold20 = Font.font("Plus Jakarta Sans", FontWeight.BOLD, 20);
        fBold28 = Font.font("Plus Jakarta Sans", FontWeight.BOLD, 28);
        fBold36 = Font.font("Plus Jakarta Sans", FontWeight.BOLD, 36);
        fReg12  = Font.font("Plus Jakarta Sans", 12);
        fReg13  = Font.font("Plus Jakarta Sans", 13);
        fReg14  = Font.font("Plus Jakarta Sans", 14);
    }

    // =========================================================
    // BUILD UI
    // =========================================================
    private void buildUI() {
        setStyle("-fx-background-color: " + BG_MAIN + ";");
        setPrefWidth(1280);
        setTop(buildHeader());

        HBox body = new HBox(16);
        body.setPadding(new Insets(0, 24, 24, 24));

        VBox mainContent = buildMainContent();
        VBox rightPanel  = buildRightPanel();

        HBox.setHgrow(mainContent, Priority.ALWAYS);
        body.getChildren().addAll(mainContent, rightPanel);
        setCenter(body);
    }

    // ─── HEADER ───────────────────────────────────────────────
    private HBox buildHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(24, 32, 16, 32));

        Label lblTitle = new Label("Dashboard");
        lblTitle.setFont(fBold28);
        lblTitle.setStyle("-fx-text-fill: " + ACCENT + ";");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblGreeting = new Label(getGreeting());
        lblGreeting.setFont(fReg12);
        lblGreeting.setStyle("-fx-text-fill: " + TEXT_MUTED + ";");

        Label lblNama = new Label("Lorem Ipsum"); // TODO: connect to user
        lblNama.setFont(fBold14);
        lblNama.setStyle("-fx-text-fill: " + TEXT_WHITE + ";");

        VBox greetingBox = new VBox(2, lblGreeting, lblNama);
        greetingBox.setAlignment(Pos.CENTER_RIGHT);

        StackPane avatar = new StackPane();
        avatar.setPrefSize(36, 36);
        avatar.setStyle("-fx-background-color: " + BG_CARD + "; -fx-background-radius: 18;");
        Label lblAvatar = new Label("👤");
        lblAvatar.setStyle("-fx-font-size: 18;");
        avatar.getChildren().add(lblAvatar);

        Label lblBell = new Label("🔔");
        lblBell.setStyle("-fx-font-size: 18; -fx-text-fill: " + TEXT_WHITE + ";");
        lblBell.setCursor(Cursor.HAND);

        HBox rightHeader = new HBox(12, lblBell, greetingBox, avatar);
        rightHeader.setAlignment(Pos.CENTER_RIGHT);

        header.getChildren().addAll(lblTitle, spacer, rightHeader);
        return header;
    }

    // ─── MAIN CONTENT (kiri) ──────────────────────────────────
    private VBox buildMainContent() {
        VBox box = new VBox(16);

        HBox subHeader = new HBox(12);
        subHeader.setAlignment(Pos.CENTER_LEFT);

        Label lblSummary = new Label("Summary");
        lblSummary.setFont(fBold20);
        lblSummary.setStyle("-fx-text-fill: " + TEXT_WHITE + ";");

        // Label filter tanggal — klik untuk buka dialog
        lblFilterRange = new Label("📅 " + dari.format(FMT_DISPLAY) + " - " + sampai.format(FMT_DISPLAY));
        lblFilterRange.setFont(fReg13);
        lblFilterRange.setStyle(
                "-fx-text-fill: " + TEXT_WHITE + ";" +
                        "-fx-background-color: " + BG_CARD + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 6 12;" +
                        "-fx-cursor: hand;"
        );
        lblFilterRange.setOnMouseClicked(e -> bukaDialogFilter());
        lblFilterRange.setOnMouseEntered(e -> lblFilterRange.setStyle(
                "-fx-text-fill: " + ACCENT + ";" +
                        "-fx-background-color: " + BG_CARD2 + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 6 12;" +
                        "-fx-cursor: hand;"
        ));
        lblFilterRange.setOnMouseExited(e -> lblFilterRange.setStyle(
                "-fx-text-fill: " + TEXT_WHITE + ";" +
                        "-fx-background-color: " + BG_CARD + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 6 12;" +
                        "-fx-cursor: hand;"
        ));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Link "Lihat Data Produksi" → navigasi ke ProdukView
        Label lblLihat = new Label("Lihat Data Produksi →");
        lblLihat.setFont(fReg13);
        lblLihat.setStyle("-fx-text-fill: " + ACCENT + "; -fx-cursor: hand;");
        lblLihat.setOnMouseClicked(e -> navigasiKeProdukView());
        lblLihat.setOnMouseEntered(e -> lblLihat.setStyle(
                "-fx-text-fill: white; -fx-cursor: hand; -fx-underline: true;"
        ));
        lblLihat.setOnMouseExited(e -> lblLihat.setStyle(
                "-fx-text-fill: " + ACCENT + "; -fx-cursor: hand;"
        ));

        subHeader.getChildren().addAll(lblSummary, lblFilterRange, spacer, lblLihat);

        emptyStateBox = buildEmptyState();
        dataStateBox  = buildDataState();

        box.getChildren().addAll(subHeader, emptyStateBox, dataStateBox);
        return box;
    }

    // ─── DIALOG FILTER TANGGAL ────────────────────────────────
    private void bukaDialogFilter() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Pilih Rentang Tanggal");

        // Styling dialog
        DialogPane pane = dialog.getDialogPane();
        pane.setStyle("-fx-background-color: " + BG_CARD + ";");

        VBox content = new VBox(16);
        content.setPadding(new Insets(20));

        Label lblJudul = new Label("Rentang Tanggal");
        lblJudul.setFont(fBold20);
        lblJudul.setStyle("-fx-text-fill: " + TEXT_WHITE + ";");

        // Shortcut buttons
        HBox shortcuts = new HBox(8);
        String[] labels = {"7 Hari", "30 Hari", "3 Bulan"};
        int[]    days   = {7, 30, 90};

        DatePicker dpDari   = new DatePicker(dari);
        DatePicker dpSampai = new DatePicker(sampai);
        stylePickerDark(dpDari);
        stylePickerDark(dpSampai);

        for (int i = 0; i < labels.length; i++) {
            final int d = days[i];
            Button btn = new Button(labels[i]);
            btn.setFont(fReg13);
            btn.setStyle(
                    "-fx-background-color: " + BG_CARD2 + ";" +
                            "-fx-text-fill: " + ACCENT + ";" +
                            "-fx-background-radius: 6;" +
                            "-fx-padding: 6 12;" +
                            "-fx-cursor: hand;"
            );
            btn.setOnAction(e -> {
                dpSampai.setValue(LocalDate.now());
                dpDari.setValue(LocalDate.now().minusDays(d - 1));
            });
            shortcuts.getChildren().add(btn);
        }

        // DatePicker rows
        HBox rowDari = new HBox(12);
        rowDari.setAlignment(Pos.CENTER_LEFT);
        Label lblDari = new Label("Dari");
        lblDari.setFont(fReg13);
        lblDari.setStyle("-fx-text-fill: " + TEXT_MUTED + ";");
        lblDari.setPrefWidth(60);
        rowDari.getChildren().addAll(lblDari, dpDari);

        HBox rowSampai = new HBox(12);
        rowSampai.setAlignment(Pos.CENTER_LEFT);
        Label lblSampai = new Label("Sampai");
        lblSampai.setFont(fReg13);
        lblSampai.setStyle("-fx-text-fill: " + TEXT_MUTED + ";");
        lblSampai.setPrefWidth(60);
        rowSampai.getChildren().addAll(lblSampai, dpSampai);

        content.getChildren().addAll(lblJudul, shortcuts, rowDari, rowSampai);
        pane.setContent(content);

        // Tombol
        ButtonType btnTerapkan = new ButtonType("Terapkan", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnBatal    = new ButtonType("Batal",    ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().addAll(btnTerapkan, btnBatal);

        // Style tombol Terapkan
        Button btnOk = (Button) pane.lookupButton(btnTerapkan);
        btnOk.setStyle(
                "-fx-background-color: " + ACCENT + ";" +
                        "-fx-text-fill: #0a1f1f;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 8 20;"
        );

        dialog.showAndWait().ifPresent(result -> {
            if (result == btnTerapkan) {
                LocalDate d = dpDari.getValue();
                LocalDate s = dpSampai.getValue();
                if (d != null && s != null && !d.isAfter(s)) {
                    dari   = d;
                    sampai = s;
                    lblFilterRange.setText("📅 " + dari.format(FMT_DISPLAY) + " - " + sampai.format(FMT_DISPLAY));
                    refresh();
                } else {
                    showAlert("Tanggal tidak valid! Pastikan tanggal awal tidak lebih dari tanggal akhir.");
                }
            }
        });
    }

    private void stylePickerDark(DatePicker dp) {
        dp.setStyle(
                "-fx-background-color: " + BG_CARD2 + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 8;"
        );
        dp.getEditor().setStyle(
                "-fx-background-color: " + BG_CARD2 + ";" +
                        "-fx-text-fill: white;"
        );
    }

    // ─── EMPTY STATE ──────────────────────────────────────────
    private VBox buildEmptyState() {
        VBox box = new VBox(16);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(60, 40, 60, 40));
        box.setStyle("-fx-background-color: " + BG_CARD + "; -fx-background-radius: 12;");

        Label lblTitle = new Label("Uh Oh");
        lblTitle.setFont(fBold20);
        lblTitle.setStyle("-fx-text-fill: " + TEXT_WHITE + ";");

        Label lblSub = new Label("Produk dan data produksi masih kosong, ayo tambahkan terlebih dahulu");
        lblSub.setFont(fReg13);
        lblSub.setStyle("-fx-text-fill: " + TEXT_MUTED + ";");
        lblSub.setWrapText(true);

        Button btnProduk = new Button("Tambah Produk");
        btnProduk.setMaxWidth(400);
        btnProduk.setFont(fBold14);
        btnProduk.setStyle(btnStyleAccent());
        btnProduk.setOnAction(e -> navigasiKeProdukView()); // ← navigasi ke ProdukView

        Button btnData = new Button("Tambah Data Produksi");
        btnData.setMaxWidth(400);
        btnData.setFont(fBold14);
        btnData.setStyle(btnStyleAccent());
        btnData.setOnAction(e -> {
            if (mainController != null) {
                mainController.navigasiDataProduksi();
            }
        });

        box.getChildren().addAll(lblTitle, lblSub, btnProduk, btnData);
        return box;
    }

    // ─── DATA STATE ───────────────────────────────────────────
    private VBox buildDataState() {
        VBox box = new VBox(16);

        // Grafik
        VBox grafikCard = new VBox(12);
        grafikCard.setPadding(new Insets(16));
        grafikCard.setStyle("-fx-background-color: " + BG_CARD + "; -fx-background-radius: 12;");

        HBox grafikHeader = new HBox(8);
        grafikHeader.setAlignment(Pos.CENTER_LEFT);
        Label lblGrafikIcon = new Label("📈");
        Label lblGrafik = new Label("GRAFIK PERKEMBANGAN");
        lblGrafik.setFont(fBold14);
        lblGrafik.setStyle("-fx-text-fill: " + TEXT_WHITE + ";");
        grafikHeader.getChildren().addAll(lblGrafikIcon, lblGrafik);

        grafikCanvas = new Canvas(650, 220);
        grafikCard.getChildren().addAll(grafikHeader, grafikCanvas);

        // Top & Worst Performing
        topPerformingBox  = new VBox(0);
        worstPerformingBox = new VBox(0);

        box.getChildren().addAll(
                grafikCard,
                buildPerformingCard("TOP PERFORMING",   topPerformingBox),
                buildPerformingCard("WORST PERFORMING", worstPerformingBox)
        );
        return box;
    }

    private VBox buildPerformingCard(String judul, VBox tableBox) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: " + BG_CARD + "; -fx-background-radius: 12;");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label lblIcon  = new Label(judul.contains("TOP") ? "📈" : "📉");
        Label lblJudul = new Label(judul);
        lblJudul.setFont(fBold14);
        lblJudul.setStyle("-fx-text-fill: " + TEXT_WHITE + ";");
        header.getChildren().addAll(lblIcon, lblJudul);

        // Header kolom
        HBox tabelHeader = new HBox();
        tabelHeader.setPadding(new Insets(8, 0, 8, 0));
        tabelHeader.setStyle("-fx-border-color: " + BG_CARD2 + "; -fx-border-width: 0 0 1 0;");
        String[] cols   = {"ID", "Produk", "Total Defect", "Total Produksi", "Performa"};
        double[] widths = {80, 220, 130, 150, 100};
        for (int i = 0; i < cols.length; i++) {
            Label lbl = new Label(cols[i]);
            lbl.setFont(fReg12);
            lbl.setStyle("-fx-text-fill: " + TEXT_MUTED + ";");
            lbl.setPrefWidth(widths[i]);
            tabelHeader.getChildren().add(lbl);
        }

        card.getChildren().addAll(header, tabelHeader, tableBox);
        return card;
    }

    // ─── RIGHT PANEL ──────────────────────────────────────────
    private VBox buildRightPanel() {
        VBox panel = new VBox(12);
        panel.setPrefWidth(220);

        lblTotalProduksi = new Label("-");
        lblTotalProduksi.setFont(fBold36);
        lblTotalProduksi.setStyle("-fx-text-fill: " + COLOR_GREEN + ";");

        lblTotalDefect = new Label("-");
        lblTotalDefect.setFont(fBold36);
        lblTotalDefect.setStyle("-fx-text-fill: " + COLOR_RED + ";");

        lblPerforma = new Label("-");
        lblPerforma.setFont(fBold36);
        lblPerforma.setStyle("-fx-text-fill: " + TEXT_WHITE + ";");

        panel.getChildren().addAll(
                buildStatCard("👍", "TOTAL PRODUKSI", lblTotalProduksi, "unit"),
                buildStatCard("👎", "TOTAL DEFECT",   lblTotalDefect,   "unit"),
                buildStatCard("📊", "PERFORMA",        lblPerforma,      ""),
                buildAktivitasCard()
        );
        return panel;
    }

    private VBox buildStatCard(String icon, String judul, Label lblNilai, String satuan) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: " + BG_CARD + "; -fx-background-radius: 12;");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label lblIcon  = new Label(icon);
        Label lblJudul = new Label(judul);
        lblJudul.setFont(fReg12);
        lblJudul.setStyle("-fx-text-fill: " + TEXT_MUTED + ";");
        header.getChildren().addAll(lblIcon, lblJudul);

        HBox nilaiBox = new HBox(6);
        nilaiBox.setAlignment(Pos.BASELINE_LEFT);
        Label lblSatuan = new Label(satuan);
        lblSatuan.setFont(fReg14);
        lblSatuan.setStyle("-fx-text-fill: " + TEXT_MUTED + ";");
        nilaiBox.getChildren().addAll(lblNilai, lblSatuan);

        card.getChildren().addAll(header, nilaiBox);
        return card;
    }

    private VBox buildAktivitasCard() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: " + BG_CARD + "; -fx-background-radius: 12;");
        VBox.setVgrow(card, Priority.ALWAYS);

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label lblIcon  = new Label("🕐");
        Label lblJudul = new Label("AKTIVITAS");
        lblJudul.setFont(fReg12);
        lblJudul.setStyle("-fx-text-fill: " + TEXT_MUTED + ";");
        header.getChildren().addAll(lblIcon, lblJudul);

        aktivitasBox = new VBox(0);
        card.getChildren().addAll(header, aktivitasBox);
        return card;
    }

    // =========================================================
    // NAVIGASI
    // =========================================================
    private void navigasiKeProdukView() {
        if (mainController != null) {
            mainController.navigasiProduk();
        }
    }

    // =========================================================
    // REFRESH DATA
    // =========================================================
    public void refresh() {
        List<ProduksiHarian> data = ProduksiHarian.getByDateRange(dari, sampai);
        boolean adaData = data != null && !data.isEmpty();

        emptyStateBox.setVisible(!adaData);
        emptyStateBox.setManaged(!adaData);
        dataStateBox.setVisible(adaData);
        dataStateBox.setManaged(adaData);

        if (adaData) {
            updateStatCards(data);
            updateGrafik(data);
            updatePerformingTables(data);
        }
        updateAktivitas(data);
    }

    private void updateStatCards(List<ProduksiHarian> data) {
        Map<String, Object> r = statistikService.getRingkasanDariData(data);
        int totalBersih = (int) r.get("totalBersih");
        int totalDefect = (int) r.get("totalDefect");
        int totalAktual = (int) r.get("totalAktual");
        double pctDefect = (double) r.getOrDefault("persentaseDefect", 0.0);
        double performa  = totalAktual > 0 ? 100.0 - pctDefect : 0;

        lblTotalProduksi.setText(String.format("%,d", totalBersih));
        lblTotalDefect.setText(String.format("%,d", totalDefect));
        lblPerforma.setText(String.format("%.0f%%", performa));
    }

    private void updateGrafik(List<ProduksiHarian> data) {
        List<Map<String, Object>> tren = statistikService.getTrenDariData(data);
        if (tren.size() < 2) return;

        GraphicsContext gc = grafikCanvas.getGraphicsContext2D();
        double w = grafikCanvas.getWidth(), h = grafikCanvas.getHeight();
        double padL = 45, padR = 10, padT = 10, padB = 30;
        double cW = w - padL - padR, cH = h - padT - padB;

        gc.clearRect(0, 0, w, h);

        int n = tren.size();
        int maxVal = (int) (tren.stream()
                .mapToInt(p -> Math.max((int) p.get("totalAktual"), (int) p.get("totalDefect")))
                .max().orElse(1) * 1.2);

        double[] xPts = new double[n], yAkt = new double[n], yDef = new double[n];
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d MMM", new Locale("id"));

        for (int i = 0; i < n; i++) {
            xPts[i] = padL + (i / (double) (n - 1)) * cW;
            yAkt[i] = padT + cH - ((int) tren.get(i).get("totalAktual") / (double) maxVal) * cH;
            yDef[i] = padT + cH - ((int) tren.get(i).get("totalDefect") / (double) maxVal) * cH;
        }

        // Area produksi hijau
        gc.setFill(Color.web("#00e5a0", 0.25));
        gc.beginPath(); gc.moveTo(xPts[0], padT + cH);
        for (int i = 0; i < n; i++) gc.lineTo(xPts[i], yAkt[i]);
        gc.lineTo(xPts[n-1], padT + cH); gc.closePath(); gc.fill();
        gc.setStroke(Color.web(COLOR_GREEN)); gc.setLineWidth(2.5);
        gc.beginPath(); gc.moveTo(xPts[0], yAkt[0]);
        for (int i = 1; i < n; i++) gc.lineTo(xPts[i], yAkt[i]); gc.stroke();

        // Area defect merah
        gc.setFill(Color.web("#e05a5a", 0.25));
        gc.beginPath(); gc.moveTo(xPts[0], padT + cH);
        for (int i = 0; i < n; i++) gc.lineTo(xPts[i], yDef[i]);
        gc.lineTo(xPts[n-1], padT + cH); gc.closePath(); gc.fill();
        gc.setStroke(Color.web(COLOR_RED)); gc.setLineWidth(2.5);
        gc.beginPath(); gc.moveTo(xPts[0], yDef[0]);
        for (int i = 1; i < n; i++) gc.lineTo(xPts[i], yDef[i]); gc.stroke();

        // Label sumbu X
        gc.setFont(Font.font("Plus Jakarta Sans", 11));
        for (int i = 0; i < n; i++) {
            try {
                String lbl = LocalDate.parse(tren.get(i).get("tanggal").toString())
                        .format(fmt).toUpperCase();
                gc.setFill(Color.web(TEXT_MUTED));
                gc.fillText(lbl, xPts[i] - 18, h - 5);
            } catch (Exception ignored) {}
        }

        // Grid + label sumbu Y
        for (int step = 0; step <= 4; step++) {
            int val = (int) (maxVal * step / 4.0);
            double y = padT + cH - (val / (double) maxVal) * cH;
            gc.setFill(Color.web(TEXT_MUTED));
            gc.fillText(String.valueOf(val), 0, y + 4);
            gc.setStroke(Color.web("#1e3a3a", 0.6)); gc.setLineWidth(0.5);
            gc.strokeLine(padL, y, w - padR, y);
        }
    }

    private void updatePerformingTables(List<ProduksiHarian> data) {
        Map<Integer, int[]> perProduk = new LinkedHashMap<>();
        for (ProduksiHarian ph : data) {
            perProduk.computeIfAbsent(ph.getIdProduk(), k -> new int[2]);
            perProduk.get(ph.getIdProduk())[0] += ph.getJumlahAktual();
            perProduk.get(ph.getIdProduk())[1] += ph.getJumlahDefect();
        }

        List<Map.Entry<Integer, int[]>> sorted = new ArrayList<>(perProduk.entrySet());
        sorted.sort((a, b) -> {
            double pA = a.getValue()[0] > 0 ? (1 - a.getValue()[1] / (double) a.getValue()[0]) * 100 : 0;
            double pB = b.getValue()[0] > 0 ? (1 - b.getValue()[1] / (double) b.getValue()[0]) * 100 : 0;
            return Double.compare(pB, pA);
        });

        topPerformingBox.getChildren().clear();
        sorted.stream().limit(3).forEach(e -> topPerformingBox.getChildren().add(buildTabelRow(e)));

        worstPerformingBox.getChildren().clear();
        List<Map.Entry<Integer, int[]>> worst = new ArrayList<>(sorted);
        Collections.reverse(worst);
        worst.stream().limit(3).forEach(e -> worstPerformingBox.getChildren().add(buildTabelRow(e)));
    }

    private HBox buildTabelRow(Map.Entry<Integer, int[]> entry) {
        int idProduk = entry.getKey();
        int aktual   = entry.getValue()[0];
        int defect   = entry.getValue()[1];
        double perf  = aktual > 0 ? (1 - defect / (double) aktual) * 100 : 0;

        Produk p    = produkController.getProdukById(idProduk);
        String nama = p != null ? p.getNama() : "Produk #" + idProduk;
        String kode = p != null && p.getKode() != null ? p.getKode() : String.valueOf(idProduk);

        HBox row = new HBox();
        row.setPadding(new Insets(10, 0, 10, 0));
        row.setStyle("-fx-border-color: " + BG_CARD2 + "; -fx-border-width: 0 0 1 0;");

        double[] widths = {80, 220, 130, 150, 100};
        String[] values = {kode, nama, String.valueOf(defect), String.valueOf(aktual), String.format("%.0f%%", perf)};

        for (int i = 0; i < values.length; i++) {
            Label lbl = new Label(values[i]);
            lbl.setFont(fReg13);
            lbl.setStyle("-fx-text-fill: " + TEXT_WHITE + ";");
            lbl.setPrefWidth(widths[i]);
            row.getChildren().add(lbl);
        }
        return row;
    }

    private void updateAktivitas(List<ProduksiHarian> data) {
        aktivitasBox.getChildren().clear();

        if (data == null || data.isEmpty()) {
            Label lbl = new Label("Tidak ada aktivitas terbaru");
            lbl.setFont(fReg13);
            lbl.setStyle("-fx-text-fill: " + TEXT_MUTED + ";");
            lbl.setPadding(new Insets(16, 0, 0, 0));
            aktivitasBox.getChildren().add(lbl);
            return;
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        data.stream()
                .sorted(Comparator.comparing(ProduksiHarian::getTanggalProduksi).reversed())
                .limit(5)
                .forEach(ph -> {
                    Produk p    = produkController.getProdukById(ph.getIdProduk());
                    String nama = p != null ? p.getNama() : "Produk #" + ph.getIdProduk();

                    VBox item = new VBox(2);
                    item.setPadding(new Insets(10, 0, 10, 0));
                    item.setStyle("-fx-border-color: " + BG_CARD2 + "; -fx-border-width: 0 0 1 0;");

                    HBox topRow = new HBox();
                    topRow.setAlignment(Pos.CENTER_LEFT);
                    Label lblAksi = new Label("Input Data Produksi");
                    lblAksi.setFont(fBold14);
                    lblAksi.setStyle("-fx-text-fill: " + TEXT_WHITE + ";");
                    Region sp = new Region();
                    HBox.setHgrow(sp, Priority.ALWAYS);
                    Label lblTgl = new Label(ph.getTanggalProduksi().format(fmt));
                    lblTgl.setFont(fReg12);
                    lblTgl.setStyle("-fx-text-fill: " + TEXT_MUTED + ";");
                    topRow.getChildren().addAll(lblAksi, sp, lblTgl);

                    Label lblUnit = new Label(ph.getJumlahAktual() + " Unit");
                    lblUnit.setFont(fReg12);
                    lblUnit.setStyle("-fx-text-fill: " + TEXT_MUTED + ";");

                    Label lblNama = new Label(nama);
                    lblNama.setFont(fReg12);
                    lblNama.setStyle("-fx-text-fill: " + TEXT_MUTED + ";");

                    item.getChildren().addAll(topRow, lblUnit, lblNama);
                    aktivitasBox.getChildren().add(item);
                });
    }

    // =========================================================
    // HELPER
    // =========================================================
    private String getGreeting() {
        int hour = java.time.LocalTime.now().getHour();
        if (hour < 11) return "Selamat Pagi";
        if (hour < 15) return "Selamat Siang";
        if (hour < 18) return "Selamat Sore";
        return "Selamat Malam";
    }

    private String btnStyleAccent() {
        return "-fx-background-color: " + ACCENT + ";" +
                "-fx-text-fill: #0a1f1f;" +
                "-fx-background-radius: 8;" +
                "-fx-padding: 12;" +
                "-fx-cursor: hand;";
    }

    private void showAlert(String pesan) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(null);
        alert.setContentText(pesan);
        alert.showAndWait();
    }

    public void showDashboard()   { refresh(); }
    public void onFilterChanged() { refresh(); }
}