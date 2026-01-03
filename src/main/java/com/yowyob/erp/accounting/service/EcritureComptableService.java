package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.EcritureComptableDto;
import com.yowyob.erp.accounting.dto.PeriodeComptableDto;
import com.yowyob.erp.accounting.entity.DetailEcriture;
import com.yowyob.erp.accounting.entity.EcritureComptable;
import com.yowyob.erp.accounting.entity.JournalAudit;
import com.yowyob.erp.accounting.entity.Tenant;
import com.yowyob.erp.accounting.repository.EcritureComptableRepository;
import com.yowyob.erp.accounting.repository.OperationComptableRepository;
import com.yowyob.erp.accounting.repository.DetailEcritureRepository;
import com.yowyob.erp.accounting.repository.TransactionRepository;
import com.yowyob.erp.accounting.repository.JournalComptableRepository;
import com.yowyob.erp.accounting.repository.PlanComptableRepository;
import com.yowyob.erp.accounting.repository.PeriodeComptableRepository;
import com.yowyob.erp.accounting.repository.JournalAuditRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing accounting entries.
 * Compatible with PostgreSQL + Redis + Kafka + Multi-tenant.
 * 
 * @author ALD
 * @date 30.09.25
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
    private final TransactionRepository transaction_repository;
    private final JournalComptableRepository journal_repository;
    private final DetailEcritureService detail_service;
    private final PeriodeComptableRepository periode_repository;
    private final JournalComptableService journal_service;
    private final PeriodeComptableService periode_service;
    private final JournalAuditRepository audit_repository;
    private final Validator validator_ref;
    private final KafkaMessageService kafka_message_service;
    private final KafkaOperations<String, Object> kafka_operations;
    private final RedisService redis_service;

    /**
     * Creates an accounting entry.
     * 
     * @param dto the entry data transfer object
     * @return the created entry DTO
     */
    @Transactional
    public EcritureComptableDto createEcriture(EcritureComptableDto dto) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        String current_user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");
        log.info("➡️ Creating accounting entry for tenant: {}", tenant_id);

        validator_ref.validate(dto);

        // Verify journal & period
        journal_service.getJournalComptable(dto.getJournal_comptable_id());
        PeriodeComptableDto periode_dto = periode_service.getPeriode(dto.getPeriode_comptable_id())
                .filter(p -> !p.getCloturee())
                .orElseThrow(() -> new BusinessException("Accounting period is closed"));

        // Create entity
        EcritureComptable ecriture = mapToEntity(dto, TenantContext.getCurrentTenantAsTenant());
        ecriture.setNumero_ecriture("ECR-" + ecriture.getId());
        ecriture.setCreated_at(LocalDateTime.now());
        ecriture.setUpdated_at(LocalDateTime.now());
        ecriture.setCreated_by(current_user);
        ecriture.setUpdated_by(current_user);
        ecriture.setValidee(false);

        EcritureComptable saved = ecriture_repository.save(ecriture);
        log.info("✅ Entry {} created", saved.getNumero_ecriture());

        // Validate balance
        List<DetailEcriture> details = detail_repository.findByTenant_IdAndEcriture_Id(tenant_id, saved.getId());
        validateBalance(details);

        // Audit + Kafka + Cache
        logAuditAndSendKafka(TenantContext.getCurrentTenantAsTenant(), saved.getId(), current_user, "CREATE",
                "Entry created");
        redis_service.delete(CACHE_ALL + tenant_id);

        return mapToDto(saved);
    }

    /**
     * Validates an accounting entry.
     * 
     * @param id   the id of the entry to validate
     * @param user the user performing the validation
     * @return the validated entry DTO
     */
    @Transactional
    public EcritureComptableDto validateEcriture(UUID id, String user) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        String current_user = Optional.ofNullable(user).orElse("system");

        EcritureComptable ecriture = ecriture_repository
                .findByTenant_IdAndId(tenant_id, id)
                .orElseThrow(() -> new ResourceNotFoundException("Entry", id.toString()));

        if (Boolean.TRUE.equals(ecriture.getValidee()))
            throw new BusinessException("Entry already validated");

        List<DetailEcriture> details = detail_repository.findByTenant_IdAndEcriture_Id(tenant_id, id);
        validateBalance(details);

        ecriture.setValidee(true);
        ecriture.setDate_validation(LocalDateTime.now());
        ecriture.setValidated_by(current_user);
        ecriture.setUpdated_at(LocalDateTime.now());
        ecriture.setUpdated_by(current_user);

        EcritureComptable validated = ecriture_repository.save(ecriture);
        logAuditAndSendKafka(TenantContext.getCurrentTenantAsTenant(), id, current_user, "VALIDATE", "Entry validated");
        redis_service.delete(CACHE_NON_VALIDATED + tenant_id);

        return mapToDto(validated);
    }

    /**
     * Retrieves all entries for the current tenant.
     * 
     * @return a list of entry DTOs
     */
    @SuppressWarnings("unchecked")
    public List<EcritureComptableDto> getAll() {
        UUID tenant_id = TenantContext.getCurrentTenant();
        String key = CACHE_ALL + tenant_id;
        List<EcritureComptableDto> cached = redis_service.get(key, List.class);
        if (cached != null)
            return cached;

        List<EcritureComptableDto> list = ecriture_repository.findByTenant_Id(tenant_id)
                .stream().map(this::mapToDto).collect(Collectors.toList());
        redis_service.save(key, list, Duration.ofMinutes(10));
        return list;
    }

    /**
     * Retrieves non-validated entries for the current tenant.
     * 
     * @return a list of non-validated entry DTOs
     */
    @SuppressWarnings("unchecked")
    public List<EcritureComptableDto> getNonValidated() {
        UUID tenant_id = TenantContext.getCurrentTenant();
        String key = CACHE_NON_VALIDATED + tenant_id;
        List<EcritureComptableDto> cached = redis_service.get(key, List.class);
        if (cached != null)
            return cached;

        List<EcritureComptableDto> list = ecriture_repository.findByTenant_IdAndValideeFalse(tenant_id)
                .stream().map(this::mapToDto).collect(Collectors.toList());
        redis_service.save(key, list, Duration.ofMinutes(10));
        return list;
    }

    /**
     * Retrieves an entry by its ID.
     * 
     * @param id the id of the entry
     * @return an optional containing the entry DTO if found
     */
    public Optional<EcritureComptableDto> getById(UUID id) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        return ecriture_repository.findByTenant_IdAndId(tenant_id, id)
                .map(this::mapToDto);
    }

    /**
     * Search entries by date range and journal.
     * 
     * @param start_date start of the date range
     * @param end_date   end of the date range
     * @param journal_id the journal id
     * @return a list of matching entry DTOs
     */
    @SuppressWarnings("unchecked")
    public List<EcritureComptableDto> searchEcritures(LocalDateTime start_date, LocalDateTime end_date,
            UUID journal_id) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        if (start_date != null && end_date != null && start_date.isAfter(end_date)) {
            throw new BusinessException("Start date must be before end date");
        }
        String cache_key = CACHE_SEARCH + tenant_id + ":" + (start_date != null ? start_date : "all")
                + ":" + (end_date != null ? end_date : "all")
                + ":" + (journal_id != null ? journal_id : "all");

        List<EcritureComptableDto> cached = redis_service.get(cache_key, List.class);
        if (cached != null)
            return cached;

        List<EcritureComptable> results;

        if (start_date != null && end_date != null && journal_id != null) {
            results = ecriture_repository.findByTenant_IdAndJournal_IdAndDate_ecritureBetween(
                    tenant_id, journal_id, start_date.toLocalDate(), end_date.toLocalDate());
        } else if (start_date != null && end_date != null) {
            results = ecriture_repository.findByTenant_IdAndDate_ecritureBetween(
                    tenant_id, start_date.toLocalDate(), end_date.toLocalDate());
        } else if (journal_id != null) {
            results = ecriture_repository.findByTenant_IdAndJournal_Id(tenant_id, journal_id);
        } else {
            results = ecriture_repository.findByTenant_Id(tenant_id);
        }

        List<EcritureComptableDto> list = results.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        redis_service.save(cache_key, list, Duration.ofMinutes(5));
        return list;
    }

    /**
     * Generates an accounting entry from a comptable object.
     * 
     * @param object the comptable object
     * @return the generated entry DTO
     */
    @Transactional
    public EcritureComptableDto generateFromComptableObject(ComptableObject object) {
        UUID tenant_id = object.get_tenant_id();
        Tenant tenant = TenantContext.getCurrentTenantAsTenant();
        String current_user = Optional.ofNullable(TenantContext.getCurrentUser()).orElse("system");

        if (object.get_montant() == null || object.get_montant().doubleValue() <= 0)
            throw new BusinessException("Invalid amount for entry generation");

        // Verify journal and active period
        journal_service.getJournalComptable(object.get_journal_comptable_id());
        PeriodeComptableDto current_periode = periode_service.getCurrentPeriode(tenant_id);

        EcritureComptable ecriture = EcritureComptable.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .numero_ecriture("ECR-" + UUID.randomUUID().toString().substring(0, 8))
                .libelle(object.get_description() != null ? object.get_description() : "Auto-generated entry")
                .journal(journal_repository.findById(object.get_journal_comptable_id())
                        .orElseThrow(() -> new ResourceNotFoundException("Journal",
                                object.get_journal_comptable_id().toString())))
                .periode(periode_repository.findById(object.get_periode_comptable_id())
                        .orElseThrow(() -> new ResourceNotFoundException("Period", current_periode.getId().toString())))
                .montant_total_debit(object.get_montant())
                .montant_total_credit(object.get_montant())
                .validee(false)
                .created_at(LocalDateTime.now())
                .updated_at(LocalDateTime.now())
                .created_by(current_user)
                .updated_by(current_user)
                .build();

        EcritureComptable saved = ecriture_repository.save(ecriture);
        log.info("🧾 Entry generated automatically from object {}", object.get_source_type());

        // Generate details (debit/credit)
        detail_service.generateDetailsFromComptableObject(saved, object);

        validateBalance(detail_repository.findByTenant_IdAndEcriture_Id(tenant_id, saved.getId()));

        logAuditAndSendKafka(tenant, saved.getId(), current_user, "AUTO_GENERATE", "Entry generated automatically");
        redis_service.delete(CACHE_ALL + tenant_id);

        return mapToDto(saved);
    }

    /**
     * Deletes an accounting entry.
     * 
     * @param id the id of the entry to delete
     */
    @Transactional
    public void deleteEcriture(UUID id) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        EcritureComptable ecriture = ecriture_repository.findByTenant_IdAndId(tenant_id, id)
                .orElseThrow(() -> new ResourceNotFoundException("Entry", id.toString()));

        if (Boolean.TRUE.equals(ecriture.getValidee()))
            throw new BusinessException("Cannot delete a validated entry");

        ecriture_repository.delete(ecriture);
        redis_service.delete(CACHE_ALL + tenant_id);
        log.info("🗑️ Entry {} deleted", id);
    }

    private void logAuditAndSendKafka(Tenant tenant, UUID ecriture_id, String user, String action, String details) {
        kafka_operations.executeInTransaction(ops -> {
            JournalAudit audit = JournalAudit.builder()
                    .tenant(tenant)
                    .ecriture_comptable_id(ecriture_id)
                    .utilisateur(user)
                    .action(action)
                    .details(details)
                    .date_action(LocalDateTime.now())
                    .build();
            audit_repository.save(audit);
            kafka_message_service.sendAuditLog(audit, tenant.getId(), action);
            return null;
        });
    }

    private void validateBalance(List<DetailEcriture> details) {
        BigDecimal debit = details.stream()
                .map(DetailEcriture::getMontant_debit)
                .map(m -> m != null ? m : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credit = details.stream()
                .map(DetailEcriture::getMontant_credit)
                .map(m -> m != null ? m : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (debit.subtract(credit).abs().compareTo(BigDecimal.valueOf(0.01)) > 0) {
            throw new BusinessException("Unbalanced entry: debit=" + debit + " credit=" + credit);
        }
    }

    private EcritureComptable mapToEntity(EcritureComptableDto dto, Tenant tenant) {
        return EcritureComptable.builder()
                .id(dto.getId())
                .tenant(tenant)
                .numero_ecriture(dto.getNumero_ecriture())
                .libelle(dto.getLibelle())
                .date_ecriture(dto.getDate_ecriture())
                .journal(journal_repository.findById(dto.getJournal_comptable_id())
                        .orElseThrow(() -> new ResourceNotFoundException("Journal",
                                dto.getJournal_comptable_id().toString())))
                .periode(periode_repository.findById(dto.getPeriode_comptable_id())
                        .orElseThrow(() -> new ResourceNotFoundException("Period",
                                dto.getPeriode_comptable_id().toString())))
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
                .journal_comptable_id(e.getJournal() != null ? e.getJournal().getId() : null)
                .periode_comptable_id(e.getPeriode() != null ? e.getPeriode().getId() : null)
                .montant_total_debit(e.getMontant_total_debit())
                .montant_total_credit(e.getMontant_total_credit())
                .validee(e.getValidee())
                .reference_externe(e.getReference_externe())
                .notes(e.getNotes())
                .created_at(e.getCreated_at())
                .updated_at(e.getUpdated_at())
                .build();
    }
}
