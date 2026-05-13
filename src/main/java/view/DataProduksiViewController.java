package view;

import model.Kategori;
import controller.ProdukController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import model.Produk;
import model.ProduksiHarian;
import model.TargetProduksi;
import service.StatistikService;
import util.Session;
import javafx.scene.control.Alert;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DataProduksiViewController {

    // — komponen dari DataProduksiView.fxml —
    @FXML private VBox emptyState;
    @FXML private VBox containerTabel;
    @FXML private VBox isiTabel;
    @FXML private ScrollPane scrollTabel;
    @FXML private TextField fieldCariProduksi;
    @FXML private ComboBox<String> comboFilterKategori;
    @FXML private ComboBox<String> comboSort;

    // — komponen dari FormProduksiDialog.fxml —
    @FXML private VBox panelCariProduk;
    @FXML private TextField fieldCariProduk;
    @FXML private VBox listProdukContainer;
    @FXML private HBox panelProdukTerpilih;
    @FXML private ImageView fotoDetailProduk;
    @FXML private Label placeholderFotoDetail;
    @FXML private Label detailNamaProduk;
    @FXML private Label detailKodeProduk;
    @FXML private Label detailDeskripsiProduk;
    @FXML private Label detailTargetProduk;
    @FXML private Label detailPerformaProduk;
    @FXML private DatePicker fieldTanggalProduksi;
    @FXML private TextField fieldJumlahProduksi;
    @FXML private TextField fieldJumlahDefect;
    @FXML private TextField fieldCatatan;
    @FXML private Label lblNamaPengguna;

    // — data & controller —
    private ProdukController produkController;
    private List<ProduksiHarian> daftarProduksi;
    private List<Produk> daftarProduk;
    private Produk produkTerpilih = null;
    private Integer idProduksiSedangDiedit = null;
    
    // ✅ Instance StatistikService untuk menghitung performa
    private StatistikService statistikService = new StatistikService();
    
    // ✅ Session untuk permission check
    private Session session = Session.getInstance();

    @FXML
    public void initialize() {
        produkController = new ProdukController();
        setupHeaderUser();

        // Inisialisasi daftar awal
        this.daftarProduksi = produkController.getAllProduksiHarian();

        // Setup Combo Kategori
        comboFilterKategori.getItems().add("Semua Kategori");
        Kategori.getAll().forEach(k -> comboFilterKategori.getItems().add(k.getNamaKategori()));
        comboFilterKategori.getSelectionModel().selectFirst();

        // Setup Combo Sort
        comboSort.getItems().addAll("Terbaru", "Terlama", "Abjad A-Z", "Abjad Z-A");
        comboSort.getSelectionModel().selectFirst();

        tampilkanListProduksi(this.daftarProduksi);
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
    // PRODUKSI — LIST & TABEL
    // =========================================================

    private void tampilkanListProduksi(List<ProduksiHarian> listProduksi) {
        this.daftarProduksi = listProduksi;
        isiTabel.getChildren().clear();

        if (listProduksi == null || listProduksi.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            containerTabel.setVisible(false);
            containerTabel.setManaged(false);
            return;
        }

        emptyState.setVisible(false);
        emptyState.setManaged(false);
        containerTabel.setVisible(true);
        containerTabel.setManaged(true);

        for (ProduksiHarian produksi : listProduksi) {
            isiTabel.getChildren().add(buatBarisTabel(produksi));
        }
    }

    private HBox buatBarisTabel(ProduksiHarian produksi) {
        HBox baris = new HBox();
        baris.setStyle("-fx-background-color: #0d2626; -fx-padding: 12 16; -fx-border-color: #132929; -fx-border-width: 0 0 1 0;");
        baris.setAlignment(javafx.geometry.Pos.CENTER);  // ← Center alignment untuk baris

        Produk produk = produkController.getProdukById(produksi.getIdProduk());
        String namaProduk = produk != null ? produk.getNama() : "-";
        String kategori = produk != null && produk.getKategori() != null
                ? produk.getKategori().getNamaKategori() : "-";

        Label lblId = buatLabel(String.valueOf(produksi.getIdProduksi()), 80);
        Label lblNama = buatLabel(namaProduk, 200);
        Label lblTotal = buatLabel(String.valueOf(produksi.getJumlahAktual()), 150);
        Label lblDefect = buatLabel(String.valueOf(produksi.getJumlahDefect()), 100);

        Label lblKategori = new Label(kategori);
        lblKategori.setPrefWidth(150);
        lblKategori.setAlignment(javafx.geometry.Pos.CENTER);  // ← Center alignment untuk kategori
        lblKategori.setStyle("-fx-text-fill: #00e5a0; -fx-background-color: #1e3a3a; -fx-background-radius: 20; -fx-padding: 2 10;");

        Label lblTanggal = buatLabel(produksi.getTanggalProduksi().toString(), 200);

        HBox aksi = new HBox(8);
        aksi.setPrefWidth(100);
        aksi.setAlignment(javafx.geometry.Pos.CENTER);  // ← Center alignment untuk aksi buttons

        Button btnEdit = new Button();
        btnEdit.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-min-width: 0; -fx-min-height: 0; -fx-pref-width: 16; -fx-pref-height: 16;");
        // ✅ Permission check: Disable untuk SUPERVISOR
        if (session.isSupervisor()) {
            btnEdit.setDisable(true);
            btnEdit.setOpacity(0.3);
        } else {
            btnEdit.setOnAction(e -> klikEditProduksi(produksi.getIdProduksi()));
        }
        // Add edit icon
        ImageView editIcon = new ImageView(new Image(getClass().getResourceAsStream("/images/icon_edit.png")));
        editIcon.setFitWidth(24);
        editIcon.setFitHeight(24);
        editIcon.setPreserveRatio(true);
        btnEdit.setGraphic(editIcon);

        Button btnHapus = new Button();
        btnHapus.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-min-width: 0; -fx-min-height: 0; -fx-pref-width: 16; -fx-pref-height: 16;");
        // ✅ Permission check: Disable untuk SUPERVISOR
        if (session.isSupervisor()) {
            btnHapus.setDisable(true);
            btnHapus.setOpacity(0.3);
        } else {
            btnHapus.setOnAction(e -> klikHapusProduksi(produksi.getIdProduksi()));
        }
        // Add delete icon
        ImageView deleteIcon = new ImageView(new Image(getClass().getResourceAsStream("/images/icon_delete.png")));
        deleteIcon.setFitWidth(24);
        deleteIcon.setFitHeight(24);
        deleteIcon.setPreserveRatio(true);
        btnHapus.setGraphic(deleteIcon);

        aksi.getChildren().addAll(btnEdit, btnHapus);
        baris.getChildren().addAll(lblId, lblNama, lblTotal, lblDefect, lblKategori, lblTanggal, aksi);
        return baris;
    }

    private Label buatLabel(String teks, double lebar) {
        Label label = new Label(teks);
        label.setPrefWidth(lebar);
        label.setAlignment(javafx.geometry.Pos.CENTER);  // ← Center alignment untuk semua label
        label.setStyle("-fx-text-fill: white; -fx-font-size: 13;");
        return label;
    }

    // =========================================================
    // PRODUKSI — FORM
    // =========================================================

    @FXML
    public void klikTambahProduksi() {
        // ✅ Permission check: Hanya OPERATOR yang bisa menambah produksi
        if (!session.isOperator()) {
            tampilkanPesan("Hanya Operator yang bisa menambah data produksi. Anda adalah: " + session.getPenggunaAktif().getPeran());
            return;
        }
        
        idProduksiSedangDiedit = null;
        produkTerpilih = null;
        tampilkanFormProduksi();
    }

    private void tampilkanFormProduksi() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/FormProduksiDialog.fxml")
            );
            loader.setController(this);
            VBox formContent = loader.load();

            // load semua produk untuk list awal
            daftarProduk = produkController.getAllProduk();
            tampilkanListProdukDiForm(daftarProduk);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Tambah Data Produksi Baru");
            dialog.getDialogPane().setContent(formContent);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
            dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setVisible(false);

            dialog.showAndWait();

        } catch (IOException e) {
            tampilkanPesan("Gagal membuka form: " + e.getMessage());
        }
    }

    private void tampilkanListProdukDiForm(List<Produk> listProduk) {
        listProdukContainer.getChildren().clear();
        for (Produk produk : listProduk) {
            HBox item = buatItemProduk(produk);
            listProdukContainer.getChildren().add(item);
        }
    }

    private HBox buatItemProduk(Produk produk) {
        HBox item = new HBox(12);
        item.setStyle("-fx-padding: 10 14; -fx-cursor: hand; -fx-background-color: transparent;");
        item.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // foto kecil
        StackPane fotoContainer = new StackPane();
        fotoContainer.setPrefSize(50, 50);
        fotoContainer.setStyle("-fx-background-color: #1e3a3a; -fx-background-radius: 6;");

        if (produk.getFoto() != null && !produk.getFoto().isEmpty()) {
            ImageView foto = new ImageView(new Image("file:" + produk.getFoto()));
            foto.setFitWidth(50);
            foto.setFitHeight(50);
            foto.setPreserveRatio(true);
            fotoContainer.getChildren().add(foto);
        } else {
            Label ph = new Label("🖼");
            ph.setStyle("-fx-font-size: 18; -fx-text-fill: #5a8a8a;");
            fotoContainer.getChildren().add(ph);
        }

        VBox info = new VBox(2);
        Label nama = new Label(produk.getNama());
        nama.setStyle("-fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold;");
        Label kode = new Label(produk.getKode() != null ? produk.getKode() : "-");
        kode.setStyle("-fx-text-fill: #5a8a8a; -fx-font-size: 12;");
        info.getChildren().addAll(nama, kode);

        item.getChildren().addAll(fotoContainer, info);

        // hover effect
        item.setOnMouseEntered(e -> item.setStyle("-fx-padding: 10 14; -fx-cursor: hand; -fx-background-color: #1e3a3a;"));
        item.setOnMouseExited(e -> item.setStyle("-fx-padding: 10 14; -fx-cursor: hand; -fx-background-color: transparent;"));

        // klik → pilih produk ini
        item.setOnMouseClicked(e -> pilihProdukDiForm(produk));

        return item;
    }

    @FXML
    public void filterProduk() {
        String keyword = fieldCariProduk.getText().toLowerCase();
        if (keyword.isEmpty()) {
            tampilkanListProdukDiForm(daftarProduk);
            return;
        }
        List<Produk> hasil = daftarProduk.stream()
                .filter(p -> p.getNama().toLowerCase().contains(keyword)
                        || (p.getKode() != null && p.getKode().toLowerCase().contains(keyword)))
                .toList();
        tampilkanListProdukDiForm(hasil);
    }

    private void pilihProdukDiForm(Produk produk) {
        this.produkTerpilih = produk;

        // sembunyikan panel cari, tampilkan panel detail
        panelCariProduk.setVisible(false);
        panelCariProduk.setManaged(false);
        panelProdukTerpilih.setVisible(true);
        panelProdukTerpilih.setManaged(true);

        // isi detail produk
        detailNamaProduk.setText(produk.getNama());
        detailKodeProduk.setText(produk.getKode() != null ? produk.getKode() : "-");
        detailDeskripsiProduk.setText(produk.getDeskripsi() != null ? produk.getDeskripsi() : "-");

        TargetProduksi target = produkController.getTargetAktif(produk.getIdProduk(), LocalDate.now());
        if (target != null) {
            detailTargetProduk.setText(target.getJumlahTarget() + " " + produk.getSatuan() + " per Bulan");
            
            // ✅ Hitung performa menggunakan StatistikService (30 hari terakhir)
            Map<String, Object> performaData = statistikService.getPerforma30Hari(produk.getIdProduk(), LocalDate.now());
            double rataRataPerforma = (Double) performaData.get("rataRataPerforma");
            detailPerformaProduk.setText(String.format("%.0f%%", rataRataPerforma));
        } else {
            detailTargetProduk.setText("Belum ada target");
            detailPerformaProduk.setText("-");
        }

        if (produk.getFoto() != null && !produk.getFoto().isEmpty()) {
            fotoDetailProduk.setImage(new Image("file:" + produk.getFoto()));
            fotoDetailProduk.setVisible(true);
            placeholderFotoDetail.setVisible(false);
        } else {
            fotoDetailProduk.setVisible(false);
            placeholderFotoDetail.setVisible(true);
        }
    }

    @FXML
    public void simpanProduksi() {
        if (produkTerpilih == null) {
            tampilkanPesan("Pilih produk terlebih dahulu!");
            return;
        }

        String jumlahStr = fieldJumlahProduksi.getText().trim();
        String defectStr = fieldJumlahDefect.getText().trim();
        LocalDate tanggal = fieldTanggalProduksi.getValue();

        if (tanggal == null || jumlahStr.isEmpty() || defectStr.isEmpty()) {
            tampilkanPesan("Tanggal, jumlah produksi, dan jumlah defect wajib diisi!");
            return;
        }

        try {
            int jumlah = Integer.parseInt(jumlahStr);
            int defect = Integer.parseInt(defectStr);
            String catatan = fieldCatatan.getText().trim();

            // ——— VALIDASI ———

            // 1. Cek nilai minimal
            if (jumlah < 0 || defect < 0) {
                tampilkanPesan("Jumlah produksi dan defect tidak boleh kurang dari 0!");
                return;
            }

            // 2. Cek batas maksimal defect
            if (defect > jumlah) {
                tampilkanPesan("Jumlah defect tidak boleh melebihi jumlah total produksi!");
                return;
            }

            // ✅ 3. Cek tanggal (Tidak boleh lebih dari hari ini)
            if (tanggal.isAfter(LocalDate.now())) {
                tampilkanPesan("Tanggal produksi tidak boleh melebihi tanggal hari ini!");
                return;
            }

            // ——— SELESAI VALIDASI ———

            // ✅ Dapatkan NIK user dari Session
            String nikUser = session.getNikAktif();

            if (idProduksiSedangDiedit == null) {
                // mode tambah
                ProduksiHarian produksi = new ProduksiHarian(
                        nikUser,
                        tanggal,
                        jumlah,
                        defect,
                        catatan.isEmpty() ? null : catatan,
                        produkTerpilih.getIdProduk()
                );
                boolean berhasil = produkController.tambahProduksiHarian(produksi);
                if (berhasil) {
                    System.out.println("✓ Produksi tersimpan oleh " + nikUser);
                } else {
                    tampilkanPesan("Gagal menyimpan data produksi!");
                    return;
                }
            } else {
                // mode edit
                ProduksiHarian produksi = produkController.getProduksiHarianById(idProduksiSedangDiedit);
                if (produksi == null) return;
                produksi.setTanggalProduksi(tanggal);
                produksi.setJumlahAktual(jumlah);
                produksi.setJumlahDefect(defect);
                produksi.setKendala(catatan.isEmpty() ? null : catatan);
                boolean berhasil = produkController.editProduksiHarian(produksi);
                if (berhasil) {
                    System.out.println("✓ Produksi diperbarui oleh " + nikUser);
                } else {
                    tampilkanPesan("Gagal memperbarui data produksi!");
                    return;
                }
            }

            fieldJumlahProduksi.getScene().getWindow().hide();
            tampilkanListProduksi(produkController.getAllProduksiHarian());

        } catch (NumberFormatException e) {
            tampilkanPesan("Jumlah produksi dan defect harus berupa angka!");
        }
    }

    // =========================================================
    // EDIT & HAPUS
    // =========================================================

    public void klikEditProduksi(int idProduksi) {
        idProduksiSedangDiedit = idProduksi;
        produkTerpilih = null;

        ProduksiHarian produksi = produkController.getProduksiHarianById(idProduksi);
        if (produksi == null) return;

        produkTerpilih = produkController.getProdukById(produksi.getIdProduk());

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/FormProduksiDialog.fxml")
            );
            loader.setController(this);
            VBox formContent = loader.load();

            // langsung tampilkan detail produk, sembunyikan panel cari
            daftarProduk = produkController.getAllProduk();
            pilihProdukDiForm(produkTerpilih);

            // isi field dengan data existing
            fieldTanggalProduksi.setValue(produksi.getTanggalProduksi());
            fieldJumlahProduksi.setText(String.valueOf(produksi.getJumlahAktual()));
            fieldJumlahDefect.setText(String.valueOf(produksi.getJumlahDefect()));
            fieldCatatan.setText(produksi.getKendala() != null ? produksi.getKendala() : "");

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Edit Data Produksi");
            dialog.getDialogPane().setContent(formContent);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
            dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setVisible(false);

            dialog.showAndWait();

        } catch (IOException e) {
            tampilkanPesan("Gagal membuka form: " + e.getMessage());
        }
    }

    public void klikHapusProduksi(int idProduksi) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText("Hapus data produksi ini?");
        alert.setContentText("Tindakan kamu tidak bisa dipulihkan");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            produkController.hapusProduksiHarian(idProduksi);
            tampilkanListProduksi(produkController.getAllProduksiHarian());
        }
    }

    // =========================================================
    // HELPER
    // =========================================================

    @FXML
    public void updateFilterDanSort() {
        String keyword = fieldCariProduksi.getText().toLowerCase();
        String kategori = comboFilterKategori.getValue();
        String sortMode = comboSort.getValue();

        List<ProduksiHarian> hasil = produkController.getAllProduksiHarian().stream()
                .filter(ph -> {
                    Produk p = produkController.getProdukById(ph.getIdProduk());
                    if (p == null) return false;

                    // Filter Keyword
                    boolean matchKeyword = p.getNama().toLowerCase().contains(keyword) ||
                            String.valueOf(ph.getIdProduksi()).contains(keyword);

                    // Filter Kategori
                    boolean matchKategori = kategori.equals("Semua Kategori") ||
                            (p.getKategori() != null && p.getKategori().getNamaKategori().equals(kategori));

                    return matchKeyword && matchKategori;
                })
                .sorted((p1, p2) -> {
                    // Logika Sorting
                    Produk prod1 = produkController.getProdukById(p1.getIdProduk());
                    Produk prod2 = produkController.getProdukById(p2.getIdProduk());

                    switch (sortMode) {
                        case "Terlama": return p1.getTanggalProduksi().compareTo(p2.getTanggalProduksi());
                        case "Abjad A-Z": return prod1.getNama().compareToIgnoreCase(prod2.getNama());
                        case "Abjad Z-A": return prod2.getNama().compareToIgnoreCase(prod1.getNama());
                        default: return p2.getTanggalProduksi().compareTo(p1.getTanggalProduksi()); // Terbaru
                    }
                })
                .toList();

        tampilkanListProduksi(hasil);
    }

    private void tampilkanPesan(String pesan) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(pesan);
        alert.showAndWait();
    }
}