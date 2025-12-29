package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.service.RapportService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
import com.yowyob.erp.config.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * Controller for generating and exporting financial reports.
 * Provides endpoints for Balance Sheet and Income Statement in JSON and PDF
 * formats.
 * 
 * @author ALD
 * @date 30.09.25
 */
@RestController
@RequestMapping("/api/accounting/rapport")
@RequiredArgsConstructor
@Tag(name = "Financial Reports", description = "Generation and exportation of financial reports (Balance Sheet, Income Statement, etc.)")
@SecurityRequirement(name = "BasicAuth")
@Slf4j
public class RapportController {

    private final RapportService rapport_service;

    /**
     * Generates a balance sheet in JSON format.
     * 
     * @param date_debut start date
     * @param date_fin   end date
     * @return response wrapper containing balance sheet data
     */
    @Operation(summary = "Generate Balance Sheet")
    @GetMapping("/bilan")
    public ResponseEntity<ApiResponseWrapper<Map<String, Object>>> generateBilan(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date_debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date_fin) {

        if (date_debut.isAfter(date_fin)) {
            return ResponseEntity.badRequest().body(ApiResponseWrapper.error("Start date must precede end date"));
        }
        UUID tenant_id = TenantContext.getCurrentTenant();
        Map<String, Object> bilan = rapport_service.generateBilan(tenant_id, date_debut.toString(),
                date_fin.toString());
        log.info("📊 Balance sheet generated for tenant {} between {} and {}", tenant_id, date_debut, date_fin);
        return ResponseEntity.ok(ApiResponseWrapper.success(bilan, "Balance sheet generated successfully"));
    }

    /**
     * Generates an income statement in JSON format.
     * 
     * @param date_debut start date
     * @param date_fin   end date
     * @return response wrapper containing income statement data
     */
    @Operation(summary = "Generate Income Statement")
    @GetMapping("/compte-resultat")
    public ResponseEntity<ApiResponseWrapper<Map<String, Object>>> generateCompteResultat(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date_debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date_fin) {

        if (date_debut.isAfter(date_fin)) {
            return ResponseEntity.badRequest().body(ApiResponseWrapper.error("Start date must precede end date"));
        }
        UUID tenant_id = TenantContext.getCurrentTenant();
        Map<String, Object> resultat = rapport_service.generateCompteResultat(tenant_id, date_debut.toString(),
                date_fin.toString());
        log.info("📈 Income statement generated for tenant {} between {} and {}", tenant_id, date_debut, date_fin);
        return ResponseEntity.ok(ApiResponseWrapper.success(resultat, "Income statement generated successfully"));
    }

    /**
     * Exports the balance sheet to a PDF file.
     * 
     * @param date_debut start date
     * @param date_fin   end date
     * @return PDF byte array
     * @throws Exception if PDF generation fails
     */
    @Operation(summary = "Export Balance Sheet to PDF")
    @GetMapping(value = "/bilan/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportBilanPDF(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date_debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date_fin) throws Exception {

        UUID tenant_id = TenantContext.getCurrentTenant();
        Map<String, Object> bilan = rapport_service.generateBilan(tenant_id, date_debut.toString(),
                date_fin.toString());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, out);
        document.open();

        Font title_font = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.BLACK);
        Font section_font = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
        Font text_font = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);

        document.add(new Paragraph("BALANCE SHEET", title_font));
        document.add(new Paragraph("Tenant: " + tenant_id, text_font));
        document.add(new Paragraph("Period: " + date_debut + " → " + date_fin));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new int[] { 2, 2 });

        table.addCell(new PdfPCell(new Phrase("Section", section_font)));
        table.addCell(new PdfPCell(new Phrase("Amount (FCFA)", section_font)));

        table.addCell("Total Assets");
        table.addCell(String.valueOf(bilan.getOrDefault("totalActif", 0)));

        table.addCell("Total Liabilities");
        table.addCell(String.valueOf(bilan.getOrDefault("totalPassif", 0)));

        document.add(table);
        document.close();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=bilan_" + date_debut + "_to_" + date_fin + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(out.toByteArray());
    }

    /**
     * Exports the income statement to a PDF file.
     * 
     * @param date_debut start date
     * @param date_fin   end date
     * @return PDF byte array
     * @throws Exception if PDF generation fails
     */
    @Operation(summary = "Export Income Statement to PDF")
    @GetMapping(value = "/compte-resultat/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportCompteResultatPDF(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date_debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date_fin) throws Exception {

        UUID tenant_id = TenantContext.getCurrentTenant();
        Map<String, Object> compte_resultat = rapport_service.generateCompteResultat(tenant_id, date_debut.toString(),
                date_fin.toString());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, out);
        document.open();

        Font title_font = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.BLACK);
        Font section_font = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
        Font text_font = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);

        document.add(new Paragraph("INCOME STATEMENT", title_font));
        document.add(new Paragraph("Tenant: " + tenant_id, text_font));
        document.add(new Paragraph("Period: " + date_debut + " → " + date_fin));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new int[] { 2, 2 });

        table.addCell(new PdfPCell(new Phrase("Section", section_font)));
        table.addCell(new PdfPCell(new Phrase("Amount (FCFA)", section_font)));

        table.addCell("Total Products");
        table.addCell(String.valueOf(compte_resultat.getOrDefault("totalProduits", 0)));

        table.addCell("Total Expenses");
        table.addCell(String.valueOf(compte_resultat.getOrDefault("totalCharges", 0)));

        table.addCell("Net Result");
        table.addCell(String.valueOf(compte_resultat.getOrDefault("resultatNet", 0)));

        document.add(table);
        document.close();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition",
                "attachment; filename=compte_resultat_" + date_debut + "_to_" + date_fin + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(out.toByteArray());
    }
}
