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
     * Creates a new fiscal year for the current tenant.
     * 
     * @param exercice_dto the fiscal year data
     * @return the created fiscal year DTO
     */
    Mono<ExerciceComptableDto> createExercice(ExerciceComptableDto exercice_dto);

    /**
     * Retrieves a fiscal year by ID within the current tenant.
     * 
     * @param id the fiscal year ID
     * @return the fiscal year DTO
     */
    Mono<ExerciceComptableDto> getExercice(UUID id);

    /**
     * Retrieves all fiscal years for the current tenant.
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
}
