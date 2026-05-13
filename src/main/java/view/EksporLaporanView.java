package view;

import controller.ReportController;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener; // Tambahkan import ini
import javafx.concurrent.Task;
import javafx.fxml.FXML;
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

public class EksporLaporanView {

    @FXML private VBox panelForm;
    @FXML private VBox panelLoading;
    @FXML private VBox panelHasil;

    @FXML private TextField fieldCariProduk;
    @FXML private FlowPane listProdukContainer;
    @FXML private Button btnSemuaProduk;

    @FXML private DatePicker fieldTglMulai;
    @FXML private DatePicker fieldTglSelesai;
    @FXML private ComboBox<String> comboFormat;

    @FXML private Button btn7Hari;
    @FXML private Button btn30Hari;
    @FXML private Button btn6Bulan;
    @FXML private Button btnSemuaData;

    @FXML private ProgressBar progressBar;

    @FXML private ImageView ikonFormatHasil;
    @FXML private Label labelFormatHasil;

    // =========================================================
    // PERBAIKAN 1: Ganti ProdukRepository menjadi Produk
    // =========================================================
    private final Produk produkManager = new Produk();
    private final ReportController reportController = new ReportController();
    private final FileGeneratorService fileGeneratorService = new FileGeneratorService();

    private List<Produk> semuaProduk = new ArrayList<>();
    private final List<Integer> idProdukTerpilih = new ArrayList<>();
    private boolean semuaProdukDipilih = false;

    private Button shortcutAktif = null;
    private Path tempFilePath = null;
    private String formatTerpilih = "pdf";

    // Simpan listener sebagai variabel agar bisa di-remove dengan benar
    private final ChangeListener<LocalDate> dateChangeListener = (obs, lama, baru) -> {
        if (baru != null) nonaktifkanShortcut();
    };

    @FXML
    public void initialize() {
        tampilkanPanel(panelForm);

        comboFormat.getItems().addAll("PDF", "Excel (.xlsx)");
        comboFormat.setValue("PDF");

        // Gunakan produkManager (karena repository sudah dimerge)
        semuaProduk = Produk.getAll();
        tampilkanListProduk(semuaProduk);

        fieldCariProduk.textProperty().addListener((obs, lama, baru) -> filterProduk(baru));

        // Tambahkan listener
        fieldTglMulai.valueProperty().addListener(dateChangeListener);
        fieldTglSelesai.valueProperty().addListener(dateChangeListener);
    }

    private void tampilkanListProduk(List<Produk> list) {
        listProdukContainer.getChildren().clear();
        for (Produk p : list) {
            listProdukContainer.getChildren().add(buatItemProduk(p));
        }
    }

