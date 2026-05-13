package view;

import controller.AuthController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import model.Pengguna;

public class LoginViewController {

    @FXML private ImageView logoImage;
    @FXML private TextField fieldNik;
    @FXML private PasswordField fieldPassword;
    @FXML private TextField fieldPasswordVisible;
    @FXML private Label linkSignUp;

    private AuthController authController;
    private boolean passwordVisible = false;

    @FXML
    public void initialize() {
        authController = new AuthController();

        try {
            Image logo = new Image(getClass().getResourceAsStream("/images/logo.png"));
            logoImage.setImage(logo);
        } catch (Exception e) {
            System.out.println("Logo tidak ditemukan: " + e.getMessage());
        }
    }

    @FXML
    public void klikMasuk() {
        String nik = fieldNik.getText().trim();
        String password = passwordVisible
                ? fieldPasswordVisible.getText().trim()
                : fieldPassword.getText().trim();

        if (nik.isEmpty() || password.isEmpty()) {
            tampilkanPesan("NIK dan password wajib diisi!");
            return;
        }

        Pengguna pengguna = authController.login(nik, password);
        if (pengguna != null) {
            System.out.println("✓ Login berhasil: " + pengguna.getNama());
            bukaMainView();
        } else {
            tampilkanPesan("NIK atau password salah, atau akun tidak aktif.");
        }
    }

    @FXML
    public void togglePassword() {
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            fieldPasswordVisible.setText(fieldPassword.getText());
            fieldPasswordVisible.setVisible(true);
            fieldPasswordVisible.setManaged(true);
            fieldPassword.setVisible(false);
            fieldPassword.setManaged(false);
        } else {
            fieldPassword.setText(fieldPasswordVisible.getText());
            fieldPassword.setVisible(true);
            fieldPassword.setManaged(true);
            fieldPasswordVisible.setVisible(false);
            fieldPasswordVisible.setManaged(false);
        }
    }

    @FXML
    public void klikSignUp() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/SignUpView.fxml")
            );
            Scene scene = new Scene(loader.load(), 1280, 900);
            Stage stage = (Stage) fieldNik.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            tampilkanPesan("Gagal membuka halaman sign up: " + e.getMessage());
        }
    }

    private void bukaMainView() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/MainView.fxml")
            );
            Scene scene = new Scene(loader.load(), 1280, 900);
            Stage stage = (Stage) fieldNik.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            tampilkanPesan("Gagal membuka aplikasi: " + e.getMessage());
        }
    }

    private void tampilkanPesan(String pesan) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(pesan);
        alert.showAndWait();
    }
}