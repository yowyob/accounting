package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.entity.*;
import com.yowyob.erp.accounting.repository.*;
import com.yowyob.erp.common.entity.ComptableObject;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import com.yowyob.erp.config.tenant.TenantContext;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import com.yowyob.erp.common.enums.Sens;

/**
 * Service for managing the creation, update, and deletion of accounting entry details.
 * Compatible with PostgreSQL + Kafka + Multi-tenant.
 *
 * @author ALD
 * @date 12/10/2025 06:22 AM WAT
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DetailEcritureService {

    private static final String COMPTE_TVA_STATIQUE = "445710";
    private static final String COMPTE_CLIENT_DYNAMIQUE = "411000";

    private final DetailEcritureRepository detailRepository;
    private final CompteRepository compteRepository;
    private final JournalAuditRepository journalAuditRepository;
    private final Validator validator;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /* MANUAL CREATION */
    @Transactional
    public DetailEcriture createDetailEcriture(DetailEcriture detail, Tenant tenant, EcritureComptable ecriture) {
        String currentUser = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        validateDetailEcriture(detail);
        detail.setTenant(tenant);
        detail.setEcriture(ecriture);
        detail.setDateEcriture(LocalDateTime.now());
        detail.setCreatedAt(LocalDateTime.now());
        detail.setUpdatedAt(LocalDateTime.now());
        detail.setCreatedBy(currentUser);
        detail.setUpdatedBy(currentUser);

        // Set opposite amount to zero based on sens
        Sens sens = detail.getSens();
        if (sens == Sens.DEBIT) {
            detail.setMontantCredit(BigDecimal.ZERO);
        } else if (sens == Sens.CREDIT) {
            detail.setMontantDebit(BigDecimal.ZERO);
        }

        DetailEcriture saved = detailRepository.save(detail);
        logAudit(tenant, ecriture.getId(), currentUser, "CREATE",
                "Creation of entry detail " + saved.getId());
        kafkaTemplate.send("detail.ecriture.created", tenant.getId().toString(), saved);

        log.info("✅ Entry detail created successfully: {}", saved.getId());
        return saved;
    }

    /* AUTOMATIC GENERATION FROM OPERATION + TRANSACTION */
    public void generateDetailsFromOperation(EcritureComptable ecriture, OperationComptable operation, Transaction transaction) {
        Tenant tenant = ecriture.getTenant();
        String currentUser = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        Compte principalAccount = compteRepository
                .findByTenant_IdAndNoCompte(tenant.getId(), operation.getComptePrincipal())
                .filter(Compte::getActif)
                .orElseThrow(() -> new ResourceNotFoundException("Main account", operation.getComptePrincipal()));

        String counterAccountNo = operation.getEstCompteStatique() ? COMPTE_TVA_STATIQUE : COMPTE_CLIENT_DYNAMIQUE;
        Compte counterAccount = compteRepository
                .findByTenant_IdAndNoCompte(tenant.getId(), counterAccountNo)
                .filter(Compte::getActif)
                .orElseThrow(() -> new ResourceNotFoundException("Counter account", counterAccountNo));

        LocalDateTime now = LocalDateTime.now();
        String libelle = String.format("Transaction %s – Operation: %s",
                transaction.getNumeroRecu(), operation.getTypeOperation());

        // Debit line
        DetailEcriture debit = DetailEcriture.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .ecriture(ecriture)
                .compte(principalAccount)
                .libelle(libelle)
                .sens(Sens.DEBIT)
                .montantDebit(transaction.getMontantTransaction())
                .montantCredit(BigDecimal.ZERO)
                .dateEcriture(now)
                .createdAt(now)
                .updatedAt(now)
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();

        // Credit line
        DetailEcriture credit = DetailEcriture.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .ecriture(ecriture)
                .compte(counterAccount)
                .libelle(libelle)
                .sens(Sens.CREDIT)
                .montantCredit(transaction.getMontantTransaction())
                .montantDebit(BigDecimal.ZERO)
                .dateEcriture(now)
                .createdAt(now)
                .updatedAt(now)
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();

        createDetailEcriture(debit, tenant, ecriture);
        createDetailEcriture(credit, tenant, ecriture);

        log.info("💾 Details generated for entry [{}] : debit={}, credit={}",
                ecriture.getId(), debit.getMontantDebit(), credit.getMontantCredit());
    }

    /* AUTOMATIC GENERATION FROM GENERIC ACCOUNTING OBJECT */
    public void generateDetailsFromComptableObject(EcritureComptable ecriture, ComptableObject object) {
        Tenant tenant = ecriture.getTenant();
        String currentUser = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");
        LocalDateTime now = LocalDateTime.now();

        Compte debitAccount = compteRepository
                .findByTenant_IdAndNoCompte(tenant.getId(), object.getDebitAccount())
                .orElseThrow(() -> new ResourceNotFoundException("Debit account", object.getDebitAccount()));

        Compte creditAccount = compteRepository
                .findByTenant_IdAndNoCompte(tenant.getId(), object.getCreditAccount())
                .orElseThrow(() -> new ResourceNotFoundException("Credit account", object.getCreditAccount()));

        BigDecimal montant = object.getMontant();
        String libelle = object.getDescription() != null ? object.getDescription()
                : "Auto entry " + object.getSourceType();

        // Debit
        DetailEcriture debit = DetailEcriture.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .ecriture(ecriture)
                .compte(debitAccount)
                .libelle(libelle)
                .sens(Sens.DEBIT)
                .montantDebit(montant)
                .montantCredit(BigDecimal.ZERO)
                .dateEcriture(now)
                .createdAt(now)
                .updatedAt(now)
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();

        // Credit
        DetailEcriture credit = DetailEcriture.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .ecriture(ecriture)
                .compte(creditAccount)
                .libelle(libelle)
                .sens(Sens.CREDIT)
                .montantCredit(montant)
                .montantDebit(BigDecimal.ZERO)
                .dateEcriture(now)
                .createdAt(now)
                .updatedAt(now)
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();

        createDetailEcriture(debit, tenant, ecriture);
        createDetailEcriture(credit, tenant, ecriture);

        log.info("⚙️ Details generated from accounting object [{}] : {} → {} ({} F)",
                object.getSourceType(), debitAccount.getNoCompte(), creditAccount.getNoCompte(), montant);
    }

    /* READING */
    public Optional<DetailEcriture> getDetailEcriture(UUID id, Tenant tenant) {
        validateTenantAccess();
        return detailRepository.findById(id)
                .filter(d -> d.getTenant().equals(tenant));
    }

    public List<DetailEcriture> getAllDetailsEcriture(Tenant tenant) {
        validateTenantAccess();
        return detailRepository.findByTenant_Id(tenant.getId());
    }

    public List<DetailEcriture> getDetailsByEcriture(Tenant tenant, EcritureComptable ecriture) {
        validateTenantAccess();
        return detailRepository.findByTenant_IdAndEcriture_Id(tenant.getId(), ecriture.getId());
    }

    /* UPDATE */
    @Transactional
    public DetailEcriture updateDetailEcriture(UUID id, DetailEcriture updatedDetail, Tenant tenant, EcritureComptable ecriture) {
        String currentUser = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");
        validateTenantAccess();

        DetailEcriture existing = detailRepository.findById(id)
                .filter(d -> d.getTenant().equals(tenant))
                .orElseThrow(() -> new IllegalArgumentException("Entry detail not found: " + id));

        validateDetailEcriture(updatedDetail);

        existing.setLibelle(updatedDetail.getLibelle());
        existing.setSens(updatedDetail.getSens());
        existing.setMontantDebit(updatedDetail.getMontantDebit());
        existing.setMontantCredit(updatedDetail.getMontantCredit());
        existing.setNotes(updatedDetail.getNotes());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(currentUser);

        // Set opposite amount to zero based on sens
        Sens sens = existing.getSens();
        if (sens == Sens.DEBIT) {
            existing.setMontantCredit(BigDecimal.ZERO);
        } else if (sens == Sens.CREDIT) {
            existing.setMontantDebit(BigDecimal.ZERO);
        }

        DetailEcriture saved = detailRepository.save(existing);
        logAudit(tenant, ecriture.getId(), currentUser, "UPDATE",
                "Update of entry detail " + saved.getId());
        kafkaTemplate.send("detail.ecriture.updated", tenant.getId().toString(), saved);

        log.info("✏️ Entry detail updated: {}", saved.getId());
        return saved;
    }

    /* DELETION */
    @Transactional
    public void deleteDetailEcriture(UUID id, Tenant tenant, EcritureComptable ecriture) {
        validateTenantAccess();
        String currentUser = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        DetailEcriture detail = detailRepository.findById(id)
                .filter(d -> d.getTenant().equals(tenant))
                .orElseThrow(() -> new IllegalArgumentException("Entry detail not found: " + id));

        detailRepository.delete(detail);
        logAudit(tenant, ecriture.getId(), currentUser, "DELETE",
                "Deletion of entry detail " + id);
        kafkaTemplate.send("detail.ecriture.deleted", tenant.getId().toString(), id);

        log.info("🗑️ Entry detail deleted: {}", id);
    }

    /* VALIDATION */
    private void validateDetailEcriture(DetailEcriture detail) {
        var violations = validator.validate(detail);
        if (!violations.isEmpty()) throw new ConstraintViolationException(violations);

        Tenant tenant = TenantContext.getCurrentTenantAsTenant();
        Compte plan = compteRepository
                .findById(detail.getCompte().getId())
                .filter(p -> p.getTenant().equals(tenant))
                .orElseThrow(() ->
                        new IllegalArgumentException("Accounting account not found: " + detail.getCompte().getId()));

        if (!Boolean.TRUE.equals(plan.getActif()))
            throw new IllegalArgumentException("Inactive account: " + plan.getNoCompte());

        if (detail.getSens() == Sens.DEBIT && detail.getMontantDebit().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Debit amount must be positive");

        if (detail.getSens() == Sens.CREDIT && detail.getMontantCredit().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Credit amount must be positive");
    }

    /* AUDIT */
    private void logAudit(Tenant tenant, UUID ecritureComptableId, String utilisateur, String action, String details) {
        JournalAudit audit = JournalAudit.builder()
                .tenant(tenant)
                .ecritureComptableId(ecritureComptableId)
                .utilisateur(utilisateur)
                .action(action)
                .details(details)
                .dateAction(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .createdBy(utilisateur)
                .updatedBy(utilisateur)
                .build();

        journalAuditRepository.save(audit);
        kafkaTemplate.send("journal.audit.created", tenant.getId().toString(), audit);
    }

    /* SECURITY */
    private void validateTenantAccess() {
        if (TenantContext.getCurrentTenant() == null)
            throw new SecurityException("Access denied: Tenant ID not defined");
    }
}
