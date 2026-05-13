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
    private String nikPengguna;

    public HistoryLog(int idHistori, Produk produk, String aksi, LocalDateTime timestamp, String keterangan, String nikPengguna) {
        this.idHistori = idHistori;
        this.produk = produk;
        this.aksi = aksi;
        this.timestamp = timestamp;
        this.keterangan = keterangan;
        this.nikPengguna = nikPengguna;
    }

    public int getIdHistori()          { return idHistori; }
    public Produk getProduk()          { return produk; }
    public String getAksi()            { return aksi; }
    public LocalDateTime getTimestamp(){ return timestamp; }
    public String getKeterangan()      { return keterangan; }
    public String getNikPengguna()     { return nikPengguna; }

    // ✅ Catat aksi dengan NIK pengguna
    public static HistoryLog catatAksi(String aksi, Produk produk, String nikPengguna) {
        String sql = "INSERT INTO history_log (id_produk, nik_pengguna, aksi, timestamp, keterangan) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, produk.getIdProduk());
            stmt.setString(2, nikPengguna);
            stmt.setString(3, aksi);
            stmt.setObject(4, LocalDateTime.now());
            stmt.setString(5, aksi + " produk " + produk.getNama() + " oleh " + nikPengguna);
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return new HistoryLog(
                            rs.getInt(1),
                            produk,
                            aksi,
                            LocalDateTime.now(),
                            aksi + " produk " + produk.getNama() + " oleh " + nikPengguna,
                            nikPengguna
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error catat aksi: " + e.getMessage());
        }
        return null;
    }

    // ✅ Backward compatibility: jika tidak ada NIK, gunakan null
    public static HistoryLog catatAksi(String aksi, Produk produk) {
        return catatAksi(aksi, produk, null);
    }
}