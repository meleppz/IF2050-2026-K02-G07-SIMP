package view;

import controller.ProdukController;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;
import model.Produk;
import service.StatistikService;
import util.Session;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.stage.Window;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.Modality;

public class ProdukViewController {

    // — komponen dari ProdukView.fxml —
    @FXML private VBox emptyState;
    @FXML private ScrollPane scrollProduk;
    @FXML private FlowPane gridProduk;
    @FXML private TextField fieldCariProduk;
    @FXML private Label lblNamaPengguna;

    // — data & controller —
    private ProdukController produkController;
    private List<Produk> daftarProduk;
    private Integer idProdukSedangDiedit = null;
    private Dialog<ButtonType> dialogDetail = null;
    
    // ✅ Instance StatistikService untuk menghitung performa
    private StatistikService statistikService = new StatistikService();
    
    // ✅ Session untuk permission check
    private Session session = Session.getInstance();

    @FXML
    public void initialize() {
        produkController = new ProdukController();

        setupHeaderUser();

        tampilkanListProduk(produkController.getAllProduk());
    }

    private void setupHeaderUser() {
        if (session.isLoggedIn()) {
            // Mengambil peran dari objek Pengguna (misal: "OPERATOR" atau "SUPERVISOR")
            String peran = session.getPenggunaAktif().getPeran().toString();
            lblNamaPengguna.setText(peran);

            // Opsional: Logging ke konsol untuk mempermudah debugging
            System.out.println("[LOG] Login as: " + peran);
            System.out.println("[LOG] Is Operator: " + session.isOperator());
        } else {
            lblNamaPengguna.setText("Guest");
        }
    }

    // =========================================================
    // PRODUK — LIST & CARD
    // =========================================================

