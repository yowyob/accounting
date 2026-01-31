package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.JournalAuditDto;
import com.yowyob.erp.accounting.dto.TauxChangeDto;
import com.yowyob.erp.accounting.entity.JournalAudit;
import com.yowyob.erp.accounting.entity.TauxChange;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.accounting.repository.DeviseRepository;
import com.yowyob.erp.accounting.repository.JournalAuditRepository;
import com.yowyob.erp.accounting.repository.TauxChangeRepository;
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
 * Reactive Service for managing exchange rates.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TauxChangeService {

        private final TauxChangeRepository taux_repository;
        private final DeviseRepository devise_repository;
        private final JournalAuditRepository audit_repository;
        private final RedisService redis_service;
        private final KafkaMessageService kafka_service;

        private static final String CACHE_TAUX_TENANT = "taux:tenant:";

        @Transactional
        public Mono<TauxChangeDto> createTauxChange(TauxChangeDto dto) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> ReactiveTenantContext.getCurrentTenantAsTenant()
                                                .flatMap(tenant -> ReactiveTenantContext.getCurrentUser()
                                                                .defaultIfEmpty("system")
                                                                .flatMap(user -> Mono.zip(
                                                                                devise_repository.findById(dto
                                                                                                .getDevise_source_id())
                                                                                                .switchIfEmpty(Mono
                                                                                                                .error(new ResourceNotFoundException(
                                                                                                                                "Devise",
                                                                                                                                dto.getDevise_source_id()
                                                                                                                                                .toString()))),
                                                                                devise_repository.findById(dto
                                                                                                .getDevise_cible_id())
                                                                                                .switchIfEmpty(Mono
                                                                                                                .error(new ResourceNotFoundException(
                                                                                                                                "Devise",
                                                                                                                                dto.getDevise_cible_id()
                                                                                                                                                .toString()))))
                                                                                .flatMap(tuple -> {
                                                                                        TauxChange entity = TauxChange
                                                                                                        .builder()
                                                                                                        .id(UUID.randomUUID())
                                                                                                        .tenantId(tenant_id)
                                                                                                        .devise_source_id(
                                                                                                                        tuple.getT1().getId())
                                                                                                        .devise_cible_id(
                                                                                                                        tuple.getT2().getId())
                                                                                                        .taux(dto.getTaux())
                                                                                                        .date_effet(dto.getDate_effet())
                                                                                                        .notes(dto.getNotes())
                                                                                                        .created_at(LocalDateTime
                                                                                                                        .now())
                                                                                                        .isNew(true)
                                                                                                        .build();

                                                                                        return taux_repository
                                                                                                        .save(entity)
                                                                                                        .doOnSubscribe(s -> log
                                                                                                                        .info("Saving new exchange rate"))
                                                                                                        .doOnSuccess(s -> log
                                                                                                                        .info("Exchange rate saved: {}",
                                                                                                                                        s.getId()))
                                                                                                        .flatMap(saved -> logAudit(
                                                                                                                        tenant,
                                                                                                                        user,
                                                                                                                        "TAUX_CHANGE_CREATED",
                                                                                                                        String.format("New rate for %s -> %s: %s",
                                                                                                                                        tuple.getT1().getCode(),
                                                                                                                                        tuple.getT2().getCode(),
                                                                                                                                        dto.getTaux()))
                                                                                                                        .then(invalidateCache(
                                                                                                                                        tenant_id))
                                                                                                                        .then(mapToDto(saved)));
                                                                                }))));
        }

        @SuppressWarnings("unchecked")
        public Mono<List<TauxChangeDto>> getTenantRates() {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> {
                                        String cache_key = CACHE_TAUX_TENANT + tenant_id;
                                        return redis_service.get(cache_key, List.class)
                                                        .map(list -> (List<TauxChangeDto>) list)
                                                        .switchIfEmpty(taux_repository.findByTenant_Id(tenant_id)
                                                                        .flatMap(this::mapToDto)
                                                                        .collectList()
                                                                        .flatMap(rates -> redis_service
                                                                                        .save(cache_key, rates, Duration
                                                                                                        .ofMinutes(15))
                                                                                        .thenReturn(rates)));
                                });
        }

        public Mono<TauxChangeDto> getLatestRate(UUID sourceId, UUID targetId, LocalDateTime date) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> taux_repository
                                                .findMostRecentRate(tenant_id, sourceId, targetId, date)
                                                .flatMap(this::mapToDto));
        }

        @Transactional
        public Mono<Void> deleteTauxChange(UUID id) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> ReactiveTenantContext.getCurrentTenantAsTenant()
                                                .flatMap(tenant -> ReactiveTenantContext.getCurrentUser()
                                                                .defaultIfEmpty("system")
                                                                .flatMap(user -> taux_repository.findById(id)
                                                                                .filter(r -> tenant_id.equals(
                                                                                                r.getTenantId()))
                                                                                .switchIfEmpty(Mono.error(
                                                                                                new ResourceNotFoundException(
                                                                                                                "TauxChange",
                                                                                                                id.toString())))
                                                                                .flatMap(rate -> taux_repository
                                                                                                .delete(rate)
                                                                                                .then(logAudit(tenant,
                                                                                                                user,
                                                                                                                "TAUX_CHANGE_DELETED",
                                                                                                                "Deletion of rate"))
                                                                                                .then(invalidateCache(
                                                                                                                tenant_id))))));
        }

        private Mono<Void> invalidateCache(UUID tenant_id) {
                return redis_service.delete(CACHE_TAUX_TENANT + tenant_id).then();
        }

        private Mono<TauxChangeDto> mapToDto(TauxChange entity) {
                return Mono.zip(
                                devise_repository.findById(entity.getDevise_source_id()),
                                devise_repository.findById(entity.getDevise_cible_id()))
                                .map(tuple -> TauxChangeDto.builder()
                                                .id(entity.getId())
                                                .devise_source_id(entity.getDevise_source_id())
                                                .devise_source_code(tuple.getT1().getCode())
                                                .devise_cible_id(entity.getDevise_cible_id())
                                                .devise_cible_code(tuple.getT2().getCode())
                                                .taux(entity.getTaux())
                                                .date_effet(entity.getDate_effet())
                                                .notes(entity.getNotes())
                                                .created_at(entity.getCreated_at())
                                                .build());
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
