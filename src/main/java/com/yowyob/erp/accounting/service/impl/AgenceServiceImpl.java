package com.yowyob.erp.accounting.service.impl;

import com.yowyob.erp.accounting.dto.AgenceDto;
import com.yowyob.erp.accounting.entity.Agence;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.accounting.repository.AgenceRepository;
import com.yowyob.erp.accounting.service.AgenceService;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import com.yowyob.erp.config.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of AgenceService.
 * Handles CRUD operations with tenant isolation.
 * 
 * @author ALD
 * @date 03.01.2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgenceServiceImpl implements AgenceService {

    private final AgenceRepository agence_repository;

    @Override
    @Transactional
    public AgenceDto createAgence(AgenceDto agence_dto) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        log.info("Creating new agency '{}' for tenant {}", agence_dto.getName(), tenant_id);

        Agence agence = Agence.builder()
                .tenant(new Tenant(tenant_id))
                .name(agence_dto.getName())
                .code(agence_dto.getCode())
                .address(agence_dto.getAddress())
                .city(agence_dto.getCity())
                .country(agence_dto.getCountry())
                .build();

        Agence saved = agence_repository.save(agence);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AgenceDto getAgence(UUID id) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        return agence_repository.findById(id)
                .filter(a -> a.getTenant().getId().equals(tenant_id))
                .map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Agence", id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgenceDto> getAllAgences() {
        UUID tenant_id = TenantContext.getCurrentTenant();
        return agence_repository.findByTenant(new Tenant(tenant_id)).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AgenceDto updateAgence(UUID id, AgenceDto agence_dto) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        log.info("Updating agency {} for tenant {}", id, tenant_id);

        Agence agence = agence_repository.findById(id)
                .filter(a -> a.getTenant().getId().equals(tenant_id))
                .orElseThrow(() -> new ResourceNotFoundException("Agence", id.toString()));

        agence.setName(agence_dto.getName());
        agence.setCode(agence_dto.getCode());
        agence.setAddress(agence_dto.getAddress());
        agence.setCity(agence_dto.getCity());
        agence.setCountry(agence_dto.getCountry());

        Agence updated = agence_repository.save(agence);
        return mapToDto(updated);
    }

    @Override
    @Transactional
    public void deleteAgence(UUID id) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        log.warn("Deleting agency {} for tenant {}", id, tenant_id);

        Agence agence = agence_repository.findById(id)
                .filter(a -> a.getTenant().getId().equals(tenant_id))
                .orElseThrow(() -> new ResourceNotFoundException("Agence", id.toString()));

        agence_repository.delete(agence);
    }

    /**
     * Maps an Agence entity to AgenceDto.
     * 
     * @param agence the entity to map
     * @return the mapped DTO
     */
    private AgenceDto mapToDto(Agence agence) {
        return AgenceDto.builder()
                .id(agence.getId())
                .tenant_id(agence.getTenant().getId())
                .name(agence.getName())
                .code(agence.getCode())
                .address(agence.getAddress())
                .city(agence.getCity())
                .country(agence.getCountry())
                .created_at(agence.getCreated_at())
                .updated_at(agence.getUpdated_at())
                .build();
    }
}
