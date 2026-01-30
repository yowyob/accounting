package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.DeviseDto;
import com.yowyob.erp.accounting.dto.JournalAuditDto;
import com.yowyob.erp.accounting.entity.Devise;
import com.yowyob.erp.accounting.entity.JournalAudit;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.accounting.repository.DeviseRepository;
import com.yowyob.erp.accounting.repository.JournalAuditRepository;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import com.yowyob.erp.config.kafka.KafkaMessageService;
import com.yowyob.erp.config.redis.RedisService;
import com.yowyob.erp.config.tenant.ReactiveTenantContext;
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
public class DeviseService {

        private final DeviseRepository devise_repository;
        private final JournalAuditRepository audit_repository;
        private final RedisService redis_service;
        private final KafkaMessageService kafka_service;

        private static final String CACHE_DEVISE_ALL = "devise:all";
        private static final String CACHE_DEVISE_ACTIVE = "devise:active";
        private static final String CACHE_DEVISE_SINGLE = "devise:single:";

        @Transactional
        public Mono<DeviseDto> createDevise(DeviseDto dto) {
                return ReactiveTenantContext.getCurrentUser().defaultIfEmpty("system")
                                .flatMap(user -> devise_repository.existsByCode(dto.getCode())
                                                .flatMap(exists -> {
                                                        if (Boolean.TRUE.equals(exists)) {
                                                                return Mono.error(
                                                                                new IllegalArgumentException(
                                                                                                "Currency code already in use: "
                                                                                                                + dto.getCode()));
                                                        }

                                                        Devise entity = Devise.builder()
                                                                        .code(dto.getCode())
                                                                        .nom(dto.getNom())
                                                                        .symbole(dto.getSymbole())
                                                                        .est_nationale(dto.isEst_nationale())
                                                                        .actif(true)
                                                                        .created_at(LocalDateTime.now())
                                                                        .build();

                                                        return devise_repository.save(entity)
                                                                        .flatMap(saved -> ReactiveTenantContext
                                                                                        .getCurrentTenantAsTenant()
                                                                                        .flatMap(tenant -> logAudit(
                                                                                                        tenant, user,
                                                                                                        "DEVISE_CREATED",
                                                                                                        "Creation of currency "
                                                                                                                        + dto.getCode()))
                                                                                        .then(invalidateCache())
                                                                                        .thenReturn(mapToDto(saved)));
                                                }));
        }

        @Transactional
        public Mono<DeviseDto> updateDevise(UUID id, DeviseDto dto) {
                return ReactiveTenantContext.getCurrentUser().defaultIfEmpty("system")
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

                                                                        existing.setCode(dto.getCode());
                                                                        existing.setNom(dto.getNom());
                                                                        existing.setSymbole(dto.getSymbole());
                                                                        existing.setEst_nationale(
                                                                                        dto.isEst_nationale());
                                                                        existing.setActif(dto.isActif());

                                                                        return devise_repository.save(existing)
                                                                                        .flatMap(saved -> ReactiveTenantContext
                                                                                                        .getCurrentTenantAsTenant()
                                                                                                        .flatMap(tenant -> logAudit(
                                                                                                                        tenant,
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
                return ReactiveTenantContext.getCurrentUser().defaultIfEmpty("system")
                                .flatMap(user -> devise_repository.findById(id)
                                                .switchIfEmpty(Mono.error(
                                                                new ResourceNotFoundException("Devise", id.toString())))
                                                .flatMap(devise -> devise_repository.delete(devise)
                                                                .then(ReactiveTenantContext.getCurrentTenantAsTenant()
                                                                                .flatMap(tenant -> logAudit(tenant,
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
                                .build();
        }

        private Mono<Void> logAudit(Tenant tenant, String utilisateur, String action, String details) {
                JournalAudit audit = JournalAudit.builder()
                                .id(UUID.randomUUID())
                                .tenantId(tenant.getId())
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

                                        return kafka_service.sendAuditLog(auditDto, tenant.getId(), action);
                                });
        }
}
