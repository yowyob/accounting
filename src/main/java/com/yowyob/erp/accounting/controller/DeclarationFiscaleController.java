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
    public ResponseEntity<ApiResponseWrapper<DeclarationFiscaleDto>> saveDeclaration(
            @Valid @RequestBody DeclarationFiscaleDto dto) {
        try {
            DeclarationFiscaleDto saved = declaration_service.saveDeclaration(dto);
            log.info("Tax declaration saved: {}", saved.getType_declaration());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponseWrapper.success(saved, "Tax declaration saved successfully"));
        } catch (Exception e) {
            log.error("Error saving tax declaration: {}", e.getMessage());
            throw new BusinessException("Error saving tax declaration: " + e.getMessage());
        }
    }

    /**
     * Retrieves a declaration by its ID.
     * 
     * @param id the declaration ID
     * @return the found declaration
     */
    @Operation(summary = "Get a tax declaration by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseWrapper<DeclarationFiscaleDto>> getById(@PathVariable UUID id) {
        log.info("Retrieving tax declaration by ID: {}", id);
        return declaration_service.getById(id)
                .map(dto -> ResponseEntity.ok(ApiResponseWrapper.success(dto)))
                .orElseThrow(() -> new ResourceNotFoundException("Tax declaration", id.toString()));
    }

    /**
     * Lists all declarations for the current tenant.
     * 
     * @return list of declarations
     */
    @Operation(summary = "List all tax declarations")
    @GetMapping
    public ResponseEntity<ApiResponseWrapper<List<DeclarationFiscaleDto>>> getAll() {
        log.info("Retrieving all tax declarations");
        List<DeclarationFiscaleDto> list = declaration_service.getAll();
        return ResponseEntity.ok(ApiResponseWrapper.success(list));
    }

    /**
     * Lists declarations by type.
     * 
     * @param type the declaration type
     * @return list of declarations
     */
    @Operation(summary = "Filter tax declarations by type")
    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponseWrapper<List<DeclarationFiscaleDto>>> getByType(@PathVariable String type) {
        log.info("Retrieving tax declarations of type: {}", type);
        List<DeclarationFiscaleDto> list = declaration_service.getByType(type);
        return ResponseEntity.ok(ApiResponseWrapper.success(list));
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
    public ResponseEntity<ApiResponseWrapper<List<DeclarationFiscaleDto>>> getByPeriodRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        if (start.isAfter(end)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseWrapper.error("Start date must be before end date"));
        }
        log.info("Searching tax declarations between {} and {}", start, end);
        List<DeclarationFiscaleDto> list = declaration_service.getByPeriodRange(start, end);
        return ResponseEntity.ok(ApiResponseWrapper.success(list));
    }

    /**
     * Deletes a tax declaration.
     * 
     * @param id the declaration ID
     * @return success message
     */
    @Operation(summary = "Delete a tax declaration")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseWrapper<Void>> delete(@PathVariable UUID id) {
        try {
            declaration_service.delete(id);
            log.info("Tax declaration deleted: {}", id);
            return ResponseEntity.ok(ApiResponseWrapper.success(null, "Tax declaration deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting tax declaration: {}", e.getMessage());
            throw new ResourceNotFoundException("Tax declaration", id.toString());
        }
    }
}
