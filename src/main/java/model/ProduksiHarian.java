package model;

import util.DBConnection;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ProduksiHarian {
    // Fields
    private int     idProduksi;
    private String  nikOperator;
    private LocalDate tanggalProduksi;
    private int     jumlahAktual;
    private int     jumlahDefect;
    private String  kendala;
    private int     idProduk;

    // Constructors
    public ProduksiHarian() {}
    public ProduksiHarian(String nikOperator, LocalDate tanggalProduksi,
                          int jumlahAktual, int jumlahDefect,
                          String kendala, int idProduk) {
        this.nikOperator      = nikOperator;
        this.tanggalProduksi  = tanggalProduksi;
        this.jumlahAktual     = jumlahAktual;
        this.jumlahDefect     = jumlahDefect;
        this.kendala          = kendala;
        this.idProduk         = idProduk;
    }

    // Getters
    public int getIdProduksi()             { return idProduksi; }
    public String getNikOperator()         { return nikOperator; }
    public LocalDate getTanggalProduksi()  { return tanggalProduksi; }
    public int getJumlahAktual()           { return jumlahAktual; }
    public int getJumlahDefect()           { return jumlahDefect; }
    public String getKendala()             { return kendala; }
    public int getIdProduk()               { return idProduk; }

    // Setters
    public void setNikOperator(String v)   { this.nikOperator = v; }
    public void setTanggalProduksi(LocalDate v) { this.tanggalProduksi = v; }
    public void setJumlahAktual(int v)     { this.jumlahAktual = v; }
    public void setJumlahDefect(int v)     { this.jumlahDefect = v; }
    public void setKendala(String v)       { this.kendala = v; }
    public void setIdProduk(int v)         { this.idProduk = v; }

    // 1. save() : untuk melakukan insert data produksi harian ke dalam database
    public boolean save() {
        String sql = """
                INSERT INTO produksi_harian
                    (nik_operator, tanggal_produksi, jumlah_aktual,
                     jumlah_defect, kendala, id_produk)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, nikOperator);
            stmt.setDate(2, Date.valueOf(tanggalProduksi));
            stmt.setInt(3, jumlahAktual);
            stmt.setInt(4, jumlahDefect);
            stmt.setString(5, kendala);
            stmt.setInt(6, idProduk);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        this.idProduksi = keys.getInt(1);
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error saat menyimpan ProduksiHarian: " + e.getMessage());
        }
        return false;
    }

    // 2. getAll() : untuk melihat seluruh data produksi harian dari database
    public static List<ProduksiHarian> getAll() {
        List<ProduksiHarian> list = new ArrayList<>();
        String sql = "SELECT * FROM produksi_harian ORDER BY tanggal_produksi DESC";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error saat mengambil semua ProduksiHarian: " + e.getMessage());
        }
        return list;
    }

    // 3. getById() : untuk melihat data produksi harian sesuai idProduksi
    public static ProduksiHarian getById(int idProduksi) {
        String sql = "SELECT * FROM produksi_harian WHERE id_produksi = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idProduksi);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error saat getById ProduksiHarian: " + e.getMessage());
        }
        return null;
    }

    // 4. getByIdProduk() : untuk melihat data produksi harian sesuai idProduk
    public static List<ProduksiHarian> getByIdProduk(int idProduk) {
        List<ProduksiHarian> list = new ArrayList<>();
        String sql = "SELECT * FROM produksi_harian WHERE id_produk = ? ORDER BY tanggal_produksi DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idProduk);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error saat getByIdProduk ProduksiHarian: " + e.getMessage());
        }
        return list;
    }

    // 5. getByDateRange() : untuk melihat data produksi harian sesuai range waktu tertentu
    public static List<ProduksiHarian> getByDateRange(LocalDate dari, LocalDate sampai) {
        List<ProduksiHarian> list = new ArrayList<>();
        String sql = """
                SELECT * FROM produksi_harian
                WHERE tanggal_produksi BETWEEN ? AND ?
                ORDER BY tanggal_produksi DESC
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(dari));
            stmt.setDate(2, Date.valueOf(sampai));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error saat getByDateRange ProduksiHarian: " + e.getMessage());
        }
        return list;
    }

    // 6. update() : untuk memperbarui data produksi harian yang sudah ada
    public boolean update() {
        String sql = """
                UPDATE produksi_harian
                SET nik_operator      = ?,
                    tanggal_produksi  = ?,
                    jumlah_aktual     = ?,
                    jumlah_defect     = ?,
                    kendala           = ?,
                    id_produk         = ?
                WHERE id_produksi = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nikOperator);
            stmt.setDate(2, Date.valueOf(tanggalProduksi));
            stmt.setInt(3, jumlahAktual);
            stmt.setInt(4, jumlahDefect);
            stmt.setString(5, kendala);
            stmt.setInt(6, idProduk);
            stmt.setInt(7, idProduksi);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error saat update ProduksiHarian: " + e.getMessage());
        }
        return false;
    }

    // 7. delete() : untuk menghapus data produksi harian berdasarkan object
    public boolean delete() {
        return deleteById(this.idProduksi);
    }

    // 8. deleteById() : untuk menghapus data produksi harian berdasarkan ID
    public static boolean deleteById(int idProduksi) {
        String sql = "DELETE FROM produksi_harian WHERE id_produksi = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idProduksi);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error saat delete ProduksiHarian: " + e.getMessage());
        }
        return false;
    }

    // Helper
    private static ProduksiHarian mapRow(ResultSet rs) throws SQLException {
        ProduksiHarian p = new ProduksiHarian();
        p.idProduksi      = rs.getInt("id_produksi");
        p.nikOperator     = rs.getString("nik_operator");
        p.tanggalProduksi = rs.getDate("tanggal_produksi").toLocalDate();
        p.jumlahAktual    = rs.getInt("jumlah_aktual");
        p.jumlahDefect    = rs.getInt("jumlah_defect");
        p.kendala         = rs.getString("kendala");
        p.idProduk        = rs.getInt("id_produk");
        return p;
    }

    // Business Logic
    public int getJumlahBersih() {
        return jumlahAktual - jumlahDefect;
    }

    public double getPersentaseDefect() {
        if (jumlahAktual == 0) return 0.0;
        return (jumlahDefect / (double) jumlahAktual) * 100.0;
    }

    // toString
    @Override
    public String toString() {
        return "ProduksiHarian{" +
                "idProduksi=" + idProduksi +
                ", idProduk=" + idProduk +
                ", tanggal=" + tanggalProduksi +
                ", aktual=" + jumlahAktual +
                ", defect=" + jumlahDefect +
                '}';
    }
}