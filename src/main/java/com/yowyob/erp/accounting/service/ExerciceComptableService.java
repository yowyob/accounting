package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.ExerciceComptableDto;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Reactive Service interface for managing Fiscal Years (Exercices Comptables).
 */
public interface ExerciceComptableService {

    /**
     * Creates a new fiscal year for the current organization.
     * 
     * @param exercice_dto the fiscal year data
     * @return the created fiscal year DTO
     */
    Mono<ExerciceComptableDto> createExercice(ExerciceComptableDto exercice_dto);

    /**
     * Retrieves a fiscal year by ID within the current organization.
     * 
     * @param id the fiscal year ID
     * @return the fiscal year DTO
     */
    Mono<ExerciceComptableDto> getExercice(UUID id);

    /**
     * Retrieves all fiscal years for the current organization.
     * 
     * @return a list of fiscal year DTOs
     */
    Mono<java.util.List<ExerciceComptableDto>> getAllExercices();

    /**
     * Finds the active fiscal year for a given date.
     * 
     * @param date the date to check
     * @return the active fiscal year DTO
     */
    Mono<ExerciceComptableDto> getActiveExerciceForDate(LocalDate date);

    /**
     * Updates an existing fiscal year.
     * 
     * @param id           the fiscal year ID
     * @param exercice_dto the new fiscal year data
     * @return the updated fiscal year DTO
     */
    Mono<ExerciceComptableDto> updateExercice(UUID id, ExerciceComptableDto exercice_dto);

    /**
     * Closes a fiscal year.
     * 
     * @param id the fiscal year ID
     */
    Mono<Void> closeExercice(UUID id);

    /**
     * Deletes a fiscal year.
     * 
     * @param id the fiscal year ID
     */
    Mono<Void> deleteExercice(UUID id);

    /**
     * Deactivates a fiscal year (soft delete).
     * 
     * @param id the fiscal year ID
     */
    Mono<Void> deactivateExercice(UUID id);

    /**
     * Retrieves all periods for a given fiscal year.
     * 
     * @param exerciceId the fiscal year ID
     * @return a list of period DTOs
     */
    Mono<java.util.List<com.yowyob.erp.accounting.dto.PeriodeComptableDto>> getPeriodesByExercice(UUID exerciceId);
}
