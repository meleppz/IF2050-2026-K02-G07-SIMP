package model;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import util.DBConnection;

public class Produk {
    // Fields
    private int           idProduk;
    private String        nama;
    private String        kode;
    private Kategori      kategori;
    private String        satuan;
    private String        deskripsi;
    private String        foto;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public Produk() {}

    public Produk(String nama, String kode, String satuan, String deskripsi, String foto) {
        this.nama = nama;
        this.kode = kode;
        this.satuan = satuan;
        this.deskripsi = deskripsi;
        this.foto = foto;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 1. getAll() : untuk mengambil seluruh data produk dari database
    public static List<Produk> getAll() {
        List<Produk> list = new ArrayList<>();
        String sql = "SELECT * FROM produk ORDER BY id_produk ASC";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {

            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("Error saat getAll Produk: " + e.getMessage());
        }
        return list;
    }

    // 2. getById() : untuk mengambil satu data produk berdasarkan ID
    public static Produk getById(int id) {
        String sql = "SELECT * FROM produk WHERE id_produk = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error saat getById Produk: " + e.getMessage());
        }
        return null;
    }

    // 3. create() : untuk membuat instance Produk baru dari data ProdukData
    public static Produk create(ProdukData data) {
        return new Produk(data.getNama(), data.getKode(), data.getSatuan(), data.getDeskripsi(), data.getFoto());
    }

    // 4. save() : untuk menyimpan data produk baru ke database
    public void save() {
        String sql = "INSERT INTO produk (nama, kode, id_kategori, satuan, deskripsi, foto, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, this.nama);
            stmt.setString(2, this.kode);
            stmt.setInt(3, (this.kategori != null) ? this.kategori.getIdKategori() : 0);
            stmt.setString(4, this.satuan);
            stmt.setString(5, this.deskripsi);
            stmt.setString(6, this.foto);
            stmt.setObject(7, this.createdAt);
            stmt.setObject(8, this.updatedAt);

            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) this.idProduk = rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error save produk: " + e.getMessage());
        }
    }

    // 5. update() : untuk memperbarui data produk yang sudah ada di database
    public void update(ProdukData dataBaru) {
        this.nama = dataBaru.getNama();
        this.kode = dataBaru.getKode();
        this.satuan = dataBaru.getSatuan();
        this.deskripsi = dataBaru.getDeskripsi();
        this.foto = dataBaru.getFoto();
        this.updatedAt = LocalDateTime.now();

        // Tambahkan id_kategori=? di query SQL
        String sql = "UPDATE produk SET nama=?, kode=?, satuan=?, deskripsi=?, foto=?, updated_at=?, id_kategori=? WHERE id_produk=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, this.nama);
            stmt.setString(2, this.kode);
            stmt.setString(3, this.satuan);
            stmt.setString(4, this.deskripsi);
            stmt.setString(5, this.foto);
            stmt.setObject(6, this.updatedAt);
            // Masukkan ID Kategori (ambil dari objek kategori yang sudah nempel di Produk)
            stmt.setInt(7, (this.kategori != null) ? this.kategori.getIdKategori() : 0);
            stmt.setInt(8, this.idProduk);

            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error update produk: " + e.getMessage());
        }
    }

    // 6. delete() : untuk menghapus data produk dari database
    public void delete() {
        String sql = "DELETE FROM produk WHERE id_produk = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, this.idProduk);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error delete produk: " + e.getMessage());
        }
    }

    // 7. save(ProduksiHarian) : untuk menyimpan rekaman produksi harian melalui objek produk
    public void save(ProduksiHarian ph) {
        String sql = "INSERT INTO produksi_harian (id_produk, nik_operator, tanggal_produksi, jumlah_aktual, jumlah_defect, kendala) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, this.idProduk);
            stmt.setString(2, ph.getNikOperator());
            stmt.setObject(3, ph.getTanggalProduksi());
            stmt.setInt(4, ph.getJumlahAktual());
            stmt.setInt(5, ph.getJumlahDefect());
            stmt.setString(6, ph.getKendala());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error save produksi harian: " + e.getMessage());
        }
    }

    // 8. update(ProduksiHarian) : untuk memperbarui rekaman produksi harian melalui objek produk
    public boolean update(ProduksiHarian ph) {
        String sql = "UPDATE produksi_harian SET tanggal_produksi=?, jumlah_aktual=?, jumlah_defect=?, kendala=? WHERE id_produksi=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, ph.getTanggalProduksi());
            stmt.setInt(2, ph.getJumlahAktual());
            stmt.setInt(3, ph.getJumlahDefect());
            stmt.setString(4, ph.getKendala());
            stmt.setInt(5, ph.getIdProduksi());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error update produksi harian: " + e.getMessage());
            return false;
        }
    }

    // 9. delete(int idProduksi) : untuk menghapus rekaman produksi harian berdasarkan ID
    public boolean delete(int idProduksi) {
        String sql = "DELETE FROM produksi_harian WHERE id_produksi = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idProduksi);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error delete produksi harian: " + e.getMessage());
            return false;
        }
    }

    // Helper: untuk memetakan baris ResultSet ke objek Produk
    private static Produk mapRow(ResultSet rs) throws SQLException {
        Produk p = new Produk();
        p.idProduk  = rs.getInt("id_produk");
        p.nama      = rs.getString("nama");
        p.kode      = rs.getString("kode");
        p.satuan    = rs.getString("satuan");
        p.deskripsi = rs.getString("deskripsi");
        p.foto      = rs.getString("foto");
        p.createdAt = rs.getTimestamp("created_at").toLocalDateTime();
        p.updatedAt = rs.getTimestamp("updated_at").toLocalDateTime();

        int idKat = rs.getInt("id_kategori");
        if (idKat > 0) p.kategori = Kategori.getById(idKat);

        return p;
    }

    // Getters & Setters
    public int getIdProduk()      { return idProduk; }
    public String getNama()       { return nama; }
    public String getKode()       { return kode; }
    public Kategori getKategori() { return kategori; }
    public void setKategori(Kategori k) { this.kategori = k; }
    public String getSatuan()     { return satuan; }
    public String getDeskripsi()  { return deskripsi; }
    public String getFoto()       { return foto; }
}