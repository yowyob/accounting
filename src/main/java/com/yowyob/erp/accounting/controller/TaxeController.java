package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.dto.TaxeDto;
import com.yowyob.erp.accounting.service.TaxeService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Reactive REST Controller for managing taxes.
 */
@RestController
@RequestMapping("/api/accounting/taxes")
@RequiredArgsConstructor
@Tag(name = "  Accounting Tax Management", description = "Endpoints for managing taxes in the accounting module")
public class TaxeController {

    private final TaxeService taxe_service;

    @PostMapping
    @Operation(summary = "Create a new tax")
    public Mono<ResponseEntity<ApiResponseWrapper<TaxeDto>>> createTaxe(@Valid @RequestBody TaxeDto dto) {
        return taxe_service.createTaxe(dto)
                .map(created -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponseWrapper.success(created, "Tax created successfully")))
                .contextWrite(ReactiveOrganizationContext.captureFromThreadLocal());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing tax")
    public Mono<ResponseEntity<ApiResponseWrapper<TaxeDto>>> updateTaxe(@PathVariable UUID id,
            @Valid @RequestBody TaxeDto dto) {
        return taxe_service.updateTaxe(id, dto)
                .map(updated -> ResponseEntity.ok(ApiResponseWrapper.success(updated, "Tax updated successfully")))
                .contextWrite(ReactiveOrganizationContext.captureFromThreadLocal());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tax by ID")
    public Mono<ResponseEntity<ApiResponseWrapper<TaxeDto>>> getTaxe(@PathVariable UUID id) {
        return taxe_service.getTaxe(id)
                .map(taxe -> ResponseEntity.ok(ApiResponseWrapper.success(taxe, "Tax found")))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponseWrapper.error("Tax not found", 404)))
                .contextWrite(ReactiveOrganizationContext.captureFromThreadLocal());
    }

    @GetMapping
    @Operation(summary = "List all taxes for the current organization")
    public Mono<ResponseEntity<ApiResponseWrapper<List<TaxeDto>>>> getAllTaxes(
            @RequestParam(required = false, defaultValue = "false") boolean onlyActive) {
        Mono<List<TaxeDto>> taxesMono = onlyActive ? taxe_service.getActiveTaxes() : taxe_service.getAllTaxes();
        return taxesMono.map(
                taxes -> ResponseEntity.ok(ApiResponseWrapper.success(taxes, "Taxes list retrieved successfully")))
                .contextWrite(ReactiveOrganizationContext.captureFromThreadLocal());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a tax")
    public Mono<ResponseEntity<ApiResponseWrapper<Object>>> deleteTaxe(@PathVariable UUID id) {
        return taxe_service.deleteTaxe(id)
                .then(Mono.fromCallable(
                        () -> ResponseEntity.ok(ApiResponseWrapper.success(null, "Tax deleted successfully"))))
                .contextWrite(ReactiveOrganizationContext.captureFromThreadLocal());
    }
}
