package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.entity.Compte;
import com.yowyob.erp.accounting.entity.DetailEcriture;
import com.yowyob.erp.accounting.entity.EcritureComptable;
import com.yowyob.erp.accounting.entity.JournalAudit;
import com.yowyob.erp.accounting.entity.OperationComptable;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.accounting.entity.Transaction;
import com.yowyob.erp.accounting.repository.CompteRepository;
import com.yowyob.erp.accounting.repository.DetailEcritureRepository;
import com.yowyob.erp.accounting.repository.JournalAuditRepository;
import com.yowyob.erp.common.entity.ComptableObject;
import com.yowyob.erp.common.enums.Sens;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import com.yowyob.erp.config.tenant.ReactiveTenantContext;
import com.yowyob.erp.accounting.dto.DetailEcritureDto;
import com.yowyob.erp.accounting.dto.JournalAuditDto;
import com.yowyob.erp.config.kafka.KafkaMessageService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Reactive Service for managing the creation, update, and deletion of
 * accounting entry
 * details.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DetailEcritureService {

        private static final String COMPTE_TVA_STATIQUE = "445710";
        private static final String COMPTE_CLIENT_DYNAMIQUE = "411000";

        private final DetailEcritureRepository detail_repository;
        private final CompteRepository compte_repository;
        private final JournalAuditRepository journal_audit_repository;
        private final Validator validator;
        private final KafkaMessageService kafka_message_service;

        /**
         * Manually creates a new accounting entry detail.
         */
        @Transactional
        public Mono<DetailEcriture> createDetailEcriture(DetailEcriture detail, Tenant tenant,
                        EcritureComptable ecriture) {
                return ReactiveTenantContext.getCurrentUser()
                                .defaultIfEmpty("system")
                                .flatMap(current_user -> {
                                        return validateDetailEcriture(detail)
                                                        .then(Mono.defer(() -> {
                                                                detail.setTenantId(tenant.getId());
                                                                detail.setEcriture_id(ecriture.getId());
                                                                detail.setDate_ecriture(LocalDateTime.now());
                                                                detail.setCreated_at(LocalDateTime.now());
                                                                detail.setUpdated_at(LocalDateTime.now());
                                                                detail.setCreated_by(current_user);
                                                                detail.setUpdated_by(current_user);

                                                                // Set opposite amount to zero based on sens
                                                                Sens sens = detail.getSens();
                                                                if (sens == Sens.DEBIT) {
                                                                        detail.setMontant_credit(BigDecimal.ZERO);
                                                                } else if (sens == Sens.CREDIT) {
                                                                        detail.setMontant_debit(BigDecimal.ZERO);
                                                                }

                                                                return detail_repository.save(detail)
                                                                                .flatMap(saved -> logAudit(tenant,
                                                                                                ecriture.getId(),
                                                                                                current_user, "CREATE",
                                                                                                "Creation of entry detail "
                                                                                                                + saved.getId())
                                                                                                .then(Mono.fromRunnable(
                                                                                                                () -> {
                                                                                                                        DetailEcritureDto savedDto = mapToDto(
                                                                                                                                        saved);
                                                                                                                        kafka_message_service
                                                                                                                                        .sendAccountingEvent(
                                                                                                                                                        savedDto,
                                                                                                                                                        tenant.getId(),
                                                                                                                                                        "DETAIL_CREATED");
                                                                                                                }))
                                                                                                .thenReturn(saved));
                                                        }));
                                });
        }

        /**
         * Generates accounting lines from a predefined operation and transaction.
         */
        public Mono<Void> generateDetailsFromOperation(EcritureComptable ecriture, OperationComptable operation,
                        Transaction transaction) {
                Tenant tenant = ecriture.getTenant();
                if (tenant == null) {
                        return Mono.error(new IllegalStateException("Tenant is required in entry"));
                }

                return Mono.zip(
                                compte_repository
                                                .findByTenant_IdAndId(tenant.getId(),
                                                                operation.getCompte_principal_id())
                                                .filter(Compte::getActif)
                                                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Main account",
                                                                operation.getCompte_principal_id().toString()))),
                                Mono.just(operation.getEst_compte_statique() ? COMPTE_TVA_STATIQUE
                                                : COMPTE_CLIENT_DYNAMIQUE)
                                                .flatMap(no -> compte_repository
                                                                .findByTenant_IdAndNo_compte(tenant.getId(), no)
                                                                .filter(Compte::getActif)
                                                                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                                                                                "Counter account", no)))))
                                .flatMap(tuple -> {
                                        Compte principal_account = tuple.getT1();
                                        Compte counter_account = tuple.getT2();

                                        LocalDateTime now = LocalDateTime.now();
                                        String libelle = String.format("Transaction %s – Operation: %s",
                                                        transaction.getNumero_recu(), operation.getType_operation());

                                        return ReactiveTenantContext.getCurrentUser().defaultIfEmpty("system")
                                                        .flatMap(current_user -> {
                                                                // Debit line
                                                                DetailEcriture debit = DetailEcriture.builder()
                                                                                .tenantId(tenant.getId())
                                                                                .ecriture_id(ecriture.getId())
                                                                                .compte_id(principal_account.getId())
                                                                                .libelle(libelle)
                                                                                .sens(Sens.DEBIT)
                                                                                .montant_debit(transaction
                                                                                                .getMontant_transaction())
                                                                                .montant_credit(BigDecimal.ZERO)
                                                                                .date_ecriture(now)
                                                                                .created_at(now)
                                                                                .updated_at(now)
                                                                                .created_by(current_user)
                                                                                .updated_by(current_user)
                                                                                .build();

                                                                // Credit line
                                                                DetailEcriture credit = DetailEcriture.builder()
                                                                                .tenantId(tenant.getId())
                                                                                .ecriture_id(ecriture.getId())
                                                                                .compte_id(counter_account.getId())
                                                                                .libelle(libelle)
                                                                                .sens(Sens.CREDIT)
                                                                                .montant_credit(transaction
                                                                                                .getMontant_transaction())
                                                                                .montant_debit(BigDecimal.ZERO)
                                                                                .date_ecriture(now)
                                                                                .created_at(now)
                                                                                .updated_at(now)
                                                                                .created_by(current_user)
                                                                                .updated_by(current_user)
                                                                                .build();

                                                                return createDetailEcriture(debit, tenant, ecriture)
                                                                                .then(createDetailEcriture(credit,
                                                                                                tenant, ecriture))
                                                                                .doOnSuccess(v -> log.info(
                                                                                                "💾 Details generated for entry [{}] : debit={}, credit={}",
                                                                                                ecriture.getId(),
                                                                                                debit.getMontant_debit(),
                                                                                                credit.getMontant_credit()))
                                                                                .then();
                                                        });
                                });
        }

        /**
         * Generates accounting lines from a generic accounting object.
         */
        public Mono<Void> generateDetailsFromComptableObject(EcritureComptable ecriture, ComptableObject object) {
                Tenant tenant = ecriture.getTenant();
                if (tenant == null) {
                        return Mono.error(new IllegalStateException("Tenant is required in entry"));
                }

                return Mono.zip(
                                compte_repository
                                                .findByTenant_IdAndNo_compte(tenant.getId(), object.get_debit_account())
                                                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Debit account",
                                                                object.get_debit_account()))),
                                compte_repository
                                                .findByTenant_IdAndNo_compte(tenant.getId(),
                                                                object.get_credit_account())
                                                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                                                                "Credit account", object.get_credit_account()))))
                                .flatMap(tuple -> {
                                        Compte debit_account = tuple.getT1();
                                        Compte credit_account = tuple.getT2();

                                        LocalDateTime now = LocalDateTime.now();
                                        BigDecimal montant = object.get_montant();
                                        String libelle = object.get_description() != null ? object.get_description()
                                                        : "Auto entry " + object.get_source_type();

                                        return ReactiveTenantContext.getCurrentUser().defaultIfEmpty("system")
                                                        .flatMap(current_user -> {
                                                                // Debit
                                                                DetailEcriture debit = DetailEcriture.builder()
                                                                                .tenantId(tenant.getId())
                                                                                .ecriture_id(ecriture.getId())
                                                                                .compte_id(debit_account.getId())
                                                                                .libelle(libelle)
                                                                                .sens(Sens.DEBIT)
                                                                                .montant_debit(montant)
                                                                                .montant_credit(BigDecimal.ZERO)
                                                                                .date_ecriture(now)
                                                                                .created_at(now)
                                                                                .updated_at(now)
                                                                                .created_by(current_user)
                                                                                .updated_by(current_user)
                                                                                .build();

                                                                // Credit
                                                                DetailEcriture credit = DetailEcriture.builder()
                                                                                .tenantId(tenant.getId())
                                                                                .ecriture_id(ecriture.getId())
                                                                                .compte_id(credit_account.getId())
                                                                                .libelle(libelle)
                                                                                .sens(Sens.CREDIT)
                                                                                .montant_credit(montant)
                                                                                .montant_debit(BigDecimal.ZERO)
                                                                                .date_ecriture(now)
                                                                                .created_at(now)
                                                                                .updated_at(now)
                                                                                .created_by(current_user)
                                                                                .updated_by(current_user)
                                                                                .build();

                                                                return createDetailEcriture(debit, tenant, ecriture)
                                                                                .then(createDetailEcriture(credit,
                                                                                                tenant, ecriture))
                                                                                .doOnSuccess(v -> log.info(
                                                                                                "⚙️ Details generated from accounting object [{}] : {} → {} ({} F)",
                                                                                                object.get_source_type(),
                                                                                                debit_account.getNo_compte(),
                                                                                                credit_account.getNo_compte(),
                                                                                                montant))
                                                                                .then();
                                                        });
                                });
        }

        /**
         * Retrieves an entry detail by ID and tenant.
         */
        public Mono<DetailEcriture> getDetailEcriture(UUID id, Tenant tenant) {
                return validateTenantAccess()
                                .then(detail_repository.findById(id))
                                .filter(d -> tenant.getId().equals(d.getTenantId()));
        }

        /**
         * Lists all details for a tenant.
         */
        public Flux<DetailEcriture> getAllDetailsEcriture(Tenant tenant) {
                return validateTenantAccess()
                                .thenMany(detail_repository.findByTenantId(tenant.getId()));
        }

        /**
         * Lists details for a specific entry.
         */
        public Flux<DetailEcriture> getDetailsByEcriture(Tenant tenant, EcritureComptable ecriture) {
                return validateTenantAccess()
                                .thenMany(detail_repository.findByTenant_IdAndEcriture_Id(tenant.getId(),
                                                ecriture.getId()));
        }

        /**
         * Updates an existing entry detail.
         */
        @Transactional
        public Mono<DetailEcriture> updateDetailEcriture(UUID id, DetailEcriture updated_detail, Tenant tenant,
                        EcritureComptable ecriture) {
                return validateTenantAccess()
                                .then(ReactiveTenantContext.getCurrentUser().defaultIfEmpty("system"))
                                .flatMap(current_user -> {
                                        return detail_repository.findById(id)
                                                        .filter(d -> tenant.getId().equals(d.getTenantId()))
                                                        .switchIfEmpty(Mono.error(new IllegalArgumentException(
                                                                        "Entry detail not found: " + id)))
                                                        .flatMap(existing -> {
                                                                return validateDetailEcriture(updated_detail)
                                                                                .then(Mono.defer(() -> {
                                                                                        existing.setLibelle(
                                                                                                        updated_detail.getLibelle());
                                                                                        existing.setSens(updated_detail
                                                                                                        .getSens());
                                                                                        existing.setMontant_debit(
                                                                                                        updated_detail.getMontant_debit());
                                                                                        existing.setMontant_credit(
                                                                                                        updated_detail.getMontant_credit());
                                                                                        existing.setNotes(updated_detail
                                                                                                        .getNotes());
                                                                                        existing.setUpdated_at(
                                                                                                        LocalDateTime.now());
                                                                                        existing.setUpdated_by(
                                                                                                        current_user);

                                                                                        // Set opposite amount to zero
                                                                                        // based on sens
                                                                                        Sens sens = existing.getSens();
                                                                                        if (sens == Sens.DEBIT) {
                                                                                                existing.setMontant_credit(
                                                                                                                BigDecimal.ZERO);
                                                                                        } else if (sens == Sens.CREDIT) {
                                                                                                existing.setMontant_debit(
                                                                                                                BigDecimal.ZERO);
                                                                                        }

                                                                                        return detail_repository
                                                                                                        .save(existing)
                                                                                                        .flatMap(saved -> logAudit(
                                                                                                                        tenant,
                                                                                                                        ecriture.getId(),
                                                                                                                        current_user,
                                                                                                                        "UPDATE",
                                                                                                                        "Update of entry detail "
                                                                                                                                        + saved.getId())
                                                                                                                        .then(Mono.fromRunnable(
                                                                                                                                        () -> {
                                                                                                                                                DetailEcritureDto savedDto = mapToDto(
                                                                                                                                                                saved);
                                                                                                                                                kafka_message_service
                                                                                                                                                                .sendAccountingEvent(
                                                                                                                                                                                savedDto,
                                                                                                                                                                                tenant.getId(),
                                                                                                                                                                                "DETAIL_UPDATED");
                                                                                                                                        }))
                                                                                                                        .thenReturn(saved));
                                                                                }));
                                                        });
                                });
        }

        /**
         * Deletes an entry detail.
         */
        @Transactional
        public Mono<Void> deleteDetailEcriture(UUID id, Tenant tenant, EcritureComptable ecriture) {
                return validateTenantAccess()
                                .then(ReactiveTenantContext.getCurrentUser().defaultIfEmpty("system"))
                                .flatMap(current_user -> {
                                        return detail_repository.findById(id)
                                                        .filter(d -> tenant.getId().equals(d.getTenantId()))
                                                        .switchIfEmpty(Mono.error(new IllegalArgumentException(
                                                                        "Entry detail not found: " + id)))
                                                        .flatMap(detail -> {
                                                                return detail_repository.delete(detail)
                                                                                .then(logAudit(tenant, ecriture.getId(),
                                                                                                current_user, "DELETE",
                                                                                                "Deletion of entry detail "
                                                                                                                + id))
                                                                                .then(kafka_message_service
                                                                                                .sendAccountingEvent(
                                                                                                                id,
                                                                                                                tenant.getId(),
                                                                                                                "DETAIL_DELETED"))
                                                                                .doOnSuccess(v -> log.info(
                                                                                                "🗑️ Entry detail deleted: {}",
                                                                                                id));
                                                        });
                                });
        }

        /**
         * Validates the data of an entry detail.
         */
        private Mono<Void> validateDetailEcriture(DetailEcriture detail) {
                return Mono.defer(() -> {
                        var violations = validator.validate(detail);
                        if (!violations.isEmpty())
                                return Mono.error(new ConstraintViolationException(violations));

                        if (detail.getCompte_id() == null) {
                                return Mono.error(new IllegalArgumentException("Compte ID is required"));
                        }

                        return ReactiveTenantContext.getTenantId()
                                        .flatMap(tenant_id -> {
                                                return compte_repository.findById(detail.getCompte_id())
                                                                .filter(p -> tenant_id.equals(p.getTenantId()))
                                                                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                                                                                "Accounting account not found: "
                                                                                                + detail.getCompte_id())))
                                                                .flatMap(plan -> {
                                                                        if (!Boolean.TRUE.equals(plan.getActif()))
                                                                                return Mono.error(
                                                                                                new IllegalArgumentException(
                                                                                                                "Inactive account: "
                                                                                                                                + plan.getNo_compte()));

                                                                        if (detail.getSens() == Sens.DEBIT && (detail
                                                                                        .getMontant_debit() == null
                                                                                        || detail.getMontant_debit()
                                                                                                        .compareTo(BigDecimal.ZERO) <= 0))
                                                                                return Mono.error(
                                                                                                new IllegalArgumentException(
                                                                                                                "Debit amount must be positive"));

                                                                        if (detail.getSens() == Sens.CREDIT && (detail
                                                                                        .getMontant_credit() == null
                                                                                        || detail.getMontant_credit()
                                                                                                        .compareTo(BigDecimal.ZERO) <= 0))
                                                                                return Mono.error(
                                                                                                new IllegalArgumentException(
                                                                                                                "Credit amount must be positive"));

                                                                        return Mono.empty();
                                                                });
                                        }).then();
                });
        }

        /**
         * Logs an audit action for an entry detail change.
         */
        private Mono<Void> logAudit(Tenant tenant, UUID ecriture_comptable_id, String utilisateur, String action,
                        String details) {
                JournalAudit audit = JournalAudit.builder()
                                .id(UUID.randomUUID())
                                .tenantId(tenant.getId())
                                .ecriture_comptable_id(ecriture_comptable_id)
                                .utilisateur(utilisateur)
                                .action(action)
                                .details(details)
                                .date_action(LocalDateTime.now())
                                .created_at(LocalDateTime.now())
                                .updated_at(LocalDateTime.now())
                                .created_by(utilisateur)
                                .updated_by(utilisateur)
                                .build();

                return journal_audit_repository.save(audit)
                                .flatMap(saved -> {
                                        JournalAuditDto auditDto = JournalAuditDto.builder()
                                                        .id(saved.getId())
                                                        .action(action)
                                                        .utilisateur(utilisateur)
                                                        .details(details)
                                                        .date_action(saved.getDate_action())
                                                        .ecriture_comptable_id(ecriture_comptable_id)
                                                        .build();

                                        kafka_message_service.sendAuditLog(auditDto, tenant.getId(), action);
                                        return Mono.empty();
                                });
        }

        /**
         * Ensures that a tenant context is defined.
         */
        private Mono<Void> validateTenantAccess() {
                return ReactiveTenantContext.getTenantId()
                                .switchIfEmpty(Mono
                                                .error(new SecurityException("Access denied: Tenant ID not defined")))
                                .then();
        }

        private DetailEcritureDto mapToDto(DetailEcriture entity) {
                return DetailEcritureDto.builder()
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
