package view;

import controller.ReportController;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import model.LaporanProduksi;
import model.Produk;
import repository.ProdukRepository;
import service.FileGeneratorService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * EksporLaporanView — controller JavaFX untuk EksporLaporanView.fxml
 *
 * Mengelola tiga state tampilan:
 *   FORM    → user mengisi parameter laporan
 *   LOADING → laporan sedang diproses di background thread
 *   HASIL   → laporan selesai, user bisa unduh atau buat laporan lain
 */
public class EksporLaporanView {

    // =========================================================
    // FXML — panel state
    // =========================================================
    @FXML private VBox panelForm;
    @FXML private VBox panelLoading;
    @FXML private VBox panelHasil;

    // =========================================================
    // FXML — komponen FORM
    // =========================================================
    @FXML private TextField fieldCariProduk;
    @FXML private VBox listProdukContainer;
    @FXML private Button btnSemuaProduk;

    @FXML private DatePicker fieldTglMulai;
    @FXML private DatePicker fieldTglSelesai;
    @FXML private ComboBox<String> comboFormat;

    // Tombol shortcut rentang waktu
    @FXML private Button btn7Hari;
    @FXML private Button btn30Hari;
    @FXML private Button btn6Bulan;
    @FXML private Button btnSemuaData;

    // =========================================================
    // FXML — komponen LOADING
    // =========================================================
    @FXML private ProgressBar progressBar;

    // =========================================================
    // FXML — komponen HASIL
    // =========================================================
    @FXML private ImageView ikonFormatHasil;
    @FXML private Label labelFormatHasil;

    // =========================================================
    // STATE INTERNAL
    // =========================================================
    private final ProdukRepository produkRepository = new ProdukRepository();
    private final ReportController reportController = new ReportController();
    private final FileGeneratorService fileGeneratorService = new FileGeneratorService();

    private List<Produk> semuaProduk = new ArrayList<>();
    private final List<Integer> idProdukTerpilih = new ArrayList<>();
    private boolean semuaProdukDipilih = false;

    // Shortcut rentang waktu yang sedang aktif
    private Button shortcutAktif = null;

    // Path file temp hasil generate — disimpan untuk diunduh nanti
    private Path tempFilePath = null;
    private String formatTerpilih = "pdf";

    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {
        tampilkanPanel(panelForm);

        // Setup ComboBox format
        comboFormat.getItems().addAll("PDF", "Excel (.xlsx)");
        comboFormat.setValue("PDF");

        // Load daftar produk
        semuaProduk = produkRepository.getAllProduk();
        tampilkanListProduk(semuaProduk);

        // Listener search produk
        fieldCariProduk.textProperty().addListener((obs, lama, baru) -> filterProduk(baru));

        // Listener DatePicker — kalau diisi manual, reset shortcut aktif
        fieldTglMulai.valueProperty().addListener((obs, lama, baru) -> {
            if (baru != null) nonaktifkanShortcut();
        });
        fieldTglSelesai.valueProperty().addListener((obs, lama, baru) -> {
            if (baru != null) nonaktifkanShortcut();
        });
    }

    // =========================================================
    // LIST PRODUK
    // =========================================================

    private void tampilkanListProduk(List<Produk> list) {
        listProdukContainer.getChildren().clear();
        for (Produk p : list) {
            listProdukContainer.getChildren().add(buatItemProduk(p));
        }
    }

    private HBox buatItemProduk(Produk produk) {
        HBox item = new HBox(12);
        item.setStyle("-fx-padding: 10 14; -fx-cursor: hand; -fx-background-radius: 8;");

        // Checkbox pilih produk
        CheckBox cb = new CheckBox();
        cb.setSelected(idProdukTerpilih.contains(produk.getIdProduk()));
        cb.selectedProperty().addListener((obs, lama, baru) -> {
            if (baru) {
                if (!idProdukTerpilih.contains(produk.getIdProduk()))
                    idProdukTerpilih.add(produk.getIdProduk());
            } else {
                idProdukTerpilih.remove((Integer) produk.getIdProduk());
                semuaProdukDipilih = false;
                btnSemuaProduk.setStyle(styleBtn(false));
            }
        });

        VBox info = new VBox(2);
        Label nama = new Label(produk.getNama());
        nama.setStyle("-fx-text-fill: white; -fx-font-size: 13; -fx-font-weight: bold;");
        Label kode = new Label(produk.getKode() != null ? produk.getKode() : "-");
        kode.setStyle("-fx-text-fill: #5a8a8a; -fx-font-size: 11;");
        info.getChildren().addAll(nama, kode);

        item.getChildren().addAll(cb, info);

        // Hover effect
        item.setOnMouseEntered(e -> item.setStyle(
                "-fx-padding: 10 14; -fx-cursor: hand; -fx-background-color: #1e3a3a; -fx-background-radius: 8;"));
        item.setOnMouseExited(e -> item.setStyle(
                "-fx-padding: 10 14; -fx-cursor: hand; -fx-background-radius: 8;"));

        // Klik item = toggle checkbox
        item.setOnMouseClicked(e -> cb.setSelected(!cb.isSelected()));

        return item;
    }

