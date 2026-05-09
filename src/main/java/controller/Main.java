package controller;
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

public class Main {
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
    public static void main(String[] args) {
        println(CYAN + BOLD + "\n╔══════════════════════════════╗");
        println(              "║   SIMP — CLI Testing Tool    ║");
        println(              "╚══════════════════════════════╝" + RESET);

        boolean running = true;
        while (running) {
            println("\n" + BOLD + "=== MAIN MENU ===" + RESET);
            println("  1. Produk");
            println("  2. Produksi Harian");
            println("  3. Target Produksi");
            println("  0. Keluar");
            int choice = promptInt("Pilih menu");
            switch (choice) {
                case 1  -> menuProduk();
                case 2  -> menuProduksiHarian();
                case 3  -> menuTargetProduksi();
                case 0  -> running = false;
                default -> warn("Pilihan tidak valid.");
            }
        }
        println(GREEN + "Sampai jumpa!" + RESET);
    }

    // 2. menuProduk() : untuk mengelola submenu data produk (CRUD)
    static void menuProduk() {
        boolean back = false;
        while (!back) {
            println("\n" + BOLD + "── PRODUK ──" + RESET);
            println("  1. Lihat semua produk");
            println("  2. Cari produk by ID");
            println("  3. Tambah produk baru");
            println("  4. Update produk");
            println("  5. Hapus produk");
            println("  0. Kembali");
            int choice = promptInt("Pilih");

            switch (choice) {
                case 1 -> {
                    List<Produk> list = Produk.getAll();
                    if (list.isEmpty()) { warn("Belum ada produk."); break; }
                    printTableProduk(list);
                }
                case 2 -> {
                    int id = promptInt("ID Produk");
                    Produk p = Produk.getById(id);
                    if (p == null) { warn("Produk tidak ditemukan."); break; }
                    printTableProduk(List.of(p));
                }
                case 3 -> {
                    println(YELLOW + "--- Tambah Produk ---" + RESET);
                    String nama      = promptStr("Nama produk");
                    String kode      = promptStr("Kode produk");
                    String satuan    = promptStr("Satuan (pcs/kg/dll)");
                    String deskripsi = promptStr("Deskripsi");
                    String foto      = promptStr("Path foto (enter = kosong)");
                    int idKategori   = promptInt("ID Kategori");

                    ProdukData data = new ProdukData(
                            nama, kode, satuan, deskripsi,
                            foto.isBlank() ? null : foto, // Gunakan variabel 'foto' langsung, bukan string "foto"
                            String.valueOf(idKategori));
                    Produk p = Produk.create(data);

                    Kategori kat = Kategori.getById(idKategori);
                    if (kat == null) { warn("Kategori ID " + idKategori + " tidak ditemukan."); break; }
                    p.setKategori(kat);
                    p.save();
                    ok("Produk berhasil disimpan. ID: " + p.getIdProduk());
                }
                case 4 -> {
                    int id = promptInt("ID Produk yang mau diupdate");
                    Produk p = Produk.getById(id);
                    if (p == null) { warn("Produk tidak ditemukan."); break; }

                    println("Nama saat ini     : " + p.getNama());
                    println("Kode saat ini     : " + p.getKode());
                    println("Satuan saat ini   : " + p.getSatuan());
                    println("Deskripsi saat ini: " + p.getDeskripsi());

                    String nama      = promptStr("Nama baru      (enter = skip)");
                    String kode      = promptStr("Kode baru      (enter = skip)");
                    String satuan    = promptStr("Satuan baru    (enter = skip)");
                    String deskripsi = promptStr("Deskripsi baru (enter = skip)");
                    String foto      = promptStr("Foto baru      (enter = skip)");

                    ProdukData data = new ProdukData(
                            nama.isBlank()      ? p.getNama()      : nama,
                            kode.isBlank()      ? p.getKode()      : kode,
                            satuan.isBlank()    ? p.getSatuan()    : satuan,
                            deskripsi.isBlank() ? p.getDeskripsi() : deskripsi,
                            foto.isBlank()      ? p.getFoto()      : foto,
                            String.valueOf(p.getKategori().getIdKategori())
                    );
                    p.update(data);
                    ok("Produk berhasil diupdate.");
                }
                case 5 -> {
                    int id = promptInt("ID Produk yang mau dihapus");
                    Produk p = Produk.getById(id);
                    if (p == null) { warn("Produk tidak ditemukan."); break; }
                    print("Yakin hapus produk \"" + p.getNama() + "\"? (y/n): ");
                    if (sc.nextLine().trim().equalsIgnoreCase("y")) {
                        p.delete();
                        ok("Produk dihapus.");
                    } else {
                        println("Dibatalkan.");
                    }
                }
                case 0 -> back = true;
                default -> warn("Pilihan tidak valid.");
            }
        }
    }

