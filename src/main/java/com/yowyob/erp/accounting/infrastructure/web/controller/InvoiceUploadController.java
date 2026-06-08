package com.yowyob.erp.accounting.infrastructure.web.controller;

import com.yowyob.erp.accounting.domain.model.FactureComptable;
import com.yowyob.erp.accounting.application.service.FactureProcessingService;
import com.yowyob.erp.accounting.infrastructure.web.dto.EcritureComptableDto;
import com.yowyob.erp.shared.infrastructure.dto.ApiResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

/**
 * Controller for handling invoice document uploads and OCR processing.
 * Provides endpoints to extract structured accounting data from raw invoice
 * images/PDFs.
 * 
 * @author ALD
 * @date 30.09.25
 */
@RestController
@RequestMapping("/api/accounting/invoices")
@RequiredArgsConstructor
@Tag(name = "Accounting Invoice Upload", description = "Endpoints for uploading and processing invoice documents via OCR")
@Slf4j
public class InvoiceUploadController {

    private final FactureProcessingService facture_service;

    /**
     * Uploads an invoice file (PDF or image) for analysis.
     * Extracts fields like amount, VAT, date, and vendor details using OCR.
     * 
     * @param file the uploaded multipart file
     * @return a wrapped response containing the extracted invoice data
     */
    @Operation(summary = "Upload and analyze an invoice document")
    @PostMapping(value = "/upload", consumes = { "multipart/form-data" })
    public Mono<ResponseEntity<ApiResponseWrapper<FactureComptable>>> upload(
            @RequestPart("file") Mono<FilePart> fileMono) {
        return fileMono.flatMap(file -> {
            log.info("📥 Uploading invoice file for analysis: {}", file.filename());
            return facture_service.extractFactureData(file)
                    .map(facture -> ResponseEntity
                            .ok(ApiResponseWrapper.success(facture, "Invoice successfully analyzed")));
        });
    }

    /**
     * Uploads an invoice file (PDF or image), extracts its data via OCR and
     * directly records it: the matching accounting journal is resolved and a
     * balanced OHADA accounting entry is generated and persisted.
     *
     * @param fileMono the uploaded multipart file
     * @return a wrapped response containing the generated accounting entry
     */
    @Operation(summary = "Upload, analyze and record an invoice (generates the accounting entry)")
    @PostMapping(value = "/upload/comptabiliser", consumes = { "multipart/form-data" })
    public Mono<ResponseEntity<ApiResponseWrapper<EcritureComptableDto>>> uploadAndComptabiliser(
            @RequestPart("file") Mono<FilePart> fileMono) {
        return fileMono.flatMap(file -> {
            log.info("📥 Uploading invoice file for analysis + comptabilisation: {}", file.filename());
            return facture_service.extractAndComptabiliser(file)
                    .map(ecriture -> ResponseEntity.ok(ApiResponseWrapper.success(
                            ecriture, "Invoice analyzed and recorded — accounting entry generated")));
        });
    }
}
