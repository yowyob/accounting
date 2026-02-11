package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.dto.PeriodeComptableDto;
import com.yowyob.erp.accounting.service.PeriodeComptableService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
import com.yowyob.erp.common.exception.BusinessException;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
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
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Reactive Controller for managing accounting periods.
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
     */
    @Operation(summary = "Create an accounting period", description = "Adds a new accounting period for the current organization.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Period created successfully", content = @Content(schema = @Schema(implementation = PeriodeComptableDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or overlapping period")
    })
    @PostMapping
    public Mono<ResponseEntity<ApiResponseWrapper<PeriodeComptableDto>>> createPeriodeComptable(
            @Valid @RequestBody PeriodeComptableDto dto) {
        log.info("Controller: createPeriodeComptable received request for code: {}", dto.getCode());
        return periode_service.createPeriode(dto)
                .map(created -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponseWrapper.success(created, "Accounting period created successfully")))
                .onErrorResume(e -> {
                    log.error("Error creating period: {}", e.getMessage());
                    return Mono.error(new BusinessException("Error creating period: " + e.getMessage()));
                })
                .contextWrite(com.yowyob.erp.config.organization.ReactiveOrganizationContext.captureFromThreadLocal());
    }

    /**
     * Retrieves an accounting period by its ID.
     */
    @Operation(summary = "Retrieve an accounting period by ID")
    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiResponseWrapper<PeriodeComptableDto>>> getPeriodeComptable(@PathVariable UUID id) {
        return periode_service.getPeriode(id)
                .map(dto -> ResponseEntity.ok(ApiResponseWrapper.success(dto, "Accounting period found")))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Accounting period", id.toString())))
                .contextWrite(com.yowyob.erp.config.organization.ReactiveOrganizationContext.captureFromThreadLocal());
    }

    /**
     * Lists all accounting periods for the current organization.
     */
    @Operation(summary = "List all accounting periods")
    @GetMapping
    public Mono<ResponseEntity<ApiResponseWrapper<List<PeriodeComptableDto>>>> getAllPeriodeComptables() {
        return periode_service.getAllPeriodes()
                .map(periodes -> ResponseEntity.ok(ApiResponseWrapper.success(periodes,
                        "Complete list of accounting periods retrieved successfully")))
                .contextWrite(com.yowyob.erp.config.organization.ReactiveOrganizationContext.captureFromThreadLocal());
    }

    /**
     * Retrieves an accounting period by its code.
     */
    @Operation(summary = "Retrieve a period by code")
    @GetMapping("/code/{code}")
    public Mono<ResponseEntity<ApiResponseWrapper<PeriodeComptableDto>>> getPeriodeByCode(@PathVariable String code) {
        return periode_service.getByCode(code)
                .map(dto -> ResponseEntity.ok(ApiResponseWrapper.success(dto, "Accounting period found")))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Accounting period", code)))
                .contextWrite(com.yowyob.erp.config.organization.ReactiveOrganizationContext.captureFromThreadLocal());
    }

    /**
     * Retrieves an accounting period containing the given date.
     */
    @Operation(summary = "Retrieve an accounting period by date")
    @GetMapping("/by-date")
    public Mono<ResponseEntity<ApiResponseWrapper<PeriodeComptableDto>>> getPeriodeByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return periode_service.getByDate(date)
                .map(dto -> ResponseEntity.ok(ApiResponseWrapper.success(dto, "Period containing the date found")))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Accounting period", date.toString())))
                .contextWrite(com.yowyob.erp.config.organization.ReactiveOrganizationContext.captureFromThreadLocal());
    }

    /**
     * Lists all non-closed (open) periods for the current organization.
     */
    @Operation(summary = "List non-closed (open) periods")
    @GetMapping("/non-closed")
    public Mono<ResponseEntity<ApiResponseWrapper<List<PeriodeComptableDto>>>> getNonClosedPeriodes() {
        return periode_service.getNonClosedPeriodes()
                .map(periodes -> ResponseEntity
                        .ok(ApiResponseWrapper.success(periodes, "Non-closed periods retrieved successfully")))
                .contextWrite(com.yowyob.erp.config.organization.ReactiveOrganizationContext.captureFromThreadLocal());
    }

    /**
     * Lists accounting periods within a specific date range.
     */
    @Operation(summary = "List periods within a date range")
    @GetMapping("/range")
    public Mono<ResponseEntity<ApiResponseWrapper<List<PeriodeComptableDto>>>> getPeriodesByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start_date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end_date) {
        if (start_date.isAfter(end_date)) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(ApiResponseWrapper.error("Start date must be before end date")));
        }
        return periode_service.getByRange(start_date, end_date)
                .map(periodes -> ResponseEntity
                        .ok(ApiResponseWrapper.success(periodes, "Periods retrieved successfully")))
                .contextWrite(com.yowyob.erp.config.organization.ReactiveOrganizationContext.captureFromThreadLocal());
    }

    /**
     * Updates an existing accounting period.
     */
    @Operation(summary = "Update an accounting period")
    @PutMapping("/{id}")
    public Mono<ResponseEntity<ApiResponseWrapper<PeriodeComptableDto>>> updatePeriodeComptable(
            @PathVariable UUID id,
            @Valid @RequestBody PeriodeComptableDto dto) {
        return periode_service.updatePeriode(id, dto)
                .map(updated -> ResponseEntity
                        .ok(ApiResponseWrapper.success(updated, "Accounting period updated successfully")))
                .onErrorResume(e -> {
                    log.error("Error updating period {} : {}", id, e.getMessage());
                    return Mono.error(new BusinessException("Error updating period: " + e.getMessage()));
                })
                .contextWrite(com.yowyob.erp.config.organization.ReactiveOrganizationContext.captureFromThreadLocal());
    }

    /**
     * Closes an existing accounting period.
     */
    @Operation(summary = "Close an accounting period")
    @PutMapping("/{id}/close")
    public Mono<ResponseEntity<ApiResponseWrapper<PeriodeComptableDto>>> closePeriodeComptable(@PathVariable UUID id) {
        return periode_service.closePeriode(id)
                .map(closed -> ResponseEntity.ok(ApiResponseWrapper.success(closed, "Period closed successfully")))
                .onErrorResume(e -> {
                    if (e instanceof IllegalStateException) {
                        return Mono.error(new BusinessException("Period already closed: " + e.getMessage()));
                    }
                    log.error("Error closing period {} : {}", id, e.getMessage());
                    return Mono.error(new BusinessException("Error closing period: " + e.getMessage()));
                })
                .contextWrite(com.yowyob.erp.config.organization.ReactiveOrganizationContext.captureFromThreadLocal());
    }

    /**
     * Deletes an accounting period by its ID.
     */
    @Operation(summary = "Delete an accounting period")
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<ApiResponseWrapper<String>>> deletePeriodeComptable(@PathVariable UUID id) {
        return periode_service.deletePeriode(id)
                .then(Mono.fromCallable(
                        () -> ResponseEntity.ok(ApiResponseWrapper.success("Accounting period deleted successfully"))))
                .onErrorResume(e -> {
                    if (e instanceof IllegalStateException) {
                        return Mono.error(new BusinessException("Cannot delete a closed period: " + e.getMessage()));
                    }
                    log.error("Error deleting period {} : {}", id, e.getMessage());
                    return Mono.error(new ResourceNotFoundException("Accounting period", id.toString()));
                })
                .contextWrite(com.yowyob.erp.config.organization.ReactiveOrganizationContext.captureFromThreadLocal());
    }
}
