package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.EcritureComptableDto;
import com.yowyob.erp.accounting.dto.JournalAuditDto;
import com.yowyob.erp.accounting.dto.JournalComptableDto;
import com.yowyob.erp.accounting.entity.EcritureComptable;
import com.yowyob.erp.accounting.entity.JournalAudit;
import com.yowyob.erp.accounting.entity.JournalComptable;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.accounting.repository.CompteRepository;
import com.yowyob.erp.accounting.repository.DetailEcritureRepository;
import com.yowyob.erp.accounting.repository.EcritureComptableRepository;
import com.yowyob.erp.accounting.repository.JournalAuditRepository;
import com.yowyob.erp.accounting.repository.JournalComptableRepository;
import com.yowyob.erp.common.constants.AppConstants;
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
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Reactive Service for managing accounting journals.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JournalComptableService {

        private final JournalComptableRepository journal_repository;
        private final EcritureComptableRepository ecriture_repository;
        private final DetailEcritureRepository detail_repository;
        private final CompteRepository compte_repository;
        private final JournalAuditRepository audit_repository;
        private final Validator validator;
        private final KafkaMessageService kafka_service;
        private final RedisService redis_service;

        private static final String CACHE_JOURNAL_ALL = "journal:all:";
        private static final String CACHE_JOURNAL_ACTIVE = "journal:active:";

        /**
         * Creates a new accounting journal.
         */
        @Transactional
        public Mono<JournalComptableDto> createJournalComptable(JournalComptableDto dto) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> ReactiveTenantContext.getCurrentUser().defaultIfEmpty("system")
                                                .flatMap(user -> {
                                                        log.info("📓 Creating accounting journal [{}] for tenant {}",
                                                                        dto.getCode_journal(),
                                                                        tenant_id);

                                                        return validateJournalComptableDto(dto)
                                                                        .then(journal_repository
                                                                                        .existsByTenant_IdAndCode_journal(
                                                                                                        tenant_id,
                                                                                                        dto.getCode_journal()))
                                                                        .flatMap(exists -> {
                                                                                if (Boolean.TRUE.equals(exists)) {
                                                                                        return Mono.error(
                                                                                                        new IllegalArgumentException(
                                                                                                                        "Journal code already in use: "
                                                                                                                                        + dto.getCode_journal()));
                                                                                }
                                                                                return ReactiveTenantContext
                                                                                                .getCurrentTenantAsTenant()
                                                                                                .flatMap(tenant -> {
                                                                                                        JournalComptable entity = mapToEntity(
                                                                                                                        dto,
                                                                                                                        tenant);
                                                                                                        entity.setTenantId(
                                                                                                                        tenant.getId());
                                                                                                        entity.setCreated_at(
                                                                                                                        LocalDateTime.now());
                                                                                                        entity.setUpdated_at(
                                                                                                                        LocalDateTime.now());
                                                                                                        entity.setCreated_by(
                                                                                                                        user);
                                                                                                        entity.setUpdated_by(
                                                                                                                        user);

                                                                                                        return journal_repository
                                                                                                                        .save(entity)
                                                                                                                        .flatMap(saved -> logAudit(
                                                                                                                                        tenant,
                                                                                                                                        user,
                                                                                                                                        "JOURNAL_CREATED",
                                                                                                                                        "Creation of journal "
                                                                                                                                                        + dto.getCode_journal())
                                                                                                                                        .then(redis_service
                                                                                                                                                        .delete(CACHE_JOURNAL_ALL
                                                                                                                                                                        + tenant_id))
                                                                                                                                        .then(redis_service
                                                                                                                                                        .delete(CACHE_JOURNAL_ACTIVE
                                                                                                                                                                        + tenant_id))
                                                                                                                                        .thenReturn(mapToDto(
                                                                                                                                                        saved)));
                                                                                                });
                                                                        });
                                                }));
        }

        /**
         * Retrieves a journal by its ID.
         */
        public Mono<JournalComptableDto> getJournalComptable(UUID journal_id) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> {
                                        log.info("🔍 Retrieving accounting journal [{}] for tenant {}", journal_id,
                                                        tenant_id);
                                        return journal_repository.findByTenant_IdAndId(tenant_id, journal_id)
                                                        .switchIfEmpty(Mono
                                                                        .error(new ResourceNotFoundException(
                                                                                        "JournalComptable",
                                                                                        journal_id.toString())))
                                                        .flatMap(journal -> {
                                                                JournalComptableDto dto = mapToDto(journal);
                                                                return ecriture_repository
                                                                                .findByTenant_IdAndJournal_Id(tenant_id,
                                                                                                journal_id)
                                                                                .flatMap(this::getFullEcritureDto)
                                                                                .collectList()
                                                                                .map(ecritures -> {
                                                                                        dto.setEcriture_comptable(
                                                                                                        ecritures);
                                                                                        return dto;
                                                                                });
                                                        });
                                });
        }

        /**
         * Retrieves all journals for the current tenant.
         */
        @SuppressWarnings("unchecked")
        public Mono<java.util.List<JournalComptableDto>> getAllJournaux() {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> {
                                        String cache_key = CACHE_JOURNAL_ALL + tenant_id;
                                        return redis_service.get(cache_key, java.util.List.class)
                                                        .map(list -> (java.util.List<JournalComptableDto>) list)
                                                        .switchIfEmpty(journal_repository.findByTenant_Id(tenant_id)
                                                                        .flatMap(journal -> {
                                                                                JournalComptableDto dto = mapToDto(
                                                                                                journal);
                                                                                return ecriture_repository
                                                                                                .findByTenant_IdAndJournal_Id(
                                                                                                                tenant_id,
                                                                                                                journal.getId())
                                                                                                .flatMap(this::getFullEcritureDto)
                                                                                                .collectList()
                                                                                                .map(ecritures -> {
                                                                                                        dto.setEcriture_comptable(
                                                                                                                        ecritures);
                                                                                                        return dto;
                                                                                                });
                                                                        })
                                                                        .collectList()
                                                                        .flatMap(list -> redis_service
                                                                                        .save(cache_key, list, Duration
                                                                                                        .ofMinutes(15))
                                                                                        .thenReturn(list)));
                                });
        }

        @SuppressWarnings("unchecked")
        public Mono<java.util.List<JournalComptableDto>> getActiveJournaux() {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> {
                                        String cache_key = CACHE_JOURNAL_ACTIVE + tenant_id;
                                        return redis_service.get(cache_key, java.util.List.class)
                                                        .map(list -> (java.util.List<JournalComptableDto>) list)
                                                        .switchIfEmpty(journal_repository
                                                                        .findByTenant_IdAndActifTrue(tenant_id)
                                                                        .flatMap(journal -> {
                                                                                JournalComptableDto dto = mapToDto(
                                                                                                journal);
                                                                                return ecriture_repository
                                                                                                .findByTenant_IdAndJournal_Id(
                                                                                                                tenant_id,
                                                                                                                journal.getId())
                                                                                                .flatMap(this::getFullEcritureDto)
                                                                                                .collectList()
                                                                                                .map(ecritures -> {
                                                                                                        dto.setEcriture_comptable(
                                                                                                                        ecritures);
                                                                                                        return dto;
                                                                                                });
                                                                        })
                                                                        .collectList()
                                                                        .flatMap(list -> redis_service
                                                                                        .save(cache_key, list, Duration
                                                                                                        .ofMinutes(15))
                                                                                        .thenReturn(list)));
                                });
        }

        /**
         * Updates an existing journal.
         */
        @Transactional
        public Mono<JournalComptableDto> updateJournalComptable(UUID id, JournalComptableDto dto) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> ReactiveTenantContext.getCurrentUser().defaultIfEmpty("system")
                                                .flatMap(user -> {
                                                        return journal_repository.findByTenant_IdAndId(tenant_id, id)
                                                                        .switchIfEmpty(Mono
                                                                                        .error(new ResourceNotFoundException(
                                                                                                        "JournalComptable",
                                                                                                        id.toString())))
                                                                        .flatMap(existing -> {
                                                                                return validateJournalComptableDto(dto)
                                                                                                .then(Mono.defer(() -> {
                                                                                                        existing.setCode_journal(
                                                                                                                        dto.getCode_journal());
                                                                                                        existing.setLibelle(
                                                                                                                        dto.getLibelle());
                                                                                                        existing.setType_journal(
                                                                                                                        dto.getType_journal());
                                                                                                        existing.setNotes(
                                                                                                                        dto.getNotes());
                                                                                                        existing.setActif(
                                                                                                                        dto.getActif());
                                                                                                        existing.setUpdated_by(
                                                                                                                        user);
                                                                                                        existing.setUpdated_at(
                                                                                                                        LocalDateTime.now());

                                                                                                        return journal_repository
                                                                                                                        .save(existing)
                                                                                                                        .flatMap(saved -> ReactiveTenantContext
                                                                                                                                        .getCurrentTenantAsTenant()
                                                                                                                                        .flatMap(tenant -> logAudit(
                                                                                                                                                        tenant,
                                                                                                                                                        user,
                                                                                                                                                        "JOURNAL_UPDATED",
                                                                                                                                                        "Update of journal "
                                                                                                                                                                        + dto.getCode_journal()))
                                                                                                                                        .then(redis_service
                                                                                                                                                        .delete(CACHE_JOURNAL_ALL
                                                                                                                                                                        + tenant_id))
                                                                                                                                        .then(redis_service
                                                                                                                                                        .delete(CACHE_JOURNAL_ACTIVE
                                                                                                                                                                        + tenant_id))
                                                                                                                                        .thenReturn(mapToDto(
                                                                                                                                                        saved)));
                                                                                                }));
                                                                        });
                                                }));
        }

        /**
         * Deletes a journal by its ID.
         */
        @Transactional
        public Mono<Void> deleteJournalComptable(UUID id) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> ReactiveTenantContext.getCurrentUser().defaultIfEmpty("system")
                                                .flatMap(user -> {
                                                        return journal_repository.findByTenant_IdAndId(tenant_id, id)
                                                                        .switchIfEmpty(Mono
                                                                                        .error(new ResourceNotFoundException(
                                                                                                        "JournalComptable",
                                                                                                        id.toString())))
                                                                        .flatMap(journal -> {
                                                                                return journal_repository
                                                                                                .delete(journal)
                                                                                                .then(ReactiveTenantContext
                                                                                                                .getCurrentTenantAsTenant()
                                                                                                                .flatMap(tenant -> logAudit(
                                                                                                                                tenant,
                                                                                                                                user,
                                                                                                                                "JOURNAL_DELETED",
                                                                                                                                "Deletion of journal "
                                                                                                                                                + journal.getCode_journal())))
                                                                                                .then(redis_service
                                                                                                                .delete(CACHE_JOURNAL_ALL
                                                                                                                                + tenant_id))
                                                                                                .then(redis_service
                                                                                                                .delete(CACHE_JOURNAL_ACTIVE
                                                                                                                                + tenant_id))
                                                                                                .then();
                                                                        });
                                                }));
        }

        /**
         * Validates a journal DTO.
         */
        private Mono<Void> validateJournalComptableDto(JournalComptableDto dto) {
                return Mono.defer(() -> {
                        var violations = validator.validate(dto);
                        if (!violations.isEmpty())
                                return Mono.error(new ConstraintViolationException(violations));

                        if (!dto.getCode_journal().matches("^[A-Z]{1,5}$")) {
                                return Mono.error(
                                                new IllegalArgumentException(
                                                                "Invalid journal code: must contain 1 to 5 uppercase letters."));
                        }

                        List<String> valid_types = List.of(
                                        AppConstants.JournalTypes.SALES,
                                        AppConstants.JournalTypes.PURCHASES,
                                        AppConstants.JournalTypes.CASH,
                                        AppConstants.JournalTypes.BANK,
                                        AppConstants.JournalTypes.GENERAL);

                        if (!valid_types.contains(dto.getType_journal())) {
                                return Mono.error(new IllegalArgumentException(
                                                "Invalid journal type: " + dto.getType_journal()));
                        }
                        return Mono.empty();
                });
        }

        /**
         * Logs an audit entry.
         */
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
                                .created_by(utilisateur)
                                .updated_by(utilisateur)
                                .build();

                return audit_repository.save(audit)
                                .flatMap(savedAudit -> {
                                        JournalAuditDto auditDto = JournalAuditDto.builder()
                                                        .id(savedAudit.getId())
                                                        .action(savedAudit.getAction())
                                                        .date_action(savedAudit.getDate_action())
                                                        .utilisateur(savedAudit.getUtilisateur())
                                                        .details(savedAudit.getDetails())
                                                        .created_at(savedAudit.getCreated_at())
                                                        .updated_at(savedAudit.getUpdated_at())
                                                        .created_by(savedAudit.getCreated_by())
                                                        .updated_by(savedAudit.getUpdated_by())
                                                        .build();

                                        return kafka_service.sendAuditLog(auditDto, tenant.getId(), action);
                                });
        }

        /**
         * Maps a DTO and tenant to a JournalComptable entity.
         */
        private JournalComptable mapToEntity(JournalComptableDto dto, Tenant tenant) {
                JournalComptable j = new JournalComptable();
                j.setId(dto.getId() != null ? dto.getId() : UUID.randomUUID());
                j.setTenantId(tenant.getId());
                j.setCode_journal(dto.getCode_journal());
                j.setLibelle(dto.getLibelle());
                j.setType_journal(dto.getType_journal());
                j.setNotes(dto.getNotes());
                j.setActif(dto.getActif() != null ? dto.getActif() : true);
                j.setCreated_at(dto.getCreated_at() != null ? dto.getCreated_at() : LocalDateTime.now());
                j.setUpdated_at(dto.getUpdated_at() != null ? dto.getUpdated_at() : LocalDateTime.now());
                return j;
        }

        /**
         * Maps a JournalComptable entity to its DTO.
         */
        private JournalComptableDto mapToDto(JournalComptable entity) {
                return JournalComptableDto.builder()
                                .id(entity.getId())
                                .code_journal(entity.getCode_journal())
                                .libelle(entity.getLibelle())
                                .type_journal(entity.getType_journal())
                                .notes(entity.getNotes())
                                .actif(entity.getActif())
                                .created_at(entity.getCreated_at())
                                .updated_at(entity.getUpdated_at())
                                .build();
        }

        /**
         * Maps an EcritureComptable entity to its DTO.
         */
        private EcritureComptableDto mapEcritureToDto(EcritureComptable e) {
                return EcritureComptableDto.builder()
                                .id(e.getId())
                                .numero_ecriture(e.getNumero_ecriture())
                                .libelle(e.getLibelle())
                                .date_ecriture(e.getDate_ecriture())
                                .journal_comptable_id(e.getJournal_id())
                                .periode_comptable_id(e.getPeriode_id())
                                .montant_total_debit(e.getMontant_total_debit())
                                .montant_total_credit(e.getMontant_total_credit())
                                .validee(e.getValidee())
                                .date_validation(e.getDate_validation())
                                .validated_by(e.getValidated_by())
                                .reference_externe(e.getReference_externe())
                                .notes(e.getNotes())
                                .created_at(e.getCreated_at())
                                .updated_at(e.getUpdated_at())
                                .details_ecriture(new ArrayList<>())
                                .build();
        }

        /**
         * Fetches an entry and its details, returning a complete DTO.
         */
        private Mono<EcritureComptableDto> getFullEcritureDto(EcritureComptable e) {
                EcritureComptableDto dto = mapEcritureToDto(e);
                return detail_repository.findByTenant_IdAndEcriture_Id(e.getTenantId(), e.getId())
                                .map(this::mapDetailToDto)
                                .collectList()
                                .map(details -> {
                                        dto.setDetails_ecriture(details);
                                        return dto;
                                });
        }

        private com.yowyob.erp.accounting.dto.DetailEcritureDto mapDetailToDto(
                        com.yowyob.erp.accounting.entity.DetailEcriture d) {
                return com.yowyob.erp.accounting.dto.DetailEcritureDto.builder()
                                .id(d.getId())
                                .ecriture_comptable_id(d.getEcriture_id())
                                .compte_comptable_id(d.getCompte_id())
                                .libelle(d.getLibelle())
                                .sens(d.getSens() != null ? d.getSens().name() : null)
                                .montant_debit(d.getMontant_debit())
                                .montant_credit(d.getMontant_credit())
                                .notes(d.getNotes())
                                .lettree(d.getLettree())
                                .date_lettrage(d.getDate_lettrage())
                                .pointee(d.getPointee())
                                .reference_bancaire(d.getReference_bancaire())
                                .date_ecriture(d.getDate_ecriture())
                                .build();
        }

        /**
         * Retrieves unique ledger accounts used in a specific journal.
         */
        public Mono<List<com.yowyob.erp.accounting.dto.CompteDto>> getComptesByJournal(UUID journalId) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> journal_repository.findByTenant_IdAndId(tenant_id, journalId)
                                                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                                                                "JournalComptable", journalId.toString())))
                                                .thenMany(compte_repository.findDistinctComptesByJournalId(tenant_id,
                                                                journalId))
                                                .map(c -> com.yowyob.erp.accounting.dto.CompteDto.builder()
                                                                .id(c.getId())
                                                                .no_compte(c.getNo_compte())
                                                                .libelle(c.getLibelle())
                                                                .type_compte(c.getType_compte())
                                                                .build())
                                                .collectList());
        }
}