    // 3. menuProduksiHarian() : untuk mengelola submenu rekaman produksi harian
    static void menuProduksiHarian() {
        boolean back = false;
        while (!back) {
            println("\n" + BOLD + "── PRODUKSI HARIAN ──" + RESET);
            println("  1. Lihat semua rekaman");
            println("  2. Cari by ID produksi");
            println("  3. Lihat by ID produk");
            println("  4. Lihat by rentang tanggal");
            println("  5. Tambah rekaman baru");
            println("  6. Update rekaman");
            println("  7. Hapus rekaman");
            println("  0. Kembali");
            int choice = promptInt("Pilih");

            switch (choice) {
                case 1 -> {
                    List<ProduksiHarian> list = ProduksiHarian.getAll();
                    if (list.isEmpty()) { warn("Belum ada rekaman."); break; }
                    printTableProduksiHarian(list);
                }
                case 2 -> {
                    int id = promptInt("ID Produksi");
                    ProduksiHarian ph = ProduksiHarian.getById(id);
                    if (ph == null) { warn("Rekaman tidak ditemukan."); break; }
                    printTableProduksiHarian(List.of(ph));
                }
                case 3 -> {
                    int idProduk = promptInt("ID Produk");
                    List<ProduksiHarian> list = ProduksiHarian.getByIdProduk(idProduk);
                    if (list.isEmpty()) { warn("Tidak ada rekaman untuk produk ini."); break; }
                    printTableProduksiHarian(list);
                }
                case 4 -> {
                    LocalDate dari   = promptDate("Dari tanggal (yyyy-MM-dd)");
                    LocalDate sampai = promptDate("Sampai tanggal (yyyy-MM-dd)");
                    List<ProduksiHarian> list = ProduksiHarian.getByDateRange(dari, sampai);
                    if (list.isEmpty()) { warn("Tidak ada rekaman di rentang tersebut."); break; }
                    printTableProduksiHarian(list);
                }
                case 5 -> {
                    println(YELLOW + "--- Tambah Rekaman Produksi Harian ---" + RESET);
                    int idProduk  = promptInt("ID Produk");
                    Produk produk = Produk.getById(idProduk);
                    if (produk == null) { warn("Produk ID " + idProduk + " tidak ditemukan."); break; }

                    String    nik     = promptStr("NIK Operator");
                    LocalDate tanggal = promptDate("Tanggal produksi (yyyy-MM-dd)");
                    int       aktual  = promptInt("Jumlah aktual");
                    int       defect  = promptInt("Jumlah defect");
                    String    kendala = promptStr("Kendala (enter = tidak ada)");

                    ProduksiHarian ph = new ProduksiHarian(
                            nik, tanggal, aktual, defect,
                            kendala.isBlank() ? null : kendala,
                            idProduk
                    );
                    produk.save(ph);
                    ok("Rekaman produksi harian berhasil disimpan.");
                }
                case 6 -> {
                    int idProduksi    = promptInt("ID Produksi yang mau diupdate");
                    ProduksiHarian ph = ProduksiHarian.getById(idProduksi);
                    if (ph == null) { warn("Rekaman tidak ditemukan."); break; }

                    Produk produk = Produk.getById(ph.getIdProduk());
                    if (produk == null) { warn("Produk terkait tidak ditemukan."); break; }

                    println("Jumlah aktual saat ini : " + ph.getJumlahAktual());
                    println("Jumlah defect saat ini : " + ph.getJumlahDefect());
                    println("Kendala saat ini        : " + ph.getKendala());

                    String aktualStr = promptStr("Jumlah aktual baru (enter = skip)");
                    String defectStr = promptStr("Jumlah defect baru (enter = skip)");
                    String kendala   = promptStr("Kendala baru        (enter = skip)");

                    if (!aktualStr.isBlank()) ph.setJumlahAktual(Integer.parseInt(aktualStr.trim()));
                    if (!defectStr.isBlank()) ph.setJumlahDefect(Integer.parseInt(defectStr.trim()));
                    if (!kendala.isBlank())   ph.setKendala(kendala);

                    if (produk.update(ph)) ok("Rekaman berhasil diupdate.");
                    else warn("Gagal update rekaman.");
                }
                case 7 -> {
                    int idProduksi    = promptInt("ID Produksi yang mau dihapus");
                    ProduksiHarian ph = ProduksiHarian.getById(idProduksi);
                    if (ph == null) { warn("Rekaman tidak ditemukan."); break; }

                    Produk produk = Produk.getById(ph.getIdProduk());
                    if (produk == null) { warn("Produk terkait tidak ditemukan."); break; }

                    print("Yakin hapus rekaman ID " + idProduksi + "? (y/n): ");
                    if (sc.nextLine().trim().equalsIgnoreCase("y")) {
                        if (produk.delete(idProduksi)) ok("Rekaman dihapus.");
                        else warn("Gagal hapus rekaman.");
                    } else {
                        println("Dibatalkan.");
                    }
                }
                case 0 -> back = true;
                default -> warn("Pilihan tidak valid.");
            }
        }
    }

