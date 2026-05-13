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
import model.TargetProduksi;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
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

    // — data & controller —
    private ProdukController produkController;
    private List<Produk> daftarProduk;
    private Integer idProdukSedangDiedit = null;
    private Dialog<ButtonType> dialogDetail = null;

    @FXML
    public void initialize() {
        produkController = new ProdukController();
        tampilkanListProduk(produkController.getAllProduk());
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
        card.setPrefWidth(380);

        HBox baris = new HBox(12);
        baris.setStyle("-fx-alignment: CENTER_LEFT;");

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

        TargetProduksi target = produkController.getTargetAktif(produk.getIdProduk(), LocalDate.now());
        String stokText = target != null ? "Stok: " + target.getRealisasi() + " " + produk.getSatuan() : "Stok: -";
        Label stok = new Label(stokText);
        stok.setStyle("-fx-text-fill: #5a8a8a; -fx-font-size: 12;");

        info.getChildren().addAll(nama, kategori, stok);
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
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/DetailProdukDialog.fxml")
            );
            // pakai controller terpisah
            DetailProdukController detailCtrl = new DetailProdukController();
            detailCtrl.init(produkController, idProduk,
                    // callback edit
                    () -> {
                        if (dialogDetail != null) dialogDetail.close();
                        idProdukSedangDiedit = idProduk;
                        // ✅ FIX: Gunakan Timeline + Platform.runLater() untuk defer tampilkanFormProduk()
                        // Timeline memberikan delay untuk cleanup Detail Dialog
                        // Platform.runLater() memastikan tampilkanFormProduk() tidak dipanggil dari dalam animation
                        Timeline delayTimeline = new Timeline(
                                new KeyFrame(Duration.millis(100), event -> {
                                    Platform.runLater(() -> tampilkanFormProduk());
                                })
                        );
                        delayTimeline.play();
                    },
                    // callback hapus
                    () -> {
                        if (!konfirmasiAksi("Hapus produk ini?")) return;
                        produkController.hapusProduk(idProduk);
                        if (dialogDetail != null) dialogDetail.close();
                        tampilkanListProduk(produkController.getAllProduk());
                    }
            );
            loader.setController(detailCtrl);
            VBox content = loader.load();
            detailCtrl.isiData(produk);

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
        // Tetap pakai konfirmasi asli kamu
        if (!konfirmasiAksi("Simpan perubahan?")) return;

        String kategoriInput = formCtrl.getKategoriInput();
        if (kategoriInput.isEmpty()) {
            tampilkanPesan("Kategori wajib diisi!");
            return;
        }

        var data = formCtrl.ambilInputProduk();
        if (data == null) {
            tampilkanPesan("Nama, kode, dan satuan wajib diisi!");
            return;
        }

        try {
            if (idProdukSedangDiedit == null) {
                Produk hasil = produkController.tambahProduk(data, kategoriInput);
                if (hasil != null && hasil.getIdProduk() != 0) {
                    String targetStr = formCtrl.getTargetInput();
                    if (!targetStr.isEmpty()) {
                        int jumlahTarget = Integer.parseInt(targetStr);
                        // Sesuaikan parameter constructor TargetProduksi kamu di sini
                        model.TargetProduksi target = new model.TargetProduksi(
                                hasil.getIdProduk(), "000000", jumlahTarget, 30
                        );
                        produkController.tambahTarget(target);
                    }
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

        // Menutup window form dengan cara yang lebih aman
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
}