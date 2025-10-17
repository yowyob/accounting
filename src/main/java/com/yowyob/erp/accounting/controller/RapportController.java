package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.service.RapportService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
import com.yowyob.erp.config.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

@RestController
@RequestMapping("/api/accounting/rapport")
@RequiredArgsConstructor
@Tag(name = "Rapports Financiers", description = "Génération et exportation des rapports financiers (Bilan, Compte de Résultat, etc.)")
@SecurityRequirement(name = "BasicAuth")
@Slf4j
public class RapportController {

    private final RapportService rapportService;

    /* ==============================================================
     *  RAPPORTS JSON
     * ============================================================== */

    @Operation(summary = "Générer le Bilan Comptable")
    @GetMapping("/bilan")
    public ResponseEntity<ApiResponseWrapper<Map<String, Object>>> generateBilan(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {

        if (dateDebut.isAfter(dateFin)) {
            return ResponseEntity.badRequest().body(ApiResponseWrapper.error("La date de début doit précéder la date de fin"));
        }
        UUID tenantId = TenantContext.getCurrentTenant();
        Map<String, Object> bilan = rapportService.generateBilan(tenantId, dateDebut.toString(), dateFin.toString());
        log.info("📊 Bilan généré pour tenant {} entre {} et {}", tenantId, dateDebut, dateFin);
        return ResponseEntity.ok(ApiResponseWrapper.success(bilan, "Bilan généré avec succès"));
    }

    @Operation(summary = "Générer le Compte de Résultat")
    @GetMapping("/compte-resultat")
    public ResponseEntity<ApiResponseWrapper<Map<String, Object>>> generateCompteResultat(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {

        if (dateDebut.isAfter(dateFin)) {
            return ResponseEntity.badRequest().body(ApiResponseWrapper.error("La date de début doit précéder la date de fin"));
        }
        UUID tenantId = TenantContext.getCurrentTenant();
        Map<String, Object> resultat = rapportService.generateCompteResultat(tenantId, dateDebut.toString(), dateFin.toString());
        log.info("📈 Compte de résultat généré pour tenant {} entre {} et {}", tenantId, dateDebut, dateFin);
        return ResponseEntity.ok(ApiResponseWrapper.success(resultat, "Compte de résultat généré avec succès"));
    }

    /* ==============================================================
     *  EXPORT PDF
     * ============================================================== */

    @Operation(summary = "Exporter le Bilan Comptable en PDF")
    @GetMapping(value = "/bilan/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportBilanPDF(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) throws Exception {

        UUID tenantId = TenantContext.getCurrentTenant();
        Map<String, Object> bilan = rapportService.generateBilan(tenantId, dateDebut.toString(), dateFin.toString());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.BLACK);
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
        Font textFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);

        document.add(new Paragraph("BILAN COMPTABLE", titleFont));
        document.add(new Paragraph("Tenant: " + tenantId, textFont));
        document.add(new Paragraph("Période: " + dateDebut + " → " + dateFin));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new int[]{2, 2});

        table.addCell(new PdfPCell(new Phrase("Rubrique", sectionFont)));
        table.addCell(new PdfPCell(new Phrase("Montant (FCFA)", sectionFont)));

        table.addCell("Total Actif");
        table.addCell(String.valueOf(bilan.getOrDefault("totalActif", 0)));

        table.addCell("Total Passif");
        table.addCell(String.valueOf(bilan.getOrDefault("totalPassif", 0)));

        document.add(table);
        document.close();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=bilan_" + dateDebut + "_to_" + dateFin + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(out.toByteArray());
    }

    @Operation(summary = "Exporter le Compte de Résultat en PDF")
    @GetMapping(value = "/compte-resultat/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportCompteResultatPDF(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) throws Exception {

        UUID tenantId = TenantContext.getCurrentTenant();
        Map<String, Object> compteResultat = rapportService.generateCompteResultat(tenantId, dateDebut.toString(), dateFin.toString());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.BLACK);
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
        Font textFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);

        document.add(new Paragraph("COMPTE DE RÉSULTAT", titleFont));
        document.add(new Paragraph("Tenant: " + tenantId, textFont));
        document.add(new Paragraph("Période: " + dateDebut + " → " + dateFin));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new int[]{2, 2});

        table.addCell(new PdfPCell(new Phrase("Rubrique", sectionFont)));
        table.addCell(new PdfPCell(new Phrase("Montant (FCFA)", sectionFont)));

        table.addCell("Total Produits");
        table.addCell(String.valueOf(compteResultat.getOrDefault("totalProduits", 0)));

        table.addCell("Total Charges");
        table.addCell(String.valueOf(compteResultat.getOrDefault("totalCharges", 0)));

        table.addCell("Résultat Net");
        table.addCell(String.valueOf(compteResultat.getOrDefault("resultat", 0)));

        document.add(table);
        document.close();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=compte_resultat_" + dateDebut + "_to_" + dateFin + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(out.toByteArray());
    }
}
