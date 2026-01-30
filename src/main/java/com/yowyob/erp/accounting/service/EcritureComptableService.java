package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.EcritureComptableDto;
import com.yowyob.erp.accounting.dto.JournalAuditDto;
import com.yowyob.erp.accounting.entity.DetailEcriture;
import com.yowyob.erp.accounting.entity.EcritureComptable;
import com.yowyob.erp.accounting.entity.JournalAudit;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.accounting.repository.EcritureComptableRepository;
import com.yowyob.erp.accounting.repository.DetailEcritureRepository;
import com.yowyob.erp.accounting.repository.JournalComptableRepository;
import com.yowyob.erp.accounting.repository.PeriodeComptableRepository;
import com.yowyob.erp.accounting.repository.JournalAuditRepository;
import com.yowyob.erp.common.entity.ComptableObject;
import com.yowyob.erp.common.exception.BusinessException;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import com.yowyob.erp.config.kafka.KafkaMessageService;
import com.yowyob.erp.config.redis.RedisService;
import com.yowyob.erp.config.tenant.ReactiveTenantContext;
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
import java.util.UUID;

/**
 * Reactive Service for managing accounting entries.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EcritureComptableService {

        private static final String CACHE_ALL = "ecritures:all:";
        private static final String CACHE_NON_VALIDATED = "ecritures:nonvalidated:";
        private static final String CACHE_SEARCH = "ecritures:search:";

        private final EcritureComptableRepository ecriture_repository;
        private final DetailEcritureRepository detail_repository;
        @SuppressWarnings("unused")
        private final JournalComptableRepository journal_repository;
        private final DetailEcritureService detail_service;
        @SuppressWarnings("unused")
        private final PeriodeComptableRepository periode_repository;
        private final JournalComptableService journal_service;
        private final PeriodeComptableService periode_service;
        private final JournalAuditRepository audit_repository;
        private final Validator validator_ref;
        private final KafkaMessageService kafka_message_service;
        private final RedisService redis_service;

        /**
         * Creates an accounting entry.
         */
        @Transactional
        public Mono<EcritureComptableDto> createEcriture(EcritureComptableDto dto) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> ReactiveTenantContext.getCurrentUser().defaultIfEmpty("system")
                                                .flatMap(current_user -> {
                                                        log.info("➡️ Creating accounting entry for tenant: {}",
                                                                        tenant_id);
                                                        validator_ref.validate(dto);

                                                        return journal_service
                                                                        .getJournalComptable(
                                                                                        dto.getJournal_comptable_id())
                                                                        .then(periode_service.getPeriode(
                                                                                        dto.getPeriode_comptable_id())
                                                                                        .flatMap(p -> {
                                                                                                if (Boolean.TRUE.equals(
                                                                                                                p.getCloturee())) {
                                                                                                        return Mono.error(
                                                                                                                        new BusinessException(
                                                                                                                                        "Accounting period is closed"));
                                                                                                }
                                                                                                return Mono.just(p);
                                                                                        }))
                                                                        .then(ReactiveTenantContext
                                                                                        .getCurrentTenantAsTenant())
                                                                        .flatMap(tenant -> {
                                                                                EcritureComptable ecriture = mapToEntity(
                                                                                                dto, tenant);
                                                                                ecriture.setId(UUID.randomUUID());
                                                                                ecriture.setTenantId(tenant.getId());
                                                                                ecriture.setNumero_ecriture(
                                                                                                "ECR-" + UUID.randomUUID()
                                                                                                                .toString()
                                                                                                                .substring(0, 8));
                                                                                ecriture.setCreated_at(
                                                                                                LocalDateTime.now());
                                                                                ecriture.setUpdated_at(
                                                                                                LocalDateTime.now());
                                                                                ecriture.setCreated_by(current_user);
                                                                                ecriture.setUpdated_by(current_user);
                                                                                ecriture.setValidee(false);

                                                                                return ecriture_repository
                                                                                                .save(ecriture)
                                                                                                .flatMap(saved -> {
                                                                                                        log.info("✅ Entry {} created",
                                                                                                                        saved.getNumero_ecriture());

                                                                                                        Mono<java.util.List<DetailEcriture>> detailsMono;

                                                                                                        if (dto.getDetails_ecriture() != null
                                                                                                                        && !dto.getDetails_ecriture()
                                                                                                                                        .isEmpty()) {
                                                                                                                java.util.List<DetailEcriture> details_entities = dto
                                                                                                                                .getDetails_ecriture()
                                                                                                                                .stream()
                                                                                                                                .map(d -> {
                                                                                                                                        DetailEcriture detail = new DetailEcriture();
                                                                                                                                        detail.setId(UUID
                                                                                                                                                        .randomUUID());
                                                                                                                                        detail.setTenantId(
                                                                                                                                                        tenant_id);
                                                                                                                                        detail.setEcriture_id(
                                                                                                                                                        saved.getId());
                                                                                                                                        detail.setCompte_id(
                                                                                                                                                        d
                                                                                                                                                                        .getCompte_comptable_id());
                                                                                                                                        detail.setLibelle(
                                                                                                                                                        d
                                                                                                                                                                        .getLibelle());
                                                                                                                                        detail.setSens(com.yowyob.erp.common.enums.Sens
                                                                                                                                                        .valueOf(d
                                                                                                                                                                        .getSens()));
                                                                                                                                        detail.setMontant_debit(
                                                                                                                                                        d.getMontant_debit());
                                                                                                                                        detail.setMontant_credit(
                                                                                                                                                        d.getMontant_credit());
                                                                                                                                        detail.setLettree(
                                                                                                                                                        d
                                                                                                                                                                        .getLettree());
                                                                                                                                        detail.setNotes(d
                                                                                                                                                        .getNotes());
                                                                                                                                        detail.setReference_bancaire(
                                                                                                                                                        d.getReference_bancaire());
                                                                                                                                        detail.setDate_ecriture(
                                                                                                                                                        saved.getDate_ecriture()
                                                                                                                                                                        .atStartOfDay());
                                                                                                                                        detail.setCreated_at(
                                                                                                                                                        LocalDateTime
                                                                                                                                                                        .now());
                                                                                                                                        detail.setUpdated_at(
                                                                                                                                                        LocalDateTime
                                                                                                                                                                        .now());
                                                                                                                                        detail.setCreated_by(
                                                                                                                                                        current_user);
                                                                                                                                        detail.setUpdated_by(
                                                                                                                                                        current_user);
                                                                                                                                        return detail;
                                                                                                                                })
                                                                                                                                .collect(java.util.stream.Collectors
                                                                                                                                                .toList());

                                                                                                                detailsMono = detail_repository
                                                                                                                                .saveAll(details_entities)
                                                                                                                                .collectList();
                                                                                                        } else {
                                                                                                                detailsMono = Mono
                                                                                                                                .just(java.util.Collections
                                                                                                                                                .emptyList());
                                                                                                        }

                                                                                                        return detailsMono
                                                                                                                        .flatMap(savedDetails -> {
                                                                                                                                saved.setDetails(
                                                                                                                                                savedDetails);
                                                                                                                                return validateBalance(
                                                                                                                                                savedDetails)
                                                                                                                                                .then(logAuditAndSendKafka(
                                                                                                                                                                tenant,
                                                                                                                                                                saved.getId(),
                                                                                                                                                                current_user,
                                                                                                                                                                "ECRITURE_CREATED",
                                                                                                                                                                "Entry created"))
                                                                                                                                                .then(redis_service
                                                                                                                                                                .delete(CACHE_ALL
                                                                                                                                                                                + tenant_id))
                                                                                                                                                .thenReturn(mapToDto(
                                                                                                                                                                saved));
                                                                                                                        });
                                                                                                });
                                                                        });
                                                }));
        }

        /**
         * Validates an accounting entry.
         */
        @Transactional
        public Mono<EcritureComptableDto> validateEcriture(UUID id, String user) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> {
                                        String current_user = user != null ? user : "system";
                                        return ecriture_repository.findByTenant_IdAndId(tenant_id, id)
                                                        .switchIfEmpty(Mono.error(new ResourceNotFoundException("Entry",
                                                                        id.toString())))
                                                        .flatMap(ecriture -> {
                                                                if (Boolean.TRUE.equals(ecriture.getValidee())) {
                                                                        return Mono.error(new BusinessException(
                                                                                        "Entry already validated"));
                                                                }

                                                                return detail_repository
                                                                                .findByTenant_IdAndEcriture_Id(
                                                                                                tenant_id, id)
                                                                                .collectList()
                                                                                .flatMap(this::validateBalance)
                                                                                .then(Mono.defer(() -> {
                                                                                        ecriture.setValidee(true);
                                                                                        ecriture.setDate_validation(
                                                                                                        LocalDateTime.now());
                                                                                        ecriture.setValidated_by(
                                                                                                        current_user);
                                                                                        ecriture.setUpdated_at(
                                                                                                        LocalDateTime.now());
                                                                                        ecriture.setUpdated_by(
                                                                                                        current_user);

                                                                                        return ecriture_repository
                                                                                                        .save(ecriture)
                                                                                                        .flatMap(validated -> {
                                                                                                                return ReactiveTenantContext
                                                                                                                                .getCurrentTenantAsTenant()
                                                                                                                                .flatMap(tenant -> logAuditAndSendKafka(
                                                                                                                                                tenant,
                                                                                                                                                id,
                                                                                                                                                current_user,
                                                                                                                                                "ECRITURE_VALIDATED",
                                                                                                                                                "Entry validated"))
                                                                                                                                .then(redis_service
                                                                                                                                                .delete(CACHE_NON_VALIDATED
                                                                                                                                                                + tenant_id))
                                                                                                                                .thenReturn(mapToDto(
                                                                                                                                                validated));
                                                                                                        });
                                                                                }));
                                                        });
                                });
        }

        /**
         * Retrieves all entries for the current tenant.
         */
        @SuppressWarnings("unchecked")
        public Mono<java.util.List<EcritureComptableDto>> getAll() {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> {
                                        String key = CACHE_ALL + tenant_id;
                                        return redis_service.get(key, java.util.List.class)
                                                        .map(list -> (java.util.List<EcritureComptableDto>) list)
                                                        .switchIfEmpty(ecriture_repository.findByTenant_Id(tenant_id)
                                                                        .flatMap(ecriture -> detail_repository
                                                                                        .findByTenant_IdAndEcriture_Id(
                                                                                                        tenant_id,
                                                                                                        ecriture.getId())
                                                                                        .collectList()
                                                                                        .map(details -> {
                                                                                                ecriture.setDetails(
                                                                                                                details);
                                                                                                return ecriture;
                                                                                        }))
                                                                        .map(this::mapToDto)
                                                                        .collectList()
                                                                        .flatMap(list -> redis_service
                                                                                        .save(key, list, Duration
                                                                                                        .ofMinutes(10))
                                                                                        .thenReturn(list)));
                                });
        }

        /**
         * Retrieves non-validated entries for the current tenant.
         */
        @SuppressWarnings("unchecked")
        public Mono<java.util.List<EcritureComptableDto>> getNonValidated() {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> {
                                        String key = CACHE_NON_VALIDATED + tenant_id;
                                        return redis_service.get(key, java.util.List.class)
                                                        .map(list -> (java.util.List<EcritureComptableDto>) list)
                                                        .switchIfEmpty(ecriture_repository
                                                                        .findByTenant_IdAndValideeFalse(tenant_id)
                                                                        .map(this::mapToDto)
                                                                        .collectList()
                                                                        .flatMap(list -> redis_service
                                                                                        .save(key, list, Duration
                                                                                                        .ofMinutes(10))
                                                                                        .thenReturn(list)));
                                });
        }

        /**
         * Retrieves an entry by its ID.
         */
        public Mono<EcritureComptableDto> getById(UUID id) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> ecriture_repository.findByTenant_IdAndId(tenant_id, id)
                                                .flatMap(ecriture -> detail_repository
                                                                .findByTenant_IdAndEcriture_Id(tenant_id,
                                                                                ecriture.getId())
                                                                .collectList()
                                                                .map(details -> {
                                                                        ecriture.setDetails(details);
                                                                        return ecriture;
                                                                }))
                                                .map(this::mapToDto));
        }

        /**
         * Search entries by date range and journal.
         */
        @SuppressWarnings("unchecked")
        public Mono<java.util.List<EcritureComptableDto>> searchEcritures(LocalDateTime start_date,
                        LocalDateTime end_date,
                        UUID journal_id) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> {
                                        if (start_date != null && end_date != null && start_date.isAfter(end_date)) {
                                                return Mono.error(new BusinessException(
                                                                "Start date must be before end date"));
                                        }
                                        String cache_key = CACHE_SEARCH + tenant_id + ":"
                                                        + (start_date != null ? start_date : "all")
                                                        + ":" + (end_date != null ? end_date : "all")
                                                        + ":" + (journal_id != null ? journal_id : "all");

                                        return redis_service.get(cache_key, java.util.List.class)
                                                        .map(list -> (java.util.List<EcritureComptableDto>) list)
                                                        .switchIfEmpty(Mono.defer(() -> {
                                                                Flux<EcritureComptable> results;
                                                                if (start_date != null && end_date != null
                                                                                && journal_id != null) {
                                                                        results = ecriture_repository
                                                                                        .findByTenant_IdAndJournal_IdAndDate_ecritureBetween(
                                                                                                        tenant_id,
                                                                                                        journal_id,
                                                                                                        start_date.toLocalDate(),
                                                                                                        end_date.toLocalDate());
                                                                } else if (start_date != null && end_date != null) {
                                                                        results = ecriture_repository
                                                                                        .findByTenant_IdAndDate_ecritureBetween(
                                                                                                        tenant_id,
                                                                                                        start_date.toLocalDate(),
                                                                                                        end_date.toLocalDate());
                                                                } else if (journal_id != null) {
                                                                        results = ecriture_repository
                                                                                        .findByTenant_IdAndJournal_Id(
                                                                                                        tenant_id,
                                                                                                        journal_id);
                                                                } else {
                                                                        results = ecriture_repository
                                                                                        .findByTenant_Id(tenant_id);
                                                                }

                                                                return results.map(this::mapToDto).collectList()
                                                                                .flatMap(list -> redis_service.save(
                                                                                                cache_key, list,
                                                                                                Duration.ofMinutes(5))
                                                                                                .thenReturn(list));
                                                        }));
                                });
        }

        /**
         * Generates an accounting entry from a comptable object.
         */
        @Transactional
        public Mono<EcritureComptableDto> generateFromComptableObject(ComptableObject object) {
                return ReactiveTenantContext.getCurrentTenantAsTenant()
                                .flatMap(tenant -> ReactiveTenantContext.getCurrentUser().defaultIfEmpty("system")
                                                .flatMap(current_user -> {
                                                        if (object.get_montant() == null
                                                                        || object.get_montant().doubleValue() <= 0)
                                                                return Mono.error(new BusinessException(
                                                                                "Invalid amount for entry generation"));

                                                        return journal_service
                                                                        .getJournalComptable(object
                                                                                        .get_journal_comptable_id())
                                                                        .then(periode_service.getCurrentPeriode(
                                                                                        tenant.getId()))
                                                                        .flatMap(current_periode -> {
                                                                                EcritureComptable ecriture = EcritureComptable
                                                                                                .builder()
                                                                                                .id(UUID.randomUUID())
                                                                                                .tenantId(tenant.getId())
                                                                                                .numero_ecriture("ECR-"
                                                                                                                + UUID.randomUUID()
                                                                                                                                .toString()
                                                                                                                                .substring(0, 8))
                                                                                                .libelle(object.get_description() != null
                                                                                                                ? object.get_description()
                                                                                                                : "Auto-generated entry")
                                                                                                .journal_id(object
                                                                                                                .get_journal_comptable_id())
                                                                                                .periode_id(current_periode
                                                                                                                .getId())
                                                                                                .montant_total_debit(
                                                                                                                object.get_montant())
                                                                                                .montant_total_credit(
                                                                                                                object.get_montant())
                                                                                                .validee(false)
                                                                                                .created_at(LocalDateTime
                                                                                                                .now())
                                                                                                .updated_at(LocalDateTime
                                                                                                                .now())
                                                                                                .created_by(current_user)
                                                                                                .updated_by(current_user)
                                                                                                .build();

                                                                                return ecriture_repository
                                                                                                .save(ecriture)
                                                                                                .flatMap(saved -> {
                                                                                                        log.info("🧾 Entry generated automatically from object {}",
                                                                                                                        object.get_source_type());
                                                                                                        return detail_service
                                                                                                                        .generateDetailsFromComptableObject(
                                                                                                                                        saved,
                                                                                                                                        object)
                                                                                                                        .then(detail_repository
                                                                                                                                        .findByTenant_IdAndEcriture_Id(
                                                                                                                                                        tenant.getId(),
                                                                                                                                                        saved.getId())
                                                                                                                                        .collectList()
                                                                                                                                        .flatMap(this::validateBalance))
                                                                                                                        .then(logAuditAndSendKafka(
                                                                                                                                        tenant,
                                                                                                                                        saved.getId(),
                                                                                                                                        current_user,
                                                                                                                                        "ECRITURE_AUTO_GENERATED",
                                                                                                                                        "Entry generated automatically"))
                                                                                                                        .then(redis_service
                                                                                                                                        .delete(CACHE_ALL
                                                                                                                                                        + tenant.getId()))
                                                                                                                        .thenReturn(mapToDto(
                                                                                                                                        saved));
                                                                                                });
                                                                        });
                                                }));
        }

        /**
         * Deletes an accounting entry.
         */
        @Transactional
        public Mono<Void> deleteEcriture(UUID id) {
                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenant_id -> ecriture_repository.findByTenant_IdAndId(tenant_id, id)
                                                .switchIfEmpty(Mono.error(
                                                                new ResourceNotFoundException("Entry", id.toString())))
                                                .flatMap(ecriture -> {
                                                        if (Boolean.TRUE.equals(ecriture.getValidee()))
                                                                return Mono.error(new BusinessException(
                                                                                "Cannot delete a validated entry"));

                                                        return ReactiveTenantContext.getCurrentUser()
                                                                        .defaultIfEmpty("system")
                                                                        .flatMap(current_user -> ReactiveTenantContext
                                                                                        .getCurrentTenantAsTenant()
                                                                                        .flatMap(tenant -> logAuditAndSendKafka(
                                                                                                        tenant,
                                                                                                        id,
                                                                                                        current_user,
                                                                                                        "ECRITURE_DELETED",
                                                                                                        "Deletion of entry: "
                                                                                                                        + ecriture.getLibelle()))
                                                                                        .then(ecriture_repository
                                                                                                        .delete(ecriture))
                                                                                        .then(redis_service.delete(
                                                                                                        CACHE_ALL + tenant_id))
                                                                                        .doOnSuccess(v -> log.info(
                                                                                                        "🗑️ Entry {} deleted",
                                                                                                        id))
                                                                                        .then());
                                                }));
        }

        private Mono<Void> logAuditAndSendKafka(Tenant tenant, UUID ecriture_id, String user, String action,
                        String details) {
                JournalAudit audit = JournalAudit.builder()
                                .id(UUID.randomUUID())
                                .tenantId(tenant.getId())
                                .ecriture_comptable_id(ecriture_id)
                                .utilisateur(user)
                                .action(action)
                                .details(details)
                                .date_action(LocalDateTime.now())
                                .created_at(LocalDateTime.now())
                                .updated_at(LocalDateTime.now())
                                .created_by(user)
                                .updated_by(user)
                                .build();

                return audit_repository.save(audit)
                                .flatMap(savedAudit -> {
                                        JournalAuditDto auditDto = JournalAuditDto.builder()
                                                        .id(savedAudit.getId())
                                                        .ecriture_comptable_id(savedAudit.getEcriture_comptable_id())
                                                        .action(savedAudit.getAction())
                                                        .date_action(savedAudit.getDate_action())
                                                        .utilisateur(savedAudit.getUtilisateur())
                                                        .details(savedAudit.getDetails())
                                                        .build();

                                        return kafka_message_service.sendAuditLog(auditDto, tenant.getId(), action);
                                });
        }

        private Mono<Void> validateBalance(java.util.List<DetailEcriture> details) {
                BigDecimal debit = details.stream()
                                .map(DetailEcriture::getMontant_debit)
                                .map(m -> m != null ? m : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal credit = details.stream()
                                .map(DetailEcriture::getMontant_credit)
                                .map(m -> m != null ? m : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                if (debit.subtract(credit).abs().compareTo(BigDecimal.valueOf(0.01)) > 0) {
                        return Mono.error(new BusinessException(
                                        "Unbalanced entry: debit=" + debit + " credit=" + credit));
                }
                return Mono.empty();
        }

        private EcritureComptable mapToEntity(EcritureComptableDto dto, Tenant tenant) {
                return EcritureComptable.builder()
                                .id(dto.getId())
                                .tenantId(tenant.getId())
                                .numero_ecriture(dto.getNumero_ecriture())
                                .libelle(dto.getLibelle())
                                .date_ecriture(dto.getDate_ecriture())
                                .journal_id(dto.getJournal_comptable_id())
                                .periode_id(dto.getPeriode_comptable_id())
                                .montant_total_debit(dto.getMontant_total_debit())
                                .montant_total_credit(dto.getMontant_total_credit())
                                .validee(dto.getValidee())
                                .reference_externe(dto.getReference_externe())
                                .notes(dto.getNotes())
                                .build();
        }

        private EcritureComptableDto mapToDto(EcritureComptable e) {
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
                                .reference_externe(e.getReference_externe())
                                .notes(e.getNotes())
                                .created_at(e.getCreated_at())
                                .updated_at(e.getUpdated_at())
                                .details_ecriture(e.getDetails() != null
                                                ? e.getDetails().stream().map(this::mapDetailToDto)
                                                                .collect(java.util.stream.Collectors.toList())
                                                : null)
                                .build();
        }

        private com.yowyob.erp.accounting.dto.DetailEcritureDto mapDetailToDto(DetailEcriture entity) {
                return com.yowyob.erp.accounting.dto.DetailEcritureDto.builder()
                                .id(entity.getId())
                                .ecriture_comptable_id(entity.getEcriture_id())
                                .compte_comptable_id(entity.getCompte_id())
                                .libelle(entity.getLibelle())
                                .sens(entity.getSens() != null ? entity.getSens().name() : null)
                                .montant_debit(entity.getMontant_debit())
                                .montant_credit(entity.getMontant_credit())
                                .lettree(entity.getLettree())
                                .date_lettrage(entity.getDate_lettrage())
                                .pointee(entity.getPointee())
                                .reference_bancaire(entity.getReference_bancaire())
                                .notes(entity.getNotes())
                                .date_ecriture(entity.getDate_ecriture())
                                .build();
        }
}
