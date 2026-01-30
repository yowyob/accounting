package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.dto.EcritureComptableDto;
import com.yowyob.erp.accounting.service.EcritureComptableService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
import com.yowyob.erp.common.entity.ComptableObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Reactive REST Controller for managing accounting entries.
 */
@RestController
@RequestMapping("/api/accounting/ecritures")
@RequiredArgsConstructor
@Tag(name = "Accounting Entries", description = "Endpoints for managing journal entries and their validations")
public class EcritureComptableController {

    private final EcritureComptableService ecriture_service;

    @PostMapping
    @Operation(summary = "Create a new accounting entry")
    public Mono<ResponseEntity<ApiResponseWrapper<EcritureComptableDto>>> createEcriture(
            @Valid @RequestBody EcritureComptableDto dto) {
        return ecriture_service.createEcriture(dto)
                .map(created -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponseWrapper.success(created, "Entry created successfully")))
                .contextWrite(com.yowyob.erp.config.tenant.ReactiveTenantContext.captureFromThreadLocal());
    }

    @PostMapping("/{id}/validate")
    @Operation(summary = "Validate an accounting entry")
    public Mono<ResponseEntity<ApiResponseWrapper<EcritureComptableDto>>> validateEcriture(@PathVariable UUID id,
            @RequestParam(required = false) String user) {
        return ecriture_service.validateEcriture(id, user)
                .map(validated -> ResponseEntity
                        .ok(ApiResponseWrapper.success(validated, "Entry validated successfully")))
                .contextWrite(com.yowyob.erp.config.tenant.ReactiveTenantContext.captureFromThreadLocal());
    }

    @GetMapping
    @Operation(summary = "List all entries for the current tenant")
    public Mono<ResponseEntity<ApiResponseWrapper<List<EcritureComptableDto>>>> getAll() {
        return ecriture_service.getAll()
                .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list, "Entries list retrieved")))
                .contextWrite(com.yowyob.erp.config.tenant.ReactiveTenantContext.captureFromThreadLocal());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an entry by its ID")
    public Mono<ResponseEntity<ApiResponseWrapper<EcritureComptableDto>>> getById(@PathVariable UUID id) {
        return ecriture_service.getById(id)
                .map(e -> ResponseEntity.ok(ApiResponseWrapper.success(e, "Entry found")))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponseWrapper.error("Entry not found", 404)))
                .contextWrite(com.yowyob.erp.config.tenant.ReactiveTenantContext.captureFromThreadLocal());
    }

    @GetMapping("/non-validated")
    @Operation(summary = "List all non-validated entries")
    public Mono<ResponseEntity<ApiResponseWrapper<List<EcritureComptableDto>>>> getNonValidated() {
        return ecriture_service.getNonValidated()
                .map(list -> ResponseEntity
                        .ok(ApiResponseWrapper.success(list, "Non-validated entries list retrieved")))
                .contextWrite(com.yowyob.erp.config.tenant.ReactiveTenantContext.captureFromThreadLocal());
    }

    @GetMapping("/search")
    @Operation(summary = "Search entries by date range and journal")
    public Mono<ResponseEntity<ApiResponseWrapper<List<EcritureComptableDto>>>> search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false) UUID journalId) {
        return ecriture_service.searchEcritures(start, end, journalId)
                .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list, "Search results")))
                .contextWrite(com.yowyob.erp.config.tenant.ReactiveTenantContext.captureFromThreadLocal());
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate an entry from a comptable object")
    public Mono<ResponseEntity<ApiResponseWrapper<EcritureComptableDto>>> generate(
            @RequestBody ComptableObject object) {
        return ecriture_service.generateFromComptableObject(object)
                .map(created -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponseWrapper.success(created, "Entry generated successfully")))
                .contextWrite(com.yowyob.erp.config.tenant.ReactiveTenantContext.captureFromThreadLocal());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an accounting entry")
    public Mono<ResponseEntity<ApiResponseWrapper<Object>>> delete(@PathVariable UUID id) {
        return ecriture_service.deleteEcriture(id)
                .then(Mono.fromCallable(
                        () -> ResponseEntity.ok(ApiResponseWrapper.success(null, "Entry deleted successfully"))))
                .contextWrite(com.yowyob.erp.config.tenant.ReactiveTenantContext.captureFromThreadLocal());
    }
}
