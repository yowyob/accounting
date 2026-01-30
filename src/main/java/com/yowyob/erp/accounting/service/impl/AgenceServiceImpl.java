package com.yowyob.erp.accounting.service.impl;

import com.yowyob.erp.accounting.dto.AgenceDto;
import com.yowyob.erp.accounting.entity.Agence;
import com.yowyob.erp.accounting.repository.AgenceRepository;
import com.yowyob.erp.accounting.service.AgenceService;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import com.yowyob.erp.config.tenant.ReactiveTenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Reactive Implementation of AgenceService.
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
        public Mono<AgenceDto> createAgence(AgenceDto agence_dto) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> {
                                        log.info("Creating new agency '{}' for tenant {}", agence_dto.getName(),
                                                        tenant_id);
                                        Agence agence = Agence.builder()
                                                        .tenantId(tenant_id)
                                                        .name(agence_dto.getName())
                                                        .code(agence_dto.getCode())
                                                        .address(agence_dto.getAddress())
                                                        .city(agence_dto.getCity())
                                                        .country(agence_dto.getCountry())
                                                        .created_at(LocalDateTime.now())
                                                        .updated_at(LocalDateTime.now())
                                                        .build();

                                        return agence_repository.save(agence)
                                                        .map(this::mapToDto);
                                });
        }

        @Override
        public Mono<AgenceDto> getAgence(UUID id) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> agence_repository.findById(id)
                                                .filter(a -> a.getTenantId().equals(tenant_id))
                                                .switchIfEmpty(Mono.error(
                                                                new ResourceNotFoundException("Agence", id.toString())))
                                                .map(this::mapToDto));
        }

        @Override
        public Flux<AgenceDto> getAllAgences() {
                return ReactiveTenantContext.getTenantId()
                                .flatMapMany(tenant_id -> {
                                        log.info("Retrieving all agencies for tenant: {}", tenant_id);
                                        return agence_repository.findByTenantId(tenant_id)
                                                        .map(this::mapToDto);
                                })
                                .switchIfEmpty(Flux.defer(() -> {
                                        log.info("No agencies found or tenant not set in context");
                                        return Flux.empty();
                                }));
        }

        @Override
        @Transactional
        public Mono<AgenceDto> updateAgence(UUID id, AgenceDto agence_dto) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> agence_repository.findById(id)
                                                .filter(a -> a.getTenantId().equals(tenant_id))
                                                .switchIfEmpty(Mono.error(
                                                                new ResourceNotFoundException("Agence", id.toString())))
                                                .flatMap(agence -> {
                                                        log.info("Updating agency {} for tenant {}", id, tenant_id);
                                                        agence.setName(agence_dto.getName());
                                                        agence.setCode(agence_dto.getCode());
                                                        agence.setAddress(agence_dto.getAddress());
                                                        agence.setCity(agence_dto.getCity());
                                                        agence.setCountry(agence_dto.getCountry());
                                                        agence.setUpdated_at(LocalDateTime.now());

                                                        return agence_repository.save(agence)
                                                                        .map(this::mapToDto);
                                                }));
        }

        @Override
        @Transactional
        public Mono<Void> deleteAgence(UUID id) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> agence_repository.findById(id)
                                                .filter(a -> a.getTenantId().equals(tenant_id))
                                                .switchIfEmpty(Mono.error(
                                                                new ResourceNotFoundException("Agence", id.toString())))
                                                .flatMap(agence -> {
                                                        log.warn("Deleting agency {} for tenant {}", id, tenant_id);
                                                        return agence_repository.delete(agence);
                                                }));
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
                                .tenant_id(agence.getTenantId())
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
