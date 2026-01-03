package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.OrganizationDto;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing Organizations.
 * 
 * @author ALD
 * @date 03.01.2026
 */
public interface OrganizationService {

    /**
     * Creates a new organization.
     * 
     * @param organization_dto the organization data
     * @return the created organization DTO
     */
    OrganizationDto createOrganization(OrganizationDto organization_dto);

    /**
     * Retrieves an organization by ID.
     * 
     * @param id the organization ID
     * @return the organization DTO
     */
    OrganizationDto getOrganization(UUID id);

    /**
     * Retrieves all organizations.
     * 
     * @return a list of organization DTOs
     */
    List<OrganizationDto> getAllOrganizations();

    /**
     * Updates an existing organization.
     * 
     * @param id               the organization ID
     * @param organization_dto the new organization data
     * @return the updated organization DTO
     */
    OrganizationDto updateOrganization(UUID id, OrganizationDto organization_dto);

    /**
     * Deletes an organization.
     * 
     * @param id the organization ID
     */
    void deleteOrganization(UUID id);
}
