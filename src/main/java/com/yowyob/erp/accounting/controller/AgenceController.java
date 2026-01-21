package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.dto.AgenceDto;
import com.yowyob.erp.accounting.service.AgenceService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for managing Agencies (Agences).
 * 
 * @author ALD
 * @date 03.01.2026
 */
@RestController
@RequestMapping("/api/accounting/agences")
@RequiredArgsConstructor
@Tag(name = "Accounting Agences", description = "Endpoints for branch/agency management")
public class AgenceController {

    private final AgenceService agence_service;

    @PostMapping
    @Operation(summary = "Create a new agency")
    public ResponseEntity<ApiResponseWrapper<AgenceDto>> createAgence(@RequestBody AgenceDto agence_dto) {
        AgenceDto created = agence_service.createAgence(agence_dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseWrapper.success(created, "Agency created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get agency by ID")
    public ResponseEntity<ApiResponseWrapper<AgenceDto>> getAgence(@PathVariable UUID id) {
        AgenceDto agence = agence_service.getAgence(id);
        return ResponseEntity.ok(ApiResponseWrapper.success(agence, "Agency found"));
    }

    @GetMapping
    @Operation(summary = "Get all agencies for current tenant")
    public ResponseEntity<ApiResponseWrapper<List<AgenceDto>>> getAllAgences() {
        List<AgenceDto> agences = agence_service.getAllAgences();
        return ResponseEntity.ok(ApiResponseWrapper.success(agences, "Agencies list retrieved"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an agency")
    public ResponseEntity<ApiResponseWrapper<AgenceDto>> updateAgence(@PathVariable UUID id,
            @RequestBody AgenceDto agence_dto) {
        AgenceDto updated = agence_service.updateAgence(id, agence_dto);
        return ResponseEntity.ok(ApiResponseWrapper.success(updated, "Agency updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an agency")
    public ResponseEntity<ApiResponseWrapper<Void>> deleteAgence(@PathVariable UUID id) {
        agence_service.deleteAgence(id);
        return ResponseEntity.ok(ApiResponseWrapper.success(null, "Agency deleted successfully"));
    }
}
