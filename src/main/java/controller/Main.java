package controller;

/*
import model.Kategori;
import model.Produk;
import model.ProdukData;
import model.ProduksiHarian;
import model.TargetProduksi;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;
*/

/**
 * CLI Testing Tool (Backup / Standby Mode)
 * File ini dinonaktifkan sementara untuk menghindari konflik saat transisi ke UI-based (MainApp).
 * Hapus komentar blok jika ingin menjalankan versi CLI kembali.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("CLI Mode is currently disabled. Please run MainApp.java for the UI version.");
    }

    /*
    // Fields
    static final Scanner sc = new Scanner(System.in);
    static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Colors
    static final String RESET  = "\u001B[0m";
    static final String BOLD   = "\u001B[1m";
    static final String CYAN   = "\u001B[36m";
    static final String GREEN  = "\u001B[32m";
    static final String YELLOW = "\u001B[33m";
    static final String RED    = "\u001B[31m";

    // 1. main() : titik masuk utama program CLI
    // (Pindahkan isi main lama ke sini jika ingin diaktifkan lagi)

    // 2. menuProduk() : untuk mengelola submenu data produk (CRUD)
    static void menuProduk() {
        // ... (seluruh isi menuProduk)
    }

    // 3. menuProduksiHarian() : untuk mengelola submenu rekaman produksi harian
    static void menuProduksiHarian() {
        // ... (seluruh isi menuProduksiHarian)
    }

    // 4. menuTargetProduksi() : untuk mengelola submenu target produksi
    static void menuTargetProduksi() {
        // ... (seluruh isi menuTargetProduksi)
    }

    // 5. printTableProduk()
    // 6. printTableProduksiHarian()
    // 7. printTableTargetProduksi()
    // 8. printRealisasi()
    // 9. promptInt()
    // 10. promptStr()
    // 11. promptDate()

    static void println(String s) { System.out.println(s); }
    static void print(String s)   { System.out.print(s); System.out.flush(); }
    static void ok(String s)      { System.out.println(GREEN + "✓ " + s + RESET); }
    static void warn(String s)    { System.out.println(RED   + "✗ " + s + RESET); }

    static String truncate(String s, int max) {
        if (s == null) return "-";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
    */
}