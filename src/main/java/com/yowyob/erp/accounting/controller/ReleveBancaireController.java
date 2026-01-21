package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.service.CsvReleveBancaireService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
import com.yowyob.erp.config.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for bank statement CSV import and processing.
 * 
 * @author ALD
 * @date 20.01.26
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
         * Uploads and parses a bank statement CSV file.
         * 
         * @param file            the CSV file to upload
         * @param compte_bancaire the bank account number
         * @return parsed transactions ready for import
         */
        @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
        @Operation(summary = "Upload d'un relevé bancaire CSV", description = "Parse un fichier CSV de relevé bancaire et retourne les transactions détectées")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Fichier uploadé et parsé"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Fichier invalide ou format non reconnu"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Non authentifié"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Accès refusé")
        })
        public ResponseEntity<ApiResponseWrapper<List<Map<String, Object>>>> uploadReleve(
                        @RequestParam("file") MultipartFile file,
                        @RequestParam("compteBancaire") String compte_bancaire) {

                UUID tenant_id = TenantContext.getCurrentTenant();
                log.info("📤 Uploading bank statement CSV for account {} of tenant {}", compte_bancaire, tenant_id);

                List<Map<String, Object>> transactions = releve_service.parseReleveCsv(file, compte_bancaire);

                return ResponseEntity.ok(ApiResponseWrapper.success(
                                transactions,
                                transactions.size() + " transactions détectées dans le relevé"));
        }

        /**
         * Gets the list of uploaded bank statements.
         * 
         * @return list of uploaded statements
         */
        @GetMapping("/list")
        @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'USER')")
        @Operation(summary = "Liste des relevés importés", description = "Retourne la liste des relevés bancaires uploadés pour le tenant")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Liste récupérée"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Non authentifié")
        })
        public ResponseEntity<ApiResponseWrapper<List<Map<String, Object>>>> getListeReleves() {
                UUID tenant_id = TenantContext.getCurrentTenant();
                log.info("📋 Getting bank statements list for tenant {}", tenant_id);

                List<Map<String, Object>> releves = releve_service.getListeReleves(tenant_id);

                return ResponseEntity.ok(ApiResponseWrapper.success(
                                releves,
                                "Liste des relevés récupérée"));
        }

        /**
         * Imports bank statement transactions as accounting entries.
         * 
         * @param releve_id the statement ID to import
         * @return import result with created entries
         */
        @PostMapping("/import/{releveId}")
        @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
        @Operation(summary = "Importer un relevé en écritures", description = "Convertit les transactions d'un relevé en écritures comptables")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Relevé importé avec succès"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Relevé déjà importé ou invalide"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Non authentifié"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Accès refusé"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Relevé non trouvé")
        })
        public ResponseEntity<ApiResponseWrapper<Map<String, Object>>> importerReleve(@PathVariable UUID releveId) {
                UUID tenant_id = TenantContext.getCurrentTenant();
                String user = TenantContext.getCurrentUser();
                log.info("💾 Importing bank statement {} for tenant {} by user {}", releveId, tenant_id, user);

                Map<String, Object> resultat = releve_service.importerReleveEnEcritures(releveId, user);

                return ResponseEntity.ok(ApiResponseWrapper.success(
                                resultat,
                                "Relevé importé avec succès"));
        }
}
