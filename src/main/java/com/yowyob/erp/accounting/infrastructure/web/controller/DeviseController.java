package com.yowyob.erp.accounting.infrastructure.web.controller;

import com.yowyob.erp.accounting.infrastructure.web.dto.DeviseDto;
import com.yowyob.erp.accounting.domain.port.in.DeviseUseCase;
import com.yowyob.erp.shared.application.service.IdempotentCreateSupport;
import com.yowyob.erp.shared.infrastructure.dto.ApiResponseWrapper;
import com.yowyob.erp.shared.domain.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Reactive Controller for managing global currencies.
 */
@RestController
@RequestMapping("/api/accounting/currencies")
@RequiredArgsConstructor
@Tag(name = "Currency Management", description = "Endpoints for managing global currencies")
@Slf4j
public class DeviseController {

    private final DeviseUseCase devise_service;
    private final IdempotentCreateSupport idempotentCreate;

    /**
     * Creates a new currency.
     */
    @PostMapping
    @Operation(summary = "Create a new currency")
    public Mono<ResponseEntity<ApiResponseWrapper<DeviseDto>>> createDevise(
            @Valid @RequestBody DeviseDto dto,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ReactiveOrganizationContext.getOrganizationId()
                .flatMap(orgId -> idempotentCreate.create(
                        orgId,
                        idempotencyKey,
                        "devise",
                        devise_service::getDevise,
                        () -> devise_service.createDevise(dto),
                        DeviseDto::getId
                ))
                .map(result -> result.alreadyProcessed()
                        ? ResponseEntity.ok(ApiResponseWrapper.success(result.data(), "ALREADY_PROCESSED"))
                        : ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponseWrapper.success(result.data(), "Currency created successfully")))
                .contextWrite(ReactiveOrganizationContext.captureFromThreadLocal());
    }

    /**
     * Updates an existing currency.
     */
    @Operation(summary = "Update an existing currency")
    @PutMapping("/{id}")
    public Mono<ResponseEntity<ApiResponseWrapper<DeviseDto>>> updateDevise(@PathVariable UUID id,
            @Valid @RequestBody DeviseDto dto) {
        return devise_service.updateDevise(id, dto)
                .map(updated -> ResponseEntity
                        .ok(ApiResponseWrapper.success(updated, "Currency updated successfully")))
                .contextWrite(ReactiveOrganizationContext.captureFromThreadLocal());
    }

    /**
     * Retrieves a currency by its ID.
     */
    @Operation(summary = "Get currency by ID")
    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiResponseWrapper<DeviseDto>>> getDevise(@PathVariable UUID id) {
        return devise_service.getDevise(id)
                .map(devise -> ResponseEntity.ok(ApiResponseWrapper.success(devise, "Currency found")))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Currency", id.toString())))
                .contextWrite(ReactiveOrganizationContext.captureFromThreadLocal());
    }

    /**
     * Lists all currencies.
     */
    @Operation(summary = "List all currencies")
    @GetMapping
    public Mono<ResponseEntity<ApiResponseWrapper<List<DeviseDto>>>> getAllDevises(
            @RequestParam(required = false, defaultValue = "false") boolean onlyActive) {
        Mono<List<DeviseDto>> listMono = onlyActive ? devise_service.getActiveDevises()
                : devise_service.getAllDevises();
        return listMono.map(devises -> ResponseEntity
                .ok(ApiResponseWrapper.success(devises, "Currencies list retrieved successfully")))
                .contextWrite(ReactiveOrganizationContext.captureFromThreadLocal());
    }

    /**
     * Deletes a currency by its ID.
     */
    @Operation(summary = "Delete a currency")
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<ApiResponseWrapper<Object>>> deleteDevise(@PathVariable UUID id) {
        return devise_service.deleteDevise(id)
                .then(Mono.fromCallable(
                        () -> ResponseEntity.ok(ApiResponseWrapper.success(null, "Currency deleted successfully"))))
                .contextWrite(ReactiveOrganizationContext.captureFromThreadLocal());
    }
}
