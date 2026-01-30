package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.JournalAuditDto;
import com.yowyob.erp.accounting.dto.PlanComptableDto;
import com.yowyob.erp.accounting.entity.JournalAudit;
import com.yowyob.erp.accounting.entity.PlanComptable;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.accounting.repository.PlanComptableRepository;
import com.yowyob.erp.accounting.repository.PlanComptableTemplateRepository;
import com.yowyob.erp.accounting.repository.JournalAuditRepository;
import com.yowyob.erp.common.exception.BusinessException;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import com.yowyob.erp.common.service.ValidationService;
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
 * Reactive Service for managing the accounting plan (Plan Comptable).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlanComptableService {

        private final PlanComptableRepository account_repository;
        private final PlanComptableTemplateRepository template_repository;
        private final ValidationService validation_service;
        private final KafkaMessageService kafka_service;
        private final RedisService redis_service;
        private final JournalAuditRepository audit_repository;

        private static final String CACHE_ALL = "plancomptable:all:";
        private static final String CACHE_ACTIVE = "plancomptable:active:";
        private static final String CACHE_SINGLE = "plancomptable:id:";
        private static final String CACHE_PREFIX = "plancomptable:prefix:";
        private static final String CACHE_CLASS = "plancomptable:class:";

        @Transactional
        public Mono<Void> initializePlanComptableForTenant(UUID tenant_id) {
                log.info("Initializing accounting plan for tenant: {}", tenant_id);

                return ReactiveTenantContext.getCurrentUser().defaultIfEmpty("system")
                                .flatMap(current_user -> template_repository.findAll()
                                                .map(template -> PlanComptable.builder()
                                                                .tenantId(tenant_id)
                                                                .no_compte(template.getNumero())
                                                                .classe(template.getClasse())
                                                                .libelle(template.getLibelle())
                                                                .notes(template.getNotes())
                                                                .actif(true)
                                                                .created_at(LocalDateTime.now())
                                                                .updated_at(LocalDateTime.now())
                                                                .created_by(current_user)
                                                                .updated_by(current_user)
                                                                .build())
                                                .collectList()
                                                .flatMap(accounts -> account_repository.saveAll(accounts).then())
                                                .doOnSuccess(v -> log.info(
                                                                "Successfully initialized plan for tenant {}",
                                                                tenant_id)));
        }

        @Transactional
        public Mono<PlanComptableDto> createAccount(PlanComptableDto dto) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> ReactiveTenantContext.getCurrentUser().defaultIfEmpty("system")
                                                .flatMap(current_user -> {
                                                        log.info("Creating account {} for tenant {}",
                                                                        dto.getNo_compte(), tenant_id);

                                                        try {
                                                                validation_service.validateAccountNumber(
                                                                                dto.getNo_compte());
                                                        } catch (Exception e) {
                                                                return Mono.error(e);
                                                        }

                                                        return account_repository
                                                                        .existsByTenant_IdAndNo_compte(tenant_id,
                                                                                        dto.getNo_compte())
                                                                        .flatMap(exists -> {
                                                                                if (Boolean.TRUE.equals(exists)) {
                                                                                        return Mono.error(
                                                                                                        new BusinessException(
                                                                                                                        "Account already exists: "
                                                                                                                                        + dto.getNo_compte()));
                                                                                }

                                                                                PlanComptable account = PlanComptable
                                                                                                .builder()
                                                                                                .tenantId(tenant_id)
                                                                                                .no_compte(dto.getNo_compte())
                                                                                                .classe(Character
                                                                                                                .getNumericValue(
                                                                                                                                dto.getNo_compte()
                                                                                                                                                .charAt(0)))
                                                                                                .libelle(dto.getLibelle())
                                                                                                .notes(dto.getNotes())
                                                                                                .actif(true)
                                                                                                .created_at(LocalDateTime
                                                                                                                .now())
                                                                                                .updated_at(LocalDateTime
                                                                                                                .now())
                                                                                                .created_by(current_user)
                                                                                                .updated_by(current_user)
                                                                                                .build();

                                                                                return account_repository.save(account)
                                                                                                .flatMap(saved -> logAudit(
                                                                                                                tenant_id,
                                                                                                                current_user,
                                                                                                                "ACCOUNT_CREATED",
                                                                                                                "Creation of account: "
                                                                                                                                + saved.getNo_compte())
                                                                                                                .then(redis_service
                                                                                                                                .delete(CACHE_ALL
                                                                                                                                                + tenant_id))
                                                                                                                .thenReturn(mapToDto(
                                                                                                                                saved)));
                                                                        });
                                                }));
        }

        @SuppressWarnings("unchecked")
        public Mono<List<PlanComptableDto>> getAllAccounts() {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> {
                                        String key = CACHE_ALL + tenant_id;
                                        return redis_service.get(key, List.class)
                                                        .map(list -> (List<PlanComptableDto>) list)
                                                        .switchIfEmpty(account_repository.findByTenant_Id(tenant_id)
                                                                        .map(this::mapToDto)
                                                                        .collectList()
                                                                        .flatMap(list -> redis_service
                                                                                        .save(key, list, Duration
                                                                                                        .ofMinutes(15))
                                                                                        .thenReturn(list)));
                                });
        }

        @SuppressWarnings("unchecked")
        public Mono<List<PlanComptableDto>> getAllActiveAccounts() {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> {
                                        String key = CACHE_ACTIVE + tenant_id;
                                        return redis_service.get(key, List.class)
                                                        .map(list -> (List<PlanComptableDto>) list)
                                                        .switchIfEmpty(account_repository
                                                                        .findByTenant_IdAndActifTrue(tenant_id)
                                                                        .map(this::mapToDto)
                                                                        .collectList()
                                                                        .flatMap(list -> redis_service
                                                                                        .save(key, list, Duration
                                                                                                        .ofMinutes(15))
                                                                                        .thenReturn(list)));
                                });
        }

        public Mono<PlanComptableDto> getAccountById(UUID id) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> {
                                        String key = CACHE_SINGLE + tenant_id + ":" + id;
                                        return redis_service.get(key, PlanComptableDto.class)
                                                        .switchIfEmpty(account_repository
                                                                        .findByTenant_IdAndId(tenant_id, id)
                                                                        .switchIfEmpty(Mono.error(
                                                                                        new ResourceNotFoundException(
                                                                                                        "Accounting account",
                                                                                                        id.toString())))
                                                                        .map(this::mapToDto)
                                                                        .flatMap(dto -> redis_service
                                                                                        .save(key, dto, Duration
                                                                                                        .ofMinutes(15))
                                                                                        .thenReturn(dto)));
                                });
        }

        @SuppressWarnings("unchecked")
        public Mono<List<PlanComptableDto>> getAccountsByClass(Integer classe) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> {
                                        String key = CACHE_CLASS + tenant_id + ":" + classe;
                                        return redis_service.get(key, List.class)
                                                        .map(list -> (List<PlanComptableDto>) list)
                                                        .switchIfEmpty(account_repository
                                                                        .findByTenant_IdAndClasse(tenant_id, classe)
                                                                        .map(this::mapToDto)
                                                                        .collectList()
                                                                        .flatMap(list -> redis_service
                                                                                        .save(key, list, Duration
                                                                                                        .ofMinutes(20))
                                                                                        .thenReturn(list)));
                                });
        }

        @SuppressWarnings("unchecked")
        public Mono<List<PlanComptableDto>> getAccountsByPrefix(String prefix) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> {
                                        String key = CACHE_PREFIX + tenant_id + ":" + prefix;
                                        return redis_service.get(key, List.class)
                                                        .map(list -> (List<PlanComptableDto>) list)
                                                        .switchIfEmpty(account_repository
                                                                        .findByTenant_IdAndNo_compteStartingWith(
                                                                                        tenant_id, prefix)
                                                                        .map(this::mapToDto)
                                                                        .collectList()
                                                                        .flatMap(list -> redis_service
                                                                                        .save(key, list, Duration
                                                                                                        .ofMinutes(20))
                                                                                        .thenReturn(list)));
                                });
        }

        @Transactional
        public Mono<PlanComptableDto> updateAccount(UUID id, PlanComptableDto dto) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> ReactiveTenantContext.getCurrentUser().defaultIfEmpty("system")
                                                .flatMap(current_user -> account_repository
                                                                .findByTenant_IdAndId(tenant_id, id)
                                                                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                                                                                "Accounting account", id.toString())))
                                                                .flatMap(account -> {
                                                                        account.setLibelle(dto.getLibelle());
                                                                        account.setNotes(dto.getNotes());
                                                                        account.setUpdated_at(LocalDateTime.now());
                                                                        account.setUpdated_by(current_user);

                                                                        return account_repository.save(account)
                                                                                        .flatMap(saved -> logAudit(
                                                                                                        tenant_id,
                                                                                                        current_user,
                                                                                                        "ACCOUNT_UPDATED",
                                                                                                        "Update of account: "
                                                                                                                        + saved.getNo_compte())
                                                                                                        .then(redis_service
                                                                                                                        .delete(CACHE_SINGLE
                                                                                                                                        + tenant_id
                                                                                                                                        + ":"
                                                                                                                                        + id))
                                                                                                        .then(redis_service
                                                                                                                        .delete(CACHE_ALL
                                                                                                                                        + tenant_id))
                                                                                                        .thenReturn(mapToDto(
                                                                                                                        saved)));
                                                                })));
        }

        @Transactional
        public Mono<Void> deactivateAccount(UUID id) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> ReactiveTenantContext.getCurrentUser().defaultIfEmpty("system")
                                                .flatMap(current_user -> account_repository
                                                                .findByTenant_IdAndId(tenant_id, id)
                                                                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                                                                                "Accounting account", id.toString())))
                                                                .flatMap(account -> {
                                                                        account.setActif(false);
                                                                        account.setUpdated_at(LocalDateTime.now());
                                                                        account.setUpdated_by(current_user);

                                                                        return account_repository.save(account)
                                                                                        .flatMap(saved -> logAudit(
                                                                                                        tenant_id,
                                                                                                        current_user,
                                                                                                        "ACCOUNT_DEACTIVATED",
                                                                                                        "Deactivation of account: "
                                                                                                                        + saved.getNo_compte())
                                                                                                        .then(redis_service
                                                                                                                        .delete(CACHE_SINGLE
                                                                                                                                        + tenant_id
                                                                                                                                        + ":"
                                                                                                                                        + id))
                                                                                                        .then(redis_service
                                                                                                                        .delete(CACHE_ACTIVE
                                                                                                                                        + tenant_id))
                                                                                                        .then());
                                                                })));
        }

        private PlanComptableDto mapToDto(PlanComptable entity) {
                return PlanComptableDto.builder()
                                .id(entity.getId())
                                .no_compte(entity.getNo_compte())
                                .libelle(entity.getLibelle())
                                .classe(entity.getClasse())
                                .notes(entity.getNotes())
                                .actif(entity.getActif())
                                .created_at(entity.getCreated_at())
                                .updated_at(entity.getUpdated_at())
                                .created_by(entity.getCreated_by())
                                .updated_by(entity.getUpdated_by())
                                .build();
        }

        private Mono<Void> logAudit(UUID tenant_id, String user, String action, String details) {
                JournalAudit audit = JournalAudit.builder()
                                .id(UUID.randomUUID())
                                .tenantId(tenant_id)
                                .action(action)
                                .utilisateur(user)
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
                                                        .id(saved.getId())
                                                        .action(saved.getAction())
                                                        .utilisateur(saved.getUtilisateur())
                                                        .details(saved.getDetails())
                                                        .date_action(saved.getDate_action())
                                                        .build();

                                        return kafka_service.sendAuditLog(auditDto, tenant_id, action);
                                });
        }

        @SuppressWarnings("unused")
        private Mono<Void> logAudit(Tenant tenant, String user, String action, String details) {
                return logAudit(tenant.getId(), user, action, details);
        }
}
