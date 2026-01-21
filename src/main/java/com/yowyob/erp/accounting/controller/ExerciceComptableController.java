package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.dto.ExerciceComptableDto;
import com.yowyob.erp.accounting.service.ExerciceComptableService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for managing Fiscal Years (Exercices Comptables).
 * 
 * @author ALD
 * @date 03.01.2026
 */
@RestController
@RequestMapping("/api/accounting/exercices")
@RequiredArgsConstructor
@Tag(name = "Accounting Fiscal Years", description = "Endpoints for managing fiscal years (exercices comptables)")
public class ExerciceComptableController {

    private final ExerciceComptableService exercice_service;

    @PostMapping
    @Operation(summary = "Create a new fiscal year")
    public ResponseEntity<ApiResponseWrapper<ExerciceComptableDto>> createExercice(
            @RequestBody ExerciceComptableDto exercice_dto) {
        ExerciceComptableDto created = exercice_service.createExercice(exercice_dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseWrapper.success(created, "Fiscal year created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get fiscal year by ID")
    public ResponseEntity<ApiResponseWrapper<ExerciceComptableDto>> getExercice(@PathVariable UUID id) {
        ExerciceComptableDto exercice = exercice_service.getExercice(id);
        return ResponseEntity.ok(ApiResponseWrapper.success(exercice, "Fiscal year found"));
    }

    @GetMapping
    @Operation(summary = "Get all fiscal years for current tenant")
    public ResponseEntity<ApiResponseWrapper<List<ExerciceComptableDto>>> getAllExercices() {
        List<ExerciceComptableDto> exercices = exercice_service.getAllExercices();
        return ResponseEntity.ok(ApiResponseWrapper.success(exercices, "Fiscal years list retrieved"));
    }

    @GetMapping("/active")
    @Operation(summary = "Get active fiscal year for a given date")
    public ResponseEntity<ApiResponseWrapper<ExerciceComptableDto>> getActiveExercice(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        ExerciceComptableDto active = exercice_service.getActiveExerciceForDate(date);
        return ResponseEntity.ok(ApiResponseWrapper.success(active, "Active fiscal year found"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a fiscal year")
    public ResponseEntity<ApiResponseWrapper<ExerciceComptableDto>> updateExercice(@PathVariable UUID id,
            @RequestBody ExerciceComptableDto exercice_dto) {
        ExerciceComptableDto updated = exercice_service.updateExercice(id, exercice_dto);
        return ResponseEntity.ok(ApiResponseWrapper.success(updated, "Fiscal year updated successfully"));
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Close a fiscal year")
    public ResponseEntity<ApiResponseWrapper<Void>> closeExercice(@PathVariable UUID id) {
        exercice_service.closeExercice(id);
        return ResponseEntity.ok(ApiResponseWrapper.success(null, "Fiscal year closed successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a fiscal year")
    public ResponseEntity<ApiResponseWrapper<Void>> deleteExercice(@PathVariable UUID id) {
        exercice_service.deleteExercice(id);
        return ResponseEntity.ok(ApiResponseWrapper.success(null, "Fiscal year deleted successfully"));
    }
}
