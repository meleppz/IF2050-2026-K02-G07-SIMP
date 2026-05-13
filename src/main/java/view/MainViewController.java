package view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;

public class MainViewController {

    @FXML private StackPane contentArea;
    @FXML private ImageView logoImage;
    @FXML private Button btnDashboard;
    @FXML private Button btnProduk;
    @FXML private Button btnDataProduksi;
    @FXML private Button btnBuatLaporan;
    @FXML private ImageView iconDashboard;
    @FXML private ImageView iconProduk;
    @FXML private ImageView iconDataProduksi;
    @FXML private ImageView iconLaporan;
    @FXML private ImageView iconKeluar;

    private static final String STYLE_AKTIF    = "-fx-background-color: #1e3a3a; -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold; -fx-alignment: CENTER_LEFT; -fx-background-radius: 8; -fx-padding: 12 16; -fx-cursor: hand;";
    private static final String STYLE_NONAKTIF = "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14; -fx-alignment: CENTER_LEFT; -fx-background-radius: 8; -fx-padding: 12 16; -fx-cursor: hand;";

    @FXML
    public void initialize() {
        loadIcon(logoImage, "/images/logo.png");
        loadIcon(iconDashboard, "/images/icon_dashboard.png");
        loadIcon(iconProduk, "/images/icon_produk.png");
        loadIcon(iconDataProduksi, "/images/icon_data_produksi.png");
        loadIcon(iconLaporan, "/images/icon_laporan.png");
        loadIcon(iconKeluar, "/images/icon_keluar.png");

        // default screen: Produk
        navigasiBuatLaporan();
        navigasiProduk();
    }

    @FXML
    public void navigasiDashboard() {
        setAktif(btnDashboard);
        DashboardView dashboard = new DashboardView(this);
        dashboard.prefWidthProperty().bind(contentArea.widthProperty());
        dashboard.prefHeightProperty().bind(contentArea.heightProperty());
        contentArea.getChildren().setAll(dashboard);
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
        loadScreen("/view/EksporLaporanView.fxml");
    }
    @FXML
    public void klikKeluar() {
        util.Session.getInstance().logout();
        System.out.println("[LOG] Pengguna telah logout.");

        try {
            // 2. Load halaman Login
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/LoginView.fxml"));
            javafx.scene.Parent root = loader.load();

            // 3. Tampilkan stage Login baru
            Stage loginStage = new Stage();
            loginStage.setTitle("Login - SIMP");
            loginStage.setScene(new javafx.scene.Scene(root));
            loginStage.show();

            // 4. Tutup jendela utama saat ini
            Stage currentStage = (Stage) contentArea.getScene().getWindow();
            currentStage.close();

        } catch (IOException e) {
            System.err.println("Gagal memuat halaman login: " + e.getMessage());
        }
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

        loadIcon(iconDashboard, "/images/icon_dashboard.png");
        loadIcon(iconProduk, "/images/icon_produk.png");
        loadIcon(iconDataProduksi, "/images/icon_data_produksi.png");
        loadIcon(iconLaporan, "/images/icon_laporan.png");

        if (aktif == btnDashboard)      loadIcon(iconDashboard,     "/images/icon_dashboard_active.png");
        else if (aktif == btnProduk)    loadIcon(iconProduk,        "/images/icon_produk_active.png");
        else if (aktif == btnDataProduksi) loadIcon(iconDataProduksi, "/images/icon_data_produksi_active.png");
        else if (aktif == btnBuatLaporan)  loadIcon(iconLaporan,    "/images/icon_laporan_active.png");
    }

    private void loadIcon(ImageView imageView, String path) {
        try {
            var stream = getClass().getResourceAsStream(path);
            if (stream != null) imageView.setImage(new Image(stream));
        } catch (Exception e) {
            System.out.println("Icon tidak ditemukan: " + path);
        }
    }
}