    private void tampilkanListProduk(List<Produk> listProduk) {
        this.daftarProduk = listProduk;
        gridProduk.getChildren().clear();

        if (listProduk == null || listProduk.isEmpty()) {
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

        for (Produk produk : listProduk) {
            gridProduk.getChildren().add(buatCardProduk(produk));
        }
    }

    private VBox buatCardProduk(Produk produk) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: #132929; -fx-background-radius: 12; -fx-padding: 12; -fx-cursor: hand;");
        card.setPrefWidth(493);

        HBox baris = new HBox(12);
        baris.setStyle("-fx-alignment: CENTER_LEFT;");
        baris.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        StackPane fotoContainer = new StackPane();
        fotoContainer.setPrefSize(80, 80);
        fotoContainer.setStyle("-fx-background-color: #1e3a3a; -fx-background-radius: 8;");

        if (produk.getFoto() != null && !produk.getFoto().isEmpty()) {
            ImageView foto = new ImageView(new Image("file:" + produk.getFoto()));
            foto.setFitWidth(80);
            foto.setFitHeight(80);
            foto.setPreserveRatio(true);
            fotoContainer.getChildren().add(foto);
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

    // =========================================================
    // PRODUK — DETAIL
    // =========================================================

    public void pilihProduk(int idProduk) {
        Produk produk = produkController.getProdukById(idProduk);
        if (produk == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/DetailProdukDialog.fxml"));
            DetailProdukController detailCtrl = new DetailProdukController();

            detailCtrl.init(produkController, idProduk,
                    // callback edit
                    () -> {
                        // Proteksi tambahan jika tombol tidak di-disable
                        if (session.isSupervisor()) return;

                        if (dialogDetail != null) dialogDetail.close();
                        idProdukSedangDiedit = idProduk;
                        Timeline delayTimeline = new Timeline(
                                new KeyFrame(Duration.millis(100), event -> {
                                    Platform.runLater(() -> tampilkanFormProduk());
                                })
                        );
                        delayTimeline.play();
                    },
                    // callback hapus
                    () -> {
                        // Proteksi tambahan
                        if (session.isSupervisor()) return;

                        if (!konfirmasiAksi("Hapus produk ini?")) return;
                        produkController.hapusProduk(idProduk);
                        if (dialogDetail != null) dialogDetail.close();
                        tampilkanListProduk(produkController.getAllProduk());
                    }
            );

            loader.setController(detailCtrl);
            VBox content = loader.load();
            detailCtrl.isiData(produk);

            // ✅ LOGIKA DISABLE BUTTON LANGSUNG
            // Pastikan di DetailProdukController kamu punya getter atau akses ke button-nya
            if (session.isSupervisor()) {
                if (detailCtrl.getBtnEditDetail() != null) {
                    detailCtrl.getBtnEditDetail().setDisable(true);
                    detailCtrl.getBtnEditDetail().setOpacity(0.4); // Agar terlihat redup seperti di DataProduksi
                }
                if (detailCtrl.getBtnHapusDetail() != null) {
                    detailCtrl.getBtnHapusDetail().setDisable(true);
                    detailCtrl.getBtnHapusDetail().setOpacity(0.4);
                }
            }

            dialogDetail = new Dialog<>();
            dialogDetail.setTitle("Detail Produk");
            dialogDetail.getDialogPane().setContent(content);
            dialogDetail.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
            dialogDetail.getDialogPane().lookupButton(ButtonType.CANCEL).setVisible(false);

            idProdukSedangDiedit = idProduk;
            dialogDetail.initOwner(gridProduk.getScene().getWindow()); // ← tambahkan ini
            dialogDetail.showAndWait();

        } catch (IOException e) {
            tampilkanPesan("Gagal membuka detail: " + e.getMessage());
        }
    }

    // =========================================================
    // PRODUK — FORM TAMBAH & EDIT
    // =========================================================

    @FXML
    public void klikTambahProduk() {
        // ✅ Permission check: Hanya OPERATOR yang bisa menambah produk
        if (!session.isOperator()) {
            tampilkanPesan("Hanya Operator yang bisa menambah produk. Anda adalah: " + session.getPenggunaAktif().getPeran());
            return;
        }
        
        idProdukSedangDiedit = null;
        tampilkanFormProduk();
    }

    private void tampilkanFormProduk() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/FormProdukDialog.fxml")
            );
            FormProdukController formCtrl = new FormProdukController();
            formCtrl.init(produkController, () -> simpanDariForm(formCtrl));
            loader.setController(formCtrl);
            VBox formContent = loader.load();

            // ✅ Setup data LANGSUNG sebelum Stage ditampilkan
            if (idProdukSedangDiedit == null) {
                formCtrl.setupUntukTambah();
            } else {
                Produk produk = produkController.getProdukById(idProdukSedangDiedit);
                if (produk != null) formCtrl.setupUntukEdit(produk);
            }

            // ✅ Root VBox: explicit disable input consumption
            formContent.setFocusTraversable(false);

            Stage formStage = new Stage();
            formStage.setTitle(idProdukSedangDiedit == null ? "Tambah Produk Baru" : "Edit Produk");
            formStage.initModality(Modality.APPLICATION_MODAL);
            formStage.initOwner(gridProduk.getScene().getWindow());
            javafx.scene.Scene scene = new javafx.scene.Scene(formContent);
            formStage.setScene(scene);
            
            formStage.setOnShown(e -> {
                // ✅ Explicit layout flush untuk memastikan scene sudah 100% rendered
                formContent.layout();
                
                // ✅ Gunakan Timeline untuk delay fokus yang reliable dan ensure event queue clear
                Timeline focusTimeline = new Timeline(
                        new KeyFrame(Duration.millis(300), event -> {
                            formCtrl.fokusKeFieldPertama();
                        })
                );
                focusTimeline.play();
            });
            
            formStage.showAndWait();

        } catch (IOException e) {
            tampilkanPesan("Gagal membuka form: " + e.getMessage());
        }
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
        tampilkanListProduk(produkController.getAllProduk());
    }

    // =========================================================
    // HELPER
    // =========================================================

    @FXML
    public void cariProduk() {
        String keyword = fieldCariProduk.getText().toLowerCase();
        if (keyword.isEmpty()) {
            tampilkanListProduk(daftarProduk);
            return;
        }
        List<Produk> hasil = daftarProduk.stream()
                .filter(p -> p.getNama().toLowerCase().contains(keyword))
                .toList();
        tampilkanListProduk(hasil);
    }

    private void tampilkanPesan(String pesan) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(pesan);
        alert.showAndWait();
    }

    public boolean konfirmasiAksi(String aksi) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText(aksi);
        alert.setContentText("Tindakan kamu tidak bisa dipulihkan");
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private String formatKategori(String input) {
        if (input == null || input.trim().isEmpty()) return "";
        String trimed = input.trim().toLowerCase();
        // Mengubah huruf pertama jadi kapital, sisanya kecil
        return trimed.substring(0, 1).toUpperCase() + trimed.substring(1);
    }
}