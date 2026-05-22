package view; // Sesuaikan dengan package kamu

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.text.Font;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/view/LoginView.fxml")
        );

        // FIX 1: Hapus angka 1280, 900 agar scene mengikuti ukuran root element
        Scene scene = new Scene(loader.load());

        stage.setTitle("SIMP - Sistem Informasi Manajemen Produksi");
        stage.setScene(scene);

        // Load Fonts
        Font.loadFont(getClass().getResourceAsStream("/fonts/PlusJakartaSans-Regular.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/PlusJakartaSans-Bold.ttf"), 14);

        // Cek DB (Biarkan tetap ada)
        try {
            java.sql.Connection conn = util.DBConnection.getConnection();
            System.out.println("=== DB INFO ===");
            System.out.println("Connected: " + !conn.isClosed());
        } catch (Exception e) {
            System.out.println("DB Error: " + e.getMessage());
        }

        // FIX 2: Tambahkan ini agar saat LOGIN atau MAIN terbuka, dia langsung fit ke layar
        stage.setMaximized(true);

        // FIX 3: Opsional, set minimum size agar tidak bisa dikecilkan sampai rusak
        stage.setMinWidth(1024);
        stage.setMinHeight(700);

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}