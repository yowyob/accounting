package com.yowyob.erp.accounting.application.service.impl;
import com.yowyob.erp.accounting.domain.port.in.AgenceUseCase;

import com.yowyob.erp.accounting.infrastructure.web.dto.AgenceDto;
import com.yowyob.erp.accounting.domain.model.Agence;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.AgenceRepository;
import com.yowyob.erp.accounting.domain.port.in.AgenceUseCase;
import com.yowyob.erp.shared.domain.exception.ResourceNotFoundException;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Reactive Implementation of AgenceUseCase.
 * Handles CRUD operations with organization isolation.
 * 
 * @author ALD
 * @date 03.01.2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgenceServiceImpl implements AgenceUseCase {

        private final AgenceRepository agence_repository;

        @Override
        @Transactional
        public Mono<AgenceDto> createAgence(AgenceDto agence_dto) {
                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> {
                                        log.info("Creating new agency '{}' for organization {}", agence_dto.getName(),
                                                        organization_id);
                                        Agence agence = Agence.builder()
                                                        .organizationId(organization_id)
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
                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> agence_repository.findById(id)
                                                .filter(a -> a.getOrganizationId().equals(organization_id))
                                                .switchIfEmpty(Mono.error(
                                                                new ResourceNotFoundException("Agence", id.toString())))
                                                .map(this::mapToDto));
        }

        @Override
        public Flux<AgenceDto> getAllAgences() {
                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMapMany(organization_id -> {
                                        log.info("Retrieving all agencies for organization: {}", organization_id);
                                        return agence_repository.findByOrganizationId(organization_id)
                                                        .map(this::mapToDto);
                                })
                                .switchIfEmpty(Flux.defer(() -> {
                                        log.info("No agencies found or organization not set in context");
                                        return Flux.empty();
                                }));
        }

        @Override
        @Transactional
        public Mono<AgenceDto> updateAgence(UUID id, AgenceDto agence_dto) {
                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> agence_repository.findById(id)
                                                .filter(a -> a.getOrganizationId().equals(organization_id))
                                                .switchIfEmpty(Mono.error(
                                                                new ResourceNotFoundException("Agence", id.toString())))
                                                .flatMap(agence -> {
                                                        log.info("Updating agency {} for organization {}", id, organization_id);
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
                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> agence_repository.findById(id)
                                                .filter(a -> a.getOrganizationId().equals(organization_id))
                                                .switchIfEmpty(Mono.error(
                                                                new ResourceNotFoundException("Agence", id.toString())))
                                                .flatMap(agence -> {
                                                        log.warn("Deleting agency {} for organization {}", id, organization_id);
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
                                .organization_id(agence.getOrganizationId())
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
