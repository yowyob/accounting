package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.dto.TauxChangeDto;
import com.yowyob.erp.accounting.service.TauxChangeService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.yowyob.erp.config.tenant.ReactiveTenantContext;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Reactive Controller for managing exchange rates.
 */
@RestController
@RequestMapping("/api/accounting/exchange-rates")
@RequiredArgsConstructor
@Tag(name = "Exchange Rate Management", description = "Endpoints for managing tenant-specific exchange rates")
@Slf4j
public class TauxChangeController {

        private final TauxChangeService taux_service;

        /**
         * Creates a new exchange rate.
         */
        @PostMapping
        @Operation(summary = "Create a new exchange rate")
        public Mono<ResponseEntity<ApiResponseWrapper<TauxChangeDto>>> createTauxChange(
                        @Valid @RequestBody TauxChangeDto dto) {
                log.info("Received request to create exchange rate: {}", dto);
                return taux_service.createTauxChange(dto)
                                .map(created -> ResponseEntity.status(HttpStatus.CREATED)
                                                .body(ApiResponseWrapper.success(created,
                                                                "Exchange rate created successfully")))
                                .switchIfEmpty(Mono.error(new RuntimeException(
                                                "Service returned empty result (possibly missing tenant context)")))
                                .doOnError(e -> log.error("Error creating exchange rate", e))
                                .contextWrite(ReactiveTenantContext.captureFromThreadLocal());
        }

        /**
         * Lists all exchange rates for the current tenant.
         */
        @GetMapping
        @Operation(summary = "List all exchange rates for the current tenant")
        public Mono<ResponseEntity<ApiResponseWrapper<List<TauxChangeDto>>>> getTenantRates() {
                return taux_service.getTenantRates()
                                .map(rates -> ResponseEntity
                                                .ok(ApiResponseWrapper.success(rates,
                                                                "Exchange rates list retrieved successfully")));
        }

        /**
         * Retrieves the latest rate for a currency pair.
         */
        @GetMapping("/latest")
        @Operation(summary = "Get the latest rate for a currency pair at a specific date")
        public Mono<ResponseEntity<ApiResponseWrapper<TauxChangeDto>>> getLatestRate(
                        @RequestParam UUID sourceId,
                        @RequestParam UUID targetId,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {

                LocalDateTime targetDate = date != null ? date : LocalDateTime.now();
                return taux_service.getLatestRate(sourceId, targetId, targetDate)
                                .map(rate -> ResponseEntity.ok(ApiResponseWrapper.success(rate, "Rate found")))
                                .switchIfEmpty(
                                                Mono.error(new ResourceNotFoundException(
                                                                "Exchange rate not found for this pair and date")));
        }

        /**
         * Deletes an exchange rate by its ID.
         */
        @DeleteMapping("/{id}")
        @Operation(summary = "Delete an exchange rate")
        public Mono<ResponseEntity<ApiResponseWrapper<Void>>> deleteTauxChange(@PathVariable UUID id) {
                return taux_service.deleteTauxChange(id)
                                .then(Mono.fromCallable(() -> ResponseEntity
                                                .ok(ApiResponseWrapper.success(null,
                                                                "Exchange rate deleted successfully"))));
        }
}
