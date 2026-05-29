package com.yowyob.erp.accounting.application.service;

import com.yowyob.erp.accounting.infrastructure.web.dto.OrganizationDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive Service interface for managing Organizations.
 */
public interface OrganizationService {

    /**
     * Creates a new organization.
     */
    Mono<OrganizationDto> createOrganization(OrganizationDto organization_dto);

    /**
     * Retrieves an organization by ID.
     */
    Mono<OrganizationDto> getOrganization(UUID id);

    /**
     * Retrieves all organizations.
     */
    Flux<OrganizationDto> getAllOrganizations();

    /**
     * Updates an existing organization.
     */
    Mono<OrganizationDto> updateOrganization(UUID id, OrganizationDto organization_dto);

    /**
     * Deletes an organization.
     */
    Mono<Void> deleteOrganization(UUID id);
}
