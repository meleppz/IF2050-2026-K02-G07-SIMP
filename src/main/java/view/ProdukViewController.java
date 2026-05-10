package view;

import controller.ProdukController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import model.Produk;
import model.ProdukData;
import model.TargetProduksi;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class ProdukViewController {

    // — komponen dari ProdukView.fxml —
    @FXML private VBox emptyState;
    @FXML private ScrollPane scrollProduk;
    @FXML private FlowPane gridProduk;
    @FXML private TextField fieldCariProduk;

    // — komponen dari FormProdukDialog.fxml —
    @FXML private Label labelJudulForm;
    @FXML private StackPane containerFoto;
    @FXML private Label labelPlaceholderFoto;
    @FXML private ImageView previewFoto;
    @FXML private TextField fieldNamaProduk;
    @FXML private TextField fieldSatuan;
    @FXML private ComboBox<String> combKategori;
    @FXML private TextField fieldDeskripsi;
    @FXML private TextField fieldTargetPerBulan;

    // — komponen dari DetailProdukDialog.fxml —
    @FXML private ImageView detailFoto;
    @FXML private Label labelPlaceholderDetail;
    @FXML private Label detailNama;
    @FXML private Label detailKode;
    @FXML private Label detailDeskripsi;
    @FXML private Label detailTarget;
    @FXML private Label detailPerforma;

    // — data & controller —
    private ProdukController produkController;
    private List<Produk> daftarProduk;
    private Integer idProdukSedangDiedit = null;
    private String pathFotoTerpilih = null;
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

        // foto kecil di card
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

        // hitung stok dari target aktif
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
            loader.setController(this);
            VBox content = loader.load();

            // isi data
            detailNama.setText(produk.getNama());
            detailKode.setText(produk.getKode() != null ? produk.getKode() : "-");
            detailDeskripsi.setText(produk.getDeskripsi() != null ? produk.getDeskripsi() : "-");

            // target aktif
            TargetProduksi target = produkController.getTargetAktif(idProduk, LocalDate.now());
            if (target != null) {
                detailTarget.setText(target.getJumlahTarget() + " " + produk.getSatuan() + " per Bulan");
                detailPerforma.setText(String.format("%.0f%%", target.getPersentasePencapaian()));
            } else {
                detailTarget.setText("Belum ada target");
                detailPerforma.setText("-");
            }

            // foto
            if (produk.getFoto() != null && !produk.getFoto().isEmpty()) {
                detailFoto.setImage(new Image("file:" + produk.getFoto()));
                detailFoto.setVisible(true);
                labelPlaceholderDetail.setVisible(false);
            } else {
                detailFoto.setVisible(false);
                labelPlaceholderDetail.setVisible(true);
            }

            dialogDetail = new Dialog<>();
            dialogDetail.setTitle("Detail Produk");
            dialogDetail.getDialogPane().setContent(content);
            dialogDetail.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
            dialogDetail.getDialogPane().lookupButton(ButtonType.CANCEL).setVisible(false);

            // simpan id untuk tombol edit/hapus
            idProdukSedangDiedit = idProduk;

            dialogDetail.showAndWait();

        } catch (IOException e) {
            tampilkanPesan("Gagal membuka detail: " + e.getMessage());
        }
    }

    @FXML
    public void klikEditDariDetail() {
        if (dialogDetail != null) dialogDetail.close();
        tampilkanFormProduk("Edit Produk");
    }

    @FXML
    public void klikHapusDariDetail() {
        if (!konfirmasiAksi("Hapus produk ini?")) return;
        produkController.hapusProduk(idProdukSedangDiedit);
        if (dialogDetail != null) dialogDetail.close();
        tampilkanListProduk(produkController.getAllProduk());
    }

    // =========================================================
    // PRODUK — FORM TAMBAH & EDIT
    // =========================================================

    @FXML
    public void klikTambahProduk() {
        idProdukSedangDiedit = null;
        pathFotoTerpilih = null;
        tampilkanFormProduk("Tambah Produk Baru");
    }

    private void tampilkanFormProduk(String judul) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/FormProdukDialog.fxml")
            );
            loader.setController(this);
            VBox formContent = loader.load();

            labelJudulForm.setText(judul);
            combKategori.getItems().clear();
            combKategori.getItems().addAll("Minuman", "Makanan", "Lainnya");

            // kalau mode edit, isi form dengan data existing
            if (idProdukSedangDiedit != null) {
                Produk produk = produkController.getProdukById(idProdukSedangDiedit);
                if (produk != null) {
                    fieldNamaProduk.setText(produk.getNama());
                    fieldSatuan.setText(produk.getSatuan());
                    fieldDeskripsi.setText(produk.getDeskripsi() != null ? produk.getDeskripsi() : "");
                    if (produk.getKategori() != null) {
                        combKategori.setValue(produk.getKategori().getNamaKategori());
                    }
                    if (produk.getFoto() != null && !produk.getFoto().isEmpty()) {
                        pathFotoTerpilih = produk.getFoto();
                        previewFoto.setImage(new Image("file:" + produk.getFoto()));
                        previewFoto.setVisible(true);
                        labelPlaceholderFoto.setVisible(false);
                    }
                }
            }

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle(judul);
            dialog.getDialogPane().setContent(formContent);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
            dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setVisible(false);

            dialog.showAndWait();

        } catch (IOException e) {
            tampilkanPesan("Gagal membuka form: " + e.getMessage());
        }
    }

    @FXML
    public void klikUnggahGambar() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Pilih Gambar Produk");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("File Gambar", "*.png", "*.jpg", "*.jpeg")
        );

        File file = fileChooser.showOpenDialog(containerFoto.getScene().getWindow());
        if (file != null) {
            pathFotoTerpilih = file.getAbsolutePath();
            previewFoto.setImage(new Image(file.toURI().toString()));
            previewFoto.setVisible(true);
            labelPlaceholderFoto.setVisible(false);
        }
    }

    @FXML
    public void simpanProduk() {
        if (!konfirmasiAksi("Simpan perubahan?")) return;

        ProdukData data = ambilInputProduk();
        if (data == null) return;

        if (idProdukSedangDiedit == null) {
            produkController.tambahProduk(data, 1);
        } else {
            produkController.editProduk(idProdukSedangDiedit, data, null);
        }

        fieldNamaProduk.getScene().getWindow().hide();
        tampilkanListProduk(produkController.getAllProduk());
    }

    public ProdukData ambilInputProduk() {
        String nama = fieldNamaProduk.getText().trim();
        String kategori = combKategori.getValue();
        String deskripsi = fieldDeskripsi.getText().trim();
        String satuan = fieldSatuan.getText().trim();
        String foto = pathFotoTerpilih != null ? pathFotoTerpilih : "";

        if (nama.isEmpty() || kategori == null || satuan.isEmpty()) {
            tampilkanPesan("Nama, kategori, dan satuan wajib diisi!");
            return null;
        }

        return new ProdukData(nama, "", kategori, satuan, deskripsi, foto);
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

    public void klikEditProduk(int idProduk) {
        idProdukSedangDiedit = idProduk;
        tampilkanFormProduk("Edit Produk");
    }

    public void klikHapusProduk(int idProduk) {
        if (!konfirmasiAksi("Hapus produk ini?")) return;
        produkController.hapusProduk(idProduk);
        tampilkanListProduk(produkController.getAllProduk());
    }
}