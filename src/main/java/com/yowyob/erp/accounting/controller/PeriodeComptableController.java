package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.dto.PeriodeComptableDto;
import com.yowyob.erp.accounting.service.PeriodeComptableService;
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
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Controller for managing accounting periods.
 * Provides REST endpoints for CRUD operations, closing, and retrieval of
 * periods.
 * 
 * @author ALD
 * @date 30.09.25
 */
@RestController
@RequestMapping("/api/accounting/periodes")
@RequiredArgsConstructor
@Tag(name = "Accounting Periods", description = "Comprehensive management of accounting periods (creation, update, closing, retrieval).")
@SecurityRequirement(name = "BasicAuth")
@Slf4j
public class PeriodeComptableController {

    private final PeriodeComptableService periode_service;

    /**
     * Creates a new accounting period.
     * 
     * @param dto the period data
     * @return the created period wrapped in ApiResponseWrapper
     */
    @Operation(summary = "Create an accounting period", description = "Adds a new accounting period for the current tenant.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Period created successfully", content = @Content(schema = @Schema(implementation = PeriodeComptableDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or overlapping period")
    })
    @PostMapping
    public ResponseEntity<ApiResponseWrapper<PeriodeComptableDto>> createPeriodeComptable(
            @Valid @RequestBody PeriodeComptableDto dto) {
        try {
            UUID tenant_id = TenantContext.getCurrentTenant();
            log.info("Request to create an accounting period for tenant {}", tenant_id);
            PeriodeComptableDto created = periode_service.createPeriode(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponseWrapper.success(created, "Accounting period created successfully"));
        } catch (Exception e) {
            log.error("Error creating period: {}", e.getMessage());
            throw new BusinessException("Error creating period: " + e.getMessage());
        }
    }

    /**
     * Retrieves an accounting period by its ID.
     * 
     * @param id the period ID
     * @return the period wrapped in ApiResponseWrapper
     */
    @Operation(summary = "Retrieve an accounting period by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseWrapper<PeriodeComptableDto>> getPeriodeComptable(@PathVariable UUID id) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        log.info("Retrieving period ID={} for tenant {}", id, tenant_id);
        return periode_service.getPeriode(id)
                .map(dto -> ResponseEntity.ok(ApiResponseWrapper.success(dto, "Accounting period found")))
                .orElseThrow(() -> new ResourceNotFoundException("Accounting period", id.toString()));
    }

    /**
     * Lists all accounting periods for the current tenant.
     * 
     * @return list of periods wrapped in ApiResponseWrapper
     */
    @Operation(summary = "List all accounting periods")
    @GetMapping
    public ResponseEntity<ApiResponseWrapper<List<PeriodeComptableDto>>> getAllPeriodeComptables() {
        UUID tenant_id = TenantContext.getCurrentTenant();
        log.info("Retrieving all accounting periods for tenant {}", tenant_id);
        List<PeriodeComptableDto> periodes = periode_service.getAllPeriodes();
        return ResponseEntity
                .ok(ApiResponseWrapper.success(periodes, "Complete list of accounting periods retrieved successfully"));
    }

    /**
     * Retrieves an accounting period by its code.
     * 
     * @param code the period code
     * @return the period wrapped in ApiResponseWrapper
     */
    @Operation(summary = "Retrieve a period by code")
    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponseWrapper<PeriodeComptableDto>> getPeriodeByCode(@PathVariable String code) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        log.info("Retrieving period by code={} for tenant {}", code, tenant_id);
        return periode_service.getByCode(code)
                .map(dto -> ResponseEntity.ok(ApiResponseWrapper.success(dto, "Accounting period found")))
                .orElseThrow(() -> new ResourceNotFoundException("Accounting period", code));
    }

