package view;

import javafx.scene.control.ProgressIndicator;
import util.Session;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import controller.ProdukController;
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
    @FXML private VBox panelUhOh; // Panel baru untuk data kosong

    @FXML private TextField fieldCariProduk;
    @FXML private HBox listProdukHorizontal;
    @FXML private Button btnSemuaProduk;

    @FXML private DatePicker fieldTglMulai;
    @FXML private DatePicker fieldTglSelesai;
    @FXML private ComboBox<String> comboFormat;
    @FXML private ComboBox<String> comboFilterKategori;
    @FXML private Label lblNamaPengguna;
    @FXML private Button btn7Hari;
    @FXML private Button btn30Hari;
    @FXML private Button btn6Bulan;
    @FXML private Button btnSemuaData;

    @FXML private ProgressIndicator progressBar;
    @FXML private Label labelFormatHasil;

    private final ReportController reportController = new ReportController();
    private final FileGeneratorService fileGeneratorService = new FileGeneratorService();
    private Session session = Session.getInstance();
    private List<Produk> semuaProduk = new ArrayList<>();
    private final List<Integer> idProdukTerpilih = new ArrayList<>();
    private boolean semuaProdukDipilih = false;
    private Button shortcutAktif = null;
    private Path tempFilePath = null;
    private String formatTerpilih = "pdf";
    private final ProdukController produkController = new ProdukController();

    private final ChangeListener<LocalDate> dateChangeListener = (obs, lama, baru) -> {
        if (baru != null) nonaktifkanShortcut();
    };

    @FXML
    public void initialize() {
        setupHeaderUser();
        tampilkanPanel(panelForm);
        setupComboBoxFormat();

        semuaProduk = produkController.getAllProduk();

        if (comboFilterKategori != null) {
            ObservableList<String> listKategori = FXCollections.observableArrayList("Semua Kategori");
            semuaProduk.stream()
                    .map(p -> p.getKategori() != null ? p.getKategori().getNamaKategori() : "-")
                    .distinct()
                    .forEach(listKategori::add);
            comboFilterKategori.setItems(listKategori);
            comboFilterKategori.getSelectionModel().selectFirst();
            comboFilterKategori.setOnAction(e -> filterProduk(fieldCariProduk.getText()));
        }

        tampilkanListProduk(semuaProduk);

        fieldCariProduk.textProperty().addListener((obs, lama, baru) -> {
            if (fieldCariProduk.isFocused()) filterProduk(baru);
        });

        fieldTglMulai.valueProperty().addListener(dateChangeListener);
        fieldTglSelesai.valueProperty().addListener(dateChangeListener);
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
        VBox item = new VBox(8);
        item.setAlignment(Pos.CENTER);
        item.setStyle("-fx-padding: 12; -fx-background-color: #1e3a3a; -fx-background-radius: 16; -fx-min-width: 140; -fx-cursor: hand;");

        CheckBox cb = new CheckBox();
        cb.setSelected(idProdukTerpilih.contains(produk.getIdProduk()));

        ImageView img = new ImageView();
        if (produk.getFoto() != null && !produk.getFoto().isEmpty()) {
            try {
                img.setImage(new Image("file:" + produk.getFoto(), true));
            } catch (Exception e) {
                System.out.println("Gagal load gambar file: " + e.getMessage());
            }
        } else {
            try {
                img.setImage(new Image(getClass().getResourceAsStream("/icons/package.png")));
            } catch (Exception ignored) {}
        }
        img.setFitWidth(50);
        img.setFitHeight(50);
        img.setPreserveRatio(true);

        Label nama = new Label(produk.getNama());
        nama.setStyle("-fx-text-fill: white; -fx-font-size: 13; -fx-font-weight: bold;");
        nama.setWrapText(true);
        nama.setMaxWidth(120);
        nama.setAlignment(Pos.CENTER);

        String kodeLabel = produk.getKode() != null ? produk.getKode() : "-";
        Label infoLabel = new Label("ID: " + produk.getIdProduk() + " | " + kodeLabel);
        infoLabel.setStyle("-fx-text-fill: #00e5a0; -fx-font-size: 10;");

        item.getChildren().addAll(cb, img, nama, infoLabel);

        cb.selectedProperty().addListener((obs, lama, baru) -> {
            if (baru) {
                if (!idProdukTerpilih.contains(produk.getIdProduk())) idProdukTerpilih.add(produk.getIdProduk());
            } else {
                idProdukTerpilih.remove((Integer) produk.getIdProduk());
                semuaProdukDipilih = false;
                btnSemuaProduk.setStyle(styleBtn(false));
            }
            if (!fieldCariProduk.isFocused()) updateTextFieldStatus();
        });

        item.setOnMouseClicked(e -> cb.setSelected(!cb.isSelected()));

        return item;
    }

    private void updateTextFieldStatus() {
        if (semuaProdukDipilih) {
            fieldCariProduk.setText("Semua Produk");
        } else if (idProdukTerpilih.isEmpty()) {
            fieldCariProduk.setText("");
        } else {
            String gabunganNama = semuaProduk.stream()
                    .filter(p -> idProdukTerpilih.contains(p.getIdProduk()))
                    .map(Produk::getNama)
                    .collect(Collectors.joining(", "));
            fieldCariProduk.setText(gabunganNama);
        }
    }

    private void filterProduk(String keyword) {
        String kw = (keyword == null) ? "" : keyword.toLowerCase();
        String kategoriTerpilih = (comboFilterKategori != null) ? comboFilterKategori.getValue() : "Semua Kategori";

        List<Produk> hasil = semuaProduk.stream()
                .filter(p -> {
                    boolean cocokKeyword = p.getNama().toLowerCase().contains(kw) ||
                            String.valueOf(p.getIdProduk()).contains(kw) ||
                            (p.getKode() != null && p.getKode().toLowerCase().contains(kw));
                    String katProduk = p.getKategori() != null ? p.getKategori().getNamaKategori() : "-";
                    boolean cocokKategori = kategoriTerpilih.equals("Semua Kategori") || katProduk.equals(kategoriTerpilih);
                    return cocokKeyword && cocokKategori;
                })
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

        Task<LaporanProduksi> task = new Task<>() {
            @Override protected LaporanProduksi call() throws Exception {
                updateProgress(2, 10);
                LaporanProduksi laporan = reportController.susunLaporan(idList, tglMulai, tglSelesai);
                updateProgress(6, 10);
                return laporan;
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());

        task.setOnSucceeded(e -> {
            LaporanProduksi laporan = task.getValue();

            // Cek apakah data produksi tersedia
            if (laporan.getBarisTabel().isEmpty()) {
                tampilkanPanel(panelUhOh);
                return;
            }

            // Ada data — lanjut generate file
            Task<Path> taskGenerate = new Task<>() {
                @Override protected Path call() throws Exception {
                    return fileGeneratorService.generate(laporan, formatTerpilih);
                }
            };
            taskGenerate.setOnSucceeded(ev -> { tempFilePath = taskGenerate.getValue(); tampilkanHasil(); });
            taskGenerate.setOnFailed(ev -> { tampilkanPanel(panelForm); tampilkanError("Gagal ekspor: " + taskGenerate.getException().getMessage()); });
            new Thread(taskGenerate).start();
        });

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

    // Dipanggil dari tombol "Kembali" di panelUhOh — reuse method yang sama
    @FXML
    public void klikKembaliDariUhOh() {
        tampilkanPanel(panelForm);
    }

    private void tampilkanPanel(VBox panel) {
        panelForm.setVisible(false); panelForm.setManaged(false);
        panelLoading.setVisible(false); panelLoading.setManaged(false);
        panelHasil.setVisible(false); panelHasil.setManaged(false);
        panelUhOh.setVisible(false); panelUhOh.setManaged(false);
        panel.setVisible(true); panel.setManaged(true);
    }
    private void setupHeaderUser() {
        if (session.isLoggedIn()) {
            String peran = session.getPenggunaAktif().getPeran().toString();
            lblNamaPengguna.setText(peran);
        } else {
            lblNamaPengguna.setText("Guest");
        }
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