    // 4. menuTargetProduksi() : untuk mengelola submenu target produksi
    static void menuTargetProduksi() {
        boolean back = false;
        while (!back) {
            println("\n" + BOLD + "── TARGET PRODUKSI ──" + RESET);
            println("  1. Lihat semua target");
            println("  2. Cari by ID target");
            println("  3. Lihat by ID produk");
            println("  4. Cek target aktif hari ini");
            println("  5. Tambah target baru");
            println("  6. Update target");
            println("  7. Hapus target");
            println("  0. Kembali");
            int choice = promptInt("Pilih");

            switch (choice) {
                case 1 -> {
                    List<TargetProduksi> list = TargetProduksi.getAll();
                    if (list.isEmpty()) { warn("Belum ada target."); break; }
                    printTableTargetProduksi(list);
                }
                case 2 -> {
                    int id = promptInt("ID Target");
                    TargetProduksi t = TargetProduksi.getById(id);
                    if (t == null) { warn("Target tidak ditemukan."); break; }
                    printTableTargetProduksi(List.of(t));
                    printRealisasi(t);
                }
                case 3 -> {
                    int idProduk = promptInt("ID Produk");
                    List<TargetProduksi> list = TargetProduksi.getByIdProduk(idProduk);
                    if (list.isEmpty()) { warn("Tidak ada target untuk produk ini."); break; }
                    printTableTargetProduksi(list);
                }
                case 4 -> {
                    int idProduk = promptInt("ID Produk");
                    TargetProduksi t = TargetProduksi.getAktifPadaTanggal(idProduk, LocalDate.now());
                    if (t == null) { warn("Tidak ada target aktif hari ini untuk produk ini."); break; }
                    printTableTargetProduksi(List.of(t));
                    printRealisasi(t);
                }
                case 5 -> {
                    println(YELLOW + "--- Tambah Target Produksi ---" + RESET);
                    int       idProduk = promptInt("ID Produk");
                    LocalDate awal     = promptDate("Periode awal  (yyyy-MM-dd)");
                    LocalDate akhir    = promptDate("Periode akhir (yyyy-MM-dd)");
                    int       target   = promptInt("Jumlah target (unit)");
                    String    ket      = promptStr("Keterangan (enter = kosong)");

                    TargetProduksi t = new TargetProduksi(
                            idProduk, awal, akhir, target,
                            ket.isBlank() ? null : ket
                    );
                    if (t.save()) ok("Target disimpan. ID: " + t.getIdTarget());
                    else warn("Gagal menyimpan target.");
                }
                case 6 -> {
                    int id = promptInt("ID Target yang mau diupdate");
                    TargetProduksi t = TargetProduksi.getById(id);
                    if (t == null) { warn("Target tidak ditemukan."); break; }

                    println("Jumlah target saat ini : " + t.getJumlahTarget());
                    println("Keterangan saat ini    : " + t.getKeterangan());

                    String targetStr = promptStr("Jumlah target baru (enter = skip)");
                    String ket       = promptStr("Keterangan baru    (enter = skip)");

                    if (!targetStr.isBlank()) t.setJumlahTarget(Integer.parseInt(targetStr.trim()));
                    if (!ket.isBlank())       t.setKeterangan(ket);

                    if (t.update()) ok("Target berhasil diupdate.");
                    else warn("Gagal update target.");
                }
                case 7 -> {
                    int id = promptInt("ID Target yang mau dihapus");
                    print("Yakin hapus target ID " + id + "? (y/n): ");
                    if (sc.nextLine().trim().equalsIgnoreCase("y")) {
                        if (TargetProduksi.deleteById(id)) ok("Target dihapus.");
                        else warn("Gagal hapus / ID tidak ditemukan.");
                    } else {
                        println("Dibatalkan.");
                    }
                }
                case 0 -> back = true;
                default -> warn("Pilihan tidak valid.");
            }
        }
    }

