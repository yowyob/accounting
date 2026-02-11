package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.service.CsvReleveBancaireService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reactive Controller for bank statement CSV import and processing.
 */
@RestController
@RequestMapping("/api/comptable/releve")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Relevé Bancaire", description = "Endpoints pour l'import et le traitement des relevés bancaires CSV")
@SecurityRequirement(name = "Bearer Authentication")
public class ReleveBancaireController {

        private final CsvReleveBancaireService releve_service;

        /**
         * Uploads and parses a bank statement CSV file reactively.
         */
        @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
        @Operation(summary = "Upload d'un relevé bancaire CSV", description = "Parse un fichier CSV de relevé bancaire et retourne les transactions détectées")
        public Mono<ResponseEntity<ApiResponseWrapper<List<Map<String, Object>>>>> uploadReleve(
                        @RequestParam("file") MultipartFile file,
                        @RequestParam("compteBancaire") String compte_bancaire) {

                log.info("📤 Uploading bank statement CSV for account {}", compte_bancaire);

                return releve_service.parseReleveCsv(file, compte_bancaire)
                                .map(transactions -> ResponseEntity.ok(ApiResponseWrapper.success(
                                                transactions,
                                                transactions.size() + " transactions détectées dans le relevé")))
                                .contextWrite(ReactiveOrganizationContext.captureFromThreadLocal());
        }

        /**
         * Gets the list of uploaded bank statements reactively.
         */
        @GetMapping("/list")
        @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'USER')")
        @Operation(summary = "Liste des relevés importés", description = "Retourne la liste des relevés bancaires uploadés pour le tenant")
        public Mono<ResponseEntity<ApiResponseWrapper<List<Map<String, Object>>>>> getListeReleves() {
                log.info("📋 Getting bank statements list");

                // Assuming organizationId is handled reactively within the service
                return releve_service.getListeReleves(null)
                                .map(releves -> ResponseEntity.ok(ApiResponseWrapper.success(
                                                releves,
                                                "Liste des relevés récupérée")))
                                .contextWrite(ReactiveOrganizationContext.captureFromThreadLocal());
        }

        /**
         * Imports bank statement transactions as accounting entries reactively.
         */
        @PostMapping("/import/{releveId}")
        @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
        @Operation(summary = "Importer un relevé en écritures", description = "Convertit les transactions d'un relevé en écritures comptables")
        public Mono<ResponseEntity<ApiResponseWrapper<Map<String, Object>>>> importerReleve(
                        @PathVariable UUID releveId) {
                log.info("💾 Importing bank statement {}", releveId);

                // Assuming user and tenant are handled reactively
                return releve_service.importerReleveEnEcritures(releveId, "system")
                                .map(resultat -> ResponseEntity.ok(ApiResponseWrapper.success(
                                                resultat,
                                                "Relevé importé avec succès")))
                                .contextWrite(ReactiveOrganizationContext.captureFromThreadLocal());
        }
}
