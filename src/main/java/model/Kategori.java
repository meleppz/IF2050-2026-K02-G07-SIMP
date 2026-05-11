package model;

import util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Kategori {
    // Fields
    private int idKategori;
    private String namaKategori;

    // Constructor
    public Kategori(int idKategori, String namaKategori) {
        this.idKategori = idKategori;
        this.namaKategori = namaKategori;
    }

    // 1. getById() : untuk mengambil data kategori berdasarkan ID
    public static Kategori getById(int id) {
        String sql = "SELECT * FROM kategori WHERE id_kategori = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Kategori(
                            rs.getInt("id_kategori"),
                            rs.getString("nama_kategori")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getById kategori: " + e.getMessage());
        }
        return null;
    }

    // 2. create() : untuk membuat dan menyimpan kategori baru ke database
    public static Kategori create(String nama, String deskripsi) {
        String sql = "INSERT INTO kategori (nama_kategori) VALUES (?) RETURNING id_kategori";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nama);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Kategori(rs.getInt("id_kategori"), nama);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error create kategori: " + e.getMessage());
        }
        return null;
    }

    // 3. delete() : untuk menghapus kategori dari database berdasarkan nama
    public void delete(String nama) {
        String sql = "DELETE FROM kategori WHERE nama_kategori = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nama);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error delete kategori: " + e.getMessage());
        }
    }

    public static List<Kategori> getAll() {
        List<Kategori> list = new ArrayList<>();
        String sql = "SELECT * FROM kategori ORDER BY nama_kategori ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(new Kategori(
                        rs.getInt("id_kategori"),
                        rs.getString("nama_kategori")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getAll kategori: " + e.getMessage());
        }
        return list;
    }

    // Method untuk mencari kategori berdasarkan nama
    public static Kategori getByName(String nama) {
        String sql = "SELECT * FROM kategori WHERE nama_kategori = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nama);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Kategori(rs.getInt("id_kategori"), rs.getString("nama_kategori"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getByName kategori: " + e.getMessage());
        }
        return null;
    }

    // Method sakti: Cari kalau ada, buat kalau tidak ada
    public static Kategori getOrCreate(String nama) {
        Kategori existing = getByName(nama);
        if (existing != null) return existing;

        // Kalau tidak ada, panggil method create yang sudah kamu punya
        return create(nama, "");
    }

    // Getters
    public int getIdKategori() { return idKategori; }
    public String getNamaKategori() { return namaKategori; }
}