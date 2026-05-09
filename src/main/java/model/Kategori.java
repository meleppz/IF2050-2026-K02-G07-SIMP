package model;

import util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Kategori {
    private int idKategori;
    private String namaKategori;

    public Kategori(int idKategori, String namaKategori) {
        this.idKategori = idKategori;
        this.namaKategori = namaKategori;
    }

    public int getIdKategori() { return idKategori; }
    public String getNamaKategori() { return namaKategori; }

    public static Kategori create(String nama, String deskripsi) {
        String sql = "INSERT INTO kategori (nama_kategori) VALUES (?) RETURNING id_kategori";
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, nama);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Kategori(rs.getInt("id_kategori"), nama);
            }
        } catch (SQLException e) {
            System.out.println("Error create kategori: " + e.getMessage());
        }
        return null;
    }

    public void delete(String nama) {
        String sql = "DELETE FROM kategori WHERE nama_kategori = ?";
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, nama);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error delete kategori: " + e.getMessage());
        }
    }
}