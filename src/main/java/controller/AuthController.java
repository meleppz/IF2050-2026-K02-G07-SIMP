package controller;

import model.Pengguna;
import model.Peran;
import util.DBConnection;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;

public class AuthController {

    private ArrayList<Pengguna> listPenggunaTerdaftar = new ArrayList<>();

    // =========================================================
    // PUBLIC — dipanggil dari View
    // =========================================================

    public Pengguna login(String nik, String password) {
        if (!validateNIK(nik)) return null;
        if (!checkUser(nik)) return null;
        if (!checkAccess(nik)) return null;

        String sql = "SELECT * FROM pengguna WHERE nik = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nik);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    if (!verifyPassword(password, storedPassword)) return null;

                    Pengguna p = mapRow(rs);
                    return p;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error login: " + e.getMessage());
        }
        return null;
    }

    public boolean register(String nik, String username, String password,
                            String nama, String peranStr) {
        if (!validateNIK(nik)) return false;
        if (checkUser(nik)) return false; // sudah ada

        String hashedPassword = hashPassword(password);
        if (hashedPassword == null) return false;

        Peran peran = stringToPeran(peranStr);
        return createUser(nik, username, hashedPassword, nama, peran);
    }

    // =========================================================
    // PRIVATE — internal logic
    // =========================================================

    private boolean createUser(String nik, String username,
                               String hashedPassword, String nama, Peran peran) {
        String sql = """
                INSERT INTO pengguna (nik, nama, username, password, peran)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nik);
            stmt.setString(2, nama);
            stmt.setString(3, username);
            stmt.setString(4, hashedPassword);
            stmt.setString(5, peran != null ? peran.name().toLowerCase() : null);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error createUser: " + e.getMessage());
            return false;
        }
    }

    private boolean checkUser(String nik) {
        String sql = "SELECT 1 FROM pengguna WHERE nik = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nik);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Error checkUser: " + e.getMessage());
            return false;
        }
    }

    private boolean checkAccess(String nik) {
        String sql = "SELECT status_aktif FROM pengguna WHERE nik = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nik);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getBoolean("status_aktif");
            }
        } catch (SQLException e) {
            System.err.println("Error checkAccess: " + e.getMessage());
        }
        return false;
    }

    private boolean verifyPassword(String inputPassword, String storedHash) {
        String hashed = hashPassword(inputPassword);
        return hashed != null && hashed.equals(storedHash);
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error hashing: " + e.getMessage());
            return null;
        }
    }

    private boolean validateNIK(String nik) {
        return nik != null && !nik.trim().isEmpty();
    }

    private Peran stringToPeran(String peran) {
        if (peran == null) return null;
        return switch (peran.toLowerCase()) {
            case "operator" -> Peran.OPERATOR;
            case "supervisor" -> Peran.SUPERVISOR;
            default -> null;
        };
    }

    private Pengguna mapRow(ResultSet rs) throws SQLException {
        String peranStr = rs.getString("peran");
        Peran peran = stringToPeran(peranStr);

        Pengguna p = new Pengguna(
                rs.getString("nik"),
                rs.getString("nama"),
                rs.getString("username"),
                rs.getString("password"),
                peran
        );
        p.setNomorTelepon(rs.getString("nomor_telepon"));
        p.setEmail(rs.getString("email"));
        p.setDivisi(rs.getString("divisi"));
        p.setStatusAktif(rs.getBoolean("status_aktif"));
        return p;
    }
}