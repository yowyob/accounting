package com.yowyob.erp.accounting.application.service;
import com.yowyob.erp.accounting.domain.port.in.DetailEcritureUseCase;
import com.yowyob.erp.accounting.domain.port.in.JournalComptableUseCase;
import com.yowyob.erp.accounting.domain.port.in.PeriodeComptableUseCase;
import com.yowyob.erp.accounting.domain.port.in.EcritureComptableUseCase;

import com.yowyob.erp.accounting.infrastructure.web.dto.EcritureComptableDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.JournalAuditDto;
import com.yowyob.erp.accounting.domain.model.DetailEcriture;
import com.yowyob.erp.accounting.domain.model.EcritureComptable;
import com.yowyob.erp.accounting.domain.model.EcritureStatut;
import com.yowyob.erp.accounting.domain.model.JournalAudit;
import com.yowyob.erp.accounting.domain.model.Organization;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.EcritureComptableRepository;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.DetailEcritureRepository;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.JournalComptableRepository;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.PeriodeComptableRepository;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.JournalAuditRepository;
import com.yowyob.erp.shared.domain.model.ComptableObject;
import com.yowyob.erp.shared.domain.exception.BusinessException;
import com.yowyob.erp.shared.domain.exception.ResourceNotFoundException;
import com.yowyob.erp.config.kafka.KafkaMessageService;
import com.yowyob.erp.config.redis.RedisService;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
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
public class EcritureComptableService implements EcritureComptableUseCase {

        private static final String CACHE_ALL = "ecritures:all:";
        private static final String CACHE_NON_VALIDATED = "ecritures:nonvalidated:";
        private static final String CACHE_SEARCH = "ecritures:search:";

        private final EcritureComptableRepository ecriture_repository;
        private final DetailEcritureRepository detail_repository;
        @SuppressWarnings("unused")
        private final JournalComptableRepository journal_repository;
        private final DetailEcritureUseCase detail_service;
        @SuppressWarnings("unused")
        private final PeriodeComptableRepository periode_repository;
        private final JournalComptableUseCase journal_service;
        private final PeriodeComptableUseCase periode_service;
        private final JournalAuditRepository audit_repository;
        private final Validator validator_ref;
        private final KafkaMessageService kafka_message_service;
        private final RedisService redis_service;

