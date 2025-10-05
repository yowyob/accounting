package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.JournalComptableDto;
import com.yowyob.erp.accounting.dto.EcritureComptableDto;
import com.yowyob.erp.accounting.entity.JournalAudit;
import com.yowyob.erp.accounting.entity.JournalComptable;
import com.yowyob.erp.accounting.entity.EcritureComptable;
import com.yowyob.erp.accounting.entityKey.JournalAuditKey;
import com.yowyob.erp.accounting.entityKey.JournalComptableKey;
import com.yowyob.erp.accounting.repository.JournalAuditRepository;
import com.yowyob.erp.accounting.repository.JournalComptableRepository;
import com.yowyob.erp.accounting.repository.EcritureComptableRepository;
import com.yowyob.erp.common.constants.AppConstants;
import com.yowyob.erp.config.tenant.TenantContext;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JournalComptableService {

    private static final Logger logger = LoggerFactory.getLogger(JournalComptableService.class);
    private final JournalComptableRepository journalComptableRepository;
    private final EcritureComptableRepository ecritureComptableRepository;
    private final JournalAuditRepository journalAuditRepository;
    private final Validator validator;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public JournalComptableService(
        JournalComptableRepository journalComptableRepository, 
       JournalAuditRepository journalAuditRepository, 
       EcritureComptableRepository ecritureComptableRepository,
       Validator validator, 
       KafkaTemplate<String, Object> kafkaTemplate) {
        this.journalComptableRepository = journalComptableRepository;
        this.journalAuditRepository = journalAuditRepository;
        this.ecritureComptableRepository = ecritureComptableRepository;
        this.validator = validator;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public JournalComptableDto createJournalComptable(JournalComptableDto journalComptableDto) {
        logger.info("Création d'un journal comptable");
        validerJournalComptableDto(journalComptableDto);
        UUID tenantId = TenantContext.getCurrentTenant();
        String currentUser = TenantContext.getCurrentUser();

        if (journalComptableRepository.existsByKeyTenantIdAndCodeJournal(tenantId, journalComptableDto.getCodeJournal())) {
            throw new IllegalArgumentException("Code journal déjà utilisé : " + journalComptableDto.getCodeJournal());
        }

        // Map DTO to entity
        JournalComptable journalComptable = mapToEntity(journalComptableDto, tenantId);
        JournalComptableKey key = new JournalComptableKey();
        key.setTenantId(tenantId);
        key.setId(UUID.randomUUID());
        journalComptable.setKey(key);
        journalComptable.setCreatedAt(LocalDateTime.now());
        journalComptable.setUpdatedAt(LocalDateTime.now());
        journalComptable.setCreatedBy(currentUser != null ? currentUser : "system");
        journalComptable.setUpdatedBy(currentUser != null ? currentUser : "system");

        JournalComptable savedJournalComptable = journalComptableRepository.save(journalComptable);
        logAudit(tenantId, null, currentUser, "CREATE", "Created journal: " + journalComptableDto.getCodeJournal());
        kafkaTemplate.send("journal.comptable.created", tenantId.toString(), savedJournalComptable);
        logger.info("Journal comptable créé avec succès : {}", savedJournalComptable.getKey().getId());
        return mapToDto(savedJournalComptable);
    }

    public JournalComptableDto getJournalComptable(UUID journalComptableId) {
        logger.info("Récupération du journal comptable avec l'ID : {}", journalComptableId);
        validerAccesTenantId();
        UUID tenantId = TenantContext.getCurrentTenant();
        return journalComptableRepository.findByKeyTenantIdAndKeyId(tenantId, journalComptableId)
                .map(journal -> {
                    JournalComptableDto dto = mapToDto(journal);
                    // Fetch and map all associated EcritureComptable entries
                    List<EcritureComptableDto> ecritures = ecritureComptableRepository.findByKeyTenantIdAndJournalComptableId(tenantId, journalComptableId)
                            .stream()
                            .map(this::mapEcritureToDto) 
                            .collect(Collectors.toList());
                    dto.setEcritureComptable(ecritures);
                    return dto;
                });
    }
        

        

    public List<JournalComptableDto> getAllJournalComptables() {
        logger.info("Récupération de tous les journals comptables pour le tenant");
        validerAccesTenantId();
        return journalComptableRepository.findByKeyTenantId(TenantContext.getCurrentTenant())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<JournalComptableDto> getActiveJournalComptables() {
        logger.info("Récupération des journals comptables actifs pour le tenant");
        validerAccesTenantId();
        return journalComptableRepository.findByKeyTenantIdAndActifTrue(TenantContext.getCurrentTenant())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public JournalComptableDto updateJournalComptable(UUID journalComptableId, JournalComptableDto updatedJournalComptableDto) {
        logger.info("Mise à jour du journal comptable avec l'ID : {}", journalComptableId);
        validerAccesTenantId();
        UUID tenantId = TenantContext.getCurrentTenant();
        String currentUser = TenantContext.getCurrentUser();
        if (!journalComptableRepository.existsByKeyTenantIdAndKeyId(tenantId, journalComptableId)) {
            throw new IllegalArgumentException("L'ID du journal comptable n'existe pas : " + journalComptableId);
        }

        // Map DTO to entity
        JournalComptable updatedJournalComptable = mapToEntity(updatedJournalComptableDto, tenantId);
        updatedJournalComptable.setKey(new JournalComptableKey(tenantId, journalComptableId));
        validerJournalComptableDto(updatedJournalComptableDto);
        updatedJournalComptable.setUpdatedAt(LocalDateTime.now());
        updatedJournalComptable.setUpdatedBy(currentUser != null ? currentUser : "system");

        JournalComptable savedJournalComptable = journalComptableRepository.save(updatedJournalComptable);
        logAudit(tenantId, null, currentUser, "UPDATE", "Updated journal: " + updatedJournalComptableDto.getCodeJournal());
        kafkaTemplate.send("journal.comptable.updated", tenantId.toString(), savedJournalComptable);
        logger.info("Journal comptable mis à jour avec succès : {}", journalComptableId);
        return mapToDto(savedJournalComptable);
    }

    @Transactional
    public void deleteJournalComptable(UUID journalComptableId) {
        logger.info("Suppression du journal comptable avec l'ID : {}", journalComptableId);
        validerAccesTenantId();
        UUID tenantId = TenantContext.getCurrentTenant();
        String currentUser = TenantContext.getCurrentUser();
        JournalComptableKey key = new JournalComptableKey(tenantId, journalComptableId);
        if (!journalComptableRepository.existsByKeyTenantIdAndKeyId(tenantId, journalComptableId)) {
            throw new IllegalArgumentException("L'ID du journal comptable n'existe pas : " + journalComptableId);
        }
        journalComptableRepository.deleteById(key);
        logAudit(tenantId, null, currentUser, "DELETE", "Deleted journal ID: " + journalComptableId);
        kafkaTemplate.send("journal.comptable.deleted", tenantId.toString(), journalComptableId);
        logger.info("Journal comptable supprimé avec succès : {}", journalComptableId);
    }

    private void validerJournalComptableDto(JournalComptableDto journalComptableDto) {
        var violations = validator.validate(journalComptableDto);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        String codeJournal = journalComptableDto.getCodeJournal();
        if (!codeJournal.matches("^[A-Z]{1,5}$")) {
            throw new IllegalArgumentException("Code journal invalide, doit être 1-5 lettres majuscules : " + codeJournal);
        }
        // Validate journal type against AppConstants
        if (!AppConstants.JournalTypes.SALES.equals(journalComptableDto.getTypeJournal()) &&
            !AppConstants.JournalTypes.PURCHASES.equals(journalComptableDto.getTypeJournal()) &&
            !AppConstants.JournalTypes.CASH.equals(journalComptableDto.getTypeJournal()) &&
            !AppConstants.JournalTypes.BANK.equals(journalComptableDto.getTypeJournal()) &&
            !AppConstants.JournalTypes.GENERAL.equals(journalComptableDto.getTypeJournal())) {
            throw new IllegalArgumentException("Type de journal invalide : " + journalComptableDto.getTypeJournal());
        }
    }

    private void validerAccesTenantId() {
        UUID currentTenantId = TenantContext.getCurrentTenant();
        if (currentTenantId == null) {
            throw new SecurityException("Accès refusé : ID du tenant non correspondant");
        }
    }

    private void logAudit(UUID tenantId, UUID ecritureComptableId, String utilisateur, String action, String details) {
        JournalAudit audit = new JournalAudit();
        JournalAuditKey auditKey = new JournalAuditKey();
        auditKey.setTenantId(tenantId);
        auditKey.setId(UUID.randomUUID());
        audit.setKey(auditKey);
        audit.setEcritureComptableId(ecritureComptableId);
        audit.setUtilisateur(utilisateur != null ? utilisateur : "system");
        audit.setAction(action);
        audit.setDetails(details);
        audit.setDateAction(LocalDateTime.now());
        journalAuditRepository.save(audit);
        kafkaTemplate.send("journal.audit.created", tenantId.toString(), audit);
    }

    private JournalComptable mapToEntity(JournalComptableDto dto, UUID tenantId) {
        JournalComptable journal = new JournalComptable();
        journal.setKey(new JournalComptableKey(tenantId, dto.getId() != null ? dto.getId() : UUID.randomUUID()));
        journal.setCodeJournal(dto.getCodeJournal());
        journal.setLibelle(dto.getLibelle());
        journal.setTypeJournal(dto.getTypeJournal());
        journal.setNotes(dto.getNotes());
        journal.setActif(dto.getActif() != null ? dto.getActif() : true);
        journal.setCreatedAt(dto.getCreatedAt() != null ? dto.getCreatedAt() : LocalDateTime.now());
        journal.setUpdatedAt(dto.getUpdatedAt() != null ? dto.getUpdatedAt() : LocalDateTime.now());
        return journal;
    }

    private JournalComptableDto mapToDto(JournalComptable journal) {
        return JournalComptableDto.builder()
                .id(journal.getKey().getId())
                .codeJournal(journal.getCodeJournal())
                .libelle(journal.getLibelle())
                .typeJournal(journal.getTypeJournal())
                .notes(journal.getNotes())
                .actif(journal.getActif())
                .createdAt(journal.getCreatedAt())
                .updatedAt(journal.getUpdatedAt())
                .build();
    }

  
    private EcritureComptableDto mapEcritureToDto(EcritureComptable ecriture) {
      
        return EcritureComptableDto.builder()
                .id(ecriture.getKey().getId())
                .numeroEcriture(ecriture.getNumeroEcriture())
                .libelle(ecriture.getLibelle())
                .dateEcriture(ecriture.getDateEcriture())
                .journalComptableId(ecriture.getJournalComptableId())
                .periodeComptableId(ecriture.getPeriodeComptableId())
                .montantTotalDebit(ecriture.getMontantTotalDebit())
                .montantTotalCredit(ecriture.getMontantTotalCredit())
                .validee(ecriture.getValidee())
                .dateValidation(ecriture.getDateValidation())
                .utilisateurValidation(ecriture.getUtilisateurValidation())
                .referenceExterne(ecriture.getReferenceExterne())
                .notes(ecriture.getNotes())
                .createdAt(ecriture.getCreatedAt())
                .updatedAt(ecriture.getUpdatedAt())
                .build();
    }
}
