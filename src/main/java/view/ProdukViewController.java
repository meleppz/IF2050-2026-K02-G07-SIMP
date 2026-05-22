package view;

import javafx.scene.Scene;
import controller.ProdukController;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.Produk;
import service.StatistikService;
import util.Session;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ProdukViewController {

    // — komponen dari ProdukView.fxml —
    @FXML private VBox emptyState;
    @FXML private ScrollPane scrollProduk;
    @FXML private FlowPane gridProduk;
    @FXML private TextField fieldCariProduk;
    @FXML private Label lblNamaPengguna;

    // — Tambahkan komponen filter baru (Pastikan fx:id di FXML sesuai) —
    @FXML private ComboBox<String> comboFilterKategori;
    @FXML private ComboBox<String> comboSortProduk;

    // — data & controller —
    private ProdukController produkController;
    private Integer idProdukSedangDiedit = null;
    private Dialog<ButtonType> dialogDetail = null;

    // ✅ Instance StatistikService untuk menghitung performa
    private StatistikService statistikService = new StatistikService();

    // ✅ Session untuk permission check
    private Session session = Session.getInstance();

    // Menggunakan ObservableList agar filter & sort bisa real-time
    private ObservableList<Produk> masterDataProduk = FXCollections.observableArrayList();
    private FilteredList<Produk> filteredProduk;
    private SortedList<Produk> sortedProduk;

    @FXML
    public void initialize() {
        produkController = new ProdukController();
        setupHeaderUser();

        // 1. Inisialisasi Data List
        masterDataProduk.setAll(produkController.getAllProduk());
        filteredProduk = new FilteredList<>(masterDataProduk, p -> true);
        sortedProduk = new SortedList<>(filteredProduk);

        // 2. Setup Dropdown Filter Kategori (Ambil unik dari data yang ada)
        setupFilterKategori();
        if (comboFilterKategori != null) {
            comboFilterKategori.setOnAction(e -> jalankanFilterDanSort());
        }

        // 3. Setup Dropdown Sorting
        if (comboSortProduk != null) {
            comboSortProduk.setItems(FXCollections.observableArrayList(
                    "Nama: A - Z", "Nama: Z - A", "Terbaru", "Terlama"
            ));
            comboSortProduk.getSelectionModel().selectFirst();
            comboSortProduk.setOnAction(e -> jalankanFilterDanSort());
        }

        // 4. Listener Pencarian (Real-time)
        fieldCariProduk.textProperty().addListener((obs, oldVal, newVal) -> jalankanFilterDanSort());

        // 5. Tampilkan data awal
        renderGrid();
    }

    private void setupFilterKategori() {
        if (comboFilterKategori == null) return;

        ObservableList<String> listKategori = FXCollections.observableArrayList("Semua Kategori");
        // Ambil nama kategori unik dari master data produk
        masterDataProduk.stream()
                .map(p -> p.getKategori() != null ? p.getKategori().getNamaKategori() : "-")
                .distinct()
                .forEach(listKategori::add);

        comboFilterKategori.setItems(listKategori);
        // Don't change selection or set action listener here to avoid triggering filter/sort
    }

    // FUNGSI INTI: Menggabungkan Search + Filter Kategori + Sorting
    private void jalankanFilterDanSort() {
        String keyword = fieldCariProduk.getText().toLowerCase();
        String kategoriSelected = comboFilterKategori != null && comboFilterKategori.getValue() != null ? comboFilterKategori.getValue() : "Semua Kategori";

        // A. Proses Filtering
        filteredProduk.setPredicate(produk -> {
            // Filter Search
            boolean cocokKeyword = produk.getNama().toLowerCase().contains(keyword) ||
                    (produk.getKode() != null && produk.getKode().toLowerCase().contains(keyword));

            // Filter Kategori
            String katProduk = produk.getKategori() != null ? produk.getKategori().getNamaKategori() : "-";
            boolean cocokKategori = kategoriSelected.equals("Semua Kategori") || katProduk.equals(kategoriSelected);

            return cocokKeyword && cocokKategori;
        });

        // B. Proses Sorting
        if (comboSortProduk != null) {
            String sortOption = comboSortProduk.getValue();
            if (sortOption != null) {
                switch (sortOption) {
                    case "Nama: A - Z" -> sortedProduk.setComparator((p1, p2) -> p1.getNama().compareToIgnoreCase(p2.getNama()));
                    case "Nama: Z - A" -> sortedProduk.setComparator((p1, p2) -> p2.getNama().compareToIgnoreCase(p1.getNama()));
                    case "Terbaru" -> sortedProduk.setComparator((p1, p2) -> Integer.compare(p2.getIdProduk(), p1.getIdProduk()));
                    case "Terlama" -> sortedProduk.setComparator((p1, p2) -> Integer.compare(p1.getIdProduk(), p2.getIdProduk()));
                }
            }
        }

        renderGrid();
    }

    private void renderGrid() {
        gridProduk.getChildren().clear();

        if (sortedProduk.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            scrollProduk.setVisible(false);
            scrollProduk.setManaged(false);
            return;
        }

        emptyState.setVisible(false);
        emptyState.setManaged(false);
        scrollProduk.setVisible(true);
        scrollProduk.setManaged(true);

        for (Produk produk : sortedProduk) {
            gridProduk.getChildren().add(buatCardProduk(produk));
        }
    }

    // Re-aktifkan fungsi pencarian tombol jika user menekan Enter
    @FXML
    public void cariProduk() {
        jalankanFilterDanSort();
    }

    // Helper untuk merefresh data setelah Simpan/Hapus
    private void refreshDataSetelahAksi() {
        masterDataProduk.setAll(produkController.getAllProduk());
        updateFilterKategori(); // Update kategori tanpa trigger action listener
        jalankanFilterDanSort();
    }

    // --- Sisa Method (buatCardProduk, pilihProduk, dll) tetap sama tapi panggil refreshDataSetelahAksi() ---

    private void setupHeaderUser() {
        if (session.isLoggedIn()) {
            lblNamaPengguna.setText(session.getPenggunaAktif().getPeran().toString());
        } else {
            lblNamaPengguna.setText("Guest");
        }
    }

    private VBox buatCardProduk(Produk produk) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: #132929; -fx-background-radius: 12; -fx-padding: 12; -fx-cursor: hand;");
        card.setPrefWidth(493);

        HBox baris = new HBox(12);
        baris.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        StackPane fotoContainer = new StackPane();
        fotoContainer.setPrefSize(80, 80);
        fotoContainer.setStyle("-fx-background-color: #1e3a3a; -fx-background-radius: 8;");

        if (produk.getFoto() != null && !produk.getFoto().isEmpty()) {
            try {
                ImageView foto = new ImageView(new Image("file:" + produk.getFoto()));
                foto.setFitWidth(80); foto.setFitHeight(80);
                foto.setPreserveRatio(true);
                fotoContainer.getChildren().add(foto);
            } catch (Exception e) {
                fotoContainer.getChildren().add(new Label("❌"));
            }
        } else {
            Label placeholder = new Label("🖼");
            placeholder.setStyle("-fx-font-size: 24; -fx-text-fill: #5a8a8a;");
            fotoContainer.getChildren().add(placeholder);
        }

        VBox info = new VBox(4);
        Label nama = new Label(produk.getNama());
        nama.setStyle("-fx-text-fill: white; -fx-font-size: 15; -fx-font-weight: bold;");

        Label kategori = new Label(produk.getKategori() != null ? produk.getKategori().getNamaKategori() : "-");
        kategori.setStyle("-fx-background-color: #1e3a3a; -fx-text-fill: #00e5a0; -fx-font-size: 11; -fx-background-radius: 20; -fx-padding: 2 10;");

        // ✅ Hitung performa produk (30 hari terakhir)
        Map<String, Object> performaData = statistikService.getPerforma30Hari(produk.getIdProduk(), LocalDate.now());
        double rataRataPerforma = (Double) performaData.get("rataRataPerforma");
        String performaText = "Performa: " + String.format("%.0f%%", rataRataPerforma);
        Label performa = new Label(performaText);
        performa.setStyle("-fx-text-fill: #5a8a8a; -fx-font-size: 12;");

        info.getChildren().addAll(nama, kategori, performa);
        baris.getChildren().addAll(fotoContainer, info);
        card.getChildren().add(baris);
        card.setOnMouseClicked(e -> pilihProduk(produk.getIdProduk()));

        return card;
    }

    public void pilihProduk(int idProduk) {
        Produk produk = produkController.getProdukById(idProduk);
        if (produk == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/DetailProdukDialog.fxml"));
            DetailProdukController detailCtrl = new DetailProdukController();
            detailCtrl.init(produkController, idProduk,
                    () -> { // Callback Edit
                        if (session.isSupervisor()) return;
                        if (dialogDetail != null) dialogDetail.close();
                        idProdukSedangDiedit = idProduk;
                        Platform.runLater(() -> tampilkanFormProduk());
                    },
                    () -> { // Callback Hapus
                        if (session.isSupervisor()) return;
                        if (!konfirmasiAksi("Hapus produk ini?")) return;
                        produkController.hapusProduk(idProduk);
                        if (dialogDetail != null) dialogDetail.close();
                        refreshDataSetelahAksi();
                    }
            );
            loader.setController(detailCtrl);
            VBox content = loader.load();
            detailCtrl.isiData(produk);

            dialogDetail = new Dialog<>();
            dialogDetail.getDialogPane().setContent(content);
            dialogDetail.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
            dialogDetail.getDialogPane().lookupButton(ButtonType.CANCEL).setVisible(false);
            dialogDetail.initOwner(gridProduk.getScene().getWindow());
            dialogDetail.showAndWait();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    public void klikTambahProduk() {
        if (!session.isOperator()) {
            tampilkanPesan("Akses Ditolak!");
            return;
        }
        idProdukSedangDiedit = null;
        tampilkanFormProduk();
    }

    private void tampilkanFormProduk() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/FormProdukDialog.fxml"));
            FormProdukController formCtrl = new FormProdukController();
            formCtrl.init(produkController, () -> simpanDariForm(formCtrl));
            loader.setController(formCtrl);
            VBox formContent = loader.load();

            if (idProdukSedangDiedit == null) formCtrl.setupUntukTambah();
            else {
                Produk p = produkController.getProdukById(idProdukSedangDiedit);
                if (p != null) formCtrl.setupUntukEdit(p);
            }

            Stage formStage = new Stage();
            formStage.initModality(Modality.APPLICATION_MODAL);
            formStage.initOwner(gridProduk.getScene().getWindow());
            formStage.setScene(new Scene(formContent));
            formStage.showAndWait();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void simpanDariForm(FormProdukController formCtrl) {
        // 1. Konfirmasi awal
        if (!konfirmasiAksi("Simpan perubahan?")) return;

        // ✅ Validasi Kategori
        String rawKategori = formCtrl.getKategoriInput();
        if (rawKategori == null || rawKategori.trim().isEmpty()) {
            tampilkanPesan("Kategori wajib diisi!");
            return;
        }
        String kategoriInput = formatKategori(rawKategori);

        // ✅ Ambil data dasar (Nama, Kode, Satuan)
        var data = formCtrl.ambilInputProduk();
        if (data == null) {
            tampilkanPesan("Nama, kode, dan satuan wajib diisi!");
            return;
        }

        // ——— VALIDASI SATUAN (TIDAK BOLEH ADA ANGKA) ———
        // Regex ".*\\d.*" artinya: cek apakah ada digit (0-9) di sepanjang string
        if (data.getSatuan() != null && data.getSatuan().matches(".*\\d.*")) {
            tampilkanPesan("Satuan tidak boleh mengandung angka (misal: gunakan 'Lusin', bukan '12pcs')!");
            return;
        }
        // ——— SELESAI VALIDASI SATUAN ———

        // ✅ Pengecekan Target Produksi
        String targetStr = formCtrl.getTargetInput();
        int jumlahTarget = 0;

        if (idProdukSedangDiedit == null) {
            if (targetStr.isEmpty()) {
                tampilkanPesan("Target produksi wajib diisi!");
                return;
            }
            try {
                jumlahTarget = Integer.parseInt(targetStr);
                if (jumlahTarget < 1) {
                    tampilkanPesan("Target produksi harus lebih dari 0!");
                    return;
                }
            } catch (NumberFormatException e) {
                tampilkanPesan("Target produksi harus berupa angka!");
                return;
            }
        }

        // ✅ Eksekusi Simpan ke Database
        try {
            if (idProdukSedangDiedit == null) {
                Produk hasil = produkController.tambahProduk(data, kategoriInput);
                if (hasil != null && hasil.getIdProduk() != 0) {
                    produkController.tambahTarget(new model.TargetProduksi(
                            hasil.getIdProduk(), "000000", jumlahTarget, 1
                    ));
                } else {
                    tampilkanPesan("Gagal menyimpan produk!");
                    return;
                }
            } else {
                boolean berhasil = produkController.editProduk(idProdukSedangDiedit, data, kategoriInput);
                if (!berhasil) {
                    tampilkanPesan("Gagal memperbarui produk!");
                    return;
                }
            }
        } catch (Exception e) {
            tampilkanPesan("Terjadi error: " + e.getMessage());
            return;
        }

        formCtrl.fieldNamaProduk.getScene().getWindow().hide();
        refreshDataSetelahAksi();
    }

    private void tampilkanPesan(String pesan) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(pesan);
        alert.showAndWait();
    }

    public boolean konfirmasiAksi(String aksi) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText(aksi);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private String formatKategori(String input) {
        if (input == null || input.trim().isEmpty()) return "";
        String trimed = input.trim().toLowerCase();
        // Mengubah huruf pertama jadi kapital, sisanya kecil
        return trimed.substring(0, 1).toUpperCase() + trimed.substring(1);
    }

    private void updateFilterKategori() {
        if (comboFilterKategori == null) return;

        ObservableList<String> listKategori = FXCollections.observableArrayList("Semua Kategori");
        // Ambil nama kategori unik dari master data produk
        masterDataProduk.stream()
                .map(p -> p.getKategori() != null ? p.getKategori().getNamaKategori() : "-")
                .distinct()
                .forEach(listKategori::add);

        comboFilterKategori.setItems(listKategori);
        // Don't change selection or set action listener here to avoid triggering filter/sort
    }
}