package com.yowyob.erp.accounting.controller;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.yowyob.erp.accounting.service.RapportService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
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
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.Map;

/**
 * Reactive Controller for generating and exporting financial reports.
 * Provides endpoints for Balance Sheet and Income Statement in JSON and PDF
 * formats.
 *
 * @author ALD
 * @date 30.09.25
 */
@RestController
@RequestMapping("/api/accounting/rapport")
@RequiredArgsConstructor
@Tag(name = "Accounting Financial Reports", description = "Generation and exportation of financial reports (Balance Sheet, Income Statement, etc.)")
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

        /**
         * Generates a balance sheet in JSON format.
         */
        @Operation(summary = "Generate Balance Sheet")
        @GetMapping("/bilan")
        public Mono<ResponseEntity<ApiResponseWrapper<com.yowyob.erp.accounting.dto.report.BilanDto>>> generateBilan(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date_debut,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date_fin) {

                if (date_debut.isAfter(date_fin)) {
                        return Mono.just(ResponseEntity.badRequest()
                                        .body(ApiResponseWrapper.error("Start date must precede end date")));
                }

                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> rapport_service
                                                .generateBilan(organization_id, date_debut.toString(),
                                                                date_fin.toString()))
                                .map(bilan -> {
                                        log.info("📊 Balance sheet generated between {} and {}", date_debut, date_fin);
                                        return ResponseEntity.ok(ApiResponseWrapper.success(bilan,
                                                        "Balance sheet generated successfully"));
                                })
                                .contextWrite(ReactiveOrganizationContext.captureFromThreadLocal());
        }

        /**
         * Generates an income statement in JSON format.
         */
        @Operation(summary = "Generate Income Statement")
        @GetMapping("/compte-resultat")
        public Mono<ResponseEntity<ApiResponseWrapper<com.yowyob.erp.accounting.dto.report.CompteResultatDto>>> generateCompteResultat(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date_debut,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date_fin) {

                if (date_debut.isAfter(date_fin)) {
                        return Mono.just(ResponseEntity.badRequest()
                                        .body(ApiResponseWrapper.error("Start date must precede end date")));
                }

                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> rapport_service.generateCompteResultat(organization_id,
                                                date_debut.toString(), date_fin.toString()))
                                .map(resultat -> {
                                        log.info("📈 Income statement generated between {} and {}", date_debut,
                                                        date_fin);
                                        return ResponseEntity
                                                        .ok(ApiResponseWrapper.success(resultat,
                                                                        "Income statement generated successfully"));
                                })
                                .contextWrite(ReactiveOrganizationContext.captureFromThreadLocal());
        }

        /**
         * Generates cash flow in JSON format.
         */
        @Operation(summary = "Generate Cash Flow")
        @GetMapping("/flux-tresorerie")
        public Mono<ResponseEntity<ApiResponseWrapper<com.yowyob.erp.accounting.dto.report.CashFlowDto>>> generateCashFlow(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date_debut,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date_fin) {

                if (date_debut.isAfter(date_fin)) {
                        return Mono.just(ResponseEntity.badRequest()
                                        .body(ApiResponseWrapper.error("Start date must precede end date")));
                }

                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> rapport_service.generateCashFlow(organization_id,
                                                date_debut.toString(), date_fin.toString()))
                                .map(cashFlow -> {
                                        log.info("💸 Cash flow generated between {} and {}", date_debut, date_fin);
                                        return ResponseEntity.ok(ApiResponseWrapper.success(cashFlow,
                                                        "Cash flow generated successfully"));
                                })
                                .contextWrite(ReactiveOrganizationContext.captureFromThreadLocal());
        }

        /**
         * Generates executive summary in JSON format.
         */
        @Operation(summary = "Generate Executive Summary")
        @GetMapping("/resume-executif")
        public Mono<ResponseEntity<ApiResponseWrapper<com.yowyob.erp.accounting.dto.report.ExecutiveSummaryDto>>> generateExecutiveSummary(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date_debut,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date_fin) {

                if (date_debut.isAfter(date_fin)) {
                        return Mono.just(ResponseEntity.badRequest()
                                        .body(ApiResponseWrapper.error("Start date must precede end date")));
                }

                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> rapport_service.generateExecutiveSummary(organization_id,
                                                date_debut.toString(), date_fin.toString()))
                                .map(summary -> {
                                        log.info("📊 Executive summary generated between {} and {}", date_debut,
                                                        date_fin);
                                        return ResponseEntity.ok(ApiResponseWrapper.success(summary,
                                                        "Executive summary generated successfully"));
                                })
                                .contextWrite(ReactiveOrganizationContext.captureFromThreadLocal());
        }

        /**
         * Generates the General Ledger (Grand Livre).
         */
        @Operation(summary = "Generate General Ledger")
        @GetMapping("/grand-livre")
        public Mono<ResponseEntity<ApiResponseWrapper<java.util.List<com.yowyob.erp.accounting.dto.GrandLivreDto>>>> generateGrandLivre(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date_debut,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date_fin) {

                if (date_debut.isAfter(date_fin)) {
                        return Mono.just(ResponseEntity.badRequest()
                                        .body(ApiResponseWrapper.error("Start date must precede end date")));
                }

                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> rapport_service.generateGrandLivre(organization_id,
                                                date_debut.toString(), date_fin.toString()))
                                .map(grandLivre -> ResponseEntity.ok(ApiResponseWrapper.success(grandLivre,
                                                "General Ledger generated successfully")))
                                .contextWrite(ReactiveOrganizationContext.captureFromThreadLocal());
        }

        /**
         * Generates the Trial Balance (Balance des Comptes).
         */
        @Operation(summary = "Generate Trial Balance")
        @GetMapping("/balance")
        public Mono<ResponseEntity<ApiResponseWrapper<com.yowyob.erp.accounting.dto.BalanceDesComptesDto>>> generateBalance(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date_debut,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date_fin) {

                if (date_debut.isAfter(date_fin)) {
                        return Mono.just(ResponseEntity.badRequest()
                                        .body(ApiResponseWrapper.error("Start date must precede end date")));
                }

                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> rapport_service.generateBalanceDesComptes(organization_id,
                                                date_debut.toString(), date_fin.toString()))
                                .map(balance -> ResponseEntity.ok(ApiResponseWrapper.success(balance,
                                                "Trial Balance generated successfully")))
                                .contextWrite(ReactiveOrganizationContext.captureFromThreadLocal());
        }

        /**
         * Exports the balance sheet to a PDF file.
         */
        @Operation(summary = "Export Balance Sheet to PDF")
        @GetMapping(value = "/bilan/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
        public Mono<ResponseEntity<byte[]>> exportBilanPDF(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date_debut,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date_fin) {

                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> rapport_service
                                                .generateBilan(organization_id, date_debut.toString(),
                                                                date_fin.toString()))
                                .flatMap(bilan -> Mono.fromCallable(() -> {
                                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                                        Document document = new Document(PageSize.A4);
                                        PdfWriter.getInstance(document, out);
                                        document.open();

                                        Font title_font = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD,
                                                        BaseColor.BLACK);
                                        Font section_font = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);

                                        document.add(new Paragraph("BALANCE SHEET (BILAN)", title_font));
                                        document.add(new Paragraph("Period: " + date_debut + " to " + date_fin));
                                        document.add(new Paragraph(" "));

                                        PdfPTable table = new PdfPTable(2);
                                        table.setWidthPercentage(100);
                                        table.setWidths(new int[] { 2, 2 });

                                        table.addCell(new PdfPCell(new Phrase("Section", section_font)));
                                        table.addCell(new PdfPCell(new Phrase("Amount (FCFA)", section_font)));

                                        java.math.BigDecimal totalActifPdf = bilan.getActifs().stream().map(
                                                        com.yowyob.erp.accounting.dto.report.ReportItemDto::getSolde)
                                                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                                        java.math.BigDecimal totalPassifPdf = bilan.getPassifs().stream().map(
                                                        com.yowyob.erp.accounting.dto.report.ReportItemDto::getSolde)
                                                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

                                        table.addCell("Total Assets");
                                        table.addCell(String.valueOf(totalActifPdf));

                                        table.addCell("Total Liabilities");
                                        table.addCell(String.valueOf(totalPassifPdf));

                                        document.add(table);
                                        document.close();

                                        HttpHeaders headers = new HttpHeaders();
                                        headers.add("Content-Disposition",
                                                        "attachment; filename=bilan_" + date_debut + "_to_" + date_fin
                                                                        + ".pdf");

                                        return ResponseEntity.ok()
                                                        .headers(headers)
                                                        .contentType(MediaType.APPLICATION_PDF)
                                                        .body(out.toByteArray());
                                }).subscribeOn(Schedulers.boundedElastic())
                                                .doOnError(e -> log.error("Error generating PDF for Balance Sheet: {}",
                                                                e.getMessage())))
                                .contextWrite(ReactiveOrganizationContext.captureFromThreadLocal());
        }

        /**
         * Exports the income statement to a PDF file.
         */
        @Operation(summary = "Export Income Statement to PDF")
        @GetMapping(value = "/compte-resultat/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
        public Mono<ResponseEntity<byte[]>> exportCompteResultatPDF(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date_debut,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date_fin) {

                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> rapport_service.generateCompteResultat(organization_id,
                                                date_debut.toString(), date_fin.toString()))
                                .flatMap(compte_resultat -> Mono.fromCallable(() -> {
                                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                                        Document document = new Document(PageSize.A4);
                                        PdfWriter.getInstance(document, out);
                                        document.open();

                                        Font title_font = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD,
                                                        BaseColor.BLACK);
                                        Font section_font = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);

                                        document.add(new Paragraph("INCOME STATEMENT (COMPTE DE RESULTAT)",
                                                        title_font));
                                        document.add(new Paragraph("Period: " + date_debut + " to " + date_fin));
                                        document.add(new Paragraph(" "));

                                        PdfPTable table = new PdfPTable(2);
                                        table.setWidthPercentage(100);
                                        table.setWidths(new int[] { 2, 2 });

                                        table.addCell(new PdfPCell(new Phrase("Section", section_font)));
                                        table.addCell(new PdfPCell(new Phrase("Amount (FCFA)", section_font)));

                                        java.math.BigDecimal totalProduitsPdf = compte_resultat.getProduits().stream()
                                                        .map(com.yowyob.erp.accounting.dto.report.ReportItemDto::getSolde)
                                                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                                        java.math.BigDecimal totalChargesPdf = compte_resultat.getCharges().stream()
                                                        .map(com.yowyob.erp.accounting.dto.report.ReportItemDto::getSolde)
                                                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

                                        table.addCell("Total Products");
                                        table.addCell(String.valueOf(totalProduitsPdf));

                                        table.addCell("Total Expenses");
                                        table.addCell(String.valueOf(totalChargesPdf));

                                        table.addCell("Net Result");
                                        table.addCell(String.valueOf(totalProduitsPdf.subtract(totalChargesPdf)));

                                        document.add(table);
                                        document.close();

                                        HttpHeaders headers = new HttpHeaders();
                                        headers.add("Content-Disposition",
                                                        "attachment; filename=compte_resultat_" + date_debut + "_to_"
                                                                        + date_fin + ".pdf");

                                        return ResponseEntity.ok()
                                                        .headers(headers)
                                                        .contentType(MediaType.APPLICATION_PDF)
                                                        .body(out.toByteArray());
                                }).subscribeOn(Schedulers.boundedElastic())
                                                .doOnError(e -> log.error(
                                                                "Error generating PDF for Income Statement: {}",
                                                                e.getMessage())))
                                .contextWrite(ReactiveOrganizationContext.captureFromThreadLocal());
        }

        /**
         * Exports the General Ledger to a PDF file.
         */
        @Operation(summary = "Export General Ledger to PDF")
        @GetMapping(value = "/grand-livre/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
        public Mono<ResponseEntity<byte[]>> exportGrandLivrePDF(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date_debut,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date_fin) {

                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> rapport_service.generateGrandLivre(organization_id,
                                                date_debut.toString(), date_fin.toString()))
                                .flatMap(grandLivre -> Mono.fromCallable(() -> {
                                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                                        Document document = new Document(PageSize.A4.rotate()); // Landscape for Grand
                                                                                                // Livre
                                        PdfWriter.getInstance(document, out);
                                        document.open();

                                        Font title_font = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD,
                                                        BaseColor.BLACK);
                                        Font section_font = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
                                        Font text_font = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);

                                        document.add(new Paragraph("GENERAL LEDGER (GRAND LIVRE)", title_font));
                                        document.add(new Paragraph("Period: " + date_debut + " to " + date_fin));
                                        document.add(new Paragraph(" "));

                                        for (com.yowyob.erp.accounting.dto.GrandLivreDto compte : grandLivre) {
                                                document.add(new Paragraph(
                                                                "Account: " + compte.getNoCompte() + " - "
                                                                                + compte.getLibelleCompte(),
                                                                section_font));
                                                document.add(new Paragraph(
                                                                "Opening Balance: " + compte.getSoldeOuverture(),
                                                                text_font));

                                                PdfPTable table = new PdfPTable(6);
                                                table.setWidthPercentage(100);
                                                table.setWidths(new int[] { 2, 4, 3, 2, 2, 2 });
                                                table.setSpacingBefore(5f);
                                                table.setSpacingAfter(10f);

                                                table.addCell(new PdfPCell(new Phrase("Date", section_font)));
                                                table.addCell(new PdfPCell(new Phrase("Label", section_font)));
                                                table.addCell(new PdfPCell(new Phrase("Journal", section_font)));
                                                table.addCell(new PdfPCell(new Phrase("Debit", section_font)));
                                                table.addCell(new PdfPCell(new Phrase("Credit", section_font)));
                                                table.addCell(new PdfPCell(new Phrase("Ref", section_font)));

                                                for (com.yowyob.erp.accounting.dto.GrandLivreDto.LigneGrandLivreDto ligne : compte
                                                                .getLignes()) {
                                                        table.addCell(new Phrase(
                                                                        ligne.getDate().toLocalDate().toString(),
                                                                        text_font));
                                                        table.addCell(new Phrase(ligne.getLibelle(), text_font));
                                                        table.addCell(new Phrase(
                                                                        ligne.getJournal() != null ? ligne.getJournal()
                                                                                        : "",
                                                                        text_font));
                                                        table.addCell(new Phrase(String.valueOf(ligne.getDebit()),
                                                                        text_font));
                                                        table.addCell(new Phrase(String.valueOf(ligne.getCredit()),
                                                                        text_font));
                                                        table.addCell(new Phrase(
                                                                        ligne.getReference() != null
                                                                                        ? ligne.getReference()
                                                                                        : "",
                                                                        text_font));
                                                }
                                                document.add(table);
                                                document.add(new Paragraph(
                                                                "Closing Balance: " + compte.getSoldeCloture()
                                                                                + " (Debit: " + compte.getTotalDebit()
                                                                                + ", Credit: " + compte.getTotalCredit()
                                                                                + ")",
                                                                section_font));
                                                document.add(new Paragraph(
                                                                "--------------------------------------------------"));
                                        }

                                        document.close();

                                        HttpHeaders headers = new HttpHeaders();
                                        headers.add("Content-Disposition",
                                                        "attachment; filename=grand_livre_" + date_debut + "_to_"
                                                                        + date_fin + ".pdf");

                                        return ResponseEntity.ok()
                                                        .headers(headers)
                                                        .contentType(MediaType.APPLICATION_PDF)
                                                        .body(out.toByteArray());
                                }).subscribeOn(Schedulers.boundedElastic())
                                                .doOnError(e -> log.error(
                                                                "Error generating PDF for General Ledger: {}",
                                                                e.getMessage())))
                                .contextWrite(ReactiveOrganizationContext.captureFromThreadLocal());
        }

        /**
         * Exports the Trial Balance to a PDF file.
         */
        @Operation(summary = "Export Trial Balance to PDF")
        @GetMapping(value = "/balance/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
        public Mono<ResponseEntity<byte[]>> exportBalancePDF(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date_debut,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date_fin) {

                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> rapport_service.generateBalanceDesComptes(organization_id,
                                                date_debut.toString(), date_fin.toString()))
                                .flatMap(balance -> Mono.fromCallable(() -> {
                                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                                        Document document = new Document(PageSize.A4);
                                        PdfWriter.getInstance(document, out);
                                        document.open();

                                        Font title_font = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD,
                                                        BaseColor.BLACK);
                                        Font section_font = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
                                        Font text_font = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);

                                        document.add(new Paragraph("TRIAL BALANCE (BALANCE DES COMPTES)", title_font));
                                        document.add(new Paragraph("Period: " + date_debut + " to " + date_fin));
                                        document.add(new Paragraph(" "));

                                        PdfPTable table = new PdfPTable(5);
                                        table.setWidthPercentage(100);
                                        table.setWidths(new int[] { 2, 4, 3, 3, 3 });

                                        table.addCell(new PdfPCell(new Phrase("Account", section_font)));
                                        table.addCell(new PdfPCell(new Phrase("Label", section_font)));
                                        table.addCell(new PdfPCell(new Phrase("Opening Bal.", section_font)));
                                        table.addCell(new PdfPCell(new Phrase("Movements", section_font)));
                                        table.addCell(new PdfPCell(new Phrase("Closing Bal.", section_font)));

                                        for (com.yowyob.erp.accounting.dto.BalanceDesComptesDto.LigneBalanceDto ligne : balance
                                                        .getLignes()) {
                                                table.addCell(new Phrase(ligne.getNoCompte(), text_font));
                                                table.addCell(new Phrase(ligne.getLibelle(), text_font));
                                                table.addCell(new Phrase(
                                                                "D:" + ligne.getSoldeOuvertureDebit() + " C:"
                                                                                + ligne.getSoldeOuvertureCredit(),
                                                                text_font));
                                                table.addCell(new Phrase(
                                                                "D:" + ligne.getMouvementDebit() + " C:"
                                                                                + ligne.getMouvementCredit(),
                                                                text_font));
                                                table.addCell(new Phrase(
                                                                "D:" + ligne.getSoldeClotureDebit() + " C:"
                                                                                + ligne.getSoldeClotureCredit(),
                                                                text_font));
                                        }

                                        document.add(table);
                                        document.close();

                                        HttpHeaders headers = new HttpHeaders();
                                        headers.add("Content-Disposition",
                                                        "attachment; filename=balance_" + date_debut + "_to_" + date_fin
                                                                        + ".pdf");

                                        return ResponseEntity.ok()
                                                        .headers(headers)
                                                        .contentType(MediaType.APPLICATION_PDF)
                                                        .body(out.toByteArray());
                                }).subscribeOn(Schedulers.boundedElastic())
                                                .doOnError(e -> log.error(
                                                                "Error generating PDF for Trial Balance: {}",
                                                                e.getMessage())))
                                .contextWrite(ReactiveOrganizationContext.captureFromThreadLocal());
        }

        /**
         * Balance âgée : créances clients (41x) et dettes fournisseurs (40x)
         * regroupées par tranches d'ancienneté (0-30j, 31-60j, 61-90j, >90j).
         */
        @Operation(summary = "Balance âgée créances/dettes",
                description = "Classement des soldes clients et fournisseurs par ancienneté")
        @GetMapping("/balance-agee")
        public Mono<ResponseEntity<ApiResponseWrapper<com.yowyob.erp.accounting.dto.report.BalanceAgeeDto>>> generateBalanceAgee(
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date_reference) {

                return ReactiveOrganizationContext.getOrganizationId()
                        .flatMap(organization_id -> rapport_service.generateBalanceAgee(organization_id,
                                date_reference != null ? date_reference.toString() : null))
                        .map(ba -> ResponseEntity.ok(ApiResponseWrapper.success(ba, "Balance âgée générée")))
                        .contextWrite(ReactiveOrganizationContext.captureFromThreadLocal());
        }

        /**
         * Export PDF de la balance âgée.
         */
        @Operation(summary = "Export Balance Âgée to PDF")
        @GetMapping(value = "/balance-agee/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
        public Mono<ResponseEntity<byte[]>> exportBalanceAgeePDF(
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date_reference) {

                return ReactiveOrganizationContext.getOrganizationId()
                        .flatMap(organization_id -> rapport_service.generateBalanceAgee(organization_id,
                                date_reference != null ? date_reference.toString() : null))
                        .flatMap(ba -> Mono.fromCallable(() -> {
                                ByteArrayOutputStream out = new ByteArrayOutputStream();
                                Document document = new Document(PageSize.A4.rotate());
                                PdfWriter.getInstance(document, out);
                                document.open();

                                Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
                                Font headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
                                Font cellFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);

                                document.add(new Paragraph("BALANCE ÂGÉE — " + ba.getDateReference(), titleFont));
                                document.add(new Paragraph(" "));

                                for (String section : new String[]{"CLIENTS (41x)", "FOURNISSEURS (40x)"}) {
                                        document.add(new Paragraph(section, headerFont));
                                        PdfPTable table = new PdfPTable(7);
                                        table.setWidthPercentage(100);
                                        table.setWidths(new int[]{3, 5, 2, 2, 2, 2, 2});
                                        for (String h : new String[]{"Compte", "Libellé", "Total", "0-30j", "31-60j", "61-90j", ">90j"})
                                                table.addCell(new PdfPCell(new Phrase(h, headerFont)));

                                        var lignes = section.startsWith("CLIENTS") ? ba.getClients() : ba.getFournisseurs();
                                        for (var l : lignes) {
                                                table.addCell(new Phrase(l.getNoCompte(), cellFont));
                                                table.addCell(new Phrase(l.getLibelle(), cellFont));
                                                table.addCell(new Phrase(l.getSoldeTotal().toPlainString(), cellFont));
                                                table.addCell(new Phrase(l.getTranche0_30().toPlainString(), cellFont));
                                                table.addCell(new Phrase(l.getTranche31_60().toPlainString(), cellFont));
                                                table.addCell(new Phrase(l.getTranche61_90().toPlainString(), cellFont));
                                                table.addCell(new Phrase(l.getTranches90Plus().toPlainString(), cellFont));
                                        }
                                        document.add(table);
                                        document.add(new Paragraph(" "));
                                }

                                document.close();
                                HttpHeaders headers = new HttpHeaders();
                                headers.add("Content-Disposition", "attachment; filename=balance_agee_" + ba.getDateReference() + ".pdf");
                                return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF).body(out.toByteArray());
                        }).subscribeOn(Schedulers.boundedElastic()))
                        .contextWrite(ReactiveOrganizationContext.captureFromThreadLocal());
        }
}
