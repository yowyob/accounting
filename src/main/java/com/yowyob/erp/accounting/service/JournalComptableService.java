package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.EcritureComptableDto;
import com.yowyob.erp.accounting.dto.JournalComptableDto;
import com.yowyob.erp.accounting.entity.EcritureComptable;
import com.yowyob.erp.accounting.entity.JournalAudit;
import com.yowyob.erp.accounting.entity.JournalComptable;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.accounting.repository.EcritureComptableRepository;
import com.yowyob.erp.accounting.repository.JournalAuditRepository;
import com.yowyob.erp.accounting.repository.JournalComptableRepository;
import com.yowyob.erp.common.constants.AppConstants;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import com.yowyob.erp.config.kafka.KafkaMessageService;
import com.yowyob.erp.config.redis.RedisService;
import com.yowyob.erp.config.tenant.TenantContext;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing accounting journals.
 * Compatible with PostgreSQL + Redis + Kafka + Multi-tenant.
 *
 * @author ALD
 * @date 12/10/2025 06:04 PM WAT
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JournalComptableService {

    private final JournalComptableRepository journalComptableRepository;
    private final EcritureComptableRepository ecritureComptableRepository;
    private final JournalAuditRepository journalAuditRepository;
    private final Validator validator;
    private final KafkaMessageService kafkaMessageService;
    private final RedisService redisService;

    private static final String CACHE_JOURNAL_ALL = "journal:all:";
    private static final String CACHE_JOURNAL_ACTIVE = "journal:active:";

    /* -------------------------------------------------------------------------
     *  CREATION
     * ---------------------------------------------------------------------- */
    @Transactional
    public JournalComptableDto createJournalComptable(JournalComptableDto dto) {
        UUID tenantId = TenantContext.getCurrentTenant();
        String user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");
        log.info("📓 Creating accounting journal [{}] for tenant {}", dto.getCodeJournal(), tenantId);

        validateJournalComptableDto(dto);

        if (journalComptableRepository.existsByTenant_IdAndCodeJournal(tenantId, dto.getCodeJournal())) {
            throw new IllegalArgumentException("Journal code already in use: " + dto.getCodeJournal());
        }

        Tenant tenant = TenantContext.getCurrentTenantAsTenant();
        JournalComptable entity = mapToEntity(dto, tenant);
        entity.setId(UUID.randomUUID());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setCreatedBy(user);
        entity.setUpdatedBy(user);

        JournalComptable saved = journalComptableRepository.save(entity);
        logAudit(tenant, user, "CREATE", "Creation of journal " + dto.getCodeJournal());
        kafkaMessageService.sendAuditLog(saved, tenantId.toString(), "JOURNAL_CREATED");

        redisService.delete(CACHE_JOURNAL_ALL + tenantId);
        redisService.delete(CACHE_JOURNAL_ACTIVE + tenantId);

        return mapToDto(saved);
    }

    /* -------------------------------------------------------------------------
     *  READ
     * ---------------------------------------------------------------------- */
    public Optional<JournalComptableDto> getJournalComptable(UUID journalId) {
        UUID tenantId = TenantContext.getCurrentTenant();
        log.info("🔍 Retrieving accounting journal [{}] for tenant {}", journalId, tenantId);

        return journalComptableRepository.findByTenant_IdAndId(tenantId, journalId)
                .map(journal -> {
                    JournalComptableDto dto = mapToDto(journal);
                    List<EcritureComptableDto> ecritures = ecritureComptableRepository
                            .findByTenant_IdAndJournal_Id(tenantId, journalId)
                            .stream()
                            .map(this::mapEcritureToDto)
                            .collect(Collectors.toList());
                    dto.setEcritureComptable(ecritures);
                    return dto;
                });
    }

    public List<JournalComptableDto> getAllJournalComptables() {
        UUID tenantId = TenantContext.getCurrentTenant();
        log.info("📚 Retrieving all journals for tenant {}", tenantId);

        String cacheKey = CACHE_JOURNAL_ALL + tenantId;
        List<JournalComptableDto> cached = redisService.get(cacheKey, List.class);
        if (cached != null) return cached;

        List<JournalComptableDto> journals = journalComptableRepository.findByTenant_Id(tenantId)
                .stream().map(this::mapToDto).toList();

        redisService.save(cacheKey, journals, Duration.ofMinutes(10));
        return journals;
    }

    public List<JournalComptableDto> getActiveJournalComptables() {
        UUID tenantId = TenantContext.getCurrentTenant();
        log.info("📘 Retrieving active journals for tenant {}", tenantId);

        String cacheKey = CACHE_JOURNAL_ACTIVE + tenantId;
        List<JournalComptableDto> cached = redisService.get(cacheKey, List.class);
        if (cached != null) return cached;

        List<JournalComptableDto> journals = journalComptableRepository.findByTenant_IdAndActifTrue(tenantId)
                .stream().map(this::mapToDto).toList();

        redisService.save(cacheKey, journals, Duration.ofMinutes(10));
        return journals;
    }

    /* -------------------------------------------------------------------------
     *  UPDATE
     * ---------------------------------------------------------------------- */
    @Transactional
    public JournalComptableDto updateJournalComptable(UUID id, JournalComptableDto dto) {
        UUID tenantId = TenantContext.getCurrentTenant();
        String user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        JournalComptable existing = journalComptableRepository.findByTenant_IdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("JournalComptable", id.toString()));

        validateJournalComptableDto(dto);

        existing.setCodeJournal(dto.getCodeJournal());
        existing.setLibelle(dto.getLibelle());
        existing.setTypeJournal(dto.getTypeJournal());
        existing.setNotes(dto.getNotes());
        existing.setActif(dto.getActif());
        existing.setUpdatedBy(user);
        existing.setUpdatedAt(LocalDateTime.now());

        JournalComptable saved = journalComptableRepository.save(existing);
        logAudit(TenantContext.getCurrentTenantAsTenant(), user, "UPDATE", "Update of journal " + dto.getCodeJournal());
        kafkaMessageService.sendAuditLog(saved, tenantId.toString(), "JOURNAL_UPDATED");

        redisService.delete(CACHE_JOURNAL_ALL + tenantId);
        redisService.delete(CACHE_JOURNAL_ACTIVE + tenantId);

        return mapToDto(saved);
    }

    /* -------------------------------------------------------------------------
     *  DELETION
     * ---------------------------------------------------------------------- */
    @Transactional
    public void deleteJournalComptable(UUID id) {
        UUID tenantId = TenantContext.getCurrentTenant();
        String user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        JournalComptable journal = journalComptableRepository.findByTenant_IdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("JournalComptable", id.toString()));

        journalComptableRepository.delete(journal);
        logAudit(TenantContext.getCurrentTenantAsTenant(), user, "DELETE", "Deletion of journal " + journal.getCodeJournal());
        kafkaMessageService.sendAuditLog(journal, tenantId.toString(), "JOURNAL_DELETED");

        redisService.delete(CACHE_JOURNAL_ALL + tenantId);
        redisService.delete(CACHE_JOURNAL_ACTIVE + tenantId);
    }

    /* -------------------------------------------------------------------------
     *  VALIDATION
     * ---------------------------------------------------------------------- */
    private void validateJournalComptableDto(JournalComptableDto dto) {
        var violations = validator.validate(dto);
        if (!violations.isEmpty()) throw new ConstraintViolationException(violations);

        if (!dto.getCodeJournal().matches("^[A-Z]{1,5}$")) {
            throw new IllegalArgumentException("Invalid journal code: must contain 1 to 5 uppercase letters.");
        }

        List<String> validTypes = List.of(
                AppConstants.JournalTypes.SALES,
                AppConstants.JournalTypes.PURCHASES,
                AppConstants.JournalTypes.CASH,
                AppConstants.JournalTypes.BANK,
                AppConstants.JournalTypes.GENERAL
        );

        if (!validTypes.contains(dto.getTypeJournal())) {
            throw new IllegalArgumentException("Invalid journal type: " + dto.getTypeJournal());
        }
    }

    /* -------------------------------------------------------------------------
     *  LOG + MAPPING
     * ---------------------------------------------------------------------- */
    private void logAudit(Tenant tenant, String utilisateur, String action, String details) {
        JournalAudit audit = new JournalAudit();
        audit.setId(UUID.randomUUID());
        audit.setTenant(tenant);
        audit.setAction(action);
        audit.setUtilisateur(utilisateur);
        audit.setDetails(details);
        audit.setDateAction(LocalDateTime.now());
        journalAuditRepository.save(audit);
        kafkaMessageService.sendAuditLog(audit, tenant.getId().toString(), action);
    }

    private JournalComptable mapToEntity(JournalComptableDto dto, Tenant tenant) {
        JournalComptable j = new JournalComptable();
        j.setTenant(tenant);
        j.setCodeJournal(dto.getCodeJournal());
        j.setLibelle(dto.getLibelle());
        j.setTypeJournal(dto.getTypeJournal());
        j.setNotes(dto.getNotes());
        j.setActif(dto.getActif() != null ? dto.getActif() : true);
        j.setCreatedAt(dto.getCreatedAt() != null ? dto.getCreatedAt() : LocalDateTime.now());
        j.setUpdatedAt(dto.getUpdatedAt() != null ? dto.getUpdatedAt() : LocalDateTime.now());
        return j;
    }

    private JournalComptableDto mapToDto(JournalComptable entity) {
        return JournalComptableDto.builder()
                .id(entity.getId())
                .codeJournal(entity.getCodeJournal())
                .libelle(entity.getLibelle())
                .typeJournal(entity.getTypeJournal())
                .notes(entity.getNotes())
                .actif(entity.getActif())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private EcritureComptableDto mapEcritureToDto(EcritureComptable e) {
        return EcritureComptableDto.builder()
                .id(e.getId())
                .numeroEcriture(e.getNumeroEcriture())
                .libelle(e.getLibelle())
                .dateEcriture(e.getDateEcriture())
                .journalComptableId(e.getJournal() != null ? e.getJournal().getId() : null)
                .periodeComptableId(e.getPeriode().getId())
                .montantTotalDebit(e.getMontantTotalDebit())
                .montantTotalCredit(e.getMontantTotalCredit())
                .validee(e.getValidee())
                .dateValidation(e.getDateValidation())
                .validatedBy(e.getValidatedBy()) // Corrected from utilisateurValidation
                .referenceExterne(e.getReferenceExterne())
                .notes(e.getNotes())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
