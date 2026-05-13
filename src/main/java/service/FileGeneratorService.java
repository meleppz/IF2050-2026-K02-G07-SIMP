package service;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
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

    private static final DateTimeFormatter FMT_TANGGAL = DateTimeFormatter.ofPattern("dd/MM/yyyy");
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
    // EXCEL
    // =========================================================

    private void generateExcel(LaporanProduksi laporan, File outputFile) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(outputFile)) {

            // Sheet 1: Summary
            XSSFSheet sheetPerforma = workbook.createSheet("Summary Performa");
            buatHeaderSheetExcel(workbook, sheetPerforma, laporan, "Rangkuman Performa Produk");
            buatTabelPerformaExcel(workbook, sheetPerforma, laporan);

            // Sheet 2: Detail (Metode ini yang tadi hilang)
            XSSFSheet sheetProduksi = workbook.createSheet("Detail Produksi Harian");
            buatHeaderSheetExcel(workbook, sheetProduksi, laporan, "Detail Transaksi Produksi");
            buatTabelProduksiExcel(workbook, sheetProduksi, laporan);

            for (int i = 0; i < 6; i++) {
                sheetPerforma.autoSizeColumn(i);
                sheetProduksi.autoSizeColumn(i);
            }

            workbook.write(fos);
        }
    }

    private void buatTabelPerformaExcel(XSSFWorkbook wb, XSSFSheet sheet, LaporanProduksi laporan) {
        CellStyle styleHeader = buatStyleHeaderExcel(wb);
        String[] kolom = {"ID Produk", "Nama Produk", "Total Produksi", "Total Defect", "Rasio Defect (%)", "Performa Yield (%)"};
        Row rowHeader = sheet.createRow(4);
        for (int i = 0; i < kolom.length; i++) {
            org.apache.poi.ss.usermodel.Cell c = rowHeader.createCell(i);
            c.setCellValue(kolom[i]);
            c.setCellStyle(styleHeader);
        }

        int rowIdx = 5;
        for (LaporanProduksi.PerformaProduk p : laporan.getPerformaList()) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(p.getIdProduk());
            row.createCell(1).setCellValue(p.getNamaProduk());
            row.createCell(2).setCellValue(p.getTotalProduksi());
            row.createCell(3).setCellValue(p.getTotalDefect());
            row.createCell(4).setCellValue(String.format("%.2f%%", p.getRasioVsTarget()));
            row.createCell(5).setCellValue(String.format("%.2f%%", p.getRasioDefect()));
        }
    }

    // FIX: Menambahkan kembali metode buatTabelProduksiExcel yang hilang
    private void buatTabelProduksiExcel(XSSFWorkbook wb, XSSFSheet sheet, LaporanProduksi laporan) {
        CellStyle styleHeader = buatStyleHeaderExcel(wb);
        String[] kolom = {"ID Produk", "Nama Produk", "Tanggal", "Qty Produksi", "Qty Defect"};
        Row rowHeader = sheet.createRow(4);
        for (int i = 0; i < kolom.length; i++) {
            org.apache.poi.ss.usermodel.Cell c = rowHeader.createCell(i);
            c.setCellValue(kolom[i]);
            c.setCellStyle(styleHeader);
        }

        int rowIdx = 5;
        for (LaporanProduksi.BarisTabel baris : laporan.getBarisTabel()) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(baris.getIdProduk());
            row.createCell(1).setCellValue(baris.getNamaProduk());
            row.createCell(2).setCellValue(baris.getTanggalProduksi() != null ?
                    baris.getTanggalProduksi().format(FMT_TANGGAL) : "-");
            row.createCell(3).setCellValue(baris.getJumlahProduksi());
            row.createCell(4).setCellValue(baris.getJumlahDefect());
        }
    }

    // =========================================================
    // PDF
    // =========================================================

    private void generatePDF(LaporanProduksi laporan, File outputFile) throws IOException {
        try (PdfWriter writer = new PdfWriter(outputFile);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf)) {

            PdfFont fontBold = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
            PdfFont fontRegular = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA);

            doc.add(new Paragraph("LAPORAN ANALISIS PRODUKSI").setFont(fontBold).setFontSize(18).setFontColor(WARNA_HEADER_PDF));
            String periode = laporan.getTglMulai() != null ? laporan.getTglMulai().format(FMT_TANGGAL) + " s/d " + laporan.getTglSelesai().format(FMT_TANGGAL) : "Semua Periode";
            doc.add(new Paragraph("Periode: " + periode).setFont(fontRegular).setFontSize(10));
            doc.add(new Paragraph("\n"));

            doc.add(new Paragraph("RINGKASAN PERFORMA PER PRODUK").setFont(fontBold).setFontSize(12));
            Table tabelSummary = new Table(UnitValue.createPercentArray(new float[]{1, 3, 2, 2, 2, 2})).useAllAvailableWidth();
            String[] headerSum = {"ID", "Produk", "Total", "Defect", "Defect %", "Yield %"};
            for (String h : headerSum) tabelSummary.addHeaderCell(buatCellHeaderPDF(h, fontBold));

            for (LaporanProduksi.PerformaProduk p : laporan.getPerformaList()) {
                tabelSummary.addCell(buatCellDataPDF(String.valueOf(p.getIdProduk()), fontRegular));
                tabelSummary.addCell(buatCellDataPDF(p.getNamaProduk(), fontRegular));
                tabelSummary.addCell(buatCellDataPDF(String.valueOf(p.getTotalProduksi()), fontRegular));
                tabelSummary.addCell(buatCellDataPDF(String.valueOf(p.getTotalDefect()), fontRegular));
                tabelSummary.addCell(buatCellDataPDF(String.format("%.1f%%", p.getRasioVsTarget()), fontRegular));
                tabelSummary.addCell(buatCellDataPDF(String.format("%.1f%%", p.getRasioDefect()), fontRegular));
            }
            doc.add(tabelSummary);
            doc.add(new AreaBreak());

            doc.add(new Paragraph("DETAIL TRANSAKSI PRODUKSI").setFont(fontBold).setFontSize(12));
            Table tabelDetail = new Table(UnitValue.createPercentArray(new float[]{1, 3, 2, 2, 2})).useAllAvailableWidth();
            String[] headerDet = {"ID", "Produk", "Tanggal", "Qty", "Defect"};
            for (String h : headerDet) tabelDetail.addHeaderCell(buatCellHeaderPDF(h, fontBold));

            for (LaporanProduksi.BarisTabel baris : laporan.getBarisTabel()) {
                tabelDetail.addCell(buatCellDataPDF(String.valueOf(baris.getIdProduk()), fontRegular));
                tabelDetail.addCell(buatCellDataPDF(baris.getNamaProduk(), fontRegular));
                tabelDetail.addCell(buatCellDataPDF(baris.getTanggalProduksi() != null ? baris.getTanggalProduksi().format(FMT_TANGGAL) : "-", fontRegular));
                tabelDetail.addCell(buatCellDataPDF(String.valueOf(baris.getJumlahProduksi()), fontRegular));
                tabelDetail.addCell(buatCellDataPDF(String.valueOf(baris.getJumlahDefect()), fontRegular));
            }
            doc.add(tabelDetail);
        }
    }

    private void buatHeaderSheetExcel(XSSFWorkbook wb, XSSFSheet sheet, LaporanProduksi laporan, String judul) {
        CellStyle styleJudul = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short)14);
        styleJudul.setFont(f);
        Row r = sheet.createRow(0);
        org.apache.poi.ss.usermodel.Cell c = r.createCell(0);
        c.setCellValue(judul);
        c.setCellStyle(styleJudul);
        sheet.addMergedRegion(new CellRangeAddress(0,0,0,5));
    }

    private CellStyle buatStyleHeaderExcel(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)0, (byte)229, (byte)160}, new DefaultIndexedColorMap()));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font f = wb.createFont(); f.setBold(true);
        style.setFont(f);
        return style;
    }

    private com.itextpdf.layout.element.Cell buatCellHeaderPDF(String teks, PdfFont font) {
        return new com.itextpdf.layout.element.Cell()
                .add(new Paragraph(teks).setFont(font).setFontSize(9).setFontColor(WARNA_TEKS_HEADER_PDF))
                .setBackgroundColor(WARNA_HEADER_PDF).setPadding(5);
    }

    private com.itextpdf.layout.element.Cell buatCellDataPDF(String teks, PdfFont font) {
        return new com.itextpdf.layout.element.Cell()
                .add(new Paragraph(teks).setFont(font).setFontSize(9)).setPadding(5);
    }
}