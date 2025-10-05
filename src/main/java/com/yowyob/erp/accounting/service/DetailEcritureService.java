package com.yowyob.erp.accounting.service;

import com.yowyob.erp.config.tenant.TenantContext;
import com.yowyob.erp.accounting.entity.DetailEcriture;
import com.yowyob.erp.accounting.entity.JournalAudit;
import com.yowyob.erp.accounting.entity.PlanComptable;
import com.yowyob.erp.accounting.repository.DetailEcritureRepository;
import com.yowyob.erp.accounting.repository.JournalAuditRepository;
import com.yowyob.erp.accounting.repository.PlanComptableRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service gérant la création, la mise à jour et la suppression des
 * détails d'écriture comptable. Compatible PostgreSQL + JPA.
 */
@Service
public class DetailEcritureService {

    private static final Logger logger = LoggerFactory.getLogger(DetailEcritureService.class);

    private final DetailEcritureRepository detailRepository;
    private final PlanComptableRepository planComptableRepository;
    private final JournalAuditRepository journalAuditRepository;
    private final Validator validator;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public DetailEcritureService(
            DetailEcritureRepository detailRepository,
            PlanComptableRepository planComptableRepository,
            JournalAuditRepository journalAuditRepository,
            Validator validator,
            KafkaTemplate<String, Object> kafkaTemplate
    ) {
        this.detailRepository = detailRepository;
        this.planComptableRepository = planComptableRepository;
        this.journalAuditRepository = journalAuditRepository;
        this.validator = validator;
        this.kafkaTemplate = kafkaTemplate;
    }

    /* -------------------------------------------------------------------------- */
    /*                              CRÉATION D'UNE LIGNE D'ÉCRITURE                        */
    /* -------------------------------------------------------------------------- */
    @Transactional
    public DetailEcriture createDetailEcriture(DetailEcriture detail) {
        UUID tenantId = TenantContext.getCurrentTenant();
        String currentUser = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        validerDetailEcriture(detail);

        detail.setTenantId(tenantId);
        detail.setDateEcriture(LocalDateTime.now());
        detail.setCreatedAt(LocalDateTime.now());
        detail.setUpdatedAt(LocalDateTime.now());
        detail.setCreatedBy(currentUser);
        detail.setUpdatedBy(currentUser);

        // Sens → met à zéro le champ opposé
        if ("DEBIT".equalsIgnoreCase(detail.getSens())) detail.setMontantCredit(0.0);
        if ("CREDIT".equalsIgnoreCase(detail.getSens())) detail.setMontantDebit(0.0);

        DetailEcriture saved = detailRepository.save(detail);
        logAudit(tenantId, saved.getEcritureComptableId(), currentUser, "CREATE",
                "Création d’un détail d’écriture " + saved.getId());
        kafkaTemplate.send("detail.ecriture.created", tenantId.toString(), saved);

        logger.info("✅ Détail d’écriture créé avec succès : {}", saved.getId());
        return saved;
    }

    /* -------------------------------------------------------------------------- */
    /*                                  LECTURE                                   */
    /* -------------------------------------------------------------------------- */
    public Optional<DetailEcriture> getDetailEcriture(Long id, UUID tenantId) {
        validerAccesTenantId();
        return detailRepository.findById(id)
                .filter(d -> d.getTenantId().equals(tenantId));
    }

    public List<DetailEcriture> getAllDetailsEcriture(UUID tenantId) {
        validerAccesTenantId();
        return detailRepository.findByTenantId(tenantId);
    }

    public List<DetailEcriture> getDetailsByEcriture(UUID tenantId, Long ecritureComptableId) {
        validerAccesTenantId();
        return detailRepository.findByTenantIdAndEcritureComptableId(tenantId, ecritureComptableId);
    }

