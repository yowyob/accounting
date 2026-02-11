package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.dto.ExerciceComptableDto;
import com.yowyob.erp.accounting.service.ExerciceComptableService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * Reactive REST Controller for managing Fiscal Years (Exercices Comptables).
 */
@RestController
@RequestMapping("/api/accounting/exercices")
@RequiredArgsConstructor
@Tag(name = "Accounting Fiscal Years", description = "Endpoints for managing fiscal years (exercices comptables)")
@Slf4j
public class ExerciceComptableController {

    private final ExerciceComptableService exercice_service;

    @PostMapping
    @Operation(summary = "Create a new fiscal year")
    public Mono<ResponseEntity<ApiResponseWrapper<ExerciceComptableDto>>> createExercice(
            @RequestBody ExerciceComptableDto exercice_dto) {
        return exercice_service.createExercice(exercice_dto)
                .map(created -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponseWrapper.success(created, "Fiscal year created successfully")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get fiscal year by ID")
    public Mono<ResponseEntity<ApiResponseWrapper<ExerciceComptableDto>>> getExercice(@PathVariable UUID id) {
        return exercice_service.getExercice(id)
                .map(exercice -> ResponseEntity.ok(ApiResponseWrapper.success(exercice, "Fiscal year found")));
    }

    @GetMapping("/{id}/periodes")
    @Operation(summary = "Get all periods for a fiscal year")
    public Mono<ResponseEntity<ApiResponseWrapper<List<com.yowyob.erp.accounting.dto.PeriodeComptableDto>>>> getPeriodes(
            @PathVariable UUID id) {
        return exercice_service.getPeriodesByExercice(id)
                .map(periodes -> ResponseEntity.ok(ApiResponseWrapper.success(periodes, "Periods retrieved")));
    }

    @GetMapping
    @Operation(summary = "Get all fiscal years for current tenant")
    public Mono<ResponseEntity<ApiResponseWrapper<List<ExerciceComptableDto>>>> getAllExercices() {
        return exercice_service.getAllExercices()
                .map(exercices -> ResponseEntity
                        .ok(ApiResponseWrapper.success(exercices, "Fiscal years list retrieved")));
    }

    @GetMapping("/active")
    @Operation(summary = "Get active fiscal year for a given date")
    public Mono<ResponseEntity<ApiResponseWrapper<ExerciceComptableDto>>> getActiveExercice(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return exercice_service.getActiveExerciceForDate(date)
                .map(active -> ResponseEntity.ok(ApiResponseWrapper.success(active, "Active fiscal year found")));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a fiscal year")
    public Mono<ResponseEntity<ApiResponseWrapper<ExerciceComptableDto>>> updateExercice(@PathVariable UUID id,
            @RequestBody ExerciceComptableDto exercice_dto) {
        return exercice_service.updateExercice(id, exercice_dto)
                .map(updated -> ResponseEntity
                        .ok(ApiResponseWrapper.success(updated, "Fiscal year updated successfully")));
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Close a fiscal year")
    public Mono<ResponseEntity<ApiResponseWrapper<Object>>> closeExercice(@PathVariable UUID id) {
        log.info("Received request to close fiscal year: {}", id);
        return exercice_service.closeExercice(id)
                .then(Mono.fromCallable(
                        () -> ResponseEntity.ok(ApiResponseWrapper.success(null, "Fiscal year closed successfully"))))
                .contextWrite(com.yowyob.erp.config.tenant.ReactiveTenantContext.captureFromThreadLocal());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a fiscal year")
    public Mono<ResponseEntity<ApiResponseWrapper<Object>>> deleteExercice(@PathVariable UUID id) {
        return exercice_service.deleteExercice(id)
                .then(Mono.fromCallable(
                        () -> ResponseEntity.ok(ApiResponseWrapper.success(null, "Fiscal year deleted successfully"))));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a fiscal year (soft delete)")
    public Mono<ResponseEntity<ApiResponseWrapper<Object>>> deactivateExercice(@PathVariable UUID id) {
        return exercice_service.deactivateExercice(id)
                .then(Mono.fromCallable(
                        () -> ResponseEntity.ok(ApiResponseWrapper.success(null, "Fiscal year deactivated successfully"))));
    }
}
