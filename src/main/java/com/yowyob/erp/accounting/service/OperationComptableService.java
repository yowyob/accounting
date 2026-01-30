package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.ContrepartieDto;
import com.yowyob.erp.accounting.dto.JournalAuditDto;
import com.yowyob.erp.accounting.dto.OperationComptableDto;
import com.yowyob.erp.accounting.entity.Compte;
import com.yowyob.erp.accounting.entity.Contrepartie;
import com.yowyob.erp.accounting.entity.JournalAudit;
import com.yowyob.erp.accounting.entity.JournalComptable;
import com.yowyob.erp.accounting.entity.OperationComptable;
import com.yowyob.erp.accounting.entity.PlanComptable;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.accounting.repository.CompteRepository;
import com.yowyob.erp.accounting.repository.ContrepartieRepository;
import com.yowyob.erp.accounting.repository.JournalAuditRepository;
import com.yowyob.erp.accounting.repository.JournalComptableRepository;
import com.yowyob.erp.accounting.repository.OperationComptableRepository;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import com.yowyob.erp.config.kafka.KafkaMessageService;
import com.yowyob.erp.config.redis.RedisService;
import com.yowyob.erp.config.tenant.ReactiveTenantContext;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Reactive Service for managing accounting operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OperationComptableService {

        private final OperationComptableRepository operation_repository;
        private final ContrepartieRepository contrepartie_repository;
        private final JournalComptableRepository journal_repository;
        private final CompteRepository compte_repository;
        private final JournalAuditRepository audit_repository;
        private final Validator validator;
        private final KafkaMessageService kafka_service;
        private final RedisService redis_service;

        private static final String CACHE_OPERATIONS_ALL = "operations:all:";
        private static final String CACHE_OPERATION = "operation:";

        @Transactional
        public Mono<OperationComptableDto> createOperation(OperationComptableDto dto) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> ReactiveTenantContext.getCurrentUser().defaultIfEmpty("system")
                                                .flatMap(user -> {
                                                        log.info("📝 Creating operation [{} - {}] for tenant {}",
                                                                        dto.getType_operation(),
                                                                        dto.getMode_reglement(), tenant_id);

                                                        return validateOperationDto(dto)
                                                                        .then(journal_repository.findByTenant_IdAndId(
                                                                                        tenant_id,
                                                                                        dto.getJournal_comptable_id())
                                                                                        .filter(JournalComptable::getActif)
                                                                                        .switchIfEmpty(Mono.error(
                                                                                                        new IllegalArgumentException(
                                                                                                                        "Invalid/inactive journal"))))
                                                                        .then(compte_repository
                                                                                        .findByTenant_IdAndId(
                                                                                                        tenant_id,
                                                                                                        dto.getCompte_principal_id())
                                                                                        .filter(Compte::getActif)
                                                                                        .switchIfEmpty(Mono.error(
                                                                                                        new IllegalArgumentException(
                                                                                                                        "Invalid/inactive account"))))
                                                                        .then(operation_repository
                                                                                        .findByTenant_IdAndType_operationAndMode_reglement(
                                                                                                        tenant_id,
                                                                                                        dto.getType_operation(),
                                                                                                        dto.getMode_reglement())
                                                                                        .flatMap(op -> Mono.error(
                                                                                                        new IllegalArgumentException(
                                                                                                                        "Existing operation"))))
                                                                        .then(ReactiveTenantContext
                                                                                        .getCurrentTenantAsTenant())
                                                                        .flatMap(tenant -> {
                                                                                OperationComptable entity = mapToEntity(
                                                                                                dto, tenant);
                                                                                entity.setCreated_by(user);
                                                                                entity.setUpdated_by(user);
                                                                                entity.setCreated_at(
                                                                                                LocalDateTime.now());
                                                                                entity.setUpdated_at(
                                                                                                LocalDateTime.now());

                                                                                return operation_repository.save(entity)
                                                                                                .flatMap(saved -> {
                                                                                                        if (dto.getContreparties() != null
                                                                                                                        && !dto.getContreparties()
                                                                                                                                        .isEmpty()) {
                                                                                                                return Flux.fromIterable(
                                                                                                                                dto.getContreparties())
                                                                                                                                .flatMap(cpDto -> mapContrepartie(
                                                                                                                                                cpDto,
                                                                                                                                                tenant,
                                                                                                                                                saved.getId(),
                                                                                                                                                user))
                                                                                                                                .collectList()
                                                                                                                                .flatMap(cps -> contrepartie_repository
                                                                                                                                                .saveAll(cps)
                                                                                                                                                .collectList())
                                                                                                                                .thenReturn(saved);
                                                                                                        }
                                                                                                        return Mono.just(
                                                                                                                        saved);
                                                                                                })
                                                                                                .flatMap(saved -> logAudit(
                                                                                                                tenant.getId(),
                                                                                                                user,
                                                                                                                "OPERATION_CREATED",
                                                                                                                "Type: " + dto.getType_operation())
                                                                                                                .then(redis_service
                                                                                                                                .delete(CACHE_OPERATIONS_ALL
                                                                                                                                                + tenant_id))
                                                                                                                .thenReturn(saved))
                                                                                                .flatMap(this::mapToDto);
                                                                        });
                                                }));
        }

        @SuppressWarnings("unchecked")
        public Mono<List<OperationComptableDto>> getAllOperations() {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> {
                                        String key = CACHE_OPERATIONS_ALL + tenant_id;
                                        return redis_service.get(key, List.class)
                                                        .map(list -> (List<OperationComptableDto>) list)
                                                        .switchIfEmpty(operation_repository.findByTenant_Id(tenant_id)
                                                                        .flatMap(this::mapToDto)
                                                                        .collectList()
                                                                        .flatMap(list -> redis_service
                                                                                        .save(key, list, Duration
                                                                                                        .ofMinutes(10))
                                                                                        .thenReturn(list)));
                                });
        }

        public Mono<OperationComptableDto> getOperation(UUID id) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> {
                                        String key = CACHE_OPERATION + tenant_id + ":" + id;
                                        return redis_service.get(key, OperationComptableDto.class)
                                                        .switchIfEmpty(operation_repository
                                                                        .findByTenant_IdAndId(tenant_id, id)
                                                                        .switchIfEmpty(Mono.error(
                                                                                        new ResourceNotFoundException(
                                                                                                        "Operation",
                                                                                                        id.toString())))
                                                                        .flatMap(this::mapToDto)
                                                                        .flatMap(dto -> redis_service
                                                                                        .save(key, dto, Duration
                                                                                                        .ofMinutes(10))
                                                                                        .thenReturn(dto)));
                                });
        }

        public Mono<List<OperationComptableDto>> getOperationsByCompteId(UUID compte_id) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> operation_repository
                                                .findByTenant_IdAndCompte_principal_id(tenant_id, compte_id)
                                                .flatMap(this::mapToDto)
                                                .collectList());
        }

        public Mono<List<OperationComptableDto>> getOperationsByCompte(String no_compte) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> compte_repository
                                                .findByTenant_IdAndNo_compte(tenant_id, no_compte)
                                                .flatMap(compte -> getOperationsByCompteId(compte.getId()))
                                                .switchIfEmpty(Mono.error(
                                                                new ResourceNotFoundException("Compte", no_compte))));
        }

        public Mono<OperationComptableDto> getByTypeAndMode(String type, String mode) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> operation_repository
                                                .findByTenant_IdAndType_operationAndMode_reglement(tenant_id, type,
                                                                mode)
                                                .flatMap(this::mapToDto)
                                                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Operation",
                                                                "Type: " + type + ", Mode: " + mode))));
        }

        @Transactional
        public Mono<OperationComptableDto> updateOperation(UUID id, OperationComptableDto dto) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> ReactiveTenantContext.getCurrentUser().defaultIfEmpty("system")
                                                .flatMap(user -> operation_repository
                                                                .findByTenant_IdAndId(tenant_id, id)
                                                                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                                                                                "Operation", id.toString())))
                                                                .flatMap(existing -> validateOperationDto(dto)
                                                                                .then(journal_repository.findById(dto
                                                                                                .getJournal_comptable_id())
                                                                                                .switchIfEmpty(Mono
                                                                                                                .error(new ResourceNotFoundException(
                                                                                                                                "Journal",
                                                                                                                                dto.getJournal_comptable_id()
                                                                                                                                                .toString()))))
                                                                                .flatMap(journal -> {
                                                                                        existing.setType_operation(dto
                                                                                                        .getType_operation());
                                                                                        existing.setMode_reglement(dto
                                                                                                        .getMode_reglement());
                                                                                        existing.setCompte_principal_id(
                                                                                                        dto
                                                                                                                        .getCompte_principal_id());
                                                                                        existing.setSens_principal(dto
                                                                                                        .getSens_principal());
                                                                                        existing.setJournal_comptable_id(
                                                                                                        journal.getId());
                                                                                        existing.setType_montant(dto
                                                                                                        .getType_montant());
                                                                                        existing.setPlafond_client(dto
                                                                                                        .getPlafond_client());
                                                                                        existing.setEst_compte_statique(
                                                                                                        dto.getEst_compte_statique());
                                                                                        existing.setActif(
                                                                                                        dto.getActif());
                                                                                        existing.setUpdated_by(user);
                                                                                        existing.setUpdated_at(
                                                                                                        LocalDateTime.now());

                                                                                        return operation_repository
                                                                                                        .save(existing)
                                                                                                        .flatMap(saved -> contrepartie_repository
                                                                                                                        .deleteByTenantIdAndOperationComptableId(
                                                                                                                                        tenant_id,
                                                                                                                                        id)
                                                                                                                        .then(ReactiveTenantContext
                                                                                                                                        .getCurrentTenantAsTenant())
                                                                                                                        .flatMap(tenant -> {
                                                                                                                                if (dto.getContreparties() != null
                                                                                                                                                && !dto.getContreparties()
                                                                                                                                                                .isEmpty()) {
                                                                                                                                        return Flux.fromIterable(
                                                                                                                                                        dto.getContreparties())
                                                                                                                                                        .flatMap(cpDto -> mapContrepartie(
                                                                                                                                                                        cpDto,
                                                                                                                                                                        tenant,
                                                                                                                                                                        id,
                                                                                                                                                                        user))
                                                                                                                                                        .collectList()
                                                                                                                                                        .flatMap(cps -> contrepartie_repository
                                                                                                                                                                        .saveAll(cps)
                                                                                                                                                                        .collectList()
                                                                                                                                                                        .then());
                                                                                                                                }
                                                                                                                                return Mono.empty();
                                                                                                                        })
                                                                                                                        .then(logAudit(tenant_id,
                                                                                                                                        user,
                                                                                                                                        "OPERATION_UPDATED",
                                                                                                                                        "ID: " + id))
                                                                                                                        .then(redis_service
                                                                                                                                        .delete(CACHE_OPERATIONS_ALL
                                                                                                                                                        + tenant_id))
                                                                                                                        .then(redis_service
                                                                                                                                        .delete(CACHE_OPERATION
                                                                                                                                                        + tenant_id
                                                                                                                                                        + ":"
                                                                                                                                                        + id))
                                                                                                                        .thenReturn(saved))
                                                                                                        .flatMap(this::mapToDto);
                                                                                }))));
        }

        @Transactional
        public Mono<Void> deleteOperation(UUID id) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> ReactiveTenantContext.getCurrentUser().defaultIfEmpty("system")
                                                .flatMap(user -> operation_repository
                                                                .findByTenant_IdAndId(tenant_id, id)
                                                                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                                                                                "Operation", id.toString())))
                                                                .flatMap(op -> contrepartie_repository
                                                                                .deleteByTenantIdAndOperationComptableId(
                                                                                                tenant_id, id)
                                                                                .then(operation_repository.delete(op))
                                                                                .then(logAudit(tenant_id, user,
                                                                                                "OPERATION_DELETED",
                                                                                                "ID: " + id))
                                                                                .then(redis_service.delete(
                                                                                                CACHE_OPERATIONS_ALL
                                                                                                                + tenant_id))
                                                                                .then(redis_service.delete(
                                                                                                CACHE_OPERATION + tenant_id
                                                                                                                + ":"
                                                                                                                + id))
                                                                                .then())));
        }

        private Mono<Void> validateOperationDto(OperationComptableDto dto) {
                return Mono.defer(() -> {
                        var violations = validator.validate(dto);
                        if (!violations.isEmpty())
                                return Mono.error(new ConstraintViolationException(violations));
                        return Mono.empty();
                });
        }

        private Mono<Contrepartie> mapContrepartie(ContrepartieDto dto, Tenant tenant, UUID opId, String user) {
                return journal_repository.findById(dto.getJournal_comptable_id())
                                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Journal",
                                                dto.getJournal_comptable_id().toString())))
                                .map(j -> {
                                        Contrepartie cp = new Contrepartie();
                                        cp.setTenantId(tenant.getId());
                                        cp.setOperation_comptable_id(opId);
                                        cp.setJournal_comptable_id(j.getId());
                                        cp.setCompte_id(dto.getCompte_id());
                                        cp.setSens(dto.getSens());
                                        cp.setType_montant(dto.getType_montant());
                                        cp.setEst_compte_tiers(dto.getEst_compte_tiers());
                                        cp.setCreated_by(user);
                                        cp.setUpdated_by(user);
                                        cp.setCreated_at(LocalDateTime.now());
                                        cp.setUpdated_at(LocalDateTime.now());
                                        return cp;
                                });
        }

        private Mono<Void> logAudit(UUID tenant_id, String user, String action, String details) {
                JournalAudit audit = JournalAudit.builder()
                                .id(UUID.randomUUID())
                                .tenantId(tenant_id)
                                .action(action)
                                .utilisateur(user)
                                .details(details)
                                .date_action(LocalDateTime.now())
                                .created_by(user)
                                .updated_by(user)
                                .created_at(LocalDateTime.now())
                                .updated_at(LocalDateTime.now())
                                .build();
                return audit_repository.save(audit)
                                .flatMap(saved -> kafka_service.sendAuditLog(JournalAuditDto.builder()
                                                .id(saved.getId())
                                                .action(saved.getAction())
                                                .utilisateur(saved.getUtilisateur())
                                                .details(saved.getDetails())
                                                .date_action(saved.getDate_action())
                                                .created_at(saved.getCreated_at())
                                                .updated_at(saved.getUpdated_at())
                                                .created_by(saved.getCreated_by())
                                                .updated_by(saved.getUpdated_by())
                                                .build(), tenant_id, action));
        }

        private OperationComptable mapToEntity(OperationComptableDto dto, Tenant tenant) {
                OperationComptable op = new OperationComptable();
                op.setTenantId(tenant.getId());
                op.setType_operation(dto.getType_operation());
                op.setMode_reglement(dto.getMode_reglement());
                op.setCompte_principal_id(dto.getCompte_principal_id());
                op.setEst_compte_statique(dto.getEst_compte_statique());
                op.setSens_principal(dto.getSens_principal());
                op.setJournal_comptable_id(dto.getJournal_comptable_id());
                op.setType_montant(dto.getType_montant());
                op.setPlafond_client(dto.getPlafond_client() != null ? dto.getPlafond_client() : BigDecimal.ZERO);
                op.setActif(dto.getActif());
                return op;
        }

        private Mono<OperationComptableDto> mapToDto(OperationComptable op) {
                return contrepartie_repository.findByTenant_IdAndOperation_comptable_Id(op.getTenantId(), op.getId())
                                .map(cp -> ContrepartieDto.builder()
                                                .id(cp.getId())
                                                .compte_id(cp.getCompte_id())
                                                .sens(cp.getSens())
                                                .type_montant(cp.getType_montant())
                                                .est_compte_tiers(cp.getEst_compte_tiers())
                                                .journal_comptable_id(cp.getJournal_comptable_id())
                                                .build())
                                .collectList()
                                .map(cps -> OperationComptableDto.builder()
                                                .id(op.getId())
                                                .type_operation(op.getType_operation())
                                                .mode_reglement(op.getMode_reglement())
                                                .compte_principal_id(op.getCompte_principal_id())
                                                .sens_principal(op.getSens_principal())
                                                .journal_comptable_id(op.getJournal_comptable_id())
                                                .type_montant(op.getType_montant())
                                                .actif(op.getActif())
                                                .contreparties(cps)
                                                .build());
        }
}