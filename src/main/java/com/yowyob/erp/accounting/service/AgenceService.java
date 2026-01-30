package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.AgenceDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Service interface for managing Agencies (Agences).
 * 
 * @author ALD
 * @date 03.01.2026
 */
public interface AgenceService {

    /**
     * Creates a new agency for the current tenant.
     * 
     * @param agence_dto the agency data
     * @return the created agency DTO
     */
    Mono<AgenceDto> createAgence(AgenceDto agence_dto);

    /**
     * Retrieves an agency by ID within the current tenant.
     * 
     * @param id the agency ID
     * @return the agency DTO
     */
    Mono<AgenceDto> getAgence(UUID id);

    /**
     * Retrieves all agencies for the current tenant.
     * 
     * @return a list of agency DTOs
     */
    Flux<AgenceDto> getAllAgences();

    /**
     * Updates an existing agency within the current tenant.
     * 
     * @param id         the agency ID
     * @param agence_dto the new agency data
     * @return the updated agency DTO
     */
    Mono<AgenceDto> updateAgence(UUID id, AgenceDto agence_dto);

    /**
     * Deletes an agency from the current tenant.
     * 
     * @param id the agency ID
     * @return Mono<Void>
     */
    Mono<Void> deleteAgence(UUID id);
}
