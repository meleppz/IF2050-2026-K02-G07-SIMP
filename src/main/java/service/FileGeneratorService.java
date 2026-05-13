package service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import model.LaporanProduksi;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

/**
 * FileGeneratorService bertanggung jawab mengubah LaporanProduksi
 * menjadi file fisik (.xlsx atau .pdf).
 *
 * Library: Apache POI (Excel) + iText 7 (PDF)
 * Dependency Maven:
 *   <dependency>
 *     <groupId>org.apache.poi</groupId>
 *     <artifactId>poi-ooxml</artifactId>
 *     <version>5.2.5</version>
 *   </dependency>
 *   <dependency>
 *     <groupId>com.itextpdf</groupId>
 *     <artifactId>itext7-core</artifactId>
 *     <version>8.0.2</version>
 *     <type>pom</type>
 *   </dependency>
 */
public class FileGeneratorService {

    private static final DateTimeFormatter FMT_TANGGAL =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Warna tema SIMP (hijau teal)
    private static final DeviceRgb WARNA_HEADER_PDF =
            new DeviceRgb(0, 229, 160);    // #00e5a0
    private static final DeviceRgb WARNA_TEKS_HEADER_PDF =
            new DeviceRgb(10, 31, 31);     // #0a1f1f
    private static final DeviceRgb WARNA_SUBHEADER_PDF =
            new DeviceRgb(19, 41, 41);     // #132929

    // =========================================================
    // ENTRY POINT
    // =========================================================

    /**
     * Generate file laporan ke temp directory.
     *
     * @param laporan  data laporan hasil olahan ReportController
     * @param format   "pdf" atau "xlsx"
     * @return path file temp yang dihasilkan
     * @throws IOException jika gagal menulis file
     */
    public Path generate(LaporanProduksi laporan, String format) throws IOException {
        Path tempFile = Files.createTempFile("laporan_produksi_", "." + format);

        if ("xlsx".equalsIgnoreCase(format)) {
            generateExcel(laporan, tempFile.toFile());
        } else {
            generatePDF(laporan, tempFile.toFile());
        }

        return tempFile;
    }

    // =========================================================
    // EXCEL — Apache POI
    // =========================================================

