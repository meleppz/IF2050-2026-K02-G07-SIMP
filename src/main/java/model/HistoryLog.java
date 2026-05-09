package model;

import util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.sql.ResultSet;
import java.sql.Statement;

public class HistoryLog {
    private int idHistori;
    private Produk produk;
    private String aksi;
    private LocalDateTime timestamp;
    private String keterangan;

    public HistoryLog(int idHistori, Produk produk, String aksi, LocalDateTime timestamp, String keterangan) {
        this.idHistori = idHistori;
        this.produk = produk;
        this.aksi = aksi;
        this.timestamp = timestamp;
        this.keterangan = keterangan;
    }

    public int getIdHistori() { return idHistori; }
    public Produk getProduk() { return produk; }
    public String getAksi() { return aksi; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getKeterangan() { return keterangan; }

    public static HistoryLog catatAksi(String aksi, Produk produk) {
        String sql = "INSERT INTO history_log (id_produk, aksi, timestamp, keterangan) " +
                "VALUES (?, ?, ?, ?)";
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, produk.getIdProduk());
            stmt.setString(2, aksi);
            stmt.setObject(3, LocalDateTime.now());
            stmt.setString(4, aksi + " produk " + produk.getNama());
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return new HistoryLog(
                        rs.getInt(1),
                        produk,
                        aksi,
                        LocalDateTime.now(),
                        aksi + " produk " + produk.getNama()
                );
            }
        } catch (SQLException e) {
            System.out.println("Error catat aksi: " + e.getMessage());
        }
        return null;
    }
}