    private HBox buatItemProduk(Produk produk) {
        HBox item = new HBox(12);
        item.setStyle("-fx-padding: 10 14; -fx-cursor: hand; -fx-background-radius: 8;");

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
        item.setOnMouseEntered(e -> item.setStyle("-fx-padding: 10 14; -fx-cursor: hand; -fx-background-color: #1e3a3a; -fx-background-radius: 8;"));
        item.setOnMouseExited(e -> item.setStyle("-fx-padding: 10 14; -fx-cursor: hand; -fx-background-radius: 8;"));
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

    @FXML
    public void klikSemuaProduk() {
        semuaProdukDipilih = !semuaProdukDipilih;
        btnSemuaProduk.setStyle(styleBtn(semuaProdukDipilih));
        if (semuaProdukDipilih) idProdukTerpilih.clear();
        tampilkanListProduk(fieldCariProduk.getText().isEmpty() ? semuaProduk : semuaProduk.stream().filter(p -> p.getNama().toLowerCase().contains(fieldCariProduk.getText().toLowerCase())).toList());
    }

    @FXML public void klik7Hari()    { terapkanShortcut(btn7Hari, 7); }
    @FXML public void klik30Hari()   { terapkanShortcut(btn30Hari, 30); }
    @FXML public void klik6Bulan()   { terapkanShortcut(btn6Bulan, 180); }

    @FXML
    public void klikSemuaData() {
        aktifkanShortcut(btnSemuaData);
        fieldTglMulai.setValue(null);
        fieldTglSelesai.setValue(null);
    }

    private void terapkanShortcut(Button btn, int hariKebelakang) {
        aktifkanShortcut(btn);
        LocalDate selesai = LocalDate.now();
        LocalDate mulai = selesai.minusDays(hariKebelakang - 1L);

        // PERBAIKAN 2: Hilangkan error "ambiguous" dengan membuang listener yang benar
        fieldTglMulai.valueProperty().removeListener(dateChangeListener);
        fieldTglMulai.setValue(mulai);
        fieldTglMulai.valueProperty().addListener(dateChangeListener);

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

    @FXML
    public void klikBuatLaporan() {
        // 1. Validasi awal
        if (!semuaProdukDipilih && idProdukTerpilih.isEmpty()) {
            tampilkanError("Pilih minimal satu produk!");
            return;
        }

        // 2. Persiapan UI
        formatTerpilih = comboFormat.getValue().contains("Excel") ? "xlsx" : "pdf";
        tampilkanPanel(panelLoading);

        // FIX: Unbind dulu agar tidak bentrok dengan sisa proses sebelumnya
        progressBar.progressProperty().unbind();
        progressBar.setProgress(0);

        final List<Integer> idList = semuaProdukDipilih ? new ArrayList<>() : new ArrayList<>(idProdukTerpilih);
        final LocalDate tglMulai = fieldTglMulai.getValue();
        final LocalDate tglSelesai = fieldTglSelesai.getValue();

        // 3. Task Proses Background
        Task<Path> task = new Task<>() {
            @Override
            protected Path call() throws Exception {
                updateProgress(2, 10); // Mulai

                // Ambil data
                LaporanProduksi laporan = reportController.susunLaporan(idList, tglMulai, tglSelesai);
                updateProgress(5, 10);

                // Generate File (Di sini biasanya Excel error kalau library POI tidak ada)
                return fileGeneratorService.generate(laporan, formatTerpilih);
            }
        };

        // 4. Handle Succeeded & Failed
        progressBar.progressProperty().bind(task.progressProperty());

        task.setOnSucceeded(e -> {
            tempFilePath = task.getValue();
            tampilkanHasil();
        });

        task.setOnFailed(e -> {
            tampilkanPanel(panelForm);
            Throwable ex = task.getException();
            ex.printStackTrace(); // LIHAT DI TERMINAL ERRORNYA

            String pesan = ex.getMessage();
            if (pesan != null && pesan.contains("poi")) {
                tampilkanError("Error Excel: Library Apache POI tidak ditemukan atau korup!");
            } else {
                tampilkanError("Gagal ekspor " + formatTerpilih.toUpperCase() + ": " + pesan);
            }
        });

        new Thread(task).start();
    }

    @FXML
    public void klikUnduhLaporan() {
        if (tempFilePath == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Simpan Laporan");
        chooser.setInitialFileName("laporan_produksi." + formatTerpilih);
        if ("pdf".equals(formatTerpilih)) {
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF File", "*.pdf"));
        } else {
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel File", "*.xlsx"));
        }
        File tujuan = chooser.showSaveDialog(panelHasil.getScene().getWindow());
        if (tujuan == null) return;
        try {
            Files.copy(tempFilePath, tujuan.toPath(), StandardCopyOption.REPLACE_EXISTING);
            tampilkanInfo("Laporan berhasil disimpan di:\n" + tujuan.getAbsolutePath());
        } catch (IOException ex) {
            tampilkanError("Gagal menyimpan file: " + ex.getMessage());
        }
    }

    @FXML
    public void klikBuatLaporanLain() {
        if (tempFilePath != null) {
            try { Files.deleteIfExists(tempFilePath); } catch (IOException ignored) {}
            tempFilePath = null;
        }
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

    private void tampilkanPanel(VBox panel) {
        panelForm.setVisible(false);    panelForm.setManaged(false);
        panelLoading.setVisible(false); panelLoading.setManaged(false);
        panelHasil.setVisible(false);   panelHasil.setManaged(false);
        panel.setVisible(true);
        panel.setManaged(true);
    }

    private void tampilkanHasil() {
        if ("pdf".equals(formatTerpilih)) {
            labelFormatHasil.setText("PDF");
        } else {
            labelFormatHasil.setText("Excel (.xlsx)");
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

    private String styleBtn(boolean aktif) {
        return aktif ? "-fx-background-color: #00e5a0; -fx-text-fill: #0a1f1f; -fx-background-radius: 20; -fx-padding: 4 16; -fx-cursor: hand;"
                : "-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: #5a8a8a; -fx-border-radius: 20; -fx-background-radius: 20; -fx-padding: 4 16; -fx-cursor: hand;";
    }

    private String styleShortcutAktif() {
        return "-fx-background-color: #00e5a0; -fx-text-fill: #0a1f1f; -fx-background-radius: 8; -fx-padding: 6 14; -fx-cursor: hand;";
    }

    private String styleShortcutNormal() {
        return "-fx-background-color: #1e3a3a; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 6 14; -fx-cursor: hand;";
    }
}