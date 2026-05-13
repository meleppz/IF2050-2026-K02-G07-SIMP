package model;

import util.DBConnection;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TargetProduksi {
    // Fields
    private int           idTarget;
    private int           idProduk;
    private String        nikPengguna;
    private int           jumlahTarget;
    private int           periodeTarget; // dalam hari
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public TargetProduksi() {}

    public TargetProduksi(int idProduk, String nikPengguna,
                          int jumlahTarget, int periodeTarget) {
        this.idProduk     = idProduk;
        this.nikPengguna  = nikPengguna;
        this.jumlahTarget = jumlahTarget;
        this.periodeTarget = periodeTarget;
        this.createdAt    = LocalDateTime.now();
        this.updatedAt    = LocalDateTime.now();
    }

    // 1. save()
    public boolean save() {
        String sql = """
                INSERT INTO target_produksi
                    (id_produk, nik_pengguna, jumlah_target, periode_target, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, idProduk);
            stmt.setString(2, nikPengguna);
            stmt.setInt(3, jumlahTarget);
            stmt.setInt(4, periodeTarget);
            stmt.setObject(5, createdAt);
            stmt.setObject(6, updatedAt);

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) this.idTarget = keys.getInt(1);
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error saat menyimpan TargetProduksi: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // 2. getAll()
    public static List<TargetProduksi> getAll() {
        List<TargetProduksi> list = new ArrayList<>();
        String sql = "SELECT * FROM target_produksi ORDER BY created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {

            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("Error saat getAll TargetProduksi: " + e.getMessage());
        }
        return list;
    }

    // 3. getById()
    public static TargetProduksi getById(int idTarget) {
        String sql = "SELECT * FROM target_produksi WHERE id_target = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idTarget);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error saat getById TargetProduksi: " + e.getMessage());
        }
        return null;
    }

    // 4. getByIdProduk()
    public static List<TargetProduksi> getByIdProduk(int idProduk) {
        List<TargetProduksi> list = new ArrayList<>();
        String sql = "SELECT * FROM target_produksi WHERE id_produk = ? ORDER BY created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idProduk);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error saat getByIdProduk TargetProduksi: " + e.getMessage());
        }
        return list;
    }

    // 5. update()
    public boolean update() {
        this.updatedAt = LocalDateTime.now();
        String sql = """
                UPDATE target_produksi
                SET jumlah_target  = ?,
                    periode_target = ?,
                    updated_at     = ?
                WHERE id_target = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, jumlahTarget);
            stmt.setInt(2, periodeTarget);
            stmt.setObject(3, updatedAt);
            stmt.setInt(4, idTarget);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error saat update TargetProduksi: " + e.getMessage());
        }
        return false;
    }

    // 6. delete()
    public boolean delete() {
        return deleteById(this.idTarget);
    }

    // 7. deleteById()
    public static boolean deleteById(int idTarget) {
        String sql = "DELETE FROM target_produksi WHERE id_target = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idTarget);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error saat delete TargetProduksi: " + e.getMessage());
        }
        return false;
    }

    // 8. getRealisasi() — hitung realisasi dalam periode_target hari terakhir
    public int getRealisasi() {
        String sql = """
                SELECT COALESCE(SUM(jumlah_aktual - jumlah_defect), 0) AS realisasi
                FROM produksi_harian
                WHERE id_produk = ?
                  AND tanggal_produksi >= (created_at::date - (periode_target || ' days')::interval)::date
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idProduk);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("realisasi");
            }
        } catch (SQLException e) {
            System.err.println("Error saat getRealisasi TargetProduksi: " + e.getMessage());
        }
        return 0;
    }

    // 9. getPersentasePencapaian()
    public double getPersentasePencapaian() {
        if (jumlahTarget == 0) return 0.0;
        return (getRealisasi() / (double) jumlahTarget) * 100.0;
    }

    // 10. isTercapai()
    public boolean isTercapai() {
        return getRealisasi() >= jumlahTarget;
    }

    // 11. getAktifPadaTanggal() — ambil target terbaru untuk produk ini
    public static TargetProduksi getAktifPadaTanggal(int idProduk, LocalDate tanggal) {
        String sql = """
                SELECT * FROM target_produksi
                WHERE id_produk = ?
                ORDER BY created_at DESC
                LIMIT 1
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idProduk);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error getAktifPadaTanggal: " + e.getMessage());
        }
        return null;
    }

    // Helper
    private static TargetProduksi mapRow(ResultSet rs) throws SQLException {
        TargetProduksi t = new TargetProduksi();
        t.idTarget     = rs.getInt("id_target");
        t.idProduk     = rs.getInt("id_produk");
        t.nikPengguna  = rs.getString("nik_pengguna");
        t.jumlahTarget = rs.getInt("jumlah_target");
        t.periodeTarget = rs.getInt("periode_target");
        t.createdAt    = rs.getTimestamp("created_at").toLocalDateTime();
        t.updatedAt    = rs.getTimestamp("updated_at").toLocalDateTime();
        return t;
    }

    // Getters
    public int getIdTarget()        { return idTarget; }
    public int getIdProduk()        { return idProduk; }
    public String getNikPengguna()  { return nikPengguna; }
    public int getJumlahTarget()    { return jumlahTarget; }
    public int getPeriodeTarget()   { return periodeTarget; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters
    public void setIdProduk(int v)       { this.idProduk = v; }
    public void setNikPengguna(String v) { this.nikPengguna = v; }
    public void setJumlahTarget(int v)   { this.jumlahTarget = v; }
    public void setPeriodeTarget(int v)  { this.periodeTarget = v; }

    @Override
    public String toString() {
        return "TargetProduksi{" +
                "idTarget=" + idTarget +
                ", idProduk=" + idProduk +
                ", jumlahTarget=" + jumlahTarget +
                ", periodeTarget=" + periodeTarget +
                '}';
    }
}