    // 5. printTableProduk() : untuk menampilkan data produk dalam format tabel
    static void printTableProduk(List<Produk> list) {
        String fmt = "| %-4s | %-20s | %-8s | %-6s | %-12s |%n";
        String sep = "+------+----------------------+----------+--------+--------------+";
        println(sep);
        System.out.printf(fmt, "ID", "Nama", "Kode", "Satuan", "Kategori");
        println(sep);
        for (Produk p : list) {
            String katNama = p.getKategori() != null ? p.getKategori().getNamaKategori() : "-";
            System.out.printf(fmt,
                    p.getIdProduk(),
                    truncate(p.getNama(), 20),
                    truncate(p.getKode(), 8),
                    truncate(p.getSatuan(), 6),
                    truncate(katNama, 12)
            );
        }
        println(sep);
        println("Total: " + list.size() + " produk");
    }

    // 6. printTableProduksiHarian() : untuk menampilkan data produksi harian dalam format tabel
    static void printTableProduksiHarian(List<ProduksiHarian> list) {
        String fmt = "| %-5s | %-6s | %-10s | %-12s | %-7s | %-6s | %-18s |%n";
        String sep = "+-------+--------+------------+--------------+---------+--------+--------------------+";
        println(sep);
        System.out.printf(fmt, "ID", "Produk", "NIK Op.", "Tanggal", "Aktual", "Defect", "Kendala");
        println(sep);
        for (ProduksiHarian p : list) {
            System.out.printf(fmt,
                    p.getIdProduksi(),
                    p.getIdProduk(),
                    truncate(p.getNikOperator(), 10),
                    p.getTanggalProduksi(),
                    p.getJumlahAktual(),
                    p.getJumlahDefect(),
                    truncate(p.getKendala() == null ? "-" : p.getKendala(), 18)
            );
        }
        println(sep);
        println("Total: " + list.size() + " rekaman");
    }

    // 7. printTableTargetProduksi() : untuk menampilkan data target produksi dalam format tabel
    static void printTableTargetProduksi(List<TargetProduksi> list) {
        String fmt = "| %-5s | %-6s | %-12s | %-12s | %-8s | %-18s |%n";
        String sep = "+-------+--------+--------------+--------------+----------+--------------------+";
        println(sep);
        System.out.printf(fmt, "ID", "Produk", "Periode Awal", "Periode Akhir", "Target", "Keterangan");
        println(sep);
        for (TargetProduksi t : list) {
            System.out.printf(fmt,
                    t.getIdTarget(),
                    t.getIdProduk(),
                    t.getPeriodeAwal(),
                    t.getPeriodeAkhir(),
                    t.getJumlahTarget(),
                    truncate(t.getKeterangan() == null ? "-" : t.getKeterangan(), 18)
            );
        }
        println(sep);
        println("Total: " + list.size() + " target");
    }

    // 8. printRealisasi() : untuk menampilkan statistik realisasi dari suatu target
    static void printRealisasi(TargetProduksi t) {
        int    realisasi = t.getRealisasi();
        double pct       = t.getPersentasePencapaian();
        String status    = t.isTercapai()
                ? GREEN + "TERCAPAI ✓" + RESET
                : RED   + "BELUM TERCAPAI" + RESET;
        println(String.format("  Realisasi : %d / %d unit (%.1f%%)", realisasi, t.getJumlahTarget(), pct));
        println("  Status    : " + status);
    }

    // 9. promptInt() : helper untuk mengambil input integer yang valid
    static int promptInt(String label) {
        while (true) {
            print(CYAN + label + ": " + RESET);
            String line = sc.nextLine().trim();
            try { return Integer.parseInt(line); }
            catch (NumberFormatException e) { warn("Masukkan angka yang valid."); }
        }
    }

    // 10. promptStr() : helper untuk mengambil input string dari user
    static String promptStr(String label) {
        print(CYAN + label + ": " + RESET);
        return sc.nextLine();
    }

    // 11. promptDate() : helper untuk mengambil input tanggal dengan format tertentu
    static LocalDate promptDate(String label) {
        while (true) {
            print(CYAN + label + ": " + RESET);
            try { return LocalDate.parse(sc.nextLine().trim(), DATE_FMT); }
            catch (DateTimeParseException e) { warn("Format salah. Gunakan yyyy-MM-dd (contoh: 2026-05-09)"); }
        }
    }

    // Helper Output
    static void println(String s) { System.out.println(s); }
    static void print(String s)   { System.out.print(s); System.out.flush(); }
    static void ok(String s)      { System.out.println(GREEN + "✓ " + s + RESET); }
    static void warn(String s)    { System.out.println(RED   + "✗ " + s + RESET); }

    // Helper String
    static String truncate(String s, int max) {
        if (s == null) return "-";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}