package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.entity.FactureComptable;
import com.yowyob.erp.accounting.service.FactureProcessingService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
    public ResponseEntity<ApiResponseWrapper<FactureComptable>> upload(@RequestPart("file") MultipartFile file) {
        log.info("📥 Uploading invoice file for analysis: {}", file.getOriginalFilename());
        FactureComptable facture = facture_service.extractFactureData(file);
        return ResponseEntity.ok(ApiResponseWrapper.success(facture, "Invoice successfully analyzed"));
    }
}
