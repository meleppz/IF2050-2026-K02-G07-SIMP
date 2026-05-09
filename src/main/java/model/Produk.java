package model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import util.DBConnection;

public class Produk {
    private int idProduk;
    private String nama;
    private String kode;
    private Kategori kategori;
    private String satuan;
    private String deskripsi;
    private String foto;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private TargetProduksi targetProduksi;
    private ProduksiHarian produksiHarian;
    private DBConnection database;

    // Constructor
    public Produk(String nama, String kode, String satuan, String deskripsi, String foto) {
        this.nama = nama;
        this.kode = kode;
        this.satuan = satuan;
        this.deskripsi = deskripsi;
        this.foto = foto;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    public int getIdProduk() { return idProduk; }
    public String getNama() { return nama; }
    public String getKode() { return kode; }
    public Kategori getKategori() { return kategori; }
    public String getSatuan() { return satuan; }
    public String getDeskripsi() { return deskripsi; }
    public String getFoto() { return foto; }
    public TargetProduksi getTargetProduksi() { return targetProduksi; }
    public ProduksiHarian getProduksiHarian() { return produksiHarian; }

    // Method
    public static Produk create(ProdukData data) {
        return new Produk(data.getNama(), data.getKode(), data.getSatuan(), data.getDeskripsi(), data.getFoto());
    }


    public void update(ProdukData dataBaru) {
        this.nama = dataBaru.getNama();
        this.kode = dataBaru.getKode();
        this.satuan = dataBaru.getSatuan();
        this.deskripsi = dataBaru.getDeskripsi();
        this.foto = dataBaru.getFoto();
        this.updatedAt = LocalDateTime.now();

        String sql = "UPDATE produk SET nama=?, kode=?, satuan=?, deskripsi=?, foto=?, updated_at=? WHERE id_produk=?";
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, this.nama);
            stmt.setString(2, this.kode);
            stmt.setString(3, this.satuan);
            stmt.setString(4, this.deskripsi);
            stmt.setString(5, this.foto);
            stmt.setObject(6, this.updatedAt);
            stmt.setInt(7, this.idProduk);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error update produk: " + e.getMessage());
        }
    }

    public void delete() {
        String sql = "DELETE FROM produk WHERE id_produk = ?";
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, this.idProduk);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error delete produk: " + e.getMessage());
        }
    }

    public void save() {
        String sql = "INSERT INTO produk (nama, kode, id_kategori, satuan, deskripsi, foto, created_at, updated_at) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, this.nama);
            stmt.setString(2, this.kode);
            stmt.setInt(3, this.kategori.getIdKategori());
            stmt.setString(4, this.satuan);
            stmt.setString(5, this.deskripsi);
            stmt.setString(6, this.foto);
            stmt.setObject(7, this.createdAt);
            stmt.setObject(8, this.updatedAt);
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                this.idProduk = rs.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println("Error save produk: " + e.getMessage());
        }
    }

    public void save(ProduksiHarian produksiHarian) {
        String sql = "INSERT INTO produksi_harian (id_produk, nik_operator, tanggal_produksi, jumlah_aktual, jumlah_defect, kendala) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, this.idProduk);
            stmt.setString(2, produksiHarian.getNikOperator());
            stmt.setObject(3, produksiHarian.getTanggalProduksi());
            stmt.setInt(4, produksiHarian.getJumlahAktual());
            stmt.setInt(5, produksiHarian.getJumlahDefect());
            stmt.setString(6, produksiHarian.getKendala());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error save produksi harian: " + e.getMessage());
        }
    }

    public boolean update(ProduksiHarian produksiHarian) {
        String sql = "UPDATE produksi_harian SET tanggal_produksi=?, jumlah_aktual=?, jumlah_defect=?, kendala=? " +
                "WHERE id_produksi=?";

        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setObject(1, produksiHarian.getTanggalProduksi());
            stmt.setInt(2, produksiHarian.getJumlahAktual());
            stmt.setInt(3, produksiHarian.getJumlahDefect());
            stmt.setString(4, produksiHarian.getKendala());
            stmt.setInt(5, produksiHarian.getIdProduksi());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error update produksi harian: " + e.getMessage());
            return false;
        }
    }

    public boolean delete(int idProduksi) {
        String sql = "DELETE FROM produksi_harian WHERE id_produksi = ?";

        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, idProduksi);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error delete produksi harian: " + e.getMessage());
            return false;
        }
    }
}