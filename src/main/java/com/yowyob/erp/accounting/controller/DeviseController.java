package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.dto.DeviseDto;
import com.yowyob.erp.accounting.service.DeviseService;
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
 * REST Controller for managing currencies.
 * 
 * @author ALD
 * @date 30.09.25
 */
@RestController
@RequestMapping("/api/accounting/currencies")
@RequiredArgsConstructor
@Tag(name = "Currency Management", description = "Endpoints for managing global currencies")
public class DeviseController {

    private final DeviseService devise_service;

    @PostMapping
    @Operation(summary = "Create a new currency")
    public ResponseEntity<ApiResponseWrapper<DeviseDto>> createDevise(@Valid @RequestBody DeviseDto dto) {
        DeviseDto created = devise_service.createDevise(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseWrapper.success(created, "Currency created successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing currency")
    public ResponseEntity<ApiResponseWrapper<DeviseDto>> updateDevise(@PathVariable UUID id,
            @Valid @RequestBody DeviseDto dto) {
        DeviseDto updated = devise_service.updateDevise(id, dto);
        return ResponseEntity.ok(ApiResponseWrapper.success(updated, "Currency updated successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get currency by ID")
    public ResponseEntity<ApiResponseWrapper<DeviseDto>> getDevise(@PathVariable UUID id) {
        return devise_service.getDevise(id)
                .map(devise -> ResponseEntity.ok(ApiResponseWrapper.success(devise, "Currency found")))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponseWrapper.error("Currency not found", 404)));
    }

    @GetMapping
    @Operation(summary = "List all currencies")
    public ResponseEntity<ApiResponseWrapper<List<DeviseDto>>> getAllDevises(
            @RequestParam(required = false, defaultValue = "false") boolean onlyActive) {
        List<DeviseDto> devises = onlyActive ? devise_service.getActiveDevises() : devise_service.getAllDevises();
        return ResponseEntity.ok(ApiResponseWrapper.success(devises, "Currencies list retrieved successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a currency")
    public ResponseEntity<ApiResponseWrapper<Void>> deleteDevise(@PathVariable UUID id) {
        devise_service.deleteDevise(id);
        return ResponseEntity.ok(ApiResponseWrapper.success(null, "Currency deleted successfully"));
    }
}
