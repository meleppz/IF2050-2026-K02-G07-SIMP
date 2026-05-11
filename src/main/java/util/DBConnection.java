package util;

import io.github.cdimascio.dotenv.Dotenv;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final Dotenv dotenv = Dotenv.load();
    private static final String URL = "jdbc:postgresql://localhost:5432/simp_db";
    private static final String USER = "postgres";
    private static final String PASSWORD = dotenv.get("DB_PASSWORD");

    public static Connection getConnection() throws SQLException {
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✓ Koneksi berhasil");
            return conn;
        } catch (SQLException e) {
            System.out.println("❌ Koneksi gagal: " + e.getMessage());
            throw e;
        }
    }
}