    private void generateExcel(LaporanProduksi laporan, File outputFile) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(outputFile)) {

            // — Sheet 1: Tabel Produksi —
            XSSFSheet sheetProduksi = workbook.createSheet("Data Produksi");
            buatHeaderSheetExcel(workbook, sheetProduksi, laporan);
            buatTabelProduksiExcel(workbook, sheetProduksi, laporan);

            // — Sheet 2: Performa —
            XSSFSheet sheetPerforma = workbook.createSheet("Performa Produk");
            buatTabelPerformaExcel(workbook, sheetPerforma, laporan);

            // Auto-size semua kolom
            for (int i = 0; i < 6; i++) sheetProduksi.autoSizeColumn(i);
            for (int i = 0; i < 6; i++) sheetPerforma.autoSizeColumn(i);

            workbook.write(fos);
        }
    }

    private void buatHeaderSheetExcel(XSSFWorkbook wb, XSSFSheet sheet,
                                      LaporanProduksi laporan) {
        // Style judul
        CellStyle styleJudul = wb.createCellStyle();
        Font fontJudul = wb.createFont();
        fontJudul.setBold(true);
        fontJudul.setFontHeightInPoints((short) 14);
        styleJudul.setFont(fontJudul);

        // Style subtitle
        CellStyle styleSub = wb.createCellStyle();
        Font fontSub = wb.createFont();
        fontSub.setItalic(true);
        fontSub.setFontHeightInPoints((short) 11);
        styleSub.setFont(fontSub);

        // Baris judul
        Row bJudul = sheet.createRow(0);
        Cell cJudul = bJudul.createCell(0);
        cJudul.setCellValue("Laporan Produksi — SIMP");
        cJudul.setCellStyle(styleJudul);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

        // Baris periode
        Row bPeriode = sheet.createRow(1);
        Cell cPeriode = bPeriode.createCell(0);
        String periode = laporan.getTglMulai() != null
                ? laporan.getTglMulai().format(FMT_TANGGAL) + " s/d " +
                laporan.getTglSelesai().format(FMT_TANGGAL)
                : "Semua data";
        cPeriode.setCellValue("Periode: " + periode);
        cPeriode.setCellStyle(styleSub);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 5));

        // Baris tanggal generate
        Row bGenerate = sheet.createRow(2);
        Cell cGenerate = bGenerate.createCell(0);
        cGenerate.setCellValue("Dibuat pada: " +
                laporan.getTglGenerate().format(FMT_TANGGAL));
        cGenerate.setCellStyle(styleSub);
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 5));

        // Baris kosong sebelum tabel
        sheet.createRow(3);
    }

    private void buatTabelProduksiExcel(XSSFWorkbook wb, XSSFSheet sheet,
                                        LaporanProduksi laporan) {
        // Style header tabel
        CellStyle styleHeader = wb.createCellStyle();
        styleHeader.setFillForegroundColor(new XSSFColor(
                new byte[]{(byte) 0, (byte) 229, (byte) 160}, null)); // #00e5a0
        styleHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styleHeader.setBorderBottom(BorderStyle.THIN);
        Font fontHeader = wb.createFont();
        fontHeader.setBold(true);
        fontHeader.setColor(IndexedColors.BLACK.getIndex());
        styleHeader.setFont(fontHeader);

        // Style baris data (stripe)
        CellStyle styleGanjil = wb.createCellStyle();
        styleGanjil.setFillForegroundColor(new XSSFColor(
                new byte[]{(byte) 13, (byte) 38, (byte) 38}, null)); // #0d2626
        styleGanjil.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font fontPutih = wb.createFont();
        fontPutih.setColor(IndexedColors.WHITE.getIndex());
        styleGanjil.setFont(fontPutih);

        CellStyle styleGenap = wb.createCellStyle();
        styleGenap.setFillForegroundColor(new XSSFColor(
                new byte[]{(byte) 19, (byte) 41, (byte) 41}, null)); // #132929
        styleGenap.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font fontPutih2 = wb.createFont();
        fontPutih2.setColor(IndexedColors.WHITE.getIndex());
        styleGenap.setFont(fontPutih2);

        // Header tabel (baris ke-4, index 4)
        String[] kolom = {"ID Produk", "Nama Produk", "Tanggal Produksi",
                "Jumlah Produksi", "Jumlah Defect"};
        Row rowHeader = sheet.createRow(4);
        for (int i = 0; i < kolom.length; i++) {
            Cell c = rowHeader.createCell(i);
            c.setCellValue(kolom[i]);
            c.setCellStyle(styleHeader);
        }

        // Baris data
        int rowIdx = 5;
        for (LaporanProduksi.BarisTabel baris : laporan.getBarisTabel()) {
            Row row = sheet.createRow(rowIdx);
            CellStyle style = (rowIdx % 2 == 0) ? styleGanjil : styleGenap;

            buatCellExcel(row, 0, String.valueOf(baris.getIdProduk()), style);
            buatCellExcel(row, 1, baris.getNamaProduk(), style);
            buatCellExcel(row, 2,
                    baris.getTanggalProduksi() != null
                            ? baris.getTanggalProduksi().format(FMT_TANGGAL)
                            : "-",
                    style);
            buatCellExcel(row, 3, String.valueOf(baris.getJumlahProduksi()), style);
            buatCellExcel(row, 4, String.valueOf(baris.getJumlahDefect()), style);

            rowIdx++;
        }
    }

    private void buatTabelPerformaExcel(XSSFWorkbook wb, XSSFSheet sheet,
                                        LaporanProduksi laporan) {
        CellStyle styleHeader = wb.createCellStyle();
        styleHeader.setFillForegroundColor(new XSSFColor(
                new byte[]{(byte) 0, (byte) 229, (byte) 160}, null));
        styleHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font fontHeader = wb.createFont();
        fontHeader.setBold(true);
        styleHeader.setFont(fontHeader);

        CellStyle styleData = wb.createCellStyle();
        Font fontPutih = wb.createFont();
        fontPutih.setColor(IndexedColors.WHITE.getIndex());
        styleData.setFont(fontPutih);
        styleData.setFillForegroundColor(new XSSFColor(
                new byte[]{(byte) 13, (byte) 38, (byte) 38}, null));
        styleData.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Judul sheet
        Row rowJudul = sheet.createRow(0);
        Cell cJudul = rowJudul.createCell(0);
        cJudul.setCellValue("Performa Produk");
        CellStyle styleJudul = wb.createCellStyle();
        Font fontJudul = wb.createFont();
        fontJudul.setBold(true);
        fontJudul.setFontHeightInPoints((short) 13);
        styleJudul.setFont(fontJudul);
        cJudul.setCellStyle(styleJudul);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
        sheet.createRow(1); // baris kosong

        // Header kolom
        String[] kolom = {"ID Produk", "Nama Produk", "Total Produksi",
                "Total Defect", "Rasio Defect (%)", "Realisasi vs Target (%)"};
        Row rowHeader = sheet.createRow(2);
        for (int i = 0; i < kolom.length; i++) {
            Cell c = rowHeader.createCell(i);
            c.setCellValue(kolom[i]);
            c.setCellStyle(styleHeader);
        }

        // Baris data
        int rowIdx = 3;
        for (LaporanProduksi.PerformaProduk p : laporan.getPerformaList()) {
            Row row = sheet.createRow(rowIdx++);
            buatCellExcel(row, 0, String.valueOf(p.getIdProduk()), styleData);
            buatCellExcel(row, 1, p.getNamaProduk(), styleData);
            buatCellExcel(row, 2, String.valueOf(p.getTotalProduksi()), styleData);
            buatCellExcel(row, 3, String.valueOf(p.getTotalDefect()), styleData);
            buatCellExcel(row, 4,
                    String.format("%.1f%%", p.getRasioDefect()), styleData);
            buatCellExcel(row, 5,
                    p.getRasioVsTarget() != null
                            ? String.format("%.1f%%", p.getRasioVsTarget())
                            : "N/A",
                    styleData);
        }
    }

    private void buatCellExcel(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    // =========================================================
    // PDF — iText 7
    // =========================================================

    private void generatePDF(LaporanProduksi laporan, File outputFile) throws IOException {
        try (PdfWriter writer = new PdfWriter(outputFile);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf)) {

            PdfFont fontBold = PdfFontFactory.createFont(
                    com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
            PdfFont fontRegular = PdfFontFactory.createFont(
                    com.itextpdf.io.font.constants.StandardFonts.HELVETICA);

            // — Judul —
            String periode = laporan.getTglMulai() != null
                    ? laporan.getTglMulai().format(FMT_TANGGAL) + " s/d " +
                    laporan.getTglSelesai().format(FMT_TANGGAL)
                    : "Semua data";

            doc.add(new Paragraph("Laporan Produksi — SIMP")
                    .setFont(fontBold).setFontSize(16)
                    .setFontColor(WARNA_HEADER_PDF)
                    .setMarginBottom(4));
            doc.add(new Paragraph("Periode: " + periode)
                    .setFont(fontRegular).setFontSize(10)
                    .setFontColor(ColorConstants.GRAY));
            doc.add(new Paragraph("Dibuat pada: " +
                    laporan.getTglGenerate().format(FMT_TANGGAL))
                    .setFont(fontRegular).setFontSize(10)
                    .setFontColor(ColorConstants.GRAY)
                    .setMarginBottom(16));

            // — Tabel Produksi —
            doc.add(new Paragraph("Data Produksi")
                    .setFont(fontBold).setFontSize(12)
                    .setMarginBottom(6));

            float[] lebarKolomProduksi = {60f, 150f, 100f, 100f, 100f};
            Table tabelProduksi = new Table(
                    UnitValue.createPointArray(lebarKolomProduksi));
            tabelProduksi.setWidth(UnitValue.createPercentValue(100));

            // Header
            String[] kolomProduksi = {"ID Produk", "Nama Produk",
                    "Tanggal", "Jml Produksi", "Jml Defect"};
            for (String k : kolomProduksi) {
                tabelProduksi.addHeaderCell(
                        buatCellHeaderPDF(k, fontBold));
            }

            // Data
            boolean stripe = false;
            for (LaporanProduksi.BarisTabel baris : laporan.getBarisTabel()) {
                DeviceRgb warnaBaris = stripe
                        ? new DeviceRgb(19, 41, 41)
                        : new DeviceRgb(13, 38, 38);
                stripe = !stripe;

                tabelProduksi.addCell(buatCellDataPDF(
                        String.valueOf(baris.getIdProduk()), fontRegular, warnaBaris));
                tabelProduksi.addCell(buatCellDataPDF(
                        baris.getNamaProduk(), fontRegular, warnaBaris));
                tabelProduksi.addCell(buatCellDataPDF(
                        baris.getTanggalProduksi() != null
                                ? baris.getTanggalProduksi().format(FMT_TANGGAL) : "-",
                        fontRegular, warnaBaris));
                tabelProduksi.addCell(buatCellDataPDF(
                        String.valueOf(baris.getJumlahProduksi()), fontRegular, warnaBaris));
                tabelProduksi.addCell(buatCellDataPDF(
                        String.valueOf(baris.getJumlahDefect()), fontRegular, warnaBaris));
            }
            doc.add(tabelProduksi);

            // — Tabel Performa —
            doc.add(new Paragraph("Performa Produk")
                    .setFont(fontBold).setFontSize(12)
                    .setMarginTop(20).setMarginBottom(6));

            float[] lebarKolomPerforma = {60f, 120f, 80f, 80f, 80f, 100f};
            Table tabelPerforma = new Table(
                    UnitValue.createPointArray(lebarKolomPerforma));
            tabelPerforma.setWidth(UnitValue.createPercentValue(100));

            String[] kolomPerforma = {"ID Produk", "Nama Produk",
                    "Total Prod.", "Total Defect",
                    "Rasio Defect", "vs Target"};
            for (String k : kolomPerforma) {
                tabelPerforma.addHeaderCell(buatCellHeaderPDF(k, fontBold));
            }

            stripe = false;
            for (LaporanProduksi.PerformaProduk p : laporan.getPerformaList()) {
                DeviceRgb warnaBaris = stripe
                        ? new DeviceRgb(19, 41, 41)
                        : new DeviceRgb(13, 38, 38);
                stripe = !stripe;

                tabelPerforma.addCell(buatCellDataPDF(
                        String.valueOf(p.getIdProduk()), fontRegular, warnaBaris));
                tabelPerforma.addCell(buatCellDataPDF(
                        p.getNamaProduk(), fontRegular, warnaBaris));
                tabelPerforma.addCell(buatCellDataPDF(
                        String.valueOf(p.getTotalProduksi()), fontRegular, warnaBaris));
                tabelPerforma.addCell(buatCellDataPDF(
                        String.valueOf(p.getTotalDefect()), fontRegular, warnaBaris));
                tabelPerforma.addCell(buatCellDataPDF(
                        String.format("%.1f%%", p.getRasioDefect()), fontRegular, warnaBaris));
                tabelPerforma.addCell(buatCellDataPDF(
                        p.getRasioVsTarget() != null
                                ? String.format("%.1f%%", p.getRasioVsTarget()) : "N/A",
                        fontRegular, warnaBaris));
            }
            doc.add(tabelPerforma);
        }
    }

    private Cell buatCellHeaderPDF(String teks, PdfFont font) {
        return new Cell()
                .add(new Paragraph(teks).setFont(font).setFontSize(9)
                        .setFontColor(WARNA_TEKS_HEADER_PDF))
                .setBackgroundColor(WARNA_HEADER_PDF)
                .setPadding(6)
                .setTextAlignment(TextAlignment.CENTER)
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
    }

    private Cell buatCellDataPDF(String teks, PdfFont font, DeviceRgb warnaBg) {
        return new Cell()
                .add(new Paragraph(teks).setFont(font).setFontSize(9)
                        .setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(warnaBg)
                .setPadding(5)
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
    }
}