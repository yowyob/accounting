package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.dto.TauxChangeDto;
import com.yowyob.erp.accounting.service.TauxChangeService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for managing exchange rates.
 * 
 * @author ALD
 * @date 30.09.25
 */
@RestController
@RequestMapping("/api/accounting/exchange-rates")
@RequiredArgsConstructor
@Tag(name = "Exchange Rate Management", description = "Endpoints for managing tenant-specific exchange rates")
public class TauxChangeController {

    private final TauxChangeService taux_service;

    @PostMapping
    @Operation(summary = "Create a new exchange rate")
    public ResponseEntity<ApiResponseWrapper<TauxChangeDto>> createTauxChange(@Valid @RequestBody TauxChangeDto dto) {
        TauxChangeDto created = taux_service.createTauxChange(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseWrapper.success(created, "Exchange rate created successfully"));
    }

    @GetMapping
    @Operation(summary = "List all exchange rates for the current tenant")
    public ResponseEntity<ApiResponseWrapper<List<TauxChangeDto>>> getTenantRates() {
        List<TauxChangeDto> rates = taux_service.getTenantRates();
        return ResponseEntity.ok(ApiResponseWrapper.success(rates, "Exchange rates list retrieved successfully"));
    }

    @GetMapping("/latest")
    @Operation(summary = "Get the latest rate for a currency pair at a specific date")
    public ResponseEntity<ApiResponseWrapper<TauxChangeDto>> getLatestRate(
            @RequestParam UUID sourceId,
            @RequestParam UUID targetId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {

        LocalDateTime targetDate = date != null ? date : LocalDateTime.now();
        return taux_service.getLatestRate(sourceId, targetId, targetDate)
                .map(rate -> ResponseEntity.ok(ApiResponseWrapper.success(rate, "Rate found")))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponseWrapper.error("No rate found for this pair and date", 404)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an exchange rate")
    public ResponseEntity<ApiResponseWrapper<Void>> deleteTauxChange(@PathVariable UUID id) {
        taux_service.deleteTauxChange(id);
        return ResponseEntity.ok(ApiResponseWrapper.success(null, "Exchange rate deleted successfully"));
    }
}
