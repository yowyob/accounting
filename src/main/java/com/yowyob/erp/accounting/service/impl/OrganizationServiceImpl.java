package com.yowyob.erp.accounting.service.impl;

import com.yowyob.erp.accounting.dto.OrganizationDto;
import com.yowyob.erp.accounting.entity.Organization;
import com.yowyob.erp.accounting.repository.OrganizationRepository;
import com.yowyob.erp.accounting.service.OrganizationService;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of OrganizationService.
 * Handles CRUD operations and mapping between Entity and DTO.
 * 
 * @author ALD
 * @date 03.01.2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organization_repository;

    @Override
    @Transactional
    public OrganizationDto createOrganization(OrganizationDto organization_dto) {
        log.info("Creating new organization: {}", organization_dto.getName());
        Organization organization = Organization.builder()
                .name(organization_dto.getName())
                .description(organization_dto.getDescription())
                .address(organization_dto.getAddress())
                .tax_id(organization_dto.getTax_id())
                .build();

        Organization saved = organization_repository.save(organization);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationDto getOrganization(UUID id) {
        return organization_repository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationDto> getAllOrganizations() {
        return organization_repository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrganizationDto updateOrganization(UUID id, OrganizationDto organization_dto) {
        log.info("Updating organization ID: {}", id);
        Organization organization = organization_repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", id.toString()));

        organization.setName(organization_dto.getName());
        organization.setDescription(organization_dto.getDescription());
        organization.setAddress(organization_dto.getAddress());
        organization.setTax_id(organization_dto.getTax_id());

        Organization updated = organization_repository.save(organization);
        return mapToDto(updated);
    }

    @Override
    @Transactional
    public void deleteOrganization(UUID id) {
        log.warn("Deleting organization ID: {}", id);
        if (!organization_repository.existsById(id)) {
            throw new ResourceNotFoundException("Organization", id.toString());
        }
        organization_repository.deleteById(id);
    }

    /**
     * Maps an Organization entity to OrganizationDto.
     * 
     * @param organization the entity to map
     * @return the mapped DTO
     */
    private OrganizationDto mapToDto(Organization organization) {
        return OrganizationDto.builder()
                .id(organization.getId())
                .name(organization.getName())
                .description(organization.getDescription())
                .address(organization.getAddress())
                .tax_id(organization.getTax_id())
                .created_at(organization.getCreated_at())
                .updated_at(organization.getUpdated_at())
                .build();
    }
}