    /* -------------------------------------------------------------------------- */
    /*                                 MISE À JOUR                                */
    /* -------------------------------------------------------------------------- */
    @Transactional
    public DetailEcriture updateDetailEcriture(Long id, DetailEcriture updatedDetail) {
        UUID tenantId = TenantContext.getCurrentTenant();
        String currentUser = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");
        validerAccesTenantId();

        DetailEcriture existing = detailRepository.findById(id)
                .filter(d -> d.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Détail d’écriture introuvable : " + id));

        validerDetailEcriture(updatedDetail);

        existing.setLibelle(updatedDetail.getLibelle());
        existing.setSens(updatedDetail.getSens());
        existing.setMontantDebit(updatedDetail.getMontantDebit());
        existing.setMontantCredit(updatedDetail.getMontantCredit());
        existing.setNotes(updatedDetail.getNotes());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(currentUser);

        if ("DEBIT".equalsIgnoreCase(existing.getSens())) existing.setMontantCredit(0.0);
        if ("CREDIT".equalsIgnoreCase(existing.getSens())) existing.setMontantDebit(0.0);

        DetailEcriture saved = detailRepository.save(existing);
        logAudit(tenantId, saved.getEcritureComptableId(), currentUser, "UPDATE",
                "Mise à jour du détail d’écriture " + saved.getId());
        kafkaTemplate.send("detail.ecriture.updated", tenantId.toString(), saved);

        logger.info("✏️ Détail d’écriture mis à jour : {}", saved.getId());
        return saved;
    }

    /* -------------------------------------------------------------------------- */
    /*                                 SUPPRESSION                                */
    /* -------------------------------------------------------------------------- */
    @Transactional
    public void deleteDetailEcriture(Long id, UUID tenantId) {
        validerAccesTenantId();
        String currentUser = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        DetailEcriture detail = detailRepository.findById(id)
                .filter(d -> d.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Détail d’écriture introuvable : " + id));

        detailRepository.delete(detail);
        logAudit(tenantId, detail.getEcritureComptableId(), currentUser, "DELETE",
                "Suppression du détail d’écriture " + id);
        kafkaTemplate.send("detail.ecriture.deleted", tenantId.toString(), id);

        logger.info("🗑️ Détail d’écriture supprimé : {}", id);
    }

    /* -------------------------------------------------------------------------- */
    /*                                 VALIDATION                                 */
    /* -------------------------------------------------------------------------- */
    private void validerDetailEcriture(DetailEcriture detail) {
        var violations = validator.validate(detail);
        if (!violations.isEmpty()) throw new ConstraintViolationException(violations);

        UUID tenantId = TenantContext.getCurrentTenant();
        PlanComptable plan = planComptableRepository
                .findById(detail.getCompteComptableId())
                .filter(p -> p.getTenantId().equals(tenantId))
                .orElseThrow(() ->
                        new IllegalArgumentException("Compte comptable introuvable : " + detail.getCompteComptableId()));

        if (!Boolean.TRUE.equals(plan.getActif()))
            throw new IllegalArgumentException("Compte inactif : " + plan.getNoCompte());

        if ("DEBIT".equalsIgnoreCase(detail.getSens()) && detail.getMontantDebit() <= 0)
            throw new IllegalArgumentException("Montant débit doit être positif");

        if ("CREDIT".equalsIgnoreCase(detail.getSens()) && detail.getMontantCredit() <= 0)
            throw new IllegalArgumentException("Montant crédit doit être positif");
    }

    /* -------------------------------------------------------------------------- */
    /*                                   AUDIT                                    */
    /* -------------------------------------------------------------------------- */
    private void logAudit(UUID tenantId, Long ecritureComptableId, String utilisateur, String action, String details) {
        JournalAudit audit = JournalAudit.builder()
                .tenantId(tenantId)
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
        kafkaTemplate.send("journal.audit.created", tenantId.toString(), audit);
    }

    /* -------------------------------------------------------------------------- */
    /*                                   SECURITE                                 */
    /* -------------------------------------------------------------------------- */
    private void validerAccesTenantId() {
        if (TenantContext.getCurrentTenant() == null)
            throw new SecurityException("Accès refusé : ID du tenant non défini");
    }
}
