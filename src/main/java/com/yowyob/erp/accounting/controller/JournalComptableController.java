package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.dto.JournalComptableDto;
import com.yowyob.erp.accounting.service.JournalComptableService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Controller for managing accounting journals.
 * Provides REST endpoints for CRUD operations on JournalComptable entities.
 * 
 * @author ALD
 * @date 30.09.25
 */
@RestController
@RequestMapping("/api/accounting/journals")
@RequiredArgsConstructor
@Tag(name = "Accounting Journals", description = "Comprehensive management of accounting journals (creation, update, deletion, retrieval).")
@SecurityRequirement(name = "BasicAuth")
@Slf4j
public class JournalComptableController {

    private final JournalComptableService journal_service;

    /**
     * Creates a new accounting journal.
     * 
     * @param dto the journal data
     * @return the created journal wrapped in ApiResponseWrapper
     */
    @Operation(summary = "Create an accounting journal", description = "Adds a new accounting journal for the current tenant.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Journal created successfully", content = @Content(schema = @Schema(implementation = JournalComptableDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid data")
    })
    @PostMapping
    public ResponseEntity<ApiResponseWrapper<JournalComptableDto>> createJournalComptable(
            @RequestBody JournalComptableDto dto) {
        try {
            UUID tenant_id = TenantContext.getCurrentTenant();
            log.info("Request to create an accounting journal for tenant {}", tenant_id);
            JournalComptableDto saved_journal = journal_service.createJournalComptable(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponseWrapper.success(saved_journal, "Accounting journal created successfully"));
        } catch (Exception e) {
            log.error("Error creating journal: {}", e.getMessage());
            throw new BusinessException("Error creating journal: " + e.getMessage());
        }
    }

    /**
     * Retrieves an accounting journal by its ID.
     * 
     * @param id the journal ID
     * @return the journal wrapped in ApiResponseWrapper
     */
    @Operation(summary = "Retrieve an accounting journal", description = "Returns the full details of an accounting journal by its ID.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseWrapper<JournalComptableDto>> getJournalComptable(@PathVariable UUID id) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        log.info("Retrieving journal ID={} for tenant {}", id, tenant_id);

        return journal_service.getJournalComptable(id)
                .map(journal_dto -> ResponseEntity
                        .ok(ApiResponseWrapper.success(journal_dto, "Journal retrieved successfully")))
                .orElseThrow(() -> new ResourceNotFoundException("JournalComptable", id.toString()));
    }

    /**
     * Lists all journals for the current tenant.
     * 
     * @return list of journals wrapped in ApiResponseWrapper
     */
    @Operation(summary = "List all journals", description = "Complete list of accounting journals for the current tenant.")
    @GetMapping
    public ResponseEntity<ApiResponseWrapper<List<JournalComptableDto>>> getAllJournalComptables() {
        UUID tenant_id = TenantContext.getCurrentTenant();
        log.info("Retrieving all accounting journals for tenant {}", tenant_id);
        List<JournalComptableDto> journals = journal_service.getAllJournalComptables();
        return ResponseEntity
                .ok(ApiResponseWrapper.success(journals, "Complete list of journals retrieved successfully"));
    }

    /**
     * Lists all active journals for the current tenant.
     * 
     * @return list of active journals wrapped in ApiResponseWrapper
     */
    @Operation(summary = "List active journals", description = "Retrieves all active accounting journals.")
    @GetMapping("/active")
    public ResponseEntity<ApiResponseWrapper<List<JournalComptableDto>>> getActiveJournalComptables() {
        UUID tenant_id = TenantContext.getCurrentTenant();
        log.info("Retrieving active journals for tenant {}", tenant_id);
        List<JournalComptableDto> journals = journal_service.getActiveJournalComptables();
        return ResponseEntity
                .ok(ApiResponseWrapper.success(journals, "List of active journals retrieved successfully"));
    }

    /**
     * Updates an existing accounting journal.
     * 
     * @param id  the journal ID
     * @param dto the new journal data
     * @return the updated journal wrapped in ApiResponseWrapper
     */
    @Operation(summary = "Update an accounting journal", description = "Updates the information of an existing journal.")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseWrapper<JournalComptableDto>> updateJournalComptable(
            @PathVariable UUID id,
            @RequestBody JournalComptableDto dto) {
        try {
            UUID tenant_id = TenantContext.getCurrentTenant();
            log.info("Updating journal ID={} for tenant {}", id, tenant_id);
            JournalComptableDto updated = journal_service.updateJournalComptable(id, dto);
            return ResponseEntity.ok(ApiResponseWrapper.success(updated, "Journal updated successfully"));
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException("JournalComptable", id.toString());
        } catch (Exception e) {
            log.error("Error updating journal {} : {}", id, e.getMessage());
            throw new BusinessException("Error updating journal: " + e.getMessage());
        }
    }

    /**
     * Deletes an accounting journal by its ID.
     * 
     * @param id the journal ID to delete
     * @return success message wrapped in ApiResponseWrapper
     */
    @Operation(summary = "Delete an accounting journal", description = "Deletes an accounting journal by its ID.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseWrapper<String>> deleteJournalComptable(@PathVariable UUID id) {
        try {
            UUID tenant_id = TenantContext.getCurrentTenant();
            log.info("Deleting journal ID={} for tenant {}", id, tenant_id);
            journal_service.deleteJournalComptable(id);
            return ResponseEntity.ok(ApiResponseWrapper.success("Journal deleted successfully"));
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException("JournalComptable", id.toString());
        } catch (Exception e) {
            log.error("Error deleting journal {} : {}", id, e.getMessage());
            throw new BusinessException("Error deleting journal: " + e.getMessage());
        }
    }
}