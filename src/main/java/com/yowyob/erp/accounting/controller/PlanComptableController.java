package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.dto.ImportResult;
import com.yowyob.erp.accounting.dto.PlanComptableDto;
import com.yowyob.erp.accounting.service.PlanComptableService;
import com.yowyob.erp.accounting.service.PlanComptableImportService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
import com.yowyob.erp.common.exception.BusinessException;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Reactive Controller for managing the accounting plan (Plan Comptable).
 */
@RestController
@RequestMapping("/api/accounting/plan-comptable")
@RequiredArgsConstructor
@Tag(name = "Accounting Plan Comptable", description = "Accounting plan management: creation, update, deactivation, and search.")
@SecurityRequirement(name = "BasicAuth")
@Slf4j
public class PlanComptableController {

        private final PlanComptableService plan_service;
        private final PlanComptableImportService planImportService;

        /**
         * Initializes the accounting plan for a specific organization.
         */
        @Operation(summary = "Initialize accounting plan", description = "Creates a default set of accounts for the given organization.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Plan initialized successfully"),
                        @ApiResponse(responseCode = "400", description = "Validation error or organization already initialized")
        })
        @PostMapping("/admin/organizations/{organizationId}/plan-comptable/init-ohada-2025")
        public Mono<ResponseEntity<ApiResponseWrapper<String>>> initPlanComptable(
                        @PathVariable(name = "organizationId") UUID organization_id) {
                return plan_service.initializePlanComptableForOrganization(organization_id)
                                .then(Mono.just(ResponseEntity.ok(
                                                ApiResponseWrapper.success(
                                                                "OHADA 2025 accounting plan initialized for organization "
                                                                                + organization_id))));
        }

        /**
         * Creates a new accounting account for the current organization.
         */
        @Operation(summary = "Create an accounting account", description = "Creates a new account for the current organization.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Account created successfully", content = @Content(schema = @Schema(implementation = PlanComptableDto.class))),
                        @ApiResponse(responseCode = "400", description = "Validation error or account already exists")
        })
        @PostMapping
        public Mono<ResponseEntity<ApiResponseWrapper<PlanComptableDto>>> createPlanComptable(
                        @Valid @RequestBody PlanComptableDto dto) {
                return plan_service.createAccount(dto)
                                .map(created -> ResponseEntity.status(HttpStatus.CREATED)
                                                .body(ApiResponseWrapper.success(created,
                                                                "Accounting account created successfully")))
                                .onErrorResume(e -> {
                                        log.error("Error creating account: {}", e.getMessage());
                                        return Mono.error(new BusinessException(
                                                        "Error during account creation: " + e.getMessage()));
                                });
        }

        /**
         * Retrieves an account by its ID.
         */
        @Operation(summary = "Get an accounting account by ID")
        @GetMapping("/{id}")
        public Mono<ResponseEntity<ApiResponseWrapper<PlanComptableDto>>> getAccountById(@PathVariable UUID id) {
                return plan_service.getAccountById(id)
                                .map(dto -> ResponseEntity
                                                .ok(ApiResponseWrapper.success(dto, "Account retrieved successfully")))
                                .onErrorResume(e -> {
                                        if (e instanceof ResourceNotFoundException) {
                                                return Mono.error(e);
                                        }
                                        return Mono.error(new ResourceNotFoundException("Accounting account",
                                                        id.toString()));
                                });
        }

        /**
         * Lists all accounts for the current organization.
         */
        @Operation(summary = "List all accounting accounts")
        @GetMapping
        public Mono<ResponseEntity<ApiResponseWrapper<List<PlanComptableDto>>>> getAllPlanComptables() {
                return plan_service.getAllAccounts()
                                .map(accounts -> ResponseEntity
                                                .ok(ApiResponseWrapper.success(accounts,
                                                                "Accounts retrieved successfully")));
        }

        /**
         * Lists all active accounts for the current organization.
         */
        @Operation(summary = "List all active accounts")
        @GetMapping("/actifs")
        public Mono<ResponseEntity<ApiResponseWrapper<List<PlanComptableDto>>>> getActifPlanComptables() {
                return plan_service.getAllActiveAccounts()
                                .map(accounts -> ResponseEntity
                                                .ok(ApiResponseWrapper.success(accounts,
                                                                "Active accounts retrieved successfully")));
        }

        /**
         * Lists accounts by prefix.
         */
        @Operation(summary = "List accounts by prefix")
        @GetMapping("/prefix/{prefix}")
        public Mono<ResponseEntity<ApiResponseWrapper<List<PlanComptableDto>>>> getPlanComptablesByPrefix(
                        @PathVariable String prefix) {
                if (prefix == null || prefix.trim().isEmpty()) {
                        return Mono.just(ResponseEntity.badRequest()
                                        .body(ApiResponseWrapper.error("Prefix cannot be empty")));
                }
                return plan_service.getAccountsByPrefix(prefix)
                                .map(accounts -> ResponseEntity
                                                .ok(ApiResponseWrapper.success(accounts,
                                                                "Accounts retrieved successfully")));
        }

        /**
         * Lists accounts by class.
         */
        @Operation(summary = "List accounts by class")
        @GetMapping("/classe/{classe}")
        public Mono<ResponseEntity<ApiResponseWrapper<List<PlanComptableDto>>>> getPlanComptablesByClasse(
                        @PathVariable Integer classe) {
                if (classe < 1 || classe > 7) {
                        return Mono
                                        .just(ResponseEntity.badRequest().body(
                                                        ApiResponseWrapper.error("Class must be between 1 and 7")));
                }
                return plan_service.getAccountsByClass(classe)
                                .map(accounts -> ResponseEntity
                                                .ok(ApiResponseWrapper.success(accounts,
                                                                "Accounts for class " + classe + " retrieved")));
        }

        /**
         * Updates an existing account.
         */
        @Operation(summary = "Update an accounting account")
        @PutMapping("/{id}")
        public Mono<ResponseEntity<ApiResponseWrapper<PlanComptableDto>>> updatePlanComptable(
                        @PathVariable UUID id,
                        @Valid @RequestBody PlanComptableDto dto) {
                return plan_service.updateAccount(id, dto)
                                .map(updated -> ResponseEntity.ok(
                                                ApiResponseWrapper.success(updated, "Account updated successfully")))
                                .onErrorResume(e -> {
                                        log.error("Error updating account {}: {}", id, e.getMessage());
                                        return Mono.error(new BusinessException(
                                                        "Error during account update: " + e.getMessage()));
                                });
        }

        /**
         * Importe un plan comptable depuis un fichier XLSX ou CSV.
         */
        @Operation(summary = "Import plan comptable", description = "Importe des comptes depuis un fichier .xlsx ou .csv. Colonnes : no_compte, libelle, notes (optionnel)")
        @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public Mono<ResponseEntity<ApiResponseWrapper<ImportResult>>> importPlanComptable(
                        @RequestPart("file") FilePart file,
                        @AuthenticationPrincipal Jwt jwt) {

                String currentUser = jwt != null ? jwt.getSubject() : "system";

                return file.content()
                                .reduce(new byte[0], (acc, buf) -> {
                                        byte[] bytes = new byte[buf.readableByteCount()];
                                        buf.read(bytes);
                                        byte[] merged = new byte[acc.length + bytes.length];
                                        System.arraycopy(acc, 0, merged, 0, acc.length);
                                        System.arraycopy(bytes, 0, merged, acc.length, bytes.length);
                                        return merged;
                                })
                                .flatMap(bytes -> ReactiveOrganizationContext.getOrganizationId()
                                                .flatMap(orgId -> planImportService.importFromBytes(
                                                                bytes,
                                                                file.filename(),
                                                                orgId,
                                                                currentUser)))
                                .map(result -> ResponseEntity.ok(ApiResponseWrapper.success(result, "Import terminé")))
                                .onErrorResume(e -> {
                                        log.error("Erreur import plan comptable: {}", e.getMessage());
                                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                        .<ApiResponseWrapper<ImportResult>>body(
                                                                        ApiResponseWrapper.error("Erreur d'import : "
                                                                                        + e.getMessage())));
                                });
        }

        /**
         * Deactivates an account (soft delete).
         */
        @Operation(summary = "Deactivate an accounting account", description = "Deactivates an account instead of deleting it.")
        @DeleteMapping("/{id}")
        public Mono<ResponseEntity<ApiResponseWrapper<String>>> deactivatePlanComptable(@PathVariable UUID id) {
                return plan_service.deactivateAccount(id)
                                .then(Mono.fromCallable(
                                                () -> ResponseEntity.ok(ApiResponseWrapper
                                                                .success("Account deactivated successfully"))))
                                .onErrorResume(e -> {
                                        log.error("Error deactivating account {}: {}", id, e.getMessage());
                                        return Mono.error(new ResourceNotFoundException("Accounting account",
                                                        id.toString()));
                                });
        }
}
