package model;

import util.DBConnection;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TargetProduksi {
    // Fields
    private int       idTarget;
    private int       idProduk;
    private LocalDate periodeAwal;
    private LocalDate periodeAkhir;
    private int       jumlahTarget;
    private String    keterangan;

    // Constructors
    public TargetProduksi() {}

    public TargetProduksi(int idProduk, LocalDate periodeAwal,
                          LocalDate periodeAkhir, int jumlahTarget,
                          String keterangan) {
        this.idProduk     = idProduk;
        this.periodeAwal  = periodeAwal;
        this.periodeAkhir = periodeAkhir;
        this.jumlahTarget = jumlahTarget;
        this.keterangan   = keterangan;
    }

    // 1. save() : untuk melakukan insert data target produksi ke dalam database
    public boolean save() {
        String sql = """
                INSERT INTO target_produksi
                    (id_produk, periode_awal, periode_akhir,
                     jumlah_target, keterangan)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, idProduk);
            stmt.setDate(2, Date.valueOf(periodeAwal));
            stmt.setDate(3, Date.valueOf(periodeAkhir));
            stmt.setInt(4, jumlahTarget);
            stmt.setString(5, keterangan);

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        this.idTarget = keys.getInt(1);
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error saat menyimpan TargetProduksi: " + e.getMessage());
        }
        return false;
    }

    // 2. getAll() : untuk mengambil seluruh data target produksi dari database
    public static List<TargetProduksi> getAll() {
        List<TargetProduksi> list = new ArrayList<>();
        String sql = "SELECT * FROM target_produksi ORDER BY periode_awal DESC";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {

            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("Error saat getAll TargetProduksi: " + e.getMessage());
        }
        return list;
    }

    // 3. getById() : untuk mengambil data target produksi sesuai idTarget
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

    // 4. getByIdProduk() : untuk mengambil data target produksi sesuai idProduk
    public static List<TargetProduksi> getByIdProduk(int idProduk) {
        List<TargetProduksi> list = new ArrayList<>();
        String sql = "SELECT * FROM target_produksi WHERE id_produk = ? ORDER BY periode_awal DESC";

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

    // 5. update() : untuk memperbarui data target produksi yang sudah ada
    public boolean update() {
        String sql = """
                UPDATE target_produksi
                SET id_produk     = ?,
                    periode_awal  = ?,
                    periode_akhir = ?,
                    jumlah_target = ?,
                    keterangan    = ?
                WHERE id_target = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idProduk);
            stmt.setDate(2, Date.valueOf(periodeAwal));
            stmt.setDate(3, Date.valueOf(periodeAkhir));
            stmt.setInt(4, jumlahTarget);
            stmt.setString(5, keterangan);
            stmt.setInt(6, idTarget);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error saat update TargetProduksi: " + e.getMessage());
        }
        return false;
    }

    // 6. delete() : untuk menghapus data target produksi berdasarkan object
    public boolean delete() {
        return deleteById(this.idTarget);
    }

    // 7. deleteById() : untuk menghapus data target produksi berdasarkan ID secara statis
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

    // 8. getRealisasi() : untuk menghitung total realisasi produksi bersih dalam periode target
    public int getRealisasi() {
        String sql = """
                SELECT COALESCE(SUM(jumlah_aktual - jumlah_defect), 0) AS realisasi
                FROM produksi_harian
                WHERE id_produk         = ?
                  AND tanggal_produksi BETWEEN ? AND ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idProduk);
            stmt.setDate(2, Date.valueOf(periodeAwal));
            stmt.setDate(3, Date.valueOf(periodeAkhir));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("realisasi");
            }
        } catch (SQLException e) {
            System.err.println("Error saat getRealisasi TargetProduksi: " + e.getMessage());
        }
        return 0;
    }

    // 9. getPersentasePencapaian() : untuk menghitung persentase pencapaian target
    public double getPersentasePencapaian() {
        if (jumlahTarget == 0) return 0.0;
        return (getRealisasi() / (double) jumlahTarget) * 100.0;
    }

    // 10. isTercapai() : untuk mengecek apakah realisasi sudah memenuhi target
    public boolean isTercapai() {
        return getRealisasi() >= jumlahTarget;
    }

    // 11. getAktifPadaTanggal() : untuk mengambil target yang sedang berjalan di tanggal tertentu
    public static TargetProduksi getAktifPadaTanggal(int idProduk, LocalDate tanggal) {
        String sql = "SELECT * FROM target_produksi WHERE id_produk = ? AND ? BETWEEN periode_awal AND periode_akhir LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idProduk);
            stmt.setObject(2, tanggal);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
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
        t.periodeAwal  = rs.getDate("periode_awal").toLocalDate();
        t.periodeAkhir = rs.getDate("periode_akhir").toLocalDate();
        t.jumlahTarget = rs.getInt("jumlah_target");
        t.keterangan   = rs.getString("keterangan");
        return t;
    }

    // Getters
    public int getIdTarget()                  { return idTarget; }
    public int getIdProduk()                  { return idProduk; }
    public LocalDate getPeriodeAwal()         { return periodeAwal; }
    public LocalDate getPeriodeAkhir()        { return periodeAkhir; }
    public int getJumlahTarget()              { return jumlahTarget; }
    public String getKeterangan()             { return keterangan; }

    // Setters
    public void setIdProduk(int v)            { this.idProduk = v; }
    public void setPeriodeAwal(LocalDate v)   { this.periodeAwal = v; }
    public void setPeriodeAkhir(LocalDate v)  { this.periodeAkhir = v; }
    public void setJumlahTarget(int v)        { this.jumlahTarget = v; }
    public void setKeterangan(String v)       { this.keterangan = v; }

    // toString
    @Override
    public String toString() {
        return "TargetProduksi{" +
                "idTarget=" + idTarget +
                ", idProduk=" + idProduk +
                ", periode=" + periodeAwal + " s/d " + periodeAkhir +
                ", target=" + jumlahTarget +
                '}';
    }
}