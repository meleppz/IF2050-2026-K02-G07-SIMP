package service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
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

public class FileGeneratorService {

    private static final DateTimeFormatter FMT_TANGGAL =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final DeviceRgb WARNA_HEADER_PDF = new DeviceRgb(0, 229, 160);
    private static final DeviceRgb WARNA_TEKS_HEADER_PDF = new DeviceRgb(10, 31, 31);

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

            XSSFSheet sheetProduksi = workbook.createSheet("Data Produksi");
            buatHeaderSheetExcel(workbook, sheetProduksi, laporan);
            buatTabelProduksiExcel(workbook, sheetProduksi, laporan);

            XSSFSheet sheetPerforma = workbook.createSheet("Performa Produk");
            buatTabelPerformaExcel(workbook, sheetPerforma, laporan);

            for (int i = 0; i < 6; i++) sheetProduksi.autoSizeColumn(i);
            for (int i = 0; i < 6; i++) sheetPerforma.autoSizeColumn(i);

            workbook.write(fos);
        }
    }

    private void buatHeaderSheetExcel(XSSFWorkbook wb, XSSFSheet sheet, LaporanProduksi laporan) {
        CellStyle styleJudul = wb.createCellStyle();
        Font fontJudul = wb.createFont();
        fontJudul.setBold(true);
        fontJudul.setFontHeightInPoints((short) 14);
        styleJudul.setFont(fontJudul);

        CellStyle styleSub = wb.createCellStyle();
        Font fontSub = wb.createFont();
        fontSub.setItalic(true);
        fontSub.setFontHeightInPoints((short) 11);
        styleSub.setFont(fontSub);

        Row bJudul = sheet.createRow(0);
        // PERBAIKAN: Gunakan nama lengkap org.apache.poi.ss.usermodel.Cell
        org.apache.poi.ss.usermodel.Cell cJudul = bJudul.createCell(0);
        cJudul.setCellValue("Laporan Produksi — SIMP");
        cJudul.setCellStyle(styleJudul);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

        Row bPeriode = sheet.createRow(1);
        org.apache.poi.ss.usermodel.Cell cPeriode = bPeriode.createCell(0);
        String periode = laporan.getTglMulai() != null
                ? laporan.getTglMulai().format(FMT_TANGGAL) + " s/d " + laporan.getTglSelesai().format(FMT_TANGGAL)
                : "Semua data";
        cPeriode.setCellValue("Periode: " + periode);
        cPeriode.setCellStyle(styleSub);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 5));

        Row bGenerate = sheet.createRow(2);
        org.apache.poi.ss.usermodel.Cell cGenerate = bGenerate.createCell(0);
        cGenerate.setCellValue("Dibuat pada: " + laporan.getTglGenerate().format(FMT_TANGGAL));
        cGenerate.setCellStyle(styleSub);
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 5));
    }

    private void buatTabelProduksiExcel(XSSFWorkbook wb, XSSFSheet sheet, LaporanProduksi laporan) {
        DefaultIndexedColorMap colorMap = new DefaultIndexedColorMap();

        CellStyle styleHeader = wb.createCellStyle();
        styleHeader.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 0, (byte) 229, (byte) 160}, colorMap));
        styleHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font fontHeader = wb.createFont();
        fontHeader.setBold(true);
        styleHeader.setFont(fontHeader);

        CellStyle styleGanjil = wb.createCellStyle();
        styleGanjil.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 13, (byte) 38, (byte) 38}, colorMap));
        styleGanjil.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font fontWhite = wb.createFont();
        fontWhite.setColor(IndexedColors.WHITE.getIndex());
        styleGanjil.setFont(fontWhite);

        String[] kolom = {"ID Produk", "Nama Produk", "Tanggal Produksi", "Jumlah Produksi", "Jumlah Defect"};
        Row rowHeader = sheet.createRow(4);
        for (int i = 0; i < kolom.length; i++) {
            org.apache.poi.ss.usermodel.Cell c = rowHeader.createCell(i);
            c.setCellValue(kolom[i]);
            c.setCellStyle(styleHeader);
        }

        int rowIdx = 5;
        for (LaporanProduksi.BarisTabel baris : laporan.getBarisTabel()) {
            Row row = sheet.createRow(rowIdx++);
            buatCellExcel(row, 0, String.valueOf(baris.getIdProduk()), styleGanjil);
            buatCellExcel(row, 1, baris.getNamaProduk(), styleGanjil);
            buatCellExcel(row, 2, baris.getTanggalProduksi() != null ? baris.getTanggalProduksi().format(FMT_TANGGAL) : "-", styleGanjil);
            buatCellExcel(row, 3, String.valueOf(baris.getJumlahProduksi()), styleGanjil);
            buatCellExcel(row, 4, String.valueOf(baris.getJumlahDefect()), styleGanjil);
        }
    }

    private void buatTabelPerformaExcel(XSSFWorkbook wb, XSSFSheet sheet, LaporanProduksi laporan) {
        Row rowJudul = sheet.createRow(0);
        org.apache.poi.ss.usermodel.Cell cJudul = rowJudul.createCell(0);
        cJudul.setCellValue("Performa Produk");
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

        String[] kolom = {"ID Produk", "Nama Produk", "Total Produksi", "Total Defect", "Rasio Defect (%)", "Realisasi vs Target (%)"};
        Row rowHeader = sheet.createRow(2);
        for (int i = 0; i < kolom.length; i++) {
            org.apache.poi.ss.usermodel.Cell c = rowHeader.createCell(i);
            c.setCellValue(kolom[i]);
        }

        int rowIdx = 3;
        for (LaporanProduksi.PerformaProduk p : laporan.getPerformaList()) {
            Row row = sheet.createRow(rowIdx++);
            buatCellExcel(row, 0, String.valueOf(p.getIdProduk()), null);
            buatCellExcel(row, 1, p.getNamaProduk(), null);
            buatCellExcel(row, 2, String.valueOf(p.getTotalProduksi()), null);
            buatCellExcel(row, 3, String.valueOf(p.getTotalDefect()), null);
            buatCellExcel(row, 4, String.format("%.1f%%", p.getRasioDefect()), null);
            buatCellExcel(row, 5, p.getRasioVsTarget() != null ? String.format("%.1f%%", p.getRasioVsTarget()) : "N/A", null);
        }
    }

    private void buatCellExcel(Row row, int col, String value, CellStyle style) {
        org.apache.poi.ss.usermodel.Cell cell = row.createCell(col);
        cell.setCellValue(value);
        if (style != null) cell.setCellStyle(style);
    }

    // =========================================================
    // PDF — iText 7
    // =========================================================

    private void generatePDF(LaporanProduksi laporan, File outputFile) throws IOException {
        try (PdfWriter writer = new PdfWriter(outputFile);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf)) {

            PdfFont fontBold = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
            PdfFont fontRegular = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA);

            doc.add(new Paragraph("Laporan Produksi — SIMP").setFont(fontBold).setFontSize(16).setFontColor(WARNA_HEADER_PDF));

            Table tabelProduksi = new Table(UnitValue.createPercentArray(new float[]{1, 3, 2, 2, 2})).useAllAvailableWidth();
            String[] kolomProduksi = {"ID", "Produk", "Tanggal", "Produksi", "Defect"};
            for (String k : kolomProduksi) tabelProduksi.addHeaderCell(buatCellHeaderPDF(k, fontBold));

            for (LaporanProduksi.BarisTabel baris : laporan.getBarisTabel()) {
                tabelProduksi.addCell(buatCellDataPDF(String.valueOf(baris.getIdProduk()), fontRegular));
                tabelProduksi.addCell(buatCellDataPDF(baris.getNamaProduk(), fontRegular));
                tabelProduksi.addCell(buatCellDataPDF(baris.getTanggalProduksi() != null ? baris.getTanggalProduksi().format(FMT_TANGGAL) : "-", fontRegular));
                tabelProduksi.addCell(buatCellDataPDF(String.valueOf(baris.getJumlahProduksi()), fontRegular));
                tabelProduksi.addCell(buatCellDataPDF(String.valueOf(baris.getJumlahDefect()), fontRegular));
            }
            doc.add(tabelProduksi);
        }
    }

    private com.itextpdf.layout.element.Cell buatCellHeaderPDF(String teks, PdfFont font) {
        return new com.itextpdf.layout.element.Cell()
                .add(new Paragraph(teks).setFont(font).setFontSize(9).setFontColor(WARNA_TEKS_HEADER_PDF))
                .setBackgroundColor(WARNA_HEADER_PDF)
                .setPadding(5);
    }

    private com.itextpdf.layout.element.Cell buatCellDataPDF(String teks, PdfFont font) {
        return new com.itextpdf.layout.element.Cell()
                .add(new Paragraph(teks).setFont(font).setFontSize(9))
                .setPadding(5);
    }
}