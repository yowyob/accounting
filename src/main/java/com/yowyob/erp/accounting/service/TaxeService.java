package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.JournalAuditDto;
import com.yowyob.erp.accounting.dto.TaxeDto;
import com.yowyob.erp.accounting.entity.JournalAudit;
import com.yowyob.erp.accounting.entity.Taxe;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.accounting.repository.JournalAuditRepository;
import com.yowyob.erp.accounting.repository.TaxeRepository;
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
 * Reactive Service for managing taxes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaxeService {

        private final TaxeRepository taxe_repository;
        private final JournalAuditRepository audit_repository;
        private final RedisService redis_service;
        private final KafkaMessageService kafka_service;

        private static final String CACHE_TAXE_ALL = "taxe:all:";
        private static final String CACHE_TAXE_ACTIVE = "taxe:active:";
        private static final String CACHE_TAXE_SINGLE = "taxe:single:";

        @Transactional
        public Mono<TaxeDto> createTaxe(TaxeDto dto) {
                return Mono.zip(ReactiveTenantContext.getTenantId(), ReactiveTenantContext.getCurrentTenantAsTenant())
                                .flatMap(tuple -> {
                                        UUID tenant_id = tuple.getT1();
                                        Tenant tenant = tuple.getT2();

                                        return taxe_repository.existsByTenant_IdAndCode(tenant_id, dto.getCode())
                                                        .flatMap(exists -> {
                                                                if (Boolean.TRUE.equals(exists)) {
                                                                        return Mono.error(
                                                                                        new IllegalArgumentException(
                                                                                                        "Tax code already in use: "
                                                                                                                        + dto.getCode()));
                                                                }

                                                                Taxe entity = Taxe.builder()
                                                                                .tenantId(tenant_id)
                                                                                .code(dto.getCode())
                                                                                .libelle(dto.getLibelle())
                                                                                .taux(dto.getTaux())
                                                                                .compte_collecte(dto
                                                                                                .getCompte_collecte())
                                                                                .compte_deductible(dto
                                                                                                .getCompte_deductible())
                                                                                .pays(dto.getPays())
                                                                                .date_debut_validite(dto
                                                                                                .getDate_debut_validite())
                                                                                .date_fin_validite(dto
                                                                                                .getDate_fin_validite())
                                                                                .actif(true)
                                                                                .created_at(LocalDateTime.now())
                                                                                .build();

                                                                return taxe_repository.save(entity)
                                                                                .flatMap(
                                                                                                saved -> ReactiveTenantContext
                                                                                                                .getCurrentUser()
                                                                                                                .defaultIfEmpty("system")
                                                                                                                .flatMap(user -> logAudit(
                                                                                                                                tenant,
                                                                                                                                user,
                                                                                                                                "TAXE_CREATED",
                                                                                                                                "Creation of tax "
                                                                                                                                                + dto.getCode()))
                                                                                                                .then(invalidateCache(
                                                                                                                                tenant_id))
                                                                                                                .thenReturn(mapToDto(
                                                                                                                                saved)));
                                                        });
                                });
        }

        @Transactional
        public Mono<TaxeDto> updateTaxe(UUID id, TaxeDto dto) {
                return Mono.zip(ReactiveTenantContext.getTenantId(), ReactiveTenantContext.getCurrentTenantAsTenant())
                                .flatMap(tuple -> {
                                        UUID tenant_id = tuple.getT1();
                                        Tenant tenant = tuple.getT2();

                                        return taxe_repository.findByTenant_IdAndId(tenant_id, id)
                                                        .switchIfEmpty(Mono.error(new ResourceNotFoundException("Taxe",
                                                                        id.toString())))
                                                        .flatMap(existing -> taxe_repository
                                                                        .existsByTenant_IdAndCode(tenant_id,
                                                                                        dto.getCode())
                                                                        .flatMap(exists -> {
                                                                                if (!existing.getCode()
                                                                                                .equals(dto.getCode())
                                                                                                && Boolean.TRUE.equals(
                                                                                                                exists)) {
                                                                                        return Mono.error(
                                                                                                        new IllegalArgumentException(
                                                                                                                        "Tax code already in use: "
                                                                                                                                        + dto.getCode()));
                                                                                }

                                                                                existing.setCode(dto.getCode());
                                                                                existing.setLibelle(dto.getLibelle());
                                                                                existing.setTaux(dto.getTaux());
                                                                                existing.setCompte_collecte(dto
                                                                                                .getCompte_collecte());
                                                                                existing.setCompte_deductible(dto
                                                                                                .getCompte_deductible());
                                                                                existing.setPays(dto.getPays());
                                                                                existing.setDate_debut_validite(dto
                                                                                                .getDate_debut_validite());
                                                                                existing.setDate_fin_validite(dto
                                                                                                .getDate_fin_validite());
                                                                                existing.setActif(dto.isActif());

                                                                                return taxe_repository.save(existing)
                                                                                                .flatMap(saved -> ReactiveTenantContext
                                                                                                                .getCurrentUser()
                                                                                                                .defaultIfEmpty("system")
                                                                                                                .flatMap(user -> logAudit(
                                                                                                                                tenant,
                                                                                                                                user,
                                                                                                                                "TAXE_UPDATED",
                                                                                                                                "Update of tax " + dto
                                                                                                                                                .getCode()))
                                                                                                                .then(invalidateCache(
                                                                                                                                tenant_id))
                                                                                                                .then(redis_service
                                                                                                                                .delete(CACHE_TAXE_SINGLE
                                                                                                                                                + tenant_id
                                                                                                                                                + ":"
                                                                                                                                                + id))
                                                                                                                .thenReturn(mapToDto(
                                                                                                                                saved)));
                                                                        }));
                                });
        }

        public Mono<TaxeDto> getTaxe(UUID id) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> {
                                        String cache_key = CACHE_TAXE_SINGLE + tenant_id + ":" + id;
                                        return redis_service.get(cache_key, TaxeDto.class)
                                                        .switchIfEmpty(taxe_repository
                                                                        .findByTenant_IdAndId(tenant_id, id)
                                                                        .map(this::mapToDto)
                                                                        .flatMap(dto -> redis_service
                                                                                        .save(cache_key, dto, Duration
                                                                                                        .ofMinutes(15))
                                                                                        .thenReturn(dto)));
                                });
        }

        @SuppressWarnings("unchecked")
        public Mono<List<TaxeDto>> getAllTaxes() {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> {
                                        String cache_key = CACHE_TAXE_ALL + tenant_id;
                                        return redis_service.get(cache_key, List.class)
                                                        .map(list -> (List<TaxeDto>) list)
                                                        .switchIfEmpty(taxe_repository.findByTenant_Id(tenant_id)
                                                                        .map(this::mapToDto)
                                                                        .collectList()
                                                                        .flatMap(taxes -> redis_service
                                                                                        .save(cache_key, taxes, Duration
                                                                                                        .ofMinutes(10))
                                                                                        .thenReturn(taxes)));
                                });
        }

        @SuppressWarnings("unchecked")
        public Mono<List<TaxeDto>> getActiveTaxes() {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> {
                                        String cache_key = CACHE_TAXE_ACTIVE + tenant_id;
                                        return redis_service.get(cache_key, List.class)
                                                        .map(list -> (List<TaxeDto>) list)
                                                        .switchIfEmpty(taxe_repository
                                                                        .findByTenant_IdAndActifTrue(tenant_id)
                                                                        .map(this::mapToDto)
                                                                        .collectList()
                                                                        .flatMap(taxes -> redis_service
                                                                                        .save(cache_key, taxes, Duration
                                                                                                        .ofMinutes(10))
                                                                                        .thenReturn(taxes)));
                                });
        }

        @Transactional
        public Mono<Void> deleteTaxe(UUID id) {
                return Mono.zip(ReactiveTenantContext.getTenantId(), ReactiveTenantContext.getCurrentTenantAsTenant())
                                .flatMap(tuple -> {
                                        UUID tenant_id = tuple.getT1();
                                        Tenant tenant = tuple.getT2();

                                        return taxe_repository.findByTenant_IdAndId(tenant_id, id)
                                                        .switchIfEmpty(Mono.error(new ResourceNotFoundException("Taxe",
                                                                        id.toString())))
                                                        .flatMap(taxe -> taxe_repository.delete(taxe)
                                                                        .then(ReactiveTenantContext.getCurrentUser()
                                                                                        .defaultIfEmpty("system")
                                                                                        .flatMap(user -> logAudit(
                                                                                                        tenant, user,
                                                                                                        "TAXE_DELETED",
                                                                                                        "Deletion of tax "
                                                                                                                        + taxe.getCode())))
                                                                        .then(invalidateCache(tenant_id))
                                                                        .then(redis_service.delete(CACHE_TAXE_SINGLE
                                                                                        + tenant_id + ":" + id))
                                                                        .then());
                                });
        }

        private Mono<Void> invalidateCache(UUID tenant_id) {
                return redis_service.delete(CACHE_TAXE_ALL + tenant_id)
                                .then(redis_service.delete(CACHE_TAXE_ACTIVE + tenant_id))
                                .then();
        }

        private TaxeDto mapToDto(Taxe entity) {
                return TaxeDto.builder()
                                .id(entity.getId())
                                .code(entity.getCode())
                                .libelle(entity.getLibelle())
                                .taux(entity.getTaux())
                                .compte_collecte(entity.getCompte_collecte())
                                .compte_deductible(entity.getCompte_deductible())
                                .pays(entity.getPays())
                                .date_debut_validite(entity.getDate_debut_validite())
                                .date_fin_validite(entity.getDate_fin_validite())
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
