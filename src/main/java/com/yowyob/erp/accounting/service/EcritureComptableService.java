package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.*;
import com.yowyob.erp.accounting.entity.*;
import com.yowyob.erp.accounting.repository.*;
import com.yowyob.erp.common.entity.ComptableObject;
import com.yowyob.erp.common.exception.BusinessException;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import com.yowyob.erp.config.kafka.KafkaMessageService;
import com.yowyob.erp.config.redis.RedisService;
import com.yowyob.erp.config.tenant.TenantContext;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing accounting entries.
 * Compatible with PostgreSQL + Redis + Kafka + Multi-tenant.
 *
 * @author ALD
 * @date 12/10/2025 06:39 AM WAT
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EcritureComptableService {

    private static final String CACHE_ALL = "ecritures:all:";
    private static final String CACHE_NON_VALIDATED = "ecritures:nonvalidated:";
    private static final String CACHE_SEARCH = "ecritures:search:";

    private final EcritureComptableRepository ecritureRepository;
    private final OperationComptableRepository operationRepository;
    private final DetailEcritureRepository detailRepository;
    @SuppressWarnings("unused")
    private final TransactionRepository transactionRepository;
    private final JournalComptableRepository journalRepository;
    private final DetailEcritureService detailService;
    private final PlanComptableRepository planRepository;
    private final PeriodeComptableRepository periodeRepository;
    private final JournalComptableService journalService;
    private final PeriodeComptableService periodeService;
    private final JournalAuditRepository auditRepository;
    private final Validator validator;
    private final KafkaMessageService kafkaMessageService;
    private final KafkaOperations<String, Object> kafkaOperations;
    private final RedisService redisService;

    /* ============================================================================
       CREATION OF AN ACCOUNTING ENTRY
    ============================================================================ */
    @Transactional
    public EcritureComptableDto createEcriture(EcritureComptableDto dto) {
        UUID tenantId = TenantContext.getCurrentTenant();
        String currentUser = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");
        log.info("➡️ Creating accounting entry for tenant: {}", tenantId);

        validator.validate(dto);

        // Verify journal & period
        journalService.getJournalComptable(dto.getJournalComptableId());
        PeriodeComptableDto periode = periodeService.getPeriode(dto.getPeriodeComptableId())
                .filter(p -> !p.getCloturee())
                .orElseThrow(() -> new BusinessException("Accounting period is closed"));

        // Create entity
        EcritureComptable ecriture = mapToEntity(dto, TenantContext.getCurrentTenantAsTenant());
        ecriture.setNumeroEcriture("ECR-" + ecriture.getId());
        ecriture.setCreatedAt(LocalDateTime.now());
        ecriture.setUpdatedAt(LocalDateTime.now());
        ecriture.setCreatedBy(currentUser);
        ecriture.setUpdatedBy(currentUser);
        ecriture.setValidee(false);

        EcritureComptable saved = ecritureRepository.save(ecriture);
        log.info("✅ Entry {} created", saved.getNumeroEcriture());

        // Validate balance
        List<DetailEcriture> details = detailRepository.findByTenant_IdAndEcriture_Id(tenantId, saved.getId());
        validateBalance(details);

        // Audit + Kafka + Cache
        logAuditAndSendKafka(TenantContext.getCurrentTenantAsTenant(), saved.getId(), currentUser, "CREATE", "Entry created");
        redisService.delete(CACHE_ALL + tenantId);

        return mapToDto(saved);
    }

    /* ============================================================================
       VALIDATION
    ============================================================================ */
    @Transactional
    public EcritureComptableDto validateEcriture(UUID id, String user) {
        UUID tenantId = TenantContext.getCurrentTenant();
        String currentUser = Optional.ofNullable(user).orElse("system");

        EcritureComptable ecriture = ecritureRepository
                .findByTenant_IdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Entry", id.toString()));

        if (Boolean.TRUE.equals(ecriture.getValidee()))
            throw new BusinessException("Entry already validated");

        List<DetailEcriture> details = detailRepository.findByTenant_IdAndEcriture_Id(tenantId, id);
        validateBalance(details);

        ecriture.setValidee(true);
        ecriture.setDateValidation(LocalDateTime.now());
        ecriture.setValidatedBy(currentUser);
        ecriture.setUpdatedAt(LocalDateTime.now());
        ecriture.setUpdatedBy(currentUser);

        EcritureComptable validated = ecritureRepository.save(ecriture);
        logAuditAndSendKafka(TenantContext.getCurrentTenantAsTenant(), id, currentUser, "VALIDATE", "Entry validated");
        redisService.delete(CACHE_NON_VALIDATED + tenantId);

        return mapToDto(validated);
    }

    /* ============================================================================
       SEARCHES & CACHE
    ============================================================================ */
    public List<EcritureComptableDto> getAll() {
        UUID tenantId = TenantContext.getCurrentTenant();
        String key = CACHE_ALL + tenantId;
        List<EcritureComptableDto> cached = redisService.get(key, List.class);
        if (cached != null) return cached;

        List<EcritureComptableDto> list = ecritureRepository.findByTenant_Id(tenantId)
                .stream().map(this::mapToDto).collect(Collectors.toList());
        redisService.save(key, list, Duration.ofMinutes(10));
        return list;
    }

    public List<EcritureComptableDto> getNonValidated() {
        UUID tenantId = TenantContext.getCurrentTenant();
        String key = CACHE_NON_VALIDATED + tenantId;
        List<EcritureComptableDto> cached = redisService.get(key, List.class);
        if (cached != null) return cached;

        List<EcritureComptableDto> list = ecritureRepository.findByTenant_IdAndValideeFalse(tenantId)
                .stream().map(this::mapToDto).collect(Collectors.toList());
        redisService.save(key, list, Duration.ofMinutes(10));
        return list;
    }

    public Optional<EcritureComptableDto> getById(UUID id) {
        UUID tenantId = TenantContext.getCurrentTenant();
        return ecritureRepository.findByTenant_IdAndId(tenantId, id)
                .map(this::mapToDto);
    }

    /* ============================================================================
       🔍 SEARCH BY DATE RANGE AND JOURNAL
    ============================================================================ */
    public List<EcritureComptableDto> searchEcritures(LocalDateTime startDate, LocalDateTime endDate, UUID journalId) {
        UUID tenantId = TenantContext.getCurrentTenant();
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException("Start date must be before end date");
        }
        String cacheKey = CACHE_SEARCH + tenantId + ":" + (startDate != null ? startDate : "all")
                + ":" + (endDate != null ? endDate : "all")
                + ":" + (journalId != null ? journalId : "all");

        List<EcritureComptableDto> cached = redisService.get(cacheKey, List.class);
        if (cached != null) return cached;

        List<EcritureComptable> results;

        if (startDate != null && endDate != null && journalId != null) {
            results = ecritureRepository.findByTenant_IdAndJournal_IdAndDateEcritureBetween(
                    tenantId, journalId, startDate.toLocalDate(), endDate.toLocalDate());
        } else if (startDate != null && endDate != null) {
            results = ecritureRepository.findByTenant_IdAndDateEcritureBetween(
                    tenantId, startDate.toLocalDate(), endDate.toLocalDate());
        } else if (journalId != null) {
            results = ecritureRepository.findByTenant_IdAndJournal_Id(tenantId, journalId);
        } else {
            results = ecritureRepository.findByTenant_Id(tenantId);
        }

        List<EcritureComptableDto> list = results.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        redisService.save(cacheKey, list, Duration.ofMinutes(5));
        return list;
    }

    /* ============================================================================
       ⚙️ GENERATION FROM ACCOUNTING OBJECT
    ============================================================================ */
    @Transactional
    public EcritureComptableDto generateFromComptableObject(ComptableObject object) {
        UUID tenantId = object.getTenantId();
        Tenant tenant = TenantContext.getCurrentTenantAsTenant();
        String currentUser = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        if (object.getMontant() == null || object.getMontant().doubleValue() <= 0)
            throw new BusinessException("Invalid amount for entry generation");

        // Verify journal and active period
        journalService.getJournalComptable(object.getJournalComptableId());
        PeriodeComptableDto currentPeriode = periodeService.getCurrentPeriode(tenantId);

        EcritureComptable ecriture = EcritureComptable.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .numeroEcriture("ECR-" + UUID.randomUUID().toString().substring(0, 8))
                .libelle(object.getDescription() != null ? object.getDescription() : "Auto-generated entry")
                .journal(journalRepository.findById(object.getJournalComptableId())
                        .orElseThrow(() -> new ResourceNotFoundException("Journal", object.getJournalComptableId().toString())))
                .periode(periodeRepository.findById(object.getPeriodeComptableId())
                        .orElseThrow(() -> new ResourceNotFoundException("Period", currentPeriode.getId().toString())))
                .montantTotalDebit(object.getMontant())
                .montantTotalCredit(object.getMontant())
                .validee(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();

        EcritureComptable saved = ecritureRepository.save(ecriture);
        log.info("🧾 Entry generated automatically from object {}", object.getSourceType());
 
        // Generate details (debit/credit)    +
        detailService.generateDetailsFromComptableObject(saved, object);

        validateBalance(detailRepository.findByTenant_IdAndEcriture_Id(tenantId, saved.getId()));

        logAuditAndSendKafka(tenant, saved.getId(), currentUser, "AUTO_GENERATE", "Entry generated automatically");
        redisService.delete(CACHE_ALL + tenantId);

        return mapToDto(saved);
    }

    /* ============================================================================
       DELETION
    ============================================================================ */
    @Transactional
    public void deleteEcriture(UUID id) {
        UUID tenantId = TenantContext.getCurrentTenant();
        EcritureComptable ecriture = ecritureRepository.findByTenant_IdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Entry", id.toString()));

        if (Boolean.TRUE.equals(ecriture.getValidee()))
            throw new BusinessException("Cannot delete a validated entry");

        ecritureRepository.delete(ecriture);
        redisService.delete(CACHE_ALL + tenantId);
        log.info("🗑️ Entry {} deleted", id);
    }

    /* ============================================================================
       HELPERS
    ============================================================================ */
    private void logAuditAndSendKafka(Tenant tenant, UUID ecritureId, String user, String action, String details) {
        kafkaOperations.executeInTransaction(ops -> {
            JournalAudit audit = JournalAudit.builder()
                    .tenant(tenant)
                    .ecritureComptableId(ecritureId)
                    .utilisateur(user)
                    .action(action)
                    .details(details)
                    .dateAction(LocalDateTime.now())
                    .build();
            auditRepository.save(audit);
            kafkaMessageService.sendAuditLog(audit, tenant.getId().toString(), action);
            return null;
        });
    }

   private void validateBalance(List<DetailEcriture> details) {
    // Tolerance of 0.01 to handle rounding issues
    BigDecimal debit = details.stream()
            .map(DetailEcriture::getMontantDebit)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal credit = details.stream()
            .map(DetailEcriture::getMontantCredit)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (debit.subtract(credit).abs().compareTo(BigDecimal.valueOf(0.01)) > 0) {
        throw new BusinessException("Unbalanced entry: debit=" + debit + " credit=" + credit);
    }
}

    private EcritureComptable mapToEntity(EcritureComptableDto dto, Tenant tenant) {
        return EcritureComptable.builder()
                .id(dto.getId())
                .tenant(tenant)
                .numeroEcriture(dto.getNumeroEcriture())
                .libelle(dto.getLibelle())
                .dateEcriture(dto.getDateEcriture())
                .journal(journalRepository.findById(dto.getJournalComptableId())
                        .orElseThrow(() -> new ResourceNotFoundException("Journal", dto.getJournalComptableId().toString())))
                .periode(periodeRepository.findById(dto.getPeriodeComptableId())
                        .orElseThrow(() -> new ResourceNotFoundException("Period", dto.getPeriodeComptableId().toString())))             
               .montantTotalDebit(dto.getMontantTotalDebit())
                .montantTotalCredit(dto.getMontantTotalCredit())
                .validee(dto.getValidee())
                .referenceExterne(dto.getReferenceExterne())
                .notes(dto.getNotes())
                .build();
    }

    private EcritureComptableDto mapToDto(EcritureComptable e) {
        return EcritureComptableDto.builder()
                .id(e.getId())
                .numeroEcriture(e.getNumeroEcriture())
                .libelle(e.getLibelle())
                .dateEcriture(e.getDateEcriture())
                .journalComptableId(e.getJournal().getId())
                .periodeComptableId(e.getPeriode().getId())
                .montantTotalDebit(e.getMontantTotalDebit())
                .montantTotalCredit(e.getMontantTotalCredit())
                .validee(e.getValidee())
                .referenceExterne(e.getReferenceExterne())
                .notes(e.getNotes())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
