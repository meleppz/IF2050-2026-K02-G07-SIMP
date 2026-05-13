package view;

import controller.ProdukController;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import model.Produk;
import model.TargetProduksi;
import service.StatistikService;
import util.Session;

import java.time.LocalDate;
import java.util.Map;

public class DetailProdukController {

    @FXML private ImageView detailFoto;
    @FXML private Label labelPlaceholderDetail;
    @FXML private Label detailNama;
    @FXML private Label detailKode;
    @FXML private Label detailDeskripsi;
    @FXML private Label detailTarget;
    @FXML private Label detailPerforma;
    @FXML private Button btnEditDetail;
    @FXML private Button btnHapusDetail;

    private ProdukController produkController;
    private int idProduk;
    private Runnable onEdit;
    private Runnable onHapus;
    
    // ✅ Instance StatistikService untuk menghitung performa
    private StatistikService statistikService = new StatistikService();

    public void init(ProdukController produkController, int idProduk,
                     Runnable onEdit, Runnable onHapus) {
        this.produkController = produkController;
        this.idProduk = idProduk;
        this.onEdit = onEdit;
        this.onHapus = onHapus;
    }

    @FXML
    public void initialize() {}

    public void isiData(Produk produk) {
        detailNama.setText(produk.getNama());
        detailKode.setText(produk.getKode() != null ? produk.getKode() : "-");
        detailDeskripsi.setText(produk.getDeskripsi() != null ? produk.getDeskripsi() : "-");

        TargetProduksi target = produkController.getTargetAktif(idProduk, LocalDate.now());
        if (target != null) {
            detailTarget.setText(target.getJumlahTarget() + " " + produk.getSatuan() + " per Bulan");
            
            // ✅ Hitung performa menggunakan StatistikService (30 hari terakhir)
            Map<String, Object> performaData = statistikService.getPerforma30Hari(idProduk, LocalDate.now());
            double rataRataPerforma = (Double) performaData.get("rataRataPerforma");
            detailPerforma.setText(String.format("%.0f%%", rataRataPerforma));
        } else {
            detailTarget.setText("Belum ada target");
            detailPerforma.setText("-");
        }

        if (produk.getFoto() != null && !produk.getFoto().isEmpty()) {
            detailFoto.setImage(new Image("file:" + produk.getFoto()));
            detailFoto.setVisible(true);
            labelPlaceholderDetail.setVisible(false);
        } else {
            detailFoto.setVisible(false);
            labelPlaceholderDetail.setVisible(true);
        }
    }

    @FXML
    public void klikEditDariDetail() {
        if (onEdit != null) onEdit.run();
    }

    @FXML
    public void klikHapusDariDetail() {
        if (onHapus != null) onHapus.run();
    }

    public Button getBtnEditDetail() { return btnEditDetail; }
    public Button getBtnHapusDetail() { return btnHapusDetail; }
}