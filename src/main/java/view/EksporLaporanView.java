package view;

import controller.ReportController;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import model.LaporanProduksi;
import model.Produk;
import service.FileGeneratorService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EksporLaporanView {

    @FXML private VBox panelForm;
    @FXML private VBox panelLoading;
    @FXML private VBox panelHasil;

    @FXML private TextField fieldCariProduk;
    @FXML private HBox listProdukHorizontal; // Pastikan di FXML diganti dari FlowPane ke HBox
    @FXML private Button btnSemuaProduk;

    @FXML private DatePicker fieldTglMulai;
    @FXML private DatePicker fieldTglSelesai;
    @FXML private ComboBox<String> comboFormat;

    @FXML private Button btn7Hari;
    @FXML private Button btn30Hari;
    @FXML private Button btn6Bulan;
    @FXML private Button btnSemuaData;

    @FXML private ProgressBar progressBar;
    @FXML private Label labelFormatHasil;

    private final ReportController reportController = new ReportController();
    private final FileGeneratorService fileGeneratorService = new FileGeneratorService();

    private List<Produk> semuaProduk = new ArrayList<>();
    private final List<Integer> idProdukTerpilih = new ArrayList<>();
    private boolean semuaProdukDipilih = false;

    private Button shortcutAktif = null;
    private Path tempFilePath = null;
    private String formatTerpilih = "pdf";

    private final ChangeListener<LocalDate> dateChangeListener = (obs, lama, baru) -> {
        if (baru != null) nonaktifkanShortcut();
    };

    @FXML
    public void initialize() {
        tampilkanPanel(panelForm);
        setupComboBoxFormat();

        semuaProduk = Produk.getAll();
        tampilkanListProduk(semuaProduk);

        // Pencarian tetap berfungsi untuk memfilter list horizontal
        fieldCariProduk.textProperty().addListener((obs, lama, baru) -> {
            // Hanya filter jika user mengetik manual (bukan diisi otomatis oleh sistem)
            if (fieldCariProduk.isFocused()) {
                filterProduk(baru);
            }
        });

        fieldTglMulai.valueProperty().addListener(dateChangeListener);
        fieldTglSelesai.valueProperty().addListener(dateChangeListener);
        btnSemuaProduk.setStyle(styleBtn(false));
    }

    private void setupComboBoxFormat() {
        comboFormat.getItems().addAll("PDF", "Excel (.xlsx)");
        comboFormat.setValue("PDF");
        comboFormat.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else { setText(item); setStyle("-fx-text-fill: white;"); }
            }
        });
        comboFormat.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle("-fx-background-color: transparent;"); }
                else { setText(item); setStyle("-fx-background-color: #132929; -fx-text-fill: white; -fx-padding: 10;"); }
            }
        });
    }

    private void tampilkanListProduk(List<Produk> list) {
        listProdukHorizontal.getChildren().clear();
        for (Produk p : list) {
            listProdukHorizontal.getChildren().add(buatItemProdukHorizontal(p));
        }
    }

    private VBox buatItemProdukHorizontal(Produk produk) {
        VBox item = new VBox(8); // Spacing antar elemen
        item.setAlignment(Pos.CENTER);
        // Style card: Radius 16, background gelap, padding rapat
        item.setStyle("-fx-padding: 12; -fx-background-color: #1e3a3a; -fx-background-radius: 16; -fx-min-width: 140; -fx-cursor: hand;");

        // 1. Checkbox (Paling Atas)
        CheckBox cb = new CheckBox();
        cb.setSelected(idProdukTerpilih.contains(produk.getIdProduk()));

        // 2. Gambar Produk (Retrieve Path dari SQL)
        ImageView img = new ImageView();
        // Gunakan getter getFoto() sesuai contoh suksesmu
        if (produk.getFoto() != null && !produk.getFoto().isEmpty()) {
            try {
                // Tambahkan "file:" agar JavaFX mengenali path lokal
                img.setImage(new Image("file:" + produk.getFoto(), true)); // 'true' untuk load background
            } catch (Exception e) {
                System.out.println("Gagal load gambar file: " + e.getMessage());
            }
        } else {
            // Fallback jika tidak ada foto
            try {
                img.setImage(new Image(getClass().getResourceAsStream("/icons/package.png")));
            } catch (Exception ignored) {}
        }
        img.setFitWidth(50);
        img.setFitHeight(50);
        img.setPreserveRatio(true);

        // 3. Nama Produk
        Label nama = new Label(produk.getNama());
        nama.setStyle("-fx-text-fill: white; -fx-font-size: 13; -fx-font-weight: bold;");
        nama.setWrapText(true);
        nama.setMaxWidth(120);
        nama.setAlignment(Pos.CENTER);

        // 4. ID Produk (Di bawah nama)
        Label idLabel = new Label("ID: " + produk.getIdProduk());
        idLabel.setStyle("-fx-text-fill: #00e5a0; -fx-font-size: 10;");

        item.getChildren().addAll(cb, img, nama, idLabel);

        // Logic Pilih
        cb.selectedProperty().addListener((obs, lama, baru) -> {
            if (baru) {
                if (!idProdukTerpilih.contains(produk.getIdProduk())) idProdukTerpilih.add(produk.getIdProduk());
            } else {
                idProdukTerpilih.remove((Integer) produk.getIdProduk());
                semuaProdukDipilih = false;
                btnSemuaProduk.setStyle(styleBtn(false));
            }
            updateTextFieldStatus();
        });

        // Supaya satu card bisa diklik
        item.setOnMouseClicked(e -> cb.setSelected(!cb.isSelected()));

        return item;
    }

    private void updateTextFieldStatus() {
        if (semuaProdukDipilih) {
            fieldCariProduk.setText("Semua Produk");
        } else if (idProdukTerpilih.isEmpty()) {
            fieldCariProduk.setText("");
        } else {
            // Ambil nama produk berdasarkan ID yang terpilih
            String gabunganNama = semuaProduk.stream()
                    .filter(p -> idProdukTerpilih.contains(p.getIdProduk()))
                    .map(Produk::getNama)
                    .collect(Collectors.joining(", "));
            fieldCariProduk.setText(gabunganNama);
        }
    }

    private void filterProduk(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            tampilkanListProduk(semuaProduk);
            return;
        }
        String kw = keyword.toLowerCase();
        List<Produk> hasil = semuaProduk.stream()
                .filter(p -> p.getNama().toLowerCase().contains(kw))
                .toList();
        tampilkanListProduk(hasil);
    }

    @FXML
    public void klikSemuaProduk() {
        semuaProdukDipilih = !semuaProdukDipilih;
        btnSemuaProduk.setStyle(styleBtn(semuaProdukDipilih));

        idProdukTerpilih.clear();
        if (semuaProdukDipilih) {
            for (Produk p : semuaProduk) idProdukTerpilih.add(p.getIdProduk());
        }

        updateTextFieldStatus();
        tampilkanListProduk(semuaProduk);
    }

    // --- Logika Shortcut Tanggal & Buat Laporan Tetap Sama ---
    @FXML public void klik7Hari()    { terapkanShortcut(btn7Hari, 7); }
    @FXML public void klik30Hari()   { terapkanShortcut(btn30Hari, 30); }
    @FXML public void klik6Bulan()   { terapkanShortcut(btn6Bulan, 180); }
    @FXML public void klikSemuaData() { aktifkanShortcut(btnSemuaData); fieldTglMulai.setValue(null); fieldTglSelesai.setValue(null); }

    private void terapkanShortcut(Button btn, int hariKebelakang) {
        aktifkanShortcut(btn);
        LocalDate selesai = LocalDate.now();
        LocalDate mulai = selesai.minusDays(hariKebelakang - 1L);
        fieldTglMulai.valueProperty().removeListener(dateChangeListener);
        fieldTglMulai.setValue(mulai);
        fieldTglMulai.valueProperty().addListener(dateChangeListener);
        fieldTglSelesai.setValue(selesai);
    }

    private void aktifkanShortcut(Button btn) {
        nonaktifkanShortcut(); shortcutAktif = btn; btn.setStyle(styleShortcutAktif());
    }

    private void nonaktifkanShortcut() {
        if (shortcutAktif != null) { shortcutAktif.setStyle(styleShortcutNormal()); shortcutAktif = null; }
    }

    @FXML
    public void klikBuatLaporan() {
        if (idProdukTerpilih.isEmpty() && !semuaProdukDipilih) { tampilkanError("Pilih minimal satu produk!"); return; }
        formatTerpilih = comboFormat.getValue().contains("Excel") ? "xlsx" : "pdf";
        tampilkanPanel(panelLoading);
        progressBar.progressProperty().unbind();
        progressBar.setProgress(0);

        final List<Integer> idList = new ArrayList<>(idProdukTerpilih);
        final LocalDate tglMulai = fieldTglMulai.getValue();
        final LocalDate tglSelesai = fieldTglSelesai.getValue();

        Task<Path> task = new Task<>() {
            @Override protected Path call() throws Exception {
                updateProgress(2, 10);
                LaporanProduksi laporan = reportController.susunLaporan(idList, tglMulai, tglSelesai);
                updateProgress(6, 10);
                return fileGeneratorService.generate(laporan, formatTerpilih);
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());
        task.setOnSucceeded(e -> { tempFilePath = task.getValue(); tampilkanHasil(); });
        task.setOnFailed(e -> { tampilkanPanel(panelForm); tampilkanError("Gagal ekspor: " + task.getException().getMessage()); });
        new Thread(task).start();
    }

    @FXML
    public void klikUnduhLaporan() {
        if (tempFilePath == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Simpan Laporan");
        String ext = "xlsx".equals(formatTerpilih) ? ".xlsx" : ".pdf";
        chooser.setInitialFileName("laporan_produksi_" + LocalDate.now() + ext);
        File tujuan = chooser.showSaveDialog(panelHasil.getScene().getWindow());
        if (tujuan != null) {
            try {
                Files.copy(tempFilePath, tujuan.toPath(), StandardCopyOption.REPLACE_EXISTING);
                tampilkanInfo("Laporan berhasil disimpan!");
            } catch (IOException ex) { tampilkanError("Gagal menyimpan file"); }
        }
    }

    @FXML
    public void klikBuatLaporanLain() {
        idProdukTerpilih.clear(); semuaProdukDipilih = false;
        btnSemuaProduk.setStyle(styleBtn(false));
        fieldTglMulai.setValue(null); fieldTglSelesai.setValue(null);
        fieldCariProduk.clear();
        nonaktifkanShortcut(); tampilkanListProduk(semuaProduk); tampilkanPanel(panelForm);
    }

    private void tampilkanPanel(VBox panel) {
        panelForm.setVisible(false); panelForm.setManaged(false);
        panelLoading.setVisible(false); panelLoading.setManaged(false);
        panelHasil.setVisible(false); panelHasil.setManaged(false);
        panel.setVisible(true); panel.setManaged(true);
    }

    private void tampilkanHasil() { labelFormatHasil.setText(formatTerpilih.toUpperCase()); tampilkanPanel(panelHasil); }
    private void tampilkanError(String pesan) { Alert a = new Alert(Alert.AlertType.ERROR); a.setContentText(pesan); a.showAndWait(); }
    private void tampilkanInfo(String pesan) { Alert a = new Alert(Alert.AlertType.INFORMATION); a.setContentText(pesan); a.showAndWait(); }

    private String styleBtn(boolean aktif) {
        return aktif ? "-fx-background-color: #00e5a0; -fx-text-fill: #0a1f1f; -fx-background-radius: 16; -fx-padding: 4 16; -fx-cursor: hand;"
                : "-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: #5a8a8a; -fx-border-radius: 16; -fx-background-radius: 16; -fx-padding: 4 16; -fx-cursor: hand;";
    }
    private String styleShortcutAktif() { return "-fx-background-color: #00e5a0; -fx-text-fill: #0a1f1f; -fx-background-radius: 16; -fx-padding: 6 14; -fx-cursor: hand;"; }
    private String styleShortcutNormal() { return "-fx-background-color: #1e3a3a; -fx-text-fill: white; -fx-background-radius: 16; -fx-padding: 6 14; -fx-cursor: hand;"; }
}