        /**
         * Creates an accounting entry.
         */
        @Transactional
        public Mono<EcritureComptableDto> createEcriture(EcritureComptableDto dto) {
                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> ReactiveOrganizationContext.getCurrentUser()
                                                .defaultIfEmpty("system")
                                                .flatMap(current_user -> {
                                                        log.info("➡️ Creating accounting entry for organization: {}",
                                                                        organization_id);
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
                                                                        .then(ReactiveOrganizationContext
                                                                                        .getCurrentOrganizationAsOrganization())
                                                                        .flatMap(organization -> {
                                                                                EcritureComptable ecriture = mapToEntity(
                                                                                                dto, organization);
                                                                                ecriture.setId(UUID.randomUUID());
                                                                                ecriture.setOrganizationId(
                                                                                                organization.getId());
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
                                                                                ecriture.setStatut(
                                                                                                EcritureStatut.BROUILLON);
                                                                                ecriture.setActif(true);

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
                                                                                                                                        detail.setOrganizationId(
                                                                                                                                                        organization_id);
                                                                                                                                        detail.setEcriture_id(
                                                                                                                                                        saved.getId());
                                                                                                                                        detail.setCompte_id(
                                                                                                                                                        d
                                                                                                                                                                        .getCompte_comptable_id());
                                                                                                                                        detail.setLibelle(
                                                                                                                                                        d
                                                                                                                                                                        .getLibelle());
                                                                                                                                        detail.setSens(com.yowyob.erp.shared.domain.enums.Sens
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
                                                                                                                                                                organization,
                                                                                                                                                                saved.getId(),
                                                                                                                                                                current_user,
                                                                                                                                                                "ECRITURE_CREATED",
                                                                                                                                                                "Entry created"))
                                                                                                                                                .then(redis_service
                                                                                                                                                                .delete(CACHE_ALL
                                                                                                                                                                                + organization_id))
                                                                                                                                                .thenReturn(mapToDto(
                                                                                                                                                                saved));
                                                                                                                        });
                                                                                                });
                                                                        });
                                                }));
        }

        /**
         * Updates an existing accounting entry.
         */
        @Transactional
        public Mono<EcritureComptableDto> updateEcriture(UUID id, EcritureComptableDto dto) {
                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> ReactiveOrganizationContext.getCurrentUser()
                                                .defaultIfEmpty("system")
                                                .flatMap(current_user -> {
                                                        log.info("➡️ Updating accounting entry {} for organization: {}",
                                                                        id, organization_id);
                                                        validator_ref.validate(dto);

                                                        return ecriture_repository
                                                                        .findByOrganization_IdAndId(organization_id, id)
                                                                        .switchIfEmpty(Mono.error(
                                                                                        new ResourceNotFoundException(
                                                                                                        "Entry",
                                                                                                        id.toString())))
                                                                        .flatMap(existing -> {
                                                                                if (Boolean.TRUE.equals(existing
                                                                                                .getValidee())) {
                                                                                        return Mono.error(
                                                                                                        new BusinessException(
                                                                                                                        "Cannot update a validated entry"));
                                                                                }

                                                                                return journal_service
                                                                                                .getJournalComptable(dto
                                                                                                                .getJournal_comptable_id())
                                                                                                .then(periode_service
                                                                                                                .getPeriode(dto.getPeriode_comptable_id())
                                                                                                                .flatMap(p -> {
                                                                                                                        if (Boolean.TRUE.equals(
                                                                                                                                        p.getCloturee())) {
                                                                                                                                return Mono.error(
                                                                                                                                                new BusinessException(
                                                                                                                                                                "Accounting period is closed"));
                                                                                                                        }
                                                                                                                        return Mono.just(
                                                                                                                                        p);
                                                                                                                }))
                                                                                                .then(ReactiveOrganizationContext
                                                                                                                .getCurrentOrganizationAsOrganization())
                                                                                                .flatMap(organization -> {
                                                                                                        existing.setLibelle(
                                                                                                                        dto.getLibelle());
                                                                                                        existing.setDate_ecriture(
                                                                                                                        dto.getDate_ecriture());
                                                                                                        existing.setJournal_id(
                                                                                                                        dto.getJournal_comptable_id());
                                                                                                        existing.setPeriode_id(
                                                                                                                        dto.getPeriode_comptable_id());
                                                                                                        existing.setMontant_total_debit(
                                                                                                                        dto.getMontant_total_debit());
                                                                                                        existing.setMontant_total_credit(
                                                                                                                        dto.getMontant_total_credit());
                                                                                                        existing.setReference_externe(
                                                                                                                        dto.getReference_externe());
                                                                                                        existing.setNotes(
                                                                                                                        dto.getNotes());
                                                                                                        existing.setAttachment_ids(
                                                                                                                        dto.getAttachment_ids());
                                                                                                        existing.setUpdated_at(
                                                                                                                        LocalDateTime.now());
                                                                                                        existing.setUpdated_by(
                                                                                                                        current_user);
                                                                                                        existing.setNotNew();

                                                                                                        return detail_repository
                                                                                                                        .deleteByEcriture_id(
                                                                                                                                        id)
                                                                                                                        .then(Mono.defer(
                                                                                                                                        () -> {
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
                                                                                                                                                                                detail.setOrganizationId(
                                                                                                                                                                                                organization_id);
                                                                                                                                                                                detail.setEcriture_id(
                                                                                                                                                                                                existing.getId());
                                                                                                                                                                                detail.setCompte_id(
                                                                                                                                                                                                d.getCompte_comptable_id());
                                                                                                                                                                                detail.setLibelle(
                                                                                                                                                                                                d.getLibelle());
                                                                                                                                                                                detail.setSens(com.yowyob.erp.shared.domain.enums.Sens
                                                                                                                                                                                                .valueOf(d.getSens()));
                                                                                                                                                                                detail.setMontant_debit(
                                                                                                                                                                                                d.getMontant_debit());
                                                                                                                                                                                detail.setMontant_credit(
                                                                                                                                                                                                d.getMontant_credit());
                                                                                                                                                                                detail.setLettree(
                                                                                                                                                                                                d.getLettree());
                                                                                                                                                                                detail.setNotes(d
                                                                                                                                                                                                .getNotes());
                                                                                                                                                                                detail.setReference_bancaire(
                                                                                                                                                                                                d.getReference_bancaire());
                                                                                                                                                                                detail.setDate_ecriture(
                                                                                                                                                                                                existing.getDate_ecriture()
                                                                                                                                                                                                                .atStartOfDay());
                                                                                                                                                                                detail.setCreated_at(
                                                                                                                                                                                                LocalDateTime.now());
                                                                                                                                                                                detail.setUpdated_at(
                                                                                                                                                                                                LocalDateTime.now());
                                                                                                                                                                                detail.setCreated_by(
                                                                                                                                                                                                current_user);
                                                                                                                                                                                detail.setUpdated_by(
                                                                                                                                                                                                current_user);
                                                                                                                                                                                return detail;
                                                                                                                                                                        })
                                                                                                                                                                        .collect(java.util.stream.Collectors
                                                                                                                                                                                        .toList());

                                                                                                                                                        return detail_repository
                                                                                                                                                                        .saveAll(details_entities)
                                                                                                                                                                        .collectList();
                                                                                                                                                } else {
                                                                                                                                                        return Mono.just(
                                                                                                                                                                        java.util.Collections
                                                                                                                                                                                        .<DetailEcriture>emptyList());
                                                                                                                                                }
                                                                                                                                        }))
                                                                                                                        .flatMap(savedDetails -> {
                                                                                                                                existing.setDetails(
                                                                                                                                                savedDetails);
                                                                                                                                return validateBalance(
                                                                                                                                                savedDetails)
                                                                                                                                                .then(ecriture_repository
                                                                                                                                                                .save(existing))
                                                                                                                                                .flatMap(savedEcriture -> logAuditAndSendKafka(
                                                                                                                                                                organization,
                                                                                                                                                                savedEcriture.getId(),
                                                                                                                                                                current_user,
                                                                                                                                                                "ECRITURE_UPDATED",
                                                                                                                                                                "Entry updated")
                                                                                                                                                                .then(redis_service
                                                                                                                                                                                .delete(CACHE_ALL
                                                                                                                                                                                                + organization_id))
                                                                                                                                                                .thenReturn(mapToDto(
                                                                                                                                                                                savedEcriture)));
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
                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> {
                                        String current_user = user != null ? user : "system";
                                        return ecriture_repository.findByOrganization_IdAndId(organization_id, id)
                                                        .switchIfEmpty(Mono.error(new ResourceNotFoundException("Entry",
                                                                        id.toString())))
                                                        .flatMap(ecriture -> {
                                                                if (Boolean.TRUE.equals(ecriture.getValidee())) {
                                                                        return Mono.error(new BusinessException(
                                                                                        "Entry already validated"));
                                                                }

                                                                return detail_repository
                                                                                .findByOrganization_IdAndEcriture_Id(
                                                                                                organization_id, id)
                                                                                .collectList()
                                                                                .flatMap(this::validateBalance)
                                                                                .then(Mono.defer(() -> {
                                                                                        ecriture.setValidee(true);
                                                                                        ecriture.setStatut(
                                                                                                        EcritureStatut.VALIDE);
                                                                                        ecriture.setDate_validation(
                                                                                                        LocalDateTime.now());
                                                                                        ecriture.setValidated_by(
                                                                                                        current_user);
                                                                                        ecriture.setUpdated_at(
                                                                                                        LocalDateTime.now());
                                                                                        ecriture.setUpdated_by(
                                                                                                        current_user);
                                                                                        ecriture.setNotNew();

                                                                                        return ecriture_repository
                                                                                                        .save(ecriture)
                                                                                                        .flatMap(validated -> {
                                                                                                                return ReactiveOrganizationContext
                                                                                                                                .getCurrentOrganizationAsOrganization()
                                                                                                                                .flatMap(organization -> logAuditAndSendKafka(
                                                                                                                                                organization,
                                                                                                                                                id,
                                                                                                                                                current_user,
                                                                                                                                                "ECRITURE_VALIDATED",
                                                                                                                                                "Entry validated"))
                                                                                                                                .then(redis_service
                                                                                                                                                .delete(CACHE_NON_VALIDATED
                                                                                                                                                                + organization_id))
                                                                                                                                .thenReturn(mapToDto(
                                                                                                                                                validated));
                                                                                                        });
                                                                                }));
                                                        });
                                });
        }

        /**
         * Retrieves all entries for the current organization.
         */
        @SuppressWarnings("unchecked")
        public Mono<java.util.List<EcritureComptableDto>> getAll() {
                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> {
                                        String key = CACHE_ALL + organization_id;
                                        return redis_service.get(key, java.util.List.class)
                                                        .map(list -> (java.util.List<EcritureComptableDto>) list)
                                                        .switchIfEmpty(ecriture_repository
                                                                        .findByOrganization_Id(organization_id)
                                                                        .flatMap(ecriture -> detail_repository
                                                                                        .findByOrganization_IdAndEcriture_Id(
                                                                                                        organization_id,
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
         * Retrieves non-validated entries for the current organization.
         */
        @SuppressWarnings("unchecked")
        public Mono<java.util.List<EcritureComptableDto>> getNonValidated() {
                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> {
                                        String key = CACHE_NON_VALIDATED + organization_id;
                                        return redis_service.get(key, java.util.List.class)
                                                        .map(list -> (java.util.List<EcritureComptableDto>) list)
                                                        .switchIfEmpty(ecriture_repository
                                                                        .findByOrganization_IdAndValideeFalse(
                                                                                        organization_id)
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
                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> ecriture_repository
                                                .findByOrganization_IdAndId(organization_id, id)
                                                .flatMap(ecriture -> detail_repository
                                                                .findByOrganization_IdAndEcriture_Id(organization_id,
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
                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> {
                                        if (start_date != null && end_date != null && start_date.isAfter(end_date)) {
                                                return Mono.error(new BusinessException(
                                                                "Start date must be before end date"));
                                        }
                                        String cache_key = CACHE_SEARCH + organization_id + ":"
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
                                                                                        .findByOrganization_IdAndJournal_IdAndDate_ecritureBetween(
                                                                                                        organization_id,
                                                                                                        journal_id,
                                                                                                        start_date.toLocalDate(),
                                                                                                        end_date.toLocalDate());
                                                                } else if (start_date != null && end_date != null) {
                                                                        results = ecriture_repository
                                                                                        .findByOrganization_IdAndDate_ecritureBetween(
                                                                                                        organization_id,
                                                                                                        start_date.toLocalDate(),
                                                                                                        end_date.toLocalDate());
                                                                } else if (journal_id != null) {
                                                                        results = ecriture_repository
                                                                                        .findByOrganization_IdAndJournal_Id(
                                                                                                        organization_id,
                                                                                                        journal_id);
                                                                } else {
                                                                        results = ecriture_repository
                                                                                        .findByOrganization_Id(
                                                                                                        organization_id);
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
                return ReactiveOrganizationContext.getCurrentOrganizationAsOrganization()
                                .flatMap(organization -> ReactiveOrganizationContext.getCurrentUser()
                                                .defaultIfEmpty("system")
                                                .flatMap(current_user -> {
                                                        if (object.get_montant() == null
                                                                        || object.get_montant().doubleValue() <= 0)
                                                                return Mono.error(new BusinessException(
                                                                                "Invalid amount for entry generation"));

                                                        return journal_service
                                                                        .getJournalComptable(object
                                                                                        .get_journal_comptable_id())
                                                                        .then(periode_service.getCurrentPeriode(
                                                                                        organization.getId()))
                                                                        .flatMap(current_periode -> {
                                                                                EcritureComptable ecriture = EcritureComptable
                                                                                                .builder()
                                                                                                .id(UUID.randomUUID())
                                                                                                .organizationId(organization
                                                                                                                .getId())
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
                                                                                                .attachment_ids(object
                                                                                                                .get_attachment_ids())
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
                                                                                                                                        .findByOrganization_IdAndEcriture_Id(
                                                                                                                                                        organization.getId(),
                                                                                                                                                        saved.getId())
                                                                                                                                        .collectList()
                                                                                                                                        .flatMap(this::validateBalance))
                                                                                                                        .then(logAuditAndSendKafka(
                                                                                                                                        organization,
                                                                                                                                        saved.getId(),
                                                                                                                                        current_user,
                                                                                                                                        "ECRITURE_AUTO_GENERATED",
                                                                                                                                        "Entry generated automatically"))
                                                                                                                        .then(redis_service
                                                                                                                                        .delete(CACHE_ALL
                                                                                                                                                        + organization.getId()))
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
                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> ecriture_repository
                                                .findByOrganization_IdAndId(organization_id, id)
                                                .switchIfEmpty(Mono.error(
                                                                new ResourceNotFoundException("Entry", id.toString())))
                                                .flatMap(ecriture -> {
                                                        if (Boolean.TRUE.equals(ecriture.getValidee()))
                                                                return Mono.error(new BusinessException(
                                                                                "Cannot delete a validated entry"));

                                                        return ReactiveOrganizationContext.getCurrentUser()
                                                                        .defaultIfEmpty("system")
                                                                        .flatMap(current_user -> ReactiveOrganizationContext
                                                                                        .getCurrentOrganizationAsOrganization()
                                                                                        .flatMap(organization -> logAuditAndSendKafka(
                                                                                                        organization,
                                                                                                        id,
                                                                                                        current_user,
                                                                                                        "ECRITURE_DELETED",
                                                                                                        "Deletion of entry: "
                                                                                                                        + ecriture.getLibelle()))
                                                                                        .then(ecriture_repository
                                                                                                        .delete(ecriture))
                                                                                        .then(redis_service.delete(
                                                                                                        CACHE_ALL + organization_id))
                                                                                        .doOnSuccess(v -> log.info(
                                                                                                        "🗑️ Entry {} deleted",
                                                                                                        id))
                                                                                        .then());
                                                }));
        }

        /**
         * Cancels an accounting entry.
         */
        @Transactional
        public Mono<EcritureComptableDto> cancelEcriture(UUID id, String user) {
                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> {
                                        String current_user = user != null ? user : "system";
                                        return ecriture_repository.findByOrganization_IdAndId(organization_id, id)
                                                        .switchIfEmpty(Mono.error(new ResourceNotFoundException("Entry",
                                                                        id.toString())))
                                                        .flatMap(ecriture -> {
                                                                if (ecriture.getStatut() == EcritureStatut.ANNULE) {
                                                                        return Mono.error(new BusinessException(
                                                                                        "Entry already canceled"));
                                                                }

                                                                ecriture.setStatut(EcritureStatut.ANNULE);
                                                                ecriture.setUpdated_at(LocalDateTime.now());
                                                                ecriture.setUpdated_by(current_user);
                                                                ecriture.setNotNew();

                                                                return ecriture_repository.save(ecriture)
                                                                                .flatMap(saved -> ReactiveOrganizationContext
                                                                                                .getCurrentOrganizationAsOrganization()
                                                                                                .flatMap(organization -> logAuditAndSendKafka(
                                                                                                                organization,
                                                                                                                id,
                                                                                                                current_user,
                                                                                                                "ECRITURE_CANCELED",
                                                                                                                "Entry canceled"))
                                                                                                .then(redis_service
                                                                                                                .delete(CACHE_ALL
                                                                                                                                + organization_id))
                                                                                                .thenReturn(mapToDto(
                                                                                                                saved)));
                                                        });
                                });
        }

        /**
         * Deactivates an accounting entry (soft delete).
         */
        @Transactional
        public Mono<Void> deactivateEcriture(UUID id) {
                return ReactiveOrganizationContext.getOrganizationId()
                                .flatMap(organization_id -> ecriture_repository
                                                .findByOrganization_IdAndId(organization_id, id)
                                                .switchIfEmpty(Mono.error(
                                                                new ResourceNotFoundException("Entry", id.toString())))
                                                .flatMap(ecriture -> {
                                                        ecriture.setActif(false);
                                                        ecriture.setUpdated_at(LocalDateTime.now());
                                                        return ReactiveOrganizationContext.getCurrentUser()
                                                                        .defaultIfEmpty("system")
                                                                        .flatMap(user -> {
                                                                                ecriture.setUpdated_by(user);
                                                                                ecriture.setNotNew();
                                                                                return ecriture_repository
                                                                                                .save(ecriture);
                                                                        });
                                                }))
                                .then();
        }

        private Mono<Void> logAuditAndSendKafka(Organization organization, UUID ecriture_id, String user, String action,
                        String details) {
                JournalAudit audit = JournalAudit.builder()
                                .id(UUID.randomUUID())
                                .organizationId(organization.getId())
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

                                        return kafka_message_service.sendAuditLog(auditDto, organization.getId(),
                                                        action);
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

        private EcritureComptable mapToEntity(EcritureComptableDto dto, Organization organization) {
                return EcritureComptable.builder()
                                .id(dto.getId())
                                .organizationId(organization.getId())
                                .numero_ecriture(dto.getNumero_ecriture())
                                .libelle(dto.getLibelle())
                                .date_ecriture(dto.getDate_ecriture())
                                .journal_id(dto.getJournal_comptable_id())
                                .periode_id(dto.getPeriode_comptable_id())
                                .montant_total_debit(dto.getMontant_total_debit())
                                .montant_total_credit(dto.getMontant_total_credit())
                                .validee(dto.getValidee())
                                .statut(dto.getStatut() != null ? EcritureStatut.valueOf(dto.getStatut()) : null)
                                .actif(dto.getActif() != null ? dto.getActif() : true)
                                .reference_externe(dto.getReference_externe())
                                .notes(dto.getNotes())
                                .attachment_ids(dto.getAttachment_ids())
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
                                .statut(e.getStatut() != null ? e.getStatut().name() : null)
                                .actif(e.getActif())
                                .reference_externe(e.getReference_externe())
                                .notes(e.getNotes())
                                .created_at(e.getCreated_at())
                                .updated_at(e.getUpdated_at())
                                .attachment_ids(e.getAttachment_ids())
                                .details_ecriture(e.getDetails() != null
                                                ? e.getDetails().stream().map(this::mapDetailToDto)
                                                                .collect(java.util.stream.Collectors.toList())
                                                : null)
                                .build();
        }

        private com.yowyob.erp.accounting.infrastructure.web.dto.DetailEcritureDto mapDetailToDto(DetailEcriture entity) {
                return com.yowyob.erp.accounting.infrastructure.web.dto.DetailEcritureDto.builder()
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
