package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.dto.OperationComptableDto;
import com.yowyob.erp.accounting.service.OperationComptableService;
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
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Controller for managing accounting operations.
 * Handles CRUD operations with Kafka auditing, Redis caching, and multi-tenant
 * support.
 * 
 * @author ALD
 * @date 30.09.25
 */
@RestController
@RequestMapping("/api/accounting/operations")
@RequiredArgsConstructor
@Tag(name = "Accounting Operations", description = "Management of accounting operations with Kafka, Redis and multi-tenancy")
@SecurityRequirement(name = "BasicAuth")
@Slf4j
public class OperationComptableController {

    private final OperationComptableService operation_service;

    /**
     * Creates a new accounting operation for the current tenant.
     * 
     * @param dto operation data
     * @return response wrapper with created operation
     */
    @Operation(summary = "Create an accounting operation", description = "Creates a new accounting operation for the current tenant.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Operation created successfully", content = @Content(schema = @Schema(implementation = OperationComptableDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid data validation failure")
    })
    @PostMapping
    public ResponseEntity<ApiResponseWrapper<OperationComptableDto>> createOperationComptable(
            @Valid @RequestBody OperationComptableDto dto) {
        try {
            UUID tenant_id = TenantContext.getCurrentTenant();
            log.info("🧾 Creating accounting operation for tenant {}", tenant_id);
            OperationComptableDto created = operation_service.createOperation(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponseWrapper.success(created, "Accounting operation created successfully"));
        } catch (Exception e) {
            log.error("❌ Error creating operation: {}", e.getMessage());
            throw new BusinessException("Error during creation: " + e.getMessage());
        }
    }

    /**
     * Retrieves an accounting operation by its ID.
     * 
     * @param id operation ID
     * @return response wrapper with operation data
     */
    @Operation(summary = "Retrieve an accounting operation by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operation found"),
            @ApiResponse(responseCode = "404", description = "Operation not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseWrapper<OperationComptableDto>> getOperationComptable(@PathVariable UUID id) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        log.info("🔍 Retrieving accounting operation ID={} for tenant {}", id, tenant_id);
        return operation_service.getOperation(id)
                .map(dto -> ResponseEntity.ok(ApiResponseWrapper.success(dto, "Operation found")))
                .orElseThrow(() -> new ResourceNotFoundException("OperationComptable", id.toString()));
    }

    /**
     * Lists all accounting operations for the current tenant.
     * 
     * @return response wrapper with list of operations
     */
    @Operation(summary = "List all accounting operations")
    @GetMapping
    public ResponseEntity<ApiResponseWrapper<List<OperationComptableDto>>> getAllOperationsComptables() {
        UUID tenant_id = TenantContext.getCurrentTenant();
        log.info("📋 Retrieving all accounting operations for tenant {}", tenant_id);
        List<OperationComptableDto> operations = operation_service.getAllOperations();
        return ResponseEntity.ok(ApiResponseWrapper.success(operations, "List of accounting operations retrieved"));
    }

    /**
     * Retrieves operations associated with a specific principal account number.
     * 
     * @param no_compte account number
     * @return response wrapper with list of operations
     */
    @Operation(summary = "Retrieve accounting operations by principal account")
    @GetMapping("/by-no-compte")
    public ResponseEntity<ApiResponseWrapper<List<OperationComptableDto>>> getOperationsByNoCompte(
            @RequestParam String no_compte) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        log.info("🔎 Retrieving operations by account={} for tenant {}", no_compte, tenant_id);
        List<OperationComptableDto> operations = operation_service.getOperationsByCompte(no_compte);
        return ResponseEntity.ok(ApiResponseWrapper.success(operations, "Accounting operations retrieved"));
    }

    /**
     * Searches for an operation by its type and settlement mode.
     * 
     * @param type_operation operation type
     * @param mode_reglement settlement mode
     * @return response wrapper with matching operation
     */
    @Operation(summary = "Search operation by type and settlement mode")
    @GetMapping("/search")
    public ResponseEntity<ApiResponseWrapper<OperationComptableDto>> getOperationByTypeAndMode(
            @RequestParam String type_operation,
            @RequestParam String mode_reglement) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        log.info("🔎 Searching operation type={} / mode={} for tenant {}", type_operation, mode_reglement, tenant_id);
        return operation_service.getByTypeAndMode(type_operation, mode_reglement)
                .map(dto -> ResponseEntity.ok(ApiResponseWrapper.success(dto, "Operation found")))
                .orElseThrow(() -> new ResourceNotFoundException("OperationComptable",
                        type_operation + "-" + mode_reglement));
    }

    /**
     * Updates an existing accounting operation.
     * 
     * @param id  operation ID to update
     * @param dto new operation data
     * @return response wrapper with updated operation
     */
    @Operation(summary = "Update an accounting operation")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseWrapper<OperationComptableDto>> updateOperationComptable(
            @PathVariable UUID id,
            @Valid @RequestBody OperationComptableDto dto) {
        try {
            UUID tenant_id = TenantContext.getCurrentTenant();
            log.info("✏️ Updating accounting operation ID={} for tenant {}", id, tenant_id);
            OperationComptableDto updated = operation_service.updateOperation(id, dto);
            return ResponseEntity.ok(ApiResponseWrapper.success(updated, "Accounting operation updated successfully"));
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException("OperationComptable", id.toString());
        } catch (Exception e) {
            log.error("Error updating operation {}: {}", id, e.getMessage());
            throw new BusinessException("Update error: " + e.getMessage());
        }
    }

    /**
     * Deletes an accounting operation by ID.
     * 
     * @param id operation ID to delete
     * @return response wrapper with success message
     */
    @Operation(summary = "Delete an accounting operation", description = "Deletes an existing accounting operation by ID.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseWrapper<String>> deleteOperationComptable(@PathVariable UUID id) {
        try {
            UUID tenant_id = TenantContext.getCurrentTenant();
            log.info("🗑️ Deleting operation ID={} for tenant {}", id, tenant_id);
            operation_service.deleteOperation(id);
            return ResponseEntity.ok(ApiResponseWrapper.success("Accounting operation deleted successfully"));
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException("OperationComptable", id.toString());
        } catch (Exception e) {
            log.error("Error deleting operation {}: {}", id, e.getMessage());
            throw new BusinessException("Error during deletion: " + e.getMessage());
        }
    }
}
