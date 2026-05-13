package view;

import controller.ReportController;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
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

        // Konfigurasi ComboBox agar teks berwarna Putih
        setupComboBoxFormat();

        semuaProduk = Produk.getAll();
        tampilkanListProduk(semuaProduk);

        fieldCariProduk.textProperty().addListener((obs, lama, baru) -> filterProduk(baru));

        fieldTglMulai.valueProperty().addListener(dateChangeListener);
        fieldTglSelesai.valueProperty().addListener(dateChangeListener);

        // Inisialisasi style tombol semua produk
        btnSemuaProduk.setStyle(styleBtn(false));
    }

    private void setupComboBoxFormat() {
        comboFormat.getItems().addAll("PDF", "Excel (.xlsx)");
        comboFormat.setValue("PDF");

        // Renderer untuk teks yang terpilih di box
        comboFormat.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else {
                    setText(item);
                    setStyle("-fx-text-fill: white;");
                }
            }
        });

        // Renderer untuk list pilihan di dalam dropdown
        comboFormat.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item);
                    setStyle("-fx-background-color: #132929; -fx-text-fill: white; -fx-padding: 10;");
                }
            }
        });
    }

    private void tampilkanListProduk(List<Produk> list) {
        listProdukContainer.getChildren().clear();
        for (Produk p : list) {
            listProdukContainer.getChildren().add(buatItemProduk(p));
        }
    }

    private HBox buatItemProduk(Produk produk) {
        // Radius 16 pada item produk
        HBox item = new HBox(12);
        item.setStyle("-fx-padding: 10 14; -fx-cursor: hand; -fx-background-radius: 16;");

        CheckBox cb = new CheckBox();
        cb.setSelected(idProdukTerpilih.contains(produk.getIdProduk()) || semuaProdukDipilih);
        cb.selectedProperty().addListener((obs, lama, baru) -> {
            if (baru) {
                if (!idProdukTerpilih.contains(produk.getIdProduk())) idProdukTerpilih.add(produk.getIdProduk());
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
        item.setOnMouseEntered(e -> item.setStyle("-fx-padding: 10 14; -fx-cursor: hand; -fx-background-color: #1e3a3a; -fx-background-radius: 16;"));
        item.setOnMouseExited(e -> item.setStyle("-fx-padding: 10 14; -fx-cursor: hand; -fx-background-radius: 16;"));
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

        idProdukTerpilih.clear();
        if (semuaProdukDipilih) {
            for (Produk p : semuaProduk) idProdukTerpilih.add(p.getIdProduk());
        }

        tampilkanListProduk(fieldCariProduk.getText().isEmpty() ? semuaProduk :
                semuaProduk.stream().filter(p -> p.getNama().toLowerCase().contains(fieldCariProduk.getText().toLowerCase())).toList());
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
        if (idProdukTerpilih.isEmpty() && !semuaProdukDipilih) {
            tampilkanError("Pilih minimal satu produk!");
            return;
        }

        formatTerpilih = comboFormat.getValue().contains("Excel") ? "xlsx" : "pdf";
        tampilkanPanel(panelLoading);

        progressBar.progressProperty().unbind();
        progressBar.setProgress(0);

        final List<Integer> idList = new ArrayList<>(idProdukTerpilih);
        final LocalDate tglMulai = fieldTglMulai.getValue();
        final LocalDate tglSelesai = fieldTglSelesai.getValue();

        Task<Path> task = new Task<>() {
            @Override
            protected Path call() throws Exception {
                updateProgress(2, 10);
                LaporanProduksi laporan = reportController.susunLaporan(idList, tglMulai, tglSelesai);
                updateProgress(6, 10);
                return fileGeneratorService.generate(laporan, formatTerpilih);
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());
        task.setOnSucceeded(e -> {
            tempFilePath = task.getValue();
            tampilkanHasil();
        });
        task.setOnFailed(e -> {
            tampilkanPanel(panelForm);
            task.getException().printStackTrace();
            tampilkanError("Gagal ekspor: " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    @FXML
    public void klikUnduhLaporan() {
        if (tempFilePath == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Simpan Laporan");

        // 1. Tentukan nama file default beserta ekstensinya
        String namaDefault = "laporan_produksi_" + LocalDate.now();
        String ekstensi = "xlsx".equals(formatTerpilih) ? ".xlsx" : ".pdf";
        chooser.setInitialFileName(namaDefault + ekstensi);

        // 2. Tambahkan filter agar user tidak salah simpan format
        if ("pdf".equals(formatTerpilih)) {
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Dokumen PDF (*.pdf)", "*.pdf"));
        } else {
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("File Excel (*.xlsx)", "*.xlsx"));
        }

        File tujuan = chooser.showSaveDialog(panelHasil.getScene().getWindow());

        if (tujuan != null) {
            try {
                // 3. Cek lagi, jika user menghapus ekstensi saat mengetik nama file, tambahkan secara manual
                String path = tujuan.getAbsolutePath();
                if (!path.toLowerCase().endsWith(ekstensi)) {
                    tujuan = new File(path + ekstensi);
                }

                Files.copy(tempFilePath, tujuan.toPath(), StandardCopyOption.REPLACE_EXISTING);
                tampilkanInfo("Laporan berhasil disimpan di:\n" + tujuan.getName());

            } catch (IOException ex) {
                tampilkanError("Gagal menyimpan file: " + ex.getMessage());
            }
        }
    }

    @FXML
    public void klikBuatLaporanLain() {
        idProdukTerpilih.clear();
        semuaProdukDipilih = false;
        btnSemuaProduk.setStyle(styleBtn(false));
        fieldTglMulai.setValue(null);
        fieldTglSelesai.setValue(null);
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
        labelFormatHasil.setText(formatTerpilih.toUpperCase());
        tampilkanPanel(panelHasil);
    }

    private void tampilkanError(String pesan) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(pesan);
        alert.showAndWait();
    }

    private void tampilkanInfo(String pesan) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(pesan);
        alert.showAndWait();
    }

    // =========================================================
    // STYLE HELPER (Disesuaikan ke Radius 16)
    // =========================================================

    private String styleBtn(boolean aktif) {
        return aktif ? "-fx-background-color: #00e5a0; -fx-text-fill: #0a1f1f; -fx-background-radius: 16; -fx-padding: 4 16; -fx-cursor: hand;"
                : "-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: #5a8a8a; -fx-border-radius: 16; -fx-background-radius: 16; -fx-padding: 4 16; -fx-cursor: hand;";
    }

    private String styleShortcutAktif() {
        return "-fx-background-color: #00e5a0; -fx-text-fill: #0a1f1f; -fx-background-radius: 16; -fx-padding: 6 14; -fx-cursor: hand;";
    }

    private String styleShortcutNormal() {
        return "-fx-background-color: #1e3a3a; -fx-text-fill: white; -fx-background-radius: 16; -fx-padding: 6 14; -fx-cursor: hand;";
    }
}