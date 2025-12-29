package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.dto.EcritureComptableDto;
import com.yowyob.erp.accounting.service.EcritureComptableService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
import com.yowyob.erp.common.dto.ComptableObjectRequest;
import com.yowyob.erp.common.entity.ComptableObject;
import com.yowyob.erp.common.exception.BusinessException;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import com.yowyob.erp.config.tenant.TenantContext;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.yowyob.erp.accounting.util.AccountingUtils.mapToComptableObject;

/**
 * Controller for managing accounting entries.
 * Handles CRUD operations, validation, and generation from other objects.
 * 
 * @author ALD
 * @date 30.09.25
 */
@RestController
@RequestMapping("/api/accounting/entries")
@RequiredArgsConstructor
@Tag(name = "Accounting Entries", description = "Management of accounting entries with Kafka + Redis + Multi-tenant")
@SecurityRequirement(name = "BasicAuth")
@Slf4j
public class EcritureComptableController {

    private final EcritureComptableService ecriture_service;

    /**
     * Manually creates an accounting entry.
     * 
     * @param ecriture_dto the entry data
     * @return the created entry
     */
    @Operation(summary = "Create an accounting entry manually", description = "Creates a new entry after period and journal validation.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Entry created successfully", content = @Content(schema = @Schema(implementation = EcritureComptableDto.class))),
            @ApiResponse(responseCode = "400", description = "Data validation error")
    })
    @PostMapping
    public ResponseEntity<ApiResponseWrapper<EcritureComptableDto>> createEcriture(
            @Valid @RequestBody EcritureComptableDto ecriture_dto) {
        try {
            EcritureComptableDto created = ecriture_service.createEcriture(ecriture_dto);
            log.info("🧾 Entry created: {}", created.getNumero_ecriture());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponseWrapper.success(created, "Accounting entry created successfully"));
        } catch (Exception e) {
            log.error("Error creating entry: {}", e.getMessage());
            throw new BusinessException("Creation error: " + e.getMessage());
        }
    }

    /**
     * Validates an accounting entry.
     * 
     * @param id             entry ID
     * @param authentication auth context
     * @return the validated entry
     */
    @Operation(summary = "Validate an accounting entry")
    @PostMapping("/{id}/validate")
    public ResponseEntity<ApiResponseWrapper<EcritureComptableDto>> validateEcriture(
            @PathVariable UUID id, Authentication authentication) {
        String current_user = authentication != null ? authentication.getName() : "system";
        EcritureComptableDto validated = ecriture_service.validateEcriture(id, current_user);
        log.info("✅ Entry validated by {}", current_user);
        return ResponseEntity.ok(ApiResponseWrapper.success(validated, "Accounting entry validated"));
    }

    /**
     * Lists all accounting entries for the current tenant.
     * 
     * @return list of entries
     */
    @Operation(summary = "List all accounting entries")
    @GetMapping
    public ResponseEntity<ApiResponseWrapper<List<EcritureComptableDto>>> getAllEcritures() {
        UUID tenant_id = TenantContext.getCurrentTenant();
        log.info("📄 Retrieving all entries for tenant {}", tenant_id);
        List<EcritureComptableDto> list = ecriture_service.getAll();
        return ResponseEntity.ok(ApiResponseWrapper.success(list));
    }

    /**
     * Retrieves an accounting entry by its ID.
     * 
     * @param id entry ID
     * @return the entry
     */
    @Operation(summary = "Get an accounting entry by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseWrapper<EcritureComptableDto>> getEcritureById(@PathVariable UUID id) {
        EcritureComptableDto ecriture = ecriture_service.getById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Accounting Entry", id.toString()));
        log.info("🔍 Entry retrieved: {}", id);
        return ResponseEntity.ok(ApiResponseWrapper.success(ecriture));
    }

    /**
     * Lists non-validated entries.
     * 
     * @return list of non-validated entries
     */
    @Operation(summary = "List non-validated entries")
    @GetMapping("/non-validated")
    public ResponseEntity<ApiResponseWrapper<List<EcritureComptableDto>>> getNonValidatedEcritures() {
        List<EcritureComptableDto> list = ecriture_service.getNonValidated();
        log.info("⏳ {} non-validated entries retrieved", list.size());
        return ResponseEntity.ok(ApiResponseWrapper.success(list));
    }

    /**
     * Searches for entries by period and journal.
     * 
     * @param start_date start of the date range
     * @param end_date   end of the date range
     * @param journal_id journal ID
     * @return list of matching entries
     */
    @Operation(summary = "Search entries by period and journal")
    @GetMapping("/search")
    public ResponseEntity<ApiResponseWrapper<List<EcritureComptableDto>>> searchEcritures(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start_date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end_date,
            @RequestParam(required = false) UUID journal_id) {

        if (start_date != null && end_date != null && start_date.isAfter(end_date)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseWrapper.error("Start date must be before end date"));
        }

        List<EcritureComptableDto> list = ecriture_service.searchEcritures(start_date, end_date, journal_id);
        log.info("🔎 Searching entries between {} and {} for journal {}", start_date, end_date, journal_id);
        return ResponseEntity.ok(ApiResponseWrapper.success(list, "Search completed"));
    }

    /**
     * Generates an entry from a comptable object (invoice, transaction, etc).
     * 
     * @param request the generation request
     * @return the generated entry
     */
    @Operation(summary = "Generate an entry from an accounting object")
    @PostMapping("/generate-from-object")
    public ResponseEntity<ApiResponseWrapper<EcritureComptableDto>> generateFromComptableObject(
            @RequestBody ComptableObjectRequest request) {
        try {
            if (request.getTenantId() == null || request.getJournalComptableId() == null) {
                throw new BusinessException("Tenant ID and Journal ID are required");
            }
            ComptableObject object = mapToComptableObject(request);
            EcritureComptableDto generated = ecriture_service.generateFromComptableObject(object);
            log.info("⚙️ Entry generated automatically for object {}", object.get_source_type());
            return ResponseEntity.ok(ApiResponseWrapper.success(generated, "Entry generated successfully"));
        } catch (Exception e) {
            log.error("Error generating entry: {}", e.getMessage());
            throw new BusinessException("Generation error: " + e.getMessage());
        }
    }

    /**
     * Deletes an accounting entry if not validated.
     * 
     * @param id entry ID
     * @return success message
     */
    @Operation(summary = "Delete an accounting entry (if not validated)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseWrapper<Void>> deleteEcriture(@PathVariable UUID id) {
        try {
            ecriture_service.deleteEcriture(id);
            log.info("🗑️ Entry deleted {}", id);
            return ResponseEntity.ok(ApiResponseWrapper.success(null, "Entry deleted successfully"));
        } catch (IllegalStateException e) {
            throw new BusinessException("Entry already validated: " + e.getMessage());
        } catch (Exception e) {
            throw new ResourceNotFoundException("Entry not found", id.toString());
        }
    }
}
