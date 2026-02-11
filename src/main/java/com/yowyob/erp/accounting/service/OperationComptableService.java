package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.ContrepartieDto;
import com.yowyob.erp.accounting.dto.JournalAuditDto;
import com.yowyob.erp.accounting.dto.OperationComptableDto;
import com.yowyob.erp.accounting.entity.Compte;
import com.yowyob.erp.accounting.entity.Contrepartie;
import com.yowyob.erp.accounting.entity.JournalAudit;
import com.yowyob.erp.accounting.entity.JournalComptable;
import com.yowyob.erp.accounting.entity.OperationComptable;
import com.yowyob.erp.accounting.entity.Organization;
import com.yowyob.erp.accounting.repository.CompteRepository;
import com.yowyob.erp.accounting.repository.ContrepartieRepository;
import com.yowyob.erp.accounting.repository.JournalAuditRepository;
import com.yowyob.erp.accounting.repository.JournalComptableRepository;
import com.yowyob.erp.accounting.repository.OperationComptableRepository;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import com.yowyob.erp.config.kafka.KafkaMessageService;
import com.yowyob.erp.config.redis.RedisService;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
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
                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> ReactiveOrganizationContext.getCurrentUser()
                                                .defaultIfEmpty("system")
                                                .flatMap(user -> {
                                                        log.info("📝 Creating operation [{} - {}] for organization {}",
                                                                        dto.getType_operation(),
                                                                        dto.getMode_reglement(), organization_id);

                                                        return validateOperationDto(dto)
                                                                        .then(journal_repository
                                                                                        .findByOrganization_IdAndId(
                                                                                                        organization_id,
                                                                                                        dto.getJournal_comptable_id())
                                                                                        .filter(JournalComptable::getActif)
                                                                                        .switchIfEmpty(Mono.error(
                                                                                                        new IllegalArgumentException(
                                                                                                                        "Invalid/inactive journal"))))
                                                                        .then(compte_repository
                                                                                        .findByOrganization_IdAndId(
                                                                                                        organization_id,
                                                                                                        dto.getCompte_principal_id())
                                                                                        .filter(Compte::getActif)
                                                                                        .switchIfEmpty(Mono.error(
                                                                                                        new IllegalArgumentException(
                                                                                                                        "Invalid/inactive account"))))
                                                                        .then(operation_repository
                                                                                        .findByOrganization_IdAndType_operationAndMode_reglement(
                                                                                                        organization_id,
                                                                                                        dto.getType_operation(),
                                                                                                        dto.getMode_reglement())
                                                                                        .flatMap(op -> Mono.error(
                                                                                                        new IllegalArgumentException(
                                                                                                                        "Existing operation"))))
                                                                        .then(ReactiveOrganizationContext
                                                                                        .getCurrentOrganizationAsOrganization())
                                                                        .flatMap(organization -> {
                                                                                OperationComptable entity = mapToEntity(
                                                                                                dto, organization);
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
                                                                                                                                                organization,
                                                                                                                                                saved.getId(),
                                                                                                                                                user))
                                                                                                                                .collectList()
                                                                                                                                .flatMap((List<Contrepartie> cps) -> contrepartie_repository
                                                                                                                                                .saveAll(cps)
                                                                                                                                                .collectList())
                                                                                                                                .thenReturn(saved);
                                                                                                        }
                                                                                                        return Mono.just(
                                                                                                                        saved);
                                                                                                })
                                                                                                .flatMap(saved -> logAudit(
                                                                                                                organization.getId(),
                                                                                                                user,
                                                                                                                "OPERATION_CREATED",
                                                                                                                "Type: " + dto.getType_operation())
                                                                                                                .then(redis_service
                                                                                                                                .delete(CACHE_OPERATIONS_ALL
                                                                                                                                                + organization_id))
                                                                                                                .thenReturn(saved))
                                                                                                .flatMap(this::mapToDto);
                                                                        });
                                                }));
        }

        @SuppressWarnings("unchecked")
        public Mono<List<OperationComptableDto>> getAllOperations() {
                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> {
                                        String key = CACHE_OPERATIONS_ALL + organization_id;
                                        return redis_service.get(key, List.class)
                                                        .map(list -> (List<OperationComptableDto>) list)
                                                        .switchIfEmpty(operation_repository
                                                                        .findByOrganization_Id(organization_id)
                                                                        .flatMap(this::mapToDto)
                                                                        .collectList()
                                                                        .flatMap(list -> redis_service
                                                                                        .save(key, list, Duration
                                                                                                        .ofMinutes(10))
                                                                                        .thenReturn(list)));
                                });
        }

        public Mono<OperationComptableDto> getOperation(UUID id) {
                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> {
                                        String key = CACHE_OPERATION + organization_id + ":" + id;
                                        return redis_service.get(key, OperationComptableDto.class)
                                                        .switchIfEmpty(operation_repository
                                                                        .findByOrganization_IdAndId(organization_id, id)
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
                return ReactiveOrganizationContext.getOrganizationId()
                                .doOnNext(tid -> log.info("getOperationsByCompteId for account {} with organization {}",
                                                compte_id, tid))
                                .flatMap(organization_id -> operation_repository
                                                .findByOrganization_IdAndCompte_principal_id(organization_id, compte_id)
                                                .doOnNext(op -> log.info("Found operation: {}", op.getId()))
                                                .flatMap(this::mapToDto)
                                                .collectList()
                                                .doOnSuccess(list -> log.info("Collected {} operations", list.size())));
        }

        public Mono<List<OperationComptableDto>> getOperationsByCompte(String no_compte) {
                return ReactiveOrganizationContext.getOrganizationId()
                                .doOnNext(tid -> log.info("Searching for account {} with organization {}", no_compte,
                                                tid))
                                .flatMap(organization_id -> compte_repository
                                                .findByOrganization_IdAndNo_compte(organization_id, no_compte)
                                                .doOnNext(c -> log.info("Found account: {} ({})", c.getNo_compte(),
                                                                c.getId()))
                                                .flatMap(compte -> getOperationsByCompteId(compte.getId()))
                                                .switchIfEmpty(Mono.defer(() -> {
                                                        log.warn("Account {} not found for organization {}", no_compte,
                                                                        organization_id);
                                                        return Mono.just(List.of());
                                                })));
        }

        public Mono<OperationComptableDto> getByTypeAndMode(String type, String mode) {
                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> operation_repository
                                                .findByOrganization_IdAndType_operationAndMode_reglement(
                                                                organization_id, type,
                                                                mode)
                                                .flatMap(this::mapToDto)
                                                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Operation",
                                                                "Type: " + type + ", Mode: " + mode))));
        }

        @Transactional
        public Mono<OperationComptableDto> updateOperation(UUID id, OperationComptableDto dto) {
                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> ReactiveOrganizationContext.getCurrentUser()
                                                .defaultIfEmpty("system")
                                                .flatMap(user -> operation_repository
                                                                .findByOrganization_IdAndId(organization_id, id)
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
                                                                                                                        .deleteByOrganizationIdAndOperationComptableId(
                                                                                                                                        organization_id,
                                                                                                                                        id)
                                                                                                                        .then(ReactiveOrganizationContext
                                                                                                                                        .getCurrentOrganizationAsOrganization())
                                                                                                                        .flatMap(organization -> {
                                                                                                                                if (dto.getContreparties() != null
                                                                                                                                                && !dto.getContreparties()
                                                                                                                                                                .isEmpty()) {
                                                                                                                                        return Flux.fromIterable(
                                                                                                                                                        dto.getContreparties())
                                                                                                                                                        .flatMap(cpDto -> mapContrepartie(
                                                                                                                                                                        cpDto,
                                                                                                                                                                        organization,
                                                                                                                                                                        id,
                                                                                                                                                                        user))
                                                                                                                                                        .collectList()
                                                                                                                                                        .flatMap((List<Contrepartie> cps) -> contrepartie_repository
                                                                                                                                                                        .saveAll(cps)
                                                                                                                                                                        .collectList()
                                                                                                                                                                        .then());
                                                                                                                                }
                                                                                                                                return Mono.empty();
                                                                                                                        })
                                                                                                                        .then(logAudit(organization_id,
                                                                                                                                        user,
                                                                                                                                        "OPERATION_UPDATED",
                                                                                                                                        "ID: " + id))
                                                                                                                        .then(redis_service
                                                                                                                                        .delete(CACHE_OPERATIONS_ALL
                                                                                                                                                        + organization_id))
                                                                                                                        .then(redis_service
                                                                                                                                        .delete(CACHE_OPERATION
                                                                                                                                                        + organization_id
                                                                                                                                                        + ":"
                                                                                                                                                        + id))
                                                                                                                        .thenReturn(saved))
                                                                                                        .flatMap(this::mapToDto);
                                                                                }))));
        }

        @Transactional
        public Mono<Void> deleteOperation(UUID id) {
                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> ReactiveOrganizationContext.getCurrentUser()
                                                .defaultIfEmpty("system")
                                                .flatMap(user -> operation_repository
                                                                .findByOrganization_IdAndId(organization_id, id)
                                                                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                                                                                "Operation", id.toString())))
                                                                .flatMap(op -> contrepartie_repository
                                                                                .deleteByOrganizationIdAndOperationComptableId(
                                                                                                organization_id, id)
                                                                                .then(operation_repository.delete(op))
                                                                                .then(logAudit(organization_id, user,
                                                                                                "OPERATION_DELETED",
                                                                                                "ID: " + id))
                                                                                .then(redis_service.delete(
                                                                                                CACHE_OPERATIONS_ALL
                                                                                                                + organization_id))
                                                                                .then(redis_service.delete(
                                                                                                CACHE_OPERATION + organization_id
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

        private Mono<Contrepartie> mapContrepartie(ContrepartieDto dto, Organization organization, UUID opId,
                        String user) {
                return journal_repository.findById(dto.getJournal_comptable_id())
                                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Journal",
                                                dto.getJournal_comptable_id().toString())))
                                .map(j -> {
                                        Contrepartie cp = new Contrepartie();
                                        cp.setId(dto.getId() != null ? dto.getId() : UUID.randomUUID());
                                        cp.setOrganizationId(organization.getId());
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

        private Mono<Void> logAudit(UUID organization_id, String user, String action, String details) {
                JournalAudit audit = JournalAudit.builder()
                                .id(UUID.randomUUID())
                                .organizationId(organization_id)
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
                                                .build(), organization_id, action));
        }

        private OperationComptable mapToEntity(OperationComptableDto dto, Organization organization) {
                OperationComptable op = new OperationComptable();
                op.setId(dto.getId() != null ? dto.getId() : UUID.randomUUID());
                op.setOrganizationId(organization.getId());
                op.setType_operation(dto.getType_operation());
                op.setMode_reglement(dto.getMode_reglement());
                op.setCompte_principal_id(dto.getCompte_principal_id());
                op.setEst_compte_statique(dto.getEst_compte_statique());
                op.setSens_principal(dto.getSens_principal());
                op.setJournal_comptable_id(dto.getJournal_comptable_id());
                op.setType_montant(dto.getType_montant());
                op.setPlafond_client(dto.getPlafond_client() != null ? dto.getPlafond_client() : BigDecimal.ZERO);
                op.setActif(dto.getActif());
                op.setNotes(dto.getNotes());
                return op;
        }

        private Mono<OperationComptableDto> mapToDto(OperationComptable op) {
                return contrepartie_repository
                                .findByOrganization_IdAndOperation_comptable_Id(op.getOrganizationId(), op.getId())
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
                                                .est_compte_statique(op.getEst_compte_statique())
                                                .sens_principal(op.getSens_principal())
                                                .journal_comptable_id(op.getJournal_comptable_id())
                                                .type_montant(op.getType_montant())
                                                .plafond_client(op.getPlafond_client())
                                                .actif(op.getActif())
                                                .notes(op.getNotes())
                                                .contreparties(cps)
                                                .created_at(op.getCreated_at())
                                                .updated_at(op.getUpdated_at())
                                                .created_by(op.getCreated_by())
                                                .updated_by(op.getUpdated_by())
                                                .build());
        }
}