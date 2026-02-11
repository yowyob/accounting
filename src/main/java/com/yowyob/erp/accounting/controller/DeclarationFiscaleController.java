package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.dto.DeclarationFiscaleDto;
import com.yowyob.erp.accounting.service.DeclarationFiscaleService;
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
 * REST Controller for managing tax declarations (DeclarationFiscale).
 * Provides endpoints for CRUD operations and filtering.
 * 
 * @author Leonel Delmat AZANGUE
 * @date 30.09.25
 */
@RestController
@RequestMapping("/api/accounting/tax-declarations")
@RequiredArgsConstructor
@Tag(name = "Accounting Tax Declarations", description = "Management of VAT, IS, and other tax declarations.")
@SecurityRequirement(name = "BasicAuth")
@Slf4j
public class DeclarationFiscaleController {

    private final DeclarationFiscaleService declaration_service;

    /**
     * Creates or updates a tax declaration.
     * 
     * @param dto the declaration data
     * @return the saved declaration
     */
    @Operation(summary = "Save or update a tax declaration")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Declaration saved successfully", content = @Content(schema = @Schema(implementation = DeclarationFiscaleDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid data provided")
    })
    @PostMapping
    public Mono<ResponseEntity<ApiResponseWrapper<DeclarationFiscaleDto>>> saveDeclaration(
            @Valid @RequestBody DeclarationFiscaleDto dto) {
        return declaration_service.saveDeclaration(dto)
                .map(saved -> {
                    log.info("Tax declaration saved: {}", saved.getType_declaration());
                    return ResponseEntity.status(HttpStatus.CREATED)
                            .body(ApiResponseWrapper.success(saved, "Tax declaration saved successfully"));
                })
                .onErrorResume(e -> {
                    log.error("Error saving tax declaration: {}", e.getMessage());
                    return Mono.error(new BusinessException("Error saving tax declaration: " + e.getMessage()));
                });
    }

    /**
     * Retrieves a declaration by its ID.
     * 
     * @param id the declaration ID
     * @return the found declaration
     */
    @Operation(summary = "Get a tax declaration by ID")
    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiResponseWrapper<DeclarationFiscaleDto>>> getById(@PathVariable UUID id) {
        log.info("Retrieving tax declaration by ID: {}", id);
        return declaration_service.getById(id)
                .map(dto -> ResponseEntity.ok(ApiResponseWrapper.success(dto)))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Tax declaration", id.toString())));
    }

    /**
     * Lists all declarations for the current organization.
     * 
     * @return list of declarations
     */
    @Operation(summary = "List all tax declarations")
    @GetMapping
    public Mono<ResponseEntity<ApiResponseWrapper<List<DeclarationFiscaleDto>>>> getAll() {
        log.info("Retrieving all tax declarations");
        return declaration_service.getAll()
                .collectList()
                .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list)));
    }

    /**
     * Lists declarations by type.
     * 
     * @param type the declaration type
     * @return list of declarations
     */
    @Operation(summary = "Filter tax declarations by type")
    @GetMapping("/type/{type}")
    public Mono<ResponseEntity<ApiResponseWrapper<List<DeclarationFiscaleDto>>>> getByType(@PathVariable String type) {
        log.info("Retrieving tax declarations of type: {}", type);
        return declaration_service.getByType(type)
                .collectList()
                .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list)));
    }

    /**
     * Searches for declarations by period range.
     * 
     * @param start the start date
     * @param end   the end date
     * @return list of declarations
     */
    @Operation(summary = "Search tax declarations by period range")
    @GetMapping("/search")
    public Mono<ResponseEntity<ApiResponseWrapper<List<DeclarationFiscaleDto>>>> getByPeriodRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        if (start.isAfter(end)) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(ApiResponseWrapper.error("Start date must be before end date")));
        }
        log.info("Searching tax declarations between {} and {}", start, end);
        return declaration_service.getByPeriodRange(start, end)
                .collectList()
                .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list)));
    }

    /**
     * Deletes a tax declaration.
     * 
     * @param id the declaration ID
     * @return success message
     */
    @Operation(summary = "Delete a tax declaration")
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<ApiResponseWrapper<Void>>> delete(@PathVariable UUID id) {
        return declaration_service.delete(id)
                .then(Mono.fromCallable(() -> {
                    log.info("Tax declaration deleted: {}", id);
                    return ResponseEntity
                            .ok(ApiResponseWrapper.<Void>success(null, "Tax declaration deleted successfully"));
                }))
                .onErrorResume(e -> {
                    log.error("Error deleting tax declaration: {}", e.getMessage());
                    return Mono.error(new ResourceNotFoundException("Tax declaration", id.toString()));
                });
    }

    /**
     * Generates a tax declaration automatically for a period.
     * 
     * @param type  the declaration type (e.g., "TVA")
     * @param start the start date
     * @param end   the end date
     * @return the generated declaration
     */
    @Operation(summary = "Auto-generate a tax declaration for a period")
    @PostMapping("/generate")
    public Mono<ResponseEntity<ApiResponseWrapper<DeclarationFiscaleDto>>> generate(
            @RequestParam String type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        log.info("Request to generate {} declaration from {} to {}", type, start, end);
        return declaration_service.generateDeclaration(type, start, end)
                .map(generated -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponseWrapper.success(generated, "Tax declaration generated successfully")));
    }
}
