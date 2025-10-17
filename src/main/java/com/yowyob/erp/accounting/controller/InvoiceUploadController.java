package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.entity.FactureComptable;
import com.yowyob.erp.accounting.service.FactureProcessingService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/accounting/invoices")
@RequiredArgsConstructor
@Slf4j
public class InvoiceUploadController {

    private final FactureProcessingService factureProcessingService;

    @PostMapping(value = "/upload", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponseWrapper<FactureComptable>> upload(@RequestPart("file") MultipartFile file) {
        log.info("📥 Upload facture: {}", file.getOriginalFilename());
        FactureComptable facture = factureProcessingService.extractFactureData(file);
        return ResponseEntity.ok(ApiResponseWrapper.success(facture, "Facture analysée avec succès"));
    }
}
