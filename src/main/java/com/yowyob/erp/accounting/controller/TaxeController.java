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

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for managing taxes.
 * 
 * @author ALD
 * @date 30.09.25
 */
@RestController
@RequestMapping("/api/accounting/taxes")
@RequiredArgsConstructor
@Tag(name = "Tax Management", description = "Endpoints for managing taxes in the accounting module")
public class TaxeController {

    private final TaxeService taxe_service;

    @PostMapping
    @Operation(summary = "Create a new tax")
    public ResponseEntity<ApiResponseWrapper<TaxeDto>> createTaxe(@Valid @RequestBody TaxeDto dto) {
        TaxeDto created = taxe_service.createTaxe(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseWrapper.success(created, "Tax created successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing tax")
    public ResponseEntity<ApiResponseWrapper<TaxeDto>> updateTaxe(@PathVariable UUID id,
            @Valid @RequestBody TaxeDto dto) {
        TaxeDto updated = taxe_service.updateTaxe(id, dto);
        return ResponseEntity.ok(ApiResponseWrapper.success(updated, "Tax updated successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tax by ID")
    public ResponseEntity<ApiResponseWrapper<TaxeDto>> getTaxe(@PathVariable UUID id) {
        return taxe_service.getTaxe(id)
                .map(taxe -> ResponseEntity.ok(ApiResponseWrapper.success(taxe, "Tax found")))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponseWrapper.error("Tax not found", 404)));
    }

    @GetMapping
    @Operation(summary = "List all taxes for the current tenant")
    public ResponseEntity<ApiResponseWrapper<List<TaxeDto>>> getAllTaxes(
            @RequestParam(required = false, defaultValue = "false") boolean onlyActive) {
        List<TaxeDto> taxes = onlyActive ? taxe_service.getActiveTaxes() : taxe_service.getAllTaxes();
        return ResponseEntity.ok(ApiResponseWrapper.success(taxes, "Taxes list retrieved successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a tax")
    public ResponseEntity<ApiResponseWrapper<Void>> deleteTaxe(@PathVariable UUID id) {
        taxe_service.deleteTaxe(id);
        return ResponseEntity.ok(ApiResponseWrapper.success(null, "Tax deleted successfully"));
    }
}
