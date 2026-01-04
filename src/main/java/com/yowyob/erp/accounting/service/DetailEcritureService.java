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
import com.yowyob.erp.config.tenant.TenantContext;
import com.yowyob.erp.accounting.dto.JournalAuditDto;
import com.yowyob.erp.config.kafka.KafkaMessageService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing the creation, update, and deletion of accounting entry
 * details.
 * Compatible with PostgreSQL + Kafka + Multi-tenant.
 *
 * @author ALD
 * @date 30.09.25
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
         * 
         * @param detail   the detail to create
         * @param tenant   the tenant context
         * @param ecriture the associated entry
         * @return the created detail
         */
        @Transactional
        public DetailEcriture createDetailEcriture(DetailEcriture detail, Tenant tenant, EcritureComptable ecriture) {
                String current_user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

                validateDetailEcriture(detail);
                detail.setTenant(tenant);
                detail.setEcriture(ecriture);
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

                DetailEcriture saved = detail_repository.save(detail);
                logAudit(tenant, ecriture.getId(), current_user, "CREATE",
                                "Creation of entry detail " + saved.getId());
                kafka_message_service.sendAccountingEvent(saved, tenant.getId(), "DETAIL_CREATED");

                log.info("✅ Entry detail created successfully: {}", saved.getId());
                return saved;
        }

        /**
         * Generates accounting lines from a predefined operation and transaction.
         * 
         * @param ecriture    the entry
         * @param operation   the accounting operation template
         * @param transaction the source transaction
         */
        public void generateDetailsFromOperation(EcritureComptable ecriture, OperationComptable operation,
                        Transaction transaction) {
                Tenant tenant = ecriture.getTenant();
                String current_user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

                Compte principal_account = compte_repository
                                .findByTenant_IdAndNo_compte(tenant.getId(), operation.getCompte_principal())
                                .filter(Compte::getActif)
                                .orElseThrow(() -> new ResourceNotFoundException("Main account",
                                                operation.getCompte_principal()));

                String counter_account_no = operation.getEst_compte_statique() ? COMPTE_TVA_STATIQUE
                                : COMPTE_CLIENT_DYNAMIQUE;
                Compte counter_account = compte_repository
                                .findByTenant_IdAndNo_compte(tenant.getId(), counter_account_no)
                                .filter(Compte::getActif)
                                .orElseThrow(() -> new ResourceNotFoundException("Counter account",
                                                counter_account_no));

                LocalDateTime now = LocalDateTime.now();
                String libelle = String.format("Transaction %s – Operation: %s",
                                transaction.getNumero_recu(), operation.getType_operation());

                // Debit line
                DetailEcriture debit = DetailEcriture.builder()
                                .id(UUID.randomUUID())
                                .tenant(tenant)
                                .ecriture(ecriture)
                                .compte(principal_account)
                                .libelle(libelle)
                                .sens(Sens.DEBIT)
                                .montant_debit(transaction.getMontant_transaction())
                                .montant_credit(BigDecimal.ZERO)
                                .date_ecriture(now)
                                .created_at(now)
                                .updated_at(now)
                                .created_by(current_user)
                                .updated_by(current_user)
                                .build();

                // Credit line
                DetailEcriture credit = DetailEcriture.builder()
                                .id(UUID.randomUUID())
                                .tenant(tenant)
                                .ecriture(ecriture)
                                .compte(counter_account)
                                .libelle(libelle)
                                .sens(Sens.CREDIT)
                                .montant_credit(transaction.getMontant_transaction())
                                .montant_debit(BigDecimal.ZERO)
                                .date_ecriture(now)
                                .created_at(now)
                                .updated_at(now)
                                .created_by(current_user)
                                .updated_by(current_user)
                                .build();

                createDetailEcriture(debit, tenant, ecriture);
                createDetailEcriture(credit, tenant, ecriture);

                log.info("💾 Details generated for entry [{}] : debit={}, credit={}",
                                ecriture.getId(), debit.getMontant_debit(), credit.getMontant_credit());
        }

        /**
         * Generates accounting lines from a generic accounting object.
         * 
         * @param ecriture the entry
         * @param object   the accounting object
         */
        public void generateDetailsFromComptableObject(EcritureComptable ecriture, ComptableObject object) {
                Tenant tenant = ecriture.getTenant();
                String current_user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");
                LocalDateTime now = LocalDateTime.now();

                Compte debit_account = compte_repository
                                .findByTenant_IdAndNo_compte(tenant.getId(), object.get_debit_account())
                                .orElseThrow(() -> new ResourceNotFoundException("Debit account",
                                                object.get_debit_account()));

                Compte credit_account = compte_repository
                                .findByTenant_IdAndNo_compte(tenant.getId(), object.get_credit_account())
                                .orElseThrow(() -> new ResourceNotFoundException("Credit account",
                                                object.get_credit_account()));

                BigDecimal montant = object.get_montant();
                String libelle = object.get_description() != null ? object.get_description()
                                : "Auto entry " + object.get_source_type();

                // Debit
                DetailEcriture debit = DetailEcriture.builder()
                                .id(UUID.randomUUID())
                                .tenant(tenant)
                                .ecriture(ecriture)
                                .compte(debit_account)
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
                                .id(UUID.randomUUID())
                                .tenant(tenant)
                                .ecriture(ecriture)
                                .compte(credit_account)
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

                createDetailEcriture(debit, tenant, ecriture);
                createDetailEcriture(credit, tenant, ecriture);

                log.info("⚙️ Details generated from accounting object [{}] : {} → {} ({} F)",
                                object.get_source_type(), debit_account.getNo_compte(), credit_account.getNo_compte(),
                                montant);
        }

        /**
         * Retrieves an entry detail by ID and tenant.
         * 
         * @param id     the detail ID
         * @param tenant the tenant
         * @return optional detail
         */
        public Optional<DetailEcriture> getDetailEcriture(UUID id, Tenant tenant) {
                validateTenantAccess();
                return detail_repository.findById(id)
                                .filter(d -> d.getTenant().equals(tenant));
        }

        /**
         * Lists all details for a tenant.
         * 
         * @param tenant the tenant
         * @return list of details
         */
        public List<DetailEcriture> getAllDetailsEcriture(Tenant tenant) {
                validateTenantAccess();
                return detail_repository.findByTenant_Id(tenant.getId());
        }

        /**
         * Lists details for a specific entry.
         * 
         * @param tenant   the tenant
         * @param ecriture the entry
         * @return list of details
         */
        public List<DetailEcriture> getDetailsByEcriture(Tenant tenant, EcritureComptable ecriture) {
                validateTenantAccess();
                return detail_repository.findByTenant_IdAndEcriture_Id(tenant.getId(), ecriture.getId());
        }

        /**
         * Updates an existing entry detail.
         * 
         * @param id             the detail ID
         * @param updated_detail the updated data
         * @param tenant         the tenant context
         * @param ecriture       the associated entry
         * @return the updated detail
         */
        @Transactional
        public DetailEcriture updateDetailEcriture(UUID id, DetailEcriture updated_detail, Tenant tenant,
                        EcritureComptable ecriture) {
                String current_user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");
                validateTenantAccess();

                DetailEcriture existing = detail_repository.findById(id)
                                .filter(d -> d.getTenant().equals(tenant))
                                .orElseThrow(() -> new IllegalArgumentException("Entry detail not found: " + id));

                validateDetailEcriture(updated_detail);

                existing.setLibelle(updated_detail.getLibelle());
                existing.setSens(updated_detail.getSens());
                existing.setMontant_debit(updated_detail.getMontant_debit());
                existing.setMontant_credit(updated_detail.getMontant_credit());
                existing.setNotes(updated_detail.getNotes());
                existing.setUpdated_at(LocalDateTime.now());
                existing.setUpdated_by(current_user);

                // Set opposite amount to zero based on sens
                Sens sens = existing.getSens();
                if (sens == Sens.DEBIT) {
                        existing.setMontant_credit(BigDecimal.ZERO);
                } else if (sens == Sens.CREDIT) {
                        existing.setMontant_debit(BigDecimal.ZERO);
                }

                DetailEcriture saved = detail_repository.save(existing);
                logAudit(tenant, ecriture.getId(), current_user, "UPDATE",
                                "Update of entry detail " + saved.getId());
                kafka_message_service.sendAccountingEvent(saved, tenant.getId(), "DETAIL_UPDATED");

                log.info("✏️ Entry detail updated: {}", saved.getId());
                return saved;
        }

        /**
         * Deletes an entry detail.
         * 
         * @param id       the detail ID
         * @param tenant   the tenant context
         * @param ecriture the associated entry
         */
        @Transactional
        public void deleteDetailEcriture(UUID id, Tenant tenant, EcritureComptable ecriture) {
                validateTenantAccess();
                String current_user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

                DetailEcriture detail = detail_repository.findById(id)
                                .filter(d -> d.getTenant().equals(tenant))
                                .orElseThrow(() -> new IllegalArgumentException("Entry detail not found: " + id));

                detail_repository.delete(detail);
                logAudit(tenant, ecriture.getId(), current_user, "DELETE",
                                "Deletion of entry detail " + id);
                kafka_message_service.sendAccountingEvent(id, tenant.getId(), "DETAIL_DELETED");

                log.info("🗑️ Entry detail deleted: {}", id);
        }

        /**
         * Validates the data of an entry detail.
         * 
         * @param detail the detail to validate
         */
        private void validateDetailEcriture(DetailEcriture detail) {
                var violations = validator.validate(detail);
                if (!violations.isEmpty())
                        throw new ConstraintViolationException(violations);

                Tenant tenant = TenantContext.getCurrentTenantAsTenant();
                Compte plan = compte_repository
                                .findById(detail.getCompte().getId())
                                .filter(p -> p.getTenant().equals(tenant))
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Accounting account not found: " + detail.getCompte().getId()));

                if (!Boolean.TRUE.equals(plan.getActif()))
                        throw new IllegalArgumentException("Inactive account: " + plan.getNo_compte());

                if (detail.getSens() == Sens.DEBIT && detail.getMontant_debit().compareTo(BigDecimal.ZERO) <= 0)
                        throw new IllegalArgumentException("Debit amount must be positive");

                if (detail.getSens() == Sens.CREDIT && detail.getMontant_credit().compareTo(BigDecimal.ZERO) <= 0)
                        throw new IllegalArgumentException("Credit amount must be positive");
        }

        /**
         * Logs an audit action for an entry detail change.
         * 
         * @param tenant                the tenant context
         * @param ecriture_comptable_id the entry ID
         * @param utilisateur           the user performing the action
         * @param action                the action name
         * @param details               descriptive details of the action
         */
        private void logAudit(Tenant tenant, UUID ecriture_comptable_id, String utilisateur, String action,
                        String details) {
                JournalAudit audit = JournalAudit.builder()
                                .tenant(tenant)
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

                journal_audit_repository.save(audit);

                // Publish to Kafka via standardized service
                JournalAuditDto auditDto = JournalAuditDto.builder()
                                .id(audit.getId())
                                .action(action)
                                .utilisateur(utilisateur)
                                .details(details)
                                .date_action(audit.getDate_action())
                                .ecriture_comptable_id(ecriture_comptable_id)
                                .build();

                kafka_message_service.sendAuditLog(auditDto, tenant.getId(), action);
        }

        /**
         * Ensures that a tenant context is defined.
         */
        private void validateTenantAccess() {
                if (TenantContext.getCurrentTenant() == null)
                        throw new SecurityException("Access denied: Tenant ID not defined");
        }
}