    private void filterProduk(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            tampilkanListProduk(semuaProduk);
            return;
        }
        String kw = keyword.toLowerCase();
        List<Produk> hasil = semuaProduk.stream()
                .filter(p -> p.getNama().toLowerCase().contains(kw)
                        || (p.getKode() != null && p.getKode().toLowerCase().contains(kw)))
                .toList();
        tampilkanListProduk(hasil);
    }

    // =========================================================
    // AKSI — tombol shortcut produk
    // =========================================================

    @FXML
    public void klikSemuaProduk() {
        semuaProdukDipilih = !semuaProdukDipilih;
        btnSemuaProduk.setStyle(styleBtn(semuaProdukDipilih));

        if (semuaProdukDipilih) {
            idProdukTerpilih.clear();  // kosongkan — nanti ReportController query semua
        }
        // Re-render list agar checkbox ikut update
        tampilkanListProduk(
                fieldCariProduk.getText().isEmpty()
                        ? semuaProduk
                        : semuaProduk.stream()
                        .filter(p -> p.getNama().toLowerCase()
                                .contains(fieldCariProduk.getText().toLowerCase()))
                        .toList()
        );
    }

    // =========================================================
    // AKSI — shortcut rentang waktu
    // =========================================================

    @FXML public void klik7Hari()    { terapkanShortcut(btn7Hari, 7); }
    @FXML public void klik30Hari()   { terapkanShortcut(btn30Hari, 30); }
    @FXML public void klik6Bulan()   { terapkanShortcut(btn6Bulan, 180); }

    @FXML
    public void klikSemuaData() {
        // "Semuanya" = tidak ada filter tanggal, tglMulai & tglSelesai = null
        aktifkanShortcut(btnSemuaData);
        fieldTglMulai.setValue(null);
        fieldTglSelesai.setValue(null);
    }

    private void terapkanShortcut(Button btn, int hariKebelakang) {
        aktifkanShortcut(btn);
        LocalDate selesai = LocalDate.now();
        LocalDate mulai = selesai.minusDays(hariKebelakang - 1L);
        // Set tanpa memicu listener "reset shortcut"
        fieldTglMulai.valueProperty().removeListener(null);
        fieldTglMulai.setValue(mulai);
        fieldTglSelesai.setValue(selesai);
    }

    private void aktifkanShortcut(Button btn) {
        nonaktifkanShortcut();
        shortcutAktif = btn;
        btn.setStyle(styleShortcutAktif());
    }

    private void nonaktifkanShortcut() {
        if (shortcutAktif != null) {
            shortcutAktif.setStyle(styleShortcutNormal());
            shortcutAktif = null;
        }
    }

    // =========================================================
    // AKSI — Buat Laporan
    // =========================================================

    @FXML
    public void klikBuatLaporan() {
        // Validasi produk
        if (!semuaProdukDipilih && idProdukTerpilih.isEmpty()) {
            tampilkanError("Pilih minimal satu produk atau klik 'Semua Produk'.");
            return;
        }

        // Validasi rentang waktu
        LocalDate tglMulai = fieldTglMulai.getValue();
        LocalDate tglSelesai = fieldTglSelesai.getValue();

        // Jika "Semuanya" dipilih, keduanya null — ini valid
        boolean semuaData = (shortcutAktif == btnSemuaData);
        if (!semuaData) {
            if (tglMulai == null || tglSelesai == null) {
                tampilkanError("Isi rentang tanggal atau pilih shortcut waktu.");
                return;
            }
            if (tglMulai.isAfter(tglSelesai)) {
                tampilkanError("Tanggal mulai tidak boleh setelah tanggal selesai.");
                return;
            }
        }

        // Format file
        formatTerpilih = comboFormat.getValue().startsWith("PDF") ? "pdf" : "xlsx";

        // Pindah ke state LOADING
        tampilkanPanel(panelLoading);
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);

        // Variabel final untuk lambda
        final LocalDate tglMulaiFinal = semuaData ? null : tglMulai;
        final LocalDate tglSeleseaiFinal = semuaData ? null : tglSelesai;
        final List<Integer> idList = semuaProdukDipilih
                ? new ArrayList<>()   // kosong = semua produk
                : new ArrayList<>(idProdukTerpilih);

        // Jalankan di background thread agar UI tidak freeze
        Task<Path> task = new Task<>() {
            @Override
            protected Path call() throws Exception {
                updateProgress(0.3, 1.0);
                LaporanProduksi laporan = reportController.susunLaporan(
                        idList, tglMulaiFinal, tglSeleseaiFinal);
                updateProgress(0.7, 1.0);
                Path path = fileGeneratorService.generate(laporan, formatTerpilih);
                updateProgress(1.0, 1.0);
                return path;
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());

        task.setOnSucceeded(e -> {
            tempFilePath = task.getValue();
            Platform.runLater(this::tampilkanHasil);
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            Platform.runLater(() -> {
                tampilkanPanel(panelForm);
                tampilkanError("Gagal membuat laporan: " + ex.getMessage());
            });
        });

        new Thread(task).start();
    }

    // =========================================================
    // AKSI — Unduh Laporan
    // =========================================================

    @FXML
    public void klikUnduhLaporan() {
        if (tempFilePath == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Simpan Laporan");
        chooser.setInitialFileName("laporan_produksi." + formatTerpilih);

        if ("pdf".equals(formatTerpilih)) {
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PDF File", "*.pdf"));
        } else {
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Excel File", "*.xlsx"));
        }

        File tujuan = chooser.showSaveDialog(panelHasil.getScene().getWindow());
        if (tujuan == null) return; // user cancel

        try {
            Files.copy(tempFilePath, tujuan.toPath(), StandardCopyOption.REPLACE_EXISTING);
            tampilkanInfo("Laporan berhasil disimpan di:\n" + tujuan.getAbsolutePath());
        } catch (IOException ex) {
            tampilkanError("Gagal menyimpan file: " + ex.getMessage());
        }
    }

    // =========================================================
    // AKSI — Buat Laporan Lain (reset ke FORM)
    // =========================================================

    @FXML
    public void klikBuatLaporanLain() {
        // Bersihkan temp file lama
        if (tempFilePath != null) {
            try { Files.deleteIfExists(tempFilePath); } catch (IOException ignored) {}
            tempFilePath = null;
        }

        // Reset state
        idProdukTerpilih.clear();
        semuaProdukDipilih = false;
        btnSemuaProduk.setStyle(styleBtn(false));
        fieldCariProduk.clear();
        fieldTglMulai.setValue(null);
        fieldTglSelesai.setValue(null);
        comboFormat.setValue("PDF");
        nonaktifkanShortcut();
        tampilkanListProduk(semuaProduk);

        tampilkanPanel(panelForm);
    }

    // =========================================================
    // HELPER — state management panel
    // =========================================================

    private void tampilkanPanel(VBox panel) {
        panelForm.setVisible(false);    panelForm.setManaged(false);
        panelLoading.setVisible(false); panelLoading.setManaged(false);
        panelHasil.setVisible(false);   panelHasil.setManaged(false);

        panel.setVisible(true);
        panel.setManaged(true);
    }

    private void tampilkanHasil() {
        // Ganti ikon sesuai format
        if ("pdf".equals(formatTerpilih)) {
            labelFormatHasil.setText("PDF");
            // Ganti ikon jika ada asset
            // ikonFormatHasil.setImage(new Image("/assets/icon_pdf.png"));
        } else {
            labelFormatHasil.setText("Excel (.xlsx)");
            // ikonFormatHasil.setImage(new Image("/assets/icon_excel.png"));
        }
        tampilkanPanel(panelHasil);
    }

    private void tampilkanError(String pesan) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Terjadi Kesalahan");
        alert.setContentText(pesan);
        alert.showAndWait();
    }

    private void tampilkanInfo(String pesan) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(pesan);
        alert.showAndWait();
    }

    // =========================================================
    // HELPER — style string
    // =========================================================

    private String styleBtn(boolean aktif) {
        return aktif
                ? "-fx-background-color: #00e5a0; -fx-text-fill: #0a1f1f; " +
                "-fx-background-radius: 20; -fx-padding: 4 16; -fx-cursor: hand;"
                : "-fx-background-color: transparent; -fx-text-fill: white; " +
                "-fx-border-color: #5a8a8a; -fx-border-radius: 20; " +
                "-fx-background-radius: 20; -fx-padding: 4 16; -fx-cursor: hand;";
    }

    private String styleShortcutAktif() {
        return "-fx-background-color: #00e5a0; -fx-text-fill: #0a1f1f; " +
                "-fx-background-radius: 8; -fx-padding: 6 14; -fx-cursor: hand;";
    }

    private String styleShortcutNormal() {
        return "-fx-background-color: #1e3a3a; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-padding: 6 14; -fx-cursor: hand;";
    }
}