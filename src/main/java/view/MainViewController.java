package view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class MainViewController {

    @FXML private StackPane contentArea;
    @FXML private ImageView logoImage;
    @FXML private Button btnDashboard;
    @FXML private Button btnProduk;
    @FXML private Button btnDataProduksi;
    @FXML private Button btnBuatLaporan;

    private static final String STYLE_AKTIF = "-fx-background-color: #1e3a3a; -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold; -fx-alignment: CENTER_LEFT; -fx-background-radius: 8; -fx-padding: 12 16;";
    private static final String STYLE_NONAKTIF = "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14; -fx-alignment: CENTER_LEFT; -fx-background-radius: 8; -fx-padding: 12 16;";

    @FXML
    public void initialize() {
        // load logo
        try {
            Image logo = new Image(getClass().getResourceAsStream("/images/logo.png"));
            logoImage.setImage(logo);
        } catch (Exception e) {
            System.out.println("Logo tidak ditemukan: " + e.getMessage());
        }

        // default screen: Produk
        navigasiProduk();
    }

    @FXML
    public void navigasiDashboard() {
        setAktif(btnDashboard);
        // TODO: load DashboardView
    }

    @FXML
    public void navigasiProduk() {
        setAktif(btnProduk);
        loadScreen("/view/ProdukView.fxml");
    }

    @FXML
    public void navigasiDataProduksi() {
        setAktif(btnDataProduksi);
        loadScreen("/view/DataProduksiView.fxml");
    }

    @FXML
    public void navigasiBuatLaporan() {
        setAktif(btnBuatLaporan);
        // TODO: load BuatLaporanView
    }

    @FXML
    public void klikKeluar() {
        System.exit(0);
    }

    private void loadScreen(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Pane screen = loader.load();
            contentArea.getChildren().setAll(screen);
        } catch (IOException e) {
            System.err.println("Gagal load screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setAktif(Button aktif) {
        btnDashboard.setStyle(STYLE_NONAKTIF);
        btnProduk.setStyle(STYLE_NONAKTIF);
        btnDataProduksi.setStyle(STYLE_NONAKTIF);
        btnBuatLaporan.setStyle(STYLE_NONAKTIF);
        aktif.setStyle(STYLE_AKTIF);
    }
}