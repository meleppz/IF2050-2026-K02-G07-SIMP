package view;

import controller.AuthController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class SignUpViewController {

    @FXML private ImageView logoImage;
    @FXML private TextField fieldNik;
    @FXML private TextField fieldNama;
    @FXML private TextField fieldUsername;
    @FXML private PasswordField fieldPassword;
    @FXML private TextField fieldPasswordVisible;
    @FXML private PasswordField fieldKonfirmasi;
    @FXML private TextField fieldKonfirmasiVisible;
    @FXML private ComboBox<String> combPeran;
    @FXML private Label linkLogin;

    private AuthController authController;
    private boolean passwordVisible = false;
    private boolean konfirmasiVisible = false;

    @FXML
    public void initialize() {
        authController = new AuthController();

        try {
            Image logo = new Image(getClass().getResourceAsStream("/images/logo.png"));
            logoImage.setImage(logo);
        } catch (Exception e) {
            System.out.println("Logo tidak ditemukan: " + e.getMessage());
        }

        combPeran.getItems().addAll("Operator", "Supervisor");
    }

    @FXML
    public void klikDaftar() {
        String nik = fieldNik.getText().trim();
        String nama = fieldNama.getText().trim();
        String username = fieldUsername.getText().trim();
        String password = passwordVisible
                ? fieldPasswordVisible.getText().trim()
                : fieldPassword.getText().trim();
        String konfirmasi = konfirmasiVisible
                ? fieldKonfirmasiVisible.getText().trim()
                : fieldKonfirmasi.getText().trim();
        String peran = combPeran.getValue();

        if (nik.isEmpty() || nama.isEmpty() || username.isEmpty()
                || password.isEmpty() || peran == null) {
            tampilkanPesan("Semua field wajib diisi!");
            return;
        }

        if (!password.equals(konfirmasi)) {
            tampilkanPesan("Password dan konfirmasi password tidak cocok!");
            return;
        }

        boolean berhasil = authController.register(nik, username, password, nama, peran);
        if (berhasil) {
            tampilkanPesan("Akun berhasil dibuat! Silakan login.");
            klikLogin();
        } else {
            tampilkanPesan("Gagal membuat akun. NIK mungkin sudah terdaftar.");
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
    public void toggleKonfirmasi() {
        konfirmasiVisible = !konfirmasiVisible;
        if (konfirmasiVisible) {
            fieldKonfirmasiVisible.setText(fieldKonfirmasi.getText());
            fieldKonfirmasiVisible.setVisible(true);
            fieldKonfirmasiVisible.setManaged(true);
            fieldKonfirmasi.setVisible(false);
            fieldKonfirmasi.setManaged(false);
        } else {
            fieldKonfirmasi.setText(fieldKonfirmasiVisible.getText());
            fieldKonfirmasi.setVisible(true);
            fieldKonfirmasi.setManaged(true);
            fieldKonfirmasiVisible.setVisible(false);
            fieldKonfirmasiVisible.setManaged(false);
        }
    }

    @FXML
    public void klikLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/LoginView.fxml")
            );
            Scene scene = new Scene(loader.load(), 1280, 900);
            Stage stage = (Stage) fieldNik.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            tampilkanPesan("Gagal membuka halaman login: " + e.getMessage());
        }
    }

    private void tampilkanPesan(String pesan) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(pesan);
        alert.showAndWait();
    }
}