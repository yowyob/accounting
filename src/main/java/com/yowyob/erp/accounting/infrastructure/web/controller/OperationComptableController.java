package com.yowyob.erp.accounting.infrastructure.web.controller;

import com.yowyob.erp.accounting.infrastructure.web.dto.OperationComptableDto;
import com.yowyob.erp.accounting.domain.port.in.OperationComptableUseCase;
import com.yowyob.erp.shared.application.service.IdempotentCreateSupport;
import com.yowyob.erp.shared.infrastructure.dto.ApiResponseWrapper;
import com.yowyob.erp.shared.domain.exception.BusinessException;
import com.yowyob.erp.shared.domain.exception.ResourceNotFoundException;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Reactive Controller for managing accounting operations.
 */
@RestController
@RequestMapping("/api/accounting/operations")
@RequiredArgsConstructor
@Tag(name = "Accounting Operations", description = "Management of accounting operations with Kafka, Redis and multi-tenancy")
@SecurityRequirement(name = "BasicAuth")
@Slf4j
public class OperationComptableController {

    private final OperationComptableUseCase operation_service;
    private final IdempotentCreateSupport idempotentCreate;

    /**
     * Creates a new accounting operation for the current organization.
     */
    @Operation(summary = "Create an accounting operation", description = "Creates a new accounting operation for the current organization.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Operation created successfully", content = @Content(schema = @Schema(implementation = OperationComptableDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid data validation failure")
    })
    @PostMapping
    public Mono<ResponseEntity<ApiResponseWrapper<OperationComptableDto>>> createOperationComptable(
            @Valid @RequestBody OperationComptableDto dto,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ReactiveOrganizationContext.getOrganizationId()
                .flatMap(orgId -> idempotentCreate.create(
                        orgId,
                        idempotencyKey,
                        "operation_comptable",
                        operation_service::getOperation,
                        () -> operation_service.createOperation(dto),
                        OperationComptableDto::getId
                ))
                .map(result -> result.alreadyProcessed()
                        ? ResponseEntity.ok(ApiResponseWrapper.success(result.data(), "ALREADY_PROCESSED"))
                        : ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponseWrapper.success(result.data(),
                                        "Accounting operation created successfully")))
                .onErrorResume(e -> {
                    log.error("❌ Error creating operation: {}", e.getMessage());
                    return Mono.error(new BusinessException("Error during creation: " + e.getMessage()));
                })
                .contextWrite(ReactiveOrganizationContext.captureFromThreadLocal());
    }

    /**
     * Retrieves an accounting operation by its ID.
     */
    @Operation(summary = "Retrieve an accounting operation by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operation found"),
            @ApiResponse(responseCode = "404", description = "Operation not found")
    })
    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiResponseWrapper<OperationComptableDto>>> getOperationComptable(
            @PathVariable UUID id) {
        return operation_service.getOperation(id)
                .map(dto -> ResponseEntity.ok(ApiResponseWrapper.success(dto, "Operation found")))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("OperationComptable", id.toString())));
    }

    /**
     * Lists all accounting operations for the current organization.
     */
    @Operation(summary = "List all accounting operations")
    @GetMapping
    public Mono<ResponseEntity<ApiResponseWrapper<List<OperationComptableDto>>>> getAllOperationsComptables(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        return operation_service.getAllOperations()
                .map(operations -> {
                    if (since != null) {
                        operations = operations.stream()
                                .filter(o -> o.getUpdated_at() != null && !o.getUpdated_at().isBefore(since))
                                .toList();
                    }
                    return ResponseEntity
                            .ok(ApiResponseWrapper.success(operations, "List of accounting operations retrieved"));
                });
    }

    /**
     * Retrieves operations associated with a specific principal account number.
     */
    @Operation(summary = "Retrieve accounting operations by principal account")
    @GetMapping("/by-no-compte")
    public Mono<ResponseEntity<ApiResponseWrapper<List<OperationComptableDto>>>> getOperationsByNoCompte(
            @RequestParam String no_compte) {
        return operation_service.getOperationsByCompte(no_compte)
                .map(operations -> ResponseEntity
                        .ok(ApiResponseWrapper.success(operations, "Accounting operations retrieved")));
    }

    /**
     * Searches for an operation by its type and settlement mode.
     */
    
    @Operation(summary = "Search operation by type and settlement mode")
    @GetMapping("/search")
    public Mono<ResponseEntity<ApiResponseWrapper<OperationComptableDto>>> getOperationByTypeAndMode(
            @RequestParam String type_operation,
            @RequestParam String mode_reglement) {
        return operation_service.getByTypeAndMode(type_operation, mode_reglement)
                .map(dto -> ResponseEntity.ok(ApiResponseWrapper.success(dto, "Operation found")))
                .switchIfEmpty(Mono.error(
                        new ResourceNotFoundException("OperationComptable", type_operation + "-" + mode_reglement)));
    }

    /**
     * Updates an existing accounting operation.
     */
    @Operation(summary = "Update an accounting operation")
    @PutMapping("/{id}")
    public Mono<ResponseEntity<ApiResponseWrapper<OperationComptableDto>>> updateOperationComptable(
            @PathVariable UUID id,
            @Valid @RequestBody OperationComptableDto dto) {
        return operation_service.updateOperation(id, dto)
                .map(updated -> ResponseEntity
                        .ok(ApiResponseWrapper.success(updated, "Accounting operation updated successfully")))
                .onErrorResume(e -> {
                    if (e instanceof ResourceNotFoundException) {
                        return Mono.error(e);
                    }
                    log.error("Error updating operation {}: {}", id, e.getMessage());
                    return Mono.error(new BusinessException("Update error: " + e.getMessage()));
                });
    }

    /**
     * Deletes an accounting operation by ID.
     */
    @Operation(summary = "Delete an accounting operation", description = "Deletes an existing accounting operation by ID.")
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<ApiResponseWrapper<String>>> deleteOperationComptable(@PathVariable UUID id) {
        return operation_service.deleteOperation(id)
                .then(Mono.fromCallable(() -> ResponseEntity
                        .ok(ApiResponseWrapper.success("Accounting operation deleted successfully"))))
                .onErrorResume(e -> {
                    if (e instanceof ResourceNotFoundException) {
                        return Mono.error(e);
                    }
                    log.error("Error deleting operation {}: {}", id, e.getMessage());
                    return Mono.error(new BusinessException("Error during deletion: " + e.getMessage()));
                });
    }
}
