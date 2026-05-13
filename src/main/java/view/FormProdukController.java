package view;

import controller.ProdukController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Kategori;
import model.Produk;
import model.ProdukData;
import model.TargetProduksi;

import java.io.File;
import java.util.List;

public class FormProdukController {

    @FXML public Label labelJudulForm;
    @FXML public StackPane containerFoto;
    @FXML public Label labelPlaceholderFoto;
    @FXML public ImageView previewFoto;
    @FXML public TextField fieldNamaProduk;
    @FXML public TextField fieldKodeProduk;
    @FXML public TextField fieldSatuan;
    @FXML public ComboBox<String> combKategori;
    @FXML public TextField fieldDeskripsi;
    @FXML public TextField fieldTargetPerBulan;

    private ProdukController produkController;
    public String pathFotoTerpilih = null;
    private Runnable onSimpan;

    public void init(ProdukController produkController, Runnable onSimpan) {
        this.produkController = produkController;
        this.onSimpan = onSimpan;
    }

    @FXML
    public void initialize() {
        // biarkan kosong
    }

    public void setupUntukTambah() {
        labelJudulForm.setText("Tambah Produk Baru");
        loadKategori();
    }

    public void setupUntukEdit(Produk produk) {
        labelJudulForm.setText("Edit Produk");
        loadKategori();

        // ✅ PENTING: Method ini dipanggil dari setOnShown() di ProdukViewController
        // Sudah berada di FX Thread, jadi tidak perlu Platform.runLater() lagi
        fieldNamaProduk.setText(produk.getNama());
        fieldKodeProduk.setText(produk.getKode() != null ? produk.getKode() : "");
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

        TargetProduksi target = produkController.getTargetAktif(
                produk.getIdProduk(), java.time.LocalDate.now()
        );
        if (target != null) {
            fieldTargetPerBulan.setText(String.valueOf(target.getJumlahTarget()));
        }
    }

    private void loadKategori() {
        combKategori.setEditable(true);
        combKategori.getItems().clear();
        List<Kategori> listKategori = Kategori.getAll();
        for (Kategori kat : listKategori) {
            combKategori.getItems().add(kat.getNamaKategori());
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
        if (onSimpan != null) {
            onSimpan.run();
        }
    }

    public ProdukData ambilInputProduk() {
        String nama = fieldNamaProduk.getText().trim();
        String kode = fieldKodeProduk.getText().trim();
        String deskripsi = fieldDeskripsi.getText().trim();
        String satuan = fieldSatuan.getText().trim();
        String foto = pathFotoTerpilih != null ? pathFotoTerpilih : "";

        if (nama.isEmpty() || kode.isEmpty() || satuan.isEmpty()) return null;

        return new ProdukData(nama, kode, "", satuan, deskripsi, foto);
    }

    public String getKategoriInput() {
        return (combKategori.getEditor().getText() != null) ? combKategori.getEditor().getText().trim() : "";
    }

    public String getTargetInput() {
        return fieldTargetPerBulan.getText().trim();
    }

    public void tutupWindow() {
        Stage stage = (Stage) fieldNamaProduk.getScene().getWindow();
        stage.close();
    }

    /**
     * ✅ FIX: Memberikan fokus ke field pertama
     * Dipanggil dari setOnShown() yang sudah memastikan stage fully rendered
     */
    public void fokusKeFieldPertama() {
        if (fieldKodeProduk != null && !fieldKodeProduk.isDisabled()) {
            fieldKodeProduk.requestFocus();
        } else if (fieldNamaProduk != null && !fieldNamaProduk.isDisabled()) {
            fieldNamaProduk.requestFocus();
        }
    }
}