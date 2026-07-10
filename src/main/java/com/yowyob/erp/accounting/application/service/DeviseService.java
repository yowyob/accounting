package com.yowyob.erp.accounting.application.service;
import com.yowyob.erp.accounting.domain.port.in.DeviseUseCase;

import com.yowyob.erp.accounting.infrastructure.web.dto.DeviseDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.JournalAuditDto;
import com.yowyob.erp.accounting.domain.model.Devise;
import com.yowyob.erp.accounting.domain.model.JournalAudit;
import com.yowyob.erp.accounting.domain.model.Organization;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.DeviseRepository;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.JournalAuditRepository;
import com.yowyob.erp.shared.domain.exception.ResourceNotFoundException;
import com.yowyob.erp.config.kafka.KafkaMessageService;
import com.yowyob.erp.config.redis.RedisService;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Reactive Service for managing currencies.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeviseService implements DeviseUseCase {

        private final DeviseRepository devise_repository;
        private final JournalAuditRepository audit_repository;
        private final RedisService redis_service;
        private final KafkaMessageService kafka_service;

        private static final String CACHE_DEVISE_ALL = "devise:all";
        private static final String CACHE_DEVISE_ACTIVE = "devise:active";
        private static final String CACHE_DEVISE_SINGLE = "devise:single:";

        @Transactional
        public Mono<DeviseDto> createDevise(DeviseDto dto) {
                return ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system")
                                .flatMap(user -> devise_repository.existsByCode(dto.getCode())
                                                .flatMap(exists -> {
                                                        if (Boolean.TRUE.equals(exists)) {
                                                                return Mono.error(
                                                                                new IllegalArgumentException(
                                                                                                "Currency code already in use: "
                                                                                                                + dto.getCode()));
                                                        }

                                                        Devise entity = Devise.builder()
                                                                        .id(UUID.randomUUID())
                                                                        .code(dto.getCode())
                                                                        .nom(dto.getNom())
                                                                        .symbole(dto.getSymbole())
                                                                        .est_nationale(dto.isEst_nationale())
                                                                        .actif(true)
                                                                        .created_at(LocalDateTime.now())
                                                                        .updated_at(LocalDateTime.now())
                                                                        .isNew(true)
                                                                        .build();

                                                        return devise_repository.save(entity)
                                                                        .flatMap(saved -> ReactiveOrganizationContext
                                                                                        .getCurrentOrganizationAsOrganization()
                                                                                        .flatMap(organization -> logAudit(
                                                                                                        organization, user,
                                                                                                        "DEVISE_CREATED",
                                                                                                        "Creation of currency "
                                                                                                                        + dto.getCode()))
                                                                                        .then(invalidateCache())
                                                                                        .thenReturn(mapToDto(saved)));
                                                }));
        }

        @Transactional
        public Mono<DeviseDto> updateDevise(UUID id, DeviseDto dto) {
                return ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system")
                                .flatMap(user -> devise_repository.findById(id)
                                                .switchIfEmpty(Mono.error(
                                                                new ResourceNotFoundException("Devise", id.toString())))
                                                .flatMap(existing -> devise_repository.existsByCode(dto.getCode())
                                                                .flatMap(exists -> {
                                                                        if (!existing.getCode().equals(dto.getCode())
                                                                                        && Boolean.TRUE.equals(
                                                                                                        exists)) {
                                                                                return Mono.error(
                                                                                                new IllegalArgumentException(
                                                                                                                "Currency code already in use: "
                                                                                                                                + dto.getCode()));
                                                                        }

                                                                        if (dto.getUpdated_at() != null
                                                                                        && existing.getUpdated_at() != null
                                                                                        && existing.getUpdated_at()
                                                                                                        .isAfter(dto.getUpdated_at())) {
                                                                                return Mono.error(
                                                                                                new com.yowyob.erp.shared.domain.exception.ConflictException(
                                                                                                                "La devise a été modifiée sur le serveur (conflit offline)"));
                                                                        }

                                                                        existing.setCode(dto.getCode());
                                                                        existing.setNom(dto.getNom());
                                                                        existing.setSymbole(dto.getSymbole());
                                                                        existing.setEst_nationale(
                                                                                        dto.isEst_nationale());
                                                                        existing.setActif(dto.isActif());
                                                                        existing.setUpdated_at(LocalDateTime.now());
                                                                        existing.setNotNew();

                                                                        return devise_repository.save(existing)
                                                                                        .flatMap(saved -> ReactiveOrganizationContext
                                                                                                        .getCurrentOrganizationAsOrganization()
                                                                                                        .flatMap(organization -> logAudit(
                                                                                                                        organization,
                                                                                                                        user,
                                                                                                                        "DEVISE_UPDATED",
                                                                                                                        "Update of currency "
                                                                                                                                        + dto.getCode()))
                                                                                                        .then(invalidateCache())
                                                                                                        .then(redis_service
                                                                                                                        .delete(CACHE_DEVISE_SINGLE
                                                                                                                                        + id))
                                                                                                        .thenReturn(mapToDto(
                                                                                                                        saved)));
                                                                })));
        }

        public Mono<DeviseDto> getDevise(UUID id) {
                String cache_key = CACHE_DEVISE_SINGLE + id;

                return redis_service.get(cache_key, DeviseDto.class)
                                .switchIfEmpty(devise_repository.findById(id)
                                                .map(this::mapToDto)
                                                .flatMap(dto -> redis_service
                                                                .save(cache_key, dto, Duration.ofMinutes(30))
                                                                .thenReturn(dto)));
        }

        @SuppressWarnings("unchecked")
        public Mono<List<DeviseDto>> getAllDevises() {
                return redis_service.get(CACHE_DEVISE_ALL, List.class)
                                .map(list -> (List<DeviseDto>) list)
                                .switchIfEmpty(devise_repository.findAll()
                                                .map(this::mapToDto)
                                                .collectList()
                                                .flatMap(devises -> redis_service
                                                                .save(CACHE_DEVISE_ALL, devises, Duration.ofMinutes(15))
                                                                .thenReturn(devises)));
        }

        @SuppressWarnings("unchecked")
        public Mono<List<DeviseDto>> getActiveDevises() {
                return redis_service.get(CACHE_DEVISE_ACTIVE, List.class)
                                .map(list -> (List<DeviseDto>) list)
                                .switchIfEmpty(devise_repository.findByActifTrue()
                                                .map(this::mapToDto)
                                                .collectList()
                                                .flatMap(devises -> redis_service
                                                                .save(CACHE_DEVISE_ACTIVE, devises,
                                                                                Duration.ofMinutes(15))
                                                                .thenReturn(devises)));
        }

        @Transactional
        public Mono<Void> deleteDevise(UUID id) {
                return ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system")
                                .flatMap(user -> devise_repository.findById(id)
                                                .switchIfEmpty(Mono.error(
                                                                new ResourceNotFoundException("Devise", id.toString())))
                                                .flatMap(devise -> devise_repository.delete(devise)
                                                                .then(ReactiveOrganizationContext.getCurrentOrganizationAsOrganization()
                                                                                .flatMap(organization -> logAudit(organization,
                                                                                                user, "DEVISE_DELETED",
                                                                                                "Deletion of currency "
                                                                                                                + devise.getCode())))
                                                                .then(invalidateCache())
                                                                .then(redis_service.delete(CACHE_DEVISE_SINGLE + id))
                                                                .then()));
        }

        private Mono<Void> invalidateCache() {
                return redis_service.delete(CACHE_DEVISE_ALL)
                                .then(redis_service.delete(CACHE_DEVISE_ACTIVE))
                                .then();
        }

        private DeviseDto mapToDto(Devise entity) {
                return DeviseDto.builder()
                                .id(entity.getId())
                                .code(entity.getCode())
                                .nom(entity.getNom())
                                .symbole(entity.getSymbole())
                                .est_nationale(entity.isEst_nationale())
                                .actif(entity.isActif())
                                .created_at(entity.getCreated_at())
                                .updated_at(entity.getUpdated_at())
                                .build();
        }

        private Mono<Void> logAudit(Organization organization, String utilisateur, String action, String details) {
                JournalAudit audit = JournalAudit.builder()
                                .id(UUID.randomUUID())
                                .organizationId(organization.getId())
                                .action(action)
                                .utilisateur(utilisateur)
                                .details(details)
                                .date_action(LocalDateTime.now())
                                .created_at(LocalDateTime.now())
                                .updated_at(LocalDateTime.now())
                                .created_by("system")
                                .updated_by("system")
                                .build();

                return audit_repository.save(audit)
                                .flatMap(saved -> {
                                        JournalAuditDto auditDto = JournalAuditDto.builder()
                                                        .action(saved.getAction())
                                                        .utilisateur(saved.getUtilisateur())
                                                        .details(saved.getDetails())
                                                        .date_action(saved.getDate_action())
                                                        .build();

                                        return kafka_service.sendAuditLog(auditDto, organization.getId(), action);
                                });
        }
}