    /**
     * Retrieves an accounting period containing the given date.
     * 
     * @param date the date to check
     * @return the period wrapped in ApiResponseWrapper
     */
    @Operation(summary = "Retrieve an accounting period by date")
    @GetMapping("/by-date")
    public ResponseEntity<ApiResponseWrapper<PeriodeComptableDto>> getPeriodeByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        log.info("Searching for period containing date {} for tenant {}", date, tenant_id);
        return periode_service.getByDate(date)
                .map(dto -> ResponseEntity.ok(ApiResponseWrapper.success(dto, "Period containing the date found")))
                .orElseThrow(() -> new ResourceNotFoundException("Accounting period", date.toString()));
    }

    /**
     * Lists all non-closed (open) periods for the current tenant.
     * 
     * @return list of open periods wrapped in ApiResponseWrapper
     */
    @Operation(summary = "List non-closed (open) periods")
    @GetMapping("/non-closed")
    public ResponseEntity<ApiResponseWrapper<List<PeriodeComptableDto>>> getNonClosedPeriodes() {
        UUID tenant_id = TenantContext.getCurrentTenant();
        log.info("Retrieving non-closed periods for tenant {}", tenant_id);
        List<PeriodeComptableDto> periodes = periode_service.getNonClosedPeriodes();
        return ResponseEntity.ok(ApiResponseWrapper.success(periodes, "Non-closed periods retrieved successfully"));
    }

    /**
     * Lists accounting periods within a specific date range.
     * 
     * @param start_date the start date
     * @param end_date   the end date
     * @return list of periods wrapped in ApiResponseWrapper
     */
    @Operation(summary = "List periods within a date range")
    @GetMapping("/range")
    public ResponseEntity<ApiResponseWrapper<List<PeriodeComptableDto>>> getPeriodesByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start_date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end_date) {
        if (start_date.isAfter(end_date)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseWrapper.error("Start date must be before end date"));
        }
        UUID tenant_id = TenantContext.getCurrentTenant();
        log.info("Retrieving periods between {} and {} for tenant {}", start_date, end_date, tenant_id);
        List<PeriodeComptableDto> periodes = periode_service.getByRange(start_date, end_date);
        return ResponseEntity.ok(ApiResponseWrapper.success(periodes, "Periods retrieved successfully"));
    }

    /**
     * Updates an existing accounting period.
     * 
     * @param id  the period ID
     * @param dto the new period data
     * @return the updated period wrapped in ApiResponseWrapper
     */
    @Operation(summary = "Update an accounting period")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseWrapper<PeriodeComptableDto>> updatePeriodeComptable(
            @PathVariable UUID id,
            @Valid @RequestBody PeriodeComptableDto dto) {
        try {
            UUID tenant_id = TenantContext.getCurrentTenant();
            log.info("Updating period ID={} for tenant {}", id, tenant_id);
            PeriodeComptableDto updated = periode_service.updatePeriode(id, dto);
            return ResponseEntity.ok(ApiResponseWrapper.success(updated, "Accounting period updated successfully"));
        } catch (Exception e) {
            log.error("Error updating period {} : {}", id, e.getMessage());
            throw new BusinessException("Error updating period: " + e.getMessage());
        }
    }

    /**
     * Closes an existing accounting period.
     * 
     * @param id the period ID to close
     * @return the closed period wrapped in ApiResponseWrapper
     */
    @Operation(summary = "Close an accounting period")
    @PutMapping("/{id}/close")
    public ResponseEntity<ApiResponseWrapper<PeriodeComptableDto>> closePeriodeComptable(@PathVariable UUID id) {
        try {
            UUID tenant_id = TenantContext.getCurrentTenant();
            log.info("Closing period ID={} for tenant {}", id, tenant_id);
            PeriodeComptableDto closed = periode_service.closePeriode(id);
            return ResponseEntity.ok(ApiResponseWrapper.success(closed, "Period closed successfully"));
        } catch (IllegalStateException e) {
            throw new BusinessException("Period already closed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error closing period {} : {}", id, e.getMessage());
            throw new BusinessException("Error closing period: " + e.getMessage());
        }
    }

    /**
     * Deletes an accounting period by its ID.
     * 
     * @param id the period ID to delete
     * @return success message wrapped in ApiResponseWrapper
     */
    @Operation(summary = "Delete an accounting period")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseWrapper<String>> deletePeriodeComptable(@PathVariable UUID id) {
        try {
            UUID tenant_id = TenantContext.getCurrentTenant();
            log.info("Deleting period ID={} for tenant {}", id, tenant_id);
            periode_service.deletePeriode(id);
            return ResponseEntity.ok(ApiResponseWrapper.success("Accounting period deleted successfully"));
        } catch (IllegalStateException e) {
            throw new BusinessException("Cannot delete a closed period: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error deleting period {} : {}", id, e.getMessage());
            throw new ResourceNotFoundException("Accounting period", id.toString());
        }
    }
}
