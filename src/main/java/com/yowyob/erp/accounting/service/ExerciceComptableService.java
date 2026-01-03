package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.ExerciceComptableDto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing Fiscal Years (Exercices Comptables).
 * 
 * @author ALD
 * @date 03.01.2026
 */
public interface ExerciceComptableService {

    /**
     * Creates a new fiscal year for the current tenant.
     * 
     * @param exercice_dto the fiscal year data
     * @return the created fiscal year DTO
     */
    ExerciceComptableDto createExercice(ExerciceComptableDto exercice_dto);

    /**
     * Retrieves a fiscal year by ID within the current tenant.
     * 
     * @param id the fiscal year ID
     * @return the fiscal year DTO
     */
    ExerciceComptableDto getExercice(UUID id);

    /**
     * Retrieves all fiscal years for the current tenant.
     * 
     * @return a list of fiscal year DTOs
     */
    List<ExerciceComptableDto> getAllExercices();

    /**
     * Finds the active fiscal year for a given date.
     * 
     * @param date the date to check
     * @return the active fiscal year DTO
     */
    ExerciceComptableDto getActiveExerciceForDate(LocalDate date);

    /**
     * Updates an existing fiscal year.
     * 
     * @param id           the fiscal year ID
     * @param exercice_dto the new fiscal year data
     * @return the updated fiscal year DTO
     */
    ExerciceComptableDto updateExercice(UUID id, ExerciceComptableDto exercice_dto);

    /**
     * Closes a fiscal year.
     * 
     * @param id the fiscal year ID
     */
    void closeExercice(UUID id);

    /**
     * Deletes a fiscal year.
     * 
     * @param id the fiscal year ID
     */
    void deleteExercice(UUID id);
}
